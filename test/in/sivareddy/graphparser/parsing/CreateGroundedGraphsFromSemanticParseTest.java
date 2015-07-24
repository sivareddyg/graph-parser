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
            "dba", 50000, schema);
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
            false, false, false, false, true, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, 10.0, 1.0, 0.0, 0.0);

    JsonParser parser = new JsonParser();

    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(this.getClass());
    logger.setLevel(Level.DEBUG);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    String line =
        "{\"sentence\": \"What year were the Cincinnati_Reds founded ?\", \"url\": \"http://www.freebase.com/view/en/cincinnati_reds\", \"dependency_lambda\": [[\"found.arg_2(5:e , 4:m.01ypc)\", \"QUESTION(1:x)\", \"year(1:s , 1:x)\", \"found.tmod(5:e , 1:x)\", \"UNIQUE(4:m.01ypc)\"]], \"entities\": [{\"index\": 4, \"name\": \"Cincinnati_Reds\", \"entity\": \"m.01ypc\"}], \"words\": [{\"category\": \"DET\", \"head\": 1, \"end\": 3, \"start\": 0, \"break_level\": 3, \"pos\": \"WDT\", \"label\": \"det\", \"lemma\": \"what\", \"word\": \"What\"}, {\"category\": \"NOUN\", \"head\": 6, \"end\": 8, \"start\": 5, \"break_level\": 1, \"pos\": \"NN\", \"label\": \"tmod\", \"lemma\": \"year\", \"word\": \"year\"}, {\"category\": \"VERB\", \"head\": 6, \"end\": 13, \"lemma\": \"be\", \"break_level\": 1, \"pos\": \"VBD\", \"label\": \"auxpass\", \"start\": 10, \"word\": \"were\"}, {\"category\": \"DET\", \"head\": 5, \"end\": 17, \"start\": 15, \"break_level\": 1, \"pos\": \"DT\", \"label\": \"det\", \"lemma\": \"the\", \"word\": \"the\"}, {\"category\": \"NOUN\", \"head\": 6, \"end\": 33, \"start\": 30, \"break_level\": 1, \"pos\": \"NNPS\", \"label\": \"nsubjpass\", \"lemma\": \"Cincinnati_Reds\", \"word\": \"Cincinnati_Reds\"}, {\"category\": \"VERB\", \"end\": 41, \"lemma\": \"found\", \"break_level\": 1, \"pos\": \"VBN\", \"label\": \"ROOT\", \"start\": 35, \"word\": \"founded\"}, {\"category\": \".\", \"head\": 6, \"end\": 43, \"start\": 43, \"break_level\": 1, \"pos\": \".\", \"label\": \"p\", \"lemma\": \"?\", \"word\": \"?\"}], \"targetValue\": \"(list (description 1881))\"}";

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
            graphCreator.createGroundedGraph(ungroundedGraph, 1000, 100, true,
                true, true, false, false, false);
        System.out.println("# Total number of Grounded Graphs: "
            + groundedGraphs.size());

        int connectedGraphCount = 0;
        for (LexicalGraph groundedGraph : groundedGraphs) {
          if (groundedGraph.isConnected()) {
            connectedGraphCount += 1;
          }

          System.out.println("# Grounded graph: " + connectedGraphCount);
          System.out.println(groundedGraph);
          System.out.println("Graph Query: "
              + GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                  schema));
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
