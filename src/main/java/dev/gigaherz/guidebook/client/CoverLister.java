package dev.gigaherz.guidebook.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public record CoverLister(Optional<String> idPrefix) implements SpriteSource {

    private static final MapCodec<CoverLister> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.optionalFieldOf("prefix").forGetter(lister -> lister.idPrefix)).apply(inst, CoverLister::new));
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(GuidebookMod.MODID, "covers");
    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        FileToIdConverter converter = new FileToIdConverter("textures", ".png");
        BookRegistry.gatherBookCovers().forEach((cover) -> {
            ResourceLocation id = this.idPrefix.isPresent() ? cover.withPrefix(this.idPrefix.get()) : cover;
            Optional<Resource> resource = resourceManager.getResource(converter.idToFile(cover));
            if (resource.isPresent())
                output.add(id, resource.get());
        });
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }
}
