package dev.matrixglass;

import dev.matrixglass.block.MonolithBlock;
import dev.matrixglass.block.PayphoneBlock;
import dev.matrixglass.block.PoweredGlassBlock;
import dev.matrixglass.block.ProjectorBlock;
import dev.matrixglass.block.TerminalBlock;
import dev.matrixglass.effect.MatrixEffect;
import dev.matrixglass.item.PillItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

public class MatrixGlass implements ModInitializer {
    public static final String ID = "matrixglass";
    public static ResourceLocation id(String p) { return ResourceLocation.fromNamespaceAndPath(ID, p); }

    // ---- particles ----
    public static final SimpleParticleType CODE_GLYPH = FabricParticleTypes.simple();
    public static final SimpleParticleType GLASS_SHARD = FabricParticleTypes.simple();

    // ---- sounds ----
    public static final SoundEvent PHONE_RING = sound("phone_ring");
    public static final SoundEvent GLITCH = sound("glitch");
    public static final SoundEvent WAKE_UP = sound("wake_up");
    public static final SoundEvent BULLET_TIME_SFX = sound("bullet_time");
    public static final SoundEvent MONOLITH_HUM = sound("monolith_hum");

    // ---- effects ----
    public static final Holder<MobEffect> MATRIX_VISION = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            id("matrix_vision"), new MatrixEffect(MobEffectCategory.BENEFICIAL, 0x00FF41, false));
    public static final Holder<MobEffect> BULLET_TIME = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            id("bullet_time"), new MatrixEffect(MobEffectCategory.BENEFICIAL, 0x7DF9FF, true));

    // ---- blocks ----
    public static final Block GLASS_MONOLITH = block("glass_monolith", new MonolithBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).strength(3f, 6f).noOcclusion().lightLevel(s -> 8).sound(SoundType.AMETHYST_CLUSTER)));
    public static final Block PAYPHONE = block("payphone", new PayphoneBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).strength(2.5f).noOcclusion().lightLevel(s -> 5).sound(SoundType.METAL)));

    public static final Block POWERED_GLASS = block("powered_glass", new PoweredGlassBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN).strength(0.6f).noOcclusion().lightLevel(s -> 3).sound(SoundType.GLASS)
            .isValidSpawn((s, g, p, e) -> false).isRedstoneConductor((s, g, p) -> false).isSuffocating((s, g, p) -> false).isViewBlocking((s, g, p) -> false)));
    public static final Block PROJECTOR = block("projector", new ProjectorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(2.5f).noOcclusion().lightLevel(s -> 6).sound(SoundType.METAL)));
    public static final Block TERMINAL = block("terminal", new TerminalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).strength(2.5f).noOcclusion().lightLevel(s -> 7).sound(SoundType.METAL)));

    // ---- items ----
    public static final Item RED_PILL = item("red_pill", new PillItem(new Item.Properties().stacksTo(16), true));
    public static final Item BLUE_PILL = item("blue_pill", new PillItem(new Item.Properties().stacksTo(16), false));

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now = 0;
    private record Task(long at, Runnable run) {}

    /** Run something N server ticks from now. */
    public static void later(int ticks, Runnable r) { TASKS.add(new Task(now + ticks, r)); }

    private static SoundEvent sound(String n) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id(n), SoundEvent.createVariableRangeEvent(id(n)));
    }
    private static Block block(String n, Block b) {
        Registry.register(BuiltInRegistries.ITEM, id(n), new BlockItem(b, new Item.Properties()));
        return Registry.register(BuiltInRegistries.BLOCK, id(n), b);
    }
    private static Item item(String n, Item i) { return Registry.register(BuiltInRegistries.ITEM, id(n), i); }

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("code_glyph"), CODE_GLYPH);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("glass_shard"), GLASS_SHARD);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("main"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.matrixglass"))
                .icon(() -> new ItemStack(RED_PILL))
                .displayItems((ctx, out) -> {
                    out.accept(RED_PILL); out.accept(BLUE_PILL);
                    out.accept(GLASS_MONOLITH); out.accept(PAYPHONE);
                    out.accept(TERMINAL); out.accept(PROJECTOR); out.accept(POWERED_GLASS);
                }).build());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            now++;
            List<Task> due = TASKS.stream().filter(t -> t.at <= now).toList();
            TASKS.removeAll(due);
            due.forEach(t -> t.run.run());
        });
    }
}
