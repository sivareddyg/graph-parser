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

public class CreateGroundedGraphsTest {

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
        new KnowledgeBaseOnline("bravas", "http://bravas:8890/sparql", "dba",
            "dba", 0, schema);
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
        "{\"url\":\"http://www.freebase.com/view/en/united_states\",\"targetValue\":\"(list (description \\\"United States of America\\\"))\",\"sentence\":\"what each fold of the us flag means?\",\"words\":[{\"lemma\":\"what\",\"ner\":\"O\",\"word\":\"What\",\"pos\":\"WDT\"},{\"lemma\":\"each\",\"ner\":\"O\",\"word\":\"each\",\"pos\":\"DT\"},{\"lemma\":\"fold\",\"ner\":\"O\",\"word\":\"fold\",\"pos\":\"NN\"},{\"lemma\":\"of\",\"ner\":\"O\",\"word\":\"of\",\"pos\":\"IN\"},{\"lemma\":\"the\",\"ner\":\"O\",\"word\":\"the\",\"pos\":\"DT\"},{\"ner\":\"LOCATION\",\"word\":\"Us\",\"pos\":\"NNP\"},{\"lemma\":\"flag\",\"ner\":\"O\",\"word\":\"flag\",\"pos\":\"NN\"},{\"lemma\":\"mean\",\"ner\":\"O\",\"word\":\"means\",\"pos\":\"VBZ\"},{\"lemma\":\"?\",\"ner\":\"O\",\"word\":\"?\",\"pos\":\".\"}],\"entities\":[{\"index\":5,\"entity\":\"m.09c7w0\"}],\"synPars\":[{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) fa 0 2\\u003e (\\u003cL (S[wq]/(S[dcl]\\\\NP))/N What What WDT O O (S[wq]/(S[dcl]\\\\NP))/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N each each DT O O N/N\\u003e) (\\u003cT N ba 0 2\\u003e (\\u003cL N fold fold NN O O N\\u003e) (\\u003cT N\\\\N fa 0 2\\u003e (\\u003cL (N\\\\N)/NP of of IN O O (N\\\\N)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) ) (\\u003cT S[dcl]\\\\NP rp 0 2\\u003e (\\u003cL S[dcl]\\\\NP means mean VBZ O O S[dcl]\\\\NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) fa 0 2\\u003e (\\u003cL (S[wq]/(S[dcl]\\\\NP))/N What What WDT O O (S[wq]/(S[dcl]\\\\NP))/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N each each DT O O N/N\\u003e) (\\u003cL N fold fold NN O O N\\u003e) ) ) (\\u003cT S[dcl]\\\\NP rp 0 2\\u003e (\\u003cT S[dcl]\\\\NP fa 0 2\\u003e (\\u003cT (S[dcl]\\\\NP)/N bx 0 2\\u003e (\\u003cT ((S[dcl]\\\\NP)/NP)/N fc 0 2\\u003e (\\u003cL ((S[dcl]\\\\NP)/NP)/NP of of IN O O ((S[dcl]\\\\NP)/NP)/NP\\u003e) (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) ) (\\u003cT (S[X]\\\\NP)\\\\((S[X]\\\\NP)/NP) tr 0 1\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) (\\u003cL N means mean VBZ O O N\\u003e) ) (\\u003cL . ? ? . O O .\\u003e) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) fa 0 2\\u003e (\\u003cL (S[wq]/(S[dcl]\\\\NP))/N What What WDT O O (S[wq]/(S[dcl]\\\\NP))/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N each each DT O O N/N\\u003e) (\\u003cL N fold fold NN O O N\\u003e) ) ) (\\u003cT S[dcl]\\\\NP rp 0 2\\u003e (\\u003cT S[dcl]\\\\NP fa 0 2\\u003e (\\u003cT (S[dcl]\\\\NP)/N bx 0 2\\u003e (\\u003cT ((S[dcl]\\\\NP)/NP)/N fc 0 2\\u003e (\\u003cL ((S[dcl]\\\\NP)/NP)/NP of of IN O O ((S[dcl]\\\\NP)/NP)/NP\\u003e) (\\u003cT NP[nb]/N fc 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) ) ) (\\u003cT (S[X]\\\\NP)\\\\((S[X]\\\\NP)/NP) tr 0 1\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cL N flag flag NN O O N\\u003e) ) ) ) (\\u003cL N means mean VBZ O O N\\u003e) ) (\\u003cL . ? ? . O O .\\u003e) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) fa 0 2\\u003e (\\u003cL (S[wq]/(S[dcl]\\\\NP))/N What What WDT O O (S[wq]/(S[dcl]\\\\NP))/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N each each DT O O N/N\\u003e) (\\u003cL N fold fold NN O O N\\u003e) ) ) (\\u003cT S[dcl]\\\\NP fa 0 2\\u003e (\\u003cT (S[dcl]\\\\NP)/NP fa 0 2\\u003e (\\u003cL ((S[dcl]\\\\NP)/NP)/NP of of IN O O ((S[dcl]\\\\NP)/NP)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) (\\u003cT NP rp 0 2\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cL N means mean VBZ O O N\\u003e) ) (\\u003cL . ? ? . O O .\\u003e) ) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[qem] fa 0 2\\u003e (\\u003cL S[qem]/(S[dcl]/NP) What What WDT O O S[qem]/(S[dcl]/NP)\\u003e) (\\u003cT S[dcl]/NP fc 1 2\\u003e (\\u003cT S[X]/(S[X]\\\\NP) tr 0 1\\u003e (\\u003cT NP ba 0 2\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cT N fa 1 2\\u003e (\\u003cL N/N each each DT O O N/N\\u003e) (\\u003cL N fold fold NN O O N\\u003e) ) ) (\\u003cT NP\\\\NP fa 0 2\\u003e (\\u003cL (NP\\\\NP)/NP of of IN O O (NP\\\\NP)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) (\\u003cT (S[dcl]\\\\NP)/NP rp 0 2\\u003e (\\u003cL (S[dcl]\\\\NP)/NP means mean VBZ O O (S[dcl]\\\\NP)/NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) fa 0 2\\u003e (\\u003cT (S[wq]/(S[dcl]\\\\NP))/N fa 0 2\\u003e (\\u003cL ((S[wq]/(S[dcl]\\\\NP))/N)/(NP/N) What What WDT O O ((S[wq]/(S[dcl]\\\\NP))/N)/(NP/N)\\u003e) (\\u003cL NP[nb]/N each each DT O O NP[nb]/N\\u003e) ) (\\u003cT N ba 0 2\\u003e (\\u003cL N fold fold NN O O N\\u003e) (\\u003cT N\\\\N fa 0 2\\u003e (\\u003cL (N\\\\N)/NP of of IN O O (N\\\\N)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) (\\u003cT S[dcl]\\\\NP rp 0 2\\u003e (\\u003cL S[dcl]\\\\NP means mean VBZ O O S[dcl]\\\\NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[q] fa 1 2\\u003e (\\u003cL S/S What What WDT O O S/S\\u003e) (\\u003cT S[q] fa 0 2\\u003e (\\u003cT S[q]/NP fa 0 2\\u003e (\\u003cL (S[q]/NP)/NP each each DT O O (S[q]/NP)/NP\\u003e) (\\u003cT NP ba 0 2\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cL N fold fold NN O O N\\u003e) ) (\\u003cT NP\\\\NP fa 0 2\\u003e (\\u003cL (NP\\\\NP)/NP of of IN O O (NP\\\\NP)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) (\\u003cT NP rp 0 2\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cL N means mean VBZ O O N\\u003e) ) (\\u003cL . ? ? . O O .\\u003e) ) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cT S[wq]/(S[dcl]\\\\NP) bx 0 2\\u003e (\\u003cL S[wq]/(S[dcl]\\\\NP) What What WDT O O S[wq]/(S[dcl]\\\\NP)\\u003e) (\\u003cT S[wq]\\\\S[wq] fa 0 2\\u003e (\\u003cL (S[wq]\\\\S[wq])/NP each each DT O O (S[wq]\\\\S[wq])/NP\\u003e) (\\u003cT NP ba 0 2\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cL N fold fold NN O O N\\u003e) ) (\\u003cT NP\\\\NP fa 0 2\\u003e (\\u003cL (NP\\\\NP)/NP of of IN O O (NP\\\\NP)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) ) (\\u003cT S[dcl]\\\\NP rp 0 2\\u003e (\\u003cL S[dcl]\\\\NP means mean VBZ O O S[dcl]\\\\NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[qem] fa 0 2\\u003e (\\u003cT S[qem]/(S[dcl]/NP) bx 0 2\\u003e (\\u003cL S[qem]/(S[dcl]/NP) What What WDT O O S[qem]/(S[dcl]/NP)\\u003e) (\\u003cL S\\\\S each each DT O O S\\\\S\\u003e) ) (\\u003cT S[dcl]/NP fc 1 2\\u003e (\\u003cT S[X]/(S[X]\\\\NP) tr 0 1\\u003e (\\u003cT NP ba 0 2\\u003e (\\u003cT NP lex 0 1\\u003e (\\u003cL N fold fold NN O O N\\u003e) ) (\\u003cT NP\\\\NP fa 0 2\\u003e (\\u003cL (NP\\\\NP)/NP of of IN O O (NP\\\\NP)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) (\\u003cT (S[dcl]\\\\NP)/NP rp 0 2\\u003e (\\u003cL (S[dcl]\\\\NP)/NP means mean VBZ O O (S[dcl]\\\\NP)/NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) ) \",\"score\":\"1.0\"},{\"synPar\":\"(\\u003cT S[wq] fa 0 2\\u003e (\\u003cL S[wq]/(S[dcl]\\\\NP) What What WDT O O S[wq]/(S[dcl]\\\\NP)\\u003e) (\\u003cT S[dcl]\\\\NP fa 1 2\\u003e (\\u003cT (S\\\\NP)/(S\\\\NP) fa 0 2\\u003e (\\u003cL ((S\\\\NP)/(S\\\\NP))/N each each DT O O ((S\\\\NP)/(S\\\\NP))/N\\u003e) (\\u003cT N ba 0 2\\u003e (\\u003cL N fold fold NN O O N\\u003e) (\\u003cT N\\\\N fa 0 2\\u003e (\\u003cL (N\\\\N)/NP of of IN O O (N\\\\N)/NP\\u003e) (\\u003cT NP[nb] fa 0 2\\u003e (\\u003cL NP[nb]/N the the DT O O NP[nb]/N\\u003e) (\\u003cT N fa 1 2\\u003e (\\u003cL N/N Us Us NNP LOCATION O N/N\\u003e) (\\u003cL N flag flag NN O O N\\u003e) ) ) ) ) ) (\\u003cT S[dcl]\\\\NP rp 0 2\\u003e (\\u003cL S[dcl]\\\\NP means mean VBZ O O S[dcl]\\\\NP\\u003e) (\\u003cL . ? ? . O O .\\u003e) ) ) ) \",\"score\":\"1.0\"}]}";

    JsonObject jsonSentence = parser.parse(line).getAsJsonObject();

    List<LexicalGraph> graphs =
        graphCreator.buildUngroundedGraph(jsonSentence, "synPars", 1, logger);

    System.out.println("# Ungrounded Graphs");
    if (graphs.size() > 0) {
      for (LexicalGraph ungroundedGraph : graphs) {
        System.out.println(ungroundedGraph);
        System.out.println("Connected: " + ungroundedGraph.isConnected());

        List<LexicalGraph> groundedGraphs =
            graphCreator.createGroundedGraph(ungroundedGraph, 1000, 10000,
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
