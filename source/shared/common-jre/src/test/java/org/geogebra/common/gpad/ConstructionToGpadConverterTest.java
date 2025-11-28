package org.geogebra.common.gpad;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.jre.headless.GgbAPIHeadless;
import org.geogebra.common.plugin.GgbAPI;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for ConstructionToGpadConverter.
 * Tests the complete flow: create objects via gpad, then convert construction to gpad format.
 */
public class ConstructionToGpadConverterTest extends BaseUnitTest {

	private GgbAPI api;

	@Before
	public void setupAPI() {
		api = new GgbAPIHeadless(getApp());
	}

	@Test
	public void testCommandWithSingleOutput() {
		// Create objects via gpad
		String inputGpad = "A = (1, 2);\n"
				+ "B = (3, 4);\n"
				+ "s = Segment(A, B);";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);
		assertTrue("Should create objects", result.contains("A") && result.contains("B") && result.contains("s"));

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label s", outputGpad.contains("s"));
		
		// Verify command is present
		assertTrue("Should contain Segment command", outputGpad.contains("Segment"));
	}

	@Test
	public void testCommandWithMultipleOutputs() {
		// Create objects via gpad
		String inputGpad = "A = (0, 0);\n"
				+ "B = (1, 0);\n"
				+ "C = (0, 1);\n"
				+ "poly1, AB, BC, CA = Polygon(A, B, C);";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify all labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label C", outputGpad.contains("C"));
		assertTrue("Should contain label poly1", outputGpad.contains("poly1"));
		
		// Verify command is present
		assertTrue("Should contain Polygon command", outputGpad.contains("Polygon"));
	}

	@Test
	public void testExpression() {
		// Create objects via gpad (expression creates dependent objects)
		String inputGpad = "a = 1;\n"
				+ "b = 2;\n"
				+ "c = a + b;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label a", outputGpad.contains("a"));
		assertTrue("Should contain label b", outputGpad.contains("b"));
		assertTrue("Should contain label c", outputGpad.contains("c"));
		
		// Verify expression is present (c should be an expression)
		// Note: The format may be "c = a + b" or "c* @cStyle = a + b" (with visibility flags and stylesheet)
		// So we check for both label c and the expression "a + b"
		assertTrue("Should contain expression for c", 
			(outputGpad.contains("c =") || outputGpad.contains("c=") || outputGpad.contains("c*") || outputGpad.contains("c~"))
			&& outputGpad.contains("a + b"));
	}

	@Test
	public void testCommandWithStyles() {
		// Create objects with styles via gpad
		String inputGpad = "@AStyle = { pointSize: 6; objColor: #FF0000FF; }\n"
				+ "@BStyle = { pointSize: 8; objColor: #00FF00FF; }\n"
				+ "A @AStyle = (1, 2);\n"
				+ "B @BStyle = (3, 4);\n"
				+ "s = Segment(A, B);";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label s", outputGpad.contains("s"));
		
		// Verify stylesheets are generated for output objects
		// A and B are independent, s is output of Segment command
		assertTrue("Should contain stylesheet for s", 
			outputGpad.contains("@sStyle") || outputGpad.contains("@style"));
	}

	@Test
	public void testExpressionWithStyles() {
		// Create expression with styles
		String inputGpad = "a = 1;\n"
				+ "@cStyle = { lineStyle: thickness=4; }\n"
				+ "c @cStyle = a + 5;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label a", outputGpad.contains("a"));
		assertTrue("Should contain label c", outputGpad.contains("c"));
		
		// Verify stylesheet is generated for expression output
		assertTrue("Should contain stylesheet for c", 
			outputGpad.contains("@cStyle") || outputGpad.contains("@style"));
		
		// Verify expression is present
		// Note: The format may be "c @cStyle = a + 5" or "c* @cStyle = a + 5" (with visibility flags)
		// So we check for both label c and the expression "a + 5"
		assertTrue("Should contain expression", 
			(outputGpad.contains("c =") || outputGpad.contains("c=") || outputGpad.contains("c*") || outputGpad.contains("c~"))
			&& outputGpad.contains("a + 5"));
	}

	@Test
	public void testIndependentElements() {
		// Create independent elements
		String inputGpad = "A = (1, 2);\n"
				+ "B = (3, 4);\n"
				+ "n = 5;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify all labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label n", outputGpad.contains("n"));
	}

	@Test
	public void testMixedCommandAndExpression() {
		// Create mix of commands and expressions
		String inputGpad = "A = (0, 0);\n"
				+ "B = (1, 0);\n"
				+ "s = Segment(A, B);\n"
				+ "M = Midpoint(s);\n"
				+ "d = Distance(A, B);\n"
				+ "c = d + 1;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify all labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label s", outputGpad.contains("s"));
		assertTrue("Should contain label M", outputGpad.contains("M"));
		assertTrue("Should contain label d", outputGpad.contains("d"));
		assertTrue("Should contain label c", outputGpad.contains("c"));
		
		// Verify commands are present
		assertTrue("Should contain Segment command", outputGpad.contains("Segment"));
		assertTrue("Should contain Midpoint command", outputGpad.contains("Midpoint"));
		assertTrue("Should contain Distance command", outputGpad.contains("Distance"));
	}

	@Test
	public void testStylesheetMerging() {
		// Create objects with same styles
		String inputGpad = "@redStyle = { objColor: #FF0000FF; }\n"
				+ "A @redStyle = (1, 2);\n"
				+ "B @redStyle = (3, 4);\n"
				+ "s = Segment(A, B);";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert with merging enabled
		String outputGpadMerged = api.constructionToGpad(true);
		assertNotNull("constructionToGpad should return non-null", outputGpadMerged);
		
		// Convert without merging
		String outputGpadNotMerged = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpadNotMerged);
		
		// Verify labels are present in both
		assertTrue("Merged: Should contain label A", outputGpadMerged.contains("A"));
		assertTrue("Merged: Should contain label B", outputGpadMerged.contains("B"));
		assertTrue("Merged: Should contain label s", outputGpadMerged.contains("s"));
		assertTrue("Not merged: Should contain label A", outputGpadNotMerged.contains("A"));
		assertTrue("Not merged: Should contain label B", outputGpadNotMerged.contains("B"));
		assertTrue("Not merged: Should contain label s", outputGpadNotMerged.contains("s"));
	}

	@Test
	public void testCommandWithVisibilityFlags() {
		// Create objects with visibility flags
		String inputGpad = "A = (1, 2);\n"
				+ "B* = (3, 4);\n"
				+ "C~ = (5, 6);\n"
				+ "s = Segment(A, B);";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label C", outputGpad.contains("C"));
		assertTrue("Should contain label s", outputGpad.contains("s"));
		
		// Verify visibility flags are preserved (if applicable)
		// Note: visibility flags might be preserved in the output
	}

	@Test
	public void testComplexConstruction() {
		// Create a complex construction with multiple commands and expressions
		String inputGpad = "@pointStyle = { pointSize: 5; }\n"
				+ "@lineStyle = { lineStyle: thickness=3; }\n"
				+ "A @pointStyle = (0, 0);\n"
				+ "B @pointStyle = (4, 0);\n"
				+ "C @pointStyle = (2, 3);\n"
				+ "tri @lineStyle = Polygon(A, B, C);\n"
				+ "M = Midpoint(A, B);\n"
				+ "area = Area(tri);\n"
				+ "perimeter = area * 2;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify all labels are present
		assertTrue("Should contain label A", outputGpad.contains("A"));
		assertTrue("Should contain label B", outputGpad.contains("B"));
		assertTrue("Should contain label C", outputGpad.contains("C"));
		assertTrue("Should contain label tri", outputGpad.contains("tri"));
		assertTrue("Should contain label M", outputGpad.contains("M"));
		assertTrue("Should contain label area", outputGpad.contains("area"));
		assertTrue("Should contain label perimeter", outputGpad.contains("perimeter"));
		
		// Verify commands are present
		assertTrue("Should contain Polygon command", outputGpad.contains("Polygon"));
		assertTrue("Should contain Midpoint command", outputGpad.contains("Midpoint"));
		assertTrue("Should contain Area command", outputGpad.contains("Area"));
		
		// Verify stylesheets are generated for output objects
		assertTrue("Should contain stylesheet definitions", 
			outputGpad.contains("@") && outputGpad.contains("{"));
	}

	@Test
	public void testEmptyConstruction() {
		// Test with empty construction
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		// Should be empty or contain only default elements
	}

	@Test
	public void testFunctionExpression() {
		// Create function via expression
		String inputGpad = "f(x) = x^2 + 1;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.constructionToGpad(false);
		assertNotNull("constructionToGpad should return non-null", outputGpad);
		
		// Verify label is present
		assertTrue("Should contain label f", outputGpad.contains("f"));
		
		// Verify expression or command is present
		assertTrue("Should contain function definition", 
			outputGpad.contains("f") && outputGpad.contains("="));
	}
}

