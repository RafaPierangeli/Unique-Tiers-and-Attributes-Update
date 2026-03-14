package draylar.tiered.client;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.api.ARPGEquipmentData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.EffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class ARPGParticleEngine {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Travas de segurança: Só roda se o jogador estiver no mundo e o jogo não estiver pausado
            if (client.player == null || client.world == null || client.isPaused()) {
                return;
            }

            PlayerEntity player = client.player;
            Random random = player.getRandom();

            // Varre todos os 6 slots de equipamento do jogador (Cabeça, Peito, Pernas, Pés, Mão Principal, Mão Secundária)
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getEquippedStack(slot);

                if (stack.isEmpty() || !stack.contains(TieredDataComponents.ARPG_DATA)) {
                    continue;
                }

                ARPGEquipmentData data = stack.get(TieredDataComponents.ARPG_DATA);
                if (data == null || data.prestige() < 1) {
                    continue;
                }

                // 🌟 1. A ESCALA DE PODER (Os 4 Níveis de Partículas)
                int chance = 0;
                if (data.prestige() == 1) {
                    chance = 5; // Nível 1: 5% por tick (~1 partícula por segundo)
                } else if (data.prestige() == 2) {
                    chance = 15; // Nível 2: 15% por tick (~3 partículas por segundo)
                } else if (data.prestige() >= 3) {
                    if (data.level() >= 100) {
                        chance = 40; // Nível 4 (Ápice): 40% por tick (~8 partículas por segundo)
                    } else {
                        chance = 25; // Nível 3: 25% por tick (~5 partículas por segundo)
                    }
                }

                // Rola o dado de 0 a 100. Se cair dentro da chance, spawna a partícula!
                if (random.nextInt(100) < chance) {

                    // 🌟 2. A COR DO TIER
                    int color = 0xFFFFFFFF; // Branco padrão de segurança
                    Identifier tierId = ModifierUtils.getAttributeId(stack);

                    if (tierId != null) {
                        PotentialAttribute tier = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
                        if (tier != null && tier.getStyle() != null && tier.getStyle().getColor() != null) {
                            color = tier.getStyle().getColor().getRgb();
                        }
                    }

                    // Garante que a cor seja 100% opaca (Alpha = FF)
                    int argbColor = color | 0xFF000000;

                    // 🌟 3. A CIRURGIA ESPACIAL (Posição baseada no item)
                    double x = player.getX() + (random.nextDouble() - 0.5) * 1.2;
                    double z = player.getZ() + (random.nextDouble() - 0.5) * 1.2;
                    double y = player.getY();

                    switch (slot) {
                        case HEAD -> y += 1.5 + random.nextDouble() * 0.5; // Sai da cabeça
                        case CHEST -> y += 0.9 + random.nextDouble() * 0.6; // Sai do peito
                        case LEGS -> y += 0.4 + random.nextDouble() * 0.5; // Sai das pernas
                        case FEET -> y += random.nextDouble() * 0.4; // Sai dos pés
                        case MAINHAND, OFFHAND -> {
                            // Armas: Ficam flutuando ao redor do tronco/cintura
                            x = player.getX() + (random.nextDouble() - 0.5) * 1.5;
                            z = player.getZ() + (random.nextDouble() - 0.5) * 1.5;
                            y += 0.8 + random.nextDouble() * 0.5;
                        }
                    }

                    // 🌟 4. O SPAWN
                    client.world.addParticleClient(
                            EffectParticleEffect.of(ParticleTypes.EFFECT, argbColor,0),
                            x, y, z,
                            0, 0, 0 // Velocidade zero para a partícula flutuar suavemente
                    );
                }
            }
        });
    }
}