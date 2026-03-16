package draylar.tiered.mixin;

import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ItemsRegisters;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 🌟 SEGREDO NINJA: Herdamos o "Avô" (ScreenHandler) para fugir das mudanças de construtor da Mojang!
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ScreenHandler {

    @Shadow @Final private Property levelCost;
    @Shadow private int repairItemUsage;
    @Shadow private String newItemName;

    // Construtor blindado que pede apenas 2 argumentos
    protected AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {

        ItemStack input1 = this.getSlot(0).getStack();
        ItemStack input2 = this.getSlot(1).getStack();

        if (!input1.isEmpty() && !input2.isEmpty() &&
                input1.isOf(ItemsRegisters.ATTRIBUTE_SCROLL) && input2.isOf(ItemsRegisters.ATTRIBUTE_SCROLL)) {

            if (input1.getCount() > 1 || input2.getCount() > 1) {
                this.getSlot(2).setStack(ItemStack.EMPTY);
                this.levelCost.set(0);
                this.sendContentUpdates();
                ci.cancel();
                return;
            }

            ScrollData data1 = input1.get(TieredDataComponents.SCROLL_DATA);
            ScrollData data2 = input2.get(TieredDataComponents.SCROLL_DATA);

            if (data1 != null && data2 != null && !data1.attributeId().equals("empty")) {

                // 🌟 Usamos equalsIgnoreCase no Tier para garantir que "Common" junte com "common"
                if (data1.attributeId().equals(data2.attributeId()) && data1.tier().equalsIgnoreCase(data2.tier())) {

                    String nextTier = getNextTier(data1.tier());

                    if (nextTier != null) {
                        int cost = getUpgradeCost(data1.tier());

                        // =================================================================
                        // 🌟 O SEGREDO DO STACK: CRIAR UM ITEM 100% VIRGEM!
                        // Ao invés de copiar, criamos um novo do zero. Assim ele não herda
                        // tags invisíveis (como REPAIR_COST) e fica idêntico ao do Criativo!
                        // =================================================================
                        ItemStack result = new ItemStack(ItemsRegisters.ATTRIBUTE_SCROLL);

                        float newValue = calculateNextValue((float) data1.value());

                        // Injeta o ScrollData na ordem correta (Atributo, Tier, Valor)
                        result.set(TieredDataComponents.SCROLL_DATA, new ScrollData(data1.attributeId(), nextTier, newValue));

                        // Se o jogador digitou um nome na bigorna, nós aplicamos.
                        // Se não, ele fica limpo e usa o seu getName() dinâmico!
                        if (this.newItemName != null && !this.newItemName.isEmpty()) {
                            result.set(DataComponentTypes.CUSTOM_NAME, Text.literal(this.newItemName));
                            cost += 1;
                        }

                        this.getSlot(2).setStack(result);
                        this.levelCost.set(cost);
                        this.repairItemUsage = 1;
                        this.sendContentUpdates();

                        ci.cancel();
                    
                    }
                }
            }
        }
    }

    @Unique
    private String getNextTier(String currentTier) {
        // 🌟 Forçamos tudo para minúsculo. Isso garante que o pergaminho gerado
        // seja idêntico ao pergaminho que vem do baú de loot!
        return switch (currentTier.toLowerCase()) {
            case "common" -> "uncommon";
            case "uncommon" -> "rare";
            case "rare" -> "epic";
            case "epic" -> "legendary";
            case "legendary" -> "unique";
            case "unique" -> "mythic";
            default -> null;
        };
    }

    @Unique
    private int getUpgradeCost(String currentTier) {
        return switch (currentTier.toLowerCase()) {
            case "common" -> 5;
            case "uncommon" -> 10;
            case "rare" -> 15;
            case "epic" -> 20;
            case "legendary" -> 25;
            case "unique" -> 30;
            default -> 0;
        };
    }

    @Unique
    private float calculateNextValue(float currentValue) {
        // 🌟 Lógica em FLOAT puro (Note o 'f' no final dos números)
        if (currentValue < 1.0f && currentValue > 0.0f) {
            return (float) (Math.round((currentValue + 0.01f) * 100.0f) / 100.0f);
        }
        return currentValue + 1.0f;
    }
}