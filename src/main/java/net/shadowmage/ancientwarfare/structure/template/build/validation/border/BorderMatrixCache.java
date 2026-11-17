package net.shadowmage.ancientwarfare.structure.template.build.validation.border;

import java.util.HashMap;
import java.util.Map;

public class BorderMatrixCache {
	private BorderMatrixCache() {}

	private static final Map<StructureDimensions, BorderMatrix> simpleBorderMatrices = new HashMap<>();

	public static BorderMatrix getBorderMatrix(int xSize, int zSize, int borderSize) {
		boolean simpleBorder = true;
		BorderMatrix borderMatrix = null;
		if (simpleBorder) {
			StructureDimensions dims = new StructureDimensions(xSize, zSize, borderSize);
			if (!simpleBorderMatrices.containsKey(dims)) {
				simpleBorderMatrices.put(dims, constructBorderMatrix(dims));
			}
			borderMatrix = simpleBorderMatrices.get(dims);
		} else {
			//per template name border cache for custom borders
		}

		return borderMatrix;
	}

	/*
	 * Improvement (not baseline, budgeted_placement): the budgeted smoothing (SmoothingMatrixBuilder.buildSteps) builds a
	 * missing border matrix on a background thread (the builder only works on its own objects) and puts it into the cache
	 * on the server thread. Cache access stays on the server thread.
	 */
	public static BorderMatrix getCachedBorderMatrix(int xSize, int zSize, int borderSize) {
		return simpleBorderMatrices.get(new StructureDimensions(xSize, zSize, borderSize));
	}

	public static BorderMatrix buildBorderMatrix(int xSize, int zSize, int borderSize) {
		return new BorderMatrixBuilder(xSize, zSize, borderSize).build();
	}

	public static void putBorderMatrix(int xSize, int zSize, int borderSize, BorderMatrix matrix) {
		simpleBorderMatrices.putIfAbsent(new StructureDimensions(xSize, zSize, borderSize), matrix);
	}

	private static BorderMatrix constructBorderMatrix(StructureDimensions dims) {
		return new BorderMatrixBuilder(dims.xSize, dims.zSize, dims.borderSize).build();
	}

	private static class StructureDimensions {
		private int xSize;
		private int zSize;
		private int borderSize;

		public StructureDimensions(int xSize, int zSize, int borderSize) {
			this.xSize = xSize;
			this.zSize = zSize;
			this.borderSize = borderSize;
		}

		@Override
	public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			StructureDimensions that = (StructureDimensions) o;
			return xSize == that.xSize && zSize == that.zSize && borderSize == that.borderSize;
		}

		@Override
	public int hashCode() {
			int result = xSize;
			result = 31 * result + zSize;
			result = 31 * result + borderSize;
			return result;
		}
	}
}
