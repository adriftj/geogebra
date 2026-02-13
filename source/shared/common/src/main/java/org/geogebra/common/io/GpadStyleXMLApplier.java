package org.geogebra.common.io;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;

import org.geogebra.common.gpad.GpadSerializer;
import org.geogebra.common.gpad.GpadStyleDefaults;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoBoolean;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoInputBox;
import org.geogebra.common.kernel.geos.GeoNumeric;
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
	
	// Default values and required attributes are now managed by GpadStyleDefaults
	// (single authoritative source for both GGB->GPAD and GPAD->GGB directions)
	
	/**
	 * Fills missing required attributes with default values from defaultGeo.
	 * Only fills attributes that will throw exceptions if missing.
	 * 
	 * @param attrs attributes map to fill
	 * @param canModify false if need a new copy of adjusted map, true if can modify `attrs`
	 * @param tagName XML element name
	 * @param geo GeoElement (needed for type-specific defaults)
	 * @return filled map
	 * 
	 */
	private static LinkedHashMap<String, String> fillRequiredAttributes(
		LinkedHashMap<String, String> attrs, boolean canModify,
		String tagName, GeoElement geo) {
		// Get the set of required attribute names for this tag from GpadStyleDefaults
		Set<String> requiredAttrNames = GpadStyleDefaults.getRequiredAttributes(tagName);
		// If this tag has no required attributes (like "show"), skip filling
		if (requiredAttrNames.isEmpty())
			return attrs;

		LinkedHashMap<String, String> originAttrs = attrs;
		LinkedHashMap<String, String> defaultAttrs = GpadStyleDefaults.getDefaultAttrs(tagName);
		Optional<LinkedHashMap<String, String>> optDefaultAttrs =
			GpadStyleDefaults.getTypeAdjustedDefaults(tagName, geo);
		if (optDefaultAttrs.isPresent() && defaultAttrs != null) {
			defaultAttrs = new LinkedHashMap<>(defaultAttrs);
			defaultAttrs.putAll(optDefaultAttrs.get());
		}
		
		// Fill only the required attributes that are missing
		for (String attrName : requiredAttrNames) {
			if (!attrs.containsKey(attrName) || attrs.get(attrName) == null) {
				String defaultValue = null;

				if ("value".equals(tagName)) { // Use current geo's value for default
					if ("val".equals(attrName))
						defaultValue = getDefaultValueForValueElement(geo);
				} else if ("javascript".equals(tagName)) {
					if ("val".equals(attrName))
						defaultValue = ""; // for javascript element's val, use ""
				} else if (defaultAttrs != null) {
					defaultValue = defaultAttrs.get(attrName);
				} else
					continue;

				if (defaultValue != null) {
					if (!canModify && attrs == originAttrs)
						attrs = new LinkedHashMap<>(attrs);
					attrs.put(attrName, defaultValue);
				}
			}
		}
		return attrs;
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
	
	// Type-specific default adjustments are handled by GpadStyleDefaults.getTypeAdjustedDefaults()
	
	/**
	 * Applies a style sheet to a GeoElement using ConsElementXMLHandler.
	 * 
	 * @param geo GeoElement to apply styles to
	 * @param styleSheet style sheet to apply
	 * @return list of error messages (empty if no errors occurred)
	 */
	public static ArrayList<String> apply(GpadStyleSheet styleSheet, GeoElement geo) {
		if (geo == null || styleSheet == null)
			return new ArrayList<>();

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
				if (handleDirectReset(tagName, attrs, geo)) {
					if (attrs.size()<=1) // no any normal attributes
						continue;
					// Remove reset marker from attrs copy
					attrs = new LinkedHashMap<>(attrs);
					attrs.remove(RESET_MARKER);
					// There are normal attributes to apply after clearing
					attrs = fillRequiredAttributes(attrs, true, tagName, geo);
				}
				else {
					LinkedHashMap<String, String> defaultAttrs = GpadStyleDefaults.getDefaultAttrs(tagName);
					if (attrs.size()<=1) { // no normal attributes
						if (defaultAttrs == null) // no default
							continue;
						attrs = defaultAttrs;
					}
					else {
						if (defaultAttrs != null) {
							// 有其他值要设置，合并新设的值到缺省值中（新值覆盖缺省值）
							LinkedHashMap<String, String> mergedAttrs = new LinkedHashMap<>(defaultAttrs);
							GpadStyleDefaults.getTypeAdjustedDefaults(tagName, geo).ifPresent(
								dAttrs -> mergedAttrs.putAll(dAttrs)
							);
							mergedAttrs.putAll(attrs);
							attrs = mergedAttrs;
						} else // No default, has normal attributes
							attrs = new LinkedHashMap<>(attrs);
						// Remove reset marker to get normal attributes
						attrs.remove(RESET_MARKER);
					}
				}
			}
			else // Fill missing required attributes before applying
				attrs = fillRequiredAttributes(attrs, false, tagName, geo);

			// Special handling for startPoint: deserialize _corners and apply immediately
			if ("startPoint".equals(tagName)) {
				if(attrs.containsKey("_corners")) {
					String serialized = attrs.get("_corners");
					deserializeAndApplyStartPointCorners(serialized, xmlHandler, myXMLHandler.errors);
				}
			}
			// Special handling for barTag: deserialize _barTags and apply immediately
			else if ("barTag".equals(tagName)) {
				if(attrs.containsKey("_barTags")) {
					String serialized = attrs.get("_barTags");
					deserializeAndApplyBarTags(serialized, xmlHandler, myXMLHandler.errors);
				}
			}
			// Special handling for jsUpdateFunction and jsClickFunction: convert to listener element
			else if ("jsUpdateFunction".equals(tagName) || "jsClickFunction".equals(tagName)) {
				String val = attrs.get("val");
				if (val != null && !val.isEmpty()) {
					LinkedHashMap<String, String> listenerAttrs = new LinkedHashMap<>();
					listenerAttrs.put("type", "jsUpdateFunction".equals(tagName)? "objectUpdate": "objectClick");
					listenerAttrs.put("val", val);
					xmlHandler.startGeoElement("listener", listenerAttrs, myXMLHandler.errors);
				}
			}
			else {
				// Special handling for coords: map x,y,z,w to ox,oy,oz,ow for 3D objects
				if ("coords".equals(tagName))
					attrs = mapCoordsFor3DObjects(geo, attrs);
				xmlHandler.startGeoElement(tagName, attrs, myXMLHandler.errors);
			}
		}

		for (String tag : GpadStyleDefaults.ALWAYS_APPLY_TAGS) {
			if (!properties.containsKey(tag)) {
				LinkedHashMap<String, String> defaults = GpadStyleDefaults.getDefaultAttrs(tag);
				if (defaults != null)
					xmlHandler.startGeoElement(tag, defaults, myXMLHandler.errors);
			}
		}

		xmlHandler.processLists();
		xmlHandler.finish();
		
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

		// Return the list of errors (may be empty)
		return new ArrayList<>(myXMLHandler.errors);
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

			case "ggbClick":
			case "jsClick":
				// Clear click script
				geo.setScript(null, EventType.CLICK);
				return true;
				
			case "ggbUpdate":
			case "jsUpdate":
				// Clear update script
				geo.setScript(null, EventType.UPDATE);
				return true;
				
			case "ggbDragEnd":
			case "jsDragEnd":
				// Clear drag end script
				geo.setScript(null, EventType.DRAG_END);
				return true;
				
			case "ggbChange":
			case "jsChange":
				// Clear change script
				geo.setScript(null, EventType.EDITOR_KEY_TYPED);
				return true;
				
			case "jsUpdateFunction":
				// Clear update listener
				ScriptManager scriptManagerUpdate = geo.getKernel().getApplication().getScriptManager();
				scriptManagerUpdate.getUpdateListenerMap().remove(geo);
				return true;
				
			case "jsClickFunction":
				// Clear click listener
				ScriptManager scriptManagerClick = geo.getKernel().getApplication().getScriptManager();
				scriptManagerClick.getClickListenerMap().remove(geo);
				return true;
				
			case "tempUserInput":
				// Clear temp user input
				if (geo instanceof GeoInputBox)
					((GeoInputBox) geo).clearTempUserInput();
				return true;
				
			default:
				return false;
			}
		} catch (Exception e) {
			Log.debug("Failed to directly reset " + tagName + ": " + e.getMessage());
			return true;
		}
	}
	
	/**
	 * Deserializes startPoint corners from serialized string and applies them immediately.
	 * Uses GpadSerializer.deserializeStartPointCorners() for deserialization.
	 * 
	 * @param serialized serialized string
	 * @param xmlHandler XML handler to apply corners
	 * @param errors error list
	 */
	private static void deserializeAndApplyStartPointCorners(String serialized,
			ConsElementXMLHandler xmlHandler, ArrayList<String> errors) {
		if (serialized == null || serialized.isEmpty())
			return;
		
		// Use array to allow modification in lambda
		int[] cornerIndex = {0};
		
		// Deserialize using helper class
		GpadSerializer.deserializeStartPointCorners(serialized, (firstCorner, isAbsolute, cornerData) -> {
			String exp = cornerData[0];
			// Skip empty placeholder corners (gap in startPoint sequence)
			if (exp == null && cornerData[1] == null && cornerData[2] == null) {
				cornerIndex[0]++;
				return;
			}

			LinkedHashMap<String, String> corner = new LinkedHashMap<>();
			
			// Set absolute attribute if true
			if (isAbsolute)
				corner.put("absolute", "true");
			
			// Set exp or x/y/z attributes
			if (exp != null) {
				// Expression-based corner
				corner.put("exp", exp);
			} else {
				// x/y/z type
				String x = cornerData[1];
				String y = cornerData[2];
				String z = cornerData[3];
				if (x != null && y != null) {
					corner.put("x", x);
					corner.put("y", y);
					if (z != null)
						corner.put("z", z);
				}
			}
			
			// Set number attribute based on position (0, 1, 2, ...)
			// Note: number is needed for XML handler to identify corner index
			corner.put("number", String.valueOf(cornerIndex[0]++));
			
			// Apply immediately (no need to fill required attributes, startPoint has none)
			xmlHandler.startGeoElement("startPoint", corner, errors);
		});
	}
	
	/**
	 * Maps coords attributes from GPAD format (x, y, z, w) to 3D object format (ox, oy, oz, ow).
	 * For 3D objects like GeoConic3D and GeoLine3D, the origin coordinates use ox/oy/oz/ow instead of x/y/z/w.
	 * 
	 * @param geo GeoElement to check type
	 * @param attrs coords attributes from GPAD (may contain x, y, z, w)
	 * @return mapped attributes (ox, oy, oz, ow for 3D objects, or original attrs for 2D objects)
	 */
	private static LinkedHashMap<String, String> mapCoordsFor3DObjects(GeoElement geo, LinkedHashMap<String, String> attrs) {
		if (attrs == null)
			return attrs;
		
		// Check if this is a 3D object that uses ox/oy/oz/ow
		boolean needsMapping = geo instanceof org.geogebra.common.geogebra3D.kernel3D.geos.GeoConic3D
				|| geo instanceof org.geogebra.common.geogebra3D.kernel3D.geos.GeoLine3D;
		
		if (!needsMapping)
			return attrs; // 2D objects use x, y, z directly
		
		// Check if mapping is needed (if ox already exists, no need to map)
		if (attrs.containsKey("ox"))
			return attrs; // Already in 3D format
		
		// Map x, y, z, w to ox, oy, oz, ow
		LinkedHashMap<String, String> mapped = new LinkedHashMap<>(attrs);
		String x = mapped.remove("x");
		String y = mapped.remove("y");
		String z = mapped.remove("z");
		String w = mapped.remove("w");
		
		if (x != null) mapped.put("ox", x);
		if (y != null) mapped.put("oy", y);
		if (z != null) mapped.put("oz", z);
		if (w != null) mapped.put("ow", w);
		
		return mapped;
	}
	
	/**
	 * Deserializes barTag bars from serialized string and applies them immediately.
	 * Uses GpadSerializer.deserializeBarTags() for deserialization.
	 * 
	 * @param serialized serialized string
	 * @param xmlHandler XML handler to apply tag elements
	 * @param errors error list
	 */
	private static void deserializeAndApplyBarTags(String serialized,
			ConsElementXMLHandler xmlHandler, ArrayList<String> errors) {
		if (serialized == null || serialized.isEmpty())
			return;
		
		// Deserialize using helper class
		GpadSerializer.deserializeBarTags(serialized, (barNumber, rgba, fillTypeXML, hatchAngle,
				hatchDistance, image, fillSymbol) -> {
			// Generate barColor tag with rgba(r,g,b,a) format to match ChartStyle.barXml() output
			// bit 0: barColor (r, g, b)
			if (rgba[0] >= 0 && rgba[1] >= 0 && rgba[2] >= 0) {
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barColor");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", "rgba(" + rgba[0] + "," + rgba[1] + "," + rgba[2] + ",1)");
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// Generate barAlpha tag separately (if present)
			// bit 1: barAlpha
			if (rgba[3] >= 0) {
				double alpha = rgba[3] / 255.0;
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barAlpha");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", String.valueOf(alpha));
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 2: barFillType
			if (fillTypeXML != null) {
				try {
					int fillType = Integer.parseInt(fillTypeXML);
					
					// Validate fillType is within valid range (0-9 for FillType enum)
					if (fillType < 0 || fillType > 9) {
						// Invalid fillType value, skip this bar
						return;
					}
					
					// Create barFillType tag
					LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
					tagAttrs.put("key", "barFillType");
					tagAttrs.put("barNumber", barNumber);
					tagAttrs.put("value", fillTypeXML);
					xmlHandler.startGeoElement("tag", tagAttrs, errors);
				} catch (NumberFormatException e) {
					// Invalid fillType, skip
				}
			}
			
			// bit 3: barHatchAngle
			if (hatchAngle != null) {
				// Create barHatchAngle tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barHatchAngle");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", hatchAngle);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 4: barHatchDistance
			if (hatchDistance != null) {
				// Create barHatchDistance tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barHatchDistance");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", hatchDistance);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 5: barImage
			if (image != null) {
				// Create barImage tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barImage");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", image);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
			
			// bit 6: barSymbol
			if (fillSymbol != null) {
				// Create barSymbol tag
				LinkedHashMap<String, String> tagAttrs = new LinkedHashMap<>();
				tagAttrs.put("key", "barSymbol");
				tagAttrs.put("barNumber", barNumber);
				tagAttrs.put("value", fillSymbol);
				xmlHandler.startGeoElement("tag", tagAttrs, errors);
			}
		});
	}
}
