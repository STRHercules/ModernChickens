package com.setycz.chickens.blockentity;

import com.setycz.chickens.ChemicalEggRegistry;
import com.setycz.chickens.ChemicalEggRegistryItem;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.LiquidEggRegistry;
import com.setycz.chickens.LiquidEggRegistryItem;
import com.setycz.chickens.block.AvianDousingMachineBlock;
import com.setycz.chickens.integration.kubejs.MachineRecipeRegistry;
import com.setycz.chickens.integration.mekanism.MekanismChemicalHelper;
import com.setycz.chickens.item.ChickenItem;
import com.setycz.chickens.item.ChickenItemHelper;
import com.setycz.chickens.item.ChickensSpawnEggItem;
import com.setycz.chickens.item.ChemicalEggItem;
import com.setycz.chickens.item.LiquidEggItem;
import com.setycz.chickens.menu.AvianDousingMachineMenu;
import com.setycz.chickens.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid machine that soaks Smart Chickens in stored fluids or Mekanism
 * chemicals to forge the matching Modern Chickens spawn egg. The block entity
 * owns three internal buffers (RF, fluid, chemical) and only crafts when the
 * configured recipe costs are satisfied, keeping all automation-friendly.
 */
public class AvianDousingMachineBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 2;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int[] TOP_SLOTS = new int[] { INPUT_SLOT };
    private static final int[] SIDE_SLOTS = new int[] { INPUT_SLOT, OUTPUT_SLOT };
    private static final int[] BOTTOM_SLOTS = new int[] { OUTPUT_SLOT };

    public static final int LIQUID_CAPACITY = FluidType.BUCKET_VOLUME * 100;
    public static final int CHEMICAL_CAPACITY = FluidType.BUCKET_VOLUME * 100;
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int ENERGY_MAX_RECEIVE = 20_000;
    public static final int MAX_PROGRESS = 200;

    @Deprecated // use per-chicken configurable value via ChickensRegistryItem#getLiquidDousingCost()
    public static final int LIQUID_COST = FluidType.BUCKET_VOLUME * 10;
    public static final int SPECIAL_LIQUID_CAPACITY = FluidType.BUCKET_VOLUME; // 1000 mB buffer for boss infusions
    public static final int SPECIAL_PER_ITEM = 100; // Dragon's Breath bottle / Nether Star adds 100 mB
    public static final int CHEMICAL_COST = FluidType.BUCKET_VOLUME * 10;
    public static final int LIQUID_ENERGY_COST = 10_000;
    public static final int CHEMICAL_ENERGY_COST = 100_000;
    public static final int SPECIAL_ENERGY_COST = LIQUID_ENERGY_COST;

    private static final Map<ResourceLocation, Integer> LIQUID_CHICKEN_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Integer> CHEMICAL_CHICKEN_CACHE = new HashMap<>();

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidTank liquidTank = new FluidTank(LIQUID_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (specialInfusion != SpecialInfusion.NONE) {
                return false;
            }
            FluidStack stored = getFluid();
            return stored.isEmpty() || stored.getFluid() == stack.getFluid();
        }

        @Override
        protected void onContentsChanged() {
            markLiquidDirty();
        }
    };
    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage();

    private final Map<Direction, Object> chemicalHandlers = new EnumMap<>(Direction.class);

    private int maxReceive = ENERGY_MAX_RECEIVE;
    private int chemicalAmount;
    @Nullable
    private ResourceLocation chemicalId;
    private int chemicalEntryId = -1;
    private SpecialInfusion specialInfusion = SpecialInfusion.NONE;
    private int specialAmount;
    private int progress;
    private InfusionMode mode = InfusionMode.NONE;
    private boolean cachedActiveState;
    @Nullable
    private Component customName;

    public AvianDousingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AVIAN_DOUSING_MACHINE.get(), pos, state);
        cachedActiveState = state.hasProperty(AvianDousingMachineBlock.LIT)
                && state.getValue(AvianDousingMachineBlock.LIT);
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> serverTicker() {
        return (level, pos, state, blockEntity) -> {
            if (blockEntity instanceof AvianDousingMachineBlockEntity machine) {
                machine.tickServer(level);
            }
        };
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }

        boolean inventoryChanged = false;
        boolean pulledEnergy = pullEnergyFromNeighbors(level);
        OperationPlan plan = choosePlan();
        mode = plan.mode();
        if (plan.mode() == InfusionMode.NONE) {
            if (progress != 0) {
                progress = 0;
                inventoryChanged = true;
            }
            updateActiveState(level, false);
            if (inventoryChanged) {
                setChanged();
            }
            return;
        }

        boolean canAdvance = hasResourcesFor(plan);
        if (canAdvance) {
            progress++;
            if (progress >= MAX_PROGRESS) {
                completeOperation(plan);
                inventoryChanged = true;
                progress = 0;
            }
        } else if (progress > 0) {
            progress = Math.max(progress - 2, 0);
        }

        updateActiveState(level, progress > 0 || pulledEnergy);
        if (inventoryChanged || canAdvance || pulledEnergy) {
            setChanged();
        }
    }

    private OperationPlan choosePlan() {
        ItemStack input = items.get(INPUT_SLOT);
        ChickensRegistryItem inputChicken = getChicken(input);
        if (inputChicken == null) {
            return OperationPlan.none();
        }
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
            return OperationPlan.none();
        }

        if (specialInfusion == SpecialInfusion.DRAGON_BREATH && specialAmount >= SPECIAL_LIQUID_CAPACITY
                && isChicken(inputChicken, "obsidianChicken")) {
            ChickensRegistryItem dragon = findChickenByName("dragonChicken");
            if (dragon != null && canOutput(output, dragon)) {
                return new OperationPlan(InfusionMode.SPECIAL, dragon, SpecialInfusion.DRAGON_BREATH, 0, 0, SPECIAL_ENERGY_COST);
            }
        }
        if (specialInfusion == SpecialInfusion.NETHER_STAR && specialAmount >= SPECIAL_LIQUID_CAPACITY
                && isChicken(inputChicken, "soulSandChicken")) {
            ChickensRegistryItem wither = findChickenByName("witherChicken");
            if (wither != null && canOutput(output, wither)) {
                return new OperationPlan(InfusionMode.SPECIAL, wither, SpecialInfusion.NETHER_STAR, 0, 0, SPECIAL_ENERGY_COST);
            }
        }

        // Custom dousing recipes registered via KubeJS take priority over the default mappings.
        MachineRecipeRegistry.DousingRecipe customChemical = findCustomChemicalRecipe(inputChicken);
        if (customChemical != null) {
            if (chemicalAmount >= customChemical.reagentAmount()) {
                ChickensRegistryItem chicken = ChickensRegistry.getByType(customChemical.outputChickenId());
                if (chicken != null && canOutput(output, chicken)) {
                    return new OperationPlan(InfusionMode.CHEMICAL, chicken, SpecialInfusion.NONE, 0,
                            customChemical.reagentAmount(), customChemical.energyCost());
                }
            }
            return OperationPlan.none();
        }

        if (chemicalAmount >= CHEMICAL_COST && chemicalId != null) {
            ChickensRegistryItem chicken = resolveChemicalChicken(chemicalId);
            if (chicken != null && canOutput(output, chicken)) {
                return new OperationPlan(InfusionMode.CHEMICAL, chicken, SpecialInfusion.NONE, 0, CHEMICAL_COST,
                        CHEMICAL_ENERGY_COST);
            }
        }

        FluidStack stored = liquidTank.getFluid();
        if (!stored.isEmpty()) {
            MachineRecipeRegistry.DousingRecipe customLiquid = findCustomLiquidRecipe(inputChicken, stored);
            if (customLiquid != null) {
                if (stored.getAmount() >= customLiquid.reagentAmount()) {
                    ChickensRegistryItem chicken = ChickensRegistry.getByType(customLiquid.outputChickenId());
                    if (chicken != null && canOutput(output, chicken)) {
                        return new OperationPlan(InfusionMode.LIQUID, chicken, SpecialInfusion.NONE,
                                customLiquid.reagentAmount(), 0, customLiquid.energyCost());
                    }
                }
                return OperationPlan.none();
            }
            ChickensRegistryItem chicken = resolveLiquidChicken(stored);
            if (chicken != null) {
                int liquidCost = chicken.getLiquidDousingCost();
                if (stored.getAmount() >= liquidCost && canOutput(output, chicken)) {
                    return new OperationPlan(InfusionMode.LIQUID, chicken, SpecialInfusion.NONE, liquidCost, 0,
                            LIQUID_ENERGY_COST);
                }
            }
        }

        return OperationPlan.none();
    }

    private boolean hasResourcesFor(OperationPlan plan) {
        if (plan.mode() == InfusionMode.CHEMICAL) {
            return energyStorage.getEnergyStored() >= plan.energyCost() && chemicalAmount >= plan.chemicalCost();
        }
        if (plan.mode() == InfusionMode.LIQUID) {
            return energyStorage.getEnergyStored() >= plan.energyCost()
                    && liquidTank.getFluidAmount() >= plan.liquidCost();
        }
        if (plan.mode() == InfusionMode.SPECIAL) {
            return energyStorage.getEnergyStored() >= plan.energyCost() && specialAmount >= SPECIAL_LIQUID_CAPACITY;
        }
        return false;
    }

    private void completeOperation(OperationPlan plan) {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!isDousableChicken(input)) {
            return;
        }

        ItemStack result = ChickensSpawnEggItem.createFor(plan.chicken());
        if (!output.isEmpty()) {
            output.grow(1);
        } else {
            items.set(OUTPUT_SLOT, result);
        }

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        if (plan.mode() == InfusionMode.CHEMICAL) {
            if (!energyStorage.consumeEnergy(plan.energyCost())) {
                return;
            }
            chemicalAmount -= plan.chemicalCost();
            if (chemicalAmount <= 0) {
                clearChemical();
            }
            invalidateChemicalHandlers();
            markChemicalDirty();
        } else if (plan.mode() == InfusionMode.LIQUID) {
            if (!energyStorage.consumeEnergy(plan.energyCost())) {
                return;
            }
            liquidTank.drain(plan.liquidCost(), IFluidHandler.FluidAction.EXECUTE);
        } else if (plan.mode() == InfusionMode.SPECIAL) {
            if (!energyStorage.consumeEnergy(plan.energyCost())) {
                return;
            }
            specialAmount = 0;
            specialInfusion = SpecialInfusion.NONE;
            markLiquidDirty();
        }

        mode = plan.mode();
        markEnergyDirty();
    }

    private ChickensRegistryItem getChicken(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (!(stack.getItem() instanceof ChickensSpawnEggItem || stack.getItem() instanceof ChickenItem)) {
            return null;
        }
        int type = ChickenItemHelper.getChickenType(stack);
        return ChickensRegistry.getByType(type);
    }

    private boolean isSmartChicken(ItemStack stack) {
        return getChicken(stack) != null && ChickenItemHelper.getChickenType(stack) == ChickensRegistry.SMART_CHICKEN_ID;
    }

    public boolean isDousableChicken(ItemStack stack) {
        ChickensRegistryItem chicken = getChicken(stack);
        if (chicken == null) {
            return false;
        }
        if (chicken.getId() == ChickensRegistry.SMART_CHICKEN_ID) {
            return true;
        }
        // Accept any chicken that has an explicit KubeJS dousing recipe.
        if (MachineRecipeRegistry.hasDousingRecipeForInput(chicken.getId())) {
            return true;
        }
        return isChicken(chicken, "obsidianChicken") || isChicken(chicken, "soulSandChicken");
    }

    private boolean canOutput(ItemStack output, ChickensRegistryItem chicken) {
        ItemStack template = ChickensSpawnEggItem.createFor(chicken);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, template) && output.getCount() < output.getMaxStackSize();
    }

    private void updateActiveState(Level level, boolean active) {
        if (cachedActiveState == active) {
            return;
        }
        cachedActiveState = active;
        BlockState state = getBlockState();
        if (!state.hasProperty(AvianDousingMachineBlock.LIT)) {
            return;
        }
        level.setBlock(worldPosition, state.setValue(AvianDousingMachineBlock.LIT, active), Block.UPDATE_CLIENTS);
    }

    private void markLiquidDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private void markEnergyDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    private boolean pullEnergyFromNeighbors(Level level) {
        if (energyStorage.getEnergyStored() >= ENERGY_CAPACITY) {
            return false;
        }
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (energyStorage.getEnergyStored() >= ENERGY_CAPACITY) {
                break;
            }
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            int space = Math.min(maxReceive, ENERGY_CAPACITY - energyStorage.getEnergyStored());
            if (space <= 0) {
                break;
            }
            int available = neighbor.extractEnergy(space, true);
            if (available <= 0) {
                continue;
            }
            int acceptable = energyStorage.receiveEnergy(available, true);
            if (acceptable <= 0) {
                continue;
            }
            int drained = neighbor.extractEnergy(acceptable, false);
            if (drained <= 0) {
                continue;
            }
            energyStorage.receiveEnergy(drained, false);
            changed = true;
        }
        return changed;
    }

    private void markChemicalDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
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
        ItemStack result = ContainerHelper.removeItem(items, index, count);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(items, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index == INPUT_SLOT) {
            return isDousableChicken(stack);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return index == OUTPUT_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return index == INPUT_SLOT && isDousableChicken(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return TOP_SLOTS;
        }
        if (side == Direction.DOWN) {
            return BOTTOM_SLOTS;
        }
        return SIDE_SLOTS;
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
        return 64;
    }

    @Override
    public Component getDisplayName() {
        return customName != null
                ? customName
                : Component.translatable("menu.chickens.avian_dousing_machine");
    }

    public void setCustomName(Component name) {
        customName = name;
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AvianDousingMachineMenu(id, inventory, this);
    }

    public FluidTank getFluidTank(@Nullable Direction direction) {
        return liquidTank;
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction direction) {
        return energyStorage;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return ENERGY_CAPACITY;
    }

    public int getChemicalAmount() {
        return chemicalAmount;
    }

    public int getChemicalCapacity() {
        return CHEMICAL_CAPACITY;
    }

    public int getChemicalEntryId() {
        return chemicalEntryId;
    }

    public FluidStack getFluid() {
        return liquidTank.getFluid();
    }

    public int getLiquidAmount() {
        return specialAmount > 0 ? specialAmount : liquidTank.getFluidAmount();
    }

    public int getLiquidCapacity() {
        return specialAmount > 0 ? SPECIAL_LIQUID_CAPACITY : liquidTank.getCapacity();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public InfusionMode getMode() {
        return mode;
    }

    public SpecialInfusion getSpecialInfusion() {
        return specialInfusion;
    }

    public int getSpecialAmount() {
        return specialAmount;
    }

    public int getComparatorOutput() {
        if (ENERGY_CAPACITY <= 0) {
            return 0;
        }
        return Math.round(15.0F * energyStorage.getEnergyStored() / (float) ENERGY_CAPACITY);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider provider) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            loadAdditional(tag, provider);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putString("Mode", mode.name());
        tag.putString("SpecialInfusion", specialInfusion.name());
        tag.putInt("SpecialAmount", specialAmount);

        CompoundTag liquid = new CompoundTag();
        liquidTank.writeToNBT(provider, liquid);
        tag.put("Liquid", liquid);

        tag.putInt("ChemicalAmount", chemicalAmount);
        if (chemicalId != null) {
            tag.putString("ChemicalId", chemicalId.toString());
        }
        tag.putInt("ChemicalEntry", chemicalEntryId);

        if (customName != null) {
            ComponentSerialization.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), customName)
                    .result()
                    .ifPresent(component -> tag.put("CustomName", component));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        ContainerHelper.loadAllItems(tag, items, provider);
        energyStorage.setEnergy(Mth.clamp(tag.getInt("Energy"), 0, ENERGY_CAPACITY));
        progress = Mth.clamp(tag.getInt("Progress"), 0, MAX_PROGRESS);
        mode = parseMode(tag.getString("Mode"));
        specialInfusion = parseSpecial(tag.getString("SpecialInfusion"));
        specialAmount = Mth.clamp(tag.getInt("SpecialAmount"), 0, SPECIAL_LIQUID_CAPACITY);

        if (tag.contains("Liquid", Tag.TAG_COMPOUND)) {
            liquidTank.readFromNBT(provider, tag.getCompound("Liquid"));
        } else {
            liquidTank.setFluid(FluidStack.EMPTY);
        }

        chemicalAmount = Mth.clamp(tag.getInt("ChemicalAmount"), 0, CHEMICAL_CAPACITY);
        if (tag.contains("ChemicalId", Tag.TAG_STRING)) {
            chemicalId = ResourceLocation.tryParse(tag.getString("ChemicalId"));
        } else {
            chemicalId = null;
        }
        chemicalEntryId = tag.contains("ChemicalEntry", Tag.TAG_INT) ? tag.getInt("ChemicalEntry") : -1;

        if (tag.contains("CustomName", Tag.TAG_COMPOUND)) {
            ComponentSerialization.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE),
                    tag.getCompound("CustomName"))
                    .result()
                    .ifPresent(component -> customName = component);
        } else {
            customName = null;
        }

        invalidateChemicalHandlers();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        invalidateChemicalHandlers();
        if (level != null && !level.isClientSide) {
            updateActiveState(level, false);
        }
    }

    public Object getChemicalHandler(@Nullable Direction direction) {
        if (!MekanismChemicalHelper.isChemicalCapabilityAvailable()) {
            return null;
        }
        Direction key = direction == null ? Direction.NORTH : direction;
        return chemicalHandlers.computeIfAbsent(key, side -> DousingChemicalHandlerFactory.create(this));
    }

    private void invalidateChemicalHandlers() {
        chemicalHandlers.clear();
    }

    private void clearChemical() {
        chemicalAmount = 0;
        chemicalId = null;
        chemicalEntryId = -1;
    }

    private void syncChemicalEntry() {
        if (chemicalId == null) {
            chemicalEntryId = -1;
            return;
        }
        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findByChemical(chemicalId);
        if (entry != null) {
            chemicalEntryId = entry.getId();
        } else {
            chemicalEntryId = -1;
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
        chemicalAmount = Math.min((int) Math.min(amount, Integer.MAX_VALUE), CHEMICAL_CAPACITY);
        syncChemicalEntry();
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
        int space = Math.max(0, CHEMICAL_CAPACITY - chemicalAmount);
        if (space <= 0) {
            return stack;
        }
        int accepted = (int) Math.min(space, amount);
        boolean execute = action == MekanismChemicalHelper.getAction(true);
        if (execute) {
            chemicalId = id;
            chemicalAmount += accepted;
            syncChemicalEntry();
            invalidateChemicalHandlers();
            markChemicalDirty();
        }
        long remainder = amount - accepted;
        return remainder <= 0 ? MekanismChemicalHelper.emptyStack()
                : MekanismChemicalHelper.createStack(id, remainder);
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

    private static InfusionMode parseMode(String value) {
        try {
            return InfusionMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return InfusionMode.NONE;
        }
    }

    private static SpecialInfusion parseSpecial(String value) {
        try {
            return SpecialInfusion.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return SpecialInfusion.NONE;
        }
    }

    @Nullable
    private MachineRecipeRegistry.DousingRecipe findCustomChemicalRecipe(@Nullable ChickensRegistryItem inputChicken) {
        if (inputChicken == null || chemicalId == null) {
            return null;
        }
        return MachineRecipeRegistry.findDousingRecipe(MachineRecipeRegistry.DousingType.CHEMICAL,
                inputChicken.getId(), chemicalId);
    }

    @Nullable
    private MachineRecipeRegistry.DousingRecipe findCustomLiquidRecipe(@Nullable ChickensRegistryItem inputChicken,
            FluidStack stored) {
        if (inputChicken == null || stored.isEmpty()) {
            return null;
        }
        ResourceLocation fluidId = stored.getFluid().builtInRegistryHolder().key().location();
        if (fluidId == null) {
            return null;
        }
        return MachineRecipeRegistry.findDousingRecipe(MachineRecipeRegistry.DousingType.FLUID,
                inputChicken.getId(), fluidId);
    }

    @Nullable
    private ChickensRegistryItem resolveLiquidChicken(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation fluidId = stack.getFluid().builtInRegistryHolder().key().location();
        if (fluidId == null) {
            return null;
        }
        Integer cached = LIQUID_CHICKEN_CACHE.get(fluidId);
        if (cached != null) {
            ChickensRegistryItem cachedChicken = ChickensRegistry.getByType(cached);
            if (cachedChicken != null) {
                return cachedChicken;
            }
            LIQUID_CHICKEN_CACHE.remove(fluidId);
        }

        LiquidEggRegistryItem entry = LiquidEggRegistry.findByFluid(fluidId);
        if (entry == null) {
            return null;
        }
        ItemStack target = LiquidEggItem.createFor(entry);
        ChickensRegistryItem chicken = findChickenByLayItem(target);
        if (chicken != null) {
            LIQUID_CHICKEN_CACHE.put(fluidId, chicken.getId());
        }
        return chicken;
    }

    public int getLiquidCostForStoredFluid() {
        FluidStack stored = liquidTank.getFluid();
        if (stored.isEmpty()) {
            return ChickensRegistryItem.DEFAULT_LIQUID_DOUSING_COST;
        }
        // Prefer a matching KubeJS recipe so the GUI shows custom per-recipe costs.
        ChickensRegistryItem inputChicken = getChicken(items.get(INPUT_SLOT));
        MachineRecipeRegistry.DousingRecipe custom = findCustomLiquidRecipe(inputChicken, stored);
        if (custom != null) {
            return custom.reagentAmount();
        }
        ChickensRegistryItem chicken = resolveLiquidChicken(stored);
        if (chicken == null) {
            return ChickensRegistryItem.DEFAULT_LIQUID_DOUSING_COST;
        }
        return chicken.getLiquidDousingCost();
    }

    public int getLiquidEnergyCostForStoredFluid() {
        FluidStack stored = liquidTank.getFluid();
        if (stored.isEmpty()) {
            return LIQUID_ENERGY_COST;
        }
        ChickensRegistryItem inputChicken = getChicken(items.get(INPUT_SLOT));
        MachineRecipeRegistry.DousingRecipe custom = findCustomLiquidRecipe(inputChicken, stored);
        if (custom != null) {
            return custom.energyCost();
        }
        return LIQUID_ENERGY_COST;
    }

    public int getChemicalCostForStoredChemical() {
        ChickensRegistryItem inputChicken = getChicken(items.get(INPUT_SLOT));
        MachineRecipeRegistry.DousingRecipe custom = findCustomChemicalRecipe(inputChicken);
        if (custom != null) {
            return custom.reagentAmount();
        }
        return CHEMICAL_COST;
    }

    public int getChemicalEnergyCostForStoredChemical() {
        ChickensRegistryItem inputChicken = getChicken(items.get(INPUT_SLOT));
        MachineRecipeRegistry.DousingRecipe custom = findCustomChemicalRecipe(inputChicken);
        if (custom != null) {
            return custom.energyCost();
        }
        return CHEMICAL_ENERGY_COST;
    }

    @Nullable
    private ChickensRegistryItem resolveChemicalChicken(ResourceLocation id) {
        Integer cached = CHEMICAL_CHICKEN_CACHE.get(id);
        if (cached != null) {
            ChickensRegistryItem cachedChicken = ChickensRegistry.getByType(cached);
            if (cachedChicken != null) {
                return cachedChicken;
            }
            CHEMICAL_CHICKEN_CACHE.remove(id);
        }

        ChemicalEggRegistryItem entry = ChemicalEggRegistry.findByChemical(id);
        if (entry == null) {
            return null;
        }
        ItemStack target = ChemicalEggItem.createFor(entry);
        ChickensRegistryItem chicken = findChickenByLayItem(target);
        if (chicken != null) {
            CHEMICAL_CHICKEN_CACHE.put(id, chicken.getId());
        }
        return chicken;
    }

    @Nullable
    private static ChickensRegistryItem findChickenByLayItem(ItemStack layStack) {
        Collection<ChickensRegistryItem> enabled = ChickensRegistry.getItems();
        for (ChickensRegistryItem chicken : enabled) {
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), layStack)) {
                return chicken;
            }
        }
        Collection<ChickensRegistryItem> disabled = ChickensRegistry.getDisabledItems();
        for (ChickensRegistryItem chicken : disabled) {
            if (ItemStack.isSameItemSameComponents(chicken.createLayItem(), layStack)) {
                return chicken;
            }
        }
        return null;
    }

    private static boolean isChicken(ChickensRegistryItem chicken, String entityName) {
        return chicken.getEntityName().equalsIgnoreCase(entityName);
    }

    @Nullable
    private static ChickensRegistryItem findChickenByName(String entityName) {
        for (ChickensRegistryItem chicken : ChickensRegistry.getItems()) {
            if (chicken.getEntityName().equalsIgnoreCase(entityName)) {
                return chicken;
            }
        }
        for (ChickensRegistryItem chicken : ChickensRegistry.getDisabledItems()) {
            if (chicken.getEntityName().equalsIgnoreCase(entityName)) {
                return chicken;
            }
        }
        return null;
    }

    public boolean isSpecialInfusionItem(ItemStack stack) {
        return stack.is(Items.DRAGON_BREATH) || stack.is(Items.NETHER_STAR);
    }

    public boolean tryStoreSpecialInfusion(ItemStack stack, Player player) {
        if (stack.isEmpty() || !isSpecialInfusionItem(stack)) {
            return false;
        }
        if (liquidTank.getFluidAmount() > 0) {
            return false;
        }
        if (specialInfusion != SpecialInfusion.NONE && specialInfusion != SpecialInfusion.fromItem(stack)) {
            return false;
        }
        SpecialInfusion type = SpecialInfusion.fromItem(stack);
        if (type == SpecialInfusion.NONE) {
            return false;
        }
        specialInfusion = type;
        specialAmount = Math.min(SPECIAL_LIQUID_CAPACITY, specialAmount + SPECIAL_PER_ITEM);
        markLiquidDirty();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack remainder = type.remainder();
            if (!remainder.isEmpty()) {
                if (!player.addItem(remainder.copy())) {
                    player.drop(remainder.copy(), false);
                }
            }
        }
        return true;
    }

    private record OperationPlan(InfusionMode mode, @Nullable ChickensRegistryItem chicken,
                                 SpecialInfusion special, int liquidCost, int chemicalCost, int energyCost) {
        static OperationPlan none() {
            return new OperationPlan(InfusionMode.NONE, null, SpecialInfusion.NONE, 0, 0, 0);
        }
    }

    public enum InfusionMode {
        NONE,
        LIQUID,
        CHEMICAL,
        SPECIAL
    }

    public enum SpecialInfusion {
        NONE(""),
        DRAGON_BREATH("Dragon's Breath"),
        NETHER_STAR("Wither Essence");

        private final String displayName;

        SpecialInfusion(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Nullable
        static SpecialInfusion fromItem(ItemStack stack) {
            if (stack.is(Items.DRAGON_BREATH)) {
                return DRAGON_BREATH;
            }
            if (stack.is(Items.NETHER_STAR)) {
                return NETHER_STAR;
            }
            return NONE;
        }

        ItemStack remainder() {
            return this == DRAGON_BREATH ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY;
        }
    }

    private static final class DousingChemicalHandlerFactory {
        private DousingChemicalHandlerFactory() {
        }

        @Nullable
        static Object create(AvianDousingMachineBlockEntity machine) {
            if (!MekanismChemicalHelper.isChemicalCapabilityAvailable()) {
                return null;
            }
            return java.lang.reflect.Proxy.newProxyInstance(
                    MekanismChemicalHelper.class.getClassLoader(),
                    new Class<?>[] { getHandlerInterface() },
                    new Handler(machine));
        }

        private static Class<?> getHandlerInterface() {
            try {
                return Class.forName("mekanism.api.chemical.IChemicalHandler");
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("IChemicalHandler not present", ex);
            }
        }

        private static final class Handler implements java.lang.reflect.InvocationHandler {
            private final AvianDousingMachineBlockEntity machine;

            private Handler(AvianDousingMachineBlockEntity machine) {
                this.machine = machine;
            }

            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                String name = method.getName();
                return switch (name) {
                    case "getChemicalTanks" -> 1;
                    case "getChemicalInTank" -> machine.getStackCopy();
                    case "setChemicalInTank" -> {
                        machine.setFromStack(args != null && args.length > 1 ? args[1] : null);
                        yield null;
                    }
                    case "getChemicalTankCapacity" -> (long) machine.getChemicalCapacity();
                    case "isValid" -> machine.isTemplateValid(args != null && args.length > 1 ? args[1] : null);
                    case "insertChemical" -> handleInsert(args);
                    case "extractChemical" -> handleExtract(args);
                    case "equals" -> proxy == (args != null && args.length == 1 ? args[0] : null);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "AvianDousingChemicalHandler{" + machine.worldPosition + "}";
                    default -> throw new UnsupportedOperationException("Unsupported chemical handler call: " + name);
                };
            }

            private Object handleInsert(@Nullable Object[] args) {
                if (args == null || args.length == 0) {
                    return MekanismChemicalHelper.emptyStack();
                }
                if (args.length == 3 && args[0] instanceof Integer) {
                    return machine.insertStack(args[1], args[2]);
                }
                if (args.length >= 2) {
                    return machine.insertStack(args[0], args[1]);
                }
                return MekanismChemicalHelper.emptyStack();
            }

            private Object handleExtract(@Nullable Object[] args) {
                if (args == null || args.length == 0) {
                    return MekanismChemicalHelper.emptyStack();
                }
                if (args.length == 3 && args[0] instanceof Integer && args[1] instanceof Long amount) {
                    return machine.extractAmount(amount, args[2]);
                }
                if (args.length == 3 && args[0] instanceof Integer) {
                    return machine.extractStack(args[1], args[2]);
                }
                if (args.length == 2 && args[0] instanceof Long amount) {
                    return machine.extractAmount(amount, args[1]);
                }
                if (args.length >= 2) {
                    return machine.extractStack(args[0], args[1]);
                }
                return MekanismChemicalHelper.emptyStack();
            }
        }
    }

    private final class MachineEnergyStorage extends EnergyStorage {
        MachineEnergyStorage() {
            super(ENERGY_CAPACITY, ENERGY_MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (AvianDousingMachineBlockEntity.this.maxReceive <= 0) {
                return 0;
            }
            int previousMax = this.maxReceive;
            this.maxReceive = AvianDousingMachineBlockEntity.this.maxReceive;
            int received = super.receiveEnergy(amount, simulate);
            this.maxReceive = previousMax;
            if (received > 0 && !simulate) {
                markEnergyDirty();
            }
            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(amount, simulate);
            if (extracted > 0 && !simulate) {
                markEnergyDirty();
            }
            return extracted;
        }

        void setEnergy(int energy) {
            this.energy = Mth.clamp(energy, 0, getMaxEnergyStored());
        }

        boolean consumeEnergy(int amount) {
            if (amount <= 0) {
                return true;
            }
            if (this.energy < amount) {
                return false;
            }
            this.energy -= amount;
            markEnergyDirty();
            return true;
        }
    }
}
