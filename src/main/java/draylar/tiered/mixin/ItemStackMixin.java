package draylar.tiered.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {


    @Inject(method = "applyAttributeModifiers", at = @At("RETURN"))
    private void applyAttributeModifiersMixin(EquipmentSlot slot, BiConsumer<RegistryEntry<EntityAttribute>, EntityAttributeModifier> consumer, CallbackInfo info) {
        ItemStack itemStack = (ItemStack) (Object) this;

        if (itemStack.get(Tiered.TIER) != null) {
            Identifier tier = ModifierUtils.getAttributeId(itemStack);
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                for (AttributeTemplate template : potentialAttribute.getAttributes()) {

                    // 1. Processa os Slots Obrigatórios
                    List<EquipmentSlot> requiredSlots = template.getRequiredEquipmentSlots();
                    if (requiredSlots != null && requiredSlots.contains(slot)) {
                        if (Tiered.isPreferredEquipmentSlot(itemStack, slot)) {
                            template.applyModifiers(slot, consumer);
                        }
                    }

                    // 2. Processa os Slots Opcionais
                    List<EquipmentSlot> optionalSlots = template.getOptionalEquipmentSlots();
                    if (optionalSlots != null && optionalSlots.contains(slot)) {
                        if (Tiered.isPreferredEquipmentSlot(itemStack, slot)) {
                            template.applyModifiers(slot, consumer);
                        }
                    }
                }
            }
        }
    }
}