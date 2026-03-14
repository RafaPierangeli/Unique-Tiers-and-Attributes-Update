package draylar.tiered.mixin;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.util.ARPGAffinityLogic;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {

    // =================================================================
    // 💖 1. EFEITO: AUMENTO DE CURA (life_blessing e vital_guard)
    // =================================================================
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float modifyHealAmount(float amount) {
        if ((Object) this instanceof ServerPlayerEntity player && !player.getEntityWorld().isClient()) {

            double totalBonusPercentage = 0;

            // 👕 Checa o Peitoral (life_blessing)
            ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
            if (!chestplate.isEmpty()) {
                ARPGEquipmentData data = chestplate.get(TieredDataComponents.ARPG_DATA);
                if (data != null && !data.isBroken() && data.level() > 0 && "life_blessing".equals(data.affinity())) {
                    totalBonusPercentage += ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());
                }
            }

            // 🛡️ Checa o Escudo (vital_guard)
            ItemStack offHand = player.getOffHandStack();
            ItemStack mainHand = player.getMainHandStack();
            ItemStack shield = offHand.getItem() instanceof ShieldItem ? offHand : (mainHand.getItem() instanceof ShieldItem ? mainHand : ItemStack.EMPTY);

            if (!shield.isEmpty()) {
                ARPGEquipmentData data = shield.get(TieredDataComponents.ARPG_DATA);
                if (data != null && !data.isBroken() && data.level() > 0 && "vital_guard".equals(data.affinity())) {
                    totalBonusPercentage += ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());
                }
            }

            if (totalBonusPercentage > 0) {
                return amount * (float) (1.0 + (totalBonusPercentage / 100.0));
            }
        }
        return amount;
    }

    // =================================================================
    // 📈 2. XP: GANHO AO CURAR (titan_heart e winds_of_life)
    // =================================================================
    @Inject(method = "heal", at = @At("HEAD"))
    private void onHealGrantXp(float amount, CallbackInfo ci) {
        if (amount > 0 && (Object) this instanceof ServerPlayerEntity player && !player.getEntityWorld().isClient()) {

            // 🌟 SISTEMA ANTI-EXPLOIT DE REGENERAÇÃO
            float chance = amount * 0.40f;

            if (player.getRandom().nextFloat() < chance) {

                // 👕 XP APENAS para o Peitoral/Elytra
                ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
                if (!chestplate.isEmpty()) {
                    String targetAffinity = "titan_heart"; // Padrão para nível 0

                    ARPGEquipmentData data = chestplate.get(TieredDataComponents.ARPG_DATA);
                    if (data != null && data.level() > 0) {
                        String affinity = data.affinity();
                        // Só dá XP se já for uma dessas duas afinidades
                        if ("titan_heart".equals(affinity) || "winds_of_life".equals(affinity)) {
                            targetAffinity = affinity;
                        } else {
                            return; // Ignora se for outra afinidade
                        }
                    }

                    ARPGXpHelper.grantXp(chestplate, targetAffinity, 3, false, 0, player);
                }
            }
        }
    }
}