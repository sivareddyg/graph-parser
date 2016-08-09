package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.learning.GraphToQueryTraining;
import in.sivareddy.graphparser.learning.GraphToQueryTrainingMain;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RunGraphToQueryTrainingMain extends AbstractCli {

  // Sparql End point and details
  private OptionSpec<String> endpoint;

  // Freebase relation to identity the type of an entity.
  private OptionSpec<String> typeKey;

  // Schema File
  private OptionSpec<String> schema;

  // CCG Bank co-indexed mapping, non-standard unary rules, and non-standard
  // binary rules.
  private OptionSpec<String> ccgIndexedMapping;
  private OptionSpec<String> unaryRules;
  private OptionSpec<String> binaryRules;

  // CCG special categories lexicon
  private OptionSpec<String> ccgLexicon;
  private OptionSpec<String> ccgLexiconQuestions;

  // Relations that are potential types
  private OptionSpec<String> relationTypesFile;

  // Log File
  private OptionSpec<String> logFile;
  private OptionSpec<String> loadModelFromFile;
  private OptionSpec<String> lexicon;
  private OptionSpec<String> cachedKB;
  private OptionSpec<String> testFile;
  private OptionSpec<String> devFile;

  // Domain Name
  private OptionSpec<String> domain;

  // Content Pos Tags
  private OptionSpec<String> contentWordPosTags;

  // Training Corpora
  private OptionSpec<String> trainingCorpora;
  private OptionSpec<String> supervisedCorpus;
  private OptionSpec<String> semanticParseKey;
  private OptionSpec<String> goldParsesFile;
  private OptionSpec<String> mostFrequentTypesFile;

  // Optional corpus to be grounded.
  private OptionSpec<String> groundInputCorpora;
  private OptionSpec<Boolean> groundTrainingCorpusInTheEnd;

  private OptionSpec<Integer> trainingSampleSize;
  private OptionSpec<Integer> nthreads;
  private OptionSpec<Integer> timeout;
  private OptionSpec<Integer> iterations;

  // Try nbest syntactic parses
  private OptionSpec<Integer> nBestTrainSyntacticParses;
  private OptionSpec<Integer> nBestTestSyntacticParses;
  private OptionSpec<Integer> nbestGraphs;
  private OptionSpec<Integer> nbestEdges;
  private OptionSpec<Integer> forestSize;
  private OptionSpec<Integer> ngramLength;

  // Set these true, or else graph construction mechanism will be
  // completely driven by lexicon
  private OptionSpec<Boolean> debugEnabledFlag;
  private OptionSpec<Boolean> useSchema;
  private OptionSpec<Boolean> useKB;
  private OptionSpec<Boolean> groundFreeVariables;
  private OptionSpec<Boolean> groundEntityVariableEdges;
  private OptionSpec<Boolean> groundEntityEntityEdges;
  private OptionSpec<Boolean> useEmptyTypes;
  private OptionSpec<Boolean> ignoreTypes;

  // Alignment Features
  private OptionSpec<Boolean> urelGrelFlag;
  private OptionSpec<Boolean> urelPartGrelPartFlag;
  private OptionSpec<Boolean> utypeGtypeFlag;
  // gtypeGrel imposes strong biases - do not use for cai-yates
  private OptionSpec<Boolean> gtypeGrelFlag;

  // Contextual Features
  private OptionSpec<Boolean> ngramGrelPartFlag;
  private OptionSpec<Boolean> wordGrelPartFlag;
  private OptionSpec<Boolean> wordGrelFlag;
  private OptionSpec<Boolean> eventTypeGrelPartFlag;
  private OptionSpec<Boolean> argGrelPartFlag;
  private OptionSpec<Boolean> argGrelFlag;
  private OptionSpec<Boolean> questionTypeGrelPartFlag;

  // Stem features
  private OptionSpec<Boolean> stemMatchingFlag;
  private OptionSpec<Boolean> mediatorStemGrelPartMatchingFlag;
  private OptionSpec<Boolean> argumentStemMatchingFlag;
  private OptionSpec<Boolean> argumentStemGrelPartMatchingFlag;

  // Graph features
  private OptionSpec<Boolean> graphIsConnectedFlag;
  // edgeNodeCountFlag entails graphHasEdgeFlag
  private OptionSpec<Boolean> graphHasEdgeFlag;
  private OptionSpec<Boolean> countNodesFlag;
  // edgeNodeCountFlag and duplicateEdgesFlag are important on cai yates
  private OptionSpec<Boolean> edgeNodeCountFlag;
  private OptionSpec<Boolean> duplicateEdgesFlag;
  // surprisingly grelGrel not useful
  private OptionSpec<Boolean> grelGrelFlag;

  // Default weights
  private OptionSpec<Double> pointWiseF1Threshold;
  private OptionSpec<Boolean> useLexiconWeightsRel;
  private OptionSpec<Boolean> useLexiconWeightsType;

  private OptionSpec<Double> initialEdgeWeight;
  private OptionSpec<Double> initialTypeWeight;
  private OptionSpec<Double> initialWordWeight;
  private OptionSpec<Double> stemFeaturesWeight;

  // Denotation feature
  private OptionSpec<Boolean> validQueryFlag;
  private OptionSpec<Boolean> useAnswerTypeQuestionWordFlag;

  // Other features
  private OptionSpec<Boolean> useNbestGraphsFlag;
  private OptionSpec<Boolean> addBagOfWordsGraphFlag;
  private OptionSpec<Boolean> addOnlyBagOfWordsGraphFlag;
  private OptionSpec<Boolean> handleNumbersFlag;
  private OptionSpec<Boolean> entityScoreFlag;
  private OptionSpec<Boolean> entityWordOverlapFlag;
  private OptionSpec<Boolean> paraphraseScoreFlag;
  private OptionSpec<Boolean> paraphraseClassifierScoreFlag;
  private OptionSpec<Boolean> allowMerging;
  private OptionSpec<Boolean> useGoldRelations;
  private OptionSpec<Boolean> evaluateOnlyTheFirstBest;
  private OptionSpec<Boolean> evaluateBeforeTraining;
  private OptionSpec<Boolean> handleEventEventEdges;
  private OptionSpec<Boolean> useBackOffGraph;
  private OptionSpec<Boolean> useHyperExpand;

  @Override
  public void initializeOptions(OptionParser parser) {
    parser.acceptsAll(Arrays.asList("help", "h"), "Print this help message.");

    endpoint =
        parser.accepts("endpoint", "SPARQL endpoint").withRequiredArg()
            .ofType(String.class).required();

    typeKey =
        parser
            .accepts(
                "typeKey",
                "Freebase relation name to identify the type of an entity. e.g. rdf:type or fb:type.object.type")
            .withRequiredArg().ofType(String.class).defaultsTo("rdf:type");

    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();

    ccgIndexedMapping =
        parser
            .accepts("ccgIndexedMapping",
                "Co-indexation information for categories").withRequiredArg()
            .ofType(String.class)
            .defaultsTo("./lib_data/candc_markedup.modified");

    unaryRules =
        parser.accepts("unaryRules", "Type-Changing Rules in CCGbank")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./lib_data/unary_rules.txt");

    binaryRules =
        parser.accepts("binaryRules", "Binary Type-Changing rules in CCGbank")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./lib_data/binary_rules.txt");

    ccgLexicon =
        parser.accepts("ccgLexicon", "ccg special categories lexicon")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./lib_data/lexicon_specialCases.txt");

    ccgLexiconQuestions =
        parser
            .accepts("ccgLexiconQuestions",
                "ccg special categories Questions lexicon").withRequiredArg()
            .ofType(String.class)
            .defaultsTo("./lib_data/lexicon_specialCases_questions.txt");

    relationTypesFile =
        parser
            .accepts(
                "relationTypesFile",
                "File containing relations that may be potential types e.g. data/freebase/stats/business_relation_types.txt")
            .withRequiredArg().ofType(String.class).required();

    domain =
        parser
            .accepts("domain",
                "uri of the graph e.g. http://film.freebase.com. Specify multiple Uri using ;")
            .withRequiredArg().ofType(String.class).required();

    contentWordPosTags =
        parser
            .accepts("contentWordPosTags",
                "content Word Pos tags for extracting ngram features. Seperate each tag with ;")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    trainingCorpora =
        parser
            .accepts("trainingCorpora",
                "all unannotated training corpora separated by ;")
            .withRequiredArg().ofType(String.class).defaultsTo("");
    supervisedCorpus =
        parser.accepts("supervisedCorpus", "annotated training corpus file")
            .withRequiredArg().ofType(String.class).defaultsTo("");
    semanticParseKey =
        parser
            .accepts("semanticParseKey",
                "key from which a semantic parse is read").withRequiredArg()
            .ofType(String.class).defaultsTo("synPars");
    goldParsesFile =
        parser
            .accepts("goldParsesFileVal",
                "Serialized file containing gold graphs for trianing sentences")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    mostFrequentTypesFile =
        parser
            .accepts("mostFrequentTypesFile",
                "File containing most frequent Freebase types. Useful to extract answer types")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    groundInputCorpora =
        parser
            .accepts(
                "groundInputCorpora",
                "Use this option to ground an input corpus. Multiple corpus files should be separated by ;")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    groundTrainingCorpusInTheEnd =
        parser
            .accepts(
                "groundTrainingCorpusInTheEnd",
                "set this flag to true to ground the training corpus with the best model after training procedure")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    logFile =
        parser.accepts("logFile", "log file").withRequiredArg()
            .ofType(String.class).required();

    loadModelFromFile =
        parser
            .accepts("loadModelFromFile",
                "Load model from serialized model file").withRequiredArg()
            .ofType(String.class).defaultsTo("");

    lexicon =
        parser.accepts("lexicon", "lexicon containing nl to grounded mappings")
            .withRequiredArg().ofType(String.class).required();

    cachedKB =
        parser.accepts("cachedKB", "cached version of KB").withRequiredArg()
            .ofType(String.class).defaultsTo("");

    testFile =
        parser.accepts("testFile", "test file containinig questions")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    devFile =
        parser.accepts("devFile", "development file containinig questions")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    nthreads =
        parser.accepts("nthreads", "number of threads").withRequiredArg()
            .ofType(Integer.class).required();

    timeout =
        parser
            .accepts("timeout",
                "timeout for each sparql query in milli seconds")
            .withRequiredArg().ofType(Integer.class).defaultsTo(10000);

    trainingSampleSize =
        parser
            .accepts("trainingSampleSize",
                "number of training samples used in each iteration")
            .withRequiredArg().ofType(Integer.class).defaultsTo(600);

    iterations =
        parser.accepts("iterations", "number of training iterations")
            .withRequiredArg().ofType(Integer.class).defaultsTo(0);

    nBestTrainSyntacticParses =
        parser
            .accepts("nBestTrainSyntacticParses",
                "number of syntactic parses to use while training")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1);
    nBestTestSyntacticParses =
        parser
            .accepts("nBestTestSyntacticParses",
                "number of syntactic parses to use while testing")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1);
    nbestGraphs =
        parser.accepts("nbestGraphs", "beam size").withRequiredArg()
            .ofType(Integer.class).defaultsTo(1000);
    nbestEdges =
        parser
            .accepts("nbestEdges",
                "number of edges/types for each ungrounded edge/types")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1000);

    forestSize =
        parser
            .accepts("forestSize",
                "maximum number of sentences to consider in the forrest")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1);

    ngramLength =
        parser
            .accepts(
                "ngramLength",
                "if ngrams feature is activated, the length of ngrams to be considered. ngrams of lower length are automatically added.")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1);

    debugEnabledFlag =
        parser.accepts("debugEnabledFlag", "Enable debug mode")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    // Set these true, or else graph construction mechanism will be
    // completely driven by lexicon
    useSchema =
        parser
            .accepts(
                "useSchema",
                "use schema to drive graph construction - use this flag, otherwise search space explodes")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    useKB =
        parser
            .accepts("useKB",
                "use KB to drive graph construction, reduces search space drastically")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    groundFreeVariables =
        parser
            .accepts(
                "groundFreeVariables",
                "Ground free variables which do not have any entity clue e.g. ground city(x) where x is not known")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    groundEntityVariableEdges =
        parser
            .accepts("groundEntityVariableEdges",
                "Ground the edges between entities and variables.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    groundEntityEntityEdges =
        parser
            .accepts("groundEntityEntityEdges",
                "Ground the edges between entities and entities.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useEmptyTypes =
        parser.accepts("useEmptyTypes", "use type.empty for empty types")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    ignoreTypes =
        parser
            .accepts("ignoreTypes",
                "ignore types: subsumes groundFreeVariables and useEmptyTypes")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    // Alignment Features
    urelGrelFlag =
        parser
            .accepts("urelGrelFlag", "Edge Alignment features - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    urelPartGrelPartFlag =
        parser
            .accepts("urelPartGrelPartFlag",
                "subedge alignment features - depends on dataset")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    utypeGtypeFlag =
        parser
            .accepts("utypeGtypeFlag",
                "type alignment features - a good feature").withRequiredArg()
            .ofType(Boolean.class).defaultsTo(true);
    // gtypeGrel imposes strong biases - do not use for cai-yates
    gtypeGrelFlag =
        parser
            .accepts("gtypeGrelFlag",
                "edge argument restrictions - depends on dataset")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    // Contextual Features
    ngramGrelPartFlag =
        parser.accepts("ngramGrelPartFlag", "bag of word features")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    wordGrelPartFlag =
        parser
            .accepts("wordGrelPartFlag",
                "event word and edge feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    wordGrelFlag =
        parser
            .accepts("wordGrelFlag",
                "event word and edge feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    eventTypeGrelPartFlag =
        parser
            .accepts("eventTypeGrelPartFlag",
                "contextual features of a mediator node - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    argGrelPartFlag =
        parser
            .accepts("argGrelPartFlag",
                "argument word and edge feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    argGrelFlag =
        parser
            .accepts("argGrelFlag",
                "argument word and edge feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    questionTypeGrelPartFlag =
        parser
            .accepts("questionTypeGrelPartFlag",
                "question types and edge part feature").withRequiredArg()
            .ofType(Boolean.class).defaultsTo(false);

    // Stem features
    stemMatchingFlag =
        parser
            .accepts("stemMatchingFlag",
                "edge stem matching feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    mediatorStemGrelPartMatchingFlag =
        parser
            .accepts("mediatorStemGrelPartMatchingFlag",
                "subedge stem matching feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    argumentStemMatchingFlag =
        parser
            .accepts("argumentStemMatchingFlag",
                "argument word and edge stem matching feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    argumentStemGrelPartMatchingFlag =
        parser
            .accepts("argumentStemGrelPartMatchingFlag",
                "argument word and subedge stem matching feature - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    // Graph features
    graphIsConnectedFlag =
        parser
            .accepts("graphIsConnectedFlag",
                "graph is connected feature - imposes strong constraints")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    // edgeNodeCountFlag entails graphHasEdgeFlag
    graphHasEdgeFlag =
        parser.accepts("graphHasEdgeFlag", "graph has an edge feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    countNodesFlag =
        parser
            .accepts("countNodesFlag",
                "number of nodes in the graph - imposes strong constraints")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    // edgeNodeCountFlag and duplicateEdgesFlag are important on cai yates
    edgeNodeCountFlag =
        parser
            .accepts("edgeNodeCountFlag",
                "number of connected nodes - depends on dataset")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    duplicateEdgesFlag =
        parser
            .accepts("duplicateEdgesFlag",
                "remove duplicate edges next to each other - good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    // surprisingly grelGrel not useful
    grelGrelFlag =
        parser
            .accepts("grelGrelFlag",
                "edge-edge collocation feature - depends on dataset")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    // Default weights
    useLexiconWeightsRel =
        parser
            .accepts("useLexiconWeightsRel",
                "use noisy lexicon to initialise edge weights - a good feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
    useLexiconWeightsType =
        parser
            .accepts("useLexiconWeightsType",
                "use noisy lexicon to initialise type weights - depends on dataset")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);


    pointWiseF1Threshold =
        parser
            .accepts(
                "pointWiseF1Threshold",
                "update the gradient in supervised training only if predicted answer has F1 >= threshold")
            .withRequiredArg().ofType(Double.class).defaultsTo(0.90);

    initialEdgeWeight =
        parser
            .accepts("initialEdgeWeight",
                "set to a positive value if useLexiconWeightsRel is set")
            .withRequiredArg().ofType(Double.class).defaultsTo(1.0);
    initialTypeWeight =
        parser
            .accepts("initialTypeWeight",
                "set to a positive value if useLexiconWeightsType is set")
            .withRequiredArg().ofType(Double.class).defaultsTo(-2.0);
    initialWordWeight =
        parser
            .accepts("initialWordWeight",
                "set to a negative value to avoid overgenerating edges")
            .withRequiredArg().ofType(Double.class).defaultsTo(-1.0);
    stemFeaturesWeight =
        parser
            .accepts("stemFeaturesWeight", "initial weight of stem features.")
            .withRequiredArg().ofType(Double.class).defaultsTo(0.0);

    // Denotation feature
    validQueryFlag =
        parser
            .accepts("validQueryFlag",
                "use denotation as feature to see if the graph is valid - good but slow")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useAnswerTypeQuestionWordFlag =
        parser
            .accepts("useAnswerTypeQuestionWordFlag",
                "use answer type and question word feature, e.g., (type.datetime, when)")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useNbestGraphsFlag =
        parser
            .accepts(
                "useNbestGraphsFlag",
                "use n-best graphs for training. Unless you are in a supervised setting or using an unsupervised syntatic parser, set this to false")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    addBagOfWordsGraphFlag =
        parser
            .accepts(
                "addBagOfWordsGraph",
                "Adds a bag-of-words graph in-addition to the ungrounded graphs obtained from syntactic parse")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    addOnlyBagOfWordsGraphFlag =
        parser
            .accepts(
                "addOnlyBagOfWordsGraph",
                "Ignores all the ungrounded graphs from syntactic parses and just adds a bag-of-words graph. Automatically sets addBagOfWordsGraph to true.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    handleNumbersFlag =
        parser
            .accepts("handleNumbersFlag",
                "treat numbers specially and introduce COUNT. Suitable for Free917 data.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    entityScoreFlag =
        parser
            .accepts("entityScoreFlag",
                "use entity disambiguation scores as features")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    entityWordOverlapFlag =
        parser
            .accepts("entityWordOverlapFlag",
                "use entity phrase and entity name overlap features")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    paraphraseScoreFlag =
        parser.accepts("paraphraseScoreFlag", "use paraphrase scores")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    paraphraseClassifierScoreFlag =
        parser
            .accepts("paraphraseClassifierScoreFlag",
                "use paraphrase classifier scores").withRequiredArg()
            .ofType(Boolean.class).defaultsTo(false);

    allowMerging =
        parser
            .accepts("allowMerging",
                "Creates additional grounded graphs by merging nodes in the ungrounded graph")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useGoldRelations =
        parser
            .accepts("useGoldRelations",
                "use gold relations and gold mid for constructing gold graphs during training")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    evaluateOnlyTheFirstBest =
        parser
            .accepts("evaluateOnlyTheFirstBest",
                "evaluate only the first best test graph").withRequiredArg()
            .ofType(Boolean.class).defaultsTo(false);

    evaluateBeforeTraining =
        parser
            .accepts("evaluateBeforeTraining",
                "evaluate the initial model before training").withRequiredArg()
            .ofType(Boolean.class).defaultsTo(true);

    handleEventEventEdges =
        parser
            .accepts(
                "handleEventEventEdges",
                "Split event-event edge to event-entity, event-entity edges. Use only when the representation cannot handle control cases. ")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useBackOffGraph =
        parser
            .accepts(
                "useBackOffGraph",
                "Adds a back off graph if there is no path between question node and entity nodes.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useHyperExpand =
        parser
            .accepts("useHyperExpand",
                "Connects every entity with the question word if there is no direct path")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
  }

  @Override
  public void run(OptionSet options) {
    try {
      Schema schemaObj = new Schema(options.valueOf(schema));
      String relationTypesFileName = options.valueOf(relationTypesFile);
      KnowledgeBase kb = null;

      if (!options.valueOf(cachedKB).equals("")) {
        kb =
            new KnowledgeBaseCached(options.valueOf(cachedKB),
                relationTypesFileName);
      } else {
        KnowledgeBaseOnline.TYPE_KEY = options.valueOf(typeKey);
        kb =
            new KnowledgeBaseOnline(options.valueOf(endpoint), String.format(
                "http://%s:8890/sparql", options.valueOf(endpoint)), "dba",
                "dba", 50000, schemaObj);
      }

      RdfGraphTools rdfGraphTools =
          new RdfGraphTools(options.valueOf(endpoint), String.format(
              "http://%s:8890/sparql", options.valueOf(endpoint)), "dba",
              "dba", options.valueOf(timeout));
      GraphToSparqlConverter.TYPE_KEY = options.valueOf(typeKey);
      GroundedGraphs.CONTENT_WORD_POS =
          Sets.newHashSet(Splitter.on(";").trimResults().omitEmptyStrings()
              .split(options.valueOf(contentWordPosTags)));

      List<String> kbGraphUri =
          Lists.newArrayList(Splitter.on(";").split(options.valueOf(domain)));

      CcgAutoLexicon normalCcgAutoLexicon =
          new CcgAutoLexicon(options.valueOf(ccgIndexedMapping),
              options.valueOf(unaryRules), options.valueOf(binaryRules),
              options.valueOf(ccgLexicon));

      CcgAutoLexicon questionCcgAutoLexicon =
          new CcgAutoLexicon(options.valueOf(ccgIndexedMapping),
              options.valueOf(unaryRules), options.valueOf(binaryRules),
              options.valueOf(ccgLexiconQuestions));

      GroundedLexicon groundedLexicon =
          new GroundedLexicon(options.valueOf(lexicon));
      String testfile = options.valueOf(testFile);
      String devfile = options.valueOf(devFile);

      String supervisedTrainingFile = options.valueOf(supervisedCorpus);
      String corupusTrainingFile = options.valueOf(trainingCorpora);
      String groundInputCorporaFiles = options.valueOf(groundInputCorpora);
      String semanticParseKeyString = options.valueOf(semanticParseKey);
      String goldParsesFileVal = options.valueOf(goldParsesFile);
      String mostFrequentTypesFileVal = options.valueOf(mostFrequentTypesFile);

      String logfile = options.valueOf(logFile);
      String loadModelFromFileVal = options.valueOf(loadModelFromFile);
      boolean debugEnabled = options.valueOf(debugEnabledFlag);

      int threadCount = options.valueOf(nthreads);
      int iterationCount = options.valueOf(iterations);
      int trainingSampleSizeCount = options.valueOf(trainingSampleSize);

      int nBestTrainSyntacticParsesVal =
          options.valueOf(nBestTrainSyntacticParses);
      int nBestTestSyntacticParsesVal =
          options.valueOf(nBestTestSyntacticParses);
      int nbestGraphsVal = options.valueOf(nbestGraphs);
      int nbestEdgesVal = options.valueOf(nbestEdges);
      int forestSizeVal = options.valueOf(forestSize);
      int ngramLengthVal = options.valueOf(ngramLength);

      // Set these true, or else graph construction mechanism will be
      // completely driven by lexicon
      boolean useSchemaVal = options.valueOf(useSchema);
      boolean useKBVal = options.valueOf(useKB);
      boolean groundFreeVariablesVal = options.valueOf(groundFreeVariables);
      boolean groundEntityVariableEdgesVal =
          options.valueOf(groundEntityVariableEdges);
      boolean groundEntityEntityEdgesVal =
          options.valueOf(groundEntityEntityEdges);
      boolean useEmptyTypesVal = options.valueOf(useEmptyTypes);
      boolean ignoreTypesVal = options.valueOf(ignoreTypes);

      // Alignment Features
      boolean urelGrelFlagVal = options.valueOf(urelGrelFlag);
      boolean urelPartGrelPartFlagVal = options.valueOf(urelPartGrelPartFlag);
      boolean utypeGtypeFlagVal = options.valueOf(utypeGtypeFlag);
      // gtypeGrel imposes strong biases - do not use for cai-yates
      boolean gtypeGrelFlagVal = options.valueOf(gtypeGrelFlag);

      // Contextual Features
      boolean ngramGrelPartFlagVal = options.valueOf(ngramGrelPartFlag);
      boolean wordGrelPartFlagVal = options.valueOf(wordGrelPartFlag);
      boolean wordGrelFlagVal = options.valueOf(wordGrelFlag);
      boolean eventTypeGrelPartFlagVal = options.valueOf(eventTypeGrelPartFlag);
      boolean argGrelPartFlagVal = options.valueOf(argGrelPartFlag);
      boolean argGrelFlagVal = options.valueOf(argGrelFlag);
      boolean questionTypeGrelPartFlagVal =
          options.valueOf(questionTypeGrelPartFlag);

      // Stem features
      boolean stemMatchingFlagVal = options.valueOf(stemMatchingFlag);
      boolean mediatorStemGrelPartMatchingFlagVal =
          options.valueOf(mediatorStemGrelPartMatchingFlag);
      boolean argumentStemMatchingFlagVal =
          options.valueOf(argumentStemMatchingFlag);
      boolean argumentStemGrelPartMatchingFlagVal =
          options.valueOf(argumentStemGrelPartMatchingFlag);

      // Graph features
      boolean graphIsConnectedFlagVal = options.valueOf(graphIsConnectedFlag);
      // edgeNodeCountFlag entails graphHasEdgeFlag
      boolean graphHasEdgeFlagVal = options.valueOf(graphHasEdgeFlag);
      boolean countNodesFlagVal = options.valueOf(countNodesFlag);
      // edgeNodeCountFlag and duplicateEdgesFlag are important on cai
      // yates
      boolean edgeNodeCountFlagVal = options.valueOf(edgeNodeCountFlag);
      boolean duplicateEdgesFlagVal = options.valueOf(duplicateEdgesFlag);
      // surprisingly grelGrel not useful
      boolean grelGrelFlagVal = options.valueOf(grelGrelFlag);



      // Default weights
      boolean useLexiconWeightsRelVal = options.valueOf(useLexiconWeightsRel);
      boolean useLexiconWeightsTypeVal = options.valueOf(useLexiconWeightsType);

      double initialEdgeWeightVal = options.valueOf(initialEdgeWeight);
      double initialTypeWeightVal = options.valueOf(initialTypeWeight);
      double initialWordWeightVal = options.valueOf(initialWordWeight);
      double stemFeaturesWeightVal = options.valueOf(stemFeaturesWeight);

      // Denotation feature
      boolean validQueryFlagVal = options.valueOf(validQueryFlag);
      boolean useAnswerTypeQuestionWordFlagVal =
          options.valueOf(useAnswerTypeQuestionWordFlag);

      // Use n-best graphs for training. Unless you are using supervised
      // training or unsupervised syntactic parser, do not set this flag to
      // true.
      boolean useNbestGraphsVal = options.valueOf(useNbestGraphsFlag);

      boolean addBagOfWordsGraphVal = options.valueOf(addBagOfWordsGraphFlag);
      boolean addOnlyBagOfWordsGraphVal =
          options.valueOf(addOnlyBagOfWordsGraphFlag);
      boolean handleNumbersFlagVal = options.valueOf(handleNumbersFlag);

      boolean entityScoreFlagVal = options.valueOf(entityScoreFlag);
      boolean entityWordOverlapFlagVal = options.valueOf(entityWordOverlapFlag);
      boolean paraphraseScoreFlagVal = options.valueOf(paraphraseScoreFlag);
      boolean paraphraseClassifierScoreFlagVal =
          options.valueOf(paraphraseClassifierScoreFlag);
      boolean allowMergingVal = options.valueOf(allowMerging);
      boolean useGoldRelationsVal = options.valueOf(useGoldRelations);
      boolean evaluateOnlyTheFirstBestVal =
          options.valueOf(evaluateOnlyTheFirstBest);

      boolean evaluateBeforeTrainingVal =
          options.valueOf(evaluateBeforeTraining);
      boolean handleEventEventEdgesVal = options.valueOf(handleEventEventEdges);
      boolean useBackOffGraphVal = options.valueOf(useBackOffGraph);
      boolean useHyperExpandVal = options.valueOf(useHyperExpand);

      boolean groundTrainingCorpusInTheEndVal =
          options.valueOf(groundTrainingCorpusInTheEnd);

      // Set pointWiseF1Threshold for learning. IMPORTANT.
      GraphToQueryTraining.setPointWiseF1Threshold(options
          .valueOf(pointWiseF1Threshold));

      GraphToQueryTrainingMain graphToQueryModel =
          new GraphToQueryTrainingMain(schemaObj, kb, groundedLexicon,
              normalCcgAutoLexicon, questionCcgAutoLexicon, rdfGraphTools,
              kbGraphUri, testfile, devfile, supervisedTrainingFile,
              corupusTrainingFile, groundInputCorporaFiles,
              semanticParseKeyString, goldParsesFileVal,
              mostFrequentTypesFileVal, debugEnabled,
              groundTrainingCorpusInTheEndVal, trainingSampleSizeCount,
              logfile, loadModelFromFileVal, nBestTrainSyntacticParsesVal,
              nBestTestSyntacticParsesVal, nbestEdgesVal, nbestGraphsVal,
              forestSizeVal, ngramLengthVal, useSchemaVal, useKBVal,
              groundFreeVariablesVal, groundEntityVariableEdgesVal,
              groundEntityEntityEdgesVal, useEmptyTypesVal, ignoreTypesVal,
              urelGrelFlagVal, urelPartGrelPartFlagVal, utypeGtypeFlagVal,
              gtypeGrelFlagVal, ngramGrelPartFlagVal, wordGrelPartFlagVal,
              wordGrelFlagVal, eventTypeGrelPartFlagVal, argGrelPartFlagVal,
              argGrelFlagVal, questionTypeGrelPartFlagVal, stemMatchingFlagVal,
              mediatorStemGrelPartMatchingFlagVal, argumentStemMatchingFlagVal,
              argumentStemGrelPartMatchingFlagVal, graphIsConnectedFlagVal,
              graphHasEdgeFlagVal, countNodesFlagVal, edgeNodeCountFlagVal,
              duplicateEdgesFlagVal, grelGrelFlagVal, useLexiconWeightsRelVal,
              useLexiconWeightsTypeVal, validQueryFlagVal,
              useAnswerTypeQuestionWordFlagVal, useNbestGraphsVal,
              addBagOfWordsGraphVal, addOnlyBagOfWordsGraphVal,
              handleNumbersFlagVal, entityScoreFlagVal,
              entityWordOverlapFlagVal, paraphraseScoreFlagVal,
              paraphraseClassifierScoreFlagVal, allowMergingVal,
              useGoldRelationsVal, evaluateOnlyTheFirstBestVal,
              handleEventEventEdgesVal, useBackOffGraphVal, useHyperExpandVal,
              initialEdgeWeightVal, initialTypeWeightVal, initialWordWeightVal,
              stemFeaturesWeightVal);
      graphToQueryModel.train(iterationCount, threadCount,
          evaluateBeforeTrainingVal);

      // Run the best model.
      graphToQueryModel.testBestModel(threadCount);

      if (groundInputCorporaFiles != null
          && !groundInputCorporaFiles.equals("")) {
        graphToQueryModel.groundSentences(threadCount);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    new RunGraphToQueryTrainingMain().run(args);
  }
}
