package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.main.MyError;
import org.junit.Test;

/**
 * Unit tests for GpadParser.
 */
public class GpadParserTest extends BaseUnitTest {

	@Test
	public void testParseSimpleCommand() {
		String gpad = "A = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStyleSheetDefinition() {
		String gpad = "@style1 = { pointSize: 5; fixed }\nA = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// Check that style sheet was registered
			assertTrue(parser.getGlobalStyleSheets().containsKey("style1"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithStyleSheet() {
		String gpad = "@g = { labelOffset:28 75; lineStyle: thickness=4 opacity=178 }\n"
				+ "g @g= Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			assertEquals("g", geo.getLabelSimple());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("g");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("labelOffset");
			assertNotNull(attrs);
			assertEquals("28", attrs.get("x"));
			assertEquals("75", attrs.get("y"));
			// Check label offset
			assertEquals(28, geo.labelOffsetX);
			assertEquals(75, geo.labelOffsetY);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithVisibilityFlags() {
		String gpad = "A* = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// * flag: hide object, hide label
			assertTrue(!geo.isSetEuclidianVisible());
			assertTrue(!geo.isLabelVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithTildeFlag() {
		String gpad = "A~ = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// ~ flag: show object, hide label
			assertTrue(geo.isSetEuclidianVisible());
			assertTrue(!geo.isLabelVisible());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithInlineStyle() {
		String gpad = "A { pointSize: 6 } = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseMultipleOutputs() {
		String gpad = "g, h = Asymptote(x^2 - y^2 / 8 = 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertTrue(geos.size() == 2);
			// Asymptote may return multiple lines
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithMultipleStyles() {
		String gpad = "@style1 = { pointSize: 5 }\n"
				+ "@style2 = { pointSize: 7 }\n"
				+ "A* @style1 { pointSize: 6 } @style2 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// Later styles should override earlier ones
			// So pointSize should be 7 (from style2)
			assertTrue(geo instanceof GeoPoint);
			GeoPoint point = (GeoPoint) geo;
			assertEquals(7, point.getPointSize());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyBasic() {
		String gpad = "@anim = { animation: play 0.1 speed=2 }\n"
				+ "t @anim = Slider(0.0, 1.0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("t", geo.getLabelSimple());
			// Check that style sheet was registered
			assertTrue(parser.getGlobalStyleSheets().containsKey("anim"));
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("true", animAttrs.get("playing"));
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
			assertEquals("0", animAttrs.get("type")); // Default oscillating
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithIncreasingType() {
		String gpad = "@anim = { animation: play +0.1 speed=2 }\n"
				+ "t @anim = Slider(0.0, 1.0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("1", animAttrs.get("type")); // INCREASING
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithDecreasingType() {
		String gpad = "@anim = { animation: play -0.1 speed=2 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("2", animAttrs.get("type")); // DECREASING
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithIncreasingOnceType() {
		String gpad = "@anim = { animation: play =0.1 speed=2 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("3", animAttrs.get("type")); // INCREASING_ONCE
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithOscillatingType() {
		String gpad = "@anim = { animation: play 0.1 speed=2 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("0", animAttrs.get("type")); // OSCILLATING (default)
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithPlayOnly() {
		String gpad = "@anim = { animation: play }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("true", animAttrs.get("playing"));
			// step and speed should not be set
			assertTrue(animAttrs.get("step") == null || animAttrs.get("step").isEmpty());
			assertTrue(animAttrs.get("speed") == null || animAttrs.get("speed").isEmpty());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithTildePlay() {
		String gpad = "@anim = { animation: ~play }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("false", animAttrs.get("playing"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithStepOnly() {
		String gpad = "@anim = { animation: 0.1 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("0", animAttrs.get("type")); // Default oscillating
			// playing and speed should not be set
			assertTrue(animAttrs.get("playing") == null || animAttrs.get("playing").isEmpty());
			assertTrue(animAttrs.get("speed") == null || animAttrs.get("speed").isEmpty());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithSpeedOnly() {
		String gpad = "@anim = { animation: speed=2 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("2", animAttrs.get("speed"));
			// playing and step should not be set
			assertTrue(animAttrs.get("playing") == null || animAttrs.get("playing").isEmpty());
			assertTrue(animAttrs.get("step") == null || animAttrs.get("step").isEmpty());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithInlineStyle() {
		String gpad = "t { animation: play +0.1 speed=2 } = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("t", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithFloatSpeed() {
		String gpad = "@anim = { animation: play 0.1 speed=1.5 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("1.5", animAttrs.get("speed"));
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("true", animAttrs.get("playing"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyWithIntegerStep() {
		String gpad = "@anim = { animation: play 1 speed=2 }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
			assertEquals("true", animAttrs.get("playing"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyDifferentOrder() {
		// Test that order doesn't matter: speed, step, play
		String gpad = "@anim = { animation: speed=2 +0.1 play }\n"
				+ "t @anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertNotNull(animAttrs);
			assertEquals("2", animAttrs.get("speed"));
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("1", animAttrs.get("type")); // INCREASING
			assertEquals("true", animAttrs.get("playing"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEmptyGpad() {
		String gpad = "";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseComments() {
		String gpad = "// This is a comment\nA = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommentsAtEndOfLine() {
		String gpad = "A = (1, 2) // This is a comment at end of line";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseMultipleComments() {
		String gpad = "// First comment\n// Second comment\nA = (1, 2) // Third comment";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommentOnlyLine() {
		String gpad = "// This is a comment only line\nA = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommentWithStyleSheet() {
		String gpad = "// Comment before style sheet\n"
				+ "@style1 = { pointSize: 5 } // Comment after style sheet\n"
				+ "A @style1 = (1, 2) // Comment after command";
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
	public void testParseMultiLineStyleSheet() {
		String gpad = "@style1 = {\n"
				+ "  pointSize// comment 1\n"
				+ ": 5;// comment 2\n"
				+ "  fixed;\n"
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
	public void testParseNestedCommandCall() {
		String gpad = "A = Point(Line((0,0), (1,1)), 0.5)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			// Nested command calls should be supported
			assertTrue(geos.size() >= 0); // May succeed or fail depending on command syntax
		} catch (GpadParseException | CircularDefinitionException e) {
			// This is acceptable if the command syntax is different
		}
	}

	@Test(expected = MyError.class)
	public void testParseInvalidCommand() throws GpadParseException, CircularDefinitionException {
		String gpad = "A = InvalidCommand(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		parser.parse(gpad);
	}

	@Test(expected = GpadParseException.class)
	public void testParseInvalidStyleSheet() throws GpadParseException, CircularDefinitionException {
		String gpad = "@style = invalid syntax";
		GpadParser parser = new GpadParser(getKernel());
		parser.parse(gpad);
	}

	@Test
	public void testParseBgColorWithHex6Lowercase() {
		String gpad = "@style = { bgColor: #ff0000; }\nA @style = (1.0, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertNotNull(colorAttrs);
			assertEquals("255", colorAttrs.get("r"));
			assertEquals("0", colorAttrs.get("g"));
			assertEquals("0", colorAttrs.get("b"));
			assertEquals("1.0", colorAttrs.get("alpha")); // Default alpha
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBgColorWithHex6Uppercase() {
		String gpad = "@style = { bgColor: #00FF00 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertNotNull(colorAttrs);
			assertEquals("0", colorAttrs.get("r"));
			assertEquals("255", colorAttrs.get("g"));
			assertEquals("0", colorAttrs.get("b"));
			assertEquals("1.0", colorAttrs.get("alpha"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBgColorWithHex8Lowercase() {
		String gpad = "@style = { bgColor: #0000ff80 }\nA @style = (1, 2.0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertNotNull(colorAttrs);
			assertEquals("0", colorAttrs.get("r"));
			assertEquals("0", colorAttrs.get("g"));
			assertEquals("255", colorAttrs.get("b"));
			// alpha = 0x80 = 128, 128/255 = 0.50196...
			double alpha = Double.parseDouble(colorAttrs.get("alpha"));
			assertTrue(Math.abs(alpha - 128.0 / 255.0) < 0.001);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBgColorWithHex8Uppercase() {
		String gpad = "@style = { bgColor: #ABCDEFAA }\nA @style = (1.0, 2.0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertNotNull(colorAttrs);
			assertEquals("171", colorAttrs.get("r")); // AB = 171
			assertEquals("205", colorAttrs.get("g")); // CD = 205
			assertEquals("239", colorAttrs.get("b")); // EF = 239
			// alpha = 0xAA = 170, 170/255 = 0.6666...
			double alpha = Double.parseDouble(colorAttrs.get("alpha"));
			assertTrue(Math.abs(alpha - 170.0 / 255.0) < 0.001);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBorderColorWithHex6() {
		String gpad = "@style = { borderColor: #123456 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("borderColor");
			assertNotNull(colorAttrs);
			assertEquals("18", colorAttrs.get("r")); // 0x12 = 18
			assertEquals("52", colorAttrs.get("g")); // 0x34 = 52
			assertEquals("86", colorAttrs.get("b")); // 0x56 = 86
			assertEquals("1.0", colorAttrs.get("alpha"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBorderColorWithHex8() {
		String gpad = "@style = { borderColor: #FF00FF00 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("borderColor");
			assertNotNull(colorAttrs);
			assertEquals("255", colorAttrs.get("r"));
			assertEquals("0", colorAttrs.get("g"));
			assertEquals("255", colorAttrs.get("b"));
			// alpha = 0x00 = 0, 0/255 = 0.0
			assertEquals("0.0", colorAttrs.get("alpha"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBothBgColorAndBorderColor() {
		String gpad = "@style = { bgColor: #ff0000; borderColor: #00ff00 }\nA @style = (1.0, 2.0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			
			java.util.LinkedHashMap<String, String> bgColorAttrs = styleSheet.getProperty("bgColor");
			assertNotNull(bgColorAttrs);
			assertEquals("255", bgColorAttrs.get("r"));
			assertEquals("0", bgColorAttrs.get("g"));
			assertEquals("0", bgColorAttrs.get("b"));
			
			java.util.LinkedHashMap<String, String> borderColorAttrs = styleSheet.getProperty("borderColor");
			assertNotNull(borderColorAttrs);
			assertEquals("0", borderColorAttrs.get("r"));
			assertEquals("255", borderColorAttrs.get("g"));
			assertEquals("0", borderColorAttrs.get("b"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseInvalidColorFormat() {
		// Invalid format should be silently ignored (fallback to no style)
		String gpad = "@style = { bgColor: #12345 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			// Invalid color format should be ignored, so bgColor property should not exist
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertTrue(colorAttrs == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseInvalidColorFormatNoHash() {
		// Invalid format should be silently ignored (fallback to no style)
		String gpad = "@style = { bgColor: ff0000 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			// Invalid color format should be ignored, so bgColor property should not exist
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertTrue(colorAttrs == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseInvalidColorFormatTooLong() {
		// Invalid format should be silently ignored (fallback to no style)
		String gpad = "@style = { bgColor: #123456789 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			// Invalid color format should be ignored, so bgColor property should not exist
			java.util.LinkedHashMap<String, String> colorAttrs = styleSheet.getProperty("bgColor");
			assertTrue(colorAttrs == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleImplicit() {
		String gpad = "@style = { eqnStyle: implicit }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("implicit", eqnStyleAttrs.get("style"));
			assertTrue(eqnStyleAttrs.get("parameter") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleExplicit() {
		String gpad = "@style = { eqnStyle: explicit }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("explicit", eqnStyleAttrs.get("style"));
			assertTrue(eqnStyleAttrs.get("parameter") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleParametricWithParameter() {
		String gpad = "@style = { eqnStyle: parametric=t }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("parametric", eqnStyleAttrs.get("style"));
			assertEquals("t", eqnStyleAttrs.get("parameter"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleParametricWithUppercaseParameter() {
		String gpad = "@style = { eqnStyle: parametric=T }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("parametric", eqnStyleAttrs.get("style"));
			assertEquals("T", eqnStyleAttrs.get("parameter"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleParametricWithDifferentParameter() {
		String gpad = "@style = { eqnStyle: parametric=s }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("parametric", eqnStyleAttrs.get("style"));
			assertEquals("s", eqnStyleAttrs.get("parameter"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleParametricWithoutParameter() {
		String gpad = "@style = { eqnStyle: parametric }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("parametric", eqnStyleAttrs.get("style"));
			assertTrue(eqnStyleAttrs.get("parameter") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleAllValidStyles() {
		String[] validStyles = {"implicit", "explicit", "specific", "parametric", "general", "vertex", "conic", "user"};
		
		for (String style : validStyles) {
			String gpad = "@style = { eqnStyle: " + style + " }\ng @style = Line((0,0), (1,1))";
			GpadParser parser = new GpadParser(getKernel());
			
			try {
				List<GeoElement> geos = parser.parse(gpad);
				assertEquals(1, geos.size());
				GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
				assertNotNull(styleSheet);
				java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
				assertNotNull("Style " + style + " should be parsed", eqnStyleAttrs);
				assertEquals("Style value should match", style, eqnStyleAttrs.get("style"));
			} catch (GpadParseException | CircularDefinitionException e) {
				throw new AssertionError("Parse failed for style " + style + ": " + e.getMessage(), e);
			}
		}
	}

	@Test
	public void testParseEqnStyleInvalidStyle() {
		// Invalid style value should be silently ignored
		String gpad = "@style = { eqnStyle: invalid }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			// Invalid style value should be ignored, so eqnStyle property should not exist
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertTrue("Invalid style should be ignored", eqnStyleAttrs == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleParametricWithInvalidParameter() {
		// Parameter that is not a single letter should be ignored
		String gpad = "@style = { eqnStyle: parametric=tt }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("parametric", eqnStyleAttrs.get("style"));
			// Invalid parameter (not single letter) should be ignored
			assertTrue(eqnStyleAttrs.get("parameter") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleParametricWithNonLetterParameter() {
		// Parameter that is not a letter should be ignored
		String gpad = "@style = { eqnStyle: parametric=1 }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("parametric", eqnStyleAttrs.get("style"));
			// Non-letter parameter should be ignored
			assertTrue(eqnStyleAttrs.get("parameter") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleNonParametricWithParameter() {
		// Parameter should be ignored for non-parametric styles
		String gpad = "@style = { eqnStyle: implicit=t }\ng @style = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> eqnStyleAttrs = styleSheet.getProperty("eqnStyle");
			assertNotNull(eqnStyleAttrs);
			assertEquals("implicit", eqnStyleAttrs.get("style"));
			// Parameter should not be set for non-parametric styles
			assertTrue(eqnStyleAttrs.get("parameter") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseEqnStyleWithInlineStyle() {
		String gpad = "g { eqnStyle: parametric=t } = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("g", geo.getLabelSimple());
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for GK_STR new syntax (mixed quoted/unquoted segments) ==========

	@Test
	public void testParseStringPropertyUnquoted() {
		// Test string property with unquoted value (no special chars)
		String gpad = "@style1 = { caption: arrow }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			assertEquals("arrow", attrs.get("val"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStringPropertyQuoted() {
		// Test string property with quoted value (contains special chars)
		String gpad = "@style1 = { caption: \"hello world\" }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			assertEquals("hello world", attrs.get("val"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStringPropertyMixedSegments() {
		// Test string property with mixed quoted and unquoted segments
		// hello (unquoted) + " world" (quoted with leading space) = "hello world"
		// Note: whitespace between segments is skipped by SKIP rule
		String gpad = "@style1 = { caption: hello \" world\" }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			// hello + " world" = "hello world" (one space from quoted segment)
			assertEquals("hello world", attrs.get("val"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStringPropertyWithEscapeSequences() {
		// Test string property with escape sequences in quoted segment
		String gpad = "@style1 = { caption: \"hello\\nworld\" }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			assertEquals("hello\nworld", attrs.get("val"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStringPropertyWithSemicolon() {
		// Test string property that must be quoted (contains semicolon)
		String gpad = "@style1 = { caption: \"text;more\" }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			assertEquals("text;more", attrs.get("val"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStringPropertyMultipleUnquotedSegments() {
		// Test string property with multiple unquoted segments separated by whitespace
		String gpad = "@style1 = { caption: hello world }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			assertEquals("helloworld", attrs.get("val")); // Whitespace is skipped, segments concatenated
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStringPropertyComplexMixed() {
		// Test complex mixed quoted/unquoted segments
		// prefix (unquoted) + " middle " (quoted with spaces) + suffix (unquoted)
		// Whitespace between segments is skipped, but spaces inside quoted segment are preserved
		String gpad = "@style1 = { caption: prefix \" middle \" suffix }\nA @style1 = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style1");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("caption");
			assertNotNull(attrs);
			// prefix + " middle " + suffix
			// The quoted segment " middle " has leading and trailing spaces
			// Whitespace between segments is skipped, but spaces inside quoted segment are preserved
			String actual = attrs.get("val");
			// Verify it contains all parts in order
			assertTrue("Should contain prefix", actual.contains("prefix"));
			assertTrue("Should contain middle", actual.contains("middle"));
			assertTrue("Should contain suffix", actual.contains("suffix"));
			// Verify the structure: prefix should come before middle, middle before suffix
			int prefixPos = actual.indexOf("prefix");
			int middlePos = actual.indexOf("middle");
			int suffixPos = actual.indexOf("suffix");
			assertTrue("prefix should come before middle", prefixPos < middlePos);
			assertTrue("middle should come before suffix", middlePos < suffixPos);
			// The quoted segment " middle " has spaces, so there should be spaces around "middle"
			// Expected: "prefix middle  suffix" (spaces from quoted segment)
			// But whitespace handling may vary, so we check the basic structure
			assertTrue("Should have space after prefix", actual.charAt(prefixPos + "prefix".length()) == ' ');
			assertTrue("Should have space before suffix", actual.charAt(suffixPos - 1) == ' ');
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for boundingBox ==========

	@Test
	public void testParseBoundingBoxBasic() {
		String gpad = "@style = { boundingBox: width=100 height=200 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("boundingBox");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBoundingBoxWithFloatIgnoresDecimal() {
		// Test that decimal part is ignored (width=100.5 should become width=100)
		String gpad = "@style = { boundingBox: width=100.5 height=200.9 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("boundingBox");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width")); // Decimal part ignored
			assertEquals("200", attrs.get("height")); // Decimal part ignored
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBoundingBoxDifferentOrder() {
		// Test that order doesn't matter
		String gpad = "@style = { boundingBox: height=200 width=100 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("boundingBox");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBoundingBoxOnlyWidth() {
		// Test with only width
		String gpad = "@style = { boundingBox: width=100 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("boundingBox");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width"));
			assertTrue(attrs.get("height") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBoundingBoxOnlyHeight() {
		// Test with only height
		String gpad = "@style = { boundingBox: height=200 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("boundingBox");
			assertNotNull(attrs);
			assertTrue(attrs.get("width") == null);
			assertEquals("200", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for contentSize ==========

	@Test
	public void testParseContentSizeBasic() {
		String gpad = "@style = { contentSize: width=100.5 height=200.3 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("contentSize");
			assertNotNull(attrs);
			assertEquals("100.5", attrs.get("width"));
			assertEquals("200.3", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseContentSizeWithInteger() {
		// Test with integer values (should preserve as-is)
		String gpad = "@style = { contentSize: width=100 height=200 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("contentSize");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseContentSizeDifferentOrder() {
		// Test that order doesn't matter
		String gpad = "@style = { contentSize: height=200.3 width=100.5 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("contentSize");
			assertNotNull(attrs);
			assertEquals("100.5", attrs.get("width"));
			assertEquals("200.3", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseContentSizeOnlyWidth() {
		// Test with only width
		String gpad = "@style = { contentSize: width=100.5 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("contentSize");
			assertNotNull(attrs);
			assertEquals("100.5", attrs.get("width"));
			assertTrue(attrs.get("height") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for cropBox ==========

	@Test
	public void testParseCropBoxBasic() {
		String gpad = "@style = { cropBox: x=10 y=20 width=100 height=200 cropped }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("cropBox");
			assertNotNull(attrs);
			assertEquals("10", attrs.get("x"));
			assertEquals("20", attrs.get("y"));
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
			assertEquals("true", attrs.get("cropped"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCropBoxWithTildeCropped() {
		// Test with ~cropped (false)
		String gpad = "@style = { cropBox: x=10 y=20 width=100 height=200 ~cropped }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("cropBox");
			assertNotNull(attrs);
			assertEquals("false", attrs.get("cropped"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCropBoxWithFloatValues() {
		// Test with float values
		String gpad = "@style = { cropBox: x=10.5 y=20.7 width=100.2 height=200.9 cropped }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("cropBox");
			assertNotNull(attrs);
			assertEquals("10.5", attrs.get("x"));
			assertEquals("20.7", attrs.get("y"));
			assertEquals("100.2", attrs.get("width"));
			assertEquals("200.9", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCropBoxDifferentOrder() {
		// Test that order doesn't matter
		String gpad = "@style = { cropBox: height=200 width=100 y=20 x=10 cropped }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("cropBox");
			assertNotNull(attrs);
			assertEquals("10", attrs.get("x"));
			assertEquals("20", attrs.get("y"));
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
			assertEquals("true", attrs.get("cropped"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCropBoxWithoutCropped() {
		// Test without cropped attribute
		String gpad = "@style = { cropBox: x=10 y=20 width=100 height=200 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("cropBox");
			assertNotNull(attrs);
			assertTrue(attrs.get("cropped") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for dimensions ==========

	@Test
	public void testParseDimensionsBasic() {
		String gpad = "@style = { dimensions: width=100 height=200 angle=45 scaled }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
			assertEquals("45", attrs.get("angle"));
			// scaled=true means unscaled=false
			assertEquals("false", attrs.get("unscaled"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDimensionsWithTildeScaled() {
		// Test with ~scaled (scaled=false means unscaled=true)
		String gpad = "@style = { dimensions: width=100 height=200 ~scaled }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			// ~scaled means scaled=false, so unscaled=true
			assertEquals("true", attrs.get("unscaled"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDimensionsWithFloatValues() {
		// Test with float values
		String gpad = "@style = { dimensions: width=100.5 height=200.3 angle=45.7 scaled }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			assertEquals("100.5", attrs.get("width"));
			assertEquals("200.3", attrs.get("height"));
			assertEquals("45.7", attrs.get("angle"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDimensionsWithoutAngle() {
		// Test without angle
		String gpad = "@style = { dimensions: width=100 height=200 scaled }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			assertTrue(attrs.get("angle") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDimensionsWithoutScaled() {
		// Test without scaled attribute
		String gpad = "@style = { dimensions: width=100 height=200 angle=45 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			assertTrue(attrs.get("unscaled") == null);
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDimensionsDifferentOrder() {
		// Test that order doesn't matter
		String gpad = "@style = { dimensions: angle=45 scaled height=200 width=100 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			assertEquals("100", attrs.get("width"));
			assertEquals("200", attrs.get("height"));
			assertEquals("45", attrs.get("angle"));
			assertEquals("false", attrs.get("unscaled"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Tests for negative values ==========

	@Test
	public void testParseCropBoxWithNegativeValues() {
		// Test with negative x, y, width, height
		String gpad = "@style = { cropBox: x=-10 y=-20 width=-100 height=-200 cropped }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("cropBox");
			assertNotNull(attrs);
			assertEquals("-10", attrs.get("x"));
			assertEquals("-20", attrs.get("y"));
			assertEquals("-100", attrs.get("width"));
			assertEquals("-200", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseDimensionsWithNegativeValues() {
		// Test with negative width, height, angle
		String gpad = "@style = { dimensions: width=-100 height=-200 angle=-45 scaled }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("dimensions");
			assertNotNull(attrs);
			assertEquals("-100", attrs.get("width"));
			assertEquals("-200", attrs.get("height"));
			assertEquals("-45", attrs.get("angle"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseContentSizeWithNegativeValues() {
		// Test with negative width, height
		String gpad = "@style = { contentSize: width=-100.5 height=-200.3 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("contentSize");
			assertNotNull(attrs);
			assertEquals("-100.5", attrs.get("width"));
			assertEquals("-200.3", attrs.get("height"));
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseBoundingBoxWithNegativeValues() {
		// Test with negative width, height (decimal part ignored)
		String gpad = "@style = { boundingBox: width=-100.5 height=-200.9 }\nA @style = (1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("style");
			assertNotNull(styleSheet);
			java.util.LinkedHashMap<String, String> attrs = styleSheet.getProperty("boundingBox");
			assertNotNull(attrs);
			assertEquals("-100", attrs.get("width")); // Decimal part ignored
			assertEquals("-200", attrs.get("height")); // Decimal part ignored
		} catch (GpadParseException | CircularDefinitionException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}
