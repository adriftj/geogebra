package org.geogebra.desktop.gpadtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geogebra.common.gpad.GpadGenerator;
import org.geogebra.common.gpad.GpadStyleDefaults;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Generates roundtrip comparison reports by comparing two XML strings.
 * Compares the {@code <construction>} sections of the original and converted GeoGebra XML,
 * matching objects by label and comparing element children with default-value awareness.
 */
public class RoundtripReport {
	
	private final String inputFile;
	private final String timestamp;
	private final List<GeoComparisonResult> results = new ArrayList<>();
	private int totalOriginal;
	private int totalConverted;
	
	// Summary statistics for main construction
	private int matched;
	private int different;
	private int missing;
	private int extra;

	// Summary statistics for global settings
	private int settingsMatched;
	private int settingsDifferent;
	private final List<GeoComparisonResult.PropertyDifference> settingsDiffs = new ArrayList<>();

	// Macro comparison results
	private final List<MacroComparisonResult> macroResults = new ArrayList<>();

	/** Top-level XML elements to compare for global settings roundtrip */
	private static final Set<String> SETTINGS_ELEMENTS = Set.of(
			"kernel", "euclidianView", "euclidianView3D", "gui",
			"algebraView", "spreadsheetView", "keyboard",
			"probabilityCalculator", "scripting", "tableview");

	/** Child elements within gui that we compare (others are transient UI) */
	private static final Set<String> GUI_COMPARE_CHILDREN = Set.of(
			"font", "labelingStyle", "perspectives");

	/** Child elements to skip within specific parents */
	private static final Map<String, Set<String>> SKIP_CHILDREN = Map.of(
			"kernel", Set.of("uses3D", "angleFromInvTrig"),
			"algebraView", Set.of("collapsed"),
			"spreadsheetView", Set.of("selection", "spreadsheetRow"),
			"probabilityCalculator", Set.of("statisticsCollection"),
			"gui", Set.of("window", "consProtocol",
					"consProtNavigationBar", "tooltipSettings", "menuFont",
					"settings", "splitDivider", "toolbar", "toolbarDefinition"),
			"euclidianView", Set.of("viewNumber"),
			"euclidianView3D", Set.of()
	);

	/** Attributes to skip when comparing perspective view elements (desktop-only) */
	private static final Set<String> PERSP_VIEW_SKIP_ATTRS = Set.of("inframe", "window");

	/** Attributes to skip when comparing perspective toolbar elements */
	private static final Set<String> PERSP_TOOLBAR_SKIP_ATTRS = Set.of("help");

	/** Elements to skip when comparing perspective children */
	private static final Set<String> PERSP_SKIP_ELEMENTS = Set.of("dockBar");

	/**
	 * Known default attribute values within settings elements.
	 * When original has these values but converted is missing, the difference
	 * is ignored because GPAD intentionally omits defaults.
	 * Key format: "parentTag/childTag.attrName" or "parentTag.attrName"
	 */
	private static final Map<String, Set<String>> DEFAULT_ATTR_VALUES = new java.util.HashMap<>();
	static {
		// kernel defaults (from GpadStyleDefaults.ENV_*)
		DEFAULT_ATTR_VALUES.put("kernel/usePathAndRegionParameters.val",
				Set.of(String.valueOf(GpadStyleDefaults.ENV_PATH_REGION_PARAMS)));
		DEFAULT_ATTR_VALUES.put("kernel/angleUnit.val",
				Set.of(GpadStyleDefaults.ENV_ANGLE_UNIT));
		DEFAULT_ATTR_VALUES.put("kernel/coordStyle.val",
				Set.of(String.valueOf(GpadStyleDefaults.ENV_COORD_STYLE)));
		DEFAULT_ATTR_VALUES.put("kernel/algebraStyle.val",
				Set.of(String.valueOf(GpadStyleDefaults.ENV_ALGEBRA_STYLE_VAL)));
		DEFAULT_ATTR_VALUES.put("kernel/algebraStyle.spreadsheet",
				Set.of(String.valueOf(GpadStyleDefaults.ENV_ALGEBRA_STYLE_SPREADSHEET)));
		// uses3D and angleFromInvTrig are handled by SKIP_CHILDREN

		// evSettings defaults (shared by ev1/ev2/ev3d, from GpadStyleDefaults.ENV_*)
		for (String ev : new String[]{"euclidianView", "euclidianView3D"}) {
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.rightAngleStyle",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_RIGHT_ANGLE_STYLE)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.checkboxSize",
					Set.of("13", String.valueOf(GpadStyleDefaults.ENV_CHECKBOX_SIZE)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.gridType",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_GRID_TYPE)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.pointCapturing",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_POINT_CAPTURING)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.axes",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXES)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.grid",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_GRID)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.gridIsBold",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_GRID_IS_BOLD)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.pointStyle",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_POINT_STYLE)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.allowToolTips",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_TOOLTIPS)));
			DEFAULT_ATTR_VALUES.put(ev + "/evSettings.allowShowMouseCoords",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_MOUSE_COORDS)));

			// lineStyle defaults
			DEFAULT_ATTR_VALUES.put(ev + "/lineStyle.axes",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_LINE_STYLE_AXES)));
			DEFAULT_ATTR_VALUES.put(ev + "/lineStyle.grid",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_LINE_STYLE_GRID)));

			// labelStyle defaults
			DEFAULT_ATTR_VALUES.put(ev + "/labelStyle.serif",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_LABEL_SERIF)));
			DEFAULT_ATTR_VALUES.put(ev + "/labelStyle.axes",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_LABEL_FONT_STYLE)));

			// axis defaults
			DEFAULT_ATTR_VALUES.put(ev + "/axis.show",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXIS_SHOW)));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.label", Set.of(""));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.unitLabel", Set.of(""));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.showNumbers",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXIS_SHOW_NUMBERS)));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.tickStyle",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXIS_TICK_STYLE)));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.id", Set.of("0", "1", "2"));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.tickExpression", Set.of(""));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.axisCross", Set.of("0", "0.0"));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.tickDistance", Set.of("NaN"));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.positiveAxis",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXIS_POSITIVE)));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.selectionAllowed",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXIS_SELECTION_ALLOWED)));
			DEFAULT_ATTR_VALUES.put(ev + "/axis.drawBorderAxes",
					Set.of(String.valueOf(GpadStyleDefaults.ENV_AXIS_DRAW_BORDER)));
		}

		// euclidianView3D coordSystem defaults
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coordSystem.xZero", Set.of("0", "0.0", "-0.0"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coordSystem.yZero", Set.of("0", "0.0", "-0.0"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coordSystem.zZero", Set.of("0", "0.0", "-0.0"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coordSystem.scale", Set.of("50", "50.0"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coordSystem.xAngle",
				Set.of("0.5235987755982988", "-0.4636476090008061"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coordSystem.zAngle",
				Set.of("-0.4", "0.39269908169872414"));

		// euclidianView3D specific: plate/clipping/projection/light absent = default state
		DEFAULT_ATTR_VALUES.put("euclidianView3D/plate.show", Set.of("false"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/light.val", Set.of("true"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/yAxisVertical.val", Set.of("false"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/coloredAxes.val", Set.of("false"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/clipping.use", Set.of("false"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/clipping.show", Set.of("false"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/clipping.size", Set.of("0", "1", "2", "3"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/projection.type", Set.of("0"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/projection.separation", Set.of("100"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/axesColored.val", Set.of("false"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/grid.distX", Set.of("1", "1.0"));
		DEFAULT_ATTR_VALUES.put("euclidianView3D/grid.distY", Set.of("1", "1.0"));

		// perspective view defaults (size=0 is default when missing)
		DEFAULT_ATTR_VALUES.put("gui/perspective/views/view.size", Set.of("0"));
		DEFAULT_ATTR_VALUES.put("gui/perspective/views/view.stylebar", Set.of("true"));

		// spreadsheetView defaults
		DEFAULT_ATTR_VALUES.put("spreadsheetView/spreadsheetRow.value0",
				Set.of("true", "false"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/prefCellSize.width", Set.of("70"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/size.width", Set.of("0"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/size.height", Set.of("0"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/spreadsheetBrowser.default", Set.of("true"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/prefCellSize.height", Set.of("0", "26"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/dimensions.rows", Set.of("100"));
		DEFAULT_ATTR_VALUES.put("spreadsheetView/dimensions.columns", Set.of("26"));
	}
	
	/**
	 * Holds comparison results for a single macro.
	 */
	public static class MacroComparisonResult {
		public final String cmdName;
		public final GeoComparisonResult.Status status;
		public final List<GeoComparisonResult.PropertyDifference> headerDiffs;
		public final List<GeoComparisonResult> constructionResults;
		
		public MacroComparisonResult(String cmdName, GeoComparisonResult.Status status,
				List<GeoComparisonResult.PropertyDifference> headerDiffs,
				List<GeoComparisonResult> constructionResults) {
			this.cmdName = cmdName;
			this.status = status;
			this.headerDiffs = headerDiffs != null ? headerDiffs : new ArrayList<>();
			this.constructionResults = constructionResults != null
					? constructionResults : new ArrayList<>();
		}
	}
	
	/**
	 * Groups the XML nodes for a single construction object.
	 */
	private static class XmlObjectGroup {
		String label;
		Element elementTag;      // <element type="..." label="...">
		Element expressionTag;   // <expression label="..." exp="..." />
		Element commandTag;      // <command> ... <output a0="label"/> ... </command>
	}
	
	/**
	 * Creates a new roundtrip report.
	 * @param inputFile the input file name
	 */
	public RoundtripReport(String inputFile) {
		this.inputFile = inputFile;
		this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	
	/**
	 * Compares two GeoGebra XML strings and generates comparison results.
	 * Focuses on the {@code <construction>} section.
	 *
	 * @param originalXml the original XML (from .ggb file)
	 * @param convertedXml the converted XML (from gpadToXml)
	 */
	public void compare(String originalXml, String convertedXml) {
		try {
			Document origDoc = parseXml(originalXml);
			Document convDoc = parseXml(convertedXml);

			Element origRoot = origDoc.getDocumentElement();
			Element convRoot = convDoc.getDocumentElement();

			Element origConstruction = getFirstElement(origRoot, "construction");
			Element convConstruction = getFirstElement(convRoot, "construction");

			if (origConstruction != null || convConstruction != null)
				compareConstructions(origConstruction, convConstruction, results);

			compareGlobalSettings(origRoot, convRoot);

		} catch (Exception e) {
			System.err.println("XML comparison error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Compares global settings elements (kernel, euclidianView, gui, algebraView, etc.)
	 * between original and converted XML, skipping transient UI elements.
	 */
	private void compareGlobalSettings(Element origRoot, Element convRoot) {
		for (String settingsTag : SETTINGS_ELEMENTS) {
			Element origElem = getFirstElement(origRoot, settingsTag);
			Element convElem = getFirstElement(convRoot, settingsTag);

			if (origElem == null && convElem == null) continue;
			if (origElem == null) {
				// Converted has settings but original doesn't - generally OK, skip
				continue;
			}
			if (convElem == null) {
				if (hasNonSkippedContent(origElem, settingsTag))
					addSettingsDiff(settingsTag, null,
							"(present)", "(missing)");
				continue;
			}

			if ("gui".equals(settingsTag)) {
				compareGuiElement(origElem, convElem);
			} else {
				Set<String> skipChildren = SKIP_CHILDREN.getOrDefault(settingsTag, Set.of());
				compareSettingsElement(settingsTag, origElem, convElem, skipChildren);
			}
		}

		settingsMatched = settingsDiffs.isEmpty() ? 1 : 0;
		settingsDifferent = settingsDiffs.isEmpty() ? 0 : 1;
	}

	private void compareGuiElement(Element origGui, Element convGui) {
		for (String childTag : GUI_COMPARE_CHILDREN) {
			Element origChild = getFirstElement(origGui, childTag);
			Element convChild = getFirstElement(convGui, childTag);

			if (origChild == null && convChild == null) continue;
			if (origChild == null) continue;
			if (convChild == null) {
				if (isDefaultGuiChild(childTag, origChild)) continue;
				if ("perspectives".equals(childTag)
						&& getFirstElement(origChild, "perspective") == null)
					continue;
				addSettingsDiff("gui/" + childTag, null,
						attrsToStringStatic(origChild), "(missing)");
				continue;
			}
			if ("perspectives".equals(childTag)) {
				comparePerspectives(origChild, convChild);
			} else {
				compareSettingsAttributes("gui/" + childTag, origChild, convChild);
			}
		}
	}

	private void comparePerspectives(Element origPerspectives, Element convPerspectives) {
		Element origPersp = getFirstElement(origPerspectives, "perspective");
		Element convPersp = getFirstElement(convPerspectives, "perspective");
		if (origPersp == null && convPersp == null) return;
		if (origPersp == null || convPersp == null) {
			addSettingsDiff("gui/perspectives/perspective", null,
					origPersp != null ? "(present)" : "(missing)",
					convPersp != null ? "(present)" : "(missing)");
			return;
		}

		comparePerspectiveSection(origPersp, convPersp, "panes", "pane", null, Set.of());
		comparePerspectiveSection(origPersp, convPersp, "views", "view",
				PERSP_VIEW_SKIP_ATTRS, Set.of());
		comparePerspectiveLeaf(origPersp, convPersp, "toolbar", PERSP_TOOLBAR_SKIP_ATTRS);
		comparePerspectiveLeaf(origPersp, convPersp, "input", Set.of());

		// dockBar is skipped (desktop only)
	}

	private void comparePerspectiveSection(Element origPersp, Element convPersp,
			String containerTag, String itemTag,
			Set<String> skipAttrs, Set<String> defaultAttrNames) {
		Element origContainer = getFirstElement(origPersp, containerTag);
		Element convContainer = getFirstElement(convPersp, containerTag);

		List<Element> origItems = origContainer != null
				? getChildElements(origContainer, itemTag)
				: java.util.Collections.emptyList();
		List<Element> convItems = convContainer != null
				? getChildElements(convContainer, itemTag)
				: java.util.Collections.emptyList();

		String prefix = "gui/perspective/" + containerTag + "/" + itemTag;
		int max = Math.max(origItems.size(), convItems.size());
		for (int i = 0; i < max; i++) {
			if (i < origItems.size() && i < convItems.size()) {
				compareSettingsAttributes(prefix, origItems.get(i), convItems.get(i),
						skipAttrs);
			} else if (i < origItems.size()) {
				addSettingsDiff(prefix, null,
						attrsToStringStatic(origItems.get(i)), "(missing)");
			}
		}
	}

	private void comparePerspectiveLeaf(Element origPersp, Element convPersp,
			String tag, Set<String> skipAttrs) {
		Element orig = getFirstElement(origPersp, tag);
		Element conv = getFirstElement(convPersp, tag);
		if (orig == null && conv == null) return;
		if (orig == null) return;
		if (conv == null) {
			addSettingsDiff("gui/perspective/" + tag, null,
					attrsToStringStatic(orig), "(missing)");
			return;
		}
		compareSettingsAttributes("gui/perspective/" + tag, orig, conv, skipAttrs);
	}

	private static List<Element> getChildElements(Element parent, String tagName) {
		List<Element> result = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node n = children.item(i);
			if (n instanceof Element && tagName.equals(n.getNodeName())) {
				result.add((Element) n);
			}
		}
		return result;
	}

	private static boolean isDefaultGuiChild(String tag, Element elem) {
		if ("font".equals(tag)) {
			String size = elem.getAttribute("size");
			return size.isEmpty() || "0".equals(size);
		}
		if ("labelingStyle".equals(tag)) {
			String val = elem.getAttribute("val");
			return val.isEmpty() || "0".equals(val);
		}
		return false;
	}

	private void compareSettingsElement(String parentTag, Element orig, Element conv,
			Set<String> skipChildren) {
		// Compare attributes of the element itself
		compareSettingsAttributes(parentTag, orig, conv);

		// Compare child elements
		Map<String, List<Element>> origChildren = GeoComparisonResult.collectChildElements(orig);
		Map<String, List<Element>> convChildren = GeoComparisonResult.collectChildElements(conv);

		Set<String> allChildTags = new LinkedHashSet<>();
		allChildTags.addAll(origChildren.keySet());
		allChildTags.addAll(convChildren.keySet());

		for (String childTag : allChildTags) {
			if (skipChildren.contains(childTag)) continue;

			List<Element> origList = origChildren.getOrDefault(childTag,
					java.util.Collections.emptyList());
			List<Element> convList = convChildren.getOrDefault(childTag,
					java.util.Collections.emptyList());

			String qualTag = parentTag + "/" + childTag;

			if (origList.isEmpty() && !convList.isEmpty()) {
				continue;
			}
			if (!origList.isEmpty() && convList.isEmpty()) {
				for (Element o : origList) {
					if (!hasNonDefaultAttrs(qualTag, o)) continue;
					addSettingsDiff(qualTag, null, attrsToStringStatic(o), "(missing)");
				}
				continue;
			}

			if ("axis".equals(childTag) || "spreadsheetColumn".equals(childTag)) {
				matchAxisById(qualTag, origList, convList);
			} else {
				int max = Math.max(origList.size(), convList.size());
				for (int i = 0; i < max; i++) {
					if (i < origList.size() && i < convList.size()) {
						compareSettingsAttributes(qualTag, origList.get(i), convList.get(i));
					} else if (i < origList.size()) {
						addSettingsDiff(qualTag, null,
								attrsToStringStatic(origList.get(i)), "(missing)");
					}
				}
			}
		}
	}

	private void matchAxisById(String qualTag, List<Element> origList, List<Element> convList) {
		Map<String, Element> convById = new LinkedHashMap<>();
		for (Element c : convList) convById.put(c.getAttribute("id"), c);
		for (Element o : origList) {
			String id = o.getAttribute("id");
			Element c = convById.get(id);
			if (c != null) {
				compareSettingsAttributes(qualTag, o, c);
			} else {
				if (!hasNonDefaultAttrs(qualTag, o)) continue;
				addSettingsDiff(qualTag, null, attrsToStringStatic(o), "(missing)");
			}
		}
	}

	private static final Set<String> SKIP_ATTR_COMBOS = Set.of(
			"euclidianView/evSettings.checkboxSize",
			"euclidianView/evSettings.pointStyle",
			"euclidianView3D/evSettings.checkboxSize",
			"euclidianView3D/evSettings.pointStyle"
	);

	private void compareSettingsAttributes(String tagName, Element orig, Element conv) {
		compareSettingsAttributes(tagName, orig, conv, null);
	}

	private void compareSettingsAttributes(String tagName, Element orig, Element conv,
			Set<String> skipAttrs) {
		Set<String> allAttrs = new LinkedHashSet<>();
		addAttrNames(allAttrs, orig);
		addAttrNames(allAttrs, conv);

		for (String attr : allAttrs) {
			if (skipAttrs != null && skipAttrs.contains(attr)) continue;
			if (SKIP_ATTR_COMBOS.contains(tagName + "." + attr)) continue;

			String origVal = orig.hasAttribute(attr) ? orig.getAttribute(attr) : null;
			String convVal = conv.hasAttribute(attr) ? conv.getAttribute(attr) : null;

			if (origVal == null && convVal != null) continue;
			if (origVal != null && convVal == null) {
				if (isDefaultAttrValue(tagName, attr, origVal)) continue;
				addSettingsDiff(tagName, attr, origVal, "(missing)");
			} else if (origVal != null && !settingsValuesEqual(origVal, convVal)) {
				addSettingsDiff(tagName, attr, origVal, convVal);
			}
		}
	}

	private static boolean isDefaultAttrValue(String tagName, String attr, String val) {
		String key = tagName + "." + attr;
		Set<String> defaults = DEFAULT_ATTR_VALUES.get(key);
		return defaults != null && defaults.contains(val);
	}

	private static boolean hasNonDefaultAttrs(String qualTag, Element elem) {
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			String name = attrs.item(i).getNodeName();
			String val = attrs.item(i).getNodeValue();
			if (!isDefaultAttrValue(qualTag, name, val))
				return true;
		}
		return false;
	}

	private static boolean settingsValuesEqual(String a, String b) {
		if (a == null) return b == null;
		if (a.equals(b)) return true;
		try {
			double da = Double.parseDouble(a);
			double db = Double.parseDouble(b);
			return Math.abs(da - db) < 0.00001;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean hasNonSkippedContent(Element elem, String parentTag) {
		if (isAllDefaultSettings(elem, parentTag))
			return false;
		Set<String> skip = SKIP_CHILDREN.getOrDefault(parentTag, Set.of());
		NodeList children = elem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = ((Element) child).getTagName();
				if (!skip.contains(tag)) return true;
			}
		}
		return elem.getAttributes().getLength() > 0;
	}

	/**
	 * Checks if a settings element contains only default values that the GPAD
	 * converter intentionally omits. Such elements are equivalent to being absent.
	 */
	private static boolean isAllDefaultSettings(Element elem, String tag) {
		switch (tag) {
			case "scripting":
				return isDefaultScripting(elem);
			case "tableview":
				return isDefaultTableview(elem);
			case "algebraView":
				return isDefaultAlgebraView(elem);
			case "spreadsheetView":
				return isDefaultSpreadsheetView(elem);
			case "probabilityCalculator":
				return isDefaultProbCalc(elem);
			default:
				return false;
		}
	}

	private static boolean isDefaultScripting(Element elem) {
		String blocked = elem.getAttribute("blocked");
		String disabled = elem.getAttribute("disabled");
		return (blocked.isEmpty() || "false".equals(blocked))
				&& (disabled.isEmpty() || "false".equals(disabled));
	}

	private static boolean isDefaultTableview(Element elem) {
		String min = elem.getAttribute("min");
		String max = elem.getAttribute("max");
		String step = elem.getAttribute("step");
		return isNumEqual(min, -2.0) && isNumEqual(max, 2.0) && isNumEqual(step, 1.0);
	}

	private static boolean isDefaultAlgebraView(Element elem) {
		NodeList children = elem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			Element ce = (Element) child;
			String childTag = ce.getTagName();
			if ("collapsed".equals(childTag)) continue;
			if ("mode".equals(childTag)) {
				String val = ce.getAttribute("val");
				if (!"1".equals(val)) return false;
				continue;
			}
			if ("auxiliary".equals(childTag)) {
				if ("true".equals(ce.getAttribute("show"))) return false;
				continue;
			}
			return false;
		}
		return true;
	}

	private static boolean isDefaultSpreadsheetView(Element elem) {
		Set<String> ignoreTags = Set.of("selection", "spreadsheetRow",
				"prefCellSize", "spreadsheetColumn", "layout",
				"spreadsheetBrowser", "size", "dimensions",
				"spreadsheetDimensions", "spreadsheetCellFormat",
				"initialSelection", "tab");
		NodeList children = elem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			String tag = ((Element) child).getTagName();
			if (!ignoreTags.contains(tag)) return false;
		}
		return true;
	}

	private static boolean isDefaultProbCalc(Element elem) {
		NodeList children = elem.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			Element ce = (Element) child;
			if ("statisticsCollection".equals(ce.getTagName())) {
				if ("true".equals(ce.getAttribute("active"))) return false;
				continue;
			}
			return false;
		}
		return true;
	}

	private static boolean isNumEqual(String s, double target) {
		if (s == null || s.isEmpty()) return true;
		try {
			return Math.abs(Double.parseDouble(s) - target) < 0.0001;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void addSettingsDiff(String tagName, String attr,
			String origVal, String convVal) {
		settingsDiffs.add(new GeoComparisonResult.PropertyDifference(
				tagName, attr, origVal, convVal));
	}

	private static String attrsToStringStatic(Element elem) {
		if (elem == null) return "(null)";
		StringBuilder sb = new StringBuilder();
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			if (i > 0) sb.append(", ");
			Node attr = attrs.item(i);
			sb.append(attr.getNodeName()).append("=").append(attr.getNodeValue());
		}
		return sb.toString();
	}
	
	/**
	 * Compares two construction elements and populates the given results list.
	 */
	private void compareConstructions(Element origConstruction, Element convConstruction,
			List<GeoComparisonResult> resultsList) {
		Map<String, XmlObjectGroup> origGroups = origConstruction != null
				? groupByLabel(origConstruction) : new TreeMap<>();
		Map<String, XmlObjectGroup> convGroups = convConstruction != null
				? groupByLabel(convConstruction) : new TreeMap<>();
		
		totalOriginal = origGroups.size();
		totalConverted = convGroups.size();
		
		// Merge all labels
		Set<String> allLabels = new LinkedHashSet<>();
		allLabels.addAll(origGroups.keySet());
		allLabels.addAll(convGroups.keySet());
		
		for (String label : allLabels) {
			XmlObjectGroup origGroup = origGroups.get(label);
			XmlObjectGroup convGroup = convGroups.get(label);
			
			if (origGroup == null) {
				// Extra in converted
				if (convGroup.elementTag != null
						&& !GeoComparisonResult.isElementAllDefault(convGroup.elementTag)) {
					GeoComparisonResult result = new GeoComparisonResult(label,
							GeoComparisonResult.Status.EXTRA);
					if (convGroup.elementTag != null) {
						result.setConvertedType(convGroup.elementTag.getAttribute("type"));
					}
					resultsList.add(result);
					extra++;
				} else {
					matched++; // all-default extra is considered match
				}
			} else if (convGroup == null) {
				// Missing from converted
				if (GpadGenerator.isUnsupportedGpadLabel(label)) {
					matched++; // label can't be represented in GPAD, skip
				} else if (origGroup.elementTag != null
						&& !GeoComparisonResult.isElementAllDefault(origGroup.elementTag)) {
					GeoComparisonResult result = new GeoComparisonResult(label,
							GeoComparisonResult.Status.MISSING);
					if (origGroup.elementTag != null) {
						result.setOriginalType(origGroup.elementTag.getAttribute("type"));
					}
					resultsList.add(result);
					missing++;
				} else {
					matched++; // all-default missing is considered match
				}
			} else {
				// Both present - compare
				GeoComparisonResult result = new GeoComparisonResult(label);
				
				// Compare structural definition
				result.compareStructure(
						origGroup.expressionTag, convGroup.expressionTag,
						origGroup.commandTag, convGroup.commandTag);
				
				// Compare element children (style properties)
				// Skip coords comparison for dependent objects whose coords are
				// recomputed deterministically from their definition (GPAD omits them).
				boolean skipCoords = shouldSkipCoords(origGroup);
				if (origGroup.elementTag != null && convGroup.elementTag != null) {
					result.compareElements(origGroup.elementTag, convGroup.elementTag, skipCoords);
				} else if (origGroup.elementTag != null) {
					if (!GeoComparisonResult.isElementAllDefault(origGroup.elementTag)) {
						result.addDifference("element", null,
								"type=" + origGroup.elementTag.getAttribute("type"),
								"(missing)");
					}
				} else if (convGroup.elementTag != null) {
					if (!GeoComparisonResult.isElementAllDefault(convGroup.elementTag)) {
						result.addDifference("element", null,
								"(missing)",
								"type=" + convGroup.elementTag.getAttribute("type"));
					}
				}
				
				if (result.hasDifferences()) {
					different++;
				} else {
					matched++;
				}
				resultsList.add(result);
			}
		}
	}
	
	/**
	 * Commands that produce free objects on a path/region.
	 * Coords must NOT be skipped for these because the position is user-defined.
	 */
	private static final Set<String> PATH_CONSTRAINED_COMMANDS =
			Set.of("Point", "PointIn");

	/**
	 * Determines whether coords comparison should be skipped for the given object group.
	 * Coords are omitted from GPAD for dependent objects whose position is fully determined
	 * by their definition (commands other than Point/PointIn, or expression-defined objects).
	 */
	private static boolean shouldSkipCoords(XmlObjectGroup group) {
		if (group.commandTag != null) {
			String cmdName = group.commandTag.getAttribute("name");
			return !PATH_CONSTRAINED_COMMANDS.contains(cmdName);
		}
		if (group.expressionTag != null) {
			return true;
		}
		return false;
	}

	/**
	 * Compares macro XML strings.
	 *
	 * @param originalMacroXml the original macro XML
	 * @param convertedMacroXml the converted macro XML
	 */
	public void compareMacros(String originalMacroXml, String convertedMacroXml) {
		try {
			Map<String, Element> origMacros = parseMacros(originalMacroXml);
			Map<String, Element> convMacros = parseMacros(convertedMacroXml);
			
			Set<String> allCmdNames = new LinkedHashSet<>();
			allCmdNames.addAll(origMacros.keySet());
			allCmdNames.addAll(convMacros.keySet());
			
			for (String cmdName : allCmdNames) {
				Element origMacro = origMacros.get(cmdName);
				Element convMacro = convMacros.get(cmdName);
				
				if (origMacro == null) {
					macroResults.add(new MacroComparisonResult(cmdName,
							GeoComparisonResult.Status.EXTRA, null, null));
				} else if (convMacro == null) {
					macroResults.add(new MacroComparisonResult(cmdName,
							GeoComparisonResult.Status.MISSING, null, null));
				} else {
					compareSingleMacro(cmdName, origMacro, convMacro);
				}
			}
		} catch (Exception e) {
			System.err.println("Macro XML comparison error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Compares a single macro's header and construction.
	 */
	private void compareSingleMacro(String cmdName, Element origMacro, Element convMacro) {
		List<GeoComparisonResult.PropertyDifference> headerDiffs = new ArrayList<>();
		
		// Compare macro header attributes
		// Note: toolName, toolHelp, viewId are not preserved by GPAD macro format (known limitation)
		String[] headerAttrs = {"cmdName", "iconFile",
				"showInToolBar", "copyCaptions"};
		Map<String, String> macroAttrDefaults = Map.of(
				"showInToolBar", "true", "copyCaptions", "true");
		for (String attr : headerAttrs) {
			String origVal = origMacro.hasAttribute(attr) ? origMacro.getAttribute(attr) : null;
			String convVal = convMacro.hasAttribute(attr) ? convMacro.getAttribute(attr) : null;
			if (!safeEquals(origVal, convVal)) {
				String defVal = macroAttrDefaults.get(attr);
				if (defVal != null) {
					String ov = origVal != null ? origVal : defVal;
					String cv = convVal != null ? convVal : defVal;
					if (safeEquals(ov, cv)) continue;
				}
				headerDiffs.add(new GeoComparisonResult.PropertyDifference(
						"macro", attr,
						origVal != null ? origVal : "(missing)",
						convVal != null ? convVal : "(missing)"));
			}
		}
		
		// Compare macroInput
		Element origInput = getFirstElement(origMacro, "macroInput");
		Element convInput = getFirstElement(convMacro, "macroInput");
		compareElementAttributes(headerDiffs, "macroInput", origInput, convInput);
		
		// Compare macroOutput
		Element origOutput = getFirstElement(origMacro, "macroOutput");
		Element convOutput = getFirstElement(convMacro, "macroOutput");
		compareElementAttributes(headerDiffs, "macroOutput", origOutput, convOutput);
		
		// Compare macro construction
		Element origCons = getFirstElement(origMacro, "construction");
		Element convCons = getFirstElement(convMacro, "construction");
		
		List<GeoComparisonResult> consResults = new ArrayList<>();
		if (origCons != null || convCons != null) {
			// Use a temporary report to reuse comparison logic
			// but we need to track stats separately
			Map<String, XmlObjectGroup> origGroups = origCons != null
					? groupByLabel(origCons) : new TreeMap<>();
			Map<String, XmlObjectGroup> convGroups = convCons != null
					? groupByLabel(convCons) : new TreeMap<>();
			
			Set<String> allLabels = new LinkedHashSet<>();
			allLabels.addAll(origGroups.keySet());
			allLabels.addAll(convGroups.keySet());
			
			for (String label : allLabels) {
				XmlObjectGroup origGroup = origGroups.get(label);
				XmlObjectGroup convGroup = convGroups.get(label);
				
				if (origGroup == null) {
					if (convGroup.elementTag != null
							&& !GeoComparisonResult.isElementAllDefault(convGroup.elementTag)) {
						GeoComparisonResult r = new GeoComparisonResult(label,
								GeoComparisonResult.Status.EXTRA);
						if (convGroup.elementTag != null) {
							r.setConvertedType(convGroup.elementTag.getAttribute("type"));
						}
						consResults.add(r);
					}
				} else if (convGroup == null) {
					if (origGroup.elementTag != null
							&& !GeoComparisonResult.isElementAllDefault(origGroup.elementTag)) {
						GeoComparisonResult r = new GeoComparisonResult(label,
								GeoComparisonResult.Status.MISSING);
						if (origGroup.elementTag != null) {
							r.setOriginalType(origGroup.elementTag.getAttribute("type"));
						}
						consResults.add(r);
					}
				} else {
					GeoComparisonResult r = new GeoComparisonResult(label);
					r.compareStructure(
							origGroup.expressionTag, convGroup.expressionTag,
							origGroup.commandTag, convGroup.commandTag);
					boolean macroSkipCoords = shouldSkipCoords(origGroup);
					if (origGroup.elementTag != null && convGroup.elementTag != null) {
						r.compareElements(origGroup.elementTag, convGroup.elementTag, macroSkipCoords);
					}
					if (r.hasDifferences()) {
						consResults.add(r);
					}
				}
			}
		}
		
		GeoComparisonResult.Status macroStatus;
		if (headerDiffs.isEmpty() && consResults.isEmpty()) {
			macroStatus = GeoComparisonResult.Status.MATCHED;
		} else {
			macroStatus = GeoComparisonResult.Status.DIFFERENT;
		}
		
		macroResults.add(new MacroComparisonResult(cmdName, macroStatus,
				headerDiffs, consResults));
	}
	
	/**
	 * Compares attributes of two elements and adds differences to the list.
	 */
	private void compareElementAttributes(
			List<GeoComparisonResult.PropertyDifference> diffs,
			String tagName, Element orig, Element conv) {
		if (orig == null && conv == null) {
			return;
		}
		if (orig == null) {
			diffs.add(new GeoComparisonResult.PropertyDifference(
					tagName, null, "(missing)", attrsToString(conv)));
			return;
		}
		if (conv == null) {
			diffs.add(new GeoComparisonResult.PropertyDifference(
					tagName, null, attrsToString(orig), "(missing)"));
			return;
		}
		
		Set<String> allAttrs = new LinkedHashSet<>();
		addAttrNames(allAttrs, orig);
		addAttrNames(allAttrs, conv);
		
		for (String attr : allAttrs) {
			String origVal = orig.hasAttribute(attr) ? orig.getAttribute(attr) : null;
			String convVal = conv.hasAttribute(attr) ? conv.getAttribute(attr) : null;
			if (!safeEquals(origVal, convVal)) {
				diffs.add(new GeoComparisonResult.PropertyDifference(
						tagName, attr,
						origVal != null ? origVal : "(missing)",
						convVal != null ? convVal : "(missing)"));
			}
		}
	}
	
	/**
	 * Parses macro XML and returns a map of cmdName to macro Element.
	 */
	private Map<String, Element> parseMacros(String macroXml) throws Exception {
		Map<String, Element> map = new LinkedHashMap<>();
		if (macroXml == null || macroXml.trim().isEmpty()) {
			return map;
		}
		
		Document doc = parseXml(macroXml);
		Element root = doc.getDocumentElement();
		NodeList macros = root.getElementsByTagName("macro");
		for (int i = 0; i < macros.getLength(); i++) {
			Element macro = (Element) macros.item(i);
			String cmdName = macro.getAttribute("cmdName");
			if (cmdName != null && !cmdName.isEmpty()) {
				map.put(cmdName, macro);
			}
		}
		return map;
	}
	
	/**
	 * Groups construction children by label.
	 * <ul>
	 *   <li>{@code <element>} is indexed by its {@code label} attribute</li>
	 *   <li>{@code <expression>} is indexed by its {@code label} attribute</li>
	 *   <li>{@code <command>} is indexed by each label in its {@code <output>} child</li>
	 * </ul>
	 */
	private Map<String, XmlObjectGroup> groupByLabel(Element construction) {
		Map<String, XmlObjectGroup> groups = new TreeMap<>();
		
		NodeList children = construction.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element elem = (Element) child;
			String tagName = elem.getTagName();
			
			switch (tagName) {
			case "element":
				String label = elem.getAttribute("label");
				if (label != null && !label.isEmpty()) {
					getOrCreate(groups, label).elementTag = elem;
				}
				break;
				
			case "expression":
				String exprLabel = elem.getAttribute("label");
				if (exprLabel != null && !exprLabel.isEmpty()) {
					getOrCreate(groups, exprLabel).expressionTag = elem;
				}
				break;
				
			case "command":
				// Index by output labels
				Element output = getFirstElement(elem, "output");
				if (output != null) {
					NamedNodeMap attrs = output.getAttributes();
					for (int j = 0; j < attrs.getLength(); j++) {
						String outputLabel = attrs.item(j).getNodeValue();
						if (outputLabel != null && !outputLabel.isEmpty()) {
							getOrCreate(groups, outputLabel).commandTag = elem;
						}
					}
				}
				break;
				
			default:
				// Skip other tags (like construction attributes)
				break;
			}
		}
		
		return groups;
	}
	
	/**
	 * Gets or creates an XmlObjectGroup for the given label.
	 */
	private XmlObjectGroup getOrCreate(Map<String, XmlObjectGroup> map, String label) {
		return map.computeIfAbsent(label, k -> {
			XmlObjectGroup g = new XmlObjectGroup();
			g.label = k;
			return g;
		});
	}
	
	/**
	 * Gets the list of comparison results.
	 * @return comparison results
	 */
	public List<GeoComparisonResult> getResults() {
		return results;
	}
	
	/**
	 * Gets the list of macro comparison results.
	 * @return macro comparison results
	 */
	public List<MacroComparisonResult> getMacroResults() {
		return macroResults;
	}
	
	/**
	 * Checks if the roundtrip was successful (no differences).
	 * @return true if all objects match
	 */
	public boolean isSuccess() {
		if (different != 0 || missing != 0 || extra != 0) {
			return false;
		}
		if (!settingsDiffs.isEmpty()) {
			return false;
		}
		for (MacroComparisonResult mr : macroResults) {
			if (mr.status != GeoComparisonResult.Status.MATCHED) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Prints the report to console.
	 * @param verbose whether to print all differences in detail
	 */
	public void printToConsole(boolean verbose) {
		System.out.println();
		System.out.println("=== Roundtrip Test Report ===");
		System.out.println("File: " + inputFile);
		System.out.println("Time: " + timestamp);
		System.out.println();
		System.out.println("Summary (Construction):");
		System.out.println("  Original elements:  " + totalOriginal);
		System.out.println("  Converted elements: " + totalConverted);
		System.out.println("  Matched:            " + matched);
		System.out.println("  Different:          " + different);
		System.out.println("  Missing:            " + missing);
		System.out.println("  Extra:              " + extra);
		
		// Calculate success rate
		if (totalOriginal > 0) {
			double successRate = (double) matched / totalOriginal * 100;
			System.out.printf("  Success rate:       %.1f%%%n", successRate);
		}
		
		System.out.println();

		// Print settings comparison
		if (!settingsDiffs.isEmpty()) {
			System.out.println("Global Settings Differences (" + settingsDiffs.size() + "):");
			for (GeoComparisonResult.PropertyDifference d : settingsDiffs) {
				System.out.println("  - " + d.tagName
						+ (d.attribute != null ? "." + d.attribute : "")
						+ ": " + d.originalValue + " -> " + d.convertedValue);
			}
			System.out.println();
		} else {
			System.out.println("Global settings: all matched.");
			System.out.println();
		}

		// Print differences
		printDifferences(results, verbose);
		
		// Print macro results
		if (!macroResults.isEmpty()) {
			printMacroResults(verbose);
		}
		
		System.out.println();
	}
	
	/**
	 * Prints differences from a list of comparison results.
	 */
	private void printDifferences(List<GeoComparisonResult> resultsList, boolean verbose) {
		int diffCount = 0;
		int missingCount = 0;
		int extraCount = 0;
		
		for (GeoComparisonResult result : resultsList) {
			switch (result.getStatus()) {
			case DIFFERENT:
				diffCount++;
				break;
			case MISSING:
				missingCount++;
				break;
			case EXTRA:
				extraCount++;
				break;
			default:
				break;
			}
		}
		
		if (diffCount == 0 && missingCount == 0 && extraCount == 0) {
			System.out.println("All elements match! Roundtrip successful.");
			return;
		}
		
		System.out.println("Details:");
		
		// Missing objects
		if (missingCount > 0) {
			System.out.println();
			System.out.println("Missing elements (in original but not in converted):");
			for (GeoComparisonResult result : resultsList) {
				if (result.getStatus() == GeoComparisonResult.Status.MISSING) {
					System.out.println("  - " + result.getLabel()
							+ (result.getOriginalType() != null
									? " (" + result.getOriginalType() + ")" : ""));
				}
			}
		}
		
		// Extra objects
		if (extraCount > 0) {
			System.out.println();
			System.out.println("Extra elements (in converted but not in original):");
			for (GeoComparisonResult result : resultsList) {
				if (result.getStatus() == GeoComparisonResult.Status.EXTRA) {
					System.out.println("  - " + result.getLabel()
							+ (result.getConvertedType() != null
									? " (" + result.getConvertedType() + ")" : ""));
				}
			}
		}
		
		// Different objects
		if (diffCount > 0) {
			System.out.println();
			System.out.println("Elements with differences:");
			for (GeoComparisonResult result : resultsList) {
				if (result.getStatus() == GeoComparisonResult.Status.DIFFERENT) {
					System.out.println("  " + result.getLabel() + ":");
					if (verbose) {
						for (GeoComparisonResult.PropertyDifference diff : result.getDifferences()) {
							System.out.println("    - " + diff.tagName
									+ (diff.attribute != null ? "." + diff.attribute : "")
									+ ": " + diff.originalValue
									+ " -> " + diff.convertedValue);
						}
					} else {
						// Just show count and tag names
						List<String> tagNames = new ArrayList<>();
						for (GeoComparisonResult.PropertyDifference diff : result.getDifferences()) {
							String name = diff.tagName
									+ (diff.attribute != null ? "." + diff.attribute : "");
							if (!tagNames.contains(name)) {
								tagNames.add(name);
							}
						}
						System.out.println("    " + result.getDifferences().size()
								+ " differences: " + String.join(", ", tagNames));
					}
				}
			}
		}
	}
	
	/**
	 * Prints macro comparison results.
	 */
	private void printMacroResults(boolean verbose) {
		System.out.println();
		System.out.println("Macro Comparison:");
		
		int macroMatched = 0;
		int macroDifferent = 0;
		int macroMissing = 0;
		int macroExtra = 0;
		
		for (MacroComparisonResult mr : macroResults) {
			switch (mr.status) {
			case MATCHED:
				macroMatched++;
				break;
			case DIFFERENT:
				macroDifferent++;
				break;
			case MISSING:
				macroMissing++;
				break;
			case EXTRA:
				macroExtra++;
				break;
			}
		}
		
		System.out.println("  Matched: " + macroMatched);
		System.out.println("  Different: " + macroDifferent);
		System.out.println("  Missing: " + macroMissing);
		System.out.println("  Extra: " + macroExtra);
		
		if (macroMissing > 0) {
			System.out.println();
			System.out.println("  Missing macros:");
			for (MacroComparisonResult mr : macroResults) {
				if (mr.status == GeoComparisonResult.Status.MISSING) {
					System.out.println("    - " + mr.cmdName);
				}
			}
		}
		
		if (macroExtra > 0) {
			System.out.println();
			System.out.println("  Extra macros:");
			for (MacroComparisonResult mr : macroResults) {
				if (mr.status == GeoComparisonResult.Status.EXTRA) {
					System.out.println("    - " + mr.cmdName);
				}
			}
		}
		
		if (macroDifferent > 0) {
			System.out.println();
			System.out.println("  Macros with differences:");
			for (MacroComparisonResult mr : macroResults) {
				if (mr.status == GeoComparisonResult.Status.DIFFERENT) {
					System.out.println("    " + mr.cmdName + ":");
					if (!mr.headerDiffs.isEmpty()) {
						System.out.println("      Header differences:");
						for (GeoComparisonResult.PropertyDifference diff : mr.headerDiffs) {
							System.out.println("        - " + diff.tagName
									+ (diff.attribute != null ? "." + diff.attribute : "")
									+ ": " + diff.originalValue
									+ " -> " + diff.convertedValue);
						}
					}
					if (!mr.constructionResults.isEmpty()) {
						System.out.println("      Construction differences:");
						for (GeoComparisonResult r : mr.constructionResults) {
							if (verbose) {
								System.out.println("        " + r.toString()
										.replace("\n", "\n        "));
							} else {
								System.out.println("        " + r.getLabel()
										+ ": " + r.getStatus()
										+ (r.hasDifferences()
												? " (" + r.getDifferences().size() + " diffs)" : ""));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Writes the report to a JSON file.
	 * @param outputFile the output file path
	 * @throws IOException if writing fails
	 */
	public void writeToJson(File outputFile) throws IOException {
		StringBuilder json = new StringBuilder();
		
		json.append("{\n");
		json.append("  \"inputFile\": ").append(escapeJson(inputFile)).append(",\n");
		json.append("  \"timestamp\": ").append(escapeJson(timestamp)).append(",\n");
		
		// Summary
		json.append("  \"summary\": {\n");
		json.append("    \"totalOriginal\": ").append(totalOriginal).append(",\n");
		json.append("    \"totalConverted\": ").append(totalConverted).append(",\n");
		json.append("    \"matched\": ").append(matched).append(",\n");
		json.append("    \"different\": ").append(different).append(",\n");
		json.append("    \"missing\": ").append(missing).append(",\n");
		json.append("    \"extra\": ").append(extra).append("\n");
		json.append("  },\n");
		
		// Differences
		json.append("  \"differences\": [\n");
		writeResultsJson(json, results, "    ");
		json.append("\n  ],\n");
		
		// Missing
		json.append("  \"missing\": [");
		boolean firstMissing = true;
		for (GeoComparisonResult result : results) {
			if (result.getStatus() == GeoComparisonResult.Status.MISSING) {
				if (!firstMissing) {
					json.append(", ");
				}
				firstMissing = false;
				json.append(escapeJson(result.getLabel()));
			}
		}
		json.append("],\n");
		
		// Extra
		json.append("  \"extra\": [");
		boolean firstExtra = true;
		for (GeoComparisonResult result : results) {
			if (result.getStatus() == GeoComparisonResult.Status.EXTRA) {
				if (!firstExtra) {
					json.append(", ");
				}
				firstExtra = false;
				json.append(escapeJson(result.getLabel()));
			}
		}
		json.append("],\n");
		
		// Macros
		json.append("  \"macros\": [\n");
		boolean firstMacro = true;
		for (MacroComparisonResult mr : macroResults) {
			if (!firstMacro) {
				json.append(",\n");
			}
			firstMacro = false;
			json.append("    {\n");
			json.append("      \"cmdName\": ").append(escapeJson(mr.cmdName)).append(",\n");
			json.append("      \"status\": ").append(escapeJson(mr.status.name())).append(",\n");
			json.append("      \"headerDifferences\": [\n");
			writePropDiffsJson(json, mr.headerDiffs, "        ");
			json.append("\n      ],\n");
			json.append("      \"constructionDifferences\": [\n");
			writeResultsJson(json, mr.constructionResults, "        ");
			json.append("\n      ]\n");
			json.append("    }");
		}
		json.append("\n  ]\n");
		
		json.append("}\n");
		
		// Write to file
		try (OutputStreamWriter writer = new OutputStreamWriter(
				new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
			writer.write(json.toString());
		}
	}
	
	/**
	 * Writes comparison results as JSON array items.
	 */
	private void writeResultsJson(StringBuilder json, List<GeoComparisonResult> resultsList,
			String indent) {
		boolean first = true;
		for (GeoComparisonResult result : resultsList) {
			if (result.getStatus() == GeoComparisonResult.Status.DIFFERENT) {
				if (!first) {
					json.append(",\n");
				}
				first = false;
				
				json.append(indent).append("{\n");
				json.append(indent).append("  \"label\": ")
						.append(escapeJson(result.getLabel())).append(",\n");
				json.append(indent).append("  \"status\": \"different\",\n");
				json.append(indent).append("  \"type\": {\n");
				json.append(indent).append("    \"original\": ")
						.append(escapeJson(result.getOriginalType())).append(",\n");
				json.append(indent).append("    \"converted\": ")
						.append(escapeJson(result.getConvertedType())).append("\n");
				json.append(indent).append("  },\n");
				json.append(indent).append("  \"differences\": [\n");
				writePropDiffsJson(json, result.getDifferences(), indent + "    ");
				json.append("\n").append(indent).append("  ]\n");
				json.append(indent).append("}");
			}
		}
	}
	
	/**
	 * Writes property differences as JSON array items.
	 */
	private void writePropDiffsJson(StringBuilder json,
			List<GeoComparisonResult.PropertyDifference> diffs, String indent) {
		boolean first = true;
		for (GeoComparisonResult.PropertyDifference diff : diffs) {
			if (!first) {
				json.append(",\n");
			}
			first = false;
			
			json.append(indent).append("{\n");
			json.append(indent).append("  \"tag\": ").append(escapeJson(diff.tagName)).append(",\n");
			json.append(indent).append("  \"attribute\": ")
					.append(escapeJson(diff.attribute)).append(",\n");
			json.append(indent).append("  \"original\": ")
					.append(escapeJson(diff.originalValue)).append(",\n");
			json.append(indent).append("  \"converted\": ")
					.append(escapeJson(diff.convertedValue)).append("\n");
			json.append(indent).append("}");
		}
	}
	
	// ==================== XML Parsing Helpers ====================
	
	/**
	 * Parses an XML string into a DOM Document.
	 */
	private Document parseXml(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		// Disable external entities for security
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}
	
	/**
	 * Gets the first child element with the given tag name.
	 */
	private static Element getFirstElement(Element parent, String tagName) {
		if (parent == null) {
			return null;
		}
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
					&& tagName.equals(((Element) child).getTagName())) {
				return (Element) child;
			}
		}
		return null;
	}
	
	/**
	 * Adds all attribute names from an element to a set.
	 */
	private void addAttrNames(Set<String> set, Element elem) {
		if (elem == null) {
			return;
		}
		NamedNodeMap attrs = elem.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			set.add(attrs.item(i).getNodeName());
		}
	}
	
	/**
	 * Converts an element's attributes to a readable string.
	 */
	private String attrsToString(Element elem) {
		if (elem == null) {
			return "(null)";
		}
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
	 * Null-safe string equality.
	 */
	private boolean safeEquals(String a, String b) {
		if (a == null) {
			return b == null;
		}
		return a.equals(b);
	}
	
	/**
	 * Escapes a string for JSON.
	 */
	private String escapeJson(String value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("\"");
		for (char c : value.toCharArray()) {
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < ' ') {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		sb.append("\"");
		return sb.toString();
	}
	
	// Getters for statistics
	public int getTotalOriginal() { return totalOriginal; }
	public int getTotalConverted() { return totalConverted; }
	public int getMatched() { return matched; }
	public int getDifferent() { return different; }
	public int getMissing() { return missing; }
	public int getExtra() { return extra; }
	public List<GeoComparisonResult.PropertyDifference> getSettingsDiffs() {
		return settingsDiffs;
	}
}
