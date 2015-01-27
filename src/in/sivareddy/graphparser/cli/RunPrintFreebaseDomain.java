package in.sivareddy.graphparser.cli;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import in.sivareddy.graphparser.util.PrintFreebaseDomain;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RunPrintFreebaseDomain extends AbstractCli {

  // Sparql End point and details
  private OptionSpec<String> endpoint;
  private OptionSpec<String> username;
  private OptionSpec<String> password;
  // Schema File
  private OptionSpec<String> schema;
  // Domain Name
  private OptionSpec<String> domain;

  public static void main(String[] args) {
    new RunPrintFreebaseDomain().run(args);
  }

  @Override
  public void initializeOptions(OptionParser parser) {
    parser.acceptsAll(Arrays.asList("help", "h"), "Print this help message.");

    endpoint =
        parser.accepts("endpoint", "SPARQL endpoint").withRequiredArg()
            .ofType(String.class).required();

    username =
        parser.accepts("username", "username generally dba").withRequiredArg()
            .ofType(String.class).required();

    password =
        parser.accepts("password", "password generally dba").withRequiredArg()
            .ofType(String.class).required();

    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();

    domain =
        parser
            .accepts("domain",
                "uri of the graph e.g. http://film.freebase.com. Specify multiple Uri using ;")
            .withRequiredArg().ofType(String.class).required();

  }

  @Override
  public void run(OptionSet options) {
    try {
      List<String> domains =
          Lists.newArrayList(Splitter.on(";").trimResults().omitEmptyStrings()
              .split(options.valueOf(domain)));
      PrintFreebaseDomain.print(options.valueOf(endpoint),
          options.valueOf(username), options.valueOf(password), domains,
          options.valueOf(schema));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
