/*
 * Derived from CodeChickenLib 3.2.3 (TheCBProject/CodeChickenLib, branch 1.12.x), Copyright ChickenBones and
 * covers1624, licensed under the GNU LGPL 2.1; distributed here under the GNU GPL v3 or later as permitted by
 * section 3 of the LGPL 2.1.
 * Source file: codechicken/lib/render/CCModel.java. Changed 2026-11-14 for the port of Ancient Warfare 2 to
 * NeoForge 1.21.1: reduced to quad models and the operations the Ancient Warfare renderers use, rendering through
 * the port's CCRenderState.
 */
package net.shadowmage.ancientwarfare.core.render.lib;

import javax.annotation.Nullable;
import java.util.List;

/*
 * Stand in for codechicken.lib.render.CCModel, quads only (vertex mode 7 in the 1.12 code): a flat list of
 * vertices (4 per quad) with texture coordinates in 0..1 of the model texture and optional per vertex normals.
 * Only the operations the AW renderers use are implemented, with the same semantics as CodeChickenLib 3.2.3.
 */
public class CCModel {
	public static final int vp = 4;

	public Vector3[] verts;
	public double[] us;
	public double[] vs;
	@Nullable
	public Vector3[] normals;

	private CCModel(int numVerts) {
		verts = new Vector3[numVerts];
		us = new double[numVerts];
		vs = new double[numVerts];
	}

	public static CCModel newModel(int numVerts) {
		return new CCModel(numVerts);
	}

	/*
	 * polys: one int[]{vertexIndex, uvIndex, normalIndex} (1 based, 0 = absent) per vertex, as OBJParser produces them
	 */
	public static CCModel createModel(List<Vector3> verts, List<Vector3> uvs, List<Vector3> normals, List<int[]> polys) {
		if (polys.size() < vp || polys.size() % vp != 0) {
			throw new IllegalArgumentException("Invalid number of vertices for model: " + polys.size());
		}

		boolean hasNormals = polys.get(0)[2] > 0;
		CCModel model = new CCModel(polys.size());
		if (hasNormals) {
			model.normals = new Vector3[polys.size()];
		}

		for (int i = 0; i < polys.size(); i++) {
			int[] ai = polys.get(i);
			Vector3 vert = verts.get(ai[0] - 1).copy();
			Vector3 uv = ai[1] <= 0 ? new Vector3() : uvs.get(ai[1] - 1).copy();
			if (ai[2] > 0 != hasNormals) {
				throw new IllegalArgumentException("Normals are an all or nothing deal here.");
			}

			model.verts[i] = vert;
			model.us[i] = uv.x;
			model.vs[i] = uv.y;
			if (hasNormals) {
				model.normals[i] = normals.get(ai[2] - 1).copy();
			}
		}

		return model;
	}

	public CCModel copy() {
		CCModel model = new CCModel(verts.length);
		for (int i = 0; i < verts.length; i++) {
			model.verts[i] = verts[i].copy();
			model.us[i] = us[i];
			model.vs[i] = vs[i];
		}
		if (normals != null) {
			model.normals = new Vector3[normals.length];
			for (int i = 0; i < normals.length; i++) {
				model.normals[i] = normals[i] == null ? null : normals[i].copy();
			}
		}
		return model;
	}

	public CCModel apply(Transformation t) {
		for (Vector3 vert : verts) {
			t.apply(vert);
		}

		if (normals != null) {
			for (Vector3 normal : normals) {
				t.applyN(normal);
			}
		}

		return this;
	}

	/*
	 * Same result as CodeChickenLib 3.2.3 backfacedCopy(): a copy with the winding of every polygon reversed
	 * and the normals negated (it does not keep the front faces).
	 */
	public CCModel backfacedCopy() {
		CCModel dst = copy();
		int[][] o = new int[][] {{0, 0}, {1, vp - 1}, {2, vp - 2}, {3, vp - 3}};
		for (int i = 0; i < verts.length; i++) {
			int b = (i / vp) * vp;
			int d = i % vp;
			int di = b + o[d][1];
			int si = b + o[d][0];
			dst.verts[di] = verts[si].copy();
			dst.us[di] = us[si];
			dst.vs[di] = vs[si];
			if (normals != null && normals[si] != null) {
				dst.normals[di] = normals[si].copy().negate();
			}
		}
		return dst;
	}

	/*
	 * flat normals per polygon from the first, second and last vertex
	 */
	public CCModel computeNormals() {
		if (normals == null) {
			normals = new Vector3[verts.length];
		}
		for (int i = 0; i < verts.length; i += vp) {
			Vector3 diff1 = verts[i + 1].copy().subtract(verts[i]);
			Vector3 diff2 = verts[i + vp - 1].copy().subtract(verts[i]);
			normals[i] = diff1.crossProduct(diff2).normalize();
			for (int d = 1; d < vp; d++) {
				normals[i + d] = normals[i].copy();
			}
		}
		return this;
	}

	public void render(CCRenderState state, IconTransformation icon) {
		render(state, null, icon);
	}

	public void render(CCRenderState state, @Nullable LightModel lightModel, IconTransformation icon) {
		state.render(this, lightModel, icon);
	}
}
