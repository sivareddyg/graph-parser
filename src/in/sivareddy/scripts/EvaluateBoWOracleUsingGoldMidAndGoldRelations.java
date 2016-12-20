package in.sivareddy.scripts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.util.SentenceKeys;

public class EvaluateBoWOracleUsingGoldMidAndGoldRelations {
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  private Schema schema;
  private RdfGraphTools endPoint;
  private int nthreads;

  public EvaluateBoWOracleUsingGoldMidAndGoldRelations(Schema schema,
      RdfGraphTools endPoint, int nthreads) {
    this.nthreads = nthreads;
    this.endPoint = endPoint;
    this.schema = schema;
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

    int sentCount = 1;
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        jsonSentence.addProperty(SentenceKeys.INDEX_KEY, sentCount);
        Runnable worker = new EvaluateSentenceRunnable(this, jsonSentence, out);
        threadPool.execute(worker);
        sentCount += 1;
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
  }

  public static class EvaluateSentenceRunnable implements Runnable {
    JsonObject sentence;
    EvaluateBoWOracleUsingGoldMidAndGoldRelations engine;
    PrintStream out;

    public EvaluateSentenceRunnable(
        EvaluateBoWOracleUsingGoldMidAndGoldRelations engine,
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
    String goldAnswersString =
        sentence.get(SentenceKeys.TARGET_VALUE).getAsString();
    Pattern goldAnswerPattern =
        Pattern.compile("\\(description \"?(.*?)\"?\\)[ \\)]");
    Matcher matcher = goldAnswerPattern.matcher(goldAnswersString);
    LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
    while (matcher.find()) {
      goldAnswers.add(matcher.group(1));
    }

    String sentenceString =
        sentence.get(SentenceKeys.SENTENCE_KEY).getAsString();

    Set<String> predAnswers = new HashSet<>();
    if (!sentence.has(SentenceKeys.GOLD_MID)
        || !sentence.has(SentenceKeys.GOLD_RELATIONS)) {
      printAnswer(sentenceString, goldAnswers, predAnswers, out);
      return;
    }

    String goldMid = sentence.get(SentenceKeys.GOLD_MID).getAsString();
    LexicalItem goldEntityNode = new LexicalItem("", "", "", "", "", "", null);
    goldEntityNode.setMid(goldMid);
    goldEntityNode.setWordPosition(0);

    LexicalItem answerEntityNode =
        new LexicalItem("", "", "", "", "", "", null);
    answerEntityNode.setWordPosition(1);

    JsonObject relationMap =
        sentence.get(SentenceKeys.GOLD_RELATIONS).getAsJsonArray().get(0)
            .getAsJsonObject();

    Relation relation =
        new Relation(relationMap.get(SentenceKeys.RELATION_LEFT).getAsString(),
            relationMap.get(SentenceKeys.RELATION_RIGHT).getAsString(),
            relationMap.get(SentenceKeys.SCORE).getAsDouble());

    LexicalGraph graph = new LexicalGraph();
    graph.addEdge(goldEntityNode, answerEntityNode, goldEntityNode, relation);
    String query =
        GraphToSparqlConverter.convertGroundedGraph(graph, answerEntityNode,
            schema, null, 30);
    Map<String, LinkedHashSet<String>> predictedResultsMap =
        endPoint.runQueryHttp(query);
    Map<String, LinkedHashSet<String>> goldResultsMap = new HashMap<>();
    goldResultsMap.put(SentenceKeys.TARGET_VALUE, goldAnswers);

    Pair<Set<String>, Set<String>> cleanedResults =
        RdfGraphTools.getCleanedResults(goldResultsMap, predictedResultsMap);
    printAnswer(sentenceString, cleanedResults.getLeft(),
        cleanedResults.getRight(), out);
  }

  public static synchronized void printAnswer(String sentence,
      Set<String> goldAnswers, Set<String> predictedAnswers, PrintStream out) {
    out.println(String.format("%s\t%s\t%s", sentence, gson.toJson(goldAnswers),
        gson.toJson(predictedAnswers)));
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    RdfGraphTools endPoint =
        new RdfGraphTools(String.format("jdbc:virtuoso://%s:1111", args[0]),
            String.format("http://%s:8890/sparql", args[0]), "dba", "dba");

    Schema schema = new Schema(args[1]);

    EvaluateBoWOracleUsingGoldMidAndGoldRelations engine =
        new EvaluateBoWOracleUsingGoldMidAndGoldRelations(schema, endPoint, 20);
    engine.evaluateAll(System.in, System.out);
  }
}
