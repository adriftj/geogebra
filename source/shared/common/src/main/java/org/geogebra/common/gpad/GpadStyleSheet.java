package org.geogebra.common.gpad;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a Gpad style sheet that contains property-value pairs.
 * Style sheets are used to apply visual and behavioral properties to GeoElements.
 * 
 * Properties are stored as Map<String, LinkedHashMap<String, String>> where:
 * - Key: XML tag name (e.g., "lineStyle", "objColor")
 * - Value: LinkedHashMap of attributes for that tag (e.g., {"type": "0", "thickness": "4"})
 */
public class GpadStyleSheet {
	/**
	 * Reset marker key used in attribute maps to indicate that a property should be reset to its default value.
	 * The value "~" is chosen because it cannot appear as a valid attribute name.
	 */
	private static final String RESET_MARKER = "~";
	
	private String name;
	private Map<String, LinkedHashMap<String, String>> properties;

	/**
	 * Creates a new style sheet with the given name.
	 * 
	 * @param name
	 *            style sheet name (must start with $)
	 */
	public GpadStyleSheet(String name) {
		this.name = name;
		this.properties = new LinkedHashMap<>();
	}

	/**
	 * Creates an anonymous style sheet (for inline styles).
	 */
	public GpadStyleSheet() {
		this.name = "";
		this.properties = new LinkedHashMap<>();
	}

	/**
	 * @return style sheet name, or null for anonymous style sheets
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets a property in the style sheet.
	 * If the property already has a reset marker, it will be preserved and merged with new attributes.
	 * 
	 * @param property
	 *            property name (XML tag name, e.g., "lineStyle", "objColor")
	 * @param attrs
	 *            attributes map for this property
	 */
	public void setProperty(String property, LinkedHashMap<String, String> attrs) {
		LinkedHashMap<String, String> existing = properties.get(property);
		if (existing != null && existing.containsKey(RESET_MARKER)) {
			// Preserve reset marker and merge with new attributes
			LinkedHashMap<String, String> merged = new LinkedHashMap<>(existing);
			if (attrs != null) {
				merged.putAll(attrs);
			}
			properties.put(property, merged);
		} else {
			properties.put(property, attrs);
		}
	}

	/**
	 * Gets a property attributes map.
	 * 
	 * @param property
	 *            property name (XML tag name)
	 * @return attributes map, or null if not set
	 */
	public LinkedHashMap<String, String> getProperty(String property) {
		return properties.get(property);
	}

	/**
	 * @return all properties in this style sheet
	 */
	public Map<String, LinkedHashMap<String, String>> getProperties() {
		return properties;
	}

	/**
	 * Merges properties from another style sheet into this one.
	 * If a property in the other sheet contains a reset marker, this property is completely replaced
	 * with the other's content (allowing reset marker and normal attributes to coexist).
	 * Otherwise, attributes are merged normally (other's attributes override existing ones).
	 * 
	 * @param other
	 *            style sheet to merge from
	 */
	public void mergeFrom(GpadStyleSheet other) {
		if (other != null) {
			for (Map.Entry<String, LinkedHashMap<String, String>> entry : other.properties.entrySet()) {
				LinkedHashMap<String, String> otherAttrs = entry.getValue();
				
				// If the other sheet has a reset marker for this property, completely replace it
				// This allows reset marker and normal attributes to coexist
				if (isResetMarker(otherAttrs)) {
					// Copy all attributes from other (including reset marker and normal attributes)
					LinkedHashMap<String, String> copiedAttrs = new LinkedHashMap<>();
					if (otherAttrs != null)
						copiedAttrs.putAll(otherAttrs);
					this.properties.put(entry.getKey(), copiedAttrs);
					continue;
				}
				
				// Otherwise, merge attributes normally
				// Create a new LinkedHashMap for each property to avoid reference issues
				LinkedHashMap<String, String> mergedAttrs = new LinkedHashMap<>();
				// First add existing attributes if any
				LinkedHashMap<String, String> existing = this.properties.get(entry.getKey());
				if (existing != null)
					mergedAttrs.putAll(existing);
				// Then override with new attributes
				if (otherAttrs != null)
					mergedAttrs.putAll(otherAttrs);
				this.properties.put(entry.getKey(), mergedAttrs);
			}
		}
	}
	
	/**
	 * Sets a property with reset marker, indicating it should be cleared first.
	 * This creates a property with reset marker that can coexist with normal attributes.
	 * 
	 * @param property
	 *            property name (XML tag name, e.g., "lineStyle", "objColor")
	 */
	public void resetProperty(String property) {
		LinkedHashMap<String, String> attrs = this.properties.get(property);
		if (attrs == null) {
			attrs = new LinkedHashMap<>();
			this.properties.put(property, attrs);
		}
		attrs.put(RESET_MARKER, "");
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
	 * @return true if this is an anonymous style sheet
	 */
	public boolean isAnonymous() {
		return name == "";
	}

	/**
	 * Removes a property from the style sheet.
	 * 
	 * @param property
	 *            property name to remove
	 */
	public void removeProperty(String property) {
		properties.remove(property);
	}
}
