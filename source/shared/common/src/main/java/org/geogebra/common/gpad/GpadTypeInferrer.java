package org.geogebra.common.gpad;

import java.util.List;
import java.util.Map;

/**
 * Static type inference for GPAD statements when the user omits the type prefix.
 * Used during GPAD→XML conversion ({@link GpadToXmlStaticConverter}) to fill in
 * missing {@code elementType} on {@link GpadStaticItem.TypedLabel}.
 * <p>
 * Never overwrites an explicitly specified type prefix.
 */
public class GpadTypeInferrer {

	/**
	 * Command name → default output element type.
	 * Only the first (primary) output label's type is covered here.
	 */
	private static final Map<String, String> COMMAND_TYPE_MAP = Map.ofEntries(
			// Points
			Map.entry("Point", "point"),
			Map.entry("PointIn", "point"),
			Map.entry("Midpoint", "point"),
			Map.entry("Centroid", "point"),
			Map.entry("Center", "point"),
			Map.entry("Focus", "point"),
			Map.entry("Vertex", "point"),
			Map.entry("Intersect", "point"),
			Map.entry("IntersectPath", "point"),
			Map.entry("ClosestPoint", "point"),
			Map.entry("ClosestPointRegion", "point"),
			Map.entry("Corner", "point"),
			Map.entry("CenterOfMass", "point"),
			Map.entry("Extremum", "point"),
			Map.entry("TurningPoint", "point"),
			Map.entry("Root", "point"),
			Map.entry("ComplexRoot", "point"),
			Map.entry("Dilate", "point"),
			Map.entry("Reflect", "point"),
			Map.entry("Rotate", "point"),
			Map.entry("Translate", "point"),

			// Lines
			Map.entry("Line", "line"),
			Map.entry("Ray", "ray"),
			Map.entry("PerpendicularLine", "line"),
			Map.entry("OrthogonalLine", "line"),
			Map.entry("PerpendicularBisector", "line"),
			Map.entry("LineBisector", "line"),
			Map.entry("AngleBisector", "line"),
			Map.entry("Tangent", "line"),
			Map.entry("Asymptote", "line"),
			Map.entry("Polar", "line"),
			Map.entry("Directrix", "line"),
			Map.entry("Axis", "line"),
			Map.entry("MajorAxis", "line"),
			Map.entry("MinorAxis", "line"),
			Map.entry("FirstAxis", "line"),
			Map.entry("SecondAxis", "line"),

			// Segments
			Map.entry("Segment", "segment"),
			Map.entry("Semicircle", "conic"),

			// Conics
			Map.entry("Circle", "conic"),
			Map.entry("Ellipse", "conic"),
			Map.entry("Hyperbola", "conic"),
			Map.entry("Parabola", "conic"),
			Map.entry("Conic", "conic"),
			Map.entry("OsculatingCircle", "conic"),
			Map.entry("Incircle", "conic"),

			// Vectors
			Map.entry("Vector", "vector"),
			Map.entry("UnitVector", "vector"),
			Map.entry("OrthogonalVector", "vector"),
			Map.entry("UnitOrthogonalVector", "vector"),
			Map.entry("Direction", "vector"),
			Map.entry("UnitPerpendicularVector", "vector"),

			// Numerics
			Map.entry("Distance", "numeric"),
			Map.entry("Length", "numeric"),
			Map.entry("Angle", "angle"),
			Map.entry("Area", "numeric"),
			Map.entry("Slope", "numeric"),
			Map.entry("Radius", "numeric"),
			Map.entry("Circumference", "numeric"),
			Map.entry("Perimeter", "numeric"),
			Map.entry("CountIf", "numeric"),
			Map.entry("Sum", "numeric"),
			Map.entry("Product", "numeric"),
			Map.entry("Mean", "numeric"),
			Map.entry("Median", "numeric"),
			Map.entry("SD", "numeric"),
			Map.entry("Variance", "numeric"),
			Map.entry("Min", "numeric"),
			Map.entry("Max", "numeric"),

			// Polygons
			Map.entry("Polygon", "polygon"),
			Map.entry("RigidPolygon", "polygon"),

			// Text
			Map.entry("Text", "text"),
			Map.entry("LaTeX", "text"),
			Map.entry("TableText", "text"),
			Map.entry("FractionText", "text"),
			Map.entry("SurdText", "text"),
			Map.entry("FormulaText", "text"),
			Map.entry("Name", "text"),

			// Booleans
			Map.entry("AreEqual", "boolean"),
			Map.entry("AreCongruent", "boolean"),
			Map.entry("AreParallel", "boolean"),
			Map.entry("ArePerpendicular", "boolean"),
			Map.entry("AreCollinear", "boolean"),
			Map.entry("AreConcurrent", "boolean"),
			Map.entry("AreConcyclic", "boolean"),
			Map.entry("IsInteger", "boolean"),
			Map.entry("IsTangent", "boolean"),

			// Lists
			Map.entry("Sequence", "list"),
			Map.entry("Sort", "list"),
			Map.entry("Reverse", "list"),
			Map.entry("Union", "list"),
			Map.entry("Take", "list"),
			Map.entry("First", "list"),
			Map.entry("Last", "list"),
			Map.entry("Flatten", "list"),
			Map.entry("Unique", "list"),
			Map.entry("Zip", "list"),
			Map.entry("PointList", "list"),

			// Locus / Implicit
			Map.entry("Locus", "locus"),
			Map.entry("LocusEquation", "implicitpoly"),
			Map.entry("ImplicitCurve", "implicitpoly"),

			// Curves
			Map.entry("CurveCartesian", "curve"),

			// 3D types
			Map.entry("Plane", "plane3d"),
			Map.entry("Sphere", "quadric"),
			Map.entry("InfiniteCone", "quadric"),
			Map.entry("InfiniteCylinder", "quadric"),
			Map.entry("Cone", "quadriclimited"),
			Map.entry("Cylinder", "quadriclimited"),
			Map.entry("Prism", "polyhedron"),
			Map.entry("Pyramid", "polyhedron"),
			Map.entry("Tetrahedron", "polyhedron"),
			Map.entry("Cube", "polyhedron"),
			Map.entry("Octahedron", "polyhedron"),
			Map.entry("Icosahedron", "polyhedron"),
			Map.entry("Dodecahedron", "polyhedron"),
			Map.entry("Net", "polyhedron")
	);

	/**
	 * Infer missing element types for all labels in the given item.
	 * Only fills in types that are {@code null} (never overwrites explicit types).
	 * When inference fails and a default is used, a warning is added.
	 *
	 * @param item     the static item to process
	 * @param warnings list to collect warning messages (may be {@code null})
	 */
	public static void inferMissingTypes(GpadStaticItem item, List<String> warnings) {
		if (item == null || item.type == null) return;

		switch (item.type) {
			case INDEPENDENT:
				inferIndependentType(item, warnings);
				break;
			case COMMAND:
				inferCommandType(item, warnings);
				break;
			case EXPRESSION:
				// Expression types are handled by GeoGebra kernel during XML loading.
				// The <expression> tag doesn't need a type attribute for the kernel
				// to correctly determine the result type.
				break;
			default:
				break;
		}
	}

	private static void inferIndependentType(GpadStaticItem item, List<String> warnings) {
		if (item.labels.isEmpty()) return;
		GpadStaticItem.TypedLabel tl = item.labels.get(0);
		if (tl.elementType != null) return;

		String shorthand = item.shorthandText;

		if (shorthand == null || shorthand.trim().isEmpty()) {
			if (item.extraData != null) {
				tl.elementType = "numeric";
			} else {
				tl.elementType = "numeric";
				addWarning(warnings, tl.label,
						"No shorthand value for independent element, defaulting to 'numeric'");
			}
			return;
		}

		String inferred = inferFromShorthand(shorthand.trim(), tl.label);
		if (inferred != null) {
			tl.elementType = inferred;
		} else {
			tl.elementType = "numeric";
			addWarning(warnings, tl.label,
					"Cannot infer type from shorthand '" + shorthand.trim()
							+ "', defaulting to 'numeric'");
		}
	}

	/**
	 * Infer element type from a shorthand value string.
	 *
	 * @param shorthand trimmed shorthand text (e.g. "(1, 2)", "5", "\"hello\"")
	 * @param label     the label text (used for point vs vector heuristic)
	 * @return inferred element type, or {@code null} if unknown
	 */
	static String inferFromShorthand(String shorthand, String label) {
		if (shorthand.isEmpty()) return null;

		if (isTuple(shorthand)) {
			int arity = countTupleElements(shorthand);
			boolean isVector = isLowercaseLabel(label);
			if (arity == 2)
				return isVector ? "vector" : "point";
			if (arity >= 3)
				return isVector ? "vector3d" : "point3d";
			return null;
		}

		if (isQuotedString(shorthand))
			return "text";

		if ("true".equals(shorthand) || "false".equals(shorthand))
			return "boolean";

		if (isNumericLiteral(shorthand))
			return "numeric";

		return null;
	}

	private static boolean isTuple(String s) {
		return s.startsWith("(") && s.endsWith(")");
	}

	/**
	 * Counts the number of top-level comma-separated elements in a tuple string.
	 */
	static int countTupleElements(String tuple) {
		String inner = tuple.substring(1, tuple.length() - 1).trim();
		if (inner.isEmpty()) return 0;
		return GpadStaticItem.splitTopLevelComma(inner).size();
	}

	/**
	 * Checks if the string is a quoted string literal — either a simple quoted
	 * string ({@code "..."}, {@code `...`}) or a mixed-quote string
	 * ({@code "Hello`"`"World"}) consisting entirely of adjacent
	 * double-quoted and backtick-quoted segments.
	 */
	static boolean isQuotedString(String s) {
		if (s.length() >= 2 && (s.charAt(0) == '"' || s.charAt(0) == '`'))
			return isMixedQuotedString(s);
		return false;
	}

	/**
	 * Checks if the string consists entirely of adjacent double-quoted and
	 * backtick-quoted segments. Examples:
	 * <ul>
	 *   <li>{@code "hello"} → true</li>
	 *   <li>{@code `hello`} → true</li>
	 *   <li>{@code "Hello`"`"World"} → true (mixed quoting for escaping)</li>
	 *   <li>{@code "a" + "b"} → false (contains {@code +} outside quotes)</li>
	 * </ul>
	 */
	static boolean isMixedQuotedString(String s) {
		int i = 0;
		boolean hasSegment = false;
		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == '"') {
				int end = s.indexOf('"', i + 1);
				if (end < 0) return false;
				i = end + 1;
				hasSegment = true;
			} else if (c == '`') {
				int end = s.indexOf('`', i + 1);
				if (end < 0) return false;
				i = end + 1;
				hasSegment = true;
			} else {
				return false;
			}
		}
		return hasSegment;
	}

	private static boolean isNumericLiteral(String s) {
		if (s.isEmpty()) return false;
		int i = 0;
		if (s.charAt(0) == '-' || s.charAt(0) == '+') i++;
		if (i >= s.length()) return false;
		boolean hasDot = false;
		boolean hasDigit = false;
		for (; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= '0' && c <= '9') {
				hasDigit = true;
			} else if (c == '.' && !hasDot) {
				hasDot = true;
			} else if (c == 'E' || c == 'e') {
				if (i + 1 < s.length() && (s.charAt(i + 1) == '+' || s.charAt(i + 1) == '-'))
					i++;
			} else if (c == '\u00B0') {
				// degree symbol at end → angle
				return true;
			} else if (c == '\u03C0') {
				// pi symbol
				continue;
			} else {
				return false;
			}
		}
		return hasDigit;
	}

	/**
	 * Heuristic: labels starting with a lowercase letter are assumed to be vectors,
	 * labels starting with uppercase are assumed to be points.
	 */
	static boolean isLowercaseLabel(String label) {
		if (label == null || label.isEmpty()) return false;
		char first = label.charAt(0);
		return first >= 'a' && first <= 'z';
	}

	private static void inferCommandType(GpadStaticItem item, List<String> warnings) {
		if (item.commandName == null) return;

		String mappedType = COMMAND_TYPE_MAP.get(item.commandName);

		for (GpadStaticItem.TypedLabel tl : item.labels) {
			if (tl.elementType != null) continue;

			if (mappedType != null) {
				tl.elementType = mappedType;
			} else {
				tl.elementType = "numeric";
				addWarning(warnings, tl.label,
						"Unknown command '" + item.commandName
								+ "', defaulting to 'numeric'");
			}
		}
	}

	/**
	 * Looks up the inferred element type for a command name.
	 *
	 * @param commandName the command name (e.g. "Line", "Segment")
	 * @return inferred element type, or {@code null} if not in the mapping
	 */
	static String getCommandType(String commandName) {
		return COMMAND_TYPE_MAP.get(commandName);
	}

	private static void addWarning(List<String> warnings, String label, String message) {
		if (warnings == null) return;
		if (label != null && !label.isEmpty()) {
			warnings.add("Type inference: label '" + label + "': " + message);
		} else {
			warnings.add("Type inference: " + message);
		}
	}
}
