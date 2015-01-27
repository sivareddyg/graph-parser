package in.sivareddy.ml.learning;

import in.sivareddy.ml.basic.Feature;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class StructuredPercepton {

  private Map<Feature, Double> weightVector;
  private Map<Feature, Double> cumulativeWeightVector;
  private Map<Feature, Integer> updateFrequency;

  public StructuredPercepton() {
    weightVector = Maps.newHashMap();
    cumulativeWeightVector = Maps.newHashMap();
    updateFrequency = Maps.newHashMap();
  }

  public synchronized void setWeightIfAbsent(Feature feature, Double weight) {
    if (!weightVector.containsKey(feature)) {
      weightVector.put(feature, weight);
      cumulativeWeightVector.put(feature, weight);
      updateFrequency.put(feature, 1);
    }
  }

  public synchronized Double getScoreTraining(Set<Feature> featureVector) {
    Double score = 0.0;
    for (Feature feature : featureVector) {
      Double value = feature.getFeatureValue();
      Double weight =
          weightVector.containsKey(feature) ? weightVector.get(feature) : 0.0;
      score += value * weight;
    }
    return score;
  }

  public synchronized Double getScoreTesting(Set<Feature> featureVector) {
    Double score = 0.0;
    for (Feature feature : featureVector) {
      Double value = feature.getFeatureValue();
      Double weight =
          cumulativeWeightVector.containsKey(feature) ? cumulativeWeightVector
              .get(feature) / updateFrequency.get(feature) : 0.0;
      score += value * weight;
    }
    return score;
  }

  // simple perceptron update with feature-wise averaging
  public synchronized void updateWeightVector(Set<Feature> goldFeatVec,
      Set<Feature> predFeatVec) {

    Set<Feature> features = Sets.newHashSet();
    Map<Feature, Double> goldFeatVecMap = Maps.newHashMap();
    for (Feature feature : goldFeatVec) {
      if (Math.abs(feature.getFeatureValue()) > 0.0) {
        goldFeatVecMap.put(feature, feature.getFeatureValue());
        features.add(feature);
      }
    }
    Map<Feature, Double> predFeatVecMap = Maps.newHashMap();
    for (Feature feature : predFeatVec) {
      if (Math.abs(feature.getFeatureValue()) > 0.0) {
        predFeatVecMap.put(feature, feature.getFeatureValue());
        features.add(feature);
      }
    }

    for (Feature feature : features) {
      Double goldFeatValue =
          goldFeatVecMap.containsKey(feature) ? goldFeatVecMap.get(feature)
              : 0.0;
      Double predFeatValue =
          predFeatVecMap.containsKey(feature) ? predFeatVecMap.get(feature)
              : 0.0;
      double difference = goldFeatValue - predFeatValue;

      Double oldWeight =
          weightVector.containsKey(feature) ? weightVector.get(feature) : 0.0;
      Double newWeight = oldWeight + difference;
      weightVector.put(feature, newWeight);

      double oldCumultativeWeight =
          cumulativeWeightVector.containsKey(feature) ? cumulativeWeightVector
              .get(feature) : 0.0;
      Double newCumulativeWeight = oldCumultativeWeight + newWeight;
      cumulativeWeightVector.put(feature, newCumulativeWeight);

      int oldFreqCount =
          updateFrequency.containsKey(feature) ? updateFrequency.get(feature)
              : 0;
      Integer newFreqCount = oldFreqCount + 1;
      updateFrequency.put(feature, newFreqCount);
    }
  }

  public synchronized void printFeatureWeights(Set<Feature> featVec,
      Logger logger) {
    List<Pair<Double, Feature>> feats = Lists.newArrayList();
    for (Feature feature : featVec) {
      Double weight =
          weightVector.containsKey(feature) ? weightVector.get(feature) : 0.0;
      feats.add(Pair.of(weight, feature));
    }
    Collections.sort(feats, Collections.reverseOrder());
    logger.debug("Features ==== ");
    for (Pair<Double, Feature> featPair : feats) {
      logger.debug(featPair.getRight() + ":" + featPair.getLeft());
    }
    logger.debug("====");
  }

  public synchronized void printFeatureWeightsTesting(Set<Feature> featVec,
      Logger logger) {
    List<Pair<Double, Feature>> feats = Lists.newArrayList();
    for (Feature feature : featVec) {
      Double weight =
          cumulativeWeightVector.containsKey(feature) ? cumulativeWeightVector
              .get(feature) / updateFrequency.get(feature) : 0.0;
      feats.add(Pair.of(weight, feature));
    }
    Collections.sort(feats, Collections.reverseOrder());
    logger.debug("Features ==== ");
    for (Pair<Double, Feature> featPair : feats) {
      logger.debug(featPair.getRight() + ":" + featPair.getLeft());
    }
    logger.debug("====");
  }

  public synchronized boolean containsFeature(Feature feature) {
    return weightVector.containsKey(feature);
  }
}
