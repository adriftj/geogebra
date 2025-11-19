package org.geogebra.common.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geogebra.common.gpad.GpadParseException;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;

/**
 * Helper class to apply Gpad style sheets to GeoElements using ConsElementXMLHandler.
 * This class is in the same package as ConsElementXMLHandler so it can access
 * the protected startGeoElement method.
 */
public class GpadStyleXMLApplier {
	
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
				xmlHandler.startGeoElement(tagName, attrs, errors);
			}
		}

		// Finish processing
		xmlHandler.finish();

		if (!errors.isEmpty()) {
			throw new GpadParseException("Failed to apply style sheet: " + errors.toString());
		}
	}
}
