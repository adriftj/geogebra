package org.geogebra.common.gpad;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.arithmetic.FunctionalNVar;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.geos.GeoImage;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.kernel.kernelND.GeoPointND;
import org.geogebra.common.kernel.kernelND.GeoVectorND;
import org.geogebra.common.util.StringUtil;
import org.geogebra.common.util.debug.Log;

/**
 * Converts GeoElement objects to Gpad format.
 * Uses XML to extract style properties, then converts to Gpad format.
 */
public class GeoElementToGpadConverter {
	static final StringTemplate myTPL = StringTemplate.noLocalDefault;
	
	/**
	 * Set of style properties that should be removed for objects that are not
	 * shown in the EuclidianView (geometry view).
	 * These styles are only relevant for visual display in the geometry view.
	 */
	private static final Set<String> EUCLIDIAN_DISPLAY_STYLES = Set.of(
		"angleStyle",
		"animation",
		"bgColor",
		"labelMode",
		"layer",
		"lineStyle",
		"objColor"
	);

	private int styleSheetCounter = 0;
	private Map<GeoElement, String> styleSheetMap = new HashMap<>();

	/**
	 * Creates a new GeoElementToGpadConverter.
	 */
	public GeoElementToGpadConverter() {
	}

	/**
	 * Extracts the <element>...</element> part from full XML.
	 * This is needed because QDParser may have issues with <expression> tags.
	 * 
	 * @param fullXML full XML string
	 * @return element XML string, or null if not found
	 */
	private static String extractElementXML(String fullXML) {
		if (fullXML == null || fullXML.trim().isEmpty())
			return null;
		
		int startIdx = fullXML.indexOf("<element");
		if (startIdx < 0)
			return null;
		
		// Find the matching closing tag
		int depth = 0;
		int idx = startIdx;
		while (idx < fullXML.length()) {
			if (fullXML.startsWith("<element", idx)) {
				depth++;
				idx = fullXML.indexOf(">", idx) + 1;
			} else if (fullXML.startsWith("</element>", idx)) {
				depth--;
				if (depth == 0)
					return fullXML.substring(startIdx, idx + "</element>".length());
				idx += "</element>".length();
			} else
				idx++;
		}
		
		return null; // No matching closing tag found
	}

	/**
	 * Filters style map before conversion to Gpad format.
	 * - Removes object and label attributes from show style
	 * - Removes EuclidianView display styles for objects that are not shown in geometry view
	 * - Removes file style for independent GeoImage objects (filename is already in Image command)
	 * 
	 * @param styleMap style map to filter (modified in place)
	 * @param geo GeoElement to check visibility
	 */
	private static void filterStyleMap(Map<String, LinkedHashMap<String, String>> styleMap, GeoElement geo) {
		if (styleMap == null)
			return;
		
		// Remove object and label attributes from show style
		LinkedHashMap<String, String> showAttrs = styleMap.get("show");
		if (showAttrs != null) {
			showAttrs.remove("object");
			showAttrs.remove("label");
			// If show style becomes empty after removal, remove it from styleMap
			if (showAttrs.isEmpty())
				styleMap.remove("show");
		}
		
		// Remove file style for independent GeoImage objects
		// The filename is already included in the Image command, so it shouldn't be in the style sheet
		if (geo instanceof GeoImage && geo.isIndependent())
			styleMap.remove("file");
		
		// Remove EuclidianView display styles for objects not shown in geometry view
		if (!geo.isEuclidianShowable()) {
			for (String styleKey : EUCLIDIAN_DISPLAY_STYLES)
				styleMap.remove(styleKey);
		}
	}

	/**
	 * Extracts style sheet content string (without name) from a GeoElement's XML.
	 * Returns only the content part "{ ... }" without the "@name = " prefix.
	 * 
	 * @param geo GeoElement to extract styles from
	 * @return null if empty (which means [null, false]), or Object array with [0]=String (style sheet content string in format "{ ... }"), [1]=Boolean (true if contains expressions)
	 */
	public static Object[] extractStyleSheetContent(GeoElement geo) {
		if (geo == null)
			return null;
		
		// Use getXML() instead of getStyleXML() to include coords and other tags
		// getStyleXML() only includes style properties, not data like coords
		String fullXML = geo.getXML();
		// Extract only the <element>...</element> part for parsing
		// QDParser may have issues with <expression> tags
		String elementXML = extractElementXML(fullXML);
		if (elementXML == null || elementXML.trim().isEmpty())
			return null;
		
		// Parse XML to get style map
		XMLToStyleMapParser xmlParser = new XMLToStyleMapParser();
		try {
			Map<String, LinkedHashMap<String, String>> styleMap = xmlParser.parse(elementXML);
			if (styleMap == null || styleMap.isEmpty())
				return null;
			
			// Filter style map before conversion
			filterStyleMap(styleMap, geo);
			
			return StyleMapToGpadConverter.convertToContentOnly(styleMap, geo.getTypeString());
		} catch (GpadParseException e) {
			return null;
		}
	}

	public static boolean buildOutputLabel(StringBuilder sb, GeoElement geo) {
		String label = geo.getLabel(myTPL);
		if (label == null || label.isEmpty()) {
			Log.error("Geo has empty label, skipping");
			return false;
		}

		// Check if this is a function (GeoFunction or GeoFunctionNVar)
		// For functions: f(u,v,...) instead of just f
		if (geo instanceof FunctionalNVar) {
			FunctionalNVar func = (FunctionalNVar) geo;
			org.geogebra.common.kernel.arithmetic.FunctionVariable[] vars = null;
			
			// First, try to get from fun field directly (most reliable)
			if (geo instanceof GeoFunction) {
				GeoFunction geoFunc = (GeoFunction) geo;
				org.geogebra.common.kernel.arithmetic.Function fun = geoFunc.getFunction();
				if (fun != null)
					vars = fun.getFunctionVariables();
			} else if (geo instanceof org.geogebra.common.kernel.geos.GeoFunctionNVar) {
				org.geogebra.common.kernel.geos.GeoFunctionNVar geoFuncNVar = 
					(org.geogebra.common.kernel.geos.GeoFunctionNVar) geo;
				org.geogebra.common.kernel.arithmetic.FunctionNVar fun = geoFuncNVar.getFunction();
				if (fun != null)
					vars = fun.getFunctionVariables();
			}
			
			// If still no variables, try getFunctionVariables() from interface
			if (vars == null || vars.length == 0)
				vars = func.getFunctionVariables();
			
			// If still no variables, try to get from definition's ExpressionNode left field
			// FunctionParser.assignment() creates ExpressionNode with FunctionNVar in left field
			if ((vars == null || vars.length == 0) && geo.getDefinition() != null) {
				org.geogebra.common.kernel.arithmetic.ExpressionValue def = geo.getDefinition().unwrap();
				if (def instanceof org.geogebra.common.kernel.arithmetic.ExpressionNode) {
					org.geogebra.common.kernel.arithmetic.ExpressionNode defNode = 
						(org.geogebra.common.kernel.arithmetic.ExpressionNode) def;
					org.geogebra.common.kernel.arithmetic.ExpressionValue left = defNode.getLeft();
					if (left instanceof org.geogebra.common.kernel.arithmetic.FunctionNVar)
						vars = ((org.geogebra.common.kernel.arithmetic.FunctionNVar) left).getFunctionVariables();
					else if (left instanceof org.geogebra.common.kernel.arithmetic.Function)
						vars = ((org.geogebra.common.kernel.arithmetic.Function) left).getFunctionVariables();
				} else if (def instanceof org.geogebra.common.kernel.arithmetic.FunctionNVar)
					vars = ((org.geogebra.common.kernel.arithmetic.FunctionNVar) def).getFunctionVariables();
				else if (def instanceof org.geogebra.common.kernel.arithmetic.Function)
					vars = ((org.geogebra.common.kernel.arithmetic.Function) def).getFunctionVariables();
			}
			
			// Last resort: try to get from definition's local variables
			// FunctionParser.assignment() stores local variables in ExpressionNode via setLocalVariables()
			if ((vars == null || vars.length == 0) && geo.getDefinition() != null) {
				java.util.List<String> localVars = geo.getDefinition().getLocalVariables();
				if (localVars != null && !localVars.isEmpty()) {
					// Create FunctionVariable array from local variable names
					vars = new org.geogebra.common.kernel.arithmetic.FunctionVariable[localVars.size()];
					for (int i = 0; i < localVars.size(); i++) {
						vars[i] = new org.geogebra.common.kernel.arithmetic.FunctionVariable(
							geo.getKernel(), localVars.get(i));
					}
				}
			}
			
			// Build label with variable list: f(u,v,...)
			sb.append(label);
			
			// Add variable list in parentheses
			if (vars != null && vars.length > 0) {
				sb.append("(");
				for (int i = 0; i < vars.length; i++) {
					if (i > 0)
						sb.append(", ");
					sb.append(vars[i].toString(myTPL));
				}
				sb.append(")");
			}
		} else // For non-function elements, use standard label
			sb.append(label);
		
		// Only add visibility suffixes for objects that can be shown in EuclidianView
		// Objects that don't show in geometry view (e.g., GeoNumeric without slider,
		// GeoScriptAction, GeoCasCell, etc.) should not have "*" or "~" suffixes
		if (geo.isEuclidianShowable()) {
			// Determine visibility flags
			boolean hideObject = !geo.isSetEuclidianVisible();
			boolean hideLabel = !geo.isLabelVisible();

			if (hideObject)
				sb.append("*");
			else if (hideLabel)
				sb.append("~");
		}
		
		return true;
	}

	public static boolean buildCommand(StringBuilder sb, GeoElement geo, String styleSheetName) {
		// Extract command definition
		String command = extractCommand(geo);
		if (command == null || command.isEmpty())
			return false;

		if (!buildOutputLabel(sb, geo))
			return false;

		// Add style sheet reference if available
		if (styleSheetName != null)
			sb.append(" @").append(styleSheetName);

		// Add command
		sb.append(" = ").append(command).append(";");
		return true;
	}

	/**
	 * Converts a GeoElement to Gpad format.
	 * 
	 * @param geo GeoElement to convert
	 * @return Gpad string representation
	 */
	public String toGpad(GeoElement geo) {
		if (geo != null) {
			StringBuilder sb = new StringBuilder();
			Object[] result = extractStyleSheetContent(geo);
			String styleSheetGpad = (result != null) ? (String) result[0] : null;
			String styleSheetName = generateStyleSheetName(geo);
			if (styleSheetGpad != null && !styleSheetGpad.isEmpty()) {
				sb.append("@").append(styleSheetName).append(" = ");
				sb.append(styleSheetGpad);
				sb.append("\n");
			}
			if (buildCommand(sb, geo, styleSheetName))
				return sb.toString();
		}
		return "";
	}

	/**
	 * Converts multiple GeoElements to Gpad format.
	 * 
	 * @param geos list of GeoElements to convert
	 * @return Gpad string representation
	 */
	public static String toGpad(List<GeoElement> geos) {
		GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
		StringBuilder sb = new StringBuilder();
		for (GeoElement geo : geos) {
			String gpad = converter.toGpad(geo);
			if (!gpad.isEmpty())
				sb.append(gpad).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Extracts command definition from GeoElement.
	 * Uses the same logic as XML generation to determine how to represent the element.
	 * 
	 * XML generation logic:
	 * 1. If independent && definition != null && getDefaultGeoType() < 0:
	 *    - Generate <expression> tag with definition (like colorGreyDark = "#4D4D4D")
	 * 2. Otherwise, for independent elements:
	 *    - Generate <element> tag with <coords> or <value> tags (like PDilate with coords)
	 * 
	 * This method follows the same logic:
	 * - If condition 1 is met, return definition (corresponds to <expression>)
	 * - Otherwise, use toValueString() or type-specific handling (corresponds to <coords>/<value>)
	 * 
	 * @param geo GeoElement
	 * @return command string, or null if not available
	 */
	private static String extractCommand(GeoElement geo) {
		// If it has a parent algorithm, get command from parent (unless it's Expression)
		if (geo.getParentAlgorithm() != null) {
			String cmdName = geo.getParentAlgorithm().getDefinitionName(myTPL);
			if (cmdName != null && !"Expression".equals(cmdName)) {
				// Get command string with parameters
				return geo.getParentAlgorithm().getDefinition(myTPL);
			}
		}

		if (geo instanceof GeoPointND) {
			GeoPointND point = (GeoPointND) geo;
			if (point.isDefined() && point.isFinite()) {
				// Check if label starts with lowercase (would be parsed as vector)
				String label = geo.getLabelSimple();
				boolean labelIsLowercase = label != null && !label.isEmpty() 
					&& StringUtil.isLowerCase(label.charAt(0));
				
				// If label is lowercase, use explicit Point command to avoid ambiguity
				if (labelIsLowercase) {
					double x = point.getInhomX();
					double y = point.getInhomY();
					double z = point.getInhomZ();
					// Use Point[{x, y}] or Point[{x, y, z}] format (list syntax)
					if (point.getDimension() == 3 && z != 1.0)
						return "Point[{" + x + ", " + y + ", " + z + "}]";
					else
						return "Point[{" + x + ", " + y + "}]";
				}
			}
		}

		if (geo instanceof GeoVectorND) {
			GeoVectorND vector = (GeoVectorND) geo;
			if (vector.isDefined() && vector.isFinite()) {
				// Check if label starts with uppercase (would be parsed as point)
				String label = geo.getLabelSimple();
				boolean labelIsUppercase = label != null && !label.isEmpty() 
					&& !StringUtil.isLowerCase(label.charAt(0));
				
				// If label is uppercase, use explicit Vector command to avoid ambiguity
				if (labelIsUppercase) {
					double x = vector.getX();
					double y = vector.getY();
					double z = vector.getZ();
					// Use Vector[{x, y}] or Vector[{x, y, z}] format (list syntax, same as Point)
					if (vector.getDimension() == 3 && z != 0.0)
						return "Vector[{" + x + ", " + y + ", " + z + "}]";
					else
						return "Vector[{" + x + ", " + y + "}]";
				}
			}
		}

		if (geo instanceof GeoNumeric) {
			GeoNumeric num = (GeoNumeric) geo;
			if (num.isSliderable()) {
				// Build Slider command: Slider(min, max)
				double min = num.getIntervalMin();
				double max = num.getIntervalMax();
				return "Slider(" + min + ", " + max + ")";
			}
			// For independent numeric without slider and without definition, use its value
			// This corresponds to XML <element type="numeric"><value val="..."/>
			if (num.isIndependent() && num.isDefined() && num.getDefinition() == null) {
				// Use toValueString to get properly formatted number
				String valStr = num.toValueString(myTPL);
				if (valStr != null && !valStr.isEmpty() && !"?".equals(valStr))
					return valStr;
				// Fallback to simple double representation
				double value = num.getDouble();
				if (Double.isFinite(value))
					return String.valueOf(value);
			}
		}

		// Special handling for GeoBoolean: extract boolean value
		// This corresponds to XML <element type="boolean"><value val="true"/>
		if (geo instanceof GeoBoolean) {
			GeoBoolean bool = (GeoBoolean) geo;
			if (bool.isDefined()) // Return boolean value as string: "true" or "false"
				return bool.getBoolean() ? "true" : "false";
			// If undefined, return "?" (though this shouldn't happen for independent booleans)
			return "?";
		}

		// Special handling for GeoText: use Text("...") format for independent text
		// that doesn't satisfy expression condition (getDefaultGeoType() >= 0)
		// This corresponds to XML <element type="text"> with text content (not <expression>)
		if (geo instanceof GeoText) {
			GeoText text = (GeoText) geo;
			if (text.isDefined()) {
				String textStr = text.getTextStringSafe();
				if (textStr != null) {
					// Build Text command: Text("...")
					// Escape the string for gpad format
					return "Text(\"" + escapeString(textStr) + "\")";
				}
			}
			// If undefined or empty, return Text("")
			return "Text(\"\")";
		}

		// Special handling for GeoFunction: extract expression from function
		if (geo instanceof GeoFunction) {
			GeoFunction func = (GeoFunction) geo;
			if (func.isDefined()) {
				// Try to get expression from function
				org.geogebra.common.kernel.arithmetic.ExpressionNode exprNode = func.getFunctionExpression();
				if (exprNode != null) {
					String expr = exprNode.toString(myTPL);
					if (expr != null && !expr.isEmpty())
						return expr;
				}
			}
		}

		if (geo instanceof GeoButton) {
			GeoButton button = (GeoButton) geo;
			String caption = button.getCaption(myTPL);
			if (caption != null && !caption.isEmpty()) // with caption
				return "Button(\"" + escapeString(caption) + "\")";
			else // without caption
				return "Button()";
		}

		if (geo instanceof GeoImage) {
			GeoImage image = (GeoImage) geo;
			// Only handle independent images (not created by commands)
			if (image.isIndependent()) {
				String fileName = image.getGraphicsAdapter().getImageFileName();
				if (fileName != null && !fileName.isEmpty()) // with filename/URL
					return "Image(\"" + escapeString(fileName) + "\")";
				else // without filename
					return "Image()";
			}
		}

		if (geo.getDefinition() != null && geo.getDefaultGeoType() < 0) {
			String str = geo.getDefinition(myTPL);
			if (str != null && !str.isEmpty())
				return str;
		}
		return geo.toValueString(myTPL); // fallback
	}
	
	/**
	 * Escapes special characters in a string for Gpad format.
	 * 
	 * @param str string to escape
	 * @return escaped string
	 */
	private static String escapeString(String str) {
		if (str == null)
			return "";
		// Escape backslash and double quote
		return str.replace("\\", "\\\\").replace("\"", "\\\"");
	}


	/**
	 * Generates a unique style sheet name for a GeoElement.
	 * 
	 * @param geo GeoElement
	 * @return style sheet name
	 */
	private String generateStyleSheetName(GeoElement geo) {
		// Check if we already have a style sheet for this geo
		if (styleSheetMap.containsKey(geo))
			return styleSheetMap.get(geo);

		// Generate new name based on label or counter
		String name;
		String label = geo.getLabelSimple();
		if (label != null && !label.isEmpty())
			name = label + "Style";
		else
			name = "style" + (++styleSheetCounter);

		styleSheetMap.put(geo, name);
		return name;
	}
}
