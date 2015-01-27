package in.sivareddy.graphparser.util.graph;

import in.sivareddy.graphparser.util.knowledgebase.Relation;

import java.util.Comparator;

/**
 * Created by bisk1 on 1/26/15.
 */
public class Edge<T> implements Comparable<Edge<T>> {
  T node1;
  T node2;
  T mediator;
  private Relation relation;

  public Edge(T node1, T node2, T mediator, Relation relation) {
    this.mediator = mediator;
    this.node1 = node1;
    this.node2 = node2;
    this.relation = relation;
  }

  public Edge<T> inverse() {
    return new Edge<>(node2, node1, mediator, relation.inverse());
  }

  public T getLeft() {
    return node1;
  }

  public T getRight() {
    return node2;
  }

  public T getMediator() {
    return mediator;
  }

  public Relation getRelation() {
    return relation;
  }

  @Override
  public int hashCode() {
    T mediator = this.mediator;
    T node1;
    T node2;
    Relation relation;
    if (this.relation.getLeft().compareTo(this.relation.getRight()) < 0) {
      node1 = this.node1;
      node2 = this.node2;
      relation = this.relation;
    } else {
      node1 = this.node2;
      node2 = this.node1;
      relation = this.relation.inverse();
    }

    final int prime = 31;
    int result = 1;
    int node1Hash = node1.hashCode();
    int node2Hash = node2.hashCode();
    int mediatorHash = mediator.hashCode();

    int relationHash = relation.hashCode();
    result = prime * result + node1Hash;
    result = prime * result + node2Hash;
    result = prime * result + mediatorHash;
    result = prime * result + relationHash;

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
    Edge<?> other = (Edge<?>) obj;
    if (other.mediator.equals(mediator) && other.node1.equals(node1)
        && other.node2.equals(node2) && other.relation.equals(relation)) {
      return true;
    }
    if (other.mediator.equals(mediator) && other.node1.equals(node2)
        && other.node2.equals(node1)
        && other.relation.equals(relation.inverse())) {
      return true;
    }
    return false;
  }

  @Override
  public int compareTo(Edge<T> o) {
    if (this.equals(o)) {
      return 0;
    }
    int returnValue = relation.compareTo(o.relation);
    if (returnValue == 0 && !mediator.equals(o.mediator)) {
      return mediator.hashCode() > o.mediator.hashCode() ? 1 : -1;
    }
    if (returnValue == 0 && !node1.equals(o.node1)) {
      return node1.hashCode() > o.node1.hashCode() ? 1 : -1;
    }
    if (returnValue == 0 && !node2.equals(o.node2)) {
      return node2.hashCode() > o.node2.hashCode() ? 1 : -1;
    }
    return returnValue;
  }

  public static class EdgeComparator<T> implements Comparator<Edge<T>> {
    @Override
    public int compare(Edge<T> o1, Edge<T> o2) {
      return o1.compareTo(o2);
    }
  }

  @Override
  public String toString() {
    return relation + "\t" + node1.toString() + "\t" + node2.toString();
  }

}
