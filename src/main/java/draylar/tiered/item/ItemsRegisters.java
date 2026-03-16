package draylar.tiered.item;

import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import static com.mojang.text2speech.Narrator.LOGGER;

public class ItemsRegisters {

    // 🌟 CORREÇÃO CRÍTICA: Usando 'new ScrollItem' em vez de 'new Item'
    public static final Item ATTRIBUTE_SCROLL = registerItem("attribute_scroll", new ScrollItem(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tiered", "attribute_scroll")))));

    public static final Item MAGIC_PIERCER = registerItem("magic_piercer", new Item(new Item.Settings().maxCount(64)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tiered", "magic_piercer")))));

    public static final Item MAGIC_EXTRACTOR = registerItem("magic_extractor", new Item(new Item.Settings().maxCount(64)
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tiered", "magic_extractor")))));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of("tiered", name), item);
    }

    public static void registerModItems() {
        LOGGER.info("Registering Tiered Items for tiered");

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            // Adiciona os itens utilitários normais
            entries.add(MAGIC_PIERCER);
            entries.add(MAGIC_EXTRACTOR);

            // 🌟 O LOOP DO CRIATIVO FICA AQUI! (Common Side)
            String[] tiers = {"common", "uncommon", "rare", "epic", "legendary", "unique", "mythic"};
            String[] attributes = {
                    "max_health",
                    "attack_damage",
                    "attack_speed",
                    "tiered:critical_chance",
                    "armor",
                    "movement_speed",
                    "tiered:range_attack_damage"
            };

            // Gera todas as 49 combinações de pergaminhos dinamicamente
            for (String attr : attributes) {
                for (int i = 0; i < tiers.length; i++) {
                    String tier = tiers[i];
                    float value = (i + 1) * 1.0f;

                    ItemStack scroll = new ItemStack(ATTRIBUTE_SCROLL);
                    scroll.set(TieredDataComponents.SCROLL_DATA, new ScrollData(attr, tier, value));

                    entries.add(scroll);
                }
            }
        });
    }
}