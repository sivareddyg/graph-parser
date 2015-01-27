package in.sivareddy.lambda;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;

public class QuantifierExpression extends AbstractExpression {
  private static final long serialVersionUID = 1L;

  private final String quantifierName;
  private final List<ConstantExpression> boundVariables;
  private final Expression body;

  public QuantifierExpression(String quantifierName,
      List<ConstantExpression> boundVariables, Expression body) {
    this.quantifierName = quantifierName;
    this.boundVariables = ImmutableList.copyOf(boundVariables);
    this.body = Preconditions.checkNotNull(body);
  }

  public String getQuantifierName() {
    return quantifierName;
  }

  /**
   * Returns the body of the statement, i.e., the portion of the statement over
   * which the quantifier the quantifier has scope.
   *
   * @return
   */
  public Expression getBody() {
    return body;
  }

  /**
   * Gets the variables which are bound by this quantifier.
   *
   * @return
   */
  public List<ConstantExpression> getBoundVariables() {
    return boundVariables;
  }

  @Override
  public void getFreeVariables(Set<Expression> accumulator) {
    body.getFreeVariables(accumulator);
    accumulator.removeAll(boundVariables);
  }

  @Override
  public Expression substitute(Expression constant,
      Expression replacement) {
    if (!boundVariables.contains(constant)) {
      Expression substitution = body.substitute(constant, replacement);
      return new QuantifierExpression(quantifierName, boundVariables,
          substitution);
    } else {
      return this;
    }
  }

  @Override
  public Expression simplify() {
    Expression simplifiedBody = body.simplify();
    return new QuantifierExpression(quantifierName, boundVariables,
        simplifiedBody);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append(quantifierName);
    for (Expression argument : boundVariables) {
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
    result = prime * result + ((body == null) ? 0 : body.hashCode());
    result =
        prime * result
            + ((boundVariables == null) ? 0 : boundVariables.hashCode());
    result =
        prime * result
            + ((quantifierName == null) ? 0 : quantifierName.hashCode());
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
    QuantifierExpression other = (QuantifierExpression) obj;
    if (body == null) {
      if (other.body != null) {
        return false;
      }
    } else if (!body.equals(other.body)) {
      return false;
    }
    if (boundVariables == null) {
      if (other.boundVariables != null) {
        return false;
      }
    } else if (!boundVariables.equals(other.boundVariables)) {
      return false;
    }
    if (quantifierName == null) {
      if (other.quantifierName != null) {
        return false;
      }
    } else if (!quantifierName.equals(other.quantifierName)) {
      return false;
    }
    return true;
  }
}
