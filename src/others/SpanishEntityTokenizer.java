/**
 * 
 */
package others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Takes input of the following format
 * 
 * m.01_6xcx "Connie's Pizza, 2373 S. Archer Ave., Chicago, IL"@en
 * 
 * and converts it into
 * 
 * m.01_6xcx "Connie 's Pizza , 2373 S. Archer Ave. , Chicago , IL"@en
 * 
 * @author siva
 *
 */
public class SpanishEntityTokenizer {
  private StanfordCoreNLP pipeline;

  public SpanishEntityTokenizer() {
    Properties props = new Properties();
    props.put("annotators", "tokenize");

    // Tokenize using Spanish settings
    props.setProperty("tokenize.language", "es");
    pipeline = new StanfordCoreNLP(props);
  }

  /**
   * Tokenizes a spanish string.
   * 
   * @param text
   * @return
   */
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
    SpanishEntityTokenizer spanishPipeline = new SpanishEntityTokenizer();

    try {
      String line = br.readLine();
      Pattern entityPattern = Pattern.compile("([^\\s]+) \"(.*)\"@([^\"]+)$");
      while (line != null) {
        Matcher matcher = entityPattern.matcher(line);
        matcher.matches();
        try {
          String entityString = matcher.group(2);
          try {
            String tokenisedEntity = spanishPipeline.processText(entityString);
            System.out.println(String.format("%s \"%s\"@%s", matcher.group(1),
                tokenisedEntity, matcher.group(3)));
          } catch (Exception e) {
            // Error in tokenization.
            System.out.println(String.format("%s \"%s\"@%s", matcher.group(1),
                matcher.group(2), matcher.group(3)));
            System.err.println("Cannot tokenize: " + line);
          }
        } catch (java.lang.IllegalStateException a) {
          // Error in format.
          System.err.println("Wrong format: " + line);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
