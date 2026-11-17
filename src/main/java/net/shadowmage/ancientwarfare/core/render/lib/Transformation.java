/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/Transformation.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to
 * NeoForge 1.21.1: reduced to per-vertex application and the at / with combinators.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

/*
 * Stand in for codechicken.lib.vec.Transformation: a transformation applied vertex by vertex on the CPU.
 */
public abstract class Transformation {

	/*
	 * applies this transformation to vec (a position)
	 */
	public abstract void apply(Vector3 vec);

	/*
	 * applies this transformation to a normal (no translation)
	 */
	public abstract void applyN(Vector3 normal);

	/*
	 * this transformation performed around point instead of the origin
	 */
	public Transformation at(Vector3 point) {
		return new TransformationList(new Translation(-point.x, -point.y, -point.z), this, point.translation());
	}

	/*
	 * this transformation followed by t
	 */
	public TransformationList with(Transformation t) {
		return new TransformationList(this, t);
	}
}
