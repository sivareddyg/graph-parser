package in.sivareddy.graphparser.ccg;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CcgAutoLexiconTest {

  @Test
  public void testMapSynCatToIndexSynCatFromFile() throws IOException {
    CcgAutoLexicon lexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases.txt");
    String indexCat =
        lexicon.getIndexedSyntacticCategory("((S[adj]\\NP)/(S[to]\\NP))/PP");
    assertEquals(indexCat,
        "(((S[adj]{_}\\NP{Y}){_}/(S[to]{Z}\\NP{Y}){Z}){_}/PP{_}){_};_ 1 Y,_ 2 Z");
  }
}
