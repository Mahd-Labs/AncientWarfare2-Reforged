package net.shadowmage.ancientwarfare.core.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.shadowmage.ancientwarfare.AncientWarfare;
import net.shadowmage.ancientwarfare.core.render.property.IExtendedBlockState;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/*
 * CLIENT ONLY. Replacement for CodeChickenLib CCBakeryModel + ModelBakery: the BakedModel that is put into the
 * model registry for a block (and its item) rendered by a BaseBakery.
 *
 *  1.12                                                                    port
 *  ModelRegistryHelper.register(LOCATION, new CCBakeryModel() {            modBus.addListener((ModelEvent.ModifyBakingResult e) ->
 *      getParticleTexture() { return INSTANCE.sprite; } })                     e.getModels().put(LOCATION, new BakeryModel(INSTANCE, keyGenerator, () -> INSTANCE.sprite)));
 *  ModelBakery.registerBlockKeyGenerator(block, keyGenerator)              (the keyGenerator constructor argument)
 *  IBakeryProvider.getBakery() on the block                                (the bakery constructor argument)
 *  bakery.handleState(state, world, pos) via Block.getExtendedState        getModelData below
 *
 * Quads are cached by the key generator's key (blocks) or by the item key function (items, default: registry name).
 */
public class BakeryModel implements IDynamicBakedModel {
	private final BaseBakery bakery;
	@Nullable
	private final BlockStateKeyGenerator keyGenerator;
	private final Supplier<TextureAtlasSprite> particleTexture;
	private Function<ItemStack, String> itemKeyGenerator = stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

	private final Cache<String, List<BakedQuad>> blockQuadCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).maximumSize(4096).build();
	private final Cache<String, BakedModel> itemModelCache = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).maximumSize(1024).build();

	private final ItemOverrides overrides = new ItemOverrides() {
		@Override
		public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			return getItemModel(stack);
		}
	};

	/*
	 * @param keyGenerator null: quads are baked on every request (1.12 would have used the CodeChickenLib default key)
	 */
	public BakeryModel(BaseBakery bakery, @Nullable BlockStateKeyGenerator keyGenerator, Supplier<TextureAtlasSprite> particleTexture) {
		this.bakery = bakery;
		this.keyGenerator = keyGenerator;
		this.particleTexture = particleTexture;
	}

	public BakeryModel(BaseBakery bakery, @Nullable BlockStateKeyGenerator keyGenerator) {
		this(bakery, keyGenerator, () -> bakery.sprite);
	}

	/*
	 * replaces the 1.12 item key (registry name + metadata); needed when stack data changes the item model
	 */
	public BakeryModel setItemKeyGenerator(Function<ItemStack, String> itemKeyGenerator) {
		this.itemKeyGenerator = itemKeyGenerator;
		return this;
	}

	/*
	 * call when sprites or model inputs changed (resource reload creates new BakeryModel instances anyway)
	 */
	public void clearCache() {
		blockQuadCache.invalidateAll();
		itemModelCache.invalidateAll();
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		IExtendedBlockState extendedState = bakery.handleState(new IExtendedBlockState(state), level, pos);
		return modelData.derive().with(IExtendedBlockState.STATE, extendedState).build();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
		if (state == null || side != null) {
			return Collections.emptyList();
		}
		IExtendedBlockState extendedState = extraData.get(IExtendedBlockState.STATE);
		try {
			if (extendedState == null) {
				//no model data (block breaking particles, blocks rendered outside of a level): defaults of the bakery
				extendedState = bakery.handleState(new IExtendedBlockState(state), EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
			}
			if (keyGenerator == null) {
				return bakery.bakeQuads(null, extendedState);
			}
			String key = keyGenerator.generateKey(extendedState);
			List<BakedQuad> quads = blockQuadCache.getIfPresent(key);
			if (quads == null) {
				quads = bakery.bakeQuads(null, extendedState);
				blockQuadCache.put(key, quads);
			}
			return quads;
		}
		catch (RuntimeException e) {
			AncientWarfare.LOG.error("Exception baking block model for {}", state, e);
			return Collections.emptyList();
		}
	}

	private BakedModel getItemModel(ItemStack stack) {
		String key = itemKeyGenerator.apply(stack);
		BakedModel model = itemModelCache.getIfPresent(key);
		if (model == null) {
			List<BakedQuad> quads;
			try {
				quads = bakery.bakeItemQuads(null, stack);
			}
			catch (RuntimeException e) {
				AncientWarfare.LOG.error("Exception baking item model for {}", stack, e);
				quads = Collections.emptyList();
			}
			model = new ItemModel(quads, bakery.getModelProperties(stack));
			itemModelCache.put(key, model);
		}
		return model;
	}

	//PerspectiveProperties(state, ambientOcclusion = true, gui3d = true)
	@Override
	public boolean useAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean usesBlockLight() {
		return true;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return particleTexture.get();
	}

	@Override
	public ItemTransforms getTransforms() {
		return bakery.getModelProperties(ItemStack.EMPTY);
	}

	@Override
	public ItemOverrides getOverrides() {
		return overrides;
	}

	/*
	 * the baked quads of one item stack variant
	 */
	private class ItemModel implements BakedModel {
		private final List<BakedQuad> quads;
		private final ItemTransforms transforms;

		private ItemModel(List<BakedQuad> quads, ItemTransforms transforms) {
			this.quads = quads;
			this.transforms = transforms;
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
			return direction == null ? quads : Collections.emptyList();
		}

		@Override
		public boolean useAmbientOcclusion() {
			return true;
		}

		@Override
		public boolean isGui3d() {
			return true;
		}

		@Override
		public boolean usesBlockLight() {
			return true;
		}

		@Override
		public boolean isCustomRenderer() {
			return false;
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return particleTexture.get();
		}

		@Override
		public ItemTransforms getTransforms() {
			return transforms;
		}

		@Override
		public ItemOverrides getOverrides() {
			return ItemOverrides.EMPTY;
		}
	}
}
