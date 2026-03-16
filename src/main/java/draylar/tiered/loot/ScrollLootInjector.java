package draylar.tiered.loot;

import draylar.tiered.item.ItemsRegisters;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.LootFunctionType;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ScrollLootInjector {

    // Registra o tipo da nossa função de loot
    public static final LootFunctionType<ScrollLootFunction> SCROLL_LOOT_TYPE = Registry.register(
            Registries.LOOT_FUNCTION_TYPE,
            Identifier.of("tiered", "scroll_loot"),
            new LootFunctionType<>(ScrollLootFunction.CODEC)
    );

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {

            String path = key.getValue().getPath();

            // 🌟 SE FOR UM BAÚ DE ESTRUTURA
            if (path.startsWith("chests/")) {

                // =================================================================
                // 🌟 TRAVA DE SEGURANÇA: LOOT TABLE INCEPTION
                // As Câmaras do Desafio usam sub-tabelas (reward_common, reward_rare, etc).
                // Se não ignorarmos elas, o pergaminho é injetado múltiplas vezes e chove loot!
                // =================================================================
                if (path.contains("trial_chambers") && (path.contains("_common") || path.contains("_rare") || path.contains("_unique"))) {
                    return; // Sai da injeção e deixa a tabela principal fazer o trabalho dela
                }

                // Quantidade de pergaminhos garantidos por baú
                int rolls = 2;

                // 🌟 BALANCEAMENTO AAA: Já que a chance é 100%, baús difíceis dropam MAIS pergaminhos!
                if (path.contains("ancient_city") || path.contains("end_city_treasure")) {
                    rolls = 4; // Ancient City dropa 3 pergaminhos garantidos!
                } else if (path.contains("stronghold") || path.contains("bastion")) {
                    rolls = 3; // Bastion e Stronghold dropam 2!
                }

                // Cria a "Roleta" do nosso pergaminho (Sem RandomChance, ou seja, 100% garantido)
                LootPool.Builder poolBuilder = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(rolls)) // Rola 1, 2 ou 3 vezes
                        .with(ItemEntry.builder(ItemsRegisters.ATTRIBUTE_SCROLL)
                                .apply(ScrollLootFunction.builder())); // Aplica a nossa função que rola o Tier!

                // Injeta a roleta no baú
                tableBuilder.pool(poolBuilder);
            }
        });
    }
}