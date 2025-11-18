package org.geogebra.common.gpad;

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
		String gpadPropertyName = xmlTagName;

		// 检查是否是直接的属性
		Integer gkType = GpadStyleMaps.GK_PROPERTIES.get(xmlTagName);
		if (gkType == null) {
			// 检查是否需要通过名字映射转换
			gpadPropertyName = GpadStyleMaps.XML_TO_GPAD_NAME_MAP.get(xmlTagName);
			if (gpadPropertyName == null || GpadStyleMaps.GK_PROPERTIES.containsKey(gpadPropertyName))
				return "";
		}

		// 确定 XML 属性名（检查是否有特殊属性名映射）
		String attrName = GpadStyleMaps.GPAD_TO_XML_ATTR_NAME_MAP.getOrDefault(gpadPropertyName, "val");

		// 获取属性值
		String value = attrs != null ? attrs.get(attrName) : null;
		if (value == null && attrs != null && attrs.size() == 1) {
			// 如果没有找到特殊属性名，尝试使用唯一的属性值
			value = attrs.values().iterator().next();
			if (value == null)
				return null; // 找不到值，省略
		}

		if (gkType == GpadStyleMaps.GK_INT || gkType == GpadStyleMaps.GK_FLOAT)
			return gpadPropertyName + ": " + value;

		// 检查是否需要值转换（通过 VALUE_MAPS_REVERSE）
		if (GpadStyleMaps.VALUE_MAPS_REVERSE.containsKey(xmlTagName)) {
			Map<String, String> valueMap = GpadStyleMaps.VALUE_MAPS_REVERSE.get(xmlTagName);
			String convertedValue = valueMap.get(value);
			if (convertedValue != null)
				value = convertedValue;
		}

		if (gkType == GpadStyleMaps.GK_BOOL) // 省略布尔false值(不出现默认就是false)
			return "true".equals(value)? gpadPropertyName: "";

		// 现在只能是字符串值，转换为 Gpad 格式（字符串值需要用引号括起来）
		return gpadPropertyName + ": \"" + value + "\"";
	}
}
