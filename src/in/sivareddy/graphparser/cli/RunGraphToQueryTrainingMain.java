package in.sivareddy.graphparser.cli;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.learning.GraphToQueryTrainingMain;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RunGraphToQueryTrainingMain extends AbstractCli {

  // Sparql End point and details
  private OptionSpec<String> endpoint;

  // Schema File
  private OptionSpec<String> schema;

  // CCG special categories lexicon
  private OptionSpec<String> ccgLexicon;
  private OptionSpec<String> ccgLexiconQuestions;

  // Relations that are potential types
  private OptionSpec<String> relationTypesFile;

  // Log File
  private OptionSpec<String> logFile;
  private OptionSpec<String> lexicon;
  private OptionSpec<String> cachedKB;
  private OptionSpec<String> testFile;
  private OptionSpec<String> devFile;

  // Domain Name
  private OptionSpec<String> domain;

  // Training Corpora
  private OptionSpec<String> trainingCorpora;
  private OptionSpec<String> supervisedCorpus;
  private OptionSpec<String> semanticParseKey;

  private OptionSpec<Integer> trainingSampleSize;
  private OptionSpec<Integer> nthreads;
  private OptionSpec<Integer> iterations;

  // Try nbest syntactic parses
  private OptionSpec<Integer> nBestTrainSyntacticParses;
  private OptionSpec<Integer> nBestTestSyntacticParses;
  private OptionSpec<Integer> nbestGraphs;
  private OptionSpec<Integer> nbestEdges;

  // Set these true, or else graph construction mechanism will be
  // completely driven by lexicon
  private OptionSpec<Boolean> useSchema;
  private OptionSpec<Boolean> useKB;
  private OptionSpec<Boolean> groundFreeVariables;
  private OptionSpec<Boolean> useEmptyTypes;
  private OptionSpec<Boolean> ignoreTypes;

  // Alignment Features
  private OptionSpec<Boolean> urelGrelFlag;
  private OptionSpec<Boolean> urelPartGrelPartFlag;
  private OptionSpec<Boolean> utypeGtypeFlag;
  // gtypeGrel imposes strong biases - do not use for cai-yates
  private OptionSpec<Boolean> gtypeGrelFlag;

  // Contextual Features
  private OptionSpec<Boolean> wordGrelPartFlag;
  private OptionSpec<Boolean> wordGrelFlag;
  private OptionSpec<Boolean> wordBigramGrelPartFlag;
  private OptionSpec<Boolean> argGrelPartFlag;
  private OptionSpec<Boolean> argGrelFlag;

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
  private OptionSpec<Boolean> useLexiconWeightsRel;
  private OptionSpec<Boolean> useLexiconWeightsType;

  private OptionSpec<Double> initialEdgeWeight;
  private OptionSpec<Double> initialTypeWeight;
  private OptionSpec<Double> initialWordWeight;
  private OptionSpec<Double> stemFeaturesWeight;

  // Denotation feature
  private OptionSpec<Boolean> validQueryFlag;

  @Override
  public void initializeOptions(OptionParser parser) {
    parser.acceptsAll(Arrays.asList("help", "h"), "Print this help message.");

    endpoint =
        parser.accepts("endpoint", "SPARQL endpoint").withRequiredArg()
            .ofType(String.class).required();

    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();

    ccgLexicon =
        parser.accepts("ccgLexicon", "ccg special categories lexicon")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./data/lexicon_specialCases.txt");
    ccgLexiconQuestions =
        parser
            .accepts("ccgLexiconQuestions",
                "ccg special categories Questions lexicon").withRequiredArg()
            .ofType(String.class)
            .defaultsTo("./data/lexicon_specialCases_questions.txt");

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

    logFile =
        parser.accepts("logFile", "log file").withRequiredArg()
            .ofType(String.class).required();

    lexicon =
        parser.accepts("lexicon", "lexicon containing nl to grounded mappings")
            .withRequiredArg().ofType(String.class).required();

    cachedKB =
        parser.accepts("cachedKB", "cached version of KB").withRequiredArg()
            .ofType(String.class).required();

    testFile =
        parser.accepts("testFile", "test file containinig questions")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    devFile =
        parser.accepts("devFile", "development file containinig questions")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    nthreads =
        parser.accepts("nthreads", "number of threads: >10 preferred")
            .withRequiredArg().ofType(Integer.class).required();
    trainingSampleSize =
        parser
            .accepts("trainingSampleSize",
                "number of training samples used in each iteration")
            .withRequiredArg().ofType(Integer.class).required();
    iterations =
        parser.accepts("iterations", "number of training iterations")
            .withRequiredArg().ofType(Integer.class).required();

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
            .ofType(Integer.class).defaultsTo(100);
    nbestEdges =
        parser
            .accepts("nbestEdges",
                "number of edges/types for each ungrounded edge/types")
            .withRequiredArg().ofType(Integer.class).defaultsTo(20);

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
    wordBigramGrelPartFlag =
        parser
            .accepts("wordBigramGrelPartFlag",
                "contextual features - a good feature").withRequiredArg()
            .ofType(Boolean.class).defaultsTo(false);
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

  }

  @Override
  public void run(OptionSet options) {

    try {
      Schema schemaObj = new Schema(options.valueOf(schema));
      String relationTypesFileName = options.valueOf(relationTypesFile);
      KnowledgeBase kb =
          new KnowledgeBase(options.valueOf(cachedKB), relationTypesFileName);

      RdfGraphTools rdfGraphTools =
          new RdfGraphTools(String.format("jdbc:virtuoso://%s:1111",
              options.valueOf(endpoint)), String.format(
              "http://%s:8890/sparql", options.valueOf(endpoint)), "dba",
              "dba", 4);
      List<String> kbGraphUri =
          Lists.newArrayList(Splitter.on(";").split(options.valueOf(domain)));

      CcgAutoLexicon normalCcgAutoLexicon =
          new CcgAutoLexicon("./data/candc_markedup.modified",
              "./data/unary_rules.txt", "./data/binary_rules.txt",
              options.valueOf(ccgLexicon));
      CcgAutoLexicon questionCcgAutoLexicon =
          new CcgAutoLexicon("./data/candc_markedup.modified",
              "./data/unary_rules.txt", "./data/binary_rules.txt",
              options.valueOf(ccgLexiconQuestions));

      GroundedLexicon groundedLexicon =
          new GroundedLexicon(options.valueOf(lexicon));
      String testfile = options.valueOf(testFile);
      String devfile = options.valueOf(devFile);

      String supervisedTrainingFile = options.valueOf(supervisedCorpus);
      String corupusTrainingFile = options.valueOf(trainingCorpora);
      String semanticParseKeyString = options.valueOf(semanticParseKey);

      String logfile = options.valueOf(logFile);
      boolean debugEnabled = true;

      int threadCount = options.valueOf(nthreads);
      int iterationCount = options.valueOf(iterations);
      int trainingSampleSizeCount = options.valueOf(trainingSampleSize);

      int nBestTrainSyntacticParsesVal =
          options.valueOf(nBestTrainSyntacticParses);
      int nBestTestSyntacticParsesVal =
          options.valueOf(nBestTestSyntacticParses);
      int nbestGraphsVal = options.valueOf(nbestGraphs);
      int nbestEdgesVal = options.valueOf(nbestEdges);

      // Set these true, or else graph construction mechanism will be
      // completely driven by lexicon
      boolean useSchemaVal = options.valueOf(useSchema);
      boolean useKBVal = options.valueOf(useKB);
      boolean groundFreeVariablesVal = options.valueOf(groundFreeVariables);
      boolean useEmptyTypesVal = options.valueOf(useEmptyTypes);
      boolean ignoreTypesVal = options.valueOf(ignoreTypes);

      // Alignment Features
      boolean urelGrelFlagVal = options.valueOf(urelGrelFlag);
      boolean urelPartGrelPartFlagVal = options.valueOf(urelPartGrelPartFlag);
      boolean utypeGtypeFlagVal = options.valueOf(utypeGtypeFlag);
      // gtypeGrel imposes strong biases - do not use for cai-yates
      boolean gtypeGrelFlagVal = options.valueOf(gtypeGrelFlag);

      // Contextual Features
      boolean wordGrelPartFlagVal = options.valueOf(wordGrelPartFlag);
      boolean wordGrelFlagVal = options.valueOf(wordGrelFlag);
      boolean wordBigramGrelPartFlagVal =
          options.valueOf(wordBigramGrelPartFlag);
      boolean argGrelPartFlagVal = options.valueOf(argGrelPartFlag);
      boolean argGrelFlagVal = options.valueOf(argGrelFlag);

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

      GraphToQueryTrainingMain graphToQueryModel =
          new GraphToQueryTrainingMain(schemaObj, kb, groundedLexicon,
              normalCcgAutoLexicon, questionCcgAutoLexicon, rdfGraphTools,
              kbGraphUri, testfile, devfile, supervisedTrainingFile,
              corupusTrainingFile, semanticParseKeyString, debugEnabled,
              trainingSampleSizeCount, logfile, nBestTrainSyntacticParsesVal,
              nBestTestSyntacticParsesVal, nbestEdgesVal, nbestGraphsVal,
              useSchemaVal, useKBVal, groundFreeVariablesVal, useEmptyTypesVal,
              ignoreTypesVal, urelGrelFlagVal, urelPartGrelPartFlagVal,
              utypeGtypeFlagVal, gtypeGrelFlagVal, wordGrelPartFlagVal,
              wordGrelFlagVal, wordBigramGrelPartFlagVal, argGrelPartFlagVal,
              argGrelFlagVal, stemMatchingFlagVal,
              mediatorStemGrelPartMatchingFlagVal, argumentStemMatchingFlagVal,
              argumentStemGrelPartMatchingFlagVal, graphIsConnectedFlagVal,
              graphHasEdgeFlagVal, countNodesFlagVal, edgeNodeCountFlagVal,
              duplicateEdgesFlagVal, grelGrelFlagVal, useLexiconWeightsRelVal,
              useLexiconWeightsTypeVal, initialEdgeWeightVal,
              initialTypeWeightVal, initialWordWeightVal,
              stemFeaturesWeightVal, validQueryFlagVal);
      graphToQueryModel.train(iterationCount, threadCount);

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
