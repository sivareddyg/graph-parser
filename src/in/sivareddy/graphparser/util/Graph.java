package in.sivareddy.graphparser.util;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import in.sivareddy.graphparser.util.KnowledgeBase.EntityType;
import in.sivareddy.graphparser.util.KnowledgeBase.Property;
import in.sivareddy.graphparser.util.KnowledgeBase.Relation;

public class Graph<T> implements Comparable<Graph<T>> {
  private List<T> actualNodes;

  private TreeSet<T> nodes;
  private TreeSet<Edge<T>> edges;
  private TreeSet<Type<T>> types;

  private Map<T, TreeSet<Edge<T>>> nodeEdges;
  private Map<T, TreeSet<Type<T>>> nodeTypes;

  private Map<T, TreeSet<Type<T>>> eventTypes;
  private Map<T, TreeSet<Type<T>>> eventEventModifiers;

  private Map<T, Set<Property>> nodeProperties;

  private Double score;

  public List<T> getActualNodes() {
    return actualNodes;
  }

  public void setActualNodes(List<T> actualNodes) {
    this.actualNodes = actualNodes;
  }

  public TreeSet<Edge<T>> getEdges() {
    return edges;
  }

  public TreeSet<Type<T>> getTypes() {
    return types;
  }

  public Set<T> getNodes() {
    return nodes;
  }

  public Double getScore() {
    return score;
  }

  public void setScore(Double score) {
    this.score = score;
  }

  public void addScore(Double increment) {
    this.score += increment;
  }

  public TreeSet<Edge<T>> getEdges(T node) {
    if (!nodeEdges.containsKey(node)) {
      return null;
    }

    return nodeEdges.get(node);
  }

  public TreeSet<Type<T>> getTypes(T node) {
    if (!nodeTypes.containsKey(node)) {
      return null;
    }
    return nodeTypes.get(node);
  }

  public Map<T, Set<Property>> getProperties() {
    return nodeProperties;
  }

  public void addProperties(Map<T, Set<Property>> nodeProperties) {
    this.nodeProperties = nodeProperties;
    for (T node : nodeProperties.keySet())
      this.nodes.add(node);
  }

  public Set<Property> getProperties(T node) {
    if (!nodeProperties.containsKey(node)) {
      return null;
    }
    return nodeProperties.get(node);
  }

  public TreeSet<Type<T>> getEventTypes(T node) {
    if (!eventTypes.containsKey(node)) {
      return null;
    }
    return eventTypes.get(node);
  }

  public TreeSet<Type<T>> getEventEventModifiers(T node) {
    if (!eventEventModifiers.containsKey(node)) {
      return null;
    }
    return eventEventModifiers.get(node);
  }

  public void addEventTypes(Map<T, TreeSet<Type<T>>> eventTypes) {
    this.eventTypes = eventTypes;
    for (TreeSet<Type<T>> typeSet : eventTypes.values()) {
      for (Type<T> type : typeSet) {
        nodes.add(type.getParentNode());
        nodes.add(type.getModifierNode());
      }
    }
  }

  public void addEventEventModifiers(
      Map<T, TreeSet<Type<T>>> eventEventModifiers) {
    this.eventEventModifiers = eventEventModifiers;
    for (TreeSet<Type<T>> typeSet : eventEventModifiers.values()) {
      for (Type<T> type : typeSet) {
        nodes.add(type.getParentNode());
        nodes.add(type.getModifierNode());
      }
    }
  }

  public Map<T, TreeSet<Type<T>>> getEventTypes() {
    return eventTypes;
  }

  public Map<T, TreeSet<Type<T>>> getEventEventModifiers() {
    return eventEventModifiers;
  }

  public Map<T, Set<Property>> getNodeProperties() {
    return nodeProperties;
  }

  public void addProperty(T node, Property property) {
    addNode(node);
    if (!nodeProperties.containsKey(node)) {
      nodeProperties.put(node, new HashSet<Property>());
    }
    nodeProperties.get(node).add(property);
  }

  @Override
  public String toString() {
    String graphString = "";
    for (T node : nodes) {
      graphString += node.toString() + "; ";
    }
    graphString += "\n";

    graphString += "Edges: ";
    for (Edge<T> edge : edges) {
      graphString += edge + "\n";
    }

    graphString += "Types: ";
    for (Type<T> type : types) {
      graphString += type + "\n";
    }

    graphString += "Properties: ";
    for (T node : nodes) {
      if (nodeProperties.containsKey(node)) {
        graphString += nodeProperties.get(node) + "\n";
      }
    }
    return graphString;
  }

  public static class Type<T> implements Comparable<Type<T>> {
    private EntityType entityType;
    private T parentNode;
    private T modifierNode;

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

  public static class Edge<T> implements Comparable<Edge<T>> {
    private T node1;
    private T node2;
    private T mediator;
    private Relation relation;

    public Edge(T node1, T node2, T mediator, Relation relation) {
      this.mediator = mediator;
      this.node1 = node1;
      this.node2 = node2;
      this.relation = relation;
    }

    public Edge<T> inverse() {
      return new Edge<T>(node2, node1, mediator, relation.inverse());
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

  public Graph() {
    score = 0.0;
    nodes = new TreeSet<>();
    edges = new TreeSet<>();
    types = new TreeSet<>();

    nodeEdges = Maps.newHashMap();
    nodeTypes = Maps.newHashMap();
    eventTypes = Maps.newHashMap();
    eventEventModifiers = Maps.newHashMap();

    nodeProperties = Maps.newHashMap();
  }

  public void copyTo(Graph<T> newGraph) {
    Preconditions.checkNotNull(newGraph);
    newGraph.score = new Double(score);
    newGraph.nodes = new TreeSet<>(nodes);
    newGraph.edges = new TreeSet<>(edges);
    newGraph.types = new TreeSet<>(types);


    // eventTypes and eventEventModifiers remain same in every graph since
    // these edges/types are not grounded
    newGraph.eventTypes = eventTypes;
    newGraph.eventEventModifiers = eventEventModifiers;
    newGraph.actualNodes = actualNodes;

    for (T node : nodeEdges.keySet()) {
      newGraph.nodeEdges.put(node, new TreeSet<>(nodeEdges.get(node)));
    }

    for (T node : nodeTypes.keySet()) {
      newGraph.nodeTypes.put(node, new TreeSet<>(nodeTypes.get(node)));
    }

    for (T node : nodeProperties.keySet()) {
      newGraph.nodeProperties
          .put(node, new HashSet<>(nodeProperties.get(node)));
    }
  }

  public void addNode(T node) {
    if (!nodeEdges.containsKey(node)) {
      nodes.add(node);
    }
  }

  public void addEdge(T node1, T node2, T mediator, Relation relation) {
    Edge<T> edge = new Edge<T>(node1, node2, mediator, relation);
    // System.out.println(edge.hashCode());

    if (edges.contains(edge)) {
      return;
    }
    addNode(node1);
    addNode(node2);
    addNode(mediator);
    edges.add(edge);
    if (!nodeEdges.containsKey(node1)) {
      nodeEdges.put(node1, new TreeSet<Edge<T>>());
    }
    if (!nodeEdges.containsKey(node2)) {
      nodeEdges.put(node2, new TreeSet<Edge<T>>());
    }
    nodeEdges.get(node1).add(edge);
    nodeEdges.get(node2).add(edge.inverse());
  }

  public void addType(T parentNode, T modifierNode, EntityType nodeType) {
    Type<T> type = new Type<>(parentNode, modifierNode, nodeType);
    addNode(parentNode);
    addNode(modifierNode);
    types.add(type);
    if (!nodeTypes.containsKey(parentNode)) {
      nodeTypes.put(parentNode, new TreeSet<Type<T>>());
    }
    nodeTypes.get(parentNode).add(type);
  }

  public void addEventType(T parentNode, T modifierNode, EntityType nodeType) {
    Type<T> type = new Type<>(parentNode, modifierNode, nodeType);
    addNode(parentNode);
    addNode(modifierNode);

    if (!eventTypes.containsKey(parentNode)) {
      eventTypes.put(parentNode, new TreeSet<Type<T>>());
    }
    eventTypes.get(parentNode).add(type);
  }

  public void addEventEventModifier(T parentNode, T modifierNode,
      EntityType nodeType) {
    Type<T> type = new Type<>(parentNode, modifierNode, nodeType);
    addNode(parentNode);
    addNode(modifierNode);

    if (!eventEventModifiers.containsKey(parentNode)) {
      eventEventModifiers.put(parentNode, new TreeSet<Type<T>>());
    }
    eventEventModifiers.get(parentNode).add(type);
  }

  public boolean isConnected() {
    List<T> connectedNodes = Lists.newArrayList();
    Set<T> nodesCovered = Sets.newHashSet();

    if (edges.size() == 0) {
      return false;
    }

    connectedNodes.add(edges.first().node1);
    nodesCovered.add(edges.first().node1);

    int i = 0;
    while (i < connectedNodes.size()) {
      if (connectedNodes.size() == nodes.size()) {
        return true;
      }

      T node = connectedNodes.get(i);
      if (nodeEdges.containsKey(node)) {
        for (Edge<T> edge : nodeEdges.get(node)) {
          T node1 = edge.node1;
          T node2 = edge.node2;
          T mediator = edge.mediator;
          if (!nodesCovered.contains(node1)) {
            connectedNodes.add(node1);
            nodesCovered.add(node1);
          }
          if (!nodesCovered.contains(node2)) {
            connectedNodes.add(node2);
            nodesCovered.add(node2);
          }
          if (!nodesCovered.contains(mediator)) {
            connectedNodes.add(mediator);
            nodesCovered.add(mediator);
          }
        }
      }

      if (nodeTypes.containsKey(node)) {
        for (Type<T> type : nodeTypes.get(node)) {
          T modifierNode = type.modifierNode;
          T parentNode = type.parentNode;
          if (!nodesCovered.contains(modifierNode)) {
            connectedNodes.add(modifierNode);
            nodesCovered.add(modifierNode);
          }

          if (!nodesCovered.contains(parentNode)) {
            connectedNodes.add(parentNode);
            nodesCovered.add(parentNode);
          }
        }
      }
      i++;

    }

    if (connectedNodes.size() == nodes.size()) {
      return true;
    }

    // adding all the event nodes modifying other event nodes since these
    // are not grounded
    for (T node : eventEventModifiers.keySet()) {
      for (Type<T> type : eventEventModifiers.get(node)) {
        T modifierNode = type.modifierNode;
        T parentNode = type.parentNode;
        if (!nodesCovered.contains(modifierNode)) {
          connectedNodes.add(modifierNode);
          nodesCovered.add(modifierNode);
        }

        if (!nodesCovered.contains(parentNode)) {
          connectedNodes.add(parentNode);
          nodesCovered.add(parentNode);
        }
      }
    }

    if (connectedNodes.size() == nodes.size()) {
      return true;
    }

    // adding all the types of events since these are not grounded
    for (T node : eventTypes.keySet()) {
      for (Type<T> type : eventTypes.get(node)) {
        T modifierNode = type.modifierNode;
        T parentNode = type.parentNode;
        if (!nodesCovered.contains(modifierNode)) {
          connectedNodes.add(modifierNode);
          nodesCovered.add(modifierNode);
        }

        if (!nodesCovered.contains(parentNode)) {
          connectedNodes.add(parentNode);
          nodesCovered.add(parentNode);
        }
      }
    }

    if (connectedNodes.size() == nodes.size()) {
      return true;
    }

    // adding all the nodes having the property COUNT
    for (T node : nodeProperties.keySet()) {
      Set<Property> properties = nodeProperties.get(node);
      for (Property property : properties) {
        if (property.getPropertyName().equals("COUNT")) {
          String arg = property.getArguments();
          int argIndex = Integer.parseInt(arg.split(":")[0]);
          T argNode = actualNodes.get(argIndex);
          if (!nodesCovered.contains(argNode)) {
            nodesCovered.add(argNode);
            connectedNodes.add(argNode);
          }
        }
      }
    }

    return (connectedNodes.size() == nodes.size());

  }

  public int mainNodesCount() {
    List<T> connectedNodes = Lists.newArrayList();
    Set<T> nodesCovered = Sets.newHashSet();

    if (edges.size() == 0) {
      return 0;
    }

    connectedNodes.add(edges.first().node1);
    nodesCovered.add(edges.first().node1);

    int i = 0;
    while (i < connectedNodes.size()) {
      if (connectedNodes.size() == nodes.size()) {
        return connectedNodes.size();
      }

      T node = connectedNodes.get(i);
      if (nodeEdges.containsKey(node)) {
        for (Edge<T> edge : nodeEdges.get(node)) {
          T node1 = edge.node1;
          T node2 = edge.node2;
          T mediator = edge.mediator;
          if (!nodesCovered.contains(node1)) {
            connectedNodes.add(node1);
            nodesCovered.add(node1);
          }
          if (!nodesCovered.contains(node2)) {
            connectedNodes.add(node2);
            nodesCovered.add(node2);
          }
          if (!nodesCovered.contains(mediator)) {
            connectedNodes.add(mediator);
            nodesCovered.add(mediator);
          }
        }
      }

      if (nodeTypes.containsKey(node)) {
        for (Type<T> type : nodeTypes.get(node)) {
          T modifierNode = type.modifierNode;
          T parentNode = type.parentNode;
          if (!nodesCovered.contains(modifierNode)) {
            connectedNodes.add(modifierNode);
            nodesCovered.add(modifierNode);
          }

          if (!nodesCovered.contains(parentNode)) {
            connectedNodes.add(parentNode);
            nodesCovered.add(parentNode);
          }
        }
      }
      i++;

    }

    if (connectedNodes.size() == nodes.size()) {
      return connectedNodes.size();
    }

    // adding all the event nodes modifying other event nodes since these
    // are not grounded
    for (T node : eventEventModifiers.keySet()) {
      for (Type<T> type : eventEventModifiers.get(node)) {
        T modifierNode = type.modifierNode;
        T parentNode = type.parentNode;
        if (!nodesCovered.contains(modifierNode)) {
          connectedNodes.add(modifierNode);
          nodesCovered.add(modifierNode);
        }

        if (!nodesCovered.contains(parentNode)) {
          connectedNodes.add(parentNode);
          nodesCovered.add(parentNode);
        }
      }
    }

    if (connectedNodes.size() == nodes.size()) {
      return connectedNodes.size();
    }

    // adding all the nodes having the property COUNT
    for (T node : nodeProperties.keySet()) {
      Set<Property> properties = nodeProperties.get(node);
      for (Property property : properties) {
        if (property.getPropertyName().equals("COUNT")) {
          String arg = property.getArguments();
          int argIndex = Integer.parseInt(arg.split(":")[0]);
          T argNode = actualNodes.get(argIndex);
          if (!nodesCovered.contains(argNode)) {
            nodesCovered.add(argNode);
            connectedNodes.add(argNode);
          }
        }
      }
    }

    return connectedNodes.size();

  }

  public List<T> getNodesConnectedByEdges() {
    List<T> connectedNodes = Lists.newArrayList();
    Set<T> nodesCovered = Sets.newHashSet();

    if (edges.size() == 0) {
      return connectedNodes;
    }

    connectedNodes.add(edges.first().node1);
    nodesCovered.add(edges.first().node1);

    int i = 0;
    while (i < connectedNodes.size()) {
      if (connectedNodes.size() == nodes.size()) {
        return connectedNodes;
      }

      T node = connectedNodes.get(i);
      if (nodeEdges.containsKey(node)) {
        for (Edge<T> edge : nodeEdges.get(node)) {
          T node1 = edge.node1;
          T node2 = edge.node2;
          T mediator = edge.mediator;
          if (!nodesCovered.contains(node1)) {
            connectedNodes.add(node1);
            nodesCovered.add(node1);
          }
          if (!nodesCovered.contains(node2)) {
            connectedNodes.add(node2);
            nodesCovered.add(node2);
          }
          if (!nodesCovered.contains(mediator)) {
            connectedNodes.add(mediator);
            nodesCovered.add(mediator);
          }
        }
      }
      i++;

    }

    if (connectedNodes.size() == nodes.size()) {
      return connectedNodes;
    }

    /*- Commenting out since counting creates one extra node with no feature added. But the other parse will have a feature. So better not count numerical nodes
    // adding all the nodes having the property COUNT
    for (T node : nodeProperties.keySet()) {
    	Set<Property> properties = nodeProperties.get(node);
    	for (Property property : properties) {
    		if (property.getPropertyName().equals("COUNT")) {
    			String arg = property.getArguments();
    			int argIndex = Integer.parseInt(arg.split(":")[0]);
    			T argNode = actualNodes.get(argIndex);
    			if (!nodesCovered.contains(argNode)) {
    				nodesCovered.add(argNode);
    				connectedNodes.add(argNode);
    			}
    		}
    	}
    }*/

    return connectedNodes;

  }



  @Override
  public int compareTo(Graph<T> o) {
    return o.score.compareTo(score);
  }

}
