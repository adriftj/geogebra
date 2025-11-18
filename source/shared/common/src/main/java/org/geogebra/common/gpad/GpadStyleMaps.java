package org.geogebra.common.gpad;

import java.util.Map;

/**
 * 集中管理 Gpad 样式与 XML 之间的映射表。
 * 将原映射表和反映射表放在一起，方便同时维护和修改。
 */
public class GpadStyleMaps {
	// ==================== 属性名映射 ====================

	/**
	 * GpadStyleSheet 中必须更换元素名称的那些属性，在此登记
	 * 映射：Gpad 属性名 -> XML 元素名
	 */
	public static final Map<String, String> GPAD_TO_XML_NAME_MAP = Map.of(
			"@screen", "absoluteScreenLocation",
			"hideLabelInAlgebra", "algebra",
			"showIf", "condition",
			"showGeneralAngle", "emphasizeRightAngle",
			"filename", "file");

	/**
	 * XML 元素名 -> Gpad 属性名（GPAD_TO_XML_NAME_MAP 的反向映射）
	 */
	public static final Map<String, String> XML_TO_GPAD_NAME_MAP = Map.of(
			"absoluteScreenLocation", "@screen",
			"algebra", "hideLabelInAlgebra",
			"condition", "showIf",
			"emphasizeRightAngle", "showGeneralAngle",
			"file", "filename");

	// ==================== 属性名到 XML 属性名的映射 ====================

	/**
	 * GpadStyleSheet 中可转换成唯一属性的 XML 元素的那些简单属性，
	 * 若其唯一属性名不是 "val"，则在此登记
	 * 映射：Gpad 属性名 -> XML 属性名
	 */
	public static final Map<String, String> GPAD_TO_XML_ATTR_NAME_MAP = Map.of(
			"hideLabelInAlgebra", "labelVisible",
			"showIf", "showObject",
			"filename", "name",
			"audio", "src",
			"linkedGeo", "exp");

	// ==================== 布尔值相关 ====================

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

	/**
	 * 属性归类列表(从 Parser.jj 中的 GK_BOOL/GK_INT/GK_FLOAT/GK_STR TOKEN定义而来)
	 */
	public static final Map<String, Integer> GK_PROPERTIES = Map.ofEntries(
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
			Map.entry("selectionAllowed", GK_BOOL),
			Map.entry("showGeneralAngle", GK_BOOL),
			Map.entry("showOnAxis", GK_BOOL),
			Map.entry("showTrimmed", GK_BOOL),
			Map.entry("symbolic", GK_BOOL),
			Map.entry("trace", GK_BOOL),

			Map.entry("arcSize", GK_INT),
			Map.entry("decimals", GK_INT),
			Map.entry("layer", GK_INT),
			Map.entry("length", GK_INT),
			Map.entry("selectedIndex", GK_INT),
			Map.entry("significantfigures", GK_INT),
			Map.entry("slopeTriangleSize", GK_INT),

			Map.entry("fading", GK_FLOAT),
			Map.entry("ordering", GK_FLOAT),
			Map.entry("pointSize", GK_FLOAT),
			Map.entry("angleStyle", GK_STR),

			Map.entry("caption", GK_STR),
			Map.entry("content", GK_STR),
			Map.entry("coordStyle", GK_STR),
			Map.entry("decoration", GK_STR),
			Map.entry("dynamicCaption", GK_STR),
			Map.entry("endStyle", GK_STR),
			Map.entry("filename", GK_STR),
			Map.entry("headStyle", GK_STR),
			Map.entry("incrementY", GK_STR),
			Map.entry("labelMode", GK_STR),
			Map.entry("linkedGeo", GK_STR),
			Map.entry("parentLabel", GK_STR),
			Map.entry("pointStyle", GK_STR),
			Map.entry("showIf", GK_STR),
			Map.entry("startStyle", GK_STR),
			Map.entry("textAlign", GK_STR),
			Map.entry("tooltipMode", GK_STR),
			Map.entry("verticalAlign", GK_STR));

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

	// ==================== 线段始端和终端样式 ====================

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

	// ==================== 方程样式 ====================

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

	// ==================== 其他属性值映射 ====================

	// 双向映射的内部 Map（在 VALUE_MAPS 和 VALUE_MAPS_REVERSE 中相同）
	private static final Map<String, String> COORD_STYLE_MAP = Map.of(
			"cartesian", "cartesian",
			"polar", "polar",
			"complex", "complex",
			"cartesian3d", "cartesian3d",
			"spherical", "spherical");

	private static final Map<String, String> TEXT_ALIGN_MAP = Map.of(
			"left", "left",
			"center", "center",
			"right", "right");

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
			Map.entry("decoration", Map.of("none", "0",
					"single_tick", "1",
					"double_tick", "2",
					"triple_tick", "3",
					"simple_arrow", "4",
					"double_arrow", "5",
					"triple_arrow", "6")),
			Map.entry("emphasizeRightAngle", BOOLEAN_VALUE_REVERT_MAP),
			Map.entry("endStyle", START_END_STYLE_VALUES),
			Map.entry("headStyle", Map.of("default", "0", "arrow", "1")),
			Map.entry("labelMode", Map.of("name", "0",
					"namevalue", "1",
					"value", "2",
					"caption", "3")),
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
			Map.entry("decoration", Map.of("0", "none",
					"1", "single_tick",
					"2", "double_tick",
					"3", "triple_tick",
					"4", "simple_arrow",
					"5", "double_arrow",
					"6", "triple_arrow")),
			Map.entry("emphasizeRightAngle", BOOLEAN_VALUE_REVERT_MAP),
			Map.entry("endStyle", START_END_STYLE_VALUES),
			Map.entry("headStyle", Map.of("0", "default", "1", "arrow")),
			Map.entry("labelMode", Map.of("0", "name",
					"1", "namevalue",
					"2", "value",
					"3", "caption")),
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

	// 私有构造函数，防止实例化
	private GpadStyleMaps() {
	}
}
