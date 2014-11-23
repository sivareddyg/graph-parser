package in.sivareddy.util;

public class StringObject {

  private String string;

  public StringObject(String string) {
    this.string = string;
  }

  public StringObject() {
    this.string = "";
  }

  public String getString() {
    return string;
  }

  public void setString(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return string;
  }
}
