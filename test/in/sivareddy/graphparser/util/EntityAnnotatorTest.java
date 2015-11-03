package in.sivareddy.graphparser.util;

import static org.junit.Assert.*;
import in.sivareddy.graphparser.util.EntityAnnotator.PosTagCode;
import in.sivareddy.util.SentenceKeys;

import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EntityAnnotatorTest {

  private EntityAnnotator annotator = null;
  private JsonParser jsonParser = new JsonParser();

  @Before
  public void setUp() throws Exception {

    // @formatter:off
    String entities =
        "m.a\tU.S. of America.\n" +
        "m.b\tof America\n" +
        "m.b\tAmerica\n" +
        "m.c\tAmerica @$%# %$Airways%##\n" +
        "m.d\tStates\n";
    // @formatter:on
    StringReader entityReader = new StringReader(entities);
    annotator = new EntityAnnotator(entityReader, true, true);
    annotator.setDefaultNPPattern();
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public final void MaximalMatchtest() {
    JsonObject sentence =
        jsonParser
            .parse(
                "{\"sentence\":\"I went to U.S. of America Airways. I went to States America Airways. I went to States America.\",\"words\":[{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"went\",\"lemma\":\"go\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":0,\"dep\":\"ROOT\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"adpmod\"},{\"word\":\"U.S.\",\"lemma\":\"u.s.\",\"pos\":\"NOUN\",\"ner\":\"LOCATION\",\"index\":4,\"head\":3,\"dep\":\"adpobj\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":4,\"dep\":\"adpmod\"},{\"word\":\"America\",\"lemma\":\"america\",\"pos\":\"NOUN\",\"ner\":\"ORGANIZATION\",\"index\":6,\"head\":7,\"dep\":\"compmod\"},{\"word\":\"Airways\",\"lemma\":\"airway\",\"pos\":\"NOUN\",\"ner\":\"ORGANIZATION\",\"index\":7,\"head\":5,\"dep\":\"adpobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true,\"index\":8,\"head\":2,\"dep\":\"p\"},{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":9,\"head\":10,\"dep\":\"nsubj\"},{\"word\":\"went\",\"lemma\":\"go\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":10,\"head\":0,\"dep\":\"ROOT\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":11,\"head\":10,\"dep\":\"adpmod\"},{\"word\":\"States\",\"lemma\":\"state\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":12,\"head\":14,\"dep\":\"compmod\"},{\"word\":\"America\",\"lemma\":\"america\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":13,\"head\":14,\"dep\":\"compmod\"},{\"word\":\"Airways\",\"lemma\":\"airway\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":14,\"head\":11,\"dep\":\"adpobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true,\"index\":15,\"head\":10,\"dep\":\"p\"},{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":16,\"head\":17,\"dep\":\"nsubj\"},{\"word\":\"went\",\"lemma\":\"go\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":17,\"head\":0,\"dep\":\"ROOT\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":18,\"head\":17,\"dep\":\"adpmod\"},{\"word\":\"States\",\"lemma\":\"state\",\"pos\":\"NOUN\",\"ner\":\"LOCATION\",\"index\":19,\"head\":20,\"dep\":\"compmod\"},{\"word\":\"America\",\"lemma\":\"america\",\"pos\":\"NOUN\",\"ner\":\"LOCATION\",\"index\":20,\"head\":18,\"dep\":\"adpobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true,\"index\":21,\"head\":17,\"dep\":\"p\"}]}")
            .getAsJsonObject();

    annotator.maximalMatch(sentence);
    JsonArray matchedEntities =
        sentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();

    assertEquals(5, matchedEntities.size());
    assertEquals("U.S. of America", matchedEntities.get(0).getAsJsonObject()
        .get("phrase").getAsString());
  }

  @Test
  public final void getAllSpanstest() {
    JsonObject sentence =
        jsonParser
            .parse(
                "{\"sentence\":\"I went to U.S. of America Airways. I went to States America Airways. I went to States America.\",\"words\":[{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":1,\"head\":2,\"dep\":\"nsubj\"},{\"word\":\"went\",\"lemma\":\"go\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":2,\"head\":0,\"dep\":\"ROOT\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":3,\"head\":2,\"dep\":\"adpmod\"},{\"word\":\"U.S.\",\"lemma\":\"u.s.\",\"pos\":\"NOUN\",\"ner\":\"LOCATION\",\"index\":4,\"head\":3,\"dep\":\"adpobj\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":5,\"head\":4,\"dep\":\"adpmod\"},{\"word\":\"America\",\"lemma\":\"america\",\"pos\":\"NOUN\",\"ner\":\"ORGANIZATION\",\"index\":6,\"head\":7,\"dep\":\"compmod\"},{\"word\":\"Airways\",\"lemma\":\"airway\",\"pos\":\"NOUN\",\"ner\":\"ORGANIZATION\",\"index\":7,\"head\":5,\"dep\":\"adpobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true,\"index\":8,\"head\":2,\"dep\":\"p\"},{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":9,\"head\":10,\"dep\":\"nsubj\"},{\"word\":\"went\",\"lemma\":\"go\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":10,\"head\":0,\"dep\":\"ROOT\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":11,\"head\":10,\"dep\":\"adpmod\"},{\"word\":\"States\",\"lemma\":\"state\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":12,\"head\":14,\"dep\":\"compmod\"},{\"word\":\"America\",\"lemma\":\"america\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":13,\"head\":14,\"dep\":\"compmod\"},{\"word\":\"Airways\",\"lemma\":\"airway\",\"pos\":\"NOUN\",\"ner\":\"O\",\"index\":14,\"head\":11,\"dep\":\"adpobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true,\"index\":15,\"head\":10,\"dep\":\"p\"},{\"word\":\"I\",\"lemma\":\"I\",\"pos\":\"PRON\",\"ner\":\"O\",\"index\":16,\"head\":17,\"dep\":\"nsubj\"},{\"word\":\"went\",\"lemma\":\"go\",\"pos\":\"VERB\",\"ner\":\"O\",\"index\":17,\"head\":0,\"dep\":\"ROOT\"},{\"word\":\"to\",\"lemma\":\"to\",\"pos\":\"ADP\",\"ner\":\"O\",\"index\":18,\"head\":17,\"dep\":\"adpmod\"},{\"word\":\"States\",\"lemma\":\"state\",\"pos\":\"NOUN\",\"ner\":\"LOCATION\",\"index\":19,\"head\":20,\"dep\":\"compmod\"},{\"word\":\"America\",\"lemma\":\"america\",\"pos\":\"NOUN\",\"ner\":\"LOCATION\",\"index\":20,\"head\":18,\"dep\":\"adpobj\"},{\"word\":\".\",\"lemma\":\".\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true,\"index\":21,\"head\":17,\"dep\":\"p\"}]}")
            .getAsJsonObject();

    annotator.getAllEntitySpans(sentence, true, true);
    assertTrue(sentence.has(SentenceKeys.MATCHED_ENTITIES));
    JsonArray matchedEntities =
        sentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();

    assertEquals(8, matchedEntities.size());
    assertEquals("America",
        matchedEntities.get(1).getAsJsonObject().get("phrase").getAsString());

    annotator.getAllEntitySpans(sentence, false, false);
    assertTrue(sentence.has(SentenceKeys.MATCHED_ENTITIES));
    matchedEntities =
        sentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();
    assertEquals(9, matchedEntities.size());
    System.out.println(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"url\":\"http://www.freebase.com/view/en/dominic_monaghan\",\"targetValue\":\"(list (description \\\"Meriadoc Brandybuck\\\"))\",\"domain\":[\"film\"],\"sentence\":\"who does dominic monaghan play in lord of the rings?\",\"words\":[{\"word\":\"who\",\"lemma\":\"who\",\"pos\":\"WP\",\"ner\":\"O\"},{\"word\":\"does\",\"lemma\":\"do\",\"pos\":\"VBZ\",\"ner\":\"O\"},{\"word\":\"dominic\",\"lemma\":\"dominic\",\"pos\":\"NNP\",\"ner\":\"PERSON\"},{\"word\":\"monaghan\",\"lemma\":\"monaghan\",\"pos\":\"NNP\",\"ner\":\"PERSON\"},{\"word\":\"play\",\"lemma\":\"play\",\"pos\":\"VB\",\"ner\":\"O\"},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"IN\",\"ner\":\"O\"},{\"word\":\"lord\",\"lemma\":\"lord\",\"pos\":\"NNP\",\"ner\":\"O\"},{\"word\":\"of\",\"lemma\":\"of\",\"pos\":\"IN\",\"ner\":\"O\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DT\",\"ner\":\"O\"},{\"word\":\"rings\",\"lemma\":\"ring\",\"pos\":\"NNS\",\"ner\":\"O\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    EntityAnnotator.getAllNounPhrases(sentence, PosTagCode.EN_PTB);
    assertTrue(sentence.has(SentenceKeys.MATCHED_ENTITIES));
    matchedEntities =
        sentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();
    System.out.println(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"url\":\"http://www.freebase.com/view/en/steffy_forrester\",\"targetValue\":\"(list (description \\\"Jacqueline MacInnes Wood\\\"))\",\"sentence\":\"who plays steffy on bold and the beautiful?\",\"words\":[{\"word\":\"who\",\"lemma\":\"who\",\"pos\":\"WP\",\"ner\":\"O\"},{\"word\":\"plays\",\"lemma\":\"play\",\"pos\":\"VBZ\",\"ner\":\"O\"},{\"word\":\"steffy\",\"lemma\":\"steffy\",\"pos\":\"NNP\",\"ner\":\"PERSON\"},{\"word\":\"on\",\"lemma\":\"on\",\"pos\":\"IN\",\"ner\":\"O\"},{\"word\":\"bold\",\"lemma\":\"bold\",\"pos\":\"JJ\",\"ner\":\"O\"},{\"word\":\"and\",\"lemma\":\"and\",\"pos\":\"CC\",\"ner\":\"O\"},{\"word\":\"the\",\"lemma\":\"the\",\"pos\":\"DT\",\"ner\":\"O\"},{\"word\":\"beautiful\",\"lemma\":\"beautiful\",\"pos\":\"JJ\",\"ner\":\"O\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true}]}")
            .getAsJsonObject();
    EntityAnnotator.getAllNounPhrases(sentence, PosTagCode.EN_PTB);
    assertTrue(sentence.has(SentenceKeys.MATCHED_ENTITIES));
    matchedEntities =
        sentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();
    System.out.println(sentence);

    sentence =
        jsonParser
            .parse(
                "{\"url\":\"http://www.freebase.com/view/en/los_angeles\",\"targetValue\":\"(list (description August))\",\"sentence\":\"when does school start in los angeles california 2011?\",\"words\":[{\"word\":\"when\",\"lemma\":\"when\",\"pos\":\"WRB\",\"ner\":\"O\"},{\"word\":\"does\",\"lemma\":\"do\",\"pos\":\"VBZ\",\"ner\":\"O\"},{\"word\":\"school\",\"lemma\":\"school\",\"pos\":\"NNP\",\"ner\":\"O\"},{\"word\":\"start\",\"lemma\":\"start\",\"pos\":\"VB\",\"ner\":\"O\"},{\"word\":\"in\",\"lemma\":\"in\",\"pos\":\"IN\",\"ner\":\"O\"},{\"word\":\"los\",\"lemma\":\"los\",\"pos\":\"NNP\",\"ner\":\"LOCATION\"},{\"word\":\"angeles\",\"lemma\":\"angeles\",\"pos\":\"NNP\",\"ner\":\"LOCATION\"},{\"word\":\"california\",\"lemma\":\"california\",\"pos\":\"NNP\",\"ner\":\"LOCATION\"},{\"word\":\"2011\",\"lemma\":\"2011\",\"pos\":\"CD\",\"ner\":\"NUMBER\"},{\"word\":\"?\",\"lemma\":\"?\",\"pos\":\".\",\"ner\":\"O\",\"sentEnd\":true}],\"matchedEntities\":[{\"phrase\":\"when\",\"start\":0,\"end\":0},{\"phrase\":\"does\",\"start\":1,\"end\":1},{\"phrase\":\"school\",\"start\":2,\"end\":2},{\"phrase\":\"start\",\"start\":3,\"end\":3},{\"phrase\":\"in\",\"start\":4,\"end\":4},{\"phrase\":\"in los\",\"start\":4,\"end\":5},{\"phrase\":\"in los angeles\",\"start\":4,\"end\":6},{\"phrase\":\"los\",\"start\":5,\"end\":5},{\"phrase\":\"los angeles\",\"start\":5,\"end\":6},{\"phrase\":\"los angeles california\",\"start\":5,\"end\":7},{\"phrase\":\"angeles\",\"start\":6,\"end\":6}]}")
            .getAsJsonObject();
    EntityAnnotator.getAllNounPhrases(sentence, PosTagCode.EN_PTB);
    assertTrue(sentence.has(SentenceKeys.MATCHED_ENTITIES));
    matchedEntities =
        sentence.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray();
    System.out.println(sentence);
  }
}
