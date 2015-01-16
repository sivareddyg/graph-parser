package uk.ac.ed.easyccg.rebanking;

import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.ed.easyccg.syntax.Category;
import uk.ac.ed.easyccg.syntax.Category.Slash;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;
import uk.ac.ed.easyccg.syntax.Util;

/**
 * Reads in gold-standard parses from CCGBank. 
 */
public class CCGBankParseReader
{

  public final static SyntaxTreeNodeFactory factory = new SyntaxTreeNodeFactory(1000, 10000);
  
  private final static String OPEN_BRACKET = "(<";
  private final static String OPEN_LEAF = "(<L ";
  private final static String SPLIT_REGEX = " |>\\)";

  public static SyntaxTreeNode parse(String input) {
    return parse(input, new AtomicInteger(0));
  }

  
  private static SyntaxTreeNode parse(String input, AtomicInteger wordIndex)
  {
    int closeBracket = Util.findClosingBracket(input, 0);
    int nextOpenBracket = input.indexOf(OPEN_BRACKET, 1);

    SyntaxTreeNode result;
    
    if (input.startsWith(OPEN_LEAF))
    {
      //LEAF NODE
      String[] parse = input.split(SPLIT_REGEX);
      
      if (parse.length < 6) {
        return null;
      }

      result = factory.makeTerminal(new String(parse[4]), Category.valueOf(parse[1]), new String(parse[2]), null, 1.0, wordIndex.getAndIncrement());
    }
    else
    {
      int subtermCloseBracket = Util.findClosingBracket(input, nextOpenBracket);

      SyntaxTreeNode child1 = parse(input.substring(nextOpenBracket, Util.findClosingBracket(input, nextOpenBracket) + 1), wordIndex);
      
      int catEndIndex = input.indexOf(' ', 4);
      String catString = input.substring(4, catEndIndex);

      Category cat;
      if (catString.endsWith("[conj]")) {
        Category c = Category.valueOf(catString.substring(0, catString.lastIndexOf("[conj]")));
        cat = Category.make(c, Slash.BWD, c);
      } else {
        cat = Category.valueOf(catString);
      }

      
      int headIndex = Integer.parseInt(input.substring(catEndIndex + 1, catEndIndex + 2));
      
      if (subtermCloseBracket == closeBracket - 2 ||
          subtermCloseBracket == closeBracket - 1)
      {
        //Unary
        result = factory.makeUnary(cat, child1);
        
      }
      else
      {
        //Binary
        String childString = input.substring(subtermCloseBracket + 2, Util.findClosingBracket(input, subtermCloseBracket + 2) + 1);
        SyntaxTreeNode child2 = parse(childString, wordIndex);

        result = factory.makeBinary(cat, child1, child2, null, headIndex == 0);
      }
    }
    
    return result;
  }


}
