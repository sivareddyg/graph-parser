package in.sivareddy.graphparser.ccg;

import in.sivareddy.util.StringObject;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import in.sivareddy.lambda.ApplicationExpression;
import in.sivareddy.lambda.ConstantExpression;
import in.sivareddy.lambda.Expression;
import in.sivareddy.lambda.ExpressionParser;
import in.sivareddy.lambda.LambdaExpression;

import in.sivareddy.graphparser.ccg.SyntacticCategory.IndexedDependency;

/*-
 * 
 * Semantic category is a lambda expression of a given syntactic category
 * 
 * We do not lexicalise the relations in the lambda expression. Instead we use index names of the lexical items. 
 * 
 * In general the template of a relation is as follows. The first argument in the tuple represents the semantic type of the relation. 
 * We implemented around 6-8 semantic types, but you can implement additional cases (e.g. addition)
 * 
 * (RELATIONTYPE INDEX1 INDEX2 INDEX3 somestring somestring INDEX4 .... )
 * 
 * 
 * 
 * (type head argument) = head(argument_>value)
 * (type president Obama) = president(Obama->value)
 * 
 * (typemod head current relationName argument) = head.current.relationName(argument)
 * 
 * public library
 * public (N{X}/N{X}){_}
 * (typemod $X $_ 1 $X) -> library.public.1($X)
 * 
 * (eventmod head current relationName) = head.current.relationName(head->event)
 * (eventmod visit annually 1 visit) -> visit.annually.1(visit->event)
 * annually ((S[X]{Y}\NP{Z}){Y}\(S[X]{Y}\NP{Z}){Y}){_};_ 1 Y
 * (lambda $f1 $f2 $Y (exists $Z (and (($f1 $Z) $Y) ($f2 $Z) (event $Y $_ 1 $Y))))

 * (event head current relationName argument) = head.current.relationName(head->event, argument)

 * (event president of 2 US) -> president.of.2(e, US) 
 * e corresponds to the event of the head's variable, here president

 * told (((S[dcl]{_}\NP{Y}){_}/(S[to]{Z}\NP{W}){Z}){_}/NP{W}){_};_ 3 W,_ 2 Z,_ 1 Y 
 * (lambda $f1 $f2 $f3 $_ (exists $W $Z $Y (and ($f1 $W) (($f2 $W) $Z) ($f3 $Y) (event $_ $_ 1 $Y) (event $_ $_ 2 $Z) (event $_ $_ 3 $W))))

 * Bush was the president of US in 2001
 * in (((S[X]{Y}\NP{Z}){Y}\(S[X]{Y}<1>\NP{Z}){Y}){_}/NP{W}<2>){_}
 * in : (lambda $f1 $f2 $f3 $Y (exists $W $Z (and ($f1 $W) (($f2 $Z) $Y) ($f3 $Z) $Y->word.$_->word.1($Y->event, $Y->value) $Y->word.$_->word.2($Y->event, $W->value))))
 * was : ((S{Y}\N{X}<1>){Y}/N{Y}<2>){_} 
 * was : (lambda $f1 $f2 $Y (exists $X (and ($f1 $Y) ($f2 $X) $Y->word.$_->word.2($Y.event, $Y) $Y->word.$_->word_1($Y->event, $X))))
 * of : ((N{Y}\N{Y}<1>){_}/N{Z}<2>){_}
 * of : (lambda $f1 $f2 $Y (exists $Z $Y ($f1 $Z) ($f2 $Y) $Y->word.$_->word.1($Y.event, $Y.value) $Y->word.$_->word.2($Y->event, $Z->value)))
 * president.was.1(e, Bush) ^ president.was.2(e) ^ president.of.1(e) ^ president.of.2(e, US) ^ president.in.1(e) ^ president.in.2(e, 2001)
 * final semantics: president.was.1(e, Bush) ^ president.of.2(e, US) ^ president.in.2(e, 2001)

 * is.1(e, Bush) ^ is.2(e, x) ^ president(x) ^ of.1(e2, x) ^ of.2(e2, US) ^ married.in.1(e, 2001)
 * president(Bush) ^ of(e2, Bush) ^ of(e2, US) ^ in(e2, 2001)

 * Mitchell married the president of US in 2001
 * married.1(e, Mitchell) ^ married.2(e, X) ^ president(X) ^ president.of.1(e2, X) ^ president.of.2(e2, US) ^ married.in(e, 2001)

 * I live in Edinburgh of Scotland
 * of : ((N{Y}\N{Y}<1>){_}/N{Z}<2>){_}
 * of : (lambda $f1 $f2 $Y (exists $Z $Y ($f1 $Z) ($f2 $Y) $Y->word.$_->word.1($Y) $Y->word.$_->word.2($Y, $Z)))
 * of : (lambda $f1 $f2 $Y (exists $Z $Y e ($f1 $Z) ($f2 $Y) $Y->word.$_->word.1(e, $Y) $Y->word.$_->word.2(e, $Z)))
 * 
 */
public class SemanticCategory extends LambdaExpression {

  private SemanticCategory(List<ConstantExpression> argumentVariables,
      Expression body) {
    super(argumentVariables, body);
  }

  private SemanticCategory(LambdaExpression exp) {
    super(exp.getArguments(), exp.getBody());
  }

  private SemanticCategory(Expression exp) {
    // LambdaExpression lexp = ((LambdaExpression) exp);
    super(((LambdaExpression) exp).getArguments(), ((LambdaExpression) exp)
        .getBody());
  }

  private static final long serialVersionUID = 1L;

  public enum SemanticCategoryType {
    TYPE, TYPEMOD, COMPLEMENT, EVENT, EVENTMOD, NEGATION, CLOSED, EMPTY, IDENTITY, COPULA, UNIQUE, COUNT, QUESTION;

    public final static ImmutableSet<String> types = ImmutableSet.of("TYPE",
        "TYPEMOD", "COMPLEMENT", "EVENT", "EVENTMOD", "NEGATION", "CLOSED",
        "EMPTY", "IDENTITY", "COPULA", "UNIQUE", "COUNT", "QUESTION");
  }

  private static Integer functionCount = 0;
  private static Integer maxFunctionCount = 10000;

  private static final String FUNCPREFIX = "f";

  public static synchronized String createFunction() {
    String func = FUNCPREFIX + functionCount;
    functionCount++;
    if (functionCount % maxFunctionCount == 0) {
      functionCount = 0;
    }
    return "$" + func;
  }

  /**
   * reset function counter
   */
  public static synchronized void resetCounter() {
    functionCount = 0;
  }

  /**
   * Given a syntactic category, this generates a lambda expression without any
   * semantic types.
   *
   * @param synCat
   * @param lambdas
   * @param exists
   * @param bodyExpression
   */
  public static void getDeepExpression(SyntacticCategory synCat,
      List<String> lambdas, Set<String> exists, StringObject bodyExpression) {

    // converting ((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}
    // to lambda $f0 ((($f0 EMPTY) $Z:3) $Y:2)

    Preconditions.checkNotNull(bodyExpression, "bodyExpression cannot be null");
    if (bodyExpression.getString().equals("")) {
      String func = createFunction();
      bodyExpression.setString(func);
      lambdas.add(func);
    }

    if (synCat.isBasic() || synCat.isVar()) {
      Set<CategoryIndex> variables = synCat.getAllVariables();
      CategoryIndex var = variables.iterator().next();
      String varName = var.getVarNameAndKey();
      exists.add(varName);
      bodyExpression.setString("(" + bodyExpression.getString() + ' ' + varName
          + ')');
    } else {
      SyntacticCategory parent = synCat.getParent();
      SyntacticCategory arg = synCat.getArgument();

      if (arg.isBasic() || arg.isVar()) {
        getDeepExpression(arg, lambdas, exists, bodyExpression);
      } else {
        bodyExpression.setString("(" + bodyExpression.getString() + ' '
            + SemanticCategoryType.EMPTY + ')');
      }
      getDeepExpression(parent, lambdas, exists, bodyExpression);
    }
  }

  /**
   * Generates a semantic category based on the syntactic category and the
   * semantic type. Probably the most useful function in this Object.
   *
   * @param synCat
   * @param categoryType
   * @return
   */
  public static SemanticCategory generateSemanticCategory(
      SyntacticCategory synCat, SemanticCategoryType categoryType) {
    List<String> lambdas = Lists.newArrayList();
    Set<String> exists = Sets.newHashSet();
    List<String> bodyExpressions = Lists.newArrayList();
    generateSemanticCategory(synCat, categoryType, lambdas, exists,
        bodyExpressions);
    return generateSemanticCategory(lambdas, exists, bodyExpressions);
  }

  private static void generateSemanticCategory(SyntacticCategory synCat,
      SemanticCategoryType categoryType, List<String> lambdas,
      Set<String> exists, List<String> bodyExpressions) {

    if (categoryType == SemanticCategoryType.IDENTITY) {
      String func = createFunction();
      lambdas.add(func);
      bodyExpressions.add(String.format("(%s)", func));
    } else if (synCat.isBasic() || categoryType == SemanticCategoryType.TYPE) {

      StringObject bodyExpression = new StringObject();
      generateTypeOrEmpty(synCat, lambdas, exists, bodyExpression, categoryType);
      bodyExpressions.add(bodyExpression.toString());
    } else {
      SyntacticCategory parent = synCat.getParent();
      SyntacticCategory arg = synCat.getArgument();

      while (true) {
        StringObject bodyExpression = new StringObject();
        getDeepExpression(arg, lambdas, exists, bodyExpression);
        bodyExpressions.add(bodyExpression.toString());

        if (parent.isBasic()) {
          break;
        } else {
          arg = parent.getArgument();
          parent = parent.getParent();
        }
      }

      CategoryIndex head = parent.getIndex();
      CategoryIndex cur = synCat.getIndex();
      String headName = head.getVarNameAndKey();
      String curName = cur.getVarNameAndKey();
      List<IndexedDependency> deps =
          Lists.newArrayList(synCat.getDependencies(cur));
      lambdas.add(headName);
      Collections.sort(deps);

      StringBuilder sb;
      String bodyExpression;
      switch (categoryType) {
        case CLOSED:
          // nothing to do
          break;
        case TYPEMOD:
          /*- 
           * (typemod head current relationName argument) = head.current.relationName(argument)
           * public library
           * public (N{X}/N{X}){_}
           * (typemod $X $_ 1 $X) -> library.public.1($X) 
           */
          // Preconditions.checkArgument(deps.size() == 1,
          // "type modifier should have (only) one dependency");
          for (IndexedDependency dep : deps) {
            String childName = dep.getChild().getVarNameAndKey();
            sb = new StringBuilder();
            sb.append(categoryType);
            sb.append(" ");
            sb.append(headName);
            sb.append(" ");
            sb.append(curName);
            sb.append(" ");
            sb.append(dep.getRelation());
            sb.append(" ");
            sb.append(childName);
            bodyExpression = "(" + sb.toString() + ")";
            bodyExpressions.add(bodyExpression);
            exists.add(headName);
            exists.add(curName);
            exists.add(childName);
          }
          break;

        case COMPLEMENT:
          /*- 
           * (COMPLEMENT current head) = (COMPLEMENT current.var head.var) 
           */
          // Preconditions.checkArgument(deps.size() == 1,
          // "type modifier should have (only) one dependency");
          for (IndexedDependency dep : deps) {
            String childName = dep.getChild().getVarNameAndKey();
            sb = new StringBuilder();
            sb.append(categoryType);
            sb.append(" ");
            sb.append(childName);
            sb.append(" ");
            sb.append(curName);
            bodyExpression = "(" + sb.toString() + ")";
            bodyExpressions.add(bodyExpression);
            exists.add(childName);
            exists.add(curName);
          }
          break;
        case UNIQUE:
          /*-
           * (unique head) = unique(head->var) 
           */
          // Preconditions.checkArgument(deps.size() == 1,
          // "quantifier should have (only) one dependency");
          sb = new StringBuilder();
          sb.append(categoryType);
          sb.append(" ");
          sb.append(headName);
          bodyExpression = "(" + sb.toString() + ")";
          bodyExpressions.add(bodyExpression);
          exists.add(headName);
          break;
        case QUESTION:
          // (TARGET VAR)
          // right most child is the one that is required to answer the
          // question
          // may have to change this based on category
          SyntacticCategory targetCat = synCat.getDeepChildCategory();
          CategoryIndex targetIndex = targetCat.getIndex();
          String targetVarName = targetIndex.getVarNameAndKey();
          sb = new StringBuilder();
          sb.append(categoryType);
          sb.append(" ");
          sb.append(targetVarName);
          bodyExpression = "(" + sb.toString() + ")";
          bodyExpressions.add(bodyExpression);
          exists.add(targetVarName);
          break;
        case EVENT:
          /*-
           * (event head current relationName argument) = head.current.relationName(head->event, argument)
           * (event president of 2 US) -> president.of.2(e, US) 
           * e corresponds to the event of the head's variable, here president
           */

          for (IndexedDependency dep : deps) {
            sb = new StringBuilder();
            String childName = dep.getChild().getVarNameAndKey();
            sb.append(categoryType);
            sb.append(" ");
            sb.append(headName);
            sb.append(" ");
            sb.append(curName);
            sb.append(" ");
            sb.append(dep.getRelation());
            sb.append(" ");
            sb.append(childName);
            bodyExpression = "(" + sb.toString() + ")";
            bodyExpressions.add(bodyExpression);
            exists.add(headName);
            exists.add(curName);
            exists.add(childName);
          }
          break;

        case EVENTMOD:
          /*-
           * (eventmod head current relationName) = head.current.relationName(head->event)
           * (eventmod visit annually 1 visit) -> visit.annually.1(visit->event) 
           */
          // Preconditions.checkArgument(deps.size() == 1,
          // "event modifier should have (only) one dependency");
          for (IndexedDependency dep : deps) {
            sb = new StringBuilder();
            sb.append(categoryType);
            sb.append(" ");
            sb.append(headName);
            sb.append(" ");
            sb.append(curName);
            sb.append(" ");
            sb.append(dep.getRelation());
            bodyExpression = "(" + sb.toString() + ")";
            bodyExpressions.add(bodyExpression);
            exists.add(headName);
            exists.add(curName);
          }
          break;

        case NEGATION:
          /*-
           * (negation head) = NEGATION(head->event)
           * (eventmodneg head) -> NEGATION(visit->event) 
           */
          // Preconditions.checkArgument(deps.size() == 1,
          // "negation should have (only) one dependency");
          sb = new StringBuilder();
          sb.append(categoryType);
          sb.append(" ");
          sb.append(headName);
          bodyExpression = "(" + sb.toString() + ")";
          bodyExpressions.add(bodyExpression);
          exists.add(headName);
          break;

        default:
          return;
      }
    }
    exists.removeAll(lambdas);
    return;
  }

  /**
   * Builds a readable semantic category
   *
   * @param lambdas
   * @param exists
   * @param bodyExpressions
   * @return
   */
  public static SemanticCategory generateSemanticCategory(List<String> lambdas,
      Set<String> exists, List<String> bodyExpressions) {

    Preconditions.checkArgument(bodyExpressions.size() > 0,
        "Cannot generate expression: No body expressions");
    Preconditions.checkArgument(lambdas.size() > 0, "No lambdas");

    String lambdaString = Joiner.on(" ").skipNulls().join(lambdas);
    String existsString = Joiner.on(" ").skipNulls().join(exists);
    String bodyExpression = Joiner.on(" ").skipNulls().join(bodyExpressions);

    Expression exp;
    if (bodyExpressions.size() == 1) {
      if (existsString.equals("")) {
        exp =
            ExpressionParser.parseSingleExpression(String.format(
                "(lambda %s %s)", lambdaString, bodyExpression));
      } else {
        exp =
            ExpressionParser.parseSingleExpression(String.format(
                "(lambda %s (exists %s %s))", lambdaString, existsString,
                bodyExpression));
      }

    } else {
      if (existsString.equals("")) {
        exp =
            ExpressionParser.parseSingleExpression(String.format(
                "(lambda %s (and %s))", lambdaString, bodyExpression));
      } else {
        exp =
            ExpressionParser.parseSingleExpression(String.format(
                "(lambda %s (exists %s (and %s)))", lambdaString, existsString,
                bodyExpression));
      }
    }

    LambdaExpression lambdaExp = (LambdaExpression) exp;
    List<ConstantExpression> expArgs = lambdaExp.getArguments();
    Expression expBody = lambdaExp.getBody();
    SemanticCategory semCat = new SemanticCategory(expArgs, expBody);
    return semCat;
  }

  /**
   * Generate type category
   *
   * @param synCat
   * @param lambdas
   * @param exists
   * @param bodyExpression
   * @param categoryType
   */
  private static void generateTypeOrEmpty(SyntacticCategory synCat,
      List<String> lambdas, Set<String> exists, StringObject bodyExpression,
      SemanticCategoryType categoryType) {

    /*-
     * 
     * (type head argument) = head(argument_>value)
     * (type president Obama) = president(Obama->value)
     * 
     */

    Preconditions.checkArgument(synCat.isBasic(), "Category should be basic");

    Preconditions.checkArgument(categoryType == SemanticCategoryType.TYPE
        || categoryType == SemanticCategoryType.EMPTY,
        "Basic category should either be EMPTY or TYPE types");

    Set<CategoryIndex> variables = synCat.getAllVariables();
    Preconditions.checkArgument(variables.size() == 1,
        "Basic expression has more than one variable");

    CategoryIndex var = variables.iterator().next();
    String varName = var.getVarNameAndKey();

    lambdas.add(varName);
    String body = String.format("(%s %s %s)", categoryType, varName, varName);
    bodyExpression.setString(body);
  }

  /**
   * Reduces the current semantic category with the given argument category.
   * Note: we do not perform alpha reduction since no two variables (unless they
   * indicate the same variable) in each of the semantic category have the same
   * names. Perform alpha reduction in case if you are using your own semantic
   * categories which require alpha reduction.
   *
   * @param semCat
   * @return
   */
  public SemanticCategory reduce(SemanticCategory semCat) {
    List<ConstantExpression> arguments = this.getArguments();
    Preconditions.checkArgument(arguments.size() > 0, "Cannot reduce " + this
        + " with " + semCat);
    ConstantExpression var = arguments.get(0);
    Expression exp = reduceArgument(var, semCat);
    exp = exp.removeDuplicateVariables();
    return new SemanticCategory(exp);
  }

  /**
   * Perform composition
   *
   * f g -> (lambda z (f (g z)))
   *
   * @param semCat
   * @return
   */
  public SemanticCategory composition(SemanticCategory semCat) {
    // f g -> (lambda z (f (g z)))
    return generalisedComposition(semCat, 1);
  }

  /**
   *
   * Perform generalised forward composition. Requires depth of the composition.
   *
   * @param semCat2
   * @param compositionDepth
   * @return
   */
  public SemanticCategory generalisedComposition(SemanticCategory semCat2,
      int compositionDepth) {

    Preconditions.checkArgument(compositionDepth > 0,
        "Cannot peform composition");

    List<ConstantExpression> args2 = Lists.newArrayList(semCat2.getArguments());
    List<ConstantExpression> args1 = Lists.newArrayList();
    while (compositionDepth > 0) {

      args1.add(0, args2.remove(0));
      compositionDepth--;
    }

    SemanticCategory reducedSemCat2 =
        new SemanticCategory(args2, semCat2.getBody());

    // alpha reduction - renaming variables when they are shared
    String reducedSemCat2String = reducedSemCat2.toString();
    List<ConstantExpression> args1New = Lists.newArrayList();
    for (ConstantExpression arg1 : args1) {
      String arg1String = arg1.toString();

      List<String> arg1Parts =
          Lists.newArrayList(Splitter.on(":").trimResults().omitEmptyStrings()
              .split(arg1String));
      if (arg1Parts.size() > 1) {
        arg1Parts.add(1, ":");
      }
      arg1Parts.add(1, "x");
      String arg1NewString = Joiner.on("").join(arg1Parts);
      reducedSemCat2String =
          reducedSemCat2String.replace(arg1String + " ", arg1NewString + " ");
      reducedSemCat2String =
          reducedSemCat2String.replace(arg1String + ")", arg1NewString + ")");
      ConstantExpression arg1New = new ConstantExpression(arg1NewString);
      args1New.add(arg1New);
    }
    reducedSemCat2 =
        new SemanticCategory(
            ExpressionParser.parseSingleExpression(reducedSemCat2String));

    SemanticCategory resultSemCat = this.reduce(reducedSemCat2);

    args1New.addAll(resultSemCat.getArguments());
    resultSemCat = new SemanticCategory(args1New, resultSemCat.getBody());
    return resultSemCat;
  }

  /**
   *
   * Returns the type raised category
   *
   * @param syntacticCategory
   * @param semCat
   * @return
   */
  public static SemanticCategory typeRaising(
      SyntacticCategory syntacticCategory, SemanticCategory semCat) {
    SyntacticCategory parent = syntacticCategory.getParent();

    Preconditions
        .checkArgument(
            parent.isVar(),
            "The category is either not a type raised category or already instantiated category");

    CategoryIndex var = parent.getIndex();
    String varName = var.getVarNameAndKey();

    List<String> lambdas = Lists.newArrayList();
    Set<String> exists = Sets.newHashSet();
    StringObject bodyExpression = new StringObject();

    // SemanticCategory newSemCat = generateExpression(syntacticCategory,
    // SemanticCategoryType.CLOSED);
    getDeepExpression(syntacticCategory.getArgument(), lambdas, exists,
        bodyExpression);
    lambdas.add(varName);
    exists.removeAll(lambdas);

    String newBody = bodyExpression.getString();
    List<String> bodyExpressions = Lists.newArrayList();
    bodyExpressions.add(newBody);

    List<ApplicationExpression> parts = semCat.getContentExpressions();
    for (Expression part : parts)
      bodyExpressions.add(part.toString());

    SemanticCategory newCat =
        generateSemanticCategory(lambdas, exists, bodyExpressions);
    return newCat;
  }

  /**
   * Useful for unary and binary rules.
   *
   * @param vars
   * @param lambdaExpression
   * @return
   */
  private static SemanticCategory unifyVarsAndCreateSemanticCategory(
      Set<CategoryIndex> vars, String lambdaExpression) {
    Pattern lambdaPattern = Pattern.compile("lambda ([^\\(]+)");
    Matcher matcher = lambdaPattern.matcher(lambdaExpression);
    Set<String> stringVars = Sets.newHashSet();
    while (matcher.find()) {
      Set<String> tmpVars =
          Sets.newHashSet(Splitter.on(CharMatcher.WHITESPACE).trimResults()
              .omitEmptyStrings().split(matcher.group(1)));
      stringVars.addAll(tmpVars);
    }

    Pattern existsPattern = Pattern.compile("exists ([^\\(]+)");
    matcher = existsPattern.matcher(lambdaExpression);
    while (matcher.find()) {
      Set<String> tmpVars =
          Sets.newHashSet(Splitter.on(CharMatcher.WHITESPACE).trimResults()
              .omitEmptyStrings().split(matcher.group(1)));
      stringVars.addAll(tmpVars);
    }

    String newLambdaExpression = lambdaExpression;
    for (CategoryIndex var : vars) {
      String oldLambdaExpression = "";
      String varName = "$" + var.getVariableName();
      String newVarName = var.getVarNameAndKey();
      if (stringVars.contains(varName)) {
        stringVars.remove(varName);
        // maximal replacement
        while (!oldLambdaExpression.equals(newLambdaExpression)) {
          oldLambdaExpression = newLambdaExpression;
          newLambdaExpression =
              newLambdaExpression.replaceAll(String.format(
                  "([\\s\\(\\)]+)(%s)([\\s\\(\\)]+)", Pattern.quote(varName)),
                  String.format("%s%s%s", "$1", newVarName.replace("$", "\\$"),
                      "$3"));
        }
      }
    }
    Expression exp =
        ExpressionParser.parseSingleExpression(newLambdaExpression);
    return new SemanticCategory(exp);
  }

  /**
   * Create semantic category given a syntactic category and its corresponding
   * lambda rule
   *
   * @param synCat
   * @param lambdaRule
   * @return
   */
  public static SemanticCategory generateSemanticCategory(
      SyntacticCategory synCat, String lambdaRule) {
    Set<CategoryIndex> vars = synCat.getAllVariables();
    SemanticCategory newCat =
        unifyVarsAndCreateSemanticCategory(vars, lambdaRule);
    return newCat;
  }

  /**
   * Apply a unary rule.
   *
   * @param leftSyntacticCategory
   * @param newSyntacticCategory
   * @param leftSemanticCategoryOld
   * @param lambdaConversionRule
   * @return
   */
  public static SemanticCategory applyUnaryRule(
      SyntacticCategory leftSyntacticCategory,
      SyntacticCategory newSyntacticCategory,
      SemanticCategory leftSemanticCategoryOld, String lambdaConversionRule) {

    SemanticCategory newCat;
    if (lambdaConversionRule != null && !lambdaConversionRule.equals("")) {
      // lambda rule conversion present
      Set<CategoryIndex> vars = Sets.newHashSet();
      vars.addAll(leftSyntacticCategory.getAllVariables());
      vars.addAll(newSyntacticCategory.getAllVariables());

      newCat = unifyVarsAndCreateSemanticCategory(vars, lambdaConversionRule);
      newCat = newCat.reduce(leftSemanticCategoryOld);
    } else {
      // lambda rule conversion absent. Construct semantics automatically
      List<String> lambdas = Lists.newArrayList();
      Set<String> exists = Sets.newHashSet();
      List<String> bodyExpressions = Lists.newArrayList();
      if (newSyntacticCategory.isBasic()) {
        lambdas.add(newSyntacticCategory.getIndex().getVarNameAndKey());
      } else {
        generateSemanticCategory(newSyntacticCategory,
            SemanticCategoryType.CLOSED, lambdas, exists, bodyExpressions);
      }

      List<ApplicationExpression> parts =
          leftSemanticCategoryOld.getContentExpressions();
      for (Expression part : parts)
        bodyExpressions.add(part.toString());

      if (bodyExpressions.size() == 0 && newSyntacticCategory.isBasic()) {
        newCat =
            generateSemanticCategory(newSyntacticCategory,
                SemanticCategoryType.IDENTITY);
      } else {
        newCat = generateSemanticCategory(lambdas, exists, bodyExpressions);
      }
    }
    return newCat;
  }

  /**
   *
   * Apply a binary rule
   *
   * @param leftSyntacticCategory
   * @param rightSyntacticCategory
   * @param newSyntacticCategory
   * @param leftSemanticCategoryOld
   * @param rightSemanticCategoryOld
   * @param lambdaConversionRule
   * @return
   */
  public static SemanticCategory applyBinaryRule(
      SyntacticCategory leftSyntacticCategory,
      SyntacticCategory rightSyntacticCategory,
      SyntacticCategory newSyntacticCategory,
      SemanticCategory leftSemanticCategoryOld,
      SemanticCategory rightSemanticCategoryOld, String lambdaConversionRule) {

    SemanticCategory newCat;
    if (lambdaConversionRule != null && !lambdaConversionRule.equals("")) {
      // lambda rule conversion present
      Set<CategoryIndex> vars = Sets.newHashSet();
      vars.addAll(leftSyntacticCategory.getAllVariables());
      vars.addAll(rightSyntacticCategory.getAllVariables());
      vars.addAll(newSyntacticCategory.getAllVariables());

      newCat = unifyVarsAndCreateSemanticCategory(vars, lambdaConversionRule);
      newCat = newCat.reduce(leftSemanticCategoryOld);
      newCat = newCat.reduce(rightSemanticCategoryOld);
    } else {
      // lambda rule conversion absent. Construct semantics automatically
      List<String> lambdas = Lists.newArrayList();
      Set<String> exists = Sets.newHashSet();
      List<String> bodyExpressions = Lists.newArrayList();
      generateSemanticCategory(newSyntacticCategory,
          SemanticCategoryType.CLOSED, lambdas, exists, bodyExpressions);

      List<ApplicationExpression> parts =
          leftSemanticCategoryOld.getContentExpressions();
      for (Expression part : parts)
        bodyExpressions.add(part.toString());

      parts = rightSemanticCategoryOld.getContentExpressions();
      for (Expression part : parts)
        bodyExpressions.add(part.toString());

      newCat = generateSemanticCategory(lambdas, exists, bodyExpressions);
    }
    return newCat;
  }

  /**
   *
   * returns right semantic category
   *
   * @param semCat1
   * @param semCat2
   * @return
   */
  public static SemanticCategory applyLeftIdentity(SemanticCategory semCat1,
      SemanticCategory semCat2) {
    return semCat2;
  }

  /**
   * returns left semantic category
   *
   * @param semCat1
   * @param semCat2
   * @return
   */
  public static SemanticCategory applyRightIdentity(SemanticCategory semCat1,
      SemanticCategory semCat2) {
    return semCat1;
  }

  /**
   * Extracts all the relations that have semantic types
   *
   * @return
   */
  private List<ApplicationExpression> getContentExpressions() {
    LambdaExpression lambda =
        new LambdaExpression(this.getArguments(), this.getBody());

    List<ApplicationExpression> parts = getApplicationExpressions(lambda);
    List<ApplicationExpression> validParts = Lists.newArrayList();
    for (ApplicationExpression part : parts) {
      if (SemanticCategoryType.types.contains(part.getFunction().toString())) {
        validParts.add(part);
      }
    }
    return validParts;
  }

  /**
   * Extracts all the relations that have semantic types
   *
   * @return
   */
  public List<String> getContentRelations() {
    LambdaExpression lambda =
        new LambdaExpression(this.getArguments(), this.getBody());

    List<ApplicationExpression> parts = getApplicationExpressions(lambda);
    List<String> validParts = Lists.newArrayList();
    for (ApplicationExpression part : parts) {
      if (SemanticCategoryType.types.contains(part.getFunction().toString())) {
        validParts.add(part.toString());
      }
    }
    return validParts;
  }

  public String toSimpleString() {
    String simpleString = this.toString();
    List<String> vars = this.getAllVariables();
    for (String var : vars) {
      String varName = var.split(":")[0];
      simpleString = simpleString.replace(var, varName);
    }
    return simpleString;
  }
}
