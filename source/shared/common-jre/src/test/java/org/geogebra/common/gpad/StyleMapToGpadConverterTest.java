package org.geogebra.common.gpad;

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
		assertTrue("Should contain pointSize", gpad.contains("pointSize"));
	}
}

