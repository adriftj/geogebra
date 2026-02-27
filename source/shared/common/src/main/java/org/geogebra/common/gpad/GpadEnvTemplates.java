package org.geogebra.common.gpad;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable registry of @@env templates.
 * Built-in templates ({@code blank}, {@code grid}) are pre-registered
 * and can be overridden or removed at runtime via the API.
 */
public final class GpadEnvTemplates {

	private static final Map<String, String> templates =
			new ConcurrentHashMap<>();

	static {
		templates.put("blank", "ev1 { ~axes; }");
		templates.put("grid", "ev1 { grid; }");
	}

	private GpadEnvTemplates() {
	}

	/**
	 * Returns the raw content of a template, or {@code null} if not registered.
	 *
	 * @param name template name
	 * @return raw env content (without outer braces), or null
	 */
	public static String get(String name) {
		return templates.get(name);
	}

	/**
	 * Registers, overrides, or removes a template.
	 * Passing {@code null} or empty content removes the template.
	 *
	 * @param name    template name
	 * @param content raw env content (without outer braces), or null to remove
	 */
	public static void set(String name, String content) {
		if (content == null || content.isEmpty()) {
			templates.remove(name);
		} else {
			templates.put(name, content);
		}
	}
}
