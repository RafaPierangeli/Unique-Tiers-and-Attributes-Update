package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityConsumeMixin {


    @Inject(method = "consumeItem", at = @At("HEAD"))
    private void onConsumeGrantChestplateXp(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Como ServerPlayerEntity só existe no servidor, não precisamos checar getWorld().isClient()
        if (entity instanceof ServerPlayerEntity player) {

            // Pega o item que o jogador está terminando de usar (comer/beber)
            ItemStack stack = player.getActiveItem();

            if (stack.isOf(Items.POTION) || stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
                if (!chestplate.isEmpty()) {
                    // 👕 PEITORAL: Bênção da Vida (Usar itens de cura/buff)
                    ARPGXpHelper.grantXp(chestplate, "life_blessing", 4, false, 0, player);
                }
            }
        }
    }
}