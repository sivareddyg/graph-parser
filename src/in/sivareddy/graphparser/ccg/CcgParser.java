package in.sivareddy.graphparser.ccg;

/**
 * Class which provides all the functions to create ccg parse trees and create
 * semantic parses
 *
 * @author Siva Reddy
 *
 */
public class CcgParser extends CcgParseTree {
  public CcgParser(CcgAutoLexicon lexicon, String[] relationLexicalIdentifiers,
      String[] argumentLexicalIdenfiers, String[] relationTypingIdentifiers,
      boolean ignorePronouns) {
    super();
    autoLexicon = lexicon;
    RELATION_IDENTIFIERS = relationLexicalIdentifiers;
    ARGUMENT_IDENTIFIERS = argumentLexicalIdenfiers;
    RELATION_TYPING_IDENTIFIERS = relationTypingIdentifiers;
    IGNOREPRONOUNS = ignorePronouns;
  }
}
