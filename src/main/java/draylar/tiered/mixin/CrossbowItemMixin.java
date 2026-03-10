package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @Inject(method = "use", at = @At("HEAD"))
    private void onShootCrossbowGrantXp(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            ItemStack stack = player.getStackInHand(hand);

            // No 1.21.11, checamos se a besta tem projéteis carregados usando Data Components
            ChargedProjectilesComponent chargedProjectiles = stack.get(DataComponentTypes.CHARGED_PROJECTILES);

            if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
                // A besta sempre atira com força máxima, então sempre treina Saque Rápido
                ARPGXpHelper.grantXp(stack, "quick_draw", 2, true, 1, player);
            }
        }
    }
}
