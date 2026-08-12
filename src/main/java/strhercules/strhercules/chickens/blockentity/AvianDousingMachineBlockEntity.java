package strhercules.chickens.blockentity;

import strhercules.chickens.ChemicalEggRegistry;
import strhercules.chickens.ChemicalEggRegistryItem;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.LiquidEggRegistry;
import strhercules.chickens.LiquidEggRegistryItem;
import strhercules.chickens.block.AvianDousingMachineBlock;
import strhercules.chickens.config.ChickensConfigHolder;
import strhercules.chickens.integration.mekanism.MekanismChemicalHelper;
import strhercules.chickens.integration.mekanism.MekanismRadiationCompat;
import strhercules.chickens.item.ChickenItem;
import strhercules.chickens.item.ChickenItemHelper;
import strhercules.chickens.item.ChickensSpawnEggItem;
import strhercules.chickens.item.ChemicalEggItem;
import strhercules.chickens.item.LiquidEggItem;
import strhercules.chickens.menu.AvianDousingMachineMenu;
import strhercules.chickens.recipe.DousingRecipe;
import strhercules.chickens.registry.ModRecipeTypes;
import strhercules.chickens.registry.ModBlockEntities;
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
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.core.registries.BuiltInRegistries;
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
    private static final int TRANSFER_RATE = FluidType.BUCKET_VOLUME * 2;

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
    private ItemStack itemReagent = ItemStack.EMPTY;
    private int itemReagentCount;
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
                MekanismRadiationCompat.tickMachineWarning(level, pos, machine);
            }
        };
    }

    private void tickServer(Level level) {
        if (level.isClientSide) {
            return;
        }

        boolean inventoryChanged = false;
        boolean pulledFluid = pullFluidFromNeighbors(level);
        boolean pulledChemical = pullChemicalFromNeighbors(level);
        boolean pulledEnergy = pullEnergyFromNeighbors(level);
        OperationPlan plan = choosePlan();
        mode = plan.mode();
        if (plan.mode() == InfusionMode.NONE) {
            if (progress != 0) {
                progress = 0;
                inventoryChanged = true;
            }
            updateActiveState(level, hasStoredLiquidOrChemical());
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

        updateActiveState(level, hasStoredLiquidOrChemical());
        if (inventoryChanged || canAdvance || pulledFluid || pulledChemical || pulledEnergy) {
            setChanged();
        }
    }

    private OperationPlan choosePlan() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return OperationPlan.none();
        }
        ChickensRegistryItem inputChicken = getChicken(input);
        ItemStack output = items.get(OUTPUT_SLOT);
        if (!output.isEmpty() && output.getCount() >= output.getMaxStackSize()) {
            return OperationPlan.none();
        }

        boolean hasCustomItemRecipe = hasCustomRecipe(input, DousingRecipe.ReagentType.ITEM);
        DousingRecipe itemRecipe = findAvailableItemRecipe(input);
        if (itemRecipe != null && hasItemReagent(itemRecipe)) {
            OperationPlan plan = planFor(InfusionMode.ITEM, itemRecipe, output, 0);
            if (plan != null) {
                return plan;
            }
        }

        if (inputChicken != null && !hasCustomItemRecipe) {
            if (specialInfusion == SpecialInfusion.DRAGON_BREATH && specialAmount >= SPECIAL_LIQUID_CAPACITY
                    && isChicken(inputChicken, "obsidianChicken")) {
                ChickensRegistryItem dragon = findChickenByName("dragonChicken");
                if (dragon != null && canOutput(output, dragon)) {
                    return chickenPlan(InfusionMode.SPECIAL, dragon, SpecialInfusion.DRAGON_BREATH, 0, null);
                }
            }
            if (specialInfusion == SpecialInfusion.NETHER_STAR && specialAmount >= SPECIAL_LIQUID_CAPACITY
                    && isChicken(inputChicken, "soulSandChicken")) {
                ChickensRegistryItem wither = findChickenByName("witherChicken");
                if (wither != null && canOutput(output, wither)) {
                    return chickenPlan(InfusionMode.SPECIAL, wither, SpecialInfusion.NETHER_STAR, 0, null);
                }
            }
        }

        if (chemicalAmount > 0 && chemicalId != null) {
            DousingRecipe custom = findCustomRecipe(input, DousingRecipe.ReagentType.CHEMICAL, chemicalId);
            if (custom != null && chemicalAmount >= custom.reagentAmount()) {
                OperationPlan plan = planFor(InfusionMode.CHEMICAL, custom, output, custom.reagentAmount());
                if (plan != null) {
                    return plan;
                }
            }
            if (custom == null && inputChicken != null && chemicalAmount >= CHEMICAL_COST) {
                ChickensRegistryItem chicken = resolveChemicalChicken(chemicalId);
                if (chicken != null && canOutput(output, chicken)) {
                    return chickenPlan(InfusionMode.CHEMICAL, chicken, SpecialInfusion.NONE, 0, null);
                }
            }
        }

        FluidStack stored = liquidTank.getFluid();
        if (!stored.isEmpty()) {
            ResourceLocation fluidId = stored.getFluid().builtInRegistryHolder().key().location();
            DousingRecipe custom = findCustomRecipe(input, DousingRecipe.ReagentType.FLUID, fluidId);
            if (custom != null && stored.getAmount() >= custom.reagentAmount()) {
                OperationPlan plan = planFor(InfusionMode.LIQUID, custom, output, custom.reagentAmount());
                if (plan != null) {
                    return plan;
                }
            }
            if (custom == null && inputChicken != null) {
                ChickensRegistryItem chicken = resolveLiquidChicken(stored);
                if (chicken != null) {
                    int liquidCost = chicken.getLiquidDousingCost();
                    if (stored.getAmount() >= liquidCost && canOutput(output, chicken)) {
                        return chickenPlan(InfusionMode.LIQUID, chicken, SpecialInfusion.NONE, liquidCost, null);
                    }
                }
            }
        }

        return OperationPlan.none();
    }

    @Nullable
    private OperationPlan planFor(InfusionMode mode, DousingRecipe recipe, ItemStack output, int liquidCost) {
        ItemStack result = recipe.resultStack();
        if (result.isEmpty() || !canOutput(output, result)) {
            return null;
        }
        return new OperationPlan(mode, recipe.resultChicken(), result, SpecialInfusion.NONE, liquidCost, recipe);
    }

    private OperationPlan chickenPlan(InfusionMode mode, ChickensRegistryItem chicken, SpecialInfusion special,
            int liquidCost, @Nullable DousingRecipe recipe) {
        return new OperationPlan(mode, chicken, ChickensSpawnEggItem.createFor(chicken), special, liquidCost, recipe);
    }

    private boolean hasResourcesFor(OperationPlan plan) {
        if (plan.recipe() != null) {
            if (energyStorage.getEnergyStored() < plan.recipe().energyCost()) {
                return false;
            }
            return switch (plan.recipe().reagentType()) {
                case ITEM -> hasItemReagent(plan.recipe());
                case FLUID -> hasFluidReagent(plan.recipe());
                case CHEMICAL -> chemicalId != null && chemicalId.equals(plan.recipe().reagentId())
                        && chemicalAmount >= plan.recipe().reagentAmount();
            };
        }
        if (plan.mode() == InfusionMode.CHEMICAL) {
            return energyStorage.getEnergyStored() >= CHEMICAL_ENERGY_COST && chemicalAmount >= CHEMICAL_COST;
        }
        if (plan.mode() == InfusionMode.LIQUID) {
            return energyStorage.getEnergyStored() >= LIQUID_ENERGY_COST && liquidTank.getFluidAmount() >= plan.liquidCost();
        }
        if (plan.mode() == InfusionMode.SPECIAL) {
            return energyStorage.getEnergyStored() >= SPECIAL_ENERGY_COST
                    && specialInfusion == plan.special() && specialAmount >= SPECIAL_LIQUID_CAPACITY;
        }
        return false;
    }

    private boolean hasFluidReagent(DousingRecipe recipe) {
        FluidStack stored = liquidTank.getFluid();
        return !stored.isEmpty()
                && stored.getFluid().builtInRegistryHolder().key().location().equals(recipe.reagentId())
                && stored.getAmount() >= recipe.reagentAmount();
    }

    private void completeOperation(OperationPlan plan) {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);
        ItemStack result = plan.result();
        if (result.isEmpty() || !isDousableChicken(input) || !canOutput(output, result)
                || (plan.recipe() != null && (level == null
                        || !plan.recipe().matches(new SingleRecipeInput(input), level)))
                || !hasResourcesFor(plan)) {
            return;
        }

        int energyCost = plan.recipe() != null ? plan.recipe().energyCost()
                : plan.mode() == InfusionMode.CHEMICAL ? CHEMICAL_ENERGY_COST
                : plan.mode() == InfusionMode.SPECIAL ? SPECIAL_ENERGY_COST : LIQUID_ENERGY_COST;
        if (!energyStorage.consumeEnergy(energyCost)) {
            return;
        }

        if (plan.recipe() != null) {
            switch (plan.recipe().reagentType()) {
                case ITEM -> consumeItemReagent(plan.recipe());
                case FLUID -> liquidTank.drain(plan.recipe().reagentAmount(), IFluidHandler.FluidAction.EXECUTE);
                case CHEMICAL -> {
                    chemicalAmount -= plan.recipe().reagentAmount();
                    if (chemicalAmount <= 0) {
                        clearChemical();
                    }
                    invalidateChemicalHandlers();
                    markChemicalDirty();
                }
            }
        } else if (plan.mode() == InfusionMode.CHEMICAL) {
            chemicalAmount -= CHEMICAL_COST;
            if (chemicalAmount <= 0) {
                clearChemical();
            }
            invalidateChemicalHandlers();
            markChemicalDirty();
        } else if (plan.mode() == InfusionMode.LIQUID) {
            liquidTank.drain(plan.liquidCost(), IFluidHandler.FluidAction.EXECUTE);
        } else if (plan.mode() == InfusionMode.SPECIAL) {
            specialAmount = 0;
            specialInfusion = SpecialInfusion.NONE;
            markLiquidDirty();
        }

        if (!output.isEmpty()) {
            output.grow(result.getCount());
        } else {
            items.set(OUTPUT_SLOT, result.copy());
        }

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
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
        if (stack.isEmpty()) {
            return false;
        }
        ChickensRegistryItem chicken = getChicken(stack);
        if (chicken != null) {
            if (chicken.getId() == ChickensRegistry.SMART_CHICKEN_ID) {
                return true;
            }
            if (isChicken(chicken, "obsidianChicken") || isChicken(chicken, "soulSandChicken")) {
                return true;
            }
        }
        return hasCustomRecipeForInput(stack);
    }

    private boolean canOutput(ItemStack output, ChickensRegistryItem chicken) {
        return canOutput(output, ChickensSpawnEggItem.createFor(chicken));
    }

    private boolean canOutput(ItemStack output, ItemStack template) {
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, template)
                && output.getCount() + template.getCount() <= output.getMaxStackSize();
    }

    private boolean hasStoredLiquidOrChemical() {
        return liquidTank.getFluidAmount() > 0 || chemicalAmount > 0;
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

    private boolean pullFluidFromNeighbors(Level level) {
        if (liquidTank.getFluidAmount() >= LIQUID_CAPACITY || specialInfusion != SpecialInfusion.NONE) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            IFluidHandler neighbor = level.getCapability(Capabilities.FluidHandler.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (neighbor == null) {
                continue;
            }
            FluidStack available = neighbor.drain(Math.min(TRANSFER_RATE, LIQUID_CAPACITY - liquidTank.getFluidAmount()),
                    IFluidHandler.FluidAction.SIMULATE);
            int accepted = liquidTank.fill(available, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack drained = neighbor.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            return liquidTank.fill(drained, IFluidHandler.FluidAction.EXECUTE) > 0;
        }
        return false;
    }

    private boolean pullChemicalFromNeighbors(Level level) {
        if (!MekanismChemicalHelper.isChemicalCapabilityAvailable() || chemicalAmount >= CHEMICAL_CAPACITY) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            Object neighbor = MekanismChemicalHelper.getBlockChemicalHandler(level, worldPosition.relative(direction),
                    direction.getOpposite());
            Object available = MekanismChemicalHelper.extractChemical(neighbor,
                    Math.min((long) TRANSFER_RATE, CHEMICAL_CAPACITY - chemicalAmount), true);
            if (MekanismChemicalHelper.isStackEmpty(available) || !isTemplateValid(available)) {
                continue;
            }
            Object remainder = insertStack(available, MekanismChemicalHelper.getAction(false));
            long accepted = MekanismChemicalHelper.getStackAmount(available) - MekanismChemicalHelper.getStackAmount(remainder);
            if (accepted <= 0) {
                continue;
            }
            Object drained = MekanismChemicalHelper.extractChemical(neighbor, accepted, false);
            return !MekanismChemicalHelper.isStackEmpty(drained)
                    && MekanismChemicalHelper.isStackEmpty(insertStack(drained, MekanismChemicalHelper.getAction(true)));
        }
        return false;
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

    @Nullable
    public ResourceLocation getChemicalId() {
        return chemicalId;
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

    public ItemStack getItemReagent() {
        return itemReagent.copy();
    }

    public int getItemReagentCount() {
        return itemReagent.isEmpty() ? 0 : itemReagentCount;
    }

    /** Drops custom item reagents that are held outside the two visible slots. */
    public void dropItemReagent() {
        if (level == null || level.isClientSide || itemReagent.isEmpty() || itemReagentCount <= 0) {
            return;
        }
        ItemStack remaining = itemReagent.copyWithCount(itemReagentCount);
        while (!remaining.isEmpty()) {
            ItemStack drop = remaining.split(Math.min(remaining.getMaxStackSize(), remaining.getCount()));
            Containers.dropItemStack(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D, drop);
        }
        itemReagent = ItemStack.EMPTY;
        itemReagentCount = 0;
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
        if (!itemReagent.isEmpty() && itemReagentCount > 0) {
            tag.put("ItemReagent", itemReagent.save(provider));
            tag.putInt("ItemReagentCount", itemReagentCount);
        }

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
        itemReagent = tag.contains("ItemReagent", Tag.TAG_COMPOUND)
                ? ItemStack.parse(provider, tag.getCompound("ItemReagent")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        itemReagentCount = Math.max(0, tag.getInt("ItemReagentCount"));
        if (itemReagent.isEmpty() || itemReagentCount <= 0) {
            itemReagent = ItemStack.EMPTY;
            itemReagentCount = 0;
        }

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
            if (cachedChicken != null && isDousingAllowed(cachedChicken)) {
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
        if (chicken != null && isDousingAllowed(chicken)) {
            LIQUID_CHICKEN_CACHE.put(fluidId, chicken.getId());
            return chicken;
        }
        return null;
    }

    public int getLiquidCostForStoredFluid() {
        FluidStack stored = liquidTank.getFluid();
        if (stored.isEmpty()) {
            return ChickensRegistryItem.DEFAULT_LIQUID_DOUSING_COST;
        }
        ItemStack input = items.get(INPUT_SLOT);
        ResourceLocation fluidId = stored.getFluid().builtInRegistryHolder().key().location();
        DousingRecipe custom = findCustomRecipe(input, DousingRecipe.ReagentType.FLUID, fluidId);
        if (custom != null) {
            return custom.reagentAmount();
        }
        ChickensRegistryItem chicken = resolveLiquidChicken(stored);
        if (chicken == null) {
            return ChickensRegistryItem.DEFAULT_LIQUID_DOUSING_COST;
        }
        return chicken.getLiquidDousingCost();
    }

    @Nullable
    private ChickensRegistryItem resolveChemicalChicken(ResourceLocation id) {
        Integer cached = CHEMICAL_CHICKEN_CACHE.get(id);
        if (cached != null) {
            ChickensRegistryItem cachedChicken = ChickensRegistry.getByType(cached);
            if (cachedChicken != null && isDousingAllowed(cachedChicken)) {
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
        if (chicken != null && isDousingAllowed(chicken)) {
            CHEMICAL_CHICKEN_CACHE.put(id, chicken.getId());
            return chicken;
        }
        return null;
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

    /**
     * Applies the per-chicken dousing flag, with the global compatibility
     * toggle allowing every liquid and chemical chicken when enabled.
     */
    public static boolean isDousingAllowed(ChickensRegistryItem chicken) {
        if (chicken.isDousingAllowed()) {
            return true;
        }
        if (!ChickensConfigHolder.get().isAllLiquidChemicalDousingEnabled()) {
            return false;
        }
        ItemStack layItem = chicken.createLayItem();
        return layItem.getItem() instanceof LiquidEggItem
                || layItem.getItem() instanceof ChemicalEggItem;
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

    @Nullable
    private DousingRecipe findCustomRecipe(ItemStack input, DousingRecipe.ReagentType reagentType,
            @Nullable ResourceLocation reagentId) {
        if (level == null || input.isEmpty()) {
            return null;
        }
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AVIAN_DOUSING.get())) {
            DousingRecipe recipe = holder.value();
            if (recipe.reagentType() != reagentType || !recipe.matchesInput(input)) {
                continue;
            }
            if (reagentId == null || reagentId.equals(recipe.reagentId())) {
                return recipe;
            }
        }
        return null;
    }

    private boolean hasCustomRecipe(ItemStack input, DousingRecipe.ReagentType reagentType) {
        return findCustomRecipe(input, reagentType, null) != null;
    }

    @Nullable
    private DousingRecipe findAvailableItemRecipe(ItemStack input) {
        if (level == null || input.isEmpty()) {
            return null;
        }
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AVIAN_DOUSING.get())) {
            DousingRecipe recipe = holder.value();
            if (recipe.reagentType() == DousingRecipe.ReagentType.ITEM
                    && recipe.matchesInput(input)
                    && hasItemReagent(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    private DousingRecipe findCustomRecipeForItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return null;
        }
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AVIAN_DOUSING.get())) {
            DousingRecipe recipe = holder.value();
            if (recipe.reagentType() == DousingRecipe.ReagentType.ITEM
                    && recipe.matchesInput(input)
                    && recipe.reagentId().equals(itemId)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean hasCustomRecipeForInput(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return false;
        }
        for (var holder : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.AVIAN_DOUSING.get())) {
            if (holder.value().matchesInput(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasItemReagent(DousingRecipe recipe) {
        ItemStack required = recipe.reagentItem();
        if (required == null) {
            return false;
        }
        if (isSpecialInfusionItem(required)) {
            if (!itemReagent.isEmpty()
                    && ItemStack.isSameItemSameComponents(itemReagent, required)
                    && itemReagentCount >= recipe.reagentAmount()) {
                return true;
            }
            SpecialInfusion requiredInfusion = SpecialInfusion.fromItem(required);
            long requiredAmount = (long) recipe.reagentAmount() * SPECIAL_PER_ITEM;
            return specialInfusion == requiredInfusion && specialAmount >= requiredAmount;
        }
        return !itemReagent.isEmpty()
                && ItemStack.isSameItemSameComponents(itemReagent, required)
                && itemReagentCount >= recipe.reagentAmount();
    }

    private void consumeItemReagent(DousingRecipe recipe) {
        ItemStack required = recipe.reagentItem();
        if (required == null) {
            return;
        }
        if (isSpecialInfusionItem(required)) {
            if (!itemReagent.isEmpty()
                    && ItemStack.isSameItemSameComponents(itemReagent, required)
                    && itemReagentCount >= recipe.reagentAmount()) {
                itemReagentCount = Math.max(0, itemReagentCount - recipe.reagentAmount());
                if (itemReagentCount == 0) {
                    itemReagent = ItemStack.EMPTY;
                }
                markLiquidDirty();
                return;
            }
            specialAmount = Math.max(0, specialAmount - recipe.reagentAmount() * SPECIAL_PER_ITEM);
            if (specialAmount == 0) {
                specialInfusion = SpecialInfusion.NONE;
            }
            markLiquidDirty();
            return;
        }
        itemReagentCount = Math.max(0, itemReagentCount - recipe.reagentAmount());
        if (itemReagentCount == 0) {
            itemReagent = ItemStack.EMPTY;
        }
        markLiquidDirty();
    }

    public boolean isSpecialInfusionItem(ItemStack stack) {
        return stack.is(Items.DRAGON_BREATH) || stack.is(Items.NETHER_STAR);
    }

    public boolean canInsertItemReagent(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        DousingRecipe recipe = findCustomRecipeForItem(stack);
        if (recipe == null) {
            return isSpecialInfusionItem(stack) && canStoreSpecialInfusion(stack);
        }
        if (isSpecialInfusionItem(stack)
                && recipe.reagentAmount() <= SPECIAL_LIQUID_CAPACITY / SPECIAL_PER_ITEM) {
            return canStoreSpecialInfusion(stack);
        }
        return canStoreItemReagent(stack);
    }

    public boolean tryStoreItemReagent(ItemStack stack, Player player) {
        DousingRecipe recipe = findCustomRecipeForItem(stack);
        if (recipe == null && isSpecialInfusionItem(stack)) {
            return tryStoreSpecialInfusion(stack, player);
        }
        if (recipe == null || !canInsertItemReagent(stack)) {
            return false;
        }
        if (isSpecialInfusionItem(stack) && recipe.reagentAmount() <= SPECIAL_LIQUID_CAPACITY / SPECIAL_PER_ITEM) {
            return tryStoreSpecialInfusion(stack, player);
        }
        if (!canStoreItemReagent(stack)) {
            return false;
        }
        if (itemReagent.isEmpty()) {
            itemReagent = stack.copyWithCount(1);
        }
        itemReagentCount++;
        markLiquidDirty();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.is(Items.DRAGON_BREATH)) {
                ItemStack remainder = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.addItem(remainder.copy())) {
                    player.drop(remainder.copy(), false);
                }
            }
        }
        return true;
    }

    public boolean tryStoreSpecialInfusion(ItemStack stack, Player player) {
        if (!canStoreSpecialInfusion(stack)) {
            return false;
        }
        SpecialInfusion type = SpecialInfusion.fromItem(stack);
        specialInfusion = type;
        specialAmount += SPECIAL_PER_ITEM;
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

    private boolean canStoreItemReagent(ItemStack stack) {
        return !stack.isEmpty() && specialInfusion == SpecialInfusion.NONE
                && (itemReagent.isEmpty() || ItemStack.isSameItemSameComponents(itemReagent, stack))
                && itemReagentCount < Integer.MAX_VALUE;
    }

    private boolean canStoreSpecialInfusion(ItemStack stack) {
        if (stack.isEmpty() || !isSpecialInfusionItem(stack) || liquidTank.getFluidAmount() > 0
                || !itemReagent.isEmpty() || specialAmount >= SPECIAL_LIQUID_CAPACITY) {
            return false;
        }
        SpecialInfusion type = SpecialInfusion.fromItem(stack);
        return type != SpecialInfusion.NONE
                && (specialInfusion == SpecialInfusion.NONE || specialInfusion == type);
    }

    private record OperationPlan(InfusionMode mode, @Nullable ChickensRegistryItem chicken, ItemStack result,
                                 SpecialInfusion special, int liquidCost, @Nullable DousingRecipe recipe) {
        static OperationPlan none() {
            return new OperationPlan(InfusionMode.NONE, null, ItemStack.EMPTY, SpecialInfusion.NONE, 0, null);
        }
    }

    public enum InfusionMode {
        NONE,
        LIQUID,
        CHEMICAL,
        ITEM,
        SPECIAL
    }

    public enum SpecialInfusion {
        NONE(""),
        DRAGON_BREATH("Dragon's Breath"),
        NETHER_STAR("Nether Star");

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
