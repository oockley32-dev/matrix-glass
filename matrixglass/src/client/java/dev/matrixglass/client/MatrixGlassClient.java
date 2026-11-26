package dev.matrixglass.client;

import dev.matrixglass.MatrixGlass;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.renderer.RenderType;

public class MatrixGlassClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(MatrixGlass.CODE_GLYPH, GlyphParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(MatrixGlass.GLASS_SHARD, ShardParticle.Factory::new);
        BlockRenderLayerMap.INSTANCE.putBlock(MatrixGlass.GLASS_MONOLITH, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(MatrixGlass.PAYPHONE, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(MatrixGlass.POWERED_GLASS, RenderType.translucent());
        BlockRenderLayerMap.INSTANCE.putBlock(MatrixGlass.TERMINAL, RenderType.cutout());
        HudRenderCallback.EVENT.register(RainOverlay::render);
    }
}
