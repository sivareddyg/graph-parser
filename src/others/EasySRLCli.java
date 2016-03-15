package others;

import java.io.File;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import edu.uw.easysrl.main.EasySRL;
import edu.uw.easysrl.main.EasySRL.CommandLineArguments;
import edu.uw.easysrl.main.EasySRL.InputFormat;
import edu.uw.easysrl.main.EasySRL.OutputFormat;
import edu.uw.easysrl.main.EasySRL.ParsingAlgorithm;
import edu.uw.easysrl.main.InputReader;
import edu.uw.easysrl.main.ParsePrinter;
import edu.uw.easysrl.syntax.evaluation.CCGBankEvaluation;
import edu.uw.easysrl.syntax.grammar.SyntaxTreeNode;
import edu.uw.easysrl.syntax.parser.Parser;
import edu.uw.easysrl.syntax.parser.SRLParser;
import edu.uw.easysrl.syntax.parser.SRLParser.PipelineSRLParser;
import edu.uw.easysrl.syntax.tagger.POSTagger;
import edu.uw.easysrl.syntax.training.PipelineTrainer.LabelClassifier;
import edu.uw.easysrl.util.Util;
import edu.uw.easysrl.util.Util.Scored;
import edu.uw.easysrl.syntax.parser.SRLParser.CCGandSRLparse;

public class EasySRLCli {

  final SRLParser parser;
  final ParsePrinter printer;
  final InputReader reader;

  public EasySRLCli(String model, Integer nbest) throws IOException,
      ArgumentValidationException {
    List<String> argsList =
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

    String[] args = argsList.toArray(new String[argsList.size()]);
    CommandLineArguments commandLineOptions =
        CliFactory.parseArguments(CommandLineArguments.class, args);
    InputFormat input =
        InputFormat.valueOf(commandLineOptions.getInputFormat().toUpperCase());
    File modelFolder = Util.getFile(commandLineOptions.getModel());

    if (!modelFolder.exists()) {
      throw new InputMismatchException("Couldn't load model from from: "
          + modelFolder);
    }

    System.err.println("====Starting loading model====");
    final OutputFormat outputFormat =
        OutputFormat
            .valueOf(commandLineOptions.getOutputFormat().toUpperCase());
    printer = outputFormat.printer;

    parser =
        makePipelineParser(modelFolder, commandLineOptions, 0.000001, false);

    if ((outputFormat == OutputFormat.PROLOG || outputFormat == OutputFormat.EXTENDED)
        && input != InputFormat.POSANDNERTAGGED) {
      throw new Error("Must use \"-i POSandNERtagged\" for this output");
    }
    reader =
        InputReader.make(InputFormat.valueOf(commandLineOptions
            .getInputFormat().toUpperCase()));
  }

  public List<String> parse(String line) throws IOException,
      ArgumentValidationException, InterruptedException {
    List<String> parseStrings = Lists.newArrayList();
    
    final List<CCGandSRLparse> parses =
        parser.parseTokens(reader.readInput(line).getInputWords());

    if (parses == null)
      return parseStrings;
    for (CCGandSRLparse parse : parses) {
      parseStrings.add(printer.print(parse.getCcgParse(), -1));
    }
    return parseStrings;
  }

  private static PipelineSRLParser makePipelineParser(final File folder,
      final CommandLineArguments commandLineOptions,
      final double supertaggerBeam, final boolean outputDependencies)
      throws IOException {
    final POSTagger posTagger =
        POSTagger.getStanfordTagger(new File(folder, "posTagger"));
    final File labelClassifier = new File(folder, "labelClassifier");
    final LabelClassifier classifier =
        labelClassifier.exists() && outputDependencies ? Util
            .deserialize(labelClassifier)
            : CCGBankEvaluation.dummyLabelClassifier;

    return new PipelineSRLParser(EasySRL.makeParser(folder.getAbsolutePath(),
        supertaggerBeam, ParsingAlgorithm.ASTAR, 100000, false,
        Optional.empty(), commandLineOptions.getNbest(),
        commandLineOptions.getMaxLength()), classifier, posTagger);
  }

  public static void main(String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {
    EasySRLCli easysrl = new EasySRLCli("model_ccgbank_questions --rootCategories S[wq] S[q]", 2);
    System.out.println(easysrl.parse("He|Hell|O won|VB|O"));
  }
}
