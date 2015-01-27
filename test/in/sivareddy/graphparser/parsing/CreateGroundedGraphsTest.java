package in.sivareddy.graphparser.parsing;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase.KnowledgeBase;
import in.sivareddy.graphparser.util.Schema;
import org.apache.log4j.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class CreateGroundedGraphsTest {

  KnowledgeBase kb;
  GroundedLexicon groundedLexicon;

  private void load() throws IOException {
    if (groundedLexicon == null) {
      groundedLexicon =
          new GroundedLexicon(
              "data/freebase/grounded_lexicon/business_grounded_lexicon.txt");
    }
    if (kb == null) {
      kb =
          new KnowledgeBase("data/freebase/domain_facts/business_facts.txt.gz",
              "data/freebase/stats/business_relation_types.txt");
    }
  }

  @Test
  public void testGroundedGraphs() throws IOException {
    load();
    Schema schema = new Schema("data/freebase/schema/business_schema.txt");
    // KnowledgeBase kb = null;
    CcgAutoLexicon questionCcgAutoLexicon =
        new CcgAutoLexicon("./data/candc_markedup.modified",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases_questions.txt");

    CcgAutoLexicon normalCcgAutoLexicon =
        new CcgAutoLexicon("./data/candc_markedup.modified",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases.txt");

    // GroundedLexicon groundedLexicon = null;
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    GroundedGraphs graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, null, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, 10.0, 1.0, 0.0,
            0.0);

    BufferedReader br =
        new BufferedReader(new FileReader(
            "data/tests/sample_business_training_sentences.txt"));
    JsonParser parser = new JsonParser();

    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(this.getClass());
    logger.setLevel(Level.DEBUG);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    try {
      String line = br.readLine();
      for (int i = 0; i < 33; i++) {
        line = br.readLine();
      }

      while (line != null) {
        if (line.trim().equals("") || line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }
        line =
            "{\"entities\": [{\"index\": 0, \"name\": \"James_Cameron\", \"entity\": \"m.03_gd\"}, {\"index\": 4, \"name\": \"Titanic\", \"entity\": \"m.0dr_4\"}, {\"index\": 6, \"name\": \"1997\", \"entity\": \"type.datetime\"}], \"dependency_lambda\": [[\"direct.arg_1(1:e , 0:m.03_gd)\", \"direct.arg_2(1:e , 4:m.0dr_4)\", \"direct.in.arg2(1:e , 6:type.datetime)\", \"movie(3:s , 4:m.0dr_4)\"]], \"words\": [{\"category\": \"NOUN\", \"head\": 2, \"end\": 12, \"break_level\": 1, \"pos\": \"NNP\", \"label\": \"nsubj\", \"start\": 6, \"word\": \"James_Cameron\"}, {\"category\": \"VERB\", \"end\": 21, \"break_level\": 1, \"pos\": \"VBD\", \"label\": \"ROOT\", \"start\": 14, \"word\": \"directed\"}, {\"category\": \"DET\", \"head\": 4, \"end\": 25, \"break_level\": 1, \"pos\": \"DT\", \"label\": \"det\", \"start\": 23, \"word\": \"the\"}, {\"category\": \"NOUN\", \"head\": 2, \"end\": 31, \"break_level\": 1, \"pos\": \"NN\", \"label\": \"dobj\", \"start\": 27, \"word\": \"movie\"}, {\"category\": \"NOUN\", \"head\": 4, \"end\": 39, \"break_level\": 1, \"pos\": \"NNP\", \"label\": \"appos\", \"start\": 33, \"word\": \"Titanic\"}, {\"category\": \"ADP\", \"head\": 5, \"end\": 42, \"break_level\": 1, \"pos\": \"IN\", \"label\": \"prep\", \"start\": 41, \"word\": \"in\"}, {\"category\": \"NUM\", \"head\": 6, \"end\": 47, \"break_level\": 1, \"pos\": \"CD\", \"label\": \"pobj\", \"start\": 44, \"word\": \"1997\"}], \"sentence\": \"James Cameron directed the movie Titanic in 1997\\n\"}\n";



        JsonObject jsonSentence = parser.parse(line).getAsJsonObject();

        // JsonObject jsonSentence =
        // parser.parse(line).getAsJsonObject();
        List<LexicalGraph> graphs =
            graphCreator.buildUngroundedGraph(jsonSentence,
                "dependency_lambda", 1, logger);

        System.out.println("# Ungrounded Graphs");
        if (graphs.size() > 0) {
          for (LexicalGraph ungroundedGraph : graphs) {
            System.out.println(ungroundedGraph);
            System.out.println("Connected: " + ungroundedGraph.isConnected());

            List<LexicalGraph> groundedGraphs =
                graphCreator.createGroundedGraph(ungroundedGraph, 10, 10000,
                    true, true, true, false, false, false);
            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());

            int connectedGraphCount = 0;
            for (LexicalGraph groundedGraph : groundedGraphs) {
              if (groundedGraph.isConnected()) {
                connectedGraphCount += 1;
                System.out.println("# Grounded graph: " + connectedGraphCount);
                System.out.println(groundedGraph);
                System.out.println("Graph Query: "
                    + GraphToSparqlConverter.convertGroundedGraph(
                        groundedGraph, schema));
              }
            }

            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());
            System.out.println("# Total number of Connected Grounded Graphs: "
                + connectedGraphCount);
            System.out.println();
          }
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

}
