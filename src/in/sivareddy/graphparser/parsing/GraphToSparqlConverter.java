package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.graph.Type;
import in.sivareddy.graphparser.util.knowledgebase.EntityType;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class GraphToSparqlConverter {
  public static String TYPE_KEY = "rdf:type";

  public static String convertGroundedGraph(LexicalGraph graph, Schema schema) {
    return convertGroundedGraph(graph, null, schema, null);
  }

  public static String convertGroundedGraph(LexicalGraph graph, Schema schema,
      List<String> kbGraphUri) {
    return convertGroundedGraph(graph, null, schema, kbGraphUri);
  }

  // convert grounded graph to sparql query
  public static String convertGroundedGraph(LexicalGraph graph,
      LexicalItem targetNode, Schema schema, List<String> graphUris) {
    Map<String, Integer> mediatorKeys = Maps.newHashMap();
    TreeSet<Edge<LexicalItem>> edges = Sets.newTreeSet(graph.getEdges());
    int edgeCount = 0;
    List<String> queryTriples = Lists.newArrayList();

    String graphString = "";
    if (graphUris != null && graphUris.size() > 0) {
      for (String graphUri : graphUris) {
        graphString += graphString = "FROM <" + graphUri + "> ";
      }
    }

    // rdf:type
    Set<String> standardTypes =
        Sets.newHashSet("type.datetime", "type.int", "type.float");
    TreeSet<Type<LexicalItem>> nodeTypes = graph.getTypes();
    for (Type<LexicalItem> nodeType : nodeTypes) {
      String tripleName = "";
      LexicalItem parentNode = nodeType.getParentNode();
      // LexicalItem modifierNode = nodeType.getModifierNode();
      EntityType entityType = nodeType.getEntityType();
      if (standardTypes.contains(entityType.getType())
          || standardTypes.contains(parentNode.getMid())) {
        // do not ground types taking numbers as arguments
        continue;
      }
      if (entityType.getType().equals("type.empty")) {
        // do not ground EMPTY types
        continue;
      }

      String parentVarName = getNodeVariable(parentNode, targetNode);
      String typeName = entityType.getType();
      String[] parts = typeName.split("#");
      if (parts.length > 1) {
        LexicalItem mediator = nodeType.getModifierNode();
        LexicalItem leftNode = nodeType.getParentNode();
        LexicalItem rightNode = mediator.shallowCopy();
        rightNode.setLemma(parts[2]);
        rightNode.setWord(parts[2]);
        rightNode.setMid(parts[2]);
        rightNode.setWordPosition(-1);
        Relation relation = new Relation(parts[0], parts[1]);
        Edge<LexicalItem> edge =
            new Edge<>(leftNode, rightNode, mediator, relation);
        edges.add(edge);
      } else {
        tripleName =
            String.format("%s %s fb:%s . ", parentVarName, TYPE_KEY, typeName);
        queryTriples.add(tripleName);
      }
    }

    for (Edge<LexicalItem> edge : edges) {
      edgeCount += 1;
      LexicalItem leftNode = edge.getLeft();
      LexicalItem rightNode = edge.getRight();
      LexicalItem mediator = edge.getMediator();
      Relation relation = edge.getRelation();
      String leftEdge = relation.getLeft();
      String rightEdge = relation.getRight();

      int leftEdgeLength = leftEdge.length();
      int rightEdgeLength = rightEdge.length();
      String tripleName = "";

      String leftNodeVar = getNodeVariable(leftNode, targetNode);
      String rightNodeVar = getNodeVariable(rightNode, targetNode);
      if (leftEdgeLength == rightEdgeLength
          && leftEdge.substring(0, leftEdgeLength - 2).equals(
              rightEdge.substring(0, rightEdgeLength - 2))) {
        // normal
        String relationName = leftEdge.substring(0, leftEdgeLength - 2);
        if (leftEdge.charAt(leftEdgeLength - 1) == '1'
            && rightEdge.charAt(rightEdgeLength - 1) == '2') {
          tripleName =
              String.format("%s fb:%s %s . ", leftNodeVar, relationName,
                  rightNodeVar);
        } else if (leftEdge.charAt(leftEdgeLength - 1) == '2'
            && rightEdge.charAt(rightEdgeLength - 1) == '1') {
          tripleName =
              String.format("%s fb:%s %s . ", rightNodeVar, relationName,
                  leftNodeVar);
        } else {
          tripleName =
              String.format(
                  "?e%d fb:%s %s . ?e%s fb:%s %s . FILTER(%s != %s) . ",
                  edgeCount, leftEdge, leftNodeVar, edgeCount, rightEdge,
                  rightNodeVar, leftNodeVar, rightNodeVar);
        }
      } else {
        // mediator relation
        String mediatorType = schema.getMediatorArgument(leftEdge);
        Preconditions.checkArgument(mediatorType != null,
            "Relation is not mediator");
        String mediatorKey = String.valueOf(mediator.hashCode());
        mediatorKey = mediatorKey + ":" + mediatorType;
        if (!mediatorKeys.containsKey(mediatorKey)) {
          mediatorKeys.put(mediatorKey, edgeCount);
        }
        int mediatorIndex = mediatorKeys.get(mediatorKey);

        String leftEdgeTriple = "";
        List<String> leftEdgeParts =
            Lists.newArrayList(Splitter.on(".").split(leftEdge));
        if (leftEdgeParts.get(leftEdgeParts.size() - 1).equals("inverse")) {
          String relationName =
              Joiner.on(".").join(
                  leftEdgeParts.subList(0, leftEdgeParts.size() - 1));
          leftEdgeTriple =
              String.format("%s fb:%s ?m%d . ", leftNodeVar, relationName,
                  mediatorIndex);
        } else {
          String relationName = leftEdge;
          leftEdgeTriple =
              String.format("?m%d fb:%s %s . ", mediatorIndex, relationName,
                  leftNodeVar);
        }

        String rightEdgeTriple = "";
        List<String> rightEdgeParts =
            Lists.newArrayList(Splitter.on(".").split(rightEdge));
        if (rightEdgeParts.get(rightEdgeParts.size() - 1).equals("inverse")) {
          String relationName =
              Joiner.on(".").join(
                  rightEdgeParts.subList(0, rightEdgeParts.size() - 1));
          rightEdgeTriple =
              String.format("%s fb:%s ?m%d . ", rightNodeVar, relationName,
                  mediatorIndex);
        } else {
          String relationName = rightEdge;
          rightEdgeTriple =
              String.format("?m%d fb:%s %s . ", mediatorIndex, relationName,
                  rightNodeVar);
        }
        tripleName =
            leftEdgeTriple
                + rightEdgeTriple
                + String.format("FILTER(%s != %s) . ", leftNodeVar,
                    rightNodeVar);
      }
      queryTriples.add(tripleName);
    }

    String queryString = Joiner.on(" ").join(queryTriples);

    Map<LexicalItem, Set<Property>> properties = graph.getProperties();
    String targetVar = "";
    List<String> countVars = Lists.newArrayList();

    // each property has semantics associated to it.
    // TODO: NEGATION, COMPLEMENT
    for (LexicalItem node : properties.keySet()) {
      Set<Property> nodeProperties = properties.get(node);
      for (Property property : nodeProperties) {
        if (property.getPropertyName().equals("COUNT")) {
          String countNode = property.getArguments().trim().split(":")[0];
          queryString =
              String.format(
                  "SELECT count(DISTINCT %s) AS ?x%s %s WHERE { %s }",
                  getNodeVariable(node, targetNode), countNode, graphString,
                  queryString);
          countVars.add(String.format("?x%s", countNode));
        } else if (property.getPropertyName().equals("QUESTION")) {
          targetVar = getNodeVariable(node, targetNode);
        }
      }
    }

    if (targetNode != null && !targetVar.equals("")) {
      System.err
          .println("Warning: Target variable and QUESTION property both present. Target variable overides QUESTION");
    }

    if (targetNode != null) {
      // Overiding target variable.
      targetVar = getNodeVariable(targetNode, targetNode);
    }

    if (!targetVar.equals("")) {
      if (countVars.contains(targetVar)) {
        queryString =
            String
                .format(
                    "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                        + "SELECT DISTINCT %s %s WHERE { %s } LIMIT 10",
                    targetVar, graphString, queryString);
      } else {
        String nameString =
            String
                .format(
                    "OPTIONAL {FILTER(langMatches(lang(%sname), \"en\")) . FILTER(!langMatches(lang(%sname), \"en-gb\")) . %s fb:type.object.name %sname . }",
                    targetVar, targetVar, targetVar, targetVar);
        queryString =
            String
                .format(
                    "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                        + "SELECT DISTINCT %s %sname %s WHERE { %s %s } LIMIT 10",
                    targetVar, targetVar, graphString, queryString, nameString);
      }
    } else {
      queryString =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                      + "ASK %s WHERE { %s } ", graphString, queryString);
    }

    return queryString;
  }

  public static String getNodeVariable(LexicalItem node) {
    return getNodeVariable(node, null);
  }

  public static String getNodeVariable(LexicalItem node,
      LexicalItem restrictedNode) {
    String mid = node.getMid();
    if (!node.equals(restrictedNode)) {
      if (node.getMid().startsWith("m.")) {
        return "fb:" + mid;
      } else if (node.getMid().equals("type.datetime")) {
        Pattern yearPattern = Pattern.compile("([0-9]+)");
        String word = node.getWord();
        Matcher matcher = yearPattern.matcher(word);
        // default year is set to 2009 since the corpus is from
        // clueweb09
        String year =
            matcher.find() ? String.format("\"%s\"^^xsd:datetime",
                matcher.group(1)) : String.format("?x%d",
                node.getWordPosition());
        return year;
      }
    }
    return String.format("?x%d", node.getWordPosition());
  }

}
