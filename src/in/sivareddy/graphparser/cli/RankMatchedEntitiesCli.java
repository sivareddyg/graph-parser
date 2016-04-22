package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.util.RankMatchedEntities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class RankMatchedEntitiesCli extends AbstractCli {

  private OptionSpec<String> languageCode;
  private OptionSpec<String> apiKey;
  private OptionSpec<Boolean> useKG;

  @Override
  public void initializeOptions(OptionParser parser) {
    languageCode =
        parser.accepts("langCode", "Knowledge Graph/Freebase Language code.")
            .withRequiredArg().ofType(String.class).defaultsTo("en");

    apiKey =
        parser.accepts("apiKey", "Knowledge Graph/Freebase API Key")
            .withRequiredArg().ofType(String.class);

    useKG =
        parser
            .accepts("useKG",
                "Use Knowledge Graph. If this is set false, Freebase API is used.")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(true);
  }

  @Override
  public void run(OptionSet options) {
    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    String apiKeyValue = options.valueOf(apiKey);
    RankMatchedEntities ranker = new RankMatchedEntities(apiKeyValue);

    boolean useKGValue = options.valueOf(useKG);
    String languageCodeValue = options.valueOf(languageCode);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();

        if (useKGValue) {
          ranker.rankSpansUsingKnowledgeGraphAPI(sentence, languageCodeValue,
              false);
        } else {
          ranker.rankSpansUsingFreebaseAPI(sentence, languageCodeValue, false);
        }

        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) throws IOException {
    new RankMatchedEntitiesCli().run(args);
  }
}
