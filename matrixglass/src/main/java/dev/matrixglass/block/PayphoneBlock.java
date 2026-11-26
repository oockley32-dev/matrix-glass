package dev.matrixglass.block;

import com.mojang.serialization.MapCodec;
import dev.matrixglass.MatrixGlass;
import dev.matrixglass.screen.PortalBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The exit. It rings; pick up and a 1x2 window opens in front of it showing your spawn.
 * Step through to wake up there. The line drops after 15 seconds.
 */
public class PayphoneBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PayphoneBlock> CODEC = simpleCodec(PayphoneBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    private static final int RING_TICKS = 50, OPEN_TICKS = 300;

    public PayphoneBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    private static String tag(BlockPos pos) { return "mg_phone_" + pos.asLong(); }
    private static AABB area(BlockPos pos) { return new AABB(pos).inflate(3); }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !state.is(newState.getBlock()))
            PortalBridge.close(sl, area(pos), tag(pos));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (!sl.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, area(pos),
                    e -> e.getTags().contains(tag(pos))).isEmpty()) {
                sp.displayClientMessage(Component.literal("The line is already open"), true);
                return InteractionResult.SUCCESS;
            }
            level.playSound(null, pos, MatrixGlass.PHONE_RING, SoundSource.BLOCKS, 1.2f, 1f);
            sl.sendParticles(MatrixGlass.CODE_GLYPH, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 40, .3, .8, .3, .03);
            MatrixGlass.later(RING_TICKS, () -> openLine(sl, pos, sp));   // let it ring, then pick up
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openLine(ServerLevel sl, BlockPos pos, ServerPlayer sp) {
        BlockState st = sl.getBlockState(pos);
        if (!st.is(MatrixGlass.PAYPHONE)) return;                  // broken while ringing

        var server = sp.server;
        ServerLevel target = server.getLevel(sp.getRespawnDimension());
        BlockPos at = sp.getRespawnPosition();
        if (target == null || at == null) { target = server.overworld(); at = target.getSharedSpawnPos(); }

        // 1x2 doorway one block in front of the phone, facing the player. Viewers stand on the front side.
        Direction f = st.getValue(FACING);
        Vec3 fv = new Vec3(f.getStepX(), 0, f.getStepZ());
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 wv = up.cross(fv);                                    // W x H == F
        Vec3 origin = Vec3.atCenterOf(pos).add(fv).add(0, 0.5, 0); // bottom edge on the floor, 2 blocks tall
        Vec3 dest = new Vec3(at.getX() + .5, at.getY() + 1, at.getZ() + .5);

        PortalBridge.open(sl, tag(pos), origin, wv, up, 1.0, 2.0, target.dimension(), dest);
        glitchBurst(sl, origin.x, origin.y - 1, origin.z);

        MatrixGlass.later(OPEN_TICKS, () -> {
            if (PortalBridge.close(sl, area(pos), tag(pos)) > 0) glitchBurst(sl, origin.x, origin.y - 1, origin.z);
        });
    }

    private static void glitchBurst(ServerLevel l, double x, double y, double z) {
        l.playSound(null, x, y, z, MatrixGlass.GLITCH, SoundSource.PLAYERS, 1f, 1f);
        l.sendParticles(MatrixGlass.CODE_GLYPH, x, y + 1, z, 160, .6, 1.2, .6, .08);
        l.sendParticles(MatrixGlass.GLASS_SHARD, x, y + 1, z, 60, .5, 1.0, .5, .3);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0)
            level.addParticle(MatrixGlass.CODE_GLYPH, pos.getX() + .5 + (r.nextDouble() - .5) * .5, pos.getY() + 1,
                    pos.getZ() + .5 + (r.nextDouble() - .5) * .5, 0, -0.04, 0);
    }
}
