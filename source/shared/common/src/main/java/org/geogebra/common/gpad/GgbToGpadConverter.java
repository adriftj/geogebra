package org.geogebra.common.gpad;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.Macro;
import org.geogebra.common.kernel.StringTemplate;
import org.geogebra.common.kernel.algos.AlgoElement;
import org.geogebra.common.kernel.algos.ConstructionElement;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.util.debug.Log;

/**
 * Converts an entire GeoGebra construction (including macros) to Gpad format.
 * Enumerates all construction elements in order and converts them to gpad format.
 * Macros are converted first, then the main construction.
 */
public class GgbToGpadConverter {
	static final StringTemplate myTPL = StringTemplate.noLocalDefault;

	private final Construction construction;
	private final boolean mergeStylesheets;
	private final Set<GeoElement> processedOutputs = new HashSet<>();
	private final Map<String, String> styleSheetContentMap = new HashMap<>();
	private final Set<String> generatedStyleSheets = new HashSet<>();
	private final Map<GeoElement, String> deferredStyleSheets = new HashMap<>();
	private int styleSheetCounter = 0;

	/**
	 * Creates a new GgbToGpadConverter.
	 * 
	 * @param construction the construction to convert
	 * @param mergeStylesheets whether to merge identical stylesheets
	 */
	public GgbToGpadConverter(Construction construction, boolean mergeStylesheets) {
		this.construction = construction;
		this.mergeStylesheets = mergeStylesheets;
	}

	/**
	 * Converts the entire construction (including macros) to Gpad format.
	 * Macros are converted first, then the main construction.
	 * 
	 * @return Gpad string representation of the construction
	 */
	public String toGpad() {
		StringBuilder sb = new StringBuilder();
		
		// Convert all macros first
		convertMacros(sb);
		
		// Then convert the main construction
		convertConstruction(sb);
		
		return sb.toString();
	}

	/**
	 * Converts all macros to gpad format.
	 * 
	 * @param sb string builder to append to
	 */
	private void convertMacros(StringBuilder sb) {
		Kernel kernel = construction.getKernel();
		if (kernel == null)
			return;
		
		List<Macro> macros = kernel.getAllMacros();
		if (macros == null || macros.isEmpty())
			return;
		
		// Convert each macro
		for (Macro macro : macros) {
			if (macro != null)
				convertMacro(macro, sb);
		}
	}

	private void buildLabels(StringBuilder sb, GeoElement[] geos) {
		boolean first = true;
		if (geos != null) {
			for (GeoElement geo : geos) {
				if (geo != null) {
					String label = geo.getLabelSimple();
					if (label != null && !label.isEmpty()) {
						if (!first)
							sb.append(", ");
						first = false;
						sb.append(label);
					}
				}
			}
		}
	}

	/**
	 * Converts a single macro to gpad format.
	 * Format: @@macro macroName(input1, input2, ...) { ... @@return output1, output2, ... }
	 * Note: @@return statement does NOT end with semicolon (see Parser.jj line 4458)
	 * 
	 * @param macro the macro to convert
	 * @param sb string builder to append to
	 */
	private void convertMacro(Macro macro, StringBuilder sb) {
		// Get macro name
		String macroName = macro.getCommandName();
		if (macroName == null || macroName.isEmpty()) {
			Log.error("Macro has no command name, skipping");
			return;
		}
		
		// Get macro construction
		Construction macroCons = macro.getMacroConstruction();
		if (macroCons == null) {
			Log.error("Macro " + macroName + " has no construction, skipping");
			return;
		}
		
		// Start macro definition: @@macro macroName(input1, input2, ...) {
		sb.append("@@macro ").append(macroName).append("(");
		buildLabels(sb, macro.getMacroInput());
		sb.append(") {\n");
		
		// Convert macro construction using a new converter instance
		// This ensures macro's stylesheets are independent from main construction
		GgbToGpadConverter macroConverter = new GgbToGpadConverter(macroCons, mergeStylesheets);
		String macroConstructionGpad = macroConverter.convertConstructionOnly();
		
		// Append macro construction content (indented)
		if (macroConstructionGpad != null && !macroConstructionGpad.isEmpty()) {
			// Indent each line of the macro construction
			String[] lines = macroConstructionGpad.split("\n");
			for (String line : lines) {
				if (!line.trim().isEmpty())
					sb.append("    ").append(line).append("\n");
			}
		}
		
		// End macro definition: @@return output1, output2, ... }
		// Note: @@return statement does NOT end with semicolon
		sb.append("    @@return ");
		buildLabels(sb, macro.getMacroOutput());
		sb.append("\n}\n\n");
	}

	/**
	 * Converts only the construction (without macros) to gpad format.
	 * This is used internally for macro construction conversion.
	 * 
	 * @param sb string builder to append to (optional, if null, returns string)
	 * @return Gpad string representation of the construction
	 */
	private String convertConstructionOnly() {
		StringBuilder sb = new StringBuilder();
		convertConstruction(sb);
		return sb.toString();
	}

	/**
	 * Converts the main construction to gpad format.
	 * 
	 * @param sb string builder to append to
	 */
	private void convertConstruction(StringBuilder sb) {
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
				// Use getDefinitionName() to determine if it's an Expression (same logic as XML generation)
				String cmdName = algo.getDefinitionName(myTPL);
				
				// Check if it's an Expression (same logic as AlgoElement.hasExpXML())
				if ("Expression".equals(cmdName))
					processExpression(algo, sb);
				else
					processCommand(algo, sb);
			} else if (ce.isGeoElement()) {
				GeoElement geo = (GeoElement) ce;
				AlgoElement parentAlgo = geo.getParentAlgorithm();
				
				// Skip if this geo has a parent algorithm - it should be processed by its parent
				if (parentAlgo != null)
					continue;
				
				if (geo.isIndependent())
					processIndependentElement(geo, sb);
			}
		}

		// Generate deferred @@set statements for stylesheets with expressions
		generateDeferredSetStatements(sb);
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
		java.util.List<String> styleSheetNames = new java.util.ArrayList<>();

		for (int i = 0; i < algo.getOutputLength(); i++) {
			GeoElement outputGeo = algo.getOutput(i);
			if (outputGeo == null)
				continue;

			outputGeos.add(outputGeo);

			// Generate stylesheet for output object
			String styleSheetName = generateStyleSheetForOutput(outputGeo, sb);
			styleSheetNames.add(styleSheetName);
		}

		if (outputGeos.isEmpty())
			return;

		// Generate command instruction
		// Format: label1, label2, ... = CommandName(input1, input2, ...);
		boolean first = true;
		for (int i = 0; i < outputGeos.size(); i++) {
			if (!first)
				sb.append(", ");
			first = false;

			GeoElementToGpadConverter.buildOutputLabel(sb, outputGeos.get(i));

			// Add stylesheet reference (skip if deferred)
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
	 * Uses the same logic as AlgoElement.getExpXML() for getting expression string.
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

		// Get expression string - use outputGeo's definition if available,
		// otherwise use algo's definition (same logic as AlgoElement.getExpXML() uses toExpString())
		String expString = null;
		if (outputGeo.getDefinition() != null)
			expString = outputGeo.getDefinition().toString(myTPL);
		if (expString == null || expString.isEmpty())
			expString = algo.getDefinition(myTPL);
		if (expString == null || expString.isEmpty()) {
			Log.error("Expression has no exp string");
			return;
		}

		// Generate stylesheet for output object
		String styleSheetName = generateStyleSheetForOutput(outputGeo, sb);

		// Generate expression instruction
		// Format: label = expString; or f(u,v,...) @style ... = ... for functions
		GeoElementToGpadConverter.buildOutputLabel(sb, outputGeo);

		if (styleSheetName != null)
			sb.append(" @").append(styleSheetName);

		// Expression instruction must end with semicolon
		sb.append(" = ").append(expString).append(";\n");

		// Add output object to processed set
		processedOutputs.add(outputGeo);
	}

	/**
	 * Processes an independent GeoElement.
	 * Uses the same logic as GeoElement.getExpressionXML() to determine if it should
	 * be treated as an expression or as an independent element.
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
		String styleSheetName = generateStyleSheetForOutput(geo, sb);

		// Build command using GeoElementToGpadConverter
		if (GeoElementToGpadConverter.buildCommand(sb, geo, styleSheetName))
			sb.append("\n");

		// Add to processed set
		processedOutputs.add(geo);
	}

	/**
	 * Generates a stylesheet name for an output object.
	 * 
	 * @param outputGeo the output geo element
	 * @param sb string builder to append to
	 * @return stylesheet name, or null if no stylesheet needed
	 */
	private String generateStyleSheetForOutput(GeoElement outputGeo, StringBuilder sb) {
		// Extract style sheet content directly from GeoElement
		Object[] result = GeoElementToGpadConverter.extractStyleSheetContent(outputGeo);
		String contentOnly = (result != null) ? (String) result[0] : null;
		Boolean hasExpression = (result != null) ? (Boolean) result[1] : Boolean.FALSE;
		if (contentOnly == null || contentOnly.isEmpty())
			return null;

		// Check if we should merge with existing stylesheet
		String styleSheetName = null;
		boolean needGenerate = true;
		if (mergeStylesheets) {
			styleSheetName = styleSheetContentMap.get(contentOnly);
			needGenerate = styleSheetName == null;
		}
		if (styleSheetName == null) {
			// Generate new name
			String label = outputGeo.getLabelSimple();
			if (label != null && !label.isEmpty())
				styleSheetName = label + "Style";
			else
				styleSheetName = "style" + (++styleSheetCounter);
			// check if styleSheetName is duplicated
			while (generatedStyleSheets.contains(styleSheetName))
				styleSheetName = "style" + (++styleSheetCounter);

			// Store mapping for merging
			if (mergeStylesheets)
				styleSheetContentMap.put(contentOnly, styleSheetName);
		}

		if (needGenerate) {
			sb.append("@").append(styleSheetName)
			  .append(" = ").append(contentOnly).append("\n");
			generatedStyleSheets.add(styleSheetName);
		}

		// If stylesheet contains expressions, record it for deferred application
		if (hasExpression != null && hasExpression.booleanValue()) {
			deferredStyleSheets.put(outputGeo, styleSheetName);
			return null;
		}
		return styleSheetName;
	}

	/**
	 * Generates deferred @@set statements for stylesheets that contain expressions.
	 * These statements are generated after all object creation statements to ensure
	 * that any objects referenced in the expressions have already been created.
	 * 
	 * @param sb string builder to append to
	 */
	private void generateDeferredSetStatements(StringBuilder sb) {
		if (deferredStyleSheets.isEmpty())
			return;

		for (Map.Entry<GeoElement, String> entry : deferredStyleSheets.entrySet()) {
			GeoElement geo = entry.getKey();
			String styleSheetName = entry.getValue();

			// Get label using getLabelSimple() (no visibility flags needed)
			String label = geo.getLabelSimple();
			if (label == null || label.isEmpty())
				continue;

			// Generate @@set statement: @@set label @styleSheetName;
			sb.append("@@set ").append(label).append(" @").append(styleSheetName).append(";\n");
		}
	}
}
