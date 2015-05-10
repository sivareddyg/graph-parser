package in.sivareddy.graphparser.util;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.Pair;

import virtuoso.jdbc4.VirtuosoConnectionPoolDataSource;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * Tools for querying RDF Graphs
 *
 * @author Siva Reddy
 *
 */
public class RdfGraphTools {

  private VirtGraph virtGraph;
  private String httpUrl;
  private String jdbcUrl;
  private String username;
  private String password;
  private Integer timeOut = 500000; // timeout in milli seconds
  private static String XSD_PREFIX =
      "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";

  // stores query and its results
  private static Cache<String, Map<String, LinkedHashSet<String>>> queryCache =
      Caffeine.newBuilder().maximumSize(100000).build();

  public RdfGraphTools(String jdbcEndPoint, String username, String password) {
    this(jdbcEndPoint, null, username, password, 0);
  }

  public RdfGraphTools(String jdbcEndPoint, String username, String password,
      int timeOut) {
    this(jdbcEndPoint, null, username, password, timeOut);
  }

  public RdfGraphTools(String jdbcUrl, String httpUrl, String username,
      String password) {
    this(jdbcUrl, httpUrl, username, password, 0);
  }

  public RdfGraphTools(String jdbcUrl, String httpUrl, String username,
      String password, int timeOut) {
    virtGraph = new VirtGraph(null, jdbcUrl, username, password, true);

    if (timeOut > 0) {
      virtGraph.setQueryTimeout(timeOut / 1000);
      this.timeOut = timeOut;
    }

    PrefixMapping prefixMapping = virtGraph.getPrefixMapping();
    prefixMapping.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
    this.httpUrl = httpUrl;
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
  }

  public Pair<ResultSet, QueryExecution> runQueryJdbcResultSet(String query) {
    if (query == null) {
      return Pair.of(null, null);
    }

    ResultSet results = null;
    VirtuosoQueryExecution vqe = null;
    try {
      // Run Sparql query
      query = String.format("%s %s", XSD_PREFIX, query);
      Query sparql = QueryFactory.create(query);
      vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
      results = vqe.execSelect();
    } catch (QueryParseException e) {
      // System.err.println("WARNING: query parse exception: " + query);
      // Skip.
    } catch (Exception e) {
      // System.err.println("query timed out: " + e.getMessage());
      // Skip.
    }
    return Pair.of(results, vqe);
  }

  public static Pair<ResultSet, QueryExecution> runQueryJdbcResultSet(
      String query, VirtGraph virtGraph) {
    if (query == null) {
      return Pair.of(null, null);
    }

    ResultSet results = null;
    VirtuosoQueryExecution vqe = null;
    try {
      // Run Sparql query
      query = String.format("%s %s", XSD_PREFIX, query);
      Query sparql = QueryFactory.create(query);
      vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
      results = vqe.execSelect();
    } catch (QueryParseException e) {
      // System.err.println("WARNING: query parse exception: " + query);
      // Skip.
    } catch (Exception e) {
      // System.err.println("query timed out: " + e.getMessage());
      // Skip.
    }
    return Pair.of(results, vqe);
  }


  public Pair<ResultSet, QueryExecution> runQueryHttpResultSet(String query) {
    Preconditions.checkArgument(httpUrl != null, "http endpoint not specified");
    if (query == null) {
      return Pair.of(null, null);
    }
    query = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " + query;

    ResultSet results = null;
    QueryEngineHTTP vqe = null;
    try {
      vqe = new QueryEngineHTTP(httpUrl, query);
      vqe.addParam("timeout", this.timeOut.toString());
      results = vqe.execSelect();
    } catch (Exception e) {
      // Skip.
    }
    return Pair.of(results, vqe);
  }

  public static synchronized void cacheResult(String query,
      Map<String, LinkedHashSet<String>> results) {
    if (results != null) {
      queryCache.put(query, results);
    } else {
      queryCache.put(query, new HashMap<>());
    }
  }

  public Map<String, LinkedHashSet<String>> runQueryJdbc(String query) {
    if (query == null) {
      return null;
    }

    Map<String, LinkedHashSet<String>> results = queryCache.getIfPresent(query);
    if (results != null)
      return results;

    VirtGraph virtGraph = null;
    while (virtGraph == null) {
      try {
      virtGraph = new VirtGraph(null, jdbcUrl, username, password, true);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (timeOut > 0) {
      virtGraph.setQueryTimeout(timeOut / 1000);
    }
    
    Pair<ResultSet, QueryExecution> resultSetPair =
        runQueryJdbcResultSet(query, virtGraph);
    results = getResults(resultSetPair.getLeft());
    cacheResult(query, results);
    close(resultSetPair);
    virtGraph.close();
    return results;
  }

  public Map<String, LinkedHashSet<String>> runQueryHttp(String query) {
    if (query == null) {
      return null;
    }

    Map<String, LinkedHashSet<String>> results = queryCache.getIfPresent(query);
    if (results != null)
      return results;

    Pair<ResultSet, QueryExecution> resultSetPair =
        runQueryHttpResultSet(query);

    results = getResults(resultSetPair.getLeft());
    cacheResult(query, results);
    close(resultSetPair);
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
   * Returns readable outuput of gold and predicted results that can be used for
   * evaluation with Berant's script.
   * 
   * @param goldResults
   * @param predResults
   * @return
   */
  public static Pair<Set<String>, Set<String>> getCleanedResults(
      Map<String, LinkedHashSet<String>> goldResults,
      Map<String, LinkedHashSet<String>> predResults) {

    Preconditions.checkArgument(goldResults != null,
        "Gold results should not be null");
    Preconditions.checkArgument(goldResults.keySet().size() <= 2,
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

    boolean hasDate = false;
    LinkedHashSet<String> goldAnswers = goldResults.get(goldVarName);
    if (goldVarName != null && goldVarName.equals("targetValue")) {
      hasDate =
          (goldAnswers.size() > 0 && goldAnswers.iterator().next()
              .contains("XMLSchema#datetime")) ? true : false;
      if (hasDate) {
        goldAnswers = convertDatesToYears(goldAnswers);
      }
    } else {
      goldAnswers = goldResults.get(goldVar);
    }

    if (predResults == null || predResults.size() == 0)
      return Pair.of(goldAnswers, new LinkedHashSet<>());

    Preconditions.checkArgument(predResults.keySet().size() <= 2,
        "Unknown target variable");
    String predVar = null;
    String predVarName = null;
    for (String key : predResults.keySet()) {
      if (!key.contains("name")) {
        predVar = key;
      } else if (predResults.get(key).size() > 0) {
        predVarName = key;
      }
    }

    LinkedHashSet<String> predAnswersCleaned = new LinkedHashSet<>();
    if (goldVarName != null && goldVarName.equals("targetValue")) {
      LinkedHashSet<String> predAnswers =
          predVarName != null ? predResults.get(predVarName) : predResults
              .get(predVar);

      for (String predAnswer : predAnswers) {
        predAnswer = predAnswer.split("\\^\\^")[0];
        predAnswer = predAnswer.replaceAll("@[a-zA-Z\\-]+$", "");
        if (hasDate) {
          Matcher matcher = Pattern.compile("([0-9]{3,4})").matcher(predAnswer);
          if (matcher.find()) {
            predAnswer = matcher.group(1);
          }
        }
        predAnswersCleaned.add(predAnswer);
      }
    } else if (goldVar.equals("answerSubset") || goldVar.equals("answer")) {
      LinkedHashSet<String> predAnswers = predResults.get(predVar);
      for (String predAnswer : predAnswers) {
        boolean answerIsDate = predAnswer.contains("XMLSchema#datetime");
        predAnswer = predAnswer.split("\\^\\^")[0];
        String[] answers = predAnswer.split("/");
        predAnswer = answers[answers.length - 1];
        if (answerIsDate) {
          Matcher matcher = Pattern.compile("([0-9]{3,4})").matcher(predAnswer);
          if (matcher.find()) {
            predAnswer = matcher.group(1);
          }
        }
        predAnswersCleaned.add(predAnswer);
      }
    } else {
      predAnswersCleaned = predResults.get(predVar);
    }
    return Pair.of(goldAnswers, predAnswersCleaned);
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
    if (predResults == null || predResults.size() == 0) {
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
    } else if (goldVar.equals("answerSubset") || goldVar.equals("answer")) {
      // If the gold answers are subset of the predicted answers, return true.
      HashSet<String> predAnswersCleaned = new HashSet<>();
      LinkedHashSet<String> predAnswers = predResults.get(predVar);
      for (String predAnswer : predAnswers) {
        boolean answerIsDate = predAnswer.contains("XMLSchema#datetime");
        predAnswer = predAnswer.split("\\^\\^")[0];
        String[] answers = predAnswer.split("/");
        predAnswer = answers[answers.length - 1];
        if (answerIsDate) {
          Matcher matcher = Pattern.compile("([0-9]{3,4})").matcher(predAnswer);
          if (matcher.find()) {
            predAnswer = matcher.group(1);
          }
        }
        predAnswersCleaned.add(predAnswer);
      }
      if (goldVar.equals("answerSubset"))
        return predAnswersCleaned.containsAll(goldResults.get(goldVar));
      else
        return predAnswersCleaned.equals(goldResults.get(goldVar));
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



    for (int i = 0; i < 200; i++) {
      VirtGraph virtGraph =
          new VirtGraph(null, "jdbc:virtuoso://bravas:1111", "dba", "dba", true);
      System.out.println(i);
      virtGraph.setQueryTimeout(20);
      String query =
          "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?x2 from <http://rdf.freebase.com> WHERE { ?x ?y ?x2 .  } LIMIT 100";

      System.out.println(getResults(runQueryJdbcResultSet(query, virtGraph)
          .getLeft()));
    }

    String httpUrl = "http://kinloch:8890/sparql";

    String query =
        "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?x2 from <http://rdf.freebase.com> WHERE { ?m1 fb:business.company_brand_relationship.brand fb:m.04wg3q . ?m1 fb:business.company_brand_relationship.from_date ?x2 .  } LIMIT 10";

    // String query =
    // "SELECT * FROM <http://film.freebase.com> WHERE { ?s ?p ?o . } limit 100";

    RdfGraphTools rdfGraphTools = new RdfGraphTools(url, httpUrl, "dba", "dba");

    long startTime = System.currentTimeMillis();
    // ResultSet results = rdfGraphTools.runQueryJdbc(query);
    Pair<ResultSet, QueryExecution> resultsPair =
        rdfGraphTools.runQueryHttpResultSet(query);
    ResultSet results = resultsPair.getLeft();
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

  public static boolean close(Pair<ResultSet, QueryExecution> resultSetPair) {
    if (resultSetPair.getRight() != null) {
      resultSetPair.getRight().close();
      return true;
    }
    return false;
  }
}
