package in.sivareddy.lambda;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class LambdaExpression extends AbstractExpression {
  private static final long serialVersionUID = 1L;
  // private static Integer countAlphaReductions = 0;

  private final List<ConstantExpression> argumentVariables;
  private final Expression body;

  public LambdaExpression(List<ConstantExpression> argumentVariables,
      Expression body) {
    // this.argumentVariables = ImmutableList.copyOf(argumentVariables);
    this.argumentVariables = Lists.newArrayList(argumentVariables);
    this.body = Preconditions.checkNotNull(body);
  }

  /**
   * Returns the body of the function, i.e., the code that gets executed when
   * the function is invoked.
   *
   * @return
   */
  public Expression getBody() {
    return body;
  }

  /**
   * Gets the variables which are arguments to the function.
   *
   * @return
   */
  public List<ConstantExpression> getArguments() {
    return argumentVariables;
  }

  public Expression reduce(List<Expression> argumentValues) {
    Preconditions.checkArgument(argumentValues.size() <= argumentVariables
        .size());

    Expression substitutedBody = body;
    for (int i = 0; i < argumentValues.size(); i++) {
      substitutedBody =
          substitutedBody.substitute(argumentVariables.get(i),
              argumentValues.get(i));
    }

    if (argumentValues.size() == argumentVariables.size()) {
      return substitutedBody;
    } else {
      return new LambdaExpression(argumentVariables.subList(
          argumentValues.size(), argumentVariables.size()), substitutedBody);
    }
  }

  /**
   * @author siva reddy
   * @param expression
   * @return
   */
  /*
   * public Expression alphaReduction(String expression) { countAlphaReductions
   * = (countAlphaReductions + 1) % 1000; ExpressionParser expressionParser =
   * new ExpressionParser(); List<String> tokenizedExpression = expressionParser
   * .tokenize(expression); Set<String> variables = Sets.newHashSet();
   * 
   * int i = 0; while (i < tokenizedExpression.size()) { String token =
   * tokenizedExpression.get(i); if (token.equals("lambda") ||
   * token.equals("exists")) { i++; while (i < tokenizedExpression.size()) {
   * token = tokenizedExpression.get(i); if (token.equals("(")) break;
   * variables.add(token); i++; } } i++; }
   * 
   * List<String> newTokenizedExpression = Lists.newArrayList(); for (String
   * token : tokenizedExpression) { if (variables.contains(token))
   * newTokenizedExpression.add(String.format("%s_%s", token,
   * Integer.toString(countAlphaReductions))); else
   * newTokenizedExpression.add(token); } return
   * expressionParser.parseSingleExpression(newTokenizedExpression); }
   * 
   * public Expression alphaReduction() { String expression = this.toString();
   * countAlphaReductions = (countAlphaReductions + 1) % 1000; ExpressionParser
   * expressionParser = new ExpressionParser(); List<String> tokenizedExpression
   * = expressionParser .tokenize(expression); Set<String> variables =
   * Sets.newHashSet();
   * 
   * int i = 0; while (i < tokenizedExpression.size()) { String token =
   * tokenizedExpression.get(i); if (token.equals("lambda") ||
   * token.equals("exists")) { i++; while (i < tokenizedExpression.size()) {
   * token = tokenizedExpression.get(i); if (token.equals("(")) break;
   * variables.add(token); i++; } } i++; }
   * 
   * List<String> newTokenizedExpression = Lists.newArrayList(); for (String
   * token : tokenizedExpression) { if (variables.contains(token))
   * newTokenizedExpression.add(String.format("%s_%s", token,
   * Integer.toString(countAlphaReductions))); else
   * newTokenizedExpression.add(token); } return
   * expressionParser.parseSingleExpression(newTokenizedExpression); }
   */

  /**
   * @param argumentVariable
   * @param value
   * @return Make sure that the value is alpha reduced before passing it as
   *         argument. This method do not perform alpha reduction.
   */
  public Expression reduceArgument(ConstantExpression argumentVariable,
      Expression value) {
    Preconditions.checkArgument(argumentVariables.contains(argumentVariable));

    // Added by Siva Reddy
    // value = alphaReduction(value.toString());

    List<ConstantExpression> remainingArguments = Lists.newArrayList();
    Expression substitutedBody = body;
    for (int i = 0; i < argumentVariables.size(); i++) {
      if (argumentVariables.get(i).equals(argumentVariable)) {
        substitutedBody =
            substitutedBody.substitute(argumentVariables.get(i), value);
      } else {
        remainingArguments.add(argumentVariables.get(i));
      }
    }

    if (remainingArguments.size() > 0) {
      return new LambdaExpression(remainingArguments, substitutedBody);
    } else {
      return substitutedBody;
    }
  }

  @Override
  public void getFreeVariables(Set<Expression> accumulator) {
    body.getFreeVariables(accumulator);
    accumulator.removeAll(argumentVariables);
  }

  @Override
  public Expression substitute(Expression constant, Expression replacement) {
    if (!argumentVariables.contains(constant)) {
      Expression substitution = body.substitute(constant, replacement);
      return new LambdaExpression(argumentVariables, substitution);
    } else {
      return this;
    }
  }

  @Override
  public Expression simplify() {
    Expression simplifiedBody = body.simplify();
    return new LambdaExpression(argumentVariables, simplifiedBody);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(lambda");
    for (Expression argument : argumentVariables) {
      sb.append(" ");
      sb.append(argument.toString());
    }

    sb.append(" ");
    sb.append(body.toString());
    sb.append(")");
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + ((argumentVariables == null) ? 0 : argumentVariables.hashCode());
    result = prime * result + ((body == null) ? 0 : body.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LambdaExpression other = (LambdaExpression) obj;
    if (argumentVariables == null) {
      if (other.argumentVariables != null) {
        return false;
      }
    } else if (!argumentVariables.equals(other.argumentVariables)) {
      return false;
    }
    if (body == null) {
      if (other.body != null) {
        return false;
      }
    } else if (!body.equals(other.body)) {
      return false;
    }
    return true;
  }
}
