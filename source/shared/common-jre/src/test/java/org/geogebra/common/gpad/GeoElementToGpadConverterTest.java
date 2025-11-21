package org.geogebra.common.gpad;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Unit tests for GeoElementToGpadConverter.
 */
public class GeoElementToGpadConverterTest extends BaseUnitTest {

	@Test
	public void testConvertSimplePoint() {
		GeoElement geo = add("A = (1, 2)");
		
		GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
		String gpad = converter.toGpad(geo);
		
		assertNotNull(gpad);
		assertTrue(gpad.contains("A"));
		assertTrue(gpad.contains("="));
	}

	@Test
	public void testConvertPointWithStyle() {
		GeoElement geo = add("A = (1, 2)");
		if (geo instanceof GeoPoint) {
			((GeoPoint) geo).setPointSize(6);
			geo.setFixed(true);
		}
		
		GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
		String gpad = converter.toGpad(geo);
		
		assertNotNull(gpad);
		assertTrue(gpad.contains("A"));
		// Should contain style sheet definition
		assertTrue(gpad.contains("$") || gpad.contains("pointSize") || gpad.contains("fixed"));
	}

	@Test
	public void testConvertMultipleElements() {
		GeoElement geo1 = add("A = (1, 2)");
		GeoElement geo2 = add("B = (3, 4)");
		
		GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
		java.util.List<GeoElement> geos = java.util.Arrays.asList(geo1, geo2);
		String gpad = converter.toGpad(geos);
		
		assertNotNull(gpad);
		assertTrue(gpad.contains("A"));
		assertTrue(gpad.contains("B"));
	}

	@Test
	public void testConvertNull() {
		GeoElementToGpadConverter converter = new GeoElementToGpadConverter();
		String gpad = converter.toGpad((GeoElement) null);
		
		assertTrue(gpad.isEmpty());
	}
}




