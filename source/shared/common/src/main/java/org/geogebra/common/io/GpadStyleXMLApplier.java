package org.geogebra.common.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
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
	 * Map of required attributes that will throw exceptions if missing.
	 * Key: element name (e.g., "value", "slider")
	 * Value: Set of attribute names that are required (will throw exception if missing)
	 * 
	 * This map only includes attributes that will cause exceptions when parsed by
	 * ConsElementXMLHandler. Attributes that are optional (like show's object/label/ev)
	 * are not included here.
	 */
	private static final Map<String, Set<String>> REQUIRED_ATTRIBUTE_NAMES = Map.ofEntries(
			// value element: val attribute is required (throws exception when parsing)
			Map.entry("value", java.util.Set.of("val")), // gpad style "random" require this default value
			
			// slider element: width is required (throws exception in setSliderWidth)
			Map.entry("slider", java.util.Set.of("width")),
			
			// cropBox element: x, y, width, height are required
			Map.entry("cropBox", java.util.Set.of("x", "y", "width", "height")),
			
			// video element: width and height are required
			Map.entry("video", java.util.Set.of("width", "height")),
			
			// spreadsheetTrace element: many attributes are required
			Map.entry("spreadsheetTrace", java.util.Set.of(
					"val", "traceColumn1", "traceColumn2", "traceRow1", "traceRow2",
					"tracingRow", "numRows", "headerOffset", "doColumnReset", "doRowLimit",
					"showLabel", "showTraceList", "doTraceGeoCopy")),
			
			// lineStyle element: type and thickness are required
			Map.entry("lineStyle", java.util.Set.of("type", "thickness")),
			
			// boundingBox element: width and height are required
			Map.entry("boundingBox", java.util.Set.of("width", "height")),
			
			// embed element: id is required
			Map.entry("embed", java.util.Set.of("id")),
			
			// tag element (for chart styles): key, value, barNumber are required
			Map.entry("tag", java.util.Set.of("key", "value", "barNumber")),
			
			// contentSize element: width and height are required
			Map.entry("contentSize", java.util.Set.of("width", "height")),
			
			// tableview element: column is required (to avoid null pointer exception in parseDoubleNaN)
			// points is optional (default is true, but if not provided, parseBoolean(null) returns false)
			Map.entry("tableview", java.util.Set.of("column", "points"))
	);
	
	/**
	 * Fills missing required attributes with default values from defaultGeo.
	 * Only fills attributes that will throw exceptions if missing.
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
		// Get the set of required attribute names for this tag
		Set<String> requiredAttrNames = REQUIRED_ATTRIBUTE_NAMES.get(tagName);
		
		// If this tag has no required attributes (like "show"), skip filling
		if (requiredAttrNames == null || requiredAttrNames.isEmpty())
			return;
		
		// Get default attributes for this tag from the default geo
		LinkedHashMap<String, String> defaultAttrs = getDefaultAttrsForTag(geo, tagName);
		
		// Fill only the required attributes that are missing
		for (String attrName : requiredAttrNames) {
			if (!attrs.containsKey(attrName) || attrs.get(attrName) == null) {
				String defaultValue = null;

				// Special handling for value element's val attribute:
				// Use current geo's value
				if ("value".equals(tagName) && "val".equals(attrName))
					defaultValue = getDefaultValueForValueElement(geo);
				else if ("javascript".equals(tagName) && "val".equals(attrName))
					defaultValue = ""; // for javascript element's val, use ""
				else {
					// Try to get default value from default geo
					if (defaultAttrs != null && defaultAttrs.containsKey(attrName))
						defaultValue = defaultAttrs.get(attrName);
				}

				if (defaultValue != null)
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
		
		// tableview - provide default value for column only
		// Default: column=-1 (not in table)
		// points is optional: if not provided, parseBoolean(null) returns false, but we want default true
		// So we also provide points default value to ensure correct behavior
		if ("tableview".equals(tagName)) {
			LinkedHashMap<String, String> clearAttrs = new LinkedHashMap<>();
			clearAttrs.put("column", "-1");
			// Note: points default is true, but if not in attrs, parseBoolean(null) returns false
			// So we provide default to ensure correct behavior when points is not specified
			clearAttrs.put("points", "true");
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
			
			// Special handling for startPoint: deserialize _corners and apply immediately
			if ("startPoint".equals(tagName) && attrs.containsKey("_corners")) {
				String serialized = attrs.get("_corners");
				deserializeAndApplyStartPointCorners(serialized, xmlHandler, myXMLHandler.errors);
				continue;
			}
			
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
	
	/**
	 * Deserializes startPoint corners from serialized string and applies them immediately.
	 * Format: corner1\u0001corner2\u0001...
	 * Each corner: [absolute byte][type byte][content]
	 *   - absolute byte: \u0002=true, \u0003=false
	 *   - type byte: \u0002=exp, \u0003=x/y/z
	 *   - content: if exp, then exp string; if x/y/z, then "x,y,z" (z optional, no trailing comma)
	 * 
	 * @param serialized serialized string
	 * @param xmlHandler XML handler to apply corners
	 * @param errors error list
	 */
	private static void deserializeAndApplyStartPointCorners(String serialized,
			ConsElementXMLHandler xmlHandler, ArrayList<String> errors) {
		if (serialized == null || serialized.isEmpty())
			return;
		
		// Split by SOH (\u0001) to get individual corners
		String[] cornerStrings = serialized.split("\u0001", -1);
		int cornerIndex = 0;
		for (String cornerStr : cornerStrings) {
			if (cornerStr.isEmpty())
				continue;
			
			LinkedHashMap<String, String> corner = new LinkedHashMap<>();
			
			// Parse corner format: [absolute byte][type byte][content]
			if (cornerStr.length() < 2)
				continue; // Invalid format
			
			// (1) Parse absolute byte
			char absoluteByte = cornerStr.charAt(0);
			boolean isAbsolute = (absoluteByte == '\u0002');
			if (isAbsolute)
				corner.put("absolute", "true");
			
			// (2) Parse type byte
			char typeByte = cornerStr.charAt(1);
			boolean isExp = (typeByte == '\u0002');
			
			// (3) Parse content
			String content = cornerStr.substring(2);
			if (isExp) // exp type: content is the expression
				corner.put("exp", content);
			else {
				// x/y/z type: content is "x,y,z" or "x,y" (z optional)
				String[] coords = content.split(",", -1);
				if (coords.length >= 2) {
					corner.put("x", coords[0]);
					corner.put("y", coords[1]);
					if (coords.length >= 3 && !coords[2].isEmpty())
						corner.put("z", coords[2]);
				}
			}
			
			// Set number attribute based on position (0, 1, 2, ...)
			// Note: number is needed for XML handler to identify corner index
			corner.put("number", String.valueOf(cornerIndex++));
			
			// Apply immediately (no need to fill required attributes, startPoint has none)
			xmlHandler.startGeoElement("startPoint", corner, errors);
		}
	}
}
