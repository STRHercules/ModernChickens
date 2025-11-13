package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PiglinWallSkullBlock extends WallSkullBlock {
   public static final MapCodec<PiglinWallSkullBlock> CODEC = simpleCodec(PiglinWallSkullBlock::new);
   private static final Map<Direction, VoxelShape> AABBS;

   public MapCodec<PiglinWallSkullBlock> codec() {
      return CODEC;
   }

   public PiglinWallSkullBlock(BlockBehaviour.Properties var1) {
      super(SkullBlock.Types.PIGLIN, var1);
   }

   protected VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return (VoxelShape)AABBS.get(var1.getValue(FACING));
   }

   static {
      AABBS = Maps.immutableEnumMap(Map.of(Direction.NORTH, Block.box(3.0D, 4.0D, 8.0D, 13.0D, 12.0D, 16.0D), Direction.SOUTH, Block.box(3.0D, 4.0D, 0.0D, 13.0D, 12.0D, 8.0D), Direction.EAST, Block.box(0.0D, 4.0D, 3.0D, 8.0D, 12.0D, 13.0D), Direction.WEST, Block.box(8.0D, 4.0D, 3.0D, 16.0D, 12.0D, 13.0D)));
   }
}
