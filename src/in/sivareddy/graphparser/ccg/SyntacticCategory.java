package in.sivareddy.graphparser.ccg;

import in.sivareddy.util.StringObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 * Represents syntactic category as an object. Unlike traditional syntactic categories, we use
 * indexed syntactic categories as our basic categories since indexes are highly important in
 * semantics. They are also crucial in finding dependencies between words. Our notation is powerful
 * than existing known indexing schemes for candc.
 *
 *  e.g. An indexed category: syntactic category ((S[dcl]{_}\NP{X}){_}/NP{Y}){_}
 *
 *  _ represents the current word. X represents left noun phrase, Y represents right noun phrase.
 *
 *  Dependencies can be specified as follows. ((S[dcl]{_}\NP{X}){_}/NP{Y}){_};_ subj X,_ obj Y,X rel
 * Y
 *
 *  Existing software for CCG does not allow to specify the relation between X and Y, but we support
 * it. This is useful in the case of coupla e.g. "Obama is the president", here a relation between
 * "Obama" and "president" can be specified unlike exisiting tools.
 *
 *  You can work with plain categories easily by assuming you do not have anything to do with
 * indices. If you pass a plain ccg category, we convert it into an indexed category automatically.
 * But you can ignore those indices without any harm.
 *
 *  Simple form of ((S[dcl]{1}\NP{2}){_}/NP{3}){_} is (S[dcl]\NP)/NP which is formed by removing all
 * the indices
 *
 * @author Siva Reddy
 */

public class SyntacticCategory {

  public static class IndexedDependency implements Comparable<IndexedDependency> {
    private CategoryIndex parent;
    private CategoryIndex child;
    private String relation;

    public IndexedDependency(CategoryIndex parent, String relation, CategoryIndex child) {
      this.parent = parent;
      this.child = child;
      this.relation = relation;
    }

    public CategoryIndex getParent() {
      return parent;
    }

    public CategoryIndex getChild() {
      return child;
    }

    public String getRelation() {
      return relation;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + parent.hashCode();
      result = prime * result + child.hashCode();
      result = prime * result + relation.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return Objects
          .toStringHelper(this)
          .add("head", parent)
          .add("rel", relation)
          .add("child", child)
          .toString();
    }

    @Override
    public int compareTo(IndexedDependency o) {
      int i = parent.getVariableName().compareTo(o.parent.getVariableName());
      if (i != 0) {
        return i;
      }
      i = relation.compareTo(o.relation);
      if (i != 0) {
        return i;
      }
      i = child.getVariableName().compareTo(o.child.getVariableName());
      return i;
    }
  }

  public enum Direction {
    LEFT("\\"), RIGHT("/"), ANY("|");

    private final String slash;
    private static String allDirections = "\\\\\\/\\|";

    private Direction(final String slash) {
      this.slash = slash;
    }

    @Override
    public String toString() {
      return slash;
    }

    public static Direction getDirection(char c) {
      Preconditions.checkArgument(allDirections.contains(String.valueOf(c)));
      if (c == '\\') {
        return LEFT;
      } else if (c == '/') {
        return RIGHT;
      } else {
        return ANY;
      }
    }

  };

  private static String restrictedPunctuation =
      String.format("\\(\\)\\[\\]\\{\\}\\<\\>%s", Direction.allDirections);
  private static String indexPatternString = String.format("\\{([^%s]+)\\}", restrictedPunctuation);

  private static String basicCategoryPatternString = String.format(
      "([^%s]+)(\\[[^%s]+\\])?(\\{[^%s]+\\})?", restrictedPunctuation, restrictedPunctuation,
      restrictedPunctuation);

  private static Pattern basicCategoryPattern =
      Pattern.compile(String.format("^%s$", basicCategoryPatternString));

  private static String dependencyPatternString = "([\\S]+)[\\s]+([\\S]+)[\\s]+([\\S]+)";
  private static Pattern dependencyPattern = Pattern.compile(dependencyPatternString);

  private SyntacticCategory parent;
  private SyntacticCategory argument;
  private Direction direction;
  // Index of the category: In NP[X]{Y}, Y is the index
  private CategoryIndex index;
  private Set<IndexedDependency> dependencies;

  // if the category structure is unknown, it is a variable category. Useful
  // in typeRaising.
  private boolean isVar;
  private int varKey;
  private static int varCount = 0;
  private static int MAXVARCOUNT = 10000;
  private static String varPrefix = "X";

  private boolean isBasic = false;
  // NP[feature]
  private String basicCategory;

  // TODO currently feature only for basic
  // categories, can extend this to complex categories. Additionally a
  // category can have multiple features e.g. NP[nb][conj].
  private StringObject feature;

  /**
   * Constructor for basic category
   *
   * @param basicCategory
   * @param feature
   * @param index
   */
  private SyntacticCategory(String basicCategory, StringObject feature, CategoryIndex index) {
    isBasic = true;
    isVar = false;
    this.basicCategory = basicCategory;
    this.index = index;

    if (feature != null) {
      this.feature = feature;
    }
  }

  /**
   * Constructor for complex category
   *
   * @param parentCategory
   * @param argumentCategory
   * @param variableIndex
   * @param argumentDirection
   */
  public SyntacticCategory(SyntacticCategory parentCategory, SyntacticCategory argumentCategory,
      CategoryIndex variableIndex, Direction argumentDirection) {
    this.parent = parentCategory;
    this.argument = argumentCategory;
    this.index = variableIndex;
    this.direction = argumentDirection;
    isVar = false;
  }

  public static synchronized int getVarCount() {
    varCount++;
    if (varCount % MAXVARCOUNT == 0) {
      varCount = 0;
    }
    return varCount;
  }

  /**
   * Constructor for variable category which will be instantiated only when combined with other
   * categories. Useful for type raising
   */
  public SyntacticCategory() {
    isVar = true;
    varKey = getVarCount();
    this.index = new CategoryIndex();
  }

  /**
   * copy category by unifying all index variables
   *
   * @param synCat
   */
  private void copyCategory(SyntacticCategory synCat) {
    Preconditions.checkArgument(synCat.isVar == false,
        "Cannot copy variable category. Variable should be instantiated before copying");
    this.index.unify(synCat.index);
    if (synCat.isBasic()) {
      isBasic = synCat.isBasic;
      isVar = synCat.isVar;
      this.basicCategory = synCat.basicCategory;
      this.feature = synCat.feature;
    } else {
      isVar = synCat.isVar;
      this.parent = synCat.parent;
      this.argument = synCat.argument;
      this.direction = synCat.direction;
    }
  }

  /*
   * copy the syntactic category without unifying the variables with original syntactic category
   * variables
   */
  public SyntacticCategory shallowCopy() {
    String synCatCopyString = this.toSimpleIndexString();
    SyntacticCategory synCatCopy = fromString(synCatCopyString);

    /*-Set<CategoryIndex> oldVars = this.getAllVariables();
    Map<String, CategoryIndex> nameToVar = new HashMap<>();
    for (CategoryIndex oldVar : oldVars) {
    	if (oldVar.getVariableValue().isInitialised()) {
    		String x = oldVar.getVariableName();
    		nameToVar.put(x, oldVar);
    	}
    }
    Set<CategoryIndex> newVars = synCatCopy.getAllVariables();
    for (CategoryIndex newVar : newVars) {
    	String x = newVar.getVariableName();
    	if (nameToVar.containsKey(x)) {
    		CategoryIndex oldVar = nameToVar.get(x);
    		newVar.unify(oldVar);
    	}
    }*/
    return synCatCopy;
  }

  public CategoryIndex getIndex() {
    return index;
  }

  public Direction getDirection() {
    return direction;
  }

  /**
   * Tells if the current category is basic
   *
   * @return
   */
  public boolean isBasic() {
    return isBasic;
  }

  /**
   * Set the index of the current category
   *
   * @param index
   */
  public void setIndexValue(int index) {
    this.index.setVariableValue(index);
  }

  /**
   * Get all the variables in this category
   *
   * @param variables
   */
  private void getAllVariables(Set<CategoryIndex> variables) {
    variables.add(index);
    if (isBasic) {
      return;
    } else {
      parent.getAllVariables(variables);
      argument.getAllVariables(variables);
    }
  }

  /**
   *
   * Get all the variables in this category
   *
   * @return
   */
  public Set<CategoryIndex> getAllVariables() {
    Set<CategoryIndex> variables = Sets.newHashSet();
    variables.add(index);
    if (isBasic || isVar) {
      return variables;
    } else {
      parent.getAllVariables(variables);
      argument.getAllVariables(variables);
    }
    return variables;
  }

  /**
   * Returns left side category
   *
   * @return
   */
  public SyntacticCategory getParent() {
    Preconditions.checkArgument(!isBasic, "Cannot be used with basic categories");
    return parent;
  }

  /**
   * Returns right side category
   *
   * @return
   */
  public SyntacticCategory getArgument() {
    Preconditions.checkArgument(!isBasic, "Cannot be used with basic categories");
    return argument;
  }

  /**
   * Converts any syntactic category to indexed category. If the indexes are already present, it
   * uses the indexes, or else new indexes are created. Automatic indexes are not linguistically
   * motivated.
   *
   * e.g. (S\N)/PP -> ((S{$X1}\N{$X2}){$X1}/PP{$X3}){$X4}
   *
   *
   * @param categoryString
   * @return
   *
   */
  public static SyntacticCategory fromString(String categoryString) {

    List<String> parts =
        Lists.newArrayList(Splitter.on(";").omitEmptyStrings().trimResults().split(categoryString));
    Preconditions.checkArgument(parts.size() > 0, "Malformed category: " + categoryString);

    Map<String, CategoryIndex> varCache = Maps.newHashMap();
    Map<String, StringObject> featureCache = Maps.newHashMap();
    SyntacticCategory category = fromStringHidden(parts.get(0), varCache, featureCache);
    /*-if (category.index.getVariableName()
    		.startsWith(CategoryIndex.varPrefix))
    	category.index.setVariableName("_");*/

if (parts.size() > 1) {
      category.processDependencies(parts.get(1), varCache);
    }

    return category;
  }

  private void processDependencies(String depString, Map<String, CategoryIndex> varCache) {
    List<String> depStrings =
        Lists.newArrayList(Splitter.on(",").omitEmptyStrings().trimResults().split(depString));
    if (dependencies == null) {
      dependencies = Sets.newHashSet();
    }
    for (String dep : depStrings) {
      Matcher matcher = dependencyPattern.matcher(dep);
      Preconditions.checkArgument(matcher.find(), "Malformed dependencies specified:" + dep);
      String head = matcher.group(1);
      String relation = matcher.group(2);
      String child = matcher.group(3);
      Preconditions.checkArgument(varCache.containsKey(head) && varCache.containsKey(child),
          "Unknown variables used in dependency: " + dep);
      IndexedDependency dependency =
          new IndexedDependency(varCache.get(head), relation, varCache.get(child));
      dependencies.add(dependency);
    }
  }

  private static SyntacticCategory fromStringHidden(String categoryString,
      Map<String, CategoryIndex> varCache, Map<String, StringObject> featureCache) {
    Matcher basicCategoryMatcher = basicCategoryPattern.matcher(categoryString);
    SyntacticCategory category = null;

    // Basic category
    if (basicCategoryMatcher.find()) {
      String basicCategory = basicCategoryMatcher.group(1);
      String feature = basicCategoryMatcher.group(2);
      String indexName = basicCategoryMatcher.group(3);
      if (feature != null) {
        feature = CharMatcher.anyOf("\\[\\]").removeFrom(feature);
      }
      CategoryIndex variableIndex;
      if (indexName != null) {
        indexName = CharMatcher.anyOf("\\{\\}\\*").removeFrom(indexName); // * indicates long
                                                                          // distance relation.
                                                                          // Could be useful in
                                                                          // future
        if (!varCache.containsKey(indexName)) {
          variableIndex = new CategoryIndex(indexName);
          varCache.put(variableIndex.getVariableName(), variableIndex);
        } else {
          variableIndex = varCache.get(indexName);
        }
      } else {
        variableIndex = new CategoryIndex();
        varCache.put(variableIndex.getVariableName(), variableIndex);
      }

      StringObject featureObject;
      if (featureCache.containsKey(feature)) {
        featureObject = featureCache.get(feature);
      } else {
        featureObject = new StringObject(feature);
      }

      if (feature != null) {
        featureCache.put(feature, featureObject);
      }

      category = new SyntacticCategory(basicCategory, featureObject, variableIndex);
    } else {
      // Complex category
      char[] cArray = categoryString.toCharArray();
      Preconditions.checkArgument(cArray.length > 2,
          "Malformed complex category: " + categoryString);

      boolean surroundedByBrackets = false;

      // find if category is surrounded by brackets
      if (cArray[0] == '(') {
        Stack<Integer> st = new Stack<>();
        st.add(0);
        int i;
        for (i = 1; i < cArray.length; i++) {
          if (cArray[i] == '(') {
            st.add(i);
          } else if (cArray[i] == ')') {
            st.pop();
            if (st.size() == 0) {
              break;
            }
          }
        }

        // the category is surrounded by brackets
        if (!CharMatcher.anyOf(Direction.allDirections).matchesAnyOf(categoryString.substring(i))) {
          surroundedByBrackets = true;
        }
      }

      if (!surroundedByBrackets) {
        categoryString = "(" + categoryString + ")";
      }

      Pattern indexPattern = Pattern.compile(indexPatternString + "$");
      Matcher matcher = indexPattern.matcher(categoryString);

      CategoryIndex variableIndex;
      // category has index
      if (matcher.find()) {
        String indexName = matcher.group(1);
        if (!varCache.containsKey(indexName)) {
          variableIndex = new CategoryIndex(indexName);
          varCache.put(variableIndex.getVariableName(), variableIndex);
        } else {
          variableIndex = varCache.get(indexName);
        }
      } else {
        variableIndex = new CategoryIndex();
        varCache.put(variableIndex.getVariableName(), variableIndex);
      }

      // Retrieve the parent and child arguments
      Pattern pattern = Pattern.compile("^\\((.+)\\)");
      matcher = pattern.matcher(categoryString);
      String subParts = "";
      if (matcher.find()) {
        subParts = matcher.group(1);
      }

      // Direction operators not surrounded by any brackets
      Stack<Integer> st = new Stack<>();
      List<Integer> directionPosition = Lists.newArrayList();
      cArray = subParts.toCharArray();
      for (int i = 0; i < cArray.length; i++) {
        if (cArray[i] == '(') {
          st.add(i);
        } else if (cArray[i] == ')') {
          st.pop();
        } else if (Direction.allDirections.contains(String.valueOf(cArray[i]))) {
          if (st.size() == 0) {
            directionPosition.add(i);
          }
        }
      }

      Preconditions.checkArgument(directionPosition.size() == 1,
          "Malformed category: " + categoryString);

      // Get parent and child categories
      int splitPosition = directionPosition.get(0);
      String parentString = subParts.substring(0, splitPosition);
      String childString = subParts.substring(splitPosition + 1);

      Direction direction = Direction.getDirection(cArray[splitPosition]);

      SyntacticCategory parenCategory = fromStringHidden(parentString, varCache, featureCache);
      SyntacticCategory childCategory = fromStringHidden(childString, varCache, featureCache);

      category = new SyntacticCategory(parenCategory, childCategory, variableIndex, direction);

    }
    return category;
  }

  /**
   * An exception to indicate a bad parse e.g. two conjunctions in a conjunction relation
   *
   */
  public static class BadParseException extends Exception {
    private static final long serialVersionUID = 1L;

    public BadParseException() {
      super();
    }

    public BadParseException(String message) {
      super(message);
    }

    public BadParseException(String message, Throwable cause) {
      super(message, cause);
    }

    public BadParseException(Throwable cause) {
      super(cause);
    }
  }

  /**
   *
   * Unify two syntactic categories by unifying indexes, features, directions. If the syntactic
   * category contains variables, those are assigned to relevant categories
   *
   * @param synCat
   * @throws BadParseException
   */
  public void unify(SyntacticCategory synCat) throws BadParseException {
    SyntacticCategory synCat1 = this;
    SyntacticCategory synCat2 = synCat;

    Preconditions.checkArgument((synCat1.isVar() && synCat2.isVar()) == false,
        "Cannot unify two unistantiated variables");

    if (synCat1.isVar) {
      synCat1.copyCategory(synCat2);
      return;
    } else if (synCat2.isVar) {
      synCat2.copyCategory(synCat1);
      return;
    }

    synCat1.unifyVariable(synCat2);
    synCat1.unifyDirection(synCat2);
    synCat1.unifyFeature(synCat2);

    if (synCat1.isBasic() && synCat2.isBasic()) {
      return;
    }
    // both of them should either be basic or not basic
    if ((synCat1.isBasic() || synCat2.isBasic())) {
      throw new BadParseException(
          "An exception to indicate a bad parse e.g. probable cause - two conjunctions in a conjunction relation");
    }

    SyntacticCategory parent1 = synCat1.getParent();
    SyntacticCategory parent2 = synCat2.getParent();

    SyntacticCategory arg1 = synCat1.getArgument();
    SyntacticCategory arg2 = synCat2.getArgument();

    arg1.unify(arg2);
    parent1.unify(parent2);
  }

  private void unifyFeature(SyntacticCategory synCat) {
    if ((this.isBasic && synCat.isBasic) == true) {
      StringObject feat1 = this.feature;
      StringObject feat2 = synCat.feature;
      if (feat1.getString() != null && feat1.getString().matches("^[A-Z].*")) {
        if (feat2.getString() != null && feat2.getString().matches("^[^A-Z].*")) {
          feat1.setString(feat2.getString());
        }
      } else if (feat2.getString() != null && feat2.getString().matches("^[A-Z]")) {
        if (feat1.getString() != null && feat1.getString().matches("^[^A-Z]")) {
          feat2.setString(feat1.getString());
        }
      }
    } else if (!synCat.isVar && !this.isVar && !synCat.isBasic && !this.isBasic) {
      SyntacticCategory parent1 = this.parent;
      SyntacticCategory parent2 = synCat.parent;
      parent1.unifyFeature(parent2);
      SyntacticCategory arg1 = this.argument;
      SyntacticCategory arg2 = synCat.argument;
      arg1.unifyFeature(arg2);
    }
  }

  private void unifyVariable(SyntacticCategory synCat) {
    CategoryIndex var1 = this.getIndex();
    CategoryIndex var2 = synCat.getIndex();
    var1.unify(var2);

    if (!synCat.isVar && !this.isVar && !synCat.isBasic && !this.isBasic) {
      SyntacticCategory parent1 = this.parent;
      SyntacticCategory parent2 = synCat.parent;
      parent1.unifyVariable(parent2);
      SyntacticCategory arg1 = this.argument;
      SyntacticCategory arg2 = synCat.argument;
      arg1.unifyVariable(arg2);
    }
  }

  private void unifyDirection(SyntacticCategory synCat2) {
    if (this.direction != Direction.ANY) {
      synCat2.direction = this.direction;
    } else if (synCat2.direction != Direction.ANY) {
      this.direction = synCat2.direction;
    }
  }

  /**
   * Return all dependencies
   *
   * @return
   */
  public Set<IndexedDependency> getDependencies() {
    return dependencies;
  }

  /**
   * Return all dependencies with the current index
   *
   * @param var
   * @return
   */
  public Set<IndexedDependency> getDependencies(CategoryIndex var) {
    Set<IndexedDependency> filteredDeps = Sets.newHashSet();
    if (dependencies == null) {
      return filteredDeps;
    }
    for (IndexedDependency dep : dependencies) {
      if (dep.parent.equals(var)) {
        filteredDeps.add(dep);
      }
    }
    return filteredDeps;
  }

  private SyntacticCategory application(SyntacticCategory synCat) throws BadParseException {
    SyntacticCategory synCat1 = this;
    SyntacticCategory synCat2 = synCat;

    SyntacticCategory arg1 = synCat1.getArgument();

    // this also unifies the variables in semantic categories since semantic
    // categories use the same index variables as syntactic variables
    arg1.unify(synCat2);
    SyntacticCategory resultSynCat = synCat1.getParent();
    return resultSynCat;
  }

  /**
   * Returns category formed by forward application
   *
   * @param synCat1
   * @param synCat2
   * @return
   * @throws BadParseException
   */
  public static SyntacticCategory forwardApplication(SyntacticCategory synCat1,
      SyntacticCategory synCat2) throws BadParseException {
    Preconditions.checkArgument(
        synCat1.getDirection() == Direction.RIGHT || synCat1.getDirection() == Direction.ANY,
        "Wrong directionality");
    SyntacticCategory synCat = synCat1.application(synCat2);
    synCat1.direction = Direction.RIGHT;
    return synCat;
  }

  /**
   * Returns category formed by backward application
   *
   * @param synCat1
   * @param synCat2
   * @return
   * @throws BadParseException
   */
  public static SyntacticCategory backwardApplication(SyntacticCategory synCat1,
      SyntacticCategory synCat2) throws BadParseException {
    Preconditions.checkArgument(
        synCat2.getDirection() == Direction.LEFT || synCat2.getDirection() == Direction.ANY,
        "Wrong directionality");

    SyntacticCategory synCat = synCat2.application(synCat1);
    synCat2.direction = Direction.LEFT;
    return synCat;
  }

  private SyntacticCategory composition(SyntacticCategory synCat) throws BadParseException {
    SyntacticCategory synCat1 = this;
    SyntacticCategory synCat2 = synCat;

    Preconditions.checkArgument(!synCat1.isBasic() && !synCat2.isBasic(),
        "One or more categories are basic. Cannot perform composition " + synCat1 + " " + synCat2);

    SyntacticCategory parent1 = synCat1.getParent();
    SyntacticCategory arg1 = synCat1.getArgument();

    SyntacticCategory parent2 = synCat2.getParent();
    SyntacticCategory arg2 = synCat2.getArgument();

    // Syntactic composition
    arg1.unify(parent2);
    // TODO not clear which will be the index of new category. Can use the
    // head information
    SyntacticCategory resultSyncat =
        new SyntacticCategory(parent1, arg2, new CategoryIndex(), synCat2.getDirection());
    // SyntacticCategory resultSyncat = new SyntacticCategory(parent1, arg2,
    // synCat2.getIndex(), synCat2.getDirection());

    return resultSyncat;
  }

  /**
   * Returns category formed by forward composition
   *
   * @param synCat1
   * @param synCat2
   * @return
   * @throws BadParseException
   */
  public static SyntacticCategory forwardComposition(SyntacticCategory synCat1,
      SyntacticCategory synCat2) throws BadParseException {
    Preconditions.checkArgument(!synCat1.isBasic() && !synCat2.isBasic(),
        "One or more categories are basic. Cannot perform composition " + synCat1 + " " + synCat2);

    Preconditions.checkArgument(
        synCat1.getDirection() == Direction.RIGHT || synCat1.getDirection() == Direction.ANY,
        "Wrong direction. Cannot perform composition");
    SyntacticCategory resultSynCat = synCat1.composition(synCat2);
    synCat1.direction = Direction.RIGHT;
    return resultSynCat;
  }

  /**
   * Returns category formed by backward composition
   *
   * @param synCat1
   * @param synCat2
   * @return
   * @throws BadParseException
   */
  public static SyntacticCategory backwardComposition(SyntacticCategory synCat1,
      SyntacticCategory synCat2) throws BadParseException {
    Preconditions.checkArgument(!synCat1.isBasic() && !synCat1.isBasic(),
        "One or more categories are basic. Cannot perform composition " + synCat1 + " " + synCat2);

    Preconditions.checkArgument(
        synCat2.getDirection() == Direction.LEFT || synCat2.getDirection() == Direction.ANY,
        "Wrong direction. Cannot perform composition");

    SyntacticCategory resultSynCat = synCat2.composition(synCat1);
    synCat2.direction = Direction.LEFT;
    return resultSynCat;

  }

  /**
   *
   * Returns the category formed by generalised forward composition along with the depth of the
   * argument category at which the composition is performed.
   *
   *  Example: for S/(X/X) and ((X/X)/X)/X, the depth is 2. S/X and ((X/X)/X)/X the depth is 3
   *
   * @param synCat
   * @return
   * @throws BadParseException
   */
  public static Pair<SyntacticCategory, Integer> generalisedForwardComposition(
      SyntacticCategory synCat1, SyntacticCategory synCat2) throws BadParseException {
    // P1/P2;f ((P2/P3)/P4);g -> ((P1/P3)/P4); (lambda z w (f ((g z) w))

    Preconditions.checkArgument(!synCat1.isBasic() && !synCat2.isBasic(),
        "One or more categories are basic. Cannot perform composition " + synCat1 + " " + synCat2);

    Preconditions.checkArgument(
        synCat1.getDirection() == Direction.RIGHT || synCat1.getDirection() == Direction.ANY,
        "Wrong direction. Cannot perform composition");

    Pair<SyntacticCategory, Integer> resultSynCatPair = synCat1.generalisedComposition(synCat2);
    synCat1.direction = Direction.RIGHT;
    return resultSynCatPair;
  }

  /**
   *
   * Returns the category formed by generalised backward composition along with the depth of the
   * argument category at which the composition is performed.
   *
   * @param synCat1
   * @param synCat2
   * @return
   * @throws BadParseException
   */
  public static Pair<SyntacticCategory, Integer> generalisedBackwardComposition(
      SyntacticCategory synCat1, SyntacticCategory synCat2) throws BadParseException {
    // ((P2/P3)/P4);g P1\P2;f -> ((P1/P3)/P4); (lambda z w (f ((g z) w))

    Preconditions.checkArgument(!synCat1.isBasic() && !synCat2.isBasic(),
        "One or more categories are basic. Cannot perform composition " + synCat1 + " " + synCat2);

    Preconditions.checkArgument(
        synCat2.getDirection() == Direction.LEFT || synCat2.getDirection() == Direction.ANY,
        "Wrong direction. Cannot perform composition");

    Pair<SyntacticCategory, Integer> resultSynCatPair = synCat2.generalisedComposition(synCat1);
    synCat2.direction = Direction.LEFT;
    return resultSynCatPair;
  }

  /**
   * Perform the generalised composition.
   *
   *  Returns the depth of the argument category at which the composition is performed.
   *
   *  Example: for S/(X/X) and ((X/X)/X)/X, the depth is 2. S/X and ((X/X)/X)/X the depth is 3
   *
   * @param synCat
   * @return
   * @throws BadParseException
   */
  private Pair<SyntacticCategory, Integer> generalisedComposition(SyntacticCategory synCat)
      throws BadParseException {
    // X/Y (Y/W)/Z -> (X/W)/Z

    SyntacticCategory synCat1 = this;
    SyntacticCategory synCat2 = synCat;

    Preconditions.checkArgument(!synCat2.isBasic(),
        this + " cannot compose with the basic category " + synCat2);
    SyntacticCategory parent1 = synCat1.getParent();
    SyntacticCategory arg1 = synCat1.getArgument();

    String arg1Skelton = arg1.getDepthFirstSkelton();
    Stack<SyntacticCategory> categoryStack = new Stack<>();
    Stack<CategoryIndex> indexStack = new Stack<>();
    Stack<Direction> directionStack = new Stack<>();

    // findout the category with which arg1 has to unify
    SyntacticCategory parent2 = synCat2;
    int depth = 0;
    while (true) {
      depth += 1;
      indexStack.add(parent2.getIndex());
      directionStack.add(parent2.getDirection());
      categoryStack.add(parent2.getArgument());

      parent2 = parent2.parent;

      String parentCatSkelton = parent2.getDepthFirstSkelton();
      if (parent2.isVar || arg1.isVar) {
        break;
      }
      if (parentCatSkelton.equals(arg1Skelton)) {
        break;
      }
    }

    arg1.unify(parent2);
    // build back the composed category removing arguments that have unified
    SyntacticCategory newCategory = parent1;
    while (categoryStack.size() > 0) {
      SyntacticCategory arg2 = categoryStack.pop();
      // CategoryIndex newIndex = indexStack.pop();
      Direction newDirection = directionStack.pop();

      // not clear what will be the new index. can use head information
      newCategory = new SyntacticCategory(newCategory, arg2, new CategoryIndex(), newDirection);
      // newCategory = new SyntacticCategory(newCategory, arg2, newIndex,
      // newDirection);
    }

    Pair<SyntacticCategory, Integer> pair = Pair.of(newCategory, depth);
    return pair;
  }

  /**
   * Returns the skelton B->Basic F -> NonBasic
   *
   * @return
   */
  private String getDepthFirstSkelton() {
    if (isBasic) {
      return "B";
    } else {
      return "F " + argument.getDepthFirstSkelton() + " " + parent.getDepthFirstSkelton();
    }
  }

  /**
   * Returns the category index of the deepest parent
   *
   * @return
   */
  public CategoryIndex getDeepCategoryIndex() {
    Preconditions.checkArgument(isVar == false, "Cannot get deep category for a variable category");
    if (isBasic) {
      return index;
    } else {
      return parent.getDeepCategoryIndex();
    }
  }

  /**
   *
   * Return the type raised category
   *
   * @param synCat
   * @return
   */
  public static SyntacticCategory typeRaising(SyntacticCategory synCat) {
    // A -> X|(X|A)
    SyntacticCategory var = new SyntacticCategory();
    CategoryIndex newVar = new CategoryIndex();
    SyntacticCategory newArgument = new SyntacticCategory(var, synCat, newVar, Direction.ANY);
    SyntacticCategory newCategory =
        new SyntacticCategory(var, newArgument, synCat.getIndex(), Direction.ANY);
    return newCategory;
  }

  /**
   * Returns the deepest parent
   *
   * @return
   */
  public SyntacticCategory getDeepParentCategory() {
    Preconditions.checkArgument(isVar == false, "Cannot get deep category for a variable category");
    if (isBasic) {
      return this;
    } else {
      return parent.getDeepParentCategory();
    }
  }

  /**
   * Gets the deepest right most child
   *
   * @return
   */
  public SyntacticCategory getDeepChildCategory() {
    Preconditions.checkArgument(isVar == false, "Cannot get deep category for a variable category");
    if (isBasic) {
      return this;
    } else {
      return argument.getDeepChildCategory();
    }
  }

  /**
   * Returns the category formed by replacing the old Index with new Index
   *
   * @param oldIndex
   * @param newIndex
   * @return
   */
  public SyntacticCategory replaceIndex(CategoryIndex oldIndex, CategoryIndex newIndex) {
    SyntacticCategory result = this;

    if (this.isBasic) {
      if (this.index.equals(oldIndex)) {
        result = SyntacticCategory.fromString(this.toSimpleIndexString());
        result.index = newIndex;
      }
    } else {
      SyntacticCategory arg = this.argument.replaceIndex(oldIndex, newIndex);
      SyntacticCategory parent = this.parent.replaceIndex(oldIndex, newIndex);
      CategoryIndex variableIndex = this.index;
      if (variableIndex.equals(oldIndex)) {
        variableIndex = newIndex;
      }
      result = new SyntacticCategory(parent, arg, variableIndex, this.direction);
    }

    return result;
  }

  /**
   * Generate coordinate category for a given category
   *
   * @param synCat
   * @return
   */
  public static SyntacticCategory generateCoordinateCategory(SyntacticCategory synCat) {
    String simpleCat = synCat.toSimpleIndexString();
    SyntacticCategory ccCat = SyntacticCategory.fromString(simpleCat);
    // SyntacticCategory deepParentCat = ccCat.getDeepParentCategory();
    // String deepParentCatString = deepParentCat.toSuperSimpleString();

    // cc head variable can contain multiple indexes
    CategoryIndex ccVar = ccCat.index;
    ccVar.setIsCC();

    // SyntacticCategory deepParentCat1 =
    // SyntacticCategory.fromString(deepParentCatString);
    // SyntacticCategory deepParentCat2 =
    // SyntacticCategory.fromString(deepParentCatString);
    CategoryIndex var1 = new CategoryIndex();
    CategoryIndex var2 = new CategoryIndex();
    ccVar.addtoCCVar(var1);
    ccVar.addtoCCVar(var2);

    SyntacticCategory cat1 = ccCat.replaceIndex(ccVar, var1);
    SyntacticCategory cat2 = ccCat.replaceIndex(ccVar, var2);

    // SyntacticCategory cat1 = ccCat
    // .replaceDeepCategoryAndVars(deepParentCat1);
    // SyntacticCategory cat2 = ccCat
    // .replaceDeepCategoryAndVars(deepParentCat2);

    SyntacticCategory XX1 = new SyntacticCategory(ccCat, cat1, ccVar, Direction.ANY);
    SyntacticCategory XX1X2 = new SyntacticCategory(XX1, cat2, ccVar, Direction.ANY);

    return XX1X2;
  }

  /**
   * Apply unary rule by unifying the indexes with same names in the old and new category.
   *
   * @param leftSyntacticCategoryOld
   * @param leftSyntacticCategory
   * @param outSyntacticCategory
   * @throws BadParseException
   */
  public static void applyUnaryRuleByUnification(SyntacticCategory leftSyntacticCategoryOld,
      SyntacticCategory leftSyntacticCategory, SyntacticCategory outSyntacticCategory)
      throws BadParseException {
    // S[adj]\NP{X} NP{X}\NP{X}

    leftSyntacticCategory.unify(leftSyntacticCategoryOld);

    Set<CategoryIndex> varsOld = leftSyntacticCategory.getAllVariables();
    Set<CategoryIndex> varsNew = outSyntacticCategory.getAllVariables();

    for (CategoryIndex oldVar : varsOld) {
      for (CategoryIndex newVar : varsNew) {
        if (newVar.getVariableName().equals(oldVar.getVariableName())) {
          newVar.unify(oldVar);
        }
      }
    }
  }

  /**
   *
   * Applies binary rule by unification of indexes with same names in the old categories and new
   * category.
   *
   * @param leftSyntacticCategoryOld
   * @param rightSyntacticCategoryOld
   * @param leftSyntacticCategory
   * @param rightSyntacticCategory
   * @param outCategory
   * @throws BadParseException
   */
  public static void applyBinaryRuleByUnification(SyntacticCategory leftSyntacticCategoryOld,
      SyntacticCategory rightSyntacticCategoryOld, SyntacticCategory leftSyntacticCategory,
      SyntacticCategory rightSyntacticCategory, SyntacticCategory outCategory)
      throws BadParseException {
    leftSyntacticCategory.unify(leftSyntacticCategoryOld);
    rightSyntacticCategory.unify(rightSyntacticCategoryOld);

    Set<CategoryIndex> vars1 = leftSyntacticCategory.getAllVariables();
    Set<CategoryIndex> vars2 = rightSyntacticCategory.getAllVariables();

    Set<CategoryIndex> varsNew = outCategory.getAllVariables();

    for (CategoryIndex oldVar : vars1) {
      for (CategoryIndex newVar : varsNew) {
        if (newVar.getVariableName().equals(oldVar.getVariableName())) {
          newVar.unify(oldVar);
        }
      }
    }

    for (CategoryIndex oldVar : vars2) {
      for (CategoryIndex newVar : varsNew) {
        if (newVar.getVariableName().equals(oldVar.getVariableName())) {
          newVar.unify(oldVar);
        }
      }
    }
  }

  /**
   * Apply left identity. Returns the right category
   *
   * @param synCat1
   * @param synCat2
   * @return
   */
  public static SyntacticCategory applyLeftIdentity(SyntacticCategory synCat1,
      SyntacticCategory synCat2) {
    return synCat2;
  }

  /**
   * Apply right identity. Returns the left category
   *
   * @param synCat1
   * @param synCat2
   * @return
   */
  public static SyntacticCategory applyRightIdentity(SyntacticCategory synCat1,
      SyntacticCategory synCat2) {
    return synCat1;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (isVar) {
      sb.append(varPrefix + varKey);
    } else if (isBasic) {
      // Basic category
      sb.append(basicCategory);
      if (feature != null && feature.getString() != null) {
        sb.append("[");
        sb.append(feature);
        sb.append("]");
      }
      sb.append("{");
      sb.append(index.toSimpleString());
      sb.append("}");
    } else {
      // complex category
      String parentString = parent.toString();
      String argumentString = argument.toString();
      sb.append("(");
      sb.append(parentString);
      sb.append(direction.toString());
      sb.append(argumentString);
      sb.append(")");
      sb.append("{");
      sb.append(index.toSimpleString());
      sb.append("}");
    }
    return sb.toString();
  }

  /**
   * Returns ccg categories without features
   *
   * @return
   */
  public String toSimpleIndexString() {
    StringBuilder sb = new StringBuilder();
    if (isVar) {
      sb.append(varPrefix + varKey);
    } else if (isBasic) {
      // Basic category
      sb.append(basicCategory);
      if (feature != null && feature.getString() != null) {
        sb.append("[");
        sb.append(feature);
        sb.append("]");
      }
      sb.append("{");
      sb.append(index.toSimpleIndexString());
      sb.append("}");
    } else {
      // complex category
      String parentString = parent.toSimpleIndexString();
      String argumentString = argument.toSimpleIndexString();
      sb.append("(");
      sb.append(parentString);
      sb.append(direction.toString());
      sb.append(argumentString);
      sb.append(")");
      sb.append("{");
      sb.append(index.toSimpleIndexString());
      sb.append("}");
    }
    return sb.toString();
  }

  /**
   * Returns simple ccg categories without indices
   *
   * @return
   */
  public String toSimpleString() {
    StringBuilder sb = new StringBuilder();
    if (isVar) {
      sb.append(varPrefix + varKey);
    } else if (isBasic) {
      // Basic category
      sb.append(basicCategory);
      if (feature != null && feature.getString() != null) {
        sb.append("[");
        sb.append(feature);
        sb.append("]");
      }
    } else {
      // complex category
      String parentString = parent.toSimpleString();
      String argumentString = argument.toSimpleString();
      sb.append("(");
      sb.append(parentString);
      sb.append(direction.toString());
      sb.append(argumentString);
      sb.append(")");
    }
    return sb.toString();
  }

  /**
   * Returns simple ccg categories without indices and features
   *
   * @return
   */
  public String toSuperSimpleString() {
    StringBuilder sb = new StringBuilder();
    if (isVar) {
      sb.append(varPrefix + varKey);
    } else if (isBasic) {
      // Basic category
      sb.append(basicCategory);
    } else {
      // complex category
      String parentString = parent.toSuperSimpleString();
      String argumentString = argument.toSuperSimpleString();
      sb.append("(");
      sb.append(parentString);
      sb.append(direction.toString());
      sb.append(argumentString);
      sb.append(")");
    }
    return sb.toString();
  }

  /**
   * returns whether the category is instantiated or is a variable.
   *
   * @return
   */
  public boolean isVar() {
    return isVar;
  }

}
