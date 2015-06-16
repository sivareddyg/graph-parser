package in.sivareddy.graphparser.parsing;

import in.sivareddy.graphparser.ccg.LexicalItem;

import java.util.List;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BagOfWordsSemanticParseTest {

  @Test
  public final void testGetBagOfWordsUngroundedSemanticParse() {
    JsonParser parser = new JsonParser();
    String line =
        "{\"sentence\": \"What year were the Cincinnati_Reds founded ?\", \"url\": \"http://www.freebase.com/view/en/cincinnati_reds\", \"dependency_lambda\": [[\"found.arg_2(5:e , 4:m.01ypc)\", \"QUESTION(1:x)\", \"year(1:s , 1:x)\", \"found.tmod(5:e , 1:x)\", \"UNIQUE(4:m.01ypc)\"]], \"entities\": [{\"index\": 4, \"name\": \"Cincinnati_Reds\", \"entity\": \"m.01ypc\"}], \"words\": [{\"category\": \"DET\", \"head\": 1, \"end\": 3, \"start\": 0, \"break_level\": 3, \"pos\": \"WDT\", \"label\": \"det\", \"lemma\": \"what\", \"word\": \"What\"}, {\"category\": \"NOUN\", \"head\": 6, \"end\": 8, \"start\": 5, \"break_level\": 1, \"pos\": \"NN\", \"label\": \"tmod\", \"lemma\": \"year\", \"word\": \"year\"}, {\"category\": \"VERB\", \"head\": 6, \"end\": 13, \"lemma\": \"be\", \"break_level\": 1, \"pos\": \"VBD\", \"label\": \"auxpass\", \"start\": 10, \"word\": \"were\"}, {\"category\": \"DET\", \"head\": 5, \"end\": 17, \"start\": 15, \"break_level\": 1, \"pos\": \"DT\", \"label\": \"det\", \"lemma\": \"the\", \"word\": \"the\"}, {\"category\": \"NOUN\", \"head\": 6, \"end\": 33, \"start\": 30, \"break_level\": 1, \"pos\": \"NNPS\", \"label\": \"nsubjpass\", \"lemma\": \"Cincinnati_Reds\", \"word\": \"Cincinnati_Reds\"}, {\"category\": \"VERB\", \"end\": 41, \"lemma\": \"found\", \"break_level\": 1, \"pos\": \"VBN\", \"label\": \"ROOT\", \"start\": 35, \"word\": \"founded\"}, {\"category\": \".\", \"head\": 6, \"end\": 43, \"start\": 43, \"break_level\": 1, \"pos\": \".\", \"label\": \"p\", \"lemma\": \"?\", \"word\": \"?\"}], \"targetValue\": \"(list (description 1881))\"}";

    JsonObject jsonSentence = parser.parse(line).getAsJsonObject();
    List<LexicalItem> leaves =
        GroundedGraphs.buildLexicalItemsFromWords(jsonSentence);
    System.out.println(GroundedGraphs.getBagOfWordsUngroundedSemanticParse(
        jsonSentence, leaves));
  }
}
