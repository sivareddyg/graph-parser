package uk.ac.ed.easyccg.syntax.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import uk.ac.ed.easyccg.syntax.ParsePrinter;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeLeaf;
import uk.ac.ed.easyccg.syntax.Util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * Code for working with CCGBank-style dependencies. 
 */
public class CCGBankDependencies
{
  private final static File candcFolder = new File("/disk/data2/s1049478/my_parser/candc"); //TODO
  private final static ParsePrinter autoPrinter = ParsePrinter.CCGBANK_PRINTER;
    
  /**
   * Runs the C&C generate program on a list of parses for a sentence, and returns the output.
   */
  public static String getDependenciesAsString(List<SyntaxTreeNode> parses, int id) {

    try {
      File candcScripts = new File(candcFolder,  "/src/scripts/ccg");

      File catsFolder = new File(candcFolder,  "/src/data/ccg/cats/");
      File markedUpCategories = new File(catsFolder,  "/markedup");

      File autoFile = File.createTempFile("parse", ".auto");
      File convertedFile = File.createTempFile("parse", ".auto2");

      String autoString = autoPrinter.print(parses, id);
      Util.writeStringToFile(autoString, autoFile);
      String command = "cat \"" + autoFile + "\" | "  +candcScripts + "/convert_auto " + autoFile + " | sed -f " + candcScripts + "/convert_brackets > " + convertedFile;
      String command2 = candcFolder + "/bin/generate -j  " + catsFolder + " " + markedUpCategories + " " + convertedFile;
      Util.executeCommand(command);
      String deps = Util.executeCommand(command2);
      
      autoFile.delete();
      convertedFile.delete();
      
      return deps;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Runs the C&C generate program on a list of parses for a sentence, and returns the output.
   * 
   * Failed parses are represented as 'null'.
   */
  public static List<DependencyParse> getDependencies(List<SyntaxTreeNode> parses, int id) {
    
    String output = getDependenciesAsString(parses, id);
    List<String> lines = Arrays.asList(output.split("\n", -1));
    if (lines.size() == 2) {
      return Arrays.asList((CCGBankDependencies.DependencyParse) null);
    }
    
    List<DependencyParse> result = new ArrayList<DependencyParse>();
    // Skip the C&C header.
    Iterator<String> linesIt = lines.subList(3, lines.size()).iterator();
    Iterator<SyntaxTreeNode> parseIt = parses.iterator();

    while (linesIt.hasNext() && parseIt.hasNext()) {
      if (!parseIt.hasNext()) {
        throw new RuntimeException("More dependency parses than input");
      }
      result.add(getDependencyParseCandC(linesIt, parseIt.next().getWords()));
    }

    if (result.size() != parses.size()) {
      throw new RuntimeException("Error in oracle parses");
    }
    return result;
  }

  /**
   * Represents a CCGBank dependency.
   */
  public static class Dependency {

    private final SyntaxTreeNodeLeaf parent;
    private final SyntaxTreeNodeLeaf child;
    private final int argumentNumber;
    private final int sentencePositionOfPredicate;

    public int getSentencePositionOfPredicate()
    {
      return sentencePositionOfPredicate;
    }
    public int getSentencePositionOfArgument()
    {
      return sentencePositionOfArgument;
    }
    private final int sentencePositionOfArgument;
    private Dependency(SyntaxTreeNodeLeaf parent, SyntaxTreeNodeLeaf child, int index, int sentencePositionOfPredicate, int sentencePositionOfArgument)
    {
      this.parent = parent;
      this.child = child;
      this.argumentNumber = index;
      this.sentencePositionOfPredicate = sentencePositionOfPredicate;
      this.sentencePositionOfArgument = sentencePositionOfArgument;
    }
    public SyntaxTreeNodeLeaf getParent()
    {
      return parent;
    }
    public SyntaxTreeNodeLeaf getChild()
    {
      return child;
    }
    public int getArgNumber()
    {
      return argumentNumber;
    }

  }

  /**
   * Represents a complete dependency parse of a sentence.
   */
  public static class DependencyParse {
    private final Table<SyntaxTreeNodeLeaf, Integer, Collection<Dependency>> arguments = HashBasedTable.create();
    private final Multimap<SyntaxTreeNodeLeaf, Dependency> predicates = HashMultimap.create();
    private final Collection<Dependency> allDependencies = new HashSet<Dependency>();

    void addDependency(Dependency dep) {
      Collection<Dependency> args = arguments.get(dep.parent, dep.getArgNumber());
      if (args == null) {
        args = new ArrayList<Dependency>();
        arguments.put(dep.parent, dep.getArgNumber(), args);
      }

      args.add(dep);

      predicates.put(dep.getChild(), dep);
      allDependencies.add(dep);
    }

    public Collection<Dependency> getDependencies() {
      return allDependencies;
    }

    public Collection<Dependency> getArguments(SyntaxTreeNodeLeaf node, int argumentIndex)
    {
      Collection<Dependency> result = arguments.get(node, argumentIndex);
      if (result == null) return Collections.emptyList();
      return result;
    }

    public Collection<Dependency> getPredicates(SyntaxTreeNodeLeaf argument)
    {
      return predicates.get(argument);
    }
  }

  /**
   * Builds a DependencyParse, from C&C 'deps' output. 
   * 
   * 'lines' is an iterator over input lines. The function reads one parse from this iterator.
   */
  public static DependencyParse getDependencyParseCandC(Iterator<String> lines, List<SyntaxTreeNodeLeaf> goldSupertags) {
    // Pierre_1 N/N 1 Vinken_2
    DependencyParse result = new DependencyParse();
    
    while (lines.hasNext()) {
      String line = lines.next();

      if (line.isEmpty()) {
        if (result.getDependencies().size() == 0) {
          return null;
        }
        break ;
      }

      String[] fields = line.split(" ");
      int predicate = Integer.valueOf(fields[0].substring(fields[0].indexOf('_') + 1));
      int argIndex = Integer.valueOf(fields[2].trim());
      int argument = Integer.valueOf(fields[3].substring(fields[3].indexOf('_') + 1));
      result.addDependency(new Dependency(goldSupertags.get(predicate - 1), goldSupertags.get(argument - 1), argIndex, predicate, argument));
    }

    return result;
  }

  /**
   * Builds a DependencyParse, from CCGBank PARG format.
   * 
   * 'lines' is an iterator over input lines. The function reads one parse from this iterator.
   */
  public static DependencyParse getDependencyParseCCGBank(Iterator<String> lines, List<SyntaxTreeNodeLeaf> words) {
    /*    
    <s id="wsj_0001.1"> 17
    1        0       N/N     1       Vinken Pierre
    1        5       (S[adj]\NP)\NP          1       Vinken old <XB>
    1        7       (S[dcl]\NP)/(S[b]\NP)   1       Vinken will
    1        8       (S[b]\NP)/NP    1       Vinken join <XB>
    10       8       (S[b]\NP)/NP    2       board join
    10       9       NP/N    1       board the
    14       11      ((S\NP)\(S\NP))/NP      3       director as
    14       12      NP/N    1       director a
    14       13      N/N     1       director nonexecutive
    16       15      ((S\NP)\(S\NP))/N[num]          3       29 Nov.
    4        3       N/N     1       years 61
    4        5       (S[adj]\NP)\NP          2       years old
    8        11      ((S\NP)\(S\NP))/NP      2       join as
    8        15      ((S\NP)\(S\NP))/N[num]          2       join Nov.
    8        7       (S[dcl]\NP)/(S[b]\NP)   2       join will
    <\s>
     */
    DependencyParse result = new DependencyParse();

    // Header line
    String line = lines.next();
    while (lines.hasNext()) {
      line = lines.next();

      if (line.startsWith("<\\s>")) {
        break ;
      }

      String[] fields = line.split("\t");
      int argument = Integer.valueOf(fields[0].trim());
      int predicate = Integer.valueOf(fields[1].trim());
      int argIndex = Integer.valueOf(fields[3].trim());
      result.addDependency(new Dependency(words.get(predicate), words.get(argument), argIndex, predicate, argument));
    }

    return result;
  }
}
