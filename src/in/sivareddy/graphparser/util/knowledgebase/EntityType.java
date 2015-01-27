package in.sivareddy.graphparser.util.knowledgebase;

import java.util.Comparator;

/**
 *
 * Entity Type contains the type of the entity, and its weight describing how
 * important the weight is
 *
 * @author Siva Reddy
 *
 */
public class EntityType implements Comparable<EntityType> {
  private String type;
  private Double weight;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Double getWeight() {
    return weight;
  }

  public void setWeight(Double weight) {
    this.weight = weight;
  }

  public EntityType(String type) {
    this.type = type;
    this.weight = 0.0;
  }

  public EntityType(String type, Double weight) {
    this.type = type;
    this.weight = weight;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
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
    EntityType other = (EntityType) obj;
    return type.equals(other.type);
  }

  public static class EntityTypeComparator implements Comparator<EntityType> {
    @Override
    public int compare(EntityType o1, EntityType o2) {
      return o1.compareTo(o2);
    }
  }

  @Override
  public String toString() {
    return type + ":" + weight.toString();
  }

  @Override
  public int compareTo(EntityType arg0) {
    EntityType o1 = this;
    EntityType o2 = arg0;
    // if (o1.equals(o2))
    // return 0;

    Double double1 = o1.weight;
    Double double2 = o2.weight;

    if (double1.compareTo(double2) != 0) {
      return -1 * double1.compareTo(double2);
    }
    /*-if (double1 - double2 > 0.00001)
      return -1;
    else if (double1 - double2 < -0.00001)
      return 1;*/

    return type.compareTo(arg0.type);
  }

  public EntityType copy() {
    return new EntityType(type, weight);
  }
}
