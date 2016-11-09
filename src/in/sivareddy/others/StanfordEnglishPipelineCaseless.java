package in.sivareddy.others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.xml.transform.TransformerException;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class StanfordEnglishPipelineCaseless {
  public static void main(String[] args) throws IOException,
      TransformerException {
    // Stanford normal pos tagger is better for WebQuestions?
    Map<String, String> options =
        ImmutableMap
            .of("annotators",
                "tokenize, ssplit, pos, lemma, ner",
                "pos.model",
                "edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger",
                "ner.model",
                "edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz,"
                    + "edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz,"
                    + "edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");

    Gson gson = new Gson();
    StanfordPipeline englishPipeline = new StanfordPipeline(options);
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject out = englishPipeline.processSentence(line);
        System.out.println(gson.toJson(out));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
