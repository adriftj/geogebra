package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;
import org.junit.Test;

/**
 * Unit tests for StyleMapToGpadConverter.
 * Tests XML to Gpad conversion for style properties.
 */
public class StyleMapToGpadConverterTest extends BaseUnitTest {

	@Test
	public void testEqnStyleImplicit() {
		// Test implicit style conversion
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> implicitAttrs = new LinkedHashMap<>();
		implicitAttrs.put("style", "implicit");
		styleMap.put("eqnStyle", implicitAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain eqnStyle", gpad.contains("eqnStyle"));
		assertTrue("Should contain implicit", gpad.contains("implicit"));
		assertTrue("Should not contain parameter", !gpad.contains("parameter"));
	}

	@Test
	public void testEqnStyleParametricWithParameter() {
		// Test parametric style with parameter conversion
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> parametricAttrs = new LinkedHashMap<>();
		parametricAttrs.put("style", "parametric");
		parametricAttrs.put("parameter", "t");
		styleMap.put("eqnStyle", parametricAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain eqnStyle", gpad.contains("eqnStyle"));
		assertTrue("Should contain parametric", gpad.contains("parametric"));
		assertTrue("Should contain parameter", gpad.contains("parametric=t"));
	}

	@Test
	public void testEqnStyleParametricWithoutParameter() {
		// Test parametric style without parameter conversion
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> parametricNoParamAttrs = new LinkedHashMap<>();
		parametricNoParamAttrs.put("style", "parametric");
		styleMap.put("eqnStyle", parametricNoParamAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain eqnStyle", gpad.contains("eqnStyle"));
		assertTrue("Should contain parametric", gpad.contains("parametric"));
		assertTrue("Should not contain =", !gpad.contains("parametric="));
	}

	@Test
	public void testEqnStyleAllValidStyles() {
		// Test all valid eqnStyle values
		String[] validStyles = {"implicit", "explicit", "specific", "parametric", "general", "vertex", "conic", "user"};
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		
		for (String style : validStyles) {
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
			attrs.put("style", style);
			styleMap.put("eqnStyle", attrs);
			
			String gpad = converter.convert("test", styleMap);
			assertNotNull("Style " + style + " should convert", gpad);
			assertTrue("Should contain eqnStyle for " + style, gpad.contains("eqnStyle"));
			assertTrue("Should contain " + style, gpad.contains(style));
		}
	}

	@Test
	public void testEqnStyleParametricWithDifferentParameters() {
		// Test parametric style with different parameter values
		String[] parameters = {"t", "T", "s", "u", "v"};
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		
		for (String param : parameters) {
			Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
			LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
			attrs.put("style", "parametric");
			attrs.put("parameter", param);
			styleMap.put("eqnStyle", attrs);
			
			String gpad = converter.convert("test", styleMap);
			assertNotNull("Parameter " + param + " should convert", gpad);
			assertTrue("Should contain parametric=" + param, gpad.contains("parametric=" + param));
		}
	}

	@Test
	public void testEmptyStyleMap() {
		// Test empty style map
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		String gpad = converter.convert("test", styleMap);
		assertTrue("Empty style map should return null", gpad == null);
	}

	@Test
	public void testNullStyleMap() {
		// Test null style map
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		
		String gpad = converter.convert("test", null);
		assertTrue("Null style map should return null", gpad == null);
	}

	@Test
	public void testMultipleProperties() {
		// Test style map with multiple properties including eqnStyle
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		// Add eqnStyle
		LinkedHashMap<String, String> eqnStyleAttrs = new LinkedHashMap<>();
		eqnStyleAttrs.put("style", "implicit");
		styleMap.put("eqnStyle", eqnStyleAttrs);
		
		// Add another property
		LinkedHashMap<String, String> pointSizeAttrs = new LinkedHashMap<>();
		pointSizeAttrs.put("val", "5");
		styleMap.put("pointSize", pointSizeAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain eqnStyle", gpad.contains("eqnStyle"));
		assertTrue("Should not contain pointSize", !gpad.contains("pointSize")); // 因为取了默认值被省略了
	}

	// ========== Animation Tests ==========

	@Test
	public void testAnimationAllDefaults() {
		// Test animation with all default values (should not output animation property)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("playing", "false");
		animAttrs.put("type", "0");
		animAttrs.put("step", "0.1");
		animAttrs.put("speed", "1");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		// When all values are default, animation property should not be output
		assertTrue("Should not contain animation when all defaults", gpad == null || !gpad.contains("animation"));
	}

	@Test
	public void testAnimationOnlyPlaying() {
		// Test animation with only playing=true
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("playing", "true");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain play", gpad.contains("play"));
	}

	@Test
	public void testAnimationOnlyTypeIncreasing() {
		// Test animation with only type=1 (INCREASING), step is default
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("type", "1");
		animAttrs.put("step", "0.1");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain + prefix", gpad.contains("+"));
		// Should not contain step value since it's default
		assertTrue("Should not contain 0.1 after +", !gpad.contains("+0.1"));
	}

	@Test
	public void testAnimationOnlyTypeDecreasing() {
		// Test animation with only type=2 (DECREASING), step is default
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("type", "2");
		animAttrs.put("step", "0.1");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain - prefix", gpad.contains(" -"));
	}

	@Test
	public void testAnimationOnlyTypeIncreasingOnce() {
		// Test animation with only type=3 (INCREASING_ONCE), step is default
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("type", "3");
		animAttrs.put("step", "0.1");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		// Should contain = but not speed=, and not contain step value
		assertTrue("Should contain = prefix", gpad.contains("="));
		assertTrue("Should not contain speed=", !gpad.contains("speed="));
		assertTrue("Should not contain 0.1 after =", !gpad.contains("=0.1"));
	}

	@Test
	public void testAnimationOnlyStep() {
		// Test animation with only step (non-default), type is default
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("type", "0");
		animAttrs.put("step", "0.2");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain 0.2", gpad.contains("0.2"));
		assertTrue("Should not contain +, -, or = prefix", !gpad.contains("+0.2") && !gpad.contains("-0.2") && !gpad.contains("=0.2"));
	}

	@Test
	public void testAnimationOnlySpeed() {
		// Test animation with only speed (non-default)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("speed", "2");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain speed=2", gpad.contains("speed=2"));
	}

	@Test
	public void testAnimationTypeAndStep() {
		// Test animation with type and step (both non-default)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("type", "1");
		animAttrs.put("step", "0.2");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain +0.2", gpad.contains("+0.2"));
	}

	@Test
	public void testAnimationAllProperties() {
		// Test animation with all properties (non-default)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("playing", "true");
		animAttrs.put("type", "2");
		animAttrs.put("step", "0.5");
		animAttrs.put("speed", "3");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain play", gpad.contains("play"));
		assertTrue("Should contain -0.5", gpad.contains("-0.5"));
		assertTrue("Should contain speed=3", gpad.contains("speed=3"));
	}

	@Test
	public void testAnimationStepExpression() {
		// Test animation with step as expression (should be quoted)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("step", "1+1");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain quoted expression", gpad.contains("\"1+1\""));
	}

	@Test
	public void testAnimationSpeedExpression() {
		// Test animation with speed as expression (should be quoted)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("speed", "2*3");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain quoted expression", gpad.contains("speed=\"2*3\""));
	}

	@Test
	public void testAnimationStepNumericNotQuoted() {
		// Test animation with step as plain number (should not be quoted)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("step", "0.25");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain 0.25", gpad.contains("0.25"));
		assertTrue("Should not contain quotes around 0.25", !gpad.contains("\"0.25\""));
	}

	@Test
	public void testAnimationSpeedNumericNotQuoted() {
		// Test animation with speed as plain number (should not be quoted)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("speed", "2.5");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain speed=2.5", gpad.contains("speed=2.5"));
		assertTrue("Should not contain quotes around 2.5", !gpad.contains("speed=\"2.5\""));
	}

	@Test
	public void testAnimationTypeWithStepExpression() {
		// Test animation with type and step as expression
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		animAttrs.put("type", "1");
		animAttrs.put("step", "a+b");
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain animation", gpad.contains("animation"));
		assertTrue("Should contain + prefix", gpad.contains("+"));
		assertTrue("Should contain quoted expression", gpad.contains("\"a+b\""));
	}

	@Test
	public void testAnimationEmptyAttrs() {
		// Test animation with empty attributes (should not output animation property)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> animAttrs = new LinkedHashMap<>();
		styleMap.put("animation", animAttrs);
		
		String gpad = converter.convert("test", styleMap);
		// When attributes are empty, animation property should not be output
		assertTrue("Should not contain animation when attrs empty", gpad == null || !gpad.contains("animation"));
	}

	@Test
	public void testCheckboxWithFixedTrue() {
		// Test checkbox with fixed="true": should output both "checkbox;" and "fixed;"
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> checkboxAttrs = new LinkedHashMap<>();
		checkboxAttrs.put("fixed", "true");
		styleMap.put("checkbox", checkboxAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain checkbox", gpad.contains("checkbox"));
		assertTrue("Should contain fixed", gpad.contains("fixed"));
	}

	@Test
	public void testCheckboxWithFixedFalse() {
		// Test checkbox with fixed="false": should only output "checkbox;" (fixed is default false, so omitted)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> checkboxAttrs = new LinkedHashMap<>();
		checkboxAttrs.put("fixed", "false");
		styleMap.put("checkbox", checkboxAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain checkbox", gpad.contains("checkbox"));
		assertTrue("Should not contain fixed (default false, omitted)", !gpad.contains("fixed"));
	}

	@Test
	public void testCheckboxWithoutFixed() {
		// Test checkbox without fixed attribute: should only output "checkbox;" (fixed defaults to false)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> checkboxAttrs = new LinkedHashMap<>();
		styleMap.put("checkbox", checkboxAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain checkbox", gpad.contains("checkbox"));
		assertTrue("Should not contain fixed (default false, omitted)", !gpad.contains("fixed"));
	}

	@Test
	public void testCheckboxWithFixedTrueAndOtherProperties() {
		// Test checkbox with fixed="true" along with other properties
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> checkboxAttrs = new LinkedHashMap<>();
		checkboxAttrs.put("fixed", "true");
		styleMap.put("checkbox", checkboxAttrs);
		
		LinkedHashMap<String, String> traceAttrs = new LinkedHashMap<>();
		traceAttrs.put("val", "true");
		styleMap.put("trace", traceAttrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain checkbox", gpad.contains("checkbox"));
		assertTrue("Should contain fixed", gpad.contains("fixed"));
		assertTrue("Should contain trace", gpad.contains("trace"));
	}

	// ========== Tests for boundingBox ==========

	@Test
	public void testBoundingBoxBasic() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		styleMap.put("boundingBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain boundingBox", gpad.contains("boundingBox"));
		assertTrue("Should contain width=100", gpad.contains("width=100"));
		assertTrue("Should contain height=200", gpad.contains("height=200"));
	}

	@Test
	public void testBoundingBoxWithFloatIgnoresDecimal() {
		// Test that decimal part is ignored when converting from XML
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100.5");
		attrs.put("height", "200.9");
		styleMap.put("boundingBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain width=100 (decimal ignored)", gpad.contains("width=100"));
		assertTrue("Should contain height=200 (decimal ignored)", gpad.contains("height=200"));
		assertTrue("Should not contain 100.5", !gpad.contains("100.5"));
		assertTrue("Should not contain 200.9", !gpad.contains("200.9"));
	}

	@Test
	public void testBoundingBoxOnlyWidth() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		styleMap.put("boundingBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain boundingBox", gpad.contains("boundingBox"));
		assertTrue("Should contain width=100", gpad.contains("width=100"));
		assertTrue("Should not contain height", !gpad.contains("height"));
	}

	@Test
	public void testBoundingBoxOnlyHeight() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("height", "200");
		styleMap.put("boundingBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain boundingBox", gpad.contains("boundingBox"));
		assertTrue("Should contain height=200", gpad.contains("height=200"));
		assertTrue("Should not contain width", !gpad.contains("width"));
	}

	// ========== Tests for contentSize ==========

	@Test
	public void testContentSizeBasic() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100.5");
		attrs.put("height", "200.3");
		styleMap.put("contentSize", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain contentSize", gpad.contains("contentSize"));
		assertTrue("Should contain width=100.5", gpad.contains("width=100.5"));
		assertTrue("Should contain height=200.3", gpad.contains("height=200.3"));
	}

	@Test
	public void testContentSizeWithInteger() {
		// Test with integer values (should preserve as-is)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		styleMap.put("contentSize", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain width=100", gpad.contains("width=100"));
		assertTrue("Should contain height=200", gpad.contains("height=200"));
	}

	@Test
	public void testContentSizeOnlyWidth() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100.5");
		styleMap.put("contentSize", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain contentSize", gpad.contains("contentSize"));
		assertTrue("Should contain width=100.5", gpad.contains("width=100.5"));
		assertTrue("Should not contain height", !gpad.contains("height"));
	}

	// ========== Tests for cropBox ==========

	@Test
	public void testCropBoxBasic() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "10");
		attrs.put("y", "20");
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("cropped", "true");
		styleMap.put("cropBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain cropBox", gpad.contains("cropBox"));
		assertTrue("Should contain x=10", gpad.contains("x=10"));
		assertTrue("Should contain y=20", gpad.contains("y=20"));
		assertTrue("Should contain width=100", gpad.contains("width=100"));
		assertTrue("Should contain height=200", gpad.contains("height=200"));
		assertTrue("Should contain cropped", gpad.contains("cropped"));
		assertTrue("Should not contain ~cropped", !gpad.contains("~cropped"));
	}

	@Test
	public void testCropBoxWithCroppedFalse() {
		// Test with cropped=false (should not output, as it's the default value)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "10");
		attrs.put("y", "20");
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("cropped", "false");
		styleMap.put("cropBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should not contain cropped (default value)", !gpad.contains("cropped"));
		assertTrue("Should not contain ~cropped (default value)", !gpad.contains("~cropped"));
	}

	@Test
	public void testCropBoxWithFloatValues() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "10.5");
		attrs.put("y", "20.7");
		attrs.put("width", "100.2");
		attrs.put("height", "200.9");
		attrs.put("cropped", "true");
		styleMap.put("cropBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain x=10.5", gpad.contains("x=10.5"));
		assertTrue("Should contain y=20.7", gpad.contains("y=20.7"));
		assertTrue("Should contain width=100.2", gpad.contains("width=100.2"));
		assertTrue("Should contain height=200.9", gpad.contains("height=200.9"));
	}

	@Test
	public void testCropBoxWithoutCropped() {
		// Test without cropped attribute
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "10");
		attrs.put("y", "20");
		attrs.put("width", "100");
		attrs.put("height", "200");
		styleMap.put("cropBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain cropBox", gpad.contains("cropBox"));
		assertTrue("Should not contain cropped", !gpad.contains("cropped"));
	}

	// ========== Tests for dimensions ==========

	@Test
	public void testDimensionsBasic() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("angle", "45");
		attrs.put("unscaled", "false"); // unscaled=false means scaled=true
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain dimensions", gpad.contains("dimensions"));
		assertTrue("Should contain width=100", gpad.contains("width=100"));
		assertTrue("Should contain height=200", gpad.contains("height=200"));
		assertTrue("Should contain angle=45", gpad.contains("angle=45"));
		assertTrue("Should contain scaled", gpad.contains("scaled"));
		assertTrue("Should not contain ~scaled", !gpad.contains("~scaled"));
	}

	@Test
	public void testDimensionsWithUnscaledTrue() {
		// Test with unscaled=true (should not output, as it's the default value)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("unscaled", "true"); // unscaled=true is default, so should not output
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should not contain scaled (default value)", !gpad.contains("scaled"));
		assertTrue("Should not contain ~scaled (default value)", !gpad.contains("~scaled"));
	}

	@Test
	public void testDimensionsWithFloatValues() {
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100.5");
		attrs.put("height", "200.3");
		attrs.put("angle", "45.7");
		attrs.put("unscaled", "false");
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain width=100.5", gpad.contains("width=100.5"));
		assertTrue("Should contain height=200.3", gpad.contains("height=200.3"));
		assertTrue("Should contain angle=45.7", gpad.contains("angle=45.7"));
	}

	@Test
	public void testDimensionsWithoutAngle() {
		// Test without angle
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("unscaled", "false");
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain dimensions", gpad.contains("dimensions"));
		assertTrue("Should not contain angle", !gpad.contains("angle"));
	}

	@Test
	public void testDimensionsWithAngleZero() {
		// Test with angle=0 (should not output, as it's the default value)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("angle", "0");
		attrs.put("unscaled", "false");
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain dimensions", gpad.contains("dimensions"));
		assertTrue("Should not contain angle (default value)", !gpad.contains("angle"));
	}

	@Test
	public void testDimensionsWithAngleNonZero() {
		// Test with angle=45 (should output)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("angle", "45");
		attrs.put("unscaled", "false");
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain angle=45", gpad.contains("angle=45"));
	}

	@Test
	public void testDimensionsWithoutScaled() {
		// Test without unscaled attribute
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "100");
		attrs.put("height", "200");
		attrs.put("angle", "45");
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain dimensions", gpad.contains("dimensions"));
		assertTrue("Should not contain scaled", !gpad.contains("scaled"));
		assertTrue("Should not contain ~scaled", !gpad.contains("~scaled"));
	}

	// ========== Tests for negative values ==========

	@Test
	public void testCropBoxWithNegativeValues() {
		// Test conversion with negative values
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("x", "-10");
		attrs.put("y", "-20");
		attrs.put("width", "-100");
		attrs.put("height", "-200");
		attrs.put("cropped", "true");
		styleMap.put("cropBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain x=-10", gpad.contains("x=-10"));
		assertTrue("Should contain y=-20", gpad.contains("y=-20"));
		assertTrue("Should contain width=-100", gpad.contains("width=-100"));
		assertTrue("Should contain height=-200", gpad.contains("height=-200"));
	}

	@Test
	public void testDimensionsWithNegativeValues() {
		// Test conversion with negative values
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "-100");
		attrs.put("height", "-200");
		attrs.put("angle", "-45");
		attrs.put("unscaled", "false");
		styleMap.put("dimensions", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain width=-100", gpad.contains("width=-100"));
		assertTrue("Should contain height=-200", gpad.contains("height=-200"));
		assertTrue("Should contain angle=-45", gpad.contains("angle=-45"));
	}

	@Test
	public void testContentSizeWithNegativeValues() {
		// Test conversion with negative values
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "-100.5");
		attrs.put("height", "-200.3");
		styleMap.put("contentSize", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain width=-100.5", gpad.contains("width=-100.5"));
		assertTrue("Should contain height=-200.3", gpad.contains("height=-200.3"));
	}

	@Test
	public void testBoundingBoxWithNegativeValues() {
		// Test conversion with negative values (decimal part ignored)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("width", "-100.5");
		attrs.put("height", "-200.9");
		styleMap.put("boundingBox", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain width=-100 (decimal ignored)", gpad.contains("width=-100"));
		assertTrue("Should contain height=-200 (decimal ignored)", gpad.contains("height=-200"));
		assertTrue("Should not contain -100.5", !gpad.contains("-100.5"));
		assertTrue("Should not contain -200.9", !gpad.contains("-200.9"));
	}

	// ========== Tests for font ==========

	@Test
	public void testFontBasicSerifSizePlain() {
		// Test font: serif *0.5 plain
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "0.5");
		attrs.put("style", "0");
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain serif", gpad.contains("serif"));
		assertTrue("Should contain *0.5", gpad.contains("*0.5"));
		// style=0 (plain) is default, so should not be output
		assertTrue("Should not contain plain (default)", !gpad.contains("plain"));
	}

	@Test
	public void testFontTildeSerifSizeItalicBold() {
		// Test font: ~serif *2 italic bold
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false");
		attrs.put("sizeM", "2");
		attrs.put("style", "3");
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		// serif=false is default, so should not output ~serif
		assertTrue("Should not contain ~serif (default false)", !gpad.contains("~serif"));
		assertTrue("Should contain *2", gpad.contains("*2"));
		assertTrue("Should contain italic bold", gpad.contains("italic bold"));
	}

	@Test
	public void testFontSerifOnly() {
		// Test font with only serif=true
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "1.0"); // default
		attrs.put("style", "0"); // default
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain serif", gpad.contains("serif"));
		assertTrue("Should not contain * (default 1.0)", !gpad.contains("*"));
		assertTrue("Should not contain style keywords (default plain)", !gpad.contains("plain") && !gpad.contains("bold") && !gpad.contains("italic"));
	}

	@Test
	public void testFontSizeOnly() {
		// Test font with only sizeM
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false"); // default
		attrs.put("sizeM", "1.5");
		attrs.put("style", "0"); // default
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should not contain serif (default false)", !gpad.contains("serif"));
		assertTrue("Should contain *1.5", gpad.contains("*1.5"));
		assertTrue("Should not contain style keywords", !gpad.contains("plain") && !gpad.contains("bold") && !gpad.contains("italic"));
	}

	@Test
	public void testFontBoldOnly() {
		// Test font with only bold style
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false"); // default
		attrs.put("sizeM", "1.0"); // default
		attrs.put("style", "1"); // bold
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should not contain serif", !gpad.contains("serif"));
		assertTrue("Should not contain *sizeM", !gpad.contains("*"));
		assertTrue("Should contain bold", gpad.contains("bold"));
		assertTrue("Should not contain italic", !gpad.contains("italic"));
	}

	@Test
	public void testFontItalicOnly() {
		// Test font with only italic style
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false"); // default
		attrs.put("sizeM", "1.0"); // default
		attrs.put("style", "2"); // italic
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain italic", gpad.contains("italic"));
		assertTrue("Should not contain bold", !gpad.contains("bold"));
	}

	@Test
	public void testFontBoldItalic() {
		// Test font with bold+italic (style=3)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false"); // default
		attrs.put("sizeM", "1.0"); // default
		attrs.put("style", "3"); // bold + italic
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain italic bold", gpad.contains("italic bold"));
	}

	@Test
	public void testFontAllProperties() {
		// Test font with all properties
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "1.8");
		attrs.put("style", "3"); // bold + italic
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain serif", gpad.contains("serif"));
		assertTrue("Should contain *1.8", gpad.contains("*1.8"));
		assertTrue("Should contain italic bold", gpad.contains("italic bold"));
	}

	@Test
	public void testFontSerifFalseOmitted() {
		// Test that serif=false is omitted (default value)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false");
		attrs.put("sizeM", "1.0"); // default
		attrs.put("style", "0"); // default
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		// When all values are default, font property should not be output
		assertTrue("Should not contain font when all defaults", gpad == null || !gpad.contains("font"));
	}

	@Test
	public void testFontSizeDefaultOmitted() {
		// Test that sizeM=1.0 is omitted (default value)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false"); // default
		attrs.put("sizeM", "1.0");
		attrs.put("style", "1"); // bold
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should not contain size (default 1.0)", !gpad.contains("*"));
		assertTrue("Should contain bold", gpad.contains("bold"));
	}

	@Test
	public void testFontStylePlainOmitted() {
		// Test that style=0 (plain) is omitted (default value)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "1.5");
		attrs.put("style", "0"); // plain (default)
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain serif", gpad.contains("serif"));
		assertTrue("Should contain *1.5", gpad.contains("*1.5"));
		assertTrue("Should not contain plain (default)", !gpad.contains("plain"));
	}

	@Test
	public void testFontWithFloatSize() {
		// Test font with float size values
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "0.75");
		attrs.put("style", "2"); // italic
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain *0.75", gpad.contains("*0.75"));
		assertTrue("Should contain italic", gpad.contains("italic"));
	}

	@Test
	public void testFontWithLargeSize() {
		// Test font with large size value
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "false"); // default
		attrs.put("sizeM", "3.5");
		attrs.put("style", "0"); // default
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain *3.5", gpad.contains("*3.5"));
	}

	@Test
	public void testFontEmptyAttrs() {
		// Test font with empty attributes (should not output font property)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		// When attributes are empty, font property should not be output
		assertTrue("Should not contain font when attrs empty", gpad == null || !gpad.contains("font"));
	}

	@Test
	public void testFontWithInvalidStyleValue() {
		// Test font with invalid style value (should ignore)
		StyleMapToGpadConverter converter = new StyleMapToGpadConverter();
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("serif", "true");
		attrs.put("sizeM", "1.5");
		attrs.put("style", "invalid"); // invalid value
		styleMap.put("font", attrs);
		
		String gpad = converter.convert("test", styleMap);
		assertNotNull(gpad);
		assertTrue("Should contain font", gpad.contains("font"));
		assertTrue("Should contain serif", gpad.contains("serif"));
		assertTrue("Should contain *1.5", gpad.contains("*1.5"));
		// Invalid style should be ignored
		assertTrue("Should not contain style keywords", !gpad.contains("plain") && !gpad.contains("bold") && !gpad.contains("italic"));
	}
}
