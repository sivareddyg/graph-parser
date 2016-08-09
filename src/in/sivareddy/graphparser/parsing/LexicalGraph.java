package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.ccg.SemanticCategoryType;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Graph;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.ml.basic.AbstractFeature;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.util.SentenceKeys;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class LexicalGraph extends Graph<LexicalItem> {
  private static final long serialVersionUID = -4595359430330862744L;
  private final List<Feature> features;
  private String syntacticParse;
  private Set<String> semanticParse;
  private final Map<LexicalItem, LexicalItem> unifiedNodes;
  private int mergeCount = 0;

  public void increaseMergeCount() {
    mergeCount++;
  }

  public int getMergeCount() {
    return mergeCount;
  }

  private Map<Edge<LexicalItem>, Pair<Edge<LexicalItem>, Edge<LexicalItem>>> groundedToUngrounded =
      null;

  // Useful to store ungrounded graph associated with the grounded graph.
  private LexicalGraph parallelGraph = null;

  public LexicalGraph() {
    super();
    features = new ArrayList<>();
    unifiedNodes = new HashMap<>();
  }

  public LexicalItem getUnifiedNode(LexicalItem node) {
    return unifiedNodes.getOrDefault(node, node);
  }

  public void unifyNodes(LexicalItem parentNode, LexicalItem childNode) {
    if (parentNode.equals(childNode))
      return;

    for (Entry<LexicalItem, LexicalItem> entry : unifiedNodes.entrySet()) {
      if (entry.getKey().equals(childNode)
          || entry.getValue().equals(childNode)) {
        entry.setValue(parentNode);
      }
    }

    unifiedNodes.put(childNode, parentNode);
  }

  public List<Feature> getFeatures() {
    return features;
  }

  public static class MergedEdgeFeature extends AbstractFeature {
    private static final long serialVersionUID = 4867048253442317385L;

    public MergedEdgeFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class HasQuestionEntityEdgeFeature extends AbstractFeature {
    private static final long serialVersionUID = -6946577834582002816L;
    private static List<String> key = Lists
        .newArrayList("HasQuestionEntityEdge");

    public HasQuestionEntityEdgeFeature(boolean flag) {
      super(key, flag ? 1.0 : 0.0);
    }
  }

  public static class UrelGrelFeature extends AbstractFeature {
    private static final long serialVersionUID = -3957627404087338155L;

    public UrelGrelFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class UrelPartGrelPartFeature extends AbstractFeature {
    private static final long serialVersionUID = -318008383692094097L;

    public UrelPartGrelPartFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class UtypeGtypeFeature extends AbstractFeature {
    private static final long serialVersionUID = -4366189677468391943L;

    public UtypeGtypeFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class GrelGrelFeature extends AbstractFeature {
    private static final long serialVersionUID = 8038517923152193214L;

    public GrelGrelFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class GtypeGrelPartFeature extends AbstractFeature {
    private static final long serialVersionUID = 678521218675753759L;

    public GtypeGrelPartFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class WordGrelPartFeature extends AbstractFeature {
    private static final long serialVersionUID = 7030473228708723413L;

    public WordGrelPartFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class NgramGrelFeature extends AbstractFeature {
    private static final long serialVersionUID = 3250492736860001834L;

    public NgramGrelFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class WordGrelFeature extends AbstractFeature {
    private static final long serialVersionUID = 6481934970173246526L;

    public WordGrelFeature(List<?> key, Double value) {
      super(key, value);
    }
  }


  public static class ArgGrelPartFeature extends AbstractFeature {
    private static final long serialVersionUID = -195841457238867772L;

    public ArgGrelPartFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class ArgGrelFeature extends AbstractFeature {
    private static final long serialVersionUID = 586114137963669327L;

    public ArgGrelFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class EventTypeGrelPartFeature extends AbstractFeature {
    private static final long serialVersionUID = 7844786020868030663L;

    public EventTypeGrelPartFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class WordTypeFeature extends AbstractFeature {
    private static final long serialVersionUID = -508228066503934930L;

    public WordTypeFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class WordBIgramTypeFeature extends AbstractFeature {
    private static final long serialVersionUID = -8391768811682370426L;

    public WordBIgramTypeFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class ValidQueryFeature extends AbstractFeature {
    private static final long serialVersionUID = -2975306984616977348L;
    private static List<String> key = Lists.newArrayList("ValidQuery");

    public ValidQueryFeature(boolean flag) {
      super(key, flag ? 1.0 : 0.0);
    }
  }

  public static class AnswerTypeQuestionWordFeature extends AbstractFeature {
    private static final long serialVersionUID = 5997194236163353975L;

    public AnswerTypeQuestionWordFeature(List<?> key, Double value) {
      super(key, value);
    }
  }

  public static class GraphIsConnectedFeature extends AbstractFeature {
    private static final long serialVersionUID = 6624420194050239749L;
    private static List<String> key = Lists.newArrayList("GraphIsConnected");

    public GraphIsConnectedFeature(boolean flag) {
      super(key, flag ? 1.0 : 0.0);
    }
  }

  public static class GraphHasEdgeFeature extends AbstractFeature {
    private static final long serialVersionUID = 8641425379382235881L;
    private static List<String> key = Lists.newArrayList("GraphHasEdge");

    public GraphHasEdgeFeature(boolean flag) {
      super(key, flag ? 1.0 : 0.0);
    }
  }

  public static class GraphNodeCountFeature extends AbstractFeature {
    private static final long serialVersionUID = 3145845168997429173L;
    private static List<String> key = Lists.newArrayList("GraphNodeCount");

    public GraphNodeCountFeature(double value) {
      super(key, value);
    }
  }

  public static class EdgeNodeCountFeature extends AbstractFeature {
    private static final long serialVersionUID = -4032692371636714168L;
    private static List<String> key = Lists.newArrayList("EdgeNodeCount");

    public EdgeNodeCountFeature(double value) {
      super(key, value);
    }
  }

  public static class DuplicateEdgeFeature extends AbstractFeature {
    private static final long serialVersionUID = -1582319355989951987L;
    private static List<String> key = Lists.newArrayList("DuplicateEdgeCount");

    public DuplicateEdgeFeature(Double value) {
      super(key, value);
    }
  }

  public static class EntityScoreFeature extends AbstractFeature {
    private static final long serialVersionUID = -2582000343152762524L;
    private static List<String> key = Lists.newArrayList("EntityScoreFeature");

    public EntityScoreFeature(Double value) {
      super(key, value);
    }
  }

  public static class EntityWordOverlapFeature extends AbstractFeature {
    private static final long serialVersionUID = 7095420691240643004L;
    private static List<String> key = Lists
        .newArrayList("EntityWordOverlapFeature");

    public EntityWordOverlapFeature(Double value) {
      super(key, value);
    }
  }

  public static class ParaphraseScoreFeature extends AbstractFeature {
    private static final long serialVersionUID = 243665475069611504L;
    private static List<String> key = Lists
        .newArrayList("ParaphraseScoreFeature");

    public ParaphraseScoreFeature(Double value) {
      super(key, value);
    }
  }

  public static class ParaphraseClassifierScoreFeature extends AbstractFeature {
    private static final long serialVersionUID = -2627112432390822195L;
    private static List<String> key = Lists
        .newArrayList("ParaphraseClassifierScoreFeature");

    public ParaphraseClassifierScoreFeature(Double value) {
      super(key, value);
    }
  }

  public static class StemMatchingFeature extends AbstractFeature {
    private static final long serialVersionUID = 8298292895790279274L;
    private static List<String> key = Lists.newArrayList("Stem");

    public StemMatchingFeature(Double value) {
      super(key, value);
    }
  }

  public static class MediatorStemGrelPartMatchingFeature extends
      AbstractFeature {
    private static final long serialVersionUID = -5844790517734058452L;
    private static List<String> key = Lists
        .newArrayList("MediatorStemGrelPart");

    public MediatorStemGrelPartMatchingFeature(Double value) {
      super(key, value);
    }
  }

  public static class ArgStemMatchingFeature extends AbstractFeature {
    private static final long serialVersionUID = 5445137371954280470L;
    private static List<String> key = Lists.newArrayList("ArgStem");

    public ArgStemMatchingFeature(Double value) {
      super(key, value);
    }
  }

  public static class ArgStemGrelPartMatchingFeature extends AbstractFeature {
    private static final long serialVersionUID = -2146659098058016070L;
    private static List<String> key = Lists.newArrayList("ArgStemGrelPart");

    public ArgStemGrelPartMatchingFeature(Double value) {
      super(key, value);
    }
  }

  public void addFeature(Feature feature) {
    features.add(feature);
  }

  public LexicalGraph copy() {
    LexicalGraph newGraph = new LexicalGraph();

    if (getParallelGraph() != null) {
      newGraph.setParallelGraph(getParallelGraph().copy());
    }

    if (groundedToUngrounded != null) {
      newGraph.groundedToUngrounded = new HashMap<>(groundedToUngrounded);
    }

    newGraph.unifiedNodes.putAll(unifiedNodes);
    copyTo(newGraph);

    newGraph.features.addAll(features);

    newGraph.mergeCount = mergeCount;
    newGraph.syntacticParse = syntacticParse;
    newGraph.semanticParse = semanticParse;

    newGraph.setScore(new Double(getScore()));
    return newGraph;
  }

  public String getSyntacticParse() {
    return syntacticParse;
  }

  public void setSyntacticParse(String syntacticParse) {
    this.syntacticParse = syntacticParse;
  }

  public Set<String> getSemanticParse() {
    return semanticParse;
  }

  public void setSemanticParse(Set<String> semanticParse) {
    this.semanticParse = semanticParse;
  }

  @Override
  public String toString() {
    StringBuilder graphString = new StringBuilder();
    graphString.append("Score: " + this.getScore() + '\n');
    if (syntacticParse != null) {
      graphString.append("SynParse: ");
      graphString.append(this.getSyntacticParse());
      graphString.append("\n");
    }

    if (semanticParse != null) {
      graphString.append("Semantic Parse: ");
      graphString.append(this.getSemanticParse());
      graphString.append("\n");
    }

    graphString.append("Words: \n");
    for (LexicalItem node : super.getNodes()) {
      graphString.append(Objects.toStringHelper(node)
          .addValue(node.getWordPosition()).addValue(node.getWord())
          .addValue(node.getLemma()).addValue(node.getPos())
          .addValue(node.getCategory()).toString()
          + "\n");
    }

    graphString.append("Edges: \n");
    for (Edge<LexicalItem> edge : super.getEdges()) {
      graphString.append("(" + edge.getMediator().getWordPosition() + ","
          + edge.getLeft().getWordPosition() + ","
          + edge.getRight().getWordPosition() + ")" + "\t" + edge.getRelation()
          + '\n');
    }

    graphString.append("Types: \n");
    for (Type<LexicalItem> type : super.getTypes()) {
      graphString.append("(" + type.getParentNode().getWordPosition() + ","
          + type.getModifierNode().getWordPosition() + ")" + "\t"
          + type.getEntityType() + '\n');
    }

    graphString.append("Properties: \n");
    Map<LexicalItem, Set<Property>> nodeProperties = super.getProperties();
    for (LexicalItem node : nodeProperties.keySet()) {
      graphString.append(node.getWordPosition() + "\t"
          + nodeProperties.get(node) + '\n');
    }

    graphString.append("EventTypes: \n");
    Map<LexicalItem, TreeSet<Type<LexicalItem>>> eventTypes =
        super.getEventTypes();
    for (LexicalItem node : eventTypes.keySet()) {
      for (Type<LexicalItem> type : eventTypes.get(node))
        graphString.append("(" + type.getParentNode().getWordPosition() + ","
            + type.getModifierNode().getWordPosition() + ")" + "\t"
            + type.getEntityType() + '\n');
    }

    graphString.append("EventEventModifiers: \n");
    Map<LexicalItem, TreeSet<Type<LexicalItem>>> eventEventModifiers =
        super.getEventEventModifiers();
    for (LexicalItem node : eventEventModifiers.keySet()) {
      for (Type<LexicalItem> type : eventEventModifiers.get(node))
        graphString.append("(" + type.getParentNode().getWordPosition() + ","
            + type.getModifierNode().getWordPosition() + ")" + "\t"
            + type.getEntityType() + '\n');
    }

    return graphString.toString();
  }

  public LexicalGraph getParallelGraph() {
    return parallelGraph;
  }

  public void setParallelGraph(LexicalGraph parallelGraph) {
    this.parallelGraph = parallelGraph.copy();
  }

  public void updateMergedReferences(LexicalItem parentNode,
      LexicalItem childNode) {
    if (parallelGraph != null)
      parallelGraph.updateMergedReferences(parentNode, childNode);

    Map<LexicalItem, Set<Property>> properties = this.getNodeProperties();
    if (properties.containsKey(childNode)) {
      Set<Property> childProperties = properties.get(childNode);

      properties.putIfAbsent(parentNode, new HashSet<>());
      properties.get(parentNode).addAll(childProperties);
      properties.remove(childNode);
    }

    TreeSet<Edge<LexicalItem>> childEdges = this.getEdges(childNode);
    HashSet<Edge<LexicalItem>> parentEdges =
        this.getEdges(parentNode) != null ? new HashSet<>(
            this.getEdges(parentNode)) : new HashSet<>();
    if (childEdges != null && childEdges.size() > 0) {
      childEdges = new TreeSet<>(childEdges); // avoids loop and deletion clash
      for (Edge<LexicalItem> edge : childEdges) {
        if (!edge.getRight().equals(parentNode)) {
          // An edge should not exist between the new node and the parent node.
          HashSet<Edge<LexicalItem>> existingEdges =
              this.getEdges(edge.getRight()) != null ? new HashSet<>(
                  this.getEdges(edge.getRight())) : new HashSet<>();
          existingEdges.retainAll(parentEdges);
          if (existingEdges.size() == 0) {
            addEdge(parentNode, edge.getRight(), edge.getMediator(),
                edge.getRelation());
          }
        }
        removeEdge(edge);
      }
    }

    TreeSet<Type<LexicalItem>> childTypes = this.getTypes(childNode);
    if (childTypes != null && childTypes.size() > 0) {
      for (Type<LexicalItem> childType : childTypes) {
        addType(parentNode, childType.getModifierNode(),
            childType.getEntityType());
        getTypes().remove(childType);
      }
      getNodeTypes().remove(childNode);
    }
    unifyNodes(parentNode, childNode);
  }

  public void addGroundedToUngroundedEdges(Edge<LexicalItem> groundedEdge,
      Edge<LexicalItem> ungroundedEdge) {
    Preconditions.checkArgument(groundedEdge.getLeft().equals(
        ungroundedEdge.getLeft()));
    Preconditions.checkArgument(groundedEdge.getRight().equals(
        ungroundedEdge.getRight()));
    Preconditions.checkArgument(groundedEdge.getMediator().equals(
        ungroundedEdge.getMediator()));
    if (groundedToUngrounded == null)
      groundedToUngrounded = new HashMap<>();

    groundedToUngrounded.put(groundedEdge,
        Pair.of(groundedEdge, ungroundedEdge));
  }

  public Pair<Edge<LexicalItem>, Edge<LexicalItem>> getGroundedToUngroundedEdges(
      Edge<LexicalItem> groundedEdge) {
    if (groundedToUngrounded == null)
      return null;
    return groundedToUngrounded.getOrDefault(groundedEdge, null);
  }

  public void removeGroundedToUngroundedEdges(Edge<LexicalItem> groundedEdge) {
    groundedToUngrounded.remove(groundedEdge);
  }

  public void removeEdge(Edge<LexicalItem> edge) {
    getEdges().remove(edge);
    getEdges(edge.getLeft()).remove(edge);
    getEdges(edge.getRight()).remove(edge.inverse());
  }

  public boolean isQuestionNode(LexicalItem node) {
    if (getProperties().get(node) != null) {
      for (Property property : getProperties().get(node)) {
        if (property.getPropertyName().equals("QUESTION"))
          return true;
      }
    }
    return false;
  }

  public boolean isCountNode(LexicalItem node) {
    if (getProperties().get(node) != null) {
      for (Property property : getProperties().get(node)) {
        if (property.getPropertyName().equals("COUNT"))
          return true;
      }
    }
    return false;
  }

  public void removeType(Type<LexicalItem> mergedType) {
    getTypes().remove(mergedType);
    getTypes(mergedType.getParentNode()).remove(mergedType);
  }

  public HashSet<LexicalItem> getMidNode(String mid) {
    HashSet<LexicalItem> midNodes = new HashSet<>();
    for (LexicalItem node : getNodes()) {
      if (node.getMid().equals(mid)) {
        midNodes.add(node);
      }
    }
    return midNodes;
  }

  public HashSet<LexicalItem> getQuestionNode() {
    HashSet<LexicalItem> nodes = new HashSet<>();
    for (LexicalItem node : getNodes()) {
      if (isQuestionNode(node)) {
        nodes.add(node);
      }
    }
    return nodes;
  }

  public HashSet<LexicalItem> getCountNode() {
    HashSet<LexicalItem> nodes = new HashSet<>();
    for (LexicalItem node : getNodes()) {
      if (isCountNode(node)) {
        nodes.add(node);
      }
    }
    return nodes;
  }

  public boolean hasPath(LexicalItem node1, LexicalItem node2) {
    // Check if a path exists.
    Set<LexicalItem> nodesVisited = new HashSet<>();
    LinkedList<LexicalItem> toVisitNodes = new LinkedList<>();
    toVisitNodes.add(node1);
    while (toVisitNodes.peek() != null) {
      LexicalItem node = toVisitNodes.poll();
      nodesVisited.add(node);
      TreeSet<Edge<LexicalItem>> edges = getEdges(node);
      if (edges == null)
        continue;
      for (Edge<LexicalItem> edge : edges) {
        LexicalItem newNode = edge.getRight();
        if (!nodesVisited.contains(newNode)) {
          if (newNode.equals(node2)) {
            return true;
          }
          toVisitNodes.add(newNode);
        }
      }
    }
    return false;
  }

  public boolean hasPathBetweenQuestionAndEntityNodes() {
    Set<LexicalItem> realQuestionNodes = getQuestionNode();
    Set<LexicalItem> questionNodes =
        realQuestionNodes.stream().filter(x -> !x.isEntity())
            .collect(Collectors.toSet());

    if (questionNodes.size() == 0) {
      return false;
    }

    Set<LexicalItem> entityNodes =
        this.getActualNodes().stream().filter(x -> x.isEntity())
            .collect(Collectors.toSet());

    if (entityNodes.size() == 0)
      return false;

    boolean hasPath = true;
    for (LexicalItem questionNode : questionNodes) {
      for (LexicalItem entityNode : entityNodes) {
        hasPath &= hasPath(questionNode, entityNode);
      }
    }

    return hasPath;
  }

  public boolean removeMultipleQuestionNodes() {
    List<LexicalItem> questionNodes = new ArrayList<>(getQuestionNode());
    if (questionNodes.size() < 2)
      return false;
    questionNodes.sort(Comparator.comparing(x -> x.getWordPosition()));
    for (int i = 1; i < questionNodes.size(); i++) {
      LexicalItem questionNode = questionNodes.get(i);
      Set<Property> nodeProps = getProperties(questionNode);
      Set<Property> questionProps =
          nodeProps
              .stream()
              .filter(
                  x -> x.getPropertyName().equals(
                      SemanticCategoryType.QUESTION.toString()))
              .collect(Collectors.toSet());

      if (nodeProps.size() == questionProps.size()) {
        getProperties().remove(questionNode);
      } else {
        nodeProps.removeAll(questionProps);
      }
    }
    return true;
  }

  public Set<Edge<LexicalItem>> getMergeableEdges() {
    Set<Edge<LexicalItem>> mergeableEdges = new HashSet<>();
    HashSet<LexicalItem> questionNodes = getQuestionNode();
    if (questionNodes.size() == 0)
      return mergeableEdges;
    LexicalItem questionNode = questionNodes.iterator().next();
    Map<LexicalItem, Edge<LexicalItem>> shortestPathEdges = new HashMap<>();
    Set<LexicalItem> graphNodes = getNodes();

    Set<LexicalItem> nodesVisited = new HashSet<>();
    LinkedList<LexicalItem> toVisitNodes = new LinkedList<>();
    toVisitNodes.add(questionNode);
    while (toVisitNodes.peek() != null) {
      LexicalItem node = toVisitNodes.poll();
      nodesVisited.add(node);
      TreeSet<Edge<LexicalItem>> edges = getEdges(node);
      if (edges == null)
        continue;
      for (Edge<LexicalItem> edge : edges) {
        LexicalItem newNode = edge.getRight();
        if (!nodesVisited.contains(newNode)) {
          shortestPathEdges.put(newNode, edge);
          toVisitNodes.add(newNode);
        }
      }
    }

    for (LexicalItem node : graphNodes) {
      if (node.isEntity()) {
        LexicalItem tempNode = node;
        while (shortestPathEdges.containsKey(tempNode)) {
          Edge<LexicalItem> edge = shortestPathEdges.get(tempNode);
          mergeableEdges.add(edge);
          tempNode = edge.getLeft();
        }
      }
    }

    return mergeableEdges;
  }

  public List<Edge<LexicalItem>> getShortestPath(LexicalItem node1,
      LexicalItem node2) {
    Map<LexicalItem, Edge<LexicalItem>> shortestPathEdges = new HashMap<>();

    Set<LexicalItem> nodesVisited = new HashSet<>();
    LinkedList<LexicalItem> toVisitNodes = new LinkedList<>();
    toVisitNodes.add(node1);
    while (toVisitNodes.peek() != null) {
      LexicalItem node = toVisitNodes.poll();
      nodesVisited.add(node);
      TreeSet<Edge<LexicalItem>> edges = getEdges(node);
      if (edges == null)
        continue;
      for (Edge<LexicalItem> edge : edges) {
        LexicalItem newNode = edge.getRight();
        if (!nodesVisited.contains(newNode)) {
          shortestPathEdges.put(newNode, edge);
          toVisitNodes.add(newNode);
          if (newNode.equals(node2))
            break;
        }
      }
    }

    List<Edge<LexicalItem>> shortestPath = new ArrayList<>();
    LexicalItem tempNode = node2;
    while (shortestPathEdges.containsKey(tempNode)) {
      Edge<LexicalItem> edge = shortestPathEdges.get(tempNode);
      shortestPath.add(0, edge);
      tempNode = edge.getLeft();
    }

    return shortestPath;
  }

  public void hyperExpand() {
    List<LexicalItem> graphNodes = Lists.newArrayList(getActualNodes());
    HashSet<LexicalItem> questionNodes = getQuestionNode();
    if (questionNodes.size() == 0) {
      LexicalItem firstItem = getActualNodes().get(0);
      this.addProperty(firstItem,
          new Property(SemanticCategoryType.QUESTION.toString()));
      questionNodes.add(firstItem);
    }
    LexicalItem questionNode = questionNodes.iterator().next();

    for (LexicalItem entityNode : graphNodes) {
      if (entityNode.isEntity()) {
        List<Edge<LexicalItem>> shortestPath =
            getShortestPath(questionNode, entityNode);

        if (shortestPath.size() == 0) {
          LexicalItem dummyNode =
              new LexicalItem("", SentenceKeys.DUMMY_WORD,
                  SentenceKeys.DUMMY_WORD, SentenceKeys.PUNCTUATION_TAGS
                      .iterator().next(), "", null);
          dummyNode.setWordPosition(getActualNodes().size());
          getActualNodes().add(dummyNode);
          Edge<LexicalItem> directEdge =
              new Edge<>(questionNode, entityNode, dummyNode, new Relation(
                  SentenceKeys.DUMMY_WORD, SentenceKeys.DUMMY_WORD));
          addEdge(directEdge);
          shortestPath.add(directEdge);
        }

        Edge<LexicalItem> mainEdge = shortestPath.get(shortestPath.size() - 1);
        List<Edge<LexicalItem>> entityEdges =
            Lists.newArrayList(getEdges(entityNode));
        for (Edge<LexicalItem> entityEdge : entityEdges) {
          removeEdge(entityEdge);
        }
        addEdge(new Edge<>(questionNode, entityNode, mainEdge.getMediator(),
            mainEdge.getRelation()));
      }
    }
  }
}
