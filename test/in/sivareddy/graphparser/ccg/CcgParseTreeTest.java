package in.sivareddy.graphparser.ccg;

import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class CcgParseTreeTest {

  @Test
  public void testParseFromString() throws IOException,
      FunnyCombinatorException, BadParseException {
    // CcgAutoLexicon lexicon = new
    // CcgAutoLexicon("./data/candc_markedup.modified",
    // "./data/unary_rules.txt", "./data/binary_rules.txt",
    // "./data/lexicon_specialCases.txt");
    CcgAutoLexicon lexicon =
        new CcgAutoLexicon("./lib_data/candc_markedup.modified",
            "./lib_data/unary_rules.txt", "./lib_data/binary_rules.txt",
            "./lib_data/lexicon_specialCases_questions.txt");
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

    // ccgParseTree =
    // ccgParser.parseFromString("(<L N Adobe Adobe NNP I-ORG I-NP N>)");
    // assertEquals(ccgParseTree.getLeafNodes().size(), 1);

    sent = "Obama is the president of US";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Obama Obama NNP I-LOC I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N president president NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N US US NNP I-ORG I-NP N>)))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();


    sent =
        "Miyagi is a pulp culture icon who was brilliantly played by Pat_Morita .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Miyagi Miyagi NNP PERSON O N>) ) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O O (S[dcl]\\NP)/NP>) (<T NP[nb] ba 0 2> (<T NP[nb] fa 0 2> (<L NP[nb]/N a a DT O O NP[nb]/N>) (<T N fa 1 2> (<L N/N pulp pulp NN O O N/N>) (<T N fa 1 2> (<L N/N culture culture NN O O N/N>) (<L N icon icon NN O O N>) ) ) ) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]\\NP) who who WP O O (NP\\NP)/(S[dcl]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/(S[pss]\\NP) bx 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O O (S[dcl]\\NP)/(S[pss]\\NP)>) (<L (S\\NP)\\(S\\NP) brilliantly brilliantly RB O O (S\\NP)\\(S\\NP)>) ) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP played play VBN O O S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP by by IN O O ((S\\NP)\\(S\\NP))/NP>) (<T NP rp 0 2> (<T NP lex 0 1> (<L N Pat_Morita Pat-Morita NNP PERSON O N>) ) (<L . . . . O O .>) ) ) ) ) ) ) ) )");
    System.out.println(ccgParseTrees);
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Another great Mel_Gibson 's movie is Forever_Young .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<T NP[nb]/N ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N Another another DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N great great JJ O I-NP N/N>) (<L N Mel_Gibson Mel_Gibson NNP I-ORG I-NP N>))) (<L (NP[nb]/N)\\NP 's 's POS O B-NP (NP[nb]/N)\\NP>)) (<L N movie movie NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Forever_Young Forever_Young NNP O I-NP N>)))) (<L . . . . O O .>))");
    System.out.println(ccgParseTrees);
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();


    sent = "My name is Siva";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N My my PRP$ O I-NP NP[nb]/N>) (<L N name name NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Siva Siva NNP I-PER I-NP N>))))");
    System.out.println(ccgParseTrees);
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Obama 's birthplace is Hawaii";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<T NP[nb]/N ba 1 2> (<T NP lex 0 1> (<L N Obama Obama NNP I-LOC I-NP N>)) (<L (NP[nb]/N)\\NP 's 's IPOS O B-NP (NP[nb]/N)\\NP>)) (<L N birthplace birthplace NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Hawaii Hawaii NNP I-LOC I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();
    
    relations = ccgParseTrees.get(1).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    /*-relations = ccgParseTrees.get(2).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();
    
    relations = ccgParseTrees.get(3).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();*/


    sent = "what is the highest point of the state with largest area ?";
    ccgParseTrees = 
        ccgParser
            .parseFromString("(<T S[wq] fa 0 2> (<L S[wq]/(S[dcl]\\NP) what what WP O I-NP S[wq]/(S[dcl]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N highest highest JJS O I-NP N/N>) (<L N point point NN O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N state state NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP with with IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N largest largest JJS O I-NP N/N>) (<L N area area NN O I-NP N>)))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    // System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "The cooperation between Pitt and John in Casablanca yielded millions";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N cooperation cooperation NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP between between IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N Pitt Pitt NNP I-PER I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC I-PER O conj>) (<T NP lex 0 1> (<L N John John NNP I-PER I-NP N>)))))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP in in IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Casablanca Casablanca NNP I-LOC I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP yielded yield VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N millions million NNS O I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "How-many Prosthetic_makeup_artists worked on Titanic ?";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[wq] rp 0 2> (<T S[wq] fa 0 2> (<T S[wq]/(S[dcl]\\NP) fa 0 2> (<L (S[wq]/(S[dcl]\\NP))/N How-many how-many CD 0 I-NP (S[wq]/(S[dcl]\\NP))/N>) (<L N Prosthetic_makeup_artists Prosthetic_makeup_artists NN I-ORG B-NP N>)) (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP worked work VBD O I-VP S[dcl]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP on on IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N The_28_days_later The_28_days_later NNP I-DAT I-NP N>))))) (<L . ? ? . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates(true);
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Fisher is a Managing Director of Draper_Fisher_Jurvetson .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Fisher Fisher NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N Managing manage VBG I-ORG I-NP N/N>) (<L N Director Director NN I-ORG I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN I-ORG I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Draper_Fisher_Jurvetson Draper_Fisher_Jurvetson NNP I-ORG I-NP N>)))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "EDF is currently the largest supplier .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N EDF EDF NNP I-ORG I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/NP bx 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<L (S\\NP)\\(S\\NP) currently currently RB O I-ADVP (S[X]\\NP)\\(S[X]\\NP)>)) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N largest largest JJS O I-NP N/N>) (<L N supplier supplier NN O I-NP N>))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    // System.out.println(sent);
    sent = "Cameron is the director of the movie which Dicaprio acted in .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Cameron Cameron NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N director director NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N movie movie NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]/NP) which which WDT O B-NP (NP\\NP)/(S[dcl]/NP)>) (<T S[dcl]/NP fc 1 2> (<T S/(S\\NP) tr 0 1> (<T NP lex 0 1> (<L N Dicaprio Dicaprio NNP I-ORG B-NP N>))) (<T (S[dcl]\\NP)/NP fc 0 2> (<L (S[dcl]\\NP)/PP acted act VBD O I-VP (S[dcl]\\NP)/PP>) (<L PP/NP in in IN O I-PRT PP/NP>))))))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Cameron , the director of Titanic , appears in the documentary";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP rp 0 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N Cameron Cameron NNP I-PER I-NP N>)) (<T NP[conj] conj 0 2> (<L , , , , O O ,>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N director director NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Titanic Titanic NNP I-LOC I-NP N>)))))) (<L , , , , O O ,>)) (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP appears appear VBZ O I-VP S[dcl]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N documentary documentary NN O I-NP N>)))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Google wants to buy Facebook .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Google Google NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[to]\\NP) wants want VBZ O I-VP (S[dcl]\\NP)/(S[to]\\NP)>) (<T S[to]\\NP fa 0 2> (<L (S[to]\\NP)/(S[b]\\NP) to to TO O I-VP (S[to]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/NP buy buy VB O I-VP (S[b]\\NP)/NP>) (<T NP lex 0 1> (<L N Facebook Facebook NNP I-LOC I-NP N>)))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "India could win the match";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N India India NNP I-LOC I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[b]\\NP) could could MD O I-VP (S[dcl]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/NP win win VB O I-VP (S[b]\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N match match NN O I-NP N>)))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Gates is not the founder of Google";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Gates Gates NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/NP bx 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<L (S\\NP)\\(S\\NP) not not RB O O (S[X]\\NP)\\(S[X]\\NP)>)) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N founder founder NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Google Google NNP I-ORG I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "The bottle I want to buy is broken";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N bottle bottle NN O I-NP N>)) (<T NP\\NP lex 0 1> (<T S[dcl]/NP fc 1 2> (<T S/(S\\NP) tr 0 1> (<L NP I I PRP O B-NP NP>)) (<T (S[dcl]\\NP)/NP fc 0 2> (<L (S[dcl]\\NP)/(S[to]\\NP) want want VBP O I-VP (S[dcl]\\NP)/(S[to]\\NP)>) (<T (S[to]\\NP)/NP fc 0 2> (<L (S[to]\\NP)/(S[b]\\NP) to to TO O I-VP (S[to]\\NP)/(S[b]\\NP)>) (<L (S[b]\\NP)/NP buy buy VB O I-VP (S[b]\\NP)/NP>)))))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) is be VBZ O B-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L S[pss]\\NP broken break VBN O I-VP S[pss]\\NP>)))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Siva is my name";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Siva Siva NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N my my PRP$ O I-NP NP[nb]/N>) (<L N name name NN O I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "My name is Siva";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N My my PRP$ O I-NP NP[nb]/N>) (<L N name name NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Siva Siva NNP I-PER I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Obama 's birthplace is Hawaii";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<T NP[nb]/N ba 1 2> (<T NP lex 0 1> (<L N Obama Obama NNP I-LOC I-NP N>)) (<L (NP[nb]/N)\\NP 's 's POS O B-NP (NP[nb]/N)\\NP>)) (<L N birthplace birthplace NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Hawaii Hawaii NNP I-LOC I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();
    /*relations = ccgParseTrees.get(1).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();*/

    sent =
        "The Bavarian_Illuminati , a rationalist secret society , was founded by Adam_Weishaupt";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] rp 0 2> (<T NP[nb] ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N Bavarian_Illuminati Bavarian_Illuminati NNP I-ORG I-NP N>)) (<T NP[nb][conj] conj 0 2> (<L , , , , O O ,>) (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N rationalist rationalist NN O I-NP N/N>) (<T N fa 1 2> (<L N/N secret secret JJ O I-NP N/N>) (<L N society society NN O I-NP N>)))))) (<L , , , , O O ,>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP founded found VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP by by IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N Adam_Weishaupt Adam_Weishaupt NNP O I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();


    sent =
        "The Bavarian_Illuminati , a rationalist secret society , was founded by Adam_Weishaupt in 1776 in what is today Germany .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP[nb] rp 0 2> (<T NP[nb] ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N Bavarian_Illuminati Bavarian_Illuminati NNP I-ORG I-NP N>)) (<T NP[nb][conj] conj 0 2> (<L , , , , O O ,>) (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N rationalist rationalist NN O I-NP N/N>) (<T N fa 1 2> (<L N/N secret secret JJ O I-NP N/N>) (<L N society society NN O I-NP N>)))))) (<L , , , , O O ,>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> (<T S[pss]\\NP ba 0 2> (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP founded found VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP by by IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N Adam_Weishaupt Adam_Weishaupt NNP O I-NP N>)))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N 1776 1776 CD I-DAT I-NP N>)))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP fa 0 2> (<L NP/(S[dcl]\\NP) what what WP O I-NP NP/(S[dcl]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/NP bx 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<L (S\\NP)\\(S\\NP) today today NN I-DAT I-NP (S[X]\\NP)\\(S[X]\\NP)>)) (<T NP lex 0 1> (<L N Germany Germany NNP I-LOC I-NP N>)))))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();


    sent =
        "HSBC has eight branches throughout the Emirates , of which three are in Dubai";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N HSBC HSBC NNP I-ORG I-NP N>)) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP has have VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N eight eight CD O I-NP N/N>) (<L N branches branch NNS O I-NP N>)))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP throughout throughout IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP[nb] rp 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N Emirates emirate NNS I-ORG I-NP N>)) (<L , , , , O O ,>)) (<T NP\\NP fa 0 2> (<T (NP\\NP)/S[dcl] ba 1 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<L ((NP\\NP)/S[dcl])\\((NP\\NP)/NP) which which WDT O I-NP ((NP\\NP)/S[dcl])\\((NP\\NP)/NP)>)) (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N three three CD O B-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/PP are be VBP O I-VP (S[dcl]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP in in IN O I-PP PP/NP>) (<T NP lex 0 1> (<L N Dubai Dubai NNP I-LOC I-NP N>))))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "Nevada_State_College , and Touro_University_'s_College_of_Osteopathic_Medicine are both located in nearby Henderson";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP rp 0 2> (<T NP lex 0 1> (<L N Nevada_State_College Nevada_State_College NNP I-LOC I-NP N>)) (<L , , , , O O ,>)) (<T NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T NP lex 0 1> (<L N Touro_University_'s_College_of_Osteopathic_Medicine Touro_University_'s_College_of_Osteopathic_Medicine NNP I-ORG I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/(S[pss]\\NP) bx 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) are be VBP O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L (S\\NP)\\(S\\NP) both both DT O O (S[X]\\NP)\\(S[X]\\NP)>)) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP located locate VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N nearby nearby JJ O I-NP N/N>) (<L N Henderson Henderson NNP I-LOC I-NP N>)))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "The founder of Google is not Gates";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N founder founder NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Google Google NNP I-ORG I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/NP bx 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<L (S\\NP)\\(S\\NP) not not RB O O (S[X]\\NP)\\(S[X]\\NP)>)) (<T NP lex 0 1> (<L N Gates Gates NNP I-PER I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "I gave 30 dollars to 60 people ";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<L NP I I PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/PP fa 0 2> (<L ((S[dcl]\\NP)/PP)/NP gave give VBD O I-VP ((S[dcl]\\NP)/PP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N 30 30 CD O I-NP N/N>) (<L N dollars dollar NNS O I-NP N>)))) (<T PP fa 0 2> (<L PP/NP to to TO O I-PP PP/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N 60 60 CD O I-NP N/N>) (<L N people people NNS O I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "300,000 people live on Margarita Island most of whom live in the eastern part where the capital of Asuncion and the shoppers paradise of Porlamar are located";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N 300,000 300,000 CD I-NUM I-NP N/N>) (<L N people people NNS O I-NP N>))) (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP live live VBP O I-VP S[dcl]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP on on IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N Margarita_Island Margarita_Island NNP I-LOC I-NP N>)) (<T NP\\NP fa 0 2> (<T (NP\\NP)/(S[dcl]\\NP) ba 1 2> (<T NP/NP fc 0 2> (<T NP/(NP\\NP) lex 0 1> (<L NP most most JJS O I-NP NP>)) (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>)) (<L ((NP\\NP)/(S[dcl]\\NP))\\(NP/NP) whom whom WP O I-NP ((NP\\NP)/(S[dcl]\\NP))\\(NP/NP)>)) (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP live live VBP O I-VP S[dcl]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N eastern eastern JJ O I-NP N/N>) (<L N part part NN O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/S[dcl] where where WRB O I-ADVP (NP\\NP)/S[dcl]>) (<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N capital capital NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N Asuncion Asuncion NNP I-LOC I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N shoppers shopper NNS O I-NP N/N>) (<L N paradise paradise NN O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Porlamar Porlamar NNP I-LOC I-NP N>)))))))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) are be VBP O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L S[pss]\\NP located locate VBN O I-VP S[pss]\\NP>)))))))))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "His work was unknown to Darwin and  was not incorporated into evolutionary thinking until 1900 , when it resulted  in rejection of natural selection on small differences as the cause of  differences between species , and the claim that mutations caused them in  one big jump";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N His his PRP$ O I-NP NP[nb]/N>) (<L N work work NN O I-NP N>)) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[adj]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[adj]\\NP)>) (<T S[adj]\\NP fa 0 2> (<L (S[adj]\\NP)/PP unknown unknown JJ O I-ADJP (S[adj]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP to to TO O I-PP PP/NP>) (<T NP lex 0 1> (<L N Darwin Darwin NNP I-LOC I-NP N>))))) (<T S[dcl]\\NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/(S[pss]\\NP) bx 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L (S\\NP)\\(S\\NP) not not RB O I-VP (S[X]\\NP)\\(S[X]\\NP)>)) (<T S[pss]\\NP ba 0 2> (<T S[pss]\\NP fa 0 2> (<L (S[pss]\\NP)/PP incorporated incorporate VBN O I-VP (S[pss]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP into into IN O I-PP PP/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N evolutionary evolutionary JJ O I-NP N/N>) (<L N thinking thinking NN O I-NP N>))))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP until until IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N 1900 1900 CD I-DAT I-NP N>)) (<T NP\\NP lp 1 2> (<L , , , , O O ,>) (<T NP\\NP fa 0 2> (<L (NP\\NP)/S[dcl] when when WRB O I-ADVP (NP\\NP)/S[dcl]>) (<T S[dcl] ba 1 2> (<L NP it it PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/PP resulted result VBD O I-VP (S[dcl]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP in in IN O I-PP PP/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N rejection rejection NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N natural natural JJ O I-NP N/N>) (<L N selection selection NN O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP on on IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N small small JJ O I-NP N/N>) (<L N differences difference NNS O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP as as IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N cause cause NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N differences difference NNS O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP between between IN O I-PP (NP\\NP)/NP>) (<T NP[nb] ba 0 2> (<T NP lex 0 1> (<L N species species NNS O I-NP N>)) (<T NP[nb][conj] conj 1 2> (<L , , , , O O ,>) (<T NP[nb][conj] conj 0 2> (<L conj and and CC O O conj>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 0 2> (<L N/S[em] claim claim NN O I-NP N/S[em]>) (<T S[em] fa 0 2> (<L S[em]/S[dcl] that that IN O I-SBAR S[em]/S[dcl]>) (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N mutations mutation NNS O I-NP N>)) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP caused cause VBD O I-VP (S[dcl]\\NP)/NP>) (<L NP them they PRP O I-NP NP>)) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N one one CD O I-NP N/N>) (<T N fa 1 2> (<L N/N big big JJ O I-NP N/N>) (<L N jump jump NN O I-NP N>)))))))))))))))))))))))))))))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "The waitresses , known as Harvey_Girls , served delicious food , most of which had been grown and raised on Fred_Harvey farms and dairies .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP rp 0 2> (<T NP ba 0 2> (<T NP[nb] rp 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N waitresses waitress NNS O I-NP N>)) (<L , , , , O O ,>)) (<T NP\\NP lex 0 1> (<T S[pss]\\NP fa 0 2> (<L (S[pss]\\NP)/PP known know VBN O I-VP (S[pss]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP as as IN O I-PP PP/NP>) (<T NP lex 0 1> (<L N Harvey_Girls Harvey_Girls NNP O I-NP N>)))))) (<L , , , , O O ,>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP served serve VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N delicious delicious JJ O I-NP N/N>) (<L N food food NN O I-NP N>))) (<T NP\\NP lp 1 2> (<L , , , , O O ,>) (<T NP\\NP fa 0 2> (<T (NP\\NP)/(S[dcl]\\NP) ba 1 2> (<T NP/NP fc 0 2> (<T NP/(NP\\NP) lex 0 1> (<L NP most most JJS O I-NP NP>)) (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>)) (<L ((NP\\NP)/(S[dcl]\\NP))\\(NP/NP) which which WDT O I-NP ((NP\\NP)/(S[dcl]\\NP))\\(NP/NP)>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pt]\\NP) had have VBD O I-VP (S[dcl]\\NP)/(S[pt]\\NP)>) (<T S[pt]\\NP fa 0 2> (<L (S[pt]\\NP)/(S[pss]\\NP) been be VBN O I-VP (S[pt]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP grown grow VBN O I-VP S[pss]\\NP>) (<T S[pss]\\NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP raised raise VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP on on IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N Fred_Harvey Fred_Harvey NNP O I-NP N/N>) (<L N farms farm NNS O I-NP N>))) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP lex 0 1> (<L N dairies dairy NNS O I-NP N>))))))))))))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();


    sent = "Adobe recently bought Macromedia .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Adobe Adobe NNP I-ORG I-NP N>)) (<T S[dcl]\\NP fa 1 2> (<L (S\\NP)/(S\\NP) recently recently RB O I-ADVP (S[X]\\NP)/(S[X]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP bought buy VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Macromedia Macromedia NNP I-LOC I-NP N>)))))");
    Category cat = ccgParseTrees.get(0).getCategory();
    assertEquals(cat.getSyntacticCategory().toSimpleString(), "S[dcl]");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Cameron directed the movie Titanic";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Cameron Cameron NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP directed direct VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N movie movie JJ O I-NP N/N>) (<L N Titanic Titanic NNP O I-NP N>)))))");

    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Titanic is directed by Cameron";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Titanic Titanic NNP I-LOC I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) is be VBZ O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP directed direct VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP by by IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N Cameron Cameron NNP I-ORG I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "The book contains writing about traditional puppets of West Bengal";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N book book NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[ng]\\NP) contains contain VBZ O I-VP (S[dcl]\\NP)/(S[ng]\\NP)>) (<T S[ng]\\NP fa 0 2> (<L (S[ng]\\NP)/PP writing write VBG O I-VP (S[ng]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP about about IN O I-PP PP/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N traditional traditional JJ O I-NP N/N>) (<L N puppets puppet NNS O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N West West NNP I-LOC I-NP N/N>) (<L N Bengal Bengal NNP I-LOC I-NP N>)))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "I wrote about traditional puppets of Bengal";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<L NP I I PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/PP wrote write VBD O I-VP (S[dcl]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP about about IN O I-PP PP/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N traditional traditional JJ O I-NP N/N>) (<L N puppets puppet NNS O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Bengal Bengal NNP I-ORG I-NP N>)))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Obama was elected in 2010 .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Obama Obama NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP elected elect VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N 2010 2010 CD I-DAT I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "The next night I went again with Chad .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] fa 1 2> (<T S/S fa 0 2> (<L (S/S)/N The the DT O I-NP (S[X]/S[X])/N>) (<T N fa 1 2> (<L N/N next next JJ O I-NP N/N>) (<L N night night NN O I-NP N>))) (<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<L NP I I PRP O B-NP NP>) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP went go VBD O I-VP S[dcl]\\NP>) (<L (S\\NP)\\(S\\NP) again again RB O I-ADVP (S[X]\\NP)\\(S[X]\\NP)>)) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP with with IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N Chad Chad NNP I-LOC I-NP N>))))) (<L . . . . O O .>)))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "director and producer";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T N ba 0 2> (<L N director director NN O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<L N producer producer NN O I-NP N>)))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Cameron is the director and the producer of Titanic";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Cameron Cameron NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N director director NN O I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N producer producer NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N Titanic Titanic NNP I-ORG I-NP N>))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "state not bordering Texas";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T NP ba 0 2> (<T NP lex 0 1> (<L N state state NN O I-NP N>)) (<T NP\\NP lex 0 1> (<T S[ng]\\NP fa 1 2> (<L (S\\NP)/(S\\NP) not not RB O I-VP (S[X]\\NP)/(S[X]\\NP)>) (<T S[ng]\\NP fa 0 2> (<L (S[ng]\\NP)/NP bordering border VBG O I-VP (S[ng]\\NP)/NP>) (<T NP lex 0 1> (<L N Texas Texas NNP I-LOC I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "states bordered by no states";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T NP ba 0 2> (<T NP lex 0 1> (<L N states state NNS O I-NP N>)) (<T NP\\NP lex 0 1> (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP bordered border VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP by by IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N no no DT O I-NP NP[nb]/N>) (<L N states state NNS O I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "The dog which cried is taken to hospital";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<L N dog dog NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]\\NP) which which WDT O B-NP (NP\\NP)/(S[dcl]\\NP)>) (<L S[dcl]\\NP cried cry VBD O I-VP S[dcl]\\NP>))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) is be VBZ O B-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP fa 0 2> (<L (S[pss]\\NP)/PP taken take VBN O I-VP (S[pss]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP to to TO O I-PP PP/NP>) (<T NP lex 0 1> (<L N hospital hospital NN O I-NP N>)))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "The named book analyses instead how the creation of emergencies becomes a way to establish more restrictive laws and censorship , in the real world and on the Internet";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T (S\\NP)\\((S\\NP)/NP) ba 1 2> (<T (S\\NP)\\((S\\NP)/NP) bc 1 2> (<T (S\\NP)\\((S\\NP)/NP) bc 1 2> (<T (S\\NP)\\((S\\NP)/NP) tr 0 1> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N named name VBN O I-NP N/N>) (<T N fa 1 2> (<L N/N book book NN O I-NP N/N>) (<L N analyses analysis NNS O I-NP N>))))) (<L (S\\NP)\\(S\\NP) instead instead RB O I-ADVP (S[X]\\NP)\\(S[X]\\NP)>)) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/S[dcl] how how WRB O B-ADVP ((S[X]\\NP)\\(S[X]\\NP))/S[dcl]>) (<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N creation creation NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<L N emergencies emergency NNS O I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP becomes become VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<L N way way NN O I-NP N>)) (<T NP\\NP lex 0 1> (<T S[to]\\NP fa 0 2> (<L (S[to]\\NP)/(S[b]\\NP) to to TO O I-VP (S[to]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/NP establish establish VB O I-VP (S[b]\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<T N/N fa 1 2> (<L (N/N)/(N/N) more more RBR O I-NP (N/N)/(N/N)>) (<L N/N restrictive restrictive JJ O I-NP N/N>)) (<L N laws law NNS O I-NP N>))))))))))) (<T (S\\NP)\\((S\\NP)/NP)[conj] conj 1 2> (<L conj and and CC O I-NP conj>) (<T (S\\NP)\\((S\\NP)/NP) bc 1 2> (<T (S\\NP)\\((S\\NP)/NP) tr 0 1> (<T NP lex 0 1> (<L N censorship censorship NN O I-NP N>))) (<T (S\\NP)\\(S\\NP) lp 1 2> (<L , , , , O O ,>) (<T (S\\NP)\\(S\\NP) fa 1 2> (<L ((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP)) both both DT O I-NP ((S[X]\\NP)\\(S[X]\\NP))/((S[X]\\NP)\\(S[X]\\NP))>) (<T (S\\NP)\\(S\\NP) ba 1 2> (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N real real JJ O I-NP N/N>) (<L N world world NN O I-NP N>)))) (<T (S\\NP)\\(S\\NP)[conj] conj 1 2> (<L conj and and CC O O conj>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP on on IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N Internet internet NN O I-NP N>))))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();



    sent =
        "On the Internet the press releases spread like wildfire, reaching hundreds of thousands of mailing lists and newsgroup subscribers.";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] fa 1 2> (<T S/S fa 0 2> (<L (S/S)/NP On on IN O I-PP (S[X]/S[X])/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N Internet internet NN O I-NP N>))) (<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O B-NP NP[nb]/N>) (<L N press press NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pt]\\NP) releases release VBZ O I-VP (S[dcl]\\NP)/(S[pt]\\NP)>) (<T S[pt]\\NP ba 0 2> (<T S[pt]\\NP rp 0 2> (<T S[pt]\\NP ba 0 2> (<L S[pt]\\NP spread spread VBN O I-VP S[pt]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP like like IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N wildfire wildfire NN O I-NP N>)))) (<L , , , , O O ,>)) (<T (S\\NP)\\(S\\NP) lex 0 1> (<T S[ng]\\NP fa 0 2> (<L (S[ng]\\NP)/NP reaching reach VBG O I-VP (S[ng]\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N hundreds hundred NNS O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N thousands thousand NNS O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N mailing mailing NN O I-NP N/N>) (<L N lists list NNS O I-NP N>))) (<T NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N newsgroup newsgroup NN O I-NP N/N>) (<L N subscribers subscriber NNS O I-NP N>)))))))))))))) (<L . . . . O O .>)))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "food , most of which was grown";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T NP ba 0 2> (<T NP lex 0 1> (<L N food food NN O I-NP N>)) (<T NP\\NP lp 1 2> (<L , , , , O O ,>) (<T NP\\NP fa 0 2> (<T (NP\\NP)/(S[dcl]\\NP) ba 1 2> (<T NP/NP fc 0 2> (<T NP/(NP\\NP) lex 0 1> (<L NP most most JJS O I-NP NP>)) (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>)) (<L ((NP\\NP)/(S[dcl]\\NP))\\(NP/NP) which which WDT O I-NP ((NP\\NP)/(S[dcl]\\NP))\\(NP/NP)>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L S[pss]\\NP grown grow VBN O I-VP S[pss]\\NP>)))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Displayed below are the cities of west virginia";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] fa 1 2> (<T S/S lex 0 1> (<L S[pss]\\NP Displayed display VBN O I-VP S[pss]\\NP>)) (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N below below IN O I-PP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP are be VBP O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N cities city NNS O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP of of IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N west west NN O I-NP N/N>) (<L N virginia virginium NN O I-NP N>))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();



    sent =
        "Palestinian customs revenue was withheld by Israel while the EU and USA implemented punitive financial and economic measures";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N Palestinian palestinian JJ O I-NP N/N>) (<T N fa 1 2> (<L N/N customs custom NNS O I-NP N/N>) (<L N revenue revenue NN O I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP ba 0 2> (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP withheld withhold VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP by by IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N Israel Israel NNP I-LOC I-NP N>)))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/S[dcl] while while IN O I-SBAR ((S[X]\\NP)\\(S[X]\\NP))/S[dcl]>) (<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N ba 0 2> (<L N EU EU NNP I-ORG I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<L N USA USA NNP I-ORG I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP implemented implement VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N punitive punitive JJ O I-NP N/N>) (<T N fa 1 2> (<T N/N ba 1 2> (<L N/N financial financial JJ O I-NP N/N>) (<T N/N[conj] conj 1 2> (<L conj and and CC O I-NP conj>) (<L N/N economic economic JJ O I-NP N/N>))) (<L N measures measure NNS O I-NP N>))))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "I  would like to speak with the president";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<L NP I I PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[b]\\NP) would would MD O I-VP (S[dcl]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/(S[to]\\NP) like like VB O I-VP (S[b]\\NP)/(S[to]\\NP)>) (<T S[to]\\NP fa 0 2> (<L (S[to]\\NP)/(S[b]\\NP) to to TO O I-VP (S[to]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/PP speak speak VB O I-VP (S[b]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP with with IN O I-PP PP/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N president president NN O I-NP N>))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "The religious and ritual origin is evident till today when we find puppeteers , from Indonesia and India, who begin their show with prayers to the gods and look upon their puppets as divine manifestation";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<T N/N ba 1 2> (<L N/N religious religious JJ O I-NP N/N>) (<T N/N[conj] conj 1 2> (<L conj and and CC O I-NP conj>) (<L N/N ritual ritual JJ O I-NP N/N>))) (<L N origin origin NN O I-NP N>))) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[adj]\\NP) is be VBZ O I-VP (S[dcl]\\NP)/(S[adj]\\NP)>) (<L S[adj]\\NP evident evident JJ O I-ADJP S[adj]\\NP>)) (<T (S\\NP)\\(S\\NP) fa 1 2> (<L ((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP)) till till NN O I-SBAR ((S[X]\\NP)\\(S[X]\\NP))/((S[X]\\NP)\\(S[X]\\NP))>) (<T (S\\NP)\\(S\\NP) ba 1 2> (<L (S\\NP)\\(S\\NP) today today NN I-DAT I-NP (S[X]\\NP)\\(S[X]\\NP)>) (<T ((S\\NP)\\(S\\NP))\\((S\\NP)\\(S\\NP)) fa 0 2> (<L (((S\\NP)\\(S\\NP))\\((S\\NP)\\(S\\NP)))/S[dcl] when when WRB O I-ADVP (((S[X]\\NP)\\(S[X]\\NP))\\((S[X]\\NP)\\(S[X]\\NP)))/S[dcl]>) (<T S[dcl] ba 1 2> (<L NP we we PRP O I-NP NP>) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP rp 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP find find VBP O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N puppeteers puppeteer NNS O I-NP N>))) (<L , , , , O O ,>)) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP from from IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N Indonesia Indonesia NNP I-LOC I-NP N>)) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N India, India, NNP I-LOC I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]\\NP) who who WP O B-NP (NP\\NP)/(S[dcl]\\NP)>) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP begin begin VBP O I-VP (S[dcl]\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N their their PRP$ O I-NP NP[nb]/N>) (<L N show show NN O I-NP N>))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP with with IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N prayers prayer NNS O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP to to TO O I-PP (NP\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N ba 0 2> (<L N gods god NNS O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O O conj>) (<L N look look VB O I-VP N>)))))))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP upon upon IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N their their PRP$ O I-NP NP[nb]/N>) (<L N puppets puppet NNS O I-NP N>)))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP as as IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N divine divine NN O I-NP N/N>) (<L N manifestation manifestation NN O I-NP N>)))))))))))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "I went at the time the issue was printed ";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<L NP I I PRP O I-NP NP>) (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP went go VBD O I-VP S[dcl]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP at at IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N time time NN O I-NP N>)) (<T NP\\NP lex 0 1> (<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O B-NP NP[nb]/N>) (<L N issue issue NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L S[pss]\\NP printed print VBN O I-VP S[pss]\\NP>))))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Siva is the guy who is growing his hair long";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Siva Siva NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N guy guy NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]\\NP) who who WP O B-NP (NP\\NP)/(S[dcl]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[ng]\\NP) is be VBZ O I-VP (S[dcl]\\NP)/(S[ng]\\NP)>) (<T S[ng]\\NP ba 0 2> (<T S[ng]\\NP fa 0 2> (<L (S[ng]\\NP)/NP growing grow VBG O I-VP (S[ng]\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N his his PRP$ O I-NP NP[nb]/N>) (<L N hair hair NN O I-NP N>))) (<L (S\\NP)\\(S\\NP) long long RB O I-ADVP (S[X]\\NP)\\(S[X]\\NP)>)))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "John read books and Mary newpapers";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N John John NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP read read VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<T N ba 0 2> (<L N books book NNS O I-NP N>) (<T N[conj] conj 0 2> (<L conj and and CC O O conj>) (<T N fa 1 2> (<L N/N Mary Mary NNP I-PER I-NP N/N>) (<L N newspapers newspaper NNS O I-NP N>)))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "Cameron directed the Titanic , which was partly shot in Halifax , and is re-releasing the film on April 6 in 3-D with 15 new minutes .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Cameron Cameron NNP I-PER I-NP N>)) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP directed direct VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] rp 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N Titanic Titanic NNP I-LOC I-NP N>)) (<L , , , , O O ,>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]\\NP) which which WDT O I-NP (NP\\NP)/(S[dcl]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/(S[pss]\\NP) bx 0 2> (<L (S[dcl]\\NP)/(S[pss]\\NP) was be VBD O I-VP (S[dcl]\\NP)/(S[pss]\\NP)>) (<L (S\\NP)\\(S\\NP) partly partly RB O I-VP (S[X]\\NP)\\(S[X]\\NP)>)) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP shot shoot VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<L N Halifax Halifax NNP I-LOC I-NP N>)))))))) (<T S[dcl]\\NP[conj] conj 1 2> (<L , , , , O O ,>) (<T S[dcl]\\NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[ng]\\NP) is be VBZ O I-VP (S[dcl]\\NP)/(S[ng]\\NP)>) (<T S[ng]\\NP ba 0 2> (<T S[ng]\\NP ba 0 2> (<T S[ng]\\NP fa 0 2> (<L (S[ng]\\NP)/NP re-releasing re-release VBG O I-VP (S[ng]\\NP)/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N film film NN O I-NP N>))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP on on IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP lex 0 1> (<T N fa 0 2> (<L N/N[num] April April NNP I-DAT I-NP N/N[num]>) (<L N[num] 6 6 CD I-DAT I-NP N[num]>))))) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/(S[adj]\\NP) in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/(S[adj]\\NP)>) (<T S[adj]\\NP fa 0 2> (<L (S[adj]\\NP)/PP 3-D 3-D JJ O I-NP (S[adj]\\NP)/PP>) (<T PP fa 0 2> (<L PP/NP with with IN O I-PP PP/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N 15 15 CD O I-NP N/N>) (<T N fa 1 2> (<L N/N new new JJ O I-NP N/N>) (<L N minutes minute NNS O I-NP N>))))))))))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "The recent announcement by Titanic 's director James Cameron";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N recent recent JJ O I-NP N/N>) (<L N announcement announcement NN O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP by by IN O I-PP (NP\\NP)/NP>) (<T NP[nb] fa 1 2> (<T NP[nb]/N ba 1 2> (<T NP lex 0 1> (<L N Titanic Titanic NNP I-LOC I-NP N>)) (<L (NP[nb]/N)\\NP 's 's POS O B-NP (NP[nb]/N)\\NP>)) (<T N fa 1 2> (<L N/N director director NN O I-NP N/N>) (<T N fa 1 2> (<L N/N James James NNP I-PER I-NP N/N>) (<L N Cameron Cameron NNP I-PER I-NP N>))))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        "The recent announcement by Titanic director James Cameron that his forthcoming Avatar epic will be entirely shot in a new 3-D format further validates this";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP ba 0 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N The the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N recent recent JJ O I-NP N/N>) (<L N announcement announcement NN O I-NP N>))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP by by IN O I-PP (NP\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N Titanic Titanic NNP O I-NP N/N>) (<T N fa 1 2> (<L N/N director director NN O I-NP N/N>) (<T N fa 1 2> (<L N/N James James NNP I-PER I-NP N/N>) (<L N Cameron Cameron NNP I-PER I-NP N>))))))) (<T NP\\NP fa 0 2> (<L (NP\\NP)/S[dcl] that that IN O I-SBAR (NP\\NP)/S[dcl]>) (<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N his his PRP$ O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N forthcoming forthcoming JJ O I-NP N/N>) (<T N fa 1 2> (<L N/N Avatar Avatar NNP O I-NP N/N>) (<L N epic epic NN O I-NP N>)))) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[b]\\NP) will will MD O I-VP (S[dcl]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/(S[pss]\\NP) be be VB O I-VP (S[b]\\NP)/(S[pss]\\NP)>) (<T S[pss]\\NP fa 1 2> (<L (S\\NP)/(S\\NP) entirely entirely RB O I-VP (S[X]\\NP)/(S[X]\\NP)>) (<T S[pss]\\NP ba 0 2> (<L S[pss]\\NP shot shoot VBN O I-VP S[pss]\\NP>) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N new new JJ O I-NP N/N>) (<T N fa 1 2> (<L N/N 3-D 3-D JJ O I-NP N/N>) (<L N format format NN O I-NP N>)))))))))))) (<T S[dcl]\\NP fa 1 2> (<L (S\\NP)/(S\\NP) further further RB O I-ADVP (S[X]\\NP)/(S[X]\\NP)>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP validates validate VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N this this DT O I-NP N>)))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    // bad parse, but don't mind
    sent =
        "He spent a year in Vienna , Austria as an exchange student and studied robotics and mechatronics there .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<L NP He he PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP spent spend VBD O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT I-DAT I-NP NP[nb]/N>) (<L N year year NN I-DAT I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP in in IN I-DAT I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N Vienna Vienna NNP I-DAT I-NP N>)) (<T NP[conj] conj 0 2> (<L , , , , O O ,>) (<T NP ba 0 2> (<T NP ba 0 2> (<T NP lex 0 1> (<L N Austria Austria NNP I-LOC I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/NP as as IN O I-PP (NP\\NP)/NP>) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N an an DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N exchange exchange NN O I-NP N/N>) (<L N student student NN O I-NP N>))) (<T NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N studied study VBN O I-VP N/N>) (<L N robotics robotic NNS O I-NP N>))))))) (<T NP[conj] conj 0 2> (<L conj and and CC O I-NP conj>) (<T NP ba 0 2> (<T NP lex 0 1> (<L N mechatronics mechatronic NNS O I-NP N>)) (<L NP\\NP there there RB O I-ADVP NP\\NP>)))))))))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent = "Obama 's birthplace is Hawaii";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] ba 1 2> (<T NP[nb] fa 1 2> (<T NP[nb]/N ba 1 2> (<T NP lex 0 1> (<L N Obama Obama NNP I-LOC I-NP N>)) (<L (NP[nb]/N)\\NP 's 's IPOS O B-NP (NP[nb]/N)\\NP>)) (<L N birthplace birthplace NN O I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<L N Hawaii Hawaii NNP I-LOC I-NP N>))))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();
    relations = ccgParseTrees.get(1).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    sent =
        ", HQ_yesterday_morning , Google Co-Founder Sergey_Brin arrived late and looked casually dressed and very windblown .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] lp 1 2> (<L , , , , O O ,>) (<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<T N fa 1 2> (<L N/N HQ_yesterday_morning HQ_yesterday_morning NNP I-ORG I-NP N/N>) (<T N lp 1 2> (<L , , , , I-ORG O ,>) (<T N fa 1 2> (<L N/N Google Google NNP I-ORG I-NP N/N>) (<T N fa 1 2> (<L N/N Co-Founder Co-Founder NNP I-ORG I-NP N/N>) (<L N Sergey_Brin Sergey_Brin NNP I-ORG I-NP N>)))))) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP ba 0 2> (<L S[dcl]\\NP arrived arrive VBD O I-VP S[dcl]\\NP>) (<L (S\\NP)\\(S\\NP) late late JJ O I-ADJP (S[X]\\NP)\\(S[X]\\NP)>)) (<T S[dcl]\\NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[adj]\\NP) looked look VBD O I-VP (S[dcl]\\NP)/(S[adj]\\NP)>) (<T S[adj]\\NP ba 0 2> (<T S[adj]\\NP fa 1 2> (<L (S[adj]\\NP)/(S[adj]\\NP) casually casually RB O I-VP (S[adj]\\NP)/(S[adj]\\NP)>) (<L S[adj]\\NP dressed dress VBN O I-VP S[adj]\\NP>)) (<T S[adj]\\NP[conj] conj 0 2> (<L conj and and CC O O conj>) (<T S[adj]\\NP fa 1 2> (<L (S[adj]\\NP)/(S[adj]\\NP) very very RB O I-ADJP (S[adj]\\NP)/(S[adj]\\NP)>) (<L S[adj]\\NP windblown windblown JJ O I-ADJP S[adj]\\NP>)))))))) (<L . . . . O O .>)))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();

    try {
      sent =
          "I 'll concede that Microsoft_Office is probably the only above average quality product Microsoft ever came with , but Adobe ?";
      ccgParseTrees =
          ccgParser
              .parseFromString("(<T S[dcl] ba 1 2> (<L NP I I PRP O I-NP NP>) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/(S[b]\\NP) 'll 'll MD O I-VP (S[dcl]\\NP)/(S[b]\\NP)>) (<T S[b]\\NP fa 0 2> (<L (S[b]\\NP)/S[em] concede concede VB O I-VP (S[b]\\NP)/S[em]>) (<T S[em] fa 0 2> (<L S[em]/S[dcl] that that IN O I-SBAR S[em]/S[dcl]>) (<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Microsoft_Office Microsoft_Office NNP I-LOC I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<T (S[dcl]\\NP)/NP bx 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<L (S\\NP)\\(S\\NP) probably probably RB O I-ADVP (S[X]\\NP)\\(S[X]\\NP)>)) (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<T N fa 1 2> (<L N/N only only RB O I-NP N/N>) (<T N fa 1 2> (<L N/N above above JJ O I-NP N/N>) (<T N fa 1 2> (<L N/N average average JJ O I-NP N/N>) (<T N fa 1 2> (<L N/N quality quality NN O I-NP N/N>) (<L N product product NN O I-NP N>)))))) (<T NP\\NP ba 1 2> (<T NP\\NP lex 0 1> (<T S[dcl]/NP fc 1 2> (<T S/(S\\NP) tr 0 1> (<T NP lex 0 1> (<L N Microsoft Microsoft NNP I-ORG I-NP N>))) (<T (S[dcl]\\NP)/NP fc 1 2> (<L (S\\NP)/(S\\NP) ever ever RB O I-ADVP (S[X]\\NP)/(S[X]\\NP)>) (<T (S[dcl]\\NP)/NP fc 0 2> (<L (S[dcl]\\NP)/PP came come VBD O I-VP (S[dcl]\\NP)/PP>) (<L PP/NP with with IN O I-PP PP/NP>))))) (<T NP\\NP[conj] conj 0 2> (<L , , , , O O ,>) (<T NP[conj] conj 0 2> (<L conj but but CC O O conj>) (<T NP lex 0 1> (<L N Adobe Adobe NNP I-LOC I-NP N>)))))))) (<L . ? ? . O O .>))))))");
      relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
      System.out.println(sent);
      System.out.println(relations);
      System.out.println();
    } catch (BadParseException e) {
      // this sentence should fail
    }

    sent =
        "Heinz is a brand that has deep roots not just in the United_States but worldwide , Johnson said .";
    ccgParseTrees =
        ccgParser
            .parseFromString("(<T S[dcl] rp 0 2> (<T S[dcl] ba 1 2> (<T S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Heinz Heinz NNP I-PER I-NP N>)) (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP is be VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP ba 0 2> (<T NP ba 0 2> (<T NP[nb] fa 1 2> (<L NP[nb]/N a a DT O I-NP NP[nb]/N>) (<L N brand brand NN O I-NP N>)) (<T NP\\NP fa 0 2> (<L (NP\\NP)/(S[dcl]\\NP) that that WDT O B-NP (NP\\NP)/(S[dcl]\\NP)>) (<T S[dcl]\\NP ba 0 2> (<T S[dcl]\\NP fa 0 2> (<L (S[dcl]\\NP)/NP has have VBZ O I-VP (S[dcl]\\NP)/NP>) (<T NP lex 0 1> (<T N fa 1 2> (<L N/N deep deep JJ O I-NP N/N>) (<L N roots root NNS O I-NP N>)))) (<T (S\\NP)\\(S\\NP) fa 1 2> (<T ((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP)) fa 1 2> (<L (((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP)))/(((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP))) not not RB O I-CONJP (((S[X]\\NP)\\(S[X]\\NP))/((S[X]\\NP)\\(S[X]\\NP)))/(((S[X]\\NP)\\(S[X]\\NP))/((S[X]\\NP)\\(S[X]\\NP)))>) (<L ((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP)) just just RB O I-CONJP ((S[X]\\NP)\\(S[X]\\NP))/((S[X]\\NP)\\(S[X]\\NP))>)) (<T (S\\NP)\\(S\\NP) fa 0 2> (<L ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP>) (<T NP[nb] fa 1 2> (<L NP[nb]/N the the DT O I-NP NP[nb]/N>) (<L N United_States United_States NNS O I-NP N>))))))) (<T NP[conj] conj 0 2> (<L conj but but CC O I-NP conj>) (<T NP lex 0 1> (<L N worldwide worldwide NN O I-NP N>)))))) (<T S[dcl]\\S[dcl] lp 1 2> (<L , , , , O O ,>) (<T S[dcl]\\S[dcl] ba 1 2> (<T NP lex 0 1> (<L N Johnson Johnson NNP I-PER I-NP N>)) (<L (S[dcl]\\S[dcl])\\NP said say VBD O I-VP (S[dcl]\\S[dcl])\\NP>)))) (<L . . . . O O .>))");
    relations = ccgParseTrees.get(0).getLexicalisedSemanticPredicates();
    System.out.println(sent);
    System.out.println(relations);
    System.out.println();



    // Unhandled or wrong cases
    // copula preceded by relative pronoun - cities that are not capitals
    // (wrong due to the modification of be category from (S{_}\N{1})/N{2}
    // to (S{1}\N{1})/N{2})


  }
}
