package main.rice.parse;

import main.rice.node.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class responsible for parsing configuration files into ConfigFile objects.
 * Provides methods to read file content, parse configurations, and perform validation.
 */
public class ConfigFileParser{
    /**
     * Reads the content of a file given its filepath.
     *
     * @param filepath the path of the file to be read
     * @return the content of the file as a string
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static String readFile(String filepath) throws IOException {
        Reader reader = new FileReader(filepath); // Initializing FileReader to read the file
        BufferedReader bufferedReader = new BufferedReader(reader); // Buffering the FileReader
        StringBuilder sb = new StringBuilder(); // Creating a StringBuilder to append lines from the file
        String line = "";
        while ((line = bufferedReader.readLine()) != null){ // Reading each line of the file
            sb.append(line); // Appending the line to the StringBuilder
        }
        return sb.toString(); // Returning the content of the file as a string
    }

    /**
     * Parses the contents of a configuration file represented as a string.
     *
     * @param contents the contents of the configuration file
     * @return a ConfigFile object parsed from the provided contents
     * @throws InvalidConfigException if the contents cannot be parsed or contain invalid data
     */
    public static ConfigFile parse(String contents) throws InvalidConfigException{
        try{
            JSONObject jsonObj = new JSONObject(contents); // Creating a JSONObject from the provided contents
            JSONObjKeyValueCheck(jsonObj); // Checking key-value pairs in the JSONObject
            String fname = (String) jsonObj.get("fname"); // Extracting a specific string value from the JSONObject
            JSONArray types = (JSONArray) jsonObj.get("types"); // Extracting a JSONArray from the JSONObject
            JSONArray exDomain = (JSONArray) jsonObj.get("exhaustive domain"); // Extracting another JSONArray
            JSONArray ranDomain = (JSONArray) jsonObj.get("random domain"); // Extracting yet another JSONArray
            int numRandom = (int) jsonObj.get("num random"); // Extracting an integer value from the JSONObject

            // Creating a new ConfigFile object using the extracted data
            return new ConfigFile(fname, returnJSONArrayNodes(types, exDomain, ranDomain), numRandom);
        } catch (JSONException e){
            // Handling exceptions when parsing the contents fails
            throw new InvalidConfigException("Unable to convert the provided String contents to a JSON object");
        }
    }

    /**
     * Checks the key-value pairs within a JSONObject to ensure validity.
     *
     * @param jsonObj the JSONObject to be checked for key-value validity
     * @throws InvalidConfigException if the keys or values are missing, invalid, or mismatched in type
     */
    private static void JSONObjKeyValueCheck(JSONObject jsonObj) throws InvalidConfigException{
        try{
            String fname = (String) jsonObj.get("fname"); // Extracting the value for the "fname" key
            JSONArray types = (JSONArray) jsonObj.get("types"); // Extracting the value for the "types" key
            JSONArray exDomain = (JSONArray) jsonObj.get("exhaustive domain"); // Extracting the value for the "exhaustive domain" key
            JSONArray ranDomain = (JSONArray) jsonObj.get("random domain"); // Extracting the value for the "random domain" key
            int numRandom = (int) jsonObj.get("num random"); // Extracting the value for the "num random" key

            // Checking if numRandom is less than 0, throwing an exception if true
            if(numRandom < 0){
                throw new InvalidConfigException("The num random key is less than 0");
            }

            // Checking if any of the values for the keys are null, throwing an exception if any are null
            if(fname == null || types == null || exDomain == null || ranDomain == null){
                throw new InvalidConfigException("One of the five values for the JSON object is null");
            }
        }
        catch(JSONException e){
            // Handling exceptions if keys are missing or invalid
            throw new InvalidConfigException("One of the keys for the JSON object is missing or invalid");
        }
        catch (ClassCastException e){
            // Handling exceptions if types do not match the expected type
            throw new InvalidConfigException("The type for one of the five key values does not match the expected type");
        }
    }

    /**
     * Converts a string representation of a type to a corresponding PyNode object.
     *
     * @param s   the string representation of the type
     * @param exD the exhaustive domain string
     * @param rnD the random domain string
     * @return an APyNode<?> representing the converted type
     * @throws InvalidConfigException if the conversion encounters invalid or unsupported types
     */
    private static APyNode<?> StringToNodeTypeConvert(String s, String exD, String rnD) throws InvalidConfigException {
        // Stripping leading and trailing spaces from the input strings
        String strippedS = s.strip();
        exD = exD.strip();
        rnD = rnD.strip();

        // Checking if the strippedS contains "(" to determine the type of conversion
        if (!strippedS.contains("(")) {
            // Handling conversion for simple types (int, bool, float)
            switch (strippedS) {
                case "int":
                    // Creating PyIntNode and setting its domains based on input
                    PyIntNode pyIntNode = new PyIntNode();
                    pyIntNode.setExDomain(generateDomain(exD, "int"));
                    pyIntNode.setRanDomain(generateDomain(rnD, "int"));
                    return pyIntNode;
                case "bool":
                    // Creating PyBoolNode and setting its domains based on input
                    PyBoolNode pyBoolNode = new PyBoolNode();
                    pyBoolNode.setExDomain(generateDomain(exD, "bool"));
                    pyBoolNode.setRanDomain(generateDomain(rnD, "bool"));
                    return pyBoolNode;
                case "float":
                    // Creating PyFloatNode and setting its domains based on input
                    PyFloatNode pyFloatNode = new PyFloatNode();
                    pyFloatNode.setExDomain(generateDomain(exD, "float"));
                    pyFloatNode.setRanDomain(generateDomain(rnD, "float"));
                    return pyFloatNode;
                default:
                    // Handling invalid simple types
                    throw new InvalidConfigException("invalid simple type");
            }
        } else {
            // Handling complex types with inner structure (dict, list, set, tuple)
            String outerType = splitChar("(", strippedS)[0].strip();
            String innerType = splitChar("(", strippedS)[1].strip();

            // Handling conversion for string type within the outerType
            if(outerType.equals("str")){
                PyStringNode pyStringNode = new PyStringNode(parseCharDomain(innerType));
                pyStringNode.setExDomain(generateDomain(exD, "string"));
                pyStringNode.setRanDomain(generateDomain(rnD, "string"));
                return pyStringNode;
            }

            // Splitting the outer and inner domains for nested types
            String outerRnD = splitChar("(",rnD)[0].strip();
            String outerExD = splitChar("(", exD)[0].strip();
            String innerRnD = splitChar("(", rnD)[1].strip();
            String innerExD = splitChar("(",exD)[1].strip();

            // Handling conversion for different iterable types (dict, list, set, tuple)
            switch (outerType) {
                case "dict":
                    // Creating PyDictNode and setting its domains based on input
                    PyDictNode<?,?> pyDictNode = new PyDictNode<>(StringToNodeTypeConvert(splitChar(":",innerType)[0].strip(),splitChar(":", innerExD)[0].strip(), splitChar(":", innerRnD)[0].strip()),
                            StringToNodeTypeConvert(splitChar(":",innerType)[1].strip(), splitChar(":",innerExD)[1].strip(), splitChar(":",innerRnD)[1].strip()));
                    pyDictNode.setExDomain(generateDomain(outerExD, "dict"));
                    pyDictNode.setRanDomain(generateDomain(outerRnD, "dict"));
                    return pyDictNode;
                case "list":
                    // Creating PyListNode and setting its domains based on input
                    PyListNode<?> pyListNode = new PyListNode<>(StringToNodeTypeConvert(innerType, innerExD, innerRnD));
                    pyListNode.setExDomain(generateDomain(outerExD, "list"));
                    pyListNode.setRanDomain(generateDomain(outerRnD, "list"));
                    return pyListNode;
                case "set":
                    // Creating PySetNode and setting its domains based on input
                    PySetNode<?> pySetNode = new PySetNode<>(StringToNodeTypeConvert(innerType, innerExD, innerRnD));
                    pySetNode.setExDomain(generateDomain(outerExD, "set"));
                    pySetNode.setRanDomain(generateDomain(outerRnD, "set"));
                    return pySetNode;
                case "tuple":
                    // Creating PyTupleNode and setting its domains based on input
                    PyTupleNode<?> pyTupleNode = new PyTupleNode<>(StringToNodeTypeConvert(innerType, innerExD, innerRnD));
                    pyTupleNode.setExDomain(generateDomain(outerExD, "tuple"));
                    pyTupleNode.setRanDomain(generateDomain(outerRnD, "tuple"));
                    return pyTupleNode;
                default:
                    // Handling invalid iterable types
                    throw new InvalidConfigException("invalid iterable type");
            }
        }
    }

    /**
     * Splits a string based on a specified character and returns an array containing the split parts.
     *
     * @param character the character to split the string
     * @param string    the string to be split
     * @return a String array containing the split parts of the string
     * @throws InvalidConfigException if the character is not found in the string or if the string is improperly formatted
     */
    private static String[] splitChar(String character, String string) throws InvalidConfigException {
        // Checking if the specified character exists in the string
        if (!string.contains(character)) {
            throw new InvalidConfigException("Improperly formatted string: does not contain appropriate character of either ( or :");
        }
        int index = string.indexOf(character); // Finding the index of the specified character
        // Checking if the index + 1 exceeds the length of the string
        if (index + 1 >= string.length()) {
            throw new InvalidConfigException("The string is not formed correctly");
        }
        // Returning an array containing the split parts of the string
        return new String[]{string.substring(0, index), string.substring(index + 1)};
    }

    /**
     * Generates a domain based on the provided string and type.
     *
     * @param string the string containing domain information
     * @param type   the type of domain (e.g., int, float, bool, dict, list, tuple, set, string)
     * @return a List<? extends Number> representing the generated domain
     * @throws InvalidConfigException if the domain generation encounters unsupported or invalid types/values
     */
    private static List<? extends Number> generateDomain(String string, String type) throws InvalidConfigException {

        try {
            // Checking if the string contains '~' indicating a range
            if (string.contains("~")) {
                // Processing range-based domain
                List<Integer> retVal = new ArrayList<>();
                int lowerBound = Integer.parseInt(splitChar("~", string)[0].strip());
                int upperBound = Integer.parseInt(splitChar("~", string)[1].strip());
                // Generating range values and adding them to the list
                for (int i = lowerBound; i <= upperBound; i++) {
                    retVal.add(i);
                }
                // Handling invalid range bounds
                if (lowerBound > upperBound) {
                    throw new InvalidConfigException("Invalid: the lower bound is greater than the upper bound");
                }
                // Checking type-specific constraints for boolean, float, and iterable types
                if (type.equals("bool")) {
                    if (lowerBound < 0 || upperBound > 1) {
                        throw new InvalidConfigException("Unsupported values for boolean type: all values in the domain must be 0 or 1");
                    }
                }
                // Handling conversion to double for float type
                if (type.equals("float")) {
                    List<Double> doubleList = retVal.stream().map(Integer::doubleValue).toList();
                    return doubleList;
                }
                // Handling iterable type constraints for dict, tuple, set, list, and string
                if (type.equals("dict") || type.equals("tuple") || type.equals("set") || type.equals("list") || type.equals("string")) {
                    if (lowerBound < 0) {
                        throw new InvalidConfigException("The lower bound is less than 0; unsupported operation for iterable types");
                    }
                }
                return retVal;
            } else {
                // Processing list-based domain
                List<Number> retVal = new ArrayList<>();
                // Checking if the string represents a valid list format
                if (string.endsWith("]") && string.substring(0, 1).equals("[")) {
                    String retValSubstring = string.substring(1, string.length() - 1);
                    String[] stringVersion = retValSubstring.split(",");
                    // Handling type-specific conversions and constraints for float, bool, int, and iterable types
                    if (type.equals("float")) {
                        for (String doubleString : stringVersion) {
                            retVal.add(Double.parseDouble(doubleString.strip()));
                        }
                    }
                    if (type.equals("bool")) {
                        for (String boolString : stringVersion) {
                            int boolVal = Integer.parseInt(boolString.strip());
                            if (boolVal != 0 && boolVal != 1) {
                                throw new InvalidConfigException("Given the type is boolean, there are values outside of only 0 and 1; this is an invalid array");
                            }
                            retVal.add(boolVal);
                        }
                    }
                    if (type.equals("int")) {
                        for (String boolString : stringVersion) {
                            Integer intVal = Integer.parseInt(boolString.strip());
                            retVal.add(intVal);
                        }
                    }
                    if (type.equals("dict") || type.equals("tuple") || type.equals("set") || type.equals("list") || type.equals("string")) {
                        for (String boolString : stringVersion) {
                            int intVal = Integer.parseInt(boolString.strip());
                            if (intVal < 0) {
                                throw new InvalidConfigException("Negative values for the domain; invalid for iterable types");
                            }
                            retVal.add(intVal);
                        }
                    }
                    return retVal;
                } else {
                    throw new InvalidConfigException("Malformed list of Number values");
                }
            }
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("Unable to parse the value");
        }
    }

    /**
     * Generates a list of APyNode objects based on provided JSONArrays.
     *
     * @param types     JSONArray containing types information
     * @param exDomain  JSONArray containing exhaustive domain information
     * @param ranDomain JSONArray containing random domain information
     * @return a List of APyNode objects
     * @throws InvalidConfigException if parsing or conversion encounters an issue
     */
    private static List<APyNode<?>> returnJSONArrayNodes(JSONArray types, JSONArray exDomain, JSONArray ranDomain) throws InvalidConfigException {
        int sizeOfJSONArray = types.length(); // Determining the size of the JSONArray
        List<APyNode<?>> retVal = new ArrayList<>(); // Initializing the return list
        try {
            // Iterating through the JSONArrays and converting each element to APyNode
            for (int i = 0; i < sizeOfJSONArray; i++) {
                retVal.add(StringToNodeTypeConvert((String) types.get(i), (String) exDomain.get(i), (String) ranDomain.get(i)));
            }
        } catch (ClassCastException e) {
            throw new InvalidConfigException("Unable to parse to String type");
        }
        return retVal; // Returning the list of APyNode objects
    }

    /**
     * Parses a character domain represented as a string into a Set of characters.
     *
     * @param charDomain the string representation of the character domain
     * @return a Set of characters representing the parsed character domain
     */
    private static Set<Character> parseCharDomain(String charDomain) {
        Set<Character> retVal = new HashSet<>(); // Initializing the set for characters
        // Iterating through the string representation and adding each character to the set
        for (int i = 0; i < charDomain.length(); i++) {
            retVal.add(charDomain.charAt(i));
        }
        return retVal; // Returning the set of parsed characters
    }
}