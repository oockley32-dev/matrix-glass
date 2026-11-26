package dev.matrixglass.client;

import dev.matrixglass.MatrixGlass;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/** Full-screen digital rain + green tint + glitch bars while Matrix Vision / Bullet Time is active. */
public final class RainOverlay {
    private static final String CH = "ｦｱｳｴｵｶｷｹｺｻｼｽｾﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ0123456789:.=*+-<>";
    private static final int CELL = 11, TRAIL = 14;

    private static int hash(int a, int b, int c) {
        int h = a * 374761393 + b * 668265263 + c * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;

        MobEffectInstance vis = p.getEffect(MatrixGlass.MATRIX_VISION);
        MobEffectInstance bt = p.getEffect(MatrixGlass.BULLET_TIME);
        if (vis == null && bt == null) return;

        float fade = Math.min(1f, Math.max(vis != null ? vis.getDuration() : 0, bt != null ? bt.getDuration() : 0) / 40f);
        boolean slow = bt != null;
        int rgb = slow ? 0x7DF9FF : 0x00FF41;

        int w = g.guiWidth(), h = g.guiHeight();
        float t = (Util.getMillis() / 50f) * (slow ? 0.35f : 1f);

        // tint + vignette
        g.fill(0, 0, w, h, ((int) (20 * fade) << 24) | rgb);
        g.fillGradient(0, 0, w, h / 4, ((int) (70 * fade) << 24) | 0x004015, 0);
        g.fillGradient(0, h - h / 4, w, h, 0, ((int) (70 * fade) << 24) | 0x004015);

        int rows = h / CELL + 1;
        for (int c = 0; c * CELL < w; c++) {
            int seed = hash(c, 7, 13);
            float speed = 0.25f + ((seed >>> 8) & 255) / 255f * 0.6f;
            int offset = (seed >>> 16) & 1023;
            float head = (t * speed + offset) % (rows + TRAIL);
            for (int i = 0; i < TRAIL; i++) {
                int row = (int) head - i;
                if (row < 0 || row >= rows) continue;
                int a = (int) (255 * (1f - i / (float) TRAIL) * fade * 0.85f);
                if (a < 8) continue;
                int idx = Math.floorMod(hash(c, row, i == 0 ? (int) t : (int) (t * 0.15f)), CH.length());
                int color = (i == 0) ? ((a << 24) | 0xD8FFE0) : ((a << 24) | rgb);
                g.drawString(mc.font, String.valueOf(CH.charAt(idx)), c * CELL, row * CELL, color, false);
            }
        }

        // glitch bars
        int tick = (int) (t * 0.5f);
        if (hash(tick, 3, 9) % 9 == 0) {
            int y = Math.floorMod(hash(tick, 5, 1), h);
            g.fill(0, y, w, y + 2 + Math.floorMod(hash(tick, 2, 2), 8), ((int) (55 * fade) << 24) | rgb);
        }
    }
}
