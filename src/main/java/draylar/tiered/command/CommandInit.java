package draylar.tiered.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CommandInit {

    private static final List<String> TIER_LIST = List.of("common", "uncommon", "rare", "epic", "legendary", "unique","mythic");

    public static void init() {
        // 🌟 CORREÇÃO 1: Assinatura atualizada para a 1.21.11 (registryAccess)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tiered")
                    // 🌟 CORREÇÃO 2: NÍVEL DE ACESSO! Apenas OP (nível 2) pode usar este comando
                    .requires(CommandManager.requirePermissionLevel(CommandManager.ADMINS_CHECK))
                    .then(CommandManager.literal("tier")
                            .then(CommandManager.argument("targets", EntityArgumentType.players())
                                    .then(CommandManager.literal("common").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 0)))
                                    .then(CommandManager.literal("uncommon").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 1)))
                                    .then(CommandManager.literal("rare").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 2)))
                                    .then(CommandManager.literal("epic").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 3)))
                                    .then(CommandManager.literal("legendary").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 4)))
                                    .then(CommandManager.literal("unique").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 5)))
                                    .then(CommandManager.literal("mythic").executes((context) -> executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), 6)))
                            )
                    )
                    .then(CommandManager.literal("untier")
                            .then(CommandManager.argument("targets", EntityArgumentType.players()).executes((context) -> {
                                return executeCommand(context.getSource(), EntityArgumentType.getPlayers(context, "targets"), -1);
                            }))
                    )
            );
        });
    }

    // 0: common; 1: uncommon; 2: rare; 3: epic; 4: legendary; 5: unique
    private static int executeCommand(ServerCommandSource source, Collection<ServerPlayerEntity> targets, int tier) {
        for (ServerPlayerEntity player : targets) {
            ItemStack itemStack = player.getMainHandStack();

            if (itemStack.isEmpty()) {
                source.sendFeedback(() -> Text.translatable("commands.tiered.failed", player.getDisplayName()), true);
                continue;
            }

            if (tier == -1) {
                if (itemStack.get(Tiered.TIER) != null) {
                    ModifierUtils.removeItemStackAttribute(itemStack);
                    source.sendFeedback(() -> Text.translatable("commands.tiered.untier", itemStack.getName().getString(), player.getDisplayName()), true);
                } else {
                    source.sendFeedback(() -> Text.translatable("commands.tiered.untier_failed", itemStack.getName().getString(), player.getDisplayName()), true);
                }
            } else {
                List<PotentialAttribute> potentialTiers = new ArrayList<>();

                // Filtra os atributos válidos para o item e que correspondem à raridade escolhida
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(Registries.ITEM.getId(itemStack.getItem()))) {
                        String path = id.getPath();
                        String targetRarity = TIER_LIST.get(tier);

                        if (path.contains(targetRarity)) {
                            // Evita que "uncommon" seja pego quando procuramos por "common"
                            if (targetRarity.equals("common") && path.contains("uncommon")) {
                                return;
                            }
                            potentialTiers.add(attribute);
                        }
                    }
                });

                if (potentialTiers.isEmpty()) {
                    source.sendFeedback(() -> Text.translatable("commands.tiered.tiering_failed", itemStack.getName().getString(), player.getDisplayName()), true);
                    continue;
                }

                // Limpa o item antes de aplicar o novo tier
                ModifierUtils.removeItemStackAttribute(itemStack);

                // Sorteia um dos tiers válidos daquela raridade
                PotentialAttribute selectedAttribute = potentialTiers.get(player.getEntityWorld().getRandom().nextInt(potentialTiers.size()));

                // 🌟 CORREÇÃO 3: Usa o nosso metodo perfeito para aplicar os status e a durabilidade!
                ModifierUtils.applyARPGModifiers(itemStack, selectedAttribute);

                source.sendFeedback(() -> Text.translatable("commands.tiered.tier", itemStack.getName().getString(), player.getDisplayName()), true);
            }
        }
        return 1;
    }
}