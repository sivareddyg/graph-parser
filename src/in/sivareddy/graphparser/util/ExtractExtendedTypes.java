package in.sivareddy.graphparser.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

public class ExtractExtendedTypes {

  private RdfGraphTools rdfGraphTools;
  public String ns = "http://rdf.freebase.com/ns/";
  private Set<String> entityTypes = Sets.newHashSet();

  public ExtractExtendedTypes(String jdbcEndPoint) {
    rdfGraphTools = new RdfGraphTools(jdbcEndPoint, "dba", "dba");
  }

  public void extractTypes(String query, Writer bw) throws IOException {
    ResultSet resultSet = rdfGraphTools.runQueryJdbcResultSet(query);
    while (resultSet.hasNext()) {
      QuerySolution answer = resultSet.next();
      // System.out.println(answer);
      String x = answer.get("x").toString();
      x = "ns:" + x.replace(ns, "");
      String y = answer.get("y").toString();
      y = "ns:" + y.replace(ns, "");
      String yname =
          answer.get("yname") != null ? answer.get("yname").toString() : "";
      yname = yname.replaceAll("[\\s]+", "_");
      yname = yname.replaceAll("[^A-Za-z0-9_]+", "");
      yname = yname.replaceAll("en$", "");
      if (!entityTypes.contains(yname)) {
        bw.write(String.format("%s\t%s\n", y, yname));
        entityTypes.add(yname);
      }

      System.out.println(String.format("%s rdf:type %s .", x, y));
    }
  }

  public static void main(String[] args) throws IOException {
    String fileNameWithQueries = args[0];
    String jdbcEndPoint = args[1];
    String entityDictionary = args[2];
    BufferedReader br = new BufferedReader(new FileReader(fileNameWithQueries));
    ExtractExtendedTypes extractor = new ExtractExtendedTypes(jdbcEndPoint);
    System.out.println("@prefix ns: <http://rdf.freebase.com/ns/>.");
    System.out
        .println("@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.");
    BufferedWriter bw = new BufferedWriter(new FileWriter(entityDictionary));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (line.equals("") || line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }
        extractor.extractTypes(line, bw);
        line = br.readLine();
      }
    } finally {
      br.close();
      bw.close();
    }
  }
}
