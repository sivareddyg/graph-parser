package in.sivareddy.graphparser.util;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EntityAnnotator {

  public static enum PosTagCode {
    EN_PTB, EN_UD, ES_UD, DE_UD,
  };

  private Map<String, Object> nameToEntityMap = new HashMap<>();
  public static Pattern NUMBERS_OR_PUNC = Pattern.compile("[\\p{Punct}0-9]+");
  public static Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}]+");
  public static String PREFIX_PUNCTUATION = "^[\\p{Punct}]+";
  public static String SUFFIX_PUNCTUATION = "[\\p{Punct}]+$";
  private static String WORD_PREFIX = "w:";
  private static String ENTITIES = SentenceKeys.ENTITIES;
  public static Set<String> STANFORD_NER_NON_ENTITY = Sets.newHashSet("Time",
      "Money", "Percent", "Date", "O", "DURATION", "ORDINAL");

  public static int MAX_NUMBER_ENTITIES = 1000;

  public static List<Pattern> npPatterns = new ArrayList<>();

  private Gson gson = new Gson();
  private JsonParser jsonParser = new JsonParser();

  private final boolean ignoreNumbersAndPunctuation;
  private final boolean useNPPatterns;

  @SuppressWarnings("unchecked")
  public EntityAnnotator(Reader inputReader,
      boolean ignoreNumbersAndPunctuation, boolean useNPPatterns)
      throws IOException {
    // line format:
    // m.0101qvm I Think of You
    BufferedReader reader = new BufferedReader(inputReader);
    this.ignoreNumbersAndPunctuation = ignoreNumbersAndPunctuation;
    this.useNPPatterns = useNPPatterns;
    while (true) {
      String line = reader.readLine();
      if (line == null)
        break;
      String[] parts = line.split("\t", 2);

      if (ignoreNumbersAndPunctuation
          && NUMBERS_OR_PUNC.matcher(parts[1]).matches())
        continue;

      String[] name = parts[1].split("\\s+");
      Map<String, Object> curMap = nameToEntityMap;
      for (String word : name) {
        String wordKey = word.toLowerCase();
        if (ignoreNumbersAndPunctuation) {
          // Trim punctuation.
          wordKey = removePunctuation(wordKey);
        }
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

        String wordKey = word.toLowerCase();
        if (ignoreNumbersAndPunctuation) {
          wordKey = removePunctuation(wordKey);
        }

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
          && (!useNPPatterns || matchesNPPattern(getPosSequence(words,
              entityStartIndex, entityEndIndex)))) {
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
   * @param sentence input sentence containing tokenised words
   * @param checkSpanIsNP set to true if the span's pos tag sequenc should match
   *        NP pattern
   * @param getMatchedEntities set to true to retrieve all the Freebase entities
   *        that match
   */
  @SuppressWarnings("unchecked")
  public void getAllEntitySpans(JsonObject sentence, boolean checkSpanIsNP,
      boolean getMatchedEntities) {
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

            Set<String> possibleEntities = (Set<String>) curMap.get(ENTITIES);

            // Target potential entities should be less than max number of
            // entities, and if the checkSpanIsNp flag is on, make sure the pos
            // tag sequence matches NP pattern.
            if (possibleEntities.size() < MAX_NUMBER_ENTITIES
                && (!checkSpanIsNP || matchesNPPattern(getPosSequence(words,
                    entityStartIndex, entityEndIndex)))) {
              Map<String, Object> matchedEntity = new HashMap<>();
              matchedEntity.put(SentenceKeys.START, entityStartIndex);
              matchedEntity.put(SentenceKeys.END, entityEndIndex);
              matchedEntity.put(SentenceKeys.PHRASE,
                  getPhrase(words, entityStartIndex, entityEndIndex));
              if (getMatchedEntities) {
                matchedEntity.put(SentenceKeys.ENTITIES, possibleEntities);
              }
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

  public static void getAllNounPhrases(JsonObject sentence, PosTagCode code) {
    if (!sentence.has(SentenceKeys.WORDS_KEY))
      return;
    List<JsonObject> words = new ArrayList<>();
    sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray()
        .forEach(x -> words.add(x.getAsJsonObject()));

    JsonArray matchedEntities = new JsonArray();
    for (int i = 0; i < words.size(); i++) {
      if (words.get(i).get(SentenceKeys.WORD_KEY).getAsString()
          .matches("[\\p{Punct}]*")) {
        continue;
      }

      for (int j = i; j < words.size(); j++) {
        if (words.get(j).get(SentenceKeys.WORD_KEY).getAsString()
            .matches("[\\p{Punct}]*")) {
          continue;
        }

        if (spanIsNP(words, i, j, code)) {
          JsonObject span = new JsonObject();
          span.addProperty(SentenceKeys.START, i);
          span.addProperty(SentenceKeys.END, j);
          span.addProperty(
              "phrase",
              Joiner.on(" ").join(
                  words.subList(i, j + 1).stream()
                      .map(x -> x.get(SentenceKeys.WORD_KEY).getAsString())
                      .iterator()));
          span.addProperty(
              "pattern",
              Joiner.on(" ").join(
                  words
                      .subList(i, j + 1)
                      .stream()
                      .filter(
                          x -> !x.get(SentenceKeys.WORD_KEY).getAsString()
                              .matches("[\\p{Punct}]*"))
                      .map(x -> x.get(SentenceKeys.POS_KEY).getAsString())
                      .iterator()));
          matchedEntities.add(span);
        }
      }
    }
    if (matchedEntities.size() > 0) {
      sentence.add(SentenceKeys.MATCHED_ENTITIES, matchedEntities);
    }
  }

  private static boolean spanIsNP(List<JsonObject> words, int spanStart,
      int spanEnd, PosTagCode code) {

    // Sequence of non-punc pos tags.
    String posSequence =
        Joiner.on(" ")
            .join(
                words
                    .subList(spanStart, spanEnd + 1)
                    .stream()
                    .filter(
                        x -> !x.get(SentenceKeys.WORD_KEY).getAsString()
                            .matches("[\\p{Punct}]*"))
                    .map(x -> x.get(SentenceKeys.POS_KEY).getAsString())
                    .iterator());

    if (code.equals(PosTagCode.EN_PTB)) {

      // If the list is single word, it should be a proper noun or a named
      // entity.
      if (spanEnd - spanStart == 0) {
        if (words.get(spanStart).has(SentenceKeys.NER_KEY)
            && !STANFORD_NER_NON_ENTITY.contains(words.get(spanStart)
                .get(SentenceKeys.NER_KEY).getAsString())) {
          return true;
        }
        return posSequence.matches("NNP.*");
      }

      // e.g. DT|IN NN|JJ NN|CD; DT JJ NN; CD
      // the big lebowski
      // james bond
      // olympics 2012
      // obama's country
      if (posSequence
          .matches("(D[^ ]* )?([JN][^ ]* |CD |VBG |POS ){1,3}(N[^ ]*|CD)"))
        return true;

      // the movie
      if (posSequence.matches("DT [JN][^ ]*"))
        return true;

      // lord of the rings
      if (posSequence
          .matches("(D[^ ]* )?([JN][^ ]* |CD |VBG |POS ){1,3}(IN )(D[^ ]* )?([JN][^ ]*|CD){1,3}"))
        return true;

      // bold and brave
      if (posSequence
          .matches("(D[^ ]* )?([JN][^ ]* |CD |VBG |POS ){1,3}(CC )(D[^ ]* )?([JN][^ ]*|CD){1,3}"))
        return true;


      // star trek the next genreation
      if (posSequence
          .matches("(D[^ ]* )?([JN][^ ]* |CD |VBG |POS ){1,3}(D[^ ]* )?([JN][^ ]*|CD){1,3}"))
        return true;

      // to kill a mocking bird
      if (posSequence
          .matches("(I[^ ]* )(V[^ ]* )(D[^ ]* )?([JN][^ ]* |CD |VBG |POS ){1,3}(N[^ ]*|CD)"))
        return true;
    } else if (code.equals(PosTagCode.EN_UD) || code.equals(PosTagCode.ES_UD)
        || code.equals(PosTagCode.DE_UD)) {

      String potentialEntityPos = "(ADJ|NOUN|PROPN|VERB)";
      if (spanEnd - spanStart == 0) {
        return posSequence.matches(potentialEntityPos);
      }

      String nounPhrase =
          String.format("(DET )?(%s ){0,3}%s", potentialEntityPos,
              potentialEntityPos);

      if (posSequence.matches(String.format("%s", nounPhrase)))
        return true;

      // the big lebowski
      // james bond
      // olympics 2012
      // finding nemo TODO.
      // star trek the next generation
      if (posSequence.matches(String.format("%s %s", nounPhrase, nounPhrase)))
        return true;

      // lord of the rings
      // obama's country
      if (posSequence.matches(String.format("%s (ADP|PART) %s", nounPhrase,
          nounPhrase)))
        return true;

      // bold and brave
      if (posSequence.matches(String.format("%s CONJ %s", nounPhrase,
          nounPhrase)))
        return true;

      // to kill a mocking bird
      if (posSequence.matches(String.format("ADP VERB %s", nounPhrase)))
        return true;

      // the running horse.
      if (posSequence.matches(String.format("DET VERB %s", nounPhrase)))
        return true;
    }
    return false;
  }

  public static void addDateEntities(JsonObject sentenceObj) {
    Set<Integer> positionIsEntity = new HashSet<>();
    List<JsonObject> entities = new ArrayList<>();
    if (sentenceObj.has(SentenceKeys.ENTITIES)) {
      for (JsonElement entity : sentenceObj.get(SentenceKeys.ENTITIES)
          .getAsJsonArray()) {
        JsonObject entityObj = entity.getAsJsonObject();
        for (int i = entityObj.get(SentenceKeys.START).getAsInt(); i <= entityObj
            .get(SentenceKeys.END).getAsInt(); i++) {
          positionIsEntity.add(i);
        }
        entities.add(entityObj);
      }
    }

    JsonArray words = sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    for (int i = 0; i < words.size(); i++) {
      JsonObject word = words.get(i).getAsJsonObject();
      if (!positionIsEntity.contains(i) && MergeEntity.isDate(word)) {
        StringBuilder phraseBuilder = new StringBuilder();
        phraseBuilder.append(word.get(SentenceKeys.WORD_KEY).getAsString());
        int entityEnd = i;
        for (int j = i + 1; j < words.size(); j++) {
          JsonObject nextWord = words.get(j).getAsJsonObject();
          if (positionIsEntity.contains(j) || !MergeEntity.isDate(nextWord)) {
            break;
          } else {
            phraseBuilder.append(" ");
            phraseBuilder.append(nextWord.get(SentenceKeys.WORD_KEY)
                .getAsString());
            entityEnd = j;
          }
        }

        String phrase = phraseBuilder.toString();
        JsonObject entityObj = new JsonObject();
        entityObj.addProperty(SentenceKeys.PHRASE, phrase);
        entityObj.addProperty(SentenceKeys.ENTITY, "type.datetime");
        entityObj.addProperty(SentenceKeys.START, i);
        entityObj.addProperty(SentenceKeys.END, entityEnd);
        entities.add(entityObj);

        i = entityEnd;
      }
    }

    entities.sort(Comparator.comparing(x -> x.get(SentenceKeys.START)
        .getAsInt()));
    JsonArray entitiesArr = new JsonArray();
    entities.forEach(x -> entitiesArr.add(x));
    sentenceObj.add(SentenceKeys.ENTITIES, entitiesArr);
  }
  
  public static void addNamedEntities(JsonObject sentenceObj) {
    Set<Integer> positionIsEntity = new HashSet<>();
    List<JsonObject> entities = new ArrayList<>();
    if (sentenceObj.has(SentenceKeys.ENTITIES)) {
      for (JsonElement entity : sentenceObj.get(SentenceKeys.ENTITIES)
          .getAsJsonArray()) {
        JsonObject entityObj = entity.getAsJsonObject();
        for (int i = entityObj.get(SentenceKeys.START).getAsInt(); i <= entityObj
            .get(SentenceKeys.END).getAsInt(); i++) {
          positionIsEntity.add(i);
        }
        entities.add(entityObj);
      }
    }

    JsonArray words = sentenceObj.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    for (int i = 0; i < words.size(); i++) {
      JsonObject word = words.get(i).getAsJsonObject();
      if (!word.has(SentenceKeys.NER_KEY))
        continue;
      String currentEntityType = word.get(SentenceKeys.NER_KEY).getAsString(); 
      if (!positionIsEntity.contains(i)
          && !currentEntityType.equals(MergeEntity.NON_NAMED_ENTITY)) {
        StringBuilder phraseBuilder = new StringBuilder();
        phraseBuilder.append(word.get(SentenceKeys.WORD_KEY).getAsString());
        int entityEnd = i;
        for (int j = i + 1; j < words.size(); j++) {
          JsonObject nextWord = words.get(j).getAsJsonObject();
          if (positionIsEntity.contains(j)
              || !nextWord.has(SentenceKeys.NER_KEY)
              || !nextWord.get(SentenceKeys.NER_KEY).getAsString()
              .equals(currentEntityType)) {
            break;
          } else {
            phraseBuilder.append(" ");
            phraseBuilder.append(nextWord.get(SentenceKeys.WORD_KEY)
                .getAsString());
            entityEnd = j;
          }
        }

        String phrase = phraseBuilder.toString();
        JsonObject entityObj = new JsonObject();
        entityObj.addProperty(SentenceKeys.PHRASE, phrase);
        entityObj.addProperty(SentenceKeys.START, i);
        entityObj.addProperty(SentenceKeys.END, entityEnd);
        entities.add(entityObj);

        i = entityEnd;
      }
    }

    entities.sort(Comparator.comparing(x -> x.get(SentenceKeys.START)
        .getAsInt()));
    JsonArray entitiesArr = new JsonArray();
    entities.forEach(x -> entitiesArr.add(x));
    sentenceObj.add(SentenceKeys.ENTITIES, entitiesArr);
  }

  public static void main(String[] args) throws IOException {
    System.err.println(args[0]);
    EntityAnnotator entityAnnotator =
        new EntityAnnotator(new InputStreamReader(new GZIPInputStream(
            new FileInputStream(args[0])), "UTF-8"), true, true);
    entityAnnotator.setDefaultNPPattern();

    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        entityAnnotator.getAllEntitySpans(sentence, false, false);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
