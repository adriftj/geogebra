package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.io.GpadStyleXMLApplier;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Unit tests for GpadStyleSheet reset marker and merge functionality.
 * Tests the "clear then set" semantics where reset marker can coexist with normal attributes.
 */
public class GpadStyleSheetResetTest extends BaseUnitTest {

	// ========== Tests for resetProperty method ==========

	@Test
	public void testResetProperty() {
		GpadStyleSheet sheet = new GpadStyleSheet("test");
		
		// Set a property first
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("type", "1");
		attrs.put("thickness", "5");
		sheet.setProperty("lineStyle", attrs);
		
		// Reset the property
		sheet.resetProperty("lineStyle");
		
		// Check that reset marker is present
		LinkedHashMap<String, String> resetAttrs = sheet.getProperty("lineStyle");
		assertNotNull(resetAttrs);
		assertTrue("Should contain reset marker", resetAttrs.containsKey("~"));
		assertEquals("Reset marker value should be empty", "", resetAttrs.get("~"));
		// Normal attributes should still be present (resetProperty only adds marker)
		assertEquals("1", resetAttrs.get("type"));
		assertEquals("5", resetAttrs.get("thickness"));
	}

	@Test
	public void testResetPropertyOnNewProperty() {
		GpadStyleSheet sheet = new GpadStyleSheet("test");
		
		// Reset a property that doesn't exist yet
		sheet.resetProperty("lineStyle");
		
		// Check that reset marker is present
		LinkedHashMap<String, String> resetAttrs = sheet.getProperty("lineStyle");
		assertNotNull(resetAttrs);
		assertTrue("Should contain reset marker", resetAttrs.containsKey("~"));
		assertEquals("Reset marker value should be empty", "", resetAttrs.get("~"));
	}

	// ========== Tests for mergeFrom with reset marker ==========

	@Test
	public void testMergeFromWithResetMarkerCompletelyReplaces() {
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		
		// sheet1 has lineStyle with normal attributes
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("type", "1");
		attrs1.put("thickness", "5");
		sheet1.setProperty("lineStyle", attrs1);
		
		// sheet2 has lineStyle with reset marker and different attributes
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("~", "");
		attrs2.put("type", "2");
		attrs2.put("thickness", "10");
		sheet2.setProperty("lineStyle", attrs2);
		
		// Merge sheet2 into sheet1
		sheet1.mergeFrom(sheet2);
		
		// sheet1's lineStyle should be completely replaced with sheet2's content
		LinkedHashMap<String, String> mergedAttrs = sheet1.getProperty("lineStyle");
		assertNotNull(mergedAttrs);
		assertTrue("Should contain reset marker", mergedAttrs.containsKey("~"));
		assertEquals("2", mergedAttrs.get("type"));
		assertEquals("10", mergedAttrs.get("thickness"));
		// Original type="1" should be gone (replaced by type="2")
		assertEquals("2", mergedAttrs.get("type")); // Already checked above, but confirms original is gone
	}

	@Test
	public void testMergeFromWithResetMarkerAndNormalAttributes() {
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		
		// sheet1 has lineStyle
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("type", "1");
		sheet1.setProperty("lineStyle", attrs1);
		
		// sheet2 has lineStyle with reset marker and normal attributes
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("~", "");
		attrs2.put("type", "2");
		attrs2.put("thickness", "10");
		sheet2.setProperty("lineStyle", attrs2);
		
		// Merge sheet2 into sheet1
		sheet1.mergeFrom(sheet2);
		
		// sheet1 should have reset marker and all attributes from sheet2
		LinkedHashMap<String, String> mergedAttrs = sheet1.getProperty("lineStyle");
		assertNotNull(mergedAttrs);
		assertTrue("Should contain reset marker", mergedAttrs.containsKey("~"));
		assertEquals("2", mergedAttrs.get("type"));
		assertEquals("10", mergedAttrs.get("thickness"));
	}

	@Test
	public void testMergeFromWithoutResetMarkerNormalMerge() {
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		
		// sheet1 has lineStyle
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("type", "1");
		attrs1.put("opacity", "100");
		sheet1.setProperty("lineStyle", attrs1);
		
		// sheet2 has lineStyle without reset marker
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("type", "2");
		attrs2.put("thickness", "10");
		sheet2.setProperty("lineStyle", attrs2);
		
		// Merge sheet2 into sheet1
		sheet1.mergeFrom(sheet2);
		
		// sheet1 should have merged attributes (sheet2 overrides sheet1)
		LinkedHashMap<String, String> mergedAttrs = sheet1.getProperty("lineStyle");
		assertNotNull(mergedAttrs);
		assertEquals("2", mergedAttrs.get("type")); // Overridden by sheet2
		assertEquals("10", mergedAttrs.get("thickness")); // From sheet2
		assertEquals("100", mergedAttrs.get("opacity")); // From sheet1 (not overridden)
		assertTrue("Should not contain reset marker", !mergedAttrs.containsKey("~"));
	}

	@Test
	public void testMergeFromMultiplePropertiesWithResetMarker() {
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		
		// sheet1 has multiple properties
		LinkedHashMap<String, String> lineStyle1 = new LinkedHashMap<>();
		lineStyle1.put("type", "1");
		sheet1.setProperty("lineStyle", lineStyle1);
		
		LinkedHashMap<String, String> objColor1 = new LinkedHashMap<>();
		objColor1.put("r", "255");
		sheet1.setProperty("objColor", objColor1);
		
		// sheet2 has reset marker on lineStyle but not on objColor
		LinkedHashMap<String, String> lineStyle2 = new LinkedHashMap<>();
		lineStyle2.put("~", "");
		lineStyle2.put("type", "2");
		sheet2.setProperty("lineStyle", lineStyle2);
		
		LinkedHashMap<String, String> objColor2 = new LinkedHashMap<>();
		objColor2.put("g", "128");
		sheet2.setProperty("objColor", objColor2);
		
		// Merge sheet2 into sheet1
		sheet1.mergeFrom(sheet2);
		
		// lineStyle should be completely replaced (has reset marker)
		LinkedHashMap<String, String> mergedLineStyle = sheet1.getProperty("lineStyle");
		assertNotNull(mergedLineStyle);
		assertTrue("Should contain reset marker", mergedLineStyle.containsKey("~"));
		assertEquals("2", mergedLineStyle.get("type"));
		
		// objColor should be merged normally (no reset marker)
		LinkedHashMap<String, String> mergedObjColor = sheet1.getProperty("objColor");
		assertNotNull(mergedObjColor);
		assertTrue("Should not contain reset marker", !mergedObjColor.containsKey("~"));
		assertEquals("255", mergedObjColor.get("r")); // From sheet1
		assertEquals("128", mergedObjColor.get("g")); // From sheet2
	}

	@Test
	public void testMergeFromResetMarkerOnly() {
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		
		// sheet1 has lineStyle with attributes
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("type", "1");
		attrs1.put("thickness", "5");
		sheet1.setProperty("lineStyle", attrs1);
		
		// sheet2 has lineStyle with only reset marker
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("~", "");
		sheet2.setProperty("lineStyle", attrs2);
		
		// Merge sheet2 into sheet1
		sheet1.mergeFrom(sheet2);
		
		// sheet1 should have only reset marker
		LinkedHashMap<String, String> mergedAttrs = sheet1.getProperty("lineStyle");
		assertNotNull(mergedAttrs);
		assertTrue("Should contain reset marker", mergedAttrs.containsKey("~"));
		assertEquals("", mergedAttrs.get("~"));
		// Original attributes should be gone
		assertNull(mergedAttrs.get("type"));
		assertNull(mergedAttrs.get("thickness"));
	}

	// ========== Tests for GpadStyleXMLApplier with reset marker ==========

	@Test
	public void testApplyWithResetMarkerAndNormalAttributes() {
		// Create a point
		GeoPoint point = new GeoPoint(getConstruction());
		point.setCoords(1, 2, 1);
		point.setLabel("A");
		
		// Set some initial properties
		point.setPointSize(5);
		
		// Create style sheet with reset marker and normal attributes
		GpadStyleSheet styleSheet = new GpadStyleSheet("");
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("~", ""); // Reset marker
		attrs.put("val", "8"); // Normal attribute
		styleSheet.setProperty("pointSize", attrs);
		
		// Apply style sheet
		GpadStyleXMLApplier.apply(styleSheet, point);
		
		// Point size should be reset to default first, then set to 8
		// Since we're resetting first, the default value should be applied, then overridden by 8
		// Actually, the reset marker means: clear first, then set the normal attributes
		// So pointSize should be 8
		assertEquals(8, point.getPointSize());
	}

	@Test
	public void testApplyWithResetMarkerOnly() {
		// Create a line
		GeoLine line = new GeoLine(getConstruction());
		line.setLabel("g");
		
		// Set some initial properties
		line.setLineThickness(10);
		line.setLineType(1);
		
		// Create style sheet with reset marker only (no normal attributes)
		GpadStyleSheet styleSheet = new GpadStyleSheet("");
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("~", ""); // Reset marker only
		styleSheet.setProperty("lineStyle", attrs);
		
		// Apply style sheet
		GpadStyleXMLApplier.apply(styleSheet, line);
		
		// lineStyle should be reset to default values
		// Default lineStyle has type=0, thickness=5 (from ConstructionDefaults)
		assertEquals(0, line.getLineType());
		assertEquals(5, line.getLineThickness());
	}

	@Test
	public void testApplyWithResetMarkerAndMultipleAttributes() {
		// Create a line
		GeoLine line = new GeoLine(getConstruction());
		line.setLabel("g");
		
		// Set some initial properties
		line.setLineThickness(10);
		line.setLineType(1);
		
		// Create style sheet with reset marker and multiple normal attributes
		GpadStyleSheet styleSheet = new GpadStyleSheet("");
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("~", ""); // Reset marker
		attrs.put("type", "2"); // Normal attribute
		attrs.put("thickness", "5"); // Normal attribute
		styleSheet.setProperty("lineStyle", attrs);
		
		// Apply style sheet
		GpadStyleXMLApplier.apply(styleSheet, line);
		
		// lineStyle should be reset first, then set to type=2, thickness=5
		assertEquals(2, line.getLineType());
		assertEquals(5, line.getLineThickness());
	}

	@Test
	public void testApplyMultiplePropertiesWithResetMarker() {
		// Create a point
		GeoPoint point = new GeoPoint(getConstruction());
		point.setCoords(1, 2, 1);
		point.setLabel("A");
		
		// Set some initial properties
		point.setPointSize(5);
		point.setObjColor(GColor.newColor(255, 0, 0)); // Red
		
		// Create style sheet with reset marker on pointSize but not on objColor
		GpadStyleSheet styleSheet = new GpadStyleSheet("");
		
		// pointSize with reset marker
		LinkedHashMap<String, String> pointSizeAttrs = new LinkedHashMap<>();
		pointSizeAttrs.put("~", "");
		pointSizeAttrs.put("val", "8");
		styleSheet.setProperty("pointSize", pointSizeAttrs);
		
		// objColor without reset marker
		LinkedHashMap<String, String> objColorAttrs = new LinkedHashMap<>();
		objColorAttrs.put("r", "0");
		objColorAttrs.put("g", "255");
		objColorAttrs.put("b", "0");
		styleSheet.setProperty("objColor", objColorAttrs);
		
		// Apply style sheet
		GpadStyleXMLApplier.apply(styleSheet, point);
		
		// pointSize should be reset then set to 8
		assertEquals(8, point.getPointSize());
		
		// objColor should be set to green (merged with existing red, but g=255 overrides)
		// Actually, objColor is completely replaced, not merged
		assertEquals(0, point.getObjectColor().getRed());
		assertEquals(255, point.getObjectColor().getGreen());
		assertEquals(0, point.getObjectColor().getBlue());
	}

	// ========== Tests for parser ~key syntax ==========

	@Test
	public void testParseDeleteSyntax() {
		String gpad = "@style = { ~pointSize }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			java.util.List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			// Check that reset marker is set
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("pointSize");
			assertNotNull(attrs);
			assertTrue("Should contain reset marker", attrs.containsKey("~"));
			assertEquals("Reset marker value should be empty", "", attrs.get("~"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDeleteSyntaxWithNormalProperty() {
		String gpad = "@style = { ~pointSize; pointSize: 8 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			java.util.List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			// Check that reset marker and normal attribute coexist
			LinkedHashMap<String, String> attrs = styleSheet.getProperty("pointSize");
			assertNotNull(attrs);
			assertTrue("Should contain reset marker", attrs.containsKey("~"));
			assertEquals("8", attrs.get("val"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDeleteSyntaxMultipleProperties() {
		String gpad = "@style = { ~pointSize; ~lineStyle; objColor: #ff0000 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			java.util.List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			// Check pointSize has reset marker
			LinkedHashMap<String, String> pointSizeAttrs = styleSheet.getProperty("pointSize");
			assertNotNull(pointSizeAttrs);
			assertTrue("Should contain reset marker", pointSizeAttrs.containsKey("~"));
			
			// Check lineStyle has reset marker
			LinkedHashMap<String, String> lineStyleAttrs = styleSheet.getProperty("lineStyle");
			assertNotNull(lineStyleAttrs);
			assertTrue("Should contain reset marker", lineStyleAttrs.containsKey("~"));
			
			// Check objColor does not have reset marker
			LinkedHashMap<String, String> objColorAttrs = styleSheet.getProperty("objColor");
			assertNotNull(objColorAttrs);
			assertTrue("Should not contain reset marker", !objColorAttrs.containsKey("~"));
			assertEquals("255", objColorAttrs.get("r"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for complex merge scenarios ==========

	@Test
	public void testComplexMergeScenario() {
		// Test scenario: multiple style sheets merged in sequence
		// sheet1 -> sheet2 -> sheet3, where sheet2 has reset marker
		
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		GpadStyleSheet sheet3 = new GpadStyleSheet("sheet3");
		
		// sheet1: initial properties
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("type", "1");
		attrs1.put("thickness", "5");
		sheet1.setProperty("lineStyle", attrs1);
		
		// sheet2: reset marker (clears everything)
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("~", "");
		sheet2.setProperty("lineStyle", attrs2);
		
		// sheet3: new properties (no reset marker)
		LinkedHashMap<String, String> attrs3 = new LinkedHashMap<>();
		attrs3.put("type", "2");
		attrs3.put("thickness", "10");
		sheet3.setProperty("lineStyle", attrs3);
		
		// Merge: sheet1 -> sheet2 -> sheet3
		sheet1.mergeFrom(sheet2);
		sheet1.mergeFrom(sheet3);
		
		// Result: sheet2's reset marker should be preserved, then sheet3's attributes merged
		LinkedHashMap<String, String> finalAttrs = sheet1.getProperty("lineStyle");
		assertNotNull(finalAttrs);
		// Since sheet2 has reset marker, it completely replaces sheet1
		// Then sheet3 merges normally, but since sheet2 only has reset marker,
		// the final result should have reset marker + sheet3's attributes
		assertTrue("Should contain reset marker", finalAttrs.containsKey("~"));
		assertEquals("2", finalAttrs.get("type"));
		assertEquals("10", finalAttrs.get("thickness"));
	}

	@Test
	public void testMergeResetThenSet() {
		// Test scenario: reset marker with normal attributes in same sheet
		GpadStyleSheet sheet1 = new GpadStyleSheet("sheet1");
		GpadStyleSheet sheet2 = new GpadStyleSheet("sheet2");
		
		// sheet1: initial properties
		LinkedHashMap<String, String> attrs1 = new LinkedHashMap<>();
		attrs1.put("type", "1");
		attrs1.put("thickness", "5");
		sheet1.setProperty("lineStyle", attrs1);
		
		// sheet2: reset marker with new attributes (clear then set)
		LinkedHashMap<String, String> attrs2 = new LinkedHashMap<>();
		attrs2.put("~", "");
		attrs2.put("type", "2");
		attrs2.put("thickness", "10");
		sheet2.setProperty("lineStyle", attrs2);
		
		// Merge sheet2 into sheet1
		sheet1.mergeFrom(sheet2);
		
		// Result: reset marker + new attributes
		LinkedHashMap<String, String> finalAttrs = sheet1.getProperty("lineStyle");
		assertNotNull(finalAttrs);
		assertTrue("Should contain reset marker", finalAttrs.containsKey("~"));
		assertEquals("2", finalAttrs.get("type"));
		assertEquals("10", finalAttrs.get("thickness"));
	}
}

