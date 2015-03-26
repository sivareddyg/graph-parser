package others;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

public class StanfordCoreNlpDemo {
  private StanfordCoreNLP pipeline;

  public StanfordCoreNlpDemo(String languageCode) {
    Properties props = new Properties();
    if (languageCode.equals("en")) {
      props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
    } else if (languageCode.equals("en-ner")) {
      props.put("annotators", "tokenize, ssplit, pos, lemma");
    } else if (languageCode.equals("es")) {
      props.put("annotators", "tokenize, ssplit, pos, lemma, ner");

      // Tokenize using Spanish settings
      props.setProperty("tokenize.language", "es");
      props.setProperty("pos.model",
          "edu/stanford/nlp/models/pos-tagger/spanish/spanish-distsim.tagger");
      props.setProperty("ner.model",
          "edu/stanford/nlp/models/ner/spanish.ancora.distsim.s512.crf.ser.gz");
      props.setProperty("ner.applyNumericClassifiers", "false");
      props.setProperty("ner.useSUTime", "false");
    }
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
        String prev_pos = "";
        String prev_word = "";
        String prev_ner = "";
        StringBuilder sb = new StringBuilder();
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
          // this is the text of the token
          String word = token.get(TextAnnotation.class);

          // this is the POS tag of the token
          String pos = token.get(PartOfSpeechAnnotation.class);

          // this is the NER label of the token
          String ne = "O";
          if (token.containsKey(NamedEntityTagAnnotation.class))
            ne = token.get(NamedEntityTagAnnotation.class);

          if (pos.startsWith("NNP") && prev_pos.startsWith("NNP")) {
            prev_word += "_" + word;
          } else if (!ne.equals("O") && ne.equals(prev_ner)) {
            prev_word += "_" + word;
          } else {
            if (!prev_word.equals(""))
              sb.append(String.format("%s|%s|%s ", prev_word, prev_pos,
                  prev_ner));
            if (prev_pos.startsWith("NNP")
                && (pos.equals("NN") || pos.equals("NNS")))
              sb.append("'s|IPOS|O ");
            prev_word = word;
          }
          prev_ner = ne;
          prev_pos = pos;
        }
        sb.append(String.format("%s|%s|%s", prev_word, prev_pos, prev_ner));
        pText.add(sb.toString());
      }
    }
    return pText;
  }

  public static void main(String[] args) throws IOException {

    StanfordCoreNlpDemo enlgishPipeline = new StanfordCoreNlpDemo("en-ner");
    System.out.println(enlgishPipeline
        .processText("James Cameron directed Titanic in 1997 for 100$."));

    StanfordCoreNlpDemo spanishPipeline = new StanfordCoreNlpDemo("es");
    System.out
        .println(spanishPipeline
            .processText("Steven Spielberg dirigió la película E.T. en 1982. Ganó varios Oscars."));
  }
}
