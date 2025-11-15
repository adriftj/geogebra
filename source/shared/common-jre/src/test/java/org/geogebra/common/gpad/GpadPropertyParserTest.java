package org.geogebra.common.gpad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for GpadPropertyParser.
 */
public class GpadPropertyParserTest {

	@Test
	public void testParseAnimationWithPlay() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "play -0.1 2x";
		
		Map<String, String> result = parser.parseAnimation(value);
		
		assertNotNull(result);
		assertEquals("true", result.get("playing"));
		assertEquals("0.1", result.get("step"));
		assertEquals("2", result.get("speed"));
		assertEquals("2", result.get("type")); // ANIMATION_DECREASING
	}

	@Test
	public void testParseAnimationWithoutPlay() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "0.1 2x";
		
		Map<String, String> result = parser.parseAnimation(value);
		
		assertNotNull(result);
		assertEquals("false", result.get("playing"));
		assertEquals("0.1", result.get("step"));
		assertEquals("2", result.get("speed"));
		assertEquals("0", result.get("type")); // ANIMATION_OSCILLATING
	}

	@Test
	public void testParseAnimationWithPlus() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "play +0.1 2x";
		
		Map<String, String> result = parser.parseAnimation(value);
		
		assertNotNull(result);
		assertEquals("true", result.get("playing"));
		assertEquals("0.1", result.get("step"));
		assertEquals("1", result.get("type")); // ANIMATION_INCREASING
	}

	@Test
	public void testParseAnimationWithTilde() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "play ~0.1 2x";
		
		Map<String, String> result = parser.parseAnimation(value);
		
		assertNotNull(result);
		assertEquals("true", result.get("playing"));
		assertEquals("0.1", result.get("step"));
		assertEquals("3", result.get("type")); // ANIMATION_INCREASING_ONCE
	}

	@Test
	public void testParseAnimationWithMinus() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "play -0.1 2x";
		
		Map<String, String> result = parser.parseAnimation(value);
		
		assertNotNull(result);
		assertEquals("true", result.get("playing"));
		assertEquals("0.1", result.get("step"));
		assertEquals("2", result.get("type")); // ANIMATION_DECREASING
	}

	@Test
	public void testParseAnimationDefaultSpeed() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "play 0.1";
		
		Map<String, String> result = parser.parseAnimation(value);
		
		assertNotNull(result);
		assertEquals("1", result.get("speed")); // Default speed
	}

	@Test
	public void testParseCommaSeparated() {
		GpadPropertyParser parser = new GpadPropertyParser();
		String value = "28, 75";
		
		String[] result = parser.parseCommaSeparated(value);
		
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("28", result[0]);
		assertEquals("75", result[1]);
	}

	@Test
	public void testParseBooleanTrue() {
		GpadPropertyParser parser = new GpadPropertyParser();
		
		assertTrue(parser.parseBoolean(null)); // Presence means true
		assertTrue(parser.parseBoolean("true"));
		assertTrue(parser.parseBoolean("1"));
	}

	@Test
	public void testParseBooleanFalse() {
		GpadPropertyParser parser = new GpadPropertyParser();
		
		assertTrue(!parser.parseBoolean("false"));
		assertTrue(!parser.parseBoolean("0"));
	}
}


