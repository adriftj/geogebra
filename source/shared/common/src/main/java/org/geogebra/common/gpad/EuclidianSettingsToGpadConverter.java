package org.geogebra.common.gpad;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.awt.GFont;
import org.geogebra.common.euclidian.EuclidianView;
import org.geogebra.common.euclidian.background.BackgroundType;
import org.geogebra.common.euclidian3D.EuclidianView3DInterface;
import org.geogebra.common.main.App;
import org.geogebra.common.main.settings.EuclidianSettings;
import org.geogebra.common.main.settings.EuclidianSettings3D;
import org.geogebra.common.main.settings.PenToolsSettings;
import org.geogebra.common.plugin.EuclidianStyleConstants;
import org.geogebra.editor.share.util.Unicode;

/**
 * Converts EuclidianSettings to Gpad @@env format.
 * Handles both 2D (ev1, ev2) and 3D (ev3d) views.
 */
public class EuclidianSettingsToGpadConverter {
	
	// Default values for comparison (to avoid outputting defaults)
	private static final double DEFAULT_SCALE = 50.0;
	private static final int DEFAULT_GRID_TYPE = EuclidianView.GRID_CARTESIAN_WITH_SUBGRID;
	private static final int DEFAULT_POINT_CAPTURING = EuclidianStyleConstants.POINT_CAPTURING_AUTOMATIC;
	private static final int DEFAULT_TOOLTIPS = EuclidianStyleConstants.TOOLTIPS_AUTOMATIC;
	private static final GColor DEFAULT_BG_COLOR = GColor.WHITE;
	private static final GColor DEFAULT_AXES_COLOR = GColor.DEFAULT_AXES_COLOR;
	private static final int DEFAULT_TICK_STYLE = EuclidianStyleConstants.AXES_TICK_STYLE_MAJOR_MINOR;
	
	// 3D specific defaults
	private static final int DEFAULT_PROJECTION = EuclidianView3DInterface.PROJECTION_ORTHOGRAPHIC;
	private static final int DEFAULT_PROJECTION_DISTANCE = 1500;
	
	/**
	 * Converts all EuclidianSettings from an App to Gpad @@env format.
	 * 
	 * @param app the application
	 * @return Gpad @@env string, or null if no non-default settings
	 */
	public static String convert(App app) {
		if (app == null) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("@@env {\n");
		
		boolean hasContent = false;
		
		// Global settings at top level
		// Right angle style (global, not view-specific)
		int rightAngleStyle = app.rightAngleStyle;
		String rightAngleStr = rightAngleStyleToString(rightAngleStyle);
		if (rightAngleStr != null && !"square".equals(rightAngleStr)) {
			sb.append("  rightAngleStyle: ").append(rightAngleStr).append(";\n");
			hasContent = true;
		}
		
		// Pen tools settings (global, at top level)
		String penToolsContent = convertPenToolsGlobal(app.getSettings().getPenTools());
		if (penToolsContent != null && !penToolsContent.isEmpty()) {
			sb.append(penToolsContent);
			hasContent = true;
		}
		
		// Convert EV1
		EuclidianSettings ev1Settings = app.getSettings().getEuclidian(1);
		if (ev1Settings != null) {
			String ev1Content = convertEv(ev1Settings, 1, app);
			if (ev1Content != null && !ev1Content.isEmpty()) {
				sb.append("  ev1 {\n");
				sb.append(ev1Content);
				sb.append("  }\n");
				hasContent = true;
			}
		}
		
		// Convert EV2
		EuclidianSettings ev2Settings = app.getSettings().getEuclidian(2);
		if (ev2Settings != null) {
			String ev2Content = convertEv(ev2Settings, 2, app);
			if (ev2Content != null && !ev2Content.isEmpty()) {
				sb.append("  ev2 {\n");
				sb.append(ev2Content);
				sb.append("  }\n");
				hasContent = true;
			}
		}
		
		// Convert EV3D
		EuclidianSettings ev3dSettings = app.getSettings().getEuclidian(3);
		if (ev3dSettings instanceof EuclidianSettings3D) {
			String ev3dContent = convertEv3d((EuclidianSettings3D) ev3dSettings, app);
			if (ev3dContent != null && !ev3dContent.isEmpty()) {
				sb.append("  ev3d {\n");
				sb.append(ev3dContent);
				sb.append("  }\n");
				hasContent = true;
			}
		}
		
		sb.append("}\n");
		
		return hasContent ? sb.toString() : null;
	}
	
	/**
	 * Converts a 2D EuclidianSettings to Gpad format.
	 * 
	 * @param settings the settings to convert
	 * @param evNo the view number (1 or 2)
	 * @param app the application
	 * @return content string (properties only, without ev block wrapper)
	 */
	private static String convertEv(EuclidianSettings settings, int evNo, App app) {
		StringBuilder sb = new StringBuilder();
		
		// Background color
		GColor bgColor = settings.getBackground();
		if (bgColor != null && !bgColor.equals(DEFAULT_BG_COLOR)) {
			sb.append("    bgColor: ").append(colorToHex(bgColor)).append(";\n");
		}
		
		// Axes color
		GColor axesColor = settings.getAxesColor();
		if (axesColor != null && !axesColor.equals(DEFAULT_AXES_COLOR)) {
			sb.append("    axesColor: ").append(colorToHex(axesColor)).append(";\n");
		}
		
		// Grid color
		GColor gridColor = settings.getGridColor();
		if (gridColor != null && !gridColor.equals(GColor.GRAY)) {
			sb.append("    gridColor: ").append(colorToHex(gridColor)).append(";\n");
		}
		
		// Size
		if (settings.getPreferredSize() != null) {
			int width = settings.getPreferredSize().getWidth();
			int height = settings.getPreferredSize().getHeight();
			if (width > 0 && height > 0) {
				sb.append("    size: ").append(width).append(",").append(height).append(";\n");
			}
		}
		
		// Coordinate system (use min/max if available, otherwise use origin/scale)
		appendCoordSystem2D(sb, settings);
		
		// Axes
		boolean[] showAxes = settings.getShowAxes();
		if (!showAxes[0] || !showAxes[1]) {
			sb.append("    ~axes;\n");
		}
		
		// Grid
		if (settings.getShowGrid()) {
			sb.append("    grid;\n");
		}
		
		// Grid type
		int gridType = settings.getGridType();
		if (gridType != DEFAULT_GRID_TYPE) {
			String gridTypeStr = gridTypeToString(gridType);
			if (gridTypeStr != null) {
				sb.append("    gridType: ").append(gridTypeStr).append(";\n");
			}
		}
		
		// Grid bold
		if (settings.getGridIsBold()) {
			sb.append("    gridBold;\n");
		}
		
		// Point capturing
		int pointCapturing = settings.getPointCapturingMode();
		if (pointCapturing != DEFAULT_POINT_CAPTURING) {
			String capturingStr = pointCapturingToString(pointCapturing);
			if (capturingStr != null) {
				sb.append("    pointCapturing: ").append(capturingStr).append(";\n");
			}
		}
		
		// Tooltips
		int tooltips = settings.getAllowToolTips();
		if (tooltips != DEFAULT_TOOLTIPS) {
			String tooltipsStr = toolTipsToString(tooltips);
			if (tooltipsStr != null) {
				sb.append("    toolTips: ").append(tooltipsStr).append(";\n");
			}
		}
		
		// Mouse coords
		if (settings.getAllowShowMouseCoords()) {
			sb.append("    mouseCoords;\n");
		}
		
		// Locked axes ratio
		double lockedRatio = settings.getLockedAxesRatio();
		if (lockedRatio > 0) {
			sb.append("    lockedAxesRatio: ").append(lockedRatio).append(";\n");
		}
		
		// Axes properties
		appendAxisProperties(sb, settings, 0, "xAxis");
		appendAxisProperties(sb, settings, 1, "yAxis");
		
		// Grid distances
		double[] gridDists = settings.getGridDistances();
		if (gridDists != null && gridDists.length >= 2) {
			sb.append("    gridDist: x=").append(gridDists[0])
				.append(" y=").append(gridDists[1]).append(";\n");
		}
		
		// Line style (grid line style)
		int gridLineStyle = settings.getGridLineStyle();
		if (gridLineStyle != EuclidianStyleConstants.LINE_TYPE_FULL) {
			String lineStyleStr = lineStyleToString(gridLineStyle);
			if (lineStyleStr != null) {
				sb.append("    lineStyle: ").append(lineStyleStr).append(";\n");
			}
		}
		
		// Axes style
		int axesLineStyle = settings.getAxesLineStyle();
		if (axesLineStyle != EuclidianStyleConstants.AXES_LINE_TYPE_ARROW) {
			String axesStyleStr = axesStyleToString(axesLineStyle);
			if (axesStyleStr != null) {
				sb.append("    axesStyle: ").append(axesStyleStr).append(";\n");
			}
		}
		
		// Label style (font style)
		int fontStyle = settings.getAxisFontStyle();
		if (fontStyle != GFont.PLAIN) {
			String fontStyleStr = fontStyleToString(fontStyle);
			if (fontStyleStr != null) {
				sb.append("    labelStyle: ").append(fontStyleStr).append(";\n");
			}
		}
		
		// Ruler type (background type)
		BackgroundType bgType = settings.getBackgroundType();
		if (bgType != BackgroundType.NONE) {
			String rulerTypeStr = rulerTypeToString(bgType);
			if (rulerTypeStr != null) {
				sb.append("    rulerType: ").append(rulerTypeStr).append(";\n");
			}
		}
		
		// Ruler color
		GColor rulerColor = settings.getBgRulerColor();
		if (rulerColor != null && !rulerColor.equals(GColor.MOW_RULER)) {
			sb.append("    rulerColor: ").append(colorToHex(rulerColor)).append(";\n");
		}
		
		return sb.toString();
	}
	
	/**
	 * Converts a 3D EuclidianSettings to Gpad format.
	 * 
	 * @param settings the 3D settings to convert
	 * @param app the application
	 * @return content string (properties only, without ev3d block wrapper)
	 */
	private static String convertEv3d(EuclidianSettings3D settings, App app) {
		StringBuilder sb = new StringBuilder();
		
		// Background color
		GColor bgColor = settings.getBackground();
		if (bgColor != null && !bgColor.equals(DEFAULT_BG_COLOR)) {
			sb.append("    bgColor: ").append(colorToHex(bgColor)).append(";\n");
		}
		
		// Coordinate system
		appendCoordSystem3D(sb, settings);
		
		// Axes
		boolean[] showAxes = settings.getShowAxes();
		if (!showAxes[0] || !showAxes[1] || !showAxes[2]) {
			sb.append("    ~axes;\n");
		}
		
		// Grid
		if (settings.getShowGrid()) {
			sb.append("    grid;\n");
		}
		
		// Plane
		if (settings.getShowPlate()) {
			sb.append("    plane;\n");
		}
		
		// Light
		if (settings.getUseLight()) {
			sb.append("    light;\n");
		}
		
		// Y axis up
		if (settings.getYAxisVertical()) {
			sb.append("    yAxisUp;\n");
		}
		
		// Colored axes
		if (settings.getHasColoredAxes()) {
			sb.append("    coloredAxes;\n");
		}
		
		// Clipping
		if (settings.useClippingCube()) {
			sb.append("    clipping: use");
			if (settings.showClippingCube()) {
				sb.append(" show");
			}
			int reduction = settings.getClippingReduction();
			if (reduction != 0) {
				sb.append(" size=").append(reduction);
			}
			sb.append(";\n");
		}
		
		// Projection
		int projection = settings.getProjection();
		if (projection != DEFAULT_PROJECTION) {
			String projStr = projectionToString(projection);
			if (projStr != null) {
				sb.append("    projection: ").append(projStr);
				if (projection == EuclidianView3DInterface.PROJECTION_PERSPECTIVE) {
					int distance = settings.getProjectionPerspectiveEyeDistance();
					if (distance != DEFAULT_PROJECTION_DISTANCE) {
						sb.append(" distance=").append(distance);
					}
				} else if (projection == EuclidianView3DInterface.PROJECTION_GLASSES) {
					int sep = settings.getEyeSep();
					sb.append(" separation=").append(sep);
					// grayScaled (default is true)
					if (!settings.isGlassesGrayScaled()) {
						sb.append(" ~grayScaled");
					}
					// shutDownGreen (default is false)
					if (settings.isGlassesShutDownGreen()) {
						sb.append(" shutDownGreen");
					}
				} else if (projection == EuclidianView3DInterface.PROJECTION_OBLIQUE) {
					double angle = settings.getProjectionObliqueAngle();
					double factor = settings.getProjectionObliqueFactor();
					sb.append(" angle=").append(angle);
					sb.append(" factor=").append(factor);
				}
				sb.append(";\n");
			}
		}
		
		// Axes properties (including z axis)
		appendAxisProperties(sb, settings, 0, "xAxis");
		appendAxisProperties(sb, settings, 1, "yAxis");
		appendAxisProperties(sb, settings, 2, "zAxis");
		
		return sb.toString();
	}
	
	/**
	 * Appends 2D coordinate system properties.
	 */
	private static void appendCoordSystem2D(StringBuilder sb, EuclidianSettings settings) {
		// Check if we have min/max bounds
		if (settings.getXminObject() != null && settings.getXmaxObject() != null
				&& settings.getYminObject() != null && settings.getYmaxObject() != null) {
			sb.append("    coordSystem:");
			sb.append(" xMin=").append(settings.getXminObject().evaluateDouble());
			sb.append(" xMax=").append(settings.getXmaxObject().evaluateDouble());
			sb.append(" yMin=").append(settings.getYminObject().evaluateDouble());
			sb.append(" yMax=").append(settings.getYmaxObject().evaluateDouble());
			sb.append(";\n");
		} else {
			// Use origin/scale format
			double xZero = settings.getXZero();
			double yZero = settings.getYZero();
			double xscale = settings.getXscale();
			double yscale = settings.getYscale();
			
			boolean hasCustomOrigin = xZero != 0 || yZero != 0;
			boolean hasCustomScale = Math.abs(xscale - DEFAULT_SCALE) > 1e-6 
					|| Math.abs(yscale - DEFAULT_SCALE) > 1e-6;
			
			if (hasCustomOrigin || hasCustomScale) {
				sb.append("    coordSystem: origin=").append(xZero).append(",").append(yZero);
				if (Math.abs(xscale - yscale) < 1e-6) {
					sb.append(" scale=").append(xscale);
				} else {
					sb.append(" xscale=").append(xscale);
					sb.append(" yscale=").append(yscale);
				}
				sb.append(";\n");
			}
		}
	}
	
	/**
	 * Appends 3D coordinate system properties.
	 */
	private static void appendCoordSystem3D(StringBuilder sb, EuclidianSettings3D settings) {
		double xZero = settings.getXZero();
		double yZero = settings.getYZero();
		double zZero = settings.getZZero();
		double xscale = settings.getXscale();
		double yscale = settings.getYscale();
		double zscale = settings.getZscale();
		
		boolean hasContent = false;
		StringBuilder coordSb = new StringBuilder();
		coordSb.append("    coordSystem:");
		
		if (xZero != 0 || yZero != 0 || zZero != 0) {
			coordSb.append(" origin=").append(xZero).append(",").append(yZero).append(",").append(zZero);
			hasContent = true;
		}
		
		// Scale
		boolean allScalesSame = Math.abs(xscale - yscale) < 1e-6 && Math.abs(xscale - zscale) < 1e-6;
		if (allScalesSame) {
			if (Math.abs(xscale - DEFAULT_SCALE) > 1e-6) {
				coordSb.append(" scale=").append(xscale);
				hasContent = true;
			}
		} else {
			if (Math.abs(xscale - DEFAULT_SCALE) > 1e-6) {
				coordSb.append(" xscale=").append(xscale);
				hasContent = true;
			}
			if (Math.abs(yscale - DEFAULT_SCALE) > 1e-6) {
				coordSb.append(" yscale=").append(yscale);
				hasContent = true;
			}
			if (Math.abs(zscale - DEFAULT_SCALE) > 1e-6) {
				coordSb.append(" zscale=").append(zscale);
				hasContent = true;
			}
		}
		
		// Note: xAngle and zAngle not available via public API in EuclidianSettings3D
		// Rotation angles would need to be obtained from the view directly
		
		if (hasContent) {
			coordSb.append(";\n");
			sb.append(coordSb);
		}
	}
	
	/**
	 * Appends axis properties if non-default.
	 */
	private static void appendAxisProperties(StringBuilder sb, EuclidianSettings settings, 
			int axisId, String axisName) {
		StringBuilder axisSb = new StringBuilder();
		axisSb.append("    ").append(axisName).append(":");
		boolean hasContent = false;
		
		// Show
		if (!settings.getShowAxis(axisId)) {
			axisSb.append(" ~show");
			hasContent = true;
		}
		
		// Label
		String[] labels = settings.getAxesLabels();
		if (labels != null && axisId < labels.length && labels[axisId] != null && !labels[axisId].isEmpty()) {
			axisSb.append(" label=").append(quoteString(labels[axisId]));
			hasContent = true;
		}
		
		// Unit label / piUnit
		String[] unitLabels = settings.getAxesUnitLabels();
		if (unitLabels != null && axisId < unitLabels.length && unitLabels[axisId] != null && !unitLabels[axisId].isEmpty()) {
			if (Unicode.PI_STRING.equals(unitLabels[axisId])) {
				axisSb.append(" piUnit");
			} else {
				axisSb.append(" unit=").append(quoteString(unitLabels[axisId]));
			}
			hasContent = true;
		}
		
		// Show numbers
		if (!settings.getShowAxisNumbers()[axisId]) {
			axisSb.append(" ~numbers");
			hasContent = true;
		}
		
		// Tick distance
		if (settings.getAxisNumberingDistance(axisId) != null) {
			double tickDist = settings.getAxisNumberingDistance(axisId).evaluateDouble();
			axisSb.append(" tickDist=").append(tickDist);
			hasContent = true;
		}
		
		// Tick style
		int tickStyle = settings.getAxesTickStyles()[axisId];
		if (tickStyle != DEFAULT_TICK_STYLE) {
			String tickStyleStr = tickStyleToString(tickStyle);
			if (tickStyleStr != null) {
				axisSb.append(" tickStyle=").append(tickStyleStr);
				hasContent = true;
			}
		}
		
		// Axis cross
		double[] axisCross = settings.getAxesCross();
		if (axisCross != null && axisId < axisCross.length && axisCross[axisId] != 0) {
			axisSb.append(" cross=").append(axisCross[axisId]);
			hasContent = true;
		}
		
		// Draw at border
		boolean[] drawBorderAxes = settings.getDrawBorderAxes();
		if (drawBorderAxes != null && axisId < drawBorderAxes.length && drawBorderAxes[axisId]) {
			axisSb.append(" crossEdge");
			hasContent = true;
		}
		
		// Positive only
		boolean[] positiveAxes = settings.getPositiveAxes();
		if (positiveAxes != null && axisId < positiveAxes.length && positiveAxes[axisId]) {
			axisSb.append(" positive");
			hasContent = true;
		}
		
		// Selection allowed
		if (!settings.isSelectionAllowed(axisId)) {
			axisSb.append(" ~selectable");
			hasContent = true;
		}
		
		if (hasContent) {
			axisSb.append(";\n");
			sb.append(axisSb);
		}
	}
	
	// ==================== Enum conversion methods ====================
	
	/**
	 * Converts grid type integer to Gpad string.
	 */
	private static String gridTypeToString(int gridType) {
		switch (gridType) {
			case EuclidianView.GRID_NOT_SHOWN:
				return "none";
			case EuclidianView.GRID_CARTESIAN:
				return "cartesian";
			case EuclidianView.GRID_ISOMETRIC:
				return "isometric";
			case EuclidianView.GRID_POLAR:
				return "polar";
			case EuclidianView.GRID_CARTESIAN_WITH_SUBGRID:
				return "cartesianSub";
			default:
				return null;
		}
	}
	
	/**
	 * Converts point capturing mode to Gpad string.
	 */
	private static String pointCapturingToString(int mode) {
		switch (mode) {
			case EuclidianStyleConstants.POINT_CAPTURING_OFF:
				return "off";
			case EuclidianStyleConstants.POINT_CAPTURING_ON:
				return "snap";
			case EuclidianStyleConstants.POINT_CAPTURING_ON_GRID:
				return "fixed";
			case EuclidianStyleConstants.POINT_CAPTURING_AUTOMATIC:
				return "auto";
			default:
				return null;
		}
	}
	
	/**
	 * Converts right angle style to Gpad string.
	 */
	private static String rightAngleStyleToString(int style) {
		switch (style) {
			case EuclidianStyleConstants.RIGHT_ANGLE_STYLE_NONE:
				return "none";
			case EuclidianStyleConstants.RIGHT_ANGLE_STYLE_SQUARE:
				return "square";
			case EuclidianStyleConstants.RIGHT_ANGLE_STYLE_DOT:
				return "dot";
			case EuclidianStyleConstants.RIGHT_ANGLE_STYLE_L:
				return "L";
			default:
				return null;
		}
	}
	
	/**
	 * Converts tick style to Gpad string.
	 */
	private static String tickStyleToString(int style) {
		switch (style) {
			case EuclidianStyleConstants.AXES_TICK_STYLE_MAJOR_MINOR:
				return "majorMinor";
			case EuclidianStyleConstants.AXES_TICK_STYLE_MAJOR:
				return "major";
			case EuclidianStyleConstants.AXES_TICK_STYLE_NONE:
				return "none";
			default:
				return null;
		}
	}
	
	/**
	 * Converts tooltips mode to Gpad string.
	 */
	private static String toolTipsToString(int mode) {
		switch (mode) {
			case EuclidianStyleConstants.TOOLTIPS_AUTOMATIC:
				return "auto";
			case EuclidianStyleConstants.TOOLTIPS_ON:
				return "on";
			case EuclidianStyleConstants.TOOLTIPS_OFF:
				return "off";
			default:
				return null;
		}
	}
	
	/**
	 * Converts projection type to Gpad string.
	 */
	private static String projectionToString(int type) {
		switch (type) {
			case EuclidianView3DInterface.PROJECTION_ORTHOGRAPHIC:
				return "orthographic";
			case EuclidianView3DInterface.PROJECTION_PERSPECTIVE:
				return "perspective";
			case EuclidianView3DInterface.PROJECTION_GLASSES:
				return "glasses";
			case EuclidianView3DInterface.PROJECTION_OBLIQUE:
				return "oblique";
			default:
				return null;
		}
	}
	
	// ==================== Utility methods ====================
	
	/**
	 * Converts a GColor to hex string (#RRGGBB or #RRGGBBAA).
	 */
	private static String colorToHex(GColor color) {
		if (color == null) {
			return "#000000";
		}
		
		StringBuilder sb = new StringBuilder("#");
		sb.append(toHex2Digits(color.getRed()));
		sb.append(toHex2Digits(color.getGreen()));
		sb.append(toHex2Digits(color.getBlue()));
		
		// Only add alpha if not fully opaque
		if (color.getAlpha() < 255) {
			sb.append(toHex2Digits(color.getAlpha()));
		}
		
		return sb.toString();
	}
	
	/**
	 * Converts an integer (0-255) to a 2-digit uppercase hex string.
	 */
	private static String toHex2Digits(int value) {
		value = Math.max(0, Math.min(255, value));
		char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		StringBuilder sb = new StringBuilder(2);
		sb.append(hexChars[(value >> 4) & 0xF]);
		sb.append(hexChars[value & 0xF]);
		return sb.toString();
	}
	
	/**
	 * Escapes special characters in a string for Gpad.
	 */
	private static String quoteString(String str) {
		return StyleMapToGpadConverter.quoteString(str);
	}
	
	/**
	 * Converts grid line style to Gpad string.
	 */
	private static String lineStyleToString(int style) {
		switch (style) {
			case EuclidianStyleConstants.LINE_TYPE_FULL:
				return "full";
			case EuclidianStyleConstants.LINE_TYPE_DASHED_SHORT:
				return "dashedShort";
			case EuclidianStyleConstants.LINE_TYPE_DASHED_LONG:
				return "dashedLong";
			case EuclidianStyleConstants.LINE_TYPE_DOTTED:
				return "dotted";
			case EuclidianStyleConstants.LINE_TYPE_DASHED_DOTTED:
				return "dashedDotted";
			default:
				return null;
		}
	}
	
	/**
	 * Converts axes line style to Gpad string.
	 */
	private static String axesStyleToString(int style) {
		switch (style) {
			case EuclidianStyleConstants.AXES_LINE_TYPE_FULL:
				return "full";
			case EuclidianStyleConstants.AXES_LINE_TYPE_ARROW:
				return "arrow";
			case EuclidianStyleConstants.AXES_LINE_TYPE_ARROW_FILLED:
				return "arrowFilled";
			case EuclidianStyleConstants.AXES_LINE_TYPE_TWO_ARROWS:
				return "twoArrows";
			case EuclidianStyleConstants.AXES_LINE_TYPE_TWO_ARROWS_FILLED:
				return "twoArrowsFilled";
			case EuclidianStyleConstants.AXES_LINE_TYPE_FULL_BOLD:
				return "bold";
			case EuclidianStyleConstants.AXES_LINE_TYPE_ARROW_BOLD:
				return "arrowBold";
			case EuclidianStyleConstants.AXES_LINE_TYPE_ARROW_FILLED_BOLD:
				return "arrowFilledBold";
			case EuclidianStyleConstants.AXES_LINE_TYPE_TWO_ARROWS_BOLD:
				return "twoArrowsBold";
			case EuclidianStyleConstants.AXES_LINE_TYPE_TWO_ARROWS_FILLED_BOLD:
				return "twoArrowsFilledBold";
			default:
				return null;
		}
	}
	
	/**
	 * Converts font style to Gpad string.
	 */
	private static String fontStyleToString(int style) {
		if (style == GFont.PLAIN) {
			return "plain";
		} else if (style == GFont.BOLD) {
			return "bold";
		} else if (style == GFont.ITALIC) {
			return "italic";
		} else if (style == (GFont.BOLD | GFont.ITALIC)) {
			return "boldItalic";
		}
		return null;
	}
	
	/**
	 * Converts BackgroundType to Gpad string.
	 */
	private static String rulerTypeToString(BackgroundType type) {
		if (type == null) {
			return null;
		}
		switch (type) {
			case NONE:
				return "none";
			case RULER:
				return "ruler";
			case SQUARE_SMALL:
				return "squareSmall";
			case SQUARE_BIG:
				return "squareBig";
			case ELEMENTARY12:
				return "elementary12";
			case ELEMENTARY12_HOUSE:
				return "elementary12House";
			case ELEMENTARY34:
				return "elementary34";
			case MUSIC:
				return "music";
			case SVG:
				return "svg";
			case ELEMENTARY12_COLORED:
				return "elementary12Colored";
			case ISOMETRIC:
				return "isometric";
			case POLAR:
				return "polar";
			case DOTS:
				return "dots";
			default:
				return null;
		}
	}
	
	/**
	 * Converts PenToolsSettings to Gpad format (at @@env top level).
	 * pen, highlighter, eraser are at @@env top level (not nested in any ev block).
	 */
	private static String convertPenToolsGlobal(PenToolsSettings settings) {
		if (settings == null) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		StringBuilder penSb = new StringBuilder();
		StringBuilder highlighterSb = new StringBuilder();
		StringBuilder eraserSb = new StringBuilder();
		
		// Pen settings
		GColor penColor = settings.getLastSelectedPenColor();
		if (penColor != null && !penColor.equals(GColor.BLACK)) {
			penSb.append("    color: ").append(colorToHex(penColor)).append(";\n");
		}
		int penThickness = settings.getLastPenThickness();
		if (penThickness != 5) { // DEFAULT_PEN_SIZE
			penSb.append("    thickness: ").append(penThickness).append(";\n");
		}
		int penOpacity = settings.getLastPenOpacity();
		if (penOpacity != 255) {
			penSb.append("    opacity: ").append(penOpacity).append(";\n");
		}
		
		// Highlighter settings
		GColor highlighterColor = settings.getLastSelectedHighlighterColor();
		if (highlighterColor != null) {
			highlighterSb.append("    color: ").append(colorToHex(highlighterColor)).append(";\n");
		}
		int highlighterThickness = settings.getLastHighlighterThickness();
		if (highlighterThickness != 20) { // DEFAULT_HIGHLIGHTER_SIZE
			highlighterSb.append("    thickness: ").append(highlighterThickness).append(";\n");
		}
		
		// Eraser settings
		int eraserSize = settings.getDeleteToolSize();
		if (eraserSize != 40) { // DEFAULT_ERASER_SIZE
			eraserSb.append("    size: ").append(eraserSize).append(";\n");
		}
		
		// Output each tool block at @@env top level
		boolean hasPen = penSb.length() > 0;
		boolean hasHighlighter = highlighterSb.length() > 0;
		boolean hasEraser = eraserSb.length() > 0;
		
		if (hasPen) {
			sb.append("  pen {\n").append(penSb).append("  }\n");
		}
		if (hasHighlighter) {
			sb.append("  highlighter {\n").append(highlighterSb).append("  }\n");
		}
		if (hasEraser) {
			sb.append("  eraser {\n").append(eraserSb).append("  }\n");
		}
		
		return sb.toString();
	}
}
