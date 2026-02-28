package org.geogebra.common.gpad;

/**
 * Shared GPAD @@env key-name constants used by both
 * {@link XmlSettingsToGpadEnvConverter} (XML &rarr; GPAD) and
 * {@link GpadEnvToXmlConverter} (GPAD &rarr; XML).
 *
 * <p>Keeping the key strings in one place prevents the two converters
 * from drifting out of sync when a new property is added.
 */
public final class GpadEnvKeys {
	private GpadEnvKeys() {
	}

	/** Top-level @@env key for the target GeoGebra app. */
	public static final String APP = "app";

	/** Top-level @@env key for the Suite sub-app override (optional). */
	public static final String SUB_APP = "subApp";

	/** Keys inside {@code coordSystem: ...} blocks (2D and 3D). */
	public static final class CoordSystem {
		public static final String ORIGIN = "origin";
		public static final String SCALE = "scale";
		public static final String X_SCALE = "xscale";
		public static final String Y_SCALE = "yscale";
		public static final String Z_SCALE = "zscale";
		public static final String X_ANGLE = "xAngle";
		public static final String Z_ANGLE = "zAngle";
		public static final String X_MIN = "xMin";
		public static final String X_MAX = "xMax";
		public static final String Y_MIN = "yMin";
		public static final String Y_MAX = "yMax";
		public static final String Z_MIN = "zMin";
		public static final String Z_MAX = "zMax";
	}

	/** Top-level property keys inside {@code ev1/ev2/ev3d} blocks. */
	public static final class Ev {
		public static final String BG_COLOR = "bgColor";
		public static final String AXES_COLOR = "axesColor";
		public static final String GRID_COLOR = "gridColor";
		public static final String RULER_COLOR = "rulerColor";
		public static final String SIZE = "size";
		public static final String COORD_SYSTEM = "coordSystem";
		public static final String AXES = "axes";
		public static final String GRID = "grid";
		public static final String GRID_BOLD = "gridBold";
		public static final String MOUSE_COORDS = "mouseCoords";
		public static final String GRID_TYPE = "gridType";
		public static final String POINT_CAPTURING = "pointCapturing";
		public static final String TOOL_TIPS = "toolTips";
		public static final String LOCKED_AXES_RATIO = "lockedAxesRatio";
		public static final String X_AXIS = "xAxis";
		public static final String Y_AXIS = "yAxis";
		public static final String Z_AXIS = "zAxis";
		public static final String GRID_DIST = "gridDist";
		public static final String LINE_STYLE = "lineStyle";
		public static final String AXES_STYLE = "axesStyle";
		public static final String LABEL_STYLE = "labelStyle";
		public static final String LABEL_SERIF = "serif";
		public static final String RULER_TYPE = "rulerType";
		public static final String PLANE = "plane";
		public static final String LIGHT = "light";
		public static final String Y_AXIS_UP = "yAxisUp";
		public static final String COLORED_AXES = "coloredAxes";
		public static final String CLIPPING = "clipping";
		public static final String PROJECTION = "projection";
	}

	/** Sub-keys inside {@code xAxis:/yAxis:/zAxis:} blocks. */
	public static final class Axis {
		public static final String SHOW = "show";
		public static final String LABEL = "label";
		public static final String UNIT = "unit";
		public static final String PI_UNIT = "piUnit";
		public static final String NUMBERS = "numbers";
		public static final String TICK_EXPR = "tickExpr";
		public static final String TICK_DIST = "tickDist";
		public static final String TICK_STYLE = "tickStyle";
		public static final String CROSS = "cross";
		public static final String CROSS_EDGE = "crossEdge";
		public static final String POSITIVE = "positive";
		public static final String SELECTABLE = "selectable";
	}

	/** Sub-keys inside {@code projection: ...} blocks. */
	public static final class Projection {
		public static final String DISTANCE = "distance";
		public static final String SEPARATION = "separation";
		public static final String GRAY_SCALED = "grayScaled";
		public static final String SHUT_DOWN_GREEN = "shutDownGreen";
		public static final String ANGLE = "angle";
		public static final String FACTOR = "factor";
	}

	/** Sub-keys inside {@code clipping: ...} blocks. */
	public static final class Clipping {
		public static final String USE = "use";
		public static final String SHOW = "show";
		public static final String SIZE = "size";
	}

	/** Sub-keys inside {@code gridDist: ...} blocks. */
	public static final class GridDist {
		public static final String X = "x";
		public static final String Y = "y";
		public static final String THETA = "theta";
	}
}
