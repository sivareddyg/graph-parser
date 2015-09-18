package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.ccg.SemanticCategoryType;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateGroundedLexicon {
  private HashMap<Relation, HashMap<Relation, Double>> predicateToGroundedRelationMap =
      new HashMap<>();
  private HashMap<Relation, Double> predicateCounts = new HashMap<>();
  private HashMap<String, HashMap<String, Double>> langTypeToGroundedTypeMap =
      new HashMap<>();
  private HashMap<String, Double> typeCounts = new HashMap<>();

  private final GroundedGraphs graphCreator;
  private boolean ignoreTypes = false;
  private String semanticParseKey;
  private int nBestCcgParses = 1;
  private KnowledgeBase kb;
  private static JsonParser jsonParser = new JsonParser();

  public CreateGroundedLexicon(GroundedGraphs graphCreator, KnowledgeBase kb,
      String semanticParseKey, boolean ignoreTypes, int nbestCcgParses) {
    this.graphCreator = graphCreator;
    this.ignoreTypes = ignoreTypes;
    this.semanticParseKey = semanticParseKey;
    this.nBestCcgParses = nbestCcgParses;
    this.kb = kb;
  }

  private static Relation EMPTY_RELATION = new Relation("type.empty",
      "type.empty");
  private static String EMPTY_TYPE = "type.empty";
  private static Property NEGATION = new Property(String.format("%s",
      SemanticCategoryType.NEGATION));

  public void processSentence(JsonObject jsonSentence) {
    List<LexicalGraph> uGraphs =
        graphCreator.buildUngroundedGraph(jsonSentence, semanticParseKey,
            nBestCcgParses);

    for (LexicalGraph uGraph : uGraphs) {
      // If the graph contains negation, ignore it.
      boolean containsNegation = false;
      Map<LexicalItem, Set<Property>> props = uGraph.getProperties();
      if (props != null) {
        for (Entry<LexicalItem, Set<Property>> entry : props.entrySet()) {
          if (entry.getValue() != null && entry.getValue().contains(NEGATION)) {
            containsNegation = true;
            break;
          }
        }
      }
      if (containsNegation)
        continue;

      double uScore = 1.0 / uGraphs.size();
      for (Edge<LexicalItem> edge : uGraph.getEdges()) {
        LexicalItem node1 = edge.getLeft();
        LexicalItem node2 = edge.getRight();
        if (node1.isEntity() && node2.isEntity()) {
          Relation uRel = edge.getRelation();
          Relation invUrel = uRel.inverse();
          int uCompare =
              edge.getRelation().getLeft()
                  .compareTo(edge.getRelation().getRight());

          synchronized (predicateCounts) {
            if (uCompare <= 0) {
              Double prevScore = predicateCounts.getOrDefault(uRel, 0.0);
              predicateCounts.put(uRel, prevScore + uScore);
            } else {
              Double prevScore = predicateCounts.getOrDefault(invUrel, 0.0);
              predicateCounts.put(invUrel, prevScore + uScore);
            }
          }

          Set<Relation> groundedRelations =
              kb.getRelations(node1.getMid(), node2.getMid());
          if (groundedRelations != null && groundedRelations.size() > 0) {
            double gScore = 1.0 / (uGraphs.size() * groundedRelations.size());
            for (Relation grel : groundedRelations) {
              if (uCompare < 0) {
                insertUrelGrel(uRel, grel, gScore);
              } else if (uCompare > 0) {
                insertUrelGrel(invUrel, grel.inverse(), gScore);
              } else if (uCompare == 0) {
                if (grel.getLeft().compareTo(grel.getRight()) <= 0) {
                  insertUrelGrel(uRel, grel, gScore);
                } else {
                  insertUrelGrel(uRel, grel.inverse(), gScore);
                }
              }
            }
          } else {
            if (uCompare <= 0) {
              insertUrelGrel(uRel, EMPTY_RELATION, uScore);
            } else {
              insertUrelGrel(invUrel, EMPTY_RELATION, uScore);
            }
          }
        }
      }

      if (!ignoreTypes) {
        for (LexicalItem node : uGraph.getActualNodes()) {
          if (node.isEntity() && !node.isStandardEntity()) {
            TreeSet<Type<LexicalItem>> uTypes = uGraph.getTypes(node);
            if (uTypes != null && uTypes.size() > 0) {
              Set<String> gTypes = kb.getTypes(node.getMid());
              for (Type<LexicalItem> uTypeObj : uTypes) {
                String uType = uTypeObj.getEntityType().getType();

                synchronized (typeCounts) {
                  Double prevUtypeScore = typeCounts.getOrDefault(uType, 0.0);
                  typeCounts.put(uType, prevUtypeScore + uScore);
                }

                HashMap<String, Double> gTypeFreq = null;
                synchronized (langTypeToGroundedTypeMap) {
                  langTypeToGroundedTypeMap.putIfAbsent(uType, new HashMap<>());
                  gTypeFreq = langTypeToGroundedTypeMap.get(uType);
                }

                synchronized (gTypeFreq) {
                  if (gTypes != null && gTypes.size() > 0) {
                    double gScore = 1.0 / (uGraphs.size() * gTypes.size());
                    for (String gType : gTypes) {
                      Double prevScore = gTypeFreq.getOrDefault(gType, 0.0);
                      gTypeFreq.put(gType, prevScore + gScore);
                    }
                  } else {
                    Double prevScore = gTypeFreq.getOrDefault(EMPTY_TYPE, 0.0);
                    gTypeFreq.put(EMPTY_TYPE, prevScore + uScore);
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private void insertUrelGrel(Relation uRel, Relation gRel, double increment) {
    HashMap<Relation, Double> gRelFreq = null;
    synchronized (predicateToGroundedRelationMap) {
      predicateToGroundedRelationMap.putIfAbsent(uRel, new HashMap<>());
      gRelFreq = predicateToGroundedRelationMap.get(uRel);
    }

    synchronized (gRelFreq) {
      Double prevScore = gRelFreq.getOrDefault(gRel, 0.0);
      gRelFreq.put(gRel, prevScore + increment);
    }
  }


  /**
   * Compare pairs having doubles as values
   *
   * @param <T>
   */
  private class EntryComparator<T> implements Comparator<Entry<T, Double>> {
    @Override
    public int compare(Entry<T, Double> o1, Entry<T, Double> o2) {
      return o1.getValue().compareTo(o2.getValue());
    }
  }

  public void printLexicon(BufferedWriter bw) throws IOException {
    // Types
    ArrayList<Entry<String, Double>> langTypes =
        Lists.newArrayList(typeCounts.entrySet());
    Comparator<Entry<String, Double>> comparator2 =
        Collections.reverseOrder(new EntryComparator<String>());
    Collections.sort(langTypes, comparator2);

    bw.write("# Language Types to Grounded Types\n");
    for (Entry<String, Double> langTypeEntry : langTypes) {
      String langType = langTypeEntry.getKey();
      Double langTypeFreq = langTypeEntry.getValue();
      bw.write(String.format("%s\t%f\n", langType, langTypeFreq));

      ArrayList<Entry<String, Double>> groundedTypes =
          Lists
              .newArrayList(langTypeToGroundedTypeMap.get(langType).entrySet());
      Collections.sort(groundedTypes, comparator2);

      for (Entry<String, Double> groundedTypeEntry : groundedTypes) {
        String groundedType = groundedTypeEntry.getKey();
        Double freq = groundedTypeEntry.getValue();
        bw.write(String.format("\t%s\t%f\n", groundedType, freq));
      }
    }

    // Predicates
    ArrayList<Entry<Relation, Double>> predicates =
        Lists.newArrayList(predicateCounts.entrySet());
    Comparator<Entry<Relation, Double>> comparator1 =
        Collections.reverseOrder(new EntryComparator<Relation>());
    Collections.sort(predicates, comparator1);

    bw.write("# Language Predicates to Grounded Relations\n");
    for (Entry<Relation, Double> predicateEntry : predicates) {

      Relation predicate = predicateEntry.getKey();
      Double predicateFreq = predicateEntry.getValue();
      bw.write(String.format("%s %s\t%f\n", predicate.getLeft(),
          predicate.getRight(), predicateFreq));

      ArrayList<Entry<Relation, Double>> groundedRealtions =
          Lists.newArrayList(predicateToGroundedRelationMap.get(predicate)
              .entrySet());
      Collections.sort(groundedRealtions, comparator1);

      for (Entry<Relation, Double> groundedRealtionEntry : groundedRealtions) {
        Relation groundedRelation = groundedRealtionEntry.getKey();
        Double freq = groundedRealtionEntry.getValue();
        bw.write(String.format("\t%s %s\t%f\n", groundedRelation.getLeft(),
            groundedRelation.getRight(), freq));
      }
    }
  }

  public void processStream(InputStream stream, OutputStream lexiconOut,
      int nthreads) throws IOException, InterruptedException {
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
    long lineCount = 0;
    try {
      String line = br.readLine();
      long start = System.currentTimeMillis();
      while (line != null) {
        lineCount++;
        if (lineCount % 500 == 0) {
          long end = System.currentTimeMillis();
          System.err.println(String.format("Processed %d lines @ 500 sentences per %d seconds!",
              lineCount, (end - start) / 1000));
          start = end;
        }
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        if (jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray().size() <= 30) {
          Runnable worker = new PipelineRunnable(this, jsonSentence);
          threadPool.execute(worker);
        }
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

    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(lexiconOut));
    printLexicon(bw);
    bw.close();
  }

  public static class PipelineRunnable implements Runnable {
    JsonObject sentence;
    CreateGroundedLexicon engine;
    PrintStream out;

    public PipelineRunnable(CreateGroundedLexicon engine, JsonObject sentence) {
      this.engine = engine;
      this.sentence = sentence;
    }

    @Override
    public void run() {
      engine.processSentence(sentence);
    }
  }

}
