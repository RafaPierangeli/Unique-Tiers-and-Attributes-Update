package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {

    @Inject(method = "use", at = @At("RETURN"))
    private void onUseFirework(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        // Se a ação foi um sucesso e o jogador está voando
        if (!world.isClient() && cir.getReturnValue().isAccepted() && user.isGliding() && user instanceof ServerPlayerEntity player) {

            ItemStack elytra = player.getEquippedStack(EquipmentSlot.CHEST);
            if (elytra.isOf(Items.ELYTRA)) {
                // Vendaval: Impulso com foguete
                ARPGXpHelper.grantXp(elytra, "gale", 3, true, 3, player);
            }
        }
    }
}