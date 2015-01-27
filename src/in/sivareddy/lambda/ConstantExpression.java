package in.sivareddy.lambda;

import com.google.common.base.Preconditions;

import java.util.Set;

public class ConstantExpression extends AbstractExpression {
  public static final long serialVersionUID = 1L;

  public final String name;

  public ConstantExpression(String name) {
    this.name = Preconditions.checkNotNull(name);
  }

  public String getName() {
    return name;
  }

  @Override
  public void getFreeVariables(Set<Expression> accumulator) {
    accumulator.add(this);
  }

  @Override
  public Expression substitute(Expression constant, Expression replacement) {
    if (this.equals(constant)) {
      return replacement;
    } else {
      return this;
    }
  }

  @Override
  public Expression simplify() {
    return this;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    ConstantExpression other = (ConstantExpression) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }
}
