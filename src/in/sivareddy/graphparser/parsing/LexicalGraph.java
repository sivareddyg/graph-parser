package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Graph;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.ml.basic.AbstractFeature;
import in.sivareddy.ml.basic.Feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Created by bisk1 on 1/26/15.
 */
public class LexicalGraph extends Graph<LexicalItem> {
  private List<String> words;
  private Set<Feature> features;
  private String syntacticParse;
  private Set<String> semanticParse;
  StemMatchingFeature stemMatchingFeature;
  ArgStemMatchingFeature argStemMatchingFeature;
  MediatorStemGrelPartMatchingFeature mediatorStemGrelPartMatchingFeature;
  ArgStemGrelPartMatchingFeature argStemGrelPartMatchingFeature;
  DuplicateEdgeFeature duplicateEdgeFeature;

  Set<LexicalItem> argumentsStemsMatched = Sets.newHashSet();
  Set<LexicalItem> mediatorsStemsMatched = Sets.newHashSet();

  Set<LexicalItem> argumentStemGrelPartMatchedNodes = Sets.newHashSet();
  Set<LexicalItem> mediatorStemGrelPartMatchedNodes = Sets.newHashSet();

  public LexicalGraph() {
    super();
    words = new ArrayList<>();
    features = Sets.newHashSet();

    stemMatchingFeature = new StemMatchingFeature(0.0);
    argStemMatchingFeature = new ArgStemMatchingFeature(0.0);
    mediatorStemGrelPartMatchingFeature =
        new MediatorStemGrelPartMatchingFeature(0.0);
    argStemGrelPartMatchingFeature = new ArgStemGrelPartMatchingFeature(0.0);
    duplicateEdgeFeature = new DuplicateEdgeFeature(0.0);

    features.add(stemMatchingFeature);
    features.add(argStemMatchingFeature);
    features.add(mediatorStemGrelPartMatchingFeature);
    features.add(argStemGrelPartMatchingFeature);
    features.add(duplicateEdgeFeature);
  }

  public Set<Feature> getFeatures() {
    return features;
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

  @SuppressWarnings("unchecked")
  private static Set<Class<? extends AbstractFeature>> globalFeatures = Sets
      .newHashSet(StemMatchingFeature.class, ValidQueryFeature.class,
          ArgStemMatchingFeature.class,
          MediatorStemGrelPartMatchingFeature.class,
          ArgStemGrelPartMatchingFeature.class, GraphIsConnectedFeature.class,
          GraphNodeCountFeature.class, EdgeNodeCountFeature.class,
          DuplicateEdgeFeature.class);

  public void addFeature(Feature feature) {
    if (globalFeatures.contains(feature.getClass())) {
      if (features.contains(feature))
        features.remove(feature);
    }
    if (features.contains(feature)) {
      features.remove(feature);
      feature.setFeatureValue(feature.getFeatureValue() + 1.0);
      features.add(feature);
    } else {
      features.add(feature);
    }
  }

  public LexicalGraph copy() {
    LexicalGraph newGraph = new LexicalGraph();
    copyTo(newGraph);
    newGraph.words.addAll(words);
    newGraph.features = Sets.newHashSet(features);

    newGraph.syntacticParse = syntacticParse;
    newGraph.semanticParse = semanticParse;

    newGraph.stemMatchingFeature.setFeatureValue(stemMatchingFeature
        .getFeatureValue());
    newGraph.addFeature(newGraph.stemMatchingFeature);
    newGraph.mediatorsStemsMatched = Sets.newHashSet(mediatorsStemsMatched);

    newGraph.argStemMatchingFeature.setFeatureValue(argStemMatchingFeature
        .getFeatureValue());
    newGraph.addFeature(newGraph.argStemMatchingFeature);
    newGraph.argumentsStemsMatched = Sets.newHashSet(argumentsStemsMatched);

    newGraph.mediatorStemGrelPartMatchingFeature
        .setFeatureValue(mediatorStemGrelPartMatchingFeature.getFeatureValue());
    newGraph.addFeature(newGraph.mediatorStemGrelPartMatchingFeature);
    newGraph.mediatorStemGrelPartMatchedNodes =
        Sets.newHashSet(mediatorStemGrelPartMatchedNodes);

    newGraph.argStemGrelPartMatchingFeature
        .setFeatureValue(argStemGrelPartMatchingFeature.getFeatureValue());
    newGraph.addFeature(newGraph.argStemGrelPartMatchingFeature);
    newGraph.argumentStemGrelPartMatchedNodes =
        Sets.newHashSet(argumentStemGrelPartMatchedNodes);

    newGraph.duplicateEdgeFeature.setFeatureValue(duplicateEdgeFeature
        .getFeatureValue());
    newGraph.addFeature(newGraph.duplicateEdgeFeature);

    ValidQueryFeature feat = new ValidQueryFeature(false);
    newGraph.addFeature(feat);

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

  public List<String> getWords() {
    return words;
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
}
