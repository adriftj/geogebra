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
	// Collect tag elements (grouped by barNumber)
	private Map<Integer, Map<String, String>> tagElements;

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
		// Initialize tag elements map
		tagElements = new LinkedHashMap<>();
	}
	
	@Override
	public void startElement(String tag, LinkedHashMap<String, String> attrs) {
		if ("element".equals(tag)) {
			inElement = true;
			elementDepth = 0;
			// Reset collections for new element
			startPoints.clear();
			tagElements.clear();
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
			} else if ("tag".equals(tag)) {
				// Special handling for tag: collect by barNumber for later serialization
				String barNumberStr = elementAttrs.get("barNumber");
				String key = elementAttrs.get("key");
				String value = elementAttrs.get("value");
				
				if (barNumberStr != null && key != null && value != null) {
					try {
						int barNumber = Integer.parseInt(barNumberStr);
						// Get or create map for this barNumber
						Map<String, String> barTags = tagElements.get(barNumber);
						if (barTags == null) {
							barTags = new LinkedHashMap<>();
							tagElements.put(barNumber, barTags);
						}
						// Store key-value pair for this barNumber
						barTags.put(key, value);
					} catch (NumberFormatException e) {
						// Invalid barNumber, ignore
					}
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
					
					// Extract corner data
					boolean isAbsolute = "true".equals(corner.get("absolute"));
					String[] cornerData = new String[4]; // [0]=exp, [1]=x, [2]=y, [3]=z
					cornerData[0] = corner.get("exp");
					cornerData[1] = corner.get("x");
					cornerData[2] = corner.get("y");
					cornerData[3] = corner.get("z");
					
					// Serialize using helper class
					GpadSerializer.serializeStartPointCorner(serialized, first, isAbsolute, cornerData);
					first = false;
				}
				
				// Store serialized string in styleMap only if we have at least one corner
				if (!first) {
					LinkedHashMap<String, String> startPointAttrs = new LinkedHashMap<>();
					startPointAttrs.put("_corners", serialized.toString());
					styleMap.put("startPoint", startPointAttrs);
				}
			}
			
			// Serialize all collected tag elements (grouped by barNumber)
			if (!tagElements.isEmpty()) {
				StringBuilder serialized = new StringBuilder();
				
				// Sort by barNumber for consistent output
				List<Integer> barNumbers = new ArrayList<>(tagElements.keySet());
				java.util.Collections.sort(barNumbers);
				
				for (Integer barNumber : barNumbers) {
					Map<String, String> barTags = tagElements.get(barNumber);
					if (barTags == null || barTags.isEmpty())
						continue;
					
					// Extract bar attributes
					String fillType = barTags.get("barFillType");
					String hatchAngle = barTags.get("barHatchAngle");
					String hatchDistance = barTags.get("barHatchDistance");
					String image = barTags.get("barImage");
					String fillSymbol = barTags.get("barSymbol");
					
					// Parse barColor (format: rgb(r,g,b) or rgba(r,g,b,a))
					int[] rgba = {-1, -1, -1, -1};
					String barColor = barTags.get("barColor");
					if (barColor != null && (barColor.startsWith("rgb(") || barColor.startsWith("rgba(")) && barColor.endsWith(")")) {
						// Remove "rgb(" or "rgba(" prefix and ")" suffix
						int prefixLen = barColor.startsWith("rgba(") ? 5 : 4;
						String colorContent = barColor.substring(prefixLen, barColor.length() - 1);
						String[] rgb = colorContent.split(",");
						if (rgb.length >= 3) {
							try {
								rgba[0] = Integer.parseInt(rgb[0].trim());
								rgba[1] = Integer.parseInt(rgb[1].trim());
								rgba[2] = Integer.parseInt(rgb[2].trim());
							} catch (NumberFormatException e) {
								// Invalid color format, ignore
							}
						}
					}
					
					// Parse barAlpha (format: float string 0.0-1.0, convert to int 0-255)
					String barAlpha = barTags.get("barAlpha");
					if (barAlpha != null) {
						try {
							float alphaFloat = Float.parseFloat(barAlpha);
							int alphaInt = (int)(alphaFloat * 255);
							if (alphaInt < 0) alphaInt = 0;
							if (alphaInt > 255) alphaInt = 255;
							rgba[3] = alphaInt;
						} catch (NumberFormatException e) {
							// Invalid alpha, ignore
						}
					}
					
					// Serialize using helper class
					if (!GpadSerializer.serializeBarTagBar(serialized, String.valueOf(barNumber), rgba,
							fillType, hatchAngle, hatchDistance, image, fillSymbol)) {
						// Serialization failed, skip this bar
						continue;
					}
				}
				
				// Store serialized string in styleMap only if we have at least one bar
				if (serialized.length() > 0) {
					LinkedHashMap<String, String> barTagAttrs = new LinkedHashMap<>();
					barTagAttrs.put("_barTags", serialized.toString());
					styleMap.put("barTag", barTagAttrs);
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
