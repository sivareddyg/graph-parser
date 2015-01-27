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
    assertEquals(semCat.toString(), "(lambda $X:0 (TYPE $X:0 $X:0))");
    // System.out.println(semCat);

    synCat = SyntacticCategory.fromString("S");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.TYPE);
    assertEquals(semCat.toString(), "(lambda $X2:1 (TYPE $X2:1 $X2:1))");
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
        "(lambda $f0 $Y:6 (exists $_:5 (and ($f0 $Y:6) (TYPEMOD $Y:6 $_:5 1 $Y:6))))");

    // EVENT
    // syntactic category for told
    synCat =
        SyntacticCategory
            .fromString("(((S[dcl]{_}\\NP{Y}){_}/(S[to]{Z}\\NP{W}){Z}){_}/NP{W}){_};_ 3 W,_ 2 Z,_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.EVENT);
    assertEquals(
        semCat.toString(),
        "(lambda $f1 $f2 $f3 $_:7 (exists $Y:8 $Z:9 $W:10 (and ($f1 $W:10) (($f2 $W:10) $Z:9) ($f3 $Y:8) (EVENT $_:7 $_:7 1 $Y:8) (EVENT $_:7 $_:7 2 $Z:9) (EVENT $_:7 $_:7 3 $W:10))))");

    // EVENTMOD
    // syntactic category for annually
    synCat =
        SyntacticCategory
            .fromString("((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){_};_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.EVENTMOD);
    assertEquals(
        semCat.toString(),
        "(lambda $f4 $f5 $Y:12 (exists $_:11 $Z:13 (and (($f4 $Z:13) $Y:12) ($f5 $Z:13) (EVENTMOD $Y:12 $_:11 1))))");

    // CLOSED class
    // syntactic category for that
    synCat =
        SyntacticCategory
            .fromString("((NP{Y}\\NP{Y}){_}/(S[dcl]{Z}\\NP{Y}){Z}){_};_ 2 Z,_ 1 Y");
    semCat =
        SemanticCategory.generateSemanticCategory(synCat,
            SemanticCategoryType.CLOSED);
    assertEquals(semCat.toString(),
        "(lambda $f6 $f7 $Y:15 (exists $Z:16 (and (($f6 $Y:15) $Z:16) ($f7 $Y:15))))");

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
    assertEquals(bodyExpression.toString(), "((($f0 EMPTY) $Z:2) $Y:1)");
  }

}
