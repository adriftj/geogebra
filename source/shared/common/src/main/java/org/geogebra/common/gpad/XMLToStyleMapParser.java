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
					
					// Build bar data first to calculate length
					StringBuilder barData = new StringBuilder();
					
					// (1) barNumber: 1 char
					barData.append((char)(int)barNumber);
					
					// (2) flags: 1 char
					char flags = 0;
					String r = null, g = null, b = null, alpha = null;
					String fillType = barTags.get("barFillType");
					String hatchAngle = barTags.get("barHatchAngle");
					String hatchDistance = barTags.get("barHatchDistance");
					String image = barTags.get("barImage");
					String fillSymbol = barTags.get("barSymbol");
					
					// Check for barColor (format: rgb(r,g,b) or rgba(r,g,b,a))
					String barColor = barTags.get("barColor");
					if (barColor != null && (barColor.startsWith("rgb(") || barColor.startsWith("rgba(")) && barColor.endsWith(")")) {
						// Remove "rgb(" or "rgba(" prefix and ")" suffix
						int prefixLen = barColor.startsWith("rgba(") ? 5 : 4;
						String colorContent = barColor.substring(prefixLen, barColor.length() - 1);
						String[] rgb = colorContent.split(",");
						if (rgb.length >= 3) {
							try {
								r = rgb[0].trim();
								g = rgb[1].trim();
								b = rgb[2].trim();
								flags |= 0x01; // bit 0: barColor
								// Note: alpha in rgba() format is ignored here, as it's handled separately by barAlpha tag
							} catch (Exception e) {
								// Invalid color format, ignore
							}
						}
					}
					
					// Check for barAlpha
					String barAlpha = barTags.get("barAlpha");
					if (barAlpha != null) {
						alpha = barAlpha;
						flags |= 0x02; // bit 1: barAlpha
					}

					if (fillType != null)
						flags |= 0x04; // bit 2: barFillType
					if (hatchAngle != null)
						flags |= 0x08; // bit 3: barHatchAngle
					if (hatchDistance != null)
						flags |= 0x10; // bit 4: barHatchDistance
					if (image != null)
						flags |= 0x20; // bit 5: barImage
					if (fillSymbol != null)
						flags |= 0x40; // bit 6: barSymbol
					
					barData.append(flags);
					
					// (3) 属性值序列（按 flags 顺序）
					// bit 0: barColor (r, g, b)
					if ((flags & 0x01) != 0 && r != null && g != null && b != null) {
						try {
							int rVal = Integer.parseInt(r);
							int gVal = Integer.parseInt(g);
							int bVal = Integer.parseInt(b);
							barData.append((char)rVal);
							barData.append((char)gVal);
							barData.append((char)bVal);
						} catch (NumberFormatException e) {
							// Invalid color values, skip
						}
					}
					
					// bit 1: barAlpha
					if ((flags & 0x02) != 0 && alpha != null) {
						try {
							float alphaFloat = Float.parseFloat(alpha);
							int alphaInt = (int)(alphaFloat * 255);
							if (alphaInt < 0) alphaInt = 0;
							if (alphaInt > 255) alphaInt = 255;
							barData.append((char)alphaInt);
						} catch (NumberFormatException e) {
							// Invalid alpha, skip
						}
					}
					
					// bit 2: barFillType
					if ((flags & 0x04) != 0 && fillType != null) {
						try {
							int fillTypeInt = Integer.parseInt(fillType);
							if (fillTypeInt >= 0 && fillTypeInt <= 9) {
								barData.append((char)fillTypeInt);
							}
						} catch (NumberFormatException e) {
							// Invalid fillType, skip
						}
					}
					
					// bit 3: barHatchAngle
					if ((flags & 0x08) != 0 && hatchAngle != null) {
						try {
							int angle = Integer.parseInt(hatchAngle);
							if (angle < 0) angle = 0;
							if (angle > 65535) angle = 65535;
							barData.append((char)angle);
						} catch (NumberFormatException e) {
							// Invalid angle, skip
						}
					}
					
					// bit 4: barHatchDistance
					if ((flags & 0x10) != 0 && hatchDistance != null) {
						try {
							int dist = Integer.parseInt(hatchDistance);
							if (dist < 0) dist = 0;
							if (dist > 65535) dist = 65535;
							barData.append((char)dist);
						} catch (NumberFormatException e) {
							// Invalid distance, skip
						}
					}
					
					// bit 5: barImage (字符串，以 ETX 结尾)
					if ((flags & 0x20) != 0 && image != null) {
						// No need to escape control characters anymore, length prefix handles it
						barData.append(image);
						barData.append((char)3); // ETX: end of image string
					}
					
					// bit 6: barSymbol
					if ((flags & 0x40) != 0 && fillSymbol != null && fillSymbol.length() > 0) {
						barData.append(fillSymbol.charAt(0));
					}
					
					// Calculate length and prepend it
					int barLength = barData.length();
					if (barLength > 65535) {
						// Bar too long, skip it
						continue;
					}
					serialized.append((char)barLength);
					serialized.append(barData);
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
