package in.sivareddy.lambda;

import junit.framework.TestCase;

public class ExpressionParserTest extends TestCase {

  ExpressionParser parser;

  @Override
  public void setUp() {
    parser = new ExpressionParser();
  }

  public void testParseConstant() {
    Expression result = ExpressionParser.parseSingleExpression("x");

    assertTrue(result instanceof ConstantExpression);
    assertEquals("x", ((ConstantExpression) result).getName());
  }

  public void testParse() {
    Expression result =
        ExpressionParser
            .parseSingleExpression("(relation (/m/abc x) (/m/bcd y) /m/cde)");

    assertTrue(result instanceof ApplicationExpression);
    ApplicationExpression application = (ApplicationExpression) result;
    assertEquals("relation",
        ((ConstantExpression) application.getFunction()).getName());
    assertEquals(3, application.getArguments().size());

    System.out.println(application);
  }
}
