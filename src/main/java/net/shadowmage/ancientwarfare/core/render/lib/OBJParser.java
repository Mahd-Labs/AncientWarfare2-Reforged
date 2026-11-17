/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/render/OBJParser.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to
 * NeoForge 1.21.1: translated to quad-only parsing from an input stream, without the Minecraft resource manager.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Port of codechicken.lib.render.OBJParser (quad mode only): parses a Wavefront .obj into one CCModel per "g" group.
 * Same conventions: v is flipped (1 - v), polygons are stored with reversed winding (BaseBakery reverses them back
 * with backfacedCopy()), triangles become degenerate quads, groups without polygons are skipped.
 */
public class OBJParser {
	private OBJParser() {}

	public static Map<String, CCModel> parseModels(InputStream input, @Nullable Transformation coordSystem) throws IOException {
		if (coordSystem == null) {
			coordSystem = new RedundantTransformation();
		}

		HashMap<String, CCModel> modelMap = new HashMap<>();
		ArrayList<Vector3> verts = new ArrayList<>();
		ArrayList<Vector3> uvs = new ArrayList<>();
		ArrayList<Vector3> normals = new ArrayList<>();
		ArrayList<int[]> polys = new ArrayList<>();
		String modelName = "unnamed";

		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

		String line;
		while ((line = reader.readLine()) != null) {
			line = line.replaceAll("\\s+", " ").trim();
			if (line.startsWith("#") || line.length() == 0) {
				continue;
			}

			if (line.startsWith("v ")) {
				double[] values = parseDoubles(line.substring(2), " ");
				illegalAssert(values.length >= 3, "Vertices must have x, y and z components");
				Vector3 vert = new Vector3(values[0], values[1], values[2]);
				coordSystem.apply(vert);
				verts.add(vert);
				continue;
			}
			if (line.startsWith("vt ")) {
				double[] values = parseDoubles(line.substring(3), " ");
				illegalAssert(values.length >= 2, "Tex Coords must have u, and v components");
				uvs.add(new Vector3(values[0], 1 - values[1], 0));
				continue;
			}
			if (line.startsWith("vn ")) {
				double[] values = parseDoubles(line.substring(3), " ");
				illegalAssert(values.length >= 3, "Normals must have x, y and z components");
				Vector3 norm = new Vector3(values[0], values[1], values[2]).normalize();
				coordSystem.applyN(norm);
				normals.add(norm);
				continue;
			}
			if (line.startsWith("f ")) {
				String[] av = line.substring(2).split(" ");
				illegalAssert(av.length >= 3, "Polygons must have at least 3 vertices");
				int[][] polyVerts = new int[av.length][3];
				for (int i = 0; i < av.length; i++) {
					String[] as = av[i].split("/");
					for (int p = 0; p < as.length; p++) {
						if (as[p].length() > 0) {
							polyVerts[i][p] = Integer.parseInt(as[p]);
						}
					}
				}
				quadulate(polys, polyVerts);
			}
			if (line.startsWith("g ")) {
				if (!polys.isEmpty()) {
					modelMap.put(modelName, CCModel.createModel(verts, uvs, normals, polys));
					polys.clear();
				}
				modelName = line.substring(2);
			}
		}

		if (!polys.isEmpty()) {
			modelMap.put(modelName, CCModel.createModel(verts, uvs, normals, polys));
		}

		return modelMap;
	}

	private static void quadulate(List<int[]> polys, int[][] polyVerts) {
		if (polyVerts.length == 4) {
			polys.add(polyVerts[0]);
			polys.add(polyVerts[3]);
			polys.add(polyVerts[2]);
			polys.add(polyVerts[1]);
		} else {
			for (int i = 2; i < polyVerts.length; i++) {
				polys.add(polyVerts[0]);
				polys.add(polyVerts[i]);
				polys.add(polyVerts[i - 1]);
				polys.add(polyVerts[i - 1]);
			}
		}
	}

	private static double[] parseDoubles(String s, String token) {
		String[] as = s.split(token);
		double[] values = new double[as.length];
		for (int i = 0; i < as.length; i++) {
			values[i] = Double.parseDouble(as[i]);
		}
		return values;
	}

	private static void illegalAssert(boolean b, String err) {
		if (!b) {
			throw new IllegalArgumentException(err);
		}
	}
}
