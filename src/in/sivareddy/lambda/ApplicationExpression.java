package in.sivareddy.lambda;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ApplicationExpression extends AbstractExpression {
  public static final long serialVersionUID = 1L;

  public final List<Expression> subexpressions;

  public ApplicationExpression(List<? extends Expression> subexpressions) {
    Preconditions.checkArgument(subexpressions.size() >= 1);
    this.subexpressions = ImmutableList.copyOf(subexpressions);
  }

  public ApplicationExpression(Expression function, List<? extends Expression> arguments) {
    this.subexpressions = Lists.newArrayList(function);
    this.subexpressions.addAll(arguments);
  }

  public Expression getFunction() {
    return subexpressions.get(0);
  }

  public List<Expression> getArguments() {
    return subexpressions.subList(1, subexpressions.size());
  }

  @Override
  public void getFreeVariables(Set<ConstantExpression> accumulator) {
    for (Expression subexpression : subexpressions) {
      subexpression.getFreeVariables(accumulator);
    }
  }

  @Override
  public Expression substitute(ConstantExpression constant, Expression replacement) {
    List<Expression> substituted = Lists.newArrayList();
    for (Expression subexpression : subexpressions) {
      substituted.add(subexpression.substitute(constant, replacement));
    }

    return new ApplicationExpression(substituted);
  }

  @Override
  public Expression simplify() {
    // First simplify all arguments
    List<Expression> simplifiedArguments = Lists.newArrayList();
    List<Expression> arguments = getArguments();

    for (Expression argument : arguments) {
      simplifiedArguments.add(argument.simplify());
    }

    Expression function = getFunction().simplify();
    if (function instanceof LambdaExpression) {
      LambdaExpression lambdaFunction = (LambdaExpression) function;
      return lambdaFunction.reduce(simplifiedArguments).simplify();
    } else {
      List<Expression> subexpressions = Lists.newArrayList(function);
      subexpressions.addAll(simplifiedArguments);
      return new ApplicationExpression(subexpressions);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append(getFunction());
    for (Expression argument : getArguments()) {
      sb.append(" ");
      sb.append(argument.toString());
    }
    sb.append(")");
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((subexpressions == null) ? 0 : subexpressions.hashCode());
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
    ApplicationExpression other = (ApplicationExpression) obj;
    if (subexpressions == null) {
      if (other.subexpressions != null) {
        return false;
      }
    } else if (!subexpressions.equals(other.subexpressions)) {
      return false;
    }
    return true;
  }
}
