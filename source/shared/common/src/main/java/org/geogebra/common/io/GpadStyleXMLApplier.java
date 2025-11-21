package org.geogebra.common.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geogebra.common.gpad.GpadParseException;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.gpad.XMLToStyleMapParser;
import org.geogebra.common.kernel.ConstructionDefaults;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.Traceable;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.plugin.ScriptManager;
import org.geogebra.common.util.debug.Log;

/**
 * Helper class to apply Gpad style sheets to GeoElements using ConsElementXMLHandler.
 * This class is in the same package as ConsElementXMLHandler so it can access
 * the protected startGeoElement method.
 */
public class GpadStyleXMLApplier {
	/**
	 * Reset marker key used in attribute maps to indicate that a property should be reset to its default value.
	 * The value "~" is chosen because it cannot appear as a valid attribute name.
	 */
	private static final String RESET_MARKER = "~";
	
	/**
	 * Cache for default style maps by default type.
	 * Key: default type (Integer)
	 * Value: Map from tag names to default attribute maps
	 */
	private static final Map<Integer, Map<String, LinkedHashMap<String, String>>> DEFAULT_STYLE_MAP_CACHE = 
			new ConcurrentHashMap<>();
	/**
	 * Map of required attributes for each XML element.
	 * Key: element name (e.g., "value", "slider")
	 * Value: Map of attribute name -> default value
	 * 
	 * This map records attributes that will throw exceptions if missing when
	 * parsed by ConsElementXMLHandler. Missing attributes will be filled with
	 * default values before applying styles.
	 */
	private static final Map<String, Map<String, String>> REQUIRED_ATTRIBUTES = Map.ofEntries(
			// value element: val attribute is required for GeoNumeric, GeoBoolean, GeoButton
			// Note: Default value depends on object type, handled separately
			Map.entry("value", Map.of("val", "")), // Will be set based on object type
			
			// slider element
			Map.entry("slider", Map.of("width", "200")), // Default slider width
			
			// absoluteScreenLocation element
			Map.entry("absoluteScreenLocation", Map.of("x", "0", "y", "0")),
			
			// cropBox element
			Map.entry("cropBox", Map.of("x", "0", "y", "0", "width", "100", "height", "100")),
			
			// video element
			Map.entry("video", Map.of("width", "320", "height", "240")),
			
			// spreadsheetTrace element
			Map.entry("spreadsheetTrace", Map.ofEntries(
					Map.entry("traceColumn1", "0"),
					Map.entry("traceColumn2", "0"),
					Map.entry("traceRow1", "0"),
					Map.entry("traceRow2", "0"),
					Map.entry("tracingRow", "0"),
					Map.entry("numRows", "0"),
					Map.entry("headerOffset", "0"))),
			
			// pointSize element
			Map.entry("pointSize", Map.of("val", "4")),
			
			// pointStyle element
			Map.entry("pointStyle", Map.of("val", "0")),
			
			// layer element
			Map.entry("layer", Map.of("val", "0")),
			
			// lineStyle element
			Map.entry("lineStyle", Map.of("type", "0", "thickness", "1")),
			
			// decoration element
			Map.entry("decoration", Map.of("type", "0")),
			
			// headStyle element
			Map.entry("headStyle", Map.of("val", "0")),
			
			// arcSize element
			Map.entry("arcSize", Map.of("val", "30")),
			
			// angleStyle element
			Map.entry("angleStyle", Map.of("val", "0")),
			
			// slopeTriangleSize element
			Map.entry("slopeTriangleSize", Map.of("val", "1")),
			
			// decimals element
			Map.entry("decimals", Map.of("val", "10")),
			
			// significantfigures element
			Map.entry("significantfigures", Map.of("val", "5")),
			
			// labelOffset element
			Map.entry("labelOffset", Map.of("x", "0", "y", "0")),
			
			// labelMode element
			Map.entry("labelMode", Map.of("val", "0")),
			
			// tooltipMode element
			Map.entry("tooltipMode", Map.of("val", "0")),
			
			// ordering element
			Map.entry("ordering", Map.of("val", "0")),
			
			// selectedIndex element
			Map.entry("selectedIndex", Map.of("val", "0")),
			
			// borderColor element
			Map.entry("borderColor", Map.of("r", "0", "g", "0", "b", "0")),
			
			// boundingBox element
			Map.entry("boundingBox", Map.of("width", "100", "height", "50")),
			
			// embed element
			Map.entry("embed", Map.of("id", "0")),
			
			// tag element (for chart styles)
			Map.entry("tag", Map.ofEntries(
					Map.entry("key", ""),
					Map.entry("value", ""),
					Map.entry("barNumber", "0"))),
			
			// length element
			Map.entry("length", Map.of("val", "10")),
			
			// font element
			// Note: If sizeM is null, then size is required
			// We'll handle this in fillRequiredAttributes
			Map.entry("font", Map.of("size", "12")), // Required if sizeM is null
			
			// contentSize element
			// Note: Has try-catch, but we'll provide defaults to avoid errors
			Map.entry("contentSize", Map.of("width", "100", "height", "100")),
			
			// javascript element
			// Note: val attribute is required (default empty string if missing)
			Map.entry("javascript", Map.of("val", ""))
			// matrix element (for conic/quadric - A0-A5 or A0-A9)
			// Note: These are conditionally required based on needsValuesFromXML
			// We'll handle them separately if needed
	);
	
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
	 * @param geo GeoElement
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
	 * Checks if the given attributes map contains a reset marker.
	 * 
	 * @param attrs attributes map
	 * @return true if the map contains the reset marker
	 */
	private static boolean isResetMarker(LinkedHashMap<String, String> attrs) {
		return attrs != null && attrs.containsKey(RESET_MARKER);
	}
	
	/**
	 * Gets the default style map for a given default type, using cache.
	 * 
	 * @param defaultType default type from ConstructionDefaults
	 * @param defaults ConstructionDefaults instance
	 * @return map from tag names to default attribute maps, or null if default geo not found
	 */
	private static Map<String, LinkedHashMap<String, String>> getDefaultStyleMap(
			int defaultType, ConstructionDefaults defaults) {
		// Check cache first
		return DEFAULT_STYLE_MAP_CACHE.computeIfAbsent(defaultType, type -> {
			GeoElement defaultGeo = defaults.getDefaultGeo(type);
			if (defaultGeo == null) {
				return null;
			}
			
			try {
				// Parse default geo's XML to get style map
				XMLToStyleMapParser parser = new XMLToStyleMapParser();
				return parser.parse(defaultGeo.getStyleXML());
			} catch (GpadParseException e) {
				Log.debug("Failed to parse default geo XML for type " + type + ": " + e.getMessage());
				return null;
			}
		});
	}
	
	/**
	 * Gets default attributes for a specific tag from the default geo.
	 * 
	 * @param geo GeoElement to get default type for
	 * @param tagName XML tag name
	 * @return default attributes map, or null if not found
	 */
	private static LinkedHashMap<String, String> getDefaultAttrsForTag(GeoElement geo, String tagName) {
		ConstructionDefaults defaults = geo.getKernel().getConstruction().getConstructionDefaults();
		int defaultType = defaults.getDefaultType(geo);
		Map<String, LinkedHashMap<String, String>> defaultStyleMap = getDefaultStyleMap(defaultType, defaults);
		
		if (defaultStyleMap == null) {
			return null;
		}
		
		LinkedHashMap<String, String> defaultAttrs = defaultStyleMap.get(tagName);
		if (defaultAttrs != null) {
			// Return a copy to avoid modifying the cached version
			return new LinkedHashMap<>(defaultAttrs);
		}
		
		// Special handling for tags that need explicit clearing when not in default geo
		// These tags may not exist in default geo but need to be cleared if they were set
		
		// trace - clear trace flag (similar to spreadsheetTrace)
		if ("trace".equals(tagName)) {
			LinkedHashMap<String, String> disableAttrs = new LinkedHashMap<>();
			disableAttrs.put("val", "false");
			return disableAttrs;
		}
		
		// spreadsheetTrace - clear spreadsheet trace
		if ("spreadsheetTrace".equals(tagName)) {
			LinkedHashMap<String, String> disableAttrs = new LinkedHashMap<>();
			disableAttrs.put("val", "false");
			disableAttrs.put("traceColumn1", "-1");
			disableAttrs.put("traceColumn2", "-1");
			disableAttrs.put("traceRow1", "-1");
			disableAttrs.put("traceRow2", "-1");
			disableAttrs.put("tracingRow", "0");
			disableAttrs.put("numRows", "0");
			disableAttrs.put("headerOffset", "0");
			disableAttrs.put("doColumnReset", "false");
			disableAttrs.put("doRowLimit", "false");
			disableAttrs.put("showLabel", "false");
			disableAttrs.put("showTraceList", "false");
			disableAttrs.put("doTraceGeoCopy", "false");
			disableAttrs.put("pause", "false");
			return disableAttrs;
		}
		
		// condition - clear show object condition
		// Note: processShowObjectConditionList evaluates the string to GeoBoolean
		// Empty string may not work correctly (evaluateToBoolean("") returns null, which causes error)
		// However, we provide empty string as it's the best we can do via XML
		// This is kept as fallback even though handleDirectReset exists, because XML approach may not fully clear
		if ("condition".equals(tagName)) {
			LinkedHashMap<String, String> clearAttrs = new LinkedHashMap<>();
			clearAttrs.put("showObject", "");  // Empty string (may not fully clear via XML)
			return clearAttrs;
		}
		
		// dynamicCaption - clear dynamic caption
		// Note: processDynamicCaptionList looks up label, empty string may not work
		// However, we provide empty string as it's the best we can do via XML
		// This is kept as fallback even though handleDirectReset exists, because XML approach may not fully clear
		if ("dynamicCaption".equals(tagName)) {
			LinkedHashMap<String, String> clearAttrs = new LinkedHashMap<>();
			clearAttrs.put("val", "");  // Empty string (may not fully clear via XML)
			return clearAttrs;
		}
		
		return null;
	}
	
	/**
	 * Applies a style sheet to a GeoElement using ConsElementXMLHandler.
	 * 
	 * @param geo GeoElement to apply styles to
	 * @param styleSheet style sheet to apply
	 * @throws GpadParseException if application fails
	 */
	public static void apply(GpadStyleSheet styleSheet, GeoElement geo) throws GpadParseException {
		if (geo == null || styleSheet == null)
			return;

		Kernel kernel = geo.getKernel();
		MyXMLHandler myXMLHandler = kernel.newMyXMLHandler(kernel.getConstruction());
		ConsElementXMLHandler xmlHandler = new ConsElementXMLHandler(myXMLHandler, kernel.getApplication());

		// Initialize the handler with the geo element using initForGpad
		// This avoids setting default values that would interfere with partial style updates
		xmlHandler.initForGpad(geo);
		
		Map<String, LinkedHashMap<String, String>> properties = styleSheet.getProperties();
		
		// Track if spreadsheetTrace tag is processed - need to sync TraceManager afterwards
		boolean hasSpreadsheetTraceTag = properties.containsKey("spreadsheetTrace");

		// Apply each property using ConsElementXMLHandler
		// We can access startGeoElement because we're in the same package
		// Use myXMLHandler.errors directly, same as evalXML does
		// This ensures all errors (from handleValue and addError) go to the same list
		for (Map.Entry<String, LinkedHashMap<String, String>> entry : properties.entrySet()) {
			String tagName = entry.getKey();
			LinkedHashMap<String, String> attrs = entry.getValue();
			
			if (attrs == null)
				continue;
			
			// Check if this is a reset marker (key: "~")
			// Reset marker can coexist with normal attributes: {"~": "", "type": "1", "thickness": "5"}
			// This means: first clear, then set the normal attributes
			boolean hasResetMarker = isResetMarker(attrs);
			if (hasResetMarker) {
				// First, clear the property
				// For certain tags, we can directly call geo methods to clear them
				// This avoids going through XML handler and is more efficient
				if (handleDirectReset(tagName, attrs, geo)) {
					// Direct reset handled, but we still need to apply normal attributes if any
					// Remove reset marker from attrs copy for further processing
					LinkedHashMap<String, String> attrsWithoutReset = new LinkedHashMap<>(attrs);
					attrsWithoutReset.remove(RESET_MARKER);
					if (!attrsWithoutReset.isEmpty()) {
						// There are normal attributes to apply after clearing
						fillRequiredAttributes(tagName, attrsWithoutReset, geo);
						xmlHandler.startGeoElement(tagName, attrsWithoutReset, myXMLHandler.errors);
					}
					continue; // Skip default-based reset since we already cleared directly
				}
				
				// Get default attributes for this tag from the default geo
				LinkedHashMap<String, String> defaultAttrs = getDefaultAttrsForTag(geo, tagName);
				if (defaultAttrs != null) {
					// Merge normal attributes with default attributes
					// Normal attributes override defaults
					LinkedHashMap<String, String> mergedAttrs = new LinkedHashMap<>(defaultAttrs);
					// Remove reset marker and merge normal attributes
					LinkedHashMap<String, String> normalAttrs = new LinkedHashMap<>(attrs);
					normalAttrs.remove(RESET_MARKER);
					mergedAttrs.putAll(normalAttrs);
					attrs = mergedAttrs;
				} else {
					// No default value found, but we still need to apply normal attributes if any
					LinkedHashMap<String, String> normalAttrs = new LinkedHashMap<>(attrs);
					normalAttrs.remove(RESET_MARKER);
					if (normalAttrs.isEmpty()) {
						// No normal attributes and no default, skip this tag
						continue;
					}
					attrs = normalAttrs;
				}
			}
			
			// Fill missing required attributes before applying
			fillRequiredAttributes(tagName, attrs, geo);
			xmlHandler.startGeoElement(tagName, attrs, myXMLHandler.errors);
		}

		xmlHandler.processLists(); // Process deferred lists (e.g., minMaxList for slider min/max)
		xmlHandler.finish(); // Finish processing (won't set defaults in Gpad mode)
		
		// Update construction - this will internally call notifyRepaint() at the end
		// This mitigates the risk of missing updateConstruction() and notifyRepaint()
		kernel.updateConstruction(false);
		
		// If spreadsheetTrace tag was processed, sync TraceManager AFTER updateConstruction
		// This mitigates the risk of TraceManager being out of sync with GeoElement's trace settings
		// The geo's trace state may have changed (enabled or disabled), so we need to reload
		// the trace collection to ensure TraceManager reflects the current state
		// Same order as evalXML: updateConstruction() first, then loadTraceGeoCollection()
		// loadTraceGeoCollection() will call repaintSpreadsheet(), so we don't need additional repaint
		if (hasSpreadsheetTraceTag) {
			// loadTraceGeoCollection() will scan all geos and only include those with trace enabled
			// This ensures TraceManager is in sync even if trace was disabled for this geo
			// Called after updateConstruction() to avoid spurious traces (same as evalXML)
			kernel.getApplication().getTraceManager().loadTraceGeoCollection();
		}

		if (!myXMLHandler.errors.isEmpty())
			throw new GpadParseException("Failed to apply style sheet: " + myXMLHandler.errors.toString());
	}
	
	/**
	 * Handles direct reset for tags that can be cleared by calling geo methods directly.
	 * This is more efficient than going through XML handler for simple clear operations.
	 * 
	 * @param tagName XML tag name
	 * @param attrs attributes map (may contain type for listener)
	 * @param geo GeoElement to reset
	 * @return true if the reset was handled directly, false if should use default XML approach
	 */
	private static boolean handleDirectReset(String tagName, LinkedHashMap<String, String> attrs, GeoElement geo) {
		try {
			switch (tagName) {
			case "condition":
				// Clear show object condition
				geo.setShowObjectCondition(null);
				return true;
				
			case "dynamicCaption":
				// Clear dynamic caption
				geo.removeDynamicCaption();
				return true;
				
			case "trace":
				// Clear trace flag
				if (geo instanceof Traceable) {
					((Traceable) geo).setTrace(false);
					return true;
				}
				return false;
				
			case "spreadsheetTrace":
				// Clear spreadsheet trace
				if (geo.isSpreadsheetTraceable()) {
					geo.setSpreadsheetTrace(false);
					return true;
				}
				return false;
				
			case "javascript":
			case "onClick":
				// Clear click script
				geo.setScript(null, EventType.CLICK);
				return true;
				
			case "onUpdate":
				// Clear update script
				geo.setScript(null, EventType.UPDATE);
				return true;
				
			case "onDragEnd":
				// Clear drag end script
				geo.setScript(null, EventType.DRAG_END);
				return true;
				
			case "onChange":
				// Clear change script
				geo.setScript(null, EventType.EDITOR_KEY_TYPED);
				return true;
				
			case "listener":
				// Clear listener - needs type attribute
				String type = attrs != null ? attrs.get("type") : null;
				if (type != null) {
					ScriptManager scriptManager = geo.getKernel().getApplication().getScriptManager();
					if ("objectUpdate".equals(type)) {
						scriptManager.getUpdateListenerMap().remove(geo);
						return true;
					} else if ("objectClick".equals(type)) {
						scriptManager.getClickListenerMap().remove(geo);
						return true;
					}
				}
				return false;
				
			default:
				return false;
			}
		} catch (Exception e) {
			Log.debug("Failed to directly reset " + tagName + ": " + e.getMessage());
			return false; // Fall back to XML approach on error
		}
	}
}
