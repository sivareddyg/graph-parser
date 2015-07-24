package in.sivareddy.scripts;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MergeSplitsToForest {
  Set<String> forestIndices = new HashSet<>();
  Map<String, JsonObject> originalSentences = new HashMap<>();

  JsonParser parser = new JsonParser();
  Gson gson = new Gson();

  public MergeSplitsToForest(InputStream originalFileInputStream,
      InputStream newFileInputStream) throws IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(originalFileInputStream));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = parser.parse(line).getAsJsonObject();
        String index = sentence.get(SentenceKeys.INDEX_KEY).getAsString();
        originalSentences.put(index, sentence);
        line = br.readLine();
      }
    } finally {
      br.close();
    }

    br = new BufferedReader(new InputStreamReader(newFileInputStream));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = parser.parse(line).getAsJsonObject();
        String index = sentence.get(SentenceKeys.INDEX_KEY).getAsString();
        String forestIndex = index.split(":")[0];
        forestIndices.add(forestIndex);
        JsonObject originalSentence = originalSentences.get(index);

        originalSentence.add(SentenceKeys.WORDS_KEY,
            sentence.get(SentenceKeys.WORDS_KEY));
        if (sentence.has(SentenceKeys.DEPENDENCY_LAMBDA)) {
          originalSentence.add(SentenceKeys.DEPENDENCY_LAMBDA,
              sentence.get(SentenceKeys.DEPENDENCY_LAMBDA));
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }


  public void merge() {
    for (String index : forestIndices) {
      int i = 0;
      JsonArray forestArr = new JsonArray();
      while (true) {
        i++;
        String sentenceIndex = index + ":" + i;
        if (!originalSentences.containsKey(sentenceIndex))
          break;
        forestArr.add(originalSentences.get(sentenceIndex));
      }
      if (forestArr.size() == 0) {
        // There are no splits for this sentence. Add the original sentence.
        forestArr.add(originalSentences.get(index));
      }
      JsonObject forest = new JsonObject();
      forest.add(SentenceKeys.FOREST, forestArr);

      JsonObject sentence = forestArr.get(0).getAsJsonObject();
      Set<String> uselessKeys =
          Sets.newHashSet(SentenceKeys.ENTITIES, SentenceKeys.CCG_PARSES,
              SentenceKeys.DEPENDENCY_LAMBDA, SentenceKeys.INDEX_KEY,
              SentenceKeys.WORDS_KEY);

      // Add important information to the forest.
      for (Entry<String, JsonElement> entry : sentence.entrySet()) {
        String key = entry.getKey();
        if (uselessKeys.contains(key)) {
          continue;
        }
        // System.err.println("KEY: " + key);
        forest.add(key, entry.getValue());
      }
      forest.addProperty(SentenceKeys.INDEX_KEY, index);
      System.out.println(gson.toJson(forest));
    }
  }

  public static void main(String[] args) throws IOException {
    InputStream originalFileInputStream = new FileInputStream(args[0]);

    MergeSplitsToForest merger =
        new MergeSplitsToForest(originalFileInputStream, System.in);
    merger.merge();
  }
}
