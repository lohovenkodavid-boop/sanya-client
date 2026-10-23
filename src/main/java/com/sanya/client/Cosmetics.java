package com.sanya.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Own cosmetics (hat, wings, pet, aura), drawn as neon line-art around the local player.
 * Visible only to you, only from third person. Not part of EnoughVisuals' model catalog.
 */
public final class Cosmetics {
    private Cosmetics() { }

    public static boolean enabled = true;
    public static int hat, wings, pet, aura, palette;

    public static final String[] HATS = {"None", "Halo", "China Hat", "Crown", "Horns"};
    public static final String[] WINGS = {"None", "Angel", "Demon", "Butterfly", "Techno"};
    public static final String[] PETS = {"None", "Crystal", "Cube", "Spirit"};
    public static final String[] AURAS = {"None", "Rings", "Helix", "Pulse"};

    private static VertexConsumer out;
    private static MatrixStack.Entry entry;
    private static int colA, colB;

    // ------------------------------------------------------------ wing shapes (also used by the menu preview)

    /** Polylines of the RIGHT wing as flattened (u, v) pairs in metres: u = outwards, v = up. */
    public static List<double[]> wingShape(int type) {
        List<double[]> shape = new ArrayList<>();
        switch (type) {
            case 1 -> { // Angel
                int n = 6;
                double[] arm = new double[(n + 1) * 2];
                double[] tips = new double[(n + 1) * 2];
                for (int k = 0; k <= n; k++) {
                    double s = k / (double) n;
                    double au = 0.08 + 0.95 * s;
                    double av = 0.38 + 0.30 * Math.sin(s * 1.9);
                    double tu = au + 0.10 + 0.12 * s;
                    double tv = av - (0.80 - 0.42 * s);
                    arm[k * 2] = au;
                    arm[k * 2 + 1] = av;
                    tips[k * 2] = tu;
                    tips[k * 2 + 1] = tv;
                    shape.add(new double[]{au, av, tu, tv});
                }
                shape.add(0, arm);
                shape.add(1, tips);
            }
            case 2 -> { // Demon
                double[] sh = {0.05, 0.35}, el = {0.55, 0.75}, wr = {1.05, 0.50};
                double[][] tp = {{1.15, -0.25}, {0.85, -0.55}, {0.50, -0.50}, {0.08, -0.20}};
                shape.add(new double[]{sh[0], sh[1], el[0], el[1], wr[0], wr[1]});
                for (int i = 0; i < 3; i++) shape.add(new double[]{wr[0], wr[1], tp[i][0], tp[i][1]});
                shape.add(new double[]{el[0], el[1], tp[3][0], tp[3][1]});
                double[] mem = new double[(tp.length * 2 - 1) * 2];
                int p = 0;
                for (int i = 0; i < tp.length; i++) {
                    mem[p++] = tp[i][0];
                    mem[p++] = tp[i][1];
                    if (i + 1 < tp.length) {
                        double mu = (tp[i][0] + tp[i + 1][0]) / 2, mv = (tp[i][1] + tp[i + 1][1]) / 2;
                        mem[p++] = mu + (0.55 - mu) * 0.18;
                        mem[p++] = mv + (0.20 - mv) * 0.18;
                    }
                }
                shape.add(mem);
            }
            case 3 -> { // Butterfly
                shape.add(ellipse(0.50, 0.45, 0.50, 0.38, 1.0));
                shape.add(ellipse(0.50, 0.45, 0.50, 0.38, 0.55));
                shape.add(ellipse(0.38, -0.10, 0.34, 0.30, 1.0));
                shape.add(ellipse(0.38, -0.10, 0.34, 0.30, 0.55));
            }
            case 4 -> { // Techno
                for (int k = 0; k < 4; k++) {
                    double bv = 0.62 - 0.20 * k;
                    shape.add(new double[]{0.08, bv, 1.0 - 0.15 * k, 0.72 - 0.28 * k, 0.08, bv - 0.16, 0.08, bv});
                }
            }
            default -> { }
        }
        return shape;
    }

    private static double[] ellipse(double cu, double cv, double ru, double rv, double scale) {
        int n = 20;
        double[] pts = new double[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            pts[i * 2] = cu + Math.cos(a) * ru * scale;
            pts[i * 2 + 1] = cv + Math.sin(a) * rv * scale;
        }
        return pts;
    }

    // ------------------------------------------------------------ world rendering

    public static void render(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!enabled || (hat == 0 && wings == 0 && pet == 0 && aura == 0)) return;
        if (client.player == null || client.world == null || ctx.consumers() == null) return;
        if (client.options.getPerspective().isFirstPerson()) return;
        PlayerEntity player = client.player;
        if (player.isInvisible() || player.isSpectator()) return;

        float tp = client.getRenderTickCounter().getTickProgress(false);
        Vec3d cam = ctx.worldState().cameraRenderState.pos;
        Vec3d lp = player.getLerpedPos(tp);
        out = ctx.consumers().getBuffer(RenderLayers.lines());
        entry = ctx.matrices().peek();
        SanyaClient.bindBuffers(out, entry);
        colA = SanyaClient.palA(palette);
        colB = SanyaClient.palB(palette);

        double px = lp.x - cam.x, py = lp.y - cam.y, pz = lp.z - cam.z;
        double ticks = player.age + tp;
        double h = player.getHeight();
        double yaw = Math.toRadians(player.bodyYaw);
        double fwdX = -Math.sin(yaw), fwdZ = Math.cos(yaw);
        double rightX = -Math.cos(yaw), rightZ = -Math.sin(yaw);

        drawHat(px, py, pz, h, rightX, rightZ, ticks);
        drawWings(px, py, pz, h, fwdX, fwdZ, rightX, rightZ, ticks);
        drawPet(px, py, pz, h, ticks);
        drawAura(px, py, pz, h, ticks);
    }

    private static void seg(double x1, double y1, double z1, double x2, double y2, double z2,
                            int rgb, int alpha, float width) {
        SanyaClient.glowLine(out, entry, (float) x1, (float) y1, (float) z1,
                (float) x2, (float) y2, (float) z2, rgb, alpha, width);
    }

    private static void ring(double cx, double cy, double cz, double r, int n, int rgb, int alpha, float width) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n, b = Math.PI * 2 * (i + 1) / n;
            seg(cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r,
                    cx + Math.cos(b) * r, cy, cz + Math.sin(b) * r, rgb, alpha, width);
        }
    }

    private static void drawHat(double px, double py, double pz, double h, double rightX, double rightZ, double ticks) {
        double top = py + h;
        switch (hat) {
            case 1 -> {
                double y = top + 0.30 + 0.035 * Math.sin(ticks * 0.12);
                ring(px, y, pz, 0.30, 28, colA, 235, 3.2f);
                ring(px, y, pz, 0.24, 24, colB, 150, 2.0f);
            }
            case 2 -> {
                double apexY = top + 0.42, baseY = top + 0.02, spin = ticks * 0.02;
                int n = 12;
                for (int i = 0; i < n; i++) {
                    double a = spin + i * Math.PI * 2 / n;
                    seg(px, apexY, pz, px + Math.cos(a) * 0.62, baseY, pz + Math.sin(a) * 0.62, colA, 200, 2.2f);
                }
                ring(px, baseY, pz, 0.62, 32, colB, 230, 2.8f);
            }
            case 3 -> {
                ring(px, top + 0.03, pz, 0.27, 24, colB, 220, 2.4f);
                for (int i = 0; i < 10; i++) {
                    double a1 = i * Math.PI / 5, a2 = (i + 1) * Math.PI / 5;
                    double y1 = top + 0.03 + (i % 2 == 0 ? 0.24 : 0.07);
                    double y2 = top + 0.03 + ((i + 1) % 2 == 0 ? 0.24 : 0.07);
                    seg(px + Math.cos(a1) * 0.27, y1, pz + Math.sin(a1) * 0.27,
                            px + Math.cos(a2) * 0.27, y2, pz + Math.sin(a2) * 0.27, colA, 235, 3.0f);
                }
            }
            case 4 -> {
                for (int side = -1; side <= 1; side += 2) {
                    double lx = 0, ly = 0, lz = 0;
                    for (int s = 0; s <= 8; s++) {
                        double t = s / 8.0;
                        double off = 0.22 + 0.20 * Math.sin(t * 1.3);
                        double x = px + rightX * side * off, y = top - 0.05 + 0.42 * t, z = pz + rightZ * side * off;
                        if (s > 0) seg(lx, ly, lz, x, y, z, colB, 235, 3.2f);
                        lx = x;
                        ly = y;
                        lz = z;
                    }
                }
            }
            default -> { }
        }
    }

    private static void drawWings(double px, double py, double pz, double h, double fwdX, double fwdZ,
                                  double rightX, double rightZ, double ticks) {
        if (wings == 0) return;
        List<double[]> shape = wingShape(wings);
        double phi = 0.40 + 0.16 * Math.sin(ticks * 0.14);
        double cphi = Math.cos(phi), sphi = Math.sin(phi);
        double ax = px - fwdX * 0.16, ay = py + h * 0.72, az = pz - fwdZ * 0.16;
        for (int side = -1; side <= 1; side += 2) {
            for (int pi = 0; pi < shape.size(); pi++) {
                double[] poly = shape.get(pi);
                double[] prev = null;
                for (int i = 0; i + 1 < poly.length; i += 2) {
                    double u = poly[i], v = poly[i + 1];
                    double x = ax + rightX * side * (0.06 + u * cphi) - fwdX * (u * sphi);
                    double y = ay + v;
                    double z = az + rightZ * side * (0.06 + u * cphi) - fwdZ * (u * sphi);
                    if (prev != null) seg(prev[0], prev[1], prev[2], x, y, z, pi % 2 == 0 ? colA : colB, 230, 2.6f);
                    prev = new double[]{x, y, z};
                }
            }
        }
    }

    private static void drawPet(double px, double py, double pz, double h, double ticks) {
        if (pet == 0) return;
        double a = ticks * 0.05;
        double r = 0.95 + 0.08 * Math.sin(ticks * 0.07);
        double hy = py + h * 0.85 + 0.12 * Math.sin(ticks * 0.13);
        switch (pet) {
            case 1 -> SanyaClient.crystalDir(px + Math.cos(a) * r, hy, pz + Math.sin(a) * r,
                    0, 1, 0, 0.16f, ticks * 7.0, colA, colB);
            case 2 -> cube(px + Math.cos(a) * r, hy, pz + Math.sin(a) * r, 0.09, ticks * 3.0, colA);
            case 3 -> {
                int trail = 22;
                for (int i = 0; i < trail; i++) {
                    double a1 = a - i * 0.09, a2 = a - (i + 1) * 0.09;
                    double fade = 1.0 - i / (double) trail;
                    seg(px + Math.cos(a1) * r, hy, pz + Math.sin(a1) * r,
                            px + Math.cos(a2) * r, hy, pz + Math.sin(a2) * r,
                            SanyaClient.mix(colA, colB, i / (double) trail), (int) Math.round(235 * fade),
                            (float) (1.5 + 4.0 * fade));
                }
            }
            default -> { }
        }
    }

    private static void cube(double cx, double cy, double cz, double half, double rot, int rgb) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -half : half, y = (i & 2) == 0 ? -half : half, z = (i & 4) == 0 ? -half : half;
            double[] r = SanyaClient.rotXYZ(x, y, z, rot, rot * 1.3, rot * 0.7);
            c[i] = new double[]{cx + r[0], cy + r[1], cz + r[2]};
        }
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit < 8; bit <<= 1) {
                if ((i & bit) == 0) {
                    int j = i | bit;
                    seg(c[i][0], c[i][1], c[i][2], c[j][0], c[j][1], c[j][2], rgb, 230, 2.4f);
                }
            }
        }
    }

    private static void drawAura(double px, double py, double pz, double h, double ticks) {
        switch (aura) {
            case 1 -> {
                for (int k = 0; k < 3; k++) {
                    double f = (ticks * 0.02 + k / 3.0) % 1.0;
                    ring(px, py + f * h, pz, 0.55 + 0.08 * Math.sin(f * Math.PI * 2), 28, colA,
                            (int) Math.round(230 * Math.sin(f * Math.PI)), 2.6f);
                }
            }
            case 2 -> {
                int n = 40;
                for (int strand = 0; strand < 2; strand++) {
                    double lx = 0, ly = 0, lz = 0;
                    for (int i = 0; i <= n; i++) {
                        double t = i / (double) n;
                        double a = t * Math.PI * 6 + ticks * 0.08 + strand * Math.PI;
                        double x = px + Math.cos(a) * 0.58, y = py + t * h, z = pz + Math.sin(a) * 0.58;
                        if (i > 0) seg(lx, ly, lz, x, y, z, strand == 0 ? colA : colB,
                                (int) Math.round(230 * Math.sin(t * Math.PI)), 2.4f);
                        lx = x;
                        ly = y;
                        lz = z;
                    }
                }
            }
            case 3 -> {
                for (int k = 0; k < 2; k++) {
                    double f = (ticks * 0.025 + k * 0.5) % 1.0;
                    ring(px, py + 0.03, pz, 0.2 + f * 1.5, 40, k == 0 ? colA : colB,
                            (int) Math.round(235 * (1.0 - f)), 3.0f);
                }
            }
            default -> { }
        }
    }
}
