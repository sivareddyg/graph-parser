package in.sivareddy.scripts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.Relation;

/*
 * Example input format:
 * 
 * {'entropy': 1.0361987352371216, 'nl_string':
 * u"appearance.'s.arg_1|appearance.at.arg_2", 'grounding_stats':
 * [{'probability': 0.5714285969734192, 'kg_string': u'<UNGROUNDED>', 'pmi':
 * -0.050609126687049866}, {'probability': 0.2857142984867096, 'kg_string':
 * u'!/organization/organization/headquarters./location/mailing_address/country',
 * 'pmi': 0.2977018356323242}]}
 * 
 * 
 * {'entropy': 3.336534023284912, 'nl_string': u'astronomist',
 * 'grounding_stats': [{'probability': 0.05263157933950424, 'kg_string':
 * u'/location/location', 'pmi': 0.04742710292339325}, {'probability':
 * 0.05263157933950424, 'kg_string': u'/geo/oyster/oyster_feature', 'pmi':
 * 0.04574638605117798}, {'probability': 0.05263157933950424, 'kg_string':
 * u'/location/administrative_division', 'pmi': 0.09226532280445099},
 * {'probability': 0.05263157933950424, 'kg_string':
 * u'/location/dated_location', 'pmi': 0.05580758675932884}, {'probability':
 * 0.05263157933950424, 'kg_string': u'/location/statistical_region', 'pmi':
 * 0.05558693781495094}, {'probability': 0.05263157933950424, 'kg_string':
 * u'/book/book_subject', 'pmi': 0.041995249688625336}]}
 */


public class ConvertDepLambdaGroundedLexiconToGraphParser {

  private final Schema schema;
  private JsonParser jsonParser = new JsonParser();

  public ConvertDepLambdaGroundedLexiconToGraphParser(String schemaFile)
      throws IOException {
    schema = new Schema(schemaFile);
  }

  Relation emptyRelation = new Relation("type.empty.1", "type.empty.2");

  public void convert() throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        // System.out.println(line);
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        String ungrounded_relation = sentence.get("nl_string").getAsString();

        if (ungrounded_relation.contains("|")) { // Freebase relation.
          Double total = 0.0;
          System.out.println(String.format("%s\t1.0",
              ungrounded_relation.replace("|", " ")));
          Map<Relation, Double> groundedRelations = new HashMap<>();
          for (JsonElement relationElm : sentence.get("grounding_stats")
              .getAsJsonArray()) {
            Relation gpRelation;
            JsonObject relationObj = relationElm.getAsJsonObject();
            Double prob = relationObj.get("probability").getAsDouble();
            String relation = relationObj.get("kg_string").getAsString();
            if (!relation.equals("<UNGROUNDED>")) {
              total += prob;
              List<String> subedges = Splitter.on("./").splitToList(relation);
              // System.out.println(subedges);
              if (subedges.size() == 2) {
                String leftEdge =
                    subedges.get(0).replace("!", "").replaceFirst("/", "")
                        .replace("/", ".");

                String mediator = schema.getMediatorArgument(leftEdge);
                if (!mediator.equals(schema.getRelationArguments(leftEdge).get(
                    0))) {
                  leftEdge = schema.getRelation2Inverse(leftEdge);
                }

                String rightEdge =
                    subedges.get(1).replace("!", "").replace("/", ".");

                mediator = schema.getMediatorArgument(rightEdge);
                if (!mediator.equals(schema.getRelationArguments(rightEdge)
                    .get(0))) {
                  rightEdge = schema.getRelation2Inverse(rightEdge);
                }
                gpRelation = new Relation(leftEdge, rightEdge);
                if (relation.startsWith("!")) {
                  gpRelation = gpRelation.inverse();
                }
              } else {
                String mainRelation =
                    subedges.get(0).replace("!", "").replaceFirst("/", "")
                        .replace("/", ".");
                if (!schema.getRelationIsMaster(mainRelation)) {
                  mainRelation = schema.getRelation2Inverse(mainRelation);
                  gpRelation =
                      new Relation(mainRelation + ".1", mainRelation + ".2");
                } else {
                  gpRelation =
                      new Relation(mainRelation + ".2", mainRelation + ".1");
                }

                if (relation.startsWith("!")) {
                  gpRelation = gpRelation.inverse();
                }
              }
              Double prevScore =
                  groundedRelations.getOrDefault(gpRelation, 0.0);
              groundedRelations.put(gpRelation, prevScore + prob);
            }
          }
          groundedRelations.put(emptyRelation, 1.0 - total);

          List<Entry<Relation, Double>> entryList =
              Lists.newArrayList(groundedRelations.entrySet());

          // Sort in descending order.
          entryList.sort(Comparator.comparing(x -> -1 * x.getValue()));
          for (Entry<Relation, Double> entry : entryList) {
            System.out.println(String.format("\t%s %s\t%f", entry.getKey()
                .getLeft(), entry.getKey().getRight(), entry.getValue()));
          }
        } else { // Freebase type.

        }
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public static void main(String[] args) throws IOException {
    ConvertDepLambdaGroundedLexiconToGraphParser convertor =
        new ConvertDepLambdaGroundedLexiconToGraphParser(args[0]);
    convertor.convert();
  }

}
