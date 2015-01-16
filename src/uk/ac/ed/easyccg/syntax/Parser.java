package uk.ac.ed.easyccg.syntax;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Multimap;

import uk.ac.ed.easyccg.syntax.InputReader.InputToParser;
import uk.ac.ed.easyccg.syntax.ParserAStar.SuperTaggingResults;

public interface Parser
{

  /**
   * Parses a sentence, interpreting the line according to the InputReader, and returns an N-best list.
   */
  public abstract List<SyntaxTreeNode> parse(String line);

  /**
   * Ignores the InputReader and parses the supplied list of words.
   */
  public abstract List<SyntaxTreeNode> parseTokens(List<String> words);

  /**
   * 
   */
  public abstract Iterator<List<SyntaxTreeNode>> parseFile(File file, final SuperTaggingResults results)
      throws IOException;

  public abstract List<SyntaxTreeNode> parseSentence(SuperTaggingResults results, InputToParser input);

  public abstract List<SyntaxTreeNode> doParsing(InputToParser input);

  List<SyntaxTreeNode> parse(SuperTaggingResults results, String line);

  Multimap<Integer, Long> getSentenceLengthToParseTimeInNanos();

  long getParsingTimeOnlyInMillis();

  long getTaggingTimeOnlyInMillis();

}