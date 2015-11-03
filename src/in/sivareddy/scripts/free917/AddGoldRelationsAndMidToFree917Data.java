package in.sivareddy.scripts.free917;

import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AddGoldRelationsAndMidToFree917Data extends ProcessStreamInterface {
  private final Schema schema;

  public AddGoldRelationsAndMidToFree917Data(Schema schema) {
    this.schema = schema;
  }

  @Override
  public void processSentence(JsonObject sentence) {
    String query = sentence.get(SentenceKeys.SPARQL_QUERY).getAsString();
    Pattern datesPattern = Pattern.compile("xsd\\:dateTime\\(\\?([^\\)]+)\\)");
    Set<String> dates = new HashSet<>();
    Matcher datesMatcher = datesPattern.matcher(query);
    while (datesMatcher.find()) {
      dates.add(datesMatcher.group(1));
    }

    Pattern p = Pattern.compile("FILTER .*? \\.");
    query = p.matcher(query).replaceAll("");
    p = Pattern.compile("OPTIONAL \\{[^}]+\\}");
    query = p.matcher(query).replaceAll("");
    p = Pattern.compile("\\{([^}{]+)\\}");
    Matcher m = p.matcher(query);
    if (m.find()) {
      query = m.group(1);
    }

    for (String date : dates) {
      query =
          query.replaceAll(String.format("\\?%s", date), "rdf:type.datetime");
    }

    Pattern floatPattern = Pattern.compile(" [0-9][0-9\\.]+");
    query = floatPattern.matcher(query).replaceAll(" rdf:type.int");
    floatPattern = Pattern.compile("^[0-9][0-9\\.]+");
    query = floatPattern.matcher(query).replaceAll("rdf:type.int");

    query = query.replaceAll(" true", " type.boolean");
    query = query.replaceAll("^true", "type.boolean");

    List<String> predicates =
        Lists.newArrayList(Splitter.on(" . ").trimResults()
            .trimResults(CharMatcher.anyOf(" .")).omitEmptyStrings()
            .split(query));

    String relationWithEntity = null;
    String relationWithQuestion = null;
    String relationWithRdfType = null;

    for (String predicate : predicates) {
      if (!isDomainRelation(predicate))
        continue;

      if (predicate.matches("\\?.*\\?.*")) {
        relationWithQuestion = predicate;
      } else if (predicate.matches("\\?.*fb\\:.*fb\\:.*")
          || predicate.matches("fb\\:.*fb\\:.*\\?.*")) {
        relationWithEntity = predicate;
      } else {
        relationWithRdfType = predicate;
      }
    }

    if (relationWithEntity != null && isDirectRelation(relationWithEntity)) {
      processDirectRelation(sentence, relationWithEntity);
    } else if (relationWithEntity != null && relationWithQuestion != null) {
      processMediatorRelation(sentence, relationWithEntity,
          relationWithQuestion);
    } else if (relationWithRdfType != null
        && isDirectRelation(relationWithRdfType)) {
      processDirectRelation(sentence, relationWithRdfType);
    } else if (relationWithRdfType != null && relationWithQuestion != null) {
      processMediatorRelation(sentence, relationWithRdfType,
          relationWithQuestion);
    }
  }

  boolean isDomainRelation(String predicate) {
    List<String> parts =
        Splitter.on(CharMatcher.WHITESPACE).splitToList(predicate);
    String relation = parts.get(1).replace("fb:", "");
    return schema.getRelationArguments(relation) != null;
  }

  boolean isDirectRelation(String predicate) {
    List<String> parts =
        Splitter.on(CharMatcher.WHITESPACE).splitToList(predicate);
    String relation = parts.get(1).replace("fb:", "");
    return schema.getRelationArguments(relation) != null
        && !schema.hasMediatorArgument(relation);
  }

  void processMediatorRelation(JsonObject sentence, String predicateWithEntity,
      String predicateWithQuestion) {
    List<String> parts =
        Splitter.on(CharMatcher.WHITESPACE).splitToList(predicateWithEntity);
    String goldMid = null;
    String goldRelationLeft = null;
    String mediatorVar = null;
    if (parts.get(0).startsWith("?")) {
      mediatorVar = parts.get(0);
      goldMid = parts.get(2).replace("fb:", "");
      goldMid = goldMid.replace("rdf:", "");
      String goldRelation = parts.get(1).replace("fb:", "");
      goldRelationLeft = goldRelation;
    } else {
      mediatorVar = parts.get(2);
      goldMid = parts.get(0).replace("fb:", "");
      goldMid = goldMid.replace("rdf:", "");
      String goldRelation = parts.get(1).replace("fb:", "");
      String invRelation = schema.getRelation2Inverse(goldRelation);
      goldRelationLeft = invRelation;
    }

    String goldRelationRight = null;
    parts =
        Splitter.on(CharMatcher.WHITESPACE).splitToList(predicateWithQuestion);
    if (parts.get(0).equals(mediatorVar)) {
      String goldRelation = parts.get(1).replace("fb:", "");
      goldRelationRight = goldRelation;
    } else if (parts.get(2).equals(mediatorVar)) {
      String goldRelation = parts.get(1).replace("fb:", "");
      String invRelation = schema.getRelation2Inverse(goldRelation);
      goldRelationRight = invRelation;
    }

    if (goldMid != null && goldRelationLeft != null
        && goldRelationRight != null) {
      sentence.addProperty(SentenceKeys.GOLD_MID, goldMid);
      JsonObject relation = new JsonObject();
      relation.addProperty(SentenceKeys.RELATION_LEFT, goldRelationLeft);
      relation.addProperty(SentenceKeys.RELATION_RIGHT, goldRelationRight);
      relation.addProperty(SentenceKeys.SCORE, 1.0);
      JsonArray relations = new JsonArray();
      relations.add(relation);
      sentence.add(SentenceKeys.GOLD_RELATIONS, relations);
    }
  }

  void processDirectRelation(JsonObject sentence, String predicate) {
    List<String> parts =
        Splitter.on(CharMatcher.WHITESPACE).splitToList(predicate);
    String goldMid = null;
    String goldRelationLeft = null;
    String goldRelationRight = null;
    if (parts.get(0).startsWith("?")) {
      goldMid = parts.get(2).replace("fb:", "");
      goldMid = goldMid.replace("rdf:", "");
      String goldRelation = parts.get(1).replace("fb:", "");
      if (!schema.hasMediatorArgument(goldRelation)) {
        if (schema.getRelationIsMaster(goldRelation)) {
          goldRelationLeft = goldRelation + ".2";
          goldRelationRight = goldRelation + ".1";
        } else {
          String invRelation = schema.getRelation2Inverse(goldRelation);
          goldRelationLeft = invRelation + ".1";
          goldRelationRight = invRelation + ".2";
        }
      }
    } else {
      goldMid = parts.get(0).replace("fb:", "");
      goldMid = goldMid.replace("rdf:", "");
      String goldRelation = parts.get(1).replace("fb:", "");
      if (!schema.hasMediatorArgument(goldRelation)) {
        if (schema.getRelationIsMaster(goldRelation)) {
          goldRelationLeft = goldRelation + ".1";
          goldRelationRight = goldRelation + ".2";
        } else {
          String invRelation = schema.getRelation2Inverse(goldRelation);
          goldRelationLeft = invRelation + ".2";
          goldRelationRight = invRelation + ".1";
        }
      }
    }

    if (goldMid != null && goldRelationLeft != null
        && goldRelationRight != null) {
      sentence.addProperty(SentenceKeys.GOLD_MID, goldMid);
      JsonObject relation = new JsonObject();
      relation.addProperty(SentenceKeys.RELATION_LEFT, goldRelationLeft);
      relation.addProperty(SentenceKeys.RELATION_RIGHT, goldRelationRight);
      relation.addProperty(SentenceKeys.SCORE, 1.0);
      JsonArray relations = new JsonArray();
      relations.add(relation);
      sentence.add(SentenceKeys.GOLD_RELATIONS, relations);
    }
  }

  public static void main(String[] args) throws InterruptedException,
      IOException {
    Schema schema = new Schema(args[0]);

    AddGoldRelationsAndMidToFree917Data engine =
        new AddGoldRelationsAndMidToFree917Data(schema);

    engine.processStream(System.in, System.out, 1, true);
  }

}
