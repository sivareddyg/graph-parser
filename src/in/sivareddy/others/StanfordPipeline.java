package in.sivareddy.others;

import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class StanfordPipeline extends ProcessStreamInterface {
  public static String ANNOTATORS_KEY = "annotators";
  public static String POS_ANNOTATOR = "pos";
  public static String NER_ANNOTATOR = "ner";
  public static String PARSER_ANNOTATOR = "parser";
  public static String TOKENIZE_ANNOTATOR = "tokenize";
  public static String SENTENCE_SPLIT_ANNOTATOR = "ssplit";
  public static String LEMMA_ANNOTATOR = "lemma";

  public static String LANGUAGE_KEY = "languageCode";
  public static String MALT_PARSER_KEY = "maltparser";
  public static String DRAW_SVG_TREES = "drawSvgTrees";
  public static String SVG_ZOOM_FACTOR = "svgZoomFactor";
  public static String WHITESPACE_TOKENIZER = "tokenize.whitespace";
  public static String SENTENCE_EOL_SPLITTER = "ssplit.eolonly";

  private StanfordCoreNLP pipeline;
  private Map<String, String> options;
  private Set<String> annotators;
  private JsonParser jsonParser;

  public StanfordPipeline(Map<String, String> options) {
    this.options = options;
    jsonParser = new JsonParser();

    Properties props = new Properties();
    this.annotators =
        Arrays.asList(options.get(ANNOTATORS_KEY).split(",")).stream()
            .map(String::trim).collect(Collectors.toSet());

    options.entrySet().stream()
        .forEach(option -> props.put(option.getKey(), option.getValue()));
    pipeline = new StanfordCoreNLP(props);
  }

  public JsonObject processSentence(String sentence) {
    JsonObject jsonSentence = jsonParser.parse(sentence).getAsJsonObject();
    processSentence(jsonSentence);
    return jsonSentence;
  }

  public void processSentence(JsonObject jsonSentence) {
    String sentence;
    JsonArray words;
    if (jsonSentence.has(SentenceKeys.WORDS_KEY)) {
      words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
      Preconditions
          .checkArgument(options.containsKey(WHITESPACE_TOKENIZER)
              && options.get(WHITESPACE_TOKENIZER).equals("true"),
              "words are already tokenized. You should use tokenize.whitespace=true");

      Preconditions.checkArgument(options.containsKey(SENTENCE_EOL_SPLITTER)
          && options.get(SENTENCE_EOL_SPLITTER).equals("true"),
          "input is already tokenized. You should use ssplit.eolonly=true");

      List<String> wordStrings = new ArrayList<>();
      words.forEach(word -> wordStrings.add(word.getAsJsonObject()
          .get(SentenceKeys.WORD_KEY).getAsString()));
      sentence = Joiner.on(" ").join(wordStrings).toString();
    } else {
      sentence = jsonSentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
      words = new JsonArray();
    }

    Annotation annotation = new Annotation(sentence);
    try {
      pipeline.annotate(annotation);
    } catch (Exception e) {
      // pass.
    }

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
        wordObject.addProperty(SentenceKeys.WORD_KEY,
            CharMatcher.WHITESPACE.replaceFrom(word, ""));
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
        wordCount += 1;
      }
      int sentEnd = wordCount;

      if (sentEnd != sentStart) {
        JsonObject wordObject = words.get(sentEnd - 1).getAsJsonObject();
        wordObject.addProperty(SentenceKeys.SENT_END, true);
      }
    }

    if (!jsonSentence.has(SentenceKeys.WORDS_KEY))
      jsonSentence.add(SentenceKeys.WORDS_KEY, words);
  }
}
