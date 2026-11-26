package dev.matrixglass.item;

import dev.matrixglass.MatrixGlass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PillItem extends Item {
    private final boolean red;

    public PillItem(Properties props, boolean red) { super(props); this.red = red; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (level instanceof ServerLevel sl) {
            if (red) {
                p.addEffect(new MobEffectInstance(MatrixGlass.MATRIX_VISION, 20 * 120));
                p.addEffect(new MobEffectInstance(MatrixGlass.BULLET_TIME, 20 * 20));
                level.playSound(null, p.blockPosition(), MatrixGlass.WAKE_UP, SoundSource.PLAYERS, 1f, 1f);
                sl.sendParticles(MatrixGlass.CODE_GLYPH, p.getX(), p.getY() + 1, p.getZ(), 120, 0.7, 1.1, 0.7, 0.06);
                sl.sendParticles(MatrixGlass.GLASS_SHARD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 0.8, 0.5, 0.25);
            } else {
                p.removeAllEffects();
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
                level.playSound(null, p.blockPosition(), MatrixGlass.GLITCH, SoundSource.PLAYERS, 0.8f, 0.7f);
                sl.sendParticles(MatrixGlass.GLASS_SHARD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.8, 0.5, 0.1);
            }
            p.getCooldowns().addCooldown(this, 40);
            stack.consume(1, p);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
