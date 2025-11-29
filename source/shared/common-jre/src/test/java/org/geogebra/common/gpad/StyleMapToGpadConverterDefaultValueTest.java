package org.geogebra.common.gpad;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.geogebra.common.BaseUnitTest;
import org.junit.Test;

/**
 * Unit tests for StyleMapToGpadConverter default value handling.
 * Tests that properties with default values are correctly omitted from Gpad output.
 */
public class StyleMapToGpadConverterDefaultValueTest extends BaseUnitTest {

	// ========== Default Value Tests for GK_INT Properties ==========

	@Test
	public void testArcSizeDefaultValue() {
		// Test arcSize with default value (30) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "30");
		styleMap.put("arcSize", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default arcSize should be omitted", gpad == null || !gpad.contains("arcSize"));
	}

	@Test
	public void testArcSizeNonDefaultValue() {
		// Test arcSize with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "45");
		styleMap.put("arcSize", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain arcSize", gpad.contains("arcSize"));
		assertTrue("Should contain value 45", gpad.contains("45"));
	}

	@Test
	public void testDecimalsDefaultValue() {
		// Test decimals with default value (-1) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "-1");
		styleMap.put("decimals", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default decimals should be omitted", gpad == null || !gpad.contains("decimals"));
	}

	@Test
	public void testDecimalsNonDefaultValue() {
		// Test decimals with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "2");
		styleMap.put("decimals", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain decimals", gpad.contains("decimals"));
		assertTrue("Should contain value 2", gpad.contains("2"));
	}

	@Test
	public void testLayerDefaultValue() {
		// Test layer with default value (0) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("layer", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default layer should be omitted", gpad == null || !gpad.contains("layer"));
	}

	@Test
	public void testLayerNonDefaultValue() {
		// Test layer with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1");
		styleMap.put("layer", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain layer", gpad.contains("layer"));
		assertTrue("Should contain value 1", gpad.contains("1"));
	}

	@Test
	public void testLengthDefaultValue() {
		// Test length with default value (20) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "20");
		styleMap.put("length", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default length should be omitted", gpad == null || !gpad.contains("length"));
	}

	@Test
	public void testLengthNonDefaultValue() {
		// Test length with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "30");
		styleMap.put("length", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain length", gpad.contains("length"));
		assertTrue("Should contain value 30", gpad.contains("30"));
	}

	@Test
	public void testSelectedIndexDefaultValue() {
		// Test selectedIndex with default value (0) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("selectedIndex", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default selectedIndex should be omitted", gpad == null || !gpad.contains("selectedIndex"));
	}

	@Test
	public void testSelectedIndexNonDefaultValue() {
		// Test selectedIndex with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "2");
		styleMap.put("selectedIndex", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain selectedIndex", gpad.contains("selectedIndex"));
		assertTrue("Should contain value 2", gpad.contains("2"));
	}

	@Test
	public void testSignificantFiguresDefaultValue() {
		// Test significantfigures with default value (-1) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "-1");
		styleMap.put("significantfigures", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default significantfigures should be omitted", gpad == null || !gpad.contains("significantfigures"));
	}

	@Test
	public void testSignificantFiguresNonDefaultValue() {
		// Test significantfigures with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "5");
		styleMap.put("significantfigures", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain significantfigures", gpad.contains("significantfigures"));
		assertTrue("Should contain value 5", gpad.contains("5"));
	}

	@Test
	public void testSlopeTriangleSizeDefaultValue() {
		// Test slopeTriangleSize with default value (1) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1");
		styleMap.put("slopeTriangleSize", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default slopeTriangleSize should be omitted", gpad == null || !gpad.contains("slopeTriangleSize"));
	}

	@Test
	public void testSlopeTriangleSizeNonDefaultValue() {
		// Test slopeTriangleSize with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "3");
		styleMap.put("slopeTriangleSize", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain slopeTriangleSize", gpad.contains("slopeTriangleSize"));
		assertTrue("Should contain value 3", gpad.contains("3"));
	}

	// ========== Default Value Tests for GK_FLOAT Properties ==========

	@Test
	public void testFadingDefaultValue() {
		// Test fading with default value (0.0) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0.0");
		styleMap.put("fading", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default fading should be omitted", gpad == null || !gpad.contains("fading"));
	}

	@Test
	public void testFadingNonDefaultValue() {
		// Test fading with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0.5");
		styleMap.put("fading", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain fading", gpad.contains("fading"));
		assertTrue("Should contain value 0.5", gpad.contains("0.5"));
	}

	@Test
	public void testOrderingDefaultValue() {
		// Test ordering with default value (NaN) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "NaN");
		styleMap.put("ordering", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default ordering (NaN) should be omitted", gpad == null || !gpad.contains("ordering"));
	}

	@Test
	public void testOrderingNonDefaultValue() {
		// Test ordering with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1.5");
		styleMap.put("ordering", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain ordering", gpad.contains("ordering"));
		assertTrue("Should contain value 1.5", gpad.contains("1.5"));
	}

	@Test
	public void testPointSizeDefaultValue() {
		// Test pointSize with default value (5) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "5");
		styleMap.put("pointSize", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default pointSize should be omitted", gpad == null || !gpad.contains("pointSize"));
	}

	@Test
	public void testPointSizeNonDefaultValue() {
		// Test pointSize with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "8");
		styleMap.put("pointSize", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain pointSize", gpad.contains("pointSize"));
		assertTrue("Should contain value 8", gpad.contains("8"));
	}

	// ========== Default Value Tests for GK_STR Properties ==========

	@Test
	public void testAngleStyleDefaultValue() {
		// Test angleStyle with default value ("0" -> "0-360") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("angleStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default angleStyle should be omitted", gpad == null || !gpad.contains("angleStyle"));
	}

	@Test
	public void testAngleStyleNonDefaultValue() {
		// Test angleStyle with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1");
		styleMap.put("angleStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain angleStyle", gpad.contains("angleStyle"));
		assertTrue("Should contain converted value 0-180", gpad.contains("0-180"));
	}

	@Test
	public void testCaptionDefaultValue() {
		// Test caption with default value (empty string) should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "");
		styleMap.put("caption", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default caption (empty) should be omitted", gpad == null || !gpad.contains("caption"));
	}

	@Test
	public void testCaptionNonDefaultValue() {
		// Test caption with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "Test Caption");
		styleMap.put("caption", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain caption", gpad.contains("caption"));
		assertTrue("Should contain quoted value", gpad.contains("\"Test Caption\""));
	}

	@Test
	public void testCoordStyleDefaultValue() {
		// Test coordStyle with default value ("cartesian") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "cartesian");
		styleMap.put("coordStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default coordStyle should be omitted", gpad == null || !gpad.contains("coordStyle"));
	}

	@Test
	public void testCoordStyleNonDefaultValue() {
		// Test coordStyle with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "polar");
		styleMap.put("coordStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain coordStyle", gpad.contains("coordStyle"));
		// 根据新语法，不包含特殊字符的值可以不带引号
		assertTrue("Should contain value polar", gpad.contains("polar"));
	}

	@Test
	public void testDecorationDefaultValue() {
		// Test decoration with default value ("0" -> "none") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("type", "0");
		styleMap.put("decoration", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default decoration should be omitted", gpad == null || !gpad.contains("decoration"));
	}

	@Test
	public void testDecorationNonDefaultValue() {
		// Test decoration with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("type", "1");
		styleMap.put("decoration", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain decoration", gpad.contains("decoration"));
		assertTrue("Should contain converted value single_tick", gpad.contains("single_tick"));
	}

	@Test
	public void testEndStyleDefaultValue() {
		// Test endStyle with default value ("default") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "default");
		styleMap.put("endStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default endStyle should be omitted", gpad == null || !gpad.contains("endStyle"));
	}

	@Test
	public void testEndStyleNonDefaultValue() {
		// Test endStyle with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "arrow");
		styleMap.put("endStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain endStyle", gpad.contains("endStyle"));
		// 根据新语法，不包含特殊字符的值可以不带引号
		assertTrue("Should contain value arrow", gpad.contains("arrow"));
	}

	@Test
	public void testHeadStyleDefaultValue() {
		// Test headStyle with default value ("0" -> "default") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("headStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default headStyle should be omitted", gpad == null || !gpad.contains("headStyle"));
	}

	@Test
	public void testHeadStyleNonDefaultValue() {
		// Test headStyle with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1");
		styleMap.put("headStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain headStyle", gpad.contains("headStyle"));
		assertTrue("Should contain converted value arrow", gpad.contains("arrow"));
	}

	@Test
	public void testLabelModeDefaultValue() {
		// Test labelMode with default value ("0" -> "name") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("labelMode", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default labelMode should be omitted", gpad == null || !gpad.contains("labelMode"));
	}

	@Test
	public void testLabelModeNonDefaultValue() {
		// Test labelMode with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1");
		styleMap.put("labelMode", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain labelMode", gpad.contains("labelMode"));
		assertTrue("Should contain converted value namevalue", gpad.contains("namevalue"));
	}

	@Test
	public void testPointStyleDefaultValue() {
		// Test pointStyle with default value ("0" -> "dot") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("pointStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default pointStyle should be omitted", gpad == null || !gpad.contains("pointStyle"));
	}

	@Test
	public void testPointStyleNonDefaultValue() {
		// Test pointStyle with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "-1");
		styleMap.put("pointStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain pointStyle", gpad.contains("pointStyle"));
		assertTrue("Should contain converted value default", gpad.contains("default"));
	}

	@Test
	public void testStartStyleDefaultValue() {
		// Test startStyle with default value ("default") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "default");
		styleMap.put("startStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default startStyle should be omitted", gpad == null || !gpad.contains("startStyle"));
	}

	@Test
	public void testStartStyleNonDefaultValue() {
		// Test startStyle with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "arrow");
		styleMap.put("startStyle", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain startStyle", gpad.contains("startStyle"));
		// 根据新语法，不包含特殊字符的值可以不带引号
		assertTrue("Should contain value arrow", gpad.contains("arrow"));
	}

	@Test
	public void testTextAlignDefaultValue() {
		// Test textAlign with default value ("left") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "left");
		styleMap.put("textAlign", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default textAlign should be omitted", gpad == null || !gpad.contains("textAlign"));
	}

	@Test
	public void testTextAlignNonDefaultValue() {
		// Test textAlign with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "center");
		styleMap.put("textAlign", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain textAlign", gpad.contains("textAlign"));
		// 根据新语法，不包含特殊字符的值可以不带引号
		assertTrue("Should contain value center", gpad.contains("center"));
	}

	@Test
	public void testTooltipModeDefaultValue() {
		// Test tooltipMode with default value ("0" -> "algebraview") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "0");
		styleMap.put("tooltipMode", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default tooltipMode should be omitted", gpad == null || !gpad.contains("tooltipMode"));
	}

	@Test
	public void testTooltipModeNonDefaultValue() {
		// Test tooltipMode with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "1");
		styleMap.put("tooltipMode", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain tooltipMode", gpad.contains("tooltipMode"));
		assertTrue("Should contain converted value on", gpad.contains("on"));
	}

	@Test
	public void testVerticalAlignDefaultValue() {
		// Test verticalAlign with default value ("top") should be omitted
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "top");
		styleMap.put("verticalAlign", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertTrue("Default verticalAlign should be omitted", gpad == null || !gpad.contains("verticalAlign"));
	}

	@Test
	public void testVerticalAlignNonDefaultValue() {
		// Test verticalAlign with non-default value should be output
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("val", "middle");
		styleMap.put("verticalAlign", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain verticalAlign", gpad.contains("verticalAlign"));
		// 根据新语法，不包含特殊字符的值可以不带引号
		assertTrue("Should contain value middle", gpad.contains("middle"));
	}

	// ========== Tests for XML element name mapping ==========

	@Test
	public void testFileElementNameMapping() {
		// Test that XML element "file" maps to Gpad property "filename"
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("name", "test.png");
		styleMap.put("file", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain filename", gpad.contains("filename"));
		assertTrue("Should contain file name", gpad.contains("test.png"));
	}

	@Test
	public void testConditionElementNameMapping() {
		// Test that XML element "condition" maps to Gpad property "showIf"
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("showObject", "x > 0");
		styleMap.put("condition", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should contain showIf", gpad.contains("showIf"));
		assertTrue("Should contain condition value", gpad.contains("x > 0"));
	}

	@Test
	public void testAlgebraElementNameMapping() {
		// Test that XML element "algebra" maps to Gpad property "hideLabelInAlgebra"
		Map<String, LinkedHashMap<String, String>> styleMap = new LinkedHashMap<>();
		
		LinkedHashMap<String, String> attrs = new LinkedHashMap<>();
		attrs.put("labelVisible", "false");
		styleMap.put("algebra", attrs);
		
		String gpad = StyleMapToGpadConverter.convert("test", styleMap, null);
		assertNotNull(gpad);
		assertTrue("Should not contain hideLabelInAlgebra", !gpad.contains("hideLabelInAlgebra"));
	}
}
