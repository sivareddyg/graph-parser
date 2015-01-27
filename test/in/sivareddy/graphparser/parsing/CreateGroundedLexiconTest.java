package in.sivareddy.graphparser.parsing;

import com.google.common.collect.Lists;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.CreateGroundedLexicon.CreateGroundedLexiconRunnable;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;

import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

public class CreateGroundedLexiconTest {

  @Test
  public void testCreateGroundedLexicon() throws IOException,
      InterruptedException {
    String[] lexicalFields = {"lemma"};
    // dynamicField has to be set - e.g. set it to freebase mid
    String[] childIdentifierFields = {"mid"};
    // String[] relationTypingFeilds = { "neType" };
    String[] relationTypingFeilds = {};

    KnowledgeBase kb =
        new KnowledgeBase("data/freebase/domain_facts/business_facts.txt.gz",
            "data/freebase/stats/business_relation_types.txt");
    // KnowledgeBase kb = null;
    CcgAutoLexicon ccgAutoLexicon =
        new CcgAutoLexicon("./data/candc_markedup.modified",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases.txt");
    boolean ignorePronouns = true;
    CreateGroundedLexicon creator =
        new CreateGroundedLexicon(kb, ccgAutoLexicon, lexicalFields,
            childIdentifierFields, relationTypingFeilds, ignorePronouns);

    BufferedReader br =
        new BufferedReader(new FileReader(
            "data/tests/deplambda.graphparser.txt"));
    // BufferedReader br = new BufferedReader(new FileReader("working/1.txt"));

    long startTime = System.currentTimeMillis();

    int nThreads = Runtime.getRuntime().availableProcessors() / 4;
    // if (nThreads > 10)
    // nThreads = 10;

    final BlockingQueue<Runnable> queue =
        new ArrayBlockingQueue<>(nThreads * 2);
    ThreadPoolExecutor threadPool =
        new ThreadPoolExecutor(nThreads, nThreads, 30 * 60, TimeUnit.SECONDS,
            queue);
    threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        try {
          executor.getQueue().put(r);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });

    try {
      String line = br.readLine();
      List<String> lines = Lists.newArrayList();
      int count = 0;
      while (line != null) {
        count++;
        lines.add(line);
        if (count % 1000 == 0) {
          Runnable worker =
              new CreateGroundedLexiconRunnable(lines, creator,
                  "dependency_lambda", false);
          threadPool.execute(worker);
          lines = Lists.newArrayList();
        }
        line = br.readLine();
      }
      Runnable worker =
          new CreateGroundedLexiconRunnable(lines, creator,
              "dependency_lambda", false);
      threadPool.execute(worker);
      // finish all existing threads in the queue
      threadPool.shutdown();
      // Wait until all threads are finish
      while (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
        System.err.println("Awaiting completion of threads.");
      }
      System.err.println("Finished all threads");
    } finally {
      br.close();
    }


    BufferedWriter bw =
        new BufferedWriter(new FileWriter("working/lexicon.txt"));
    creator.printLexicon(bw);
    bw.close();

    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    System.err.println(totalTime);

    /*- System.out.println(creator.predicateToGroundedRelationMap);
    System.out.println();
    System.out.println(creator.langTypeToGroundedTypeMap);*/

  }

}
