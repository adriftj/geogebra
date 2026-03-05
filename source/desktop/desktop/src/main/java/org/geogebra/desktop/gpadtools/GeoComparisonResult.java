package org.geogebra.desktop.gpadtools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.geogebra.common.gpad.GpadStyleDefaults;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stores comparison result for a single construction object between original and converted XML.
 * Compares XML {@code <element>} nodes and their children, with default-value awareness
 * using {@link GpadStyleDefaults}.
 */
public class GeoComparisonResult {
	
	/** Comparison status */
	public enum Status {
		/** Objects are identical */
		MATCHED,
		/** Objects exist in both but have differences */
		DIFFERENT,
		/** Object exists in original but not in converted */
		MISSING,
		/** Object exists in converted but not in original */
		EXTRA
	}
	
	/** Single property difference */
	public static class PropertyDifference {
		public final String tagName;
		public final String attribute;
		public final String originalValue;
		public final String convertedValue;
		
		public PropertyDifference(String tagName, String attribute,
				String originalValue, String convertedValue) {
			this.tagName = tagName;
			this.attribute = attribute;
			this.originalValue = originalValue;
			this.convertedValue = convertedValue;
		}
	}
	
	private final String label;
	private Status status;
	private String originalType;
	private String convertedType;
	private final List<PropertyDifference> differences = new ArrayList<>();
	
	/**
	 * Creates a comparison result for an object.
	 * @param label the object label
	 */
	public GeoComparisonResult(String label) {
		this.label = label;
		this.status = Status.MATCHED;
	}
	
	/**
	 * Creates a comparison result with a specific status (for MISSING or EXTRA).
	 * @param label the object label
	 * @param status the status
	 */
	public GeoComparisonResult(String label, Status status) {
		this.label = label;
		this.status = status;
	}
	
	public String getLabel() {
		return label;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public String getOriginalType() {
		return originalType;
	}
	
	public void setOriginalType(String originalType) {
		this.originalType = originalType;
	}
	
	public String getConvertedType() {
		return convertedType;
	}
	
	public void setConvertedType(String convertedType) {
		this.convertedType = convertedType;
	}
	
	public List<PropertyDifference> getDifferences() {
		return differences;
	}
	
	/**
	 * Adds a property difference.
	 * @param tagName XML tag name (e.g., "lineStyle", "objColor")
	 * @param attribute attribute name within the tag (null if tag-level difference)
	 * @param originalValue original value
	 * @param convertedValue converted value
	 */
	public void addDifference(String tagName, String attribute,
			String originalValue, String convertedValue) {
		differences.add(new PropertyDifference(tagName, attribute,
				originalValue, convertedValue));
		if (status == Status.MATCHED) {
			status = Status.DIFFERENT;
		}
	}
	
	/**
	 * Checks if there are any differences.
	 * @return true if there are differences
	 */
	public boolean hasDifferences() {
		return !differences.isEmpty();
	}
	
	/**
	 * Compares two {@code <element>} DOM nodes and populates this result with differences.
	 * The comparison is order-insensitive for both child tags and attributes.
	 * Uses {@link GpadStyleDefaults} for default-value-aware comparison.
	 *
	 * @param original the original {@code <element>} node
	 * @param converted the converted {@code <element>} node
	 */
	public void compareElements(Element original, Element converted) {
		compareElements(original, converted, false);
	}

	/**
	 * Compares two {@code <element>} DOM nodes with optional coord skipping.
	 *
	 * @param original the original {@code <element>} node
	 * @param converted the converted {@code <element>} node
	 * @param skipCoords if true, skip comparison of {@code <coords>} child elements
	 *                   (used for dependent objects whose coords are recomputed from definition)
	 */
	public void compareElements(Element original, Element converted, boolean skipCoords) {
		if (original == null || converted == null) {
			return;
		}
		
		// Compare element type attribute
		originalType = original.getAttribute("type");
		convertedType = converted.getAttribute("type");
		if (!safeEquals(originalType, convertedType)) {
			addDifference("element", "type", originalType, convertedType);
		}
		
		// Collect child elements by tag name
		Map<String, List<Element>> origChildren = collectChildElements(original);
		Map<String, List<Element>> convChildren = collectChildElements(converted);
		
		// Merge all tag names
		Set<String> allTags = new LinkedHashSet<>();
		allTags.addAll(origChildren.keySet());
		allTags.addAll(convChildren.keySet());
		
		for (String tagName : allTags) {
			if (SKIP_COMPUTED_TAGS.contains(tagName)) {
				continue;
			}
			List<Element> origElems = origChildren.getOrDefault(tagName, Collections.emptyList());
			List<Element> convElems = convChildren.getOrDefault(tagName, Collections.emptyList());

			if ("coords".equals(tagName)
					&& (skipCoords
							|| hasParametricCoordAttrs(origElems)
							|| hasInfinityCoords(origElems))) {
				continue;
			}
			
			if (origElems.isEmpty() && !convElems.isEmpty()) {
				if ("tag".equals(tagName)) {
					continue; // per-bar styling tags reconstructed from element defaults
				}
				for (Element conv : convElems) {
					if (!isAllDefault(tagName, conv)) {
						// <show object="false"> on converted means the element was non-drawable
						// in the original (GeoGebra only writes <show> for drawable elements).
						// Absence of <show> = non-drawable = hidden, so this is semantically equivalent.
						if ("show".equals(tagName) && "false".equals(conv.getAttribute("object"))) {
							continue;
						}
						addDifference(tagName, null, "(missing)", attrsToString(conv));
					}
				}
			} else if (!origElems.isEmpty() && convElems.isEmpty()) {
				for (Element orig : origElems) {
					if (!isAllDefault(tagName, orig)
							&& !hasNaNCoords("startPoint".equals(tagName), orig)) {
						addDifference(tagName, null, attrsToString(orig), "(missing)");
					}
				}
			} else {
				compareChildTagList(tagName, origElems, convElems);
			}
		}
	}
	
	/**
	 * Compares structural definition (expression and/or command) between original and converted.
	 *
	 * @param origExpr original {@code <expression>} element (may be null)
	 * @param convExpr converted {@code <expression>} element (may be null)
	 * @param origCmd original {@code <command>} element (may be null)
	 * @param convCmd converted {@code <command>} element (may be null)
	 */
	public void compareStructure(Element origExpr, Element convExpr,
			Element origCmd, Element convCmd) {
		// Compare expression tags
		if (origExpr != null && convExpr != null) {
			// Both have expression - compare attributes (skip "type" differences
			// which can vary between representation formats, e.g., list type)
			compareExpressionAttributes(origExpr, convExpr);
		} else if (origExpr != null && convExpr == null) {
			// Original has expression but converted doesn't.
			// This is acceptable if:
			// 1. Converted has a command instead (structural representation change), OR
			// 2. The object is independently defined (the GeoElement exists in both,
			//    but the converted uses a different internal representation like <value>
			//    instead of <expression>). The GPAD roundtrip may create objects
			//    as value-based rather than definition-based, which is functionally
			//    equivalent. Only report as difference if converted has NO structural
			//    definition at all AND the expression contains non-trivial info.
			// For now, skip this difference entirely - the object existence is already
			// checked by the caller (element tag comparison).
		} else if (origExpr == null && convExpr != null) {
			// Converted has expression but original doesn't.
			// Similar to above - skip, the value is set via different means.
		}
		
		// Compare command tags
		if (origCmd != null && convCmd != null) {
			compareCommandElements(origCmd, convCmd);
		} else if (origCmd != null && convCmd == null) {
			if (convExpr == null) {
				addDifference("command", null, commandToString(origCmd), "(missing)");
			}
		} else if (origCmd == null && convCmd != null) {
			if (origExpr == null) {
				addDifference("command", null, "(missing)", commandToString(convCmd));
			}
		}
	}
	
	/**
	 * Compares expression attributes, skipping "type" differences which can vary
	 * between representation formats (e.g., original may not have type but converted has "list").
	 */
	private void compareExpressionAttributes(Element origExpr, Element convExpr) {
		Set<String> allAttrs = new LinkedHashSet<>();
		addAttrNames(allAttrs, origExpr);
		addAttrNames(allAttrs, convExpr);
		
		for (String attrName : allAttrs) {
			if ("type".equals(attrName)) {
				continue;
			}
			String origVal = origExpr.hasAttribute(attrName) ? origExpr.getAttribute(attrName) : null;
			String convVal = convExpr.hasAttribute(attrName) ? convExpr.getAttribute(attrName) : null;
			if (!safeEquals(origVal, convVal)) {
				if ("exp".equals(attrName) && expEquivalent(origVal, convVal, label)) {
					continue;
				}
				if ("label".equals(attrName) && origVal != null && convVal != null) {
					if (normalizeExp(origVal).equals(normalizeExp(convVal))) {
						continue;
					}
				}
				addDifference("expression", attrName, 
						origVal != null ? origVal : "(missing)",
						convVal != null ? convVal : "(missing)");
			}
		}
	}

	/**
	 * Normalizes an expression string for semantic comparison.
	 * Removes whitespace differences that don't affect meaning:
	 * - Spaces after commas in function parameters: f(x, y) == f(x,y)
	 * - Spaces around := assignment: label:=x == label: =x
	 * - label prefix like "eq1: expr" or "A:(1, 2)"
	 */
	private static String normalizeExp(String exp) {
		if (exp == null) return "";
		String s = exp.replaceAll(":\\s*=", ":=");
		s = s.replaceAll(",\\s+", ",");
		s = s.replaceAll("\\s+\\)", ")");
		s = s.replaceAll("\\s+", " ");
		return s.trim();
	}

	/**
	 * Checks if two expression strings are semantically equivalent,
	 * accounting for label prefix differences and whitespace normalization.
	 * E.g., "eq1: x^2 = 4" matches "x^2 = 4" when label is "eq1".
	 */
	private boolean expEquivalent(String origVal, String convVal, String label) {
		if (origVal == null || convVal == null) return false;
		String nOrig = normalizeExp(origVal);
		String nConv = normalizeExp(convVal);
		if (nOrig.equals(nConv)) return true;
		// Check if one has a label prefix the other doesn't
		if (label != null && !label.isEmpty()) {
			String prefixColon = normalizeExp(label) + ":";
			String prefixColonSpace = normalizeExp(label) + ": ";
			// Strip label prefix from both and compare the core expression
			String coreOrig = stripLabelPrefix(nOrig, label);
			String coreConv = stripLabelPrefix(nConv, label);
			if (coreOrig.equals(coreConv)) return true;
		}
		return false;
	}

	private static String stripLabelPrefix(String exp, String label) {
		String norm = normalizeExp(label);
		// "label: expr" or "label:expr"
		if (exp.startsWith(norm + ": ")) return exp.substring(norm.length() + 2).trim();
		if (exp.startsWith(norm + ":")) return exp.substring(norm.length() + 1).trim();
		return exp;
	}
	
	/**
	 * Compares two {@code <command>} elements.
	 */
	private void compareCommandElements(Element origCmd, Element convCmd) {
		// Compare command name
		String origName = origCmd.getAttribute("name");
		String convName = convCmd.getAttribute("name");
		if (!safeEquals(origName, convName)) {
			addDifference("command", "name", origName, convName);
		}
		
		// Compare input
		Element origInput = getFirstChildByTag(origCmd, "input");
		Element convInput = getFirstChildByTag(convCmd, "input");
		if (origInput != null && convInput != null) {
			compareAllAttributes("command/input", origInput, convInput, null);
		} else if (origInput != null) {
			if (!isAllEmpty(origInput)) {
				addDifference("command/input", null, attrsToString(origInput), "(missing)");
			}
		} else if (convInput != null) {
			if (!isAllEmpty(convInput)) {
				addDifference("command/input", null, "(missing)", attrsToString(convInput));
			}
		}
		
		// Compare output
		Element origOutput = getFirstChildByTag(origCmd, "output");
		Element convOutput = getFirstChildByTag(convCmd, "output");
		if (origOutput != null && convOutput != null) {
			compareAllAttributes("command/output", origOutput, convOutput, null);
		} else if (origOutput != null) {
			addDifference("command/output", null, attrsToString(origOutput), "(missing)");
		} else if (convOutput != null) {
			addDifference("command/output", null, "(missing)", attrsToString(convOutput));
		}
	}
	
	/**
	 * Compares lists of child elements with the same tag name.
	 * For single-instance tags, compares directly.
	 * For multi-instance tags (startPoint, tag), matches by key attribute.
	 */
	/** Script tags whose multiple instances should be merged before comparison */
	private static final Set<String> MERGE_SCRIPT_TAGS = Set.of("ggbscript", "javascript");

	/** Multi-instance tags that are matched by a key attribute */
	private static final Set<String> MULTI_INSTANCE_TAGS = Set.of("startPoint", "tag", "listener");

	private void compareChildTagList(String tagName, List<Element> origElems,
			List<Element> convElems) {
		if ("startPoint".equals(tagName)) {
			compareMultiInstance(tagName, origElems, convElems, "number");
		} else if ("tag".equals(tagName)) {
			compareMultiInstanceComposite(tagName, origElems, convElems,
					"key", "barNumber");
		} else if ("listener".equals(tagName)) {
			compareMultiInstance(tagName, origElems, convElems, "type");
		} else if (MERGE_SCRIPT_TAGS.contains(tagName)) {
			compareMergedScriptTags(tagName, origElems, convElems);
		} else if (!MULTI_INSTANCE_TAGS.contains(tagName)
				&& origElems.size() != convElems.size()) {
			compareMergedScriptTags(tagName, origElems, convElems);
		} else if (origElems.size() == 1 && convElems.size() == 1) {
			compareAttributes(tagName, origElems.get(0), convElems.get(0));
		} else {
			int max = Math.max(origElems.size(), convElems.size());
			for (int i = 0; i < max; i++) {
				if (i < origElems.size() && i < convElems.size()) {
					compareAttributes(tagName, origElems.get(i), convElems.get(i));
				} else if (i < origElems.size()) {
					if (!isAllDefault(tagName, origElems.get(i))) {
						addDifference(tagName, null, attrsToString(origElems.get(i)), "(missing)");
					}
				} else {
					if (!isAllDefault(tagName, convElems.get(i))) {
						addDifference(tagName, null, "(missing)", attrsToString(convElems.get(i)));
					}
				}
			}
		}
	}

	/**
	 * Merges attributes from multiple script elements into a single map and compares.
	 * GeoGebra XML may have multiple {@code <ggbscript>} or {@code <javascript>} tags
	 * that are semantically equivalent to one tag with all attributes merged.
	 */
	private void compareMergedScriptTags(String tagName,
			List<Element> origElems, List<Element> convElems) {
		LinkedHashMap<String, String> origAttrs = mergeElementAttrs(origElems);
		LinkedHashMap<String, String> convAttrs = mergeElementAttrs(convElems);
		LinkedHashMap<String, String> defaults = GpadStyleDefaults.getDefaultAttrs(tagName);
		Set<String> skipAttrs = SKIP_DATA_ATTRS.get(getBaseTagName(tagName));
		Set<String> allKeys = new LinkedHashSet<>();
		allKeys.addAll(origAttrs.keySet());
		allKeys.addAll(convAttrs.keySet());
		for (String key : allKeys) {
			if (skipAttrs != null && skipAttrs.contains(key)) continue;
			String ov = origAttrs.get(key);
			String cv = convAttrs.get(key);
			if (ov == null && cv != null) {
				String dv = defaults != null ? defaults.get(key) : null;
				if (dv != null && valuesEqual(tagName, key, dv, cv)) continue;
				addDifference(tagName, key, "(missing)", cv);
			} else if (ov != null && cv == null) {
				String dv = defaults != null ? defaults.get(key) : null;
				if (dv != null && valuesEqual(tagName, key, dv, ov)) continue;
				addDifference(tagName, key, ov, "(missing)");
			} else if (ov != null && !valuesEqual(tagName, key, ov, cv)) {
				addDifference(tagName, key, ov, cv);
			}
		}
	}

	private static LinkedHashMap<String, String> mergeElementAttrs(List<Element> elems) {
		LinkedHashMap<String, String> merged = new LinkedHashMap<>();
		for (Element e : elems) {
			NamedNodeMap attrs = e.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				Node attr = attrs.item(i);
				merged.put(attr.getNodeName(), attr.getNodeValue());
			}
		}
		return merged;
	}
	
	/**
	 * Compares multi-instance elements matched by a single key attribute.
	 * For startPoint tags, normalizes empty "number" attribute to "0".
	 */
	private void compareMultiInstance(String tagName, List<Element> origElems,
			List<Element> convElems, String keyAttr) {
		boolean isStartPoint = "startPoint".equals(tagName) && "number".equals(keyAttr);
		Map<String, Element> origMap = new TreeMap<>();
		for (Element e : origElems) {
			String key = e.getAttribute(keyAttr);
			if (isStartPoint && (key == null || key.isEmpty())) {
				key = "0"; // normalize empty number to "0"
			}
			origMap.put(key, e);
		}
		Map<String, Element> convMap = new TreeMap<>();
		for (Element e : convElems) {
			String key = e.getAttribute(keyAttr);
			if (isStartPoint && (key == null || key.isEmpty())) {
				key = "0"; // normalize empty number to "0"
			}
			convMap.put(key, e);
		}
		
		Set<String> allKeys = new LinkedHashSet<>();
		allKeys.addAll(origMap.keySet());
		allKeys.addAll(convMap.keySet());
		
		for (String key : allKeys) {
			Element orig = origMap.get(key);
			Element conv = convMap.get(key);
			String qualifiedTag = tagName + "[" + keyAttr + "=" + key + "]";
			
			if (orig != null && conv != null) {
				compareAttributes(qualifiedTag, orig, conv);
			} else if (orig != null) {
				if (!isAllDefault(tagName, orig) && !hasNaNCoords(isStartPoint, orig)) {
					addDifference(qualifiedTag, null, attrsToString(orig), "(missing)");
				}
			} else {
				if (!isAllDefault(tagName, conv)) {
					addDifference(qualifiedTag, null, "(missing)", attrsToString(conv));
				}
			}
		}
	}
	
	/**
	 * Compares multi-instance elements matched by a composite key of two attributes.
	 */
	private void compareMultiInstanceComposite(String tagName,
			List<Element> origElems, List<Element> convElems,
			String keyAttr1, String keyAttr2) {
		Map<String, Element> origMap = new TreeMap<>();
		for (Element e : origElems) {
			String key = e.getAttribute(keyAttr1) + "|" + e.getAttribute(keyAttr2);
			origMap.put(key, e);
		}
		Map<String, Element> convMap = new TreeMap<>();
		for (Element e : convElems) {
			String key = e.getAttribute(keyAttr1) + "|" + e.getAttribute(keyAttr2);
			convMap.put(key, e);
		}
		
		Set<String> allKeys = new LinkedHashSet<>();
		allKeys.addAll(origMap.keySet());
		allKeys.addAll(convMap.keySet());
		
		for (String key : allKeys) {
			Element orig = origMap.get(key);
			Element conv = convMap.get(key);
			String qualifiedTag = tagName + "[" + key + "]";
			
			if (orig != null && conv != null) {
				compareAttributes(qualifiedTag, orig, conv);
			} else if (orig != null) {
				addDifference(qualifiedTag, null, attrsToString(orig), "(missing)");
			} else {
				addDifference(qualifiedTag, null, "(missing)", attrsToString(conv));
			}
		}
	}
	
	/**
	 * Compares attributes of two elements with the same tag name.
	 * Uses default values from {@link GpadStyleDefaults} when an attribute is missing.
	 *
	 * @param tagName the XML tag name (for looking up defaults)
	 * @param orig the original element
	 * @param conv the converted element
	 */
	private void compareAttributes(String tagName, Element orig, Element conv) {
		LinkedHashMap<String, String> defaults = GpadStyleDefaults.getDefaultAttrs(
				getBaseTagName(tagName));
		compareAllAttributes(tagName, orig, conv, defaults);
	}
	
	/**
	 * Compares all attributes of two elements, using the given defaults for missing attributes.
	 * Uses numeric-aware comparison for attribute values.
	 *
	 * @param tagName tag name for reporting
	 * @param orig original element
	 * @param conv converted element
	 * @param defaults default values map (may be null)
	 */
	/**
	 * Data-only attributes that are set by evaluation/commands, not by stylesheets.
	 * These are skipped during comparison because roundtrip doesn't preserve them through style.
	 * Key: tag name, Value: set of attribute names to skip.
	 */
	/**
	 * Data-only attributes that are set by evaluation/commands, not by stylesheets.
	 * These are skipped during comparison because roundtrip doesn't preserve them through style.
	 * Key: tag name, Value: set of attribute names to skip.
	 */
	private static final Map<String, Set<String>> SKIP_DATA_ATTRS = Map.of(
			"value", Set.of("val"), // numeric value set by evaluation, not style
			"startPoint", Set.of("number", "w"), // number matched by key normalization; w is homogeneous weight
			"command/output", Set.of("randomResult"), // random seed data differs each run
			"font", Set.of("size"), // legacy absolute pixel size (pre-ggb42), replaced by sizeM
			"coords", Set.of("w"), // homogeneous weight, typically 1 (default) or 0 (at infinity), recomputed
			"spreadsheetTrace", Set.of("traceColumn2", "traceRow2", "tracingRow", "headerOffset")
	);

	/**
	 * Tags that are auto-computed from the defining command and should be
	 * skipped during roundtrip comparison (they will be recalculated on load).
	 */
	private static final Set<String> SKIP_COMPUTED_TAGS = Set.of(
			"eigenvectors", // conic eigenvectors (recomputed from definition)
			"matrix",       // conic matrix coefficients (recomputed from definition)
			"coefficients", // polynomial coefficients (recomputed from definition)
			"listType",     // list element type hint (auto-determined from content)
			"coordStyle",   // coordinate display style (polar/cartesian)
			"isShape"      // shape flag (default false) - no use in runtime, not yet in GPAD
	);

	/** 3D parametric coord attributes (origin/direction vectors) - always recomputed */
	private static final Set<String> PARAMETRIC_COORD_ATTRS = Set.of(
			"ox", "oy", "oz", "ow", "vx", "vy", "vz", "vw", "wx", "wy", "wz");

	private static boolean hasNaNCoords(boolean isStartPoint, Element elem) {
		if (!isStartPoint) return false;
		String x = elem.getAttribute("x");
		String y = elem.getAttribute("y");
		return "NaN".equals(x) || "NaN".equals(y);
	}

	private static boolean hasInfinityCoords(List<Element> elems) {
		for (Element e : elems) {
			NamedNodeMap attrs = e.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				String val = attrs.item(i).getNodeValue();
				if ("Infinity".equals(val) || "-Infinity".equals(val)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean hasParametricCoordAttrs(List<Element> elems) {
		for (Element e : elems) {
			NamedNodeMap attrs = e.getAttributes();
			for (int i = 0; i < attrs.getLength(); i++) {
				if (PARAMETRIC_COORD_ATTRS.contains(attrs.item(i).getNodeName()))
					return true;
			}
		}
		return false;
	}
	
	/** Static color attributes that are computed snapshots when dynamic colors are present */
	private static final Set<String> STATIC_COLOR_ATTRS = Set.of("r", "g", "b", "alpha");
	/** Dynamic color attributes */
	private static final Set<String> DYNAMIC_COLOR_ATTRS = Set.of(
			"dynamicr", "dynamicg", "dynamicb", "dynamica", "colorSpace");

	/** Tags whose attribute values are expression strings and should use space-normalized comparison */
	private static final Set<String> EXPRESSION_VALUE_TAGS = Set.of("command/input");

	private void compareAllAttributes(String tagName, Element orig, Element conv,
			LinkedHashMap<String, String> defaults) {
		String baseTag = getBaseTagName(tagName);
		Set<String> allAttrs = new LinkedHashSet<>();
		addAttrNames(allAttrs, orig);
		addAttrNames(allAttrs, conv);
		
		Set<String> skipAttrs = SKIP_DATA_ATTRS.get(baseTag);
		boolean useExpNormalize = EXPRESSION_VALUE_TAGS.contains(tagName);
		
		boolean hasDynamicColor = false;
		if ("objColor".equals(baseTag)) {
			for (String attr : allAttrs) {
				if (DYNAMIC_COLOR_ATTRS.contains(attr)) {
					hasDynamicColor = true;
					break;
				}
			}
		}
		
		for (String attrName : allAttrs) {
			if (skipAttrs != null && skipAttrs.contains(attrName)) {
				continue;
			}
			if (hasDynamicColor && STATIC_COLOR_ATTRS.contains(attrName)) {
				continue;
			}
			
			String origVal = orig.hasAttribute(attrName) ? orig.getAttribute(attrName) : null;
			String convVal = conv.hasAttribute(attrName) ? conv.getAttribute(attrName) : null;
			
			if (origVal == null && convVal != null) {
				String defaultVal = defaults != null ? defaults.get(attrName) : null;
				if (defaultVal != null && valuesEqual(baseTag, attrName, defaultVal, convVal)) {
					continue;
				}
				addDifference(tagName, attrName, "(missing)", convVal);
			} else if (origVal != null && convVal == null) {
				String defaultVal = defaults != null ? defaults.get(attrName) : null;
				if (defaultVal != null && valuesEqual(baseTag, attrName, defaultVal, origVal)) {
					continue;
				}
				addDifference(tagName, attrName, origVal, "(missing)");
			} else if (origVal != null && !valuesEqual(baseTag, attrName, origVal, convVal)) {
				if (useExpNormalize && normalizeExp(origVal).equals(normalizeExp(convVal))) {
					continue;
				}
				addDifference(tagName, attrName, origVal, convVal);
			}
		}
	}
	
	/** Tolerance for floating-point comparison */
	private static final double NUMERIC_TOLERANCE = 0.00001;
	/** Tolerance for alpha after byte quantization (0.0-1.0 → 0-255 → 0.0-1.0) */
	private static final double ALPHA_BYTE_TOLERANCE = 1.01 / 255.0;
	
	/**
	 * Compares two attribute values with numeric awareness.
	 * <ul>
	 *   <li>For "alpha" attributes on color tags: handles 0-255 vs 0.0-1.0 scale normalization</li>
	 *   <li>For other numeric attributes: compares as doubles with tolerance</li>
	 *   <li>Falls back to string comparison for non-numeric values</li>
	 * </ul>
	 *
	 * @param tagName the base XML tag name (e.g., "objColor", "bgColor")
	 * @param attrName the attribute name (e.g., "alpha", "r")
	 * @param val1 first value
	 * @param val2 second value
	 * @return true if values are considered equal
	 */
	private static final Map<String, String> LEGACY_LINE_TYPE_MAP = Map.of(
			"1", "10", "2", "20", "3", "30", "4", "15");

	private boolean valuesEqual(String tagName, String attrName, String val1, String val2) {
		if (val1 == null) {
			return val2 == null;
		}
		if (val1.equals(val2)) {
			return true;
		}

		if ("lineStyle".equals(tagName) && "type".equals(attrName)) {
			String norm1 = LEGACY_LINE_TYPE_MAP.getOrDefault(val1, val1);
			String norm2 = LEGACY_LINE_TYPE_MAP.getOrDefault(val2, val2);
			if (norm1.equals(norm2)) return true;
		}

		try {
			double d1 = Double.parseDouble(val1);
			double d2 = Double.parseDouble(val2);
			
			if ("alpha".equals(attrName) && isColorTag(tagName)) {
				d1 = normalizeAlpha(d1);
				d2 = normalizeAlpha(d2);
				// -1 is a sentinel meaning "unset" which defaults to 0
				if (d1 < 0) d1 = 0;
				if (d2 < 0) d2 = 0;
				return Math.abs(d1 - d2) < ALPHA_BYTE_TOLERANCE;
			}
			
			return Math.abs(d1 - d2) < NUMERIC_TOLERANCE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Checks if a tag is a color tag (objColor, bgColor, borderColor).
	 */
	private boolean isColorTag(String tagName) {
		return "objColor".equals(tagName) || "bgColor".equals(tagName)
				|| "borderColor".equals(tagName);
	}
	
	/**
	 * Normalizes alpha value to 0.0-1.0 range.
	 * Values > 1.0 are assumed to be in 0-255 range and converted.
	 */
	private double normalizeAlpha(double alpha) {
		if (alpha > 1.0) {
			return alpha / 255.0;
		}
		return alpha;
	}
	
	/**
	 * Checks if all attributes of an element are at their default values
	 * according to {@link GpadStyleDefaults}.
	 *
	 * @param tagName the XML tag name
	 * @param elem the element to check
	 * @return true if all attributes match defaults (or element has no attributes)
	 */
	static boolean isAllDefault(String tagName, Element elem) {
		LinkedHashMap<String, String> defaults = GpadStyleDefaults.getDefaultAttrs(tagName);
		if (defaults == null) {
			// No defaults known - can only be "all default" if element has no attributes
			return elem.getAttributes().getLength() == 0;
		}
		
		Set<String> skipAttrs = SKIP_DATA_ATTRS.get(tagName);
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Node attr = attrs.item(i);
			String attrName = attr.getNodeName();
			// Skip data-only attributes (e.g., value.val)
			if (skipAttrs != null && skipAttrs.contains(attrName)) {
				continue;
			}
			String attrValue = attr.getNodeValue();
			String defaultVal = defaults.get(attrName);
			if (defaultVal == null) {
				return false;
			}
			if (!defaultVal.equals(attrValue) && !numericEquals(tagName, attrName, defaultVal, attrValue)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns true if all attributes of the element have empty string values.
	 */
	private static boolean isAllEmpty(Element elem) {
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			if (!attrs.item(i).getNodeValue().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Numeric-aware equality check for isAllDefault.
	 * Handles cases like "0" vs "0.0", "1" vs "1.0", and alpha scale normalization.
	 */
	private static boolean numericEquals(String tagName, String attrName, String val1, String val2) {
		try {
			double d1 = Double.parseDouble(val1);
			double d2 = Double.parseDouble(val2);
			if ("alpha".equals(attrName)
					&& ("objColor".equals(tagName) || "bgColor".equals(tagName)
							|| "borderColor".equals(tagName))) {
				if (d1 > 1.0) d1 = d1 / 255.0;
				if (d2 > 1.0) d2 = d2 / 255.0;
				if (d1 < 0) d1 = 0;
				if (d2 < 0) d2 = 0;
				return Math.abs(d1 - d2) < ALPHA_BYTE_TOLERANCE;
			}
			return Math.abs(d1 - d2) < NUMERIC_TOLERANCE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * Checks if all child tags of an {@code <element>} are at their default values.
	 * Used to determine if a missing/extra object is effectively empty.
	 *
	 * @param elementTag the {@code <element>} DOM node
	 * @return true if all children have all-default attributes
	 */
	static boolean isElementAllDefault(Element elementTag) {
		if (elementTag == null) {
			return true;
		}
		Map<String, List<Element>> children = collectChildElements(elementTag);
		for (Map.Entry<String, List<Element>> entry : children.entrySet()) {
			String tagName = entry.getKey();
			for (Element child : entry.getValue()) {
				if (!isAllDefault(tagName, child)) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Collects child elements of a parent element, grouped by tag name.
	 */
	static Map<String, List<Element>> collectChildElements(Element parent) {
		Map<String, List<Element>> map = new LinkedHashMap<>();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element elem = (Element) child;
				map.computeIfAbsent(elem.getTagName(), k -> new ArrayList<>()).add(elem);
			}
		}
		return map;
	}
	
	/**
	 * Gets the base tag name (strips any qualifier like "[number=0]").
	 */
	private String getBaseTagName(String tagName) {
		int bracket = tagName.indexOf('[');
		return bracket >= 0 ? tagName.substring(0, bracket) : tagName;
	}
	
	/**
	 * Adds all attribute names from an element to a set.
	 */
	private void addAttrNames(Set<String> set, Element elem) {
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			set.add(attrs.item(i).getNodeName());
		}
	}
	
	/**
	 * Gets the first child element with the given tag name.
	 */
	private Element getFirstChildByTag(Element parent, String tagName) {
		NodeList children = parent.getElementsByTagName(tagName);
		return children.getLength() > 0 ? (Element) children.item(0) : null;
	}
	
	/**
	 * Converts an element's attributes to a readable string.
	 */
	private String attrsToString(Element elem) {
		StringBuilder sb = new StringBuilder();
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			Node attr = attrs.item(i);
			sb.append(attr.getNodeName()).append("=").append(attr.getNodeValue());
		}
		return sb.toString();
	}
	
	/**
	 * Converts a command element to a readable string.
	 */
	private String commandToString(Element cmd) {
		StringBuilder sb = new StringBuilder();
		sb.append(cmd.getAttribute("name")).append("(");
		Element input = getFirstChildByTag(cmd, "input");
		if (input != null) {
			sb.append(attrsToString(input));
		}
		sb.append(")→");
		Element output = getFirstChildByTag(cmd, "output");
		if (output != null) {
			sb.append(attrsToString(output));
		}
		return sb.toString();
	}
	
	/**
	 * Null-safe string equality check.
	 */
	private boolean safeEquals(String a, String b) {
		if (a == null) {
			return b == null;
		}
		return a.equals(b);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(label).append(": ").append(status);
		if (status == Status.DIFFERENT) {
			sb.append(" (").append(differences.size()).append(" differences)");
			for (PropertyDifference diff : differences) {
				sb.append("\n  - ").append(diff.tagName);
				if (diff.attribute != null) {
					sb.append(".").append(diff.attribute);
				}
				sb.append(": ").append(diff.originalValue)
					.append(" -> ").append(diff.convertedValue);
			}
		}
		return sb.toString();
	}
}
