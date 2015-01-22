package in.sivareddy.lambda;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class ExpressionParser {

  private static final ConstantExpression OPEN_PAREN = new ConstantExpression(
      "(");
  private static final ConstantExpression CLOSE_PAREN = new ConstantExpression(
      ")");

  public ExpressionParser() {}


  // Changed from private to public
  // Siva Reddy
  public static List<String> tokenize(String expression) {
    String transformedExpression = expression.replaceAll("([()])", " $1 ");
    return Arrays.asList(transformedExpression.trim().split("\\s+"));
  }

  public static Expression parseSingleExpression(String expression) {
    return parseSingleExpression(tokenize(expression));
  }

  public List<Expression> parse(String expressions) {
    return parse(tokenize(expressions));
  }

  public static Expression parseSingleExpression(
      List<String> tokenizedExpressionString) {
    List<Expression> expressions = parse(tokenizedExpressionString);
    Preconditions.checkState(expressions.size() == 1, "Illegal input string: "
        + tokenizedExpressionString);
    return expressions.get(0);
  }

  public static List<Expression> parse(List<String> tokenizedExpressionString) {
    Stack<Expression> stack = new Stack<Expression>();

    for (String token : tokenizedExpressionString) {
      stack.push(new ConstantExpression(token));
      if (stack.peek().equals(CLOSE_PAREN)) {
        stack.push(reduce(stack));
      }
    }

    return stack;
  }

  private static Expression reduce(Stack<Expression> stack) {
    // Pop the closing parenthesis
    Preconditions.checkArgument(stack.peek().equals(CLOSE_PAREN));
    stack.pop();

    // Pop all arguments.
    Stack<Expression> arguments = new Stack<Expression>();
    while (!stack.peek().equals(OPEN_PAREN)) {
      arguments.push(stack.pop());
    }

    // Pop the open parenthesis.
    stack.pop();

    // Add the parsed expression.
    List<Expression> subexpressions = Lists.newArrayList();
    for (Expression argument : Lists.reverse(arguments)) {
      subexpressions.add(argument);
    }

    if (subexpressions.size() > 0
        && subexpressions.get(0) instanceof ConstantExpression) {
      ConstantExpression constant = (ConstantExpression) subexpressions.get(0);
      String constantName = constant.getName();
      if (constantName.equals("lambda")) {
        List<ConstantExpression> variables = Lists.newArrayList();
        for (int i = 1; i < subexpressions.size() - 1; i++) {
          variables.add((ConstantExpression) subexpressions.get(i));
        }
        Expression body = subexpressions.get(subexpressions.size() - 1);
        return new LambdaExpression(variables, body);
      } else if (constantName.equals("and")) {
        return new CommutativeOperator(constant, subexpressions.subList(1,
            subexpressions.size()));
      } else if (constantName.equals("exists")) {
        List<ConstantExpression> variables = Lists.newArrayList();
        for (int i = 1; i < subexpressions.size() - 1; i++) {
          variables.add((ConstantExpression) subexpressions.get(i));
        }
        Expression body = subexpressions.get(subexpressions.size() - 1);
        return new QuantifierExpression(constantName, variables, body);
      }
    }
    return new ApplicationExpression(subexpressions);
  }
}
