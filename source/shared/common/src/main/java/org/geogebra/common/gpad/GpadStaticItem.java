package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a statically parsed GPAD statement. No GeoGebra runtime types are used.
 * Produced by {@code Parser.parseGpadStatic()} and consumed by
 * {@link GpadToXmlStaticConverter}.
 */
public class GpadStaticItem {

	public enum Type {
		COMMAND, EXPRESSION, INDEPENDENT, STYLESHEET, ENV, MACRO
	}

	public Type type;
	public List<TypedLabel> labels = new ArrayList<>();

	/** COMMAND / EXPRESSION: raw RHS text (after {@code =} or {@code :=}). */
	public String rhsText;

	/** INDEPENDENT shorthand: primary attribute value such as {@code (1,2)}, {@code 5}. */
	public String shorthandText;

	/** INDEPENDENT extra creation data block (e.g. slider properties). */
	public GpadStyleSheet extraData;

	/** COMMAND only — extracted command name. */
	public String commandName;

	/** COMMAND only — extracted argument list (top-level comma split). */
	public List<String> commandArgs;

	/** STYLESHEET only — the stylesheet name. */
	public String stylesheetName;

	/** STYLESHEET only — the stylesheet body. */
	public GpadStyleSheet stylesheetBody;

	/** ENV only — raw content string. */
	public String rawContent;

	/** MACRO only — the macro command name. */
	public String macroName;

	/** MACRO only — input label names. */
	public List<String> macroInputLabels;

	/** MACRO only — output label names (from @@return). */
	public List<String> macroOutputLabels;

	/** MACRO only — body items (construction statements inside the macro). */
	public List<GpadStaticItem> macroBodyItems;

	/** MACRO only — metadata properties from @meta stylesheet. */
	public java.util.Map<String, String> macroMeta;

	/**
	 * A label with optional type prefix and optional inline stylesheet.
	 */
	public static class TypedLabel {
		/** Element type from GPAD type prefix, may be {@code null} if omitted. */
		public String elementType;
		/** The label text (e.g. {@code "A"}, {@code "f(x)"}). */
		public String label;
		/** Inline or referenced stylesheet, may be {@code null}. */
		public GpadStyleSheet styleSheet;
		/** Whether the object is visible (default true). Suffix {@code *} means false. */
		public boolean showObject = true;
		/** Whether the label is visible (default true). Suffix {@code ~} means false. */
		public boolean showLabel = true;

		public TypedLabel() {}

		public TypedLabel(String elementType, String label, GpadStyleSheet styleSheet) {
			this.elementType = elementType;
			this.label = label;
			this.styleSheet = styleSheet;
		}
	}

	/**
	 * For COMMAND items, extracts the command name and argument list from {@link #rhsText}.
	 * Sets {@link #commandName} and {@link #commandArgs}.
	 */
	public void extractCommandParts() {
		if (rhsText == null || rhsText.isEmpty()) return;
		int parenIdx = rhsText.indexOf('(');
		if (parenIdx < 0) {
			commandName = rhsText.trim();
			commandArgs = new ArrayList<>();
			return;
		}
		commandName = rhsText.substring(0, parenIdx).trim();
		int lastParen = rhsText.lastIndexOf(')');
		if (lastParen <= parenIdx) {
			commandArgs = new ArrayList<>();
			return;
		}
		String argsStr = rhsText.substring(parenIdx + 1, lastParen).trim();
		commandArgs = splitTopLevelComma(argsStr);
	}

	/**
	 * Splits a string by top-level commas (respecting parentheses, brackets, braces, strings).
	 */
	static List<String> splitTopLevelComma(String s) {
		List<String> parts = new ArrayList<>();
		if (s == null || s.isEmpty()) return parts;
		int depth = 0;
		boolean inString = false;
		char stringChar = 0;
		int start = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (inString) {
				if (c == '\\') { i++; continue; }
				if (c == stringChar) inString = false;
				continue;
			}
			if (c == '"' || c == '`') { inString = true; stringChar = c; continue; }
			if (c == '(' || c == '[' || c == '{') depth++;
			else if (c == ')' || c == ']' || c == '}') depth--;
			else if (c == ',' && depth == 0) {
				parts.add(s.substring(start, i).trim());
				start = i + 1;
			}
		}
		String last = s.substring(start).trim();
		if (!last.isEmpty()) parts.add(last);
		return parts;
	}
}
