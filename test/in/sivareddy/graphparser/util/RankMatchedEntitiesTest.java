package in.sivareddy.graphparser.util;

import in.sivareddy.graphparser.util.EntityAnnotator.PosTagCode;

import java.io.IOException;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RankMatchedEntitiesTest {

  RankMatchedEntities ranker = new RankMatchedEntities();
  JsonParser jsonParser = new JsonParser();

  @Test
  public final void testRankSpansUsingKnowledgeGraphAPI() throws IOException {
    System.out.println(ranker.queryFreebaseAPI("taylor swift", "en"));
    System.out.println(ranker.queryKnowledgeGraphAPI("taylor swift", "en"));

    JsonObject sentence =
        jsonParser
            .parse(
                "{\"sentence\":\"who does dominic monaghan play in lord of the rings?\",\"words\":[{\"word\":\"who\",\"lemma\":\"who\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"does\",\"lemma\":\"do\",\"pos\":\"VERB\",\"ner\":\"O\",\"dep\":\"root\",\"head\":0,\"index\":2},{\"word\":\"dominic\",\"lemma\":\"dominic\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":3,\"head\":4,\"dep\":\"compound\"},{\"word\":\"monaghan\",\"lemma\":\"monaghan\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":4,\"head\":2,\"dep\":\"dobj\"},{\"word\":\"play\",\"lemma\":\"play\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":5,\"head\":2,\"dep\":\"xcomp\"},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":6,\"head\":7,\"dep\":\"case\"},{\"word\":\"lord\",\"lemma\":\"lord\",\"pos\":\"PROPN\",\"ner\":\"O\",\"index\":7,\"head\":5,\"dep\":\"nmod\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":8,\"head\":10,\"dep\":\"case\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DET\",\"ner\":\"O\",\"index\":9,\"head\":10,\"dep\":\"det\"},{\"word\":\"rings\",\"lemma\":\"ring\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":10,\"head\":7,\"dep\":\"nmod\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\"PUNCT\",\"ner\":\"O\",\"index\":11,\"head\":2,\"dep\":\"punct\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    EntityAnnotator.getAllNounPhrases(sentence, PosTagCode.EN_UD);
    ranker.rankSpansUsingKnowledgeGraphAPI(sentence, "en", false);
    System.out.println(sentence);
  }
}
