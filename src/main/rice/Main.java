package main.rice;

import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.parse.ConfigFile;
import main.rice.parse.ConfigFileParser;
import main.rice.parse.InvalidConfigException;
import main.rice.test.TestCase;
import main.rice.test.TestResults;
import main.rice.test.Tester;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

// TODO: implement the Main class here
/**
 * Main class responsible for generating and printing a set of test cases.
 * It reads a configuration file, generates test cases based on the configuration,
 * and prints a minimal set covering the test cases.
 */
public class Main {
    /**
     * Entry point of the application.
     *
     * @param args Command-line arguments containing file paths and parameters
     * @throws IOException            if an I/O error occurs while reading files
     * @throws InvalidConfigException if the configuration file is invalid
     * @throws InterruptedException  if the execution is interrupted
     */
    public static void main(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        Set<TestCase> toPrint = generateTests(args);
        System.out.println("Prints a set of test cases that is an approximately minimal set covering");
        for (TestCase testCase : toPrint) {
            System.out.println(testCase.toString());
        }
    }

    /**
     * Generates a set of test cases based on the provided arguments.
     *
     * @param args Command-line arguments containing file paths and parameters
     * @return a set of test cases
     * @throws IOException            if an I/O error occurs while reading files
     * @throws InvalidConfigException if the configuration file is invalid
     * @throws InterruptedException  if the execution is interrupted
     */
    public static Set<TestCase> generateTests(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        ConfigFile configFile = ConfigFileParser.parse(ConfigFileParser.readFile(args[0]));
        BaseSetGenerator baseSetGenerator = new BaseSetGenerator(configFile.getNodes(), configFile.getNumRand());
        Tester tester = new Tester(configFile.getFuncName(), args[1], args[2], baseSetGenerator.genBaseSet());
        tester.computeExpectedResults();
        TestResults testResults = tester.runTests();
        return ConciseSetGenerator.setCover(testResults);
    }
}