package net.shadowmage.ancientwarfare.structure.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.shadowmage.ancientwarfare.structure.tile.TileSoundBlock;

import javax.annotation.Nullable;
import java.util.List;

/*
 * Port notes: 1.12 CodeChickenLib block bakery (ISimpleBlockBakery): the sound block is drawn with the quads of the
 * block it is disguised as (default: jukebox). handleState (tile -> extended state property) is
 * TileSoundBlock.getModelData(), bakeQuads is getQuads below. Installed over the baked sound block model in
 * ModelEvent.ModifyBakingResult; the item keeps the plain jukebox model (bakeItemQuads).
 */
public class SoundBlockRenderer implements IDynamicBakedModel {
	private final BakedModel jukebox;

	public SoundBlockRenderer(BakedModel original) {
		this.jukebox = original;
	}

	private static BlockState getDisguise(ModelData data) {
		BlockState disguiseState = data.get(TileSoundBlock.DISGUISE_BLOCK);
		return disguiseState == null ? Blocks.JUKEBOX.defaultBlockState() : disguiseState;
	}

	private static BakedModel getModel(BlockState disguiseState) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModel(disguiseState);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		BlockState disguiseState = getDisguise(extraData);
		return getModel(disguiseState).getQuads(disguiseState, face, rand, ModelData.EMPTY, renderType);
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		BlockState disguiseState = getDisguise(data);
		return getModel(disguiseState).getRenderTypes(disguiseState, rand, ModelData.EMPTY);
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		return getModel(getDisguise(data)).getParticleIcon(ModelData.EMPTY);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return jukebox.isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return jukebox.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return jukebox.getParticleIcon();
	}

	@Override
	public ItemTransforms getTransforms() {
		return jukebox.getTransforms();
	}

	@Override
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}
}
