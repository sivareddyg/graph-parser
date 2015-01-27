package in.sivareddy.graphparser.ccg;

/**
 * candc parser uses weird combinators when standard combinators, binary and
 * unary rules fail. Semantics gets messed up when these combinators are used.
 * I recommend to ignore those parses.
 *
 */
public class FunnyCombinatorException extends Exception {
  private static final long serialVersionUID = 1L;

  public FunnyCombinatorException() {
    super();
  }

  public FunnyCombinatorException(String message) {
    super(message);
  }

  public FunnyCombinatorException(String message, Throwable cause) {
    super(message, cause);
  }

  public FunnyCombinatorException(Throwable cause) {
    super(cause);
  }
}
