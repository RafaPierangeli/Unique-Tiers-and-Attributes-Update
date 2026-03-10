package draylar.tiered.mixin;

import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.util.ARPGAffinityLogic;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityExhaustionMixin {

    // Intercepta o valor 'exhaustion' antes dele ser adicionado ao jogador
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float modifyExhaustionGain(float originalExhaustion) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Só funciona no servidor para evitar dessincronização
        if (!player.getEntityWorld().isClient()) {
            ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);

            if (!leggings.isEmpty() && leggings.contains(TieredDataComponents.ARPG_DATA)) {
                ARPGEquipmentData data = leggings.get(TieredDataComponents.ARPG_DATA);

                // Se a calça tem a afinidade Vigor Inabalável e está despertada
                if (data != null && !data.isBroken() && data.level() > 0 && "unyielding_vigor".equals(data.affinity())) {

                    // Pega o valor do bônus (Ex: Nível 50 = 50.0)
                    double bonusValue = ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());

                    // Converte para multiplicador de redução (Ex: 50.0 -> 0.5 de redução)
                    // Limitamos a 1.0 (100%) para o jogador não ganhar fome correndo (valores negativos)
                    float reductionPercentage = (float) Math.min(bonusValue / 100.0, 0.85);

                    // Aplica o desconto! Se a redução for 100%, a exaustão vira 0.
                    return originalExhaustion * (1.0f - reductionPercentage);
                }
            }
        }

        return originalExhaustion;
    }
}