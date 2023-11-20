package main.rice;

import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.parse.ConfigFile;
import main.rice.parse.InvalidConfigException;
import main.rice.test.TestCase;
import main.rice.test.TestResults;
import main.rice.test.Tester;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

// TODO: implement the Main class here
public class Main{
    public static void main(String[] args) throws IOException, InvalidConfigException, InterruptedException{
        Set<TestCase> toPrint = generateTests(args);
        System.out.println("Prints a set of test cases that is an approximately minimal set covering");
        for (TestCase testCase: toPrint){
            System.out.println(testCase.toString());
        }
    }
    public static Set<TestCase> generateTests(String[] args) throws IOException, InvalidConfigException, InterruptedException{
        ConfigFile configFile = ConfigFileParser(args[0]);
        BaseSetGenerator baseSetGenerator = new BaseSetGenerator(configFile.getNodes(), configFile.getNumRand());
        Tester tester = new Tester(configFile.getFuncName(), args[1], args[2],baseSetGenerator.genBaseSet());
        tester.computeExpectedResults();
        TestResults testResults = tester.runTests();
        return ConciseSetGenerator.setCover(testResults);
    }
}