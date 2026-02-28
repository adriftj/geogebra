package org.geogebra.desktop.gpadtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.geogebra.common.io.MyXMLio;
import org.geogebra.common.jre.io.StreamUtil;
import org.geogebra.desktop.headless.AppDNoGui;
import org.geogebra.desktop.main.LocalizationD;
import org.geogebra.common.plugin.GgbAPI;

/**
 * Command-line tool to convert GeoGebra (.ggb) files to GPAD format.
 * 
 * This tool uses {@link org.geogebra.common.gpad.ToGpadConverter} internally
 * to perform the conversion by extracting XML directly from the ggb zip archive,
 * which supports converting both the construction and all macros to GPAD format.
 * 
 * Usage:
 *   Single file: java GgbToGpad input.ggb
 *   Single file with output: java GgbToGpad -i input.ggb -o output.gpad
 *   Directory: java GgbToGpad -i inputDir -o outputDir
 * 
 * Options:
 *   -i <file|dir>  Input file or directory
 *   -o <file|dir>  Output file or directory
 *   -m             Merge identical stylesheets (default: false)
 *   -w             Overwrite existing files (default: skip and prompt)
 */
public class GgbToGpad {
	private static boolean mergeStylesheets = true;
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
			} else if ("-m".equals(arg)) {
				mergeStylesheets = true;
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
				// Default: same directory, change extension to .gpad
				String inputPath = input.getAbsolutePath();
				int lastDot = inputPath.lastIndexOf('.');
				if (lastDot > 0) {
					output = new File(inputPath.substring(0, lastDot) + ".gpad");
				} else {
					output = new File(inputPath + ".gpad");
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
				if (input.getName().toLowerCase().endsWith(".ggb")) {
					convertFile(input, output, mergeStylesheets, overwrite);
				} else {
					System.err.println("Error: Input file must have .ggb extension");
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
				convertDirectory(input, output, mergeStylesheets, overwrite);
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
	 * Convert a single GGB file to GPAD format by extracting XML directly
	 * from the ggb zip archive and converting it using xmlToGpad.
	 */
	private static boolean convertFile(File inputFile, File outputFile, 
			boolean mergeStyles, boolean overwrite) {
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

		// Extract embedded files from ggb zip archive
		extractEmbeddedFiles(inputFile, outputFile);

		try {
			// Extract XML files directly from ggb zip archive
			String xmlFile = extractXmlFromGgb(inputFile, MyXMLio.XML_FILE);
			if (xmlFile == null) {
				String error = "Failed to extract " + MyXMLio.XML_FILE + " from ggb file: " + inputPath;
				errors.add(error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}
			
			String xmlMacro = extractXmlFromGgb(inputFile, MyXMLio.XML_FILE_MACRO);
			// If macro file doesn't exist, use empty string
			if (xmlMacro == null) {
				xmlMacro = "";
			}
			
			AppDNoGui app = new AppDNoGui(new LocalizationD(3), true);
			GgbAPI ggbApi = app.getGgbApi();
			
			String gpadText = ggbApi.xmlToGpad(xmlFile, xmlMacro, mergeStyles);

			String xmlToGpadWarning = ggbApi.getLastWarning();
			if (xmlToGpadWarning != null && !xmlToGpadWarning.trim().isEmpty()) {
				for (String w : xmlToGpadWarning.split("\n")) {
					if (w != null && !w.trim().isEmpty()) {
						System.err.println("Warning [" + inputPath + "]: " + w.trim());
					}
				}
			}

			if (gpadText == null || gpadText.isEmpty()) {
				String error = "Conversion produced empty result: " + inputPath;
				errors.add(error);
				System.err.println("Error [" + inputPath + "]: " + error);
				failCount++;
				return false;
			}

			// Write to output file with UTF-8 encoding
			try (OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
				writer.write(gpadText);
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
		} catch (Exception e) {
			String error = "Unexpected error: " + e.getMessage();
			errors.add(inputPath + ": " + error);
			System.err.println("Error [" + inputPath + "]: " + error);
			System.err.println("Stack trace for " + inputPath + ":");
			e.printStackTrace();
			failCount++;
			return false;
		}
	}

	/**
	 * Recursively convert all GGB files in a directory
	 */
	private static void convertDirectory(File inputDir, File outputDir, 
			boolean mergeStyles, boolean overwrite) {
		if (!inputDir.isDirectory()) {
			errors.add("Input is not a directory: " + inputDir.getAbsolutePath());
			return;
		}

		// Get relative path from input directory
		Path inputPath = inputDir.toPath();
		Path outputPath = outputDir.toPath();

		// Recursively find all .ggb files
		List<File> ggbFiles = findGgbFiles(inputDir);

		if (ggbFiles.isEmpty()) {
			System.out.println("No .ggb files found in: " + inputDir.getAbsolutePath());
			return;
		}

		System.out.println("Found " + ggbFiles.size() + " .ggb file(s) to convert");

		// Convert each file
		for (File ggbFile : ggbFiles) {
			// Output current file being processed
			System.out.println("Processing: " + ggbFile.getAbsolutePath());
			
			// Calculate relative path
			Path relativePath = inputPath.relativize(ggbFile.toPath());
			
			// Determine output file path
			String ggbFileName = ggbFile.getName();
			String gpadFileName = ggbFileName.substring(0, ggbFileName.lastIndexOf('.')) + ".gpad";
			
			// Build output path: preserve directory structure
			Path parentPath = relativePath.getParent();
			Path outputFilePath;
			if (parentPath != null) {
				outputFilePath = outputPath.resolve(parentPath).resolve(gpadFileName);
			} else {
				// File is directly in the input directory root
				outputFilePath = outputPath.resolve(gpadFileName);
			}
			File outputFile = outputFilePath.toFile();

			// Convert file
			convertFile(ggbFile, outputFile, mergeStyles, overwrite);
		}
	}

	/**
	 * Extracts XML content from ggb zip archive.
	 * 
	 * @param inputFile the input ggb file (zip archive)
	 * @param xmlFileName the name of the XML file to extract (e.g., "geogebra.xml" or "geogebra_macro.xml")
	 * @return XML content as string, or null if file not found
	 */
	private static String extractXmlFromGgb(File inputFile, String xmlFileName) {
		try (ZipInputStream zip = new ZipInputStream(new FileInputStream(inputFile))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String entryName = entry.getName();
				if (xmlFileName.equals(entryName)) {
					// Found the XML file, read its content
					byte[] content = StreamUtil.loadIntoMemory(zip);
					return new String(content, StandardCharsets.UTF_8);
				}
				zip.closeEntry();
			}
		} catch (IOException e) {
			String error = "Failed to extract " + xmlFileName + ": " + e.getMessage();
			errors.add(inputFile.getAbsolutePath() + ": " + error);
			System.err.println("Error [" + inputFile.getAbsolutePath() + "]: " + error);
		}
		return null; // File not found
	}

	/**
	 * Extract embedded files from ggb zip archive to subdirectories relative to output gpad file
	 * 
	 * @param inputFile the input ggb file (zip archive)
	 * @param outputFile the output gpad file
	 */
	private static void extractEmbeddedFiles(File inputFile, File outputFile) {
		// System files that should not be extracted as embedded files
		Set<String> systemFiles = new HashSet<>();
		systemFiles.add(MyXMLio.XML_FILE);
		systemFiles.add(MyXMLio.XML_FILE_MACRO);
		systemFiles.add(MyXMLio.XML_FILE_DEFAULTS_2D);
		systemFiles.add(MyXMLio.XML_FILE_DEFAULTS_3D);
		systemFiles.add(MyXMLio.JAVASCRIPT_FILE);
		systemFiles.add(MyXMLio.XML_FILE_THUMBNAIL);
		systemFiles.add("structure.json");

		// Get output directory (where gpad file is located)
		File outputDir = outputFile.getParentFile();
		if (outputDir == null) {
			// If output file has no parent, use current directory
			outputDir = new File(".");
		}

		int extractedCount = 0;
		try (ZipInputStream zip = new ZipInputStream(new FileInputStream(inputFile))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String entryName = entry.getName();
				
				// Skip system files and directories
				if (systemFiles.contains(entryName) || entry.isDirectory()) {
					zip.closeEntry();
					continue;
				}

				// Extract embedded file
				// Preserve directory structure in zip, but relative to output directory
				// Normalize path: remove leading slash if present (zip entries may have it)
				String normalizedName = entryName;
				if (normalizedName.startsWith("/")) {
					normalizedName = normalizedName.substring(1);
				}
				Path entryPath = Paths.get(normalizedName).normalize();
				
				// Security check: ensure the path doesn't escape the output directory
				// Check for path traversal attempts (e.g., ".." or absolute paths)
				if (entryPath.isAbsolute() || entryPath.toString().contains("..")) {
					String error = "Skipping embedded file with dangerous path (would escape output directory): " + entryName;
					errors.add(inputFile.getAbsolutePath() + ": " + error);
					System.err.println("Error [" + inputFile.getAbsolutePath() + "]: " + error);
					zip.closeEntry();
					continue;
				}
				
				Path outputDirPath = outputDir.toPath().toAbsolutePath().normalize();
				Path outputPath = outputDirPath.resolve(entryPath).normalize();
				
				// Additional security check: ensure the resolved path is still within output directory
				if (!outputPath.startsWith(outputDirPath)) {
					String error = "Skipping embedded file with dangerous path (would escape output directory): " + entryName;
					errors.add(inputFile.getAbsolutePath() + ": " + error);
					System.err.println("Error [" + inputFile.getAbsolutePath() + "]: " + error);
					zip.closeEntry();
					continue;
				}
				
				// Create parent directories if needed
				File outputFileForEntry = outputPath.toFile();
				File parentDir = outputFileForEntry.getParentFile();
				if (parentDir != null && !parentDir.exists()) {
					parentDir.mkdirs();
				}

				// Extract file content
				byte[] content = StreamUtil.loadIntoMemory(zip);
				
				// Write to output file
				try (FileOutputStream fos = new FileOutputStream(outputFileForEntry)) {
					fos.write(content);
				}
				
				extractedCount++;
				zip.closeEntry();
			}
		} catch (IOException e) {
			// Log error but don't fail the conversion
			String error = "Failed to extract embedded files: " + e.getMessage();
			errors.add(inputFile.getAbsolutePath() + ": " + error);
			System.err.println("Warning [" + inputFile.getAbsolutePath() + "]: " + error);
		}

		if (extractedCount > 0) {
			System.out.println("Extracted " + extractedCount + " embedded file(s) from " + inputFile.getName());
		}
	}

	/**
	 * Recursively find all .ggb files in a directory
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
				// Recursively search subdirectories
				ggbFiles.addAll(findGgbFiles(file));
			} else if (file.isFile() && file.getName().toLowerCase().endsWith(".ggb")) {
				ggbFiles.add(file);
			}
		}

		return ggbFiles;
	}

	/**
	 * Print usage information
	 */
	private static void printUsage() {
		System.err.println("Usage: GgbToGpad [options] [input] [output]");
		System.err.println();
		System.err.println("Options:");
		System.err.println("  -i <file|dir>  Input file or directory");
		System.err.println("  -o <file|dir>  Output file or directory");
		System.err.println("  -m             Merge identical stylesheets (default: false)");
		System.err.println("  -w             Overwrite existing files (default: skip)");
		System.err.println();
		System.err.println("Examples:");
		System.err.println("  GgbToGpad input.ggb");
		System.err.println("  GgbToGpad -i input.ggb -o output.gpad");
		System.err.println("  GgbToGpad -i inputDir -o outputDir");
		System.err.println("  GgbToGpad -i dir -o dir -m -w");
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
