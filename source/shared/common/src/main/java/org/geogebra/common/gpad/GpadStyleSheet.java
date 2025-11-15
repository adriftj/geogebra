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
		this.name = null;
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
	 * 
	 * @param property
	 *            property name (XML tag name, e.g., "lineStyle", "objColor")
	 * @param attrs
	 *            attributes map for this property
	 */
	public void setProperty(String property, LinkedHashMap<String, String> attrs) {
		properties.put(property, attrs);
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
	 * Properties from the other style sheet will override existing ones.
	 * 
	 * @param other
	 *            style sheet to merge from
	 */
	public void mergeFrom(GpadStyleSheet other) {
		if (other != null) {
			for (Map.Entry<String, LinkedHashMap<String, String>> entry : other.properties.entrySet()) {
				// Create a new LinkedHashMap for each property to avoid reference issues
				LinkedHashMap<String, String> mergedAttrs = new LinkedHashMap<>();
				// First add existing attributes if any
				LinkedHashMap<String, String> existing = this.properties.get(entry.getKey());
				if (existing != null)
					mergedAttrs.putAll(existing);
				// Then override with new attributes
				if (entry.getValue() != null)
					mergedAttrs.putAll(entry.getValue());
				this.properties.put(entry.getKey(), mergedAttrs);
			}
		}
	}

	/**
	 * @return true if this is an anonymous style sheet
	 */
	public boolean isAnonymous() {
		return name == null;
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
