package in.sivareddy.scripts;

import in.sivareddy.graphparser.util.EntityAnnotator;
import in.sivareddy.graphparser.util.EntityAnnotator.PosTagCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NounPhraseAnnotator {

  public static void main(String[] args) throws IOException {
    Preconditions
        .checkArgument(
            args.length == 1,
            "Argument should be specified. See in.sivareddy.graphparser.util.EntityAnnotator for one of the possible pos tag language codes.");

    PosTagCode code = null;
    if (args[0].equals("EN_PTB"))
      code = PosTagCode.EN_PTB;
    else {
      Preconditions
          .checkArgument(
              false,
              "Wrong arguemnt. See in.sivareddy.graphparser.util.EntityAnnotator for one of the possible pos tag language codes.");
    }


    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        EntityAnnotator.getAllNounPhrases(sentence, code);
        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
