package in.sivareddy.graphparser.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 *
 * Prints a freebase domain
 *
 * @author Siva Reddy
 *
 *
 */
public class PrintFreebaseDomain {

  /**
   * @throws FileNotFoundException
   *
   */

  /**
   * @param url
   * @param username
   * @param password
   * @param domainUri
   * @param schemaFileName
   * @throws IOException
   */
  public static void print(String url, String username, String password,
      List<String> domainUris, String schemaFileName) throws IOException {

    Schema schema = new Schema(schemaFileName);
    String namespace = "http://rdf.freebase.com/ns/";

    RdfGraphTools rdfGraphTools = new RdfGraphTools(url, username, password);
    Map<List<String>, Set<List<String>>> domainRelations = Maps.newHashMap();
    Map<String, Set<String>> domainTypes = Maps.newHashMap();

    Set<String> types = schema.getTypes();

    String domainUri = "";
    for (String domain : domainUris)
      domainUri += String.format("FROM <%s> ", domain);

    for (String type : types) {
      System.err.println("Type: " + type);
      List<String> typeRelations = schema.getType2Relations(type);

      Boolean typeIsMediator = schema.typeIsMediator(type);

      // if the type is not mediator
      if (!typeIsMediator) {

        // Get unary relations
        String query =
            String
                .format(
                    "PREFIX ns: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
                        + "SELECT ?x ?y %s "
                        + "WHERE { "
                        + "?x rdf:type ns:%s . }", domainUri, type);
        System.err.println("Query = " + query);

        ResultSet results = rdfGraphTools.runQueryJdbcResultSet(query);
        int count = 0;
        while (results.hasNext()) {
          count += 1;
          QuerySolution result = results.nextSolution();
          String x = result.get("x").toString();
          x = x.replace(namespace, "");

          if (!x.startsWith("m.")) {
            continue;
          }

          String key = x;
          String value = type;

          if (!domainTypes.containsKey(key)) {
            domainTypes.put(key, new HashSet<String>());
          }
          domainTypes.get(key).add(value);
        }
        System.err.println("Result count = " + count);

        // Get binary relations
        for (String relation : typeRelations) {
          System.err.println("Relation: " + relation);
          Boolean master = schema.getRelationIsMaster(relation);
          String childArgType = schema.getRelationArguments(relation).get(1);
          Boolean childIsMediator = schema.typeIsMediator(childArgType);
          if (childIsMediator == null || childIsMediator) {
            continue;
          }

          // child should either be a type in the domain or an acceptable type
          if (!types.contains(childArgType)
              && !Schema.acceptableCommonTypes.contains(childArgType)) {
            continue;
          }

          if (master) {
            query =
                String.format("PREFIX ns: <http://rdf.freebase.com/ns/> "
                    + "SELECT ?x ?y %s " + "WHERE { " + "?x ns:%s ?y . }",
                    domainUri, relation);
            System.err.println("Query = " + query);

            results = rdfGraphTools.runQueryJdbcResultSet(query);
            count = 0;
            while (results.hasNext()) {
              count += 1;
              QuerySolution result = results.nextSolution();
              String x = result.get("x").toString();
              String y = result.get("y").toString();
              x = x.replace(namespace, "");

              if (Schema.acceptableCommonTypes.contains(childArgType)) {
                y = childArgType;
              } else {
                y = y.replace(namespace, "");
              }

              if (!x.startsWith("m.")) {
                continue;
              }

              List<String> key = Lists.newArrayList(x, y);
              List<String> value = Lists.newArrayList(relation);

              if (!domainRelations.containsKey(key)) {
                domainRelations.put(key, new HashSet<List<String>>());
              }
              domainRelations.get(key).add(value);
            }
            System.err.println("Result count = " + count);
          }
        }
      } else {
        // type is mediator
        for (int i = 0; i < typeRelations.size(); i++) {
          for (int j = i + 1; j < typeRelations.size(); j++) {
            String relation1 = typeRelations.get(i);
            String relation2 = typeRelations.get(j);

            String relation1ChildType =
                schema.getRelationArguments(relation1).get(1);
            String relation2ChildType =
                schema.getRelationArguments(relation2).get(1);

            // at least one of the entities should belong to the
            // domain
            if (!types.contains(relation1ChildType)
                && !types.contains(relation2ChildType)) {
              continue;
            }

            Boolean relation1ChildTypeMediator =
                schema.typeIsMediator(relation1ChildType);
            Boolean relation2ChildTypeMediator =
                schema.typeIsMediator(relation2ChildType);

            if (relation1ChildTypeMediator != null
                && relation1ChildTypeMediator) {
              continue;
            }

            if (relation2ChildTypeMediator != null
                && relation2ChildTypeMediator) {
              continue;
            }

            System.err.println("Relation1: " + relation1);
            System.err.println("Relation2: " + relation2);

            /*- if (!relation1.startsWith(domain)
            		|| !relation2.startsWith(domain))
            	continue; */

            String query;
            if (relation1.contains(".inverse")
                && relation2.contains(".inverse")) {
              query =
                  String.format("PREFIX ns: <http://rdf.freebase.com/ns/> "
                      + "SELECT ?x ?y %s " + "WHERE { " + "?x ns:%s ?z . "
                      + "?y ns:%s ?z . FILTER (?x != ?y) .}", domainUri,
                      relation1.replace(".inverse", ""),
                      relation2.replace(".inverse", ""));
            } else if (relation1.contains(".inverse")) {
              query =
                  String.format("PREFIX ns: <http://rdf.freebase.com/ns/> "
                      + "SELECT ?x ?y %s " + "WHERE { " + "?x ns:%s ?z . "
                      + "?z ns:%s ?y . FILTER (?x != ?y) .}", domainUri,
                      relation1.replace(".inverse", ""), relation2);
            } else if (relation2.contains(".inverse")) {
              query =
                  String.format("PREFIX ns: <http://rdf.freebase.com/ns/> "
                      + "SELECT ?x ?y %s " + "WHERE { " + "?z ns:%s ?x . "
                      + "?y ns:%s ?z . FILTER (?x != ?y) .}", domainUri,
                      relation1, relation2.replace(".inverse", ""));
            } else {
              query =
                  String.format("PREFIX ns: <http://rdf.freebase.com/ns/> "
                      + "SELECT ?x ?y %s " + "WHERE { " + "?z ns:%s ?x . "
                      + "?z ns:%s ?y . FILTER (?x != ?y) .}", domainUri,
                      relation1, relation2);
            }

            System.err.println("Query = " + query);

            ResultSet results = rdfGraphTools.runQueryJdbcResultSet(query);
            int count = 0;
            while (results.hasNext()) {
              count += 1;
              QuerySolution result = results.nextSolution();
              String x = result.get("x").toString();
              String y = result.get("y").toString();
              if (Schema.acceptableCommonTypes.contains(relation1ChildType)) {
                x = relation1ChildType;
              } else {
                x = x.replace(namespace, "");
              }
              if (Schema.acceptableCommonTypes.contains(relation2ChildType)) {
                y = relation2ChildType;
              } else {
                y = y.replace(namespace, "");
              }

              if (!x.startsWith("m.") && !y.startsWith("m.")) {
                continue;
              }

              List<String> key = Lists.newArrayList(x, y);
              List<String> value = Lists.newArrayList(relation1, relation2);

              if (!domainRelations.containsKey(key)) {
                domainRelations.put(key, new HashSet<List<String>>());
              }
              domainRelations.get(key).add(value);
            }
            System.err.println("Result count = " + count);
          }
        }
      }

    }

    Gson gson = new Gson();

    System.out.println("# Entity Types");
    for (String key : domainTypes.keySet()) {
      System.out.println(String.format("%s\t%s", gson.toJson(key),
          gson.toJson(domainTypes.get(key))));
    }

    System.out.println("# Binary Relations");
    for (List<String> key : domainRelations.keySet()) {
      System.out.println(String.format("%s\t%s", gson.toJson(key),
          gson.toJson(domainRelations.get(key))));
    }

  }

  /**
   * @param args
   * @throws FileNotFoundException
   */
  public static void main(String[] args) throws IOException {
    String url;
    if (args.length == 0) {
      url = "jdbc:virtuoso://catzilla:1111";
    } else {
      url = args[0];
    }

    String username = "dba";
    String password = "dba";
    List<String> domainUris =
        Lists.newArrayList("http://business.freebase.com");
    String schemaFileName = "data/freebase/schema/business_schema.txt";

    PrintFreebaseDomain.print(url, username, password, domainUris,
        schemaFileName);
  }

}
