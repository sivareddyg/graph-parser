package in.sivareddy.paraphrasing;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CreateForestFromSpectralParaphrases {
  private Map<String, JsonObject> sentenceToEntities = new HashMap<>();
  private Map<String, List<JsonObject>> sentenceToParaphrases = new HashMap<>();
  private JsonParser jsonParser = new JsonParser();
  private Gson gson = new Gson();

  public CreateForestFromSpectralParaphrases(InputStream entityDisambiguation,
      InputStream paraphrases) throws IOException {
    readEntityDisambiguations(entityDisambiguation);
    readParaphrases(paraphrases);
  }

  void readEntityDisambiguations(InputStream entityDisambiguation)
      throws IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(entityDisambiguation));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        sentenceToEntities.put(sentence.get(SentenceKeys.SENTENCE_KEY)
            .getAsString(), sentence);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  void readParaphrases(InputStream paraphrasesStream) throws IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(paraphrasesStream));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        String sent = sentence.get("original").getAsString();
        sent = sent.replace(" ?", "?");
        List<JsonObject> paraphrases =
            sentenceToParaphrases.getOrDefault(sent, new ArrayList<>());
        paraphrases.add(sentence);
        sentenceToParaphrases.putIfAbsent(sent, paraphrases);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  void process() {
    for (String sent : sentenceToEntities.keySet()) {
      JsonObject sentObj = sentenceToEntities.get(sent);
      JsonArray disambiguatedEntities =
          sentObj.get(SentenceKeys.DISAMBIGUATED_ENTITIES).getAsJsonArray();

      System.err.println(sent);
      for (JsonObject paraphraseObj : sentenceToParaphrases.get(sent)) {
        String paraphrase =
            paraphraseObj.getAsJsonObject().get("utterance").getAsString();
        Double paraphraseScore =
            paraphraseObj.getAsJsonObject().get("utteranceScore").getAsDouble();
        List<String> words =
            Splitter.on(CharMatcher.WHITESPACE).trimResults()
                .omitEmptyStrings().splitToList(paraphrase);

        JsonArray wordsArr = new JsonArray();
        for (String word : words) {
          JsonObject wordObj = new JsonObject();
          wordObj.addProperty(SentenceKeys.WORD_KEY, word);
          wordsArr.add(wordObj);
        }

        JsonArray newDisambiguatedEntites = new JsonArray();
        for (JsonElement entitiesElm : disambiguatedEntities) {
          JsonArray entities =
              entitiesElm.getAsJsonObject().get(SentenceKeys.ENTITIES)
                  .getAsJsonArray();
          boolean paraphraseContainsEntities = true;
          for (JsonElement entityElm : entities) {
            JsonObject entityObj = entityElm.getAsJsonObject();
            String phrase = entityObj.get(SentenceKeys.PHRASE).getAsString();
            if (!paraphrase.contains(phrase)) {
              paraphraseContainsEntities = false;
              break;
            }
          }

          if (paraphraseContainsEntities) {
            JsonArray newEntities = new JsonArray();
            for (JsonElement entityElm : entities) {
              JsonObject newEntity =
                  jsonParser.parse(gson.toJson(entityElm)).getAsJsonObject();
              JsonObject entityObj = entityElm.getAsJsonObject();
              String phrase = entityObj.get(SentenceKeys.PHRASE).getAsString();
              List<String> phraseWords =
                  Splitter.on(CharMatcher.WHITESPACE).trimResults()
                      .omitEmptyStrings().splitToList(phrase);
              for (int i = 0; i < words.size(); i++) {
                if (words.get(i).equals(phraseWords.get(0))
                    && words.size() >= (i + phraseWords.size())
                    && words.subList(i, i + phraseWords.size()).equals(
                        phraseWords)) {
                  newEntity.addProperty(SentenceKeys.START, i);
                  newEntity.addProperty(SentenceKeys.END,
                      i + phraseWords.size() - 1);
                  newEntities.add(newEntity);
                  break;
                }
              }
            }
            JsonObject newEntitiesObj = new JsonObject();
            newEntitiesObj.add(SentenceKeys.SCORE, entitiesElm
                .getAsJsonObject().get(SentenceKeys.SCORE));
            newEntitiesObj.add(SentenceKeys.ENTITIES, newEntities);
            newDisambiguatedEntites.add(newEntitiesObj);
          }
        }

        JsonObject newSentObj =
            jsonParser.parse(gson.toJson(sentObj)).getAsJsonObject();
        newSentObj.add(SentenceKeys.DISAMBIGUATED_ENTITIES,
            newDisambiguatedEntites);
        newSentObj.add(SentenceKeys.WORDS_KEY, wordsArr);
        newSentObj.addProperty(SentenceKeys.PARAPHRASE_SCORE, paraphraseScore);
        newSentObj.addProperty(SentenceKeys.PARAPHRASE,
            Joiner.on(" ").join(words));
        newSentObj.remove(SentenceKeys.MATCHED_ENTITIES);
        System.out.println(gson.toJson(newSentObj));
      }
    }
  }

  public static void main(String[] args) throws IOException {
    CreateForestFromSpectralParaphrases engine =
        new CreateForestFromSpectralParaphrases(new FileInputStream(args[0]),
            System.in);
    engine.process();
  }
}
