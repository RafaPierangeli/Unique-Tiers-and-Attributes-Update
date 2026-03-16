package draylar.tiered.reforge;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.Tiered;
import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.ScrollData;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ItemsRegisters;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class MagicStationScreen extends HandledScreen<MagicStationScreenHandler> {

    public static final Identifier TEXTURE = Identifier.of("tiered", "textures/gui/magic_screen.png");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    public MagicStationScreen.ProcessButton processButton;

    // 🌟 VARIÁVEIS DA ANIMAÇÃO (Sucesso e Falha)
    private boolean expectingMagic = false;
    private int lastCatalystCount = 0;
    private int lastMode = -1; // 0=Furar, 1=Inserir, 2=Extrair
    private int oldMaxSlots = 0;
    private int oldPhysicalScrolls = 0;
    private Text floatingText = null;
    private int floatingTick = 0;
    private int syncDelay = 0;

    public MagicStationScreen(MagicStationScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title);
        this.titleX = 8;
        this.titleY = 8;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        this.processButton = this.addDrawableChild(new MagicStationScreen.ProcessButton(i + 142, j + 35, (button) -> {
            if (button instanceof MagicStationScreen.ProcessButton btn && !btn.disabled) {

                // 🌟 PREPARA A ANIMAÇÃO (Tira uma "foto" do estado atual)
                this.expectingMagic = true;
                this.lastCatalystCount = this.handler.getSlot(2).getStack().getCount();

                if (this.handler.isPiercingMode()) this.lastMode = 0;
                else if (this.handler.isInsertionMode()) this.lastMode = 1;
                else if (this.handler.isExtractionMode()) this.lastMode = 2;

                ItemStack equipment = this.handler.getSlot(0).getStack();
                ARPGEquipmentData data = equipment.get(TieredDataComponents.ARPG_DATA);
                this.oldMaxSlots = data != null ? data.maxSlots() : 0;

                this.oldPhysicalScrolls = 0;
                for (int k = 3; k < 6; k++) {
                    if (!this.handler.getSlot(k).getStack().isEmpty()) this.oldPhysicalScrolls++;
                }

                // Envia o comando para o servidor
                if (this.client != null && this.client.interactionManager != null) {
                    this.client.interactionManager.clickButton(this.handler.syncId, 100);
                }
            }
        }));
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (this.processButton != null) {
            this.processButton.setDisabled(!this.handler.isReady());
        }

        // 🌟 FASE 1: Espera o End Crystal ser consumido pelo servidor
        if (this.expectingMagic) {
            ItemStack currentCatalyst = this.handler.getSlot(2).getStack();
            if (currentCatalyst.getCount() < this.lastCatalystCount || (this.lastCatalystCount > 0 && currentCatalyst.isEmpty())) {
                this.expectingMagic = false;
                this.syncDelay = 2; // Espera 2 ticks para a rede sincronizar os itens novos
            }
        }

        // 🌟 FASE 2: O Julgamento
        if (this.syncDelay > 0) {
            this.syncDelay--;
            if (this.syncDelay == 0) {
                boolean success = false;
                ItemStack equipment = this.handler.getSlot(0).getStack();
                ARPGEquipmentData data = equipment.get(TieredDataComponents.ARPG_DATA);

                if (this.lastMode == 0) { // Furar
                    int newMaxSlots = data != null ? data.maxSlots() : 0;
                    success = newMaxSlots > this.oldMaxSlots;
                    this.floatingText = success ? Text.translatable("tiered.magic_station.pierce.success").formatted(Formatting.GREEN, Formatting.BOLD)
                            : Text.translatable("tiered.magic_station.pierce.fail").formatted(Formatting.RED, Formatting.BOLD);
                } else if (this.lastMode == 1) { // Inserir
                    int newPhysicalScrolls = 0;
                    for (int k = 3; k < 6; k++) {
                        if (!this.handler.getSlot(k).getStack().isEmpty()) newPhysicalScrolls++;
                    }
                    // Se tem menos pergaminhos físicos na mesa agora, é porque eles entraram na arma!
                    success = newPhysicalScrolls < this.oldPhysicalScrolls;
                    this.floatingText = success ? Text.translatable("tiered.magic_station.insert.success").formatted(Formatting.GREEN, Formatting.BOLD)
                            : Text.translatable("tiered.magic_station.insert.fail").formatted(Formatting.RED, Formatting.BOLD);
                } else if (this.lastMode == 2) { // Extrair
                    // Se o Slot 6 (Saída) não está vazio, a extração funcionou!
                    success = !this.handler.getSlot(6).getStack().isEmpty();
                    this.floatingText = success ? Text.translatable("tiered.magic_station.extract.success").formatted(Formatting.GREEN, Formatting.BOLD)
                            : Text.translatable("tiered.magic_station.extract.fail").formatted(Formatting.RED, Formatting.BOLD);
                }

                this.floatingTick = 40; // Inicia a animação de subida
            }
        }

        if (this.floatingTick > 0) {
            this.floatingTick--;
        }
    }

    // 🌟 A MÁGICA DA EXTRAÇÃO: Intercepta o clique direto no Slot nativo!
    @Override
    protected void onMouseClick(net.minecraft.screen.slot.Slot slot, int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType) {

        // Se a mesa estiver no Modo Extração e o jogador clicou em um slot válido
        if (this.handler.isExtractionMode() && slot != null) {

            // Verifica se o slot clicado é um dos 3 buracos de baixo (IDs 3, 4 ou 5 na nossa mesa)
            if (slot.id >= 3 && slot.id <= 5) {

                // Converte o ID do slot (3, 4, 5) para o Índice do Pergaminho (0, 1, 2)
                int scrollIndex = slot.id - 3;

                // Envia o Índice para o servidor selecionar qual pergaminho será destruído
                if (this.client != null && this.client.interactionManager != null) {
                    this.client.interactionManager.clickButton(this.handler.syncId, scrollIndex);
                }

                // 🌟 CRÍTICO: Retorna aqui para abortar o clique!
                // Isso impede que o jogador consiga "roubar" o pergaminho da arma com o mouse.
                return;
            }
        }

        // Se não for o Modo Extração (ou se clicou em outro slot), segue o jogo normalmente
        super.onMouseClick(slot, slotId, button, actionType);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // 🌟 LAPIDAÇÃO 4: TOOLTIP DOS PERGAMINHOS EMBUTIDOS
        ItemStack equipment = this.handler.getSlot(0).getStack();
        ARPGEquipmentData data = equipment.get(TieredDataComponents.ARPG_DATA);
        if (data != null) {
            int i = (this.width - this.backgroundWidth) / 2;
            int j = (this.height - this.backgroundHeight) / 2;

            for (int slotIndex = 0; slotIndex < 3; slotIndex++) {
                if (slotIndex < data.maxSlots()) {
                    int slotX = i + 17 + (slotIndex * 34);
                    int slotY = j + 51;

                    // Se o mouse está em cima deste slot
                    if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                        // E não tem item físico lá
                        if (this.handler.getSlot(3 + slotIndex).getStack().isEmpty()) {
                            if (data.slots() != null && slotIndex < data.slots().size()) {
                                ScrollData scrollData = data.slots().get(slotIndex);
                                if (scrollData != null && !scrollData.attributeId().equals("empty")) {

                                    // Cria um item falso e manda o Minecraft desenhar a Tooltip nativa dele!
                                    ItemStack fakeScroll = new ItemStack(ItemsRegisters.ATTRIBUTE_SCROLL);
                                    fakeScroll.set(TieredDataComponents.SCROLL_DATA, scrollData);
                                    context.drawItemTooltip(this.textRenderer, fakeScroll, mouseX, mouseY);

                                }
                            }
                        }
                    }
                }
            }
            // 🌟 DESENHA O TEXTO FLUTUANTE ANIMADO
            if (this.floatingTick > 0 && this.floatingText != null) {
                float progress = (40 - this.floatingTick + delta) / 40.0f;
                int textX = this.width / 2;
                int textY = (this.height / 2) - 40 - (int)(progress * 40); // Sobe 40 pixels

                int alpha = (int) ((1.0f - progress) * 255);
                alpha = Math.max(5, Math.min(255, alpha));
                int color = (alpha << 24) | 0xFFFFFF;

                context.getMatrices().pushMatrix();
                context.drawCenteredTextWithShadow(this.textRenderer, this.floatingText, textX, textY, color);
                context.getMatrices().popMatrix();
            }
        }

        // Mantém o seu sistema de sorte intacto!
        this.renderLuckAndChances(context, mouseX, mouseY);

        // 🌟 TOOLTIPS DINÂMICAS DO BOTÃO (Agora 100% Traduzíveis)
        if (this.isPointWithinBounds(142, 35, 18, 18, (double) mouseX, (double) mouseY)) {
            List<Text> tooltip = new ArrayList<>();

            if (equipment.isEmpty()) {
                tooltip.add(Text.translatable("tiered.magic_station.insert_equipment").formatted(Formatting.YELLOW));
            } else {
                if (data == null) {
                    tooltip.add(Text.translatable("tiered.magic_station.incompatible_item").formatted(Formatting.RED));
                } else {
                    if (this.handler.isPiercingMode()) {
                        tooltip.add(Text.translatable("tiered.magic_station.mode.pierce").formatted(Formatting.AQUA, Formatting.BOLD));

                        int limit = data.prestige() >= 3 ? 3 : 2;

                        if (data.maxSlots() >= limit) {
                            tooltip.add(Text.translatable("tiered.magic_station.max_slots").formatted(Formatting.RED));
                            if (data.prestige() < 3) {
                                tooltip.add(Text.translatable("tiered.magic_station.requires_prestige").formatted(Formatting.GRAY));
                            }
                        } else {
                            checkRequirements(tooltip, 500, Items.END_CRYSTAL);
                        }
                    } else if (this.handler.isInsertionMode()) {
                        tooltip.add(Text.translatable("tiered.magic_station.mode.insert").formatted(Formatting.GREEN, Formatting.BOLD));
                        int scrollCount = 0;
                        for (int i = 0; i < 3; i++) {
                            if (this.handler.isSlotUnlocked(i) && this.handler.getSlot(3 + i).hasStack()) scrollCount++;
                        }
                        if (scrollCount == 0) {
                            tooltip.add(Text.translatable("tiered.magic_station.insert_scrolls").formatted(Formatting.YELLOW));
                        } else {
                            checkRequirements(tooltip, 500 * scrollCount, Items.END_CRYSTAL);
                        }
                    } else if (this.handler.isExtractionMode()) {
                        tooltip.add(Text.translatable("tiered.magic_station.mode.extract").formatted(Formatting.RED, Formatting.BOLD));
                        int selected = this.handler.getSelectedExtractionSlot();

                        if (selected == -1) {
                            tooltip.add(Text.translatable("tiered.magic_station.click_to_extract").formatted(Formatting.YELLOW));
                        } else {
                            // 🌟 VERIFICA SE O SLOT SELECIONADO REALMENTE TEM UM PERGAMINHO
                            boolean hasScroll = false;
                            if (data.slots() != null && selected < data.slots().size()) {
                                draylar.tiered.api.ScrollData s = data.slots().get(selected);
                                if (s != null && !s.attributeId().equals("empty")) {
                                    hasScroll = true;
                                }
                            }

                            if (!hasScroll) {
                                // Slot vazio selecionado!
                                tooltip.add(Text.translatable("tiered.magic_station.empty_slot_selected").formatted(Formatting.RED));
                            } else {
                                // Slot válido selecionado!
                                tooltip.add(Text.translatable("tiered.magic_station.extracting_slot", (selected + 1)).formatted(Formatting.YELLOW));
                                checkRequirements(tooltip, 500, net.minecraft.item.Items.END_CRYSTAL);
                            }
                        }
                    } else {
                        tooltip.add(Text.translatable("tiered.magic_station.insert_magic_item").formatted(Formatting.YELLOW));
                        tooltip.add(Text.translatable("tiered.magic_station.item.piercer").formatted(Formatting.GRAY));
                        tooltip.add(Text.translatable("tiered.magic_station.item.amethyst").formatted(Formatting.GRAY));
                        tooltip.add(Text.translatable("tiered.magic_station.item.extractor").formatted(Formatting.GRAY));
                    }
                }
            }
            if (!tooltip.isEmpty()) {
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    private void checkRequirements(List<Text> tooltip, int xpCost, Item catalyst) {
        ItemStack catStack = this.handler.getSlot(2).getStack();
        if (catStack.isEmpty() || !catStack.isOf(catalyst)) {
            tooltip.add(Text.translatable("tiered.magic_station.requires").formatted(Formatting.RED)
                    .append(catalyst.getName().copy().formatted(Formatting.GRAY)));
        }

        if (this.client != null && this.client.player != null) {
            // 🌟 LÊ AS ESTANTES E CALCULA A CHANCE TOTAL PARA A TELA
            int luck = (int) this.client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.LUCK);
            int bookshelves = this.handler.getBookshelfCount();

            double chance = Math.max(0, Math.min(100, 50.0 + luck + (bookshelves * 0.5)));

            // Formata para não mostrar ".0" se for número inteiro (ex: 57.5% vs 58%)
            String chanceStr = (chance % 1 == 0) ? String.valueOf((int)chance) : String.valueOf(chance);

            tooltip.add(Text.translatable("tiered.magic_station.success_chance", chanceStr).formatted(chance >= 50 ? Formatting.GREEN : Formatting.YELLOW));

            if (this.client.player.totalExperience < xpCost && !this.client.player.isCreative()) {
                tooltip.add(Text.translatable("tiered.magic_station.cost_xp", xpCost).formatted(Formatting.RED));
            } else {
                tooltip.add(Text.translatable("tiered.magic_station.cost_xp", xpCost).formatted(Formatting.GREEN));
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);

        ItemStack equipment = this.handler.getSlot(0).getStack();
        ARPGEquipmentData data = equipment.get(TieredDataComponents.ARPG_DATA);

        for (int slotIndex = 0; slotIndex < 3; slotIndex++) {
            // 🌟 A MATEMÁTICA DO PIXEL: Soma 1 apenas no 3º slot!
            int slotX = i + 17 + (slotIndex * 34) + (slotIndex == 2 ? 1 : 0);
            int slotY = j + 51; // Ajuste o Y se necessário

            if (data == null || slotIndex >= data.maxSlots()) {
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0xCC000000);
            } else {
                boolean hasPhysicalItem = !this.handler.getSlot(3 + slotIndex).getStack().isEmpty();

                ScrollData appliedScroll = null;
                if (data.slots() != null && slotIndex < data.slots().size()) {
                    ScrollData s = data.slots().get(slotIndex);
                    if (s != null && !s.attributeId().equals("empty")) {
                        appliedScroll = s;
                    }
                }

                if (!hasPhysicalItem) {
                    if (appliedScroll != null) {
                        ItemStack fakeScroll = new ItemStack(ItemsRegisters.ATTRIBUTE_SCROLL);
                        fakeScroll.set(TieredDataComponents.SCROLL_DATA, appliedScroll);
                        context.drawItemWithoutEntity(fakeScroll, slotX, slotY);
                    } else {
                        ItemStack ghostScroll = new ItemStack(ItemsRegisters.ATTRIBUTE_SCROLL);
                        ghostScroll.set(TieredDataComponents.SCROLL_DATA, new ScrollData("common", "empty", 0));
                        context.drawItemWithoutEntity(ghostScroll, slotX, slotY);
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x44FFFFFF);
                    }
                }

                if (this.handler.isExtractionMode()) {
                    if (this.handler.getSelectedExtractionSlot() == slotIndex) {
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x66FF0000);
                    } else {
                        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x66000000);
                    }
                }
            }
        }

        // 🌟 NOVOS GHOST ITEMS
        if (this.handler.getSlot(1).getStack().isEmpty()) {
            context.drawItemWithoutEntity(new ItemStack(Items.AMETHYST_SHARD), i + 51, j + 23);
            context.fill(i + 51, j + 23, i + 51 + 16, j + 23 + 16, 0x99000000);
        }
        if (this.handler.getSlot(2).getStack().isEmpty()) {
            context.drawItemWithoutEntity(new ItemStack(Items.END_CRYSTAL), i + 86, j + 23);
            context.fill(i + 86, j + 23, i + 86 + 16, j + 23 + 16, 0x99000000);
        }
    }

    // 🌟 O SISTEMA DE SORTE E ESTANTES
    private void renderLuckAndChances(DrawContext context, int mouseX, int mouseY) {
        if (this.client == null || this.client.player == null) return;

        double luck = this.client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.LUCK);
        int bookshelves = this.handler.getBookshelfCount();

        String luckText = "🍀 " + PERCENT_FORMAT.format(luck);
        String bookText = "📚 " + bookshelves;

        int textX = this.x + 142;
        int textY = this.y + 8;
        float scale = 0.7f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(textX, textY);
        context.getMatrices().scale(scale, scale);

        // Desenha a Sorte
        context.drawText(this.textRenderer, luckText, 0, 0, 0xFF55FF55, true);
        // Desenha as Estantes (12 pixels abaixo)
        context.drawText(this.textRenderer, bookText, 0, 12, 0xFFFFD700, true);

        context.getMatrices().popMatrix();

        int scaledWidth = (int) (this.textRenderer.getWidth(luckText) * scale);
        int scaledHeight = (int) (this.textRenderer.fontHeight * scale);

        // Tooltip da Sorte
        if (mouseX >= textX && mouseX <= textX + scaledWidth && mouseY >= textY && mouseY <= textY + scaledHeight) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.translatable("tiered.magic_station.luck_bonus.title").formatted(Formatting.GOLD, Formatting.ITALIC));
            tooltip.add(Text.translatable("tiered.magic_station.luck_bonus.desc", (int)luck).formatted(Formatting.GRAY));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }

        // Tooltip das Estantes
        int bookScaledWidth = (int) (this.textRenderer.getWidth(bookText) * scale);
        int bookY = textY + 9; // Posição Y aproximada após o scale

        if (mouseX >= textX && mouseX <= textX + bookScaledWidth && mouseY >= bookY && mouseY <= bookY + scaledHeight) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.translatable("tiered.magic_station.bookshelf_bonus.title").formatted(Formatting.GOLD, Formatting.ITALIC));

            double extraChance = bookshelves * 0.5;
            String chanceStr = (extraChance % 1 == 0) ? String.valueOf((int)extraChance) : String.valueOf(extraChance);

            tooltip.add(Text.translatable("tiered.magic_station.bookshelf_bonus.desc", chanceStr).formatted(Formatting.GRAY));
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    // O Botão usando a mesma textura da Reforja
    public class ProcessButton extends ButtonWidget {
        private boolean disabled;
        private static final Identifier BUTTON_TEXTURE = Identifier.of("tiered", "textures/gui/reforging_button.png");

        public ProcessButton(int x, int y, ButtonWidget.PressAction onPress) {
            super(x, y, 18, 18, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.disabled = true;
            this.active = false;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            int u = 0;
            if (this.disabled) u = 21;
            else if (this.isHovered()) u = 41;
            else u = 1;

            context.drawTexture(RenderPipelines.GUI_TEXTURED, BUTTON_TEXTURE, this.getX(), this.getY(), u, 1, this.width, this.height, this.width, this.height, 60, 20);
        }

        public void setDisabled(boolean disable) {
            this.disabled = disable;
            this.active = !disable;
        }
    }
}