package deplambda.entityannotator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import deplambda.entityannotator.EntityScorer.EntityCandidate;
import deplambda.others.SentenceKeys;
import in.sivareddy.graphparser.util.DisambiguateEntities;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.util.ProcessStreamInterface;

public class EntityDisambiguator extends ProcessStreamInterface {

  private final int nbest;
  private final EntityScorer scorer;
  private final KnowledgeBase kb;

  public EntityDisambiguator(EntityScorer scorer, KnowledgeBase kb, int nbest) {
    this.scorer = scorer;
    this.nbest = nbest;
    this.kb = kb;
  }

  @Override
  public void processSentence(JsonObject jsonSentence) {
    List<EntityCandidate> candidates = scorer.extractCandidates(jsonSentence);
    scorer.rankCandidates(candidates, true);
    int endIndex = nbest < candidates.size() ? nbest : candidates.size();
    jsonSentence.remove(SentenceKeys.MATCHED_ENTITIES);

    // Creating input for the next stage {@code
    // in.sivareddy.graphparser.util.DisambiguateEntities}
    Set<JsonObject> newMatchedEntitiesSet = new HashSet<>();

    for (int i = 0; i < endIndex; i++) {
      EntityCandidate candidate = candidates.get(i);
      JsonObject matchedEntity = candidate.getMatchedEntity();
      matchedEntity.remove(SentenceKeys.RANKED_ENTITIES);
      matchedEntity.add(SentenceKeys.RANKED_ENTITIES, new JsonArray());
      newMatchedEntitiesSet.add(matchedEntity);
    }

    List<JsonObject> newMatchedEntitiesArr = new ArrayList<>();
    newMatchedEntitiesArr.addAll(newMatchedEntitiesSet);
    newMatchedEntitiesArr.sort(Comparator.comparing(x -> x.get(
        SentenceKeys.START).getAsInt()));
    JsonArray newMatchedEntities = new JsonArray();
    newMatchedEntitiesArr.forEach(x -> newMatchedEntities.add(x));

    for (int i = 0; i < endIndex; i++) {
      EntityCandidate candidate = candidates.get(i);
      JsonObject matchedEntity = candidate.getMatchedEntity();
      JsonArray rankedEntities =
          matchedEntity.get(SentenceKeys.RANKED_ENTITIES).getAsJsonArray();

      JsonObject rankedEntity = candidate.getRankedEntity();
      Double score = candidate.getScore();
      rankedEntity.addProperty(SentenceKeys.SCORE, score);
      rankedEntities.add(candidate.getRankedEntity());
    }

    jsonSentence.add(SentenceKeys.MATCHED_ENTITIES, newMatchedEntities);

    // Use this if you want to allow multiple entities in the sentence that may
    // not have any relation.
    // latticeBasedDisambiguation(jsonSentence, nbest);


    DisambiguateEntities.latticeBasedDisambiguation(jsonSentence, nbest, nbest,
        nbest, false, kb, false, false, false, false, false, false, false,
        false, false);
  }

  private static class LatticeEntry {
    int start = -1;
    int end = -1;
    double score = 0.0;
    List<Pair<JsonObject, JsonObject>> matchedAndRankedEntities =
        new ArrayList<>();

    public LatticeEntry copy() {
      LatticeEntry copy = new LatticeEntry();
      copy.start = start;
      copy.end = end;
      copy.score = score;
      copy.matchedAndRankedEntities.addAll(matchedAndRankedEntities);
      return copy;
    }
  }

  /**
   * This method builds a lattice based on entity spans and choose the paths
   * that have highest summation of individual span scores.
   * 
   * TODO: This method prefers having multiple entities in a sentence even
   * though they may not have any relation. For example, francisco coronado, is
   * originally a single entity with high score. But if we break it into
   * francisco and coronado, the sum of the components is more than when its
   * together. Use mutual information of the entities as additional feature to
   * avoid this.
   * 
   * @param jsonSentence
   * @param nbest
   */
  public static void latticeBasedDisambiguation(JsonObject jsonSentence,
      int nbest) {
    JsonArray matchedEntities =
        jsonSentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();

    List<LatticeEntry> latticeEntries = new ArrayList<>();
    for (JsonElement matchedElement : matchedEntities) {
      JsonObject matchedEntity = matchedElement.getAsJsonObject();
      List<JsonObject> rankedEntities = new ArrayList<>();
      matchedEntity.get(SentenceKeys.RANKED_ENTITIES).getAsJsonArray()
          .forEach(x -> rankedEntities.add(x.getAsJsonObject()));
      int startIndex = matchedEntity.get(SentenceKeys.START).getAsInt();
      int endIndex = matchedEntity.get(SentenceKeys.END).getAsInt();


      List<LatticeEntry> newLatticeEntries = new ArrayList<>();

      // Add the current span to the lattice.
      for (JsonObject rankedEntity : rankedEntities) {
        LatticeEntry latticeEntry = new LatticeEntry();
        latticeEntry.start = startIndex;
        latticeEntry.end = endIndex;
        latticeEntry.matchedAndRankedEntities.add(Pair.of(matchedEntity,
            rankedEntity));
        latticeEntry.score = rankedEntity.get(SentenceKeys.SCORE).getAsDouble();
        newLatticeEntries.add(latticeEntry);
      }

      for (LatticeEntry existingLatticeEntry : latticeEntries) {
        // Check that the lattice entry's span does not overlap with the
        // current entry.
        if (existingLatticeEntry.end >= startIndex)
          continue;

        for (JsonObject rankedEntity : rankedEntities) {
          LatticeEntry newLatticeEntry = existingLatticeEntry.copy();
          newLatticeEntry.end = endIndex;
          newLatticeEntry.score +=
              rankedEntity.get(SentenceKeys.SCORE).getAsDouble();
          newLatticeEntry.matchedAndRankedEntities.add(Pair.of(matchedEntity,
              rankedEntity));
          newLatticeEntries.add(newLatticeEntry);
        }
      }

      latticeEntries.addAll(newLatticeEntries);
    }

    latticeEntries.sort(Comparator.comparing(x -> -1 * x.score));
    JsonArray disambiguatedEntities = new JsonArray();
    jsonSentence
        .add(SentenceKeys.DISAMBIGUATED_ENTITIES, disambiguatedEntities);
    Integer finalSize =
        nbest < latticeEntries.size() ? nbest : latticeEntries.size();
    for (LatticeEntry latticeEntry : latticeEntries.subList(0, finalSize)) {
      JsonObject disambiguatedEntity = new JsonObject();
      disambiguatedEntities.add(disambiguatedEntity);

      JsonArray spanEntities = new JsonArray();
      disambiguatedEntity.add(SentenceKeys.ENTITIES, spanEntities);
      disambiguatedEntity.addProperty(SentenceKeys.SCORE, latticeEntry.score);

      if (latticeEntry.matchedAndRankedEntities.size() > 1) {
        System.err.println(latticeEntry.matchedAndRankedEntities);
      }
      for (Pair<JsonObject, JsonObject> matchedAndRankedEntity : latticeEntry.matchedAndRankedEntities) {
        JsonObject matchedEntity = matchedAndRankedEntity.getLeft();
        JsonObject rankedEntity = matchedAndRankedEntity.getRight();

        JsonObject entity = new JsonObject();
        spanEntities.add(entity);
        entity.add(SentenceKeys.START, matchedEntity.get(SentenceKeys.START));
        entity.add(SentenceKeys.END, matchedEntity.get(SentenceKeys.END));
        entity.add(SentenceKeys.PHRASE, matchedEntity.get(SentenceKeys.PHRASE));
        entity.add(SentenceKeys.ENTITY, rankedEntity.get(SentenceKeys.ENTITY));
        entity.add(SentenceKeys.SCORE, rankedEntity.get(SentenceKeys.SCORE));
        entity.add(SentenceKeys.ENTITY_NAME,
            rankedEntity.get(SentenceKeys.ENTITY_NAME));
        if (rankedEntity.has(SentenceKeys.ENTITY_ID))
          entity.add(SentenceKeys.ENTITY_ID,
              rankedEntity.get(SentenceKeys.ENTITY_ID));
      }
    }
  }
}
