package org.geogebra.common.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;

import org.geogebra.common.gpad.GpadParseException;
import org.geogebra.common.gpad.GpadSerializer;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoInputBox;
import org.geogebra.common.kernel.geos.GeoNumeric;
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
	 * 样式重置信息
	 * 用于定义每个 XML 标签在重置时的处理方式
	 */
	public static class ResetInfo {
		public final LinkedHashMap<String, String> defaultAttrs;
		
		private ResetInfo(LinkedHashMap<String, String> defaultAttrs) {
			this.defaultAttrs = defaultAttrs;
		}
		
		public static ResetInfo defaults(LinkedHashMap<String, String> attrs) {
			return new ResetInfo(attrs);
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
	
	// 布尔类型特殊属性名
	/** algebra: labelVisible="false" */
	private static final LinkedHashMap<String, String> ALGEBRA_FALSE = new LinkedHashMap<String, String>() {{
		put("labelVisible", "false");
	}};

	/** checkbox: fixed="false" */
	private static final LinkedHashMap<String, String> CHECKBOX_FIXED_FALSE = new LinkedHashMap<String, String>() {{
		put("fixed", "false");
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
	
	/** linkedGeo: exp="" */
	private static final LinkedHashMap<String, String> LINKEDGEO_EXP_EMPTY = new LinkedHashMap<String, String>() {{
		put("exp", "");
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

	/** bgColor/borderColor/objColor: #000000 */
	private static final LinkedHashMap<String, String> COLOR_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("r", "0");
		put("g", "0");
		put("b", "0");
	}};

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
	
	/** slider: width="200" (普通数字的缺省值，角度的缺省值需要特殊处理为 180) */
	private static final LinkedHashMap<String, String> SLIDER_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("width", "200");
	}};

	/** slider: width="180" (角度的缺省值) */
	private static final LinkedHashMap<String, String> SLIDER_ANGLE_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("width", "180");
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

	/** startPoint: _corners="..." */
	private static final LinkedHashMap<String, String> STARTPOINT_DEFAULTS = new LinkedHashMap<String, String>() {{
		put("_corners", "\u0002\u00030,0");
	}};
	
	/**
	 * 样式重置信息 Map
	 * Key: XML 标签名
	 * Value: ResetInfo 对象，定义重置时的处理方式
	 */
	private static final Map<String, ResetInfo> RESET_INFO_MAP = Map.ofEntries(
		// ==================== 调用handleDirectReset直接清除的 ====================
		// "condition":
		// "dynamicCaption":
		// "javascript":
		// "onClick":
		// "onUpdate":
		// "onDragEnd":
		// "onChange":
		// "listener":
		// "tempUserInput":
		
		// ==================== 布尔类型（缺省值 false）====================
		Map.entry("absoluteScreenLocation", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("algebra", ResetInfo.defaults(ALGEBRA_FALSE)),
		Map.entry("autocolor", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("auxiliary", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("breakpoint", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("centered", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("checkbox", ResetInfo.defaults(CHECKBOX_FIXED_FALSE)),
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
		Map.entry("trace", ResetInfo.defaults(BOOL_FALSE)),
		Map.entry("userinput", ResetInfo.defaults(USER_INPUT_FALSE)),
		Map.entry("value", ResetInfo.defaults(RANDOM_FALSE)),
		
		// ==================== 整数类型 ====================
		Map.entry("arcSize", ResetInfo.defaults(ARC_SIZE_30)),
		Map.entry("decimals", ResetInfo.defaults(INT_M1)),
		Map.entry("layer", ResetInfo.defaults(INT_ZERO)),
		Map.entry("length", ResetInfo.defaults(LENGTH_20)),
		Map.entry("selectedIndex", ResetInfo.defaults(INT_ZERO)),
		Map.entry("significantfigures", ResetInfo.defaults(INT_M1)),
		Map.entry("slopeTriangleSize", ResetInfo.defaults(SLOPE_TRIANGLE_SIZE_1)),
		
		// ==================== 浮点数类型 ====================
		Map.entry("fading", ResetInfo.defaults(INT_ZERO)),
		Map.entry("ordering", ResetInfo.defaults(ORDERING_NAN)),
		Map.entry("pointSize", ResetInfo.defaults(POINT_SIZE_5)),
		
		// ==================== 字符串类型（GK_STR）====================
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
		Map.entry("linkedGeo", ResetInfo.defaults(LINKEDGEO_EXP_EMPTY)),
		Map.entry("parentLabel", ResetInfo.defaults(STR_EMPTY)),
		Map.entry("pointStyle", ResetInfo.defaults(INT_ZERO)),
		Map.entry("startStyle", ResetInfo.defaults(STR_DEFAULT)),
		Map.entry("textAlign", ResetInfo.defaults(TEXT_ALIGN_LEFT)),
		Map.entry("tooltipMode", ResetInfo.defaults(INT_ZERO)),
		Map.entry("verticalAlign", ResetInfo.defaults(VERTICAL_ALIGN_TOP)),
		Map.entry("video", ResetInfo.defaults(SRC_EMPTY)),
		
		// ==================== 复杂样式（多个属性）====================
		Map.entry("animation", ResetInfo.defaults(ANIMATION_DEFAULTS)),
		Map.entry("bgColor", ResetInfo.defaults(COLOR_DEFAULTS)),
		Map.entry("borderColor", ResetInfo.defaults(COLOR_DEFAULTS)),
		Map.entry("boundingBox", ResetInfo.defaults(BOUNDING_BOX_DEFAULTS)),
		Map.entry("contentSize", ResetInfo.defaults(CONTENT_SIZE_DEFAULTS)),
		Map.entry("cropBox", ResetInfo.defaults(CROP_BOX_DEFAULTS)),
		Map.entry("dimensions", ResetInfo.defaults(DIMENSIONS_DEFAULTS)),
		Map.entry("embed", ResetInfo.defaults(EMBED_DEFAULTS)),
		Map.entry("eqnStyle", ResetInfo.defaults(EQN_STYLE_DEFAULTS)),
		Map.entry("font", ResetInfo.defaults(FONT_DEFAULTS)),
		Map.entry("labelOffset", ResetInfo.defaults(LABEL_OFFSET_DEFAULTS)),
		Map.entry("lineStyle", ResetInfo.defaults(LINE_STYLE_DEFAULTS)),
		Map.entry("objColor", ResetInfo.defaults(COLOR_DEFAULTS)),
		Map.entry("show", ResetInfo.defaults(SHOW_DEFAULTS)),
		Map.entry("slider", ResetInfo.defaults(SLIDER_DEFAULTS)), // width需要根据对象类型特殊处理
		Map.entry("spreadsheetTrace", ResetInfo.defaults(SPREADSHEETTRACE_DEFAULTS)),
		Map.entry("startPoint", ResetInfo.defaults(STARTPOINT_DEFAULTS)),
		Map.entry("tableview", ResetInfo.defaults(TABLEVIEW_DEFAULTS))
		
		// 其他复杂样式，不登记，所以查map时会得到null，表示不需要重置或使用默认值
		// "allowReflexAngle" // 已废弃
		// "casMap" // 是个缓存，不需要作为样式
		// "coefficients"
		// "coords"
		// "eigenvectors"
		// "embedSettings" // 不需要重置
		// "forceReflexAngle" // 已废弃
		// "listType" // 由命令自动生成，不需要作为样式
		// "matrix"
		// "parent"
		// "strokeCoords" // 由命令自动生成，不需要作为样式
		// "tag"
		// "variables" // 由命令自动生成，不需要作为样式
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
	 * @param attrs attributes map to fill
	 * @param canModify false if need a new copy of adjusted map, true if can modify `attrs`
	 * @param tagName XML element name
	 * @param geo GeoElement (needed for type-specific defaults)
	 * @return filled map
	 * 
	 */
	private static LinkedHashMap<String, String> fillRequiredAttributes(
		LinkedHashMap<String, String> attrs, boolean canModify,
		String tagName, GeoElement geo) {
		// Get the set of required attribute names for this tag
		Set<String> requiredAttrNames = REQUIRED_ATTRIBUTE_NAMES.get(tagName);
		// If this tag has no required attributes (like "show"), skip filling
		if (requiredAttrNames == null || requiredAttrNames.isEmpty())
			return attrs;

		LinkedHashMap<String, String> originAttrs = attrs;
		ResetInfo resetInfo = RESET_INFO_MAP.get(tagName);
		LinkedHashMap<String, String> defaultAttrs =
			resetInfo == null? null: resetInfo.defaultAttrs;
		Optional<LinkedHashMap<String, String>> optDefaultAttrs = needAdjustDefault(tagName, geo);
		if(optDefaultAttrs.isPresent()) {
			defaultAttrs = new LinkedHashMap<String, String>(defaultAttrs);
			defaultAttrs.putAll(optDefaultAttrs.get());
		}
		
		// Fill only the required attributes that are missing
		for (String attrName : requiredAttrNames) {
			if (!attrs.containsKey(attrName) || attrs.get(attrName) == null) {
				String defaultValue = null;

				if ("value".equals(tagName)) { // Use current geo's value for default
					if("val".equals(attrName))
						defaultValue = getDefaultValueForValueElement(geo);
				} else if ("javascript".equals(tagName)) {
					if ("val".equals(attrName))
						defaultValue = ""; // for javascript element's val, use ""
				} else if (defaultAttrs != null) {
					defaultValue = defaultAttrs.get(attrName);
				} else
					continue;

				if (defaultValue != null) {
					if (!canModify && attrs == originAttrs)
						attrs = new LinkedHashMap<String, String>(attrs);
					attrs.put(attrName, defaultValue);
				}
			}
		}
		return attrs;
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
	 * Check if need adjust default attributes for a specific tag.
	 * 
	 * @param tagName XML tag name
	 * @param geo GeoElement to get default type for
	 * @return optional map to adjust
	 */
	private static Optional<LinkedHashMap<String, String>> needAdjustDefault(String tagName, GeoElement geo) {
		// 特殊处理：slider 的 width 需要根据对象类型动态确定
		if ("slider".equals(tagName)) {
			// 根据 geo 类型确定 width 缺省值
			// 如果是 GeoAngle，改成180；否则用原来的缺省值
			if (geo instanceof GeoAngle)
				return Optional.of(SLIDER_ANGLE_DEFAULTS);
		}
		
		return Optional.empty();
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
			LinkedHashMap<String, String> attrs = entry.getValue();
			if (attrs == null)
				continue;

			// Check if this is a reset marker (key: "~")
			// Reset marker can coexist with normal attributes: {"~": "", "type": "1", "thickness": "5"}
			// This means: first clear, then set the normal attributes
			boolean hasResetMarker = isResetMarker(attrs);
			if (hasResetMarker) {
				if (handleDirectReset(tagName, attrs, geo)) {
					if (attrs.size()<=1) // no any normal attributes
						continue;
					// Remove reset marker from attrs copy
					attrs = new LinkedHashMap<>(attrs);
					attrs.remove(RESET_MARKER);
					// There are normal attributes to apply after clearing
					attrs = fillRequiredAttributes(attrs, true, tagName, geo);
				}
				else {
					ResetInfo resetInfo = RESET_INFO_MAP.get(tagName);
					if (attrs.size()<=1) { // no normal attributes
						if (resetInfo == null) // no default
							continue;
						attrs = resetInfo.defaultAttrs;
					}
					else {
						if(resetInfo != null) {
							// 有其他值要设置，合并新设的值到缺省值中（新值覆盖缺省值）
							LinkedHashMap<String, String> mergedAttrs = new LinkedHashMap<>(resetInfo.defaultAttrs);
							needAdjustDefault(tagName, geo).ifPresent(
					            dAttrs -> mergedAttrs.putAll(dAttrs)
        					);
							mergedAttrs.putAll(attrs);
							attrs = mergedAttrs;
						} else // No default, has normal attributes
							attrs = new LinkedHashMap<>(attrs);
						// Remove reset marker to get normal attributes
						attrs.remove(RESET_MARKER);
					}
				}
			}
			else // Fill missing required attributes before applying
				attrs = fillRequiredAttributes(attrs, false, tagName, geo);

			// Special handling for startPoint: deserialize _corners and apply immediately
			if ("startPoint".equals(tagName)) {
				if(attrs.containsKey("_corners")) {
					String serialized = attrs.get("_corners");
					deserializeAndApplyStartPointCorners(serialized, xmlHandler, myXMLHandler.errors);
				}
			}
			// Special handling for barTag: deserialize _barTags and apply immediately
			else if ("barTag".equals(tagName)) {
				if(attrs.containsKey("_barTags")) {
					String serialized = attrs.get("_barTags");
					deserializeAndApplyBarTags(serialized, xmlHandler, myXMLHandler.errors);
				}
			}
			// Special handling for coords: map x,y,z,w to ox,oy,oz,ow for 3D objects
			else {
				if ("coords".equals(tagName))
					attrs = mapCoordsFor3DObjects(geo, attrs);
				xmlHandler.startGeoElement(tagName, attrs, myXMLHandler.errors);
			}
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
					if ("objectUpdate".equals(type))
						scriptManager.getUpdateListenerMap().remove(geo);
					else if ("objectClick".equals(type))
						scriptManager.getClickListenerMap().remove(geo);
				}
				return true;
				
			case "tempUserInput":
				// Clear temp user input
				if (geo instanceof GeoInputBox)
					((GeoInputBox) geo).clearTempUserInput();
				return true;
				
			default:
				return false;
			}
		} catch (Exception e) {
			Log.debug("Failed to directly reset " + tagName + ": " + e.getMessage());
			return true;
		}
	}
	
	/**
	 * Deserializes startPoint corners from serialized string and applies them immediately.
	 * Uses GpadSerializer.deserializeStartPointCorners() for deserialization.
	 * 
	 * @param serialized serialized string
	 * @param xmlHandler XML handler to apply corners
	 * @param errors error list
	 */
	private static void deserializeAndApplyStartPointCorners(String serialized,
			ConsElementXMLHandler xmlHandler, ArrayList<String> errors) {
		if (serialized == null || serialized.isEmpty())
			return;
		
		// Use array to allow modification in lambda
		int[] cornerIndex = {0};
		
		// Deserialize using helper class
		GpadSerializer.deserializeStartPointCorners(serialized, (firstCorner, isAbsolute, cornerData) -> {
			LinkedHashMap<String, String> corner = new LinkedHashMap<>();
			
			// Set absolute attribute if true
			if (isAbsolute)
				corner.put("absolute", "true");
			
			// Set exp or x/y/z attributes
			String exp = cornerData[0];
			if (exp != null) {
				// Expression-based corner
				corner.put("exp", exp);
			} else {
				// x/y/z type
				String x = cornerData[1];
				String y = cornerData[2];
				String z = cornerData[3];
				if (x != null && y != null) {
					corner.put("x", x);
					corner.put("y", y);
					if (z != null)
						corner.put("z", z);
				}
			}
			
			// Set number attribute based on position (0, 1, 2, ...)
			// Note: number is needed for XML handler to identify corner index
			corner.put("number", String.valueOf(cornerIndex[0]++));
			
			// Apply immediately (no need to fill required attributes, startPoint has none)
			xmlHandler.startGeoElement("startPoint", corner, errors);
		});
	}
	
	/**
	 * Maps coords attributes from GPAD format (x, y, z, w) to 3D object format (ox, oy, oz, ow).
	 * For 3D objects like GeoConic3D and GeoLine3D, the origin coordinates use ox/oy/oz/ow instead of x/y/z/w.
	 * 
	 * @param geo GeoElement to check type
	 * @param attrs coords attributes from GPAD (may contain x, y, z, w)
	 * @return mapped attributes (ox, oy, oz, ow for 3D objects, or original attrs for 2D objects)
	 */
	private static LinkedHashMap<String, String> mapCoordsFor3DObjects(GeoElement geo, LinkedHashMap<String, String> attrs) {
		if (attrs == null)
			return attrs;
		
		// Check if this is a 3D object that uses ox/oy/oz/ow
		boolean needsMapping = geo instanceof org.geogebra.common.geogebra3D.kernel3D.geos.GeoConic3D
				|| geo instanceof org.geogebra.common.geogebra3D.kernel3D.geos.GeoLine3D;
		
		if (!needsMapping)
			return attrs; // 2D objects use x, y, z directly
		
		// Check if mapping is needed (if ox already exists, no need to map)
		if (attrs.containsKey("ox"))
			return attrs; // Already in 3D format
		
		// Map x, y, z, w to ox, oy, oz, ow
		LinkedHashMap<String, String> mapped = new LinkedHashMap<>(attrs);
		String x = mapped.remove("x");
		String y = mapped.remove("y");
		String z = mapped.remove("z");
		String w = mapped.remove("w");
		
		if (x != null) mapped.put("ox", x);
		if (y != null) mapped.put("oy", y);
		if (z != null) mapped.put("oz", z);
		if (w != null) mapped.put("ow", w);
		
		return mapped;
	}
	
	/**
	 * Deserializes barTag bars from serialized string and applies them immediately.
	 * Uses GpadSerializer.deserializeBarTags() for deserialization.
	 * 
	 * @param serialized serialized string
	 * @param xmlHandler XML handler to apply tag elements
	 * @param errors error list
	 */
	private static void deserializeAndApplyBarTags(String serialized,
			ConsElementXMLHandler xmlHandler, ArrayList<String> errors) {
		if (serialized == null || serialized.isEmpty())
			return;
		
		// Deserialize using helper class
		GpadSerializer.deserializeBarTags(serialized, (barNumber, rgba, fillTypeXML, hatchAngle,
				hatchDistance, image, fillSymbol) -> {
			// Generate barColor tag with rgba(r,g,b,a) format to match ChartStyle.barXml() output
			// bit 0: barColor (r, g, b)
			if (rgba[0] >= 0 && rgba[1] >= 0 && rgba[2] >= 0) {
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barColor");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", "rgba(" + rgba[0] + "," + rgba[1] + "," + rgba[2] + ",1)");
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// Generate barAlpha tag separately (if present)
			// bit 1: barAlpha
			if (rgba[3] >= 0) {
				double alpha = rgba[3] / 255.0;
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barAlpha");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", String.valueOf(alpha));
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 2: barFillType
			if (fillTypeXML != null) {
				try {
					int fillType = Integer.parseInt(fillTypeXML);
					
					// Validate fillType is within valid range (0-9 for FillType enum)
					if (fillType < 0 || fillType > 9) {
						// Invalid fillType value, skip this bar
						return;
					}
					
					// Create barFillType tag
					LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
					tagAttrs.put("key", "barFillType");
					tagAttrs.put("barNumber", barNumber);
					tagAttrs.put("value", fillTypeXML);
					xmlHandler.startGeoElement("tag", tagAttrs, errors);
				} catch (NumberFormatException e) {
					// Invalid fillType, skip
				}
			}
			
			// bit 3: barHatchAngle
			if (hatchAngle != null) {
				// Create barHatchAngle tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barHatchAngle");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", hatchAngle);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 4: barHatchDistance
			if (hatchDistance != null) {
				// Create barHatchDistance tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barHatchDistance");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", hatchDistance);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 5: barImage
			if (image != null) {
				// Create barImage tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barImage");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", image);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 6: barSymbol
			if (fillSymbol != null) {
				// Create barSymbol tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barSymbol");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", fillSymbol);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
		});
	}
}
