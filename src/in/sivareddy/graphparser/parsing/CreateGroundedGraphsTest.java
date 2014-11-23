package in.sivareddy.graphparser.parsing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Test;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.GroundedGraphs.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.KnowledgeBase;
import in.sivareddy.graphparser.util.Schema;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateGroundedGraphsTest {

  KnowledgeBase kb;
  GroundedLexicon groundedLexicon;

  private void load() throws IOException {
    if (groundedLexicon == null) {
      groundedLexicon =
          new GroundedLexicon("data/freebase/grounded_lexicon/business_grounded_lexicon.txt");
    }
    if (kb == null) {
      kb = new KnowledgeBase("data/freebase/domain_facts/business_facts.txt.gz",
          "data/freebase/stats/business_relation_types.txt");
    }
  }

  @Test
  public void testGroundedGraphs() throws IOException {
    load();
    Schema schema = new Schema("data/freebase/schema/business_schema.txt");
    // KnowledgeBase kb = null;
    CcgAutoLexicon questionCcgAutoLexicon = new CcgAutoLexicon("./data/candc_markedup.modified",
        "./data/unary_rules.txt", "./data/binary_rules.txt",
        "./data/lexicon_specialCases_questions.txt");

    CcgAutoLexicon normalCcgAutoLexicon = new CcgAutoLexicon("./data/candc_markedup.modified",
        "./data/unary_rules.txt", "./data/binary_rules.txt", "./data/lexicon_specialCases.txt");

    // GroundedLexicon groundedLexicon = null;
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    GroundedGraphs graphCreator = new GroundedGraphs(schema,
        kb,
        groundedLexicon,
        normalCcgAutoLexicon,
        questionCcgAutoLexicon,
        relationLexicalIdentifiers,
        relationTypingIdentifiers,
        null,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        10.0,
        1.0,
        0.0,
        0.0);

    BufferedReader br =
        new BufferedReader(new FileReader("data/tests/sample_business_training_sentences.txt"));
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
            "{\"domain\": [\"business\"], \"sentence\": \"where is the nra headquarters located?\", \"url\": \"http://www.freebase.com/view/en/national_rifle_association\", \"synPars\": [{\"synPar\": \"(<T S[wq] rp 0 2> (<T S[wq] fa 0 2> (<T S[wq]/(S[q]/PP) fa 0 2> (<L (S[wq]/(S[q]/PP))/(S[wq]/(S[q]/NP)) In in IN O I-PP (S[wq]/(S[q]/PP))/(S[wq]/(S[q]/NP))>) (<T S[wq]/(S[q]/NP) fa 0 2> (<L (S[wq]/(S[q]/NP))/N what what WDT O I-NP (S[wq]/(S[q]/NP))/N>) (<L N location location NN O I-NP N>))) (<T S[q]/PP fc 0 2> (<T S[q]/(S[pss]\\\\NP) fa 0 2> (<L (S[q]/(S[pss]\\\\NP))/NP is be VBZ O I-VP (S[q]/(S[pss]\\\\NP))/NP>) (<T NP[nb] fa 1 2> (<T NP[nb]/N ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N Nra Nra NNP I-LOC I-NP N>)) (<L (NP[nb]/N)\\\\NP 's 's POS O B-NP (NP[nb]/N)\\\\NP>)) (<L N headquarters headquarters NN O I-NP N>))) (<L (S[pss]\\\\NP)/PP located locate VBN O I-VP (S[pss]\\\\NP)/PP>))) (<L . ? ? . O O .>))\", \"score\": 18.6452}], \"entities\": [{\"index\": 5, \"entity\": \"m.0j6f9\"}], \"words\": [{\"ner\": \"O\", \"word\": \"In\", \"pos\": \"IN\"}, {\"ner\": \"O\", \"word\": \"what\", \"pos\": \"WDT\"}, {\"ner\": \"O\", \"word\": \"location\", \"pos\": \"NN\"}, {\"ner\": \"O\", \"word\": \"is\", \"pos\": \"VBZ\"}, {\"ner\": \"O\", \"word\": \"the\", \"pos\": \"DT\"}, {\"ner\": \"ORGANIZATION\", \"word\": \"Nra\", \"pos\": \"NNP\"}, {\"ner\": \"0\", \"word\": \"'s\", \"pos\": \"POS\"}, {\"ner\": \"O\", \"word\": \"headquarters\", \"pos\": \"NN\"}, {\"ner\": \"O\", \"word\": \"located\", \"pos\": \"VBN\"}, {\"ner\": \"O\", \"word\": \"?\", \"pos\": \".\"}], \"targetValue\": \"(list (description Fairfax))\"}";
        JsonObject jsonSentence = parser.parse(line).getAsJsonObject();

        // JsonObject jsonSentence =
        // parser.parse(line).getAsJsonObject();
        List<LexicalGraph> graphs = graphCreator.buildUngroundedGraph(jsonSentence, 1, logger);

        System.out.println("# Ungrounded Graphs");
        if (graphs.size() > 0) {
          for (LexicalGraph ungroundedGraph : graphs) {
            System.out.println(ungroundedGraph);
            System.out.println("Connected: " + ungroundedGraph.isConnected());

            List<LexicalGraph> groundedGraphs = graphCreator.createGroundedGraph(ungroundedGraph,
                10,
                100,
                true,
                true,
                true,
                true,
                true,
                false);
            System.out.println("# Total number of Grounded Graphs: " + groundedGraphs.size());

            int connectedGraphCount = 0;
            for (LexicalGraph groundedGraph : groundedGraphs) {
              // if (groundedGraph.isConnected()) {
              connectedGraphCount += 1;
              System.out.println("# Grounded graph: " + connectedGraphCount);
              System.out.println(groundedGraph);
              System.out.println("Graph Query: "
                  + GraphToSparqlConverter.convertGroundedGraph(groundedGraph, schema));
              // }
            }

            System.out.println("# Total number of Grounded Graphs: " + groundedGraphs.size());
            System.out.println(
                "# Total number of Connected Grounded Graphs: " + connectedGraphCount);
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
