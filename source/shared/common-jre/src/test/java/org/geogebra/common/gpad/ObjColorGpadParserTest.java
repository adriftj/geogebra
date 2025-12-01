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
import org.junit.Test;

/**
 * Unit tests for objColor property parsing in GpadParser.
 */
public class ObjColorGpadParserTest extends BaseUnitTest {

	// ========== Tests for objColor ==========

	@Test
	public void testParseObjColorStaticHex6() {
		String gpad = "@style = { objColor: #FF0000 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("255", attrs.get("r"));
			assertEquals("0", attrs.get("g"));
			assertEquals("0", attrs.get("b"));
			assertNull(attrs.get("alpha")); // alpha is absence
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorStaticHex8() {
		String gpad = "@style = { objColor: #FF000080 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("255", attrs.get("r"));
			assertEquals("0", attrs.get("g"));
			assertEquals("0", attrs.get("b"));
			// alpha = 0x80 = 128, 128/255 = 0.50196...
			double alpha = Double.parseDouble(attrs.get("alpha"));
			assertTrue(Math.abs(alpha - 128.0 / 255.0) < 0.001);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorStaticHexLowercase() {
		String gpad = "@style = { objColor: #00ff00 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("0", attrs.get("r"));
			assertEquals("255", attrs.get("g"));
			assertEquals("0", attrs.get("b"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicRgb3Params() {
		String gpad = "@style = { objColor: rgb(x, y, z) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x", attrs.get("dynamicr"));
			assertEquals("y", attrs.get("dynamicg"));
			assertEquals("z", attrs.get("dynamicb"));
			assertTrue(attrs.get("dynamica") == null); // No alpha
			assertEquals("0", attrs.get("colorSpace")); // RGB
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicRgb4Params() {
		String gpad = "@style = { objColor: rgb(x, y, z, a) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x", attrs.get("dynamicr"));
			assertEquals("y", attrs.get("dynamicg"));
			assertEquals("z", attrs.get("dynamicb"));
			assertEquals("a", attrs.get("dynamica")); // Has alpha
			assertEquals("0", attrs.get("colorSpace")); // RGB
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicHsv3Params() {
		String gpad = "@style = { objColor: hsv(h, s, v) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("h", attrs.get("dynamicr"));
			assertEquals("s", attrs.get("dynamicg"));
			assertEquals("v", attrs.get("dynamicb"));
			assertEquals("1", attrs.get("colorSpace")); // HSB
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicHsv4Params() {
		String gpad = "@style = { objColor: hsv(h, s, v, a) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("h", attrs.get("dynamicr"));
			assertEquals("s", attrs.get("dynamicg"));
			assertEquals("v", attrs.get("dynamicb"));
			assertEquals("a", attrs.get("dynamica"));
			assertEquals("1", attrs.get("colorSpace")); // HSB
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicHsl3Params() {
		String gpad = "@style = { objColor: hsl(h, s, l) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("h", attrs.get("dynamicr"));
			assertEquals("s", attrs.get("dynamicg"));
			assertEquals("l", attrs.get("dynamicb"));
			assertEquals("2", attrs.get("colorSpace")); // HSL
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicHsl4Params() {
		String gpad = "@style = { objColor: hsl(h, s, l, a) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("h", attrs.get("dynamicr"));
			assertEquals("s", attrs.get("dynamicg"));
			assertEquals("l", attrs.get("dynamicb"));
			assertEquals("a", attrs.get("dynamica"));
			assertEquals("2", attrs.get("colorSpace")); // HSL
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicWithComplexExpressions() {
		String gpad = "@style = { objColor: rgb(x*255, sin(y)*128+128, z^2*255) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x*255", attrs.get("dynamicr"));
			assertEquals("sin(y)*128+128", attrs.get("dynamicg"));
			assertEquals("z^2*255", attrs.get("dynamicb"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicWithQuotedExpressions() {
		String gpad = "@style = { objColor: rgb(\"x*255\", \"y+128\", \"z/2\") }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x*255", attrs.get("dynamicr"));
			assertEquals("y+128", attrs.get("dynamicg"));
			assertEquals("z/2", attrs.get("dynamicb"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicWithEscapeSequences() {
		String gpad = "@style = { objColor: rgb(\"x\\n*255\", \"y\\t+128\", \"z\\r/2\") }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x\n*255", attrs.get("dynamicr")); // \n converted to newline
			assertEquals("y\t+128", attrs.get("dynamicg")); // \t converted to tab
			assertEquals("z\r/2", attrs.get("dynamicb")); // \r converted to carriage return
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithFillType() {
		String gpad = "@style = { objColor: #FF0000 fill=hatch }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("1", attrs.get("fillType")); // HATCH
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithAllFillTypes() {
		String[] fillTypes = {"standard", "hatch", "crosshatch", "chessboard", "dotted", 
				"honeycomb", "brick", "weaving", "symbols", "image"};
		int[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		
		for (int i = 0; i < fillTypes.length; i++) {
			String gpad = "@style = { objColor: #FF0000 fill=" + fillTypes[i] + " }\nA @style = (1, 2)";
			GpadParser parser = new GpadParser(getKernel());
			
			try {
				List<GeoElement> geos = parser.parse(gpad);
				assertEquals(1, geos.size());
				GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
				assertNotNull(styleSheet);
				java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
				assertNotNull("Fill type " + fillTypes[i] + " should be parsed", attrs);
				assertEquals("Fill type value should match", String.valueOf(expectedValues[i]), attrs.get("fillType"));
			} catch (GpadParseException e) {
				throw new AssertionError("Parse failed for fill type " + fillTypes[i] + ": " + e.getMessage(), e);
			}
		}
	}

	@Test
	public void testParseObjColorWithHatchAngle() {
		String gpad = "@style = { objColor: #FF0000 fill=hatch angle=30 dist=10 }\nA = (1, 2);B=(2,2);C=(2,1);D=(1,1);P @style,a,b,c,d=Polygon(A,B,C,D)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(9, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("30", attrs.get("hatchAngle"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithHatchDistance() {
		String gpad = "@style = { objColor: #FF0000 fill=hatch dist=15 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("15", attrs.get("hatchDistance"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithImage() {
		String gpad = "@style = { objColor: #FF0000 fill=image image=path/to/image.png }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("path/to/image.png", attrs.get("image"));
			assertEquals("9", attrs.get("fillType")); // IMAGE
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithImageQuoted() {
		String gpad = "@style = { objColor: #FF0000 fill=image image=\"path/to/image with spaces.png\" }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("path/to/image with spaces.png", attrs.get("image"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithFillSymbol() {
		String gpad = "@style = { objColor: #FF0000 fill=symbols symbol=$ }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("$", attrs.get("fillSymbol"));
			assertEquals("8", attrs.get("fillType")); // SYMBOLS
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithFillSymbolQuoted() {
		String gpad = "@style = { objColor: #FF0000 fill=symbols symbol=\"*\" }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("*", attrs.get("fillSymbol"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithInverse() {
		String gpad = "@style = { objColor: #FF0000 inverse }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("true", attrs.get("inverseFill"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithTildeInverse() {
		String gpad = "@style = { objColor: #FF0000 ~inverse }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("false", attrs.get("inverseFill")); // ~inverse clears inverse
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorCombinedProperties() {
		String gpad = "@style = { objColor: rgb(x, y, z) fill=hatch angle=30 dist=15 inverse }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x", attrs.get("dynamicr"));
			assertEquals("y", attrs.get("dynamicg"));
			assertEquals("z", attrs.get("dynamicb"));
			assertEquals("1", attrs.get("fillType")); // HATCH
			assertEquals("30", attrs.get("hatchAngle"));
			assertEquals("15", attrs.get("hatchDistance"));
			assertEquals("true", attrs.get("inverseFill"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorAllProperties() {
		String gpad = "@style = { objColor: hsv(h, s, v, a) fill=crosshatch angle=60 dist=20 image=test.png symbol=* inverse }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("h", attrs.get("dynamicr"));
			assertEquals("s", attrs.get("dynamicg"));
			assertEquals("v", attrs.get("dynamicb"));
			assertEquals("a", attrs.get("dynamica"));
			assertEquals("1", attrs.get("colorSpace")); // HSB
			assertEquals("2", attrs.get("fillType")); // CROSSHATCHED
			assertEquals("60", attrs.get("hatchAngle"));
			assertEquals("20", attrs.get("hatchDistance"));
			assertEquals("test.png", attrs.get("image"));
			assertEquals("*", attrs.get("fillSymbol"));
			assertEquals("true", attrs.get("inverseFill"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDifferentOrder() {
		// Test that order doesn't matter
		String gpad = "@style = { objColor: #FF0000 inverse fill=hatch dist=15 angle=30 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("1", attrs.get("fillType"));
			assertEquals("30", attrs.get("hatchAngle"));
			assertEquals("15", attrs.get("hatchDistance"));
			assertEquals("true", attrs.get("inverseFill"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithInlineStyle() {
		String gpad = "A { objColor: #FF0000 fill=hatch angle=45 } = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorStaticWithOnlyFill() {
		// Test with only fill type (no color specified)
		String gpad = "@style = { objColor: fill=hatch }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			// Should still have fillType even if no color
			assertNotNull(attrs);
			assertEquals("1", attrs.get("fillType"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicHsvWithComplexExpressions() {
		String gpad = "@style = { objColor: hsv(x*360, sin(y), cos(z), abs(a)) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x*360", attrs.get("dynamicr"));
			assertEquals("sin(y)", attrs.get("dynamicg"));
			assertEquals("cos(z)", attrs.get("dynamicb"));
			assertEquals("abs(a)", attrs.get("dynamica"));
			assertEquals("1", attrs.get("colorSpace")); // HSB
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorDynamicHslWithComplexExpressions() {
		String gpad = "@style = { objColor: hsl(x*360, s*0.8, l+0.2, min(a,1)) }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("x*360", attrs.get("dynamicr"));
			assertEquals("s*0.8", attrs.get("dynamicg"));
			assertEquals("l+0.2", attrs.get("dynamicb"));
			assertEquals("min(a,1)", attrs.get("dynamica"));
			assertEquals("2", attrs.get("colorSpace")); // HSL
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseObjColorWithEscapeSequencesInImage() {
		String gpad = "@style = { objColor: #FF0000 fill=image image=\"path\\nwith\\tnewlines.png\" }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("objColor");
			assertNotNull(attrs);
			assertEquals("path\nwith\tnewlines.png", attrs.get("image")); // Escape sequences processed
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for XML to Gpad conversion (objColor) ==========

	@Test
	public void testConvertObjColorStaticHex6() {
		// Test static color with 6-digit hex (no alpha or alpha=1.0)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("alpha", "1.0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain objColor", gpad.contains("objColor"));
		assertTrue("Should contain #FF0000", gpad.contains("#FF0000"));
		assertTrue("Should not contain alpha (default 1.0)", !gpad.contains("#FF0000FF"));
	}

	@Test
	public void testConvertObjColorStaticHex8() {
		// Test static color with 8-digit hex (alpha != 1.0)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("alpha", "0.5"); // 128/255
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain objColor", gpad.contains("objColor"));
		assertTrue("Should contain 8-digit hex with alpha", gpad.contains("#FF0000"));
		// Alpha = 0.5 * 255 = 127.5 â‰?128 = 0x80
		assertTrue("Should contain alpha component", gpad.contains("80") || gpad.contains("7F"));
	}

	@Test
	public void testConvertObjColorStaticHexWithoutAlpha() {
		// Test static color without alpha attribute (should default to 1.0, so no alpha in output)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "0");
		attrs.put("g", "255");
		attrs.put("b", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain #00FF00", gpad.contains("#00FF00"));
		assertTrue("Should not contain 8-digit hex", !gpad.contains("#00FF00FF"));
	}

	@Test
	public void testConvertObjColorDynamicRgb3Params() {
		// Test dynamic RGB color with 3 parameters
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x");
		attrs.put("dynamicg", "y");
		attrs.put("dynamicb", "z");
		attrs.put("colorSpace", "0"); // RGB
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain objColor", gpad.contains("objColor"));
		assertTrue("Should contain rgb(", gpad.contains("rgb("));
		assertTrue("Should contain x,y,z", gpad.contains("x") && gpad.contains("y") && gpad.contains("z"));
		assertTrue("Should not contain 4th parameter", !gpad.contains("rgb(x,y,z,"));
	}

	@Test
	public void testConvertObjColorDynamicRgb4Params() {
		// Test dynamic RGB color with 4 parameters (including alpha)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x");
		attrs.put("dynamicg", "y");
		attrs.put("dynamicb", "z");
		attrs.put("dynamica", "a");
		attrs.put("colorSpace", "0"); // RGB
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain rgb(", gpad.contains("rgb("));
		assertTrue("Should contain 4 parameters", gpad.contains("rgb(x,y,z,a)"));
	}

	@Test
	public void testConvertObjColorDynamicHsv3Params() {
		// Test dynamic HSV color with 3 parameters
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "v");
		attrs.put("colorSpace", "1"); // HSB/HSV
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain hsv(", gpad.contains("hsv("));
		assertTrue("Should contain h,s,v", gpad.contains("h") && gpad.contains("s") && gpad.contains("v"));
	}

	@Test
	public void testConvertObjColorDynamicHsv4Params() {
		// Test dynamic HSV color with 4 parameters
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "v");
		attrs.put("dynamica", "a");
		attrs.put("colorSpace", "1"); // HSB/HSV
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain hsv(", gpad.contains("hsv("));
		assertTrue("Should contain 4 parameters", gpad.contains("hsv(h,s,v,a)"));
	}

	@Test
	public void testConvertObjColorDynamicHsl3Params() {
		// Test dynamic HSL color with 3 parameters
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "l");
		attrs.put("colorSpace", "2"); // HSL
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain hsl(", gpad.contains("hsl("));
		assertTrue("Should contain h,s,l", gpad.contains("h") && gpad.contains("s") && gpad.contains("l"));
	}

	@Test
	public void testConvertObjColorDynamicHsl4Params() {
		// Test dynamic HSL color with 4 parameters
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "l");
		attrs.put("dynamica", "a");
		attrs.put("colorSpace", "2"); // HSL
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain hsl(", gpad.contains("hsl("));
		assertTrue("Should contain 4 parameters", gpad.contains("hsl(h,s,l,a)"));
	}

	@Test
	public void testConvertObjColorDynamicWithComplexExpressions() {
		// Test dynamic color with complex expressions (should be quoted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x*255");
		attrs.put("dynamicg", "sin(y)*128+128");
		attrs.put("dynamicb", "z^2*255");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain rgb(", gpad.contains("rgb("));
		// Complex expressions should not need quotes if they don't contain special chars
		assertTrue("Should contain expressions", gpad.contains("x*255") || gpad.contains("\"x*255\""));
	}

	@Test
	public void testConvertObjColorDynamicWithExpressionsNeedingQuotes() {
		// Test dynamic color with expressions containing special characters (should be quoted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x, y");
		attrs.put("dynamicg", "a; b");
		attrs.put("dynamicb", "c d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain rgb(", gpad.contains("rgb("));
		// Expressions with special chars should be quoted
		assertTrue("Should contain quoted expressions", gpad.contains("\"x, y\"") || gpad.contains("\"a; b\""));
	}

	@Test
	public void testConvertObjColorWithFillType() {
		// Test objColor with fill type
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "1"); // HATCH
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain fill=hatch", gpad.contains("fill=hatch"));
	}

	@Test
	public void testConvertObjColorWithAllFillTypes() {
		// Test all fill types
		String[] fillTypes = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
		String[] expectedGpad = {"standard", "hatch", "crosshatch", "chessboard", "dotted", 
				"honeycomb", "brick", "weaving", "symbols", "image"};
		
		
		for (int i = 0; i < fillTypes.length; i++) {
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
			attrs.put("r", "255");
			attrs.put("g", "0");
			attrs.put("b", "0");
			attrs.put("fillType", fillTypes[i]);
			styleMap.put("objColor", attrs);
			
			String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
			if (i == 0) {
				// fillType=0 (STANDARD) is default, should be omitted
				assertTrue("fillType=0 should be omitted", !gpad.contains("fill="));
			} else {
				assertNotNull("Fill type " + fillTypes[i] + " should convert", gpad);
				assertTrue("Should contain fill=" + expectedGpad[i], gpad.contains("fill=" + expectedGpad[i]));
			}
		}
	}

	@Test
	public void testConvertObjColorWithHatchAngle() {
		// Test objColor with hatch angle (non-default)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "1");
		attrs.put("hatchAngle", "30");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain angle=30", gpad.contains("angle=30"));
	}

	@Test
	public void testConvertObjColorWithHatchAngleDefault() {
		// Test objColor with hatch angle = 45 (default, should be omitted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "1");
		attrs.put("hatchAngle", "45"); // Default value
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should not contain angle=45 (default)", !gpad.contains("angle=45"));
	}

	@Test
	public void testConvertObjColorWithHatchDistance() {
		// Test objColor with hatch distance (non-default)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "1");
		attrs.put("hatchDistance", "15");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain dist=15", gpad.contains("dist=15"));
	}

	@Test
	public void testConvertObjColorWithHatchDistanceDefault() {
		// Test objColor with hatch distance = 10 (default, should be omitted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "1");
		attrs.put("hatchDistance", "10"); // Default value
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should not contain dist=10 (default)", !gpad.contains("dist=10"));
	}

	@Test
	public void testConvertObjColorWithImage() {
		// Test objColor with image path
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "9"); // IMAGE
		attrs.put("image", "path/to/image.png");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain image=", gpad.contains("image="));
		assertTrue("Should contain path", gpad.contains("path/to/image.png"));
	}

	@Test
	public void testConvertObjColorWithImageNeedingQuotes() {
		// Test objColor with image path containing special characters (should be quoted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "9");
		attrs.put("image", "path/to/image with spaces.png");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain image=", gpad.contains("image="));
		// Path with spaces should be quoted
		assertTrue("Should contain quoted path", gpad.contains("\"path/to/image with spaces.png\""));
	}

	@Test
	public void testConvertObjColorWithFillSymbol() {
		// Test objColor with fill symbol
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "8"); // SYMBOLS
		attrs.put("fillSymbol", "$");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain symbol=", gpad.contains("symbol="));
		assertTrue("Should contain $", gpad.contains("$"));
	}

	@Test
	public void testConvertObjColorWithSpecialFillSymbol() {
		// Test objColor with fill symbol containing special characters (should be quoted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("fillType", "8");
		attrs.put("fillSymbol", "\r");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Symbol with special chars should be skipped
		assertTrue("Should not contain special symbol", !gpad.contains("symbol="));
	}

	@Test
	public void testConvertObjColorWithInverseFill() {
		// Test objColor with inverseFill=true
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("inverseFill", "true");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain inverse", gpad.contains("inverse"));
		assertTrue("Should not contain ~inverse", !gpad.contains("~inverse"));
	}

	@Test
	public void testConvertObjColorWithInverseFillFalse() {
		// Test objColor with inverseFill=false (default, should be omitted)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("inverseFill", "false");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should not contain inverse (default false)", !gpad.contains("inverse"));
	}

	@Test
	public void testConvertObjColorCombinedProperties() {
		// Test objColor with multiple properties
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x");
		attrs.put("dynamicg", "y");
		attrs.put("dynamicb", "z");
		attrs.put("colorSpace", "0");
		attrs.put("fillType", "1");
		attrs.put("hatchAngle", "30");
		attrs.put("hatchDistance", "15");
		attrs.put("inverseFill", "true");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain rgb(", gpad.contains("rgb("));
		assertTrue("Should contain fill=hatch", gpad.contains("fill=hatch"));
		assertTrue("Should contain angle=30", gpad.contains("angle=30"));
		assertTrue("Should contain dist=15", gpad.contains("dist=15"));
		assertTrue("Should contain inverse", gpad.contains("inverse"));
	}

	@Test
	public void testConvertObjColorAllProperties() {
		// Test objColor with all properties
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "v");
		attrs.put("dynamica", "a");
		attrs.put("colorSpace", "1"); // HSV
		attrs.put("fillType", "2"); // CROSSHATCHED
		attrs.put("hatchAngle", "60");
		attrs.put("hatchDistance", "20");
		attrs.put("image", "test.png");
		attrs.put("fillSymbol", "*");
		attrs.put("inverseFill", "true");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain hsv(", gpad.contains("hsv("));
		assertTrue("Should contain fill=crosshatch", gpad.contains("fill=crosshatch"));
		assertTrue("Should contain angle=60", gpad.contains("angle=60"));
		assertTrue("Should contain dist=20", gpad.contains("dist=20"));
		assertTrue("Should contain image=", gpad.contains("image="));
		assertTrue("Should contain symbol=", gpad.contains("symbol="));
		assertTrue("Should contain inverse", gpad.contains("inverse"));
	}

	@Test
	public void testConvertObjColorWithEscapeSequences() {
		// Test objColor with expressions containing escape sequences
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x\n*255");
		attrs.put("dynamicg", "y\t+128");
		attrs.put("dynamicb", "z\r/2");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain rgb(", gpad.contains("rgb("));
		// Expressions with newlines/tabs should be quoted with escape sequences
		assertTrue("Should contain escape sequences", 
				gpad.contains("\\n") || gpad.contains("\\t") || gpad.contains("\\r"));
	}

	@Test
	public void testConvertObjColorEmptyAttrs() {
		// Test objColor with empty attributes (should return empty string)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		// When attributes are empty, objColor property should not be output
		assertTrue("Should not contain objColor when attrs empty", gpad == null || !gpad.contains("objColor"));
	}

	@Test
	public void testConvertObjColorOnlyFillType() {
		// Test objColor with only fillType (no color)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("fillType", "1"); // HATCH
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		// Without color, objColor should still be output if fillType is set
		assertNotNull(gpad);
		assertTrue("Should contain fill=hatch", gpad.contains("fill=hatch"));
	}

	@Test
	public void testConvertObjColorStaticColorWithDefaultsOmitted() {
		// Test static color with all default values (should only output color)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("r", "255");
		attrs.put("g", "0");
		attrs.put("b", "0");
		attrs.put("alpha", "1.0"); // Default
		attrs.put("fillType", "0"); // Default STANDARD
		attrs.put("hatchAngle", "45"); // Default
		attrs.put("hatchDistance", "10"); // Default
		attrs.put("inverseFill", "false"); // Default
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain #FF0000", gpad.contains("#FF0000"));
		assertTrue("Should not contain fill= (default)", !gpad.contains("fill="));
		assertTrue("Should not contain angle= (default)", !gpad.contains("angle="));
		assertTrue("Should not contain dist= (default)", !gpad.contains("dist="));
		assertTrue("Should not contain inverse (default)", !gpad.contains("inverse"));
	}

	// ========== Tests for simplifyExpression (via objColor dynamic colors) ==========

	@Test
	public void testSimpleExpressionSingleLabel() {
		// Test simple expression: single label (x, y, z, a, etc.)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x");
		attrs.put("dynamicg", "y");
		attrs.put("dynamicb", "z");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Simple expressions should not be quoted
		assertTrue("Should contain rgb(x,y,z)", gpad.contains("rgb(x,y,z)"));
		assertTrue("Should not contain quotes around x", !gpad.contains("\"x\""));
		assertTrue("Should not contain quotes around y", !gpad.contains("\"y\""));
		assertTrue("Should not contain quotes around z", !gpad.contains("\"z\""));
	}

	@Test
	public void testSimpleExpressionNumber() {
		// Test simple expression: number
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "255");
		attrs.put("dynamicg", "128");
		attrs.put("dynamicb", "64");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Numbers should not be quoted
		assertTrue("Should contain rgb(255,128,64)", gpad.contains("rgb(255,128,64)"));
		assertTrue("Should not contain quotes around numbers", !gpad.contains("\"255\""));
	}

	@Test
	public void testSimpleExpressionFloat() {
		// Test simple expression: float number
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "255.5");
		attrs.put("dynamicg", "128.25");
		attrs.put("dynamicb", "64.75");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Float numbers should not be quoted
		assertTrue("Should contain float numbers", gpad.contains("255.5"));
		assertTrue("Should not contain quotes around floats", !gpad.contains("\"255.5\""));
	}

	@Test
	public void testSimpleExpressionAddition() {
		// Test simple expression: addition (x+y)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x+y");
		attrs.put("dynamicg", "a+b");
		attrs.put("dynamicb", "c+d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Addition expressions should not be quoted
		assertTrue("Should contain x+y", gpad.contains("x+y"));
		assertTrue("Should not contain quotes around x+y", !gpad.contains("\"x+y\""));
	}

	@Test
	public void testSimpleExpressionSubtraction() {
		// Test simple expression: subtraction (x-y)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x-y");
		attrs.put("dynamicg", "a-b");
		attrs.put("dynamicb", "c-d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Subtraction expressions should not be quoted
		assertTrue("Should contain x-y", gpad.contains("x-y"));
		assertTrue("Should not contain quotes around x-y", !gpad.contains("\"x-y\""));
	}

	@Test
	public void testSimpleExpressionMultiplication() {
		// Test simple expression: multiplication (x*y)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x*y");
		attrs.put("dynamicg", "a*b");
		attrs.put("dynamicb", "c*d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Multiplication expressions should not be quoted
		assertTrue("Should contain x*y", gpad.contains("x*y"));
		assertTrue("Should not contain quotes around x*y", !gpad.contains("\"x*y\""));
	}

	@Test
	public void testSimpleExpressionDivision() {
		// Test simple expression: division (x/y)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x/y");
		attrs.put("dynamicg", "a/b");
		attrs.put("dynamicb", "c/d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Division expressions should not be quoted
		assertTrue("Should contain x/y", gpad.contains("x/y"));
		assertTrue("Should not contain quotes around x/y", !gpad.contains("\"x/y\""));
	}

	@Test
	public void testSimpleExpressionPower() {
		// Test simple expression: power (x^2)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x^2");
		attrs.put("dynamicg", "y^3");
		attrs.put("dynamicb", "z^4");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Power expressions should not be quoted
		assertTrue("Should contain x^2", gpad.contains("x^2"));
		assertTrue("Should not contain quotes around x^2", !gpad.contains("\"x^2\""));
	}

	@Test
	public void testSimpleExpressionUnaryMinus() {
		// Test simple expression: unary minus (-x)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "-x");
		attrs.put("dynamicg", "-y");
		attrs.put("dynamicb", "-z");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Unary minus expressions should not be quoted
		assertTrue("Should contain -x", gpad.contains("-x"));
		assertTrue("Should not contain quotes around -x", !gpad.contains("\"-x\""));
	}

	@Test
	public void testSimpleExpressionParentheses() {
		// Test simple expression: parentheses ((x+y)*z)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "(x+y)*z");
		attrs.put("dynamicg", "(a-b)/c");
		attrs.put("dynamicb", "(d+e)^2");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Parenthesized expressions should not be quoted
		assertTrue("Should contain (x+y)*z", gpad.contains("(x+y)*z"));
		assertTrue("Should not contain quotes around (x+y)*z", !gpad.contains("\"(x+y)*z\""));
	}

	@Test
	public void testSimpleExpressionFunctionCall() {
		// Test simple expression: function call (sin(x))
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "sin(x)");
		attrs.put("dynamicg", "cos(y)");
		attrs.put("dynamicb", "tan(z)");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Function calls should not be quoted
		assertTrue("Should contain sin(x)", gpad.contains("sin(x)"));
		assertTrue("Should not contain quotes around sin(x)", !gpad.contains("\"sin(x)\""));
	}

	@Test
	public void testSimpleExpressionComplex() {
		// Test simple expression: complex expression (sin(x+y)*z^2)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "sin(x+y)*z^2");
		attrs.put("dynamicg", "cos(a-b)/c^3");
		attrs.put("dynamicb", "tan(d+e)^4");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Complex but valid expressions should not be quoted
		assertTrue("Should contain sin(x+y)*z^2", gpad.contains("sin(x+y)*z^2"));
		assertTrue("Should not contain quotes around complex expression", !gpad.contains("\"sin(x+y)*z^2\""));
	}

	@Test
	public void testComplexExpressionWithComma() {
		// Test complex expression: contains comma (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x, y");
		attrs.put("dynamicg", "a, b");
		attrs.put("dynamicb", "c, d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with comma should be quoted
		assertTrue("Should contain quoted x, y", gpad.contains("\"x, y\""));
	}

	@Test
	public void testComplexExpressionWithSemicolon() {
		// Test complex expression: contains semicolon (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x; y");
		attrs.put("dynamicg", "a; b");
		attrs.put("dynamicb", "c; d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with semicolon should be quoted
		assertTrue("Should contain quoted x; y", gpad.contains("\"x; y\""));
	}

	@Test
	public void testComplexExpressionWithRightBrace() {
		// Test complex expression: contains right brace (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x } y");
		attrs.put("dynamicg", "a } b");
		attrs.put("dynamicb", "c } d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with right brace should be quoted
		assertTrue("Should contain quoted x } y", gpad.contains("\"x } y\""));
	}

	@Test
	public void testComplexExpressionWithNewline() {
		// Test complex expression: contains newline (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x\ny");
		attrs.put("dynamicg", "a\nb");
		attrs.put("dynamicb", "c\nd");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with newline should be quoted and escaped
		assertTrue("Should contain quoted expression with \\n", gpad.contains("\"x\\ny\""));
	}

	@Test
	public void testComplexExpressionWithCarriageReturn() {
		// Test complex expression: contains carriage return (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x\ry");
		attrs.put("dynamicg", "a\rb");
		attrs.put("dynamicb", "c\rd");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with carriage return should be quoted and escaped
		assertTrue("Should contain quoted expression with \\r", gpad.contains("\"x\\ry\""));
	}

	@Test
	public void testComplexExpressionWithTab() {
		// Test complex expression: contains tab (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x\ty");
		attrs.put("dynamicg", "a\tb");
		attrs.put("dynamicb", "c\td");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with tab should be quoted but not escaped
		assertTrue("Should contain quoted but not escaped expression with <tab>", gpad.contains("\"x\ty\""));
	}

	@Test
	public void testComplexExpressionWithSpace() {
		// Test complex expression: contains space (needs quotes)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x y");
		attrs.put("dynamicg", "a b");
		attrs.put("dynamicb", "c d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with space should be quoted
		assertTrue("Should contain quoted x y", gpad.contains("\"x y\""));
	}

	@Test
	public void testComplexExpressionWithQuote() {
		// Test complex expression: contains quote (needs quotes and escaping)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x\"y");
		attrs.put("dynamicg", "a\"b");
		attrs.put("dynamicb", "c\"d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with quote should be quoted and escaped
		assertTrue("Should contain quoted expression with escaped quote", gpad.contains("\"x\\\"y\""));
	}

	@Test
	public void testComplexExpressionWithBackslash() {
		// Test complex expression: contains backslash (needs quotes and escaping)
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x\\y");
		attrs.put("dynamicg", "a\\b");
		attrs.put("dynamicb", "c\\d");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Expressions with backslash should be quoted and escaped
		assertTrue("Should contain quoted expression with escaped backslash", gpad.contains("\"x\\\\y\""));
	}

	@Test
	public void testComplexExpressionMultipleSpecialChars() {
		// Test complex expression: multiple special characters
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x, y; z } w");
		attrs.put("dynamicg", "a\nb\tc");
		attrs.put("dynamicb", "d\"e\\f");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// All special characters should be properly escaped
		assertTrue("Should contain quoted expression", gpad.contains("\"x, y; z } w\""));
		assertTrue("Should contain escaped newline", gpad.contains("\"a\\nb\tc\""));
		assertTrue("Should contain escaped quote and backslash", gpad.contains("\"d\\\"e\\\\f\""));
	}

	@Test
	public void testSimpleExpressionWithAlpha() {
		// Test simple expression with alpha channel
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "x");
		attrs.put("dynamicg", "y");
		attrs.put("dynamicb", "z");
		attrs.put("dynamica", "a");
		attrs.put("colorSpace", "0");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Simple expressions with alpha should not be quoted
		assertTrue("Should contain rgb(x,y,z,a)", gpad.contains("rgb(x,y,z,a)"));
		assertTrue("Should not contain quotes around a", !gpad.contains("\"a\""));
	}

	@Test
	public void testSimpleExpressionHSV() {
		// Test simple expression with HSV color space
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "v");
		attrs.put("colorSpace", "1");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Simple expressions should not be quoted
		assertTrue("Should contain hsv(h,s,v)", gpad.contains("hsv(h,s,v)"));
		assertTrue("Should not contain quotes", !gpad.contains("\"h\"") && !gpad.contains("\"s\"") && !gpad.contains("\"v\""));
	}

	@Test
	public void testSimpleExpressionHSL() {
		// Test simple expression with HSL color space
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("dynamicr", "h");
		attrs.put("dynamicg", "s");
		attrs.put("dynamicb", "l");
		attrs.put("colorSpace", "2");
		styleMap.put("objColor", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		// Simple expressions should not be quoted
		assertTrue("Should contain hsl(h,s,l)", gpad.contains("hsl(h,s,l)"));
		assertTrue("Should not contain quotes", !gpad.contains("\"h\"") && !gpad.contains("\"s\"") && !gpad.contains("\"l\""));
	}
}

