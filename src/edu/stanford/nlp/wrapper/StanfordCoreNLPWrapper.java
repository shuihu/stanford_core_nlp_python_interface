package edu.stanford.nlp.wrapper;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import edu.stanford.nlp.util.logging.Redwood;
import edu.stanford.nlp.util.logging.StanfordRedwoodConfiguration;

import java.io.*;
import java.lang.IllegalArgumentException;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class StanfordCoreNLPWrapper {
  private static List<String> readFileList(String filename) throws IOException {
	List<String> lines = new LinkedList<String>();
	FileInputStream stream = new FileInputStream(new File(filename));
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    
    String line = null;
    while ((line = reader.readLine()) != null) {
      String toAdd = line;
      lines.add(toAdd);
    }
    return lines;
  }	
  
  private static List<String> concat(String dirName, List<String> filepaths) {
	List<String> concats = new LinkedList<String>();
	for (String filepath : filepaths) {
      concats.add(new File(dirName, new File(filepath).getName()).getPath());
	}
	return concats;
  }
  
  private static List<Runnable> createProcessFileRunnables(final StanfordCoreNLP stanfordCoreNLP, final List<String> inputFilenames, final List<String> outputFilenames) {
    List<Runnable> runnables = new LinkedList<Runnable>();
	int numFiles = inputFilenames.size();
	List<Integer> range = new LinkedList<Integer>();
	for (int i = 0; i < numFiles; i++) {
      range.add(i);
	}
	for (final int i : range) {
      runnables.add(
        new Runnable() {
          public void run() {
        	try {
        	  processFile(stanfordCoreNLP, inputFilenames.get(i), outputFilenames.get(i));
        	} catch(Exception e) {
              System.out.println("Failure when processing " + inputFilenames.get(i) + " into " + outputFilenames.get(i) + ":");
              System.out.println(e.toString());
        	}
          }
        }
      );
	}
	return runnables;
  }
  
  private static void writeJsonArray(JSONArray jsonArray, String filename) {
	try {
	  PrintWriter writer = new PrintWriter(filename);
	  writer.println(jsonArray.toJSONString());
      writer.close();
	} catch(Exception e) {
      System.out.println("Writing to " + filename + " failed:");
	  System.out.println(e.toString());
    }  
  }
  
  private static JSONArray extractTokensAsJson(Annotation annotation) {
	JSONArray allTokens = new JSONArray();
    for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
      JSONArray tokens = new JSONArray();
      for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
        tokens.add(token.word());
      }
      allTokens.add(tokens);
    }
    return allTokens;
  }
  
  private static JSONArray extractDepParsesAsJson(Annotation annotation) {
	JSONArray allDepTrees = new JSONArray();
	for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
	  JSONArray depTree = new JSONArray();	
	  for (SemanticGraphEdge edge : sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).edgeListSorted()) {
		List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
		  
		JSONObject dependency = new JSONObject();
		String relation = edge.getRelation().toString().replaceAll("\\s+", "");
		dependency.put("relation", relation);
		
		JSONObject head = new JSONObject();
		int headIndex = edge.getSource().index();
		head.put("index", headIndex);
		head.put("token", tokens.get(headIndex - 1).word());
		dependency.put("head", head);
		
		JSONObject dependent = new JSONObject();
		int dependentIndex = edge.getTarget().index();
		dependent.put("index", dependentIndex);
		dependent.put("token", tokens.get(dependentIndex - 1).word());
		dependency.put("dependent", dependent);
		
		depTree.add(dependency);
	  }
	  allDepTrees.add(depTree);
	}
	return allDepTrees;
  }
  
  private static void processFile(StanfordCoreNLP stanfordCoreNLP, String inputFilename, String outputFilename) throws IOException, ParseException {
	JSONParser parser = new JSONParser();
	String emailJsonsString = readFileList(inputFilename).get(0);
	JSONArray emailJsonArray = (JSONArray) (parser.parse(emailJsonsString));
	int emailIndex = 0;
	for (Object emailObject : emailJsonArray) {
      try {
    	JSONObject emailJson = (JSONObject) emailObject;
        String rawEmailText = (String) emailJson.get("body");
        if (rawEmailText != null) {
    	  String unescapedEmailText = rawEmailText.replaceAll("\\n", "\n").replaceAll("\\\"", "\"");
    	  Annotation annotation = stanfordCoreNLP.process(unescapedEmailText);
    	  emailJson.put("tokenized_body", extractTokensAsJson(annotation));
    	  emailJson.put("dependency_parsed_body", extractDepParsesAsJson(annotation));
    	}
      } catch(Exception e) {
        System.out.println("Failure when processing email " + emailIndex + " of " + inputFilename + ":");
        System.out.println(e.toString());
      }
      emailIndex++;
	}
	writeJsonArray(emailJsonArray, outputFilename);
  }
  
  public static void tokenizeAndDepParse(String inputName, String outputName, int numThreads) {
	StanfordRedwoodConfiguration.minimalSetup();
	String[] args = new String[]{StanfordCoreNLP.STANFORD_TOKENIZE, StanfordCoreNLP.STANFORD_SSPLIT, StanfordCoreNLP.STANFORD_PARSE};
	Properties props = StringUtils.argsToProperties(args);
    StanfordCoreNLP stanfordCoreNLP = new StanfordCoreNLP(props);
    
    try {
      if (inputName.endsWith(".json")) {
        processFile(stanfordCoreNLP, inputName, outputName);
      } else if (inputName.endsWith(".txt")) {
        // Get input and output file names
        List<String> inputFilenames = readFileList(inputName);
        List<String> outputFilenames = concat(outputName, inputFilenames);
        // Create jobs (for multi-threading)
        List<Runnable> toRun = createProcessFileRunnables(stanfordCoreNLP, inputFilenames, outputFilenames);
      
        // Run jobs
        if(numThreads == 1){
          for(Runnable r : toRun){ r.run(); }
        } else {
          Redwood.Util.threadAndRun("StanfordCoreNLPWrapper <" + numThreads + " threads>", toRun, numThreads);
        }
      } else {
        System.out.println("The first argument for tokenizeAndDepParse must be either a .json or a .txt file");
      }
    } catch(Exception e) {
      System.out.println("tokenizeAndDepParse failed while processing " + inputName + " into " + outputName);
    }
    
    System.out.println("StanfordCoreNLPWrapper.tokenizeAndDepParse(" + inputName + ", " + outputName + ") complete");
  }
  
  public static void main(String[] args) throws IllegalArgumentException {
    if (args.length != 3) {
      throw new IllegalArgumentException("StanfordCoreNLPWrapper takes three arguments");
    }
    tokenizeAndDepParse(args[0], args[1], Integer.parseInt(args[2]));
    System.out.println("StanfordCoreNLPWrapper complete");
  }
}