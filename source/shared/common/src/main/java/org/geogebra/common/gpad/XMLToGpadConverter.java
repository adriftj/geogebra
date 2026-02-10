package org.geogebra.common.gpad;

import java.io.IOException;
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
import org.geogebra.common.util.debug.Log;

/**
 * Converts GeoGebra construction XML to Gpad format by parsing XML directly.
 * This is an alternative implementation to GgbToGpadConverter that works from XML
 * instead of iterating through ConstructionElement objects.
 */
public class XMLToGpadConverter implements DocHandler {
	private final String xmlFile;
	private final String xmlMacro;
	private final GpadGenerator gpadGenerator;
	// Map from label to visibility flags (* and ~)
	private final Map<String, String> labelToVisibilityFlags = new HashMap<>();

	// Current parsing state
	private String currentCommandName = null;
	private List<String> currentInputArgs = new ArrayList<>();
	private List<String> currentOutputLabels = new ArrayList<>();
	private String currentExpressionLabel = null;
	private String currentExpressionExp = null;
	private String currentExpressionType = null;
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
	// Map from label to styleMap for dependency extraction
	private Map<String, Map<String, LinkedHashMap<String, String>>> labelToStyleMap = new HashMap<>();

	/**
	 * Creates a new XMLToGpadConverter.
	 * 
	 * @param xmlFile complete GeoGebra XML file (with <geogebra> as root element), containing construction content
	 * @param xmlMacro complete macro XML (with <geogebra> as root element), containing all macro definitions (may contain multiple <macro> elements)
	 * @param mergeStylesheets whether to merge identical stylesheets
	 */
	public XMLToGpadConverter(String xmlFile, String xmlMacro, boolean mergeStylesheets) {
		this.xmlFile = xmlFile;
		this.xmlMacro = xmlMacro;
		this.gpadGenerator = new GpadGenerator(mergeStylesheets, false);
	}

	/**
	 * Internal constructor for parsing construction XML fragment only.
	 * Used when converting macro constructions or other fragments.
	 * 
	 * @param mergeStylesheets whether to merge identical stylesheets
	 * @param inMacroConstruction whether this converter is for macro construction (needs indentation)
	 */
	private XMLToGpadConverter(boolean mergeStylesheets, boolean inMacroConstruction) {
		this.xmlFile = null;
		this.xmlMacro = null;
		this.gpadGenerator = new GpadGenerator(mergeStylesheets, inMacroConstruction);
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
		if (xmlMacro != null && !xmlMacro.trim().isEmpty())
			convertMacrosXML(xmlMacro, sb);
		
		// Then convert the main construction
		if (xmlFile != null && !xmlFile.trim().isEmpty())
			convertConstructionXML(xmlFile, sb);
		
		return sb.toString();
	}

	/**
	 * Common method to parse XML using a DocHandler.
	 * 
	 * @param handler the DocHandler to use for parsing
	 * @param xml the XML string to parse
	 * @throws XMLParseException if parsing fails
	 * @throws IOException if IO error occurs
	 */
	private static void parseXML(DocHandler handler, String xml) throws XMLParseException, IOException {
		QDParser parser = new QDParser();
		parser.parse(handler, new StringReader(xml));
	}

	/**
	 * Converts all macros to gpad format from XML.
	 * Macros are output directly when </macro> is encountered during parsing.
	 * 
	 * @param xmlMacro complete macro XML (with <geogebra> as root element)
	 * @param sb string builder to append to
	 */
	private void convertMacrosXML(String xmlMacro, StringBuilder sb) {
		if (xmlMacro == null || xmlMacro.trim().isEmpty())
			return;
		
		// Parse macro XML and output macros directly
		MacroParserHandler handler = new MacroParserHandler(this, sb);
		try {
			parseXML(handler, xmlMacro);
		} catch (XMLParseException e) {
			Log.error("Failed to parse macro XML: " + e.getMessage());
		} catch (IOException e) {
			Log.error("IO error parsing macro XML: " + e.getMessage());
		} catch (Exception e) {
			Log.error("Error parsing macro XML: " + e.getMessage());
		}
	}

	/**
	 * Internal class to store macro information parsed from XML.
	 */
	private static class MacroInfo {
		String name;
		List<String> inputLabels;
		List<String> outputLabels;
		String constructionGpad; // Store gpad output directly, no XML needed
		
		MacroInfo() {
			inputLabels = new ArrayList<>();
			outputLabels = new ArrayList<>();
		}
	}
	
	
	/**
	 * Builds comma-separated list of labels from a list of strings.
	 */
	private void buildLabels(StringBuilder sb, List<String> labels) {
		boolean first = true;
		if (labels != null) {
			for (String label : labels) {
				if (label != null && !label.isEmpty()) {
					if (!first)
						sb.append(", ");
					first = false;
					sb.append(label);
				}
			}
		}
	}

	/**
	 * DocHandler implementation for parsing macro XML.
	 * Uses nested DocHandler to directly convert construction without XML string concatenation.
	 * Outputs macros directly when </macro> is encountered.
	 */
	private class MacroParserHandler implements DocHandler {
		private XMLToGpadConverter parentConverter;
		private StringBuilder output;
		private MacroInfo currentMacro = null;
		private boolean inMacro = false;
		private boolean inConstruction = false;
		private XMLToGpadConverter constructionConverter = null; // Nested converter for construction
		private int constructionDepth = 0;
		
		MacroParserHandler(XMLToGpadConverter parentConverter, StringBuilder output) {
			this.parentConverter = parentConverter;
			this.output = output;
		}
		
		@Override
		public void startDocument() throws XMLParseException {
		}
		
		@Override
		public void startElement(String tag, LinkedHashMap<String, String> attrs) throws XMLParseException {
			if ("macro".equals(tag)) {
				inMacro = true;
				currentMacro = new MacroInfo();
				currentMacro.name = attrs.get("cmdName");
				if (currentMacro.name == null || currentMacro.name.isEmpty()) {
					Log.error("Macro has no cmdName attribute, skipping");
					currentMacro = null;
					inMacro = false;
				}
			} else if (inMacro && "macroInput".equals(tag)) {
				XMLToGpadConverter.extractIndexedAttributes(attrs, currentMacro.inputLabels);
			} else if (inMacro && "macroOutput".equals(tag)) {
				XMLToGpadConverter.extractIndexedAttributes(attrs, currentMacro.outputLabels);
			} else if (inMacro && "construction".equals(tag)) {
				// Start of construction: create nested converter to handle it directly
				// Mark it as macro construction so it adds indentation when generating
				inConstruction = true;
				constructionDepth = 1;
				constructionConverter = new XMLToGpadConverter(parentConverter.gpadGenerator.mergeStylesheets, true);
				constructionConverter.startDocument();
				// Forward the construction start to the nested converter
				constructionConverter.startElement("construction", attrs);
			} else if (inConstruction && constructionConverter != null) {
				// Forward all elements inside construction to the nested converter
				constructionDepth++;
				constructionConverter.startElement(tag, attrs);
			}
		}
		
		@Override
		public void endElement(String tag) throws XMLParseException {
			if ("macro".equals(tag)) {
				if (currentMacro != null) {
					// When </macro> is encountered, get gpad output from nested converter and output directly
					if (constructionConverter != null) {
						StringBuilder macroOutput = new StringBuilder();
						constructionConverter.processAndOutputCollectedObjects(macroOutput);
						constructionConverter.endDocument();
						currentMacro.constructionGpad = macroOutput.toString();
					}
					// Output macro directly
					convertMacro(currentMacro, output);
				}
				currentMacro = null;
				inMacro = false;
				inConstruction = false;
				constructionConverter = null;
				constructionDepth = 0;
			} else if (inConstruction && "construction".equals(tag)) {
				constructionDepth--;
				if (constructionDepth == 0) {
					// End of construction: forward to nested converter
					if (constructionConverter != null) {
						constructionConverter.endElement("construction");
					}
					inConstruction = false;
				} else if (constructionConverter != null) {
					constructionConverter.endElement(tag);
				}
			} else if (inConstruction && constructionConverter != null) {
				constructionDepth--;
				constructionConverter.endElement(tag);
			}
		}
		
		@Override
		public void text(String str) throws XMLParseException {
			if (inConstruction && constructionConverter != null) {
				constructionConverter.text(str);
			}
		}
		
		@Override
		public void endDocument() throws XMLParseException {
		}
	}
	
	/**
	 * Converts a single macro to gpad format.
	 * Now uses pre-converted gpad output from MacroInfo.
	 * 
	 * @param macroInfo the macro information to convert
	 * @param sb string builder to append to
	 */
	private void convertMacro(MacroInfo macroInfo, StringBuilder sb) {
		if (macroInfo.name == null || macroInfo.name.isEmpty()) {
			Log.error("Macro has no name, skipping");
			return;
		}
		
		String gpad = macroInfo.constructionGpad;
		if (gpad == null || gpad.trim().isEmpty()) {
			Log.error("Macro " + macroInfo.name + " has no construction gpad, skipping");
			return;
		}
		
		// Start macro definition: @@macro macroName(input1, input2, ...) {
		sb.append("@@macro ").append(macroInfo.name).append("(");
		buildLabels(sb, macroInfo.inputLabels);
		sb.append(") {\n").append(gpad);
		
		// End macro definition: @@return output1, output2, ... }
		// Note: @@return statement does NOT end with semicolon
		sb.append("    @@return ");
		buildLabels(sb, macroInfo.outputLabels);
		sb.append("\n}\n\n");
	}

	/**
	 * Converts construction XML to gpad format.
	 * Uses nested DocHandler to directly convert construction without XML string extraction.
	 * 
	 * @param xmlFile complete GeoGebra XML file (with <geogebra> as root element)
	 * @param sb string builder to append to
	 */
	private void convertConstructionXML(String xmlFile, StringBuilder sb) {
		if (xmlFile == null || xmlFile.trim().isEmpty())
			return;
		
		ConstructionParserHandler handler = new ConstructionParserHandler(this);
		try {
			parseXML(handler, xmlFile);
			// Process collected objects and output in sorted order
			processAndOutputCollectedObjects(sb);
		} catch (XMLParseException e) {
			Log.error("Failed to parse construction XML: " + e.getMessage());
		} catch (IOException e) {
			Log.error("IO error parsing construction XML: " + e.getMessage());
		} catch (Exception e) {
			Log.error("Error parsing construction XML: " + e.getMessage());
		}
	}
	
	/**
	 * Process collected objects: collect pending elements, build dependency graph,
	 * perform topological sort, and output stylesheets and objects in sorted order.
	 * 
	 * @param sb string builder to append output to
	 */
	private void processAndOutputCollectedObjects(StringBuilder sb) {
		// Collect any pending command/expression at the end
		collectPendingElements();
		
		// Use GpadGenerator to handle dependency detection, topological sort, and output
		gpadGenerator.generate(sb);
	}

	/**
	 * DocHandler implementation for parsing construction from complete GeoGebra XML.
	 * Uses nested DocHandler to directly convert construction without XML string extraction.
	 */
	private class ConstructionParserHandler implements DocHandler {
		private XMLToGpadConverter constructionConverter;
		private boolean inConstruction = false;
		private int constructionDepth = 0;
		
		ConstructionParserHandler(XMLToGpadConverter converter) {
			this.constructionConverter = converter;
		}
		
		@Override
		public void startDocument() throws XMLParseException {
			inConstruction = false;
			constructionDepth = 0;
			constructionConverter.resetState();
		}
		
		@Override
		public void startElement(String tag, LinkedHashMap<String, String> attrs) throws XMLParseException {
			if ("construction".equals(tag)) {
				// Start of construction: use the converter directly
				inConstruction = true;
				constructionDepth = 1;
				constructionConverter.startElement("construction", attrs);
			} else if (inConstruction && constructionConverter != null) {
				// Forward all elements inside construction to the converter
				constructionDepth++;
				constructionConverter.startElement(tag, attrs);
			}
		}
		
		@Override
		public void endElement(String tag) throws XMLParseException {
			if (inConstruction && "construction".equals(tag)) {
				constructionDepth--;
				if (constructionDepth == 0) {
					// End of construction: forward to converter
					constructionConverter.endElement("construction");
					inConstruction = false;
				} else if (constructionConverter != null)
					constructionConverter.endElement(tag);
			} else if (inConstruction && constructionConverter != null) {
				constructionDepth--;
				constructionConverter.endElement(tag);
			}
		}
		
		@Override
		public void text(String str) throws XMLParseException {
			if (inConstruction && constructionConverter != null) {
				constructionConverter.text(str);
			}
		}
		
		@Override
		public void endDocument() throws XMLParseException {
		}
	}

	private void resetState() {
		currentCommandName = null;
		currentInputArgs.clear();
		currentOutputLabels.clear();
		currentExpressionLabel = null;
		currentExpressionExp = null;
		currentExpressionType = null;
		currentElementLabel = null;
		currentElementType = null;
		currentElementStyleMap = null;
		currentStartPointSerializer = null;
		currentBarTagSerializer = null;
		labelToVisibilityFlags.clear();
		labelToStyleSheetName.clear();
		labelToStyleMap.clear();
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
		if ("command".equals(tag))
			endCommand();
		else if ("expression".equals(tag))
			endExpression();
		else if ("element".equals(tag))
			endElement();
		else if (inElement && elementDepth > 0)
			elementDepth--;
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
		// Keep empty labels as empty strings for matching
		pendingOutputLabels = currentOutputLabels.stream()
								.filter(Objects::nonNull).collect(Collectors.toSet());
		// Command will be collected when next command/expression/element starts or at end of document
	}

	private void startExpression(LinkedHashMap<String, String> attrs) {
		currentExpressionLabel = attrs.get("label");
		currentExpressionExp = attrs.get("exp");
		currentExpressionType = null;
	}

	private void endExpression() {
		pendingOutputLabels.clear();
		if (currentExpressionLabel != null) {
			pendingOutputLabels.add(currentExpressionLabel);
		}
	}

	private void startElement(LinkedHashMap<String, String> attrs) {
		inElement = true;
		elementDepth = 0;
		currentElementLabel = attrs.get("label");
		// Keep empty label as empty string for matching with pendingOutputLabels
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
		
		// Special handling for show element: extract visibility flags
		if ("show".equals(tag) && currentElementLabel != null) {
			String objectAttr = attrs.get("object");
			String labelAttr = attrs.get("label");
			String visibilityFlags = getVisibilityFlags(objectAttr, labelAttr);
			labelToVisibilityFlags.put(currentElementLabel, visibilityFlags);
		}
		
		// Special handling for startPoint: collect for later serialization
		if ("startPoint".equals(tag)) {
			if (currentStartPointSerializer != null)
				currentStartPointSerializer.add(elementAttrs);
		} else if ("tag".equals(tag)) {
			// Special handling for tag: collect by barNumber for later serialization
			String barNumberStr = elementAttrs.get("barNumber");
			if (currentBarTagSerializer != null && barNumberStr != null)
				currentBarTagSerializer.add(barNumberStr, elementAttrs);
		} else  // Store child element of <element> in the style map
			currentElementStyleMap.put(tag, elementAttrs);
	}
	
	/**
	 * Get visibility flags string from show element attributes.
	 * * flag: when object attribute is false
	 * ~ flag: when label attribute is false (only meaningful when object is true)
	 * 
	 * @param objectAttr object attribute value ("true" or "false")
	 * @param labelAttr label attribute value ("true" or "false")
	 * @return visibility flags string (* and/or ~), or empty string if none
	 */
	private static String getVisibilityFlags(String objectAttr, String labelAttr) {
		// * flag: when object is false
		if ("false".equals(objectAttr))
			return "*";
		// ~ flag: when label is false and object is true
		if ("false".equals(labelAttr))
			return "~";
		return "";
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
			
			// Store styleMap for dependency extraction (make a copy to avoid modification)
			Map<String, LinkedHashMap<String, String>> styleMapCopy = new LinkedHashMap<>();
			if (currentElementStyleMap != null) {
				for (Map.Entry<String, LinkedHashMap<String, String>> entry : currentElementStyleMap.entrySet()) {
					LinkedHashMap<String, String> attrsCopy = new LinkedHashMap<>(entry.getValue());
					styleMapCopy.put(entry.getKey(), attrsCopy);
				}
			}
			labelToStyleMap.put(currentElementLabel, styleMapCopy);
			
			// Check if this element matches any pending command output label
			if (pendingOutputLabels.contains(currentElementLabel)) {
				// This element provides style for one of the command/expression outputs
				// Generate stylesheet immediately and store for later use
				String styleSheetName = generateStyleSheet(currentElementLabel, currentElementType, currentElementStyleMap);
				labelToStyleSheetName.put(currentElementLabel, styleSheetName);
				currentExpressionType = currentElementType;
				pendingOutputLabels.remove(currentElementLabel);
			}
			else {
				// Collect pending elements first
				collectPendingElements();
				// Collect independent element
				collectIndependentElement(currentElementLabel, currentElementType, currentElementStyleMap);
			}
		}
		
		inElement = false;
		elementDepth = 0;
	}
	
	/**
	 * Collect independent element information instead of outputting it immediately.
	 */
	private void collectIndependentElement(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		// Extract command from style map
		String command = extractCommandFromStyleMap(label, type, styleMap);
		if (command != null && !command.isEmpty()) {
			GpadGenerator.SingleElementInfo elemInfo = new GpadGenerator.SingleElementInfo();
			elemInfo.label = label;
			elemInfo.visibilityFlags = labelToVisibilityFlags.get(label);
			
			// Create CollectedItem
			GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
			item.elements.add(elemInfo);
			item.commandString = command;
			
			// Generate stylesheet immediately
			elemInfo.styleSheetName = generateStyleSheet(label, type, styleMap);
			
			// Extract attribute values for dependency extraction
			if (styleMap != null && !styleMap.isEmpty())
				extractAttributeValuesForDependency(styleMap, item.regularAttributeValues, item.jsAttributeValues);
			
			gpadGenerator.addCollectedItem(item);
		}
	}
	private String extractCommandFromStyleMap(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
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
					if (z != null && !"1.0".equals(z) && !"1".equals(z))
						return "Point(" + x + ", " + y + ", " + z + ")";
					else
						return "Point(" + x + ", " + y + ")";
				} else if ("vector".equals(type)) {
					if (z != null && !"0.0".equals(z) && !"0".equals(z))
						return "Vector(" + x + ", " + y + ", " + z + ")";
					else
						return "Vector(" + x + ", " + y + ")";
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
		
		// Extract caption and file for Button
		if ("button".equals(type)) {
			LinkedHashMap<String, String> caption = styleMap.get("caption");
			LinkedHashMap<String, String> file = styleMap.get("file");
			
			String captionVal = (caption != null) ? caption.get("val") : null;
			String fileName = (file != null) ? file.get("name") : null;
			
			// Build Button command based on available parameters
			// Button command syntax: Button[], Button["caption"], Button["caption", "image"]
			// If image is provided but no caption, use empty string for caption
			StringBuilder cmd = new StringBuilder("Button(");
			boolean hasParams = false;
			
			// Add caption if available, or empty string if image is provided but no caption
			if (captionVal != null && !captionVal.isEmpty()) {
				cmd.append("\"").append(safeString(captionVal)).append("\"");
				hasParams = true;
			} else if (fileName != null && !fileName.isEmpty()) {
				// If only image is provided, use empty string as caption (first parameter is required)
				cmd.append("\"\"");
				hasParams = true;
			}
			
			// Add image if available
			if (fileName != null && !fileName.isEmpty()) {
				if (hasParams)
					cmd.append(", ");
				cmd.append("\"").append(safeString(fileName)).append("\"");
			}
			
			cmd.append(")");
			return cmd.toString();
		}
		
		// Extract file for Image
		if ("image".equals(type)) {
			LinkedHashMap<String, String> file = styleMap.get("file");
			String fileName = (file != null) ? file.get("name") : "";
			if (fileName == null) fileName = "";
			return "Image(\"" +safeString(fileName) + "\")";
		}
		
		return null;
	}

	/**
	 * Safe string for use in Gpad format (remove double quotes).
	 * 
	 * @param str string to safe
	 * @return safe string
	 */
	private static String safeString(String str) {
		if (str == null)
			return "";
		return str.replace("\"", "");
	}

	private void processPendingElements() {
		// Check if there are unmatched style elements and log warnings
		if (!pendingOutputLabels.isEmpty()) {
			Log.warn("Command/expression ended but " + pendingOutputLabels.size()
				+ " style element(s) expected for output labels: " + pendingOutputLabels);
			pendingOutputLabels.clear();
		}
		collectPendingExpression();
		collectPendingCommand();
	}
	
	/**
	 * Collect pending elements instead of outputting them immediately.
	 * This is used during the collection phase.
	 */
	private void collectPendingElements() {
		// Check if there are unmatched style elements and log warnings
		if (!pendingOutputLabels.isEmpty()) {
			Log.warn("Command/expression ended but " + pendingOutputLabels.size()
				+ " style element(s) expected for output labels: " + pendingOutputLabels);
			pendingOutputLabels.clear();
		}
		collectPendingExpression();
		collectPendingCommand();
	}
	
	/**
	 * Generate stylesheet immediately and return stylesheet name.
	 * Delegates to GpadGenerator.
	 * 
	 * @param label label for naming the stylesheet
	 * @param type element type
	 * @param styleMap style map to convert
	 * @return stylesheet name, or null if no stylesheet needed
	 */
	private String generateStyleSheet(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		return gpadGenerator.generateStyleSheet(label, type, styleMap);
	}
	
	/**
	 * Extract attribute values from style map for dependency extraction.
	 * Delegates to GpadGenerator.
	 * 
	 * @param styleMap style map to extract from
	 * @param regularValues output list for regular attribute values
	 * @param jsValues output list for JavaScript attribute values
	 */
	private void extractAttributeValuesForDependency(Map<String, LinkedHashMap<String, String>> styleMap, 
	                                                  List<String> regularValues, List<String> jsValues) {
		gpadGenerator.extractAttributeValuesForDependency(styleMap, regularValues, jsValues);
	}
	
	/**
	 * Merge style maps from multiple outputs, extracting attribute values for dependency extraction.
	 * Returns two lists: regular attribute values and JavaScript attribute values.
	 * 
	 * @param styleMaps list of style maps to merge
	 * @return array with two elements: [regularValues, jsValues]
	 */
	@SuppressWarnings("unchecked")
	private List<String>[] mergeStyleMapsForDependency(List<Map<String, LinkedHashMap<String, String>>> styleMaps) {
		List<String> regularValues = new ArrayList<>();
		List<String> jsValues = new ArrayList<>();
		for (Map<String, LinkedHashMap<String, String>> styleMap : styleMaps)
			extractAttributeValuesForDependency(styleMap, regularValues, jsValues);
		return new List[] { regularValues, jsValues };
	}
	
	/**
	 * Collect command information instead of outputting it immediately.
	 */
	private void collectPendingCommand() {
		if (currentCommandName != null && !currentOutputLabels.isEmpty()) {
			// Build command string for storage
			StringBuilder cmdBuilder = new StringBuilder();
			boolean first = true;
			for (String arg : currentInputArgs) {
				if (arg != null) {
					if (!first)
						cmdBuilder.append(", ");
					first = false;
					cmdBuilder.append(arg);
				}
			}
			String argsStr = cmdBuilder.length() > 0 ? "(" + cmdBuilder.toString() + ")" : "";
			String commandStr = currentCommandName + argsStr;
			
			// Create CollectedItem
			GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
			item.commandString = commandStr;
			
			// Collect all outputs with their stylesheets (generated immediately)
			List<Map<String, LinkedHashMap<String, String>>> outputStyleMaps = new ArrayList<>();
			for (String label : currentOutputLabels) {
				if (label == null)
					continue;
				
				String visibilityFlags = labelToVisibilityFlags.get(label);
				Map<String, LinkedHashMap<String, String>> styleMap = labelToStyleMap.get(label);
				
				// Generate stylesheet immediately
				String styleSheetName = null;
				if (styleMap != null && !styleMap.isEmpty()) {
					// Get element type from styleMap (we need to find it from collected items or use a default)
					// For commands, we don't have a single type, so we'll use null
					styleSheetName = generateStyleSheet(label, null, styleMap);
				}
				
				item.elements.add(new GpadGenerator.SingleElementInfo(label, visibilityFlags, styleSheetName));
				outputStyleMaps.add(styleMap);
			}
			
			// Extract attribute values from all outputs for dependency extraction
			List<String>[] dependencyValues = mergeStyleMapsForDependency(outputStyleMaps);
			item.regularAttributeValues = dependencyValues[0];
			item.jsAttributeValues = dependencyValues[1];
			
			// Add input arguments to regularAttributeValues for dependency extraction
			for (String arg : currentInputArgs) {
				if (arg != null && !arg.isEmpty()) {
					item.regularAttributeValues.add(arg);
				}
			}
			
			gpadGenerator.addCollectedItem(item);

			// Clear command state
			currentCommandName = null;
			currentInputArgs.clear();
			currentOutputLabels.clear();
		}
	}
	
	/**
	 * Collect expression information instead of outputting it immediately.
	 */
	private void collectPendingExpression() {
		if (currentExpressionLabel != null && currentExpressionExp != null) {
			GpadGenerator.SingleElementInfo exprInfo = new GpadGenerator.SingleElementInfo();
			exprInfo.label = currentExpressionLabel;
			exprInfo.visibilityFlags = labelToVisibilityFlags.get(currentExpressionLabel);
			
			String exp = currentExpressionExp;
			if ("function".equals(currentExpressionType)) {
				// function's exp should like "f(x, y) = ..."
				int equalsIndex = exp.indexOf('=');
				if (equalsIndex >= 0) { // Found "="
					exprInfo.label = exp.substring(0, equalsIndex).trim();
					exp = exp.substring(equalsIndex + 1).trim();
				}
			} else if ("point".equals(currentExpressionType)) {
				// Convert (x, y) or (x, y, z) format to Point(x, y) or Point(x, y, z) format
				String trimmedExp = exp.trim();
				if (trimmedExp.startsWith("(") && trimmedExp.endsWith(")")) {
					String content = trimmedExp.substring(1, trimmedExp.length() - 1);
					exp = "Point(" + content + ")";
				}
			} else if ("vector".equals(currentExpressionType)) {
				// Convert (x, y) or (x, y, z) format to Vector(x, y) or Vector(x, y, z) format
				String trimmedExp = exp.trim();
				if (trimmedExp.startsWith("(") && trimmedExp.endsWith(")")) {
					String content = trimmedExp.substring(1, trimmedExp.length() - 1);
					exp = "Vector(" + content + ")";
				}
			}
			
			// Create CollectedItem
			GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
			item.elements.add(exprInfo);
			item.commandString = exp;
			
			// Add expression to regularAttributeValues for dependency extraction
			if (exp != null && !exp.isEmpty())
				item.regularAttributeValues.add(exp);
			
			// Get styleMap and generate stylesheet immediately
			Map<String, LinkedHashMap<String, String>> styleMap = labelToStyleMap.get(exprInfo.label);
			if (styleMap != null && !styleMap.isEmpty()) {
				exprInfo.styleSheetName = generateStyleSheet(exprInfo.label, currentExpressionType, styleMap);
				// Extract attribute values for dependency extraction
				extractAttributeValuesForDependency(styleMap, item.regularAttributeValues, item.jsAttributeValues);
			}
			
			gpadGenerator.addCollectedItem(item);
			
			currentExpressionLabel = null;
			currentExpressionExp = null;
		}
	}

	
}
