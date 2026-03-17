package draylar.tiered.loot;

import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ItemsRegisters;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class ScrollTradeInjector {

    public static void register() {
        // 🌟 Adiciona no Bibliotecário (Librarian) no Nível 4 (Expert)
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 4, factories -> {
            factories.add((entity, level, random) -> createScrollTrade(random, 4));
        });

        // 🌟 Adiciona no Bibliotecário no Nível 5 (Master)
        TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 5, factories -> {
            factories.add((entity, level, random) -> createScrollTrade(random, 5));
        });

        // =================================================================
        // 🌟 ADICIONA NO VENDEDOR AMBULANTE (WANDERING TRADER)
        // O nível 2 representa as trocas "Raras" dele (ele sempre escolhe 1 troca rara por visita)
        // Passamos o número '6' apenas para a nossa lógica interna saber que é ele.
        // =================================================================
        // =================================================================
        // 🌟 ADICIONA NO VENDEDOR AMBULANTE (WANDERING TRADER)
        // Como ele usa a mesma interface TradeOffers.Factory, a lambda
        // OBRIGATORIAMENTE precisa dos 3 parâmetros: (entity, level, random)
        // =================================================================
        // =================================================================
        // 🌟 ADICIONA NO VENDEDOR AMBULANTE (WANDERING TRADER)
        // Na 1.21.11, o Vendedor usa um sistema de "Pools" (Piscinas de Trocas).
        // Criamos uma piscina exclusiva para o nosso mod!
        // =================================================================
        TradeOfferHelper.registerWanderingTraderOffers(builder -> {
            builder.pool(
                    net.minecraft.util.Identifier.of("tiered", "wandering_scrolls"), // ID único da nossa piscina
                    1, // Quantas trocas ele vai "pescar" dessa piscina (1 = 100% de chance de trazer)
                    (entity, level, random) -> createScrollTrade(random, 6) // A nossa fábrica de trocas
            );
        });

    }

    private static TradeOffer createScrollTrade(Random random, int villagerLevel) {
        ItemStack scroll = new ItemStack(ItemsRegisters.ATTRIBUTE_SCROLL);

        // Rola o Tier e o Atributo
        String tier = rollTradeTier(random, villagerLevel);
        String attribute = rollAttribute(random);
        float value = calculateValue(tier);

        // Aplica os dados no pergaminho
        scroll.set(TieredDataComponents.SCROLL_DATA, new ScrollData(attribute, tier, value));

        // Calcula o preço em Esmeraldas baseado no Tier
        int emeraldCost = getEmeraldCost(tier, random);

        // Retorna a oferta: Custa X Esmeraldas, entrega 1 Pergaminho
        return new TradeOffer(
                new TradedItem(Items.EMERALD, emeraldCost),
                scroll,
                3, // Máximo de usos antes de bloquear a troca
                villagerLevel == 4 ? 15 : 30, // XP que o Villager ganha
                0.05f // Multiplicador de preço se o jogador bater no villager
        );
    }

    private static String rollTradeTier(Random random, int level) {
        int roll = random.nextInt(100);
        if (level == 4) {
            // Nível 4: Comum a Épico
            if (roll < 50) return "common";
            if (roll < 80) return "uncommon";
            if (roll < 95) return "rare";
            return "epic";
        } else if (level == 5) {
            // Nível 5: Incomum a Lendário (Pequena chance de Único)
            if (roll < 40) return "uncommon";
            if (roll < 70) return "rare";
            if (roll < 90) return "epic";
            if (roll < 98) return "legendary";
            return "unique";
        } else {
        // 🌟 Vendedor Ambulante (level == 6): Uma roleta mais caótica e aventureira!
        // Ele pode trazer desde lixo até coisas muito boas.
        if (roll < 30) return "rare";
        if (roll < 60) return "epic";
        if (roll < 85) return "legendary";
        if (roll < 98) return "unique";
        return "mythic"; // 2% de chance de trazer um Mitico!
    }
    }

    private static String rollAttribute(Random random) {
        String[] attributes = {
                "max_health",
                "attack_damage",
                "armor",
                "movement_speed",
                "attack_speed",
                "tiered:critical_chance",
                "tiered:range_attack_damage"
        };
        return attributes[random.nextInt(attributes.length)];
    }

    private static float calculateValue(String tier) {
        int tierIndex = List.of("common", "uncommon", "rare", "epic", "legendary", "unique", "mythic").indexOf(tier);
        return 1.0f + tierIndex;
    }

    private static int getEmeraldCost(String tier, Random random) {
        return switch (tier) {
            case "common" -> 5 + random.nextInt(5);      // 5 a 9 Esmeraldas
            case "uncommon" -> 10 + random.nextInt(6);   // 10 a 15 Esmeraldas
            case "rare" -> 18 + random.nextInt(7);       // 18 a 24 Esmeraldas
            case "epic" -> 30 + random.nextInt(11);      // 30 a 40 Esmeraldas
            case "legendary" -> 45 + random.nextInt(16); // 45 a 60 Esmeraldas
            case "unique" -> 64;                         // 64 Esmeraldas (Máximo)
            default -> 10;
        };
    }
}