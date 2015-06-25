package in.sivareddy.graphparser.util;

import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DisambiguateEntities {

  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  public static final Set<String> PROPER_NOUNS = Sets.newHashSet("NNP", "NNPS");

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

  public static List<JsonObject> cykStyledDisambiguation(JsonObject sentence,
      int initialNbest, int intermediateNbest, int finalNbest,
      boolean entityHasReadableId, KnowledgeBase kb) {
    List<JsonObject> returnSentences = new ArrayList<>();
    if (!sentence.has(SentenceKeys.MATCHED_ENTITIES)) {
      returnSentences.add(jsonParser.parse(gson.toJson(sentence))
          .getAsJsonObject());
      returnSentences.get(0).add(SentenceKeys.ENTITIES, new JsonArray());
      return returnSentences;
    }


    List<JsonObject> words = new ArrayList<>();
    sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray()
        .forEach(x -> words.add(x.getAsJsonObject()));

    Map<Pair<Integer, Integer>, List<ChartEntry>> spanToEntities =
        new HashMap<>();
    Map<Integer, List<ChartEntry>> spanStartToEntities = new HashMap<>();

    for (JsonElement matchedEntity : sentence
        .get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
      JsonObject matchedEntityObj = matchedEntity.getAsJsonObject();
      if (!matchedEntityObj.has(SentenceKeys.RANKED_ENTITIES))
        continue;

      int spanStart = matchedEntityObj.get(SentenceKeys.START).getAsInt();
      int spanEnd = matchedEntityObj.get(SentenceKeys.END).getAsInt();
      Pair<Integer, Integer> span = Pair.of(spanStart, spanEnd);

      // Entity span should start with a named entity.
      String startNer =
          words.get(spanStart).get(SentenceKeys.NER_KEY).getAsString();
      String startTag =
          words.get(spanStart).get(SentenceKeys.POS_KEY).getAsString();
      if (startNer.equals("O") && !PROPER_NOUNS.contains(startTag))
        continue;


      String endNer =
          words.get(spanEnd).get(SentenceKeys.NER_KEY).getAsString();
      String endTag =
          words.get(spanEnd).get(SentenceKeys.POS_KEY).getAsString();

      // Entity end should not contain a weird word. - strange rule. Don't know
      // why this works.
      if (endNer.equals("O") && PROPER_NOUNS.contains(endTag))
        continue;

      // Entity span should not be preceded by an entity that has the same ner tag.
      if (spanStart > 0) {
        String prevNer =
            words.get(spanStart - 1).get(SentenceKeys.NER_KEY).getAsString();
        String prevTag =
            words.get(spanStart - 1).get(SentenceKeys.POS_KEY).getAsString();
        if ((!prevNer.equals("O") && prevNer.equals(startNer))
            || PROPER_NOUNS.contains(prevTag))
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

    List<ChartEntry> prevSpanEntities = new ArrayList<>();
    for (int spanStart = words.size() - 1; spanStart >= 0; spanStart--) {
      List<ChartEntry> curSpanEntities = new ArrayList<>();
      curSpanEntities.addAll(prevSpanEntities);
      if (spanStartToEntities.containsKey(spanStart)) {
        curSpanEntities.addAll(spanStartToEntities.get(spanStart));
        Collections.sort(curSpanEntities, Collections.reverseOrder());
        if (curSpanEntities.size() > intermediateNbest)
          curSpanEntities = curSpanEntities.subList(0, intermediateNbest);

        for (ChartEntry newSpanEntry : spanStartToEntities.get(spanStart)) {
          for (ChartEntry oldSpanEntry : prevSpanEntities) {
            JsonObject newEntity = newSpanEntry.getEntities().get(0);
            Pair<Integer, Integer> newEntitySpan =
                newSpanEntry.getEntitySpans().get(0);
            Integer newEntitySpanEnd = newEntitySpan.getRight();

            Integer oldEntitySpanStart =
                oldSpanEntry.getEntitySpans().get(0).getLeft();

            if (newEntitySpanEnd < oldEntitySpanStart) {
              // The new entity does not have overlapping span.
              String newEntityMid =
                  newEntity.get(SentenceKeys.ENTITY).getAsString();
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
                  newChartEntry.getEntities()
                      .addAll(oldSpanEntry.getEntities());

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
      JsonObject newSentence =
          jsonParser.parse(gson.toJson(sentence)).getAsJsonObject();
      newSentence.remove(SentenceKeys.MATCHED_ENTITIES);
      JsonArray entities = new JsonArray();
      for (int i = 0; i < chartEntry.getEntities().size(); i++) {
        JsonObject entity = new JsonObject();
        entity.addProperty(SentenceKeys.ENTITY, chartEntry.getEntities().get(i)
            .get(SentenceKeys.ENTITY).getAsString());
        entity.addProperty(SentenceKeys.SCORE, chartEntry.getEntities().get(i)
            .get(SentenceKeys.SCORE).getAsDouble());
        entity.addProperty(SentenceKeys.START,
            chartEntry.getEntitySpans().get(i).getLeft());
        entity.addProperty(SentenceKeys.END, chartEntry.getEntitySpans().get(i)
            .getRight());
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
          entity.addProperty("name", chartEntry.getEntities().get(i)
              .get("name").getAsString());
        }

        if (chartEntry.getEntities().get(i).has("id")) {
          entity.addProperty("id", chartEntry.getEntities().get(i).get("id")
              .getAsString());
        }

        entities.add(entity);
      }
      newSentence.add(SentenceKeys.ENTITIES, entities);
      returnSentences.add(newSentence);

      numberOfEntityPaths++;
      if (numberOfEntityPaths > finalNbest)
        break;
    }

    if (returnSentences.size() == 0) {
      returnSentences.add(jsonParser.parse(gson.toJson(sentence))
          .getAsJsonObject());
      returnSentences.get(0).add(SentenceKeys.ENTITIES, new JsonArray());
    }
    return returnSentences;
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
