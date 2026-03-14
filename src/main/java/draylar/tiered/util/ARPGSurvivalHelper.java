package draylar.tiered.util;

import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.data.TieredDataComponents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class ARPGSurvivalHelper {

    /**
     * 🌟 MÉTODO AUXILIAR: Soma o bônus de todos os itens equipados.
     */
    public static double getTotalAffinityBonus(ServerPlayerEntity player, String targetAffinity) {
        double totalBonus = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = player.getEquippedStack(slot);
            if (equipped != null && !equipped.isEmpty()) {
                ARPGEquipmentData data = equipped.get(TieredDataComponents.ARPG_DATA);
                if (data != null && !data.isBroken() && data.level() > 0 && targetAffinity.equals(data.affinity())) {
                    totalBonus += ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());
                }
            }
        }
        return totalBonus;
    }
}