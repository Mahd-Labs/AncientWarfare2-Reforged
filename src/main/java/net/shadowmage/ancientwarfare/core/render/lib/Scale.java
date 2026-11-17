/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/Scale.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to NeoForge
 * 1.21.1: reduced to applying the scale to positions and normals.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

public class Scale extends Transformation {
	public Vector3 factor;

	public Scale(Vector3 factor) {
		this.factor = factor;
	}

	public Scale(double factor) {
		this(new Vector3(factor, factor, factor));
	}

	public Scale(double x, double y, double z) {
		this(new Vector3(x, y, z));
	}

	@Override
	public void apply(Vector3 vec) {
		vec.x *= factor.x;
		vec.y *= factor.y;
		vec.z *= factor.z;
	}

	@Override
	public void applyN(Vector3 normal) {
		//same as CodeChickenLib: normals are not rescaled
	}

	@Override
	public String toString() {
		return "Scale(" + factor.x + ", " + factor.y + ", " + factor.z + ")";
	}
}
