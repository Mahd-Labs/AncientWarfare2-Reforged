/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/vec/Vector3.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to NeoForge
 * 1.21.1: reduced to the methods the Ancient Warfare renderers use.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

/*
 * Minimal stand in for codechicken.lib.vec.Vector3 (only what the AW renderers use). Mutable, double precision.
 */
public class Vector3 {
	public static final Vector3 center = new Vector3(0.5, 0.5, 0.5);

	public double x;
	public double y;
	public double z;

	public Vector3() {
	}

	public Vector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3 copy() {
		return new Vector3(x, y, z);
	}

	public Vector3 set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public Vector3 add(Vector3 vec) {
		x += vec.x;
		y += vec.y;
		z += vec.z;
		return this;
	}

	public Vector3 subtract(Vector3 vec) {
		x -= vec.x;
		y -= vec.y;
		z -= vec.z;
		return this;
	}

	public Vector3 multiply(double f) {
		x *= f;
		y *= f;
		z *= f;
		return this;
	}

	public Vector3 negate() {
		x = -x;
		y = -y;
		z = -z;
		return this;
	}

	public double mag() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public Vector3 normalize() {
		double d = mag();
		if (d != 0) {
			multiply(1 / d);
		}
		return this;
	}

	public double dotProduct(Vector3 vec) {
		double d = vec.x * x + vec.y * y + vec.z * z;
		if (d > 1 && d < 1.00001) {
			d = 1;
		} else if (d < -1 && d > -1.00001) {
			d = -1;
		}
		return d;
	}

	public Vector3 crossProduct(Vector3 vec) {
		double d = y * vec.z - z * vec.y;
		double d1 = z * vec.x - x * vec.z;
		double d2 = x * vec.y - y * vec.x;
		x = d;
		y = d1;
		z = d2;
		return this;
	}

	public Vector3 apply(Transformation t) {
		t.apply(this);
		return this;
	}

	public Translation translation() {
		return new Translation(x, y, z);
	}

	@Override
	public String toString() {
		return "Vector3(" + x + ", " + y + ", " + z + ")";
	}
}
