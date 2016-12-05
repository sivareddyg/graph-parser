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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Before;
import org.junit.Test;
import org.maltparser.core.helper.HashSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

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
        new KnowledgeBaseOnline("buck.inf.ed.ac.uk", "http://buck.inf.ed.ac.uk:8890/sparql", "dba",
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

    graphCreator = new GroundedGraphs(schema, kb, groundedLexicon,
        normalCcgAutoLexicon, questionCcgAutoLexicon,
        relationLexicalIdentifiers, relationTypingIdentifiers,
        new StructuredPercepton(), 1, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, true, true, true, true,
        true, true, false, 10.0, 1.0, 0.0, 0.0);

    logger.setLevel(Level.DEBUG);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);
  }

  @Test
  public void testBoWGroundedGraphs() throws IOException {
    // GroundedLexicon groundedLexicon = null;
    String line =
        "{\"index\": \"bf14eac129d0642f6fb4d8aad17f0614\", \"sentence\": \"what are major religions in the united states?\", \"url\": \"http://www.freebase.com/view/en/united_states\", \"goldRelations\": [{\"relationLeft\": \"location.statistical_region.religions.inverse\", \"score\": 1.0, \"relationRight\": \"location.religion_percentage.religion\"}], \"forest\": [{\"index\": \"bf14eac129d0642f6fb4d8aad17f0614:1\", \"sentence\": \"what are major religions in the united states?\", \"url\": \"http://www.freebase.com/view/en/united_states\", \"synPars\": [{\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) (<T NP\\\\NP fa 0 2> (<L (NP\\\\NP)/NP in in IN O O (NP\\\\NP)/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP ba 0 2> (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) ) (<T (S/NP)\\\\(S/NP) fa 0 2> (<L ((S/NP)\\\\(S/NP))/NP in in IN O O ((S/NP)\\\\(S/NP))/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP[nb] ba 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N major major JJ O O NP[nb]/N>) (<L N religions religion NNS O O N>) ) (<T NP\\\\NP fa 0 2> (<L (NP\\\\NP)/NP in in IN O O (NP\\\\NP)/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] ba 0 2> (<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) ) ) (<T S[wq]\\\\S[wq] fa 0 2> (<L (S[wq]\\\\S[wq])/NP in in IN O O (S[wq]\\\\S[wq])/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP bx 0 2> (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) ) (<T S\\\\S fa 0 2> (<L (S\\\\S)/NP in in IN O O (S\\\\S)/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) \", \"score\": 1.0}], \"goldRelations\": [{\"relationLeft\": \"location.statistical_region.religions.inverse\", \"score\": 1.0, \"relationRight\": \"location.religion_percentage.religion\"}], \"entities\": [{\"phrase\": \"united states\", \"score\": 1652.30927975584, \"index\": 6, \"name\": \"United States\", \"entity\": \"m.04sg45j\"}], \"words\": [{\"lemma\": \"what\", \"ner\": \"O\", \"word\": \"What\", \"pos\": \"WP\"}, {\"lemma\": \"be\", \"ner\": \"O\", \"word\": \"are\", \"pos\": \"VBP\"}, {\"lemma\": \"major\", \"ner\": \"O\", \"word\": \"major\", \"pos\": \"JJ\"}, {\"lemma\": \"religion\", \"ner\": \"O\", \"word\": \"religions\", \"pos\": \"NNS\"}, {\"lemma\": \"in\", \"ner\": \"O\", \"word\": \"in\", \"pos\": \"IN\"}, {\"lemma\": \"the\", \"ner\": \"O\", \"word\": \"the\", \"pos\": \"DT\"}, {\"lemma\": \"united_states\", \"ner\": \"LOCATION\", \"word\": \"United_States\", \"pos\": \"NNP\"}, {\"lemma\": \"?\", \"ner\": \"O\", \"word\": \"?\", \"pos\": \".\", \"sentEnd\": true}], \"targetValue\": \"(list (description \\\"Unitarian Universalism\\\") (description Judaism) (description Christianity) (description Atheism) (description Buddhism) (description Hinduism) (description Islam))\", \"goldMid\": \"m.09c7w0\"}, {\"index\": \"bf14eac129d0642f6fb4d8aad17f0614:2\", \"sentence\": \"what are major religions in the united states?\", \"url\": \"http://www.freebase.com/view/en/united_states\", \"synPars\": [{\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) (<T NP\\\\NP fa 0 2> (<L (NP\\\\NP)/NP in in IN O O (NP\\\\NP)/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP ba 0 2> (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) ) (<T (S/NP)\\\\(S/NP) fa 0 2> (<L ((S/NP)\\\\(S/NP))/NP in in IN O O ((S/NP)\\\\(S/NP))/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP[nb] ba 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N major major JJ O O NP[nb]/N>) (<L N religions religion NNS O O N>) ) (<T NP\\\\NP fa 0 2> (<L (NP\\\\NP)/NP in in IN O O (NP\\\\NP)/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] ba 0 2> (<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) ) ) (<T S[wq]\\\\S[wq] fa 0 2> (<L (S[wq]\\\\S[wq])/NP in in IN O O (S[wq]\\\\S[wq])/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) \", \"score\": 1.0}, {\"synPar\": \"(<T S[wq] fa 0 2> (<L S[wq]/(S[q]/NP) What What WP O O S[wq]/(S[q]/NP)>) (<T S[q]/NP bx 0 2> (<T S[q]/NP fa 0 2> (<L (S[q]/NP)/NP are be VBP O O (S[q]/NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N major major JJ O O N/N>) (<L N religions religion NNS O O N>) ) ) ) (<T S\\\\S fa 0 2> (<L (S\\\\S)/NP in in IN O O (S\\\\S)/NP>) (<T NP[nb] rp 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N the the DT O O NP[nb]/N>) (<L N United_States United-States NNP O O N>) ) (<L . ? ? . O O .>) ) ) ) ) \", \"score\": 1.0}], \"goldRelations\": [{\"relationLeft\": \"location.statistical_region.religions.inverse\", \"score\": 1.0, \"relationRight\": \"location.religion_percentage.religion\"}], \"entities\": [{\"index\": 6, \"name\": \"United States\", \"entity\": \"m.09c7w0\", \"score\": 1652.30927975584, \"phrase\": \"united states\", \"id\": \"/en/united_states_of_america\"}], \"words\": [{\"lemma\": \"what\", \"ner\": \"O\", \"word\": \"What\", \"pos\": \"WP\"}, {\"lemma\": \"be\", \"ner\": \"O\", \"word\": \"are\", \"pos\": \"VBP\"}, {\"lemma\": \"major\", \"ner\": \"O\", \"word\": \"major\", \"pos\": \"JJ\"}, {\"lemma\": \"religion\", \"ner\": \"O\", \"word\": \"religions\", \"pos\": \"NNS\"}, {\"lemma\": \"in\", \"ner\": \"O\", \"word\": \"in\", \"pos\": \"IN\"}, {\"lemma\": \"the\", \"ner\": \"O\", \"word\": \"the\", \"pos\": \"DT\"}, {\"lemma\": \"united_states\", \"ner\": \"LOCATION\", \"word\": \"United_States\", \"pos\": \"NNP\"}, {\"lemma\": \"?\", \"ner\": \"O\", \"word\": \"?\", \"pos\": \".\", \"sentEnd\": true}], \"targetValue\": \"(list (description \\\"Unitarian Universalism\\\") (description Judaism) (description Christianity) (description Atheism) (description Buddhism) (description Hinduism) (description Islam))\", \"goldMid\": \"m.09c7w0\"}], \"targetValue\": \"(list (description \\\"Unitarian Universalism\\\") (description Judaism) (description Christianity) (description Atheism) (description Buddhism) (description Hinduism) (description Islam))\", \"goldMid\": \"m.09c7w0\"}";
    line = "{\"sentence\":\"portable document format is supported by how many computing platforms?\",\"qid\":97000400,\"num_node\":2,\"num_edge\":1,\"words\":[{\"pos\":\"PROPN\",\"word\":\"PortableDocumentFormat\",\"lemma\":\"PortableDocumentFormat\"},{\"lemma\":\"be\",\"word\":\"is\",\"pos\":\"AUX\"},{\"lemma\":\"support\",\"word\":\"supported\",\"pos\":\"VERB\"},{\"lemma\":\"by\",\"word\":\"by\",\"pos\":\"ADP\"},{\"lemma\":\"how\",\"word\":\"how\",\"pos\":\"ADV\"},{\"lemma\":\"many\",\"word\":\"many\",\"pos\":\"ADJ\"},{\"lemma\":\"compute\",\"word\":\"computing\",\"pos\":\"NOUN\"},{\"lemma\":\"platform\",\"word\":\"platforms\",\"pos\":\"NOUN\"},{\"lemma\":\"?\",\"word\":\"?\",\"pos\":\"PUNCT\",\"sentEnd\":true}],\"answer\":[\"2\"],\"goldMids\":[\"m.0600q\"],\"id\":97000400,\"index\":\"6bcdba240354ae7a4b85dff04e1ed69d:1\",\"entities\":[{\"entity\":\"m.050_gqz\",\"score\":17.335253697667465,\"phrase\":\"portable document format\",\"name\":\"Portable document format reference manual\",\"index\":0}]}";

    JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();

    List<LexicalGraph> graphs =
        graphCreator.buildUngroundedGraph(jsonSentence, "bow_question_graph",
            1, logger);

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
  public void testGraphPaths() throws IOException {
    // GroundedLexicon groundedLexicon = null;
    String line =
        "{\"sentence\":\"The libel OBJ has proved that the SUBJ was defamatory\",\"entities\":[{\"entity\":\"m.OBJ\",\"index\":2},{\"entity\":\"m.SUBJ\",\"index\":7}],\"words\":[{\"word\":\"The\",\"lemma\":\"the\",\"pos\":\"DT\",\"ner\":\"O\"},{\"word\":\"libel\",\"lemma\":\"libel\",\"pos\":\"NN\",\"ner\":\"O\"},{\"word\":\"OBJ\",\"lemma\":\"OBJ\",\"pos\":\"NNP\",\"ner\":\"O\"},{\"word\":\"has\",\"lemma\":\"have\",\"pos\":\"VBZ\",\"ner\":\"O\"},{\"word\":\"proved\",\"lemma\":\"prove\",\"pos\":\"VBN\",\"ner\":\"O\"},{\"word\":\"that\",\"lemma\":\"that\",\"pos\":\"IN\",\"ner\":\"O\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DT\",\"ner\":\"O\"},{\"word\":\"SUBJ\",\"lemma\":\"SUBJ\",\"pos\":\"NNP\",\"ner\":\"O\"},{\"word\":\"was\",\"lemma\":\"be\",\"pos\":\"VBD\",\"ner\":\"O\"},{\"word\":\"defamatory\",\"lemma\":\"defamatory\",\"pos\":\"JJ\",\"ner\":\"O\",\"sentEnd\":true}], \"dependency_lambda\":[[\"defamatory.1(9:s , 7:m.SUBJ)\",\"UNIQUE(7:m.SUBJ)\",\"proved.2(4:e , 9:e)\",\"libel(1:s , 2:m.OBJ)\",\"proved.1(4:e , 2:m.OBJ)\",\"UNIQUE(2:m.OBJ)\"]]}";

    JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();

    List<LexicalGraph> graphs =
        graphCreator.buildUngroundedGraph(jsonSentence, "dependency_lambda", 1,
            logger);

    System.out.println("# Ungrounded Graphs");
    if (graphs.size() > 0) {
      for (LexicalGraph ungroundedGraph : graphs) {
        System.out.println(ungroundedGraph);
        IndexedWord obj =
            GroundedGraphs.makeWord(ungroundedGraph.getActualNodes().get(2));
        IndexedWord subj =
            GroundedGraphs.makeWord(ungroundedGraph.getActualNodes().get(7));
        SemanticGraph semanticGraph =
            graphCreator.buildSemanticGraphFromSemanticParse(
                ungroundedGraph.getSemanticParse(),
                ungroundedGraph.getActualNodes());
        System.out.println(semanticGraph);
        System.out.println("Directed: "
            + semanticGraph.getShortestDirectedPathNodes(subj, obj));

        System.out.println("Path from SUBJ to OBJ");
        List<SemanticGraphEdge> edges =
            semanticGraph.getShortestUndirectedPathEdges(subj, obj);
        for (SemanticGraphEdge edge : edges) {
          System.out.println(edge.getRelation());
        }

        System.out.println("Path from OBJ to SUBJ");
        edges = semanticGraph.getShortestUndirectedPathEdges(obj, subj);
        for (SemanticGraphEdge edge : edges) {
          System.out.println(edge.getRelation());
        }
        System.out.println();
      }
    }
  }


  @Test
  public void testGroundedGraphsWithMergeAndFiltering() throws IOException {
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
    
    sentence =
        jsonParser
            .parse(
                "{\"index\": \"0f48624da600dcbb7079e87bab410fc3:1\", \"sentence\": \"what is the name of the first harry potter novel?\", \"url\": \"http://www.freebase.com/view/en/harry_potter\", \"dependency_lambda\": [[\"QUESTION(0:x)\", \"name.arg0(3:e , 3:x)\", \"name(3:s , 3:x)\", \"what.arg1(0:e , 3:x)\", \"what(0:s , 0:x)\", \"first(6:s , 8:x)\", \"novel.arg0(8:e , 8:x)\", \"name.nmod.of(3:e , 8:x)\", \"novel.compound(8:e , 7:m.harrypotter)\", \"what.arg0(0:e , 0:x)\", \"novel(8:s , 8:x)\"]], \"deplambda_oblique_tree\": \"(l-punct (l-nsubj (l-cop w-1-what w-2-is) (l-nmod (l-det w-4-name w-3-the) (l-case (l-det (l-compound (l-amod w-9-novel w-7-first) w-8-harrypotter) w-6-the) w-5-of))) w-10-?)\", \"goldRelations\": [{\"relationLeft\": \"book.written_work.part_of_series.2\", \"score\": 0.25, \"relationRight\": \"book.written_work.part_of_series.1\"}], \"entities\": [{\"index\": 7, \"end\": 7, \"name\": \"Harry Potter literary series\", \"entity\": \"m.078ffw\", \"start\": 7, \"score\": 55.480726287805602, \"phrase\": \"harry potter\", \"id\": \"/en/harry_potter\"}], \"deplambda_expression\": \"(lambda $0:<a,e> (exists:ex $1:<a,e> (and:c (and:c (p_TYPE_w-1-what:u $0) (p_EVENT_w-1-what:u $0) (p_EVENT.ENTITY_arg0:b $0 $0) (p_TARGET:u $0)) (exists:ex $2:<a,e> (and:c (and:c (and:c (p_TYPE_w-4-name:u $1) (p_EVENT_w-4-name:u $1) (p_EVENT.ENTITY_arg0:b $1 $1)) (p_EMPTY:u $1)) (and:c (exists:ex $3:<a,e> (and:c (and:c (and:c (p_TYPE_w-9-novel:u $2) (p_EVENT_w-9-novel:u $2) (p_EVENT.ENTITY_arg0:b $2 $2)) (p_TYPEMOD_w-7-first:u $2)) (p_TYPE_w-8-harrypotter:u $3) (p_EVENT.ENTITY_l-compound:b $2 $3))) (p_EMPTY:u $2)) (p_EVENT.ENTITY_l-nmod.w-5-of:b $1 $2))) (p_EVENT.ENTITY_arg1:b $0 $1))))\", \"words\": [{\"index\": 1, \"head\": \"0\", \"word\": \"What\", \"dep\": \"root\", \"fpos\": \"PRON\", \"pos\": \"PRON\", \"lemma\": \"what\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 2, \"head\": \"1\", \"word\": \"is\", \"dep\": \"cop\", \"fpos\": \"VERB\", \"pos\": \"VERB\", \"lemma\": \"be\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 3, \"head\": \"4\", \"word\": \"the\", \"dep\": \"det\", \"fpos\": \"DET\", \"pos\": \"DET\", \"lemma\": \"the\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 4, \"head\": \"1\", \"word\": \"name\", \"dep\": \"nsubj\", \"fpos\": \"NOUN\", \"pos\": \"NOUN\", \"lemma\": \"name\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 5, \"head\": \"9\", \"word\": \"of\", \"dep\": \"case\", \"fpos\": \"ADP\", \"pos\": \"ADP\", \"lemma\": \"of\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 6, \"head\": \"9\", \"word\": \"the\", \"dep\": \"det\", \"fpos\": \"DET\", \"pos\": \"DET\", \"lemma\": \"the\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 7, \"head\": \"9\", \"word\": \"first\", \"dep\": \"amod\", \"fpos\": \"ADJ\", \"pos\": \"ADJ\", \"lemma\": \"first\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 8, \"head\": \"9\", \"word\": \"HarryPotter\", \"dep\": \"compound\", \"fpos\": \"PROPN\", \"pos\": \"PROPN\", \"lemma\": \"HarryPotter\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 9, \"head\": \"4\", \"word\": \"novel\", \"dep\": \"nmod\", \"fpos\": \"NOUN\", \"pos\": \"NOUN\", \"lemma\": \"novel\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 10, \"head\": \"1\", \"word\": \"?\", \"dep\": \"punct\", \"fpos\": \"PUNCT\", \"pos\": \"PUNCT\", \"sentEnd\": true, \"lemma\": \"?\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}], \"targetValue\": \"(list (description \\\"Harry Potter and the Philosopher's Stone\\\"))\", \"goldMid\": \"m.078ffw\", \"original\": \"what is the name of the first harry potter novel?\"}")
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

  @Test
  public void testGroundedGraphsWithMerge() throws IOException {
    List<JsonObject> jsonSentences = Lists.newArrayList();

    JsonObject sentence =
        jsonParser
            .parse(
                "{\"index\":\"1d86f34a22e77448b9e97bc1138c812b:1\",\"sentence\":\"what type of breast cancer did sheryl crow have?\",\"url\":\"http://www.freebase.com/view/en/sheryl_crow\",\"goldRelations\":[{\"relationLeft\":\"medicine.disease.notable_people_with_this_condition.2\",\"score\":1.0,\"relationRight\":\"medicine.disease.notable_people_with_this_condition.1\"}],\"targetValue\":\"(list (description Meningioma))\",\"goldMid\":\"m.06rgq\",\"entities\":[{\"name\":\"Sheryl Crow\",\"entity\":\"m.06rgq\",\"score\":656.828857,\"phrase\":\"sheryl crow\",\"id\":\"/en/cheryl_crow\",\"index\":6}],\"words\":[{\"category\":\"DET\",\"head\":1,\"end\":3,\"start\":0,\"dep\":\"det\",\"break_level\":3,\"pos\":\"WDT\",\"lemma\":\"what\",\"word\":\"What\"},{\"category\":\"NOUN\",\"head\":7,\"end\":8,\"start\":5,\"dep\":\"dobj\",\"break_level\":1,\"pos\":\"NN\",\"lemma\":\"type\",\"word\":\"type\"},{\"category\":\"ADP\",\"head\":1,\"end\":11,\"start\":10,\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"lemma\":\"of\",\"word\":\"of\"},{\"category\":\"NOUN\",\"head\":4,\"end\":18,\"start\":13,\"dep\":\"nn\",\"break_level\":1,\"pos\":\"NN\",\"lemma\":\"breast\",\"word\":\"breast\"},{\"category\":\"NOUN\",\"head\":2,\"end\":25,\"start\":20,\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NN\",\"lemma\":\"cancer\",\"word\":\"cancer\"},{\"category\":\"VERB\",\"head\":7,\"end\":29,\"lemma\":\"do\",\"dep\":\"aux\",\"break_level\":1,\"pos\":\"VBD\",\"start\":27,\"word\":\"did\"},{\"category\":\"NOUN\",\"head\":7,\"end\":41,\"start\":38,\"dep\":\"nsubj\",\"break_level\":1,\"pos\":\"NNP\",\"lemma\":\"Sheryl_Crow\",\"word\":\"Sheryl_Crow\"},{\"category\":\"VERB\",\"end\":46,\"start\":43,\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"VB\",\"lemma\":\"have\",\"word\":\"have\"},{\"category\":\".\",\"head\":7,\"end\":48,\"start\":48,\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"lemma\":\"?\",\"word\":\"?\"}],\"dependency_lambda\":[[\"type.arg_1(1:e , 1:x)\",\"type(1:s , 1:x)\",\"arg_1(6:e , 6:m.06rgq)\",\"cancer.arg_1(4:e , 4:x)\",\"have.arg_2(7:e , 1:x)\",\"cancer(4:s , 4:x)\",\"type.of.arg_2(1:e , 4:x)\",\"QUESTION(1:x)\",\"cancer.breast(3:s , 4:x)\",\"have.arg_1(7:e , 6:m.06rgq)\"]]}")
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
          if (ungroundedGraph.getQuestionNode().iterator().hasNext()) {
            List<LexicalGraph> groundedGraphs =
                graphCreator.createGroundedGraph(ungroundedGraph, null, 1000,
                    10000, true, true, false, false, false, false, true, false);
            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());

            for (LexicalGraph groundedGraph : groundedGraphs) {
              System.out.println(groundedGraph.getParallelGraph());
              System.out.println(groundedGraph);
              System.out.println("Graph Query: "
                  + GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                      schema, 200));
              System.out.println("Features: " + groundedGraph.getFeatures());
            }

            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());
            System.out.println();
          }
        }
      }
    }
  }

  @Test
  public void testGroundedGraphsWithMergeAndExpand() throws IOException {
    List<JsonObject> jsonSentences = Lists.newArrayList();

    JsonObject sentence =
        jsonParser
            .parse(
                "{\"sentence\":\"what is the name of the founder of Stanford in California?\",\"entities\": [{\"index\": 8, \"score\": 0.56333, \"entity\": \"m.06pwq\"}, {\"index\": 10, \"score\": 0.999988, \"entity\": \"m.01n7q\"}],\"words\":[{\"word\":\"what\",\"lemma\":\"what\",\"pos\":\"PRON\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":1},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":1,\"dep\":\"cop\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"det\"},{\"word\":\"name\",\"lemma\":\"name\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":4,\"head\":1,\"dep\":\"nsubj\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":7,\"dep\":\"case\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":6,\"head\":7,\"dep\":\"det\"},{\"word\":\"founder\",\"lemma\":\"founder\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":7,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":8,\"head\":9,\"dep\":\"case\"},{\"word\":\"Stanford\",\"lemma\":\"stanford\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":9,\"head\":7,\"dep\":\"nmod\"},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":10,\"head\":9,\"dep\":\"case\"},{\"word\":\"California\",\"lemma\":\"california\",\"pos\":\"PROPN\",\"ner\":\"LOCATION\",\"index\":11,\"head\":4,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":12,\"head\":1,\"dep\":\"punct\",\"sentEnd\":true}],\"dependency_lambda\":[[\"QUESTION(0:x)\",\"name.arg0(3:e , 3:x)\",\"arg0(8:e , 8:m.stanford)\",\"arg0(10:e , 10:m.california)\",\"founder.arg0(6:e , 6:x)\",\"founder.nmod.of(6:e , 8:m.stanford)\",\"what.arg1(0:e , 3:x)\",\"name(3:s , 3:x)\",\"name.nmod.of(3:e , 6:x)\",\"what(0:s , 0:x)\",\"founder(6:s , 6:x)\",\"nmod.in(8:e , 10:m.california)\",\"what.arg0(0:e , 0:x)\"]]}")
            .getAsJsonObject();
    jsonSentences.add(sentence);
    
    sentence =
        jsonParser
            .parse(
                "{\"sentence\": \"who made a significant influence on apostle paul?\", \"entities\": [{\"index\": 6, \"end\": 6, \"name\": \"Paul the Apostle\", \"entity\": \"m.060nc\", \"start\": 6, \"score\": 49.377895077274999, \"phrase\": \"apostle paul\"}], \"words\": [{\"index\": 1, \"head\": \"2\", \"word\": \"who\", \"dep\": \"nsubj\", \"fpos\": \"PRON\", \"pos\": \"PRON\", \"lemma\": \"who\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 2, \"head\": \"0\", \"word\": \"made\", \"dep\": \"root\", \"fpos\": \"VERB\", \"pos\": \"VERB\", \"lemma\": \"make\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 3, \"head\": \"5\", \"word\": \"a\", \"dep\": \"det\", \"fpos\": \"DET\", \"pos\": \"DET\", \"lemma\": \"a\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 4, \"head\": \"5\", \"word\": \"significant\", \"dep\": \"amod\", \"fpos\": \"ADJ\", \"pos\": \"ADJ\", \"lemma\": \"significant\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 5, \"head\": \"2\", \"word\": \"influence\", \"dep\": \"dobj\", \"fpos\": \"NOUN\", \"pos\": \"NOUN\", \"lemma\": \"influence\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 6, \"head\": \"7\", \"word\": \"on\", \"dep\": \"case\", \"fpos\": \"ADP\", \"pos\": \"ADP\", \"lemma\": \"on\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 7, \"head\": \"5\", \"word\": \"apostlepaul\", \"dep\": \"nmod\", \"fpos\": \"PROPN\", \"pos\": \"PROPN\", \"lemma\": \"ApostlePaul\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 8, \"head\": \"2\", \"word\": \"?\", \"dep\": \"punct\", \"fpos\": \"PUNCT\", \"pos\": \"PUNCT\", \"sentEnd\": true, \"lemma\": \"?\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}], \"dependency_lambda\": [[\"influence(4:s , 4:x)\", \"influence.nmod.on(4:e , 6:m.apostlepaul)\", \"QUESTION(0:x)\", \"make.arg1(1:e , 0:x)\", \"who(0:s , 0:x)\", \"influence.arg0(4:e , 4:x)\", \"significant(3:s , 4:x)\", \"make.arg2(1:e , 4:x)\"]]}")
            .getAsJsonObject();
    jsonSentences.add(sentence);
    

    for (JsonObject jsonSentence : jsonSentences) {
      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(jsonSentence,
              SentenceKeys.DEPENDENCY_LAMBDA, 1, logger);

      System.out.println("# Ungrounded Graphs");
      Set<LexicalGraph> totalUGraphs = new HashSet<>();
      if (graphs.size() > 0) {
        for (LexicalGraph ungroundedGraph : graphs) {
          System.out.println(ungroundedGraph);
          if (ungroundedGraph.getQuestionNode().iterator().hasNext()) {
            List<LexicalGraph> groundedGraphs =
                graphCreator.createGroundedGraph(ungroundedGraph, null, 1000,
                    10000, true, true, false, false, false, false, true, false);
            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());

            for (LexicalGraph groundedGraph : groundedGraphs) {
              System.out.println(groundedGraph.getParallelGraph());
              totalUGraphs.add(groundedGraph.getParallelGraph());
              System.out.println(groundedGraph);
              System.out.println("Graph Query: "
                  + GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                      schema, 200));
              System.out.println("Features: " + groundedGraph.getFeatures());
            }

            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());
            System.out.println();
          }
          System.out.println(totalUGraphs);
        }
      }
    }
  }

  

  @Test
  public void testUngroundedFromDependency() throws IOException {
    JsonObject sentence =
        jsonParser
            .parse(
                "{\"index\":\"7c862e4910ddf5154495e18a1c38354a:1\",\"domain\":[\"people\"],\"sentence\":\"what did harriet beecher stowe do as an abolitionist?\",\"url\":\"http://www.freebase.com/view/en/harriet_beecher_stowe\",\"goldRelations\":[{\"relationLeft\":\"people.person.profession.1\",\"score\":1.0,\"relationRight\":\"people.person.profession.2\"}],\"targetValue\":\"(list (description Novelist) (description Writer) (description Author))\",\"goldMid\":\"m.015v00\",\"entities\":[{\"name\":\"Harriet Beecher Stowe\",\"entity\":\"m.015v00\",\"score\":746.807495,\"phrase\":\"harriet beecher stowe\",\"id\":\"/en/harriet_beecher_stowe\",\"index\":2}],\"words\":[{\"category\":\"PRON\",\"head\":3,\"end\":3,\"lemma\":\"what\",\"dep\":\"dobj\",\"break_level\":3,\"pos\":\"WP\",\"start\":0,\"word\":\"What\"},{\"category\":\"VERB\",\"head\":3,\"end\":7,\"start\":5,\"dep\":\"aux\",\"break_level\":1,\"pos\":\"VBD\",\"lemma\":\"do\",\"word\":\"did\"},{\"category\":\"NOUN\",\"head\":3,\"end\":29,\"lemma\":\"Harriet_Beecher_Stowe\",\"dep\":\"nsubj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":25,\"word\":\"Harriet_Beecher_Stowe\"},{\"category\":\"VERB\",\"end\":32,\"lemma\":\"do\",\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"VB\",\"start\":31,\"word\":\"do\"},{\"category\":\"ADP\",\"head\":3,\"end\":35,\"lemma\":\"as\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":34,\"word\":\"as\"},{\"category\":\"DET\",\"head\":6,\"end\":38,\"lemma\":\"an\",\"dep\":\"det\",\"break_level\":1,\"pos\":\"DT\",\"start\":37,\"word\":\"an\"},{\"category\":\"NOUN\",\"head\":4,\"end\":51,\"lemma\":\"abolitionist\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NN\",\"start\":40,\"word\":\"abolitionist\"},{\"category\":\".\",\"head\":3,\"end\":53,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":53,\"word\":\"?\"}],\"dependency_lambda\":[[\"abolitionist(6:s , 6:x)\",\"do.arg_1(3:e , 2:m.015v00)\",\"QUESTION(0:x)\",\"do.as.arg_2(3:e , 6:x)\",\"do.arg_2(3:e , 0:x)\"]]}")
            .getAsJsonObject();

    List<JsonObject> jsonSentences = Lists.newArrayList();
    jsonSentences.add(sentence);

    for (JsonObject jsonSentence : jsonSentences) {
      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(jsonSentence,
              SentenceKeys.DEPENDENCY_QUESTION_GRAPH, 1, logger);

      System.out.println("# Ungrounded Graphs");
      if (graphs.size() > 0) {
        for (LexicalGraph ungroundedGraph : graphs) {
          System.out.println("Ungrounded Graph: ");
          System.out.println(ungroundedGraph);
        }
      }
    }
  }

  
  @Test
  public void testHyperExpand() throws IOException {
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    graphCreator = new GroundedGraphs(schema, kb, groundedLexicon,
        normalCcgAutoLexicon, questionCcgAutoLexicon,
        relationLexicalIdentifiers, relationTypingIdentifiers,
        new StructuredPercepton(), 1, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, false, false, false,
        false, false, false, true, 10.0, 1.0, 0.0, 0.0);
    JsonObject sentence =
        jsonParser
            .parse(
                "{\"function\":\"none\",\"commonness\":-18.42626988711012,\"sparql_query\":\"PREFIX rdf: \\u003chttp://www.w3.org/1999/02/22-rdf-syntax-ns#\\u003e PREFIX rdfs: \\u003chttp://www.w3.org/2000/01/rdf-schema#\\u003e PREFIX : \\u003chttp://rdf.freebase.com/ns/\\u003e \\nSELECT (?x0 AS ?value) WHERE {\\nSELECT DISTINCT ?x0  WHERE { \\n?x0 :type.object.type :base.peleton.cycling_team_staff . \\nVALUES ?x1 { :en.team_csc } \\n?x1 :base.peleton.cycling_team_professional.general_manager ?x0 . \\nFILTER ( ?x0 !\\u003d ?x1  )\\n}\\n}\",\"graph_query\":{\"nodes\":[{\"function\":\"none\",\"question_node\":1,\"friendly_name\":\"Cycling Team Staff\",\"nid\":0,\"class\":\"base.peleton.cycling_team_staff\",\"node_type\":\"class\",\"id\":\"base.peleton.cycling_team_staff\"},{\"function\":\"none\",\"question_node\":0,\"friendly_name\":\"Team Saxo Bank-SunGard\",\"nid\":1,\"class\":\"base.peleton.cycling_team_professional\",\"node_type\":\"entity\",\"id\":\"en.team_csc\"}],\"edges\":[{\"start\":1,\"end\":0,\"friendly_name\":\"General Manager\",\"relation\":\"base.peleton.cycling_team_professional.general_manager\"}]},\"sentence\":\"team saxo bank-sungard\\u0027s general manager is who?\",\"qid\":24000300,\"num_node\":2,\"num_edge\":1,\"answerF1\":[\"Bjarne Riis\"],\"goldMids\":[\"m.06s1mb\"],\"id\":24000300,\"words\":[{\"pos\":\"PROPN\",\"word\":\"TeamSaxo\",\"lemma\":\"TeamSaxo\"},{\"pos\":\"PROPN\",\"word\":\"Bank-sungard\",\"lemma\":\"Bank-sungard\"},{\"word\":\"\\u0027s\",\"lemma\":\"\\u0027s\",\"pos\":\"PART\"},{\"word\":\"general\",\"lemma\":\"general\",\"pos\":\"ADJ\"},{\"word\":\"manager\",\"lemma\":\"manager\",\"pos\":\"NOUN\"},{\"word\":\"is\",\"lemma\":\"be\",\"pos\":\"VERB\"},{\"word\":\"who\",\"lemma\":\"who\",\"pos\":\"PRON\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"sentEnd\":true}],\"index\":\"8603baca89e44aeb56e2ef264d77b6f5:1\",\"entities\":[{\"entity\":\"m.09rsrj6\",\"score\":24.111584164826308,\"phrase\":\"team saxo\",\"name\":\"2010 Team Saxo Bank season\",\"index\":0},{\"entity\":\"m.06s1mb\",\"score\":23.674190536504206,\"phrase\":\"bank-sungard\",\"name\":\"Team Saxo Bank-SunGard\",\"index\":1}],\"synPars\":[{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) fa 0 2\\u003e (\\u003cT (S[wq]/(S[dcl]\\\\NP))/N ba 1 2\\u003e (\\u003cT S[X]/(S[X]\\\\NP) tr 0 1\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cT N fa 1 2\\u003e (\\u003cL N/N TeamSaxo TeamSaxo NNP O O N/N\\u003e) (\\u003cL N Bank-sungard Bank-sungard NNP O O N\\u003e) ) ) ) (\\u003cL ((S[wq]/(S[dcl]\\\\NP))/N)\\\\(S[wq]/(S[dcl]\\\\NP)) \\u0027s \\u0027s POS O O ((S[wq]/(S[dcl]\\\\NP))/N)\\\\(S[wq]/(S[dcl]\\\\NP))\\u003e) ) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N general general JJ O O N/N\\u003e) (\\u003cL N manager manager NN O O N\\u003e) ) ) (\\u003cT S[dcl]\\\\NP fa 0 2\\u003e (\\u003cL (S[dcl]\\\\NP)/(S[pss]\\\\NP) is be VBZ O O (S[dcl]\\\\NP)/(S[pss]\\\\NP)\\u003e) (\\u003cT S[pss]\\\\NP rp 0 2\\u003e (\\u003cL S[pss]\\\\NP who who WP O O S[pss]\\\\NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) ) \",\"score\":1.0}]}")
            .getAsJsonObject();

    List<JsonObject> jsonSentences = Lists.newArrayList();
    jsonSentences.add(sentence);

    for (JsonObject jsonSentence : jsonSentences) {
      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(jsonSentence,
              SentenceKeys.CCG_PARSES, 1, logger);

      System.out.println("# Ungrounded Graphs");
      if (graphs.size() > 0) {
        for (LexicalGraph ungroundedGraph : graphs) {
          System.out.println("Ungrounded Graph: ");
          System.out.println(ungroundedGraph);
        }
      }
    }
  }
  
  @Test
  public void testHyperExpandWithCount() throws IOException {
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    graphCreator = new GroundedGraphs(schema, kb, groundedLexicon,
        normalCcgAutoLexicon, questionCcgAutoLexicon,
        relationLexicalIdentifiers, relationTypingIdentifiers,
        new StructuredPercepton(), 1, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, true, true, true, true,
        true, true, true, true, true, true, true, true, false, false, false,
        false, false, false, true, 10.0, 1.0, 0.0, 0.0);
    JsonObject sentence =
        jsonParser
            .parse(
                "{\"entities\": [{\"index\": 0, \"end\": 0, \"name\": \"Portable document format reference manual\", \"entity\": \"m.050_gqz\", \"start\": 0, \"score\": 22.863161010229465, \"phrase\": \"portable document format\"}], \"words\": [{\"index\": 1, \"head\": \"2\", \"word\": \"portabledocumentformat\", \"dep\": \"nsubj\", \"fpos\": \"PROPN\", \"pos\": \"PROPN\", \"lemma\": \"PortableDocumentFormat\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 2, \"head\": \"0\", \"word\": \"supports\", \"dep\": \"root\", \"fpos\": \"VERB\", \"pos\": \"VERB\", \"lemma\": \"support\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 3, \"head\": \"4\", \"word\": \"how\", \"dep\": \"advmod\", \"fpos\": \"ADV\", \"pos\": \"ADV\", \"lemma\": \"how\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 4, \"head\": \"6\", \"word\": \"many\", \"dep\": \"amod\", \"fpos\": \"ADJ\", \"pos\": \"ADJ\", \"lemma\": \"many\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 5, \"head\": \"6\", \"word\": \"computing\", \"dep\": \"compound\", \"fpos\": \"NOUN\", \"pos\": \"NOUN\", \"lemma\": \"compute\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 6, \"head\": \"2\", \"word\": \"platforms\", \"dep\": \"dobj\", \"fpos\": \"NOUN\", \"pos\": \"NOUN\", \"lemma\": \"platform\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 7, \"head\": \"2\", \"word\": \"?\", \"dep\": \"punct\", \"fpos\": \"PUNCT\", \"pos\": \"PUNCT\", \"sentEnd\": true, \"lemma\": \"?\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}], \"dependency_lambda\": [[\"COUNT(5:x , 2:x)\", \"computing.platforms(4:s , 5:x)\", \"how(2:s , 2:x)\", \"QUESTION(2:x)\", \"support.arg2(1:e , 5:x)\", \"many(3:s , 2:x)\", \"platforms(5:s , 5:x)\"], [\"many(3:s , 5:x)\", \"support.arg1(1:e , 0:m.portabledocumentformat)\", \"how(2:s , 5:x)\", \"computing.platforms(4:s , 5:x)\", \"QUESTION(5:x)\", \"support.arg2(1:e , 5:x)\", \"platforms(5:s , 5:x)\"]]}")
            .getAsJsonObject();

    List<JsonObject> jsonSentences = Lists.newArrayList();
    jsonSentences.add(sentence);

    for (JsonObject jsonSentence : jsonSentences) {
      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(jsonSentence,
              SentenceKeys.DEPENDENCY_LAMBDA, 1, logger);

      System.out.println("# Ungrounded Graphs");
      if (graphs.size() > 0) {
        for (LexicalGraph ungroundedGraph : graphs) {
          System.out.println("Ungrounded Graph: ");
          System.out.println(ungroundedGraph);
        }
      }
    }
  }

  @Test
  public void testUngroundedFromDependencyUD() throws IOException {
    JsonObject sentence =
        jsonParser
            .parse(
                "{\"url\":\"http://www.freebase.com/view/en/benjamin_franklin\",\"targetValue\":\"(list (description Boston))\",\"goldMid\":\"m.019fz\",\"original\":\"where was ben franklin born?\",\"sentence\":\"where was ben franklin born?\",\"index\":\"b6410085a1f26218e64f62d534203d4c:1\",\"goldRelations\":[{\"relationLeft\":\"people.person.place_of_birth.1\",\"relationRight\":\"people.person.place_of_birth.2\",\"score\":1.0},{\"relationLeft\":\"people.place_lived.person\",\"relationRight\":\"people.place_lived.location\",\"score\":0.4}],\"entities\":[{\"entity\":\"m.019fz\",\"score\":59.339392289757676,\"phrase\":\"ben franklin\",\"name\":\"Benjamin Franklin\",\"id\":\"/en/benjamin_franklin\",\"index\":2}],\"words\":[{\"word\":\"Where\",\"lemma\":\"where\",\"pos\":\"ADV\",\"index\":1,\"head\":3,\"dep\":\"advmod\"},{\"word\":\"was\",\"lemma\":\"be\",\"pos\":\"VERB\",\"index\":2,\"head\":3,\"dep\":\"cop\"},{\"word\":\"Ben_Franklin\",\"lemma\":\"ben_franklin\",\"pos\":\"PROPN\",\"dep\":\"root\",\"head\":0,\"index\":3},{\"word\":\"born\",\"lemma\":\"bear\",\"pos\":\"VERB\",\"index\":4,\"head\":3,\"dep\":\"acl\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"sentEnd\":true,\"index\":5,\"head\":3,\"dep\":\"punct\"}]}")
            .getAsJsonObject();

    List<JsonObject> jsonSentences = Lists.newArrayList();
    jsonSentences.add(sentence);

    for (JsonObject jsonSentence : jsonSentences) {
      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(jsonSentence,
              SentenceKeys.DEPENDENCY_QUESTION_GRAPH, 1, logger);

      System.out.println("# Ungrounded Graphs");
      if (graphs.size() > 0) {
        for (LexicalGraph ungroundedGraph : graphs) {
          System.out.println("Ungrounded Graph: ");
          System.out.println(ungroundedGraph);
        }
      }
    }
  }
  
  
  @Test
  public void testBackoffGroundedGraphsWithMerge() throws IOException {
    List<JsonObject> jsonSentences = Lists.newArrayList();

    JsonObject sentence =
        jsonParser
            .parse(
                "{\"index\":\"095596d7bf516419aca164552319522e:1\",\"domain\":[\"film\"],\"sentence\":\"who played dorothy in the wizard of oz movie?\",\"url\":\"http://www.freebase.com/view/en/dorothy_gale\",\"goldRelations\":[{\"relationLeft\":\"film.performance.character\",\"score\":0.16666666666666669,\"relationRight\":\"film.performance.actor\"}],\"targetValue\":\"(list (description \\\"Judy Garland\\\"))\",\"goldMid\":\"m.020hj1\",\"entities\":[{\"name\":\"Dorothy Gale\",\"entity\":\"m.020hj1\",\"score\":84.735451,\"phrase\":\"dorothy in the wizard\",\"id\":\"/en/dorothy_gale\",\"index\":2},{\"name\":\"The Wizard of Oz\",\"entity\":\"m.02q52q\",\"score\":390.615906,\"phrase\":\"oz movie\",\"id\":\"/en/the_wizard_of_oz\",\"index\":4}],\"words\":[{\"category\":\"PRON\",\"head\":1,\"end\":2,\"lemma\":\"who\",\"dep\":\"nsubj\",\"break_level\":3,\"pos\":\"WP\",\"start\":0,\"word\":\"Who\"},{\"category\":\"VERB\",\"end\":9,\"start\":4,\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"VBD\",\"lemma\":\"play\",\"word\":\"played\"},{\"category\":\"NOUN\",\"head\":1,\"end\":17,\"lemma\":\"Dorothy_In_The_Wizard\",\"dep\":\"dobj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":11,\"word\":\"Dorothy_In_The_Wizard\"},{\"category\":\"ADP\",\"head\":2,\"end\":34,\"lemma\":\"of\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":33,\"word\":\"of\"},{\"category\":\"NOUN\",\"head\":3,\"end\":43,\"lemma\":\"Oz_Movie\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":39,\"word\":\"Oz_Movie\"},{\"category\":\".\",\"head\":1,\"end\":45,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":45,\"word\":\"?\"}],\"dependency_lambda\":[[\"play.arg_2(1:e , 2:m.020hj1)\",\"of.arg_2(2:e , 4:m.02q52q)\",\"QUESTION(0:x)\",\"play.arg_1(1:e , 0:x)\",\"who(0:s , 0:x)\"]]}")
            .getAsJsonObject(); // jsonSentences.add(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"index\":\"40753f7151fa1c0aa520999f3b3a254c:1\",\"sentence\":\"what time do the polls open in indiana 2012?\",\"url\":\"http://www.freebase.com/view/en/indiana\",\"goldRelations\":[{\"relationLeft\":\"time.time_zone.locations_in_this_time_zone.2\",\"score\":0.4,\"relationRight\":\"time.time_zone.locations_in_this_time_zone.1\"}],\"targetValue\":\"(list (description UTC−06:00))\",\"goldMid\":\"m.03v1s\",\"entities\":[{\"name\":\"Indiana\",\"entity\":\"m.03v1s\",\"score\":139.72702,\"phrase\":\"indiana\",\"id\":\"/en/indiana\",\"index\":7},{\"phrase\":\"2012\",\"entity\":\"type.datetime\",\"index\":8}],\"words\":[{\"category\":\"DET\",\"head\":1,\"end\":3,\"lemma\":\"what\",\"dep\":\"det\",\"break_level\":3,\"pos\":\"WDT\",\"start\":0,\"word\":\"What\"},{\"category\":\"NOUN\",\"head\":5,\"end\":8,\"lemma\":\"time\",\"dep\":\"dep\",\"break_level\":1,\"pos\":\"NN\",\"start\":5,\"word\":\"time\"},{\"category\":\"VERB\",\"head\":5,\"end\":11,\"lemma\":\"do\",\"dep\":\"aux\",\"break_level\":1,\"pos\":\"VBP\",\"start\":10,\"word\":\"do\"},{\"category\":\"DET\",\"head\":4,\"end\":15,\"lemma\":\"the\",\"dep\":\"det\",\"break_level\":1,\"pos\":\"DT\",\"start\":13,\"word\":\"the\"},{\"category\":\"NOUN\",\"head\":5,\"end\":21,\"start\":17,\"dep\":\"nsubj\",\"break_level\":1,\"pos\":\"NNS\",\"lemma\":\"poll\",\"word\":\"polls\"},{\"category\":\"ADJ\",\"end\":26,\"lemma\":\"open\",\"dep\":\"ROOT\",\"break_level\":1,\"pos\":\"JJ\",\"start\":23,\"word\":\"open\"},{\"category\":\"ADP\",\"head\":5,\"end\":29,\"lemma\":\"in\",\"dep\":\"prep\",\"break_level\":1,\"pos\":\"IN\",\"start\":28,\"word\":\"in\"},{\"category\":\"NOUN\",\"head\":6,\"end\":37,\"lemma\":\"Indiana\",\"dep\":\"pobj\",\"break_level\":1,\"pos\":\"NNP\",\"start\":31,\"word\":\"Indiana\"},{\"category\":\"NUM\",\"head\":7,\"end\":42,\"lemma\":\"2012\",\"dep\":\"num\",\"break_level\":1,\"pos\":\"CD\",\"start\":39,\"word\":\"2012\"},{\"category\":\".\",\"head\":5,\"end\":44,\"lemma\":\"?\",\"dep\":\"p\",\"break_level\":1,\"pos\":\".\",\"start\":44,\"word\":\"?\"}],\"dependency_lambda\":[[\"poll(4:s , 4:x)\",\"time.open(5:s , 1:x)\",\"time.in.arg_2(1:e , 7:m.03v1s)\",\"2012(8:s , 7:m.03v1s)\",\"time.arg_1(1:e , 4:x)\",\"QUESTION(1:x)\",\"UNIQUE(4:x)\",\"time(1:s , 1:x)\"]]}")
            .getAsJsonObject();
    
    sentence =
        jsonParser
            .parse(
                "{\"index\": \"13a21608e95071f42977991f47e6dcc2:1\", \"sentence\": \"on which river is paris?\", \"url\": \"http://www.freebase.com/view/en/paris\", \"dependency_lambda\": [[]], \"deplambda_oblique_tree\": \"(l-punct (l-cop w-5-paris (l-nsubj (l-nmod w-4-is (l-case w-2-which w-1-on)) w-3-river)) w-6-?)\", \"goldRelations\": [{\"relationLeft\": \"geography.river.cities.2\", \"score\": 1.0, \"relationRight\": \"geography.river.cities.1\"}, {\"relationLeft\": \"location.location.partially_contains.1\", \"score\": 0.66666666666666663, \"relationRight\": \"location.location.partially_contains.2\"}], \"entities\": [{\"index\": 4, \"end\": 4, \"name\": \"Paris\", \"entity\": \"m.05qtj\", \"start\": 4, \"score\": 49.91848436595545, \"phrase\": \"paris\", \"id\": \"/en/paris\"}], \"deplambda_expression\": \"(lambda $0:<a,e> (and:c (p_TYPE_w-5-paris:u $0) (p_EVENT_w-5-paris:u $0) (p_EVENT.ENTITY_arg0:b $0 $0)))\", \"words\": [{\"index\": 1, \"head\": \"2\", \"word\": \"on\", \"dep\": \"case\", \"fpos\": \"ADP\", \"pos\": \"ADP\", \"lemma\": \"on\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 2, \"head\": \"4\", \"word\": \"which\", \"dep\": \"nmod\", \"fpos\": \"PRON\", \"pos\": \"PRON\", \"lemma\": \"which\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 3, \"head\": \"4\", \"word\": \"river\", \"dep\": \"nsubj\", \"fpos\": \"NOUN\", \"pos\": \"NOUN\", \"lemma\": \"river\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 4, \"head\": \"5\", \"word\": \"is\", \"dep\": \"cop\", \"fpos\": \"VERB\", \"pos\": \"VERB\", \"lemma\": \"be\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 5, \"head\": \"0\", \"word\": \"paris\", \"dep\": \"root\", \"fpos\": \"PROPN\", \"pos\": \"PROPN\", \"lemma\": \"Paris\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}, {\"index\": 6, \"head\": \"5\", \"word\": \"?\", \"dep\": \"punct\", \"fpos\": \"PUNCT\", \"pos\": \"PUNCT\", \"sentEnd\": true, \"lemma\": \"?\", \"phead\": \"_\", \"feats\": \"_\", \"pdep\": \"_\"}], \"targetValue\": \"(list (description Seine))\", \"goldMid\": \"m.05qtj\", \"original\": \"on which river is paris?\"}")
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
          if (ungroundedGraph.getQuestionNode().iterator().hasNext()) {
            List<LexicalGraph> groundedGraphs =
                graphCreator.createGroundedGraph(ungroundedGraph, null, 1000,
                    10000, true, true, false, false, false, false, true, false);
            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());

            for (LexicalGraph groundedGraph : groundedGraphs) {
              System.out.println(groundedGraph.getParallelGraph());
              System.out.println(groundedGraph);
              System.out.println("Graph Query: "
                  + GraphToSparqlConverter.convertGroundedGraph(groundedGraph,
                      schema, 200));
              System.out.println("Features: " + groundedGraph.getFeatures());
            }

            System.out.println("# Total number of Grounded Graphs: "
                + groundedGraphs.size());
            System.out.println();
          }
        }
      }
    }
  }
}
