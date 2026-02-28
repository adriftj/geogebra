package org.geogebra.common.gpad;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.GeoGebraConstants;
import org.geogebra.common.io.MyXMLio;
import org.geogebra.common.io.XMLStringBuilder;
import org.geogebra.common.util.debug.Log;

/**
 * Converts a list of {@link GpadStaticItem} into GeoGebra construction XML
 * without any kernel interaction. This is the inverse of
 * {@link ToGpadConverter}: GPAD text → {@code Parser.parseGpadStatic()} →
 * {@code GpadStaticItem} list → this class → XML string.
 * <p>
 * This converter is purely static — it never reads from an App's runtime
 * state.  All settings come from the GPAD @@env block; when no @@env is
 * present, minimal GeoGebra defaults are emitted.
 */
public class GpadToXmlStaticConverter {

	private static final String DEFAULT_KERNEL_XML =
			"<kernel>\n"
			+ "\t<continuous val=\"false\"/>\n"
			+ "\t<decimals val=\"15\"/>\n"
			+ "\t<angleUnit val=\"degree\"/>\n"
			+ "\t<algebraStyle val=\"0\" spreadsheet=\"0\"/>\n"
			+ "\t<coordStyle val=\"0\"/>\n"
			+ "\t<uses3D val=\"false\"/>\n"
			+ "</kernel>\n";

	private static final String DEFAULT_PERSPECTIVES_XML =
			"<perspectives>\n"
			+ "\t<perspective id=\"tmp\">\n"
			+ "\t\t<panes>\n"
			+ "\t\t\t<pane location=\"\" divider=\"0.3\" orientation=\"1\"/>\n"
			+ "\t\t</panes>\n"
			+ "\t\t<views>\n"
			+ "\t\t\t<view id=\"1\" visible=\"true\" inframe=\"false\" stylebar=\"false\""
			+ " location=\"1\" size=\"500\" window=\"100,100,600,400\"/>\n"
			+ "\t\t\t<view id=\"2\" visible=\"false\" inframe=\"false\" stylebar=\"false\""
			+ " location=\"3\" size=\"200\" tab=\"TOOLS\" window=\"100,100,250,400\"/>\n"
			+ "\t\t</views>\n"
			+ "\t\t<toolbar show=\"true\" items=\"0 77 73 62 | 1 501 67 , 5 19 , 72 75 76"
			+ " | 2 15 45 , 18 65 , 7 37 | 4 3 8 9 , 13 44 , 58 , 47 | 16 51 64 , 70"
			+ " | 10 34 53 11 , 24  20 22 , 21 23 | 55 56 57 , 12 | 36 46 , 38 49  50 , 71  14  68"
			+ " | 30 29 54 32 31 33 | 25 17 26 60 52 61 | 40 41 42 , 27 28 35 , 6\""
			+ " position=\"1\" help=\"false\"/>\n"
			+ "\t\t<input show=\"true\" cmd=\"true\" top=\"algebra\"/>\n"
			+ "\t\t<dockBar show=\"false\" east=\"false\"/>\n"
			+ "\t</perspective>\n"
			+ "</perspectives>\n";

	private static final String DEFAULT_GUI_XML =
			"<gui>\n" + DEFAULT_PERSPECTIVES_XML + "</gui>\n";

	private static final String DEFAULT_EV_XML =
			"<euclidianView>\n"
			+ "\t<coordSystem xZero=\"215\" yZero=\"315\" scale=\"50\" yscale=\"50\"/>\n"
			+ "\t<evSettings axes=\"true\" grid=\"false\" gridIsBold=\"false\""
			+ " pointCapturing=\"3\" rightAngleStyle=\"1\" checkboxSize=\"26\""
			+ " gridType=\"3\"/>\n"
			+ "\t<bgColor r=\"255\" g=\"255\" b=\"255\"/>\n"
			+ "\t<axesColor r=\"0\" g=\"0\" b=\"0\"/>\n"
			+ "\t<gridColor r=\"192\" g=\"192\" b=\"192\"/>\n"
			+ "\t<lineStyle axes=\"1\" grid=\"0\"/>\n"
			+ "\t<axis id=\"0\" show=\"true\" label=\"\" unitLabel=\"\" tickStyle=\"1\" showNumbers=\"true\"/>\n"
			+ "\t<axis id=\"1\" show=\"true\" label=\"\" unitLabel=\"\" tickStyle=\"1\" showNumbers=\"true\"/>\n"
			+ "</euclidianView>\n";

	/**
	 * Builds a complete {@code geogebra.xml} document from a list of static
	 * items using only the GPAD text content — no runtime App state is used.
	 *
	 * @param items list of parsed static items
	 * @return full XML string suitable for writing into a .ggb file
	 */
	public static String buildFullXml(List<GpadStaticItem> items) {
		return buildFullXml(items, null);
	}

	/**
	 * Builds a complete {@code geogebra.xml} document from a list of static
	 * items, collecting type-inference warnings.
	 *
	 * @param items    list of parsed static items
	 * @param warnings list to collect type-inference warnings (may be {@code null})
	 * @return full XML string suitable for writing into a .ggb file
	 */
	public static String buildFullXml(List<GpadStaticItem> items,
			List<String> warnings) {
		// Process env first so we know the app code before writing the XML header.
		GpadStaticItem envItem = findEnvItem(items);
		String envRaw = envItem != null ? envItem.rawContent : null;
		boolean hasEnvContent = (envRaw != null && !envRaw.isEmpty())
				|| (envItem != null && envItem.templateName != null);

		GpadEnvToXmlConverter.ConvertResult env = null;
		String appCode = GeoGebraConstants.GEOMETRY_APPCODE;
		String subAppCode = null;
		if (hasEnvContent) {
			String templateContent = resolveTemplate(
					envItem != null ? envItem.templateName : null);
			env = GpadEnvToXmlConverter.convertAll(templateContent, envRaw);
			if (env.appCode != null) appCode = env.appCode;
			subAppCode = env.subAppCode;
		}

		XMLStringBuilder sb = new XMLStringBuilder();
		MyXMLio.addXMLHeader(sb);
		addStaticGeoGebraHeader(sb, false, appCode, subAppCode);

		if (env != null) {
			appendIfNotEmpty(sb, env.ev1Xml);
			appendIfNotEmpty(sb, env.ev2Xml);
			appendIfNotEmpty(sb, env.ev3dXml);

			sb.append(new XMLStringBuilder(
					mergeGuiXml(env.guiXml, env.perspectiveXml)));

			if (env.kernelXml.length() > 0)
				sb.append(new XMLStringBuilder(env.kernelXml));
			else
				sb.append(new XMLStringBuilder(new StringBuilder(DEFAULT_KERNEL_XML)));

			appendIfNotEmpty(sb, env.spreadsheetViewXml);
			appendIfNotEmpty(sb, env.algebraViewXml);
			appendIfNotEmpty(sb, env.keyboardXml);
			appendIfNotEmpty(sb, env.probCalcXml);
			appendIfNotEmpty(sb, env.tableviewXml);
			appendIfNotEmpty(sb, env.scriptingXml);
		} else {
			sb.append(new XMLStringBuilder(new StringBuilder(DEFAULT_EV_XML)));
			sb.append(new XMLStringBuilder(new StringBuilder(DEFAULT_KERNEL_XML)));
			sb.append(new XMLStringBuilder(new StringBuilder(DEFAULT_GUI_XML)));
		}

		String constructionXml = buildConstructionXml(items, warnings);
		sb.append(new XMLStringBuilder(new StringBuilder(constructionXml)));

		sb.closeTag("geogebra");
		return sb.toString();
	}

	private static void addStaticGeoGebraHeader(XMLStringBuilder sb, boolean isMacro,
			String appCode, String subAppCode) {
		sb.startOpeningTag("geogebra", 0);
		sb.attrRaw("format", GeoGebraConstants.XML_FILE_FORMAT);
		sb.attrRaw("version", GeoGebraConstants.VERSION_STRING);
		sb.attrRaw("app", appCode);
		if (subAppCode != null)
			sb.attrRaw("subApp", subAppCode);
		sb.attrRaw("platform", "d");
		StringBuilder schema = new StringBuilder("https://www.geogebra.org/apps/xsd/");
		if (isMacro)
			schema.append(GeoGebraConstants.GGT_XSD_FILENAME);
		else
			schema.append(GeoGebraConstants.GGB_XSD_FILENAME);
		sb.attrRaw("xsi:noNamespaceSchemaLocation", schema.toString());
		sb.endTag();
	}

	private static void appendIfNotEmpty(XMLStringBuilder sb, StringBuilder content) {
		if (content != null && content.length() > 0)
			sb.append(new XMLStringBuilder(content));
	}

	/**
	 * Merges separate gui XML (font/labeling) and perspective XML into
	 * a single {@code <gui>...</gui>} block. If guiXml is present it already
	 * wraps content in {@code <gui>} tags; the perspective content is inserted
	 * before the closing {@code </gui>}. If only perspective is present,
	 * a new {@code <gui>} wrapper is created. If only guiXml is present
	 * (no perspective), a default perspective is injected so that
	 * {@code MyXMLHandler} always populates {@code dockPanelData}.
	 */
	private static StringBuilder mergeGuiXml(StringBuilder guiXml, StringBuilder perspXml) {
		StringBuilder effectivePersp = (perspXml != null && perspXml.length() > 0)
				? perspXml : new StringBuilder(DEFAULT_PERSPECTIVES_XML);
		if (guiXml.length() > 0) {
			String guiStr = guiXml.toString();
			int closeIdx = guiStr.lastIndexOf("</gui>");
			if (closeIdx >= 0) {
				StringBuilder merged = new StringBuilder();
				merged.append(guiStr, 0, closeIdx);
				merged.append(effectivePersp);
				merged.append("</gui>\n");
				return merged;
			}
		}
		StringBuilder wrapped = new StringBuilder();
		wrapped.append("<gui>\n");
		wrapped.append(effectivePersp);
		wrapped.append("</gui>\n");
		return wrapped;
	}

	private static GpadStaticItem findEnvItem(List<GpadStaticItem> items) {
		for (GpadStaticItem item : items) {
			if (item.type == GpadStaticItem.Type.ENV) {
				return item;
			}
		}
		return null;
	}

	private static String resolveTemplate(String templateName) {
		if (templateName == null || templateName.isEmpty()) {
			return null;
		}
		String content = GpadEnvTemplates.get(templateName);
		if (content == null) {
			Log.warn("Unknown @@env template: " + templateName + ", ignoring");
		}
		return content;
	}

	/**
	 * Builds a complete {@code <construction>} XML fragment from a list of
	 * static items. Stylesheet and @@set items are resolved and merged into
	 * element styles.
	 *
	 * @param items list of parsed static items
	 * @return XML string containing the {@code <construction>} block
	 */
	public static String buildConstructionXml(List<GpadStaticItem> items) {
		return buildConstructionXml(items, null);
	}

	/**
	 * Builds a complete {@code <construction>} XML fragment from a list of
	 * static items, collecting type-inference warnings.
	 *
	 * @param items    list of parsed static items
	 * @param warnings list to collect type-inference warnings (may be {@code null})
	 * @return XML string containing the {@code <construction>} block
	 */
	public static String buildConstructionXml(List<GpadStaticItem> items,
			List<String> warnings) {
		XMLStringBuilder sb = new XMLStringBuilder();
		sb.startOpeningTag("construction", 0);
		sb.attr("title", "");
		sb.attr("author", "");
		sb.attr("date", "");
		sb.endTag();

		for (GpadStaticItem item : items) {
			if (item.type == null) continue;
			GpadTypeInferrer.inferMissingTypes(item, warnings);
			switch (item.type) {
				case COMMAND:    emitCommand(sb, item); break;
				case EXPRESSION: emitExpression(sb, item); break;
				case INDEPENDENT: emitIndependent(sb, item); break;
				default: break;
			}
		}

		sb.closeTag("construction");
		return sb.toString();
	}

	// ============================================================
	// COMMAND:  point A @s1, line f @s2 = Line(B, C);
	// ============================================================

	private static void emitCommand(XMLStringBuilder sb, GpadStaticItem item) {
		if (item.commandName == null) return;

		sb.startOpeningTag("command", 1);
		sb.attr("name", item.commandName);
		sb.endTag();

		// <input a0="B" a1="C"/>
		if (item.commandArgs != null && !item.commandArgs.isEmpty()) {
			sb.startTag("input", 2);
			for (int i = 0; i < item.commandArgs.size(); i++)
				sb.attr("a" + i, item.commandArgs.get(i));
			sb.endTag();
		}

		// <output a0="A" a1="f"/>
		if (!item.labels.isEmpty()) {
			sb.startTag("output", 2);
			for (int i = 0; i < item.labels.size(); i++) {
				String lbl = item.labels.get(i).label;
				if (lbl.startsWith("OriginalEmpty_"))
					lbl = "";
				sb.attr("a" + i, lbl);
			}
			sb.endTag();
		}

		sb.closeTag("command");

		for (GpadStaticItem.TypedLabel tl : item.labels) {
			if (!tl.label.startsWith("OriginalEmpty_"))
				emitElement(sb, tl.elementType, tl.label, tl.styleSheet, tl.showObject, tl.showLabel);
		}
	}

	// ============================================================
	// EXPRESSION:  point A @s1 := (1, 2);
	// ============================================================

	private static void emitExpression(XMLStringBuilder sb, GpadStaticItem item) {
		if (item.labels.isEmpty() || item.rhsText == null) return;

		GpadStaticItem.TypedLabel tl = item.labels.get(0);
		String label = tl.label;
		String rhs = item.rhsText;
		String elType = tl.elementType;

		String exp = reconstructExp(label, rhs, elType);
		String xmlExprType = deriveExprType(elType);

		sb.startTag("expression", 1);
		sb.attr("label", stripFunctionParams(label));
		sb.attr("exp", exp);
		if (xmlExprType != null)
			sb.attrRaw("type", xmlExprType);
		sb.endTag();

		emitElement(sb, elType, stripFunctionParams(label), tl.styleSheet, tl.showObject, tl.showLabel);
	}

	/**
	 * Reconstruct the {@code exp} attribute for an {@code <expression>} tag.
	 * <ul>
	 *   <li>Label contains {@code (} → function form: {@code "f(x) = rhs"}</li>
	 *   <li>Equation-type element with relational operators → {@code "label: rhs"}</li>
	 *   <li>Otherwise → just {@code rhs}</li>
	 * </ul>
	 */
	static String reconstructExp(String label, String rhs, String elType) {
		if (label.indexOf('(') >= 0)
			return label + " = " + rhs;
		if (isEquationType(elType) && containsRelational(rhs)
				&& !startsWithSimpleVarAssignment(rhs)) {
			String prefix = stripFunctionParams(label);
			if (rhs.startsWith("="))
				return prefix + ":" + rhs;
			return prefix + ": " + rhs;
		}
		return rhs;
	}

	/**
	 * Checks if the RHS starts with a single-letter variable followed by '='
	 * at depth 0 (e.g., "y = 2x + 3"). Such expressions don't need label prefix
	 * because GeoGebra recognizes them as standard coordinate equations.
	 */
	private static boolean startsWithSimpleVarAssignment(String rhs) {
		boolean inString = false;
		int depth = 0;
		for (int i = 0; i < rhs.length(); i++) {
			char c = rhs.charAt(i);
			if (c == '"') { inString = !inString; continue; }
			if (inString) continue;
			if (c == '(' || c == '[' || c == '{') depth++;
			else if (c == ')' || c == ']' || c == '}') depth--;
			else if (depth == 0 && c == '=') {
				String lhs = rhs.substring(0, i).trim();
				return lhs.length() == 1 && Character.isLetter(lhs.charAt(0));
			}
		}
		return false;
	}

	private static boolean isEquationType(String elType) {
		if (elType == null) return true;
		switch (elType) {
			case "conic": case "conic3d":
			case "line": case "line3d":
			case "plane3d":
			case "implicitpoly":
			case "implicitsurface3d":
			case "quadric": case "quadricpart": case "quadriclimited":
				return true;
			default:
				return false;
		}
	}

	private static boolean containsRelational(String s) {
		if (s == null) return false;
		boolean inString = false;
		int depth = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '"') { inString = !inString; continue; }
			if (inString) continue;
			if (c == '(' || c == '[' || c == '{') depth++;
			else if (c == ')' || c == ']' || c == '}') depth--;
			else if (depth == 0 && (c == '=' || c == '<' || c == '>'))
				return true;
		}
		return false;
	}

	/**
	 * Derive the XML {@code <expression type="...">} value from GPAD element type.
	 */
	static String deriveExprType(String elementType) {
		if (elementType == null) return null;
		switch (elementType) {
			case "point": case "point3d": return "point";
			case "vector": case "vector3d": return "vector";
			case "line": case "line3d": return "line";
			case "plane3d": return "plane";
			case "conic": case "conic3d": return "conic";
			case "quadric": case "quadricpart": case "quadriclimited": return "quadric";
			case "implicitpoly": return "implicitpoly";
			case "implicitsurface3d": return "implicitsurface";
			default: return null;
		}
	}

	private static String stripFunctionParams(String label) {
		int p = label.indexOf('(');
		return p >= 0 ? label.substring(0, p) : label;
	}

	// ============================================================
	// INDEPENDENT:  point A @s1 = (1, 2);
	// ============================================================

	private static void emitIndependent(XMLStringBuilder sb, GpadStaticItem item) {
		if (item.labels.isEmpty()) return;

		GpadStaticItem.TypedLabel tl = item.labels.get(0);
		String elType = tl.elementType;
		String label = tl.label;
		if (label.startsWith("OriginalEmpty_"))
			label = "";

		sb.startOpeningTag("element", 1);
		sb.attr("type", elType != null ? elType : "numeric");
		sb.attr("label", label);
		sb.endTag();

		// Collect extra attrs from style sheets for the primary tag (e.g., value.random)
		String primaryTag = getPrimaryXmlTag(elType);
		LinkedHashMap<String, String> mergedPrimaryAttrs = null;
		if (primaryTag != null) {
			mergedPrimaryAttrs = extractAndRemoveProperty(item.extraData, primaryTag);
			LinkedHashMap<String, String> fromStyle = extractAndRemoveProperty(
					tl.styleSheet, primaryTag);
			if (fromStyle != null) {
				if (mergedPrimaryAttrs == null) mergedPrimaryAttrs = fromStyle;
				else mergedPrimaryAttrs.putAll(fromStyle);
			}
		}

		emitShorthandAsXml(sb, elType, item.shorthandText, mergedPrimaryAttrs);

		if (item.extraData != null)
			buildStyleXml(sb, item.extraData);

		injectShowAttrs(tl.styleSheet, tl.showObject, tl.showLabel);
		if (tl.styleSheet != null)
			buildStyleXml(sb, tl.styleSheet);

		sb.closeTag("element");
	}

	/**
	 * Converts shorthand text to the corresponding XML child element(s)
	 * based on element type.
	 */
	private static String getPrimaryXmlTag(String elType) {
		if (elType == null) return null;
		switch (elType) {
			case "point": case "point3d":
			case "vector": case "vector3d":
			case "line": case "line3d": case "plane3d":
				return "coords";
			case "numeric": case "angle": case "angle3d": case "boolean": case "text":
				return "value";
			case "button":
				return "caption";
			case "image":
				return "file";
			default: return null;
		}
	}

	private static LinkedHashMap<String, String> extractAndRemoveProperty(
			GpadStyleSheet sheet, String tagName) {
		if (sheet == null) return null;
		Map<String, LinkedHashMap<String, String>> props = sheet.getProperties();
		if (props == null) return null;
		return props.remove(tagName);
	}

	private static void emitExtraAttrs(XMLStringBuilder sb,
			LinkedHashMap<String, String> extra) {
		if (extra == null) return;
		for (Map.Entry<String, String> e : extra.entrySet()) {
			if (!"~".equals(e.getKey()))
				sb.attr(e.getKey(), e.getValue());
		}
	}

	private static void emitShorthandAsXml(XMLStringBuilder sb, String elType,
			String shorthand, LinkedHashMap<String, String> extraAttrs) {
		if (shorthand == null || shorthand.isEmpty() || elType == null) return;

		switch (elType) {
			case "point": case "point3d":
				emitCoordsFromTuple(sb, shorthand, "1");
				break;
			case "vector": case "vector3d":
				emitCoordsFromTuple(sb, shorthand, "0");
				break;
			case "line": case "line3d": case "plane3d":
				emitCoordsFromTuple(sb, shorthand, null);
				break;
			case "numeric": case "angle": case "angle3d": case "boolean":
				sb.startTag("value", 2);
				sb.attr("val", shorthand);
				emitExtraAttrs(sb, extraAttrs);
				sb.endTag();
				break;
			case "text":
				sb.startTag("value", 2);
				sb.attr("val", unquote(shorthand));
				emitExtraAttrs(sb, extraAttrs);
				sb.endTag();
				break;
			case "button":
				sb.startTag("caption", 2);
				sb.attr("val", unquote(shorthand));
				emitExtraAttrs(sb, extraAttrs);
				sb.endTag();
				break;
			case "image":
				sb.startTag("file", 2);
				sb.attr("name", unquote(shorthand));
				emitExtraAttrs(sb, extraAttrs);
				sb.endTag();
				break;
			default:
				break;
		}
	}

	/**
	 * Parses a tuple like {@code (1, 2)} or {@code (1, 2, 3)} and emits
	 * {@code <coords x="1" y="2" z="..."/>}.
	 *
	 * @param defaultZ default z value if not present in tuple (e.g. "1" for points, "0" for vectors)
	 */
	private static void emitCoordsFromTuple(XMLStringBuilder sb, String tuple, String defaultZ) {
		String inner = tuple.trim();
		if (inner.startsWith("(")) inner = inner.substring(1);
		if (inner.endsWith(")")) inner = inner.substring(0, inner.length() - 1);

		List<String> parts = GpadStaticItem.splitTopLevelComma(inner);
		if (parts.size() < 2) return;

		sb.startTag("coords", 2);
		sb.attr("x", parts.get(0).trim());
		sb.attr("y", parts.get(1).trim());
		if (parts.size() >= 3)
			sb.attr("z", parts.get(2).trim());
		else if (defaultZ != null)
			sb.attr("z", defaultZ);
		if (parts.size() >= 4)
			sb.attr("w", parts.get(3).trim());
		sb.endTag();
	}

	/**
	 * Decodes a shorthand quoted string into its plain-text value.
	 * Handles mixed-quote strings (e.g. {@code "Hello`"`"World"} → {@code Hello"World})
	 * and simple single-delimiter strings ({@code "abc"} → {@code abc}).
	 */
	static String unquote(String s) {
		if (s == null) return "";
		String t = s.trim();
		if (t.isEmpty()) return "";
		if (GpadTypeInferrer.isMixedQuotedString(t)) {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			while (i < t.length()) {
				char delim = t.charAt(i);
				int end = t.indexOf(delim, i + 1);
				if (end < 0) break;
				sb.append(t, i + 1, end);
				i = end + 1;
			}
			return sb.toString();
		}
		return t;
	}

	// ============================================================
	// Shared: emitElement and buildStyleXml
	// ============================================================

	private static void emitElement(XMLStringBuilder sb, String elType,
			String label, GpadStyleSheet styleSheet) {
		emitElement(sb, elType, label, styleSheet, true, true);
	}

	private static void emitElement(XMLStringBuilder sb, String elType,
			String label, GpadStyleSheet styleSheet, boolean showObject, boolean showLabel) {
		sb.startOpeningTag("element", 1);
		sb.attr("type", elType != null ? elType : "numeric");
		sb.attr("label", label);
		sb.endTag();

		injectShowAttrs(styleSheet, showObject, showLabel);
		if (styleSheet != null)
			buildStyleXml(sb, styleSheet);

		sb.closeTag("element");
	}

	/**
	 * Injects show.object/show.label into the stylesheet's show property,
	 * merging with any existing ev bits from the ev: GPAD property.
	 * Always emits object and label when show tag is needed, to match original XML.
	 */
	private static void injectShowAttrs(GpadStyleSheet styleSheet,
			boolean showObject, boolean showLabel) {
		if (styleSheet == null) return;

		LinkedHashMap<String, String> existingShow = styleSheet.getProperty("show");
		boolean needsShow = !showObject || !showLabel || existingShow != null;
		if (!needsShow) return;

		LinkedHashMap<String, String> merged = new LinkedHashMap<>();
		merged.put("object", showObject ? "true" : "false");
		merged.put("label", showLabel ? "true" : "false");
		if (existingShow != null)
			merged.putAll(existingShow);
		styleSheet.setProperty("show", merged);
	}

	private static final Map<String, String> GGB_SCRIPT_PROP_TO_ATTR = Map.of(
			"ggbClick", "val", "ggbUpdate", "onUpdate",
			"ggbDragEnd", "onDragEnd", "ggbChange", "onChange");
	private static final Map<String, String> JS_SCRIPT_PROP_TO_ATTR = Map.of(
			"jsClick", "val", "jsUpdate", "onUpdate",
			"jsDragEnd", "onDragEnd", "jsChange", "onChange");

	/**
	 * Serializes {@link GpadStyleSheet} properties as XML child elements.
	 * Reuses the serialization logic from {@link GpadConstructionXmlBuilder}.
	 */
	private static void buildStyleXml(XMLStringBuilder sb, GpadStyleSheet styleSheet) {
		Map<String, LinkedHashMap<String, String>> properties = styleSheet.getProperties();
		if (properties == null || properties.isEmpty()) return;

		LinkedHashMap<String, String> ggbScriptAttrs = null;
		LinkedHashMap<String, String> jsScriptAttrs = null;
		java.util.Set<String> emittedTags = new java.util.HashSet<>();

		for (Map.Entry<String, LinkedHashMap<String, String>> entry : properties.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();
			if (attrs == null) continue;

			emittedTags.add(tagName);

			if (GGB_SCRIPT_PROP_TO_ATTR.containsKey(tagName)) {
				if (ggbScriptAttrs == null) ggbScriptAttrs = new LinkedHashMap<>();
				String xmlAttr = GGB_SCRIPT_PROP_TO_ATTR.get(tagName);
				String val = attrs.get("val");
				ggbScriptAttrs.put(xmlAttr, val != null ? val : "");
				continue;
			}
			if (JS_SCRIPT_PROP_TO_ATTR.containsKey(tagName)) {
				if (jsScriptAttrs == null) jsScriptAttrs = new LinkedHashMap<>();
				String xmlAttr = JS_SCRIPT_PROP_TO_ATTR.get(tagName);
				String val = attrs.get("val");
				jsScriptAttrs.put(xmlAttr, val != null ? val : "");
				continue;
			}

			if ("startPoint".equals(tagName) && attrs.containsKey("_corners")) {
				buildStartPointXml(sb, attrs.get("_corners"));
				continue;
			}

			if ("barTag".equals(tagName) && attrs.containsKey("_barTags")) {
				buildBarTagXml(sb, attrs.get("_barTags"));
				continue;
			}

			sb.startTag(tagName, 2);
			for (Map.Entry<String, String> attr : attrs.entrySet()) {
				if ("~".equals(attr.getKey())) continue;
				sb.attr(attr.getKey(), attr.getValue());
			}
			sb.endTag();
		}

		for (String tag : GpadStyleDefaults.ALWAYS_APPLY_TAGS) {
			if (emittedTags.contains(tag)) continue;
			LinkedHashMap<String, String> defaults = GpadStyleDefaults.getDefaultAttrs(tag);
			if (defaults == null) continue;
			sb.startTag(tag, 2);
			for (Map.Entry<String, String> attr : defaults.entrySet())
				sb.attr(attr.getKey(), attr.getValue());
			sb.endTag();
		}

		if (ggbScriptAttrs != null) {
			sb.startTag("ggbscript", 2);
			for (Map.Entry<String, String> attr : ggbScriptAttrs.entrySet())
				sb.attr(attr.getKey(), attr.getValue());
			sb.endTag();
		}
		if (jsScriptAttrs != null) {
			sb.startTag("javascript", 2);
			for (Map.Entry<String, String> attr : jsScriptAttrs.entrySet())
				sb.attr(attr.getKey(), attr.getValue());
			sb.endTag();
		}
	}

	private static void buildStartPointXml(XMLStringBuilder sb, String serialized) {
		if (serialized == null || serialized.isEmpty()) return;

		int[] cornerIndex = {0};
		GpadSerializer.deserializeStartPointCorners(serialized, (firstCorner, isAbsolute, cornerData) -> {
			String exp = cornerData[0];
			boolean hasCoords = cornerData[1] != null && cornerData[2] != null;
			if (exp == null && !hasCoords) {
				cornerIndex[0]++;
				return;
			}

			sb.startTag("startPoint", 2);
			if (isAbsolute)
				sb.attr("absolute", "true");

			if (exp != null) {
				sb.attr("exp", exp);
			} else {
				sb.attr("x", cornerData[1]);
				sb.attr("y", cornerData[2]);
				if (cornerData[3] != null)
					sb.attr("z", cornerData[3]);
			}

			sb.attr("number", String.valueOf(cornerIndex[0]++));
			sb.endTag();
		});
	}

	// ============================================================
	// MACRO: @@macro Name(inputs) { body @@return outputs }
	// ============================================================

	/**
	 * Builds a complete {@code geogebra_macro.xml}-style XML fragment for all
	 * MACRO items in the static item list.
	 *
	 * @param items list of parsed static items (may contain zero or more MACRO items)
	 * @return macro XML string, or {@code null} if no macros are present
	 */
	public static String buildMacroXml(List<GpadStaticItem> items) {
		boolean hasMacros = false;
		for (GpadStaticItem item : items) {
			if (item.type == GpadStaticItem.Type.MACRO) {
				hasMacros = true;
				break;
			}
		}
		if (!hasMacros) return null;

		XMLStringBuilder sb = new XMLStringBuilder();
		MyXMLio.addXMLHeader(sb);
		addStaticGeoGebraHeader(sb, true, GeoGebraConstants.GEOMETRY_APPCODE, null);
		for (GpadStaticItem item : items) {
			if (item.type == GpadStaticItem.Type.MACRO)
				emitMacro(sb, item);
		}
		sb.closeTag("geogebra");
		return sb.toString();
	}

	private static void emitMacro(XMLStringBuilder sb, GpadStaticItem item) {
		sb.startOpeningTag("macro", 0);
		sb.attr("cmdName", item.macroName);
		sb.attr("toolName", getMeta(item, "toolName", item.macroName));
		sb.attr("toolHelp", getMeta(item, "toolHelp", ""));
		sb.attr("iconFile", getMeta(item, "iconFile", ""));
		sb.attr("showInToolBar", !"false".equals(getMeta(item, "showInToolBar", "true")));
		sb.attr("copyCaptions", !"false".equals(getMeta(item, "copyCaptions", "true")));
		String viewName = getMeta(item, "view", null);
		if (viewName != null)
			sb.attr("viewId", GpadStyleMaps.viewNameToId(viewName));
		sb.endTag();

		if (item.macroInputLabels != null && !item.macroInputLabels.isEmpty()) {
			sb.startTag("macroInput", 1);
			for (int i = 0; i < item.macroInputLabels.size(); i++)
				sb.attr("a" + i, item.macroInputLabels.get(i));
			sb.endTag();
		}

		if (item.macroOutputLabels != null && !item.macroOutputLabels.isEmpty()) {
			sb.startTag("macroOutput", 1);
			for (int i = 0; i < item.macroOutputLabels.size(); i++)
				sb.attr("a" + i, item.macroOutputLabels.get(i));
			sb.endTag();
		}

		if (item.macroBodyItems != null && !item.macroBodyItems.isEmpty()) {
			String constructionXml = buildConstructionXml(item.macroBodyItems);
			sb.append(new XMLStringBuilder(new StringBuilder(constructionXml)));
		}

		sb.closeTag("macro");
	}

	private static String getMeta(GpadStaticItem item, String key, String defaultValue) {
		if (item.macroMeta != null && item.macroMeta.containsKey(key))
			return item.macroMeta.get(key);
		return defaultValue;
	}

	private static void buildBarTagXml(XMLStringBuilder sb, String serialized) {
		if (serialized == null || serialized.isEmpty()) return;

		GpadSerializer.deserializeBarTags(serialized, (barNumber, rgba, fillTypeXML, hatchAngle,
				hatchDistance, image, fillSymbol) -> {
			if (rgba[0] >= 0 && rgba[1] >= 0 && rgba[2] >= 0) {
				sb.startTag("tag", 2);
				sb.attr("key", "barColor");
				sb.attr("barNumber", barNumber);
				sb.attr("value", "rgba(" + rgba[0] + "," + rgba[1] + "," + rgba[2] + ",1)");
				sb.endTag();
			}
			if (rgba[3] >= 0) {
				double alpha = rgba[3] / 255.0;
				sb.startTag("tag", 2);
				sb.attr("key", "barAlpha");
				sb.attr("barNumber", barNumber);
				sb.attr("value", String.valueOf(alpha));
				sb.endTag();
			}
			if (fillTypeXML != null) {
				sb.startTag("tag", 2);
				sb.attr("key", "barFillType");
				sb.attr("barNumber", barNumber);
				sb.attr("value", fillTypeXML);
				sb.endTag();
			}
			if (hatchAngle != null) {
				sb.startTag("tag", 2);
				sb.attr("key", "barHatchAngle");
				sb.attr("barNumber", barNumber);
				sb.attr("value", hatchAngle);
				sb.endTag();
			}
			if (hatchDistance != null) {
				sb.startTag("tag", 2);
				sb.attr("key", "barHatchDistance");
				sb.attr("barNumber", barNumber);
				sb.attr("value", hatchDistance);
				sb.endTag();
			}
			if (image != null) {
				sb.startTag("tag", 2);
				sb.attr("key", "barImage");
				sb.attr("barNumber", barNumber);
				sb.attr("value", image);
				sb.endTag();
			}
			if (fillSymbol != null) {
				sb.startTag("tag", 2);
				sb.attr("key", "barSymbol");
				sb.attr("barNumber", barNumber);
				sb.attr("value", fillSymbol);
				sb.endTag();
			}
		});
	}
}
