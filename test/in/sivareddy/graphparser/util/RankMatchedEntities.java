package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;

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

  public static void rankSpansUsingFreebaseAPI(JsonObject jsonSentence)
      throws IOException {
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
      entityObject.get(SentenceKeys.ENTITIES).getAsJsonArray()
          .forEach(x -> matchedEntitySet.add(x.getAsString()));


      JsonObject response = queryFreebaseAPI(query);
      JsonArray rankedEntities = new JsonArray();
      for (JsonElement result : response.get("result").getAsJsonArray()) {
        JsonObject resultObject = result.getAsJsonObject();
        String mid =
            resultObject.get("mid").getAsString().replaceFirst("/", "")
                .replaceAll("/", ".");
        if (matchedEntitySet.contains(mid)) {
          resultObject.remove("mid");
          resultObject.addProperty(SentenceKeys.ENTITY, mid);
          rankedEntities.add(resultObject);
        }
      }
      if (rankedEntities.size() > 0) {
        entityObject.add("rankedEntities", rankedEntities);
      }
    }
  }

  private static JsonObject queryFreebaseAPI(String query) throws IOException {
    String requestQuery =
        String.format("query=%s&key=%s", URLEncoder.encode(query, charset),
            API_KEY);

    URL url = new URL(FREEBASE_ENDPOINT + "?" + requestQuery);
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    
    connection.setRequestProperty("Accept-Charset", charset);
    InputStream responseRecieved = connection.getInputStream();
    JsonObject response =
        jsonParser.parse(IOUtils.toString(responseRecieved, charset))
            .getAsJsonObject();
    return response;
  }


  public static void main(String[] args) throws IOException {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        rankSpansUsingFreebaseAPI(sentence);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
