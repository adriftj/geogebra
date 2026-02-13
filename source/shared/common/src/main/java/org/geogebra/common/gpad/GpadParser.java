package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.parser.Parser;

/**
 * Main Gpad parser – static GPAD↔XML conversion only, no runtime evaluation.
 */
public class GpadParser {
	private Kernel kernel;
	private Parser parser;

	/**
	 * Creates a new Gpad parser.
	 * 
	 * @param kernel GeoGebra kernel
	 */
	public GpadParser(Kernel kernel) {
		this.kernel = kernel;
		this.parser = new Parser(kernel);
	}

	/**
	 * Parses GPAD text statically (no kernel evaluation) and returns a list of
	 * {@link GpadStaticItem} that can be fed to
	 * {@link GpadToXmlStaticConverter}.
	 *
	 * @param gpadText GPAD text to parse
	 * @return list of static items
	 * @throws GpadParseException if parsing fails
	 */
	public List<GpadStaticItem> parseStaticItems(String gpadText) throws GpadParseException {
		if (gpadText == null || gpadText.trim().isEmpty())
			return new ArrayList<>();
		return parser.parseGpadStatic(gpadText);
	}

	/**
	 * Parses GPAD text statically (no kernel evaluation) and produces
	 * a construction XML string that can be loaded by {@code MyXMLio.loadXml()}.
	 *
	 * @param gpadText GPAD text to convert
	 * @return construction XML string ({@code <construction>...</construction>})
	 * @throws GpadParseException if parsing fails
	 */
	public String parseStaticToXml(String gpadText) throws GpadParseException {
		List<GpadStaticItem> items = parseStaticItems(gpadText);
		return GpadToXmlStaticConverter.buildConstructionXml(items);
	}

	/**
	 * Parses a GPAD stylesheet body string (e.g. {@code "{ show: val true; }"})
	 * into a {@link GpadStyleSheet}.
	 *
	 * @param styleSheetStr stylesheet body string
	 * @return parsed stylesheet
	 * @throws GpadParseException if parsing fails
	 */
	public GpadStyleSheet parseStyleSheet(String styleSheetStr) throws GpadParseException {
		if (styleSheetStr == null || styleSheetStr.trim().isEmpty())
			return new GpadStyleSheet("");
		return parser.parseGpadStyleSheet(styleSheetStr);
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
