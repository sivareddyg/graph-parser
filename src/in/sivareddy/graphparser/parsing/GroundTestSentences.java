package in.sivareddy.graphparser.parsing;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.ml.learning.StructuredPercepton;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroundTestSentences {

  public static void run(Schema schema, KnowledgeBase kb,
      GroundedLexicon groundedLexicon, RdfGraphTools rdfGraphTools)
      throws IOException {
    CcgAutoLexicon normalCcgAutoLexicon =
        new CcgAutoLexicon("./data/candc_markedup.modified",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases.txt");
    CcgAutoLexicon questionCcgAutoLexicon =
        new CcgAutoLexicon("./data/candc_markedup.modified",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases_questions.txt");

    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};

    // System.out.println("Types: " + schema.getTypes().size() +
    // schema.getTypes());

    boolean urelGrelFlag = true;
    boolean urelPartGrelPartFlag = true;
    boolean utypeGtypeFlag = true;
    boolean gtypeGrelFlag = true;
    boolean grelGrelFlag = false;
    boolean wordGrelPartFlag = true;
    boolean wordGrelFlag = true;
    boolean argGrelPartFlag = true;
    boolean argGrelFlag = true;
    boolean wordBigramGrelPartFlag = true;
    boolean stemMatchingFlag = true;
    boolean mediatorStemGrelPartMatchingFlag = true;
    boolean argumentStemMatchingFlag = true;
    boolean argumentStemGrelPartMatchingFlag = true;

    boolean graphIsConnectedFlag = false;
    boolean graphHasEdgeFlag = false;
    boolean countNodesFlag = false;
    boolean edgeNodeCountFlag = true;
    boolean duplicateEdgesFlag = true;

    boolean useLexiconWeightsRel = true;
    boolean useLexiconWeightsType = false;

    double initialEdgeWeight = -1.0;
    double initialTypeWeight = -1.0;
    double initialWordWeight = -1.0;
    double stemFeaturesWeight = 0.0;

    StructuredPercepton learningModel = new StructuredPercepton();

    // GroundedLexicon groundedLexicon = null;
    GroundedGraphs graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, learningModel, urelGrelFlag,
            urelPartGrelPartFlag, utypeGtypeFlag, gtypeGrelFlag, grelGrelFlag,
            wordGrelPartFlag, wordGrelFlag, argGrelPartFlag, argGrelFlag,
            wordBigramGrelPartFlag, stemMatchingFlag,
            mediatorStemGrelPartMatchingFlag, argumentStemMatchingFlag,
            argumentStemGrelPartMatchingFlag, graphIsConnectedFlag,
            graphHasEdgeFlag, countNodesFlag, edgeNodeCountFlag,
            useLexiconWeightsRel, useLexiconWeightsType, duplicateEdgesFlag,
            initialEdgeWeight, initialTypeWeight, initialWordWeight,
            stemFeaturesWeight);
    JsonParser jsonParser = new JsonParser();
    // BufferedReader br = new BufferedReader(new
    // FileReader("data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_parse.txt"));
    // BufferedReader br = new BufferedReader(new
    // FileReader("data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_parse.txt"));

    for (int iteration = 0; iteration < 10; iteration++) {
      BufferedReader br =
          new BufferedReader(
              new FileReader(
                  "data/webquestions/webquestions.examples.test.domains.parse.filtered.json"));
      ConcurrentHashMap<Integer, Integer> positives = new ConcurrentHashMap<>();
      ConcurrentHashMap<Integer, Integer> negatives = new ConcurrentHashMap<>();
      List<Integer> maxbestList =
          Lists.newArrayList(1, 5, 10, 50, 100, 500, 1000, 2000, 3000, 5000);
      int sentcount = 0;

      try {
        String line = br.readLine();

        for (int i = 0; i < 0; i++) {
          sentcount += 1;
          line = br.readLine();
        }

        while (line != null) {
          if (line.equals("") || line.charAt(0) == '#') {
            line = br.readLine();
            continue;
          }
          sentcount += 1;

          if (sentcount == 100) {
            break;
          }

          System.out.println(sentcount + ": " + line);
          JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();

          String sentence = jsonSentence.get("sentence").getAsString();
          System.out.println("Sentence: " + sentence);
          boolean hasGoldQuery =
              jsonSentence.has("sparqlQuery")
                  || jsonSentence.has("targetValue");
          System.out.println("Supervised Example");
          if (!hasGoldQuery) {
            return;
          }

          Map<String, LinkedHashSet<String>> goldResults = null;
          String goldQuery = null;
          if (jsonSentence.has("sparqlQuery")) {
            goldQuery = jsonSentence.get("sparqlQuery").getAsString();
            System.out.println("Gold Query : " + goldQuery);
            goldResults = rdfGraphTools.runQueryHttp(goldQuery);
          } else if (jsonSentence.has("targetValue")) {
            String goldAnswersString =
                jsonSentence.get("targetValue").getAsString();
            Pattern goldAnswerPattern =
                Pattern.compile("\\(description \"?([^\\)\"]+)\"?\\)");
            Matcher matcher = goldAnswerPattern.matcher(goldAnswersString);
            LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
            while (matcher.find()) {
              goldAnswers.add(matcher.group(1));
            }
            goldResults = new HashMap<>();
            goldResults.put("targetValue", goldAnswers);
          }
          System.out.println("Gold Results : " + goldResults);

          List<LexicalGraph> graphs =
              graphCreator.buildUngroundedGraph(jsonSentence, "synPars", 1);
          /*- while (results.hasNext()) {
          	QuerySolution result = results.nextSolution();
          	System.out.println(result);
          }*/

          System.out.println("# Ungrounded Graphs");

          if (graphs.size() > 0) {
            List<LexicalGraph> groundedGraphs = Lists.newArrayList();
            int nbestLexicon = 100;
            int nbestGraphs = 100;
            boolean useSchema = true;
            boolean useKB = true;
            boolean groundFreeVariables = false;
            boolean useEmtpyTypes = false;
            boolean ignoreTypes = false;
            for (LexicalGraph ungroundedGraph : graphs) {
              System.out.print(ungroundedGraph);
              System.out.println("Connected: " + ungroundedGraph.isConnected()
                  + "\n");
              List<LexicalGraph> currentGroundedGraphs =
                  graphCreator.createGroundedGraph(ungroundedGraph,
                      nbestLexicon, nbestGraphs, useSchema, useKB,
                      groundFreeVariables, useEmtpyTypes, ignoreTypes, false);
              groundedGraphs.addAll(currentGroundedGraphs);
            }
            Collections.sort(groundedGraphs);
            groundedGraphs =
                groundedGraphs.size() < nbestGraphs ? groundedGraphs
                    : groundedGraphs.subList(0, nbestGraphs);

            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());
            // int connectedGraphCount = 0;
            int count = 0;
            boolean foundAnswer = false;
            Set<Feature> predGraphFeatures = null;
            Set<Feature> goldGraphFeatures = null;
            for (LexicalGraph groundedGraph : groundedGraphs) {
              count += 1;
              /*-if (count > 1) {
              	break;
              }*/
              System.out.println("# Grounded graph: ");
              System.out.print(groundedGraph);
              System.out.println("Connected: " + groundedGraph.isConnected()
                  + "\n");
              String query =
                  GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                      schema);
              System.out.println("Sentence: " + sentence);
              System.out.println("Pred Query: " + query);
              System.out.println("Gold Query: " + goldQuery);
              Map<String, LinkedHashSet<String>> predResults =
                  rdfGraphTools.runQueryHttp(query);

              boolean areEqual =
                  RdfGraphTools.equalResults(goldResults, predResults);
              System.out.println("Features: " + groundedGraph.getFeatures());
              if (count == 1) {
                predGraphFeatures = groundedGraph.getFeatures();
              }
              System.out.println("Predicted Results: " + predResults);
              System.out.println("Gold Results: " + goldResults);
              /*-if (groundedGraph.isConnected()) {
              	connectedGraphCount += 1;
              	// System.out.println("# Grounded graph: " +
              	// connectedGraphCount);
              }*/

              if (areEqual) {
                goldGraphFeatures = groundedGraph.getFeatures();
                System.out
                    .println("Before Update: " + groundedGraph.getScore());
                learningModel.updateWeightVector(goldGraphFeatures,
                    predGraphFeatures);
                groundedGraph.setScore(learningModel
                    .getScoreTraining(goldGraphFeatures));
                System.out.println("After Update: " + groundedGraph.getScore());
                System.out.println("CORRECT!!");
                foundAnswer = true;
                break;
              } else {
                System.out.println("WRONG!!");
              }
            }

            for (Integer nthBest : maxbestList) {
              if (foundAnswer && count <= nthBest) {
                positives.putIfAbsent(nthBest, 0);
                Integer value = positives.get(nthBest);
                positives.put(nthBest, value + 1);
              } else if (groundedGraphs.size() > 0) {
                negatives.putIfAbsent(nthBest, 0);
                Integer value = negatives.get(nthBest);
                negatives.put(nthBest, value + 1);
              }
            }

            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());
            // System.out.println("# Total number of Connected Grounded Graphs: "
            // + connectedGraphCount);
            System.out.println("\n###########################");
            System.out.println();
          }

          line = br.readLine();
        }
      } finally {
        br.close();
      }

      System.out.println("Iteration: " + iteration);

      for (Integer key : maxbestList) {
        if (positives.containsKey(key) && negatives.containsKey(key)) {
          Integer positive_hits = positives.get(key);
          Integer negative_hits = negatives.get(key);
          int total_hits = sentcount;
          Double precision =
              (positive_hits + 0.0) / (positive_hits + negative_hits) * 100;
          Double recall = (positive_hits + 0.0) / (total_hits) * 100;
          Double fmeas = 2 * precision * recall / (precision + recall);
          System.out
              .println(String
                  .format(
                      "Nbest:%d Positives:%d Negatives:%d Total:%d Prec:%.1f Rec:%.1f Fmeas:%.1f",
                      key, positive_hits, negative_hits, total_hits, precision,
                      recall, fmeas));
        }
      }
      System.out.println("===============================================");
    }
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    // Schema schema = new
    // Schema("data/freebase/schema/business_schema.txt");
    // Schema schema = new
    // Schema("data/freebase/schema/business_schema.txt");
    Schema schema =
        new Schema("data/freebase/schema/business_film_people_schema.txt");
    // Schema schema = null;
    // KnowledgeBase kb = new
    // KnowledgeBase("data/freebase/domain_facts/business_facts.txt.gz");
    KnowledgeBase kb =
        new KnowledgeBase(
            "data/freebase/domain_facts/business_film_people_facts.txt.gz",
            "data/freebase/stats/business_film_people_relation_types.txt");
    // GroundedLexicon groundedLexicon = new
    // GroundedLexicon("data/freebase/grounded_lexicon/business_grounded_lexicon.txt");
    GroundedLexicon groundedLexicon =
        new GroundedLexicon(
            "data/freebase/grounded_lexicon/business_film_people_grounded_lexicon.txt");
    // KnowledgeBase kb = null;

    // CcgParser CcgParser = new CcgParser(ccgAutoLexicon,
    // relationLexicalIdentifiers, argumentLexicalIdenfiers,
    // relationTypingIdentifiers, true);

    RdfGraphTools rdfGraphTools =
        new RdfGraphTools("jdbc:virtuoso://oscart.hot:1111",
            "http://oscart.hot:8890/sparql", "dba", "dba", 2);
    run(schema, kb, groundedLexicon, rdfGraphTools);
  }
}
