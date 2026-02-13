package org.geogebra.common.gpad;

import java.util.LinkedHashMap;
import java.util.List;

import org.geogebra.common.util.debug.Log;

/**
 * Converts {@link XmlSettingsCollector} data (captured from GeoGebra XML)
 * into GPAD @@env statements.  These statements are appended inside the
 * existing @@env { ... } block produced by
 * {@link EuclidianSettingsToGpadConverter}.
 *
 * The output is a series of indented lines (no surrounding @@env { } braces)
 * that get inserted before the closing brace.
 */
public class XmlSettingsToGpadEnvConverter {

	private XmlSettingsToGpadEnvConverter() {
	}

	/**
	 * Converts collected settings to GPAD env content lines.
	 *
	 * @param collector the collected XML settings
	 * @return GPAD lines to append inside @@env block, or empty string
	 */
	public static String convert(XmlSettingsCollector collector) {
		if (collector == null || !collector.hasAnySettings()) return "";

		StringBuilder sb = new StringBuilder();

		if (collector.hasKernel())
			convertKernel(sb, collector.getKernelChildren());
		if (collector.hasGui())
			convertGui(sb, collector.getGuiChildren());
		if (collector.hasAlgebraView()) {
			convertAlgebraView(sb, collector.getAlgebraViewChildren(),
					collector.getKernelChildren());
		} else if (collector.hasKernel()
				&& findAlgebraStyleVal(collector.getKernelChildren()) != null
				&& !GpadStyleDefaults.ENV_AV_STYLE.equals(findAlgebraStyleVal(collector.getKernelChildren()))) {
			convertAlgebraView(sb, java.util.Collections.emptyList(),
					collector.getKernelChildren());
		}
		if (collector.hasSpreadsheetView()) {
			convertSpreadsheetView(sb, collector.getSpreadsheetViewAttrs(),
					collector.getSpreadsheetViewChildren(),
					collector.getKernelChildren());
		} else if (collector.hasKernel()) {
			String ssVal = findSpreadsheetStyleVal(collector.getKernelChildren());
			if (ssVal != null && !GpadStyleDefaults.ENV_AV_STYLE.equals(ssVal)) {
				convertSpreadsheetView(sb, new java.util.LinkedHashMap<>(),
						java.util.Collections.emptyList(),
						collector.getKernelChildren());
			}
		}
		if (collector.hasKeyboard())
			convertKeyboard(sb, collector.getKeyboardAttrs());
		if (collector.hasProbCalc())
			convertProbCalc(sb, collector.getProbCalcChildren());
		if (collector.hasScripting())
			convertScripting(sb, collector.getScriptingAttrs());
		if (collector.hasTableview())
			convertTableview(sb, collector.getTableviewAttrs());

		String rightAngleStyle = extractRightAngleStyle(collector);
		if (rightAngleStyle != null)
			sb.append("  rightAngleStyle: ").append(rightAngleStyle).append(";\n");

		if (collector.hasEv1())
			convertEvBlock(sb, "ev1", collector.getEv1Children());
		if (collector.hasEv2())
			convertEvBlock(sb, "ev2", collector.getEv2Children());
		if (collector.hasEv3d())
			convertEv3dBlock(sb, collector.getEv3dChildren());

		if (collector.hasPerspective())
			convertPerspective(sb, collector);

		return sb.toString();
	}

	// ===================== kernel -> top-level =====================

	private static void convertKernel(StringBuilder sb,
			List<XmlSettingsCollector.TagAttrs> children) {
		for (XmlSettingsCollector.TagAttrs ta : children) {
			switch (ta.tag) {
				case "continuous":
					emitBool(sb, "continuous", ta.attrs.get("val")); break;
				case "symbolic":
					emitBoolIfTrue(sb, "symbolic", ta.attrs.get("val")); break;
				case "decimals":
					sb.append("  precision: ").append(ta.attrs.get("val"))
							.append("dp;\n");
					break;
				case "significantfigures":
					sb.append("  precision: ").append(ta.attrs.get("val"))
							.append("sf;\n");
					break;
				case "angleUnit":
					emitAngleUnit(sb, ta.attrs.get("val")); break;
				case "coordStyle":
					emitCoordStyle(sb, ta.attrs.get("val")); break;
				case "startAnimation":
					emitBoolIfTrue(sb, "startAnimation", ta.attrs.get("val"));
					break;
			case "usePathAndRegionParameters":
				String prp = ta.attrs.get("val");
				if (prp != null && !String.valueOf(GpadStyleDefaults.ENV_PATH_REGION_PARAMS).equals(prp))
					sb.append("  pathRegionParams: ").append(prp).append(";\n");
					break;
				case "localization":
					emitLocalization(sb, ta.attrs); break;
				case "casSettings":
					emitCas(sb, ta.attrs); break;
				case "algebraStyle":
					// handled inside algebraView / spreadsheetView
					break;
			case "uses3D":
				break;
			case "angleFromInvTrig":
				if ("true".equals(ta.attrs.get("val")))
					Log.warn("angleFromInvTrig=true (pre-v5.0.290 legacy) not preserved in GPAD");
				break;
			default:
				break;
			}
		}
	}

	private static void emitAngleUnit(StringBuilder sb, String xmlVal) {
		if (xmlVal == null || GpadStyleDefaults.ENV_ANGLE_UNIT.equals(xmlVal)) return;
		String gpadVal;
		switch (xmlVal) {
			case "radiant": gpadVal = "radian"; break;
			case "degreesMinutesSeconds": gpadVal = "dms"; break;
			default: gpadVal = xmlVal; break;
		}
		sb.append("  angleUnit: ").append(gpadVal).append(";\n");
	}

	private static void emitCoordStyle(StringBuilder sb, String xmlVal) {
		if (xmlVal == null) return;
		String gpadVal;
		switch (xmlVal) {
			case "0": gpadVal = "default"; break;
			case "1": gpadVal = "austrian"; break;
			case "2": gpadVal = "french"; break;
			default: return;
		}
		if ("default".equals(gpadVal)) return;
		sb.append("  coordStyle: ").append(gpadVal).append(";\n");
	}

	private static void emitLocalization(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		String digits = attrs.get("digits");
		String labels = attrs.get("labels");
		if (digits == null && labels == null) return;
		sb.append("  localization:");
		if ("true".equals(digits)) sb.append(" digits");
		else sb.append(" ~digits");
		if ("true".equals(labels)) sb.append(" labels");
		else sb.append(" ~labels");
		sb.append(";\n");
	}

	private static void emitCas(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		String timeout = attrs.get("timeout");
		String expRoots = attrs.get("expRoots");
		boolean hasNonDefault = (timeout != null
						&& !String.valueOf(GpadStyleDefaults.ENV_CAS_TIMEOUT).equals(timeout))
				|| (expRoots != null
						&& !String.valueOf(GpadStyleDefaults.ENV_CAS_EXP_ROOTS).equals(expRoots));
		if (!hasNonDefault) return;
		sb.append("  cas:");
		if (timeout != null) sb.append(" timeout=").append(timeout);
		if ("true".equals(expRoots)) sb.append(" rootForm");
		else sb.append(" ~rootForm");
		sb.append(";\n");
	}

	// ===================== gui -> top-level =====================

	private static void convertGui(StringBuilder sb,
			List<XmlSettingsCollector.TagAttrs> children) {
		for (XmlSettingsCollector.TagAttrs ta : children) {
			switch (ta.tag) {
				case "font":
					String size = ta.attrs.get("size");
					if (size != null && !"0".equals(size))
						sb.append("  font: ").append(size).append(";\n");
					break;
				case "labelingStyle":
					emitLabeling(sb, ta.attrs.get("val")); break;
			}
		}
	}

	private static void emitLabeling(StringBuilder sb, String xmlVal) {
		if (xmlVal == null) return;
		String gpadVal;
		switch (xmlVal) {
			case "0": gpadVal = "automatic"; break;
			case "1": gpadVal = "alwaysOn"; break;
			case "2": gpadVal = "alwaysOff"; break;
			case "3": gpadVal = "pointsOnly"; break;
			case "4": gpadVal = "useDefaults"; break;
			default: return;
		}
		sb.append("  labeling: ").append(gpadVal).append(";\n");
	}

	// ===================== algebraView =====================

	private static void convertAlgebraView(StringBuilder sb,
			List<XmlSettingsCollector.TagAttrs> avChildren,
			List<XmlSettingsCollector.TagAttrs> kernelChildren) {
		String sortMode = null;
		boolean auxiliary = false;

		for (XmlSettingsCollector.TagAttrs ta : avChildren) {
			switch (ta.tag) {
				case "mode":
					sortMode = sortModeToString(ta.attrs.get("val")); break;
				case "auxiliary":
					auxiliary = "true".equals(ta.attrs.get("show")); break;
			}
		}

		String styleVal = findAlgebraStyleVal(kernelChildren);

		boolean hasNonDefault = (sortMode != null && !GpadStyleDefaults.ENV_AV_SORT_MODE.equals(sortMode))
				|| auxiliary
				|| (styleVal != null && !GpadStyleDefaults.ENV_AV_STYLE.equals(styleVal));
		if (!hasNonDefault) return;

		sb.append("  algebraView:");
		if (sortMode != null) sb.append(" sort=").append(sortMode);
		if (styleVal != null) sb.append(" style=").append(styleVal);
		if (auxiliary) sb.append(" auxiliary");
		sb.append(";\n");
	}

	private static String findAlgebraStyleVal(
			List<XmlSettingsCollector.TagAttrs> kernelChildren) {
		for (XmlSettingsCollector.TagAttrs ta : kernelChildren) {
			if ("algebraStyle".equals(ta.tag))
				return algebraStyleToString(ta.attrs.get("val"));
		}
		return null;
	}

	private static String findSpreadsheetStyleVal(
			List<XmlSettingsCollector.TagAttrs> kernelChildren) {
		for (XmlSettingsCollector.TagAttrs ta : kernelChildren) {
			if ("algebraStyle".equals(ta.tag))
				return algebraStyleToString(ta.attrs.get("spreadsheet"));
		}
		return null;
	}

	private static String sortModeToString(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "dependency";
			case "1": return "type";
			case "2": return "layer";
			case "3": return "order";
			default: return null;
		}
	}

	private static String algebraStyleToString(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "value";
			case "1": return "description";
			case "2": return "definition";
			case "3": return "definitionAndValue";
			case "4": return "linearNotation";
			default: return null;
		}
	}

	// ===================== spreadsheetView =====================

	private static void convertSpreadsheetView(StringBuilder sb,
			LinkedHashMap<String, String> topAttrs,
			List<XmlSettingsCollector.TagAttrs> children,
			List<XmlSettingsCollector.TagAttrs> kernelChildren) {
		StringBuilder inner = new StringBuilder();

		String ssStyle = findSpreadsheetStyleVal(kernelChildren);
		if (ssStyle != null && !GpadStyleDefaults.ENV_AV_STYLE.equals(ssStyle))
			inner.append("    style: ").append(ssStyle).append(";\n");

		for (XmlSettingsCollector.TagAttrs ta : children) {
			switch (ta.tag) {
				case "size":
					appendKV(inner, "size", ta.attrs.get("width"), ta.attrs.get("height"));
					break;
				case "prefCellSize": {
					String cw = ta.attrs.get("width");
					String ch = ta.attrs.get("height");
					if (cw != null || ch != null)
						inner.append("    cellSize: ").append(cw != null ? cw : "0")
								.append(" ").append(ch != null ? ch : "0").append(";\n");
					break;
				}
				case "spreadsheetDimensions":
					if (ta.attrs.get("rows") != null)
						inner.append("    rows: ").append(ta.attrs.get("rows")).append(";\n");
					if (ta.attrs.get("columns") != null)
						inner.append("    columns: ").append(ta.attrs.get("columns")).append(";\n");
					break;
				case "spreadsheetColumn":
					appendColumnLine(inner, children); break;
				case "layout":
					appendLayout(inner, ta.attrs); break;
				case "spreadsheetCellFormat":
					String fmt = ta.attrs.get("formatMap");
					if (fmt != null && !fmt.isEmpty())
						inner.append("    cellFormat: \"").append(fmt).append("\";\n");
					break;
			}
		}

		if (inner.length() == 0) return;
		sb.append("  spreadsheetView {\n");
		sb.append(inner);
		sb.append("  }\n");
	}

	private static void appendKV(StringBuilder sb, String key, String v1, String v2) {
		if (v1 == null && v2 == null) return;
		sb.append("    ").append(key).append(": ");
		if (v1 != null) sb.append(v1);
		if (v2 != null) sb.append(" ").append(v2);
		sb.append(";\n");
	}

	private static void appendColumnLine(StringBuilder inner,
			List<XmlSettingsCollector.TagAttrs> children) {
		java.util.TreeMap<Integer, String> cols = new java.util.TreeMap<>();
		for (XmlSettingsCollector.TagAttrs ta : children) {
			if ("spreadsheetColumn".equals(ta.tag)) {
				String id = ta.attrs.get("id");
				String width = ta.attrs.get("width");
				if (id != null && width != null) {
					try {
						cols.put(Integer.parseInt(id), width);
					} catch (NumberFormatException ignored) {
					}
				}
			}
		}
		if (cols.isEmpty()) return;
		int maxId = cols.lastKey();
		inner.append("    column:");
		String prevWidth = null;
		for (int i = 0; i <= maxId; i++) {
			String w = cols.get(i);
			if (w == null) {
				inner.append(" default");
			} else if (w.equals(prevWidth)) {
				inner.append(" =");
			} else {
				inner.append(" ").append(w);
			}
			prevWidth = w;
		}
		inner.append(";\n");
	}

	private static void appendLayout(StringBuilder inner,
			LinkedHashMap<String, String> attrs) {
		inner.append("    layout:");
		appendLayoutBool(inner, "grid", attrs.get("showGrid"));
		appendLayoutBool(inner, "formulaBar", attrs.get("showFormulaBar"));
		appendLayoutBool(inner, "hScroll", attrs.get("showHScrollBar"));
		appendLayoutBool(inner, "vScroll", attrs.get("showVScrollBar"));
		appendLayoutBool(inner, "browserPanel", attrs.get("showBrowserPanel"));
		appendLayoutBool(inner, "columnHeader", attrs.get("showColumnHeader"));
		appendLayoutBool(inner, "rowHeader", attrs.get("showRowHeader"));
		appendLayoutBool(inner, "specialEditor", attrs.get("allowSpecialEditor"));
		appendLayoutBool(inner, "toolTips", attrs.get("allowToolTips"));
		appendLayoutBool(inner, "equalsRequired", attrs.get("equalsRequired"));
		inner.append(";\n");
	}

	private static void appendLayoutBool(StringBuilder sb, String name, String val) {
		if (val == null) return;
		if ("true".equals(val)) sb.append(" ").append(name);
		else sb.append(" ~").append(name);
	}

	// ===================== keyboard =====================

	private static void convertKeyboard(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		sb.append("  keyboard:");
		appendAttr(sb, "width", attrs.get("width"));
		appendAttr(sb, "height", attrs.get("height"));
		appendAttr(sb, "opacity", attrs.get("opacity"));
		String lang = attrs.get("language");
		if (lang != null && !lang.isEmpty())
			sb.append(" language=\"").append(lang).append("\"");
		if ("true".equals(attrs.get("show"))) sb.append(" show");
		sb.append(";\n");
	}

	// ===================== probabilityCalculator =====================

	private static void convertProbCalc(StringBuilder sb,
			List<XmlSettingsCollector.TagAttrs> children) {
		StringBuilder inner = new StringBuilder();
		for (XmlSettingsCollector.TagAttrs ta : children) {
			switch (ta.tag) {
				case "distribution":
					inner.append("    distribution:");
					String distName = distTypeToString(ta.attrs.get("type"));
					if (distName != null) inner.append(" ").append(distName);
					appendBoolAttr(inner, "cumulative", ta.attrs.get("isCumulative"));
					appendBoolAttr(inner, "overlayActive",
							ta.attrs.get("isOverlayActive"));
					String params = ta.attrs.get("parameters");
					if (params != null)
						inner.append(" parameters=\"").append(params).append("\"");
					inner.append(";\n");
					break;
				case "interval":
					inner.append("    interval:");
					String mode = intervalModeToString(ta.attrs.get("mode"));
					if (mode != null) inner.append(" ").append(mode);
					appendAttr(inner, "low", ta.attrs.get("low"));
					appendAttr(inner, "high", ta.attrs.get("high"));
					inner.append(";\n");
					break;
			}
		}
		if (inner.length() == 0) return;
		sb.append("  probCalc {\n");
		sb.append(inner);
		sb.append("  }\n");
	}

	private static String distTypeToString(String ordinal) {
		if (ordinal == null) return null;
		switch (ordinal) {
			case "0": return "normal";
			case "1": return "student";
			case "2": return "chiSquare";
			case "3": return "f";
			case "4": return "cauchy";
			case "5": return "exponential";
			case "6": return "beta";
			case "7": return "gamma";
			case "8": return "weibull";
			case "9": return "logistic";
			case "10": return "logNormal";
			case "11": return "binomial";
			case "12": return "pascal";
			case "13": return "hypergeometric";
			case "14": return "poisson";
			default: return ordinal;
		}
	}

	private static String intervalModeToString(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "interval";
			case "1": return "left";
			case "2": return "right";
			case "3": return "twoTailed";
			default: return xmlVal;
		}
	}

	// ===================== scripting =====================

	private static void convertScripting(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		boolean blocked = "true".equals(attrs.get("blocked"));
		boolean disabled = "true".equals(attrs.get("disabled"));
		if (!blocked && !disabled) return;
		sb.append("  scripting:");
		if (blocked) sb.append(" blocked"); else sb.append(" ~blocked");
		if (disabled) sb.append(" disabled"); else sb.append(" ~disabled");
		sb.append(";\n");
	}

	// ===================== tableview =====================

	private static void convertTableview(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		String xValues = attrs.get("xValues");
		if (xValues != null) {
			sb.append("  tableView: xValues=\"").append(xValues).append("\"");
			String xCaption = attrs.get("xCaption");
			if (xCaption != null)
				sb.append(" xCaption=\"").append(xCaption).append("\"");
			sb.append(";\n");
		} else {
			String min = attrs.get("min");
			String max = attrs.get("max");
			String step = attrs.get("step");
			boolean isDefault = isDefaultTableview(min, max, step);
			if (isDefault) return;
			sb.append("  tableView:");
			appendAttr(sb, "min", min);
			appendAttr(sb, "max", max);
			appendAttr(sb, "step", step);
			sb.append(";\n");
		}
	}

	private static boolean isDefaultTableview(String min, String max, String step) {
		return isNumEq(min, GpadStyleDefaults.ENV_TABLE_MIN)
				&& isNumEq(max, GpadStyleDefaults.ENV_TABLE_MAX)
				&& isNumEq(step, GpadStyleDefaults.ENV_TABLE_STEP);
	}

	private static boolean isNumEq(String s, double target) {
		if (s == null) return true;
		try {
			return Math.abs(Double.parseDouble(s) - target) < 0.0001;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	// ===================== helpers =====================

	private static void emitBool(StringBuilder sb, String name, String val) {
		if ("true".equals(val)) sb.append("  ").append(name).append(";\n");
		else if ("false".equals(val)) sb.append("  ~").append(name).append(";\n");
	}

	private static void emitBoolIfTrue(StringBuilder sb, String name, String val) {
		if ("true".equals(val)) sb.append("  ").append(name).append(";\n");
	}

	private static void appendAttr(StringBuilder sb, String key, String val) {
		if (val != null) sb.append(" ").append(key).append("=").append(val);
	}

	private static void appendBoolAttr(StringBuilder sb, String name, String val) {
		if ("true".equals(val)) sb.append(" ").append(name);
		else if ("false".equals(val)) sb.append(" ~").append(name);
	}

	// ===================== euclidianView =====================

	private static String extractRightAngleStyle(XmlSettingsCollector collector) {
		java.util.List<XmlSettingsCollector.TagAttrs> evChildren = null;
		if (collector.hasEv1()) evChildren = collector.getEv1Children();
		else if (collector.hasEv2()) evChildren = collector.getEv2Children();
		if (evChildren == null) return null;
		for (XmlSettingsCollector.TagAttrs ta : evChildren) {
			if ("evSettings".equals(ta.tag)) {
				String val = ta.attrs.get("rightAngleStyle");
				return rightAngleStyleToGpad(val);
			}
		}
		return null;
	}

	private static String rightAngleStyleToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "none";
			case "1": return "square";
			case "2": return "dot";
			case "3": return "L";
			default: return null;
		}
	}

	private static void convertEvBlock(StringBuilder sb, String evName,
			java.util.List<XmlSettingsCollector.TagAttrs> children) {
		StringBuilder inner = new StringBuilder();

		LinkedHashMap<String, String> evSettings = null;
		LinkedHashMap<String, String> coordSystem = null;
		LinkedHashMap<String, String> lineStyle = null;
		LinkedHashMap<String, String> labelStyle = null;
		LinkedHashMap<String, String> gridAttrs = null;
		LinkedHashMap<String, String> bgColor = null;
		LinkedHashMap<String, String> axesColor = null;
		LinkedHashMap<String, String> gridColor = null;
		LinkedHashMap<String, String> rulerColor = null;
		LinkedHashMap<String, String> sizeAttrs = null;
		String rulerTypeVal = null;
		java.util.Map<Integer, LinkedHashMap<String, String>> axisMap = new java.util.TreeMap<>();

		for (XmlSettingsCollector.TagAttrs ta : children) {
			switch (ta.tag) {
				case "evSettings": evSettings = ta.attrs; break;
				case "coordSystem": coordSystem = ta.attrs; break;
				case "lineStyle": lineStyle = ta.attrs; break;
				case "labelStyle": labelStyle = ta.attrs; break;
				case "grid": gridAttrs = ta.attrs; break;
				case "bgColor": bgColor = ta.attrs; break;
				case "axesColor": axesColor = ta.attrs; break;
				case "gridColor": gridColor = ta.attrs; break;
				case "rulerColor": rulerColor = ta.attrs; break;
				case "rulerType": rulerTypeVal = ta.attrs.get("val"); break;
				case "size": sizeAttrs = ta.attrs; break;
				case "axis":
					String idStr = ta.attrs.get("id");
					if (idStr != null) {
						try {
							axisMap.put(Integer.parseInt(idStr), ta.attrs);
						} catch (NumberFormatException ignored) { }
					}
					break;
			}
		}

		emitColorProp(inner, "bgColor", bgColor);
		emitColorProp(inner, "axesColor", axesColor);
		emitColorProp(inner, "gridColor", gridColor);

		if (sizeAttrs != null) {
			String w = sizeAttrs.get("width"), h = sizeAttrs.get("height");
			if (w != null && h != null)
				inner.append("    size: ").append(w).append(",").append(h).append(";\n");
		}

		emitCoordSystem2D(inner, coordSystem);
		emitEvSettingsProps(inner, evSettings);

		emitAxisProp(inner, "xAxis", axisMap.get(0));
		emitAxisProp(inner, "yAxis", axisMap.get(1));

		emitGridDist(inner, gridAttrs);
		emitLineAndAxesStyle(inner, lineStyle);
		emitLabelStyleProp(inner, labelStyle);

		if (rulerTypeVal != null) {
			String gpad = rulerTypeIntToGpad(rulerTypeVal);
			if (gpad != null && !"none".equals(gpad))
				inner.append("    rulerType: ").append(gpad).append(";\n");
		}
		emitColorProp(inner, "rulerColor", rulerColor);

		if (inner.length() == 0) return;
		sb.append("  ").append(evName).append(" {\n");
		sb.append(inner);
		sb.append("  }\n");
	}

	private static void convertEv3dBlock(StringBuilder sb,
			java.util.List<XmlSettingsCollector.TagAttrs> children) {
		StringBuilder inner = new StringBuilder();

		LinkedHashMap<String, String> evSettings = null;
		LinkedHashMap<String, String> coordSystem = null;
		LinkedHashMap<String, String> bgColor = null;
		LinkedHashMap<String, String> lineStyle = null;
		LinkedHashMap<String, String> labelStyle = null;
		java.util.Map<Integer, LinkedHashMap<String, String>> axisMap = new java.util.TreeMap<>();
		boolean hasPlane = false, lightVal = true, lightExplicit = false;
		boolean yAxisUp = false, coloredAxes = false;
		LinkedHashMap<String, String> clippingAttrs = null;
		LinkedHashMap<String, String> projectionAttrs = null;

		for (XmlSettingsCollector.TagAttrs ta : children) {
			switch (ta.tag) {
				case "evSettings": evSettings = ta.attrs; break;
				case "coordSystem": coordSystem = ta.attrs; break;
				case "bgColor": bgColor = ta.attrs; break;
				case "lineStyle": lineStyle = ta.attrs; break;
				case "labelStyle": labelStyle = ta.attrs; break;
				case "plate": hasPlane = "true".equals(ta.attrs.get("show")); break;
				case "light":
					lightExplicit = true;
					lightVal = "true".equals(ta.attrs.get("val"));
					break;
				case "yAxisVertical": yAxisUp = "true".equals(ta.attrs.get("val")); break;
				case "coloredAxes": coloredAxes = "true".equals(ta.attrs.get("val")); break;
				case "clipping": clippingAttrs = ta.attrs; break;
				case "projection": projectionAttrs = ta.attrs; break;
				case "axis":
					String idStr = ta.attrs.get("id");
					if (idStr != null) {
						try { axisMap.put(Integer.parseInt(idStr), ta.attrs); }
						catch (NumberFormatException ignored) { }
					}
					break;
			}
		}

		emitColorProp(inner, "bgColor", bgColor);
		emitCoordSystem3D(inner, coordSystem);
		emitEvSettingsProps(inner, evSettings);

		if (hasPlane) inner.append("    plane;\n");
		if (lightExplicit && !lightVal) inner.append("    ~light;\n");
		else if (lightExplicit && lightVal) inner.append("    light;\n");
		if (yAxisUp) inner.append("    yAxisUp;\n");
		if (coloredAxes) inner.append("    coloredAxes;\n");

		if (clippingAttrs != null) {
			boolean clUse = "true".equals(clippingAttrs.get("use"));
			boolean clShow = "true".equals(clippingAttrs.get("show"));
			if (clUse || clShow) {
				inner.append("    clipping:");
				if (clUse) inner.append(" use");
				else inner.append(" ~use");
				if (clShow) inner.append(" show");
				String size = clippingAttrs.get("size");
				if (size != null && !"0".equals(size)) inner.append(" size=").append(size);
				inner.append(";\n");
			}
		}

		emitProjectionProp(inner, projectionAttrs);
		emitLineAndAxesStyle(inner, lineStyle);
		emitLabelStyleProp(inner, labelStyle);

		emitAxisProp(inner, "xAxis", axisMap.get(0));
		emitAxisProp(inner, "yAxis", axisMap.get(1));
		emitAxisProp(inner, "zAxis", axisMap.get(2));

		if (inner.length() == 0) return;
		sb.append("  ev3d {\n");
		sb.append(inner);
		sb.append("  }\n");
	}

	private static void emitColorProp(StringBuilder sb, String name,
			LinkedHashMap<String, String> rgb) {
		if (rgb == null) return;
		String r = rgb.get("r"), g = rgb.get("g"), b = rgb.get("b");
		if (r == null || g == null || b == null) return;
		try {
			int ri = Integer.parseInt(r), gi = Integer.parseInt(g), bi = Integer.parseInt(b);
			sb.append("    ").append(name).append(": #")
					.append(hex2(ri)).append(hex2(gi)).append(hex2(bi));
			String a = rgb.get("alpha");
			if (a != null) {
				double alpha = Double.parseDouble(a);
				if (alpha < 0.999) sb.append(hex2((int) Math.round(alpha * 255)));
			}
			sb.append(";\n");
		} catch (NumberFormatException ignored) { }
	}

	private static String hex2(int v) {
		v = Math.max(0, Math.min(255, v));
		String h = Integer.toHexString(v).toUpperCase();
		return h.length() < 2 ? "0" + h : h;
	}

	private static void emitCoordSystem2D(StringBuilder sb,
			LinkedHashMap<String, String> cs) {
		if (cs == null) return;
		String xMin = cs.get("xMin"), xMax = cs.get("xMax");
		String yMin = cs.get("yMin"), yMax = cs.get("yMax");
		if (xMin != null && xMax != null && yMin != null && yMax != null) {
			sb.append("    coordSystem: xMin=").append(quoteExpr(xMin))
					.append(" xMax=").append(quoteExpr(xMax))
					.append(" yMin=").append(quoteExpr(yMin))
					.append(" yMax=").append(quoteExpr(yMax)).append(";\n");
			return;
		}
		String xZero = cs.get("xZero"), yZero = cs.get("yZero");
		String scale = cs.get("scale"), yscale = cs.get("yscale");
		if (xZero == null && scale == null) return;
		sb.append("    coordSystem:");
		if (xZero != null || yZero != null)
			sb.append(" origin=").append(nvl(xZero, "0")).append(",").append(nvl(yZero, "0"));
		if (scale != null && yscale != null && scale.equals(yscale))
			sb.append(" scale=").append(scale);
		else {
			if (scale != null) sb.append(" xscale=").append(scale);
			if (yscale != null) sb.append(" yscale=").append(yscale);
		}
		sb.append(";\n");
	}

	private static void emitCoordSystem3D(StringBuilder sb,
			LinkedHashMap<String, String> cs) {
		if (cs == null) return;
		String xMin = cs.get("xMin"), xMax = cs.get("xMax");
		String yMin = cs.get("yMin"), yMax = cs.get("yMax");
		String zMin = cs.get("zMin"), zMax = cs.get("zMax");
		String xZero = cs.get("xZero"), yZero = cs.get("yZero"), zZero = cs.get("zZero");
		String scale = cs.get("scale"), yscale = cs.get("yscale"), zscale = cs.get("zscale");
		String xAngle = cs.get("xAngle"), zAngle = cs.get("zAngle");

		boolean hasBounded = xMin != null && xMax != null && yMin != null
				&& yMax != null && zMin != null && zMax != null;
		boolean hasOrigin = !isZero(xZero) || !isZero(yZero) || !isZero(zZero);
		boolean hasScale = !isDefaultScale(scale) || !isDefaultScale(yscale)
				|| !isDefaultScale(zscale);
		boolean hasAngle = xAngle != null || zAngle != null;
		if (!hasBounded && !hasOrigin && !hasScale && !hasAngle) return;

		sb.append("    coordSystem:");
		if (hasOrigin)
			sb.append(" origin=").append(nvl(xZero, "0")).append(",")
					.append(nvl(yZero, "0")).append(",").append(nvl(zZero, "0"));
		if (hasScale) {
			boolean allSame = eq(scale, yscale) && eq(scale, zscale);
			if (allSame && scale != null)
				sb.append(" scale=").append(scale);
			else {
				if (scale != null) sb.append(" xscale=").append(scale);
				if (yscale != null) sb.append(" yscale=").append(yscale);
				if (zscale != null) sb.append(" zscale=").append(zscale);
			}
		}
		if (xAngle != null) sb.append(" xAngle=").append(xAngle);
		if (zAngle != null) sb.append(" zAngle=").append(zAngle);
		if (hasBounded) {
			sb.append(" xMin=").append(quoteExpr(xMin))
					.append(" xMax=").append(quoteExpr(xMax))
					.append(" yMin=").append(quoteExpr(yMin))
					.append(" yMax=").append(quoteExpr(yMax))
					.append(" zMin=").append(quoteExpr(zMin))
					.append(" zMax=").append(quoteExpr(zMax));
		}
		sb.append(";\n");
	}

	private static void emitEvSettingsProps(StringBuilder sb,
			LinkedHashMap<String, String> es) {
		if (es == null) return;

		if ("false".equals(es.get("axes")))
			sb.append("    ~axes;\n");
		if ("true".equals(es.get("grid")))
			sb.append("    grid;\n");
		if ("true".equals(es.get("gridIsBold")))
			sb.append("    gridBold;\n");

		String gridType = es.get("gridType");
		if (gridType != null) {
			String gpad = gridTypeToGpad(gridType);
			if (gpad != null && !"cartesianSub".equals(gpad))
				sb.append("    gridType: ").append(gpad).append(";\n");
		}

		String pc = es.get("pointCapturing");
		if (pc != null) {
			String gpad = pointCapturingToGpad(pc);
			if (gpad != null && !"auto".equals(gpad))
				sb.append("    pointCapturing: ").append(gpad).append(";\n");
		}

		String tt = es.get("allowToolTips");
		if (tt != null) {
			String gpad = toolTipsToGpad(tt);
			if (gpad != null && !"auto".equals(gpad))
				sb.append("    toolTips: ").append(gpad).append(";\n");
		}

		if ("true".equals(es.get("allowShowMouseCoords")))
			sb.append("    mouseCoords;\n");

		String lar = es.get("lockedAxesRatio");
		if (lar != null && !isZero(lar))
			sb.append("    lockedAxesRatio: ").append(lar).append(";\n");
	}

	private static void emitAxisProp(StringBuilder sb, String axisName,
			LinkedHashMap<String, String> attrs) {
		if (attrs == null) return;
		StringBuilder axisSb = new StringBuilder();
		axisSb.append("    ").append(axisName).append(":");
		boolean hasContent = false;

		if ("false".equals(attrs.get("show"))) {
			axisSb.append(" ~show"); hasContent = true;
		}
		String label = attrs.get("label");
		if (label != null && !label.isEmpty()) {
			axisSb.append(" label=").append(quoteIfNeeded(label)); hasContent = true;
		}
		String unitLabel = attrs.get("unitLabel");
		if (unitLabel != null && !unitLabel.isEmpty()) {
			if ("\u03c0".equals(unitLabel)) {
				axisSb.append(" piUnit");
			} else {
				axisSb.append(" unit=").append(quoteIfNeeded(unitLabel));
			}
			hasContent = true;
		}
		if ("false".equals(attrs.get("showNumbers"))) {
			axisSb.append(" ~numbers"); hasContent = true;
		}
		String tickExpr = attrs.get("tickExpression");
		if (tickExpr != null) {
			axisSb.append(" tickExpr=").append(quoteIfNeeded(tickExpr)); hasContent = true;
		}
		String tickDist = attrs.get("tickDistance");
		if (tickDist != null) {
			axisSb.append(" tickDist=").append(tickDist); hasContent = true;
		}
		String tickStyle = attrs.get("tickStyle");
		if (tickStyle != null) {
			String gpad = tickStyleToGpad(tickStyle);
			if (gpad != null && !"major".equals(gpad)) {
				axisSb.append(" tickStyle=").append(gpad); hasContent = true;
			}
		}
		String cross = attrs.get("axisCross");
		if (cross != null && !isZero(cross)) {
			axisSb.append(" cross=").append(cross); hasContent = true;
		}
		if ("true".equals(attrs.get("drawBorderAxes"))) {
			axisSb.append(" crossEdge"); hasContent = true;
		}
		if ("true".equals(attrs.get("positiveAxis"))) {
			axisSb.append(" positive"); hasContent = true;
		}
		if ("false".equals(attrs.get("selectionAllowed"))) {
			axisSb.append(" ~selectable"); hasContent = true;
		}

		if (hasContent) {
			axisSb.append(";\n");
			sb.append(axisSb);
		}
	}

	private static void emitGridDist(StringBuilder sb,
			LinkedHashMap<String, String> gridAttrs) {
		if (gridAttrs == null) return;
		String dx = gridAttrs.get("distX"), dy = gridAttrs.get("distY");
		String dTheta = gridAttrs.get("distTheta");
		if (dx != null || dy != null || dTheta != null) {
			sb.append("    gridDist:");
			if (dx != null) sb.append(" x=").append(dx);
			if (dy != null) sb.append(" y=").append(dy);
			if (dTheta != null) sb.append(" theta=").append(dTheta);
			sb.append(";\n");
		}
	}

	private static void emitLineAndAxesStyle(StringBuilder sb,
			LinkedHashMap<String, String> ls) {
		if (ls == null) return;
		String gridStyle = ls.get("grid");
		if (gridStyle != null) {
			String gpad = lineStyleToGpad(gridStyle);
			if (gpad != null && !"full".equals(gpad))
				sb.append("    lineStyle: ").append(gpad).append(";\n");
		}
		String axesStyle = ls.get("axes");
		if (axesStyle != null) {
			String gpad = axesStyleToGpad(axesStyle);
			if (gpad != null && !"arrow".equals(gpad))
				sb.append("    axesStyle: ").append(gpad).append(";\n");
		}
	}

	private static void emitLabelStyleProp(StringBuilder sb,
			LinkedHashMap<String, String> ls) {
		if (ls == null) return;
		boolean hasContent = false;
		StringBuilder lsSb = new StringBuilder("    labelStyle:");
		String axes = ls.get("axes");
		if (axes != null) {
			String gpad = fontStyleToGpad(axes);
			if (gpad != null && !"plain".equals(gpad)) {
				lsSb.append(" ").append(gpad);
				hasContent = true;
			}
		}
		if ("true".equals(ls.get("serif"))) {
			lsSb.append(" serif");
			hasContent = true;
		}
		if (hasContent) {
			lsSb.append(";\n");
			sb.append(lsSb);
		}
	}

	private static void emitProjectionProp(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		if (attrs == null) return;
		String type = attrs.get("type");
		if (type == null) return;
		String gpad = projectionToGpad(type);
		if (gpad == null || "orthographic".equals(gpad)) return;
		sb.append("    projection: ").append(gpad);
		if ("perspective".equals(gpad)) {
			String dist = attrs.get("distance");
			if (dist != null) sb.append(" distance=").append(dist);
		} else if ("glasses".equals(gpad)) {
			String sep = attrs.get("separation");
			if (sep != null) sb.append(" separation=").append(sep);
			if ("false".equals(attrs.get("grayScaled"))) sb.append(" ~grayScaled");
			if ("true".equals(attrs.get("shutDownGreen"))) sb.append(" shutDownGreen");
		} else if ("oblique".equals(gpad)) {
			String angle = attrs.get("angle");
			String factor = attrs.get("factor");
			if (angle != null) sb.append(" angle=").append(angle);
			if (factor != null) sb.append(" factor=").append(factor);
		}
		sb.append(";\n");
	}

	// ===================== EV enum converters =====================

	private static String gridTypeToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "-1": return "none";
			case "0": return "cartesian";
			case "1": return "isometric";
			case "2": return "polar";
			case "3": return "cartesianSub";
			default: return null;
		}
	}

	private static String pointCapturingToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "off";
			case "1": return "snap";
			case "2": return "fixed";
			case "3": return "auto";
			default: return null;
		}
	}

	private static String toolTipsToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "auto";
			case "1": return "on";
			case "2": return "off";
			default: return null;
		}
	}

	private static String tickStyleToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "majorMinor";
			case "1": return "major";
			case "2": return "none";
			default: return null;
		}
	}

	private static String lineStyleToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "full";
			case "10": return "dashedShort";
			case "15": return "dashedLong";
			case "20": return "dotted";
			case "30": return "dashedDotted";
			default: return null;
		}
	}

	private static String axesStyleToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "full";
			case "1": return "arrow";
			case "2": return "bold";
			case "3": return "arrowBold";
			case "5": return "twoArrows";
			case "7": return "twoArrowsBold";
			case "9": return "arrowFilled";
			case "11": return "arrowFilledBold";
			case "13": return "twoArrowsFilled";
			case "15": return "twoArrowsFilledBold";
			default: return null;
		}
	}

	private static String fontStyleToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "plain";
			case "1": return "bold";
			case "2": return "italic";
			case "3": return "boldItalic";
			default: return null;
		}
	}

	private static String rulerTypeIntToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "none";
			case "1": return "ruler";
			case "2": return "squareSmall";
			case "3": return "squareBig";
			case "4": return "elementary12";
			case "5": return "elementary12House";
			case "6": return "elementary34";
			case "7": return "music";
			case "8": return "svg";
			case "9": return "elementary12Colored";
			case "10": return "isometric";
			case "11": return "polar";
			case "12": return "dots";
			default: return null;
		}
	}

	private static String projectionToGpad(String xmlVal) {
		if (xmlVal == null) return null;
		switch (xmlVal) {
			case "0": return "orthographic";
			case "1": return "perspective";
			case "2": return "glasses";
			case "3": return "oblique";
			default: return null;
		}
	}

	// ===================== perspective =====================

	private static void convertPerspective(StringBuilder sb,
			XmlSettingsCollector collector) {
		sb.append("  perspective {\n");

		for (XmlSettingsCollector.PaneData pane : collector.getPerspectivePanes()) {
			emitPane(sb, pane);
		}
		for (XmlSettingsCollector.ViewData view : collector.getPerspectiveViews()) {
			emitView(sb, view);
		}
		if (collector.getPerspectiveToolbarAttrs() != null) {
			emitPerspectiveToolbar(sb, collector.getPerspectiveToolbarAttrs());
		}
		if (collector.getPerspectiveInputAttrs() != null) {
			emitPerspectiveInput(sb, collector.getPerspectiveInputAttrs());
		}

		sb.append("  }\n");
	}

	private static void emitPane(StringBuilder sb, XmlSettingsCollector.PaneData pane) {
		sb.append("    pane: \"").append(pane.location != null ? pane.location : "").append("\" ");
		sb.append(pane.divider != null ? pane.divider : "0.5").append(' ');
		sb.append("1".equals(pane.orientation) ? "horizontal" : "vertical");
		sb.append(";\n");
	}

	private static void emitView(StringBuilder sb, XmlSettingsCollector.ViewData view) {
		sb.append("    view ").append(GpadStyleMaps.viewIdToName(view.id)).append(": ");
		boolean visible = !"false".equals(view.visible);
		sb.append(visible ? "show" : "~show");

		boolean stylebarVisible = !"false".equals(view.stylebar);
		if (!stylebarVisible) {
			sb.append(" ~stylebar");
		}

		if (view.location != null) {
			sb.append(" location=\"").append(view.location).append("\"");
		}
		if (view.size != null && !"0".equals(view.size)) {
			sb.append(" size=").append(view.size);
		}
		if (view.toolbar != null && !view.toolbar.isEmpty()) {
			sb.append(" toolbar=\"").append(view.toolbar).append("\"");
		}
		if (view.tab != null) {
			String tabName = GpadStyleMaps.tabIdToName(view.tab);
			if (tabName != null) {
				sb.append(" tab=").append(tabName);
			}
		}
		if (view.plane != null && !view.plane.isEmpty()) {
			sb.append(" plane=\"").append(view.plane).append("\"");
		}
		sb.append(";\n");
	}

	private static void emitPerspectiveToolbar(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		sb.append("    toolbar: ");
		boolean show = !"false".equals(attrs.get("show"));
		sb.append(show ? "show" : "~show");

		String items = attrs.get("items");
		if (items != null) {
			sb.append(" \"").append(items).append("\"");
		}

		String pos = attrs.get("position");
		if (pos != null) {
			switch (pos) {
				case "1": sb.append(" north"); break;
				case "3": sb.append(" south"); break;
				default: sb.append(" position=").append(pos); break;
			}
		}

		sb.append(";\n");
	}

	private static void emitPerspectiveInput(StringBuilder sb,
			LinkedHashMap<String, String> attrs) {
		sb.append("    input: ");
		boolean show = !"false".equals(attrs.get("show"));
		sb.append(show ? "show" : "~show");

		boolean showCmd = !"false".equals(attrs.get("cmd"));
		if (!showCmd) {
			sb.append(" ~cmd");
		} else {
			sb.append(" cmd");
		}

		String top = attrs.get("top");
		if ("true".equals(top)) {
			sb.append(" top");
		} else if ("algebra".equals(top)) {
			sb.append(" algebra");
		} else {
			sb.append(" bottom");
		}

		sb.append(";\n");
	}

	// ===================== EV utility helpers =====================

	private static String nvl(String s, String def) {
		return s != null ? s : def;
	}

	private static boolean isZero(String s) {
		if (s == null || s.isEmpty()) return true;
		try { return Math.abs(Double.parseDouble(s)) < 1e-10; }
		catch (NumberFormatException e) { return false; }
	}

	private static boolean isDefaultScale(String s) {
		if (s == null) return true;
		try { return Math.abs(Double.parseDouble(s) - 50.0) < 1e-6; }
		catch (NumberFormatException e) { return false; }
	}

	private static boolean eq(String a, String b) {
		if (a == null) return b == null;
		return a.equals(b);
	}

	private static String quoteIfNeeded(String s) {
		if (s == null) return "\"\"";
		return "\"" + s + "\"";
	}

	private static String quoteExpr(String val) {
		if (val == null) return "0";
		for (int i = 0; i < val.length(); i++) {
			char c = val.charAt(i);
			if (!((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+' || c == 'E' || c == 'e'))
				return "\"" + val + "\"";
		}
		return val;
	}
}
