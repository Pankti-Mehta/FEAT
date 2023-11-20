package main.rice.parse;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class ConfigFileParser{
    public static String readFile(String filepath) throws IOException {
        Reader reader = new FileReader();
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line = "";
        while ((line = bufferedReader.readLine()) != null){
            sb.append(line);
        }
        return sb.toString();
    }

    private void JSONObjKeyValueCheck(JSONObject jsonObj) throws InvalidConfigException{
        try{jsonObj.get("fname");
        jsonObj.get("types");
        jsonObj.get("exhaustive domain");
        jsonObj.get("random domain");
        jsonObj.get("num random")}
        catch()
    }
}