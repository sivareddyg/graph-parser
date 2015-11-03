package in.sivareddy.graphparser.util.knowledgebase;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class KnowledgeBaseCached implements KnowledgeBase {

  private Map<Integer, Set<Integer>> entity2Types;
  private Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> entityPair2Relations;
  private Map<Integer, Set<Pair<Integer, Integer>>> entity2Relations;
  private BiMap<String, Integer> typeIDs;
  private BiMap<String, Integer> relationIDs;
  private Map<String, Integer> entityIDs;
  private int typeCount;
  private int relationCount;
  private int entityCount;
  private Set<Pair<Integer, Integer>> relationsThatAreTypes;

  public KnowledgeBaseCached(String kbCompressedFile, String relationTypesFile)
      throws IOException {
    entity2Types = Maps.newHashMap();
    entityPair2Relations = Maps.newHashMap();
    entity2Relations = Maps.newHashMap();
    relationsThatAreTypes = Sets.newHashSet();

    typeIDs = HashBiMap.create();
    relationIDs = HashBiMap.create();

    entityIDs = Maps.newHashMap();

    entityIDs.put("type.int", 0);
    entityIDs.put("type.float", 1);
    entityIDs.put("type.datetime", 2);

    typeIDs.put("type.int", 0);
    typeIDs.put("type.float", 1);
    typeIDs.put("type.datetime", 2);

    Set<Integer> integerSet = Sets.newHashSet(0);
    entity2Types.put(0, integerSet);

    Set<Integer> floatSet = Sets.newHashSet(1);
    entity2Types.put(1, floatSet);

    Set<Integer> dateSet = Sets.newHashSet(2);
    entity2Types.put(2, dateSet);

    entityCount = 3;
    typeCount = 3;
    relationCount = 0;

    if (relationTypesFile != null)
      loadRelationsThatAreTypes(relationTypesFile);

    if (kbCompressedFile != null)
      loadKB(kbCompressedFile);
  }

  private void loadRelationsThatAreTypes(String relationTypesFile)
      throws IOException {
    JsonParser parser = new JsonParser();
    BufferedReader br = new BufferedReader(new FileReader(relationTypesFile));
    try {
      String line = br.readLine();
      while (line != null) {
        String[] parts = line.split("\t")[0].split(" # ");
        String argNumber = parts[1];
        JsonElement jelement = parser.parse(parts[0]);
        JsonArray relation = jelement.getAsJsonArray();

        JsonArray relationEdges = relation.getAsJsonArray();
        String edge1;
        String edge2;
        if (relationEdges.size() == 1) {
          String relationName = relationEdges.get(0).getAsString();
          edge1 = relationName + ".1";
          edge2 = relationName + ".2";
        } else {
          edge1 = relationEdges.get(0).getAsString();
          edge2 = relationEdges.get(1).getAsString();
        }

        if (!relationIDs.containsKey(edge1)) {
          relationIDs.put(edge1, relationCount);
          relationCount++;
        }
        if (!relationIDs.containsKey(edge2)) {
          relationIDs.put(edge2, relationCount);
          relationCount++;
        }

        if (argNumber.equals("right_arg")) {
          Pair<Integer, Integer> edge =
              Pair.of(relationIDs.get(edge1), relationIDs.get(edge2));
          relationsThatAreTypes.add(edge);
        } else if (argNumber.equals("left_arg")) {
          Pair<Integer, Integer> edge_inverse =
              Pair.of(relationIDs.get(edge2), relationIDs.get(edge1));
          relationsThatAreTypes.add(edge_inverse);
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }

  }

  private int getOrCreateEntityID(String entity) {
    if (!entityIDs.containsKey(entity)) {
      entityIDs.put(entity, entityCount);
      entityCount++;
    }
    return entityIDs.get(entity);
  }

  private void loadKB(String kbCompressedFile) throws IOException {
    InputStream fileStream = new FileInputStream(kbCompressedFile);
    InputStream gzipStream = new GZIPInputStream(fileStream);
    Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
    BufferedReader br = new BufferedReader(decoder);

    // Entity Type Format:
    // "m.0gbltb2"
    // ["film.film_casting_director","film.editor","film.writer","film.director"]

    // Relation Type Format:
    // ["m.04q6psm","type.datetime"]
    // [["film.film_regional_release_date.film","film.film_regional_release_date.release_date"]]
    // ["m.0n64zwd","m.0n64zwm"]
    // [["film.film.directed_by"],["film.film.produced_by"],["film.film.written_by"],
    // ["film.film_crew_gig.film","film.film_crew_gig.crewmember"],["film.film.cinematography"]]

    JsonParser parser = new JsonParser();

    try {
      // String line =
      // "[\"m.0n64zwd\",\"m.0n64zwm\"]	[[\"film.film.directed_by\"],[\"film.film.produced_by\"],[\"film.film.written_by\"], [\"film.film_crew_gig.film\",\"film.film_crew_gig.crewmember\"],[\"film.film.cinematography\"]]";
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (line.equals("")) {
          line = br.readLine();
          continue;
        }
        if (line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }
        String[] parts = line.split("\t");
        if (parts[0].charAt(0) == '[') {
          // Fact describing relation between entities
          JsonElement jelement = parser.parse(parts[0]);
          JsonArray entities = jelement.getAsJsonArray();
          String entity1 = entities.get(0).getAsString();
          String entity2 = entities.get(1).getAsString();

          int entity1Id = getOrCreateEntityID(entity1);
          int entity2Id = getOrCreateEntityID(entity2);

          Set<Pair<Integer, Integer>> relationsSet = Sets.newHashSet();
          Set<Pair<Integer, Integer>> inverseRelationsSet = Sets.newHashSet();
          jelement = parser.parse(parts[1]);
          JsonArray relations = jelement.getAsJsonArray();
          for (JsonElement relation : relations) {
            JsonArray relationEdges = relation.getAsJsonArray();
            String edge1;
            String edge2;
            if (relationEdges.size() == 1) {
              String relationName = relationEdges.get(0).getAsString();
              edge1 = relationName + ".1";
              edge2 = relationName + ".2";
            } else {
              edge1 = relationEdges.get(0).getAsString();
              edge2 = relationEdges.get(1).getAsString();
            }

            // TODO: Remove in future BAD BAD: relation
            // film_regional_release_date upsets film results.
            // Ambigious relation between initial_release_date.
            if (edge1.contains("film_regional_release_date")
                || edge2.contains("film_regional_release_date")) {
              continue;
            }

            if (!relationIDs.containsKey(edge1)) {
              relationIDs.put(edge1, relationCount);
              relationCount++;
            }
            if (!relationIDs.containsKey(edge2)) {
              relationIDs.put(edge2, relationCount);
              relationCount++;
            }
            Pair<Integer, Integer> edge =
                Pair.of(relationIDs.get(edge1), relationIDs.get(edge2));
            Pair<Integer, Integer> edge_inverse =
                Pair.of(relationIDs.get(edge2), relationIDs.get(edge1));
            relationsSet.add(edge);
            inverseRelationsSet.add(edge_inverse);

            // Adding relations that are potential types
            if (relationsThatAreTypes.contains(edge)) {
              String entityTypeString =
                  String.format("%s#%s#%s", edge1, edge2, entity2);
              if (!typeIDs.containsKey(entityTypeString)) {
                typeIDs.put(entityTypeString, typeCount);
                typeCount++;
              }
              if (!entity2Types.containsKey(entity1Id)) {
                entity2Types.put(entity1Id, new HashSet<Integer>());
              }
              entity2Types.get(entity1Id).add(typeIDs.get(entityTypeString));
            }

            if (relationsThatAreTypes.contains(edge_inverse)) {
              String entityTypeString =
                  String.format("%s#%s#%s", edge2, edge1, entity1);
              if (!typeIDs.containsKey(entityTypeString)) {
                typeIDs.put(entityTypeString, typeCount);
                typeCount++;
              }
              if (!entity2Types.containsKey(entity2Id)) {
                entity2Types.put(entity2Id, new HashSet<Integer>());
              }
              entity2Types.get(entity2Id).add(typeIDs.get(entityTypeString));
            }
          }

          if (entity1.compareTo(entity2) < 0) {
            Pair<Integer, Integer> entityPair = Pair.of(entity1Id, entity2Id);
            if (!entityPair2Relations.containsKey(entityPair)) {
              entityPair2Relations.put(entityPair, relationsSet);
            } else {
              entityPair2Relations.get(entityPair).addAll(relationsSet);
            }
          } else {
            Pair<Integer, Integer> entityPair = Pair.of(entity2Id, entity1Id);
            if (!entityPair2Relations.containsKey(entityPair)) {
              entityPair2Relations.put(entityPair, inverseRelationsSet);
            } else {
              entityPair2Relations.get(entityPair).addAll(inverseRelationsSet);
            }
          }

          if (!entity2Relations.containsKey(entity1Id)) {
            entity2Relations.put(entity1Id,
                new HashSet<Pair<Integer, Integer>>());
          }
          entity2Relations.get(entity1Id).addAll(relationsSet);

          if (!entity2Relations.containsKey(entity2Id)) {
            entity2Relations.put(entity2Id,
                new HashSet<Pair<Integer, Integer>>());
          }
          entity2Relations.get(entity2Id).addAll(inverseRelationsSet);

        } else {
          // Fact describing an entity type
          String entity = parser.parse(parts[0]).getAsString();
          int entityId = getOrCreateEntityID(entity);

          JsonElement jelement = parser.parse(parts[1]);
          JsonArray entityTypes = jelement.getAsJsonArray();
          if (!entity2Types.containsKey(entityId)) {
            entity2Types.put(entityId, new HashSet<Integer>());
          }
          for (JsonElement entityType : entityTypes) {
            String entityTypeString = entityType.getAsString();
            if (!typeIDs.containsKey(entityTypeString)) {
              typeIDs.put(entityTypeString, typeCount);
              typeCount++;
            }
            entity2Types.get(entityId).add(typeIDs.get(entityTypeString));
          }
        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
    System.err.println("Knowledge Base loaded");
  }

  public Set<Relation> getRelations(String entity1, String entity2) {
    if (!entityIDs.containsKey(entity1) || !entityIDs.containsKey(entity2)) {
      return null;
    }
    int entity1Id = entityIDs.get(entity1);
    int entity2Id = entityIDs.get(entity2);
    Pair<Integer, Integer> entityIdPair = Pair.of(entity1Id, entity2Id);

    Set<Relation> rels = Sets.newHashSet();
    if (entityPair2Relations.containsKey(entityIdPair)) {
      for (Pair<Integer, Integer> entityPairIds : entityPair2Relations
          .get(entityIdPair)) {
        String relationEdge1 =
            relationIDs.inverse().get(entityPairIds.getLeft());
        String relationEdge2 =
            relationIDs.inverse().get(entityPairIds.getRight());
        rels.add(Relation.of(relationEdge1, relationEdge2));
      }
    }

    // if inverse entity pair exists
    entityIdPair = Pair.of(entity2Id, entity1Id);
    if (entityPair2Relations.containsKey(entityIdPair)) {
      for (Pair<Integer, Integer> entityPairIds : entityPair2Relations
          .get(entityIdPair)) {
        String relationEdge2 =
            relationIDs.inverse().get(entityPairIds.getLeft());
        String relationEdge1 =
            relationIDs.inverse().get(entityPairIds.getRight());
        rels.add(Relation.of(relationEdge1, relationEdge2));
      }
    }

    return rels;
  }

  public Set<Relation> getRelations(String entity1) {
    if (!entityIDs.containsKey(entity1)) {
      return null;
    }
    int entity1Id = entityIDs.get(entity1);

    Set<Relation> rels = Sets.newHashSet();
    if (entity2Relations.containsKey(entity1Id)) {
      for (Pair<Integer, Integer> entityPairIds : entity2Relations
          .get(entity1Id)) {
        String relationEdge1 =
            relationIDs.inverse().get(entityPairIds.getLeft());
        String relationEdge2 =
            relationIDs.inverse().get(entityPairIds.getRight());
        rels.add(Relation.of(relationEdge1, relationEdge2));
      }
    }
    return rels;
  }

  public boolean hasRelation(String entity1, String entity2) {
    if (!entityIDs.containsKey(entity1) || !entityIDs.containsKey(entity2)) {
      return false;
    }
    int entity1Id = entityIDs.get(entity1);
    int entity2Id = entityIDs.get(entity2);
    Pair<Integer, Integer> entityIdPair = Pair.of(entity1Id, entity2Id);
    if (entityPair2Relations.containsKey(entityIdPair)) {
      return true;
    }
    entityIdPair = Pair.of(entity2Id, entity1Id);
    return entityPair2Relations.containsKey(entityIdPair);
  }

  public Set<String> getTypes(String entity) {
    Set<String> types = null;
    int entityId = entityIDs.containsKey(entity) ? entityIDs.get(entity) : -1;
    if (entity2Types.containsKey(entityId)) {
      types = Sets.newHashSet();
      for (int entityTypeId : entity2Types.get(entityId)) {
        String type = typeIDs.inverse().get(entityTypeId);
        types.add(type);
      }
    }
    return types;
  }

  public boolean hasEntity(String entity) {
    return entityIDs.containsKey(entity);
  }
}
