package in.sivareddy.scripts;

import in.sivareddy.graphparser.util.MergeEntity;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import others.StanfordPipeline;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RunPosTaggerAndNerPipeline {
  private static Map<String, String> options =
      ImmutableMap
          .of("annotators",
              "tokenize, ssplit, pos, lemma, ner",
              "pos.model",
              "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger",
              "ner.model",
              "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
  private static StanfordPipeline englishPipeline = new StanfordPipeline(
      options);
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();

  int nthreads = 1;

  public RunPosTaggerAndNerPipeline(int nthreads)
      throws ArgumentValidationException, IOException {
    this.nthreads = nthreads;
  }
  
  public List<JsonObject> processText(JsonObject sentence)
      throws ArgumentValidationException, IOException, InterruptedException {
    sentence = processSentence(gson.toJson(sentence));
    List<JsonObject> sentences = splitSentences(sentence);
    return sentences;
  }

  public JsonObject processSentence(String line) {
    JsonObject sentence = jsonParser.parse(line).getAsJsonObject();

    String sentenceString =
        sentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
    sentenceString = sentenceString.trim();
    String cleanSentenceString =
        Joiner
            .on(" ")
            .join(
                Splitter
                    .on(CharMatcher.WHITESPACE)
                    .trimResults(
                        CharMatcher.JAVA_LETTER_OR_DIGIT.or(
                            CharMatcher.anyOf(",?.")).negate())
                    .omitEmptyStrings().split(sentenceString)).toString();

    sentence.addProperty(SentenceKeys.SENTENCE_KEY, cleanSentenceString);

    sentence = englishPipeline.processSentence(gson.toJson(sentence));
    sentence =
        MergeEntity.mergeNamedEntitiesToSingleWord(gson.toJson(sentence));
    return sentence;
  }

  public void processStream(InputStream stream, PrintStream out)
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
        JsonObject jsonSentence = new JsonObject();
        jsonSentence.addProperty(SentenceKeys.SENTENCE_KEY, line);
        Runnable worker = new PipelineRunnable(this, jsonSentence, out);
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
    JsonObject sentence;
    RunPosTaggerAndNerPipeline engine;
    PrintStream out;

    public PipelineRunnable(RunPosTaggerAndNerPipeline engine,
        JsonObject sentence, PrintStream out) {
      this.engine = engine;
      this.sentence = sentence;
      this.out = out;
    }

    @Override
    public void run() {
      try {
        for (JsonObject sentenceSplit : engine.processText(sentence)) {
          engine.print(gson.toJson(sentenceSplit), out);
        }
      } catch (ArgumentValidationException | IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public synchronized void print(String string, PrintStream out) {
    out.println(string);
  }

  public static List<JsonObject> splitSentences(JsonObject jsonSentence) {
    List<JsonObject> sentences = new ArrayList<>();
    JsonArray newSentenceWords = new JsonArray();
    for (JsonElement wordElm : jsonSentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      JsonObject word = wordElm.getAsJsonObject();
      newSentenceWords.add(word);
      if (word.has(SentenceKeys.SENT_END)
          && word.get(SentenceKeys.SENT_END).getAsBoolean()) {
        if (newSentenceWords.size() != 0) {
          JsonObject newSentence = new JsonObject();
          newSentence.add(SentenceKeys.WORDS_KEY, newSentenceWords);
          sentences.add(newSentence);
        }
        newSentenceWords = new JsonArray();
      }
    }

    if (newSentenceWords.size() != 0) {
      JsonObject newSentence = new JsonObject();
      newSentence.add(SentenceKeys.WORDS_KEY, newSentenceWords);
      sentences.add(newSentence);
    }
    return sentences;
  }

  public static void main(String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(RunPosTaggerAndNerPipeline.class);
    logger.setLevel(Level.DEBUG);
    logger.setAdditivity(false);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    RunPosTaggerAndNerPipeline engine = new RunPosTaggerAndNerPipeline(20);
    engine.processStream(System.in, System.out);
  }
}
