package org.geogebra.common.gpad;

import java.util.Map;

public class GpadStyleMaps {
	// ==================== 属性名映射 ====================

	/**
	 * GpadStyleSheet 中必须更换元素名称的那些属性，在此登记
	 * 映射：Gpad 属性名 -> XML 元素名
	 */
	public static final Map<String, String> GPAD_TO_XML_NAME_MAP = Map.ofEntries(
			Map.entry("@screen", "absoluteScreenLocation"),
			Map.entry("color", "objColor"),
			Map.entry("hideLabelInAlgebra", "algebra"),
			Map.entry("showIf", "condition"),
			Map.entry("showGeneralAngle", "emphasizeRightAngle"),
			Map.entry("random", "value"),
			Map.entry("unselectable", "selectionAllowed"));

	/**
	 * XML 元素名 -> Gpad 属性名（GPAD_TO_XML_NAME_MAP 的反向映射）
	 */
	public static final Map<String, String> XML_TO_GPAD_NAME_MAP = Map.ofEntries(
			Map.entry("absoluteScreenLocation", "@screen"),
			Map.entry("algebra", "hideLabelInAlgebra"),
			Map.entry("condition", "showIf"),
			Map.entry("emphasizeRightAngle", "showGeneralAngle"),
			Map.entry("objColor", "color"),
			Map.entry("selectionAllowed", "unselectable"));

	// ==================== 属性名到XML属性名的映射 ====================

	/**
	 * GpadStyleSheet 中可转换成唯一属性的 XML 元素的那些简单属性，
	 * 若其唯一属性名不是 "val"，则在此登记
	 * 映射：Gpad 属性名 -> XML 属性名
	 */
	public static final Map<String, String> GPAD_TO_XML_ATTR_NAME_MAP = Map.ofEntries(
		Map.entry("audio", "src"),
		Map.entry("curveParam", "t"),
		Map.entry("decoration", "type"),
		Map.entry("file", "name"),
		Map.entry("hideLabelInAlgebra", "labelVisible"),
		Map.entry("linkedGeo", "exp"),
		Map.entry("random", "random"),
		Map.entry("showIf", "showObject"),
		Map.entry("userinput", "show"),
		Map.entry("video", "src"));

	/**
	 * 布尔值反转取值表
	 * 用于需要反转布尔值的属性（如 emphasizeRightAngle）
	 */
	public static final Map<String, String> BOOLEAN_VALUE_REVERT_MAP = Map.of(
			"true", "false",
			"false", "true");

	public static final Integer GK_BOOL = 0;
	public static final Integer GK_INT = 1;
	public static final Integer GK_FLOAT = 2;
	public static final Integer GK_STR = 3;

	// ==================== 属性类型表 ====================

	/**
	 * 属性类型表：Gpad 属性名 -> 类型常量（GK_BOOL/GK_INT/GK_FLOAT/GK_STR）。
	 * 默认值通过 {@link #getSimpleDefaultValue(String)} 从 {@link GpadStyleDefaults} 获取。
	 */
	public static final Map<String, Integer> PROPERTY_INFO = Map.ofEntries(
			// GK_BOOL 类型
			Map.entry("allowReflexAngle", GK_BOOL),
			Map.entry("autocolor", GK_BOOL),
			Map.entry("auxiliary", GK_BOOL),
			Map.entry("breakpoint", GK_BOOL),
			Map.entry("centered", GK_BOOL),
			Map.entry("comboBox", GK_BOOL),
			Map.entry("contentSerif", GK_BOOL),
			Map.entry("fixed", GK_BOOL),
			Map.entry("hideLabelInAlgebra", GK_BOOL),
			Map.entry("inBackground", GK_BOOL),
			Map.entry("interpolate", GK_BOOL),
			Map.entry("isLaTeX", GK_BOOL),
			Map.entry("isMask", GK_BOOL),
			Map.entry("keepTypeOnTransform", GK_BOOL),
			Map.entry("levelOfDetailQuality", GK_BOOL),
			Map.entry("outlyingIntersections", GK_BOOL),
			Map.entry("random", GK_BOOL),
			Map.entry("unselectable", GK_BOOL),
			Map.entry("showGeneralAngle", GK_BOOL),
			Map.entry("showOnAxis", GK_BOOL),
			Map.entry("showTrimmed", GK_BOOL),
			Map.entry("symbolic", GK_BOOL),
			Map.entry("trace", GK_BOOL),
			Map.entry("userinput", GK_BOOL),

			// GK_INT 类型
			Map.entry("arcSize", GK_INT),
			Map.entry("decimals", GK_INT),
			Map.entry("layer", GK_INT),
			Map.entry("length", GK_INT),
			Map.entry("levelOfDetail", GK_INT),
			Map.entry("selectedIndex", GK_INT),
			Map.entry("significantfigures", GK_INT),
			Map.entry("slopeTriangleSize", GK_INT),

			// GK_FLOAT 类型
			Map.entry("fading", GK_FLOAT),
			Map.entry("ordering", GK_FLOAT),
			Map.entry("pointSize", GK_FLOAT),

			// GK_STR 类型
			Map.entry("angleStyle", GK_STR),
			Map.entry("audio", GK_STR),
			Map.entry("caption", GK_STR),
			Map.entry("content", GK_STR),
			Map.entry("coordStyle", GK_STR),
			Map.entry("curveParam", GK_STR),
			Map.entry("decoration", GK_STR),
			Map.entry("dynamicCaption", GK_STR),
			Map.entry("endStyle", GK_STR),
			Map.entry("file", GK_STR),
			Map.entry("headStyle", GK_STR),
			Map.entry("incrementY", GK_STR),
			Map.entry("jsClickFunction", GK_STR),
			Map.entry("jsUpdateFunction", GK_STR),
			Map.entry("labelMode", GK_STR),
			Map.entry("linkedGeo", GK_STR),
			Map.entry("parentLabel", GK_STR),
			Map.entry("pointStyle", GK_STR),
			Map.entry("showIf", GK_STR),
			Map.entry("startStyle", GK_STR),
			Map.entry("textAlign", GK_STR),
			Map.entry("tooltipMode", GK_STR),
			Map.entry("verticalAlign", GK_STR),
			Map.entry("video", GK_STR));

	/**
	 * Gets the default value for a simple property (GK_BOOL/GK_INT/GK_FLOAT/GK_STR)
	 * from the GpadStyleDefaults authoritative source.
	 * This converts from Gpad property name to XML tag name and reads the appropriate attr.
	 * 
	 * @param gpadName Gpad property name
	 * @return default value in XML format, or null if no default
	 */
	public static String getSimpleDefaultValue(String gpadName) {
		String xmlTagName = GPAD_TO_XML_NAME_MAP.getOrDefault(gpadName, gpadName);
		String attrName = GPAD_TO_XML_ATTR_NAME_MAP.getOrDefault(gpadName, "val");
		return GpadStyleDefaults.getDefaultAttrValue(xmlTagName, attrName);
	}

	// ==================== lineStyle 相关 ====================

	/**
	 * lineStyle 的 type 取值表
	 * 映射：Gpad 键 -> XML 值
	 */
	public static final Map<String, String> LINE_STYLE_TYPE_MAP = Map.of(
			"pointwise", "-1",
			"full", "0",
			"dashedshort", "10",
			"dashedlong", "15",
			"dotted", "20",
			"dasheddotted", "30");

	/**
	 * lineStyle 的 type 值反向映射：XML 值 -> Gpad 键
	 */
	public static final Map<String, String> LINE_STYLE_TYPE_REVERSE_MAP = Map.ofEntries(
			Map.entry("-1", "pointwise"),
			Map.entry("0", "full"),
			Map.entry("1", "dashedshort"),   // legacy value for dashed
			Map.entry("2", "dotted"),        // legacy value for dotted
			Map.entry("3", "dasheddotted"),  // legacy value for dashed-dotted
			Map.entry("4", "dashedlong"),    // legacy value for dashed-long
			Map.entry("10", "dashedshort"),
			Map.entry("15", "dashedlong"),
			Map.entry("20", "dotted"),
			Map.entry("30", "dasheddotted"));

	/**
	 * lineStyle 的 typeHidden 取值表（用于 3D）
	 * 映射：Gpad 键 -> XML 值
	 */
	public static final Map<String, String> LINE_STYLE_TYPE_HIDDEN_MAP = Map.of(
			"", "0",
			"dashed", "1",
			"show", "2");

	/**
	 * lineStyle 的 typeHidden 值反向映射：XML 值 -> Gpad 键
	 */
	public static final Map<String, String> LINE_STYLE_TYPE_HIDDEN_REVERSE_MAP = Map.of(
			"0", "",
			"1", "dashed",
			"2", "show");

	/**
	 * 线段始端和终端样式取值表
	 */
	public static final Map<String, String> START_END_STYLE_VALUES = Map.ofEntries(
			Map.entry("default", "default"),
			Map.entry("line", "line"),
			Map.entry("arrow", "arrow"),
			Map.entry("crows_foot", "crows_foot"),
			Map.entry("arrow_outline", "arrow_outline"),
			Map.entry("arrow_filled", "arrow_filled"),
			Map.entry("circle_outline", "circle_outline"),
			Map.entry("circle", "circle"),
			Map.entry("square_outline", "square_outline"),
			Map.entry("square", "square"),
			Map.entry("diamond_outline", "diamond_outline"),
			Map.entry("diamond", "diamond"));

	/**
	 * 直线和二次曲线的方程显示形式
	 */
	public static final Map<String, String> EQN_STYLE_VALUES = Map.of(
			"implicit", "implicit",
			"explicit", "explicit",
			"parametric", "parametric",
			"specific", "specific",
			"general", "general",
			"vertex", "vertex",
			"conic", "conic",
			"user", "user");

	/**
	 * 坐标系样式取值表
	 */
	private static final Map<String, String> COORD_STYLE_MAP = Map.of(
			"cartesian", "cartesian",
			"polar", "polar",
			"complex", "complex",
			"cartesian3d", "cartesian3d",
			"spherical", "spherical");

	/**
	 * 文本左右对齐方式取值表
	 */
	private static final Map<String, String> TEXT_ALIGN_MAP = Map.of(
			"left", "left",
			"center", "center",
			"right", "right");

	/**
	 * 上下对齐方式取值表
	 */
	private static final Map<String, String> VERTICAL_ALIGN_MAP = Map.of(
			"top", "top",
			"middle", "middle",
			"bottom", "bottom");

	/**
	 * GpadStyleSheet 中属性值到 XML 元素属性值的映射表
	 * 这是一个嵌套的 Map，外层 key 是XML元素名，内层 Map 是值映射（Gpad 值 -> XML 值）
	 */
	public static final Map<String, Map<String, String>> VALUE_MAPS = Map.ofEntries(
			Map.entry("angleStyle", Map.of("0-360", "0",
					"0-180", "1",
					"180-360", "2",
					"any", "3")),
			Map.entry("coordStyle", COORD_STYLE_MAP),
			Map.entry("decoration", Map.ofEntries(
					Map.entry("none", "0"),
					Map.entry("single_tick", "1"),
					Map.entry("double_tick", "2"),
					Map.entry("triple_tick", "3"),
					Map.entry("simple_arrow", "4"),
					Map.entry("double_arrow", "5"),
					Map.entry("triple_arrow", "6"),
					Map.entry("right_angle", "7"),
					Map.entry("right_angle_dot", "8"),
					Map.entry("clockwise", "9"))),
			Map.entry("emphasizeRightAngle", BOOLEAN_VALUE_REVERT_MAP),
			Map.entry("selectionAllowed", BOOLEAN_VALUE_REVERT_MAP),
			Map.entry("endStyle", START_END_STYLE_VALUES),
			Map.entry("headStyle", Map.of("default", "0", "arrow", "1")),
			Map.entry("labelMode", Map.ofEntries(
					Map.entry("name", "0"),
					Map.entry("namevalue", "1"),
					Map.entry("value", "2"),
					Map.entry("caption", "3"),
					Map.entry("default", "4"),
					Map.entry("defaultname", "5"),
					Map.entry("defaultnamevalue", "6"),
					Map.entry("defaultvalue", "7"),
					Map.entry("defaultcaption", "8"),
					Map.entry("captionvalue", "9"))),
			Map.entry("pointStyle", Map.ofEntries(
					Map.entry("default", "-1"),
					Map.entry("dot", "0"),
					Map.entry("cross", "1"),
					Map.entry("circle", "2"),
					Map.entry("plus", "3"),
					Map.entry("diamond", "4"),
					Map.entry("empty_diamond", "5"),
					Map.entry("triangle_north", "6"),
					Map.entry("triangle_south", "7"),
					Map.entry("triangle_east", "8"),
					Map.entry("triangle_west", "9"),
					Map.entry("no_outline", "10"))),
			Map.entry("startStyle", START_END_STYLE_VALUES),
			Map.entry("textAlign", TEXT_ALIGN_MAP),
			Map.entry("tooltipMode", Map.of("algebraview", "0",
					"on", "1",
					"off", "2",
					"caption", "3",
					"nextcell", "4")),
			Map.entry("verticalAlign", VERTICAL_ALIGN_MAP));

	/**
	 * VALUE_MAPS 的反映射表
	 * 外层 key 保持不变（XML 元素名），内层 Map 反向（XML 值 -> Gpad 值）
	 */
	public static final Map<String, Map<String, String>> VALUE_MAPS_REVERSE = Map.ofEntries(
			Map.entry("angleStyle", Map.of("0", "0-360",
					"1", "0-180",
					"2", "180-360",
					"3", "any")),
			Map.entry("coordStyle", COORD_STYLE_MAP),
			Map.entry("decoration", Map.ofEntries(
					Map.entry("0", "none"),
					Map.entry("1", "single_tick"),
					Map.entry("2", "double_tick"),
					Map.entry("3", "triple_tick"),
					Map.entry("4", "simple_arrow"),
					Map.entry("5", "double_arrow"),
					Map.entry("6", "triple_arrow"),
					Map.entry("7", "right_angle"),
					Map.entry("8", "right_angle_dot"),
					Map.entry("9", "clockwise"))),
			Map.entry("emphasizeRightAngle", BOOLEAN_VALUE_REVERT_MAP),
			Map.entry("selectionAllowed", BOOLEAN_VALUE_REVERT_MAP),
			Map.entry("endStyle", START_END_STYLE_VALUES),
			Map.entry("headStyle", Map.of("0", "default", "1", "arrow")),
			Map.entry("labelMode", Map.ofEntries(
					Map.entry("0", "name"),
					Map.entry("1", "namevalue"),
					Map.entry("2", "value"),
					Map.entry("3", "caption"),
					Map.entry("4", "default"),
					Map.entry("5", "defaultname"),
					Map.entry("6", "defaultnamevalue"),
					Map.entry("7", "defaultvalue"),
					Map.entry("8", "defaultcaption"),
					Map.entry("9", "captionvalue"))),
			Map.entry("pointStyle", Map.ofEntries(
					Map.entry("-1", "default"),
					Map.entry("0", "dot"),
					Map.entry("1", "cross"),
					Map.entry("2", "circle"),
					Map.entry("3", "plus"),
					Map.entry("4", "diamond"),
					Map.entry("5", "empty_diamond"),
					Map.entry("6", "triangle_north"),
					Map.entry("7", "triangle_south"),
					Map.entry("8", "triangle_east"),
					Map.entry("9", "triangle_west"),
					Map.entry("10", "no_outline"))),
			Map.entry("startStyle", START_END_STYLE_VALUES),
			Map.entry("textAlign", TEXT_ALIGN_MAP),
			Map.entry("tooltipMode", Map.of("0", "algebraview",
					"1", "on",
					"2", "off",
					"3", "caption",
					"4", "nextcell")),
			Map.entry("verticalAlign", VERTICAL_ALIGN_MAP));

	// ==================== FillType 相关 ====================

	/**
	 * FillType 枚举值到 Gpad 字符串的映射
	 * 映射：XML 值（整数） -> Gpad 字符串
	 */
	public static final Map<String, String> FILL_TYPE_REVERSE_MAP = Map.ofEntries(
			Map.entry("0", "standard"),
			Map.entry("1", "hatch"),
			Map.entry("2", "crosshatch"),
			Map.entry("3", "chessboard"),
			Map.entry("4", "dotted"),
			Map.entry("5", "honeycomb"),
			Map.entry("6", "brick"),
			Map.entry("7", "weaving"),
			Map.entry("8", "symbols"),
			Map.entry("9", "image"));

	/**
	 * Gpad 字符串到 FillType 枚举值的映射
	 * 映射：Gpad 字符串 -> XML 值（整数）
	 */
	public static final Map<String, String> FILL_TYPE_MAP = Map.ofEntries(
			Map.entry("standard", "0"),
			Map.entry("hatch", "1"),
			Map.entry("crosshatch", "2"),
			Map.entry("chessboard", "3"),
			Map.entry("dotted", "4"),
			Map.entry("honeycomb", "5"),
			Map.entry("brick", "6"),
			Map.entry("weaving", "7"),
			Map.entry("symbols", "8"),
			Map.entry("image", "9"));

	// ==================== 视图 ID / 名称映射 ====================

	public static final Map<String, String> VIEW_NAME_TO_ID = Map.ofEntries(
			Map.entry("ev1", "1"),
			Map.entry("algebra", "2"),
			Map.entry("spreadsheet", "4"),
			Map.entry("cas", "8"),
			Map.entry("ev2", "16"),
			Map.entry("protocol", "32"),
			Map.entry("probability", "64"),
			Map.entry("data", "70"),
			Map.entry("inspector", "128"),
			Map.entry("ev3d", "512"),
			Map.entry("ev3d2", "513"),
			Map.entry("plane", "1024"),
			Map.entry("properties", "4097"),
			Map.entry("table", "8192"),
			Map.entry("tools", "16384"),
			Map.entry("sidePanel", "32768"));

	public static final Map<Integer, String> VIEW_ID_TO_NAME;
	static {
		java.util.Map<Integer, String> m = new java.util.LinkedHashMap<>();
		for (Map.Entry<String, String> e : VIEW_NAME_TO_ID.entrySet()) {
			try {
				m.put(Integer.parseInt(e.getValue()), e.getKey());
			} catch (NumberFormatException ignored) { }
		}
		VIEW_ID_TO_NAME = java.util.Collections.unmodifiableMap(m);
	}

	public static String viewNameToId(String name) {
		String mapped = VIEW_NAME_TO_ID.get(name);
		return mapped != null ? mapped : name;
	}

	public static String viewIdToName(String idStr) {
		if (idStr == null) return "0";
		try {
			int id = Integer.parseInt(idStr);
			String name = VIEW_ID_TO_NAME.get(id);
			return name != null ? name : idStr;
		} catch (NumberFormatException e) {
			return idStr;
		}
	}

	// ==================== Tab ID / 名称映射 ====================

	public static final Map<String, String> TAB_NAME_TO_ID = Map.ofEntries(
			Map.entry("algebra", "ALGEBRA"),
			Map.entry("tools", "TOOLS"),
			Map.entry("table", "TABLE"),
			Map.entry("distribution", "DISTRIBUTION"),
			Map.entry("spreadsheet", "SPREADSHEET"));

	public static final Map<String, String> TAB_ID_TO_NAME = Map.ofEntries(
			Map.entry("0", "algebra"),
			Map.entry("ALGEBRA", "algebra"),
			Map.entry("1", "tools"),
			Map.entry("TOOLS", "tools"),
			Map.entry("2", "table"),
			Map.entry("TABLE", "table"),
			Map.entry("3", "distribution"),
			Map.entry("DISTRIBUTION", "distribution"),
			Map.entry("4", "spreadsheet"),
			Map.entry("SPREADSHEET", "spreadsheet"));

	public static String tabNameToId(String name) {
		if (name == null) return null;
		String mapped = TAB_NAME_TO_ID.get(name);
		return mapped != null ? mapped : name;
	}

	public static String tabIdToName(String idOrName) {
		if (idOrName == null) return null;
		return TAB_ID_TO_NAME.get(idOrName);
	}

	// 私有构造函数，防止实例化
	private GpadStyleMaps() {
	}
}
