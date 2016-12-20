package in.sivareddy.graphparser.util;

import org.ejml.simple.SimpleMatrix;

import edu.stanford.nlp.neural.Embedding;
import edu.stanford.nlp.neural.NeuralUtils;

public class CrossLingualEmbeddingSimilarity extends Embedding {
  public CrossLingualEmbeddingSimilarity(String embeddingFile) {
    super(embeddingFile);
  }
  
  public double cosine(String word1, String word2) {
    SimpleMatrix embedding1 = this.get(word1);
    if (embedding1 == null) return 0.0;
    
    SimpleMatrix embedding2 = this.get(word2);
    if (embedding2 == null) return 0.0;
    
    double sim = NeuralUtils.cosine(embedding1, embedding2);
    if (sim < 0.0)
      return 0.0;
    return sim;
  }

  public static double cosine(SimpleMatrix vector1, SimpleMatrix vector2) {
    double sim =
        NeuralUtils.dot(vector1, vector2) / (vector1.normF() * vector2.normF());
    return sim < 0.0 ? 0.0 : sim;
  }
  
  public static void main(String[] args) {
    CrossLingualEmbeddingSimilarity embeddings = new CrossLingualEmbeddingSimilarity("data/en-es-de.translation_invariance.emb");
    System.out.println(embeddings.cosine("de:männer", "en:men"));
    System.out.println(embeddings.cosine("de:männer", "es:hombres"));
    System.out.println(embeddings.cosine("de:männer", "es:mujer"));
    System.out.println(embeddings.cosine("en:house", "en:city"));
    System.out.println(embeddings.cosine("en:home", "en:city"));
  }

}
