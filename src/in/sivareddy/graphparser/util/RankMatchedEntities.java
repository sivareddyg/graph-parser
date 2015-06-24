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

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RankMatchedEntities {
  public static String API_KEY = "AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg";
  public static String FREEBASE_ENDPOINT =
      "https://www.googleapis.com/freebase/v1/search";
  public static String charset = "UTF-8";
  public static JsonParser jsonParser = new JsonParser();

  public static LoadingCache<String, String> queryToResults = Caffeine
      .newBuilder().maximumSize(100000).build(x -> queryFreebaseAPIPrivate(x));

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
  public static void rankSpansUsingFreebaseAPI(JsonObject jsonSentence,
      boolean useMatchedEntities) throws IOException {
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


      JsonObject response = queryFreebaseAPI(query);
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

  private static JsonObject queryFreebaseAPI(String query) {
    String result = queryToResults.get(query);
    if (result != null)
      return jsonParser.parse(result).getAsJsonObject();
    return null;
  }

  private static String queryFreebaseAPIPrivate(String query) {
    try {
      String requestQuery =
          String.format("query=%s&key=%s", URLEncoder.encode(query, charset),
              API_KEY);

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

  public static void main(String[] args) throws IOException {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        rankSpansUsingFreebaseAPI(sentence, false);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
