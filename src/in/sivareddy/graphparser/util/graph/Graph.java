package in.sivareddy.graphparser.util.graph;

import in.sivareddy.graphparser.util.knowledgebase.EntityType;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;

    for (Edge<T> edge : edges) {
      result += edge.hashCode();
    }
    result = prime * result;

    for (Type<T> type : types) {
      result += type.hashCode();
    }
    return result;
  }

  @Override
  public boolean equals(Object arg0) {
    if (arg0 == null)
      return false;
    return hashCode() == arg0.hashCode();
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
    Edge<T> edge = new Edge<>(node1, node2, mediator, relation);
    addEdge(edge);
  }

  public void addEdge(Edge<T> edge) {
    T node1 = edge.getLeft();
    T node2 = edge.getRight();
    T mediator = edge.getMediator();

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

    connectedNodes.add(edges.first().getLeft());
    nodesCovered.add(edges.first().getLeft());

    int i = 0;
    while (i < connectedNodes.size()) {
      if (connectedNodes.size() == nodes.size()) {
        return true;
      }

      T node = connectedNodes.get(i);
      if (nodeEdges.containsKey(node)) {
        for (Edge<T> edge : nodeEdges.get(node)) {
          T node1 = edge.getLeft();
          T node2 = edge.getRight();
          T mediator = edge.getMediator();
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

    connectedNodes.add(edges.first().getLeft());
    nodesCovered.add(edges.first().getLeft());

    int i = 0;
    while (i < connectedNodes.size()) {
      if (connectedNodes.size() == nodes.size()) {
        return connectedNodes.size();
      }

      T node = connectedNodes.get(i);
      if (nodeEdges.containsKey(node)) {
        for (Edge<T> edge : nodeEdges.get(node)) {
          T node1 = edge.getLeft();
          T node2 = edge.getRight();
          T mediator = edge.getMediator();
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

    connectedNodes.add(edges.first().getLeft());
    nodesCovered.add(edges.first().getLeft());

    int i = 0;
    while (i < connectedNodes.size()) {
      if (connectedNodes.size() == nodes.size()) {
        return connectedNodes;
      }

      T node = connectedNodes.get(i);
      if (nodeEdges.containsKey(node)) {
        for (Edge<T> edge : nodeEdges.get(node)) {
          T node1 = edge.getLeft();
          T node2 = edge.getRight();
          T mediator = edge.getMediator();
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

    return connectedNodes;
  }

  @Override
  public int compareTo(Graph<T> o) {
    return o.score.compareTo(score);
  }

  public Map<T, TreeSet<Edge<T>>> getNodeEdges() {
    return nodeEdges;
  }

  public Map<T, TreeSet<Type<T>>> getNodeTypes() {
    return nodeTypes;
  }

}
