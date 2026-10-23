package com.sanya.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Smooth TrueType UI font (Carlito, with DejaVu Sans for symbols) instead of the pixel Minecraft font.
 * Defined in assets/sanya/font/ui.json and ui_bold.json; can be switched off in the settings.
 */
public final class UiFont {
    private UiFont() { }

    public static boolean enabled = true;

    private static final Style REGULAR = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sanya", "ui")));
    private static final Style BOLD = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("sanya", "ui_bold")));

    public static Text regular(String s) {
        MutableText t = Text.literal(s);
        return enabled ? t.setStyle(REGULAR) : t;
    }

    public static Text bold(String s) {
        MutableText t = Text.literal(s);
        return enabled ? t.setStyle(BOLD) : t.formatted(net.minecraft.util.Formatting.BOLD);
    }
}
