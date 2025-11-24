package org.geogebra.common.gpad;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.io.DocHandler;
import org.geogebra.common.io.QDParser;
import org.geogebra.common.io.XMLParseException;

/**
 * Parses XML element tags and converts them to a Map structure
 * that can be used by GpadStyleSheet.
 * 
 * This parser extracts all child elements of an <element> tag
 * and converts them to Map<String, LinkedHashMap<String, String>> format
 * where the key is the XML tag name and the value is the attributes map.
 */
public class XMLToStyleMapParser implements DocHandler {
	private Map<String, LinkedHashMap<String, String>> styleMap;
	private boolean inElement = false;
	private int elementDepth = 0;
	// Collect startPoint elements (dynamically growing list)
	private List<LinkedHashMap<String, String>> startPoints;

	/**
	 * Parses XML string and extracts style properties.
	 * 
	 * @param xmlString
	 *            XML string (should contain <element> tag with child elements)
	 * @return map from XML tag names to attribute maps
	 * @throws GpadParseException
	 *             if parsing fails
	 */
	public Map<String, LinkedHashMap<String, String>> parse(String xmlString) throws GpadParseException {
		styleMap = new LinkedHashMap<>();
		inElement = false;
		elementDepth = 0;

		try {
			QDParser parser = new QDParser();
			parser.parse(this, new StringReader(xmlString));
			return styleMap;
		} catch (XMLParseException e) {
			throw new GpadParseException("Failed to parse XML: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new GpadParseException("Error parsing XML: " + e.getMessage(), e);
		}
	}

	@Override
	public void startDocument() {
		styleMap.clear();
		inElement = false;
		elementDepth = 0;
		// Initialize startPoint list
		startPoints = new ArrayList<>();
	}

	@Override
	public void startElement(String tag, LinkedHashMap<String, String> attrs) {
		if ("element".equals(tag)) {
			inElement = true;
			elementDepth = 0;
			// Reset startPoint collection for new element
			startPoints.clear();
		} else if (inElement) {
			LinkedHashMap<String, String> elementAttrs = new LinkedHashMap<>();
			if (attrs != null)
				elementAttrs.putAll(attrs);
			// Special handling for startPoint: collect for later serialization
			if ("startPoint".equals(tag)) {
				// Use number attribute as list index
				int number = 0;
				String numberStr = elementAttrs.get("number");
				if (numberStr != null) {
					try {
						number = Integer.parseInt(numberStr);
					} catch (NumberFormatException e) {
						// Invalid number, use 0 as default
						number = 0;
					}
				}
				
				if (number < 0)
					org.geogebra.common.util.debug.Log.error(
						"startPoint number attribute is negative: " + number + ". Ignoring.");
				else {
					// Grow list if necessary
					while (startPoints.size() <= number)
						startPoints.add(null);
					startPoints.set(number, elementAttrs);
				}
			} else
				styleMap.put(tag, elementAttrs); // Store child element of <element> in the style map
			elementDepth++;
		}
	}

	@Override
	public void endElement(String tag) {
		if ("element".equals(tag)) {
			// Serialize all collected startPoint elements (in order by number)
			// Stop at first null, ignore any non-null entries after that (with error log)
			if (!startPoints.isEmpty()) {
				StringBuilder serialized = new StringBuilder();
				boolean first = true;
				
				for (int i = 0; i < startPoints.size(); i++) {
					LinkedHashMap<String, String> corner = startPoints.get(i);
					if (corner == null) {
						// Check if there are any non-null entries after this
						for (int j = i + 1; j < startPoints.size(); j++) {
							if (startPoints.get(j) != null) {
								org.geogebra.common.util.debug.Log.error(
									"startPoint: found null at index " + i + ", but non-null entry exists at index " + j + ". Ignoring entries after index " + i);
								break;
							}
						}
						break;
					}
					
					if (!first)
						serialized.append('\u0001'); // SOH: separator between corners
					first = false;
					
					// (1) absolute: \u0002=true, \u0003=false
					boolean isAbsolute = "true".equals(corner.get("absolute"));
					serialized.append(isAbsolute ? '\u0002' : '\u0003');
					
					// (2) type: \u0002=exp, \u0003=x/y/z
					String exp = corner.get("exp");
					if (exp != null) {
						// exp type
						serialized.append('\u0002');
						// (3) exp content
						serialized.append(exp);
					} else {
						// x/y/z type
						serialized.append('\u0003');
						// (3) x,y,z content (z optional, no trailing comma)
						String x = corner.get("x");
						String y = corner.get("y");
						String z = corner.get("z");
						if (x != null && y != null) {
							serialized.append(x);
							serialized.append(',');
							serialized.append(y);
							if (z != null) {
								serialized.append(',');
								serialized.append(z);
							}
						}
					}
				}
				
				// Store serialized string in styleMap only if we have at least one corner
				if (!first) {
					LinkedHashMap<String, String> startPointAttrs = new LinkedHashMap<>();
					startPointAttrs.put("_corners", serialized.toString());
					styleMap.put("startPoint", startPointAttrs);
				}
			}
			
			inElement = false;
			elementDepth = 0;
		} else if (inElement && elementDepth > 0)
			elementDepth--;
	}

	@Override
	public void endDocument() {
		// Nothing to do
	}

	@Override
	public void text(String str) throws XMLParseException {
		// Ignore text content
	}
}
