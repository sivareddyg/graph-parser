package in.sivareddy.graphparser.util.knowledgebase;

import java.io.Serializable;

/**
 *
 * {@link Property} describe properties of
 * {@link in.sivareddy.graphparser.util.knowledgebase.EntityType}s or
 * {@link in.sivareddy.graphparser.util.knowledgebase.Relation}s.
 *
 * @author Siva Reddy
 *
 */
public class Property implements Serializable {
  private static final long serialVersionUID = -9033237257804335644L;
  private String propertyName;
  private String arguments;

  public String getPropertyName() {
    return propertyName;
  }

  public String getArguments() {
    return arguments;
  }

  public Property(String propertyName) {
    this.propertyName = propertyName;
  }

  public Property(String propertyName, String args) {
    this.propertyName = propertyName;
    this.arguments = args;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(getClass())) {
      return false;
    }
    Property other = (Property) obj;
    if (other.propertyName.equals(propertyName) && arguments == null
        && other.arguments == null) {
      return true;
    }
    if (other.propertyName.equals(propertyName) && arguments != null
        && arguments.equals(other.arguments)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = result * prime + propertyName.hashCode();
    result = result * prime + (arguments == null ? 0 : arguments.hashCode());
    return result;
  }

  @Override
  public String toString() {
    String propertyString = "";
    propertyString += propertyName;
    if (arguments != null) {
      propertyString += ", " + arguments;
    }
    return propertyString;
  }
}
