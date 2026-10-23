package com.sanya.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saves and loads all SANЯ CLIENT settings to config/sanya_client.properties. */
public final class SanyaConfig {
    private SanyaConfig() { }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("sanya_client.properties");
    }

    private static boolean bool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int integer(Properties p, String key, int def, int size) {
        try {
            int v = Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
            return v >= 0 && v < size ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static void load() {
        Path path = file();
        if (!Files.exists(path)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            System.err.println("[SANЯ CLIENT] Не вдалося прочитати конфіг: " + e);
            return;
        }
        SanyaClient.hudEnabled = bool(p, "hud", SanyaClient.hudEnabled);
        SanyaClient.keystrokesEnabled = bool(p, "keystrokes", SanyaClient.keystrokesEnabled);
        SanyaClient.coordinatesEnabled = bool(p, "coordinates", SanyaClient.coordinatesEnabled);
        SanyaClient.lowParticlesEnabled = bool(p, "lowParticles", SanyaClient.lowParticlesEnabled);
        SanyaClient.cpsEnabled = bool(p, "cps", SanyaClient.cpsEnabled);
        SanyaClient.fullBrightEnabled = bool(p, "fullBright", SanyaClient.fullBrightEnabled);
        SanyaClient.hitColorEnabled = bool(p, "hitColor", SanyaClient.hitColorEnabled);
        SanyaClient.armorHudEnabled = bool(p, "armorHud", SanyaClient.armorHudEnabled);
        SanyaClient.effectsHudEnabled = bool(p, "effectsHud", SanyaClient.effectsHudEnabled);
        SanyaClient.cooldownHudEnabled = bool(p, "cooldownHud", SanyaClient.cooldownHudEnabled);
        SanyaClient.targetHudEnabled = bool(p, "targetHud", SanyaClient.targetHudEnabled);
        SanyaClient.targetEspEnabled = bool(p, "targetEsp", SanyaClient.targetEspEnabled);
        SanyaClient.echoHandsEnabled = bool(p, "echoHands", SanyaClient.echoHandsEnabled);
        SanyaClient.aspectRatioIndex = integer(p, "aspectRatio", SanyaClient.aspectRatioIndex, SanyaClient.ASPECT_RATIOS.length);
        SanyaClient.targetEspMode = integer(p, "espStyle", SanyaClient.targetEspMode, SanyaClient.ESP_MODES.length);
        SanyaClient.timeMode = integer(p, "timeMode", SanyaClient.timeMode, SanyaClient.TIME_MODES.length);
        SanyaClient.weatherMode = integer(p, "weatherMode", SanyaClient.weatherMode, SanyaClient.WEATHER_MODES.length);
        UiFont.enabled = bool(p, "smoothFont", UiFont.enabled);
        HudRenderer.hudPalette = integer(p, "hudPalette", HudRenderer.hudPalette, SanyaClient.PALETTE_NAMES.length);
        SanyaClient.espPalette = integer(p, "espPalette", SanyaClient.espPalette, SanyaClient.PALETTE_NAMES.length);
        ShaderSky.enabled = bool(p, "shaderSky", ShaderSky.enabled);
        ShaderSky.mode = integer(p, "skyMode", ShaderSky.mode, ShaderSky.MODES.length);
        ShaderSky.palette = integer(p, "skyPalette", ShaderSky.palette, SanyaClient.PALETTE_NAMES.length);
        ShaderSky.speed = integer(p, "skySpeed", ShaderSky.speed, ShaderSky.SPEEDS.length);
        ShaderSky.opacity = integer(p, "skyOpacity", ShaderSky.opacity, ShaderSky.OPACITIES.length);
        Cosmetics.enabled = bool(p, "cosmetics", Cosmetics.enabled);
        Cosmetics.hat = integer(p, "cosmeticHat", Cosmetics.hat, Cosmetics.HATS.length);
        Cosmetics.wings = integer(p, "cosmeticWings", Cosmetics.wings, Cosmetics.WINGS.length);
        Cosmetics.pet = integer(p, "cosmeticPet", Cosmetics.pet, Cosmetics.PETS.length);
        Cosmetics.aura = integer(p, "cosmeticAura", Cosmetics.aura, Cosmetics.AURAS.length);
        Cosmetics.palette = integer(p, "cosmeticPalette", Cosmetics.palette, SanyaClient.PALETTE_NAMES.length);
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("hud", String.valueOf(SanyaClient.hudEnabled));
        p.setProperty("keystrokes", String.valueOf(SanyaClient.keystrokesEnabled));
        p.setProperty("coordinates", String.valueOf(SanyaClient.coordinatesEnabled));
        p.setProperty("lowParticles", String.valueOf(SanyaClient.lowParticlesEnabled));
        p.setProperty("cps", String.valueOf(SanyaClient.cpsEnabled));
        p.setProperty("fullBright", String.valueOf(SanyaClient.fullBrightEnabled));
        p.setProperty("hitColor", String.valueOf(SanyaClient.hitColorEnabled));
        p.setProperty("armorHud", String.valueOf(SanyaClient.armorHudEnabled));
        p.setProperty("effectsHud", String.valueOf(SanyaClient.effectsHudEnabled));
        p.setProperty("cooldownHud", String.valueOf(SanyaClient.cooldownHudEnabled));
        p.setProperty("targetHud", String.valueOf(SanyaClient.targetHudEnabled));
        p.setProperty("targetEsp", String.valueOf(SanyaClient.targetEspEnabled));
        p.setProperty("echoHands", String.valueOf(SanyaClient.echoHandsEnabled));
        p.setProperty("aspectRatio", String.valueOf(SanyaClient.aspectRatioIndex));
        p.setProperty("espStyle", String.valueOf(SanyaClient.targetEspMode));
        p.setProperty("timeMode", String.valueOf(SanyaClient.timeMode));
        p.setProperty("weatherMode", String.valueOf(SanyaClient.weatherMode));
        p.setProperty("smoothFont", String.valueOf(UiFont.enabled));
        p.setProperty("hudPalette", String.valueOf(HudRenderer.hudPalette));
        p.setProperty("espPalette", String.valueOf(SanyaClient.espPalette));
        p.setProperty("shaderSky", String.valueOf(ShaderSky.enabled));
        p.setProperty("skyMode", String.valueOf(ShaderSky.mode));
        p.setProperty("skyPalette", String.valueOf(ShaderSky.palette));
        p.setProperty("skySpeed", String.valueOf(ShaderSky.speed));
        p.setProperty("skyOpacity", String.valueOf(ShaderSky.opacity));
        p.setProperty("cosmetics", String.valueOf(Cosmetics.enabled));
        p.setProperty("cosmeticHat", String.valueOf(Cosmetics.hat));
        p.setProperty("cosmeticWings", String.valueOf(Cosmetics.wings));
        p.setProperty("cosmeticPet", String.valueOf(Cosmetics.pet));
        p.setProperty("cosmeticAura", String.valueOf(Cosmetics.aura));
        p.setProperty("cosmeticPalette", String.valueOf(Cosmetics.palette));
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                p.store(w, "SANЯ CLIENT settings");
            }
        } catch (IOException e) {
            System.err.println("[SANЯ CLIENT] Не вдалося зберегти конфіг: " + e);
        }
    }
}
