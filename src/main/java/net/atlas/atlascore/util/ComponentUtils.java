package net.atlas.atlascore.util;

import net.atlas.atlascore.AtlasCore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class ComponentUtils {

    public static MutableComponent separatorLine(MutableComponent title) {
        return separatorLine(title, false);
    }
    public static MutableComponent separatorLine(MutableComponent title, boolean retainFormatting) {
        String spaces = "                                                                ";

        if (title != null) {
            int lineLength = spaces.length() - Math.round((float)title.getString().length() * 1.33F) - 4;
            title = retainFormatting ? title.withStyle(title.getStyle().withStrikethrough(false)) : title.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.configNameDisplayColour.get())).withStrikethrough(false));
            return Component.literal(spaces.substring(0, lineLength / 2)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(true))
                    .append(Component.literal("[ ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(false)))
                    .append(title)
                    .append(Component.literal(" ]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(false)))
                    .append(Component.literal(spaces.substring(0, (lineLength + 1) / 2))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(true));
        } else {
            return Component.literal(spaces).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(true));
        }
    }
}
