package in.sivareddy.scripts;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

import in.sivareddy.graphparser.util.RdfGraphTools;

public class PrintAllEntities {
  RdfGraphTools endPoint = null;

  public PrintAllEntities(String jdbcEndPoint) {
    endPoint = new RdfGraphTools(jdbcEndPoint, "dba", "dba");
  }


  public void print() {
    String query =
        "SELECT ?s ?o FROM <http://rdf.freebase.com> WHERE { ?s ?p ?o . }";
    ResultSet results = endPoint.runQueryJdbcResultSet(query);

    while (results.hasNext()) {
      QuerySolution result = results.nextSolution();
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