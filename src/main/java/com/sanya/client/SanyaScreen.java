package com.sanya.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** SANЯ CLIENT menu, drawn by hand to match the HTML mockup (virtual size 760x430). */
public class SanyaScreen extends Screen {
    // ---- layout (virtual units) ----
    private static final int PW = 760, PH = 430, SIDE = 140, TOP = 44;
    private static final int MX = 160, MW = 580, GRID_Y = 146, LW = 333, LH = 252, RX = 503, RW = 237;

    // ---- palette ----
    private static final int BG = 0xFF121511, LINE = 0xFF2B3029, PANEL = 0xFF171A16;
    private static final int MUTED = 0xFF828A7E, TEXT = 0xFFEDF1E7, LIME = 0xFFC7F36B;

    private static final String[] NAV = {"Огляд", "PvP HUD", "Візуал", "Косметика", "Небо", "Оптимізація", "Налаштування"};
    private static final String[] NAV_ICON = {"◈", "⚔", "▧", "✦", "☾", "◉", "⌘"};
    private static final String[] CRUMB = {"DASHBOARD", "PVP HUD", "VISUALS", "COSMETICS", "SHADER SKY", "OPTIMIZATION", "SETTINGS", "PROFILE"};
    private static final String[][] HERO = {
            {"Привіт, ", "Саня.", "Твій блоковий хаос тепер під контролем."},
            {"Бойовий ", "HUD.", "Усе потрібне для бою — на одному екрані."},
            {"Чистий ", "візуал.", "Те, що ти бачиш у грі, під твоїм контролем."},
            {"Твоя ", "косметика.", "Капелюх, крила, супутник і аура — видно лише тобі."},
            {"Нове ", "небо.", "Shader Sky, час і погода — лише візуально, на твоєму клієнті."},
            {"Більше ", "FPS.", "Менше зайвого — плавніша гра."},
            {"Твої ", "налаштування.", "Пропорції екрана та стиль Target ESP."},
            {"Мій ", "профіль.", "Інформація про клієнт і збірку."}
    };
    private static final String[] TABS = {"Огляд", "Модулі", "HUD редактор", "Профілі"};
    private static final int PROFILE_PAGE = 7;

    private int page = 0;
    private final long openedAt = System.currentTimeMillis();

    private record Row(String icon, String name, String desc, BooleanSupplier get,
                       Consumer<Boolean> set, Runnable cycle, Supplier<String> value) { }

    public SanyaScreen() {
        super(Text.literal("SANЯ CLIENT"));
    }

    // ---------------------------------------------------------------- rows

    private static Row toggle(String icon, String name, String desc, BooleanSupplier get, Consumer<Boolean> set) {
        return new Row(icon, name, desc, get, set, null, null);
    }

    private static Row select(String icon, String name, String desc, Runnable cycle, Supplier<String> value) {
        return new Row(icon, name, desc, null, null, cycle, value);
    }

    private static Row info(String icon, String name, String desc, Supplier<String> value) {
        return new Row(icon, name, desc, null, null, null, value);
    }

    private List<Row> rows() {
        List<Row> r = new ArrayList<>();
        switch (page) {
            case 0 -> {
                r.add(toggle("⌁", "FPS Boost", "Оптимізація частинок і ефектів", () -> SanyaClient.lowParticlesEnabled, v -> SanyaClient.lowParticlesEnabled = v));
                r.add(toggle("⌖", "Keystrokes HUD", "Клавіші та CPS на екрані", () -> SanyaClient.keystrokesEnabled, v -> SanyaClient.keystrokesEnabled = v));
                r.add(toggle("♡", "Armor & Potion HUD", "Спорядження і таймер ефектів", () -> SanyaClient.armorHudEnabled,
                        v -> { SanyaClient.armorHudEnabled = v; SanyaClient.effectsHudEnabled = v; }));
                r.add(toggle("✳", "Hit Color", "Підсвітка влучання, лише візуально", () -> SanyaClient.hitColorEnabled, v -> SanyaClient.hitColorEnabled = v));
                r.add(toggle("⌗", "Low Particles", "Менше зайвих частинок", () -> SanyaClient.lowParticlesEnabled, v -> SanyaClient.lowParticlesEnabled = v));
            }
            case 1 -> {
                r.add(toggle("▣", "HUD Overlay", "FPS, координати та CPS", () -> SanyaClient.hudEnabled, v -> SanyaClient.hudEnabled = v));
                r.add(toggle("⌖", "Coordinates", "Координати гравця", () -> SanyaClient.coordinatesEnabled, v -> SanyaClient.coordinatesEnabled = v));
                r.add(toggle("✳", "CPS Counter", "Кліків за секунду", () -> SanyaClient.cpsEnabled, v -> SanyaClient.cpsEnabled = v));
                r.add(toggle("♡", "Armor HUD", "Здоров’я та броня", () -> SanyaClient.armorHudEnabled, v -> SanyaClient.armorHudEnabled = v));
                r.add(toggle("⌁", "Potion Effects", "Ефекти та час їх дії", () -> SanyaClient.effectsHudEnabled, v -> SanyaClient.effectsHudEnabled = v));
                r.add(toggle("◔", "Item Cooldown", "Перезарядка предмета в руці", () -> SanyaClient.cooldownHudEnabled, v -> SanyaClient.cooldownHudEnabled = v));
                r.add(toggle("◎", "Target HUD", "HP, броня та дистанція цілі", () -> SanyaClient.targetHudEnabled, v -> SanyaClient.targetHudEnabled = v));
            }
            case 2 -> {
                r.add(toggle("☀", "Full Bright", "Нічне бачення без зілля (G)", () -> SanyaClient.fullBrightEnabled, v -> SanyaClient.fullBrightEnabled = v));
                r.add(toggle("✋", "Echo Hands", "Анімована рука від першої особи", () -> SanyaClient.echoHandsEnabled, v -> SanyaClient.echoHandsEnabled = v));
                r.add(toggle("◎", "Target ESP", "Контур цілі в прямій видимості", () -> SanyaClient.targetEspEnabled, v -> SanyaClient.targetEspEnabled = v));
                r.add(select("◎", "ESP style", "Вигляд Target ESP (клавіша J)",
                        () -> SanyaClient.targetEspMode = (SanyaClient.targetEspMode + 1) % SanyaClient.ESP_MODES.length,
                        () -> SanyaClient.ESP_MODES[SanyaClient.targetEspMode]));
                r.add(select("◐", "ESP colour", "Палітра Target ESP",
                        () -> SanyaClient.espPalette = (SanyaClient.espPalette + 1) % SanyaClient.PALETTE_NAMES.length,
                        () -> SanyaClient.PALETTE_NAMES[SanyaClient.espPalette]));
                r.add(toggle("✳", "Hit Color", "Підсвітка влучання, лише візуально", () -> SanyaClient.hitColorEnabled, v -> SanyaClient.hitColorEnabled = v));
            }
            case 3 -> {
                r.add(toggle("✦", "Show cosmetics", "Показувати косметику (від 3-ї особи)", () -> Cosmetics.enabled, v -> Cosmetics.enabled = v));
                r.add(select("♔", "Hat", "Капелюх на голові",
                        () -> Cosmetics.hat = (Cosmetics.hat + 1) % Cosmetics.HATS.length, () -> Cosmetics.HATS[Cosmetics.hat]));
                r.add(select("❖", "Wings", "Крила за спиною",
                        () -> Cosmetics.wings = (Cosmetics.wings + 1) % Cosmetics.WINGS.length, () -> Cosmetics.WINGS[Cosmetics.wings]));
                r.add(select("◎", "Pet", "Супутник, що літає навколо",
                        () -> Cosmetics.pet = (Cosmetics.pet + 1) % Cosmetics.PETS.length, () -> Cosmetics.PETS[Cosmetics.pet]));
                r.add(select("◌", "Aura", "Ефект навколо тіла",
                        () -> Cosmetics.aura = (Cosmetics.aura + 1) % Cosmetics.AURAS.length, () -> Cosmetics.AURAS[Cosmetics.aura]));
                r.add(select("◐", "Colour", "Палітра косметики",
                        () -> Cosmetics.palette = (Cosmetics.palette + 1) % SanyaClient.PALETTE_NAMES.length,
                        () -> SanyaClient.PALETTE_NAMES[Cosmetics.palette]));
            }
            case 4 -> {
                r.add(toggle("☾", "Shader Sky", "Неонове небо (найкраще вночі)", () -> ShaderSky.enabled, v -> ShaderSky.enabled = v));
                r.add(select("◈", "Mode", "Вигляд неба",
                        () -> ShaderSky.mode = (ShaderSky.mode + 1) % ShaderSky.MODES.length, () -> ShaderSky.MODES[ShaderSky.mode]));
                r.add(select("◐", "Sky colour", "Палітра неба",
                        () -> ShaderSky.palette = (ShaderSky.palette + 1) % SanyaClient.PALETTE_NAMES.length,
                        () -> SanyaClient.PALETTE_NAMES[ShaderSky.palette]));
                r.add(select("⌁", "Speed", "Швидкість анімації",
                        () -> ShaderSky.speed = (ShaderSky.speed + 1) % ShaderSky.SPEEDS.length, () -> ShaderSky.SPEEDS[ShaderSky.speed]));
                r.add(select("◔", "Opacity", "Прозорість неба",
                        () -> ShaderSky.opacity = (ShaderSky.opacity + 1) % ShaderSky.OPACITIES.length, () -> ShaderSky.OPACITIES[ShaderSky.opacity]));
                r.add(select("◔", "Time", "Час у грі, лише візуально",
                        () -> SanyaClient.timeMode = (SanyaClient.timeMode + 1) % SanyaClient.TIME_MODES.length,
                        () -> SanyaClient.TIME_MODES[SanyaClient.timeMode]));
                r.add(select("☁", "Weather", "Погода в грі, лише візуально",
                        () -> SanyaClient.weatherMode = (SanyaClient.weatherMode + 1) % SanyaClient.WEATHER_MODES.length,
                        () -> SanyaClient.WEATHER_MODES[SanyaClient.weatherMode]));
            }
            case 5 -> {
                r.add(toggle("⌁", "FPS Boost", "Оптимізація частинок і ефектів", () -> SanyaClient.lowParticlesEnabled, v -> SanyaClient.lowParticlesEnabled = v));
                r.add(toggle("⌗", "Low Particles", "Менше зайвих частинок", () -> SanyaClient.lowParticlesEnabled, v -> SanyaClient.lowParticlesEnabled = v));
            }
            case 6 -> {
                r.add(select("▭", "Screen ratio", "Пропорції екрана (чорні смуги)",
                        () -> SanyaClient.aspectRatioIndex = (SanyaClient.aspectRatioIndex + 1) % SanyaClient.ASPECT_RATIOS.length,
                        () -> SanyaClient.ASPECT_RATIOS[SanyaClient.aspectRatioIndex]));
                r.add(select("◎", "ESP style", "Вигляд Target ESP",
                        () -> SanyaClient.targetEspMode = (SanyaClient.targetEspMode + 1) % SanyaClient.ESP_MODES.length,
                        () -> SanyaClient.ESP_MODES[SanyaClient.targetEspMode]));
                r.add(toggle("Aa", "Smooth font", "Гладкий шрифт замість майнкрафтного", () -> UiFont.enabled, v -> UiFont.enabled = v));
                r.add(select("◐", "HUD colour", "Акцентний колір HUD",
                        () -> HudRenderer.hudPalette = (HudRenderer.hudPalette + 1) % SanyaClient.PALETTE_NAMES.length,
                        () -> SanyaClient.PALETTE_NAMES[HudRenderer.hudPalette]));
                r.add(toggle("⌖", "Keystrokes HUD", "Клавіші та CPS на екрані", () -> SanyaClient.keystrokesEnabled, v -> SanyaClient.keystrokesEnabled = v));
                r.add(toggle("▣", "HUD Overlay", "FPS, координати та CPS", () -> SanyaClient.hudEnabled, v -> SanyaClient.hudEnabled = v));
            }
            default -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                r.add(info("◌", "Нікнейм", "Поточний гравець", () -> mc.player != null ? mc.player.getName().getString() : "—"));
                r.add(info("▧", "Збірка", "Версія клієнта", () -> "SANЯ CLIENT 0.0.6"));
                r.add(info("◈", "Minecraft", "Версія гри та завантажувач", () -> "1.21.11 · Fabric"));
            }
        }
        return r;
    }

    private int rowHeight(int n) {
        return Math.max(20, Math.min(33, 208 / Math.max(1, n)));
    }

    // ---------------------------------------------------------------- scale / mapping

    private float uiScale() {
        float fit = Math.min((width - 12f) / PW, (height - 12f) / PH);
        float base = Math.min(1.0f, fit);
        float p = Math.min(1.0f, (System.currentTimeMillis() - openedAt) / 180.0f);
        float eased = 1.0f - (1.0f - p) * (1.0f - p) * (1.0f - p);
        return base * (0.94f + 0.06f * eased);
    }

    private int originX(float s) { return Math.round((width - PW * s) / 2.0f); }

    private int originY(float s) { return Math.round((height - PH * s) / 2.0f); }

    // ---------------------------------------------------------------- draw helpers

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

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

    private static void rbox(DrawContext c, int x, int y, int w, int h, int r, int border, int fill) {
        rrect(c, x, y, w, h, r, border);
        rrect(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    private static void vgrad(DrawContext c, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0 : i / (float) (h - 1);
            int rr = Math.round(((top >> 16) & 255) * (1 - t) + ((bottom >> 16) & 255) * t);
            int gg = Math.round(((top >> 8) & 255) * (1 - t) + ((bottom >> 8) & 255) * t);
            int bb = Math.round((top & 255) * (1 - t) + (bottom & 255) * t);
            c.fill(x, y + i, x + w, y + i + 1, 0xFF000000 | (rr << 16) | (gg << 8) | bb);
        }
    }

    private void text(DrawContext c, Text t, float x, float y, int color, float sc) {
        c.getMatrices().pushMatrix();
        c.getMatrices().translate(x, y);
        c.getMatrices().scale(sc, sc);
        c.drawTextWithShadow(textRenderer, t, 0, 0, color);
        c.getMatrices().popMatrix();
    }

    private void text(DrawContext c, String s, float x, float y, int color, float sc) {
        text(c, UiFont.regular(s), x, y, color, sc);
    }

    private static Text bold(String s) {
        return UiFont.bold(s);
    }

    private int w(String s, float sc) {
        return Math.round(textRenderer.getWidth(UiFont.regular(s)) * sc);
    }

    private int w(Text t, float sc) {
        return Math.round(textRenderer.getWidth(t) * sc);
    }

    private void label(DrawContext c, String s, int x, int y) {
        text(c, s, x, y, 0xFF697164, 0.6f);
    }

    // ---------------------------------------------------------------- render

    // Ванільний фон з блюром вимкнено: у 1.21.11 блюр можна викликати лише раз за кадр.
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x90000000);
        float s = uiScale();
        int ox = originX(s), oy = originY(s);
        int vx = (int) Math.floor((mouseX - ox) / s), vy = (int) Math.floor((mouseY - oy) / s);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) ox, (float) oy);
        context.getMatrices().scale(s, s);
        drawApp(context, vx, vy);
        context.getMatrices().popMatrix();

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawApp(DrawContext c, int mx, int my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        rbox(c, 0, 0, PW, PH, 8, 0xFF343A30, BG);

        // ---- top bar
        c.fill(1, TOP - 1, PW - 1, TOP, LINE);
        rrect(c, 16, 13, 18, 18, 5, LIME);
        text(c, bold("S"), 22, 18, 0xFF151A10, 1.0f);
        text(c, bold("SANЯ CLIENT"), 44, 13, TEXT, 0.95f);
        text(c, "BLOCK GAME TOOLKIT", 44, 25, MUTED, 0.55f);
        rbox(c, PW - 38, 11, 22, 22, 4, LINE, BG);
        text(c, "⚙", PW - 38 + 7, 11 + 7, 0xFFA3AAA0, 0.9f);
        String pill = "● READY";
        int pillW = w(pill, 0.7f) + 14;
        int pillX = PW - 38 - 8 - pillW;
        rbox(c, pillX, 14, pillW, 16, 8, 0xFF37402F, BG);
        text(c, pill, pillX + 7, 19, LIME, 0.7f);

        // ---- sidebar
        c.fill(SIDE - 1, TOP, SIDE, PH - 1, LINE);
        label(c, "WORKSPACE", 12, TOP + 16);
        for (int i = 0; i < NAV.length; i++) navItem(c, mx, my, i, NAV_ICON[i], NAV[i], 74 + i * 24);
        label(c, "PROFILE", 12, 250);
        navItem(c, mx, my, PROFILE_PAGE, "◌", "Мій профіль", 262);
        rbox(c, 10, PH - 70, 120, 50, 5, LINE, 0xFF171A15);
        text(c, "АКТИВНИЙ ПРОФІЛЬ", 20, PH - 61, 0xFF8D9588, 0.55f);
        text(c, bold("Трішки пітний ☻"), 20, PH - 47, TEXT, 0.8f);

        // ---- header
        text(c, "SANЯ / " + CRUMB[page], MX, TOP + 14, 0xFF7C8577, 0.6f);
        String[] hero = HERO[page];
        Text h1a = bold(hero[0]);
        text(c, h1a, MX, 68, TEXT, 2.0f);
        text(c, bold(hero[1]), MX + w(h1a, 2.0f), 68, LIME, 2.0f);
        text(c, hero[2], MX, 92, 0xFF899184, 0.85f);

        int vx = MX + MW;
        String[] parts = {"MC ", "1.21.11", "  ·  build ", "0.0.6"};
        int total = 0;
        for (String p : parts) total += w(p, 0.7f);
        int vbW = total + 16;
        rbox(c, vx - vbW, 72, vbW, 18, 4, LINE, BG);
        int cx = vx - vbW + 8;
        for (int i = 0; i < parts.length; i++) {
            text(c, parts[i], cx, 77, i % 2 == 1 ? LIME : 0xFFAFB6AA, 0.7f);
            cx += w(parts[i], 0.7f);
        }

        // ---- tabs
        int tx = MX;
        for (int i = 0; i < TABS.length; i++) {
            int tw = w(TABS[i], 0.85f);
            text(c, TABS[i], tx, 120, i == 0 ? LIME : 0xFF777F74, 0.85f);
            if (i == 0) c.fill(tx - 2, 133, tx + tw + 2, 135, LIME);
            tx += tw + 18;
        }
        c.fill(MX, 135, MX + MW, 136, LINE);

        // ---- left card: toggles
        List<Row> rows = rows();
        rbox(c, MX, GRID_Y, LW, LH, 5, LINE, PANEL);
        text(c, bold(page == 0 ? "Швидкі перемикачі" : NAV_OR_PROFILE(page)), MX + 12, GRID_Y + 11, TEXT, 0.9f);
        String count = String.format("%02d MODULES", rows.size());
        text(c, count, MX + LW - 12 - w(count, 0.6f), GRID_Y + 13, 0xFF7E8878, 0.6f);
        int rowsTop = GRID_Y + 34;
        int rh = rowHeight(rows.size());
        for (int i = 0; i < rows.size(); i++) drawRow(c, mx, my, rows.get(i), rowsTop + i * rh, rh, i);

        // ---- right column
        rbox(c, RX, GRID_Y, RW, 164, 5, LINE, PANEL);
        text(c, bold(page == 3 ? "Попередній перегляд косметики" : page == 4 ? "Попередній перегляд неба" : "Попередній перегляд HUD"), RX + 12, GRID_Y + 11, TEXT, 0.9f);
        String live = "LIVE PREVIEW";
        text(c, live, RX + RW - 12 - w(live, 0.6f), GRID_Y + 13, 0xFF7E8878, 0.6f);
        if (page == 3) drawCosmeticPreview(c, RX + 12, GRID_Y + 30, RW - 24, 122);
        else if (page == 4) drawSkyPreview(c, RX + 12, GRID_Y + 30, RW - 24, 122);
        else drawPreview(c, mc, RX + 12, GRID_Y + 30, RW - 24, 122);

        int sy = GRID_Y + 174;
        rbox(c, RX, sy, RW, 78, 5, LINE, PANEL);
        text(c, bold("Стан системи"), RX + 12, sy + 11, TEXT, 0.9f);
        String opt = "● OPTIMIZED";
        text(c, opt, RX + RW - 12 - w(opt, 0.6f), sy + 13, 0xFF98A78A, 0.6f);
        int bw = (RW - 24 - 12) / 3;
        int bx = RX + 12, by = sy + 30;
        stat(c, bx, by, bw, "FPS", String.valueOf(mc.getCurrentFps()), "fps");
        stat(c, bx + bw + 6, by, bw, "LATENCY", String.valueOf(ping(mc)), "ms");
        stat(c, bx + (bw + 6) * 2, by, bw, "MODULES", String.format("%02d", enabledModules()), "/ 13");

        // ---- footer
        int fy = PH - 22;
        String f1 = "made for block enjoyers ", f2 = "·", f3 = " не для поту";
        text(c, f1, MX, fy, 0xFF687164, 0.65f);
        text(c, f2, MX + w(f1, 0.65f), fy, 0xFFA0A99A, 0.65f);
        text(c, f3, MX + w(f1, 0.65f) + w(f2, 0.65f), fy, 0xFF687164, 0.65f);
        String r1 = "SANЯ CLIENT ", r2 = "v0.0.6";
        int rx = MX + MW - w(r1, 0.65f) - w(r2, 0.65f);
        text(c, r1, rx, fy, 0xFF687164, 0.65f);
        text(c, r2, rx + w(r1, 0.65f), fy, 0xFFA0A99A, 0.65f);
    }

    private static String NAV_OR_PROFILE(int p) {
        return p == PROFILE_PAGE ? "Мій профіль" : NAV[p];
    }

    private void navItem(DrawContext c, int mx, int my, int idx, String icon, String name, int y) {
        boolean active = page == idx;
        if (active) {
            rrect(c, 8, y, 124, 22, 4, 0xFF252D20);
            c.fill(8, y + 3, 10, y + 19, LIME);
        } else if (in(mx, my, 8, y, 124, 22)) {
            rrect(c, 8, y, 124, 22, 4, 0xFF1A1F17);
        }
        text(c, icon, 18, y + 7, active ? LIME : 0xFF788171, 0.95f);
        text(c, name, 38, y + 7, active ? LIME : 0xFF939B90, 0.95f);
    }

    private void drawRow(DrawContext c, int mx, int my, Row row, int y, int rh, int index) {
        if (in(mx, my, MX + 6, y, LW - 12, rh)) rrect(c, MX + 6, y + 1, LW - 12, rh - 2, 4, 0xFF1B1F1A);
        if (index > 0) c.fill(MX + 12, y, MX + LW - 12, y + 1, 0xFF262B25);

        int by = y + (rh - 20) / 2;
        rbox(c, MX + 12, by, 20, 20, 4, 0xFF323A2D, 0xFF20261C);
        text(c, row.icon(), MX + 12 + (20 - w(row.icon(), 0.9f)) / 2.0f, by + 6, LIME, 0.9f);
        text(c, bold(row.name()), MX + 40, y + rh / 2.0f - 9, TEXT, 0.85f);
        text(c, row.desc(), MX + 40, y + rh / 2.0f + 2, 0xFF788174, 0.62f);

        if (row.get() != null) {
            boolean on = row.get().getAsBoolean();
            int sx = MX + LW - 12 - 22, sy = y + (rh - 12) / 2;
            rrect(c, sx, sy, 22, 12, 6, on ? 0xFF647D37 : 0xFF383E35);
            rrect(c, sx + (on ? 12 : 2), sy + 2, 8, 8, 4, on ? LIME : 0xFF92998D);
        } else if (row.value() != null) {
            String v = row.value().get();
            int pw = w(v, 0.75f) + 14;
            int px = MX + LW - 12 - pw, py = y + (rh - 14) / 2;
            rbox(c, px, py, pw, 14, 4, 0xFF37402F, 0xFF20261C);
            text(c, v, px + 7, py + 4, LIME, 0.75f);
        }
    }

    private void stat(DrawContext c, int x, int y, int w, String label, String value, String em) {
        rbox(c, x, y, w, 36, 4, 0xFF292F27, 0xFF1B1F19);
        text(c, label, x + 6, y + 6, 0xFF7F8979, 0.55f);
        text(c, value, x + 6, y + 17, TEXT, 1.2f);
        text(c, em, x + 6 + w(value, 1.2f) + 3, y + 22, LIME, 0.6f);
    }

    private int enabledModules() {
        boolean[] all = {SanyaClient.lowParticlesEnabled, SanyaClient.keystrokesEnabled, SanyaClient.hudEnabled,
                SanyaClient.coordinatesEnabled, SanyaClient.cpsEnabled, SanyaClient.armorHudEnabled,
                SanyaClient.effectsHudEnabled, SanyaClient.cooldownHudEnabled, SanyaClient.targetHudEnabled,
                SanyaClient.targetEspEnabled, SanyaClient.fullBrightEnabled, SanyaClient.echoHandsEnabled,
                SanyaClient.hitColorEnabled};
        int n = 0;
        for (boolean b : all) if (b) n++;
        return n;
    }

    private int ping(MinecraftClient mc) {
        try {
            if (mc.player != null && mc.getNetworkHandler() != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) return entry.getLatency();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    // ---------------------------------------------------------------- HUD preview

    private void drawPreview(DrawContext c, MinecraftClient mc, int x, int y, int pw, int ph) {
        // frame + sky
        rrect(c, x, y, pw, ph, 5, 0xFF354331);
        vgrad(c, x + 1, y + 3, pw - 2, ph - 6, 0xFF3D5131, 0xFF17241D);
        vgrad(c, x + 2, y + 1, pw - 4, 2, 0xFF3D5131, 0xFF3D5131);

        // sun
        int sr = 17, scx = x + pw - 21 - sr, scy = y + 15 + sr;
        for (int i = -sr; i <= sr; i++) {
            int half = (int) Math.round(Math.sqrt(sr * sr - i * i));
            c.fill(scx - half, scy + i, scx + half, scy + i + 1, 0xFFE2CB88);
        }

        // mountains
        double[][] prof = {{0, .78}, {.18, .24}, {.29, .60}, {.46, .05}, {.68, .73}, {.83, .31}, {1, .74}};
        int mTop = y + 38, mH = 56;
        for (int col = 1; col < pw - 1; col++) {
            double f = col / (double) (pw - 1);
            double yf = prof[prof.length - 1][1];
            for (int k = 0; k + 1 < prof.length; k++) {
                if (f >= prof[k][0] && f <= prof[k + 1][0]) {
                    double t = (f - prof[k][0]) / (prof[k + 1][0] - prof[k][0]);
                    yf = prof[k][1] + (prof[k + 1][1] - prof[k][1]) * t;
                    break;
                }
            }
            c.fill(x + col, mTop + (int) (yf * mH), x + col + 1, mTop + mH, 0xFF263825);
        }

        // ground
        int gy = y + ph - 30;
        for (int col = 1; col < pw - 1; col += 16) {
            int cw = Math.min(16, pw - 1 - col);
            c.fill(x + col, gy, x + col + cw, y + ph - 3, ((col / 16) % 2 == 0) ? 0xFF594D35 : 0xFF68593A);
        }
        c.fill(x + 1, gy, x + pw - 1, gy + 2, 0xFF77934C);

        // crosshair
        int cx = x + pw / 2, cy = y + ph / 2 - 6;
        c.fill(cx - 4, cy, cx + 5, cy + 1, 0xFFFFFFFF);
        c.fill(cx, cy - 4, cx + 1, cy + 5, 0xFFFFFFFF);

        // live HUD text
        int fps = mc.getCurrentFps();
        String xyz = mc.player != null
                ? String.format("XYZ %.0f / %.0f / %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ())
                : "XYZ 128 / 64 / -32";
        text(c, "FPS " + fps, x + 7, y + 7, 0xFFEFF6DF, 0.65f);
        text(c, xyz, x + 7, y + 15, 0xFFEFF6DF, 0.65f);

        // mode tag
        String mode = "SURVIVAL";
        try {
            if (mc.interactionManager != null) mode = mc.interactionManager.getCurrentGameMode().name();
        } catch (Throwable ignored) {
        }
        String tag = mode + " · 1.21";
        int tw = w(tag, 0.55f) + 10;
        rrect(c, x + pw - 7 - tw, y + 6, tw, 12, 3, 0xFF121711);
        text(c, tag, x + pw - 7 - tw + 5, y + 10, LIME, 0.55f);

        // hearts
        text(c, "♥ ♥ ♥ ♥ ♥", x + 7, gy - 13, 0xFFFF7769, 0.75f);

        // keystrokes
        String[] keys = {"W", "A", "S", "D", "CPS " + SanyaClient.currentCps()};
        int kx = x + pw - 7;
        for (int i = keys.length - 1; i >= 0; i--) {
            int kw = w(keys[i], 0.55f) + 8;
            kx -= kw;
            rbox(c, kx, y + ph - 17, kw, 11, 2, 0xFF55683A, 0xFF10150F);
            text(c, keys[i], kx + 4, y + ph - 14, 0xFFE3F2C7, 0.55f);
            kx -= 3;
        }
    }

    // ---------------------------------------------------------------- cosmetics preview (2D)

    private static void px(DrawContext c, int x, int y, int color) {
        c.fill(x, y, x + 1, y + 1, color);
        int glow = (color & 0x00FFFFFF) | 0x55000000;
        c.fill(x + 1, y, x + 2, y + 1, glow);
        c.fill(x, y + 1, x + 1, y + 2, glow);
    }

    private static void ln(DrawContext c, double x0, double y0, double x1, double y1, int color) {
        int steps = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            px(c, (int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), color);
        }
    }

    private static void ell(DrawContext c, double cx, double cy, double rx, double ry, int color) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n, b = Math.PI * 2 * (i + 1) / n;
            ln(c, cx + Math.cos(a) * rx, cy + Math.sin(a) * ry, cx + Math.cos(b) * rx, cy + Math.sin(b) * ry, color);
        }
    }

    private void drawCosmeticPreview(DrawContext c, int x, int y, int pw, int ph) {
        rrect(c, x, y, pw, ph, 5, 0xFF2A3326);
        vgrad(c, x + 2, y + 2, pw - 4, ph - 4, 0xFF1E2619, 0xFF121711);
        c.fill(x + 2, y + ph - 22, x + pw - 2, y + ph - 2, 0xFF181E15);

        int a = 0xFF000000 | SanyaClient.palA(Cosmetics.palette);
        int b = 0xFF000000 | SanyaClient.palB(Cosmetics.palette);
        double t = System.currentTimeMillis() / 1000.0;
        int cx = x + pw / 2, feet = y + ph - 24;
        int shoulder = feet - 44, headTop = feet - 62;

        // wings (behind the avatar)
        if (Cosmetics.wings > 0) {
            double flap = 1.0 + 0.06 * Math.sin(t * 3.0);
            List<double[]> shape = Cosmetics.wingShape(Cosmetics.wings);
            for (int side = -1; side <= 1; side += 2) {
                for (int pi = 0; pi < shape.size(); pi++) {
                    double[] poly = shape.get(pi);
                    for (int i = 0; i + 3 < poly.length; i += 2) {
                        ln(c, cx + side * (6 + poly[i] * 34 * flap), shoulder - poly[i + 1] * 34,
                                cx + side * (6 + poly[i + 2] * 34 * flap), shoulder - poly[i + 3] * 34, pi % 2 == 0 ? a : b);
                    }
                }
            }
        }
        // aura (behind)
        switch (Cosmetics.aura) {
            case 1 -> {
                for (int k = 0; k < 3; k++) {
                    double f = (t * 0.5 + k / 3.0) % 1.0;
                    ell(c, cx, feet - f * 60, 14, 3, ((int) Math.round(200 * Math.sin(f * Math.PI)) << 24) | (a & 0xFFFFFF));
                }
            }
            case 2 -> {
                for (int strand = 0; strand < 2; strand++) {
                    double lx = 0, ly = 0;
                    for (int i = 0; i <= 30; i++) {
                        double u = i / 30.0, ang = u * Math.PI * 4 + t * 2 + strand * Math.PI;
                        double X = cx + Math.cos(ang) * 15, Y = feet - u * 62;
                        if (i > 0) ln(c, lx, ly, X, Y, strand == 0 ? a : b);
                        lx = X;
                        ly = Y;
                    }
                }
            }
            case 3 -> {
                for (int k = 0; k < 2; k++) {
                    double f = (t * 0.6 + k * 0.5) % 1.0;
                    ell(c, cx, feet, 8 + f * 38, 2 + f * 6, ((int) Math.round(220 * (1 - f)) << 24) | ((k == 0 ? a : b) & 0xFFFFFF));
                }
            }
            default -> { }
        }

        // avatar
        c.fill(cx - 8, feet - 22, cx - 1, feet, 0xFF3A3F9E);
        c.fill(cx + 1, feet - 22, cx + 8, feet, 0xFF3A3F9E);
        c.fill(cx - 8, feet - 46, cx + 8, feet - 22, 0xFF2B9EA6);
        c.fill(cx - 14, feet - 46, cx - 8, feet - 24, 0xFFC58C6B);
        c.fill(cx + 8, feet - 46, cx + 14, feet - 24, 0xFFC58C6B);
        c.fill(cx - 8, headTop, cx + 8, feet - 46, 0xFFC58C6B);
        c.fill(cx - 5, headTop + 6, cx - 3, headTop + 8, 0xFF2B1B14);
        c.fill(cx + 3, headTop + 6, cx + 5, headTop + 8, 0xFF2B1B14);

        // hat
        switch (Cosmetics.hat) {
            case 1 -> ell(c, cx, headTop - 8 + Math.sin(t * 2) * 1.5, 10, 3, a);
            case 2 -> {
                ln(c, cx, headTop - 18, cx - 20, headTop - 1, a);
                ln(c, cx, headTop - 18, cx + 20, headTop - 1, a);
                ln(c, cx - 20, headTop - 1, cx + 20, headTop - 1, b);
            }
            case 3 -> {
                double[][] pts = {{-8, 0}, {-8, -9}, {-4, -4}, {0, -10}, {4, -4}, {8, -9}, {8, 0}, {-8, 0}};
                for (int i = 0; i + 1 < pts.length; i++) ln(c, cx + pts[i][0], headTop + pts[i][1], cx + pts[i + 1][0], headTop + pts[i + 1][1], a);
            }
            case 4 -> {
                for (int side = -1; side <= 1; side += 2) {
                    ln(c, cx + side * 7, headTop + 1, cx + side * 11, headTop - 6, b);
                    ln(c, cx + side * 11, headTop - 6, cx + side * 9, headTop - 14, b);
                }
            }
            default -> { }
        }

        // pet
        double bob = Math.sin(t * 2.6) * 3;
        int petX = cx + 36, petY = (int) Math.round(feet - 50 + bob);
        switch (Cosmetics.pet) {
            case 1 -> {
                ln(c, petX, petY - 8, petX + 5, petY, a);
                ln(c, petX + 5, petY, petX, petY + 8, b);
                ln(c, petX, petY + 8, petX - 5, petY, b);
                ln(c, petX - 5, petY, petX, petY - 8, a);
            }
            case 2 -> {
                ln(c, petX - 5, petY - 5, petX + 5, petY - 5, a);
                ln(c, petX + 5, petY - 5, petX + 5, petY + 5, a);
                ln(c, petX + 5, petY + 5, petX - 5, petY + 5, a);
                ln(c, petX - 5, petY + 5, petX - 5, petY - 5, a);
            }
            case 3 -> {
                for (int i = 0; i < 10; i++) {
                    int col = (((int) Math.round(230 * (1 - i / 10.0))) << 24) | (a & 0xFFFFFF);
                    px(c, petX - i * 2, petY + (int) Math.round(Math.sin(t * 5 - i * 0.4) * 2), col);
                    if (i < 4) px(c, petX - i * 2, petY + 1 + (int) Math.round(Math.sin(t * 5 - i * 0.4) * 2), col);
                }
            }
            default -> { }
        }

        if (Cosmetics.hat + Cosmetics.wings + Cosmetics.pet + Cosmetics.aura == 0) {
            String hint = "Обери косметику зліва";
            text(c, hint, x + (pw - w(hint, 0.7f)) / 2.0f, y + 10, 0xFF7E8878, 0.7f);
        }
    }

    private void drawSkyPreview(DrawContext c, int x, int y, int pw, int ph) {
        rrect(c, x, y, pw, ph, 5, 0xFF2A3326);
        int a = 0xFF000000 | SanyaClient.palA(ShaderSky.palette);
        int b = 0xFF000000 | SanyaClient.palB(ShaderSky.palette);
        int top = 0xFF0A0F1E, bottom = 0xFF1B2440;
        int gy = y + ph - 22;
        if (ShaderSky.mode == 3) {
            top = 0xFF000000 | SanyaClient.mix(0x0A0F1E, SanyaClient.palB(ShaderSky.palette), 0.5);
            bottom = 0xFF000000 | SanyaClient.mix(0x1B2440, SanyaClient.palA(ShaderSky.palette), 0.55);
        }
        vgrad(c, x + 2, y + 2, pw - 4, ph - 4, top, bottom);
        double[] spd = {0.5, 1.0, 2.0, 3.0};
        double t = System.currentTimeMillis() / 1000.0 * spd[Math.floorMod(ShaderSky.speed, spd.length)];
        java.util.Random rnd = new java.util.Random(7);

        switch (ShaderSky.mode) {
            case 0 -> {
                for (int i = 0; i < 44; i++) {
                    double u = i / 43.0;
                    int X = x + 6 + (int) Math.round(u * (pw - 12));
                    double base = gy - 6 - 6 * Math.sin(u * 7 + t * 0.8), tp = base - 34 - 12 * Math.sin(u * 11 - t * 1.2);
                    double edge = Math.sin(u * Math.PI);
                    for (int s = 0; s < 6; s++) {
                        double y1 = base + (tp - base) * s / 6.0, y2 = base + (tp - base) * (s + 1) / 6.0;
                        int al = (int) Math.round(210 * edge * (1 - s / 6.0));
                        c.fill(X, (int) y2, X + 2, (int) y1, (al << 24) | (SanyaClient.mix(SanyaClient.palA(ShaderSky.palette), SanyaClient.palB(ShaderSky.palette), s / 5.0) & 0xFFFFFF));
                    }
                }
            }
            case 1 -> {
                for (int i = 0; i < 46; i++) {
                    int sx = x + 4 + rnd.nextInt(pw - 8), sy = y + 4 + rnd.nextInt(Math.max(1, gy - y - 8));
                    int al = (int) Math.round(110 + 140 * (0.5 + 0.5 * Math.sin(t * 1.7 + i)));
                    c.fill(sx, sy, sx + 1, sy + 1, (al << 24) | 0xFFFFFF);
                }
                ell(c, x + pw - 30, y + 24, 7, 7, 0xFFEFEFFF);
            }
            case 2 -> {
                for (int i = 0; i < 44; i++) {
                    double fx = rnd.nextDouble(), fy = rnd.nextDouble(), ph2 = rnd.nextDouble() * 6;
                    int sx = x + 4 + (int) Math.round(fx * (pw - 8) + Math.sin(t + ph2) * 3);
                    int sy = y + 4 + (int) Math.round(((fy + t * 0.15) % 1.0) * (gy - y - 8));
                    c.fill(sx, sy, sx + 2, sy + 2, 0xE0FFFFFF);
                }
            }
            case 4 -> {
                for (int i = 0; i < 6; i++) {
                    int sx = x + 14 + rnd.nextInt(pw - 28), sy = y + 12 + rnd.nextInt(Math.max(1, gy - y - 30));
                    double L = 3 + 3 * (0.5 + 0.5 * Math.sin(t * 1.4 + i));
                    ln(c, sx - L, sy, sx + L, sy, a);
                    ln(c, sx, sy - L, sx, sy + L, a);
                }
            }
            case 5 -> {
                for (int k = 0; k < 3; k++) {
                    ell(c, x + pw / 2.0, gy, 40 + k * 22, 6 + k * 6, ((int) Math.round(180 * (0.6 + 0.4 * Math.sin(t * 1.1 + k))) << 24) | ((k == 1 ? b : a) & 0xFFFFFF));
                }
            }
            case 6 -> {
                for (int k = 0; k < 6; k++) {
                    double lx = 0, ly = 0;
                    for (int i = 0; i <= 30; i++) {
                        double X = x + 4 + i * (pw - 8) / 30.0, Y = y + 12 + k * ((gy - y - 20) / 6.0) + 3 * Math.sin(i * 0.5 + t * 1.2 + k);
                        if (i > 0) ln(c, lx, ly, X, Y, k % 2 == 0 ? a : b);
                        lx = X;
                        ly = Y;
                    }
                }
            }
            case 7 -> {
                for (int i = 0; i < 6; i++) {
                    double cx = x + ((rnd.nextDouble() * pw + t * 4) % pw), cy = y + 14 + rnd.nextInt(Math.max(1, gy - y - 34));
                    for (int k = 0; k < 4; k++) {
                        int len = 14 - Math.abs(k - 1) * 4 + rnd.nextInt(4);
                        c.fill((int) cx - len, (int) cy + k * 3, (int) cx + len, (int) cy + k * 3 + 3, 0x66DDE6F5);
                    }
                }
            }
            default -> { }
        }

        for (int col = 2; col < pw - 2; col++) {
            int h = 8 + (int) Math.round(6 * Math.sin(col * 0.07) + 4 * Math.sin(col * 0.19));
            c.fill(x + col, gy - h + 8, x + col + 1, y + ph - 2, 0xFF0F1410);
        }
        if (!ShaderSky.enabled) {
            String hint = "Shader Sky вимкнено";
            text(c, hint, x + (pw - w(hint, 0.7f)) / 2.0f, y + 8, 0xFF7E8878, 0.7f);
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            float s = uiScale();
            int vx = (int) Math.floor((click.x() - originX(s)) / s);
            int vy = (int) Math.floor((click.y() - originY(s)) / s);

            for (int i = 0; i < NAV.length; i++) {
                if (in(vx, vy, 8, 74 + i * 24, 124, 22)) {
                    page = i;
                    return true;
                }
            }
            if (in(vx, vy, 8, 262, 124, 22)) {
                page = PROFILE_PAGE;
                return true;
            }

            List<Row> rows = rows();
            int rowsTop = GRID_Y + 34;
            int rh = rowHeight(rows.size());
            for (int i = 0; i < rows.size(); i++) {
                if (in(vx, vy, MX + 6, rowsTop + i * rh, LW - 12, rh)) {
                    Row row = rows.get(i);
                    if (row.get() != null) row.set().accept(!row.get().getAsBoolean());
                    else if (row.cycle() != null) row.cycle().run();
                    SanyaConfig.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
