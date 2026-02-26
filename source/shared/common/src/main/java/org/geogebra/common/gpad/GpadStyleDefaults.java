package org.geogebra.common.gpad;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoElement;

/**
 * Single authoritative source for GPAD style default values (XML format).
 * Used by both conversion directions:
 * <ul>
 *   <li>GGB-&gt;GPAD ({@code StyleMapToGpadConverter}): compare against defaults to decide what to omit</li>
 *   <li>GPAD-&gt;GGB ({@code GpadStyleXMLApplier}): fill missing properties with defaults, or reset to defaults</li>
 * </ul>
 * All default values are stored in XML attribute format because both directions
 * work with XML-style attribute maps.
 * <p>
 * Key is the XML tag name (e.g., "lineStyle", "objColor", "fixed").
 */
public final class GpadStyleDefaults {

	// ==================== Simple boolean defaults ====================

	private static final LinkedHashMap<String, String> BOOL_FALSE = createMap("val", "false");
	private static final LinkedHashMap<String, String> BOOL_TRUE = createMap("val", "true");

	// Boolean types with special attr names
	private static final LinkedHashMap<String, String> ALGEBRA_FALSE = createMap("labelVisible", "false");
	private static final LinkedHashMap<String, String> CHECKBOX_FIXED_FALSE = createMap("fixed", "false");
	private static final LinkedHashMap<String, String> USER_INPUT_FALSE = createMap("show", "false");
	private static final LinkedHashMap<String, String> RANDOM_FALSE = createMap("random", "false");

	// ==================== Simple numeric/string defaults ====================

	private static final LinkedHashMap<String, String> VAL_0 = createMap("val", "0");
	private static final LinkedHashMap<String, String> VAL_M1 = createMap("val", "-1");
	private static final LinkedHashMap<String, String> VAL_EMPTY = createMap("val", "");
	private static final LinkedHashMap<String, String> VAL_DEFAULT = createMap("val", "default");
	private static final LinkedHashMap<String, String> VAL_30 = createMap("val", "30");
	private static final LinkedHashMap<String, String> VAL_20 = createMap("val", "20");
	private static final LinkedHashMap<String, String> VAL_1 = createMap("val", "1");
	private static final LinkedHashMap<String, String> VAL_NAN = createMap("val", "NaN");
	private static final LinkedHashMap<String, String> VAL_5 = createMap("val", "5");
	private static final LinkedHashMap<String, String> VAL_LEFT = createMap("val", "left");
	private static final LinkedHashMap<String, String> VAL_TOP = createMap("val", "top");
	private static final LinkedHashMap<String, String> VAL_CARTESIAN = createMap("val", "cartesian");

	// String types with special attr names
	private static final LinkedHashMap<String, String> SRC_EMPTY = createMap("src", "");
	private static final LinkedHashMap<String, String> CURVE_PARAM_EMPTY = createMap("t", "");
	private static final LinkedHashMap<String, String> DECORATION_0 = createMap("type", "0");
	private static final LinkedHashMap<String, String> FILE_EMPTY = createMap("name", "");
	private static final LinkedHashMap<String, String> LINKEDGEO_EXP_EMPTY = createMap("exp", "");

	// ==================== Complex style defaults ====================

	/** bgColor/borderColor/objColor default: #000000 */
	private static final LinkedHashMap<String, String> COLOR_DEFAULTS = createMap(
			"r", "0", "g", "0", "b", "0");

	/** lineStyle default: type=0 (full), thickness=5, typeHidden=1 (dashed) */
	private static final LinkedHashMap<String, String> LINE_STYLE_DEFAULTS = createMap(
			"type", "0", "thickness", "5", "typeHidden", "1");

	/** show default: object=true, label=true, ev=0 */
	private static final LinkedHashMap<String, String> SHOW_DEFAULTS = createMap(
			"object", "true", "label", "true", "ev", "0");

	/** tableview default: column=-1, points=true */
	private static final LinkedHashMap<String, String> TABLEVIEW_DEFAULTS = createMap(
			"column", "-1", "points", "true");

	/** animation default: playing=false, type=0, step=0.1, speed=1
	 * Note: step and speed defaults must match the values used in
	 * StyleMapToGpadConverter.convertAnimation() which treats step=0.1 and speed=1 as default. */
	private static final LinkedHashMap<String, String> ANIMATION_DEFAULTS = createMap(
			"playing", "false", "type", "0", "step", "0.1", "speed", "1");

	/** font default: serif=false, size=0, sizeM=1.0, style=0 */
	private static final LinkedHashMap<String, String> FONT_DEFAULTS = createMap(
			"serif", "false", "size", "0", "sizeM", "1.0", "style", "0");

	/** eqnStyle default: style=implicit */
	private static final LinkedHashMap<String, String> EQN_STYLE_DEFAULTS = createMap(
			"style", "implicit");

	/** boundingBox default: width=0, height=0 */
	private static final LinkedHashMap<String, String> BOUNDING_BOX_DEFAULTS = createMap(
			"width", "0", "height", "0");

	/** contentSize default: width=800, height=600 */
	private static final LinkedHashMap<String, String> CONTENT_SIZE_DEFAULTS = createMap(
			"width", "800", "height", "600");

	/** cropBox default: x=0, y=0, width=0, height=0 */
	private static final LinkedHashMap<String, String> CROP_BOX_DEFAULTS = createMap(
			"x", "0", "y", "0", "width", "0", "height", "0");

	/** dimensions default: width=0, height=0, angle=0, unscaled=true */
	private static final LinkedHashMap<String, String> DIMENSIONS_DEFAULTS = createMap(
			"width", "0", "height", "0", "angle", "0", "unscaled", "true");

	/** labelOffset default: x=0, y=0 */
	private static final LinkedHashMap<String, String> LABEL_OFFSET_DEFAULTS = createMap(
			"x", "0", "y", "0");

	/** slider default: width=200, fixed=false, showAlgebra=false (GeoAngle uses width=180, handled via getAdjustedDefaults) */
	private static final LinkedHashMap<String, String> SLIDER_DEFAULTS = createMap(
			"width", "200", "fixed", "false", "showAlgebra", "false");

	/** slider angle override: width=180 */
	private static final LinkedHashMap<String, String> SLIDER_ANGLE_DEFAULTS = createMap("width", "180");

	/** spreadsheetTrace defaults */
	private static final LinkedHashMap<String, String> SPREADSHEETTRACE_DEFAULTS;
	static {
		SPREADSHEETTRACE_DEFAULTS = new LinkedHashMap<>();
		SPREADSHEETTRACE_DEFAULTS.put("val", "false");
		SPREADSHEETTRACE_DEFAULTS.put("traceColumn1", "-1");
		SPREADSHEETTRACE_DEFAULTS.put("traceColumn2", "-1");
		SPREADSHEETTRACE_DEFAULTS.put("traceRow1", "-1");
		SPREADSHEETTRACE_DEFAULTS.put("traceRow2", "-1");
		SPREADSHEETTRACE_DEFAULTS.put("tracingRow", "0");
		SPREADSHEETTRACE_DEFAULTS.put("numRows", "10");
		SPREADSHEETTRACE_DEFAULTS.put("headerOffset", "1");
		SPREADSHEETTRACE_DEFAULTS.put("doColumnReset", "false");
		SPREADSHEETTRACE_DEFAULTS.put("doRowLimit", "false");
		SPREADSHEETTRACE_DEFAULTS.put("showLabel", "true");
		SPREADSHEETTRACE_DEFAULTS.put("showTraceList", "false");
		SPREADSHEETTRACE_DEFAULTS.put("doTraceGeoCopy", "false");
	}

	/** startPoint default */
	private static final LinkedHashMap<String, String> STARTPOINT_DEFAULTS = createMap(
			"_corners", "\u0002\u00030,0");

	// ==================== Unified defaults map (XML tag name -> default attrs) ====================

	/**
	 * Authoritative default values map.
	 * Key: XML tag name
	 * Value: default attribute map for that tag
	 * <p>
	 * Tags not listed here either:
	 * <ul>
	 *   <li>Are handled by direct reset (condition, dynamicCaption, scripts, tempUserInput, etc.)</li>
	 *   <li>Are auto-generated by commands and don't need defaults (coefficients, eigenvectors, matrix, etc.)</li>
	 * </ul>
	 */
	private static final Map<String, LinkedHashMap<String, String>> DEFAULT_ATTRS_MAP = Map.ofEntries(
			// ==================== Boolean types ====================
			Map.entry("absoluteScreenLocation", BOOL_FALSE),
			Map.entry("allowReflexAngle", BOOL_FALSE),
			Map.entry("algebra", ALGEBRA_FALSE),
			Map.entry("autocolor", BOOL_FALSE),
			Map.entry("auxiliary", BOOL_FALSE),
			Map.entry("breakpoint", BOOL_FALSE),
			Map.entry("centered", BOOL_FALSE),
			Map.entry("checkbox", CHECKBOX_FIXED_FALSE),
			Map.entry("comboBox", BOOL_FALSE),
			Map.entry("contentSerif", BOOL_FALSE),
			Map.entry("emphasizeRightAngle", BOOL_TRUE),
			Map.entry("fixed", BOOL_FALSE),
			Map.entry("inBackground", BOOL_FALSE),
			Map.entry("interpolate", BOOL_FALSE),
			Map.entry("isCaptureSnap", BOOL_FALSE),
			Map.entry("isLaTeX", BOOL_FALSE),
			Map.entry("isMask", BOOL_FALSE),
			Map.entry("keepTypeOnTransform", BOOL_FALSE),
			Map.entry("levelOfDetailQuality", BOOL_FALSE),
			Map.entry("outlyingIntersections", BOOL_FALSE),
			Map.entry("selectionAllowed", BOOL_TRUE),
			Map.entry("showOnAxis", BOOL_FALSE),
			Map.entry("showTrimmed", BOOL_FALSE),
			Map.entry("symbolic", BOOL_FALSE),
			Map.entry("trace", BOOL_FALSE),
			Map.entry("userinput", USER_INPUT_FALSE),
			Map.entry("value", RANDOM_FALSE),

			// ==================== Integer types ====================
			Map.entry("arcSize", VAL_30),
			Map.entry("decimals", VAL_M1),
			Map.entry("layer", VAL_0),
			Map.entry("length", VAL_20),
			Map.entry("selectedIndex", VAL_0),
			Map.entry("significantfigures", VAL_M1),
			Map.entry("slopeTriangleSize", VAL_1),

			// ==================== Float types ====================
			Map.entry("fading", VAL_0),
			Map.entry("ordering", VAL_NAN),
			Map.entry("pointSize", VAL_5),

			// ==================== String types ====================
			Map.entry("angleStyle", VAL_0),
			Map.entry("audio", SRC_EMPTY),
			Map.entry("caption", VAL_EMPTY),
			Map.entry("content", VAL_EMPTY),
			Map.entry("coordStyle", VAL_CARTESIAN),
			Map.entry("curveParam", CURVE_PARAM_EMPTY),
			Map.entry("decoration", DECORATION_0),
			Map.entry("dynamicCaption", VAL_EMPTY),
			Map.entry("endStyle", VAL_DEFAULT),
			Map.entry("file", FILE_EMPTY),
			Map.entry("headStyle", VAL_0),
			Map.entry("incrementY", VAL_EMPTY),
			Map.entry("jsClickFunction", VAL_EMPTY),
			Map.entry("jsUpdateFunction", VAL_EMPTY),
			Map.entry("labelMode", VAL_0),
			Map.entry("linkedGeo", LINKEDGEO_EXP_EMPTY),
			Map.entry("parentLabel", VAL_EMPTY),
			Map.entry("pointStyle", VAL_M1),
			Map.entry("condition", createMap("showObject", "")),
			Map.entry("startStyle", VAL_DEFAULT),
			Map.entry("textAlign", VAL_LEFT),
			Map.entry("tooltipMode", VAL_0),
			Map.entry("verticalAlign", VAL_TOP),
			Map.entry("video", SRC_EMPTY),

			// ==================== Complex types (multi-attribute) ====================
			Map.entry("animation", ANIMATION_DEFAULTS),
			Map.entry("bgColor", COLOR_DEFAULTS),
			Map.entry("borderColor", COLOR_DEFAULTS),
			Map.entry("boundingBox", BOUNDING_BOX_DEFAULTS),
			Map.entry("contentSize", CONTENT_SIZE_DEFAULTS),
			Map.entry("cropBox", CROP_BOX_DEFAULTS),
			Map.entry("dimensions", DIMENSIONS_DEFAULTS),
			Map.entry("eqnStyle", EQN_STYLE_DEFAULTS),
			Map.entry("font", FONT_DEFAULTS),
			Map.entry("labelOffset", LABEL_OFFSET_DEFAULTS),
			Map.entry("lineStyle", LINE_STYLE_DEFAULTS),
			Map.entry("objColor", COLOR_DEFAULTS),
			Map.entry("show", SHOW_DEFAULTS),
			Map.entry("slider", SLIDER_DEFAULTS),
			Map.entry("spreadsheetTrace", SPREADSHEETTRACE_DEFAULTS),
			Map.entry("startPoint", STARTPOINT_DEFAULTS),
			Map.entry("tableview", TABLEVIEW_DEFAULTS)
	);

	// ==================== Required attributes ====================

	/**
	 * Map of required attributes that will throw exceptions if missing
	 * when processed by ConsElementXMLHandler.
	 * Key: XML tag name
	 * Value: Set of attribute names that must be present
	 */
	private static final Map<String, Set<String>> REQUIRED_ATTRS_MAP = Map.ofEntries(
			Map.entry("value", Set.of("val")),
			Map.entry("slider", Set.of("width")),
			Map.entry("cropBox", Set.of("x", "y", "width", "height")),
			Map.entry("video", Set.of("width", "height")),
			Map.entry("spreadsheetTrace", Set.of(
					"val", "traceColumn1", "traceColumn2", "traceRow1", "traceRow2",
					"tracingRow", "numRows", "headerOffset", "doColumnReset", "doRowLimit",
					"showLabel", "showTraceList", "doTraceGeoCopy")),
			Map.entry("lineStyle", Set.of("type", "thickness")),
			Map.entry("boundingBox", Set.of("width", "height")),
			Map.entry("embed", Set.of("id")),
			Map.entry("tag", Set.of("key", "value", "barNumber")),
			Map.entry("contentSize", Set.of("width", "height")),
			Map.entry("tableview", Set.of("column", "points"))
	);

	// ==================== Always-apply tags ====================

	/**
	 * Tags that must be explicitly applied even when absent from a GPAD stylesheet.
	 * <p>
	 * During GPAD→GGB conversion, the applier normally only processes tags present in the
	 * stylesheet. Tags not present are left to GeoElement.init() defaults. However, for
	 * certain tags, init() defaults may differ across GeoElement types or be overridden
	 * by post-creation logic (e.g., AlgebraProcessor calling setFixed(true) for equations).
	 * <p>
	 * To ensure roundtrip consistency (GGB→GPAD→GGB), the applier explicitly applies the
	 * default values from {@link #DEFAULT_ATTRS_MAP} for these tags when they are not in
	 * the stylesheet. This guarantees the GeoElement ends up with the unified default,
	 * regardless of what init() or other creation logic did.
	 */
	public static final Set<String> ALWAYS_APPLY_TAGS = Set.of("fixed", "lineStyle", "layer", "labelMode");

	// ==================== Public API ====================

	/**
	 * Gets the default attributes for a given XML tag.
	 *
	 * @param xmlTagName XML tag name (e.g., "lineStyle", "fixed", "objColor")
	 * @return default attributes map (unmodifiable), or null if no defaults registered
	 */
	public static LinkedHashMap<String, String> getDefaultAttrs(String xmlTagName) {
		return DEFAULT_ATTRS_MAP.get(xmlTagName);
	}

	/**
	 * Gets the default value of a specific attribute within an XML tag.
	 *
	 * @param xmlTagName XML tag name
	 * @param attrName   attribute name within the tag
	 * @return default value string, or null if no default
	 */
	public static String getDefaultAttrValue(String xmlTagName, String attrName) {
		LinkedHashMap<String, String> defaults = DEFAULT_ATTRS_MAP.get(xmlTagName);
		return defaults != null ? defaults.get(attrName) : null;
	}

	/**
	 * Gets the set of required attribute names for a given XML tag.
	 * Required attributes must be present to avoid exceptions in ConsElementXMLHandler.
	 *
	 * @param xmlTagName XML tag name
	 * @return set of required attribute names, or empty set if none
	 */
	public static Set<String> getRequiredAttributes(String xmlTagName) {
		Set<String> required = REQUIRED_ATTRS_MAP.get(xmlTagName);
		return required != null ? required : Collections.emptySet();
	}

	/**
	 * Gets the type-adjusted default attributes for a given XML tag and GeoElement.
	 * Some tags have defaults that depend on the object type (e.g., slider width
	 * is 200 for numbers but 180 for angles).
	 *
	 * @param xmlTagName XML tag name
	 * @param geo        GeoElement to check type
	 * @return optional map of type-specific overrides to merge into defaults,
	 *         empty if no adjustment needed
	 */
	public static Optional<LinkedHashMap<String, String>> getTypeAdjustedDefaults(
			String xmlTagName, GeoElement geo) {
		if ("slider".equals(xmlTagName) && geo instanceof GeoAngle)
			return Optional.of(SLIDER_ANGLE_DEFAULTS);
		return Optional.empty();
	}

	// ==================== Helper methods ====================

	private static LinkedHashMap<String, String> createMap(String... keyValuePairs) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			map.put(keyValuePairs[i], keyValuePairs[i + 1]);
		}
		return map;
	}

	// ==================== @@env settings defaults ====================

	// kernel
	public static final String ENV_ANGLE_UNIT = "degree";
	public static final int ENV_COORD_STYLE = 0;
	public static final int ENV_ALGEBRA_STYLE_VAL = 0;
	public static final int ENV_ALGEBRA_STYLE_SPREADSHEET = 0;
	public static final boolean ENV_PATH_REGION_PARAMS = true;

	// cas
	public static final int ENV_CAS_TIMEOUT = 5;
	public static final boolean ENV_CAS_EXP_ROOTS = true;

	// algebraView
	public static final String ENV_AV_SORT_MODE = "type";
	public static final String ENV_AV_STYLE = "value";

	// tableView
	public static final double ENV_TABLE_MIN = -2.0;
	public static final double ENV_TABLE_MAX = 2.0;
	public static final double ENV_TABLE_STEP = 1.0;

	// evSettings
	public static final int ENV_RIGHT_ANGLE_STYLE = 1;
	public static final int ENV_CHECKBOX_SIZE = 26;
	public static final int ENV_GRID_TYPE = 3;
	public static final int ENV_POINT_CAPTURING = 3;
	public static final boolean ENV_AXES = true;
	public static final boolean ENV_GRID = false;
	public static final boolean ENV_GRID_IS_BOLD = false;
	public static final int ENV_POINT_STYLE = 0;
	public static final int ENV_TOOLTIPS = 0;
	public static final boolean ENV_MOUSE_COORDS = false;

	// ev lineStyle
	public static final int ENV_LINE_STYLE_AXES = 1;
	public static final int ENV_LINE_STYLE_GRID = 0;

	// ev labelStyle
	public static final boolean ENV_LABEL_SERIF = false;
	public static final int ENV_LABEL_FONT_STYLE = 0;

	// axis
	public static final boolean ENV_AXIS_SHOW = true;
	public static final boolean ENV_AXIS_SHOW_NUMBERS = true;
	public static final int ENV_AXIS_TICK_STYLE = 1;
	public static final boolean ENV_AXIS_POSITIVE = false;
	public static final boolean ENV_AXIS_SELECTION_ALLOWED = true;
	public static final boolean ENV_AXIS_DRAW_BORDER = false;

	// ev coordSystem defaults
	public static final double ENV_EV_SCALE = 50.0;
	public static final double ENV_EV_X_ZERO = 215;
	public static final double ENV_EV_Y_ZERO = 315;

	private GpadStyleDefaults() {
		// prevent instantiation
	}
}
