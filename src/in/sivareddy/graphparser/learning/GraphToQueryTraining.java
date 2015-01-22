package in.sivareddy.graphparser.learning;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.CcgParseTree.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.ValidQueryFeature;
import in.sivareddy.graphparser.util.Graph.Edge;
import in.sivareddy.graphparser.util.Graph.Type;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase;
import in.sivareddy.graphparser.util.KnowledgeBase.Property;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.ml.learning.StructuredPercepton;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GraphToQueryTraining {
  private StructuredPercepton learningModel;
  private Schema schema;
  private KnowledgeBase kb;
  private GroundedLexicon groundedLexicon;
  private GroundedGraphs graphCreator;

  int nbestEdges = 20;
  int nbestGraphs = 100;
  int nbestTrainSyntacticParses = 1;
  int nbestTestSyntacticParses = 1;
  boolean useEntityTypes = true;
  boolean useKB = true;
  boolean groundFreeVariables = false;
  boolean useEmtpyTypes = false;
  boolean ignoreTypes = false;

  boolean validQueryFlag = true;
  String semanticParseKey;

  RdfGraphTools rdfGraphTools;
  List<String> kbGraphUri = null;
  double MARGIN = 30;

  public GraphToQueryTraining(Schema schema, KnowledgeBase kb,
      GroundedLexicon groundedLexicon, CcgAutoLexicon normalCcgAutoLexicon,
      CcgAutoLexicon questionCcgAutoLexicon, String semanticParseKey,
      int nbestTrainSyntacticParses, int nbestTestSyntacticParses,
      int nbestEdges, int nbestGraphs, boolean useSchema, boolean useKB,
      boolean groundFreeVariables, boolean useEmtpyTypes, boolean ignoreTypes,
      StructuredPercepton learningModel, boolean urelGrelFlag,
      boolean urelPartGrelPartFlag, boolean utypeGtypeFlag,
      boolean gtypeGrelFlag, boolean grelGrelFlag, boolean wordGrelPartFlag,
      boolean wordGrelFlag, boolean argGrelPartFlag, boolean argGrelFlag,
      boolean wordBigramGrelPartFlag, boolean stemMatchingFlag,
      boolean mediatorStemGrelPartMatchingFlag,
      boolean argumentStemMatchingFlag,
      boolean argumentStemGrelPartMatchingFlag, boolean graphIsConnectedFlag,
      boolean graphHasEdgeFlag, boolean countNodesFlag,
      boolean edgeNodeCountFlag, boolean useLexiconWeightsRel,
      boolean useLexiconWeightsType, boolean duplicateEdgesFlag,
      boolean validQueryFlag, double initialEdgeWeight,
      double initialTypeWeight, double initialWordWeight,
      double stemFeaturesWeight, RdfGraphTools rdfGraphTools,
      List<String> kbGraphUri) throws IOException {
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};

    this.semanticParseKey = semanticParseKey;

    this.nbestTrainSyntacticParses = nbestTrainSyntacticParses;
    this.nbestTestSyntacticParses = nbestTestSyntacticParses;
    this.nbestEdges = nbestEdges;
    this.nbestGraphs = nbestGraphs;
    this.useEntityTypes = useSchema;
    this.useKB = useKB;
    this.groundFreeVariables = groundFreeVariables;
    this.useEmtpyTypes = useEmtpyTypes;
    this.ignoreTypes = ignoreTypes;

    this.validQueryFlag = validQueryFlag;

    this.learningModel = learningModel;
    this.schema = schema;
    this.kb = kb;
    this.groundedLexicon = groundedLexicon;

    this.rdfGraphTools = rdfGraphTools;
    this.kbGraphUri = kbGraphUri;

    this.graphCreator =
        new GroundedGraphs(this.schema, this.kb, this.groundedLexicon,
            normalCcgAutoLexicon, questionCcgAutoLexicon,
            relationLexicalIdentifiers, relationTypingIdentifiers,
            this.learningModel, urelGrelFlag, urelPartGrelPartFlag,
            utypeGtypeFlag, gtypeGrelFlag, grelGrelFlag, wordGrelPartFlag,
            wordGrelFlag, argGrelPartFlag, argGrelFlag, wordBigramGrelPartFlag,
            stemMatchingFlag, mediatorStemGrelPartMatchingFlag,
            argumentStemMatchingFlag, argumentStemGrelPartMatchingFlag,
            graphIsConnectedFlag, graphHasEdgeFlag, countNodesFlag,
            edgeNodeCountFlag, useLexiconWeightsRel, useLexiconWeightsType,
            duplicateEdgesFlag, initialEdgeWeight, initialTypeWeight,
            initialWordWeight, stemFeaturesWeight);

  }

  JsonParser jsonParser = new JsonParser();

  public void trainFromSentences(List<String> trainingSample, int nthreads,
      String logFile, boolean debugEnabled) throws IOException,
      InterruptedException {
    Logger logger = Logger.getLogger(logFile);
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    logger.setAdditivity(false);
    if (debugEnabled) {
      logger.setLevel(Level.DEBUG);
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
    for (int i = 0; i < nthreads + 2; i++) {
      Logger threadLogger = Logger.getLogger(logFile + ".thread" + i);
      threadLogger.setAdditivity(true);
      if (debugEnabled) {
        threadLogger.setLevel(Level.DEBUG);
        RollingFileAppender appender =
            new RollingFileAppender(layout, logFile + ".thread" + i);
        appender.setMaxFileSize("100MB");
        threadLogger.addAppender(appender);
        threadLogger.info("######## Training Starts");
      }
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

    // Wait until all threads are finished
    while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
      logger.debug("Awaiting completion of threads.");
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
          jsonSentence.has("sparqlQuery") || jsonSentence.has("targetValue");
      if (hasGoldQuery) {
        // supervised training can make use of more number of syntactic
        // parses
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
    List<LexicalGraph> uGraphs =
        graphCreator.buildUngroundedGraph(jsonSentence, semanticParseKey,
            nbestParses, logger);
    if (uGraphs.size() < 1) {
      logger.debug("No uGraphs");
      return;
    }

    Set<LexicalItem> lexicalisedNodes = Sets.newHashSet();
    Set<LexicalItem> uniqueNodes = Sets.newHashSet();
    Property uniqPropery = new Property("UNIQUE");

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
      // wild graphs do not use the entity type of target variable
      List<LexicalGraph> wildGraphs =
          graphCreator.createGroundedGraph(uGraph, Sets.newHashSet(targetNode),
              nbestEdges, nbestGraphs, useEntityTypes, useKB,
              groundFreeVariables, useEmtpyTypes, ignoreTypes, false);
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
      logger.debug("Reranking the top " + nbestPredictedGraphs + " parses");
      for (LexicalGraph pGraph : predGgraphsWild.subList(0,
          nbestPredictedGraphs)) {
        String query =
            GraphToSparqlConverter.convertGroundedGraph(pGraph, targetNode,
                schema, kbGraphUri);
        logger.debug("Predicted graph query: " + query);
        // ResultSet resultSet = rdfGraphTools.runQueryJdbc(query);
        Map<String, LinkedHashSet<String>> resultsMap =
            rdfGraphTools.runQueryHttp(query);
        logger.debug("Predicted Results: " + resultsMap);
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

    // if the first predicted graph results in answer, there is nothing to
    // learn
    LexicalGraph firstPredictedGraph = predGgraphsWild.get(0);
    String query =
        GraphToSparqlConverter.convertGroundedGraph(firstPredictedGraph,
            targetNode, schema, kbGraphUri);
    logger.debug("First predicted graph query: " + query);
    // ResultSet resultSet = rdfGraphTools.runQueryJdbc(query);
    Map<String, LinkedHashSet<String>> resultsMap =
        rdfGraphTools.runQueryHttp(query);
    logger.debug("First Predicted Results: " + resultsMap);
    LinkedHashSet<String> results =
        resultsMap != null && resultsMap.containsKey(targetVar) ? resultsMap
            .get(targetVar) : null;
    if (answerIsDecimal) {
      logger.debug("Answer is decimal: " + integerAnswer + " : " + results);
      if (setContainsDecimal(results, integerAnswer)) {
        logger
            .debug("Decimal answer found: First best graph predicts the answer");
        return;
      }
    } else {
      if (answerIsDate) {
        results = RdfGraphTools.convertDatesToYears(results);
      }
      if (results != null && results.contains(answer)) {
        logger.debug("First best graph predicts the answer");
        // learning even from correct predictions since the model is
        // average perceptron
        Set<Feature> predGraphFeatures = firstPredictedGraph.getFeatures();
        Set<Feature> goldGraphFeatures = firstPredictedGraph.getFeatures();
        learningModel.updateWeightVector(goldGraphFeatures, predGraphFeatures);
        return;
      }
    }

    // constrained graphs which are used to select gold graphs - constrained
    // graphs help in search space reduction so that SPARQL query bottleneck
    // can be removed to some extent - Sadly Sparql querying is still slow.
    List<LexicalGraph> predGgraphsConstrained = Lists.newArrayList();
    for (LexicalGraph uGraph : uGraphs) {
      if (uGraph.getEdges().size() == 0) {
        logger.debug("Graph has NO edges. Discard ");
        continue;
      }
      // constrained graphs are created using entity types
      List<LexicalGraph> constrainedGraphs =
          graphCreator.createGroundedGraph(uGraph, nbestEdges, nbestGraphs,
              useEntityTypes, useKB, groundFreeVariables, useEmtpyTypes,
              ignoreTypes, false);
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
    for (LexicalGraph gGraph : predGgraphsConstrained) {
      query =
          GraphToSparqlConverter.convertGroundedGraph(gGraph, targetNode,
              schema, kbGraphUri);
      logger.debug("Pred constrained query: " + query);
      // resultSet = rdfGraphTools.runQueryJdbc(query);
      resultsMap = rdfGraphTools.runQueryHttp(query);
      logger.debug("Pred results: " + resultsMap);
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
    Collections.sort(gGraphsAndResults,
        new Comparator<Pair<LexicalGraph, LinkedHashSet<String>>>() {
          @Override
          public int compare(Pair<LexicalGraph, LinkedHashSet<String>> o1,
              Pair<LexicalGraph, LinkedHashSet<String>> o2) {
            return o1.getLeft().compareTo(o2.getLeft());
          }
        });

    // Selecting the gold graphs
    List<Pair<Integer, LexicalGraph>> goldGraphs = Lists.newArrayList();
    List<Pair<Integer, LexicalGraph>> goldGraphsPossible = Lists.newArrayList();
    for (Pair<LexicalGraph, LinkedHashSet<String>> gGraphPair : gGraphsAndResults) {
      LexicalGraph gGraph = gGraphPair.getLeft();
      results = gGraphPair.getRight();
      // if (gGraph.getScore() < 0)
      // break;

      if (answerIsDecimal) {
        logger.debug("Decimal answers: " + results);
        if (setContainsDecimal(results, integerAnswer)) {
          goldGraphsPossible.add(Pair.of(results.size(), gGraph));
          logger.debug("Decimal answer found: " + results);
        }
      } else {
        if (answerIsDate) {
          results = RdfGraphTools.convertDatesToYears(results);
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

    // second filtering - the graph should have all entities

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
        query =
            GraphToSparqlConverter.convertGroundedGraph(goldGraph, targetNode,
                schema, kbGraphUri);
        logger.debug("Year query: " + query);
        // resultSet = rdfGraphTools.runQueryJdbc(query);
        resultsMap = rdfGraphTools.runQueryHttp(query);
        logger.debug("Year pred results: " + resultsMap);
        results =
            resultsMap != null && resultsMap.containsKey(targetVar) ? resultsMap
                .get(targetVar) : null;
        results = RdfGraphTools.convertDatesToYears(results);
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

    LexicalGraph predGraph = firstPredictedGraph;

    if (debugEnabled) {
      logger.debug("Predicted Graph: " + predGraph);
      logger.debug("Gold Graphs: " + goldGraphs);
    }

    if (goldGraphs == null || goldGraphs.size() == 0) {
      logger.debug("No gold graphs found");
      return;
    }

    LexicalGraph firstGoldGraph = goldGraphs.get(0).getRight();
    double marginDifference =
        Math.abs(firstGoldGraph.getScore() - firstPredictedGraph.getScore());
    if (marginDifference > MARGIN) {
      logger.debug("Difference is greater than margin. " + marginDifference);
      return;
    }

    Set<Feature> goldGraphFeatures = firstGoldGraph.getFeatures();
    /*-Set<Feature> goldGraphFeatures = Sets.newHashSet();

    for (Pair<Integer, LexicalGraph> goldGraphPair : goldGraphs) {
    	Set<Feature> feats = goldGraphPair.getRight().getFeatures();
    	goldGraphFeatures.addAll(feats);
    }*/
    Set<Feature> predGraphFeatures = predGraph.getFeatures();

    logger.debug("Sentence: " + sentence);

    if (debugEnabled) {
      logger.debug("Gold graph features before update");
      learningModel.printFeatureWeights(goldGraphFeatures, logger);
    }

    if (debugEnabled) {
      logger.debug("Predicted graph features before update");
      learningModel.printFeatureWeights(predGraphFeatures, logger);
    }

    logger.debug("Predicted Before Update: " + predGraph.getScore());
    logger.debug("Gold Before Update: " + firstGoldGraph.getScore());
    learningModel.updateWeightVector(goldGraphFeatures, predGraphFeatures);
    predGraph.setScore(learningModel.getScoreTraining(predGraphFeatures));
    firstGoldGraph.setScore(learningModel.getScoreTraining(goldGraphFeatures));
    logger.debug("Predicted After Update: " + predGraph.getScore());
    logger.debug("Gold After Update: " + firstGoldGraph.getScore());

    if (debugEnabled) {
      logger.debug("Predicted graph features after update");
      learningModel.printFeatureWeights(predGraphFeatures, logger);
    }

    if (debugEnabled) {
      logger.debug("Gold graph features after update");
      learningModel.printFeatureWeights(goldGraphFeatures, logger);
    }

    logger.debug("#############");
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
        jsonSentence.has("sparqlQuery") || jsonSentence.has("targetValue");
    logger.debug("Supervised Example");
    if (!hasGoldQuery) {
      return;
    }

    Map<String, LinkedHashSet<String>> goldResults = null;
    String goldQuery = null;
    if (jsonSentence.has("sparqlQuery")) {
      goldQuery = jsonSentence.get("sparqlQuery").getAsString();
      logger.info("Gold Query : " + goldQuery);
      goldResults = rdfGraphTools.runQueryHttp(goldQuery);
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
    }
    logger.info("Gold Results : " + goldResults);

    // Get ungrounded graphs
    List<LexicalGraph> uGraphs =
        graphCreator.buildUngroundedGraph(jsonSentence, semanticParseKey,
            nbestParses, logger);
    if (uGraphs.size() < 1) {
      logger.debug("No uGraphs");
      return;
    }

    // Get grounded Graphs
    List<LexicalGraph> gGraphs = Lists.newArrayList();
    for (LexicalGraph uGraph : uGraphs) {
      if (debugEnabled) {
        logger.debug(uGraph);
      }
      if (uGraph.getEdges().size() == 0) {
        logger.debug("Graph has NO edges. Discard ");
        continue;
      }
      List<LexicalGraph> currentGroundedGraphs =
          graphCreator.createGroundedGraph(uGraph, nbestEdges, nbestGraphs,
              useEntityTypes, useKB, groundFreeVariables, useEmtpyTypes,
              ignoreTypes, false);

      if (validQueryFlag) {
        for (LexicalGraph gGraph : currentGroundedGraphs) {
          String query =
              GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
                  kbGraphUri);
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
          if (validQueryFlag) {
            if ((predResults == null || predResults.get(targetVar).size() == 0)
                || (predResults.size() == 1 && predResults.get(targetVar)
                    .iterator().next().startsWith("0^^"))) {
              ValidQueryFeature feat = new ValidQueryFeature(false);
              gGraph.addFeature(feat);
            } else {
              // if the query produces answer, update its score
              ValidQueryFeature feat = new ValidQueryFeature(true);
              gGraph.addFeature(feat);
              gGraph.setScore(learningModel.getScoreTesting(gGraph
                  .getFeatures()));
            }
          }
        }
      }

      gGraphs.addAll(currentGroundedGraphs);
      Collections.sort(gGraphs);
      gGraphs =
          gGraphs.size() < nbestGraphs ? gGraphs : gGraphs.subList(0,
              nbestGraphs);
    }

    logger.info("Total number of grounded graphs: " + gGraphs.size());

    Set<Feature> predGraphFeatures = null;
    Set<Feature> goldGraphFeatures = null;
    int count = 0;
    for (LexicalGraph gGraph : gGraphs) {
      count += 1;
      /*-if (count > 1) {
      	break;
      }*/
      if (debugEnabled) {
        logger.debug(gGraph);
        // log.debug("Connected: " + gGraph.isConnected() + "\n");
        logger.debug("Grounded graph features: ");
        learningModel.printFeatureWeights(gGraph.getFeatures(), logger);
      }

      if (count == 1) {
        predGraphFeatures = gGraph.getFeatures();
      }

      String query =
          GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
              kbGraphUri);
      logger.debug("Predicted query: " + query);
      logger.debug("Gold query: " + goldQuery);
      Map<String, LinkedHashSet<String>> predResults =
          rdfGraphTools.runQueryHttp(query);
      logger.debug("Predicted Results: " + predResults);
      logger.debug("Gold Results: " + goldResults);
      boolean areEqual = RdfGraphTools.equalResults(goldResults, predResults);
      if (areEqual) {
        logger.debug("Sentence: " + sentence);
        logger.debug("Predicted Graph Features: ");
        learningModel.printFeatureWeights(predGraphFeatures, logger);

        goldGraphFeatures = gGraph.getFeatures();
        logger.debug("Gold Graph Features: ");
        learningModel.printFeatureWeights(goldGraphFeatures, logger);

        logger.debug("Before Update: " + gGraph.getScore());
        learningModel.updateWeightVector(goldGraphFeatures, predGraphFeatures);
        gGraph.setScore(learningModel.getScoreTraining(goldGraphFeatures));
        logger.debug("After Update: " + gGraph.getScore());
        logger.debug("CORRECT!!");
        break;
      } else {
        logger.debug("WRONG!!");
      }
    }
  }

  public void testCurrentModel(List<String> testSentences, Logger logger,
      String logFile, boolean debugEnabled,
      List<Integer> testingNbestParsesRange, int nthreads) throws IOException,
      InterruptedException {
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    logger.debug("Testing: =======================================");
    if (testSentences == null || testSentences.size() == 0) {
      logger.debug("No test sentences");
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
    for (int i = 0; i < nthreads; i++) {
      Logger threadLogger = Logger.getLogger(logFile + ".thread" + i);
      threadLogger.setAdditivity(false);
      if (debugEnabled) {
        threadLogger.setLevel(Level.DEBUG);
        RollingFileAppender appender =
            new RollingFileAppender(layout, logFile + ".thread" + i);
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

    for (String testSentence : testSentences) {
      JsonObject jsonSentence =
          jsonParser.parse(testSentence).getAsJsonObject();
      Runnable worker =
          new testCurrentModelSentenceRunnable(this, jsonSentence, sentCount,
              deadThredsLogs, positives, negatives, firstBestPredictionsMap,
              testingNbestParsesRange);
      threadPool.execute(worker);
      sentCount += 1;
    }
    threadPool.shutdown();

    while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
      logger.debug("Awaiting completion of threads.");
    }

    for (Integer key : testingNbestParsesRange) {
      if (positives.containsKey(key) && negatives.containsKey(key)) {
        Integer positive_hits = positives.get(key);
        Integer negative_hits = negatives.get(key);
        int total_hits = sentCount;
        Double precision =
            (positive_hits + 0.0) / (positive_hits + negative_hits) * 100;
        Double recall = (positive_hits + 0.0) / (total_hits) * 100;
        Double fmeas = 2 * precision * recall / (precision + recall);
        logger
            .info(String
                .format(
                    "Nbest:%d Positives:%d Negatives:%d Total:%d Prec:%.1f Rec:%.1f Fmeas:%.1f",
                    key, positive_hits, negative_hits, total_hits, precision,
                    recall, fmeas));
      }
    }

    List<Integer> keys = Lists.newArrayList(firstBestPredictionsMap.keySet());
    Collections.sort(keys);

    for (Integer key : keys) {
      firstBestPredictions.add(firstBestPredictionsMap.get(key).toString());
    }

    logger.info("First Best Predictions");
    logger.info(Joiner.on(" ").join(firstBestPredictions));
  }

  public static class testCurrentModelSentenceRunnable implements Runnable {
    private JsonObject jsonSentence;
    GraphToQueryTraining graphToQuery;
    int sentCount;
    Queue<Logger> logs;
    Map<Integer, Integer> positives;
    Map<Integer, Integer> negatives;
    Map<Integer, Integer> firstBestMap;
    List<Integer> testingNbestParsesRange;

    public testCurrentModelSentenceRunnable(GraphToQueryTraining graphToQuery,
        JsonObject jsonSentence, int sentCount, Queue<Logger> logs,
        Map<Integer, Integer> positives, Map<Integer, Integer> negatives,
        Map<Integer, Integer> firstBestMap,
        List<Integer> testingNbestParsesRange) {
      this.jsonSentence = jsonSentence;
      this.graphToQuery = graphToQuery;
      this.sentCount = sentCount;
      this.logs = logs;

      this.positives = positives;
      this.negatives = negatives;
      this.firstBestMap = firstBestMap;
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
          positives, negatives, firstBestMap, testingNbestParsesRange);
      logs.add(logger);
    }
  }

  private void testCurrentModelSentence(JsonObject jsonSentence, Logger logger,
      int sentCount, Map<Integer, Integer> positives,
      Map<Integer, Integer> negatives, Map<Integer, Integer> firstBestMap,
      List<Integer> testingNbestParsesRange) {
    boolean debugEnabled = logger.isDebugEnabled();
    boolean foundAnswer = false;
    Preconditions.checkArgument(
        jsonSentence.has("sparqlQuery") || jsonSentence.has("targetValue"),
        "Test sentence should either have a gold query or targetValue");
    String sentence = jsonSentence.get("sentence").getAsString();

    logger.info("Sentence " + sentCount + ": " + sentence);
    Map<String, LinkedHashSet<String>> goldResults = null;
    String goldQuery = null;
    if (jsonSentence.has("sparqlQuery")) {
      goldQuery = jsonSentence.get("sparqlQuery").getAsString();
      logger.info("Gold Query : " + goldQuery);
      goldResults = rdfGraphTools.runQueryHttp(goldQuery);
      logger.info("Gold Results : " + goldResults);
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
    }

    // Get ungrounded graphs
    List<LexicalGraph> uGraphs =
        graphCreator.buildUngroundedGraph(jsonSentence, semanticParseKey,
            nbestTestSyntacticParses, logger);
    if (uGraphs.size() < 1) {
      logger.debug("No uGraphs");
      // Syntactic Parser mistakes
      firstBestMap.put(sentCount, -1);
      return;
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
              useEntityTypes, useKB, groundFreeVariables, useEmtpyTypes,
              ignoreTypes, true);
      gGraphs.addAll(currentGroundedGraphs);
      Collections.sort(gGraphs);
      gGraphs =
          gGraphs.size() < nbestGraphs ? gGraphs : gGraphs.subList(0,
              nbestGraphs);
    }

    logger.info("Total number of grounded graphs: " + gGraphs.size());

    List<Pair<LexicalGraph, Map<String, LinkedHashSet<String>>>> gGraphsAndResults =
        Lists.newArrayList();
    for (LexicalGraph gGraph : gGraphs) {
      String query =
          GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
              kbGraphUri, ignoreTypes);
      Map<String, LinkedHashSet<String>> predResults =
          rdfGraphTools.runQueryHttp(query);
      gGraphsAndResults.add(Pair.of(gGraph, predResults));

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
      if (validQueryFlag) {
        if ((predResults == null || predResults.get(targetVar).size() == 0)
            || (predResults.size() == 1 && predResults.get(targetVar)
                .iterator().next().startsWith("0^^"))) {
          ValidQueryFeature feat = new ValidQueryFeature(false);
          gGraph.addFeature(feat);
        } else {
          // if the query produces answer, update its score
          ValidQueryFeature feat = new ValidQueryFeature(true);
          gGraph.addFeature(feat);
          gGraph.setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
        }
      }
    }

    Collections
        .sort(
            gGraphsAndResults,
            new Comparator<Pair<LexicalGraph, Map<String, LinkedHashSet<String>>>>() {

              @Override
              public int compare(
                  Pair<LexicalGraph, Map<String, LinkedHashSet<String>>> o1,
                  Pair<LexicalGraph, Map<String, LinkedHashSet<String>>> o2) {
                return o1.getLeft().compareTo(o2.getLeft());
              }
            });

    int count = 0;
    for (Pair<LexicalGraph, Map<String, LinkedHashSet<String>>> gGraphPair : gGraphsAndResults) {
      LexicalGraph gGraph = gGraphPair.getLeft();
      Map<String, LinkedHashSet<String>> predResults = gGraphPair.getRight();

      count += 1;
      /*-if (count > 1) {
      	break;
      }*/

      if (debugEnabled) {
        logger.debug(gGraph);
        // log.debug("Connected: " + gGraph.isConnected() + "\n");
      }

      String predQuery =
          GraphToSparqlConverter.convertGroundedGraph(gGraph, schema,
              kbGraphUri, ignoreTypes);
      logger.info("Predicted Query: " + predQuery);
      logger.info("Gold Query: " + goldQuery);

      if (debugEnabled) {
        learningModel.printFeatureWeightsTesting(gGraph.getFeatures(), logger);
      }
      logger.info("Predicted Results: " + predResults);
      logger.info("Gold Results: " + goldResults);
      boolean areEqual = RdfGraphTools.equalResults(goldResults, predResults);

      if (areEqual) {
        logger.info("CORRECT!!");
        foundAnswer = true;
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

    for (Integer nthBest : testingNbestParsesRange) {
      if (foundAnswer && count <= nthBest) {
        Integer value =
            positives.containsKey(nthBest) ? positives.get(nthBest) : 0;
        positives.put(nthBest, value + 1);
      } else if (gGraphs.size() > 0) {
        Integer value =
            negatives.containsKey(nthBest) ? negatives.get(nthBest) : 0;
        negatives.put(nthBest, value + 1);
      }
    }

    logger.info("# Total number of Grounded Graphs: " + gGraphs.size());
    // System.out.println("# Total number of Connected Grounded Graphs: "
    // + connectedGraphCount);
    logger.info("\n###########################\n");

  }

}
