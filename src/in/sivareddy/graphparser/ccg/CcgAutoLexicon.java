package in.sivareddy.graphparser.ccg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import in.sivareddy.graphparser.ccg.SemanticCategory.SemanticCategoryType;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CcgAutoLexicon {
  public static Set<String> typePosTags =
      Sets.newHashSet("NN", "NNP", "NNPS", "NNS", "PRP", "PRP$", "CD");
  public static Set<String> pronounPosTags = Sets.newHashSet("PRP", "PRP$");
  public static Set<String> typeModPosTags = Sets.newHashSet("JJ", "JJR", "JJS");

  public static Set<String> eventPosTags =
      Sets.newHashSet("VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "IN", "POS", "TO");
  private static Set<String> eventModPosTags = Sets.newHashSet("RB", "RBR", "RBS", "RP");

  private static Set<String> closedPosTags = Sets.newHashSet("WDT", "WP", "WP$", "WRB");
  public static Set<String> questionPosTags = Sets.newHashSet("WP", "WP$", "WDT", "WRB");

  private static Set<String> modalityPosTags = Sets.newHashSet("MD");

  private static Set<String> quantifierPosTags = Sets.newHashSet("DT");
  private static Set<String> negationLemmas = Sets.newHashSet("not", "n't");
  public static Set<String> complementLemmas = Sets.newHashSet("no");

  // TODO: currently candc markedup file has one-to-one mappings. Could handle
  // one-to-many using Map<String, List<String>>
  protected Map<String, String> synCatToIndexSynCatMap = Maps.newHashMap();

  protected Map<String, String> unaryRulesMap = Maps.newHashMap();

  protected Map<String, String> binaryRulesMap = Maps.newHashMap();

  protected Map<String, List<Pair<String, String>>> specialCases = Maps.newHashMap();

  // all the patterns that are used frequently
  private static Pattern multFeaturesPattern = Pattern.compile("\\](\\[[^\\]+]\\])+");
  private static Pattern nonBasicFeatures = Pattern.compile("\\)(\\[[^\\]+]\\])+");
  private static Pattern depPattern = Pattern.compile("\\{([^\\}]+)\\}\\<([^\\>]+)\\>");
  private static Pattern longDistanceIndex = Pattern.compile("\\*}");
  private static Pattern indexToSimplePattern = Pattern.compile("\\{[^\\}]+\\}");

  public static String cleanIndexedDepString(String indexedString) {
    // replace multiple features with one feature TODO future
    indexedString = multFeaturesPattern.matcher(indexedString).replaceAll("]");
    // replace nonBasic features e.g. (S\NP)[conj] TODO future
    indexedString = nonBasicFeatures.matcher(indexedString).replaceAll("");
    // no distinction between long range and short range
    indexedString = longDistanceIndex.matcher(indexedString).replaceAll("}");

    // Exceptions handling
    indexedString = ppHandling(indexedString);

    // TODO: currently relation names e.g. nsubj. dobj are not used. May be
    // useful
    Matcher deps = depPattern.matcher(indexedString);
    List<String> depStrings = Lists.newArrayList();
    while (deps.find()) {
      depStrings.add("_ " + deps.group(2) + " " + deps.group(1));
    }
    String depString = Joiner.on(",").join(depStrings);
    indexedString = indexedString.replaceAll("\\<[^\\>]+\\>", "");
    indexedString = indexedString + ";" + depString;
    return indexedString;
  }

  public String getIndexedSyntacticCategory(String categoryString) {
    if (synCatToIndexSynCatMap.containsKey(categoryString)) {
      return synCatToIndexSynCatMap.get(categoryString);
    }
    return null;
  }

  private static Pattern ppPattern = Pattern.compile("PP\\{([^\\}]+)\\}");

  private static String ppHandling(String indexedString) {
    // Special case for handling PPs. Treating head of a PP as
    // the head of word that PP as argument.
    Matcher ppMatcher = ppPattern.matcher(indexedString);
    Set<String> ppVars = Sets.newHashSet();
    while (ppMatcher.find()) {
      ppVars.add(ppMatcher.group(1));
    }

    if (ppVars.size() > 0) {
      int count = 1;
      String indexedStringStripped =
          Pattern.compile("\\<[^\\>]+\\>").matcher(indexedString).replaceAll("");
      SyntacticCategory indexedSyntacticCategory =
          SyntacticCategory.fromString(indexedStringStripped);
      for (String ppVar : ppVars) {
        if (ppVar.equals("_")) {
          indexedString = indexedString.replaceAll("PP\\{_\\}", "PP\\{X" + count + "\\}");
          count++;
        } else {
          String varName =
              indexedSyntacticCategory.getDeepParentCategory().getIndex().getVariableName();
          indexedString = Pattern.compile("PP\\{" + ppVar + "\\}(\\<[^\\>]+\\>)?")
              .matcher(indexedString).replaceAll("PP\\{" + varName + "\\}");
        }
      }
    }
    return indexedString;
  }

  /**
   * read candc markedup file
   *
   * @param fileName
   * @throws IOException
   */
  public void mapSynCatToIndexSynCatFromFile(String fileName) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    try {
      String line = br.readLine();
      while (line != null) {
        if (!line.equals("") && line.charAt(0) != ' ' && line.charAt(0) != '#'
            && line.charAt(0) != '=') {
          String synCat = line.trim();
          line = br.readLine();
          List<String> parts = Lists.newArrayList(
              Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings().split(line));
          String indexedSynCat = parts.get(1);
          indexedSynCat = cleanIndexedDepString(indexedSynCat);
          synCatToIndexSynCatMap.put(synCat, indexedSynCat);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public CcgAutoLexicon(String synCatToIndexSynCatFile, String unaryRulesFile,
      String binaryRulesFile, String specialCasesFile) throws IOException {
    mapSynCatToIndexSynCatFromFile(synCatToIndexSynCatFile);
    mapUnaryRules(unaryRulesFile);
    mapBinaryRules(binaryRulesFile);
    mapSpecialRules(specialCasesFile);
  }

  private void mapBinaryRules(String binaryRulesFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(binaryRulesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (!line.equals("") && line.charAt(0) != '#') {
          String rule = line;
          String plainRule = indexToSimplePattern.matcher(rule).replaceAll("");
          String[] parts = plainRule.split("\t");
          SyntacticCategory syncat1 = SyntacticCategory.fromString(parts[0]);
          SyntacticCategory syncat2 = SyntacticCategory.fromString(parts[1]);
          SyntacticCategory syncat3 = SyntacticCategory.fromString(parts[2]);
          parts[0] = syncat1.toSimpleString();
          parts[1] = syncat2.toSimpleString();
          parts[2] = syncat3.toSimpleString();
          plainRule = parts[0] + '\t' + parts[1] + "\t" + parts[2];
          binaryRulesMap.put(plainRule, rule);

          parts[0] = syncat1.toSuperSimpleString();
          parts[1] = syncat2.toSuperSimpleString();
          parts[2] = syncat3.toSuperSimpleString();
          plainRule = parts[0] + '\t' + parts[1] + "\t" + parts[2];
          binaryRulesMap.put(plainRule, rule);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  private void mapUnaryRules(String unaryRulesFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(unaryRulesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (!line.equals("") && line.charAt(0) != '#') {
          String rule = line;
          String plainRule = indexToSimplePattern.matcher(rule).replaceAll("");
          String[] parts = plainRule.split("\t");
          SyntacticCategory syncat1 = SyntacticCategory.fromString(parts[0]);
          SyntacticCategory syncat2 = SyntacticCategory.fromString(parts[1]);
          parts[0] = syncat1.toSimpleString();
          parts[1] = syncat2.toSimpleString();
          plainRule = parts[0] + '\t' + parts[1];
          unaryRulesMap.put(plainRule, rule);

          parts[0] = syncat1.toSuperSimpleString();
          parts[1] = syncat2.toSuperSimpleString();
          plainRule = parts[0] + '\t' + parts[1];
          unaryRulesMap.put(plainRule, rule);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  private void mapSpecialRules(String specialCasesFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(specialCasesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (!line.equals("") && line.charAt(0) != '#') {
          String[] parts = line.split("\t");
          parts[2] = SyntacticCategory.fromString(parts[2]).toSimpleString();
          String key = parts[0] + "\t" + parts[1] + "\t" + parts[2];
          Pair<String, String> value = Pair.of(parts[3], parts[4]);
          if (!specialCases.containsKey(key)) {
            List<Pair<String, String>> values = new ArrayList<>();
            values.add(value);
            specialCases.put(key, values);
          } else {
            specialCases.get(key).add(value);
          }

        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }

  }

  public String selectBinaryRule(String synCat1String, String synCat2String,
      String resultSynCatString) {
    String key = synCat1String + '\t' + synCat2String + '\t' + resultSynCatString;
    if (binaryRulesMap.containsKey(key)) {
      return binaryRulesMap.get(key);
    }
    return null;
  }

  public String selectUnaryRule(String synCatString, String resultSynCatString) {
    String key = synCatString + '\t' + resultSynCatString;
    if (unaryRulesMap.containsKey(key)) {
      return unaryRulesMap.get(key);
    }
    return null;
  }

  public List<Category> getCategory(String lemma, String pos, String synCat) {
    List<Category> cats;
    // synCat = SyntacticCategory.fromString(synCat).toSimpleString();
    cats = getSpecialCasesCategory(lemma, pos, synCat);
    if (cats == null) {
      cats = getSpecialCasesCategory(lemma, "*", synCat);
    }
    if (cats == null) {
      cats = getSpecialCasesCategory(lemma, pos, "*");
    }
    if (cats == null) {
      cats = getSpecialCasesCategory("*", pos, synCat);
    }
    if (cats == null) {
      cats = getSpecialCasesCategory(lemma, "*", "*");
    }
    if (cats == null) {
      cats = getSpecialCasesCategory("*", "*", synCat);
    }

    if (cats != null) {
      return cats;
    }

    String indexSynCat = synCat;
    if (synCatToIndexSynCatMap.containsKey(synCat)) {
      indexSynCat = synCatToIndexSynCatMap.get(synCat);
    }
    SyntacticCategory indexedSyntacticCategory = SyntacticCategory.fromString(indexSynCat);

    SemanticCategory semCat = generateSemanticCategory(indexedSyntacticCategory, lemma, pos);
    Category cat = new Category(indexedSyntacticCategory, semCat);
    cats = new ArrayList<>();
    cats.add(cat);
    return cats;
  }

  private List<Category> getSpecialCasesCategory(String lemma, String pos, String synCat) {
    synCat = SyntacticCategory.fromString(synCat).toSimpleString();
    String key = lemma + "\t" + pos + "\t" + synCat;
    if (specialCases.containsKey(key)) {
      List<Pair<String, String>> values = specialCases.get(key);
      List<Category> cats = new ArrayList<>();
      for (Pair<String, String> value : values) {
        String synCatString = value.getLeft();
        String semCatString = value.getRight();
        SyntacticCategory mainSynCat = SyntacticCategory.fromString(synCatString);
        SemanticCategory mainSemCat = null;
        if (SemanticCategoryType.types.contains(semCatString)) {
          SemanticCategoryType semCatType = SemanticCategoryType.valueOf(semCatString);
          mainSemCat = SemanticCategory.generateSemanticCategory(mainSynCat, semCatType);
        } else {
          mainSemCat = SemanticCategory.generateSemanticCategory(mainSynCat, semCatString);
        }
        Category result = new Category(mainSynCat, mainSemCat);
        cats.add(result);
      }
      return cats;
    }
    return null;
  }

  public static Set<String> closedVerbs = Sets.newHashSet("be", "has", "do", "have", "to");

  private SemanticCategory generateSemanticCategory(SyntacticCategory synCat, String lemma,
      String pos) {
    SemanticCategory result = null;
    if (typePosTags.contains(pos) && synCat.isBasic()) {
      result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.TYPE);
    } else if (synCat.isBasic()) {
      result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.IDENTITY);
    } else if (typeModPosTags.contains(pos) || typePosTags.contains(pos)) {
      if (!synCat.getDeepParentCategory().toSuperSimpleString().equals("S")) {
        if (complementLemmas.contains(lemma)) {
          result =
              SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.COMPLEMENT);
        } else {
          result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.TYPEMOD);
        }
      } else {
        if (negationLemmas.contains(lemma)) {
          result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.NEGATION);
        } else if (synCat.getDeepCategoryIndex().equals(synCat.getIndex())) {
          //
          result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.TYPEMOD);
        } else {
          result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.EVENTMOD);
        }
      }
    } else if (eventModPosTags.contains(pos)) {
      if (negationLemmas.contains(lemma)) {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.NEGATION);
      } else {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.EVENTMOD);
      }
    }
    // if the postag implies an event
    else if (eventPosTags.contains(pos) || modalityPosTags.contains(pos)) {
      if (closedVerbs.contains(lemma) && !synCat.getArgument().isBasic()) {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.CLOSED);
      } else {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.EVENT);
      }
    } else if (closedPosTags.contains(pos)) {
      if (questionPosTags.contains(pos)
          && synCat.getDeepParentCategory().toSimpleString().equals("S[wq]")) {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.QUESTION);
      } else {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.CLOSED);
      }
    } else if (quantifierPosTags.contains(pos)) {
      if (complementLemmas.contains(lemma)) {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.COMPLEMENT);
      } else {
        result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.CLOSED);
      }
    } else {
      result = SemanticCategory.generateSemanticCategory(synCat, SemanticCategoryType.CLOSED);
    }
    return result;
  }
}
