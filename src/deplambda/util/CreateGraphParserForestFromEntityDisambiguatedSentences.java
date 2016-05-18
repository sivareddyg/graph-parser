package deplambda.util;

import in.sivareddy.graphparser.util.MergeEntity;
import in.sivareddy.graphparser.util.SplitForrestToSentences;
import in.sivareddy.util.ProcessStreamInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import deplambda.others.NlpPipeline;
import deplambda.others.SentenceKeys;

public class CreateGraphParserForestFromEntityDisambiguatedSentences extends
    ProcessStreamInterface {
  private static final Gson gson = new Gson();

  private final NlpPipeline pipeline;

  public CreateGraphParserForestFromEntityDisambiguatedSentences(
      NlpPipeline pipeline) {
    this.pipeline = pipeline;
  }

  public JsonObject runGraphParserPipeline(String disambiguatedSentence)
      throws IOException, InterruptedException {
    JsonArray forrest = new JsonArray();
    List<JsonObject> sentences =
        SplitForrestToSentences.split(disambiguatedSentence);
    for (JsonObject sentence : sentences) {
      JsonObject mergedSentence =
          MergeEntity.mergeEntityWordsToSingleWord(gson.toJson(sentence));
      mergedSentence =
          MergeEntity.mergeDateEntities(gson.toJson(mergedSentence));
      pipeline.processSentence(mergedSentence);
      JsonArray words =
          mergedSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
      if (mergedSentence.has(SentenceKeys.ENTITIES)) {
        for (JsonElement entityElm : mergedSentence.get(SentenceKeys.ENTITIES)
            .getAsJsonArray()) {
          JsonObject entityObj = entityElm.getAsJsonObject();
          if (!entityObj.get(SentenceKeys.ENTITY).getAsString()
              .matches("type.*")) {
            JsonObject word =
                words.get(entityObj.get(SentenceKeys.INDEX_KEY).getAsInt())
                    .getAsJsonObject();
            if (word.has(SentenceKeys.POS_KEY)) {
              word.addProperty(SentenceKeys.POS_KEY,
                  SentenceKeys.UD_PROPER_NOUN_TAG);
            }

            if (word.has(SentenceKeys.LEMMA_KEY)) {
              word.addProperty(SentenceKeys.LEMMA_KEY,
                  word.get(SentenceKeys.WORD_KEY).getAsString().toLowerCase());
            }
          }
        }
      }
      forrest.add(mergedSentence);
    }
    JsonObject forrestObj = new JsonObject();
    forrestObj.add(SentenceKeys.FOREST, forrest);

    if (forrest.size() > 0) {
      // Duplicating other fields in the sentence that applies to the forest.
      JsonObject sentence = forrest.get(0).getAsJsonObject();
      for (Entry<String, JsonElement> entry : sentence.entrySet()) {
        String key = entry.getKey();
        if (key.equals(SentenceKeys.INDEX_KEY)) {
          forrestObj.addProperty(key,
              entry.getValue().getAsString().split(":")[0]);
        } else if (!key.equals(SentenceKeys.ENTITIES)
            && !key.equals(SentenceKeys.WORDS_KEY)
            && !key.equals(SentenceKeys.CCG_PARSES)) {
          forrestObj.add(key, entry.getValue());
        }
      }
    }
    return forrestObj;
  }

  @Override
  public void processSentence(JsonObject sentence) {
    JsonObject sentenceNew = null;
    try {
      sentenceNew = runGraphParserPipeline(gson.toJson(sentence));
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
    for (Entry<String, JsonElement> entry : sentence.entrySet()) {
      sentence.remove(entry.getKey());
    }

    for (Entry<String, JsonElement> entry : sentenceNew.entrySet()) {
      sentence.add(entry.getKey(), entry.getValue());
    }
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    if (args.length == 0 || args.length % 2 != 0) {
      System.err
          .println("Specify pipeline arguments, e.g., annotator, languageCode. See the NlpPipelineTest file.");
    }

    Map<String, String> options = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) {
      options.put(args[i], args[i + 1]);
    }

    NlpPipeline pipeline = new NlpPipeline(options);

    CreateGraphParserForestFromEntityDisambiguatedSentences engine =
        new CreateGraphParserForestFromEntityDisambiguatedSentences(pipeline);
    engine.processStream(System.in, System.out, 20, true);
  }
}
