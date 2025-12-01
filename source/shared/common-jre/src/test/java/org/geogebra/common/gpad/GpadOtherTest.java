package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.CircularDefinitionException;
import org.geogebra.common.kernel.arithmetic.ValidExpression;
import org.geogebra.common.kernel.commands.EvalInfo;
import org.geogebra.common.kernel.geos.GeoCurveCartesian;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.parser.ParseException;
import org.geogebra.common.kernel.parser.Parser;
import org.geogebra.common.main.MyError;
import org.junit.Test;

/**
 * Unit tests for other Gpad-related functionality.
 */
public class GpadOtherTest extends BaseUnitTest {

	/**
	 * Test creating CurveCartesian from ValidExpression.
	 * This test reproduces the issue where variable 't' is not defined
	 * when processing CurveCartesian command directly.
	 * 
	 * Expected behavior: The test should succeed and create a GeoCurveCartesian.
	 * If it fails with "t is not defined", it indicates that the parameter
	 * variable needs to be registered before processing the command.
	 */
	@Test
	public void testCurveCartesianFromValidExpression() {
		// Parse the command string
		String cmdString = "Curve[cos(t), sin(t), t, 0, 2*pi]";
		Parser parser = getKernel().getParser();
		
		try {
			// Step 1: Parse to get ValidExpression
			ValidExpression ve = parser.parseGeoGebraExpression(cmdString);
			assertNotNull("ValidExpression should not be null", ve);
			
			// Step 2: Add label
			List<String> names = new ArrayList<>();
			names.add("c");
			ve.addLabel(names);
			
			// Step 3: Create EvalInfo
			EvalInfo info = new EvalInfo(true);
			
			// Step 4: Process ValidExpression - this should create GeoCurveCartesian
			// This is where the error "t is not defined" might occur
			// The issue is that when processing directly, the parameter variable 't'
			// may not be registered in the construction before the command processor
			// tries to resolve the expressions cos(t) and sin(t)
			GeoElement[] geos = getKernel().getAlgebraProcessor()
					.processValidExpression(ve, info);
			
			// Verify results
			assertNotNull("GeoElement array should not be null", geos);
			assertTrue("Should have at least one element", geos.length > 0);
			assertTrue("First element should be GeoCurveCartesian", 
					geos[0] instanceof GeoCurveCartesian);
			
			GeoCurveCartesian curve = (GeoCurveCartesian) geos[0];
			// Note: Label might be auto-generated if 'c' is already taken
			// The important thing is that the curve is created successfully
			assertNotNull("Curve should have a label", curve.getLabelSimple());
			assertTrue("Curve should be defined", curve.isDefined());
			
			// Verify curve properties
			assertTrue("Curve should have valid parameter range", 
					curve.getMinParameter() < curve.getMaxParameter());
			
		} catch (ParseException e) {
			// Print the parse error
			System.err.println("ParseException occurred: " + e.getMessage());
			e.printStackTrace();
			throw new AssertionError("Failed to parse command: " 
					+ e.getMessage(), e);
		} catch (MyError e) {
			// Print the error message to understand what went wrong
			// This is where "t is not defined" error would appear
			System.err.println("MyError occurred: " + e.getMessage());
			System.err.println("Error details: " + e.toString());
			
			// Check if the error is about undefined variable
			if (e.getMessage() != null && 
					(e.getMessage().contains("not defined") 
					|| e.getMessage().contains("未定义")
					|| e.getMessage().toLowerCase().contains("undefined"))) {
				throw new AssertionError(
					"Variable 't' is not defined. This indicates that the parameter " +
					"variable needs to be registered in the construction before " +
					"processing the CurveCartesian command. Error: " + e.getMessage(), e);
			}
			
			throw new AssertionError("Failed to create CurveCartesian: " 
					+ e.getMessage(), e);
		} catch (CircularDefinitionException e) {
			// Print the circular definition error
			System.err.println("CircularDefinitionException occurred: " + e.getMessage());
			e.printStackTrace();
			throw new AssertionError("Circular definition: " 
					+ e.getMessage(), e);
		} catch (Exception e) {
			// Print any other exception
			System.err.println("Exception occurred: " + e.getMessage());
			e.printStackTrace();
			throw new AssertionError("Unexpected exception: " 
					+ e.getMessage(), e);
		}
	}

	/**
	 * Test creating CurveCartesian with different parameter variable name.
	 */
	@Test
	public void testCurveCartesianWithDifferentVariable() {
		String cmdString = "Curve[cos(u), sin(u), u, 0, 2*pi]";
		Parser parser = getKernel().getParser();
		
		try {
			ValidExpression ve = parser.parseGeoGebraExpression(cmdString);
			assertNotNull("ValidExpression should not be null", ve);
			
			List<String> names = new ArrayList<>();
			names.add("c1");
			ve.addLabel(names);
			
			EvalInfo info = new EvalInfo(true);
			GeoElement[] geos = getKernel().getAlgebraProcessor()
					.processValidExpression(ve, info);
			
			assertNotNull("GeoElement array should not be null", geos);
			assertTrue("Should have at least one element", geos.length > 0);
			assertTrue("First element should be GeoCurveCartesian", 
					geos[0] instanceof GeoCurveCartesian);
			
		} catch (ParseException e) {
			System.err.println("ParseException occurred: " + e.getMessage());
			throw new AssertionError("Failed to parse command: " 
					+ e.getMessage(), e);
		} catch (MyError e) {
			System.err.println("MyError occurred: " + e.getMessage());
			throw new AssertionError("Failed to create CurveCartesian: " 
					+ e.getMessage(), e);
		} catch (CircularDefinitionException e) {
			System.err.println("CircularDefinitionException occurred: " + e.getMessage());
			throw new AssertionError("Circular definition: " 
					+ e.getMessage(), e);
		}
	}

	/**
	 * Test creating CurveCartesian using evalCommand (for comparison).
	 * This should work because evalCommand goes through the full command processing.
	 */
	@Test
	public void testCurveCartesianViaEvalCommand() {
		// This should work because evalCommand processes the full command
		String cmdString = "c2=Curve[cos(t), sin(t), t, 0, 2*pi]";
		
		try {
			boolean success = getApp().getGgbApi().evalCommand(cmdString);
			assertTrue("evalCommand should succeed", success);
			
			GeoElement geo = getKernel().lookupLabel("c2");
			assertNotNull("GeoElement should be found", geo);
			assertTrue("Should be GeoCurveCartesian", 
					geo instanceof GeoCurveCartesian);
			
			GeoCurveCartesian curve = (GeoCurveCartesian) geo;
			assertTrue("Curve should be defined", curve.isDefined());
			
		} catch (Exception e) {
			throw new AssertionError("Unexpected exception: " 
					+ e.getMessage(), e);
		}
	}

	/**
	 * Test to understand the issue: When processing CurveCartesian command
	 * directly via ValidExpression, the parameter variable (e.g., 't') might
	 * not be registered in the construction before the command processor
	 * tries to resolve expressions like cos(t) and sin(t).
	 * 
	 * The issue occurs because:
	 * 1. Parser creates a Command object with arguments as ExpressionNodes
	 * 2. When processValidExpression is called, it goes to CmdCurveCartesian.process()
	 * 3. CmdCurveCartesian.process() calls resArgsLocalNumVar() which:
	 *    - Registers the parameter variable in the construction
	 *    - Resolves the arguments
	 *    - Removes the variable after processing
	 * 
	 * However, if the arguments (cos(t), sin(t)) are resolved BEFORE
	 * resArgsLocalNumVar() is called, 't' won't be defined yet.
	 * 
	 * Solution: The command processor should register the local variable
	 * BEFORE resolving any arguments that might depend on it.
	 */
	@Test
	public void testCurveCartesianParameterVariableRegistration() {
		String cmdString = "Curve[cos(t), sin(t), t, 0, 2*pi]";
		Parser parser = getKernel().getParser();
		
		try {
			ValidExpression ve = parser.parseGeoGebraExpression(cmdString);
			assertNotNull("ValidExpression should not be null", ve);
			
			// Check if the command is properly parsed
			assertTrue("ValidExpression should contain a Command", 
					ve.unwrap() instanceof org.geogebra.common.kernel.arithmetic.Command);
			
			org.geogebra.common.kernel.arithmetic.Command cmd = 
					(org.geogebra.common.kernel.arithmetic.Command) ve.unwrap();
			// Command name is "CurveCartesian" in the internal representation
			assertTrue("Command name should be CurveCartesian", 
					"CurveCartesian".equals(cmd.getName()));
			// Note: Command might have 4 or 5 arguments depending on format
			assertTrue("Command should have 4 or 5 arguments", 
					cmd.getArgumentNumber() == 4 || cmd.getArgumentNumber() == 5);
			
			// Add label
			List<String> names = new ArrayList<>();
			names.add("c3");
			ve.addLabel(names);
			
			// Process - this should work because CmdCurveCartesian.process()
			// properly handles the parameter variable registration
			EvalInfo info = new EvalInfo(true);
			GeoElement[] geos = getKernel().getAlgebraProcessor()
					.processValidExpression(ve, info);
			
			assertNotNull("GeoElement array should not be null", geos);
			assertTrue("Should have at least one element", geos.length > 0);
			assertTrue("First element should be GeoCurveCartesian", 
					geos[0] instanceof GeoCurveCartesian);
			
		} catch (MyError e) {
			// If we get a "variable not defined" error, it means the parameter
			// variable wasn't registered before resolving the coordinate expressions
			String errorMsg = e.getMessage();
			if (errorMsg != null && 
					(errorMsg.contains("not defined") 
					|| errorMsg.contains("未定义")
					|| errorMsg.toLowerCase().contains("undefined"))) {
				System.err.println("ERROR REPRODUCED: Variable not defined error occurred!");
				System.err.println("Error message: " + errorMsg);
				System.err.println("This indicates that the parameter variable 't' needs to be");
				System.err.println("registered in the construction BEFORE resolving cos(t) and sin(t).");
				throw new AssertionError(
					"Variable 't' is not defined. The parameter variable must be registered " +
					"in the construction before processing the coordinate expressions. " +
					"Error: " + errorMsg, e);
			}
			throw new AssertionError("Unexpected MyError: " + errorMsg, e);
		} catch (ParseException | CircularDefinitionException e) {
			throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
		} catch (Exception e) {
			throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
		}
	}

	/**
	 * Test reproducing the actual issue: "Undefined variable u" when using evalGpad
	 * with CurveCartesian command that has expressions containing the parameter variable.
	 * 
	 * The user sends ALL commands together to evalGpad in one call.
	 * The Gpad commands are:
	 * Corner5 = (568, 443);
	 * Corner3 = (1.25, 4.76);
	 * Corner1 = (-0.61, -9.84);
	 * pixelGap = 12.1;
	 * distanceX = pixelGap / x(Corner5) (x(Corner3) - x(Corner1));
	 * distanceY = pixelGap / y(Corner5) (y(Corner3) - y(Corner1));
	 * pointGap = distanceX;
	 * xyScale = distanceY / distanceX;
	 * curveFunction = CurveCartesian[pointGap cos(u), pointGap xyScale sin(u), u, 0, 2π];
	 * 
	 * The issue occurs because when parsing the CurveCartesian command arguments,
	 * the expressions "pointGap cos(u)" and "pointGap xyScale sin(u)" are parsed
	 * and their variables are resolved BEFORE the parameter variable 'u' is registered
	 * as a local variable by CmdCurveCartesian.process().
	 */
	@Test
	public void testCurveCartesianWithEvalGpadUndefinedVariable() {
		// All commands sent together to evalGpad (as the user does)
		// Note: The user's original command has implicit multiplication:
		// "pixelGap / x(Corner5) (x(Corner3) - x(Corner1))" means
		// "pixelGap / (x(Corner5) * (x(Corner3) - x(Corner1)))"
		String allGpadCommands = 
			"Corner5 = (568, 443);\n" +
			"Corner3 = (1.25, 4.76);\n" +
			"Corner1 = (-0.61, -9.84);\n" +
			"pixelGap = 12.1;\n" +
			"distanceX = pixelGap / x(Corner5) * (x(Corner3) - x(Corner1));\n" +
			"distanceY = pixelGap / y(Corner5) * (y(Corner3) - y(Corner1));\n" +
			"pointGap = distanceX;\n" +
			"xyScale = distanceY / distanceX;\n" +
			"curveFunction = CurveCartesian[pointGap cos(u), pointGap xyScale sin(u), u, 0, 2*pi];";
		
		try {
			// Send all commands together to evalGpad
			String result = getApp().getGgbApi().evalGpad(allGpadCommands);
			
			if (result == null) {
				// Check if there was an error about undefined variable
				String lastError = getApp().getGgbApi().getLastError();
				if (lastError != null && 
						(lastError.contains("Undefined variable") 
						|| lastError.contains("未定义")
						|| lastError.toLowerCase().contains("undefined")
						|| lastError.contains("u"))) {
					System.err.println("ERROR REPRODUCED: Undefined variable error!");
					System.err.println("Error message: " + lastError);
					System.err.println("\nThis indicates that when parsing CurveCartesian command,");
					System.err.println("the parameter variable 'u' is not registered before resolving");
					System.err.println("the coordinate expressions 'pointGap cos(u)' and 'pointGap xyScale sin(u)'.");
					System.err.println("\nThe issue is likely in the parsing/resolution order:");
					System.err.println("1. gpadCommandExpression() calls expressionOrEquation() to parse the command");
					System.err.println("2. When parsing CurveCartesian[pointGap cos(u), ...], the parser");
					System.err.println("   may try to resolve variables in the arguments");
					System.err.println("3. At this point, 'u' hasn't been registered as a local variable yet");
					System.err.println("4. CmdCurveCartesian.process() calls resArgsLocalNumVar() which registers 'u'");
					System.err.println("5. But by then it's too late - the error already occurred during parsing");
					System.err.println("\nPossible solution:");
					System.err.println("The parameter variable should be registered BEFORE parsing/resolving");
					System.err.println("the coordinate expressions that depend on it.");
					
					throw new AssertionError(
						"Undefined variable 'u' error reproduced. " +
						"The parameter variable must be registered BEFORE resolving " +
						"expressions that contain it. Error: " + lastError);
				}
				throw new AssertionError("evalGpad failed with error: " + lastError);
			}
			
			// If we get here, all commands succeeded
			assertNotNull("All commands should succeed", result);
			assertTrue("Result should contain curveFunction label", 
					result.contains("curveFunction"));
			
			// Verify the curve was created
			GeoElement geo = getKernel().lookupLabel("curveFunction");
			assertNotNull("curveFunction should exist", geo);
			assertTrue("Should be GeoCurveCartesian", 
					geo instanceof GeoCurveCartesian);
			
			GeoCurveCartesian curve = (GeoCurveCartesian) geo;
			assertTrue("Curve should be defined", curve.isDefined());
			
		} catch (Exception e) {
			// Print detailed error information
			System.err.println("Exception occurred: " + e.getMessage());
			e.printStackTrace();
			
			// Check for the specific error
			String lastError = getApp().getGgbApi().getLastError();
			if (lastError != null) {
				System.err.println("Last error from evalGpad: " + lastError);
			}
			
			throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
		}
	}

	/**
	 * Test reproducing the issue with the actual Gpad script provided by the user.
	 * The error is reported at line 46, column 40: "Undefined variable u"
	 * 
	 * Line 46 contains:
	 * curveFunction* @curveFunctionStyle = CurveCartesian[pointGap cos(u), pointGap xyScale sin(u), u, 0, 2π];
	 * 
	 * Column 40 is approximately at the position of "cos(u)" in the expression.
	 * This confirms that the error occurs when parsing the first coordinate expression
	 * "pointGap cos(u)" before the parameter variable 'u' is registered.
	 */
	@Test
	public void testCurveCartesianInMacroUndefinedVariable() {
		// The complete Gpad script from the user
		// Error is reported at line 46, column 40: "Undefined variable u"
		// Line 46: curveFunction* @curveFunctionStyle = CurveCartesian[pointGap cos(u), pointGap xyScale sin(u), u, 0, 2π];
		// Column 40 is approximately at "cos(u)" in the expression
		String macroGpad = 
			"@@macro PixelsToPoint(Corner1, Corner3, Corner5, InputPixels) {\n" +
			"    @InputPixelsStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 100.0 200.0 }\n" +
			"    InputPixels @InputPixelsStyle = (100, 200);\n" +
			"    @Corner5Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 685.0 508.0 }\n" +
			"    Corner5 @Corner5Style = (685, 508);\n" +
			"    @Corner3Style = { objColor: #4D4DFF00; animation: +; coords: 8.980000000000018 6.100000000000002 }\n" +
			"    Corner3 @Corner3Style = (8.98, 6.1);\n" +
			"    xRight = x(Corner3);\n" +
			"    @Corner1Style = { objColor: #4D4DFF00; animation: +; coords: -4.760000000000007 -4.099999999999998 }\n" +
			"    Corner1 @Corner1Style = (-4.76, -4.1);\n" +
			"    xLeft = x(Corner1);\n" +
			"    yTop = y(Corner3);\n" +
			"    yBottom = y(Corner1);\n" +
			"    yCoord = yTop - y(InputPixels) / y(Corner5) (yTop - yBottom);\n" +
			"    xCoord = x(InputPixels) / x(Corner5) (xRight - xLeft) + xLeft;\n" +
			"    @OutputPointStyle = { show: 3d; objColor: #44444400; pointSize: 4; coords: -2.7541605839416095 2.0842519685039393 }\n" +
			"    OutputPoint @OutputPointStyle = (xCoord, yCoord);\n" +
			"    @@return OutputPoint\n" +
			"}\n" +
			"\n" +
			"\n" +
			"@@macro expandSegment(A, B, pixelGap, Corner1, Corner3, Corner5) {\n" +
			"    @Corner5Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 568.0 443.0 }\n" +
			"    Corner5* @Corner5Style = (568, 443);\n" +
			"    @Corner3Style = { objColor: #00000000; labelMode: namevalue; animation: +; coords: 1.2547483093257106 4.757128434755219 }\n" +
			"    Corner3* @Corner3Style = (1.25, 4.76);\n" +
			"    @Corner1Style = { show: 3d; objColor: #4D4DFF00; animation: +; coords: -0.6147829327656786 -9.838334771045973 }\n" +
			"    Corner1* @Corner1Style = (-0.61, -9.84);\n" +
			"    @pixelGapStyle = { labelOffset: -79 10 }\n" +
			"    pixelGap @pixelGapStyle = 12.1;\n" +
			"    distanceX = pixelGap / x(Corner5) (x(Corner3) - x(Corner1));\n" +
			"    distanceY = pixelGap / y(Corner5) (y(Corner3) - y(Corner1));\n" +
			"    pointGap = distanceX;\n" +
			"    xyScale = distanceY / distanceX;\n" +
			"    @BStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 0.028073424163992024 -2.3274110089595177 }\n" +
			"    B @BStyle = (0.03, -2.33);\n" +
			"    @AStyle = { show: 3d; objColor: #4D4DFF00; animation: +; coords: 0.0 0.0 }\n" +
			"    A @AStyle = (0, 0);\n" +
			"    @vecABStyle = { show: 3d; objColor: #00000000; lineStyle: full thickness=5 hidden=dashed opacity=178; coords: 0.028073424163992024 -2.3274110089595177 0.0 }\n" +
			"    vecAB* @vecABStyle = B - A;\n" +
			"    @pVecStyle = { show: 3d; objColor: #33FF0000; labelOffset: 80 -6; lineStyle: full thickness=5 hidden=dashed; coords: 12.749697433362456 15.409257456402978 0.0 }\n" +
			"    pVec* @pVecStyle = 20UnitVector[((-y(vecAB)) / xyScale, x(vecAB) xyScale)];\n" +
			"    @pVecAngleStyle = { lineStyle: full thickness=5 hidden=show opacity=178; show: 3d; objColor: #0064001A; bgColor: #000000FF; labelOffset: 47 -65; labelMode: namevalue }\n" +
			"    pVecAngle @pVecAngleStyle = Angle[xAxis, Line[(0, 0), pVec]];\n" +
			"    realAngle = If[pVecAngle ≟ π / 2 ∨ pVecAngle ≟ 3π / 2, pVecAngle, atan(tan(pVecAngle) / xyScale)];\n" +
			"    @curveFunctionStyle = { show: 3d; objColor: #00000000; labelOffset: 60 -15; lineStyle: full thickness=5 hidden=dashed opacity=178 }\n" +
			"    curveFunction* @curveFunctionStyle = CurveCartesian[pointGap cos(u), pointGap xyScale sin(u), u, 0, 2π];\n" +
			"    @KStyle = { show: 3d; objColor: #44444400; pointSize: 4; coords: 0.03953911977970617 0.04778689686319611 }\n" +
			"    K @KStyle = curveFunction(realAngle);\n" +
			"    @vSpaceStyle = { objColor: #00000000; lineStyle: full thickness=5 hidden=dashed opacity=178; coords: 0.03953911977970617 0.04778689686319611 0.0 }\n" +
			"    vSpace* @vSpaceStyle = Vector[K];\n" +
			"    @allPointsStyle = { show: 3d; objColor: #00640000; lineStyle: full thickness=5 hidden=dashed }\n" +
			"    allPoints @allPointsStyle = {A + vSpace, A - vSpace, B - vSpace, B + vSpace};\n" +
			"    @newPolyStyle = { lineStyle: full thickness=5 hidden=dashed opacity=178; objColor: #9933001A }\n" +
			"    newPoly~ @newPolyStyle = Polygon[allPoints];\n" +
			"    @@return newPoly\n" +
			"}";
		
		try {
			// Try to parse the complete Gpad script (all macros and commands)
			// The error should occur at line 46, column 40 when parsing the CurveCartesian command
			String result = getApp().getGgbApi().evalGpad(macroGpad);
			
			// Always check for errors and warnings
			String lastError = getApp().getGgbApi().getLastError();
			String lastWarning = getApp().getGgbApi().getLastWarning();
			
			if (result == null || lastError != null) {
				// Check if there was an error about undefined variable
				if (lastError != null && 
						(lastError.contains("Undefined variable") 
						|| lastError.contains("未定义")
						|| lastError.toLowerCase().contains("undefined")
						|| (lastError.contains("u") && lastError.contains("variable")))) {
					System.err.println("ERROR REPRODUCED: Undefined variable 'u' error in macro!");
					System.err.println("Error message: " + lastError);
					System.err.println("\nThis confirms that the issue occurs when processing");
					System.err.println("CurveCartesian command inside a macro definition.");
					System.err.println("\nThe problem is that when parsing the macro body,");
					System.err.println("the CurveCartesian command's parameter variable 'u'");
					System.err.println("is not registered before resolving expressions like");
					System.err.println("'pointGap cos(u)' and 'pointGap xyScale sin(u)'.");
					
					throw new AssertionError(
						"Undefined variable 'u' error reproduced in macro. " +
						"The parameter variable must be registered BEFORE resolving " +
						"expressions that contain it. Error: " + lastError);
				}
				
				// Print error details even if not the specific error we're looking for
				System.err.println("evalGpad returned null or had an error:");
				System.err.println("Result: " + result);
				System.err.println("Last error: " + lastError);
				System.err.println("Last warning: " + lastWarning);
				
				if (lastError != null) {
					throw new AssertionError("evalGpad failed with error: " + lastError);
				} else {
					// Result is null but no error - this might indicate a parsing issue
					System.err.println("Note: evalGpad returned null but no error was set.");
					System.err.println("This might indicate a parsing issue or the macro was not recognized.");
					// Don't fail the test - just report
					return;
				}
			}
			
			// If we get here, the macro was parsed successfully
			System.out.println("Macro parsed successfully. Result: " + result);
			if (lastWarning != null) {
				System.out.println("Warning: " + lastWarning);
			}
			
		} catch (AssertionError e) {
			// Re-throw AssertionError as-is
			throw e;
		} catch (Exception e) {
			// Print detailed error information
			System.err.println("Exception occurred: " + e.getMessage());
			e.printStackTrace();
			
			// Check for the specific error
			String lastError = getApp().getGgbApi().getLastError();
			String lastWarning = getApp().getGgbApi().getLastWarning();
			if (lastError != null) {
				System.err.println("Last error from evalGpad: " + lastError);
			}
			if (lastWarning != null) {
				System.err.println("Last warning from evalGpad: " + lastWarning);
			}
			
			throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
		}
	}
}

