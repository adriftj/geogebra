package org.geogebra.common.gpad;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

import org.geogebra.common.io.DocHandler;
import org.geogebra.common.io.QDParser;
import org.geogebra.common.io.XMLParseException;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.Macro;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.util.StringUtil;
import org.geogebra.common.util.debug.Log;

/**
 * Converts GeoGebra construction XML to Gpad format by parsing XML directly.
 * This is an alternative implementation to GgbToGpadConverter that works from XML
 * instead of iterating through ConstructionElement objects.
 */
public class XMLToGpadConverter implements DocHandler {
	private final Construction construction;
	private final boolean mergeStylesheets;
	private final Map<String, String> styleSheetContentMap = new HashMap<>();
	private final Set<String> generatedStyleSheets = new java.util.HashSet<>();
	private final Map<String, String> deferredStyleSheets = new HashMap<>();
	private int styleSheetCounter = 0;

	// Current parsing state
	private StringBuilder output = new StringBuilder();
	private String currentCommandName = null;
	private List<String> currentInputArgs = new ArrayList<>();
	private List<String> currentOutputLabels = new ArrayList<>();
	private String currentExpressionLabel = null;
	private String currentExpressionExp = null;
	private String currentElementLabel = null;
	private String currentElementType = null;
	private Map<String, LinkedHashMap<String, String>> currentElementStyleMap = null;
	// Serializers for startPoint and tag elements
	private GpadSerializer.GpadSerializeStartPoint currentStartPointSerializer;
	private GpadSerializer.GpadSerializeBarTag currentBarTagSerializer;
	private boolean inCommand = false;
	private boolean inElement = false;
	private int elementDepth = 0;
	// Map from label to stylesheet name (for stylesheets without expressions)
	private Map<String, String> labelToStyleSheetName = new HashMap<>();
	private Set<String> pendingOutputLabels = new java.util.HashSet<>();

	/**
	 * Creates a new XMLToGpadConverter.
	 * 
	 * @param construction the construction to convert
	 * @param mergeStylesheets whether to merge identical stylesheets
	 */
	public XMLToGpadConverter(Construction construction, boolean mergeStylesheets) {
		this.construction = construction;
		this.mergeStylesheets = mergeStylesheets;
	}

	/**
	 * Converts the entire construction (including macros) to Gpad format.
	 * Macros are converted first, then the main construction.
	 * 
	 * @return Gpad string representation of the construction
	 */
	public String toGpad() {
		StringBuilder sb = new StringBuilder();
		
		// Convert all macros first
		convertMacros(sb);
		
		// Then convert the main construction
		convertConstruction(sb);
		
		return sb.toString();
	}

	/**
	 * Converts all macros to gpad format.
	 * 
	 * @param sb string builder to append to
	 */
	private void convertMacros(StringBuilder sb) {
		Kernel kernel = construction.getKernel();
		if (kernel == null)
			return;
		
		List<Macro> macros = kernel.getAllMacros();
		if (macros == null || macros.isEmpty())
			return;
		
		// Convert each macro
		for (Macro macro : macros) {
			if (macro != null)
				convertMacro(macro, sb);
		}
	}

	private void buildLabels(StringBuilder sb, GeoElement[] geos) {
		boolean first = true;
		if (geos != null) {
			for (GeoElement geo : geos) {
				if (geo != null) {
					String label = geo.getLabelSimple();
					if (label != null && !label.isEmpty()) {
						if (!first)
							sb.append(", ");
						first = false;
						sb.append(label);
					}
				}
			}
		}
	}

	/**
	 * Converts a single macro to gpad format.
	 * 
	 * @param macro the macro to convert
	 * @param sb string builder to append to
	 */
	private void convertMacro(Macro macro, StringBuilder sb) {
		// Get macro name
		String macroName = macro.getCommandName();
		if (macroName == null || macroName.isEmpty()) {
			Log.error("Macro has no command name, skipping");
			return;
		}
		
		// Get macro construction
		Construction macroCons = macro.getMacroConstruction();
		if (macroCons == null) {
			Log.error("Macro " + macroName + " has no construction, skipping");
			return;
		}
		
		// Start macro definition: @@macro macroName(input1, input2, ...) {
		sb.append("@@macro ").append(macroName).append("(");
		buildLabels(sb, macro.getMacroInput());
		sb.append(") {\n");
		
		// Convert macro construction XML using a new converter instance
		XMLToGpadConverter macroConverter = new XMLToGpadConverter(macroCons, mergeStylesheets);
		StringBuilder sbGpad = new StringBuilder();
		macroConverter.convertConstruction(sbGpad);
		String macroConstructionGpad = sbGpad.toString();
		
		// Append macro construction content (indented)
		if (macroConstructionGpad != null && !macroConstructionGpad.isEmpty()) {
			// Indent each line of the macro construction
			String[] lines = macroConstructionGpad.split("\n");
			for (String line : lines) {
				if (!line.trim().isEmpty())
					sb.append("    ").append(line).append("\n");
			}
		}
		
		// End macro definition: @@return output1, output2, ... }
		// Note: @@return statement does NOT end with semicolon
		sb.append("    @@return ");
		buildLabels(sb, macro.getMacroOutput());
		sb.append("\n}\n\n");
	}

	/**
	 * Converts construction XML to gpad format.
	 * 
	 * @param constructionXML the construction XML string
	 * @return Gpad string representation
	 */
	private String convertConstructionXML(String constructionXML) {
		output = new StringBuilder();
		resetState();

		try {
			QDParser parser = new QDParser();
			parser.parse(this, new StringReader(constructionXML));
			// Output any pending command/expression at the end
			processPendingElements();
			// Generate deferred @@set statements for stylesheets with expressions
			generateDeferredSetStatements(output);
			return output.toString();
		} catch (XMLParseException e) {
			Log.error("Failed to parse construction XML: " + e.getMessage());
			return "";
		} catch (Exception e) {
			Log.error("Error parsing construction XML: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Converts the main construction to gpad format.
	 * 
	 * @param sb string builder to append to
	 */
	private void convertConstruction(StringBuilder sb) {
		// Get construction XML
		StringBuilder consXML = new StringBuilder();
		construction.getConstructionXML(consXML, false);

		// Convert construction XML
		String gpad = convertConstructionXML(consXML.toString());
		sb.append(gpad);
	}

	private void resetState() {
		currentCommandName = null;
		currentInputArgs.clear();
		currentOutputLabels.clear();
		currentExpressionLabel = null;
		currentExpressionExp = null;
		currentElementLabel = null;
		currentElementType = null;
		currentElementStyleMap = null;
		currentStartPointSerializer = null;
		currentBarTagSerializer = null;
		labelToStyleSheetName.clear();
		inCommand = false;
		inElement = false;
		elementDepth = 0;
		pendingOutputLabels.clear();
	}

	@Override
	public void startDocument() throws XMLParseException {
		resetState();
	}

	@Override
	public void endDocument() throws XMLParseException {
	}

	@Override
	public void startElement(String tag, LinkedHashMap<String, String> attrs) throws XMLParseException {
		// Output pending command/expression before starting a new one
		if ("command".equals(tag)) {
			processPendingElements();
			startCommand(attrs);
		} else if ("expression".equals(tag)) {
			processPendingElements();
			startExpression(attrs);
		} else if ("element".equals(tag))
			startElement(attrs); // Don't output pending expression/command
		else if (inCommand)
			handleCommandChild(tag, attrs);
		else if (inElement)
			handleElementChild(tag, attrs);
	}

	@Override
	public void endElement(String tag) throws XMLParseException {
		if ("command".equals(tag)) {
			endCommand();
		} else if ("expression".equals(tag)) {
			endExpression();
		} else if ("element".equals(tag)) {
			endElement();
		} else if (inElement && elementDepth > 0) {
			elementDepth--;
		}
	}

	@Override
	public void text(String str) throws XMLParseException {
		// Ignore text content
	}

	private void startCommand(LinkedHashMap<String, String> attrs) {
		inCommand = true;
		currentCommandName = attrs.get("name");
		currentInputArgs.clear();
		currentOutputLabels.clear();
	}

	private void handleCommandChild(String tag, LinkedHashMap<String, String> attrs) {
		if ("input".equals(tag))
			extractIndexedAttributes(attrs, currentInputArgs);
		else if ("output".equals(tag))
			extractIndexedAttributes(attrs, currentOutputLabels);
	}
	
	/**
	 * Extracts indexed attributes (a0, a1, ...) from attrs and stores them in the list.
	 * 
	 * @param attrs attribute map containing a0, a1, ... attributes
	 * @param list list to store the extracted values (will be grown as needed)
	 */
	private static void extractIndexedAttributes(LinkedHashMap<String, String> attrs, List<String> list) {
		for (String key : attrs.keySet()) {
			if (key.startsWith("a") && key.length() > 1) {
				try {
					Integer.parseInt(key.substring(1));
					String value = attrs.get(key);
					if (value != null) {
						// Ensure list is large enough
						int index = Integer.parseInt(key.substring(1));
						while (list.size() <= index)
							list.add(null);
						list.set(index, value);
					}
				} catch (NumberFormatException e) {
					// Not a numeric attribute, ignore
				}
			}
		}
	}

	private void endCommand() {
		inCommand = false;
		// Save output labels for matching with following style elements
		pendingOutputLabels = currentOutputLabels.stream()
								.filter(Objects::nonNull).collect(Collectors.toSet());
		// Command will be output when next command/expression/element starts or at end of document
	}

	private void startExpression(LinkedHashMap<String, String> attrs) {
		currentExpressionLabel = attrs.get("label");
		currentExpressionExp = attrs.get("exp");
	}

	private void endExpression() {
		pendingOutputLabels.clear();
		if (currentExpressionLabel != null)
			pendingOutputLabels.add(currentExpressionLabel);
	}

	private void startElement(LinkedHashMap<String, String> attrs) {
		inElement = true;
		elementDepth = 0;
		currentElementLabel = attrs.get("label");
		currentElementType = attrs.get("type");
		currentElementStyleMap = new LinkedHashMap<>();
		// Initialize serializers for startPoint and tag elements
		currentStartPointSerializer = GpadSerializer.beginSerializeStartPoint();
		currentBarTagSerializer = GpadSerializer.beginSerializeBarTag();
	}

	private void handleElementChild(String tag, LinkedHashMap<String, String> attrs) {
		elementDepth++;
		
		LinkedHashMap<String, String> elementAttrs = new LinkedHashMap<>();
		if (attrs != null)
			elementAttrs.putAll(attrs);
		
		// Special handling for startPoint: collect for later serialization
		if ("startPoint".equals(tag)) {
			if (currentStartPointSerializer != null)
				currentStartPointSerializer.add(elementAttrs);
		} else if ("tag".equals(tag)) {
			// Special handling for tag: collect by barNumber for later serialization
			String barNumberStr = elementAttrs.get("barNumber");
			if (currentBarTagSerializer != null && barNumberStr != null)
				currentBarTagSerializer.add(barNumberStr, elementAttrs);
		} else {
			// Store child element of <element> in the style map
			currentElementStyleMap.put(tag, elementAttrs);
		}
	}

	private void endElement() {
		// Serialize startPoint elements if any
		if (currentStartPointSerializer != null) {
			String serialized = currentStartPointSerializer.end();
			if (serialized != null) {
				LinkedHashMap<String, String> startPointAttrs = new LinkedHashMap<>();
				startPointAttrs.put("_corners", serialized);
				currentElementStyleMap.put("startPoint", startPointAttrs);
			}
		}
		
		// Serialize tag elements if any
		if (currentBarTagSerializer != null) {
			String serialized = currentBarTagSerializer.end();
			if (serialized != null) {
				LinkedHashMap<String, String> barTagAttrs = new LinkedHashMap<>();
				barTagAttrs.put("_barTags", serialized);
				currentElementStyleMap.put("barTag", barTagAttrs);
			}
		}
		
		// Process element based on context
		if (currentElementLabel == null) // No label, warning, nothing to process
			Log.warn("Element hasn't label");
		else {
			// Check if this element matches any pending command output label
			if (pendingOutputLabels.contains(currentElementLabel)) {
				// This element provides style for one of the command/express outputs
				processStyleElement(currentElementLabel, currentElementStyleMap);
				pendingOutputLabels.remove(currentElementLabel);
			}
			else {
				processPendingElements();
				processIndependentElement(currentElementLabel, currentElementType, currentElementStyleMap);
			}
		}
		
		inElement = false;
		elementDepth = 0;
	}

	private void processStyleElement(String label, Map<String, LinkedHashMap<String, String>> styleMap) {
		GeoElement geo = construction.getKernel().lookupLabel(label);
		String type = geo != null ? geo.getTypeString() : null;
		generateStyleSheet(geo, label, type, styleMap);
	}

	private void processIndependentElement(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		GeoElement geo = construction.getKernel().lookupLabel(label);
		String styleSheetName = generateStyleSheet(geo, label, type, styleMap);
		
		// Extract command from style map or GeoElement
		String command = extractCommandFromElement(label, type, styleMap);
		if (command != null && !command.isEmpty()) {
			// Build output label with visibility flags
			if (geo != null)
				GeoElementToGpadConverter.buildOutputLabel(output, geo);
			else
				output.append(label);
			
			if (styleSheetName != null)
				output.append(" @").append(styleSheetName);
			
			output.append(" = ").append(command).append(";\n");
		}
	}

	private String extractCommandFromElement(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		// Try to get from kernel lookup first
		GeoElement geo = construction.getKernel().lookupLabel(label);
		if (geo != null && geo.isIndependent())
			return GeoElementToGpadConverter.extractCommand(geo);
		// Fallback to style map parsing
		return extractCommandFromStyleMap(styleMap, label, type);
	}

	private String extractCommandFromStyleMap(Map<String, LinkedHashMap<String, String>> styleMap, String label, String type) {
		if (styleMap == null)
			return null;
		
		// Extract coords for points/vectors
		LinkedHashMap<String, String> coords = styleMap.get("coords");
		if (coords != null) {
			String x = coords.get("x");
			String y = coords.get("y");
			String z = coords.get("z");
			
			if (x != null && y != null) {
				if ("point".equals(type) || type == null) {
					// Check if label starts with lowercase
					boolean labelIsLowercase = label != null && !label.isEmpty() 
						&& StringUtil.isLowerCase(label.charAt(0));
					
					if (labelIsLowercase && z != null && !"1.0".equals(z)) {
						return "Point[{" + x + ", " + y + ", " + z + "}]";
					} else {
						return "Point[{" + x + ", " + y + "}]";
					}
				} else if ("vector".equals(type)) {
					// Check if label starts with uppercase
					boolean labelIsUppercase = label != null && !label.isEmpty() 
						&& !StringUtil.isLowerCase(label.charAt(0));
					
					if (labelIsUppercase && z != null && !"0.0".equals(z)) {
						return "Vector[{" + x + ", " + y + ", " + z + "}]";
					} else {
						return "Vector[{" + x + ", " + y + "}]";
					}
				}
			}
		}
		
		// Extract value for numeric/boolean
		LinkedHashMap<String, String> value = styleMap.get("value");
		if (value != null) {
			String val = value.get("val");
			if (val != null) {
				if ("boolean".equals(type) || "numeric".equals(type) || type == null)
					return val;
			}
		}
		
		return null;
	}

	private void processPendingElements() {
		// Check if there are unmatched style elements and log warnings
		if (!pendingOutputLabels.isEmpty()) {
			Log.warn("Command/expression ended but " + pendingOutputLabels.size()
				+ " style element(s) expected for output labels: " + pendingOutputLabels);
			pendingOutputLabels.clear();
		}
		outputPendingExpression();
		outputPendingCommand();
	}

	private String generateStyleSheet(GeoElement geo, String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		if (geo != null && styleMap != null && !styleMap.isEmpty())
			GeoElementToGpadConverter.filterStyleMap(styleMap, geo);

		// Convert style map to gpad format
		Object[] result = StyleMapToGpadConverter.convertToContentOnly(styleMap, type);
		String content = (result != null) ? (String) result[0] : null;
		boolean hasExpression = (result != null) ? ((Boolean) result[1]).booleanValue() : false;
		
		if (content == null || content.isEmpty())
			return null;

		// Check if we should merge with existing stylesheet
		String styleSheetName = null;
		boolean needGenerate = true;
		if (mergeStylesheets) {
			styleSheetName = styleSheetContentMap.get(content);
			if (styleSheetName != null)
				needGenerate = false;
		}
		
		if (styleSheetName == null) {
			// Generate new name
			if (label != null && !label.isEmpty())
				styleSheetName = label + "Style";
			else
				styleSheetName = "style" + (++styleSheetCounter);
			
			// Check if styleSheetName is duplicated
			while (generatedStyleSheets.contains(styleSheetName))
				styleSheetName = "style" + (++styleSheetCounter);
			
			// Store mapping for merging
			if (mergeStylesheets)
				styleSheetContentMap.put(content, styleSheetName);
		}

		// Always generate stylesheet definition immediately
		if (needGenerate) {
			output.append("@").append(styleSheetName).append(" = ").append(content).append("\n");
			generatedStyleSheets.add(styleSheetName);
		}
		
		// If stylesheet contains expressions, defer application to @@set
		if (hasExpression) {
			deferredStyleSheets.put(label, styleSheetName);
			return null;
		}
		
		// If stylesheet doesn't contain expressions, store it for output with label
		labelToStyleSheetName.put(label, styleSheetName);
		return styleSheetName;
	}

	private void outputPendingCommand() {
		if (currentCommandName != null && !currentOutputLabels.isEmpty()) {
			// Build command string: label1, label2, ... = CommandName(input1, input2, ...);
			boolean first = true;
			for (String label : currentOutputLabels) {
				if (label != null) {
					if (!first)
						output.append(", ");
					first = false;
					
					// Build output label with visibility flags
					GeoElement geo = construction.getKernel().lookupLabel(label);
					if (geo != null)
						GeoElementToGpadConverter.buildOutputLabel(output, geo);
					else
						output.append(label);
					
					// Add stylesheet name if available (and not deferred)
					String styleSheetName = labelToStyleSheetName.get(label);
					if (styleSheetName != null)
						output.append(" @").append(styleSheetName);
				}
			}
			
			// Build input arguments
			StringBuilder args = new StringBuilder();
			first = true;
			for (String arg : currentInputArgs) {
				if (arg != null) {
					if (!first)
						args.append(", ");
					first = false;
					args.append(arg);
				}
			}
			
			output.append(" = ").append(currentCommandName);
			if (args.length() > 0)
				output.append("(").append(args).append(")");
			output.append(";\n");
			
			// Clear command state
			currentCommandName = null;
			currentInputArgs.clear();
			currentOutputLabels.clear();
		}
	}
	
	private void outputPendingExpression() {
		if (currentExpressionLabel != null && currentExpressionExp != null) {
			// Build expression string: label = exp;
			// Build output label with visibility flags
			GeoElement geo = construction.getKernel().lookupLabel(currentExpressionLabel);
			if (geo != null)
				GeoElementToGpadConverter.buildOutputLabel(output, geo);
			else
				output.append(currentExpressionLabel);
			
			// Add stylesheet name if available (and not deferred)
			String styleSheetName = labelToStyleSheetName.get(currentExpressionLabel);
			if (styleSheetName != null)
				output.append(" @").append(styleSheetName);
			
			output.append(" = ").append(currentExpressionExp).append(";\n");
			
			currentExpressionLabel = null;
			currentExpressionExp = null;
		}
	}

	private void generateDeferredSetStatements(StringBuilder sb) {
		if (deferredStyleSheets.isEmpty())
			return;

		for (Map.Entry<String, String> entry : deferredStyleSheets.entrySet()) {
			String label = entry.getKey();
			String styleSheetName = entry.getValue();

			if (label == null || label.isEmpty())
				continue;

			sb.append("@@set ").append(label).append(" @").append(styleSheetName).append(";\n");
		}
	}
}
