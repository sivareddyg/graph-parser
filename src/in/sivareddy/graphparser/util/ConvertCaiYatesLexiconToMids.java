package in.sivareddy.graphparser.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConvertCaiYatesLexiconToMids {
  String namespace = "http://rdf.freebase.com/ns/";
  private RdfGraphTools rdfGraphTools;

  public ConvertCaiYatesLexiconToMids(String url, String username,
      String password) {
    rdfGraphTools = new RdfGraphTools(url, username, password);
  }

  public String getMid(String entityKey, String domain) {
    if (entityKey.startsWith("/m/")) {
      return entityKey.split(":")[0].replace("/", ".").substring(1);
    } else if (entityKey.indexOf("/type/int") != -1) {
      return "type.int";
    } else if (entityKey.indexOf("/type/float") != -1) {
      return "type.float";
    } else if (entityKey.indexOf("/type/datetime") != -1) {
      return "type.datetime";
    }

    entityKey = entityKey.split(":")[0];
    String query =
        String.format("PREFIX ns: <http://rdf.freebase.com/ns/> PREFIX "
            + "rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "SELECT ?x FROM <%s> WHERE { ?x ns:type.object.key \"%s\" . }",
            domain, entityKey);
    System.err.println("Query = " + query);
    List<Map<String, String>> results = rdfGraphTools.runQueryHttpSolutions(query);
    List<String> resultStrings = new ArrayList<>();
    for (Map<String, String> result : results) {
      String x = result.get("x").toString();
      x = x.replace(namespace, "");
      resultStrings.add(x);
    }
    if (resultStrings.size() > 1) {
      System.err
          .println("warning: key matched more than one entity. Manually correct it");
    }
    // Preconditions.checkArgument(resultStrings.size() < 2,
    // "entity key matched none or more than one entity");
    return resultStrings.size() == 0 ? null : resultStrings.get(0);
  }

  /**
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    String endpoint = args[0];
    // String endpoint = "jdbc:virtuoso://darkstar:1111";
    String domainUrl = args[1];
    // String domainUrl = "http://film.freebase.com";
    ConvertCaiYatesLexiconToMids db =
        new ConvertCaiYatesLexiconToMids(endpoint, "dba", "dba");

    BufferedReader br =
        new BufferedReader(new FileReader(
            "data/cai-yates-2013/fixed-np-manually.txt"));
    try {
      String line = br.readLine();
      while (line != null) {
        String[] parts = line.split(" :- NP : ");
        String word = parts[0];
        String entityKey = parts[1];
        String mid = db.getMid(entityKey, domainUrl);
        if (mid != null) {
          System.out.println(word + " :- NP : " + mid);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }

  }
}
