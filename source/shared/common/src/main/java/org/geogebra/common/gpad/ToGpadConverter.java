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
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.util.debug.Log;

/**
 * Converts GeoGebra construction XML to Gpad format by parsing XML directly.
 * Produces the static roundtrip format with type prefixes and {@code :=} / {@code =} syntax.
 */
public class ToGpadConverter implements DocHandler {
	private final String xmlFile;
	private final String xmlMacro;
	private final GpadGenerator gpadGenerator;

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
	private GpadSerializer.GpadSerializeStartPoint currentStartPointSerializer;
	private GpadSerializer.GpadSerializeBarTag currentBarTagSerializer;
	private boolean inCommand = false;
	private boolean inElement = false;
	private int elementDepth = 0;
	private Set<String> pendingOutputLabels = new java.util.HashSet<>();
	private Map<String, Map<String, LinkedHashMap<String, String>>> labelToStyleMap = new HashMap<>();
	private Map<String, String> labelToElementType = new HashMap<>();

	private static final java.util.Set<String> CREATION_DATA_TAGS = java.util.Set.of(
			"coords", "value", "matrix", "slider", "caption", "file", "linkedGeo");

	private final XmlSettingsCollector settingsCollector = new XmlSettingsCollector();

	public ToGpadConverter(String xmlFile, String xmlMacro, boolean mergeStylesheets) {
		this.xmlFile = xmlFile;
		this.xmlMacro = xmlMacro;
		this.gpadGenerator = new GpadGenerator(mergeStylesheets, false);
	}

	private ToGpadConverter(boolean mergeStylesheets, boolean inMacroConstruction) {
		this.xmlFile = null;
		this.xmlMacro = null;
		this.gpadGenerator = new GpadGenerator(mergeStylesheets, inMacroConstruction);
	}

	public String toGpad() {
		StringBuilder sb = new StringBuilder();
		if (xmlMacro != null && !xmlMacro.trim().isEmpty())
			convertMacrosXML(xmlMacro, sb);
		if (xmlFile != null && !xmlFile.trim().isEmpty())
			convertConstructionXML(xmlFile, sb);
		return sb.toString();
	}

	/**
	 * Converts a single GeoElement to Gpad format using the XML-based pipeline.
	 *
	 * @param geo GeoElement to convert
	 * @return Gpad string representation, or empty string if geo is null
	 */
	public static String fromGeoElement(GeoElement geo) {
		if (geo == null)
			return "";

		StringBuilder xmlBuilder = new StringBuilder();
		xmlBuilder.append("<geogebra format=\"5.0\"><construction>");

		if (geo.getParentAlgorithm() != null)
			xmlBuilder.append(geo.getParentAlgorithm().getXML());
		else
			xmlBuilder.append(geo.getXML());

		xmlBuilder.append("</construction></geogebra>");

		ToGpadConverter converter = new ToGpadConverter(
				xmlBuilder.toString(), null, false);
		String result = converter.toGpad();
		return result != null ? result.trim() : "";
	}

	private static void parseXML(DocHandler handler, String xml) throws XMLParseException, IOException {
		QDParser parser = new QDParser();
		parser.parse(handler, new StringReader(xml));
	}

	// ============================================================
	// Macro conversion (unchanged structure, delegates to nested converter)
	// ============================================================

	private void convertMacrosXML(String xmlMacro, StringBuilder sb) {
		if (xmlMacro == null || xmlMacro.trim().isEmpty()) return;
		MacroParserHandler handler = new MacroParserHandler(this, sb);
		try {
			parseXML(handler, xmlMacro);
		} catch (Exception e) {
			Log.error("Error parsing macro XML: " + e.getMessage());
		}
	}

	private static class MacroInfo {
		String name;
		List<String> inputLabels = new ArrayList<>();
		List<String> outputLabels = new ArrayList<>();
		String constructionGpad;
		String toolName, toolHelp, iconFile, showInToolBar, copyCaptions, viewId;
	}

	private void buildLabels(StringBuilder sb, List<String> labels) {
		boolean first = true;
		if (labels != null) {
			for (String label : labels) {
				if (label != null && !label.isEmpty()) {
					if (!first) sb.append(", ");
					first = false;
					sb.append(label);
				}
			}
		}
	}

	private class MacroParserHandler implements DocHandler {
		private ToGpadConverter parentConverter;
		private StringBuilder output;
		private MacroInfo currentMacro = null;
		private boolean inMacro = false;
		private boolean inConstruction = false;
		private ToGpadConverter constructionConverter = null;
		private int constructionDepth = 0;

		MacroParserHandler(ToGpadConverter parentConverter, StringBuilder output) {
			this.parentConverter = parentConverter;
			this.output = output;
		}

		@Override
		public void startDocument() throws XMLParseException {}

		@Override
		public void startElement(String tag, LinkedHashMap<String, String> attrs) throws XMLParseException {
			if ("macro".equals(tag)) {
				inMacro = true;
				currentMacro = new MacroInfo();
				currentMacro.name = attrs.get("cmdName");
				currentMacro.toolName = attrs.get("toolName");
				currentMacro.toolHelp = attrs.get("toolHelp");
				currentMacro.iconFile = attrs.get("iconFile");
				currentMacro.showInToolBar = attrs.get("showInToolBar");
				currentMacro.copyCaptions = attrs.get("copyCaptions");
				currentMacro.viewId = attrs.get("viewId");
				if (currentMacro.name == null || currentMacro.name.isEmpty()) {
					currentMacro = null;
					inMacro = false;
				}
			} else if (inMacro && "macroInput".equals(tag)) {
				extractIndexedAttributes(attrs, currentMacro.inputLabels);
			} else if (inMacro && "macroOutput".equals(tag)) {
				extractIndexedAttributes(attrs, currentMacro.outputLabels);
			} else if (inMacro && "construction".equals(tag)) {
				inConstruction = true;
				constructionDepth = 1;
				constructionConverter = new ToGpadConverter(parentConverter.gpadGenerator.mergeStylesheets, true);
				constructionConverter.startDocument();
				constructionConverter.startElement("construction", attrs);
			} else if (inConstruction && constructionConverter != null) {
				constructionDepth++;
				constructionConverter.startElement(tag, attrs);
			}
		}

		@Override
		public void endElement(String tag) throws XMLParseException {
			if ("macro".equals(tag)) {
				if (currentMacro != null) {
					if (constructionConverter != null) {
						StringBuilder macroOutput = new StringBuilder();
						constructionConverter.processAndOutputCollectedObjects(macroOutput);
						constructionConverter.endDocument();
						currentMacro.constructionGpad = macroOutput.toString();
					}
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
					if (constructionConverter != null)
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
			if (inConstruction && constructionConverter != null)
				constructionConverter.text(str);
		}

		@Override
		public void endDocument() throws XMLParseException {}
	}

	private void convertMacro(MacroInfo macroInfo, StringBuilder sb) {
		if (macroInfo.name == null || macroInfo.name.isEmpty()) return;
		String gpad = macroInfo.constructionGpad;
		if (gpad == null || gpad.trim().isEmpty()) return;
		sb.append("@@macro ").append(macroInfo.name).append("(");
		buildLabels(sb, macroInfo.inputLabels);
		sb.append(") {\n");
		String metaLine = buildMacroMetaLine(macroInfo);
		if (metaLine != null) sb.append("    ").append(metaLine).append("\n");
		sb.append(gpad);
		sb.append("    @@return ");
		buildLabels(sb, macroInfo.outputLabels);
		sb.append("\n}\n\n");
	}

	private static String buildMacroMetaLine(MacroInfo info) {
		StringBuilder props = new StringBuilder();
		boolean first = true;
		if (info.toolName != null && !info.toolName.isEmpty()) {
			props.append("toolName: ").append(StyleMapToGpadConverter.quoteString(info.toolName));
			first = false;
		}
		if (info.toolHelp != null && !info.toolHelp.isEmpty()) {
			if (!first) props.append("; ");
			props.append("toolHelp: ").append(StyleMapToGpadConverter.quoteString(info.toolHelp));
			first = false;
		}
		if (info.iconFile != null && !info.iconFile.isEmpty()) {
			if (!first) props.append("; ");
			props.append("iconFile: ").append(StyleMapToGpadConverter.quoteString(info.iconFile));
			first = false;
		}
		if ("false".equals(info.showInToolBar)) {
			if (!first) props.append("; ");
			props.append("~showInToolBar");
			first = false;
		}
		if ("false".equals(info.copyCaptions)) {
			if (!first) props.append("; ");
			props.append("~copyCaptions");
			first = false;
		}
		if (info.viewId != null && !info.viewId.isEmpty()) {
			if (!first) props.append("; ");
			props.append("view: ").append(GpadStyleMaps.viewIdToName(info.viewId));
			first = false;
		}
		return first ? null : "@meta = { " + props + " }";
	}

	// ============================================================
	// Construction conversion
	// ============================================================

	private void convertConstructionXML(String xmlFile, StringBuilder sb) {
		if (xmlFile == null || xmlFile.trim().isEmpty()) return;
		ConstructionParserHandler handler = new ConstructionParserHandler(this);
		try {
			parseXML(handler, xmlFile);
			processAndOutputCollectedObjects(sb);
		} catch (Exception e) {
			Log.error("Error parsing construction XML: " + e.getMessage());
		}
	}

	private void processAndOutputCollectedObjects(StringBuilder sb) {
		collectPendingElements();
		if (settingsCollector.hasAnySettings()) {
			String envSettings = XmlSettingsToGpadEnvConverter.convert(settingsCollector);
			if (envSettings != null && !envSettings.isEmpty()) {
				String existingEnv = gpadGenerator.getEnvContent();
				if (existingEnv != null && !existingEnv.isEmpty()) {
					gpadGenerator.appendEnvContent(envSettings);
				} else {
					gpadGenerator.setEnvContent(
							"@@env {\n" + envSettings + "}\n\n");
				}
			}
		}
		gpadGenerator.generate(sb);
	}

	private class ConstructionParserHandler implements DocHandler {
		private ToGpadConverter converter;
		private boolean inConstruction = false;
		private int constructionDepth = 0;

		private static final int SETTINGS_NONE = 0;
		private static final int SETTINGS_KERNEL = 1;
		private static final int SETTINGS_GUI = 2;
		private static final int SETTINGS_ALGEBRA_VIEW = 3;
		private static final int SETTINGS_SPREADSHEET_VIEW = 4;
		private static final int SETTINGS_PROB_CALC = 5;
		private static final int SETTINGS_EV = 6;
		private static final int SETTINGS_EV3D = 7;
		private int settingsMode = SETTINGS_NONE;
		private int settingsDepth = 0;
		private int evCount = 0;

		// Perspective parsing state: tracks nesting within gui > perspectives > perspective
		private static final int PERSP_NONE = 0;
		private static final int PERSP_PERSPECTIVES = 1;
		private static final int PERSP_PERSPECTIVE = 2;
		private static final int PERSP_PANES = 3;
		private static final int PERSP_VIEWS = 4;
		private int perspState = PERSP_NONE;
		private int perspSkipDepth = 0;

		private static final java.util.Set<String> GUI_CAPTURE_TAGS =
				java.util.Set.of("font", "labelingStyle");

		private static final java.util.Set<String> VIEW_SKIP_ATTRS =
				java.util.Set.of("inframe", "window");

		ConstructionParserHandler(ToGpadConverter converter) {
			this.converter = converter;
		}

		@Override public void startDocument() throws XMLParseException {
			converter.resetState();
		}

		@Override public void startElement(String tag, LinkedHashMap<String, String> attrs) throws XMLParseException {
			if (settingsMode != SETTINGS_NONE) {
				handleSettingsChild(tag, attrs);
				return;
			}
			if ("construction".equals(tag)) {
				inConstruction = true;
				constructionDepth = 1;
				converter.startElement("construction", attrs);
			} else if (inConstruction) {
				constructionDepth++;
				converter.startElement(tag, attrs);
			} else if ("kernel".equals(tag)) {
				settingsMode = SETTINGS_KERNEL;
				settingsDepth = 1;
				converter.settingsCollector.setHasKernel(true);
			} else if ("gui".equals(tag)) {
				settingsMode = SETTINGS_GUI;
				settingsDepth = 1;
				perspState = PERSP_NONE;
				converter.settingsCollector.setHasGui(true);
			} else if ("algebraView".equals(tag)) {
				settingsMode = SETTINGS_ALGEBRA_VIEW;
				settingsDepth = 1;
				converter.settingsCollector.setHasAlgebraView(true);
			} else if ("spreadsheetView".equals(tag)) {
				settingsMode = SETTINGS_SPREADSHEET_VIEW;
				settingsDepth = 1;
				converter.settingsCollector.setHasSpreadsheetView(true);
				converter.settingsCollector.setSpreadsheetViewAttrs(attrs);
			} else if ("probabilityCalculator".equals(tag)) {
				settingsMode = SETTINGS_PROB_CALC;
				settingsDepth = 1;
				converter.settingsCollector.setHasProbCalc(true);
			} else if ("keyboard".equals(tag)) {
				converter.settingsCollector.setKeyboardAttrs(attrs);
			} else if ("scripting".equals(tag)) {
				converter.settingsCollector.setScriptingAttrs(attrs);
			} else if ("tableview".equals(tag)) {
				converter.settingsCollector.setTableviewAttrs(attrs);
			} else if ("euclidianView".equals(tag)) {
				settingsMode = SETTINGS_EV;
				settingsDepth = 1;
				evCount++;
				if (evCount == 1) {
					converter.settingsCollector.setHasEv1(true);
				} else {
					converter.settingsCollector.setHasEv2(true);
				}
			} else if ("euclidianView3D".equals(tag)) {
				settingsMode = SETTINGS_EV3D;
				settingsDepth = 1;
				converter.settingsCollector.setHasEv3d(true);
			}
		}

		private void handleSettingsChild(String tag, LinkedHashMap<String, String> attrs) {
			settingsDepth++;
			switch (settingsMode) {
				case SETTINGS_KERNEL:
					converter.settingsCollector.addKernelChild(tag, attrs);
					break;
				case SETTINGS_GUI:
					handleGuiChild(tag, attrs);
					break;
				case SETTINGS_ALGEBRA_VIEW:
					converter.settingsCollector.addAlgebraViewChild(tag, attrs);
					break;
				case SETTINGS_SPREADSHEET_VIEW:
					converter.settingsCollector.addSpreadsheetViewChild(tag, attrs);
					break;
				case SETTINGS_PROB_CALC:
					converter.settingsCollector.addProbCalcChild(tag, attrs);
					break;
				case SETTINGS_EV:
					if (evCount >= 2)
						converter.settingsCollector.addEv2Child(tag, attrs);
					else
						converter.settingsCollector.addEv1Child(tag, attrs);
					break;
				case SETTINGS_EV3D:
					converter.settingsCollector.addEv3dChild(tag, attrs);
					break;
			}
		}

		private void handleGuiChild(String tag, LinkedHashMap<String, String> attrs) {
			if (perspSkipDepth > 0) {
				perspSkipDepth++;
				return;
			}

			switch (perspState) {
				case PERSP_NONE:
					if (GUI_CAPTURE_TAGS.contains(tag)) {
						converter.settingsCollector.addGuiChild(tag, attrs);
					} else if ("perspectives".equals(tag)) {
						perspState = PERSP_PERSPECTIVES;
					}
					break;

				case PERSP_PERSPECTIVES:
					if ("perspective".equals(tag)) {
						perspState = PERSP_PERSPECTIVE;
						converter.settingsCollector.setHasPerspective(true);
					}
					break;

				case PERSP_PERSPECTIVE:
					if ("panes".equals(tag)) {
						perspState = PERSP_PANES;
					} else if ("views".equals(tag)) {
						perspState = PERSP_VIEWS;
					} else if ("toolbar".equals(tag)) {
						LinkedHashMap<String, String> filtered = new LinkedHashMap<>(attrs);
						filtered.remove("help");
						converter.settingsCollector.setPerspectiveToolbarAttrs(filtered);
					} else if ("input".equals(tag)) {
						converter.settingsCollector.setPerspectiveInputAttrs(attrs);
					} else if ("dockBar".equals(tag)) {
						// skip dockBar — desktop only
					} else {
						perspSkipDepth = 1;
					}
					break;

				case PERSP_PANES:
					if ("pane".equals(tag)) {
						converter.settingsCollector.addPerspectivePane(
								new XmlSettingsCollector.PaneData(
										attrs.get("location"),
										attrs.get("divider"),
										attrs.get("orientation")));
					}
					break;

				case PERSP_VIEWS:
					if ("view".equals(tag)) {
						LinkedHashMap<String, String> filtered = new LinkedHashMap<>(attrs);
						for (String skip : VIEW_SKIP_ATTRS) {
							filtered.remove(skip);
						}
						converter.settingsCollector.addPerspectiveView(
								new XmlSettingsCollector.ViewData(filtered));
					}
					break;
			}
		}

		private void handleGuiEndElement(String tag) {
			if (perspSkipDepth > 0) {
				perspSkipDepth--;
				return;
			}
			switch (perspState) {
				case PERSP_PANES:
					if ("panes".equals(tag)) perspState = PERSP_PERSPECTIVE;
					break;
				case PERSP_VIEWS:
					if ("views".equals(tag)) perspState = PERSP_PERSPECTIVE;
					break;
				case PERSP_PERSPECTIVE:
					if ("perspective".equals(tag)) perspState = PERSP_PERSPECTIVES;
					break;
				case PERSP_PERSPECTIVES:
					if ("perspectives".equals(tag)) perspState = PERSP_NONE;
					break;
			}
		}

		@Override public void endElement(String tag) throws XMLParseException {
			if (settingsMode != SETTINGS_NONE) {
				if (settingsMode == SETTINGS_GUI) {
					handleGuiEndElement(tag);
				}
				settingsDepth--;
				if (settingsDepth == 0)
					settingsMode = SETTINGS_NONE;
				return;
			}
			if (inConstruction && "construction".equals(tag)) {
				constructionDepth--;
				if (constructionDepth == 0) {
					converter.endElement("construction");
					inConstruction = false;
				} else converter.endElement(tag);
			} else if (inConstruction) {
				constructionDepth--;
				converter.endElement(tag);
			}
		}
		@Override public void text(String str) throws XMLParseException {}
		@Override public void endDocument() throws XMLParseException {}
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
		labelToStyleMap.clear();
		labelToElementType.clear();
		inCommand = false;
		inElement = false;
		elementDepth = 0;
		pendingOutputLabels.clear();
	}

	@Override public void startDocument() throws XMLParseException { resetState(); }
	@Override public void endDocument() throws XMLParseException {}

	@Override
	public void startElement(String tag, LinkedHashMap<String, String> attrs) throws XMLParseException {
		if ("command".equals(tag)) {
			collectPendingElements();
			startCommand(attrs);
		} else if ("expression".equals(tag)) {
			collectPendingElements();
			startExpression(attrs);
		} else if ("element".equals(tag))
			startElementTag(attrs);
		else if (inCommand)
			handleCommandChild(tag, attrs);
		else if (inElement)
			handleElementChild(tag, attrs);
	}

	@Override
	public void endElement(String tag) throws XMLParseException {
		if ("command".equals(tag)) endCommand();
		else if ("expression".equals(tag)) endExpression();
		else if ("element".equals(tag)) endElementTag();
		else if (inElement && elementDepth > 0) elementDepth--;
	}

	@Override public void text(String str) throws XMLParseException {}

	private void startCommand(LinkedHashMap<String, String> attrs) {
		inCommand = true;
		currentCommandName = attrs.get("name");
		currentInputArgs.clear();
		currentOutputLabels.clear();
	}

	private void handleCommandChild(String tag, LinkedHashMap<String, String> attrs) {
		if ("input".equals(tag)) extractIndexedAttributes(attrs, currentInputArgs);
		else if ("output".equals(tag)) extractIndexedAttributes(attrs, currentOutputLabels);
	}

	static void extractIndexedAttributes(LinkedHashMap<String, String> attrs, List<String> list) {
		for (String key : attrs.keySet()) {
			if (key.startsWith("a") && key.length() > 1) {
				try {
					int index = Integer.parseInt(key.substring(1));
					String value = attrs.get(key);
					if (value != null) {
						while (list.size() <= index) list.add(null);
						list.set(index, value);
					}
				} catch (NumberFormatException e) { /* ignore */ }
			}
		}
	}

	private void endCommand() {
		inCommand = false;
		pendingOutputLabels = currentOutputLabels.stream()
				.filter(Objects::nonNull).collect(Collectors.toSet());
	}

	private void startExpression(LinkedHashMap<String, String> attrs) {
		currentExpressionLabel = attrs.get("label");
		currentExpressionExp = attrs.get("exp");
		currentExpressionType = attrs.get("type");
	}

	private void endExpression() {
		pendingOutputLabels.clear();
		if (currentExpressionLabel != null)
			pendingOutputLabels.add(currentExpressionLabel);
	}

	private void startElementTag(LinkedHashMap<String, String> attrs) {
		inElement = true;
		elementDepth = 0;
		currentElementLabel = attrs.get("label");
		currentElementType = attrs.get("type");
		currentElementStyleMap = new LinkedHashMap<>();
		currentStartPointSerializer = GpadSerializer.beginSerializeStartPoint();
		currentBarTagSerializer = GpadSerializer.beginSerializeBarTag();
	}

	private void handleElementChild(String tag, LinkedHashMap<String, String> attrs) {
		elementDepth++;
		LinkedHashMap<String, String> elementAttrs = new LinkedHashMap<>();
		if (attrs != null) elementAttrs.putAll(attrs);
		if ("startPoint".equals(tag)) {
			if (currentStartPointSerializer != null) currentStartPointSerializer.add(elementAttrs);
		} else if ("tag".equals(tag)) {
			String barNumberStr = elementAttrs.get("barNumber");
			if (currentBarTagSerializer != null && barNumberStr != null)
				currentBarTagSerializer.add(barNumberStr, elementAttrs);
		} else {
			LinkedHashMap<String, String> existing = currentElementStyleMap.get(tag);
			if (existing != null) {
				existing.putAll(elementAttrs);
			} else {
				currentElementStyleMap.put(tag, elementAttrs);
			}
		}
	}

	private void endElementTag() {
		if (currentStartPointSerializer != null) {
			String serialized = currentStartPointSerializer.end();
			if (serialized != null) {
				LinkedHashMap<String, String> a = new LinkedHashMap<>();
				a.put("_corners", serialized);
				currentElementStyleMap.put("startPoint", a);
			}
		}
		if (currentBarTagSerializer != null) {
			String serialized = currentBarTagSerializer.end();
			if (serialized != null) {
				LinkedHashMap<String, String> a = new LinkedHashMap<>();
				a.put("_barTags", serialized);
				currentElementStyleMap.put("barTag", a);
			}
		}

		if (currentElementLabel == null) {
			Log.warn("Element hasn't label");
		} else {
			Map<String, LinkedHashMap<String, String>> styleMapCopy = new LinkedHashMap<>();
			if (currentElementStyleMap != null) {
				for (Map.Entry<String, LinkedHashMap<String, String>> entry : currentElementStyleMap.entrySet())
					styleMapCopy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
			}
			labelToStyleMap.put(currentElementLabel, styleMapCopy);
			labelToElementType.put(currentElementLabel, currentElementType);

			if (pendingOutputLabels.contains(currentElementLabel)) {
				StyleMapToGpadConverter.extractShowVisibility(currentElementStyleMap);
				generateStyleSheet(currentElementLabel, currentElementType, currentElementStyleMap);
				pendingOutputLabels.remove(currentElementLabel);
			} else {
				collectPendingElements();
				collectIndependentElement(currentElementLabel, currentElementType, currentElementStyleMap);
			}
		}
		inElement = false;
		elementDepth = 0;
	}

	// ============================================================
	// Collection methods — populate CollectedItem with new fields
	// ============================================================

	private void collectIndependentElement(String label, String type,
			Map<String, LinkedHashMap<String, String>> styleMap) {
		if (styleMap == null) return;

		GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
		item.itemType = GpadGenerator.CollectedItem.ItemType.INDEPENDENT;

		GpadGenerator.SingleElementInfo elemInfo = new GpadGenerator.SingleElementInfo();
		elemInfo.label = label;
		elemInfo.elementType = type;
		item.elements.add(elemInfo);

		Map<String, LinkedHashMap<String, String>> creationMap = new LinkedHashMap<>();
		Map<String, LinkedHashMap<String, String>> styleOnlyMap = new LinkedHashMap<>();

		for (Map.Entry<String, LinkedHashMap<String, String>> e : styleMap.entrySet()) {
			if (CREATION_DATA_TAGS.contains(e.getKey()))
				creationMap.put(e.getKey(), e.getValue());
			else
				styleOnlyMap.put(e.getKey(), e.getValue());
		}

		boolean[] vis = StyleMapToGpadConverter.extractShowVisibility(styleOnlyMap);
		elemInfo.showObject = vis[0];
		elemInfo.showLabel = vis[1];

		String primaryTag = getPrimaryAttributeTag(type);
		if (primaryTag != null && creationMap.containsKey(primaryTag)) {
			LinkedHashMap<String, String> primaryAttrs = creationMap.get(primaryTag);
			item.shorthandText = generateShorthand(type, primaryTag, primaryAttrs);
			String shorthandAttr = getShorthandAttrName(primaryTag);
			if (shorthandAttr != null) {
				primaryAttrs.remove(shorthandAttr);
				if (primaryAttrs.isEmpty())
					creationMap.remove(primaryTag);
			} else {
				creationMap.remove(primaryTag);
			}
		}

		if (!creationMap.isEmpty()) {
			String content = StyleMapToGpadConverter.convertToContentOnly(creationMap, type);
			if (content != null && !content.isEmpty())
				item.extraCreationData = content;
		}

		elemInfo.styleSheetName = generateStyleSheet(label, type, styleOnlyMap);
		gpadGenerator.addCollectedItem(item);
	}

	/**
	 * Commands whose output objects are free on a path/region.
	 * Coords must be preserved for these to maintain the constrained position.
	 */
	private static final java.util.Set<String> PATH_CONSTRAINED_COMMANDS =
			java.util.Set.of("Point", "PointIn");

	private void collectPendingCommand() {
		if (currentCommandName != null && !currentOutputLabels.isEmpty()) {
			StringBuilder cmdBuilder = new StringBuilder();
			boolean first = true;
			for (String arg : currentInputArgs) {
				if (arg != null) {
					if (!first) cmdBuilder.append(", ");
					first = false;
					cmdBuilder.append(arg);
				}
			}
			String argsStr = cmdBuilder.length() > 0 ? "(" + cmdBuilder + ")" : "()";
			String commandStr = currentCommandName + argsStr;

			boolean keepCoords = PATH_CONSTRAINED_COMMANDS.contains(currentCommandName);

			GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
			item.itemType = GpadGenerator.CollectedItem.ItemType.COMMAND;
			item.rhsText = commandStr;

			for (String label : currentOutputLabels) {
				if (label == null) continue;
				Map<String, LinkedHashMap<String, String>> styleMap = labelToStyleMap.get(label);
				String elType = labelToElementType.get(label);
				String styleSheetName = null;
				if (!keepCoords && styleMap != null)
					styleMap.remove("coords");
				boolean[] vis = StyleMapToGpadConverter.extractShowVisibility(styleMap);
				if (styleMap != null && !styleMap.isEmpty())
					styleSheetName = generateStyleSheet(label, null, styleMap);
				GpadGenerator.SingleElementInfo ei = new GpadGenerator.SingleElementInfo(label, elType, styleSheetName);
				ei.showObject = vis[0];
				ei.showLabel = vis[1];
				item.elements.add(ei);
			}

			gpadGenerator.addCollectedItem(item);
			currentCommandName = null;
			currentInputArgs.clear();
			currentOutputLabels.clear();
		}
	}

	private void collectPendingExpression() {
		if (currentExpressionLabel == null || currentExpressionExp == null)
			return;

		GpadGenerator.SingleElementInfo exprInfo = new GpadGenerator.SingleElementInfo();
		exprInfo.label = currentExpressionLabel;

		String originalLabel = currentExpressionLabel;
		String exp = currentExpressionExp;

		String labelColonSpacePrefix = currentExpressionLabel + ": ";
		String labelColonPrefix = currentExpressionLabel + ":";
		if (exp.startsWith(labelColonSpacePrefix))
			exp = exp.substring(labelColonSpacePrefix.length()).trim();
		else if (exp.startsWith(labelColonPrefix))
			exp = exp.substring(labelColonPrefix.length()).trim();

		if (exp.contains("=") && exp.indexOf('=') > 0) {
			String lhs = exp.substring(0, exp.indexOf('=')).trim();
			if (lhs.contains("(") && lhs.startsWith(currentExpressionLabel + "(")) {
				exprInfo.label = lhs;
				exp = exp.substring(exp.indexOf('=') + 1).trim();
			} else if ("function".equals(currentExpressionType)
					&& lhs.equals(currentExpressionLabel)) {
				exprInfo.label = lhs;
				exp = exp.substring(exp.indexOf('=') + 1).trim();
			}
		}

		// Strip simple "label = " prefix for non-function cases
		String simpleLabelEq = currentExpressionLabel + " = ";
		if (exp.startsWith(simpleLabelEq)) {
			exp = exp.substring(simpleLabelEq.length()).trim();
		}

		String elType = labelToElementType.get(originalLabel);
		if (elType == null) elType = currentExpressionType;
		exprInfo.elementType = elType;

		GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
		item.itemType = GpadGenerator.CollectedItem.ItemType.EXPRESSION;
		item.elements.add(exprInfo);
		item.rhsText = exp;

		Map<String, LinkedHashMap<String, String>> styleMap = labelToStyleMap.get(originalLabel);
		if (styleMap != null)
			styleMap.remove("coords");
		boolean[] vis = StyleMapToGpadConverter.extractShowVisibility(styleMap);
		exprInfo.showObject = vis[0];
		exprInfo.showLabel = vis[1];
		if (styleMap != null && !styleMap.isEmpty())
			exprInfo.styleSheetName = generateStyleSheet(originalLabel, elType, styleMap);

		gpadGenerator.addCollectedItem(item);
		currentExpressionLabel = null;
		currentExpressionExp = null;
	}

	private void collectPendingElements() {
		if (!pendingOutputLabels.isEmpty()) {
			Log.warn("Command/expression ended but " + pendingOutputLabels.size()
					+ " style element(s) expected for output labels: " + pendingOutputLabels);
			pendingOutputLabels.clear();
		}
		collectPendingExpression();
		collectPendingCommand();
	}

	private String generateStyleSheet(String label, String type,
			Map<String, LinkedHashMap<String, String>> styleMap) {
		return gpadGenerator.generateStyleSheet(label, type, styleMap);
	}

	// ============================================================
	// Primary attribute shorthand generation
	// ============================================================

	private static String getPrimaryAttributeTag(String type) {
		if (type == null) return null;
		switch (type) {
			case "point": case "point3d":
			case "vector": case "vector3d":
			case "line": case "line3d":
			case "plane3d":
				return "coords";
			case "numeric": case "angle": case "angle3d":
			case "boolean":
				return "value";
			case "text":
				return "value";
			case "button":
				return "caption";
			case "image":
				return "file";
			default:
				return null;
		}
	}

	private static String getShorthandAttrName(String primaryTag) {
		switch (primaryTag) {
			case "value": case "caption": return "val";
			case "file": return "name";
			default: return null; // coords uses x/y/z — fully consumed
		}
	}

	private String generateShorthand(String type, String primaryTag, LinkedHashMap<String, String> attrs) {
		if (attrs == null) return null;
		switch (primaryTag) {
			case "coords": return generateCoordsShorthand(type, attrs);
			case "value":  return generateValueShorthand(type, attrs);
			case "caption": return generateCaptionShorthand(attrs);
			case "file":   return generateFileShorthand(attrs);
			default: return null;
		}
	}

	private String generateCoordsShorthand(String type, LinkedHashMap<String, String> attrs) {
		String x = formatNum(attrs.get("x"));
		String y = formatNum(attrs.get("y"));
		if (x == null || y == null) return null;
		String z = attrs.get("z") != null ? formatNum(attrs.get("z")) : null;
		String w = attrs.get("w") != null ? formatNum(attrs.get("w")) : null;

		if ("plane3d".equals(type)) {
			return "(" + x + ", " + y + ", " + (z != null ? z : "0") + ", " + (w != null ? w : "0") + ")";
		}
		if ("line".equals(type) || "line3d".equals(type)) {
			return "(" + x + ", " + y + ", " + (z != null ? z : "0") + ")";
		}

		boolean is3d = type != null && type.endsWith("3d");
		if (z != null) {
			String defaultZ = ("point".equals(type) || "point3d".equals(type)) ? "1" : "0";
			boolean zIsDefault = z.equals(defaultZ) || z.equals(defaultZ + ".0");
			if (is3d || !zIsDefault)
				return "(" + x + ", " + y + ", " + z + ")";
		}
		return "(" + x + ", " + y + ")";
	}

	private static String generateValueShorthand(String type, LinkedHashMap<String, String> attrs) {
		String val = attrs.get("val");
		if (val == null) return null;
		if ("text".equals(type))
			return StyleMapToGpadConverter.quoteString(val);
		return val;
	}

	private static String generateCaptionShorthand(LinkedHashMap<String, String> attrs) {
		String val = attrs.get("val");
		if (val == null) val = "";
		return StyleMapToGpadConverter.quoteString(val);
	}

	private static String generateFileShorthand(LinkedHashMap<String, String> attrs) {
		String name = attrs.get("name");
		if (name == null) name = "";
		return StyleMapToGpadConverter.quoteString(name);
	}

	// ============================================================
	// Utility methods
	// ============================================================

	private static String formatNum(String numStr) {
		if (numStr == null) return null;
		try {
			double d = Double.parseDouble(numStr);
			return roundSignificant(d, 12);
		} catch (NumberFormatException e) {
			return numStr;
		}
	}

	/**
	 * Rounds a double to the given number of significant digits.
	 * Returns a plain decimal string (no scientific notation).
	 */
	static String roundSignificant(double d, int sigDigits) {
		if (Math.abs(d) < 1e-10) return "0";
		java.math.BigDecimal bd = new java.math.BigDecimal(d)
				.round(new java.math.MathContext(sigDigits));
		return bd.stripTrailingZeros().toPlainString();
	}
}
