package draylar.tiered.mixin.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void getNameMixin(CallbackInfoReturnable<Text> info) {
        ItemStack stack = (ItemStack) (Object) this;

        if (stack.get(Tiered.TIER) != null) {
            Identifier tierId = Identifier.of(stack.get(Tiered.TIER).tier());

            if (Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().containsKey(tierId)) {
                PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);

                if (potentialAttribute != null) {
                    // 🌟 CORREÇÃO: Limpando o ID para remover nomes de pastas (ex: "all_armor/")
                    String rawId = potentialAttribute.getID();
                    String cleanId = rawId;

                    if (rawId.contains("/")) {
                        String namespace = rawId.split(":")[0];
                        String path = rawId.substring(rawId.lastIndexOf('/') + 1);
                        cleanId = namespace + ":" + path;
                    }

                    // 🌟 CORREÇÃO: Usando Text.literal(" ") para o espaço, padrão da 1.21.11
                    info.setReturnValue(Text.translatable(cleanId + ".label")
                            .append(Text.literal(" "))
                            .append(info.getReturnValue())
                            .setStyle(potentialAttribute.getStyle()));
                }
            }
        }
    }
}