package draylar.tiered.reforge;

import java.text.DecimalFormat;
import java.util.*;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.network.TieredClientPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class ReforgeScreen extends HandledScreen<ReforgeScreenHandler> {

    public static final Identifier TEXTURE = Identifier.of("tiered", "textures/gui/reforging_screen3.png");
    public static final Identifier REFORGE_UNIQUE = Identifier.of("tiered", "textures/gui/reforging_unique.png");
    public static final Identifier REFORGE_MYTHIC = Identifier.of("tiered", "textures/gui/reforging_mythic.png");

    public ReforgeScreen.ReforgeButton reforgeButton;
    private ItemStack last;
    private List<Item> baseItems;

    // falha aqui
    // 🌟 Formatador para as porcentagens e sorte
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    // 🌟 VARIÁVEIS DA ANIMAÇÃO (Sucesso e Falha)
    private boolean expectingReforge = false;
    private int lastIngredientCount = 0;
    private Identifier lastTier = null;
    private int lastPrestige = -1;
    private int lastLevel = -1;
    private Text floatingText = null;
    private int floatingTick = 0;

    // 🌟 ADICIONE ESTA LINHA AQUI PARA PARAR O ERRO DE COMPILAÇÃO:
    private int syncDelay = 0;

    public ReforgeScreen(ReforgeScreenHandler handler, PlayerInventory playerInventory, Text title) {
        super(handler, playerInventory, title);
        this.titleX = 8;
        // 🌟 UX: Abaixando o título da Forja (O padrão é 6, mudamos para 12 ou 14)
        this.titleY = 8;

        // 🌟 UX: Garantindo que o título do seu inventário ("Inventário") fique no lugar certo
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        this.reforgeButton = this.addDrawableChild(new ReforgeScreen.ReforgeButton(i + 79, j + 56, (button) -> {
            if (button instanceof ReforgeScreen.ReforgeButton reforgeBtn && !reforgeBtn.disabled) {
                TieredClientPacket.writeC2SReforgePacket();
                this.expectingReforge = true;
                this.lastIngredientCount = this.handler.getSlot(0).getStack().getCount();

                ItemStack weapon = this.handler.getSlot(1).getStack();
                this.lastTier = ModifierUtils.getAttributeId(weapon);

                draylar.tiered.api.ARPGEquipmentData data = weapon.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
                this.lastPrestige = data != null ? data.prestige() : -1;
                this.lastLevel = data != null ? data.level() : -1;
            }
        }));
    }

    // 🌟 NOVO: Atualiza o botão a cada frame do jogo

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        if (this.reforgeButton != null) {
            this.reforgeButton.setDisabled(!this.handler.isReforgeReady());
        }

        // 🌟 FASE 1: Espera o ingrediente ser consumido
        if (this.expectingReforge) {
            ItemStack currentIngredient = this.handler.getSlot(0).getStack();

            // Se a Nether Star sumiu, o servidor processou!
            if (currentIngredient.getCount() < this.lastIngredientCount || (this.lastIngredientCount > 0 && currentIngredient.isEmpty())) {
                this.expectingReforge = false; // Desliga a espera do ingrediente
                this.syncDelay = 1; // 🌟 Inicia o delay de 5 ticks para esperar o pacote da arma chegar pela rede
            }
        }

        // 🌟 FASE 2: O Delay de Sincronização
        if (this.syncDelay > 0) {
            this.syncDelay--;

            // Quando o delay acaba (chegou a zero), nós julgamos o resultado!
            if (this.syncDelay == 0) {
                ItemStack currentWeapon = this.handler.getSlot(1).getStack();
                Identifier currentTier = ModifierUtils.getAttributeId(currentWeapon);
                draylar.tiered.api.ARPGEquipmentData data = currentWeapon.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
                int currentPrestige = data != null ? data.prestige() : -1;

                if (this.lastLevel >= 100 && this.lastPrestige < 3) {
                    // 🌟 FOI UMA TENTATIVA DE PRESTÍGIO
                    if (currentPrestige > this.lastPrestige) {
                        this.floatingText = Text.translatable("tiered.arpg.reforge.prestige_success").formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC);
                    } else {
                        this.floatingText = Text.translatable("tiered.arpg.reforge.prestige_fail").formatted(Formatting.RED, Formatting.BOLD);
                    }
                } else {
                    // 🌟 FOI UMA REFORJA NORMAL
                    if (currentTier != null && !currentTier.equals(this.lastTier)) {
                        this.floatingText = Text.literal("✨ ").append(currentWeapon.getName()).append(" ✨").formatted(Formatting.YELLOW);
                    } else {
                        this.floatingText = Text.literal("✨ ").append(currentWeapon.getName()).append(" ✨").formatted(Formatting.GRAY);
                    }
                }

                this.floatingTick = 40; // Inicia a animação do texto subindo
            }
        }

        // Animação do texto flutuante
        if (this.floatingTick > 0) {
            this.floatingTick--;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        // 🌟 DESENHA O SISTEMA DE SORTE E CHANCES (Chamada Nova)
        this.renderLuckAndChances(context, mouseX, mouseY);

        // Lógica do Tooltip quando passa o mouse no botão
        if (this.isPointWithinBounds(79, 56, 18, 18, (double) mouseX, (double) mouseY)) {
            ItemStack itemStack = this.getScreenHandler().getSlot(1).getStack();
            List<Text> tooltip = new ArrayList<Text>();

            Identifier tierId = ModifierUtils.getAttributeId(itemStack);
            boolean isUniqueLocked = tierId != null && tierId.getPath().contains("unique") && !ConfigInit.CONFIG.uniqueReforge;
            boolean isMythicLocked = tierId != null && tierId.getPath().contains("mythic");

            if (itemStack.isEmpty()) {
                // ESTADO 1: Mesa vazia
                tooltip.add(Text.translatable("screen.tiered.reforge_insert_equipment").formatted(Formatting.YELLOW));
            }
            // 🌟 NOVO: ESTADO DE PRESTÍGIO (Tem prioridade sobre os bloqueios de Único/Mítico)
            else if (this.getScreenHandler().isPrestigeMode()) {
                tooltip.add(Text.translatable("tiered.arpg.reforge.prestige_ascension.title").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));

                ItemStack ingredient = this.getScreenHandler().getSlot(0).getStack();
                if (ingredient.isEmpty() || !ingredient.isOf(net.minecraft.item.Items.NETHER_STAR)) {
                    tooltip.add(Text.translatable("tiered.arpg.reforge.requires").formatted(Formatting.RED).append(net.minecraft.item.Items.NETHER_STAR.getName().copy().formatted(Formatting.GRAY)));
                }

                ItemStack addition = this.getScreenHandler().getSlot(2).getStack();
                if (addition.isEmpty() || !addition.isOf(net.minecraft.item.Items.ECHO_SHARD)) {
                    tooltip.add(Text.translatable("tiered.arpg.reforge.requires").formatted(Formatting.RED).append(net.minecraft.item.Items.ECHO_SHARD.getName().copy().formatted(Formatting.GRAY)));
                }

                // 🌟 LÓGICA DINÂMICA DE TOOLTIP
                draylar.tiered.api.ARPGEquipmentData data = itemStack.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
                int currentPrestige = data != null ? data.prestige() : 0;
                int xpCost = this.getScreenHandler().getPrestigeXpCost(currentPrestige);

                // 🌟 CORREÇÃO: Passamos o jogador do Client para a tela mostrar a chance real!
                int chance = this.getScreenHandler().getPrestigeSuccessChance(currentPrestige, this.client.player);

                // Se o jogador tiver sorte extra, podemos até mudar a cor para verde para dar um feedback visual!
                Formatting chanceColor = chance > this.getScreenHandler().getPrestigeSuccessChance(currentPrestige, null) ? Formatting.GREEN : Formatting.YELLOW;
                tooltip.add(Text.translatable("tiered.arpg.reforge.success_chance", chance).formatted(chanceColor));


                if (this.client != null && this.client.player != null) {
                    if (this.client.player.totalExperience < xpCost && !this.client.player.isCreative()) {
                        tooltip.add(Text.translatable("tiered.arpg.reforge.cost_xp", xpCost).formatted(Formatting.RED));
                    } else {
                        tooltip.add(Text.translatable("tiered.arpg.reforge.cost_xp", xpCost).formatted(Formatting.GREEN));
                    }
                }
            }
            else if (itemStack.isIn(TieredItemTags.MODIFIER_RESTRICTED)) {
                // ESTADO 2: Item proibido
                tooltip.add(Text.translatable("screen.tiered.reforge_restricted").formatted(Formatting.RED));
            }
            else if (isUniqueLocked) {
                // ESTADO 3: ITEM ÚNICO BLOQUEADO!
                tooltip.add(Text.translatable("screen.tiered.reforge_unique_locked").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
            }
            else if (isMythicLocked) {
                // ESTADO 4: ITEM MITICO BLOQUEADO!
                tooltip.add(Text.translatable("screen.tiered.reforge_mythic_locked").formatted(Formatting.AQUA, Formatting.BOLD));
            }
            // 🌟 NOVO ESTADO: ARMA DESPERTADA BLOQUEADA!
            else if (this.getScreenHandler().isAwakened(itemStack) && !this.getScreenHandler().isPrestigeMode()) {
                tooltip.add(Text.translatable("tiered.arpg.reforge.awakened.title").formatted(Formatting.RED, Formatting.BOLD));
                tooltip.add(Text.translatable("tiered.arpg.reforge.awakened.line1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tiered.arpg.reforge.awakened.line2").formatted(Formatting.GRAY));
            }
            else {
                // ESTADO 5: Equipamento válido! Checa o que falta para a reforja normal.
                if (itemStack != last) {
                    last = itemStack;
                    baseItems = new ArrayList<Item>();
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(itemStack.getItem());

                    if (!items.isEmpty()) {
                        baseItems.addAll(items);
                    } else {
                        var repairable = itemStack.get(DataComponentTypes.REPAIRABLE);
                        if (repairable != null && repairable.items() != null) {
                            for (RegistryEntry<Item> entry : repairable.items()) {
                                baseItems.add(entry.value());
                            }
                        } else {
                            for (RegistryEntry<Item> itemRegistryEntry : Registries.ITEM.getOrThrow(TieredItemTags.REFORGE_BASE_ITEM)) {
                                baseItems.add(itemRegistryEntry.value());
                            }
                        }
                    }
                }

                // Checa Ingrediente Base
                if (!baseItems.isEmpty()) {
                    ItemStack ingredient = this.getScreenHandler().getSlot(0).getStack();
                    if (ingredient.isEmpty() || !baseItems.contains(ingredient.getItem())) {
                        tooltip.add(Text.translatable("screen.tiered.reforge_ingredient").formatted(Formatting.RED));
                        for (Item item : baseItems) {
                            tooltip.add(Text.literal(" - ").append(item.getName()).formatted(Formatting.GRAY));
                        }
                    }
                }

                // Checa Catalisador
                ItemStack addition = this.getScreenHandler().getSlot(2).getStack();
                if (addition.isEmpty() || !addition.isIn(TieredItemTags.REFORGE_ADDITION)) {
                    tooltip.add(Text.translatable("screen.tiered.reforge_addition").formatted(Formatting.RED));
                }

                // Checa Dano
                if (itemStack.isDamageable() && itemStack.isDamaged()) {
                    tooltip.add(Text.translatable("screen.tiered.reforge_damaged").formatted(Formatting.RED));
                }

                // Checa XP
                if (this.client != null && this.client.player != null) {
                    int xpCost = ConfigInit.CONFIG.reforgeXpCost;
                    if (this.client.player.totalExperience < xpCost && !this.client.player.isCreative()) {
                        tooltip.add(Text.translatable("screen.tiered.reforge_xp_missing", xpCost).formatted(Formatting.RED));
                    } else {
                        tooltip.add(Text.translatable("screen.tiered.reforge_xp_cost", xpCost).formatted(Formatting.GREEN));
                    }
                }
            }

            if (!tooltip.isEmpty()) {
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }

        // Desenha o cadeado se for único e a config não permitir
        if (!ConfigInit.CONFIG.uniqueReforge && !this.getScreenHandler().getSlot(1).getStack().isEmpty()) {
            Identifier attrId = ModifierUtils.getAttributeId(this.getScreenHandler().getSlot(1).getStack());
            if (attrId != null && attrId.getPath().contains("unique")) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, REFORGE_UNIQUE, this.x + 75, this.y + 30, 0, 0, 26, 25, 26, 25);
            }
        }
        // Desenha o cadeado se for Mythic e a config não permitir
        if (!this.getScreenHandler().getSlot(1).getStack().isEmpty()) {
            Identifier attrId = ModifierUtils.getAttributeId(this.getScreenHandler().getSlot(1).getStack());
            if (attrId != null && attrId.getPath().contains("mythic")) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, REFORGE_MYTHIC, this.x + 75, this.y + 30, 0, 0, 26, 25, 26, 25);
            }
        }

        // ... (seu código de tooltip existente) ...

        // 🌟 DESENHA O TEXTO FLUTUANTE ANIMADO
        if (this.floatingTick > 0 && this.floatingText != null) {
            // Calcula o progresso da animação (de 0.0 a 1.0)
            float progress = (40 - this.floatingTick + delta) / 40.0f;

            int textX = this.width / 2;
            // Começa perto do item e sobe 40 pixels suavemente
            int textY = (this.height / 2) - 40 - (int)(progress * 40);

            // Calcula a transparência (Fade out) - Fica invisível no final
            int alpha = (int) ((1.0f - progress) * 255);
            alpha = Math.max(5, Math.min(255, alpha)); // Trava entre 5 e 255
            int color = (alpha << 24) | 0xFFFFFF; // Aplica a transparência

            context.getMatrices().pushMatrix();

            context.drawCenteredTextWithShadow(this.textRenderer, this.floatingText, textX, textY, color);

            context.getMatrices().popMatrix();
        }

    }


    // =================================================================
    // 🌟 O SISTEMA DE SORTE E TOOLTIP DINÂMICA (Sincronizado com o Servidor)
    // =================================================================
    private void renderLuckAndChances(DrawContext context, int mouseX, int mouseY) {
        if (this.client == null || this.client.player == null) return;

        double luck = this.client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.LUCK);
        String luckText = "🍀 " + PERCENT_FORMAT.format(luck);

        int textX = this.x + 142;
        int textY = this.y + 8; // Se quiser descer a sorte também, mude o 8 para 12
        int textWidth = this.textRenderer.getWidth(luckText);
        int textHeight = this.textRenderer.fontHeight;

        // 🌟 UX: Diminuindo o tamanho do texto da Sorte (80% do tamanho original)
        float scale = 0.7f;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(textX, textY);
        context.getMatrices().scale(scale, scale);
        context.drawText(this.textRenderer, luckText, 0, 0, 0xFF55FF55, true);
        context.getMatrices().popMatrix();

        // Ajusta a área de colisão do mouse para o novo tamanho do texto
        int scaledWidth = (int) (textWidth * scale);
        int scaledHeight = (int) (textHeight * scale);

        if (mouseX >= textX && mouseX <= textX + scaledWidth && mouseY >= textY && mouseY <= textY + scaledHeight) {

            if (this.getScreenHandler().isPrestigeMode()) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(Text.translatable("tiered.arpg.reforge.prestige_mode.title").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
                tooltip.add(Text.translatable("tiered.arpg.reforge.prestige_mode.line1").formatted(Formatting.GRAY));
                tooltip.add(Text.translatable("tiered.arpg.reforge.prestige_mode.line2").formatted(Formatting.GRAY));
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
                return; // Sai do metodo para não desenhar as chances normais de reforja
            }




            List<Text> tooltip = new ArrayList<>();
            tooltip.add(Text.translatable("tiered.tooltip.reforge_chance").formatted(Formatting.GOLD, Formatting.ITALIC));

            double reforgeMod = ConfigInit.CONFIG.reforgeModifier;
            double luckMod = ConfigInit.CONFIG.luckReforgeModifier;

            class DynamicTier {
                String keyword;
                Text name;
                Formatting color;
                double currentTotalWeight = 0.0;

                DynamicTier(String keyword, Text name, Formatting color) {
                    this.keyword = keyword;
                    this.name = name;
                    this.color = color;
                }
            }

            DynamicTier[] tiers = {
                    new DynamicTier("common", Text.translatable("tiered.tooltip.reforge_common"), Formatting.WHITE),
                    new DynamicTier("uncommon", Text.translatable("tiered.tooltip.reforge_uncommon"), Formatting.GREEN),
                    new DynamicTier("rare", Text.translatable("tiered.tooltip.reforge_rare"), Formatting.BLUE),
                    new DynamicTier("epic", Text.translatable("tiered.tooltip.reforge_epic"), Formatting.DARK_PURPLE),
                    new DynamicTier("legendary", Text.translatable("tiered.tooltip.reforge_legendary"), Formatting.GOLD),
                    new DynamicTier("unique", Text.translatable("tiered.tooltip.reforge_unique"), Formatting.LIGHT_PURPLE),
                    new DynamicTier("mythic", Text.translatable("tiered.tooltip.reforge_mythic"), Formatting.AQUA)
            };

            var allAttributes = draylar.tiered.Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes();
            if (allAttributes.isEmpty()) return;

            net.minecraft.item.ItemStack equipmentStack = net.minecraft.item.ItemStack.EMPTY;
            net.minecraft.item.ItemStack resultStack = net.minecraft.item.ItemStack.EMPTY;

            int customSlotCount = 0;
            for (net.minecraft.screen.slot.Slot slot : this.handler.slots) {
                if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
                    if (customSlotCount == 1) equipmentStack = slot.getStack();
                    if (customSlotCount == 2) resultStack = slot.getStack();
                    customSlotCount++;
                }
            }

            if (equipmentStack.isEmpty() && !resultStack.isEmpty()) {
                equipmentStack = resultStack;
            }

            net.minecraft.util.Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(equipmentStack.getItem());
            boolean hasItem = !equipmentStack.isEmpty();

            // 🌟 UX: Separação Cirúrgica das Armaduras
            String categoryKey = "tiered.tooltip.category.global";

            if (hasItem) {
                if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) categoryKey = "tiered.tooltip.category.sword";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.PICKAXES)) categoryKey = "tiered.tooltip.category.pickaxe";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.AXES)) categoryKey = "tiered.tooltip.category.axe";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.SHOVELS)) categoryKey = "tiered.tooltip.category.shovel";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.HOES)) categoryKey = "tiered.tooltip.category.hoe";
                    // Separação das peças de armadura
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.HEAD_ARMOR)) categoryKey = "tiered.tooltip.category.helmet";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.CHEST_ARMOR)) categoryKey = "tiered.tooltip.category.chestplate";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.LEG_ARMOR)) categoryKey = "tiered.tooltip.category.leggings";
                else if (equipmentStack.isIn(net.minecraft.registry.tag.ItemTags.FOOT_ARMOR)) categoryKey = "tiered.tooltip.category.boots";
                    // Restante
                else if (equipmentStack.isOf(net.minecraft.item.Items.BOW)) categoryKey = "tiered.tooltip.category.bow";
                else if (equipmentStack.isOf(net.minecraft.item.Items.CROSSBOW)) categoryKey = "tiered.tooltip.category.crossbow";
                else if (equipmentStack.isOf(net.minecraft.item.Items.SHIELD)) categoryKey = "tiered.tooltip.category.shield";
                else if (equipmentStack.isOf(net.minecraft.item.Items.TRIDENT)) categoryKey = "tiered.tooltip.category.trident";
                else if (equipmentStack.isOf(net.minecraft.item.Items.ELYTRA)) categoryKey = "tiered.tooltip.category.elytra";
                else if (equipmentStack.isOf(net.minecraft.item.Items.MACE)) categoryKey = "tiered.tooltip.category.mace";
                else categoryKey = "tiered.tooltip.category.item";
            }

            java.util.Map<net.minecraft.util.Identifier, draylar.tiered.api.PotentialAttribute> validAttributes = new java.util.HashMap<>();
            for (var entry : allAttributes.entrySet()) {
                if (!hasItem || entry.getValue().isValid(itemId)) {
                    validAttributes.put(entry.getKey(), entry.getValue());
                }
            }

            if (validAttributes.isEmpty()) {
                categoryKey = "tiered.tooltip.category.global";
                for (var entry : allAttributes.entrySet()) {
                    validAttributes.put(entry.getKey(), entry.getValue());
                }
            }

            tooltip.add(Text.translatable(categoryKey).formatted(Formatting.YELLOW));
            tooltip.add(Text.empty());

            double initialMaxWeight = 0.0;
            for (var attr : validAttributes.values()) {
                double w = attr.getWeight() + 1.0;
                if (w > initialMaxWeight) initialMaxWeight = w;
            }

            double newMaxWeight = 0.0;
            java.util.Map<net.minecraft.util.Identifier, Double> reforgedWeights = new java.util.HashMap<>();

            for (var entry : validAttributes.entrySet()) {
                double w = entry.getValue().getWeight() + 1.0;
                if (w > initialMaxWeight / 2.0) {
                    w = (int) (w * reforgeMod);
                }
                reforgedWeights.put(entry.getKey(), w);

                if (w > newMaxWeight) newMaxWeight = w;
            }

            double absoluteTotalWeight = 0.0;

            for (var entry : reforgedWeights.entrySet()) {
                net.minecraft.util.Identifier id = entry.getKey();
                double w = entry.getValue();

                if (luck > 0) {
                    if (w > newMaxWeight / 3.0) {
                        w = (int) (w * (1.0 - (0.02 * luck)));
                        w = Math.max(2.0, w);
                    } else {
                        w = (int) (w + (1.0 + (luckMod * luck)));
                    }
                }

                absoluteTotalWeight += w;

                String path = id.getPath().toLowerCase();
                for (DynamicTier tier : tiers) {
                    if (path.contains(tier.keyword)) {
                        // 🌟 A MÁGICA: Impede que a palavra "uncommon" caia na regra do "common"
                        if (tier.keyword.equals("common") && path.contains("uncommon")) {
                            continue; // Pula para o próximo (que será o uncommon de verdade)
                        }

                        tier.currentTotalWeight += w;
                        break;
                    }
                }
            }

            if (absoluteTotalWeight > 0) {
                for (DynamicTier t : tiers) {
                    if (t.currentTotalWeight <= 0) continue;

                    double chance = (t.currentTotalWeight / absoluteTotalWeight) * 100.0;
                    String chanceStr = chance < 0.01 ? "< 0.01" : PERCENT_FORMAT.format(chance);

                    tooltip.add(t.name.copy().append(": ").formatted(t.color)
                            .append(Text.literal(chanceStr + "%").formatted(Formatting.GRAY)));
                }
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }




    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, this.backgroundWidth, this.backgroundHeight);

        // 🌟 DESENHO DOS ÍCONES FANTASMAS (GHOST ITEMS) DINÂMICOS
        ItemStack weapon = this.handler.getSlot(1).getStack();

        if (!weapon.isEmpty()) {
            boolean isPrestige = this.handler.isPrestigeMode();

            // Slot 0 (Esquerda - Material Base)
            if (this.handler.getSlot(0).getStack().isEmpty()) {
                ItemStack ghostBase = ItemStack.EMPTY;

                if (isPrestige) {
                    ghostBase = new ItemStack(net.minecraft.item.Items.NETHER_STAR);
                } else {
                    // Tenta descobrir qual é o minério base da arma
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(weapon.getItem());
                    if (!items.isEmpty()) {
                        ghostBase = new ItemStack(items.get(0));
                    } else {
                        var repairable = weapon.get(DataComponentTypes.REPAIRABLE);
                        if (repairable != null && repairable.items() != null && repairable.items().size() > 0) {
                            ghostBase = new ItemStack(repairable.items().get(0).value());
                        }
                    }
                }

                if (!ghostBase.isEmpty()) {
                    context.drawItemWithoutEntity(ghostBase, i + 45, j + 47);
                    // Aplica uma máscara escura (preto com 60% de opacidade) para parecer um "fantasma"
                    context.fill(i + 45, j + 47, i + 45 + 16, j + 47 + 16, 0x99000000);
                }
            }

            // Slot 2 (Direita - Cristal/Adição)
            if (this.handler.getSlot(2).getStack().isEmpty()) {
                ItemStack ghostAddition = ItemStack.EMPTY;

                if (isPrestige) {
                    ghostAddition = new ItemStack(net.minecraft.item.Items.ECHO_SHARD);
                } else {
                    // Usa Ametista como padrão visual para o cristal de reforja
                    ghostAddition = new ItemStack(net.minecraft.item.Items.AMETHYST_SHARD);
                }

                if (!ghostAddition.isEmpty()) {
                    context.drawItemWithoutEntity(ghostAddition, i + 115, j + 47);
                    context.fill(i + 115, j + 47, i + 115 + 16, j + 47 + 16, 0x99000000);
                }
            }
        }
    }

    public class ReforgeButton extends ButtonWidget {
        private boolean disabled;

        private static final Identifier BUTTON_TEXTURE = Identifier.of("tiered", "textures/gui/reforging_button.png");

        public ReforgeButton(int x, int y, ButtonWidget.PressAction onPress) {
            super(x, y, 18, 18, ScreenTexts.EMPTY, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.disabled = true;
            this.active = false; // Diz pro Vanilla que não pode ser clicado
        }
@Override
protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            int u = 0; // Posição X na imagem (sempre 0, pois a imagem tem 20px de largura)
            int v = 0; // Posição Y na imagem (muda dependendo do estado)

            if (this.disabled) {
                u = 21; // Pega o 3º botão (Escuro/Bloqueado)
            } else if (this.isHovered()) {
                u = 41; // Pega o 2º botão (Azul/Mouse em cima)
            } else {
                u = 1;  // Pega o 1º botão (Normal/Liberado)
            }
            context.drawTexture(RenderPipelines.GUI_TEXTURED, BUTTON_TEXTURE, this.getX(), this.getY(), u, 1, this.width, this.height, this.width, this.height,60,20);
        }

        public void setDisabled(boolean disable) {
            this.disabled = disable;
            this.active = !disable;
        }

    }


}