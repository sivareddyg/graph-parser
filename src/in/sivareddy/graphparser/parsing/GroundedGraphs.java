package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.CategoryIndex;
import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.CcgParseTree;
import in.sivareddy.graphparser.ccg.CcgParseTree.TooManyParsesException;
import in.sivareddy.graphparser.ccg.CcgParser;
import in.sivareddy.graphparser.ccg.FunnyCombinatorException;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.ccg.SemanticCategory;
import in.sivareddy.graphparser.ccg.SemanticCategoryType;
import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;
import in.sivareddy.graphparser.parsing.LexicalGraph.ArgGrelFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.ArgGrelPartFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.ArgStemGrelPartMatchingFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.ArgStemMatchingFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.DuplicateEdgeFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.EdgeNodeCountFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.EntityScoreFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.EntityWordOverlapFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.EventTypeGrelPartFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.GraphHasEdgeFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.GraphIsConnectedFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.GraphNodeCountFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.GrelGrelFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.GtypeGrelPartFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.HasQuestionEntityEdgeFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.MediatorStemGrelPartMatchingFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.MergedEdgeFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.NgramGrelFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.ParaphraseClassifierScoreFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.ParaphraseScoreFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.StemMatchingFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.UrelGrelFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.UrelPartGrelPartFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.UtypeGtypeFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.WordGrelFeature;
import in.sivareddy.graphparser.parsing.LexicalGraph.WordGrelPartFeature;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.EntityType;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.PorterStemmer;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.google.common.base.CharMatcher;
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

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.international.Language;
import edu.stanford.nlp.trees.TypedDependency;

public class GroundedGraphs {
  private Schema schema;
  private GroundedLexicon groundedLexicon;
  private KnowledgeBase kb;
  private CcgParser normalCcgParser;
  private CcgParser questionCcgParser;

  private static Set<String> lexicalPosTags = Sets.newHashSet("NNP", "NNPS",
      "PROPN");
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();

  private boolean urelGrelFlag = true;
  private boolean urelPartGrelPartFlag = true;
  private boolean utypeGtypeFlag = true;
  private boolean gtypeGrelPartFlag = true;
  private boolean grelGrelFlag = true;
  private boolean wordGrelPartFlag = true;
  private boolean ngramGrelPartFlag = true;
  private boolean wordGrelFlag = true;
  private boolean argGrelPartFlag = true;
  private boolean argGrelFlag = true;
  private boolean eventTypeGrelPartFlag = false;
  private boolean stemMatchingFlag = true;
  private boolean mediatorStemGrelPartMatchingFlag = true;
  private boolean argumentStemMatchingFlag = true;
  private boolean argumentStemGrelPartMatchingFlag = true;
  private boolean graphIsConnectedFlag = false;
  private boolean graphHasEdgeFlag = false;
  private boolean countNodesFlag = false;
  private boolean edgeNodeCountFlag = true;
  private boolean useLexiconWeightsRel = false;
  private boolean useLexiconWeightsType = false;
  private boolean duplicateEdgesFlag = true;
  private boolean handleNumbers = false;
  private boolean entityScoreFlag = false;
  private boolean entityWordOverlapFlag = false;
  private boolean paraphraseScoreFlag = false;
  private boolean paraphraseClassifierScoreFlag = false;
  private boolean allowMerging = false;
  private boolean handleEventEventEdges = false;
  private boolean useBackOffGraph = false;

  private StructuredPercepton learningModel;
  private int ngramLength = 2;
  public double initialEdgeWeight;
  public double initialTypeWeight;
  public double initialWordWeight;
  public double stemFeaturesWeight;
  public Logger logger;

  private Map<String, String> stems = Maps.newConcurrentMap();

  public GroundedGraphs(Schema schema, KnowledgeBase kb,
      GroundedLexicon groundedLexicon, CcgAutoLexicon normalCcgAutoLexicon,
      CcgAutoLexicon questionCcgAutoLexicon,
      String[] relationLexicalIdentifiers, String[] relationTypingIdentifiers,
      StructuredPercepton learningModel, int ngramLength, boolean urelGrelFlag,
      boolean urelPartGrelPartFlag, boolean utypeGtypeFlag,
      boolean gtypeGrelPartFlag, boolean grelGrelFlag, boolean ngramGrelFlag,
      boolean wordGrelPartFlag, boolean wordGrelFlag, boolean argGrelPartFlag,
      boolean argGrelFlag, boolean eventTypeGrelPartFlag,
      boolean stemMatchingFlag, boolean mediatorStemGrelPartMatchingFlag,
      boolean argumentStemMatchingFlag,
      boolean argumentStemGrelPartMatchingFlag, boolean graphIsConnectedFlag,
      boolean graphHasEdgeFlag, boolean countNodesFlag,
      boolean edgeNodeCountFlag, boolean useLexiconWeightsRel,
      boolean useLexiconWeightsType, boolean duplicateEdgesFlag,
      boolean ignorePronouns, boolean handleNumbers, boolean entityScoreFlag,
      boolean entityWordOverlapFlag, boolean paraphraseScoreFlag,
      boolean paraphraseClassifierScoreFlag, boolean allowMerging,
      boolean handleEventEventEdges, boolean useBackOffGraph,
      double initialEdgeWeight, double initialTypeWeight,
      double initialWordWeight, double stemFeaturesWeight) throws IOException {

    // ccg parser initialisation
    String[] argumentLexicalIdenfiers = {"mid"};
    normalCcgParser =
        new CcgParser(normalCcgAutoLexicon, relationLexicalIdentifiers,
            argumentLexicalIdenfiers, relationTypingIdentifiers, ignorePronouns);
    questionCcgParser =
        new CcgParser(questionCcgAutoLexicon, relationLexicalIdentifiers,
            argumentLexicalIdenfiers, relationTypingIdentifiers, ignorePronouns);

    this.handleNumbers = handleNumbers;
    this.groundedLexicon = groundedLexicon;
    this.kb = kb;
    this.schema = schema;
    this.learningModel =
        learningModel != null ? learningModel : new StructuredPercepton();

    this.urelGrelFlag = urelGrelFlag;
    this.urelPartGrelPartFlag = urelPartGrelPartFlag;
    this.utypeGtypeFlag = utypeGtypeFlag;
    this.gtypeGrelPartFlag = gtypeGrelPartFlag;
    this.grelGrelFlag = grelGrelFlag;
    this.wordGrelPartFlag = wordGrelPartFlag;
    this.ngramGrelPartFlag = ngramGrelFlag;
    this.wordGrelFlag = wordGrelFlag;
    this.argGrelPartFlag = argGrelPartFlag;
    this.argGrelFlag = argGrelFlag;
    this.eventTypeGrelPartFlag = eventTypeGrelPartFlag;
    this.stemMatchingFlag = stemMatchingFlag;
    this.mediatorStemGrelPartMatchingFlag = mediatorStemGrelPartMatchingFlag;
    this.argumentStemMatchingFlag = argumentStemMatchingFlag;
    this.argumentStemGrelPartMatchingFlag = argumentStemGrelPartMatchingFlag;

    this.graphIsConnectedFlag = graphIsConnectedFlag;
    this.graphHasEdgeFlag = graphHasEdgeFlag;
    this.countNodesFlag = countNodesFlag;
    this.edgeNodeCountFlag = edgeNodeCountFlag;
    this.duplicateEdgesFlag = duplicateEdgesFlag;
    this.allowMerging = allowMerging;
    this.handleEventEventEdges = handleEventEventEdges;
    this.useBackOffGraph = useBackOffGraph;

    this.entityScoreFlag = entityScoreFlag;
    this.entityWordOverlapFlag = entityWordOverlapFlag;
    this.paraphraseScoreFlag = paraphraseScoreFlag;
    this.paraphraseClassifierScoreFlag = paraphraseClassifierScoreFlag;

    this.useLexiconWeightsRel = useLexiconWeightsRel;
    this.useLexiconWeightsType = useLexiconWeightsType;

    this.ngramLength = ngramLength;
    this.initialEdgeWeight = initialEdgeWeight;
    this.initialTypeWeight = initialTypeWeight;
    this.initialWordWeight = initialWordWeight;
    this.stemFeaturesWeight = stemFeaturesWeight;

    this.logger = Logger.getLogger(this.getClass());

    StemMatchingFeature stemFeature = new StemMatchingFeature(0.0);
    this.learningModel.setWeightIfAbsent(stemFeature, stemFeaturesWeight);

    ArgStemMatchingFeature argStemFeature = new ArgStemMatchingFeature(0.0);
    this.learningModel.setWeightIfAbsent(argStemFeature, stemFeaturesWeight);

    MediatorStemGrelPartMatchingFeature mediatorStemGrelPartMatchingFeature =
        new MediatorStemGrelPartMatchingFeature(0.0);
    this.learningModel.setWeightIfAbsent(mediatorStemGrelPartMatchingFeature,
        stemFeaturesWeight);

    ArgStemGrelPartMatchingFeature argStemGrelPartMatchingFeature =
        new ArgStemGrelPartMatchingFeature(0.0);
    this.learningModel.setWeightIfAbsent(argStemGrelPartMatchingFeature,
        stemFeaturesWeight);

    this.learningModel.setWeightIfAbsent(
        new HasQuestionEntityEdgeFeature(true), 3.0);
    this.learningModel.setWeightIfAbsent(new GraphHasEdgeFeature(true), 2.0);

    this.learningModel.setWeightIfAbsent(new EntityScoreFeature(1.0), 2.0);
    this.learningModel
        .setWeightIfAbsent(new EntityWordOverlapFeature(1.0), 2.0);
  }

  /**
   * Constructs ungrounded semantic graphs from ccg parses of a given sentence.
   * Sentence is in json format containing information about entities, ccg
   * derivations
   * 
   * @param jsonSentence
   * @param nbest
   * @return
   */
  public List<LexicalGraph> buildUngroundedGraph(JsonObject jsonSentence,
      String key, int nbest) {
    return buildUngroundedGraph(jsonSentence, key, nbest, logger);
  }

  /**
   * Constructs ungrounded semantic graphs from ccg parses of a given sentence.
   * Sentence is in json format containing information about entities, ccg
   * derivations
   * 
   * @param jsonSentence
   * @param nbest
   * @param logger
   * @return
   * @throws BadParseException
   * @throws FunnyCombinatorException
   */
  public List<LexicalGraph> buildUngroundedGraph(JsonObject jsonSentence,
      String key, int nbest, Logger logger) {
    List<LexicalGraph> graphs = Lists.newArrayList();
    List<String> words = Lists.newArrayList();
    for (JsonElement word : jsonSentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      words
          .add(word.getAsJsonObject().get(SentenceKeys.WORD_KEY).getAsString());
    }
    logger.debug("Tokenized Sentence: " + Joiner.on(" ").join(words));

    if (key.equals(SentenceKeys.CCG_PARSES)) {
      Set<Set<String>> allSemanticParses = new HashSet<>();

      if (!jsonSentence.has(SentenceKeys.CCG_PARSES))
        return graphs;
      JsonArray synPars =
          jsonSentence.get(SentenceKeys.CCG_PARSES).getAsJsonArray();

      int parseCount = 1;
      for (JsonElement synParseElement : synPars) {
        if (parseCount > nbest)
          break;
        parseCount += 1;

        JsonObject synParseObject = synParseElement.getAsJsonObject();
        String synParse =
            synParseObject.get(SentenceKeys.CCG_PARSE).getAsString();
        Double score = synParseObject.get(SentenceKeys.SCORE).getAsDouble();

        List<CcgParseTree> ccgParses;
        try {
          if (synParse.startsWith("(<T S[dcl] ")
              || synParse.startsWith("(<T S[pss] ")
              || synParse.startsWith("(<T S[pt] ")
              || synParse.startsWith("(<T S[b] ")
              || synParse.startsWith("(<T S[ng] ")
              || synParse.startsWith("(<T S "))
            ccgParses = normalCcgParser.parseFromString(synParse);
          else
            ccgParses = questionCcgParser.parseFromString(synParse);
        } catch (FunnyCombinatorException | BadParseException
            | TooManyParsesException e) {
          // bad parse
          continue;
        }

        for (CcgParseTree ccgParse : ccgParses) {
          List<LexicalItem> leaves = ccgParse.getLeafNodes();
          lexicaliseArgumentsToDomainEntities(leaves, jsonSentence);
          Set<Set<String>> semanticParses =
              ccgParse.getLexicalisedSemanticPredicates(handleNumbers);
          for (Set<String> semanticParse : semanticParses) {
            // Check if the semantic parse is already present. Sometimes
            // different CCG derivations could lead to the same same semantic
            // parse.
            if (allSemanticParses.contains(semanticParse)) {
              continue;
            }
            allSemanticParses.add(semanticParse);

            int prev_size = graphs.size();

            try {
              buildUngroundeGraphFromSemanticParse(semanticParse, leaves,
                  score, graphs);
            } catch (Exception e) {
              System.err.println("Warining: bad semantic parse: "
                  + semanticParse);
              System.err.println("Corresponding sentence: "
                  + Joiner.on(" ").join(words));
              e.printStackTrace();
            }

            // Traceback each graph to its original syntactic parse.
            for (int ithGraph = prev_size; ithGraph < graphs.size(); ithGraph++) {
              graphs.get(ithGraph).setSyntacticParse(synParse);
            }
          }
        }
      }
    } else if (key.equals(SentenceKeys.DEPENDENCY_QUESTION_GRAPH)) {
      graphs.addAll(getDependencyUngroundedGraph(jsonSentence, true));
    } else if (key.equals(SentenceKeys.DEPENDENCY_GRAPH)) {
      graphs.addAll(getDependencyUngroundedGraph(jsonSentence, false));
    } else if (key.equals(SentenceKeys.BOW_QUESTION_GRAPH)) {
      graphs.addAll(getBagOfWordsUngroundedGraph(jsonSentence));
    } else { // the semantic parses are already given
      if (!jsonSentence.has(key))
        return graphs;
      List<LexicalItem> leaves = buildLexicalItemsFromWords(jsonSentence);
      JsonArray semPars = jsonSentence.get(key).getAsJsonArray();

      for (JsonElement semPar : semPars) {
        JsonArray predicates = semPar.getAsJsonArray();
        if (predicates.size() == 0)
          continue;
        Set<String> semanticParse = new HashSet<>();
        predicates.forEach(x -> semanticParse.add(x.getAsString()));

        try {
          buildUngroundeGraphFromSemanticParse(semanticParse, leaves, 0.0,
              graphs);
        } catch (Exception e) {
          System.err.println("Warining: bad semantic parse: " + semanticParse);
          System.err.println("Corresponding sentence: "
              + Joiner.on(" ").join(words));
          e.printStackTrace();
        }
      }
    }

    // TODO(sivareddyg): Currently, {@link
    // in.sivareddy.graphparser.parsing.GraphToSparqlConverter} does not
    // support questions with multiple TARGETs. A hacky solution is to remove
    // multiple question nodes, and retain only the one that appear first.
    graphs.forEach(g -> g.removeMultipleQuestionNodes());

    if (useBackOffGraph
        && (key.equals(SentenceKeys.CCG_PARSES) || key
            .equals(SentenceKeys.DEPENDENCY_LAMBDA))) {
      // If there is no path found between question and entity nodes, use a
      // backoff graph.
      boolean graphHasPath = false;
      for (LexicalGraph graph : graphs) {
        graphHasPath |= graph.hasPathBetweenQuestionAndEntityNodes();
      }

      if (!graphHasPath) {
        JsonArray semPars = new JsonArray();
        graphs.forEach(x -> semPars.add(jsonParser.parse(gson.toJson(x
            .getSemanticParse()))));
        graphs.clear();
        for (JsonElement semPar : semPars) {
          JsonArray predicates = semPar.getAsJsonArray();
          if (predicates.size() == 0)
            continue;
          Set<String> semanticParse = new HashSet<>();
          predicates.forEach(x -> semanticParse.add(x.getAsString()));
          LexicalGraph backOffGraph =
              buildBackOffUngroundedGraph(jsonSentence, semanticParse);
          if (backOffGraph != null) {
            graphs.add(backOffGraph);
          }
        }
      }
    }

    // Add ungrounded graph features.
    addUngroundedGraphFeatures(jsonSentence, graphs);
    return graphs;
  }

  public void addUngroundedGraphFeatures(JsonObject jsonSentence,
      List<LexicalGraph> graphs) {
    // Add entity disambiguation scores.
    if (entityScoreFlag) {
      double entityScore = 0.0;
      if (jsonSentence.has(SentenceKeys.ENTITIES)) {
        for (JsonElement entity : jsonSentence.get(SentenceKeys.ENTITIES)
            .getAsJsonArray()) {
          JsonObject entityObj = entity.getAsJsonObject();
          if (entityObj.has(SentenceKeys.SCORE)) {
            entityScore += entityObj.get(SentenceKeys.SCORE).getAsDouble();
          }
        }
      }
      if (entityScore > 1.0) {
        EntityScoreFeature feature =
            new EntityScoreFeature(Math.log10(entityScore));
        for (LexicalGraph uGraph : graphs) {
          uGraph.addFeature(feature);
        }
      }
    }

    // Add entity overlap scores.
    if (entityWordOverlapFlag) {
      double entityWordOverlapScore = 0.0;
      if (jsonSentence.has(SentenceKeys.ENTITIES)) {
        for (JsonElement entity : jsonSentence.get(SentenceKeys.ENTITIES)
            .getAsJsonArray()) {
          JsonObject entityObj = entity.getAsJsonObject();
          if (entityObj.has(SentenceKeys.PHRASE)
              && entityObj.has(SentenceKeys.ENTITY_NAME)) {
            Set<String> originalWords =
                Sets.newHashSet(Splitter.on(CharMatcher.WHITESPACE).split(
                    entityObj.get(SentenceKeys.PHRASE).getAsString()
                        .toLowerCase()));
            Set<String> predictedEntityWords =
                Sets.newHashSet(Splitter.on(CharMatcher.WHITESPACE).split(
                    entityObj.get(SentenceKeys.ENTITY_NAME).getAsString()
                        .toLowerCase()));
            int total1 = originalWords.size();
            int total2 = predictedEntityWords.size();
            originalWords.retainAll(predictedEntityWords);
            int intersection = originalWords.size();
            double p1 = (intersection + 0.0) / total1;
            double p2 = (intersection + 0.0) / total2;
            double mean = 0.0;
            if (p1 + p2 > 0.0005) {
              mean = 2.0 * p1 * p2 / (p1 + p2);
            }
            entityWordOverlapScore += mean;
          }
        }
      }
      if (entityWordOverlapScore > 0.0005) {
        EntityWordOverlapFeature feature =
            new EntityWordOverlapFeature(entityWordOverlapScore);
        for (LexicalGraph uGraph : graphs) {
          uGraph.addFeature(feature);
        }
      }
    }

    if (paraphraseScoreFlag) {
      if (jsonSentence.has(SentenceKeys.PARAPHRASE_SCORE)) {
        double score =
            jsonSentence.get(SentenceKeys.PARAPHRASE_SCORE).getAsDouble();
        ParaphraseScoreFeature feature = new ParaphraseScoreFeature(score);
        for (LexicalGraph uGraph : graphs) {
          uGraph.addFeature(feature);
        }
      }
    }

    if (paraphraseClassifierScoreFlag) {
      if (jsonSentence.has(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)) {
        double score =
            jsonSentence.get(SentenceKeys.PARAPHRASE_CLASSIFIER_SCORE)
                .getAsDouble();
        ParaphraseClassifierScoreFeature feature =
            new ParaphraseClassifierScoreFeature(Math.exp(score));
        for (LexicalGraph uGraph : graphs) {
          uGraph.addFeature(feature);
        }
      }
    }
  }

  /**
   * Returns a bag-of-words based ungrounded graph. For questions containing
   * "how many", this implementation returns two graphs.
   * 
   * @param jsonSentence
   * @return
   */
  public List<LexicalGraph> getBagOfWordsUngroundedGraph(JsonObject jsonSentence) {
    List<LexicalGraph> graphs = Lists.newArrayList();
    List<LexicalItem> leaves = buildLexicalItemsFromWords(jsonSentence);
    Set<String> parse =
        getBagOfWordsUngroundedSemanticParse(jsonSentence, leaves);
    buildUngroundeGraphFromSemanticParse(parse, leaves, 0.0, graphs);

    Set<String> countStyledParse =
        createCountStyledSemanticParse(jsonSentence, parse);

    if (countStyledParse != null) {
      buildUngroundeGraphFromSemanticParse(countStyledParse, leaves, 0.0,
          graphs);
    }

    // Add ungrounded graph features.
    addUngroundedGraphFeatures(jsonSentence, graphs);
    return graphs;
  }


  public static Set<String> createCountStyledSemanticParse(
      JsonObject jsonSentence, Set<String> originalParse) {
    StringBuilder sentenceBuilder = new StringBuilder();
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    words.forEach(x -> {
      sentenceBuilder.append(x.getAsJsonObject().get(SentenceKeys.WORD_KEY)
          .getAsString().toLowerCase());
      sentenceBuilder.append(" ");
    });

    if (sentenceBuilder.toString().contains("how many")) {
      // If the sentence has "how many", add an additional graph with COUNT.
      String questionPredicate = null;
      for (String parsePart : originalParse) {
        if (parsePart.startsWith("QUESTION(")) {
          questionPredicate = parsePart;
          break;
        }
      }

      if (questionPredicate != null) {
        Set<String> newParse = new HashSet<>(originalParse);
        newParse.remove(questionPredicate);
        String questionVar =
            questionPredicate.replace("QUESTION(", "").replace(")", "");
        String countVar =
            questionVar.equals("0:x") ? String.format("%d:x", words.size() - 1)
                : "0:x";
        newParse.add(String.format("QUESTION(%s)", countVar));
        newParse.add(String.format("COUNT(%s , %s)", questionVar, countVar));
        return newParse;
      }
    }
    return null;
  }

  /**
   * Returns a bag-of-words styled ungrounded semantic parse.
   * 
   * @param jsonSentence
   * @return
   */
  public static Set<String> getBagOfWordsUngroundedSemanticParse(
      JsonObject jsonSentence, List<LexicalItem> leaves) {
    LexicalItem dummyNode =
        new LexicalItem("", SentenceKeys.BLANK_WORD, SentenceKeys.BLANK_WORD,
            "NNP", "", null);
    leaves.add(dummyNode);
    int questionWordIndex = leaves.size() - 1;
    dummyNode.setWordPosition(questionWordIndex);

    Set<String> parse = new HashSet<>();
    if (!jsonSentence.has(SentenceKeys.ENTITIES))
      return parse;
    JsonArray entities =
        jsonSentence.get(SentenceKeys.ENTITIES).getAsJsonArray();
    if (entities.size() == 0)
      return parse;

    // Create all the edges.
    for (JsonElement entity : entities) {
      JsonObject entityObj = entity.getAsJsonObject();
      int index = entityObj.get(SentenceKeys.INDEX_KEY).getAsInt();

      String fbEntity = entityObj.get(SentenceKeys.ENTITY).getAsString();
      String edge =
          String.format("dummy.edge.entity(%d:e , %d:%s)", questionWordIndex,
              index, fbEntity);
      parse.add(edge);
    }

    for (LexicalItem leaf : leaves) {
      if (leaf.getMid().startsWith("type.")) {
        String edge =
            String.format("dummy.edge.entity(%d:e , %d:%s)", questionWordIndex,
                leaf.getWordPosition(), leaf.getMid());
        parse.add(edge);
      }
    }

    parse.add(String.format("dummy.edge.question(%d:e , %d:x)",
        questionWordIndex, questionWordIndex));
    parse.add(String.format("QUESTION(%d:x)", questionWordIndex));
    return parse;
  }

  public List<LexicalGraph> getDependencyUngroundedGraph(
      JsonObject jsonSentence, boolean isQuestion) {
    List<LexicalGraph> graphs = Lists.newArrayList();
    List<LexicalItem> leaves = buildLexicalItemsFromWords(jsonSentence);

    Map<Integer, String> indexToEntity = new HashMap<>();
    if (jsonSentence.has(SentenceKeys.ENTITIES)) {
      JsonArray entities =
          jsonSentence.get(SentenceKeys.ENTITIES).getAsJsonArray();
      // Create all the edges.
      for (JsonElement entity : entities) {
        JsonObject entityObj = entity.getAsJsonObject();
        int index = entityObj.get(SentenceKeys.INDEX_KEY).getAsInt();
        String fbEntity = entityObj.get(SentenceKeys.ENTITY).getAsString();
        indexToEntity.put(index, fbEntity);
      }
    }

    int i = -1;
    Set<String> semanticParse = new HashSet<>();
    for (JsonElement word : jsonSentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      i += 1;
      JsonObject wordObj = word.getAsJsonObject();
      if (wordObj.has(SentenceKeys.HEAD_KEY)
          && wordObj.has(SentenceKeys.DEPENDENCY_KEY)) {
        int head = wordObj.get(SentenceKeys.HEAD_KEY).getAsInt() - 1;

        if (head < 0) {
          continue;
        }

        if (head >= leaves.size())
          continue;


        String childArgName = "";
        if (isQuestion && i == 0 && !indexToEntity.containsKey(i)) {
          // Assuming first node is a question node if the input is a
          // question.
          childArgName = "0:x";
          semanticParse.add("QUESTION(0:x)");
        } else if (leaves.get(i).getLemma().equals(SentenceKeys.BLANK_WORD)) {
          childArgName = String.format("%d:x", i);
          semanticParse.add(String.format("QUESTION(%s)", childArgName));
        } else {
          childArgName =
              indexToEntity.containsKey(i) ? i + ":" + leaves.get(i).getMid()
                  : i + ":x";
        }

        String parentArgName = "";
        if (isQuestion && head == 0 && !indexToEntity.containsKey(head)) {
          // Assuming first node is a question node if the input is a
          // question.
          parentArgName = "0:x";
          semanticParse.add("QUESTION(0:x)");
        } else if (leaves.get(head).getLemma().equals(SentenceKeys.BLANK_WORD)) {
          parentArgName = String.format("%d:x", head);
          semanticParse.add(String.format("QUESTION(%s)", parentArgName));
        } else {
          parentArgName =
              indexToEntity.containsKey(head) ? head + ":"
                  + leaves.get(head).getMid() : head + ":x";
        }

        String depLabel =
            wordObj.get(SentenceKeys.DEPENDENCY_KEY).getAsString();
        String eventName = head + ":e";
        String childPredicateName =
            indexToEntity.containsKey(head) ? depLabel : String.format("%s.%s",
                leaves.get(head).getLemma(), depLabel);
        String parentPredicateName =
            indexToEntity.containsKey(head) ? "arg_0" : String.format(
                "%s.arg_0", leaves.get(head).getLemma());

        semanticParse.add(String.format("%s(%s , %s)", childPredicateName,
            eventName, childArgName));

        semanticParse.add(String.format("%s(%s , %s)", parentPredicateName,
            eventName, parentArgName));
      }
    }

    buildUngroundeGraphFromSemanticParse(semanticParse, leaves, 0.0, graphs);

    if (isQuestion) {
      Set<String> countStyledParse =
          createCountStyledSemanticParse(jsonSentence, semanticParse);
      if (countStyledParse != null) {
        buildUngroundeGraphFromSemanticParse(countStyledParse, leaves, 0.0,
            graphs);
      }
    }

    return graphs;
  }

  private static Set<String> stopWordsUniversal = Sets
      .newHashSet(SentenceKeys.BLANK_WORD);
  private static Pattern punctuation = Pattern.compile("[\\p{Punct}]+");

  public static Set<String> CONTENT_WORD_POS = Sets.newHashSet();

  // Sets.newHashSet("NOUN", "VERB",
  // "ADJ", "NN", "NNS", "JJ", "JJR", "JJS", "VB", "VBD", "VBN", "VBP", "VBZ",
  // "VBG", "NNP", "NNPS");

  public static List<String> getNgrams(List<LexicalItem> words, int nGram) {
    List<String> wordStrings = new ArrayList<>();
    for (LexicalItem word : words) {
      if (!word.getLemma().equals(word.getMid()) && !word.getMid().equals("x")) {
        // Current word is an entity.
        continue;
      }

      if (CONTENT_WORD_POS.size() > 0
          && !CONTENT_WORD_POS.contains(word.getPos())) {
        // Current word is not content word.
        continue;
      }

      String wordString = word.getLemma();
      if (stopWordsUniversal.contains(wordString)
          || punctuation.matcher(wordString).matches()) {
        continue;
      }
      wordStrings.add(wordString);
    }

    if (nGram == 1)
      return wordStrings;
    else {
      List<String> nGrams = new ArrayList<>();
      wordStrings.add("EOL");
      for (int i = nGram - 1; i < wordStrings.size(); i++) {
        StringBuffer sb = new StringBuffer();
        sb.append(wordStrings.get(i - nGram + 1));
        for (int j = nGram - 2; j >= 0; j--) {
          sb.append("::");
          sb.append(wordStrings.get(i - j));
        }
        nGrams.add(sb.toString());
      }
      return nGrams;
    }
  }

  /**
   * Useful to free memory. Due to bad design, I ended up having static
   * variables at many places. This should solve the problem partly. Do not call
   * this function if you are running multiple threads.
   */
  public static synchronized void resetAllCounters() {
    SemanticCategory.resetCounter();
    CategoryIndex.resetCounter();
    CcgParser.resetCounter();
  }

  public static List<LexicalItem> buildLexicalItemsFromWords(
      JsonObject jsonSentence) {
    Preconditions.checkArgument(jsonSentence.has("words"));
    Map<Integer, String> tokenToEntity = new HashMap<>();
    List<LexicalItem> items = new ArrayList<>();

    if (jsonSentence.has("entities")) {
      for (JsonElement entityElement : jsonSentence.get("entities")
          .getAsJsonArray()) {
        JsonObject entityObject = entityElement.getAsJsonObject();
        tokenToEntity.put(entityObject.get("index").getAsInt(), entityObject
            .get("entity").getAsString());
      }
    }

    int i = 0;
    for (JsonElement wordElement : jsonSentence.get("words").getAsJsonArray()) {
      JsonObject wordObject = wordElement.getAsJsonObject();
      String word = wordObject.get("word").getAsString();
      String lemma =
          wordObject.has("lemma") ? wordObject.get("lemma").getAsString()
              : word;
      String pos = wordObject.get("pos").getAsString();
      String ner =
          wordObject.has(SentenceKeys.NER_KEY) ? wordObject.get(
              SentenceKeys.NER_KEY).getAsString() : "";
      LexicalItem lex = new LexicalItem("", word, lemma, pos, ner, null);
      lex.setWordPosition(i);
      if (tokenToEntity.containsKey(i)) {
        lex.setMid(tokenToEntity.get(i));
      } else {
        setNumericalMids(wordObject, lex);
      }
      items.add(lex);
      ++i;
    }
    return items;
  }

  private static class UngroundedGraphBuildingBlocks {
    public Map<Integer, Set<Pair<String, Integer>>> events = Maps.newHashMap();
    public Map<Integer, Set<Pair<String, Integer>>> types = Maps.newHashMap();
    public Map<Integer, Set<Pair<String, Integer>>> eventTypes = Maps
        .newHashMap();
    public Map<Integer, Set<Pair<String, Integer>>> eventEventModifiers = Maps
        .newHashMap();
    public Map<Integer, Set<Pair<String, String>>> specialTypes = Maps
        .newHashMap();

    public UngroundedGraphBuildingBlocks(Set<String> semanticParse) {
      Pattern relationPattern =
          Pattern.compile("(.*)\\(([0-9]+)\\:e , ([0-9]+\\:.*)\\)");
      Pattern typePattern =
          Pattern.compile("(.*)\\(([0-9]+)\\:s , ([0-9]+\\:.*)\\)");

      // NEGATION(E), NEGATION(S), COUNT(X, int)
      Pattern specialPattern =
          Pattern.compile("(.*)\\(([0-9]+)\\:[^\\s]+( , )?([0-9]+:.*)?\\)");
      // Pattern varPattern = Pattern.compile("^[0-9]+\\:x$");
      Pattern eventPattern = Pattern.compile("^[0-9]+\\:e$");

      // Build the semantic tree
      for (String predicate : semanticParse) {
        boolean isMatchedAlready = false;
        Matcher matcher = relationPattern.matcher(predicate);
        if (matcher.find()) {
          isMatchedAlready = true;
          String relationName = matcher.group(1);

          // Removing this constraint since there might be cases with completely
          // capitalised predicates, and fall in Semantic Category types. Check
          // if
          // this has any side effects in future.
          /*-Preconditions.checkArgument(
              !SemanticCategoryType.types.contains(relationName),
              "relation pattern should not match special types");*/

          Integer eventIndex = Integer.valueOf(matcher.group(2));
          String argumentName = matcher.group(3);
          Integer argumentIndex = Integer.valueOf(argumentName.split(":")[0]);

          if (eventPattern.matcher(argumentName).matches()) {
            // if the event takes event as argument
            Pair<String, Integer> value = Pair.of(relationName, argumentIndex);
            if (!eventEventModifiers.containsKey(eventIndex))
              eventEventModifiers.put(eventIndex,
                  new HashSet<Pair<String, Integer>>());
            eventEventModifiers.get(eventIndex).add(value);
          } else {
            // if the event takes an entity as an argument
            if (!events.containsKey(eventIndex))
              events.put(eventIndex, new HashSet<Pair<String, Integer>>());
            Pair<String, Integer> edge = Pair.of(relationName, argumentIndex);
            events.get(eventIndex).add(edge);
          }
        }

        if (isMatchedAlready)
          continue;
        matcher = typePattern.matcher(predicate);
        if (matcher.find()) {
          isMatchedAlready = true;
          String typeName = matcher.group(1);

          // Removing this constraint since there might be cases with completely
          // capitalised predicates, and fall in Semantic Category types. Check
          // if
          // this has any side effects in future.
          /*-Preconditions.checkArgument(
              !SemanticCategoryType.types.contains(typeName),
              "type pattern should not match special types");*/

          Integer stateIndex = Integer.valueOf(matcher.group(2));
          String argumentName = matcher.group(3);
          Integer argumentIndex = Integer.valueOf(argumentName.split(":")[0]);

          if (eventPattern.matcher(argumentName).matches()) {
            // if the state takes event as argument
            Pair<String, Integer> value = Pair.of(typeName, stateIndex);
            if (!eventTypes.containsKey(argumentIndex))
              eventTypes.put(argumentIndex,
                  new HashSet<Pair<String, Integer>>());
            eventTypes.get(argumentIndex).add(value);
          } else {
            // if the state takes entity as argument
            if (!types.containsKey(argumentIndex))
              types.put(argumentIndex, new HashSet<Pair<String, Integer>>());
            Pair<String, Integer> entityType = Pair.of(typeName, stateIndex);
            types.get(argumentIndex).add(entityType);
          }
        }

        if (isMatchedAlready)
          continue;
        matcher = specialPattern.matcher(predicate);
        if (matcher.find()) {
          String specialTypeName = matcher.group(1);
          Integer entityIndex = Integer.valueOf(matcher.group(2));
          String args = matcher.group(4);

          // System.out.println(predicate);
          // System.out.println(specialTypeName);
          // CHANGE THIS LATER

          Preconditions.checkArgument(
              SemanticCategoryType.types.contains(specialTypeName),
              "Unknown special type");
          if (!specialTypes.containsKey(entityIndex))
            specialTypes.put(entityIndex, new HashSet<Pair<String, String>>());
          Pair<String, String> specialTypeProperties =
              Pair.of(specialTypeName, args);
          specialTypes.get(entityIndex).add(specialTypeProperties);
        }
      }
    }
  }

  private void buildUngroundeGraphFromSemanticParse(Set<String> semanticParse,
      List<LexicalItem> leaves, double parseScore, List<LexicalGraph> graphs) {
    logger.debug("Semantic Parse:" + semanticParse);

    UngroundedGraphBuildingBlocks blocks =
        new UngroundedGraphBuildingBlocks(semanticParse);

    // Build the graph
    LexicalGraph graph =
        buildUngroundedGraph(leaves, blocks.events, blocks.types,
            blocks.specialTypes, blocks.eventTypes, blocks.eventEventModifiers);
    graph.setActualNodes(leaves);
    graph.setScore(parseScore);

    // Traceback the semantic parse from which the graph is created.
    graph.setSemanticParse(semanticParse);
    graphs.add(graph);
  }

  private LexicalGraph buildUngroundedGraph(List<LexicalItem> leaves,
      Map<Integer, Set<Pair<String, Integer>>> events,
      Map<Integer, Set<Pair<String, Integer>>> types,
      Map<Integer, Set<Pair<String, String>>> specialTypes,
      Map<Integer, Set<Pair<String, Integer>>> eventTypes,
      Map<Integer, Set<Pair<String, Integer>>> eventEventModifiers) {

    if (handleEventEventEdges) {
      // TODO(sivareddyg): This is a hacky! A better way to handle
      // event-event edges should be explored.
      for (Integer eventIndex : eventEventModifiers.keySet()) {
        for (Pair<String, Integer> type : eventEventModifiers.get(eventIndex)) {
          Integer modifierIndex = type.getRight();
          String entityTypeString = type.getLeft();

          if (eventIndex.equals(modifierIndex))
            continue;

          events.putIfAbsent(eventIndex, new HashSet<>());
          events.get(eventIndex).add(
              Pair.of(entityTypeString + ".arg_1", modifierIndex));

          events.putIfAbsent(modifierIndex, new HashSet<>());
          events.get(modifierIndex).add(
              Pair.of(entityTypeString + ".arg_2", modifierIndex));
        }
      }
    }

    LexicalGraph graph = new LexicalGraph();

    for (Integer event : events.keySet()) {
      List<Pair<String, Integer>> subEdges =
          Lists.newArrayList(events.get(event));
      if (subEdges.size() > 10) {
        // TODO: throw exception.
        return graph;
      }
      for (int i = 0; i < subEdges.size(); i++) {
        for (int j = i + 1; j < subEdges.size(); j++) {
          String leftEdge = subEdges.get(i).getLeft();
          String rightEdge = subEdges.get(j).getLeft();

          int node1Index = subEdges.get(i).getRight();
          int node2Index = subEdges.get(j).getRight();

          if (node1Index == node2Index)
            continue;

          LexicalItem node1 = leaves.get(node1Index);
          LexicalItem node2 = leaves.get(node2Index);
          LexicalItem mediator = leaves.get(event);

          // if (groundedLexicon)
          Relation relation = new Relation(leftEdge, rightEdge);
          // System.out.println(relation);
          relation.setWeight(groundedLexicon
              .getUngroundedRelationScore(relation));
          graph.addEdge(node1, node2, mediator, relation);
        }
      }
    }

    for (Integer entityIndex : types.keySet()) {
      for (Pair<String, Integer> type : types.get(entityIndex)) {
        Integer modifierIndex = type.getRight();
        String entityTypeString = type.getLeft();

        LexicalItem parentNode = leaves.get(entityIndex);
        LexicalItem modifierNode = leaves.get(modifierIndex);
        EntityType nodeType = new EntityType(entityTypeString);
        nodeType.setWeight(groundedLexicon.getUngroundedTypeScore(nodeType));
        graph.addType(parentNode, modifierNode, nodeType);
      }
    }

    for (Integer index : specialTypes.keySet()) {
      for (Pair<String, String> specialType : specialTypes.get(index)) {
        LexicalItem node = leaves.get(index);
        String propertyName = specialType.getLeft();
        String args = specialType.getRight();
        Property property;
        if (args == null || args.equals(""))
          property = new Property(propertyName);
        else {
          // args = args.split(":")[1];
          property = new Property(propertyName, args);
        }
        graph.addProperty(node, property);
      }
    }

    for (Integer eventIndex : eventTypes.keySet()) {
      for (Pair<String, Integer> type : eventTypes.get(eventIndex)) {
        Integer modifierIndex = type.getRight();
        String entityTypeString = type.getLeft();

        LexicalItem parentNode = leaves.get(eventIndex);
        LexicalItem modifierNode = leaves.get(modifierIndex);
        EntityType nodeType = new EntityType(entityTypeString);
        graph.addEventType(parentNode, modifierNode, nodeType);
      }
    }

    for (Integer eventIndex : eventEventModifiers.keySet()) {
      for (Pair<String, Integer> type : eventEventModifiers.get(eventIndex)) {
        Integer modifierIndex = type.getRight();
        String entityTypeString = type.getLeft();

        LexicalItem parentNode = leaves.get(eventIndex);
        LexicalItem modifierNode = leaves.get(modifierIndex);
        EntityType nodeType = new EntityType(entityTypeString);
        graph.addEventEventModifier(parentNode, modifierNode, nodeType);
      }
    }

    // adding the type "direcotor.photography.1" if
    // "director of photography" is found

    return graph;
  }


  public LexicalGraph buildBackOffUngroundedGraph(JsonObject sentence,
      Set<String> semanticParse) {
    Pattern badQuestionPattern =
        Pattern.compile(String.format("%s\\(([0-9]+)\\:[^x].*\\)",
            SemanticCategoryType.QUESTION));

    Set<String> semanticParseCopy =
        semanticParse.stream()
            .filter(x -> !badQuestionPattern.matcher(x).matches())
            .collect(Collectors.toSet());


    if (sentence.get(SentenceKeys.ENTITIES) == null)
      return null;

    JsonArray entities = sentence.get(SentenceKeys.ENTITIES).getAsJsonArray();
    if (entities.size() == 0)
      return null;

    Set<Integer> entityPositions = new HashSet<>();
    entities.forEach(x -> entityPositions.add(x.getAsJsonObject()
        .get(SentenceKeys.INDEX_KEY).getAsInt()));

    Pattern relationPattern =
        Pattern.compile("(.*)\\(([0-9]+)\\:e , ([0-9]+)\\:.*\\)");
    Pattern questionPattern =
        Pattern.compile(String.format("%s\\(([0-9]+)\\:x\\)",
            SemanticCategoryType.QUESTION));

    HashMap<Integer, Set<Pair<Integer, String>>> entityToEvent =
        new HashMap<>();
    HashMap<Integer, Set<Integer>> eventToEntities = new HashMap<>();
    HashSet<Integer> questionIndices = new HashSet<>();
    Set<String> questionStrings = new HashSet<>();
    for (String element : semanticParseCopy) {
      Matcher eventEntitymatcher = relationPattern.matcher(element);
      if (eventEntitymatcher.matches()) {
        String predicate = eventEntitymatcher.group(1);
        int event = Integer.parseInt(eventEntitymatcher.group(2));
        int entity = Integer.parseInt(eventEntitymatcher.group(3));
        entityToEvent.putIfAbsent(entity, new HashSet<>());
        entityToEvent.get(entity).add(Pair.of(event, predicate));
        eventToEntities.putIfAbsent(event, new HashSet<>());
        eventToEntities.get(event).add(entity);
      }

      Matcher questionMatcher = questionPattern.matcher(element);
      if (questionMatcher.matches()) {
        questionStrings.add(questionMatcher.group());
        int questionIndex = Integer.parseInt(questionMatcher.group(1));
        questionIndices.add(questionIndex);
      }
    }

    semanticParseCopy.removeAll(questionStrings);
    if (questionIndices.size() == 0) {
      questionIndices.add(sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray()
          .size() - 1);
    }
    Integer questionIndex = questionIndices.iterator().next();
    semanticParseCopy.add(String.format("%s(%d:x)",
        SemanticCategoryType.QUESTION, questionIndex));

    if (entityToEvent.keySet().containsAll(entityPositions)) {
      // Case 1: If all the entities are present in the parse,
      // add question to the event nodes.
      for (Set<Pair<Integer, String>> eventAndPredicates : entityToEvent
          .values()) {
        for (Pair<Integer, String> eventAndPredicate : eventAndPredicates) {
          Integer eventIndex = eventAndPredicate.getLeft();

          // Add the question node to the only the events it is not connected.
          if (eventToEntities.get(eventIndex).contains(questionIndex))
            continue;
          String predicate = eventAndPredicate.getRight();
          int lastIndex = predicate.lastIndexOf(".");
          if (lastIndex < 0)
            lastIndex = predicate.length() - 1;
          predicate = predicate.substring(0, lastIndex);
          semanticParseCopy.add(String.format("%s.dep_arg2(%d:e , %d:x)",
              predicate, eventIndex, questionIndex));
        }
      }
    } else {
      // Case 2: If question node is connected to an event, add entities to
      // the question's event.
      Pattern questionEventPattern =
          Pattern.compile(String.format("(.*)\\(([0-9]+)\\:e , %d\\:x\\)",
              questionIndex));
      boolean eventFound = false;
      for (String element : semanticParse) {
        Matcher eventEntitymatcher = questionEventPattern.matcher(element);
        if (eventEntitymatcher.matches()) {
          eventFound = true;
          String predicate = eventEntitymatcher.group(1);
          int lastIndex = predicate.lastIndexOf(".");
          if (lastIndex < 0)
            lastIndex = predicate.length() - 1;
          predicate = predicate.substring(0, lastIndex);

          int eventIndex = Integer.parseInt(eventEntitymatcher.group(2));
          for (JsonElement entityElm : entities) {
            JsonObject entityObj = entityElm.getAsJsonObject();
            int entityIndex =
                entityObj.get(SentenceKeys.ENTITY_INDEX).getAsInt();

            // Add event-entity subedges only if they are not already present.
            if (eventToEntities.get(eventIndex).contains(entityIndex))
              continue;

            String entity = entityObj.get(SentenceKeys.ENTITY).getAsString();
            semanticParseCopy.add(String.format("%s.dep_arg2(%d:e , %d:%s)",
                predicate, eventIndex, entityIndex, entity));
          }
        }
      }

      if (!eventFound) {
        // Case 3: If no question node event found, create a dummy event 0:e,
        // and connect all entities and question node to it.
        for (JsonElement entityElm : entities) {
          JsonObject entityObj = entityElm.getAsJsonObject();
          int entityIndex = entityObj.get(SentenceKeys.ENTITY_INDEX).getAsInt();
          String entity = entityObj.get(SentenceKeys.ENTITY).getAsString();
          if (eventToEntities.containsKey(questionIndex)
              && eventToEntities.get(questionIndex).contains(entityIndex))
            continue;
          semanticParseCopy.add(String.format("dep_arg2(%d:e , %d:%s)",
              questionIndex, entityIndex, entity));
        }
        semanticParseCopy.add(String.format(String.format(
            "dep_arg2(%d:e , %d:x)", questionIndex, questionIndex)));
      }
    }

    List<LexicalItem> leaves = buildLexicalItemsFromWords(sentence);
    List<LexicalGraph> graphs = new ArrayList<>();
    buildUngroundeGraphFromSemanticParse(semanticParseCopy, leaves, 0.0, graphs);
    Preconditions.checkArgument(graphs.size() == 1);
    return graphs.get(0);
  }

  public static final GrammaticalRelation EVENT_TO_ENTITY_EDGE =
      new GrammaticalRelation(Language.Any, "ev_en", "event_to_entity_edge",
          null);
  public static final GrammaticalRelation EVENT_TO_EVENT_EDGE =
      new GrammaticalRelation(Language.Any, "ev_ev", "event_to_event_edge",
          null);
  public static final GrammaticalRelation ENTITY_TO_TYPE =
      new GrammaticalRelation(Language.Any, "en_t", "entity_type", null);
  public static final GrammaticalRelation EVENT_TO_TYPE =
      new GrammaticalRelation(Language.Any, "ev_t", "event_type", null);
  public static final GrammaticalRelation ANY_TYPE = new GrammaticalRelation(
      Language.Any, "__any__", "__any__", null);

  public static final Map<String, GrammaticalRelation> grammaticalRelationCache;
  static {
    grammaticalRelationCache = new HashMap<>();
    grammaticalRelationCache.put("gov", GrammaticalRelation.GOVERNOR);
    grammaticalRelationCache.put("dep", GrammaticalRelation.DEPENDENT);
    grammaticalRelationCache.put("root", GrammaticalRelation.ROOT);
    grammaticalRelationCache.put("KILL", GrammaticalRelation.KILL);
  }

  public static synchronized GrammaticalRelation getGrammaticalRelation(
      String relName, String relType) {
    // No distinction between rel types for now. TODO.
    String key = relName;
    if (grammaticalRelationCache.containsKey(key)) {
      return grammaticalRelationCache.get(key);
    }
    GrammaticalRelation rel =
        new GrammaticalRelation(Language.Any, relName, relName, ANY_TYPE);
    grammaticalRelationCache.put(key, rel);
    return rel;
  }

  /**
   * Builds Stanford style {@code SemanticGraph} from the given semantic parse.
   * 
   * @param semanticParse
   * @param leaves
   * @return
   */
  public SemanticGraph buildSemanticGraphFromSemanticParse(
      Set<String> semanticParse, List<LexicalItem> leaves) {
    UngroundedGraphBuildingBlocks blocks =
        new UngroundedGraphBuildingBlocks(semanticParse);
    return buildSemanticGraph(leaves, blocks.events, blocks.types,
        blocks.specialTypes, blocks.eventTypes, blocks.eventEventModifiers);
  }

  public static IndexedWord makeWord(LexicalItem word) {
    CoreLabel w = new CoreLabel();
    w.setWord(word.getWord());
    w.setValue(word.getLemma());
    if (word.getWordPosition() >= 0) {
      w.setIndex(word.getWordPosition());
    }
    return new IndexedWord(w);
  }

  private SemanticGraph buildSemanticGraph(List<LexicalItem> leaves,
      Map<Integer, Set<Pair<String, Integer>>> events,
      Map<Integer, Set<Pair<String, Integer>>> types,
      Map<Integer, Set<Pair<String, String>>> specialTypes,
      Map<Integer, Set<Pair<String, Integer>>> eventTypes,
      Map<Integer, Set<Pair<String, Integer>>> eventEventModifiers) {


    List<TypedDependency> dependencies = new ArrayList<>();
    for (Integer event : events.keySet()) {
      List<Pair<String, Integer>> subEdges =
          Lists.newArrayList(events.get(event));
      for (int i = 0; i < subEdges.size(); i++) {
        String leftEdge = subEdges.get(i).getLeft();

        int node1Index = subEdges.get(i).getRight();

        LexicalItem node1 = leaves.get(node1Index);
        LexicalItem mediator = leaves.get(event);


        GrammaticalRelation leftRel = getGrammaticalRelation(leftEdge, "ev_en");

        IndexedWord node1Word = makeWord(node1);
        IndexedWord mediatorWord = makeWord(mediator);
        TypedDependency leftDep =
            new TypedDependency(leftRel, mediatorWord, node1Word);
        dependencies.add(leftDep);
      }
    }

    for (Integer entityIndex : types.keySet()) {
      for (Pair<String, Integer> type : types.get(entityIndex)) {
        Integer modifierIndex = type.getRight();
        String entityTypeString = type.getLeft();

        LexicalItem parentNode = leaves.get(entityIndex);
        LexicalItem modifierNode = leaves.get(modifierIndex);

        GrammaticalRelation rel =
            getGrammaticalRelation(entityTypeString, "en_t");
        IndexedWord parentWord = makeWord(parentNode);
        IndexedWord modifierWord = makeWord(modifierNode);
        TypedDependency dep =
            new TypedDependency(rel, parentWord, modifierWord);
        dependencies.add(dep);
      }
    }

    for (Integer eventIndex : eventTypes.keySet()) {
      for (Pair<String, Integer> type : eventTypes.get(eventIndex)) {
        Integer modifierIndex = type.getRight();
        String entityTypeString = type.getLeft();

        LexicalItem parentNode = leaves.get(eventIndex);
        LexicalItem modifierNode = leaves.get(modifierIndex);

        GrammaticalRelation rel =
            getGrammaticalRelation(entityTypeString, "ev_t");
        IndexedWord parentWord = makeWord(parentNode);
        IndexedWord modifierWord = makeWord(modifierNode);
        TypedDependency dep =
            new TypedDependency(rel, parentWord, modifierWord);
        dependencies.add(dep);
      }
    }

    for (Integer eventIndex : eventEventModifiers.keySet()) {
      for (Pair<String, Integer> type : eventEventModifiers.get(eventIndex)) {
        Integer modifierIndex = type.getRight();
        String entityTypeString = type.getLeft();

        LexicalItem parentNode = leaves.get(eventIndex);
        LexicalItem modifierNode = leaves.get(modifierIndex);

        GrammaticalRelation rel =
            getGrammaticalRelation(entityTypeString, "ev_ev");
        IndexedWord parentWord = makeWord(parentNode);
        IndexedWord modifierWord = makeWord(modifierNode);
        TypedDependency dep =
            new TypedDependency(rel, parentWord, modifierWord);
        dependencies.add(dep);
      }
    }

    return new SemanticGraph(dependencies);
  }

  private void lexicaliseArgumentsToDomainEntities(List<LexicalItem> leaves,
      JsonObject jsonSentence) {
    JsonArray entities;
    if (jsonSentence.has("entities"))
      entities = jsonSentence.getAsJsonArray("entities");
    else
      entities = new JsonArray();

    JsonArray words = jsonSentence.getAsJsonArray("words");
    List<JsonObject> wordObjects = Lists.newArrayList();
    for (JsonElement word : words) {
      JsonObject wordObject = word.getAsJsonObject();
      wordObjects.add(wordObject);
    }

    for (int i = 0; i < leaves.size(); i++) {
      JsonObject wordObject = wordObjects.get(i);
      LexicalItem leaf = leaves.get(i);
      setNumericalMids(wordObject, leaf);
    }

    // mids from freebase annotation or geoquery entity recognition
    for (JsonElement entityElement : entities) {
      JsonObject entityObject = entityElement.getAsJsonObject();
      int index = entityObject.get("index").getAsInt();
      String mid = entityObject.get("entity").getAsString();
      leaves.get(index).setMID(mid);
    }
  }

  public static Map<String, String> cardinalTypes = ImmutableMap
      .<String, String>builder().put("I-DAT", "type.datetime")
      .put("DATE", "type.datetime").put("PERCENT", "type.float")
      .put("TIME", "type.datetime").put("MONEY", "type.float")
      .put("CD.int", "type.int").put("CD.float", "type.float")
      .put("FLOAT", "type.float").put("INT", "type.int").build();
  private static Pattern floatPattern = Pattern.compile(".*[\\.][0-9].*");
  private static Pattern datePattern = Pattern.compile("[0-9]{3,4}");

  public static void setNumericalMids(JsonObject wordObject, LexicalItem leaf) {
    String stanfordNer = "";
    if (wordObject.has("ner")) {
      stanfordNer = wordObject.get("ner").getAsString();
    }
    String word = leaf.getWord();
    String ner = leaf.getNeType();
    String posTag = leaf.getPos();

    if (datePattern.matcher(word).matches()) {
      ner = "DATE";
    } else if (posTag.equals("CD")) {
      if (floatPattern.matcher(word).matches()) {
        posTag = "CD.float";
      } else {
        posTag = "CD.int";
      }
    }
    String mid =
        cardinalTypes.containsKey(ner) ? cardinalTypes.get(ner)
            : (cardinalTypes.containsKey(stanfordNer) ? cardinalTypes
                .get(stanfordNer)
                : (cardinalTypes.containsKey(posTag) ? cardinalTypes
                    .get(posTag) : null));

    if (mid != null) {
      leaf.setMid(mid);
    }
  }

  public List<LexicalGraph> createGroundedGraph(LexicalGraph graph,
      int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean groundEntityVariableEdges,
      boolean groundEntityEntityEdges, boolean useEmtpyTypes,
      boolean ignoreTypes, boolean testing) {
    return createGroundedGraph(graph, null, nbestEdges, nbestGraphs,
        useEntityTypes, useKB, groundFreeVariables, groundEntityVariableEdges,
        groundEntityEntityEdges, useEmtpyTypes, ignoreTypes, testing);
  }

  public List<LexicalGraph> createGroundedGraph(LexicalGraph graph,
      Set<LexicalItem> restrictedNodes, int nbestEdges, int nbestGraphs,
      boolean useEntityTypes, boolean useKB, boolean groundFreeVariables,
      boolean groundEntityVariableEdges, boolean groundEntityEntityEdges,
      boolean useEmtpyTypes, boolean ignoreTypes, boolean testing) {
    return createGroundedGraph(graph, restrictedNodes, null, null, nbestEdges,
        nbestGraphs, useEntityTypes, useKB, groundFreeVariables,
        groundEntityVariableEdges, groundEntityEntityEdges, useEmtpyTypes,
        ignoreTypes, testing);
  }

  public List<LexicalGraph> createGroundedGraph(
      LexicalGraph graph,
      Set<LexicalItem> restrictedNodes,
      Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints,
      Set<LexicalItem> nonMergableNodes, int nbestEdges, int nbestGraphs,
      boolean useEntityTypes, boolean useKB, boolean groundFreeVariables,
      boolean groundEntityVariableEdges, boolean groundEntityEntityEdges,
      boolean useEmtpyTypes, boolean ignoreTypes, boolean testing) {

    List<LexicalGraph> groundedGraphs =
        createGroundedGraphPrivate(graph, restrictedNodes,
            edgeGroundingConstraints, nonMergableNodes,
            new ConcurrentHashMap<>(), nbestEdges, nbestGraphs, useEntityTypes,
            useKB, groundFreeVariables, groundEntityVariableEdges,
            groundEntityEntityEdges, useEmtpyTypes, ignoreTypes, testing);

    // Add features from ungrounded graph such as entity overlap features,
    // entity score features.
    if (graph.getFeatures().size() > 0) {
      for (LexicalGraph gGraph : groundedGraphs) {
        gGraph.getFeatures().addAll(graph.getFeatures());
        gGraph.setScore(getScore(gGraph, testing));
      }
      Collections.sort(groundedGraphs); // this sorts in descending order
    }

    return groundedGraphs;
  }

  private List<LexicalGraph> createGroundedGraphPrivate(
      LexicalGraph graph,
      Set<LexicalItem> restrictedNodes,
      Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints,
      Set<LexicalItem> nonMergableNodes,
      ConcurrentHashMap<Pair<Integer, Integer>, Boolean> graphsSoFar,
      int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean groundEntityVariableEdges,
      boolean groundEntityEntityEdges, boolean useEmtpyTypes,
      boolean ignoreTypes, boolean testing) {

    TreeSet<Edge<LexicalItem>> edges = graph.getEdges();
    List<LexicalGraph> groundedGraphs = Lists.newArrayList();
    if (edges.size() == 0)
      return groundedGraphs;

    // See if there are any question nodes and ignore the graphs that do not
    // have an edge from question node
    Map<LexicalItem, Set<Property>> graphProperties = graph.getProperties();
    LexicalItem questionOrCountNode = null;
    for (LexicalItem node : graphProperties.keySet()) {
      questionOrCountNode =
          graph.isQuestionNode(node) || graph.isCountNode(node) ? node : null;
      if (questionOrCountNode != null)
        break;
    }

    LexicalGraph groundedGraph = new LexicalGraph();
    groundedGraph.setParallelGraph(graph);

    // Properties are universal across both ungrounded and grounded graphs.
    groundedGraph.addProperties(graph.getProperties());
    groundedGraph.setActualNodes(graph.getActualNodes());
    groundedGraphs.add(groundedGraph);
    graphsSoFar.put(Pair.of(groundedGraph.hashCode(), groundedGraph
        .getParallelGraph().hashCode()), true);
    // graphsSoFar.add(groundedGraph);

    Set<LexicalItem> nodesCovered = Sets.newHashSet();
    for (Edge<LexicalItem> edge : edges) {
      LexicalItem node1 = edge.getLeft();
      LexicalItem node2 = edge.getRight();

      // Graphs formed using MERGE operation.
      List<LexicalGraph> mergedGraphs = new ArrayList<>();
      if (allowMerging) {
        mergedGraphs =
            mergeEdge(groundedGraphs, edge, restrictedNodes,
                edgeGroundingConstraints, nonMergableNodes, graphsSoFar,
                nbestEdges, nbestGraphs, useEntityTypes, useKB,
                groundFreeVariables, groundEntityVariableEdges,
                groundEntityEntityEdges, testing, ignoreTypes);
      }

      // ground the edge.
      groundedGraphs.addAll(groundTheEdge(groundedGraphs, edge,
          restrictedNodes, edgeGroundingConstraints, graphsSoFar, nbestEdges,
          nbestGraphs, useEntityTypes, useKB, groundFreeVariables,
          groundEntityVariableEdges, groundEntityEntityEdges, testing));
      groundedGraphs.addAll(mergedGraphs);

      // sort descending order.
      Collections.sort(groundedGraphs);
      groundedGraphs =
          groundedGraphs.size() < nbestGraphs ? groundedGraphs : groundedGraphs
              .subList(0, nbestGraphs);

      if (!ignoreTypes) {
        // ground the node types.
        if (!nodesCovered.contains(node1)) {
          nodesCovered.add(node1);
          if (graph.getTypes(node1) != null) {
            List<Type<LexicalItem>> nodeTypes =
                Lists.newArrayList(graph.getTypes(node1));

            Type<LexicalItem> nodeType = nodeTypes.get(0);
            List<Type<LexicalItem>> additionalNodeTypes =
                (nodeTypes.size() > 1) ? nodeTypes.subList(1, nodeTypes.size())
                    : new ArrayList<>();

            groundedGraphs.addAll(groundTheType(groundedGraphs, nodeType,
                additionalNodeTypes, restrictedNodes, nbestEdges, nbestGraphs,
                useEntityTypes, useKB, groundFreeVariables, useEmtpyTypes,
                testing));
          }
        }

        // ground the node types
        if (!nodesCovered.contains(node2)) {
          nodesCovered.add(node2);

          if (graph.getTypes(node2) != null) {
            List<Type<LexicalItem>> nodeTypes =
                Lists.newArrayList(graph.getTypes(node2));
            Type<LexicalItem> nodeType = nodeTypes.get(0);
            List<Type<LexicalItem>> additionalNodeTypes =
                nodeTypes.size() > 1 ? nodeTypes.subList(1, nodeTypes.size())
                    : new ArrayList<>();
            groundedGraphs.addAll(groundTheType(groundedGraphs, nodeType,
                additionalNodeTypes, restrictedNodes, nbestEdges, nbestGraphs,
                useEntityTypes, useKB, groundFreeVariables, useEmtpyTypes,
                testing));
          }
        }

        // sort descending order
        Collections.sort(groundedGraphs);
        groundedGraphs =
            groundedGraphs.size() < nbestGraphs ? groundedGraphs
                : groundedGraphs.subList(0, nbestGraphs);
      }
    }

    if (questionOrCountNode != null) {
      List<LexicalGraph> validGroundedGraphs = Lists.newArrayList();
      for (LexicalGraph gGraph : groundedGraphs) {
        HashSet<LexicalItem> countNodes = gGraph.getCountNode();

        // If there is a count node, assuming that is the variable of interest.
        LexicalItem questionOrCountNodeNew =
            countNodes.size() > 0 ? gGraph.getUnifiedNode(countNodes.iterator()
                .next()) : gGraph.getUnifiedNode(questionOrCountNode);
        Set<Edge<LexicalItem>> questionNodeEdges =
            gGraph.getEdges(questionOrCountNodeNew);
        if (questionNodeEdges != null && questionNodeEdges.size() > 0)
          validGroundedGraphs.add(gGraph);
      }
      groundedGraphs = validGroundedGraphs;
    }

    if (graphIsConnectedFlag) {
      for (LexicalGraph gGraph : groundedGraphs) {
        boolean connected = gGraph.isConnected();
        GraphIsConnectedFeature graphIsConnectedFeature =
            connected ? new GraphIsConnectedFeature(true)
                : new GraphIsConnectedFeature(false);
        gGraph.addFeature(graphIsConnectedFeature);
        if (connected) {
          if (testing) {
            gGraph
                .setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
          } else {
            gGraph
                .setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
          }
        }
      }
    }

    if (graphHasEdgeFlag) {
      for (LexicalGraph gGraph : groundedGraphs) {
        boolean graphHasEdge = gGraph.getEdges().size() > 0 ? true : false;
        GraphHasEdgeFeature graphHasEdgeFeature =
            new GraphHasEdgeFeature(graphHasEdge);
        gGraph.addFeature(graphHasEdgeFeature);
        if (graphHasEdge) {
          if (testing) {
            gGraph
                .setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
          } else {
            gGraph
                .setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
          }
        }
      }
    }

    if (countNodesFlag) {
      for (LexicalGraph gGraph : groundedGraphs) {
        int nodeCount = gGraph.mainNodesCount();
        GraphNodeCountFeature graphNodeCountFeature =
            new GraphNodeCountFeature(new Double(nodeCount));
        gGraph.addFeature(graphNodeCountFeature);
        if (testing) {
          gGraph.setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
        } else {
          gGraph.setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
        }
      }
    }

    if (edgeNodeCountFlag) {
      for (LexicalGraph gGraph : groundedGraphs) {
        List<LexicalItem> connectedNodes = gGraph.getNodesConnectedByEdges();
        int nodeCount = connectedNodes.size();

        /*- // adding node counts when integers are present .e.g. 120000 people work in Novartis
         * check getNodesConnectedByEdges since count node are not counted there.
        for (LexicalItem node : connectedNodes) {
        	if (node.getMid().equals("type.int")) {
        		Set<Type<LexicalItem>> nodeTypes = gGraph.getTypes(node);
        		if (nodeTypes != null && nodeTypes.size() > 0)
        			nodeCount += 1;
        	}
        }*/

        EdgeNodeCountFeature edgeNodeCountFeature =
            new EdgeNodeCountFeature(new Double(nodeCount));
        gGraph.addFeature(edgeNodeCountFeature);
        if (testing) {
          gGraph.setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
        } else {
          gGraph.setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
        }
      }
    }

    Collections.sort(groundedGraphs);
    groundedGraphs =
        groundedGraphs.size() < nbestGraphs ? groundedGraphs : groundedGraphs
            .subList(0, nbestGraphs);

    // Set the syntactic and semantic parse to the corresponding ungrounded
    // graph.
    for (LexicalGraph gGraph : groundedGraphs) {
      if (graph.getSyntacticParse() != null) {
        gGraph.setSyntacticParse(graph.getSyntacticParse());
      }
      gGraph.setSemanticParse(graph.getSemanticParse());
    }
    return groundedGraphs;
  }

  private List<LexicalGraph> mergeEdge(
      List<LexicalGraph> groundedGraphs,
      Edge<LexicalItem> edge,
      Set<LexicalItem> restrictedNodes,
      Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints,
      Set<LexicalItem> nonMergableNodes,
      ConcurrentHashMap<Pair<Integer, Integer>, Boolean> graphsSoFar,
      int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean groundEntityVariableEdges,
      boolean groundEntityEntityEdges, boolean testing, boolean ignoreTypes) {
    LexicalItem node1Old = edge.getLeft();
    LexicalItem node2Old = edge.getRight();
    List<LexicalGraph> mergedGraphs = new ArrayList<>();
    for (LexicalGraph gGraph : groundedGraphs) {
      LexicalItem node1 = gGraph.getUnifiedNode(node1Old);
      LexicalItem node2 = gGraph.getUnifiedNode(node2Old);

      if (!node1.isEntity() && !node2.isEntity()) {
        // The way you combine two nodes should be consistent, or else you will
        // end up with conflicting features. This effect is empirically
        // significant.
        int order =
            edge.getRelation().getLeft()
                .compareTo(edge.getRelation().getRight());
        if (order == 0) {
          order = node1.getPos().compareTo(node2.getPos());
        }

        if (order <= 0) {
          mergedGraphs.addAll(mergeNodes(gGraph, node1, node2, restrictedNodes,
              edgeGroundingConstraints, nonMergableNodes, graphsSoFar,
              nbestEdges, nbestGraphs, useEntityTypes, useKB,
              groundFreeVariables, groundEntityVariableEdges,
              groundEntityEntityEdges, testing, ignoreTypes));
        } else {
          mergedGraphs.addAll(mergeNodes(gGraph, node2, node1, restrictedNodes,
              edgeGroundingConstraints, nonMergableNodes, graphsSoFar,
              nbestEdges, nbestGraphs, useEntityTypes, useKB,
              groundFreeVariables, groundEntityVariableEdges,
              groundEntityEntityEdges, testing, ignoreTypes));
        }
      } else {
        mergedGraphs.addAll(mergeNodes(gGraph, node1, node2, restrictedNodes,
            edgeGroundingConstraints, nonMergableNodes, graphsSoFar,
            nbestEdges, nbestGraphs, useEntityTypes, useKB,
            groundFreeVariables, groundEntityVariableEdges,
            groundEntityEntityEdges, testing, ignoreTypes));
        mergedGraphs.addAll(mergeNodes(gGraph, node2, node1, restrictedNodes,
            edgeGroundingConstraints, nonMergableNodes, graphsSoFar,
            nbestEdges, nbestGraphs, useEntityTypes, useKB,
            groundFreeVariables, groundEntityVariableEdges,
            groundEntityEntityEdges, testing, ignoreTypes));
      }
    }
    return mergedGraphs;
  }

  private List<LexicalGraph> mergeNodes(
      LexicalGraph gGraph,
      LexicalItem parentNode,
      LexicalItem childNode,
      Set<LexicalItem> restrictedNodes,
      Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints,
      Set<LexicalItem> nonMergableNodes,
      ConcurrentHashMap<Pair<Integer, Integer>, Boolean> graphsSoFar,
      int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean groundEntityVariableEdges,
      boolean groundEntityEntityEdges, boolean testing, boolean ignoreTypes) {
    List<LexicalGraph> mergedGraphs = new ArrayList<>();

    // Ignore cyclic edges.
    if (parentNode.equals(childNode))
      return mergedGraphs;

    HashSet<LexicalItem> revisednonMergableNodes = new HashSet<>();
    if (nonMergableNodes != null)
      nonMergableNodes.forEach(x -> revisednonMergableNodes.add(gGraph
          .getUnifiedNode(x)));

    // If childNode represents a node that cannot be merged, return.
    if (revisednonMergableNodes.contains(childNode))
      return mergedGraphs;

    // Compute revised edge grounding constraints.
    HashMap<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> revisedEdgeGroundingConstraints =
        new HashMap<>();
    if (edgeGroundingConstraints != null) {
      for (Entry<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> entry : edgeGroundingConstraints
          .entrySet()) {
        LexicalItem newNode1 = gGraph.getUnifiedNode(entry.getKey().getLeft());
        LexicalItem newNode2 = gGraph.getUnifiedNode(entry.getKey().getRight());
        revisedEdgeGroundingConstraints.put(Pair.of(newNode1, newNode2),
            entry.getValue());
      }
    }

    // Edges that should appear in the final graph cannot be merged.
    if (revisedEdgeGroundingConstraints.containsKey(Pair.of(parentNode,
        childNode)))
      return mergedGraphs;

    if (gGraph.getEdges(childNode) != null
        && gGraph.getEdges(parentNode) != null) {
      // Check if a grounded edge exists between the two nodes.
      Set<Edge<LexicalItem>> commonNodes =
          new HashSet<>(gGraph.getEdges(childNode));

      // TreeSet and HashSet intersection behaves weirdly.
      commonNodes.retainAll(new HashSet<>(gGraph.getEdges(parentNode)));
      if (commonNodes.size() > 0) {
        // A grounded graph with these nodes merged should already exist. Do not
        // merge this nodes.
        return mergedGraphs;
      }
    }

    LexicalGraph mergedGraph = gGraph.copy();
    Set<Edge<LexicalItem>> childEdges = mergedGraph.getEdges(childNode);
    Set<Edge<LexicalItem>> toBeGroundedEgdes = new TreeSet<>();

    if (childEdges != null) {
      childEdges = new HashSet<>(childEdges);
      for (Edge<LexicalItem> childEdge : childEdges) {
        Pair<Edge<LexicalItem>, Edge<LexicalItem>> edgePair =
            mergedGraph.getGroundedToUngroundedEdges(childEdge);

        Edge<LexicalItem> ungroundedEdge = edgePair.getRight();
        Edge<LexicalItem> groundedEdge = edgePair.getLeft();

        mergedGraph.removeEdge(childEdge);

        // TODO: May over-generate negative features like ArgGrel* when a node
        // that has merged merges again. This can be solved by revising the
        // parent node groundings. Handle these if they become a pain.
        List<Feature> childFeatures =
            getEdgeFeatures(mergedGraph, ungroundedEdge.getLeft(),
                ungroundedEdge.getRight(), ungroundedEdge.getMediator(),
                ungroundedEdge.getRelation(), groundedEdge.getRelation());
        for (Feature feature : childFeatures) {
          feature.setFeatureValue(feature.getFeatureValue() * -1.0);
          mergedGraph.addFeature(feature);
        }
        mergedGraph.removeGroundedToUngroundedEdges(childEdge);

        if (childEdge.getLeft().equals(groundedEdge.getLeft())) {
          toBeGroundedEgdes.add(new Edge<>(parentNode, ungroundedEdge
              .getRight(), ungroundedEdge.getMediator(), ungroundedEdge
              .getRelation()));
        } else {
          toBeGroundedEgdes.add(new Edge<>(ungroundedEdge.getLeft(),
              parentNode, ungroundedEdge.getMediator(), ungroundedEdge
                  .getRelation()));
        }
      }
    }

    HashSet<Edge<LexicalItem>> mergedEgdes =
        new HashSet<>(mergedGraph.getParallelGraph().getEdges(childNode));
    // TreeSet and HashSet intersection behaves weirdly.
    mergedEgdes.retainAll(new HashSet<>(mergedGraph.getParallelGraph()
        .getEdges(parentNode)));
    for (Edge<LexicalItem> mergedEdge : mergedEgdes) {
      String childIsEntity = childNode.isEntity() ? "1" : "0";
      String parentIsEntity = parentNode.isEntity() ? "1" : "0";
      List<?> key =
          Lists.newArrayList(mergedEdge.getRelation().getLeft(), mergedEdge
              .getRelation().getRight(), childIsEntity, parentIsEntity);
      MergedEdgeFeature mergedFeature = new MergedEdgeFeature(key, 1.0);
      mergedGraph.addFeature(mergedFeature);
      learningModel.setWeightIfAbsent(mergedFeature, -0.5);

      /*-key =
          Lists.newArrayList(childNode.getPos(), parentNode.getPos(),
              childIsEntity, parentIsEntity);
      mergedFeature = new MergedEdgeFeature(key, 1.0);
      mergedGraph.addFeature(mergedFeature);
      learningModel.setWeightIfAbsent(mergedFeature, -2.0);*/

      /*-// Add a feature indicating the edge has been merged.
      MergedEdgeFeature hasMergedEdge =
          new MergedEdgeFeature(Lists.newArrayList(Boolean.TRUE), 1.0);
      mergedGraph.addFeature(hasMergedEdge);*/
    }

    // Remove the type grounding features which came from child node.
    // Currently, new type groundings that arrive after merging are ignored.
    TreeSet<Type<LexicalItem>> childUngroundedTypes =
        mergedGraph.getParallelGraph().getTypes(childNode);
    if (!ignoreTypes && mergedGraph.getTypes(childNode) != null
        && childUngroundedTypes != null && childUngroundedTypes.size() > 0) {
      TreeSet<Type<LexicalItem>> childTypes =
          new TreeSet<>(mergedGraph.getTypes(childNode));
      for (Type<LexicalItem> ungroundedType : childUngroundedTypes) {
        List<Feature> childFeatures =
            getAdditionalTypeFeatures(ungroundedType, childTypes.first()
                .getEntityType());
        for (Feature feature : childFeatures) {
          feature.setFeatureValue(feature.getFeatureValue() * -1.0);
          mergedGraph.addFeature(feature);
        }

        for (Type<LexicalItem> childType : childTypes) {
          mergedGraph.removeType(childType);
        }
      }
      mergedGraph.getNodeTypes().remove(childNode);
    }

    // Transform the graph before grounding.
    mergedGraph.updateMergedReferences(parentNode, childNode);
    mergedGraph.increaseMergeCount();
    mergedGraph.setScore(getScore(mergedGraph, testing));

    if (graphsSoFar.putIfAbsent(Pair.of(mergedGraph.hashCode(), mergedGraph
        .getParallelGraph().hashCode()), true) == null) {
      mergedGraphs.add(mergedGraph);
    }

    // Revise parent node features.
    Set<Edge<LexicalItem>> parentEdges = gGraph.getEdges(parentNode);
    if (parentEdges != null) {
      parentEdges = new HashSet<>(parentEdges);
      for (Edge<LexicalItem> parentEdge : parentEdges) {
        Pair<Edge<LexicalItem>, Edge<LexicalItem>> edgePair =
            gGraph.getGroundedToUngroundedEdges(parentEdge);

        Edge<LexicalItem> ungroundedEdge = edgePair.getRight();
        Edge<LexicalItem> groundedEdge = edgePair.getLeft();

        List<Feature> parentFeatures =
            getEdgeFeatures(gGraph, ungroundedEdge.getLeft(),
                ungroundedEdge.getRight(), ungroundedEdge.getMediator(),
                ungroundedEdge.getRelation(), groundedEdge.getRelation());
        for (Feature feature : parentFeatures) {
          feature.setFeatureValue(feature.getFeatureValue() * -1.0);
          mergedGraph.addFeature(feature);
        }

        List<Feature> parentFeaturesNew =
            getEdgeFeatures(mergedGraph, ungroundedEdge.getLeft(),
                ungroundedEdge.getRight(), ungroundedEdge.getMediator(),
                ungroundedEdge.getRelation(), groundedEdge.getRelation());
        for (Feature feature : parentFeaturesNew) {
          mergedGraph.addFeature(feature);
        }
      }
    }

    for (Edge<LexicalItem> edge : toBeGroundedEgdes) {
      mergedGraphs.addAll(groundTheEdge(mergedGraphs, edge, restrictedNodes,
          edgeGroundingConstraints, graphsSoFar, nbestEdges, nbestGraphs,
          useEntityTypes, useKB, groundFreeVariables,
          groundEntityVariableEdges, groundEntityEntityEdges, testing));
    }

    return mergedGraphs;
  }

  private static Set<String> standardTypes = Sets.newHashSet("type.datetime",
      "type.int", "type.float");

  public static boolean nodeIsLexicalised(LexicalItem node) {
    return (standardTypes.contains(node.getMid()) || node.getMid().startsWith(
        "m."));
  }

  private List<LexicalGraph> groundTheType(List<LexicalGraph> groundedGraphs,
      Type<LexicalItem> nodeType, List<Type<LexicalItem>> additionalNodeTypes,
      Set<LexicalItem> restrictedNodes, int nbestTypes, int nbestGraphs,
      boolean useEntityTypes, boolean useKB, boolean groundFreeVariables,
      boolean useEmptyTypes, boolean testing) {

    List<LexicalGraph> tempGraphs = Lists.newArrayList();
    for (LexicalGraph oldGraph : groundedGraphs) {
      EntityType unGroundedEntityType = nodeType.getEntityType();
      LexicalItem parentNode =
          oldGraph.getUnifiedNode(nodeType.getParentNode());
      LexicalItem modifierNode = nodeType.getModifierNode();
      String entity = parentNode.getWord();
      Set<LexicalItem> revisedRestrictedNodes = new HashSet<>();
      if (restrictedNodes != null)
        restrictedNodes.forEach(x -> revisedRestrictedNodes.add(x));
      if (!revisedRestrictedNodes.contains(parentNode))
        entity = parentNode.getMid();

      List<EntityType> groundedEntityTypes = null;

      if ((useKB || useEntityTypes) && kb.hasEntity(entity)) {
        Set<String> gtypes = kb.getTypes(entity);
        if (gtypes != null) {
          groundedEntityTypes = Lists.newArrayList();
          for (String gtype : gtypes) {
            EntityType groundedEntityType = new EntityType(gtype);
            Double prob =
                groundedLexicon.getUtypeGtypeProb(
                    unGroundedEntityType.getType(),
                    groundedEntityType.getType());
            groundedEntityType.setWeight(prob);
            groundedEntityTypes.add(groundedEntityType);
          }
          Collections.sort(groundedEntityTypes);
        }
      } else if (groundFreeVariables) {
        List<EntityType> entityTypes =
            groundedLexicon.getGroundedTypes(unGroundedEntityType);
        if (entityTypes != null)
          groundedEntityTypes = Lists.newArrayList(entityTypes);
      }

      // Comment this in case if EMPTY edge features are useless
      if (useEmptyTypes) {
        if (groundedEntityTypes == null)
          groundedEntityTypes = Lists.newArrayList();
        EntityType emptyGroundedEntityType = new EntityType("type.empty");
        emptyGroundedEntityType.setWeight(1.0);
        groundedEntityTypes.add(0, emptyGroundedEntityType);
      }

      if (groundedEntityTypes == null)
        continue;

      int nbestCount = 0;
      for (EntityType groundedEntityType : groundedEntityTypes) {
        nbestCount++;
        if (nbestCount > nbestTypes)
          break;

        // if the entity type is basic, check if the edges going out
        // satisfy that constraint.
        boolean nodeTypeSatisifiesEdgeType = true;
        if (standardTypes.contains(groundedEntityType.getType())) {
          Set<Edge<LexicalItem>> parentEdges = oldGraph.getEdges(parentNode);
          if (parentEdges == null)
            parentEdges = Sets.newHashSet();
          for (Edge<LexicalItem> edge : parentEdges) {
            String edgeName =
                edge.getLeft().equals(parentNode) ? edge.getRelation()
                    .getLeft() : edge.getRelation().getRight();
            ArrayList<String> edgeParts =
                Lists.newArrayList(Splitter.on(".").split(edgeName));
            int edgePartsLength = edgeParts.size();
            if (edgeParts.get(edgePartsLength - 1).equals("1")) {
              String edgeActualName =
                  Joiner.on(".")
                      .join(edgeParts.subList(0, edgePartsLength - 1));
              String nodeTypeFromEdge =
                  schema.getRelationArguments(edgeActualName).get(0);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            } else if (edgeParts.get(edgePartsLength - 1).equals("2")) {
              String edgeActualName =
                  Joiner.on(".")
                      .join(edgeParts.subList(0, edgePartsLength - 1));
              String nodeTypeFromEdge =
                  schema.getRelationArguments(edgeActualName).get(1);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            } else if (edgeParts.get(edgePartsLength - 1).equals("inverse")) {
              String edgeActualName =
                  Joiner.on(".")
                      .join(edgeParts.subList(0, edgePartsLength - 1));
              String nodeTypeFromEdge =
                  schema.getRelationArguments(edgeActualName).get(0);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            } else {
              String edgeActualName = edgeName;
              String nodeTypeFromEdge =
                  schema.getRelationArguments(edgeActualName).get(1);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            }
          }
        }

        if (nodeTypeSatisifiesEdgeType == false)
          continue;

        LexicalGraph newGraph = oldGraph.copy();
        if (newGraph.getTypes(parentNode) != null
            && newGraph.getTypes(parentNode).size() > 0) {
          newGraph.getFeatures().addAll(
              getAdditionalTypeFeatures(nodeType, groundedEntityType));
          newGraph.addType(parentNode, modifierNode, groundedEntityType);
        } else {
          newGraph.getFeatures().addAll(
              getTypeMainFeatures(newGraph, nodeType, groundedEntityType));
          newGraph.addType(parentNode, modifierNode, groundedEntityType);
        }

        // adding node types from additional modifiers of the same type.
        // e.g. for society additional modifiers are society.secret.1
        // This does not work for cases like director and producer. In
        // such cases, you have to ground each entity type separately.
        for (Type<LexicalItem> additionalNodeType : additionalNodeTypes) {
          newGraph.getFeatures()
              .addAll(
                  getAdditionalTypeFeatures(additionalNodeType,
                      groundedEntityType));
          newGraph.addType(additionalNodeType.getParentNode(),
              additionalNodeType.getModifierNode(), groundedEntityType);
        }

        Double score;
        if (testing) {
          score = learningModel.getScoreTesting(newGraph.getFeatures());
        } else {
          score = learningModel.getScoreTraining(newGraph.getFeatures());
        }
        newGraph.setScore(score);
        tempGraphs.add(newGraph);
      }
    }

    return tempGraphs;
  }

  private List<Feature> getTypeMainFeatures(LexicalGraph newGraph,
      Type<LexicalItem> nodeType, EntityType groundedEntityType) {
    LexicalItem parentNode = nodeType.getParentNode();
    List<Feature> features = new ArrayList<>();
    List<?> featureKey;
    // adding utype gtype features
    if (utypeGtypeFlag) {
      EntityType unGroundedEntityType = nodeType.getEntityType();
      featureKey = Lists.newArrayList(unGroundedEntityType, groundedEntityType);
      UtypeGtypeFeature utypeGtypeFeature =
          new UtypeGtypeFeature(featureKey, 1.0);
      features.add(utypeGtypeFeature);
      if (!learningModel.containsFeature(utypeGtypeFeature)) {
        double value;
        if (useLexiconWeightsType)
          value = groundedEntityType.getWeight() + initialTypeWeight;
        else
          value = initialTypeWeight;

        learningModel.setWeightIfAbsent(utypeGtypeFeature, value);
      }
    }

    // adding gtype grel features
    if (gtypeGrelPartFlag) {
      Set<Edge<LexicalItem>> nodeEdges = newGraph.getEdges(parentNode);
      if (nodeEdges != null) {
        for (Edge<LexicalItem> nodeEdge : nodeEdges) {
          Relation groundedRelation = nodeEdge.getRelation();
          String grelPart = groundedRelation.getLeft();
          String entityType = groundedEntityType.getType();
          featureKey = Lists.newArrayList(entityType, grelPart);
          GtypeGrelPartFeature gtypeGrelPartFeature =
              new GtypeGrelPartFeature(featureKey, 1.0);
          newGraph.addFeature(gtypeGrelPartFeature);
        }
      }
    }
    return features;
  }

  private List<Feature> getAdditionalTypeFeatures(
      Type<LexicalItem> additionalNodeType, EntityType groundedEntityType) {
    List<Feature> features = new ArrayList<>();
    if (utypeGtypeFlag) {
      // adding utype gtype features for additional types.
      EntityType unGroundedAdditionalEntityType =
          additionalNodeType.getEntityType();
      Double prob =
          groundedLexicon.getUtypeGtypeProb(
              unGroundedAdditionalEntityType.getType(),
              groundedEntityType.getType());
      List<?> featureKey =
          Lists
              .newArrayList(unGroundedAdditionalEntityType, groundedEntityType);
      UtypeGtypeFeature utypeGtypeFeature =
          new UtypeGtypeFeature(featureKey, 1.0);
      features.add(utypeGtypeFeature);
      if (!learningModel.containsFeature(utypeGtypeFeature)) {
        double value;
        if (useLexiconWeightsType)
          value = prob + initialTypeWeight;
        else
          value = initialTypeWeight;

        learningModel.setWeightIfAbsent(utypeGtypeFeature, value);
      }
    }
    return features;
  }

  public boolean stringContainsWord(String grelLeftStripped, String modifierWord) {
    if (!stems.containsKey(modifierWord))
      stems.put(modifierWord, PorterStemmer.getStem(modifierWord));
    String modifierStem = stems.get(modifierWord);
    Iterator<String> it =
        Splitter.on(CharMatcher.anyOf("._")).trimResults().omitEmptyStrings()
            .split(grelLeftStripped).iterator();
    while (it.hasNext()) {
      String part = it.next();
      if (!stems.containsKey(part))
        stems.put(part, PorterStemmer.getStem(part));
      String partStem = stems.get(part);
      if (partStem.equals(modifierStem))
        return true;
      // if (part.startsWith(modifierWord))
      // return true;
    }
    return false;
  }

  private List<LexicalGraph> groundTheEdge(
      List<LexicalGraph> groundedGraphs,
      Edge<LexicalItem> edge,
      Set<LexicalItem> restrictedNodes,
      Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints,
      ConcurrentHashMap<Pair<Integer, Integer>, Boolean> graphsSoFar,
      int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean groundEntityVariableEdges,
      boolean groundEntityEntityEdges, boolean testing) {

    List<LexicalGraph> tempGraphs = Lists.newArrayList();
    for (LexicalGraph oldGraph : groundedGraphs) {
      // Add Graphs with edges between node1 and node2.
      Relation ungroundedRelation = edge.getRelation();
      LexicalItem node1 = oldGraph.getUnifiedNode(edge.getLeft());
      LexicalItem node2 = oldGraph.getUnifiedNode(edge.getRight());
      LexicalItem mediator = edge.getMediator();

      if (node1.equals(node2))
        continue;

      // Check if a grounded edge exists between the two nodes.
      if (oldGraph.getEdges(node1) != null && oldGraph.getEdges(node2) != null) {
        Set<Edge<LexicalItem>> commonNodes =
            new HashSet<>(oldGraph.getEdges(node1));

        // TreeSet and HashSet intersection behaves weirdly.
        commonNodes.retainAll(new HashSet<>(oldGraph.getEdges(node2)));
        if (commonNodes.size() > 0) {
          // A grounded graph with these nodes merged should already exist. Do
          // not merge this nodes.
          continue;
        }
      }

      Set<LexicalItem> revisedRestrictedNodes = new HashSet<>();
      if (restrictedNodes != null) {
        restrictedNodes.forEach(x -> revisedRestrictedNodes.add(oldGraph
            .getUnifiedNode(x)));
      }

      // Compute revised edge grounding constraints.
      HashMap<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> revisedEdgeGroundingConstraints =
          new HashMap<>();
      if (edgeGroundingConstraints != null) {
        for (Entry<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> entry : edgeGroundingConstraints
            .entrySet()) {
          LexicalItem newNode1 =
              oldGraph.getUnifiedNode(entry.getKey().getLeft());
          LexicalItem newNode2 =
              oldGraph.getUnifiedNode(entry.getKey().getRight());
          revisedEdgeGroundingConstraints.put(Pair.of(newNode1, newNode2),
              entry.getValue());
        }
      }

      String entity1 = node1.getMid();
      String entity2 = node2.getMid();

      // useless edge since the edge contains out of domain entity
      if ((useKB || useEntityTypes) && lexicalPosTags.contains(node1.getPos())
          && !kb.hasEntity(entity1) && !entity1.equals("x")) {
        continue;
      }

      if ((useKB || useEntityTypes) && lexicalPosTags.contains(node2.getPos())
          && !kb.hasEntity(entity2) && !entity2.equals("x")) {
        continue;
      }

      // if node is not present in restrictedNodes, we can use the entity type
      if (!revisedRestrictedNodes.contains(node1))
        entity1 = node1.getMid();

      if (!revisedRestrictedNodes.contains(node2))
        entity2 = node2.getMid();

      List<Relation> groundedRelations = null;
      if (useKB && (kb.hasEntity(entity1) || kb.hasEntity(entity2))) {
        groundedRelations = Lists.newArrayList();
        Set<Relation> groundedRelationsSet = null;
        if (kb.hasEntity(entity1)
            && kb.hasEntity(entity2)
            && !(standardTypes.contains(entity1) && standardTypes
                .contains(entity2))) {
          if (groundEntityEntityEdges) {
            groundedRelationsSet = kb.getRelations(entity1, entity2);
          }
        } else if ((groundEntityVariableEdges || oldGraph.isQuestionNode(node2)
            || oldGraph.isCountNode(node2) || revisedRestrictedNodes
              .contains(node2))
            && !standardTypes.contains(entity1)
            && kb.hasEntity(entity1)) {
          groundedRelationsSet = kb.getRelations(entity1);
        } else if ((groundEntityVariableEdges || oldGraph.isQuestionNode(node1)
            || oldGraph.isCountNode(node1) || revisedRestrictedNodes
              .contains(node1))
            && !standardTypes.contains(entity2)
            && kb.hasEntity(entity2)) {
          Set<Relation> groundedRelationsSetInverse = kb.getRelations(entity2);
          if (groundedRelationsSetInverse != null) {
            groundedRelationsSet = Sets.newHashSet();
            for (Relation groundedRelation : groundedRelationsSetInverse) {
              groundedRelationsSet.add(groundedRelation.inverse());
            }
          }
        }
        if (groundedRelationsSet != null) {
          for (Relation groundedRelation : groundedRelationsSet) {
            groundedRelation = groundedRelation.copy();
            Double prob =
                groundedLexicon.getUrelGrelProb(ungroundedRelation,
                    groundedRelation);
            groundedRelation.setWeight(prob);
            groundedRelations.add(groundedRelation);
          }
        }

        Collections.sort(groundedRelations);
      } else if (groundEntityVariableEdges && groundFreeVariables) {
        // if both the entities are not in the database, we shall use
        // lexicon. One could also use the schema to explore all the
        // relations in the domain, but that would lead to large search
        // space.
        groundedRelations =
            groundedLexicon.getGroundedRelations(ungroundedRelation);
      }

      if (groundedRelations == null)
        continue;

      Set<Relation> groundedRelationsCopy = Sets.newHashSet(groundedRelations);
      for (Relation groundedRelation : groundedRelations) {
        boolean checkIfValid =
            checkValidRelation(groundedRelation, node1, node2);
        if (!checkIfValid)
          groundedRelationsCopy.remove(groundedRelation);
      }

      // Use only the allowed relations.
      if (revisedEdgeGroundingConstraints.containsKey(Pair.of(node1, node2))) {
        // TreeSet and HashSet intersection behaves weirdly.
        groundedRelationsCopy.retainAll(new HashSet<>(
            revisedEdgeGroundingConstraints.get(Pair.of(node1, node2))));
      }

      // Add each new edge to each of the old graphs
      int nbestCount = 0;
      for (Relation groundedRelation : groundedRelationsCopy) {

        nbestCount++;
        if (nbestCount > nbestEdges)
          break;

        // if database is not used, then only entity type checking is used
        if (!useKB && useEntityTypes) {
          Set<String> entity1Types =
              kb.hasEntity(entity1) ? kb.getTypes(entity1) : null;
          Set<String> entity2Types =
              kb.hasEntity(entity2) ? kb.getTypes(entity2) : null;
          if (entity1Types != null || entity2Types != null) {
            boolean isValidRelation =
                checkIsValidRelation(groundedRelation, entity1Types,
                    entity2Types);
            if (!isValidRelation)
              continue;
          }
        }

        LexicalGraph newGraph = oldGraph.copy();
        List<Feature> features =
            getEdgeFeatures(newGraph, node1, node2, mediator,
                ungroundedRelation, groundedRelation);
        Edge<LexicalItem> groundedEgde =
            new Edge<>(node1, node2, mediator, groundedRelation);
        Edge<LexicalItem> unGroundedEgde =
            new Edge<>(node1, node2, mediator, edge.getRelation());
        newGraph.addGroundedToUngroundedEdges(groundedEgde, unGroundedEgde);
        newGraph.addEdge(node1, node2, mediator, groundedRelation);
        newGraph.getFeatures().addAll(features);

        /*-// Add an indicator feature for indicating the edge has not been merged.
        if (allowMerging) {
          MergedEdgeFeature edgeIsNotMerged =
              new MergedEdgeFeature(Lists.newArrayList(Boolean.FALSE), 1.0);
          newGraph.addFeature(edgeIsNotMerged);
        }*/

        Double score = getScore(newGraph, testing);
        newGraph.setScore(score);

        if (graphsSoFar.putIfAbsent(Pair.of(newGraph.hashCode(), newGraph
            .getParallelGraph().hashCode()), true) == null) {
          tempGraphs.add(newGraph);
        }
      }
    }

    // start again with new set of graphs
    return tempGraphs;
  }

  private Double getScore(LexicalGraph gGraph, boolean testing) {
    Double score = 0.0;
    // compute score of the new graph
    if (testing) {
      score = learningModel.getScoreTesting(gGraph.getFeatures());
    } else {
      score = learningModel.getScoreTraining(gGraph.getFeatures());
    }
    return score;
  }


  private List<Feature> getEdgeFeatures(LexicalGraph gGraph, LexicalItem node1,
      LexicalItem node2, LexicalItem mediator, Relation ungroundedRelation,
      Relation groundedRelation) {
    LexicalGraph uGraph = gGraph.getParallelGraph();
    List<Feature> features = new ArrayList<>();

    String urelLeft = ungroundedRelation.getLeft();
    String grelLeft = groundedRelation.getLeft();

    String urelRight = ungroundedRelation.getRight();
    String grelRight = groundedRelation.getRight();

    List<?> key;
    Double value;

    // Graph has question and entity edge feature.
    if (gGraph.isQuestionNode(node1) || gGraph.isQuestionNode(node2)
        || gGraph.isCountNode(node1) || gGraph.isCountNode(node2)) {
      LexicalItem otherNode =
          gGraph.isQuestionNode(node1) || gGraph.isCountNode(node1) ? node2
              : node1;
      if (otherNode.isEntity()) {
        HashSet<LexicalItem> questionEdgeEntityNodes = new HashSet<>();
        HashSet<LexicalItem> questionOrCountNodes = new HashSet<>();
        questionOrCountNodes.addAll(gGraph.getQuestionNode());
        questionOrCountNodes.addAll(gGraph.getCountNode());
        for (LexicalItem qNode : questionOrCountNodes) {
          TreeSet<Edge<LexicalItem>> questionEdges = gGraph.getEdges(qNode);
          if (questionEdges != null)
            questionEdges.forEach(x -> {
              if (x.getRight().isEntity())
                questionEdgeEntityNodes.add(x.getRight());
            });
        }

        // No other entity node should be connected to the questionNode.
        if (questionEdgeEntityNodes.size() == 0) {
          features.add(new HasQuestionEntityEdgeFeature(true));
        }
      }
    }

    if (urelPartGrelPartFlag) {
      // adding prob urel part grel part feature
      key = Lists.newArrayList(urelLeft, grelLeft);
      UrelPartGrelPartFeature urelPartGrelPartFeature =
          new UrelPartGrelPartFeature(key, 1.0);
      features.add(urelPartGrelPartFeature);
      if (!learningModel.containsFeature(urelPartGrelPartFeature)) {
        if (useLexiconWeightsRel)
          value =
              groundedLexicon.getUrelPartGrelPartProb(urelLeft, grelLeft)
                  + initialEdgeWeight;
        else
          value = initialEdgeWeight;
        learningModel.setWeightIfAbsent(urelPartGrelPartFeature, value);
      }

      key = Lists.newArrayList(urelRight, grelRight);
      urelPartGrelPartFeature = new UrelPartGrelPartFeature(key, 1.0);
      features.add(urelPartGrelPartFeature);
      if (!learningModel.containsFeature(urelPartGrelPartFeature)) {
        if (useLexiconWeightsRel)
          value =
              groundedLexicon.getUrelPartGrelPartProb(urelRight, grelRight)
                  + initialEdgeWeight;
        else
          value = initialEdgeWeight;
        learningModel.setWeightIfAbsent(urelPartGrelPartFeature, value);
      }
    }

    if (urelGrelFlag) {
      // adding prob urel grel feature
      int urelGrelFound =
          groundedLexicon.hasUrelGrel(ungroundedRelation, groundedRelation);
      if (urelGrelFound == 1) {
        key = Lists.newArrayList(ungroundedRelation, groundedRelation);
      } else if (urelGrelFound == -1) {
        key =
            Lists.newArrayList(ungroundedRelation.inverse(),
                groundedRelation.inverse());
      } else {
        // urelgrel not in lexicon
        if (ungroundedRelation.getLeft().compareTo(
            ungroundedRelation.getRight()) < 0) {
          key = Lists.newArrayList(ungroundedRelation, groundedRelation);
        } else {
          key =
              Lists.newArrayList(ungroundedRelation.inverse(),
                  groundedRelation.inverse());
        }
      }
      UrelGrelFeature urelGrelFeature = new UrelGrelFeature(key, 1.0);
      features.add(urelGrelFeature);
      if (!learningModel.containsFeature(urelGrelFeature)) {
        if (useLexiconWeightsRel)
          value =
              groundedLexicon.getUrelGrelProb(ungroundedRelation,
                  groundedRelation) + initialEdgeWeight;
        else
          value = initialEdgeWeight;
        learningModel.setWeightIfAbsent(urelGrelFeature, value);
      }
    }

    if (duplicateEdgesFlag) {
      // checking if duplicate edges are next to each other
      Set<Edge<LexicalItem>> neighboringEdges = gGraph.getEdges(node1);
      if (neighboringEdges != null) {
        for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
          Relation neighboringRelation = neighboringEdge.getRelation();
          if (neighboringRelation.equals(groundedRelation)) {
            DuplicateEdgeFeature feat = new DuplicateEdgeFeature(1.0);
            features.add(feat);
          }
        }
      }
      neighboringEdges = gGraph.getEdges(node2);
      if (neighboringEdges != null) {
        Relation inverse = groundedRelation.inverse();
        for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
          Relation neighboringRelation = neighboringEdge.getRelation();
          if (neighboringRelation.equals(inverse)) {
            DuplicateEdgeFeature feat = new DuplicateEdgeFeature(1.0);
            features.add(feat);
          }
        }
      }
    }

    if (grelGrelFlag) {
      // adding edge bigram features
      Set<Edge<LexicalItem>> neighboringEdges = gGraph.getEdges(node1);
      if (neighboringEdges != null) {
        for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
          Relation neighboringRelation = neighboringEdge.getRelation();

          // order of edges should be the same across all the
          // features
          if (neighboringRelation.hashCode() < groundedRelation.hashCode())
            key = Lists.newArrayList(neighboringRelation, groundedRelation);
          else
            key = Lists.newArrayList(groundedRelation, neighboringRelation);

          GrelGrelFeature bigram = new GrelGrelFeature(key, 1.0);
          features.add(bigram);
          if (!learningModel.containsFeature(bigram)) {
            // value =
            // groundedLexicon.getGrelGrelUpperBoundProb(neighboringRelation,
            // groundedRelation);
            learningModel.setWeightIfAbsent(bigram, initialWordWeight);
          }
        }
      }
      neighboringEdges = gGraph.getEdges(node2);
      if (neighboringEdges != null) {
        Relation inverse = groundedRelation.inverse();
        for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
          Relation neighboringRelation = neighboringEdge.getRelation();

          if (neighboringRelation.hashCode() < inverse.hashCode())
            key = Lists.newArrayList(neighboringRelation, inverse);
          else
            key = Lists.newArrayList(inverse, neighboringRelation);

          GrelGrelFeature bigram = new GrelGrelFeature(key, 1.0);
          features.add(bigram);
          if (!learningModel.containsFeature(bigram)) {
            // value =
            // groundedLexicon.getGrelGrelUpperBoundProb(neighboringRelation,
            // inverse);
            learningModel.setWeightIfAbsent(bigram, initialWordWeight);
          }
        }
      }
    }

    if (gtypeGrelPartFlag) {
      // adding nodeType, edge features
      Set<Type<LexicalItem>> nodeTypes = gGraph.getTypes(node1);
      if (nodeTypes != null) {
        for (Type<LexicalItem> nodeType : nodeTypes) {
          String entityType = nodeType.getEntityType().getType();
          String grelPart = groundedRelation.getLeft();
          key = Lists.newArrayList(entityType, grelPart);
          GtypeGrelPartFeature gtypeGrelPartFeature =
              new GtypeGrelPartFeature(key, 1.0);
          features.add(gtypeGrelPartFeature);
          /*-if (!learningModel.containsFeature(gtypeGrelFeature)) {
            value = groundedLexicon.getGtypeGrelUpperBoundProb(entityType.getType(), groundedRelation);
            learningModel.setWeightIfAbsent(gtypeGrelFeature, value);
          }*/
        }
      }

      nodeTypes = gGraph.getTypes(node2);
      if (nodeTypes != null) {
        Relation inverse = groundedRelation.inverse();
        for (Type<LexicalItem> nodeType : nodeTypes) {
          String entityType = nodeType.getEntityType().getType();
          String grelPart = inverse.getLeft();
          key = Lists.newArrayList(entityType, grelPart);
          GtypeGrelPartFeature gtypeGrelFeature =
              new GtypeGrelPartFeature(key, 1.0);
          features.add(gtypeGrelFeature);
          /*-if (!learningModel.containsFeature(gtypeGrelFeature)) {
            value = groundedLexicon.getGtypeGrelUpperBoundProb(entityType.getType(), inverse);
            learningModel.setWeightIfAbsent(gtypeGrelFeature, value);
          }*/
        }
      }
    }

    if (argGrelPartFlag) {
      // adding argument word, grel feature
      Set<Type<LexicalItem>> nodeTypes = uGraph.getTypes(node1);
      if (nodeTypes != null) {
        for (Type<LexicalItem> nodeType : nodeTypes) {
          LexicalItem modifierNode = nodeType.getModifierNode();
          String modifierWord = modifierNode.getLemma();
          key = Lists.newArrayList(modifierWord, grelLeft);
          ArgGrelPartFeature argGrelPartFeature =
              new ArgGrelPartFeature(key, 1.0);
          features.add(argGrelPartFeature);
          learningModel
              .setWeightIfAbsent(argGrelPartFeature, initialWordWeight);
        }
      }

      nodeTypes = uGraph.getTypes(node2);
      if (nodeTypes != null) {
        for (Type<LexicalItem> nodeType : nodeTypes) {
          LexicalItem modifierNode = nodeType.getModifierNode();
          String modifierWord = modifierNode.getLemma();
          key = Lists.newArrayList(modifierWord, grelRight);
          ArgGrelPartFeature argGrelPartFeature =
              new ArgGrelPartFeature(key, 1.0);
          features.add(argGrelPartFeature);
          learningModel
              .setWeightIfAbsent(argGrelPartFeature, initialWordWeight);
        }
      }
    }

    if (argGrelFlag) {
      // adding argument word, grel feature
      Set<Type<LexicalItem>> nodeTypes = uGraph.getTypes(node1);
      if (nodeTypes != null) {
        for (Type<LexicalItem> nodeType : nodeTypes) {
          LexicalItem modifierNode = nodeType.getModifierNode();
          String modifierWord = modifierNode.getLemma();
          key = Lists.newArrayList(modifierWord, grelLeft, grelRight);
          ArgGrelFeature argGrelFeature = new ArgGrelFeature(key, 1.0);
          features.add(argGrelFeature);
          learningModel.setWeightIfAbsent(argGrelFeature, initialWordWeight);
        }
      }

      nodeTypes = uGraph.getTypes(node2);
      if (nodeTypes != null) {
        for (Type<LexicalItem> nodeType : nodeTypes) {
          LexicalItem modifierNode = nodeType.getModifierNode();
          String modifierWord = modifierNode.getLemma();
          key = Lists.newArrayList(modifierWord, grelRight, grelLeft);
          ArgGrelFeature argGrelFeature = new ArgGrelFeature(key, 1.0);
          features.add(argGrelFeature);
          learningModel.setWeightIfAbsent(argGrelFeature, initialWordWeight);
        }
      }
    }

    if (ngramGrelPartFlag) {
      for (int n = 1; n <= ngramLength; n++) {
        for (String biGram : getNgrams(uGraph.getActualNodes(), n)) {
          key = Lists.newArrayList(biGram, grelLeft);
          NgramGrelFeature nGramGrelFeature = new NgramGrelFeature(key, 1.0);
          features.add(nGramGrelFeature);

          key = Lists.newArrayList(biGram, grelRight);
          nGramGrelFeature = new NgramGrelFeature(key, 1.0);
          features.add(nGramGrelFeature);

          if (node1.getWordPosition() <= node2.getWordPosition()) {
            key = Lists.newArrayList(biGram, grelLeft, grelRight);
          } else {
            key = Lists.newArrayList(biGram, grelRight, grelLeft);
          }
          nGramGrelFeature = new NgramGrelFeature(key, 1.0);
          features.add(nGramGrelFeature);
        }
      }
    }

    if (wordGrelPartFlag && !mediator.isEntity()) {
      String mediatorWord = mediator.getLemma();
      key = Lists.newArrayList(mediatorWord, grelLeft);
      WordGrelPartFeature wordGrelPartFeature =
          new WordGrelPartFeature(key, 1.0);
      features.add(wordGrelPartFeature);

      key = Lists.newArrayList(mediatorWord, grelRight);
      wordGrelPartFeature = new WordGrelPartFeature(key, 1.0);
      features.add(wordGrelPartFeature);
      learningModel.setWeightIfAbsent(wordGrelPartFeature, initialWordWeight);
    }

    if (wordGrelFlag && !mediator.isEntity()) {
      String mediatorWord = mediator.getLemma();
      if (node1.getWordPosition() <= node2.getWordPosition()) {
        key = Lists.newArrayList(mediatorWord, grelLeft, grelRight);
      } else {
        key = Lists.newArrayList(mediatorWord, grelRight, grelLeft);
      }
      WordGrelFeature wordGrelFeature = new WordGrelFeature(key, 1.0);
      features.add(wordGrelFeature);
      learningModel.setWeightIfAbsent(wordGrelFeature, initialWordWeight);
    }

    if (eventTypeGrelPartFlag) {
      String mediatorWord = mediator.getLemma();
      Set<Type<LexicalItem>> mediatorTypes = uGraph.getTypes(mediator);
      if (mediatorTypes != null) {
        // birth place, place of birth (from madeup type
        // modifiers in function getUngroundedGraph).
        for (Type<LexicalItem> type : mediatorTypes) {
          LexicalItem modifierNode = type.getModifierNode();
          String modifierNodeString = modifierNode.getLemma();
          if (modifierNodeString.equals(mediatorWord))
            // (place, place , grelLeft) feature is useless
            // since (place, grelLeft) is already present
            continue;
          key = Lists.newArrayList(modifierNodeString, grelLeft);
          EventTypeGrelPartFeature eventTypeGrelPartFeature =
              new EventTypeGrelPartFeature(key, 1.0);
          features.add(eventTypeGrelPartFeature);
          learningModel.setWeightIfAbsent(eventTypeGrelPartFeature,
              initialWordWeight);

          key = Lists.newArrayList(modifierNodeString, grelRight);
          eventTypeGrelPartFeature = new EventTypeGrelPartFeature(key, 1.0);
          features.add(eventTypeGrelPartFeature);
          learningModel.setWeightIfAbsent(eventTypeGrelPartFeature,
              initialWordWeight);
        }
      }

      mediatorTypes = uGraph.getEventTypes(mediator);
      if (mediatorTypes != null) {
        // birth place, place of birth (from madeup type
        // modifiers
        // in function getUngroundedGraph)
        for (Type<LexicalItem> type : mediatorTypes) {
          LexicalItem modifierNode = type.getModifierNode();
          String modifierNodeString = modifierNode.getLemma();
          if (modifierNodeString.equals(mediatorWord))
            // (place, place , grelLeft) feature is useless
            // since (place, grelLeft) is already present
            continue;
          key = Lists.newArrayList(modifierNodeString, grelLeft);
          EventTypeGrelPartFeature eventTypeGrelPartFeature =
              new EventTypeGrelPartFeature(key, 1.0);
          features.add(eventTypeGrelPartFeature);
          learningModel.setWeightIfAbsent(eventTypeGrelPartFeature,
              initialWordWeight);

          key = Lists.newArrayList(modifierNodeString, grelRight);
          eventTypeGrelPartFeature = new EventTypeGrelPartFeature(key, 1.0);
          features.add(eventTypeGrelPartFeature);
          learningModel.setWeightIfAbsent(eventTypeGrelPartFeature,
              initialWordWeight);
        }
      }

      // agreed to direct
      Set<Type<LexicalItem>> eventModifierNodes =
          uGraph.getEventEventModifiers(mediator);
      if (eventModifierNodes != null) {
        for (Type<LexicalItem> type : eventModifierNodes) {
          LexicalItem modifierNode = type.getModifierNode();
          String modifierNodeString = modifierNode.getLemma();
          key = Lists.newArrayList(modifierNodeString, grelLeft);
          EventTypeGrelPartFeature eventTypeGrelPartFeature =
              new EventTypeGrelPartFeature(key, 1.0);
          features.add(eventTypeGrelPartFeature);
          learningModel.setWeightIfAbsent(eventTypeGrelPartFeature,
              initialWordWeight);

          key = Lists.newArrayList(modifierNodeString, grelRight);
          eventTypeGrelPartFeature = new EventTypeGrelPartFeature(key, 1.0);
          features.add(eventTypeGrelPartFeature);
          learningModel.setWeightIfAbsent(eventTypeGrelPartFeature,
              initialWordWeight);
        }
      }
    }

    if (stemMatchingFlag) {
      String grelLeftStripped = grelLeft;
      String grelRightStripped = grelRight;
      if (grelLeft.length() == grelRight.length()) {
        grelLeftStripped = grelLeftStripped.replaceAll("\\.[12]$", "");
        grelRightStripped = grelRightStripped.replaceAll("\\.[12]$", "");
      }

      String grelLeftInverse =
          schema.getRelation2Inverse(grelLeftStripped) != null ? schema
              .getRelation2Inverse(grelLeftStripped) : grelLeftStripped;
      String grelRightInverse =
          schema.getRelation2Inverse(grelRightStripped) != null ? schema
              .getRelation2Inverse(grelRightStripped) : grelRightStripped;

      List<String> parts =
          Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
      int toIndex = parts.size();
      int fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      grelLeftStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

      parts = Lists.newArrayList(Splitter.on(".").split(grelLeftInverse));
      toIndex = parts.size();
      fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      grelLeftInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

      parts = Lists.newArrayList(Splitter.on(".").split(grelRightStripped));
      toIndex = parts.size();
      fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      grelRightStripped =
          Joiner.on(".").join(parts.subList(fromIndex, toIndex));

      parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
      toIndex = parts.size();
      fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      grelRightInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

      String mediatorWord = mediator.getLemma();
      if (!mediator.getPos().equals("IN")
          && !CcgAutoLexicon.closedVerbs.contains(mediator.getLemma())) {
        /*-if (!stems.containsKey(mediatorWord))
            stems.put(mediatorWord, PorterStemmer.getStem(mediatorWord));
        String mediatorStem = stems.get(mediatorWord);*/
        String mediatorStem = mediatorWord;

        // boolean isMediatorRel =
        // schema.hasMediatorArgument(grelLeftStripped);
        if ((stringContainsWord(grelLeftStripped, mediatorStem) || stringContainsWord(
            grelLeftInverse, mediatorStem))
            && (stringContainsWord(grelRightStripped, mediatorStem) || stringContainsWord(
                grelRightInverse, mediatorStem))) {
          StemMatchingFeature s =
              new StemMatchingFeature(2.0 / (countMediator(mediator,
                  uGraph.getEdges()) + 1.0));
          features.add(s);
        }
      }

      Set<Type<LexicalItem>> mediatorTypes = uGraph.getTypes(mediator);
      if (mediatorTypes != null) {
        // birth place, place of birth (from madeup type
        // modifiers in function getUngroundedGraph)
        for (Type<LexicalItem> type : mediatorTypes) {
          LexicalItem modifierNode = type.getModifierNode();
          if (modifierNode.getPos().equals("IN")
              || CcgAutoLexicon.closedVerbs.contains(modifierNode.getLemma()))
            continue;
          String modifierNodeString = modifierNode.getLemma();
          if (modifierNodeString.equals(mediatorWord))
            // (place, place , grelLeft) feature is useless
            // since (place, grelLeft) is already present
            continue;
          /*-if (!stems.containsKey(modifierNodeString))
            stems.put(modifierNodeString, PorterStemmer.getStem(modifierNodeString));
          String modifierStem = stems.get(modifierNodeString);*/
          String modifierStem = modifierNodeString;

          if ((stringContainsWord(grelLeftStripped, modifierStem) || stringContainsWord(
              grelLeftInverse, modifierStem))
              && (stringContainsWord(grelRightStripped, modifierStem) || stringContainsWord(
                  grelRightInverse, modifierStem))) {
            StemMatchingFeature s =
                new StemMatchingFeature(2.0 / (countMediator(mediator,
                    uGraph.getEdges()) + 1.0));
            features.add(s);
          }
        }
      }
    }

    if (mediatorStemGrelPartMatchingFlag) {
      String grelLeftStripped = grelLeft;
      String grelRightStripped = grelRight;
      if (grelLeft.length() == grelRight.length()) {
        grelLeftStripped = grelLeftStripped.replaceAll("\\.[12]$", "");
        grelRightStripped = grelRightStripped.replaceAll("\\.[12]$", "");
      }

      String grelLeftInverse =
          schema.getRelation2Inverse(grelLeftStripped) != null ? schema
              .getRelation2Inverse(grelLeftStripped) : grelLeftStripped;
      // if (!schema.hasMediatorArgument(grelLeftInverse))
      // grelLeftInverse += ".0";

      String grelRightInverse =
          schema.getRelation2Inverse(grelRightStripped) != null ? schema
              .getRelation2Inverse(grelRightStripped) : grelRightStripped;
      // if (!schema.hasMediatorArgument(grelRightInverse))
      // grelRightInverse += ".0";

      List<String> parts =
          Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
      int toIndex = parts.size();
      int fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      if (!grelLeftStripped.endsWith("inverse"))
        grelLeftStripped =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));
      else
        grelLeftStripped =
            Joiner.on(".").join(
                parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));;

      parts = Lists.newArrayList(Splitter.on(".").split(grelLeftInverse));
      toIndex = parts.size();
      fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      if (!grelLeftInverse.endsWith("inverse"))
        grelLeftInverse =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));
      else
        grelLeftInverse =
            Joiner.on(".").join(
                parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

      parts = Lists.newArrayList(Splitter.on(".").split(grelRightStripped));
      toIndex = parts.size();
      fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      if (!grelRightStripped.endsWith("inverse"))
        grelRightStripped =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));
      else
        grelRightStripped =
            Joiner.on(".").join(
                parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

      parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
      toIndex = parts.size();
      fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
      if (!grelRightInverse.contains("inverse"))
        grelRightInverse =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));
      else
        grelRightInverse =
            Joiner.on(".").join(
                parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

      String mediatorWord = mediator.getLemma();
      if (!mediator.getPos().equals("IN")
          && !CcgAutoLexicon.closedVerbs.contains(mediator.getLemma())) {
        /*-if (!stems.containsKey(mediatorWord))
            stems.put(mediatorWord, PorterStemmer.getStem(mediatorWord));
        String mediatorStem = stems.get(mediatorWord);*/
        String mediatorStem = mediatorWord;

        // boolean isMediatorRel =
        // schema.hasMediatorArgument(grelLeftStripped);
        if ((stringContainsWord(grelLeftStripped, mediatorStem) && stringContainsWord(
            grelLeftInverse, mediatorStem))
            || (stringContainsWord(grelRightStripped, mediatorStem) && stringContainsWord(
                grelRightInverse, mediatorStem))) {
          MediatorStemGrelPartMatchingFeature s =
              new MediatorStemGrelPartMatchingFeature(2.0 / (countMediator(
                  mediator, uGraph.getEdges()) + 1.0));
          features.add(s);
        }
      }

      Set<Type<LexicalItem>> mediatorTypes = uGraph.getTypes(mediator);
      if (mediatorTypes != null) {
        // birth place, place of birth (from madeup type
        // modifiers in function getUngroundedGraph)
        for (Type<LexicalItem> type : mediatorTypes) {
          LexicalItem modifierNode = type.getModifierNode();
          if (modifierNode.getPos().equals("IN")
              || CcgAutoLexicon.closedVerbs.contains(modifierNode.getLemma()))
            continue;
          String modifierNodeString = modifierNode.getLemma();
          if (modifierNodeString.equals(mediatorWord))
            // (place, place , grelLeft) feature is useless
            // since (place, grelLeft) is already present
            continue;
          /*-if (!stems.containsKey(modifierNodeString))
            stems.put(modifierNodeString, PorterStemmer.getStem(modifierNodeString));
          String modifierStem = stems.get(modifierNodeString);*/
          String modifierStem = modifierNodeString;

          if ((stringContainsWord(grelLeftStripped, modifierStem) && stringContainsWord(
              grelLeftInverse, modifierStem))
              || (stringContainsWord(grelRightStripped, modifierStem) && stringContainsWord(
                  grelRightInverse, modifierStem))) {
            MediatorStemGrelPartMatchingFeature s =
                new MediatorStemGrelPartMatchingFeature(2.0 / (countMediator(
                    mediator, uGraph.getEdges()) + 1.0));
            features.add(s);
          }
        }
      }
    }

    if (argumentStemMatchingFlag) {
      Set<Type<LexicalItem>> nodeTypes1 = uGraph.getTypes(node1);
      Set<Type<LexicalItem>> nodeTypes2 = uGraph.getTypes(node2);

      Set<Type<LexicalItem>> nodeTypes = Sets.newTreeSet();
      if (nodeTypes1 != null)
        nodeTypes.addAll(nodeTypes1);
      if (nodeTypes2 != null)
        nodeTypes.addAll(nodeTypes2);

      if (nodeTypes != null && nodeTypes.size() > 0) {
        // adding nodeType, edge features
        String grelLeftStripped = grelLeft;
        String grelRightStripped = grelRight;
        if (grelLeft.length() == grelRight.length()) {
          grelLeftStripped = grelLeftStripped.replaceAll("\\.[12]$", "");
          grelRightStripped = grelRightStripped.replaceAll("\\.[12]$", "");
        }

        String grelLeftInverse =
            schema.getRelation2Inverse(grelLeftStripped) != null ? schema
                .getRelation2Inverse(grelLeftStripped) : grelLeftStripped;
        String grelRightInverse =
            schema.getRelation2Inverse(grelRightStripped) != null ? schema
                .getRelation2Inverse(grelRightStripped) : grelRightStripped;

        List<String> parts =
            Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
        int toIndex = parts.size();
        int fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        grelLeftStripped =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));

        parts = Lists.newArrayList(Splitter.on(".").split(grelLeftInverse));
        toIndex = parts.size();
        fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        grelLeftInverse =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));

        parts = Lists.newArrayList(Splitter.on(".").split(grelRightStripped));
        toIndex = parts.size();
        fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        grelRightStripped =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));

        parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
        toIndex = parts.size();
        fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        grelRightInverse =
            Joiner.on(".").join(parts.subList(fromIndex, toIndex));

        for (Type<LexicalItem> nodeType : nodeTypes) {
          LexicalItem modifierNode = nodeType.getModifierNode();

          if (modifierNode.getPos().equals("IN")
              || CcgAutoLexicon.closedVerbs.contains(modifierNode.getLemma())) {
            continue;
          }

          String modifierWord = modifierNode.getLemma();
          /*-if (!stems.containsKey(modifierWord)) {
            stems.put(modifierWord, PorterStemmer.getStem(modifierWord));
          }
          String modifierStem = stems.get(modifierWord);*/
          String modifierStem = modifierWord;

          if ((stringContainsWord(grelLeftStripped, modifierStem) || stringContainsWord(
              grelLeftInverse, modifierStem))
              && (stringContainsWord(grelRightStripped, modifierStem) || stringContainsWord(
                  grelRightInverse, modifierStem))) {
            ArgStemMatchingFeature s =
                new ArgStemMatchingFeature(2.0 / (uGraph.getEdges(
                    nodeType.getParentNode()).size() + 1.0));
            features.add(s);
          }
        }
      }
    }

    if (argumentStemGrelPartMatchingFlag) {
      Set<Type<LexicalItem>> nodeTypes1 = uGraph.getTypes(node1);
      Set<Type<LexicalItem>> nodeTypes2 = uGraph.getTypes(node2);

      Set<Type<LexicalItem>> nodeTypes = Sets.newTreeSet();
      if (nodeTypes1 != null)
        nodeTypes.addAll(nodeTypes1);
      if (nodeTypes2 != null)
        nodeTypes.addAll(nodeTypes2);

      if (nodeTypes != null && nodeTypes.size() > 0) {
        // adding nodeType, edge features
        String grelLeftStripped = grelLeft;
        String grelRightStripped = grelRight;
        if (grelLeft.length() == grelRight.length()) {
          grelLeftStripped = grelLeftStripped.replaceAll("\\.[12]$", "");
          grelRightStripped = grelRightStripped.replaceAll("\\.[12]$", "");
        }

        String grelLeftInverse =
            schema.getRelation2Inverse(grelLeftStripped) != null ? schema
                .getRelation2Inverse(grelLeftStripped) : grelLeftStripped;
        // if (!schema.hasMediatorArgument(grelLeftInverse))
        // grelLeftInverse += ".0";

        String grelRightInverse =
            schema.getRelation2Inverse(grelRightStripped) != null ? schema
                .getRelation2Inverse(grelRightStripped) : grelRightStripped;
        // if (!schema.hasMediatorArgument(grelRightInverse))
        // grelRightInverse += ".0";

        List<String> parts =
            Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
        int toIndex = parts.size();
        int fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        if (!grelLeftStripped.endsWith("inverse"))
          grelLeftStripped =
              Joiner.on(".").join(parts.subList(fromIndex, toIndex));
        else
          grelLeftStripped =
              Joiner.on(".")
                  .join(
                      parts.subList(fromIndex > 0 ? fromIndex - 1 : 0,
                          toIndex - 1));

        parts = Lists.newArrayList(Splitter.on(".").split(grelLeftInverse));
        toIndex = parts.size();
        fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        if (!grelLeftInverse.endsWith("inverse"))
          grelLeftInverse =
              Joiner.on(".").join(parts.subList(fromIndex, toIndex));
        else
          grelLeftInverse =
              Joiner.on(".")
                  .join(
                      parts.subList(fromIndex > 0 ? fromIndex - 1 : 0,
                          toIndex - 1));

        parts = Lists.newArrayList(Splitter.on(".").split(grelRightStripped));
        toIndex = parts.size();
        fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        if (!grelRightStripped.endsWith("inverse"))
          grelRightStripped =
              Joiner.on(".").join(parts.subList(fromIndex, toIndex));
        else
          grelRightStripped =
              Joiner.on(".")
                  .join(
                      parts.subList(fromIndex > 0 ? fromIndex - 1 : 0,
                          toIndex - 1));

        parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
        toIndex = parts.size();
        fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
        if (!grelRightInverse.contains("inverse"))
          grelRightInverse =
              Joiner.on(".").join(parts.subList(fromIndex, toIndex));
        else
          grelRightInverse =
              Joiner.on(".")
                  .join(
                      parts.subList(fromIndex > 0 ? fromIndex - 1 : 0,
                          toIndex - 1));

        for (Type<LexicalItem> nodeType : nodeTypes) {
          LexicalItem modifierNode = nodeType.getModifierNode();

          if (modifierNode.getPos().equals("IN")
              || CcgAutoLexicon.closedVerbs.contains(modifierNode.getLemma())) {
            continue;
          }

          String modifierWord = modifierNode.getLemma();
          /*-if (!stems.containsKey(modifierWord)) {
            stems.put(modifierWord, PorterStemmer.getStem(modifierWord));
          }
          String modifierStem = stems.get(modifierWord);*/
          String modifierStem = modifierWord;

          if ((stringContainsWord(grelLeftStripped, modifierStem) && stringContainsWord(
              grelLeftInverse, modifierStem))
              || (stringContainsWord(grelRightStripped, modifierStem) && stringContainsWord(
                  grelRightInverse, modifierStem))) {
            ArgStemGrelPartMatchingFeature s =
                new ArgStemGrelPartMatchingFeature(2.0 / (uGraph.getEdges(
                    nodeType.getParentNode()).size() + 1.0));
            features.add(s);
          }
        }
      }
    }

    return features;
  }

  private double countMediator(LexicalItem mediator,
      TreeSet<Edge<LexicalItem>> edges) {
    double count = 0.0;
    for (Edge<LexicalItem> edge : edges) {
      if (edge.getMediator().equals(mediator))
        count += 1;
    }
    return count;
  }

  /**
   * 
   * If any of the entities is of standard type, remove relations that do not
   * contain standard types as arguments.
   * 
   * @param groundedRelation
   * @param node1
   * @param node2
   * @return
   */
  private boolean checkValidRelation(Relation groundedRelation,
      LexicalItem node1, LexicalItem node2) {
    String mid1 = node1.getMid();
    String mid2 = node2.getMid();

    if (standardTypes.contains(mid1) || standardTypes.contains(mid2)) {
      Set<String> entity1Types =
          standardTypes.contains(mid1) ? Sets.newHashSet(mid1) : null;
      Set<String> entity2Types =
          standardTypes.contains(mid2) ? Sets.newHashSet(mid2) : null;
      return checkIsValidRelation(groundedRelation, entity1Types, entity2Types);
    }
    return true;
  }

  private boolean checkIsValidRelation(Relation groundedRelation,
      Set<String> entity1Types, Set<String> entity2Types) {
    boolean leftTypeIsValid = false;
    boolean rightTypeIsValid = false;

    if (entity1Types == null)
      leftTypeIsValid = true;

    if (entity2Types == null)
      rightTypeIsValid = true;

    // cannot decide
    if (leftTypeIsValid && rightTypeIsValid)
      return true;

    String leftEdge = groundedRelation.getLeft();
    String rightEdge = groundedRelation.getRight();

    String arg1Type;
    String arg2Type;

    if (leftEdge.substring(0, leftEdge.length() - 2).equals(
        rightEdge.substring(0, rightEdge.length() - 2))) {
      // relation is not of type mediator in the database.
      int length = leftEdge.length();
      // System.out.println(relationName);

      if (leftEdge.charAt(length - 1) == '1'
          && rightEdge.charAt(length - 1) == '2') {
        String relationName = leftEdge.substring(0, length - 2);
        List<String> args = schema.getRelationArguments(relationName);
        arg1Type = args.get(0);
        arg2Type = args.get(1);
      } else if (leftEdge.charAt(length - 1) == '2'
          && rightEdge.charAt(length - 1) == '1') {
        String relationName = leftEdge.substring(0, length - 2);
        List<String> args = schema.getRelationArguments(relationName);
        arg1Type = args.get(1);
        arg2Type = args.get(0);
      } else if (leftEdge.equals(rightEdge)) {
        // symmetric relation
        String relationName = leftEdge;
        List<String> args = schema.getRelationArguments(relationName);
        arg1Type = args.get(1);
        arg2Type = args.get(0);
      } else {
        // relation is of mediator type
        List<String> args1 = schema.getRelationArguments(leftEdge);
        List<String> args2 = schema.getRelationArguments(rightEdge);

        arg1Type = args1.get(1);
        arg2Type = args2.get(1);
      }
    } else {
      // relation is of mediator type
      List<String> args1 = schema.getRelationArguments(leftEdge);
      List<String> args2 = schema.getRelationArguments(rightEdge);

      arg1Type = args1.get(1);
      arg2Type = args2.get(1);
    }

    if (entity1Types != null && entity1Types.contains(arg1Type)) {
      leftTypeIsValid = true;
    }

    if (!leftTypeIsValid)
      return false;

    if (entity2Types != null && entity2Types.contains(arg2Type)) {
      rightTypeIsValid = true;
    }

    return rightTypeIsValid;
  }

  public void setLearningModel(StructuredPercepton learningModel) {
    this.learningModel = learningModel;
  }
}
