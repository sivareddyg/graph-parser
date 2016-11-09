package in.sivareddy.others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class SpanishPosNerParser {
  private StanfordCoreNLP pipeline;
  private JsonParser jsonParser;
  private Gson gson;

  public SpanishPosNerParser() {
    jsonParser = new JsonParser();
    gson = new Gson();

    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, ner, parse");

    // Spanish settings.
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ssplit.eolonly", "true");
    props.setProperty("pos.model",
        "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger");
    props.setProperty("ner.model",
        "edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.crf.ser.gz");
    props.setProperty("ner.applyNumericClassifiers", "false");
    props.setProperty("parse.model",
        "edu/stanford/nlp/models/lexparser/spanishPCFG.ser.gz");
    props.setProperty("ner.useSUTime", "false");

    pipeline = new StanfordCoreNLP(props);
  }

  public String processSentence(String line) {
    JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
    String sentence = jsonSentence.get("sentence").getAsString();
    Annotation annotation = new Annotation(sentence);
    pipeline.annotate(annotation);

    List<Map<String, String>> words = new ArrayList<>();
    for (CoreMap sentenceAnnotation : annotation.get(SentencesAnnotation.class)) {
      for (CoreLabel token : sentenceAnnotation.get(TokensAnnotation.class)) {
        String word = token.get(TextAnnotation.class);
        String pos = token.get(PartOfSpeechAnnotation.class);
        String ne = token.get(NamedEntityTagAnnotation.class);
        Map<String, String> word_map = new HashMap<>();
        word_map.put("word", word);
        word_map.put("pos", pos);
        word_map.put("ner", ne);
        words.add(word_map);
      }
      System.out.println(sentenceAnnotation.get(TreeAnnotation.class));
    }
    String words_string = gson.toJson(words);
    jsonSentence.add("words", jsonParser.parse(words_string));
    return gson.toJson(jsonSentence);
  }

  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    SpanishPosNerParser spanishPipeline = new SpanishPosNerParser();
    try {
      String line = br.readLine();
      while (line != null) {
        try {
          String processedSentence = spanishPipeline.processSentence(line);
          System.out.println(processedSentence);
        } catch (Exception e) {
          // Skip sentence.
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
