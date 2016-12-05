package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

public class CreateGroundedLexiconTest {

  @Test
  public void testCreateGroundedLexicon() throws IOException,
      InterruptedException {
    String[] lexicalFields = {"lemma"};
    String[] relationTypingFeilds = {};

    Schema schemaObj =
        new Schema("data/freebase/stats/business_relation_types.txt");
    KnowledgeBaseCached kb =
        new KnowledgeBaseCached(
            "data/freebase/domain_facts/business_facts.txt.gz",
            "data/freebase/stats/business_relation_types.txt");
    CcgAutoLexicon normalCcgAutoLexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases.txt");

    GroundedLexicon groundedLexicon = new GroundedLexicon(null);
    GroundedGraphs graphCreator = new GroundedGraphs(schemaObj, kb,
        groundedLexicon, normalCcgAutoLexicon, normalCcgAutoLexicon,
        lexicalFields, relationTypingFeilds, null, 1, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, 0.0, 0.0, 0.0, 0.0);

    CreateGroundedLexicon engine =
        new CreateGroundedLexicon(graphCreator, kb, "dependency_lambda", true,
            1);

    long startTime = System.currentTimeMillis();
    engine.processStream(new FileInputStream(
        "data/tests/deplambda.graphparser.txt"), new FileOutputStream(
        "working/lexicon.txt"), 4);
    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    System.err.println(totalTime);
  }
}
