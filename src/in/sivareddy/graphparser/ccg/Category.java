package in.sivareddy.graphparser.ccg;

import org.apache.commons.lang3.tuple.Pair;

import in.sivareddy.graphparser.ccg.SemanticCategory.SemanticCategoryType;
import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;

import com.google.common.base.Preconditions;

public class Category {
  private SyntacticCategory synCat;
  private SemanticCategory semCat;

  public Category(SyntacticCategory synCat, SemanticCategory semCat) {
    this.synCat = synCat;
    this.semCat = semCat;
  }

  public static Category forwardApplication(Category cat1, Category cat2)
      throws BadParseException {
    SyntacticCategory synCat1 = cat1.getSyntacticCategory();
    SyntacticCategory synCat2 = cat2.getSyntacticCategory();
    SyntacticCategory resultSynCat =
        SyntacticCategory.forwardApplication(synCat1, synCat2);

    SemanticCategory semCat1 = cat1.getSemanticCategory();
    SemanticCategory semCat2 = cat2.getSemanticCategory();
    SemanticCategory resultSemCat = semCat1.reduce(semCat2);

    Category resultCategory = new Category(resultSynCat, resultSemCat);
    return resultCategory;
  }

  public static Category backwardApplication(Category cat1, Category cat2)
      throws BadParseException {
    SyntacticCategory synCat1 = cat1.getSyntacticCategory();
    SyntacticCategory synCat2 = cat2.getSyntacticCategory();
    SyntacticCategory resultSynCat =
        SyntacticCategory.backwardApplication(synCat1, synCat2);

    SemanticCategory semCat1 = cat1.getSemanticCategory();
    SemanticCategory semCat2 = cat2.getSemanticCategory();
    SemanticCategory resultSemCat = semCat2.reduce(semCat1);

    Category resultCategory = new Category(resultSynCat, resultSemCat);
    return resultCategory;
  }

  public static Category forwardComposition(Category cat1, Category cat2)
      throws BadParseException {
    // P1/P1;f P3/P4;g -> P1/P4; (lambda z (f (g z))
    SyntacticCategory synCat1 = cat1.getSyntacticCategory();
    SyntacticCategory synCat2 = cat2.getSyntacticCategory();
    SyntacticCategory resultSynCat =
        SyntacticCategory.forwardComposition(synCat1, synCat2);

    // Semantic composition
    SemanticCategory semCat1 = cat1.getSemanticCategory();
    SemanticCategory semCat2 = cat2.getSemanticCategory();
    SemanticCategory resultSemCat = semCat1.composition(semCat2);

    Category resultCategory = new Category(resultSynCat, resultSemCat);
    return resultCategory;
  }

  public static Category backwardComposition(Category cat1, Category cat2)
      throws BadParseException {
    // P4/P1;g P3\P4;f -> P3/P1; (lambda z (f (g z))
    SyntacticCategory synCat1 = cat1.getSyntacticCategory();
    SyntacticCategory synCat2 = cat2.getSyntacticCategory();
    SyntacticCategory resultSynCat =
        SyntacticCategory.backwardComposition(synCat1, synCat2);

    // Semantic composition
    SemanticCategory semCat1 = cat1.getSemanticCategory();
    SemanticCategory semCat2 = cat2.getSemanticCategory();
    SemanticCategory resultSemCat = semCat2.composition(semCat1);

    Category resultCategory = new Category(resultSynCat, resultSemCat);
    return resultCategory;
  }

  public static Category generalisedForwardComposition(Category cat1,
      Category cat2) throws BadParseException {
    // P1/P2;f ((P2/P3)/P4);g -> ((P1/P3)/P4); (lambda z w (f ((g z) w))

    // Syntactic Composition
    SyntacticCategory synCat1 = cat1.getSyntacticCategory();
    SyntacticCategory synCat2 = cat2.getSyntacticCategory();
    Pair<SyntacticCategory, Integer> resultSynCatPair =
        SyntacticCategory.generalisedForwardComposition(synCat1, synCat2);
    SyntacticCategory resultSynCat = resultSynCatPair.getLeft();
    Integer compositionDepth = resultSynCatPair.getRight();

    // Semantic composition
    SemanticCategory semCat1 = cat1.getSemanticCategory();
    SemanticCategory semCat2 = cat2.getSemanticCategory();
    SemanticCategory resultSemCat =
        semCat1.generalisedComposition(semCat2, compositionDepth);

    Category cat = new Category(resultSynCat, resultSemCat);
    return cat;
  }

  public static Category generalisedBackwardComposition(Category cat1,
      Category cat2) throws BadParseException {
    // ((P2/P3)/P4);g P1\P2;f -> ((P1/P3)/P4); (lambda z w (f ((g z) w))

    // Syntactic Composition
    SyntacticCategory synCat1 = cat1.getSyntacticCategory();
    SyntacticCategory synCat2 = cat2.getSyntacticCategory();
    Pair<SyntacticCategory, Integer> resultSynCatPair =
        SyntacticCategory.generalisedBackwardComposition(synCat1, synCat2);
    SyntacticCategory resultSynCat = resultSynCatPair.getLeft();
    Integer compositionDepth = resultSynCatPair.getRight();

    // Semantic composition
    SemanticCategory semCat1 = cat1.getSemanticCategory();
    SemanticCategory semCat2 = cat2.getSemanticCategory();
    SemanticCategory resultSemCat =
        semCat2.generalisedComposition(semCat1, compositionDepth);

    Category cat = new Category(resultSynCat, resultSemCat);
    return cat;
  }

  public static Category typeRaising(Category cat) {
    // Z:g -> X|(X|Z): (lambda $f (f g))

    SyntacticCategory synCat = cat.getSyntacticCategory();
    synCat = SyntacticCategory.typeRaising(synCat);

    SemanticCategory semCat = cat.getSemanticCategory();
    semCat = SemanticCategory.typeRaising(synCat, semCat);

    Category resultSemCat = new Category(synCat, semCat);
    return resultSemCat;
  }

  public static Category coordinationApplication(Category cat)
      throws BadParseException {
    SyntacticCategory argSynCat = cat.synCat;

    SyntacticCategory ccSynCat =
        SyntacticCategory.generateCoordinateCategory(argSynCat);
    SemanticCategory ccSemCat =
        SemanticCategory.generateSemanticCategory(ccSynCat,
            SemanticCategoryType.CLOSED);

    Category ccCat = new Category(ccSynCat, ccSemCat);
    Category returnCat = Category.forwardApplication(ccCat, cat);
    return returnCat;
  }

  public static Category applyUnaryRule(Category cat, String rule)
      throws BadParseException {
    SyntacticCategory leftSyntacticCategoryOld = cat.synCat;
    SemanticCategory leftSemanticCategoryOld = cat.semCat;

    String[] parts = rule.split("\t");
    Preconditions.checkArgument(parts.length >= 2, "Incorrect rule format");
    String lambdaConversionRule = null;
    if (parts.length > 2) {
      lambdaConversionRule = parts[2];
    }

    SyntacticCategory leftSyntacticCategory =
        SyntacticCategory.fromString(parts[0]);
    SyntacticCategory newSyntacticCategory =
        SyntacticCategory.fromString(parts[1]);

    SyntacticCategory.applyUnaryRuleByUnification(leftSyntacticCategoryOld,
        leftSyntacticCategory, newSyntacticCategory);
    SemanticCategory newSemanticCategory =
        SemanticCategory
            .applyUnaryRule(leftSyntacticCategory, newSyntacticCategory,
                leftSemanticCategoryOld, lambdaConversionRule);

    return new Category(newSyntacticCategory, newSemanticCategory);
  }

  public static Category applyBinaryRule(Category cat1, Category cat2,
      String rule) throws BadParseException {
    SyntacticCategory leftSyntacticCategoryOld = cat1.synCat;
    SemanticCategory leftSemanticCategoryOld = cat1.semCat;

    SyntacticCategory rightSyntacticCategoryOld = cat2.synCat;
    SemanticCategory rightSemanticCategoryOld = cat2.semCat;

    String[] parts = rule.split("\t");
    Preconditions.checkArgument(parts.length >= 3, "Incorrect rule format");
    String lambdaConversionRule = null;
    if (parts.length > 3) {
      lambdaConversionRule = parts[3];
    }

    SyntacticCategory leftSyntacticCategory =
        SyntacticCategory.fromString(parts[0]);
    SyntacticCategory rightSyntacticCategory =
        SyntacticCategory.fromString(parts[1]);
    SyntacticCategory newSyntacticCategory =
        SyntacticCategory.fromString(parts[2]);

    SyntacticCategory.applyBinaryRuleByUnification(leftSyntacticCategoryOld,
        rightSyntacticCategoryOld, leftSyntacticCategory,
        rightSyntacticCategory, newSyntacticCategory);
    SemanticCategory newSemanticCategory =
        SemanticCategory.applyBinaryRule(leftSyntacticCategory,
            rightSyntacticCategory, newSyntacticCategory,
            leftSemanticCategoryOld, rightSemanticCategoryOld,
            lambdaConversionRule);

    return new Category(newSyntacticCategory, newSemanticCategory);
  }

  public static Category applyLeftIdentity(Category cat1, Category cat2) {
    return cat2;
  }

  public static Category applyRightIdentity(Category cat1, Category cat2) {
    return cat1;
  }

  public SyntacticCategory getSyntacticCategory() {
    return synCat;
  }

  public SemanticCategory getSemanticCategory() {
    return semCat;
  }

  @Override
  public String toString() {
    return synCat.toString() + "; " + semCat.toString();
  }

  /**
   * Copies the category without unifying the variables in new category with
   * original category variables.
   *
   * @return
   */
  public Category shallowCopy() {
    SyntacticCategory synCatCopy = this.synCat.shallowCopy();
    String simpleSemCat = this.semCat.toSimpleString();
    SemanticCategory semCatCopy =
        SemanticCategory.generateSemanticCategory(synCatCopy, simpleSemCat);
    return new Category(synCatCopy, semCatCopy);
  }
}
