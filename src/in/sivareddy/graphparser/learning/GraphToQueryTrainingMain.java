package in.sivareddy.graphparser.learning;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.ml.learning.StructuredPercepton;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public class GraphToQueryTrainingMain {

  private GraphToQueryTraining graphToQuery;
  private List<List<String>> trainingExamples;
  private List<String> testingExamples;
  private List<String> devExamples;
  private List<String> supervisedTrainingExamples;
  private Integer trainingSampleSize;
  private Logger logger;
  PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
  private String logFile = "/dev/null";
  private boolean debugEnabled = false;
  private List<Integer> testingNbestParsesRange = Lists.newArrayList(1, 5, 10, 20, 50, 100);
  private int nBestTestSyntacticParses;
  private int nBestTrainSyntacticParses;
  private String semanticParseKey;

  // Cai and Yates annotations do not handle types properly. Though types are
  // useful in training, we ignore them during testing as we work with
  // CaiYates.

  public GraphToQueryTrainingMain(Schema schema, KnowledgeBase kb, GroundedLexicon groundedLexicon,
      CcgAutoLexicon normalCcgAutoLexicon, CcgAutoLexicon questionCcgAutoLexicon,
      RdfGraphTools rdfGraphTools, List<String> kbGraphUri, String testingFile, String devFile,
      String supervisedTrainingFile, String unsupervisedTrainingFile, String sematicParseKey,
      boolean debugEnabled, int trainingSampleSize, String logFile, int nBestTrainSyntacticParses,
      int nBestTestSyntacticParses, int nbestBestEdges, int nbestGraphs, boolean useSchema,
      boolean useKB, boolean groundFreeVariables, boolean useEmtpyTypes, boolean ignoreTypes,
      boolean urelGrelFlag, boolean urelPartGrelPartFlag, boolean utypeGtypeFlag,
      boolean gtypeGrelFlag, boolean wordGrelPartFlag, boolean wordGrelFlag,
      boolean wordBigramGrelPartFlag, boolean argGrelPartFlag, boolean argGrelFlag,
      boolean stemMatchingFlag, boolean mediatorStemGrelPartMatchingFlag,
      boolean argumentStemMatchingFlag, boolean argumentStemGrelPartMatchingFlag,
      boolean graphIsConnectedFlag, boolean graphHasEdgeFlag, boolean countNodesFlag,
      boolean edgeNodeCountFlag, boolean duplicateEdgesFlag, boolean grelGrelFlag,
      boolean useLexiconWeightsRel, boolean useLexiconWeightsType, double initialEdgeWeight,
      double initialTypeWeight, double initialWordWeight, double stemFeaturesWeight,
      boolean validQueryFlag) throws IOException {

    this.semanticParseKey = sematicParseKey;
    this.nBestTestSyntacticParses = nBestTestSyntacticParses;
    this.nBestTrainSyntacticParses = nBestTrainSyntacticParses;

    // Miscellaneous
    this.trainingSampleSize = trainingSampleSize;
    this.debugEnabled = debugEnabled;
    this.logger = Logger.getLogger(this.getClass());
    this.logFile = logFile;

    if (this.debugEnabled) {
      this.logger.setLevel(Level.DEBUG);
      if (logFile != null) {
        Appender appender = new FileAppender(layout, logFile, false);
        this.logger.addAppender(appender);

        // Appender stdoutAppender = new ConsoleAppender(layout);
        // this.logger.addAppender(stdoutAppender);
      } else {
        Appender stdoutAppender = new ConsoleAppender(layout);
        this.logger.addAppender(stdoutAppender);
      }
    }

    StructuredPercepton learningModel = new StructuredPercepton();

    graphToQuery = new GraphToQueryTraining(schema, kb, groundedLexicon, normalCcgAutoLexicon,
        questionCcgAutoLexicon, semanticParseKey, this.nBestTrainSyntacticParses,
        this.nBestTestSyntacticParses, nbestBestEdges, nbestGraphs, useSchema, useKB,
        groundFreeVariables, useEmtpyTypes, ignoreTypes, learningModel, urelGrelFlag,
        urelPartGrelPartFlag, utypeGtypeFlag, gtypeGrelFlag, grelGrelFlag, wordGrelPartFlag,
        wordGrelFlag, argGrelPartFlag, argGrelFlag, wordBigramGrelPartFlag, stemMatchingFlag,
        mediatorStemGrelPartMatchingFlag, argumentStemMatchingFlag,
        argumentStemGrelPartMatchingFlag, graphIsConnectedFlag, graphHasEdgeFlag, countNodesFlag,
        edgeNodeCountFlag, useLexiconWeightsRel, useLexiconWeightsType, duplicateEdgesFlag,
        validQueryFlag, initialEdgeWeight, initialTypeWeight, initialWordWeight, stemFeaturesWeight,
        rdfGraphTools, kbGraphUri);

    if (supervisedTrainingFile != null && !supervisedTrainingFile.equals("")) {
      supervisedTrainingExamples = loadExamples(new FileReader(supervisedTrainingFile));
    }

    if (testingFile != null && !testingFile.equals("")) {
      testingExamples = loadExamples(new FileReader(testingFile));
    }

    if (devFile != null && !devFile.equals("")) {
      devExamples = loadExamples(new FileReader(devFile));
    }

    // Loading training files from all domains
    trainingExamples = Lists.newArrayList();
    if (unsupervisedTrainingFile != null && !unsupervisedTrainingFile.equals("")) {
      List<String> unsupervisedTrainingFiles =
          Lists.newArrayList(Splitter.on(";").split(unsupervisedTrainingFile));
      for (String fileName : unsupervisedTrainingFiles) {
        List<String> trainingExamplesPart = Lists.newArrayList();
        if (fileName != null && fileName.endsWith(".gz")) {
          trainingExamplesPart = loadExamples(
              new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName)), "UTF-8"));
        } else if (fileName != null) {
          trainingExamplesPart = loadExamples(new FileReader(fileName));
        }
        trainingExamples.add(trainingExamplesPart);
      }
    }
  }

  public static List<String> selectRandomExamples(List<String> totalExamples, int sampleSize) {
    if (totalExamples == null) {
      return Lists.newArrayList();
    }
    if (totalExamples.size() < sampleSize) {
      sampleSize = totalExamples.size();
    }
    List<String> examples = Lists.newArrayList();
    Random random = new Random();
    int maxSize = totalExamples.size();
    for (int i = 0; i < sampleSize; i++) {
      int randomInt = random.nextInt(maxSize);
      examples.add(totalExamples.get(randomInt));
    }
    return examples;
  }

  public void train(int iterations, int nthreads) throws IOException, InterruptedException {
    // int nthreads = Runtime.getRuntime().availableProcessors();

    for (int i = 0; i < iterations; i++) {
      List<String> trainingSample = getTrainingSample(trainingSampleSize);
      graphToQuery.trainFromSentences(trainingSample, nthreads, logFile + ".train.iteration" + i,
          debugEnabled);

      Logger evalLogger = Logger.getLogger(GraphToQueryTraining.class + ".eval.iteration" + i);
      if (debugEnabled) {
        evalLogger.setLevel(Level.DEBUG);
        RollingFileAppender appender =
            new RollingFileAppender(layout, logFile + ".eval.iteration" + i);
        appender.setMaxFileSize("100MB");
        evalLogger.addAppender(appender);
      }

      evalLogger.info("######## Development Data ###########");
      graphToQuery.testCurrentModel(devExamples, evalLogger, logFile + ".eval.iteration" + i,
          debugEnabled, testingNbestParsesRange, nthreads);
      evalLogger.info("######## Testing Data ###########");
      graphToQuery.testCurrentModel(testingExamples, evalLogger, logFile + ".eval.iteration" + i,
          debugEnabled, testingNbestParsesRange, nthreads);
    }
  }

  public List<String> getTrainingSample(int trainingSampleSize) {
    List<String> trainingSample = Lists.newArrayList();
    for (List<String> trainingExamplesPart : trainingExamples) {
      List<String> trainingSamplePart =
          selectRandomExamples(trainingExamplesPart, trainingSampleSize);
      trainingSample.addAll(trainingSamplePart);
      Collections.shuffle(trainingSample);
    }
    // number of supervised examples should be at least double the number of
    // unsupervised examples
    // Speculative: Adding supervised examples at the end of unsupervised
    // training is
    // helpful
    if (supervisedTrainingExamples != null) {
      List<String> examplesCopy = Lists.newArrayList(supervisedTrainingExamples);
      int maxIterations =
          trainingSample.size() > 0 ? trainingSample.size() / supervisedTrainingExamples.size() : 0;
      for (int i = 0; i < 2 * (maxIterations + 1); i++) {
        Collections.shuffle(examplesCopy);
        trainingSample.addAll(examplesCopy);
      }
    }
    // Collections.shuffle(trainingSample);
    return trainingSample;
  }

  public List<String> loadExamples(Reader inputReader) throws IOException {
    BufferedReader br = new BufferedReader(inputReader);
    List<String> examples = Lists.newArrayList();
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (line.equals("") || line.charAt(0) == '#') {
          continue;
        }
        examples.add(line);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
    return examples;
  }

  public static void main_func(Schema schema, KnowledgeBase kb, GroundedLexicon groundedLexicon,
      RdfGraphTools rdfGraphTools, List<String> kbGraphUri) throws IOException,
      InterruptedException {
    CcgAutoLexicon normalCcgAutoLexicon = new CcgAutoLexicon("./data/candc_markedup.modified",
        "./data/unary_rules.txt", "./data/binary_rules.txt", "./data/lexicon_specialCases.txt");
    CcgAutoLexicon questionCcgAutoLexicon = new CcgAutoLexicon("./data/candc_markedup.modified",
        "./data/unary_rules.txt", "./data/binary_rules.txt",
        "./data/lexicon_specialCases_questions.txt");

    // String testFile =
    // "data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_parse.txt";

    String testFile =
        "data/tests/webquestions.examples.test.domains.easyccg.parse.filtered.100.json";

    String devFile = null;

    // String supervisedTrainingFile =
    // "data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_parse.txt";

    String supervisedTrainingFile =
        "data/tests/webquestions.examples.test.domains.easyccg.parse.filtered.471.json";

    // String corupusTrainingFile =
    // "data/freebase/sentences_training_filtered/business_training_sentences_filtered_00000.txt.gz";
    String corupusTrainingFile = null;

    String logFile = "working/sup_easyccg.log.txt";
    boolean debugEnabled = true;
    int trainingSampleSize = 1000;

    int nBestTrainSyntacticParses = 1;
    int nBestTestSyntacticParses = 1;
    int nbestBestEdges = 20;
    int nbestGraphs = 100;

    // Set these true, or else graph construction mechanism will be
    // completely driven by lexicon
    boolean useSchema = true;
    boolean useKB = true;
    boolean groundFreeVariables = false;
    boolean useEmtpyTypes = false;
    boolean ignoreTypes = false;

    // Alignment Features
    boolean urelGrelFlag = true;
    boolean urelPartGrelPartFlag = true;
    boolean utypeGtypeFlag = true;
    // gtypeGrel imposes strong biases - do not use for cai-yates
    boolean gtypeGrelFlag = true;

    // Contextual Features
    boolean wordGrelPartFlag = true;
    boolean wordGrelFlag = true;
    boolean wordBigramGrelPartFlag = true;
    boolean argGrelPartFlag = true;
    boolean argGrelFlag = true;

    // Stem features
    boolean stemMatchingFlag = true;
    boolean mediatorStemGrelPartMatchingFlag = true;
    boolean argumentStemMatchingFlag = true;
    boolean argumentStemGrelPartMatchingFlag = true;

    // Graph features
    boolean graphIsConnectedFlag = false;
    // edgeNodeCountFlag entails graphHasEdgeFlag
    boolean graphHasEdgeFlag = true;
    boolean countNodesFlag = false;
    // edgeNodeCountFlag and duplicateEdgesFlag are important on cai yates
    boolean edgeNodeCountFlag = false;
    boolean duplicateEdgesFlag = true;
    // surprisingly grelGrel not useful
    boolean grelGrelFlag = false;

    // Default weights
    boolean useLexiconWeightsRel = false;
    boolean useLexiconWeightsType = false;
    double initialEdgeWeight = 0.0;
    double initialTypeWeight = 0.0;
    double initialWordWeight = -1.0;
    double stemFeaturesWeight = 0.0;

    String semanticParseKey = "synPars";

    // Denotation feature
    boolean validQueryFlag = true;

    GraphToQueryTrainingMain graphToQueryModel = new GraphToQueryTrainingMain(schema, kb,
        groundedLexicon, normalCcgAutoLexicon, questionCcgAutoLexicon, rdfGraphTools, kbGraphUri,
        testFile, devFile, supervisedTrainingFile, corupusTrainingFile, semanticParseKey,
        debugEnabled, trainingSampleSize, logFile, nBestTrainSyntacticParses,
        nBestTestSyntacticParses, nbestBestEdges, nbestGraphs, useSchema, useKB,
        groundFreeVariables, useEmtpyTypes, ignoreTypes, urelGrelFlag, urelPartGrelPartFlag,
        utypeGtypeFlag, gtypeGrelFlag, wordGrelPartFlag, wordGrelFlag, wordBigramGrelPartFlag,
        argGrelPartFlag, argGrelFlag, stemMatchingFlag, mediatorStemGrelPartMatchingFlag,
        argumentStemMatchingFlag, argumentStemGrelPartMatchingFlag, graphIsConnectedFlag,
        graphHasEdgeFlag, countNodesFlag, edgeNodeCountFlag, duplicateEdgesFlag, grelGrelFlag,
        useLexiconWeightsRel, useLexiconWeightsType, initialEdgeWeight, initialTypeWeight,
        initialWordWeight, stemFeaturesWeight, validQueryFlag);

    int iterations = 10;
    int nthreads = 1;
    graphToQueryModel.train(iterations, nthreads);
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    // Schema schema = new
    // Schema("data/freebase/schema/business_schema.txt");
    Schema schema = new Schema("data/freebase/schema/business_film_people_schema.txt");

    // KnowledgeBase kb = new
    // KnowledgeBase("data/freebase/domain_facts/business_facts.txt.gz");
    KnowledgeBase kb = new KnowledgeBase(
        "data/freebase/domain_facts/business_film_people_facts.txt.gz",
        "data/freebase/stats/business_film_people_relation_types.txt");

    // GroundedLexicon groundedLexicon = new
    // GroundedLexicon("data/freebase/grounded_lexicon/business_grounded_lexicon.txt");
    GroundedLexicon groundedLexicon = new GroundedLexicon(
        "data/freebase/grounded_lexicon/business_film_people_grounded_lexicon.txt");

    RdfGraphTools rdfGraphTools = new RdfGraphTools("jdbc:virtuoso://bravas:1111",
        "http://bravas:8890/sparql", "dba", "dba", 2);
    List<String> kbGraphUri = Lists.newArrayList("http://business.freebase.com",
        "http://film.freebase.com", "http://people.freebase.com");

    // this helps separating loading database from debugging
    main_func(schema, kb, groundedLexicon, rdfGraphTools, kbGraphUri);
  }
}
