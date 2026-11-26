package dev.matrixglass.effect;

import dev.matrixglass.MatrixGlass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/** Matrix Vision is visual only (client draws the code rain). Bullet Time bends the world around you. */
public class MatrixEffect extends MobEffect {
    private final boolean bulletTime;

    public MatrixEffect(MobEffectCategory cat, int color, boolean bulletTime) {
        super(cat, color);
        this.bulletTime = bulletTime;
        if (bulletTime) {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, MatrixGlass.id("bt_speed"), 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.ATTACK_SPEED, MatrixGlass.id("bt_attack"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return bulletTime && duration % 2 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity self, int amplifier) {
        if (!bulletTime || !(self.level() instanceof ServerLevel level)) return true;

        // everything else crawls
        var box = self.getBoundingBox().inflate(12);
        for (LivingEntity o : level.getEntitiesOfClass(LivingEntity.class, box, o -> o != self && !(o instanceof Player))) {
            o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 5, false, false, false));
        }
        // bullets hang in the air
        for (Projectile p : level.getEntitiesOfClass(Projectile.class, box, p -> p.getOwner() != self)) {
            p.setDeltaMovement(p.getDeltaMovement().scale(0.35));
            p.hurtMarked = true;
        }
        // glass-shard afterimage trail
        level.sendParticles(MatrixGlass.GLASS_SHARD, self.getX(), self.getY() + 1.0, self.getZ(), 3, 0.3, 0.6, 0.3, 0.02);
        if (self.tickCount % 10 == 0)
            level.sendParticles(MatrixGlass.CODE_GLYPH, self.getX(), self.getY() + 0.1, self.getZ(), 6, 0.5, 0.1, 0.5, 0.02);
        return true;
    }
}
