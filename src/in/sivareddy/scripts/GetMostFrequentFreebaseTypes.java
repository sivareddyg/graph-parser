package in.sivareddy.scripts;

import java.util.List;
import java.util.Map;

import in.sivareddy.graphparser.util.RdfGraphTools;

public class GetMostFrequentFreebaseTypes {

  public static void main(String[] args) {
    RdfGraphTools endPoint =
        new RdfGraphTools("jdbc:virtuoso://rockall:1111",
            "http://rockall:8890/sparql", "dba", "dba");
    String query =
        "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> select count(?type) as ?count ?type  where {select  ?type where {?entity fb:type.object.type ?type . ?entity fb:type.object.type fb:common.topic . FILTER (?type != fb:common.topic) } LIMIT 10000000 }  ORDER BY DESC(?count) LIMIT 50";
    List<Map<String, String>> results = endPoint.runQueryHttpSolutions(query);

    for (Map<String, String> result : results) {
      System.out.println(String.format("%s\t%s",
          result.get("type").split("/ns/")[1],
          result.get("count").split("\\^")[0]));
    }
  }
}
