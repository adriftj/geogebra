package org.geogebra.common.gpad;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.Algos;
import org.geogebra.common.kernel.algos.ConstructionElement;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.util.debug.Log;

/**
 * Converts an entire Construction to Gpad format.
 * Enumerates all construction elements in order and converts them to gpad format.
 */
public class ConstructionToGpadConverter {
	static final StringTemplate myTPL = StringTemplate.noLocalDefault;

	private final Construction construction;
	private final boolean mergeStylesheets;
	private final Set<GeoElement> processedOutputs = new HashSet<>();
	private final Map<GeoElement, String> styleSheetMap = new HashMap<>();
	private final Map<String, String> styleSheetContentMap = new HashMap<>();
	private final Set<String> generatedStyleSheets = new HashSet<>();
	private int styleSheetCounter = 0;

	/**
	 * Creates a new ConstructionToGpadConverter.
	 * 
	 * @param construction the construction to convert
	 * @param mergeStylesheets whether to merge identical stylesheets
	 */
	public ConstructionToGpadConverter(Construction construction, boolean mergeStylesheets) {
		this.construction = construction;
		this.mergeStylesheets = mergeStylesheets;
	}

	/**
	 * Converts the entire construction to Gpad format.
	 * 
	 * @return Gpad string representation of the construction
	 */
	public String toGpad() {
		StringBuilder sb = new StringBuilder();
		int steps = construction.steps();

		for (int i = 0; i < steps; i++) {
			ConstructionElement ce = construction.getConstructionElement(i);
			if (ce == null)
				continue;

			// Skip if this is a GeoElement that has already been processed as output
			if (ce.isGeoElement()) {
				GeoElement geo = (GeoElement) ce;
				if (processedOutputs.contains(geo))
					continue;
			}

			if (ce.isAlgoElement()) {
				AlgoElement algo = (AlgoElement) ce;
				// Check if it's an Expression
				if (Algos.Expression.equals(algo.getClassName()))
					processExpression(algo, sb);
				else
					processCommand(algo, sb);
			} else if (ce.isGeoElement()) {
				GeoElement geo = (GeoElement) ce;
				if (geo.isIndependent())
					processIndependentElement(geo, sb);
			}
		}

		return sb.toString();
	}

	/**
	 * Processes a command (AlgoElement that is not Expression).
	 * 
	 * @param algo the algorithm element
	 * @param sb string builder to append to
	 */
	private void processCommand(AlgoElement algo, StringBuilder sb) {
		// Collect output objects with labels
		java.util.List<GeoElement> outputGeos = new java.util.ArrayList<>();
		java.util.List<String> outputLabels = new java.util.ArrayList<>();
		java.util.List<String> styleSheetNames = new java.util.ArrayList<>();

		for (int i = 0; i < algo.getOutputLength(); i++) {
			GeoElement outputGeo = algo.getOutput(i);
			if (outputGeo == null)
				continue;

			if (!outputGeo.isLabelSet()) {
				Log.error("Output object at index " + i + " of command " 
					+ algo.getClassName() + " has no label, skipping");
				continue;
			}

			String label = outputGeo.getLabel(myTPL);
			if (label == null || label.isEmpty()) {
				Log.error("Output object at index " + i + " of command " 
					+ algo.getClassName() + " has empty label, skipping");
				continue;
			}

			outputGeos.add(outputGeo);
			outputLabels.add(label);

			// Generate stylesheet for output object
			String styleSheetName = generateStyleSheetForOutput(outputGeo);
			styleSheetNames.add(styleSheetName);
		}

		if (outputGeos.isEmpty())
			return;

		// Generate stylesheet definitions first (only once per unique stylesheet)
		// Note: Stylesheet definitions do NOT end with semicolon (format: @name = { ... })
		for (int i = 0; i < outputGeos.size(); i++) {
			GeoElement outputGeo = outputGeos.get(i);
			String styleSheetName = styleSheetNames.get(i);
			appendStyleSheetDefinitionIfNeeded(sb, outputGeo, styleSheetName);
		}

		// Generate command instruction
		// Format: label1, label2, ... = CommandName(input1, input2, ...);
		boolean first = true;
		for (int i = 0; i < outputLabels.size(); i++) {
			if (!first)
				sb.append(", ");
			first = false;

			String label = outputLabels.get(i);
			sb.append(label);

			// Add visibility flags
			GeoElement outputGeo = outputGeos.get(i);
			boolean hideObject = !outputGeo.isSetEuclidianVisible();
			boolean hideLabel = !outputGeo.isLabelVisible();
			if (hideObject)
				sb.append("*");
			else if (hideLabel)
				sb.append("~");

			// Add stylesheet reference
			String styleSheetName = styleSheetNames.get(i);
			if (styleSheetName != null)
				sb.append(" @").append(styleSheetName);
		}

		// Get command definition (includes command name and input parameters)
		String commandDef = algo.getDefinition(myTPL);
		if (commandDef == null || commandDef.isEmpty()) {
			Log.error("Command " + algo.getClassName() + " has no definition");
			return;
		}

		// Command instruction must end with semicolon
		sb.append(" = ").append(commandDef).append(";\n");

		// Add all output objects to processed set
		processedOutputs.addAll(outputGeos);
	}

	/**
	 * Processes an expression (AlgoElement with Expression className).
	 * 
	 * @param algo the algorithm element
	 * @param sb string builder to append to
	 */
	private void processExpression(AlgoElement algo, StringBuilder sb) {
		if (algo.getOutputLength() != 1) {
			Log.error("Expression should have exactly one output, but has " 
				+ algo.getOutputLength());
			return;
		}

		GeoElement outputGeo = algo.getOutput(0);
		if (outputGeo == null) {
			Log.error("Expression output is null");
			return;
		}

		if (!outputGeo.isLabelSet()) {
			Log.error("Expression output has no label, skipping");
			return;
		}

		String label = outputGeo.getLabel(myTPL);
		if (label == null || label.isEmpty()) {
			Log.error("Expression output has empty label, skipping");
			return;
		}

		// Generate stylesheet for output object
		String styleSheetName = generateStyleSheetForOutput(outputGeo);

		// Generate stylesheet definition first (only once per unique stylesheet)
		appendStyleSheetDefinitionIfNeeded(sb, outputGeo, styleSheetName);

		// Generate expression instruction
		// Format: label = expString;
		sb.append(label);

		// Add visibility flags
		boolean hideObject = !outputGeo.isSetEuclidianVisible();
		boolean hideLabel = !outputGeo.isLabelVisible();
		if (hideObject)
			sb.append("*");
		else if (hideLabel)
			sb.append("~");

		// Add stylesheet reference
		if (styleSheetName != null)
			sb.append(" @").append(styleSheetName);

		// Get expression string
		// For Expression type, try to get from output GeoElement's definition first
		String expString = null;
		if (outputGeo.getDefinition() != null)
			expString = outputGeo.getDefinition().toString(myTPL);
		// Fallback to algo.getDefinition if outputGeo doesn't have definition
		if (expString == null || expString.isEmpty()) {
			// For Expression type, getDefinition returns the expression string (via toString)
			expString = algo.getDefinition(myTPL);
		}
		if (expString == null || expString.isEmpty()) {
			Log.error("Expression has no exp string");
			return;
		}

		// Expression instruction must end with semicolon
		sb.append(" = ").append(expString).append(";\n");

		// Add output object to processed set
		processedOutputs.add(outputGeo);
	}

	/**
	 * Processes an independent GeoElement.
	 * 
	 * @param geo the independent geo element
	 * @param sb string builder to append to
	 */
	private void processIndependentElement(GeoElement geo, StringBuilder sb) {
		if (!geo.isLabelSet()) {
			Log.error("Independent element has no label, skipping");
			return;
		}

		// Generate stylesheet for independent element (supports merging)
		String styleSheetName = generateStyleSheetForOutput(geo);

		// Generate stylesheet definition first (only once per unique stylesheet)
		appendStyleSheetDefinitionIfNeeded(sb, geo, styleSheetName);

		// Build command using GeoElementToGpadConverter
		if (GeoElementToGpadConverter.buildCommand(sb, geo, styleSheetName)) {
			sb.append("\n");
		}

		// Add to processed set
		processedOutputs.add(geo);
	}

	/**
	 * Appends stylesheet definition to StringBuilder if needed (only once per unique stylesheet).
	 * Note: Stylesheet definitions do NOT end with semicolon (format: @name = { ... })
	 * 
	 * @param sb string builder to append to
	 * @param geo the geo element
	 * @param styleSheetName the stylesheet name, or null if no stylesheet
	 */
	private void appendStyleSheetDefinitionIfNeeded(StringBuilder sb, GeoElement geo, String styleSheetName) {
		if (styleSheetName != null && !generatedStyleSheets.contains(styleSheetName)) {
			String styleSheetGpad = generateStyleSheetGpad(geo, styleSheetName);
			if (styleSheetGpad != null && !styleSheetGpad.isEmpty()) {
				sb.append(styleSheetGpad);
				sb.append("\n");
				generatedStyleSheets.add(styleSheetName);
			}
		}
	}

	/**
	 * Generates a stylesheet name for an output object.
	 * 
	 * @param outputGeo the output geo element
	 * @return stylesheet name, or null if no stylesheet needed
	 */
	private String generateStyleSheetForOutput(GeoElement outputGeo) {
		// Check if we already have a stylesheet for this geo
		if (styleSheetMap.containsKey(outputGeo))
			return styleSheetMap.get(outputGeo);

		// Extract style sheet content directly from GeoElement
		String contentOnly = GeoElementToGpadConverter.extractStyleSheetContent(outputGeo);
		if (contentOnly == null || contentOnly.isEmpty())
			return null;

		// Check if we should merge with existing stylesheet
		String styleSheetName;
		if (mergeStylesheets && styleSheetContentMap.containsKey(contentOnly)) {
			// Reuse existing stylesheet name
			styleSheetName = styleSheetContentMap.get(contentOnly);
		} else {
			// Generate new name
			String label = outputGeo.getLabelSimple();
			if (label != null && !label.isEmpty())
				styleSheetName = label + "Style";
			else
				styleSheetName = "style" + (++styleSheetCounter);

			// Store mapping for merging
			if (mergeStylesheets)
				styleSheetContentMap.put(contentOnly, styleSheetName);
		}

		styleSheetMap.put(outputGeo, styleSheetName);
		return styleSheetName;
	}

	/**
	 * Generates the gpad stylesheet definition string.
	 * 
	 * @param outputGeo the output geo element
	 * @param styleSheetName the stylesheet name
	 * @return gpad stylesheet string, or null if generation fails
	 */
	private String generateStyleSheetGpad(GeoElement outputGeo, String styleSheetName) {
		// Extract style sheet content directly from GeoElement
		String contentOnly = GeoElementToGpadConverter.extractStyleSheetContent(outputGeo);
		if (contentOnly == null || contentOnly.isEmpty())
			return null;

		// Build full stylesheet definition with name
		return "@" + styleSheetName + " = " + contentOnly;
	}
}

