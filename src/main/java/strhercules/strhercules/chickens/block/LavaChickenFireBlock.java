package strhercules.chickens.block;

import com.mojang.serialization.MapCodec;
import strhercules.chickens.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;

/**
 * A short-lived fire visual. It is a normal block rather than vanilla fire,
 * so it has no spread, collision, or entity-damage behaviour.
 */
public final class LavaChickenFireBlock extends Block {
    public static final MapCodec<LavaChickenFireBlock> CODEC = simpleCodec(LavaChickenFireBlock::new);
    private static final int LIFETIME_TICKS = 30;

    public LavaChickenFireBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .noCollission()
                .noOcclusion()
                .replaceable()
                .strength(0.0F)
                .sound(SoundType.WOOL));
    }

    public LavaChickenFireBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<LavaChickenFireBlock> codec() {
        return CODEC;
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, LIFETIME_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean occupiedByBurningPlayer = !level.getEntitiesOfClass(Player.class,
                new AABB(pos), player -> player.hasEffect(ModEffects.BURNING)).isEmpty();
        if (occupiedByBurningPlayer) {
            level.scheduleTick(pos, this, LIFETIME_TICKS);
        } else if (level.getBlockState(pos).is(this)) {
            level.removeBlock(pos, false);
        }
    }
}
