package org.geogebra.desktop.gpadtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.io.XMLParseException;
import org.geogebra.common.kernel.Construction;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.plugin.GgbAPI;
import org.geogebra.common.util.debug.Log;
import org.geogebra.desktop.headless.AppDNoGui;
import org.geogebra.desktop.headless.GFileHandler;
import org.geogebra.desktop.main.LocalizationD;

/**
 * Command-line tool to test GeoGebra to GPAD roundtrip conversion.
 * 
 * This tool:
 * 1. Loads a GGB file
 * 2. Converts it to GPAD format
 * 3. Parses the GPAD back to a new construction
 * 4. Compares the original and converted Geo objects
 * 5. Generates a report (console + JSON)
 * 
 * Usage:
 *   Single file: java GgbGpadRoundtripTest input.ggb
 *   With options: java GgbGpadRoundtripTest -i input.ggb -o reportDir -v
 *   Directory: java GgbGpadRoundtripTest -i inputDir -o reportDir
 * 
 * Options:
 *   -i <file|dir>  Input GGB file or directory
 *   -o <dir>       Output directory for reports (default: current directory)
 *   -m             Merge identical stylesheets in GPAD conversion
 *   -v             Verbose mode (print all differences in detail)
 *   -w             Overwrite existing report files
 */
public class GgbGpadRoundtripTest {
	
	private static boolean mergeStylesheets = false;
	private static boolean verbose = false;
	private static boolean overwrite = false;
	private static List<String> errors = new ArrayList<>();
	private static int successCount = 0;
	private static int failCount = 0;
	private static int skipCount = 0;
	
	// Aggregated statistics for directory processing
	private static int totalMatched = 0;
	private static int totalDifferent = 0;
	private static int totalMissing = 0;
	private static int totalExtra = 0;
	private static int totalOriginalGeos = 0;
	private static int totalConvertedGeos = 0;

	/**
	 * Main entry point
	 */
	public static void main(String[] args) {
		// Set headless mode to prevent AWT initialization issues
		System.setProperty("java.awt.headless", "true");
		
		if (args.length == 0) {
			printUsage();
			System.exit(1);
		}

		File input = null;
		File outputDir = null;

		// Parse command line arguments
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if ("-i".equals(arg)) {
				if (i + 1 >= args.length) {
					System.err.println("Error: -i requires a value");
					printUsage();
					System.exit(1);
				}
				input = new File(args[++i]);
			} else if ("-o".equals(arg)) {
				if (i + 1 >= args.length) {
					System.err.println("Error: -o requires a value");
					printUsage();
					System.exit(1);
				}
				outputDir = new File(args[++i]);
			} else if ("-m".equals(arg)) {
				mergeStylesheets = true;
			} else if ("-v".equals(arg)) {
				verbose = true;
			} else if ("-w".equals(arg)) {
				overwrite = true;
			} else if (arg.startsWith("-")) {
				System.err.println("Error: Unknown option: " + arg);
				printUsage();
				System.exit(1);
			} else {
				// Positional argument: treat as input file if not set
				if (input == null) {
					input = new File(arg);
				} else {
					System.err.println("Error: Too many arguments");
					printUsage();
					System.exit(1);
				}
			}
		}

		if (input == null) {
			System.err.println("Error: Input file or directory not specified");
			printUsage();
			System.exit(1);
		}

		if (!input.exists()) {
			System.err.println("Error: Input does not exist: " + input.getAbsolutePath());
			System.exit(1);
		}

		// Determine output directory
		if (outputDir == null) {
			outputDir = new File(".");
		}
		if (!outputDir.exists()) {
			outputDir.mkdirs();
		}

		// Perform roundtrip test
		try {
			if (input.isFile()) {
				// Single file test
				if (input.getName().toLowerCase().endsWith(".ggb")) {
					String baseName = input.getName();
					baseName = baseName.substring(0, baseName.lastIndexOf('.'));
					File reportFile = new File(outputDir, baseName + "_roundtrip.json");
					testFile(input, reportFile, verbose, overwrite);
				} else {
					System.err.println("Error: Input file must have .ggb extension");
					System.exit(1);
				}
			} else {
				// Directory test
				testDirectory(input, outputDir, verbose, overwrite);
			}

			// Print summary
			printSummary();

			// Exit with error code if there were failures
			if (failCount > 0 || errors.size() > 0) {
				System.exit(1);
			}
		} catch (Exception e) {
			System.err.println("Fatal error: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Test a single GGB file
	 */
	private static boolean testFile(File inputFile, File reportFile, boolean verbose, boolean overwrite) {
		String inputPath = inputFile.getAbsolutePath();
		
		// Check if report file exists
		if (reportFile.exists() && !overwrite) {
			System.err.println("Skipping (report exists): " + inputPath);
			skipCount++;
			return false;
		}

		System.out.println("Testing: " + inputFile.getName());

		AppDNoGui originalApp = null;
		AppDNoGui convertedApp = null;
		
		try {
			// Create headless app instance for original
			originalApp = createHeadlessApp(inputPath);
			
			// Load GGB file
			boolean loaded = false;
			try (FileInputStream fis = new FileInputStream(inputFile)) {
				loaded = GFileHandler.loadXML(originalApp, fis, false);
			}
			
			if (!loaded) {
				String error = "Failed to load GGB file: " + inputPath;
				errors.add(error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			// Stop animations
			stopAnimations(originalApp);
			
			// Get original construction
			Construction originalCons = originalApp.getKernel().getConstruction();
			
			// Convert to GPAD
			GgbAPI ggbApi = originalApp.getGgbApi();
			String gpadText = ggbApi.toGpad(mergeStylesheets);
			
			if (gpadText == null || gpadText.isEmpty()) {
				String error = "GPAD conversion produced empty result: " + inputPath;
				errors.add(error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			// Create new app for converted construction
			convertedApp = createHeadlessApp(inputPath);
			
			// Parse GPAD into new construction
			GgbAPI convertedApi = convertedApp.getGgbApi();
			String result = convertedApi.evalGpad(gpadText);
			
			if (result == null) {
				String error = convertedApi.getLastError();
				if (error == null || error.trim().isEmpty()) {
					error = "Failed to parse GPAD: Unknown error";
				}
				errors.add(inputPath + ": " + error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			// Get converted construction
			Construction convertedCons = convertedApp.getKernel().getConstruction();
			
			// Compare constructions
			RoundtripReport report = new RoundtripReport(inputFile.getName());
			report.compare(originalCons, convertedCons);
			
			// Print to console
			report.printToConsole(verbose);
			
			// Write JSON report
			report.writeToJson(reportFile);
			System.out.println("Report written to: " + reportFile.getName());
			
			// Update aggregated statistics
			totalMatched += report.getMatched();
			totalDifferent += report.getDifferent();
			totalMissing += report.getMissing();
			totalExtra += report.getExtra();
			totalOriginalGeos += report.getTotalOriginal();
			totalConvertedGeos += report.getTotalConverted();
			
			if (report.isSuccess()) {
				successCount++;
			} else {
				failCount++;
			}
			
			return report.isSuccess();

		} catch (XMLParseException e) {
			String error = "XML parse error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			failCount++;
			return false;
		} catch (IOException e) {
			String error = "IO error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			failCount++;
			return false;
		} catch (Exception e) {
			String error = "Unexpected error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			e.printStackTrace();
			failCount++;
			return false;
		} finally {
			// Clean up
			cleanupApp(originalApp);
			cleanupApp(convertedApp);
		}
	}

	/**
	 * Creates a headless app instance with proper configuration.
	 */
	private static AppDNoGui createHeadlessApp(String inputPath) {
		AppDNoGui app = new AppDNoGui(new LocalizationD(3), true);
		
		// Override Log handler to prevent AssertionError
		Log.setLogger(new Log() {
			@Override
			public void print(Log.Level level, Object logEntry) {
				if (logEntry instanceof RuntimeException) {
					Throwable t = (Throwable) logEntry;
					Throwable cause = t.getCause();
					if (cause != null && cause.getClass().getName().contains("ParseException")) {
						System.err.println("Warning [" + inputPath + "]: LaTeX parsing error (non-fatal): " + cause.getMessage());
					} else {
						System.err.println("Warning [" + inputPath + "]: RuntimeException in headless mode: " + t.getMessage());
					}
				} else if (logEntry instanceof Throwable) {
					((Throwable) logEntry).printStackTrace();
				} else if (level == Log.Level.ERROR || level == Log.Level.WARN) {
					System.err.println(logEntry);
				}
			}
		});
		
		// Disable UI updates
		app.getKernel().setNotifyRepaintActive(false);
		app.getKernel().setNotifyViewsActive(false);
		app.getKernel().getAnimationManager().stopAnimation();
		
		return app;
	}

	/**
	 * Stops all animations in the app.
	 */
	private static void stopAnimations(AppDNoGui app) {
		app.getKernel().getAnimationManager().stopAnimation();
		app.getKernel().setNotifyRepaintActive(false);
		app.getKernel().setNotifyViewsActive(false);
		
		// Disable all animating objects
		GeoElement[] allObjects = app.getKernel().getConstruction()
				.getGeoSetConstructionOrder().toArray(new GeoElement[0]);
		for (GeoElement geo : allObjects) {
			if (geo != null && geo.isAnimating()) {
				geo.setAnimating(false);
			}
		}
		
		app.getKernel().getAnimationManager().stopAnimation();
	}

	/**
	 * Cleans up an app instance.
	 */
	private static void cleanupApp(AppDNoGui app) {
		if (app != null) {
			try {
				app.getKernel().getAnimationManager().stopAnimation();
				app.getKernel().setNotifyRepaintActive(false);
				
				GeoElement[] allObjects = app.getKernel().getConstruction()
						.getGeoSetConstructionOrder().toArray(new GeoElement[0]);
				for (GeoElement geo : allObjects) {
					if (geo != null && geo.isAnimating()) {
						geo.setAnimating(false);
					}
				}
				
				app.reset();
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}

	/**
	 * Recursively test all GGB files in a directory.
	 */
	private static void testDirectory(File inputDir, File outputDir, boolean verbose, boolean overwrite) {
		if (!inputDir.isDirectory()) {
			errors.add("Input is not a directory: " + inputDir.getAbsolutePath());
			return;
		}

		Path inputPath = inputDir.toPath();
		Path outputPath = outputDir.toPath();

		// Recursively find all .ggb files
		List<File> ggbFiles = findGgbFiles(inputDir);

		if (ggbFiles.isEmpty()) {
			System.out.println("No .ggb files found in: " + inputDir.getAbsolutePath());
			return;
		}

		System.out.println("Found " + ggbFiles.size() + " .ggb file(s) to test");
		System.out.println();

		// Test each file
		for (File ggbFile : ggbFiles) {
			// Calculate relative path
			Path relativePath = inputPath.relativize(ggbFile.toPath());
			
			// Determine report file path
			String baseName = ggbFile.getName();
			baseName = baseName.substring(0, baseName.lastIndexOf('.'));
			String reportFileName = baseName + "_roundtrip.json";
			
			// Build output path: preserve directory structure
			Path parentPath = relativePath.getParent();
			Path reportFilePath;
			if (parentPath != null) {
				reportFilePath = outputPath.resolve(parentPath).resolve(reportFileName);
				// Ensure parent directory exists
				reportFilePath.getParent().toFile().mkdirs();
			} else {
				reportFilePath = outputPath.resolve(reportFileName);
			}
			File reportFile = reportFilePath.toFile();

			testFile(ggbFile, reportFile, verbose, overwrite);
		}
	}

	/**
	 * Recursively find all .ggb files in a directory.
	 */
	private static List<File> findGgbFiles(File dir) {
		List<File> ggbFiles = new ArrayList<>();
		if (!dir.isDirectory()) {
			return ggbFiles;
		}

		File[] files = dir.listFiles();
		if (files == null) {
			return ggbFiles;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				ggbFiles.addAll(findGgbFiles(file));
			} else if (file.isFile() && file.getName().toLowerCase().endsWith(".ggb")) {
				ggbFiles.add(file);
			}
		}

		return ggbFiles;
	}

	/**
	 * Print usage information.
	 */
	private static void printUsage() {
		System.err.println("Usage: GgbGpadRoundtripTest [options] input.ggb");
		System.err.println();
		System.err.println("Options:");
		System.err.println("  -i <file|dir>  Input GGB file or directory");
		System.err.println("  -o <dir>       Output directory for reports (default: current directory)");
		System.err.println("  -m             Merge identical stylesheets in GPAD conversion");
		System.err.println("  -v             Verbose mode (print all differences in detail)");
		System.err.println("  -w             Overwrite existing report files");
		System.err.println();
		System.err.println("Examples:");
		System.err.println("  GgbGpadRoundtripTest input.ggb");
		System.err.println("  GgbGpadRoundtripTest -i input.ggb -o reports -v");
		System.err.println("  GgbGpadRoundtripTest -i inputDir -o reportDir");
		System.err.println("  GgbGpadRoundtripTest -i dir -o dir -m -v -w");
	}

	/**
	 * Print test summary.
	 */
	private static void printSummary() {
		System.out.println();
		System.out.println("========================================");
		System.out.println("Roundtrip Test Summary");
		System.out.println("========================================");
		System.out.println();
		System.out.println("File Statistics:");
		System.out.println("  Files tested:  " + (successCount + failCount));
		System.out.println("  Passed:        " + successCount);
		System.out.println("  Failed:        " + failCount);
		if (skipCount > 0) {
			System.out.println("  Skipped:       " + skipCount);
		}
		
		System.out.println();
		System.out.println("Geo Object Statistics (aggregated):");
		System.out.println("  Total original:  " + totalOriginalGeos);
		System.out.println("  Total converted: " + totalConvertedGeos);
		System.out.println("  Matched:         " + totalMatched);
		System.out.println("  Different:       " + totalDifferent);
		System.out.println("  Missing:         " + totalMissing);
		System.out.println("  Extra:           " + totalExtra);
		
		if (totalOriginalGeos > 0) {
			double successRate = (double) totalMatched / totalOriginalGeos * 100;
			System.out.printf("  Overall success: %.1f%%%n", successRate);
		}

		if (!errors.isEmpty()) {
			System.err.println();
			System.err.println("Errors:");
			for (String error : errors) {
				System.err.println("  " + error);
			}
		}
	}
}
