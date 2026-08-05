package strhercules.chickens.integration.mekanism;

import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.blockentity.AvianDousingMachineBlockEntity;
import strhercules.chickens.entity.ChickensChicken;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Optional Mekanism radiation integration. This class has no Mekanism imports. */
public final class MekanismRadiationCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChickensMekanismRadiation");
    private static final ResourceLocation RADIATION_PARTICLE_ID =
            ResourceLocation.fromNamespaceAndPath("mekanism", "radiation");

    private static final int EMISSION_INTERVAL = 20;
    private static final double EGG_MAGNITUDE = 0.00002D;
    private static final double INVENTORY_MAGNITUDE = 0.00001D;
    private static final double MAX_INVENTORY_MAGNITUDE = 0.00008D;
    private static final int MAX_STACK_CONTRIBUTION = 8;
    private static final Set<BlockEntity> TRACKED_CONTAINERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Object TRACKED_CONTAINERS_LOCK = new Object();

    private static boolean resolutionAttempted;
    private static boolean available;
    private static boolean listenerRegistered;
    @Nullable
    private static Object radiationManager;
    @Nullable
    private static Method isRadiationEnabledMethod;
    @Nullable
    private static Method baselineRadiationMethod;
    @Nullable
    private static Method getRadiationLevelMethod;
    @Nullable
    private static Method radiateLevelMethod;
    @Nullable
    private static Method radiateEntityMethod;
    @Nullable
    private static Method dumpRadiationMethod;
    @Nullable
    private static Method getEntityCapabilityMethod;
    @Nullable
    private static Method setRadiationEntityMethod;
    @Nullable
    private static Object radiationDamageTypeKey;
    @Nullable
    private static Object radiationEntityCapability;

    private static boolean failureReported;
    @Nullable
    private static SimpleParticleType radiationParticle;

    private MekanismRadiationCompat() {
    }

    public static void init() {
        if (!listenerRegistered) {
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, MekanismRadiationCompat::onEntityTick);
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,
                    MekanismRadiationCompat::onEntityInvulnerability);
            NeoForge.EVENT_BUS.addListener(MekanismRadiationCompat::onPlayerTick);
            NeoForge.EVENT_BUS.addListener(MekanismRadiationCompat::onLevelTick);
            NeoForge.EVENT_BUS.addListener(MekanismRadiationCompat::onChunkLoad);
            NeoForge.EVENT_BUS.addListener(MekanismRadiationCompat::onChunkUnload);
            listenerRegistered = true;
        }
    }

    public static void spillDousingRadiation(Level level, BlockPos pos,
            AvianDousingMachineBlockEntity machine) {
        if (!(level instanceof ServerLevel)
                || !isAvailable()
                || !isRadiationEnabled()
                || machine.getChemicalAmount() <= 0
                || machine.getChemicalId() == null
                || dumpRadiationMethod == null) {
            return;
        }
        Object stack = MekanismChemicalHelper.createStack(machine.getChemicalId(), machine.getChemicalAmount());
        if (!MekanismChemicalHelper.isStackEmpty(stack)) {
            invoke(dumpRadiationMethod, level, pos, stack);
        }
    }

    private static synchronized void resolveRadiationApi() {
        if (resolutionAttempted) {
            return;
        }
        resolutionAttempted = true;
        try {
            // Do not initialize IRadiationManager until after mod setup. Its INSTANCE field
            // loads Mekanism's ServiceLoader implementation and can be too early during mod
            // construction.
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager", false,
                    MekanismRadiationCompat.class.getClassLoader());
            Field instance = managerClass.getField("INSTANCE");
            Object manager = instance.get(null);
            Method enabled = managerClass.getMethod("isRadiationEnabled");
            Method baseline = managerClass.getMethod("baselineRadiation");
            Method getLevel = managerClass.getMethod("getRadiationLevel", Level.class, BlockPos.class);
            Method radiateLevel = managerClass.getMethod("radiate", Level.class, BlockPos.class, double.class);
            Method radiateEntity = managerClass.getMethod("radiate", net.minecraft.world.entity.LivingEntity.class,
                    double.class);
            Class<?> chemicalStackClass = Class.forName("mekanism.api.chemical.ChemicalStack", false,
                    MekanismRadiationCompat.class.getClassLoader());
            Method dumpRadiation = managerClass.getMethod("dumpRadiation", Level.class, BlockPos.class,
                    chemicalStackClass);
            Method damageTypeKey = managerClass.getMethod("getRadiationDamageTypeKey");

            Class<?> entityCapabilityClass = Class.forName("net.neoforged.neoforge.capabilities.EntityCapability",
                    false, MekanismRadiationCompat.class.getClassLoader());
            Class<?> capabilitiesClass = Class.forName("mekanism.common.capabilities.Capabilities", false,
                    MekanismRadiationCompat.class.getClassLoader());
            Object radiationCapability = capabilitiesClass.getField("RADIATION_ENTITY").get(null);
            Method getCapability = Entity.class.getMethod("getCapability", entityCapabilityClass);
            Class<?> radiationEntityClass = Class.forName("mekanism.api.radiation.capability.IRadiationEntity",
                    false, MekanismRadiationCompat.class.getClassLoader());
            Method setRadiation = radiationEntityClass.getMethod("set", double.class);
            if (manager == null) {
                throw new IllegalStateException("IRadiationManager.INSTANCE was null");
            }
            radiationManager = manager;
            isRadiationEnabledMethod = enabled;
            baselineRadiationMethod = baseline;
            getRadiationLevelMethod = getLevel;
            radiateLevelMethod = radiateLevel;
            radiateEntityMethod = radiateEntity;
            dumpRadiationMethod = dumpRadiation;
            getEntityCapabilityMethod = getCapability;
            setRadiationEntityMethod = setRadiation;
            radiationEntityCapability = radiationCapability;
            available = true;
            radiationDamageTypeKey = invoke(damageTypeKey);
            LOGGER.info("Mekanism radiation integration enabled");
        } catch (ClassNotFoundException ex) {
            LOGGER.debug("Mekanism radiation API not present; integration disabled");
        } catch (Throwable ex) {
            LOGGER.warn("Mekanism radiation API could not be initialized; integration disabled", ex);
        }
    }

    private static boolean isAvailable() {
        if (!resolutionAttempted) {
            resolveRadiationApi();
        }
        return available;
    }

    public static void tickMachineWarning(Level level, BlockPos pos, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)
                || !isAvailable()
                || Math.floorMod(level.getGameTime(), (long) EMISSION_INTERVAL) != 0
                || !isRadiationEnabled()
                || !RadioactiveContentHelper.hasRadioactiveMachineContents(blockEntity)) {
            return;
        }
        emitSource(serverLevel, pos, EGG_MAGNITUDE);
        SimpleParticleType particle = getRadiationParticle();
        if (particle == null) {
            return;
        }
        int count = 1 + serverLevel.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            serverLevel.sendParticles(particle,
                    pos.getX() + 0.2D + serverLevel.random.nextDouble() * 0.6D,
                    pos.getY() + 0.8D + serverLevel.random.nextDouble() * 0.6D,
                    pos.getZ() + 0.2D + serverLevel.random.nextDouble() * 0.6D,
                    1, 0D, 0D, 0D, 0D);
        }
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !isAvailable() || !isRadiationEnabled()) {
            return;
        }

        if (entity instanceof ChickensChicken chicken) {
            if (RadioactiveContentHelper.isRadioactive(ChickensRegistry.getByType(chicken.getChickenType()))) {
                clearEntityRadiation(chicken);
                if (Math.floorMod(level.getGameTime() + entity.getId(), (long) EMISSION_INTERVAL) == 0) {
                    emitEntityParticles(level, chicken);
                }
            }
        } else if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (RadioactiveContentHelper.isRadioactive(stack)
                    && Math.floorMod(level.getGameTime() + entity.getId(), (long) EMISSION_INTERVAL) == 0) {
                emitSource(level, itemEntity.blockPosition(), Math.min(MAX_STACK_CONTRIBUTION, stack.getCount())
                        * EGG_MAGNITUDE);
                emitEntityParticles(level, itemEntity);
            }
        }
    }

    private static void onEntityInvulnerability(EntityInvulnerabilityCheckEvent event) {
        if (event.isInvulnerable()
                || !(event.getEntity() instanceof ChickensChicken chicken)
                || !RadioactiveContentHelper.isRadioactive(ChickensRegistry.getByType(chicken.getChickenType()))
                || !isRadiationDamage(event.getSource())) {
            return;
        }
        event.setInvulnerable(true);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (Math.floorMod(level.getGameTime() + player.getId(), (long) EMISSION_INTERVAL) == 0) {
            // Also discover containers placed after their chunk was loaded.
            trackChunkContainers(level.getChunkAt(player.blockPosition()));
        }
        if (!isAvailable() || !isRadiationEnabled()) {
            return;
        }
        emitInventoryRadiation(player);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || Math.floorMod(level.getGameTime(), (long) EMISSION_INTERVAL) != 0
                || !isAvailable()
                || !isRadiationEnabled()) {
            return;
        }
        // Work from a snapshot because checking a container can synchronously load or
        // unload chunks through another mod. Those chunk events update the tracked set
        // and would otherwise invalidate its IdentityHashMap iterator.
        for (BlockEntity blockEntity : trackedContainersSnapshot()) {
            if (blockEntity.isRemoved()) {
                untrackContainer(blockEntity);
            } else if (blockEntity.getLevel() == level
                    && !(blockEntity instanceof AvianDousingMachineBlockEntity)) {
                tickMachineWarning(level, blockEntity.getBlockPos(), blockEntity);
            }
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        trackChunkContainers(event.getChunk());
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof LevelChunk chunk) {
            synchronized (TRACKED_CONTAINERS_LOCK) {
                TRACKED_CONTAINERS.removeAll(chunk.getBlockEntities().values());
            }
        }
    }

    private static void trackChunkContainers(ChunkAccess chunk) {
        if (chunk instanceof LevelChunk levelChunk) {
            synchronized (TRACKED_CONTAINERS_LOCK) {
                for (BlockEntity blockEntity : levelChunk.getBlockEntities().values()) {
                    if (blockEntity instanceof Container) {
                        TRACKED_CONTAINERS.add(blockEntity);
                    }
                }
            }
        }
    }

    private static List<BlockEntity> trackedContainersSnapshot() {
        synchronized (TRACKED_CONTAINERS_LOCK) {
            return new ArrayList<>(TRACKED_CONTAINERS);
        }
    }

    private static void untrackContainer(BlockEntity blockEntity) {
        synchronized (TRACKED_CONTAINERS_LOCK) {
            TRACKED_CONTAINERS.remove(blockEntity);
        }
    }

    private static void emitInventoryRadiation(Player player) {
        int radioactiveCount = 0;
        for (ItemStack stack : player.getInventory().items) {
            radioactiveCount += radioactiveCount(stack);
        }
        for (ItemStack stack : player.getArmorSlots()) {
            radioactiveCount += radioactiveCount(stack);
        }
        radioactiveCount += radioactiveCount(player.getItemBySlot(EquipmentSlot.OFFHAND));
        if (radioactiveCount > 0) {
            radiateEntity(player, Math.min(MAX_INVENTORY_MAGNITUDE, radioactiveCount * INVENTORY_MAGNITUDE));
        }
    }

    private static int radioactiveCount(ItemStack stack) {
        return RadioactiveContentHelper.isRadioactive(stack)
                ? Math.min(MAX_STACK_CONTRIBUTION, stack.getCount())
                : 0;
    }

    private static void emitSource(ServerLevel level, BlockPos pos, double targetMagnitude) {
        double current = getRadiationLevel(level, pos);
        if (current >= targetMagnitude) {
            return;
        }
        invoke(radiateLevelMethod, level, pos, targetMagnitude - Math.max(current, baselineRadiation()));
    }

    private static void emitEntityParticles(ServerLevel level, Entity entity) {
        SimpleParticleType particle = getRadiationParticle();
        if (particle == null) {
            return;
        }
        int count = 1 + level.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            level.sendParticles(particle,
                    entity.getX() - 0.3D + level.random.nextDouble() * 0.6D,
                    entity.getY() + 0.2D + level.random.nextDouble() * 0.8D,
                    entity.getZ() - 0.3D + level.random.nextDouble() * 0.6D,
                    1, 0D, 0D, 0D, 0D);
        }
    }

    private static void clearEntityRadiation(net.minecraft.world.entity.LivingEntity entity) {
        if (getEntityCapabilityMethod == null || setRadiationEntityMethod == null || radiationEntityCapability == null) {
            return;
        }
        try {
            Object radiationEntity = getEntityCapabilityMethod.invoke(entity, radiationEntityCapability);
            if (radiationEntity != null) {
                setRadiationEntityMethod.invoke(radiationEntity, baselineRadiation());
            }
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {
            reportFailure(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isRadiationDamage(DamageSource source) {
        if (radiationDamageTypeKey instanceof ResourceKey<?> key) {
            try {
                return source.is((ResourceKey<DamageType>) key);
            } catch (RuntimeException ex) {
                reportFailure(ex);
            }
        }
        String messageId = source.getMsgId();
        return "mekanism.radiation".equals(messageId)
                || "radiation".equals(messageId)
                || "death.attack.mekanism.radiation".equals(messageId);
    }

    private static boolean isRadiationEnabled() {
        Object result = invoke(isRadiationEnabledMethod);
        return result instanceof Boolean enabled && enabled;
    }

    private static double baselineRadiation() {
        Object result = invoke(baselineRadiationMethod);
        return result instanceof Number number ? number.doubleValue() : 0D;
    }

    private static double getRadiationLevel(Level level, BlockPos pos) {
        Object result = invoke(getRadiationLevelMethod, level, pos);
        return result instanceof Number number ? number.doubleValue() : baselineRadiation();
    }

    private static void radiateEntity(net.minecraft.world.entity.LivingEntity entity, double magnitude) {
        if (magnitude > 0D) {
            invoke(radiateEntityMethod, entity, magnitude);
        }
    }

    @Nullable
    private static SimpleParticleType getRadiationParticle() {
        if (radiationParticle == null) {
            if (BuiltInRegistries.PARTICLE_TYPE.get(RADIATION_PARTICLE_ID) instanceof SimpleParticleType particle) {
                radiationParticle = particle;
            }
        }
        return radiationParticle;
    }

    @Nullable
    private static Object invoke(@Nullable Method method, Object... args) {
        if (method == null || radiationManager == null || !available) {
            return null;
        }
        try {
            return method.invoke(radiationManager, args);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {
            reportFailure(ex);
            return null;
        }
    }

    private static void reportFailure(Throwable error) {
        if (!failureReported) {
            failureReported = true;
            available = false;
            LOGGER.warn("Mekanism radiation bridge failed; Modern Chickens radiation integration disabled.", error);
        }
    }
}
