package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.geos.GeoElement;
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
	public List<GeoElement> parse(String gpadText) throws GpadParseException {
		if (gpadText == null || gpadText.trim().isEmpty())
			return new ArrayList<>();

		// parseGpad now only throws GpadParseException, so we can directly propagate
		List<GeoElement> results = parser.parseGpad(gpadText);
		
		// Update global style sheets
		Map<String, GpadStyleSheet> parserStyleSheets = parser.getGpadStyleSheets();
		if (parserStyleSheets != null)
			globalStyleSheets.putAll(parserStyleSheets);
		
		return results;
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

	/**
	 * Gets the list of warnings collected during the last parse operation.
	 * 
	 * @return list of warning messages, or null if no warnings were collected
	 */
	public List<String> getGpadWarnings() {
		if (parser == null)
			return null;
		return parser.getGpadWarnings();
	}
}
