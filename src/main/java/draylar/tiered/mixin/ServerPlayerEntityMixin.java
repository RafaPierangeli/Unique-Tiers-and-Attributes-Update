package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTickElytraXp(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // A cada 5 segundos (100 ticks) voando, o jogador ganha XP
        if (player.isGliding() && player.age % 100 == 0) {
            ItemStack elytra = player.getEquippedStack(EquipmentSlot.CHEST);

            if (elytra.isOf(Items.ELYTRA)) {
                // Acrobata: Manter-se no ar (Ganha 2 XP Específico + 1 XP Base)
                ARPGXpHelper.grantXp(elytra, "acrobat", 2, true, 1, player);
            }
        }
    }
}