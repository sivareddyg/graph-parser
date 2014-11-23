package in.sivareddy.lambda;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CommutativeOperator extends AbstractExpression {
  private static final long serialVersionUID = 1L;

  private final ConstantExpression operatorName;
  private final List<Expression> arguments;

  public CommutativeOperator(ConstantExpression function, List<Expression> arguments) {
    this.operatorName = Preconditions.checkNotNull(function);
    this.arguments = ImmutableList.copyOf(arguments);
  }

  public ConstantExpression getOperatorName() {
    return operatorName;
  }

  public List<Expression> getArguments() {
    return arguments;
  }

  @Override
  public void getFreeVariables(Set<ConstantExpression> accumulator) {
    for (Expression argument : arguments) {
      argument.getFreeVariables(accumulator);
    }
  }

  @Override
  public Expression substitute(ConstantExpression constant, Expression replacement) {
    List<Expression> substituted = Lists.newArrayList();
    for (Expression subexpression : arguments) {
      substituted.add(subexpression.substitute(constant, replacement));
    }
    return new CommutativeOperator(operatorName, substituted);
  }

  @Override
  public Expression simplify() {
    List<Expression> simplified = Lists.newArrayList();
    List<QuantifierExpression> wrappingQuantifiers = Lists.newArrayList();
    for (Expression subexpression : arguments) {
      Expression simplifiedArgument = subexpression.simplify();
      
      // Push quantifiers outside of logical operators.
      if (simplifiedArgument instanceof QuantifierExpression) {
        QuantifierExpression quantifierArgument = ((QuantifierExpression) simplifiedArgument); 
        wrappingQuantifiers.add(quantifierArgument);
        simplified.add(quantifierArgument.getBody());
      } else {
        simplified.add(simplifiedArgument);
      }      
    }

    List<Expression> resultClauses = Lists.newArrayList();
    for (Expression subexpression : simplified) {
      if (subexpression instanceof CommutativeOperator) {
        CommutativeOperator commutative = (CommutativeOperator) subexpression;
        if (commutative.getOperatorName().equals(getOperatorName())) {
          resultClauses.addAll(commutative.getArguments());
        } else {
          resultClauses.add(commutative);
        }
      } else {
        resultClauses.add(subexpression);
      }
    }

    Expression result = new CommutativeOperator(operatorName, resultClauses);
    // Wrap the result with the appropriate quantifiers.
    for (QuantifierExpression quantifier : wrappingQuantifiers) {
      result = new QuantifierExpression(quantifier.getQuantifierName(), quantifier.getBoundVariables(), result);
    }

    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    sb.append(getOperatorName());
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
    result = prime * result + ((arguments == null) ? 0 : arguments.hashCode());
    result = prime * result + ((operatorName == null) ? 0 : operatorName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CommutativeOperator other = (CommutativeOperator) obj;
    if (arguments == null) {
      if (other.arguments != null)
        return false;
    } else if (!arguments.equals(other.arguments))
      return false;
    if (operatorName == null) {
      if (other.operatorName != null)
        return false;
    } else if (!operatorName.equals(other.operatorName))
      return false;
    return true;
  }
}
