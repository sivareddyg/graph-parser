package in.sivareddy.graphparser.parsing;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.CcgParseTree;
import in.sivareddy.graphparser.ccg.CcgParseTree.CcgParser;
import in.sivareddy.graphparser.ccg.CcgParseTree.FunnyCombinatorException;
import in.sivareddy.graphparser.ccg.CcgParseTree.LexicalItem;
import in.sivareddy.graphparser.ccg.SemanticCategory.SemanticCategoryType;
import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.ArgGrelFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.ArgGrelPartFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.ArgStemGrelPartMatchingFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.ArgStemMatchingFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.DuplicateEdgeFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.EdgeNodeCountFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.GraphHasEdgeFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.GraphIsConnectedFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.GraphNodeCountFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.GrelGrelFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.GtypeGrelPartFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.MediatorStemGrelPartMatchingFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.StemMatchingFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.UrelGrelFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.UrelPartGrelPartFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.UtypeGtypeFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.WordBigramGrelPartFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.WordGrelFeature;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph.WordGrelPartFeature;
import in.sivareddy.graphparser.util.Graph;
import in.sivareddy.graphparser.util.Graph.Edge;
import in.sivareddy.graphparser.util.Graph.Type;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase;
import in.sivareddy.graphparser.util.KnowledgeBase.EntityType;
import in.sivareddy.graphparser.util.KnowledgeBase.Property;
import in.sivareddy.graphparser.util.KnowledgeBase.Relation;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.ml.basic.AbstractFeature;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.PorterStemmer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroundedGraphs {
  private Schema schema;
  private GroundedLexicon groundedLexicon;
  private KnowledgeBase kb;
  private CcgParser normalCcgParser;
  private CcgParser questionCcgParser;

  private static Set<String> lexicalPosTags = Sets.newHashSet("NNP", "NNPS");

  private boolean urelGrelFlag = true;
  private boolean urelPartGrelPartFlag = true;
  private boolean utypeGtypeFlag = true;
  private boolean gtypeGrelPartFlag = true;
  private boolean grelGrelFlag = true;
  private boolean wordGrelPartFlag = true;
  private boolean wordGrelFlag = true;
  private boolean argGrelPartFlag = true;
  private boolean argGrelFlag = true;
  private boolean wordBigramGrelPartFlag = true;
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

  private StructuredPercepton learningModel;
  public double initialEdgeWeight;
  public double initialTypeWeight;
  public double initialWordWeight;
  public double stemFeaturesWeight;
  public Logger logger;

  private Map<String, String> stems = Maps.newConcurrentMap();

  public GroundedGraphs(Schema schema, KnowledgeBase kb, GroundedLexicon groundedLexicon,
      CcgAutoLexicon normalCcgAutoLexicon, CcgAutoLexicon questionCcgAutoLexicon,
      String[] relationLexicalIdentifiers, String[] relationTypingIdentifiers,
      StructuredPercepton learningModel, boolean urelGrelFlag, boolean urelPartGrelPartFlag,
      boolean utypeGtypeFlag, boolean gtypeGrelPartFlag, boolean grelGrelFlag,
      boolean wordGrelPartFlag, boolean wordGrelFlag, boolean argGrelPartFlag, boolean argGrelFlag,
      boolean wordBigramGrelPartFlag, boolean stemMatchingFlag,
      boolean mediatorStemGrelPartMatchingFlag, boolean argumentStemMatchingFlag,
      boolean argumentStemGrelPartMatchingFlag, boolean graphIsConnectedFlag,
      boolean graphHasEdgeFlag, boolean countNodesFlag, boolean edgeNodeCountFlag,
      boolean useLexiconWeightsRel, boolean useLexiconWeightsType, boolean duplicateEdgesFlag,
      double initialEdgeWeight, double initialTypeWeight, double initialWordWeight,
      double stemFeaturesWeight) throws IOException {

    // ccg parser initialisation
    String[] argumentLexicalIdenfiers = {"mid"};
    boolean ignorePronouns = true;
    normalCcgParser =
        new CcgParser(normalCcgAutoLexicon, relationLexicalIdentifiers, argumentLexicalIdenfiers,
            relationTypingIdentifiers, ignorePronouns);
    questionCcgParser =
        new CcgParser(questionCcgAutoLexicon, relationLexicalIdentifiers, argumentLexicalIdenfiers,
            relationTypingIdentifiers, ignorePronouns);

    this.groundedLexicon = groundedLexicon;
    this.kb = kb;
    this.schema = schema;
    this.learningModel = learningModel != null ? learningModel : new StructuredPercepton();

    this.urelGrelFlag = urelGrelFlag;
    this.urelPartGrelPartFlag = urelPartGrelPartFlag;
    this.utypeGtypeFlag = utypeGtypeFlag;
    this.gtypeGrelPartFlag = gtypeGrelPartFlag;
    this.grelGrelFlag = grelGrelFlag;
    this.wordGrelPartFlag = wordGrelPartFlag;
    this.wordGrelFlag = wordGrelFlag;
    this.argGrelPartFlag = argGrelPartFlag;
    this.argGrelFlag = argGrelFlag;
    this.wordBigramGrelPartFlag = wordBigramGrelPartFlag;
    this.stemMatchingFlag = stemMatchingFlag;
    this.mediatorStemGrelPartMatchingFlag = mediatorStemGrelPartMatchingFlag;
    this.argumentStemMatchingFlag = argumentStemMatchingFlag;
    this.argumentStemGrelPartMatchingFlag = argumentStemGrelPartMatchingFlag;

    this.graphIsConnectedFlag = graphIsConnectedFlag;
    this.graphHasEdgeFlag = graphHasEdgeFlag;
    this.countNodesFlag = countNodesFlag;
    this.edgeNodeCountFlag = edgeNodeCountFlag;
    this.duplicateEdgesFlag = duplicateEdgesFlag;

    this.useLexiconWeightsRel = useLexiconWeightsRel;
    this.useLexiconWeightsType = useLexiconWeightsType;

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
    this.learningModel.setWeightIfAbsent(mediatorStemGrelPartMatchingFeature, stemFeaturesWeight);

    ArgStemGrelPartMatchingFeature argStemGrelPartMatchingFeature =
        new ArgStemGrelPartMatchingFeature(0.0);
    this.learningModel.setWeightIfAbsent(argStemGrelPartMatchingFeature, stemFeaturesWeight);
  }

  public static class LexicalGraph extends Graph<LexicalItem> {
    private Set<Feature> features;
    private StemMatchingFeature stemMatchingFeature;
    private ArgStemMatchingFeature argStemMatchingFeature;
    private MediatorStemGrelPartMatchingFeature mediatorStemGrelPartMatchingFeature;
    private ArgStemGrelPartMatchingFeature argStemGrelPartMatchingFeature;
    private DuplicateEdgeFeature duplicateEdgeFeature;

    private Set<LexicalItem> argumentsStemsMatched = Sets.newHashSet();
    private Set<LexicalItem> mediatorsStemsMatched = Sets.newHashSet();

    private Set<LexicalItem> argumentStemGrelPartMatchedNodes = Sets.newHashSet();
    private Set<LexicalItem> mediatorStemGrelPartMatchedNodes = Sets.newHashSet();

    public LexicalGraph() {
      super();
      features = Sets.newHashSet();

      stemMatchingFeature = new StemMatchingFeature(0.0);
      argStemMatchingFeature = new ArgStemMatchingFeature(0.0);
      mediatorStemGrelPartMatchingFeature = new MediatorStemGrelPartMatchingFeature(0.0);
      argStemGrelPartMatchingFeature = new ArgStemGrelPartMatchingFeature(0.0);
      duplicateEdgeFeature = new DuplicateEdgeFeature(0.0);

      features.add(stemMatchingFeature);
      features.add(argStemMatchingFeature);
      features.add(mediatorStemGrelPartMatchingFeature);
      features.add(argStemGrelPartMatchingFeature);
      features.add(duplicateEdgeFeature);
    }

    public Set<Feature> getFeatures() {
      return features;
    }

    public static class UrelGrelFeature extends AbstractFeature {
      public UrelGrelFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class UrelPartGrelPartFeature extends AbstractFeature {
      public UrelPartGrelPartFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class UtypeGtypeFeature extends AbstractFeature {
      public UtypeGtypeFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class GrelGrelFeature extends AbstractFeature {
      public GrelGrelFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class GtypeGrelPartFeature extends AbstractFeature {
      public GtypeGrelPartFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class WordGrelPartFeature extends AbstractFeature {
      public WordGrelPartFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class WordGrelFeature extends AbstractFeature {
      public WordGrelFeature(List<?> key, Double value) {
        super(key, value);
      }
    }


    public static class ArgGrelPartFeature extends AbstractFeature {
      public ArgGrelPartFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class ArgGrelFeature extends AbstractFeature {
      public ArgGrelFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class WordBigramGrelPartFeature extends AbstractFeature {
      public WordBigramGrelPartFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class WordTypeFeature extends AbstractFeature {
      public WordTypeFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class WordBIgramTypeFeature extends AbstractFeature {
      public WordBIgramTypeFeature(List<?> key, Double value) {
        super(key, value);
      }
    }

    public static class ValidQueryFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("ValidQuery");

      public ValidQueryFeature(boolean flag) {
        super(key, flag ? 1.0 : 0.0);
      }
    }

    public static class GraphIsConnectedFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("GraphIsConnected");

      public GraphIsConnectedFeature(boolean flag) {
        super(key, flag ? 1.0 : 0.0);
      }
    }

    public static class GraphHasEdgeFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("GraphHasEdge");

      public GraphHasEdgeFeature(boolean flag) {
        super(key, flag ? 1.0 : 0.0);
      }
    }

    public static class GraphNodeCountFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("GraphNodeCount");

      public GraphNodeCountFeature(double value) {
        super(key, value);
      }
    }

    public static class EdgeNodeCountFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("EdgeNodeCount");

      public EdgeNodeCountFeature(double value) {
        super(key, value);
      }
    }

    public static class DuplicateEdgeFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("DuplicateEdgeCount");

      public DuplicateEdgeFeature(Double value) {
        super(key, value);
      }
    }

    public static class StemMatchingFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("Stem");

      public StemMatchingFeature(Double value) {
        super(key, value);
      }
    }

    public static class MediatorStemGrelPartMatchingFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("MediatorStemGrelPart");

      public MediatorStemGrelPartMatchingFeature(Double value) {
        super(key, value);
      }
    }

    public static class ArgStemMatchingFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("ArgStem");

      public ArgStemMatchingFeature(Double value) {
        super(key, value);
      }
    }

    public static class ArgStemGrelPartMatchingFeature extends AbstractFeature {
      private static List<String> key = Lists.newArrayList("ArgStemGrelPart");

      public ArgStemGrelPartMatchingFeature(Double value) {
        super(key, value);
      }
    }

    @SuppressWarnings("unchecked")
    private static Set<Class<? extends AbstractFeature>> globalFeatures = Sets.newHashSet(
        StemMatchingFeature.class, ValidQueryFeature.class, ArgStemMatchingFeature.class,
        MediatorStemGrelPartMatchingFeature.class, ArgStemGrelPartMatchingFeature.class,
        GraphIsConnectedFeature.class, GraphNodeCountFeature.class, EdgeNodeCountFeature.class,
        DuplicateEdgeFeature.class);

    public void addFeature(Feature feature) {
      if (globalFeatures.contains(feature.getClass())) {
        if (features.contains(feature))
          features.remove(feature);
      }
      features.add(feature);
    }

    public LexicalGraph copy() {
      LexicalGraph newGraph = new LexicalGraph();
      copyTo(newGraph);
      newGraph.features = Sets.newHashSet(features);

      newGraph.stemMatchingFeature.setFeatureValue(stemMatchingFeature.getFeatureValue());
      newGraph.addFeature(newGraph.stemMatchingFeature);
      newGraph.mediatorsStemsMatched = Sets.newHashSet(mediatorsStemsMatched);

      newGraph.argStemMatchingFeature.setFeatureValue(argStemMatchingFeature.getFeatureValue());
      newGraph.addFeature(newGraph.argStemMatchingFeature);
      newGraph.argumentsStemsMatched = Sets.newHashSet(argumentsStemsMatched);

      newGraph.mediatorStemGrelPartMatchingFeature
          .setFeatureValue(mediatorStemGrelPartMatchingFeature.getFeatureValue());
      newGraph.addFeature(newGraph.mediatorStemGrelPartMatchingFeature);
      newGraph.mediatorStemGrelPartMatchedNodes = Sets.newHashSet(mediatorStemGrelPartMatchedNodes);

      newGraph.argStemGrelPartMatchingFeature.setFeatureValue(argStemGrelPartMatchingFeature
          .getFeatureValue());
      newGraph.addFeature(newGraph.argStemGrelPartMatchingFeature);
      newGraph.argumentStemGrelPartMatchedNodes = Sets.newHashSet(argumentStemGrelPartMatchedNodes);

      newGraph.duplicateEdgeFeature.setFeatureValue(duplicateEdgeFeature.getFeatureValue());
      newGraph.addFeature(newGraph.duplicateEdgeFeature);

      ValidQueryFeature feat = new ValidQueryFeature(false);
      newGraph.addFeature(feat);

      newGraph.setScore(new Double(getScore()));
      return newGraph;
    }

    @Override
    public String toString() {
      StringBuilder graphString = new StringBuilder();
      graphString.append("Score: " + this.getScore() + '\n');
      graphString.append("Words: \n");
      for (LexicalItem node : super.getNodes()) {
        graphString.append(Objects.toStringHelper(node).addValue(node.getWordPosition())
            .addValue(node.getWord()).addValue(node.getLemma()).addValue(node.getPos())
            .addValue(node.getCategory()).toString()
            + "\n");
      }

      graphString.append("Edges: \n");
      for (Edge<LexicalItem> edge : super.getEdges()) {
        graphString.append("(" + edge.getMediator().getWordPosition() + ","
            + edge.getLeft().getWordPosition() + "," + edge.getRight().getWordPosition() + ")"
            + "\t" + edge.getRelation() + '\n');
      }

      graphString.append("Types: \n");
      for (Type<LexicalItem> type : super.getTypes()) {
        graphString.append("(" + type.getParentNode().getWordPosition() + ","
            + type.getModifierNode().getWordPosition() + ")" + "\t" + type.getEntityType() + '\n');
      }

      graphString.append("Properties: \n");
      Map<LexicalItem, Set<Property>> nodeProperties = super.getProperties();
      for (LexicalItem node : nodeProperties.keySet()) {
        graphString.append(node.getWordPosition() + "\t" + nodeProperties.get(node) + '\n');
      }

      graphString.append("EventTypes: \n");
      Map<LexicalItem, TreeSet<Type<LexicalItem>>> eventTypes = super.getEventTypes();
      for (LexicalItem node : eventTypes.keySet()) {
        for (Type<LexicalItem> type : eventTypes.get(node))
          graphString
              .append("(" + type.getParentNode().getWordPosition() + ","
                  + type.getModifierNode().getWordPosition() + ")" + "\t" + type.getEntityType()
                  + '\n');
      }

      graphString.append("EventEventModifiers: \n");
      Map<LexicalItem, TreeSet<Type<LexicalItem>>> eventEventModifiers =
          super.getEventEventModifiers();
      for (LexicalItem node : eventEventModifiers.keySet()) {
        for (Type<LexicalItem> type : eventEventModifiers.get(node))
          graphString
              .append("(" + type.getParentNode().getWordPosition() + ","
                  + type.getModifierNode().getWordPosition() + ")" + "\t" + type.getEntityType()
                  + '\n');
      }

      return graphString.toString();
    }
  }

  /**
   * Constructs ungrounded semantic graphs from ccg parses of a given sentence. Sentence is in json
   * format containing information about entities, ccg derivations
   * 
   * @param jsonSentence
   * @param nbest
   * @return
   */
  public List<LexicalGraph> buildUngroundedGraph(JsonObject jsonSentence, String key, int nbest) {
    return buildUngroundedGraph(jsonSentence, key, nbest, logger);
  }

  /**
   * Constructs ungrounded semantic graphs from ccg parses of a given sentence. Sentence is in json
   * format containing information about entities, ccg derivations
   * 
   * @param jsonSentence
   * @param nbest
   * @param logger
   * @return
   * @throws BadParseException
   * @throws FunnyCombinatorException
   */
  public List<LexicalGraph> buildUngroundedGraph(JsonObject jsonSentence, String key, int nbest,
      Logger logger) {
    List<LexicalGraph> graphs = Lists.newArrayList();
    List<String> words = Lists.newArrayList();
    for (JsonElement word : jsonSentence.get("words").getAsJsonArray()) {
      words.add(word.getAsJsonObject().get("word").getAsString());
    }
    logger.debug("Tokenized Sentence: " + Joiner.on(" ").join(words));

    if (key.equals("synPars")) {
      if (!jsonSentence.has("synPars"))
        return graphs;
      JsonArray synPars = jsonSentence.get("synPars").getAsJsonArray();

      int parseCount = 1;

      for (JsonElement synParseElement : synPars) {
        if (parseCount > nbest)
          break;
        parseCount += 1;

        JsonObject synParseObject = synParseElement.getAsJsonObject();
        String synParse = synParseObject.get("synPar").getAsString();
        Double score = synParseObject.get("score").getAsDouble();
        logger.debug("SynParse: " + synParse + " : " + score);
        List<CcgParseTree> ccgParses;
        try {
          if (synParse.startsWith("(<T S[dcl] ") || synParse.startsWith("(<T S[pss] ")
              || synParse.startsWith("(<T S[pt] ") || synParse.startsWith("(<T S[b] ")
              || synParse.startsWith("(<T S[ng] "))
            ccgParses = normalCcgParser.parseFromString(synParse);
          else
            ccgParses = questionCcgParser.parseFromString(synParse);
        } catch (FunnyCombinatorException | BadParseException e) {
          // bad parse
          continue;
        }

        for (CcgParseTree ccgParse : ccgParses) {
          lexicaliseArgumentsToDomainEntities(ccgParse, jsonSentence);
          Set<Set<String>> semanticParses = ccgParse.getLexicalisedSemanticPredicates();
          List<LexicalItem> leaves = ccgParse.getLeafNodes();
          for (Set<String> semanticParse : semanticParses) {
            buildUngroundeGraphFromSemanticParse(semanticParse, leaves, score, graphs);
          }
        }
      }
    } else { // the semantic parses are already given
      if (!jsonSentence.has(key))
        return graphs;
      JsonArray semPars = jsonSentence.get(key).getAsJsonArray();
      Set<String> semanticParse = new HashSet<>();
      for (JsonElement semPar : semPars) {
        JsonArray predicates = semPar.getAsJsonArray();
        for (JsonElement predicate : predicates) {
          semanticParse.add(predicate.getAsString());
        }
      }
      List<LexicalItem> leaves = BuildLexicalItemsFromWords(jsonSentence);
      buildUngroundeGraphFromSemanticParse(semanticParse, leaves, 0.0, graphs);
    }
    return graphs;
  }

  static private List<LexicalItem> BuildLexicalItemsFromWords(JsonObject jsonSentence) {
    Preconditions.checkArgument(jsonSentence.has("words"));
    Map<Integer, String> tokenToEntity = new HashMap<>();
    for (JsonElement entityElement : jsonSentence.get("entities").getAsJsonArray()) {
      JsonObject entityObject = entityElement.getAsJsonObject();
      tokenToEntity.put(entityObject.get("index").getAsInt(), entityObject.get("entity")
          .getAsString());
    }

    List<LexicalItem> items = new ArrayList<>();
    int i = 0;
    for (JsonElement wordElement : jsonSentence.get("words").getAsJsonArray()) {
      JsonObject wordObject = wordElement.getAsJsonObject();
      String word = wordObject.get("word").getAsString();
      String lemma = wordObject.has("lemma") ? wordObject.get("lemma").getAsString() : word;
      String pos = wordObject.get("pos").getAsString();
      LexicalItem lex = new LexicalItem("", word, lemma, pos, "", null);
      lex.setWordPosition(i);
      if (tokenToEntity.containsKey(i)) {
        lex.setMid(tokenToEntity.get(i));
      }
      items.add(lex);
      ++i;
    }
    return items;
  }

  private void buildUngroundeGraphFromSemanticParse(Set<String> semanticParse,
      List<LexicalItem> leaves, double parseScore, List<LexicalGraph> graphs) {
    Pattern relationPattern = Pattern.compile("(.*)\\(([0-9]+)\\:e , ([0-9]+\\:.*)\\)");
    Pattern typePattern = Pattern.compile("(.*)\\(([0-9]+)\\:s , ([0-9]+\\:.*)\\)");

    // NEGATION(E), NEGATION(S), COUNT(X, int)
    Pattern specialPattern = Pattern.compile("(.*)\\(([0-9]+)\\:[^\\s]+( , )?([0-9]+:.*)?\\)");
    // Pattern varPattern = Pattern.compile("^[0-9]+\\:x$");
    Pattern eventPattern = Pattern.compile("^[0-9]+\\:e$");

    logger.debug("Semantic Parse:" + semanticParse);

    Map<Integer, Set<Pair<String, Integer>>> events = Maps.newHashMap();
    Map<Integer, Set<Pair<String, Integer>>> types = Maps.newHashMap();
    Map<Integer, Set<Pair<String, Integer>>> eventTypes = Maps.newHashMap();
    Map<Integer, Set<Pair<String, Integer>>> eventEventModifiers = Maps.newHashMap();

    Map<Integer, Set<Pair<String, String>>> specialTypes = Maps.newHashMap();

    // Build the semantic tree
    for (String predicate : semanticParse) {
      boolean isMatchedAlready = false;
      Matcher matcher = relationPattern.matcher(predicate);
      if (matcher.find()) {
        isMatchedAlready = true;
        String relationName = matcher.group(1);
        Preconditions.checkArgument(!SemanticCategoryType.types.contains(relationName),
            "relation pattern should not match special types");

        Integer eventIndex = Integer.valueOf(matcher.group(2));
        String argumentName = matcher.group(3);
        Integer argumentIndex = Integer.valueOf(argumentName.split(":")[0]);

        if (eventPattern.matcher(argumentName).matches()) {
          // if the event takes event as argument
          Pair<String, Integer> value = Pair.of(relationName, argumentIndex);
          if (!eventEventModifiers.containsKey(eventIndex))
            eventEventModifiers.put(eventIndex, new HashSet<Pair<String, Integer>>());
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
        Preconditions.checkArgument(!SemanticCategoryType.types.contains(typeName),
            "type pattern should not match special types");

        Integer stateIndex = Integer.valueOf(matcher.group(2));
        String argumentName = matcher.group(3);
        Integer argumentIndex = Integer.valueOf(argumentName.split(":")[0]);

        if (eventPattern.matcher(argumentName).matches()) {
          // if the state takes event as argument
          Pair<String, Integer> value = Pair.of(typeName, stateIndex);
          if (!eventTypes.containsKey(argumentIndex))
            eventTypes.put(argumentIndex, new HashSet<Pair<String, Integer>>());
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

        Preconditions.checkArgument(SemanticCategoryType.types.contains(specialTypeName),
            "Unknown special type");
        if (!specialTypes.containsKey(entityIndex))
          specialTypes.put(entityIndex, new HashSet<Pair<String, String>>());
        Pair<String, String> specialTypeProperties = Pair.of(specialTypeName, args);
        specialTypes.get(entityIndex).add(specialTypeProperties);
      }
    }

    // Build the graph
    LexicalGraph graph =
        buildUngroundedGraph(leaves, events, types, specialTypes, eventTypes, eventEventModifiers);
    graph.setActualNodes(leaves);
    graph.setScore(parseScore);
    graphs.add(graph);

  }

  private LexicalGraph buildUngroundedGraph(List<LexicalItem> leaves,
      Map<Integer, Set<Pair<String, Integer>>> events,
      Map<Integer, Set<Pair<String, Integer>>> types,
      Map<Integer, Set<Pair<String, String>>> specialTypes,
      Map<Integer, Set<Pair<String, Integer>>> eventTypes,
      Map<Integer, Set<Pair<String, Integer>>> eventEventModifiers) {

    LexicalGraph graph = new LexicalGraph();

    for (Integer event : events.keySet()) {
      List<Pair<String, Integer>> subEdges = Lists.newArrayList(events.get(event));
      for (int i = 0; i < subEdges.size(); i++) {
        for (int j = i + 1; j < subEdges.size(); j++) {
          String leftEdge = subEdges.get(i).getLeft();
          String rightEdge = subEdges.get(j).getLeft();

          if (leftEdge.equals(rightEdge))
            continue;

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
          relation.setWeight(groundedLexicon.getUngroundedRelationScore(relation));
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

        // adding additional types (madeup types) using edges of type
        // "director of photography"
        String modifierString = modifierNode.getMid();
        if (!events.containsKey(modifierIndex) || modifierString.startsWith("m.")
            || modifierString.startsWith("type."))
          continue;
        TreeSet<Edge<LexicalItem>> modifierNodeEdges = graph.getEdges(modifierNode);
        if (modifierNodeEdges == null)
          continue;
        for (Edge<LexicalItem> modifierNodeEdge : modifierNodeEdges) {
          LexicalItem newModifierNode = modifierNodeEdge.getRight();
          Pair<String, Integer> newModifierNodeRelPart =
              Pair.of(modifierNodeEdge.getRelation().getRight(), newModifierNode.getWordPosition());
          // dirty code to see if director modifies photography or
          // photography modifies director
          if (!events.get(modifierIndex).contains(newModifierNodeRelPart))
            continue;

          String newModifierNodeString = newModifierNode.getMid();
          String newModifierPos = newModifierNode.getPos();
          if (newModifierNodeString.startsWith("m.") || newModifierNodeString.startsWith("type.")
              || CcgParseTree.lexicalPosTags.contains(newModifierPos))
            continue;
          newModifierNodeString = modifierString + "." + newModifierNodeString + ".1";
          nodeType = new EntityType(newModifierNodeString);
          nodeType.setWeight(groundedLexicon.getUngroundedTypeScore(nodeType));
          graph.addType(modifierNode, newModifierNode, nodeType);
        }
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

  public static Map<String, String> cardinalTypes = ImmutableMap.<String, String>builder()
      .put("I-DAT", "type.datetime").put("DATE", "type.datetime").put("PERCENT", "type.float")
      .put("TIME", "type.datetime").put("MONEY", "type.float").put("CD.int", "type.int")
      .put("CD.float", "type.float").put("FLOAT", "type.float").put("INT", "type.int").build();
  Pattern floatPattern = Pattern.compile(".*[\\.][0-9].*");

  private void lexicaliseArgumentsToDomainEntities(CcgParseTree ccgParse, JsonObject jsonSentence) {
    JsonArray entities;
    if (jsonSentence.has("entities"))
      entities = jsonSentence.getAsJsonArray("entities");
    else
      entities = new JsonArray();

    JsonArray words = jsonSentence.getAsJsonArray("words");
    List<LexicalItem> leaves = ccgParse.getLeafNodes();

    List<JsonObject> wordObjects = Lists.newArrayList();
    for (JsonElement word : words) {
      JsonObject wordObject = word.getAsJsonObject();
      wordObjects.add(wordObject);
    }

    for (int i = 0; i < leaves.size(); i++) {
      String stanfordNer = wordObjects.get(i).get("ner").getAsString();
      LexicalItem leaf = leaves.get(i);
      String candcNer = leaf.getNeType();
      String posTag = leaf.getPos();
      if (posTag.equals("CD")) {
        String word = leaf.getWord();
        if (floatPattern.matcher(word).matches()) {
          posTag = "CD.float";
        } else {
          posTag = "CD.int";
        }
      }
      String mid =
          cardinalTypes.containsKey(candcNer) ? cardinalTypes.get(candcNer) : (cardinalTypes
              .containsKey(stanfordNer) ? cardinalTypes.get(stanfordNer) : (cardinalTypes
              .containsKey(posTag) ? cardinalTypes.get(posTag) : leaf.getMid()));
      leaf.setMid(mid);
    }

    // mids from freebase annotation or geoquery entity recognition
    for (JsonElement entityElement : entities) {
      JsonObject entityObject = entityElement.getAsJsonObject();
      int index = entityObject.get("index").getAsInt();
      String mid = entityObject.get("entity").getAsString();
      leaves.get(index).setMID(mid);
    }
  }

  /*-public List<LexicalGraph> createGroundedGraph(LexicalGraph graph, int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
  		boolean groundFreeVariables) {
  	return createGroundedGraph(graph, null, nbestEdges, nbestGraphs, useEntityTypes, useKB, groundFreeVariables, false);
  }*/

  public List<LexicalGraph> createGroundedGraph(LexicalGraph graph, int nbestEdges,
      int nbestGraphs, boolean useEntityTypes, boolean useKB, boolean groundFreeVariables,
      boolean useEmtpyTypes, boolean ignoreTypes, boolean testing) {
    return createGroundedGraph(graph, null, nbestEdges, nbestGraphs, useEntityTypes, useKB,
        groundFreeVariables, useEmtpyTypes, ignoreTypes, testing);
  }

  public List<LexicalGraph> createGroundedGraph(LexicalGraph graph,
      Set<LexicalItem> restrictedNodes, int nbestEdges, int nbestGraphs, boolean useEntityTypes,
      boolean useKB, boolean groundFreeVariables, boolean useEmtpyTypes, boolean ignoreTypes,
      boolean testing) {
    TreeSet<Edge<LexicalItem>> edges = graph.getEdges();
    List<LexicalGraph> groundedGraphs = Lists.newArrayList();
    if (edges.size() == 0)
      return groundedGraphs;

    LexicalGraph groundedGraph = new LexicalGraph();
    groundedGraph.addEventTypes(graph.getEventTypes());
    groundedGraph.addEventEventModifiers(graph.getEventEventModifiers());
    groundedGraph.addProperties(graph.getProperties());
    groundedGraph.setActualNodes(graph.getActualNodes());
    groundedGraphs.add(groundedGraph);

    Set<LexicalItem> nodesCovered = Sets.newHashSet();

    for (Edge<LexicalItem> edge : edges) {
      LexicalItem node1 = edge.getLeft();
      LexicalItem node2 = edge.getRight();

      String entity1 = node1.getMid();
      String entity2 = node2.getMid();

      // useless edge since the edge contains out of domain entity
      if ((useKB || useEntityTypes) && lexicalPosTags.contains(node1.getPos())
          && !kb.hasEntity(entity1))
        continue;

      if ((useKB || useEntityTypes) && lexicalPosTags.contains(node2.getPos())
          && !kb.hasEntity(entity2))
        continue;

      // ground the edge
      groundedGraphs =
          groundTheEdge(groundedGraphs, graph, edge, restrictedNodes, nbestEdges, nbestGraphs,
              useEntityTypes, useKB, groundFreeVariables, testing);

      // sort descending order
      Collections.sort(groundedGraphs);
      groundedGraphs =
          groundedGraphs.size() < nbestGraphs ? groundedGraphs : groundedGraphs.subList(0,
              nbestGraphs);

      if (!ignoreTypes) {
        // ground the node types
        if (!nodesCovered.contains(node1)) {
          nodesCovered.add(node1);
          if (graph.getTypes(node1) != null) {
            List<Type<LexicalItem>> nodeTypes = Lists.newArrayList(graph.getTypes(node1));
            /*-Map<Type<LexicalItem>, List<Type<LexicalItem>>> organisedNodeTypes = organiseTypes(nodeTypes);
            for (Type<LexicalItem> nodeType : organisedNodeTypes.keySet()) {
            	List<Type<LexicalItem>> additionalNodeTypes = organisedNodeTypes.get(nodeType);
            	groundedGraphs = groundTheType(groundedGraphs, nodeType, additionalNodeTypes, nbestEdges, nbestGraphs, useEntityTypes, useKB);
            }*/

            Type<LexicalItem> nodeType = nodeTypes.get(0);
            List<Type<LexicalItem>> additionalNodeTypes =
                nodeTypes.size() > 1 ? nodeTypes.subList(1, nodeTypes.size())
                    : new ArrayList<Type<LexicalItem>>();

            groundedGraphs =
                groundTheType(groundedGraphs, graph, nodeType, additionalNodeTypes,
                    restrictedNodes, nbestEdges, nbestGraphs, useEntityTypes, useKB,
                    groundFreeVariables, useEmtpyTypes, testing);
          }
        }

        // ground the node types
        if (!nodesCovered.contains(node2)) {
          nodesCovered.add(node2);

          if (graph.getTypes(node2) != null) {
            List<Type<LexicalItem>> nodeTypes = Lists.newArrayList(graph.getTypes(node2));
            /*-Map<Type<LexicalItem>, List<Type<LexicalItem>>> organisedNodeTypes = organiseTypes(nodeTypes);
            for (Type<LexicalItem> nodeType : organisedNodeTypes.keySet()) {
            	List<Type<LexicalItem>> additionalNodeTypes = organisedNodeTypes.get(nodeType);
            	groundedGraphs = groundTheType(groundedGraphs, nodeType, additionalNodeTypes, nbestEdges, nbestGraphs, useEntityTypes, useKB);
            }*/
            Type<LexicalItem> nodeType = nodeTypes.get(0);
            List<Type<LexicalItem>> additionalNodeTypes =
                nodeTypes.size() > 1 ? nodeTypes.subList(1, nodeTypes.size())
                    : new ArrayList<Type<LexicalItem>>();
            groundedGraphs =
                groundTheType(groundedGraphs, graph, nodeType, additionalNodeTypes,
                    restrictedNodes, nbestEdges, nbestGraphs, useEntityTypes, useKB,
                    groundFreeVariables, useEmtpyTypes, testing);
          }
        }

        // sort descending order
        Collections.sort(groundedGraphs);
        groundedGraphs =
            groundedGraphs.size() < nbestGraphs ? groundedGraphs : groundedGraphs.subList(0,
                nbestGraphs);
      }
    }

    // See if there are any question nodes and ignore the graphs that do not
    // have an edge from question node
    Map<LexicalItem, Set<Property>> graphProperties = graph.getProperties();
    LexicalItem questionNode = null;
    for (LexicalItem node : graphProperties.keySet()) {
      for (Property property : graphProperties.get(node)) {
        if (property.getPropertyName().equals("QUESTION")) {
          questionNode = node;
          break;
        }
      }
      if (questionNode != null)
        break;
    }

    if (questionNode != null) {
      List<LexicalGraph> validGroundedGraphs = Lists.newArrayList();
      for (LexicalGraph gGraph : groundedGraphs) {
        Set<Edge<LexicalItem>> questionNodeEdges = gGraph.getEdges(questionNode);
        if (questionNodeEdges != null && questionNodeEdges.size() > 0)
          validGroundedGraphs.add(gGraph);
      }
      groundedGraphs = validGroundedGraphs;
    }

    if (graphIsConnectedFlag) {
      for (LexicalGraph gGraph : groundedGraphs) {
        boolean connected = gGraph.isConnected();
        GraphIsConnectedFeature graphIsConnectedFeature =
            connected ? new GraphIsConnectedFeature(true) : new GraphIsConnectedFeature(false);
        gGraph.addFeature(graphIsConnectedFeature);
        if (connected) {
          if (testing) {
            gGraph.setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
          } else {
            gGraph.setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
          }
        }
      }
    }

    if (graphHasEdgeFlag) {
      for (LexicalGraph gGraph : groundedGraphs) {
        boolean graphHasEdge = gGraph.getEdges().size() > 0 ? true : false;
        GraphHasEdgeFeature graphHasEdgeFeature = new GraphHasEdgeFeature(graphHasEdge);
        gGraph.addFeature(graphHasEdgeFeature);
        if (graphHasEdge) {
          if (testing) {
            gGraph.setScore(learningModel.getScoreTesting(gGraph.getFeatures()));
          } else {
            gGraph.setScore(learningModel.getScoreTraining(gGraph.getFeatures()));
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

        EdgeNodeCountFeature edgeNodeCountFeature = new EdgeNodeCountFeature(new Double(nodeCount));
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
        groundedGraphs.size() < nbestGraphs ? groundedGraphs : groundedGraphs.subList(0,
            nbestGraphs);


    return groundedGraphs;
  }

  @SuppressWarnings("unused")
  private Map<Type<LexicalItem>, List<Type<LexicalItem>>> organiseTypes(
      List<Type<LexicalItem>> nodeTypes) {
    // returns a map containing the main type and all its subtypes
    // find nodes that are from the same parent e.g. society.1 is already
    // present in secret.society.1
    Map<Type<LexicalItem>, List<Type<LexicalItem>>> map = Maps.newHashMap();
    Set<Type<LexicalItem>> typesCovered = Sets.newHashSet();
    for (int k = 0; k < nodeTypes.size(); k++) {
      Type<LexicalItem> kthType = nodeTypes.get(k);
      EntityType kthEntityType = kthType.getEntityType();
      String kthEntityTypeString = kthEntityType.getType();
      for (int j = k + 1; j < nodeTypes.size(); j++) {
        Type<LexicalItem> jthType = nodeTypes.get(j);
        EntityType jthEntityType = jthType.getEntityType();
        String jthEntityTypeString = jthEntityType.getType();
        // types modifying the same parent
        if (jthType.getParentNode().equals(kthType.getParentNode())) {
          if (jthEntityTypeString.contains(kthEntityTypeString)) {
            // secret.society.1 containing society.1
            if (!map.containsKey(kthType))
              map.put(kthType, new ArrayList<Type<LexicalItem>>());
            if (map.containsKey(jthType))
              map.get(kthType).addAll(map.get(jthType));
            map.get(kthType).add(jthType);
            typesCovered.add(kthType);
            typesCovered.add(jthType);
          } else if (kthEntityTypeString.contains(jthEntityTypeString)) {
            if (!map.containsKey(jthType))
              map.put(jthType, new ArrayList<Type<LexicalItem>>());
            if (map.containsKey(kthType))
              map.get(jthType).addAll(map.get(kthType));
            map.get(jthType).add(kthType);
            typesCovered.add(kthType);
            typesCovered.add(jthType);
          }
        }
      }
    }

    for (Type<LexicalItem> nodeType : nodeTypes) {
      if (!typesCovered.contains(nodeType))
        map.put(nodeType, new ArrayList<Type<LexicalItem>>());
    }

    return map;
  }

  private static Set<String> standardTypes = Sets.newHashSet("type.datetime", "type.int",
      "type.float");

  public static boolean nodeIsLexicalised(LexicalItem node) {
    return (standardTypes.contains(node.getMid()) || node.getMid().startsWith("m."));
  }

  private List<LexicalGraph> groundTheType(List<LexicalGraph> groundedGraphs,
      LexicalGraph ungroundedGraph, Type<LexicalItem> nodeType,
      List<Type<LexicalItem>> additionalNodeTypes, Set<LexicalItem> restrictedNodes,
      int nbestTypes, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean useEmptyTypes, boolean testing) {
    EntityType unGroundedEntityType = nodeType.getEntityType();
    LexicalItem parentNode = nodeType.getParentNode();
    LexicalItem modifierNode = nodeType.getModifierNode();
    String entity = parentNode.getWord();
    if (restrictedNodes == null || !restrictedNodes.contains(parentNode))
      entity = parentNode.getMid();

    List<LexicalGraph> tempGraphs = Lists.newArrayList();
    List<EntityType> groundedEntityTypes = null;

    if ((useKB || useEntityTypes) && kb.hasEntity(entity)) {
      Set<String> gtypes = kb.getTypes(entity);
      if (gtypes != null) {
        groundedEntityTypes = Lists.newArrayList();
        for (String gtype : gtypes) {
          EntityType groundedEntityType = new EntityType(gtype);
          Double prob =
              groundedLexicon.getUtypeGtypeProb(unGroundedEntityType.getType(),
                  groundedEntityType.getType());
          groundedEntityType.setWeight(prob);
          groundedEntityTypes.add(groundedEntityType);
        }
        Collections.sort(groundedEntityTypes);
      }
    } else if (groundFreeVariables) {
      List<EntityType> entityTypes = groundedLexicon.getGroundedTypes(unGroundedEntityType);
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
      return groundedGraphs;

    List<?> featureKey;
    int nbestCount = 0;
    for (EntityType groundedEntityType : groundedEntityTypes) {
      nbestCount++;
      if (nbestCount > nbestTypes)
        break;

      for (LexicalGraph oldGraph : groundedGraphs) {
        // if the entity type is basic, check if the edges going out
        // satisfy that constraint
        boolean nodeTypeSatisifiesEdgeType = true;
        if (standardTypes.contains(groundedEntityType.getType())) {
          Set<Edge<LexicalItem>> parentEdges = oldGraph.getEdges(parentNode);
          if (parentEdges == null)
            parentEdges = Sets.newHashSet();
          for (Edge<LexicalItem> edge : parentEdges) {
            String edgeName =
                edge.getLeft().equals(parentNode) ? edge.getRelation().getLeft() : edge
                    .getRelation().getRight();
            ArrayList<String> edgeParts = Lists.newArrayList(Splitter.on(".").split(edgeName));
            int edgePartsLength = edgeParts.size();
            if (edgeParts.get(edgePartsLength - 1).equals("1")) {
              String edgeActualName =
                  Joiner.on(".").join(edgeParts.subList(0, edgePartsLength - 1));
              String nodeTypeFromEdge = schema.getRelationArguments(edgeActualName).get(0);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            } else if (edgeParts.get(edgePartsLength - 1).equals("2")) {
              String edgeActualName =
                  Joiner.on(".").join(edgeParts.subList(0, edgePartsLength - 1));
              String nodeTypeFromEdge = schema.getRelationArguments(edgeActualName).get(1);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            } else if (edgeParts.get(edgePartsLength - 1).equals("inverse")) {
              String edgeActualName =
                  Joiner.on(".").join(edgeParts.subList(0, edgePartsLength - 1));
              String nodeTypeFromEdge = schema.getRelationArguments(edgeActualName).get(0);
              if (!nodeTypeFromEdge.equals(groundedEntityType.getType())) {
                nodeTypeSatisifiesEdgeType = false;
                break;
              }
            } else {
              String edgeActualName = edgeName;
              String nodeTypeFromEdge = schema.getRelationArguments(edgeActualName).get(1);
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
        newGraph.addType(parentNode, modifierNode, groundedEntityType);

        // adding utype gtype features
        if (utypeGtypeFlag) {
          featureKey = Lists.newArrayList(unGroundedEntityType, groundedEntityType);
          UtypeGtypeFeature utypeGtypeFeature = new UtypeGtypeFeature(featureKey, 1.0);
          newGraph.addFeature(utypeGtypeFeature);
          if (!learningModel.containsFeature(utypeGtypeFeature)) {
            double value;
            if (useLexiconWeightsType)
              value = groundedEntityType.getWeight() * initialTypeWeight;
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
              GtypeGrelPartFeature gtypeGrelPartFeature = new GtypeGrelPartFeature(featureKey, 1.0);
              newGraph.addFeature(gtypeGrelPartFeature);
              /*-if (!learningModel.containsFeature(gtypeGrelFeature)) {
              	double value = groundedLexicon.getGtypeGrelUpperBoundProb(entityType.getType(), groundedRelation);
              	learningModel.setWeightIfAbsent(gtypeGrelFeature, value);
              }*/
            }
          }
        }

        // adding node types from additional modifiers of the same type.
        // e.g. for society additional modifiers are society.secret.1
        // This does not work for cases like director and producer. In
        // such cases, you have to ground each entity type separately.
        for (Type<LexicalItem> additionalNodeType : additionalNodeTypes) {
          EntityType unGroundedAdditionalEntityType = additionalNodeType.getEntityType();
          EntityType groundedAdditionalType = groundedEntityType.copy();
          Double prob =
              groundedLexicon.getUtypeGtypeProb(unGroundedAdditionalEntityType.getType(),
                  groundedAdditionalType.getType());
          groundedAdditionalType.setWeight(prob);

          newGraph.addType(additionalNodeType.getParentNode(),
              additionalNodeType.getModifierNode(), groundedAdditionalType);

          // adding utype gtype features
          if (utypeGtypeFlag) {
            featureKey = Lists.newArrayList(unGroundedAdditionalEntityType, groundedAdditionalType);
            UtypeGtypeFeature utypeGtypeFeature = new UtypeGtypeFeature(featureKey, 1.0);
            newGraph.addFeature(utypeGtypeFeature);
            if (!learningModel.containsFeature(utypeGtypeFeature)) {
              double value;
              if (useLexiconWeightsType)
                value = groundedAdditionalType.getWeight() * initialTypeWeight;
              else
                value = initialTypeWeight;

              learningModel.setWeightIfAbsent(utypeGtypeFeature, value);
            }
          }
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

    if (tempGraphs.size() == 0)
      return groundedGraphs;

    // If empty types is not used
    if (!useEmptyTypes) {
      // adding all the graphs without the node types
      tempGraphs.addAll(groundedGraphs);
    }
    return tempGraphs;
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

  private List<LexicalGraph> groundTheEdge(List<LexicalGraph> groundedGraphs,
      LexicalGraph ungroundedGraph, Edge<LexicalItem> edge, Set<LexicalItem> restrictedNodes,
      int nbestEdges, int nbestGraphs, boolean useEntityTypes, boolean useKB,
      boolean groundFreeVariables, boolean testing) {
    Relation ungroundedRelation = edge.getRelation();
    LexicalItem node1 = edge.getLeft();
    LexicalItem node2 = edge.getRight();
    LexicalItem mediator = edge.getMediator();

    String entity1 = node1.getWord();
    // if node is not present in restrictedNodes, we can use the entity type
    if (restrictedNodes == null || !restrictedNodes.contains(node1))
      entity1 = node1.getMid();

    String entity2 = node2.getWord();
    if (restrictedNodes == null || !restrictedNodes.contains(node2))
      entity2 = node2.getMid();

    List<Relation> groundedRelations = null;
    if (useKB && (kb.hasEntity(entity1) || kb.hasEntity(entity2))) {
      groundedRelations = Lists.newArrayList();
      Set<Relation> groundedRelationsSet = null;
      if (kb.hasEntity(entity1) && kb.hasEntity(entity2)
          && !(standardTypes.contains(entity1) && standardTypes.contains(entity2))) {
        groundedRelationsSet = kb.getRelations(entity1, entity2);
      } else if (kb.hasEntity(entity1) && !standardTypes.contains(entity1)) {
        groundedRelationsSet = kb.getRelations(entity1);
      } else if (kb.hasEntity(entity2) && !standardTypes.contains(entity2)) {
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
          Double prob = groundedLexicon.getUrelGrelProb(ungroundedRelation, groundedRelation);
          groundedRelation.setWeight(prob);
          groundedRelations.add(groundedRelation);
        }
      }

      Collections.sort(groundedRelations);
    } else if (groundFreeVariables) {
      // if both the entities are not in the database, we shall use
      // lexicon. One could also use the schema to explore all the
      // relations in the domain, but that would lead to large search
      // space.
      groundedRelations = groundedLexicon.getGroundedRelations(ungroundedRelation);
    }

    if (groundedRelations == null)
      return groundedGraphs;

    Set<Relation> groundedRelationsCopy = Sets.newHashSet(groundedRelations);
    for (Relation groundedRelation : groundedRelationsCopy) {
      boolean checkIfValid = checkValidRelation(groundedRelation, node1, node2);
      if (!checkIfValid)
        groundedRelations.remove(groundedRelation);
    }

    List<LexicalGraph> tempGraphs = Lists.newArrayList();

    // Add each new edge to each of the old graphs
    int nbestCount = 0;
    for (Relation groundedRelation : groundedRelations) {
      nbestCount++;
      if (nbestCount > nbestEdges)
        break;

      // if database is not used, then only entity type checking is used
      if (!useKB && useEntityTypes) {
        Set<String> entity1Types = kb.hasEntity(entity1) ? kb.getTypes(entity1) : null;
        Set<String> entity2Types = kb.hasEntity(entity2) ? kb.getTypes(entity2) : null;
        if (entity1Types != null || entity2Types != null) {
          boolean isValidRelation =
              checkIsValidRelation(groundedRelation, entity1Types, entity2Types);
          if (!isValidRelation)
            continue;
        }
      }

      for (LexicalGraph oldGraph : groundedGraphs) {
        LexicalGraph newGraph = oldGraph.copy();

        newGraph.addEdge(node1, node2, mediator, groundedRelation);

        String urelLeft = ungroundedRelation.getLeft();
        String grelLeft = groundedRelation.getLeft();

        String urelRight = ungroundedRelation.getRight();
        String grelRight = groundedRelation.getRight();

        List<?> key;
        Double value;

        if (urelPartGrelPartFlag) {
          // adding prob urel part grel part feature
          key = Lists.newArrayList(urelLeft, grelLeft);
          UrelPartGrelPartFeature urelPartGrelPartFeature = new UrelPartGrelPartFeature(key, 1.0);
          newGraph.addFeature(urelPartGrelPartFeature);
          if (!learningModel.containsFeature(urelPartGrelPartFeature)) {
            if (useLexiconWeightsRel)
              value =
                  groundedLexicon.getUrelPartGrelPartProb(urelLeft, grelLeft) * initialEdgeWeight;
            else
              value = initialEdgeWeight;
            learningModel.setWeightIfAbsent(urelPartGrelPartFeature, value);
          }

          key = Lists.newArrayList(urelRight, grelRight);
          urelPartGrelPartFeature = new UrelPartGrelPartFeature(key, 1.0);
          newGraph.addFeature(urelPartGrelPartFeature);
          if (!learningModel.containsFeature(urelPartGrelPartFeature)) {
            if (useLexiconWeightsRel)
              value =
                  groundedLexicon.getUrelPartGrelPartProb(urelRight, grelRight) * initialEdgeWeight;
            else
              value = initialEdgeWeight;
            learningModel.setWeightIfAbsent(urelPartGrelPartFeature, value);
          }
        }

        if (urelGrelFlag) {
          // adding prob urel grel feature
          int urelGrelFound = groundedLexicon.hasUrelGrel(ungroundedRelation, groundedRelation);
          if (urelGrelFound == 1) {
            key = Lists.newArrayList(ungroundedRelation, groundedRelation);
          } else if (urelGrelFound == -1) {
            key = Lists.newArrayList(ungroundedRelation.inverse(), groundedRelation.inverse());
          } else {
            // urelgrel not in lexicon
            if (ungroundedRelation.getLeft().compareTo(ungroundedRelation.getRight()) < 0) {
              key = Lists.newArrayList(ungroundedRelation, groundedRelation);
            } else {
              key = Lists.newArrayList(ungroundedRelation.inverse(), groundedRelation.inverse());
            }
          }
          UrelGrelFeature urelGrelFeature = new UrelGrelFeature(key, 1.0);
          newGraph.addFeature(urelGrelFeature);
          if (!learningModel.containsFeature(urelGrelFeature)) {
            if (useLexiconWeightsRel)
              value =
                  groundedLexicon.getUrelGrelProb(ungroundedRelation, groundedRelation)
                      * initialEdgeWeight;
            else
              value = initialEdgeWeight;
            learningModel.setWeightIfAbsent(urelGrelFeature, value);
          }
        }

        if (duplicateEdgesFlag) {
          // checking if duplicate edges are next to each other
          Set<Edge<LexicalItem>> neighboringEdges = oldGraph.getEdges(node1);
          if (neighboringEdges != null) {
            for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
              Relation neighboringRelation = neighboringEdge.getRelation();
              if (neighboringRelation.equals(groundedRelation)) {
                DuplicateEdgeFeature feat = newGraph.duplicateEdgeFeature;
                feat.setFeatureValue(feat.getFeatureValue() + 1.0);
              }
            }
          }
          neighboringEdges = oldGraph.getEdges(node2);
          if (neighboringEdges != null) {
            Relation inverse = groundedRelation.inverse();
            for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
              Relation neighboringRelation = neighboringEdge.getRelation();
              if (neighboringRelation.equals(inverse)) {
                DuplicateEdgeFeature feat = newGraph.duplicateEdgeFeature;
                feat.setFeatureValue(feat.getFeatureValue() + 1.0);
              }
            }
          }
        }

        if (grelGrelFlag) {
          // adding edge bigram features
          Set<Edge<LexicalItem>> neighboringEdges = oldGraph.getEdges(node1);
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
              newGraph.addFeature(bigram);
              if (!learningModel.containsFeature(bigram)) {
                // value =
                // groundedLexicon.getGrelGrelUpperBoundProb(neighboringRelation,
                // groundedRelation);
                learningModel.setWeightIfAbsent(bigram, initialWordWeight);
              }
            }
          }
          neighboringEdges = oldGraph.getEdges(node2);
          if (neighboringEdges != null) {
            Relation inverse = groundedRelation.inverse();
            for (Edge<LexicalItem> neighboringEdge : neighboringEdges) {
              Relation neighboringRelation = neighboringEdge.getRelation();

              if (neighboringRelation.hashCode() < inverse.hashCode())
                key = Lists.newArrayList(neighboringRelation, inverse);
              else
                key = Lists.newArrayList(inverse, neighboringRelation);

              GrelGrelFeature bigram = new GrelGrelFeature(key, 1.0);
              newGraph.addFeature(bigram);
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
          Set<Type<LexicalItem>> nodeTypes = newGraph.getTypes(node1);
          if (nodeTypes != null) {
            for (Type<LexicalItem> nodeType : nodeTypes) {
              String entityType = nodeType.getEntityType().getType();
              String grelPart = groundedRelation.getLeft();
              key = Lists.newArrayList(entityType, grelPart);
              GtypeGrelPartFeature gtypeGrelPartFeature = new GtypeGrelPartFeature(key, 1.0);
              newGraph.addFeature(gtypeGrelPartFeature);
              /*-if (!learningModel.containsFeature(gtypeGrelFeature)) {
              	value = groundedLexicon.getGtypeGrelUpperBoundProb(entityType.getType(), groundedRelation);
              	learningModel.setWeightIfAbsent(gtypeGrelFeature, value);
              }*/
            }
          }

          nodeTypes = newGraph.getTypes(node2);
          if (nodeTypes != null) {
            Relation inverse = groundedRelation.inverse();
            for (Type<LexicalItem> nodeType : nodeTypes) {
              String entityType = nodeType.getEntityType().getType();
              String grelPart = inverse.getLeft();
              key = Lists.newArrayList(entityType, grelPart);
              GtypeGrelPartFeature gtypeGrelFeature = new GtypeGrelPartFeature(key, 1.0);
              newGraph.addFeature(gtypeGrelFeature);
              /*-if (!learningModel.containsFeature(gtypeGrelFeature)) {
              	value = groundedLexicon.getGtypeGrelUpperBoundProb(entityType.getType(), inverse);
              	learningModel.setWeightIfAbsent(gtypeGrelFeature, value);
              }*/
            }
          }
        }

        if (argGrelPartFlag) {
          // adding argument word, grel feature
          Set<Type<LexicalItem>> nodeTypes = ungroundedGraph.getTypes(node1);
          if (nodeTypes != null) {
            for (Type<LexicalItem> nodeType : nodeTypes) {
              LexicalItem modifierNode = nodeType.getModifierNode();
              String modifierWord = modifierNode.getLemma();
              key = Lists.newArrayList(modifierWord, grelLeft);
              ArgGrelPartFeature argGrelPartFeature = new ArgGrelPartFeature(key, 1.0);
              newGraph.addFeature(argGrelPartFeature);
              learningModel.setWeightIfAbsent(argGrelPartFeature, initialWordWeight);
            }
          }

          nodeTypes = ungroundedGraph.getTypes(node2);
          if (nodeTypes != null) {
            for (Type<LexicalItem> nodeType : nodeTypes) {
              LexicalItem modifierNode = nodeType.getModifierNode();
              String modifierWord = modifierNode.getLemma();
              key = Lists.newArrayList(modifierWord, grelRight);
              ArgGrelPartFeature argGrelPartFeature = new ArgGrelPartFeature(key, 1.0);
              newGraph.addFeature(argGrelPartFeature);
              learningModel.setWeightIfAbsent(argGrelPartFeature, initialWordWeight);
            }
          }
        }

        if (argGrelFlag) {
          // adding argument word, grel feature
          Set<Type<LexicalItem>> nodeTypes = ungroundedGraph.getTypes(node1);
          if (nodeTypes != null) {
            for (Type<LexicalItem> nodeType : nodeTypes) {
              LexicalItem modifierNode = nodeType.getModifierNode();
              String modifierWord = modifierNode.getLemma();
              key = Lists.newArrayList(modifierWord, grelLeft, grelRight);
              ArgGrelFeature argGrelFeature = new ArgGrelFeature(key, 1.0);
              newGraph.addFeature(argGrelFeature);
              learningModel.setWeightIfAbsent(argGrelFeature, initialWordWeight);
            }
          }

          nodeTypes = ungroundedGraph.getTypes(node2);
          if (nodeTypes != null) {
            for (Type<LexicalItem> nodeType : nodeTypes) {
              LexicalItem modifierNode = nodeType.getModifierNode();
              String modifierWord = modifierNode.getLemma();
              key = Lists.newArrayList(modifierWord, grelRight, grelLeft);
              ArgGrelFeature argGrelFeature = new ArgGrelFeature(key, 1.0);
              newGraph.addFeature(argGrelFeature);
              learningModel.setWeightIfAbsent(argGrelFeature, initialWordWeight);
            }
          }
        }


        if (argumentStemMatchingFlag) {
          Set<Type<LexicalItem>> nodeTypes1 = ungroundedGraph.getTypes(node1);
          Set<Type<LexicalItem>> nodeTypes2 = ungroundedGraph.getTypes(node2);

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

            List<String> parts = Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
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
            grelRightStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

            parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
            toIndex = parts.size();
            fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
            grelRightInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

            for (Type<LexicalItem> nodeType : nodeTypes) {
              LexicalItem modifierNode = nodeType.getModifierNode();

              if (newGraph.argumentsStemsMatched.contains(modifierNode)
                  || modifierNode.getPos().equals("IN")
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
                ArgStemMatchingFeature s = newGraph.argStemMatchingFeature;
                s.setFeatureValue(s.getFeatureValue() + 1.0);
                newGraph.argumentsStemsMatched.add(modifierNode);
              }
            }
          }
        }

        if (argumentStemGrelPartMatchingFlag) {
          Set<Type<LexicalItem>> nodeTypes1 = ungroundedGraph.getTypes(node1);
          Set<Type<LexicalItem>> nodeTypes2 = ungroundedGraph.getTypes(node2);

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

            List<String> parts = Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
            int toIndex = parts.size();
            int fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
            if (!grelLeftStripped.endsWith("inverse"))
              grelLeftStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
            else
              grelLeftStripped =
                  Joiner.on(".")
                      .join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

            parts = Lists.newArrayList(Splitter.on(".").split(grelLeftInverse));
            toIndex = parts.size();
            fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
            if (!grelLeftInverse.endsWith("inverse"))
              grelLeftInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
            else
              grelLeftInverse =
                  Joiner.on(".")
                      .join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

            parts = Lists.newArrayList(Splitter.on(".").split(grelRightStripped));
            toIndex = parts.size();
            fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
            if (!grelRightStripped.endsWith("inverse"))
              grelRightStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
            else
              grelRightStripped =
                  Joiner.on(".")
                      .join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

            parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
            toIndex = parts.size();
            fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
            if (!grelRightInverse.contains("inverse"))
              grelRightInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
            else
              grelRightInverse =
                  Joiner.on(".")
                      .join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

            for (Type<LexicalItem> nodeType : nodeTypes) {
              LexicalItem modifierNode = nodeType.getModifierNode();

              if (newGraph.argumentStemGrelPartMatchedNodes.contains(modifierNode)
                  || modifierNode.getPos().equals("IN")
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
                ArgStemGrelPartMatchingFeature s = newGraph.argStemGrelPartMatchingFeature;
                s.setFeatureValue(s.getFeatureValue() + 1.0);
                newGraph.argumentStemGrelPartMatchedNodes.add(modifierNode);
              }
            }
          }
        }

        if (wordGrelPartFlag) {
          String mediatorWord = mediator.getLemma();
          key = Lists.newArrayList(mediatorWord, grelLeft);
          WordGrelPartFeature wordGrelPartFeature = new WordGrelPartFeature(key, 1.0);
          newGraph.addFeature(wordGrelPartFeature);

          key = Lists.newArrayList(mediatorWord, grelRight);
          wordGrelPartFeature = new WordGrelPartFeature(key, 1.0);
          newGraph.addFeature(wordGrelPartFeature);
          learningModel.setWeightIfAbsent(wordGrelPartFeature, initialWordWeight);
        }

        if (wordGrelFlag) {
          String mediatorWord = mediator.getLemma();
          key = Lists.newArrayList(mediatorWord, grelLeft, grelRight);
          WordGrelFeature wordGrelFeature = new WordGrelFeature(key, 1.0);
          newGraph.addFeature(wordGrelFeature);
          learningModel.setWeightIfAbsent(wordGrelFeature, initialWordWeight);
        }

        if (wordBigramGrelPartFlag) {
          String mediatorWord = mediator.getLemma();
          Set<Type<LexicalItem>> mediatorTypes = ungroundedGraph.getTypes(mediator);
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
              key = Lists.newArrayList(mediatorWord, modifierNodeString, grelLeft);
              WordBigramGrelPartFeature wordBigramGrelPartFeature =
                  new WordBigramGrelPartFeature(key, 1.0);
              newGraph.addFeature(wordBigramGrelPartFeature);
              learningModel.setWeightIfAbsent(wordBigramGrelPartFeature, initialWordWeight);

              key = Lists.newArrayList(mediatorWord, modifierNodeString, grelRight);
              wordBigramGrelPartFeature = new WordBigramGrelPartFeature(key, 1.0);
              newGraph.addFeature(wordBigramGrelPartFeature);
              learningModel.setWeightIfAbsent(wordBigramGrelPartFeature, initialWordWeight);
            }
          }

          // agreed to direct
          Set<Type<LexicalItem>> eventModifierNodes =
              ungroundedGraph.getEventEventModifiers(mediator);
          if (eventModifierNodes != null) {
            for (Type<LexicalItem> type : eventModifierNodes) {
              LexicalItem modifierNode = type.getModifierNode();
              String modifierNodeString = modifierNode.getLemma();
              key = Lists.newArrayList(mediatorWord, modifierNodeString, grelLeft);
              WordBigramGrelPartFeature wordBigramGrelPartFeature =
                  new WordBigramGrelPartFeature(key, 1.0);
              newGraph.addFeature(wordBigramGrelPartFeature);
              learningModel.setWeightIfAbsent(wordBigramGrelPartFeature, initialWordWeight);

              key = Lists.newArrayList(mediatorWord, modifierNodeString, grelRight);
              wordBigramGrelPartFeature = new WordBigramGrelPartFeature(key, 1.0);
              newGraph.addFeature(wordBigramGrelPartFeature);
              learningModel.setWeightIfAbsent(wordBigramGrelPartFeature, initialWordWeight);
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

          List<String> parts = Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
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
          grelRightStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

          parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
          toIndex = parts.size();
          fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
          grelRightInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));

          String mediatorWord = mediator.getLemma();
          if (!newGraph.mediatorsStemsMatched.contains(mediator) && !mediator.getPos().equals("IN")
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
              StemMatchingFeature s = newGraph.stemMatchingFeature;
              s.setFeatureValue(s.getFeatureValue() + 1.0);
              newGraph.mediatorsStemsMatched.add(mediator);
            }
          }

          Set<Type<LexicalItem>> mediatorTypes = ungroundedGraph.getTypes(mediator);
          if (mediatorTypes != null) {
            // birth place, place of birth (from madeup type
            // modifiers in function getUngroundedGraph)
            for (Type<LexicalItem> type : mediatorTypes) {
              LexicalItem modifierNode = type.getModifierNode();
              if (newGraph.mediatorsStemsMatched.contains(modifierNode)
                  || modifierNode.getPos().equals("IN")
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
                StemMatchingFeature s = newGraph.stemMatchingFeature;
                s.setFeatureValue(s.getFeatureValue() + 1.0);
                newGraph.mediatorsStemsMatched.add(modifierNode);
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

          List<String> parts = Lists.newArrayList(Splitter.on(".").split(grelLeftStripped));
          int toIndex = parts.size();
          int fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
          if (!grelLeftStripped.endsWith("inverse"))
            grelLeftStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
          else
            grelLeftStripped =
                Joiner.on(".").join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));;

          parts = Lists.newArrayList(Splitter.on(".").split(grelLeftInverse));
          toIndex = parts.size();
          fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
          if (!grelLeftInverse.endsWith("inverse"))
            grelLeftInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
          else
            grelLeftInverse =
                Joiner.on(".").join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

          parts = Lists.newArrayList(Splitter.on(".").split(grelRightStripped));
          toIndex = parts.size();
          fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
          if (!grelRightStripped.endsWith("inverse"))
            grelRightStripped = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
          else
            grelRightStripped =
                Joiner.on(".").join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

          parts = Lists.newArrayList(Splitter.on(".").split(grelRightInverse));
          toIndex = parts.size();
          fromIndex = toIndex - 2 < 0 ? 0 : toIndex - 2;
          if (!grelRightInverse.contains("inverse"))
            grelRightInverse = Joiner.on(".").join(parts.subList(fromIndex, toIndex));
          else
            grelRightInverse =
                Joiner.on(".").join(parts.subList(fromIndex > 0 ? fromIndex - 1 : 0, toIndex - 1));

          String mediatorWord = mediator.getLemma();
          if (!newGraph.mediatorStemGrelPartMatchedNodes.contains(mediator)
              && !mediator.getPos().equals("IN")
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
              MediatorStemGrelPartMatchingFeature s = newGraph.mediatorStemGrelPartMatchingFeature;
              s.setFeatureValue(s.getFeatureValue() + 1.0);
              newGraph.mediatorStemGrelPartMatchedNodes.add(mediator);
            }
          }

          Set<Type<LexicalItem>> mediatorTypes = ungroundedGraph.getTypes(mediator);
          if (mediatorTypes != null) {
            // birth place, place of birth (from madeup type
            // modifiers in function getUngroundedGraph)
            for (Type<LexicalItem> type : mediatorTypes) {
              LexicalItem modifierNode = type.getModifierNode();
              if (newGraph.mediatorStemGrelPartMatchedNodes.contains(modifierNode)
                  || modifierNode.getPos().equals("IN")
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
                    newGraph.mediatorStemGrelPartMatchingFeature;
                s.setFeatureValue(s.getFeatureValue() + 1.0);
                newGraph.mediatorStemGrelPartMatchedNodes.add(modifierNode);
              }
            }
          }
        }

        Double score;
        // compute score of the new graph
        if (testing) {
          score = learningModel.getScoreTesting(newGraph.getFeatures());
        } else {
          score = learningModel.getScoreTraining(newGraph.getFeatures());
        }
        newGraph.setScore(score);
        tempGraphs.add(newGraph);

        /*-Double score = 0.0;
        score += groundedLexicon.getUrelPartGrelPartProb(urelLeft, grelLeft);
        score += groundedLexicon.getUrelPartGrelPartProb(urelRight, grelRight);
        score += groundedRelation.getWeight();
        newGraph.addScore(score);*/
      }
    }

    // Add empty edge i.e. just nodes to each of the old graphs
    for (LexicalGraph oldGraph : groundedGraphs) {
      oldGraph.addNode(node1);
      oldGraph.addNode(node2);
      tempGraphs.add(oldGraph);
    }
    // start again with new set of graphs
    return tempGraphs;
  }

  /**
   * 
   * If any of the entities is of standard type, remove relations that do not contain standard types
   * as arguments. Additionally if the relation has type.datetime as argument, make sure the entity
   * allows it.
   * 
   * @param groundedRelation
   * @param node1
   * @param node2
   * @return
   */
  private boolean checkValidRelation(Relation groundedRelation, LexicalItem node1, LexicalItem node2) {
    String mid1 = node1.getMid();
    String mid2 = node2.getMid();

    if (standardTypes.contains(mid1) || standardTypes.contains(mid2)) {
      Set<String> entity1Types = standardTypes.contains(mid1) ? Sets.newHashSet(mid1) : null;
      Set<String> entity2Types = standardTypes.contains(mid2) ? Sets.newHashSet(mid2) : null;
      return checkIsValidRelation(groundedRelation, entity1Types, entity2Types);
    } else {
      String leftEdge = groundedRelation.getLeft();
      String rightEdge = groundedRelation.getRight();

      String arg1Type;
      String arg2Type;

      if (leftEdge.substring(0, leftEdge.length() - 2).equals(
          rightEdge.substring(0, rightEdge.length() - 2))) {
        // relation is not of type mediator in the database.
        int length = leftEdge.length();
        // System.out.println(relationName);

        if (leftEdge.charAt(length - 1) == '1' && rightEdge.charAt(length - 1) == '2') {
          String relationName = leftEdge.substring(0, length - 2);
          List<String> args = schema.getRelationArguments(relationName);
          arg1Type = args.get(0);
          arg2Type = args.get(1);
        } else if (leftEdge.charAt(length - 1) == '2' && rightEdge.charAt(length - 1) == '1') {
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
      // type.datetime is restricted and allowed to come with only few
      // words
      if (arg1Type.equals("type.datetime") || arg2Type.equals("type.datetime"))
        return false;
      return true;
    }
  }

  private boolean checkIsValidRelation(Relation groundedRelation, Set<String> entity1Types,
      Set<String> entity2Types) {
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

      if (leftEdge.charAt(length - 1) == '1' && rightEdge.charAt(length - 1) == '2') {
        String relationName = leftEdge.substring(0, length - 2);
        List<String> args = schema.getRelationArguments(relationName);
        arg1Type = args.get(0);
        arg2Type = args.get(1);
      } else if (leftEdge.charAt(length - 1) == '2' && rightEdge.charAt(length - 1) == '1') {
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
}
