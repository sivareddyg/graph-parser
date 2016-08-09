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
        new KnowledgeBaseOnline("kinloch", "http://kinloch:8890/sparql", "dba",
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
            relationTypingIdentifiers, null, 1, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false,
            false, false, 10.0, 1.0, 0.0, 0.0);

    JsonParser parser = new JsonParser();

    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(this.getClass());
    logger.setLevel(Level.DEBUG);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    String line =
        "{\"freeVarCount\": 0, \"sentence\": \"_blank_ and Barbara_Eden divorced in 1974 .\", \"synPars\": [{\"synPar\": \"(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N _blank_ _blank_ NNP O I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP lex 0 1> (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))))) (<L . . . . O O .>))\", \"score\": 19.7954}, {\"synPar\": \"(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N ba 0 2> (<L N _blank_ _blank_ NNP O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))))) (<L . . . . O O .>))\", \"score\": 19.0183}, {\"synPar\": \"(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N _blank_ _blank_ NNP O I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP lex 0 1> (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP fa 0 2> (<L (S[dcl]\\\\NP)/PP divorced divorce VBD O I-VP (S[dcl]\\\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP in in IN O I-PP PP/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))))) (<L . . . . O O .>))\", \"score\": 15.9478}, {\"synPar\": \"(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N ba 0 2> (<L N _blank_ _blank_ NNP O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP fa 0 2> (<L (S[dcl]\\\\NP)/PP divorced divorce VBD O I-VP (S[dcl]\\\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP in in IN O I-PP PP/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))))) (<L . . . . O O .>))\", \"score\": 15.1707}, {\"synPar\": \"(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N _blank_ _blank_ NNP O I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP lex 0 1> (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) rp 0 2> (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))) (<L . . . . O O .>))))\", \"score\": 14.3616}, {\"synPar\": \"(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N _blank_ _blank_ NNP O I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP lex 0 1> (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP rp 0 2> (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>)))) (<L . . . . O O .>)))\", \"score\": 14.0932}, {\"synPar\": \"(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N Michael_Ansara Michael_Ansara NNP I-LOC I-NP N/N>) (<T N funny 1 2> (<L conj and and CC O I-NP conj>) (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))))) (<L . . . . O O .>))\", \"score\": 14.087}, {\"synPar\": \"(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N ba 0 2> (<L N _blank_ _blank_ NNP O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) rp 0 2> (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>))) (<L . . . . O O .>))))\", \"score\": 13.5845}, {\"synPar\": \"(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N ba 0 2> (<L N _blank_ _blank_ NNP O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP rp 0 2> (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>)))) (<L . . . . O O .>)))\", \"score\": 13.3161}, {\"synPar\": \"(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N _blank_ _blank_ NNP O I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP lex 0 1> (<L N Barbara_Eden Barbara_Eden NNP I-LOC I-NP N>)))) (<T S[dcl]\\\\NP ba 0 2> (<L S[dcl]\\\\NP divorced divorce VBD O I-VP S[dcl]\\\\NP>) (<T (S\\\\NP)\\\\(S\\\\NP) fa 0 2> (<L ((S\\\\NP)\\\\(S\\\\NP))/NP in in IN O I-PP ((S[X]\\\\NP)\\\\(S[X]\\\\NP))/NP>) (<T NP rp 0 2> (<T NP lex 0 1> (<L N 1974 1974 CD I-DAT I-NP N>)) (<L . . . . O O .>)))))\", \"score\": 12.8142}], \"freeEntityCount\": 0, \"boundedVarCount\": 0, \"negationCount\": 0, \"entities\": [{\"index\": 2, \"score\": 1.0, \"entity\": \"m.034jjp\"}], \"foreignEntityCount\": 0, \"words\": [{\"ner\": \"O\", \"supertags\": [\"N\"], \"word\": \"_blank_\", \"pos\": \"NNP\"}, {\"ner\": \"O\", \"word\": \"and\", \"pos\": \"CC\"}, {\"ner\": \"O\", \"word\": \"Barbara_Eden\", \"pos\": \"NNP\"}, {\"ner\": \"O\", \"word\": \"divorced\", \"pos\": \"VBD\"}, {\"ner\": \"O\", \"word\": \"in\", \"pos\": \"IN\"}, {\"ner\": \"DATE\", \"word\": \"1974\", \"pos\": \"CD\"}, {\"ner\": \"O\", \"word\": \".\", \"pos\": \".\"}], \"answerSubset\": [\"m.06xkzs\"]}";

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
                true, true, true, true, true, false, false, false);
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
                    schema, 100));
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
