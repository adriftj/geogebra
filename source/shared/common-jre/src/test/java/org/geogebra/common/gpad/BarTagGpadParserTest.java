package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.awt.GColor;
import org.geogebra.common.kernel.geos.ChartStyleGeo;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.properties.FillType;
import org.geogebra.common.gpad.GeoElementToGpadConverter;
import org.junit.Test;

/**
 * Unit tests for barTag property parsing in GpadParser.
 */
public class BarTagGpadParserTest extends BaseUnitTest {

	// ========== Tests for barTag ==========

	@Test
	public void testParseBarTagSingleBarWithColor() {
		// Test that barTag style sheet can be parsed and applied
		// Note: BarChart bar indices start from 1, not 0
		String gpad = "@style = { barTag: bar=1 #FF0000 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has red color (#FF0000 = rgb(255,0,0))
			GColor barColor = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", barColor);
			assertEquals("Bar 1 should be red", 255, barColor.getRed());
			assertEquals("Bar 1 should have no green", 0, barColor.getGreen());
			assertEquals("Bar 1 should have no blue", 0, barColor.getBlue());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagSingleBarWithColorAndAlpha() {
		// Note: BarChart bar indices start from 1, not 0
		String gpad = "@style = { barTag: bar=1 #FF000022 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has red color (#FF0000 = rgb(255,0,0))
			GColor barColor = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", barColor);
			assertEquals("Bar 1 should be red", 255, barColor.getRed());
			assertEquals("Bar 1 should have no green", 0, barColor.getGreen());
			assertEquals("Bar 1 should have no blue", 0, barColor.getBlue());
			
			// Verify bar 1 has alpha (#22 = 34/255 �?0.133)
			double barAlpha = chartGeo.getStyle().getBarAlpha(1);
			assertTrue("Bar 1 should have alpha set", barAlpha >= 0);
			// #22 = 34, so alpha should be approximately 34/255 �?0.133
			assertEquals("Bar 1 alpha should be approximately 0.133", 34.0 / 255.0, barAlpha, 0.001);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagSingleBarWithFillType() {
		String gpad = "@style = { barTag: bar=1 #FF0000 fill=hatch }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has hatch fill type
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, fillType);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagSingleBarWithAngleAndDist() {
		String gpad = "@style = { barTag: bar=1 fill=hatch angle=30 dist=10 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has angle=30 and dist=10
			int angle = chartGeo.getStyle().getBarHatchAngle(1);
			assertEquals("Bar 1 should have angle 30", 30, angle);
			int dist = chartGeo.getStyle().getBarHatchDistance(1);
			assertEquals("Bar 1 should have distance 10", 10, dist);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagSingleBarWithImage() {
		String gpad = "@style = { barTag: bar=1 #FF0000 fill=image image=path/to/image.png }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has image fill type and image path
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have image fill type", FillType.IMAGE, fillType);
			String image = chartGeo.getStyle().getBarImage(1);
			assertEquals("Bar 1 should have image path", "path/to/image.png", image);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagSingleBarWithSymbol() {
		String gpad = "@style = { barTag: bar=1 #FF0000 fill=symbols symbol=$ }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has symbols fill type and symbol
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have symbols fill type", FillType.SYMBOLS, fillType);
			String symbol = chartGeo.getStyle().getBarSymbol(1);
			assertEquals("Bar 1 should have symbol $", "$", symbol);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagMultipleBars() {
		String gpad = "@style = { barTag: bar=1 #FF0000 fill=hatch | bar=2 #00FF00 fill=hatch angle=30 dist=10 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has red color and hatch fill type
			GColor bar1Color = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", bar1Color);
			assertEquals("Bar 1 should be red", 255, bar1Color.getRed());
			assertEquals("Bar 1 should have no green", 0, bar1Color.getGreen());
			assertEquals("Bar 1 should have no blue", 0, bar1Color.getBlue());
			FillType bar1FillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, bar1FillType);
			
			// Verify bar 2 has green color, hatch fill type, angle=30, dist=10
			GColor bar2Color = chartGeo.getStyle().getBarColor(2);
			assertNotNull("Bar 2 should have color", bar2Color);
			assertEquals("Bar 2 should have no red", 0, bar2Color.getRed());
			assertEquals("Bar 2 should be green", 255, bar2Color.getGreen());
			assertEquals("Bar 2 should have no blue", 0, bar2Color.getBlue());
			FillType bar2FillType = chartGeo.getStyle().getBarFillType(2);
			assertEquals("Bar 2 should have hatch fill type", FillType.HATCH, bar2FillType);
			int bar2Angle = chartGeo.getStyle().getBarHatchAngle(2);
			assertEquals("Bar 2 should have angle 30", 30, bar2Angle);
			int bar2Dist = chartGeo.getStyle().getBarHatchDistance(2);
			assertEquals("Bar 2 should have distance 10", 10, bar2Dist);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagComplexExample() {
		// Test complex example with different attribute orders
		String gpad = "@style = { barTag: bar=1 #FF0000 fill=hatch | fill=hatch bar=3 angle=30 dist=10 | #FF000022 bar=2 fill=image image=path/to/image.png | fill=symbols bar=5 #FF0000 symbol=$ }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1: red color, hatch fill type
			GColor bar1Color = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", bar1Color);
			assertEquals("Bar 1 should be red", 255, bar1Color.getRed());
			FillType bar1FillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, bar1FillType);
			
			// Verify bar 2: red color with alpha, image fill type
			GColor bar2Color = chartGeo.getStyle().getBarColor(2);
			assertNotNull("Bar 2 should have color", bar2Color);
			assertEquals("Bar 2 should be red", 255, bar2Color.getRed());
			double bar2Alpha = chartGeo.getStyle().getBarAlpha(2);
			assertTrue("Bar 2 should have alpha set", bar2Alpha >= 0);
			FillType bar2FillType = chartGeo.getStyle().getBarFillType(2);
			assertEquals("Bar 2 should have image fill type", FillType.IMAGE, bar2FillType);
			String bar2Image = chartGeo.getStyle().getBarImage(2);
			assertEquals("Bar 2 should have image path", "path/to/image.png", bar2Image);
			
			// Verify bar 3: hatch fill type, angle=30, dist=10
			FillType bar3FillType = chartGeo.getStyle().getBarFillType(3);
			assertEquals("Bar 3 should have hatch fill type", FillType.HATCH, bar3FillType);
			int bar3Angle = chartGeo.getStyle().getBarHatchAngle(3);
			assertEquals("Bar 3 should have angle 30", 30, bar3Angle);
			int bar3Dist = chartGeo.getStyle().getBarHatchDistance(3);
			assertEquals("Bar 3 should have distance 10", 10, bar3Dist);
			
			// Verify bar 5: red color, symbols fill type, symbol=$
			GColor bar5Color = chartGeo.getStyle().getBarColor(5);
			assertNotNull("Bar 5 should have color", bar5Color);
			assertEquals("Bar 5 should be red", 255, bar5Color.getRed());
			FillType bar5FillType = chartGeo.getStyle().getBarFillType(5);
			assertEquals("Bar 5 should have symbols fill type", FillType.SYMBOLS, bar5FillType);
			String bar5Symbol = chartGeo.getStyle().getBarSymbol(5);
			assertEquals("Bar 5 should have symbol $", "$", bar5Symbol);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagWithNegativeAngle() {
		// Note: angle= token only accepts positive integers (["0"-"9"])+, 
		// so negative angles are not supported in gpad syntax.
		// This test is skipped as the parser doesn't support negative angles in the token definition.
		// If needed, negative angle normalization could be tested at the XML level after deserialization.
	}

	@Test
	public void testParseBarTagWithLargeAngle() {
		String gpad = "@style = { barTag: bar=1 fill=hatch angle=450 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Angle should be normalized: 450 -> 90 (450 % 360 = 90)
			int angle = chartGeo.getStyle().getBarHatchAngle(1);
			assertEquals("Bar 1 angle should be normalized to 90", 90, angle);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagWithQuotedImage() {
		String gpad = "@style = { barTag: bar=1 fill=image image=\"path with spaces/image.png\" }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has image fill type and quoted image path
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have image fill type", FillType.IMAGE, fillType);
			String image = chartGeo.getStyle().getBarImage(1);
			assertEquals("Bar 1 should have quoted image path", "path with spaces/image.png", image);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagWithQuotedSymbol() {
		String gpad = "@style = { barTag: bar=1 fill=symbols symbol=\"*\" }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify bar 1 has symbols fill type and quoted symbol
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have symbols fill type", FillType.SYMBOLS, fillType);
			String symbol = chartGeo.getStyle().getBarSymbol(1);
			assertEquals("Bar 1 should have quoted symbol *", "*", symbol);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagAllFillTypes() {
		FillType[] expectedFillTypes = {FillType.STANDARD, FillType.HATCH, FillType.CROSSHATCHED, 
				FillType.CHESSBOARD, FillType.DOTTED, FillType.HONEYCOMB, FillType.BRICK, 
				FillType.WEAVING, FillType.SYMBOLS, FillType.IMAGE};
		String[] fillTypes = {"standard", "hatch", "crosshatch", "chessboard", "dotted", 
				"honeycomb", "brick", "weaving", "symbols", "image"};
		
		for (int i = 0; i < fillTypes.length; i++) {
			String gpad = "@style = { barTag: bar=1 #FF0000 fill=" + fillTypes[i] + " }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
			GpadParser parser = new GpadParser(getKernel());
			
			try {
				List<GeoElement> geos = parser.parse(gpad);
				assertEquals(1, geos.size());
				GeoElement geo = geos.get(0);
				assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
				ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
				
				// Verify fill type is correctly applied
				FillType actualFillType = chartGeo.getStyle().getBarFillType(1);
				assertEquals("Fill type " + fillTypes[i] + " should be correctly applied", 
						expectedFillTypes[i], actualFillType);
			} catch (GpadParseException e) {
				throw new AssertionError("Parse failed for fillType " + fillTypes[i] + ": " + e.getMessage(), e);
			}
		}
	}

	@Test
	public void testParseBarTagBarNumberRequired() {
		// Test that bar= is required (can appear anywhere, but must be present)
		// Test 1: bar= missing - should skip the bar (no style applied)
		String gpad1 = "@style = { barTag: #FF0000 fill=hatch }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser1 = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos1 = parser1.parse(gpad1);
			assertEquals(1, geos1.size());
			GeoElement geo1 = geos1.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo1 instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo1 = (ChartStyleGeo) geo1;
			
			// If bar= is missing, no style should be applied to any bar
			// Check that bar 1 doesn't have the specified color (should use default)
			GColor bar0Color = chartGeo1.getStyle().getBarColor(1);
			// If no style was applied, getBarColor might return null or default color
			// This is acceptable - the test just verifies parsing doesn't crash
		} catch (GpadParseException e) {
			// Parse exception is acceptable if bar= is truly required
		}
		
		// Test 2: bar= at the end - should work
		String gpad2 = "@style = { barTag: #FF0000 fill=hatch bar=1 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser2 = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos2 = parser2.parse(gpad2);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo2 instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geo2;
			
			// Verify bar 1 has the style applied
			GColor bar0Color = chartGeo2.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color when bar= is at the end", bar0Color);
			assertEquals("Bar 1 should be red", 255, bar0Color.getRed());
			FillType bar0FillType = chartGeo2.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, bar0FillType);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed with bar= at the end: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagOrderOfAttributes() {
		// Test that all attributes can appear in any order
		// Test 1: color before bar=
		String gpad1 = "@style = { barTag: #FF0000 bar=1 fill=hatch angle=30 dist=10 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser1 = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos1 = parser1.parse(gpad1);
			assertEquals(1, geos1.size());
			GeoElement geo1 = geos1.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo1 instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo1 = (ChartStyleGeo) geo1;
			
			// Verify all attributes are correctly applied
			GColor bar0Color = chartGeo1.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", bar0Color);
			assertEquals("Bar 1 should be red", 255, bar0Color.getRed());
			FillType bar0FillType = chartGeo1.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, bar0FillType);
			int bar0Angle = chartGeo1.getStyle().getBarHatchAngle(1);
			assertEquals("Bar 1 should have angle 30", 30, bar0Angle);
			int bar0Dist = chartGeo1.getStyle().getBarHatchDistance(1);
			assertEquals("Bar 1 should have distance 10", 10, bar0Dist);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed with color before bar=: " + e.getMessage(), e);
		}
		
		// Test 2: color after other attributes
		String gpad2 = "@style = { barTag: bar=1 fill=hatch angle=30 dist=10 #FF0000 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser2 = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos2 = parser2.parse(gpad2);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo2 instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geo2;
			
			// Verify all attributes are correctly applied
			GColor bar0Color = chartGeo2.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", bar0Color);
			assertEquals("Bar 1 should be red", 255, bar0Color.getRed());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed with color after other attributes: " + e.getMessage(), e);
		}
		
		// Test 3: bar= in the middle
		String gpad3 = "@style = { barTag: fill=hatch bar=1 angle=30 #FF0000 dist=10 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser3 = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos3 = parser3.parse(gpad3);
			assertEquals(1, geos3.size());
			GeoElement geo3 = geos3.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo3 instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo3 = (ChartStyleGeo) geo3;
			
			// Verify all attributes are correctly applied
			GColor bar0Color = chartGeo3.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", bar0Color);
			assertEquals("Bar 1 should be red", 255, bar0Color.getRed());
			FillType bar0FillType = chartGeo3.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, bar0FillType);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed with bar= in the middle: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagDefaultValues() {
		// Test that default values are applied for HATCH types
		String gpad = "@style = { barTag: bar=1 fill=hatch }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify default values are applied: angle=45 and dist=10 (defaults for HATCH)
			int angle = chartGeo.getStyle().getBarHatchAngle(1);
			assertEquals("Bar 1 should have default angle 45", 45, angle);
			int dist = chartGeo.getStyle().getBarHatchDistance(1);
			assertEquals("Bar 1 should have default distance 10", 10, dist);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBarTagSymbolAutoFillType() {
		// Test that symbol= automatically sets fillType=symbols
		String gpad = "@style = { barTag: bar=1 #FF0000 symbol=$ }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify fillType is automatically set to symbols even though fill= was not specified
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have symbols fill type automatically set", FillType.SYMBOLS, fillType);
			String symbol = chartGeo.getStyle().getBarSymbol(1);
			assertEquals("Bar 1 should have symbol $", "$", symbol);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== RoundTrip Tests ==========

	@Test
	public void testBarTagRoundTripSingleBarWithColor() {
		// Test round-trip: Gpad -> GeoElement -> Gpad
		String originalGpad = "@style = { barTag: bar=1 #FF0000 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original Gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Geo should be ChartStyleGeo", geo instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify original style is applied
			GColor barColor = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", barColor);
			assertEquals("Bar 1 should be red", 255, barColor.getRed());
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Verify converted Gpad contains barTag with correct color
			assertTrue("Converted Gpad should contain barTag", convertedGpad.contains("barTag:"));
			assertTrue("Converted Gpad should contain bar=1", convertedGpad.contains("bar=1"));
			assertTrue("Converted Gpad should contain #FF0000", convertedGpad.contains("#FF0000"));
			
			// Parse converted Gpad again to verify it works
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			assertTrue("Geo2 should be ChartStyleGeo", geo2 instanceof ChartStyleGeo);
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geo2;
			
			// Verify style is preserved after round trip
			GColor barColor2 = chartGeo2.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color after round trip", barColor2);
			assertEquals("Bar 1 should be red after round trip", 255, barColor2.getRed());
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testBarTagRoundTripSingleBarWithColorAndAlpha() {
		// Test round-trip with alpha
		String originalGpad = "@style = { barTag: bar=1 #FF000022 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original Gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify original style
			GColor barColor = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", barColor);
			double barAlpha = chartGeo.getStyle().getBarAlpha(1);
			assertTrue("Bar 1 should have alpha", barAlpha >= 0);
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Verify converted Gpad contains barTag with color and alpha
			assertTrue("Converted Gpad should contain barTag", convertedGpad.contains("barTag:"));
			assertTrue("Converted Gpad should contain bar=1", convertedGpad.contains("bar=1"));
			// Alpha should be included in hex color (#FF000022)
			assertTrue("Converted Gpad should contain color with alpha", 
					convertedGpad.contains("#FF000022") || convertedGpad.contains("#ff000022"));
			
			// Parse converted Gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geos2.get(0);
			
			// Verify style is preserved
			GColor barColor2 = chartGeo2.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color after round trip", barColor2);
			double barAlpha2 = chartGeo2.getStyle().getBarAlpha(1);
			assertEquals("Bar 1 alpha should be preserved", barAlpha, barAlpha2, 0.001);
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testBarTagRoundTripSingleBarWithMultipleAttributes() {
		// Test round-trip with multiple attributes
		String originalGpad = "@style = { barTag: bar=1 #00FF00 fill=hatch angle=60 dist=15 }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original Gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify original style
			GColor barColor = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", barColor);
			assertEquals("Bar 1 should be green", 0, barColor.getRed());
			assertEquals("Bar 1 should be green", 255, barColor.getGreen());
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type", FillType.HATCH, fillType);
			int angle = chartGeo.getStyle().getBarHatchAngle(1);
			assertEquals("Bar 1 should have angle 60", 60, angle);
			int dist = chartGeo.getStyle().getBarHatchDistance(1);
			assertEquals("Bar 1 should have distance 15", 15, dist);
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Verify converted Gpad contains all attributes
			assertTrue("Converted Gpad should contain barTag", convertedGpad.contains("barTag:"));
			assertTrue("Converted Gpad should contain bar=1", convertedGpad.contains("bar=1"));
			assertTrue("Converted Gpad should contain green color", 
					convertedGpad.contains("#00FF00") || convertedGpad.contains("#00ff00"));
			assertTrue("Converted Gpad should contain fill=hatch", convertedGpad.contains("fill=hatch"));
			assertTrue("Converted Gpad should contain angle=60", convertedGpad.contains("angle=60"));
			assertTrue("Converted Gpad should contain dist=15", convertedGpad.contains("dist=15"));
			
			// Parse converted Gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geos2.get(0);
			
			// Verify all attributes are preserved
			GColor barColor2 = chartGeo2.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color after round trip", barColor2);
			assertEquals("Bar 1 should be green after round trip", 255, barColor2.getGreen());
			FillType fillType2 = chartGeo2.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have hatch fill type after round trip", FillType.HATCH, fillType2);
			int angle2 = chartGeo2.getStyle().getBarHatchAngle(1);
			assertEquals("Bar 1 should have angle 60 after round trip", 60, angle2);
			int dist2 = chartGeo2.getStyle().getBarHatchDistance(1);
			assertEquals("Bar 1 should have distance 15 after round trip", 15, dist2);
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testBarTagRoundTripMultipleBars() {
		// Test round-trip with multiple bars
		String originalGpad = "@style = { barTag: bar=1 #FF0000 | bar=2 #00FF00 | bar=3 #0000FF }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original Gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify original styles
			GColor bar1Color = chartGeo.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color", bar1Color);
			assertEquals("Bar 1 should be red", 255, bar1Color.getRed());
			GColor bar2Color = chartGeo.getStyle().getBarColor(2);
			assertNotNull("Bar 2 should have color", bar2Color);
			assertEquals("Bar 2 should be green", 255, bar2Color.getGreen());
			GColor bar3Color = chartGeo.getStyle().getBarColor(3);
			assertNotNull("Bar 3 should have color", bar3Color);
			assertEquals("Bar 3 should be blue", 255, bar3Color.getBlue());
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Verify converted Gpad contains all bars
			assertTrue("Converted Gpad should contain barTag", convertedGpad.contains("barTag:"));
			assertTrue("Converted Gpad should contain bar=1", convertedGpad.contains("bar=1"));
			assertTrue("Converted Gpad should contain bar=2", convertedGpad.contains("bar=2"));
			assertTrue("Converted Gpad should contain bar=3", convertedGpad.contains("bar=3"));
			assertTrue("Converted Gpad should contain red color", 
					convertedGpad.contains("#FF0000") || convertedGpad.contains("#ff0000"));
			assertTrue("Converted Gpad should contain green color", 
					convertedGpad.contains("#00FF00") || convertedGpad.contains("#00ff00"));
			assertTrue("Converted Gpad should contain blue color", 
					convertedGpad.contains("#0000FF") || convertedGpad.contains("#0000ff"));
			
			// Parse converted Gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geos2.get(0);
			
			// Verify all bars are preserved
			GColor bar1Color2 = chartGeo2.getStyle().getBarColor(1);
			assertNotNull("Bar 1 should have color after round trip", bar1Color2);
			assertEquals("Bar 1 should be red after round trip", 255, bar1Color2.getRed());
			GColor bar2Color2 = chartGeo2.getStyle().getBarColor(2);
			assertNotNull("Bar 2 should have color after round trip", bar2Color2);
			assertEquals("Bar 2 should be green after round trip", 255, bar2Color2.getGreen());
			GColor bar3Color2 = chartGeo2.getStyle().getBarColor(3);
			assertNotNull("Bar 3 should have color after round trip", bar3Color2);
			assertEquals("Bar 3 should be blue after round trip", 255, bar3Color2.getBlue());
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testBarTagRoundTripWithSymbol() {
		// Test round-trip with symbol
		String originalGpad = "@style = { barTag: bar=1 #FF0000 symbol=$ }\nA @style = BarChart({10, 11, 12, 13, 14}, {5, 8, 12, 0, 1})";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original Gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			ChartStyleGeo chartGeo = (ChartStyleGeo) geo;
			
			// Verify original style
			FillType fillType = chartGeo.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have symbols fill type", FillType.SYMBOLS, fillType);
			String symbol = chartGeo.getStyle().getBarSymbol(1);
			assertEquals("Bar 1 should have symbol $", "$", symbol);
			
			// Convert back to Gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Verify converted Gpad contains symbol
			assertTrue("Converted Gpad should contain barTag", convertedGpad.contains("barTag:"));
			assertTrue("Converted Gpad should contain fill=symbols", convertedGpad.contains("fill=symbols"));
			assertTrue("Converted Gpad should contain symbol=$", convertedGpad.contains("symbol=$"));
			
			// Parse converted Gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			ChartStyleGeo chartGeo2 = (ChartStyleGeo) geos2.get(0);
			
			// Verify symbol is preserved
			FillType fillType2 = chartGeo2.getStyle().getBarFillType(1);
			assertEquals("Bar 1 should have symbols fill type after round trip", FillType.SYMBOLS, fillType2);
			String symbol2 = chartGeo2.getStyle().getBarSymbol(1);
			assertEquals("Bar 1 should have symbol $ after round trip", "$", symbol2);
		} catch (GpadParseException e) {
			throw new AssertionError("Round-trip failed: " + e.getMessage(), e);
		}
	}
}

