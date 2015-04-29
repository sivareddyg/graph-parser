package in.sivareddy.graphparser.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import in.sivareddy.util.SentenceKeys;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DisambiguateEntities {

  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();

  public DisambiguateEntities() {

  }

  public static void chooseHighestFreebaseAPIScore(JsonObject sentence) {
    if (!sentence.has(SentenceKeys.MATCHED_ENTITIES))
      return;

    JsonObject highestScoringMatch = null;
    double highestScore = 0.0;
    String mid = null;
    for (JsonElement matchedEntity : sentence
        .get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
      JsonObject matchedEntityObj = matchedEntity.getAsJsonObject();
      if (matchedEntityObj.has(SentenceKeys.RANKED_ENTITIES)) {
        JsonObject rankedEntity =
            matchedEntityObj.get(SentenceKeys.RANKED_ENTITIES).getAsJsonArray()
                .get(0).getAsJsonObject();
        double score = rankedEntity.get("score").getAsDouble();
        if (score > highestScore) {
          highestScore = score;
          highestScoringMatch = matchedEntityObj;
          mid = rankedEntity.get(SentenceKeys.ENTITY).getAsString();
        }
      }
    }

    if (highestScoringMatch != null) {
      JsonArray finalEntities = new JsonArray();
      JsonObject entity =
          jsonParser.parse(gson.toJson(highestScoringMatch)).getAsJsonObject();
      entity.remove(SentenceKeys.ENTITIES);
      entity.remove(SentenceKeys.RANKED_ENTITIES);
      entity.addProperty(SentenceKeys.ENTITY, mid);
      finalEntities.add(entity);
      sentence.add(SentenceKeys.ENTITIES, finalEntities);
    }
  }

  public static void chooseNamedEntityHighestFreebaseAPIScore(
      JsonObject sentence) {
    if (!sentence.has(SentenceKeys.MATCHED_ENTITIES))
      return;

    JsonObject highestScoringMatch = null;
    double highestScore = 0.0;
    String mid = null;
    String name = null;

    JsonObject highestScoringMatchNER = null;
    double highestScoreNER = 0.0;
    String midNER = null;
    String nameNER = null;
    for (JsonElement matchedEntity : sentence
        .get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
      JsonObject matchedEntityObj = matchedEntity.getAsJsonObject();
      if (matchedEntityObj.has(SentenceKeys.RANKED_ENTITIES)) {
        JsonObject rankedEntity =
            matchedEntityObj.get(SentenceKeys.RANKED_ENTITIES).getAsJsonArray()
                .get(0).getAsJsonObject();

        double score = rankedEntity.get("score").getAsDouble();
        if (score > highestScore) {
          highestScore = score;
          highestScoringMatch = matchedEntityObj;
          mid = rankedEntity.get(SentenceKeys.ENTITY).getAsString();
          name = rankedEntity.get("name").getAsString();
        }

        if (score > highestScoreNER
            && hasNamedEntity(sentence.get(SentenceKeys.WORDS_KEY)
                .getAsJsonArray(), matchedEntityObj.get(SentenceKeys.START)
                .getAsInt(), matchedEntityObj.get(SentenceKeys.END).getAsInt())) {
          highestScoreNER = score;
          highestScoringMatchNER = matchedEntityObj;
          midNER = rankedEntity.get(SentenceKeys.ENTITY).getAsString();
          nameNER = rankedEntity.get("name").getAsString();
        }

      }
    }

    if (highestScoringMatchNER != null) {
      JsonArray finalEntities = new JsonArray();
      JsonObject entity =
          jsonParser.parse(gson.toJson(highestScoringMatchNER))
              .getAsJsonObject();
      entity.remove(SentenceKeys.ENTITIES);
      entity.remove(SentenceKeys.RANKED_ENTITIES);
      entity.addProperty(SentenceKeys.ENTITY, midNER);
      entity.addProperty("name", nameNER);
      finalEntities.add(entity);
      sentence.add(SentenceKeys.ENTITIES, finalEntities);
    } else if (highestScoringMatch != null) {
      JsonArray finalEntities = new JsonArray();
      JsonObject entity =
          jsonParser.parse(gson.toJson(highestScoringMatch)).getAsJsonObject();
      entity.remove(SentenceKeys.ENTITIES);
      entity.remove(SentenceKeys.RANKED_ENTITIES);
      entity.addProperty(SentenceKeys.ENTITY, mid);
      entity.addProperty("name", name);
      finalEntities.add(entity);
      sentence.add(SentenceKeys.ENTITIES, finalEntities);
    }
  }

  private static boolean hasNamedEntity(JsonArray words, int start, int end) {
    for (int i = start; i <= end; i++) {
      JsonObject wordObject = words.get(i).getAsJsonObject();
      if (wordObject.has(SentenceKeys.NER_KEY)
          && !wordObject.get(SentenceKeys.NER_KEY).getAsString().matches("O")) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] args) throws IOException {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        chooseNamedEntityHighestFreebaseAPIScore(sentence);
        sentence.remove(SentenceKeys.MATCHED_ENTITIES);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
