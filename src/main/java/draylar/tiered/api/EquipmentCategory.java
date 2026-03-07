package draylar.tiered.api;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

public enum EquipmentCategory {
    MELEE_WEAPON,
    RANGED_WEAPON,
    ARMOR_SHIELD,
    ELYTRA,
    TOOL,
    FISHING_ROD,
    UNKNOWN;

    public static EquipmentCategory getCategory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return UNKNOWN;
        }

        // 🗡️ Checa Armas Melee (Espadas, Machados, Maças e Tridentes)
        if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isOf(Items.MACE) || stack.isOf(Items.TRIDENT)) {
            return MELEE_WEAPON;
        }

        // 🏹 Checa Armas à Distância (Arcos e Bestas)
        if (stack.isOf(Items.BOW) || stack.isOf(Items.CROSSBOW)) {
            return RANGED_WEAPON;
        }

        // 🛡️ Checa Armaduras e Escudos
        if (stack.isIn(ItemTags.HEAD_ARMOR) || stack.isIn(ItemTags.CHEST_ARMOR) ||
                stack.isIn(ItemTags.LEG_ARMOR) || stack.isIn(ItemTags.FOOT_ARMOR) ||
                stack.isOf(Items.SHIELD)) {
            return ARMOR_SHIELD;
        }

        // 🪽 Checa Elytra
        if (stack.isOf(Items.ELYTRA)) {
            return ELYTRA;
        }

        // ⛏️ Checa Ferramentas (Picaretas, Pás e Enxadas)
        if (stack.isIn(ItemTags.PICKAXES) || stack.isIn(ItemTags.SHOVELS) || stack.isIn(ItemTags.HOES)) {
            return TOOL;
        }

        // 🎣 Checa Vara de Pescar
        if (stack.isOf(Items.FISHING_ROD)) {
            return FISHING_ROD;
        }

        return UNKNOWN;
    }
}