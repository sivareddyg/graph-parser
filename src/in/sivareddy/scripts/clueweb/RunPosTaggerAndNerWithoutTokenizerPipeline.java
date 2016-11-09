package in.sivareddy.scripts.clueweb;

import in.sivareddy.graphparser.util.MergeEntity;
import in.sivareddy.others.StanfordPipeline;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RunPosTaggerAndNerWithoutTokenizerPipeline extends
    ProcessStreamInterface {
  static Map<String, String> options =
      ImmutableMap
          .of("annotators",
              "tokenize, ssplit, pos, lemma, ner",
              "tokenize.whitespace",
              "true",
              "ssplit.eolonly",
              "true",
              "pos.model",
              "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger",
              "ner.model",
              "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
  private static StanfordPipeline englishPipeline = new StanfordPipeline(
      options);
  private static Gson gson = new Gson();

  public void processSentence(JsonObject sentence) {
    try {
    englishPipeline.processSentence(sentence);
    JsonObject sentenceNew =
        MergeEntity.mergeEntityWordsToSingleWord(gson.toJson(sentence));
    sentenceNew = MergeEntity.mergeDateEntities(gson.toJson(sentenceNew));
    sentenceNew =
        MergeEntity.mergeNamedEntitiesToSingleWord(gson.toJson(sentenceNew));
    sentence.add(SentenceKeys.ENTITIES, sentenceNew.get(SentenceKeys.ENTITIES));
    sentence.add(SentenceKeys.WORDS_KEY,
        sentenceNew.get(SentenceKeys.WORDS_KEY));
    } catch (Exception e) {
      System.err.println("Stanford Pipeline could not process: " + sentence);
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws IOException,
      InterruptedException {
    RunPosTaggerAndNerWithoutTokenizerPipeline engine =
        new RunPosTaggerAndNerWithoutTokenizerPipeline();
    engine.processStream(System.in, System.out, 30, true);
  }
}
