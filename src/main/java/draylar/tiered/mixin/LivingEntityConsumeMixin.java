package draylar.tiered.mixin;

import draylar.tiered.util.ARPGSurvivalHelper;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.item.ShieldItem;

@Mixin(LivingEntity.class)
public abstract class LivingEntityConsumeMixin {

    // Precisamos do Shadow para pegar o item que está sendo consumido
    @Shadow public abstract ItemStack getActiveItem();

    @Inject(method = "consumeItem", at = @At("HEAD"))
    private void onConsume(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player && !player.getEntityWorld().isClient()) {

            ItemStack stack = this.getActiveItem();
            if (stack.isEmpty()) return;

            // =================================================================
            // 🧪 1. XP: USO DE POÇÕES/MAÇÃS (life_blessing e vital_guard)
            // =================================================================
            if (stack.isOf(Items.POTION) || stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {

                // 👕 XP para o Peitoral (life_blessing)
                ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
                if (!chestplate.isEmpty()) {
                    ARPGEquipmentData data = chestplate.get(TieredDataComponents.ARPG_DATA);
                    // Treina no nível 0, ou upa se já for life_blessing
                    if (data != null && (data.level() == 0 || "life_blessing".equals(data.affinity()))) {
                        ARPGXpHelper.grantXp(chestplate, "life_blessing", 4, false, 0, player);
                    }
                }

                // 🛡️ XP para o Escudo (vital_guard)
                ItemStack offHand = player.getOffHandStack();
                ItemStack mainHand = player.getMainHandStack();
                ItemStack shield = offHand.getItem() instanceof ShieldItem ? offHand : (mainHand.getItem() instanceof ShieldItem ? mainHand : ItemStack.EMPTY);

                if (!shield.isEmpty()) {
                    ARPGEquipmentData data = shield.get(TieredDataComponents.ARPG_DATA);
                    // Treina no nível 0, ou upa se já for vital_guard
                    if (data != null && (data.level() == 0 || "vital_guard".equals(data.affinity()))) {
                        ARPGXpHelper.grantXp(shield, "vital_guard", 4, false, 0, player);
                    }
                }
            }

            // =================================================================
            // 🍖 2. LÓGICA DE SATURAÇÃO EXTRA (unyielding_vigor)
            // =================================================================
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food != null) {
                // Chama o nosso Helper para somar o bônus de todas as peças equipadas
                double bonusPercentage = ARPGSurvivalHelper.getTotalAffinityBonus(player, "unyielding_vigor");

                if (bonusPercentage > 0) {
                    int extraFood = (int) (food.nutrition() * (bonusPercentage / 100.0));

                    // Nota: Se o seu compilador reclamar de "saturation()", mude para "saturationModifier()"
                    float extraSaturation = (float) (food.saturation() * (bonusPercentage / 100.0));

                    // Garante pelo menos +1 de fome se o bônus existir
                    if (extraFood == 0 && bonusPercentage > 0) extraFood = 1;

                    // Injeta direto no estômago do jogador
                    player.getHungerManager().add(extraFood, extraSaturation);
                }
            }
        }
    }
}