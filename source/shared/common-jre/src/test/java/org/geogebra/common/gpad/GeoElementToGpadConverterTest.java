package org.geogebra.common.gpad;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoPoint;
import org.junit.Test;

/**
 * Unit tests for {@link ToGpadConverter#fromGeoElement(GeoElement)}.
 */
public class GeoElementToGpadConverterTest extends BaseUnitTest {

	@Test
	public void testConvertSimplePoint() {
		GeoElement geo = add("A = (1, 2)");

		String gpad = ToGpadConverter.fromGeoElement(geo);

		assertNotNull(gpad);
		assertTrue("Should contain type prefix 'point'", gpad.contains("point"));
		assertTrue("Should contain label A", gpad.contains("A"));
		assertTrue("Should contain coordinates (1, 2)",
				gpad.contains("(1") && gpad.contains("2)"));
	}

	@Test
	public void testConvertPointWithStyle() {
		GeoElement geo = add("A = (1, 2)");
		if (geo instanceof GeoPoint) {
			((GeoPoint) geo).setPointSize(6);
			geo.setFixed(true);
		}

		String gpad = ToGpadConverter.fromGeoElement(geo);

		assertNotNull(gpad);
		assertTrue("Should contain label A", gpad.contains("A"));
		assertTrue("Should contain stylesheet reference",
				gpad.contains("@"));
		assertTrue("Should contain pointSize or fixed",
				gpad.contains("pointSize") || gpad.contains("fixed"));
	}

	@Test
	public void testNoEnvGenerated() {
		GeoElement geo = add("A = (1, 2)");

		String gpad = ToGpadConverter.fromGeoElement(geo);

		assertNotNull(gpad);
		assertFalse("Single element should not generate @@env",
				gpad.contains("@@env"));
	}

	@Test
	public void testConvertNull() {
		String gpad = ToGpadConverter.fromGeoElement(null);

		assertTrue(gpad.isEmpty());
	}
}
