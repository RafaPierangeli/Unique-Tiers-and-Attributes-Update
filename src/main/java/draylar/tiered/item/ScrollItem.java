package draylar.tiered.item;

import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class ScrollItem extends Item {

    public ScrollItem(Settings settings) {
        super(settings);
    }

    // 🌟 NOME DINÂMICO COM ATRIBUTO (Ex: Mythic Attribute Scroll of Max Health)
    @Override
    public Text getName(ItemStack stack) {
        ScrollData data = stack.get(TieredDataComponents.SCROLL_DATA);
        if (data != null) {
            // Pega a tradução do atributo (ex: "Vida Máxima")
            String attrKey = "attribute.name." + data.attributeId().replace(":", ".");

            // Injeta o atributo traduzido dentro do nome do pergaminho usando %s
            return Text.translatable("item.tiered.attribute_scroll." + data.tier().toLowerCase(), Text.translatable(attrKey))
                    .formatted(getTierColor(data.tier()));
        }
        return super.getName(stack);
    }


    public static Formatting getTierColor(String tier) {
        return switch (tier.toLowerCase()) {
            case "common" -> Formatting.WHITE;
            case "uncommon" -> Formatting.GREEN;
            case "rare" -> Formatting.BLUE;
            case "epic" -> Formatting.DARK_PURPLE;
            case "legendary" -> Formatting.GOLD;
            case "unique" -> Formatting.LIGHT_PURPLE;
            case "mythic" -> Formatting.AQUA;
            default -> Formatting.GRAY;
        };
    }

    // 🎨 Retorna a cor ARGB baseada no Tier (Para a Layer 0 - O Papel)
    public static int getTierHexColor(String tier) {
        return switch (tier.toLowerCase()) {
            // 🌟 Trocamos o Branco por um Bege de Pergaminho Antigo!
            case "common" -> 0xFFEAD1A7; // Bege Claro/Papiro
            case "uncommon" -> 0xFF55FF55; // Verde
            case "rare" -> 0xFF5555FF; // Azul
            case "epic" -> 0xFFAA00AA; // Roxo
            case "legendary" -> 0xFFFFAA00; // Dourado
            case "unique" -> 0xFFFF55FF; // Roxo claro
            case "mythic" -> 0xFF55FFFF; // Aqua
            default -> 0xFFAAAAAA; // Cinza (Fallback)
        };
    }

    // 🎨 Retorna a cor ARGB baseada no Atributo (Para a Layer 1 - A Fita)
    public static int getAttributeHexColor(String attributeId) {
        // Limpa a string para evitar problemas com prefixos do Vanilla
        String id = attributeId.toLowerCase().replace("minecraft:", "");

        return switch (id) {
            case "attack_damage", "tiered:range_attack_damage" -> 0xFFAA0000; // Dark Red
            case "max_health" -> 0xFFFF5555; // Red
            case "attack_speed" -> 0xFF00AAAA; // Dark Aqua
            case "tiered:critical_chance" -> 0xFF555555; // Dark Gray
            case "armor" -> 0xFFFFFFFF; // Weith
            case "movement_speed" -> 0xFFFFFF55; // Yellow
            default -> 0xFFFFFFFF; // Branco (Fallback)
        };
    }
}