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
}
