package dev.matrixglass.block;

import com.mojang.serialization.MapCodec;
import dev.matrixglass.MatrixGlass;
import dev.matrixglass.screen.ChannelData;
import dev.matrixglass.screen.PortalBridge;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

/**
 * Faces a wall of Powered Glass. Right-click: toggle the screen. Sneak + right-click: change channel.
 * The glass must be one solid rectangle (max 256 blocks) directly in front of the projector.
 * People look at the glass from the far side, i.e. the side away from the projector.
 */
public class ProjectorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ProjectorBlock> CODEC = simpleCodec(ProjectorBlock::new);
    public static final IntegerProperty CHANNEL = IntegerProperty.create("channel", 0, 7);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public ProjectorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CHANNEL, 0));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, CHANNEL); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());   // projects away from the placer
    }

    private static String tag(BlockPos pos) { return "mg_" + pos.asLong(); }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !state.is(newState.getBlock()))
            PortalBridge.close(sl, new AABB(pos).inflate(64), tag(pos));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl)) return InteractionResult.sidedSuccess(true);

        if (p.isShiftKeyDown()) {
            int next = (s.getValue(CHANNEL) + 1) % 8;
            level.setBlock(pos, s.setValue(CHANNEL, next), 3);
            level.playSound(null, pos, MatrixGlass.GLITCH, SoundSource.BLOCKS, 0.5f, 1.4f);
            p.displayClientMessage(Component.literal("Projector channel " + next), true);
            return InteractionResult.SUCCESS;
        }

        Direction f = s.getValue(FACING);
        List<BlockPos> glass = glassPlane(level, pos.relative(f), f);
        if (glass.isEmpty()) { msg(p, "No Powered Glass in front of the projector"); return InteractionResult.SUCCESS; }

        int x0 = Integer.MAX_VALUE, y0 = x0, z0 = x0, x1 = Integer.MIN_VALUE, y1 = x1, z1 = x1;
        for (BlockPos g : glass) {
            x0 = Math.min(x0, g.getX()); x1 = Math.max(x1, g.getX());
            y0 = Math.min(y0, g.getY()); y1 = Math.max(y1, g.getY());
            z0 = Math.min(z0, g.getZ()); z1 = Math.max(z1, g.getZ());
        }
        AABB area = new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1).inflate(0.5);

        if (PortalBridge.close(sl, area, tag(pos)) > 0) { msg(p, "Screen off"); return InteractionResult.SUCCESS; }

        int volume = (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        if (glass.size() > 256 || volume != glass.size()) { msg(p, "The glass must be one solid rectangle (max 256 blocks)"); return InteractionResult.SUCCESS; }

        int ch = s.getValue(CHANNEL);
        GlobalPos term = ChannelData.get(sl.getServer()).get(ch);
        if (term == null) { msg(p, "No terminal on channel " + ch); return InteractionResult.SUCCESS; }

        // Orientation: portal faces the projector's direction f, so viewers on the far side see it.
        Vec3 fv = new Vec3(f.getStepX(), 0, f.getStepZ());
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 wv = up.cross(fv);                                   // W x H == F
        double width = (f.getAxis() == Direction.Axis.X) ? (z1 - z0 + 1) : (x1 - x0 + 1);
        double height = y1 - y0 + 1;
        Vec3 origin = new Vec3((x0 + x1 + 1) / 2.0, (y0 + y1 + 1) / 2.0, (z0 + z1 + 1) / 2.0);

        PortalBridge.open(sl, tag(pos), origin, wv, up, width, height, term.dimension(), Vec3.atCenterOf(term.pos()));

        level.playSound(null, pos, MatrixGlass.GLITCH, SoundSource.BLOCKS, 1f, 0.9f);
        sl.sendParticles(MatrixGlass.CODE_GLYPH, origin.x, origin.y, origin.z, 120,
                Math.abs(wv.x) * width / 2 + 0.2, height / 2, Math.abs(wv.z) * width / 2 + 0.2, 0.05);
        sl.sendParticles(MatrixGlass.GLASS_SHARD, origin.x, origin.y, origin.z, 40,
                Math.abs(wv.x) * width / 2 + 0.2, height / 2, Math.abs(wv.z) * width / 2 + 0.2, 0.2);
        msg(p, "Screen on (channel " + ch + ")");
        return InteractionResult.SUCCESS;
    }

    private static void msg(Player p, String s) { p.displayClientMessage(Component.literal(s), true); }

    /** Flood-fills connected Powered Glass in the plane perpendicular to the projection direction. */
    private static List<BlockPos> glassPlane(Level level, BlockPos start, Direction f) {
        if (!level.getBlockState(start).is(MatrixGlass.POWERED_GLASS)) return List.of();
        Set<BlockPos> seen = new HashSet<>(); Deque<BlockPos> q = new ArrayDeque<>();
        seen.add(start); q.add(start);
        while (!q.isEmpty() && seen.size() <= 257) {
            BlockPos c = q.poll();
            for (Direction d : Direction.values()) {
                if (d.getAxis() == f.getAxis()) continue;
                BlockPos n = c.relative(d);
                if (seen.contains(n) || !level.getBlockState(n).is(MatrixGlass.POWERED_GLASS)) continue;
                seen.add(n); q.add(n);
            }
        }
        return new ArrayList<>(seen);
    }
}
