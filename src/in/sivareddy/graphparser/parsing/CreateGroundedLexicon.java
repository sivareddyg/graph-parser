package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.CcgParseTree;
import in.sivareddy.graphparser.ccg.CcgParser;
import in.sivareddy.graphparser.ccg.FunnyCombinatorException;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.ccg.SemanticCategoryType;
import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.Relation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateGroundedLexicon {
  private ConcurrentMap<Relation, ConcurrentMap<Relation, Double>> predicateToGroundedRelationMap;
  private ConcurrentMap<Relation, Double> predicateCounts;
  private ConcurrentMap<String, ConcurrentMap<String, Double>> langTypeToGroundedTypeMap;
  private ConcurrentMap<String, Double> typeCounts;

  public static Map<String, String> cardinalTypes = ImmutableMap
      .<String, String>builder().put("I-DAT", "type.datetime")
      .put("DATE", "type.datetime").put("PERCENT", "type.float")
      .put("TIME", "type.datetime").put("MONEY", "type.float")
      .put("CD.int", "type.int").put("CD.float", "type.float").build();

  Pattern floatPattern = Pattern.compile(".*[\\.][0-9].*");

  private KnowledgeBase kb;
  private CcgParser ccgParser;

  public CreateGroundedLexicon(KnowledgeBase kb, CcgAutoLexicon ccgAutoLexicon,
      String[] lexicalFields, String[] argIdentifierFields,
      String[] relationTypingFeilds, boolean ignorePronouns) {
    ccgParser =
        new CcgParser(ccgAutoLexicon, lexicalFields, argIdentifierFields,
            relationTypingFeilds, ignorePronouns);
    predicateToGroundedRelationMap = new ConcurrentHashMap<>();
    langTypeToGroundedTypeMap = new ConcurrentHashMap<>();
    predicateCounts = new ConcurrentHashMap<>();
    typeCounts = new ConcurrentHashMap<>();
    this.kb = kb;
  }

  public static class CreateGroundedLexiconRunnable implements Runnable {
    private List<String> jsonSentences;
    JsonParser parser = new JsonParser();
    Gson gson = new Gson();
    private CreateGroundedLexicon creator;
    boolean printSentences;
    String semanticParseKey;

    public CreateGroundedLexiconRunnable(List<String> jsonSentences,
        CreateGroundedLexicon creator, String semanticParseKey,
        boolean printSentences) {
      Preconditions.checkArgument(jsonSentences != null);
      this.jsonSentences = jsonSentences;
      this.creator = creator;
      this.printSentences = printSentences;
      this.semanticParseKey = semanticParseKey;
    }

    @Override
    public void run() {
      List<JsonObject> usefulSentences = Lists.newArrayList();

      for (String line : jsonSentences) {
        if (line.equals("") || line.charAt(0) == '#') {
          continue;
        }
        JsonObject jsonSentence = parser.parse(line).getAsJsonObject();
        List<Set<String>> semanticParses;
        if (semanticParseKey == "synPars") {
          semanticParses =
              creator.lexicaliseArgumentsToDomainEntities(jsonSentence, 1);
        } else {
          semanticParses = new ArrayList<>();
          semanticParses = new ArrayList<>();
          if (!jsonSentence.has(semanticParseKey))
            continue;
          JsonArray semPars =
              jsonSentence.get(semanticParseKey).getAsJsonArray();
          Set<String> semanticParse = new HashSet<>();
          for (JsonElement semPar : semPars) {
            JsonArray predicates = semPar.getAsJsonArray();
            for (JsonElement predicate : predicates) {
              semanticParse.add(predicate.getAsString());
            }
            semanticParses.add(semanticParse);
          }
        }

        if (semanticParses == null || semanticParses.size() == 0) {
          continue;
        }

        // If the sentence has at least one useful parse
        boolean isUseful = false;
        for (Set<String> semanticParse : semanticParses) {
          boolean isUsefulParse =
              creator.updateLexicon(semanticParse, jsonSentence,
                  1.0 / semanticParses.size());
          if (isUsefulParse) {
            isUseful = true;
          }
        }
        if (isUseful && printSentences) {
          usefulSentences.add(jsonSentence);
        }
      }

      if (!printSentences) {
        return;
      }

      safePrintln(usefulSentences);
    }

    private synchronized void safePrintln(List<JsonObject> sentences) {
      for (JsonObject jsonSentence : sentences) {
        System.out.println(gson.toJson(jsonSentence));
      }
    }
  }

  /**
   * lexicalise arguments to domain entities
   *
   * @param jsonSentence - Sentence containing candc syntactic parse and
   *        information about entities
   * @param nparses - no of semantic parses to consider
   *
   * @return
   */
  public List<Set<String>> lexicaliseArgumentsToDomainEntities(
      JsonObject jsonSentence, int nparses) {
    List<Set<String>> allParses = Lists.newArrayList();

    // JsonParser parser = new JsonParser();
    // JsonElement jelement = parser.parse(jsonSentence);

    JsonArray entities = jsonSentence.getAsJsonArray("entities");

    JsonArray words = jsonSentence.getAsJsonArray("words");
    JsonArray syntacticParses = jsonSentence.getAsJsonArray("synPars");
    List<String> wordStrings = Lists.newArrayList();
    List<JsonObject> wordObjects = Lists.newArrayList();

    for (JsonElement word : words) {
      JsonObject wordObject = word.getAsJsonObject();
      wordObjects.add(wordObject);
      String wordString = wordObject.get("word").getAsString();
      wordStrings.add(wordString);
    }
    String sent = Joiner.on(" ").join(wordStrings);
    jsonSentence.addProperty("sentence", sent);
    // System.err.println("Sent -> " + sent);

    int parseCount = 0;
    for (JsonElement syntacticParse : syntacticParses) {
      parseCount += 1;
      if (parseCount > nparses) {
        break;
      }
      JsonObject synParseMap = syntacticParse.getAsJsonObject();
      String synParse = synParseMap.get("synPar").getAsString();
      try {
        List<CcgParseTree> trees = ccgParser.parseFromString(synParse);
        for (CcgParseTree tree : trees) {
          List<LexicalItem> leaves = tree.getLeafNodes();
          for (int i = 0; i < leaves.size(); i++) {
            String stanfordNer = wordObjects.get(i).get("ner").getAsString();
            LexicalItem leaf = leaves.get(i);
            String candcNer = leaf.getNeType();
            String posTag = leaf.getPos();
            if (posTag.equals("CD")) {
              String word = leaf.getWord();
              if (floatPattern.matcher(word).matches()) {
                posTag = "CD.float";
              } else {
                posTag = "CD.int";
              }
            }
            String mid =
                cardinalTypes.containsKey(candcNer) ? cardinalTypes
                    .get(candcNer)
                    : (cardinalTypes.containsKey(stanfordNer) ? cardinalTypes
                        .get(stanfordNer)
                        : (cardinalTypes.containsKey(posTag) ? cardinalTypes
                            .get(posTag) : leaf.getMid()));
            leaf.setMid(mid);
          }

          // mids from freebase annotation or geoquery entity
          // recognition
          for (JsonElement entityElement : entities) {
            JsonObject entityObject = entityElement.getAsJsonObject();
            int index = entityObject.get("index").getAsInt();
            String mid = entityObject.get("entity").getAsString();
            leaves.get(index).setMID(mid);
          }

          // do not handle the numbers specially for lexicon
          // generation -
          // i.e. do not produce the predicate COUNT
          Set<Set<String>> predicates =
              tree.getLexicalisedSemanticPredicates(false);
          // System.err.println(predicates);
          allParses.add(Lists.newArrayList(predicates).get(0));
        }
      } catch (FunnyCombinatorException e) {
        // skip this parse
      } catch (BadParseException e) {
        // skip this parse
      } catch (Exception e) {
        // generally when the sentence has special characters like
        // brackets which confuse brackets from semantic parse
        // skip this parse
      }
    }
    return allParses;
  }

  /**
   *
   * @return
   */
  public boolean updateLexicon(Set<String> predicates, JsonObject jsonSentence,
      Double normalisingConstant) {
    // boolean isUseful = false;
    Pattern relationPattern = Pattern.compile("(.*)\\(([0-9]+\\:e) , (.*)\\)");
    Pattern typePattern = Pattern.compile("(.*)\\([0-9]+\\:s , (.*)\\)");
    Pattern specialPattern = Pattern.compile("(.*)\\([0-9]+\\:[se][\\)\\ ]");
    Pattern varPattern = Pattern.compile("[^\\:]+\\:x");
    Pattern eventPattern = Pattern.compile("[^\\:]+\\:e");
    Set<String> negationTypes = Sets.newHashSet("NEGATION", "COMPLEMENT");
    Set<String> questionTypes = Sets.newHashSet("QUESTION");

    /*-
     * South_African_Airways flies directly from New_York to Johannesburg .
     * [fly.fly.1.I-ORG(e1 , m.0q0b4), fly.from.2.I-LOC(e1 , m.02_286), fly.to.2.I-LOC(e1 , m.0g284)], fly.directly.1(e1), 
     */

    Map<String, Set<String>> varsToEvents = Maps.newHashMap();
    Map<String, Set<String>> entityArgsToEvents = Maps.newHashMap();
    Set<String> nonDomainEntities = Sets.newHashSet();

    // 2:e=[(on.1,m.0q9h2), (in.2,m.02_286), (in.1,m.0q9h2)]
    Map<String, Set<Pair<String, String>>> events = Maps.newHashMap();
    Map<String, Set<String>> types = Maps.newHashMap();

    int negationCount = 0;

    for (String predicate : predicates) {
      boolean isMatchedAlready = false;
      Matcher matcher = relationPattern.matcher(predicate);
      if (matcher.find()) {
        isMatchedAlready = true;
        String relationName = matcher.group(1);
        String eventName = matcher.group(2);
        String argumentName = matcher.group(3);
        String entityName = argumentName.split(":")[1];
        // relationName is not lexicalised and is of special
        // type
        if (SemanticCategoryType.types.contains(relationName)) {
          negationCount++;
          continue;
        }

        if (varPattern.matcher(argumentName).matches()) {
          if (!varsToEvents.containsKey(argumentName)) {
            varsToEvents.put(argumentName, new HashSet<String>());
          }
          varsToEvents.get(argumentName).add(eventName);
          continue;
        } else if (eventPattern.matcher(argumentName).matches()) {
          continue;
        } else if (!kb.hasEntity(entityName)) {
          // if entity does not belong to the domain of interest
          nonDomainEntities.add(entityName);
          continue;
        } else {
          // argument is an entity
          if (!entityArgsToEvents.containsKey(argumentName)) {
            entityArgsToEvents.put(argumentName, new HashSet<String>());
          }
          entityArgsToEvents.get(argumentName).add(eventName);
        }

        if (!events.containsKey(eventName)) {
          events.put(eventName, new HashSet<Pair<String, String>>());
        }
        // note: lowercasing relation names
        Pair<String, String> edge =
            Pair.of(relationName.toLowerCase(), entityName);
        events.get(eventName).add(edge);
      }

      if (isMatchedAlready) {
        continue;
      }

      matcher = typePattern.matcher(predicate);
      if (matcher.find()) {
        isMatchedAlready = true;
        String relationName = matcher.group(1);
        String argumentName = matcher.group(2);
        String entityName = argumentName.split(":")[1];
        if (SemanticCategoryType.types.contains(relationName)) {
          if (negationTypes.contains(relationName)) {
            negationCount++;
          }
          continue;
        }

        if (varPattern.matcher(argumentName).matches()) {
          if (!varsToEvents.containsKey(argumentName)) {
            varsToEvents.put(argumentName, new HashSet<String>());
          }
          continue;
        } else if (eventPattern.matcher(argumentName).matches()) {
          continue;
        } else if (!kb.hasEntity(entityName)) {
          nonDomainEntities.add(entityName);
          continue;
        }

        if (!types.containsKey(entityName)) {
          types.put(entityName, new HashSet<String>());
        }
        // note: lowercasing type names
        types.get(entityName).add(relationName.toLowerCase());
      }

      if (isMatchedAlready) {
        continue;
      }

      matcher = specialPattern.matcher(predicate);
      if (matcher.find()) {
        String relationName = matcher.group(1);
        if (negationTypes.contains(relationName)) {
          negationCount++;
        }
        if (questionTypes.contains(relationName)) {
          return false;
        }
      }
    }

    int validRelCount = 0;
    Set<String> importantEvents = Sets.newHashSet();
    // Check the knowledge base to create grounded lexicon
    for (String event : events.keySet()) {
      List<Pair<String, String>> relationEdges =
          Lists.newArrayList(events.get(event));
      for (int i = 0; i < relationEdges.size(); i++) {
        for (int j = i + 1; j < relationEdges.size(); j++) {
          Pair<String, String> relationEdge1 = relationEdges.get(i);
          Pair<String, String> relationEdge2 = relationEdges.get(j);

          if (relationEdge1.compareTo(relationEdge2) < 0) {
            relationEdge1 = relationEdges.get(j);
            relationEdge2 = relationEdges.get(i);
          }

          String relation1 = relationEdge1.getLeft();
          String entity1 = relationEdge1.getRight();

          String relation2 = relationEdge2.getLeft();
          String entity2 = relationEdge2.getRight();

          if (relation1.equals(relation2)) {
            continue;
          }

          Relation languagePredicate = Relation.of(relation1, relation2);
          Set<Relation> groundedRelations = kb.getRelations(entity1, entity2);

          if (groundedRelations == null || groundedRelations.size() == 0) {
            continue;
          }

          importantEvents.add(event);

          ConcurrentHashMap<Relation, Double> groundedRelationsScore;
          predicateToGroundedRelationMap.putIfAbsent(languagePredicate,
              new ConcurrentHashMap<Relation, Double>());
          groundedRelationsScore =
              (ConcurrentHashMap<Relation, Double>) predicateToGroundedRelationMap
                  .get(languagePredicate);

          Double increment =
              1.0 / groundedRelations.size() * normalisingConstant;
          for (Relation groundedRelation : groundedRelations) {
            groundedRelationsScore.putIfAbsent(groundedRelation, 0.0);
            Double count = groundedRelationsScore.get(groundedRelation);
            count += increment;
            groundedRelationsScore.put(groundedRelation, count);
            validRelCount += 1;
          }
          predicateCounts.putIfAbsent(languagePredicate, 0.0);
          Double predicateCount = predicateCounts.get(languagePredicate);
          predicateCount += 1.0 * normalisingConstant;
          predicateCounts.put(languagePredicate, predicateCount);
        }
      }
    }

    if (validRelCount == 0) {
      return false;
    }
    // else
    // isUseful = true;

    for (String entity : types.keySet()) {
      Set<String> languageTypes = types.get(entity);
      Set<String> groundedTypes = kb.getTypes(entity);
      if (groundedTypes == null || groundedTypes.size() == 0) {
        continue;
      }
      for (String languageType : languageTypes) {
        ConcurrentMap<String, Double> groundedTypesScore;

        langTypeToGroundedTypeMap.putIfAbsent(languageType,
            new ConcurrentHashMap<String, Double>());
        groundedTypesScore = langTypeToGroundedTypeMap.get(languageType);

        Double increment = 1.0 / groundedTypes.size() * normalisingConstant;
        for (String groundedType : groundedTypes) {
          groundedTypesScore.putIfAbsent(groundedType, 0.0);
          Double count = groundedTypesScore.get(groundedType);
          count += increment;
          groundedTypesScore.put(groundedType, count);
        }
        typeCounts.putIfAbsent(languageType, 0.0);
        Double typeCount = typeCounts.get(languageType);
        typeCount += 1.0 * normalisingConstant;
        typeCounts.put(languageType, typeCount);
      }
    }

    int boundedVarCount = 0;
    int freeVarCount = 0;

    for (String var : varsToEvents.keySet()) {
      Set<String> curVarEvents = varsToEvents.get(var);
      int size = curVarEvents.size();
      if (size < 2) {
        freeVarCount += 1;
        continue;
      }

      curVarEvents.retainAll(importantEvents);
      size = curVarEvents.size();

      // if the variable appears in two important events, then
      // variable is bounded
      if (size > 1) {
        boundedVarCount += 1;
      } else {
        freeVarCount += 1;
      }
    }

    int freeEntityCount = 0;
    for (String var : entityArgsToEvents.keySet()) {
      Set<String> curEvents = entityArgsToEvents.get(var);
      int size = curEvents.size();

      curEvents.retainAll(importantEvents);
      size = curEvents.size();

      // if the entity does not appear in any important event
      if (size == 0) {
        freeEntityCount += 1;
      }
    }

    int foreignEntityCount = nonDomainEntities.size();

    // Storing the values from the most useful parse
    if (!jsonSentence.has("boundedVarCount")
        || jsonSentence.get("boundedVarCount").getAsInt() > boundedVarCount) {
      jsonSentence.addProperty("boundedVarCount", boundedVarCount);
    }

    if (!jsonSentence.has("freeVarCount")
        || jsonSentence.get("freeVarCount").getAsInt() > freeVarCount) {
      jsonSentence.addProperty("freeVarCount", freeVarCount);
    }

    if (!jsonSentence.has("freeEntityCount")
        || jsonSentence.get("freeEntityCount").getAsInt() > freeEntityCount) {
      jsonSentence.addProperty("freeEntityCount", freeEntityCount);
    }

    if (!jsonSentence.has("foreignEntityCount")
        || jsonSentence.get("foreignEntityCount").getAsInt() > foreignEntityCount) {
      jsonSentence.addProperty("foreignEntityCount", foreignEntityCount);
    }

    if (!jsonSentence.has("negationCount")
        || jsonSentence.get("negationCount").getAsInt() > negationCount) {
      jsonSentence.addProperty("negationCount", negationCount);
    }

    // System.err.println(jsonSentence.get("sentence").getAsString());

    // System.out.println();
    // return isUseful;
    return true;
  }

  /**
   * Compare pairs having doubles as values
   *
   * @param <T>
   */
  private class EntryComparator<T> implements Comparator<Entry<T, Double>> {
    @Override
    public int compare(Entry<T, Double> o1, Entry<T, Double> o2) {
      return o1.getValue().compareTo(o2.getValue());
    }
  }

  public void printLexicon(BufferedWriter bw) throws IOException {
    // Types
    ArrayList<Entry<String, Double>> langTypes =
        Lists.newArrayList(typeCounts.entrySet());
    Comparator<Entry<String, Double>> comparator2 =
        Collections.reverseOrder(new EntryComparator<String>());
    Collections.sort(langTypes, comparator2);

    bw.write("# Language Types to Grounded Types\n");
    for (Entry<String, Double> langTypeEntry : langTypes) {
      String langType = langTypeEntry.getKey();
      Double langTypeFreq = langTypeEntry.getValue();
      bw.write(String.format("%s\t%f\n", langType, langTypeFreq));

      ArrayList<Entry<String, Double>> groundedTypes =
          Lists
              .newArrayList(langTypeToGroundedTypeMap.get(langType).entrySet());
      Collections.sort(groundedTypes, comparator2);

      for (Entry<String, Double> groundedTypeEntry : groundedTypes) {
        String groundedType = groundedTypeEntry.getKey();
        Double freq = groundedTypeEntry.getValue();
        bw.write(String.format("\t%s\t%f\n", groundedType, freq));
      }
    }

    // Predicates
    ArrayList<Entry<Relation, Double>> predicates =
        Lists.newArrayList(predicateCounts.entrySet());
    Comparator<Entry<Relation, Double>> comparator1 =
        Collections.reverseOrder(new EntryComparator<Relation>());
    Collections.sort(predicates, comparator1);

    bw.write("# Language Predicates to Grounded Relations\n");
    for (Entry<Relation, Double> predicateEntry : predicates) {

      Relation predicate = predicateEntry.getKey();
      Double predicateFreq = predicateEntry.getValue();
      bw.write(String.format("%s %s\t%f\n", predicate.getLeft(),
          predicate.getRight(), predicateFreq));

      ArrayList<Entry<Relation, Double>> groundedRealtions =
          Lists.newArrayList(predicateToGroundedRelationMap.get(predicate)
              .entrySet());
      Collections.sort(groundedRealtions, comparator1);

      for (Entry<Relation, Double> groundedRealtionEntry : groundedRealtions) {
        Relation groundedRelation = groundedRealtionEntry.getKey();
        Double freq = groundedRealtionEntry.getValue();
        bw.write(String.format("\t%s %s\t%f\n", groundedRelation.getLeft(),
            groundedRelation.getRight(), freq));
      }
    }
  }
}
