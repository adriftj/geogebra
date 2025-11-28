package org.geogebra.common.gpad;

/**
 * Helper class for Gpad-related utility methods.
 */
public class GpadHelper {
	/**
	 * Extracts the <element>...</element> part from full XML.
	 * This is needed because QDParser may have issues with <expression> tags.
	 * 
	 * @param fullXML full XML string
	 * @return element XML string, or null if not found
	 */
	public static String extractElementXML(String fullXML) {
		if (fullXML == null || fullXML.trim().isEmpty()) {
			return null;
		}
		
		int startIdx = fullXML.indexOf("<element");
		if (startIdx < 0) {
			return null;
		}
		
		// Find the matching closing tag
		int depth = 0;
		int idx = startIdx;
		while (idx < fullXML.length()) {
			if (fullXML.startsWith("<element", idx)) {
				depth++;
				idx = fullXML.indexOf(">", idx) + 1;
			} else if (fullXML.startsWith("</element>", idx)) {
				depth--;
				if (depth == 0) {
					return fullXML.substring(startIdx, idx + "</element>".length());
				}
				idx += "</element>".length();
			} else {
				idx++;
			}
		}
		
		return null; // No matching closing tag found
	}
}

