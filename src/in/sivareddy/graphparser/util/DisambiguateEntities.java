package in.sivareddy.graphparser.util;

import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DisambiguateEntities {

  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  public static final Set<String> PROPER_NOUNS = Sets.newHashSet("NNP", "NNPS",
      "PROPN");
  public static final Set<String> SINGLE_WORD_ENTITY_TAGS = Sets.newHashSet(
      "PROPN", "NOUN", "ADJ");

  public static final Set<String> QUESTION_WORDS = Sets.newHashSet("what",
      "when", "who", "where", "which", "how", "many", "much", "quién?", "qué",
      "dónde?", "cuándo", "cuánto", "cuánta", "cuántos", "cuántas", "cuál",
      "quiénes", "cuáles");

  public static final Set<String> VERB_TAGS = Sets.newHashSet("VERB");

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
        double score = rankedEntity.get(SentenceKeys.SCORE).getAsDouble();
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

        double score = rankedEntity.get(SentenceKeys.SCORE).getAsDouble();
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

  /**
   * @param sentence
   * @param initialNbest
   * @param intermediateNbest
   * @param finalNbest
   * @param entityHasReadableId
   * @param kb
   * @param shouldStartWithNamedEntity
   * @param containsNamedEntity
   * @param noPrecedingNamedEntity
   * @param noSucceedingNamedEntity
   * @param containsProperNoun
   * @param noPrecedingProperNoun
   * @param noSucceedingProperNoun
   * @param ignoreEntitiesWithVerbs
   * @param ignoreEntitiesWithQuestionWords
   */
  public static void latticeBasedDisambiguation(JsonObject sentence,
      int initialNbest, int intermediateNbest, int finalNbest,
      boolean entityHasReadableId, KnowledgeBase kb,
      boolean shouldStartWithNamedEntity, boolean containsNamedEntity,
      boolean noPrecedingNamedEntity, boolean noSucceedingNamedEntity,
      boolean containsProperNoun, boolean noPrecedingProperNoun,
      boolean noSucceedingProperNoun, boolean ignoreEntitiesWithVerbs,
      boolean ignoreEntitiesWithQuestionWords) {
    if (!sentence.has(SentenceKeys.MATCHED_ENTITIES)) {
      return;
    }

    List<JsonObject> words = new ArrayList<>();
    sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray()
        .forEach(x -> words.add(x.getAsJsonObject()));

    Map<Pair<Integer, Integer>, List<ChartEntry>> spanToEntities =
        new HashMap<>();
    Map<Integer, List<ChartEntry>> spanStartToEntities = new HashMap<>();

    JsonArray disambiguatedEntities = null;
    if (sentence.has(SentenceKeys.DISAMBIGUATED_ENTITIES)) {
      disambiguatedEntities =
          sentence.get(SentenceKeys.DISAMBIGUATED_ENTITIES).getAsJsonArray();
    }

    if (disambiguatedEntities == null || disambiguatedEntities.size() == 0) {
      JsonObject emptyObject = new JsonObject();
      emptyObject.add(SentenceKeys.ENTITIES, new JsonArray());
      emptyObject.addProperty(SentenceKeys.SCORE, 0.0);
      disambiguatedEntities = new JsonArray();
      disambiguatedEntities.add(emptyObject);
    }


    for (JsonElement matchedEntity : sentence
        .get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
      JsonObject matchedEntityObj = matchedEntity.getAsJsonObject();
      if (!matchedEntityObj.has(SentenceKeys.RANKED_ENTITIES))
        continue;

      int spanStart = matchedEntityObj.get(SentenceKeys.START).getAsInt();
      int spanEnd = matchedEntityObj.get(SentenceKeys.END).getAsInt();
      Pair<Integer, Integer> span = Pair.of(spanStart, spanEnd);

      // If the entity is a single word, it should either be a noun or
      // adjective.
      String startTag =
          words.get(spanStart).get(SentenceKeys.POS_KEY).getAsString();
      if (spanEnd - spanStart == 0 && !startTag.startsWith("N")
          && !startTag.startsWith("J") && !PROPER_NOUNS.contains(startTag)
          && !SINGLE_WORD_ENTITY_TAGS.contains(startTag))
        continue;

      String startNer =
          words.get(spanStart).has(SentenceKeys.NER_KEY) ? words.get(spanStart)
              .get(SentenceKeys.NER_KEY).getAsString() : "O";

      if (shouldStartWithNamedEntity) {
        // Entity span should start with a named entity or with a proper noun.
        if (startNer.equals("O") && !PROPER_NOUNS.contains(startTag))
          continue;
      }

      String endNer =
          words.get(spanEnd).has(SentenceKeys.NER_KEY) ? words.get(spanEnd)
              .get(SentenceKeys.NER_KEY).getAsString() : "O";

      if (containsNamedEntity) {
        boolean hasNamedEntity = false;
        for (int i = spanStart; i <= spanEnd; i++) {
          String contextNer =
              words.get(i).get(SentenceKeys.NER_KEY).getAsString();
          if (!EntityAnnotator.STANFORD_NER_NON_ENTITY.contains(contextNer)) {
            hasNamedEntity = true;
            break;
          }
        }
        if (!hasNamedEntity)
          continue;
      }

      if (containsProperNoun) {
        boolean hasProperNoun = false;
        for (int i = spanStart; i <= spanEnd; i++) {
          String contextPos =
              words.get(i).get(SentenceKeys.POS_KEY).getAsString();
          if (PROPER_NOUNS.contains(contextPos)) {
            hasProperNoun = true;
            break;
          }
        }
        if (!hasProperNoun)
          continue;
      }

      if (noPrecedingNamedEntity) {
        // Entity span should not be preceded by an entity that has the same
        // ner tag.
        if (spanStart > 0) {
          String prevNer =
              words.get(spanStart - 1).get(SentenceKeys.NER_KEY).getAsString();
          String prevTag =
              words.get(spanStart - 1).get(SentenceKeys.POS_KEY).getAsString();
          if (!prevNer.equals("O") && prevNer.equals(startNer)
              && (prevTag.startsWith("N") || prevTag.startsWith("J")))
            continue;
        }
      }

      if (noPrecedingProperNoun) {
        // Entity span should not be preceded by a proper noun.
        if (spanStart > 0) {
          String prevTag =
              words.get(spanStart - 1).get(SentenceKeys.POS_KEY).getAsString();
          if (PROPER_NOUNS.contains(prevTag))
            continue;
        }
      }

      if (noSucceedingNamedEntity) {
        // Entity span should not be succeeded by an entity that has the same
        // ner tag.
        if (spanEnd < words.size() - 1) {
          String nextNer =
              words.get(spanEnd + 1).get(SentenceKeys.NER_KEY).getAsString();
          String nextTag =
              words.get(spanEnd + 1).get(SentenceKeys.POS_KEY).getAsString();
          if (!nextNer.equals("O") && nextNer.equals(endNer)
              && (nextTag.startsWith("N") || nextTag.startsWith("J")))
            continue;
        }
      }

      if (noSucceedingProperNoun) {
        // Entity span should not be succeeded by a proper noun.
        if (spanEnd < words.size() - 1) {
          String nextTag =
              words.get(spanEnd + 1).get(SentenceKeys.POS_KEY).getAsString();
          if (PROPER_NOUNS.contains(nextTag))
            continue;
        }
      }

      if (ignoreEntitiesWithVerbs) {
        String posTagPattern =
            matchedEntityObj.get(SentenceKeys.PATTERN).getAsString();
        Set<String> posTags =
            Sets.newHashSet(Splitter.on(" ").split(posTagPattern));
        posTags.retainAll(VERB_TAGS);
        if (posTags.size() > 0) {
          continue;
        }
      }

      if (ignoreEntitiesWithQuestionWords) {
        String phrase =
            matchedEntityObj.get(SentenceKeys.PHRASE).getAsString()
                .toLowerCase();
        Set<String> phraseWords =
            Sets.newHashSet(Splitter.on(" ").split(phrase));
        phraseWords.retainAll(QUESTION_WORDS);
        if (phraseWords.size() > 0)
          continue;
      }

      spanToEntities.put(span, new ArrayList<>());
      if (!spanStartToEntities.containsKey(spanStart))
        spanStartToEntities.put(spanStart, new ArrayList<>());
      JsonArray rankedEntities =
          matchedEntityObj.get(SentenceKeys.RANKED_ENTITIES).getAsJsonArray();
      int count = 0;
      for (JsonElement rankedEntity : rankedEntities) {
        JsonObject rankedEntityObj = rankedEntity.getAsJsonObject();
        // If the entity does not have a readable Freebase id, ignore it.
        if (entityHasReadableId
            && (!rankedEntityObj.has("id") || !rankedEntityObj.get("id")
                .getAsString().startsWith("/en/")))
          continue;

        double curScore = rankedEntityObj.get(SentenceKeys.SCORE).getAsDouble();
        ChartEntry chartEntry = new ChartEntry();
        chartEntry.getEntities().add(rankedEntityObj);
        chartEntry.getEntitySpans().add(span);
        chartEntry.setScore(curScore);

        spanToEntities.get(span).add(chartEntry);
        spanStartToEntities.get(spanStart).add(chartEntry);
        count++;
        if (count > initialNbest)
          break;
      }
    }

    List<JsonObject> newDisambiguatedEntities = new ArrayList<>();
    for (JsonElement disambiguatedEntitiesEntry : disambiguatedEntities) {
      JsonObject disambiguatedEntry =
          disambiguatedEntitiesEntry.getAsJsonObject();
      JsonArray existingEntitiesArray =
          disambiguatedEntry.get(SentenceKeys.ENTITIES).getAsJsonArray();
      if (existingEntitiesArray.size() > 0) {
        newDisambiguatedEntities.add(disambiguatedEntry);
      }
      List<JsonObject> existingEntities = new ArrayList<>();
      existingEntitiesArray.forEach(x -> existingEntities.add(x
          .getAsJsonObject()));

      List<ChartEntry> prevSpanEntities = new ArrayList<>();
      for (int spanStart = words.size() - 1; spanStart >= 0; spanStart--) {
        List<ChartEntry> curSpanEntities = new ArrayList<>();
        curSpanEntities.addAll(prevSpanEntities);
        if (spanStartToEntities.containsKey(spanStart)) {
          // Discard the entries that are not in relation with existing
          // entities.
          List<ChartEntry> potentialEntries =
              selectValidChartEntries(spanStartToEntities.get(spanStart),
                  existingEntities, kb);
          if (potentialEntries.size() != 0) {
            curSpanEntities.addAll(potentialEntries);
            Collections.sort(curSpanEntities, Collections.reverseOrder());
            if (curSpanEntities.size() > intermediateNbest)
              curSpanEntities = curSpanEntities.subList(0, intermediateNbest);
          }

          for (ChartEntry newSpanEntry : potentialEntries) {
            JsonObject newEntity = newSpanEntry.getEntities().get(0);
            Pair<Integer, Integer> newEntitySpan =
                newSpanEntry.getEntitySpans().get(0);
            Integer newEntitySpanEnd = newEntitySpan.getRight();
            String newEntityMid =
                newEntity.get(SentenceKeys.ENTITY).getAsString();

            for (ChartEntry oldSpanEntry : prevSpanEntities) {
              Integer oldEntitySpanStart =
                  oldSpanEntry.getEntitySpans().get(0).getLeft();

              if (newEntitySpanEnd < oldEntitySpanStart) {
                // The new entity does not have overlapping span.
                double nthBestEntryScore =
                    curSpanEntities.get(curSpanEntities.size() - 1).getScore();
                double newEntryScore =
                    oldSpanEntry.getScore() + newSpanEntry.getScore();

                if (newEntryScore > nthBestEntryScore) {

                  // Check if the entity is connected with all other entities in
                  // the sentence.
                  boolean newEntityHasRelation = true;
                  for (JsonObject oldEntityObj : oldSpanEntry.getEntities()) {
                    String oldEntityMid =
                        oldEntityObj.get(SentenceKeys.ENTITY).getAsString();
                    if (newEntityMid.equals(oldEntityMid)
                        || !kb.hasRelation(newEntityMid, oldEntityMid)) {
                      newEntityHasRelation = false;
                      break;
                    }
                  }

                  if (newEntityHasRelation) {
                    ChartEntry newChartEntry = new ChartEntry();
                    newChartEntry.setScore(newEntryScore);

                    newChartEntry.getEntitySpans().add(newEntitySpan);
                    newChartEntry.getEntitySpans().addAll(
                        oldSpanEntry.getEntitySpans());

                    newChartEntry.getEntities().add(newEntity);
                    newChartEntry.getEntities().addAll(
                        oldSpanEntry.getEntities());

                    // Insert the new entry using insertion sort algorithm.
                    for (int i = curSpanEntities.size() - 1; i >= 0; i--) {
                      if (i > 0
                          && newEntryScore >= curSpanEntities.get(i).getScore()
                          && newEntryScore <= curSpanEntities.get(i - 1)
                              .getScore()) {
                        curSpanEntities.add(i, newChartEntry);
                        break;
                      } else if (i == 0) {
                        curSpanEntities.add(i, newChartEntry);
                        break;
                      }
                    }

                    if (curSpanEntities.size() > intermediateNbest)
                      curSpanEntities.remove(curSpanEntities.size() - 1);
                  }
                }
              }
            }
          }
        }
        prevSpanEntities = curSpanEntities;
      }

      int numberOfEntityPaths = 1;
      for (ChartEntry chartEntry : prevSpanEntities) {
        List<JsonObject> entities = new ArrayList<>();
        if (chartEntry.getEntities().size() < 0)
          continue;

        for (int i = 0; i < chartEntry.getEntities().size(); i++) {
          JsonObject entity = new JsonObject();
          entity.addProperty(SentenceKeys.ENTITY,
              chartEntry.getEntities().get(i).get(SentenceKeys.ENTITY)
                  .getAsString());
          entity.addProperty(SentenceKeys.SCORE, chartEntry.getEntities()
              .get(i).get(SentenceKeys.SCORE).getAsDouble());
          entity.addProperty(SentenceKeys.START, chartEntry.getEntitySpans()
              .get(i).getLeft());
          entity.addProperty(SentenceKeys.END,
              chartEntry.getEntitySpans().get(i).getRight());
          entity.addProperty(
              SentenceKeys.PHRASE,
              Joiner.on(" ").join(
                  words
                      .subList(chartEntry.getEntitySpans().get(i).getLeft(),
                          chartEntry.getEntitySpans().get(i).getRight() + 1)
                      .stream()
                      .map(x -> x.get(SentenceKeys.WORD_KEY).getAsString())
                      .iterator()));

          if (chartEntry.getEntities().get(i).has("name")) {
            entity.addProperty("name",
                chartEntry.getEntities().get(i).get("name").getAsString());
          }

          if (chartEntry.getEntities().get(i).has("id")) {
            entity.addProperty("id", chartEntry.getEntities().get(i).get("id")
                .getAsString());
          }
          entities.add(entity);
        }
        entities.addAll(existingEntities);
        entities.sort(Comparator.comparing(x -> x.get(SentenceKeys.START)
            .getAsInt()));
        JsonArray entitiesArr = new JsonArray();
        entities.forEach(x -> entitiesArr.add(x));

        // Add entities that are already present.
        existingEntities.forEach(x -> entities.add(x));
        JsonObject newDisambiguatedEntry = new JsonObject();
        newDisambiguatedEntry.add(SentenceKeys.ENTITIES, entitiesArr);
        newDisambiguatedEntry.addProperty(SentenceKeys.SCORE,
            chartEntry.getScore()
                + disambiguatedEntry.get(SentenceKeys.SCORE).getAsDouble());
        newDisambiguatedEntities.add(newDisambiguatedEntry);

        numberOfEntityPaths++;
        if (numberOfEntityPaths > finalNbest)
          break;
      }
    }

    if (newDisambiguatedEntities.size() > 0) {
      newDisambiguatedEntities.sort(Comparator.comparing(
          x -> ((JsonObject) x).get(SentenceKeys.SCORE).getAsDouble())
          .reversed());
    }

    if (newDisambiguatedEntities.size() > finalNbest)
      newDisambiguatedEntities =
          newDisambiguatedEntities.subList(0, finalNbest);
    JsonArray newDisambiguatedEntitiesArray = new JsonArray();
    newDisambiguatedEntities.forEach(x -> newDisambiguatedEntitiesArray.add(x));
    sentence.add(SentenceKeys.DISAMBIGUATED_ENTITIES,
        newDisambiguatedEntitiesArray);
    return;
  }

  private static List<ChartEntry> selectValidChartEntries(
      List<ChartEntry> newSpanEntries, List<JsonObject> existingEntities,
      KnowledgeBase kb) {
    // Current span should not intersect with already existing entity
    // spans.
    List<ChartEntry> potentialEntries = new ArrayList<>();
    for (ChartEntry newSpanEntry : newSpanEntries) {
      String newEntityMid =
          newSpanEntry.getEntities().get(0).get(SentenceKeys.ENTITY)
              .getAsString();
      Integer newEntitySpanStart =
          newSpanEntry.getEntitySpans().get(0).getLeft();
      Integer newEntitySpanEnd =
          newSpanEntry.getEntitySpans().get(0).getRight();
      boolean newSpanEntryIsValid = true;
      for (JsonObject existingEntity : existingEntities) {
        int existingSpanStart =
            existingEntity.get(SentenceKeys.START).getAsInt();
        int existingSpanEnd = existingEntity.get(SentenceKeys.END).getAsInt();
        if (newEntitySpanEnd < existingSpanStart
            || newEntitySpanStart > existingSpanEnd) {
          // pass.
        } else {
          newSpanEntryIsValid = false;
          break;
        }
      }
      if (!newSpanEntryIsValid)
        continue;

      // Current entity should be in relation with existing entities in the
      // sentence.
      for (JsonObject existingEntity : existingEntities) {
        String existingMid =
            existingEntity.get(SentenceKeys.ENTITY).getAsString();
        if (newEntityMid.equals(existingMid)
            || !kb.hasRelation(newEntityMid, existingMid)) {
          newSpanEntryIsValid = false;
          break;
        }
      }
      if (!newSpanEntryIsValid)
        continue;

      potentialEntries.add(newSpanEntry);
    }
    return potentialEntries;
  }

  private static class ChartEntry implements Comparable<ChartEntry> {
    private double score;
    private List<JsonObject> selectedEntities = new ArrayList<>();
    private List<Pair<Integer, Integer>> entitySpans = new ArrayList<>();

    public ChartEntry() {}

    public double getScore() {
      return score;
    }

    public void setScore(double score) {
      this.score = score;
    }

    public List<JsonObject> getEntities() {
      return selectedEntities;
    }

    public List<Pair<Integer, Integer>> getEntitySpans() {
      return entitySpans;
    }

    @Override
    public int compareTo(ChartEntry x) {
      return Double.compare(score, x.score);
    }

    @Override
    public String toString() {
      return Objects
          .toStringHelper(this.getClass())
          .add("score", score)
          .add("spans", entitySpans)
          .add(
              "entities",
              selectedEntities
                  .stream()
                  .map(
                      x -> Pair.of(x.get("entity").getAsString(), x.get("name")
                          .getAsString())).collect(Collectors.toList()))
          .toString();
    }
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
