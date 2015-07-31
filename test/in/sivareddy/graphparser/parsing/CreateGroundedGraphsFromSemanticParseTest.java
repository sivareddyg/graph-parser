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
  GroundedGraphs graphCreator;

  JsonParser jsonParser = new JsonParser();

  PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
  Logger logger = Logger.getLogger(this.getClass());

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

    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};

    graphCreator =
        new GroundedGraphs(schema, kb, groundedLexicon, normalCcgAutoLexicon,
            questionCcgAutoLexicon, relationLexicalIdentifiers,
            relationTypingIdentifiers, null, 1, false, false, false, false,
            false, false, false, false, false, true, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, 10.0, 1.0, 0.0, 0.0);

    logger.setLevel(Level.DEBUG);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);
  }


  @Test
  public void testGroundedGraphs() throws IOException {
    // GroundedLexicon groundedLexicon = null;
    String line =
        "{\"sentence\": \"What year were the Cincinnati_Reds founded ?\", \"url\": \"http://www.freebase.com/view/en/cincinnati_reds\", \"dependency_lambda\": [[\"found.arg_2(5:e , 4:m.01ypc)\", \"QUESTION(1:x)\", \"year(1:s , 1:x)\", \"found.tmod(5:e , 1:x)\", \"UNIQUE(4:m.01ypc)\"]], \"entities\": [{\"index\": 4, \"name\": \"Cincinnati_Reds\", \"entity\": \"m.01ypc\"}], \"words\": [{\"category\": \"DET\", \"head\": 1, \"end\": 3, \"start\": 0, \"break_level\": 3, \"pos\": \"WDT\", \"label\": \"det\", \"lemma\": \"what\", \"word\": \"What\"}, {\"category\": \"NOUN\", \"head\": 6, \"end\": 8, \"start\": 5, \"break_level\": 1, \"pos\": \"NN\", \"label\": \"tmod\", \"lemma\": \"year\", \"word\": \"year\"}, {\"category\": \"VERB\", \"head\": 6, \"end\": 13, \"lemma\": \"be\", \"break_level\": 1, \"pos\": \"VBD\", \"label\": \"auxpass\", \"start\": 10, \"word\": \"were\"}, {\"category\": \"DET\", \"head\": 5, \"end\": 17, \"start\": 15, \"break_level\": 1, \"pos\": \"DT\", \"label\": \"det\", \"lemma\": \"the\", \"word\": \"the\"}, {\"category\": \"NOUN\", \"head\": 6, \"end\": 33, \"start\": 30, \"break_level\": 1, \"pos\": \"NNPS\", \"label\": \"nsubjpass\", \"lemma\": \"Cincinnati_Reds\", \"word\": \"Cincinnati_Reds\"}, {\"category\": \"VERB\", \"end\": 41, \"lemma\": \"found\", \"break_level\": 1, \"pos\": \"VBN\", \"label\": \"ROOT\", \"start\": 35, \"word\": \"founded\"}, {\"category\": \".\", \"head\": 6, \"end\": 43, \"start\": 43, \"break_level\": 1, \"pos\": \".\", \"label\": \"p\", \"lemma\": \"?\", \"word\": \"?\"}], \"targetValue\": \"(list (description 1881))\"}";

    JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();

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
                  schema, 100));
        }

        System.out.println("# Total number of Grounded Graphs: "
            + groundedGraphs.size());
        System.out.println("# Total number of Connected Grounded Graphs: "
            + connectedGraphCount);
        System.out.println();
      }
    }
  }

  @Test
  public void testGroundedGraphsWithCollapseAndFiltering() throws IOException {
    /*-
    Semantic Parse: [prophet.first(6:s , 7:x), UNIQUE(3:x), QUESTION(0:x), name(3:s , 3:x), prophet.of.arg_2(7:e , 9:m.0flw86), prophet(7:s , 7:x), UNIQUE(7:x), name.of.arg_1(3:e , 3:x), be.copula.arg_2(1:e , 0:x), name.of.arg_2(3:e , 7:x), be.copula.arg_1(1:e , 3:x), prophet.of.arg_1(7:e , 7:x)]
    Words: 
    LexicalItem{0, What, what, WP, null}
    LexicalItem{1, is, be, VBZ, null}
    LexicalItem{3, name, name, NN, null}
    LexicalItem{6, first, first, JJ, null}
    LexicalItem{7, prophet, prophet, NN, null}
    LexicalItem{9, Islam, islam, NNP, null}
    Edges: 
    (1,0,3) (be.copula.arg_2,be.copula.arg_1):0.0
    (3,7,3) (name.of.arg_2,name.of.arg_1):0.0
    (7,9,7) (prophet.of.arg_2,prophet.of.arg_1):0.0
    Types: 
    (3,3)   name:0.0
    (7,7)   prophet:0.0
    (7,6)   prophet.first:0.0
    Properties: 
    0   [QUESTION]
    3   [UNIQUE]
    7   [UNIQUE]
    EventTypes: 
    EventEventModifiers: 
     */
    JsonObject jsonSentence =
        jsonParser
            .parse(
                "{\"index\":\"738be854c421d0d077850b10577310ac:1\",\"domain\":[\"people\"],\"sentence\":\"what is the name of the first prophet of islam?\",\"url\":\"http://www.freebase.com/view/en/islam\",\"goldRelations\":[{\"relationLeft\":\"religion.religion.founding_figures.1\",\"score\":1.0,\"relationRight\":\"religion.religion.founding_figures.2\"},{\"relationLeft\":\"religion.religion.notable_figures.1\",\"score\":0.4,\"relationRight\":\"religion.religion.notable_figures.2\"},{\"relationLeft\":\"book.written_work.subjects.2\",\"score\":0.020202020202020204,\"relationRight\":\"book.written_work.subjects.1\"},{\"relationLeft\":\"book.written_work.subjects.2\",\"score\":0.020202020202020204,\"relationRight\":\"book.written_work.subjects.1\"},{\"relationLeft\":\"people.person.religion.2\",\"score\":0.02,\"relationRight\":\"people.person.religion.1\"}],\"targetValue\":\"(list (description Muhammad))\",\"goldMid\":\"m.0flw86\",\"entities\":[{\"name\":\"Islam\",\"entity\":\"m.0flw86\",\"score\":137.033417,\"phrase\":\"islam\",\"id\":\"/en/islam\",\"index\":9}],\"words\":[{\"category\":\"PRON\",\"head\":1,\"end\":3,\"lemma\":\"what\",\"break_level\":3,\"pos\":\"WP\",\"label\":\"attr\",\"start\":0,\"word\":\"What\"},{\"category\":\"VERB\",\"end\":6,\"start\":5,\"break_level\":1,\"pos\":\"VBZ\",\"label\":\"ROOT\",\"lemma\":\"be\",\"word\":\"is\"},{\"category\":\"DET\",\"head\":3,\"end\":10,\"lemma\":\"the\",\"break_level\":1,\"pos\":\"DT\",\"label\":\"det\",\"start\":8,\"word\":\"the\"},{\"category\":\"NOUN\",\"head\":1,\"end\":15,\"lemma\":\"name\",\"break_level\":1,\"pos\":\"NN\",\"label\":\"nsubj\",\"start\":12,\"word\":\"name\"},{\"category\":\"ADP\",\"head\":3,\"end\":18,\"lemma\":\"of\",\"break_level\":1,\"pos\":\"IN\",\"label\":\"prep\",\"start\":17,\"word\":\"of\"},{\"category\":\"DET\",\"head\":7,\"end\":22,\"lemma\":\"the\",\"break_level\":1,\"pos\":\"DT\",\"label\":\"det\",\"start\":20,\"word\":\"the\"},{\"category\":\"ADJ\",\"head\":7,\"end\":28,\"lemma\":\"first\",\"break_level\":1,\"pos\":\"JJ\",\"label\":\"amod\",\"start\":24,\"word\":\"first\"},{\"category\":\"NOUN\",\"head\":4,\"end\":36,\"lemma\":\"prophet\",\"break_level\":1,\"pos\":\"NN\",\"label\":\"pobj\",\"start\":30,\"word\":\"prophet\"},{\"category\":\"ADP\",\"head\":7,\"end\":39,\"lemma\":\"of\",\"break_level\":1,\"pos\":\"IN\",\"label\":\"prep\",\"start\":38,\"word\":\"of\"},{\"category\":\"NOUN\",\"head\":8,\"end\":45,\"lemma\":\"Islam\",\"break_level\":1,\"pos\":\"NNP\",\"label\":\"pobj\",\"start\":41,\"word\":\"Islam\"},{\"category\":\".\",\"head\":1,\"end\":47,\"lemma\":\"?\",\"break_level\":1,\"pos\":\".\",\"label\":\"p\",\"start\":47,\"word\":\"?\"}],\"dependency_lambda\":[[\"prophet.of.arg_2(7:e , 9:m.0flw86)\",\"be.copula.arg_1(1:e , 3:x)\",\"name.of.arg_1(3:e , 3:x)\",\"be.copula.arg_2(1:e , 0:x)\",\"prophet(7:s , 7:x)\",\"prophet.first(6:s , 7:x)\",\"name.of.arg_2(3:e , 7:x)\",\"QUESTION(0:x)\",\"prophet.of.arg_1(7:e , 7:x)\",\"UNIQUE(3:x)\",\"name(3:s , 3:x)\",\"UNIQUE(7:x)\"]]}")
            .getAsJsonObject();

    List<LexicalGraph> graphs =
        graphCreator.buildUngroundedGraph(jsonSentence, "dependency_lambda", 1,
            logger);
    
    System.out.println("# Ungrounded Graphs");
    if (graphs.size() > 0) {
      for (LexicalGraph ungroundedGraph : graphs) {
        System.out.println(ungroundedGraph);
        System.out.println("Connected: " + ungroundedGraph.isConnected());

        List<LexicalGraph> groundedGraphs =
            graphCreator.createGroundedGraph(ungroundedGraph, null, 1000, 100, true,
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
                  schema, 100));
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
