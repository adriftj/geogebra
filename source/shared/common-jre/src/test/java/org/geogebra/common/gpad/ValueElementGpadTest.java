package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.geogebra.common.BaseUnitTest;

import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoNumeric;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.plugin.JsReference;
import org.geogebra.common.plugin.ScriptManager;
import org.geogebra.common.plugin.script.Script;
import org.junit.Test;

import java.util.HashMap;

/**
 * Unit tests for value element conversion to Gpad format.
 * Tests conversion of value element attributes (val, random) to javascript and random styles
 * based on object type (Button, Numeric).
 * Tests are based on gpad syntax to create objects.
 */
public class ValueElementGpadTest extends BaseUnitTest {

	// ==================== Button Type Tests ====================

	@Test
	public void testButtonWithScriptStyle() {
		// Test: Create Button with javascript style using gpad syntax
		String gpad = "@style = { javascript: \"SetValue(a,1)\" }\n"
				+ "btn @style = Button(\"Click\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoButton);
			GeoButton button = (GeoButton) geo;
			
			// Verify button has script set
			Script clickScript = button.getScript(EventType.CLICK);
			assertNotNull("Button should have click script", clickScript);
			assertTrue("Script should contain SetValue", 
					clickScript.getText().contains("SetValue"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testButtonWithScriptStyleSimple() {
		// Test: Create Button with simple javascript (no quotes needed)
		String gpad = "@style = { javascript: SetValue(a,1) }\n"
				+ "btn @style = Button(\"Click\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoButton);
			GeoButton button = (GeoButton) geo;
			
			// Verify button has script set
			Script clickScript = button.getScript(EventType.CLICK);
			assertNotNull("Button should have click script", clickScript);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testButtonWithScriptStyleSpecialChars() {
		// Test: Create Button with javascript containing special characters (quoted)
		String gpad = "@style = { javascript: \"SetValue(a,1); SetValue(b,2)\" }\n"
				+ "btn @style = Button(\"Click\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoButton);
			GeoButton button = (GeoButton) geo;
			
			// Verify button has script set with special characters
			Script clickScript = button.getScript(EventType.CLICK);
			assertNotNull("Button should have click script", clickScript);
			assertTrue("Script should contain semicolon", 
					clickScript.getText().contains(";"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testButtonToGpadWithScript() {
		// Test: Create Button with javascript, then convert back to gpad
		String gpad = "@style = { jsClick: \"SetValue(a,1)\" }\n"
				+ "btn @style = Button(\"Click\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			assertNotNull("Converted gpad should not be null", convertedGpad);
			System.out.println("["+convertedGpad+"]");
			assertTrue("Should contain jsClick style", convertedGpad.contains("jsClick:"));
			assertTrue("Should contain the jsClick value", convertedGpad.contains("SetValue"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== Listener Function Tests ====================

	@Test
	public void testPointWithJsUpdateFunction() {
		// Test: Create Point with jsUpdateFunction style using gpad syntax
		String gpad = "@style = { jsUpdateFunction: \"onUpdate\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			
			// Verify point has update listener set
			ScriptManager scriptManager = getKernel().getApplication().getScriptManager();
			HashMap<GeoElement, JsReference> updateMap = scriptManager.getUpdateListenerMap();
			assertNotNull("Update listener map should not be null", updateMap);
			assertTrue("Point should have update listener", updateMap.containsKey(geo));
			JsReference listener = updateMap.get(geo);
			assertNotNull("Listener should not be null", listener);
			assertEquals("onUpdate", listener.getText());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testPointWithJsClickFunction() {
		// Test: Create Point with jsClickFunction style using gpad syntax
		String gpad = "@style = { jsClickFunction: \"onClick\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			
			// Verify point has click listener set
			ScriptManager scriptManager = getKernel().getApplication().getScriptManager();
			HashMap<GeoElement, JsReference> clickMap = scriptManager.getClickListenerMap();
			assertNotNull("Click listener map should not be null", clickMap);
			assertTrue("Point should have click listener", clickMap.containsKey(geo));
			JsReference listener = clickMap.get(geo);
			assertNotNull("Listener should not be null", listener);
			assertEquals("onClick", listener.getText());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testPointWithBothListenerFunctions() {
		// Test: Create Point with both jsUpdateFunction and jsClickFunction
		String gpad = "@style = { jsUpdateFunction: \"onUpdate\"; jsClickFunction: \"onClick\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoPoint);
			
			// Verify point has both listeners set
			ScriptManager scriptManager = getKernel().getApplication().getScriptManager();
			
			HashMap<GeoElement, JsReference> updateMap = scriptManager.getUpdateListenerMap();
			assertTrue("Point should have update listener", updateMap.containsKey(geo));
			assertEquals("onUpdate", updateMap.get(geo).getText());
			
			HashMap<GeoElement, JsReference> clickMap = scriptManager.getClickListenerMap();
			assertTrue("Point should have click listener", clickMap.containsKey(geo));
			assertEquals("onClick", clickMap.get(geo).getText());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testPointToGpadWithJsUpdateFunction() {
		// Test: Create Point with jsUpdateFunction, then convert back to gpad
		String gpad = "@style = { jsUpdateFunction: \"onUpdate\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			System.out.println(convertedGpad);
			
			assertNotNull("Converted gpad should not be null", convertedGpad);
			assertTrue("Should contain jsUpdateFunction style", 
					convertedGpad.contains("jsUpdateFunction:"));
			assertTrue("Should contain the function name", 
					convertedGpad.contains("onUpdate"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testPointToGpadWithJsClickFunction() {
		// Test: Create Point with jsClickFunction, then convert back to gpad
		String gpad = "@style = { jsClickFunction: \"onClick\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			System.out.println(convertedGpad);
			
			assertNotNull("Converted gpad should not be null", convertedGpad);
			assertTrue("Should contain jsClickFunction style", 
					convertedGpad.contains("jsClickFunction:"));
			assertTrue("Should contain the function name", 
					convertedGpad.contains("onClick"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testPointRoundTripWithJsUpdateFunction() {
		// Test: Create Point with jsUpdateFunction, convert to gpad, parse again
		String originalGpad = "@style = { jsUpdateFunction: \"onUpdate\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Parse converted gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			
			// Verify listener is preserved
			ScriptManager scriptManager = getKernel().getApplication().getScriptManager();
			HashMap<GeoElement, JsReference> updateMap = scriptManager.getUpdateListenerMap();
			assertTrue("Point should have update listener after round trip", 
					updateMap.containsKey(geo2));
			assertEquals("onUpdate", updateMap.get(geo2).getText());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testPointRoundTripWithJsClickFunction() {
		// Test: Create Point with jsClickFunction, convert to gpad, parse again
		String originalGpad = "@style = { jsClickFunction: \"onClick\" }\n"
				+ "A @style = (0, 0)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Parse converted gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			
			// Verify listener is preserved
			ScriptManager scriptManager = getKernel().getApplication().getScriptManager();
			HashMap<GeoElement, JsReference> clickMap = scriptManager.getClickListenerMap();
			assertTrue("Point should have click listener after round trip", 
					clickMap.containsKey(geo2));
			assertEquals("onClick", clickMap.get(geo2).getText());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== Numeric Type Tests ====================

	@Test
	public void testNumericWithRandomStyle() {
		// Test: Create Numeric with random style using gpad syntax
		// Use Slider command to create independent GeoNumeric without definition
		String gpad = "@style = { random }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertNotNull("Parse should return non-null list", geos);
			assertEquals("Should have exactly 1 element", 1, geos.size());
			GeoElement geo = geos.get(0);
			assertNotNull("GeoElement should not be null", geo);
			assertTrue("GeoElement should be GeoNumeric", geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify numeric has random set
			assertTrue("Numeric should have random=true", num.isRandom());
		} catch (GpadParseException e) {
			String errorMsg = e.getMessage();
			if (errorMsg == null) {
				errorMsg = e.getClass().getSimpleName();
			}
			throw new AssertionError("Parse failed: " + errorMsg, e);
		}
	}

	@Test
	public void testNumericWithoutRandomStyle() {
		// Test: Create Numeric without random style (default)
		String gpad = "a = 5";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify numeric does not have random (default false)
			assertTrue("Numeric should have random=false (default)", !num.isRandom());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testNumericToGpadWithRandom() {
		// Test: Create Numeric with random, then convert back to gpad
		// Use Slider command to create independent GeoNumeric without definition
		String gpad = "@style = { random }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			assertNotNull("Converted gpad should not be null", convertedGpad);
			assertTrue("Should contain random style", convertedGpad.contains("random"));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testNumericToGpadWithoutRandom() {
		// Test: Create Numeric without random, then convert back to gpad
		String gpad = "a = 5";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// random=false should not output random style (default value)
			if (convertedGpad != null) {
				assertTrue("Should not contain random when random=false", !convertedGpad.contains("random"));
			}
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testNumericWithRandomAndOtherProperties() {
		// Test: Create Numeric with random and other properties
		// Use Slider command to create independent GeoNumeric without definition
		String gpad = "@style = { random; slider: fixed }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify numeric has random and fixed set
			assertTrue("Numeric should have random=true", num.isRandom());
			assertTrue("Numeric should have fixed=true", num.isLockedPosition());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== Round Trip Tests ====================

	@Test
	public void testButtonRoundTrip() {
		// Test: Create Button with javascript, convert to gpad, parse again
		String originalGpad = "@style = { javascript: \"SetValue(a,1)\" }\n"
				+ "btn @style = Button(\"Click\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Parse converted gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			assertTrue(geo2 instanceof GeoButton);
			GeoButton button2 = (GeoButton) geo2;
			
			// Verify script is preserved
			Script clickScript2 = button2.getScript(EventType.CLICK);
			assertNotNull("Button should have click script after round trip", clickScript2);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testNumericRoundTrip() {
		// Test: Create Numeric with random, convert to gpad, parse again
		// Use Slider command to create independent GeoNumeric without definition
		String originalGpad = "@style = { random }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Parse original gpad
			List<GeoElement> geos = parser.parse(originalGpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Parse converted gpad again
			GpadParser parser2 = new GpadParser(getKernel());
			List<GeoElement> geos2 = parser2.parse(convertedGpad);
			assertEquals(1, geos2.size());
			GeoElement geo2 = geos2.get(0);
			assertTrue(geo2 instanceof GeoNumeric);
			GeoNumeric num2 = (GeoNumeric) geo2;
			
			// Verify random is preserved
			assertTrue("Numeric should have random=true after round trip", num2.isRandom());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ==================== Edge Cases ====================

	@Test
	public void testButtonWithEmptyScript() {
		// Test: Button without javascript should not output javascript style
		String gpad = "btn = Button(\"Click\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			
			// Convert back to gpad
			GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
			String convertedGpad = converter.toGpad(geo);
			
			// Empty javascript should not output javascript style
			if (convertedGpad != null) {
				assertTrue("Should not contain javascript when javascript is empty", !convertedGpad.contains("javascript:"));
			}
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testNumericWithSliderAndRandom() {
		// Test: Numeric with slider style and random
		String gpad = "@style = { slider: min=0 max=10 width=200; random }\n"
				+ "a @style = Slider(0, 10)";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			GeoElement geo = geos.get(0);
			assertTrue(geo instanceof GeoNumeric);
			GeoNumeric num = (GeoNumeric) geo;
			
			// Verify numeric has random and slider properties
			assertTrue("Numeric should have random=true", num.isRandom());
			assertEquals(0.0, num.getIntervalMin(), 1e-10);
			assertEquals(10.0, num.getIntervalMax(), 1e-10);
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}

