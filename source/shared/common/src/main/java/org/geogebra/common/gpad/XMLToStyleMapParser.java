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
	// Serializers for startPoint and tag elements
	private GpadSerializer.GpadSerializeStartPoint startPointSerializer;
	private GpadSerializer.GpadSerializeBarTag barTagSerializer;

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
		// Initialize serializers for startPoint and tag elements
		startPointSerializer = GpadSerializer.beginSerializeStartPoint();
		barTagSerializer = GpadSerializer.beginSerializeBarTag();
	}
	
	@Override
	public void startElement(String tag, LinkedHashMap<String, String> attrs) {
		if ("element".equals(tag)) {
			inElement = true;
			elementDepth = 0;
			// Reset serializers for new element
			startPointSerializer = GpadSerializer.beginSerializeStartPoint();
			barTagSerializer = GpadSerializer.beginSerializeBarTag();
		} else if (inElement) {
			LinkedHashMap<String, String> elementAttrs = new LinkedHashMap<>();
			if (attrs != null)
				elementAttrs.putAll(attrs);
			// Special handling for startPoint: collect for later serialization
			if ("startPoint".equals(tag)) {
				if (startPointSerializer != null)
					startPointSerializer.add(elementAttrs);
			} else if ("tag".equals(tag)) {
				// Special handling for tag: collect by barNumber for later serialization
				String barNumberStr = elementAttrs.get("barNumber");
				if (barTagSerializer != null && barNumberStr != null)
					barTagSerializer.add(barNumberStr, elementAttrs);
			} else if ("listener".equals(tag)) {
				// Special handling for listener: convert to jsUpdateFunction or jsClickFunction
				String type = elementAttrs.get("type");
				String val = elementAttrs.get("val");
				if (val != null && !val.isEmpty()) {
					LinkedHashMap<String, String> listenerAttrs = new LinkedHashMap<>();
					listenerAttrs.put("val", val);
					if ("objectUpdate".equals(type))
						styleMap.put("jsUpdateFunction", listenerAttrs);
					else if ("objectClick".equals(type))
						styleMap.put("jsClickFunction", listenerAttrs);
				}
			} else
				styleMap.put(tag, elementAttrs); // Store child element of <element> in the style map
			elementDepth++;
		}
	}

	@Override
	public void endElement(String tag) {
		if ("element".equals(tag)) {
			// Serialize startPoint elements if any
			if (startPointSerializer != null) {
				String serialized = startPointSerializer.end();
				if (serialized != null) {
					LinkedHashMap<String, String> startPointAttrs = new LinkedHashMap<>();
					startPointAttrs.put("_corners", serialized);
					styleMap.put("startPoint", startPointAttrs);
				}
			}
			
			// Serialize tag elements if any
			if (barTagSerializer != null) {
				String serialized = barTagSerializer.end();
				if (serialized != null) {
					LinkedHashMap<String, String> barTagAttrs = new LinkedHashMap<>();
					barTagAttrs.put("_barTags", serialized);
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
