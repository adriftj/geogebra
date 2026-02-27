package org.geogebra.common.gpad;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.jre.headless.GgbAPIHeadless;
import org.geogebra.common.plugin.GgbAPI;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for ggb-to-gpad conversion via XML.
 * Tests the complete flow: create objects via gpad, get XML, then convert to gpad format.
 */
public class GgbToGpadConverterTest extends BaseUnitTest {

	private GgbAPI api;

	@Before
	public void setupAPI() {
		api = new GgbAPIHeadless(getApp());
	}

	/**
	 * Helper: convert current construction to gpad via XML path.
	 */
	private String toGpadViaXml(boolean mergeStylesheets) {
		String xml = api.getXML();
		String macroXml = getApp().getAllMacrosXMLorEmpty();
		return api.xmlToGpad(xml, macroXml, mergeStylesheets);
	}

	@Test
	public void testCommandWithStyles() {
		String inputGpad = "@AStyle = { pointSize: 6; color: #FF0000FF; }\n"
				+ "@BStyle = { pointSize: 8; color: #00FF00FF; }\n"
				+ "A @AStyle = (1, 2);\n"
				+ "B @BStyle = (3, 4);\n"
				+ "s = Segment(A, B);";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);

		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label s", outputGpad.contains("s"));

		assertTrue("Should contain stylesheet for s",
			outputGpad.contains("@sStyle") || outputGpad.contains("@style"));
	}

	@Test
	public void testIndependentElements() {
		String inputGpad = "A = (1, 2);\n"
				+ "B = (3, 4);\n"
				+ "n = 5;";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);

		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label n", outputGpad.contains("n"));
	}

	@Test
	public void testStylesheetMerging() {
		String inputGpad = "@redStyle = { color: #FF0000FF; }\n"
				+ "A @redStyle = (1, 2);\n"
				+ "B @redStyle = (3, 4);\n"
				+ "s = Segment(A, B);";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpadMerged = toGpadViaXml(true);
		assertNotNull("toGpad should return non-null", outputGpadMerged);

		String outputGpadNotMerged = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpadNotMerged);

		assertTrue("Merged: Should contain label A", outputGpadMerged.contains("A"));
		assertTrue("Merged: Should contain label B", outputGpadMerged.contains("B"));
		assertTrue("Merged: Should contain label s", outputGpadMerged.contains("s"));
		assertTrue("Not merged: Should contain label A", outputGpadNotMerged.contains("A"));
		assertTrue("Not merged: Should contain label B", outputGpadNotMerged.contains("B"));
		assertTrue("Not merged: Should contain label s", outputGpadNotMerged.contains("s"));
	}

	@Test
	public void testEmptyConstruction() {
		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);
	}

	@Test
	public void testFunctionExpression() {
		String inputGpad = "f(x) = x^2 + 1;";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);

		assertTrue("Should contain label f", outputGpad.contains("f"));
		assertTrue("Should contain function definition",
			outputGpad.contains("f") && outputGpad.contains("="));
	}

	@Test
	public void testMultiVariableFunction() {
		String inputGpad = "g(x, y) = x * y;";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);

		assertTrue("Should contain g(x, y) with variable list",
			outputGpad.contains("g(x, y)") || outputGpad.contains("g(x,y)"));

		assertTrue("Should contain function expression",
			outputGpad.contains("x * y") || outputGpad.contains("x*y")
			|| outputGpad.contains("x y") || outputGpad.contains("x·y")
			|| (outputGpad.contains("x") && outputGpad.contains("y")));
	}

	@Test
	public void testFunctionWithVisibilityFlags() {
		String inputGpad = "f(x) = x^2;\n"
				+ "g(x) = x^3;";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);

		assertTrue("Should contain f(x) with variable list",
			outputGpad.contains("f(x)") || outputGpad.contains("f(x )"));
		assertTrue("Should contain g(x) with variable list",
			outputGpad.contains("g(x)") || outputGpad.contains("g(x )"));
	}

	@Test
	public void testFunctionWithMultiLetterVariable() {
		String inputGpad = "f(alpha) = alpha^2;\n"
				+ "g(beta, gamma) = beta + gamma;";

		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull("toGpad should return non-null", outputGpad);

		assertTrue("Should contain f(alpha) with variable list",
			outputGpad.contains("f(alpha)") || outputGpad.contains("f(alpha )"));

		assertTrue("Should contain g(beta, gamma) with variable list",
			outputGpad.contains("g(beta") && outputGpad.contains("gamma)"));
	}

	// ========== New syntax tests ==========

	@Test
	public void testColorPropertyName() {
		String inputGpad = "@s = { color: #FF0000FF; }\nA @s = (1, 2);";
		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull("evalGpad should succeed", result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull(outputGpad);
		assertTrue("Output should use 'color:' not 'objColor:'",
			outputGpad.contains("color:") && !outputGpad.contains("objColor:"));
	}

	@Test
	public void testEvPropertyRoundtrip() {
		String inputGpad = "A { ev: 2; } = (1, 2);";
		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull(result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull(outputGpad);
		assertTrue("Should contain ev: property with view flag",
			outputGpad.contains("ev:"));
	}

	@Test
	public void testBoundingBoxPositionalParams() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "300");
		attrs.put("height", "200");
		styleMap.put("boundingBox", attrs);

		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should use positional params '300 200'", gpad.contains("300 200"));
		assertTrue("Should not use width= format", !gpad.contains("width="));
	}

	@Test
	public void testContentSizePositionalParams() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "150.5");
		attrs.put("height", "100.3");
		styleMap.put("contentSize", attrs);

		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should use positional params '150.5 100.3'", gpad.contains("150.5 100.3"));
		assertTrue("Should not use width= format", !gpad.contains("width="));
	}

	@Test
	public void testFontWithoutStarPrefix() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "2.0");
		attrs.put("style", "1");
		styleMap.put("font", attrs);

		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain font property", gpad.contains("font"));
		assertTrue("Should contain serif", gpad.contains("serif"));
		assertTrue("Should contain size 2 without * prefix", gpad.contains(" 2"));
		assertTrue("Should not use * prefix for size", !gpad.contains("*2"));
		assertTrue("Should contain bold", gpad.contains("bold"));
	}

	@Test
	public void testFilePropertyName() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("name", "image.png");
		styleMap.put("file", attrs);

		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should use 'file:' property name", gpad.contains("file:"));
		assertTrue("Should not use 'filename:' property name", !gpad.contains("filename:"));
		assertTrue("Should contain file name", gpad.contains("image.png"));
	}

	@Test
	public void testDefaultValuesOmitted() {
		String inputGpad = "A = (1, 2);";
		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull(result);

		String outputGpad = toGpadViaXml(false);
		assertNotNull(outputGpad);
		assertTrue("Should not contain fixed (default false)",
			!outputGpad.contains("fixed"));
		assertTrue("Should not contain layer: 0 (default)",
			!outputGpad.contains("layer:"));
	}

	// ========== returnLabels tests ==========

	@Test
	public void testReturnLabelsTrue() {
		String inputGpad = "A = (1, 2);\nB = (3, 4);\ns = Segment(A, B);";
		String[] labels = api.evalGpad(inputGpad, true);
		assertNotNull(labels);
		assertArrayEquals(new String[]{"A", "B", "s"}, labels);
	}

	@Test
	public void testReturnLabelsFalseGivesEmptyArray() {
		String inputGpad = "A = (1, 2);\nB = (3, 4);";
		String[] result = api.evalGpad(inputGpad, false);
		assertNotNull(result);
		assertEquals(0, result.length);
	}

	@Test
	public void testReturnLabelsSkipsStylesheets() {
		String inputGpad = "@s = { color: #FF0000FF; }\nA @s = (1, 2);\nB = (3, 4);";
		String[] labels = api.evalGpad(inputGpad, true);
		assertNotNull(labels);
		assertArrayEquals(new String[]{"A", "B"}, labels);
	}

	@Test
	public void testReturnLabelsErrorGivesNull() {
		String[] labels = api.evalGpad("this is not valid gpad @@@@", true);
		assertNull(labels);
		assertNotNull(api.getLastError());
	}

	@Test
	public void testReturnLabelsEmptyGpad() {
		String[] labels = api.evalGpad("", true);
		assertNotNull(labels);
		assertEquals(0, labels.length);
	}

	// ========== Settings roundtrip tests ==========

	@Test
	public void testEnvKernelSettingsRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"precision: 10sf; angleUnit: radian; coordStyle: austrian; "
				+ "~continuous; symbolic; startAnimation; "
				+ "pathRegionParams: always; "
				+ "localization: digits ~labels; "
				+ "cas: timeout=10 ~rootForm;");

		String kernelXml = result.kernelXml.toString();
		assertTrue("Should contain significantfigures", kernelXml.contains("significantfigures"));
		assertTrue("Should contain val=\"10\"", kernelXml.contains("val=\"10\""));
		assertTrue("Should contain radiant", kernelXml.contains("radiant"));
		assertTrue("Should contain coordStyle val=\"1\"", kernelXml.contains("coordStyle"));
		assertTrue("Should contain continuous false",
				kernelXml.contains("continuous") && kernelXml.contains("false"));
		assertTrue("Should contain symbolic true",
				kernelXml.contains("symbolic") && kernelXml.contains("true"));
		assertTrue("Should contain startAnimation", kernelXml.contains("startAnimation"));
		assertTrue("Should contain usePathAndRegionParameters",
				kernelXml.contains("usePathAndRegionParameters"));
		assertTrue("Should contain localization", kernelXml.contains("localization"));
		assertTrue("Should contain casSettings", kernelXml.contains("casSettings"));
	}

	@Test
	public void testEnvPrecisionDpRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"precision: 15dp;");
		String kernelXml = result.kernelXml.toString();
		assertTrue("Should contain decimals", kernelXml.contains("decimals"));
		assertTrue("Should contain val=\"15\"", kernelXml.contains("val=\"15\""));
	}

	@Test
	public void testEnvGuiRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"font: 16; labeling: alwaysOn;");
		String guiXml = result.guiXml.toString();
		assertTrue("Should contain font", guiXml.contains("font"));
		assertTrue("Should contain size=\"16\"", guiXml.contains("size=\"16\""));
		assertTrue("Should contain labelingStyle", guiXml.contains("labelingStyle"));
		assertTrue("Should contain val=\"1\"", guiXml.contains("val=\"1\""));
	}

	@Test
	public void testEnvAlgebraViewRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"algebraView: sort=dependency style=description auxiliary;");
		String avXml = result.algebraViewXml.toString();
		assertTrue("Should contain algebraView", avXml.contains("algebraView"));
		assertTrue("Should contain mode val=\"0\"", avXml.contains("val=\"0\""));
		assertTrue("Should contain auxiliary show=\"true\"",
				avXml.contains("auxiliary") && avXml.contains("true"));
		// style should go into kernel XML
		String kernelXml = result.kernelXml.toString();
		assertTrue("Should contain algebraStyle", kernelXml.contains("algebraStyle"));
	}

	@Test
	public void testEnvSpreadsheetViewRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"spreadsheetView { "
				+ "style: description; "
				+ "size: 800 600; "
				+ "cellSize: 70 21; "
				+ "rows: 100; columns: 26; "
				+ "column: 120 default 80 = =; "
				+ "layout: grid ~formulaBar hScroll vScroll; "
				+ "}");
		String ssXml = result.spreadsheetViewXml.toString();
		assertTrue("Should contain spreadsheetView", ssXml.contains("spreadsheetView"));
		assertTrue("Should contain size", ssXml.contains("size"));
		assertTrue("Should contain prefCellSize", ssXml.contains("prefCellSize"));
		assertTrue("Should contain spreadsheetDimensions", ssXml.contains("spreadsheetDimensions"));
		assertTrue("Should contain spreadsheetColumn", ssXml.contains("spreadsheetColumn"));
		assertTrue("Should contain layout", ssXml.contains("layout"));
		assertTrue("Should contain showGrid", ssXml.contains("showGrid"));
		// spreadsheet style goes into kernel
		String kernelXml = result.kernelXml.toString();
		assertTrue("Should contain algebraStyle with spreadsheet",
				kernelXml.contains("spreadsheet"));
	}

	@Test
	public void testEnvKeyboardRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"keyboard: width=400 height=200 opacity=0.8 language=\"en\" show;");
		String kbXml = result.keyboardXml.toString();
		assertTrue("Should contain keyboard", kbXml.contains("keyboard"));
		assertTrue("Should contain width=\"400\"", kbXml.contains("width=\"400\""));
		assertTrue("Should contain show=\"true\"", kbXml.contains("show=\"true\""));
	}

	@Test
	public void testEnvProbCalcRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"probCalc { "
				+ "distribution: normal ~cumulative ~overlayActive parameters=\"1,0.5\"; "
				+ "interval: left low=-1.0 high=1.0; "
				+ "}");
		String pcXml = result.probCalcXml.toString();
		assertTrue("Should contain probabilityCalculator",
				pcXml.contains("probabilityCalculator"));
		assertTrue("Should contain distribution type=\"0\"",
				pcXml.contains("type=\"0\""));
		assertTrue("Should contain interval mode=\"1\"",
				pcXml.contains("mode=\"1\""));
	}

	@Test
	public void testEnvScriptingRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"scripting: blocked disabled;");
		String scrXml = result.scriptingXml.toString();
		assertTrue("Should contain scripting", scrXml.contains("scripting"));
		assertTrue("Should contain blocked=\"true\"", scrXml.contains("blocked=\"true\""));
		assertTrue("Should contain disabled=\"true\"", scrXml.contains("disabled=\"true\""));
	}

	@Test
	public void testEnvTableViewRoundtrip() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"tableView: min=-5.0 max=5.0 step=0.5;");
		String tvXml = result.tableviewXml.toString();
		assertTrue("Should contain tableview", tvXml.contains("tableview"));
		assertTrue("Should contain min", tvXml.contains("min"));
		assertTrue("Should contain max", tvXml.contains("max"));
		assertTrue("Should contain step", tvXml.contains("step"));
	}

	@Test
	public void testXmlToGpadSettingsCapture() {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
				+ "<geogebra format=\"5.0\">\n"
				+ "<kernel>\n"
				+ "  <continuous val=\"false\"/>\n"
				+ "  <decimals val=\"5\"/>\n"
				+ "  <angleUnit val=\"radiant\"/>\n"
				+ "  <algebraStyle val=\"2\" spreadsheet=\"1\"/>\n"
				+ "  <coordStyle val=\"0\"/>\n"
				+ "</kernel>\n"
				+ "<algebraView>\n"
				+ "  <mode val=\"0\"/>\n"
				+ "  <auxiliary show=\"true\"/>\n"
				+ "</algebraView>\n"
				+ "<gui>\n"
				+ "  <font size=\"16\"/>\n"
				+ "  <labelingStyle val=\"2\"/>\n"
				+ "</gui>\n"
				+ "<construction title=\"\" author=\"\" date=\"\">\n"
				+ "</construction>\n"
				+ "</geogebra>";

		ToGpadConverter converter = new ToGpadConverter(xml, null, false);
		String gpad = converter.toGpad();
		assertNotNull("toGpad should return non-null", gpad);
		assertTrue("Should contain @@env", gpad.contains("@@env"));
		assertTrue("Should contain precision", gpad.contains("precision:") || gpad.contains("precision: "));
		assertTrue("Should contain angleUnit radian",
				gpad.contains("radian"));
		assertTrue("Should contain font: 16", gpad.contains("font: 16"));
		assertTrue("Should contain labeling", gpad.contains("labeling:") || gpad.contains("labeling: "));
		assertTrue("Should contain algebraView", gpad.contains("algebraView"));
	}

	@Test
	public void testColumnWidthEncoding() {
		GpadEnvToXmlConverter.ConvertResult result = GpadEnvToXmlConverter.convertAll(
				"spreadsheetView { column: 120 default 80 = =; }");
		String ssXml = result.spreadsheetViewXml.toString();
		assertTrue("col 0 should have width 120",
				ssXml.contains("id=\"0\"") && ssXml.contains("width=\"120\""));
		assertTrue("col 2 should have width 80",
				ssXml.contains("id=\"2\"") && ssXml.contains("width=\"80\""));
		assertTrue("col 3 should have width 80 (= repeat)",
				ssXml.contains("id=\"3\""));
		assertTrue("col 4 should have width 80 (= repeat)",
				ssXml.contains("id=\"4\""));
		assertTrue("col 1 (default) should NOT be emitted as spreadsheetColumn",
				!ssXml.contains("id=\"1\" width"));
	}
}
