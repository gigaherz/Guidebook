package dev.gigaherz.guidebook.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
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
    public void onResourceManagerReload(ResourceManager resourceManager)
    {
        // No need to do anything for this loader.
    }

    @Override
    public Model read(JsonDeserializationContext deserializationContext, JsonObject modelContents)
    {
        BlockModel model = deserializationContext.deserialize(GsonHelper.getAsJsonObject(modelContents, "model"), BlockModel.class);
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
        public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation)
        {
            BakedModel childModel = model.bake(bakery, model, spriteGetter, modelTransform, modelLocation, model.getGuiLight().lightLikeBlock());
            return new Baked(childModel, overrides);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            return model.getMaterials(modelGetter, missingTextureErrors);
        }
    }

    public class Baked implements IDynamicBakedModel
    {
        private final Map<BakedQuad, BakedQuad> processedQuadCache = Maps.newHashMap();
        private final BakedModel childModel;
        private final ItemOverrides overrides;

        public Baked(BakedModel childModel, ItemOverrides overrides)
        {
            this.childModel = childModel;
            this.overrides = overrides;
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull IModelData extraData)
        {
            List<BakedQuad> quads = childModel.getQuads(state, side, rand, extraData);

            List<BakedQuad> newQuads = Lists.newArrayList();
            for (BakedQuad quad : quads)
            {
                newQuads.add(processedQuadCache.computeIfAbsent(quad, transformer));
            }
            return newQuads;
        }

        @Override
        public boolean useAmbientOcclusion()
        {
            return childModel.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d()
        {
            return childModel.isGui3d();
        }

        @Override
        public boolean usesBlockLight()
        {
            return childModel.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer()
        {
            return childModel.isCustomRenderer();
        }

        @Override
        public TextureAtlasSprite getParticleIcon()
        {
            return childModel.getParticleIcon();
        }

        @Override
        public ItemOverrides getOverrides()
        {
            return overrides;
        }
    }
}
