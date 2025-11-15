package org.geogebra.common.gpad;

import org.geogebra.common.io.GpadStyleXMLApplier;
import org.geogebra.common.kernel.geos.GeoElement;

/**
 * Applies style sheet properties to GeoElement objects.
 * Uses GpadStyleXMLApplier (in org.geogebra.common.io package) to access
 * protected ConsElementXMLHandler methods.
 */
public class GpadStyleApplier {
	/**
	 * Static method to apply a style sheet to a GeoElement.
	 * This is called from Parser.jj.
	 * 
	 * @param styleSheet
	 *            style sheet to apply
	 * @param geo
	 *            GeoElement to apply styles to
	 */
	public static void apply(GpadStyleSheet styleSheet, GeoElement geo) {
		if (geo == null || styleSheet == null) {
			return;
		}

		try {
			// Use the helper class in org.geogebra.common.io package
			// which can access protected ConsElementXMLHandler methods
			GpadStyleXMLApplier.apply(styleSheet, geo);
		} catch (GpadParseException e) {
			// Log error but don't throw - allow parsing to continue
			org.geogebra.common.util.debug.Log.debug("Failed to apply style sheet: " + e.getMessage());
		}
	}
}
