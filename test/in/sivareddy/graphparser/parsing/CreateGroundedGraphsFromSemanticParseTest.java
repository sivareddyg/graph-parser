package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
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
            relationTypingIdentifiers, new StructuredPercepton(), 1, true,
            true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, 10.0, 1.0, 0.0, 0.0);

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
                true, false, false, false, false, true, false);
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
          System.out.println("Features: " + groundedGraph.getFeatures());
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

    List<JsonObject> jsonSentences = Lists.newArrayList();

    JsonObject sentence =
        jsonParser
            .parse(
                "{\"index\":\"3f6f7716f41813307f6c46fcd380e14f:1\",\"domain\":[\"film\"],\"sentence\":\"who plays lois lane in superman returns?\",\"url\":\"http://www.freebase.com/view/en/lois_lane\",\"goldRelations\":[{\"relationLeft\":\"film.performance.character\",\"score\":0.16666666666666669,\"relationRight\":\"film.performance.actor\"}],\"targetValue\":\"(list (description \\\"Kate Bosworth\\\"))\",\"goldMid\":\"m.04pzy\",\"entities\":[{\"name\":\"Lois Lane\",\"entity\":\"m.04pzy\",\"score\":1000.254395,\"phrase\":\"lois lane\",\"id\":\"/en/lois_lane\",\"index\":2},{\"name\":\"Superman Returns\",\"entity\":\"m.044g_k\",\"score\":751.707764,\"phrase\":\"superman returns\",\"id\":\"/en/superman_returns\",\"index\":4}],\"words\":[{\"category\":\"PRON\",\"head\":1,\"end\":2,\"lemma\":\"who\",\"dep\":\"nsubj\",\"break_level\":3,\"pos\":\"WP\",\"start\":0,\"word\":\"Who\"},{\"category\":\"VERB\",\"end\":8,\"start\":4,\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"VBZ\",\"lemma\":\"play\",\"word\":\"plays\"},{\"category\":\"NOUN\",\"head\":1,\"end\":18,\"lemma\":\"Lois_Lane\",\"dep\":\"dobj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":15,\"word\":\"Lois_Lane\"},{\"category\":\"ADP\",\"head\":1,\"end\":21,\"lemma\":\"in\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":20,\"word\":\"in\"},{\"category\":\"NOUN\",\"head\":3,\"end\":38,\"lemma\":\"Superman_Returns\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NNS\",\"start\":32,\"word\":\"Superman_Returns\"},{\"category\":\".\",\"head\":1,\"end\":40,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":40,\"word\":\"?\"}],\"dependency_lambda\":[[\"QUESTION(0:x)\",\"play.in.arg_2(1:e , 4:m.044g_k)\",\"play.arg_1(1:e , 0:x)\",\"play.arg_2(1:e , 2:m.04pzy)\",\"who(0:s , 0:x)\"]]}")
            .getAsJsonObject();
    jsonSentences.add(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"index\":\"7c862e4910ddf5154495e18a1c38354a:1\",\"domain\":[\"people\"],\"sentence\":\"what did harriet beecher stowe do as an abolitionist?\",\"url\":\"http://www.freebase.com/view/en/harriet_beecher_stowe\",\"goldRelations\":[{\"relationLeft\":\"people.person.profession.1\",\"score\":1.0,\"relationRight\":\"people.person.profession.2\"}],\"targetValue\":\"(list (description Novelist) (description Writer) (description Author))\",\"goldMid\":\"m.015v00\",\"entities\":[{\"name\":\"Harriet Beecher Stowe\",\"entity\":\"m.015v00\",\"score\":746.807495,\"phrase\":\"harriet beecher stowe\",\"id\":\"/en/harriet_beecher_stowe\",\"index\":2}],\"words\":[{\"category\":\"PRON\",\"head\":3,\"end\":3,\"lemma\":\"what\",\"dep\":\"dobj\",\"break_level\":3,\"pos\":\"WP\",\"start\":0,\"word\":\"What\"},{\"category\":\"VERB\",\"head\":3,\"end\":7,\"start\":5,\"dep\":\"aux\",\"break_level\":1,\"pos\":\"VBD\",\"lemma\":\"do\",\"word\":\"did\"},{\"category\":\"NOUN\",\"head\":3,\"end\":29,\"lemma\":\"Harriet_Beecher_Stowe\",\"dep\":\"nsubj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":25,\"word\":\"Harriet_Beecher_Stowe\"},{\"category\":\"VERB\",\"end\":32,\"lemma\":\"do\",\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"VB\",\"start\":31,\"word\":\"do\"},{\"category\":\"ADP\",\"head\":3,\"end\":35,\"lemma\":\"as\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":34,\"word\":\"as\"},{\"category\":\"DET\",\"head\":6,\"end\":38,\"lemma\":\"an\",\"dep\":\"det\",\"break_level\":1,\"pos\":\"DT\",\"start\":37,\"word\":\"an\"},{\"category\":\"NOUN\",\"head\":4,\"end\":51,\"lemma\":\"abolitionist\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NN\",\"start\":40,\"word\":\"abolitionist\"},{\"category\":\".\",\"head\":3,\"end\":53,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":53,\"word\":\"?\"}],\"dependency_lambda\":[[\"abolitionist(6:s , 6:x)\",\"do.arg_1(3:e , 2:m.015v00)\",\"QUESTION(0:x)\",\"do.as.arg_2(3:e , 6:x)\",\"do.arg_2(3:e , 0:x)\"]]}")
            .getAsJsonObject();
    jsonSentences.add(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"index\":\"30ad81dc40a1d99a8f25d618145dde51:1\",\"domain\":[\"film\"],\"sentence\":\"what movies gerard butler in?\",\"url\":\"http://www.freebase.com/view/en/gerard_butler\",\"goldRelations\":[{\"relationLeft\":\"film.performance.actor\",\"score\":0.375,\"relationRight\":\"film.performance.film\"},{\"relationLeft\":\"tv.regular_tv_appearance.actor\",\"score\":0.14285714285714285,\"relationRight\":\"tv.regular_tv_appearance.series\"}],\"targetValue\":\"(list (description Attila) (description \\\"Beowulf \\u0026 Grendel\\\") (description 300) (description \\\"Dear Frankie\\\") (description \\\"Butterfly on a Wheel\\\") (description Gamer) (description \\\"Fast Food\\\") (description \\\"Dracula 2000\\\") (description Coriolanus))\",\"goldMid\":\"m.038rzr\",\"entities\":[{\"name\":\"Gerard Butler\",\"entity\":\"m.038rzr\",\"score\":639.104919,\"phrase\":\"gerard butler\",\"id\":\"/en/gerard_butler\",\"index\":2}],\"words\":[{\"category\":\"DET\",\"head\":1,\"end\":3,\"lemma\":\"what\",\"dep\":\"dep\",\"break_level\":3,\"pos\":\"WDT\",\"start\":0,\"word\":\"What\"},{\"category\":\"NOUN\",\"end\":10,\"start\":5,\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"NNS\",\"lemma\":\"movie\",\"word\":\"movies\"},{\"category\":\"NOUN\",\"head\":1,\"end\":24,\"lemma\":\"Gerard_Butler\",\"dep\":\"dep\",\"break_level\":1,\"pos\":\"NNP\",\"start\":19,\"word\":\"Gerard_Butler\"},{\"category\":\"ADP\",\"head\":1,\"end\":27,\"lemma\":\"in\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":26,\"word\":\"in\"},{\"category\":\".\",\"head\":1,\"end\":29,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":29,\"word\":\"?\"}],\"dependency_lambda\":[[\"movie(1:s , 1:x)\",\"movie.in.arg_1(1:e , 1:x)\",\"QUESTION(4:x)\",\"movie.in.arg_2(1:e , 4:x)\",\"movie.dep_arg_2(1:e , 2:m.038rzr)\"]]}")
            .getAsJsonObject();
    jsonSentences.add(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"index\":\"738be854c421d0d077850b10577310ac:1\",\"domain\":[\"people\"],\"sentence\":\"what is the name of the first prophet of islam?\",\"url\":\"http://www.freebase.com/view/en/islam\",\"goldRelations\":[{\"relationLeft\":\"religion.religion.founding_figures.1\",\"score\":1.0,\"relationRight\":\"religion.religion.founding_figures.2\"},{\"relationLeft\":\"religion.religion.notable_figures.1\",\"score\":0.4,\"relationRight\":\"religion.religion.notable_figures.2\"},{\"relationLeft\":\"book.written_work.subjects.2\",\"score\":0.020202020202020204,\"relationRight\":\"book.written_work.subjects.1\"},{\"relationLeft\":\"book.written_work.subjects.2\",\"score\":0.020202020202020204,\"relationRight\":\"book.written_work.subjects.1\"},{\"relationLeft\":\"people.person.religion.2\",\"score\":0.02,\"relationRight\":\"people.person.religion.1\"}],\"targetValue\":\"(list (description Muhammad))\",\"goldMid\":\"m.0flw86\",\"entities\":[{\"name\":\"Islam\",\"entity\":\"m.0flw86\",\"score\":137.033417,\"phrase\":\"islam\",\"id\":\"/en/islam\",\"index\":9}],\"words\":[{\"category\":\"PRON\",\"head\":1,\"end\":3,\"lemma\":\"what\",\"dep\":\"attr\",\"break_level\":3,\"pos\":\"WP\",\"start\":0,\"word\":\"What\"},{\"category\":\"VERB\",\"end\":6,\"start\":5,\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"VBZ\",\"lemma\":\"be\",\"word\":\"is\"},{\"category\":\"DET\",\"head\":3,\"end\":10,\"lemma\":\"the\",\"dep\":\"det\",\"break_level\":1,\"pos\":\"DT\",\"start\":8,\"word\":\"the\"},{\"category\":\"NOUN\",\"head\":1,\"end\":15,\"lemma\":\"name\",\"dep\":\"nsubj\",\"break_level\":1,\"pos\":\"NN\",\"start\":12,\"word\":\"name\"},{\"category\":\"ADP\",\"head\":3,\"end\":18,\"lemma\":\"of\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":17,\"word\":\"of\"},{\"category\":\"DET\",\"head\":7,\"end\":22,\"lemma\":\"the\",\"dep\":\"det\",\"break_level\":1,\"pos\":\"DT\",\"start\":20,\"word\":\"the\"},{\"category\":\"ADJ\",\"head\":7,\"end\":28,\"lemma\":\"first\",\"dep\":\"amod\",\"break_level\":1,\"pos\":\"JJ\",\"start\":24,\"word\":\"first\"},{\"category\":\"NOUN\",\"head\":4,\"end\":36,\"lemma\":\"prophet\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NN\",\"start\":30,\"word\":\"prophet\"},{\"category\":\"ADP\",\"head\":7,\"end\":39,\"lemma\":\"of\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":38,\"word\":\"of\"},{\"category\":\"NOUN\",\"head\":8,\"end\":45,\"lemma\":\"Islam\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":41,\"word\":\"Islam\"},{\"category\":\".\",\"head\":1,\"end\":47,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":47,\"word\":\"?\"}],\"dependency_lambda\":[[\"prophet.of.arg_2(7:e , 9:m.0flw86)\",\"be.copula.arg_1(1:e , 3:x)\",\"name.of.arg_1(3:e , 3:x)\",\"be.copula.arg_2(1:e , 0:x)\",\"prophet(7:s , 7:x)\",\"prophet.first(6:s , 7:x)\",\"name.of.arg_2(3:e , 7:x)\",\"QUESTION(0:x)\",\"prophet.of.arg_1(7:e , 7:x)\",\"UNIQUE(3:x)\",\"name(3:s , 3:x)\",\"UNIQUE(7:x)\"]]}")
            .getAsJsonObject();
    jsonSentences.add(sentence);

    for (JsonObject jsonSentence : jsonSentences) {
      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(jsonSentence,
              SentenceKeys.DEPENDENCY_LAMBDA, 1, logger);

      System.out.println("# Ungrounded Graphs");
      if (graphs.size() > 0) {
        for (LexicalGraph ungroundedGraph : graphs) {
          System.out.println(ungroundedGraph);
          System.out.println("Connected: " + ungroundedGraph.isConnected());

          LexicalItem goldNode =
              ungroundedGraph
                  .getMidNode(
                      jsonSentence.get(SentenceKeys.GOLD_MID).getAsString())
                  .iterator().next();

          if (ungroundedGraph.getQuestionNode().iterator().hasNext()) {
            LexicalItem questionNode =
                ungroundedGraph.getQuestionNode().iterator().next();

            Pair<LexicalItem, LexicalItem> mainEdgeKey =
                Pair.of(goldNode, questionNode);
            Pair<LexicalItem, LexicalItem> mainEdgeInverseKey =
                Pair.of(questionNode, goldNode);
            Map<Pair<LexicalItem, LexicalItem>, TreeSet<Relation>> edgeGroundingConstraints =
                new HashMap<>();
            edgeGroundingConstraints.put(mainEdgeKey, new TreeSet<>());
            edgeGroundingConstraints.put(mainEdgeInverseKey, new TreeSet<>());
            for (JsonElement goldRelation : jsonSentence.get(
                SentenceKeys.GOLD_RELATIONS).getAsJsonArray()) {
              JsonObject goldRelationObj = goldRelation.getAsJsonObject();
              Relation mainRelation =
                  new Relation(goldRelationObj.get(SentenceKeys.RELATION_LEFT)
                      .getAsString(), goldRelationObj.get(
                      SentenceKeys.RELATION_RIGHT).getAsString(),
                      goldRelationObj.get(SentenceKeys.SCORE).getAsDouble());
              Relation mainRelationInverse = mainRelation.inverse();
              edgeGroundingConstraints.get(mainEdgeKey).add(mainRelation);
              edgeGroundingConstraints.get(mainEdgeInverseKey).add(
                  mainRelationInverse);
            }

            System.out.println(edgeGroundingConstraints);

            List<LexicalGraph> groundedGraphs =
                graphCreator.createGroundedGraph(ungroundedGraph, null,
                    edgeGroundingConstraints, Sets.newHashSet(goldNode), 1000,
                    10000, true, true, false, false, false, false, true, false);
            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());

            int connectedGraphCount = 0;
            for (LexicalGraph groundedGraph : groundedGraphs) {
              if (groundedGraph.isConnected()) {
                connectedGraphCount += 1;
              }

              System.out.println("# Grounded graph: " + connectedGraphCount);
              System.out.println(groundedGraph.getParallelGraph());
              System.out.println(groundedGraph);
              System.out.println("Graph Query: "
                  + GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                      schema, 200));
              System.out.println("Features: " + groundedGraph.getFeatures());
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
  }
}
