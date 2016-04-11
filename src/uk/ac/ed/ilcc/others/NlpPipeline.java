package uk.ac.ed.ilcc.others;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.core.exception.MaltChainedException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

public class NlpPipeline {
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
  private static String WHITESPACE_TOKENIZER = "tokenize.whitespace";
  private static String SENTENCE_EOL_SPLITTER = "ssplit.eolonly";

  private StanfordCoreNLP pipeline;
  private Map<String, String> options;
  private Set<String> annotators;
  private JsonParser jsonParser;
  ConcurrentMaltParserModel maltModel = null;
  RenderSVG svgRenderer = null;

  public NlpPipeline(Map<String, String> options) {
    this.options = options;
    jsonParser = new JsonParser();

    Properties props = new Properties();
    this.annotators =
        Arrays.asList(options.get(ANNOTATORS_KEY).split(",")).stream()
            .map(String::trim).collect(Collectors.toSet());

    options.entrySet().stream()
        .forEach(option -> props.put(option.getKey(), option.getValue()));
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

    if (options.containsKey(DRAW_SVG_TREES)) {
      svgRenderer = new RenderSVG();
      if (options.containsKey(SVG_ZOOM_FACTOR)) {
        svgRenderer.setZoomInFactor(Double.parseDouble(options
            .get(SVG_ZOOM_FACTOR)));
      }
    }
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

      Preconditions
          .checkArgument(options.containsKey(WHITESPACE_TOKENIZER)
              && options.get(WHITESPACE_TOKENIZER).equals("true"),
              "input is already tokenized. You should use tokenize.whitespace=true");

      Preconditions.checkArgument(options.containsKey(SENTENCE_EOL_SPLITTER)
          && options.get(SENTENCE_EOL_SPLITTER).equals("true"),
          "input is already tokenized. You should use ssplit.eolonly=true");

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
        wordCount += 1;
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
  }

  public static void main(String[] args) throws IOException,
      TransformerException {
    Map<String, String> options =
        ImmutableMap.of("annotators", "tokenize, ssplit, pos, lemma",
            "maltparser",
            "lib_data/utb-models/en/parser/en-stackproj-coarse.mco",
            "pos.model",
            "lib_data/utb-models/en/pos-tagger/utb-en-left3words.tagger");

    NlpPipeline englishPipeline = new NlpPipeline(options);
    JsonObject sent =
        englishPipeline
            .processSentence("{\"sentence\":\"Obama is the president of United States. He won a Nobel prize.\"}");

    Map<String, String> newOptions = new HashMap<>();
    newOptions.put("annotators", "tokenize, ssplit, pos, lemma");
    newOptions.put("maltparser",
        "lib_data/utb-models/en/parser/en-stackproj-coarse.mco");
    newOptions.put("pos.model",
        "lib_data/utb-models/en/pos-tagger/utb-en-left3words.tagger");
    newOptions.put("tokenize.whitespace", "true");
    newOptions.put("ssplit.eolonly", "true");
    newOptions.put(DRAW_SVG_TREES, "true");

    NlpPipeline englishWhitPipeline = new NlpPipeline(newOptions);
    englishWhitPipeline.processSentence(sent);
    System.out.println(sent.get(SentenceKeys.SVG_TREES));
    BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/canvas.html"));
    bw.write(sent.get(SentenceKeys.SVG_TREES).getAsString());
    bw.close();
  }
}
