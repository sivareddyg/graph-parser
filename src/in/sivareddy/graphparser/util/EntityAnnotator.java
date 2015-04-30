package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EntityAnnotator {
  private Map<String, Object> nameToEntityMap = new HashMap<>();
  public static Pattern NUMBERS_OR_PUNC = Pattern.compile("[\\p{Punct}0-9]+");
  public static Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}]+");
  public static String PREFIX_PUNCTUATION = "^[\\p{Punct}]+";
  public static String SUFFIX_PUNCTUATION = "[\\p{Punct}]+$";
  private static String WORD_PREFIX = "w:";
  private static String ENTITIES = SentenceKeys.ENTITIES;

  public static int MAX_NUMBER_ENTITIES = 1000;

  public static List<Pattern> npPatterns = new ArrayList<>();

  private Gson gson = new Gson();
  private JsonParser jsonParser = new JsonParser();



  @SuppressWarnings("unchecked")
  public EntityAnnotator(Reader inputReader) throws IOException {
    // line format:
    // m.0101qvm I Think of You
    BufferedReader reader = new BufferedReader(inputReader);
    while (true) {
      String line = reader.readLine();
      if (line == null)
        break;
      String[] parts = line.split("\t", 2);
      if (NUMBERS_OR_PUNC.matcher(parts[1]).matches())
        continue;

      String[] name = parts[1].split("\\s+");
      Map<String, Object> curMap = nameToEntityMap;
      for (String word : name) {
        // Trim punctuation.
        String wordKey = removePunctuation(word).toLowerCase();
        if (!wordKey.isEmpty()) {
          wordKey = WORD_PREFIX + wordKey;
          curMap.putIfAbsent(wordKey, new HashMap<String, Object>());
          curMap = (Map<String, Object>) curMap.get(wordKey);
        }
      }

      if (curMap != nameToEntityMap) {
        curMap.putIfAbsent(ENTITIES, new HashSet<String>());
        ((Set<String>) curMap.get(ENTITIES)).add(parts[0]);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void maximalMatch(JsonObject sentence) {
    if (!sentence.has(SentenceKeys.WORDS_KEY))
      return;
    JsonArray words = sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    List<Map<String, Object>> matchedEntities = new ArrayList<>();
    for (int i = 0; i < words.size(); i++) {
      Set<String> lastEntitiesMatched = null;
      int entityEndIndex = -1;
      int entityStartIndex = -1;
      Map<String, Object> curMap = nameToEntityMap;

      for (int j = i; j < words.size(); j++) {
        String word =
            words.get(j).getAsJsonObject().get(SentenceKeys.WORD_KEY)
                .getAsString();

        String wordKey = removePunctuation(word).toLowerCase();
        if (wordKey.isEmpty()) {
          continue;
        }

        wordKey = WORD_PREFIX + wordKey;
        if (curMap.containsKey(wordKey)) {
          if (curMap == nameToEntityMap)
            entityStartIndex = j;

          curMap = (Map<String, Object>) curMap.get(wordKey);
          if (curMap.containsKey(ENTITIES)) {
            lastEntitiesMatched = (Set<String>) curMap.get(ENTITIES);
            entityEndIndex = j;
          }
        } else
          break;
      }

      if (entityEndIndex != -1
          && matchesNPPattern(getPosSequence(words, entityStartIndex,
              entityEndIndex))) {
        Map<String, Object> matchedEntity = new HashMap<>();
        matchedEntity.put(SentenceKeys.START, entityStartIndex);
        matchedEntity.put(SentenceKeys.END, entityEndIndex);
        matchedEntity.put(SentenceKeys.ENTITIES, lastEntitiesMatched);
        matchedEntity.put(SentenceKeys.PHRASE,
            getPhrase(words, entityStartIndex, entityEndIndex));
        matchedEntities.add(matchedEntity);
        i = entityEndIndex;
      }
    }

    if (matchedEntities.size() > 0) {
      sentence.add("matchedEntities",
          jsonParser.parse(gson.toJson(matchedEntities)));
    }
  }

  /**
   * Annotates entities based on the following criteria. - Identify all the
   * spans which match entity names in Freebase. - Each span should match NP
   * pattern - Each span should not be associated with more than MAX_NUMBER
   * entities
   * 
   * @param sentence
   */
  @SuppressWarnings("unchecked")
  public void getAllEntitySpans(JsonObject sentence) {
    if (!sentence.has(SentenceKeys.WORDS_KEY))
      return;
    JsonArray words = sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    List<Map<String, Object>> matchedEntities = new ArrayList<>();
    for (int i = 0; i < words.size(); i++) {
      int entityEndIndex = -1;
      int entityStartIndex = -1;

      Map<String, Object> curMap = nameToEntityMap;
      for (int j = i; j < words.size(); j++) {
        JsonObject wordObject = words.get(j).getAsJsonObject();
        String word = wordObject.get(SentenceKeys.WORD_KEY).getAsString();

        String wordKey = removePunctuation(word).toLowerCase();
        if (wordKey.isEmpty()) {
          continue;
        }

        wordKey = WORD_PREFIX + wordKey;
        if (curMap.containsKey(wordKey)) {
          if (curMap == nameToEntityMap)
            entityStartIndex = j;

          curMap = (Map<String, Object>) curMap.get(wordKey);
          if (curMap.containsKey(ENTITIES)) {
            entityEndIndex = j;
            String posSequence =
                getPosSequence(words, entityStartIndex, entityEndIndex);
            Set<String> possibleEntities = (Set<String>) curMap.get(ENTITIES);
            if (possibleEntities.size() < MAX_NUMBER_ENTITIES
                && matchesNPPattern(posSequence)) {
              Map<String, Object> matchedEntity = new HashMap<>();
              matchedEntity.put(SentenceKeys.START, entityStartIndex);
              matchedEntity.put(SentenceKeys.END, entityEndIndex);
              matchedEntity.put(SentenceKeys.ENTITIES, possibleEntities);
              matchedEntity.put(SentenceKeys.PHRASE,
                  getPhrase(words, entityStartIndex, entityEndIndex));
              matchedEntities.add(matchedEntity);
            }
          }
        } else {
          break;
        }
      }
    }

    if (matchedEntities.size() > 0) {
      sentence.add("matchedEntities",
          jsonParser.parse(gson.toJson(matchedEntities)));
    }
  }

  public static String getPhrase(JsonArray words, int entityStartIndex,
      int entityEndIndex) {
    StringBuilder sb = new StringBuilder();
    for (int i = entityStartIndex; i <= entityEndIndex; i++) {
      sb.append(words.get(i).getAsJsonObject().get(SentenceKeys.WORD_KEY)
          .getAsString());
      if (i < entityEndIndex) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  public static String getPosSequence(JsonArray words, int entityStartIndex,
      int entityEndIndex) {
    StringBuilder sb = new StringBuilder();
    for (int i = entityStartIndex; i <= entityEndIndex; i++) {
      sb.append(words.get(i).getAsJsonObject().get(SentenceKeys.POS_KEY)
          .getAsString());
      if (i < entityEndIndex) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  public static String removePunctuation(String word) {
    String cleanWord = word.replaceAll(PREFIX_PUNCTUATION, "");
    cleanWord = cleanWord.replaceAll(SUFFIX_PUNCTUATION, "");
    return cleanWord;
  }


  public void setDefaultNPPattern() {
    // Single noun or adjective.
    npPatterns.add(Pattern.compile("^[NJ][^\\s]*$"));

    // Noun phrase.
    npPatterns.add(Pattern.compile("^[DJN].* [NJC][^\\s]*$"));
  }

  public void setNPPatterns(List<String> patterns) {
    for (String pattern : patterns) {
      npPatterns.add(Pattern.compile(pattern));
    }
  }

  public boolean matchesNPPattern(String input) {
    for (Pattern p : npPatterns) {
      if (p.matcher(input).matches())
        return true;
    }
    return false;
  }

  public static void main(String[] args) throws IOException {
    System.err.println(args[0]);
    EntityAnnotator entityAnnotator =
        new EntityAnnotator(new InputStreamReader(new GZIPInputStream(
            new FileInputStream(args[0])), "UTF-8"));
    entityAnnotator.setDefaultNPPattern();

    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        entityAnnotator.getAllEntitySpans(sentence);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
