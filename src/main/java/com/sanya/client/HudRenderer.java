package com.sanya.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game HUD in the EnoughVisuals style: dark capsule panels, accent-coloured units,
 * thin dividers, rounded item slots. (EV draws with custom fonts/icons and blur; this is a
 * 1.21.11 re-drawing with plain fills and vanilla text.)
 */
public final class HudRenderer {
    private HudRenderer() { }

    // palette from EnoughVisuals' HudDesign
    private static final int PANEL = 0xF0121214;
    private static final int PANEL_EDGE = 0xFF232327;
    private static final int SLOT = 0xFF121214;
    private static final int SLOT_EDGE = 0xFF2A2A2E;
    private static final int DIVIDER = 0xFF34343A;
    private static final int ROW_MARK = 0xFF29292C;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF8D8D94;
    private static final int TRACK = 0xFF2A2A2E;
    private static final int DANGER = 0xFFED2E2E;

    public static int hudPalette = 3; // Lime, like the menu

    private static double lastX, lastZ;
    private static boolean hasLast;
    private static float speedBps;
    private static float healthAnim = -1f;

    private static int accent() {
        return 0xFF000000 | SanyaClient.palA(hudPalette);
    }

    private static int tint(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------ per-tick state

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            hasLast = false;
            speedBps = 0f;
            return;
        }
        double x = client.player.getX(), z = client.player.getZ();
        if (hasLast) {
            float raw = (float) (Math.hypot(x - lastX, z - lastZ) * 20.0);
            speedBps += (raw - speedBps) * 0.3f;
        }
        lastX = x;
        lastZ = z;
        hasLast = true;
    }

    private static int ping(MinecraftClient mc) {
        try {
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) return Math.max(0, entry.getLatency());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    // ------------------------------------------------------------ drawing primitives

    private static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    private static void panel(DrawContext c, int x, int y, int w, int h, int r) {
        rrect(c, x, y, w, h, r, PANEL_EDGE);
        rrect(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), PANEL);
    }

    private static void slot(DrawContext c, int x, int y, int size) {
        rrect(c, x, y, size, size, 7, SLOT_EDGE);
        rrect(c, x + 1, y + 1, size - 2, size - 2, 6, SLOT);
    }

    private static void text(DrawContext c, MinecraftClient mc, String s, float x, float y, int color, float sc) {
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(x, y);
        c.getMatrices().scale(sc, sc);
        c.drawTextWithShadow(mc.textRenderer, UiFont.regular(s), 0, 0, color);
        c.getMatrices().popMatrix();
    }

    private static int tw(MinecraftClient mc, String s, float sc) {
        return Math.round(mc.textRenderer.getWidth(UiFont.regular(s)) * sc);
    }

    private static void divider(DrawContext c, int x, int y, int h) {
        c.fill(x, y, x + 1, y + h, DIVIDER);
    }

    private static void item(DrawContext c, ItemStack stack, float x, float y, float sc) {
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(x, y);
        c.getMatrices().scale(sc, sc);
        c.drawItem(stack, 0, 0);
        c.getMatrices().popMatrix();
    }

    /** A progress outline around a rectangle, clockwise from the top-left corner. */
    private static void outline(DrawContext c, int x, int y, int w, int h, int t, float progress, int color) {
        float remaining = progress * 2f * (w + h);
        int run = Math.min(w, Math.round(remaining));
        if (run > 0) c.fill(x, y, x + run, y + t, color);
        remaining -= w;
        if (remaining <= 0) return;
        run = Math.min(h, Math.round(remaining));
        if (run > 0) c.fill(x + w - t, y, x + w, y + run, color);
        remaining -= h;
        if (remaining <= 0) return;
        run = Math.min(w, Math.round(remaining));
        if (run > 0) c.fill(x + w - run, y + h - t, x + w, y + h, color);
        remaining -= w;
        if (remaining <= 0) return;
        run = Math.min(h, Math.round(remaining));
        if (run > 0) c.fill(x, y + h - run, x + t, y + h, color);
    }

    // ------------------------------------------------------------ entry point

    public static void render(DrawContext c, MinecraftClient mc, float tickDelta) {
        if (mc.player == null) return;
        if (SanyaClient.hudEnabled) watermark(c, mc);
        if (SanyaClient.armorHudEnabled) armor(c, mc);
        if (SanyaClient.cooldownHudEnabled) cooldown(c, mc, tickDelta);
        if (SanyaClient.keystrokesEnabled) keystrokes(c, mc);

        int rightY = 8;
        PlayerEntity target = SanyaClient.currentTarget();
        if (SanyaClient.targetHudEnabled && target != null && !target.isRemoved()) {
            rightY += targetHud(c, mc, target, rightY) + 6;
        } else {
            healthAnim = -1f;
        }
        if (SanyaClient.effectsHudEnabled) effects(c, mc, rightY);
    }

    // ------------------------------------------------------------ watermark (top centre)

    private record Seg(String icon, String value, String unit) { }

    private static int segWidth(MinecraftClient mc, Seg s, float sc) {
        int w = tw(mc, s.value(), sc);
        if (s.icon() != null) w += 11;
        if (s.unit() != null) w += 2 + tw(mc, s.unit(), sc);
        return w;
    }

    private static int drawSeg(DrawContext c, MinecraftClient mc, Seg s, int x, int y, float sc) {
        float ty = y + (18 - 8 * sc) / 2f + 0.5f;
        if (s.icon() != null) {
            text(c, mc, s.icon(), x, ty, TEXT_DIM, sc);
            x += 11;
        }
        text(c, mc, s.value(), x, ty, TEXT, sc);
        x += tw(mc, s.value(), sc);
        if (s.unit() != null) {
            text(c, mc, s.unit(), x + 2, ty, accent(), sc);
            x += 2 + tw(mc, s.unit(), sc);
        }
        return x;
    }

    private static void watermark(DrawContext c, MinecraftClient mc) {
        float sc = 0.85f;
        int h = 18, y = 4;
        String user = mc.player.getName().getString();
        String server = mc.isInSingleplayer() ? "Singleplayer"
                : (mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Local");
        if (server.length() > 22) server = server.substring(0, 22);

        List<Seg> left = new ArrayList<>();
        left.add(new Seg("◌", user, null));
        left.add(new Seg("▣", server, null));
        List<Seg> right = new ArrayList<>();
        right.add(new Seg(null, String.valueOf(mc.getCurrentFps()), "FPS"));
        right.add(new Seg(null, String.valueOf(ping(mc)), "MS"));
        if (SanyaClient.cpsEnabled) right.add(new Seg(null, String.valueOf(SanyaClient.currentCps()), "CPS"));

        int gap = 9, logo = 18, pad = 8;
        int total = logo + pad;
        for (Seg s : left) total += segWidth(mc, s, sc) + gap;
        total += 1 + gap;
        for (int i = 0; i < right.size(); i++) total += segWidth(mc, right.get(i), sc) + (i + 1 < right.size() ? gap : 0);
        total += pad;

        int sw = c.getScaledWindowWidth();
        int x = (sw - total) / 2;
        panel(c, x, y, total, h, 9);
        rrect(c, x + 1, y + 1, logo - 1, h - 2, 8, 0xFF1B1B1F);
        text(c, mc, "S", x + 1 + (logo - 1 - tw(mc, "S", 1.0f)) / 2f, y + 5.5f, accent(), 1.0f);

        int cx = x + logo + pad;
        for (Seg s : left) cx = drawSeg(c, mc, s, cx, y, sc) + gap;
        divider(c, cx - gap / 2 - 1, y + 4, h - 8);
        cx += 1 + gap / 2;
        for (int i = 0; i < right.size(); i++) {
            cx = drawSeg(c, mc, right.get(i), cx, y, sc);
            if (i + 1 < right.size()) cx += gap;
        }

        if (SanyaClient.coordinatesEnabled) {
            String coords = String.format("x: %d  y: %d  z: %d", (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ()));
            String speed = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0f, speedBps));
            int w2 = 8 + 11 + tw(mc, coords, sc) + gap + 1 + gap + tw(mc, speed, sc) + 2 + tw(mc, "BPS", sc) + 8;
            int y2 = y + h + 4, x2 = (sw - w2) / 2;
            panel(c, x2, y2, w2, h, 9);
            float ty = y2 + (h - 8 * sc) / 2f + 0.5f;
            text(c, mc, "⌖", x2 + 8, ty, TEXT_DIM, sc);
            text(c, mc, coords, x2 + 8 + 11, ty, TEXT, sc);
            int dx = x2 + 8 + 11 + tw(mc, coords, sc) + gap;
            divider(c, dx, y2 + 4, h - 8);
            text(c, mc, speed, dx + 1 + gap, ty, TEXT, sc);
            text(c, mc, "BPS", dx + 1 + gap + tw(mc, speed, sc) + 2, ty, accent(), sc);
        }
    }

    // ------------------------------------------------------------ armor slots (left of the hotbar)

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static void armor(DrawContext c, MinecraftClient mc) {
        int size = 22, gap = 3;
        int total = ARMOR.length * size + (ARMOR.length - 1) * gap;
        int x0 = c.getScaledWindowWidth() / 2 - 91 - 8 - total;
        int y0 = c.getScaledWindowHeight() - size - 4;
        for (int i = 0; i < ARMOR.length; i++) {
            int sx = x0 + i * (size + gap);
            slot(c, sx, y0, size);
            ItemStack stack = mc.player.getEquippedStack(ARMOR[i]);
            if (stack.isEmpty()) continue;
            item(c, stack, sx + 3, sy(y0), 1.0f);
            if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                float remaining = 1f - stack.getDamage() / (float) stack.getMaxDamage();
                int bx = sx + 3, by = y0 + 19, bw = 16;
                c.fill(bx, by, bx + bw, by + 2, TRACK);
                c.fill(bx, by, bx + Math.round(bw * Math.max(0f, remaining)), by + 2, remaining < 0.22f ? DANGER : accent());
            }
        }
    }

    private static int sy(int y0) {
        return y0 + 2;
    }

    // ------------------------------------------------------------ item cooldown (bottom centre)

    private static void cooldown(DrawContext c, MinecraftClient mc, float tickDelta) {
        float cd = mc.player.getItemCooldownManager().getCooldownProgress(mc.player.getMainHandStack(), tickDelta);
        if (cd <= 0.001f) return;
        float sc = 0.85f;
        int w = 104, h = 20;
        int x = (c.getScaledWindowWidth() - w) / 2, y = c.getScaledWindowHeight() - 62;
        panel(c, x, y, w, h, 10);
        text(c, mc, "◔", x + 8, y + 6, accent(), sc);
        text(c, mc, "Cooldown", x + 20, y + 6, TEXT, sc);
        String pct = Math.round(cd * 100) + "%";
        text(c, mc, pct, x + w - 8 - tw(mc, pct, sc), y + 6, accent(), sc);
        c.fill(x + 10, y + h - 4, x + w - 10, y + h - 2, TRACK);
        c.fill(x + 10, y + h - 4, x + 10 + Math.round((w - 20) * (1.0f - cd)), y + h - 2, accent());
    }

    // ------------------------------------------------------------ keystrokes (bottom right)

    private static void cap(DrawContext c, MinecraftClient mc, String label, KeyBinding binding, int x, int y, int w, int h) {
        boolean down = binding.isPressed();
        rrect(c, x, y, w, h, 4, down ? tint(accent(), 0xE0) : tint(accent(), 0x33));
        float sc = label.length() > 1 ? 0.7f : 0.9f;
        text(c, mc, label, x + (w - tw(mc, label, sc)) / 2f, y + (h - 8 * sc) / 2f + 0.5f,
                down ? 0xFF0B0B0D : TEXT, sc);
    }

    private static void keystrokes(DrawContext c, MinecraftClient mc) {
        int k = 16, g = 2;
        int bx = c.getScaledWindowWidth() - 8 - (k * 3 + g * 2);
        int by = c.getScaledWindowHeight() - 8 - (k * 2 + g + 14 + g);
        cap(c, mc, "W", mc.options.forwardKey, bx + k + g, by, k, k);
        cap(c, mc, "A", mc.options.leftKey, bx, by + k + g, k, k);
        cap(c, mc, "S", mc.options.backKey, bx + k + g, by + k + g, k, k);
        cap(c, mc, "D", mc.options.rightKey, bx + 2 * (k + g), by + k + g, k, k);
        cap(c, mc, "JUMP", mc.options.jumpKey, bx, by + 2 * (k + g), k * 3 + g * 2, 14);
    }

    // ------------------------------------------------------------ target HUD (top right)

    private static int targetHud(DrawContext c, MinecraftClient mc, PlayerEntity target, int y) {
        float maxHp = Math.max(1f, target.getMaxHealth());
        float hp = Math.max(0f, target.getHealth());
        healthAnim = healthAnim < 0 ? hp : healthAnim + (hp - healthAnim) * 0.18f;
        float sc = 0.85f;

        String name = target.getName().getString();
        if (name.length() > 14) name = name.substring(0, 14);
        String hpText = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0f, healthAnim));
        int nameW = Math.max(tw(mc, name, sc) + 11, tw(mc, hpText, sc) + 11 + 2 + tw(mc, "HP", sc));
        int cell = 11;
        int w = 8 + 26 + 6 + 1 + 6 + nameW + 6 + 1 + 6 + cell * 4 + 6, h = 38;
        int x = c.getScaledWindowWidth() - w - 8;
        panel(c, x, y, w, h, 13);

        int hx = x + 8, hy = y + 6;
        rrect(c, hx + 1, hy + 1, 24, 24, 7, 0xFF1B1B1F);
        String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        text(c, mc, initial, hx + (26 - tw(mc, initial, 1.2f)) / 2f, hy + 8.5f, TEXT, 1.2f);
        outline(c, hx, hy, 26, 26, 2, 1f, ROW_MARK);
        outline(c, hx, hy, 26, 26, 2, Math.max(0f, Math.min(1f, healthAnim / maxHp)), accent());

        int dx = hx + 26 + 6;
        divider(c, dx, y + 6, h - 12);
        int tx = dx + 7;
        text(c, mc, "◌", tx, y + 9, TEXT_DIM, sc);
        text(c, mc, name, tx + 11, y + 9, TEXT, sc);
        text(c, mc, "♥", tx, y + 22, accent(), sc);
        text(c, mc, hpText, tx + 11, y + 22, TEXT, sc);
        text(c, mc, "HP", tx + 11 + tw(mc, hpText, sc) + 2, y + 22, accent(), sc);

        int d2 = tx + nameW + 6;
        divider(c, d2, y + 6, h - 12);
        int ex = d2 + 7, ey = y + 9;
        ItemStack[] cells = new ItemStack[6];
        cells[0] = target.getMainHandStack();
        cells[1] = target.getOffHandStack();
        for (int i = 0; i < 4; i++) cells[2 + i] = target.getEquippedStack(ARMOR[i]);
        for (int i = 0; i < 6; i++) {
            int col = i < 2 ? i : i - 2, row = i < 2 ? 0 : 1;
            int cx = ex + col * cell, cy = ey + row * 11;
            ItemStack st = cells[i];
            if (st == null || st.isEmpty()) text(c, mc, "×", cx + 2, cy + 1, 0xFF4D4D52, 0.8f);
            else item(c, st, cx, cy, 0.62f);
        }
        return h;
    }

    // ------------------------------------------------------------ potion effects (right column)

    private static void effects(DrawContext c, MinecraftClient mc, int y) {
        List<StatusEffectInstance> list = new ArrayList<>(mc.player.getStatusEffects());
        if (list.isEmpty()) return;
        list.sort((a, b) -> Integer.compare(a.isInfinite() ? Integer.MAX_VALUE : a.getDuration(),
                b.isInfinite() ? Integer.MAX_VALUE : b.getDuration()));
        float sc = 0.8f;
        int rows = Math.min(8, list.size());
        int w = 120, headerH = 18, rowH = 15;
        int h = headerH + rows * rowH + 4;
        int x = c.getScaledWindowWidth() - w - 8;
        panel(c, x, y, w, h, 12);
        text(c, mc, "◈", x + 9, y + 5, accent(), 0.95f);
        text(c, mc, "Ефекти", x + 22, y + 5.5f, TEXT, 0.9f);

        for (int i = 0; i < rows; i++) {
            StatusEffectInstance e = list.get(i);
            int ry = y + headerH + i * rowH;
            boolean harmful = e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
            int seconds = e.isInfinite() ? -1 : e.getDuration() / 20;
            boolean urgent = seconds >= 0 && seconds < 10;
            String time = seconds < 0 ? "∞" : String.format("%d:%02d", seconds / 60, seconds % 60);
            int semantic = urgent || harmful ? DANGER : accent();
            int pillW = Math.max(30, tw(mc, time, sc) + 10);
            int px = x + w - 8 - pillW;
            rrect(c, px, ry + 1, pillW, 12, 5, tint(semantic, 0x33));
            text(c, mc, time, px + (pillW - tw(mc, time, sc)) / 2f, ry + 3.5f, semantic, sc);
            String name = e.getEffectType().value().getName().getString();
            int maxW = px - (x + 10) - 6;
            while (name.length() > 1 && tw(mc, name, sc) > maxW) name = name.substring(0, name.length() - 1);
            text(c, mc, name, x + 10, ry + 3.5f, harmful ? DANGER : TEXT, sc);
        }
    }
}
