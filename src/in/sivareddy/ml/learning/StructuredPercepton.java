package in.sivareddy.ml.learning;

import in.sivareddy.ml.basic.Feature;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class StructuredPercepton implements Serializable {
  private static final long serialVersionUID = 5892061696728510700L;
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
    Double weight;
    for (Feature feature : featureVector) {
      weight = weightVector.get(feature);
      score += feature.getFeatureValue() * (weight == null ? 0.0 : weight);
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

  // Simple perceptron update with feature-wise averaging
  // This is different from traditional averaging. This is found to be working
  // better than averaged perceptron.
  public synchronized void updateWeightVector(int goldParsesSize,
      List<Feature> goldFeatVec, int wrongParsesSize, List<Feature> predFeatVec) {
    Double goldParsesWeight = 1.0 / goldParsesSize;
    Double wrongParsesWeight = 1.0 / wrongParsesSize;

    Set<Feature> features = Sets.newHashSet();
    Map<Feature, Double> goldFeatVecMap = Maps.newHashMap();
    for (Feature feature : goldFeatVec) {
      if (Math.abs(feature.getFeatureValue()) > 0.0) {
        if (!goldFeatVecMap.containsKey(feature)) {
          goldFeatVecMap.put(feature, 0.0);
        }
        goldFeatVecMap.put(feature, goldFeatVecMap.get(feature)
            + goldParsesWeight * feature.getFeatureValue());
        features.add(feature);
      }
    }
    Map<Feature, Double> predFeatVecMap = Maps.newHashMap();
    for (Feature feature : predFeatVec) {
      if (Math.abs(feature.getFeatureValue()) > 0.0) {
        if (!predFeatVecMap.containsKey(feature)) {
          predFeatVecMap.put(feature, 0.0);
        }
        predFeatVecMap.put(feature, predFeatVecMap.get(feature)
            + wrongParsesWeight * feature.getFeatureValue());
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

  public synchronized void printFeatureWeights(Collection<Feature> featVec,
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

  /**
   * Saves the serialized object in a file. Additionally, a readable model is
   * also saved into fileName.readable.txt.
   * 
   * @param fileName
   * @throws IOException
   */
  public void saveModel(String fileName) throws IOException {
    FileOutputStream fileOut = new FileOutputStream(fileName);
    ObjectOutputStream out = new ObjectOutputStream(fileOut);
    out.writeObject(this);
    out.close();
    fileOut.close();

    List<Entry<Feature, Double>> entries =
        new ArrayList<>(cumulativeWeightVector.entrySet());
    entries.sort(Comparator.comparing(e -> -1 * e.getValue()
        / updateFrequency.get(e.getKey())));
    BufferedWriter bw =
        new BufferedWriter(new FileWriter(fileName + ".readable.txt"));

    for (Entry<Feature, Double> entry : entries) {
      bw.write(String.format("%f\t%s\n",
          entry.getValue() / updateFrequency.get(entry.getKey()),
          entry.getKey()));
    }
    bw.close();
  }

  /**
   * Loads and returns the model from the given serialized file.
   * 
   * @param fileName
   * @return
   * @throws IOException
   */
  public static StructuredPercepton loadModel(String fileName)
      throws IOException {
    FileInputStream fileIn = new FileInputStream(fileName);
    ObjectInputStream in = new ObjectInputStream(fileIn);
    StructuredPercepton sp = null;
    try {
      sp = (StructuredPercepton) in.readObject();
    } catch (ClassNotFoundException e1) {
      e1.printStackTrace();
    }
    in.close();
    fileIn.close();
    return sp;
  }

  /**
   * Returns a clone using serialization.
   * 
   * @return
   */
  public StructuredPercepton serialClone() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(this);

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      return (StructuredPercepton) ois.readObject();
    } catch (ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }
    return null;
  }
}
