package draylar.tiered.mixin;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ARPGXpHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin {

    // Intercepta o método de usar a enxada no bloco logo após ele terminar de rodar (RETURN)
    @Inject(method = "useOnBlock", at = @At("RETURN"))
    private void onTillDirtGrantXp(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = context.getPlayer();

        // cir.getReturnValue().isAccepted() garante que a ação foi um SUCESSO (a terra foi arada)
        // Se ele clicar numa pedra, retorna PASS ou FAIL, e o código ignora.
        if (cir.getReturnValue().isAccepted() && player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack hoe = context.getStack();

            if (!hoe.isEmpty()) {
                // 🌾 ENXADA: Mestre do Solo (Arar a terra)
                // Dá 3 XP específico para soil_master e o XP Base de mineração
                int baseAmount = ConfigInit.CONFIG.xpBaseMineBlock;
                ARPGXpHelper.grantXp(hoe, "soil_master", 3, true, baseAmount, serverPlayer);
            }
        }
    }
}