package others;

import java.io.IOException;
import java.util.HashMap;

import uk.ac.ed.easyccg.lemmatizer.Lemmatizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

public class AddLemma extends ProcessStreamInterface {

  private static HashMap<String, String> udToPtb = new HashMap<>();
  static {
    udToPtb.put("ADJ", "JJ");
    udToPtb.put("ADV", "RB");
    udToPtb.put("INTJ", "UH");
    udToPtb.put("NOUN", "NN");
    udToPtb.put("PROPN", "NNP");
    udToPtb.put("VERB", "VB");
    udToPtb.put("ADP", "IN");
    udToPtb.put("AUX", "VB");
    udToPtb.put("CONJ", "CC");
    udToPtb.put("DET", "DT");
    udToPtb.put("NUM", "CD");
    udToPtb.put("PART", "RP");
    udToPtb.put("SCONJ", "CC");
    udToPtb.put("PUNCT", "SYM");
    udToPtb.put("SYM", "SYM");
    udToPtb.put("X", "SYM");
  }

  @Override
  public void processSentence(JsonObject sentence) {
    if (sentence.has(SentenceKeys.FOREST)) {
      for (JsonElement sentElm : sentence.get(SentenceKeys.FOREST)
          .getAsJsonArray()) {
        JsonObject sentObj = sentElm.getAsJsonObject();
        processSentence(sentObj);
      }
    } else {
      JsonArray words = sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
      for (JsonElement word : words) {
        JsonObject wordObj = word.getAsJsonObject();
        String wordString = wordObj.get(SentenceKeys.WORD_KEY).getAsString();
        String posString = wordObj.get(SentenceKeys.POS_KEY).getAsString();
        posString = udToPtb.getOrDefault(posString, posString);
        String lemma = wordString;
        try {
          lemma = Lemmatizer.lemmatize(wordString, posString);
        } catch (Exception e) {
          // pass.
        }
        wordObj.addProperty(SentenceKeys.LEMMA_KEY, lemma);
      }
    }
  }

  public static void main(String args[]) throws IOException,
      InterruptedException {
    AddLemma lemmatizer = new AddLemma();
    int nthreads = args.length > 0 ? Integer.parseInt(args[0]) : 20;
    lemmatizer.processStream(System.in, System.out, nthreads, true);
  }
}
