package others;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

public class RunEasyCCGJsonSentence {
  EasyCcgCli ccgParser;
  EasyCcgCli ccgParserQuestions;
  Gson gson = new Gson();
  JsonParser jsonParser = new JsonParser();

  public RunEasyCCGJsonSentence(String dataFolder, int nbestParses,
      boolean useQuestionsModel) throws ArgumentValidationException,
      IOException {
    String ccgModelDir = Paths.get(dataFolder, "easyccg_model").toString();

    ccgParser = new EasyCcgCli(ccgModelDir, nbestParses);
    if (useQuestionsModel) {
      String ccgModelDirQuestions =
          Paths.get(dataFolder, "easyccg_model_questions").toString();
      ccgParserQuestions =
          new EasyCcgCli(ccgModelDirQuestions + " -s -r S[q] S[qem] S[wq]",
              nbestParses);
    }
  }

  public void processSentence(JsonObject jsonSentence)
      throws ArgumentValidationException, IOException, InterruptedException {
    JsonArray words = jsonSentence.get("words").getAsJsonArray();
    List<String> wordStrings = new ArrayList<>();
    words.forEach(x -> wordStrings.add(String.format("%s|%s|%s", x
        .getAsJsonObject().get("word").getAsString(),
        x.getAsJsonObject().get("pos").getAsString(),
        x.getAsJsonObject().get("ner").getAsString())));

    String sentence = Joiner.on(" ").join(wordStrings);

    List<String> ccgParseStrings =
        ccgParserQuestions != null && sentence.endsWith("?|.|O") ? ccgParserQuestions
            .parse(sentence) : ccgParser.parse(sentence);
    List<Map<String, String>> ccgParses = new ArrayList<>();
    for (String ccgParseString : ccgParseStrings) {
      Map<String, String> ccgParseMap = new HashMap<>();
      ccgParseMap.put("synPar", ccgParseString);
      ccgParseMap.put("score", "1.0");
      ccgParses.add(ccgParseMap);
    }
    jsonSentence.add("synPars", jsonParser.parse(gson.toJson(ccgParses)));
  }

  public static void main(String[] args) throws IOException, ArgumentValidationException, InterruptedException {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();
    RunEasyCCGJsonSentence easyCCG = new RunEasyCCGJsonSentence("lib_data/", 10, true);
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();
        easyCCG.processSentence(jsonSentence);
        System.out.println(gson.toJson(jsonSentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
