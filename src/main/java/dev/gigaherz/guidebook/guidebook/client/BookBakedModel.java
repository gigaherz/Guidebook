package dev.gigaherz.guidebook.guidebook.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class BookBakedModel implements BakedModel
{
    private final boolean isSideLit;
    private final ItemTransforms cameraTransforms;
    private final TextureAtlasSprite particle;
    private final ItemOverrides overrideList;

    public BookBakedModel(BakedModel baseModel, ModelBakery bakery, UnbakedModel unbakedModel, Function<ResourceLocation, UnbakedModel> modelGetter,
                          Function<Material, TextureAtlasSprite> spriteGetter, boolean isSideLit, ItemTransforms cameraTransforms,
                          Map<ResourceLocation, BakedModel> bookModels, Map<ResourceLocation, BakedModel> coverModels, @Nullable TextureAtlasSprite particle, ItemOverrides originalOverrides)
    {
        this.particle = particle;
        this.isSideLit = isSideLit;
        this.cameraTransforms = cameraTransforms;
        this.overrideList = new ItemOverrides(bakery, unbakedModel, modelGetter, spriteGetter, Collections.emptyList())
        {
            @Nullable
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel worldIn, @Nullable LivingEntity entityIn, int p_173469_)
            {
                CompoundTag tag = stack.getTag();
                if (tag != null)
                {
                    String book = tag.getString("Book");
                    BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
                    if (bookDocument != null)
                    {
                        ResourceLocation modelLocation = bookDocument.getModel();
                        if (modelLocation != null)
                        {
                            BakedModel bakedModel = bookModels.get(modelLocation);
                            return bakedModel.getOverrides().resolve(bakedModel, stack, worldIn, entityIn, p_173469_);
                        }
                        else
                        {
                            ResourceLocation cover = bookDocument.getCover();

                            if (cover != null)
                            {
                                BakedModel bakedModel = coverModels.get(cover);
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

    public static class Model implements IModelGeometry<Model>
    {
        private final BlockModel baseModel;
        private final Map<ResourceLocation, UnbakedModel> bookModels = Maps.newHashMap();
        private final Map<ResourceLocation, UnbakedModel> coverModels = Maps.newHashMap();

        public Model(BlockModel baseModel)
        {
            this.baseModel = baseModel;
        }

        @Override
        public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation)
        {
            Material particleLocation = owner.resolveTexture("particle");
            TextureAtlasSprite part = spriteGetter.apply(particleLocation);

            Map<ResourceLocation, BakedModel> bakedBookModels = ImmutableMap.copyOf(Maps.transformEntries(bookModels, (k, v) -> v.bake(bakery, spriteGetter, modelTransform, k)));
            Map<ResourceLocation, BakedModel> bakedCoverModels = ImmutableMap.copyOf(Maps.transformEntries(coverModels, (k, v) -> v.bake(bakery, spriteGetter, modelTransform, k)));

            return new BookBakedModel(
                    baseModel.bake(bakery, baseModel, spriteGetter, modelTransform, modelLocation, true),
                    bakery, owner.getOwnerModel(), bakery::getModel, spriteGetter, owner.isSideLit(), owner.getCameraTransforms(), bakedBookModels, bakedCoverModels, part, overrides);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            Set<Material> textures = Sets.newHashSet();

            textures.addAll(baseModel.getMaterials(modelGetter, missingTextureErrors));

            for (ResourceLocation bookModel : BookRegistry.gatherBookModels())
            {
                bookModels.computeIfAbsent(bookModel, (loc) -> {
                    UnbakedModel mdl = modelGetter.apply(loc);
                    textures.addAll(mdl.getMaterials(modelGetter, missingTextureErrors));
                    return mdl;
                });
            }

            for (ResourceLocation bookCover : BookRegistry.gatherBookCovers())
            {
                coverModels.computeIfAbsent(bookCover, (loc) -> {
                    BlockModel mdl = new BlockModel(
                            new ResourceLocation(bookCover.getNamespace(), "generated/cover_models/" + bookCover.getPath()),
                            Collections.emptyList(),
                            ImmutableMap.of("cover", Either.<Material, String>left(new Material(TextureAtlas.LOCATION_BLOCKS, bookCover))),
                            true, owner.isSideLit() ? BlockModel.GuiLight.SIDE : BlockModel.GuiLight.FRONT, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
                    mdl.parent = baseModel;
                    textures.addAll(mdl.getMaterials(modelGetter, missingTextureErrors));
                    return mdl;
                });
            }

            for (BookDocument renderer : BookRegistry.getLoadedBooks().values())
            {
                renderer.findTextures(textures);
            }

            return textures;
        }
    }

    public static class ModelLoader implements IModelLoader<Model>
    {
        @Override
        public void onResourceManagerReload(ResourceManager resourceManager)
        {
            BookRegistry.parseAllBooks(resourceManager);
        }

        @Override
        public Model read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
        {
            BlockModel baseModel = deserializationContext.deserialize(GsonHelper.getAsJsonObject(modelContents, "base_model"), BlockModel.class);
            return new Model(baseModel);
        }
    }
}
