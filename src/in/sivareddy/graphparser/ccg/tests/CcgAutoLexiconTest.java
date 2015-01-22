package in.sivareddy.graphparser.ccg.tests;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;

public class CcgAutoLexiconTest {

  @Test
  public void testMapSynCatToIndexSynCatFromFile() throws IOException {
    CcgAutoLexicon lexicon =
        new CcgAutoLexicon("./tools/candc_nbest/models/parser/cats/markedup",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases.txt");
    String indexCat =
        lexicon.getIndexedSyntacticCategory("((S[adj]\\NP)/(S[to]\\NP))/PP");
    assertEquals(indexCat,
        "(((S[adj]{_}\\NP{Y}){_}/(S[to]{Z}\\NP{Y}){Z}){_}/PP{_}){_};_ 1 Y,_ 2 Z");
  }
}
