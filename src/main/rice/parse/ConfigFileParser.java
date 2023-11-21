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

public class ConfigFileParser{
    public static String readFile(String filepath) throws IOException {
        Reader reader = new FileReader(filepath);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null){
            sb.append(line);
        }
        return sb.toString();
    }

    public static ConfigFile parse(String contents) throws InvalidConfigException{
        try{
            JSONObject jsonObj = new JSONObject(contents);
        JSONObjKeyValueCheck(jsonObj);
        String fname = (String) jsonObj.get("fname");
        JSONArray types = (JSONArray) jsonObj.get("types");
        JSONArray exDomain = (JSONArray) jsonObj.get("exhaustive domain");
        JSONArray ranDomain = (JSONArray) jsonObj.get("random domain");
        int numRandom = (int) jsonObj.get("num random");
        return new ConfigFile(fname, returnJSONArrayNodes(types, exDomain, ranDomain), numRandom);
        } catch (JSONException e){
            throw new InvalidConfigException("Unable to conver the provided String contents to a JSON object");
        }
    }

    private static void JSONObjKeyValueCheck(JSONObject jsonObj) throws InvalidConfigException{
        try{
            String fname = (String) jsonObj.get("fname");
            JSONArray types = (JSONArray) jsonObj.get("types");
            JSONArray exDomain = (JSONArray) jsonObj.get("exhaustive domain");
            JSONArray ranDomain = (JSONArray) jsonObj.get("random domain");
            int numRandom = (int) jsonObj.get("num random");
            if(numRandom < 0){
                throw new InvalidConfigException("The num random key is less than 0");
            }
            if(fname == null || types == null || exDomain == null || ranDomain == null){
                throw new InvalidConfigException("One of the five values for the JSON object is null");
            }
        }
        catch(JSONException e){
            throw new InvalidConfigException("One of the keys for the JSON object is missing or invalid");
        }
        catch (ClassCastException e){
            throw new InvalidConfigException("The type for one of the five key values does not match the expected type");
        }
    }

    private static APyNode<?> StringToNodeTypeConvert(String s, String exD, String rnD) throws InvalidConfigException {
        String strippedS = s.strip();
        exD = exD.strip();
        rnD = rnD.strip();
        if (!strippedS.contains("(")) {
            switch (strippedS) {
                case "int":
                    PyIntNode pyIntNode = new PyIntNode();
                    pyIntNode.setExDomain(generateDomain(exD, "int"));
                    pyIntNode.setRanDomain(generateDomain(rnD, "int"));
                    return pyIntNode;
                case "bool":
                    PyBoolNode pyBoolNode = new PyBoolNode();
                    pyBoolNode.setExDomain(generateDomain(exD, "bool"));
                    pyBoolNode.setRanDomain(generateDomain(rnD,"bool"));
                    return pyBoolNode;
                case "float":
                    PyFloatNode pyFloatNode = new PyFloatNode();
                    pyFloatNode.setExDomain(generateDomain(exD, "float"));
                    pyFloatNode.setRanDomain(generateDomain(rnD, "float"));
                    return pyFloatNode;
                default:
                    throw new InvalidConfigException("invalid simple type");
            }
        } else {
            String outerType = splitChar("(", strippedS)[0].strip();
            String innerType = splitChar("(", strippedS)[1].strip();
            if(outerType.equals("str")){
                PyStringNode pyStringNode = new PyStringNode(parseCharDomain(innerType));
                pyStringNode.setExDomain(generateDomain(exD, "string"));
                pyStringNode.setRanDomain(generateDomain(rnD, "string"));
                return pyStringNode;
            }
            String outerRnD = splitChar("(",rnD)[0].strip();
            String outerExD = splitChar("(", exD)[0].strip();
            String innerRnD = splitChar("(", rnD)[1].strip();
            String innerExD = splitChar("(",exD)[1].strip();

            switch (outerType) {
                case "dict":
                   PyDictNode<?,?> pyDictNode = new PyDictNode<>(StringToNodeTypeConvert(splitChar(":",innerType)[0].strip(),splitChar(":", innerExD)[0].strip(), splitChar(":", innerRnD)[0].strip()),
                            StringToNodeTypeConvert(splitChar(":",innerType)[1].strip(), splitChar(":",innerExD)[1].strip(), splitChar(":",innerRnD)[1].strip()));
                   pyDictNode.setExDomain(generateDomain(outerExD, "dict"));
                   pyDictNode.setRanDomain(generateDomain(outerRnD, "dict"));
                   return pyDictNode;
                case "list":
                    PyListNode<?> pyListNode = new PyListNode<>(StringToNodeTypeConvert(innerType, innerExD, innerRnD));
                    pyListNode.setExDomain(generateDomain(outerExD, "list"));
                    pyListNode.setRanDomain(generateDomain(outerRnD, "list"));
                    return pyListNode;
                case "set":
                    PySetNode<?> pySetNode = new PySetNode<>(StringToNodeTypeConvert(innerType, innerExD, innerRnD));
                    pySetNode.setExDomain(generateDomain(outerExD, "set"));
                    pySetNode.setRanDomain(generateDomain(outerRnD, "set"));
                    return pySetNode;
                case "tuple":
                    PyTupleNode<?> pyTupleNode = new PyTupleNode<>(StringToNodeTypeConvert(innerType, innerExD, innerRnD));
                    pyTupleNode.setExDomain(generateDomain(outerExD, "tuple"));
                    pyTupleNode.setRanDomain(generateDomain(outerRnD, "tuple"));
                    return pyTupleNode;
                default:
                    throw new InvalidConfigException("invalid iterable type");
            }
        }
    }

    private static String[] splitChar(String character, String string) throws InvalidConfigException{
        if(!string.contains(character)){
            throw new InvalidConfigException("Improperly formatted string: does not contain appropriate character of either ( or :");
        }
        int index = string.indexOf(character);
        if(index+1 >= string.length()){
            throw new InvalidConfigException("The string is not formed correctly");
        }
        return new String[]{string.substring(0,index), string.substring(index+1)};
    }

    private static List<? extends Number> generateDomain(String string, String type) throws InvalidConfigException{

        try{if(string.contains("~")){
            List<Integer> retVal = new ArrayList<>();
            int lowerBound = Integer.parseInt(splitChar("~",string)[0].strip());
            int upperBound = Integer.parseInt(splitChar("~",string)[1].strip());
            for (int i = lowerBound; i <= upperBound; i++){
                retVal.add(i);
            }
            if(lowerBound > upperBound){
                throw new InvalidConfigException("Invalid: the lower bound is greater than the upper bound");
            }
            if(type.equals("bool")){
                //make sure they are all 0s and 1s
                if(lowerBound < 0 || upperBound > 1){
                    throw new InvalidConfigException("Unsupported values for boolean type: all values in the domain must be 0 or 1");
                }
            }
            if(type.equals("float")){
                List<Double> doubleList = retVal.stream().map(Integer::doubleValue).toList();
                return doubleList;
            }
            if(type.equals("dict") || type.equals("tuple") || type.equals("set") || type.equals("list") || type.equals("string")){
                if(lowerBound < 0){
                    throw new InvalidConfigException("The lower bound is less than 0; unsupported operation for iterable types");
                }
            }
            return retVal;
        }
        else{
            List<Number> retVal = new ArrayList<>();
            if(string.endsWith("]") && string.substring(0,1).equals("[")){
                String retValSubstring = string.substring(1,string.length()-1);
                String[] stringVersion = retValSubstring.split(",");
                if(type.equals("float")){
                    for(String doubleString: stringVersion){
                        retVal.add(Double.parseDouble(doubleString.strip()));
                    }
                }
                if(type.equals("bool")){
                    for(String boolString: stringVersion){
                        int boolVal = Integer.parseInt(boolString.strip());
                        if(boolVal != 0 && boolVal != 1){
                            throw new InvalidConfigException("Given the type is boolean, there are values outside of only 0 and 1; this is an invalid array");
                        }
                        retVal.add(boolVal);
                    }
                }
                if(type.equals("int")){
                    for(String boolString: stringVersion){
                        Integer intVal = Integer.parseInt(boolString.strip());
                        retVal.add(intVal);
                    }
                }
                if(type.equals("dict") || type.equals("tuple") || type.equals("set") || type.equals("list") || type.equals("string")){
                    for(String boolString: stringVersion){
                        int intVal = Integer.parseInt(boolString.strip());
                        if(intVal < 0){
                            throw new InvalidConfigException("Negative values for the domain; invalid for iterable types");
                        }
                        retVal.add(intVal);
                    }
                }
                return retVal;
            }
            else{
                throw new InvalidConfigException("Malformed list of Number values");
            }
        }}catch (NumberFormatException e){
            throw new InvalidConfigException("Unable to parse the value");
        }
    }

    private static List<APyNode<?>> returnJSONArrayNodes(JSONArray types, JSONArray exDomain, JSONArray ranDomain) throws InvalidConfigException{
        int sizeOfJSONArray = types.length();
        List<APyNode<?>> retVal = new ArrayList<>();
        try{
        for (int i = 0; i < sizeOfJSONArray; i ++){
            retVal.add(StringToNodeTypeConvert((String)types.get(i), (String)exDomain.get(i), (String)ranDomain.get(i)));
        }}
        catch(ClassCastException e){
            throw new InvalidConfigException("Unable to parse to String type");
        }
        return retVal;

    }

    private static Set<Character> parseCharDomain(String charDomain){
        Set<Character> retVal = new HashSet<>();
        for(int i = 0; i < charDomain.length(); i ++){
            retVal.add(charDomain.charAt(i));
        }
        return retVal;
    }
}