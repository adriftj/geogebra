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
			Map.entry("hideLabelInAlgebra", "algebra"),
			Map.entry("showIf", "condition"),
			Map.entry("showGeneralAngle", "emphasizeRightAngle"),
			Map.entry("filename", "file"),
			Map.entry("random", "value"));

	/**
	 * XML 元素名 -> Gpad 属性名（GPAD_TO_XML_NAME_MAP 的反向映射）
	 */
	public static final Map<String, String> XML_TO_GPAD_NAME_MAP = Map.ofEntries(
			Map.entry("absoluteScreenLocation", "@screen"),
			Map.entry("algebra", "hideLabelInAlgebra"),
			Map.entry("condition", "showIf"),
			Map.entry("emphasizeRightAngle", "showGeneralAngle"),
			Map.entry("file", "filename"));
			// Note: javascript tag uses the same name in Gpad, no mapping needed

	// ==================== 属性名到XML属性名的映射 ====================

	/**
	 * GpadStyleSheet 中可转换成唯一属性的 XML 元素的那些简单属性，
	 * 若其唯一属性名不是 "val"，则在此登记
	 * 映射：Gpad 属性名 -> XML 属性名
	 */
	public static final Map<String, String> GPAD_TO_XML_ATTR_NAME_MAP = Map.of(
			"audio", "src",
			"curveParam", "t",
			"decoration", "type",
			"filename", "name",
			"hideLabelInAlgebra", "labelVisible",
			"linkedGeo", "exp",
			"random", "random",
			"showIf", "showObject",
			"video", "src");
			// Note: javascript uses default "val" attribute, no mapping needed

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

	// ==================== 属性信息内部类 ====================

	/**
	 * 属性信息，包含类型和默认值
	 * 默认值存储的是 XML 格式的值（因为比较时使用的是从 XML 读取的值）
	 */
	public static class PropertyInfo {
		public final Integer type;
		public final String defaultValue;

		public PropertyInfo(Integer type, String defaultValue) {
			this.type = type;
			this.defaultValue = defaultValue;
		}
	}

	// ==================== 属性信息表（合并了类型和默认值） ====================

	/**
	 * 属性信息表，同时包含类型（GK_BOOL/GK_INT/GK_FLOAT/GK_STR）和默认值
	 * 用于统一管理属性类型和默认值，方便维护
	 * Key 是 Gpad 属性名
	 * 默认值存储的是 XML 格式的值（因为比较时使用的是从 XML 读取的值）
	 */
	public static final Map<String, PropertyInfo> PROPERTY_INFO = Map.ofEntries(
			// GK_BOOL 类型（布尔类型没有默认值，因为 false 值会被省略）
			Map.entry("autocolor", new PropertyInfo(GK_BOOL, null)),
			Map.entry("auxiliary", new PropertyInfo(GK_BOOL, null)),
			Map.entry("breakpoint", new PropertyInfo(GK_BOOL, null)),
			Map.entry("centered", new PropertyInfo(GK_BOOL, null)),
			Map.entry("comboBox", new PropertyInfo(GK_BOOL, null)),
			Map.entry("contentSerif", new PropertyInfo(GK_BOOL, null)),
			Map.entry("fixed", new PropertyInfo(GK_BOOL, null)),
			Map.entry("hideLabelInAlgebra", new PropertyInfo(GK_BOOL, null)),
			Map.entry("inBackground", new PropertyInfo(GK_BOOL, null)),
			Map.entry("interpolate", new PropertyInfo(GK_BOOL, null)),
			Map.entry("isLaTeX", new PropertyInfo(GK_BOOL, null)),
			Map.entry("isMask", new PropertyInfo(GK_BOOL, null)),
			Map.entry("keepTypeOnTransform", new PropertyInfo(GK_BOOL, null)),
			Map.entry("levelOfDetailQuality", new PropertyInfo(GK_BOOL, null)),
			Map.entry("outlyingIntersections", new PropertyInfo(GK_BOOL, null)),
			Map.entry("random", new PropertyInfo(GK_BOOL, null)),
			Map.entry("selectionAllowed", new PropertyInfo(GK_BOOL, null)),
			Map.entry("showGeneralAngle", new PropertyInfo(GK_BOOL, null)),
			Map.entry("showOnAxis", new PropertyInfo(GK_BOOL, null)),
			Map.entry("showTrimmed", new PropertyInfo(GK_BOOL, null)),
			Map.entry("symbolic", new PropertyInfo(GK_BOOL, null)),
			Map.entry("trace", new PropertyInfo(GK_BOOL, null)),

			// GK_INT 类型
			Map.entry("arcSize", new PropertyInfo(GK_INT, "30")),
			Map.entry("decimals", new PropertyInfo(GK_INT, "-1")),
			Map.entry("layer", new PropertyInfo(GK_INT, "0")),
			Map.entry("length", new PropertyInfo(GK_INT, "20")),
			Map.entry("selectedIndex", new PropertyInfo(GK_INT, "0")),
			Map.entry("significantfigures", new PropertyInfo(GK_INT, "-1")),
			Map.entry("slopeTriangleSize", new PropertyInfo(GK_INT, "1")),

			// GK_FLOAT 类型
			Map.entry("fading", new PropertyInfo(GK_FLOAT, "0.0")),
			Map.entry("ordering", new PropertyInfo(GK_FLOAT, "NaN")),
			Map.entry("pointSize", new PropertyInfo(GK_FLOAT, "5")),

			// GK_STR 类型（存储 XML 格式的默认值）
			// 对于有值映射的属性，这里存储的是 XML 值（如 "0"），而不是 Gpad 值（如 "0-360"）
			Map.entry("angleStyle", new PropertyInfo(GK_STR, "0")), // XML 中 "0" 对应 Gpad 的 "0-360"
			Map.entry("audio", new PropertyInfo(GK_STR, "")), // XML 属性名是 "src"
			Map.entry("caption", new PropertyInfo(GK_STR, "")),
			Map.entry("content", new PropertyInfo(GK_STR, "")),
			Map.entry("coordStyle", new PropertyInfo(GK_STR, "cartesian")),
			Map.entry("curveParam", new PropertyInfo(GK_STR, "")), // 值可能是表达式（字符串）
			Map.entry("decoration", new PropertyInfo(GK_STR, "0")), // XML 中 "0" 对应 Gpad 的 "none"
			Map.entry("dynamicCaption", new PropertyInfo(GK_STR, "")),
			Map.entry("endStyle", new PropertyInfo(GK_STR, "default")),
			Map.entry("filename", new PropertyInfo(GK_STR, "")), // XML 元素名是 "file"
			Map.entry("headStyle", new PropertyInfo(GK_STR, "0")), // XML 中 "0" 对应 Gpad 的 "default"
			Map.entry("incrementY", new PropertyInfo(GK_STR, "")),
			Map.entry("labelMode", new PropertyInfo(GK_STR, "0")), // XML 中 "0" 对应 Gpad 的 "name"
			Map.entry("linkedGeo", new PropertyInfo(GK_STR, "")),
			Map.entry("parentLabel", new PropertyInfo(GK_STR, "")),
			Map.entry("pointStyle", new PropertyInfo(GK_STR, "-1")), // XML 中 "-1" 对应 Gpad 的 "default"
			Map.entry("javascript", new PropertyInfo(GK_STR, "")), // javascript uses same name in Gpad and XML
			Map.entry("showIf", new PropertyInfo(GK_STR, "")), // XML 元素名是 "condition"
			Map.entry("startStyle", new PropertyInfo(GK_STR, "default")),
			Map.entry("textAlign", new PropertyInfo(GK_STR, "left")),
			Map.entry("tooltipMode", new PropertyInfo(GK_STR, "0")), // XML 中 "0" 对应 Gpad 的 "algebraview"
			Map.entry("verticalAlign", new PropertyInfo(GK_STR, "top")),
			Map.entry("video", new PropertyInfo(GK_STR, ""))); // XML 属性名是 "src"

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

	// 私有构造函数，防止实例化
	private GpadStyleMaps() {
	}
}
