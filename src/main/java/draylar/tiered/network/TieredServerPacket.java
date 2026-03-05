package draylar.tiered.network;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import draylar.tiered.Tiered;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.network.packet.AttributePacket;
import draylar.tiered.network.packet.HealthPacket;
import draylar.tiered.network.packet.ReforgeItemSyncPacket;
import draylar.tiered.network.packet.ReforgePacket;
import draylar.tiered.network.packet.ReforgeReadyPacket;
import draylar.tiered.reforge.ReforgeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class TieredServerPacket {

    public static void init() {
        // Registra os pacotes de sincronização de dados
        PayloadTypeRegistry.playS2C().register(AttributePacket.PACKET_ID, AttributePacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(HealthPacket.PACKET_ID, HealthPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeReadyPacket.PACKET_ID, ReforgeReadyPacket.PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeItemSyncPacket.PACKET_ID, ReforgeItemSyncPacket.PACKET_CODEC);

        // Registra o pacote do botão "Reforjar"
        PayloadTypeRegistry.playC2S().register(ReforgePacket.PACKET_ID, ReforgePacket.PACKET_CODEC);

        // 🌟 O ÚNICO RECEBEDOR NECESSÁRIO AGORA: O clique no botão de Reforjar!
        ServerPlayNetworking.registerGlobalReceiver(ReforgePacket.PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ReforgeScreenHandler reforgeScreenHandler) {
                    reforgeScreenHandler.reforge();
                }
            });
        });
    }

    public static void writeS2CHealthPacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new HealthPacket(serverPlayerEntity.getHealth()));
    }

    public static void writeS2CReforgeItemSyncPacket(ServerPlayerEntity serverPlayerEntity) {
        List<Identifier> ids = new ArrayList<Identifier>();
        List<Integer> listSize = new ArrayList<Integer>();
        List<Integer> itemIds = new ArrayList<Integer>();

        Tiered.REFORGE_DATA_LOADER.getReforgeIdentifiers().forEach(id -> {
            ids.add(id);

            List<Integer> list = new ArrayList<Integer>();
            Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(Registries.ITEM.get(id)).forEach(item -> {
                list.add(Registries.ITEM.getRawId(item));
            });
            listSize.add(list.size());

            list.forEach(rawId -> {
                itemIds.add(rawId);
            });
        });

        ServerPlayNetworking.send(serverPlayerEntity, new ReforgeItemSyncPacket(ids, listSize, itemIds));
    }

    public static void writeS2CAttributePacket(ServerPlayerEntity serverPlayerEntity) {
        List<String> attributeIds = new ArrayList<String>();
        List<String> attributeJsons = new ArrayList<String>();

        Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            attributeIds.add(id.toString());

            try {
                JsonElement jsonElement = PotentialAttribute.CODEC.encodeStart(JsonOps.INSTANCE, attribute)
                        .getOrThrow(error -> new IllegalStateException("Falha ao codificar atributo para a rede: " + error));

                attributeJsons.add(jsonElement.toString());
            } catch (Exception e) {
                Tiered.LOGGER.error("Erro ao serializar atributo {} para enviar ao cliente", id, e);
            }
        });

        ServerPlayNetworking.send(serverPlayerEntity, new AttributePacket(attributeIds, attributeJsons));
    }
}