package org.geogebra.common.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geogebra.common.gpad.GpadParseException;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;

/**
 * Helper class to apply Gpad style sheets to GeoElements using ConsElementXMLHandler.
 * This class is in the same package as ConsElementXMLHandler so it can access
 * the protected startGeoElement method.
 */
public class GpadStyleXMLApplier {
	
	/**
	 * Map of required attributes for each XML element.
	 * Key: element name (e.g., "value", "slider")
	 * Value: Map of attribute name -> default value
	 * 
	 * This map records attributes that will throw exceptions if missing when
	 * parsed by ConsElementXMLHandler. Missing attributes will be filled with
	 * default values before applying styles.
	 */
	private static final Map<String, Map<String, String>> REQUIRED_ATTRIBUTES = new HashMap<>();
	
	static {
		// Initialize required attributes map
		initRequiredAttributes();
	}
	
	/**
	 * Initialize the required attributes map with all known required attributes
	 * and their default values.
	 */
	private static void initRequiredAttributes() {
		// value element: val attribute is required for GeoNumeric, GeoBoolean, GeoButton
		// Note: Default value depends on object type, handled separately
		Map<String, String> valueAttrs = new HashMap<>();
		valueAttrs.put("val", ""); // Will be set based on object type
		REQUIRED_ATTRIBUTES.put("value", valueAttrs);
		
		// slider element
		Map<String, String> sliderAttrs = new HashMap<>();
		sliderAttrs.put("width", "200"); // Default slider width
		REQUIRED_ATTRIBUTES.put("slider", sliderAttrs);
		
		// absoluteScreenLocation element
		Map<String, String> absScreenAttrs = new HashMap<>();
		absScreenAttrs.put("x", "0");
		absScreenAttrs.put("y", "0");
		REQUIRED_ATTRIBUTES.put("absoluteScreenLocation", absScreenAttrs);
		
		// cropBox element
		Map<String, String> cropBoxAttrs = new HashMap<>();
		cropBoxAttrs.put("x", "0");
		cropBoxAttrs.put("y", "0");
		cropBoxAttrs.put("width", "100");
		cropBoxAttrs.put("height", "100");
		REQUIRED_ATTRIBUTES.put("cropBox", cropBoxAttrs);
		
		// video element
		Map<String, String> videoAttrs = new HashMap<>();
		videoAttrs.put("width", "320");
		videoAttrs.put("height", "240");
		REQUIRED_ATTRIBUTES.put("video", videoAttrs);
		
		// spreadsheetTrace element
		Map<String, String> spreadsheetTraceAttrs = new HashMap<>();
		spreadsheetTraceAttrs.put("traceColumn1", "0");
		spreadsheetTraceAttrs.put("traceColumn2", "0");
		spreadsheetTraceAttrs.put("traceRow1", "0");
		spreadsheetTraceAttrs.put("traceRow2", "0");
		spreadsheetTraceAttrs.put("tracingRow", "0");
		spreadsheetTraceAttrs.put("numRows", "0");
		spreadsheetTraceAttrs.put("headerOffset", "0");
		REQUIRED_ATTRIBUTES.put("spreadsheetTrace", spreadsheetTraceAttrs);
		
		// pointSize element
		Map<String, String> pointSizeAttrs = new HashMap<>();
		pointSizeAttrs.put("val", "4");
		REQUIRED_ATTRIBUTES.put("pointSize", pointSizeAttrs);
		
		// pointStyle element
		Map<String, String> pointStyleAttrs = new HashMap<>();
		pointStyleAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("pointStyle", pointStyleAttrs);
		
		// layer element
		Map<String, String> layerAttrs = new HashMap<>();
		layerAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("layer", layerAttrs);
		
		// lineStyle element
		Map<String, String> lineStyleAttrs = new HashMap<>();
		lineStyleAttrs.put("type", "0");
		lineStyleAttrs.put("thickness", "1");
		REQUIRED_ATTRIBUTES.put("lineStyle", lineStyleAttrs);
		
		// decoration element
		Map<String, String> decorationAttrs = new HashMap<>();
		decorationAttrs.put("type", "0");
		REQUIRED_ATTRIBUTES.put("decoration", decorationAttrs);
		
		// headStyle element
		Map<String, String> headStyleAttrs = new HashMap<>();
		headStyleAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("headStyle", headStyleAttrs);
		
		// arcSize element
		Map<String, String> arcSizeAttrs = new HashMap<>();
		arcSizeAttrs.put("val", "30");
		REQUIRED_ATTRIBUTES.put("arcSize", arcSizeAttrs);
		
		// angleStyle element
		Map<String, String> angleStyleAttrs = new HashMap<>();
		angleStyleAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("angleStyle", angleStyleAttrs);
		
		// slopeTriangleSize element
		Map<String, String> slopeTriangleSizeAttrs = new HashMap<>();
		slopeTriangleSizeAttrs.put("val", "1");
		REQUIRED_ATTRIBUTES.put("slopeTriangleSize", slopeTriangleSizeAttrs);
		
		// decimals element
		Map<String, String> decimalsAttrs = new HashMap<>();
		decimalsAttrs.put("val", "10");
		REQUIRED_ATTRIBUTES.put("decimals", decimalsAttrs);
		
		// significantfigures element
		Map<String, String> figuresAttrs = new HashMap<>();
		figuresAttrs.put("val", "5");
		REQUIRED_ATTRIBUTES.put("significantfigures", figuresAttrs);
		
		// labelOffset element
		Map<String, String> labelOffsetAttrs = new HashMap<>();
		labelOffsetAttrs.put("x", "0");
		labelOffsetAttrs.put("y", "0");
		REQUIRED_ATTRIBUTES.put("labelOffset", labelOffsetAttrs);
		
		// labelMode element
		Map<String, String> labelModeAttrs = new HashMap<>();
		labelModeAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("labelMode", labelModeAttrs);
		
		// tooltipMode element
		Map<String, String> tooltipModeAttrs = new HashMap<>();
		tooltipModeAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("tooltipMode", tooltipModeAttrs);
		
		// ordering element
		Map<String, String> orderingAttrs = new HashMap<>();
		orderingAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("ordering", orderingAttrs);
		
		// selectedIndex element
		Map<String, String> selectedIndexAttrs = new HashMap<>();
		selectedIndexAttrs.put("val", "0");
		REQUIRED_ATTRIBUTES.put("selectedIndex", selectedIndexAttrs);
		
		// borderColor element
		Map<String, String> borderColorAttrs = new HashMap<>();
		borderColorAttrs.put("r", "0");
		borderColorAttrs.put("g", "0");
		borderColorAttrs.put("b", "0");
		REQUIRED_ATTRIBUTES.put("borderColor", borderColorAttrs);
		
		// boundingBox element
		Map<String, String> boundingBoxAttrs = new HashMap<>();
		boundingBoxAttrs.put("width", "100");
		boundingBoxAttrs.put("height", "50");
		REQUIRED_ATTRIBUTES.put("boundingBox", boundingBoxAttrs);
		
		// embed element
		Map<String, String> embedAttrs = new HashMap<>();
		embedAttrs.put("id", "0");
		REQUIRED_ATTRIBUTES.put("embed", embedAttrs);
		
		// matrix element (for conic/quadric - A0-A5 or A0-A9)
		// Note: These are conditionally required based on needsValuesFromXML
		// We'll handle them separately if needed
		
		// tag element (for chart styles)
		Map<String, String> tagAttrs = new HashMap<>();
		tagAttrs.put("key", "");
		tagAttrs.put("value", "");
		tagAttrs.put("barNumber", "0");
		REQUIRED_ATTRIBUTES.put("tag", tagAttrs);
		
		// length element
		Map<String, String> lengthAttrs = new HashMap<>();
		lengthAttrs.put("val", "10");
		REQUIRED_ATTRIBUTES.put("length", lengthAttrs);
		
		// font element
		// Note: If sizeM is null, then size is required
		// We'll handle this in fillRequiredAttributes
		Map<String, String> fontAttrs = new HashMap<>();
		fontAttrs.put("size", "12"); // Required if sizeM is null
		REQUIRED_ATTRIBUTES.put("font", fontAttrs);
		
		// contentSize element
		// Note: Has try-catch, but we'll provide defaults to avoid errors
		Map<String, String> contentSizeAttrs = new HashMap<>();
		contentSizeAttrs.put("width", "100");
		contentSizeAttrs.put("height", "100");
		REQUIRED_ATTRIBUTES.put("contentSize", contentSizeAttrs);
		
		// javascript element
		// Note: val attribute is required (default empty string if missing)
		Map<String, String> javascriptAttrs = new HashMap<>();
		javascriptAttrs.put("val", "");
		REQUIRED_ATTRIBUTES.put("javascript", javascriptAttrs);
	}
	
	/**
	 * Fills missing required attributes with default values.
	 * 
	 * @param tagName
	 *            XML element name
	 * @param attrs
	 *            attributes map to fill
	 * @param geo
	 *            GeoElement (needed for type-specific defaults)
	 */
	private static void fillRequiredAttributes(String tagName,
			LinkedHashMap<String, String> attrs, GeoElement geo) {
		Map<String, String> required = REQUIRED_ATTRIBUTES.get(tagName);
		if (required == null) {
			return;
		}
		
		// Special handling for font element: if sizeM is null, size is required
		if ("font".equals(tagName)) {
			if (!attrs.containsKey("sizeM") || attrs.get("sizeM") == null) {
				if (!attrs.containsKey("size") || attrs.get("size") == null) {
					attrs.put("size", "12");
				}
			}
		}
		
		for (Map.Entry<String, String> entry : required.entrySet()) {
			String attrName = entry.getKey();
			String defaultValue = entry.getValue();
			
			// Check if attribute is missing
			if (!attrs.containsKey(attrName) || attrs.get(attrName) == null) {
				// Special handling for value element's val attribute
				if ("value".equals(tagName) && "val".equals(attrName)) {
					defaultValue = getDefaultValueForValueElement(geo);
				}
				attrs.put(attrName, defaultValue);
			}
		}
	}
	
	/**
	 * Gets the default value for the value element's val attribute based on
	 * the GeoElement type.
	 * 
	 * @param geo
	 *            GeoElement
	 * @return default value string
	 */
	private static String getDefaultValueForValueElement(GeoElement geo) {
		if (geo instanceof GeoNumeric) {
			GeoNumeric num = (GeoNumeric) geo;
			// Use current value if available, otherwise use 0
			return num.isDefined() ? String.valueOf(num.getValue()) : "0";
		} else if (geo instanceof GeoBoolean) {
			GeoBoolean bool = (GeoBoolean) geo;
			// Use current value if available, otherwise use false
			return bool.isDefined() ? String.valueOf(bool.getBoolean()) : "false";
		} else if (geo instanceof GeoButton) {
			// For buttons, use empty string for script
			return "";
		}
		// Fallback
		return "0";
	}
	
	/**
	 * Applies a style sheet to a GeoElement using ConsElementXMLHandler.
	 * 
	 * @param geo
	 *            GeoElement to apply styles to
	 * @param styleSheet
	 *            style sheet to apply
	 * @throws GpadParseException
	 *             if application fails
	 */
	public static void apply(GpadStyleSheet styleSheet, GeoElement geo) throws GpadParseException {
		if (geo == null || styleSheet == null) {
			return;
		}

		Kernel kernel = geo.getKernel();
		MyXMLHandler myXMLHandler = kernel.newMyXMLHandler(kernel.getConstruction());
		ConsElementXMLHandler xmlHandler = new ConsElementXMLHandler(myXMLHandler, kernel.getApplication());

		// Initialize the handler with the geo element
		LinkedHashMap<String, String> initAttrs = new LinkedHashMap<>();
		String label = geo.getLabelSimple();
		if (label == null || label.isEmpty()) {
			throw new GpadParseException("GeoElement must have a label to apply styles");
		}
		initAttrs.put("label", label);
		initAttrs.put("type", geo.getGeoClassType().toString().toLowerCase());
		xmlHandler.init(initAttrs);

		// Apply each property using ConsElementXMLHandler
		// We can access startGeoElement because we're in the same package
		ArrayList<String> errors = new ArrayList<>();
		Map<String, LinkedHashMap<String, String>> properties = styleSheet.getProperties();
		
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : properties.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();
			if (attrs != null) {
				// Fill missing required attributes before applying
				fillRequiredAttributes(tagName, attrs, geo);
				xmlHandler.startGeoElement(tagName, attrs, errors);
			}
		}

		// Process deferred lists (e.g., minMaxList for slider min/max)
		xmlHandler.processLists();
		
		// Finish processing
		xmlHandler.finish();

		if (!errors.isEmpty()) {
			throw new GpadParseException("Failed to apply style sheet: " + errors.toString());
		}
	}
}
