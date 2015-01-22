package in.sivareddy.graphparser.ccg.tests;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.CcgParseTree;
import in.sivareddy.graphparser.ccg.CcgParseTree.CcgParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

public class CcgParseToUngroundedSemanticParse {


  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    JsonParser jsonParser = new JsonParser();

    CcgAutoLexicon lexicon =
        new CcgAutoLexicon("./data/candc_markedup.modified",
            "./data/unary_rules.txt", "./data/binary_rules.txt",
            "./data/lexicon_specialCases.txt");
    String[] relationLexicalIdentifiers = {"lemma"};
    String[] argumentLexicalIdenfiers = {"lemma"};
    String[] relationTypingIdentifiers = {};
    boolean ignorePronouns = false;

    CcgParser ccgParser =
        new CcgParser(lexicon, relationLexicalIdentifiers,
            argumentLexicalIdenfiers, relationTypingIdentifiers, ignorePronouns);
    Set<Set<String>> relations;
    Gson gson = new Gson();

    // ccgParseTree =
    // ccgParser.parseFromString("(<L N Adobe Adobe NNP I-ORG I-NP N>)");
    // assertEquals(ccgParseTree.getLeafNodes().size(), 1);

    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject jsonSentence = jsonParser.parse(line).getAsJsonObject();

        if (!jsonSentence.has("synPars")) {
          System.out.println(line);
        } else {
          JsonArray synPars = jsonSentence.get("synPars").getAsJsonArray();

          // First parse
          JsonElement synParseElement = synPars.get(0);
          JsonObject synParseObject = synParseElement.getAsJsonObject();
          String synParse = synParseObject.get("synPar").getAsString();
          try {
            // Take only the first tree
            CcgParseTree ccgParseTree =
                ccgParser.parseFromString(synParse).get(0);
            relations = ccgParseTree.getLexicalisedSemanticPredicates(false);
            jsonSentence.add("relations", gson.toJsonTree(relations));
            System.out.println(gson.toJson(jsonSentence));
          } catch (Exception e) {
            System.out.println(line);
          }
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
