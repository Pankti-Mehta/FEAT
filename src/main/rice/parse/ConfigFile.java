package main.rice.parse;

import main.rice.node.APyNode;

import java.util.List;

// TODO: implement the ConfigFile class here
public class ConfigFile{
    private String funcName;
    private List<APyNode<?>> nodes;
    private int numRand;
    public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand){
        this.funcName = funcName;
        this.nodes = nodes;
        this.numRand = numRand;
    }

    public String getFuncName(){
        return this.funcName;
    }

    public List<APyNode<?>> getNodes(){
        return this.nodes;
    }
    public int getNumRand(){
        return this.numRand;
    }

}
