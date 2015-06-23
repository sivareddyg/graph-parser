package in.sivareddy.graphparser.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class InsertTriplesIntoGraph {

  private static void insert(String jdbcEndPoint, String domainGraphUri,
      String triplesFileName) throws IOException {
    RdfGraphTools rdfGraph = new RdfGraphTools(jdbcEndPoint, "dba", "dba");

    BufferedReader br = new BufferedReader(new FileReader(triplesFileName));
    try {
      String line = br.readLine();
      while (line != null) {
        if (line.equals("") || line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }
        String[] triple = line.split("\t", 3);
        rdfGraph.insertIntoGraph(domainGraphUri, triple[0], triple[1],
            triple[2]);
        // rdfGraph.deleteFromGraph(domainGraphUri, triple[0], triple[1],
        // triple[2]);
        line = br.readLine();
      }
    } finally {
      br.close();
    }

  }

  public static void main(String[] args) throws IOException {
    String jdbcEndPoint = args[0];
    String domainGraphUri = args[1];
    String triplesFileName = args[2];
    insert(jdbcEndPoint, domainGraphUri, triplesFileName);
  }

}
