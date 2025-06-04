package dev.gigaherz.guidebook.guidebook.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BookBakedModel implements BakedModel
{
    private final boolean isSideLit;
    private final ItemTransforms cameraTransforms;
    private final BakedModel baseModel;
    private final Map<ResourceLocation, BakedModel> bookModels;
    private final Map<ResourceLocation, BakedModel> coverModels;
    private final TextureAtlasSprite particle;

    public BakedModel getActualModel(@Nullable ResourceLocation book)
    {
        if (book != null)
        {
            BookDocument bookDocument = BookRegistry.get(book);
            if (bookDocument != null)
            {
                var standaloneModel = bookDocument.getModelStandalone();
                var cover = bookDocument.getCover();
                if (standaloneModel != null)
                {
                    BakedModel bakedModel = bookModels.get(standaloneModel);
                    if (bakedModel != null)
                        return bakedModel;
                }
                else if (cover != null)
                {
                    BakedModel bakedModel = coverModels.get(cover);
                    if (bakedModel != null)
                        return bakedModel;
                }
            }
        }
        return baseModel;
    }

    public BookBakedModel(BakedModel baseModel,
                          boolean isSideLit, ItemTransforms cameraTransforms,
                          Map<ResourceLocation, BakedModel> standaloneModels,
                          Map<ResourceLocation, BakedModel> coverModels, @Nullable TextureAtlasSprite particle)
    {
        this.baseModel = baseModel;
        this.bookModels = standaloneModels;
        this.coverModels = coverModels;
        this.particle = particle;
        this.isSideLit = isSideLit;
        this.cameraTransforms = cameraTransforms;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion()
    {
        return true;
    }

    @Override
    public boolean isGui3d()
    {
        return true;
    }

    @Override
    public boolean usesBlockLight()
    {
        return isSideLit;
    }

    @Override
    public TextureAtlasSprite getParticleIcon()
    {
        return particle;
    }

    @Deprecated
    @Override
    public ItemTransforms getTransforms()
    {
        return cameraTransforms;
    }

    public static class Model implements UnbakedModel
    {
        private final UnbakedModel baseModel;
        private final Map<ResourceLocation, UnbakedModel> bookModels = Maps.newHashMap();
        private final Map<ResourceLocation, UnbakedModel> coverModels = Maps.newHashMap();

        public Model(UnbakedModel baseModel)
        {
            this.baseModel = baseModel;
        }

        @Override
        public BakedModel bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, boolean useAmbientOcclusion, boolean usesBlockLight, ItemTransforms itemTransforms, ContextMap additionalProperties)
        {
            var part = baker.findSprite(textureSlots, TextureSlot.PARTICLE.getId());

            Map<ResourceLocation, BakedModel> bakedBookModels = ImmutableMap.copyOf(Maps.transformValues(bookModels,
                    v -> UnbakedModel.bakeWithTopModelValues(v, baker, modelState)));
            Map<ResourceLocation, BakedModel> bakedCoverModels = ImmutableMap.copyOf(Maps.transformValues(coverModels,
                    v -> UnbakedModel.bakeWithTopModelValues(v, baker, modelState)));

            var baseModel = UnbakedModel.bakeWithTopModelValues(this.baseModel, baker, modelState);

            return new BookBakedModel(
                    baseModel,
                    usesBlockLight, itemTransforms, bakedBookModels, bakedCoverModels, part);
        }

        @Override
        public BakedModel bake(TextureSlots textureSlots, ModelBaker baker, ModelState modelState, boolean useAmbientOcclusion, boolean usesBlockLight, ItemTransforms itemTransforms)
        {
            return this.bake(textureSlots, baker, modelState, useAmbientOcclusion, usesBlockLight, itemTransforms, ContextMap.EMPTY);
        }

        @Override
        public void resolveDependencies(Resolver resolver)
        {
            baseModel.resolveDependencies(resolver);

            BookRegistry.gatherStandaloneBookModels().forEach((bookModel) -> {
                bookModels.computeIfAbsent(bookModel, resolver::resolve);
            });

            BookRegistry.gatherBookCovers().forEach((bookCover) -> {
                coverModels.computeIfAbsent(bookCover, (loc) -> {
                    BlockModel mdl = new BlockModel(
                            ResourceLocation.fromNamespaceAndPath(bookCover.getNamespace(), "generated/cover_models/" + bookCover.getPath()),
                            List.of(),
                            new TextureSlots.Data.Builder().addTexture("cover", new Material(TextureAtlas.LOCATION_BLOCKS, bookCover)).build(),
                            null, null, null);
                    //mdl.parentLocation = null;
                    mdl.parent = baseModel;
                    //mdl.resolveDependencies(resolver);
                    return mdl;
                });
            });
        }
    }

    public static class ModelLoader implements UnbakedModelLoader<Model>
    {
        @Override
        public Model read(JsonObject modelContents, JsonDeserializationContext deserializationContext)
        {
            var baseModel = readBaseModel(GsonHelper.getAsJsonObject(modelContents,"base_model"), deserializationContext);
            return new Model(baseModel);
        }

        private static UnbakedModel readBaseModel(
                JsonObject childrenJsonObject,
                JsonDeserializationContext context) {
            return context.deserialize(childrenJsonObject, UnbakedModel.class);
        }
    }
}
