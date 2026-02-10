package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geogebra.common.util.debug.Log;

/**
 * Generates Gpad output from collected items with dependency detection and topological sorting.
 * Handles dependency graph construction, topological sorting, and output generation.
 * Manages shared state for stylesheet generation and dependency extraction.
 */
public class GpadGenerator {
	// Shared state for stylesheet generation and dependency extraction
	private final Map<String, String> styleSheetContentMap = new HashMap<>();
	private final Set<String> generatedStyleSheets = new HashSet<>();
	private final Set<String> allLabels = new HashSet<>();
	private final List<CollectedItem> collectedItems = new ArrayList<>();
	private int styleSheetCounter = 0;
	final boolean mergeStylesheets; // Package-private for access from XMLToGpadConverter
	private final boolean inMacroConstruction; // Whether this is for macro construction (needs indentation)
	
	/**
	 * Creates a new GpadGenerator.
	 * 
	 * @param mergeStylesheets whether to merge identical stylesheets
	 * @param inMacroConstruction whether this is for macro construction (needs indentation)
	 */
	public GpadGenerator(boolean mergeStylesheets, boolean inMacroConstruction) {
		this.mergeStylesheets = mergeStylesheets;
		this.inMacroConstruction = inMacroConstruction;
	}
	/**
	 * Internal class to store single element information.
	 * Used for both Command outputs and Expression/IndependentElement.
	 * Command is essentially a way to generate multiple SingleElementInfo simultaneously.
	 */
	public static class SingleElementInfo {
		public String label;
		public String visibilityFlags;
		public String styleSheetName;
		
		public SingleElementInfo() {
		}
		
		public SingleElementInfo(String label, String visibilityFlags, String styleSheetName) {
			this.label = label;
			this.visibilityFlags = visibilityFlags;
			this.styleSheetName = styleSheetName;
		}
	}
	
	/**
	 * Union type for all collected items (Command/Expression/IndependentElement).
	 * - List size > 1: Command (multiple outputs)
	 * - List size == 1: Expression or IndependentElement (single output)
	 */
	public static class CollectedItem {
		public List<SingleElementInfo> elements; // All output elements (labels, visibility flags, stylesheet names)
		public String commandString;
		public List<String> regularAttributeValues; // Attribute values for regular dependency extraction
		public List<String> jsAttributeValues; // Attribute values for JavaScript dependency extraction
		
		public CollectedItem() {
			elements = new ArrayList<>();
			regularAttributeValues = new ArrayList<>();
			jsAttributeValues = new ArrayList<>();
		}
		
		// Get label for dependency mapping
		public String getLabel() {
			if (!elements.isEmpty())
				return elements.get(0).label;
			return null;
		}
		
		// Get all labels
		public List<String> getAllLabels() {
			List<String> labels = new ArrayList<>();
			for (SingleElementInfo element : elements) {
				if (element.label != null)
					labels.add(element.label);
			}
			return labels;
		}
	}
	
	/**
	 * Main entry point: generates Gpad output from collected items.
	 * Performs dependency detection, topological sorting, and output generation.
	 * 
	 * @param sb string builder to append output to
	 */
	public void generate(StringBuilder sb) {
		// Ensure allLabels contains all labels from collected items
		// (in case some labels were not added during parsing)
		for (CollectedItem item : collectedItems) {
			List<String> labels = item.getAllLabels();
			for (String label : labels) {
				if (label != null)
					allLabels.add(label);
			}
		}
		
		// Build dependency graph and perform topological sort
		Map<Integer, Set<Integer>> dependencies = buildDependencyGraph(collectedItems, allLabels);
		List<CollectedItem> sortedItems = topologicalSort(collectedItems, dependencies);
		
		// Output stylesheet definitions first (in generation order)
		outputStyleSheetDefinitions(collectedItems, sb);
		
		// Output items in sorted order
		outputSortedItems(sortedItems, sb);
	}
	
	/**
	 * Output all stylesheet definitions in generation order.
	 */
	private void outputStyleSheetDefinitions(List<CollectedItem> items, StringBuilder sb) {
		// Output stylesheets in the order they were generated
		// We need to collect all unique stylesheet contents and output them
		Map<String, String> contentToName = new LinkedHashMap<>();
		
		// Collect all stylesheet contents from collected items
		for (CollectedItem item : items) {
			for (SingleElementInfo element : item.elements) {
				if (element.styleSheetName != null) {
					// Find stylesheet content from styleSheetContentMap
					for (Map.Entry<String, String> entry : styleSheetContentMap.entrySet()) {
						if (entry.getValue().equals(element.styleSheetName)) {
							contentToName.putIfAbsent(entry.getKey(), element.styleSheetName);
							break;
						}
					}
				}
			}
		}
		
		// Output each stylesheet definition
		for (Map.Entry<String, String> entry : contentToName.entrySet()) {
			String content = entry.getKey();
			String name = entry.getValue();
			if (inMacroConstruction) sb.append("    ");
			sb.append("@").append(name).append(" = ").append(content).append("\n");
		}
	}
	
	/**
	 * Output sorted items in the correct order (respecting topological sort).
	 */
	private void outputSortedItems(List<CollectedItem> sortedItems, StringBuilder sb) {
		for (CollectedItem item : sortedItems) {
			// Unified output logic for all items (Command/Expression/IndependentElement)
			if (item.elements.isEmpty())
				continue;
			
			// Output all labels
			boolean first = true;
			for (SingleElementInfo element : item.elements) {
				if (element.label == null)
					continue;
				
				if (inMacroConstruction && first) sb.append("    ");
				
				if (!first)
					sb.append(", ");
				first = false;
				
				String outputLabel = element.label.isEmpty() ? "OriginalEmpty1459" : element.label;
				sb.append(outputLabel);
				
				// Visibility flags: "*" = object not visible, "~" = label not visible, "" = both visible
				// null means no show element in XML (from XMLToGpadConverter), use default "*"
				// "" means object is visible (from GgbToGpadConverter), don't add any flag
				if (element.visibilityFlags != null) {
					if (!element.visibilityFlags.isEmpty())
						sb.append(element.visibilityFlags);
					// else: visibilityFlags is "", meaning visible, don't add any flag
				} else {
					// visibilityFlags is null, use default (*)
					sb.append("*");
				}
				
				if (element.styleSheetName != null)
					sb.append(" @").append(element.styleSheetName);
			}
			
			sb.append(" = ").append(item.commandString).append(";\n");
		}
	}
	
	/**
	 * Build dependency graph from collected items.
	 * Dependencies are between items (command/expression/independent element), not labels.
	 * 
	 * @param items list of collected items
	 * @param allLabels set of all known labels
	 * @return map from item index to set of item indices it depends on
	 */
	private Map<Integer, Set<Integer>> buildDependencyGraph(List<CollectedItem> items, Set<String> allLabels) {
		// Create a map from label to item index
		Map<String, Integer> labelToIndex = new HashMap<>();
		for (int i = 0; i < items.size(); i++) {
			CollectedItem item = items.get(i);
			List<String> labels = item.getAllLabels();
			for (String label : labels) {
				if (label != null) {
					// Use first occurrence for dependency mapping
					if (!labelToIndex.containsKey(label))
						labelToIndex.put(label, i);
				}
			}
		}
		
		Map<Integer, Set<Integer>> dependencies = new HashMap<>();
		
		for (int i = 0; i < items.size(); i++) {
			CollectedItem item = items.get(i);
			String itemLabel = item.getLabel();
			if (itemLabel == null)
				continue;
			
			Set<Integer> deps = new java.util.HashSet<>();
			
			// Extract dependencies from regular attribute values (shared by all types)
			for (String value : item.regularAttributeValues) {
				if (value != null && !value.isEmpty()) {
					Set<String> valueDeps = extractLabelReferences(value, allLabels);
					for (String depLabel : valueDeps) {
						Integer depIndex = labelToIndex.get(depLabel);
						if (depIndex != null && depIndex != i) {
							deps.add(depIndex);
						}
					}
				}
			}
			
			// Extract dependencies from JavaScript attribute values (shared by all types)
			for (String value : item.jsAttributeValues) {
				if (value != null && !value.isEmpty()) {
					Set<String> valueDeps = extractLabelReferencesFromJavaScript(value, allLabels);
					for (String depLabel : valueDeps) {
						Integer depIndex = labelToIndex.get(depLabel);
						if (depIndex != null && depIndex != i) {
							deps.add(depIndex);
						}
					}
				}
			}
			
			if (!deps.isEmpty())
				dependencies.put(i, deps);
		}
		
		return dependencies;
	}
	
	/**
	 * Topological sort using Kahn's algorithm.
	 * Dependencies are between items (by index), not labels.
	 * Maintains original order when multiple nodes have no dependencies.
	 * 
	 * @param items list of items to sort
	 * @param dependencies dependency graph (item index -> set of item indices it depends on)
	 * @return sorted list of items
	 */
	private List<CollectedItem> topologicalSort(List<CollectedItem> items, Map<Integer, Set<Integer>> dependencies) {
		// Calculate in-degrees for each item index
		Map<Integer, Integer> inDegree = new HashMap<>();
		for (int i = 0; i < items.size(); i++) {
			inDegree.put(i, 0);
		}
		
		// Build reverse dependency graph and calculate in-degrees
		// dependencies: itemIndex -> set of depIndex (itemIndex depends on depIndex)
		// reverseDeps: depIndex -> set of itemIndex (itemIndex depends on depIndex)
		// When depIndex is processed, all itemIndex that depend on it become ready
		Map<Integer, Set<Integer>> reverseDeps = new HashMap<>();
		for (Map.Entry<Integer, Set<Integer>> entry : dependencies.entrySet()) {
			Integer itemIndex = entry.getKey();
			Set<Integer> deps = entry.getValue();
			
			for (Integer depIndex : deps) {
				if (depIndex >= 0 && depIndex < items.size()) {
					// itemIndex depends on depIndex, so itemIndex has an incoming edge from depIndex
					// In reverse dependency graph: depIndex -> itemIndex means itemIndex depends on depIndex
					// When we process depIndex, we can then process itemIndex
					reverseDeps.computeIfAbsent(depIndex, k -> new java.util.HashSet<>()).add(itemIndex);
					inDegree.put(itemIndex, inDegree.getOrDefault(itemIndex, 0) + 1);
				}
			}
		}
		
		// Kahn's algorithm with order preservation
		List<CollectedItem> result = new ArrayList<>();
		// Use a list to maintain order when multiple nodes have in-degree 0
		List<Integer> readyNodes = new ArrayList<>();
		
		// Add all nodes with in-degree 0 to readyNodes, maintaining original order
		for (int i = 0; i < items.size(); i++) {
			if (inDegree.get(i) == 0) {
				readyNodes.add(i);
			}
		}
		
		// Process readyNodes
		while (!readyNodes.isEmpty()) {
			// Process the first node (maintains original order)
			Integer itemIndex = readyNodes.remove(0);
			CollectedItem item = items.get(itemIndex);
			if (item != null) {
				result.add(item);
			}
			
			// Decrease in-degree of neighbors and add to readyNodes if in-degree becomes 0
			// reverseDeps maps depIndex -> set of itemIndex that depend on depIndex
			// When we process depIndex (itemIndex here), all itemIndex that depend on it become ready
			Set<Integer> neighbors = reverseDeps.get(itemIndex);
			if (neighbors != null) {
				// Collect newly ready nodes
				List<Integer> newlyReady = new ArrayList<>();
				for (Integer neighbor : neighbors) {
					int newInDegree = inDegree.get(neighbor) - 1;
					inDegree.put(neighbor, newInDegree);
					if (newInDegree == 0) {
						newlyReady.add(neighbor);
					}
				}
				// Sort newly ready nodes by original index and append to readyNodes
				newlyReady.sort(Integer::compare);
				readyNodes.addAll(newlyReady);
			}
		}
		
		// Check for cycles (nodes not in result)
		if (result.size() < items.size()) {
			Set<Integer> processed = new java.util.HashSet<>();
			for (CollectedItem item : result) {
				int index = items.indexOf(item);
				if (index >= 0) {
					processed.add(index);
				}
			}
			
			// Add remaining items in original order (cycle fallback)
			Log.warn("Circular dependency detected, using original order for remaining items");
			for (int i = 0; i < items.size(); i++) {
				if (!processed.contains(i)) {
					result.add(items.get(i));
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Extract label references from style content using simple character matching.
	 * Based on GLABEL definition: <LETTER> ( <INDEX> | <LETTER> | <DIGIT> | "'" | "'" | "'" )* ("." <GINTEGER>)?
	 * 
	 * @param content the style content string to search
	 * @param allLabels set of all known labels
	 * @return set of label references found in the content
	 */
	private Set<String> extractLabelReferences(String content, Set<String> allLabels) {
		Set<String> references = new java.util.HashSet<>();
		if (content == null || content.isEmpty() || allLabels == null || allLabels.isEmpty())
			return references;
		
		// Iterate through content to find potential label matches
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			
			// Check if this could be the start of a label (must be a letter)
			if (isVariableNameLetter(c)) {
				// Try to match a label starting at this position
				int endPos = matchLabel(content, i, allLabels);
				if (endPos > i) {
					// matchLabel already checks if the label is in allLabels and validates boundaries
					// If it returns endPos > i, it means a valid label was found
					String potentialLabel = content.substring(i, endPos);
					references.add(potentialLabel);
					// Continue searching from endPos (don't skip, as labels can overlap in edge cases)
					// But we'll skip to avoid finding the same label multiple times
					i = endPos - 1; // -1 because loop will increment
				}
			}
		}

		return references;
	}
	
	/**
	 * Check if a character is a valid variable name letter.
	 * Uses the same logic as StyleMapToGpadConverter.isVariableNameLetter().
	 */
	private boolean isVariableNameLetter(char c) {
		// "$" for absolute references in the spreadsheet
		if (c == '$')
			return true;
		
		// Upper case (A-Z)
		if (c >= '\u0041' && c <= '\u005a')
			return true;
		
		// Lower case (a-z)
		if (c >= '\u0061' && c <= '\u007a')
			return true;
		
		// For CAS labels
		if (c >= '\u00a5' && c <= '\u00aa')
			return true;
		
		// Accentuated letters
		if (c >= '\u00c0' && c <= '\u00d6')
			return true;
		if (c >= '\u00d8' && c <= '\u00f6')
			return true;
		if (c >= '\u00f8' && c <= '\u01bf')
			return true;
		if (c >= '\u01c4' && c <= '\u02a8')
			return true;
		
		// Greek
		if (c >= '\u038e' && c <= '\u03f5')
			return true;
		
		// Cyrillic
		if (c >= '\u0401' && c <= '\u0481')
			return true;
		if (c >= '\u0490' && c <= '\u04f9')
			return true;
		
		// A lot of signs (Arabic, accentuated, ...)
		if (c >= '\u0531' && c <= '\u167F')
			return true;
		if (c >= '\u1681' && c <= '\u1ffc')
			return true;
		
		// Asian letters
		if (c >= '\u3041' && c <= '\u3357')
			return true;
		if (c >= '\u4e00' && c <= '\ud7a3')
			return true;
		if (c >= '\uf71d' && c <= '\ufa2d')
			return true;
		
		// Armenian, Hebrew, Arabic
		if (c >= '\ufb13' && c <= '\ufdfb')
			return true;
		if (c >= '\ufe80' && c <= '\ufefc')
			return true;
		
		// Katakana
		if (c >= '\uff66' && c <= '\uff9d')
			return true;
		
		// Hangul
		if (c >= '\uffa1' && c <= '\uffdc')
			return true;
		
		return false;
	}
	
	/**
	 * Try to match a label starting at the given position.
	 * Returns the end position if a valid label is found, otherwise returns startPos.
	 * 
	 * Label format: <LETTER> ( <INDEX> | <LETTER> | <DIGIT> | "'" | "'" | "'" )* ("." <GINTEGER>)?
	 * INDEX format: "_" ( <CHAR> | ("{" (~["}"])+ "}" ) )
	 */
	private int matchLabel(String content, int startPos, Set<String> allLabels) {
		if (startPos >= content.length())
			return startPos;
		
		int pos = startPos;
		
		// First character must be a letter
		if (!isVariableNameLetter(content.charAt(pos)))
			return startPos;
		pos++;
		
		// Match body: ( <INDEX> | <LETTER> | <DIGIT> | "'" | "'" | "'" )* ("." <GINTEGER>)?
		// Continue matching to find the longest possible label sequence
		while (pos < content.length()) {
			char c = content.charAt(pos);
			
			// Check for index: "_" followed by char or "{...}"
			if (c == '_') {
				pos++;
				if (pos >= content.length())
					break;
				
				// Check for "{...}" format
				if (content.charAt(pos) == '{') {
					pos++;
					// Find matching '}'
					int braceDepth = 1;
					while (pos < content.length() && braceDepth > 0) {
						if (content.charAt(pos) == '{')
							braceDepth++;
						else if (content.charAt(pos) == '}')
							braceDepth--;
						pos++;
					}
					if (braceDepth > 0)
						break; // Unmatched brace
				} else {
					// Single character after underscore
					pos++;
				}
				continue;
			}
			
			// Check for letter, digit, or apostrophe
			if (isVariableNameLetter(c) || Character.isDigit(c) || c == '\'' || c == '\u2018' || c == '\u2019') {
				pos++;
				continue;
			}
			
			// Check for optional "." <GINTEGER> suffix
			if (c == '.') {
				int dotPos = pos;
				pos++;
				// Match digits
				boolean hasDigits = false;
				while (pos < content.length() && Character.isDigit(content.charAt(pos))) {
					pos++;
					hasDigits = true;
				}
				if (!hasDigits) {
					// Not a valid .integer suffix, backtrack
					pos = dotPos;
				}
			}
			
			// If we get here, we've reached the end of potential label characters
			break;
		}
		
		// Check if the longest matched string is in allLabels
		// Labels are complete - AB can only match AB, not A or B
		String longestMatch = content.substring(startPos, pos);
		if (allLabels.contains(longestMatch)) {
			// Check boundary after the matched string
			// If we've reached the end or next char is not a label continuation char, it's a valid match
			if (pos >= content.length() || !isValidLabelContinuationChar(content.charAt(pos))) {
				return pos;
			}
			// The matched string is in allLabels but next char is a continuation char
			// This means the matched string is a prefix of a longer potential label
			// But since labels are complete, we should not continue matching
			// Return startPos to indicate no match (the matched string is not a complete label at this position)
		}
		
		// If longest match is not in allLabels, don't try shorter matches
		return startPos;
	}
	
	/**
	 * Check if a character is a valid continuation of a label (letter, digit, apostrophe, underscore).
	 */
	private boolean isValidLabelContinuationChar(char c) {
		return isVariableNameLetter(c) || Character.isDigit(c) || c == '\'' || c == '\u2018' || c == '\u2019' || c == '_';
	}
	
	/**
	 * Extract label references from JavaScript code, only searching within string literals.
	 * JavaScript strings can be enclosed in single quotes ('...'), double quotes ("..."), or backticks (`...`).
	 * 
	 * @param jsCode the JavaScript code to search
	 * @param allLabels set of all known labels
	 * @return set of label references found in string literals
	 */
	private Set<String> extractLabelReferencesFromJavaScript(String jsCode, Set<String> allLabels) {
		Set<String> references = new java.util.HashSet<>();
		if (jsCode == null || jsCode.isEmpty() || allLabels == null || allLabels.isEmpty())
			return references;
		
		// Parse JavaScript code to find string literals
		// Handle single quotes, double quotes, and template literals (backticks)
		for (int i = 0; i < jsCode.length(); i++) {
			char c = jsCode.charAt(i);
			
			// Check for string start: single quote, double quote, or backtick
			if (c == '\'' || c == '"' || c == '`') {
				char quoteChar = c;
				int stringStart = i + 1; // Start of string content (after quote)
				i++; // Skip opening quote
				
				// Find the end of the string
				boolean escaped = false;
				while (i < jsCode.length()) {
					char ch = jsCode.charAt(i);
					
					if (escaped) {
						escaped = false;
						i++;
						continue;
					}
					
					if (ch == '\\') {
						escaped = true;
						i++;
						continue;
					}
					
					if (ch == quoteChar) {
						// Found closing quote
						int stringEnd = i;
						// Extract string content and search for labels
						String stringContent = jsCode.substring(stringStart, stringEnd);
						Set<String> deps = extractLabelReferences(stringContent, allLabels);
						references.addAll(deps);
						break; // Continue searching for next string
					}
					
					i++;
				}
			}
		}
		
		return references;
	}
	
	/**
	 * Add a collected item to the list.
	 * 
	 * @param item the collected item to add
	 */
	public void addCollectedItem(CollectedItem item) {
		if (item != null)
			collectedItems.add(item);
	}
	
	/**
	 * Getter for styleSheetContentMap.
	 */
	public Map<String, String> getStyleSheetContentMap() {
		return styleSheetContentMap;
	}
	
	/**
	 * Generate stylesheet immediately and return stylesheet name.
	 * Stores content in styleSheetContentMap for later output.
	 * 
	 * @param label label for naming the stylesheet
	 * @param type element type
	 * @param styleMap style map to convert
	 * @return stylesheet name, or null if no stylesheet needed
	 */
	public String generateStyleSheet(String label, String type, Map<String, LinkedHashMap<String, String>> styleMap) {
		if (styleMap == null || styleMap.isEmpty())
			return null;
		
		filterStyleMap(styleMap, type);

		// Convert style map to gpad format
		String content = StyleMapToGpadConverter.convertToContentOnly(styleMap, type);
		
		if (content == null || content.isEmpty())
			return null;

		// Check if we should merge with existing stylesheet
		String styleSheetName = null;
		if (mergeStylesheets)
			styleSheetName = styleSheetContentMap.get(content);
		
		if (styleSheetName == null) {
			// Generate new name
			if (label != null && !label.isEmpty())
				styleSheetName = label + "Style";
			else
				styleSheetName = "style" + (++styleSheetCounter);
			
			// Check if styleSheetName is duplicated
			while (generatedStyleSheets.contains(styleSheetName))
				styleSheetName = "style" + (++styleSheetCounter);
			
			// Store mapping for merging (content -> name)
			styleSheetContentMap.put(content, styleSheetName);
			generatedStyleSheets.add(styleSheetName);
		}
		
		return styleSheetName;
	}
	
	/**
	 * Extract attribute values from style map for dependency extraction.
	 * Separates regular attribute values and JavaScript attribute values.
	 * 
	 * @param styleMap style map to extract from
	 * @param regularValues output list for regular attribute values
	 * @param jsValues output list for JavaScript attribute values
	 */
	public void extractAttributeValuesForDependency(Map<String, LinkedHashMap<String, String>> styleMap, 
	                                                  List<String> regularValues, List<String> jsValues) {
		if (styleMap == null || styleMap.isEmpty())
			return;
		
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : styleMap.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();
			
			// Check if this property might contain expressions by checking TAGS_MAYBE_EXPR
			// Only process properties that are in the TAGS_MAYBE_EXPR map
			if (!TAGS_MAYBE_EXPR.containsKey(tagName))
				continue;
			
			// Check if this is a JavaScript element
			boolean isJavaScriptElement = "javascript".equals(tagName);
			
			// Extract attribute values
			String[] attrNames = TAGS_MAYBE_EXPR.get(tagName);
			if (attrNames != null) {
				for (String attr : attrNames) {
					String value = attrs.get(attr);
					if (value != null && !value.isEmpty()) {
						if (isJavaScriptElement)
							jsValues.add(value);
						else
							regularValues.add(value);
					}
				}
			}
			
			// Special handling for startPoint
			if ("startPoint".equals(tagName)) {
				String serialized = attrs.get("_corners");
				if (serialized != null && !serialized.isEmpty() && GpadSerializer.hasStartPointExp(serialized)) {
					regularValues.add(serialized);
				}
			}
		}
	}
	
	/**
	 * Map for tags that need to check attributes for expressions.
	 * Key is tag name, value is array of attribute names to check.
	 * Single-attribute tags use an array with one element.
	 */
	private static final Map<String, String[]> TAGS_MAYBE_EXPR = Map.ofEntries(
		Map.entry("animation", new String[]{"speed", "step"}),
		Map.entry("condition", new String[]{"showObject"}),
		Map.entry("dynamicCaption", new String[]{"val"}),
		Map.entry("ggbscript", new String[]{"val", "onUpdate", "onDragEnd", "onChange"}),
		Map.entry("incrementY", new String[]{"val"}),
		Map.entry("javascript", new String[]{"val", "onUpdate", "onDragEnd", "onChange"}),
		Map.entry("linkedGeo", new String[]{"exp"}),
		Map.entry("objColor", new String[]{"dynamicr", "dynamicg", "dynamicb", "dynamica"}),
		Map.entry("parentLabel", new String[]{"val"}),
		Map.entry("slider", new String[]{"min", "max"})
	);
	
	/**
	 * Set of style properties that should be removed for objects that are not
	 * shown in the EuclidianView (geometry view).
	 * These styles are only relevant for visual display in the geometry view.
	 */
	private static final Set<String> EUCLIDIAN_DISPLAY_STYLES = Set.of(
		"angleStyle",
		"animation",
		"arcSize",
		"bgColor",
		"labelMode",
		"layer",
		"lineStyle",
		"objColor"
	);
	
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
		// check before removing attributes
		boolean isEuclidianShowable = showAttrs != null;
		if (showAttrs != null) {
			showAttrs.remove("object");
			showAttrs.remove("label");
			// If show style becomes empty after removal, remove it from styleMap
			if (showAttrs.isEmpty())
				styleMap.remove("show");
		}
		
		// Filter EuclidianView display styles for objects not shown in geometry view
		// Different object types use different elements to indicate visibility in geometry view:
		// - numeric/angle: slider element
		// - boolean: checkbox element
		// - list: combo element
		// - others: show element
		if (isEuclidianShowable) {
			if ("numeric".equals(type) || "angle".equals(type))
				isEuclidianShowable = styleMap.containsKey("slider");
			else if ("boolean".equals(type))
				isEuclidianShowable = styleMap.containsKey("checkbox");
			else if ("list".equals(type))
				isEuclidianShowable = styleMap.containsKey("combo");
		}
		
		if (!isEuclidianShowable) {
			// Remove EuclidianView display styles
			for (String styleKey : EUCLIDIAN_DISPLAY_STYLES)
				styleMap.remove(styleKey);
		}
		
		// Remove file,slider,... style(already included in the command)
		styleMap.remove("file");
	}
}

