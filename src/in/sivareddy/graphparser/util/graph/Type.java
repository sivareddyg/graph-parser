package in.sivareddy.graphparser.util.graph;

import in.sivareddy.graphparser.util.knowledgebase.EntityType;

import java.util.Comparator;

/**
 * Created by bisk1 on 1/26/15.
 */
public class Type<T> implements Comparable<Type<T>> {
  private EntityType entityType;
  T parentNode;
  T modifierNode;

  public EntityType getEntityType() {
    return entityType;
  }

  public T getParentNode() {
    return parentNode;
  }

  public T getModifierNode() {
    return modifierNode;
  }

  public Type(T parentNode, T modifierNode, EntityType nodeType) {
    this.entityType = nodeType;
    this.parentNode = parentNode;
    this.modifierNode = modifierNode;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    int parentNodeHash = parentNode.hashCode();
    int modifierNodeHash = modifierNode.hashCode();
    int entityTypeHash = entityType.hashCode();
    result = prime * result + parentNodeHash;
    result = prime * result + modifierNodeHash;
    result = prime * result + entityTypeHash;
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
    if (!obj.getClass().equals(getClass())) {
      return false;
    }
    Type<?> other = (Type<?>) obj;
    if (other.parentNode.equals(parentNode)
        && other.modifierNode.equals(modifierNode)
        && other.entityType.equals(entityType)) {
      return true;
    }
    return false;
  }

  @Override
  public int compareTo(Type<T> o) {
    if (this == o) {
      return 0;
    }
    int x = entityType.compareTo(o.entityType);
    if (x != 0) {
      return x;
    }
    if (!parentNode.equals(o.parentNode)) {
      return parentNode.hashCode() > o.parentNode.hashCode() ? 1 : -1;
    }
    if (!modifierNode.equals(o.modifierNode)) {
      return modifierNode.hashCode() > o.modifierNode.hashCode() ? 1 : -1;
    }
    return 0;
  }

  public static class PropertyComparator<T> implements Comparator<Type<T>> {
    @Override
    public int compare(Type<T> o1, Type<T> o2) {
      return o1.compareTo(o2);
    }
  }

  @Override
  public String toString() {
    return entityType + "\t" + parentNode.toString() + "\t" + modifierNode;
  }

}
