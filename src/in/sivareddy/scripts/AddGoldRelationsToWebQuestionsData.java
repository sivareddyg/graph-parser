package in.sivareddy.scripts;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AddGoldRelationsToWebQuestionsData {
  RdfGraphTools endPoint;
  KnowledgeBase kb;
  Schema schema;
  int nthreads;
  private static JsonParser jsonParser = new JsonParser();
  private static Gson gson = new Gson();

  public AddGoldRelationsToWebQuestionsData(RdfGraphTools endPoint,
      KnowledgeBase kb, Schema schema, int nthreads) {
    this.endPoint = endPoint;
    this.kb = kb;
    this.schema = schema;
    this.nthreads = nthreads;
  }

  public void add(InputStream stream, PrintStream out) throws IOException,
      InterruptedException {
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
        Runnable worker = new AddGoldRelationsRunnable(this, jsonSentence, out);
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

  public static class AddGoldRelationsRunnable implements Runnable {
    JsonObject sentence;
    AddGoldRelationsToWebQuestionsData engine;
    PrintStream out;

    public AddGoldRelationsRunnable(AddGoldRelationsToWebQuestionsData engine,
        JsonObject sentence, PrintStream out) {
      this.engine = engine;
      this.sentence = sentence;
      this.out = out;
    }

    @Override
    public void run() {
      engine.addGoldRelations(sentence);
      engine.printSentence(sentence, out);
    }
  }


  public synchronized void printSentence(JsonObject sentence, PrintStream out) {
    out.println(gson.toJson(sentence));
  }

  public void addGoldRelations(JsonObject sentence) {
    if (!sentence.has(SentenceKeys.GOLD_MID)
        || !sentence.has(SentenceKeys.TARGET_VALUE))
      return;

    String goldAnswersString =
        sentence.get(SentenceKeys.TARGET_VALUE).getAsString();
    Pattern goldAnswerPattern =
        Pattern.compile("\\(description \"?(.*?)\"?\\)[ \\)]");
    Matcher matcher = goldAnswerPattern.matcher(goldAnswersString);
    LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
    while (matcher.find()) {
      goldAnswers.add(matcher.group(1));
    }
    Map<String, LinkedHashSet<String>> goldResults = new HashMap<>();
    goldResults.put("targetValue", goldAnswers);

    if (goldAnswers.size() == 0) {
      System.err.println("Gold Answer size cannot be 0: " + goldAnswersString);
      System.exit(0);
      return;
    }

    String goldMid = sentence.get(SentenceKeys.GOLD_MID).getAsString();

    Set<String> potentialEntites = new HashSet<>();
    for (String answer : goldAnswers) {
      potentialEntites = getPotentialAnswerEntities(goldMid, answer);
      if (potentialEntites.size() > 0)
        break;
    }
    if (potentialEntites.size() == 0) {
      System.err.print("No answer mids found: " + goldMid + " ");
      System.err.println(goldAnswers);
    }

    LexicalItem goldEntityNode = new LexicalItem("", "", "", "", "", null);
    goldEntityNode.setMid(goldMid);
    goldEntityNode.setWordPosition(0);

    LexicalItem answerEntityNode = new LexicalItem("", "", "", "", "", null);
    answerEntityNode.setWordPosition(1);

    List<JsonObject> predictedRelationScores = new ArrayList<>();
    for (String answerEntity : potentialEntites) {
      answerEntity = answerEntity.substring(answerEntity.lastIndexOf('/') + 1);
      Set<Relation> relations = kb.getRelations(goldMid, answerEntity);
      for (Relation relation : relations) {
        LexicalGraph graph = new LexicalGraph();
        graph.addEdge(goldEntityNode, answerEntityNode, goldEntityNode,
            relation);
        String query =
            GraphToSparqlConverter.convertGroundedGraph(graph,
                answerEntityNode, schema, null, 100);
        Map<String, LinkedHashSet<String>> predictedAnswers =
            endPoint.runQueryHttp(query);

        double f1 = RdfGraphTools.getPointWiseF1(goldResults, predictedAnswers);
        if (f1 > 0.005) {
          JsonObject relationWithScore = new JsonObject();
          relationWithScore.addProperty(SentenceKeys.RELATION_LEFT,
              relation.getLeft());
          relationWithScore.addProperty(SentenceKeys.RELATION_RIGHT,
              relation.getRight());
          relationWithScore.addProperty(SentenceKeys.SCORE, f1);
          predictedRelationScores.add(relationWithScore);
        }
      }
    }
    predictedRelationScores.sort(Comparator.comparing(x -> -1
        * x.get(SentenceKeys.SCORE).getAsDouble()));
    JsonArray predictedRelationScoresArr = new JsonArray();
    predictedRelationScores.forEach(x -> predictedRelationScoresArr.add(x));

    if (predictedRelationScores.size() > 0)
      sentence.add(SentenceKeys.GOLD_RELATIONS, predictedRelationScoresArr);
    else {
      System.err.print("No relations found: " + goldMid + " ");
      System.err.println(goldAnswers);
    }
  }

  private static Pattern datePattern = Pattern
      .compile("([0-9]*/)?([0-9]*/)?[0-9]{3,4}");

  public Set<String> getPotentialAnswerEntities(String goldEntity, String answer) {
    HashSet<String> answerEntities = new HashSet<>();
    if (datePattern.matcher(answer).matches()) {
      answerEntities.add("type.datetime");
    }

    String query =
        String.format("PREFIX fb: <http://rdf.freebase.com/ns/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT DISTINCT * FROM <http://rdf.freebase.com> "
            + "WHERE { fb:%s ?rel ?x . ?x fb:type.object.name \"%s\"@en . }",
            goldEntity, answer);
    Map<String, LinkedHashSet<String>> results = endPoint.runQueryHttp(query);
    answerEntities.addAll(results.getOrDefault("x", new LinkedHashSet<>()));

    query =
        String.format("PREFIX fb: <http://rdf.freebase.com/ns/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT DISTINCT * FROM <http://rdf.freebase.com> "
            + "WHERE { ?x ?rel fb:%s  . ?x fb:type.object.name \"%s\"@en . }",
            goldEntity, answer);
    results = endPoint.runQueryHttp(query);
    answerEntities.addAll(results.getOrDefault("x", new LinkedHashSet<>()));

    query =
        String.format("PREFIX fb: <http://rdf.freebase.com/ns/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT DISTINCT * FROM <http://rdf.freebase.com> "
            + "WHERE { ?m ?rel1 fb:%s . "
            + "?m %s ?z . ?z fb:freebase.type_hints.mediator true ."
            + "?m ?rel2 ?x . " + "?x fb:type.object.name \"%s\"@en . " + "}",
            goldEntity, KnowledgeBaseOnline.TYPE_KEY, answer);
    results = endPoint.runQueryHttp(query);
    answerEntities.addAll(results.getOrDefault("x", new LinkedHashSet<>()));

    query =
        String.format("PREFIX fb: <http://rdf.freebase.com/ns/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT DISTINCT * FROM <http://rdf.freebase.com> "
            + "WHERE { ?m ?rel1 fb:%s . "
            + "?m %s ?z . ?z fb:freebase.type_hints.mediator true ."
            + "?x ?rel2_inv ?m . " + "?x fb:type.object.name \"%s\"@en . "
            + "}", goldEntity, KnowledgeBaseOnline.TYPE_KEY, answer);
    results = endPoint.runQueryHttp(query);
    answerEntities.addAll(results.getOrDefault("x", new LinkedHashSet<>()));

    query =
        String.format("PREFIX fb: <http://rdf.freebase.com/ns/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT DISTINCT * FROM <http://rdf.freebase.com> "
            + "WHERE {fb:%s ?rel1_inv  ?m . "
            + "?m %s ?z . ?z fb:freebase.type_hints.mediator true ."
            + "?m ?rel2 ?x . " + "?x fb:type.object.name \"%s\"@en . " + "}",
            goldEntity, KnowledgeBaseOnline.TYPE_KEY, answer);
    results = endPoint.runQueryHttp(query);
    answerEntities.addAll(results.getOrDefault("x", new LinkedHashSet<>()));

    query =
        String.format("PREFIX fb: <http://rdf.freebase.com/ns/> "
            + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT DISTINCT * FROM <http://rdf.freebase.com> "
            + "WHERE {fb:%s ?rel1_inv ?m . "
            + "?m %s ?z . ?z fb:freebase.type_hints.mediator true ."
            + "?x ?rel2_inv ?m . " + "?x fb:type.object.name \"%s\"@en . "
            + "}", goldEntity, KnowledgeBaseOnline.TYPE_KEY, answer);

    results = endPoint.runQueryHttp(query);
    answerEntities.addAll(results.getOrDefault("x", new LinkedHashSet<>()));
    return answerEntities;
  }

  public static void main(String[] args) throws InterruptedException,
      IOException {
    RdfGraphTools endPoint =
        new RdfGraphTools(String.format("jdbc:virtuoso://%s:1111", args[0]),
            String.format("http://%s:8890/sparql", args[0]), "dba", "dba");

    Schema schema = new Schema(args[1]);
    KnowledgeBase kb =
        new KnowledgeBaseOnline(String.format("jdbc:virtuoso://%s:1111",
            args[0]), String.format("http://%s:8890/sparql", args[0]), "dba",
            "dba", 500000, schema);
    AddGoldRelationsToWebQuestionsData engine =
        new AddGoldRelationsToWebQuestionsData(endPoint, kb, schema, 20);

    engine.add(System.in, System.out);
  }
}
