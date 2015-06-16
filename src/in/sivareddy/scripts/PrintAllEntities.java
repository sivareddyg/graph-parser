package in.sivareddy.scripts;

import in.sivareddy.graphparser.util.RdfGraphTools;

import java.util.List;
import java.util.Map;

public class PrintAllEntities {
  RdfGraphTools endPoint = null;

  public PrintAllEntities(String jdbcEndPoint) {
    endPoint = new RdfGraphTools(jdbcEndPoint, "dba", "dba");
  }


  public void print() {
    String query =
        "SELECT ?s ?o FROM <http://rdf.freebase.com> WHERE { ?s ?p ?o . }";
    List<Map<String, String>> results = endPoint.runQueryJdbcSolutions(query);

    for (Map<String, String> result : results) {
      System.out.println(result.get("s"));
      System.out.println(result.get("o"));
    }
  }

  public static void main(String[] args) {
    PrintAllEntities printer =
        new PrintAllEntities("jdbc:virtuoso://localhost:1111");
    printer.print();
  }
}
