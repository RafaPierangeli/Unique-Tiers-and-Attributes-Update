package draylar.tiered.reforge;

import draylar.tiered.Tiered;
import draylar.tiered.api.ARPGEquipmentData;
import draylar.tiered.api.ScrollData;
import draylar.tiered.data.TieredDataComponents;
import draylar.tiered.item.ItemsRegisters;
import draylar.tiered.util.ScrollHelper;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

public class MagicStationScreenHandler extends ScreenHandler {

    // 🌟 AGORA SÃO 7 SLOTS (O último é a saída da extração)
    private final Inventory inventory = new SimpleInventory(7) {
        @Override
        public void markDirty() {
            super.markDirty();
            MagicStationScreenHandler.this.onContentChanged(this);
        }
    };

    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    private final PropertyDelegate propertyDelegate;
    private int selectedExtractionSlot = -1;

    public MagicStationScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }
    public int getBookshelfCount() {
        return this.propertyDelegate.get(2);
    }

    public MagicStationScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Tiered.MAGIC_STATION_SCREEN_HANDLER_TYPE, syncId);
        this.context = context;
        this.player = playerInventory.player;

        this.propertyDelegate = new PropertyDelegate() {
            private final int[] values = new int[3]; // 🌟 Agora são 3 valores!
            @Override public int get(int index) { return values[index]; }
            @Override public void set(int index, int value) { values[index] = value; }
            @Override public int size() { return 3; }
        };
        this.addProperties(this.propertyDelegate);

        // 🌟 LINHA SUPERIOR (Ajuste os valores X e Y conforme sua textura)
        this.addSlot(new Slot(this.inventory, 0, 17, 23));

        this.addSlot(new Slot(this.inventory, 1, 51, 23) {
            @Override
            public boolean canInsert(ItemStack stack) {
                // 🌟 TROCADO PARA AMETHYST SHARD
                return stack.isOf(ItemsRegisters.MAGIC_PIERCER) ||
                        stack.isOf(ItemsRegisters.MAGIC_EXTRACTOR) ||
                        stack.isOf(Items.AMETHYST_SHARD);
            }
        });

        this.addSlot(new Slot(this.inventory, 2, 86, 23) {
            @Override
            public boolean canInsert(ItemStack stack) {
                // 🌟 TROCADO PARA END CRYSTAL
                return stack.isOf(Items.END_CRYSTAL);
            }
        });

        // 🌟 LINHA INFERIOR (Buracos da Arma)
        for (int i = 0; i < 3; i++) {
            final int slotIndex = i;
            // 🌟 AJUSTE FINO: Soma 1 pixel extra apenas no 3º slot (index 2)
            int slotX = 17 + (i * 34) + (i == 2 ? 1 : 0);

            this.addSlot(new Slot(this.inventory, 3 + i, slotX, 51) { // Ajuste o Y (50) conforme sua textura
                @Override
                public boolean canInsert(ItemStack stack) {
                    return isInsertionMode() && stack.isOf(ItemsRegisters.ATTRIBUTE_SCROLL) && isSlotUnlocked(slotIndex);
                }
                @Override
                public boolean canTakeItems(PlayerEntity playerEntity) {
                    return isInsertionMode();
                }
            });
        }

        // 🌟 NOVO: SLOT DE SAÍDA (Extração)
        // Alinhado na altura do botão (Y=35) e depois do 3º slot (X=115)
        this.addSlot(new Slot(this.inventory, 6, 115, 37) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false; // O jogador não pode colocar itens aqui, só tirar!
            }
        });

        // Inventário do Jogador
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    public boolean isPiercingMode() { return this.getSlot(1).getStack().isOf(ItemsRegisters.MAGIC_PIERCER); }
    public boolean isExtractionMode() { return this.getSlot(1).getStack().isOf(ItemsRegisters.MAGIC_EXTRACTOR); }
    public boolean isInsertionMode() { return this.getSlot(1).getStack().isOf(Items.AMETHYST_SHARD); }

    public boolean isSlotUnlocked(int slotIndex) {
        ItemStack weapon = this.getSlot(0).getStack();
        ARPGEquipmentData data = weapon.get(TieredDataComponents.ARPG_DATA);
        return data != null && slotIndex < data.maxSlots();
    }

    private ScrollData getScrollAt(ARPGEquipmentData data, int index) {
        if (data.slots() != null && index >= 0 && index < data.slots().size()) {
            ScrollData scroll = data.slots().get(index);
            if (scroll != null && !scroll.attributeId().equals("empty")) return scroll;
        }
        return null;
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
        if (inventory == this.inventory) this.updateResult();
    }

    private void updateResult() {
        if (this.player.getEntityWorld().isClient()) return;

        // 🌟 1. O RADAR DE ESTANTES (Anel 5x5, 2 de altura)
        this.context.run((world, pos) -> {
            int count = 0;
            for (int x = -2; x <= 2; ++x) {
                for (int z = -2; z <= 2; ++z) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        for (int y = 0; y <= 1; ++y) {
                            // Usa a Tag nativa do Minecraft (suporta estantes de outros mods!)
                            if (world.getBlockState(pos.add(x, y, z)).isIn(net.minecraft.registry.tag.BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
                                count++;
                            }
                        }
                    }
                }
            }
            this.propertyDelegate.set(2, count); // Salva para o Cliente ler
        });

        // ... resto do código (boolean isReady = false; ...)

        boolean isReady = false;
        ItemStack equipment = this.getSlot(0).getStack();
        ItemStack actionItem = this.getSlot(1).getStack();
        ItemStack catalyst = this.getSlot(2).getStack();

        if (!equipment.isEmpty() && equipment.contains(TieredDataComponents.ARPG_DATA) && !actionItem.isEmpty() && catalyst.isOf(Items.END_CRYSTAL)) {
            ARPGEquipmentData data = equipment.get(TieredDataComponents.ARPG_DATA);

            if (isPiercingMode()) {
                int limit = data.prestige() >= 3 ? 3 : 2;
                if (data.maxSlots() < limit && (this.player.isCreative() || this.player.totalExperience >= 500)) {
                    isReady = true;
                }
            } else if (isInsertionMode()) {
                int scrollCount = 0;
                for (int i = 0; i < 3; i++) {
                    if (isSlotUnlocked(i) && this.getSlot(3 + i).hasStack()) scrollCount++;
                }
                if (scrollCount > 0 && (this.player.isCreative() || this.player.totalExperience >= (500 * scrollCount))) {
                    isReady = true;
                }
            } else if (isExtractionMode()) {
                if (selectedExtractionSlot >= 0 && selectedExtractionSlot < data.maxSlots()) {
                    if (getScrollAt(data, selectedExtractionSlot) != null && this.getSlot(6).getStack().isEmpty() && (this.player.isCreative() || this.player.totalExperience >= 500)) {
                        isReady = true;
                    }
                }
            } else {
                this.selectedExtractionSlot = -1; // Reseta se colocar outro item no meio
            }
        } else {
            this.selectedExtractionSlot = -1; // Reseta se tirar a arma, o extrator ou o cristal
        }

        this.propertyDelegate.set(0, isReady ? 1 : 0);
        this.propertyDelegate.set(1, selectedExtractionSlot);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (isExtractionMode() && id >= 0 && id < 3) {
            this.selectedExtractionSlot = id;
            this.updateResult();
            return true;
        }
        if (id == 100) {
            this.processMagic();
            return true;
        }
        return false;
    }

    public void processMagic() {
        if (this.propertyDelegate.get(0) == 0) return;

        ItemStack equipment = this.getSlot(0).getStack();
        ARPGEquipmentData data = equipment.get(TieredDataComponents.ARPG_DATA);
        if (data == null) return;

        // 🌟 O MOTOR DE RNG (50% Base + Sorte + 0.5% por Estante)
        int luck = (int) this.player.getAttributeValue(EntityAttributes.LUCK);
        int bookshelves = this.propertyDelegate.get(2);

        double chance = Math.max(0, Math.min(100, 50.0 + luck + (bookshelves * 0.5)));
        boolean success = this.player.getRandom().nextDouble() * 100.0 < chance;

        if (isPiercingMode()) {
            // 🌟 Trava de segurança do servidor
            int limit = data.prestige() >= 3 ? 3 : 2;
            if (data.maxSlots() >= limit) return;

            if (!this.player.isCreative()) this.player.addExperience(-500);
            this.decrementStack(1); // Consome Perfurador
            this.decrementStack(2); // Consome End Crystal

            if (success) {
                int newMaxSlots = data.maxSlots() + 1;
                ARPGEquipmentData newData = new ARPGEquipmentData(
                        data.level(), data.currentXp(), data.prestige(), data.affinity(),
                        data.trainingXp(), newMaxSlots, data.slots(), data.isBroken()
                );
                equipment.set(TieredDataComponents.ARPG_DATA, newData);
                playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE);
            } else {
                playSound(SoundEvents.BLOCK_ANVIL_DESTROY); // Falhou, mas a arma tá salva!
            }

        } else if (isInsertionMode()) {
            int scrollCount = 0;
            List<ScrollData> currentSlots = new ArrayList<>(data.slots());
            while (currentSlots.size() < data.maxSlots()) {
                currentSlots.add(new ScrollData("empty", "empty", 0));
            }

            for (int i = 0; i < 3; i++) {
                if (isSlotUnlocked(i) && this.getSlot(3 + i).hasStack()) scrollCount++;
            }

            if (!this.player.isCreative()) this.player.addExperience(-(500 * scrollCount));
            this.decrementStack(1); // Consome Ametista
            this.decrementStack(2); // Consome End Crystal

            if (success) {
                for (int i = 0; i < 3; i++) {
                    if (isSlotUnlocked(i) && this.getSlot(3 + i).hasStack()) {
                        ScrollData scrollData = this.getSlot(3 + i).getStack().get(TieredDataComponents.SCROLL_DATA);
                        if (scrollData != null) {
                            currentSlots.set(i, scrollData);
                            this.decrementStack(3 + i); // 🌟 SÓ CONSOME O PERGAMINHO SE DER SUCESSO!
                        }
                    }
                }
                ARPGEquipmentData newData = new ARPGEquipmentData(
                        data.level(), data.currentXp(), data.prestige(), data.affinity(),
                        data.trainingXp(), data.maxSlots(), currentSlots, data.isBroken()
                );
                equipment.set(TieredDataComponents.ARPG_DATA, newData);
                ScrollHelper.updateWeaponScrollAttributes(equipment);
                playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE);
            } else {
                playSound(SoundEvents.BLOCK_ANVIL_DESTROY); // Falhou, pergaminho continua no slot!
            }

        } else if (isExtractionMode()) {
            if (!this.player.isCreative()) this.player.addExperience(-500);
            this.decrementStack(1); // Consome Extrator
            this.decrementStack(2); // Consome End Crystal

            if (success) {
                List<ScrollData> currentSlots = new ArrayList<>(data.slots());
                while (currentSlots.size() < data.maxSlots()) {
                    currentSlots.add(new ScrollData("empty", "empty", 0));
                }

                if (selectedExtractionSlot >= 0 && selectedExtractionSlot < currentSlots.size()) {
                    ScrollData extractedData = currentSlots.get(selectedExtractionSlot);

                    // 🌟 DEVOLVE O PERGAMINHO NO SLOT 6
                    ItemStack extractedScroll = new ItemStack(ItemsRegisters.ATTRIBUTE_SCROLL);
                    extractedScroll.set(TieredDataComponents.SCROLL_DATA, extractedData);
                    this.inventory.setStack(6, extractedScroll);

                    // Remove da arma
                    currentSlots.set(selectedExtractionSlot, new ScrollData("empty", "empty", 0));
                }

                ARPGEquipmentData newData = new ARPGEquipmentData(
                        data.level(), data.currentXp(), data.prestige(), data.affinity(),
                        data.trainingXp(), data.maxSlots(), currentSlots, data.isBroken()
                );
                equipment.set(TieredDataComponents.ARPG_DATA, newData);
                ScrollHelper.updateWeaponScrollAttributes(equipment);
                this.selectedExtractionSlot = -1;
                playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE);
            } else {
                playSound(SoundEvents.BLOCK_ANVIL_DESTROY); // Falhou, pergaminho continua na arma!
            }
        }

        this.inventory.setStack(0, equipment);
        this.updateResult();
    }

    private void playSound(net.minecraft.sound.SoundEvent sound) {
        this.context.run((world, pos) -> {
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0f, 1.0f);
        });
    }

    private void decrementStack(int slot) {
        ItemStack itemStack = this.inventory.getStack(slot);
        itemStack.decrement(1);
        this.inventory.setStack(slot, itemStack);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> this.dropInventory(player, this.inventory));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // 🌟 AGORA SÃO 7 SLOTS (0 a 6)
            if (index < 7) {
                if (!this.insertItem(originalStack, 7, 43, true)) return ItemStack.EMPTY;
            } else {
                if (originalStack.contains(TieredDataComponents.ARPG_DATA)) {
                    if (!this.insertItem(originalStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ItemsRegisters.MAGIC_PIERCER) || originalStack.isOf(ItemsRegisters.MAGIC_EXTRACTOR) || originalStack.isOf(Items.AMETHYST_SHARD)) {
                    if (!this.insertItem(originalStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(Items.END_CRYSTAL)) {
                    if (!this.insertItem(originalStack, 2, 3, false)) return ItemStack.EMPTY;
                } else if (originalStack.isOf(ItemsRegisters.ATTRIBUTE_SCROLL)) {

                    boolean inserted = false;
                    if (isInsertionMode()) {
                        ARPGEquipmentData data = this.getSlot(0).getStack().get(TieredDataComponents.ARPG_DATA);

                        for (int i = 0; i < 3; i++) {
                            if (isSlotUnlocked(i)) {
                                Slot targetSlot = this.slots.get(3 + i);
                                boolean hasAppliedScroll = false;
                                if (data != null && data.slots() != null && i < data.slots().size()) {
                                    ScrollData s = data.slots().get(i);
                                    if (s != null && !s.attributeId().equals("empty")) hasAppliedScroll = true;
                                }

                                if (!targetSlot.hasStack() && !hasAppliedScroll) {
                                    targetSlot.setStack(originalStack.split(1));
                                    targetSlot.markDirty();
                                    inserted = true;
                                    break;
                                }
                            }
                        }

                        if (!inserted) {
                            for (int i = 0; i < 3; i++) {
                                if (isSlotUnlocked(i)) {
                                    Slot targetSlot = this.slots.get(3 + i);
                                    if (!targetSlot.hasStack()) {
                                        targetSlot.setStack(originalStack.split(1));
                                        targetSlot.markDirty();
                                        inserted = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (!inserted) return ItemStack.EMPTY;

                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }

    public boolean isReady() { return this.propertyDelegate.get(0) == 1; }
    public int getSelectedExtractionSlot() { return this.propertyDelegate.get(1); }
    @Override public boolean canUse(PlayerEntity player) { return true; }
}