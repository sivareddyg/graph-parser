package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MergeEntity {

  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  public static String PROPER_NOUN_TAG = "NNP";
  public static String NON_NAMED_ENTITY = "O";

  public static JsonObject mergeEntityWordsToSingleWord(String sentence) {
    JsonObject sentenceObj = jsonParser.parse(sentence).getAsJsonObject();
    JsonObject newSentence = jsonParser.parse(sentence).getAsJsonObject();
    if (!sentenceObj.has(SentenceKeys.ENTITIES))
      return newSentence;

    JsonArray oldWords =
        sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    if (oldWords.size() > 0) {
      JsonObject oldWordObj = oldWords.get(0).getAsJsonObject();
      String oldWord = oldWordObj.get(SentenceKeys.WORD_KEY).getAsString();
      oldWord =
          oldWord.substring(0, 1).toUpperCase()
              + oldWord.substring(1, oldWord.length());
      oldWordObj.addProperty(SentenceKeys.WORD_KEY, oldWord);
    }

    JsonArray newWords = new JsonArray();
    JsonArray newEntities = new JsonArray();
    int wordIndex = 0;
    int newWordIndex = 0;
    for (JsonElement entity : sentenceObj.get(SentenceKeys.ENTITIES)
        .getAsJsonArray()) {
      JsonObject entityObj = entity.getAsJsonObject();
      int entityStart = entityObj.get(SentenceKeys.START).getAsInt();
      int entityEnd = entityObj.get(SentenceKeys.END).getAsInt();
      while (wordIndex < entityStart) {
        newWords.add(oldWords.get(wordIndex));
        newWordIndex++;
        wordIndex++;
      }
      JsonObject newEntity = entityObj;
      newEntity.remove(SentenceKeys.START);
      newEntity.remove(SentenceKeys.END);
      newEntity.addProperty(SentenceKeys.ENTITY_INDEX, newWordIndex);
      newEntities.add(newEntity);

      JsonObject newWord =
          jsonParser.parse(
              gson.toJson(oldWords.get(wordIndex).getAsJsonObject()))
              .getAsJsonObject();
      newWord.remove(SentenceKeys.WORD_KEY);


      String entityPhrase =
          getWordsCapitalizedInitial(oldWords, entityStart, entityEnd);
      newWord.addProperty(SentenceKeys.WORD_KEY, entityPhrase);

      if (newWord.has(SentenceKeys.LEMMA_KEY)) {
        newWord.remove(SentenceKeys.LEMMA_KEY);
        newWord.addProperty(SentenceKeys.LEMMA_KEY, entityPhrase);
      }

      if (newWord.has(SentenceKeys.POS_KEY)) {
        newWord.remove(SentenceKeys.POS_KEY);
        newWord.addProperty(SentenceKeys.POS_KEY, PROPER_NOUN_TAG);
      }

      if (newWord.has(SentenceKeys.NER_KEY)) {
        newWord.remove(SentenceKeys.NER_KEY);
        newWord.addProperty(SentenceKeys.NER_KEY,
            getFirstNERTag(oldWords, entityStart, entityEnd));
      }
      newWords.add(newWord);
      newWordIndex++;
      wordIndex = entityEnd + 1;
    }
    while (wordIndex < oldWords.size()) {
      newWords.add(oldWords.get(wordIndex));
      wordIndex++;
    }

    newSentence.remove(SentenceKeys.WORDS_KEY);
    newSentence.add(SentenceKeys.WORDS_KEY, newWords);
    newSentence.add(SentenceKeys.ENTITIES, newEntities);
    return newSentence;
  }

  public static JsonObject mergeNamedEntitiesToSingleWord(String sentence) {
    JsonObject sentenceObj = jsonParser.parse(sentence).getAsJsonObject();
    JsonObject newSentence = jsonParser.parse(sentence).getAsJsonObject();

    JsonArray oldWords =
        sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    Map<Integer, Integer> oldToNewMap = new HashMap<>();
    JsonArray newWords = new JsonArray();

    int newIndex = 0;
    for (int i = 0; i < oldWords.size(); i++) {
      JsonObject oldWord = oldWords.get(i).getAsJsonObject();
      oldToNewMap.put(i, newIndex);
      if (oldWord.has(SentenceKeys.NER_KEY)
          && !oldWord.get(SentenceKeys.NER_KEY).getAsString()
              .equals(NON_NAMED_ENTITY)) {
        String namedEntity = oldWord.get(SentenceKeys.NER_KEY).getAsString();
        int entityEnd = i;
        for (int j = i + 1; j < oldWords.size(); j++) {
          String nextNamedEntity =
              oldWords.get(j).getAsJsonObject().get(SentenceKeys.NER_KEY)
                  .getAsString();
          if (!nextNamedEntity.equals(namedEntity)) {
            break;
          } else {
            oldToNewMap.put(j, newIndex);
            entityEnd = j;
          }
        }

        JsonObject newWord =
            jsonParser.parse(gson.toJson(oldWord)).getAsJsonObject();
        String newWordString =
            getWordsCapitalizedInitial(oldWords, i, entityEnd);
        newWord.addProperty(SentenceKeys.WORD_KEY, newWordString);

        if (newWord.has(SentenceKeys.LEMMA_KEY)) {
          newWord.addProperty(SentenceKeys.LEMMA_KEY, newWordString);
        }

        if (newWord.has(SentenceKeys.POS_KEY)) {
          newWord.addProperty(SentenceKeys.POS_KEY, PROPER_NOUN_TAG);
        }
        
        newWords.add(newWord);
        i = entityEnd;
      } else {
        newWords.add(oldWord);
      }
      newIndex++;
    }
    newSentence.add(SentenceKeys.WORDS_KEY, newWords);

    if (newSentence.has(SentenceKeys.ENTITIES)) {
      for (JsonElement entity : newSentence.get(SentenceKeys.ENTITIES)
          .getAsJsonArray()) {
        JsonObject entityObj = entity.getAsJsonObject();
        int newEntityIndex =
            oldToNewMap
                .get(entityObj.get(SentenceKeys.ENTITY_INDEX).getAsInt());
        entityObj.addProperty(SentenceKeys.ENTITY_INDEX, newEntityIndex);
      }
    }
    return newSentence;
  }

  private static String getWordsCapitalizedInitial(JsonArray words,
      int entityStart, int entityEnd) {
    StringBuilder sb = new StringBuilder();
    for (int i = entityStart; i <= entityEnd; i++) {
      String word =
          words.get(i).getAsJsonObject().get(SentenceKeys.WORD_KEY)
              .getAsString();
      sb.append(word.substring(0, 1).toUpperCase());
      sb.append(word.substring(1, word.length()));
      if (i < entityEnd)
        sb.append("_");
    }
    return sb.toString();
  }

  private static String getFirstNERTag(JsonArray words, int entityStart,
      int entityEnd) {
    for (int i = entityStart; i <= entityEnd; i++) {
      String ner =
          words.get(i).getAsJsonObject().get(SentenceKeys.NER_KEY)
              .getAsString();
      if (!ner.equals(NON_NAMED_ENTITY))
        return ner;
    }
    return NON_NAMED_ENTITY;
  }

  public static void main(String[] args) throws IOException {
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject newSentence = mergeEntityWordsToSingleWord(line);
        newSentence = mergeNamedEntitiesToSingleWord(gson.toJson(newSentence));
        System.out.println(gson.toJson(newSentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
