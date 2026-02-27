package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.io.XMLStringBuilder;

/**
 * Converts @@env raw content (space-joined EV_BLOCK tokens) into the
 * corresponding XML fragments for {@code <euclidianView>},
 * {@code <euclidianView3D>}, and kernel settings.
 *
 * This is the static inverse of {@link EuclidianSettingsToGpadConverter}.
 */
public class GpadEnvToXmlConverter {

	private final String[] tokens;
	private int pos;

	private GpadEnvToXmlConverter(String rawContent) {
		this.tokens = tokenize(rawContent);
		this.pos = 0;
	}

	/**
	 * Result of converting @@env content to XML fragments.
	 */
	public static class ConvertResult {
		public final StringBuilder ev1Xml = new StringBuilder();
		public final StringBuilder ev2Xml = new StringBuilder();
		public final StringBuilder ev3dXml = new StringBuilder();
		public int rightAngleStyle = -1;
		public final StringBuilder kernelXml = new StringBuilder();
		public final StringBuilder guiXml = new StringBuilder();
		public final StringBuilder perspectiveXml = new StringBuilder();
		public final StringBuilder algebraViewXml = new StringBuilder();
		public final StringBuilder spreadsheetViewXml = new StringBuilder();
		public final StringBuilder keyboardXml = new StringBuilder();
		public final StringBuilder probCalcXml = new StringBuilder();
		public final StringBuilder scriptingXml = new StringBuilder();
		public final StringBuilder tableviewXml = new StringBuilder();
	}

	/**
	 * Mutable parsing state accumulated across one or more parse passes.
	 * Extracting this from local variables enables template merging:
	 * parse the template content first, then parse user content into
	 * the same state — user-set fields override, unmentioned fields
	 * keep the template values.
	 */
	static class ParseState {
		EvBlockData ev1, ev2;
		Ev3dBlockData ev3d;
		PenToolsData penData = new PenToolsData();
		KernelData kernel = new KernelData();
		AlgebraViewData avData = new AlgebraViewData();
		SpreadsheetViewData ssData = new SpreadsheetViewData();
		KeyboardData kbData;
		ProbCalcData pcData;
		ScriptingData scrData;
		TableViewData tvData;
		GuiData guiData = new GuiData();
		PerspectiveData perspData;
		int rightAngleStyle = -1;
		boolean hasAlgebraView;
		boolean hasSpreadsheetView;
		boolean hasKernel;
	}

	/**
	 * Converts @@env raw content into XML fragments (legacy signature).
	 */
	public static void convert(String rawContent,
			StringBuilder ev1Xml, StringBuilder ev2Xml, StringBuilder ev3dXml,
			int[] rightAngleStyle) {
		ConvertResult result = convertAll(rawContent);
		ev1Xml.append(result.ev1Xml);
		ev2Xml.append(result.ev2Xml);
		ev3dXml.append(result.ev3dXml);
		rightAngleStyle[0] = result.rightAngleStyle;
	}

	/**
	 * Converts @@env raw content into all XML fragments.
	 *
	 * @param rawContent space-joined EV_BLOCK tokens from gpadStaticEnvStatement
	 * @return conversion result with all XML fragments
	 */
	public static ConvertResult convertAll(String rawContent) {
		return convertAll(null, rawContent);
	}

	/**
	 * Converts @@env content with optional template into all XML fragments.
	 * If templateContent is non-null, it is parsed first to set baseline
	 * values, then rawContent is parsed on top — user-set fields override
	 * template values, unmentioned fields keep the template defaults.
	 *
	 * @param templateContent template raw content (may be {@code null})
	 * @param rawContent      user raw content from @@env block
	 * @return conversion result with all XML fragments
	 */
	public static ConvertResult convertAll(String templateContent, String rawContent) {
		ParseState state = new ParseState();
		if (templateContent != null && !templateContent.isEmpty()) {
			new GpadEnvToXmlConverter(templateContent).parseInto(state);
		}
		if (rawContent != null && !rawContent.isEmpty()) {
			new GpadEnvToXmlConverter(rawContent).parseInto(state);
		}
		ConvertResult result = new ConvertResult();
		emitAll(state, result);
		return result;
	}

	private void parseInto(ParseState state) {
		while (pos < tokens.length) {
			String tok = peek();
			boolean negated = false;
			if ("~".equals(tok)) {
				negated = true;
				advance();
				tok = peek();
			}

			switch (tok) {
				case "ev1": advance(); state.ev1 = parseEvBlock(state.ev1); break;
				case "ev2": advance(); state.ev2 = parseEvBlock(state.ev2); break;
				case "ev3d": advance(); state.ev3d = parseEv3dBlock(state.ev3d); break;
				case "rightAngleStyle":
					advance(); expect(":"); String ras = advance();
					state.rightAngleStyle = rightAngleStyleToInt(ras);
					consumeIf(";"); break;
				case "pen": advance(); parsePenBlock(state.penData.pen); break;
				case "highlighter": advance(); parsePenBlock(state.penData.highlighter); break;
				case "eraser": advance(); parsePenBlock(state.penData.eraser); break;

				case "continuous":
					advance(); state.kernel.continuous = negated ? 0 : 1;
					state.hasKernel = true; consumeIf(";"); break;
				case "symbolic":
					advance(); state.kernel.symbolic = negated ? 0 : 1;
					state.hasKernel = true; consumeIf(";"); break;
				case "precision":
					advance(); expect(":");
					state.kernel.precisionVal = advance();
					state.hasKernel = true; consumeIf(";"); break;
				case "angleUnit":
					advance(); expect(":"); state.kernel.angleUnit = advance();
					state.hasKernel = true; consumeIf(";"); break;
				case "coordStyle":
					advance(); expect(":"); state.kernel.coordStyle = advance();
					state.hasKernel = true; consumeIf(";"); break;
				case "startAnimation":
					advance(); state.kernel.startAnimation = negated ? 0 : 1;
					state.hasKernel = true; consumeIf(";"); break;
				case "pathRegionParams":
					advance(); expect(":"); state.kernel.pathRegionParams = advance();
					state.hasKernel = true; consumeIf(";"); break;
				case "localization":
					advance(); expect(":");
					parseLocalization(state.kernel);
					state.hasKernel = true; consumeIf(";"); break;
				case "cas":
					advance(); expect(":");
					parseCas(state.kernel);
					state.hasKernel = true; consumeIf(";"); break;

				case "font":
					advance(); expect(":"); state.guiData.fontSize = parseInt(advance());
					consumeIf(";"); break;
				case "labeling":
					advance(); expect(":"); state.guiData.labelingStyle = labelingToInt(advance());
					consumeIf(";"); break;

				case "algebraView":
					advance(); expect(":");
					parseAlgebraViewStatement(state.avData, state.kernel);
					state.hasAlgebraView = true;
					consumeIf(";"); break;

				case "spreadsheetView":
					advance();
					parseSpreadsheetViewBlock(state.ssData, state.kernel);
					state.hasSpreadsheetView = true; break;

				case "keyboard":
					advance(); expect(":");
					if (state.kbData == null) state.kbData = new KeyboardData();
					parseKeyboardStatement(state.kbData);
					consumeIf(";"); break;

				case "probCalc":
					advance();
					if (state.pcData == null) state.pcData = new ProbCalcData();
					parseProbCalcBlock(state.pcData);
					break;

				case "scripting":
					advance(); expect(":");
					if (state.scrData == null) state.scrData = new ScriptingData();
					parseScriptingStatement(state.scrData, negated);
					consumeIf(";"); break;

				case "tableView":
					advance(); expect(":");
					if (state.tvData == null) state.tvData = new TableViewData();
					parseTableViewStatement(state.tvData);
					consumeIf(";"); break;

				case "perspective":
					advance();
					if (state.perspData == null) state.perspData = new PerspectiveData();
					parsePerspectiveBlock(state.perspData);
					break;

				default:
					advance(); break;
			}
		}
	}

	private static void emitAll(ParseState state, ConvertResult result) {
		result.rightAngleStyle = state.rightAngleStyle;

		if (state.ev1 != null)
			emitEuclidianViewXml(state.ev1, 1, state.rightAngleStyle, result.ev1Xml);
		if (state.ev2 != null)
			emitEuclidianViewXml(state.ev2, 2, state.rightAngleStyle, result.ev2Xml);
		if (state.ev3d != null)
			emitEv3dXml(state.ev3d, state.rightAngleStyle, result.ev3dXml);

		if (state.hasAlgebraView) {
			state.kernel.algebraStyleVal = algebraStyleToInt(state.avData.style);
		}
		if (state.hasSpreadsheetView) {
			state.kernel.algebraStyleSpreadsheet = algebraStyleToInt(state.ssData.style);
		}
		if (state.hasKernel || state.kernel.algebraStyleVal >= 0
				|| state.kernel.algebraStyleSpreadsheet >= 0)
			emitKernelXml(state.kernel, result.kernelXml);
		if (state.guiData.fontSize >= 0 || state.guiData.labelingStyle >= 0)
			emitGuiXml(state.guiData, result.guiXml);
		if (state.hasAlgebraView)
			emitAlgebraViewXml(state.avData, result.algebraViewXml);
		if (state.hasSpreadsheetView)
			emitSpreadsheetViewXml(state.ssData, result.spreadsheetViewXml);
		if (state.kbData != null)
			emitKeyboardXml(state.kbData, result.keyboardXml);
		if (state.pcData != null)
			emitProbCalcXml(state.pcData, result.probCalcXml);
		if (state.scrData != null)
			emitScriptingXml(state.scrData, result.scriptingXml);
		if (state.tvData != null)
			emitTableviewXml(state.tvData, result.tableviewXml);
		if (state.perspData != null)
			emitPerspectiveXml(state.perspData, result.perspectiveXml);
	}

	// ===================== Data classes =====================

	private static class EvBlockData {
		String bgColor, axesColor, gridColor;
		int sizeW = -1, sizeH = -1;
		String csOriginX, csOriginY, csScale, csXscale, csYscale;
		String csXMin, csXMax, csYMin, csYMax;
		boolean hasAxes = true, hasGrid = false, gridBold = false;
		int gridType = Integer.MIN_VALUE, pointCapturing = -1, tooltips = -1;
		boolean mouseCoords = false;
		double lockedAxesRatio = -1;
		AxisData xAxis, yAxis, zAxis;
		double gridDistX = Double.NaN, gridDistY = Double.NaN, gridDistTheta = Double.NaN;
		int gridLineStyle = -1, axesLineStyle = -1;
		int labelFontStyle = -1;
		boolean labelSerif = false;
		String rulerType;
		String rulerColor;
	}

	private static class Ev3dBlockData extends EvBlockData {
		boolean hasPlane = false;
		boolean lightExplicit = false, lightVal = true;
		boolean yAxisUp = false, coloredAxes = false;
		boolean useClipping = false, showClipping = false;
		int clippingReduction = 0;
		int projection = -1;
		int projDistance = -1;
		int projSeparation = -1;
		boolean projGrayScaled = true, projShutDownGreen = false;
		double projAngle = Double.NaN, projFactor = Double.NaN;
		String csOriginZ, csZscale;
		String csZMin, csZMax;
		String csXAngle, csZAngle;
	}

	private static class AxisData {
		boolean show = true;
		String label, unit;
		boolean piUnit = false;
		boolean showNumbers = true;
		double tickDist = Double.NaN;
		String tickExpression;
		String tickStyle;
		double cross = 0;
		boolean crossEdge = false, positive = false, selectable = true;
	}

	private static class PenToolBlock {
		String color;
		int thickness = -1;
		int opacity = -1;
		int size = -1;
	}

	private static class PenToolsData {
		PenToolBlock pen = new PenToolBlock();
		PenToolBlock highlighter = new PenToolBlock();
		PenToolBlock eraser = new PenToolBlock();
	}

	// --- New data classes for extended settings ---

	private static class KernelData {
		int continuous = -1;  // -1=unset, 0=false, 1=true
		int symbolic = -1;
		String precisionVal;  // e.g. "15dp" or "10sf"
		String angleUnit;
		String coordStyle;
		int startAnimation = -1;
		String pathRegionParams;
		String locDigits, locLabels; // "true"/"false"
		String casTimeout;
		int casRootForm = -1;
		int algebraStyleVal = -1;
		int algebraStyleSpreadsheet = -1;
	}

	private static class AlgebraViewData {
		String sort;  // dependency|type|layer|order
		String style; // value|description|...
		boolean auxiliary = false;
	}

	private static class SpreadsheetViewData {
		String style;
		int sizeW = -1, sizeH = -1;
		int cellW = -1, cellH = -1;
		int rows = -1, columns = -1;
		List<String> columnWidths; // positional, "default" / "=" / number
		String layoutGrid, layoutFormulaBar, layoutHScroll, layoutVScroll;
		String layoutBrowserPanel, layoutColumnHeader, layoutRowHeader;
		String layoutSpecialEditor, layoutToolTips, layoutEqualsRequired;
		String cellFormat;
	}

	private static class KeyboardData {
		String width, height, opacity, language;
		boolean show = false;
	}

	private static class ProbCalcData {
		String distType;
		int cumulative = -1, overlayActive = -1;
		String parameters;
		String intervalMode;
		String intervalLow, intervalHigh;
	}

	private static class ScriptingData {
		int blocked = -1, disabled = -1;
	}

	private static class TableViewData {
		String min, max, step;
		String xValues, xCaption;
	}

	private static class GuiData {
		int fontSize = -1;
		int labelingStyle = -1;
	}

	private static class PerspectiveData {
		final List<PerspPaneData> panes = new ArrayList<>();
		final List<PerspViewData> views = new ArrayList<>();
		boolean toolbarShow = true;
		String toolbarItems;
		String toolbarPosition;
		boolean inputShow = true;
		boolean inputCmd = true;
		String inputTop;
	}

	private static class PerspPaneData {
		String location;
		String divider;
		String orientation;
	}

	private static class PerspViewData {
		String id;
		boolean visible = true;
		boolean stylebar = true;
		String location;
		String size;
		String toolbar;
		String tab;
		String plane;
	}

	// ===================== Parsing =====================

	private EvBlockData parseEvBlock(EvBlockData existing) {
		EvBlockData data = existing != null ? existing : new EvBlockData();
		expect("{");
		while (pos < tokens.length && !"}".equals(peek())) {
			parseEvProperty(data);
		}
		consumeIf("}");
		return data;
	}

	private Ev3dBlockData parseEv3dBlock(Ev3dBlockData existing) {
		Ev3dBlockData data = existing != null ? existing : new Ev3dBlockData();
		expect("{");
		while (pos < tokens.length && !"}".equals(peek())) {
			parseEv3dProperty(data);
		}
		consumeIf("}");
		return data;
	}

	private void parseEvProperty(EvBlockData data) {
		parseEvProperty(data, false);
	}

	private void parseEvProperty(EvBlockData data, boolean callerNegated) {
		String tok = peek();
		boolean negated = callerNegated;
		if ("~".equals(tok)) {
			negated = true;
			advance();
			tok = peek();
		}

		switch (tok) {
			case GpadEnvKeys.Ev.BG_COLOR:
				advance(); expect(":"); data.bgColor = advance(); consumeIf(";"); break;
			case GpadEnvKeys.Ev.AXES_COLOR:
				advance(); expect(":"); data.axesColor = advance(); consumeIf(";"); break;
			case GpadEnvKeys.Ev.GRID_COLOR:
				advance(); expect(":"); data.gridColor = advance(); consumeIf(";"); break;
			case GpadEnvKeys.Ev.RULER_COLOR:
				advance(); expect(":"); data.rulerColor = advance(); consumeIf(";"); break;
			case GpadEnvKeys.Ev.SIZE:
				advance(); expect(":");
				data.sizeW = parseInt(advance());
				expect(",");
				data.sizeH = parseInt(advance());
				consumeIf(";"); break;
			case GpadEnvKeys.Ev.COORD_SYSTEM:
				advance(); expect(":");
				parseCoordSystem2D(data);
				consumeIf(";"); break;
			case GpadEnvKeys.Ev.AXES:
				advance(); data.hasAxes = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.GRID:
				advance(); data.hasGrid = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.GRID_BOLD:
				advance(); data.gridBold = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.MOUSE_COORDS:
				advance(); data.mouseCoords = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.GRID_TYPE:
				advance(); expect(":"); data.gridType = gridTypeToInt(advance()); consumeIf(";"); break;
			case GpadEnvKeys.Ev.POINT_CAPTURING:
				advance(); expect(":"); data.pointCapturing = pointCapturingToInt(advance()); consumeIf(";"); break;
			case GpadEnvKeys.Ev.TOOL_TIPS:
				advance(); expect(":"); data.tooltips = toolTipsToInt(advance()); consumeIf(";"); break;
			case GpadEnvKeys.Ev.LOCKED_AXES_RATIO:
				advance(); expect(":"); data.lockedAxesRatio = parseDouble(advance()); consumeIf(";"); break;
			case GpadEnvKeys.Ev.X_AXIS:
				advance(); expect(":"); data.xAxis = parseAxisProperties(negated); consumeIf(";"); break;
			case GpadEnvKeys.Ev.Y_AXIS:
				advance(); expect(":"); data.yAxis = parseAxisProperties(negated); consumeIf(";"); break;
			case GpadEnvKeys.Ev.Z_AXIS:
				advance(); expect(":"); data.zAxis = parseAxisProperties(negated); consumeIf(";"); break;
			case GpadEnvKeys.Ev.GRID_DIST:
				advance(); expect(":");
				parseGridDist(data);
				consumeIf(";"); break;
			case GpadEnvKeys.Ev.LINE_STYLE:
				advance(); expect(":"); data.gridLineStyle = lineStyleToInt(advance()); consumeIf(";"); break;
			case GpadEnvKeys.Ev.AXES_STYLE:
				advance(); expect(":"); data.axesLineStyle = axesStyleToInt(advance()); consumeIf(";"); break;
			case GpadEnvKeys.Ev.LABEL_STYLE:
				advance(); expect(":");
				while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
					String lsTok = peek();
					if (GpadEnvKeys.Ev.LABEL_SERIF.equals(lsTok)) {
						advance(); data.labelSerif = true;
					} else {
						int fs = fontStyleToInt(lsTok);
						if (fs >= 0) { data.labelFontStyle = fs; advance(); }
						else advance();
					}
				}
				consumeIf(";"); break;
			case GpadEnvKeys.Ev.RULER_TYPE:
				advance(); expect(":"); data.rulerType = advance(); consumeIf(";"); break;
			default:
				advance(); break;
		}
	}

	private void parseEv3dProperty(Ev3dBlockData data) {
		String tok = peek();
		boolean negated = false;
		if ("~".equals(tok)) {
			negated = true;
			advance();
			tok = peek();
		}

		switch (tok) {
			case GpadEnvKeys.Ev.PLANE:
				advance(); data.hasPlane = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.LIGHT:
				advance(); data.lightExplicit = true; data.lightVal = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.Y_AXIS_UP:
				advance(); data.yAxisUp = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.COLORED_AXES:
				advance(); data.coloredAxes = !negated; consumeIf(";"); break;
			case GpadEnvKeys.Ev.CLIPPING:
				advance(); expect(":");
				parseClipping(data);
				consumeIf(";"); break;
			case GpadEnvKeys.Ev.PROJECTION:
				advance(); expect(":");
				parseProjection(data);
				consumeIf(";"); break;
			case GpadEnvKeys.Ev.COORD_SYSTEM:
				advance(); expect(":");
				parseCoordSystem3D(data);
				consumeIf(";"); break;
			default:
				parseEvProperty(data, negated);
				break;
		}
	}

	private void parseCoordSystem2D(EvBlockData data) {
		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			if (key.startsWith(GpadEnvKeys.CoordSystem.ORIGIN)) {
				data.csOriginX = advanceValue();
				expect(",");
				data.csOriginY = advance();
			} else if (key.startsWith(GpadEnvKeys.CoordSystem.SCALE)
					&& !GpadEnvKeys.CoordSystem.X_SCALE.equals(key)
					&& !GpadEnvKeys.CoordSystem.Y_SCALE.equals(key)) {
				data.csScale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.X_SCALE.equals(key)) {
				data.csXscale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.Y_SCALE.equals(key)) {
				data.csYscale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.X_MIN.equals(key)) {
				data.csXMin = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.X_MAX.equals(key)) {
				data.csXMax = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.Y_MIN.equals(key)) {
				data.csYMin = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.Y_MAX.equals(key)) {
				data.csYMax = unquote(advanceValue());
			} else {
				advance();
			}
		}
	}

	private void parseCoordSystem3D(Ev3dBlockData data) {
		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			if (GpadEnvKeys.CoordSystem.ORIGIN.equals(key)) {
				data.csOriginX = advanceValue();
				expect(",");
				data.csOriginY = advance();
				if (",".equals(peek())) {
					advance();
					data.csOriginZ = advance();
				}
			} else if (GpadEnvKeys.CoordSystem.SCALE.equals(key)) {
				data.csScale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.X_SCALE.equals(key)) {
				data.csXscale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.Y_SCALE.equals(key)) {
				data.csYscale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.Z_SCALE.equals(key)) {
				data.csZscale = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.X_ANGLE.equals(key)) {
				data.csXAngle = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.Z_ANGLE.equals(key)) {
				data.csZAngle = advanceValue();
			} else if (GpadEnvKeys.CoordSystem.Z_MIN.equals(key)) {
				data.csZMin = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.Z_MAX.equals(key)) {
				data.csZMax = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.X_MIN.equals(key)) {
				data.csXMin = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.X_MAX.equals(key)) {
				data.csXMax = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.Y_MIN.equals(key)) {
				data.csYMin = unquote(advanceValue());
			} else if (GpadEnvKeys.CoordSystem.Y_MAX.equals(key)) {
				data.csYMax = unquote(advanceValue());
			} else {
				parseCoordSystem2D(data);
				return;
			}
		}
	}

	private void parseGridDist(EvBlockData data) {
		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			if (GpadEnvKeys.GridDist.X.equals(key)) {
				data.gridDistX = parseDouble(advanceValue());
			} else if (GpadEnvKeys.GridDist.Y.equals(key)) {
				data.gridDistY = parseDouble(advanceValue());
			} else if (GpadEnvKeys.GridDist.THETA.equals(key)) {
				data.gridDistTheta = parseDouble(advanceValue());
			} else {
				advance();
			}
		}
	}

	private void parseClipping(Ev3dBlockData data) {
		while (pos < tokens.length && !";".equals(peek())) {
			String val = peek();
			if ("~".equals(val)) {
				advance();
				String next = peek();
				if (GpadEnvKeys.Clipping.USE.equals(next)) { advance(); data.useClipping = false; }
				else if (GpadEnvKeys.Clipping.SHOW.equals(next)) { advance(); data.showClipping = false; }
				else advance();
			} else if (GpadEnvKeys.Clipping.USE.equals(val)) {
				advance();
				data.useClipping = true;
			} else if (GpadEnvKeys.Clipping.SHOW.equals(val)) {
				advance();
				data.showClipping = true;
			} else if (GpadEnvKeys.Clipping.SIZE.equals(val)) {
				data.clippingReduction = parseInt(advanceValue());
			} else {
				advance();
			}
		}
	}

	private void parseProjection(Ev3dBlockData data) {
		String projName = advance();
		data.projection = projectionToInt(projName);
		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			boolean negated = false;
			if ("~".equals(key)) { negated = true; advance(); key = peek(); }
			if (GpadEnvKeys.Projection.DISTANCE.equals(key)) {
				data.projDistance = parseInt(advanceValue());
			} else if (GpadEnvKeys.Projection.SEPARATION.equals(key)) {
				data.projSeparation = parseInt(advanceValue());
			} else if (GpadEnvKeys.Projection.GRAY_SCALED.equals(key)) {
				advance();
				data.projGrayScaled = !negated;
			} else if (GpadEnvKeys.Projection.SHUT_DOWN_GREEN.equals(key)) {
				advance();
				data.projShutDownGreen = !negated;
			} else if (GpadEnvKeys.Projection.ANGLE.equals(key)) {
				data.projAngle = parseDouble(advanceValue());
			} else if (GpadEnvKeys.Projection.FACTOR.equals(key)) {
				data.projFactor = parseDouble(advanceValue());
			} else {
				advance();
			}
		}
	}

	private AxisData parseAxisProperties(boolean negatedOuter) {
		AxisData axis = new AxisData();
		if (negatedOuter) axis.show = false;

		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			boolean negated = false;
			if ("~".equals(key)) { negated = true; advance(); key = peek(); }

			switch (key) {
				case GpadEnvKeys.Axis.SHOW: advance(); axis.show = !negated; break;
				case GpadEnvKeys.Axis.LABEL: axis.label = unquote(advanceValue()); break;
				case GpadEnvKeys.Axis.UNIT: axis.unit = unquote(advanceValue()); break;
				case GpadEnvKeys.Axis.PI_UNIT: advance(); axis.piUnit = !negated; break;
				case GpadEnvKeys.Axis.NUMBERS: advance(); axis.showNumbers = !negated; break;
				case GpadEnvKeys.Axis.TICK_EXPR: axis.tickExpression = unquote(advanceValue()); break;
				case GpadEnvKeys.Axis.TICK_DIST: axis.tickDist = parseDouble(advanceValue()); break;
				case GpadEnvKeys.Axis.TICK_STYLE: axis.tickStyle = advanceValue(); break;
				case GpadEnvKeys.Axis.CROSS: axis.cross = parseDouble(advanceValue()); break;
				case GpadEnvKeys.Axis.CROSS_EDGE: advance(); axis.crossEdge = !negated; break;
				case GpadEnvKeys.Axis.POSITIVE: advance(); axis.positive = !negated; break;
				case GpadEnvKeys.Axis.SELECTABLE: advance(); axis.selectable = !negated; break;
				default: advance(); break;
			}
		}
		return axis;
	}

	private void parsePenBlock(PenToolBlock block) {
		expect("{");
		while (pos < tokens.length && !"}".equals(peek())) {
			String key = peek();
			switch (key) {
				case "color":
					advance(); expect(":"); block.color = advance(); consumeIf(";"); break;
				case "thickness":
					advance(); expect(":"); block.thickness = parseInt(advance()); consumeIf(";"); break;
				case "opacity":
					advance(); expect(":"); block.opacity = parseInt(advance()); consumeIf(";"); break;
				case "size":
					advance(); expect(":"); block.size = parseInt(advance()); consumeIf(";"); break;
				default:
					advance(); break;
			}
		}
		consumeIf("}");
	}

	// ===================== New parsing methods =====================

	private void parseLocalization(KernelData kernel) {
		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			boolean neg = false;
			if ("~".equals(key)) { neg = true; advance(); key = peek(); }
			if ("digits".equals(key)) {
				advance(); kernel.locDigits = neg ? "false" : "true";
			} else if ("labels".equals(key)) {
				advance(); kernel.locLabels = neg ? "false" : "true";
			} else { advance(); }
		}
	}

	private void parseCas(KernelData kernel) {
		while (pos < tokens.length && !";".equals(peek())) {
			String key = peek();
			boolean neg = false;
			if ("~".equals(key)) { neg = true; advance(); key = peek(); }
			if ("rootForm".equals(key)) {
				advance(); kernel.casRootForm = neg ? 0 : 1;
			} else if ("timeout".equals(key)) {
				kernel.casTimeout = advanceValue();
			} else if (key.startsWith("timeout=")) {
				advance(); kernel.casTimeout = key.substring("timeout=".length());
			} else { advance(); }
		}
	}

	private void parseAlgebraViewStatement(AlgebraViewData data, KernelData kernel) {
		while (pos < tokens.length && !";".equals(peek())) {
			String tok = peek();
			boolean neg = false;
			if ("~".equals(tok)) { neg = true; advance(); tok = peek(); }
			if ("sort".equals(tok)) {
				data.sort = advanceValue();
			} else if ("style".equals(tok)) {
				data.style = advanceValue();
			} else if (tok.startsWith("sort=")) {
				advance(); data.sort = tok.substring(5);
			} else if (tok.startsWith("style=")) {
				advance(); data.style = tok.substring(6);
			} else if ("auxiliary".equals(tok)) {
				advance(); data.auxiliary = !neg;
			} else { advance(); }
		}
	}

	private void parseSpreadsheetViewBlock(SpreadsheetViewData data, KernelData kernel) {
		expect("{");
		while (pos < tokens.length && !"}".equals(peek())) {
			String key = peek();
			switch (key) {
				case "style":
					advance(); expect(":"); data.style = advance(); consumeIf(";"); break;
				case "size":
					advance(); expect(":");
					data.sizeW = parseInt(advance());
					data.sizeH = parseInt(advance());
					consumeIf(";"); break;
				case "cellSize":
					advance(); expect(":");
					data.cellW = parseInt(advance());
					data.cellH = parseInt(advance());
					consumeIf(";"); break;
				case "rows":
					advance(); expect(":"); data.rows = parseInt(advance());
					consumeIf(";"); break;
				case "columns":
					advance(); expect(":"); data.columns = parseInt(advance());
					consumeIf(";"); break;
				case "column":
					advance(); expect(":");
					data.columnWidths = parseColumnWidths();
					consumeIf(";"); break;
				case "layout":
					advance(); expect(":");
					parseSpreadsheetLayout(data);
					consumeIf(";"); break;
				case "cellFormat":
					advance(); expect(":");
					data.cellFormat = unquote(advance());
					consumeIf(";"); break;
				default:
					advance(); break;
			}
		}
		consumeIf("}");
	}

	private List<String> parseColumnWidths() {
		List<String> widths = new ArrayList<>();
		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			widths.add(advance());
		}
		return widths;
	}

	private void parseSpreadsheetLayout(SpreadsheetViewData data) {
		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			String tok = peek();
			boolean neg = false;
			if ("~".equals(tok)) { neg = true; advance(); tok = peek(); }
			String val = neg ? "false" : "true";
			switch (tok) {
				case "grid": advance(); data.layoutGrid = val; break;
				case "formulaBar": advance(); data.layoutFormulaBar = val; break;
				case "hScroll": advance(); data.layoutHScroll = val; break;
				case "vScroll": advance(); data.layoutVScroll = val; break;
				case "browserPanel": advance(); data.layoutBrowserPanel = val; break;
				case "columnHeader": advance(); data.layoutColumnHeader = val; break;
				case "rowHeader": advance(); data.layoutRowHeader = val; break;
				case "specialEditor": advance(); data.layoutSpecialEditor = val; break;
				case "toolTips": advance(); data.layoutToolTips = val; break;
				case "equalsRequired": advance(); data.layoutEqualsRequired = val; break;
				default: advance(); break;
			}
		}
	}

	private void parseKeyboardStatement(KeyboardData data) {
		while (pos < tokens.length && !";".equals(peek())) {
			String tok = peek();
			if ("width".equals(tok)) { data.width = advanceValue(); }
			else if ("height".equals(tok)) { data.height = advanceValue(); }
			else if ("opacity".equals(tok)) { data.opacity = advanceValue(); }
			else if ("language".equals(tok)) {
				data.language = unquote(advanceValue());
			} else if (tok.startsWith("width=")) { advance(); data.width = tok.substring(6); }
			else if (tok.startsWith("height=")) { advance(); data.height = tok.substring(7); }
			else if (tok.startsWith("opacity=")) { advance(); data.opacity = tok.substring(8); }
			else if (tok.startsWith("language=")) {
				advance(); data.language = unquote(tok.substring(9));
			} else if ("show".equals(tok)) { advance(); data.show = true; }
			else { advance(); }
		}
	}

	private void parseProbCalcBlock(ProbCalcData data) {
		expect("{");
		while (pos < tokens.length && !"}".equals(peek())) {
			String key = peek();
			switch (key) {
				case "distribution":
					advance(); expect(":");
					parseProbCalcDistribution(data);
					consumeIf(";"); break;
				case "interval":
					advance(); expect(":");
					parseProbCalcInterval(data);
					consumeIf(";"); break;
				default: advance(); break;
			}
		}
		consumeIf("}");
	}

	private void parseProbCalcDistribution(ProbCalcData data) {
		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			String tok = peek();
			boolean neg = false;
			if ("~".equals(tok)) { neg = true; advance(); tok = peek(); }
			if ("cumulative".equals(tok)) {
				advance(); data.cumulative = neg ? 0 : 1;
			} else if ("overlayActive".equals(tok)) {
				advance(); data.overlayActive = neg ? 0 : 1;
			} else if ("parameters".equals(tok)) {
				data.parameters = unquote(advanceValue());
			} else if (tok.startsWith("parameters=")) {
				advance(); data.parameters = unquote(tok.substring(11));
			} else if (data.distType == null && !tok.startsWith("~")) {
				advance(); data.distType = tok;
			} else { advance(); }
		}
	}

	private void parseProbCalcInterval(ProbCalcData data) {
		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			String tok = peek();
			if ("low".equals(tok)) { data.intervalLow = advanceValue(); }
			else if ("high".equals(tok)) { data.intervalHigh = advanceValue(); }
			else if (tok.startsWith("low=")) { advance(); data.intervalLow = tok.substring(4); }
			else if (tok.startsWith("high=")) { advance(); data.intervalHigh = tok.substring(5); }
			else if (data.intervalMode == null) { advance(); data.intervalMode = tok; }
			else { advance(); }
		}
	}

	private void parseScriptingStatement(ScriptingData data, boolean negatedOuter) {
		while (pos < tokens.length && !";".equals(peek())) {
			String tok = peek();
			boolean neg = negatedOuter;
			if ("~".equals(tok)) { neg = true; advance(); tok = peek(); }
			if ("blocked".equals(tok)) { advance(); data.blocked = neg ? 0 : 1; }
			else if ("disabled".equals(tok)) { advance(); data.disabled = neg ? 0 : 1; }
			else { advance(); }
		}
	}

	private void parseTableViewStatement(TableViewData data) {
		while (pos < tokens.length && !";".equals(peek())) {
			String tok = peek();
			if ("min".equals(tok)) { data.min = advanceValue(); }
			else if ("max".equals(tok)) { data.max = advanceValue(); }
			else if ("step".equals(tok)) { data.step = advanceValue(); }
			else if ("xValues".equals(tok)) {
				data.xValues = unquote(advanceValue());
			} else if ("xCaption".equals(tok)) {
				data.xCaption = unquote(advanceValue());
			} else if (tok.startsWith("min=")) { advance(); data.min = tok.substring(4); }
			else if (tok.startsWith("max=")) { advance(); data.max = tok.substring(4); }
			else if (tok.startsWith("step=")) { advance(); data.step = tok.substring(5); }
			else if (tok.startsWith("xValues=")) {
				advance(); data.xValues = unquote(tok.substring(8));
			} else if (tok.startsWith("xCaption=")) {
				advance(); data.xCaption = unquote(tok.substring(9));
			} else { advance(); }
		}
	}

	// ===================== New XML Emission =====================

	private static void emitKernelXml(KernelData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("kernel", 0).endTag();

		if (data.continuous >= 0)
			xml.startTag("continuous").attr("val", data.continuous == 1).endTag();
		if (data.symbolic == 1)
			xml.startTag("symbolic").attr("val", true).endTag();

		if (data.pathRegionParams != null)
			xml.startTag("usePathAndRegionParameters")
					.attr("val", data.pathRegionParams).endTag();

		if (data.precisionVal != null) {
			if (data.precisionVal.endsWith("dp")) {
				String num = data.precisionVal.substring(0, data.precisionVal.length() - 2);
				xml.startTag("decimals").attr("val", parseInt(num)).endTag();
			} else if (data.precisionVal.endsWith("sf")) {
				String num = data.precisionVal.substring(0, data.precisionVal.length() - 2);
				xml.startTag("significantfigures").attr("val", parseInt(num)).endTag();
			}
		}

		if (data.angleUnit != null) {
			String xmlUnit;
			switch (data.angleUnit) {
				case "radian": xmlUnit = "radiant"; break;
				case "dms": xmlUnit = "degreesMinutesSeconds"; break;
				default: xmlUnit = data.angleUnit; break;
			}
			xml.startTag("angleUnit").attrRaw("val", xmlUnit).endTag();
		}

		if (data.algebraStyleVal >= 0 || data.algebraStyleSpreadsheet >= 0) {
			xml.startTag("algebraStyle");
			xml.attr("val", data.algebraStyleVal >= 0 ? data.algebraStyleVal : 0);
			xml.attr("spreadsheet",
					data.algebraStyleSpreadsheet >= 0 ? data.algebraStyleSpreadsheet : 0);
			xml.endTag();
		}

		if (data.coordStyle != null) {
			int cs;
			switch (data.coordStyle) {
				case "austrian": cs = 1; break;
				case "french": cs = 2; break;
				default: cs = 0; break;
			}
			xml.startTag("coordStyle").attr("val", cs).endTag();
		}

		if (data.startAnimation == 1)
			xml.startTag("startAnimation").attr("val", true).endTag();

		if (data.locDigits != null || data.locLabels != null) {
			xml.startTag("localization");
			xml.attr("digits", "true".equals(data.locDigits));
			xml.attr("labels", "true".equals(data.locLabels));
			xml.endTag();
		}

		if (data.casTimeout != null || data.casRootForm >= 0) {
			xml.startTag("casSettings");
			xml.attr("timeout", data.casTimeout != null ? parseInt(data.casTimeout) : 5);
			xml.attr("expRoots", data.casRootForm == 1);
			xml.endTag();
		}

		xml.closeTag("kernel");
		sb.append(xml.toString());
	}

	private static void emitGuiXml(GuiData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("gui", 0).endTag();
		if (data.fontSize >= 0)
			xml.startTag("font").attr("size", data.fontSize).endTag();
		if (data.labelingStyle >= 0)
			xml.startTag("labelingStyle").attr("val", data.labelingStyle).endTag();
		xml.closeTag("gui");
		sb.append(xml.toString());
	}

	private static void emitAlgebraViewXml(AlgebraViewData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("algebraView", 0).endTag();
		if (data.sort != null) {
			xml.startTag("mode").attr("val", sortModeToInt(data.sort)).endTag();
		}
		if (data.auxiliary)
			xml.startTag("auxiliary").attr("show", true).endTag();
		xml.closeTag("algebraView");
		sb.append(xml.toString());
	}

	private static void emitSpreadsheetViewXml(SpreadsheetViewData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("spreadsheetView", 0).endTag();

		if (data.sizeW > 0 && data.sizeH > 0)
			xml.startTag("size").attr("width", data.sizeW).attr("height", data.sizeH).endTag();
		if (data.cellW > 0 || data.cellH > 0) {
			xml.startTag("prefCellSize");
			if (data.cellW > 0) xml.attr("width", data.cellW);
			if (data.cellH > 0) xml.attr("height", data.cellH);
			xml.endTag();
		}
		if (data.rows > 0 || data.columns > 0) {
			xml.startTag("spreadsheetDimensions");
			if (data.rows > 0) xml.attr("rows", data.rows);
			if (data.columns > 0) xml.attr("columns", data.columns);
			xml.endTag();
		}

		if (data.columnWidths != null) {
			String prevWidth = null;
			for (int i = 0; i < data.columnWidths.size(); i++) {
				String w = data.columnWidths.get(i);
				if ("default".equals(w)) {
					prevWidth = null;
					continue;
				}
				String actualWidth;
				if ("=".equals(w)) {
					if (prevWidth == null) continue;
					actualWidth = prevWidth;
				} else {
					actualWidth = w;
				}
				xml.startTag("spreadsheetColumn")
						.attr("id", i).attr("width", parseInt(actualWidth)).endTag();
				prevWidth = actualWidth;
			}
		}

		emitSpreadsheetLayout(xml, data);

		if (data.cellFormat != null && !data.cellFormat.isEmpty()) {
			xml.startTag("spreadsheetCellFormat")
					.attrRaw("formatMap", data.cellFormat).endTag();
		}

		xml.closeTag("spreadsheetView");
		sb.append(xml.toString());
	}

	private static void emitSpreadsheetLayout(XMLStringBuilder xml, SpreadsheetViewData data) {
		boolean hasLayout = data.layoutGrid != null || data.layoutFormulaBar != null
				|| data.layoutHScroll != null || data.layoutVScroll != null
				|| data.layoutBrowserPanel != null || data.layoutColumnHeader != null
				|| data.layoutRowHeader != null || data.layoutSpecialEditor != null
				|| data.layoutToolTips != null || data.layoutEqualsRequired != null;
		if (!hasLayout) return;

		xml.startTag("layout");
		attrBool(xml, "showGrid", data.layoutGrid);
		attrBool(xml, "showFormulaBar", data.layoutFormulaBar);
		attrBool(xml, "showHScrollBar", data.layoutHScroll);
		attrBool(xml, "showVScrollBar", data.layoutVScroll);
		attrBool(xml, "showBrowserPanel", data.layoutBrowserPanel);
		attrBool(xml, "showColumnHeader", data.layoutColumnHeader);
		attrBool(xml, "showRowHeader", data.layoutRowHeader);
		attrBool(xml, "allowSpecialEditor", data.layoutSpecialEditor);
		attrBool(xml, "allowToolTips", data.layoutToolTips);
		attrBool(xml, "equalsRequired", data.layoutEqualsRequired);
		xml.endTag();
	}

	private static void attrBool(XMLStringBuilder xml, String name, String val) {
		if (val != null) xml.attr(name, "true".equals(val));
	}

	private static void emitKeyboardXml(KeyboardData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startTag("keyboard", 0);
		if (data.width != null) xml.attr("width", parseInt(data.width));
		if (data.height != null) xml.attr("height", parseInt(data.height));
		if (data.opacity != null) xml.attrRaw("opacity", data.opacity);
		if (data.language != null) xml.attr("language", data.language);
		xml.attr("show", data.show);
		xml.endTag();
		sb.append(xml.toString());
	}

	private static void emitProbCalcXml(ProbCalcData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("probabilityCalculator", 0).endTag();

		if (data.distType != null) {
			xml.startTag("distribution");
			xml.attr("type", distTypeToInt(data.distType));
			if (data.cumulative >= 0) xml.attr("isCumulative", data.cumulative == 1);
			if (data.overlayActive >= 0) xml.attr("isOverlayActive", data.overlayActive == 1);
			if (data.parameters != null) xml.attr("parameters", data.parameters);
			xml.endTag();
		}

		if (data.intervalMode != null) {
			xml.startTag("interval");
			xml.attr("mode", intervalModeToInt(data.intervalMode));
			if (data.intervalLow != null) xml.attrRaw("low", data.intervalLow);
			if (data.intervalHigh != null) xml.attrRaw("high", data.intervalHigh);
			xml.endTag();
		}

		xml.closeTag("probabilityCalculator");
		sb.append(xml.toString());
	}

	private static void emitScriptingXml(ScriptingData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startTag("scripting", 0);
		xml.attr("blocked", data.blocked == 1);
		if (data.disabled >= 0)
			xml.attr("disabled", data.disabled == 1);
		xml.endTag();
		sb.append(xml.toString());
	}

	private static void emitTableviewXml(TableViewData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startTag("tableview", 0);
		if (data.xValues != null) {
			xml.attr("xValues", data.xValues);
			if (data.xCaption != null) xml.attr("xCaption", data.xCaption);
		} else {
			if (data.min != null) xml.attrRaw("min", data.min);
			if (data.max != null) xml.attrRaw("max", data.max);
			if (data.step != null) xml.attrRaw("step", data.step);
		}
		xml.endTag();
		sb.append(xml.toString());
	}

	// ===================== New value converters =====================

	private static int labelingToInt(String val) {
		switch (val) {
			case "automatic": return 0;
			case "alwaysOn": return 1;
			case "alwaysOff": return 2;
			case "pointsOnly": return 3;
			case "useDefaults": return 4;
			default: return -1;
		}
	}

	private static int sortModeToInt(String val) {
		switch (val) {
			case "dependency": return 0;
			case "type": return 1;
			case "layer": return 2;
			case "order": return 3;
			default: return 1;
		}
	}

	private static int algebraStyleToInt(String val) {
		if (val == null) return -1;
		switch (val) {
			case "value": return 0;
			case "description": return 1;
			case "definition": return 2;
			case "definitionAndValue": return 3;
			case "linearNotation": return 4;
			default: return -1;
		}
	}

	private static int distTypeToInt(String val) {
		switch (val) {
			case "normal": return 0;
			case "student": return 1;
			case "chiSquare": return 2;
			case "f": return 3;
			case "cauchy": return 4;
			case "exponential": return 5;
			case "beta": return 6;
			case "gamma": return 7;
			case "weibull": return 8;
			case "logistic": return 9;
			case "logNormal": return 10;
			case "binomial": return 11;
			case "pascal": return 12;
			case "hypergeometric": return 13;
			case "poisson": return 14;
			default:
				try { return Integer.parseInt(val); }
				catch (NumberFormatException e) { return 0; }
		}
	}

	private static int intervalModeToInt(String val) {
		switch (val) {
			case "interval": return 0;
			case "left": return 1;
			case "right": return 2;
			case "twoTailed": return 3;
			default:
				try { return Integer.parseInt(val); }
				catch (NumberFormatException e) { return 0; }
		}
	}

	// ===================== Existing XML Emission =====================

	private static void emitEuclidianViewXml(EvBlockData data, int viewNo,
			int rightAngleStyle, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("euclidianView", 0).endTag();

		if (viewNo == 2) {
			xml.startTag("viewNumber", 1).attr("viewNo", viewNo).endTag();
		}

		if (data.sizeW > 0 && data.sizeH > 0) {
			xml.startTag("size", 1).attr("width", data.sizeW).attr("height", data.sizeH).endTag();
		}

		emitCoordSystem2D(xml, data);
		emitEvSettings(xml, data, rightAngleStyle);

		if (data.bgColor != null)
			emitColorTag(xml, "bgColor", data.bgColor);
		if (data.axesColor != null)
			emitColorTag(xml, "axesColor", data.axesColor);
		if (data.gridColor != null)
			emitColorTag(xml, "gridColor", data.gridColor);

		if (data.rulerType != null) {
			xml.startTag("rulerType", 1).attr("val", rulerTypeToInt(data.rulerType)).endTag();
		}
		if (data.rulerColor != null) {
			emitColorTag(xml, "rulerColor", data.rulerColor);
		}

		emitLineStyle(xml, data);
		emitLabelStyle(xml, data);

		if (data.xAxis != null) emitAxis(xml, 0, data.xAxis);
		if (data.yAxis != null) emitAxis(xml, 1, data.yAxis);

		emitGridDist(xml, data);

		xml.closeTag("euclidianView");
		sb.append(xml.toString());
	}

	private static void emitEv3dXml(Ev3dBlockData data, int rightAngleStyle, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("euclidianView3D", 0).endTag();

		emitCoordSystem3D(xml, data);
		emitEvSettings(xml, data, rightAngleStyle);

		if (data.bgColor != null)
			emitColorTag(xml, "bgColor", data.bgColor);

		if (data.hasPlane) xml.startTag("plate", 1).attr("show", true).endTag();
		if (data.lightExplicit) xml.startTag("light", 1).attr("val", data.lightVal).endTag();
		if (data.yAxisUp) xml.startTag("yAxisVertical", 1).attr("val", true).endTag();
		if (data.coloredAxes) xml.startTag("coloredAxes", 1).attr("val", true).endTag();

		if (data.useClipping || data.showClipping) {
			xml.startTag("clipping", 1)
					.attr("use", data.useClipping)
					.attr("show", data.showClipping)
					.attr("size", data.clippingReduction)
					.endTag();
		}

		emitProjection(xml, data);
		emitLineStyle(xml, data);
		emitLabelStyle(xml, data);

		if (data.xAxis != null) emitAxis(xml, 0, data.xAxis);
		if (data.yAxis != null) emitAxis(xml, 1, data.yAxis);
		if (data.zAxis != null) emitAxis(xml, 2, data.zAxis);

		xml.closeTag("euclidianView3D");
		sb.append(xml.toString());
	}

	private static void emitCoordSystem2D(XMLStringBuilder xml, EvBlockData data) {
		if (data.csXMin != null && data.csXMax != null
				&& data.csYMin != null && data.csYMax != null) {
			xml.startTag("coordSystem", 1)
					.attrRaw("xMin", data.csXMin)
					.attrRaw("xMax", data.csXMax)
					.attrRaw("yMin", data.csYMin)
					.attrRaw("yMax", data.csYMax)
					.endTag();
		} else if (data.csOriginX != null || data.csScale != null || data.csXscale != null) {
			xml.startTag("coordSystem", 1);
			if (data.csOriginX != null) xml.attrRaw("xZero", data.csOriginX);
			if (data.csOriginY != null) xml.attrRaw("yZero", data.csOriginY);
			if (data.csScale != null) {
				xml.attrRaw("scale", data.csScale);
				xml.attrRaw("yscale", data.csScale);
			} else {
				if (data.csXscale != null) xml.attrRaw("scale", data.csXscale);
				if (data.csYscale != null) xml.attrRaw("yscale", data.csYscale);
			}
			xml.endTag();
		}
	}

	private static void emitCoordSystem3D(XMLStringBuilder xml, Ev3dBlockData data) {
		boolean hasBounded = data.csXMin != null && data.csXMax != null
				&& data.csYMin != null && data.csYMax != null
				&& data.csZMin != null && data.csZMax != null;
		boolean hasContent = data.csOriginX != null || data.csScale != null
				|| data.csXscale != null || data.csXAngle != null || hasBounded;
		if (!hasContent) return;

		xml.startTag("coordSystem", 1);
		if (data.csOriginX != null) xml.attrRaw("xZero", data.csOriginX);
		if (data.csOriginY != null) xml.attrRaw("yZero", data.csOriginY);
		if (data.csOriginZ != null) xml.attrRaw("zZero", data.csOriginZ);
		if (data.csScale != null) {
			xml.attrRaw("scale", data.csScale);
			xml.attrRaw("yscale", data.csScale);
			xml.attrRaw("zscale", data.csScale);
		} else {
			if (data.csXscale != null) xml.attrRaw("scale", data.csXscale);
			if (data.csYscale != null) xml.attrRaw("yscale", data.csYscale);
			if (data.csZscale != null) xml.attrRaw("zscale", data.csZscale);
		}
		if (data.csXAngle != null) xml.attrRaw("xAngle", data.csXAngle);
		if (data.csZAngle != null) xml.attrRaw("zAngle", data.csZAngle);
		if (data.csXMin != null) xml.attrRaw("xMin", data.csXMin);
		if (data.csXMax != null) xml.attrRaw("xMax", data.csXMax);
		if (data.csYMin != null) xml.attrRaw("yMin", data.csYMin);
		if (data.csYMax != null) xml.attrRaw("yMax", data.csYMax);
		if (data.csZMin != null) xml.attrRaw("zMin", data.csZMin);
		if (data.csZMax != null) xml.attrRaw("zMax", data.csZMax);
		xml.endTag();
	}

	private static void emitEvSettings(XMLStringBuilder xml, EvBlockData data, int rightAngleStyle) {
		xml.startTag("evSettings", 1);
		xml.attr("axes", data.hasAxes);
		xml.attr("grid", data.hasGrid);
		xml.attr("gridIsBold", data.gridBold);
		if (data.pointCapturing >= 0)
			xml.attr("pointCapturing", data.pointCapturing);
		if (rightAngleStyle >= 0)
			xml.attr("rightAngleStyle", rightAngleStyle);
		if (data.tooltips >= 0)
			xml.attr("allowToolTips", data.tooltips);
		if (data.mouseCoords)
			xml.attr("allowShowMouseCoords", true);
		xml.attr("checkboxSize", GpadStyleDefaults.ENV_CHECKBOX_SIZE);
		if (data.gridType != Integer.MIN_VALUE)
			xml.attr("gridType", data.gridType);
		if (data.lockedAxesRatio > 0)
			xml.attrRaw("lockedAxesRatio", String.valueOf(data.lockedAxesRatio));
		xml.endTag();
	}

	private static void emitLineStyle(XMLStringBuilder xml, EvBlockData data) {
		if (data.axesLineStyle >= 0 || data.gridLineStyle >= 0) {
			xml.startTag("lineStyle", 1);
			if (data.axesLineStyle >= 0)
				xml.attr("axes", data.axesLineStyle);
			if (data.gridLineStyle >= 0)
				xml.attr("grid", data.gridLineStyle);
			xml.endTag();
		}
	}

	private static void emitLabelStyle(XMLStringBuilder xml, EvBlockData data) {
		if (data.labelFontStyle >= 0 || data.labelSerif) {
			xml.startTag("labelStyle", 1);
			if (data.labelFontStyle >= 0)
				xml.attr("axes", data.labelFontStyle);
			if (data.labelSerif)
				xml.attr("serif", true);
			xml.endTag();
		}
	}

	private static void emitAxis(XMLStringBuilder xml, int id, AxisData axis) {
		xml.startTag("axis", 1);
		xml.attr("id", id);
		xml.attr("show", axis.show);
		if (axis.label != null) xml.attr("label", axis.label);
		if (axis.piUnit) {
			xml.attr("unitLabel", "\u03c0");
		} else if (axis.unit != null) {
			xml.attr("unitLabel", axis.unit);
		}
		xml.attr("showNumbers", axis.showNumbers);
		if (axis.tickStyle != null) {
			xml.attr("tickStyle", tickStyleToInt(axis.tickStyle));
		}
		if (axis.tickExpression != null) {
			xml.attrRaw("tickExpression", axis.tickExpression);
		}
		if (!Double.isNaN(axis.tickDist)) {
			xml.attrRaw("tickDistance", String.valueOf(axis.tickDist));
		}
		if (axis.cross != 0)
			xml.attrRaw("axisCross", String.valueOf(axis.cross));
		if (axis.positive) xml.attr("positiveAxis", true);
		if (axis.crossEdge) xml.attr("drawBorderAxes", true);
		if (!axis.selectable) xml.attr("selectionAllowed", false);
		xml.endTag();
	}

	private static void emitGridDist(XMLStringBuilder xml, EvBlockData data) {
		if (!Double.isNaN(data.gridDistX) || !Double.isNaN(data.gridDistY)) {
			xml.startTag("grid", 1);
			if (!Double.isNaN(data.gridDistX))
				xml.attrRaw("distX", String.valueOf(data.gridDistX));
			if (!Double.isNaN(data.gridDistY))
				xml.attrRaw("distY", String.valueOf(data.gridDistY));
			if (!Double.isNaN(data.gridDistTheta))
				xml.attrRaw("distTheta", String.valueOf(data.gridDistTheta));
			xml.endTag();
		}
	}

	private static void emitProjection(XMLStringBuilder xml, Ev3dBlockData data) {
		if (data.projection >= 0) {
			xml.startTag("projection", 1).attr("type", data.projection);
			if (data.projDistance >= 0)
				xml.attr("distance", data.projDistance);
			if (data.projSeparation >= 0)
				xml.attr("separation", data.projSeparation);
			if (!data.projGrayScaled)
				xml.attr("grayScaled", false);
			if (data.projShutDownGreen)
				xml.attr("shutDownGreen", true);
			if (!Double.isNaN(data.projAngle))
				xml.attrRaw("obliqueAngle", String.valueOf(data.projAngle));
			if (!Double.isNaN(data.projFactor))
				xml.attrRaw("obliqueFactor", String.valueOf(data.projFactor));
			xml.endTag();
		}
	}

	private static void emitColorTag(XMLStringBuilder xml, String tagName, String hexColor) {
		int[] rgb = parseHexColor(hexColor);
		if (rgb == null) return;
		xml.startTag(tagName, 1);
		xml.attr("r", rgb[0]);
		xml.attr("g", rgb[1]);
		xml.attr("b", rgb[2]);
		if (rgb.length > 3 && rgb[3] >= 0) xml.attr("alpha", rgb[3]);
		xml.endTag();
	}

	// ===================== Perspective parsing and emission =====================

	private void parsePerspectiveBlock(PerspectiveData data) {
		expect("{");
		while (pos < tokens.length && !"}".equals(peek())) {
			String tok = peek();
			boolean negated = false;
			if ("~".equals(tok)) {
				negated = true;
				advance();
				tok = peek();
			}
			switch (tok) {
				case "pane":
					advance(); expect(":");
					parsePerspPane(data);
					consumeIf(";");
					break;
				case "view":
					advance();
					parsePerspView(data, negated);
					consumeIf(";");
					break;
				case "toolbar":
					advance(); expect(":");
					parsePerspToolbar(data, negated);
					consumeIf(";");
					break;
				case "input":
					advance(); expect(":");
					parsePerspInput(data, negated);
					consumeIf(";");
					break;
				default:
					advance();
					break;
			}
		}
		consumeIf("}");
	}

	private void parsePerspPane(PerspectiveData data) {
		PerspPaneData pane = new PerspPaneData();
		pane.location = unquote(advance());
		pane.divider = advance();
		String orient = advance();
		pane.orientation = "vertical".equals(orient) ? "0" : "1";
		data.panes.add(pane);
	}

	private void parsePerspView(PerspectiveData data, boolean negatedBefore) {
		PerspViewData view = new PerspViewData();
		String nameOrId = advance();
		view.id = GpadStyleMaps.viewNameToId(nameOrId);
		expect(":");

		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			String tok = peek();
			boolean neg = false;
			if ("~".equals(tok)) {
				neg = true;
				advance();
				tok = peek();
			}
			switch (tok) {
				case "show":
					advance();
					view.visible = neg ? false : true;
					break;
				case "stylebar":
					advance();
					view.stylebar = neg ? false : true;
					break;
				case "location":
					view.location = unquote(advanceValue());
					break;
				case "size":
					view.size = advanceValue();
					break;
				case "toolbar":
					view.toolbar = unquote(advanceValue());
					break;
				case "tab":
					view.tab = GpadStyleMaps.tabNameToId(advanceValue());
					break;
				case "plane":
					view.plane = unquote(advanceValue());
					break;
				default:
					advance();
					break;
			}
		}
		data.views.add(view);
	}

	private void parsePerspToolbar(PerspectiveData data, boolean negated) {
		data.toolbarShow = !negated;
		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			String tok = peek();
			if ("~".equals(tok)) {
				advance();
				tok = peek();
				if ("show".equals(tok)) {
					data.toolbarShow = false;
					advance();
				}
				continue;
			}
			if ("show".equals(tok)) {
				data.toolbarShow = true;
				advance();
			} else if ("north".equals(tok)) {
				data.toolbarPosition = "1";
				advance();
			} else if ("south".equals(tok)) {
				data.toolbarPosition = "3";
				advance();
			} else if ("position".equals(tok)) {
				data.toolbarPosition = advanceValue();
			} else {
				data.toolbarItems = unquote(tok);
				advance();
			}
		}
	}

	private void parsePerspInput(PerspectiveData data, boolean negated) {
		data.inputShow = !negated;
		while (pos < tokens.length && !";".equals(peek()) && !"}".equals(peek())) {
			String tok = peek();
			boolean neg = false;
			if ("~".equals(tok)) {
				neg = true;
				advance();
				tok = peek();
			}
			switch (tok) {
				case "show":
					data.inputShow = !neg;
					advance();
					break;
				case "cmd":
					data.inputCmd = !neg;
					advance();
					break;
				case "top":
					data.inputTop = "true";
					advance();
					break;
				case "bottom":
					data.inputTop = "false";
					advance();
					break;
				case "algebra":
					data.inputTop = "algebra";
					advance();
					break;
				default:
					advance();
					break;
			}
		}
	}

	private static void emitPerspectiveXml(PerspectiveData data, StringBuilder sb) {
		XMLStringBuilder xml = new XMLStringBuilder();
		xml.startOpeningTag("perspectives", 0).endTag();
		xml.startOpeningTag("perspective", 1).attrRaw("id", "tmp").endTag();

		if (!data.panes.isEmpty()) {
			xml.startOpeningTag("panes", 2).endTag();
			for (PerspPaneData pane : data.panes) {
				xml.startTag("pane", 3);
				xml.attrRaw("location", pane.location);
				xml.attrRaw("divider", pane.divider);
				xml.attrRaw("orientation", pane.orientation);
				xml.endTag();
			}
			xml.closeTag("panes");
		}

		if (!data.views.isEmpty()) {
			xml.startOpeningTag("views", 2).endTag();
			for (PerspViewData view : data.views) {
				xml.startTag("view", 3);
				xml.attrRaw("id", view.id);
				if (view.toolbar != null && !view.toolbar.isEmpty())
					xml.attrRaw("toolbar", view.toolbar);
				xml.attrRaw("visible", String.valueOf(view.visible));
				xml.attrRaw("inframe", "false");
				xml.attrRaw("stylebar", String.valueOf(view.stylebar));
				if (view.location != null)
					xml.attrRaw("location", view.location);
				if (view.size != null)
					xml.attrRaw("size", view.size);
				else
					xml.attrRaw("size", "0");
				if (view.tab != null)
					xml.attrRaw("tab", view.tab);
				if (view.plane != null)
					xml.attrRaw("plane", view.plane);
				xml.attrRaw("window", "100,100,600,400");
				xml.endTag();
			}
			xml.closeTag("views");
		}

		// toolbar
		xml.startTag("toolbar", 2);
		xml.attrRaw("show", String.valueOf(data.toolbarShow));
		if (data.toolbarItems != null)
			xml.attrRaw("items", data.toolbarItems);
		if (data.toolbarPosition != null)
			xml.attrRaw("position", data.toolbarPosition);
		else
			xml.attrRaw("position", "1");
		xml.attrRaw("help", "false");
		xml.endTag();

		// input
		xml.startTag("input", 2);
		xml.attrRaw("show", String.valueOf(data.inputShow));
		xml.attrRaw("cmd", String.valueOf(data.inputCmd));
		if (data.inputTop != null)
			xml.attrRaw("top", data.inputTop);
		else
			xml.attrRaw("top", "true");
		xml.endTag();

		// dockBar (always emit default for roundtrip)
		xml.startTag("dockBar", 2);
		xml.attrRaw("show", "false");
		xml.attrRaw("east", "false");
		xml.endTag();

		xml.closeTag("perspective");
		xml.closeTag("perspectives");
		sb.append(xml.toString());
	}

	// ===================== Tokenizer =====================

	private static final String DELIMITER_CHARS = ":;{}~,";

	private static boolean isDelimiter(char c) {
		return DELIMITER_CHARS.indexOf(c) >= 0;
	}

	private static String[] tokenize(String raw) {
		List<String> result = new ArrayList<>();
		int i = 0;
		while (i < raw.length()) {
			char c = raw.charAt(i);
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
				i++;
				continue;
			}
			if (c == '"') {
				int end = raw.indexOf('"', i + 1);
				if (end < 0) end = raw.length();
				result.add(raw.substring(i, end + 1));
				i = end + 1;
			} else if (isDelimiter(c)) {
				result.add(String.valueOf(c));
				i++;
			} else {
				int start = i;
				while (i < raw.length() && raw.charAt(i) != ' ' && raw.charAt(i) != '\t'
						&& raw.charAt(i) != '\r' && raw.charAt(i) != '\n'
						&& !isDelimiter(raw.charAt(i))) {
					i++;
				}
				result.add(raw.substring(start, i));
			}
		}
		return result.toArray(new String[0]);
	}

	private String peek() {
		return pos < tokens.length ? tokens[pos] : "";
	}

	private String advance() {
		return pos < tokens.length ? tokens[pos++] : "";
	}

	private void expect(String expected) {
		if (pos < tokens.length && expected.equals(tokens[pos]))
			pos++;
	}

	private void consumeIf(String token) {
		if (pos < tokens.length && token.equals(tokens[pos]))
			pos++;
	}

	/** Advance past keyword, consume optional '=', return the value token. */
	private String advanceValue() {
		advance();
		consumeIf("=");
		return advance();
	}

	// ===================== Value converters =====================

	private static int[] parseHexColor(String hex) {
		if (hex == null || !hex.startsWith("#")) return null;
		String h = hex.substring(1).toUpperCase();
		try {
			if (h.length() == 6) {
				return new int[]{
						Integer.parseInt(h.substring(0, 2), 16),
						Integer.parseInt(h.substring(2, 4), 16),
						Integer.parseInt(h.substring(4, 6), 16)
				};
			} else if (h.length() == 8) {
				return new int[]{
						Integer.parseInt(h.substring(0, 2), 16),
						Integer.parseInt(h.substring(2, 4), 16),
						Integer.parseInt(h.substring(4, 6), 16),
						Integer.parseInt(h.substring(6, 8), 16)
				};
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return null;
	}

	private static int parseInt(String s) {
		try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
	}

	private static double parseDouble(String s) {
		try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
	}

	private static String unquote(String s) {
		if (s == null) return "";
		if (s.length() >= 6 && s.startsWith("\"\"\"") && s.endsWith("\"\"\""))
			return s.substring(3, s.length() - 3);
		if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
			return s.substring(1, s.length() - 1);
		if (s.length() >= 2 && s.charAt(0) == '`' && s.charAt(s.length() - 1) == '`')
			return s.substring(1, s.length() - 1);
		return s;
	}

	private static int rightAngleStyleToInt(String val) {
		switch (val) {
			case "none": return 0;
			case "square": return 1;
			case "dot": return 2;
			case "L": return 3;
			default: return 1;
		}
	}

	private static int gridTypeToInt(String val) {
		switch (val) {
			case "none": return -1;
			case "cartesian": return 0;
			case "isometric": return 1;
			case "polar": return 2;
			case "cartesianSub": return 3;
			default: return 3;
		}
	}

	private static int pointCapturingToInt(String val) {
		switch (val) {
			case "off": return 0;
			case "snap": return 1;
			case "fixed": return 2;
			case "auto": return 3;
			default: return 3;
		}
	}

	private static int toolTipsToInt(String val) {
		switch (val) {
			case "auto": return 0;
			case "on": return 1;
			case "off": return 2;
			default: return 0;
		}
	}

	private static int lineStyleToInt(String val) {
		switch (val) {
			case "full": return 0;
			case "dashedShort": return 10;
			case "dashedLong": return 15;
			case "dotted": return 20;
			case "dashedDotted": return 30;
			default: return 0;
		}
	}

	private static int axesStyleToInt(String val) {
		switch (val) {
			case "full": return 0;
			case "arrow": return 1;
			case "bold": return 2;
			case "arrowBold": return 3;
			case "twoArrows": return 5;
			case "twoArrowsBold": return 7;
			case "arrowFilled": return 9;
			case "arrowFilledBold": return 11;
			case "twoArrowsFilled": return 13;
			case "twoArrowsFilledBold": return 15;
			default: return 1;
		}
	}

	private static int fontStyleToInt(String val) {
		switch (val) {
			case "plain": return 0;
			case "bold": return 1;
			case "italic": return 2;
			case "boldItalic": return 3;
			default: return 0;
		}
	}

	private static int tickStyleToInt(String val) {
		switch (val) {
			case "majorMinor": return 0;
			case "major": return 1;
			case "none": return 2;
			default: return 0;
		}
	}

	private static int projectionToInt(String val) {
		switch (val) {
			case "orthographic": return 0;
			case "perspective": return 1;
			case "glasses": return 2;
			case "oblique": return 3;
			default: return 0;
		}
	}

	private static int rulerTypeToInt(String val) {
		switch (val) {
			case "none": return 0;
			case "ruler": return 1;
			case "squareSmall": return 2;
			case "squareBig": return 3;
			case "elementary12": return 4;
			case "elementary12House": return 5;
			case "elementary34": return 6;
			case "music": return 7;
			case "svg": return 8;
			case "elementary12Colored": return 9;
			case "isometric": return 10;
			case "polar": return 11;
			case "dots": return 12;
			default: return 0;
		}
	}
}
