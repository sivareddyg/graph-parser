package in.sivareddy.graphparser.cli;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * This Object has functionalities for parsing arguments and also create a log of parameters used
 * which helps in replicability of the experiments.
 *
 *
 * @author Siva Reddy
 *
 *         Parts of this code is taken from Jayant Krishnamurthy
 *
 */
public abstract class AbstractCli {

  /**
   * @param args
   */

  private final Set<CommonOptions> opts;
  private OptionSet parsedOptions;

  // help options
  private OptionSpec<Void> helpOpt;

  public static enum CommonOptions {
    /**
     * Enables options for constructing a uk.ac.ed.sempar.lexicon, e.g., by providing a training
     * sentences, database and type hierarchy.
     */
    LEXICON_GENERATION,
  };

  public AbstractCli(CommonOptions... opts) {
    this.opts = new HashSet<CommonOptions>();
    for (CommonOptions opt : opts)
      this.opts.add(opt);
  }

  private void initializeCommonOptions(OptionParser parser) {
    helpOpt = parser.acceptsAll(Arrays.asList("help", "h"), "Print this help message.");
  }

  /**
   * Adds subclass-specific options to {@code parser}. Subclasses must implement this method in
   * order to accept class-specific options.
   *
   * @param parser option parser to which additional command-line options should be added.
   */
  public abstract void initializeOptions(OptionParser parser);

  /**
   * Runs the program using parsed {@code options}.
   *
   * @param options option values passed to the program
   */
  public abstract void run(OptionSet options);

  /**
   * Runs the program, parsing any options from {@code args}.
   *
   * @param args arguments to the program, in the same format as provided by {@code main}.
   */
  public void run(String[] args) {
    // Add and parse options.
    OptionParser parser = new OptionParser();
    initializeCommonOptions(parser);
    initializeOptions(parser);

    String errorMessage = null;
    try {
      parsedOptions = parser.parse(args);
    } catch (OptionException e) {
      errorMessage = e.getMessage();
    }

    boolean printHelp = false;
    if (errorMessage != null) {
      // If an error occurs, the options don't parse.
      // Therefore, we must manually check if the help option was given.
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("--help")) {
          printHelp = true;
        }
      }
    }

    if (errorMessage != null && !printHelp) {
      System.out.println(errorMessage);
      System.out.println("Try --help for more information about options.");
      System.exit(0);
    }

    if (printHelp || parsedOptions.has(helpOpt)) {
      // If a help option is given, print help then quit.
      try {
        parser.printHelpOn(System.out);
      } catch (IOException ioException) {
        throw new RuntimeException(ioException);
      }
      System.exit(0);
    }

    String workingDir = System.getProperty("user.dir");
    System.out.println("# Working Directory = " + workingDir);


    String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    path = path.replace(workingDir + "/", "");

    String decodedPath = "";
    try {
      decodedPath = URLDecoder.decode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    System.out.println("# Command = java -cp .:" + decodedPath + " " + this.getClass().getName()
        + " " + Joiner.on(" ").join(args));
    /*- System.out.println(" is loaded from "
    		+ getClass().getProtectionDomain().getCodeSource()
    				.getLocation());*/

// Log any passed-in options.
    System.out.println("# Command-line options:");
    for (OptionSpec<?> optionSpec : parsedOptions.specs()) {
      if (parsedOptions.hasArgument(optionSpec)) {
        System.out.println("# --" + Iterables.getFirst(optionSpec.options(), "") + " "
            + parsedOptions.valueOf(optionSpec));
      } else {
        System.out.println("# --" + Iterables.getFirst(optionSpec.options(), ""));
      }
    }
    System.out.println("");

    // Run the program.
    long startTime = System.currentTimeMillis();
    run(parsedOptions);
    long endTime = System.currentTimeMillis();
    double timeElapsed = ((double) (endTime - startTime)) / 1000;
    System.out.println("# Total time elapsed: " + timeElapsed + " seconds");
    System.exit(0);
  }
}
