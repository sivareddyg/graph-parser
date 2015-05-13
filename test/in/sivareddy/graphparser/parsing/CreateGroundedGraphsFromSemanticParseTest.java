package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateGroundedGraphsFromSemanticParseTest {

  KnowledgeBase kb;
  GroundedLexicon groundedLexicon;
  Schema schema;
  CcgAutoLexicon questionCcgAutoLexicon;
  CcgAutoLexicon normalCcgAutoLexicon;

  @Before
  public void setUp() throws Exception {
    /*
     * groundedLexicon = new GroundedLexicon(
     * "data/freebase/grounded_lexicon/business_grounded_lexicon.txt");
     */
    groundedLexicon = new GroundedLexicon("lib_data/dummy.txt");
    schema = new Schema("data/freebase/schema/all_domains_schema.txt");
    kb =
        new KnowledgeBaseOnline("stkilda", "http://stkilda:8890/sparql", "dba",
            "dba", 300, schema);
    questionCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases_questions_vanilla.txt");

    normalCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases.txt");
  }


  @Test
  public void testGroundedGraphs() throws IOException {
    // GroundedLexicon groundedLexicon = null;
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    GroundedGraphs graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, null, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, 10.0, 1.0,
            0.0, 0.0);

    JsonParser parser = new JsonParser();

    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(this.getClass());
    logger.setLevel(Level.DEBUG);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    String line =
        "{\"domain\": [\"people\"], \"sentence\": \"What was Vasco_Nunez_De_Balboa original purpose of his journey ?\", \"url\": \"http://www.freebase.com/view/en/vasco_nunez_de_balboa\", \"dependency_lambda\": [[\"purpose.q.arg_2(4:e , 2:m.0cl9qq)\", \"purpose.of.arg_2(4:e , 7:x)\", \"his(6:s , 6:x)\", \"purpose.of.arg_1(4:e , 4:x)\", \"purpose(4:s , 4:x)\", \"QUESTION(4:x)\", \"journey's.arg_1(7:e , 7:x)\", \"purpose.original(3:s , 4:x)\", \"journey(7:s , 7:x)\", \"purpose.q.arg_1(4:e , 4:x)\", \"journey's.arg_2(7:e , 6:x)\"], [\"purpose.q.arg_2(4:e , 2:m.0cl9qq)\", \"purpose.of.arg_2(4:e , 7:x)\", \"his(6:s , 7:x)\", \"purpose.of.arg_1(4:e , 4:x)\", \"purpose(4:s , 4:x)\", \"QUESTION(4:x)\", \"purpose.original(3:s , 4:x)\", \"journey(7:s , 7:x)\", \"purpose.q.arg_1(4:e , 4:x)\"], [\"purpose.of.arg_1(2:e , 2:m.0cl9qq)\", \"his(6:s , 6:x)\", \"purpose(4:s , 2:m.0cl9qq)\", \"QUESTION(2:m.0cl9qq)\", \"purpose.of.arg_2(2:e , 7:x)\", \"purpose.original(3:s , 2:m.0cl9qq)\", \"journey's.arg_1(7:e , 7:x)\", \"journey(7:s , 7:x)\", \"journey's.arg_2(7:e , 6:x)\"], [\"purpose.of.arg_1(2:e , 2:m.0cl9qq)\", \"purpose(4:s , 2:m.0cl9qq)\", \"QUESTION(2:m.0cl9qq)\", \"his(6:s , 7:x)\", \"purpose.of.arg_2(2:e , 7:x)\", \"purpose.original(3:s , 2:m.0cl9qq)\", \"journey(7:s , 7:x)\"], [\"purpose.q.arg_2(4:e , 2:m.0cl9qq)\", \"be.arg_1(1:e , 4:x)\", \"purpose.of.arg_2(4:e , 7:x)\", \"his(6:s , 6:x)\", \"purpose.of.arg_1(4:e , 4:x)\", \"purpose(4:s , 4:x)\", \"journey's.arg_1(7:e , 7:x)\", \"QUESTION(0:x)\", \"be.arg_2(1:e , 0:x)\", \"purpose.original(3:s , 4:x)\", \"journey(7:s , 7:x)\", \"purpose.q.arg_1(4:e , 4:x)\", \"journey's.arg_2(7:e , 6:x)\"], [\"purpose.q.arg_2(4:e , 2:m.0cl9qq)\", \"be.arg_1(1:e , 4:x)\", \"purpose.of.arg_2(4:e , 7:x)\", \"his(6:s , 7:x)\", \"purpose.of.arg_1(4:e , 4:x)\", \"purpose(4:s , 4:x)\", \"QUESTION(0:x)\", \"be.arg_2(1:e , 0:x)\", \"purpose.original(3:s , 4:x)\", \"journey(7:s , 7:x)\", \"purpose.q.arg_1(4:e , 4:x)\"], [\"purpose.of.arg_1(2:e , 2:m.0cl9qq)\", \"his(6:s , 6:x)\", \"purpose(4:s , 2:m.0cl9qq)\", \"purpose.of.arg_2(2:e , 7:x)\", \"journey's.arg_1(7:e , 7:x)\", \"QUESTION(0:x)\", \"purpose.original(3:s , 2:m.0cl9qq)\", \"be.arg_2(1:e , 0:x)\", \"journey(7:s , 7:x)\", \"be.arg_1(1:e , 2:m.0cl9qq)\", \"journey's.arg_2(7:e , 6:x)\"], [\"purpose.of.arg_1(2:e , 2:m.0cl9qq)\", \"purpose(4:s , 2:m.0cl9qq)\", \"his(6:s , 7:x)\", \"purpose.of.arg_2(2:e , 7:x)\", \"QUESTION(0:x)\", \"purpose.original(3:s , 2:m.0cl9qq)\", \"be.arg_2(1:e , 0:x)\", \"journey(7:s , 7:x)\", \"be.arg_1(1:e , 2:m.0cl9qq)\"]], \"entities\": [{\"index\": 2, \"name\": \"Vasco_Nunez_De_Balboa\", \"entity\": \"m.0cl9qq\"}], \"words\": [{\"category\": \"PRON\", \"head\": 1, \"end\": 3, \"start\": 0, \"break_level\": 3, \"pos\": \"WP\", \"label\": \"attr\", \"lemma\": \"what\", \"word\": \"What\"}, {\"category\": \"VERB\", \"end\": 7, \"lemma\": \"be\", \"break_level\": 1, \"pos\": \"VBD\", \"label\": \"ROOT\", \"start\": 5, \"word\": \"was\"}, {\"category\": \"NOUN\", \"head\": 7, \"end\": 29, \"start\": 24, \"break_level\": 1, \"pos\": \"NNP\", \"label\": \"nn\", \"lemma\": \"Vasco_Nunez_De_Balboa\", \"word\": \"Vasco_Nunez_De_Balboa\"}, {\"category\": \"ADJ\", \"head\": 7, \"end\": 38, \"start\": 31, \"break_level\": 1, \"pos\": \"JJ\", \"label\": \"amod\", \"lemma\": \"original\", \"word\": \"original\"}, {\"category\": \"NOUN\", \"head\": 1, \"end\": 46, \"start\": 40, \"break_level\": 1, \"pos\": \"NN\", \"label\": \"nsubj\", \"lemma\": \"purpose\", \"word\": \"purpose\"}, {\"category\": \"ADP\", \"head\": 7, \"end\": 49, \"start\": 48, \"break_level\": 1, \"pos\": \"IN\", \"label\": \"prep\", \"lemma\": \"of\", \"word\": \"of\"}, {\"category\": \"PRON\", \"head\": 10, \"end\": 53, \"start\": 51, \"break_level\": 1, \"pos\": \"PRP$\", \"label\": \"poss\", \"lemma\": \"his\", \"word\": \"his\"}, {\"category\": \"NOUN\", \"head\": 8, \"end\": 61, \"start\": 55, \"break_level\": 1, \"pos\": \"NN\", \"label\": \"pobj\", \"lemma\": \"journey\", \"word\": \"journey\"}, {\"category\": \".\", \"head\": 1, \"end\": 63, \"start\": 63, \"break_level\": 1, \"pos\": \".\", \"label\": \"p\", \"lemma\": \"?\", \"word\": \"?\"}], \"targetValue\": \"(list (description Explorer))\"}";

    JsonObject jsonSentence = parser.parse(line).getAsJsonObject();

    List<LexicalGraph> graphs =
        graphCreator.buildUngroundedGraph(jsonSentence, "dependency_lambda", 1,
            logger);

    System.out.println("# Ungrounded Graphs");
    if (graphs.size() > 0) {
      for (LexicalGraph ungroundedGraph : graphs) {
        System.out.println(ungroundedGraph);
        System.out.println("Connected: " + ungroundedGraph.isConnected());

        List<LexicalGraph> groundedGraphs =
            graphCreator.createGroundedGraph(ungroundedGraph, 1000, 100,
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
                + GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                    schema));
          }
        }

        System.out.println("# Total number of Grounded Graphs: "
            + groundedGraphs.size());
        System.out.println("# Total number of Connected Grounded Graphs: "
            + connectedGraphCount);
        System.out.println();
      }
    }
  }
}
