package org.geogebra.common.gpad;

import java.io.StringReader;
import java.util.LinkedHashMap;
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
	}

	@Override
	public void startElement(String tag, LinkedHashMap<String, String> attrs) {
		if ("element".equals(tag)) {
			inElement = true;
			elementDepth = 0;
		} else if (inElement) {
			// This is a child element of <element>
			// Store it in the style map
			LinkedHashMap<String, String> elementAttrs = new LinkedHashMap<>();
			if (attrs != null) {
				elementAttrs.putAll(attrs);
			}
			styleMap.put(tag, elementAttrs);
			elementDepth++;
		}
	}

	@Override
	public void endElement(String tag) {
		if ("element".equals(tag)) {
			inElement = false;
			elementDepth = 0;
		} else if (inElement && elementDepth > 0) {
			elementDepth--;
		}
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





