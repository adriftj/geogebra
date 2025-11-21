package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoFunction;
import org.geogebra.common.kernel.kernelND.GeoEvaluatable;
import org.junit.Test;

/**
 * Unit tests for tableview GPAD syntax.
 * Tests tableview property parsing, XML to GPAD conversion, and geometry object creation.
 */
public class TableViewGpadTest extends BaseUnitTest {

	// ==================== GPAD to Geometry Object Tests ====================

	@Test
	public void testCreateFunctionWithTableViewColumnAndPoints() {
		// tableview: 2 points; // column=2, points=true
		String gpad = "@style = { tableview: 2 points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify tableview properties
			assertEquals(2, evaluatable.getTableColumn());
			assertTrue(evaluatable.isPointsVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateFunctionWithTableViewColumnAndNoPoints() {
		// tableview: 1 ~points; // column=1, points=false
		String gpad = "@style = { tableview: 1 ~points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify tableview properties
			assertEquals(1, evaluatable.getTableColumn());
			assertTrue(!evaluatable.isPointsVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateFunctionWithTableViewPointsOnly() {
		// tableview: points; // no column, points=true
		String gpad = "@style = { tableview: points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertNotNull("geos should not be null", geos);
			assertTrue("geos should not be empty, size: " + geos.size(), geos.size() > 0);
			GeoElement geo = geos.get(0);
			assertNotNull("geo should not be null", geo);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify tableview properties
			// Column should remain at default (-1) when not specified
			assertEquals(-1, evaluatable.getTableColumn());
			assertTrue(evaluatable.isPointsVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateFunctionWithTableViewColumnOnly() {
		// tableview: 3; // column=3, no points (default)
		String gpad = "@style = { tableview: 3 }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify tableview properties
			assertEquals(3, evaluatable.getTableColumn());
			// points should remain at default (true) when not specified
			assertTrue(evaluatable.isPointsVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateFunctionWithTableViewNoPointsOnly() {
		// tableview: ~points; // no column, points=false
		String gpad = "@style = { tableview: ~points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertNotNull("geos should not be null", geos);
			assertTrue("geos should not be empty, size: " + geos.size(), geos.size() > 0);
			GeoElement geo = geos.get(0);
			assertNotNull("geo should not be null", geo);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify tableview properties
			// Column should remain at default (-1) when not specified
			assertEquals(-1, evaluatable.getTableColumn());
			assertTrue(!evaluatable.isPointsVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateFunctionWithTableViewZeroColumn() {
		// Test with column 0
		String gpad = "@style = { tableview: 0 points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify tableview properties
			assertEquals(0, evaluatable.getTableColumn());
			assertTrue(evaluatable.isPointsVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== XML to GPAD Conversion Tests ====================

	@Test
	public void testConvertTableViewColumnAndPoints() {
		// tableview: 2 points; // column=2, points=true
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("column", "2");
		tableviewAttrs.put("points", "true");
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain column 2", gpad.contains("2"));
		assertTrue("Should contain points", gpad.contains("points"));
		// Should not contain ~points
		assertTrue("Should not contain ~points", !gpad.contains("~points"));
	}

	@Test
	public void testConvertTableViewColumnAndNoPoints() {
		// tableview: 1 ~points; // column=1, points=false
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("column", "1");
		tableviewAttrs.put("points", "false");
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain column 1", gpad.contains("1"));
		assertTrue("Should contain ~points", gpad.contains("~points"));
		// Should not contain points (without ~)
		String tableviewPart = gpad.substring(gpad.indexOf("tableview:"));
		assertTrue("Should not contain points without ~", !tableviewPart.contains(" points"));
	}

	@Test
	public void testConvertTableViewPointsOnly() {
		// tableview: points; // no column, points=true
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("points", "true");
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain points", gpad.contains("points"));
		// Should not contain a column number (only points)
		String tableviewPart = gpad.substring(gpad.indexOf("tableview:"));
		// Check that there's no digit before "points" (except in "tableview:")
		int pointsIndex = tableviewPart.indexOf("points");
		String beforePoints = tableviewPart.substring(0, pointsIndex);
		// Should only have "tableview: " before points
		assertTrue("Should only have tableview: before points", 
				beforePoints.trim().equals("tableview:"));
	}

	@Test
	public void testConvertTableViewColumnOnly() {
		// tableview: 3; // column=3, no points (default)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("column", "3");
		// points not set (default is true, but we don't output it when only column is set)
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain column 3", gpad.contains("3"));
		// Should not contain points or ~points
		String tableviewPart = gpad.substring(gpad.indexOf("tableview:"));
		assertTrue("Should not contain points", !tableviewPart.contains("points"));
	}

	@Test
	public void testConvertTableViewNoPointsOnly() {
		// tableview: ~points; // no column, points=false
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("points", "false");
		// column not set (default is -1, which we don't output)
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain ~points", gpad.contains("~points"));
		// Should not contain a column number
		String tableviewPart = gpad.substring(gpad.indexOf("tableview:"));
		// Check that there's no digit after "tableview: " and before "~points"
		int tildeIndex = tableviewPart.indexOf("~points");
		String beforeTilde = tableviewPart.substring(0, tildeIndex);
		// Should only have "tableview: " before ~points
		assertTrue("Should only have tableview: before ~points", 
				beforeTilde.trim().equals("tableview:"));
	}

	@Test
	public void testConvertTableViewWithDefaultColumn() {
		// Test with column=-1 (default, should not output)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("column", "-1");
		tableviewAttrs.put("points", "true");
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Should only contain points, not column -1
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain points", gpad.contains("points"));
		// Should not contain -1
		String tableviewPart = gpad.substring(gpad.indexOf("tableview:"));
		assertTrue("Should not contain -1", !tableviewPart.contains("-1"));
	}

	@Test
	public void testConvertTableViewWithZeroColumn() {
		// Test with column 0
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> tableviewAttrs = new LinkedHashMap<>();
		tableviewAttrs.put("column", "0");
		tableviewAttrs.put("points", "true");
		styleMap.put("tableview", tableviewAttrs);
		
		String gpad = converter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tableview", gpad.contains("tableview:"));
		assertTrue("Should contain 0", gpad.contains(" 0 "));
		assertTrue("Should contain points", gpad.contains("points"));
	}

	// ==================== Integration Tests ====================

	@Test
	public void testRoundTripTableViewColumnAndPoints() {
		// Test round trip: GPAD -> XML -> GPAD
		// tableview: 2 points; // column=2, points=true
		String gpad = "@style = { tableview: 2 points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify properties are set
			assertEquals(2, evaluatable.getTableColumn());
			assertTrue(evaluatable.isPointsVisible());
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Should contain tableview with column and points
			assertTrue("Converted GPAD should contain tableview. Actual: " + convertedGpad, convertedGpad.contains("tableview:"));
			assertTrue("Converted GPAD should contain column 2", convertedGpad.contains("2"));
			assertTrue("Converted GPAD should contain points", convertedGpad.contains("points"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testRoundTripTableViewColumnAndNoPoints() {
		// Test round trip: GPAD -> XML -> GPAD
		// tableview: 1 ~points; // column=1, points=false
		String gpad = "@style = { tableview: 1 ~points }\n"
				+ "f @style = x^2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoEvaluatable);
			GeoEvaluatable evaluatable = (GeoEvaluatable) geo;
			
			// Verify properties are set
			assertEquals(1, evaluatable.getTableColumn());
			assertTrue(!evaluatable.isPointsVisible());
			
			// Convert back to GPAD
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Should contain tableview with column and ~points
			assertTrue("Converted GPAD should contain tableview", convertedGpad.contains("tableview:"));
			assertTrue("Converted GPAD should contain column 1", convertedGpad.contains("1"));
			assertTrue("Converted GPAD should contain ~points", convertedGpad.contains("~points"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}

