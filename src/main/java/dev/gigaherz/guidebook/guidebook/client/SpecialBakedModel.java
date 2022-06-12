package dev.gigaherz.guidebook.guidebook.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.gigaherz.guidebook.guidebook.BookRegistry;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.geometry.IModelGeometry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class SpecialBakedModel implements BakedModel
{
    private final boolean isSideLit;
    private final ItemTransforms cameraTransforms;
    private final TextureAtlasSprite particle;
    private final ItemOverrides overrideList;

    public SpecialBakedModel(ModelBakery bakery, UnbakedModel unbakedModel, Function<ResourceLocation, UnbakedModel> modelGetter,
                             Function<Material, TextureAtlasSprite> spriteGetter, boolean isSideLit, ItemTransforms cameraTransforms,
                             @Nullable TextureAtlasSprite particle)
    {
        this.particle = particle;
        this.isSideLit = isSideLit;
        this.cameraTransforms = cameraTransforms;
        this.overrideList = new ItemOverrides(bakery, unbakedModel, modelGetter, spriteGetter, Collections.emptyList());
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
        return true;
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

    public static ItemTransforms.TransformType cameraTransformType;

    @Override
    public BakedModel handlePerspective(ItemTransforms.TransformType cameraTransformType, PoseStack mat)
    {
        SpecialBakedModel.cameraTransformType = cameraTransformType;
        return this;
    }

    public static class Model implements IModelGeometry<Model>
    {
        @Override
        public BakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation)
        {
            Material particleLocation = owner.resolveTexture("particle");
            TextureAtlasSprite part = spriteGetter.apply(particleLocation);

            return new SpecialBakedModel(
                    bakery, owner.getOwnerModel(), bakery::getModel, spriteGetter, owner.isSideLit(), owner.getCameraTransforms(), part);
        }

        @Override
        public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors)
        {
            return Collections.emptyList();
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
            return new Model();
        }
    }
}
