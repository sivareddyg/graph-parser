package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.cli.AbstractCli;
import in.sivareddy.ml.learning.StructuredPercepton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.gson.JsonObject;

import in.sivareddy.graphparser.util.entityannotator.EntityScorer;
import in.sivareddy.util.SentenceUtils;

public class RunTrainEntityScorer extends AbstractCli {

  private OptionSpec<Boolean> useStartIndex;
  private OptionSpec<Boolean> useEntityLength;
  private OptionSpec<Boolean> useNameOverlap;
  private OptionSpec<Boolean> useAPIScore;
  private OptionSpec<Boolean> hasId;
  private OptionSpec<Boolean> usePos;
  private OptionSpec<Boolean> usePosBigram;
  private OptionSpec<Boolean> useNextWordPos;
  private OptionSpec<Boolean> usePrevWordPos;

  private OptionSpec<Boolean> useWord;
  private OptionSpec<Boolean> useWordBigram;
  private OptionSpec<Boolean> useNextWord;
  private OptionSpec<Boolean> usePrevWord;

  private OptionSpec<Boolean> useWordEntity;
  private OptionSpec<Boolean> useWordBigramEntity;


  private OptionSpec<Integer> nthreads;
  private OptionSpec<Integer> iterations;

  private OptionSpec<String> loadModelFromFile;
  private OptionSpec<String> trainFile;
  private OptionSpec<String> devFile;
  private OptionSpec<String> testFile;
  private OptionSpec<String> saveToFile;

  @Override
  public void initializeOptions(OptionParser parser) {
    useStartIndex =
        parser.accepts("useStartIndex", "use start index as feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useEntityLength =
        parser
            .accepts("useEntityLength",
                "use entity length in the input sentence as feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    useNameOverlap =
        parser
            .accepts(
                "useNameOverlap",
                "use overlap between the entity's name and phrase overlap in the input sentence as feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useAPIScore =
        parser.accepts("useAPIScore", "use api score as feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    hasId =
        parser
            .accepts(
                "hasId",
                "if the entity has a readble id, it is likely a good entity. This information is absent in KG API.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    usePos =
        parser
            .accepts("usePos", "use unigram features from the current phrase")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    usePosBigram =
        parser
            .accepts("usePosBigram",
                "use bigram features from the current phrase")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useWord =
        parser.accepts("useWord", "use words in the matched phrase")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useWordBigram =
        parser
            .accepts("useWordBigram", "use word bigrams in the matched phrase")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useNextWord =
        parser.accepts("useNextWord", "use word next to the entity")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    usePrevWord =
        parser.accepts("usePrevWord", "use word before the entity")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useWordEntity =
        parser.accepts("useWordEntity", "use word and entity pairs")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useWordBigramEntity =
        parser
            .accepts("useWordBigramEntity",
                "use word bigrams from the current phrase and the entity feature")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    useNextWordPos =
        parser.accepts("useNextWordPos", "use next word's pos tag")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    usePrevWordPos =
        parser.accepts("usePrevWordPos", "use previous word's pos tag")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    loadModelFromFile =
        parser.accepts("loadModelFromFile", "load initial model from file")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    nthreads =
        parser.accepts("nthreads", "number of threads").withRequiredArg()
            .ofType(Integer.class).required();

    iterations =
        parser.accepts("iterations", "number of training iterations")
            .withRequiredArg().ofType(Integer.class).defaultsTo(0);

    trainFile =
        parser
            .accepts("trainFile",
                "training file containing matched and ranked entities, along with goldMids")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    devFile =
        parser
            .accepts("devFile",
                "dev file containing matched and ranked entities, along with goldMids")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    testFile =
        parser
            .accepts("testFile",
                "test file containing matched and ranked entities, along with goldMids")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    saveToFile =
        parser.accepts("saveToFile", "destination to save the final model")
            .withRequiredArg().ofType(String.class).defaultsTo("");
  }

  @Override
  public void run(OptionSet options) {
    StructuredPercepton ranker = null;
    String modelFile = options.valueOf(loadModelFromFile);
    if (!modelFile.equals("")) {
      try {
        ranker = StructuredPercepton.loadModel(modelFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      ranker = new StructuredPercepton();
    }

    EntityScorer scorer =
        new EntityScorer(ranker, options.valueOf(useStartIndex),
            options.valueOf(useEntityLength), options.valueOf(useNameOverlap),
            options.valueOf(useAPIScore), options.valueOf(hasId),
            options.valueOf(usePos), options.valueOf(usePosBigram),
            options.valueOf(useNextWordPos), options.valueOf(usePrevWordPos),
            options.valueOf(useWord), options.valueOf(useWordBigram),
            options.valueOf(useNextWord), options.valueOf(usePrevWord),
            options.valueOf(useWordEntity),
            options.valueOf(useWordBigramEntity));

    String trainFileVal = options.valueOf(trainFile);
    String devFileVal = options.valueOf(devFile);
    String testFileVal = options.valueOf(testFile);

    int iterationsVal = options.valueOf(iterations);
    int nthreadsVal = options.valueOf(nthreads);

    List<JsonObject> trainingSentences = new ArrayList<>();
    if (!trainFileVal.equals("") && iterationsVal > 0) {
      try {
        System.out.println("Reading training sentences from " + trainFileVal);
        SentenceUtils.loadSentences(SentenceUtils.getInputStream(trainFileVal),
            trainingSentences);
        System.out.println("Done reading! " + trainFileVal);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    List<JsonObject> devSentences = new ArrayList<>();
    if (!devFileVal.equals("")) {
      try {
        SentenceUtils.loadSentences(SentenceUtils.getInputStream(devFileVal),
            devSentences);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    System.out.println(String.format("Before Training"));
    try {
      scorer.evaluate(devSentences, nthreadsVal);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    StructuredPercepton bestModelSoFar = ranker.serialClone();
    double bestScoreSoFar = 0.00;
    for (int i = 0; i < iterationsVal; i++) {
      Collections.shuffle(trainingSentences);
      try {
        System.out.println(String.format("Starting iteration %d:", i));
        System.out.println(String.format("Training Size %d:",
            trainingSentences.size()));
        scorer.processList(trainingSentences, null, nthreadsVal, false);

        System.out.println(String.format("After iteration %d:", i));

        double currentScore = scorer.evaluate(devSentences, nthreadsVal);

        System.out.println(String.format("Current: %.2f, BestSoFar: %.2f",
            currentScore, bestScoreSoFar));
        if (currentScore > bestScoreSoFar) {
          bestScoreSoFar = currentScore;
          bestModelSoFar = scorer.getRanker().serialClone();
        } else {
          scorer.setRanker(bestModelSoFar.serialClone());
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }

    List<JsonObject> testSentences = new ArrayList<>();
    if (!testFileVal.equals("")) {
      try {
        SentenceUtils.loadSentences(SentenceUtils.getInputStream(testFileVal),
            testSentences);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    System.out.println(String.format("Entity Annotation on Test File: "));
    try {
      double testScore = scorer.evaluate(testSentences, nthreadsVal);
      System.out.println(String.format("Test score: %.2f", testScore));
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    String saveToFileVal = options.valueOf(saveToFile);
    if (!saveToFileVal.equals("")) {
      try {
        scorer.saveModel(saveToFileVal);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    new RunTrainEntityScorer().run(args);
  }
}
