package dev.matrixglass.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** A single falling, flickering green glyph. Fullbright. */
public class GlyphParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected GlyphParticle(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(l, x, y, z);
        this.sprites = sprites;
        pickSprite(sprites);
        lifetime = 30 + random.nextInt(40);
        quadSize = 0.10f + random.nextFloat() * 0.07f;
        gravity = 0f;
        hasPhysics = false;
        xd = vx + (random.nextDouble() - .5) * 0.02;
        yd = vy;
        zd = vz + (random.nextDouble() - .5) * 0.02;
        rCol = 0.1f; gCol = 1f; bCol = 0.25f;
    }

    @Override
    public void tick() {
        super.tick();
        if (age % 3 == 0) pickSprite(sprites);                       // glyph flicker
        float life = age / (float) lifetime;
        alpha = 1f - life * life;
        if (age < 3) { rCol = 0.8f; gCol = 1f; bCol = 0.85f; }       // white-hot head
        else { rCol = 0.1f; gCol = 1f - life * 0.4f; bCol = 0.25f; }
    }

    @Override protected int getLightColor(float pt) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            return new GlyphParticle(l, x, y, z, vx, vy, vz, sprites);
        }
    }
}
