package in.sivareddy.scripts;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DumpBoWGroundedGraphs extends ProcessStreamInterface {
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  private final Schema schema;
  private final RdfGraphTools endPoint;
  private final GroundedLexicon groundedLexicon;
  private final KnowledgeBaseOnline kb;
  private final GroundedGraphs graphCreator;
  PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
  Logger logger = Logger.getLogger(this.getClass());

  final ConcurrentHashMap<String, Pair<HashSet<LexicalGraph>, Double>> sentenceToGoldGraphs =
      new ConcurrentHashMap<>();

  public DumpBoWGroundedGraphs(String schemaFile, String endPointName,
      String lexiconFileName) throws IOException {

    Schema schema = new Schema(schemaFile);
    RdfGraphTools endPoint =
        new RdfGraphTools(
            String.format("jdbc:virtuoso://%s:1111", endPointName),
            String.format("http://%s:8890/sparql", endPointName), "dba", "dba");

    this.endPoint = endPoint;
    this.schema = schema;

    groundedLexicon = new GroundedLexicon(lexiconFileName);
    kb =
        new KnowledgeBaseOnline(endPointName, String.format(
            "http://%s:8890/sparql", endPointName), "dba", "dba", 500000,
            schema);

    CcgAutoLexicon questionCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases_questions_vanilla.txt");

    CcgAutoLexicon normalCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases.txt");

    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};

    this.graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, new StructuredPercepton(), null, 1,
            true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, false, false,
            false, false, 10.0, 1.0, 0.0, 0.0, 0.0);

    logger.setLevel(Level.DEBUG);
    logger.removeAllAppenders();
  }


  @Override
  public void processSentence(JsonObject sentence) {
    Map<String, LinkedHashSet<String>> goldResultsMap = new HashMap<>();
    LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
    if (sentence.has(SentenceKeys.TARGET_VALUE)) {
      String goldAnswersString =
          sentence.get(SentenceKeys.TARGET_VALUE).getAsString();
      Pattern goldAnswerPattern =
          Pattern.compile("\\(description \"?(.*?)\"?\\)[ \\)]");
      Matcher matcher = goldAnswerPattern.matcher(goldAnswersString);
      while (matcher.find()) {
        goldAnswers.add(matcher.group(1));
      }
      goldResultsMap.put(SentenceKeys.TARGET_VALUE, goldAnswers);
    }
    Map<String, LinkedHashSet<String>> predictedResultsMap = new HashMap<>();
    Pair<Set<String>, Set<String>> cleanedResults =
        RdfGraphTools.getCleanedResults(goldResultsMap, predictedResultsMap);

    sentence.add("answer",
        jsonParser.parse(gson.toJson(cleanedResults.getLeft())));

    if (sentence.has(SentenceKeys.FOREST)) {
      for (JsonElement sentElm : sentence.get(SentenceKeys.FOREST)
          .getAsJsonArray()) {
        processIndividualSentence(sentElm.getAsJsonObject());
      }
    }

    if (sentence.has("goldRelations"))
      sentence.remove("goldRelations");
    if (sentence.has("url"))
      sentence.remove("url");
    if (sentence.has("index"))
      sentence.remove("index");
    if (sentence.has("dependency_lambda"))
      sentence.remove("dependency_lambda");
    if (sentence.has("deplambda_oblique_tree"))
      sentence.remove("deplambda_oblique_tree");
    if (sentence.has("deplambda_expression"))
      sentence.remove("deplambda_expression");
    if (sentence.has("targetValue"))
      sentence.remove("targetValue");
    if (sentence.has("original"))
      sentence.remove("original");
    if (sentence.has("goldMid"))
      sentence.remove("goldMid");

  }

  public void processIndividualSentence(JsonObject jsonSentence) {
    List<LexicalGraph> uGraphs =
        graphCreator.buildUngroundedGraph(jsonSentence,
            SentenceKeys.BOW_QUESTION_GRAPH, 1);
    JsonArray graphsArray = new JsonArray();
    jsonSentence.add("graphs", graphsArray);
    for (LexicalGraph uGraph : uGraphs) {
      List<LexicalGraph> groundedGraphs =
          graphCreator.createGroundedGraph(uGraph, 1000, 10000, true, true,
              false, false, false, false, true, false);

      for (LexicalGraph graph : groundedGraphs) {
        String query =
            GraphToSparqlConverter
                .convertGroundedGraph(graph, schema, null, 30);
        Map<String, LinkedHashSet<String>> predictedResultsMap =
            endPoint.runQueryHttp(query);
        Map<String, LinkedHashSet<String>> goldResultsMap = new HashMap<>();
        goldResultsMap.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>());

        Pair<Set<String>, Set<String>> cleanedResults =
            RdfGraphTools
                .getCleanedResults(goldResultsMap, predictedResultsMap);
        // System.out.println(graph);
        // System.out.println(cleanedResults.getRight());

        if (cleanedResults.getRight().size() > 0) {
          JsonObject graphObj = new JsonObject();
          LexicalItem questionNode = graph.getQuestionNode().iterator().next();
          JsonArray edgesArray = new JsonArray();
          for (Edge<LexicalItem> edge : graph.getEdges(questionNode)) {
            JsonObject edgeObject = new JsonObject();
            edgeObject.addProperty("entityIndex", edge.getRight()
                .getWordPosition());
            edgeObject.addProperty(SentenceKeys.RELATION_LEFT, edge
                .getRelation().getRight());
            edgeObject.addProperty(SentenceKeys.RELATION_RIGHT, edge
                .getRelation().getLeft());
            edgesArray.add(edgeObject);
          }
          graphObj.add("graph", edgesArray);
          JsonElement answerArray =
              jsonParser.parse(gson.toJson(cleanedResults.getRight()));
          graphObj.add("denotation", answerArray);
          graphsArray.add(graphObj);
        }
      }
    }
    if (jsonSentence.has("goldRelations"))
      jsonSentence.remove("goldRelations");
    if (jsonSentence.has("url"))
      jsonSentence.remove("url");
    if (jsonSentence.has("index"))
      jsonSentence.remove("index");
    if (jsonSentence.has("dependency_lambda"))
      jsonSentence.remove("dependency_lambda");
    if (jsonSentence.has("deplambda_oblique_tree"))
      jsonSentence.remove("deplambda_oblique_tree");
    if (jsonSentence.has("deplambda_expression"))
      jsonSentence.remove("deplambda_expression");
    if (jsonSentence.has("targetValue"))
      jsonSentence.remove("targetValue");
    if (jsonSentence.has("original"))
      jsonSentence.remove("original");
    if (jsonSentence.has("goldMid"))
      jsonSentence.remove("goldMid");
    if (jsonSentence.has("sentence"))
      jsonSentence.remove("sentence");

    for (JsonElement wordElm : jsonSentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      JsonObject wordObj = wordElm.getAsJsonObject();
      if (wordObj.has("fpos"))
        wordObj.remove("fpos");
      if (wordObj.has("index"))
        wordObj.remove("index");
      if (wordObj.has("feats"))
        wordObj.remove("feats");
      if (wordObj.has("head"))
        wordObj.remove("head");
      if (wordObj.has("dep"))
        wordObj.remove("dep");
      if (wordObj.has("phead"))
        wordObj.remove("phead");
      if (wordObj.has("pdep"))
        wordObj.remove("pdep");
      if (wordObj.has("lang"))
        wordObj.remove("lang");
    }
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    String schemaFile = args[0];
    String endPointName = args[1];
    String lexiconFileName = "lib_data/dummy.txt";
    int nthreads = args.length > 2 ? Integer.parseInt(args[2]) : 10;

    DumpBoWGroundedGraphs engine =
        new DumpBoWGroundedGraphs(schemaFile, endPointName, lexiconFileName);
    engine.processStream(System.in, System.out, nthreads, true);
  }
}
