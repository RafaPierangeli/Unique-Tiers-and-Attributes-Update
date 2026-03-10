package draylar.tiered.mixin;

import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {

    // Injeta exatamente quando o método addExperience é chamado (jogador absorveu o orbe)
    @Inject(method = "onPlayerCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"))
    private void onCollectOrbGiveHelmetXp(PlayerEntity player, CallbackInfo ci) {
        if (!player.getEntityWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack helmet = serverPlayer.getEquippedStack(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                // Ação Base (1 XP) e Específica para ancient_wisdom (3 XP)
                ARPGXpHelper.grantXp(helmet, "ancient_wisdom", 1, false, 0, serverPlayer);
            }
        }
    }
}
