package org.geogebra.desktop.gpadtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.geos.GeoElement;

/**
 * Generates roundtrip comparison reports in console and JSON format.
 */
public class RoundtripReport {
	
	private final String inputFile;
	private final String timestamp;
	private final List<GeoComparisonResult> results = new ArrayList<>();
	private int totalOriginal;
	private int totalConverted;
	
	// Summary statistics
	private int matched;
	private int different;
	private int missing;
	private int extra;
	
	/**
	 * Creates a new roundtrip report.
	 * @param inputFile the input file path
	 */
	public RoundtripReport(String inputFile) {
		this.inputFile = inputFile;
		this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
	
	/**
	 * Compares two constructions and generates comparison results.
	 * @param original the original construction
	 * @param converted the converted construction
	 */
	public void compare(Construction original, Construction converted) {
		// Get all GeoElements from both constructions
		Map<String, GeoElement> originalGeos = new HashMap<>();
		Map<String, GeoElement> convertedGeos = new HashMap<>();
		
		// Collect original geos
		for (GeoElement geo : original.getGeoSetConstructionOrder()) {
			String label = geo.getLabelSimple();
			if (label != null && !label.isEmpty()) {
				originalGeos.put(label, geo);
			}
		}
		
		// Collect converted geos
		for (GeoElement geo : converted.getGeoSetConstructionOrder()) {
			String label = geo.getLabelSimple();
			if (label != null && !label.isEmpty()) {
				convertedGeos.put(label, geo);
			}
		}
		
		totalOriginal = originalGeos.size();
		totalConverted = convertedGeos.size();
		
		// Track which labels have been processed
		TreeSet<String> allLabels = new TreeSet<>();
		allLabels.addAll(originalGeos.keySet());
		allLabels.addAll(convertedGeos.keySet());
		
		// Compare each object
		for (String label : allLabels) {
			GeoElement origGeo = originalGeos.get(label);
			GeoElement convGeo = convertedGeos.get(label);
			
			GeoComparisonResult result;
			
			if (origGeo == null) {
				// Object only exists in converted (extra)
				result = new GeoComparisonResult(label, GeoComparisonResult.Status.EXTRA);
				result.setConvertedType(convGeo.getTypeString());
				extra++;
			} else if (convGeo == null) {
				// Object only exists in original (missing)
				result = new GeoComparisonResult(label, GeoComparisonResult.Status.MISSING);
				result.setOriginalType(origGeo.getTypeString());
				missing++;
			} else {
				// Object exists in both - compare
				result = new GeoComparisonResult(label);
				result.compare(origGeo, convGeo);
				
				if (result.hasDifferences()) {
					different++;
				} else {
					matched++;
				}
			}
			
			results.add(result);
		}
	}
	
	/**
	 * Gets the list of comparison results.
	 * @return comparison results
	 */
	public List<GeoComparisonResult> getResults() {
		return results;
	}
	
	/**
	 * Checks if the roundtrip was successful (no differences).
	 * @return true if all objects match
	 */
	public boolean isSuccess() {
		return different == 0 && missing == 0 && extra == 0;
	}
	
	/**
	 * Prints the report to console.
	 * @param verbose whether to print all differences in detail
	 */
	public void printToConsole(boolean verbose) {
		System.out.println();
		System.out.println("=== Roundtrip Test Report ===");
		System.out.println("File: " + inputFile);
		System.out.println("Time: " + timestamp);
		System.out.println();
		System.out.println("Summary:");
		System.out.println("  Original objects:  " + totalOriginal);
		System.out.println("  Converted objects: " + totalConverted);
		System.out.println("  Matched:           " + matched);
		System.out.println("  Different:         " + different);
		System.out.println("  Missing:           " + missing);
		System.out.println("  Extra:             " + extra);
		
		// Calculate success rate
		if (totalOriginal > 0) {
			double successRate = (double) matched / totalOriginal * 100;
			System.out.printf("  Success rate:      %.1f%%%n", successRate);
		}
		
		System.out.println();
		
		// Print differences
		if (different > 0 || missing > 0 || extra > 0) {
			System.out.println("Details:");
			
			// Missing objects
			if (missing > 0) {
				System.out.println();
				System.out.println("Missing objects (in original but not in converted):");
				for (GeoComparisonResult result : results) {
					if (result.getStatus() == GeoComparisonResult.Status.MISSING) {
						System.out.println("  - " + result.getLabel() + " (" + result.getOriginalType() + ")");
					}
				}
			}
			
			// Extra objects
			if (extra > 0) {
				System.out.println();
				System.out.println("Extra objects (in converted but not in original):");
				for (GeoComparisonResult result : results) {
					if (result.getStatus() == GeoComparisonResult.Status.EXTRA) {
						System.out.println("  - " + result.getLabel() + " (" + result.getConvertedType() + ")");
					}
				}
			}
			
			// Different objects
			if (different > 0) {
				System.out.println();
				System.out.println("Objects with differences:");
				for (GeoComparisonResult result : results) {
					if (result.getStatus() == GeoComparisonResult.Status.DIFFERENT) {
						System.out.println("  " + result.getLabel() + ":");
						if (verbose) {
							for (GeoComparisonResult.PropertyDifference diff : result.getDifferences()) {
								System.out.println("    - " + diff.property + ": " 
										+ diff.originalValue + " -> " + diff.convertedValue);
							}
						} else {
							// Just show count and property names
							List<String> propNames = new ArrayList<>();
							for (GeoComparisonResult.PropertyDifference diff : result.getDifferences()) {
								propNames.add(diff.property);
							}
							System.out.println("    " + result.getDifferences().size() 
									+ " differences: " + String.join(", ", propNames));
						}
					}
				}
			}
		} else {
			System.out.println("All objects match! Roundtrip successful.");
		}
		
		System.out.println();
	}
	
	/**
	 * Writes the report to a JSON file.
	 * @param outputFile the output file path
	 * @throws IOException if writing fails
	 */
	public void writeToJson(File outputFile) throws IOException {
		StringBuilder json = new StringBuilder();
		
		json.append("{\n");
		json.append("  \"inputFile\": ").append(escapeJson(inputFile)).append(",\n");
		json.append("  \"timestamp\": ").append(escapeJson(timestamp)).append(",\n");
		
		// Summary
		json.append("  \"summary\": {\n");
		json.append("    \"totalOriginal\": ").append(totalOriginal).append(",\n");
		json.append("    \"totalConverted\": ").append(totalConverted).append(",\n");
		json.append("    \"matched\": ").append(matched).append(",\n");
		json.append("    \"different\": ").append(different).append(",\n");
		json.append("    \"missing\": ").append(missing).append(",\n");
		json.append("    \"extra\": ").append(extra).append("\n");
		json.append("  },\n");
		
		// Differences
		json.append("  \"differences\": [\n");
		boolean firstDiff = true;
		for (GeoComparisonResult result : results) {
			if (result.getStatus() == GeoComparisonResult.Status.DIFFERENT) {
				if (!firstDiff) json.append(",\n");
				firstDiff = false;
				
				json.append("    {\n");
				json.append("      \"label\": ").append(escapeJson(result.getLabel())).append(",\n");
				json.append("      \"status\": \"different\",\n");
				json.append("      \"type\": {\n");
				json.append("        \"original\": ").append(escapeJson(result.getOriginalType())).append(",\n");
				json.append("        \"converted\": ").append(escapeJson(result.getConvertedType())).append("\n");
				json.append("      },\n");
				json.append("      \"differences\": [\n");
				
				boolean firstProp = true;
				for (GeoComparisonResult.PropertyDifference diff : result.getDifferences()) {
					if (!firstProp) json.append(",\n");
					firstProp = false;
					
					json.append("        {\n");
					json.append("          \"property\": ").append(escapeJson(diff.property)).append(",\n");
					json.append("          \"original\": ").append(escapeJson(diff.originalValue)).append(",\n");
					json.append("          \"converted\": ").append(escapeJson(diff.convertedValue)).append("\n");
					json.append("        }");
				}
				json.append("\n      ]\n");
				json.append("    }");
			}
		}
		json.append("\n  ],\n");
		
		// Missing
		json.append("  \"missing\": [");
		boolean firstMissing = true;
		for (GeoComparisonResult result : results) {
			if (result.getStatus() == GeoComparisonResult.Status.MISSING) {
				if (!firstMissing) json.append(", ");
				firstMissing = false;
				json.append(escapeJson(result.getLabel()));
			}
		}
		json.append("],\n");
		
		// Extra
		json.append("  \"extra\": [");
		boolean firstExtra = true;
		for (GeoComparisonResult result : results) {
			if (result.getStatus() == GeoComparisonResult.Status.EXTRA) {
				if (!firstExtra) json.append(", ");
				firstExtra = false;
				json.append(escapeJson(result.getLabel()));
			}
		}
		json.append("]\n");
		
		json.append("}\n");
		
		// Write to file
		try (OutputStreamWriter writer = new OutputStreamWriter(
				new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
			writer.write(json.toString());
		}
	}
	
	/**
	 * Escapes a string for JSON.
	 */
	private String escapeJson(String value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("\"");
		for (char c : value.toCharArray()) {
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < ' ') {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		sb.append("\"");
		return sb.toString();
	}
	
	// Getters for statistics
	public int getTotalOriginal() { return totalOriginal; }
	public int getTotalConverted() { return totalConverted; }
	public int getMatched() { return matched; }
	public int getDifferent() { return different; }
	public int getMissing() { return missing; }
	public int getExtra() { return extra; }
}
