package uk.ac.ed.easyccg.syntax;

import java.text.DecimalFormat;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import uk.ac.ed.easyccg.main.EasyCCG.CommandLineArguments;
import uk.ac.ed.easyccg.main.EasyCCG.InputFormat;
import uk.ac.ed.easyccg.syntax.InputReader.InputWord;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeLeaf;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

public abstract class Tagger
{

  /**
   * Assigned a distribution over lexical categories for a list of words.
   * For each word in the sentence, it returns an ordered list of SyntaxTreeNode representing
   * their category assignment.
   */
  public abstract List<List<SyntaxTreeNodeLeaf>> tag(List<InputWord> words);

  /**
   * Not-really-supported function for getting supertags out.
   * @throws ArgumentValidationException 
   */
  public static void main(String[] args) throws ArgumentValidationException {
    CommandLineArguments parsedArgs = CliFactory.parseArguments(CommandLineArguments.class, args);

    if (!parsedArgs.getModel().exists()) throw new InputMismatchException("Couldn't load model from from: " + parsedArgs.getModel());
    System.err.println("Loading model...");

    Tagger tagger = new TaggerEmbeddings(parsedArgs.getModel(), parsedArgs.getMaxLength(), parsedArgs.getSupertaggerbeam(), parsedArgs.getMaxTagsPerWord());

    // Read from stdin
    Scanner sc = new Scanner(System.in,"UTF-8");
    System.err.println("Model loaded, ready to parse.");
    DecimalFormat df = new DecimalFormat("#.########");
    Stopwatch timer = Stopwatch.createStarted();

    InputReader inputReader = InputReader.make(InputFormat.POSTAGGED, new SyntaxTreeNodeFactory(parsedArgs.getMaxLength(), 0));
    int id = 0;
    while(sc.hasNext()) {
      String line = sc.nextLine().trim();

      if (line.startsWith("#") || line.isEmpty()) continue ;
      
      
      List<List<SyntaxTreeNodeLeaf>> supertags = tagger.tag(inputReader.readInput(line).getInputWords());
      id++;
      for (int i=0 ; i < supertags.size(); i++) {
        List<SyntaxTreeNodeLeaf> tagsForWord = supertags.get(i);
        boolean isFirstTag = true;

        if (i > 0) {
          System.out.print("|");
        }

        for (SyntaxTreeNodeLeaf tagForWord : tagsForWord) {
          if (isFirstTag) {
            System.out.print(tagForWord.getWord() + "\t" + tagForWord.getPos() + "\t0");
            isFirstTag = false;
          }            

          System.out.print("\t" + tagForWord.getCategory() + "\t" + df.format(Math.exp(tagForWord.probability)));
        }

      }
      System.out.println();
      
    }
    
    System.err.println("Time taken: " + timer.elapsed(TimeUnit.SECONDS));
  }
}