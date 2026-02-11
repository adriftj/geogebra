package org.geogebra.desktop.gpadtools;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.awt.GColor;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.PointProperties;
import org.geogebra.common.util.DoubleUtil;

/**
 * Stores comparison result for a single Geo object between original and converted constructions.
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
		public final String property;
		public final String originalValue;
		public final String convertedValue;
		
		public PropertyDifference(String property, String originalValue, String convertedValue) {
			this.property = property;
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
	 * @param property property name
	 * @param originalValue original value
	 * @param convertedValue converted value
	 */
	public void addDifference(String property, String originalValue, String convertedValue) {
		differences.add(new PropertyDifference(property, originalValue, convertedValue));
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
	 * Compares two GeoElements and populates this result with differences.
	 * @param original the original GeoElement
	 * @param converted the converted GeoElement
	 */
	public void compare(GeoElement original, GeoElement converted) {
		if (original == null || converted == null) {
			return;
		}
		
		// Set types
		originalType = original.getTypeString();
		convertedType = converted.getTypeString();
		
		// Compare type
		if (!originalType.equals(convertedType)) {
			addDifference("type", originalType, convertedType);
		}
		
		// Compare value using isEqual
		if (!original.isEqual(converted)) {
			String origDef = getDefinitionString(original);
			String convDef = getDefinitionString(converted);
			if (!origDef.equals(convDef)) {
				addDifference("value", origDef, convDef);
			}
		}
		
		// Compare style properties
		compareStyles(original, converted);
		
		// Compare visibility
		compareVisibility(original, converted);
		
		// Compare label properties
		compareLabelProperties(original, converted);
	}
	
	/**
	 * Gets the definition string for a GeoElement.
	 */
	private String getDefinitionString(GeoElement geo) {
		if (geo.getDefinition() != null) {
			return geo.getDefinition().toString(StringTemplate.defaultTemplate);
		}
		return geo.toValueString(StringTemplate.defaultTemplate);
	}
	
	/**
	 * Compares style properties between two GeoElements.
	 */
	private void compareStyles(GeoElement original, GeoElement converted) {
		// Color
		GColor origColor = original.getObjectColor();
		GColor convColor = converted.getObjectColor();
		if (!colorsEqual(origColor, convColor)) {
			addDifference("color", colorToString(origColor), colorToString(convColor));
		}
		
		// Alpha (transparency)
		double origAlpha = original.getAlphaValue();
		double convAlpha = converted.getAlphaValue();
		if (!DoubleUtil.isEqual(origAlpha, convAlpha, 0.01)) {
			addDifference("alpha", String.valueOf(origAlpha), String.valueOf(convAlpha));
		}
		
		// Line thickness
		int origThickness = original.getLineThickness();
		int convThickness = converted.getLineThickness();
		if (origThickness != convThickness) {
			addDifference("lineThickness", String.valueOf(origThickness), String.valueOf(convThickness));
		}
		
		// Line type
		int origLineType = original.getLineType();
		int convLineType = converted.getLineType();
		if (origLineType != convLineType) {
			addDifference("lineType", String.valueOf(origLineType), String.valueOf(convLineType));
		}
		
		// Point size (if applicable)
		if (original instanceof PointProperties && converted instanceof PointProperties) {
			int origPointSize = ((PointProperties) original).getPointSize();
			int convPointSize = ((PointProperties) converted).getPointSize();
			if (origPointSize != convPointSize) {
				addDifference("pointSize", String.valueOf(origPointSize), String.valueOf(convPointSize));
			}
			
			int origPointStyle = ((PointProperties) original).getPointStyle();
			int convPointStyle = ((PointProperties) converted).getPointStyle();
			if (origPointStyle != convPointStyle) {
				addDifference("pointStyle", String.valueOf(origPointStyle), String.valueOf(convPointStyle));
			}
		}
		
		// Fill type
		int origFillType = original.getFillType().ordinal();
		int convFillType = converted.getFillType().ordinal();
		if (origFillType != convFillType) {
			addDifference("fillType", original.getFillType().name(), converted.getFillType().name());
		}
		
		// Layer
		int origLayer = original.getLayer();
		int convLayer = converted.getLayer();
		if (origLayer != convLayer) {
			addDifference("layer", String.valueOf(origLayer), String.valueOf(convLayer));
		}
	}
	
	/**
	 * Compares visibility properties between two GeoElements.
	 */
	private void compareVisibility(GeoElement original, GeoElement converted) {
		// Euclidian visibility
		boolean origVisible = original.isEuclidianVisible();
		boolean convVisible = converted.isEuclidianVisible();
		if (origVisible != convVisible) {
			addDifference("euclidianVisible", String.valueOf(origVisible), String.valueOf(convVisible));
		}
		
		// Label visibility
		boolean origLabelVisible = original.isLabelVisible();
		boolean convLabelVisible = converted.isLabelVisible();
		if (origLabelVisible != convLabelVisible) {
			addDifference("labelVisible", String.valueOf(origLabelVisible), String.valueOf(convLabelVisible));
		}
		
		// Algebra visibility
		boolean origAlgVisible = original.isAlgebraVisible();
		boolean convAlgVisible = converted.isAlgebraVisible();
		if (origAlgVisible != convAlgVisible) {
			addDifference("algebraVisible", String.valueOf(origAlgVisible), String.valueOf(convAlgVisible));
		}
		
		// Fixed
		boolean origFixed = original.isLocked();
		boolean convFixed = converted.isLocked();
		if (origFixed != convFixed) {
			addDifference("fixed", String.valueOf(origFixed), String.valueOf(convFixed));
		}
		
		// Selectable
		boolean origSelectable = original.isSelectionAllowed(null);
		boolean convSelectable = converted.isSelectionAllowed(null);
		if (origSelectable != convSelectable) {
			addDifference("selectable", String.valueOf(origSelectable), String.valueOf(convSelectable));
		}
	}
	
	/**
	 * Compares label properties between two GeoElements.
	 */
	private void compareLabelProperties(GeoElement original, GeoElement converted) {
		// Caption
		String origCaption = original.getCaptionSimple();
		String convCaption = converted.getCaptionSimple();
		if (!stringsEqual(origCaption, convCaption)) {
			addDifference("caption", origCaption == null ? "" : origCaption, 
					convCaption == null ? "" : convCaption);
		}
		
		// Label mode
		int origLabelMode = original.getLabelMode();
		int convLabelMode = converted.getLabelMode();
		if (origLabelMode != convLabelMode) {
			addDifference("labelMode", String.valueOf(origLabelMode), String.valueOf(convLabelMode));
		}
	}
	
	/**
	 * Compares two colors for equality.
	 */
	private boolean colorsEqual(GColor c1, GColor c2) {
		if (c1 == c2) return true;
		if (c1 == null || c2 == null) return false;
		return c1.getRed() == c2.getRed() 
				&& c1.getGreen() == c2.getGreen() 
				&& c1.getBlue() == c2.getBlue();
	}
	
	/**
	 * Converts a color to a hex string.
	 */
	private String colorToString(GColor color) {
		if (color == null) return "null";
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}
	
	/**
	 * Compares two strings for equality, treating null as empty.
	 */
	private boolean stringsEqual(String s1, String s2) {
		if (s1 == null) s1 = "";
		if (s2 == null) s2 = "";
		return s1.equals(s2);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(label).append(": ").append(status);
		if (status == Status.DIFFERENT) {
			sb.append(" (").append(differences.size()).append(" differences)");
			for (PropertyDifference diff : differences) {
				sb.append("\n  - ").append(diff.property)
					.append(": ").append(diff.originalValue)
					.append(" -> ").append(diff.convertedValue);
			}
		}
		return sb.toString();
	}
}
