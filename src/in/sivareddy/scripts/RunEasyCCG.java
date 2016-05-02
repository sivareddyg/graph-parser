package in.sivareddy.scripts;

import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import others.EasyCcgCli;
import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RunEasyCCG extends ProcessStreamInterface {
  EasyCcgCli ccgParser;
  EasyCcgCli ccgParserQuestions;
  Gson gson = new Gson();
  JsonParser jsonParser = new JsonParser();

  public RunEasyCCG(String dataFolder, String normalParserOptions,
      int nbestParses, boolean useQuestionsModel)
      throws ArgumentValidationException, IOException {
    String ccgModelDir = Paths.get(dataFolder, "easyccg_model").toString();

    ccgParser =
        new EasyCcgCli(
            String.format("%s %s", ccgModelDir, normalParserOptions),
            nbestParses);
    if (useQuestionsModel) {
      String ccgModelDirQuestions =
          Paths.get(dataFolder, "easyccg_model_questions").toString();
      ccgParserQuestions =
          new EasyCcgCli(ccgModelDirQuestions + " -s -r S[q] S[qem] S[wq]",
              nbestParses);
    }
  }

  public void processSentence(JsonObject jsonSentence) {
    JsonArray words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
    List<String> wordStrings = new ArrayList<>();
    words.forEach(x -> wordStrings.add(String.format("%s|%s|%s", x
        .getAsJsonObject().get(SentenceKeys.WORD_KEY).getAsString(), x
        .getAsJsonObject().get(SentenceKeys.POS_KEY).getAsString(), x
        .getAsJsonObject().get(SentenceKeys.NER_KEY).getAsString())));

    String sentence = Joiner.on(" ").join(wordStrings);

    try {
      List<String> ccgParseStrings =
          ccgParserQuestions != null && sentence.endsWith("?|.|O") ? ccgParserQuestions
              .parse(sentence) : ccgParser.parse(sentence);

      List<Map<String, String>> ccgParses = new ArrayList<>();
      for (String ccgParseString : ccgParseStrings) {
        Map<String, String> ccgParseMap = new HashMap<>();
        ccgParseMap.put(SentenceKeys.CCG_PARSE, ccgParseString);
        ccgParseMap.put(SentenceKeys.SCORE, "1.0");
        ccgParses.add(ccgParseMap);
      }
      jsonSentence.add(SentenceKeys.CCG_PARSES,
          jsonParser.parse(gson.toJson(ccgParses)));
    } catch (Exception e) {
      StringBuilder sb = new StringBuilder();
      words
          .forEach(x -> {
            sb.append(x.getAsJsonObject().get(SentenceKeys.WORD_KEY)
                .getAsString());
            sb.append(" ");
          });
      System.err.println("EasyCCG could not parse: " + sb.toString());
    }
  }

  public static void main(String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {
    int nbest = args.length > 0 ? Integer.parseInt(args[0]) : 10;
    int nthreads = args.length > 1 ? Integer.parseInt(args[1]) : 30;
    RunEasyCCG easyCCG = 
        new RunEasyCCG("lib_data/", " -r S[dcl] S[pss] S[pt] S[b] S[ng] S",
            nbest, false);
    easyCCG.processStream(System.in, System.out, nthreads, true);
  }
}
