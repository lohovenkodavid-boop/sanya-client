package com.sanya.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.util.math.Box;

public class SanyaClient implements ClientModInitializer {
    public static boolean hudEnabled = true;
    public static boolean keystrokesEnabled = true;
    public static boolean coordinatesEnabled = true;
    public static boolean lowParticlesEnabled = false;
    public static boolean cpsEnabled = true;
    public static boolean fullBrightEnabled = false;
    public static boolean hitColorEnabled = true;
    public static boolean armorHudEnabled = true;
    public static boolean effectsHudEnabled = true;
    public static boolean cooldownHudEnabled = true;
    public static boolean targetHudEnabled = true;
    public static boolean targetEspEnabled = true;
    public static int targetEspMode = 0;
    public static final String[] ESP_MODES = {"Crystals", "Cube", "Souls", "Square", "Corners", "Orbit", "Ring", "Classic"};
    public static final String[] PALETTE_NAMES = {"Violet", "Pink", "Cyan", "Lime", "Red", "Gold"};
    private static final int[][] PALETTE = {
            {0x8A5CFF, 0x5CA0FF}, {0xFF5CCB, 0xB45CFF}, {0x5CF0FF, 0x5C8AFF},
            {0xC7F36B, 0x5CE08A}, {0xFF5C5C, 0xFFA05C}, {0xFFD65C, 0xFF9F5C}};
    public static int espPalette = 0;
    public static int timeMode = 0;
    public static final String[] TIME_MODES = {"Default", "Sunrise", "Day", "Noon", "Sunset", "Night", "Midnight"};
    private static final long[] TIME_VALUES = {-1L, 23000L, 1000L, 6000L, 12000L, 14000L, 18000L};
    public static int weatherMode = 0;
    public static final String[] WEATHER_MODES = {"Default", "Clear", "Rain", "Thunder"};
    private static boolean timeOverridden;
    private static long savedTimeOfDay;
    private static long savedGameTime;
    private static boolean weatherOverridden;
    private static KeyBinding espStyleKey;
    public static boolean echoHandsEnabled = false;
    public static int aspectRatioIndex = 0;
    public static final String[] ASPECT_RATIOS = {"Original", "16:9", "16:10", "4:3", "21:9"};
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("sanya", "controls"));
    private static KeyBinding openMenuKey;
    private static KeyBinding fullBrightKey;
    private static boolean fullBrightApplied;
    private static ParticlesMode previousParticles;
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static boolean leftMouseWasDown;
    private static PlayerEntity visibleTarget;

    @Override
    public void onInitializeClient() {
        SanyaConfig.load();
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> SanyaConfig.save());
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sanya.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY));
        fullBrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sanya.fullbright", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                CATEGORY));

        espStyleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sanya.esp_style", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (espStyleKey.wasPressed()) {
                targetEspMode = (targetEspMode + 1) % ESP_MODES.length;
                SanyaConfig.save();
            }
            applyWorldVisuals(client);
            HudRenderer.tick(client);
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen instanceof SanyaScreen) client.setScreen(null);
                else client.setScreen(new SanyaScreen());
            }
            if (client.options != null && lowParticlesEnabled) {
                if (previousParticles == null) previousParticles = client.options.getParticles().getValue();
                client.options.getParticles().setValue(ParticlesMode.MINIMAL);
            } else if (client.options != null && previousParticles != null) {
                client.options.getParticles().setValue(previousParticles);
                previousParticles = null;
            }
            // FullBright from Sanya Visuals: client-side Night Vision effect
            while (fullBrightKey.wasPressed()) {
                fullBrightEnabled = !fullBrightEnabled;
                SanyaConfig.save();
            }
            if (client.player != null) {
                if (fullBrightEnabled) {
                    client.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION, 400, 0, false, false));
                    fullBrightApplied = true;
                } else if (fullBrightApplied) {
                    client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                    fullBrightApplied = false;
                }
            }
            updateTarget(client);
            if (client.player != null && client.currentScreen == null) {
                boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
                long now = System.currentTimeMillis();
                if (down && !leftMouseWasDown) leftClicks.addLast(now);
                leftMouseWasDown = down;
                while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000) leftClicks.removeFirst();
            } else {
                leftMouseWasDown = false;
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> renderHud(context, 0.0f));
        WorldRenderEvents.AFTER_ENTITIES.register(SanyaClient::renderTargetEsp);
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Cosmetics.render(context);
            ShaderSky.render(context);
        });
    }

    // Client-side only: the server's real time and weather are not touched.
    private static void applyWorldVisuals(MinecraftClient client) {
        net.minecraft.client.world.ClientWorld world = client.world;
        if (world == null) {
            timeOverridden = false;
            weatherOverridden = false;
            return;
        }
        if (timeMode > 0 && timeMode < TIME_VALUES.length) {
            long now = world.getTimeOfDay();
            if (!timeOverridden) {
                savedTimeOfDay = now;
                savedGameTime = world.getTime();
                timeOverridden = true;
            }
            long day = Math.floorDiv(now, 24000L);
            world.setTime(world.getTime(), day * 24000L + TIME_VALUES[timeMode], false);
        } else if (timeOverridden) {
            long restored = savedTimeOfDay + (world.getTime() - savedGameTime);
            world.setTime(world.getTime(), restored, true);
            timeOverridden = false;
        }
        if (weatherMode > 0) {
            world.setRainGradient(weatherMode >= 2 ? 1.0f : 0.0f);
            world.setThunderGradient(weatherMode == 3 ? 1.0f : 0.0f);
            weatherOverridden = true;
        } else if (weatherOverridden) {
            weatherOverridden = false; // vanilla ticking eases the gradients back to the real weather
        }
    }

    public static int currentCps() {
        return leftClicks.size();
    }

    private static void renderHud(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        if (hudEnabled && hitColorEnabled && client.player.hurtTime > 0) {
            int alpha = Math.min(78, client.player.hurtTime * 6);
            context.fill(0, 0, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(),
                    (alpha << 24) | 0x00FF2525);
        }
        HudRenderer.render(context, client, tickDelta);
        drawAspectBars(context, client);
    }

    static PlayerEntity currentTarget() {
        return visibleTarget;
    }

    private static void updateTarget(MinecraftClient client) {
        PlayerEntity candidate = null;
        if (client.player != null && client.currentScreen == null
                && client.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof PlayerEntity player
                && player != client.player && client.player.canSee(player)) {
            candidate = player;
        }
        visibleTarget = candidate;
    }

    private static void renderTargetEsp(net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity target = visibleTarget;
        if (!targetEspEnabled || target == null || target.isRemoved() || !target.isAlive()
                || client.player == null || client.world == null || !client.player.canSee(target)
                || !(client.crosshairTarget instanceof EntityHitResult hit) || hit.getEntity() != target
                || context.consumers() == null) return;

        float tp = client.getRenderTickCounter().getTickProgress(false);
        net.minecraft.util.math.Vec3d camera = context.worldState().cameraRenderState.pos;
        net.minecraft.util.math.Vec3d lp = target.getLerpedPos(tp);
        Box box = target.getBoundingBox().offset(lp.x - target.getX() - camera.x,
                lp.y - target.getY() - camera.y, lp.z - target.getZ() - camera.z);
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.lines());
        net.minecraft.client.util.math.MatrixStack.Entry entry = context.matrices().peek();
        bindBuffers(lines, entry);
        double t = System.currentTimeMillis() / 1000.0;
        double ticks = client.player.age + tp;
        double cx = (box.minX + box.maxX) / 2, cy = (box.minY + box.maxY) / 2, cz = (box.minZ + box.maxZ) / 2;
        switch (targetEspMode) {
            case 0 -> espCrystals(lines, entry, box, target, cx, cz, ticks);
            case 1 -> espCube(lines, entry, box, target, cx, cz);
            case 2 -> espSouls(lines, entry, box, target, cx, cz);
            case 3 -> espSquare(lines, entry, cx, cy, cz, ticks);
            case 5 -> espOrbit(lines, entry, box, cx, cz, t);
            case 6 -> espRing(lines, entry, box, cx, cz, t);
            case 7 -> espClassic(lines, entry, box, t);
            default -> espCorners(lines, entry, box, cx, cy, cz, t);
        }
    }

    // EnoughVisuals "Crystals": three tiers of orbiting crystals (re-drawn as neon line-art).
    private static void espCrystals(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                    Box box, PlayerEntity target, double cx, double cz, double ticks) {
        double height = box.maxY - box.minY, centerY = box.minY + height * 0.5;
        double radius = target.getWidth() * 0.5 + 0.40, time = ticks * 3.5;
        int tiers = 3, perTier = 4;
        for (int tier = 0; tier < tiers; tier++) {
            double tierY = box.minY + height / (tiers + 1) * (tier + 1);
            double speed = (1.0 + tier * 0.3) * (tier % 2 == 0 ? -1.0 : 1.0);
            for (int i = 0; i < perTier; i++) {
                double angle = time * speed + i * (360.0 / perTier);
                double rad = Math.toRadians(angle);
                double x = cx + Math.cos(rad) * radius, z = cz + Math.sin(rad) * radius;
                double colorT = Math.sin(Math.toRadians(i * 30 + tier * 60 + ticks * 3)) * 0.5 + 0.5;
                crystalDirTo(out, e, x, tierY, z, cx - x, centerY - tierY, cz - z, 0.27f, angle * 2.0,
                        mix(espA(), espB(), colorT), mix(espB(), espA(), colorT));
            }
        }
    }

    private static float cubeCurve(float progress) {
        progress = Math.max(0f, Math.min(1f, progress));
        float slowStart = 0.62f, startSpeed = 1.16f;
        float startValue = slowStart * startSpeed;
        if (progress <= slowStart) return progress * startSpeed;
        float u = (progress - slowStart) / (1f - slowStart);
        float remaining = 1f - startValue;
        float startSlope = startSpeed * (1f - slowStart) / remaining;
        float u2 = u * u, u3 = u2 * u;
        float tail = (u3 - 2f * u2 + u) * startSlope + (-2f * u3 + 3f * u2);
        return startValue + remaining * tail;
    }

    // EnoughVisuals "Cube": a cube made of corner bars that flips 180 degrees on changing axes.
    private static void espCube(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                Box box, PlayerEntity target, double cx, double cz) {
        double cy = box.minY + (box.maxY - box.minY) * 0.52;
        long ms = System.currentTimeMillis();
        long idx = ms / 900L;
        float eased = cubeCurve(Math.min(1.0f, (ms % 900L) / 300.0f));
        int[] masks = {1, 2, 4, 3, 5, 6};
        int n = (int) (idx % 12L);
        double[] rot = new double[3];
        for (int k = 0; k < n; k++) {
            for (int a = 0; a < 3; a++) if ((masks[k % 6] & (1 << a)) != 0) rot[a] += 180.0;
        }
        for (int a = 0; a < 3; a++) if ((masks[n % 6] & (1 << a)) != 0) rot[a] += 180.0 * eased;

        int rgb = target.hurtTime > 0 ? mix(espA(), 0xFF4444, 0.85) : espA();
        double half = 0.26, bar = half * 0.9;
        for (int i = 0; i < 8; i++) {
            double[] c = {(i & 1) == 0 ? -half : half, (i & 2) == 0 ? -half : half, (i & 4) == 0 ? -half : half};
            double[] p0 = rotXYZ(c[0], c[1], c[2], rot[0], rot[1], rot[2]);
            for (int a = 0; a < 3; a++) {
                double[] end = c.clone();
                end[a] -= Math.signum(c[a]) * bar;
                double[] p1 = rotXYZ(end[0], end[1], end[2], rot[0], rot[1], rot[2]);
                glowLine(out, e, (float) (cx + p0[0]), (float) (cy + p0[1]), (float) (cz + p0[2]),
                        (float) (cx + p1[0]), (float) (cy + p1[1]), (float) (cz + p1[2]), rgb, 235, 3.2f);
            }
        }
    }

    // EnoughVisuals "Souls": glowing souls with trails on three tilted orbits.
    private static void espSouls(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                 Box box, PlayerEntity target, double cx, double cz) {
        double cy = box.minY + (box.maxY - box.minY) * 0.55;
        double r = 0.3 + target.getWidth() * 0.5 + 0.2;
        long ms = System.currentTimeMillis();
        int trail = 24;
        for (int k = 0; k < 3; k++) {
            int rgb = mix(espA(), espB(), k / 2.0);
            for (int i = 0; i < trail; i++) {
                double a1 = ms * 0.0042 + k * 2.09 - i * 0.075, a2 = ms * 0.0042 + k * 2.09 - (i + 1) * 0.075;
                double[] p1 = rotXYZ(Math.cos(a1) * r, 0, Math.sin(a1) * r, 40 + k * 20, k * 120.0, 0);
                double[] p2 = rotXYZ(Math.cos(a2) * r, 0, Math.sin(a2) * r, 40 + k * 20, k * 120.0, 0);
                double fade = 1.0 - i / (double) trail;
                glowLine(out, e, (float) (cx + p1[0]), (float) (cy + p1[1]), (float) (cz + p1[2]),
                        (float) (cx + p2[0]), (float) (cy + p2[1]), (float) (cz + p2[2]),
                        rgb, (int) Math.round(255 * fade), (float) (1.5 + 5.0 * fade));
            }
        }
    }

    // "Square": two counter-rotating squares in a camera-facing plane (the textured squares, as line-art).
    private static void espSquare(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                  double cx, double cy, double cz, double ticks) {
        double fl = Math.hypot(cx, cz);
        if (fl < 1.0e-4) fl = 1.0;
        double rx = cz / fl, rz = -(cx / fl);
        double ang = Math.toRadians(ticks * 4.0);
        for (int layer = 0; layer < 2; layer++) {
            double s = (layer == 0 ? 0.55 : 0.34) * 1.4142;
            double a0 = layer == 0 ? ang : -ang * 1.4 + Math.PI / 4;
            for (int i = 0; i < 4; i++) {
                double a1 = a0 + i * Math.PI / 2 + Math.PI / 4, a2 = a0 + (i + 1) * Math.PI / 2 + Math.PI / 4;
                double u1 = Math.cos(a1) * s, v1 = Math.sin(a1) * s, u2 = Math.cos(a2) * s, v2 = Math.sin(a2) * s;
                glowLine(out, e, (float) (cx + rx * u1), (float) (cy + v1), (float) (cz + rz * u1),
                        (float) (cx + rx * u2), (float) (cy + v2), (float) (cz + rz * u2),
                        layer == 0 ? espA() : espB(), 235, layer == 0 ? 3.5f : 2.5f);
            }
        }
    }

    // Style 1: rounded glowing brackets in a frame that always faces the camera.
    private static void espCorners(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                   Box box, double cx, double cy, double cz, double t) {
        double fl = Math.hypot(cx, cz);
        if (fl < 1.0e-4) fl = 1.0;
        double rx = (cz / fl), rz = -(cx / fl);
        double halfW = 0.62, halfH = (box.maxY - box.minY) / 2 + 0.12;
        double arm = 0.42, armV = 0.5, rad = 0.17;
        int alpha = 215 + (int) Math.round(40 * Math.sin(t * 5.0));
        for (int sxi = 0; sxi < 2; sxi++) for (int syi = 0; syi < 2; syi++) {
            double sx = sxi == 0 ? -1 : 1, sy = syi == 0 ? -1 : 1;
            java.util.List<double[]> pts = new java.util.ArrayList<>();
            pts.add(new double[]{sx * (halfW - arm), sy * halfH});
            for (int i = 0; i <= 8; i++) {
                double th = Math.PI / 2 * i / 8.0;
                pts.add(new double[]{sx * (halfW - rad) + sx * rad * Math.sin(th),
                        sy * (halfH - rad) + sy * rad * Math.cos(th)});
            }
            pts.add(new double[]{sx * halfW, sy * (halfH - armV)});
            for (int i = 0; i + 1 < pts.size(); i++) {
                double[] p = pts.get(i), q = pts.get(i + 1);
                glowLine(out, e,
                        (float) (cx + rx * p[0]), (float) (cy + p[1]), (float) (cz + rz * p[0]),
                        (float) (cx + rx * q[0]), (float) (cy + q[1]), (float) (cz + rz * q[0]),
                        espA(), alpha, 4.5f);
            }
        }
    }

    // Style 2: two glowing comets orbiting the target at changing heights.
    private static void espOrbit(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                 Box box, double cx, double cz, double t) {
        double h = box.maxY - box.minY, radius = 0.8;
        int trail = 26;
        for (int c = 0; c < 2; c++) {
            for (int i = 0; i < trail; i++) {
                double a1 = t * 2.4 + c * Math.PI - i * 0.07;
                double a2 = t * 2.4 + c * Math.PI - (i + 1) * 0.07;
                double y1 = box.minY + h * (0.5 + 0.4 * Math.sin(a1 * 0.9 + c * 2.0));
                double y2 = box.minY + h * (0.5 + 0.4 * Math.sin(a2 * 0.9 + c * 2.0));
                float fade = 1.0f - i / (float) trail;
                glowLine(out, e,
                        (float) (cx + Math.cos(a1) * radius), (float) y1, (float) (cz + Math.sin(a1) * radius),
                        (float) (cx + Math.cos(a2) * radius), (float) y2, (float) (cz + Math.sin(a2) * radius),
                        espA(), Math.round(255 * fade), 1.5f + 5.0f * fade);
            }
        }
    }

    // Style 3: a glowing purple cylinder band around the target's feet.
    private static void espRing(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                Box box, double cx, double cz, double t) {
        double r = 0.64 * (0.97 + 0.04 * Math.sin(t * 3.0));
        int layers = 6, segments = 48;
        for (int l = 0; l < layers; l++) {
            double y = box.minY + 0.03 + l * 0.07;
            float fade = 1.0f - l / (float) layers;
            for (int i = 0; i < segments; i++) {
                double a = Math.PI * 2 * i / segments, b = Math.PI * 2 * (i + 1) / segments;
                glowLine(out, e,
                        (float) (cx + Math.cos(a) * r), (float) y, (float) (cz + Math.sin(a) * r),
                        (float) (cx + Math.cos(b) * r), (float) y, (float) (cz + Math.sin(b) * r),
                        espB(), Math.round(230 * fade), 2.0f + 2.5f * fade);
            }
        }
    }

    // Style 4 (the original look): 3D corner brackets on the hitbox plus a pulsing ring at the feet.
    private static void espClassic(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                   Box box, double t) {
        float pulse = 0.78f + 0.22f * (float) Math.sin(t * 5.5);
        int alpha = 190 + Math.round(55 * pulse);
        float minX = (float) box.minX, maxX = (float) box.maxX;
        float minY = (float) box.minY, maxY = (float) box.maxY;
        float minZ = (float) box.minZ, maxZ = (float) box.maxZ;
        float width = maxX - minX, depth = maxZ - minZ, height = maxY - minY;
        float cornerX = Math.min(width * 0.32f, 0.16f);
        float cornerZ = Math.min(depth * 0.32f, 0.16f);
        float cornerY = Math.min(height * 0.16f, 0.28f);
        for (int xi = 0; xi < 2; xi++) for (int zi = 0; zi < 2; zi++) {
            float x = xi == 0 ? minX : maxX, z = zi == 0 ? minZ : maxZ;
            float sx = xi == 0 ? 1 : -1, sz = zi == 0 ? 1 : -1;
            for (int yi = 0; yi < 2; yi++) {
                float y = yi == 0 ? minY : maxY;
                float sy = yi == 0 ? 1 : -1;
                glowLine(out, e, x, y, z, x + sx * cornerX, y, z, espA(), alpha, 2.5f);
                glowLine(out, e, x, y, z, x, y, z + sz * cornerZ, espA(), alpha, 2.5f);
                glowLine(out, e, x, y, z, x, y + sy * cornerY, z, espA(), alpha, 2.5f);
            }
        }
        double radiusX = Math.max(0.42, width * 0.72) * (0.94 + pulse * 0.08);
        double radiusZ = Math.max(0.42, depth * 0.72) * (0.94 + pulse * 0.08);
        float ringY = minY + 0.035f;
        double rcx = minX + width / 2.0, rcz = minZ + depth / 2.0;
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40.0, b = Math.PI * 2 * (i + 1) / 40.0;
            glowLine(out, e,
                    (float) (rcx + Math.cos(a) * radiusX), ringY, (float) (rcz + Math.sin(a) * radiusZ),
                    (float) (rcx + Math.cos(b) * radiusX), ringY, (float) (rcz + Math.sin(b) * radiusZ),
                    espA(), Math.round(alpha * 0.78f), 2.0f);
        }
    }

    static int palA(int idx) { return PALETTE[Math.floorMod(idx, PALETTE.length)][0]; }

    static int palB(int idx) { return PALETTE[Math.floorMod(idx, PALETTE.length)][1]; }

    private static int espA() { return palA(espPalette); }

    private static int espB() { return palB(espPalette); }

    static int mix(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Euler rotation in degrees: X, then Y, then Z. */
    static double[] rotXYZ(double x, double y, double z, double rx, double ry, double rz) {
        double c = Math.cos(Math.toRadians(rx)), s = Math.sin(Math.toRadians(rx));
        double y1 = y * c - z * s, z1 = y * s + z * c;
        c = Math.cos(Math.toRadians(ry));
        s = Math.sin(Math.toRadians(ry));
        double x2 = x * c + z1 * s, z2 = -x * s + z1 * c;
        c = Math.cos(Math.toRadians(rz));
        s = Math.sin(Math.toRadians(rz));
        return new double[]{x2 * c - y1 * s, x2 * s + y1 * c, z2};
    }

    // A soft neon glow made of three overlapping lines: wide and faint, medium, thin and bright.
    static void glowLine(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                         float x1, float y1, float z1, float x2, float y2, float z2,
                         int rgb, int alpha, float width) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int core = mix(rgb, 0xFFFFFF, 0.45);
        line(out, e, x1, y1, z1, x2, y2, z2, r, g, b, Math.round(alpha * 0.14f), width * 3.2f);
        line(out, e, x1, y1, z1, x2, y2, z2, r, g, b, Math.round(alpha * 0.38f), width * 1.9f);
        line(out, e, x1, y1, z1, x2, y2, z2, (core >> 16) & 255, (core >> 8) & 255, core & 255, alpha, width);
    }

    /** An octahedral crystal whose tips point along (fx, fy, fz), rolled around that axis. */
    static void crystalDir(double px, double py, double pz, double fx, double fy, double fz,
                           float scale, double rollDeg, int rgbTop, int rgbBottom) {
        // used by cosmetics; the ESP crystals call the private overload with the live buffers
        crystalDirTo(CRYSTAL_OUT, CRYSTAL_ENTRY, px, py, pz, fx, fy, fz, scale, rollDeg, rgbTop, rgbBottom);
    }

    private static VertexConsumer CRYSTAL_OUT;
    private static net.minecraft.client.util.math.MatrixStack.Entry CRYSTAL_ENTRY;

    static void bindBuffers(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry entry) {
        CRYSTAL_OUT = out;
        CRYSTAL_ENTRY = entry;
    }

    private static void crystalDirTo(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry e,
                                     double px, double py, double pz, double fx, double fy, double fz,
                                     float scale, double rollDeg, int rgbTop, int rgbBottom) {
        if (out == null || e == null) return;
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (fl < 1.0e-6) return;
        fx /= fl;
        fy /= fl;
        fz /= fl;
        double ux = -fz, uy = 0, uz = fx;
        double ul = Math.sqrt(ux * ux + uz * uz);
        if (ul < 1.0e-4) { ux = 1; uz = 0; ul = 1; }
        ux /= ul;
        uz /= ul;
        double vx = fy * uz - fz * uy, vy = fz * ux - fx * uz, vz = fx * uy - fy * ux;
        double c = Math.cos(Math.toRadians(rollDeg)), s = Math.sin(Math.toRadians(rollDeg));
        double aX = ux * c + vx * s, aY = uy * c + vy * s, aZ = uz * c + vz * s;
        double bX = -ux * s + vx * c, bY = -uy * s + vy * c, bZ = -uz * s + vz * c;
        double h = scale, w = scale * 0.5;
        double[][] eq = {
                {px + aX * w, py + aY * w, pz + aZ * w}, {px + bX * w, py + bY * w, pz + bZ * w},
                {px - aX * w, py - aY * w, pz - aZ * w}, {px - bX * w, py - bY * w, pz - bZ * w}};
        double[] top = {px + fx * h, py + fy * h, pz + fz * h};
        double[] bot = {px - fx * h, py - fy * h, pz - fz * h};
        for (int i = 0; i < 4; i++) {
            double[] q = eq[i], n = eq[(i + 1) % 4];
            glowLine(out, e, (float) top[0], (float) top[1], (float) top[2], (float) q[0], (float) q[1], (float) q[2], rgbTop, 235, 3.0f);
            glowLine(out, e, (float) bot[0], (float) bot[1], (float) bot[2], (float) q[0], (float) q[1], (float) q[2], rgbBottom, 235, 3.0f);
            glowLine(out, e, (float) q[0], (float) q[1], (float) q[2], (float) n[0], (float) n[1], (float) n[2],
                    mix(rgbTop, rgbBottom, 0.5), 235, 3.0f);
        }
    }

    static void line(VertexConsumer out, net.minecraft.client.util.math.MatrixStack.Entry entry,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int r, int g, int b, int alpha, float width) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0e-5f) return;
        float nx = dx / length, ny = dy / length, nz = dz / length;
        int a = Math.max(0, Math.min(255, alpha));
        out.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, nx, ny, nz).lineWidth(width);
        out.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, nx, ny, nz).lineWidth(width);
    }

    private static void renderTargetHud(DrawContext context, MinecraftClient client, PlayerEntity target) {
        int x = client.getWindow().getScaledWidth() - 174;
        int y = 10;
        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float healthRatio = Math.max(0.0f, Math.min(1.0f, target.getHealth() / maxHealth));
        context.fill(x, y, x + 164, y + 57, 0xD9121511);
        context.fill(x, y, x + 2, y + 57, 0xFFC7F36B);
        context.drawTextWithShadow(client.textRenderer, "TARGET", x + 9, y + 7, 0xFFC7F36B);
        context.drawTextWithShadow(client.textRenderer, target.getName().getString(), x + 53, y + 7, 0xFFFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
                String.format("HP %.1f / %.0f   ARM %d", target.getHealth(), maxHealth, target.getArmor()),
                x + 9, y + 22, 0xFFE5EBDD);
        context.fill(x + 9, y + 39, x + 154, y + 44, 0xFF30362D);
        context.fill(x + 9, y + 39, x + 9 + Math.round(145 * healthRatio), y + 44, 0xFFC7F36B);
        context.drawTextWithShadow(client.textRenderer,
                String.format("DIST  %.1f m", target.distanceTo(client.player)), x + 9, y + 46, 0xFF9AA392);
    }

    private static void drawAspectBars(DrawContext context, MinecraftClient client) {
        int index = aspectRatioIndex;
        if (index <= 0 || index >= ASPECT_RATIOS.length) return;
        float[] ratios = {0f, 16f / 9f, 16f / 10f, 4f / 3f, 21f / 9f};
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float desired = ratios[index];
        float current = (float) width / height;
        if (current > desired) {
            int viewportWidth = Math.round(height * desired);
            int side = (width - viewportWidth) / 2;
            context.fill(0, 0, side, height, 0xFF000000);
            context.fill(width - side, 0, width, height, 0xFF000000);
        } else {
            int viewportHeight = Math.round(width / desired);
            int edge = (height - viewportHeight) / 2;
            context.fill(0, 0, width, edge, 0xFF000000);
            context.fill(0, height - edge, width, height, 0xFF000000);
        }
    }

    private static void key(DrawContext context, MinecraftClient client, String label, KeyBinding binding, int x, int y) {
        int color = binding.isPressed() ? 0xFFC7F36B : 0xFFE5EBDD;
        context.fill(x, y, x + (label.equals("JUMP") ? 43 : 19), y + 11, binding.isPressed() ? 0xAA42562D : 0xAA272C25);
        context.drawCenteredTextWithShadow(client.textRenderer, label, x + (label.equals("JUMP") ? 21 : 9), y + 2, color);
    }
}
