package draylar.tiered.mixin;

import draylar.tiered.config.ConfigInit;
import draylar.tiered.util.ARPGXpHelper;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    // 🛡️ INJECT 1: HEAD (Antes do dano) - EXCLUSIVO PARA O ESCUDO
    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamageShieldCheck(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount <= 0) return;

        if ((Object) this instanceof ServerPlayerEntity defender) {
            // Checa se está bloqueando ANTES do jogo processar e possivelmente cancelar o dano
            boolean isBlocked = defender.isBlocking() && !source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_SHIELD);

            if (isBlocked) {
                ItemStack shield = defender.getOffHandStack();
                if (!(shield.getItem() instanceof net.minecraft.item.ShieldItem)) {
                    shield = defender.getMainHandStack();
                }

                if (shield.getItem() instanceof net.minecraft.item.ShieldItem) {
                    String specificAffinity = null;
                    int specificAmount = 0;
                    int baseAmount = 1;

                    if (amount >= 3.0f) {
                        specificAffinity = "bulwark";
                        specificAmount = 3;
                    }
                    else if (source.getSource() instanceof net.minecraft.entity.projectile.PersistentProjectileEntity) {
                        specificAffinity = "wall";
                        specificAmount = 3;
                    }
                    else if (source.getAttacker() instanceof LivingEntity) {
                        specificAffinity = "spiked_vengeance";
                        specificAmount = 3;
                    }

                    ARPGXpHelper.grantXp(shield, specificAffinity, specificAmount, true, baseAmount, defender);

                    // 🌵 LÓGICA DE VINGANÇA DE ESPINHOS
                    draylar.tiered.api.ARPGEquipmentData data = shield.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
                    if (data != null && !data.isBroken() && data.level() > 0 && "spiked_vengeance".equals(data.affinity())) {
                        if (source.getAttacker() instanceof LivingEntity attackerEntity) {
                            double thornsDamage = draylar.tiered.util.ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());
                            if (thornsDamage > 0) {
                                attackerEntity.damage(world, defender.getDamageSources().thorns(defender), (float) thornsDamage);
                                world.playSound(null, defender.getBlockPos(), net.minecraft.sound.SoundEvents.ENCHANT_THORNS_HIT, net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.0f);
                            }
                        }
                    }
                }
            }
        }
    }

    // ⚔️ INJECT 2: RETURN (Depois do dano) - PARA ARMAS E ARMADURAS
    @Inject(method = "damage", at = @At("RETURN"))
    private void onDamageGiveArmorXp(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Só prossegue se o dano realmente aconteceu e não foi bloqueado/cancelado
        if (!cir.getReturnValue() || amount <= 0) return;

        LivingEntity target = (LivingEntity) (Object) this;

        // 🛡️ 1. O Jogador RECEBEU dano (Treina a Armadura)
        if (target instanceof ServerPlayerEntity player) {
            int xpAmount = ConfigInit.CONFIG.xpBaseTakeDamage;
            if (amount >= 10.0f) xpAmount *= 3;

            boolean isCombatDamage = source.getAttacker() instanceof LivingEntity && source.getAttacker() != player;

            EquipmentSlot[] armorSlots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

            for (EquipmentSlot slot : armorSlots) {
                ItemStack armorStack = player.getEquippedStack(slot);
                if (!armorStack.isEmpty()) {
                    String specificAffinity = null;
                    int specificAmount = 0;

                    if (slot == EquipmentSlot.CHEST && isCombatDamage) {
                        specificAffinity = "bulwark";
                        specificAmount = 3;
                    }
                    else if (slot == EquipmentSlot.FEET && source.isOf(DamageTypes.FALL)) {
                        specificAffinity = "solid_foundation";
                        specificAmount = 4;
                    }

                    boolean isBaseAction = isCombatDamage;
                    int baseAmount = isBaseAction ? xpAmount : 0;

                    if (isBaseAction || specificAffinity != null) {
                        ARPGXpHelper.grantXp(armorStack, specificAffinity, specificAmount, isBaseAction, baseAmount, player);
                    }
                }
            }
        }

        // ⚔️ 2. O Jogador CAUSOU dano (Treina Capacete, Armas, etc)
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {

            // 🪖 CAPACETE
            ItemStack helmet = attacker.getEquippedStack(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                if (amount > 5.0f) {
                    ARPGXpHelper.grantXp(helmet, "focused_mind", 3, false, 0, attacker);
                }
                double distance = attacker.distanceTo(target);
                if (distance >= 2.7) {
                    ARPGXpHelper.grantXp(helmet, "far_sight", 3, false, 0, attacker);
                }
            }

            // 👕 PEITORAL
            ItemStack chestplate = attacker.getEquippedStack(EquipmentSlot.CHEST);
            if (!chestplate.isEmpty()) {
                ARPGXpHelper.grantXp(chestplate, "retaliation", 3, false, 0, attacker);
            }
            // 🪽 ELYTRA: Mergulho Mortal (Causar dano enquanto voa)
            if (attacker.isGliding()) {
                ItemStack elytra = attacker.getEquippedStack(EquipmentSlot.CHEST);
                if (elytra.isOf(net.minecraft.item.Items.ELYTRA)) {
                    ARPGXpHelper.grantXp(elytra, "deadly_dive", 30, false, 0, attacker);
                }
            }

            // ⚔️ ARMAS CORPO A CORPO
            ItemStack weapon = attacker.getMainHandStack();
            if (weapon.isIn(net.minecraft.registry.tag.ItemTags.SWORDS) || weapon.isIn(net.minecraft.registry.tag.ItemTags.AXES)) {

                boolean isKill = target.isDead();
                // Se matou, o XP base é maior (ex: 2), se só bateu, é 1.
                int baseAmount = isKill ? ConfigInit.CONFIG.xpBaseKillEntity : 1;

                boolean isVanillaCrit = !attacker.isOnGround() && attacker.fallDistance > 0.0F &&
                        !attacker.isClimbing() && !attacker.isTouchingWater() &&
                        !attacker.hasVehicle() && !attacker.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS);

                boolean isCritical = isVanillaCrit || draylar.tiered.util.AttributeHelper.shouldMeeleCrit(attacker);

                long currentTick = world.getTime();
                long lastTick = draylar.tiered.util.ARPGXpHelper.lastDamageTick.getOrDefault(attacker.getUuid(), 0L);
                boolean isSweep = (currentTick - lastTick) <= 1;
                draylar.tiered.util.ARPGXpHelper.lastDamageTick.put(attacker.getUuid(), currentTick);

                // 🌟 1. AÇÃO BASE: Causar dano (ou matar) sempre dá o XP Base 1 única vez!
                draylar.tiered.util.ARPGXpHelper.grantXp(weapon, "brute_force", 2, true, baseAmount, attacker);

                // 🌟 2. AÇÕES ESPECÍFICAS: Dão apenas XP Específico (isBaseAction = false, baseAmount = 0)

                // Sede de Sangue
                if (attacker.getHealth() < (attacker.getMaxHealth() * 0.5f)) {
                    draylar.tiered.util.ARPGXpHelper.grantXp(weapon, "bloodthirst", 4, false, 0, attacker);
                }

                // Lâmina Dançante
                if (isSweep) {
                    draylar.tiered.util.ARPGXpHelper.grantXp(weapon, "dancing_blade", 3, false, 0, attacker);
                }

                // Golpe Certeiro
                if (isCritical) {
                    draylar.tiered.util.ARPGXpHelper.grantXp(weapon, "true_strike", 3, false, 0, attacker);
                }

                // Força Bruta
                if (isKill) {
                    draylar.tiered.util.ARPGXpHelper.grantXp(weapon, "brute_force", 3, true, 1, attacker);
                }

                // 🩸 LÓGICA DE ROUBO DE VIDA (Mantida intacta)
                draylar.tiered.api.ARPGEquipmentData data = weapon.get(draylar.tiered.data.TieredDataComponents.ARPG_DATA);
                if (data != null && !data.isBroken() && data.level() > 0 && "bloodthirst".equals(data.affinity())) {
                    double lifestealPercentage = draylar.tiered.util.ARPGAffinityLogic.getBonusValue(data.affinity(), data.level(), data.prestige());
                    if (lifestealPercentage > 0) {
                        float healAmount = (float) (amount * (lifestealPercentage / 100.0));
                        if (healAmount > 0) {
                            attacker.heal(healAmount);
                            if (isKill) {
                                world.playSound(null, attacker.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_PLAYER_BURP, net.minecraft.sound.SoundCategory.PLAYERS, 0.5f, 1.2f);
                            }
                        }
                    }
                }
            }

            // 🏹 ARMAS À DISTÂNCIA (Arco e Besta) - O Impacto
            if (source.getSource() instanceof net.minecraft.entity.projectile.PersistentProjectileEntity arrow) {

                ItemStack rangedWeapon = attacker.getMainHandStack();
                if (!(rangedWeapon.getItem() instanceof net.minecraft.item.BowItem || rangedWeapon.getItem() instanceof net.minecraft.item.CrossbowItem)) {
                    rangedWeapon = attacker.getOffHandStack();
                }

                if (rangedWeapon.getItem() instanceof net.minecraft.item.BowItem || rangedWeapon.getItem() instanceof net.minecraft.item.CrossbowItem) {

                    boolean isKill = target.isDead();
                    boolean isVanillaCrit = arrow.isCritical();
                    boolean isArpgCrit = draylar.tiered.util.AttributeHelper.shouldMeeleCrit(attacker);
                    double distance = attacker.distanceTo(target);

                    // 🌟 CORREÇÃO: Como a puxada do arco já dá o XP Base em outro Mixin,
                    // o impacto da flecha serve APENAS para XP Específico.
                    // isBaseAction = false | baseAmount = 0

                    // 1. Sniper (Tiro longo)
                    if (distance >= 15.0) {
                        draylar.tiered.util.ARPGXpHelper.grantXp(rangedWeapon, "elite_shooter", 7, false, 0, attacker);
                    }

                    // 2. Tiro Crítico
                    if (isVanillaCrit || isArpgCrit) {
                        draylar.tiered.util.ARPGXpHelper.grantXp(rangedWeapon, "eagle_eye", 4, false, 0, attacker);
                    }

                    // 3. Dano Massivo
                    if (amount >= 9.0f) {
                        draylar.tiered.util.ARPGXpHelper.grantXp(rangedWeapon, "heavy_shot", 3, false, 0, attacker);
                    }

                    // 4. Abate
                    if (isKill) {
                        // Abate alimenta o Atirador de Elite
                        draylar.tiered.util.ARPGXpHelper.grantXp(rangedWeapon, "eagle_eye", 3, false, 0, attacker);
                    }
                }
            }
        }
    }
}