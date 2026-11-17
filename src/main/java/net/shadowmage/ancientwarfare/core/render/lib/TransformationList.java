/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/TransformationList.java. Changed 2026-11-14 for the port of Ancient Warfare 2
 * to NeoForge 1.21.1: reduced to an ordered list applied vertex by vertex, no matrix compaction.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransformationList extends Transformation {
	private final List<Transformation> transformations = new ArrayList<>();

	public TransformationList(Transformation... transforms) {
		for (Transformation t : transforms) {
			if (t instanceof TransformationList) {
				transformations.addAll(((TransformationList) t).transformations);
			} else {
				transformations.add(t);
			}
		}
	}

	@Override
	public void apply(Vector3 vec) {
		for (Transformation t : transformations) {
			t.apply(vec);
		}
	}

	@Override
	public void applyN(Vector3 normal) {
		for (Transformation t : transformations) {
			t.applyN(normal);
		}
	}

	@Override
	public TransformationList with(Transformation t) {
		if (t instanceof TransformationList) {
			transformations.addAll(((TransformationList) t).transformations);
		} else {
			transformations.add(t);
		}
		return this;
	}

	@Override
	public String toString() {
		return Arrays.toString(transformations.toArray());
	}
}
