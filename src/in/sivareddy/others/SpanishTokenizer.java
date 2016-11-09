package in.sivareddy.others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class SpanishTokenizer {
  private StanfordCoreNLP pipeline;

  public SpanishTokenizer() {
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit");

    // Tokenize using Spanish settings
    props.setProperty("tokenize.language", "es");
    props.setProperty("pos.model",
        "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger");
    props.setProperty("ner.model",
        "edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.crf.ser.gz");
    props.setProperty("ner.applyNumericClassifiers", "false");
    props.setProperty("ner.useSUTime", "false");

    pipeline = new StanfordCoreNLP(props);
  }

  public List<String> processText(String text) {
    List<String> pText = new ArrayList<>();
    Annotation annotation = new Annotation(text);
    pipeline.annotate(annotation);

    List<CoreMap> sentences =
        annotation.get(CoreAnnotations.SentencesAnnotation.class);
    if (sentences != null && sentences.size() > 0) {
      for (CoreMap sentenceIter : sentences) {
        ArrayCoreMap sentence = (ArrayCoreMap) sentenceIter;
        StringBuilder sb = new StringBuilder();
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
          // this is the text of the token
          String word = token.get(TextAnnotation.class);
          sb.append(word + " ");
        }
        pText.add(sb.toString());
      }
    }
    return pText;
  }

  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    SpanishTokenizer spanishPipeline = new SpanishTokenizer();

    try {
      String line = br.readLine();
      while (line != null) {
        try {
          for (String tokenisedLine : spanishPipeline.processText(line)) {
            System.out.println(tokenisedLine);
          }
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
