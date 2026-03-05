package draylar.tiered.mixin;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.TradeOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.village.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TradeOutputSlot.class)
public abstract class TradeOutputSlotMixin {

    @Shadow @Final private Merchant merchant;

    // 🌟 MUDANÇA CRÍTICA: Mudamos de "HEAD" para "RETURN".
    // Agora nós esperamos o Minecraft e o Mod original fazerem toda a bagunça deles.
    // Quando eles terminarem, nós entramos e damos a palavra final!
    @Inject(method = "onTakeItem", at = @At("RETURN"))
    private void onTakeItemMixin(PlayerEntity player, ItemStack stack, CallbackInfo ci) {

        if (player instanceof ServerPlayerEntity serverPlayer) {

            // 🌟 PASSO 1: O LIMPA-TRILHOS (Sempre executa)
            // Arrancamos qualquer Tier genérico que o evento de Crafting tenha colocado milissegundos atrás.
            ModifierUtils.removeItemStackAttribute(stack);

            // 🌟 PASSO 2: APLICAÇÃO (Só executa se a config principal estiver ligada)
            if (ConfigInit.CONFIG.merchantModifier) {
                int merchantLevel = 0;

                // Verifica se o escalonamento por nível está ligado
                if (ConfigInit.CONFIG.merchantLevelScaling && this.merchant instanceof VillagerEntity villager) {
                    merchantLevel = villager.getVillagerData().level();
                }

                ModifierUtils.setItemStackAttribute(serverPlayer, stack, false, merchantLevel);
            }

            // 🌟 PASSO 3: ANTI-DESYNC (Sempre executa)
            // Atualiza o mouse do jogador para ele ver o item correto (seja com Tier ou Vanilla limpo)
            serverPlayer.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    -1,
                    serverPlayer.currentScreenHandler.nextRevision(),
                    -1,
                    stack
            ));
        }
    }
}