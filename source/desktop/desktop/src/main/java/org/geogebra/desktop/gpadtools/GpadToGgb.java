package org.geogebra.desktop.gpadtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.util.debug.Log;
import org.geogebra.desktop.headless.AppDNoGui;
import org.geogebra.desktop.main.LocalizationD;
import org.geogebra.common.plugin.GgbAPI;
import org.geogebra.common.jre.io.MyXMLioJre;

/**
 * Command-line tool to convert GPAD format files to GeoGebra (.ggb) files.
 * 
 * Usage:
 *   Single file: java GpadToGgb input.gpad
 *   Single file with output: java GpadToGgb -i input.gpad -o output.ggb
 *   Directory: java GpadToGgb -i inputDir -o outputDir
 * 
 * Options:
 *   -i <file|dir>  Input file or directory
 *   -o <file|dir>  Output file or directory
 *   -w             Overwrite existing files (default: skip and prompt)
 */
public class GpadToGgb {

	private static boolean overwrite = false;
	private static List<String> errors = new ArrayList<>();
	private static int successCount = 0;
	private static int failCount = 0;
	private static int skipCount = 0;

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
		File output = null;

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
				output = new File(args[++i]);
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
				} else if (output == null) {
					output = new File(arg);
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

		// Determine output location
		if (output == null) {
			if (input.isFile()) {
				// Default: same directory, change extension to .ggb
				String inputPath = input.getAbsolutePath();
				int lastDot = inputPath.lastIndexOf('.');
				if (lastDot > 0) {
					output = new File(inputPath.substring(0, lastDot) + ".ggb");
				} else {
					output = new File(inputPath + ".ggb");
				}
			} else {
				System.err.println("Error: Output directory required when input is a directory");
				printUsage();
				System.exit(1);
			}
		}

		// Perform conversion
		try {
			if (input.isFile()) {
				// Single file conversion
				if (input.getName().toLowerCase().endsWith(".gpad")) {
					convertFile(input, output, overwrite);
				} else {
					System.err.println("Error: Input file must have .gpad extension");
					System.exit(1);
				}
			} else {
				// Directory conversion
				if (!output.exists()) {
					output.mkdirs();
				}
				if (!output.isDirectory()) {
					System.err.println("Error: Output must be a directory when input is a directory");
					System.exit(1);
				}
				convertDirectory(input, output, overwrite);
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
	 * Convert a single GPAD file to GGB format
	 */
	private static boolean convertFile(File inputFile, File outputFile, boolean overwrite) {
		String inputPath = inputFile.getAbsolutePath();
		
		// Check if output file exists
		if (outputFile.exists() && !overwrite) {
			System.err.println("Skipping (file exists): " + inputPath + " -> " + outputFile.getAbsolutePath());
			skipCount++;
			return false;
		}

		// Ensure output directory exists
		File outputDir = outputFile.getParentFile();
		if (outputDir != null && !outputDir.exists()) {
			outputDir.mkdirs();
		}

		AppDNoGui app = null;
		try {
			// Create headless app instance
			app = new AppDNoGui(new LocalizationD(3), true);
			
			// Override the default Log handler to prevent AssertionError from being thrown
			// The default handler in AppCommon throws AssertionError for RuntimeException,
			// which may cause issues during GPAD parsing
			Log.setLogger(new Log() {
				@Override
				public void print(Log.Level level, Object logEntry) {
					if (logEntry instanceof RuntimeException) {
						// Don't throw AssertionError, just print the error
						Throwable t = (Throwable) logEntry;
						Throwable cause = t.getCause();
						if (cause != null && cause.getClass().getName().contains("ParseException")) {
							// LaTeX parsing error - log as warning but don't throw
							System.err.println("Warning [" + inputPath + "]: LaTeX parsing error (non-fatal): " + cause.getMessage());
						} else {
							// Other RuntimeException - print but don't throw
							System.err.println("Warning [" + inputPath + "]: RuntimeException in headless mode: " + t.getMessage());
							t.printStackTrace();
						}
					} else if (logEntry instanceof Throwable) {
						((Throwable) logEntry).printStackTrace();
					} else {
						System.out.println(logEntry);
					}
				}
			});
			
			// Disable animations, repainting, and view notifications to prevent blocking
			app.getKernel().setNotifyRepaintActive(false);
			app.getKernel().setNotifyViewsActive(false);
			app.getKernel().getAnimationManager().stopAnimation();
			
			// Read GPAD file (UTF-8 encoding)
			StringBuilder gpadText = new StringBuilder();
			try (InputStreamReader reader = new InputStreamReader(
					new FileInputStream(inputFile), StandardCharsets.UTF_8)) {
				char[] buffer = new char[8192];
				int read;
				while ((read = reader.read(buffer)) != -1) {
					gpadText.append(buffer, 0, read);
				}
			}
			
			if (gpadText.length() == 0) {
				String error = "GPAD file is empty: " + inputPath;
				errors.add(error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			// Parse GPAD and create construction
			GgbAPI ggbApi = app.getGgbApi();
			String result = ggbApi.evalGpad(gpadText.toString());
			
			if (result == null) {
				// Check for error message
				String lastError = ggbApi.getLastError();
				String error = "Failed to parse GPAD: " + (lastError != null ? lastError : "Unknown error");
				errors.add(inputPath + ": " + error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			// Get XML representation of the construction
			String xmlString = ggbApi.getXML();
			
			if (xmlString == null || xmlString.isEmpty()) {
				String error = "Conversion produced empty XML: " + inputPath;
				errors.add(error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			// Write to GGB file (ZIP format)
			try (FileOutputStream fos = new FileOutputStream(outputFile)) {
				MyXMLioJre.writeZipped(fos, new StringBuilder(xmlString));
			}

			System.out.println("Converted: " + inputFile.getName() + " -> " + outputFile.getName());
			successCount++;
			return true;

		} catch (IOException e) {
			String error = "IO error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			failCount++;
			return false;
		} catch (AssertionError e) {
			// Handle AssertionError (may be caused by LaTeX parsing errors)
			Throwable cause = e.getCause();
			if (cause != null && cause.getClass().getName().contains("ParseException")) {
				String error = "LaTeX parsing error (non-fatal): " + cause.getMessage();
				errors.add(inputPath + ": " + error);
				System.err.println("Warning [" + inputPath + "]: " + error);
				System.err.println("Attempting to continue conversion despite LaTeX error...");
				
				// Try to continue with conversion even if LaTeX rendering failed
				try {
					// Ensure repainting is still disabled
					app.getKernel().setNotifyRepaintActive(false);
					app.getKernel().getAnimationManager().stopAnimation();
					
					// Get XML and write to file
					GgbAPI ggbApi = app.getGgbApi();
					String xmlString = ggbApi.getXML();
					
					if (xmlString != null && !xmlString.isEmpty()) {
						try (FileOutputStream fos = new FileOutputStream(outputFile)) {
							MyXMLioJre.writeZipped(fos, new StringBuilder(xmlString));
						}
						System.out.println("Converted (with LaTeX warning): " + inputFile.getName() + " -> " + outputFile.getName());
						successCount++;
						return true;
					}
				} catch (Exception continueException) {
					String error2 = "Failed to continue after LaTeX error: " + continueException.getMessage();
					errors.add(inputPath + ": " + error2);
					System.err.println("Error [" + inputPath + "]: " + error2);
					failCount++;
					return false;
				}
			}
			// Other AssertionError - treat as fatal
			String error = "Assertion error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			System.err.println("Stack trace for " + inputPath + ":");
			e.printStackTrace();
			failCount++;
			return false;
		} catch (RuntimeException e) {
			String error = "Runtime error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			System.err.println("Stack trace for " + inputPath + ":");
			e.printStackTrace();
			failCount++;
			return false;
		} catch (Exception e) {
			String error = "Unexpected error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			System.err.println("Stack trace for " + inputPath + ":");
			e.printStackTrace();
			failCount++;
			return false;
		} finally {
			// Clean up app instance if needed
			if (app != null) {
				try {
					// Stop all animations and timers
					app.getKernel().getAnimationManager().stopAnimation();
					app.getKernel().setNotifyRepaintActive(false);
					
					// Disable all animating objects
					org.geogebra.common.kernel.geos.GeoElement[] allObjects = 
						app.getKernel().getConstruction().getGeoSetConstructionOrder()
							.toArray(new org.geogebra.common.kernel.geos.GeoElement[0]);
					for (org.geogebra.common.kernel.geos.GeoElement geo : allObjects) {
						if (geo != null && geo.isAnimating()) {
							geo.setAnimating(false);
						}
					}
					
					app.reset();
				} catch (Exception cleanupException) {
					// Ignore cleanup errors
				}
			}
		}
	}

	/**
	 * Recursively convert all GPAD files in a directory
	 */
	private static void convertDirectory(File inputDir, File outputDir, boolean overwrite) {
		if (!inputDir.isDirectory()) {
			errors.add("Input is not a directory: " + inputDir.getAbsolutePath());
			return;
		}

		// Get relative path from input directory
		Path inputPath = inputDir.toPath();
		Path outputPath = outputDir.toPath();

		// Recursively find all .gpad files
		List<File> gpadFiles = findGpadFiles(inputDir);

		if (gpadFiles.isEmpty()) {
			System.out.println("No .gpad files found in: " + inputDir.getAbsolutePath());
			return;
		}

		System.out.println("Found " + gpadFiles.size() + " .gpad file(s) to convert");

		// Convert each file
		for (File gpadFile : gpadFiles) {
			// Output current file being processed
			System.out.println("Processing: " + gpadFile.getAbsolutePath());
			
			// Calculate relative path
			Path relativePath = inputPath.relativize(gpadFile.toPath());
			
			// Determine output file path
			String gpadFileName = gpadFile.getName();
			String ggbFileName = gpadFileName.substring(0, gpadFileName.lastIndexOf('.')) + ".ggb";
			
			// Build output path: preserve directory structure
			Path parentPath = relativePath.getParent();
			Path outputFilePath;
			if (parentPath != null) {
				outputFilePath = outputPath.resolve(parentPath).resolve(ggbFileName);
			} else {
				// File is directly in the input directory root
				outputFilePath = outputPath.resolve(ggbFileName);
			}
			File outputFile = outputFilePath.toFile();

			// Convert file
			convertFile(gpadFile, outputFile, overwrite);
		}
	}

	/**
	 * Recursively find all .gpad files in a directory
	 */
	private static List<File> findGpadFiles(File dir) {
		List<File> gpadFiles = new ArrayList<>();
		if (!dir.isDirectory()) {
			return gpadFiles;
		}

		File[] files = dir.listFiles();
		if (files == null) {
			return gpadFiles;
		}

		for (File file : files) {
			if (file.isDirectory()) {
				// Recursively search subdirectories
				gpadFiles.addAll(findGpadFiles(file));
			} else if (file.isFile() && file.getName().toLowerCase().endsWith(".gpad")) {
				gpadFiles.add(file);
			}
		}

		return gpadFiles;
	}

	/**
	 * Print usage information
	 */
	private static void printUsage() {
		System.err.println("Usage: GpadToGgb [options] [input] [output]");
		System.err.println();
		System.err.println("Options:");
		System.err.println("  -i <file|dir>  Input file or directory");
		System.err.println("  -o <file|dir>  Output file or directory");
		System.err.println("  -w             Overwrite existing files (default: skip)");
		System.err.println();
		System.err.println("Examples:");
		System.err.println("  GpadToGgb input.gpad");
		System.err.println("  GpadToGgb -i input.gpad -o output.ggb");
		System.err.println("  GpadToGgb -i inputDir -o outputDir");
		System.err.println("  GpadToGgb -i dir -o dir -w");
	}

	/**
	 * Print conversion summary
	 */
	private static void printSummary() {
		System.out.println();
		System.out.println("Conversion Summary:");
		System.out.println("  Success: " + successCount);
		if (skipCount > 0) {
			System.out.println("  Skipped: " + skipCount);
		}
		if (failCount > 0) {
			System.err.println("  Failed: " + failCount);
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


