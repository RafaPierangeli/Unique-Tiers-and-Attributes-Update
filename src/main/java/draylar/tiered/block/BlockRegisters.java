package draylar.tiered.block;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import draylar.tiered.block.ReforgeBlock;

public class BlockRegisters {



    public static final Block REFORGE_BLOCK = registerBlock("reforge_block",
            new ReforgeBlock(AbstractBlock.Settings //
                    .create()
                    .registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("tiered","reforge_block")))
                    .requiresTool()
                    .strength(1.0F)
                    .sounds(BlockSoundGroup.ANVIL)
                    .nonOpaque()
            ));




    private static Block registerBlock(String name, Block block){

        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of("tiered", name), block);
    }

    private static void registerBlockItem(String name, Block block){

        Registry.register(Registries.ITEM, Identifier.of("tiered",name),
                new BlockItem(block, new Item.Settings()
                        .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of("tiered", name)))
                        .useBlockPrefixedTranslationKey()));
    }

    public static void registerModBlocks(){

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {

            entries.add(REFORGE_BLOCK);
        });
    }


}
