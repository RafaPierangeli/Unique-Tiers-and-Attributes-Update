package draylar.tiered.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import draylar.tiered.Tiered;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;

public class ItemVerifier {

    // 🌟 O CODEC: Lê os campos "id" ou "tag" do JSON de forma opcional
    public static final Codec<ItemVerifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id").forGetter(v -> Optional.ofNullable(v.id)),
            Identifier.CODEC.optionalFieldOf("tag").forGetter(v -> Optional.ofNullable(v.tag))
    ).apply(instance, (idOpt, tagOpt) -> new ItemVerifier(idOpt.orElse(null), tagOpt.orElse(null))));

    // Agora usamos Identifier nativo em vez de String
    private final Identifier id;
    private final Identifier tag;

    public ItemVerifier(Identifier id, Identifier tag) {
        this.id = id;
        this.tag = tag;
    }

    // Mantido para retrocompatibilidade com outras partes do seu mod
    public ItemVerifier(String id, String tag) {
        this.id = id != null ? Identifier.of(id) : null;
        this.tag = tag != null ? Identifier.of(tag) : null;
    }

    /**
     * Verifica se o Identifier fornecido é válido para este verificador.
     */
    public boolean isValid(Identifier itemID) {
        if (this.id != null) {
            // Checagem direta por ID (ex: minecraft:diamond_sword)
            return this.id.equals(itemID);
        } else if (this.tag != null) {
            // Checagem segura por Tag (ex: #minecraft:swords)
            TagKey<Item> itemTag = TagKey.of(RegistryKeys.ITEM, this.tag);

            // Na 1.21.11, pegamos o Entry opcional do registro para evitar NullPointerExceptions
            var entry = Registries.ITEM.getEntry(itemID);
            return entry.isPresent() && entry.get().isIn(itemTag);
        }

        return false;
    }

    public boolean isValid(String itemID) {
        return isValid(Identifier.of(itemID));
    }

    public String getId() {
        return id != null ? id.toString() : null;
    }

    public TagKey<Item> getTagKey() {
        return tag != null ? TagKey.of(RegistryKeys.ITEM, tag) : null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemVerifier other)) return false;
        return Objects.equals(id, other.id) && Objects.equals(tag, other.tag);
    }
}