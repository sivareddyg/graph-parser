package in.sivareddy.ml.learning;

import java.util.ArrayList;
import java.util.List;

import in.sivareddy.graphparser.parsing.LexicalGraph.UrelGrelFeature;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.ml.basic.Feature;
import junit.framework.TestCase;

public class StrcuturedPerceptronSerializationTest extends TestCase {
  StructuredPercepton sp;

  @Override
  protected void setUp() throws Exception {
    sp = new StructuredPercepton();
  }

  /**
   * Tests if object serialization is working.
   * 
   */
  public void testSerilization() {
    Relation urel = new Relation("uleft", "uright", 1.5);
    Relation grel = new Relation("gleft", "gright", 2.0);
    List<Relation> key = new ArrayList<>();
    key.add(urel);
    key.add(grel);
    UrelGrelFeature feat = new UrelGrelFeature(key, 0.92);
    List<Feature> goldFeatures = new ArrayList<>();
    goldFeatures.add(feat);

    List<Feature> predictedFeatures = new ArrayList<>();

    sp.updateWeightVector(1, goldFeatures, 1, predictedFeatures);
    sp.updateWeightVector(1, goldFeatures, 1, predictedFeatures);
    sp.updateWeightVector(1, goldFeatures, 1, predictedFeatures);
    sp.updateWeightVector(1, goldFeatures, 1, predictedFeatures);

    urel = new Relation("uleft", "uright");
    grel = new Relation("gleft", "gright");
    key = new ArrayList<>();
    key.add(urel);
    key.add(grel);
    feat = new UrelGrelFeature(key, 0.92);
    goldFeatures = new ArrayList<>();
    goldFeatures.add(feat);

    StructuredPercepton clone = sp.serialClone();
    assertEquals(sp.getScoreTraining(new ArrayList<>(goldFeatures)),
        clone.getScoreTraining(new ArrayList<>(goldFeatures)));
    assertEquals(sp.getScoreTesting(new ArrayList<>(goldFeatures)),
        clone.getScoreTesting(new ArrayList<>(goldFeatures)));
  }
}
