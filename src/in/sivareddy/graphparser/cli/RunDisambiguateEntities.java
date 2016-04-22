package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.util.DisambiguateEntities;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RunDisambiguateEntities extends AbstractCli {

  // Schema File
  private OptionSpec<String> schema;

  // Sparql End point and details
  private OptionSpec<String> endpoint;

  // Freebase relation to identity the type of an entity.
  private OptionSpec<String> typeKey;

  // Input file.
  private OptionSpec<String> inputFile;

  // output file.
  private OptionSpec<String> outputFile;

  // nbest options.
  private OptionSpec<Integer> nthreads;
  private OptionSpec<Integer> initialNbest;
  private OptionSpec<Integer> intermediateNbest;
  private OptionSpec<Integer> finalNbest;

  private OptionSpec<Boolean> entityHasReadableId;
  private OptionSpec<Boolean> shouldStartWithNamedEntity;
  private OptionSpec<Boolean> containsNamedEntity;
  private OptionSpec<Boolean> noPrecedingNamedEntity;
  private OptionSpec<Boolean> noSucceedingNamedEntity;

  private OptionSpec<Boolean> containsProperNoun;
  private OptionSpec<Boolean> noPrecedingProperNoun;
  private OptionSpec<Boolean> noSucceedingProperNoun;

  private OptionSpec<Boolean> ignoreEntitiesWithVerbs;
  private OptionSpec<Boolean> ignoreEntitiesWithQuestionWords;
  
  @Override
  public void initializeOptions(OptionParser parser) {
    endpoint =
        parser.accepts("endpoint", "SPARQL endpoint").withRequiredArg()
            .ofType(String.class).required();

    typeKey =
        parser
            .accepts(
                "typeKey",
                "Freebase relation name to identify the type of an entity. e.g. rdf:type or fb:type.object.type")
            .withRequiredArg().ofType(String.class).required();

    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();


    inputFile =
        parser
            .accepts(
                "inputFile",
                "Input file contaning sentences to be entity tagged which already have entity spans and rankedEntities tagged")
            .withRequiredArg().ofType(String.class).defaultsTo("stdin");

    nthreads =
        parser
            .accepts("nthreads",
                "number of threads. As the threads increase, load on sparql server increases")
            .withRequiredArg().ofType(Integer.class).defaultsTo(10);

    initialNbest =
        parser
            .accepts("initialNbest",
                "number of entities to be considered for each span initially")
            .withRequiredArg().ofType(Integer.class).defaultsTo(5);

    intermediateNbest =
        parser
            .accepts("intermediateNbest",
                "number of entities to be considered in each intermediate step for each span")
            .withRequiredArg().ofType(Integer.class).defaultsTo(10);

    finalNbest =
        parser
            .accepts("finalNbest",
                "number of entities to be tagged finally for each span")
            .withRequiredArg().ofType(Integer.class).defaultsTo(10);

    outputFile =
        parser
            .accepts("outputFile",
                "Output file where entity tagged should be written")
            .withRequiredArg().ofType(String.class).required();

    entityHasReadableId =
        parser
            .accepts(
                "entityHasReadableId",
                "consider an entity as valid only if it has a readble Freebase id. Use false if you use Knowledge Graph.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    shouldStartWithNamedEntity =
        parser
            .accepts(
                "shouldStartWithNamedEntity",
                "entity span should start with named entity or a proper noun. Use false for universal dependencies.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    containsNamedEntity =
        parser
            .accepts("containsNamedEntity",
                "entity span should contain at least one named entity.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    noPrecedingNamedEntity =
        parser
            .accepts(
                "noPrecedingNamedEntity",
                "entity span should not be preceeded by a named entity of the same type as current span.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    noSucceedingNamedEntity =
        parser
            .accepts(
                "noSucceedingNamedEntity",
                "entity span should not be succeeded by a named entity of the same type as current span.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);

    containsProperNoun =
        parser
            .accepts("containsProperNoun",
                "entity span should contain at least one proper noun")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    noPrecedingProperNoun =
        parser
            .accepts("noPrecedingProperNoun",
                "entity span should not be preceeded by a proper noun")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    noSucceedingProperNoun =
        parser
            .accepts("noSucceedingProperNoun",
                "entity span should not be succeeded by a proper noun")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);

    ignoreEntitiesWithVerbs =
        parser
            .accepts("ignoreEntitiesWithVerbs",
                "ignore entities that have a verb")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
    
    ignoreEntitiesWithQuestionWords =
        parser
            .accepts("ignoreEntitiesWithQuestionWords",
                "ignore entities that a question word in it")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
  }

  @Override
  public void run(OptionSet options) {
    Schema schemaObj;
    try {
      schemaObj = new Schema(options.valueOf(schema));
      KnowledgeBaseOnline.TYPE_KEY = options.valueOf(typeKey);
      KnowledgeBase kb =
          new KnowledgeBaseOnline(options.valueOf(endpoint), String.format(
              "http://%s:8890/sparql", options.valueOf(endpoint)), "dba",
              "dba", 50000, schemaObj);
      RunDisambiguateEntitiesMain runner =
          new RunDisambiguateEntitiesMain(kb, options.valueOf(nthreads),
              options.valueOf(initialNbest),
              options.valueOf(intermediateNbest), options.valueOf(finalNbest),
              options.valueOf(entityHasReadableId),
              options.valueOf(shouldStartWithNamedEntity),
              options.valueOf(containsNamedEntity),
              options.valueOf(noPrecedingNamedEntity),
              options.valueOf(noSucceedingNamedEntity),
              options.valueOf(containsProperNoun),
              options.valueOf(noPrecedingProperNoun),
              options.valueOf(noSucceedingProperNoun),
              options.valueOf(ignoreEntitiesWithVerbs),
              options.valueOf(ignoreEntitiesWithQuestionWords));

      String inputFileString = options.valueOf(inputFile);
      InputStream in = null;
      if (inputFileString.equals("stdin")) {
        in = System.in;
      } else {
        in = new FileInputStream(inputFileString);
      }

      String outFile = options.valueOf(outputFile);
      PrintStream out = null;

      if (outFile.equals("stdout")) {
        out = System.out;
      } else {
        out = new PrintStream(new FileOutputStream(outFile));
      }

      runner.disambiguate(in, out);
      in.close();
      out.close();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static class RunDisambiguateEntitiesMain {
    private JsonParser jsonParser = new JsonParser();
    private Gson gson = new Gson();
    int nthreads;
    int initalNbest;
    int intermediateNbest;
    int finalNbest;
    KnowledgeBase kb;

    boolean entityHasReadableId;
    boolean shouldStartWithNamedEntity;
    boolean containsNamedEntity;
    boolean noPrecedingNamedEntity;
    boolean noSucceedingNamedEntity;
    final boolean containsProperNoun;
    final boolean noPrecedingProperNoun;
    final boolean noSucceedingProperNoun;
    final boolean ignoreEntitiesWithVerbs;
    final boolean ignoreEntitiesWithQuestionWords;

    public RunDisambiguateEntitiesMain(KnowledgeBase kb, int nthreads,
        int initalNbest, int intermediateNbest, int finalNbest,
        boolean entityHasReadableId, boolean shouldStartWithNamedEntity,
        boolean containsNamedEntity, boolean noPrecedingNamedEntity,
        boolean noSucceedingNamedEntity, boolean containsProperNoun,
        boolean noPrecedingProperNoun, boolean noSucceedingProperNoun,
        boolean ignoreEntitiesWithVerbs, boolean ignoreEntitiesWithQuestionWords) {
      this.kb = kb;
      this.nthreads = nthreads;
      this.initalNbest = initalNbest;
      this.intermediateNbest = intermediateNbest;
      this.finalNbest = finalNbest;

      this.entityHasReadableId = entityHasReadableId;
      this.shouldStartWithNamedEntity = shouldStartWithNamedEntity;
      this.containsNamedEntity = containsNamedEntity;
      this.noPrecedingNamedEntity = noPrecedingNamedEntity;
      this.noSucceedingNamedEntity = noSucceedingNamedEntity;

      this.containsProperNoun = containsProperNoun;
      this.noPrecedingProperNoun = noPrecedingProperNoun;
      this.noSucceedingProperNoun = noSucceedingProperNoun;

      this.ignoreEntitiesWithVerbs = ignoreEntitiesWithVerbs;
      this.ignoreEntitiesWithQuestionWords = ignoreEntitiesWithQuestionWords;
    }

    public void disambiguate(InputStream stream, PrintStream out)
        throws IOException, InterruptedException {
      final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(nthreads);
      ThreadPoolExecutor threadPool =
          new ThreadPoolExecutor(nthreads, nthreads, 600, TimeUnit.SECONDS,
              queue);

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
          if (line.startsWith("#") || line.trim().equals("")) {
            line = br.readLine();
            continue;
          }

          JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
          if (!jsonSentence.has(SentenceKeys.INDEX_KEY)) {
            jsonSentence.addProperty(SentenceKeys.INDEX_KEY, sentCount);
            sentCount += 1;
          }
          Runnable worker =
              new DisambiguateSentenceRunnable(this, jsonSentence, kb,
                  initalNbest, intermediateNbest, finalNbest,
                  entityHasReadableId, shouldStartWithNamedEntity,
                  containsNamedEntity, noPrecedingNamedEntity,
                  noSucceedingNamedEntity, containsProperNoun,
                  noPrecedingProperNoun, noSucceedingProperNoun,
                  ignoreEntitiesWithVerbs, ignoreEntitiesWithQuestionWords, out);
          threadPool.execute(worker);
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

    public synchronized void printSentence(JsonObject sentence, PrintStream out) {
      out.println(gson.toJson(sentence));
    }

  }

  public static class DisambiguateSentenceRunnable implements Runnable {
    private JsonObject jsonSentence;
    int initialNbest;
    int intermediateNbest;
    int finalNbest;
    KnowledgeBase kb;
    RunDisambiguateEntitiesMain engine;
    PrintStream out;
    boolean entityHasReadableId;
    boolean shouldStartWithNamedEntity;
    boolean containsNamedEntity;
    boolean noPrecedingNamedEntity;
    boolean noSucceedingNamedEntity;
    final boolean containsProperNoun;
    final boolean noPrecedingProperNoun;
    final boolean noSucceedingProperNoun;
    final boolean ignoreEntitiesWithVerbs;
    final boolean ignoreEntitiesWithQuestionWords;

    public DisambiguateSentenceRunnable(RunDisambiguateEntitiesMain engine,
        JsonObject jsonSentence, KnowledgeBase kb, int initialNbest,
        int intermediateNbest, int finalNbest, boolean entityHasReadableId,
        boolean shouldStartWithNamedEntity, boolean containsNamedEntity,
        boolean noPrecedingNamedEntity, boolean noSucceedingNamedEntity,
        boolean containsProperNoun, boolean noPrecedingProperNoun,
        boolean noSucceedingProperNoun, boolean ignoreEntitiesWithVerbs,
        boolean ignoreEntitiesWithQuestionWords, PrintStream out) {
      this.engine = engine;
      this.jsonSentence = jsonSentence;
      this.initialNbest = initialNbest;
      this.intermediateNbest = intermediateNbest;
      this.finalNbest = finalNbest;
      this.kb = kb;
      this.out = out;

      this.entityHasReadableId = entityHasReadableId;
      this.shouldStartWithNamedEntity = shouldStartWithNamedEntity;
      this.containsNamedEntity = containsNamedEntity;
      this.noPrecedingNamedEntity = noPrecedingNamedEntity;
      this.noSucceedingNamedEntity = noSucceedingNamedEntity;

      this.containsProperNoun = containsProperNoun;
      this.noPrecedingProperNoun = noPrecedingProperNoun;
      this.noSucceedingProperNoun = noSucceedingProperNoun;

      this.ignoreEntitiesWithVerbs = ignoreEntitiesWithVerbs;
      this.ignoreEntitiesWithQuestionWords = ignoreEntitiesWithQuestionWords;
    }

    @Override
    public void run() {
      DisambiguateEntities.latticeBasedDisambiguation(jsonSentence,
          initialNbest, intermediateNbest, finalNbest, entityHasReadableId, kb,
          shouldStartWithNamedEntity, containsNamedEntity,
          noPrecedingNamedEntity, noSucceedingNamedEntity, containsProperNoun,
          noPrecedingProperNoun, noSucceedingProperNoun,
          ignoreEntitiesWithVerbs, ignoreEntitiesWithQuestionWords);
      engine.printSentence(jsonSentence, out);
    }
  }

  public static void main(String[] args) throws IOException {
    new RunDisambiguateEntities().run(args);
  }
}
