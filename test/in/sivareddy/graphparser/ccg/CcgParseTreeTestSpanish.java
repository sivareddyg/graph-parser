package in.sivareddy.graphparser.ccg;

import in.sivareddy.graphparser.ccg.CcgParseTree.TooManyParsesException;
import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;

import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class CcgParseTreeTestSpanish {
  @Test
  public void testParseFromString() throws IOException,
      FunnyCombinatorException, BadParseException, TooManyParsesException {
    CcgAutoLexicon lexicon =
        new CcgAutoLexicon("./lib_data/ybisk-semi-mapping.txt",
            "./lib_data/dummy.txt", "./lib_data/dummy.txt",
            "./data/distant_eval/spanish/semisup/lexicon_fullSpecialCases.txt");

    String[] relationLexicalIdentifiers = {"lemma"};
    String[] argumentLexicalIdenfiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    boolean ignorePronouns = false;
    CcgParser ccgParser =
        new CcgParser(lexicon, relationLexicalIdentifiers,
            argumentLexicalIdenfiers, relationTypingIdentifiers, ignorePronouns);

    List<CcgParseTree> ccgParseTrees;

    Set<Set<String>> relations;
    String sent;

    sent = "No tiene fuerzas armadas propias y su defensa es responsabilidad de España y Francia .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S ba 0 2> (<T S fa 1 2> (<L S/S No No ADV _ _ S_22/S_22>) (<T S fa 0 2> (<L S/N tiene tiene VERB _ _ S/N_31>) (<T N ba 0 2> (<T N ba 0 2> (<L N fuerzas fuerzas NOUN _ _ N>) (<L N\\N armadas armadas ADJ _ _ N_53\\N_53>)) (<L N\\N propias propias ADJ _ _ N_68\\N_68>)))) (<T S[conj] conj 1 2> (<L conj y y CONJ _ _ conj>) (<T S ba 1 2> (<T N fa 1 2> (<L N/N su su DET _ _ N_101/N_101>) (<L N defensa defensa NOUN _ _ N>)) (<T S\\N fa 0 2> (<T (S\\N)/PP fa 0 2> (<L ((S\\N)/PP)/N es es VERB _ _ ((S\\N_128)/PP_132)/N_135>) (<L N responsabilidad responsabilidad NOUN _ _ N>)) (<T PP fa 0 2> (<L PP/N de de ADP _ _ PP/N_164>) (<T N ba 0 2> (<L N España España NOUN _ _ N>) (<T N[conj] conj 1 2> (<L conj y y CONJ _ _ conj>) (<L N Francia Francia NOUN _ _ N>))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

  }
}
