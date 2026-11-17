# core.render - bakery models without CodeChickenLib

1.12 rendered the torque / flywheel / windmill / sound blocks with CodeChickenLib (CCL): an OBJ file parsed into
named groups (`CCModel`), CPU side transformations per group, quads baked on demand per extended block state
(`ISimpleBlockBakery`, `CCBakeryModel`, `ModelBakery` key cache) and the same groups drawn every frame by a
`FastTESR` for the moving parts. CCL does not exist for 1.21.1 and the NeoForge OBJ loader bakes a model once with
fixed geometry, which cannot express "rotate group X by the tile's current angle". So the small part of CCL that AW
used is re-implemented here with the same class and method names, on top of NeoForge `IDynamicBakedModel`,
`ModelData` / `ModelProperty` and `BakedQuad`.

**Attribution:** the classes in `lib/` are derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch
1.12.x), Copyright ChickenBones and covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3
or later as permitted by section 3 of the LGPL 2.1. Each file carries this notice and its changes; the same entry is
in `NOTICE` at the repository root.

Everything in this package except `property/CoreProperties` and `property/IExtendedBlockState` is client only.

## What maps to what

| 1.12 (CCL / Forge) | port |
|---|---|
| `codechicken.lib.render.CCModel`, `OBJParser`, `CCRenderState` | `core.render.lib.CCModel`, `OBJParser`, `CCRenderState` |
| `codechicken.lib.vec.Vector3`, `Transformation`, `Rotation`, `Translation`, `Scale`, `RedundantTransformation`, `TransformationList` | same names in `core.render.lib` |
| `codechicken.lib.vec.uv.IconTransformation` | `core.render.lib.IconTransformation` |
| `codechicken.lib.lighting.LightModel.standardLightModel` | `core.render.lib.LightModel.standardLightModel` |
| `BakingVertexBuffer.create()` + `buffer.begin(7, ITEM)` + `ccrs.bind(buffer)` ... `buffer.bake()` | `ccrs.reset(); ccrs.startBaking(blockShading)` ... `ccrs.bake()` |
| `ccrs.bind(bufferBuilder); ccrs.setBrightness(world, pos)` (FastTESR) | `ccrs.reset(); ccrs.bind(vertexConsumer, poseStack.last(), packedLight, packedOverlay)` |
| `IExtendedBlockState` (Forge) | `core.render.property.IExtendedBlockState` (final class: `BlockState` + unlisted values) |
| `IUnlistedProperty<T>`, `Properties.toUnlisted(..)`, `PropertyFloat` | `ModelProperty<T>` (`new ModelProperty<>()`) |
| `ISimpleBlockBakery` implemented by `BaseBakery` | `BaseBakery` is the base class itself |
| `CCBakeryModel` + `ModelRegistryHelper.register(location, model)` | `new BakeryModel(bakery, keyGenerator, particleSprite)` put into `ModelEvent.ModifyBakingResult#getModels()` |
| `ModelBakery.registerBlockKeyGenerator(block, generator)` | the `BlockStateKeyGenerator` argument of `BakeryModel` |
| `IBakeryProvider.getBakery()` on the block | not needed, the bakery is an argument of `BakeryModel` |
| `Block.getExtendedState` -> `bakery.handleState(state, world, pos)` | `BakeryModel.getModelData` calls `bakery.handleState(new IExtendedBlockState(state), level, pos)` |
| `PerspectiveProperties` (`getModelProperties`) | `ItemTransforms` |
| `TextureStitchEvent.Pre` + `registerSprite` + `bakery.setSprite(sprite)` | `TextureAtlasStitchedEvent` (block atlas): `bakery.setSprite(atlas.getSprite(id))` |
| `ModelLoaderHelper.registerItem(block, MODEL_LOCATION)` | same call from the module client init (aliases the item model to that baked model) |

`BaseBakery` keeps its fields (`groups`, `sprite`, `iconTransform`) and override points (`getBaseTransformation`,
`handleState`, `applyModelTransforms`, `renderBlockModels`, `renderItemModels`, `getIconTransform(state|stack)`,
`bakeQuads`, `bakeItemQuads`, `getModelProperties`). `RotatableBlockRenderer` is unchanged apart from the types.

## How an automation renderer ports

```java
public class TorqueShaftRenderer extends TorqueTieredRenderer<TileTorqueShaft> {
    public static final ModelResourceLocation LIGHT_MODEL_LOCATION = ...;   // any id, it only keys the model registry
    public static final TorqueShaftRenderer INSTANCE = new TorqueShaftRenderer();
    private TorqueShaftRenderer() {
        super("automation/torque_shaft.obj");                // read from the jar: assets/ancientwarfare/models/block/automation/torque_shaft.obj
        inputHead = removeGroups(s -> s.startsWith("southShaft."));   // unchanged
    }
    // applyModelTransforms / transformMovingParts / handleAdditionalProperties: unchanged bodies,
    // imports switch to core.render.lib.* and core.render.property.IExtendedBlockState
}
```

Client init of the module:

```java
modBus.addListener((TextureAtlasStitchedEvent e) -> {
    if (e.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS))
        TorqueShaftRenderer.INSTANCE.setSprite(TorqueTier.LIGHT, e.getAtlas().getSprite(ResourceLocation.fromNamespaceAndPath("ancientwarfare", "model/automation/torque_shaft_light")));
});
modBus.addListener((ModelEvent.ModifyBakingResult e) -> {
    BakeryModel model = new BakeryModel(TorqueShaftRenderer.INSTANCE, keyGenerator, () -> TorqueShaftRenderer.INSTANCE.getSprite(TorqueTier.LIGHT));
    // replace the model of every block state of the block (BlockModelShaper.stateToModelLocation(state)) and, for the item,
    // either put it under ModelResourceLocation.inventory(itemId) or use ModelLoaderHelper.registerItem(block, LOCATION)
    e.getModels().put(BlockModelShaper.stateToModelLocation(state), model);
});
```

The block still needs a blockstate json (any placeholder model) so that the model location exists and no "missing
model" error is logged; the BakeryModel replaces it. Blocks whose model depends on the block entity must make the
chunk re-mesh when that data changes (`level.sendBlockUpdated`), exactly as 1.12 needed `markBlockRangeForRenderUpdate`.

Textures under `textures/model/**` are not part of the block atlas by default in 1.21.1. This branch adds
`assets/minecraft/atlases/blocks.json` with a directory source `model` (prefix `model/`), so the 1.12 sprite names
(`ancientwarfare:model/automation/torque_shaft_light`) stay valid.

Moving parts (1.12 `BaseAnimationRenderer extends FastTESR`): a `BlockEntityRenderer` builds the dynamic
`IExtendedBlockState` like `TorqueAnimationRenderer.handleState` did and calls the bakery's
`renderTransformedBlockModels(ccrs, state)`:

```java
CCRenderState ccrs = CCRenderState.instance();
ccrs.reset();
ccrs.bind(buffers.getBuffer(RenderType.cutout()), poseStack.last(), packedLight, OverlayTexture.NO_OVERLAY);
bakery.renderTransformedBlockModels(ccrs, exState);     // group.render(ccrs, LightModel.standardLightModel, getIconTransform(state))
```

Use a block render type (`solid` / `cutout`): they take the vertex colour as is, so the CCL light model result is what
is shown. The entity render types would shade a second time from the normals.

## Behaviour notes (verified / not verified)

- `OBJParser`, `CCModel.backfacedCopy()` (in CCL 3.2.3 this *replaces* the faces by their reversed copies, it does not
  double them), `computeNormals()`, `Rotation` and `Transformation.at/with` were written against the CCL 3.2.3
  sources. Checked headless on the 11 automation OBJ files: 535 groups, 2850 quads, all normals point outwards, all
  texture coordinates within 0..1. Not checked visually.
- Shading of baked block quads: 1.12 Forge applied `LightUtil.diffuseLight(normal)` per vertex. 1.21.1 shades a
  quad by its `Direction` only, so `startBaking(true)` multiplies that same diffuse term into the vertex colour and
  flags the quad as unshaded. Item quads are left white and are lit through their normals.
- Item display transforms are the 1.12 values; the left hand ones are entered so that vanilla's own left hand
  mirroring ends up at the 1.12 matrices. Not verified in game.
- The quad cache is per `BakeryModel` (Guava cache, 5 minutes after last access, max 4096 keys) and is thread safe;
  `CCRenderState.instance()` is per thread because chunk meshing calls `getQuads` on worker threads.
- Model files are read from the mod jar, not through the resource manager: resource packs cannot override the
  `.obj` files (they could in 1.12).
