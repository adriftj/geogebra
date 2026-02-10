package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;

import org.geogebra.common.kernel.geos.GeoAngle;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.junit.Test;

/**
 * Unit tests for slider GPAD syntax.
 * Tests slider property parsing, XML to GPAD conversion, and geometry object creation.
 */
public class SliderGpadTest extends BaseUnitTest {

	// ==================== GPAD to Geometry Object Tests ====================

	@Test
	public void testCreateSliderWithMinMax() {
		// Use Slider command to create a GeoNumeric slider with default value 1
		// Note: Slider(min, max) creates a slider, then we apply slider style to configure it
		String gpad = "@style = { slider: min=0 max=10 width=200 }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify slider properties
			assertEquals(0.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
			assertEquals(200.0, num.getSliderWidth(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithXYCoordinates() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=0 max=10 width=200 x=100 y=150 }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify slider properties
			assertEquals(0.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
			assertEquals(200.0, num.getSliderWidth(), 1e-10);
			assertEquals(100.0, num.getSliderX(), 1e-10);
			assertEquals(150.0, num.getSliderY(), 1e-10);
			// When x/y are set in slider, absoluteScreenLocation should be false
			assertTrue(!num.isAbsoluteScreenLocActive());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithScreenCoordinates() {
		// Use Slider command to create a GeoNumeric slider
		// Note: slider's @screen is independent from object-level @screen
		String gpad = "@style = { slider: min=0 max=10 width=200 @screen x=100 y=200 }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify slider properties
			assertEquals(0.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
			assertEquals(200.0, num.getSliderWidth(), 1e-10);
			// When @screen is set in slider, absoluteScreenLocation should be true
			assertTrue(num.isAbsoluteScreenLocActive());
			// Coordinates should come from slider's x/y
			assertEquals(100.0, num.getAbsoluteScreenLocX(), 1e-10);
			assertEquals(200.0, num.getAbsoluteScreenLocY(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithVertical() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=0 max=10 width=200 vertical }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify slider is vertical (horizontal = false)
			assertTrue(!num.isSliderHorizontal());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithAlgebra() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=0 max=10 width=200 algebra }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify algebra view visibility
			assertTrue(num.isAVSliderOrCheckboxVisible());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithConstant() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=0 max=10 width=200 constant }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify arbitrary constant
			assertTrue(getKernel().getConstruction().getUnclaimedArbitraryConstants().contains(num));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithFixed() {
		// Use Slider command to create a GeoNumeric slider
		// Note: slider's fixed is independent from object-level fixed
		String gpad = "@style = { slider: min=0 max=10 width=200 fixed }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify fixed property from slider
			assertTrue(num.isLockedPosition());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithExpressionMinMax() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=-5 max=5+5 width=200 }\n"
				+ "a @style = Slider(-5, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify slider properties with expressions
			assertEquals(-5.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithAllProperties() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=0 max=10 width=200 x=100 y=150 vertical algebra constant }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify all properties
			assertEquals(0.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
			assertEquals(200.0, num.getSliderWidth(), 1e-10);
			assertEquals(100.0, num.getSliderX(), 1e-10);
			assertEquals(150.0, num.getSliderY(), 1e-10);
			assertTrue(!num.isSliderHorizontal());
			assertTrue(num.isAVSliderOrCheckboxVisible());
			assertTrue(getKernel().getConstruction().getUnclaimedArbitraryConstants().contains(num));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithTildeFlags() {
		// Use Slider command to create a GeoNumeric slider
		String gpad = "@style = { slider: min=0 max=10 width=200 ~vertical ~algebra ~constant }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify boolean flags are cleared
			assertTrue(num.isSliderHorizontal()); // ~vertical means horizontal (default)
			assertTrue(!num.isAVSliderOrCheckboxVisible()); // ~algebra means false
			assertTrue(!getKernel().getConstruction().getUnclaimedArbitraryConstants().contains(num)); // ~constant means false
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderWithScreenAndFixed() {
		// Use Slider command to create a GeoNumeric slider
		// Note: slider's @screen and fixed are independent from object-level properties
		String gpad = "@style = { slider: min=0 max=10 width=200 @screen x=100 y=200 fixed }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify fixed and screen coordinates from slider properties
			assertTrue(num.isLockedPosition());
			assertTrue(num.isAbsoluteScreenLocActive());
			assertEquals(100.0, num.getAbsoluteScreenLocX(), 1e-10);
			assertEquals(200.0, num.getAbsoluteScreenLocY(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== XML to GPAD Conversion Tests ====================

	@Test
	public void testConvertSliderMinMaxWidth() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		assertTrue("Should contain min=0", gpad.contains("min=0"));
		assertTrue("Should contain max=10", gpad.contains("max=10"));
		assertTrue("Should contain width=200", gpad.contains("width=200"));
	}

	@Test
	public void testConvertSliderWithXY() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		sliderAttrs.put("x", "100");
		sliderAttrs.put("y", "150");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		assertTrue("Should contain x=100", gpad.contains("x=100"));
		assertTrue("Should contain y=150", gpad.contains("y=150"));
	}


	@Test
	public void testConvertSliderWithVertical() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		sliderAttrs.put("horizontal", "false"); // vertical = true
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		assertTrue("Should contain vertical", gpad.contains("vertical"));
	}

	@Test
	public void testConvertSliderWithAlgebra() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		sliderAttrs.put("showAlgebra", "true");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		assertTrue("Should contain algebra", gpad.contains("algebra"));
	}

	@Test
	public void testConvertSliderWithConstant() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		sliderAttrs.put("arbitraryConstant", "true");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		assertTrue("Should contain constant", gpad.contains("constant"));
	}

	@Test
	public void testConvertSliderWithExpressionMinMax() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "-5");
		sliderAttrs.put("max", "a+5");
		sliderAttrs.put("width", "200");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		assertTrue("Should contain min=-5", gpad.contains("min=-5"));
		assertTrue("Should contain max expression", gpad.contains("max="));
	}

	@Test
	public void testConvertSliderWithFixed() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		sliderAttrs.put("fixed", "true");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		// fixed should be in slider output (independent property)
		String sliderPart = gpad.substring(gpad.indexOf("slider:"));
		assertTrue("Should contain fixed in slider", sliderPart.contains("fixed"));
	}

	@Test
	public void testConvertSliderWithScreen() {
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> sliderAttrs = new LinkedHashMap<>();
		sliderAttrs.put("min", "0");
		sliderAttrs.put("max", "10");
		sliderAttrs.put("width", "200");
		sliderAttrs.put("absoluteScreenLocation", "true");
		sliderAttrs.put("x", "100");
		sliderAttrs.put("y", "200");
		styleMap.put("slider", sliderAttrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slider", gpad.contains("slider:"));
		// @screen should be in slider output (independent property)
		String sliderPart = gpad.substring(gpad.indexOf("slider:"));
		assertTrue("Should contain @screen in slider", sliderPart.contains("@screen"));
		assertTrue("Should contain x=100 in slider", sliderPart.contains("x=100"));
		assertTrue("Should contain y=200 in slider", sliderPart.contains("y=200"));
	}

	// ==================== Test Number Expression with Slider Style ====================

	@Test
	public void testCreateSliderFromNumberExpression() {
		// Test: Can we create a slider by using just a number expression with slider style?
		// This tests if setting slider: min/max/width in style can automatically
		// convert a plain GeoNumeric (created from number expression) into a slider
		String gpad = "@style = { slider: min=0 max=10 width=200 }\n"
				+ "a @style = 1";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify that slider properties are set
			// If slider style works, these should be set even though we used "1" instead of Slider()
			assertEquals(0.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
			assertEquals(200.0, num.getSliderWidth(), 1e-10);
			
			// Verify that the object is actually a slider (has valid interval)
			// Note: isSliderable() checks if object can be a slider
			// For independent GeoNumeric with valid intervals, this should be true
			assertTrue("Number expression with slider style should create a slider", 
					num.isSliderable());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderFromNumberExpressionWithShow() {
		// Test: Can we create a visible slider by using a number expression with 
		// slider style and show style?
		// This tests if setting slider: min/max and show: object label can 
		// convert a plain GeoNumeric (created from number expression) into a visible slider
		String gpad = "@style = { slider: min=1 max=3 width=200; show: object label }\n"
				+ "a @style = 2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertNotNull("Parse should return a non-null list", geos);
			assertTrue("Should create at least one GeoElement", geos.size() >= 1);
			GeoElement geo = geos.get(0);
			assertNotNull("GeoElement should not be null", geo);
			assertTrue("Should be a GeoNumeric, but got: " + geo.getClass().getSimpleName(), 
					geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify label
			assertEquals("a", num.getLabelSimple());
			
			// Verify value
			assertEquals(2.0, num.getValue(), 1e-10);
			
			// Verify that slider properties are set
			assertEquals(1.0, num.getIntervalMin(), 1e-10);
			assertEquals(3.0, num.getIntervalMax(), 1e-10);
			
			// Verify that the object is visible in Euclidian view
			assertTrue("Number expression with slider and show style should be visible in Euclidian view", 
					num.isEuclidianVisible());
			
			// Verify that the object is actually a slider
			// isSlider() returns true if object is independent and visible in Euclidian view
			assertTrue("Number expression with slider and show style should create a slider", 
					num.isSlider());
			
			// Verify that the object is sliderable (has valid intervals and is independent)
			assertTrue("Number expression with slider style should be sliderable", 
					num.isSliderable());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateSliderFromNumberExpressionWithHiddenShow() {
		// Test: Can we create a slider (but hidden) by using a number expression with 
		// slider style and show style that hides the object?
		// This tests if setting slider: min/max/width and visibilityFlag='*' can
		// create a slider that is not visible in Euclidian view
		String gpad = "@style = { slider: min=1 max=3 width=200; }\n"
				+ "a* @style = 2";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertNotNull("Parse should return a non-null list", geos);
			assertTrue("Should create at least one GeoElement", geos.size() >= 1);
			GeoElement geo = geos.get(0);
			assertNotNull("GeoElement should not be null", geo);
			assertTrue("Should be a GeoNumeric, but got: " + geo.getClass().getSimpleName(), 
					geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify label
			assertEquals("a", num.getLabelSimple());
			
			// Verify value
			assertEquals(2.0, num.getValue(), 1e-10);
			
			// Verify that slider properties are set
			assertEquals(1.0, num.getIntervalMin(), 1e-10);
			assertEquals(3.0, num.getIntervalMax(), 1e-10);
			assertEquals(200.0, num.getSliderWidth(), 1e-10);
			
			// Verify that the object is NOT visible in Euclidian view
			assertTrue("Number expression with slider and flag* should NOT be visible in Euclidian view", 
					!num.isEuclidianVisible());
			
			// Verify that the object is NOT a slider (because it's not visible)
			// isSlider() returns true only if object is independent AND visible in Euclidian view
			assertTrue("Number expression with slider and ~object show style should NOT create a visible slider", 
					!num.isSlider());
			
			// Verify that the object is still sliderable (has valid intervals and is independent)
			// isSliderable() only checks if object can be a slider, not if it's currently visible
			assertTrue("Number expression with slider style should be sliderable even when hidden", 
					num.isSliderable());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateAngleSliderFromAngleExpression() {
		// Test: Can we apply slider style to an angle expression?
		// This tests if setting slider: min/max/width in style can be applied
		// to a GeoAngle object created from an angle expression.
		// Note: pi/4 alone creates a GeoNumeric, not a GeoAngle. Angle(pi/4) creates
		// a dependent GeoAngle (not independent), which cannot be a slider (isSliderable()=false).
		// However, we can still verify that the slider style properties (min/max/width)
		// are correctly applied to the angle object, even if it's not independent.
		String gpad = "@style = { slider: min=0 max=2*pi width=180 }\n"
				+ "α @style = Angle(pi/4)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue("Should create a GeoAngle, but got: " + geo.getClass().getSimpleName(), 
					geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify that slider properties from style are set
			// Even though the object is dependent and not sliderable,
			// the min/max/width properties should still be applied
			assertEquals(0.0, angle.getIntervalMin(), 1e-10);
			assertEquals(2 * Math.PI, angle.getIntervalMax(), 1e-10);
			assertEquals(180.0, angle.getSliderWidth(), 1e-10);
			
			// Note: isSliderable() will return false because Angle(pi/4) creates
			// a dependent object (not independent). Only independent objects can be sliders.
			// This is expected behavior in GeoGebra.
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== GeoAngle Tests ====================

	@Test
	public void testCreateAngleSliderWithMinMax() {
		// Use Slider command to create a GeoAngle slider
		// Slider(min, max, increment, speed, width, angle=true) creates an angle slider
		String gpad = "@style = { slider: min=0 max=2*pi width=180 }\n"
				+ "α @style = Slider(0, 2*pi, 0.01, 1, 180, true)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify slider properties
			assertEquals(0.0, angle.getIntervalMin(), 1e-10);
			assertEquals(2 * Math.PI, angle.getIntervalMax(), 1e-10);
			assertEquals(180.0, angle.getSliderWidth(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateAngleSliderWithXYCoordinates() {
		// Use Slider command to create a GeoAngle slider
		String gpad = "@style = { slider: min=0 max=2*pi width=180 x=100 y=150 }\n"
				+ "α @style = Slider(0, 2*pi, 0.01, 1, 180, true)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify slider properties
			assertEquals(0.0, angle.getIntervalMin(), 1e-10);
			assertEquals(2 * Math.PI, angle.getIntervalMax(), 1e-10);
			assertEquals(180.0, angle.getSliderWidth(), 1e-10);
			assertEquals(100.0, angle.getSliderX(), 1e-10);
			assertEquals(150.0, angle.getSliderY(), 1e-10);
			assertTrue(!angle.isAbsoluteScreenLocActive());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateAngleSliderWithScreenCoordinates() {
		// Use Slider command to create a GeoAngle slider
		// Note: slider's @screen is independent from object-level @screen
		String gpad = "@style = { slider: min=0 max=2*pi width=180 @screen x=100 y=200 }\n"
				+ "α @style = Slider(0, 2*pi, 0.01, 1, 180, true)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify slider properties
			assertEquals(0.0, angle.getIntervalMin(), 1e-10);
			assertEquals(2 * Math.PI, angle.getIntervalMax(), 1e-10);
			assertEquals(180.0, angle.getSliderWidth(), 1e-10);
			// When @screen is set in slider, absoluteScreenLocation should be true
			assertTrue(angle.isAbsoluteScreenLocActive());
			// Coordinates should come from slider's x/y
			assertEquals(100.0, angle.getAbsoluteScreenLocX(), 1e-10);
			assertEquals(200.0, angle.getAbsoluteScreenLocY(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateAngleSliderWithVertical() {
		// Use Slider command to create a GeoAngle slider
		String gpad = "@style = { slider: min=0 max=2*pi width=180 vertical }\n"
				+ "α @style = Slider(0, 2*pi, 0.01, 1, 180, true)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify slider is vertical (horizontal = false)
			assertTrue(!angle.isSliderHorizontal());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateAngleSliderWithUnboundedRange() {
		// Test angle slider with unbounded range (can be < 0 or > 2π)
		// Use Slider command to create a GeoAngle slider
		String gpad = "@style = { slider: min=-pi max=3*pi width=180 }\n"
				+ "α @style = Slider(-pi, 3*pi, 0.01, 1, 180, true)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify slider properties with unbounded range
			assertEquals(-Math.PI, angle.getIntervalMin(), 1e-10);
			assertEquals(3 * Math.PI, angle.getIntervalMax(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testCreateAngleSliderWithAllProperties() {
		// Use Slider command to create a GeoAngle slider
		String gpad = "@style = { slider: min=0 max=2*pi width=180 x=100 y=150 vertical algebra }\n"
				+ "α @style = Slider(0, 2*pi, 0.01, 1, 180, true)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoAngle);
			GeoAngle angle = (GeoAngle) geo;
			
			// Verify all properties
			assertEquals(0.0, angle.getIntervalMin(), 1e-10);
			assertEquals(2 * Math.PI, angle.getIntervalMax(), 1e-10);
			assertEquals(180.0, angle.getSliderWidth(), 1e-10);
			assertEquals(100.0, angle.getSliderX(), 1e-10);
			assertEquals(150.0, angle.getSliderY(), 1e-10);
			assertTrue(!angle.isSliderHorizontal());
			assertTrue(angle.isAVSliderOrCheckboxVisible());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}
