package in.sivareddy.graphparser.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import virtuoso.jena.driver.*;

/**
 * Tools for querying RDF Graphs
 *
 * @author Siva Reddy
 *
 */
public class RdfGraphTools {

  private VirtGraph virtGraph;
  private String httpUrl;
  private Integer timeOut = 500000;

  // stores query and its results
  private static Map<String, Map<String, LinkedHashSet<String>>> queryCache =
      Maps.newHashMap();

  public RdfGraphTools(String jdbcEndPoint, String username, String password) {
    this(jdbcEndPoint, username, password, 0);
  }

  public RdfGraphTools(String jdbcEndPoint, String username, String password,
      int timeOut) {
    // virtGraph = new VirtGraph(jdbcEndPoint, "dba", "dba");
    virtGraph = new VirtGraph(null, jdbcEndPoint, "dba", "dba", true);
    if (timeOut > 0) {
      virtGraph.setQueryTimeout(timeOut);
      this.timeOut = timeOut;
    }
    PrefixMapping prefixMapping = virtGraph.getPrefixMapping();
    prefixMapping.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixMapping.setNsPrefix("rdf",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
  }

  public RdfGraphTools(String jdbcUrl, String httpUrl, String username,
      String password) {
    this(jdbcUrl, httpUrl, username, password, 0);
  }

  public RdfGraphTools(String jdbcUrl, String httpUrl, String username,
      String password, int timeOut) {
    // virtGraph = new VirtGraph(jdbcUrl, "dba", "dba");
    virtGraph = new VirtGraph(null, jdbcUrl, "dba", "dba", true);
    if (timeOut > 0) {
      virtGraph.setQueryTimeout(timeOut);
      this.timeOut = timeOut;
    }
    PrefixMapping prefixMapping = virtGraph.getPrefixMapping();
    prefixMapping.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    prefixMapping.setNsPrefix("rdf",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    this.httpUrl = httpUrl;
  }

  public ResultSet runQueryJdbcResultSet(String query) {
    if (query == null) {
      return null;
    }
    query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + query;
    // Create Sparql query
    ResultSet results = null;
    try {
      Query sparql = QueryFactory.create(query);
      // Run Sparql query
      VirtuosoQueryExecution vqe =
          VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
      results = vqe.execSelect();
    } catch (QueryParseException e) {
      System.err.println("Query parse exception: Using http endpoint instead");
      if (httpUrl != null) {
        return runQueryHttpResultSet(query);
      }
    } catch (Exception e) {
      System.err.println("query timed out: " + e.getMessage());
    }
    return results;
  }


  public ResultSet runQueryHttpResultSet(String query) {
    Preconditions.checkArgument(httpUrl != null, "http endpoint not specified");
    if (query == null) {
      return null;
    }
    query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + query;
    // Create Sparql query
    ResultSet results = null;
    try {
      QueryEngineHTTP vqe = new QueryEngineHTTP(httpUrl, query);
      vqe.addParam("timeout", this.timeOut.toString());
      results = vqe.execSelect();
    } catch (Exception e) {
      System.err.println("http exception: " + e.getMessage());
    }
    return results;
  }

  public static synchronized void cacheResult(String query,
      Map<String, LinkedHashSet<String>> results) {
    queryCache.put(query, results);
  }

  public Map<String, LinkedHashSet<String>> runQueryJdbc(String query) {
    if (query == null) {
      return null;
    }
    if (queryCache.containsKey(query)) {
      return queryCache.get(query);
    }
    ResultSet resultSet = runQueryJdbcResultSet(query);
    Map<String, LinkedHashSet<String>> results = getResults(resultSet);
    cacheResult(query, results);
    // queryCache.put(query, results);
    return results;
  }

  public Map<String, LinkedHashSet<String>> runQueryHttp(String query) {
    if (query == null) {
      return null;
    }
    if (queryCache.containsKey(query)) {
      return queryCache.get(query);
    }
    ResultSet resultSet = runQueryHttpResultSet(query);
    Map<String, LinkedHashSet<String>> results = getResults(resultSet);
    cacheResult(query, results);
    // queryCache.put(query, results);
    return results;
  }

  public void insertIntoGraph(String graphURI, String s, String p, String o) {
    s = s.trim();
    p = p.trim();
    o = o.trim();
    String query =
        String
            .format(
                "INSERT INTO GRAPH <%s> { %s %s %s . } WHERE { FILTER NOT EXISTS {  %s %s %s . } }",
                graphURI, s, p, o, s, p, o);
    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, virtGraph);
    vur.exec();
  }

  public void deleteFromGraph(String graphURI, String s, String p, String o) {
    s = s.trim();
    p = p.trim();
    o = o.trim();
    String query =
        String
            .format(
                "DELETE FROM GRAPH <%s> { %s %s %s . } WHERE { FILTER EXISTS {  %s %s %s . } }",
                graphURI, s, p, o, s, p, o);
    VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, virtGraph);
    vur.exec();
  }

  public static Map<String, LinkedHashSet<String>> getResults(
      ResultSet resultSet) {
    if (resultSet == null) {
      return null;
    }
    Map<String, LinkedHashSet<String>> results = Maps.newHashMap();
    List<?> vars = resultSet.getResultVars();
    for (Object var : vars) {
      results.put(var.toString(), new LinkedHashSet<String>());
    }
    try {
      while (resultSet.hasNext()) {
        QuerySolution result = resultSet.nextSolution();
        for (Object var : vars) {
          String varString = var.toString();
          RDFNode value = result.get(varString);
          if (value != null) {
            results.get(varString).add(value.toString());
          }
        }
      }
    } catch (Exception e) {
      return results;
    }
    return results;
  }

  /**
   * Useful in the case of questions with single variable
   *
   * @param goldResults
   * @param predResults
   * @return
   */
  public static boolean equalResults(
      Map<String, LinkedHashSet<String>> goldResults,
      Map<String, LinkedHashSet<String>> predResults) {
    Preconditions.checkArgument(goldResults != null,
        "Gold results should not be null");
    if (predResults == null) {
      return false;
    }

    Preconditions.checkArgument(goldResults.keySet().size() <= 2,
        "Unknown target variable");
    Preconditions.checkArgument(predResults.keySet().size() <= 2,
        "Unknown target variable");

    String goldVar = null;
    String goldVarName = null;
    for (String key : goldResults.keySet()) {
      if (key.equals("targetValue")) {
        goldVarName = key;
      } else if (!key.contains("name")) {
        goldVar = key;
      }
    }

    String predVar = null;
    String predVarName = null;
    for (String key : predResults.keySet()) {
      if (!key.contains("name")) {
        predVar = key;
      } else if (predResults.get(key).size() > 0) {
        predVarName = key;
      }
    }

    // Preconditions.checkArgument(goldVar != null && predVar != null,
    // "No target variable");
    if (goldVarName != null && goldVarName.equals("targetValue")) {
      LinkedHashSet<String> goldAnswers = goldResults.get(goldVarName);

      boolean hasDate =
          (goldAnswers.size() > 0 && goldAnswers.iterator().next()
              .contains("XMLSchema#datetime")) ? true : false;
      if (hasDate) {
        goldAnswers = convertDatesToYears(goldAnswers);
      }

      LinkedHashSet<String> predAnswers =
          predVarName != null ? predResults.get(predVarName) : predResults
              .get(predVar);

      LinkedHashSet<String> predAnswersCleaned = new LinkedHashSet<>();
      for (String predAnswer : predAnswers) {
        predAnswer = predAnswer.split("\\^\\^")[0];
        predAnswer = predAnswer.replaceAll("@[a-zA-Z\\-]+$", "");
        if (hasDate) {
          Matcher matcher = Pattern.compile("([0-9]{3,4})").matcher(predAnswer);
          if (matcher.find()) {
            predAnswer = matcher.group(1);
          }
        }
        if (!goldAnswers.contains(predAnswer)) {
          return false;
        }
        predAnswersCleaned.add(predAnswer);
      }

      if (predAnswersCleaned.size() != goldAnswers.size()) {
        return false;
      }
      return predAnswersCleaned.equals(goldAnswers);
    } else {
      return goldResults.get(goldVar).equals(predResults.get(predVar));
    }
  }

  public static void main(String[] args) {
    String url;
    if (args.length == 0) {
      url = "jdbc:virtuoso://kinloch:1111";
    } else {
      url = args[0];
    }

    String httpUrl = "http://kinloch:8890/sparql";

    // String query =
    // "SELECT * FROM <http://film.freebase.com> WHERE { ?s ?p ?o . } limit 100";
    String query =
        "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?x2 from <http://business.freebase.com> WHERE { ?m1 fb:business.company_brand_relationship.brand fb:m.04wg3q . ?m1 fb:business.company_brand_relationship.from_date ?x2 .  } LIMIT 10";

    RdfGraphTools rdfGraphTools = new RdfGraphTools(url, httpUrl, "dba", "dba");

    long startTime = System.currentTimeMillis();
    // ResultSet results = rdfGraphTools.runQueryJdbc(query);
    ResultSet results = rdfGraphTools.runQueryHttpResultSet(query);
    // System.out.println(RdfGraphTools.getResults(results));
    while (results.hasNext()) {
      QuerySolution result = results.nextSolution();
      System.out.println(results.getResultVars());
      System.out.println(result);
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    System.out.println(elapsedTime);
  }

  public static LinkedHashSet<String> convertDatesToYears(Set<String> results) {
    if (results == null) {
      return null;
    }
    LinkedHashSet<String> dates = Sets.newLinkedHashSet();
    for (String result : results) {
      // date = 2008-12-31^^http://www.w3.org/2001/XMLSchema#datetime
      if (result.contains("XMLSchema#datetime")) {
        String date = Splitter.on("^^").split(result).iterator().next();
        date = Splitter.on("-").split(date).iterator().next();
        dates.add(date);
      }
    }
    return dates;
  }

}
