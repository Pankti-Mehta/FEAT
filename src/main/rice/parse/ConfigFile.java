package main.rice.parse;

import main.rice.node.APyNode;
import java.util.List;

// TODO: implement the ConfigFile class here
public class ConfigFile {
    private String funcName; // Represents the name of the function
    private List<APyNode<?>> nodes; // Represents a list of APyNodes
    private int numRand; // Represents the number of random values

    // Constructor for the ConfigFile class
    public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand) {
        this.funcName = funcName;
        this.nodes = nodes;
        this.numRand = numRand;
    }

    // Method to get the function name
    public String getFuncName() {
        return this.funcName;
    }

    // Method to get the list of nodes
    public List<APyNode<?>> getNodes() {
        return this.nodes;
    }

    // Method to get the number of random values
    public int getNumRand() {
        return this.numRand;
    }
}