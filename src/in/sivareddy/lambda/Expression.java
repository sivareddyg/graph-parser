package in.sivareddy.lambda;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public interface Expression extends Serializable, Comparable<Expression> {

  /**
   * Gets the set of unbound variables in this expression.
   *
   * @return
   */
  Set<ConstantExpression> getFreeVariables();

  void getFreeVariables(Set<ConstantExpression> accumulator);

  List<String> getAllVariables();


  /**
   * Replaces the free variable named {@code constant} by {@code replacement}.
   *
   * @param constant
   * @param replacement
   * @return
   */
  Expression substitute(ConstantExpression constant, Expression replacement);

  Expression simplify();

  Expression removeDuplicateVariables();

  Set<GeneralisedRelation> simplifyAndGetRelations(Set<String> entities);

  Set<GeneralisedRelation> getRelationsWithoutSimplification(
      Set<String> entities);

  boolean isContentTypeSemanticCategory();

  Set<GeneralisedRelation> getRelationsWithoutSimplification(
      Set<String> entities, Integer relationArity);

  String removeExtraBrackets();

  String replaceEntitiesWithTypes();

  void setParseScore(Double score);

  void appendLogicalParts(List<Expression> expressions);

  List<Expression> getLogicalParts();

  void appendSpannedWords(List<String> words);

  List<String> getSpannedWords();

  Double getParseScore();

  @Override
  int compareTo(Expression expression);

  @Override
  int hashCode();

  @Override
  boolean equals(Object o);
}
