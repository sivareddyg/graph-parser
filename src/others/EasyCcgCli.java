package others;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

import uk.ac.ed.easyccg.main.EasyCCG.CommandLineArguments;
import uk.ac.ed.easyccg.main.EasyCCG.InputFormat;
import uk.ac.ed.easyccg.main.EasyCCG.OutputFormat;
import uk.ac.ed.easyccg.syntax.Category;
import uk.ac.ed.easyccg.syntax.InputReader;
import uk.ac.ed.easyccg.syntax.ParsePrinter;
import uk.ac.ed.easyccg.syntax.Parser;
import uk.ac.ed.easyccg.syntax.ParserAStar;
import uk.ac.ed.easyccg.syntax.ParserAStar.SuperTaggingResults;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;
import uk.ac.ed.easyccg.syntax.TagDict;
import uk.ac.ed.easyccg.syntax.TaggerEmbeddings;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class EasyCcgCli {

  Parser parser;
  ParsePrinter printer;

  public EasyCcgCli(String model, Integer nbest) throws IOException,
      ArgumentValidationException {

    List<String> argsList =
        Lists.newArrayList(Splitter
            .on(CharMatcher.WHITESPACE)
            .trimResults()
            .omitEmptyStrings()
            .split(
                String.format("-m %s -i POSandNERtagged -o extended -n %d",
                    model, nbest)).iterator());
    String[] args = argsList.toArray(new String[argsList.size()]);
    CommandLineArguments commandLineOptions =
        CliFactory.parseArguments(CommandLineArguments.class, args);

    InputFormat input =
        InputFormat.valueOf(commandLineOptions.getInputFormat().toUpperCase());

    if (commandLineOptions.getMakeTagDict()) {
      InputReader reader =
          InputReader.make(input,
              new SyntaxTreeNodeFactory(commandLineOptions.getMaxLength(), 0));
      Map<String, Collection<Category>> tagDict =
          TagDict.makeDict(reader.readFile(commandLineOptions.getInputFile()));
      TagDict.writeTagDict(tagDict, commandLineOptions.getModel());
      System.exit(0);
    }

    if (!commandLineOptions.getModel().exists())
      throw new InputMismatchException("Couldn't load model from from: "
          + commandLineOptions.getModel());
    System.err.println("Loading model...");

    parser =
        new ParserAStar(new TaggerEmbeddings(commandLineOptions.getModel(),
            commandLineOptions.getMaxLength(),
            commandLineOptions.getSupertaggerbeam(),
            commandLineOptions.getMaxTagsPerWord()),
            commandLineOptions.getMaxLength(), commandLineOptions.getNbest(),
            commandLineOptions.getNbestbeam(), input,
            commandLineOptions.getRootCategories(), new File(
                commandLineOptions.getModel(), "unaryRules"), new File(
                commandLineOptions.getModel(), "binaryRules"),
            commandLineOptions.getUnrestrictedRules() ? null : new File(
                commandLineOptions.getModel(), "seenRules"));
    OutputFormat outputFormat =
        OutputFormat
            .valueOf(commandLineOptions.getOutputFormat().toUpperCase());
    printer = outputFormat.printer;

  }

  public List<String> parse(String line) throws IOException,
      ArgumentValidationException, InterruptedException {
    List<String> parseStrings = Lists.newArrayList();
    final SuperTaggingResults supertaggingResults = new SuperTaggingResults();
    List<SyntaxTreeNode> parses = parser.parse(supertaggingResults, line);
    if (parses == null) return parseStrings;
    for (SyntaxTreeNode parse : parses) {
      parseStrings.add(printer.print(parse, -1));
    }
    return parseStrings;
  }


  public static void main(String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {
    EasyCcgCli easyccg =
        new EasyCcgCli("lib_data/easyccg_model -r S[dcl] S[pss] S[b]", 2);
    System.out.println(easyccg.parse("He|PRP|O won|VB|O"));
  }
}
