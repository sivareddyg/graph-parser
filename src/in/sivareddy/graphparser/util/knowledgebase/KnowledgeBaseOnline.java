package in.sivareddy.graphparser.util.knowledgebase;

import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class KnowledgeBaseOnline implements KnowledgeBase {
  static Integer MAX_CACHE_SIZE = 100000;
  static String FB_MEDIATIOR = "freebase.type_hints.mediator";
  static String FB_MASTER = "type.property.master_property";

  static Set<String> standardTypes = Sets.newHashSet("type.datetime",
      "type.int", "type.float");
  static Map<String, String> fbStandardToRDFStandard = ImmutableMap.of(
      "type.datetime", "xsd:datetime", "type.float", "xsd:float",
      "type.integer", "xsd:integer");

  private RdfGraphTools endPoint = null;
  private Schema schema;

  LoadingCache<Pair<String, String>, Set<Relation>> entitiesToRelations;
  LoadingCache<String, Set<Relation>> entityToRelations;
  LoadingCache<String, Set<String>> entityToTypes;
  Set<Relation> relationsThatAreTypes = new HashSet<>();

  public KnowledgeBaseOnline(String jdbcEndPoint, String httpEndPoint,
      String username, String password, int timeOut, Schema schema) {
    super();
    endPoint =
        new RdfGraphTools(jdbcEndPoint, httpEndPoint, username, password,
            timeOut);

    entitiesToRelations =
        Caffeine.newBuilder().maximumSize(100000)
            .build(x -> getRelationsPrivate(x));

    entityToRelations =
        Caffeine.newBuilder().maximumSize(100000)
            .build(x -> getRelationsPrivate(x));

    entityToTypes =
        Caffeine.newBuilder().maximumSize(100000)
            .build(x -> getTypesPrivate(x));

    this.schema = schema;
  }

  @Override
  public Set<Relation> getRelations(String entity1, String entity2) {
    if (standardTypes.contains(entity1) && standardTypes.contains(entity2)) {
      return new HashSet<>();
    }

    boolean order = inOrder(entity1, entity2);
    Pair<String, String> key =
        order ? Pair.of(entity1, entity2) : Pair.of(entity2, entity1);

    Set<Relation> relations = entitiesToRelations.get(key);
    if (order) {
      return relations;
    }
    return getInverseRelations(relations);
  }

  private static Set<Relation> getInverseRelations(Set<Relation> relations) {
    Set<Relation> inverseRelations = Sets.newHashSet();
    relations.forEach(x -> inverseRelations.add(x.inverse()));
    return inverseRelations;
  }

  private Set<Relation> getRelationsPrivate(Pair<String, String> key) {
    Set<Relation> relations = new HashSet<>();

    if (!standardTypes.contains(key.getRight())) {
      // Master relations.
      String query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel from <http://rdf.freebase.com> WHERE { fb:%s ?rel fb:%s . MINUS{?rel fb:type.property.master_property ?master .}}",
                  key.getLeft(), key.getRight());
      // ResultSet results = endPoint.runQueryJdbcResult(query);
      List<Map<String, String>> results =
          endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String relation = querySolution.get("rel").toString();
        relation = relation.substring(relation.lastIndexOf("/") + 1);
        if (schema.isDomainRelationAndMaster(relation)
            && !schema.hasMediatorArgument(relation)) {
          relations.add(new Relation(relation + ".1", relation + ".2"));
        }
      }

      // Inverse relations.
      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel from <http://rdf.freebase.com> WHERE { fb:%s ?rel fb:%s . MINUS { ?rel fb:type.property.master_property ?master . }}",
                  key.getRight(), key.getLeft());
      // results = endPoint.runQueryHttpSolutions(query);
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String relation = querySolution.get("rel").toString();
        relation = relation.substring(relation.lastIndexOf("/") + 1);
        if (schema.isDomainRelationAndMaster(relation)
            && !schema.hasMediatorArgument(relation)) {
          relations.add(new Relation(relation + ".2", relation + ".1"));
        }
      }

      // Mediator relations.
      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { ?m ?rel1 fb:%s . ?m ?rel2 fb:%s . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . }",
                  key.getLeft(), key.getRight());
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String rel1 = querySolution.get("rel1").toString();
        rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
        String rel2 = querySolution.get("rel2").toString();
        rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
        if (schema.isDomainRelationAndMaster(rel1)
            && schema.isDomainRelationAndMaster(rel2)
            && schema.firstArgIsMediator(rel1)
            && schema.firstArgIsMediator(rel2)) {
          relations.add(new Relation(rel1, rel2));
        }
      }

      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { fb:%s ?rel1 ?m . ?m ?rel2 fb:%s . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . }",
                  key.getLeft(), key.getRight());
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String rel1 = querySolution.get("rel1").toString();
        rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
        String rel2 = querySolution.get("rel2").toString();
        rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
        String rel1_inv = schema.getRelation2Inverse(rel1);
        if (rel1_inv != null && schema.isDomainRelationAndMaster(rel2)
            && schema.firstArgIsMediator(rel1_inv)
            && schema.firstArgIsMediator(rel2)) {
          relations.add(new Relation(rel1_inv, rel2));
        }
      }

      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE {fb:%s ?rel2 ?m . ?m ?rel1 fb:%s . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . }",
                  key.getRight(), key.getLeft());
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String rel1 = querySolution.get("rel1").toString();
        rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
        String rel2 = querySolution.get("rel2").toString();
        rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
        String rel2_inv = schema.getRelation2Inverse(rel2);
        if (rel2_inv != null && schema.isDomainRelationAndMaster(rel1)
            && schema.firstArgIsMediator(rel1)
            && schema.firstArgIsMediator(rel2_inv)) {
          relations.add(new Relation(rel1, rel2_inv));
        }
      }

      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { fb:%s ?rel1 ?m . fb:%s ?rel2 ?m . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . }",
                  key.getLeft(), key.getRight());
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String rel1 = querySolution.get("rel1").toString();
        rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
        String rel2 = querySolution.get("rel2").toString();
        rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
        String rel1_inv = schema.getRelation2Inverse(rel1);
        String rel2_inv = schema.getRelation2Inverse(rel2);
        if (rel1_inv != null && rel2_inv != null
            && schema.firstArgIsMediator(rel1_inv)
            && schema.firstArgIsMediator(rel2_inv)) {
          relations.add(new Relation(rel1_inv, rel2_inv));
        }
      }
    } else if (standardTypes.contains(key.getRight())) {
      // Master relation.
      String query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel from <http://rdf.freebase.com> WHERE { fb:%s ?rel ?e2 . MINUS{?rel fb:type.property.master_property ?master .} FILTER(datatype(?e2) = %s) .}",
                  key.getLeft(), fbStandardToRDFStandard.get(key.getRight()));
      List<Map<String, String>> results =
          endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String relation = querySolution.get("rel").toString();
        relation = relation.substring(relation.lastIndexOf("/") + 1);
        if (schema.isDomainRelationAndMaster(relation)
            && !schema.hasMediatorArgument(relation)) {
          relations.add(new Relation(relation + ".1", relation + ".2"));
        }
      }

      // Mediator relations.
      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { ?m ?rel1 fb:%s . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . ?m ?rel2 ?e2 . FILTER(datatype(?e2) = %s) . }",
                  key.getLeft(), fbStandardToRDFStandard.get(key.getRight()));
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String rel1 = querySolution.get("rel1").toString();
        rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
        String rel2 = querySolution.get("rel2").toString();
        rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
        if (schema.isDomainRelationAndMaster(rel1)
            && schema.isDomainRelationAndMaster(rel2)
            && schema.firstArgIsMediator(rel1)
            && schema.firstArgIsMediator(rel2)) {
          relations.add(new Relation(rel1, rel2));
        }
      }

      query =
          String
              .format(
                  "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { fb:%s ?rel1 ?m . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . ?m ?rel2 ?e2 . FILTER(datatype(?e2) = %s) . }",
                  key.getLeft(), fbStandardToRDFStandard.get(key.getRight()));
      results = endPoint.runQueryHttpSolutions(query);
      for (Map<String, String> querySolution : results) {
        String rel1 = querySolution.get("rel1").toString();
        rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
        String rel2 = querySolution.get("rel2").toString();
        rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
        String rel1_inv = schema.getRelation2Inverse(rel1);
        if (rel1_inv != null && schema.isDomainRelationAndMaster(rel2)
            && schema.firstArgIsMediator(rel1_inv)
            && schema.firstArgIsMediator(rel2)) {
          relations.add(new Relation(rel1_inv, rel2));
        }
      }
    }
    return relations;
  }

  private static boolean inOrder(String arg0, String arg1) {
    if (standardTypes.contains(arg0)) {
      return false;
    } else if (standardTypes.contains(arg1)) {
      return true;
    }
    return arg0.compareTo(arg1) < 0;
  }

  @Override
  public Set<Relation> getRelations(String entity) {
    if (standardTypes.contains(entity)) {
      return new HashSet<>();
    }
    Set<Relation> relations = entityToRelations.get(entity);
    return relations;
  }

  private Set<Relation> getRelationsPrivate(String entity) {
    Set<Relation> relations = new HashSet<>();

    // Master relations.
    String query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel from <http://rdf.freebase.com> WHERE { fb:%s ?rel ?e2 . MINUS{?rel fb:type.property.master_property ?master .}}",
                entity);
    List<Map<String, String>> results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String relation = querySolution.get("rel").toString();
      relation = relation.substring(relation.lastIndexOf("/") + 1);
      if (schema.isDomainRelationAndMaster(relation)
          && !schema.hasMediatorArgument(relation)) {
        relations.add(new Relation(relation + ".1", relation + ".2"));
      }
    }

    // Inverse relations.
    query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel from <http://rdf.freebase.com> WHERE { ?e2 ?rel fb:%s . MINUS { ?rel fb:type.property.master_property ?master . }}",
                entity);
    results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String relation = querySolution.get("rel").toString();
      relation = relation.substring(relation.lastIndexOf("/") + 1);
      if (schema.isDomainRelationAndMaster(relation)
          && !schema.hasMediatorArgument(relation)) {
        relations.add(new Relation(relation + ".2", relation + ".1"));
      }
    }

    // Mediator relations.
    query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { ?m ?rel1 fb:%s . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . ?m ?rel2 ?e2 . FILTER (?e2 != fb:%s) .}",
                entity, entity);
    results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String rel1 = querySolution.get("rel1").toString();
      rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
      String rel2 = querySolution.get("rel2").toString();
      rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
      if (schema.isDomainRelationAndMaster(rel1)
          && schema.isDomainRelationAndMaster(rel2)
          && schema.firstArgIsMediator(rel1) && schema.firstArgIsMediator(rel2)) {
        relations.add(new Relation(rel1, rel2));
      }
    }

    query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { fb:%s ?rel1 ?m . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . ?m ?rel2 ?e2 . FILTER (?e2 != fb:%s) .}",
                entity, entity);
    results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String rel1 = querySolution.get("rel1").toString();
      rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
      String rel2 = querySolution.get("rel2").toString();
      rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
      String rel1_inv = schema.getRelation2Inverse(rel1);
      if (rel1_inv != null && schema.isDomainRelationAndMaster(rel2)
          && schema.firstArgIsMediator(rel1_inv)
          && schema.firstArgIsMediator(rel2)) {
        relations.add(new Relation(rel1_inv, rel2));
      }
    }

    query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE {?m ?rel1 fb:%s . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . ?e2 ?rel2 ?m . FILTER (?e2 != fb:%s) .}",
                entity, entity);
    results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String rel1 = querySolution.get("rel1").toString();
      rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
      String rel2 = querySolution.get("rel2").toString();
      rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
      String rel2_inv = schema.getRelation2Inverse(rel2);
      if (rel2_inv != null && schema.isDomainRelationAndMaster(rel1)
          && schema.firstArgIsMediator(rel1)
          && schema.firstArgIsMediator(rel2_inv)) {
        relations.add(new Relation(rel1, rel2_inv));
      }
    }

    query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?rel1 ?rel2 from <http://rdf.freebase.com> WHERE { fb:%s ?rel1 ?m . ?m fb:type.object.type ?z . ?z fb:freebase.type_hints.mediator true . ?e2 ?rel2 ?m . FILTER (?e2 != fb:%s) .}",
                entity, entity);
    results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String rel1 = querySolution.get("rel1").toString();
      rel1 = rel1.substring(rel1.lastIndexOf("/") + 1);
      String rel2 = querySolution.get("rel2").toString();
      rel2 = rel2.substring(rel2.lastIndexOf("/") + 1);
      String rel1_inv = schema.getRelation2Inverse(rel1);
      String rel2_inv = schema.getRelation2Inverse(rel2);
      if (rel1_inv != null && rel2_inv != null
          && schema.firstArgIsMediator(rel1_inv)
          && schema.firstArgIsMediator(rel2_inv)) {
        relations.add(new Relation(rel1_inv, rel2_inv));
      }
    }
    return relations;
  }

  @Override
  public boolean hasRelation(String entity1, String entity2) {
    return getRelations(entity1, entity2).size() > 0;
  }

  @Override
  public Set<String> getTypes(String entity) {
    return entityToTypes.get(entity);
  }

  private Set<String> getTypesPrivate(String entity) {
    Set<String> types = new HashSet<>();

    String query =
        String
            .format(
                "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?type from <http://rdf.freebase.com> WHERE { fb:%s fb:type.object.type ?type . }",
                entity);
    List<Map<String, String>> results = endPoint.runQueryHttpSolutions(query);
    for (Map<String, String> querySolution : results) {
      String type = querySolution.get("type").toString();
      type = type.substring(type.lastIndexOf("/") + 1);
      if (schema.isMainType(type)) {
        types.add(type);
      }
    }

    // TODO(sivareddyg) Relations that are types.
    return types;
  }

  @Override
  public boolean hasEntity(String entity) {
    if (entity.startsWith("m.") || standardTypes.contains(entity))
      return true;
    return false;
  }

  public void loadRelationsThatAreTypes(String relationTypesFile)
      throws IOException {
    JsonParser parser = new JsonParser();
    BufferedReader br = new BufferedReader(new FileReader(relationTypesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        String[] parts = line.split("\t")[0].split(" # ");
        String argNumber = parts[1];
        JsonElement jelement = parser.parse(parts[0]);
        JsonArray relation = jelement.getAsJsonArray();

        JsonArray relationEdges = relation.getAsJsonArray();
        String edge1;
        String edge2;
        if (relationEdges.size() == 1) {
          String relationName = relationEdges.get(0).getAsString();
          edge1 = relationName + ".1";
          edge2 = relationName + ".2";
        } else {
          edge1 = relationEdges.get(0).getAsString();
          edge2 = relationEdges.get(1).getAsString();
        }

        if (argNumber.equals("right_arg")) {
          Relation edge = Relation.of(edge1, edge2);
          relationsThatAreTypes.add(edge);
        } else if (argNumber.equals("left_arg")) {
          Relation edge_inverse = Relation.of(edge2, edge1);
          relationsThatAreTypes.add(edge_inverse);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
