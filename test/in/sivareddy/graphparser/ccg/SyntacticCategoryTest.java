package in.sivareddy.graphparser.ccg;

import in.sivareddy.graphparser.ccg.SyntacticCategory.IndexedDependency;
import in.sivareddy.util.StringUtils;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Set;

/**
 * Test units for Syntactic Category functions.
 *
 * Some of the test cases are written in CategoryTest.java
 *
 * @author Siva Reddy
 *
 */
public class SyntacticCategoryTest extends TestCase {



  @Test
  public void testFromString() {
    CategoryIndex.resetCounter();
    SyntacticCategory cat = SyntacticCategory.fromString("S[feat]{0}");
    assertEquals(cat.toString(), "S[feat]{0=?}");
    cat = SyntacticCategory.fromString("S");
    assertEquals(cat.toString(), "S{X2=?}");

    try {
      cat = SyntacticCategory.fromString("(S)");
      // Basic categories do not have braces around them
      fail("Did not throw error: should throw error since the category is invalid");
    } catch (Exception IllegalArgumentException) {
      // Do nothing.
    }

    cat = SyntacticCategory.fromString("NP{0}");
    assertEquals(cat.toString(), "NP{0=?}");

    cat = SyntacticCategory.fromString("(S\\NP)");
    assertEquals(cat.toString(), "(S{X6=?}\\NP{X7=?}){X5=?}");

    cat =
        SyntacticCategory
            .fromString("((S\\NP)/NP[sing]{X})/((S\\NP[plu])/NP{X})");
    assertEquals(
        cat.toString(),
        "(((S{X11=?}\\NP{X12=?}){X10=?}/NP[sing]{X=?}){X9=?}/((S{X16=?}\\NP[plu]{X17=?}){X15=?}/NP{X=?}){X14=?}){X8=?}");

    assertEquals(cat.toSimpleString(), "(((S\\NP)/NP[sing])/((S\\NP[plu])/NP))");

    cat =
        SyntacticCategory
            .fromString("((S[dcl]{_}\\NP{X}){_}/NP{Y}){_};_ subj X, _ obj Y,X dummy Y");
    Set<IndexedDependency> deps = cat.getDependencies();
    assertEquals(deps.size(), 3);

    cat =
        SyntacticCategory
            .fromString("((((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}\\((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}){V}/(((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}\\((S[X]{Y}\\NP{Z}){Y}\\(S[X]{Y}\\NP{Z}){Y}){W}){V}){_}");

  }

  @Test
  public void testUnification() {
    CategoryIndex x = new CategoryIndex("X");
    CategoryIndex y = new CategoryIndex("Y");
    CategoryIndex z = new CategoryIndex("Z");
    CategoryIndex w = new CategoryIndex("W");
    CategoryIndex q = new CategoryIndex("Q");

    x.unify(y);
    w.unify(z);
    y.unify(z);

    w.setVariableValue(2);

    assertEquals(x.getVariableValue().getValue(), Integer.valueOf(2));
    try {
      x.setVariableValue(3);
      fail("Should not change a variable value which is already initialised");
    } catch (Exception IllegalArgumentException) {
      // Test passed
    }

    // x and z are unified variables
    assertEquals(x.equals(w), true);
    assertEquals(x.equals(q), false);
  }

  @Test
  public void removeExtraBrackets() {
    String inputString = "(((S\\NP))/(NP))";

    String expected = StringUtils.removeExtraBrackets(inputString);
    String actual = "((S\\NP)/(NP))";
    assertEquals(expected, actual);

    inputString = "(((NP)))";
    expected = StringUtils.removeExtraBrackets(inputString);
    actual = "(NP)";
    assertEquals(expected, actual);

    inputString = "(((NP/((NP))))/((NP)))";
    expected = StringUtils.removeExtraBrackets(inputString);
    actual = "((NP/(NP))/(NP))";
    assertEquals(expected, actual);
  }
}
