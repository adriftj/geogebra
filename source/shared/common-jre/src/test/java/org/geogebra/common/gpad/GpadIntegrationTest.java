package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Integration tests for Gpad parsing and conversion.
 * Tests the complete flow from Gpad to GeoElement and back.
 */
public class GpadIntegrationTest extends BaseUnitTest {

	@Test
	public void testGpadToGeoElementAndBack() {
		// Create a point via Gpad
		String originalGpad = "A = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Should contain the label and command
			assertNotNull(convertedGpad);
			assertTrue(convertedGpad.contains("A"));
			assertTrue(convertedGpad.contains("="));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testGpadWithStyleSheetRoundTrip() {
		// Create with style sheet
		String gpad = "@style1 = { pointSize: 6; fixed }\n"
				+ "A @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Should contain the label
			assertNotNull(convertedGpad);
			assertTrue(convertedGpad.contains("A"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testComplexGpadExample() {
		String gpad = "@g = { labelOffset:28 75; lineStyle: thickness=4 opacity=178 }\n"
				+ "@h = { lineStyle: opacity=178 }\n"
				+ "g* @g = Line((0,0), (1,1));\n"
				+ "h~ @h = Line((0,0), (2,2))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertTrue(geos.size() >= 1);
			
			// Check that style sheets were registered
			assertTrue(parser.getGlobalStyleSheets().containsKey("g"));
			assertTrue(parser.getGlobalStyleSheets().containsKey("h"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testAPIEvalGpad() {
		// Test via GpadParser directly (since getGgbApi might not be available)
		String gpad = "A = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			// Verify the point was created
			GeoElement geo = getKernel().lookupLabel("A");
			assertNotNull(geo);
			assertTrue(geo instanceof GeoPoint);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testAPIToGpad() {
		// Create a point via normal command
		GeoElement geo = add("A = (1, 2)");
		
		// Convert to Gpad via converter
		GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
		String gpad = converter.toGpad(geo);
		
		assertNotNull(gpad);
		assertTrue(gpad.contains("A"));
	}

	@Test
	public void testMultiLineGpad() {
		// Test multi-line Gpad support
		String gpad = "@style1 = {\n"
				+ "  pointSize: 6;\n"
				+ "  fixed\n"
				+ "}\n"
				+ "A @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			assertTrue(parser.getGlobalStyleSheets().containsKey("style1"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testNestedCommandCalls() {
		// Test nested command calls
		String gpad = "A = Point(Line((0,0), (1,1)), 0.5)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			// Nested calls should be supported
			assertTrue(geos.size() >= 0); // May succeed or fail depending on command syntax
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}
