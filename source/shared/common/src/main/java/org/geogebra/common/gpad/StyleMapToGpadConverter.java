package org.geogebra.common.gpad;

import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;
import org.geogebra.common.kernel.parser.Parser;

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

		String convertedValue = null;

		switch (tagName) {
		case "lineStyle":
			// lineStyle: dashedlong thickness=5 hidden opacity=128 ~arrow
			convertedValue = convertLineStyle(attrs);
			break;
		case "objColor":
			// objColor: #rrggbbaa | rgb(...) | hsv(...) | hsl(...) [fill=...] [angle=...] [dist=...] [image=...] [symbol=...] [inverse|~inverse]
			convertedValue = convertObjColor(attrs);
			break;
		case "bgColor":
		case "borderColor":
			// bgColor/borderColor: #rrggbb or #rrggbbaa (if alpha is not default)
			convertedValue = convertColorToHex(attrs);
			break;
		case "absoluteScreenLocation": // @screen: 100 200
		case "labelOffset": // labelOffset: 28 75
			if (attrs.containsKey("x") && attrs.containsKey("y"))
				convertedValue = attrs.get("x") + " " + attrs.get("y");
			break;
		case "animation":
			// animation: play +0.1 speed=2
			convertedValue = convertAnimation(attrs);
			if (convertedValue == null || convertedValue.isEmpty())
				return null;
			break;
		case "eqnStyle":
			// eqnStyle: implicit; or eqnStyle: parametric=t;
			if (attrs.containsKey("style")) {
				String style = attrs.get("style");
				StringBuilder eqnSb = new StringBuilder();
				eqnSb.append(style);
				// If style is parametric and has parameter attribute, add =parameter
				if ("parametric".equals(style) && attrs.containsKey("parameter"))
					eqnSb.append("=").append(attrs.get("parameter"));
				convertedValue = eqnSb.toString();
			}
			break;
		case "boundingBox":
			// boundingBox: width=100 height=200
			convertedValue = convertBoundingBox(attrs);
			break;
		case "contentSize":
			// contentSize: width=100.5 height=200.3
			convertedValue = convertContentSize(attrs);
			break;
		case "cropBox":
			// cropBox: x=10 y=20 width=100 height=200 cropped
			convertedValue = convertCropBox(attrs);
			break;
		case "dimensions":
			// dimensions: width=100 height=200 angle=45 scaled
			convertedValue = convertDimensions(attrs);
			break;
		case "font":
			// font: serif size=0.5 plain; or font: ~serif size=2 italic bold;
			convertedValue = convertFont(attrs);
			break;
		case "show":
			// show: object ~label ev1 ~ev2 plane ~3d;
			convertedValue = convertShow(attrs);
			break;
		}

		// Only add property name prefix if converted value is not null and not empty
		if (convertedValue == null || convertedValue.isEmpty())
			return null;

		String gpadName = GpadStyleMaps.XML_TO_GPAD_NAME_MAP.getOrDefault(tagName, tagName);
		return gpadName + ": " + convertedValue;
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
			if (isNotSimpleNumber(step))
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
			if (isNotSimpleNumber(speed))
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
	private boolean isNotSimpleNumber(String value) {
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
	 * Converts RGB color values to hex format.
	 * Outputs #rrggbb if alpha is default (1.0), otherwise #rrggbbaa.
	 * 
	 * @param rStr red value as string (0-255)
	 * @param gStr green value as string (0-255)
	 * @param bStr blue value as string (0-255)
	 * @param alphaStr alpha value as string (0.0-1.0), can be null
	 * @return hex color string (e.g., "#FF0000" or "#FF0000FF"), or "#000000" on parse error
	 */
	private String convertRgbToHex(String rStr, String gStr, String bStr, String alphaStr) {
		try {
			// Convert r, g, b to hex
			int r = rStr == null ? 0 : Integer.parseInt(rStr);
			int g = gStr == null ? 0 : Integer.parseInt(gStr);
			int b = bStr == null ? 0 : Integer.parseInt(bStr);

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
	 * Converts color attributes (r, g, b, alpha) to hex format.
	 * Outputs #rrggbb if alpha is default (1.0 or "ff"), otherwise #rrggbbaa.
	 * 
	 * @param attrs
	 *            color attributes map
	 * @return hex color string (e.g., "#FF0000" or "#FF0000FF")
	 */
	private String convertColorToHex(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		String rStr = attrs.get("r");
		String gStr = attrs.get("g");
		String bStr = attrs.get("b");
		String alphaStr = attrs.get("alpha");

		if (rStr == null && gStr == null && bStr == null && alphaStr == null)
			return "";

		return convertRgbToHex(rStr, gStr, bStr, alphaStr);
	}

	/**
	 * Converts objColor XML attributes to Gpad format.
	 * Syntax: [#rrggbbaa | rgb(...) | hsv(...) | hsl(...)] [fill=...] [angle=...] [dist=...] [image=...] [symbol=...] [inverse|~inverse]
	 * 
	 * @param attrs objColor attributes map
	 * @return Gpad objColor string (e.g., "#FF0000" or "rgb(x, y, z) fill=hatch angle=45")
	 */
	private String convertObjColor(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Check for dynamic colors
		String dynamicr = attrs.get("dynamicr");
		String dynamicg = attrs.get("dynamicg");
		String dynamicb = attrs.get("dynamicb");
		String dynamica = attrs.get("dynamica");
		String colorSpace = attrs.get("colorSpace");

		if (dynamicr != null && dynamicg != null && dynamicb != null) {
			// Dynamic color
			// Determine color space (default is RGB = 0)
			int colorSpaceInt = 0; // COLORSPACE_RGB
			if (colorSpace != null) {
				try {
					colorSpaceInt = Integer.parseInt(colorSpace);
				} catch (NumberFormatException e) {
					// Default to RGB
				}
			}

			// Determine function name based on color space
			String funcName;
			switch (colorSpaceInt) {
			case 1: // COLORSPACE_HSB
				funcName = "hsv";
				break;
			case 2: // COLORSPACE_HSL
				funcName = "hsl";
				break;
			default: // COLORSPACE_RGB (0)
				funcName = "rgb";
				break;
			}

			// Build function call
			sb.append(funcName).append("(");
			
			// Simplify and add parameters
			sb.append(simplifyExpression(dynamicr));
			sb.append(",");
			sb.append(simplifyExpression(dynamicg));
			sb.append(",");
			sb.append(simplifyExpression(dynamicb));
			
			// Add alpha if present
			if (dynamica != null && !dynamica.isEmpty()) {
				sb.append(",");
				sb.append(simplifyExpression(dynamica));
			}
			
			sb.append(")");
			first = false;
		} else {
			// Static color - use hex format
			String s = convertColorToHex(attrs);
			if (s != null && !s.isEmpty()) {
				sb.append(s);
				first = false;
			}
		}

		// Add fill type (default is STANDARD = 0, so only output if not default)
		String fillTypeStr = attrs.get("fillType");
		if (fillTypeStr != null) {
			try {
				int fillTypeInt = Integer.parseInt(fillTypeStr);
				if (fillTypeInt != 0) { // Not STANDARD
					String fillTypeGpad = GpadStyleMaps.FILL_TYPE_REVERSE_MAP.get(fillTypeStr);
					if (fillTypeGpad != null) {
						if (!first)
							sb.append(" ");
						sb.append("fill=").append(fillTypeGpad);
						first = false;
					}
				}
			} catch (NumberFormatException e) {
				// Ignore invalid fillType
			}
		}

		// Add hatch angle (default is 45, so only output if different)
		String hatchAngleStr = attrs.get("hatchAngle");
		if (hatchAngleStr != null) {
			try {
				int hatchAngle = Integer.parseInt(hatchAngleStr);
				if (hatchAngle != 45) { // Not default
					if (!first)
						sb.append(" ");
					sb.append("angle=").append(hatchAngle);
					first = false;
				}
			} catch (NumberFormatException e) {
				// Ignore invalid hatchAngle
			}
		}

		// Add hatch distance (default is 10, so only output if different)
		String hatchDistanceStr = attrs.get("hatchDistance");
		if (hatchDistanceStr != null) {
			try {
				int hatchDistance = Integer.parseInt(hatchDistanceStr);
				if (hatchDistance != 10) { // Not default
					if (!first)
						sb.append(" ");
					sb.append("dist=").append(hatchDistance);
					first = false;
				}
			} catch (NumberFormatException e) {
				// Ignore invalid hatchDistance
			}
		}

		// Add image (if present)
		String image = attrs.get("image");
		if (image != null && !image.isEmpty()) {
			if (!first)
				sb.append(" ");
			sb.append("image=");
			// Image path may contain special characters, so escape if needed
			if (containsSpecialChars(image))
				sb.append("\"").append(escapeString(image)).append("\"");
			else
				sb.append(image);
			first = false;
		}

		// Add fill symbol (if present)
		String fillSymbol = attrs.get("fillSymbol");
		if (fillSymbol != null && !fillSymbol.isEmpty()) {
			// Only use the first character
			char firstChar = fillSymbol.charAt(0);
			// Check if it's a printable Unicode character (>= 0x20 and not 0x7F)
			if (firstChar >= 0x20 && firstChar != 0x7F) {
				if (!first)
					sb.append(" ");
				sb.append("symbol=").append(firstChar);
				first = false;
			}
			// If not printable, omit it (equivalent to no fillSymbol attribute)
		}

		// Add inverse fill (default is false, so only output if true)
		// Note: ~inverse is used to clear previous style sheet settings, not for XML conversion
		String inverseFillStr = attrs.get("inverseFill");
		if (inverseFillStr != null) {
			if ("true".equals(inverseFillStr)) {
				if (!first)
					sb.append(" ");
				sb.append("inverse");
				first = false;
			}
			// If false, don't output (default value)
		}

		return sb.toString();
	}

	/**
	 * Escapes special characters in a string for use in quoted Gpad strings.
	 * Escapes: backslash, double quote, newline, carriage return.
	 * Note: tab character (\t) does not need escaping in quoted strings.
	 * 
	 * @param str the string to escape
	 * @return the escaped string
	 */
	private String escapeString(String str) {
		return str.replace("\\", "\\\\")
				  .replace("\"", "\\\"")
				  .replace("\n", "\\n")
				  .replace("\r", "\\r");
	}

	/**
	 * Simplifies an expression by checking if it's a "simple" expression according to grammar rules.
	 * If the expression can be parsed by gpadObjColorExpression() without quotes and consumes all input, it's simple.
	 * Otherwise, it wraps the expression in quotes and escapes special characters.
	 * 
	 * @param expr the expression to simplify
	 * @return simplified expression (either as-is or quoted with escapes)
	 */
	private String simplifyExpression(String expr) {
		if (expr == null || expr.isEmpty())
			return "";

		// First, check if expression contains special characters that definitely need quotes
		// These characters cannot appear in unquoted expressions in GK_objColor_BLOCK
		if (containsSpecialChars(expr))
			return "\"" + escapeString(expr) + "\"";

		// No obvious special characters, try to parse the expression using the grammar rules
		// If it parses successfully and consumes all input, it's a "simple" expression and doesn't need quotes
		boolean needsQuotes = true;
		try {
			Parser parser = new Parser();
			parser.ReInit(new org.geogebra.common.kernel.parser.StringProvider(expr.trim()));
			parser.token_source.SwitchTo(org.geogebra.common.kernel.parser.ParserConstants.GK_objColor_BLOCK);
			parser.gpadObjColorExpression();
			// Check if all input was consumed by checking if next token is EOF
			// getToken(0) returns the current token, getToken(1) returns the next token
			org.geogebra.common.kernel.parser.Token nextToken = parser.getToken(1);
			needsQuotes = !(nextToken != null && nextToken.kind == org.geogebra.common.kernel.parser.ParserConstants.EOF);
			// if all input consumed, it's a simple expression
		} catch (Exception e) {
			// Parsing failed or other exception, expression needs quotes
			// needsQuotes is already true, no need to set it again
		}

		if (!needsQuotes) // No special characters, return as-is
			return expr;

		// Parsing failed or didn't consume all input, wrap in quotes and escape
		return "\"" + escapeString(expr) + "\"";
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
		if (value == null)
			return null; // 找不到值，省略

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
			value = "\"" + escapeString(value) + "\"";
		return gpadName + ": " + value;
	}

    // 特殊字符：分号、双引号、右大括号、空格、制表符、回车、换行
    // 这些字符在 GK_STR 值和 objColor 表达式中必须用引号括起来
    // 注意：逗号不需要在此检测，因为语法分析可以处理包含逗号的合法表达式（如函数参数）
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

	/**
	 * Converts font XML attributes to Gpad format.
	 * Syntax: font: [serif|~serif] [*sizeM] [plain|bold|italic|bold italic|italic bold];
	 * serif: serif (true) or ~serif (false), default is false (omit if false)
	 * sizeM: multiplier (float), default is 1.0 (omit if 1.0)
	 * style: plain (0), bold (1), italic (2), or bold italic/italic bold (3), default is plain (omit if 0)
	 * 
	 * @param attrs font attributes map
	 * @return Gpad font string (e.g., "serif *0.5 plain" or "~serif *2 italic bold")
	 */
	private String convertFont(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert serif (only output if true, false is default so omit)
		String serif = attrs.get("serif");
		if (serif != null && "true".equals(serif)) {
			if (!first)
				sb.append(" ");
			sb.append("serif");
			first = false;
		}

		// Convert sizeM (only output if not default 1.0)
		String sizeM = attrs.get("sizeM");
		if (sizeM != null) {
			try {
				double sizeValue = Double.parseDouble(sizeM);
				// Only output if not default (1.0)
				if (Math.abs(sizeValue - 1.0) > 1e-9) {
					if (!first)
						sb.append(" ");
					sb.append("*").append(sizeM);
					first = false;
				}
			} catch (NumberFormatException e) {
				// If not a valid number, ignore sizeM
			}
		}

		// Convert style (0=plain, 1=bold, 2=italic, 3=bold+italic)
		String styleValue = attrs.get("style");
		if (styleValue != null) {
			try {
				int style = Integer.parseInt(styleValue);
				if (style >=1 && style <= 3) { // Only output if not plain (default)
					if (!first)
						sb.append(" ");
					switch (style) {
					case 1:
						sb.append("bold");
						break;
					case 2:
						sb.append("italic");
						break;
					default:
						sb.append("italic bold");
						break;
					}
					first = false;
				}
			} catch (NumberFormatException e) {
				// If not a valid number, ignore style
			}
		}

		return sb.toString();
	}

	/**
	 * Converts show attributes to Gpad format.
	 * Syntax: show: [object|~object] [label|~label] [ev1|~ev1] [ev2|~ev2] [3d|~3d] [plane|~plane];
	 * Default values: object="true", label="true", ev=0 (bit 0=0, bit 1=0)
	 * Note: ev1 and ~ev2 are no-ops when bit 0=0 and bit 1=0 (default state), so don't output them
	 * 
	 * @param attrs show attributes (object, label, ev)
	 * @return Gpad show string (e.g., "~label ~ev1 ev2 plane ~3d")
	 */
	private String convertShow(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert object attribute (default is "true", only output if "false")
		String object = attrs.get("object");
		if (object != null && "false".equals(object)) {
			if (!first)
				sb.append(" ");
			sb.append("~object");
			first = false;
		}

		// Convert label attribute (default is "true", only output if "false")
		String label = attrs.get("label");
		if (label != null && "false".equals(label)) {
			if (!first)
				sb.append(" ");
			sb.append("~label");
			first = false;
		}

		// Convert ev attribute (bitmask)
		// Default ev=0 means: bit 0=0 (visible in EV1), bit 1=0 (hidden in EV2)
		// ev1 clears bit 0, ~ev2 clears bit 1 - both are no-ops when bits are already 0
		String evStr = attrs.get("ev");
		if (evStr != null) {
			try {
				int ev = Integer.parseInt(evStr);
				
				// Bit 0 (mask=1): if set -> ~ev1, if clear -> don't output ev1 (no-op)
				if ((ev & 1) != 0) {
					if (!first)
						sb.append(" ");
					sb.append("~ev1");
					first = false;
				}
				// else: bit 0 is clear (default), ev1 would be no-op, so don't output

				// Bit 1 (mask=2): if set -> ev2, if clear -> don't output ~ev2 (no-op)
				if ((ev & 2) != 0) {
					if (!first)
						sb.append(" ");
					sb.append("ev2");
					first = false;
				}
				// else: bit 1 is clear (default), ~ev2 would be no-op, so don't output

				// Bits 2 (mask=4) and 3 (mask=8): 3d handling
				boolean bit2 = (ev & 4) != 0;
				boolean bit3 = (ev & 8) != 0;
				if (bit2 && !bit3) {
					// Bit 2 set, bit 3 clear -> 3d
					if (!first)
						sb.append(" ");
					sb.append("3d");
					first = false;
				} else if (!bit2 && bit3) {
					// Bit 2 clear, bit 3 set -> ~3d
					if (!first)
						sb.append(" ");
					sb.append("~3d");
					first = false;
				}
				// else: both bits are 0 (default) or both are set (invalid state), don't output

				// Bits 4 (mask=16) and 5 (mask=32): plane handling
				boolean bit4 = (ev & 16) != 0;
				boolean bit5 = (ev & 32) != 0;
				if (bit4 && !bit5) {
					// Bit 4 set, bit 5 clear -> plane
					if (!first)
						sb.append(" ");
					sb.append("plane");
					first = false;
				} else if (!bit4 && bit5) {
					// Bit 4 clear, bit 5 set -> ~plane
					if (!first)
						sb.append(" ");
					sb.append("~plane");
					first = false;
				}
				// else: both bits are 0 (default) or both are set (invalid state), don't output
			} catch (NumberFormatException e) {
				// If ev is not a valid number, ignore it
			}
		}

		return sb.toString();
	}
}
