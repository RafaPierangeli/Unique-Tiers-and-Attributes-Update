package draylar.tiered;

import draylar.tiered.api.*;
import draylar.tiered.block.BlockRegisters;
import draylar.tiered.command.CommandInit;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.data.AttributeDataLoader;
import draylar.tiered.data.ReforgeDataLoader;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.network.TieredServerPacket;
import draylar.tiered.reforge.ReforgeScreenHandler;
import draylar.tiered.util.AoEMiningHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.UnaryOperator;




public class Tiered implements ModInitializer {

    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    public static final ReforgeDataLoader REFORGE_DATA_LOADER = new ReforgeDataLoader();

    public static ScreenHandlerType<ReforgeScreenHandler> REFORGE_SCREEN_HANDLER_TYPE;

    public static final ComponentType<TierComponent> TIER = registerComponent("tiered:tier", builder -> builder.codec(TierComponent.CODEC).packetCodec(TierComponent.PACKET_CODEC));

    public static final Logger LOGGER = LogManager.getLogger();


    @Override
    public void onInitialize() {
        ConfigInit.init();
        TieredItemTags.init();
        CustomEntityAttributes.init();
        CommandInit.init();
        TieredDataComponents.init();
        ARPGEventHandlers.register(); // 🌟 Registra os eventos do nosso ARPG!
        draylar.tiered.util.AoEMiningHelper.registerToggleEvent();
        AoEMiningHelper.registerTillingEvent();

        // Coloque isso dentro do seu método onInitialize()
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                AoEMiningHelper.processAoEMining((ServerWorld) world, serverPlayer, pos, state);
            }
        });

        BlockRegisters.registerModBlocks();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.ATTRIBUTE_DATA_LOADER);
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(Tiered.REFORGE_DATA_LOADER);

        // Coloque isso DENTRO do seu método onInitialize()
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            // alive = false significa que o jogador morreu (não é só uma viagem pelo portal do The End)
            if (!alive) {
                Map<Integer, ItemStack> soulboundItems = ((SoulboundAccessor) oldPlayer).tiered$getSoulboundItems();

                for (Map.Entry<Integer, ItemStack> entry : soulboundItems.entrySet()) {
                    int slot = entry.getKey();
                    ItemStack stack = entry.getValue();

                    // Tenta devolver exatamente no mesmo slot que estava antes de morrer
                    if (newPlayer.getInventory().getStack(slot).isEmpty()) {
                        newPlayer.getInventory().setStack(slot, stack);
                    } else {
                        // Se por algum motivo bizarro o slot estiver ocupado, joga no inventário geral
                        newPlayer.getInventory().insertStack(stack);
                    }
                }
            }
        });





        REFORGE_SCREEN_HANDLER_TYPE = Registry.register(Registries.SCREEN_HANDLER, "tiered:reforge",
                new ScreenHandlerType<>((syncId, inventory) -> new ReforgeScreenHandler(syncId, inventory, ScreenHandlerContext.EMPTY), FeatureFlags.VANILLA_FEATURES));

        TieredServerPacket.init();

        ServerPlayConnectionEvents.JOIN.register((network, packetSender, minecraftServer) -> {
            TieredServerPacket.writeS2CReforgeItemSyncPacket(network.getPlayer());
            TieredServerPacket.writeS2CAttributePacket(network.getPlayer());
            TieredServerPacket.writeS2CHealthPacket(network.getPlayer());
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (success) {
                for (int i = 0; i < server.getPlayerManager().getPlayerList().size(); i++) {
                    ModifierUtils.updateItemStackComponent(server.getPlayerManager().getPlayerList().get(i).getInventory());
                }
                LOGGER.info("Finished reload on {}", Thread.currentThread());
            } else {
                LOGGER.error("Failed to reload on {}", Thread.currentThread());
            }
        });


        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            ModifierUtils.updateItemStackComponent(handler.player.getInventory());
        });

    }

    private static <T> ComponentType<T> registerComponent(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, id, builderOperator.apply(ComponentType.builder()).build());
    }


    public static Identifier id(String path) {
        return Identifier.of("tiered", path);
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        // 🌟 CORREÇÃO APLICADA AQUI:
        // Usamos o Data Component EQUIPPABLE em vez da interface Equipment
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            return equippable.slot() == slot;
        }

        if (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof RangedWeaponItem || stack.isIn(TieredItemTags.MAIN_OFFHAND_ITEM)) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }

        return slot == EquipmentSlot.MAINHAND;
    }
}