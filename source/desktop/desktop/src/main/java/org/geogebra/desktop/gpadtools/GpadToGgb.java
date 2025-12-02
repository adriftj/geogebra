package org.geogebra.desktop.gpadtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.geogebra.common.io.MyXMLio;
import org.geogebra.common.util.debug.Log;
import org.geogebra.desktop.headless.AppDNoGui;
import org.geogebra.desktop.main.LocalizationD;
import org.geogebra.common.plugin.GgbAPI;

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
	private static String embedRootDirPattern = null; // Relative path pattern (e.g., "../images" or "."), default is "."
	private static List<String> embedPatterns = new ArrayList<>();
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
			} else if ("-d".equals(arg)) {
				if (i + 1 >= args.length) {
					System.err.println("Error: -d requires a value");
					printUsage();
					System.exit(1);
				}
				embedRootDirPattern = args[++i];
			} else if ("-e".equals(arg)) {
				if (i + 1 >= args.length) {
					System.err.println("Error: -e requires a value");
					printUsage();
					System.exit(1);
				}
				embedPatterns.add(args[++i]);
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
					// Resolve embed root directory relative to gpad file directory
					File actualEmbedRoot = resolveEmbedRootDir(input, embedRootDirPattern);
					convertFile(input, output, overwrite, actualEmbedRoot);
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
	private static boolean convertFile(File inputFile, File outputFile, boolean overwrite, File embedRootDir) {
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
			
			// Check for warnings (non-fatal issues) - output before checking for errors
			// Warnings should be output even if parsing fails
			String lastWarning = ggbApi.getLastWarning();
			boolean hasWarnings = false;
			if (lastWarning != null && !lastWarning.trim().isEmpty()) {
				hasWarnings = true;
				// Output warnings (multiple warnings may be separated by newlines)
				String[] warnings = lastWarning.split("\n");
				for (String warning : warnings) {
					if (warning != null && !warning.trim().isEmpty()) {
						System.err.println("Warning [" + inputPath + "]: " + warning.trim());
					}
				}
			}
			
			if (result == null) {
				// Check for error message
				String error = ggbApi.getLastError();
				if (error == null || error.trim().isEmpty())
					error = "Failed to parse GPAD: Unknown error";
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
			
			// Collect files to embed
			Set<String> filesToEmbed = collectFilesToEmbed(gpadText.toString(), inputFile, embedRootDir);
			
			// Write to GGB file (ZIP format) with embedded files
			try (FileOutputStream fos = new FileOutputStream(outputFile)) {
				writeZippedWithEmbeddedFiles(fos, xmlString, filesToEmbed, embedRootDir, app);
			}

			// Output success message, including warning indicator if warnings were present
			if (hasWarnings) {
				System.out.println("Converted (with warnings): " + inputFile.getName() + " -> " + outputFile.getName());
			} else {
				System.out.println("Converted: " + inputFile.getName() + " -> " + outputFile.getName());
			}
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
						// Collect files to embed
						StringBuilder gpadTextForEmbed = new StringBuilder();
						try (InputStreamReader reader = new InputStreamReader(
								new FileInputStream(inputFile), StandardCharsets.UTF_8)) {
							char[] buffer = new char[8192];
							int read;
							while ((read = reader.read(buffer)) != -1) {
								gpadTextForEmbed.append(buffer, 0, read);
							}
						}
						Set<String> filesToEmbed = collectFilesToEmbed(gpadTextForEmbed.toString(), inputFile, embedRootDir);
						
						try (FileOutputStream fos = new FileOutputStream(outputFile)) {
							writeZippedWithEmbeddedFiles(fos, xmlString, filesToEmbed, embedRootDir, app);
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
	 * Resolve embed root directory relative to gpad file directory
	 * 
	 * @param gpadFile the gpad file
	 * @param embedRootPattern the pattern for embed root (relative path like "../images" or ".", null means ".")
	 * @return the resolved embed root directory
	 */
	private static File resolveEmbedRootDir(File gpadFile, String embedRootPattern) {
		File gpadDir = gpadFile.getParentFile();
		if (gpadDir == null) {
			gpadDir = new File(".");
		}
		
		// If no pattern specified, default to gpad file directory
		if (embedRootPattern == null || embedRootPattern.isEmpty()) {
			return gpadDir;
		}
		
		// Resolve pattern relative to gpad file directory
		Path gpadDirPath = gpadDir.toPath();
		Path embedRootPath = gpadDirPath.resolve(embedRootPattern).normalize();
		
		// Security check: ensure resolved path doesn't escape too far (optional, but good practice)
		// We allow relative paths like .. but we should ensure they're reasonable
		File embedRoot = embedRootPath.toFile();
		return embedRoot;
	}

	/**
	 * Collect files to embed from gpad content and command line patterns
	 */
	private static Set<String> collectFilesToEmbed(String gpadContent, File gpadFile, File embedRootDir) {
		Set<String> filesToEmbed = new HashSet<>();
		
		// Extract filenames from gpad content using regex
		// Pattern 1: filename:"path" or filename:path; (allows whitespace after : and before ;)
		Pattern filenamePattern = Pattern.compile("filename:\\s*(\"([^\"]+)\"|([^;]+?)\\s*;)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = filenamePattern.matcher(gpadContent);
		while (matcher.find()) {
			String filename = matcher.group(2); // Quoted path
			if (filename == null) {
				filename = matcher.group(3); // Unquoted path (ends with ;)
				if (filename != null) {
					// Remove trailing whitespace (semicolon already excluded by regex)
					filename = filename.trim();
				}
			} else {
				// Quoted path - trim in case there was whitespace handling
				filename = filename.trim();
			}
			if (filename != null && !filename.isEmpty()) {
				// Skip URLs and data URLs
				if (!filename.startsWith("http://") && !filename.startsWith("https://") 
						&& !filename.startsWith("data:") && !filename.startsWith("/")) {
					filesToEmbed.add(filename);
				}
			}
		}
		
		// Pattern 2: Image("path") - extract path from Image command
		// Match Image("path") or Image('path'), allowing whitespace
		Pattern imagePattern = Pattern.compile("Image\\s*\\(\\s*[\"']([^\"']+)[\"']\\s*\\)", Pattern.CASE_INSENSITIVE);
		Matcher imageMatcher = imagePattern.matcher(gpadContent);
		while (imageMatcher.find()) {
			String imagePath = imageMatcher.group(1);
			if (imagePath != null && !imagePath.isEmpty()) {
				// Skip URLs and data URLs
				if (!imagePath.startsWith("http://") && !imagePath.startsWith("https://") 
						&& !imagePath.startsWith("data:") && !imagePath.startsWith("/")) {
					filesToEmbed.add(imagePath);
				}
			}
		}
		
		// Add files from -e patterns
		for (String pattern : embedPatterns) {
			// Resolve pattern relative to embed root directory
			Path patternPath = Paths.get(pattern);
			if (patternPath.isAbsolute()) {
				// Absolute path - use as is
				findFilesByPattern(patternPath.toFile(), filesToEmbed, embedRootDir);
			} else {
				// Relative path - resolve against embed root
				File patternFile = embedRootDir.toPath().resolve(patternPath).toFile();
				findFilesByPattern(patternFile, filesToEmbed, embedRootDir);
			}
		}
		
		return filesToEmbed;
	}
	
	/**
	 * Find files matching a pattern (supports wildcards in filename part)
	 */
	private static void findFilesByPattern(File patternFile, Set<String> result, File embedRootDir) {
		if (patternFile == null) {
			return;
		}
		
		Path patternPath = patternFile.toPath().normalize();
		String patternStr = patternPath.toString();
		Path embedRootPath = embedRootDir.toPath().toAbsolutePath().normalize();
		
		// Check if pattern contains wildcards
		if (patternStr.contains("*") || patternStr.contains("?")) {
			// Extract directory and filename pattern
			File parentDir = patternFile.getParentFile();
			if (parentDir == null || !parentDir.isDirectory()) {
				return;
			}
			
			String filenamePattern = patternFile.getName();
			// Convert simple wildcard pattern to regex
			String regex = filenamePattern
				.replace(".", "\\.")
				.replace("*", ".*")
				.replace("?", ".");
			
			Pattern filePattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			
			// Search in directory
			File[] files = parentDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && filePattern.matcher(file.getName()).matches()) {
						// Get relative path from embed root
						Path filePath = file.toPath().toAbsolutePath().normalize();
						// Security check: ensure file is within embed root
						if (filePath.startsWith(embedRootPath)) {
							Path relativePath = embedRootPath.relativize(filePath);
							result.add(relativePath.toString().replace('\\', '/'));
						}
					}
				}
			}
		} else {
			// No wildcards - exact file match
			if (patternFile.isFile()) {
				// Get relative path from embed root
				Path filePath = patternFile.toPath().toAbsolutePath().normalize();
				// Security check: ensure file is within embed root
				if (filePath.startsWith(embedRootPath)) {
					Path relativePath = embedRootPath.relativize(filePath);
					result.add(relativePath.toString().replace('\\', '/'));
				}
			}
		}
	}
	
	/**
	 * Write GGB file with embedded files
	 */
	private static void writeZippedWithEmbeddedFiles(FileOutputStream fos, String xmlString, 
			Set<String> filesToEmbed, File embedRootDir, AppDNoGui app) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(fos);
		
		// Write XML file
		zip.putNextEntry(new ZipEntry(MyXMLio.XML_FILE));
		zip.write(xmlString.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
		
		// Write macro XML file if macros exist
		if (app != null && app.getKernel() != null && app.getKernel().hasMacros()) {
			java.util.ArrayList<org.geogebra.common.kernel.Macro> macros = app.getKernel().getAllMacros();
			if (macros != null && !macros.isEmpty()) {
				String macroXML = app.getXMLio().getFullMacroXML(macros);
				if (macroXML != null && !macroXML.isEmpty()) {
					zip.putNextEntry(new ZipEntry(MyXMLio.XML_FILE_MACRO));
					zip.write(macroXML.getBytes(StandardCharsets.UTF_8));
					zip.closeEntry();
				}
			}
		}
		
		// Write embedded files
		int embeddedCount = 0;
		for (String relativePath : filesToEmbed) {
			try {
				// Resolve file path relative to embed root directory
				Path filePath = embedRootDir.toPath().resolve(relativePath).normalize();
				
				// Security check: ensure file is within embed root directory
				Path embedRootPath = embedRootDir.toPath().toAbsolutePath().normalize();
				Path fileAbsolutePath = filePath.toAbsolutePath().normalize();
				
				if (!fileAbsolutePath.startsWith(embedRootPath)) {
					String error = "Skipping embedded file with dangerous path (would escape embed root): " + relativePath;
					errors.add(error);
					System.err.println("Error: " + error);
					continue;
				}
				
				File fileToEmbed = filePath.toFile();
				if (!fileToEmbed.exists() || !fileToEmbed.isFile()) {
					String error = "Embedded file not found: " + relativePath + " (resolved to: " + filePath.toAbsolutePath() + ")";
					errors.add(error);
					System.err.println("Warning: " + error);
					continue;
				}
				
				// Add file to zip
				String zipEntryName = relativePath.replace('\\', '/');
				zip.putNextEntry(new ZipEntry(zipEntryName));
				try (FileInputStream fis = new FileInputStream(fileToEmbed)) {
					byte[] buffer = new byte[8192];
					int read;
					while ((read = fis.read(buffer)) != -1) {
						zip.write(buffer, 0, read);
					}
				}
				zip.closeEntry();
				embeddedCount++;
			} catch (IOException e) {
				String error = "Failed to embed file " + relativePath + ": " + e.getMessage();
				errors.add(error);
				System.err.println("Warning: " + error);
			}
		}
		
		zip.close();
		
		if (embeddedCount > 0) {
			System.out.println("Embedded " + embeddedCount + " file(s)");
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

			// Resolve embed root directory relative to gpad file directory
			File actualEmbedRoot = resolveEmbedRootDir(gpadFile, embedRootDirPattern);
			// Convert file
			convertFile(gpadFile, outputFile, overwrite, actualEmbedRoot);
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
		System.err.println("  -d <dir>       Root directory for embedded files (relative to each gpad file directory)");
		System.err.println("                 Default: \".\" (gpad file directory). Supports relative paths like \"../images\"");
		System.err.println("  -e <pattern>   File pattern to embed (relative to -d, can appear multiple times)");
		System.err.println("                 Supports wildcards (*, ?) in filename part");
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
		
		// Note: Warnings are already printed during conversion, so we don't need to repeat them here
		// This keeps the summary clean while ensuring warnings are visible during processing
	}
}


