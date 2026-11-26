package dev.matrixglass.block;

import com.mojang.serialization.MapCodec;
import dev.matrixglass.MatrixGlass;
import dev.matrixglass.screen.PortalBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Glass that a Projector can turn into a screen. Breaking it kills any screen touching it. */
public class PoweredGlassBlock extends TransparentBlock {
    public static final MapCodec<PoweredGlassBlock> CODEC = simpleCodec(PoweredGlassBlock::new);

    public PoweredGlassBlock(Properties p) { super(p); }

    @Override protected MapCodec<? extends TransparentBlock> codec() { return CODEC; }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !state.is(newState.getBlock()))
            PortalBridge.close(sl, new AABB(pos).inflate(1), null);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(25) == 0)
            level.addParticle(MatrixGlass.CODE_GLYPH, pos.getX() + r.nextDouble(), pos.getY() + 1, pos.getZ() + r.nextDouble(), 0, -0.03, 0);
    }
}
