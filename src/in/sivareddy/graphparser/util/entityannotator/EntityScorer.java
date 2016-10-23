package in.sivareddy.graphparser.util.entityannotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import in.sivareddy.ml.basic.AbstractFeature;
import in.sivareddy.ml.basic.Feature;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

public class EntityScorer extends ProcessStreamInterface {
  public StructuredPercepton ranker;

  private final boolean useStartIndex;
  private final boolean useEntityLength;
  private final boolean useNameOverlap;
  private final boolean useAPIScore;
  private final boolean hasId;
  private final boolean useUnigramPos;
  private final boolean useBigramPos;
  private final boolean useNextPos;
  private final boolean usePrevPos;

  private final boolean useNextWord;
  private final boolean usePrevWord;

  private final boolean useWordEntity;
  private final boolean useWordBigramEntity;

  private final boolean useWord;
  private final boolean useWordBigram;

  public EntityScorer(StructuredPercepton ranker, boolean useStartIndex,
      boolean useEntityLength, boolean useNameOverlap, boolean useAPIScore,
      boolean hasId, boolean useUnigramPos, boolean useBigramPos,
      boolean useNextWordPos, boolean usePrevWordPos, boolean useWord,
      boolean useWordBigram, boolean useNextWord, boolean usePrevWord,
      boolean useWordEntity, boolean useWordBigramEntity) {
    this.ranker = ranker;
    this.useStartIndex = useStartIndex;
    this.useEntityLength = useEntityLength;
    this.useNameOverlap = useNameOverlap;
    this.useAPIScore = useAPIScore;
    this.hasId = hasId;
    this.useUnigramPos = useUnigramPos;
    this.useBigramPos = useBigramPos;
    this.useNextPos = useNextWordPos;
    this.usePrevPos = usePrevWordPos;

    this.useWord = useWord;
    this.useWordBigram = useWordBigram;
    this.useNextWord = useNextWord;
    this.usePrevWord = usePrevWord;

    this.useWordEntity = useWordEntity;
    this.useWordBigramEntity = useWordBigramEntity;

  }

  public StructuredPercepton getRanker() {
    return ranker;
  }

  public void setRanker(StructuredPercepton ranker) {
    this.ranker = ranker;
  }

  private static class StartIndexFeature extends AbstractFeature {
    private static final long serialVersionUID = -1603303654164709257L;
    private static List<String> key = Lists.newArrayList("StartIndexFeature");

    public StartIndexFeature(Integer value) {
      super(key, value.doubleValue());
    }
  }

  private static class EntityLengthFeature extends AbstractFeature {
    private static final long serialVersionUID = 2586727947069724615L;
    private static List<String> key = Lists.newArrayList("EntityLengthFeature");

    public EntityLengthFeature(Integer value) {
      super(key, value.doubleValue());
    }
  }

  private static class NameOverlapFeature extends AbstractFeature {
    private static final long serialVersionUID = -5521564994924138321L;
    private static List<String> key = Lists.newArrayList("NameOverlapFeature");

    public NameOverlapFeature(Double value) {
      super(key, value);
    }
  }

  private static class ApiScoreFeature extends AbstractFeature {
    private static final long serialVersionUID = 5501263596714596024L;
    private static List<String> key = Lists.newArrayList("ApiScoreFeature");

    public ApiScoreFeature(Double value) {
      super(key, value);
    }
  }

  private static class HasIdFeature extends AbstractFeature {
    private static final long serialVersionUID = 2915136520473671521L;
    private static List<String> key = Lists.newArrayList("HasIdFeature");

    public HasIdFeature(boolean value) {
      super(key, value ? 1.0 : 0.0);
    }
  }

  private static class PosFeature extends AbstractFeature {
    private static final long serialVersionUID = -7612207746507878774L;

    public PosFeature(String key) {
      super(Lists.newArrayList(key), 1.0);
    }
  }

  private static class PosBigramFeature extends AbstractFeature {
    private static final long serialVersionUID = -5426648812488566835L;

    public PosBigramFeature(String pos1, String pos2) {
      super(Lists.newArrayList(pos1, pos2), 1.0);
    }
  }

  private static class PrevPosFeature extends AbstractFeature {
    private static final long serialVersionUID = 5340793221164946964L;

    public PrevPosFeature(String key) {
      super(Lists.newArrayList(key), 1.0);
    }
  }

  private static class NextPosFeature extends AbstractFeature {
    private static final long serialVersionUID = -2174201667979620334L;

    public NextPosFeature(String key) {
      super(Lists.newArrayList(key), 1.0);
    }
  }

  private static class PrevWordFeature extends AbstractFeature {
    private static final long serialVersionUID = -811248159671102541L;

    public PrevWordFeature(String key) {
      super(Lists.newArrayList(key), 1.0);
    }
  }

  private static class NextWordFeature extends AbstractFeature {
    private static final long serialVersionUID = 2647393103839587958L;

    public NextWordFeature(String key) {
      super(Lists.newArrayList(key), 1.0);
    }
  }

  private static class WordFeature extends AbstractFeature {
    private static final long serialVersionUID = -5073022001547778707L;

    public WordFeature(String word) {
      super(Lists.newArrayList(word), 1.0);
    }
  }

  private static class WordBigramFeature extends AbstractFeature {
    private static final long serialVersionUID = -1885828482951124152L;

    public WordBigramFeature(String word1, String word2) {
      super(Lists.newArrayList(word1, word2), 1.0);
    }
  }


  private static class WordEntityFeature extends AbstractFeature {
    private static final long serialVersionUID = 4172056880966783057L;

    public WordEntityFeature(String word, String entity) {
      super(Lists.newArrayList(word, entity), 1.0);
    }
  }

  private static class WordBigramEntityFeature extends AbstractFeature {
    private static final long serialVersionUID = -2744932165595796809L;

    public WordBigramEntityFeature(String word1, String word2, String entity) {
      super(Lists.newArrayList(word1, word2, entity), 1.0);
    }
  }

  public static class EntityCandidate implements Comparable<EntityCandidate> {
    private Boolean isGold = false;
    private List<Feature> features = new ArrayList<>();
    private final JsonObject matchedEntity;
    private final JsonObject rankedEntity;
    private Double score = 0.0;

    public EntityCandidate(JsonObject matchedEntity, JsonObject rankedEntity) {
      this.matchedEntity = matchedEntity;
      this.rankedEntity = rankedEntity;
    }

    public boolean isGold() {
      return isGold;
    }

    public void setIsGold(boolean val) {
      isGold = val;
    }

    public List<Feature> getFeatures() {
      return features;
    }

    public JsonObject getMatchedEntity() {
      return matchedEntity;
    }

    public JsonObject getRankedEntity() {
      return rankedEntity;
    }

    public Double getScore() {
      return score;
    }

    @Override
    public int compareTo(EntityCandidate arg0) {
      return score.compareTo(arg0.score);
    }
  }

  public List<EntityCandidate> extractCandidates(JsonObject jsonSentence) {

    List<EntityCandidate> candidates = new ArrayList<>();
    if (jsonSentence.has(SentenceKeys.MATCHED_ENTITIES)) {
      for (JsonElement matchedEntityElm : jsonSentence.get(
          SentenceKeys.MATCHED_ENTITIES).getAsJsonArray()) {
        JsonObject matchedEntity = matchedEntityElm.getAsJsonObject();
        if (matchedEntity.has(SentenceKeys.RANKED_ENTITIES)) {
          for (JsonElement rankedEntityElm : matchedEntity.get(
              SentenceKeys.RANKED_ENTITIES).getAsJsonArray()) {
            JsonObject rankedEntity = rankedEntityElm.getAsJsonObject();
            candidates.add(extractEntityCandidate(jsonSentence, matchedEntity,
                rankedEntity));
          }
        }
      }
    }
    return candidates;
  }

  private Double getF1Overlap(String s1, String s2) {
    Set<String> s1Words =
        Sets.newHashSet(Splitter.on(CharMatcher.WHITESPACE).split(s1));
    Set<String> s2Words =
        Sets.newHashSet(Splitter.on(CharMatcher.WHITESPACE).split(s2));
    int total1 = s1Words.size();
    int total2 = s2Words.size();
    s1Words.retainAll(s2Words);
    int intersection = s1Words.size();
    double p1 = (intersection + 0.0) / total1;
    double p2 = (intersection + 0.0) / total2;
    double mean = 0.0;
    if (p1 + p2 > 0.0005) {
      mean = 2.0 * p1 * p2 / (p1 + p2);
    }
    return mean;
  }

  private EntityCandidate extractEntityCandidate(JsonObject jsonSentence,
      JsonObject matchedEntity, JsonObject rankedEntity) {
    EntityCandidate candidate =
        new EntityCandidate(matchedEntity, rankedEntity);

    String currentEntity = rankedEntity.get(SentenceKeys.ENTITY).getAsString();

    Set<String> goldEntities = new HashSet<>();
    if (jsonSentence.has(SentenceKeys.GOLD_MID)) {
      goldEntities.add(jsonSentence.get(SentenceKeys.GOLD_MID).getAsString());
    }

    if (jsonSentence.has(SentenceKeys.GOLD_MIDS)) {
      jsonSentence.get(SentenceKeys.GOLD_MIDS).getAsJsonArray()
          .forEach(x -> goldEntities.add(x.getAsString()));
    }
    candidate.setIsGold(goldEntities.contains(currentEntity));

    List<Feature> features = candidate.getFeatures();

    if (useStartIndex) {
      int startIndex = matchedEntity.get(SentenceKeys.START).getAsInt();
      features.add(new StartIndexFeature(startIndex));
    }

    if (useEntityLength) {
      int startIndex = matchedEntity.get(SentenceKeys.START).getAsInt();
      int endIndex = matchedEntity.get(SentenceKeys.END).getAsInt() + 1;
      features.add(new EntityLengthFeature(endIndex - startIndex));
    }

    if (useAPIScore) {
      double score = rankedEntity.get(SentenceKeys.SCORE).getAsDouble();
      features.add(new ApiScoreFeature(Math.log(score)));
    }

    if (useNameOverlap) {
      String phrase =
          matchedEntity.get(SentenceKeys.PHRASE).getAsString().toLowerCase();
      String name =
          rankedEntity.get(SentenceKeys.ENTITY_NAME).getAsString()
              .toLowerCase();
      features.add(new NameOverlapFeature(getF1Overlap(phrase, name)));
    }

    if (hasId) {
      features.add(new HasIdFeature(rankedEntity.has(SentenceKeys.ENTITY_ID)
          && rankedEntity.get(SentenceKeys.ENTITY_ID).getAsString()
              .startsWith("/en/")));
    }

    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    if (usePrevPos) {
      int startIndex = matchedEntity.get(SentenceKeys.START).getAsInt();
      String prevPos =
          startIndex == 0 ? "_BOS_" : words.get(startIndex - 1)
              .getAsJsonObject().get(SentenceKeys.POS_KEY).getAsString();
      features.add(new PrevPosFeature(prevPos));
    }

    if (useNextPos) {
      int endIndex = matchedEntity.get(SentenceKeys.END).getAsInt();
      String nextPos =
          (endIndex + 1) == words.size() ? "_EOS_" : words.get(endIndex + 1)
              .getAsJsonObject().get(SentenceKeys.POS_KEY).getAsString();
      features.add(new NextPosFeature(nextPos));
    }

    if (useUnigramPos) {
      for (String pos : Splitter.on(" ").split(
          matchedEntity.get(SentenceKeys.PATTERN).getAsString())) {
        features.add(new PosFeature(pos));
      }
    }

    if (useBigramPos) {
      List<String> posList =
          Lists.newArrayList(Splitter.on(" ").split(
              matchedEntity.get(SentenceKeys.PATTERN).getAsString()));
      posList.add(0, "_BOS_");
      posList.add("_EOS_");
      for (int i = 0; i < posList.size() - 1; i++) {
        features.add(new PosBigramFeature(posList.get(i), posList.get(i + 1)));
      }
    }

    if (useWord) {
      for (String phraseWord : Splitter.on(" ").split(
          matchedEntity.get(SentenceKeys.PHRASE).getAsString().toLowerCase())) {
        features.add(new WordFeature(phraseWord));
      }
    }

    if (useWordBigram) {
      List<String> wordList =
          Lists.newArrayList(Splitter.on(" ").split(
              matchedEntity.get(SentenceKeys.PHRASE).getAsString()
                  .toLowerCase()));
      wordList.add(0, "_BOE_"); // beginning of entity
      wordList.add("_EOE_"); // end of entity
      for (int i = 0; i < wordList.size() - 1; i++) {
        features
            .add(new WordBigramFeature(wordList.get(i), wordList.get(i + 1)));
      }
    }

    if (usePrevWord) {
      int startIndex = matchedEntity.get(SentenceKeys.START).getAsInt();
      String prevPos =
          startIndex == 0 ? "_BOS_" : words.get(startIndex - 1)
              .getAsJsonObject().get(SentenceKeys.WORD_KEY).getAsString();
      features.add(new PrevWordFeature(prevPos));
    }

    if (useNextWord) {
      int endIndex = matchedEntity.get(SentenceKeys.END).getAsInt();
      String nextPos =
          (endIndex + 1) == words.size() ? "_EOS_" : words.get(endIndex + 1)
              .getAsJsonObject().get(SentenceKeys.WORD_KEY).getAsString();
      features.add(new NextWordFeature(nextPos));
    }

    if (useWordEntity) {
      for (String phraseWord : Splitter.on(" ").split(
          matchedEntity.get(SentenceKeys.PHRASE).getAsString().toLowerCase())) {
        features.add(new WordEntityFeature(phraseWord, currentEntity));
      }
    }

    if (useWordBigramEntity) {
      List<String> wordList =
          Lists.newArrayList(Splitter.on(" ").split(
              matchedEntity.get(SentenceKeys.PHRASE).getAsString()
                  .toLowerCase()));
      wordList.add(0, "_BOE_"); // beginning of entity
      wordList.add("_EOE_"); // end of entity
      for (int i = 0; i < wordList.size() - 1; i++) {
        features.add(new WordBigramEntityFeature(wordList.get(i), wordList
            .get(i + 1), currentEntity));
      }
    }

    return candidate;
  }

  public void rankCandidates(List<EntityCandidate> candidates, boolean testing) {
    for (EntityCandidate candidate : candidates) {
      if (testing) {
        candidate.score = ranker.getScoreTesting(candidate.getFeatures());
      } else {
        candidate.score = ranker.getScoreTraining(candidate.getFeatures());
      }
    }
    candidates.sort(Comparator.reverseOrder());
  }

  public EntityCandidate getSurrogateGoldCandidate(
      List<EntityCandidate> sortedCandidates) {
    for (EntityCandidate candidate : sortedCandidates) {
      if (candidate.isGold()) {
        return candidate;
      }
    }
    return null;
  }

  /*
   * Training step.
   * 
   * (non-Javadoc)
   * 
   * @see
   * in.sivareddy.util.ProcessStreamInterface#processSentence(com.google.gson
   * .JsonObject)
   */
  @Override
  public void processSentence(JsonObject jsonSentence) {
    List<EntityCandidate> candidates = extractCandidates(jsonSentence);
    rankCandidates(candidates, false);
    EntityCandidate goldCandidate = getSurrogateGoldCandidate(candidates);
    if (goldCandidate != null) {
      ranker.updateWeightVector(1, goldCandidate.features, 1,
          candidates.get(0).features);
    }
  }

  public static class Evaluator extends ProcessStreamInterface {
    Set<String> positiveSentences = new HashSet<>();
    Set<String> totalSentences = new HashSet<>();

    private final EntityScorer entityScorer;

    public Evaluator(EntityScorer entityScorer) {
      this.entityScorer = entityScorer;
    }

    public synchronized void updatePositive(String sentence) {
      positiveSentences.add(sentence);
    }

    public synchronized void updateTotal(String sentence) {
      totalSentences.add(sentence);
    }

    @Override
    public void processSentence(JsonObject jsonSentence) {
      List<EntityCandidate> candidates =
          entityScorer.extractCandidates(jsonSentence);
      String sentence =
          jsonSentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
      entityScorer.rankCandidates(candidates, true);
      if (candidates.size() > 0) {
        EntityCandidate bestPrediction = candidates.get(0);
        String predictedMid =
            bestPrediction.getRankedEntity().get(SentenceKeys.ENTITY)
                .getAsString();

        Set<String> goldEntities = new HashSet<>();
        if (jsonSentence.has(SentenceKeys.GOLD_MID)) {
          goldEntities.add(jsonSentence.get(SentenceKeys.GOLD_MID)
              .getAsString());
        }

        if (jsonSentence.has(SentenceKeys.GOLD_MIDS)) {
          jsonSentence.get(SentenceKeys.GOLD_MIDS).getAsJsonArray()
              .forEach(x -> goldEntities.add(x.getAsString()));
        }

        if (goldEntities.contains(predictedMid)) {
          updatePositive(sentence);
        }
      }
      updateTotal(sentence);
    }

    public void printResults() {
      System.out.println(String.format("%d/%d\t%.2f", positiveSentences.size(),
          totalSentences.size(), (positiveSentences.size() + 0.0)
              / totalSentences.size() * 100.0));
      System.out.println("Failed Sentences: ");

      totalSentences.removeAll(positiveSentences);
      for (String negativeSentence : positiveSentences) {
        System.out.println(negativeSentence);
      }
    }

    public double getAccuracy() {
      if (totalSentences.size() > 0) {
        return (positiveSentences.size() + 0.0) / totalSentences.size() * 100.0;
      }
      return 0.0;
    }
  }

  public double evaluate(List<JsonObject> jsonSentences, int nthreads)
      throws IOException, InterruptedException {
    Evaluator evaluator = new Evaluator(this);
    evaluator.processList(jsonSentences, null, nthreads, false);
    return evaluator.getAccuracy();
  }

  public void saveModel(String fileName) throws IOException {
    ranker.saveModel(fileName);
  }

}
