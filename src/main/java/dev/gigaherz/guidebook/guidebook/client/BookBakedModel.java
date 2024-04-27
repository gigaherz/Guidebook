package dev.gigaherz.guidebook.guidebook.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import dev.gigaherz.guidebook.GuidebookMod;
import dev.gigaherz.guidebook.guidebook.BookDocument;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BookBakedModel implements BakedModel
{
    private final boolean isSideLit;
    private final ItemTransforms cameraTransforms;
    private final TextureAtlasSprite particle;
    private final ItemOverrides overrideList;

    public BookBakedModel(BakedModel baseModel, ModelBaker bakery, Function<ResourceLocation, UnbakedModel> modelGetter,
                          Function<Material, TextureAtlasSprite> spriteGetter, boolean isSideLit, ItemTransforms cameraTransforms,
                          Map<ResourceLocation, BakedModel> bookModels, Map<ResourceLocation, BakedModel> coverModels, @Nullable TextureAtlasSprite particle, ItemOverrides originalOverrides)
    {
        this.particle = particle;
        this.isSideLit = isSideLit;
        this.cameraTransforms = cameraTransforms;
        this.overrideList = new ItemOverrides()
        {
            @Nullable
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel worldIn, @Nullable LivingEntity entityIn, int p_173469_)
            {
                ResourceLocation book = stack.get(GuidebookMod.BOOK_ID);
                if (book != null)
                {
                    BookDocument bookDocument = BookRegistry.get(book);
                    if (bookDocument != null)
                    {
                        ResourceLocation modelLocation = bookDocument.getModel();
                        if (modelLocation != null)
                        {
                            BakedModel bakedModel = bookModels.get(modelLocation);
                            if (bakedModel != null)
                                return bakedModel.getOverrides().resolve(bakedModel, stack, worldIn, entityIn, p_173469_);
                        }
                        else
                        {
                            ResourceLocation cover = bookDocument.getCover();

                            if (cover != null)
                            {
                                BakedModel bakedModel = coverModels.get(cover);
                                if (bakedModel != null)
                                    return bakedModel.getOverrides().resolve(bakedModel, stack, worldIn, entityIn, p_173469_);
                            }
                        }
                    }
                }

                var fallbackModel =  baseModel.getOverrides().resolve(baseModel, stack, worldIn, entityIn, p_173469_);
                return originalOverrides.resolve(fallbackModel, stack, worldIn, entityIn, p_173469_);
            }
        };
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
    public boolean isCustomRenderer()
    {
        return false;
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

    @Override
    public ItemOverrides getOverrides()
    {
        return overrideList;
    }

    public static class Model implements IUnbakedGeometry<Model>
    {
        private final BlockModel baseModel;
        private final Map<ResourceLocation, UnbakedModel> bookModels = Maps.newHashMap();
        private final Map<ResourceLocation, UnbakedModel> coverModels = Maps.newHashMap();

        public Model(BlockModel baseModel)
        {
            this.baseModel = baseModel;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation)
        {
            Material particleLocation = context.getMaterial("particle");
            TextureAtlasSprite part = spriteGetter.apply(particleLocation);

            Map<ResourceLocation, BakedModel> bakedBookModels = ImmutableMap.copyOf(Maps.transformEntries(bookModels, (k, v) -> v.bake(bakery, spriteGetter, modelTransform, k)));
            Map<ResourceLocation, BakedModel> bakedCoverModels = ImmutableMap.copyOf(Maps.transformEntries(coverModels, (k, v) -> v.bake(bakery, spriteGetter, modelTransform, k)));

            return new BookBakedModel(
                    baseModel.bake(bakery, baseModel, spriteGetter, modelTransform, modelLocation, true),
                    bakery, bakery::getModel, spriteGetter, context.useBlockLight(), context.getTransforms(), bakedBookModels, bakedCoverModels, part, overrides);
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context)
        {
            for (ResourceLocation bookModel : BookRegistry.gatherBookModels())
            {
                bookModels.computeIfAbsent(bookModel, modelGetter);
            }

            for (ResourceLocation bookCover : BookRegistry.gatherBookCovers())
            {
                coverModels.computeIfAbsent(bookCover, (loc) -> {
                    BlockModel mdl = new BlockModel(
                            new ResourceLocation(bookCover.getNamespace(), "generated/cover_models/" + bookCover.getPath()),
                            Collections.emptyList(),
                            ImmutableMap.of("cover", Either.left(new Material(TextureAtlas.LOCATION_BLOCKS, bookCover))),
                            true, context.useBlockLight() ? BlockModel.GuiLight.SIDE : BlockModel.GuiLight.FRONT, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
                    mdl.parent = baseModel;
                    mdl.resolveParents(modelGetter);
                    return mdl;
                });
            }
        }
    }

    public static class ModelLoader implements IGeometryLoader<Model>
    {
        @Override
        public Model read(JsonObject modelContents, JsonDeserializationContext deserializationContext)
        {
            BlockModel baseModel = deserializationContext.deserialize(GsonHelper.getAsJsonObject(modelContents, "base_model"), BlockModel.class);
            return new Model(baseModel);
        }
    }
}
