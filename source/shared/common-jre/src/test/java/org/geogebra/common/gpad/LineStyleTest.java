package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.junit.Test;

/**
 * Unit tests for lineStyle parsing and conversion.
 */
public class LineStyleTest extends BaseUnitTest {

	/**
	 * Test parsing lineStyle with all five parts (example 1 from requirements).
	 */
	@Test
	public void testParseLineStyleExample1() {
		String gpad = "@style1 = { lineStyle: dashedlong thickness=5.3 hidden opacity=128.6 ~arrow }\n"
				+ "g @style1 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			assertEquals(15, geo.getLineType()); // dashedlong -> 15
			assertEquals(5, geo.getLineThickness()); // 5.3 -> 5 (decimal ignored)
			assertEquals(0, geo.getLineTypeHidden()); // hidden (no value) -> 0
			assertEquals(128, geo.getLineOpacity()); // 128.6 -> 128 (decimal ignored)
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			
			// Check XML attributes
			assertEquals("15", attrs.get("type")); // dashedlong -> 15
			assertEquals("5", attrs.get("thickness")); // 5.3 -> 5 (decimal ignored)
			assertEquals("0", attrs.get("typeHidden")); // hidden (no value) -> 0
			assertEquals("128", attrs.get("opacity")); // 128.6 -> 128 (decimal ignored)
			assertEquals("false", attrs.get("drawArrow")); // ~arrow -> false
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with all five parts in different order (example 2 from requirements).
	 */
	@Test
	public void testParseLineStyleExample2() {
		String gpad = "@style2 = { lineStyle: hidden=dashed arrow thickness=5.3 full opacity=127 }\n"
				+ "g @style2 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			assertEquals(0, geo.getLineType()); // full -> 0
			assertEquals(5, geo.getLineThickness()); // 5.3 -> 5
			assertEquals(1, geo.getLineTypeHidden()); // hidden=dashed -> 1
			assertEquals(127, geo.getLineOpacity()); // 127 -> 127
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style2");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			
			// Check XML attributes
			assertEquals("0", attrs.get("type")); // full -> 0
			assertEquals("5", attrs.get("thickness")); // 5.3 -> 5
			assertEquals("1", attrs.get("typeHidden")); // hidden=dashed -> 1
			assertEquals("127", attrs.get("opacity")); // 127 -> 127
			assertEquals("true", attrs.get("drawArrow")); // arrow -> true
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with only type.
	 */
	@Test
	public void testParseLineStyleTypeOnly() {
		String gpad = "@style = { lineStyle: dotted }\n"
				+ "g @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			// When only type is specified, thickness should default to 5
			assertEquals(20, geo.getLineType()); // dotted -> 20
			assertEquals(5, geo.getLineThickness()); // default thickness
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			assertEquals("20", attrs.get("type")); // dotted -> 20
			assertEquals("5", attrs.get("thickness")); // default thickness
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with only thickness.
	 */
	@Test
	public void testParseLineStyleThicknessOnly() {
		String gpad = "@style = { lineStyle: thickness=10.5 }\n"
				+ "g @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			// When only thickness is specified, type should default to 0 (full)
			assertEquals(0, geo.getLineType()); // default type (full)
			assertEquals(10, geo.getLineThickness()); // 10.5 -> 10
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			assertEquals("0", attrs.get("type")); // default type (full)
			assertEquals("10", attrs.get("thickness")); // 10.5 -> 10
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with hidden=show.
	 */
	@Test
	public void testParseLineStyleHiddenShow() {
		String gpad = "@style = { lineStyle: hidden=show }\n"
				+ "g @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			// When only typeHidden is specified, type and thickness should default
			assertEquals(0, geo.getLineType()); // default type (full)
			assertEquals(5, geo.getLineThickness()); // default thickness
			assertEquals(2, geo.getLineTypeHidden()); // hidden=show -> 2
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			assertEquals("0", attrs.get("type")); // default type (full)
			assertEquals("5", attrs.get("thickness")); // default thickness
			assertEquals("2", attrs.get("typeHidden")); // hidden=show -> 2
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with all type values.
	 */
	@Test
	public void testParseLineStyleAllTypes() {
		String[] types = {"pointwise", "full", "dashedshort", "dashedlong", "dotted", "dasheddotted"};
		String[] expectedValues = {"-1", "0", "10", "15", "20", "30"};
		
		for (int i = 0; i < types.length; i++) {
			String gpad = "@style" + i + " = { lineStyle: " + types[i] + " }\n"
					+ "g @style" + i + " = Line((0,0), (1,1))";
			GpadParser parser = new GpadParser(getKernel());
			
			try {
				List<GeoElement> geos = parser.parse(gpad);
				assertEquals(1, geos.size());
				GeoElement geo = geos.get(0);
				assertTrue(geo instanceof GeoLine);
				
				// Verify geometric object properties
				int expectedType = Integer.parseInt(expectedValues[i]);
				assertEquals(expectedType, geo.getLineType());
				assertEquals(5, geo.getLineThickness()); // default thickness
				
				GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style" + i);
				assertNotNull(styleSheet);
				LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
				assertNotNull(attrs);
				assertEquals(expectedValues[i], attrs.get("type"));
				assertEquals("5", attrs.get("thickness")); // default thickness
			} catch (GpadParseException e) {
				throw new AssertionError("Parse failed for type " + types[i] + ": " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Test parsing lineStyle with inline style.
	 */
	@Test
	public void testParseLineStyleInline() {
		String gpad = "g { lineStyle: dashedshort thickness=3 opacity=100 arrow } = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			assertEquals(10, geo.getLineType()); // dashedshort -> 10
			assertEquals(3, geo.getLineThickness()); // thickness=3
			assertEquals(100, geo.getLineOpacity()); // opacity=100
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test converting XML to Gpad format - example 1.
	 */
	@Test
	public void testConvertLineStyleXMLToGpadExample1() {
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("type", "15"); // dashedlong
		attrs.put("thickness", "5");
		attrs.put("typeHidden", "0");
		attrs.put("opacity", "128");
		attrs.put("drawArrow", "false");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("lineStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("lineStyle:"));
		
		// Check that all parts are present (order may vary)
		assertTrue(gpad.contains("dashedlong") || gpad.contains("type=\"15\""));
		assertTrue(gpad.contains("thickness=5"));
		assertTrue(gpad.contains("hidden"));
		assertTrue(gpad.contains("opacity=128"));
		assertTrue(!gpad.contains("arrow"));
	}

	/**
	 * Test converting XML to Gpad format - example 2.
	 */
	@Test
	public void testConvertLineStyleXMLToGpadExample2() {
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("type", "0"); // full
		attrs.put("thickness", "5");
		attrs.put("typeHidden", "1"); // dashed
		attrs.put("opacity", "127");
		attrs.put("drawArrow", "true");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("lineStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("lineStyle:"));
		
		// Check that all parts are present
		assertTrue(gpad.contains("full"));
		assertTrue(gpad.contains("thickness=5"));
		assertTrue(gpad.contains("hidden=dashed"));
		assertTrue(gpad.contains("opacity=127"));
		assertTrue(gpad.contains("arrow"));
	}

	/**
	 * Test converting XML to Gpad format with all type values.
	 */
	@Test
	public void testConvertLineStyleAllTypes() {
		
		String[] xmlValues = {"-1", "0", "10", "15", "20", "30"};
		String[] gpadKeys = {"pointwise", "full", "dashedshort", "dashedlong", "dotted", "dasheddotted"};
		
		for (int i = 0; i < xmlValues.length; i++) {
			LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
			attrs.put("type", xmlValues[i]);
			
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			styleMap.put("lineStyle", attrs);
			
			String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
			assertTrue("Type " + xmlValues[i] + " should convert to " + gpadKeys[i],
					gpad.contains(gpadKeys[i]));
		}
	}

	/**
	 * Test converting XML to Gpad format with typeHidden values.
	 */
	@Test
	public void testConvertLineStyleTypeHidden() {
		
		// Test typeHidden="0" -> hidden
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("typeHidden", "0");
		Map<String, LinkedHashMap<String, String>> styleMap1 = new LinkedHashMap<>();
		styleMap1.put("lineStyle", attrs1);
		String gpad1 = StyleMapToGpadConverter.convert("test", styleMap1, null);
		assertTrue(gpad1.contains("hidden"));
		assertTrue(!gpad1.contains("hidden=dashed") && !gpad1.contains("hidden=show"));
		
		// Test typeHidden="1" -> hidden=dashed
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("typeHidden", "1");
		Map<String, LinkedHashMap<String, String>> styleMap2 = new LinkedHashMap<>();
		styleMap2.put("lineStyle", attrs2);
		String gpad2 = StyleMapToGpadConverter.convert("test", styleMap2, null);
		assertTrue(gpad2.contains("hidden=dashed"));
		
		// Test typeHidden="2" -> hidden=show
		LinkedHashMap<String, String> attrs3 = new LinkedHashMap<>();
		attrs3.put("typeHidden", "2");
		Map<String, LinkedHashMap<String, String>> styleMap3 = new LinkedHashMap<>();
		styleMap3.put("lineStyle", attrs3);
		String gpad3 = StyleMapToGpadConverter.convert("test", styleMap3, null);
		assertTrue(gpad3.contains("hidden=show"));
	}

	/**
	 * Test converting XML to Gpad format with drawArrow values.
	 */
	@Test
	public void testConvertLineStyleDrawArrow() {
		
		// Test drawArrow="true" -> arrow
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("drawArrow", "true");
		Map<String, LinkedHashMap<String, String>> styleMap1 = new LinkedHashMap<>();
		styleMap1.put("lineStyle", attrs1);
		String gpad1 = StyleMapToGpadConverter.convert("test", styleMap1, null);
		assertTrue(gpad1.contains("arrow"));
		assertTrue(!gpad1.contains("~arrow"));
		
		// Test drawArrow="false" -> 不出�?
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("drawArrow", "false");
		Map<String, LinkedHashMap<String, String>> styleMap2 = new LinkedHashMap<>();
		styleMap2.put("lineStyle", attrs2);
		String gpad2 = StyleMapToGpadConverter.convert("test", styleMap2, null);
		assertTrue(!gpad2.contains("arrow"));
	}

	/**
	 * Test round-trip conversion: Gpad -> XML -> Gpad.
	 */
	@Test
	public void testLineStyleRoundTrip() {
		// Original Gpad
		String originalGpad = "@style = { lineStyle: dashedlong thickness=5.3 hidden opacity=128.6 ~arrow }\n"
				+ "g @style = Line((0,0), (1,1))";
		
		GpadParser parser = new GpadParser(getKernel());
		try {
			// Parse Gpad to get style sheet
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			assertEquals(15, geo.getLineType()); // dashedlong -> 15
			assertEquals(5, geo.getLineThickness()); // 5.3 -> 5
			assertEquals(0, geo.getLineTypeHidden()); // hidden -> 0
			assertEquals(128, geo.getLineOpacity()); // 128.6 -> 128
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			// Get XML attributes
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			
			// Convert back to Gpad
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			styleMap.put("lineStyle", attrs);
			String convertedGpad = StyleMapToGpadConverter.convert("style", styleMap, null);
			
			// Verify conversion contains expected values
			assertTrue(convertedGpad.contains("dashedlong"));
			assertTrue(convertedGpad.contains("thickness=5"));
			assertTrue(convertedGpad.contains("hidden"));
			assertTrue(convertedGpad.contains("opacity=128"));
			assertTrue(!convertedGpad.contains("arrow"));
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with empty value (should be ignored).
	 */
	@Test
	public void testParseLineStyleEmpty() {
		String gpad = "@style = { lineStyle: }\n"
				+ "g @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			// Empty lineStyle should not create a property or create empty attributes
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			// Either null or empty is acceptable
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing lineStyle with partial attributes.
	 */
	@Test
	public void testParseLineStylePartial() {
		String gpad = "@style = { lineStyle: thickness=3 arrow }\n"
				+ "g @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			
			// Verify geometric object properties
			// When only thickness and drawArrow are specified, type should default to 0 (full)
			assertEquals(0, geo.getLineType()); // default type (full)
			assertEquals(3, geo.getLineThickness()); // thickness=3
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("lineStyle");
			assertNotNull(attrs);
			assertEquals("0", attrs.get("type")); // default type (full)
			assertEquals("3", attrs.get("thickness"));
			assertEquals("true", attrs.get("drawArrow"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}

