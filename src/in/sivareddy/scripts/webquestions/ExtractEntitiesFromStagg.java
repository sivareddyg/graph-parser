package in.sivareddy.scripts.webquestions;

import in.sivareddy.graphparser.util.EntityAnnotator;
import in.sivareddy.others.StanfordPipeline;
import in.sivareddy.scripts.MapNewMidToOldMid;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExtractEntitiesFromStagg extends ProcessStreamInterface {
  JsonParser jsonParser = new JsonParser();
  URLDecoder urlDecoder = new URLDecoder();
  Map<String, String> options =
      ImmutableMap
          .of("annotators",
              "tokenize, ssplit, pos, lemma, ner",
              "pos.model",
              "edu/stanford/nlp/models/pos-tagger/english-caseless-left3words-distsim.tagger",
              "ner.model",
              "edu/stanford/nlp/models/ner/english.all.3class.caseless.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.conll.4class.caseless.distsim.crf.ser.gz");

  Gson gson = new Gson();
  StanfordPipeline englishPipeline = new StanfordPipeline(options);

  Map<Integer, List<String>> sentIdToEntityMapping = new HashMap<>();
  public HashMap<String, String> nameToMid;

  public ExtractEntitiesFromStagg(String entityFileName,
      HashMap<String, String> nameToMid) throws FileNotFoundException,
      IOException {
    readEntities(new FileInputStream(entityFileName));
    this.nameToMid = nameToMid;
  }

  void readEntities(InputStream stream) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    // WebQTrn-5 lord of the rings 38 17 /m/017jd9
    // The_Lord_of_the_Rings%3a_The_Return_of_the_King 17.6310243670667
    try {
      String line = br.readLine();
      while (line != null) {
        int sentId = Integer.parseInt(line.split("\t")[0].split("-")[1]);
        sentIdToEntityMapping.putIfAbsent(sentId, new ArrayList<>());
        sentIdToEntityMapping.get(sentId).add(line);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public void processSentence(JsonObject jsonSentence) {
    String sentString =
        jsonSentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
    englishPipeline.processSentence(jsonSentence);
    JsonObject sentenceCopy = jsonParser.parse(gson.toJson(jsonSentence)).getAsJsonObject(); 
    Integer sentId = jsonSentence.get(SentenceKeys.INDEX_KEY).getAsInt() - 1;

    if (!sentIdToEntityMapping.containsKey(sentId)) {
      return;
    }

    /*-
     * matchedEntities":[{"start":2,"end":2,"phrase":"jamaican","pattern":"JJ","rankedEntities":[{"id":"/en/jamaica","name":"Jamaica","notable":{"name":"Country","id":"/location/country"},"lang":"en","score":90.495514,"entity":"m.03_r3"},{"id":"/en/jamaican_creole","name":"Jamaican Creole English Language","notable":{"name":"Human Language","id":"/language/human_language"},"lang":"en","score":65.496033,"entity":"m.04ygk0"},{"id":"/en/jamaica_national_football_team","name":"Jamaica national football team","notable":{"name":"Soccer team","id":"/soccer/football_team"},"lang":"en","score":46.985256,"entity":"m.03zbg0"},{"id":"/en/dancehall","name":"Dancehall","notable":{"name":"Musical genre","id":"/music/genre"},"lang":"en","score":37.949532,"entity":"m.0190yn"},{"id":"/en/jamaica_cricket_team","name":"Jamaica national cricket team","notable":{"name":"Cricket Team","id":"/cricket/cricket_team"},"lang":"en","score":34.876347,"entity":"m.025sn19"},{"name":"Music of Jamaica","lang":"en","score":32.529236,"entity":"m.01199xfl"},{"id":"/en/jamaican_english","name":"Jamaican English","notable":{"name":"Human Language","id":"/language/human_language"},"lang":"en","score":29.530621,"entity":"m.01428y"},{"id":"/en/jamaican_american","name":"Jamaican American","notable":{"name":"Ethnicity","id":"/people/ethnicity"},"lang":"en","score":29.472410,"entity":"m.0283js_"},{"id":"/en/jamaican_dollar","name":"Jamaican dollar","notable":{"name":"Currency","id":"/finance/currency"},"lang":"en","score":28.438152,"entity":"m.04xc2m"},{"name":"Jamaicans","lang":"en","score":28.382172,"entity":"m.0j3c70b"},{"id":"/en/reggae","name":"Reggae","notable":{"name":"Musical genre","id":"/music/genre"},"lang":"en","score":27.629330,"entity":"m.06cqb"},{"id":"/en/jamaicans_of_african_ancestry","name":"Afro-Jamaican","notable":{"name":"Ethnicity","id":"/people/ethnicity"},"lang":"en","score":27.558334,"entity":"m.047ch9x"},{"id":"/en/jamaican","name":"Jamaican food","notable":{"name":"Cuisine","id":"/dining/cuisine"},"lang":"en","score":26.409084,"entity":"m.02sp4_"},{"id":"/en/disc_jockey","name":"Disc jockey","notable":{"name":"Profession","id":"/people/profession"},"lang":"en","score":25.273626,"entity":"m.02dsz"},{"id":"/en/jamaican_jerk_spice","name":"Jamaican jerk spice","notable":{"name":"Food","id":"/food/food"},"lang":"en","score":24.309946,"entity":"m.03_j5r"},{"id":"/en/jamaica_national_bobsled_team","name":"Jamaica National Bobsled Team","notable":{"name":"Sports Team","id":"/sports/sports_team"},"lang":"en","score":23.917952,"entity":"m.027fwz3"},{"id":"/en/jamaican_british","name":"British Jamaican","notable":{"name":"Ethnicity","id":"/people/ethnicity"},"lang":"en","score":23.533815,"entity":"m.02r3wfk"},{"id":"/en/ska","name":"Ska","notable":{"name":"Musical genre","id":"/music/genre"},"lang":"en","score":22.784977,"entity":"m.06rqw"},{"id":"/en/jamaican_posse","name":"Jamaican posse","notable":{"name":"Organisation","id":"/organization/organization"},"lang":"en","score":22.100435,"entity":"m.08dryc"},{"id":"/en/jamaican_defence_force","name":"Jamaica Defence Force","notable":{"name":"Organisation","id":"/organization/organization"},"lang":"en","score":22.096529,"entity":"m.04l559"}]}
     */
    Map<Pair<Integer, Integer>, JsonObject> spanToEntities = new HashMap<>();
    for (String staggEntityLine : sentIdToEntityMapping.get(sentId)) {
      // WebQTrn-5 lord of the rings 38 17 /m/017jd9
      // The_Lord_of_the_Rings%3a_The_Return_of_the_King 17.6310243670667
      String[] parts = staggEntityLine.split("\t");
      String entityString =
          sentString.substring(Integer.parseInt(parts[2]),
              Integer.parseInt(parts[2]) + Integer.parseInt(parts[3]));
      JsonObject entitySentence = new JsonObject();
      entitySentence.addProperty(SentenceKeys.SENTENCE_KEY, entityString);
      englishPipeline.processSentence(entitySentence);
      String entity = parts[4];
      if (entity.startsWith("en.")) {
        String entityKey = parts[4].split("\\.", 2)[1];
        entity = nameToMid.getOrDefault(entityKey, parts[4]);
      }

      List<String> entityWords = new ArrayList<>();
      entitySentence
          .get(SentenceKeys.WORDS_KEY)
          .getAsJsonArray()
          .forEach(
              x -> entityWords.add(x.getAsJsonObject()
                  .get(SentenceKeys.WORD_KEY).getAsString()));
      
      InputStream entityStream =
          IOUtils.toInputStream(String.format("%s\t%s", entity, Joiner.on(" ")
              .join(entityWords)));
      EntityAnnotator entityAnnotator = null;
      try {
        entityAnnotator = new EntityAnnotator(new InputStreamReader(entityStream), false, false);
      } catch (IOException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      entityAnnotator.maximalMatch(sentenceCopy);
      
      if (!sentenceCopy.has(SentenceKeys.MATCHED_ENTITIES))
        continue;
      
      JsonObject entityObj =
          sentenceCopy.get(SentenceKeys.MATCHED_ENTITIES).getAsJsonArray().get(0)
              .getAsJsonObject();
      sentenceCopy.remove(SentenceKeys.MATCHED_ENTITIES);
      
      int start = entityObj.get(SentenceKeys.START).getAsInt();
      int end = entityObj.get(SentenceKeys.END).getAsInt();
      

      Pair<Integer, Integer> key = Pair.of(start, end);
      if (!spanToEntities.containsKey(key)) {
        JsonObject spanEntities = new JsonObject();
        spanToEntities.put(key, spanEntities);

        spanEntities.addProperty(SentenceKeys.START, start);
        spanEntities.addProperty(SentenceKeys.END, end);
        JsonArray words =
            jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
        ArrayList<String> phraseWords = new ArrayList<>();
        for (int i = start; i < end + 1; i++) {
          JsonObject wordObj = words.get(i).getAsJsonObject();
          String word = wordObj.get(SentenceKeys.WORD_KEY).getAsString();
          phraseWords.add(word);
        }
        spanEntities.addProperty(SentenceKeys.PHRASE,
            Joiner.on(" ").join(phraseWords));
        spanEntities.add(SentenceKeys.RANKED_ENTITIES, new JsonArray());
        spanEntities.addProperty("pattern", "NNP");
      }

      JsonArray rankedEntities =
          spanToEntities.get(key).get(SentenceKeys.RANKED_ENTITIES)
              .getAsJsonArray();
      JsonObject rankedEntity = new JsonObject();
      if (parts[4].startsWith("en.")) {
        rankedEntity.addProperty(SentenceKeys.ENTITY_ID,
            String.format("/%s", parts[4].replaceFirst("\\.", "/")));
      }
      rankedEntity.addProperty(SentenceKeys.ENTITY, entity);

      try {
        rankedEntity.addProperty("name", URLDecoder.decode(parts[5], "UTF-8")
            .replace("_", " "));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      rankedEntity
          .addProperty(SentenceKeys.SCORE, Double.parseDouble(parts[6]));
      rankedEntities.add(rankedEntity);
    }
    ArrayList<Entry<Pair<Integer, Integer>, JsonObject>> entries =
        new ArrayList<>(spanToEntities.entrySet());
    entries.sort(Comparator.comparing(x -> x.getKey()));
    JsonArray matchesEntities = new JsonArray();
    entries.forEach(x -> matchesEntities.add(x.getValue()));
    jsonSentence.add(SentenceKeys.MATCHED_ENTITIES, matchesEntities);
  }

  public static void main(String[] args) throws FileNotFoundException,
      IOException, InterruptedException {

    HashMap<String, String> nameToMid = new HashMap<>();
    MapNewMidToOldMid.loadNameToMid(args[0], nameToMid);
    nameToMid.put("australian_open_tennis_tournament", "m.0p58j");
    nameToMid.put("greenwich_mean_time_zone", "m.03bdv");
    nameToMid.put("the_ncaa_mens_division_i_basketball_championship_game",
        "m.02jp2w");
    nameToMid.put("family_guy_-_season_13", "m.0y4_pjq");
    nameToMid.put("wwe", "m.0gy1_");
    nameToMid.put("huddie_william_ledbetter", "m.01wxlnl");
    nameToMid.put("the_us_open_tennis", "m.0l6c9");
    nameToMid.put("the_us_open_golf", "m.01gm0t");
    nameToMid.put("les_petits_chanteurs_dasni�res", "m.01vq0ss");
    nameToMid.put("seeds_bones", "m.0z51bhs");
    nameToMid.put("afc-nfc_pro_bowl", "m.01p38d");
    nameToMid.put("pnk", "m.01vrt_c");
    nameToMid.put("dan_james_white", "m.02dzj8");
    nameToMid.put("christian_unitarianism", "m.07x21");
    nameToMid.put("bank_of_america_corporation", "m.03_3d");
    nameToMid.put("edi_rock", "m.0zqlm10");

    ExtractEntitiesFromStagg engine =
        new ExtractEntitiesFromStagg(args[1], nameToMid);
    engine.processStream(System.in, System.out, 30, true);
  }
}
