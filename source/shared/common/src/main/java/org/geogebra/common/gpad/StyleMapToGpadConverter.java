package org.geogebra.common.gpad;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts style map (Map<String, LinkedHashMap<String, String>>) to Gpad format.
 * This converter takes the XML-style attribute maps and converts them to Gpad syntax.
 */
public class StyleMapToGpadConverter {
	/**
	 * Converts a style map to Gpad style sheet format.
	 * 
	 * @param name
	 *            style sheet name (without $ prefix)
	 * @param styleMap
	 *            map from XML tag names to attribute maps
	 * @return Gpad style sheet string, or null if styleMap is empty
	 */
	public String convert(String name, Map<String, LinkedHashMap<String, String>> styleMap) {
		if (styleMap == null || styleMap.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("$").append(name).append(" = {");

		boolean first = true;
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : styleMap.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();

			// Special handling for checkbox: if fixed="true", output both "checkbox;" and "fixed;"
			if ("checkbox".equals(tagName)) {
				String fixed = attrs != null ? attrs.get("fixed") : null;
				if (!first)
					sb.append(";");
				sb.append(" checkbox");
				first = false;
				if ("true".equals(fixed)) // Also output fixed (as a boolean property)
					sb.append("; fixed");
				// If fixed="false" or not present, only output "checkbox;" (fixed is default false, so omitted)
				continue;
			}

			// Convert XML tag name and attributes to Gpad format
			String gpadProperty = convertPropertyToGpad(tagName, attrs);
			if (gpadProperty != null && !gpadProperty.isEmpty()) {
				if (!first)
					sb.append(";");
				sb.append(" ");
				sb.append(gpadProperty);
				first = false;
			}
		}

		sb.append(" }");
		return sb.toString();
	}

	/**
	 * Converts a single property (XML tag + attributes) to Gpad format.
	 * 
	 * @param tagName XML tag name (e.g., "lineStyle", "objColor")
	 * @param attrs attribute map
	 * @return Gpad property string (e.g., "lineStyle: thickness=4 opacity=178")
	 */
	private String convertPropertyToGpad(String tagName, LinkedHashMap<String, String> attrs) {
		String prop = convertSimplePropertyToGpad(tagName, attrs);
		if (prop == null)
			return null;
		if (!prop.isEmpty())
			return prop;

		if (attrs == null || attrs.isEmpty())
			return null;

		String gpadName = GpadStyleMaps.XML_TO_GPAD_NAME_MAP.getOrDefault(tagName, tagName);
		StringBuilder sb = new StringBuilder();
		sb.append(gpadName).append(": ");

		// Handle different property types
		switch (tagName) {
		case "lineStyle":
			// lineStyle: dashedlong thickness=5 hidden opacity=128 ~arrow
			sb.append(convertLineStyle(attrs));
			break;
		case "objColor":
		case "bgColor":
		case "borderColor":
			// objColor/bgColor/borderColor: #rrggbb or #rrggbbaa (if alpha is not default)
			sb.append(convertColorToHex(attrs));
			break;
		case "absoluteScreenLocation": // @screen: 100 200
		case "labelOffset": // labelOffset: 28 75
			if (attrs.containsKey("x") && attrs.containsKey("y"))
				sb.append(attrs.get("x")).append(" ").append(attrs.get("y"));
			break;
		case "animation":
			// animation: play +0.1 speed=2
			String animationResult = convertAnimation(attrs);
			if (animationResult == null || animationResult.isEmpty())
				return null;
			sb.append(animationResult);
			break;
		case "eqnStyle":
			// eqnStyle: implicit; or eqnStyle: parametric=t;
			if (attrs.containsKey("style")) {
				String style = attrs.get("style");
				sb.append(style);
				// If style is parametric and has parameter attribute, add =parameter
				if ("parametric".equals(style) && attrs.containsKey("parameter"))
					sb.append("=").append(attrs.get("parameter"));
			}
			break;
		case "boundingBox":
			// boundingBox: width=100 height=200
			sb.append(convertBoundingBox(attrs));
			break;
		case "contentSize":
			// contentSize: width=100.5 height=200.3
			sb.append(convertContentSize(attrs));
			break;
		case "cropBox":
			// cropBox: x=10 y=20 width=100 height=200 cropped
			sb.append(convertCropBox(attrs));
			break;
		case "dimensions":
			// dimensions: width=100 height=200 angle=45 scaled
			sb.append(convertDimensions(attrs));
			break;
		}

		return sb.toString();
	}

	/**
	 * Converts animation attributes to Gpad format.
	 * Syntax: animation: [play|~play] [type step] [speed x];
	 * Three parts separated by whitespace, any order, any can be omitted
	 * Default values: playing=false, type=0, step=0.1, speed=1
	 * 
	 * @param attrs animation attributes
	 * @return Gpad animation string (e.g., "play +0.2 speed=2" or "=0.5 speed=\"1+1\"")
	 */
	private String convertAnimation(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean hasAny = false;

		// Playing state: play (default is false, so only output when true)
		String playing = attrs.get("playing");
		if ("true".equals(playing)) {
			sb.append("play");
			hasAny = true;
		}

		// Step with type prefix: [+|-|=][step]
		// + for type="1" (ANIMATION_INCREASING)
		// - for type="2" (ANIMATION_DECREASING)
		// = for type="3" (ANIMATION_INCREASING_ONCE)
		// no prefix for type="0" (ANIMATION_OSCILLATING, default)
		// Output type prefix if it is not default(0), even if step is default
		// If both type and step exist, step should follow type immediately without space
		String type = attrs.get("type");
		String prefix = null;
		if (type != null) {
			switch (type) {
			case "1": // ANIMATION_INCREASING
				prefix = "+";
				break;
			case "2": // ANIMATION_DECREASING
				prefix = "-";
				break;
			case "3": // ANIMATION_INCREASING_ONCE
				prefix = "=";
				break;
			}
			if (prefix != null) {
				if (hasAny)
					sb.append(" ");
				sb.append(prefix);
				hasAny = true;
			}
		}
			
		// Output step if non-default (immediately after type prefix if exists)
		String step = attrs.get("step");
		if (step != null && !"0.1".equals(step)) {
			if (hasAny && prefix==null)
				sb.append(" ");
			// If step is an expression (not a plain number), wrap it in quotes
			if (isExpression(step))
				sb.append("\"").append(step).append("\"");
			else
				sb.append(step);
			hasAny = true;
		}

		// Speed: speed=value (default is 1, so only output when different)
		String speed = attrs.get("speed");
		if (speed != null && !"1".equals(speed)) {
			if (hasAny)
				sb.append(" ");
			sb.append("speed=");
			// If speed is an expression (not a plain number), wrap it in quotes
			if (isExpression(speed))
				sb.append("\"").append(speed).append("\"");
			else
				sb.append(speed);
		}

		return sb.toString();
	}

	/**
	 * Checks if a string is an expression (not a plain unsigned number).
	 * A plain number is an integer or floating-point number without sign.
	 * 
	 * @param value the string to check
	 * @return true if the string is an expression, false if it's a plain number
	 */
	private boolean isExpression(String value) {
		if (value == null || value.isEmpty())
			return false;
		
		// Try to match unsigned integer or floating-point number
		// Pattern: optional digits, optional decimal point, optional digits
		// Must not start with + or -
		return !value.matches("^\\d+(\\.\\d+)?$");
	}

	/**
	 * Converts lineStyle XML attributes to Gpad format.
	 * Syntax: [type] [thickness=value] [hidden[=dashed|show]] [opacity=value] [arrow|~arrow]
	 * 
	 * @param attrs
	 *            lineStyle attributes map
	 * @return Gpad lineStyle string
	 */
	private String convertLineStyle(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert type (no prefix)
		String typeValue = attrs.get("type");
		if (typeValue != null) {
			String typeKey = GpadStyleMaps.LINE_STYLE_TYPE_REVERSE_MAP.get(typeValue);
			if (typeKey != null) {
				if (!first)
					sb.append(" ");
				sb.append(typeKey);
				first = false;
			}
		}

		// Convert thickness=value
		String thickness = attrs.get("thickness");
		if (thickness != null) {
			if (!first)
				sb.append(" ");
			sb.append("thickness=").append(thickness);
			first = false;
		}

		// Convert typeHidden (hidden, hidden=dashed, or hidden=show)
		String typeHiddenValue = attrs.get("typeHidden");
		if (typeHiddenValue != null) {
			if (!first)
				sb.append(" ");
			String typeHiddenKey = GpadStyleMaps.LINE_STYLE_TYPE_HIDDEN_REVERSE_MAP.get(typeHiddenValue);
			if (typeHiddenKey != null) {
				if (typeHiddenKey.isEmpty())
					sb.append("hidden");
				else
					sb.append("hidden=").append(typeHiddenKey);
			} else // Default to hidden if value not recognized
				sb.append("hidden");
			first = false;
		}

		// Convert opacity=value
		String opacity = attrs.get("opacity");
		if (opacity != null) {
			if (!first)
				sb.append(" ");
			sb.append("opacity=").append(opacity);
			first = false;
		}

		// Convert drawArrow (arrow or ~arrow)
		String drawArrow = attrs.get("drawArrow");
		if (drawArrow != null) {
			if ("true".equals(drawArrow)) {
				if (!first)
					sb.append(" ");
				sb.append("arrow");
				first = false;
			}
		}

		return sb.toString();
	}

	/**
	 * Converts color attributes (r, g, b, alpha) to hex format.
	 * Outputs #rrggbb if alpha is default (1.0 or "ff"), otherwise #rrggbbaa.
	 * 
	 * @param attrs
	 *            color attributes map
	 * @return hex color string (e.g., "#FF0000" or "#FF0000FF")
	 */
	private String convertColorToHex(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty()) {
			return "";
		}

		// Check if we have r, g, b values
		String rStr = attrs.get("r");
		String gStr = attrs.get("g");
		String bStr = attrs.get("b");
		String alphaStr = attrs.get("alpha");

		try {
			// Convert r, g, b to hex
			int r = rStr==null? 0: Integer.parseInt(rStr);
			int g = gStr==null? 0: Integer.parseInt(gStr);
			int b = bStr==null? 0: Integer.parseInt(bStr);

			// Clamp values to 0-255
			r = Math.max(0, Math.min(255, r));
			g = Math.max(0, Math.min(255, g));
			b = Math.max(0, Math.min(255, b));

			StringBuilder sb = new StringBuilder("#");
			sb.append(String.format("%02X", r));
			sb.append(String.format("%02X", g));
			sb.append(String.format("%02X", b));

			if (alphaStr != null) {
				double alpha;
				try {
					alpha = Double.parseDouble(alphaStr);
				} catch (NumberFormatException e) {
					// If alpha is not a valid number, default to 1.0
					alpha = 1.0;
				}

				// Only append alpha if it's not the default value (1.0 = ff)
				// Use a small epsilon to handle floating point precision issues
				if (Math.abs(alpha - 1.0) > 1e-6) {
					// Convert alpha from 0.0-1.0 to 0-255
					int alphaInt = (int) Math.round(alpha * 255);
					alphaInt = Math.max(0, Math.min(255, alphaInt));
					sb.append(String.format("%02X", alphaInt));
				}
			}

			return sb.toString();
		} catch (NumberFormatException e) {
			return "#000000";
		}
	}

	/**
	 * 转换简单xml元素(GK_BOOL/GK_INT/GK_FLOAT/GK_STR)的属性map到Gpad样式
	 * 
	 * @param xmlTagName XML标签名
	 * @param attrs 属性映射
	 * @return Gpad 格式的字符串（如 "angleStyle: 0-360" 或 "caption: \"text\""）
	 *         返回空串表示不是简单xml元素，返回null表示省略此样式
	 */
	private String convertSimplePropertyToGpad(String xmlTagName, LinkedHashMap<String, String> attrs) {
		// 检查是否需要通过名字映射转换（XML 元素名 -> Gpad 属性名）
		String gpadName = GpadStyleMaps.XML_TO_GPAD_NAME_MAP.get(xmlTagName);
		if (gpadName == null)
			gpadName = xmlTagName;
		// 从 PROPERTY_INFO 获取属性信息（key 是 Gpad 属性名）
		GpadStyleMaps.PropertyInfo propInfo = GpadStyleMaps.PROPERTY_INFO.get(gpadName);
		if (propInfo == null) // 如果找不到，说明不是已知的属性
			return "";

		Integer gkType = propInfo.type;
		// 确定 XML 属性名（检查是否有特殊属性名映射）
		String attrName = GpadStyleMaps.GPAD_TO_XML_ATTR_NAME_MAP.getOrDefault(gpadName, "val");

		// 获取属性值
		String value = attrs != null ? attrs.get(attrName) : null;
		if (value == null && attrs != null && attrs.size() == 1) {
			// 如果没有找到特殊属性名，尝试使用唯一的属性值
			value = attrs.values().iterator().next();
			if (value == null)
				return null; // 找不到值，省略
		}

		// 检查是否是默认值，如果是则返回 null 表示省略
		String defaultValue = propInfo.defaultValue;
		if (defaultValue != null) {
			// 对于数值类型，需要比较数值是否相等
			if (gkType == GpadStyleMaps.GK_INT || gkType == GpadStyleMaps.GK_FLOAT) {
				try {
					if (gkType == GpadStyleMaps.GK_INT) {
						// 整数比较
						if (Integer.parseInt(value) == Integer.parseInt(defaultValue))
							return null; // 默认值，省略
					} else {
						// 浮点数比较（包括 NaN 的特殊处理）
						if ("NaN".equals(defaultValue)) {
							// 检查 value 是否是 NaN
							if ("NaN".equals(value))
								return null; // 默认值 NaN，省略
							if (Double.isNaN(Double.parseDouble(value)))
								return null; // 默认值 NaN，省略
						} else {
							double val = Double.parseDouble(value);
							double def = Double.parseDouble(defaultValue);
							// 使用小的 epsilon 来比较浮点数
							if (Math.abs(val - def) < 1e-9)
								return null; // 默认值，省略
						}
					}
				} catch (NumberFormatException e) {
					// 如果解析失败，继续处理（可能是表达式）
				}
			} else {
				// 字符串类型，直接比较
				if (value.equals(defaultValue))
					return null; // 默认值，省略
			}
		}

		if (gkType == GpadStyleMaps.GK_INT || gkType == GpadStyleMaps.GK_FLOAT)
			return gpadName + ": " + value;

		// 检查是否需要值转换（通过 VALUE_MAPS_REVERSE）
		if (GpadStyleMaps.VALUE_MAPS_REVERSE.containsKey(xmlTagName)) {
			Map<String, String> valueMap = GpadStyleMaps.VALUE_MAPS_REVERSE.get(xmlTagName);
			String convertedValue = valueMap.get(value);
			if (convertedValue != null)
				value = convertedValue;
		}

		if (gkType == GpadStyleMaps.GK_BOOL) // 省略布尔false值(不出现默认就是false)
			return "true".equals(value)? gpadName: "";

		// 现在只能是字符串值，转换为 Gpad 格式（如果有特殊字符，需要用引号括起来并转义）
		if (containsSpecialChars(value)) // 注意：这里的顺序很重要
			value = "\"" + value.replace("\\", "\\\\")
						 .replace("\"", "\\\"")
						 .replace("\n", "\\n")
						 .replace("\r", "\\r") + "\"";
		return gpadName + ": " + value;
	}

    // 特殊字符：双引号、分号、右大括号、空格、制表符、回车、换行
    // 这些字符在 GK_STR 值中必须用引号括起来
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[;\"}\t \r\n]");

	private static boolean containsSpecialChars(String text) {
        return SPECIAL_CHARS_PATTERN.matcher(text).find();
    }

	/**
	 * Converts boundingBox XML attributes to Gpad format.
	 * Syntax: boundingBox: width=<value> height=<value>;
	 * Both width and height are integers (decimal part ignored when converting from XML)
	 * 
	 * @param attrs boundingBox attributes map
	 * @return Gpad boundingBox string (e.g., "width=100 height=200")
	 */
	private String convertBoundingBox(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String width = attrs.get("width");
		if (width != null) {
			// Extract integer part (ignore decimal if present)
			int dotIndex = width.indexOf('.');
			if (dotIndex >= 0)
				width = width.substring(0, dotIndex);
			if (!first)
				sb.append(" ");
			sb.append("width=").append(width);
			first = false;
		}

		String height = attrs.get("height");
		if (height != null) {
			// Extract integer part (ignore decimal if present)
			int dotIndex = height.indexOf('.');
			if (dotIndex >= 0)
				height = height.substring(0, dotIndex);
			if (!first)
				sb.append(" ");
			sb.append("height=").append(height);
			first = false;
		}

		return sb.toString();
	}

	/**
	 * Converts contentSize XML attributes to Gpad format.
	 * Syntax: contentSize: width=<value> height=<value>;
	 * Both width and height are floats
	 * 
	 * @param attrs contentSize attributes map
	 * @return Gpad contentSize string (e.g., "width=100.5 height=200.3")
	 */
	private String convertContentSize(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String width = attrs.get("width");
		if (width != null) {
			if (!first)
				sb.append(" ");
			sb.append("width=").append(width);
			first = false;
		}

		String height = attrs.get("height");
		if (height != null) {
			if (!first)
				sb.append(" ");
			sb.append("height=").append(height);
			first = false;
		}

		return sb.toString();
	}

	/**
	 * Converts cropBox XML attributes to Gpad format.
	 * Syntax: cropBox: x=<value> y=<value> width=<value> height=<value> [cropped|~cropped];
	 * x, y, width, height are floats, cropped is boolean (cropped for true, ~cropped for false)
	 * 
	 * @param attrs cropBox attributes map
	 * @return Gpad cropBox string (e.g., "x=10 y=20 width=100 height=200 cropped")
	 */
	private String convertCropBox(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String x = attrs.get("x");
		if (x != null) {
			if (!first)
				sb.append(" ");
			sb.append("x=").append(x);
			first = false;
		}

		String y = attrs.get("y");
		if (y != null) {
			if (!first)
				sb.append(" ");
			sb.append("y=").append(y);
			first = false;
		}

		String width = attrs.get("width");
		if (width != null) {
			if (!first)
				sb.append(" ");
			sb.append("width=").append(width);
			first = false;
		}

		String height = attrs.get("height");
		if (height != null) {
			if (!first)
				sb.append(" ");
			sb.append("height=").append(height);
			first = false;
		}

		String cropped = attrs.get("cropped");
		if (cropped != null) {
			// Only output if cropped=true (default is false, so false is not output)
			if ("true".equals(cropped)) {
				if (!first)
					sb.append(" ");
				sb.append("cropped");
			}
			// If cropped=false, don't output (default value)
		}

		return sb.toString();
	}

	/**
	 * Converts dimensions XML attributes to Gpad format.
	 * Syntax: dimensions: width=<value> height=<value> [angle=<value>] [scaled|~scaled];
	 * width, height, angle are floats, scaled is boolean (scaled for true, ~scaled for false)
	 * Note: unscaled default is true in XML, so we use scaled (inverse) in gpad syntax
	 * 
	 * @param attrs dimensions attributes map
	 * @return Gpad dimensions string (e.g., "width=100 height=200 angle=45 scaled")
	 */
	private String convertDimensions(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String width = attrs.get("width");
		if (width != null) {
			if (!first)
				sb.append(" ");
			sb.append("width=").append(width);
			first = false;
		}

		String height = attrs.get("height");
		if (height != null) {
			if (!first)
				sb.append(" ");
			sb.append("height=").append(height);
			first = false;
		}

		String angle = attrs.get("angle");
		if (angle != null) {
			// Only output if angle is not default (0)
			try {
				double angleValue = Double.parseDouble(angle);
				if (Math.abs(angleValue) > 1e-9) { // Not zero (with epsilon)
					if (!first)
						sb.append(" ");
					sb.append("angle=").append(angle);
					first = false;
				}
			} catch (NumberFormatException e) {
				// If not a valid number, output as-is (might be an expression)
				if (!first)
					sb.append(" ");
				sb.append("angle=").append(angle);
				first = false;
			}
		}

		String unscaled = attrs.get("unscaled");
		if (unscaled != null) {
			// Convert unscaled to scaled (inverse)
			// unscaled=true means scaled=false, so would output ~scaled
			// unscaled=false means scaled=true, so output scaled
			// But unscaled default is true (scaled default is false), so only output scaled when unscaled=false
			if ("false".equals(unscaled)) {
				// unscaled=false means scaled=true, output scaled
				if (!first)
					sb.append(" ");
				sb.append("scaled");
			}
			// If unscaled=true (default), don't output ~scaled (default value)
		}

		return sb.toString();
	}
}
