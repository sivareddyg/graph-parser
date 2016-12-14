package in.sivareddy.graphparser.learning;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.parsing.LexicalGraph.AnswerTypeQuestionWordFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.ValidQueryFeature;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GraphToQueryTraining {
  private static double POINTWISE_F1_THRESHOLD = 0.90;

  private StructuredPercepton learningModel;
  private Schema schema;
  private KnowledgeBase kb;
  private GroundedLexicon groundedLexicon;
  private GroundedGraphs graphCreator;

  int nbestEdges = 20;
  int nbestGraphs = 100;
  int nbestTrainSyntacticParses = 1;
  int nbestTestSyntacticParses = 1;
  int forrestSize = 1;

  boolean useEntityTypes = true;
  boolean useKB = true;
  boolean groundFreeVariables = false;
  boolean groundEntityVariableEdges = false;
  boolean groundEntityEntityEdges = true;
  boolean useEmtpyTypes = false;
  boolean ignoreTypes = false;

  boolean validQueryFlag = true;
  boolean useAnswerTypeQuestionWordFlag = true;
  boolean useNbestSurrogateGraphs = false;
  boolean addBagOfWordsGraph = false;
  boolean addOnlyBagOfWordsGraph = false;
  boolean useGoldRelations = false;
  boolean evaluateOnlyTheFirstBest = false;
  boolean paraphraseClassifierScoreFlag = false;

  String semanticParseKey;

  RdfGraphTools rdfGraphTools;
  List<String> kbGraphUri = null;
  double MARGIN = 30;

  ConcurrentHashMap<String, Pair<Set<LexicalGraph>, Double>> goldGraphsMap =
      null;

  Map<String, Integer> mostFrequentTypes = new HashMap<>();

  public GraphToQueryTraining(Schema schema, KnowledgeBase kb,
      GroundedLexicon groundedLexicon, CcgAutoLexicon normalCcgAutoLexicon,
      CcgAutoLexicon questionCcgAutoLexicon, String semanticParseKey,
      String goldParsesFile, String mostFrequentTypesFile,
      int nbestTrainSyntacticParses, int nbestTestSyntacticParses,
      int nbestEdges, int nbestGraphs, int forrestSize, int ngramLength,
      boolean useSchema, boolean useKB, boolean groundFreeVariables,
      boolean groundEntityVariableEdges, boolean groundEntityEntityEdges,
      boolean useEmtpyTypes, boolean ignoreTypes,
      StructuredPercepton learningModel, boolean urelGrelFlag,
      boolean urelPartGrelPartFlag, boolean utypeGtypeFlag,
      boolean gtypeGrelFlag, boolean grelGrelFlag, boolean ngramGrelPartFlag,
      boolean wordGrelPartFlag, boolean wordGrelFlag, boolean argGrelPartFlag,
      boolean argGrelFlag, boolean questionTypeGrelPartFlag,
      boolean eventTypeGrelPartFlag, boolean stemMatchingFlag,
      boolean mediatorStemGrelPartMatchingFlag,
      boolean argumentStemMatchingFlag,
      boolean argumentStemGrelPartMatchingFlag, boolean ngramStemMatchingFlag,
      boolean graphIsConnectedFlag, boolean graphHasEdgeFlag,
      boolean countNodesFlag, boolean edgeNodeCountFlag,
      boolean useLexiconWeightsRel, boolean useLexiconWeightsType,
      boolean duplicateEdgesFlag, boolean validQueryFlag,
      boolean useAnswerTypeQuestionWordFlag, boolean useNbestSurrogateGraphs,
      boolean addBagOfWordsGraph, boolean addOnlyBagOfWordsGraph,
      boolean handleNumbers, boolean entityScoreFlag,
      boolean entityWordOverlapFlag, boolean paraphraseScoreFlag,
      boolean paraphraseClassifierScoreFlag, boolean allowMerging,
      boolean useGoldRelations, boolean evaluateOnlyTheFirstBest,
      boolean handleEventEventEdges, boolean useExpand, boolean useHyperExpand,
      double initialEdgeWeight, double initialTypeWeight,
      double initialWordWeight, double mergeEdgeWeight,
      double stemFeaturesWeight,
      RdfGraphTools rdfGraphTools, List<String> kbGraphUri) throws IOException {
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};

    this.semanticParseKey = semanticParseKey;

    this.nbestTrainSyntacticParses = nbestTrainSyntacticParses;
    this.nbestTestSyntacticParses = nbestTestSyntacticParses;
    this.nbestEdges = nbestEdges;
    this.nbestGraphs = nbestGraphs;
    this.forrestSize = forrestSize;

    this.useEntityTypes = useSchema;
    this.useKB = useKB;
    this.groundFreeVariables = groundFreeVariables;
    this.groundEntityVariableEdges = groundEntityVariableEdges;
    this.groundEntityEntityEdges = groundEntityEntityEdges;
    this.useEmtpyTypes = useEmtpyTypes;
    this.ignoreTypes = ignoreTypes;

    this.validQueryFlag = validQueryFlag;
    this.useAnswerTypeQuestionWordFlag = useAnswerTypeQuestionWordFlag;

    this.learningModel = learningModel;
    this.schema = schema;
    this.kb = kb;
    this.groundedLexicon = groundedLexicon;

    this.rdfGraphTools = rdfGraphTools;
    this.kbGraphUri = kbGraphUri;
    this.useNbestSurrogateGraphs = useNbestSurrogateGraphs;
    this.addOnlyBagOfWordsGraph = addOnlyBagOfWordsGraph;
    this.addBagOfWordsGraph = addBagOfWordsGraph || addOnlyBagOfWordsGraph;
    this.useGoldRelations = useGoldRelations;
    this.evaluateOnlyTheFirstBest = evaluateOnlyTheFirstBest;
    this.paraphraseClassifierScoreFlag = paraphraseClassifierScoreFlag;

    // Load gold parses for training sentences.
    loadGoldParsesFile(goldParsesFile);

    // Loads most frequent types.
    loadMostFrequentTypes(mostFrequentTypesFile);

    boolean ignorePronouns = true;
    this.graphCreator = new GroundedGraphs(this.schema, this.kb,
        this.groundedLexicon, normalCcgAutoLexicon, questionCcgAutoLexicon,
        relationLexicalIdentifiers, relationTypingIdentifiers,
        this.learningModel, ngramLength, urelGrelFlag, urelPartGrelPartFlag,
        utypeGtypeFlag, gtypeGrelFlag, grelGrelFlag, ngramGrelPartFlag,
        wordGrelPartFlag, wordGrelFlag, argGrelPartFlag, argGrelFlag,
        questionTypeGrelPartFlag, eventTypeGrelPartFlag, stemMatchingFlag,
        mediatorStemGrelPartMatchingFlag, argumentStemMatchingFlag,
        argumentStemGrelPartMatchingFlag, ngramStemMatchingFlag,
        graphIsConnectedFlag, graphHasEdgeFlag, countNodesFlag,
        edgeNodeCountFlag, useLexiconWeightsRel, useLexiconWeightsType,
        duplicateEdgesFlag, ignorePronouns, handleNumbers, entityScoreFlag,
        entityWordOverlapFlag, paraphraseScoreFlag,
        paraphraseClassifierScoreFlag, allowMerging, handleEventEventEdges,
        useExpand, useHyperExpand, initialEdgeWeight, initialTypeWeight,
        initialWordWeight, mergeEdgeWeight, stemFeaturesWeight);
  }


  /**
   * Loads most frequent types.
   * 
   * @param fileName
   * @throws IOException
   */
  void loadMostFrequentTypes(String fileName) throws IOException {
    if (fileName == null || fileName.trim().equals(""))
      return;

    BufferedReader br = new BufferedReader(new FileReader(fileName));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (!line.equals("") && !line.startsWith("#")) {
          String[] parts = line.split("\t");
          mostFrequentTypes.put(parts[0], Integer.parseInt(parts[1]));
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  @SuppressWarnings("unchecked")
  void loadGoldParsesFile(String goldParsesFile) throws IOException {
    if (goldParsesFile != null && !goldParsesFile.equals("")) {
      FileInputStream fileIn = new FileInputStream(goldParsesFile);
      ObjectInputStream in = new ObjectInputStream(fileIn);
      try {
        this.goldGraphsMap =
            (ConcurrentHashMap<String, Pair<Set<LexicalGraph>, Double>>) in
                .readObject();
      } catch (ClassNotFoundException e1) {
        e1.printStackTrace();
      }
      in.close();
      fileIn.close();
    }
  }

  JsonParser jsonParser = new JsonParser();

  public void trainFromSentences(List<String> trainingSample, int nthreads,
      String logFile, boolean debugEnabled) throws IOException,
      InterruptedException {
    Logger logger = Logger.getLogger(logFile);
    logger.removeAllAppenders();
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    logger.setAdditivity(false);
    if (debugEnabled) {
      logger.setLevel(Level.DEBUG);
      RollingFileAppender appender = new RollingFileAppender(layout, logFile);
      appender.setMaxFileSize("100MB");
      logger.addAppender(appender);
    } else {
      logger.setLevel(Level.INFO);
      RollingFileAppender appender = new RollingFileAppender(layout, logFile);
      appender.setMaxFileSize("100MB");
      logger.addAppender(appender);
    }

    if (trainingSample == null || trainingSample.size() == 0) {
      logger.info("Training sample empty");
      return;
    }

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

    Queue<Logger> deadThredsLogs = new ConcurrentLinkedQueue<>();
    List<RollingFileAppender> appenders = new ArrayList<>();
    for (int i = 0; i < nthreads; i++) {
      // nthreads + 2 to have two extra loggers so that if one of the logger
      // fails, there are other loggers to carry on.
      Logger threadLogger = Logger.getLogger(logFile + ".thread" + i);
      threadLogger.removeAllAppenders();
      threadLogger.setAdditivity(false);
      if (debugEnabled)
        threadLogger.setLevel(Level.DEBUG);
      else
        threadLogger.setLevel(Level.INFO);

      RollingFileAppender appender =
          new RollingFileAppender(layout, logFile + ".thread" + i);
      appenders.add(appender);
      appender.setMaxFileSize("100MB");
      threadLogger.addAppender(appender);
      threadLogger.info("######## Training Starts");
      deadThredsLogs.add(threadLogger);
    }

    int sentCount = 0;
    for (String jsonSentenceString : trainingSample) {
      JsonObject jsonSentence =
          jsonParser.parse(jsonSentenceString).getAsJsonObject();
      Runnable worker =
          new trainFromSentenceRunnable(this, jsonSentence, sentCount,
              debugEnabled, deadThredsLogs);
      threadPool.execute(worker);
      sentCount += 1;
    }
    threadPool.shutdown();

    // Wait until all threads are finished.
    while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
      logger.debug("Awaiting completion of threads.");
    }
    for (RollingFileAppender appender : appenders) {
      appender.close();
    }
  }

  public static class trainFromSentenceRunnable implements Runnable {
    private JsonObject jsonSentence;
    boolean debugEnabled;
    GraphToQueryTraining graphToQuery;
    int sentCount;
    Queue<Logger> logs;

    public trainFromSentenceRunnable(GraphToQueryTraining graphToQuery,
        JsonObject jsonSentence, int sentCount, boolean debugEnabled,
        Queue<Logger> logs) {
      this.jsonSentence = jsonSentence;
      this.debugEnabled = debugEnabled;

      this.graphToQuery = graphToQuery;
      this.sentCount = sentCount;
      this.logs = logs;
    }

    @Override
    public void run() {
      Preconditions
          .checkArgument(
              logs.size() > 0,
              "Insufficient number of loggers. Loggers should be at the size of blocking queue");
      Logger log = logs.poll();
      log.info("##### Sentence count: " + sentCount);
      boolean hasGoldQuery =
          jsonSentence.has("sparqlQuery") || jsonSentence.has("targetValue")
              || jsonSentence.has(SentenceKeys.ANSWER_F1)
              || jsonSentence.has("answerSubset") || jsonSentence.has("answer");
      if (hasGoldQuery) {
        // Supervised training can make use of more number of syntactic
        // parses.
        graphToQuery.supervisedTraining(jsonSentence, log, debugEnabled);
      } else {
        graphToQuery.trainingByQuestioning(jsonSentence, log, debugEnabled);
      }
      logs.add(log);
    }
  }

  private void trainingByQuestioning(JsonObject jsonSentence, Logger logger,
      boolean debugEnabled) {
    int nbestParses = nbestTrainSyntacticParses;
    String sentence = jsonSentence.get("sentence").getAsString();
    logger.debug("######### Sentence: " + sentence);
    // Get ungrounded graphs
    List<LexicalGraph> uGraphs = new ArrayList<>();

    // Add graphs from syntactic parse/already given semantic parses.
    if (!addOnlyBagOfWordsGraph) {
      uGraphs.addAll(graphCreator.buildUngroundedGraph(jsonSentence,
          semanticParseKey, nbestParses, logger));
    }

    // Add a bag-of-word graph.
    if (addOnlyBagOfWordsGraph || addBagOfWordsGraph) {
      uGraphs.addAll(graphCreator.getBagOfWordsUngroundedGraph(jsonSentence));
    }


    if (uGraphs.size() < 1) {
      logger.debug("No uGraphs");
      return;
    }

    Set<LexicalItem> lexicalisedNodes = Sets.newHashSet();
    Set<LexicalItem> uniqueNodes = Sets.newHashSet();
    Property uniqPropery = new Property("UNIQUE");

    @SuppressWarnings("unused")
    boolean hasDecimal = false;
    boolean hasYear = false;
    Set<LexicalItem> years = Sets.newHashSet();
    Set<LexicalItem> decimals = Sets.newHashSet();
    LexicalItem mostPopularNode = null;
    int highestDegree = 0;
    for (int i = 0; i < uGraphs.size(); i++) {
      LexicalGraph uGraph = uGraphs.get(i);
      if (debugEnabled) {
        logger.debug("Ungrounded Graph: " + i + " " + uGraph);
      }

      Set<LexicalItem> nodes = uGraph.getNodes();
      for (LexicalItem node : nodes) {
        // A lexicalised node should be lexicalised in all the
        // ungrounded graphs.
        // Similarly UNIQUE nodes should also have UNIQUE property in
        // all the ungrounded graphs
        if (!GroundedGraphs.nodeIsLexicalised(node)) {
          if (i > 0 && lexicalisedNodes.contains(node)) {
            lexicalisedNodes.remove(node);
          }
          if (i > 0 && uniqueNodes.contains(node)) {
            uniqueNodes.remove(node);
          }
          if (i > 0 && decimals.contains(node)) {
            decimals.remove(node);
          }
          if (i > 0 && years.contains(node)) {
            years.remove(node);
          }
          continue;
        }

        // node is lexicalised
        if (i > 0 && !lexicalisedNodes.contains(node)) {
          continue;
        } else if (i == 0) {
          lexicalisedNodes.add(node);

          // checking if the example has decimals in it
          if (node.getMid().equals("type.float")
              || node.getMid().equals("type.int")) {
            hasDecimal = true;
            logger.debug("Example contains decimal");
            decimals.add(node);
            // return;
          }

          // checking if the example has valid years in it
          if (node.getMid().equals("type.datetime")) {
            hasYear = true;
            Pattern yearPattern = Pattern.compile("([0-9]{3,4})");
            String word = node.getWord();
            Matcher matcher = yearPattern.matcher(word);
            if (matcher.find()) {
              years.add(node);
            } else {
              logger.debug("unknown year - cannot use this example: " + node);
              return;
            }
          }

          Set<Edge<LexicalItem>> edges = uGraph.getEdges(node);
          Set<Type<LexicalItem>> types = uGraph.getTypes(node);
          int degree = 0;
          if (edges != null) {
            degree += edges.size();
          }
          if (types != null) {
            degree += types.size();
          }
          if (highestDegree <= degree) {
            highestDegree = degree;
            mostPopularNode = node;
          }
        }

        // node has UNIQUE property
        Set<Property> nodeProperties = uGraph.getProperties(node);
        if (nodeProperties == null || !nodeProperties.contains(uniqPropery)) {
          if (i > 0 && uniqueNodes.contains(node)) {
            uniqueNodes.remove(node);
          }
          continue;
        } else if (i == 0) {
          uniqueNodes.add(node);
        }
      }
    }

    for (LexicalItem node : decimals) {
      String decimalString = node.getWord();
      Long decimal = convertToInteger(decimalString);
      if (decimal == null) {
        logger.debug("Wrong decimal type: cannot use example" + node);
        return;
      } else if (decimal < 5) {
        logger.debug("Decimal value less than 5: cannot use example" + node);
        return;
      }
    }

    // choosing the target node which is used to create the question
    boolean hasUnique = false;
    LexicalItem targetNode;
    if (decimals.size() > 0) {
      targetNode = decimals.iterator().next();
    } else if (lexicalisedNodes.size() > 0) {
      // if the sentence has lexicalised units, select a random node
      // List<LexicalItem> lexicalisedNodesList =
      // Lists.newArrayList(lexicalisedNodes);
      // int randUniqNode = new Random().nextInt(lexicalisedNodes.size());
      // targetNode = lexicalisedNodesList.get(randUniqNode);
      targetNode = mostPopularNode;
      if (uniqueNodes.contains(targetNode)) {
        hasUnique = true;
      }
    } else {
      logger
          .debug("No lexicalised entities found. Cannot use the sentence for training");
      return;
    }

    if (debugEnabled) {
      logger.debug("Chosen target node: " + targetNode);
    }

    // target variable in the sparql query and its answer
    String targetVar =
        GraphToSparqlConverter.getNodeVariable(targetNode, targetNode).replace(
            "?", "");
    String answer = targetNode.getMid();
    Long integerAnswer = -1L;
    boolean answerIsDate = false;
    boolean answerIsDecimal = false;
    if (answer.startsWith("m.")) {
      answer = "http://rdf.freebase.com/ns/" + answer;
    } else if (answer.equals("type.datetime")) {
      Pattern yearPattern = Pattern.compile("([0-9]{3,4})");
      String word = targetNode.getWord();
      Matcher matcher = yearPattern.matcher(word);
      if (matcher.find()) {
        answer = matcher.group(1);
      } else {
        logger.debug("unknown year - cannot use as target type");
        return;
      }
      answerIsDate = true;
    } else if (answer.equals("type.int") || answer.equals("type.float")) {
      logger.debug("answer type is decimal");
      integerAnswer = convertToInteger(targetNode.getWord());
      if (integerAnswer == null) {
        logger.debug("unknown decimal type: cannot use as target type"
            + integerAnswer);
        return;
      }
      answerIsDecimal = true;
    }

    // replacing the target entity with variable
    List<LexicalGraph> predGgraphsWild = Lists.newArrayList();
    for (LexicalGraph uGraph : uGraphs) {
      if (uGraph.getEdges().size() == 0) {
        logger.debug("Graph has NO edges. Discard ");
        continue;
      }

      // Wild graphs have an empty question slot on the target node.
      List<LexicalGraph> wildGraphs =
          graphCreator.createGroundedGraph(uGraph, Sets.newHashSet(targetNode),
              nbestEdges, nbestGraphs, useEntityTypes, useKB,
              groundFreeVariables, groundEntityVariableEdges,
              groundEntityEntityEdges, useEmtpyTypes, ignoreTypes, false);

      // Setting syntactic parse of the wild graphs.
      if (uGraph.getSyntacticParse() != null) {
        for (LexicalGraph wildGraph : wildGraphs) {
          wildGraph.setSyntacticParse(uGraph.getSyntacticParse());
        }
      }

      predGgraphsWild.addAll(wildGraphs);
      Collections.sort(predGgraphsWild);
      predGgraphsWild =
          predGgraphsWild.size() < nbestGraphs ? predGgraphsWild
              : predGgraphsWild.subList(0, nbestGraphs);
    }

    if (predGgraphsWild.size() == 0) {
      logger.debug("No predicted graphs found");
      return;
    }

    if (validQueryFlag) {
      // Predicted graph reranking using ValidQuery Feature
      int nbestPredictedGraphs =
          predGgraphsWild.size() < nbestGraphs / 4 ? predGgraphsWild.size()
              : nbestGraphs / 4;
      logger.debug("validQueryFlag on: Reranking the top "
          + nbestPredictedGraphs + " parses");
      for (LexicalGraph pGraph : predGgraphsWild.subList(0,
          nbestPredictedGraphs)) {
        String query =
            GraphToSparqlConverter.convertGroundedGraph(pGraph, targetNode,
                schema, kbGraphUri, 10);

        Map<String, LinkedHashSet<String>> resultsMap =
            rdfGraphTools.runQueryHttp(query);
        LinkedHashSet<String> results =
            resultsMap != null && resultsMap.containsKey(targetVar) ? resultsMap
                .get(targetVar) : null;

        if ((results == null || results.size() == 0)
            || (results.size() == 1 && results.iterator().next()
                .startsWith("0^^"))) {
          ValidQueryFeature feat = new ValidQueryFeature(false);
          pGraph.addFeature(feat);
        } else {
          // if the query produces answer, update its score
          ValidQueryFeature feat = new ValidQueryFeature(true);
          pGraph.addFeature(feat);
          pGraph.setScore(learningModel.getScoreTraining(pGraph.getFeatures()));
        }
      }
    }

    Collections.sort(predGgraphsWild);

    // One or few of the constrained graphs are used to select gold graphs -
    // constrained graphs help in search space reduction so that SPARQL query
    // bottleneck can be removed to some extent - Sadly Sparql querying is still
    // slow.
    List<LexicalGraph> predGgraphsConstrained = Lists.newArrayList();
    for (LexicalGraph uGraph : uGraphs) {
      if (uGraph.getEdges().size() == 0) {
        logger.debug("Graph has NO edges. Discard ");
        continue;
      }

      // Constrained graphs try to use all the entities in the sentence while
      // constructing grounded graphs.
      List<LexicalGraph> constrainedGraphs =
          graphCreator.createGroundedGraph(uGraph, nbestEdges, nbestGraphs,
              useEntityTypes, useKB, groundFreeVariables,
              groundEntityVariableEdges, groundEntityEntityEdges,
              useEmtpyTypes, ignoreTypes, false);

      predGgraphsConstrained.addAll(constrainedGraphs);
      Collections.sort(predGgraphsConstrained);
      predGgraphsConstrained =
          predGgraphsConstrained.size() < nbestGraphs ? predGgraphsConstrained
              : predGgraphsConstrained.subList(0, nbestGraphs);
    }

    if (predGgraphsConstrained.size() == 0) {
      logger.debug("No grounded graphs found");
      return;
    }

    // Add the feature if the graph results is a valid query
    List<Pair<LexicalGraph, LinkedHashSet<String>>> gGraphsAndResults =
        Lists.newArrayList();
    Map<String, LinkedHashSet<String>> resultsMap;
    LinkedHashSet<String> results;
    for (LexicalGraph gGraph : predGgraphsConstrained) {
      String query =
          GraphToSparqlConverter.convertGroundedGraph(gGraph, targetNode,
              schema, kbGraphUri, 10);
      resultsMap = rdfGraphTools.runQueryHttp(query);
      results =
          resultsMap != null && resultsMap.containsKey(targetVar) ? resultsMap
              .get(targetVar) : null;

      gGraphsAndResults.add(Pair.of(gGraph, results));

      if (validQueryFlag) {
        if ((results == null || results.size() == 0)
            || (results.size() == 1 && results.iterator().next()
                .startsWith("0^^"))) {
          ValidQueryFeature feat = new ValidQueryFeature(false);
          gGraph.addFeature(feat);
        } else {
          // if the query produces answer, update its score
          ValidQueryFeature feat = new ValidQueryFeature(true);
          gGraph.addFeature(feat);
          gGraph.setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
        }
      }
    }
    gGraphsAndResults.sort(Comparator.comparing(x -> x.getLeft()));

    // Selecting the gold graphs.
    List<Pair<Integer, LexicalGraph>> goldGraphs = Lists.newArrayList();
    List<Pair<Integer, LexicalGraph>> goldGraphsPossible = Lists.newArrayList();
    for (Pair<LexicalGraph, LinkedHashSet<String>> gGraphPair : gGraphsAndResults) {
      LexicalGraph gGraph = gGraphPair.getLeft();
      results = gGraphPair.getRight();
      if (answerIsDecimal) {
        logger.debug("Decimal answers: " + results);
        if (setContainsDecimal(results, integerAnswer)) {
          goldGraphsPossible.add(Pair.of(results.size(), gGraph));
          logger.debug("Decimal answer found: " + results);
        }
      } else {
        if (answerIsDate) {
          results = RdfGraphTools.handleXMLSchemaEntries(results);
        }
        if (results != null && results.contains(answer)) {
          goldGraphsPossible.add(Pair.of(results.size(), gGraph));
        }
      }
    }

    if (goldGraphsPossible.size() == 0) {
      logger.debug("No gold graphs found");
      return;
    }

    // select the graphs which have few results if the target variable is of
    // UNIQUE type
    if (hasUnique || answerIsDate) {
      Collections.sort(goldGraphsPossible);
      int smallestSize = goldGraphsPossible.get(0).getLeft();
      if (smallestSize > 1) {
        logger.debug("Unique but the result set is big");
        return;
      }
      for (Pair<Integer, LexicalGraph> goldGraphPair : goldGraphsPossible) {
        Integer resultsSize = goldGraphPair.getLeft();
        if (smallestSize == resultsSize) {
          goldGraphs.add(goldGraphPair);
        } else {
          break;
        }
      }
    } else {
      goldGraphs = goldGraphsPossible;
    }

    // second filtering - If the sentence has a year, check if the year is
    // predicted.
    if (hasYear && !answerIsDate) {
      List<Pair<Integer, LexicalGraph>> filteredGoldGraphs =
          Lists.newArrayList();
      for (Pair<Integer, LexicalGraph> goldGraphPair : goldGraphs) {
        LexicalGraph goldGraph = goldGraphPair.getRight();
        // using year as target node
        targetNode = years.iterator().next();
        targetVar =
            GraphToSparqlConverter.getNodeVariable(targetNode, targetNode)
                .replace("?", "");
        logger.debug("Example has an year: " + targetNode);
        String year = targetNode.getWord();
        Pattern yearPattern = Pattern.compile("([0-9]{3,4})");
        Matcher matcher = yearPattern.matcher(year);
        if (matcher.find()) {
          year = matcher.group(1);
        }
        String query =
            GraphToSparqlConverter.convertGroundedGraph(goldGraph, targetNode,
                schema, kbGraphUri, 10);
        logger.debug("Year query: " + query);
        resultsMap = rdfGraphTools.runQueryHttp(query);
        logger.debug("Year pred results: " + resultsMap);
        results =
            resultsMap != null && resultsMap.containsKey(targetVar) ? resultsMap
                .get(targetVar) : null;
        results = RdfGraphTools.handleXMLSchemaEntries(results);
        if (results == null || !results.contains(year)) {
          logger
              .debug("The gold graph does not predict the year. Cannot use this example");
          return;
        } else {
          filteredGoldGraphs.add(goldGraphPair);
        }
      }
      goldGraphs = filteredGoldGraphs;
    }

    if (goldGraphs == null || goldGraphs.size() == 0) {
      logger.debug("No gold graphs found");
      return;
    }

    LexicalGraph bestGoldGraph = goldGraphs.get(0).getRight();

    // Predicted Graphs within margin of bestGoldGraph.
    List<LexicalGraph> predGraphsWithinMargin = Lists.newArrayList();
    for (LexicalGraph gGraph : predGgraphsWild) {
      double marginDifference =
          Math.abs(bestGoldGraph.getScore() - gGraph.getScore());
      if (marginDifference > MARGIN)
        break;
      predGraphsWithinMargin.add(gGraph);
      if (!useNbestSurrogateGraphs)
        break;
    }

    if (predGraphsWithinMargin.size() == 0) {
      logger.debug("Difference in predicted and gold are beyond margin");
      return;
    }
    LexicalGraph bestPredictedGraph = predGraphsWithinMargin.get(0);

    // Gold Graphs that are used for learning.
    List<LexicalGraph> finalGoldGraphs = Lists.newArrayList();
    for (Pair<Integer, LexicalGraph> gGraphPair : goldGraphs) {
      finalGoldGraphs.add(gGraphPair.getRight());
      if (!useNbestSurrogateGraphs)
        break;
      else {
        // Duplicate the gold graph since all gold graphs will also be present
        // in the predicted graphs.
        finalGoldGraphs.add(gGraphPair.getRight());
      }
    }

    // Collect all features from gold graphs.
    List<Feature> goldGraphFeatures = Lists.newArrayList();
    for (LexicalGraph goldGraph : finalGoldGraphs) {
      List<Feature> feats = goldGraph.getFeatures();
      goldGraphFeatures.addAll(feats);
    }

    // Collect all features from predicted graphs.
    List<Feature> predGraphFeatures = Lists.newArrayList();
    for (LexicalGraph predGraph : predGraphsWithinMargin) {
      List<Feature> feats = predGraph.getFeatures();
      predGraphFeatures.addAll(feats);
    }

    logger.info("Sentence: " + sentence);
    logger.info("Predicted Graphs Within Margin Size: "
        + predGraphsWithinMargin.size());
    logger.info("Gold Graphs Size: " + finalGoldGraphs.size());
    logger.info("Best predicted graph: " + predGraphsWithinMargin.get(0));
    logger.info("Best gold graph: " + finalGoldGraphs.get(0));

    if (debugEnabled && !useNbestSurrogateGraphs) {
      logger.debug("Best Gold graph features before update");
      learningModel.printFeatureWeights(bestGoldGraph.getFeatures(), logger);
    }

    if (debugEnabled && !useNbestSurrogateGraphs) {
      logger.debug("Best Predicted graph features before update");
      learningModel.printFeatureWeights(bestPredictedGraph.getFeatures(),
          logger);
    }

    logger.debug("Predicted Before Update: " + bestPredictedGraph.getScore());
    logger.debug("Gold Before Update: " + bestGoldGraph.getScore());
    learningModel
        .updateWeightVector(1, goldGraphFeatures, 1, predGraphFeatures);
    bestPredictedGraph.setScore(learningModel
        .getScoreTraining(bestPredictedGraph.getFeatures()));
    bestGoldGraph.setScore(learningModel.getScoreTraining(bestGoldGraph
        .getFeatures()));
    logger.debug("Predicted After Update: " + bestPredictedGraph.getScore());
    logger.debug("Gold After Update: " + bestGoldGraph.getScore());

    if (debugEnabled && !useNbestSurrogateGraphs) {
      logger.debug("Predicted graph features after update");
      learningModel.printFeatureWeights(bestPredictedGraph.getFeatures(),
          logger);
    }

    if (debugEnabled && !useNbestSurrogateGraphs) {
      logger.debug("Gold graph features after update");
      learningModel.printFeatureWeights(bestGoldGraph.getFeatures(), logger);
    }

    if (semanticParseKey.equals(SentenceKeys.CCG_PARSES)) {
      Set<String> goldSynParsSet = new HashSet<>();
      JsonArray goldSynPars = new JsonArray();
      for (LexicalGraph gGraph : finalGoldGraphs) {
        String goldSynPar = gGraph.getSyntacticParse();
        if (!goldSynParsSet.contains(goldSynPar)) {
          JsonObject synParseObject = new JsonObject();
          synParseObject.addProperty("score", gGraph.getScore());
          synParseObject.addProperty("synPar", goldSynPar);
          goldSynPars.add(synParseObject);
          goldSynParsSet.add(goldSynPar);
        }
      }
      jsonSentence.add("goldSynPars", goldSynPars);
      if (goldSynParsSet.size() > 0) {
        logger.info("Valid Gold Parses: " + jsonSentence.toString());
      }
    }
    logger.debug("#############");
  }

  public void groundSentences(List<String> sentences, Logger logger,
      String logFile, int nthreads) throws IOException, InterruptedException {
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    logger.info("Grounding Input Sentences: =============================");
    if (sentences == null || sentences.size() == 0) {
      logger.info("No sentences to ground");
      return;
    }

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

    Queue<Logger> deadThredsLogs = new ConcurrentLinkedQueue<>();
    List<RollingFileAppender> appenders = new ArrayList<>();
    for (int i = 0; i < nthreads + 2; i++) {
      Logger threadLogger = Logger.getLogger(logFile + ".thread" + i);
      threadLogger.removeAllAppenders();
      threadLogger.setAdditivity(false);
      threadLogger.setLevel(Level.INFO);
      RollingFileAppender appender =
          new RollingFileAppender(layout, logFile + ".thread" + i);
      appenders.add(appender);
      appender.setMaxFileSize("10000MB");
      threadLogger.addAppender(appender);
      threadLogger.info("#### Grounding starts");
      deadThredsLogs.add(threadLogger);
    }

    int sentCount = 0;
    for (String testSentence : sentences) {
      JsonObject jsonSentence =
          jsonParser.parse(testSentence).getAsJsonObject();
      Runnable worker =
          new GetGroundedGraphsWithAllEntitiesRunnable(this, jsonSentence,
              deadThredsLogs, sentCount);
      threadPool.execute(worker);
      sentCount += 1;
    }
    threadPool.shutdown();

    while (!threadPool.awaitTermination(15, TimeUnit.SECONDS)) {
      logger.debug("Awaiting completion of threads.");
    }
    for (RollingFileAppender appender : appenders) {
      appender.close();
    }
  }

  public List<LexicalGraph> getGroundedGraphsWithAllEntities(
      JsonObject jsonSentence, Logger logger, int sentCount) {
    String sentence = jsonSentence.get("sentence").getAsString();
    logger.info("Sentence " + sentCount + ": " + sentence);
    List<LexicalGraph> uGraphs = new ArrayList<>();

    // Add graphs from syntactic parse/already given semantic parses.
    if (!addOnlyBagOfWordsGraph) {
      uGraphs.addAll(graphCreator.buildUngroundedGraph(jsonSentence,
          semanticParseKey, nbestTestSyntacticParses, logger));
    }

    // Add a bag-of-word graph.
    if (addOnlyBagOfWordsGraph || addBagOfWordsGraph) {
      uGraphs.addAll(graphCreator.getBagOfWordsUngroundedGraph(jsonSentence));
    }

    if (uGraphs.size() < 1) {
      logger.info("No ungrounded graphs found");
      return null;
    }

    List<LexicalGraph> bestGroundedGraphs = Lists.newArrayList();
    for (LexicalGraph uGraph : uGraphs) {
      if (uGraph.getEdges().size() == 0) {
        continue;
      }

      // Constrained graphs try to use all the entities in the sentence while
      // constructing grounded graphs.
      List<LexicalGraph> groundedGraphs =
          graphCreator.createGroundedGraph(uGraph, nbestEdges, nbestGraphs,
              useEntityTypes, useKB, groundFreeVariables,
              groundEntityVariableEdges, groundEntityEntityEdges,
              useEmtpyTypes, ignoreTypes, true);

      bestGroundedGraphs.addAll(groundedGraphs);
      Collections.sort(bestGroundedGraphs);
      bestGroundedGraphs =
          bestGroundedGraphs.size() < nbestGraphs ? bestGroundedGraphs
              : bestGroundedGraphs.subList(0, nbestGraphs);
    }

    if (bestGroundedGraphs.size() == 0) {
      logger.info("No grounded graphs found");
      return null;
    }

    Set<String> entities = new HashSet<>();
    for (JsonElement entityElement : jsonSentence.get("entities")
        .getAsJsonArray()) {
      JsonObject entityObject = entityElement.getAsJsonObject();
      if (entityObject.has("entity")) {
        if (entityObject.get("entity").getAsString().startsWith("m.")
            || entityObject.get("entity").getAsString().equals("type.datetime")) {
          entities.add(entityObject.get("entity").getAsString());
        }
      }
    }

    List<LexicalGraph> validGraphs = new ArrayList<>();
    for (LexicalGraph groundedGraph : bestGroundedGraphs) {
      Set<String> graphEntities = new HashSet<>();
      List<LexicalItem> connectedNodes =
          groundedGraph.getNodesConnectedByEdges();
      for (LexicalItem node : connectedNodes) {
        graphEntities.add(node.getMID());
      }
      if (graphEntities.containsAll(entities)) {
        validGraphs.add(groundedGraph);
      }
    }

    if (validGraphs.size() > 0) {
      logger.info("Number of grounded graphs containing all entities: "
          + validGraphs.size());
      logger.info("Best ungrounded graph: ");
      logger.info(validGraphs.get(0).getParallelGraph());
      logger.info("Best grounded graph: ");
      logger.info(validGraphs.get(0));
    } else if (bestGroundedGraphs.size() > 0) {
      logger
          .info("Grounded graphs does not have all entities: Ignoring groundings: "
              + bestGroundedGraphs.size());
    } else {
      logger.info("No groundings found!");
    }
    return validGraphs;
  }

  public static class GetGroundedGraphsWithAllEntitiesRunnable implements
      Runnable {
    private JsonObject jsonSentence;
    GraphToQueryTraining graphToQuery;
    Queue<Logger> loggers;
    int sentCount = 0;

    public GetGroundedGraphsWithAllEntitiesRunnable(
        GraphToQueryTraining graphToQuery, JsonObject jsonSentence,
        Queue<Logger> loggers, int sentCount) {
      this.jsonSentence = jsonSentence;
      this.graphToQuery = graphToQuery;
      this.loggers = loggers;
      this.sentCount = sentCount;
    }

    @Override
    public void run() {
      Preconditions
          .checkArgument(
              loggers.size() > 0,
              "Insufficient number of loggers. Loggers should be at the size of blocking queue");
      Logger logger = loggers.poll();
      List<LexicalGraph> validGraphs =
          graphToQuery.getGroundedGraphsWithAllEntities(jsonSentence, logger,
              sentCount);

      if (graphToQuery.semanticParseKey.equals("synPars")
          && validGraphs != null) {
        Set<String> goldSynParsSet = new HashSet<>();
        JsonArray goldSynPars = new JsonArray();
        for (LexicalGraph gGraph : validGraphs) {
          String goldSynPar = gGraph.getSyntacticParse();
          if (!goldSynParsSet.contains(goldSynPar)) {
            JsonObject synParseObject = new JsonObject();
            synParseObject.addProperty("score", gGraph.getScore());
            synParseObject.addProperty("synPar", goldSynPar);
            goldSynPars.add(synParseObject);
            goldSynParsSet.add(goldSynPar);
          }
        }
        jsonSentence.add("goldSynPars", goldSynPars);
        if (goldSynParsSet.size() > 0) {
          logger.info("Valid Gold Parses: " + jsonSentence.toString());
        }
      }
      loggers.add(logger);
    }
  }

  private boolean setContainsDecimal(Set<String> results, Long integerAnswer) {
    if (results == null || integerAnswer == null) {
      return false;
    }
    for (String resultString : results) {
      if (!resultString.contains("integer")
          && !resultString.contains("decimal")) {
        return false;
      }
      String integerString =
          Splitter.on("^^").split(resultString).iterator().next();
      Long predictedInteger = convertToInteger(integerString);
      // Integer value need not be exact but close enough
      if (predictedInteger / (integerAnswer + 0.0) >= 6 / 7.0
          && predictedInteger / (integerAnswer + 0.0) <= 7 / 6.0) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, Long> numbers = ImmutableMap
      .<String, Long>builder().put("one", 1L).put("two", 2L).put("three", 3L)
      .put("four", 4L).put("five", 5L).put("six", 6L).put("seven", 7L)
      .put("eight", 8L).put("nine", 9L).build();

  private static Long convertToInteger(String number) {
    try {
      number = number.toLowerCase();
      int product = 1;
      if (number.contains("million")) {
        product *= 1000000;
      }
      if (number.contains("billion")) {
        product *= 1000000000;
      }
      if (number.contains("thousand")) {
        product *= 1000;
      }
      if (number.contains("hundred")) {
        product *= 1000;
      }
      if (numbers.containsKey(number)) {
        return numbers.get(number);
      }
      number = number.replaceAll("[^0-9\\.]", "");
      if (number.equals("")) {
        return null;
      }
      return new Long(Math.round(Double.parseDouble(number)) * product);
    } catch (Exception e) {
      return null;
    }
  }

  private void supervisedTraining(JsonObject jsonSentence, Logger logger,
      boolean debugEnabled) {
    int nbestParses = nbestTrainSyntacticParses;
    String sentence = jsonSentence.get("sentence").getAsString();
    logger.debug("Sentence: " + sentence);
    boolean hasGoldQuery =
        jsonSentence.has("sparqlQuery") || jsonSentence.has("targetValue")
            || jsonSentence.has(SentenceKeys.ANSWER_F1)
            || jsonSentence.has("answerSubset") || jsonSentence.has("answer");
    logger.debug("Supervised Example");
    if (!hasGoldQuery) {
      return;
    }

    Map<String, LinkedHashSet<String>> goldResults = null;
    String goldQuery = null;
    if (jsonSentence.has(SentenceKeys.SPARQL_QUERY)) {
      Map<String, LinkedHashSet<String>> dummyGoldResults = new HashMap<>();
      dummyGoldResults.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>());
      goldQuery = jsonSentence.get(SentenceKeys.SPARQL_QUERY).getAsString();
      logger.info("Gold Query : " + goldQuery);
      Map<String, LinkedHashSet<String>> results =
          rdfGraphTools.runQueryHttp(goldQuery);
      Pair<Set<String>, Set<String>> cleanedGoldResults =
          RdfGraphTools.getCleanedResults(dummyGoldResults, results);
      LinkedHashSet<String> goldAnswers =
          new LinkedHashSet<>(cleanedGoldResults.getRight());
      goldResults = new HashMap<>();
      goldResults.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>(
          goldAnswers));

      // If there is no gold answer, return.
      if (goldAnswers == null
          || goldAnswers.size() == 0
          || (goldAnswers.size() == 1 && (goldAnswers.iterator().next()
              .equals("0") || goldAnswers.iterator().next().equals("")))) {
        logger.info("Gold Results empty. Ignoring this sentence");
        return;
      }
    } else if (jsonSentence.has(SentenceKeys.TARGET_VALUE)) {
      String goldAnswersString =
          jsonSentence.get(SentenceKeys.TARGET_VALUE).getAsString();
      Pattern goldAnswerPattern =
          Pattern.compile("\\(description \"?([^\\)\"]+)\"?\\)");
      Matcher matcher = goldAnswerPattern.matcher(goldAnswersString);
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      while (matcher.find()) {
        goldAnswers.add(matcher.group(1));
      }
      goldResults = new HashMap<>();
      goldResults.put(SentenceKeys.TARGET_VALUE, goldAnswers);

      // If there is no gold answer, return.
      if (goldAnswers == null
          || goldAnswers.size() == 0
          || (goldAnswers.size() == 1 && (goldAnswers.iterator().next()
              .equals("0") || goldAnswers.iterator().next().equals("")))) {
        logger.info("Gold Results empty. Ignoring this sentence");
        return;
      }
    } else if (jsonSentence.has(SentenceKeys.ANSWER_F1)) {
      JsonArray goldAnswersArray =
          jsonSentence.get(SentenceKeys.ANSWER_F1).getAsJsonArray();
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      goldAnswersArray.forEach(answer -> goldAnswers.add(answer.getAsString()));
      goldResults = new HashMap<>();
      goldResults.put(SentenceKeys.TARGET_VALUE, goldAnswers);
    } else if (jsonSentence.has("answer")) {
      JsonArray goldAnswersArray = jsonSentence.get("answer").getAsJsonArray();
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      goldAnswersArray.forEach(answer -> goldAnswers.add(answer.getAsString()));
      goldResults = new HashMap<>();
      goldResults.put("answer", goldAnswers);
    } else if (jsonSentence.has("answerSubset")) {
      JsonArray goldAnswersArray =
          jsonSentence.get("answerSubset").getAsJsonArray();
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      goldAnswersArray.forEach(answer -> goldAnswers.add(answer.getAsString()));
      goldResults = new HashMap<>();
      goldResults.put("answerSubset", goldAnswers);
    }
    logger.info("Gold Results : " + goldResults);

    int totalForestSize = 0;
    while (true) {
      double currentBatchParaphraseScore = 0;
      List<JsonObject> forest = new ArrayList<>();
      if (jsonSentence.has(SentenceKeys.FOREST)) {
        JsonArray jsonForrest =
            jsonSentence.get(SentenceKeys.FOREST).getAsJsonArray();
        List<JsonObject> inputForestArray = new ArrayList<>();
        jsonForrest.forEach(x -> inputForestArray.add(x.getAsJsonObject()));

        // Sort the entries on paraphrase scores if paraphrase scores are
        // available.
        inputForestArray
            .sort(Comparator.comparing(x -> x
                .has(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE) ? -1
                * x.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE).getAsDouble()
                : 0));
        for (int i = totalForestSize; i < inputForestArray.size(); i++) {
          JsonObject element = inputForestArray.get(i);

          // Consider the paraphrases in a graded fashion. First consider the
          // batch with the highest
          // score, next the second highest and so on.
          if (paraphraseClassifierScoreFlag
              && element.has(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)) {
            if (forest.size() == 0) {
              currentBatchParaphraseScore =
                  element.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)
                      .getAsDouble();
            } else {
              double currentParaphraseScore =
                  element.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)
                      .getAsDouble();
              if (currentParaphraseScore < currentBatchParaphraseScore)
                break;
            }
          }
          forest.add(element);
          totalForestSize++;
          if (totalForestSize >= forrestSize)
            break;
        }
      } else {
        forest.add(jsonSentence);
      }

      // Get ungrounded graphs.
      List<LexicalGraph> uGraphs = Lists.newArrayList();
      for (JsonObject element : forest) {
        // Add graphs from syntactic parse/already given semantic parses.
        if (!addOnlyBagOfWordsGraph) {
          uGraphs.addAll(graphCreator.buildUngroundedGraph(element,
              semanticParseKey, nbestParses, logger));
        }

        // Add a bag-of-word graph.
        if (addOnlyBagOfWordsGraph || addBagOfWordsGraph) {
          uGraphs.addAll(graphCreator.getBagOfWordsUngroundedGraph(element));
        }
      }

      if (uGraphs.size() < 1) {
        logger.debug("No uGraphs");
        return;
      }

      // Get grounded Graphs
      List<LexicalGraph> gGraphs = Lists.newArrayList();
      List<LexicalGraph> filteredGraphs = Lists.newArrayList();
      for (LexicalGraph uGraph : uGraphs) {
        if (debugEnabled) {
          try {
            logger.debug(uGraph);
          } catch (Exception e) {
            // pass.
          }
        }
        if (uGraph.getEdges().size() == 0) {
          logger.debug("Graph has NO edges. Discard ");
          continue;
        }
        List<LexicalGraph> currentGroundedGraphs =
            graphCreator.createGroundedGraph(uGraph, nbestEdges, nbestGraphs,
                useEntityTypes, useKB, groundFreeVariables,
                groundEntityVariableEdges, groundEntityEntityEdges,
                useEmtpyTypes, ignoreTypes, false);

        gGraphs.addAll(currentGroundedGraphs);
        Collections.sort(gGraphs);
        gGraphs =
            gGraphs.size() < nbestGraphs ? gGraphs : gGraphs.subList(0,
                nbestGraphs);


        if (useGoldRelations) {
          if (jsonSentence.get(SentenceKeys.GOLD_MID) == null)
            continue;
          String goldMid =
              jsonSentence.get(SentenceKeys.GOLD_MID).getAsString();
          HashSet<LexicalItem> mainEntityNodes = uGraph.getMidNode(goldMid);
          if (mainEntityNodes == null || mainEntityNodes.size() == 0)
            continue;
          HashSet<LexicalItem> questionNodes = uGraph.getQuestionNode();
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

          if (jsonSentence.get(SentenceKeys.GOLD_RELATIONS) == null)
            continue;
          for (JsonElement goldRelation : jsonSentence.get(
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

          List<LexicalGraph> filteredGroundedGraphs =
              graphCreator.createGroundedGraph(uGraph, null,
                  edgeGroundingConstraints, Sets.newHashSet(goldNode),
                  nbestEdges, 10000, useEntityTypes, useKB,
                  groundFreeVariables, groundEntityVariableEdges,
                  groundEntityEntityEdges, useEmtpyTypes, ignoreTypes, false);
          filteredGraphs.addAll(filteredGroundedGraphs);
          Collections.sort(filteredGraphs);
        }
      }

      logger.info("Total number of grounded graphs: " + gGraphs.size());
      LexicalGraph predictedGraph = getPredictedGraph(gGraphs, false);

      LexicalGraph goldGraph = null;
      if (useGoldRelations) {
        logger.info("Using gold annotations for training. Total gold graphs: "
            + filteredGraphs.size());
        goldGraph = getGoldGraph(jsonSentence, filteredGraphs, goldResults);
      } else {
        goldGraph = getGoldGraph(jsonSentence, gGraphs, goldResults);
      }

      List<Feature> predGraphFeatures =
          predictedGraph != null ? predictedGraph.getFeatures()
              : new ArrayList<>();
      List<Feature> goldGraphFeatures =
          goldGraph != null ? goldGraph.getFeatures() : new ArrayList<>();

      if (predictedGraph != null && goldGraph != null) {
        String query =
            GraphToSparqlConverter.convertGroundedGraph(predictedGraph, schema,
                kbGraphUri, 30);
        logger.debug("Predicted query: " + query);
        Map<String, LinkedHashSet<String>> predResults =
            rdfGraphTools.runQueryHttp(query);

        goldQuery =
            GraphToSparqlConverter.convertGroundedGraph(goldGraph, schema,
                kbGraphUri, 30);
        logger.debug("Gold query: " + goldQuery);
        Map<String, LinkedHashSet<String>> goldPredResults =
            rdfGraphTools.runQueryHttp(goldQuery);

        logger.debug("Predicted Results: " + predResults);
        logger.debug("Gold Prediction Results: " + goldPredResults);
        logger.debug("Gold Results: " + goldResults);

        logger.debug("Predicted graph is correct with F1: "
            + RdfGraphTools.getPointWiseF1(goldResults, predResults));
        logger.debug("Predicted Gold graph is correct with F1: "
            + RdfGraphTools.getPointWiseF1(goldResults, goldPredResults));
      }

      if (goldGraph != null) {
        logger.debug("Sentence: " + sentence);
        logger.debug("Predicted Graph: " + predictedGraph);
        logger.debug("Predicted Graph Features: ");
        learningModel.printFeatureWeights(predGraphFeatures, logger);

        logger.debug("Gold Graph: " + goldGraph);
        logger.debug("Gold Graph Features: ");
        learningModel.printFeatureWeights(goldGraphFeatures, logger);

        if (predictedGraph != null)
          logger.debug("Predicted before update: " + predictedGraph.getScore());
        logger.debug("Gold before update: " + goldGraph.getScore());

        learningModel.updateWeightVector(1,
            Lists.newArrayList(goldGraphFeatures), 1,
            Lists.newArrayList(predGraphFeatures));

        if (predictedGraph != null) {
          predictedGraph.setScore(learningModel
              .getScoreTraining(predGraphFeatures));
          logger.debug("Predicted after update: " + predictedGraph.getScore());
        }

        goldGraph.setScore(learningModel.getScoreTraining(goldGraphFeatures));
        logger.debug("Gold after update: " + goldGraph.getScore());
      } else {
        logger.debug("No Gold Graph Found! ");
      }

      // Consider the next set of paraphrases only if
      // paraphraseClassifierScoreFlag is set, and a
      // gold graph is not found.
      if (!jsonSentence.has(SentenceKeys.FOREST)
          || !paraphraseClassifierScoreFlag || goldGraph != null) {
        break;
      }
      if (totalForestSize >= forrestSize)
        break;

      if (jsonSentence.has(SentenceKeys.FOREST)
          && totalForestSize >= jsonSentence.get(SentenceKeys.FOREST)
              .getAsJsonArray().size())
        break;

      logger.info("Considering next set of paraphrases: ");
    }
  }

  public LexicalGraph getPredictedGraph(List<LexicalGraph> graphs,
      boolean testing) {
    if (graphs == null || graphs.size() == 0)
      return null;

    if (!validQueryFlag && !useAnswerTypeQuestionWordFlag)
      return graphs.get(0).copy();

    double firstBestGraphScore = graphs.get(0).getScore();
    ArrayList<Feature> validQueryFeature =
        Lists.newArrayList(new ValidQueryFeature(true));
    double validQueryScore =
        testing ? learningModel.getScoreTesting(validQueryFeature)
            : learningModel.getScoreTraining(validQueryFeature);

    LexicalGraph bestGraphSoFar = null;
    Double bestGraphSoFarScore = -1000000.0;
    for (LexicalGraph gGraph : graphs) {
      LexicalGraph gGraphCopy = gGraph.copy();
      // If maximum estimate of the score is less than the first best graph
      // score.
      if (!useAnswerTypeQuestionWordFlag
          && gGraphCopy.getScore() + validQueryScore < firstBestGraphScore) {
        return gGraphCopy;
      }

      String query =
          GraphToSparqlConverter.convertGroundedGraph(gGraphCopy, schema,
              kbGraphUri, 30);
      Map<String, LinkedHashSet<String>> predResults =
          rdfGraphTools.runQueryHttp(query);

      String targetVar = null;
      if (predResults != null) {
        Set<String> keys = predResults.keySet();
        for (String key : keys) {
          if (!key.contains("name")) {
            targetVar = key;
            break;
          }
        }
      }

      // adding the feature if the query produces any answer
      if ((predResults == null || predResults.get(targetVar) == null || predResults
          .get(targetVar).size() == 0)
          || (predResults.size() == 1 && predResults.get(targetVar).iterator()
              .next().startsWith("0^^"))) {
        // pass.
      } else {
        // if the query produces answer, update its score
        ValidQueryFeature feat = new ValidQueryFeature(true);
        gGraphCopy.addFeature(feat);
        gGraphCopy.setScore(gGraphCopy.getScore() + validQueryScore);
        if (!useAnswerTypeQuestionWordFlag) {
          return gGraphCopy;
        }
      }

      if (useAnswerTypeQuestionWordFlag) {
        String answerType = getMostFrequentAnswerType(predResults);
        HashSet<LexicalItem> qNodes =
            gGraphCopy.getParallelGraph().getQuestionNode();
        if (answerType != null && qNodes != null && qNodes.size() > 0) {
          for (LexicalItem qNode : qNodes) {
            TreeSet<Type<LexicalItem>> qTypes =
                gGraphCopy.getParallelGraph().getTypes(qNode);
            if (qTypes != null) {
              for (Type<LexicalItem> qType : qTypes) {
                AnswerTypeQuestionWordFeature answerTypeFeature =
                    new AnswerTypeQuestionWordFeature(Lists.newArrayList(
                        answerType, qType.getModifierNode().getLemma()), 1.0);
                Double featureWeight =
                    testing ? learningModel.getScoreTesting(Lists
                        .newArrayList(answerTypeFeature))
                        : learningModel.getScoreTraining(Lists
                            .newArrayList(answerTypeFeature));
                gGraphCopy.addFeature(answerTypeFeature);
                gGraphCopy.setScore(gGraphCopy.getScore() + featureWeight);
              }
            }
          }
        }
      }

      if (bestGraphSoFarScore < gGraphCopy.getScore()) {
        bestGraphSoFar = gGraphCopy;
        bestGraphSoFarScore = gGraphCopy.getScore();
      }
    }
    return bestGraphSoFar;
  }

  public LexicalGraph getGoldGraph(JsonObject jsonSentence,
      List<LexicalGraph> graphs, Map<String, LinkedHashSet<String>> goldResults) {
    String sentence = jsonSentence.get(SentenceKeys.SENTENCE_KEY).getAsString();

    if (graphs == null || graphs.size() == 0)
      return null;

    LexicalGraph bestGraphSoFar = null;
    double bestSoFar = 0.0000005;

    for (LexicalGraph gGraph : graphs) {
      if (goldGraphsMap != null && goldGraphsMap.containsKey(sentence)) {
        // If gold graphs are already given.
        Set<LexicalGraph> goldGraphs = goldGraphsMap.get(sentence).getLeft();
        if (goldGraphs.contains(gGraph)) {
          System.err.println("Gold graph found: " + sentence);
          bestSoFar = goldGraphsMap.get(sentence).getRight();
          bestGraphSoFar = gGraph;
          break;
        }
      } else {
        String query =
            GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
                kbGraphUri, 30);
        Map<String, LinkedHashSet<String>> predResults =
            rdfGraphTools.runQueryHttp(query);

        double pointWiseF1 =
            RdfGraphTools.getPointWiseF1(goldResults, predResults);

        if (bestSoFar < pointWiseF1) {
          bestSoFar = pointWiseF1;
          bestGraphSoFar = gGraph;
        }

        if (pointWiseF1 >= 0.8) {
          break;
        }
      }
    }

    if (bestGraphSoFar != null) {
      LexicalGraph returnGraph = bestGraphSoFar.copy();
      if (validQueryFlag) {
        ValidQueryFeature feat = new ValidQueryFeature(true);
        returnGraph.addFeature(feat);
        returnGraph.setScore(returnGraph.getScore()
            + learningModel.getScoreTraining(Lists.newArrayList(feat)));
      }

      if (useAnswerTypeQuestionWordFlag) {
        String query =
            GraphToSparqlConverter.convertGroundedGraph(returnGraph, schema,
                kbGraphUri, 30);
        Map<String, LinkedHashSet<String>> queryResults =
            rdfGraphTools.runQueryHttp(query);
        String answerType = getMostFrequentAnswerType(queryResults);

        HashSet<LexicalItem> qNodes =
            returnGraph.getParallelGraph().getQuestionNode();
        if (answerType != null && qNodes != null && qNodes.size() > 0) {
          for (LexicalItem qNode : qNodes) {
            TreeSet<Type<LexicalItem>> qTypes =
                returnGraph.getParallelGraph().getTypes(qNode);
            if (qTypes != null) {
              for (Type<LexicalItem> qType : qTypes) {
                AnswerTypeQuestionWordFeature answerTypeFeature =
                    new AnswerTypeQuestionWordFeature(Lists.newArrayList(
                        answerType, qType.getModifierNode().getLemma()), 1.0);
                returnGraph.addFeature(answerTypeFeature);
                returnGraph.setScore(returnGraph.getScore()
                    + learningModel.getScoreTraining(Lists
                        .newArrayList(answerTypeFeature)));
              }
            }
          }
        }
      }

      return returnGraph;
    }
    System.err.println("Gold graph NOT found: " + sentence);
    return null;
  }

  public static String getQuestionWord(List<LexicalItem> wordNodes) {
    int i = 0;
    for (LexicalItem leaf : wordNodes) {
      if (leaf.getPos() != null && leaf.getPos().startsWith("W")) {
        String qWord = leaf.getWord().toLowerCase();
        if (qWord.equals("what")) {
          return null;
        } else if (qWord.equals("how")) {
          if (wordNodes.size() > i + 1) {
            String nextWord = wordNodes.get(i + 1).getWord().toLowerCase();
            if (nextWord.equals("many")) {
              return "how many";
            } else {
              return "how";
            }
          }
        } else {
          return qWord;
        }
      }
      i += 1;
    }
    return null;
  }

  /**
   * Returns F1-measure of first best parse of the current model evaluated on
   * the given test sentences.
   * 
   * @param testSentences
   * @param logger
   * @param logFile
   * @param debugEnabled
   * @param testingNbestParsesRange
   * @param nthreads
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  public Double testCurrentModel(List<String> testSentences, Logger logger,
      String logFile, boolean debugEnabled,
      List<Integer> testingNbestParsesRange, int nthreads) throws IOException,
      InterruptedException {
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    logger.debug("Testing: =======================================");
    if (testSentences == null || testSentences.size() == 0) {
      logger.debug("No test sentences");
      return 0.0;
    }

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

    Queue<Logger> deadThredsLogs = new ConcurrentLinkedQueue<>();
    List<RollingFileAppender> appenders = new ArrayList<>();
    for (int i = 0; i < nthreads + 2; i++) {
      Logger threadLogger = Logger.getLogger(logFile + ".thread" + i);
      threadLogger.removeAllAppenders();
      threadLogger.setAdditivity(false);
      if (debugEnabled) {
        threadLogger.setLevel(Level.DEBUG);
        RollingFileAppender appender =
            new RollingFileAppender(layout, logFile + ".thread" + i);
        appenders.add(appender);
        appender.setMaxFileSize("100MB");
        threadLogger.addAppender(appender);
        threadLogger.info("#### Testing starts");
      }
      deadThredsLogs.add(threadLogger);
    }

    List<String> firstBestPredictions = Lists.newArrayList();
    int sentCount = 0;
    Map<Integer, Integer> positives = Maps.newConcurrentMap();
    Map<Integer, Integer> negatives = Maps.newConcurrentMap();
    Map<Integer, Integer> firstBestPredictionsMap = Maps.newConcurrentMap();
    Map<Integer, Double> avgF1 = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, Map<Integer, Pair<Set<String>, Set<String>>>> results =
        new ConcurrentHashMap<>();
    Map<Integer, String> sentenceIndexMap = new HashMap<>();


    for (String testSentence : testSentences) {
      JsonObject jsonSentence =
          jsonParser.parse(testSentence).getAsJsonObject();
      sentenceIndexMap.put(sentCount, jsonSentence.get("sentence")
          .getAsString());
      Runnable worker =
          new testCurrentModelSentenceRunnable(this, jsonSentence, sentCount,
              deadThredsLogs, results, positives, negatives,
              firstBestPredictionsMap, avgF1, testingNbestParsesRange);
      threadPool.execute(worker);
      sentCount += 1;
    }
    threadPool.shutdown();

    while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
      logger.debug("Awaiting completion of threads.");
    }
    for (RollingFileAppender appender : appenders) {
      appender.close();
    }

    double totalAvgF1 = 0.0;
    for (Integer key : avgF1.keySet()) {
      totalAvgF1 += avgF1.get(key);
    }
    Double avgF1Meas = totalAvgF1 / sentCount * 100;

    for (Integer key : testingNbestParsesRange) {
      if (positives.containsKey(key) && negatives.containsKey(key)) {
        Integer positive_hits = positives.get(key);
        Integer negative_hits = negatives.get(key);
        int total_hits = sentCount;
        Double precision =
            (positive_hits + 0.0) / (positive_hits + negative_hits) * 100;
        Double recall = (positive_hits + 0.0) / (total_hits) * 100;
        Double fmeas = 2 * precision * recall / (precision + recall);

        if (key == 1) {
          logger
              .info(String
                  .format(
                      "Nbest:%d Positives:%d Negatives:%d Total:%d Prec:%.1f Rec:%.1f F1:%.1f AvgF1:%.1f",
                      key, positive_hits, negative_hits, total_hits, precision,
                      recall, fmeas, avgF1Meas));
        } else {
          logger
              .info(String
                  .format(
                      "Nbest:%d Positives:%d Negatives:%d Total:%d Prec:%.1f Rec:%.1f F1:%.1f",
                      key, positive_hits, negative_hits, total_hits, precision,
                      recall, fmeas));
        }
      }
    }

    // Write answers to file in Berant's format.
    Map<Integer, BufferedWriter> bwMap = new HashMap<>();
    for (Integer nBest : testingNbestParsesRange) {
      BufferedWriter bw =
          new BufferedWriter(new FileWriter(String.format(
              "%s.%dbest.answers.txt", logFile, nBest)));
      bwMap.put(nBest, bw);
    }

    Gson gson = new Gson();
    for (int i = 0; i < sentCount; i++) {
      Map<Integer, Pair<Set<String>, Set<String>>> nbestCurrentResults =
          results.get(i);
      for (int nBest : testingNbestParsesRange) {
        Pair<Set<String>, Set<String>> currentResults =
            nbestCurrentResults.get(nBest);
        if (currentResults != null) {
          if (currentResults.getLeft().size() != 0) {
            bwMap.get(nBest).write(
                String.format("%s\t%s\t%s\n", sentenceIndexMap.get(i),
                    gson.toJson(currentResults.getLeft()),
                    gson.toJson(currentResults.getRight())));
          } else {
            bwMap.get(nBest).write(
                String.format("%s\t%s\t%s\n", sentenceIndexMap.get(i), "[]",
                    gson.toJson(currentResults.getRight())));
          }
        } else {
          bwMap.get(nBest).write(
              String.format("%s\t[]\t[]\n", sentenceIndexMap.get(i)));
        }
      }
    }

    for (int nBest : testingNbestParsesRange) {
      bwMap.get(nBest).close();
    }

    List<Integer> keys = Lists.newArrayList(firstBestPredictionsMap.keySet());
    Collections.sort(keys);

    for (Integer key : keys) {
      firstBestPredictions.add(firstBestPredictionsMap.get(key).toString());
    }

    logger.info("First Best Predictions");
    logger.info(Joiner.on(" ").join(firstBestPredictions));
    return avgF1Meas;
  }

  public static class testCurrentModelSentenceRunnable implements Runnable {
    private JsonObject jsonSentence;
    GraphToQueryTraining graphToQuery;
    int sentCount;
    Queue<Logger> logs;
    Map<Integer, Integer> positives;
    Map<Integer, Integer> negatives;
    Map<Integer, Integer> firstBestMap;
    Map<Integer, Double> avgF1;
    Map<Integer, Map<Integer, Pair<Set<String>, Set<String>>>> results;
    List<Integer> testingNbestParsesRange;

    public testCurrentModelSentenceRunnable(GraphToQueryTraining graphToQuery,
        JsonObject jsonSentence, int sentCount, Queue<Logger> logs,
        Map<Integer, Map<Integer, Pair<Set<String>, Set<String>>>> results,
        Map<Integer, Integer> positives, Map<Integer, Integer> negatives,
        Map<Integer, Integer> firstBestMap, Map<Integer, Double> avgF1,
        List<Integer> testingNbestParsesRange) {
      this.jsonSentence = jsonSentence;
      this.graphToQuery = graphToQuery;
      this.sentCount = sentCount;
      this.logs = logs;

      this.results = results;
      this.positives = positives;
      this.negatives = negatives;
      this.firstBestMap = firstBestMap;
      this.avgF1 = avgF1;
      this.testingNbestParsesRange = testingNbestParsesRange;
    }

    @Override
    public void run() {
      Preconditions
          .checkArgument(
              logs.size() > 0,
              "Insufficient number of loggers. Loggers should be at the size of blocking queue");
      Logger logger = logs.poll();
      graphToQuery.testCurrentModelSentence(jsonSentence, logger, sentCount,
          results, positives, negatives, firstBestMap, avgF1,
          testingNbestParsesRange);
      logs.add(logger);
    }
  }

  private void testCurrentModelSentence(JsonObject jsonSentence, Logger logger,
      int sentCount,
      Map<Integer, Map<Integer, Pair<Set<String>, Set<String>>>> results,
      Map<Integer, Integer> positives, Map<Integer, Integer> negatives,
      Map<Integer, Integer> firstBestMap, Map<Integer, Double> avgF1,
      List<Integer> testingNbestParsesRange) {
    boolean debugEnabled = logger.isDebugEnabled();
    boolean foundAnswer = false;
    Preconditions
        .checkArgument(
            jsonSentence.has("sparqlQuery") || jsonSentence.has("targetValue")
                || jsonSentence.has(SentenceKeys.ANSWER_F1)
                || jsonSentence.has("answerSubset")
                || jsonSentence
                    .has("answer"),
        "Test sentence should either have a gold query or targetValue or answer values");
    String sentence = jsonSentence.get("sentence").getAsString();

    logger.info("Sentence " + sentCount + ": " + sentence);
    Map<String, LinkedHashSet<String>> goldResults = null;
    String goldQuery = null;
    if (jsonSentence.has("sparqlQuery")) {
      Map<String, LinkedHashSet<String>> dummyGoldResults = new HashMap<>();
      dummyGoldResults.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>());
      goldQuery = jsonSentence.get(SentenceKeys.SPARQL_QUERY).getAsString();
      logger.info("Gold Query : " + goldQuery);

      // Get cleaned results.
      Map<String, LinkedHashSet<String>> queryResults =
          rdfGraphTools.runQueryHttp(goldQuery);
      Pair<Set<String>, Set<String>> cleanedGoldResults =
          RdfGraphTools.getCleanedResults(dummyGoldResults, queryResults);
      LinkedHashSet<String> goldAnswers =
          new LinkedHashSet<>(cleanedGoldResults.getRight());
      goldResults = new HashMap<>();
      goldResults.put(SentenceKeys.TARGET_VALUE, new LinkedHashSet<>(
          goldAnswers));
      logger.info("Gold Results : " + goldResults);

      // If there is no gold answer, return.
      if (goldAnswers == null
          || goldAnswers.size() == 0
          || (goldAnswers.size() == 1 && (goldAnswers.iterator().next()
              .equals("0") || goldAnswers.iterator().next().equals("")))) {
        results.put(sentCount, new HashMap<>());
        avgF1.put(sentCount, 1.0);
        for (Integer nthBest : testingNbestParsesRange) {
          results.get(sentCount)
              .put(nthBest, Pair.of(goldAnswers, goldAnswers));
          positives.put(nthBest, positives.getOrDefault(nthBest, 0) + 1);
        }
        logger.info("Gold Results empty. Ignoring this sentence");
        return;
      }
    } else if (jsonSentence.has("targetValue")) {
      String goldAnswersString = jsonSentence.get("targetValue").getAsString();
      Pattern goldAnswerPattern =
          Pattern.compile("\\(description \"?([^\\)\"]+)\"?\\)");
      Matcher matcher = goldAnswerPattern.matcher(goldAnswersString);
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      while (matcher.find()) {
        goldAnswers.add(matcher.group(1));
      }
      goldResults = new HashMap<>();
      goldResults.put("targetValue", goldAnswers);

      // If there is no gold answer, return.
      if (goldAnswers == null
          || goldAnswers.size() == 0
          || (goldAnswers.size() == 1 && (goldAnswers.iterator().next()
              .equals("0") || goldAnswers.iterator().next().equals("")))) {
        results.put(sentCount, new HashMap<>());
        avgF1.put(sentCount, 1.0);
        for (Integer nthBest : testingNbestParsesRange) {
          results.get(sentCount)
              .put(nthBest, Pair.of(goldAnswers, goldAnswers));
          positives.put(nthBest, positives.getOrDefault(nthBest, 0) + 1);
        }
        logger.info("Gold Results empty. Ignoring this sentence");
        return;
      }
    } else if (jsonSentence.has(SentenceKeys.ANSWER_F1)) {
      JsonArray goldAnswersArray =
          jsonSentence.get(SentenceKeys.ANSWER_F1).getAsJsonArray();
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      goldAnswersArray.forEach(answer -> goldAnswers.add(answer.getAsString()));
      goldResults = new HashMap<>();
      goldResults.put(SentenceKeys.TARGET_VALUE, goldAnswers);

      // If there is no gold answer, return.
      if (goldAnswers == null || goldAnswers.size() == 0
          || (goldAnswers.size() == 1
              && (goldAnswers.iterator().next().equals("0")
                  || goldAnswers.iterator().next().equals("")))) {
        results.put(sentCount, new HashMap<>());
        avgF1.put(sentCount, 1.0);
        for (Integer nthBest : testingNbestParsesRange) {
          results.get(sentCount).put(nthBest,
              Pair.of(goldAnswers, goldAnswers));
          positives.put(nthBest, positives.getOrDefault(nthBest, 0) + 1);
        }
        logger.info("Gold Results empty. Ignoring this sentence");
        return;
      }
    } else if (jsonSentence.has("answer")) {
      JsonArray goldAnswersArray = jsonSentence.get("answer").getAsJsonArray();
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      goldAnswersArray.forEach(answer -> goldAnswers.add(answer.getAsString()));
      goldResults = new HashMap<>();
      goldResults.put("answer", goldAnswers);
    } else if (jsonSentence.has("answerSubset")) {
      JsonArray goldAnswersArray =
          jsonSentence.get("answerSubset").getAsJsonArray();
      LinkedHashSet<String> goldAnswers = new LinkedHashSet<>();
      goldAnswersArray.forEach(answer -> goldAnswers.add(answer.getAsString()));
      goldResults = new HashMap<>();
      goldResults.put("answerSubset", goldAnswers);
    }

    int totalForestSize = 0;
    while (true) {
      double currentBatchParaphraseScore = 0;
      List<JsonObject> forest = new ArrayList<>();
      if (jsonSentence.has(SentenceKeys.FOREST)) {
        JsonArray jsonForrest =
            jsonSentence.get(SentenceKeys.FOREST).getAsJsonArray();
        List<JsonObject> inputForestArray = new ArrayList<>();
        jsonForrest.forEach(x -> inputForestArray.add(x.getAsJsonObject()));

        // Sort the entries on paraphrase scores if paraphrase scores are
        // available.
        inputForestArray
            .sort(Comparator.comparing(x -> x
                .has(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE) ? -1
                * x.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE).getAsDouble()
                : 0));
        for (int i = totalForestSize; i < inputForestArray.size(); i++) {
          JsonObject element = inputForestArray.get(i);

          // Consider the paraphrases in a graded fashion. First consider the
          // batch with the highest
          // score, next the second highest and so on.
          if (paraphraseClassifierScoreFlag
              && element.has(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)) {
            if (forest.size() == 0) {
              currentBatchParaphraseScore =
                  element.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)
                      .getAsDouble();
            } else {
              double currentParaphraseScore =
                  element.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)
                      .getAsDouble();
              if (currentParaphraseScore < currentBatchParaphraseScore)
                break;
            }
          }
          forest.add(element);
          totalForestSize++;
          if (totalForestSize >= forrestSize)
            break;
        }
      } else {
        forest.add(jsonSentence);
      }

      // Get ungrounded graphs
      List<LexicalGraph> uGraphs = Lists.newArrayList();
      for (JsonObject element : forest) {
        // Add graphs from syntactic parse/already given semantic parses.
        if (!addOnlyBagOfWordsGraph) {
          uGraphs.addAll(graphCreator.buildUngroundedGraph(element,
              semanticParseKey, nbestTestSyntacticParses, logger));
        }

        // Add a bag-of-word graph.
        if (addOnlyBagOfWordsGraph || addBagOfWordsGraph) {
          uGraphs.addAll(graphCreator.getBagOfWordsUngroundedGraph(element));
        }

        if (uGraphs.size() < 1) {
          logger.debug("No uGraphs");

          // Syntactic parser mistakes. There are no predicted results.
          firstBestMap.put(sentCount, -1);
          results.put(sentCount, new HashMap<>());
          for (Integer nthBest : testingNbestParsesRange) {
            results.get(sentCount).put(nthBest,
                RdfGraphTools.getCleanedResults(goldResults, null));
          }
          return;
        }
      }

      // Get grounded Graphs
      List<LexicalGraph> gGraphs = Lists.newArrayList();

      for (LexicalGraph uGraph : uGraphs) {
        if (debugEnabled) {
          logger.debug("Ungrounded Graph: " + uGraph);
        }
        if (uGraph.getEdges().size() == 0) {
          logger.debug("Graph has NO edges. Discard ");
          continue;
        }
        List<LexicalGraph> currentGroundedGraphs =
            graphCreator.createGroundedGraph(uGraph, nbestEdges, nbestGraphs,
                useEntityTypes, useKB, groundFreeVariables,
                groundEntityVariableEdges, groundEntityEntityEdges,
                useEmtpyTypes, ignoreTypes, true);
        gGraphs.addAll(currentGroundedGraphs);
        Collections.sort(gGraphs);
        gGraphs =
            gGraphs.size() < nbestGraphs ? gGraphs : gGraphs.subList(0,
                nbestGraphs);
      }

      logger.info("Total number of grounded graphs: " + gGraphs.size());

      List<Pair<LexicalGraph, Map<String, LinkedHashSet<String>>>> gGraphsAndResults =
          Lists.newArrayList();

      // firstBestGraphScore and validQueryFeatureScore are used for speeding up
      // testing.
      double firstBestGraphScore =
          gGraphs.size() > 0 ? gGraphs.get(0).getScore() : 0.0;
      double validQueryFeatureScore =
          learningModel.getScoreTesting(Lists
              .newArrayList(new ValidQueryFeature(true)));
      for (LexicalGraph gGraph : gGraphs) {
        String query =
            GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
                kbGraphUri, 30);
        Map<String, LinkedHashSet<String>> predResults =
            rdfGraphTools.runQueryHttp(query);

        // adding the feature if the query produces any answer
        if (validQueryFlag) {
          String targetVar = null;
          if (predResults != null) {
            Set<String> keys = predResults.keySet();
            for (String key : keys) {
              if (!key.contains("name")) {
                targetVar = key;
                break;
              }
            }
          }

          if ((predResults == null || predResults.get(targetVar) == null || predResults
              .get(targetVar).size() == 0)
              || (predResults.size() == 1 && predResults.get(targetVar)
                  .iterator().next().startsWith("0^^"))) {
            ValidQueryFeature feat = new ValidQueryFeature(false);
            gGraph.addFeature(feat);
          } else {
            // if the query produces answer, update its score
            ValidQueryFeature feat = new ValidQueryFeature(true);
            gGraph.addFeature(feat);
            gGraph.setScore(gGraph.getScore() + validQueryFeatureScore);
          }
        }

        if (useAnswerTypeQuestionWordFlag) {
          String answerType = getMostFrequentAnswerType(predResults);

          HashSet<LexicalItem> qNodes =
              gGraph.getParallelGraph().getQuestionNode();
          if (answerType != null && qNodes != null && qNodes.size() > 0) {
            for (LexicalItem qNode : qNodes) {
              TreeSet<Type<LexicalItem>> qTypes =
                  gGraph.getParallelGraph().getTypes(qNode);
              if (qTypes != null) {
                for (Type<LexicalItem> qType : qTypes) {
                  AnswerTypeQuestionWordFeature answerTypeFeature =
                      new AnswerTypeQuestionWordFeature(Lists.newArrayList(
                          answerType, qType.getModifierNode().getLemma()), 1.0);
                  gGraph.addFeature(answerTypeFeature);
                  gGraph.setScore(gGraph.getScore()
                      + learningModel.getScoreTesting(Lists
                          .newArrayList(answerTypeFeature)));
                }
              }
            }
          }
        }

        gGraphsAndResults.add(Pair.of(gGraph, predResults));
        if (evaluateOnlyTheFirstBest && !useAnswerTypeQuestionWordFlag) {
          if (!validQueryFlag)
            break;

          // Graph with valid query found.
          if (firstBestGraphScore + 0.00005 < gGraph.getScore())
            break;

          // No other graph can score higher than the first graph.
          if (firstBestGraphScore > gGraph.getScore() + validQueryFeatureScore)
            break;
        }
      }

      gGraphsAndResults.sort(Comparator.comparing(x -> x.getLeft()));
      int count = 0;

      Map<String, LinkedHashSet<String>> firstBestPredictions = null;
      Map<String, LinkedHashSet<String>> bestPredictedResults = null;
      for (Pair<LexicalGraph, Map<String, LinkedHashSet<String>>> gGraphPair : gGraphsAndResults) {
        LexicalGraph gGraph = gGraphPair.getLeft();
        Map<String, LinkedHashSet<String>> predResults = gGraphPair.getRight();

        if (count == 0)
          firstBestPredictions = predResults;
        count += 1;

        if (debugEnabled) {
          try {
            logger.debug(gGraph);
          } catch (Exception e) {
            // pass.
          }
        }

        String predQuery =
            GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
                kbGraphUri, 30);
        logger.info("Predicted Query: " + predQuery);
        logger.info("Gold Query: " + goldQuery);

        if (debugEnabled) {
          learningModel
              .printFeatureWeightsTesting(gGraph.getFeatures(), logger);
        }
        logger.info("Predicted Results: " + predResults);
        logger.info("Gold Results: " + goldResults);

        double pointWiseF1 =
            RdfGraphTools.getPointWiseF1(goldResults, predResults);

        if (count == 1) {
          avgF1.put(sentCount, pointWiseF1);
        }

        if (pointWiseF1 > 0.9) {
          logger.info("CORRECT!!");
          foundAnswer = true;
          bestPredictedResults = predResults;
          break;
        } else {
          logger.info("WRONG!!");
        }
      }

      if (foundAnswer && count == 1) {
        firstBestMap.put(sentCount, 1);
      } else if (gGraphs.size() > 0) {
        firstBestMap.put(sentCount, 0);
      } else {
        firstBestMap.put(sentCount, -1);
      }

      results.put(sentCount, new HashMap<>());
      for (Integer nthBest : testingNbestParsesRange) {
        // Store the results.
        if (foundAnswer && count <= nthBest) {
          Integer value =
              positives.containsKey(nthBest) ? positives.get(nthBest) : 0;
          positives.put(nthBest, value + 1);

          // Store the results.
          results.get(sentCount).put(nthBest,
              RdfGraphTools.getCleanedResults(goldResults, bestPredictedResults));
        } else if (gGraphs.size() > 0) {
          Integer value =
              negatives.containsKey(nthBest) ? negatives.get(nthBest) : 0;
          negatives.put(nthBest, value + 1);

          // Store the results.
          results.get(sentCount).put(
              nthBest,
              RdfGraphTools
                  .getCleanedResults(goldResults, firstBestPredictions));
        } else {
          // Store the results.
          results.get(sentCount).put(
              nthBest,
              RdfGraphTools
                  .getCleanedResults(goldResults, firstBestPredictions));
        }
      }

      logger.info("# Total number of Grounded Graphs: " + gGraphs.size());
      // System.out.println("# Total number of Connected Grounded Graphs: "
      // + connectedGraphCount);

      // Consider the next set of paraphrases only if
      // paraphraseClassifierScoreFlag is set, and
      // current paraphrase do not predict an answer.
      if (!jsonSentence.has(SentenceKeys.FOREST)
          || !paraphraseClassifierScoreFlag
          || results.get(sentCount).get(1).getRight().size() > 0) {
        break;
      }

      if (totalForestSize >= forrestSize)
        break;

      if (jsonSentence.has(SentenceKeys.FOREST)
          && totalForestSize >= jsonSentence.get(SentenceKeys.FOREST)
              .getAsJsonArray().size())
        break;

      logger.info("Considering next set of paraphrases: ");
    }
    logger.info("\n###########################\n");
  }

  public StructuredPercepton getLearningModel() {
    return learningModel;
  }

  public void setLearningModel(StructuredPercepton learningModel) {
    this.learningModel = learningModel;
    graphCreator.setLearningModel(learningModel);
  }

  /**
   * @return the POINTWISE_F1_THRESHOLD
   */
  public static double getPointWiseF1Threshold() {
    return POINTWISE_F1_THRESHOLD;
  }

  /**
   * @param value the POINTWISE_F1_THRESHOLD to set
   */
  public static void setPointWiseF1Threshold(double value) {
    POINTWISE_F1_THRESHOLD = value;
  }


  public String getMostFrequentAnswerType(
      Map<String, LinkedHashSet<String>> sparqlQueryResults) {

    if (sparqlQueryResults == null)
      return null;

    // Non named variable
    String mainVar = null;
    for (String var : sparqlQueryResults.keySet()) {
      if (!var.contains("name")) {
        mainVar = var;
        break;
      }
    }

    if (mainVar != null) {
      for (String result : sparqlQueryResults.get(mainVar)) {
        if (result.contains("XMLSchema")) {
          return result.split("XMLSchema#")[1].trim().replace("\">", "");
        } else {
          if (!result.contains("/ns/"))
            continue;
          String mid = result.split("/ns/")[1];
          Set<String> entityTypes = kb.getTypes(mid.trim());

          int freq = 0;
          String mostFrequentType = null;
          for (String entityType : entityTypes) {
            if (mostFrequentTypes.containsKey(entityType)) {
              int curTypeFreq = mostFrequentTypes.get(entityType);
              if (freq < curTypeFreq) {
                freq = curTypeFreq;
                mostFrequentType = entityType;
              }
            }
          }
          return mostFrequentType;
        }
      }
      return null;
    }
    return null;
  }
}
