package in.sivareddy.graphparser.util;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.ejml.simple.SimpleMatrix;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import edu.stanford.nlp.neural.Embedding;
import edu.stanford.nlp.neural.NeuralUtils;

public class CrossLingualEmbeddingSimilarity extends Embedding {
  
  private Map<String, SimpleMatrix> edgeEmbeddings = Maps.newConcurrentMap();
  private Map<String, Boolean> hasEdgeEmbeddings = Maps.newConcurrentMap();
  private Map<Pair<String, String>, Double> edgeEmbeddingSimilarities = Maps.newConcurrentMap();
  
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
  
  public double computeEdgeSimilarity(String word, String subEdge, String kbLanguage) {
    Pair<String, String> key = Pair.of(word, subEdge);
    if (edgeEmbeddingSimilarities.containsKey(key))
      return edgeEmbeddingSimilarities.get(key);
    
    SimpleMatrix wordEmbedding = this.get(word);
    if (wordEmbedding == null) {
      edgeEmbeddingSimilarities.put(key, 0.0);
      return 0.0;
    }
    
    if (!hasEdgeEmbeddings.getOrDefault(subEdge, true)) {
      edgeEmbeddingSimilarities.put(key, 0.0);
      return 0.0;
    }
    
    SimpleMatrix edgeEmbedding = getEdgeEmbedding(subEdge, kbLanguage);
    if (edgeEmbedding == null) {
      edgeEmbeddingSimilarities.put(key, 0.0);
      return 0.0;
    }
    
    double sim = CrossLingualEmbeddingSimilarity.cosine(edgeEmbedding, wordEmbedding);
    edgeEmbeddingSimilarities.put(key, sim);
    return sim;
  }
  
  private SimpleMatrix getEdgeEmbedding(String subEdge, String kbLanguage) {
    if (!hasEdgeEmbeddings.getOrDefault(subEdge, true))
      return null;
    
    if (edgeEmbeddings.containsKey(subEdge))
      return edgeEmbeddings.get(subEdge);
    Iterator<String> it =
        Splitter.on(CharMatcher.anyOf("._")).trimResults().omitEmptyStrings()
            .split(subEdge).iterator();
    SimpleMatrix edgeEmbedding = null;
    while (it.hasNext()) {
      String part = String.format("%s:%s", kbLanguage, it.next());
      SimpleMatrix partEmbedding = this.get(part);
      if (partEmbedding != null) {
        if (edgeEmbedding != null) {
          edgeEmbedding = edgeEmbedding.plus(partEmbedding);
        } else {
          edgeEmbedding = partEmbedding;
        }
      }
    }
    if (edgeEmbedding == null) {
      hasEdgeEmbeddings.put(subEdge, false);
    } else {
      hasEdgeEmbeddings.put(subEdge, true);
      edgeEmbeddings.put(subEdge, edgeEmbedding);
    }
    return edgeEmbedding;
  }
  
  public static void main(String[] args) {
    CrossLingualEmbeddingSimilarity embeddings = new CrossLingualEmbeddingSimilarity("data/en-es-de.translation_invariance.emb");
    System.out.println(embeddings.computeEdgeSimilarity("de:geschrieben", "award.presented_by", "en"));
    System.out.println(embeddings.cosine("de:männer", "en:men"));
    
    System.out.println(embeddings.cosine("de:männer", "en:men"));
    System.out.println(embeddings.cosine("de:männer", "es:hombres"));
    System.out.println(embeddings.cosine("de:männer", "es:mujer"));
    System.out.println(embeddings.cosine("en:house", "en:city"));
    System.out.println(embeddings.cosine("en:home", "en:city"));
    System.out.println(embeddings.cosine("en:of", "en:city"));
    System.out.println(embeddings.cosine("en:he", "en:city"));
    System.out.println(embeddings.cosine("en:it", "en:city"));
    System.out.println(embeddings.cosine("en:is", "en:city"));
    System.out.println(embeddings.cosine("en:place", "en:city"));
    System.out.println(embeddings.cosine("en:village", "en:city"));
  }

}
