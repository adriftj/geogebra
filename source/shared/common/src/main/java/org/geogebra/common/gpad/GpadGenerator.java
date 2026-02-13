package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Gpad output from collected items.
 * Items are output in the original XML construction order (no topological sorting).
 * Manages shared state for stylesheet generation.
 */
public class GpadGenerator {
	private final Map<String, String> styleSheetContentMap = new HashMap<>();
	private final Map<String, String> styleSheetNameToContent = new LinkedHashMap<>();
	private final Set<String> generatedStyleSheets = new HashSet<>();
	private final List<CollectedItem> collectedItems = new ArrayList<>();
	private int styleSheetCounter = 0;
	final boolean mergeStylesheets;
	private final boolean inMacroConstruction;
	private String envContent;
	private int emptyLabelCounter = 0;

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
	 * Single element information, used for both Command outputs and
	 * Expression/Independent elements.
	 */
	public static class SingleElementInfo {
		public String label;
		public String elementType;
		public String styleSheetName;
		public boolean showObject = true;
		public boolean showLabel = true;

		public SingleElementInfo() {
		}

		public SingleElementInfo(String label, String elementType, String styleSheetName) {
			this.label = label;
			this.elementType = elementType;
			this.styleSheetName = styleSheetName;
		}
	}

	/**
	 * Collected construction item. Discriminated by {@link ItemType}.
	 */
	public static class CollectedItem {
		public enum ItemType { COMMAND, EXPRESSION, INDEPENDENT }

		public ItemType itemType;
		public List<SingleElementInfo> elements = new ArrayList<>();
		public String rhsText;
		public String shorthandText;
		public String extraCreationData;

		public String getLabel() {
			if (!elements.isEmpty())
				return elements.get(0).label;
			return null;
		}

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
	 * Gets the current @@env statement content.
	 */
	public String getEnvContent() {
		return envContent;
	}

	/**
	 * Sets the @@env statement content.
	 */
	public void setEnvContent(String content) {
		this.envContent = content;
	}

	/**
	 * Appends additional content to the @@env block.
	 * If env content already exists, the new content is inserted
	 * before the closing "}" of the @@env block.
	 */
	public void appendEnvContent(String extraContent) {
		if (extraContent == null || extraContent.isEmpty()) return;
		if (envContent == null || envContent.isEmpty()) {
			envContent = extraContent;
			return;
		}
		int closingBrace = envContent.lastIndexOf('}');
		if (closingBrace >= 0) {
			StringBuilder sb = new StringBuilder(envContent.substring(0, closingBrace));
			sb.append(extraContent);
			sb.append(envContent.substring(closingBrace));
			envContent = sb.toString();
		} else {
			envContent = envContent + extraContent;
		}
	}

	/**
	 * Generates Gpad output from collected items in original order.
	 */
	public void generate(StringBuilder sb) {
		if (envContent != null && !envContent.isEmpty()) {
			if (inMacroConstruction) {
				for (String line : envContent.split("\n")) {
					sb.append("    ").append(line).append("\n");
				}
			} else {
				sb.append(envContent);
			}
		}

		outputStyleSheetDefinitions(collectedItems, sb);
		outputItems(collectedItems, sb);
	}

	private void outputStyleSheetDefinitions(List<CollectedItem> items, StringBuilder sb) {
		Set<String> usedStyleSheets = new LinkedHashSet<>();
		for (CollectedItem item : items) {
			for (SingleElementInfo element : item.elements) {
				if (element.styleSheetName != null) {
					usedStyleSheets.add(element.styleSheetName);
				}
			}
		}

		for (String name : usedStyleSheets) {
			String content = styleSheetNameToContent.get(name);
			if (content != null) {
				if (inMacroConstruction) sb.append("    ");
				sb.append("@").append(name).append(" = ").append(content).append("\n");
			}
		}
	}

	private void outputItems(List<CollectedItem> items, StringBuilder sb) {
		for (CollectedItem item : items) {
			if (item.elements.isEmpty())
				continue;

			if (inMacroConstruction) sb.append("    ");

			boolean first = true;
			for (SingleElementInfo element : item.elements) {
				if (element.label == null)
					continue;
				if (!first)
					sb.append(", ");
				first = false;

				if (element.elementType != null)
					sb.append(element.elementType).append(' ');

				String outputLabel = element.label.isEmpty()
						? "OriginalEmpty_" + (++emptyLabelCounter) : element.label;
				sb.append(outputLabel);
				if (!element.showObject)
					sb.append('*');
				if (!element.showLabel)
					sb.append('~');

				if (element.styleSheetName != null)
					sb.append(" @").append(element.styleSheetName);
			}

			if (item.itemType == null)
				item.itemType = CollectedItem.ItemType.COMMAND;

			switch (item.itemType) {
				case COMMAND:
					sb.append(" = ").append(item.rhsText).append(";\n");
					break;
				case EXPRESSION:
					sb.append(" := ").append(item.rhsText).append(";\n");
					break;
				case INDEPENDENT:
					if (item.shorthandText != null && !item.shorthandText.isEmpty()) {
						sb.append(" = ").append(item.shorthandText);
						if (item.extraCreationData != null && !item.extraCreationData.isEmpty())
							sb.append(' ').append(item.extraCreationData);
						sb.append(";\n");
					} else if (item.extraCreationData != null && !item.extraCreationData.isEmpty()) {
						sb.append(" = ").append(item.extraCreationData).append(";\n");
					} else {
						sb.append(";\n");
					}
					break;
			}
		}
	}

	/**
	 * Add a collected item.
	 * Filters out items whose labels contain characters unsupported by GPAD syntax.
	 */
	public void addCollectedItem(CollectedItem item) {
		if (item != null) {
			String label = item.getLabel();
			if (label != null && isUnsupportedGpadLabel(label))
				return;
			collectedItems.add(item);
		}
	}

	/**
	 * Checks if a label contains characters that cannot be represented in GPAD format.
	 * Includes: brackets, colons, ≟, and spaces before the first parenthesis
	 * (e.g., "Point (dependent)"). Labels like "f(x, y)" where spaces are only
	 * inside parentheses are allowed.
	 */
	public static boolean isUnsupportedGpadLabel(String label) {
		if (label.indexOf('[') >= 0 || label.indexOf(':') >= 0
				|| label.indexOf('\u225f') >= 0)
			return true;
		int parenIdx = label.indexOf('(');
		if (parenIdx > 0) {
			for (int i = 0; i < parenIdx; i++) {
				if (label.charAt(i) == ' ')
					return true;
			}
		} else if (parenIdx < 0 && label.indexOf(' ') >= 0) {
			return true;
		}
		return false;
	}

	public Map<String, String> getStyleSheetContentMap() {
		return styleSheetContentMap;
	}

	/**
	 * Generate stylesheet immediately and return stylesheet name.
	 */
	public String generateStyleSheet(String label, String type,
			Map<String, LinkedHashMap<String, String>> styleMap) {
		if (styleMap == null || styleMap.isEmpty())
			return null;

		filterStyleMap(styleMap);

		String content = StyleMapToGpadConverter.convertToContentOnly(styleMap, type);
		if (content == null || content.isEmpty())
			return null;

		String styleSheetName = null;
		if (mergeStylesheets)
			styleSheetName = styleSheetContentMap.get(content);

		if (styleSheetName == null) {
			if (isValidGpadLabel(label)) {
				styleSheetName = label + "Style";
			} else {
				styleSheetName = "style" + (++styleSheetCounter);
			}

			while (generatedStyleSheets.contains(styleSheetName))
				styleSheetName = "style" + (++styleSheetCounter);

			styleSheetContentMap.put(content, styleSheetName);
			generatedStyleSheets.add(styleSheetName);
		}

		styleSheetNameToContent.put(styleSheetName, content);
		return styleSheetName;
	}

	static boolean isValidGpadLabel(String label) {
		if (label == null || label.isEmpty()) return false;
		char first = label.charAt(0);
		if (!isLetter(first)) return false;
		for (int i = 1; i < label.length(); i++) {
			char c = label.charAt(i);
			if (!isLetter(c) && !isDigit(c) && c != '_' && c != '\'' && c != '\u2018' && c != '\u2019')
				return false;
		}
		return true;
	}

	private static boolean isLetter(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
				|| c == '$' || c == '_'
				|| (c >= '\u00C0' && c <= '\u024F')
				|| (c >= '\u0370' && c <= '\u03FF')  // Greek
				|| (c >= '\u0400' && c <= '\u04FF')  // Cyrillic
				|| (c >= '\u3041' && c <= '\u9FFF');  // CJK
	}

	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	private static void filterStyleMap(Map<String, LinkedHashMap<String, String>> styleMap) {
		if (styleMap == null) return;
		LinkedHashMap<String, String> showAttrs = styleMap.get("show");
		if (showAttrs != null && showAttrs.isEmpty())
			styleMap.remove("show");
	}
}
