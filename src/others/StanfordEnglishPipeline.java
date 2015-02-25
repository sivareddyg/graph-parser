package others;

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

public class StanfordEnglishPipeline {
  private StanfordCoreNLP pipeline;
  private Gson gson;
  private JsonParser jsonParser;

  public StanfordEnglishPipeline(String languageCode) {
    gson = new Gson();
    jsonParser = new JsonParser();
    Properties props = new Properties();
    if (languageCode.equals("en")) {
      props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
      props
          .setProperty("pos.model",
              "edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger");
      props
          .setProperty(
              "ner.model",
              "edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz,"
              + "edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz,"
              + "edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");
      props.setProperty("tokenize.whitespace", "true");
      props.setProperty("ssplit.eolonly", "true");
    }
    pipeline = new StanfordCoreNLP(props);
  }

  public String processText(String sentence) {
    Annotation annotation = new Annotation(sentence);
    pipeline.annotate(annotation);

    JsonObject sentenceObject = new JsonObject();
    List<Map<String, String>> words = new ArrayList<>();
    for (CoreMap sentenceAnnotation : annotation.get(SentencesAnnotation.class)) {
      for (CoreLabel token : sentenceAnnotation.get(TokensAnnotation.class)) {
        String word = token.get(TextAnnotation.class);
        String lemma = token.get(LemmaAnnotation.class);
        String pos = token.get(PartOfSpeechAnnotation.class);
        String ne = token.get(NamedEntityTagAnnotation.class);
        Map<String, String> word_map = new HashMap<>();
        word_map.put("word", word);
        word_map.put("lemma", lemma);
        word_map.put("pos", pos);
        word_map.put("ner", ne);
        words.add(word_map);
      }
    }
    String words_string = gson.toJson(words);
    sentenceObject.add("words", jsonParser.parse(words_string));
    return gson.toJson(sentenceObject);
  }

  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    StanfordEnglishPipeline enlgishPipeline = new StanfordEnglishPipeline("en");
    try {
      String line = br.readLine();
      while (line != null) {
        try {
          String processedSentence = enlgishPipeline.processText(line);
          System.out.println(processedSentence);
        } catch (Exception e) {
          System.err.println("Skipping: " + line);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
