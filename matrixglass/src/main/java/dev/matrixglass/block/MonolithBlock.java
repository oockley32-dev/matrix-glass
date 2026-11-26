package dev.matrixglass.block;

import com.mojang.serialization.MapCodec;
import dev.matrixglass.MatrixGlass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Black glass slab streaming code. Touch it to see the Matrix for 30s. */
public class MonolithBlock extends Block {
    public static final MapCodec<MonolithBlock> CODEC = simpleCodec(MonolithBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);

    public MonolithBlock(Properties p) { super(p); }

    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            p.addEffect(new MobEffectInstance(MatrixGlass.MATRIX_VISION, 20 * 30));
            level.playSound(null, pos, MatrixGlass.GLITCH, SoundSource.BLOCKS, 1f, 1.2f);
            sl.sendParticles(MatrixGlass.GLASS_SHARD, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 25, .3, .6, .3, .15);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(2) == 0)
            level.addParticle(MatrixGlass.CODE_GLYPH, pos.getX() + .2 + r.nextDouble() * .6, pos.getY() + 1.0,
                    pos.getZ() + .2 + r.nextDouble() * .6, 0, -0.03, 0);
        if (r.nextInt(30) == 0)
            level.addParticle(MatrixGlass.GLASS_SHARD, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                    (r.nextDouble() - .5) * .1, .05, (r.nextDouble() - .5) * .1);
        if (r.nextInt(120) == 0)
            level.playLocalSound(pos, MatrixGlass.MONOLITH_HUM, SoundSource.BLOCKS, 0.5f, 1f, false);
    }
}
