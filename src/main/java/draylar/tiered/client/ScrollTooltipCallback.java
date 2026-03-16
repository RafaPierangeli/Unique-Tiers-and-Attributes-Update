package draylar.tiered.client;

import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ItemsRegisters;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class ScrollTooltipCallback {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {

            // 🌟 A SUA IDEIA APLICADA AQUI: Só roda se o item for o Pergaminho!
            if (stack.isOf(ItemsRegisters.ATTRIBUTE_SCROLL)) {
                ScrollData data = stack.get(TieredDataComponents.SCROLL_DATA);

                if (data != null) {
                    String valueStr = (data.value() % 1 == 0) ? String.valueOf((int)data.value()) : String.valueOf(data.value());
                    String attrKey = "attribute.name." + data.attributeId().replace(":", ".");

                    // Visual limpo: "+7 Vida Máxima" em cinza
                    lines.add(Text.literal("+" + valueStr + " ")
                            .append(Text.translatable(attrKey))
                            .formatted(Formatting.GRAY));
                } else {
                    lines.add(Text.literal("Pergaminho Vazio (Erro de Geração)").formatted(Formatting.DARK_GRAY));
                }
            }
        });
    }
}