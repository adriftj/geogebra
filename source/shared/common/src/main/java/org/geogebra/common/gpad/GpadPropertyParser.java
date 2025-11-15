package org.geogebra.common.gpad;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses property values from Gpad style sheet syntax.
 * Handles both simple properties and complex properties with special syntax.
 */
public class GpadPropertyParser {

	/**
	 * Parses an animation property value.
	 * 
	 * Syntax: [play] [step] [speed]
	 * - "play" keyword sets playing=true
	 * - step: number with optional prefix (+/-/~) determining type
	 * - speed: number followed by 'x'
	 * 
	 * @param value
	 *            property value string
	 * @return map with keys: playing, step, speed, type
	 */
	public Map<String, String> parseAnimation(String value) {
		Map<String, String> result = new HashMap<>();
		
		// Default values
		result.put("playing", "false");
		result.put("speed", "1");
		result.put("type", "0"); // ANIMATION_OSCILLATING
		result.put("step", null);

		if (value == null || value.trim().isEmpty()) {
			return result;
		}

		String[] parts = value.trim().split("\\s+");
		
		for (String part : parts) {
			part = part.trim();
			if (part.isEmpty()) {
				continue;
			}

			// Check for "play" keyword
			if ("play".equalsIgnoreCase(part)) {
				result.put("playing", "true");
				continue;
			}

			// Check for speed (number followed by 'x')
			if (part.endsWith("x") || part.endsWith("X")) {
				try {
					String speedStr = part.substring(0, part.length() - 1);
					double speed = Double.parseDouble(speedStr);
					result.put("speed", String.valueOf(speed));
					continue;
				} catch (NumberFormatException e) {
					// Ignore invalid speed format
				}
			}

			// Check for step (number with optional prefix)
			if (part.startsWith("+") || part.startsWith("-") || part.startsWith("~")) {
				try {
					String stepStr = part.substring(1);
					double step = Math.abs(Double.parseDouble(stepStr));
					result.put("step", String.valueOf(step));
					
					// Determine type based on prefix
					char prefix = part.charAt(0);
					if (prefix == '+') {
						result.put("type", "1"); // ANIMATION_INCREASING
					} else if (prefix == '-') {
						result.put("type", "2"); // ANIMATION_DECREASING
					} else if (prefix == '~') {
						result.put("type", "3"); // ANIMATION_INCREASING_ONCE
					}
					continue;
				} catch (NumberFormatException e) {
					// Ignore invalid step format
				}
			}

			// Try to parse as step without prefix (type=0, oscillating)
			try {
				double step = Math.abs(Double.parseDouble(part));
				result.put("step", String.valueOf(step));
				result.put("type", "0"); // ANIMATION_OSCILLATING
			} catch (NumberFormatException e) {
				// Ignore invalid format
			}
		}

		return result;
	}

	/**
	 * Parses a simple property value.
	 * For properties with only "val" attribute, the value can be simplified.
	 * 
	 * @param value
	 *            property value string
	 * @return parsed value
	 */
	public String parseSimpleValue(String value) {
		if (value == null) {
			return null;
		}
		return value.trim();
	}

	/**
	 * Parses a comma-separated list of values (e.g., for labelOffset: "28, 75").
	 * 
	 * @param value
	 *            comma-separated values
	 * @return array of parsed values
	 */
	public String[] parseCommaSeparated(String value) {
		if (value == null || value.trim().isEmpty()) {
			return new String[0];
		}
		String[] parts = value.split(",");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		return parts;
	}

	/**
	 * Parses a boolean value.
	 * 
	 * @param value
	 *            boolean string (can be null for true)
	 * @return boolean value
	 */
	public boolean parseBoolean(String value) {
		if (value == null || value.trim().isEmpty()) {
			return true; // Presence means true
		}
		String lower = value.toLowerCase().trim();
		return "true".equals(lower) || "1".equals(lower);
	}
}





