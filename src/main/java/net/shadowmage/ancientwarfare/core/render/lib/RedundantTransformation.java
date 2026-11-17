/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/RedundantTransformation.java. Changed 2026-11-14 for the port of Ancient
 * Warfare 2 to NeoForge 1.21.1: reduced to the identity operations of the port's Transformation.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

/*
 * identity transformation
 */
public class RedundantTransformation extends Transformation {
	@Override
	public void apply(Vector3 vec) {
	}

	@Override
	public void applyN(Vector3 normal) {
	}

	@Override
	public Transformation at(Vector3 point) {
		return this;
	}

	@Override
	public String toString() {
		return "Nothing()";
	}
}
