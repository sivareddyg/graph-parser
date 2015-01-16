package others;

import java.io.*;
import java.util.*;

import edu.stanford.nlp.io.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.*;
// import edu.stanford.nlp.semgraph.SemanticGraph;
// import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
// import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.*;

public class StanfordCoreNlpDemo {
  private StanfordCoreNLP pipeline;

  public StanfordCoreNlpDemo() {
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
    pipeline = new StanfordCoreNLP(props);
  }

  public List<String> processText(String text) {
    List<String> pText = new ArrayList<>();
    Annotation annotation = new Annotation(text);
    pipeline.annotate(annotation);

    List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

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
          String ne = token.get(NamedEntityTagAnnotation.class);

          if (pos.startsWith("NNP") && prev_pos.startsWith("NNP")) {
            prev_word += "_" + word;
          } else if (!ne.equals("O") && ne.equals(prev_ner)) {
            prev_word += "_" + word;
          } else {
            if (!prev_word.equals(""))
              sb.append(String.format("%s|%s|%s ", prev_word, prev_pos, prev_ner));
            if (prev_pos.startsWith("NNP") && (pos.equals("NN") || pos.equals("NNS")))
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

    StanfordCoreNlpDemo demo = new StanfordCoreNlpDemo();
    System.out.println(demo
        .processText("He didn't get a reply. Kosgi Santosh sent an email to Hitlon hotel."));

    /*-PrintWriter out;
    if (args.length > 1) {
    	out = new PrintWriter(args[1]);
    } else {
    	out = new PrintWriter(System.out);
    }
    PrintWriter xmlOut = null;
    if (args.length > 2) {
    	xmlOut = new PrintWriter(args[2]);
    }

    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    // StanfordCoreNLP pipeline = new StanfordCoreNLP();
    Annotation annotation;
    if (args.length > 0) {
    	annotation = new Annotation(IOUtils.slurpFileNoExceptions(args[0]));
    } else {
    	annotation = new Annotation(
    			"Kosgi Santosh sent an email to Stanford University. He didn't get a reply.");
    }

    pipeline.annotate(annotation);
    pipeline.prettyPrint(annotation, out);
    if (xmlOut != null) {
    	pipeline.xmlPrint(annotation, xmlOut);
    }

    // An Annotation is a Map and you can get and use the various analyses
    // individually.
    // For instance, this gets the parse tree of the first sentence in the
    // text.
    out.println();
    // The toString() method on an Annotation just prints the text of the
    // Annotation
    // But you can see what is in it with other methods like
    // toShorterString()
    out.println("The top level annotation");
    out.println(annotation.toShorterString());
    List<CoreMap> sentences = annotation
    		.get(CoreAnnotations.SentencesAnnotation.class);
    if (sentences != null && sentences.size() > 0) {
    	ArrayCoreMap sentence = (ArrayCoreMap) sentences.get(0);

    	out.println("The first sentence is:");
    	out.println(sentence.toShorterString());

    	for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
    		// this is the text of the token
    		String word = token.get(TextAnnotation.class);
    		// this is the POS tag of the token
    		String pos = token.get(PartOfSpeechAnnotation.class);
    		// this is the NER label of the token
    		String ne = token.get(NamedEntityTagAnnotation.class);
    		System.out.println(word + " " + pos + " " + ne);
    	} */

    /*-Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
    out.println();
    out.println("The first sentence tokens are:");
    for (CoreMap token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
      ArrayCoreMap aToken = (ArrayCoreMap) token;
      out.println(aToken.toShorterString());
    }
    out.println("The first sentence parse tree is:");
    tree.pennPrint(out);
    out.println("The first sentence basic dependencies are:"); 
    System.out.println(sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class).toString("plain"));
    out.println("The first sentence collapsed, CC-processed dependencies are:");
    SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
    System.out.println(graph.toString("plain"));*/
    // }

  }

}
