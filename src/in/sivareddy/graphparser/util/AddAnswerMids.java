package in.sivareddy.graphparser.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hp.hpl.jena.query.ResultSet;

public class AddAnswerMids {
  public static void main(String[] args) throws IOException {
    String httpUrl = "http://kinloch:8890/sparql";
    String url = "jdbc:virtuoso://kinloch:1111";
    RdfGraphTools server = new RdfGraphTools(url, httpUrl, "dba", "dba", 3000);    
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      JsonParser jsonParser = new JsonParser();
      Gson gson = new Gson();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        String query = sentence.get("sparqlQuery").getAsString();
        // System.out.println(query);
        ResultSet results = server.runQueryHttpResultSet(query);
        Map<String, LinkedHashSet<String>> resultMap = RdfGraphTools.getResults(results);
        for (String key : resultMap.keySet()) {
          if (!key.contains("name")) {
            // System.out.println(resultMap.get(key));
            sentence.add("answer", jsonParser.parse(gson.toJson(resultMap.get(key))));
          }
        }
        sentence.remove("synPars");
        sentence.remove("sparqlQuery");
        sentence.remove("originalFormula");
        sentence.remove("targetFormula");
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
