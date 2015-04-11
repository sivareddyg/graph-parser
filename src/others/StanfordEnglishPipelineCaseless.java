package others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
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

/**
 * Reads in a json formatted sentence with words tokenized, and applies nlp
 * pipeline creating new fields.
 * 
 * @author siva
 *
 */
public class StanfordEnglishPipelineCaseless {

  private StanfordCoreNLP pipeline;
  Gson gson = new Gson();
  JsonParser jsonParser = new JsonParser();

  public StanfordEnglishPipelineCaseless(String languageCode) {
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

  public List<Map<String, String>> processSentence(String sentence) {
    Annotation annotation = new Annotation(sentence);
    pipeline.annotate(annotation);
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
    return words;
  }

  public String processJsonSentence(String jsonSentence) {
    JsonElement jelement = jsonParser.parse(jsonSentence);
    JsonObject jobject = jelement.getAsJsonObject();
    JsonArray words = jobject.getAsJsonArray("words");

    List<JsonObject> wordObjects = Lists.newArrayList();
    List<String> wordStrings = Lists.newArrayList();

    for (JsonElement word : words) {
      JsonObject wordObject = word.getAsJsonObject();
      String wordString = gson.fromJson(wordObject.get("word"), String.class);
      wordObjects.add(wordObject);
      wordStrings.add(wordString);
    }

    String sent = Joiner.on(" ").join(wordStrings);
    List<Map<String, String>> processed_words = processSentence(sent);
    for (int i = 0; i < words.size(); i++) {
      JsonObject wordObject = wordObjects.get(i);
      Map<String, String> processed_word = processed_words.get(i);
      for (String key : processed_word.keySet()) {
        wordObject.addProperty(key, processed_word.get(key));
      }
    }
    return gson.toJson(jelement);
  }

  public static void main(String[] args) throws IOException {
    StanfordEnglishPipelineCaseless enlgishPipeline =
        new StanfordEnglishPipelineCaseless("en");
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input;
      while ((input = br.readLine()) != null) {
        System.out.println(enlgishPipeline.processJsonSentence(input));
      }
    } catch (IOException io) {
      io.printStackTrace();
    }
  }
}
