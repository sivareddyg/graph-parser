package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;
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


  public static JsonObject mergeDateEntities(String sentence) {
    JsonObject sentenceObj = jsonParser.parse(sentence).getAsJsonObject();
    JsonObject newSentence = jsonParser.parse(sentence).getAsJsonObject();

    Set<Integer> entityPostions = new HashSet<>();
    List<JsonObject> entities = new ArrayList<>();
    if (sentenceObj.has(SentenceKeys.ENTITIES)) {
      for (JsonElement entity : sentenceObj.get(SentenceKeys.ENTITIES)
          .getAsJsonArray()) {
        JsonObject entityObj = entity.getAsJsonObject();
        entityPostions.add(entityObj.get(SentenceKeys.ENTITY_INDEX).getAsInt());
        entities.add(entityObj);
      }
    }

    JsonArray oldWords =
        sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    Map<Integer, Integer> oldToNewMap = new HashMap<>();
    JsonArray newWords = new JsonArray();

    int newIndex = 0;
    for (int i = 0; i < oldWords.size(); i++) {
      JsonObject oldWord = oldWords.get(i).getAsJsonObject();
      oldToNewMap.put(i, newIndex);
      if (!entityPostions.contains(i) && isDate(oldWord)) {
        StringBuilder phraseBuilder = new StringBuilder();
        phraseBuilder.append(oldWord.get(SentenceKeys.WORD_KEY).getAsString());
        int entityEnd = i;
        for (int j = i + 1; j < oldWords.size(); j++) {
          JsonObject nextWord = oldWords.get(j).getAsJsonObject();
          if (entityPostions.contains(j) || !isDate(nextWord)) {
            break;
          } else {
            phraseBuilder.append(" ");
            phraseBuilder.append(nextWord.get(SentenceKeys.WORD_KEY)
                .getAsString());
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

        newWords.add(newWord);
        String phrase = phraseBuilder.toString();
        JsonObject entityObj = new JsonObject();
        entityObj.addProperty(SentenceKeys.PHRASE, phrase);
        entityObj.addProperty(SentenceKeys.ENTITY, "type.datetime");
        entityObj.addProperty(SentenceKeys.ENTITY_INDEX, i);
        entities.add(entityObj);

        i = entityEnd;
      } else {
        newWords.add(oldWord);
      }
      newIndex++;
    }
    newSentence.add(SentenceKeys.WORDS_KEY, newWords);
    entities.sort(Comparator.comparing(x -> x.get(SentenceKeys.ENTITY_INDEX)
        .getAsInt()));

    if (entities.size() > 0) {
      JsonArray entityArr = new JsonArray();
      newSentence.add(SentenceKeys.ENTITIES, entityArr);

      for (JsonObject entityObj : entities) {
        int newEntityIndex =
            oldToNewMap
                .get(entityObj.get(SentenceKeys.ENTITY_INDEX).getAsInt());
        entityObj.addProperty(SentenceKeys.ENTITY_INDEX, newEntityIndex);
        entityArr.add(entityObj);
      }
    }
    return newSentence;
  }

  private static Set<String> timeEntities = Sets.newHashSet("TIME", "DATE");
  private static Pattern yearPattern = Pattern.compile("[0-9]{3,4}");

  private static boolean isDate(JsonObject wordObject) {
    String word = wordObject.get(SentenceKeys.WORD_KEY).getAsString();
    if (yearPattern.matcher(word).matches()) {
      return true;
    }

    if (wordObject.has(SentenceKeys.NER_KEY)) {
      String ner = wordObject.get(SentenceKeys.NER_KEY).getAsString();
      if (timeEntities.contains(ner))
        return true;
    }
    return false;
  }

  private static Set<String> PROPER_NOUN_NERS = Sets.newHashSet("LOCATION",
      "PERSON", "ORGANIZATION");

  public static JsonObject mergeNamedEntitiesToSingleWord(String sentence) {
    JsonObject sentenceObj = jsonParser.parse(sentence).getAsJsonObject();
    JsonObject newSentence = jsonParser.parse(sentence).getAsJsonObject();

    JsonArray oldWords =
        sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    Map<Integer, Integer> oldToNewMap = new HashMap<>();
    JsonArray newWords = new JsonArray();

    Set<Integer> entityPositions = new HashSet<>();
    if (newSentence.has(SentenceKeys.ENTITIES)) {
      for (JsonElement entity : newSentence.get(SentenceKeys.ENTITIES)
          .getAsJsonArray()) {
        JsonObject entityObj = entity.getAsJsonObject();
        entityPositions.add(entityObj.get(SentenceKeys.INDEX_KEY).getAsInt());
      }
    }

    int newIndex = 0;
    for (int i = 0; i < oldWords.size(); i++) {
      JsonObject oldWord = oldWords.get(i).getAsJsonObject();
      oldToNewMap.put(i, newIndex);
      if (oldWord.has(SentenceKeys.NER_KEY)
          && !entityPositions.contains(i)
          && !oldWord.get(SentenceKeys.NER_KEY).getAsString()
              .equals(NON_NAMED_ENTITY)) {
        String namedEntity = oldWord.get(SentenceKeys.NER_KEY).getAsString();
        int entityEnd = i;
        for (int j = i + 1; j < oldWords.size(); j++) {
          String nextNamedEntity =
              oldWords.get(j).getAsJsonObject().get(SentenceKeys.NER_KEY)
                  .getAsString();
          if (!nextNamedEntity.equals(namedEntity)
              || entityPositions.contains(j)) {
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

        if (newWord.has(SentenceKeys.POS_KEY)
            && PROPER_NOUN_NERS.contains(namedEntity)) {
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
        newSentence = mergeDateEntities(gson.toJson(newSentence));
        // newSentences =
        // mergeNamedEntitiesToSingleWord(gson.toJson(newSentences));
        System.out.println(gson.toJson(newSentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
