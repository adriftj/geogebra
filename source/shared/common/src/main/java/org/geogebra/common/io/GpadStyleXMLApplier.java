package org.geogebra.common.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.geogebra.common.gpad.GpadParseException;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.gpad.XMLToStyleMapParser;
import org.geogebra.common.kernel.ConstructionDefaults;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.Traceable;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.plugin.ScriptManager;
import org.geogebra.common.util.debug.Log;

/**
 * Helper class to apply Gpad style sheets to GeoElements using ConsElementXMLHandler.
 * This class is in the same package as ConsElementXMLHandler so it can access
 * the protected startGeoElement method.
 */
public class GpadStyleXMLApplier {
	/**
	 * Reset marker key used in attribute maps to indicate that a property should be reset to its default value.
	 * The value "~" is chosen because it cannot appear as a valid attribute name.
	 */
	private static final String RESET_MARKER = "~";
	
	/**
	 * Cache for default style maps by default type.
	 * Key: default type (Integer)
	 * Value: Map from tag names to default attribute maps
	 */
	private static final Map<Integer, Map<String, LinkedHashMap<String, String>>> DEFAULT_STYLE_MAP_CACHE = 
			new ConcurrentHashMap<>();
	
	/**
	 * 样式重置信息
	 * 用于定义每个 XML 标签在重置时的处理方式
	 */
	public static class ResetInfo {
		/**
		 * 重置类型
		 */
		public enum ResetType {
			/** 第二类：需要特殊处理（调用 handleDirectReset） */
			DIRECT,
			/** 第一类：使用缺省值 Map */
			DEFAULT_MAP
		}
		
		public final ResetType type;
		/** 如果是 DEFAULT_MAP，存储缺省属性 Map；如果是 DIRECT，为 null */
		public final LinkedHashMap<String, String> defaultAttrs;
		
		private ResetInfo(ResetType type, LinkedHashMap<String, String> defaultAttrs) {
			this.type = type;
			this.defaultAttrs = defaultAttrs;
		}
		
		public static ResetInfo direct() {
			return new ResetInfo(ResetType.DIRECT, null);
		}
		
		public static ResetInfo defaults(LinkedHashMap<String, String> attrs) {
			return new ResetInfo(ResetType.DEFAULT_MAP, attrs);
		}
	}
	
	// ==================== Static final 缺省值 Map ====================
	
	/** 布尔类型缺省值：val="false" */
	private static final LinkedHashMap<String, String> BOOL_FALSE = new LinkedHashMap<String, String>() {{
		put("val", "false");
	}};
	
	/** 布尔类型缺省值：val="true" */
	private static final LinkedHashMap<String, String> BOOL_TRUE = new LinkedHashMap<String, String>() {{
		put("val", "true");
	}};
	
	/** 整数/浮点数/字符串类型缺省值：val="0"（同时用于整数类型、浮点数类型和字符串类型中值为 "0" 的情况） */
	private static final LinkedHashMap<String, String> INT_ZERO = new LinkedHashMap<String, String>() {{
		put("val", "0");
	}};
	
	/** 整数类型缺省值：val="-1" */
	private static final LinkedHashMap<String, String> INT_M1 = new LinkedHashMap<String, String>() {{
		put("val", "-1");
	}};
	
	/** 字符串类型缺省值：val="" */
	private static final LinkedHashMap<String, String> STR_EMPTY = new LinkedHashMap<String, String>() {{
		put("val", "");
	}};
	
	/** 字符串类型缺省值：val="default" */
	private static final LinkedHashMap<String, String> STR_DEFAULT = new LinkedHashMap<String, String>() {{
		put("val", "default");
	}};
	
	/** 空 Map */
	private static final LinkedHashMap<String, String> EMPTY_MAP = new LinkedHashMap<>();
	
	// 布尔类型特殊属性名
	/** algebra: labelVisible="false" */
	private static final LinkedHashMap<String, String> ALGEBRA_FALSE = new LinkedHashMap<String, String>() {{
		put("labelVisible", "false");
	}};
	
	/** userinput: show="false" */
	private static final LinkedHashMap<String, String> USER_INPUT_FALSE = new LinkedHashMap<String, String>() {{
		put("show", "false");
	}};
	
	/** value: random="false" */
	private static final LinkedHashMap<String, String> RANDOM_FALSE = new LinkedHashMap<String, String>() {{
		put("random", "false");
	}};
	
	// 整数类型特殊缺省值
	/** arcSize: val="30" */
	private static final LinkedHashMap<String, String> ARC_SIZE_30 = new LinkedHashMap<String, String>() {{
		put("val", "30");
	}};
	
	/** length: val="20" */
	private static final LinkedHashMap<String, String> LENGTH_20 = new LinkedHashMap<String, String>() {{
		put("val", "20");
	}};
	
	/** slopeTriangleSize: val="1" */
	private static final LinkedHashMap<String, String> SLOPE_TRIANGLE_SIZE_1 = new LinkedHashMap<String, String>() {{
		put("val", "1");
	}};
	
	// 浮点数类型特殊缺省值
	/** ordering: val="NaN" */
	private static final LinkedHashMap<String, String> ORDERING_NAN = new LinkedHashMap<String, String>() {{
		put("val", "NaN");
	}};
	
	/** pointSize: val="5" */
	private static final LinkedHashMap<String, String> POINT_SIZE_5 = new LinkedHashMap<String, String>() {{
		put("val", "5");
	}};
	
	// 字符串类型特殊属性名（空值）
	/** audio/video: src="" */
	private static final LinkedHashMap<String, String> SRC_EMPTY = new LinkedHashMap<String, String>() {{
		put("src", "");
	}};
	
	/** curveParam: t="" */
	private static final LinkedHashMap<String, String> CURVE_PARAM_EMPTY = new LinkedHashMap<String, String>() {{
		put("t", "");
	}};
	
	/** file: name="" */
	private static final LinkedHashMap<String, String> FILE_EMPTY = new LinkedHashMap<String, String>() {{
		put("name", "");
	}};
	
	/** decoration: type="0" */
	private static final LinkedHashMap<String, String> DECORATION_0 = new LinkedHashMap<String, String>() {{
		put("type", "0");
	}};
	
	// 字符串类型特殊缺省值
	/** coordStyle: val="cartesian" */
	private static final LinkedHashMap<String, String> COORD_STYLE_CARTESIAN = new LinkedHashMap<String, String>() {{
		put("val", "cartesian");
	}};
	
	/** textAlign: val="left" */
	private static final LinkedHashMap<String, String> TEXT_ALIGN_LEFT = new LinkedHashMap<String, String>() {{
		put("val", "left");
	}};
	
	/** verticalAlign: val="top" */
	private static final LinkedHashMap<String, String> VERTICAL_ALIGN_TOP = new LinkedHashMap<String, String>() {{
		put("val", "top");
	}};
	
	// 复杂样式缺省值
	/** lineStyle: type="0", thickness="5" */
	private static final LinkedHashMap<String, String> LINE_STYLE_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("type", "0");
		put("thickness", "5");
	}};
	
	/** show: object="true", label="true", ev="0" */
	private static final LinkedHashMap<String, String> SHOW_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("object", "true");
		put("label", "true");
		put("ev", "0");
	}};
	
	/** tableview: column="-1", points="true" */
	private static final LinkedHashMap<String, String> TABLEVIEW_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("column", "-1");
		put("points", "true");
	}};
	
	/** animation: playing="false", type="0", step="", speed="" */
	private static final LinkedHashMap<String, String> ANIMATION_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("playing", "false");
		put("type", "0");
		put("step", "");
		put("speed", "");
	}};
	
	/** font: serif="false", sizeM="1.0", style="0" */
	private static final LinkedHashMap<String, String> FONT_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("serif", "false");
		put("sizeM", "1.0");
		put("style", "0");
	}};
	
	/** eqnStyle: style="implicit" */
	private static final LinkedHashMap<String, String> EQN_STYLE_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("style", "implicit");
	}};
	
	/** boundingBox: width="0", height="0" */
	private static final LinkedHashMap<String, String> BOUNDING_BOX_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("width", "0");
		put("height", "0");
	}};
	
	/** contentSize: width="800", height="600" */
	private static final LinkedHashMap<String, String> CONTENT_SIZE_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("width", "800");
		put("height", "600");
	}};
	
	/** cropBox: x="0", y="0", width="0", height="0" */
	private static final LinkedHashMap<String, String> CROP_BOX_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("x", "0");
		put("y", "0");
		put("width", "0");
		put("height", "0");
	}};
	
	/** dimensions: width="0", height="0", angle="0", unscaled="true" */
	private static final LinkedHashMap<String, String> DIMENSIONS_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("width", "0");
		put("height", "0");
		put("angle", "0");
		put("unscaled", "true");
	}};
	
	/** embed: id="-1" */
	private static final LinkedHashMap<String, String> EMBED_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("id", "-1");
	}};
	
	/** labelOffset: x="0", y="0" */
	private static final LinkedHashMap<String, String> LABEL_OFFSET_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("x", "0");
		put("y", "0");
	}};
	
	/** slider: width="200" (普通数字的缺省值，角度会在 getDefaultAttrsForTag 中特殊处理为 180) */
	private static final LinkedHashMap<String, String> SLIDER_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("width", "200");
	}};
	
	/** spreadsheetTrace: ... */
	private static final LinkedHashMap<String, String> SPREADSHEETTRACE_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("val", "false");
		put("traceColumn1", "-1");
		put("traceColumn2", "-1");
		put("traceRow1", "-1");
		put("traceRow2", "-1");
		put("tracingRow", "0");
		put("numRows", "10");
		put("headerOffset", "1");
		put("doColumnReset", "false");
		put("doRowLimit", "false");
		put("showLabel", "true");
		put("showTraceList", "false");
		put("doTraceGeoCopy", "false");
	}};
	
	/**
	 * 样式重置信息 Map
	 * Key: XML 标签名
	 * Value: ResetInfo 对象，定义重置时的处理方式
	 */
	private static final Map<String, ResetInfo> RESET_INFO_MAP = Map.ofEntries(
		// ==================== 第二类：需要特殊处理 ====================
		Map.entry("condition", ResetInfo.direct()),
		Map.entry("dynamicCaption", ResetInfo.direct()),
		Map.entry("javascript", ResetInfo.direct()),
		Map.entry("linkedGeo", ResetInfo.direct()),
		Map.entry("listener", ResetInfo.direct()),
		Map.entry("onChange", ResetInfo.direct()),
		Map.entry("onClick", ResetInfo.direct()),
		Map.entry("onDragEnd", ResetInfo.direct()),
		Map.entry("onUpdate", ResetInfo.direct()),
		Map.entry("startPoint", ResetInfo.direct()),
		Map.entry("trace", ResetInfo.direct()),
		
		// ==================== 第一类：布尔类型（缺省值 false）====================
		Map.entry("absoluteScreenLocation", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("algebra", ResetInfo.defaults(ALGEBRA_FALSE)),
		Map.entry("autocolor", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("auxiliary", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("breakpoint", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("centered", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("comboBox", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("contentSerif", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("emphasizeRightAngle", ResetInfo.defaults(BOOL_TRUE)),
		Map.entry("fixed", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("inBackground", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("interpolate", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("isLaTeX", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("isMask", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("keepTypeOnTransform", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("levelOfDetailQuality", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("outlyingIntersections", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("selectionAllowed", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("showOnAxis", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("showTrimmed", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("symbolic", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("userinput", ResetInfo.defaults(USER_INPUT_FALSE)),
		Map.entry("value", ResetInfo.defaults(RANDOM_FALSE)),
		
		// ==================== 第一类：整数类型 ====================
		Map.entry("arcSize", ResetInfo.defaults(ARC_SIZE_30)),
		Map.entry("decimals", ResetInfo.defaults(INT_M1)),
		Map.entry("layer", ResetInfo.defaults(INT_ZERO)),
		Map.entry("length", ResetInfo.defaults(LENGTH_20)),
		Map.entry("selectedIndex", ResetInfo.defaults(INT_ZERO)),
		Map.entry("significantfigures", ResetInfo.defaults(INT_M1)),
		Map.entry("slopeTriangleSize", ResetInfo.defaults(SLOPE_TRIANGLE_SIZE_1)),
		
		// ==================== 第一类：浮点数类型 ====================
		Map.entry("fading", ResetInfo.defaults(INT_ZERO)),
		Map.entry("ordering", ResetInfo.defaults(ORDERING_NAN)),
		Map.entry("pointSize", ResetInfo.defaults(POINT_SIZE_5)),
		
		// ==================== 第一类：字符串类型（GK_STR）====================
		Map.entry("angleStyle", ResetInfo.defaults(INT_ZERO)),
		Map.entry("audio", ResetInfo.defaults(SRC_EMPTY)),
		Map.entry("caption", ResetInfo.defaults(STR_EMPTY)),
		Map.entry("content", ResetInfo.defaults(STR_EMPTY)),
		Map.entry("coordStyle", ResetInfo.defaults(COORD_STYLE_CARTESIAN)),
		Map.entry("curveParam", ResetInfo.defaults(CURVE_PARAM_EMPTY)),
		Map.entry("decoration", ResetInfo.defaults(DECORATION_0)),
		Map.entry("endStyle", ResetInfo.defaults(STR_DEFAULT)),
		Map.entry("file", ResetInfo.defaults(FILE_EMPTY)),
		Map.entry("headStyle", ResetInfo.defaults(INT_ZERO)),
		Map.entry("incrementY", ResetInfo.defaults(STR_EMPTY)),
		Map.entry("labelMode", ResetInfo.defaults(INT_ZERO)),
		Map.entry("parentLabel", ResetInfo.defaults(STR_EMPTY)),
		Map.entry("pointStyle", ResetInfo.defaults(INT_ZERO)),
		Map.entry("startStyle", ResetInfo.defaults(STR_DEFAULT)),
		Map.entry("textAlign", ResetInfo.defaults(TEXT_ALIGN_LEFT)),
		Map.entry("tooltipMode", ResetInfo.defaults(INT_ZERO)),
		Map.entry("verticalAlign", ResetInfo.defaults(VERTICAL_ALIGN_TOP)),
		Map.entry("video", ResetInfo.defaults(SRC_EMPTY)),
		
		// ==================== 第一类：复杂样式（多个属性）====================
		Map.entry("animation", ResetInfo.defaults(ANIMATION_DEFAULTS)),
		Map.entry("boundingBox", ResetInfo.defaults(BOUNDING_BOX_DEFAULTS)),
		Map.entry("contentSize", ResetInfo.defaults(CONTENT_SIZE_DEFAULTS)),
		Map.entry("cropBox", ResetInfo.defaults(CROP_BOX_DEFAULTS)),
		Map.entry("dimensions", ResetInfo.defaults(DIMENSIONS_DEFAULTS)),
		Map.entry("embed", ResetInfo.defaults(EMBED_DEFAULTS)),
		Map.entry("eqnStyle", ResetInfo.defaults(EQN_STYLE_DEFAULTS)),
		Map.entry("font", ResetInfo.defaults(FONT_DEFAULTS)),
		Map.entry("labelOffset", ResetInfo.defaults(LABEL_OFFSET_DEFAULTS)),
		Map.entry("lineStyle", ResetInfo.defaults(LINE_STYLE_DEFAULTS)),
		Map.entry("show", ResetInfo.defaults(SHOW_DEFAULTS)),
		Map.entry("slider", ResetInfo.defaults(SLIDER_DEFAULTS)), // width 会在 getDefaultAttrsForTag 中根据对象类型特殊处理
		Map.entry("spreadsheetTrace", ResetInfo.defaults(SPREADSHEETTRACE_DEFAULTS)),
		Map.entry("tableview", ResetInfo.defaults(TABLEVIEW_DEFAULTS)),
		
		// 其他复杂样式（暂时使用空 Map，表示不需要重置或使用默认值）
		Map.entry("allowReflexAngle", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("casMap", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("checkbox", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("coefficients", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("coords", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("eigenvectors", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("embedSettings", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("forceReflexAngle", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("listType", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("matrix", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("parent", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("strokeCoords", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("tag", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("tempUserInput", ResetInfo.defaults(EMPTY_MAP)),
		Map.entry("variables", ResetInfo.defaults(EMPTY_MAP))
	);
	
	/**
	 * Map of required attributes that will throw exceptions if missing.
	 * Key: element name (e.g., "value", "slider")
	 * Value: Set of attribute names that are required (will throw exception if missing)
	 * 
	 * This map only includes attributes that will cause exceptions when parsed by
	 * ConsElementXMLHandler. Attributes that are optional (like show's object/label/ev)
	 * are not included here.
	 */
	private static final Map<String, Set<String>> REQUIRED_ATTRIBUTE_NAMES = Map.ofEntries(
			// value element: val attribute is required (throws exception when parsing)
			Map.entry("value", java.util.Set.of("val")), // gpad style "random" require this default value
			
			// slider element: width is required (throws exception in setSliderWidth)
			Map.entry("slider", java.util.Set.of("width")),
			
			// cropBox element: x, y, width, height are required
			Map.entry("cropBox", java.util.Set.of("x", "y", "width", "height")),
			
			// video element: width and height are required
			Map.entry("video", java.util.Set.of("width", "height")),
			
			// spreadsheetTrace element: many attributes are required
			Map.entry("spreadsheetTrace", java.util.Set.of(
					"val", "traceColumn1", "traceColumn2", "traceRow1", "traceRow2",
					"tracingRow", "numRows", "headerOffset", "doColumnReset", "doRowLimit",
					"showLabel", "showTraceList", "doTraceGeoCopy")),
			
			// lineStyle element: type and thickness are required
			Map.entry("lineStyle", java.util.Set.of("type", "thickness")),
			
			// boundingBox element: width and height are required
			Map.entry("boundingBox", java.util.Set.of("width", "height")),
			
			// embed element: id is required
			Map.entry("embed", java.util.Set.of("id")),
			
			// tag element (for chart styles): key, value, barNumber are required
			Map.entry("tag", java.util.Set.of("key", "value", "barNumber")),
			
			// contentSize element: width and height are required
			Map.entry("contentSize", java.util.Set.of("width", "height")),
			
			// tableview element: column is required (to avoid null pointer exception in parseDoubleNaN)
			// points is optional (default is true, but if not provided, parseBoolean(null) returns false)
			Map.entry("tableview", java.util.Set.of("column", "points"))
	);
	
	/**
	 * Fills missing required attributes with default values from defaultGeo.
	 * Only fills attributes that will throw exceptions if missing.
	 * 
	 * @param tagName
	 *            XML element name
	 * @param attrs
	 *            attributes map to fill
	 * @param geo
	 *            GeoElement (needed for type-specific defaults)
	 */
	private static void fillRequiredAttributes(String tagName,
			LinkedHashMap<String, String> attrs, GeoElement geo) {
		// Get the set of required attribute names for this tag
		Set<String> requiredAttrNames = REQUIRED_ATTRIBUTE_NAMES.get(tagName);
		
		// If this tag has no required attributes (like "show"), skip filling
		if (requiredAttrNames == null || requiredAttrNames.isEmpty())
			return;
		
		// Get default attributes for this tag
		LinkedHashMap<String, String> defaultAttrs = getDefaultAttrsForTag(geo, tagName);
		
		// Fill only the required attributes that are missing
		for (String attrName : requiredAttrNames) {
			if (!attrs.containsKey(attrName) || attrs.get(attrName) == null) {
				String defaultValue = null;

				// Special handling for value element's val attribute:
				// Use current geo's value
				if ("value".equals(tagName) && "val".equals(attrName))
					defaultValue = getDefaultValueForValueElement(geo);
				else if ("javascript".equals(tagName) && "val".equals(attrName))
					defaultValue = ""; // for javascript element's val, use ""
				else {
					// Try to get default value from default geo
					if (defaultAttrs != null && defaultAttrs.containsKey(attrName))
						defaultValue = defaultAttrs.get(attrName);
				}

				if (defaultValue != null)
					attrs.put(attrName, defaultValue);
			}
		}
	}
	
	/**
	 * Gets the default value for the value element's val attribute based on
	 * the GeoElement type.
	 * 
	 * @param geo GeoElement
	 * @return default value string
	 */
	private static String getDefaultValueForValueElement(GeoElement geo) {
		if (geo instanceof GeoNumeric) {
			GeoNumeric num = (GeoNumeric) geo;
			// Use current value if available, otherwise use 0
			return num.isDefined() ? String.valueOf(num.getValue()) : "0";
		} else if (geo instanceof GeoBoolean) {
			GeoBoolean bool = (GeoBoolean) geo;
			// Use current value if available, otherwise use false
			return bool.isDefined() ? String.valueOf(bool.getBoolean()) : "false";
		} else if (geo instanceof GeoButton) {
			// For buttons, use empty string for script
			return "";
		}
		// Fallback
		return "0";
	}
	
	/**
	 * Checks if the given attributes map contains a reset marker.
	 * 
	 * @param attrs attributes map
	 * @return true if the map contains the reset marker
	 */
	private static boolean isResetMarker(LinkedHashMap<String, String> attrs) {
		return attrs != null && attrs.containsKey(RESET_MARKER);
	}
	
	/**
	 * Gets default attributes for a specific tag.
	 * 
	 * @param geo GeoElement to get default type for
	 * @param tagName XML tag name
	 * @return default attributes map, or null if not found
	 */
	private static LinkedHashMap<String, String> getDefaultAttrsForTag(GeoElement geo, String tagName) {
		// 首先检查 RESET_INFO_MAP
		ResetInfo resetInfo = RESET_INFO_MAP.get(tagName);
		if (resetInfo == null)
			return null;

		if (resetInfo.type == ResetInfo.ResetType.DIRECT) // 第二类：需要特殊处理，返回 null（由 handleDirectReset 处理）
			return null;
			
		// 第一类：使用缺省值 Map
		LinkedHashMap<String, String> defaultAttrs = resetInfo.defaultAttrs;
		if (defaultAttrs == null)
			return null;

		// 特殊处理：slider 的 width 需要根据对象类型动态确定
		if ("slider".equals(tagName)) {
			// 根据 geo 类型确定 width 缺省值
			// 如果是 GeoAngle，使用 180；否则使用defaultAttrs里的(200)
			if (geo instanceof GeoAngle) {
				LinkedHashMap<String, String> result = new LinkedHashMap<>(defaultAttrs);
				result.put("width", "180");
				return result;
			}
		}
		
		// 返回缺省值的副本，避免修改原始 Map
		return new LinkedHashMap<>(defaultAttrs);
	}
	
	/**
	 * Applies a style sheet to a GeoElement using ConsElementXMLHandler.
	 * 
	 * @param geo GeoElement to apply styles to
	 * @param styleSheet style sheet to apply
	 * @throws GpadParseException if application fails
	 */
	public static void apply(GpadStyleSheet styleSheet, GeoElement geo) throws GpadParseException {
		if (geo == null || styleSheet == null)
			return;

		Kernel kernel = geo.getKernel();
		MyXMLHandler myXMLHandler = kernel.newMyXMLHandler(kernel.getConstruction());
		ConsElementXMLHandler xmlHandler = new ConsElementXMLHandler(myXMLHandler, kernel.getApplication());

		// Initialize the handler with the geo element using initForGpad
		// This avoids setting default values that would interfere with partial style updates
		xmlHandler.initForGpad(geo);
		
		Map<String, LinkedHashMap<String, String>> properties = styleSheet.getProperties();
		
		// Track if spreadsheetTrace tag is processed - need to sync TraceManager afterwards
		boolean hasSpreadsheetTraceTag = properties.containsKey("spreadsheetTrace");

		// Apply each property using ConsElementXMLHandler
		// We can access startGeoElement because we're in the same package
		// Use myXMLHandler.errors directly, same as evalXML does
		// This ensures all errors (from handleValue and addError) go to the same list
		
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : properties.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> originalAttrs = entry.getValue();
			
			if (originalAttrs == null)
				continue;
			
			// Create a copy of attrs to avoid modifying the original styleSheet
			LinkedHashMap<String, String> attrs = new LinkedHashMap<>(originalAttrs);
			
			// Special handling for startPoint: deserialize _corners and apply immediately
			if ("startPoint".equals(tagName) && attrs.containsKey("_corners")) {
				String serialized = attrs.get("_corners");
				deserializeAndApplyStartPointCorners(serialized, xmlHandler, myXMLHandler.errors);
				continue;
			}
			
			// Check if this is a reset marker (key: "~")
			// Reset marker can coexist with normal attributes: {"~": "", "type": "1", "thickness": "5"}
			// This means: first clear, then set the normal attributes
			boolean hasResetMarker = isResetMarker(attrs);
			if (hasResetMarker) {
				// Check if this is a second-class tag (DIRECT) or first-class tag (DEFAULT_MAP)
				ResetInfo resetInfo = RESET_INFO_MAP.get(tagName);
				
				if (resetInfo != null && resetInfo.type == ResetInfo.ResetType.DIRECT) {
					// 第二类：需要特殊处理，调用 handleDirectReset
					if (handleDirectReset(tagName, attrs, geo)) {
						// Direct reset handled, but we still need to apply normal attributes if any
						// Remove reset marker from attrs copy for further processing
						LinkedHashMap<String, String> attrsWithoutReset = new LinkedHashMap<>(attrs);
						attrsWithoutReset.remove(RESET_MARKER);
						if (!attrsWithoutReset.isEmpty()) {
							// There are normal attributes to apply after clearing
							fillRequiredAttributes(tagName, attrsWithoutReset, geo);
							xmlHandler.startGeoElement(tagName, attrsWithoutReset, myXMLHandler.errors);
						}
						continue; // Skip default-based reset since we already cleared directly
					}
					// If handleDirectReset returned false, fall through to default handling
				}
				
				// 第一类：使用缺省值 Map
				// Remove reset marker to get normal attributes (if any)
				LinkedHashMap<String, String> normalAttrs = new LinkedHashMap<>(attrs);
				normalAttrs.remove(RESET_MARKER);
				
				// Get default attributes for this tag
				LinkedHashMap<String, String> defaultAttrs = getDefaultAttrsForTag(geo, tagName);
				
				if (defaultAttrs != null) {
					if (normalAttrs.isEmpty()) {
						// 没有其他值要设置，直接用缺省值
						attrs = defaultAttrs;
					} else {
						// 有其他值要设置，合并新设的值到缺省值中（新值覆盖缺省值）
						LinkedHashMap<String, String> mergedAttrs = new LinkedHashMap<>(defaultAttrs);
						mergedAttrs.putAll(normalAttrs);
						attrs = mergedAttrs;
					}
				} else {
					// No default value found, but we still need to apply normal attributes if any
					if (normalAttrs.isEmpty()) {
						// No normal attributes and no default, skip this tag
						continue;
					}
					attrs = normalAttrs;
				}
			}
			
			// Fill missing required attributes before applying
			fillRequiredAttributes(tagName, attrs, geo);
			xmlHandler.startGeoElement(tagName, attrs, myXMLHandler.errors);
		}

		xmlHandler.processLists(); // Process deferred lists (e.g., minMaxList for slider min/max)
		xmlHandler.finish(); // Finish processing (won't set defaults in Gpad mode)
		
		// Update construction - this will internally call notifyRepaint() at the end
		// This mitigates the risk of missing updateConstruction() and notifyRepaint()
		kernel.updateConstruction(false);
		
		// If spreadsheetTrace tag was processed, sync TraceManager AFTER updateConstruction
		// This mitigates the risk of TraceManager being out of sync with GeoElement's trace settings
		// The geo's trace state may have changed (enabled or disabled), so we need to reload
		// the trace collection to ensure TraceManager reflects the current state
		// Same order as evalXML: updateConstruction() first, then loadTraceGeoCollection()
		// loadTraceGeoCollection() will call repaintSpreadsheet(), so we don't need additional repaint
		if (hasSpreadsheetTraceTag) {
			// loadTraceGeoCollection() will scan all geos and only include those with trace enabled
			// This ensures TraceManager is in sync even if trace was disabled for this geo
			// Called after updateConstruction() to avoid spurious traces (same as evalXML)
			kernel.getApplication().getTraceManager().loadTraceGeoCollection();
		}

		if (!myXMLHandler.errors.isEmpty())
			throw new GpadParseException("Failed to apply style sheet: " + myXMLHandler.errors.toString());
	}
	
	/**
	 * Handles direct reset for tags that can be cleared by calling geo methods directly.
	 * This is more efficient than going through XML handler for simple clear operations.
	 * 
	 * @param tagName XML tag name
	 * @param attrs attributes map (may contain type for listener)
	 * @param geo GeoElement to reset
	 * @return true if the reset was handled directly, false if should use default XML approach
	 */
	private static boolean handleDirectReset(String tagName, LinkedHashMap<String, String> attrs, GeoElement geo) {
		try {
			switch (tagName) {
			case "condition":
				// Clear show object condition
				geo.setShowObjectCondition(null);
				return true;
				
			case "dynamicCaption":
				// Clear dynamic caption
				geo.removeDynamicCaption();
				return true;
				
			case "trace":
				// Clear trace flag
				if (geo instanceof Traceable) {
					((Traceable) geo).setTrace(false);
					return true;
				}
				return false;
				
			case "javascript":
			case "onClick":
				// Clear click script
				geo.setScript(null, EventType.CLICK);
				return true;
				
			case "onUpdate":
				// Clear update script
				geo.setScript(null, EventType.UPDATE);
				return true;
				
			case "onDragEnd":
				// Clear drag end script
				geo.setScript(null, EventType.DRAG_END);
				return true;
				
			case "onChange":
				// Clear change script
				geo.setScript(null, EventType.EDITOR_KEY_TYPED);
				return true;
				
			case "listener":
				// Clear listener - needs type attribute
				String type = attrs != null ? attrs.get("type") : null;
				if (type != null) {
					ScriptManager scriptManager = geo.getKernel().getApplication().getScriptManager();
					if ("objectUpdate".equals(type)) {
						scriptManager.getUpdateListenerMap().remove(geo);
						return true;
					} else if ("objectClick".equals(type)) {
						scriptManager.getClickListenerMap().remove(geo);
						return true;
					}
				}
				return false;
				
			default:
				return false;
			}
		} catch (Exception e) {
			Log.debug("Failed to directly reset " + tagName + ": " + e.getMessage());
			return false; // Fall back to XML approach on error
		}
	}
	
	/**
	 * Deserializes startPoint corners from serialized string and applies them immediately.
	 * Format: corner1\u0001corner2\u0001...
	 * Each corner: [absolute byte][type byte][content]
	 *   - absolute byte: \u0002=true, \u0003=false
	 *   - type byte: \u0002=exp, \u0003=x/y/z
	 *   - content: if exp, then exp string; if x/y/z, then "x,y,z" (z optional, no trailing comma)
	 * 
	 * @param serialized serialized string
	 * @param xmlHandler XML handler to apply corners
	 * @param errors error list
	 */
	private static void deserializeAndApplyStartPointCorners(String serialized,
			ConsElementXMLHandler xmlHandler, ArrayList<String> errors) {
		if (serialized == null || serialized.isEmpty())
			return;
		
		// Split by SOH (\u0001) to get individual corners
		String[] cornerStrings = serialized.split("\u0001", -1);
		int cornerIndex = 0;
		for (String cornerStr : cornerStrings) {
			if (cornerStr.isEmpty())
				continue;
			
			LinkedHashMap<String, String> corner = new LinkedHashMap<>();
			
			// Parse corner format: [absolute byte][type byte][content]
			if (cornerStr.length() < 2)
				continue; // Invalid format
			
			// (1) Parse absolute byte
			char absoluteByte = cornerStr.charAt(0);
			boolean isAbsolute = (absoluteByte == '\u0002');
			if (isAbsolute)
				corner.put("absolute", "true");
			
			// (2) Parse type byte
			char typeByte = cornerStr.charAt(1);
			boolean isExp = (typeByte == '\u0002');
			
			// (3) Parse content
			String content = cornerStr.substring(2);
			if (isExp) // exp type: content is the expression
				corner.put("exp", content);
			else {
				// x/y/z type: content is "x,y,z" or "x,y" (z optional)
				String[] coords = content.split(",", -1);
				if (coords.length >= 2) {
					corner.put("x", coords[0]);
					corner.put("y", coords[1]);
					if (coords.length >= 3 && !coords[2].isEmpty())
						corner.put("z", coords[2]);
				}
			}
			
			// Set number attribute based on position (0, 1, 2, ...)
			// Note: number is needed for XML handler to identify corner index
			corner.put("number", String.valueOf(cornerIndex++));
			
			// Apply immediately (no need to fill required attributes, startPoint has none)
			xmlHandler.startGeoElement("startPoint", corner, errors);
		}
	}
}
