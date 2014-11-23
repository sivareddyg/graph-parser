package in.sivareddy.graphparser.ccg.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import in.sivareddy.graphparser.ccg.Category;
import in.sivareddy.graphparser.ccg.CategoryIndex;
import in.sivareddy.graphparser.ccg.SemanticCategory;
import in.sivareddy.graphparser.ccg.SyntacticCategory;
import in.sivareddy.graphparser.ccg.SemanticCategory.SemanticCategoryType;
import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;

public class CategoryTest {

  @Test
  public void testApplication() throws BadParseException {
    SyntacticCategory synCat1 = SyntacticCategory.fromString("NP{X}");
    SemanticCategory semCat1 =
        SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.TYPE);

    SyntacticCategory synCat2 = SyntacticCategory.fromString("(NP{Y}/NP{Y}){_};_ 1 Y");
    SemanticCategory semCat2 =
        SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.TYPEMOD);

    SyntacticCategory synCat3 = SyntacticCategory.fromString("(N{Z}/NP{K}){W}");

    synCat3.unify(synCat2);
    assertTrue(synCat3.getIndex().equals(synCat2.getIndex()));

    Category cat1 = new Category(synCat1, semCat1);
    Category cat2 = new Category(synCat2, semCat2);

    try {
      Category.forwardApplication(cat1, cat2);
      fail("Wrong implementation of forward application");
    } catch (Exception e) {
      // nothing to do;
    }

    Category cat = Category.forwardApplication(cat2, cat1);
    assertEquals(cat.getSyntacticCategory().toSimpleString(), "NP");

    synCat2 = SyntacticCategory.fromString("(NP{Y}\\NP{Y}){_};_ 1 Y");
    semCat2 = SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.TYPEMOD);

    cat2 = new Category(synCat2, semCat2);
    cat = Category.backwardApplication(cat1, cat2);
    assertEquals(cat.getSyntacticCategory().toSimpleString(), "NP");

    // albeit illegally
    synCat1 =
        SyntacticCategory.fromString(
            "(((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}/((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}){_}");
    synCat2 = SyntacticCategory.fromString("((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){_};_ 1 Y");
    semCat1 = SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.CLOSED);
    semCat2 = SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.EVENTMOD);

    cat1 = new Category(synCat1, semCat1);
    cat2 = new Category(synCat2, semCat2);

    cat = Category.forwardApplication(cat1, cat2);
    assertEquals(cat.getSyntacticCategory().toSuperSimpleString(), "((S\\NP)\\(S\\NP))");

  }

  @Test
  public void testComposition() throws BadParseException {
    // Composition
    // might prove
    SyntacticCategory synCat1 =
        SyntacticCategory.fromString("((S[dcl]{_}\\NP{Y}){_}/(S[b]{Z}\\NP{Y}){Z}){_};_ 1 Y,_ 2 Z");
    SyntacticCategory synCat2 =
        SyntacticCategory.fromString("((S[b]{_}\\NP{Y}){_}/NP{Z}){_};_ 1 Y,_ 2 Z");
    SemanticCategory semCat1 =
        SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.EVENT);
    SemanticCategory semCat2 =
        SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.EVENT);

    Category cat1 = new Category(synCat1, semCat1);
    Category cat2 = new Category(synCat2, semCat2);

    Category cat = Category.forwardComposition(cat1, cat2);
    assertEquals(cat.getSyntacticCategory().toSuperSimpleString(), "((S\\NP)/NP)");


    synCat1 =
        SyntacticCategory.fromString("((S[dcl]{_}\\NP{Y}){_}/(S[b]{Z}\\NP{Y}){Z}){_};_ 1 Y,_ 2 Z");
    synCat2 = SyntacticCategory.fromString("((S[b]{_}\\NP{Y}){_}/NP{Z}){_};_ 1 Y,_ 2 Z");
    semCat1 = SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.EVENT);
    semCat2 = SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.EVENT);

    cat1 = new Category(synCat1, semCat1);
    cat2 = new Category(synCat2, semCat2);

    cat = Category.generalisedForwardComposition(cat1, cat2);
    assertEquals(cat.getSyntacticCategory().toSuperSimpleString(), "((S\\NP)/NP)");

    // Example from Mark's Quick introduction to CCG.
    synCat1 = SyntacticCategory.fromString("((VP{X}/PP)/((VP{X}/PP)/NP))");
    synCat2 = SyntacticCategory.fromString("(VP{Y}\\(VP{Y}/PP))");
    semCat1 = SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.CLOSED);
    semCat2 = SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.CLOSED);

    cat1 = new Category(synCat1, semCat1);
    cat2 = new Category(synCat2, semCat2);

    cat = Category.generalisedBackwardComposition(cat1, cat2);
    assertEquals(cat.getSyntacticCategory().toSuperSimpleString(), "(VP/((VP/PP)/NP))");

    cat = Category.backwardComposition(cat1, cat2);
    assertEquals(cat.getSyntacticCategory().toSuperSimpleString(), "(VP/((VP/PP)/NP))");
  }

  @Test
  public void testTypeRaising() throws BadParseException {
    CategoryIndex.resetCounter();
    SemanticCategory.resetCounter();

    // Obama won
    SyntacticCategory synCat1 = SyntacticCategory.fromString("NP{_}");
    SyntacticCategory synCat2 = SyntacticCategory.fromString("(S[dcl]{_}\\NP{Y}){_};_ 1 Y");

    SemanticCategory semCat1 =
        SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.TYPE);
    SemanticCategory semCat2 =
        SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.EVENT);

    Category cat1 = new Category(synCat1, semCat1);
    Category cat2 = new Category(synCat2, semCat2);

    Category cat3 = Category.backwardApplication(cat1, cat2);

    assertEquals(cat3.getSyntacticCategory().toSuperSimpleString(), "S");

    System.out.println(cat3);

    Category typeRaisedCat = Category.typeRaising(cat1);
    Category cat4 = Category.forwardApplication(typeRaisedCat, cat2);
    assertEquals(cat4.getSyntacticCategory().toSuperSimpleString(), "S");

    System.out.println(cat4);

    Category typeRaisedCat2 = Category.typeRaising(cat2);
    Category cat5 = Category.backwardApplication(typeRaisedCat, typeRaisedCat2);
    assertEquals(cat5.getSyntacticCategory().toSuperSimpleString(), "S");

    System.out.println(cat5);
  }

  @Test
  public void testCoordinateConj() throws BadParseException {
    CategoryIndex.resetCounter();
    SemanticCategory.resetCounter();

    SyntacticCategory synCat1 = SyntacticCategory.fromString("NP{X}");
    SemanticCategory semCat1 =
        SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.TYPE);

    Category cat = new Category(synCat1, semCat1);


    Category outCat = Category.coordinationApplication(cat);
    assertEquals(outCat.getSyntacticCategory().toSuperSimpleString(), "(NP|NP)");

    synCat1 = SyntacticCategory.fromString("(S{_}\\NP{X}){_};_ 1 X");
    semCat1 = SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.EVENT);

    cat = new Category(synCat1, semCat1);
    outCat = Category.coordinationApplication(cat);
    assertEquals(outCat.getSyntacticCategory().toSuperSimpleString(), "((S\\NP)|(S\\NP))");
  }

  @Test
  public void testUnaryRule() throws BadParseException {
    CategoryIndex.resetCounter();
    SemanticCategory.resetCounter();

    SyntacticCategory synCat1 = SyntacticCategory.fromString("(S[pss]{_}\\NP{Y}){_};_ 1 Y");
    SemanticCategory semCat1 =
        SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.EVENT);

    String rule = "S[pss]\\NP{X}	NP{X}\\NP{X}";
    Category cat1 = new Category(synCat1, semCat1);
    Category cat2 = Category.applyUnaryRule(cat1, rule);
    assertEquals(cat2.getSyntacticCategory().toSuperSimpleString(), "(NP\\NP)");

    rule =
        "S[dcl]{X}	NP{Z}\\NP{Z}	(lambda $f1 $f2 $Z (exists $X (and (EVENT $X $X rel $Z) ($f1 $X) (($f2 $Z) $Z))))";

    synCat1 = SyntacticCategory.fromString("S[dcl]{_}");
    semCat1 = SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.TYPE);
    cat1 = new Category(synCat1, semCat1);
    cat2 = Category.applyUnaryRule(cat1, rule);
    assertEquals(cat2.getSyntacticCategory().toSuperSimpleString(), "(NP\\NP)");
    // verify the semantics manually
  }

  @Test
  public void testBinaryRule() throws BadParseException {

    CategoryIndex.resetCounter();
    SemanticCategory.resetCounter();

    String rule = "S[dcl]/S[dcl]{X}	,	(S{X}\\NP)\\(S{X}\\NP)";

    SyntacticCategory synCat1 = SyntacticCategory.fromString("(S[dcl]{_}/S[dcl]{B}){_};_ 1 B");
    SemanticCategory semCat1 =
        SemanticCategory.generateSemanticCategory(synCat1, SemanticCategoryType.EVENTMOD);

    SyntacticCategory synCat2 = SyntacticCategory.fromString(",");
    SemanticCategory semCat2 =
        SemanticCategory.generateSemanticCategory(synCat2, SemanticCategoryType.IDENTITY);

    Category cat1 = new Category(synCat1, semCat1);
    Category cat2 = new Category(synCat2, semCat2);
    Category cat = Category.applyBinaryRule(cat1, cat2, rule);

    assertEquals(cat.getSyntacticCategory().toSuperSimpleString(), "((S\\NP)\\(S\\NP))");
    // verify the semantics manually
  }
}
