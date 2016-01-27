package in.sivareddy.scripts;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EvaluateGraphParserOracleUsingGoldMidAndGoldRelations {
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  private Schema schema;
  private RdfGraphTools endPoint;
  private int nthreads;
  private GroundedLexicon groundedLexicon;
  private KnowledgeBaseOnline kb;
  private GroundedGraphs graphCreator;
  private String semanticParseKey;
  PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
  Logger logger = Logger.getLogger(this.getClass());
  int totalMerges = 0;
  int totalGraphs = 0;
  int graphsFoundSentCount = 0;
  String goldOutputFile;
  final ConcurrentHashMap<String, Pair<HashSet<LexicalGraph>, Double>> sentenceToGoldGraphs =
      new ConcurrentHashMap<>();

  public EvaluateGraphParserOracleUsingGoldMidAndGoldRelations(
      String schemaFile, String endPointName, String semanticParseKey,
      String goldOutputFile, String lexiconFileName, int nthreads,
      boolean allowMerging, boolean handleEventEventEdges,
      boolean useBackoffGraph) throws IOException {

    Schema schema = new Schema(schemaFile);
    RdfGraphTools endPoint =
        new RdfGraphTools(
            String.format("jdbc:virtuoso://%s:1111", endPointName),
            String.format("http://%s:8890/sparql", endPointName), "dba", "dba");

    this.nthreads = nthreads;
    this.endPoint = endPoint;
    this.schema = schema;
    this.goldOutputFile = goldOutputFile;

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

    this.semanticParseKey = semanticParseKey;
    this.graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, new StructuredPercepton(), 1, true,
            true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, allowMerging, handleEventEventEdges,
            useBackoffGraph, 10.0, 1.0, 0.0, 0.0);

    logger.setLevel(Level.DEBUG);
    logger.removeAllAppenders();
  }

  public void evaluateAll(InputStream stream, PrintStream out)
      throws IOException, InterruptedException {
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(nthreads);
    ThreadPoolExecutor threadPool =
        new ThreadPoolExecutor(nthreads, nthreads, 600, TimeUnit.SECONDS, queue);

    threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // this will block if the queue is full
        try {
          executor.getQueue().put(r);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    int sentCount = 0;
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    try {
      String line = br.readLine();
      while (line != null) {
        sentCount += 1;
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        jsonSentence.addProperty(SentenceKeys.INDEX_KEY, sentCount);
        Runnable worker = new EvaluateSentenceRunnable(this, jsonSentence, out);
        threadPool.execute(worker);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
    threadPool.shutdown();

    // Wait until all threads are finished
    while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
      // pass.
    }

    BufferedWriter bw =
        new BufferedWriter(new FileWriter(goldOutputFile + ".readable.txt"));
    if (sentCount > 0) {
      bw.write("Average Merge Count: " + (totalMerges + 0.0) / sentCount);
      bw.write("\n");

      if (graphsFoundSentCount > 0)
        bw.write("Average Number of Grounded Graphs: " + (totalGraphs + 0.0)
            / graphsFoundSentCount);
      bw.write("\n");
    }

    for (Entry<String, Pair<HashSet<LexicalGraph>, Double>> entry : sentenceToGoldGraphs
        .entrySet()) {
      bw.write("Sentence: ");
      bw.write(entry.getKey());
      bw.write("\n");
      bw.write("F1 score: ");
      bw.write(entry.getValue().getRight().toString());
      bw.write("\n");
      Set<LexicalGraph> groundedGraphs = entry.getValue().getLeft();
      bw.write("Total number of grounded graphs: ");
      bw.write(String.valueOf(groundedGraphs.size()));
      bw.write("\n");
      for (LexicalGraph groundedGraph : groundedGraphs) {
        bw.write("Ungrounded graph: ");
        if (groundedGraph != null) {
          bw.write(groundedGraph.getParallelGraph().toString());
        }
        bw.write("\n");
        bw.write("Grounded graph: ");
        if (groundedGraph != null) {
          bw.write(groundedGraph.toString());
        }
        bw.write("\n");
      }
    }
    bw.close();

    // Save gold graphs.
    FileOutputStream fileOut = new FileOutputStream(goldOutputFile + ".ser");
    ObjectOutputStream objStream = new ObjectOutputStream(fileOut);
    objStream.writeObject(sentenceToGoldGraphs);
    objStream.close();
    fileOut.close();
  }

  public static class EvaluateSentenceRunnable implements Runnable {
    JsonObject sentence;
    EvaluateGraphParserOracleUsingGoldMidAndGoldRelations engine;
    PrintStream out;

    public EvaluateSentenceRunnable(
        EvaluateGraphParserOracleUsingGoldMidAndGoldRelations engine,
        JsonObject sentence, PrintStream out) {
      this.engine = engine;
      this.sentence = sentence;
      this.out = out;
    }

    @Override
    public void run() {
      engine.evaluate(sentence, out);
    }
  }


  public void evaluate(JsonObject sentence, PrintStream out) {
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
    } else if (sentence.has(SentenceKeys.SPARQL_QUERY)) {
      Map<String, LinkedHashSet<String>> dummyGoldResultsMap = new HashMap<>();
      dummyGoldResultsMap.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>());
      String goldQuery = sentence.get(SentenceKeys.SPARQL_QUERY).getAsString();
      Map<String, LinkedHashSet<String>> results =
          endPoint.runQueryHttp(goldQuery);
      Pair<Set<String>, Set<String>> cleanedGoldResults =
          RdfGraphTools.getCleanedResults(dummyGoldResultsMap, results);
      goldAnswers = new LinkedHashSet<>(cleanedGoldResults.getRight());
      goldResultsMap.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>(
          goldAnswers));
    }

    String sentenceString =
        sentence.get(SentenceKeys.SENTENCE_KEY).getAsString();

    Set<String> predAnswers = new HashSet<>();
    if (!sentence.has(SentenceKeys.GOLD_MID)
        || !sentence.has(SentenceKeys.GOLD_RELATIONS)) {
      printAnswer(sentenceString, null, goldAnswers, predAnswers, out, logger);
      return;
    }

    List<JsonObject> allSentences = new ArrayList<>();
    if (sentence.has(SentenceKeys.FOREST)) {
      sentence.get(SentenceKeys.FOREST).getAsJsonArray()
          .forEach(x -> allSentences.add(x.getAsJsonObject()));
    } else {
      allSentences.add(sentence);
    }

    // Get all the ungrounded graphs which has main entity node, and a path from
    // main entity node to question node.
    double bestSoFar = 0.0;
    LexicalGraph bestGraph = null;
    HashSet<LexicalGraph> bestGraphs = new HashSet<>();
    Map<String, LinkedHashSet<String>> bestAnswers = null;
    for (JsonObject instance : allSentences) {
      String goldMid = sentence.get(SentenceKeys.GOLD_MID).getAsString();
      List<LexicalGraph> newGraphs =
          graphCreator.buildUngroundedGraph(instance, semanticParseKey, 1);
      for (LexicalGraph newGraph : newGraphs) {
        HashSet<LexicalItem> mainEntityNodes = newGraph.getMidNode(goldMid);
        if (mainEntityNodes == null || mainEntityNodes.size() == 0)
          continue;
        HashSet<LexicalItem> questionNodes = newGraph.getQuestionNode();
        if (questionNodes == null || questionNodes.size() == 0)
          continue;
        LexicalItem questionNode = questionNodes.iterator().next();
        LexicalItem goldNode = mainEntityNodes.iterator().next();

        Pair<LexicalItem, LexicalItem> mainEdgeKey =
            Pair.of(goldNode, questionNode);
        Pair<LexicalItem, LexicalItem> mainEdgeInverseKey =
            Pair.of(questionNode, goldNode);
        Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints =
            new HashMap<>();
        edgeGroundingConstraints.put(mainEdgeKey, new TreeSet<>());
        edgeGroundingConstraints.put(mainEdgeInverseKey, new TreeSet<>());
        for (JsonElement goldRelation : instance.get(
            SentenceKeys.GOLD_RELATIONS).getAsJsonArray()) {
          JsonObject goldRelationObj = goldRelation.getAsJsonObject();
          Relation mainRelation =
              new Relation(goldRelationObj.get(SentenceKeys.RELATION_LEFT)
                  .getAsString(), goldRelationObj.get(
                  SentenceKeys.RELATION_RIGHT).getAsString(), goldRelationObj
                  .get(SentenceKeys.SCORE).getAsDouble());
          Relation mainRelationInverse = mainRelation.inverse();
          edgeGroundingConstraints.get(mainEdgeKey).add(mainRelation);
          edgeGroundingConstraints.get(mainEdgeInverseKey).add(
              mainRelationInverse);
        }

        List<LexicalGraph> groundedGraphs = Lists.newArrayList();
        try {
          groundedGraphs =
              graphCreator.createGroundedGraph(newGraph, null,
                  edgeGroundingConstraints, Sets.newHashSet(goldNode), 1000,
                  10000, true, true, false, false, false, false, true, false);
        } catch (Exception e) {
          System.err.println(instance);
        }

        totalGraphs += groundedGraphs.size();
        for (LexicalGraph graph : groundedGraphs) {
          String query =
              GraphToSparqlConverter.convertGroundedGraph(graph, schema, null,
                  30);
          Map<String, LinkedHashSet<String>> predictedResultsMap =
              endPoint.runQueryHttp(query);
          double f1 =
              RdfGraphTools.getPointWiseF1(goldResultsMap, predictedResultsMap);

          if (f1 > 0.0 && f1 > bestSoFar + 0.00000005) {
            bestGraph = graph;
            bestSoFar = f1;
            bestAnswers = predictedResultsMap;
            bestGraphs = new HashSet<>();
            bestGraphs.add(graph);
          } else if (f1 > 0.0 && f1 + 0.00000005 > bestSoFar
              && bestGraphs.size() > 0) {
            bestGraphs.add(graph);
          }
        }

        /*-if (bestSoFar > 0.9) {
          break;
        }*/
      }
      /*-if (bestSoFar > 0.9) {
        break;
      }*/
    }

    // Store gold graphs for serialisation.
    sentenceToGoldGraphs.put(sentenceString, Pair.of(bestGraphs, bestSoFar));

    Pair<Set<String>, Set<String>> cleanedResults =
        RdfGraphTools.getCleanedResults(goldResultsMap, bestAnswers);
    printAnswer(sentenceString, bestGraph, cleanedResults.getLeft(),
        cleanedResults.getRight(), out, logger);
  }

  public synchronized void printAnswer(String sentence, LexicalGraph bestGraph,
      Set<String> goldAnswers, Set<String> predictedAnswers, PrintStream out,
      Logger log) {
    out.println(String.format("%s\t%s\t%s", sentence, gson.toJson(goldAnswers),
        gson.toJson(predictedAnswers)));

    if (bestGraph != null) {
      totalMerges += bestGraph.getMergeCount();
      log.debug(bestGraph);
      log.debug("Predicted: " + predictedAnswers);
      log.debug("Gold: " + goldAnswers);
      graphsFoundSentCount += 1;
    }
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    String schemaFile = args[0];
    String endPointName = args[1];
    String semanticParseKey = args[2];
    String goldOutputFile = args[3];
    String lexiconFileName = args[4];
    int nthreads = 20;
    boolean allowMerging = Boolean.parseBoolean(args[5]);
    boolean useBackoffGraph = true;
    if (args.length > 6)
      useBackoffGraph = Boolean.parseBoolean(args[6]);

    boolean handleEventEventEdges = false;
    if (semanticParseKey.equals(SentenceKeys.DEPENDENCY_LAMBDA)) {
      handleEventEventEdges = true;
    }

    EvaluateGraphParserOracleUsingGoldMidAndGoldRelations engine =
        new EvaluateGraphParserOracleUsingGoldMidAndGoldRelations(schemaFile,
            endPointName, semanticParseKey, goldOutputFile, lexiconFileName,
            nthreads, allowMerging, handleEventEventEdges, useBackoffGraph);
    engine.evaluateAll(System.in, System.out);
  }
}
