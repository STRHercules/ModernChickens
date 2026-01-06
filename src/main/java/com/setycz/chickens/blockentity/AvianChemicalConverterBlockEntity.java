package com.setycz.chickens.blockentity;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.GasEggRegistry;
import com.setycz.chickens.block.AvianChemicalConverterBlock;
import com.setycz.chickens.config.ChickensConfigHolder;
import com.setycz.chickens.config.ChickensConfigValues;
import com.setycz.chickens.integration.kubejs.MachineRecipeRegistry;
import com.setycz.chickens.integration.mekanism.MekanismChemicalHelper;
import com.setycz.chickens.item.ChemicalEggItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.GasEggItem;
import com.setycz.chickens.menu.AvianChemicalConverterMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;

/**
 * Reflection-driven chemical storage block entity that mirrors the fluid
 * converter architecture. Chemical eggs get cracked into an internal buffer
 * and the contents are exposed through Mekanism's chemical capability when the
 * API is available at runtime.
 */
public class AvianChemicalConverterBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 1;
    private static final int[] ACCESSIBLE_SLOTS = new int[] { 0 };
    private static final int DEFAULT_TANK_CAPACITY = 8_000;
    private static final int DEFAULT_TRANSFER_RATE = 2_000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final Map<Direction, Object> capabilityCache = new EnumMap<>(Direction.class);

    private int chemicalAmount;
    private int tankCapacity = DEFAULT_TANK_CAPACITY;
    private int transferRate = DEFAULT_TRANSFER_RATE;
    @Nullable
    private ResourceLocation chemicalId;
    private int chemicalEntryId = -1;
    private boolean storedGaseous;
    private boolean cachedActiveState;
    @Nullable
    private Component customName;

    public AvianChemicalConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AVIAN_CHEMICAL_CONVERTER.get(), pos, state);
        if (state.hasProperty(AvianChemicalConverterBlock.LIT)) {
            cachedActiveState = state.getValue(AvianChemicalConverterBlock.LIT);
        }
        syncWithConfig(true);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> serverTicker() {
        return (level, pos, state, blockEntity) -> {
            if (blockEntity instanceof AvianChemicalConverterBlockEntity converter) {
                converter.tickServer(level);
            }
        };
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }
        boolean drained = crackChemicalEgg();
        boolean exported = pushChemicalToNeighbors(level);
        boolean shouldGlow = shouldBlockGlow(drained || exported);
        updateActiveState(level, shouldGlow);
        if (drained || exported) {
            setChanged();
        }
    }

    private boolean crackChemicalEgg() {
        ItemStack stack = items.get(0);
        if (!isChemicalEgg(stack)) {
            return false;
        }
        ChemicalEggRegistryItem entry = resolveChemicalEntry(stack);
        if (entry == null) {
            items.set(0, ItemStack.EMPTY);
            setChanged();
            return true;
        }
        // Prefer KubeJS overrides if the input chemical has a custom conversion mapping.
        MachineRecipeRegistry.ChemicalConverterRecipe custom = MachineRecipeRegistry.findChemicalConverterRecipe(entry.getChemicalId());
        if (custom != null) {
            ResourceLocation outputId = custom.outputChemicalId();
            if (!canAccept(outputId)) {
                return false;
            }
            int volume = custom.outputAmount();
            if (chemicalAmount + volume > tankCapacity) {
                return false;
            }
            items.set(0, ItemStack.EMPTY);
            storeChemical(outputId, volume);
            markChemicalDirty();
            return true;
        }
        if (!canAccept(entry)) {
            return false;
        }
        int volume = Math.max(entry.getVolume(), 0);
        if (volume <= 0) {
            items.set(0, ItemStack.EMPTY);
            setChanged();
            return true;
        }
        if (chemicalAmount + volume > tankCapacity) {
            return false;
        }
        items.set(0, ItemStack.EMPTY);
        storeChemical(entry.getChemicalId(), volume);
        markChemicalDirty();
        return true;
    }

    private boolean canAccept(ChemicalEggRegistryItem entry) {
        if (entry == null) {
            return false;
        }
        return canAccept(entry.getChemicalId());
    }

    private boolean canAccept(@Nullable ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (chemicalId == null || chemicalAmount <= 0) {
            return true;
        }
        return chemicalId.equals(id);
    }

    private void storeChemical(ResourceLocation id, int volume) {
        // Centralise bookkeeping so custom recipes keep GUI data in sync.
        chemicalId = id;
        chemicalAmount += volume;
        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findByChemical(id);
        if (entry == null) {
            entry = GasEggRegistry.findByChemical(id);
        }
        if (entry != null) {
            chemicalEntryId = entry.getId();
            storedGaseous = entry.isGaseous();
        } else {
            chemicalEntryId = -1;
            storedGaseous = false;
        }
        invalidateChemicalHandlers();
    }

    /**
     * Attempts to transfer stored chemicals into every neighbouring block that
     * exposes Mekanism's chemical capability. Reflection keeps the codebase
     * decoupled from Mekanism while still allowing automation support when the
     * mod is present.
     */
    private boolean pushChemicalToNeighbors(Level level) {
        if (chemicalAmount <= 0 || chemicalId == null) {
            return false;
        }
        if (!MekanismChemicalHelper.isChemicalCapabilityAvailable()) {
            return false;
        }
        boolean exported = false;
        for (Direction direction : Direction.values()) {
            if (chemicalAmount <= 0) {
                break;
            }
            Object handler = MekanismChemicalHelper.getBlockChemicalHandler(level,
                    worldPosition.relative(direction), direction.getOpposite());
            if (handler == null) {
                continue;
            }
            long toSend = Math.min(transferRate, chemicalAmount);
            Object requested = MekanismChemicalHelper.createStack(chemicalId, toSend);
            if (MekanismChemicalHelper.isStackEmpty(requested)) {
                continue;
            }
            Object simulated = MekanismChemicalHelper.insertChemical(handler, requested, true);
            long accepted = toSend - MekanismChemicalHelper.getStackAmount(simulated);
            if (accepted <= 0) {
                continue;
            }
            Object executeStack = MekanismChemicalHelper.createStack(chemicalId, accepted);
            Object remainder = MekanismChemicalHelper.insertChemical(handler, executeStack, false);
            long inserted = accepted - MekanismChemicalHelper.getStackAmount(remainder);
            if (inserted <= 0) {
                continue;
            }
            chemicalAmount -= (int) Math.min(inserted, Integer.MAX_VALUE);
            exported = true;
            if (chemicalAmount <= 0) {
                clearChemical();
            }
        }
        if (exported) {
            markChemicalDirty();
        }
        return exported;
    }

    private void clearChemical() {
        chemicalAmount = 0;
        chemicalId = null;
        chemicalEntryId = -1;
        storedGaseous = false;
        invalidateChemicalHandlers();
    }

    private void invalidateChemicalHandlers() {
        capabilityCache.clear();
    }

    private void markChemicalDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            updateActiveState(level, shouldBlockGlow(false));
        }
    }

    @Nullable
    public ChemicalEggRegistryItem getStoredEntry() {
        if (chemicalId == null) {
            return null;
        }
        ChemicalEggRegistryItem entry = storedGaseous
                ? GasEggRegistry.findByChemical(chemicalId)
                : ChemicalEggRegistry.findByChemical(chemicalId);
        if (entry == null) {
            entry = ChemicalEggRegistry.findByChemical(chemicalId);
        }
        if (entry == null) {
            entry = GasEggRegistry.findByChemical(chemicalId);
        }
        return entry;
    }

    public int getChemicalAmount() {
        return chemicalAmount;
    }

    public int getTankCapacity() {
        return tankCapacity;
    }

    @Nullable
    public ResourceLocation getChemicalId() {
        return chemicalId;
    }

    public int getStoredEntryId() {
        return chemicalEntryId;
    }

    public boolean isStoredGaseous() {
        return storedGaseous;
    }

    public int getComparatorOutput() {
        int capacity = Math.max(1, tankCapacity);
        return Math.round(15.0F * chemicalAmount / (float) capacity);
    }

    @Nullable
    public Object getChemicalHandler(Direction direction) {
        if (!MekanismChemicalHelper.isChemicalCapabilityAvailable()) {
            return null;
        }
        return capabilityCache.computeIfAbsent(direction == null ? Direction.NORTH : direction, side ->
                AvianChemicalHandlerFactory.create(this, side));
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, index, count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack removed = ContainerHelper.takeItem(items, index);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            updateActiveState(level, shouldBlockGlow(false));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return isChemicalEgg(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Component getDisplayName() {
        return customName != null ? customName : Component.translatable("menu.chickens.avian_chemical_converter");
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(Component name) {
        customName = name;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AvianChemicalConverterMenu(id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        CompoundTag tankTag = new CompoundTag();
        tankTag.putInt("Amount", chemicalAmount);
        tankTag.putInt("Capacity", tankCapacity);
        if (chemicalId != null) {
            tankTag.putString("Chemical", chemicalId.toString());
        }
        tankTag.putInt("EntryId", chemicalEntryId);
        tankTag.putBoolean("Gaseous", storedGaseous);
        tag.put("ChemicalTank", tankTag);
        tag.putInt("TransferRate", transferRate);
        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), customName)
                    .result().ifPresent(component -> tag.put("CustomName", component));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        if (tag.contains("ChemicalTank", Tag.TAG_COMPOUND)) {
            CompoundTag tankTag = tag.getCompound("ChemicalTank");
            chemicalAmount = Math.max(0, tankTag.getInt("Amount"));
            tankCapacity = Math.max(1, tankTag.getInt("Capacity"));
            if (tankTag.contains("Chemical")) {
                chemicalId = ResourceLocation.tryParse(tankTag.getString("Chemical"));
            } else {
                chemicalId = null;
            }
            chemicalEntryId = tankTag.contains("EntryId") ? tankTag.getInt("EntryId") : -1;
            storedGaseous = tankTag.getBoolean("Gaseous");
        } else {
            chemicalAmount = 0;
            tankCapacity = DEFAULT_TANK_CAPACITY;
            chemicalId = null;
            chemicalEntryId = -1;
            storedGaseous = false;
        }
        transferRate = Math.max(0, tag.getInt("TransferRate"));
        if (tag.contains("CustomName", Tag.TAG_COMPOUND)) {
            ComponentSerialization.CODEC
                    .parse(provider.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("CustomName"))
                    .result().ifPresent(component -> customName = component);
        } else {
            customName = null;
        }
        syncWithConfig(false);
        cachedActiveState = false;
        invalidateChemicalHandlers();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncWithConfig(false);
        invalidateChemicalHandlers();
        if (level != null && !level.isClientSide) {
            updateActiveState(level, shouldBlockGlow(false));
        }
    }

    private void syncWithConfig(boolean overwriteCapacity) {
        ChickensConfigValues config = ChickensConfigHolder.get();
        int configuredCapacity = Math.max(1,
                config.getAvianChemicalConverterCapacity(DEFAULT_TANK_CAPACITY));
        int configuredTransfer = Math.max(1,
                config.getAvianChemicalConverterTransfer(DEFAULT_TRANSFER_RATE));
        transferRate = configuredTransfer;
        int clampedCapacity = overwriteCapacity
                ? configuredCapacity
                : Math.min(Math.max(tankCapacity, 1), configuredCapacity);
        tankCapacity = clampedCapacity;
        if (chemicalAmount > tankCapacity) {
            chemicalAmount = tankCapacity;
        }
    }

    private boolean shouldBlockGlow(boolean drainedThisTick) {
        boolean hasEggReserves = isChemicalEgg(items.get(0)) && chemicalAmount < tankCapacity;
        if (!ChickensConfigHolder.get().isAvianChemicalConverterEffectsEnabled()) {
            return false;
        }
        return drainedThisTick || hasEggReserves || chemicalAmount > 0;
    }

    private void updateActiveState(Level level, boolean active) {
        if (cachedActiveState == active) {
            return;
        }
        cachedActiveState = active;
        BlockState state = getBlockState();
        if (!state.hasProperty(AvianChemicalConverterBlock.LIT)) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(AvianChemicalConverterBlock.LIT, active), Block.UPDATE_CLIENTS);
    }

    private static boolean isChemicalEgg(ItemStack stack) {
        return stack.getItem() instanceof ChemicalEggItem || stack.getItem() instanceof GasEggItem;
    }

    @Nullable
    private ChemicalEggRegistryItem resolveChemicalEntry(ItemStack stack) {
        int id = ChickenItemHelper.getChickenType(stack);
        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findById(id);
        if (entry == null) {
            entry = GasEggRegistry.findById(id);
        }
        return entry;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    /**
     * Lightweight chemical handler implemented via a dynamic proxy so the mod
     * can expose Mekanism's capability without a direct compile-time dependency
     * on the API jar.
     */
    private static final class AvianChemicalHandlerFactory {
        private AvianChemicalHandlerFactory() {
        }

        @Nullable
        static Object create(AvianChemicalConverterBlockEntity converter, Direction side) {
            if (!MekanismChemicalHelper.isChemicalCapabilityAvailable()) {
                return null;
            }
            return Proxy.newProxyInstance(
                    MekanismChemicalHelper.class.getClassLoader(),
                    new Class<?>[] { getHandlerInterface() },
                    new Handler(converter));
        }

        private static Class<?> getHandlerInterface() {
            try {
                return Class.forName("mekanism.api.chemical.IChemicalHandler");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("IChemicalHandler not present", ex);
            }
        }

        private static final class Handler implements InvocationHandler {
            private final AvianChemicalConverterBlockEntity converter;

            private Handler(AvianChemicalConverterBlockEntity converter) {
                this.converter = converter;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                return switch (name) {
                    case "getChemicalTanks" -> 1;
                    case "getChemicalInTank" -> converter.getStackCopy();
                    case "setChemicalInTank" -> {
                        converter.setFromStack(args != null && args.length > 1 ? args[1] : null);
                        yield null;
                    }
                    case "getChemicalTankCapacity" -> (long) converter.getTankCapacity();
                    case "isValid" -> converter.isTemplateValid(args != null && args.length > 1 ? args[1] : null);
                    case "insertChemical" -> handleInsert(args);
                    case "extractChemical" -> handleExtract(args);
                    case "equals" -> proxy == (args != null && args.length == 1 ? args[0] : null);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "AvianChemicalHandler{" + converter.worldPosition + "}";
                    default -> throw new UnsupportedOperationException("Unsupported chemical handler call: " + name);
                };
            }

            private Object handleInsert(@Nullable Object[] args) {
                if (args == null || args.length == 0) {
                    return MekanismChemicalHelper.emptyStack();
                }
                if (args.length == 3 && args[0] instanceof Integer) {
                    return converter.insertStack(args[1], args[2]);
                }
                if (args.length >= 2) {
                    return converter.insertStack(args[0], args[1]);
                }
                return MekanismChemicalHelper.emptyStack();
            }

            private Object handleExtract(@Nullable Object[] args) {
                if (args == null || args.length == 0) {
                    return MekanismChemicalHelper.emptyStack();
                }
                if (args.length == 3 && args[0] instanceof Integer && args[1] instanceof Long amount) {
                    return converter.extractAmount(amount, args[2]);
                }
                if (args.length == 3 && args[0] instanceof Integer) {
                    return converter.extractStack(args[1], args[2]);
                }
                if (args.length == 2 && args[0] instanceof Long amount) {
                    return converter.extractAmount(amount, args[1]);
                }
                if (args.length >= 2) {
                    return converter.extractStack(args[0], args[1]);
                }
                return MekanismChemicalHelper.emptyStack();
            }
        }
    }

    private Object getStackCopy() {
        if (chemicalId == null || chemicalAmount <= 0) {
            return MekanismChemicalHelper.emptyStack();
        }
        return MekanismChemicalHelper.createStack(chemicalId, chemicalAmount);
    }

    private void setFromStack(@Nullable Object stack) {
        if (stack == null || MekanismChemicalHelper.isStackEmpty(stack)) {
            clearChemical();
            markChemicalDirty();
            return;
        }
        ResourceLocation id = MekanismChemicalHelper.getStackChemicalId(stack);
        long amount = MekanismChemicalHelper.getStackAmount(stack);
        if (id == null) {
            return;
        }
        chemicalId = id;
        chemicalAmount = Math.min((int) Math.min(amount, Integer.MAX_VALUE), tankCapacity);
        chemicalEntryId = -1;
        storedGaseous = false;
        invalidateChemicalHandlers();
        markChemicalDirty();
    }

    private boolean isTemplateValid(@Nullable Object stack) {
        if (stack == null || MekanismChemicalHelper.isStackEmpty(stack)) {
            return true;
        }
        ResourceLocation id = MekanismChemicalHelper.getStackChemicalId(stack);
        if (id == null) {
            return false;
        }
        return chemicalId == null || chemicalId.equals(id);
    }

    /**
     * Handles incoming capability insertions by respecting the configured tank
     * capacity and only accepting a single chemical type.
     */
    private Object insertStack(@Nullable Object stack, @Nullable Object action) {
        if (stack == null || MekanismChemicalHelper.isStackEmpty(stack)) {
            return MekanismChemicalHelper.emptyStack();
        }
        ResourceLocation id = MekanismChemicalHelper.getStackChemicalId(stack);
        if (id == null) {
            return stack;
        }
        long amount = MekanismChemicalHelper.getStackAmount(stack);
        if (amount <= 0) {
            return MekanismChemicalHelper.emptyStack();
        }
        if (chemicalId != null && !chemicalId.equals(id)) {
            return stack;
        }
        int space = Math.max(0, tankCapacity - chemicalAmount);
        if (space <= 0) {
            return stack;
        }
        int accepted = (int) Math.min(space, amount);
        boolean execute = action == MekanismChemicalHelper.getAction(true);
        if (execute) {
            chemicalId = id;
            chemicalAmount += accepted;
            invalidateChemicalHandlers();
            markChemicalDirty();
        }
        long remainder = amount - accepted;
        return remainder <= 0 ? MekanismChemicalHelper.emptyStack() : MekanismChemicalHelper.createStack(id, remainder);
    }

    private Object extractAmount(long amount, @Nullable Object action) {
        if (chemicalId == null || chemicalAmount <= 0 || amount <= 0) {
            return MekanismChemicalHelper.emptyStack();
        }
        int extracted = (int) Math.min(amount, chemicalAmount);
        boolean execute = action == MekanismChemicalHelper.getAction(true);
        if (execute) {
            chemicalAmount -= extracted;
            if (chemicalAmount <= 0) {
                clearChemical();
            }
            markChemicalDirty();
        }
        return MekanismChemicalHelper.createStack(chemicalId, extracted);
    }

    private Object extractStack(@Nullable Object template, @Nullable Object action) {
        if (template == null || MekanismChemicalHelper.isStackEmpty(template)) {
            return MekanismChemicalHelper.emptyStack();
        }
        ResourceLocation id = MekanismChemicalHelper.getStackChemicalId(template);
        long requested = MekanismChemicalHelper.getStackAmount(template);
        if (id == null || requested <= 0) {
            return MekanismChemicalHelper.emptyStack();
        }
        if (chemicalId == null || !chemicalId.equals(id)) {
            return MekanismChemicalHelper.emptyStack();
        }
        return extractAmount(requested, action);
    }
}
