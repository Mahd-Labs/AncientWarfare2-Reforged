/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/Rotation.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to NeoForge
 * 1.21.1: reduced to axis-angle rotation of positions and normals, no matrix or GL path.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

/*
 * Stand in for codechicken.lib.vec.Rotation: rotation of angle radians around the (normalised) axis x, y, z,
 * same quaternion maths as CodeChickenLib so that the 1.12 angles and axis signs carry over unchanged.
 */
public class Rotation extends Transformation {
	public double angle;
	public Vector3 axis;

	//quaternion s, x, y, z
	private double qs;
	private double qx;
	private double qy;
	private double qz;
	private boolean hasQuat;

	public Rotation(double angle, Vector3 axis) {
		this.angle = angle;
		this.axis = axis;
	}

	public Rotation(double angle, double x, double y, double z) {
		this(angle, new Vector3(x, y, z));
	}

	private void computeQuat() {
		if (!hasQuat) {
			double half = angle * 0.5;
			double sin = Math.sin(half);
			qs = Math.cos(half);
			qx = axis.x * sin;
			qy = axis.y * sin;
			qz = axis.z * sin;
			hasQuat = true;
		}
	}

	@Override
	public void apply(Vector3 vec) {
		computeQuat();
		double d = -qx * vec.x - qy * vec.y - qz * vec.z;
		double d1 = qs * vec.x + qy * vec.z - qz * vec.y;
		double d2 = qs * vec.y - qx * vec.z + qz * vec.x;
		double d3 = qs * vec.z + qx * vec.y - qy * vec.x;
		vec.x = d1 * qs - d * qx - d2 * qz + d3 * qy;
		vec.y = d2 * qs - d * qy + d1 * qz - d3 * qx;
		vec.z = d3 * qs - d * qz - d1 * qy + d2 * qx;
	}

	@Override
	public void applyN(Vector3 normal) {
		apply(normal);
	}

	@Override
	public String toString() {
		return "Rotation(" + angle + ", " + axis.x + ", " + axis.y + ", " + axis.z + ")";
	}
}
