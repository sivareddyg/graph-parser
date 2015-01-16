package in.sivareddy.graphparser.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.tuple.Pair;

import in.sivareddy.graphparser.util.KnowledgeBase.EntityType;
import in.sivareddy.graphparser.util.KnowledgeBase.Relation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroundedLexicon {

  private Map<EntityType, List<EntityType>> utypeToGtypeMap = Maps.newHashMap();
  private Map<Relation, List<Relation>> urelToGrelMap = Maps.newHashMap();

  private Map<Pair<Relation, Relation>, Double> urelGrelFreq = Maps.newHashMap();
  double urelGrelFreqMax = 0.0;

  private Map<Pair<String, String>, Double> urelPartGrelPartFreq = Maps.newHashMap();
  double urelPartGrelPartFreqMax = 0.0;

  private Map<Pair<String, String>, Double> utypeGtypeFreq = Maps.newHashMap();
  double utypeGtypeFreqMax = 0.0;

  private Map<Relation, Double> tfIdfRelation = Maps.newHashMap();
  private Map<EntityType, Double> tfIdfType = Maps.newHashMap();

  Map<Relation, Double> urelFreq = Maps.newHashMap();
  double urelFreqMax = 0.0;
  Map<Relation, Double> grelFreq = Maps.newHashMap();
  double grelFreqMax = 0.0;
  Map<String, Double> urelPartFreq = Maps.newHashMap();
  double urelPartFreqMax = 0.0;
  Map<String, Double> grelPartFreq = Maps.newHashMap();
  double grelPartFreqMax = 0.0;
  Map<String, Double> utypeFreq = Maps.newHashMap();
  double utypeFreqMax = 0.0;
  Map<String, Double> gtypeFreq = Maps.newHashMap();
  double gtypeFreqMax = 0.0;

  private Double relCount = 0.0;
  private Double typeCount = 0.0;

  public GroundedLexicon(String lexiconFileName) throws IOException {
    if (lexiconFileName == null)
      return;
    BufferedReader br = new BufferedReader(new FileReader(lexiconFileName));
    try {
      String line = br.readLine();
      boolean isRelation = false;
      EntityType sourceType = null;
      EntityType targetType = null;
      Relation sourceRelation = null;
      Relation targetRelation = null;
      Double sourceFreq = 1.0;
      Double targetFreq = 0.0;

      while (line != null) {
        if (line.equals("") || line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }

        if (line.charAt(0) != '\t') {
          // line represents either a source type or relation
          line = line.trim();
          String[] parts = line.split("\t");
          sourceFreq = Double.valueOf(parts[1]);
          if (parts[0].contains(" ")) {
            // line represents a source relation
            isRelation = true;
            String[] relationParts = parts[0].split(" ");
            sourceRelation = new Relation(relationParts[0], relationParts[1], sourceFreq);
            urelFreq.put(sourceRelation, sourceFreq);
            if (sourceFreq > urelFreqMax) {
              urelFreqMax = sourceFreq;
            }
            relCount += sourceFreq;

            String uleftEdge = sourceRelation.getLeft();
            if (!urelPartFreq.containsKey(uleftEdge)) {
              urelPartFreq.put(uleftEdge, 0.0);
            }
            Double value = urelPartFreq.get(uleftEdge);
            value += sourceFreq;
            urelPartFreq.put(uleftEdge, value);
            if (value > urelPartFreqMax) {
              urelPartFreqMax = value;
            }

            String urightEdge = sourceRelation.getRight();
            if (!urelPartFreq.containsKey(urightEdge)) {
              urelPartFreq.put(urightEdge, 0.0);
            }
            value = urelPartFreq.get(urightEdge);
            value += sourceFreq;
            urelPartFreq.put(urightEdge, value);
            if (value > urelPartFreqMax) {
              urelPartFreqMax = value;
            }

          } else {
            // line represents a source type
            isRelation = false;
            sourceType = new EntityType(parts[0], sourceFreq);
            utypeFreq.put(parts[0], sourceFreq);
            if (sourceFreq > utypeFreqMax) {
              utypeFreqMax = sourceFreq;
            }
            typeCount += sourceFreq;
          }
        } else {
          line = line.trim();
          String[] parts = line.split("\t");
          targetFreq = Double.valueOf(parts[1]);
          if (isRelation) {
            // line represents a target relation
            String[] relationParts = parts[0].split(" ");
            targetRelation = new Relation(relationParts[0], relationParts[1], targetFreq);
            if (!urelToGrelMap.containsKey(sourceRelation)) {
              urelToGrelMap.put(sourceRelation, new ArrayList<Relation>());
            }
            urelToGrelMap.get(sourceRelation).add(targetRelation);
            urelGrelFreq.put(Pair.of(sourceRelation, targetRelation), targetFreq);
            if (targetFreq > urelGrelFreqMax) {
              urelGrelFreqMax = targetFreq;
            }

            if (grelFreq.containsKey(targetRelation)) {
              Double value = grelFreq.get(targetRelation);
              value += targetFreq;
              grelFreq.put(targetRelation, value);
              if (value > grelFreqMax) {
                grelFreqMax = value;
              }
            } else if (grelFreq.containsKey(targetRelation.inverse())) {
              Relation inverse = targetRelation.inverse();
              Double value = grelFreq.get(inverse);
              value += targetFreq;
              grelFreq.put(inverse, value);
              if (value > grelFreqMax) {
                grelFreqMax = value;
              }
            } else {
              grelFreq.put(targetRelation, targetFreq);
              if (targetFreq > grelFreqMax) {
                grelFreqMax = targetFreq;
              }
            }

            String uleftEdge = sourceRelation.getLeft();
            String urightEdge = sourceRelation.getRight();

            Double value;
            String gleftEdge = targetRelation.getLeft();
            if (!grelPartFreq.containsKey(gleftEdge)) {
              grelPartFreq.put(gleftEdge, 0.0);
            }
            value = grelPartFreq.get(gleftEdge);
            value += targetFreq;
            grelPartFreq.put(gleftEdge, value);
            if (value > grelPartFreqMax) {
              grelPartFreqMax = value;
            }

            String grightEdge = targetRelation.getRight();
            if (!grelPartFreq.containsKey(grightEdge)) {
              grelPartFreq.put(grightEdge, 0.0);
            }
            value = grelPartFreq.get(grightEdge);
            value += targetFreq;
            grelPartFreq.put(grightEdge, value);
            if (value > grelPartFreqMax) {
              grelPartFreqMax = value;
            }

            Pair<String, String> urelGrelLeft = Pair.of(uleftEdge, gleftEdge);
            if (!urelPartGrelPartFreq.containsKey(urelGrelLeft)) {
              urelPartGrelPartFreq.put(urelGrelLeft, 0.0);
            }
            value = urelPartGrelPartFreq.get(urelGrelLeft);
            value += targetFreq;
            urelPartGrelPartFreq.put(urelGrelLeft, value);
            if (value > urelPartGrelPartFreqMax) {
              urelPartGrelPartFreqMax = value;
            }

            Pair<String, String> urelGrelRight = Pair.of(urightEdge, grightEdge);
            if (!urelPartGrelPartFreq.containsKey(urelGrelRight)) {
              urelPartGrelPartFreq.put(urelGrelRight, 0.0);
            }
            value = urelPartGrelPartFreq.get(urelGrelRight);
            value += targetFreq;
            urelPartGrelPartFreq.put(urelGrelRight, value);
            if (value > urelPartGrelPartFreqMax) {
              urelPartGrelPartFreqMax = value;
            }

          } else {
            // line represents a target type
            targetType = new EntityType(parts[0], targetFreq);
            if (!utypeToGtypeMap.containsKey(sourceType)) {
              utypeToGtypeMap.put(sourceType, new ArrayList<EntityType>());
            }
            utypeToGtypeMap.get(sourceType).add(targetType);
            utypeGtypeFreq.put(Pair.of(sourceType.getType(), targetType.getType()), targetFreq);
            if (targetFreq > utypeGtypeFreqMax) {
              utypeGtypeFreqMax = targetFreq;
            }

            String gType = targetType.getType();
            if (!gtypeFreq.containsKey(gType)) {
              gtypeFreq.put(gType, 0.0);
            }
            Double value = gtypeFreq.get(gType);
            value += targetFreq;
            gtypeFreq.put(gType, value);
            if (value > gtypeFreqMax) {
              gtypeFreqMax = value;
            }
          }
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }

    // tf-idf weighting of ungrounded relations
    Set<Relation> urels = urelToGrelMap.keySet();
    for (Relation sourceRelation : urels) {
      Double tf = sourceRelation.getWeight();
      Double idf =
          Math.log((urels.size() + 0.0) / (urelToGrelMap.get(sourceRelation).size() + 0.0));
      Double weight = tf * idf;
      sourceRelation.setWeight(weight);
      tfIdfRelation.put(sourceRelation, weight);
      // p(grel / urel) is stored in target relation
      List<Relation> grels = urelToGrelMap.get(sourceRelation);
      for (Relation grel : grels) {
        Double count = grel.getWeight();
        Double pGrelGivenUrel = count / tf;
        // Double prob = count / relCount;
        // Double maxprob = count / urelGrelFreqMax;
        grel.setWeight(pGrelGivenUrel);
      }
    }

    // tf-idf weighting of ungrounded types
    Set<EntityType> utypes = utypeToGtypeMap.keySet();
    for (EntityType sourceType : utypes) {
      Double tf = sourceType.getWeight();
      Double idf = Math.log((utypes.size() + 0.0) / (utypeToGtypeMap.get(sourceType).size() + 0.0));
      Double weight = tf * idf;
      sourceType.setWeight(weight);
      tfIdfType.put(sourceType, weight);
      List<EntityType> gtypes = utypeToGtypeMap.get(sourceType);

      boolean hasInt = false;
      boolean hasFloat = false;
      double score = 0.0;
      double freq = 0.0;
      for (EntityType gtype : gtypes) {
        Double count = gtype.getWeight();
        Double pGtypeGivenUtype = count / tf;
        // Double prob = count / typeCount;
        // gtype.setWeight(prob);
        // Double probmax = count / utypeGtypeFreqMax;
        gtype.setWeight(pGtypeGivenUtype);
        if (gtype.getType().equals("type.int")) {
          hasInt = true;
          score = gtype.getWeight();
          freq = count;
        } else if (gtype.getType().equals("type.float")) {
          hasFloat = true;
          score = gtype.getWeight();
          freq = count;
        }

      }

      // adding int type if float is present, and float type if int is
      // present. adding a score of half of the type that is present.
      if (hasInt && !hasFloat) {
        EntityType floatType = new EntityType("type.float", score / 2.0);
        gtypes.add(floatType);
        Collections.sort(gtypes);
        utypeGtypeFreq.put(Pair.of(sourceType.getType(), floatType.getType()), freq / 2.0);
      }
      if (hasFloat && !hasInt) {
        EntityType intType = new EntityType("type.int", score / 2.0);
        gtypes.add(intType);
        Collections.sort(gtypes);
        utypeGtypeFreq.put(Pair.of(sourceType.getType(), intType.getType()), freq / 2.0);
      }
    }
  }

  public double getUngroundedRelationFreq(Relation relation) {
    if (urelFreq.containsKey(relation)) {
      return urelFreq.get(relation);
    }
    relation = relation.inverse();
    if (urelFreq.containsKey(relation)) {
      return urelFreq.get(relation);
    }
    return 0.0;
  }

  public double getGroundedRelationFreq(Relation relation) {
    if (grelFreq.containsKey(relation)) {
      return grelFreq.get(relation);
    }
    relation = relation.inverse();
    if (grelFreq.containsKey(relation)) {
      return grelFreq.get(relation);
    }
    return 0.0;
  }

  public double getUngroundedTypeFreq(String type) {
    if (utypeFreq.containsKey(type)) {
      return utypeFreq.get(type);
    }
    return 0.0;
  }

  public double getGroundedTypeFreq(String type) {
    if (gtypeFreq.containsKey(type)) {
      return gtypeFreq.get(type);
    }
    return 0.0;
  }

  public double getRelationTotalFreq() {
    return relCount;
  }

  public double getTypeTotalFreq() {
    return typeCount;
  }

  public double getUngroundedRelationScore(Relation relation) {
    if (tfIdfRelation.containsKey(relation)) {
      return tfIdfRelation.get(relation);
    }
    relation = relation.inverse();
    if (tfIdfRelation.containsKey(relation)) {
      return tfIdfRelation.get(relation);
    }
    return 0.0;
  }

  public double getUngroundedTypeScore(EntityType entityType) {
    if (tfIdfType.containsKey(entityType)) {
      return tfIdfType.get(entityType);
    }
    return 0.0;
  }

  public double getUrelGrelProb(Relation urel, Relation grel) {
    Pair<Relation, Relation> pair = Pair.of(urel, grel);
    if (urelGrelFreq.containsKey(pair)) {
      Double countUrelGrel = urelGrelFreq.get(pair);
      Double urelCount = urelFreq.get(urel);
      return countUrelGrel / urelCount;
      // return countUrelGrel / relCount;
      // return countUrelGrel / urelGrelFreqMax;
    }
    urel = urel.inverse();
    grel = grel.inverse();
    pair = Pair.of(urel, grel);
    if (urelGrelFreq.containsKey(pair)) {
      Double countUrelGrel = urelGrelFreq.get(pair);
      Double urelCount = urelFreq.get(urel);
      return countUrelGrel / urelCount;
      // return countUrelGrel / relCount;
      // return countUrelGrel / urelGrelFreqMax;
    }
    return 0.0;
  }

  public double getGrelGrelUpperBoundProb(Relation grel1, Relation grel2) {
    double freq1 = getGroundedRelationFreq(grel1);
    double freq2 = getGroundedRelationFreq(grel2);
    return (freq1 + freq2) / (2.0 * grelFreqMax);
  }

  public double getGtypeGrelUpperBoundProb(String gtype, Relation grel) {
    double freq1 = getGroundedTypeFreq(gtype);
    double freq2 = getGroundedRelationFreq(grel);
    return (freq1 + freq2) / (gtypeFreqMax + grelFreqMax);
  }

  /**
   * Returns 1 if urel, grel pair is found in the lexicon. Returns -1 if inverse of urel, inverse of
   * grel pair is found. Returns 0 if none.
   * 
   * @param urel
   * @param grel
   * @return
   * 
   * 
   */
  public int hasUrelGrel(Relation urel, Relation grel) {
    Pair<Relation, Relation> pair = Pair.of(urel, grel);
    if (urelGrelFreq.containsKey(pair)) {
      return 1;
    }
    urel = urel.inverse();
    grel = grel.inverse();
    pair = Pair.of(urel, grel);
    if (urelGrelFreq.containsKey(pair)) {
      return -1;
    }
    return 0;
  }

  public double getUrelPartGrelPartProb(String urelPart, String grelPart) {
    Pair<String, String> pair = Pair.of(urelPart, grelPart);
    if (urelPartGrelPartFreq.containsKey(pair)) {
      Double count = urelPartGrelPartFreq.get(pair);
      Double urelPartCount = urelPartFreq.get(urelPart);
      return count / urelPartCount;
      // each relation has two parts, and so the frequency of parts is two
      // times the relation count
      // return count / (2.0 * relCount);
      // return count / urelPartGrelPartFreqMax;
    }
    return 0.0;
  }

  public double getUtypeGtypeProb(String utype, String gtype) {
    Pair<String, String> pair = Pair.of(utype, gtype);
    if (utypeGtypeFreq.containsKey(pair)) {
      Double count = utypeGtypeFreq.get(pair);
      Double utypeCount = utypeFreq.get(utype);
      return count / utypeCount;
      // return count / typeCount;
      // return count / utypeGtypeFreqMax;
    }
    return 0.0;
  }

  public List<Relation> getGroundedRelations(Relation ungroundedRelation) {
    if (urelToGrelMap.containsKey(ungroundedRelation)) {
      return urelToGrelMap.get(ungroundedRelation);
    }
    // if the inverse relation exists
    ungroundedRelation = ungroundedRelation.inverse();
    if (urelToGrelMap.containsKey(ungroundedRelation)) {
      List<Relation> relations = Lists.newArrayList();
      for (Relation relation : urelToGrelMap.get(ungroundedRelation)) {
        relations.add(relation.inverse());
      }
      return relations;
    }
    return null;
  }

  public List<EntityType> getGroundedTypes(EntityType ungroundedType) {
    if (utypeToGtypeMap.containsKey(ungroundedType)) {
      return utypeToGtypeMap.get(ungroundedType);
    }
    return null;
  }

}
