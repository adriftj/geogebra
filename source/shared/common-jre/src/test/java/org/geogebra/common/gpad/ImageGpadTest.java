package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.geogebra.common.BaseUnitTest;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoImage;
import org.geogebra.common.util.ImageManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for Image command and gpad style filename attribute.
 * Tests creating GeoImage objects from URLs and filenames,
 * and dynamic modification of images via gpad styles.
 * 
 * Note: Some tests may fail if Image command is not yet fully supported
 * in the gpad parser. The tests first verify that Image command works
 * via normal command processing, then test gpad parser support.
 */
public class ImageGpadTest extends BaseUnitTest {

	@Before
	public void setupImageManager() {
		// Mock ImageManager to avoid actual image loading
		getApp().setImageManager(Mockito.mock(ImageManager.class));
	}

	// ========== Basic Image Command Tests ==========

	@Test
	public void testImageCommandWithURL() {
		// Test Image command with HTTP URL
		// First test if Image command works via normal command processing
		GeoElement result = add("img1 = Image(\"https://example.com/image.png\")");
		assertNotNull(result);
		assertTrue(result instanceof GeoImage);
		
		GeoImage image = (GeoImage) result;
		assertEquals("https://example.com/image.png", 
				image.getGraphicsAdapter().getImageFileName());
		
		// Now test via gpad parser
		String gpad = "img2 = Image(\"https://example.com/image2.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image2 = (GeoImage) getKernel().lookupLabel("img2");
			assertNotNull(image2);
			assertEquals("https://example.com/image2.png", 
					image2.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testImageCommandWithHTTPSURL() {
		// Test Image command with HTTPS URL
		String gpad = "img2 = Image(\"https://geogebra.org/images/logo.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img2");
			assertNotNull(image);
			assertEquals("https://geogebra.org/images/logo.png", 
					image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testImageCommandWithLocalFilename() {
		// Test Image command with local filename
		String gpad = "img3 = Image(\"test.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img3");
			assertNotNull(image);
			assertEquals("test.png", 
					image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testImageCommandWithDataURL() {
		// Test Image command with data URL (base64 encoded 1x1 red pixel PNG)
		String dataURL = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
		String gpad = "img4 = Image(\"" + dataURL + "\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img4");
			assertNotNull(image);
			assertEquals(dataURL, image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testImageCommandWithPathWithSlash() {
		// Test Image command with path starting with slash
		String gpad = "img5 = Image(\"/path/to/image.jpg\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img5");
			assertNotNull(image);
			// The leading slash should be preserved or removed based on implementation
			String fileName = image.getGraphicsAdapter().getImageFileName();
			assertTrue("/path/to/image.jpg".equals(fileName) || 
					"path/to/image.jpg".equals(fileName));
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Gpad Style Filename Tests ==========

	@Test
	public void testGpadStyleFilenameWithURL() {
		// Test applying gpad style with filename attribute (URL)
		String gpad = "@style = { filename: \"https://example.com/image1.png\" }\n"
				+ "img1 @style = Image(\"initial.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			// The filename should be overridden by the style
			assertEquals("https://example.com/image1.png", 
					image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testGpadStyleFilenameChange() {
		// Test changing filename via gpad style by redefining the object
		// First create an image
		String gpad1 = "img1 = Image(\"initial.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			parser.parse(gpad1);
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			assertEquals("initial.png", 
					image.getGraphicsAdapter().getImageFileName());
			
			// Now redefine the object with a style to change the filename
			// Gpad syntax requires redefinition: label @style = command(...)
			String gpad2 = "@style = { filename: \"https://example.com/new.png\" }\n"
					+ "img1 @style = Image(\"initial.png\")";
			parser.parse(gpad2);
			
			// The filename should be updated by the style
			GeoImage image2 = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image2);
			assertEquals("https://example.com/new.png", 
					image2.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testGpadStyleFilenameWithDataURL() {
		// Test gpad style filename with data URL
		String dataURL = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
		String gpad = "@style = { filename: \"" + dataURL + "\" }\n"
				+ "img1 @style = Image(\"test.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			assertEquals(dataURL, image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testGpadStyleFilenameMultipleImages() {
		// Test applying same filename style to multiple images
		// Use semicolons to separate statements
		String gpad = "@style = { filename: \"https://example.com/shared.png\" }\n"
				+ "img1 @style = Image(\"test1.png\");\n"
				+ "img2 @style = Image(\"test2.png\");";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(2, geos.size());
			
			GeoImage image1 = (GeoImage) getKernel().lookupLabel("img1");
			GeoImage image2 = (GeoImage) getKernel().lookupLabel("img2");
			assertNotNull(image1);
			assertNotNull(image2);
			
			// Both images should have the same filename from the style
			String expected = "https://example.com/shared.png";
			assertEquals(expected, image1.getGraphicsAdapter().getImageFileName());
			assertEquals(expected, image2.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testGpadStyleFilenameWithOtherProperties() {
		// Test filename style combined with other style properties
		String gpad = "@style = { filename: \"https://example.com/image.png\"; "
				+ "objColor: #FF0000FF; pointSize: 6 }\n"
				+ "img1 @style = Image(\"test.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			// Filename should be set
			assertEquals("https://example.com/image.png", 
					image.getGraphicsAdapter().getImageFileName());
			// Other properties should also be applied
			// (Note: objColor and pointSize may not apply to GeoImage, but filename should)
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Edge Cases and Error Handling ==========

	@Test
	public void testImageCommandWithEmptyString() {
		// Test Image command with empty string (should still create object)
		String gpad = "img1 = Image(\"\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			assertEquals("", image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testImageCommandWithSpecialCharacters() {
		// Test Image command with URL containing special characters
		String gpad = "img1 = Image(\"https://example.com/image%20with%20spaces.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			assertEquals("https://example.com/image%20with%20spaces.png", 
					image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	@Test
	public void testImageCommandWithQueryParameters() {
		// Test Image command with URL containing query parameters
		String gpad = "img1 = Image(\"https://example.com/image.png?size=large&format=png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			List<GeoElement> geos = parser.parse(gpad);
			assertEquals(1, geos.size());
			
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			assertEquals("https://example.com/image.png?size=large&format=png", 
					image.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}

	// ========== Integration Tests ==========

	@Test
	public void testImageCommandRoundTrip() {
		// Test creating image, then modifying via style by redefining, then checking again
		String gpad1 = "img1 = Image(\"initial.png\")";
		GpadParser parser = new GpadParser(getKernel());
		
		try {
			// Create initial image
			parser.parse(gpad1);
			GeoImage image = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image);
			assertEquals("initial.png", 
					image.getGraphicsAdapter().getImageFileName());
			
			// Change to URL by redefining with style
			String gpad2 = "@style1 = { filename: \"https://example.com/url.png\" }\n"
					+ "img1 @style1 = Image(\"initial.png\")";
			parser.parse(gpad2);
			GeoImage image2 = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image2);
			assertEquals("https://example.com/url.png", 
					image2.getGraphicsAdapter().getImageFileName());
			
			// Change to another URL by redefining with different style
			String gpad3 = "@style2 = { filename: \"https://example.com/another.png\" }\n"
					+ "img1 @style2 = Image(\"initial.png\")";
			parser.parse(gpad3);
			GeoImage image3 = (GeoImage) getKernel().lookupLabel("img1");
			assertNotNull(image3);
			assertEquals("https://example.com/another.png", 
					image3.getGraphicsAdapter().getImageFileName());
		} catch (GpadParseException e) {
			throw new AssertionError("Parse failed: " + e.getMessage(), e);
		}
	}
}

