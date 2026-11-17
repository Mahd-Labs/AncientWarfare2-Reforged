/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/Translation.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to
 * NeoForge 1.21.1: reduced to applying the translation to positions.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

public class Translation extends Transformation {
	public Vector3 vec;

	public Translation(Vector3 vec) {
		this.vec = vec;
	}

	public Translation(double x, double y, double z) {
		this(new Vector3(x, y, z));
	}

	@Override
	public void apply(Vector3 vec) {
		vec.add(this.vec);
	}

	@Override
	public void applyN(Vector3 normal) {
		//translation does not change normals
	}

	@Override
	public String toString() {
		return "Translation(" + vec.x + ", " + vec.y + ", " + vec.z + ")";
	}
}
