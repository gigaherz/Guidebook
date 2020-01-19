package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.*;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.SelectiveReloadStateHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class BookBakedModel implements IBakedModel
{
    private final Map<ResourceLocation, IBakedModel> bookModels;
    private final Map<ResourceLocation, IBakedModel> coverModels;
    private final TextureAtlasSprite particle;
    private final ItemOverrideList overrideList;

    public BookBakedModel(ModelBakery bakery, IUnbakedModel unbakedModel, Function<ResourceLocation, IUnbakedModel> modelGetter,
                          Function<Material, TextureAtlasSprite> spriteGetter,
                          Map<ResourceLocation, IBakedModel> bookModels, Map<ResourceLocation, IBakedModel> coverModels, @Nullable TextureAtlasSprite particle)
    {
        this.bookModels = bookModels;
        this.coverModels = coverModels;
        this.particle = particle;
        this.overrideList = new ItemOverrideList(bakery, unbakedModel, modelGetter, spriteGetter, Collections.emptyList())
        {
            @Nullable
            @Override
            public IBakedModel getModelWithOverrides(IBakedModel model, ItemStack stack, @Nullable World worldIn, @Nullable LivingEntity entityIn)
            {
                CompoundNBT tag = stack.getTag();
                if (tag != null)
                {
                    String book = tag.getString("Book");
                    BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
                    if (bookDocument != null)
                    {
                        ResourceLocation modelLocation = bookDocument.getModel();
                        if (modelLocation != null)
                        {
                            return bookModels.get(modelLocation);
                        }
                        else
                        {
                            ResourceLocation cover = bookDocument.getCover();

                            if (cover != null)
                                return coverModels.get(cover);
                        }
                    }
                }

                return model;
            }
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isAmbientOcclusion()
    {
        return true;
    }

    @Override
    public boolean isGui3d()
    {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer()
    {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture()
    {
        return particle;
    }

    @Deprecated
    @Override
    public ItemCameraTransforms getItemCameraTransforms()
    {
        return null;
    }

    @Override
    public ItemOverrideList getOverrides()
    {
        return overrideList;
    }

    public static class Model implements IModelGeometry<Model>
    {
        private final Map<ResourceLocation, IUnbakedModel> bookModels = Maps.newHashMap();
        private final Map<ResourceLocation, IUnbakedModel> coverModels = Maps.newHashMap();

        @Override
        public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation)
        {
            Material particleLocation = owner.resolveTexture("particle");
            TextureAtlasSprite part = spriteGetter.apply(particleLocation);

            Map<ResourceLocation, IBakedModel> bakedBookModels = Maps.transformEntries(bookModels, (k, v) -> v.bake(bakery, spriteGetter, modelTransform, k));
            Map<ResourceLocation, IBakedModel> bakedCoverModels = Maps.transformEntries(coverModels, (k, v) -> v.bake(bakery, spriteGetter, modelTransform, k));

            return new BookBakedModel(bakery, (IUnbakedModel)owner, bakery::getUnbakedModel, spriteGetter, bakedBookModels, bakedCoverModels, part);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            Set<Material> textures = Sets.newHashSet();

            for(ResourceLocation bookModel : BookRegistry.gatherBookModels())
            {
                bookModels.computeIfAbsent(bookModel, (loc) -> {
                    IUnbakedModel mdl = modelGetter.apply(loc);
                    textures.addAll(mdl.getTextureDependencies(modelGetter, missingTextureErrors));
                    return mdl;
                });
            }

            for(ResourceLocation bookCover : BookRegistry.gatherBookCovers())
            {
                bookModels.computeIfAbsent(bookCover, (loc) -> {
                    BlockModel mdl = new BlockModel(
                            new ResourceLocation(bookCover.getNamespace(), "generated/cover_models/" + bookCover.getPath()),
                            Collections.emptyList(),
                            ImmutableMap.of("cover", Either.<Material,String>left(new Material(AtlasTexture.LOCATION_BLOCKS_TEXTURE, bookCover))),
                            true, true, ItemCameraTransforms.DEFAULT, Collections.emptyList());
                    mdl.parent = owner.getOwnerModel() instanceof BlockModel ? (BlockModel)owner.getOwnerModel() : null;
                    textures.addAll(mdl.getTextureDependencies(modelGetter, missingTextureErrors));
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
        public void onResourceManagerReload(IResourceManager resourceManager)
        {
            // For compatibility, call the selective version from the non-selective function
            onResourceManagerReload(resourceManager, SelectiveReloadStateHandler.INSTANCE.get());
        }

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate)
        {
            if (resourcePredicate.test(BookResourceType.INSTANCE))
                BookRegistry.parseAllBooks(resourceManager);
        }

        @Override
        public Model read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
        {
            return new Model();
        }
    }
}
