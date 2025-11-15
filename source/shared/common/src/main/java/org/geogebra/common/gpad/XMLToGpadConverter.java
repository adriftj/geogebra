package org.geogebra.common.gpad;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.main.App;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts XML construction to Gpad format.
 * Uses XML parsing to create GeoElements, then converts them to Gpad.
 */
public class XMLToGpadConverter {
	private Kernel kernel;
	private GeoElementToGpadConverter geoConverter;

	/**
	 * Creates a new XML to Gpad converter.
	 * 
	 * @param kernel
	 *            GeoGebra kernel
	 */
	public XMLToGpadConverter(Kernel kernel) {
		this.kernel = kernel;
		this.geoConverter = new GeoElementToGpadConverter();
	}

	/**
	 * Converts XML string to Gpad format.
	 * 
	 * @param xmlString
	 *            XML string to convert
	 * @return Gpad string representation
	 * @throws GpadParseException
	 *             if conversion fails
	 */
	public String convert(String xmlString) throws GpadParseException {
		if (xmlString == null || xmlString.trim().isEmpty()) {
			return "";
		}

		try {
			// Parse XML to create GeoElements
			App app = kernel.getApplication();
			if (app == null) {
				throw new GpadParseException("Application is null");
			}

			// Use app's XML processing capability
			// Note: This will add elements to the current construction
			// We need to be careful not to modify the existing construction
			
			// Get current construction elements count
			int startCount = kernel.getConstruction().getGeoSetConstructionOrder().size();
			
			// Parse XML using app's setXML method (simpler approach)
			// Wrap in minimal XML structure if needed
			String fullXML = xmlString;
			if (!xmlString.contains("<geogebra")) {
				fullXML = "<geogebra><construction>" + xmlString + "</construction></geogebra>";
			}
			app.setXML(fullXML, false);
			
			// Get newly created elements
			List<GeoElement> newElements = new ArrayList<>();
			java.util.TreeSet<GeoElement> allElements = kernel.getConstruction().getGeoSetConstructionOrder();
			int index = 0;
			for (GeoElement geo : allElements) {
				if (index >= startCount) {
					newElements.add(geo);
				}
				index++;
			}

			// Convert to Gpad
			return geoConverter.toGpad(newElements);

		} catch (Exception e) {
			throw new GpadParseException("Failed to convert XML to Gpad: " + e.getMessage(), e);
		}
	}

	/**
	 * Converts XML element tag to Gpad format.
	 * This is a more direct conversion without parsing through GeoElement.
	 * 
	 * @param elementXML
	 *            XML element string
	 * @return Gpad string representation
	 * @throws GpadParseException
	 *             if conversion fails
	 */
	public String convertElement(String elementXML) throws GpadParseException {
		// This is a simplified version - in production, you might want to
		// parse the XML more carefully and extract command and properties
		
		// For now, we'll use the full conversion approach
		// Wrap in minimal XML structure
		String fullXML = "<geogebra><construction>" + elementXML + "</construction></geogebra>";
		return convert(fullXML);
	}
}





