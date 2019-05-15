package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.SelectiveReloadStateHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class BookBakedModel implements IBakedModel
{
    private static final ResourceLocation FAKE_LOCATION = GuidebookMod.location("models/block/custom/book");
    private static final ResourceLocation BASE_MODEL = GuidebookMod.location("block/book.obj");

    private final TextureAtlasSprite particle;
    private final ItemOverrideList overrideList;

    public BookBakedModel(IUnbakedModel unbakedModel, Function<ResourceLocation, IUnbakedModel> modelGetter,
                          Function<ResourceLocation, TextureAtlasSprite> spriteGetter,
                          IModelState state,
                          VertexFormat format,
                          @Nullable TextureAtlasSprite particle)
    {
        this.particle = particle;

        /*int number = 0;
        Map<ResourceLocation, Float> propertyMap = Maps.newHashMap();
        Map<ResourceLocation, ModelResourceLocation> modelMap = Maps.newHashMap();
        List<ItemOverride>
        for(Map.Entry<ResourceLocation, BookDocument> entry : BookRegistry.getLoadedBooks().entrySet())
        {
            BookDocument book = entry.getValue();
            ModelResourceLocation mrl = book == null ? null : book.getModel();
            if (mrl != null)
            {
                modelMap.put(entry.getKey(), mrl);
            }

            ItemOverride io = new ItemOverride()
        }*/

        this.overrideList = new ItemOverrideList(unbakedModel, modelGetter, spriteGetter, Collections.emptyList());
        // TODO: Custom cover images without fully custom model
        /*{
            @Nullable
            @Override
            public IBakedModel getModelWithOverrides(IBakedModel model, ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                return super.getModelWithOverrides(model, stack, worldIn, entityIn);
            }

            //TODO: @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity)
            {
                ModelHandle mh = ModelHandle.of(BASE_MODEL);

                if (state != null)
                {
                    mh = mh.state(state);
                }

                NBTTagCompound tag = stack.getTag();
                if (tag != null)
                {
                    String book = tag.getString("Book");
                    BookDocument bookDocument = BookRegistry.get(new ResourceLocation(book));
                    if (bookDocument != null)
                    {
                        ResourceLocation cover = bookDocument.getCover();
                        if (cover != null)
                            mh = mh.replace("#CoverGraphics", cover.toString());
                    }
                }

                return mh.get();
            }
        };*/
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, Random rand)
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

    private static class Model implements IUnbakedModel
    {
        @Nullable
        private final ResourceLocation particle;

        Model()
        {
            this.particle = null;
        }

        Model(@Nullable String particle)
        {
            this.particle = particle == null ? null : new ResourceLocation(particle);
        }

        @Override
        public Collection<ResourceLocation> getDependencies()
        {
            List<ResourceLocation> dependencies = Lists.newArrayList();
            dependencies.add(BASE_MODEL);
            return dependencies;
        }

        @Override
        public Collection<ResourceLocation> getTextures(Function<ResourceLocation, IUnbakedModel> modelGetter, Set<String> missingTextureErrors)
        {
            Set<ResourceLocation> textures = Sets.newHashSet();
            if (particle != null)
                textures.add(particle);

            for (BookDocument renderer : BookRegistry.getLoadedBooks().values())
            {
                renderer.findTextures(textures);
            }

            return textures;
        }

        @Nullable
        @Override
        public IBakedModel bake(Function<ResourceLocation, IUnbakedModel> modelGetter, Function<ResourceLocation, TextureAtlasSprite> spriteGetter, IModelState state, boolean uvlock, VertexFormat format)
        {
            TextureAtlasSprite part = null;
            if (particle != null) part = spriteGetter.apply(particle);
            return new BookBakedModel(this, modelGetter, spriteGetter, state, format, part);
        }

        @Override
        public IModelState getDefaultState()
        {
            return part -> Optional.empty();
        }

        @Override
        public IUnbakedModel retexture(ImmutableMap<String, String> textures)
        {
            return new Model(textures.get("particle"));
        }
    }

    public static class ModelLoader implements ICustomModelLoader
    {
        @Override
        public boolean accepts(ResourceLocation modelLocation)
        {
            return FAKE_LOCATION.equals(modelLocation);
        }

        @Override
        public IUnbakedModel loadModel(ResourceLocation modelLocation) throws Exception
        {
            return new Model();
        }

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
    }
}
