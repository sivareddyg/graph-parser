package in.sivareddy.graphparser.cli;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.Schema;

import org.apache.log4j.*;

import others.EasyCcgCli;
import others.StanfordCoreNlpDemo;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CcgParseToUngroundedGraphs {

  public static void main(String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    int nbestParses = 1;
    String ccgModelDir = "lib_data/easyccg_model/ -r S[dcl] S[pss] S[b]";
    EasyCcgCli ccgParser = new EasyCcgCli(ccgModelDir, nbestParses);

    StanfordCoreNlpDemo nlpPipeline = new StanfordCoreNlpDemo("en");

    CcgAutoLexicon questionCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases_questions.txt");

    CcgAutoLexicon normalCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases.txt");

    String[] relationLexicalIdentifiers = {"word"};
    String[] relationTypingIdentifiers = {};

    Schema schema = null;
    KnowledgeBase kb = new KnowledgeBase(null, null);
    GroundedLexicon groundedLexicon = new GroundedLexicon(null);
    GroundedGraphs graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, null, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, 10.0, 1.0, 0.0,
            0.0);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(graphCreator.getClass());
    logger.setLevel(Level.DEBUG);
    logger.setAdditivity(false);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    try {
      String line = br.readLine();
      // String line = "{\"sentence\" : \"Obama 's birthplace is Kenya .\"}";
      while (line != null) {
        if (line.trim().equals("") || line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }

        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        if (jsonSentence.has("sentence"))
          logger.debug("Input Sentence: "
              + jsonSentence.get("sentence").getAsString());
        String sentence = jsonSentence.get("sentence").getAsString();
        List<String> processedText = nlpPipeline.processText(sentence);

        for (String processedSentence : processedText) {
          List<String> ccgParseStrings = ccgParser.parse(processedSentence);
          List<Map<String, String>> ccgParses = new ArrayList<>();
          for (String ccgParseString : ccgParseStrings) {
            Map<String, String> ccgParseMap = new HashMap<>();
            ccgParseMap.put("synPar", ccgParseString);
            ccgParseMap.put("score", "1.0");
            ccgParses.add(ccgParseMap);
          }
          jsonSentence.add("synPars", jsonParser.parse(gson.toJson(ccgParses)));

          String[] wordsString = processedSentence.split("\\s");
          List<Map<String, String>> words = Lists.newArrayList();
          for (String word : wordsString) {
            String[] parts = word.split("\\|");
            Map<String, String> wordMap = new HashMap<>();
            wordMap.put("word", parts[0]);
            wordMap.put("pos", parts[1]);
            wordMap.put("ner", parts[2]);
            words.add(wordMap);
          }

          jsonSentence.add("words", jsonParser.parse(gson.toJson(words)));

          List<LexicalGraph> graphs =
              graphCreator.buildUngroundedGraph(jsonSentence, "synPars",
                  nbestParses, logger);

          logger.debug("# Ungrounded Graphs");
          if (graphs.size() > 0) {
            for (LexicalGraph ungroundedGraph : graphs) {
              logger.debug(ungroundedGraph);
              /*-List<LexicalGraph> groundedGraphs = graphCreator
              		.createGroundedGraph(ungroundedGraph, 10, 100,
              				true, true, true, true, true, false);
              System.out
              		.println("# Total number of Grounded Graphs: "
              				+ groundedGraphs.size());

              int connectedGraphCount = 0;
              for (LexicalGraph groundedGraph : groundedGraphs) {
              	// if (groundedGraph.isConnected()) {
              	connectedGraphCount += 1;
              	System.out.println("# Grounded graph: "
              			+ connectedGraphCount);
              	System.out.println(groundedGraph);
              	System.out.println("Graph Query: "
              			+ GraphToSparqlConverter
              					.convertGroundedGraph(
              							groundedGraph, schema));
              	// }
              }

              System.out
              		.println("# Total number of Grounded Graphs: "
              				+ groundedGraphs.size());
              System.out
              		.println("# Total number of Connected Grounded Graphs: "
              				+ connectedGraphCount);
              System.out.println();*/
            }
          }
          line = br.readLine();
        }
      }
    } finally {
      br.close();
    }
  }
}
