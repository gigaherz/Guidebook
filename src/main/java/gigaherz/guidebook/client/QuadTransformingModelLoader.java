package gigaherz.guidebook.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class QuadTransformingModelLoader implements IModelLoader<QuadTransformingModelLoader.Model>
{
    private final Function<BakedQuad, BakedQuad> transformer;

    public QuadTransformingModelLoader(Function<BakedQuad, BakedQuad> transformer)
    {
        this.transformer = transformer;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager)
    {
        // No need to do anything for this loader.
    }

    @Override
    public Model read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
    {
        BlockModel model = deserializationContext.deserialize(JSONUtils.getJsonObject(modelContents, "model"), BlockModel.class);
        return new Model(model);
    }

    public class Model implements IModelGeometry<Model>
    {
        private final BlockModel model;

        public Model(BlockModel model)
        {
            this.model = model;
        }

        @Override
        public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation)
        {
            IBakedModel childModel = model.bakeModel(bakery, model, spriteGetter, modelTransform, modelLocation, model.func_230176_c_().func_230178_a_());
            return new BakedModel(childModel, overrides);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            return model.getTextures(modelGetter, missingTextureErrors);
        }
    }

    public class BakedModel implements IDynamicBakedModel
    {
        private final Map<BakedQuad, BakedQuad> processedQuadCache = Maps.newHashMap();
        private final IBakedModel childModel;
        private final ItemOverrideList overrides;

        public BakedModel(IBakedModel childModel, ItemOverrideList overrides)
        {
            this.childModel = childModel;
            this.overrides = overrides;
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData)
        {
            List<BakedQuad> quads = childModel.getQuads(state, side, rand, extraData);

            List<BakedQuad> newQuads = Lists.newArrayList();
            for(BakedQuad quad : quads)
            {
                newQuads.add(processedQuadCache.computeIfAbsent(quad, transformer));
            }
            return newQuads;
        }

        @Override
        public boolean isAmbientOcclusion()
        {
            return childModel.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d()
        {
            return childModel.isGui3d();
        }

        @Override
        public boolean func_230044_c_()
        {
            return childModel.func_230044_c_();
        }

        @Override
        public boolean isBuiltInRenderer()
        {
            return childModel.isBuiltInRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleTexture()
        {
            return childModel.getParticleTexture();
        }

        @Override
        public ItemOverrideList getOverrides()
        {
            return overrides;
        }
    }
}
