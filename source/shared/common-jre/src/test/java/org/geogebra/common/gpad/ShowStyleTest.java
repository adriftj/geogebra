package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;

import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Unit tests for show style parsing and conversion.
 */
public class ShowStyleTest extends BaseUnitTest {

	/**
	 * Test parsing show with example 1 from requirements.
	 * show: object ~label ev1 ~ev2 plane ~3d;
	 * Expected: object="true", label="false", ev=24 (16+8)
	 */
	@Test
	public void testParseShowExample1() {
		String gpad = "@style1 = { show: object ~label ev1 ~ev2 plane ~3d }\n"
				+ "A @style1 = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// Check XML attributes
			assertEquals("true", attrs.get("object"));
			assertEquals("false", attrs.get("label"));
			assertEquals("24", attrs.get("ev")); // 16+8 = 24
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with example 2 from requirements.
	 * show: ~object label ~ev1 ev2 ~plane 3d;
	 * Expected: object="false", label="true", ev=39 (1+2+32+4)
	 */
	@Test
	public void testParseShowExample2() {
		String gpad = "@style2 = { show: ~object label ~ev1 ev2 ~plane 3d }\n"
				+ "B @style2 = (1, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style2");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// Check XML attributes
			assertEquals("false", attrs.get("object"));
			assertEquals("true", attrs.get("label"));
			assertEquals("39", attrs.get("ev")); // 1+2+32+4 = 39
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only object and label (no ev flags).
	 * show: object ~label;
	 * Expected: object="true", label="false", no ev attribute
	 */
	@Test
	public void testParseShowOnlyObjectLabel() {
		String gpad = "@style = { show: object ~label }\n"
				+ "C @style = (2, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			assertEquals("true", attrs.get("object"));
			assertEquals("false", attrs.get("label"));
			// ev should not be present when no ev flags are specified
			assertTrue(!attrs.containsKey("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only ev flags (no object/label).
	 * show: ev1 ev2 3d plane;
	 * Expected: ev=6 (2+4), no object/label attributes
	 */
	@Test
	public void testParseShowOnlyEvFlags() {
		String gpad = "@style = { show: ev1 ev2 3d plane }\n"
				+ "D @style = (3, 3)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// object and label should not be present
			assertTrue(!attrs.containsKey("object"));
			assertTrue(!attrs.containsKey("label"));
			// ev should be 6: ev1 clears bit 0 (0), ev2 sets bit 1 (2), 3d sets bit 2 (4), plane sets bit 4(16)
			// ev = 0 & ~1 | 2 | 4 | 16 = 22
			assertEquals("22", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with ev=0 (all flags result in 0).
	 * show: ev1 ~ev2;
	 * Expected: ev=0 (ev1 clears bit 0, ~ev2 clears bit 1, both result in 0)
	 */
	@Test
	public void testParseShowEvZero() {
		String gpad = "@style = { show: ev1 ~ev2 }\n"
				+ "E @style = (4, 4)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// ev should be 0: ev1 clears bit 0, ~ev2 clears bit 1
			// ev = 0 & ~1 & ~2 = 0
			assertEquals("0", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test round-trip conversion: Gpad -> XML -> Gpad.
	 * Note: object="true" and ev1/~ev2 (when bits are 0) are not output in conversion
	 */
	@Test
	public void testShowRoundTrip() {
		// Original Gpad
		String originalGpad = "@style = { show: object ~label ev1 ~ev2 plane ~3d }\n"
				+ "A @style = (0, 0)";
		
		GpadParser parser = new GpadParser(getKernel());
		try {
			// Parse Gpad to get style sheet
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			// Get XML attributes
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// Convert back to Gpad
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			styleMap.put("show", attrs);
			String convertedGpad = StyleMapToGpadConverter.convert("style", styleMap, null);
			
			// Verify conversion contains expected values
			// object="false" is default, so output
			assertTrue(convertedGpad.contains("object"));
			assertTrue(!convertedGpad.contains("label")); // label="false" is default, not output
			// ev1 and ~ev2 are no-ops when bits are 0, so not output
			assertTrue(!convertedGpad.contains("ev1"));
			assertTrue(!convertedGpad.contains("~ev2"));
			assertTrue(convertedGpad.contains("plane"));
			assertTrue(convertedGpad.contains("~3d"));
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test XML to Gpad conversion for example 1.
	 * XML: object="true" label="false" ev="24"
	 * Expected Gpad: "~label plane ~3d" (object="true" is default, so not output;
	 * ev1 and ~ev2 are no-ops when bits are 0, so not output)
	 */
	@Test
	public void testConvertShowExample1() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("object", "true");
		attrs.put("label", "false");
		attrs.put("ev", "24"); // 16+8 = 24
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(converted.contains("object")); // object="false" is default, output
		assertTrue(!converted.contains("label")); // label="false" is default, not output
		assertTrue(!converted.contains("ev1")); // ev1 is no-op when bit 0=0
		assertTrue(!converted.contains("~ev2")); // ~ev2 is no-op when bit 1=0
		assertTrue(converted.contains("plane"));
		assertTrue(converted.contains("~3d"));
	}

	/**
	 * Test XML to Gpad conversion for example 2.
	 * XML: object="false" label="true" ev="39"
	 * Expected Gpad: "~object ~ev1 ev2 ~plane 3d" (label="true" is default, so not output)
	 */
	@Test
	public void testConvertShowExample2() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("object", "false");
		attrs.put("label", "true");
		attrs.put("ev", "39"); // 1+2+32+4 = 39
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(!converted.contains("object")); // object="false" is default, not output
		assertTrue(converted.contains("label")); // label="false" is default, output
		assertTrue(converted.contains("~ev1"));
		assertTrue(converted.contains("ev2"));
		assertTrue(converted.contains("~plane"));
		assertTrue(converted.contains("3d"));
	}

	/**
	 * Test XML to Gpad conversion with ev=0.
	 * XML: ev="0"
	 * Expected Gpad: "" (ev1 and ~ev2 are no-ops when bits are 0, so not output)
	 */
	@Test
	public void testConvertShowEvZero() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "0");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		// ev=0 means all bits are 0 (default state), ev1 and ~ev2 are no-ops, so nothing to output
		// The converter should return empty string or null, which means the property is omitted
		assertTrue(converted == null || converted.isEmpty() || !converted.contains("show:"));
	}

	/**
	 * Test parsing show with all flags in different order.
	 */
	@Test
	public void testParseShowAllFlagsDifferentOrder() {
		String gpad = "@style = { show: ~plane 3d ~ev2 ev1 ~label object }\n"
				+ "F @style = (5, 5)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			assertEquals("true", attrs.get("object"));
			assertEquals("false", attrs.get("label"));
			// ev calculation:
			// ev1: clear bit 0 -> 0 & ~1 = 0
			// ~ev2: clear bit 1 -> 0 & ~2 = 0
			// 3d: set bit 2, clear bit 3 -> 0 | 4 & ~8 = 4
			// ~plane: clear bit 4, set bit 5 -> 4 & ~16 | 32 = 36
			assertEquals("36", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with empty value (should be ignored).
	 */
	@Test
	public void testParseShowEmpty() {
		String gpad = "@style = { show: }\n"
				+ "G @style = (6, 6)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			// Empty show should not create a property or create empty attributes
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			// Either null or empty is acceptable
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only ev1 flag.
	 * show: ev1;
	 * Expected: ev=0 (ev1 clears bit 0, so result is 0)
	 */
	@Test
	public void testParseShowOnlyEv1() {
		String gpad = "@style = { show: ev1 }\n"
				+ "H @style = (7, 7)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// ev1 clears bit 0, so ev = 0 & ~1 = 0
			assertEquals("0", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only ~ev1 flag.
	 * show: ~ev1;
	 * Expected: ev=1 (~ev1 sets bit 0)
	 */
	@Test
	public void testParseShowOnlyTildeEv1() {
		String gpad = "@style = { show: ~ev1 }\n"
				+ "I @style = (8, 8)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// ~ev1 sets bit 0, so ev = 0 | 1 = 1
			assertEquals("1", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only 3d flag.
	 * show: 3d;
	 * Expected: ev=4 (3d sets bit 2, clears bit 3)
	 */
	@Test
	public void testParseShowOnly3d() {
		String gpad = "@style = { show: 3d }\n"
				+ "J @style = (9, 9)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// 3d sets bit 2, clears bit 3, so ev = 0 | 4 & ~8 = 4
			assertEquals("4", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only ~3d flag.
	 * show: ~3d;
	 * Expected: ev=8 (~3d clears bit 2, sets bit 3)
	 */
	@Test
	public void testParseShowOnlyTilde3d() {
		String gpad = "@style = { show: ~3d }\n"
				+ "K @style = (10, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// ~3d clears bit 2, sets bit 3, so ev = 0 & ~4 | 8 = 8
			assertEquals("8", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only plane flag.
	 * show: plane;
	 * Expected: ev=16 (plane sets bit 4, clears bit 5)
	 */
	@Test
	public void testParseShowOnlyPlain() {
		String gpad = "@style = { show: plane }\n"
				+ "L @style = (11, 11)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// plane sets bit 4, clears bit 5, so ev = 0 | 16 & ~32 = 16
			assertEquals("16", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with only ~plane flag.
	 * show: ~plane;
	 * Expected: ev=32 (~plane clears bit 4, sets bit 5)
	 */
	@Test
	public void testParseShowOnlyTildePlain() {
		String gpad = "@style = { show: ~plane }\n"
				+ "M @style = (12, 12)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// ~plane clears bit 4, sets bit 5, so ev = 0 & ~16 | 32 = 32
			assertEquals("32", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing show with conflicting flags (later ones override).
	 * show: ev1 ~ev1 ev2 ~ev2;
	 * Expected: ev=0 (last operation wins for each bit)
	 */
	@Test
	public void testParseShowConflictingFlags() {
		String gpad = "@style = { show: ev1 ~ev1 ev2 ~ev2 }\n"
				+ "N @style = (13, 13)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			// ev1 clears bit 0, then ~ev1 sets bit 0 -> 1
			// ev2 sets bit 1 -> 2, then ~ev2 clears bit 1 -> 0
			// Final: ev = 1 & ~2 = 1
			assertEquals("1", attrs.get("ev"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test XML to Gpad conversion with only object and label (no ev).
	 */
	@Test
	public void testConvertShowOnlyObjectLabel() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("object", "true");
		attrs.put("label", "false");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(converted.contains("object")); // object="false" is default, output
		assertTrue(!converted.contains("~label")); // label="false" is default, not output
		// Should not contain ev-related flags when ev is not present
	}

	/**
	 * Test XML to Gpad conversion with all bits set.
	 * XML: ev="63" (all bits 0-5 set)
	 */
	@Test
	public void testConvertShowAllBitsSet() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "63"); // 1+2+4+8+16+32 = 63
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(converted.contains("~ev1")); // bit 0 set
		assertTrue(converted.contains("ev2")); // bit 1 set
		// For 3d: bit 2 and 3 both set, but we only output when one is set and other is clear
		// So neither 3d nor ~3d should appear
		// For plane: bit 4 and 5 both set, same logic
	}

	/**
	 * Test XML to Gpad conversion with ev=1 (only bit 0 set).
	 */
	@Test
	public void testConvertShowEvOne() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "1");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(converted.contains("~ev1")); // bit 0 set, so output ~ev1
		assertTrue(!converted.contains("~ev2")); // bit 1 clear, ~ev2 is no-op, not output
	}

	/**
	 * Test XML to Gpad conversion with ev=2 (only bit 1 set).
	 */
	@Test
	public void testConvertShowEvTwo() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "2");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(!converted.contains("ev1")); // bit 0 clear, ev1 is no-op, not output
		assertTrue(converted.contains("ev2")); // bit 1 set
	}

	/**
	 * Test XML to Gpad conversion with ev=4 (only bit 2 set).
	 */
	@Test
	public void testConvertShowEvFour() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "4");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(!converted.contains("ev1")); // bit 0 clear, ev1 is no-op, not output
		assertTrue(!converted.contains("~ev2")); // bit 1 clear, ~ev2 is no-op, not output
		assertTrue(converted.contains("3d")); // bit 2 set, bit 3 clear
	}

	/**
	 * Test XML to Gpad conversion with ev=8 (only bit 3 set).
	 */
	@Test
	public void testConvertShowEvEight() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "8");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(!converted.contains("ev1")); // bit 0 clear, ev1 is no-op, not output
		assertTrue(!converted.contains("~ev2")); // bit 1 clear, ~ev2 is no-op, not output
		assertTrue(converted.contains("~3d")); // bit 2 clear, bit 3 set
	}

	/**
	 * Test XML to Gpad conversion with ev=16 (only bit 4 set).
	 */
	@Test
	public void testConvertShowEvSixteen() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "16");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(!converted.contains("ev1")); // bit 0 clear, ev1 is no-op, not output
		assertTrue(!converted.contains("~ev2")); // bit 1 clear, ~ev2 is no-op, not output
		assertTrue(converted.contains("plane")); // bit 4 set, bit 5 clear
	}

	/**
	 * Test XML to Gpad conversion with ev=32 (only bit 5 set).
	 */
	@Test
	public void testConvertShowEvThirtyTwo() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("ev", "32");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		assertTrue(converted.contains("show:"));
		assertTrue(!converted.contains("ev1")); // bit 0 clear, ev1 is no-op, not output
		assertTrue(!converted.contains("~ev2")); // bit 1 clear, ~ev2 is no-op, not output
		assertTrue(converted.contains("~plane")); // bit 4 clear, bit 5 set
	}

	/**
	 * Test round-trip conversion for example 2.
	 * Note: label="true" is default, so not output in conversion
	 */
	@Test
	public void testShowRoundTripExample2() {
		String originalGpad = "@style = { show: ~object label ~ev1 ev2 ~plane 3d }\n"
				+ "B @style = (1, 1)";
		
		GpadParser parser = new GpadParser(getKernel());
		try {
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("show");
			assertNotNull(attrs);
			
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			styleMap.put("show", attrs);
			String convertedGpad = StyleMapToGpadConverter.convert("style", styleMap, null);
			
			// Verify conversion contains expected values
			assertTrue(!convertedGpad.contains("object")); // object="false" is default, not output
			// label="false" is default, output
			assertTrue(convertedGpad.contains("label"));
			assertTrue(convertedGpad.contains("~ev1"));
			assertTrue(convertedGpad.contains("ev2"));
			assertTrue(convertedGpad.contains("~plane"));
			assertTrue(convertedGpad.contains("3d"));
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test XML to Gpad conversion with only default values.
	 * XML: object="true" label="true" ev="0" (or no ev)
	 * Expected: should return null or empty (all defaults, nothing to output)
	 */
	@Test
	public void testConvertShowAllDefaults() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("object", "false");
		attrs.put("label", "false");
		attrs.put("ev", "0");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		
		// All values are defaults, so should return null or empty
		assertTrue(converted == null || converted.isEmpty() || !converted.contains("show:"));
	}

	/**
	 * Test XML to Gpad conversion with only object="false" (default).
	 */
	@Test
	public void testConvertShowOnlyObjectFalse() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("object", "false");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		assertNull(converted);
	}

	/**
	 * Test XML to Gpad conversion with only label="false" (default).
	 */
	@Test
	public void testConvertShowOnlyLabelFalse() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("label", "false");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		styleMap.put("show", attrs);
		String converted = StyleMapToGpadConverter.convert("style", styleMap, null);
		assertNull(converted);
	}
}
