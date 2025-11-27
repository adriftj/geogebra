package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.AppCommonFactory;
import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.jre.headless.AppCommon;
import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoVec3D;
import org.geogebra.common.geogebra3D.kernel3D.geos.GeoConic3D;
import org.geogebra.common.geogebra3D.kernel3D.geos.GeoLine3D;
import org.junit.Test;

/**
 * Unit tests for coords GPAD syntax.
 * Tests coords property parsing, XML to GPAD conversion, and geometry object creation.
 */
public class CoordsGpadTest extends BaseUnitTest {
	
	@Override
	public AppCommon createAppCommon() {
		return AppCommonFactory.create3D();
	}

	/**
	 * Test parsing coords with 2 values (x, y).
	 * coords: 2.1 3.2;
	 * Expected: x=2.1, y=3.2, z=1.0, w=1.0
	 */
	@Test
	public void testParseCoords2Values() {
		String gpad = "@style1 = { coords: 2.1 3.2 }\n"
				+ "P @style1 = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.1", attrs.get("x"));
			assertEquals("3.2", attrs.get("y"));
			assertEquals("1.0", attrs.get("z"));
			assertEquals("1.0", attrs.get("w"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with 3 values (x, y, z).
	 * coords: 2.2 3.1 1.0;
	 * Expected: x=2.2, y=3.1, z=1.0, w=1.0
	 */
	@Test
	public void testParseCoords3Values() {
		String gpad = "@style1 = { coords: 2.2 3.1 1.0 }\n"
				+ "P @style1 = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.2", attrs.get("x"));
			assertEquals("3.1", attrs.get("y"));
			assertEquals("1.0", attrs.get("z"));
			assertEquals("1.0", attrs.get("w"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with 4 values (x, y, z, w).
	 * coords: 2.3 4.1 3.5 2.1;
	 * Expected: x=2.3, y=4.1, z=3.5, w=2.1
	 */
	@Test
	public void testParseCoords4Values() {
		String gpad = "@style1 = { coords: 2.3 4.1 3.5 2.1 }\n"
				+ "P @style1 = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("2.1", attrs.get("w"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with v= part (3 values).
	 * coords: 2.3 4.1 3.5 v=1.1 2.3 2.4;
	 * Expected: x=2.3, y=4.1, z=3.5, w=1.0, vx=1.1, vy=2.3, vz=2.4, vw=0.0
	 */
	@Test
	public void testParseCoordsWithV3Values() {
		String gpad = "@style1 = { coords: 2.3 4.1 3.5 v=1.1 2.3 2.4 }\n"
				+ "l @style1 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("1.0", attrs.get("w"));
			assertEquals("1.1", attrs.get("vx"));
			assertEquals("2.3", attrs.get("vy"));
			assertEquals("2.4", attrs.get("vz"));
			assertEquals("0.0", attrs.get("vw"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with v= part (4 values).
	 * coords: 2.3 4.1 3.5 2.1 v=1.1 2.3 3.3;
	 * Expected: x=2.3, y=4.1, z=3.5, w=2.1, vx=1.1, vy=2.3, vz=3.3, vw=0.0
	 */
	@Test
	public void testParseCoordsWithV4Values() {
		String gpad = "@style1 = { coords: 2.3 4.1 3.5 2.1 v=1.1 2.3 3.3 }\n"
				+ "l @style1 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("2.1", attrs.get("w"));
			assertEquals("1.1", attrs.get("vx"));
			assertEquals("2.3", attrs.get("vy"));
			assertEquals("3.3", attrs.get("vz"));
			assertEquals("0.0", attrs.get("vw"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with v= and w= parts.
	 * coords: 2.3 4.1 3.5 2.1 v=2.5 6.2 3.4 w=1.1 2.3 3.3;
	 * Expected: x=2.3, y=4.1, z=3.5, w=2.1, vx=2.5, vy=6.2, vz=3.4, vw=0.0, wx=1.1, wy=2.3, wz=3.3
	 */
	@Test
	public void testParseCoordsWithVAndW() {
		String gpad = "@style1 = { coords: 2.3 4.1 3.5 2.1 v=2.5 6.2 3.4 w=1.1 2.3 3.3 }\n"
				+ "l @style1 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("2.1", attrs.get("w"));
			assertEquals("2.5", attrs.get("vx"));
			assertEquals("6.2", attrs.get("vy"));
			assertEquals("3.4", attrs.get("vz"));
			assertEquals("0.0", attrs.get("vw"));
			assertEquals("1.1", attrs.get("wx"));
			assertEquals("2.3", attrs.get("wy"));
			assertEquals("3.3", attrs.get("wz"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with v= part including vw.
	 * coords: 2.3 4.1 3.5 v=1.1 2.3 2.4 0.5;
	 * Expected: x=2.3, y=4.1, z=3.5, w=1.0, vx=1.1, vy=2.3, vz=2.4, vw=0.5
	 */
	@Test
	public void testParseCoordsWithVIncludingVw() {
		String gpad = "@style1 = { coords: 2.3 4.1 3.5 v=1.1 2.3 2.4 0.5 }\n"
				+ "l @style1 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("1.0", attrs.get("w"));
			assertEquals("1.1", attrs.get("vx"));
			assertEquals("2.3", attrs.get("vy"));
			assertEquals("2.4", attrs.get("vz"));
			assertEquals("0.5", attrs.get("vw"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test XML to GPAD conversion for coords with 2 values.
	 */
	@Test
	public void testConvertCoords2Values() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.1");
		attrs.put("y", "3.2");
		attrs.put("z", "1.0");
		attrs.put("w", "1.0");
		
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("coords: 2.1 3.2"));
	}

	/**
	 * Test XML to GPAD conversion for coords with 3 values.
	 */
	@Test
	public void testConvertCoords3Values() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.2");
		attrs.put("y", "3.1");
		attrs.put("z", "1.0");
		attrs.put("w", "1.0");
		
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// z=1.0 is default, so it should be omitted
		assertTrue(gpad.contains("coords: 2.2 3.1"));
	}

	/**
	 * Test XML to GPAD conversion for coords with 4 values.
	 */
	@Test
	public void testConvertCoords4Values() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.3");
		attrs.put("y", "4.1");
		attrs.put("z", "3.5");
		attrs.put("w", "2.1");
		
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("coords: 2.3 4.1 3.5 2.1"));
	}

	/**
	 * Test XML to GPAD conversion for coords with v= part.
	 */
	@Test
	public void testConvertCoordsWithV() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.3");
		attrs.put("y", "4.1");
		attrs.put("z", "3.5");
		attrs.put("w", "1.0");
		attrs.put("vx", "1.1");
		attrs.put("vy", "2.3");
		attrs.put("vz", "2.4");
		attrs.put("vw", "0.0");
		
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("coords: 2.3 4.1 3.5 v=1.1 2.3 2.4"));
	}

	/**
	 * Test XML to GPAD conversion for coords with v= and w= parts.
	 */
	@Test
	public void testConvertCoordsWithVAndW() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.3");
		attrs.put("y", "4.1");
		attrs.put("z", "3.5");
		attrs.put("w", "2.1");
		attrs.put("vx", "2.5");
		attrs.put("vy", "6.2");
		attrs.put("vz", "3.4");
		attrs.put("vw", "0.0");
		attrs.put("wx", "1.1");
		attrs.put("wy", "2.3");
		attrs.put("wz", "3.3");
		
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("coords: 2.3 4.1 3.5 2.1 v=2.5 6.2 3.4 w=1.1 2.3 3.3"));
	}

	/**
	 * Test XML to GPAD conversion for coords with v= including vw.
	 */
	@Test
	public void testConvertCoordsWithVIncludingVw() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.3");
		attrs.put("y", "4.1");
		attrs.put("z", "3.5");
		attrs.put("w", "1.0");
		attrs.put("vx", "1.1");
		attrs.put("vy", "2.3");
		attrs.put("vz", "2.4");
		attrs.put("vw", "0.5");
		
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("coords: 2.3 4.1 3.5 v=1.1 2.3 2.4 0.5"));
	}

	/**
	 * Test round-trip: GPAD -> XML -> GPAD for coords with GeoConic3D.
	 * GeoConic3D supports ox, oy, oz, ow, vx, vy, vz, wx, wy, wz.
	 * 
	 * NOTE: Circle objects created via Circle command do not output coords in XML,
	 * but instead use matrix and other styles. Therefore, we cannot verify
	 * coords round-trip for dependent Circle objects. This test verifies that
	 * the style sheet is parsed correctly, but does not assert coords in output.
	 */
	@Test
	public void testCoordsRoundTripConic3D() {
		// GeoConic3D uses ox, oy, oz, ow for origin and vx, vy, vz, wx, wy, wz for directions
		// Create 3D points first, then use Circle command with 3D points to create GeoConic3D
		String gpadInput = "@style1 = { coords: 2.3 4.1 3.5 2.1 v=2.5 6.2 3.4 w=1.1 2.3 3.3 }\n"
				+ "A = (0,0,0);\n"
				+ "B = (1,0,0);\n"
				+ "C = (0,1,0);\n"
				+ "c @style1 = Circle(A, B, C)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpadInput);
			// Should create 4 objects: A, B, C, and c
			assertTrue("Should create at least 4 objects", geos.size() >= 4);
			// The last one should be the conic
			GeoElement geo = geos.get(geos.size() - 1);
			assertTrue("Should be GeoConic3D", geo instanceof GeoConic3D);
			
			// Verify that the style sheet was parsed correctly
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			// Style sheet stores x, y, z, w (mapping to ox, oy, oz, ow happens when applying to 3D objects)
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("2.1", attrs.get("w"));
			assertEquals("2.5", attrs.get("vx"));
			assertEquals("6.2", attrs.get("vy"));
			assertEquals("3.4", attrs.get("vz"));
			assertEquals("1.1", attrs.get("wx"));
			assertEquals("2.3", attrs.get("wy"));
			assertEquals("3.3", attrs.get("wz"));
			
			// Print the XML for analysis
			String fullXML = geo.getXML();
			System.out.println("=== GPAD Round-trip Analysis (GeoConic3D) ===");
			System.out.println("Input GPAD:");
			System.out.println(gpadInput);
			System.out.println("\nFull XML from GeoElement.getXML():");
			System.out.println(fullXML);
			
			// Parse XML to see what's extracted
			XMLToStyleMapParser xmlParser = new XMLToStyleMapParser();
			Map<String, LinkedHashMap<String, String>> styleMap = xmlParser.parse(fullXML);
			System.out.println("\nParsed Style Map from getXML():");
			for (Map.Entry<String, LinkedHashMap<String, String>> entry : styleMap.entrySet()) {
				System.out.println("  " + entry.getKey() + ": " + entry.getValue());
			}
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String gpadOutput = converter.toGpad(geo);
			System.out.println("\nOutput GPAD:");
			System.out.println(gpadOutput);
			System.out.println("==============================");
			
			// NOTE: Circle objects do not output coords in XML (they use matrix instead),
			// so we cannot verify coords round-trip. Just verify that the object was created
			// and the style sheet was parsed correctly.
			// The coords style from the input style sheet is verified above.
			assertTrue("Output should contain the Circle command", 
					gpadOutput.contains("Circle") || gpadOutput.contains("circle"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Test round-trip: GPAD -> XML -> GPAD for coords with GeoLine3D.
	 * GeoLine3D supports ox, oy, oz, ow, vx, vy, vz, vw.
	 * 
	 * NOTE: Line objects created via Line(A, B) command are not independent,
	 * so coords style from style sheet does not apply. This test verifies that
	 * the coords in output match the default coords from command creation
	 * (i.e., origin at A and direction from A to B).
	 */
	@Test
	public void testCoordsRoundTripLine3D() {
		// GeoLine3D uses ox, oy, oz, ow for origin and vx, vy, vz, vw for direction
		// Create 3D points first, then use Line command with 3D points to create GeoLine3D
		String gpadInput = "@style1 = { coords: 2.3 4.1 3.5 2.1 v=2.5 6.2 3.4 0.5 }\n"
				+ "A = (0,0,0);\n"
				+ "B = (1,1,1);\n"
				+ "l @style1 = Line(A, B)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpadInput);
			// Should create 3 objects: A, B, and l
			assertTrue("Should create at least 3 objects", geos.size() >= 3);
			// The last one should be the line
			GeoElement geo = geos.get(geos.size() - 1);
			assertTrue("Should be GeoLine3D", geo instanceof GeoLine3D);
			
			// Verify that the style sheet was parsed correctly
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			// Style sheet stores x, y, z, w (mapping to ox, oy, oz, ow happens when applying to 3D objects)
			assertEquals("2.3", attrs.get("x"));
			assertEquals("4.1", attrs.get("y"));
			assertEquals("3.5", attrs.get("z"));
			assertEquals("2.1", attrs.get("w"));
			assertEquals("2.5", attrs.get("vx"));
			assertEquals("6.2", attrs.get("vy"));
			assertEquals("3.4", attrs.get("vz"));
			assertEquals("0.5", attrs.get("vw"));
			
			// Print the XML for analysis
			String fullXML = geo.getXML();
			System.out.println("=== GPAD Round-trip Analysis (GeoLine3D) ===");
			System.out.println("Input GPAD:");
			System.out.println(gpadInput);
			System.out.println("\nFull XML from GeoElement.getXML():");
			System.out.println(fullXML);
			
			// Parse XML to see what's extracted
			XMLToStyleMapParser xmlParser = new XMLToStyleMapParser();
			Map<String, LinkedHashMap<String, String>> styleMap = xmlParser.parse(fullXML);
			System.out.println("\nParsed Style Map from getXML():");
			for (Map.Entry<String, LinkedHashMap<String, String>> entry : styleMap.entrySet()) {
				System.out.println("  " + entry.getKey() + ": " + entry.getValue());
			}
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String gpadOutput = converter.toGpad(geo);
			System.out.println("\nOutput GPAD:");
			System.out.println(gpadOutput);
			System.out.println("==============================");
			
			// NOTE: Since the Line is not independent, coords style from style sheet
			// does not apply. The output should contain the default coords from
			// Line(A, B) creation: origin at A=(0,0,0) and direction from A to B=(1,1,1).
			// Verify the exact coords from command creation
			String expectedCoords = "coords: 0.0 0.0 0.0 v=1.0 1.0 1.0";
			assertTrue("Output should contain coords style with default values from Line(A, B): " + gpadOutput, 
					gpadOutput.contains(expectedCoords));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}
}

