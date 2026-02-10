package org.geogebra.common.gpad;

import java.util.HashSet;
import java.util.LinkedHashMap;
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
	private final GpadGenerator gpadGenerator;
	private final Set<GeoElement> processedOutputs = new HashSet<>();

	/**
	 * Creates a new GgbToGpadConverter.
	 * 
	 * @param construction the construction to convert
	 * @param mergeStylesheets whether to merge identical stylesheets
	 */
	public GgbToGpadConverter(Construction construction, boolean mergeStylesheets) {
		this(construction, mergeStylesheets, false);
	}
	
	/**
	 * Creates a new GgbToGpadConverter.
	 * 
	 * @param construction the construction to convert
	 * @param mergeStylesheets whether to merge identical stylesheets
	 * @param inMacroConstruction whether this is for macro construction (needs indentation)
	 */
	private GgbToGpadConverter(Construction construction, boolean mergeStylesheets, boolean inMacroConstruction) {
		this.construction = construction;
		this.gpadGenerator = new GpadGenerator(mergeStylesheets, inMacroConstruction);
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
		GgbToGpadConverter macroConverter = new GgbToGpadConverter(macroCons, this.gpadGenerator.mergeStylesheets, true);
		macroConverter.convertConstruction(sb);
		
		// End macro definition: @@return output1, output2, ... }
		// Note: @@return statement does NOT end with semicolon
		sb.append("    @@return ");
		buildLabels(sb, macro.getMacroOutput());
		sb.append("\n}\n\n");
	}

	/**
	 * Converts the main construction to gpad format.
	 * 
	 * @param sb string builder to append to
	 */
	private void convertConstruction(StringBuilder sb) {
		int steps = construction.steps();

		// First pass: collect all items
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
					collectExpression(algo);
				else
					collectCommand(algo);
			} else if (ce.isGeoElement()) {
				GeoElement geo = (GeoElement) ce;
				AlgoElement parentAlgo = geo.getParentAlgorithm();
				
				// Skip if this geo has a parent algorithm - it should be processed by its parent
				if (parentAlgo != null)
					continue;
				
				if (geo.isIndependent())
					collectIndependentElement(geo);
			}
		}

		// Use GpadGenerator to handle dependency detection, topological sort, and output
		gpadGenerator.generate(sb);
	}

	/**
	 * Collects a command (AlgoElement that is not Expression) for later processing.
	 * 
	 * @param algo the algorithm element
	 */
	private void collectCommand(AlgoElement algo) {
		// Get command definition (includes command name and input parameters)
		String commandDef = algo.getDefinition(myTPL);
		if (commandDef == null || commandDef.isEmpty()) {
			Log.error("Command " + algo.getClassName() + " has no definition");
			return;
		}

		// Create CollectedItem
		GpadGenerator.CollectedItem item = new GpadGenerator.CollectedItem();
		item.commandString = commandDef;

		// Collect all outputs with their stylesheets
		for (int i = 0; i < algo.getOutputLength(); i++) {
			GeoElement outputGeo = algo.getOutput(i);
			if (outputGeo != null)
				collectGeo(outputGeo, item, null, false);
		}

		if (item.elements.isEmpty())
			return;

		// Extract dependencies from command definition (input arguments)
		// The command definition string contains the input parameters
		item.regularAttributeValues.add(commandDef);
		gpadGenerator.addCollectedItem(item);
	}

	/**
	 * Collects an expression (AlgoElement with Expression className) for later processing.
	 * Uses the same logic as AlgoElement.getExpXML() for getting expression string.
	 * 
	 * @param algo the algorithm element
	 */
	private void collectExpression(AlgoElement algo) {
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

		collectGeo(outputGeo, null, expString, true);
	}

	/**
	 * Collects an independent GeoElement for later processing.
	 * Uses the same logic as GeoElement.getExpressionXML() to determine if it should
	 * be treated as an expression or as an independent element.
	 * 
	 * @param geo the independent geo element
	 */
	private void collectIndependentElement(GeoElement geo) {
		String command = GeoElementToGpadConverter.extractCommand(geo);
		if (command == null || command.isEmpty())
			return;
		collectGeo(geo, null, command, false);
	}
	
	/**
	 * Gets visibility flags from a GeoElement.
	 * 
	 * @param geo the geo element
	 * @return visibility flags string (* and/or ~), or empty string if none
	 */
	private String getVisibilityFlags(GeoElement geo) {
		// * flag: when object is not shown
		if (!geo.isSetEuclidianVisible())
			return "*";
		// ~ flag: when label is not shown (only meaningful when object is shown)
		if (!geo.getLabelVisible())
			return "~";
		return "";
	}
	
	/**
	 * Extracts dependencies from a GeoElement's stylesheet.
	 * Similar to XMLToGpadConverter's extractAttributeValuesForDependency.
	 * 
	 * @param geo the geo element
	 * @param item the collected item to add dependencies to
	 */
	private GpadGenerator.CollectedItem collectGeo(GeoElement geo, GpadGenerator.CollectedItem item, String cmdString, boolean maybeExpr) {
		String label = GeoElementToGpadConverter.getOutputLabel(geo);
		if (label == null)
			return null;

		processedOutputs.add(geo);

		// Get visibility flags
		String visibilityFlags = getVisibilityFlags(geo);

		// Extract style map from GeoElement
		Map<String, LinkedHashMap<String, String>> styleMap = GeoElementToGpadConverter.extractStyleMap(geo);
		// Use GpadGenerator to generate stylesheet
		// Use getLabelSimple() for stylesheet name to avoid invalid names like "f(x)Style"
		String styleSheetName = gpadGenerator.generateStyleSheet(geo.getLabelSimple(), geo.getTypeString(), styleMap);

		boolean newItem = item == null;
		if (newItem)
			item = new GpadGenerator.CollectedItem();
		item.elements.add(new GpadGenerator.SingleElementInfo(label, visibilityFlags, styleSheetName));
		if (cmdString != null)
			item.commandString = cmdString;

		// Add expression to regularAttributeValues for dependency extraction
		if (maybeExpr)
			item.regularAttributeValues.add(cmdString);

		// Use GpadGenerator to extract attribute values for dependency extraction
		gpadGenerator.extractAttributeValuesForDependency(styleMap, item.regularAttributeValues, item.jsAttributeValues);

		if (newItem)
			gpadGenerator.addCollectedItem(item);

		return item;
	}
}
