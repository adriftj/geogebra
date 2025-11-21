package org.geogebra.common.gpad;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.util.debug.Log;

/**
 * Converts GeoElement objects to Gpad format.
 * Uses XML to extract style properties, then converts to Gpad format.
 */
public class GeoElementToGpadConverter {
	static final StringTemplate myTPL = StringTemplate.noLocalDefault;

	private int styleSheetCounter = 0;
	private Map<GeoElement, String> styleSheetMap = new HashMap<>();
	private XMLToStyleMapParser xmlParser;
	private StyleMapToGpadConverter styleConverter;

	/**
	 * Creates a new GeoElementToGpadConverter.
	 */
	public GeoElementToGpadConverter() {
		this.xmlParser = new XMLToStyleMapParser();
		this.styleConverter = new StyleMapToGpadConverter();
	}

	/**
	 * Converts a GeoElement to Gpad format.
	 * 
	 * @param geo GeoElement to convert
	 * @return Gpad string representation
	 */
	public String toGpad(GeoElement geo) {
		if (geo == null)
			return "";

		StringBuilder sb = new StringBuilder();
		String styleXML = geo.getStyleXML();
		// Parse XML to get style map
		Map<String, LinkedHashMap<String, String>> styleMap = null;
		if (styleXML != null && !styleXML.trim().isEmpty()) {
			try {
				styleMap = xmlParser.parse(styleXML);
			} catch (GpadParseException e) {
				// If parsing fails, continue without styles
				Log.debug("Failed to parse style XML: " + e.getMessage());
			}
		}

		String styleSheetName = null;
		if (styleMap != null && !styleMap.isEmpty()) {
			styleSheetName = generateStyleSheetName(geo);
			String objectType = geo.getTypeString();
			
			String styleSheetGpad = styleConverter.convert(styleSheetName, styleMap, objectType);
			if (styleSheetGpad != null && !styleSheetGpad.isEmpty()) {
				sb.append(styleSheetGpad);
				sb.append("\n");
			}
		}

		// Extract command definition
		String command = extractCommand(geo);
		if (command == null || command.isEmpty())
			return "";

		// Build output part
		String label = geo.getLabelSimple();

		// Determine visibility flags
		boolean hideObject = !geo.isSetEuclidianVisible();
		boolean hideLabel = !geo.isLabelVisible();

		// Build output specification
		sb.append(label);
		if (hideObject)
			sb.append("*");
		else if (hideLabel)
			sb.append("~");

		// Add style sheet reference if available
		if (styleSheetName != null)
			sb.append(" @").append(styleSheetName);

		// Add command
		sb.append(" = ").append(command).append(";");
		return sb.toString();
	}

	/**
	 * Converts multiple GeoElements to Gpad format.
	 * 
	 * @param geos list of GeoElements to convert
	 * @return Gpad string representation
	 */
	public String toGpad(List<GeoElement> geos) {
		StringBuilder sb = new StringBuilder();
		for (GeoElement geo : geos) {
			String gpad = toGpad(geo);
			if (!gpad.isEmpty())
				sb.append(gpad).append("\n");
		}
		return sb.toString();
	}


	/**
	 * Extracts command definition from GeoElement.
	 * 
	 * @param geo GeoElement
	 * @return command string, or null if not available
	 */
	private String extractCommand(GeoElement geo) {
		if (geo.getParentAlgorithm() != null) {
			// Get command from parent algorithm
			String cmdName = geo.getParentAlgorithm().getDefinitionName(myTPL);
			if (cmdName != null && !"Expression".equals(cmdName)) {
				// Get command string with parameters
				return geo.getParentAlgorithm().getDefinition(myTPL);
			}
		}

		// Special handling for Button objects
		if (geo instanceof GeoButton) {
			GeoButton button = (GeoButton) geo;
			String caption = button.getCaption(myTPL);
			if (caption != null && !caption.isEmpty()) // with caption
				return "Button(\"" + escapeString(caption) + "\")";
			else // without caption
				return "Button()";
		}

		// Special handling for Slider objects (GeoNumeric with slider properties)
		if (geo instanceof GeoNumeric) {
			GeoNumeric num = (GeoNumeric) geo;
			if (num.isSliderable()) {
				// Build Slider command: Slider(min, max)
				double min = num.getIntervalMin();
				double max = num.getIntervalMax();
				return "Slider(" + min + ", " + max + ")";
			}
		}

		// For independent elements, try to get definition
		if (geo.isIndependent() && geo.getDefinition() != null)
			return geo.getDefinition(myTPL);
		return null;
	}
	
	/**
	 * Escapes special characters in a string for Gpad format.
	 * 
	 * @param str string to escape
	 * @return escaped string
	 */
	private String escapeString(String str) {
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
