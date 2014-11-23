package in.sivareddy.util;

/**
 *
 * IntegerObject, an object for Integer.
 *
 * @author Siva Reddy
 *
 */
public class IntegerObject {
  private int value;
  private boolean hasValue;

  public IntegerObject(int value) {
    this.value = value;
    hasValue = true;
  }

  public IntegerObject() {
    hasValue = false;
  }

  public Integer getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
    hasValue = true;
  }

  public boolean isInitialised() {
    return hasValue;
  }

  @Override
  public String toString() {
    return hasValue ? String.valueOf(value) : "?";
  }

}
