package net.shadowmage.ancientwarfare.structure.template;

import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.util.INBTSerializable;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;
import net.shadowmage.ancientwarfare.core.util.MathUtils;
import net.shadowmage.ancientwarfare.core.util.NBTHelper;
import net.shadowmage.ancientwarfare.structure.api.TemplateRule;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleBlock;
import net.shadowmage.ancientwarfare.structure.api.TemplateRuleEntityBase;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidationType;
import net.shadowmage.ancientwarfare.structure.template.build.validation.StructureValidator;
import net.shadowmage.ancientwarfare.structure.template.datafixes.DataFixManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StructureTemplate {
	/*
	 * base datas
	 */
	public final String name;
	public final Vec3i size;
	public final Vec3i offset;
	private Version version;

	/*
	 * stored template data
	 */
	public final Set<String> modDependencies;
	private Map<Integer, TemplateRuleBlock> blockRules;
	private Map<Integer, TemplateRuleEntityBase> entityRules;
	/*
	 * Port change (memory): 1.12 keeps one short per block of every loaded template for good - about 1.2 GB for the
	 * bundled pack (1484 templates, 595 million positions; 50 templates hold 70 % of it), which made a dedicated
	 * server with a 2 GB heap run out of memory during world generation. The data is kept run-length encoded
	 * (templates are mostly long runs of air and of the same rule) and the plain array is rebuilt on demand and held
	 * by a soft reference, so it lives while a template is being built or scanned and can go when memory is short.
	 */
	private short[] packedData = new short[0];
	private int dataLength = 0;
	private java.lang.ref.SoftReference<short[]> plainData = new java.lang.ref.SoftReference<>(null);
	private List<BuildResource> resourceList;

	/*
	 * world generation placement validation settings
	 */
	private StructureValidator validator = StructureValidationType.GROUND.getValidator();

	public StructureTemplate(String name, Set<String> modDependencies, Vec3i size, Vec3i offset) {
		this(name, modDependencies, DataFixManager.getCurrentVersion(), size, offset);
	}

	public StructureTemplate(String name, Set<String> modDependencies, Version version, Vec3i size, Vec3i offset) {
		this.modDependencies = modDependencies;
		this.version = version;
		this.name = name;
		this.size = size;
		this.offset = offset;
	}

	public Map<Integer, TemplateRuleEntityBase> getEntityRules() {
		return entityRules;
	}

	public Map<Integer, TemplateRuleBlock> getBlockRules() {
		return blockRules;
	}

	public short[] getTemplateData() {
		short[] plain = plainData.get();
		if (plain == null) {
			plain = unpack(packedData, dataLength);
			plainData = new java.lang.ref.SoftReference<>(plain);
		}
		return plain;
	}

	/* pairs of (value, run length as an unsigned short); the plain data itself when that is not smaller */
	private static short[] pack(short[] plain) {
		int runs = 0;
		for (int i = 0; i < plain.length; ) {
			int j = i + 1;
			while (j < plain.length && plain[j] == plain[i] && j - i < 65535) {
				j++;
			}
			runs++;
			i = j;
		}
		if (runs * 2 >= plain.length) {
			return plain;
		}
		short[] packed = new short[runs * 2];
		int out = 0;
		for (int i = 0; i < plain.length; ) {
			int j = i + 1;
			while (j < plain.length && plain[j] == plain[i] && j - i < 65535) {
				j++;
			}
			packed[out++] = plain[i];
			packed[out++] = (short) (j - i);
			i = j;
		}
		return packed;
	}

	private static short[] unpack(short[] packed, int length) {
		if (packed.length == length) {
			return packed;
		}
		short[] plain = new short[length];
		int pos = 0;
		for (int i = 0; i + 1 < packed.length && pos < length; i += 2) {
			int run = Math.min(packed[i + 1] & 0xFFFF, length - pos);
			if (packed[i] != 0) {
				java.util.Arrays.fill(plain, pos, pos + run, packed[i]);
			}
			pos += run;
		}
		return plain;
	}

	public StructureValidator getValidationSettings() {
		return validator;
	}

	public void setBlockRules(Map<Integer, TemplateRuleBlock> rules) {
		this.blockRules = rules;
	}

	public void setEntityRules(Map<Integer, TemplateRuleEntityBase> rules) {
		this.entityRules = rules;
	}

	public void setTemplateData(short[] datas) {
		this.dataLength = datas.length;
		this.packedData = pack(datas);
		this.plainData = new java.lang.ref.SoftReference<>(datas);
	}

	public void setValidationSettings(StructureValidator settings) {
		this.validator = settings;
	}

	public Optional<TemplateRuleBlock> getRuleAt(Vec3i pos) {
		int index = getIndex(pos, size);
		short[] templateData = getTemplateData();
		int ruleIndex = index >= 0 && index < templateData.length ? templateData[index] : -1;
		return Optional.ofNullable(blockRules.get(ruleIndex));
	}

	public static int getIndex(Vec3i pos, Vec3i size) {
		return (pos.getY() * size.getX() * size.getZ()) + (pos.getZ() * size.getX()) + pos.getX();
	}

	@Override
	public String toString() {
		return "name: " + name + "\n" + "size: " + size.getX() + ", " + size.getY() + ", " + size.getZ() + "\n" + "buildKey: " + offset.getX() + ", " + offset.getY() + ", " + offset.getZ();
	}

	public List<BuildResource> getResourceList() {
		if (resourceList == null) {
			List<BuildResource> allResources = new ArrayList<>();

			NonNullList<ItemStack> consumeOnlyResources = NonNullList.create();

			MathUtils.getAllVecsInBox(Vec3i.ZERO, new Vec3i(size.getX() - 1, size.getY() - 1, size.getZ() - 1))
					.forEach(pos -> getRuleAt(pos).ifPresent(r -> {
								ItemStack remainingStack = r.getRemainingStack();
								List<ItemStack> resources = r.getResources();
								if (remainingStack.isEmpty() || resources.size() > 1) {
									consumeOnlyResources.addAll(resources);
								} else {
									resources.forEach(res -> allResources.add(new BuildResource(res, remainingStack)));
								}
							})
					);
			InventoryTools.compactStackList(consumeOnlyResources).forEach(res -> allResources.add(new BuildResource(res)));

			resourceList = allResources;
		}
		return resourceList;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof StructureTemplate))
			return false;
		StructureTemplate that = (StructureTemplate) o;
		return size.equals(that.size) && offset.equals(that.offset) && name.equals(that.name);
	}

	@Override
	public int hashCode() {
		int result = size.hashCode();
		result = 31 * result + offset.hashCode();
		result = 31 * result + name.hashCode();
		return result;
	}

	public Version getVersion() {
		return version;
	}

	public Vec3i getSize() {
		return size;
	}

	public Vec3i getOffset() {
		return offset;
	}

	public CompoundTag serializeNBT() {
		CompoundTag tag = new CompoundTag();

		tag.putString("name", name);
		tag.putLong("size", MathUtils.toLong(size));
		tag.putLong("offset", MathUtils.toLong(offset));
		tag.put("blockRules", serializeRules(blockRules));
		tag.put("entityRules", serializeRules(entityRules));
		tag.putIntArray("templateData", MathUtils.toIntArray(getTemplateData()));
		tag.putString("validationType", validator.validationType.getName());
		tag.put("validator", validator.serializeToNBT());

		return tag;
	}

	public static StructureTemplate deserializeNBT(CompoundTag tag) {
		String name = tag.getString("name");
		Vec3i size = MathUtils.fromLong(tag.getLong("size"));
		Vec3i offset = MathUtils.fromLong(tag.getLong("offset"));

		StructureTemplate template = new StructureTemplate(name, Collections.emptySet(), size, offset);
		template.setBlockRules(deserializeRules(tag.getList("blockRules", Tag.TAG_COMPOUND)));
		template.setEntityRules(deserializeRules(tag.getList("entityRules", Tag.TAG_COMPOUND)));
		template.setTemplateData(MathUtils.toShortArray(tag.getIntArray("templateData")));
		StructureValidationType.getTypeFromName(tag.getString("validationType")).ifPresent(type -> {
			StructureValidator structureValidator = type.getValidator();
			structureValidator.readFromNBT(tag.getCompound("validator"));
			template.setValidationSettings(structureValidator);
		});
		return template;
	}

	private static <T extends TemplateRule> Map<Integer, T> deserializeRules(ListTag blockRules) {
		Map<Integer, T> ret = new HashMap<>();
		for (Tag data : blockRules) {
			CompoundTag ruleData = (CompoundTag) data;
			StructurePluginManager.INSTANCE.getRuleByName(ruleData.getString("pluginName")).ifPresent(
					rule -> {
						rule.parseRule(ruleData);
						//noinspection unchecked
						ret.put(ruleData.getInt("ruleNumber"), (T) rule);
					}
			);

		}
		return ret;
	}

	private ListTag serializeRules(Map<Integer, ? extends TemplateRule> templateRules) {
		return templateRules.entrySet().stream().map(this::serializeRule).collect(NBTHelper.NBTLIST_COLLECTOR);
	}

	private CompoundTag serializeRule(Map.Entry<Integer, ? extends TemplateRule> blockRule) {
		CompoundTag ruleData = new CompoundTag();
		ruleData.putInt("ruleNumber", blockRule.getKey());
		ruleData.putString("pluginName", blockRule.getValue().getPluginName());
		blockRule.getValue().writeRuleData(ruleData);
		return ruleData;
	}

	public static class Version {
		private final int major;
		private final int minor;

		public Version(int major, int minor) {
			this.major = major;
			this.minor = minor;
		}

		public Version(String version) {
			this(Integer.valueOf(version.substring(0, version.indexOf('.'))), Integer.valueOf(version.substring(version.indexOf('.') + 1)));
		}

		public boolean isGreaterThan(Version otherVersion) {
			return getMajor() > otherVersion.getMajor() || getMinor() > otherVersion.getMinor();
		}

		public static final Version NONE = new Version(0, 0);

		public int getMajor() {
			return major;
		}

		public int getMinor() {
			return minor;
		}
	}

	public static class BuildResource implements INBTSerializable<CompoundTag> {
		private static final String STACK_TO_RETURN_TAG = "stackToReturn";
		private ItemStack stackRequired;
		private ItemStack stackToReturn = ItemStack.EMPTY;
		private int requiredOriginalCount = 0;

		private BuildResource(ItemStack stackRequired, ItemStack stackToReturn) {
			this.stackRequired = stackRequired;
			this.stackToReturn = stackToReturn;
			requiredOriginalCount = stackRequired.getCount();
		}

		private BuildResource(ItemStack stackRequired) {
			this.stackRequired = stackRequired;
		}

		public BuildResource() {}

		public ItemStack getStackRequired() {
			return stackRequired;
		}

		public BuildResource copy() {
			return new BuildResource(stackRequired.copy(), stackToReturn.copy());
		}

		public ItemStack shrinkStackRequiredAndGetRemaining() {
			stackRequired.shrink(1);
			if (!stackToReturn.isEmpty() && stackRequired.getCount() % requiredOriginalCount == 0) {
				return stackToReturn.copy();
			}
			return ItemStack.EMPTY;
		}

		public boolean isEmpty() {
			return stackRequired.isEmpty();
		}

		public CompoundTag serializeNBT() {
			CompoundTag tag = new CompoundTag();
			tag.put("stackRequired", NBTHelper.writeItemStack(stackRequired));
			if (!stackToReturn.isEmpty()) {
				tag.put(STACK_TO_RETURN_TAG, NBTHelper.writeItemStack(stackToReturn));
				tag.putInt("requiredOriginalCount", requiredOriginalCount);
			}
			return tag;
		}

		public void deserializeNBT(CompoundTag tag) {
			stackRequired = NBTHelper.readItemStack(tag.getCompound("stackRequired"));
			if (tag.contains(STACK_TO_RETURN_TAG)) {
				stackToReturn = NBTHelper.readItemStack(tag.getCompound(STACK_TO_RETURN_TAG));
				requiredOriginalCount = tag.getInt("requiredOriginalCount");
			}
		}
	}
}
