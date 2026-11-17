/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/lighting/LightModel.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to
 * NeoForge 1.21.1: mostly rewritten as a grey-scale per-normal light term; the standard light values are kept.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

import java.util.ArrayList;
import java.util.List;

/*
 * Stand in for codechicken.lib.lighting.LightModel (grey scale only): ambient term plus diffuse directional lights,
 * evaluated per vertex normal. standardLightModel has the CodeChickenLib values.
 */
public class LightModel {
	public static final LightModel standardLightModel = new LightModel().setAmbient(0.4).addLight(new Vector3(0.2, 1, -0.7), 0.6).addLight(new Vector3(-0.2, 1, 0.7), 0.6);

	private double ambient;
	private final List<Vector3> lightDirections = new ArrayList<>();
	private final List<Double> lightDiffuse = new ArrayList<>();

	public LightModel setAmbient(double ambient) {
		this.ambient = ambient;
		return this;
	}

	public LightModel addLight(Vector3 direction, double diffuse) {
		lightDirections.add(direction.copy().normalize());
		lightDiffuse.add(diffuse);
		return this;
	}

	/*
	 * @return brightness multiplier 0..1 for a surface with the given normal
	 */
	public float apply(Vector3 normal) {
		double c = ambient;
		for (int i = 0; i < lightDirections.size(); i++) {
			double n_l = lightDirections.get(i).dotProduct(normal);
			double f = n_l > 0 ? 1 : 0;
			c += lightDiffuse.get(i) * f * n_l;
		}
		if (c > 1) {
			c = 1;
		}
		return (float) c;
	}
}
