package in.sivareddy.scripts;

import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MapNewMidToOldMid {
  public static void convertDisambiguatedEntitiesToOldMids(
      Map<String, Set<String>> newKBMappings,
      Map<String, Set<String>> oldKBMappings, JsonObject sentence) {
    if (sentence.has(SentenceKeys.DISAMBIGUATED_ENTITIES)) {
      JsonArray array =
          sentence.get(SentenceKeys.DISAMBIGUATED_ENTITIES).getAsJsonArray();
      for (JsonElement element : array) {
        JsonObject elementObj = element.getAsJsonObject();
        if (elementObj.has(SentenceKeys.ENTITIES)) {
          for (JsonElement entity : elementObj.get(SentenceKeys.ENTITIES)
              .getAsJsonArray()) {
            JsonObject entityObj = entity.getAsJsonObject();
            if (entityObj.has(SentenceKeys.ENTITY)) {
              String mid = entityObj.get(SentenceKeys.ENTITY).getAsString();
              if (newKBMappings.containsKey(mid)
                  && !oldKBMappings.containsKey(mid)) {
                System.err.println(mid);
                Set<String> oldMids = newKBMappings.get(mid);
                boolean found = false;
                for (String oldMid : oldMids) {
                  if (oldKBMappings.containsKey(oldMid)) {
                    entityObj.addProperty(SentenceKeys.ENTITY, oldMid);
                    entityObj.addProperty("newMid", mid);
                    found = true;
                    break;
                  }
                }
                if (!found && oldMids.size() > 1) {
                  System.err.println("Ambigious!! " + mid + " -> " + oldMids);
                } else if (!found) {
                  // entityObj.addProperty(SentenceKeys.ENTITY,
                  // oldMids.iterator()
                  // .next());
                  // entityObj.addProperty("newMid", mid);
                }
              }
            }
          }
        }
      }
    }
  }

  public static void simpleMapping(Map<String, String> nameToMid,
      Set<String> entities, JsonObject sentence) {
    if (sentence.has(SentenceKeys.DISAMBIGUATED_ENTITIES)) {
      JsonArray array =
          sentence.get(SentenceKeys.DISAMBIGUATED_ENTITIES).getAsJsonArray();
      for (JsonElement element : array) {
        JsonObject elementObj = element.getAsJsonObject();
        if (elementObj.has(SentenceKeys.ENTITIES)) {
          for (JsonElement entity : elementObj.get(SentenceKeys.ENTITIES)
              .getAsJsonArray()) {
            JsonObject entityObj = entity.getAsJsonObject();
            if (entityObj.has(SentenceKeys.ENTITY)) {
              String mid = entityObj.get(SentenceKeys.ENTITY).getAsString();
              String[] parts =
                  entityObj.get(SentenceKeys.ENTITY_ID).getAsString()
                      .split("/");
              String name = parts[parts.length - 1];
              if (!entities.contains(mid)) {
                if (nameToMid.containsKey(name)
                    && entities.contains(nameToMid.get(name))) {
                  entityObj.addProperty(SentenceKeys.ENTITY,
                      nameToMid.get(name));
                  entityObj.addProperty("newMid", mid);
                } else {
                  System.err.println("Unknown mid: " + mid);
                }
              }
            }
          }
        }
      }
    }
  }

  public static void loadMappings(String mappingsFile,
      Map<String, Set<String>> mappings) throws FileNotFoundException,
      IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(new GZIPInputStream(
            new FileInputStream(mappingsFile))));
    try {
      String line = br.readLine();
      while (line != null) {
        String[] parts = line.split("\t");
        // System.err.print(parts[0] + ' ');
        // System.err.println(parts[1]);
        if (!mappings.containsKey(parts[1])) {
          mappings.put(parts[1], new HashSet<>());
        }
        mappings.get(parts[1]).add(parts[0]);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public static void loadEntities(String oldEntitiesFile, Set<String> entities)
      throws FileNotFoundException, IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(new GZIPInputStream(
            new FileInputStream(oldEntitiesFile))));
    try {
      String line = br.readLine();
      while (line != null) {
        entities.add(line);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public static void loadNameToMid(String idToMidMappingFile,
      Map<String, String> nameToMid) throws FileNotFoundException, IOException {
    BufferedReader br =
        new BufferedReader(new InputStreamReader(new GZIPInputStream(
            new FileInputStream(idToMidMappingFile))));
    try {
      String line = br.readLine();
      while (line != null) {
        String[] parts = line.split("\t");
        nameToMid.put(parts[1], parts[0]);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public static void main(String[] args) throws IOException {
    // HashMap<String, Set<String>> newKBMappings = new HashMap<>();
    // HashMap<String, Set<String>> oldKBMappings = new HashMap<>();
    HashMap<String, String> nameToMid = new HashMap<>();

    Set<String> entities = new HashSet<>();
    // loadMappings(args[1], oldKBMappings);
    // loadMappings(args[0], newKBMappings);
    loadEntities(args[0], entities);
    loadNameToMid(args[1], nameToMid);

    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        if (line.startsWith("#") || line.trim().equals("")) {
          line = br.readLine();
          continue;
        }
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        simpleMapping(nameToMid, entities, sentence);
        // convertDisambiguatedEntitiesToOldMids(newKBMappings, oldKBMappings,
        // sentence);

        System.out.println(gson.toJson(sentence));
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
