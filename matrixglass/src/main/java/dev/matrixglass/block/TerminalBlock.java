package dev.matrixglass.block;

import com.mojang.serialization.MapCodec;
import dev.matrixglass.MatrixGlass;
import dev.matrixglass.screen.ChannelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A CRT that marks a spot in the world. Screens on the same channel look out from here. Right-click to change channel. */
public class TerminalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TerminalBlock> CODEC = simpleCodec(TerminalBlock::new);
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 7);
    private static final VoxelShape SHAPE = Block.box(2, 0, 3, 14, 15, 13);

    public TerminalBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CHANNEL, 0));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, CHANNEL); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (level instanceof ServerLevel sl && !old.is(this))
            ChannelData.get(sl.getServer()).set(state.getValue(CHANNEL), GlobalPos.of(sl.dimension(), pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !state.is(newState.getBlock()))
            ChannelData.get(sl.getServer()).remove(GlobalPos.of(sl.dimension(), pos));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) {
            int next = (s.getValue(CHANNEL) + 1) % 8;
            var gp = GlobalPos.of(sl.dimension(), pos);
            ChannelData data = ChannelData.get(sl.getServer());
            data.remove(gp);
            level.setBlock(pos, s.setValue(CHANNEL, next), 3);
            data.set(next, gp);
            level.playSound(null, pos, MatrixGlass.GLITCH, SoundSource.BLOCKS, 0.5f, 1.4f);
            p.displayClientMessage(Component.literal("Terminal channel " + next), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
