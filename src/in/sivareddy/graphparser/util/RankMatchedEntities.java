package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RankMatchedEntities {
  private static String API_KEY = "AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg";
  private static String FREEBASE_ENDPOINT =
      "https://www.googleapis.com/freebase/v1/search";
  private static String KNOWLEDGE_GRAPH_ENDPOINT =
      "https://kgsearch.googleapis.com/v1/entities:search";
  private static String charset = "UTF-8";
  private static JsonParser jsonParser = new JsonParser();
  private static int ENTITY_LIMIT = 10;

  private LoadingCache<Pair<String, String>, String> queryToResults = Caffeine
      .newBuilder().maximumSize(100000)
      .build(x -> queryFreebaseAPIPrivate(x.getLeft(), x.getRight()));
  private LoadingCache<Pair<String, String>, String> queryToKGResults =
      Caffeine.newBuilder().maximumSize(100000)
          .build(x -> queryKnowledgeGraphAPIPrivate(x.getLeft(), x.getRight()));

  public RankMatchedEntities() {
    disableCertificateValidation();
  }

  public RankMatchedEntities(String apiKey) {
    this();
    API_KEY = apiKey;
  }

  /**
   * Create a trust manager that does not validate certificate chains. Not safe
   * for production. Source: http://stackoverflow
   * .com/questions/875467/java-client-certificates-over-https-ssl/876785#876785
   */
  public static void disableCertificateValidation() {
    //
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {}

      public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }};

    // Ignore differences between given hostname and certificate hostname
    HostnameVerifier hv = new HostnameVerifier() {
      public boolean verify(String hostname, SSLSession session) {
        return true;
      }
    };

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(hv);
    } catch (Exception e) {
    }
  }

  /**
   * Annotate each span using Freebase API. Additional information from the API
   * call is also stored.
   * 
   * @param jsonSentence
   * @param useMatchedEntities if set true, only the entities that are chosen
   *        both by API and already matched entities, are used for final
   *        ranking.
   * @throws IOException
   */
  public void rankSpansUsingKnowledgeGraphAPI(JsonObject jsonSentence,
      String languageCode, boolean useMatchedEntities) throws IOException {
    if (!jsonSentence.has(SentenceKeys.MATCHED_ENTITIES))
      return;

    for (JsonElement entityMatched : jsonSentence.get(
        SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
      JsonObject entityObject = entityMatched.getAsJsonObject();
      String query =
          entityObject.has(SentenceKeys.PHRASE) ? entityObject.get(
              SentenceKeys.PHRASE).getAsString() : EntityAnnotator.getPhrase(
              jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray(),
              jsonSentence.get(SentenceKeys.START).getAsInt(), jsonSentence
                  .get(SentenceKeys.END).getAsInt());

      Set<String> matchedEntitySet = new HashSet<>();
      if (useMatchedEntities) {
        entityObject.get(SentenceKeys.ENTITIES).getAsJsonArray()
            .forEach(x -> matchedEntitySet.add(x.getAsString()));
      }


      JsonObject response = queryKnowledgeGraphAPI(query, languageCode);
      JsonArray rankedEntities = new JsonArray();
      if (response != null && response.has("itemListElement"))
        for (JsonElement result : response.get("itemListElement")
            .getAsJsonArray()) {
          JsonObject resultObject =
              result.getAsJsonObject().get("result").getAsJsonObject();

          String mid =
              resultObject.get("@id").getAsString().replaceFirst("kg:", "")
                  .replaceFirst("/", "").replaceAll("/", ".");
          resultObject.remove("@id");

          resultObject
              .add("score", result.getAsJsonObject().get("resultScore"));
          if (resultObject.has("description"))
            resultObject.remove("description");
          if (resultObject.has("url"))
            resultObject.remove("url");
          if (resultObject.has("image"))
            resultObject.remove("image");
          if (resultObject.has("detailedDescription"))
            resultObject.remove("detailedDescription");

          if (useMatchedEntities) {
            if (matchedEntitySet.contains(mid)) {
              resultObject.addProperty(SentenceKeys.ENTITY, mid);
              rankedEntities.add(resultObject);
            }
          } else {
            resultObject.remove("mid");
            resultObject.addProperty(SentenceKeys.ENTITY, mid);
            rankedEntities.add(resultObject);
          }
        }
      if (rankedEntities.size() > 0) {
        entityObject.add(SentenceKeys.RANKED_ENTITIES, rankedEntities);
      }
    }
  }

  /**
   * Annotate each span using Freebase API. Additional information from the API
   * call is also stored.
   * 
   * @param jsonSentence
   * @param useMatchedEntities if set true, only the entities that are chosen
   *        both by API and already matched entities, are used for final
   *        ranking.
   * @throws IOException
   */
  public void rankSpansUsingFreebaseAPI(JsonObject jsonSentence,
      String languageCode, boolean useMatchedEntities) throws IOException {
    if (!jsonSentence.has(SentenceKeys.MATCHED_ENTITIES))
      return;

    for (JsonElement entityMatched : jsonSentence.get(
        SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
      JsonObject entityObject = entityMatched.getAsJsonObject();
      String query =
          entityObject.has(SentenceKeys.PHRASE) ? entityObject.get(
              SentenceKeys.PHRASE).getAsString() : EntityAnnotator.getPhrase(
              jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray(),
              jsonSentence.get(SentenceKeys.START).getAsInt(), jsonSentence
                  .get(SentenceKeys.END).getAsInt());

      Set<String> matchedEntitySet = new HashSet<>();
      if (useMatchedEntities) {
        entityObject.get(SentenceKeys.ENTITIES).getAsJsonArray()
            .forEach(x -> matchedEntitySet.add(x.getAsString()));
      }


      JsonObject response = queryFreebaseAPI(query, languageCode);
      JsonArray rankedEntities = new JsonArray();
      for (JsonElement result : response.get("result").getAsJsonArray()) {
        JsonObject resultObject = result.getAsJsonObject();
        String mid =
            resultObject.get("mid").getAsString().replaceFirst("/", "")
                .replaceAll("/", ".");
        if (useMatchedEntities) {
          if (matchedEntitySet.contains(mid)) {
            resultObject.remove("mid");
            resultObject.addProperty(SentenceKeys.ENTITY, mid);
            rankedEntities.add(resultObject);
          }
        } else {
          resultObject.remove("mid");
          resultObject.addProperty(SentenceKeys.ENTITY, mid);
          rankedEntities.add(resultObject);
        }
      }
      if (rankedEntities.size() > 0) {
        entityObject.add(SentenceKeys.RANKED_ENTITIES, rankedEntities);
      }
    }
  }

  protected JsonObject queryFreebaseAPI(String query, String languageCode) {
    String result = queryToResults.get(Pair.of(query, languageCode));
    if (result != null)
      return jsonParser.parse(result).getAsJsonObject();
    return null;
  }

  protected JsonObject queryKnowledgeGraphAPI(String query, String languageCode) {
    String result = queryToKGResults.get(Pair.of(query, languageCode));
    if (result != null)
      return jsonParser.parse(result).getAsJsonObject();
    return null;
  }

  private String queryFreebaseAPIPrivate(String query, String languageCode) {
    try {
      String requestQuery =
          String.format("query=%s&key=%s&lang=%s",
              URLEncoder.encode(query, charset), API_KEY, languageCode);

      URL url = new URL(FREEBASE_ENDPOINT + "?" + requestQuery);
      HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

      connection.setRequestProperty("Accept-Charset", charset);
      InputStream responseRecieved = connection.getInputStream();
      return IOUtils.toString(responseRecieved, charset);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String queryKnowledgeGraphAPIPrivate(String query, String languageCode) {
    try {
      String requestQuery =
          String.format("query=%s&key=%s&languages=%s&limit=%d",
              URLEncoder.encode(query, charset), API_KEY, languageCode,
              ENTITY_LIMIT);

      URL url = new URL(KNOWLEDGE_GRAPH_ENDPOINT + "?" + requestQuery);
      HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

      connection.setRequestProperty("Accept-Charset", charset);
      InputStream responseRecieved = connection.getInputStream();
      
      try {
        // 1000 milliseconds is one second. 10 milliseconds delay is equal to
        // 100 queries per second, and 10000 queries per 100 milliseconds.
        Thread.sleep(50);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      
      return IOUtils.toString(responseRecieved, charset);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void main(String[] args) throws IOException {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();
    RankMatchedEntities ranker = new RankMatchedEntities();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();

        ranker.rankSpansUsingFreebaseAPI(sentence, "en", false);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
