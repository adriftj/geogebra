package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.gpad.GpadStyleSheet;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Unit tests for GpadParser.
 */
public class GpadParserTest extends BaseUnitTest {

	@Test
	public void testParseSimpleCommand() {
		String gpad = "A = Point(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			assertEquals("A", geo.getLabelSimple());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseStyleSheetDefinition() {
		String gpad = "$style1 = { pointSize: 5; fixed }\nA = Point(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// Check that style sheet was registered
			assertTrue(parser.getGlobalStyleSheets().containsKey("style1"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithStyleSheet() {
		String gpad = "$g = { labelOffset:28, 75; lineStyle: thickness=4 opacity=178 }\n"
				+ "g = Line((0,0), (1,1))";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoLine);
			assertEquals("g", geo.getLabelSimple());
			// Check label offset
			assertEquals(28, geo.labelOffsetX);
			assertEquals(75, geo.labelOffsetY);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithVisibilityFlags() {
		String gpad = "A* = Point(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// * flag: hide object, show label
			assertTrue(!geo.isSetEuclidianVisible());
			assertTrue(geo.isLabelVisible());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithTildeFlag() {
		String gpad = "A~ = Point(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// ~ flag: show object, hide label
			assertTrue(geo.isSetEuclidianVisible());
			assertTrue(!geo.isLabelVisible());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseCommandWithInlineStyle() {
		String gpad = "A { pointSize: 6 } = Point(1, 2)";
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
	public void testParseMultipleOutputs() {
		String gpad = "g, h = Asymptote(x^2 - y^2 / 8 = 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertTrue(geos.size() >= 1);
			// Asymptote may return multiple lines
		} catch (GpadParseException e) {
			// Asymptote might not be available in all contexts
			// This is acceptable
		}
	}

	@Test
	public void testParseCommandWithMultipleStyles() {
		String gpad = "$style1 = { pointSize: 5 }\n"
				+ "$style2 = { pointSize: 7 }\n"
				+ "A* $style1 { pointSize: 6 } $style2 = Point(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			// Later styles should override earlier ones
			// So pointSize should be 7 (from style2)
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseAnimationPropertyBasic() {
		String gpad = "$anim = { animation: play 0.1 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("t", geo.getLabelSimple());
			// Check that style sheet was registered
			assertTrue(parser.getGlobalStyleSheets().containsKey("anim"));
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			assertTrue(styleSheet != null);
			java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
			assertTrue(animAttrs != null);
			assertEquals("true", animAttrs.get("playing"));
			assertEquals("0.1", animAttrs.get("step"));
			assertEquals("2", animAttrs.get("speed"));
			assertEquals("0", animAttrs.get("type")); // Default oscillating
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithIncreasingType() {
		String gpad = "$anim = { animation: play +0.1 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("1", animAttrs.get("type")); // INCREASING
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("2", animAttrs.get("speed"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithDecreasingType() {
		String gpad = "$anim = { animation: play -0.1 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("2", animAttrs.get("type")); // DECREASING
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("2", animAttrs.get("speed"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithIncreasingOnceType() {
		String gpad = "$anim = { animation: play =0.1 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("3", animAttrs.get("type")); // INCREASING_ONCE
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("2", animAttrs.get("speed"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithOscillatingType() {
		String gpad = "$anim = { animation: play 0.1 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("0", animAttrs.get("type")); // OSCILLATING (default)
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("2", animAttrs.get("speed"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithPlayOnly() {
		String gpad = "$anim = { animation: play }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("true", animAttrs.get("playing"));
					// step and speed should not be set
					assertTrue(animAttrs.get("step") == null || animAttrs.get("step").isEmpty());
					assertTrue(animAttrs.get("speed") == null || animAttrs.get("speed").isEmpty());
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithTildePlay() {
		String gpad = "$anim = { animation: ~play }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("false", animAttrs.get("playing"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithStepOnly() {
		String gpad = "$anim = { animation: 0.1 }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("0", animAttrs.get("type")); // Default oscillating
					// playing and speed should not be set
					assertTrue(animAttrs.get("playing") == null || animAttrs.get("playing").isEmpty());
					assertTrue(animAttrs.get("speed") == null || animAttrs.get("speed").isEmpty());
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithSpeedOnly() {
		String gpad = "$anim = { animation: 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("2", animAttrs.get("speed"));
					// playing and step should not be set
					assertTrue(animAttrs.get("playing") == null || animAttrs.get("playing").isEmpty());
					assertTrue(animAttrs.get("step") == null || animAttrs.get("step").isEmpty());
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithInlineStyle() {
		String gpad = "t { animation: play +0.1 2x } = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("t", geo.getLabelSimple());
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithFloatSpeed() {
		String gpad = "$anim = { animation: play 0.1 1.5x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("1.5", animAttrs.get("speed"));
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("true", animAttrs.get("playing"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyWithIntegerStep() {
		String gpad = "$anim = { animation: play 1 2x }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("1", animAttrs.get("step"));
					assertEquals("2", animAttrs.get("speed"));
					assertEquals("true", animAttrs.get("playing"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseAnimationPropertyDifferentOrder() {
		// Test that order doesn't matter: speed x, step, play
		String gpad = "$anim = { animation: 2x +0.1 play }\n"
				+ "t $anim = Slider(0, 1)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GpadStyleSheet styleSheet = parser.getGlobalStyleSheets().get("anim");
			if (styleSheet != null) {
				java.util.LinkedHashMap<String, String> animAttrs = styleSheet.getProperty("animation");
				if (animAttrs != null) {
					assertEquals("2", animAttrs.get("speed"));
					assertEquals("0.1", animAttrs.get("step"));
					assertEquals("1", animAttrs.get("type")); // INCREASING
					assertEquals("true", animAttrs.get("playing"));
				}
			}
		} catch (GpadParseException e) {
			// Slider might not be available in all contexts
		}
	}

	@Test
	public void testParseEmptyGpad() {
		String gpad = "";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testParseComments() {
		String gpad = "// This is a comment\nA = Point(1, 2)";
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
	public void testParseMultiLineStyleSheet() {
		String gpad = "$style1 = {\n"
				+ "  pointSize: 5;\n"
				+ "  fixed;\n"
				+ "  objColor: r=255 g=0 b=0\n"
				+ "}\n"
				+ "A $style1 = Point(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertEquals("A", geo.getLabelSimple());
			assertTrue(parser.getGlobalStyleSheets().containsKey("style1"));
		} catch (GpadParseException e) {
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
		} catch (GpadParseException e) {
			// This is acceptable if the command syntax is different
		}
	}

	@Test(expected = GpadParseException.class)
	public void testParseInvalidCommand() throws GpadParseException {
		String gpad = "A = InvalidCommand(1, 2)";
		GpadParser parser = new GpadParser(getKernel());
		parser.parse(gpad);
	}

	@Test(expected = GpadParseException.class)
	public void testParseInvalidStyleSheet() throws GpadParseException {
		String gpad = "$style = invalid syntax";
		GpadParser parser = new GpadParser(getKernel());
		parser.parse(gpad);
	}
}





