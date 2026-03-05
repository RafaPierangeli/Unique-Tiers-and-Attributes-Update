package draylar.tiered.mixin.client;

import draylar.tiered.Tiered;
import draylar.tiered.client.TooltipContextHolder;
import draylar.tiered.config.ConfigInit;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipBackgroundRenderer.class)
public abstract class TooltipBackgroundRendererMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private static void onRenderTooltipBackground(DrawContext context, int x, int y, int width, int height, Identifier texture, CallbackInfo ci) {

        if (!ConfigInit.CONFIG.enableTierColorBorder && !ConfigInit.CONFIG.enableTierOrnaments) {
            return;
        }

        ItemStack stack = TooltipContextHolder.currentStack;

        if (stack != null && !stack.isEmpty() && stack.get(Tiered.TIER) != null) {

            String fullTierId = stack.get(Tiered.TIER).tier();
            String baseRarity = "common";

            int borderColor = 0xFFAAAAAA; // Cinza padrão

            if (fullTierId.contains("uncommon")) {
                baseRarity = "uncommon";
                borderColor = 0xFF55FF55; // Verde
            } else if (fullTierId.contains("rare")) {
                baseRarity = "rare";
                borderColor = 0xFF5555FF; // Azul
            } else if (fullTierId.contains("epic")) {
                baseRarity = "epic";
                borderColor = 0xFFAA00AA; // Roxo
            } else if (fullTierId.contains("legendary")) {
                baseRarity = "legendary";
                borderColor = 0xFFFFAA00; // Dourado
            } else if (fullTierId.contains("unique")) {
                baseRarity = "unique";
                borderColor = 0xFFFF55FF; // ligth_purple
            }
            else if (fullTierId.contains("mythic")) {
                baseRarity = "mythic";
                borderColor = 0xFF55FFFF; // Aqua
            }

            if (ConfigInit.CONFIG.enableTierColorBorder) {
                drawSolidColorBorder(context, x, y, width, height, borderColor);
            }

            if (ConfigInit.CONFIG.enableTierOrnaments) {
                Identifier ornamentsTexture = Identifier.of("tiered", "textures/gui/tooltip_borders/" + baseRarity + ".png");
                drawOrnaments(context, ornamentsTexture, x, y, width, height);
            }
        }

        // 🌟 CORREÇÃO 1: O FIM DO VAZAMENTO DE MEMÓRIA!
        // Esvaziamos o porta-malas aqui. Assim, tooltips de botões (ModMenu)
        // ou itens normais nunca vão herdar a borda do último item olhado.
        TooltipContextHolder.currentStack = ItemStack.EMPTY;
    }

    private static void drawSolidColorBorder(DrawContext context, int x, int y, int width, int height, int color) {
        int expand = 3;
        context.fill(x - expand, y - expand, x + width + expand, y - expand + 1, color);
        context.fill(x - expand, y + height + expand - 1, x + width + expand, y + height + expand, color);
        context.fill(x - expand, y - expand + 1, x - expand + 1, y + height + expand - 1, color);
        context.fill(x + width + expand - 1, y - expand + 1, x + width + expand, y + height + expand - 1, color);
    }

    // 🌟 METODO 2: Os Ornamentos Flutuantes (Otimizado para PNG 64x32)
    private static void drawOrnaments(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        int texWidth = 64;  // Nova largura total da imagem
        int texHeight = 32; // Nova altura total da imagem
        int cornerSize = 16; // Tamanho de cada canto no PNG
        int iconSize = 16;   // Tamanho do ícone central no PNG
        int offset = 7;     // Distância que os cantos saltam para fora

        // CANTOS
        // Superior Esquerdo (Pega do u=0, v=0 no PNG)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x - offset, y - offset, 0, 0, cornerSize, cornerSize, texWidth, texHeight);

        // Superior Direito (Pega do u=48, v=0 no PNG)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x + width - cornerSize + offset, y - offset, 48, 0, cornerSize, cornerSize, texWidth, texHeight);

        // Inferior Esquerdo (Pega do u=0, v=16 no PNG) -> 32 (altura total) - 16 (tamanho do canto) = 16
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x - offset, y + height - cornerSize + offset, 0, 16, cornerSize, cornerSize, texWidth, texHeight);

        // Inferior Direito (Pega do u=48, v=16 no PNG)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x + width - cornerSize + offset, y + height - cornerSize + offset, 48, 16, cornerSize, cornerSize, texWidth, texHeight);

        // ÍCONE CENTRAL
        int centerX = x + (width / 2) - (iconSize / 2);


        int iconY = y - 11;

        // As novas coordenadas do seu ícone no PNG
        int iconU = 24; // Posição X no PNG
        int iconV = 8;  // Posição Y no PNG

        // Desenha o ícone usando as novas dimensões da textura (texWidth, texHeight)
        context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, centerX, iconY, iconU, iconV, iconSize, iconSize, texWidth, texHeight);
    }
}