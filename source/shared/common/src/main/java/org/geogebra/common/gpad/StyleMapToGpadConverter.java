package org.geogebra.common.gpad;

import java.util.LinkedHashMap;
import java.util.Map;
import org.geogebra.common.kernel.parser.Parser;
import org.geogebra.common.kernel.parser.ParserConstants;
import org.geogebra.common.kernel.parser.StringProvider;
import org.geogebra.common.kernel.parser.Token;

/**
 * Converts style map (Map<String, LinkedHashMap<String, String>>) to Gpad format.
 * This converter takes the XML-style attribute maps and converts them to Gpad syntax.
 */
public class StyleMapToGpadConverter {
	/**
	 * Converts a style map to Gpad style sheet format.
	 * Note: The returned format is "@name = { ... }" (NO semicolon at the end).
	 * Stylesheet definitions do not end with semicolon, unlike command/expression instructions.
	 * 
	 * @param name
	 *            style sheet name (without @ prefix)
	 * @param styleMap
	 *            map from XML tag names to attribute maps
	 * @param objectType
	 *            object type name (e.g., "Button", "Numeric"), can be null
	 * @return Gpad style sheet string in format "@name = { ... }" (no semicolon), or null if styleMap is empty
	 */
	public static String convert(String name, Map<String, LinkedHashMap<String, String>> styleMap, String objectType) {
		if (styleMap == null || styleMap.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("@").append(name).append(" = {");
		boolean hasContent = buildStyleSheetContent(sb, styleMap, objectType);
		if (!hasContent)
			return null;
		sb.append(" }");
		return sb.toString();
	}

	/**
	 * Converts a style map to Gpad style sheet content only (without name).
	 * Returns only the content part "{ ... }" without the "@name = " prefix.
	 * This is useful for comparing stylesheet contents when merging identical stylesheets.
	 * 
	 * @param styleMap
	 *            map from XML tag names to attribute maps
	 * @param objectType
	 *            object type name (e.g., "Button", "Numeric"), can be null
	 * @return Gpad style sheet content string in format "{ ... }" or null if empty
	 */
	public static String convertToContentOnly(Map<String, LinkedHashMap<String, String>> styleMap, String objectType) {
		if (styleMap == null || styleMap.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		boolean hasContent = buildStyleSheetContent(sb, styleMap, objectType);
		if (!hasContent)
			return null;
		sb.append(" }");
		return sb.toString();
	}

	/**
	 * Extracts show.object and show.label from the style map's "show" entry,
	 * removing them from the attrs. Returns [showObject, showLabel] as booleans.
	 * If "show" is absent or object/label are absent, defaults to true.
	 * If the "show" entry becomes empty after extraction, removes it from the map.
	 *
	 * @param styleMap style map (may be modified)
	 * @return boolean[2]: [0]=showObject, [1]=showLabel
	 */
	public static boolean[] extractShowVisibility(Map<String, LinkedHashMap<String, String>> styleMap) {
		boolean showObject = true;
		boolean showLabel = true;
		if (styleMap == null)
			return new boolean[]{showObject, showLabel};
		LinkedHashMap<String, String> showAttrs = styleMap.get("show");
		if (showAttrs == null)
			return new boolean[]{showObject, showLabel};

		String objectVal = showAttrs.remove("object");
		String labelVal = showAttrs.remove("label");
		if ("false".equals(objectVal))
			showObject = false;
		if ("false".equals(labelVal))
			showLabel = false;

		if (showAttrs.isEmpty())
			styleMap.remove("show");

		return new boolean[]{showObject, showLabel};
	}

	/**
	 * Builds the Gpad style sheet content part "{ ... }" from a style map and appends it to the given StringBuilder.
	 * This is the common logic used by both convert() and convertToContentOnly().
	 * 
	 * @param sb
	 *            StringBuilder to append the content to
	 * @param styleMap
	 *            map from XML tag names to attribute maps
	 * @param objectType
	 *            object type name (e.g., "Button", "Numeric"), can be null
	 * @return true if content is not empty, false if empty
	 */
	private static boolean buildStyleSheetContent(StringBuilder sb, Map<String, LinkedHashMap<String, String>> styleMap, String objectType) {
		if (styleMap == null || styleMap.isEmpty())
			return false;

		boolean first = true;
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : styleMap.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();
			String gpadProperty = null;

			// checkbox has its own fixed sub-property, independent from object-level fixed.
			// Syntax: checkbox | checkbox: fixed | checkbox: ~fixed
			if ("checkbox".equals(tagName)) {
				String fixed = attrs != null ? attrs.get("fixed") : null;
				if ("true".equals(fixed))
					gpadProperty = "checkbox: fixed";
				else
					gpadProperty = "checkbox";
			} else if ("value".equals(tagName)) {
				gpadProperty = convertValueElement(attrs, objectType);
			} else {
				if ("ggbscript".equals(tagName) || "javascript".equals(tagName))
					gpadProperty = convertScriptElementToGpad(tagName, attrs);
				else
					gpadProperty = convertPropertyToGpad(tagName, attrs, objectType);
			}

			if (gpadProperty != null && !gpadProperty.isEmpty()) {
				if (!first)
					sb.append(";");
				sb.append(" ");
				sb.append(gpadProperty);
				first = false;
			}
		}

		return !first;
	}

	/**
	 * Converts value element to Gpad format based on object type.
	 * - For Numeric type: converts random attribute to random style (if random="true")
	 * - Otherwise: ignores
	 * 
	 * @param attrs value element attributes
	 * @param objectType object type name (e.g., "Button", "Numeric")
	 * @return Gpad property string, or null if should be omitted
	 */
	private static String convertValueElement(LinkedHashMap<String, String> attrs, String objectType) {
		if (attrs == null || attrs.isEmpty())
			return null;

		// For Numeric type: convert random attribute to random style
		if ("numeric".equalsIgnoreCase(objectType)) {
			if ("true".equals(attrs.get("random")))
				return "random";
		}

		// For other types: ignore
		return null;
	}

	/**
	 * Converts a single property (XML tag + attributes) to Gpad format.
	 * 
	 * @param tagName XML tag name (e.g., "lineStyle", "objColor"/"color")
	 * @param attrs attribute map
	 * @param objectType object type name (e.g., "Button", "Numeric"), can be null
	 * @return Gpad property string (e.g., "lineStyle: thickness=4 opacity=178")
	 */
	private static String convertPropertyToGpad(String tagName, LinkedHashMap<String, String> attrs, String objectType) {
		String prop = convertSimplePropertyToGpad(tagName, attrs);
		if (prop == null)
			return null;
		if (!prop.isEmpty())
			return prop;

		if (attrs == null || attrs.isEmpty())
			return null;

		String convertedValue = null;

		switch (tagName) {
		case "slider":
			convertedValue = convertSlider(attrs);
			break;
		case "tableview":
			convertedValue = convertTableView(attrs);
			break;
		case "spreadsheetTrace":
			convertedValue = convertSpreadsheetTrace(attrs);
			break;
		case "lineStyle":
			convertedValue = convertLineStyle(attrs);
			break;
		case "objColor":
			// color: #rrggbbaa | rgb(...) | hsv(...) | hsl(...) [fill=...] [angle=...] [dist=...] [image=...] [symbol=...] [inverse|~inverse]
			convertedValue = convertObjColor(attrs);
			break;
		case "barTag":
			// barTag: bar=<number> [#rrggbbaa] [fill=...] [angle=...] [dist=...] [image=...] [symbol=...] | bar=<number> ...
			convertedValue = convertBarTag(attrs);
			break;
		case "bgColor":
		case "borderColor":
			// bgColor/borderColor alpha is 0-255 integer (unlike objColor which is 0.0-1.0)
			convertedValue = convertColorToHexIntAlpha(attrs);
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
			// boundingBox: <width> <height>
			convertedValue = convertBoundingBox(attrs);
			break;
		case "contentSize":
			// contentSize: <width> <height>
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
		case "startPoint":
			// startPoint: absolute x y z | "expA"
			convertedValue = convertStartPoint(attrs);
			break;
		case "coords":
			// coords: x y [z]
			// Only for points constrained on a Path
			convertedValue = convertCoords(attrs);
			break;
		case "font":
			// font: serif size=0.5 plain; or font: ~serif size=2 italic bold;
			convertedValue = convertFont(attrs);
			break;
		case "show":
			// ev: ~1 2 3d ~plane;
			convertedValue = convertEv(attrs);
			if (convertedValue == null || convertedValue.isEmpty())
				return null;
			return "ev: " + convertedValue;
		case "tempUserInput":
			// tempUserInput: eval="..." display="...";
			convertedValue = convertTempUserInput(attrs);
			break;
		}

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
	private static String convertAnimation(LinkedHashMap<String, String> attrs) {
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
	private static boolean isNotSimpleNumber(String value) {
		if (value == null || value.isEmpty())
			return false;
		
		// Try to match integer or floating-point number
		// Pattern: optinal +/-, digits, optional decimal point, optional digits
		return !value.matches("^[+-]?\\d+(\\.\\d+)?$");
	}

	/**
	 * Converts lineStyle XML attributes to Gpad format.
	 * Syntax: [type] [thickness] [hidden[=dashed|show]] [opacity=value] [arrow|~arrow]
	 * 
	 * @param attrs
	 *            lineStyle attributes map
	 * @return Gpad lineStyle string
	 */
	private static String convertLineStyle(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String typeValue = attrs.get("type");
		String defaultType = GpadStyleDefaults.getDefaultAttrValue("lineStyle", "type");
		if (typeValue != null && !typeValue.equals(defaultType)) {
			String typeKey = GpadStyleMaps.LINE_STYLE_TYPE_REVERSE_MAP.get(typeValue);
			if (typeKey != null) {
				if (!first)
					sb.append(" ");
				sb.append(typeKey);
				first = false;
			}
		}

		String thickness = attrs.get("thickness");
		String defaultThickness = GpadStyleDefaults.getDefaultAttrValue("lineStyle", "thickness");
		if (thickness != null && !thickness.equals(defaultThickness)) {
			if (!first)
				sb.append(" ");
			sb.append(thickness);
			first = false;
		}

		// Convert typeHidden (hidden, hidden=dashed, or hidden=show)
		String typeHiddenValue = attrs.get("typeHidden");
		// default is 1, so only output when different
		if (typeHiddenValue != null && !"1".equals(typeHiddenValue)) {
			String typeHiddenKey = GpadStyleMaps.LINE_STYLE_TYPE_HIDDEN_REVERSE_MAP.get(typeHiddenValue);
			if (typeHiddenKey != null) {
				if (!first)
					sb.append(" ");
				if (typeHiddenKey.isEmpty())
					sb.append("hidden");
				else
					sb.append("hidden=").append(typeHiddenKey);
				first = false;
			}
		}

		// Convert opacity=value
		String opacity = attrs.get("opacity");
		// default is 255, so only output when different
		if (opacity != null && !"255".equals(opacity)) {
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
	private static String convertRgbToHex(String rStr, String gStr, String bStr, String alphaStr) {
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
			sb.append(toHex2Digits(r));
			sb.append(toHex2Digits(g));
			sb.append(toHex2Digits(b));

			if (alphaStr != null) {
				double alpha;
				try {
					alpha = Double.parseDouble(alphaStr);
				} catch (NumberFormatException e) {
					alpha = 1.0;
				}

				if (alpha < 0) {
					// Negative alpha is a sentinel (e.g., -1 for lists).
					sb.append("00");
				} else {
					int alphaInt = (int) Math.round(alpha * 255);
					alphaInt = Math.max(0, Math.min(255, alphaInt));
					if (alphaInt != 255) {
						sb.append(toHex2Digits(alphaInt));
					}
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
	private static String convertColorToHex(LinkedHashMap<String, String> attrs) {
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
	 * Converts color attributes with integer alpha (0-255) to hex format.
	 * Used for bgColor/borderColor where alpha is stored as integer 0-255.
	 */
	private static String convertColorToHexIntAlpha(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		String rStr = attrs.get("r");
		String gStr = attrs.get("g");
		String bStr = attrs.get("b");
		String alphaStr = attrs.get("alpha");

		if (rStr == null && gStr == null && bStr == null && alphaStr == null)
			return "";

		try {
			int r = rStr == null ? 0 : Integer.parseInt(rStr);
			int g = gStr == null ? 0 : Integer.parseInt(gStr);
			int b = bStr == null ? 0 : Integer.parseInt(bStr);

			r = Math.max(0, Math.min(255, r));
			g = Math.max(0, Math.min(255, g));
			b = Math.max(0, Math.min(255, b));

			StringBuilder sb = new StringBuilder("#");
			sb.append(toHex2Digits(r));
			sb.append(toHex2Digits(g));
			sb.append(toHex2Digits(b));

			if (alphaStr != null) {
				int alphaInt = Integer.parseInt(alphaStr);
				alphaInt = Math.max(0, Math.min(255, alphaInt));
				if (alphaInt != 255) {
					sb.append(toHex2Digits(alphaInt));
				}
			}

			return sb.toString();
		} catch (NumberFormatException e) {
			return "#000000";
		}
	}

	/**
	 * Converts objColor XML attributes to Gpad "color" format.
	 * Syntax: [#rrggbbaa | rgb(...) | hsv(...) | hsl(...)] [fill=...] [angle=...] [dist=...] [image=...] [symbol=...] [inverse|~inverse]
	 * 
	 * @param attrs objColor attributes map
	 * @return Gpad color string (e.g., "#FF0000" or "rgb(x, y, z) fill=hatch angle=45")
	 */
	private static String convertObjColor(LinkedHashMap<String, String> attrs) {
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
				sb.append(quoteString(image));
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
	 * Quotes a string using "..." and/or `...` delimiters with concatenation.
	 * Uses a greedy algorithm to minimise the number of segments.
	 */
	static String quoteString(String str) {
		if (str == null) return "\"\"";
		if (!str.contains("\""))
			return "\"" + str + "\"";
		if (!str.contains("`"))
			return "`" + str + "`";
		return splitQuote(str);
	}

	/**
	 * Quotes a string for style-sheet GK_STR / GK_SCRIPT property values.
	 * Uses the same "..." / `...` concatenation scheme as {@link #quoteString}.
	 */
	static String quoteStringForStyleSheet(String str) {
		return quoteString(str);
	}

	/**
	 * Splits a string that contains both {@code "} and {@code `} into
	 * concatenated segments, each wrapped with whichever delimiter allows
	 * the longest run (greedy, O(n), provably minimum segments).
	 */
	private static String splitQuote(String str) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		int len = str.length();
		while (i < len) {
			int nextQuote = str.indexOf('"', i);
			if (nextQuote < 0) nextQuote = len;
			int nextBacktick = str.indexOf('`', i);
			if (nextBacktick < 0) nextBacktick = len;

			if (nextQuote >= nextBacktick) {
				result.append('"').append(str, i, nextQuote).append('"');
				i = nextQuote;
			} else {
				result.append('`').append(str, i, nextBacktick).append('`');
				i = nextBacktick;
			}
		}
		return result.toString();
	}

	/**
	 * Simplifies an expression by checking if it's a "simple" expression according to grammar rules.
	 * If the expression can be parsed by gpadExpr() without quotes and consumes all input, it's simple.
	 * Otherwise, it wraps the expression in quotes and escapes special characters.
	 * 
	 * @param expr the expression to simplify
	 * @return simplified expression (either as-is or quoted with escapes)
	 */
	private static String simplifyExpression(String expr) {
		if (expr == null || expr.isEmpty())
			return "";

		// First, check if expression contains special characters that definitely need quotes
		// These characters cannot appear in unquoted expressions in GK_EXPR_BLOCK
		if (containsSpecialChars(expr))
			return quoteString(expr);

		boolean needsQuotes = true;
		try {
			Parser parser = new Parser();
			parser.ReInit(new StringProvider(expr.trim()));
			parser.token_source.SwitchTo(ParserConstants.GK_EXPR_BLOCK);
			parser.gpadExpr();
			Token nextToken = parser.getToken(1);
			needsQuotes = !(nextToken != null && nextToken.kind == ParserConstants.EOF);
		} catch (Exception e) {
			// Parsing failed, expression needs quotes
		}

		if (!needsQuotes)
			return expr;

		return quoteString(expr);
	}

	/**
	 * 转换简单xml元素(GK_BOOL/GK_INT/GK_FLOAT/GK_STR)的属性map到Gpad样式
	 * 
	 * @param xmlTagName XML标签名
	 * @param attrs 属性映射
	 * @return Gpad 格式的字符串（如 "angleStyle: 0-360" 或 "caption: \"text\""）
	 *         返回空串表示不是简单xml元素，返回null表示省略此样式
	 */
	private static String convertSimplePropertyToGpad(String xmlTagName, LinkedHashMap<String, String> attrs) {
		String gpadName = GpadStyleMaps.XML_TO_GPAD_NAME_MAP.get(xmlTagName);
		if (gpadName == null)
			gpadName = xmlTagName;
		
		Integer gkType = GpadStyleMaps.PROPERTY_INFO.get(gpadName);
		if (gkType == null) {
			return "";
		}
		String attrName = GpadStyleMaps.GPAD_TO_XML_ATTR_NAME_MAP.getOrDefault(gpadName, "val");

		String value = attrs != null ? attrs.get(attrName) : null;
		
		if (value == null)
			return null;

		String defaultValue = GpadStyleMaps.getSimpleDefaultValue(gpadName);
		if (defaultValue != null) {
			if (gkType == GpadStyleMaps.GK_INT || gkType == GpadStyleMaps.GK_FLOAT) {
				try {
					if (gkType == GpadStyleMaps.GK_INT) {
						if (Integer.parseInt(value) == Integer.parseInt(defaultValue))
							return null;
					} else {
						if ("NaN".equals(defaultValue)) {
							if ("NaN".equals(value))
								return null;
							if (Double.isNaN(Double.parseDouble(value)))
								return null;
						} else {
							double val = Double.parseDouble(value);
							double def = Double.parseDouble(defaultValue);
							if (Math.abs(val - def) < 1e-9)
								return null;
						}
					}
				} catch (NumberFormatException e) {
					// expression, continue
				}
			} else {
				if (value.equals(defaultValue))
					return null;
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

		if (gkType == GpadStyleMaps.GK_BOOL) {
			if ("true".equals(value))
				return gpadName;
			else
				return "";
		}

		if ("javascript".equals(gpadName) || containsSpecialChars(value))
			value = quoteStringForStyleSheet(value);
		
		return gpadName + ": " + value;
	}

	private static boolean containsSpecialChars(String text) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == ';' || c == '"' || c == '}' || c == '\t' || c == ' ' || c == '\r' || c == '\n'
					|| c == '[' || c == ']' || c == '(' || c == ')' || c == '{'
					|| c == '\u2221' // ∡ POLAR_SEPARATOR token — must be quoted
					|| isUnicodeWhitespace(c)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isUnicodeWhitespace(char c) {
		return (c >= '\u2000' && c <= '\u200B')
				|| c == '\u00A0' || c == '\u1680' || c == '\u202F'
				|| c == '\u2028' || c == '\u2029' || c == '\u205F'
				|| c == '\u3000' || c == '\uFEFF';
	}

	/**
	 * Converts an integer (0-255) to a 2-digit uppercase hex string.
	 * GWT-compatible replacement for String.format("%02X", value).
	 * 
	 * @param value the integer value (0-255)
	 * @return 2-digit hex string (e.g., "FF", "0A")
	 */
	private static String toHex2Digits(int value) {
		value = Math.max(0, Math.min(255, value));
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		StringBuilder sb = new StringBuilder(2);
		sb.append(hexChars[(value >> 4) & 0xF]);
		sb.append(hexChars[value & 0xF]);
		return sb.toString();
	}

	/**
	 * Converts boundingBox XML attributes to Gpad format.
	 * Syntax: boundingBox: <width> <height>;
	 * Both width and height are integers (decimal part ignored when converting from XML)
	 * 
	 * @param attrs boundingBox attributes map
	 * @return Gpad boundingBox string (e.g., "100 200")
	 */
	private static String convertBoundingBox(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String width = attrs.get("width");
		if (width != null) {
			int dotIndex = width.indexOf('.');
			if (dotIndex >= 0)
				width = width.substring(0, dotIndex);
			if (!first)
				sb.append(" ");
			sb.append(width);
			first = false;
		}

		String height = attrs.get("height");
		if (height != null) {
			int dotIndex = height.indexOf('.');
			if (dotIndex >= 0)
				height = height.substring(0, dotIndex);
			if (!first)
				sb.append(" ");
			sb.append(height);
			first = false;
		}

		return sb.toString();
	}

	/**
	 * Converts contentSize XML attributes to Gpad format.
	 * Syntax: contentSize: <width> <height>;
	 * Both width and height are floats
	 * 
	 * @param attrs contentSize attributes map
	 * @return Gpad contentSize string (e.g., "100.5 200.3")
	 */
	private static String convertContentSize(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String width = attrs.get("width");
		if (width != null) {
			if (!first)
				sb.append(" ");
			sb.append(width);
			first = false;
		}

		String height = attrs.get("height");
		if (height != null) {
			if (!first)
				sb.append(" ");
			sb.append(height);
			first = false;
		}

		return sb.toString();
	}

	/**
	 * Converts tempUserInput XML attributes to Gpad format.
	 * Syntax: tempUserInput: eval="<string>" display="<string>";
	 * Both eval and display are quoted strings (required to be quoted)
	 * 
	 * @param attrs tempUserInput attributes map
	 * @return Gpad tempUserInput string (e.g., "eval=\"x+1\" display=\"x + 1\"")
	 */
	private static String convertTempUserInput(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		String eval = attrs.get("eval");
		if (eval != null) {
			if (!first)
				sb.append(" ");
			sb.append("eval=").append(quoteString(eval));
			first = false;
		}

		String display = attrs.get("display");
		if (display != null) {
			if (!first)
				sb.append(" ");
			sb.append("display=").append(quoteString(display));
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
	private static String convertCropBox(LinkedHashMap<String, String> attrs) {
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
	private static String convertDimensions(LinkedHashMap<String, String> attrs) {
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
	 * Syntax: font: [serif|~serif] [sizeM] [plain|bold|italic|bold italic|italic bold];
	 * serif: serif (true) or ~serif (false), default is false (omit if false)
	 * sizeM: multiplier (float), default is 1.0 (omit if 1.0)
	 * style: plain (0), bold (1), italic (2), or bold italic/italic bold (3), default is plain (omit if 0)
	 * 
	 * @param attrs font attributes map
	 * @return Gpad font string (e.g., "serif *0.5 plain" or "~serif *2 italic bold")
	 */
	private static String convertFont(LinkedHashMap<String, String> attrs) {
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

		// Note: font.size (old absolute pixel size) is not converted to GPAD.
		// Modern GeoGebra uses sizeM (multiplier). The roundtrip comparison
		// skips font.size as a known legacy attribute.

		// Convert sizeM (only output if not default 1.0)
		String sizeM = attrs.get("sizeM");
		if (sizeM != null) {
			try {
				double sizeValue = Double.parseDouble(sizeM);
				// Only output if not default (1.0)
				if (Math.abs(sizeValue - 1.0) > 1e-9) {
					if (!first)
						sb.append(" ");
					sb.append(sizeM);
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
	 * Converts show tag's ev bitmask to the "ev:" GPAD property format.
	 * Syntax: ev: [~1] [2] [3d] [~plane];
	 * Only handles ev/3d/plane bits; object/label are handled as label suffixes.
	 */
	private static String convertEv(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return "";

		String evStr = attrs.get("ev");
		if (evStr == null)
			return "";

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		try {
			int ev = Integer.parseInt(evStr);

			if ((ev & 1) != 0) {
				sb.append("~1");
				first = false;
			}
			if ((ev & 2) != 0) {
				if (!first) sb.append(" ");
				sb.append("2");
				first = false;
			}

			boolean bit2 = (ev & 4) != 0;
			boolean bit3 = (ev & 8) != 0;
			if (bit2 && !bit3) {
				if (!first) sb.append(" ");
				sb.append("3d");
				first = false;
			} else if (!bit2 && bit3) {
				if (!first) sb.append(" ");
				sb.append("~3d");
				first = false;
			}

			boolean bit4 = (ev & 16) != 0;
			boolean bit5 = (ev & 32) != 0;
			if (bit4 && !bit5) {
				if (!first) sb.append(" ");
				sb.append("plane");
				first = false;
			} else if (!bit4 && bit5) {
				if (!first) sb.append(" ");
				sb.append("~plane");
				first = false;
			}
		} catch (NumberFormatException e) {
			return "";
		}

		return sb.toString();
	}

	/**
	 * Converts slider attributes to Gpad format.
	 * Syntax: slider: [min=<表达式>] [max=<表达式>] [width=<浮点数>] [x=<浮点数>] [y=<浮点数>] [vertical|~vertical] [algebra|~algebra] [constant|~constant] [@screen|~@screen] [fixed|~fixed];
	 * Note: @screen, fixed, x, y are independent properties for the slider
	 * 
	 * @param attrs slider attributes
	 * @return Gpad slider string (e.g., "min=0 max=10 width=200 x=100 y=200 vertical algebra @screen fixed")
	 */
	private static String convertSlider(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert min (expression)
		String min = attrs.get("min");
		if (min != null && !min.isEmpty()) {
			if (!first)
				sb.append(" ");
			sb.append("min=");
			sb.append(simplifyExpression(min));
			first = false;
		}

		// Convert max (expression)
		String max = attrs.get("max");
		if (max != null && !max.isEmpty()) {
			if (!first)
				sb.append(" ");
			sb.append("max=");
			sb.append(simplifyExpression(max));
			first = false;
		}

		// Convert width (float)
		String width = attrs.get("width");
		if (width != null && !width.isEmpty()) {
			if (!first)
				sb.append(" ");
			sb.append("width=");
			sb.append(width);
			first = false;
		}

		// Convert x/y coordinates: always output if present
		String x = attrs.get("x");
		if (x != null && !x.isEmpty()) {
			if (!first)
				sb.append(" ");
			sb.append("x=");
			sb.append(x);
			first = false;
		}

		String y = attrs.get("y");
		if (y != null && !y.isEmpty()) {
			if (!first)
				sb.append(" ");
			sb.append("y=");
			sb.append(y);
			first = false;
		}

		// Convert absoluteScreenLocation to @screen
		// Default is false, so only output @screen if true
		String absoluteScreenLocation = attrs.get("absoluteScreenLocation");
		if (absoluteScreenLocation != null && "true".equals(absoluteScreenLocation)) {
			if (!first)
				sb.append(" ");
			sb.append("@screen");
			first = false;
		}
		// If absoluteScreenLocation=false (default), don't output anything

		// Convert fixed
		// Default is false, so only output fixed if true
		String fixed = attrs.get("fixed");
		if (fixed != null && "true".equals(fixed)) {
			if (!first)
				sb.append(" ");
			sb.append("fixed");
			first = false;
		}
		// If fixed=false (default), don't output anything

		// Convert horizontal to vertical (inverse)
		// Default is horizontal (vertical=false), so only output vertical if true
		String horizontal = attrs.get("horizontal");
		if (horizontal != null) {
			boolean isHorizontal = "true".equals(horizontal);
			if (!isHorizontal) { // vertical = true
				if (!first)
					sb.append(" ");
				sb.append("vertical");
				first = false;
			}
			// If horizontal=true (default), don't output anything
		}

		// Convert showAlgebra to algebra
		// Default is false, so only output algebra if true
		String showAlgebra = attrs.get("showAlgebra");
		if (showAlgebra != null && "true".equals(showAlgebra)) {
			if (!first)
				sb.append(" ");
			sb.append("algebra");
			first = false;
		}
		// If showAlgebra=false (default), don't output anything

		// Convert arbitraryConstant to constant
		// Default is false, so only output constant if true
		String arbitraryConstant = attrs.get("arbitraryConstant");
		if (arbitraryConstant != null && "true".equals(arbitraryConstant)) {
			if (!first)
				sb.append(" ");
			sb.append("constant");
			first = false;
		}
		// If arbitraryConstant=false (default), don't output anything

		// If no attributes were output, return null (omit the property)
		if (first)
			return null;

		return sb.toString();
	}

	/**
	 * Converts tableview attributes to Gpad format.
	 * Syntax: tableview: [<整数>] [points|~points];
	 * Examples:
	 *   tableview: 2 points;     // column=2, points=true
	 *   tableview: 1 ~points;    // column=1, points=false
	 *   tableview: points;       // no column, points=true
	 *   tableview: 3;            // column=3, no points
	 *   tableview: ~points;      // no column, points=false
	 * 
	 * @param attrs tableview attributes
	 * @return Gpad tableview string (e.g., "2 points" or "~points")
	 */
	private static String convertTableView(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert column (integer)
		String column = attrs.get("column");
		if (column != null && !column.isEmpty()) {
			// Extract integer part (ignore decimal if present)
			int dotIndex = column.indexOf('.');
			String columnStr = dotIndex >= 0 ? column.substring(0, dotIndex) : column;
			// Only output if column is not -1 (default value meaning not in table)
			try {
				int columnInt = Integer.parseInt(columnStr);
				if (columnInt != -1) {
					if (!first)
						sb.append(" ");
					sb.append(columnStr);
					first = false;
				}
			} catch (NumberFormatException e) {
				// If not a valid number, ignore
			}
		}

		// Convert points (boolean)
		// Default is true, so:
		// - If points=false, output "~points" (always)
		// - If points=true, output "points" (always, regardless of column)
		// Note: When converting from XML, points is always present in attrs
		// (XMLBuilder always outputs it), so we always output it when present
		String points = attrs.get("points");
		if (points != null) {
			if ("false".equals(points)) {
				if (!first)
					sb.append(" ");
				sb.append("~points");
				first = false;
			} else if ("true".equals(points)) {
				// Always output points when explicitly set to true
				// (matches examples: "2 points" and "points")
				if (!first)
					sb.append(" ");
				sb.append("points");
				first = false;
			}
		}

		// If no attributes were output, return null (omit the property)
		// This should only happen if column is -1 and points is not present
		if (first)
			return null;

		return sb.toString();
	}

	/**
	 * Converts spreadsheetTrace XML attributes to Gpad format.
	 * Syntax: [trace|~trace] [column=<int>] [row=<int>[/<int>]] 
	 *         [reset|~reset] [label|~label] [list|~list] [copy|~copy] [pause|~pause]
	 * 
	 * row format:
	 *   row=12/30    -> traceRow1=12, numRows=30, doRowLimit=true
	 *   row=12       -> traceRow1=12, doRowLimit=false, numRows not set
	 * 
	 * @param attrs spreadsheetTrace attributes map
	 * @return Gpad spreadsheetTrace string, or null if should be omitted
	 */
	private static String convertSpreadsheetTrace(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Convert trace (val attribute) - default is false, only output if true
		String val = attrs.get("val");
		if ("true".equals(val)) {
			if (!first)
				sb.append(" ");
			sb.append("trace");
			first = false;
		}

		// Convert column (traceColumn1)
		String traceColumn1 = attrs.get("traceColumn1");
		if (traceColumn1 != null && !traceColumn1.isEmpty()) {
			// Only output if column is not -1 (default value)
			if (!"-1".equals(traceColumn1)) {
				if (!first)
					sb.append(" ");
				sb.append("column=").append(traceColumn1);
				first = false;
			}
		}

		// Convert row (traceRow1, numRows, doRowLimit)
		String traceRow1 = attrs.get("traceRow1");
		if (traceRow1 != null && !traceRow1.isEmpty()) {
			// Only output if traceRow1 is not -1 (default value)
			if (!"-1".equals(traceRow1)) {
				if (!first)
					sb.append(" ");
				
				String doRowLimit = attrs.get("doRowLimit");
				String numRows = attrs.get("numRows");
				
				// Format: row=12/30 if doRowLimit=true and numRows is set
				// Format: row=12 if doRowLimit=false or numRows is not set
				if ("true".equals(doRowLimit) && numRows != null && !numRows.isEmpty())
					sb.append("row=").append(traceRow1).append("/").append(numRows);
				else
					sb.append("row=").append(traceRow1);
				first = false;
			}
		}

		// Convert reset (doColumnReset) - default is false, only output if true
		String doColumnReset = attrs.get("doColumnReset");
		if ("true".equals(doColumnReset)) {
			if (!first)
				sb.append(" ");
			sb.append("reset");
			first = false;
		}

		// Convert label (showLabel) - default is true, only output if false
		String showLabel = attrs.get("showLabel");
		if ("false".equals(showLabel)) {
			if (!first)
				sb.append(" ");
			sb.append("~label");
			first = false;
		}

		// Convert list (showTraceList) - default is false, only output if true
		String showTraceList = attrs.get("showTraceList");
		if ("true".equals(showTraceList)) {
			if (!first)
				sb.append(" ");
			sb.append("list");
			first = false;
		}

		// Convert copy (doTraceGeoCopy) - default is false, only output if true
		String doTraceGeoCopy = attrs.get("doTraceGeoCopy");
		if ("true".equals(doTraceGeoCopy)) {
			if (!first)
				sb.append(" ");
			sb.append("copy");
			first = false;
		}

		// Convert pause - default is false, only output if true
		String pause = attrs.get("pause");
		if ("true".equals(pause)) {
			if (!first)
				sb.append(" ");
			sb.append("pause");
			first = false;
		}

		// If no attributes were output, return null (omit the property)
		if (first)
			return null;

		return sb.toString();
	}
	
	/**
	 * Converts startPoint elements to Gpad format.
	 * startPoint is now serialized in XMLToStyleMapParser as _corners attribute.
	 * This method deserializes it and converts to gpad format.
	 * 
	 * @param attrs startPoint attributes
	 * @return Gpad property string (e.g., "startPoint: absolute 1 2 | \"A\" ~absolute")
	 */
	private static String convertStartPoint(LinkedHashMap<String, String> attrs) {
		if (attrs == null || !attrs.containsKey("_corners"))
			return null;
		
		String serialized = attrs.get("_corners");
		if (serialized == null || serialized.isEmpty())
			return null;
		
		// Collect corners first, then join with " | " to avoid trailing separators
		java.util.List<String> corners = new java.util.ArrayList<>();

		GpadSerializer.deserializeStartPointCorners(serialized, (firstCorner, isAbsolute, cornerData) -> {
			String exp = cornerData[0];
			if (exp != null) {
				StringBuilder csb = new StringBuilder();
				if (isAbsolute)
					csb.append("absolute ");
				csb.append(quoteString(exp));
				corners.add(csb.toString());
			} else {
				String x = cornerData[1];
				String y = cornerData[2];
				String z = cornerData[3];
				if (x != null && !"NaN".equals(x) && y != null && !"NaN".equals(y)) {
					StringBuilder csb = new StringBuilder();
					if (isAbsolute)
						csb.append("absolute ");
					csb.append(formatNumber(x)).append(" ").append(formatNumber(y));
					if (z != null && !"NaN".equals(z))
						csb.append(" ").append(formatNumber(z));
					corners.add(csb.toString());
				} else {
					corners.add("_");
				}
			}
		});

		// Trim trailing empty corners
		while (!corners.isEmpty() && "_".equals(corners.get(corners.size() - 1)))
			corners.remove(corners.size() - 1);

		return corners.isEmpty() ? null : String.join(" | ", corners);
	}

	/**
	 * Converts coords XML attributes to Gpad format.
	 * Syntax: coords: x y [z];
	 * Only for points constrained on a Path.
	 * Defaults: z=1.0
	 * 
	 * @param attrs coords attributes map
	 * @return Gpad coords string (e.g., "2.1 3.2" or "2.3 4.1 3.5")
	 */
	private static String convertCoords(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();

		// x y [z]
		String x = attrs.get("x");
		String y = attrs.get("y");
		if (x == null || y == null)
			return null;

		sb.append(formatNumber(x)).append(" ").append(formatNumber(y));

		String z = attrs.get("z");
		boolean zIsDefault = z == null || "1.0".equals(z);
		if (!zIsDefault)
			sb.append(" ").append(formatNumber(z));

		return sb.toString();
	}

	/**
	 * Formats a numeric string: rounds to 12 significant digits and avoids
	 * scientific notation. Values extremely close to zero are rounded to 0.
	 */
	private static String formatNumber(String numStr) {
		if (numStr == null)
			return "0";
		try {
			double d = Double.parseDouble(numStr);
			if (Math.abs(d) < 1e-10)
				return "0";
			java.math.BigDecimal bd = new java.math.BigDecimal(d)
					.round(new java.math.MathContext(12));
			return bd.stripTrailingZeros().toPlainString();
		} catch (NumberFormatException e) {
			return numStr;
		}
	}
	
	/**
	 * Converts barTag XML attributes to Gpad format.
	 * Syntax: barTag: bar=<number> [#rrggbbaa] [fill=...] [angle=...] [dist=...] [image=...] [symbol=...] | bar=<number> ...
	 * Multiple bar styles separated by |
	 * 
	 * @param attrs barTag attributes map (should contain _barTags serialized string)
	 * @return Gpad barTag string, or null if should be omitted
	 */
	private static String convertBarTag(LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return null;
		
		String serialized = attrs.get("_barTags");
		if (serialized == null || serialized.isEmpty())
			return null;
		
		StringBuilder sb = new StringBuilder();
		boolean[] firstBar = {true}; // Use array to allow modification in lambda
		
		// Deserialize using helper class
		GpadSerializer.deserializeBarTags(serialized, (barNumber, rgba, fillTypeXML, hatchAngle,
				hatchDistance, image, fillSymbol) -> {
			if (!firstBar[0])
				sb.append(" | ");
			firstBar[0] = false;
			
			sb.append("bar=").append(barNumber);
			boolean firstAttr = true;
			
			// Output color (with alpha if present)
			if (rgba[0] >= 0 && rgba[1] >= 0 && rgba[2] >= 0) {
				if (!firstAttr)
					sb.append(" ");
				sb.append("#");
				sb.append(toHex2Digits(rgba[0]));
				sb.append(toHex2Digits(rgba[1]));
				sb.append(toHex2Digits(rgba[2]));
				if (rgba[3] >= 0) {
					// Include alpha in hex color
					sb.append(toHex2Digits(rgba[3]));
				}
				firstAttr = false;
			}
			
			// bit 2: barFillType
			if (fillTypeXML != null) {
				try {
					int fillType = Integer.parseInt(fillTypeXML);
					String fillTypeGpad = GpadStyleMaps.FILL_TYPE_REVERSE_MAP.get(fillTypeXML);
					if (fillTypeGpad != null && fillType != 0) { // 0 is STANDARD, omit it
						if (!firstAttr)
							sb.append(" ");
						sb.append("fill=").append(fillTypeGpad);
						firstAttr = false;
					}
				} catch (NumberFormatException e) {
					// Invalid fillType, ignore
				}
			}
			
			// bit 3: barHatchAngle
			if (hatchAngle != null) {
				try {
					int angle = Integer.parseInt(hatchAngle);
					if (angle != 45) { // Not default
						if (!firstAttr)
							sb.append(" ");
						sb.append("angle=").append(angle);
						firstAttr = false;
					}
				} catch (NumberFormatException e) {
					// Invalid angle, ignore
				}
			}
			
			// bit 4: barHatchDistance
			if (hatchDistance != null) {
				try {
					int dist = Integer.parseInt(hatchDistance);
					if (dist != 10) { // Not default
						if (!firstAttr)
							sb.append(" ");
						sb.append("dist=").append(dist);
						firstAttr = false;
					}
				} catch (NumberFormatException e) {
					// Invalid distance, ignore
				}
			}
			
			// bit 5: barImage
			if (image != null) {
				if (!firstAttr)
					sb.append(" ");
				sb.append("fill=image image=");
				// Escape if needed
				if (containsSpecialChars(image))
					sb.append(quoteString(image));
				else
					sb.append(image);
				firstAttr = false;
			}
			
			// bit 6: barSymbol
			if (fillSymbol != null) {
				if (!firstAttr)
					sb.append(" ");
				sb.append("fill=symbols symbol=");
				// Escape if needed
				if (containsSpecialChars(fillSymbol))
					sb.append("\"").append(fillSymbol).append("\"");
				else
					sb.append(fillSymbol);
				firstAttr = false;
			}
		});
		
		String result = sb.toString();
		return result.isEmpty() ? null : result;
	}

	/**
	 * Converts ggbscript or javascript XML element to Gpad script properties.
	 * Converts each attribute (val, onUpdate, onDragEnd, onChange) to corresponding gpad property.
	 * 
	 * @param tagName XML tag name ("ggbscript" or "javascript")
	 * @param attrs attribute map containing val, onUpdate, onDragEnd, onChange
	 * @return semicolon-separated string of gpad properties (e.g., "ggbClick: \"...\"; ggbUpdate: \"...\"")
	 *         or null if no valid attributes found
	 */
	private static String convertScriptElementToGpad(String tagName, LinkedHashMap<String, String> attrs) {
		if (attrs == null || attrs.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		// Determine prefix based on tag name
		String prefix = "ggbscript".equals(tagName) ? "ggb" : "js";

		// Convert each attribute to corresponding gpad property
		// val -> Click, onUpdate -> Update, onDragEnd -> DragEnd, onChange -> Change
		String[] attrNames = {"val", "onUpdate", "onDragEnd", "onChange"};
		String[] propSuffixes = {"Click", "Update", "DragEnd", "Change"};

		for (int i = 0; i < attrNames.length; i++) {
			String attrName = attrNames[i];
			String scriptContent = attrs.get(attrName);
			
			if (scriptContent == null)
				continue;

			if (!first)
				sb.append("; ");
			
			String propName = prefix + propSuffixes[i];
			sb.append(propName).append(": ");

			sb.append(quoteStringForStyleSheet(scriptContent));
			
			first = false;
		}

		return sb.toString();
	}
}
