package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.*;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.SelectiveReloadStateHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class BookBakedModel implements IBakedModel
{
    public static final ResourceLocation BASE_MODEL = GuidebookMod.location("block/book.obj");

    private final TextureAtlasSprite particle;
    private final ItemOverrideList overrideList;

    public BookBakedModel(ModelBakery bakery, IUnbakedModel unbakedModel, Function<ResourceLocation, IUnbakedModel> modelGetter,
                          Function<ResourceLocation, TextureAtlasSprite> spriteGetter,
                          @Nullable IModelState state,
                          VertexFormat format,
                          @Nullable TextureAtlasSprite particle)
    {
        this.particle = particle;
        this.overrideList = new ItemOverrideList(bakery, unbakedModel, modelGetter, spriteGetter, Collections.emptyList(), format)
        {
            @Nullable
            @Override
            public IBakedModel getModelWithOverrides(IBakedModel model, ItemStack stack, @Nullable World worldIn, @Nullable LivingEntity entityIn)
            {
                ModelHandle mh = ModelHandle.of(BASE_MODEL);

                if (state != null)
                {
                    mh = mh.state(state);
                }

                CompoundNBT tag = stack.getTag();
                if (tag != null)
                {
                    String book = tag.getString("Book");
                    BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
                    if (bookDocument != null)
                    {
                        ModelResourceLocation _model = bookDocument.getModel();
                        if (_model != null)
                        {
                            mh = ModelHandle.of(_model);
                            if (state != null)
                            {
                                mh = mh.state(state);
                            }
                        }
                        else
                        {
                            ResourceLocation cover = bookDocument.getCover();
                            if (cover != null)
                                mh = mh.replace("#CoverGraphics", cover.toString());
                        }
                    }
                }

                return mh.get();
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
        public Collection<ResourceLocation> getDependencies()
        {
            Set<ResourceLocation> dependencies = Sets.newHashSet();
            dependencies.add(BASE_MODEL);
            Collections.addAll(dependencies, BookRegistry.gatherBookModels());
            return dependencies;
        }

        @Override
        public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<ResourceLocation, TextureAtlasSprite> spriteGetter, ISprite sprite, VertexFormat format, ItemOverrideList overrides)
        {
            String particleLocation = owner.resolveTexture("particle");
            TextureAtlasSprite part = spriteGetter.apply(new ResourceLocation(particleLocation));
            return new BookBakedModel(bakery, (IUnbakedModel)owner, bakery::getUnbakedModel, spriteGetter, sprite.getState(), format, part);
        }

        @Override
        public Collection<ResourceLocation> getTextureDependencies(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors)
        {
            Set<ResourceLocation> textures = Sets.newHashSet();

            for (ResourceLocation loc : getDependencies())
            {
                IUnbakedModel mdl = ModelLoaderRegistry.getModelOrMissing(loc);
                textures.addAll(mdl.getTextures(modelGetter, missingTextureErrors));
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
