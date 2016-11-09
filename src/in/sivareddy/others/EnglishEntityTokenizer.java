/**
 * 
 */
package in.sivareddy.others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Takes input of the following format
 * 
 * m.01_6xcx    Connie's Pizza, 2373 S. Archer Ave., Chicago, IL
 * 
 * and converts it into
 * 
 * m.01_6xcx    Connie 's Pizza , 2373 S. Archer Ave. , Chicago , IL
 * 
 * @author siva
 *
 */
public class EnglishEntityTokenizer {
  private StanfordCoreNLP pipeline;

  public EnglishEntityTokenizer() {
    Properties props = new Properties();
    props.put("annotators", "tokenize");
    
    pipeline = new StanfordCoreNLP(props);
  }

  public String processText(String text) {
    Annotation annotation = new Annotation(text);
    pipeline.annotate(annotation);
    StringBuilder sb = new StringBuilder();
    for (CoreLabel token : annotation
        .get(CoreAnnotations.TokensAnnotation.class)) {
      // this is the text of the token.
      sb.append(token.get(TextAnnotation.class));
      sb.append(" ");
    }
    return sb.toString().trim();
  }

  /**
   * Takes input of the following format
   * 
   * m.01_6xcx "Connie's Pizza, 2373 S. Archer Ave., Chicago, IL"@en
   * 
   * and converts it into
   * 
   * m.01_6xcx "Connie 's Pizza , 2373 S. Archer Ave. , Chicago , IL"@en
   * 
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    EnglishEntityTokenizer englishPipeline = new EnglishEntityTokenizer();

    try {
      String line = br.readLine();
      while (line != null) {
        try {
        String[] parts = line.split("\t", 2);
        String tokenized = englishPipeline.processText(parts[1]);
        System.out.println(String.format("%s\t%s", parts[0], tokenized));
        } catch (Exception e) {
          System.err.println("Cannot tokenize: " + line);
          e.printStackTrace();
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
