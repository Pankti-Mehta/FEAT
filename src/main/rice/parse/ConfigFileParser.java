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
import java.util.List;

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

    private void JSONObjKeyValueCheck(JSONObject jsonObj) throws InvalidConfigException{
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

    private APyNode<?> StringToNodeTypeConvert(String s, String exD, String rnD) throws InvalidConfigException {
        String strippedS = s.strip();
        exD = exD.strip();
        rnD = rnD.strip();
        if (!strippedS.contains("(")) {
            switch (strippedS) {
                case "int":
                    return new PyIntNode();
                case "bool":
                    return new PyBoolNode();
                case "float":
                    return new PyFloatNode();
                default:
                    throw new InvalidConfigException("invalid simple type");
            }
        } else {
            String outerType = splitChar("(", strippedS)[0].strip();
            String innerType = splitChar("(", strippedS)[1];
            switch (outerType) {
                case "dict":
                    return new PyDictNode<>(StringToNodeTypeConvert(splitChar(":",innerType)[0]),
                            StringToNodeTypeConvert(splitChar(":",innerType)[1]));
                case "list":
                    return new PyListNode<>(StringToNodeTypeConvert(innerType));
                case "set":
                    return new PySetNode<>(StringToNodeTypeConvert(innerType));
                case "tuple":
                    return new PyTupleNode<>(StringToNodeTypeConvert(innerType));
                default:
                    throw new InvalidConfigException("invalid iterable type");
            }
        }
    }

    private static String[] splitChar(String character, String string) throws InvalidConfigException{
        int index = string.indexOf(character);
        if(index+1 < string.length()){
            throw new InvalidConfigException("The string is not formed correctly");
        }
        return new String[]{string.substring(0,index), string.substring(index+1)};
    }

    private static List<? extends Number> generateDomain(String string, String type) throws InvalidConfigException{

        if(string.contains("∼")){
            List<Integer> retVal = new ArrayList<>();
            int lowerBound = Integer.parseInt(splitChar("∼",string)[0]);
            int upperBound = Integer.parseInt(splitChar("∼",string)[1]);
            for (int i = lowerBound; i <= upperBound; i++){
                retVal.add(i);
            }
            if(lowerBound > upperBound){
                throw new InvalidConfigException("Invalid: the lower bound is greater than the upper bound");
            }
            if(type.equals("bool")){
                //make sure they are all 0s and 1s
                if(lowerBound < 0 || upperBound > 1){
                    throw new InvalidConfigException()
                }
            }
            return retVal;
        }
        else{
            List<Double> retVal = new ArrayList<>();
            if(string.endsWith("]") && string.substring(0,1).equals("[")){
                String retValSubstring = string.substring(1,string.length()-1);
                String[] stringVersion = retValSubstring.split(",");
                for(String doubleString: stringVersion){
                    retVal.add(Double.parseDouble(doubleString));
                }
                return retVal;
            }
            else{
                throw new InvalidConfigException("Malformed list of double values");
            }
        }
    }
}