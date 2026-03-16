package draylar.tiered.client;

import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ItemsRegisters;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

@Environment(EnvType.CLIENT)
public class ScrollTooltipCallback {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {

            // Só roda se o item for o Pergaminho!
            if (stack.isOf(ItemsRegisters.ATTRIBUTE_SCROLL)) {
                ScrollData data = stack.get(TieredDataComponents.SCROLL_DATA);

                if (data != null && !data.attributeId().equals("empty")) {
                    String valueStr = (data.value() % 1 == 0) ? String.valueOf((int)data.value()) : String.valueOf(data.value());
                    String attrKey = "attribute.name." + data.attributeId().replace(":", ".");

                    // 🌟 1. Extrai o ícone e o nome limpo
                    String[] iconAndName = extractIconAndName(attrKey);
                    String icon = iconAndName[0];
                    String nameWithoutIcon = iconAndName[1];

                    // 🌟 2. Verifica se é porcentagem
                    net.minecraft.util.Identifier attrId = net.minecraft.util.Identifier.tryParse(data.attributeId());
                    String path = attrId != null ? attrId.getPath() : "";

                    boolean isPercentage = path.equals("movement_speed") ||
                            path.equals("jump_strength") ||
                            path.equals("mining_efficiency") ||
                            path.equals("critical_chance") ||
                            path.equals("knockback_resistance") ||
                            path.equals("movement_efficiency") ||
                            path.equals("sneaking_speed");

                    String suffix = isPercentage ? "%" : "";

                    // 🌟 3. Monta a linha com as cores corretas
                    net.minecraft.text.MutableText scrollLine = Text.empty();

                    // Se tiver ícone do Resource Pack, adiciona forçando a cor BRANCA!
                    if (!icon.isEmpty()) {
                        scrollLine.append(Text.literal(icon + " ").formatted(Formatting.WHITE));
                    }

                    // Adiciona o valor (+7%) e o nome limpo na cor CINZA
                    scrollLine.append(Text.literal("+" + valueStr + suffix + " ").formatted(Formatting.GRAY))
                            .append(Text.literal(nameWithoutIcon).formatted(Formatting.GRAY));

                    lines.add(scrollLine);
                } else {
                    lines.add(Text.literal("Pergaminho Vazio (Erro de Geração)").formatted(Formatting.DARK_GRAY));
                }
            }
        });
    }

    // 🌟 O seu metodo extrator de ícones (Copiado para cá para manter a classe independente)
    private static String[] extractIconAndName(String translationKey) {
        String rawTranslated = Language.getInstance().get(translationKey);
        if (rawTranslated == null) rawTranslated = translationKey;

        String cleanTranslated = rawTranslated.replaceAll("§[0-9a-fk-or]", "");

        for (int i = 0; i < cleanTranslated.length(); ) {
            int cp = cleanTranslated.codePointAt(i);
            if ((cp >= 0xE000 && cp <= 0xF8FF) || (cp >= 0xF900 && cp <= 0xFAFF) ||
                    (cp >= 0x1CD00 && cp <= 0x1CDFF) || (cp >= 0x1FB00 && cp <= 0x1FBFF) ||
                    (cp >= 0xF0000 && cp <= 0xFFFFD) || (cp >= 0x100000 && cp <= 0x10FFFD)) {

                int len = Character.charCount(cp);
                String icon = new String(Character.toChars(cp));
                String nameWithoutIcon = cleanTranslated.substring(i + len).trim();
                return new String[]{icon, nameWithoutIcon};
            }
            i += Character.charCount(cp);
        }
        return new String[]{"", cleanTranslated};
    }
}