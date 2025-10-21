package com.setycz.chickens.client.render;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Simple wrapper around the baked chicken item model that swaps the override
 * handler for one that knows how to service custom chicken ids. All other
 * behaviour delegates straight through to the vanilla model so durability
 * bars, foil rendering, and perspective transforms remain untouched.
 */
final class ChickenItemOverridesModel implements BakedModel {
    private final BakedModel delegate;
    private final ItemOverrides overrides;

    ChickenItemOverridesModel(BakedModel delegate, ModelBakery bakery) {
        this.delegate = delegate;
        this.overrides = new CustomChickenItemOverrides(delegate.getOverrides(), bakery);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return delegate.getQuads(state, side, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
