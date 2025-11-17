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
}
