package in.sivareddy.lambda;

import in.sivareddy.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import in.sivareddy.graphparser.ccg.SemanticCategory.SemanticCategoryType;

public abstract class AbstractExpression implements Expression {
  private static final long serialVersionUID = 1L;
  private static Integer countAlphaReductions = 0;
  private Double parseScore = null;
  private List<Expression> logicalParts = null;
  private List<String> spannedWords = null;

  @Override
  public void setParseScore(Double score) {
    if (score == null) {
      return;
    }
    parseScore = Double.valueOf(score);
  }

  @Override
  public Double getParseScore() {
    return parseScore;
  }

  @Override
  public Set<ConstantExpression> getFreeVariables() {
    Set<ConstantExpression> variables = Sets.newHashSet();
    getFreeVariables(variables);
    return variables;
  }

  @Override
  public void appendLogicalParts(List<Expression> expressions) {
    if (expressions == null) {
      return;
    }
    if (logicalParts == null) {
      logicalParts = Lists.newArrayList();
    }
    logicalParts.addAll(expressions);
  }

  @Override
  public List<Expression> getLogicalParts() {
    return logicalParts;
  }

  @Override
  public void appendSpannedWords(List<String> words) {
    if (words == null) {
      return;
    }
    if (spannedWords == null) {
      spannedWords = Lists.newArrayList();
    }
    spannedWords.addAll(words);
  }

  @Override
  public List<String> getSpannedWords() {
    return spannedWords;
  }

  public Expression alphaReduction(String expression) {
    countAlphaReductions = (countAlphaReductions + 1) % 1000;
    List<String> tokenizedExpression = ExpressionParser.tokenize(expression);
    Set<String> variables = Sets.newHashSet();

    int i = 0;
    while (i < tokenizedExpression.size()) {
      String token = tokenizedExpression.get(i);
      if (token.equals("lambda") || token.equals("exists")) {
        i++;
        while (i < tokenizedExpression.size()) {
          token = tokenizedExpression.get(i);
          if (token.equals("(")) {
            break;
          }
          variables.add(token);
          i++;
        }
      }
      i++;
    }

    List<String> newTokenizedExpression = Lists.newArrayList();
    for (String token : tokenizedExpression) {
      if (variables.contains(token)) {
        newTokenizedExpression.add(String.format("%s_%s", token,
            Integer.toString(countAlphaReductions)));
      } else {
        newTokenizedExpression.add(token);
      }
    }
    return ExpressionParser.parseSingleExpression(newTokenizedExpression);
  }

  @Override
  public Set<GeneralisedRelation> simplifyAndGetRelations(Set<String> entities) {
    Expression simplifiedExpression =
        this.simplify().removeDuplicateVariables();
    if (simplifiedExpression == null) {
      return null;
    }
    return getRelationsWithoutSimplification(entities);
  }

  @Override
  public Set<GeneralisedRelation> getRelationsWithoutSimplification(
      Set<String> entities, Integer relationArity) {
    Set<GeneralisedRelation> relations =
        this.getRelationsWithoutSimplification(entities);
    if (relations == null) {
      return null;
    }
    Set<GeneralisedRelation> validRelations = Sets.newHashSet();
    for (GeneralisedRelation relation : relations) {
      if (relation.entities.size() == relationArity) {
        validRelations.add(relation);
      }
    }
    return validRelations;
  }

  @Override
  public Set<GeneralisedRelation> getRelationsWithoutSimplification(
      Set<String> entities) {
    String expressionString = this.toString();
    Pattern relationPattern =
        Pattern
            .compile("\\(([^\\(\\)\\s]+) (([^\\(\\)\\s]+ )?)([^\\(\\)\\s]+)\\)");
    Matcher relationMatcher;
    Set<String> entitiesSet;
    if (entities == null) {
      entitiesSet = null;
    } else {
      entitiesSet = entities;
    }

    Set<GeneralisedRelation> relations = Sets.newHashSet();
    relationMatcher = relationPattern.matcher(expressionString);
    while (relationMatcher.find()) {
      String relationName = relationMatcher.group(1);
      if (relationName.startsWith("$")) {
        continue;
      }
      String arg1 = relationMatcher.group(2).trim();
      String arg2 = relationMatcher.group(4).trim();
      if (!arg1.equals("") && entitiesSet != null
          && !entitiesSet.contains(arg1)) {
        continue;
      }
      if (!arg2.equals("") && entitiesSet != null
          && !entitiesSet.contains(arg2)) {
        continue;
      }
      GeneralisedRelation relation =
          arg1.equals("") ? new GeneralisedRelation(relationName, arg2)
              : new GeneralisedRelation(relationName, arg1, arg2);
      relations.add(relation);
    }
    return relations;
  }

  @Override
  public boolean isContentTypeSemanticCategory() {
    Set<GeneralisedRelation> relations =
        this.getRelationsWithoutSimplification(null);

    boolean check = false;
    for (GeneralisedRelation relation : relations) {
      if (!relation.relationName.equals("target:t")) {
        check = true;
        break;
      }
    }
    return check;
  }

  public Expression alphaReduction() {
    String expression = this.toString();
    countAlphaReductions = (countAlphaReductions + 1) % 1000;
    List<String> tokenizedExpression = ExpressionParser.tokenize(expression);
    Set<String> variables = Sets.newHashSet();

    int i = 0;
    while (i < tokenizedExpression.size()) {
      String token = tokenizedExpression.get(i);
      if (token.equals("lambda") || token.equals("exists")) {
        i++;
        while (i < tokenizedExpression.size()) {
          token = tokenizedExpression.get(i);
          if (token.equals("(")) {
            break;
          }
          variables.add(token);
          i++;
        }
      }
      i++;
    }

    List<String> newTokenizedExpression = Lists.newArrayList();
    for (String token : tokenizedExpression) {
      if (variables.contains(token)) {
        newTokenizedExpression.add(String.format("%s_%s", token,
            Integer.toString(countAlphaReductions)));
      } else {
        newTokenizedExpression.add(token);
      }
    }
    return ExpressionParser.parseSingleExpression(newTokenizedExpression);
  }

  @Override
  public String removeExtraBrackets() {
    String expressionString = this.toString();
    return AbstractExpression.removeExtraBrackets(expressionString);
  }

  public static String removeExtraBrackets(String expressionString) {
    // Code to remove extra brackets

    String stringWithoutQuantifiers = new String(expressionString);
    Stack<String> stack = new Stack<>();
    for (int i = 0; i < stringWithoutQuantifiers.length(); i++) {
      String c = stringWithoutQuantifiers.substring(i, i + 1);
      if (c.equals("(")) {
        stack.add("(");
      } else if (c.equals(")")) {
        while (stack.peek().equals(" "))
          stack.pop();

        if (stack.peek().equals("(")) {
          stack.pop();
        } else {
          Stack<String> tempStack = new Stack<>();
          while (!stack.peek().equals("(")) {
            tempStack.add(stack.pop());
          }
          stack.pop();
          if (tempStack.size() > 1 || !tempStack.peek().startsWith("(")) {
            String newElement = "";
            while (tempStack.size() > 0) {
              newElement += tempStack.pop();
            }
            newElement = "(" + newElement + ") ";
            stack.add(newElement);
          } else {
            stack.add(tempStack.pop());
          }
        }
      } else {
        stack.add(c);
      }
    }

    Preconditions
        .checkArgument(stack.size() == 1, "Error in remove Duplicates");
    stringWithoutQuantifiers = stack.pop();
    return stringWithoutQuantifiers;
  }

  private String processEmpty(String expressionString) {
    Pattern emptyPattern = Pattern.compile("\\(EMPTY [^\\(^\\)]+\\)");
    Matcher matcher = emptyPattern.matcher(expressionString);
    String newExpression = matcher.replaceAll("EMPTY");
    while (!newExpression.equals(expressionString)) {
      expressionString = newExpression;
      matcher = emptyPattern.matcher(expressionString);
      newExpression = matcher.replaceAll("EMPTY");
    }
    expressionString = newExpression;
    newExpression = newExpression.replaceAll(" EMPTY ", " ");
    while (!newExpression.equals(expressionString)) {
      expressionString = newExpression;
      newExpression = newExpression.replaceAll(" EMPTY ", " ");
    }
    return newExpression;
  }

  @Override
  public List<String> getAllVariables() {
    String expressionString = this.simplify().toString();
    ArrayList<String> vars = Lists.newArrayList();

    Pattern lambdaPattern = Pattern.compile("lambda ([^\\(]+)");
    Pattern existsPattern = Pattern.compile("exists ([^\\(]+)");

    Matcher lambdaMatcher = lambdaPattern.matcher(expressionString);
    Matcher existsMatcher = existsPattern.matcher(expressionString);
    while (lambdaMatcher.find()) {
      vars.addAll(Lists.newArrayList(Splitter.on(' ').omitEmptyStrings()
          .split(lambdaMatcher.group(1))));
    }

    while (existsMatcher.find()) {
      vars.addAll(Lists.newArrayList(Splitter.on(' ').omitEmptyStrings()
          .split(existsMatcher.group(1))));
    }

    return vars;
  }

  @Override
  public Expression removeDuplicateVariables() {
    String expressionString = this.simplify().toString();
    expressionString = processEmpty(expressionString);
    // expressionString = removeExtraBrackets(expressionString);
    // expressionString =
    // ExpressionParser.parseSingleExpression(expressionString).simplify().toString();
    // System.out.println(expressionString);
    ArrayList<String> lambdas = Lists.newArrayList();
    ArrayList<String> exists = Lists.newArrayList();

    Pattern lambdaPattern = Pattern.compile("lambda ([^\\(]+)");
    Pattern existsPattern = Pattern.compile("exists ([^\\(]+)");

    Matcher lambdaMatcher = lambdaPattern.matcher(expressionString);
    Matcher existsMatcher = existsPattern.matcher(expressionString);
    while (lambdaMatcher.find()) {
      lambdas.addAll(Lists.newArrayList(Splitter.on(' ').omitEmptyStrings()
          .split(lambdaMatcher.group(1))));
    }

    while (existsMatcher.find()) {
      exists.addAll(Lists.newArrayList(Splitter.on(' ').omitEmptyStrings()
          .split(existsMatcher.group(1))));
    }

    String stringWithoutQuantifiers = expressionString;
    lambdaMatcher = lambdaPattern.matcher(stringWithoutQuantifiers);
    stringWithoutQuantifiers = lambdaMatcher.replaceAll("");
    existsMatcher = existsPattern.matcher(stringWithoutQuantifiers);
    stringWithoutQuantifiers = existsMatcher.replaceAll("");

    Pattern duplicatePattern =
        Pattern.compile("\\(([^\\(\\)\\s]+) ([^\\(\\)\\s]+)\\)");
    Matcher duplicateMatches =
        duplicatePattern.matcher(stringWithoutQuantifiers);

    while (duplicateMatches.find()) {
      String var1 = duplicateMatches.group(1);
      String var2 = duplicateMatches.group(2);
      if (exists.contains(var1) && exists.contains(var2)) {
        stringWithoutQuantifiers =
            stringWithoutQuantifiers.replace(
                String.format("(%s %s)", var1, var2), "");
        exists.remove(var2);
        Pattern varPattern =
            Pattern.compile(String.format("([\\s\\(])(%s)([\\s\\)])",
                var2.replace("$", "\\$")));
        Matcher varMatcher = varPattern.matcher(stringWithoutQuantifiers);
        stringWithoutQuantifiers =
            varMatcher.replaceAll(String.format("$1%s$3",
                var1.replace("$", "\\$")));
      } else if (!var1.startsWith("$")
          && !SemanticCategoryType.types.contains(var1)
          && exists.contains(var2)) {
        stringWithoutQuantifiers =
            stringWithoutQuantifiers.replace(
                String.format("(%s %s)", var1, var2),
                String.format("(equal %s %s)", var2, var1));
      } else if (!var2.startsWith("$")
          && !SemanticCategoryType.types.contains(var2)
          && exists.contains(var1)) {
        stringWithoutQuantifiers =
            stringWithoutQuantifiers.replace(
                String.format("(%s %s)", var1, var2),
                String.format("(equal %s %s)", var1, var2));
      }
    }

    // code for removing equalities
    Pattern varInstPattern =
        Pattern.compile("\\(equal ([^\\(\\)\\s]+) ([^\\(\\)\\s]+)\\)");
    Matcher varInstMatcher;

    varInstMatcher = varInstPattern.matcher(stringWithoutQuantifiers);
    HashMap<String, String> varToEntity = Maps.newHashMap();
    boolean validParse = true;
    while (varInstMatcher.find()) {
      String varName = varInstMatcher.group(1);
      String value = varInstMatcher.group(2);
      if (varToEntity.containsKey(varName) || !varName.startsWith("$")) {
        // System.out.println("Invalid logical parse : Same variable assigned to different entities");
        validParse = false;
        break;
      } else {
        varToEntity.put(varName, value);
      }
    }

    if (!validParse) {
      return null;
    }

    ArrayList<String> relationsConcat = Lists.newArrayList();
    Pattern relationPattern =
        Pattern.compile("\\(([^\\(\\)\\s]+)\\s([^\\(\\)\\s]+)\\)");
    Matcher relationMatcher = relationPattern.matcher(stringWithoutQuantifiers);
    while (relationMatcher.find()) {
      String relationName = relationMatcher.group(1);
      String arg1 = relationMatcher.group(2);
      String curRelation = relationMatcher.group();
      if (varToEntity.containsKey(arg1)) {
        String arg1New = varToEntity.get(arg1);
        String newRelation = String.format("(%s %s)", relationName, arg1New);
        stringWithoutQuantifiers =
            stringWithoutQuantifiers.replace(curRelation, newRelation);
        relationsConcat.add(newRelation);
      } else {
        relationsConcat.add(curRelation);
      }
    }

    relationPattern =
        Pattern
            .compile("\\(([^\\(\\)\\s]+)\\s([^\\(\\)\\s]+)\\s([^\\(\\)\\s]+)\\)");
    relationMatcher = relationPattern.matcher(stringWithoutQuantifiers);
    while (relationMatcher.find()) {
      String relationName = relationMatcher.group(1);
      String arg1 = relationMatcher.group(2);
      String arg2 = relationMatcher.group(3);
      String curRelation = relationMatcher.group();
      if (relationName.equals("equal")) {
        if (exists.contains(arg1)) {
          stringWithoutQuantifiers =
              stringWithoutQuantifiers.replace(curRelation, "");
          exists.remove(arg1);
        } else {
          relationsConcat.add(curRelation);
        }

      } else {
        if (varToEntity.containsKey(arg1) || varToEntity.containsKey(arg2)) {
          String arg1New =
              varToEntity.containsKey(arg1) ? varToEntity.get(arg1) : arg1;
          String arg2New =
              varToEntity.containsKey(arg2) ? varToEntity.get(arg2) : arg2;
          String newRelation =
              String.format("(%s %s %s)", relationName, arg1New, arg2New);
          stringWithoutQuantifiers =
              stringWithoutQuantifiers.replace(curRelation, newRelation);
          relationsConcat.add(newRelation);
        } else {
          relationsConcat.add(curRelation);
        }
      }
    }

    // System.out.println(stringWithoutQuantifiers);
    while (true) {
      String neWstringWithoutQuantifiers =
          stringWithoutQuantifiers.replaceAll("\\(\\s*\\)", "");
      if (neWstringWithoutQuantifiers.equals(stringWithoutQuantifiers)) {
        break;
      } else {
        stringWithoutQuantifiers = neWstringWithoutQuantifiers;
      }
    }

    // Remove Extra Brackets
    // System.err.println(stringWithoutQuantifiers);
    stringWithoutQuantifiers =
        StringUtils.removeExtraBrackets(stringWithoutQuantifiers);
    // removeExtraBrackets(stringWithoutQuantifiers);

    // Below code is to remove extra brackets but only works if all the
    // logical expressions contain binary functions
    // Do not use this.
    /*
     * if (relationsConcat.size() > 1) { stringWithoutQuantifiers = "(and " +
     * Joiner.on(" ").join(relationsConcat) + ")"; } else if
     * (relationsConcat.size() == 1){ stringWithoutQuantifiers =
     * relationsConcat.get(0); } else { return null; }
     */

    String lambdaExpression = Joiner.on(" ").join(lambdas);
    lambdaExpression =
        lambdaExpression.equals("") ? lambdaExpression : "lambda "
            + lambdaExpression;
    String existsExpression = Joiner.on(" ").join(exists);
    existsExpression =
        existsExpression.equals("") ? existsExpression : "exists "
            + existsExpression;

    String finalReducedExpression;
    if (lambdaExpression.equals("") && existsExpression.equals("")) {
      finalReducedExpression = stringWithoutQuantifiers;
    } else if (lambdaExpression.equals("")) {
      finalReducedExpression =
          String.format("(%s %s)", existsExpression, stringWithoutQuantifiers);
    } else if (existsExpression.equals("")) {
      finalReducedExpression =
          String.format("(%s %s)", lambdaExpression, stringWithoutQuantifiers);
    } else {
      finalReducedExpression =
          String.format("(%s (%s %s))", lambdaExpression, existsExpression,
              stringWithoutQuantifiers);
    }

    // System.out.println(finalReducedExpression);

    Expression finalSimplified =
        ExpressionParser.parseSingleExpression(finalReducedExpression);
    finalSimplified.appendLogicalParts(this.getLogicalParts());
    finalSimplified.appendSpannedWords(this.getSpannedWords());
    finalSimplified.setParseScore(this.getParseScore());
    return finalSimplified;
  }

  @Override
  public String replaceEntitiesWithTypes() {
    String expressionString = this.toString();
    Pattern relationPattern =
        Pattern.compile("[\\(\\s]([^\\(\\s]+):([^\\)\\s]+)");
    Matcher Matcher = relationPattern.matcher(expressionString);
    while (Matcher.find()) {
      String entityName = Matcher.group(1);
      String entityType = Matcher.group(2);
      if (!entityType.equals("t")) {
        expressionString =
            expressionString.replace(entityName + ":" + entityType, "TYPE:"
                + entityType);
      }
    }
    return expressionString;
  }

  public static List<ApplicationExpression> getApplicationExpressions(
      Expression expression) {
    List<ApplicationExpression> result = Lists.newArrayList();

    if (expression.getClass().equals(CommutativeOperator.class)) {
      CommutativeOperator exp = (CommutativeOperator) expression;
      List<Expression> args = exp.getArguments();
      for (Expression arg : args) {
        List<ApplicationExpression> out = getApplicationExpressions(arg);
        result.addAll(out);
      }
    } else if (expression.getClass().equals(LambdaExpression.class)) {
      LambdaExpression exp = (LambdaExpression) expression;
      Expression body = exp.getBody();
      List<ApplicationExpression> out = getApplicationExpressions(body);
      result.addAll(out);
    } else if (expression.getClass().equals(QuantifierExpression.class)) {
      QuantifierExpression exp = (QuantifierExpression) expression;
      Expression body = exp.getBody();
      List<ApplicationExpression> out = getApplicationExpressions(body);
      result.addAll(out);
    } else if (expression.getClass().equals(ApplicationExpression.class)) {
      ApplicationExpression exp = (ApplicationExpression) expression;
      List<Expression> subexps = exp.subexpressions;
      if (subexps.size() > 0
          && subexps.get(0).getClass().equals(ConstantExpression.class)) {
        result.add(exp);
      } else {
        for (Expression subexp : subexps) {
          List<ApplicationExpression> out = getApplicationExpressions(subexp);
          result.addAll(out);
        }
      }
    }
    return result;
  }

  @Override
  public int compareTo(Expression expression) {
    Preconditions.checkNotNull(expression.getParseScore(),
        "Cannot compare expressions - parse score is null");
    Preconditions.checkNotNull(this.getParseScore(),
        "Cannot compare expressions - parse score is null");
    Double thisParseScore = this.getParseScore();
    Double curParseScore = expression.getParseScore();
    int x = thisParseScore.compareTo(curParseScore);
    if (x == 0) {
      // Expression having fewer relations are preferred
      x =
          ((Integer) this.getRelationsWithoutSimplification(null).size())
              .compareTo(expression.getRelationsWithoutSimplification(null)
                  .size());
      x = x * -1;
    }
    if (x == 0) {
      x = this.toString().compareTo(expression.toString());
    }
    return x;
  }

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object other);
}
