package in.sivareddy.scripts.clueweb;

import in.sivareddy.others.StanfordPipeline;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 * [
 * " Among the most famous ABC are tennis player Michael Chen and Yahoo co-founder Jerry Yang."
 * , ["/m/0gsg7", 0.840163, 23, 26], ["/m/011zfk", 0.997802, 79, 89],
 * ["/m/019rl6", 0.976942, 62, 67]]
 */

public class RunTokenizerOnEntityTaggedClueweb {
  private static Map<String, String> options = ImmutableMap.of("annotators",
      "tokenize, ssplit", "ssplit.eolonly", "true");


  private static StanfordPipeline pipeline = new StanfordPipeline(options);
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();

  String ENTITY_START = "ENTITY_START";
  String ENTITY_END = "ENTITY_END";

  public JsonObject processLine(String line) {
    try {
      JsonArray sentence = jsonParser.parse(line).getAsJsonArray();
      String originalSent = sentence.get(0).getAsString();

      List<JsonArray> entitiesCopy = new ArrayList<>();
      sentence.get(1).getAsJsonArray()
          .forEach(x -> entitiesCopy.add(x.getAsJsonArray()));
      entitiesCopy.sort(Comparator.comparing(x -> x.get(2).getAsInt()));
      List<JsonArray> entities = new ArrayList<>();

      int previousEnd = 0;
      for (JsonArray entity : entitiesCopy) {
        int entityStart = entity.get(2).getAsInt();
        int entityEnd = entity.get(3).getAsInt();
        if (entityStart >= previousEnd && entityStart < originalSent.length()
            && entityEnd <= originalSent.length()) {
          entities.add(entity);
          previousEnd = entityEnd;
        }
      }

      int prevPosition = 0;
      StringBuilder sb = new StringBuilder();
      for (JsonArray entity : entities) {
        int start = entity.get(2).getAsInt();
        sb.append(originalSent.subSequence(prevPosition, start));
        sb.append(" ");
        sb.append(ENTITY_START);
        sb.append(" ");
        int end = entity.get(3).getAsInt();
        sb.append(originalSent.subSequence(start, end));
        sb.append(" ");
        sb.append(ENTITY_END);
        sb.append(" ");
        prevPosition = end;
      }
      sb.append(originalSent.substring(prevPosition, originalSent.length()));
      JsonObject sentenceObj = new JsonObject();
      sentenceObj.addProperty(SentenceKeys.SENTENCE_KEY,
          CharMatcher.WHITESPACE.replaceFrom(sb, ' '));
      pipeline.processSentence(sentenceObj);

      JsonObject returnSentence = new JsonObject();
      JsonArray returnEntities = new JsonArray();
      JsonArray returnWords = new JsonArray();

      JsonArray tempWords =
          sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
      int returnWordIndex = 0;
      int entityCount = 0;
      for (int j = 0; j < tempWords.size(); j++) {
        JsonObject word = tempWords.get(j).getAsJsonObject();
        if (word.get(SentenceKeys.WORD_KEY).getAsString().equals(ENTITY_START)) {
          j++;
          word = tempWords.get(j).getAsJsonObject();
          int entityStart = returnWordIndex;
          while (j < tempWords.size()
              && !word.get(SentenceKeys.WORD_KEY).getAsString()
                  .equals(ENTITY_END)) {
            returnWords.add(word);
            returnWordIndex++;
            j++;
            word = tempWords.get(j).getAsJsonObject();
          }
          int entityEnd = returnWordIndex - 1;
          if (entityEnd >= entityStart) {
            JsonObject returnEntity = new JsonObject();
            returnEntity.addProperty(
                SentenceKeys.ENTITY,
                entities.get(entityCount).get(0).getAsString()
                    .replaceFirst("/", "").replaceFirst("/", "."));
            returnEntity.add(SentenceKeys.SCORE,
                entities.get(entityCount).get(1));
            returnEntity.addProperty(SentenceKeys.START, entityStart);
            returnEntity.addProperty(SentenceKeys.END, entityEnd);
            returnEntities.add(returnEntity);
          }
          entityCount++;
        } else {
          returnWords.add(word);
          returnWordIndex++;
        }
      }
      returnSentence.add(SentenceKeys.WORDS_KEY, returnWords);
      returnSentence.add(SentenceKeys.ENTITIES, returnEntities);
      return returnSentence;
    } catch (Exception e) {
      System.err.println("Could not tokenize: " + line);
      e.printStackTrace();
    }
    return new JsonObject();
  }

  public void processStream(InputStream stream, PrintStream out, int nthreads)
      throws IOException, InterruptedException {
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

    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    try {
      String line = br.readLine();
      while (line != null) {
        Runnable worker = new PipelineRunnable(this, line, out);
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

  public static class PipelineRunnable implements Runnable {
    String line;
    RunTokenizerOnEntityTaggedClueweb engine;
    PrintStream out;
    boolean printOutput;

    public PipelineRunnable(RunTokenizerOnEntityTaggedClueweb engine,
        String line, PrintStream out) {
      this.engine = engine;
      this.line = line;
      this.out = out;
    }

    @Override
    public void run() {
      JsonObject sentence = engine.processLine(line);
      engine.print(gson.toJson(sentence), out);
    }
  }

  public synchronized void print(String string, PrintStream out) {
    out.println(string);
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    RunTokenizerOnEntityTaggedClueweb engine =
        new RunTokenizerOnEntityTaggedClueweb();
    engine.processStream(System.in, System.out, 30);
  }
}
