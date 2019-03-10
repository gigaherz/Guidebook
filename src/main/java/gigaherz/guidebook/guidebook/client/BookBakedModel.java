package gigaherz.guidebook.guidebook.client;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gigaherz.common.client.ModelHandle;
import gigaherz.guidebook.GuidebookMod;
import gigaherz.guidebook.guidebook.BookDocument;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.SelectiveReloadStateHandler;
import net.minecraftforge.common.model.IModelState;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class BookBakedModel implements IBakedModel
{
    private static final ResourceLocation FAKE_LOCATION = GuidebookMod.location("models/block/custom/book");
    private static final ResourceLocation BASE_MODEL = GuidebookMod.location("block/book.obj");

    private final TextureAtlasSprite particle;
    private final IModelState state;

    public BookBakedModel(IModelState state, @Nullable TextureAtlasSprite particle)
    {
        this.state = state;
        this.particle = particle;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand)
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
        return new ItemOverrideList(Collections.emptyList())
        {
            @Override
            public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, World world, EntityLivingBase entity)
            {
                ModelHandle mh = ModelHandle.of(BASE_MODEL);

                if (state != null)
                {
                    mh = mh.state(state);
                }

                NBTTagCompound tag = stack.getTagCompound();
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
        };
    }

    private static class Model implements IModel
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
        public Collection<ResourceLocation> getTextures()
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

        @Override
        public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
        {
            TextureAtlasSprite part = null;
            if (particle != null) part = bakedTextureGetter.apply(particle);
            return new BookBakedModel(state, part);
        }

        @Override
        public IModelState getDefaultState()
        {
            return part -> Optional.empty();
        }

        @Override
        public IModel retexture(ImmutableMap<String, String> textures)
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
        public IModel loadModel(ResourceLocation modelLocation) throws Exception
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
