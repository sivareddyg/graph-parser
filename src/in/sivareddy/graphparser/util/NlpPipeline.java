package in.sivareddy.graphparser.util;

import in.sivareddy.graphparser.util.EntityAnnotator;
import in.sivareddy.graphparser.util.MergeEntity;
import in.sivareddy.others.CcgSyntacticParserCli;
import in.sivareddy.others.EasyCcgCli;
import in.sivareddy.others.EasySRLCli;
import in.sivareddy.others.RenderSVG;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.core.exception.MaltChainedException;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class NlpPipeline extends ProcessStreamInterface {
  public static String ANNOTATORS_KEY = "annotators";
  public static String POS_ANNOTATOR = "pos";
  public static String NER_ANNOTATOR = "ner";
  public static String PARSER_ANNOTATOR = "parser";
  public static String TOKENIZE_ANNOTATOR = "tokenize";
  public static String SENTENCE_SPLIT_ANNOTATOR = "ssplit";
  public static String LEMMA_ANNOTATOR = "lemma";

  // Pre-processing
  public static String PREPROCESS_CAPITALIZE_USING_POSTAGS =
      "preprocess.capitalizeUsingPosTags";
  public static String PREPROCESS_ADD_DATE_ENTITIES =
      "preprocess.addDateEntities";
  public static String PREPROCESS_LOWERCASE = "preprocess.lowerCase";
  public static String PREPROCESS_CAPITALIZE_ENTITIES =
      "preprocess.capitalizeEntities";
  public static String PREPROCESS_CAPITALIZE_FIRST_WORD =
      "preprocess.capitalizeFirstWord";
  public static String PREPROCESS_MERGE_ENTITY_WORDS =
      "preprocess.mergeEntityWords";

  public static String LANGUAGE_KEY = "languageCode";
  public static String POS_TAG_KEY = "posTagKey";
  public static String MALT_PARSER_KEY = "maltparser";
  public static String STANFORD_DEP_PARSER_KEY = "depparse";
  public static String DRAW_SVG_TREES = "drawSvgTrees";
  public static String SVG_ZOOM_FACTOR = "svgZoomFactor";
  public static String WHITESPACE_TOKENIZER = "tokenize.whitespace";
  public static String SENTENCE_EOL_SPLITTER = "ssplit.eolonly";
  public static String NEW_LINE_IS_SENTENCE_BREAK =
      "ssplit.newlineIsSentenceBreak";

  public static String POSTPROCESS_CORRECT_POS_TAGS =
      "postprocess.correctPosTags";
  public static String POSTPROCESS_REMOVE_MULTIPLE_ROOTS =
      "postprocess.removeMultipleRoots";

  public static String DEPLAMBDA = "deplambda";
  public static String DEPLAMBDA_LEXICALIZE_PREDICATES =
      "deplambda.lexicalizePredicates";
  public static String DEPLAMBDA_TREE_TRANSFORMATIONS_FILE =
      "deplambda.treeTransformationsFile";
  public static String DEPLAMBDA_DEFINED_TYPES_FILE =
      "deplambda.definedTypesFile";
  public static String DEPLAMBDA_RELATION_PRORITIES_FILE =
      "deplambda.relationPrioritiesFile";
  public static String DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE =
      "deplambda.lambdaAssignmentRulesFile";
  public static String DEPLAMBDA_DEBUG = "deplambda.debug";

  // Accepted values for ccgParser are easyccg and easysrl
  public static String CCGPARSER_KEY = "ccgParser";
  public static String CCGPARSER_NBEST_KEY = "ccgParser.nbest";
  public static String CCGPARSER_MODEL_FOLDER_KEY = "ccgParser.modelFolder";

  // Easyccg question model takes these arguments -s,-r,S[q],S[qem],S[wq]
  // EasySRL question model arguments are --rootCategories,S[q],S[qem],S[wq]
  public static String CCGPARSER_ARGUMENTS = "ccgParser.parserArguments";

  private static Gson gson = new Gson();

  private StanfordCoreNLP pipeline;
  private Map<String, String> options;
  private Set<String> annotators;
  ConcurrentMaltParserModel maltModel = null;
  private CcgSyntacticParserCli ccgParser = null;
  RenderSVG svgRenderer = null;

  public NlpPipeline(Map<String, String> options) throws Exception {
    System.err.println(options);
    this.options = options;

    Properties props = new Properties();
    annotators =
        options.containsKey(ANNOTATORS_KEY) ? Arrays
            .asList(options.get(ANNOTATORS_KEY).split(",")).stream()
            .map(String::trim).collect(Collectors.toSet()) : new HashSet<>();

    if (annotators.contains(STANFORD_DEP_PARSER_KEY)) {
      // Stanford parser picks up default parsing model if
      // deparse.extradependencies is not set.
      options.putIfAbsent("depparse.extradependencies", "ref_only_uncollapsed");
    }

    options.entrySet().stream()
        .forEach(option -> props.put(option.getKey(), option.getValue()));

    System.err.println("NlpPipeline Specified Options : " + props);
    pipeline = new StanfordCoreNLP(props);

    if (options.containsKey(MALT_PARSER_KEY)) {
      URL modelURL = null;
      try {
        modelURL = new File(options.get(MALT_PARSER_KEY)).toURI().toURL();
        maltModel = ConcurrentMaltParserService.initializeParserModel(modelURL);
      } catch (MalformedURLException | MaltChainedException e) {
        e.printStackTrace();
      }
    }
    
    if (options.containsKey(CCGPARSER_KEY)) {
      int nbestParses =
          Integer.parseInt(options.getOrDefault(CCGPARSER_NBEST_KEY, "1"));
      // CCG Parser.
      String ccgModelDir = options.get(CCGPARSER_MODEL_FOLDER_KEY);
      String parserArgs = Joiner.on(" ")
          .join(options.getOrDefault(CCGPARSER_ARGUMENTS, "").split(","));
      if (options.get(CCGPARSER_KEY).equals("easyccg")) {
        ccgParser = new EasyCcgCli(ccgModelDir + " " + parserArgs, nbestParses);
      } else if (options.get(CCGPARSER_KEY).equals("easysrl")) {
        ccgParser = new EasySRLCli(ccgModelDir + " " + parserArgs, nbestParses);
      }
    }

    if (options.containsKey(DRAW_SVG_TREES)) {
      svgRenderer = new RenderSVG();
      if (options.containsKey(SVG_ZOOM_FACTOR)) {
        svgRenderer.setZoomInFactor(Double.parseDouble(options
            .get(SVG_ZOOM_FACTOR)));
      }
    }
  }

  @Override
  public void processSentence(JsonObject jsonSentence) {
    if (jsonSentence.has(SentenceKeys.FOREST)) {
      for (JsonElement individualSentence : jsonSentence.get(
          SentenceKeys.FOREST).getAsJsonArray()) {
        processIndividualSentence(individualSentence.getAsJsonObject());
      }
    } else {
      processIndividualSentence(jsonSentence);
    }
  }

  public void processIndividualSentence(JsonObject jsonSentence) {
    String sentence;
    JsonArray words;

    // Capitalize first word
    if (options.containsKey(PREPROCESS_LOWERCASE)
        && options.get(PREPROCESS_LOWERCASE).equals("true")) {
      lowerCase(jsonSentence);
    }

    // Capitalize using PoS tags before running the pipeline.
    if (options.containsKey(PREPROCESS_CAPITALIZE_USING_POSTAGS)
        && options.get(PREPROCESS_CAPITALIZE_USING_POSTAGS).equals("true")) {
      Preconditions.checkArgument(options.containsKey(WHITESPACE_TOKENIZER)
          && options.get(WHITESPACE_TOKENIZER).equals("true"),
          "Capitalization requires whitespace tokenizer");
      Preconditions.checkArgument(options.containsKey(POS_TAG_KEY),
          String.format("Capitalization requires %s key", POS_TAG_KEY));
      Preconditions.checkArgument(options.containsKey(LANGUAGE_KEY),
          "Capitalization requires languageCode argument");
      String posTagCode = options.get(POS_TAG_KEY);
      String languageCode = options.get(LANGUAGE_KEY);
      capitalizeUsingPosTags(jsonSentence, posTagCode, languageCode);
    }

    // Capitalize before running the pipeline.
    if (options.containsKey(PREPROCESS_CAPITALIZE_ENTITIES)
        && options.get(PREPROCESS_CAPITALIZE_ENTITIES).equals("true")) {
      capitalizeEntities(jsonSentence);
    }

    // Capitalize first word
    if (options.containsKey(PREPROCESS_CAPITALIZE_FIRST_WORD)
        && options.get(PREPROCESS_CAPITALIZE_FIRST_WORD).equals("true")) {
      capitalizeFirstWord(jsonSentence);
    }

    // Add dates.
    if (options.containsKey(PREPROCESS_ADD_DATE_ENTITIES)
        && options.get(PREPROCESS_ADD_DATE_ENTITIES).equals("true")) {
      EntityAnnotator.addDateEntities(jsonSentence);
    }

    // Merge entity words to single sentence.
    if (options.containsKey(PREPROCESS_MERGE_ENTITY_WORDS)
        && options.get(PREPROCESS_MERGE_ENTITY_WORDS).equals("true")) {
      JsonObject mergedSentence =
          MergeEntity.mergeEntityWordsToSingleWord(gson.toJson(jsonSentence));
      for (Entry<String, JsonElement> entry : mergedSentence.entrySet()) {
        jsonSentence.add(entry.getKey(), entry.getValue());
      }
    }

    if (jsonSentence.has(SentenceKeys.WORDS_KEY)) {
      words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
      Preconditions
          .checkArgument(options.containsKey(WHITESPACE_TOKENIZER)
              && options.get(WHITESPACE_TOKENIZER).equals("true"),
              "words are already tokenized. You should use tokenize.whitespace=true");

      Preconditions
          .checkArgument(options.containsKey(WHITESPACE_TOKENIZER)
              && options.get(WHITESPACE_TOKENIZER).equals("true"),
              "input is already tokenized. You should use tokenize.whitespace=true");

      Preconditions
          .checkArgument(
              (options.containsKey(SENTENCE_EOL_SPLITTER) && options.get(
                  SENTENCE_EOL_SPLITTER).equals("true"))
                  || (options.containsKey(NEW_LINE_IS_SENTENCE_BREAK) && options
                      .get(NEW_LINE_IS_SENTENCE_BREAK).equals("always")),
              "input is already tokenized. You should either use "
                  + "ssplit.eolonly=true if you have a single sentence or"
                  + " ssplit.newlineIsSentenceBreak=always if there are multiple sentences");

      StringBuilder sb = new StringBuilder();
      jsonSentence
          .get(SentenceKeys.WORDS_KEY)
          .getAsJsonArray()
          .forEach(
              word -> {
                sb.append(word.getAsJsonObject().get(SentenceKeys.WORD_KEY)
                    .getAsString());
                sb.append(" ");
              });
      sentence = sb.toString().trim();
    } else {
      sentence = jsonSentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
      words = new JsonArray();
    }

    Annotation annotation = new Annotation(sentence);
    pipeline.annotate(annotation);
    int wordCount = 0;
    for (CoreMap sentenceAnnotation : annotation.get(SentencesAnnotation.class)) {
      int sentStart = wordCount;
      for (CoreLabel token : sentenceAnnotation.get(TokensAnnotation.class)) {
        JsonObject wordObject;
        if (jsonSentence.has(SentenceKeys.WORDS_KEY)) {
          Preconditions
              .checkArgument(
                  wordCount < words.size(),
                  "Inconsistent number of already tokenized words, and newly tokenized words. Remove the key 'words' and try again");
          wordObject = words.get(wordCount).getAsJsonObject();
        } else {
          wordObject = new JsonObject();
          words.add(wordObject);
        }

        String word = token.get(TextAnnotation.class);
        wordObject.addProperty(SentenceKeys.WORD_KEY, word);
        if (annotators.contains(LEMMA_ANNOTATOR)) {
          String lemma = token.get(LemmaAnnotation.class);
          wordObject.addProperty(SentenceKeys.LEMMA_KEY, lemma);
        }
        if (annotators.contains(POS_ANNOTATOR)) {
          String pos = token.get(PartOfSpeechAnnotation.class);
          wordObject.addProperty(SentenceKeys.POS_KEY, pos);
        }
        if (annotators.contains(NER_ANNOTATOR)) {
          String ner = token.get(NamedEntityTagAnnotation.class);
          wordObject.addProperty(SentenceKeys.NER_KEY, ner);
        }
        if (options.containsKey(LANGUAGE_KEY)) {
          wordObject.addProperty(SentenceKeys.LANGUAGE_CODE,
              options.get(LANGUAGE_KEY));
        }
        wordCount += 1;
      }

      if (annotators.contains(STANFORD_DEP_PARSER_KEY)) {
        SemanticGraph depTree =
            sentenceAnnotation
                .get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        for (IndexedWord root : depTree.getRoots()) {
          int rootIndex = sentStart + root.index();
          JsonObject rootWord = words.get(rootIndex - 1).getAsJsonObject();
          rootWord.addProperty(SentenceKeys.DEPENDENCY_KEY, "root");
          rootWord.addProperty(SentenceKeys.HEAD_KEY, 0);
          rootWord.addProperty(SentenceKeys.INDEX_KEY, rootIndex);
        }
        for (SemanticGraphEdge edge : depTree.edgeIterable()) {
          int dependentIndex = sentStart + edge.getDependent().index();
          int sourceIndex = sentStart + edge.getSource().index();
          String relation = edge.getRelation().getShortName();

          JsonObject dependent =
              words.get(dependentIndex - 1).getAsJsonObject();
          dependent.addProperty(SentenceKeys.INDEX_KEY, dependentIndex);
          dependent.addProperty(SentenceKeys.HEAD_KEY, sourceIndex);
          dependent.addProperty(SentenceKeys.DEPENDENCY_KEY, relation);
        }
      }

      int sentEnd = wordCount;
      if (sentEnd != sentStart) {
        JsonObject wordObject = words.get(sentEnd - 1).getAsJsonObject();
        wordObject.addProperty(SentenceKeys.SENT_END, true);
      }
    }


    if (options.containsKey(MALT_PARSER_KEY)) {
      int sentStart = 0;
      int currentWordIndex = -1;
      List<String> sentTokens = new ArrayList<>();
      for (JsonElement word : words) {
        currentWordIndex++;
        JsonObject currentWord = word.getAsJsonObject();
        sentTokens.add(String.format("%d\t%s\t_\t%s\t_\t_", currentWordIndex
            - sentStart + 1, currentWord.get(SentenceKeys.WORD_KEY)
            .getAsString(), currentWord.has(SentenceKeys.POS_KEY) ? currentWord
            .get(SentenceKeys.POS_KEY).getAsString() : "_"));

        if ((currentWord.has(SentenceKeys.SENT_END) && currentWord.get(
            SentenceKeys.SENT_END).getAsBoolean())
            || currentWordIndex + 1 == words.size()) {
          try {
            String[] sentTokensArr = new String[sentTokens.size()];
            sentTokensArr = sentTokens.toArray(sentTokensArr);
            sentTokens = new ArrayList<>();

            String[] parsedTokens = maltModel.parseTokens(sentTokensArr);
            for (int i = 0; i < parsedTokens.length; i++) {
              String[] parts = parsedTokens[i].split("\t");
              int head = Integer.parseInt(parts[6]);
              if (head != 0)
                head = head + sentStart;
              String label = parts[7];
              JsonObject wordObject =
                  words.get(i + sentStart).getAsJsonObject();
              wordObject.addProperty(SentenceKeys.INDEX_KEY, i + sentStart + 1);
              wordObject.addProperty(SentenceKeys.HEAD_KEY, head);
              wordObject.addProperty(SentenceKeys.DEPENDENCY_KEY, label);
            }
          } catch (MaltChainedException e) {
            e.printStackTrace();
          }
          sentStart = currentWordIndex + 1;
        }
      }
    }
    
    // Annotate with CCG Parses only if the sentence does not have a CCG Parse.
    if (options.containsKey(CCGPARSER_KEY)
        && !jsonSentence.has(SentenceKeys.CCG_PARSES)) {
      List<String> processedWords = new ArrayList<>();
      for (JsonElement word : words) {
        JsonObject wordObj = word.getAsJsonObject();
        processedWords.add(String.format("%s|%s|O",
            wordObj.get(SentenceKeys.WORD_KEY).getAsString(),
            wordObj.get(SentenceKeys.POS_KEY).getAsString()));
      }
      
      try {
        List<String> parses =
            ccgParser.parse(Joiner.on(" ").join(processedWords));

        JsonArray jsonParses = new JsonArray();
        for (String parse : parses) {
          JsonObject synPar = new JsonObject();
          synPar.addProperty(SentenceKeys.CCG_PARSE, parse);
          synPar.addProperty(SentenceKeys.SCORE, 1.0);
          jsonParses.add(synPar);
        }
        
        if (parses.size() > 0)
          jsonSentence.add(SentenceKeys.CCG_PARSES, jsonParses);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (!jsonSentence.has(SentenceKeys.WORDS_KEY))
      jsonSentence.add(SentenceKeys.WORDS_KEY, words);

    if (options.containsKey(DRAW_SVG_TREES)
        && options.get(DRAW_SVG_TREES).equals("true")) {
      try {
        jsonSentence.addProperty(SentenceKeys.SVG_TREES,
            svgRenderer.drawSVGTrees(jsonSentence));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Correct PoS Tags
    if (options.containsKey(POSTPROCESS_CORRECT_POS_TAGS)
        && options.get(POSTPROCESS_CORRECT_POS_TAGS).equals("true")) {
      Preconditions.checkArgument(options.containsKey(POS_TAG_KEY),
          String.format("Correcting POS tag requires %s key", POS_TAG_KEY));
      String posTagCode = options.get(POS_TAG_KEY);
      correctPosTags(jsonSentence, posTagCode);
    }

    // Remove multiple roots
    if (options.containsKey(POSTPROCESS_REMOVE_MULTIPLE_ROOTS)
        && options.get(POSTPROCESS_REMOVE_MULTIPLE_ROOTS).equals("true")) {
      removeMultipleRoots(jsonSentence);
    }
  }

  private void lowerCase(JsonObject jsonSentence) {
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    for (JsonElement wordElm : words) {
      JsonObject word = wordElm.getAsJsonObject();
      word.addProperty(SentenceKeys.WORD_KEY, word.get(SentenceKeys.WORD_KEY)
          .getAsString().toLowerCase());
    }
  }

  private void removeMultipleRoots(JsonObject jsonSentence) {
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    Set<Integer> roots = new HashSet<>();
    Integer lastRootIndex = -1;
    for (JsonElement wordElm : words) {
      JsonObject word = wordElm.getAsJsonObject();
      if (!word.has(SentenceKeys.HEAD_KEY))
        continue;

      if (word.get(SentenceKeys.HEAD_KEY).getAsString().endsWith("_"))
        continue;

      int head = word.get(SentenceKeys.HEAD_KEY).getAsInt();
      if (head <= 0) {
        int currentIndex = word.get(SentenceKeys.INDEX_KEY).getAsInt();
        roots.add(currentIndex);
        lastRootIndex = currentIndex;
      }
    }

    if (roots.size() > 1) {
      for (JsonElement wordElm : words) {
        JsonObject word = wordElm.getAsJsonObject();
        int head = word.get(SentenceKeys.HEAD_KEY).getAsInt();
        int currentIndex = word.get(SentenceKeys.INDEX_KEY).getAsInt();
        if (roots.contains(head)) {
          word.addProperty(SentenceKeys.HEAD_KEY, lastRootIndex);
        }

        if (roots.contains(currentIndex) && currentIndex != lastRootIndex) {
          word.addProperty(SentenceKeys.HEAD_KEY, lastRootIndex);
          word.addProperty(SentenceKeys.DEPENDENCY_KEY,
              SentenceKeys.DEFAULT_DEPENDENCY_KEY);
        }
      }
    }
  }

  private void correctPosTags(JsonObject sentence, String posTagCode) {
    JsonArray words = sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    for (JsonElement entityElm : sentence.get(SentenceKeys.ENTITIES)
        .getAsJsonArray()) {
      JsonObject entityObj = entityElm.getAsJsonObject();
      if (!entityObj.get(SentenceKeys.ENTITY).getAsString().matches("type.*")) {
        if (entityObj.has(SentenceKeys.END)) {
          makeProperNoun(words.get(entityObj.get(SentenceKeys.END).getAsInt())
              .getAsJsonObject(), posTagCode);
        }

        if (entityObj.has(SentenceKeys.START)) {
          makeProperNoun(words
              .get(entityObj.get(SentenceKeys.START).getAsInt())
              .getAsJsonObject(), posTagCode);
        }

        if (entityObj.has(SentenceKeys.ENTITY_INDEX)) {
          makeProperNoun(
              words.get(entityObj.get(SentenceKeys.ENTITY_INDEX).getAsInt())
                  .getAsJsonObject(), posTagCode);
        }
      }
    }
  }

  private void makeProperNoun(JsonObject word, String posTagCode) {
    if (word.has(SentenceKeys.POS_KEY)) {
      if (posTagCode.equals(SentenceKeys.UNIVERSAL_DEPENDENCIES_POS_TAG_CODE)) {
        word.addProperty(SentenceKeys.POS_KEY, SentenceKeys.UD_PROPER_NOUN_TAG);
      } else if (posTagCode.equals(SentenceKeys.PENN_DEPENDENCIES_POS_TAG_CODE)) {
        word.addProperty(SentenceKeys.POS_KEY,
            SentenceKeys.PENN_PROPER_NOUN_TAG);
      }
    }
  }

  /**
   * Capitalization based on entity annotation.
   * 
   * @param jsonSentence
   */
  private void capitalizeEntities(JsonObject jsonSentence) {
    if (!jsonSentence.has(SentenceKeys.ENTITIES))
      return;

    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    for (JsonElement entityElm : jsonSentence.get(SentenceKeys.ENTITIES)
        .getAsJsonArray()) {
      JsonObject entity = entityElm.getAsJsonObject();
      for (int i = entity.get(SentenceKeys.START).getAsInt(); i <= entity.get(
          SentenceKeys.END).getAsInt(); i++) {
        JsonObject word = words.get(i).getAsJsonObject();
        String wordStr = word.get(SentenceKeys.WORD_KEY).getAsString();
        word.addProperty(SentenceKeys.WORD_KEY, getCasedWord(wordStr));
      }
    }
  }

  private void capitalizeUsingPosTags(JsonObject jsonSentence,
      String posTagCode, String languageCode) {
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();



    // Capitalization based on POS tags.
    for (JsonElement wordElm : words) {
      JsonObject word = wordElm.getAsJsonObject();
      if (!word.has(SentenceKeys.POS_KEY))
        break;
      String posTag = word.get(SentenceKeys.POS_KEY).getAsString();
      if (posTagCode.equals(SentenceKeys.UNIVERSAL_DEPENDENCIES_POS_TAG_CODE)) {
        if (posTag.equals(SentenceKeys.UD_PROPER_NOUN_TAG)
            || (languageCode.equals(SentenceKeys.GERMAN_LANGUAGE_CODE) && posTag
                .equals(SentenceKeys.UD_NOUN_TAG))) {
          String wordStr = word.get(SentenceKeys.WORD_KEY).getAsString();
          word.addProperty(SentenceKeys.WORD_KEY, getCasedWord(wordStr));
        }
      }
    }
  }

  private void capitalizeFirstWord(JsonObject jsonSentence) {
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();

    // Capitalize first word
    if (words.size() > 0) {
      JsonObject word = words.get(0).getAsJsonObject();
      word.addProperty(SentenceKeys.WORD_KEY,
          getCasedWord(word.get(SentenceKeys.WORD_KEY).getAsString()));
    }
  }


  private String getCasedWord(String wordStr) {
    return wordStr.equals("") ? wordStr : String.format("%s%s", wordStr
        .substring(0, 1).toUpperCase(), wordStr.substring(1));
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || args.length % 2 != 0) {
      System.err
          .println("Specify pipeline arguments, e.g., annotator, languageCode, preprocess.capitalize. See the NlpPipelineTest file.");
      System.exit(0);
    }

    Map<String, String> options = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) {
      options.put(args[i], args[i + 1]);
    }

    NlpPipeline englishPipeline = new NlpPipeline(options);
    int nthreads =
        options.containsKey(SentenceKeys.NTHREADS) ? Integer.parseInt(options
            .get(SentenceKeys.NTHREADS)) : 20;
    englishPipeline.processStream(System.in, System.out, nthreads, true);
  }
}
