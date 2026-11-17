# Entity / block-entity models (vehicle and structure modules)

Client-only. These are the 1.12.2 `ModelBase` / `ModelRenderer` classes of `vehicle/model` and `structure/model`,
converted to 1.21.1 `LayerDefinition` / `ModelPart` by a one-off converter script that is not part of this
repository (input: the original 1.12 sources). The generated Java files are the source now; edit them directly.

## How to use from a renderer

```java
// registration (module client entry point, mod bus)
modBus.addListener(VehicleModelLayers::register);      // EntityRenderersEvent.RegisterLayerDefinitions
modBus.addListener(StructureModelLayers::register);

// in the renderer constructor (EntityRendererProvider.Context / BlockEntityRendererProvider.Context)
ModelCatapultStandFixed model = new ModelCatapultStandFixed(context.bakeLayer(ModelCatapultStandFixed.LAYER));

// per frame: same setter calls as 1.12, then
model.render(poseStack, bufferSource.getBuffer(renderType), packedLight, packedOverlay, color);
```

- 1.12 `new ModelX()` -> `new ModelX(ModelPart root)`; `ModelX.createLayer()` builds the tree, `ModelX.LAYER` is its location.
- 1.12 `model.render(entity, 0, 0, 0, 0, 0, 0.0625f)` -> `model.render(poseStack, buffer, packedLight, packedOverlay, color)`
  (`color` is ARGB, `-1` = white). It renders the same top level parts in the same order. The `0.0625f` scale is
  what `ModelPart` always applies, so the renderer keeps all its other transforms unchanged.
- Gate partial renders (`renderTop()`, `renderCorner()`, ... which used `0.0625f`) take the same five render arguments.
- Coffins: `renderAll()` / `renderAll(float lidAngle)` -> `renderAll(poseStack, buffer, light, overlay, color)` /
  `renderAll(poseStack, buffer, light, overlay, color, lidAngle)`. In 1.12 they rendered with scale `1f` (16x the
  usual size, the renderers scale by 0.09 / 0.074 themselves); the ported methods wrap the parts in
  `poseStack.scale(16, 16, 16)` so the renderer transforms can again be ported unchanged.
- All animation setters keep name, parameters (degrees) and behaviour; they write `ModelPart.xRot/yRot/zRot`. No
  original changed rotation points at runtime. State set by a setter persists until set again, as in 1.12.
- Constructor defaults of 1.12 (`setHitchAngle(25)`, `setArmRotations(-27, -64)`, arrow head yaw 90 degrees) are
  applied in the new constructors.
- `Trig.toRadians` is inlined as a private helper (`degrees * (3.141592653589793f / 180.f)`, identical to 1.12
  `core.util.Trig`) so the models do not depend on other modules.
- The classes are plain classes (no `Model`/`EntityModel` superclass, like 1.12 where `ModelBase` was only a holder),
  parts needed by the methods are `public final ModelPart` fields named like the 1.12 fields.
- Child names are the 1.12 part names (`new ModelRenderer(this, "name")`, or the variable name for the two Tabula
  coffin models); 1.12 top level parts are children of the baked root.

## Original quirks kept on purpose

- `ModelBoatBallista.render` draws `armMain` on its own and then again as a child of `bottomP1` (1.12 did
  `armMain.render(f6); bottomP1.render(f6); ...` although `bottomP1.addChild(armMain)`); the port does the same.
- `ModelCannonMobileFixed/StandFixed.setTurretRotation(yaw, pitch)` ignore `yaw`, `Model*Boat*.setWheelRotations`
  only use `fr`, as in 1.12.
- The registration helpers are not wired into `AncientWarfareVehiclesClient` / `AncientWarfareStructureClient` yet.

## Differences that could not be kept

- 1.12 drew children in `addChild` order; `PartDefinition` keeps children in a hash map, so draw order inside a part
  is unspecified. Irrelevant for the opaque/cutout render types these models use.
- Top level parts that the original created but never rendered are still in the layer (under the root) and are
  likewise never rendered, because `render` only draws the parts the original drew.

## Self check

`convert_models.py` aborts on any source line it does not recognise, then parses the generated Java again with
independent regular expressions and compares every part (path, boxes, uv offsets, mirror flag, pivot, rotation,
texture size) numerically with the tree parsed from the 1.12 source, and compares the plain counts of
`addOrReplaceChild(` / `.addBox(` with the 1.12 part / box counts (listed per model below).

## Models

The models of the vehicles that are not part of the reduced vehicle module (mobile variants, battering ram, chest cart,
boats: ModelBallistaMobile, ModelBatteringRam, ModelBoatBallista, ModelBoatCatapult, ModelBoatTransport, ModelCannonMobileFixed, ModelCatapultMobileFixed, ModelCatapultMobileTurret, ModelChestCart, ModelTrebuchetMobileFixed) were removed from this branch; they are archived on the branch `vehicle-models`.

### `vehicle.model.ModelArrow2`

- layer `ancientwarfare:arrow2#main`, texture size 192x36, parts 24 (1.12) / 24 (ported), boxes 24 / 24
- top level parts: `headPoint`
- part fields: `headPoint`
- constructor defaults (as 1.12): `headPoint.yRot = toRadians(90.f);`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`

### `vehicle.model.ModelBallistaStand`

- layer `ancientwarfare:ballista_stand#main`, texture size 256x256, parts 45 (1.12) / 45 (ported), boxes 45 / 45
- top level parts: `baseMain`, `armMain`
- part fields: `baseMain`, `armMain`, `trigger1`, `crankAxle`, `armRightMain`, `stringRight`, `armLeftMain`, `stringLeft`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setBowAndStringRotation(float bow, float string)` - unchanged
- `void setTurretRotation(float yaw, float pitch)` - unchanged
- `void setCrankRotations(float angle)` - unchanged
- `void setTriggerAngle(float angle)` - unchanged

### `vehicle.model.ModelCannonStandFixed`

- layer `ancientwarfare:cannon_stand_fixed#main`, texture size 256x256, parts 47 (1.12) / 47 (ported), boxes 47 / 47
- top level parts: `cannonBaseMid`
- part fields: `cannonBaseMid`, `axleRight`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setTurretRotation(float yaw, float pitch)` - unchanged

### `vehicle.model.ModelCannonStandTurret`

- layer `ancientwarfare:cannon_stand_turret#main`, texture size 256x256, parts 52 (1.12) / 52 (ported), boxes 52 / 52
- top level parts: `cannonBaseMid`, `cannonBaseMidcopy17`
- part fields: `cannonBaseMid`, `axleRight`, `cannonBaseMidcopy17`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setTurretRotation(float yaw, float pitch)` - unchanged

### `vehicle.model.ModelCatapultStandFixed`

- layer `ancientwarfare:catapult_stand_fixed#main`, texture size 256x256, parts 23 (1.12) / 23 (ported), boxes 23 / 23
- top level parts: `turretMidBeam`
- part fields: `turretMidBeam`, `pulleyAxle`, `armMain`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setArmRotation(float pitch)` - unchanged
- `void setCrankRotations(float angle)` - unchanged

### `vehicle.model.ModelCatapultStandTurret`

- layer `ancientwarfare:catapult_stand_turret#main`, texture size 256x256, parts 39 (1.12) / 39 (ported), boxes 39 / 39
- top level parts: `standBaseMidBeam`, `turretMidBeam`
- part fields: `standBaseMidBeam`, `turretMidBeam`, `pulleyAxle`, `armMain`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setTurretRotation(float yaw)` - unchanged
- `void setArmRotation(float pitch)` - unchanged
- `void setCrankRotations(float angle)` - unchanged

### `vehicle.model.ModelHwacha`

- layer `ancientwarfare:hwacha#main`, texture size 256x256, parts 52 (1.12) / 52 (ported), boxes 52 / 52
- top level parts: `frontAxle`
- part fields: `frontAxle`, `FRWheelPivot`, `FLWheelPivot`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setModelPitch(float pitch)` - unchanged
- `void setWheelRotations(float fl, float fr, float rl, float rr)` - unchanged

### `vehicle.model.ModelShot`

- layer `ancientwarfare:shot#main`, texture size 128x128, parts 12 (1.12) / 12 (ported), boxes 12 / 12
- top level parts: `Shape1`
- part fields: `Shape1`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`

### `vehicle.model.ModelTrebuchetStandFixed`

- layer `ancientwarfare:trebuchet_stand_fixed#main`, texture size 256x256, parts 24 (1.12) / 24 (ported), boxes 24 / 24
- top level parts: `turretMidBeam`
- part fields: `turretMidBeam`, `trebuchetAxle`, `trebuchetWeightAxle`, `trebuchetSlingString`
- constructor defaults (as 1.12): `setArmRotations(-27.f, -64.f);`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setArmRotations(float main, float string)` - unchanged

### `vehicle.model.ModelTrebuchetStandTurret`

- layer `ancientwarfare:trebuchet_stand_turret#main`, texture size 256x256, parts 43 (1.12) / 43 (ported), boxes 43 / 43
- top level parts: `standBaseMidBeam`, `turretMidBeam`
- part fields: `standBaseMidBeam`, `turretMidBeam`, `trebuchetAxle`, `trebuchetWeightAxle`, `trebuchetSlingString`
- constructor defaults (as 1.12): `setArmRotations(-27.f, -64.f);`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void setArmRotations(float main, float string)` - unchanged
- `void setTurretRotation(float yaw)` - unchanged

### `structure.model.ModelCoffin`

- layer `ancientwarfare:coffin#main`, texture size 144x80, parts 14 (1.12) / 14 (ported), boxes 14 / 14
- top level parts: `sideR2`, `bottom`, `sideL2`, `lid`, `sideBottom`, `sideTop`, `sideR1`, `sideL1`
- part fields: `sideR2`, `bottom`, `sideL2`, `lid`, `sideBottom`, `sideTop`, `sideR1`, `sideL1`
- `void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderAll()` (hand ported)
- `void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color, float lidAngle)` - was `void renderAll(float lidAngle)` (hand ported)

### `structure.model.ModelGateBasic`

- layer `ancientwarfare:gate_basic#main`, texture size 256x256, parts 25 (1.12) / 25 (ported), boxes 25 / 25
- top level parts: `part1`, `top1`, `barsTop`, `cornerTop1`, `side1`
- part fields: `part1`, `top1`, `barsTop`, `cornerTop1`, `side1`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void renderTop(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderTop()`
- `void renderCorner(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderCorner()`
- `void renderSide(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderSide()`
- `void renderSolidWall(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderSolidWall()`
- `void renderBars(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderBars()`
- `void setModelRotation(float rot)` - unchanged
- `float getModelRotation()` - unchanged

### `structure.model.ModelGateBridge`

- layer `ancientwarfare:gate_bridge#main`, texture size 256x256, parts 12 (1.12) / 12 (ported), boxes 12 / 12
- top level parts: `part1`, `bridgePiece`, `bridgeSide1`, `bridgeSide4`, `bridgeCorner1`, `bridgeCorner2`, `bridgeTop1`, `bridgeTop2`, `bridgeSide3`, `bridgeSide2`, `bridgeCorner4`, `bridgeCorner3`
- part fields: `part1`, `bridgePiece`, `bridgeSide1`, `bridgeSide4`, `bridgeCorner1`, `bridgeCorner2`, `bridgeTop1`, `bridgeTop2`, `bridgeSide3`, `bridgeSide2`, `bridgeCorner4`, `bridgeCorner3`
- `void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void render(Entity entity, float f1, float f2, float f3, float f4, float f5, float f6)`
- `void renderGateBlock(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderGateBlock()`
- `void renderTop(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderTop()`
- `void renderCorner(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderCorner()`
- `void renderCorner2(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderCorner2()`
- `void renderSide1(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderSide1()`
- `void renderSide2(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderSide2()`
- `void renderSolidWall(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderSolidWall()`
- `void setModelRotation(float rot)` - unchanged
- `float getModelRotation()` - unchanged

### `structure.model.ModelStoneCoffin`

- layer `ancientwarfare:stone_coffin#main`, texture size 128x128, parts 7 (1.12) / 7 (ported), boxes 7 / 7
- top level parts: `lid`, `baseface`
- part fields: `lid`, `baseface`
- `void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)` - was `void renderAll()` (hand ported)
- `void renderAll(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color, float lidAngle)` - was `void renderAll(float lidAngle)` (hand ported)
