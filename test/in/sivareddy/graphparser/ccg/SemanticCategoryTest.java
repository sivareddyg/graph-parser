package in.sivareddy.graphparser.ccg;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import in.sivareddy.util.StringObject;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SemanticCategoryTest {

  @Test
  public void testGenerateExpression() {

    CategoryIndex.resetCounter();
    SemanticCategory.resetCounter();

    // TYPES
    SyntacticCategory synCat = SyntacticCategory.fromString("NP{X}");
    SemanticCategory semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.TYPE);
    assertEquals("(lambda $X:1 (TYPE $X:1 $X:1))", semCat.toString());
    // System.out.println(semCat);

    synCat = SyntacticCategory.fromString("S");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.TYPE);
    assertEquals("(lambda $X2:2 (TYPE $X2:2 $X2:2))", semCat.toString());
    // System.out.println(semCat);

    try {
      synCat = SyntacticCategory.fromString("S\\S");
      semCat =
          SemanticCategory.generateSemanticCategory(synCat,
              SemanticCategoryType.TYPE);
      fail("Should not give a type since category is not basic");
    } catch (Exception e) {
      // nothing
    }

    // TYPEMOD
    synCat = SyntacticCategory.fromString("(NP[nb]{Y}/N{Y}){_};_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.TYPEMOD);
    assertEquals(semCat.toString(),
        "(lambda $f0 $Y:7 (exists $_:6 (and ($f0 $Y:7) (TYPEMOD $Y:7 $_:6 1 $Y:7))))");

    // EVENT
    // syntactic category for told
    synCat =
        SyntacticCategory
            .fromString("(((S[dcl]{_}\\NP{Y}){_}/(S[to]{Z}\\NP{W}){Z}){_}/NP{W}){_};_ 3 W,_ 2 Z,_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.EVENT);
    assertEquals(
        "(lambda $f1 $f2 $f3 $_:8 (exists $W:11 $Y:9 $Z:10 (and ($f1 $W:11) (($f2 $W:11) $Z:10) ($f3 $Y:9) (EVENT $_:8 $_:8 1 $Y:9 ENTITY) (EVENT $_:8 $_:8 2 $Z:10 EVENT) (EVENT $_:8 $_:8 3 $W:11 ENTITY))))",
        semCat.toString());

    // EVENTMOD
    // syntactic category for annually
    synCat =
        SyntacticCategory
            .fromString("((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){_};_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.EVENTMOD);
    assertEquals(
        "(lambda $f4 $f5 $Y:13 (exists $Z:14 $_:12 (and (($f4 $Z:14) $Y:13) ($f5 $Z:14) (EVENTMOD $Y:13 $_:12 1))))",
        semCat.toString());

    // CLOSED class
    // syntactic category for that
    synCat =
        SyntacticCategory
            .fromString("((NP{Y}\\NP{Y}){_}/(S[dcl]{Z}\\NP{Y}){Z}){_};_ 2 Z,_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.CLOSED);
    assertEquals(
        "(lambda $f6 $f7 $Y:16 (exists $Z:17 (and (($f6 $Y:16) $Z:17) ($f7 $Y:16))))",
        semCat.toString());

  }

  @Test
  public void testGetDeepExpression() {
    CategoryIndex.resetCounter();
    SemanticCategory.resetCounter();
    SyntacticCategory synCat =
        SyntacticCategory
            .fromString("((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}");
    StringObject bodyExpression = new StringObject();
    List<String> lambdas = Lists.newArrayList();
    Set<String> exists = Sets.newHashSet();
    SemanticCategory.getDeepExpression(synCat, lambdas, exists, bodyExpression);
    assertEquals(lambdas.size(), 1);
    assertEquals(exists.size(), 2);
    assertEquals("((($f0 EMPTY) $Z:3) $Y:2)", bodyExpression.toString());
  }

}
