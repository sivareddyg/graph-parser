package others;

import java.io.IOException;

import uk.ac.ed.easyccg.lemmatizer.Lemmatizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

public class AddLemma extends ProcessStreamInterface {

  @Override
  public void processSentence(JsonObject sentence) {
    JsonArray words = sentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    for (JsonElement word : words) {
      JsonObject wordObj = word.getAsJsonObject();
      String wordString = wordObj.get(SentenceKeys.WORD_KEY).getAsString();
      String posString = wordObj.get(SentenceKeys.POS_KEY).getAsString();
      String lemma = wordString;
      try {
        lemma = Lemmatizer.lemmatize(wordString, posString);
      } catch (Exception e) {
        // pass.
      }
      wordObj.addProperty(SentenceKeys.LEMMA_KEY, lemma);
    }
  }

  public static void main(String args[]) throws IOException,
      InterruptedException {
    AddLemma lemmatizer = new AddLemma();
    int nthreads = args.length > 0 ? Integer.parseInt(args[0]) : 20;
    lemmatizer.processStream(System.in, System.out, nthreads, true);
  }
}
