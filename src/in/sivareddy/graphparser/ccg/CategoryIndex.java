package in.sivareddy.graphparser.ccg;

import in.sivareddy.util.IntegerObject;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Class useful to represent the variables used in a syntactic category. e.g. in
 * NP{X}, X represents the variable.
 * 
 * @author Siva Reddy
 * 
 */
public class CategoryIndex implements Serializable {
  private static final long serialVersionUID = -3142867484224764555L;
  private IntegerObject variableValue = null;
  private String variableName;

  public static String varPrefix = "X";

  // Store all the variables that are unified with the current object
  private Set<CategoryIndex> unifiedVariables = Sets.newHashSet();

  // To count the number of Category Index variables
  private static int keyCount = 0;
  private static Map<Integer, CategoryIndex> varCache = Maps.newConcurrentMap();
  private static Integer maxKeyCount = 100000;

  // useful for variables indicating coordinate categories
  private boolean isCC = false;
  private Set<CategoryIndex> coordinatedVars = null;

  // unique value specific to this Category.
  private Integer key;

  public static synchronized int getKeyCount() {
    keyCount++;
    if (keyCount % maxKeyCount == 0)
      keyCount = 0;
    return keyCount;
  }

  public void setKey() {
    key = getKeyCount();
    varCache.put(key, this);
  }

  public static void deleteVarKey(Integer key) {
    if (varCache.containsKey(key)) {
      varCache.remove(key);
    }
  }

  public static boolean containsVarKey(Integer key) {
    return key == null ? false : varCache.containsKey(key);
  }

  public static CategoryIndex getCategoryIndex(String varNameKey) {
    if (varNameKey == null || varNameKey.equals("")
        || varNameKey.charAt(0) != '$')
      return null;
    varNameKey = varNameKey.replace("$", "");
    List<String> vars = Lists.newArrayList(Splitter.on(":").split(varNameKey));
    if (vars.size() != 2)
      return null;
    String varName = vars.get(0);
    Integer key = Integer.parseInt(vars.get(1));
    CategoryIndex cat = null;
    if (key != null && varCache.containsKey(key))
      cat = varCache.get(key);
    else
      return null;

    if (cat.getVariableName().equals(varName))
      return cat;
    return null;
  }

  public int getKey() {
    return key;
  }

  public String getVarNameAndKey() {
    return "$" + variableName + ":" + key;
  }

  public static synchronized void resetCounter() {
    keyCount = 0;
    varCache = Maps.newConcurrentMap();
  }

  public CategoryIndex(String variableName, int variableValue) {
    this.variableValue = new IntegerObject(variableValue);
    this.variableName = variableName;
    unifiedVariables.add(this);
    setKey();
  }

  public CategoryIndex(String variableName) {
    this.variableName = variableName;
    unifiedVariables.add(this);
    setKey();
  }

  public CategoryIndex() {
    setKey();
    this.variableName = varPrefix + key;
    unifiedVariables.add(this);
  }

  public void setIsCC() {
    isCC = true;
    coordinatedVars = Sets.newHashSet();
  }

  public boolean isCC() {
    return isCC;
  }

  public Set<CategoryIndex> getCCvars() {
    return getCCvars(new HashSet<>());
  }

  public Set<CategoryIndex> getCCvars(Set<CategoryIndex> visitedVars) {
    Set<CategoryIndex> ccVars = new HashSet<>();
    for (CategoryIndex var : coordinatedVars) {
      // Make sure there are no cyclic dependencies between cc variables.
      if (visitedVars.contains(var))
        continue;
      visitedVars.add(var);

      if (var.isCC()) {
        ccVars.addAll(var.getCCvars(visitedVars));
      } else {
        ccVars.add(var);
      }
    }
    return ccVars;
  }

  public static CategoryIndex ccCategoryIndex() {
    CategoryIndex index = new CategoryIndex();
    index.setIsCC();
    return index;
  }

  public IntegerObject getVariableValue() {
    return variableValue;
  }

  public String getVariableName() {
    return variableName;
  }

  public void setVariableName(String name) {
    variableName = name;
  }

  private void setVariableValue(IntegerObject value) {
    for (CategoryIndex var : unifiedVariables)
      var.variableValue = value;
  }

  public void setVariableValue(int value) {
    Preconditions.checkArgument(
        variableValue == null || !variableValue.isInitialised(),
        this.toString() + " already initialised");
    if (variableValue == null) {
      IntegerObject newValue = new IntegerObject(value);
      setVariableValue(newValue);
    } else if (!variableValue.isInitialised()) {
      variableValue.setValue(value);
    }
  }

  private void setUnifiedCategoryVariables(Set<CategoryIndex> variables) {
    for (CategoryIndex variable : unifiedVariables) {
      variables.add(variable);
    }
    unifiedVariables = variables;
  }

  public boolean addtoCCVar(CategoryIndex foreignVar) {
    Preconditions.checkArgument(isCC == true, this + " is not a ccVar");
    coordinatedVars.add(foreignVar);
    return true;
  }

  /**
   * Unifies the variables. If one of the variable's content change, then the
   * other variable's content also changes. Once unified, they are unified
   * forever.
   * 
   * @param foreignVar
   * @return
   * 
   */
  public boolean unify(CategoryIndex foreignVar) {
    if (foreignVar == null)
      return false;

    if (unifiedVariables.contains(foreignVar)) // already unified
      return true;

    if (isCC || foreignVar.isCC) {
      if (!isCC)
        setIsCC();
      if (!foreignVar.isCC)
        foreignVar.setIsCC();
      unifyTwoCCVars(this, foreignVar);
      return true;
    }

    IntegerObject foreignValue = foreignVar.getVariableValue();

    if (variableValue == null && foreignValue == null) {
      IntegerObject newValue = new IntegerObject();
      setVariableValue(newValue);
      foreignVar.setVariableValue(newValue);
    } else if (variableValue == null) {
      setVariableValue(foreignValue);
    } else if (foreignValue == null) {
      foreignVar.setVariableValue(variableValue);
    } else {
      if (foreignValue.isInitialised() && variableValue.isInitialised()) {
        if (foreignValue.getValue() == variableValue.getValue())
          foreignVar.setVariableValue(variableValue);
        else
          // cannot unify if the variables are already
          // instantiated
          return false;
      } else if (foreignValue.isInitialised()) {
        setVariableValue(foreignValue);
      } else {
        foreignVar.setVariableValue(variableValue);
      }
    }

    // copy all the unified variables
    foreignVar.setUnifiedCategoryVariables(unifiedVariables);
    return true;
  }

  private boolean unifyTwoCCVars(CategoryIndex ccvar1, CategoryIndex ccvar2) {

    if (ccvar1.unifiedVariables.contains(ccvar2)) // already unified
      return true;

    Set<CategoryIndex> vars1 = ccvar1.coordinatedVars;
    Set<CategoryIndex> vars2 = ccvar2.coordinatedVars;
    vars2.addAll(vars1);

    Set<CategoryIndex> ccvars1Unified = ccvar1.unifiedVariables;

    for (CategoryIndex var : ccvars1Unified) {
      var.coordinatedVars = vars2;
    }

    ccvar2.setUnifiedCategoryVariables(ccvars1Unified);
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    if (unifiedVariables == ((CategoryIndex) obj).unifiedVariables)
      return true;
    return false;
  }

  @Override
  public String toString() {
    String value = "?";
    if (variableValue != null)
      value = variableValue.toString();
    return Objects.toStringHelper(this).add(variableName, value).toString();
  }

  public String toSimpleString() {
    String value = "?";
    if (variableValue != null)
      value = variableValue.toString();
    return variableName + "=" + value;
  }

  public String toSimpleIndexString() {
    return variableName;
  }

  /**
   * Returns the same string for all unified variables.
   * 
   * @return
   */
  public String toUnifiedString() {
    return unifiedVariables.iterator().next().variableName;
  }
}
