package in.sivareddy.paraphrasing;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MergeParaphrasesIntoForest {

  Map<String, List<JsonObject>> sentenceToForest = new HashMap<>();
  Gson gson = new Gson();
  JsonParser jsonParser = new JsonParser();

  public MergeParaphrasesIntoForest(InputStream stream) throws IOException {
    read(stream);
  }

  public void read(InputStream stream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sent = jsonParser.parse(line).getAsJsonObject();
        String sentence = sent.get(SentenceKeys.SENTENCE_KEY).getAsString();
        sentenceToForest.putIfAbsent(sentence, new ArrayList<>());
        sentenceToForest.get(sentence).add(sent);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public void print(int nbest) {
    for (String sentKey : sentenceToForest.keySet()) {
      List<JsonObject> forrests = sentenceToForest.get(sentKey);
      JsonObject finalParse =
          jsonParser.parse(gson.toJson(forrests.get(0))).getAsJsonObject();
      finalParse.remove(SentenceKeys.FOREST);
      JsonArray jsonForrest = new JsonArray();
      List<JsonObject> forrestList = new ArrayList<>();
      for (JsonObject forrest : forrests) {
        for (JsonElement jsonSentenceElm : forrest.get(SentenceKeys.FOREST)
            .getAsJsonArray()) {
          JsonObject jsonSentence = jsonSentenceElm.getAsJsonObject();
          if (jsonSentence.has(SentenceKeys.ENTITIES)
              && jsonSentence.get(SentenceKeys.ENTITIES).getAsJsonArray()
                  .size() > 0) {
            // jsonForrest.add(jsonSentence);
            forrestList.add(jsonSentence);
          }
        }
      }
      forrestList.sort(new ParaphraseComparator().reversed());
      if (forrestList.size() > nbest)
        forrestList =  forrestList.subList(0, nbest);
      forrestList.forEach(x -> jsonForrest.add(x));
      
      finalParse.add(SentenceKeys.FOREST, jsonForrest);
      finalParse.remove(SentenceKeys.PARAPHRASE);
      finalParse.remove(SentenceKeys.PARAPHRASE_SCORE);
      System.out.println(gson.toJson(finalParse));
    }
  }

  public static class ParaphraseComparator implements Comparator<JsonObject> {
    @Override
    public int compare(JsonObject arg0, JsonObject arg1) {
      double score0 = arg0.get(SentenceKeys.PARAPHRASE_SCORE).getAsDouble();
      double score1 = arg1.get(SentenceKeys.PARAPHRASE_SCORE).getAsDouble();

      if (score0 != score1) {
        return Double.compare(score0, score1);
      } else {
        double entityScore0 = 0.0;
        for (JsonElement entityElm : arg0.get(SentenceKeys.ENTITIES)
            .getAsJsonArray()) {
          JsonObject entityObj = entityElm.getAsJsonObject();
          if (entityObj.has(SentenceKeys.SCORE)) {
            entityScore0 += entityObj.get(SentenceKeys.SCORE).getAsDouble();
          }
        }

        double entityScore1 = 0.0;
        for (JsonElement entityElm : arg1.get(SentenceKeys.ENTITIES)
            .getAsJsonArray()) {
          JsonObject entityObj = entityElm.getAsJsonObject();
          if (entityObj.has(SentenceKeys.SCORE)) {
            entityScore1 += entityObj.get(SentenceKeys.SCORE).getAsDouble();
          }
        }
        return Double.compare(entityScore0, entityScore1);
      }
    }
  }

  public static void main(String args[]) throws IOException {
    MergeParaphrasesIntoForest engine =
        new MergeParaphrasesIntoForest(System.in);
    engine.print(Integer.parseInt(args[0]));
  }
}
