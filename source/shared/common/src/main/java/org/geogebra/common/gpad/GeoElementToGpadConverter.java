package org.geogebra.common.gpad;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.StringTemplate;

/**
 * Converts GeoElement objects to Gpad format.
 * Uses XML to extract style properties, then converts to Gpad format.
 */
public class GeoElementToGpadConverter {
	private int styleSheetCounter = 0;
	private Map<GeoElement, String> styleSheetMap = new java.util.HashMap<>();
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
	 * @param geo
	 *            GeoElement to convert
	 * @return Gpad string representation
	 */
	public String toGpad(GeoElement geo) {
		if (geo == null)
			return "";

		StringBuilder sb = new StringBuilder();

		// Get XML style string from GeoElement
		String styleXML = geo.getStyleXML();
		
		// Parse XML to get style map
		Map<String, LinkedHashMap<String, String>> styleMap = null;
		if (styleXML != null && !styleXML.trim().isEmpty()) {
			try {
				styleMap = xmlParser.parse(styleXML);
			} catch (GpadParseException e) {
				// If parsing fails, continue without styles
				org.geogebra.common.util.debug.Log.debug("Failed to parse style XML: " + e.getMessage());
			}
		}

		// Convert style map to Gpad style sheet if needed
		String styleSheetName = null;
		if (styleMap != null && !styleMap.isEmpty()) {
			styleSheetName = generateStyleSheetName(geo);
			String styleSheetGpad = styleConverter.convert(styleSheetName, styleMap);
			if (styleSheetGpad != null && !styleSheetGpad.isEmpty()) {
				sb.append(styleSheetGpad);
				sb.append("\n");
			}
		}

		// Extract command definition
		String command = extractCommand(geo);
		if (command == null || command.isEmpty()) {
			// If no command, it might be an independent element
			// Try to get its definition
			if (geo.isIndependent()) {
				command = geo.getDefinition(StringTemplate.defaultTemplate);
				if (command == null || command.isEmpty()) {
					return ""; // Cannot convert
				}
			} else {
				return ""; // Cannot convert dependent elements without command
			}
		}

		// Build output part
		String label = geo.getLabelSimple();
		if (label == null || label.isEmpty()) {
			label = geo.getLabel(StringTemplate.defaultTemplate);
		}

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
			sb.append(" $").append(styleSheetName);

		// Add command
		sb.append(" = ").append(command);

		return sb.toString();
	}

	/**
	 * Converts multiple GeoElements to Gpad format.
	 * 
	 * @param geos
	 *            list of GeoElements to convert
	 * @return Gpad string representation
	 */
	public String toGpad(List<GeoElement> geos) {
		StringBuilder sb = new StringBuilder();
		
		for (GeoElement geo : geos) {
			String gpad = toGpad(geo);
			if (!gpad.isEmpty()) {
				sb.append(gpad).append("\n");
			}
		}

		return sb.toString();
	}


	/**
	 * Extracts command definition from GeoElement.
	 * 
	 * @param geo
	 *            GeoElement
	 * @return command string, or null if not available
	 */
	private String extractCommand(GeoElement geo) {
		if (geo.getParentAlgorithm() != null) {
			// Get command from parent algorithm
			String cmdName = geo.getParentAlgorithm().getDefinitionName(
					StringTemplate.defaultTemplate);
			
			if (cmdName != null && !"Expression".equals(cmdName)) {
				// Get command string with parameters
				return geo.getParentAlgorithm().getDefinition(
						StringTemplate.defaultTemplate);
			}
		}

		// For independent elements, try to get definition
		if (geo.isIndependent() && geo.getDefinition() != null) {
			return geo.getDefinition(StringTemplate.defaultTemplate);
		}

		return null;
	}


	/**
	 * Generates a unique style sheet name for a GeoElement.
	 * 
	 * @param geo
	 *            GeoElement
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
