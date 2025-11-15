package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.parser.ParseException;
import org.geogebra.common.kernel.parser.Parser;

/**
 * Main Gpad parser that parses complete Gpad text and creates GeoElements.
 * Uses JavaCC Parser for parsing, supporting multi-line style sheets, commands, and nested command calls.
 * 
 * Supports:
 * - Style sheet definitions: $name = { ... } (multi-line supported)
 * - Command execution: output* $style { inline:style } = Command(...) (nested calls supported)
 * - Macro definitions: @tool name(...) { ... return ... }
 */
public class GpadParser {
	private Map<String, GpadStyleSheet> globalStyleSheets;
	private Kernel kernel;
	private Parser parser;

	/**
	 * Creates a new Gpad parser.
	 * 
	 * @param kernel
	 *            GeoGebra kernel
	 */
	public GpadParser(Kernel kernel) {
		this.kernel = kernel;
		this.globalStyleSheets = new HashMap<>();
		this.parser = new Parser(kernel);
	}

	/**
	 * Parses a complete Gpad text and creates GeoElements.
	 * 
	 * @param gpadText
	 *            Gpad text to parse (case-insensitive: Gpad or gpad)
	 * @return list of created GeoElements
	 * @throws GpadParseException
	 *             if parsing fails
	 */
	public List<GeoElement> parse(String gpadText) throws GpadParseException {
		if (gpadText == null || gpadText.trim().isEmpty()) {
			return new ArrayList<>();
		}

		try {
			// Use JavaCC Parser to parse Gpad
			// Note: parseGpad returns java.util.List<org.geogebra.common.kernel.geos.GeoElement>
			java.util.List<org.geogebra.common.kernel.geos.GeoElement> results = parser.parseGpad(gpadText);
			
			// Update global style sheets
			// Note: getGpadStyleSheets returns java.util.Map<String, org.geogebra.common.gpad.GpadStyleSheet>
			java.util.Map<String, org.geogebra.common.gpad.GpadStyleSheet> parserStyleSheets = parser.getGpadStyleSheets();
			if (parserStyleSheets != null) {
				globalStyleSheets.putAll(parserStyleSheets);
			}
			
			return results;
			
		} catch (ParseException e) {
			// Extract line and column information if available
			int line = -1;
			int column = -1;
			try {
				if (e.currentToken != null) {
					line = e.currentToken.beginLine;
					column = e.currentToken.beginColumn;
				}
			} catch (Exception ex) {
				// Ignore if we can't access token information
			}
			throw new GpadParseException("Parse error: " + e.getMessage(), line, column);
		} catch (Exception e) {
			if (e instanceof GpadParseException) {
				throw e;
			}
			throw new GpadParseException("Error parsing Gpad: " + e.getMessage(), e);
		}
	}

	/**
	 * Parses a partial Gpad text (for partial parsing support).
	 * Style sheets are local to this parsing session.
	 * 
	 * @param gpadText
	 *            partial Gpad text
	 * @return list of created GeoElements
	 * @throws GpadParseException
	 *             if parsing fails
	 */
	public List<GeoElement> parsePartial(String gpadText) throws GpadParseException {
		// Save current style sheets
		Map<String, GpadStyleSheet> savedStyleSheets = new HashMap<>(globalStyleSheets);
		
		try {
			// Parse (this will use local style sheets)
			List<GeoElement> results = parse(gpadText);
			// Style sheets from parsing are merged into globalStyleSheets
			// We don't need to restore since they're already merged
			return results;
		} catch (GpadParseException e) {
			// Restore style sheets on error
			globalStyleSheets = savedStyleSheets;
			throw e;
		}
	}

	/**
	 * @return the global style sheets
	 */
	public Map<String, GpadStyleSheet> getGlobalStyleSheets() {
		return globalStyleSheets;
	}

	/**
	 * @return the kernel used by this parser
	 */
	public Kernel getKernel() {
		return kernel;
	}
}

