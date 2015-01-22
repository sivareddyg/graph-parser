package others;

import java.io.*;
import java.util.*;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;

import edu.stanford.nlp.util.*;

public class SpanishTokenizer {
  private StanfordCoreNLP pipeline;

  public SpanishTokenizer(String languageCode) {
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
    SpanishTokenizer spanishPipeline = new SpanishTokenizer("es");

    try {
      String line = br.readLine();
      while (line != null) {
        for (String tokenisedLine : spanishPipeline.processText(line)) {
          System.out.println(tokenisedLine);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
