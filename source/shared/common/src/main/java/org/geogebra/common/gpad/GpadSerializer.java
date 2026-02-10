package org.geogebra.common.gpad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for serializing and deserializing Gpad style properties.
 * Handles serialization and deserialization of startPoint corners and barTag bars.
 */
public class GpadSerializer {
    /**
     * Functional interface for processing a deserialized startPoint corner.
     */
    @FunctionalInterface
    public interface StartPointCornerHandler {
        /**
         * Processes a deserialized corner.
         * @param firstCorner true if this is the first corner
         * @param isAbsolute true if absolute positioning
         * @param cornerData array with [0]=exp, [1]=x, [2]=y, [3]=z
         */
        void handle(boolean firstCorner, boolean isAbsolute, String[] cornerData);
    }
    
    /**
     * Functional interface for processing a deserialized barTag bar.
     */
    @FunctionalInterface
    public interface BarTagBarHandler {
        /**
         * Processes a deserialized bar.
         * @param barNumber bar number as string
         * @param rgba array with [0]=r, [1]=g, [2]=b, [3]=alpha (-1 if not set)
         * @param fillTypeXML fill type XML value (null if not set)
         * @param hatchAngle hatch angle string (null if not set)
         * @param hatchDistance hatch distance string (null if not set)
         * @param image image string (null if not set)
         * @param fillSymbol fill symbol string (null if not set)
         */
        void handle(String barNumber, int[] rgba, String fillTypeXML, String hatchAngle,
                String hatchDistance, String image, String fillSymbol);
    }
    
    /**
     * Serializes a single startPoint corner to the StringBuilder.
     * Format: [absolute byte][type byte][content]
     *   - absolute byte: \u0002=true, \u0003=false
     *   - type byte: \u0002=exp, \u0003=x/y/z
     *   - content: if exp, then exp string; if x/y/z, then "x,y,z" (z optional, no trailing comma)
     * 
     * @param serialized StringBuilder to append serialized data
     * @param firstCorner true if this is the first corner (no separator needed)
     * @param isAbsolute true if absolute positioning
     * @param cornerData array with [0]=exp, [1]=x, [2]=y, [3]=z
     */
    public static void serializeStartPointCorner(StringBuilder serialized, boolean firstCorner,
            boolean isAbsolute, String[] cornerData) {
        // Add separator if not first corner
        if (!firstCorner)
            serialized.append((char)1); // SOH: separator between corners
        
        // (1) absolute: \u0002=true, \u0003=false
        serialized.append(isAbsolute ? (char)2 : (char)3);
        
        // (2) type: \u0002=exp, \u0003=x/y/z
        String exp = cornerData[0];
        if (exp != null) {
            // exp type
            serialized.append((char)2);
            // (3) exp content
            serialized.append(exp);
        } else {
            // x/y/z type
            String x = cornerData[1];
            String y = cornerData[2];
            String z = cornerData[3];
            serialized.append((char)3);
            // (3) x,y,z content (z optional, no trailing comma)
            serialized.append(x);
            serialized.append(',');
            serialized.append(y);
            if (z != null) {
                serialized.append(',');
                serialized.append(z);
            }
        }
    }
    
    /**
     * Serializes a single barTag bar to the StringBuilder.
     * Format: [length char][barNumber char][flags char][属性值序列]
     *   - length: total length of barNumber + flags + 属性值序列 (excluding length itself)
     *   - barNumber: 1 char (0-65535)
     *   - flags: 1 char, bit flags
     *     bit 0 (0x01): barColor (r, g, b)
     *     bit 1 (0x02): barAlpha
     *     bit 2 (0x04): barFillType
     *     bit 3 (0x08): barHatchAngle
     *     bit 4 (0x10): barHatchDistance
     *     bit 5 (0x20): barImage (字符串，以 ETX 结尾)
     *     bit 6 (0x40): barSymbol
     *   - 属性值序列（按 flags 顺序）
     * 
     * @param serialized StringBuilder to append serialized data
     * @param barNumber bar number (1-based)
     * @param rgba array with [0]=r, [1]=g, [2]=b, [3]=alpha (-1 if not set)
     * @param fillTypeXML fill type XML value (null if not set)
     * @param hatchAngle hatch angle string (null if not set)
     * @param hatchDistance hatch distance string (null if not set)
     * @param image image string (null if not set)
     * @param fillSymbol fill symbol string (null if not set)
     * @return true if serialization succeeded, false if bar should be skipped
     */
    public static boolean serializeBarTagBar(StringBuilder serialized, String barNumber,
            int[] rgba, String fillTypeXML, String hatchAngle, String hatchDistance,
            String image, String fillSymbol) {
        // Build bar data first to calculate length
        StringBuilder barData = new StringBuilder();
        
        // (1) barNumber: 1 char (0-65535)
        try {
            int barNumberInt = Integer.parseInt(barNumber);
            barData.append((char)barNumberInt);
        } catch (NumberFormatException e) {
            // Invalid barNumber, skip this bar
            return false;
        }
        
        // (2) flags: 1 char, bit flags
        char flags = 0;
        if (rgba[0] >= 0 && rgba[1] >= 0 && rgba[2] >= 0)
            flags |= 0x01; // bit 0: barColor
        if (rgba[3] >= 0)
            flags |= 0x02; // bit 1: barAlpha
        if (fillTypeXML != null)
            flags |= 0x04; // bit 2: barFillType
        if (hatchAngle != null)
            flags |= 0x08; // bit 3: barHatchAngle
        if (hatchDistance != null)
            flags |= 0x10; // bit 4: barHatchDistance
        if (image != null)
            flags |= 0x20; // bit 5: barImage
        if (fillSymbol != null)
            flags |= 0x40; // bit 6: barSymbol
        barData.append(flags);
        
        // (3) 属性值序列（按 flags 顺序）
        // bit 0: barColor (r, g, b)
        if ((flags & 0x01) != 0) {
            barData.append((char)rgba[0]);
            barData.append((char)rgba[1]);
            barData.append((char)rgba[2]);
        }
        
        // bit 1: barAlpha
        if ((flags & 0x02) != 0)
            barData.append((char)rgba[3]);
        
        // bit 2: barFillType
        if ((flags & 0x04) != 0 && fillTypeXML != null) {
            try {
                int fillTypeInt = Integer.parseInt(fillTypeXML);
                if (fillTypeInt >= 0 && fillTypeInt <= 9) {
                    barData.append((char)fillTypeInt);
                }
            } catch (NumberFormatException e) {
                // Invalid fillType, skip this bar
                return false;
            }
        }
        
        // bit 3: barHatchAngle
        if ((flags & 0x08) != 0 && hatchAngle != null) {
            try {
                int angle = Integer.parseInt(hatchAngle);
                // Normalize angle to 0-359 range
                angle = ((angle % 360) + 360) % 360;
                barData.append((char)angle);
            } catch (NumberFormatException e) {
                // Invalid angle, skip this bar
                return false;
            }
        }
        
        // bit 4: barHatchDistance
        if ((flags & 0x10) != 0 && hatchDistance != null) {
            try {
                int dist = Integer.parseInt(hatchDistance);
                if (dist > 65535) dist = 65535;
                barData.append((char)dist);
            } catch (NumberFormatException e) {
                // Invalid distance, skip this bar
                return false;
            }
        }
        
        // bit 5: barImage (字符串，以 ETX 结尾)
        if ((flags & 0x20) != 0) {
            barData.append(image);
            barData.append((char)3); // ETX: end of image string
        }
        
        // bit 6: barSymbol
        if ((flags & 0x40) != 0)
            barData.append(fillSymbol.charAt(0));
        
        // Calculate length and prepend it
        int barLength = barData.length();
        if (barLength > 65535) {
            // Bar too long, skip it
            return false;
        }
        serialized.append((char)barLength);
        serialized.append(barData);
        return true;
    }
    
    /**
     * Deserializes startPoint corners from a serialized string and calls the handler for each corner.
     * Format: [corner1][\u0001][corner2]...
     * Each corner: [absolute byte][type byte][content]
     *   - absolute byte: \u0002=true, \u0003=false
     *   - type byte: \u0002=exp, \u0003=x/y/z
     *   - content: if exp, then exp string; if x/y/z, then "x,y,z" (z optional)
     * 
     * @param serialized the serialized string (from _corners attribute)
     * @param handler the handler to call for each deserialized corner
     */
    public static void deserializeStartPointCorners(String serialized, StartPointCornerHandler handler) {
        if (serialized == null || serialized.isEmpty())
            return;
        
        // Deserialize corners (separated by \u0001)
        String[] cornerStrings = serialized.split("\u0001", -1);
        if (cornerStrings.length == 0)
            return;
        
        boolean firstCorner = true;
        for (String cornerStr : cornerStrings) {
            if (cornerStr.isEmpty())
                continue;
            
            // Parse corner format: [absolute byte][type byte][content]
            if (cornerStr.length() < 2)
                continue; // Invalid format
            
            // (1) Parse absolute byte
            char absoluteByte = cornerStr.charAt(0);
            boolean isAbsolute = (absoluteByte == '\u0002');
            
            // (2) Parse type byte
            char typeByte = cornerStr.charAt(1);
            boolean isExp = (typeByte == '\u0002');
            
            // (3) Parse content
            String content = cornerStr.substring(2);
            
            // Build cornerData array
            String[] cornerData = new String[4]; // [0]=exp, [1]=x, [2]=y, [3]=z
            if (isExp) {
                // Expression-based corner
                cornerData[0] = content;
            } else {
                // x/y/z type: content is "x,y,z" or "x,y" (z optional)
                String[] coords = content.split(",", -1);
                if (coords.length >= 2) {
                    cornerData[1] = coords[0];
                    cornerData[2] = coords[1];
                    if (coords.length >= 3 && !coords[2].isEmpty())
                        cornerData[3] = coords[2];
                }
            }
            
            // Call handler
            handler.handle(firstCorner, isAbsolute, cornerData);
            firstCorner = false;
        }
    }
    
    /**
     * Detect startPoint corners has expression.
     * @param serialized the serialized string (from _corners attribute)
     * @return true if has expression, false else
     */
    public static boolean hasStartPointExp(String serialized) {
        if (serialized == null || serialized.isEmpty())
            return false;
        
        // Deserialize corners (separated by \u0001)
        String[] cornerStrings = serialized.split("\u0001", -1);
        if (cornerStrings.length == 0)
            return false;
        
        for (String cornerStr : cornerStrings) {
            if (cornerStr.isEmpty())
                continue;
            
            // Parse corner format: [absolute byte][type byte][content]
            if (cornerStr.length() < 2)
                continue; // Invalid format
            
            // Parse type byte
            char typeByte = cornerStr.charAt(1);
            boolean isExp = (typeByte == '\u0002');
            if (isExp)
                return true;
        }
        return false;
    }
    
    /**
     * Helper class for serializing startPoint corners.
     */
    public static class GpadSerializeStartPoint {
        private final List<LinkedHashMap<String, String>> startPoints = new ArrayList<>();
        
        private GpadSerializeStartPoint() {
        }
        
        /**
         * Adds a startPoint corner from its attributes.
         * @param attrs corner attributes (should contain number, absolute, exp, x, y, z)
         */
        public void add(LinkedHashMap<String, String> attrs) {
            if (attrs == null)
                return;
            
            // Use number attribute as list index
            int number = 0;
            String numberStr = attrs.get("number");
            if (numberStr != null) {
                try {
                    number = Integer.parseInt(numberStr);
                } catch (NumberFormatException e) {
                    // Invalid number, use 0 as default
                    number = 0;
                }
            }
            
            if (number < 0) {
                org.geogebra.common.util.debug.Log.error(
                    "startPoint number attribute is negative: " + number + ". Ignoring.");
            } else {
                // Grow list if necessary
                while (startPoints.size() <= number)
                    startPoints.add(null);
                startPoints.set(number, attrs);
            }
        }
        
        /**
         * Ends serialization and returns the serialized string.
         * @return serialized string, or null if no corners were added
         */
        public String end() {
            if (startPoints.isEmpty())
                return null;
            
            StringBuilder serialized = new StringBuilder();
            boolean first = true;
            
            for (int i = 0; i < startPoints.size(); i++) {
                LinkedHashMap<String, String> corner = startPoints.get(i);
                if (corner == null) {
                    // Check if there are any non-null entries after this
                    for (int j = i + 1; j < startPoints.size(); j++) {
                        if (startPoints.get(j) != null) {
                            org.geogebra.common.util.debug.Log.error(
                                "startPoint: found null at index " + i + ", but non-null entry exists at index " + j + ". Ignoring entries after index " + i);
                            break;
                        }
                    }
                    break;
                }
                
                // Extract corner data
                boolean isAbsolute = "true".equals(corner.get("absolute"));
                String[] cornerData = new String[4]; // [0]=exp, [1]=x, [2]=y, [3]=z
                cornerData[0] = corner.get("exp");
                cornerData[1] = corner.get("x");
                cornerData[2] = corner.get("y");
                cornerData[3] = corner.get("z");
                
                // Serialize using helper class
                serializeStartPointCorner(serialized, first, isAbsolute, cornerData);
                first = false;
            }
            
            // Return serialized string only if we have at least one corner
            return first ? null : serialized.toString();
        }
    }
    
    /**
     * Helper class for serializing barTag bars.
     */
    public static class GpadSerializeBarTag {
        private final Map<Integer, Map<String, String>> tagElements = new LinkedHashMap<>();
        
        private GpadSerializeBarTag() {
        }
        
        /**
         * Adds a tag element from its attributes.
         * @param barNumberStr bar number as string
         * @param attrs tag attributes (should contain key, value, and bar-related attributes)
         */
        public void add(String barNumberStr, LinkedHashMap<String, String> attrs) {
            if (barNumberStr == null || attrs == null)
                return;
            
            String key = attrs.get("key");
            String value = attrs.get("value");
            
            if (key != null && value != null) {
                try {
                    int barNumber = Integer.parseInt(barNumberStr);
                    // Get or create map for this barNumber
                    Map<String, String> barTags = tagElements.get(barNumber);
                    if (barTags == null) {
                        barTags = new LinkedHashMap<>();
                        tagElements.put(barNumber, barTags);
                    }
                    // Store key-value pair for this barNumber
                    barTags.put(key, value);
                } catch (NumberFormatException e) {
                    // Invalid barNumber, ignore
                }
            }
        }
        
        /**
         * Ends serialization and returns the serialized string.
         * @return serialized string, or null if no bars were added
         */
        public String end() {
            if (tagElements.isEmpty())
                return null;
            
            StringBuilder serialized = new StringBuilder();
            
            // Sort by barNumber for consistent output
            List<Integer> barNumbers = new ArrayList<>(tagElements.keySet());
            java.util.Collections.sort(barNumbers);
            
            for (Integer barNumber : barNumbers) {
                Map<String, String> barTags = tagElements.get(barNumber);
                if (barTags == null || barTags.isEmpty())
                    continue;
                
                // Extract bar attributes
                String fillType = barTags.get("barFillType");
                String hatchAngle = barTags.get("barHatchAngle");
                String hatchDistance = barTags.get("barHatchDistance");
                String image = barTags.get("barImage");
                String fillSymbol = barTags.get("barSymbol");
                
                // Parse barColor (format: rgb(r,g,b) or rgba(r,g,b,a))
                int[] rgba = {-1, -1, -1, -1};
                String barColor = barTags.get("barColor");
                if (barColor != null && (barColor.startsWith("rgb(") || barColor.startsWith("rgba(")) && barColor.endsWith(")")) {
                    // Remove "rgb(" or "rgba(" prefix and ")" suffix
                    int prefixLen = barColor.startsWith("rgba(") ? 5 : 4;
                    String colorContent = barColor.substring(prefixLen, barColor.length() - 1);
                    String[] rgb = colorContent.split(",");
                    if (rgb.length >= 3) {
                        try {
                            rgba[0] = Integer.parseInt(rgb[0].trim());
                            rgba[1] = Integer.parseInt(rgb[1].trim());
                            rgba[2] = Integer.parseInt(rgb[2].trim());
                        } catch (NumberFormatException e) {
                            // Invalid color format, ignore
                        }
                    }
                }
                
                // Parse barAlpha (format: float string 0.0-1.0, convert to int 0-255)
                String barAlpha = barTags.get("barAlpha");
                if (barAlpha != null) {
                    try {
                        float alphaFloat = Float.parseFloat(barAlpha);
                        int alphaInt = (int)(alphaFloat * 255);
                        if (alphaInt < 0) alphaInt = 0;
                        if (alphaInt > 255) alphaInt = 255;
                        rgba[3] = alphaInt;
                    } catch (NumberFormatException e) {
                        // Invalid alpha, ignore
                    }
                }
                
                // Serialize using helper class
                if (!serializeBarTagBar(serialized, String.valueOf(barNumber), rgba,
                        fillType, hatchAngle, hatchDistance, image, fillSymbol)) {
                    // Serialization failed, skip this bar
                    continue;
                }
            }
            
            // Return serialized string only if we have at least one bar
            return serialized.length() > 0 ? serialized.toString() : null;
        }
    }
    
    /**
     * Begins serialization of startPoint corners.
     * @return a new GpadSerializeStartPoint instance
     */
    public static GpadSerializeStartPoint beginSerializeStartPoint() {
        return new GpadSerializeStartPoint();
    }
    
    /**
     * Begins serialization of barTag bars.
     * @return a new GpadSerializeBarTag instance
     */
    public static GpadSerializeBarTag beginSerializeBarTag() {
        return new GpadSerializeBarTag();
    }
    
    /**
     * Deserializes barTag bars from a serialized string and calls the handler for each bar.
     * Format: [length1 char][bar1 data][length2 char][bar2 data]...
     * Each bar: [length char][barNumber char][flags char][属性值序列]
     *   - length: total length of barNumber + flags + 属性值序列 (excluding length itself)
     *   - barNumber: 1 char (0-65535)
     *   - flags: 1 char, bit flags
     *     bit 0 (0x01): barColor (r, g, b)
     *     bit 1 (0x02): barAlpha
     *     bit 2 (0x04): barFillType
     *     bit 3 (0x08): barHatchAngle
     *     bit 4 (0x10): barHatchDistance
     *     bit 5 (0x20): barImage (字符串，以 ETX 结尾)
     *     bit 6 (0x40): barSymbol
     *   - 属性值序列（按 flags 顺序）
     * 
     * @param serialized the serialized string (from _barTags attribute)
     * @param handler the handler to call for each deserialized bar
     */
    public static void deserializeBarTags(String serialized, BarTagBarHandler handler) {
        if (serialized == null || serialized.isEmpty())
            return;
        
        // Read bars by length prefix
        int pos = 0;
        while (pos < serialized.length()) {
            if (pos + 1 > serialized.length())
                break; // Need at least length char
            
            // Read length
            int barLength = (int)serialized.charAt(pos++);
            if (barLength <= 0 || barLength > 65535)
                break; // Invalid length
            
            if (pos + barLength > serialized.length())
                break; // Not enough data for this bar
            
            // Extract bar data
            String barStr = serialized.substring(pos, pos + barLength);
            pos += barLength;
            
            if (barStr.length() < 2)
                continue; // Invalid format (need at least barNumber and flags)
            
            // (1) Parse barNumber
            char barNumberChar = barStr.charAt(0);
            int barNumberInt = (int)barNumberChar;
            String barNumber = String.valueOf(barNumberInt);
            
            // (2) Parse flags
            char flags = barStr.charAt(1);
            int posB = 2; // Start position for reading attribute values
            
            // Initialize attributes
            int[] rgba = {-1, -1, -1, -1};
            String fillTypeXML = null;
            String hatchAngle = null;
            String hatchDistance = null;
            String image = null;
            String fillSymbol = null;
            
            // (3) Parse attribute values according to flags
            // bit 0: barColor (r, g, b)
            if ((flags & 0x01) != 0) {
                if (posB + 3 > barStr.length())
                    continue; // Invalid format
                rgba[0] = (int)barStr.charAt(posB++);
                rgba[1] = (int)barStr.charAt(posB++);
                rgba[2] = (int)barStr.charAt(posB++);
            }
            
            // bit 1: barAlpha
            if ((flags & 0x02) != 0) {
                if (posB >= barStr.length())
                    continue; // Invalid format
                rgba[3] = (int)barStr.charAt(posB++);
            }
            
            // bit 2: barFillType
            if ((flags & 0x04) != 0) {
                if (posB >= barStr.length())
                    continue; // Invalid format
                int fillType = (int)barStr.charAt(posB++);
                fillTypeXML = String.valueOf(fillType);
            }
            
            // bit 3: barHatchAngle
            if ((flags & 0x08) != 0) {
                if (posB >= barStr.length())
                    continue; // Invalid format
                int angle = (int)barStr.charAt(posB++);
                hatchAngle = String.valueOf(angle);
            }
            
            // bit 4: barHatchDistance
            if ((flags & 0x10) != 0) {
                if (posB >= barStr.length())
                    continue; // Invalid format
                int dist = (int)barStr.charAt(posB++);
                hatchDistance = String.valueOf(dist);
            }
            
            // bit 5: barImage (字符串，以 ETX 结尾)
            if ((flags & 0x20) != 0) {
                int etxPos = barStr.indexOf('\u0003', posB);
                if (etxPos < 0)
                    continue; // Invalid format (no ETX found)
                
                image = barStr.substring(posB, etxPos);
                posB = etxPos + 1;
            }
            
            // bit 6: barSymbol
            if ((flags & 0x40) != 0) {
                if (posB >= barStr.length())
                    continue; // Invalid format
                char symbol = barStr.charAt(posB++);
                fillSymbol = String.valueOf(symbol);
            }
            
            // Call handler
            handler.handle(barNumber, rgba, fillTypeXML, hatchAngle, hatchDistance, image, fillSymbol);
        }
    }
}

