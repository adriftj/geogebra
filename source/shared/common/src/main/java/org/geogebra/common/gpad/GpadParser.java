package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.parser.ParseException;
import org.geogebra.common.kernel.parser.Parser;

/**
 * Main Gpad parser that parses complete Gpad text and creates GeoElements.
 */
public class GpadParser {
	private Map<String, GpadStyleSheet> globalStyleSheets;
	private Kernel kernel;
	private Parser parser;

	/**
	 * Creates a new Gpad parser.
	 * 
	 * @param kernel GeoGebra kernel
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
	 * @throws CircularDefinitionException
	 *             if circular definition is detected
	 */
	public List<GeoElement> parse(String gpadText) throws GpadParseException, CircularDefinitionException {
		if (gpadText == null || gpadText.trim().isEmpty())
			return new ArrayList<>();

		try {
			List<GeoElement> results = parser.parseGpad(gpadText);
			
			// Update global style sheets
			Map<String, GpadStyleSheet> parserStyleSheets = parser.getGpadStyleSheets();
			if (parserStyleSheets != null)
				globalStyleSheets.putAll(parserStyleSheets);
			
			return results;
		} catch (CircularDefinitionException e) {
			// Let CircularDefinitionException propagate
			throw e;
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
			String errorMsg = e.getMessage();
			if (errorMsg == null) {
				errorMsg = "Parse error";
			}
			throw new GpadParseException("Parse error: " + errorMsg, line, column);
		} catch (Exception e) {
			if (e instanceof GpadParseException) {
				throw e;
			}
			String errorMsg = e.getMessage();
			if (errorMsg == null) {
				errorMsg = e.getClass().getSimpleName();
			}
			throw new GpadParseException("Error parsing Gpad: " + errorMsg, e);
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
	 * @throws CircularDefinitionException
	 *             if circular definition is detected
	 */
	public List<GeoElement> parsePartial(String gpadText) throws GpadParseException, CircularDefinitionException {
		// Save current style sheets
		Map<String, GpadStyleSheet> savedStyleSheets = new HashMap<>(globalStyleSheets);
		
		try {
			// Parse (this will use local style sheets)
			List<GeoElement> results = parse(gpadText);
			// Style sheets from parsing are merged into globalStyleSheets
			// We don't need to restore since they're already merged
			return results;
		} catch (GpadParseException | CircularDefinitionException e) {
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
