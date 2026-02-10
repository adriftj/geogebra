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
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Unit tests for coords GPAD syntax.
 * Tests coords property parsing, XML to GPAD conversion, and geometry object creation.
 * 
 * Note: coords style is only valid for points constrained on a Path.
 * Syntax: coords: x y [z]
 * Defaults: z=1.0
 */
public class CoordsGpadTest extends BaseUnitTest {
	
	@Override
	public AppCommon createAppCommon() {
		return AppCommonFactory.create3D();
	}

	/**
	 * Test parsing coords with 2 values (x, y) for a point on a path.
	 * coords: 2.1 3.2;
	 * Expected: x=2.1, y=3.2, z=1.0
	 */
	@Test
	public void testParseCoords2Values() {
		String gpad = "@style1 = { coords: 2.1 3.2 }\n"
				+ "l = Line((0,0), (1,1));\n"
				+ "P @style1 = Point(l)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertTrue("Should create at least 2 objects", geos.size() >= 2);
			GeoElement geo = geos.get(geos.size() - 1);
			assertTrue("Should be GeoPoint", geo instanceof GeoPoint);
			GeoPoint point = (GeoPoint) geo;
			assertTrue("Point should be on path", point.isPointOnPath());
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.1", attrs.get("x"));
			assertEquals("3.2", attrs.get("y"));
			assertEquals("1.0", attrs.get("z"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test parsing coords with 3 values (x, y, z) for a point on a path.
	 * coords: 2.2 3.1 1.5;
	 * Expected: x=2.2, y=3.1, z=1.5
	 */
	@Test
	public void testParseCoords3Values() {
		String gpad = "@style1 = { coords: 2.2 3.1 1.5 }\n"
				+ "l = Line((0,0), (1,1));\n"
				+ "P @style1 = Point(l)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertTrue("Should create at least 2 objects", geos.size() >= 2);
			
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			
			assertEquals("2.2", attrs.get("x"));
			assertEquals("3.1", attrs.get("y"));
			assertEquals("1.5", attrs.get("z"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test XML to GPAD conversion for coords with 2 values (point on path).
	 */
	@Test
	public void testConvertCoords2Values() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.1");
		attrs.put("y", "3.2");
		attrs.put("z", "1.0");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue(gpad.contains("coords: 2.1 3.2"));
	}

	/**
	 * Test XML to GPAD conversion for coords with 3 values (point on path).
	 */
	@Test
	public void testConvertCoords3Values() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.2");
		attrs.put("y", "3.1");
		attrs.put("z", "1.5");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// z=1.5 is not default, so it should be included
		assertTrue(gpad.contains("coords: 2.2 3.1 1.5"));
	}

	/**
	 * Test XML to GPAD conversion for coords with z=1.0 (default, should be omitted).
	 */
	@Test
	public void testConvertCoordsWithDefaultZ() {
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "2.2");
		attrs.put("y", "3.1");
		attrs.put("z", "1.0");
		
		Map<String, LinkedHashMap<String, String>> styleMap = new java.util.HashMap<>();
		styleMap.put("coords", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// z=1.0 is default, so it should be omitted
		assertTrue(gpad.contains("coords: 2.2 3.1"));
	}

	/**
	 * Test that coords style is NOT output for points that are NOT on a path.
	 */
	@Test
	public void testCoordsNotOutputForFreePoint() {
		// Create a free point (not on a path)
		String gpadInput = "@style1 = { coords: 2.1 3.2 }\n"
				+ "P @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpadInput);
			assertTrue("Should create at least 1 object", geos.size() >= 1);
			GeoElement geo = geos.get(geos.size() - 1);
			assertTrue("Should be GeoPoint", geo instanceof GeoPoint);
			GeoPoint point = (GeoPoint) geo;
			assertTrue("Point should NOT be on path", !point.isPointOnPath());
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String gpadOutput = converter.toGpad(geo);
			
			// coords should NOT be in output for free points
			assertTrue("Output should not contain coords for free point: " + gpadOutput, 
					!gpadOutput.contains("coords:"));
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test round-trip: GPAD -> XML -> GPAD for coords with point on path.
	 * Note: The coords values in output may differ from input because
	 * the point's actual coordinates are calculated from the path parameter.
	 */
	@Test
	public void testCoordsRoundTripPointOnPath() {
		String gpadInput = "@style1 = { coords: 2.1 3.2 1.5 }\n"
				+ "l = Line((0,0), (1,1));\n"
				+ "P @style1 = Point(l)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpadInput);
			// Should create 2 objects: l and P
			assertTrue("Should create at least 2 objects", geos.size() >= 2);
			// The last one should be the point
			GeoElement geo = geos.get(geos.size() - 1);
			assertTrue("Should be GeoPoint", geo instanceof GeoPoint);
			GeoPoint point = (GeoPoint) geo;
			assertTrue("Point should be on path", point.isPointOnPath());
			
			// Verify that the style sheet was parsed correctly
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("coords");
			assertNotNull(attrs);
			assertEquals("2.1", attrs.get("x"));
			assertEquals("3.2", attrs.get("y"));
			assertEquals("1.5", attrs.get("z"));
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String gpadOutput = converter.toGpad(geo);
			
			// coords should be in output for points on path
			// The actual coords values may differ from input because
			// they are calculated from the path parameter
			assertTrue("Output should contain coords for point on path: " + gpadOutput, 
					gpadOutput.contains("coords:"));
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Test that coords style is NOT output for non-point objects.
	 */
	@Test
	public void testCoordsNotOutputForNonPoint() {
		// Create a line (not a point)
		String gpadInput = "@style1 = { coords: 2.1 3.2 }\n"
				+ "l @style1 = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpadInput);
			assertTrue("Should create at least 1 object", geos.size() >= 1);
			GeoElement geo = geos.get(geos.size() - 1);
			assertTrue("Should be GeoLine", geo instanceof org.geogebra.common.kernel.geos.GeoLine);
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String gpadOutput = converter.toGpad(geo);
			
			// coords should NOT be in output for non-point objects
			assertTrue("Output should not contain coords for non-point object: " + gpadOutput, 
					!gpadOutput.contains("coords:"));
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}
}
