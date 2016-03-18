package others;

import java.io.File;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Collection;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import edu.uw.easysrl.main.EasySRL.CommandLineArguments;
import edu.uw.easysrl.main.EasySRL.InputFormat;
import edu.uw.easysrl.main.EasySRL.OutputFormat;
import edu.uw.easysrl.main.InputReader;
import edu.uw.easysrl.main.ParsePrinter;
import edu.uw.easysrl.syntax.grammar.SyntaxTreeNode;
import edu.uw.easysrl.syntax.model.SupertagFactoredModel.SupertagFactoredModelFactory;
import edu.uw.easysrl.syntax.parser.Parser;
import edu.uw.easysrl.syntax.parser.ParserAStar;
import edu.uw.easysrl.syntax.tagger.Tagger;
import edu.uw.easysrl.syntax.tagger.TaggerEmbeddings;
import edu.uw.easysrl.util.Util.Scored;
import edu.uw.easysrl.util.Util;
import edu.uw.easysrl.syntax.grammar.Category;

public class EasySRLCli implements CcgSyntacticParserCli {

  final Parser parser;
  final ParsePrinter printer;
  final InputReader reader;

  public EasySRLCli(final String model, final Integer nbest)
      throws IOException, ArgumentValidationException {
    final List<String> argsList =
        Lists
            .newArrayList(Splitter
                .on(CharMatcher.WHITESPACE)
                .trimResults()
                .omitEmptyStrings()
                .split(
                    String
                        .format(
                            "-m %s -i POSandNERtagged --outputFormat extended --nbest %d",
                            model, nbest)).iterator());

    final String[] args = argsList.toArray(new String[argsList.size()]);
    final CommandLineArguments commandLineOptions =
        CliFactory.parseArguments(CommandLineArguments.class, args);
    final InputFormat input =
        InputFormat.valueOf(commandLineOptions.getInputFormat().toUpperCase());
    final File modelFolder = Util.getFile(commandLineOptions.getModel());

    if (!modelFolder.exists()) {
      throw new InputMismatchException("Couldn't load model from from: "
          + modelFolder);
    }

    System.err.println("====Starting loading model====");
    final OutputFormat outputFormat =
        OutputFormat
            .valueOf(commandLineOptions.getOutputFormat().toUpperCase());
    printer = outputFormat.printer;

    Collection<Category> lexicalCategories =
        TaggerEmbeddings.loadCategories(new File(modelFolder, "categories"));
    parser =
        new ParserAStar(new SupertagFactoredModelFactory(Tagger.make(
            modelFolder, commandLineOptions.getSupertaggerbeam(), 50, null),
            lexicalCategories, false), commandLineOptions.getMaxLength(),
            nbest, commandLineOptions.getRootCategories(), modelFolder, 100000);

    if ((outputFormat == OutputFormat.PROLOG || outputFormat == OutputFormat.EXTENDED)
        && input != InputFormat.POSANDNERTAGGED) {
      throw new Error("Must use \"-i POSandNERtagged\" for this output");
    }
    reader =
        InputReader.make(InputFormat.valueOf(commandLineOptions
            .getInputFormat().toUpperCase()));
  }

  public List<String> parse(final String line) throws IOException,
      ArgumentValidationException, InterruptedException {
    final List<String> parseStrings = Lists.newArrayList();

    final List<Scored<SyntaxTreeNode>> parses =
        parser.doParsing(reader.readInput(line));

    if (parses == null) {
      return parseStrings;
    }
    for (final Scored<SyntaxTreeNode> parse : parses) {
      parseStrings.add(printer.print(parse.getObject(), -1));
    }
    return parseStrings;
  }

  public static void main(final String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {

    final EasySRLCli easysrl =
        new EasySRLCli("model_ccgbank_questions --rootCategories S[wq] S[q]", 2);
    System.out.println(easysrl.parse("Who|WP|O won|VB|O ?|.|O"));
  }
}
