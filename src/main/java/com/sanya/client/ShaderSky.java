package com.sanya.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * "Shader Sky" from EnoughVisuals, re-made for 1.21.11 as animated neon line-art on a sky dome.
 * (The original uses GLSL shaders for the 1.21.4 render pipeline, which no longer exists.)
 * The lines are depth-tested, so terrain hides them like a real sky. Best seen at night.
 */
public final class ShaderSky {
    private ShaderSky() { }

    public static boolean enabled = false;
    public static int mode = 0, palette = 0, speed = 1, opacity = 3;

    public static final String[] MODES = {"Aurora", "Night", "Snow", "Sky", "Star", "Glow", "Caustic", "Clouds"};
    public static final String[] SPEEDS = {"0.5x", "1x", "2x", "3x"};
    private static final double[] SPEED_V = {0.5, 1.0, 2.0, 3.0};
    public static final String[] OPACITIES = {"25%", "50%", "75%", "100%"};
    private static final double[] OPACITY_V = {0.25, 0.5, 0.75, 1.0};

    private static final int STARS = 260;
    private static final double[] STAR_AZ = new double[STARS], STAR_EL = new double[STARS], STAR_PH = new double[STARS];
    private static final int BIG = 14;
    private static final double[] BIG_AZ = new double[BIG], BIG_EL = new double[BIG], BIG_PH = new double[BIG];
    private static final int CLOUDS = 22;
    private static final double[] CL_AZ = new double[CLOUDS], CL_EL = new double[CLOUDS], CL_SZ = new double[CLOUDS];
    private static final int FLAKES = 150;
    private static final double[] FL_X = new double[FLAKES], FL_Y = new double[FLAKES], FL_Z = new double[FLAKES], FL_PH = new double[FLAKES];

    static {
        Random rnd = new Random(1337);
        for (int i = 0; i < STARS; i++) {
            STAR_AZ[i] = rnd.nextDouble() * Math.PI * 2;
            STAR_EL[i] = Math.asin(0.04 + rnd.nextDouble() * 0.96);
            STAR_PH[i] = rnd.nextDouble() * Math.PI * 2;
        }
        for (int i = 0; i < BIG; i++) {
            BIG_AZ[i] = rnd.nextDouble() * Math.PI * 2;
            BIG_EL[i] = Math.toRadians(14 + rnd.nextDouble() * 62);
            BIG_PH[i] = rnd.nextDouble() * Math.PI * 2;
        }
        for (int i = 0; i < CLOUDS; i++) {
            CL_AZ[i] = rnd.nextDouble() * Math.PI * 2;
            CL_EL[i] = Math.toRadians(9 + rnd.nextDouble() * 42);
            CL_SZ[i] = 0.10 + rnd.nextDouble() * 0.12;
        }
        for (int i = 0; i < FLAKES; i++) {
            FL_X[i] = rnd.nextDouble() * 28.0;
            FL_Y[i] = rnd.nextDouble() * 18.0;
            FL_Z[i] = rnd.nextDouble() * 28.0;
            FL_PH[i] = rnd.nextDouble() * Math.PI * 2;
        }
    }

    private static VertexConsumer out;
    private static MatrixStack.Entry entry;
    private static double radius, alphaMul, t;
    private static int colA, colB;

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!enabled || mc.player == null || mc.world == null || ctx.consumers() == null) return;

        out = ctx.consumers().getBuffer(RenderLayers.lines());
        entry = ctx.matrices().peek();
        int viewDistance = mc.options.getViewDistance().getValue();
        radius = Math.max(40.0, Math.min(96.0, viewDistance * 16 * 0.85));
        colA = SanyaClient.palA(palette);
        colB = SanyaClient.palB(palette);
        alphaMul = OPACITY_V[Math.floorMod(opacity, OPACITY_V.length)];
        t = System.currentTimeMillis() / 1000.0 * SPEED_V[Math.floorMod(speed, SPEED_V.length)];
        Vec3d cam = ctx.worldState().cameraRenderState.pos;

        switch (mode) {
            case 0 -> aurora();
            case 1 -> night();
            case 2 -> snow(cam);
            case 3 -> skyBands();
            case 4 -> bigStars();
            case 5 -> glow();
            case 6 -> caustic();
            default -> clouds();
        }
    }

    // ------------------------------------------------------------ helpers

    private static double[] dome(double az, double el) {
        double ce = Math.cos(el);
        return new double[]{radius * ce * Math.sin(az), radius * Math.sin(el), radius * ce * Math.cos(az)};
    }

    private static void ln(double az1, double el1, double az2, double el2, int rgb, double alpha, float w) {
        double[] a = dome(az1, el1), b = dome(az2, el2);
        SanyaClient.line(out, entry, (float) a[0], (float) a[1], (float) a[2], (float) b[0], (float) b[1], (float) b[2],
                (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) Math.round(alpha * alphaMul), w);
    }

    /** A faint wide line plus a bright thin one: cheap glow. */
    private static void soft(double az1, double el1, double az2, double el2, int rgb, double alpha, float w) {
        ln(az1, el1, az2, el2, rgb, alpha * 0.25, w * 2.6f);
        ln(az1, el1, az2, el2, rgb, alpha, w);
    }

    private static void ringAt(double el, int segments, int rgb, double alpha, float w) {
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2 * i / segments, b = Math.PI * 2 * (i + 1) / segments;
            ln(a, el, b, el, rgb, alpha, w);
        }
    }

    // ------------------------------------------------------------ modes

    private static void aurora() {
        for (int c = 0; c < 3; c++) {
            double center = c * 2.1;
            for (int i = 0; i < 28; i++) {
                double u = i / 27.0, az = center + (u - 0.5) * 1.8;
                double base = Math.toRadians(14 + 7 * Math.sin(az * 3 + t * 0.35 + c));
                double top = base + Math.toRadians(26 + 10 * Math.sin(az * 5 - t * 0.6 + c * 2));
                double edge = Math.sin(u * Math.PI);
                double shimmer = 0.6 + 0.4 * Math.sin(t * 1.3 + u * 6 + c);
                for (int s = 0; s < 4; s++) {
                    double e1 = base + (top - base) * s / 4.0, e2 = base + (top - base) * (s + 1) / 4.0;
                    double fade = 1.0 - s / 4.0;
                    double[] p1 = dome(az, e1), p2 = dome(az, e2);
                    SanyaClient.glowLine(out, entry, (float) p1[0], (float) p1[1], (float) p1[2],
                            (float) p2[0], (float) p2[1], (float) p2[2],
                            SanyaClient.mix(colA, colB, s / 3.0),
                            (int) Math.round(210 * edge * fade * shimmer * alphaMul), 5.0f);
                }
            }
        }
    }

    private static void night() {
        for (int i = 0; i < STARS; i++) {
            double tw = 0.5 + 0.5 * Math.sin(t * 1.7 + STAR_PH[i]);
            double d = 0.010 + 0.006 * (i % 3);
            int rgb = SanyaClient.mix(0xFFFFFF, i % 5 == 0 ? colA : colB, 0.25);
            double az = STAR_AZ[i], el = STAR_EL[i];
            ln(az - d, el, az + d, el, rgb, 90 + 165 * tw, 2.2f);
            ln(az, el - d, az, el + d, rgb, 90 + 165 * tw, 2.2f);
        }
        // moon: two soft rings
        double maz = 2.4, mel = Math.toRadians(38);
        for (int k = 0; k < 2; k++) {
            double r = 0.07 - k * 0.025;
            for (int i = 0; i < 24; i++) {
                double a1 = Math.PI * 2 * i / 24, a2 = Math.PI * 2 * (i + 1) / 24;
                soft(maz + Math.cos(a1) * r, mel + Math.sin(a1) * r, maz + Math.cos(a2) * r, mel + Math.sin(a2) * r,
                        SanyaClient.mix(0xFFFFFF, colA, 0.3), 230, 3.0f);
            }
        }
        // shooting star every ~7 seconds
        double phase = (t % 7.0) / 1.1;
        if (phase < 1.0) {
            double az0 = 0.6 + Math.floor(t / 7.0) * 1.9, el0 = Math.toRadians(55);
            for (int i = 0; i < 8; i++) {
                double f1 = Math.max(0, phase - i * 0.03), f2 = Math.max(0, phase - (i + 1) * 0.03);
                soft(az0 + f1 * 0.7, el0 - f1 * 0.45, az0 + f2 * 0.7, el0 - f2 * 0.45, 0xFFFFFF, 240 * (1 - i / 8.0), 3.0f);
            }
        }
    }

    private static void snow(Vec3d cam) {
        int rgb = SanyaClient.mix(0xFFFFFF, colA, 0.2);
        for (int i = 0; i < FLAKES; i++) {
            double x = Math.floorMod((long) Math.floor(FL_X[i] - cam.x), 28L) + ((FL_X[i] - cam.x) % 1.0 + 1.0) % 1.0 - 14.0;
            double z = Math.floorMod((long) Math.floor(FL_Z[i] - cam.z), 28L) + ((FL_Z[i] - cam.z) % 1.0 + 1.0) % 1.0 - 14.0;
            double fall = (FL_Y[i] - t * 1.6 - cam.y) % 18.0;
            if (fall < 0) fall += 18.0;
            double y = fall - 9.0;
            x += Math.sin(t * 0.7 + FL_PH[i]) * 0.5;
            z += Math.cos(t * 0.6 + FL_PH[i]) * 0.5;
            float s = 0.06f;
            int a = (int) Math.round(215 * alphaMul);
            SanyaClient.line(out, entry, (float) x - s, (float) y, (float) z, (float) x + s, (float) y, (float) z,
                    (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a, 3.0f);
            SanyaClient.line(out, entry, (float) x, (float) y - s, (float) z, (float) x, (float) y + s, (float) z,
                    (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a, 3.0f);
            SanyaClient.line(out, entry, (float) x, (float) y, (float) z - s, (float) x, (float) y, (float) z + s,
                    (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a, 3.0f);
        }
    }

    private static void skyBands() {
        for (int i = 0; i < 10; i++) {
            double el = Math.toRadians(2 + i * 8.5);
            int rgb = SanyaClient.mix(colA, colB, (i / 9.0 + 0.15 * Math.sin(t * 0.2 + i * 0.4) + 0.15));
            ringAt(el, 40, rgb, 80, 38.0f);
        }
    }

    private static void bigStars() {
        for (int i = 0; i < BIG; i++) {
            double pulse = 0.55 + 0.45 * Math.sin(t * 1.4 + BIG_PH[i]);
            double az = BIG_AZ[i], el = BIG_EL[i];
            double L = 0.045 + 0.035 * pulse, d = L * 0.55;
            int rgb = SanyaClient.mix(colA, 0xFFFFFF, 0.35);
            soft(az - L, el, az + L, el, rgb, 140 + 115 * pulse, 3.2f);
            soft(az, el - L, az, el + L, rgb, 140 + 115 * pulse, 3.2f);
            soft(az - d, el - d, az + d, el + d, colB, 90 + 100 * pulse, 2.2f);
            soft(az - d, el + d, az + d, el - d, colB, 90 + 100 * pulse, 2.2f);
        }
    }

    private static void glow() {
        double[] els = {2, 7, 13};
        for (int k = 0; k < els.length; k++) {
            double pulse = 0.6 + 0.4 * Math.sin(t * 1.1 + k * 1.3);
            ringAt(Math.toRadians(els[k]), 48, SanyaClient.mix(colA, colB, k / 2.0), 95 * pulse, 30.0f - k * 6.0f);
        }
        for (int i = 0; i < 12; i++) {
            double az = i * Math.PI / 6 + t * 0.1;
            double top = Math.toRadians(24 + 14 * Math.sin(t * 0.8 + i));
            for (int s = 0; s < 4; s++) {
                double e1 = Math.toRadians(2) + (top - Math.toRadians(2)) * s / 4.0;
                double e2 = Math.toRadians(2) + (top - Math.toRadians(2)) * (s + 1) / 4.0;
                soft(az, e1, az, e2, colA, 150 * (1 - s / 4.0), 4.0f);
            }
        }
    }

    private static void caustic() {
        for (int lat = 0; lat < 9; lat++) {
            double base = Math.toRadians(10 + lat * 8.5);
            for (int j = 0; j < 60; j++) {
                double a1 = Math.PI * 2 * j / 60, a2 = Math.PI * 2 * (j + 1) / 60;
                double e1 = base + Math.toRadians(2.5) * Math.sin(a1 * 6 + t * 1.2 + lat);
                double e2 = base + Math.toRadians(2.5) * Math.sin(a2 * 6 + t * 1.2 + lat);
                soft(a1, e1, a2, e2, SanyaClient.mix(colA, colB, lat / 8.0), 160, 1.8f);
            }
        }
        for (int m = 0; m < 16; m++) {
            double az = m * Math.PI / 8;
            for (int s = 0; s < 14; s++) {
                double e1 = Math.toRadians(5 + s * 6), e2 = Math.toRadians(5 + (s + 1) * 6);
                soft(az + Math.toRadians(1.5) * Math.sin(e1 * 8 + t), e1, az + Math.toRadians(1.5) * Math.sin(e2 * 8 + t), e2,
                        colB, 110, 1.6f);
            }
        }
    }

    private static void clouds() {
        int rgb = SanyaClient.mix(0xFFFFFF, colA, 0.25);
        for (int i = 0; i < CLOUDS; i++) {
            double az = CL_AZ[i] + t * 0.01, el = CL_EL[i], size = CL_SZ[i];
            for (int k = 0; k < 5; k++) {
                double len = size * (1.0 - Math.abs(k - 2) * 0.28);
                double e = el + k * 0.018;
                ln(az - len, e, az + len, e, rgb, 70 + 12 * k, 18.0f);
            }
        }
    }
}
