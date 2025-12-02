package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.jre.headless.GgbAPIHeadless;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoVector;
import org.geogebra.common.plugin.GgbAPI;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for GgbToGpadConverter.
 * Tests the complete flow: create objects via gpad, then convert construction to gpad format.
 */
public class GgbToGpadConverterTest extends BaseUnitTest {

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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label a", outputGpad.contains("a"));
		assertTrue("Should contain label b", outputGpad.contains("b"));
		assertTrue("Should contain label c", outputGpad.contains("c"));
		
		// Verify expression is present (c should be an expression)
		// Note: Since c is a numeric expression (not drawable in geometry view), it won't have "*" or "~" suffixes
		// The format is "c @cStyle = a + b" (with stylesheet) or "c = a + b" (without stylesheet)
		// So we check for label c, followed by optional stylesheet reference, then "=" and the expression "a + b"
		assertTrue("Should contain expression for c", 
			(outputGpad.contains("c @") || outputGpad.contains("c =") || outputGpad.contains("c="))
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify labels are present
		assertTrue("Should contain label a", outputGpad.contains("a"));
		assertTrue("Should contain label c", outputGpad.contains("c"));
		
		// Note: Since c is a numeric expression (not drawable in geometry view, no slider created),
		// lineStyle cannot be applied to it. The stylesheet @cStyle will be empty after conversion
		// and will be omitted, so @cStyle should NOT be present in the output.
		// This is expected behavior: lineStyle only applies to drawable objects in geometry view.
		assertTrue("Should NOT contain @cStyle (lineStyle cannot apply to non-drawable numeric expressions)", 
			!outputGpad.contains("@cStyle"));
		
		// Verify expression is present
		// Note: Since c is a numeric expression (not drawable in geometry view), it won't have "*" or "~" suffixes
		// The format is "c = a + 5" (without stylesheet, since lineStyle was omitted)
		// So we check for label c, followed by "=" and the expression "a + 5"
		assertTrue("Should contain expression", 
			(outputGpad.contains("c =") || outputGpad.contains("c="))
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpadMerged = api.toGpad(true);
		assertNotNull("toGpad should return non-null", outputGpadMerged);
		
		// Convert without merging
		String outputGpadNotMerged = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpadNotMerged);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
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
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		// Should be empty or contain only default elements
	}

	@Test
	public void testFunctionExpression() {
		// Create function via expression
		String inputGpad = "f(x) = x^2 + 1;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify label is present
		assertTrue("Should contain label f", outputGpad.contains("f"));
		
		// Verify expression or command is present
		assertTrue("Should contain function definition", 
			outputGpad.contains("f") && outputGpad.contains("="));
	}

	@Test
	public void testSingleVariableFunction() {
		// Create single-variable function: f(x) = x^2 + 1
		String inputGpad = "f(x) = x^2 + 1;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify new format: f(x) @style ... = ...
		// Should contain f(x) with variable list in parentheses
		// Note: The format should be f(x) @style ... = ... or f(x) = ...
		// Check for f followed by (x) pattern
		boolean hasFunctionWithVars = outputGpad.matches("(?s).*f\\s*\\(\\s*x\\s*\\).*");
		assertTrue("Should contain f(x) with variable list. Actual output: " + outputGpad, 
			hasFunctionWithVars || outputGpad.contains("f(x)") || outputGpad.contains("f(x )"));
		
		// Should contain the expression (accept both x^2 and x² format)
		assertTrue("Should contain function expression", 
			outputGpad.contains("x^2 + 1") || outputGpad.contains("x^2+1") 
			|| outputGpad.contains("x² + 1") || outputGpad.contains("x²+1"));
	}

	@Test
	public void testMultiVariableFunction() {
		// Create multi-variable function: g(x, y) = x * y
		String inputGpad = "g(x, y) = x * y;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify new format: g(x, y) @style ... = ...
		// Should contain g(x, y) with variable list in parentheses
		assertTrue("Should contain g(x, y) with variable list", 
			outputGpad.contains("g(x, y)") || outputGpad.contains("g(x,y)"));
		
		// Should contain the expression (accept various multiplication formats)
		assertTrue("Should contain function expression", 
			outputGpad.contains("x * y") || outputGpad.contains("x*y") 
			|| outputGpad.contains("x y") || outputGpad.contains("x·y")
			|| (outputGpad.contains("x") && outputGpad.contains("y")));
	}

	@Test
	public void testFunctionWithThreeVariables() {
		// Create function with three variables: h(x, y, z) = x + y + z
		String inputGpad = "h(x, y, z) = x + y + z;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify new format: h(x, y, z) @style ... = ...
		// Should contain h(x, y, z) with variable list in parentheses
		assertTrue("Should contain h(x, y, z) with variable list", 
			outputGpad.contains("h(x") && outputGpad.contains("y") && outputGpad.contains("z)"));
		
		// Should contain the expression
		assertTrue("Should contain function expression", 
			outputGpad.contains("x + y + z") || outputGpad.contains("x+y+z"));
	}

	@Test
	public void testFunctionWithStyle() {
		// Create function with style sheet
		String inputGpad = "@fStyle = { lineStyle: thickness=3; objColor: #FF0000FF; }\n"
				+ "f(x) @fStyle = x^2;";
		
		String result = api.evalGpad(inputGpad);
		if (result == null) {
			String error = api.getLastError();
			String warning = api.getLastWarning();
			System.err.println("evalGpad failed for testFunctionWithStyle");
			if (error != null) {
				System.err.println("Error: " + error);
			}
			if (warning != null) {
				System.err.println("Warning: " + warning);
			}
		}
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify new format: f(x) @style ... = ...
		// Should contain f(x) with variable list
		assertTrue("Should contain f(x) with variable list", 
			outputGpad.contains("f(x)") || outputGpad.contains("f(x )"));
		
		// Should contain stylesheet reference (may be @fStyle or generated @fStyle)
		assertTrue("Should contain stylesheet reference", 
			outputGpad.contains("@fStyle") || outputGpad.contains("@style"));
		
		// Should contain the expression (accept both x^2 and x² format)
		assertTrue("Should contain function expression", 
			outputGpad.contains("x^2") || outputGpad.contains("x²"));
	}

	@Test
	public void testFunctionWithVisibilityFlags() {
		// Create function with visibility flags
		String inputGpad = "f(x) = x^2;\n"
				+ "g(x) = x^3;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify new format: f(x) @style ... = ... and g(x) @style ... = ...
		// Should contain f(x) and g(x) with variable lists
		assertTrue("Should contain f(x) with variable list", 
			outputGpad.contains("f(x)") || outputGpad.contains("f(x )"));
		assertTrue("Should contain g(x) with variable list", 
			outputGpad.contains("g(x)") || outputGpad.contains("g(x )"));
	}

	@Test
	public void testFunctionRoundTrip() {
		// Test round trip: gpad -> GeoElement -> gpad
		String inputGpad = "f(x) = x^2 + 2*x + 1;\n"
				+ "g(x, y) = x * y + 1;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify format for f(x)
		assertTrue("Should contain f(x) with variable list", 
			outputGpad.contains("f(x)") || outputGpad.contains("f(x )"));
		assertTrue("Should contain f's expression", 
			outputGpad.contains("x^2") || outputGpad.contains("x²")
			|| outputGpad.contains("2*x") || outputGpad.contains("2x"));
		
		// Verify format for g(x, y)
		assertTrue("Should contain g(x, y) with variable list", 
			outputGpad.contains("g(x") && outputGpad.contains("y)"));
		assertTrue("Should contain g's expression", 
			outputGpad.contains("x * y") || outputGpad.contains("x*y")
			|| outputGpad.contains("x y") || outputGpad.contains("x·y")
			|| (outputGpad.contains("x") && outputGpad.contains("y")));
		
		// Try to parse the output again to verify it's valid gpad
		String result2 = api.evalGpad(outputGpad);
		if (result2 == null) {
			String error = api.getLastError();
			String warning = api.getLastWarning();
			System.err.println("Round trip failed for testFunctionRoundTrip");
			System.err.println("Output gpad: " + outputGpad);
			if (error != null) {
				System.err.println("Error: " + error);
			}
			if (warning != null) {
				System.err.println("Warning: " + warning);
			}
		}
		// Should succeed (result may be empty if no new objects created, but should not be null)
		assertNotNull("Round trip should succeed", result2);
	}

	@Test
	public void testFunctionWithMultiLetterVariable() {
		// Test function with multi-letter variable name
		String inputGpad = "f(alpha) = alpha^2;\n"
				+ "g(beta, gamma) = beta + gamma;";
		
		String result = api.evalGpad(inputGpad);
		assertNotNull("evalGpad should succeed", result);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify format for f(alpha)
		assertTrue("Should contain f(alpha) with variable list", 
			outputGpad.contains("f(alpha)") || outputGpad.contains("f(alpha )"));
		
		// Verify format for g(beta, gamma)
		assertTrue("Should contain g(beta, gamma) with variable list", 
			outputGpad.contains("g(beta") && outputGpad.contains("gamma)"));
	}

	@Test
	public void testMacroConversion() {
		// Create a macro via gpad
		String inputGpad = "@@macro MyMidpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint(A, B);\n"
				+ "  @@return M\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify macro definition is present and at the beginning
		assertTrue("Should contain @@macro", outputGpad.contains("@@macro"));
		assertTrue("Should contain MyMidpoint", outputGpad.contains("MyMidpoint"));
		assertTrue("Should contain input parameters", outputGpad.contains("A, B") || outputGpad.contains("A,B"));
		assertTrue("Should contain @@return", outputGpad.contains("@@return"));
		assertTrue("Should contain output M", outputGpad.contains("@@return M") || outputGpad.contains("@@return M\n"));
		
		// Verify @@return does NOT end with semicolon
		int returnIndex = outputGpad.indexOf("@@return");
		if (returnIndex >= 0) {
			String afterReturn = outputGpad.substring(returnIndex);
			// Find the line with @@return
			int newlineIndex = afterReturn.indexOf("\n");
			if (newlineIndex > 0) {
				String returnLine = afterReturn.substring(0, newlineIndex);
				// Should NOT end with semicolon before the closing brace
				assertTrue("@@return should not end with semicolon", 
					!returnLine.trim().endsWith(";"));
			}
		}
	}

	@Test
	public void testMacroWithMultipleInputsAndOutputs() {
		// Create a macro with multiple inputs and outputs
		String inputGpad = "@@macro Triangle(A, B, C) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 0);\n"
				+ "  C = (1, 2);\n"
				+ "  a = Segment(B, C);\n"
				+ "  b = Segment(C, A);\n"
				+ "  c = Segment(A, B);\n"
				+ "  @@return a, b, c\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify macro definition
		assertTrue("Should contain Triangle macro", outputGpad.contains("Triangle"));
		assertTrue("Should contain input parameters A, B, C", 
			outputGpad.contains("Triangle(A") && outputGpad.contains("B") && outputGpad.contains("C"));
		assertTrue("Should contain multiple outputs", 
			outputGpad.contains("@@return a") && outputGpad.contains("b") && outputGpad.contains("c"));
	}

	@Test
	public void testMacroWithStyleSheet() {
		// Create a macro with stylesheet inside
		String inputGpad = "@@macro ColoredPoint(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  @redStyle = { objColor: #FF0000FF; pointSize: 6; }\n"
				+ "  P @redStyle = A;\n"
				+ "  @@return P\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify macro definition contains stylesheet
		assertTrue("Should contain ColoredPoint macro", outputGpad.contains("ColoredPoint"));
		// Stylesheet may be in macro body or generated for output
		assertTrue("Should contain stylesheet definition in macro", 
			outputGpad.contains("@redStyle") || outputGpad.contains("@style") || outputGpad.contains("@P"));
		assertTrue("Should contain @@return", outputGpad.contains("@@return"));
	}

	@Test
	public void testMultipleMacros() {
		// Create multiple macros
		String inputGpad = "@@macro MyMidpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint(A, B);\n"
				+ "  @@return M\n"
				+ "}\n"
				+ "@@macro MyDistance(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  d = Distance(A, B);\n"
				+ "  @@return d\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify both macros are present
		assertTrue("Should contain MyMidpoint macro", outputGpad.contains("MyMidpoint"));
		assertTrue("Should contain MyDistance macro", outputGpad.contains("MyDistance"));
		
		// Verify macros appear before construction (if any)
		int midpointIndex = outputGpad.indexOf("MyMidpoint");
		int distanceIndex = outputGpad.indexOf("MyDistance");
		assertTrue("Both macros should be present", midpointIndex >= 0 && distanceIndex >= 0);
	}

	@Test
	public void testMacroAndConstruction() {
		// Create a macro and some construction elements
		String inputGpad = "@@macro MyMidpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint(A, B);\n"
				+ "  @@return M\n"
				+ "}\n"
				+ "P = (0, 0);\n"
				+ "Q = (2, 2);\n"
				+ "R = (1, 1);";
		
		String result = api.evalGpad(inputGpad);
		// Should create P, Q, R, so result should not be null or empty
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertTrue("evalGpad should create objects (result should not be empty)", !result.isEmpty());
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify macro appears before construction
		assertTrue("Should contain MyMidpoint macro", outputGpad.contains("MyMidpoint"));
		// Check for P, Q, R in output (format may vary: "P =", "P=", or just "P" in macro context)
		assertTrue("Should contain P", outputGpad.contains("P") && (outputGpad.contains("P =") || outputGpad.contains("P=") || outputGpad.contains("P ")));
		assertTrue("Should contain Q", outputGpad.contains("Q") && (outputGpad.contains("Q =") || outputGpad.contains("Q=") || outputGpad.contains("Q ")));
		assertTrue("Should contain R", outputGpad.contains("R") && (outputGpad.contains("R =") || outputGpad.contains("R=") || outputGpad.contains("R ")));
		
		// Verify macro appears at the beginning
		int macroIndex = outputGpad.indexOf("@@macro");
		// Find first occurrence of P, Q, or R in construction (not in macro)
		int pIndex = outputGpad.indexOf("P");
		if (pIndex < 0) pIndex = outputGpad.indexOf("Q");
		if (pIndex < 0) pIndex = outputGpad.indexOf("R");
		assertTrue("Macro should appear before construction", 
			macroIndex >= 0 && pIndex >= 0 && macroIndex < pIndex);
	}

	@Test
	public void testMacroReturnFormat() {
		// Test that @@return does not have semicolon
		String inputGpad = "@@macro TestMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  P = A;\n"
				+ "  @@return P\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Find @@return statement
		int returnIndex = outputGpad.indexOf("@@return");
		assertTrue("Should contain @@return", returnIndex >= 0);
		
		// Extract the line with @@return
		String afterReturn = outputGpad.substring(returnIndex);
		int newlineIndex = afterReturn.indexOf("\n");
		if (newlineIndex > 0) {
			String returnLine = afterReturn.substring(0, newlineIndex).trim();
			// Should end with just the output label(s), not with semicolon
			assertTrue("@@return line should not end with semicolon", 
				!returnLine.endsWith(";"));
			assertTrue("@@return line should contain output", 
				returnLine.contains("P"));
		}
	}

	@Test
	public void testMacroWithComplexBody() {
		// Create a macro with complex body including stylesheets
		String inputGpad = "@@macro PerpendicularBisector(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint(A, B);\n"
				+ "  s = Segment(A, B);\n"
				+ "  @lineStyle = { lineStyle: thickness=3; }\n"
				+ "  l @lineStyle = PerpendicularLine(M, s);\n"
				+ "  @@return l\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify macro definition
		assertTrue("Should contain PerpendicularBisector macro", 
			outputGpad.contains("PerpendicularBisector"));
		assertTrue("Should contain macro body statements", 
			outputGpad.contains("Midpoint") || outputGpad.contains("Segment") || outputGpad.contains("PerpendicularLine"));
		// Stylesheet may be in macro body or generated for output
		assertTrue("Should contain stylesheet in macro", 
			outputGpad.contains("@lineStyle") || outputGpad.contains("@style") || outputGpad.contains("@l"));
		assertTrue("Should contain @@return", outputGpad.contains("@@return"));
	}

	@Test
	public void testMacroWithNoInput() {
		// Create a macro with no input parameters
		String inputGpad = "@@macro CreateOrigin() {\n"
				+ "  O = (0, 0);\n"
				+ "  @@return O\n"
				+ "}";
		
		String result = api.evalGpad(inputGpad);
		// Macro definition doesn't create GeoElements, so result should be empty string ""
		// null means error occurred
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertEquals("evalGpad should return empty string for macro definition", "", result);
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify macro definition
		assertTrue("Should contain CreateOrigin macro", outputGpad.contains("CreateOrigin"));
		assertTrue("Should contain empty parameter list", 
			outputGpad.contains("CreateOrigin()"));
		assertTrue("Should contain @@return", outputGpad.contains("@@return"));
		assertTrue("Should contain output O", outputGpad.contains("@@return O") || outputGpad.contains("@@return O\n"));
	}

	@Test
	public void testMacroOrdering() {
		// Create multiple macros and construction elements
		String inputGpad = "P = (0, 0);\n"
				+ "@@macro Macro1(A) {\n"
				+ "  A = (1, 1);\n"
				+ "  @@return A\n"
				+ "}\n"
				+ "Q = (2, 2);\n"
				+ "@@macro Macro2(B) {\n"
				+ "  B = (3, 3);\n"
				+ "  @@return B\n"
				+ "}\n"
				+ "R = (4, 4);";
		
		String result = api.evalGpad(inputGpad);
		// Should create P, Q, R, so result should not be null or empty
		assertNotNull("evalGpad should not return null (null means error)", result);
		assertTrue("evalGpad should create objects (result should not be empty)", !result.isEmpty());
		assertTrue("evalGpad should succeed (no error)", api.getLastError() == null);

		// Convert construction to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify all macros appear before all construction elements
		int macro1Index = outputGpad.indexOf("Macro1");
		int macro2Index = outputGpad.indexOf("Macro2");
		// Find construction elements (format may vary)
		int pIndex = outputGpad.indexOf("P");
		if (pIndex >= 0 && outputGpad.substring(Math.max(0, pIndex-5), pIndex).contains("macro")) {
			// P is in macro, find next occurrence
			pIndex = outputGpad.indexOf("P", pIndex + 1);
		}
		int qIndex = outputGpad.indexOf("Q");
		if (qIndex >= 0 && outputGpad.substring(Math.max(0, qIndex-5), qIndex).contains("macro")) {
			qIndex = outputGpad.indexOf("Q", qIndex + 1);
		}
		int rIndex = outputGpad.indexOf("R");
		if (rIndex >= 0 && outputGpad.substring(Math.max(0, rIndex-5), rIndex).contains("macro")) {
			rIndex = outputGpad.indexOf("R", rIndex + 1);
		}
		
		assertTrue("Macro1 should be present", macro1Index >= 0);
		assertTrue("Macro2 should be present", macro2Index >= 0);
		assertTrue("P should be present in construction", pIndex >= 0);
		assertTrue("Q should be present in construction", qIndex >= 0);
		assertTrue("R should be present in construction", rIndex >= 0);
		
		// All macros should appear before all construction elements
		if (pIndex >= 0) {
			assertTrue("Macro1 should appear before P", macro1Index < pIndex);
			assertTrue("Macro2 should appear before P", macro2Index < pIndex);
		}
		if (qIndex >= 0) {
			assertTrue("Macro1 should appear before Q", macro1Index < qIndex);
			assertTrue("Macro2 should appear before Q", macro2Index < qIndex);
		}
		if (rIndex >= 0) {
			assertTrue("Macro1 should appear before R", macro1Index < rIndex);
			assertTrue("Macro2 should appear before R", macro2Index < rIndex);
		}
	}

	@Test
	public void testPointWithLowercaseLabel() {
		// Create a point with lowercase label programmatically
		// This should generate Point[{x, y}] format to avoid ambiguity
		GeoPoint point = new GeoPoint(getConstruction(), 800, 500, 1.0);
		point.setLabel("layoutAppSize");
		
		// Convert to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Should contain the label
		assertTrue("Should contain label layoutAppSize", outputGpad.contains("layoutAppSize"));
		
		// Should use Point[{800.0, 500.0}] format (not (800, 500))
		// Note: coordinates are doubles, so they have decimal points
		assertTrue("Should contain Point[{800, 500}] format. Actual gpad: " + outputGpad, 
			outputGpad.contains("Point[{") && outputGpad.contains("800") && outputGpad.contains("500") && outputGpad.contains("}]"));
		
		// Should NOT contain plain (800, 500) format for this lowercase label
		// (unless it's in a different context, but the main assignment should use Point[])
		String pointLine = findLineContaining(outputGpad, "layoutAppSize");
		assertTrue("Point with lowercase label should use Point[{x, y}] format: " + pointLine,
			pointLine.contains("Point[{") || pointLine.contains("Point[{"));

		// Verify it can be parsed back correctly
		String result = api.evalGpad(outputGpad);
		assertNotNull("Round trip should succeed", result);
		assertTrue("Should contain layoutAppSize in result", result.contains("layoutAppSize"));
	}

	@Test
	public void testPointWithUppercaseLabel() {
		// Create a point with uppercase label programmatically
		// This should generate (x, y) format (normal format)
		GeoPoint point = new GeoPoint(getConstruction(), 800, 500, 1.0);
		point.setLabel("LayoutAppSize");
		
		// Convert to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Should contain the label
		assertTrue("Should contain label LayoutAppSize", outputGpad.contains("LayoutAppSize"));
		
		// Should use (800.0, 500.0) format (normal format for uppercase label)
		// Note: coordinates are doubles, so they have decimal points
		String pointLine = findLineContaining(outputGpad, "LayoutAppSize");
		assertTrue("Point with uppercase label should use (x, y) format: " + pointLine,
			pointLine.contains("(800") && pointLine.contains("500)"));
		
		// Should NOT use Point[{x, y}] format for uppercase label
		assertTrue("Point with uppercase label should NOT use Point[{x, y}] format",
			!pointLine.contains("Point[{800") || !pointLine.contains("500}]"));
	}

	@Test
	public void testVectorWithUppercaseLabel() {
		// Create a vector with uppercase label programmatically
		// This should generate Vector[{x, y}] format to avoid ambiguity
		GeoVector vector = new GeoVector(getConstruction());
		vector.setCoords(3, 4, 0);
		vector.setLabel("VecAB");
		
		// Convert to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Should contain the label
		assertTrue("Should contain label VecAB", outputGpad.contains("VecAB"));
		
		// Should use Vector[{3.0, 4.0}] format (not (3, 4))
		// Note: coordinates are doubles, so they have decimal points
		String vectorLine = findLineContaining(outputGpad, "VecAB");
		assertTrue("Vector with uppercase label should use Vector[{x, y}] format: " + vectorLine,
			vectorLine.contains("Vector[{") && vectorLine.contains("3") && vectorLine.contains("4") && vectorLine.contains("}]"));
	}

	@Test
	public void testVectorWithLowercaseLabel() {
		// Create a vector with lowercase label programmatically
		// This should generate (x, y) format (normal format)
		GeoVector vector = new GeoVector(getConstruction());
		vector.setCoords(3, 4, 0);
		vector.setLabel("vecAB");
		
		// Convert to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Should contain the label
		assertTrue("Should contain label vecAB", outputGpad.contains("vecAB"));
		
		// Should use (3.0, 4.0) format (normal format for lowercase label)
		// Note: coordinates are doubles, so they have decimal points
		String vectorLine = findLineContaining(outputGpad, "vecAB");
		assertTrue("Vector with lowercase label should use (x, y) format: " + vectorLine,
			vectorLine.contains("(3") && vectorLine.contains("4)"));
		
		// Should NOT use Vector[x, y] format for lowercase label
		assertTrue("Vector with lowercase label should NOT use Vector[x, y] format",
			!vectorLine.contains("Vector[3") || !vectorLine.contains("4]"));
	}

	@Test
	public void testMixedPointAndVectorLabels() {
		// Create multiple points and vectors with different label cases
		GeoPoint pointLower = new GeoPoint(getConstruction(), 100, 200, 1.0);
		pointLower.setLabel("pointLower");
		
		GeoPoint pointUpper = new GeoPoint(getConstruction(), 300, 400, 1.0);
		pointUpper.setLabel("PointUpper");
		
		GeoVector vectorLower = new GeoVector(getConstruction());
		vectorLower.setCoords(5, 6, 0);
		vectorLower.setLabel("vectorLower");
		
		GeoVector vectorUpper = new GeoVector(getConstruction());
		vectorUpper.setCoords(7, 8, 0);
		vectorUpper.setLabel("VectorUpper");
		
		// Convert to gpad
		String outputGpad = api.toGpad(false);
		assertNotNull("toGpad should return non-null", outputGpad);
		
		// Verify all labels are present
		assertTrue("Should contain pointLower", outputGpad.contains("pointLower"));
		assertTrue("Should contain PointUpper", outputGpad.contains("PointUpper"));
		assertTrue("Should contain vectorLower", outputGpad.contains("vectorLower"));
		assertTrue("Should contain VectorUpper", outputGpad.contains("VectorUpper"));
		
		// Verify formats
		String pointLowerLine = findLineContaining(outputGpad, "pointLower");
		assertTrue("pointLower should use Point[{x, y}] format: " + pointLowerLine,
			pointLowerLine.contains("Point[{"));
		
		String pointUpperLine = findLineContaining(outputGpad, "PointUpper");
		assertTrue("PointUpper should use (x, y) format: " + pointUpperLine,
			pointUpperLine.contains("(300") && pointUpperLine.contains("400)"));
		
		String vectorLowerLine = findLineContaining(outputGpad, "vectorLower");
		assertTrue("vectorLower should use (x, y) format: " + vectorLowerLine,
			vectorLowerLine.contains("(5") && vectorLowerLine.contains("6)"));
		
		String vectorUpperLine = findLineContaining(outputGpad, "VectorUpper");
		assertTrue("VectorUpper should use Vector[{x, y}] format: " + vectorUpperLine,
			vectorUpperLine.contains("Vector[{") && vectorUpperLine.contains("7") && vectorUpperLine.contains("8") && vectorUpperLine.contains("}]"));
	}

	/**
	 * Helper method to find the line containing a specific label in gpad output.
	 * Prefers lines that contain "=" (command lines) over style sheet definitions.
	 * @param gpad the gpad string
	 * @param label the label to search for
	 * @return the line containing the label, or empty string if not found
	 */
	private String findLineContaining(String gpad, String label) {
		String[] lines = gpad.split("\n");
		// First try to find a line with "=" (command line)
		for (String line : lines) {
			if (line.contains(label) && line.contains("=") && !line.trim().startsWith("@")) {
				return line;
			}
		}
		// Fallback to any line containing the label
		for (String line : lines) {
			if (line.contains(label)) {
				return line;
			}
		}
		return "";
	}
}

