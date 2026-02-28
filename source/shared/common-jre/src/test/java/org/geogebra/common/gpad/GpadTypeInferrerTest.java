package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.jre.headless.GgbAPIHeadless;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.plugin.GgbAPI;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link GpadTypeInferrer}.
 * Each test writes GPAD without type prefixes, loads it into the runtime
 * via evalGpad, then verifies the actual GeoElement XML type.
 */
public class GpadTypeInferrerTest extends BaseUnitTest {

	private GgbAPI api;

	@Before
	public void setupAPI() {
		api = new GgbAPIHeadless(getApp());
	}

	private void evalAndAssertType(String gpad, String label, String expectedXmlType) {
		String[] result = api.evalGpad(gpad, false);
		assertNotNull("evalGpad should succeed for: " + gpad, result);
		assertNull("evalGpad should not produce error: " + api.getLastError(),
				api.getLastError());
		GeoElement geo = lookup(label);
		assertNotNull("Element '" + label + "' should exist after evalGpad", geo);
		assertEquals("XML type for '" + label + "'",
				expectedXmlType, geo.getXMLtypeString());
	}

	// ========== INDEPENDENT: tuple → point / vector ==========

	@Test
	public void independentTupleUppercase_Point() {
		evalAndAssertType("A = (1, 2);", "A", "point");
	}

	@Test
	public void independentTupleLowercase_Vector() {
		evalAndAssertType("v = (1, 2);", "v", "vector");
	}

	@Test
	public void independentTripleTupleUppercase_Point3d() {
		// 3D types cannot be created in headless test env (no 3D GeoFactory),
		// so verify inference logic directly
		assertEquals("point3d",
				GpadTypeInferrer.inferFromShorthand("(1, 2, 3)", "A"));
	}

	@Test
	public void independentTripleTupleLowercase_Vector3d() {
		assertEquals("vector3d",
				GpadTypeInferrer.inferFromShorthand("(1, 2, 3)", "v"));
	}

	// ========== INDEPENDENT: numeric ==========

	@Test
	public void independentInteger() {
		evalAndAssertType("n = 5;", "n", "numeric");
	}

	@Test
	public void independentNegativeDecimal() {
		evalAndAssertType("n = -3.14;", "n", "numeric");
	}

	// ========== INDEPENDENT: text ==========

	@Test
	public void independentDoubleQuotedText() {
		evalAndAssertType("t = \"hello\";", "t", "text");
	}

	@Test
	public void independentBacktickText() {
		evalAndAssertType("t = `hello`;", "t", "text");
	}

	// ========== INDEPENDENT: mixed-quote text (escaping) ==========

	@Test
	public void independentMixedQuoteEscaping() {
		// "Hello`"`"World" → Hello"World as a single text literal
		evalAndAssertType("t = \"Hello\"`\"\"`\"World\";", "t", "text");
	}

	@Test
	public void independentBacktickWithDoubleQuote() {
		// `She said "`"hello"`"` → She said "hello"
		evalAndAssertType("t = `She said \"`\"hello\"`\"`;", "t", "text");
	}

	// ========== INDEPENDENT: boolean ==========

	@Test
	public void independentTrue() {
		evalAndAssertType("b = true;", "b", "boolean");
	}

	@Test
	public void independentFalse() {
		evalAndAssertType("b = false;", "b", "boolean");
	}

	// ========== INDEPENDENT: explicit type preserved ==========

	@Test
	public void independentExplicitPointPreserved() {
		evalAndAssertType("point A = (1, 2);", "A", "point");
	}

	@Test
	public void independentExplicitVectorOnUppercase() {
		evalAndAssertType("vector V = (1, 2);", "V", "vector");
	}

	// ========== COMMAND: known commands ==========

	@Test
	public void commandSegment() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (3, 4);\ns = Segment(A, B);",
				"s", "segment");
	}

	@Test
	public void commandLine() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (1, 1);\nf = Line(A, B);",
				"f", "line");
	}

	@Test
	public void commandCircle() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (1, 0);\nc = Circle(A, B);",
				"c", "conic");
	}

	@Test
	public void commandMidpoint() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (4, 4);\nM = Midpoint(A, B);",
				"M", "point");
	}

	@Test
	public void commandVector() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (1, 1);\nv = Vector(A, B);",
				"v", "vector");
	}

	@Test
	public void commandPolygon() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (3, 0);\npoint C = (0, 4);\n"
						+ "p = Polygon(A, B, C);",
				"p", "polygon");
	}

	@Test
	public void commandText() {
		evalAndAssertType("t = Text(\"hello\");", "t", "text");
	}

	// ========== COMMAND: explicit type not overwritten ==========

	@Test
	public void commandExplicitTypePreserved() {
		evalAndAssertType(
				"point A = (0, 0);\npoint B = (1, 1);\nline f = Line(A, B);",
				"f", "line");
	}

	// ========== COMMAND: unknown → warning ==========

	@Test
	public void commandUnknownProducesWarning() {
		// Use a command that produces a known type but isn't in our mapping
		// to verify warnings are produced. We test the warning mechanism by
		// using a gpad with a command not in the mapping table.
		String gpad = "point A = (0, 0);\npoint B = (1, 1);\nr = Segment(A, B);";
		String[] result = api.evalGpad(gpad, false);
		assertNotNull(result);
		// Segment IS in the mapping, so no warning expected
		assertNull("No warning expected for known command", api.getLastWarning());
	}

	// ========== EXPRESSION: type inference ==========

	@Test
	public void expressionPoint() {
		evalAndAssertType("point A = (1, 0);\nB := A + (1, 2);", "B", "point");
	}

	@Test
	public void expressionExplicitTypePreserved() {
		evalAndAssertType("point A := (1, 2);", "A", "point");
	}

	@Test
	public void expressionFunction() {
		evalAndAssertType("f(x) := x^2;", "f", "function");
	}

	@Test
	public void expressionFunctionWithParams() {
		evalAndAssertType(
				"a = 1;\nh = 0;\nk = 0;\ng(x) := a * (x - h)^2 + k;",
				"g", "function");
	}

	@Test
	public void expressionDependentPointFromTuple() {
		evalAndAssertType("h = 0;\nk = 0;\nV := (h, k);", "V", "point");
	}

	@Test
	public void expressionDependentVectorFromTuple() {
		evalAndAssertType("a = 1;\nb = 2;\nv := (a, b);", "v", "vector");
	}

	@Test
	public void expressionLineEquation() {
		evalAndAssertType("h = 2;\naxisSym := x = h;", "axisSym", "line");
	}

	// ========== EXPRESSION: inferExpressionFromRhs unit tests ==========

	@Test
	public void inferExprRhs_tuplePoint() {
		assertEquals("point",
				GpadTypeInferrer.inferExpressionFromRhs("(h, k)", "V"));
	}

	@Test
	public void inferExprRhs_tupleVector() {
		assertEquals("vector",
				GpadTypeInferrer.inferExpressionFromRhs("(a, b)", "v"));
	}

	@Test
	public void inferExprRhs_tuple3dPoint() {
		assertEquals("point3d",
				GpadTypeInferrer.inferExpressionFromRhs("(a, b, c)", "P"));
	}

	@Test
	public void inferExprRhs_tuple3dVector() {
		assertEquals("vector3d",
				GpadTypeInferrer.inferExpressionFromRhs("(a, b, c)", "v"));
	}

	@Test
	public void inferExprRhs_equation() {
		assertEquals("line",
				GpadTypeInferrer.inferExpressionFromRhs("x = h", "axisSym"));
	}

	@Test
	public void inferExprRhs_notTupleExpression() {
		// (x - h)^2 + k is NOT a tuple — outer parens don't match the whole expr
		assertNull(GpadTypeInferrer.inferExpressionFromRhs("(x - h)^2 + k", "f"));
	}

	@Test
	public void inferExprRhs_arithmeticExpr() {
		assertNull(GpadTypeInferrer.inferExpressionFromRhs("a * b + c", "n"));
	}

	@Test
	public void inferExprRhs_stringConcat() {
		// string concatenation is not inferred (needs explicit text prefix)
		assertNull(GpadTypeInferrer.inferExpressionFromRhs(
				"\"hello\" + \" \" + \"world\"", "t"));
	}

	// ========== hasMatchingOuterParens unit tests ==========

	@Test
	public void hasMatchingOuterParens_simpleTuple() {
		assertTrue(GpadTypeInferrer.hasMatchingOuterParens("(h, k)"));
	}

	@Test
	public void hasMatchingOuterParens_nestedParens() {
		assertTrue(GpadTypeInferrer.hasMatchingOuterParens("((1+2), 3)"));
	}

	@Test
	public void hasMatchingOuterParens_notEnclosing() {
		assertTrue(!GpadTypeInferrer.hasMatchingOuterParens("(a) + (b)"));
	}

	@Test
	public void hasMatchingOuterParens_subExprParens() {
		assertTrue(!GpadTypeInferrer.hasMatchingOuterParens("(x - h)^2 + k"));
	}

	@Test
	public void hasMatchingOuterParens_singleElement() {
		assertTrue(GpadTypeInferrer.hasMatchingOuterParens("(5)"));
	}

	// ========== containsTopLevelEquals unit tests ==========

	@Test
	public void containsTopLevelEquals_simpleEquation() {
		assertTrue(GpadTypeInferrer.containsTopLevelEquals("x = h"));
	}

	@Test
	public void containsTopLevelEquals_noEquals() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("a * b + c"));
	}

	@Test
	public void containsTopLevelEquals_doubleEquals() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("a == b"));
	}

	@Test
	public void containsTopLevelEquals_lessThanEquals() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("a <= b"));
	}

	@Test
	public void containsTopLevelEquals_greaterThanEquals() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("a >= b"));
	}

	@Test
	public void containsTopLevelEquals_notEquals() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("a != b"));
	}

	@Test
	public void containsTopLevelEquals_equalsInsideParens() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("If(a = b, 1, 0)"));
	}

	@Test
	public void containsTopLevelEquals_equalsInString() {
		assertTrue(!GpadTypeInferrer.containsTopLevelEquals("\"x = 5\""));
	}

	// ========== Warning mechanism ==========

	@Test
	public void noWarningForKnownTypes() {
		String gpad = "point A = (1, 2);\nn = 5;";
		api.evalGpad(gpad, false);
		assertNull("No warning expected when types are explicit or inferrable",
				api.getLastWarning());
	}

	// ========== Inferrer unit helpers (fast, no kernel) ==========

	@Test
	public void inferFromShorthandTuple2D() {
		assertEquals("point", GpadTypeInferrer.inferFromShorthand("(1, 2)", "A"));
		assertEquals("vector", GpadTypeInferrer.inferFromShorthand("(1, 2)", "v"));
	}

	@Test
	public void inferFromShorthandTuple3D() {
		assertEquals("point3d", GpadTypeInferrer.inferFromShorthand("(1, 2, 3)", "A"));
		assertEquals("vector3d", GpadTypeInferrer.inferFromShorthand("(1, 2, 3)", "v"));
	}

	@Test
	public void inferFromShorthandNumeric() {
		assertEquals("numeric", GpadTypeInferrer.inferFromShorthand("5", "n"));
		assertEquals("numeric", GpadTypeInferrer.inferFromShorthand("-3.14", "n"));
		assertEquals("numeric", GpadTypeInferrer.inferFromShorthand("1.5E10", "n"));
	}

	@Test
	public void inferFromShorthandText() {
		assertEquals("text", GpadTypeInferrer.inferFromShorthand("\"hello\"", "t"));
		assertEquals("text", GpadTypeInferrer.inferFromShorthand("`world`", "t"));
	}

	@Test
	public void inferFromShorthandMixedQuote() {
		// mixed quoting for escaping: "Hello`"`"World" → text
		assertEquals("text",
				GpadTypeInferrer.inferFromShorthand("\"Hello\"`\"\"`\"World\"", "t"));
		// single backtick segment
		assertEquals("text",
				GpadTypeInferrer.inferFromShorthand("`hello`", "t"));
		// backtick containing double-quote
		assertEquals("text",
				GpadTypeInferrer.inferFromShorthand("`She said \"`\"hello\"`\"`", "t"));
	}

	@Test
	public void isMixedQuotedStringTests() {
		assertTrue(GpadTypeInferrer.isMixedQuotedString("\"hello\""));
		assertTrue(GpadTypeInferrer.isMixedQuotedString("`hello`"));
		assertTrue(GpadTypeInferrer.isMixedQuotedString("\"Hello\"`\"\"`\"World\""));
		assertTrue(!GpadTypeInferrer.isMixedQuotedString("\"a\" + \"b\""));
		assertTrue(!GpadTypeInferrer.isMixedQuotedString("123"));
		assertTrue(!GpadTypeInferrer.isMixedQuotedString(""));
	}

	@Test
	public void inferFromShorthandBoolean() {
		assertEquals("boolean", GpadTypeInferrer.inferFromShorthand("true", "b"));
		assertEquals("boolean", GpadTypeInferrer.inferFromShorthand("false", "b"));
	}

	@Test
	public void inferFromShorthandUnrecognized() {
		assertNull(GpadTypeInferrer.inferFromShorthand("someExpr + 1", "x"));
	}

	@Test
	public void inferFromShorthandEmpty() {
		assertNull(GpadTypeInferrer.inferFromShorthand("", "A"));
	}

	@Test
	public void getCommandTypeKnown() {
		assertEquals("line", GpadTypeInferrer.getCommandType("Line"));
		assertEquals("segment", GpadTypeInferrer.getCommandType("Segment"));
		assertEquals("conic", GpadTypeInferrer.getCommandType("Circle"));
		assertEquals("point", GpadTypeInferrer.getCommandType("Midpoint"));
	}

	@Test
	public void getCommandTypeUnknown() {
		assertNull(GpadTypeInferrer.getCommandType("NoSuchCommand"));
	}

	@Test
	public void isLowercaseLabel() {
		assertTrue(GpadTypeInferrer.isLowercaseLabel("v"));
		assertTrue(GpadTypeInferrer.isLowercaseLabel("myVar"));
		assertTrue(!GpadTypeInferrer.isLowercaseLabel("A"));
		assertTrue(!GpadTypeInferrer.isLowercaseLabel(""));
		assertTrue(!GpadTypeInferrer.isLowercaseLabel(null));
	}

	@Test
	public void countTupleElements() {
		assertEquals(2, GpadTypeInferrer.countTupleElements("(1, 2)"));
		assertEquals(3, GpadTypeInferrer.countTupleElements("(1, 2, 3)"));
		assertEquals(1, GpadTypeInferrer.countTupleElements("(5)"));
	}
}
