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
import org.geogebra.common.util.StringUtil;
import org.geogebra.common.util.debug.Log;

/**
 * Converts GeoGebra construction XML to Gpad format by parsing XML directly.
 * This is an alternative implementation to GgbToGpadConverter that works from XML
 * instead of iterating through ConstructionElement objects.
 */
public class XMLToGpadConverter implements DocHandler {
	private final String xmlFile;
	private final String xmlMacro;
	private final boolean mergeStylesheets;
	private final Map<String, String> styleSheetContentMap = new HashMap<>();
	private final Set<String> generatedStyleSheets = new java.util.HashSet<>();
	private final Map<String, String> deferredStyleSheets = new HashMap<>();
	private int styleSheetCounter = 0;
	// Map from label to visibility flags (* and ~)
	private final Map<String, String> labelToVisibilityFlags = new HashMap<>();

	// Current parsing state
	private StringBuilder output = new StringBuilder();
	private boolean inMacroConstruction = false; // Flag to indicate if we're generating macro construction content
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
		this.mergeStylesheets = mergeStylesheets;
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
		this.mergeStylesheets = mergeStylesheets;
		this.inMacroConstruction = inMacroConstruction;
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
				constructionConverter = new XMLToGpadConverter(parentConverter.mergeStylesheets, true);
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
						// Process pending elements and generate deferred set statements before endDocument
						constructionConverter.processPendingElements();
						constructionConverter.generateDeferredSetStatements(constructionConverter.output);
						constructionConverter.endDocument();
						currentMacro.constructionGpad = constructionConverter.output.toString();
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
			// Output any pending command/expression at the end
			processPendingElements();
			// Generate deferred @@set statements for stylesheets with expressions
			generateDeferredSetStatements(output);
			sb.append(output.toString());
		} catch (XMLParseException e) {
			Log.error("Failed to parse construction XML: " + e.getMessage());
		} catch (IOException e) {
			Log.error("IO error parsing construction XML: " + e.getMessage());
		} catch (Exception e) {
			Log.error("Error parsing construction XML: " + e.getMessage());
		}
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
			constructionConverter.output = new StringBuilder();
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
			if (visibilityFlags != null && !visibilityFlags.isEmpty())
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
		} else {
			// Store child element of <element> in the style map
			currentElementStyleMap.put(tag, elementAttrs);
		}
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

	private void buildOutputLabel(String label) {
		// If label is empty, use temporary label for output
		// Use fixed suffix 1459 to avoid conflicts with other labels
		String outputLabel = label;
		if (label != null && label.isEmpty())
			outputLabel = "OriginalEmpty1459";
		output.append(outputLabel);
		// Add visibility flags if available (use original label for lookup)
		String visibilityFlags = labelToVisibilityFlags.get(label);
		if (visibilityFlags != null && !visibilityFlags.isEmpty())
			output.append(visibilityFlags);
	}

	private void processStyleElement(String label, Map<String, LinkedHashMap<String, String>> styleMap) {
		// Get type from styleMap if available, otherwise use null
		String type = currentElementType;
		generateStyleSheet(label, type, styleMap);
	}

	private void processIndependentElement(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		// Extract command from style map
		String command = extractCommandFromStyleMap(label, type, styleMap);
		if (command != null && !command.isEmpty()) {
			String styleSheetName = generateStyleSheet(label, type, styleMap);

			if (inMacroConstruction) output.append("    ");
			// Build output label with visibility flags
			buildOutputLabel(label);
			if (styleSheetName != null)
				output.append(" @").append(styleSheetName);
			
			output.append(" = ").append(command).append(";\n");
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
					// Check if label starts with lowercase
					boolean labelIsLowercase = label != null && !label.isEmpty() 
						&& StringUtil.isLowerCase(label.charAt(0));
					
					if (labelIsLowercase && z != null && !"1.0".equals(z))
						return "Point[{" + x + ", " + y + ", " + z + "}]";
					else
						return "Point[{" + x + ", " + y + "}]";
				} else if ("vector".equals(type)) {
					// Check if label starts with uppercase
					boolean labelIsUppercase = label != null && !label.isEmpty() 
						&& !StringUtil.isLowerCase(label.charAt(0));
					
					if (labelIsUppercase && z != null && !"0.0".equals(z))
						return "Vector[{" + x + ", " + y + ", " + z + "}]";
					else
						return "Vector[{" + x + ", " + y + "}]";
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
	 * Filters style map before conversion to Gpad format.
	 * - Removes object and label attributes from show style
	 * - Removes EuclidianView display styles for objects that are not shown in geometry view
	 * - Removes file style for independent GeoImage objects (filename is already in Image command)
	 * 
	 * @param styleMap style map to filter (modified in place)
	 * @param type element type from XML (e.g., "point", "numeric", "image")
	 */
	private static void filterStyleMap(Map<String, LinkedHashMap<String, String>> styleMap, String type) {
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
		
		// Filter EuclidianView display styles for objects not shown in geometry view
		// If there's no <show> element in XML, the object is not euclidian showable
		// Note: we check if "show" was in the original styleMap before we removed it
		boolean isEuclidianShowable = showAttrs != null;
		if (!isEuclidianShowable) {
			// Remove EuclidianView display styles
			for (String styleKey : EUCLIDIAN_DISPLAY_STYLES)
				styleMap.remove(styleKey);
		}
		
		// Remove file style(already included in the command)
		styleMap.remove("file");
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
		outputPendingExpression();
		outputPendingCommand();
		labelToVisibilityFlags.clear();
	}

	private String generateStyleSheet(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		filterStyleMap(styleMap, type);

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
			if (inMacroConstruction) output.append("    ");
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
			if (inMacroConstruction) output.append("    ");
			// Build command string: label1 @style1, label2, ... = CommandName(input1, input2, ...);
			boolean first = true;
			for (String label : currentOutputLabels) {
				if (!first)
					output.append(", ");
				first = false;
				buildOutputLabel(label);
				// Add stylesheet name if available (and not deferred)
				String styleSheetName = labelToStyleSheetName.get(label);
				if (styleSheetName != null)
					output.append(" @").append(styleSheetName);
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
			// Build expression string: label @labelStyle = exp;
			// Add stylesheet name if available (and not deferred)
			String styleSheetName = labelToStyleSheetName.get(currentExpressionLabel);
			if (inMacroConstruction) output.append("    ");
			String visibilityFlags = labelToVisibilityFlags.get(currentExpressionLabel);
			if ("function".equals(currentElementType)) {
				// fuction's exp should like "f(x, y) = ..."
				int equalsIndex = currentExpressionExp.indexOf('=');
				if (equalsIndex >= 0) { // Found "="
					currentExpressionLabel = currentExpressionExp.substring(0, equalsIndex).trim();
					currentExpressionExp = currentExpressionExp.substring(equalsIndex + 1).trim();
				}
			} else if ("point".equals(currentElementType)) {
				// Check if label starts with lowercase
				boolean labelIsLowercase = currentExpressionLabel != null && !currentExpressionLabel.isEmpty()
						&& StringUtil.isLowerCase(currentExpressionLabel.charAt(0));
				if (labelIsLowercase) {
					String trimmedExp = currentExpressionExp.trim();
					if (trimmedExp.startsWith("(") && trimmedExp.endsWith(")")) {
						// Extract content inside parentheses: "(200, 300)" -> "200, 300"
						String content = trimmedExp.substring(1, trimmedExp.length() - 1);
						currentExpressionExp = "Point[{" + content + "}]";
					}
				}
			} else if ("vector".equals(currentElementType)) {
				// Check if label starts with uppercase
				boolean labelIsUppercase = currentExpressionLabel != null && !currentExpressionLabel.isEmpty()
						&& !StringUtil.isLowerCase(currentExpressionLabel.charAt(0));
				if (labelIsUppercase) {
					String trimmedExp = currentExpressionExp.trim();
					if (trimmedExp.startsWith("(") && trimmedExp.endsWith(")")) {
						// Extract content inside parentheses: "(200, 300)" -> "200, 300"
						String content = trimmedExp.substring(1, trimmedExp.length() - 1);
						currentExpressionExp = "Vector[{" + content + "}]";
					}
				}
			}

			output.append(currentExpressionLabel);
			if (visibilityFlags != null && !visibilityFlags.isEmpty())
				output.append(visibilityFlags);
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

			if (inMacroConstruction) output.append("    ");
			sb.append("@@set ").append(label).append(" @").append(styleSheetName).append(";\n");
		}
	}
}
