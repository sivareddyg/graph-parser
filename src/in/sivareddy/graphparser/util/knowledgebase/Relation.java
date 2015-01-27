package in.sivareddy.graphparser.util.knowledgebase;

import java.util.Comparator;

/**
 * A relation contains two edges from a hypothetical node to entities in
 * relation. Relation also contains a weight describing how important the
 * relation is.
 *
 */
public class Relation implements Comparable<Relation> {
  private String leftEdge;
  private String rightEdge;
  private Double weight;

  public Double getWeight() {
    return weight;
  }

  public void setWeight(Double weight) {
    this.weight = weight;
  }

  public Relation(String leftEdge, String rightEdge, Double weight) {
    this.leftEdge = leftEdge;
    this.rightEdge = rightEdge;
    this.weight = weight;
  }

  public Relation(String leftEdge, String rightEdge) {
    this.leftEdge = leftEdge;
    this.rightEdge = rightEdge;
    this.weight = 0.0;
  }

  public static Relation of(String leftEdge, String rightEdge) {
    Relation relation = new Relation(leftEdge, rightEdge);
    return relation;
  }

  @Override
  public int compareTo(Relation other) {
    if (this.equals(other)) {
      return 0;
    }

    Double double1 = this.weight;
    Double double2 = other.weight;

    if (double1.compareTo(double2) != 0) {
      return -1 * double1.compareTo(double2);
    }

    /*-if (double1 - double2 > 0.00001)
      return -1;
    else if (double1 - double2 < -0.00001)
      return 1;*/

    if (!leftEdge.equals(other.leftEdge)) {
      return leftEdge.compareTo(other.leftEdge);
    }

    if (!rightEdge.equals(other.rightEdge)) {
      return rightEdge.compareTo(other.rightEdge);
    }

    return 0;

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = result * prime + leftEdge.hashCode();
    result = result * prime + rightEdge.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(this.getClass())) {
      return false;
    }
    Relation other = (Relation) obj;
    if (other.leftEdge.equals(leftEdge) && other.rightEdge.equals(rightEdge)) {
      return true;
    }
    return false;
  }

  public static class RelationComparator implements Comparator<Relation> {
    @Override
    public int compare(Relation o1, Relation o2) {
      return o1.compareTo(o2);
    }
  }

  @Override
  public String toString() {
    return "(" + leftEdge + "," + rightEdge + ")" + ":" + weight.toString();
  }

  public Relation inverse() {
    return new Relation(this.rightEdge, this.leftEdge, this.weight);
  }

  public String getRight() {
    return rightEdge;
  }

  public String getLeft() {
    return leftEdge;
  }

  public Relation copy() {
    return new Relation(leftEdge, rightEdge, weight);
  }
}
