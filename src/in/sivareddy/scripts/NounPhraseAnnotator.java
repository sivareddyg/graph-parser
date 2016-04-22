package in.sivareddy.scripts;

import in.sivareddy.graphparser.util.EntityAnnotator;
import in.sivareddy.graphparser.util.EntityAnnotator.PosTagCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
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
    Map<String, PosTagCode> stringToPosTagCode =
        ImmutableMap.of("EN_PTB", PosTagCode.EN_PTB, "en_ud", PosTagCode.EN_UD,
            "es_ud", PosTagCode.ES_UD, "de_ud", PosTagCode.DE_UD);

    Preconditions
        .checkArgument(
            stringToPosTagCode.containsKey(args[0]),
            "Wrong arguemnt. See in.sivareddy.graphparser.util.EntityAnnotator for one of the possible pos tag language codes.");

    code = stringToPosTagCode.get(args[0]);

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
