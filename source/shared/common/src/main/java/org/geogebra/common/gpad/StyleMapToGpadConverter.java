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
		if (attrs == null || attrs.isEmpty()) {
			// Boolean property without value
			return tagName;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(tagName).append(": ");

		// Handle different property types
		switch (tagName) {
		case "lineStyle":
			// lineStyle: thickness=4 opacity=178 type=0
			sb.append(convertKeyValuePairs(attrs));
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
}
