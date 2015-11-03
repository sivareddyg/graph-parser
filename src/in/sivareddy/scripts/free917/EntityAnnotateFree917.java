package in.sivareddy.scripts.free917;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.util.EntityAnnotator;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

public class EntityAnnotateFree917 extends ProcessStreamInterface {
  private final EntityAnnotator entityAnnotator;

  private final Gson gson = new Gson();
  private final JsonParser jsonParser = new JsonParser();

  public EntityAnnotateFree917(String entityMappings)
      throws FileNotFoundException, IOException {
    entityAnnotator =
        new EntityAnnotator(new FileReader(entityMappings), false, false);
  }

  @Override
  public void processSentence(JsonObject sentence) {
    entityAnnotator.maximalMatch(sentence);
    JsonArray disambiguatedEntities = new JsonArray();
    if (sentence.has(SentenceKeys.MATCHED_ENTITIES)) {
      for (JsonElement matchElm : sentence.get(SentenceKeys.MATCHED_ENTITIES)
          .getAsJsonArray()) {
        if (disambiguatedEntities.size() == 0)
          disambiguatedEntities.add(new JsonArray());

        JsonObject match = matchElm.getAsJsonObject();
        JsonArray newDisambiguatedEntities = new JsonArray();
        for (JsonElement disambiguatedEntityElm : disambiguatedEntities) {
          for (JsonElement entityElm : match.get(SentenceKeys.ENTITIES)
              .getAsJsonArray()) {
            JsonObject entity =
                jsonParser.parse(gson.toJson(match)).getAsJsonObject();
            entity.remove(SentenceKeys.ENTITIES);
            entity.add(SentenceKeys.ENTITY, entityElm);
            entity.addProperty(SentenceKeys.SCORE, 1.0);
            JsonArray newDisambiguatedEntityList =
                jsonParser.parse(gson.toJson(disambiguatedEntityElm))
                    .getAsJsonArray();
            newDisambiguatedEntityList.add(entity);
            newDisambiguatedEntities.add(newDisambiguatedEntityList);
          }
        }
        newDisambiguatedEntities.addAll(disambiguatedEntities);
        disambiguatedEntities = newDisambiguatedEntities;
      }
    }

    if (disambiguatedEntities.size() > 0) {
      JsonArray finalEntities = new JsonArray();
      for (JsonElement elm : disambiguatedEntities) {
        if (elm.getAsJsonArray().size() > 0) {
          JsonObject entitiesObj = new JsonObject();
          entitiesObj.add(SentenceKeys.ENTITIES, elm);
          entitiesObj.addProperty(SentenceKeys.SCORE, elm.getAsJsonArray()
              .size());
          finalEntities.add(entitiesObj);
        }
      }
      if (finalEntities.size() > 0)
        sentence.add(SentenceKeys.DISAMBIGUATED_ENTITIES, finalEntities);
    }
  }

  public static void main(String[] args) throws FileNotFoundException,
      IOException, InterruptedException {
    EntityAnnotateFree917 engine = new EntityAnnotateFree917(args[0]);
    engine.processStream(System.in, System.out, 1, true);
  }
}
