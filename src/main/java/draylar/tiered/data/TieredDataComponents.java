package draylar.tiered.data;


import draylar.tiered.api.ARPGEquipmentData;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class TieredDataComponents {

    // Registra o nosso Super Componente!
    public static final ComponentType<ARPGEquipmentData> ARPG_DATA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("tiered", "arpg_data"),
            ComponentType.<ARPGEquipmentData>builder()
                    .codec(ARPGEquipmentData.CODEC)
                    .packetCodec(ARPGEquipmentData.PACKET_CODEC)
                    .build()
    );

    public static void init() {
        // Apenas para carregar a classe no onInitialize()
    }
}