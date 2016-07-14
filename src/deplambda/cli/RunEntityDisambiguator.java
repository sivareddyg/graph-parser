package deplambda.cli;

import in.sivareddy.graphparser.cli.AbstractCli;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.ml.learning.StructuredPercepton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.gson.JsonObject;

import deplambda.entityannotator.EntityDisambiguator;
import deplambda.entityannotator.EntityScorer;
import deplambda.others.SentenceUtils;

public class RunEntityDisambiguator extends AbstractCli {
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

  private OptionSpec<String> loadModelFromFile;
  private OptionSpec<String> inputFile;

  // Sparql End point and details
  private OptionSpec<String> endpoint;

  // Schema File
  private OptionSpec<String> schema;

  private OptionSpec<Integer> nbestEntities;

  private OptionSpec<Boolean> entitiesHasRelation;

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
            .withRequiredArg().ofType(String.class).required();

    nthreads =
        parser.accepts("nthreads", "number of threads").withRequiredArg()
            .ofType(Integer.class).defaultsTo(20);

    inputFile =
        parser
            .accepts("inputFile",
                "input file containing entity spans and ranked entities")
            .withRequiredArg().ofType(String.class).defaultsTo("stdin");

    endpoint =
        parser.accepts("endpoint", "Freebase SPARQL endpoint")
            .withRequiredArg().ofType(String.class).required();

    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();

    nbestEntities =
        parser
            .accepts("nbestEntities",
                "the number of final disambiguation possibilities")
            .withRequiredArg().ofType(Integer.class).defaultsTo(10);

    entitiesHasRelation =
        parser
            .accepts("entitiesHasRelation",
                "lattice path should comprise only of entities that are in relation")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
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

    String inputFileVal = options.valueOf(inputFile);

    int nthreadsVal = options.valueOf(nthreads);

    List<JsonObject> inputSentences = new ArrayList<>();
    try {
      SentenceUtils.loadSentences(SentenceUtils.getInputStream(inputFileVal),
          inputSentences);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      System.err.println(String.format("First best results: %.2f",
          scorer.evaluate(inputSentences, nthreadsVal)));
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

    KnowledgeBase kb = null;
    try {
      KnowledgeBaseOnline.TYPE_KEY = "fb:type.object.type";
      kb =
          new KnowledgeBaseOnline(options.valueOf(endpoint), String.format(
              "http://%s:8890/sparql", options.valueOf(endpoint)), "dba",
              "dba", 50000, new Schema(options.valueOf(schema)));
    } catch (IOException e) {
      e.printStackTrace();
    }

    EntityDisambiguator disambiguator =
        new EntityDisambiguator(scorer, kb, options.valueOf(nbestEntities),
            options.valueOf(entitiesHasRelation));

    try {
      disambiguator.processList(inputSentences, System.out,
          options.valueOf(nthreads), true);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new RunEntityDisambiguator().run(args);
  }
}
