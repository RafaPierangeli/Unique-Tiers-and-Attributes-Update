package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public abstract class BowItemMixin {

    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void onShootBowGrantXp(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            // Calcula o tempo que o jogador segurou o arco
            int useTime = ((BowItem)(Object)this).getMaxUseTime(stack, user) - remainingUseTicks;
            float pullProgress = BowItem.getPullProgress(useTime);

            // Se puxou quase até o fim (0.8 a 1.0), treina o Saque Rápido
            if (pullProgress >= 0.8f) {
                ARPGXpHelper.grantXp(stack, "heavy_shot", 3, true, 2, player);
            }
            // Se deu um tirinho fraco, ganha só o XP base de uso
            else if (pullProgress < 0.8f) {
                ARPGXpHelper.grantXp(stack, "quick_draw", 3, true, 2, player);
            }
        }
    }
}
