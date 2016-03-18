package others;

import java.io.IOException;
import java.util.List;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public interface CcgSyntacticParserCli {
  public List<String> parse(String input) throws IOException,
      ArgumentValidationException, InterruptedException;
}
