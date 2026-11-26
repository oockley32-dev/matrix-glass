package dev.matrixglass.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Tumbling glass shard that catches light as it spins. */
public class ShardParticle extends TextureSheetParticle {
    private final float spin;

    protected ShardParticle(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(l, x, y, z);
        pickSprite(sprites);
        lifetime = 25 + random.nextInt(25);
        quadSize = 0.06f + random.nextFloat() * 0.08f;
        gravity = 0.5f;
        friction = 0.94f;
        xd = vx + (random.nextDouble() - .5) * 0.15;
        yd = vy + random.nextDouble() * 0.12;
        zd = vz + (random.nextDouble() - .5) * 0.15;
        spin = (random.nextFloat() - .5f) * 0.6f;
        roll = random.nextFloat() * 6.28f;
        rCol = 0.7f; gCol = 1f; bCol = 1f;
    }

    @Override
    public void tick() {
        oRoll = roll;
        roll += spin;
        super.tick();
        float flash = 0.55f + 0.45f * (float) Math.abs(Math.sin(age * 0.9 + spin * 10));  // glint
        rCol = flash * 0.7f; gCol = flash; bCol = flash;
        alpha = 1f - (age / (float) lifetime);
    }

    @Override protected int getLightColor(float pt) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
            return new ShardParticle(l, x, y, z, vx, vy, vz, sprites);
        }
    }
}
