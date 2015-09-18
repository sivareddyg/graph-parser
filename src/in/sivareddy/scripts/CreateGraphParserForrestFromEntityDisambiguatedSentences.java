package in.sivareddy.scripts;

import in.sivareddy.graphparser.util.MergeEntity;
import in.sivareddy.graphparser.util.SplitForrestToSentences;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import others.EasyCcgCli;
import others.StanfordPipeline;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CreateGraphParserForrestFromEntityDisambiguatedSentences extends
    ProcessStreamInterface {
  private static final Gson gson = new Gson();

  private final StanfordPipeline pipeline;
  private final EasyCcgCli ccgParser;

  public CreateGraphParserForrestFromEntityDisambiguatedSentences(
      StanfordPipeline pipeline, EasyCcgCli ccgParser) {
    this.pipeline = pipeline;
    this.ccgParser = ccgParser;
  }

  public JsonObject runGraphParserPipeline(String disambiguatedSentence)
      throws ArgumentValidationException, IOException, InterruptedException {
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
                  MergeEntity.PROPER_NOUN_TAG);
            }
          }
        }
      }
      List<String> processedWords = new ArrayList<>();
      for (JsonElement word : words) {
        JsonObject wordObj = word.getAsJsonObject();
        processedWords.add(String.format("%s|%s|O",
            wordObj.get(SentenceKeys.WORD_KEY).getAsString(),
            wordObj.get(SentenceKeys.POS_KEY).getAsString()));
      }
      List<String> parses =
          ccgParser.parse(Joiner.on(" ").join(processedWords));
      JsonArray jsonParses = new JsonArray();
      for (String parse : parses) {
        JsonObject synPar = new JsonObject();
        synPar.addProperty(SentenceKeys.CCG_PARSE, parse);
        synPar.addProperty(SentenceKeys.SCORE, 1.0);
        jsonParses.add(synPar);
      }
      mergedSentence.add(SentenceKeys.CCG_PARSES, jsonParses);
      forrest.add(mergedSentence);
    }
    JsonObject forrestObj = new JsonObject();
    forrestObj.add(SentenceKeys.FOREST, forrest);

    if (forrest.size() > 0) {
      // Duplicating other fields in the sentence that applies to the forest.
      JsonObject sentence = forrest.get(0).getAsJsonObject();
      for (Entry<String, JsonElement> entry : sentence.entrySet()) {
        String key = entry.getKey();
        if (!key.equals(SentenceKeys.ENTITIES)
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
    } catch (ArgumentValidationException | IOException | InterruptedException e) {
      e.printStackTrace();
    }
    for (Entry<String, JsonElement> entry : sentence.entrySet()) {
      sentence.remove(entry.getKey());
    }

    for (Entry<String, JsonElement> entry : sentenceNew.entrySet()) {
      sentence.add(entry.getKey(), entry.getValue());
    }
  }
  
  public static void main(String[] args) throws ArgumentValidationException,
      IOException, InterruptedException {
    // Stanford pipeline.
    Map<String, String> options =
        ImmutableMap
            .of("annotators",
                "tokenize, ssplit, pos, lemma",
                StanfordPipeline.WHITESPACE_TOKENIZER,
                "true",
                StanfordPipeline.SENTENCE_EOL_SPLITTER,
                "true",
                "pos.model",
                "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
    StanfordPipeline pipeline = new StanfordPipeline(options);

    // CCG Parser.
    String ccgModelDir =
        Paths.get("lib_data", "easyccg_model_questions").toString();
    int nbestParses = 5;
    EasyCcgCli ccgParser =
        new EasyCcgCli(ccgModelDir + " -s -r S[q] S[qem] S[wq]", nbestParses);

    CreateGraphParserForrestFromEntityDisambiguatedSentences engine =
        new CreateGraphParserForrestFromEntityDisambiguatedSentences(pipeline,
            ccgParser);
    engine.processStream(System.in, System.out, 30, true);
  }
}
