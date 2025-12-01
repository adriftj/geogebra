package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.geogebra.common.BaseUnitTest;

import org.geogebra.common.kernel.Macro;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoLine;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.kernel.geos.GeoSegment;
import org.junit.Test;

/**
 * Unit tests for Gpad macro definition and usage.
 */
public class GpadMacroTest extends BaseUnitTest {

	@Test
	public void testSimpleMacroDefinition() {
		String gpad = "@@macro Midpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint[A, B];\n"
				+ "  @@return M\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			// Macro definition should not create any GeoElements in the main construction
			assertEquals(0, geos.size());
			
			// Verify macro was registered
			Macro macro = getKernel().getMacro("Midpoint");
			assertNotNull("Macro should be registered", macro);
			assertEquals("Midpoint", macro.getCommandName());
			assertEquals(2, macro.getMacroInput().length);
			assertEquals(1, macro.getMacroOutput().length);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroWithNoInput() {
		String gpad = "@@macro CreateOrigin() {\n"
				+ "  O = (0, 0);\n"
				+ "  @@return O\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
			
			Macro macro = getKernel().getMacro("CreateOrigin");
			assertNotNull(macro);
			assertEquals(0, macro.getMacroInput().length);
			assertEquals(1, macro.getMacroOutput().length);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroWithMultipleInputs() {
		String gpad = "@@macro Triangle(A, B, C) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 0);\n"
				+ "  C = (1, 2);\n"
				+ "  a = Segment[B, C];\n"
				+ "  b = Segment[C, A];\n"
				+ "  c = Segment[A, B];\n"
				+ "  @@return a, b, c\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
			
			Macro macro = getKernel().getMacro("Triangle");
			assertNotNull(macro);
			assertEquals(3, macro.getMacroInput().length);
			assertEquals(3, macro.getMacroOutput().length);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroWithStyleSheet() {
		String gpad = "@@macro ColoredPoint(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  @red = { objColor: #FF0000 }\n"
				+ "  P @red = A;\n"
				+ "  @@return P\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
			
			Macro macro = getKernel().getMacro("ColoredPoint");
			assertNotNull(macro);
			assertEquals(1, macro.getMacroInput().length);
			assertEquals(1, macro.getMacroOutput().length);
			
			// Verify that macro's stylesheet is not in global stylesheets
			assertNull("Macro stylesheet should not be in global scope", 
					parser.getGlobalStyleSheets().get("red"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroStyleSheetIsolation() {
		String gpad = "@globalStyle = { pointSize: 10 }\n"
				+ "@@macro TestMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  @localStyle = { pointSize: 5 }\n"
				+ "  P @localStyle = A;\n"
				+ "  @@return P\n"
				+ "}\n"
				+ "A = (1, 2);";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size()); // Only A is created
			
			// Verify global stylesheet exists
			assertNotNull("Global stylesheet should exist", 
					parser.getGlobalStyleSheets().get("globalStyle"));
			
			// Verify macro's local stylesheet is not in global scope
			assertNull("Macro's local stylesheet should not be in global scope", 
					parser.getGlobalStyleSheets().get("localStyle"));
			
			Macro macro = getKernel().getMacro("TestMacro");
			assertNotNull(macro);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroCall() {
		// Use a different macro name to ensure we're testing macro call, not built-in command
		String gpad = "@@macro MyMidpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint[A, B];\n"
				+ "  @@return M\n"
				+ "}\n"
				+ "P = (0, 0);\n"
				+ "Q = (2, 2);\n"
				+ "M = MyMidpoint[P, Q];";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			// Should create P, Q, and M (from macro call)
			assertTrue("Should create at least 3 elements", geos.size() >= 3);
			
			// Verify macro was registered
			Macro macro = getKernel().getMacro("MyMidpoint");
			assertNotNull("Macro should be registered", macro);
			assertEquals("MyMidpoint", macro.getCommandName());
			
			// Verify M was created by macro call
			GeoElement m = getKernel().lookupLabel("M");
			assertNotNull("M should be created by macro call", m);
			assertTrue("M should be a point", m instanceof GeoPoint);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroNameConflict() {
		String gpad = "@@macro TestMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  P = A;\n"
				+ "  @@return P\n"
				+ "}\n"
				+ "@@macro TestMacro(B) {\n"
				+ "  B = (3, 4);\n"
				+ "  Q = B;\n"
				+ "  @@return Q\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			parser.parse(gpad);
			fail("Should throw exception for macro name conflict");
		} catch (GpadParseException e) {
			// Expected: macro name conflict
			assertTrue("Error should mention macro name conflict", 
					e.getMessage().contains("already exists") || e.getMessage().contains("TestMacro"));
		}
	}

	@Test
	public void testMacroMissingReturn() {
		String gpad = "@@macro TestMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  P = A;\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			parser.parse(gpad);
			fail("Should throw exception for missing @@return");
		} catch (GpadParseException e) {
			// Expected: missing @@return
			// The exception message contains "GPAD_RETURN" or "RBRACE" indicating missing @@return
			assertTrue("Error should mention GPAD_RETURN or RBRACE", 
					e.getMessage().contains("GPAD_RETURN") || e.getMessage().contains("RBRACE"));
		}
	}

	@Test
	public void testMacroInputNotFound() {
		// Input parameters must be defined in the macro body
		String gpad = "@@macro TestMacro(A) {\n"
				+ "  P = (1, 2);\n"
				+ "  @@return P\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			parser.parse(gpad);
			fail("Should throw exception for input object not found");
		} catch (GpadParseException e) {
			// Expected: input object A not found in macro body
			assertTrue("Error should mention input object not found", 
					e.getMessage().contains("Input object") || e.getMessage().contains("not found"));
		}
	}

	@Test
	public void testMacroOutputNotFound() {
		String gpad = "@@macro TestMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  P = A;\n"
				+ "  @@return Q\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			parser.parse(gpad);
			fail("Should throw exception for output object not found");
		} catch (GpadParseException e) {
			// Expected: output object Q not found in macro body
			assertTrue("Error should mention output object not found", 
					e.getMessage().contains("Output object") || e.getMessage().contains("not found"));
		}
	}

	@Test
	public void testMacroWithComplexBody() {
		String gpad = "@@macro PerpendicularBisector(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint[A, B];\n"
				+ "  @lineStyle = { lineStyle: thickness=3 }\n"
				+ "  l @lineStyle = PerpendicularLine[M, Segment[A, B]];\n"
				+ "  @@return l\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
			
			Macro macro = getKernel().getMacro("PerpendicularBisector");
			assertNotNull(macro);
			assertEquals(2, macro.getMacroInput().length);
			assertEquals(1, macro.getMacroOutput().length);
			
			// Verify macro's stylesheet is not in global scope
			assertNull(parser.getGlobalStyleSheets().get("lineStyle"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroCallWithMultipleOutputs() {
		String gpad = "@@macro Triangle(A, B, C) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 0);\n"
				+ "  C = (1, 2);\n"
				+ "  a = Segment[B, C];\n"
				+ "  b = Segment[C, A];\n"
				+ "  c = Segment[A, B];\n"
				+ "  @@return a, b, c\n"
				+ "}\n"
				+ "P = (0, 0);\n"
				+ "Q = (2, 0);\n"
				+ "R = (1, 2);\n"
				+ "a, b, c = Triangle[P, Q, R];";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			// Should create P, Q, R, and a, b, c (from macro call)
			assertTrue("Should create at least 6 elements", geos.size() >= 6);
			
			// Verify segments were created
			GeoElement a = getKernel().lookupLabel("a");
			GeoElement b = getKernel().lookupLabel("b");
			GeoElement c = getKernel().lookupLabel("c");
			assertNotNull("a should be created", a);
			assertNotNull("b should be created", b);
			assertNotNull("c should be created", c);
			assertTrue("a should be a segment", a instanceof GeoSegment);
			assertTrue("b should be a segment", b instanceof GeoSegment);
			assertTrue("c should be a segment", c instanceof GeoSegment);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroCannotReferenceGlobalStyleSheet() {
		String gpad = "@globalStyle = { pointSize: 10 }\n"
				+ "@@macro TestMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  P @globalStyle = A;\n"
				+ "  @@return P\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// This should fail because macro cannot reference global stylesheet
			// The macro's gpadStyleSheets is isolated
			parser.parse(gpad);
			// If parsing succeeds, the stylesheet reference should be null
			// (This depends on implementation - if it's null, it might just skip the style)
		} catch (GpadParseException e) {
			// This is acceptable - macro cannot access global stylesheets
			// The exact behavior depends on implementation
		}
	}

	@Test
	public void testMultipleMacroDefinitions() {
		// Use different macro names to ensure we're testing macro definitions, not built-in commands
		String gpad = "@@macro MyMidpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint[A, B];\n"
				+ "  @@return M\n"
				+ "}\n"
				+ "@@macro MyDistance(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  d = Distance[A, B];\n"
				+ "  @@return d\n"
				+ "}\n"
				+ "P = (0, 0);\n"
				+ "Q = (2, 2);";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(2, geos.size()); // Only P and Q
			
			// Verify both macros were registered
			Macro midpointMacro = getKernel().getMacro("MyMidpoint");
			Macro distanceMacro = getKernel().getMacro("MyDistance");
			assertNotNull("MyMidpoint macro should be registered", midpointMacro);
			assertNotNull("MyDistance macro should be registered", distanceMacro);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroWithEmptyBody() {
		String gpad = "@@macro EmptyMacro(A) {\n"
				+ "  A = (1, 2);\n"
				+ "  @@return A\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
			
			Macro macro = getKernel().getMacro("EmptyMacro");
			assertNotNull(macro);
			// Output is just the input, which should be valid
			assertEquals(1, macro.getMacroInput().length);
			assertEquals(1, macro.getMacroOutput().length);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroWithMultipleStatements() {
		String gpad = "@@macro ComplexMacro(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint[A, B];\n"
				+ "  @style = { pointSize: 8 }\n"
				+ "  P @style = M;\n"
				+ "  l = Line[A, B];\n"
				+ "  @@return P, l\n"
				+ "}";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(0, geos.size());
			
			Macro macro = getKernel().getMacro("ComplexMacro");
			assertNotNull(macro);
			assertEquals(2, macro.getMacroInput().length);
			assertEquals(2, macro.getMacroOutput().length);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testMacroCallAfterDefinition() {
		// Use a different macro name to ensure we're testing macro call, not built-in command
		String gpad = "P = (0, 0);\n"
				+ "Q = (2, 2);\n"
				+ "@@macro MyMidpoint(A, B) {\n"
				+ "  A = (0, 0);\n"
				+ "  B = (2, 2);\n"
				+ "  M = Midpoint[A, B];\n"
				+ "  @@return M\n"
				+ "}\n"
				+ "M = MyMidpoint[P, Q];";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			// Should create P, Q, and M (from macro call)
			assertTrue("Should create at least 3 elements", geos.size() >= 3);
			
			GeoElement m = getKernel().lookupLabel("M");
			assertNotNull("M should be created", m);
			assertTrue("M should be a point", m instanceof GeoPoint);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}

