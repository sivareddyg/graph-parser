package uk.ac.ed.easyccg.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import uk.ac.ed.easyccg.syntax.Category;
import uk.ac.ed.easyccg.syntax.InputReader;
import uk.ac.ed.easyccg.syntax.InputReader.InputToParser;
import uk.ac.ed.easyccg.syntax.ParsePrinter;
import uk.ac.ed.easyccg.syntax.Parser;
import uk.ac.ed.easyccg.syntax.ParserAStar;
import uk.ac.ed.easyccg.syntax.ParserAStar.SuperTaggingResults;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeFactory;
import uk.ac.ed.easyccg.syntax.TagDict;
import uk.ac.ed.easyccg.syntax.TaggerEmbeddings;
import uk.ac.ed.easyccg.syntax.Util;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.DependencyParse;
import uk.ac.ed.easyccg.syntax.evaluation.Evaluate;
import uk.ac.ed.easyccg.syntax.evaluation.Evaluate.Results;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;
import uk.co.flamingpenguin.jewel.cli.Option;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class EasyCCG {

	/**
	 * Command Line Interface
	 */
	public interface CommandLineArguments {
		@Option(shortName = "m", description = "Path to the parser model")
		File getModel();

		@Option(shortName = "f", defaultValue = "", description = "(Optional) Path to the input text file. Otherwise, the parser will read from stdin.")
		File getInputFile();

		@Option(shortName = "i", defaultValue = "tokenized", description = "(Optional) Input Format: one of \"tokenized\", \"POStagged\", \"POSandNERtagged\", \"gold\", \"deps\" or \"supertagged\"")
		String getInputFormat();

		@Option(shortName = "o", description = "Output Format: one of \"ccgbank\", \"html\", or \"prolog\"", defaultValue = "ccgbank")
		String getOutputFormat();

		@Option(shortName = "l", defaultValue = "70", description = "(Optional) Maximum length of sentences in words. Defaults to 70.")
		int getMaxLength();

		@Option(shortName = "n", defaultValue = "1", description = "(Optional) Number of parses to return per sentence. Defaults to 1.")
		int getNbest();

		@Option(shortName = "r", defaultValue = { "S[dcl]", "S[wq]", "S[q]",
				"S[qem]", "NP" }, description = "(Optional) List of valid categories for the root node of the parse. Defaults to: S[dcl] S[wq] S[q] NP")
		List<String> getRootCategories();

		@Option(shortName = "s", description = "(Optional) Allow rules not involving category combinations seen in CCGBank. Slows things down by around 20%.")
		boolean getUnrestrictedRules();

		@Option(description = "(Optional) If true, print detailed timing information.")
		boolean getTiming();

		@Option(defaultValue = "0.0001", description = "(Optional) Prunes lexical categories whose probability is less than this ratio of the best category. Defaults to 0.0001.")
		double getSupertaggerbeam();

		@Option(defaultValue = "50", description = "(Optional) Maximum number of categores per word output by the supertagger. Defaults to 50.")
		int getMaxTagsPerWord();

		@Option(defaultValue = "0.0", description = "(Optional) If using N-best parsing, filter parses whose probability is lower than this fraction of the probability of the best parse. Defaults to 0.0")
		double getNbestbeam();

		@Option(defaultValue = "1", description = "(Optional) Number of threads to use. If greater than 1, the output order may differ from the input.")
		int getThreads();

		@Option(defaultValue = "", description = "(Optional) Gold dependencies file, for use in oracle experiments")
		File getGoldDependenciesFile();

		@Option(helpRequest = true, description = "Display this message", shortName = "h")
		boolean getHelp();

		@Option(description = "(Optional) Make a tag dictionary")
		boolean getMakeTagDict();

	}

	// Set of supported InputFormats
	public enum InputFormat {
		TOKENIZED, GOLD, SUPERTAGGED, POSTAGGED, POSANDNERTAGGED
	}

	// Set of supported OutputFormats
	public enum OutputFormat {
		CCGBANK(ParsePrinter.CCGBANK_PRINTER), HTML(ParsePrinter.HTML_PRINTER), SUPERTAGS(
				ParsePrinter.SUPERTAG_PRINTER), PROLOG(
				ParsePrinter.PROLOG_PRINTER), EXTENDED(
				ParsePrinter.EXTENDED_CCGBANK_PRINTER), DEPS(
				new ParsePrinter.DependenciesPrinter());

		public final ParsePrinter printer;

		OutputFormat(ParsePrinter printer) {
			this.printer = printer;
		}
	}

	final Parser parser;
	final ParsePrinter printer;

	public EasyCCG(String model, Integer nbest) throws IOException,
			ArgumentValidationException {
		List<String> argsList = Lists.newArrayList(Splitter
				.on(CharMatcher.WHITESPACE)
				.trimResults()
				.omitEmptyStrings()
				.split(String.format(
						"-m %s -i POSandNERtagged -o extended -n %d", model,
						nbest)).iterator());
		String[] args = argsList.toArray(new String[argsList.size()]);
		CommandLineArguments commandLineOptions = CliFactory.parseArguments(
				CommandLineArguments.class, args);

		InputFormat input = InputFormat.valueOf(commandLineOptions
				.getInputFormat().toUpperCase());

		if (commandLineOptions.getMakeTagDict()) {
			InputReader reader = InputReader.make(input,
					new SyntaxTreeNodeFactory(
							commandLineOptions.getMaxLength(), 0));
			Map<String, Collection<Category>> tagDict = TagDict.makeDict(reader
					.readFile(commandLineOptions.getInputFile()));
			TagDict.writeTagDict(tagDict, commandLineOptions.getModel());
			System.exit(0);
		}

		if (!commandLineOptions.getModel().exists())
			throw new InputMismatchException("Couldn't load model from from: "
					+ commandLineOptions.getModel());
		System.err.println("Loading model...");

		parser = new ParserAStar(new TaggerEmbeddings(
				commandLineOptions.getModel(),
				commandLineOptions.getMaxLength(),
				commandLineOptions.getSupertaggerbeam(),
				commandLineOptions.getMaxTagsPerWord()),
				commandLineOptions.getMaxLength(),
				commandLineOptions.getNbest(),
				commandLineOptions.getNbestbeam(), input,
				commandLineOptions.getRootCategories(), new File(
						commandLineOptions.getModel(), "unaryRules"), new File(
						commandLineOptions.getModel(), "binaryRules"),
				commandLineOptions.getUnrestrictedRules() ? null : new File(
						commandLineOptions.getModel(), "seenRules"));
		OutputFormat outputFormat = OutputFormat.valueOf(commandLineOptions
				.getOutputFormat().toUpperCase());
		printer = outputFormat.printer;

	}

	public List<String> parse(String line) throws IOException,
			ArgumentValidationException, InterruptedException {
		List<String> parseStrings = Lists.newArrayList();
		final SuperTaggingResults supertaggingResults = new SuperTaggingResults();
		List<SyntaxTreeNode> parses = parser.parse(supertaggingResults, line);
		for (SyntaxTreeNode parse : parses) {
			parseStrings.add(printer.print(parse, -1));
		}
		return parseStrings;
	}

	public static void main(String[] args) throws IOException,
			ArgumentValidationException, InterruptedException {

		EasyCCG easyccg = new EasyCCG("lib_data/easyccg_model -r S[dcl] S[pss] S[b]", 2);
		System.out.println(easyccg.parse("He|PRP|O won|VB|O"));

		/*-CommandLineArguments commandLineOptions = CliFactory.parseArguments(
				CommandLineArguments.class, args);
		InputFormat input = InputFormat.valueOf(commandLineOptions
				.getInputFormat().toUpperCase());

		if (commandLineOptions.getMakeTagDict()) {
			InputReader reader = InputReader.make(input,
					new SyntaxTreeNodeFactory(
							commandLineOptions.getMaxLength(), 0));
			Map<String, Collection<Category>> tagDict = TagDict.makeDict(reader
					.readFile(commandLineOptions.getInputFile()));
			TagDict.writeTagDict(tagDict, commandLineOptions.getModel());
			System.exit(0);
		}

		if (!commandLineOptions.getModel().exists())
			throw new InputMismatchException("Couldn't load model from from: "
					+ commandLineOptions.getModel());
		System.err.println("Loading model...");

		final Parser parser = new ParserAStar(new TaggerEmbeddings(
				commandLineOptions.getModel(),
				commandLineOptions.getMaxLength(),
				commandLineOptions.getSupertaggerbeam(),
				commandLineOptions.getMaxTagsPerWord()),
				commandLineOptions.getMaxLength(),
				commandLineOptions.getNbest(),
				commandLineOptions.getNbestbeam(), input,
				commandLineOptions.getRootCategories(), new File(
						commandLineOptions.getModel(), "unaryRules"), new File(
						commandLineOptions.getModel(), "binaryRules"),
				commandLineOptions.getUnrestrictedRules() ? null : new File(
						commandLineOptions.getModel(), "seenRules"));

		OutputFormat outputFormat = OutputFormat.valueOf(commandLineOptions
				.getOutputFormat().toUpperCase());
		final ParsePrinter printer = outputFormat.printer;

		if ((outputFormat == OutputFormat.PROLOG || outputFormat == OutputFormat.EXTENDED)
				&& input != InputFormat.POSANDNERTAGGED)
			throw new Error("Must use \"-i POSandNERtagged\" for this output");

		final boolean readingFromStdin;
		final Iterator<String> inputLines;
		if (commandLineOptions.getInputFile().getName().isEmpty()) {
			// Read from STDIN
			inputLines = new Scanner(System.in, "UTF-8");
			readingFromStdin = true;
		} else {
			// Read from file
			inputLines = Util.readFile(commandLineOptions.getInputFile())
					.iterator();
			readingFromStdin = false;
		}

		System.err.println("Parsing...");

		Stopwatch timer = Stopwatch.createStarted();
		final SuperTaggingResults supertaggingResults = new SuperTaggingResults();
		final Results dependencyResults = new Results();
		ExecutorService executorService = Executors
				.newFixedThreadPool(commandLineOptions.getThreads());

		final Iterator<String> goldDependencyParses;

		InputReader goldInputReader;

		// Used in Oracle experiments.
		final boolean usingGoldFile;
		if (!commandLineOptions.getGoldDependenciesFile().getPath().isEmpty()) {
			if (!commandLineOptions.getGoldDependenciesFile().exists()) {
				throw new RuntimeException(
						"Can't find gold dependencies file: "
								+ commandLineOptions.getGoldDependenciesFile());
			}

			if (input != InputFormat.GOLD) {
				throw new RuntimeException(
						"If evaluating dependencies, must use \"gold\" input format");
			}

			usingGoldFile = true;
			goldDependencyParses = Util.readFile(
					commandLineOptions.getGoldDependenciesFile()).iterator();
			goldInputReader = InputReader.make(InputFormat.GOLD,
					new SyntaxTreeNodeFactory(
							commandLineOptions.getMaxLength(), 0));
			while (goldDependencyParses.hasNext()) {
				String line = goldDependencyParses.next();
				// Skip header
				if (!line.isEmpty() && !line.startsWith("#"))
					break;
			}
		} else {
			goldDependencyParses = null;
			goldInputReader = null;
			usingGoldFile = false;
		}

		final BufferedWriter sysout = new BufferedWriter(
				new OutputStreamWriter(System.out));

		int id = 0;
		while (inputLines.hasNext()) {
			// Read each sentence, either from STDIN or a parse.
			final String line = inputLines instanceof Scanner ? ((Scanner) inputLines)
					.nextLine().trim() : inputLines.next();
			if (!line.isEmpty() && !line.startsWith("#")) {
				id++;
				final int id2 = id;
				final DependencyParse goldParse;

				if (goldDependencyParses != null
						&& goldDependencyParses.hasNext()) {
					// For Oracle experiments, read in the corresponding gold
					// parse.
					InputToParser goldInput = goldInputReader.readInput(line);
					goldParse = CCGBankDependencies.getDependencyParseCandC(
							goldDependencyParses,
							goldInput.getInputSupertags1best());
				} else {
					goldParse = null;
				}

				// Make a new ExecutorService job for each sentence to parse.
				executorService.execute(new Runnable() {
					public void run() {

						List<SyntaxTreeNode> parses = parser.parse(
								supertaggingResults, line);
						String output;
						if (parses != null && usingGoldFile) {
							if (goldParse != null) {
								output = printer.print(
										Evaluate.getOracle(parses, goldParse),
										id2);
							} else {
								// Just print 1-best when doing Oracle
								// experiments.
								output = printer.print(parses.subList(0, 1),
										id2);
							}

						} else {
							// Not doing Oracle experiments - print all ouput.
							output = printer.print(parses, id2);
						}

						synchronized (printer) {
							try {
								// It's a bit faster to buffer output than use
								// System.out.println() directly.
								sysout.write(output);
								sysout.newLine();

								if (readingFromStdin)
									sysout.flush();
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
					}
				});
			}
		}
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		sysout.close();

		DecimalFormat twoDP = new DecimalFormat("#.##");
		System.err.println("Coverage: "
				+ twoDP.format(100.0
		 * supertaggingResults.parsedSentences.get()
						/ supertaggingResults.totalSentences.get()) + "%");
		if (supertaggingResults.totalCats.get() > 0) {
			System.err.println("Accuracy: "
					+ twoDP.format(100.0 * supertaggingResults.rightCats.get()
							/ supertaggingResults.totalCats.get()) + "%");
		}

		if (!dependencyResults.isEmpty()) {
			System.out.println("F1=" + dependencyResults.getF1());
		}

		System.err.println("Sentences parsed: "
				+ supertaggingResults.parsedSentences);
		System.err.println("Speed: "
				+ twoDP.format(1000.0
		 * supertaggingResults.parsedSentences.get()
						/ timer.elapsed(TimeUnit.MILLISECONDS))
				+ " sentences per second");

		if (commandLineOptions.getTiming()) {
			printDetailedTiming(parser, twoDP);
		}*/
	}

	public static void printDetailedTiming(final Parser parser,
			DecimalFormat format) {
		// Prints some statistic about how long each length sentence took to
		// parse.
		int sentencesCovered = 0;
		Multimap<Integer, Long> sentenceLengthToParseTimeInNanos = parser
				.getSentenceLengthToParseTimeInNanos();
		int binNumber = 0;
		int binSize = 10;
		while (sentencesCovered < sentenceLengthToParseTimeInNanos.size()) {
			double totalTimeForBinInMillis = 0;
			int totalSentencesInBin = 0;
			for (int sentenceLength = binNumber * binSize + 1; sentenceLength < 1
					+ (binNumber + 1) * binSize; sentenceLength = sentenceLength + 1) {
				for (long time : sentenceLengthToParseTimeInNanos
						.get(sentenceLength)) {
					totalTimeForBinInMillis += ((double) time / 1000000);
					totalSentencesInBin++;
				}
			}
			sentencesCovered += totalSentencesInBin;
			double averageTimeInMillis = (double) totalTimeForBinInMillis
					/ (totalSentencesInBin);
			if (totalSentencesInBin > 0) {
				System.err.println("Average time for sentences of length "
						+ (1 + binNumber * binSize) + "-" + (binNumber + 1)
						* binSize + " (" + totalSentencesInBin + "): "
						+ format.format(averageTimeInMillis) + "ms");
			}

			binNumber++;
		}

		int totalSentencesTimes1000 = sentenceLengthToParseTimeInNanos.size() * 1000;
		long totalMillis = parser.getParsingTimeOnlyInMillis()
				+ parser.getTaggingTimeOnlyInMillis();
		System.err.println("Just Parsing Time: "
				+ parser.getParsingTimeOnlyInMillis()
				+ "ms "
				+ (totalSentencesTimes1000 / parser
						.getParsingTimeOnlyInMillis()) + " per second");
		System.err.println("Just Tagging Time: "
				+ parser.getTaggingTimeOnlyInMillis()
				+ "ms "
				+ (totalSentencesTimes1000 / parser
						.getTaggingTimeOnlyInMillis()) + " per second");
		System.err.println("Total Time:        " + totalMillis + "ms "
				+ (totalSentencesTimes1000 / totalMillis) + " per second");
	}
}
