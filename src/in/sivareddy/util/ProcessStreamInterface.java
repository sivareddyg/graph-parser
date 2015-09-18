package in.sivareddy.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class ProcessStreamInterface {
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();

  public abstract void processSentence(JsonObject sentence);

  public void processStream(InputStream stream, PrintStream out, int nthreads,
      boolean printOutput) throws IOException, InterruptedException {
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
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        Runnable worker =
            new PipelineRunnable(this, jsonSentence, out, printOutput);
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
    private final JsonObject sentence;
    private final ProcessStreamInterface engine;
    private final PrintStream out;
    private final boolean printOutput;

    public PipelineRunnable(ProcessStreamInterface engine, JsonObject sentence,
        PrintStream out, boolean printOutput) {
      this.engine = engine;
      this.sentence = sentence;
      this.out = out;
      this.printOutput = printOutput;
    }

    @Override
    public void run() {
      engine.processSentence(sentence);
      if (printOutput) {
        engine.print(gson.toJson(sentence), out);
      }
    }
  }

  public synchronized void print(String string, PrintStream out) {
    out.println(string);
  }

}
