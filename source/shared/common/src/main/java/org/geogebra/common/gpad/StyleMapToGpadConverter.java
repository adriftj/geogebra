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
		if (styleMap == null || styleMap.isEmpty()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("$").append(name).append(" = {");

		boolean first = true;
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : styleMap.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();

			if (!first) {
				sb.append(";");
			}
			sb.append(" ");

			// Convert XML tag name and attributes to Gpad format
			String gpadProperty = convertPropertyToGpad(tagName, attrs);
			if (gpadProperty != null && !gpadProperty.isEmpty()) {
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
	 * @param tagName
	 *            XML tag name (e.g., "lineStyle", "objColor")
	 * @param attrs
	 *            attribute map
	 * @return Gpad property string (e.g., "lineStyle: thickness=4 opacity=178")
	 */
	private String convertPropertyToGpad(String tagName, LinkedHashMap<String, String> attrs) {
		// Check if this is a GK_BOOL property
		String gpadPropertyName = convertBooleanPropertyName(tagName);
		if (gpadPropertyName != null) // This is a boolean property
			return convertBooleanPropertyToGpad(tagName, gpadPropertyName, attrs);

		if (attrs == null || attrs.isEmpty()) {
			// Boolean property without value
			return tagName;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(tagName).append(": ");

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
		case "labelOffset":
			// labelOffset: 28, 75
			if (attrs.containsKey("x") && attrs.containsKey("y")) {
				sb.append(attrs.get("x")).append(", ").append(attrs.get("y"));
			} else if (attrs.containsKey("val")) {
				sb.append(attrs.get("val"));
			}
			break;
		case "animation":
			// animation: play +0.1 2x
			sb.append(convertAnimation(attrs));
			break;
		case "absoluteScreenLocation":
			// @screen: 100 200
			if (attrs.containsKey("x") && attrs.containsKey("y")) {
				sb.append("@screen: ").append(attrs.get("x")).append(" ").append(attrs.get("y"));
			}
			break;
		case "angleStyle":
			// angleStyle: "0-360"
			if (attrs.containsKey("val")) {
				sb.append("\"").append(attrs.get("val")).append("\"");
			}
			break;
		case "eqnStyle":
			// eqnStyle: implicit; or eqnStyle: parametric=t;
			if (attrs.containsKey("style")) {
				String style = attrs.get("style");
				sb.append(style);
				// If style is parametric and has parameter attribute, add =parameter
				if ("parametric".equals(style) && attrs.containsKey("parameter")) {
					sb.append("=").append(attrs.get("parameter"));
				}
			}
			break;
		default:
			// Generic property: use "val" if present, otherwise convert all attributes
			if (attrs.containsKey("val") && attrs.size() == 1) {
				sb.append(attrs.get("val"));
			} else {
				sb.append(convertKeyValuePairs(attrs));
			}
			break;
		}

		return sb.toString();
	}

	/**
	 * Converts a map of key-value pairs to Gpad format.
	 * 
	 * @param attrs
	 *            attribute map
	 * @return Gpad string (e.g., "thickness=4 opacity=178")
	 */
	private String convertKeyValuePairs(LinkedHashMap<String, String> attrs) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : attrs.entrySet()) {
			if (!first) {
				sb.append(" ");
			}
			sb.append(entry.getKey()).append("=").append(entry.getValue());
			first = false;
		}
		return sb.toString();
	}

	/**
	 * Converts animation attributes to Gpad format.
	 * 
	 * @param attrs
	 *            animation attributes
	 * @return Gpad animation string (e.g., "play +0.1 2x")
	 */
	private String convertAnimation(LinkedHashMap<String, String> attrs) {
		StringBuilder sb = new StringBuilder();
		boolean hasAny = false;

		// Playing state
		if ("true".equals(attrs.get("playing"))) {
			sb.append("play");
			hasAny = true;
		}

		// Step with prefix
		String step = attrs.get("step");
		if (step != null) {
			if (hasAny) {
				sb.append(" ");
			}
			String type = attrs.get("type");
			if (type != null) {
				switch (type) {
				case "1": // ANIMATION_INCREASING
					sb.append("+");
					break;
				case "2": // ANIMATION_DECREASING
					sb.append("-");
					break;
				case "3": // ANIMATION_INCREASING_ONCE
					sb.append("~");
					break;
				case "0": // ANIMATION_OSCILLATING
				default:
					// No prefix
					break;
				}
			}
			sb.append(step);
			hasAny = true;
		}

		// Speed
		String speed = attrs.get("speed");
		if (speed != null && !"1".equals(speed)) {
			if (hasAny) {
				sb.append(" ");
			}
			sb.append(speed).append("x");
		}

		return sb.toString();
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
		if (attrs == null || attrs.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert type (no prefix)
		String typeValue = attrs.get("type");
		if (typeValue != null) {
			String typeKey = GpadStyleMaps.LINE_STYLE_TYPE_REVERSE_MAP.get(typeValue);
			if (typeKey != null) {
				if (!first) {
					sb.append(" ");
				}
				sb.append(typeKey);
				first = false;
			}
		}

		// Convert thickness=value
		String thickness = attrs.get("thickness");
		if (thickness != null) {
			if (!first) {
				sb.append(" ");
			}
			sb.append("thickness=").append(thickness);
			first = false;
		}

		// Convert typeHidden (hidden, hidden=dashed, or hidden=show)
		String typeHiddenValue = attrs.get("typeHidden");
		if (typeHiddenValue != null) {
			if (!first) {
				sb.append(" ");
			}
			String typeHiddenKey = GpadStyleMaps.LINE_STYLE_TYPE_HIDDEN_REVERSE_MAP.get(typeHiddenValue);
			if (typeHiddenKey != null) {
				if (typeHiddenKey.isEmpty()) {
					sb.append("hidden");
				} else {
					sb.append("hidden=").append(typeHiddenKey);
				}
			} else {
				// Default to hidden if value not recognized
				sb.append("hidden");
			}
			first = false;
		}

		// Convert opacity=value
		String opacity = attrs.get("opacity");
		if (opacity != null) {
			if (!first) {
				sb.append(" ");
			}
			sb.append("opacity=").append(opacity);
			first = false;
		}

		// Convert drawArrow (arrow or ~arrow)
		String drawArrow = attrs.get("drawArrow");
		if (drawArrow != null) {
			if (!first) {
				sb.append(" ");
			}
			if ("true".equals(drawArrow)) {
				sb.append("arrow");
			} else if ("false".equals(drawArrow)) {
				sb.append("~arrow");
			}
			first = false;
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
	 * 检查并转换布尔属性的名字（XML -> Gpad）
	 * 
	 * @param xmlTagName
	 *            XML 标签名
	 * @return Gpad 属性名，如果不是布尔属性则返回 null
	 */
	private String convertBooleanPropertyName(String xmlTagName) {
		// 首先检查是否是直接的 GK_BOOL 属性
		if (GpadStyleMaps.GK_BOOL_PROPERTIES.containsKey(xmlTagName))
			return xmlTagName;

		// 检查是否需要通过名字映射转换
		String gpadName = GpadStyleMaps.XML_TO_GPAD_NAME_MAP.get(xmlTagName);
		if (gpadName != null && GpadStyleMaps.GK_BOOL_PROPERTIES.containsKey(gpadName))
			return gpadName;

		return null;
	}

	/**
	 * 转换布尔属性到 Gpad 格式
	 * 
	 * 转换逻辑：
	 * 1. 用 Gpad 属性名查 gpadToXmlAttrNameMap 得到 XML 属性名（如果没有，默认是 "val"）
	 * 2. 用 Gpad 属性名查 xmlToGpadNameMap 的反向得到 XML 元素名（实际上我们已经有了 xmlTagName）
	 * 3. 用 XML 元素名查 booleanValueRevertMap 判断是否需要反转布尔值
	 * 
	 * 特殊情况：
	 * - hideLabelInAlgebra (gpad) -> algebra (xml元素) + labelVisible (xml属性)
	 *   由于 labelVisible 在 XML 中是反义的（labelVisible=true 表示隐藏），
	 *   所以 labelVisible 的值需要反转才能得到正确的 Gpad 布尔值
	 * - showGeneralAngle (gpad) -> emphasizeRightAngle (xml元素)
	 *   由于 emphasizeRightAngle 需要反转，所以 XML 的 val 值需要反转
	 * 
	 * @param xmlTagName
	 *            XML 标签名
	 * @param gpadPropertyName
	 *            Gpad 属性名
	 * @param attrs
	 *            属性映射
	 * @return Gpad 格式的字符串（如 "autocolor;" 或 "~autocolor;"）
	 */
	private String convertBooleanPropertyToGpad(String xmlTagName, String gpadPropertyName,
			LinkedHashMap<String, String> attrs) {
		// 1. 确定 XML 属性名（检查是否有特殊属性名映射）
		String attrName = GpadStyleMaps.GPAD_TO_XML_ATTR_NAME_MAP.getOrDefault(gpadPropertyName, "val");

		// 2. 获取属性值
		String value = attrs != null ? attrs.get(attrName) : null;
		if (value == null && attrs != null && attrs.size() == 1) {
			// 如果没有找到特殊属性名，尝试使用唯一的属性值
			value = attrs.values().iterator().next();
		}

		// 3. 解析布尔值
		boolean boolValue = true; // 默认值
		if (value != null)
			boolValue = "true".equals(value);

		// 4. 检查属性值是否需要转换（如 emphasizeRightAngle）
		if (GpadStyleMaps.VALUE_MAPS_REVERSE.containsKey(xmlTagName))
			boolValue = !boolValue;

		// 5. 转换为 Gpad 格式
		return boolValue? gpadPropertyName: "~" + gpadPropertyName;
	}
}
