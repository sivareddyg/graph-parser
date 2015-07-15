package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SplitForrestToSentences {
  private static final Gson gson = new Gson();
  private static final JsonParser jsonParser = new JsonParser();

  public static List<JsonObject> split(String jsonSentenceString) {
    MessageDigest md = null;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    List<JsonObject> sentences = new ArrayList<>();
    JsonObject jsonSentence =
        jsonParser.parse(jsonSentenceString).getAsJsonObject();
    if (jsonSentence.has(SentenceKeys.MATCHED_ENTITIES)) {
      jsonSentence.remove(SentenceKeys.MATCHED_ENTITIES);
    }
    if (jsonSentence.has(SentenceKeys.RANKED_ENTITIES)) {
      jsonSentence.remove(SentenceKeys.RANKED_ENTITIES);
    }

    try {
      String index =
          new String(Hex.encodeHex(md.digest(jsonSentence
              .get(SentenceKeys.SENTENCE_KEY).getAsString().getBytes("UTF-8"))));
      jsonSentence.addProperty(SentenceKeys.INDEX_KEY, index);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    if (!jsonSentence.has(SentenceKeys.DISAMBIGUATED_ENTITIES)) {
      sentences.add(jsonSentence);
      return sentences;
    }

    JsonArray disambiguatedEntities =
        jsonSentence.get(SentenceKeys.DISAMBIGUATED_ENTITIES).getAsJsonArray();
    jsonSentence.remove(SentenceKeys.DISAMBIGUATED_ENTITIES);

    for (JsonElement entities : disambiguatedEntities) {
      JsonElement entitiesList =
          entities.getAsJsonObject().get(SentenceKeys.ENTITIES);
      JsonObject newJsonSentence =
          jsonParser.parse(gson.toJson(jsonSentence)).getAsJsonObject();
      newJsonSentence.add(SentenceKeys.ENTITIES, entitiesList);
      sentences.add(newJsonSentence);
    }
    return sentences;
  }

  public static void main(String[] args) throws IOException {
    Gson gson = new Gson();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        for (JsonObject sentence : split(line)) {
          System.out.println(gson.toJson(sentence));
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
