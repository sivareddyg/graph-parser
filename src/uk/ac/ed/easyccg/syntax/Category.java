package uk.ac.ed.easyccg.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableSet;

public abstract class Category {
  private final String asString;
  private final int id;
  private final static String WILDCARD_FEATURE = "X"; 
  private final static Set<String> bracketAndQuoteCategories = ImmutableSet.of("LRB", "RRB", "LQU", "RQU");

  private Category(String asString, String semanticAnnotation) {
    this.asString = asString + (semanticAnnotation == null ? "" : "{" + semanticAnnotation + "}");
    this.id = numCats.getAndIncrement();
  }

  public enum Slash {
    FWD, BWD, EITHER;
    public String toString()
    {
      String result = "";

      switch (this)
      {
      case FWD: result = "/"; break;
      case BWD: result = "\\"; break;
      case EITHER: result = "|"; break;

      }

      return result;
    }

    public static Slash fromString(String text) {
      if (text != null) {
        for (Slash slash : values()) {
          if (text.equalsIgnoreCase(slash.toString())) 
          {
            return slash;
          }
        }
      }
      throw new IllegalArgumentException("Invalid slash: " + text);
    }

    public boolean matches(Slash other)
    {
      return this == EITHER || this == other;
    }
  }

  private static AtomicInteger numCats = new AtomicInteger();
  private final static Map<String, Category> cache = new HashMap<String, Category>();
  public static final Category COMMA = Category.valueOf(",");
  public static final Category SEMICOLON = Category.valueOf(";");
  public static final Category CONJ = Category.valueOf("conj");
  public final static Category N = valueOf("N"); 
  public static final Category LQU = Category.valueOf("LQU");
  public static final Category LRB = Category.valueOf("LRB");
  public static final Category NP = Category.valueOf("NP");
  public static final Category PP = Category.valueOf("PP");
  public static final Category PREPOSITION = Category.valueOf("PP/NP");
  public static final Category PR = Category.valueOf("PR");

  public static Category valueOf(String cat) {

    Category result = cache.get(cat);
    if (result == null) {

      String name = Util.dropBrackets(cat);
      result = cache.get(name);

      if (result == null) {
        result = Category.valueOfUncached(name);
        if (name != cat) {
          synchronized(cache) {
            cache.put(name, result);
          }
        }
      }

      synchronized(cache) {
        cache.put(cat, result);
      }
    }

    return result;
  }

  /**
   * Builds a category from a string representation.
   */
  private static Category valueOfUncached(String source)
  {
    // Categories have the form: ((X/Y)\Z[feature]){ANNOTATION}
    String newSource = source;

    String semanticAnnotation;
    if (newSource.endsWith("}")) {
      int openIndex = newSource.lastIndexOf("{");
      semanticAnnotation = newSource.substring(openIndex + 1, newSource.length() - 1);
      newSource = newSource.substring(0, openIndex);
    } else {
      semanticAnnotation = null;
    }

    if (newSource.startsWith("("))
    {
      int closeIndex = Util.findClosingBracket(newSource);

      if (Util.indexOfAny(newSource.substring(closeIndex), "/\\|") == -1)
      {
        // Simplify (X) to X
        newSource = newSource.substring(1, closeIndex);
        Category result = valueOfUncached(newSource);

        return result;
      }
    }

    int endIndex = newSource.length();

    int opIndex =  Util.findNonNestedChar(newSource, "/\\|");


    if (opIndex == -1)
    {
      // Atomic Category
      int featureIndex = newSource.indexOf("[");
      List<String> features = new ArrayList<String>();

      String base = (featureIndex == -1 ? newSource : newSource.substring(0, featureIndex));

      while (featureIndex > -1)
      {
        features.add(newSource.substring(featureIndex + 1, newSource.indexOf("]", featureIndex)));
        featureIndex = newSource.indexOf("[", featureIndex + 1);
      }

      if (features.size() > 1) {
        throw new RuntimeException("Can only handle single features: " + source);
      }

      Category c = new AtomicCategory(base, features.size() == 0 ? null : features.get(0), semanticAnnotation);
      return c;
    }
    else
    {
      // Functor Category

      Category left = valueOf(newSource.substring(0, opIndex));
      Category right = valueOf(newSource.substring(opIndex + 1, endIndex));
      return new FunctorCategory(left, 
          Slash.fromString(newSource.substring(opIndex, opIndex + 1)), 
          right,
          semanticAnnotation
      );
    }
  }  

  public String toString() {
    return asString;
  }

  @Override
  public boolean equals(Object other) {
    return this == other;
  }

  @Override 
  public int hashCode() {
    return id;
  }

  abstract boolean isTypeRaised();
  abstract boolean isForwardTypeRaised();
  abstract boolean isBackwardTypeRaised();

  public abstract boolean isModifier();
  public abstract boolean matches(Category other);
  public abstract Category getLeft();
  public abstract Category getRight();
  abstract Slash getSlash();
  abstract String getFeature();

  abstract String toStringWithBrackets();
  public abstract Category dropPPandPRfeatures();

  static class FunctorCategory extends Category {
    private final Category left;
    private final Category right;
    private final Slash slash;
    private final boolean isMod;

    private FunctorCategory(Category left, Slash slash, Category right, String semanticAnnotation) {
      super(left.toStringWithBrackets() + slash + right.toStringWithBrackets(), semanticAnnotation);
      this.left = left;
      this.right = right;
      this.slash = slash;
      this.isMod = left.equals(right);

      // X|(X|Y)
      this.isTypeRaised = right.isFunctor() && right.getLeft().equals(left);
    }

    @Override
    public boolean isModifier()
    {
      return isMod;
    }

    @Override
    public boolean matches(Category other)
    {
      return other.isFunctor() && left.matches(other.getLeft()) && right.matches(other.getRight()) && slash.matches(other.getSlash());
    }

    @Override
    public Category getLeft()
    {
      return left;
    }

    @Override
    public Category getRight()
    {
      return right;
    }

    @Override
    Slash getSlash()
    {
      return slash;
    }

    @Override
    String getFeature()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    String toStringWithBrackets()
    {
      return "(" + toString() + ")";
    }

    @Override
    public boolean isFunctor()
    {
      return true;
    }

    @Override
    public boolean isPunctuation()
    {
      return false;
    }

    @Override
    String getType()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    String getSubstitution(Category other)
    {
      String result = getRight().getSubstitution(other.getRight());
      if (result == null) {
        // Bit of a hack, but seems to reproduce CCGBank in cases of clashing features.
        result = getLeft().getSubstitution(other.getLeft());
      }
      return result;
    }

    private final boolean isTypeRaised;
    @Override
    public boolean isTypeRaised()
    {
      return isTypeRaised;
    }

    @Override
    public boolean isForwardTypeRaised()
    {
      // X/(X\Y)
      return isTypeRaised() && getSlash() == Slash.FWD;
    }

    @Override
    public boolean isBackwardTypeRaised()
    {
      // X\(X/Y)
      return isTypeRaised() && getSlash() == Slash.BWD;
    }

    @Override
    boolean isNounOrNP()
    {
      return false;
    }

    @Override
    public Category addFeature(String preposition)
    {
      throw new RuntimeException("Functor categories cannot take features");
    }

    @Override
    public int getNumberOfArguments()
    {
      return 1 + left.getNumberOfArguments();
    }

    @Override
    public Category replaceArgument(int argNumber, Category newCategory)
    {
      if (argNumber == getNumberOfArguments()) {
        return Category.make(left, slash, newCategory);
      } else {
        return Category.make(left.replaceArgument(argNumber, newCategory), slash, right);
      }
    }

    @Override
    public Category getArgument(int argNumber)
    {
      if (argNumber == getNumberOfArguments()) {
        return right;
      } else {
        return left.getArgument(argNumber);
      }
    }

    @Override
    public Category getHeadCategory()
    {
      return left.getHeadCategory();
    }

    @Override
    public boolean isFunctionIntoModifier()
    {
      return isModifier() || left.isModifier();
    }

    @Override
    public boolean isFunctionInto(Category into)
    {
      return into.matches(this) || left.isFunctionInto(into);
    }

    @Override
    public Category dropPPandPRfeatures()
    {
      return Category.make(left.dropPPandPRfeatures(), slash, right.dropPPandPRfeatures());
    }
  }

  abstract String getSubstitution(Category other);


  static class AtomicCategory extends Category {

    private AtomicCategory(String type, String feature, String semanticAnnotation)
    {
      super(type + (feature == null ? "" : "[" + feature + "]"), semanticAnnotation);
      this.type = type;
      this.feature = feature;
      isPunctuation = !type.matches("[A-Za-z]+") || bracketAndQuoteCategories.contains(type);
    }

    private final String type;
    private final String feature;
    @Override
    public boolean isModifier()
    {
      return false;
    }

    @Override
    public boolean matches(Category other)
    {
      return !other.isFunctor() && type.equals(other.getType()) && 
      (feature == null || feature.equals(other.getFeature()) || WILDCARD_FEATURE.equals(getFeature()) || WILDCARD_FEATURE.equals(other.getFeature())
          || feature.equals("nb") // Ignoring the NP[nb] feature, which isn't very helpful. For example, it stops us coordinating "John and a girl",
          // because "and a girl" ends up with a NP[nb]\NP[nb] tag.
      );
    }

    @Override
    public Category getLeft()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public Category getRight()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    Slash getSlash()
    {
      throw new UnsupportedOperationException();
    }

    @Override
    String getFeature()
    {
      return feature;
    }

    @Override
    String toStringWithBrackets()
    {
      return toString();
    }

    @Override
    public boolean isFunctor()
    {
      return false;
    }

    private final boolean isPunctuation;
    @Override
    public boolean isPunctuation()
    {
      return isPunctuation ;
    }

    @Override
    String getType()
    {
      return type;
    }

    @Override
    String getSubstitution(Category other)
    {
      if (WILDCARD_FEATURE.equals(getFeature())) {
        return other.getFeature();
      } else if (WILDCARD_FEATURE.equals(other.getFeature())) {
        return feature;
      }
      return null;
    }

    @Override
    public boolean isTypeRaised()
    {
      return false;
    }

    public boolean isForwardTypeRaised()
    {
      return false;
    }

    public boolean isBackwardTypeRaised()
    {
      return false;
    }

    @Override
    boolean isNounOrNP()
    {
      return type.equals("N") || type.equals("NP");
    }

    @Override
    public Category addFeature(String newFeature)
    {
      if (feature != null) throw new RuntimeException("Only one feature allowed. Can't add feature: " + newFeature + " to category: " + this);
      newFeature = newFeature.replaceAll("/", "");
      newFeature = newFeature.replaceAll("\\\\", "");
      return valueOf(type + "[" + newFeature + "]");
    }

    @Override
    public int getNumberOfArguments()
    {
      return 0;
    }

    @Override
    public Category replaceArgument(int argNumber, Category newCategory)
    {
      if (argNumber == 0) return newCategory;
      throw new RuntimeException("Error replacing argument of category");
    }

    @Override
    public Category getArgument(int argNumber)
    {
      if (argNumber == 0) return this;
      throw new RuntimeException("Error getting argument of category");
    }

    @Override
    public Category getHeadCategory()
    {
      return this;
    }

    @Override
    public boolean isFunctionIntoModifier()
    {
      return false;
    }

    @Override
    public boolean isFunctionInto(Category into)
    {
      return into.matches(this);
    }

    @Override
    public Category dropPPandPRfeatures()
    {
      if (type.equals("PP") || type.equals("PP")) {
        return valueOf(type);
      } else {
        return this;
      }
    }    
  }

  public static Category make(Category left, Slash op, Category right)
  {
    return valueOf(left.toStringWithBrackets() + op + right.toStringWithBrackets());
  }

  abstract String getType();

  abstract boolean isFunctor();

  abstract boolean isPunctuation();

  /**
   * Returns the Category created by substituting all [X] wildcard features with the supplied argument.
   */
  Category doSubstitution(String substitution)
  {
    if (substitution == null) return this;
    return valueOf(toString().replaceAll(WILDCARD_FEATURE, substitution));
  }

  /**
   * A unique numeric identifier for this category.
   */
  int getID()
  {
    return id;
  }

  abstract boolean isNounOrNP();

  public abstract Category addFeature(String preposition);

  public abstract int getNumberOfArguments();
  public abstract Category getHeadCategory();
  public abstract boolean isFunctionInto(Category into);


  /**
   * Replaces the argument with the given index with the new Category. Arguments are count from the left, starting with 0 for the head Category.
   * e.g. (((S\NP)/NP)/PP)/PR @replaceArgument(3, PP[in]) =  (((S\NP)/NP)/PP[in])/PR
   */
  public abstract Category replaceArgument(int argNumber, Category newCategory);

  public abstract Category getArgument(int argNumber);
  public abstract boolean isFunctionIntoModifier();


}