package in.sivareddy.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.RdfGraphTools;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.graphparser.util.knowledgebase.Property;
import in.sivareddy.graphparser.util.knowledgebase.Relation;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

public class DumpDatabaseFromEntities extends ProcessStreamInterface {
  private KnowledgeBase kb;
  private RdfGraphTools endPoint;
  private Schema schema;
  private final Gson gson;
  private JsonParser jsonParser = new JsonParser();

  public DumpDatabaseFromEntities(Schema schema, KnowledgeBase kb,
      RdfGraphTools endPoint) {
    this.kb = kb;
    this.endPoint = endPoint;
    this.schema = schema;
    this.gson = new Gson();
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    KnowledgeBaseOnline.TYPE_KEY = "fb:type.object.type";

    Schema schema =
        new Schema("data/freebase/schema/business_film_people_schema.txt");

    KnowledgeBase kb = new KnowledgeBaseOnline("buck.inf.ed.ac.uk",
        "http://buck.inf.ed.ac.uk:8890/sparql", "dba", "dba", 100000, schema);

    RdfGraphTools endPoint =
        new RdfGraphTools("jdbc:virtuoso://buck.inf.ed.ac.uk:1111",
            "http://buck.inf.ed.ac.uk:8890/sparql", "dba", "dba");
    DumpDatabaseFromEntities dumper =
        new DumpDatabaseFromEntities(schema, kb, endPoint);

    dumper.processStream(System.in, System.out, 10, true);
    
    // JsonObject sentence = new JsonObject();
    // sentence.addProperty(SentenceKeys.ENTITY, "m.0lwkh");
    // dumper.processSentence(sentence);
    // Gson gson = new Gson();
    // System.out.println(gson.toJson(sentence));
  }

  @Override
  public void processSentence(JsonObject sentence) {
    String mid = sentence.get(SentenceKeys.ENTITY).getAsString();
    Set<Relation> relations = kb.getRelations(mid);

    JsonArray triples = new JsonArray();
    for (Relation relation : relations) {
      LexicalGraph graph = new LexicalGraph();
      LexicalItem entityNode = new LexicalItem("", mid, mid, "NNP", "", null);
      entityNode.setWordPosition(0);
      entityNode.setMid(mid);

      LexicalItem questionNode = new LexicalItem("", "q", "q", "NNP", "", null);
      questionNode.setWordPosition(1);

      LexicalItem mediatorNode = new LexicalItem("", "m", "m", "1", "", null);
      mediatorNode.setWordPosition(2);

      Edge<LexicalItem> edge =
          new Edge<>(entityNode, questionNode, mediatorNode, relation);
      graph.addNode(entityNode);
      graph.addNode(questionNode);
      graph.addNode(mediatorNode);

      Property questionProperty = new Property("QUESTION");
      graph.addEdge(edge);
      graph.addProperty(questionNode, questionProperty);

      String query =
          GraphToSparqlConverter.convertGroundedGraph(graph, schema, 100000000);
      List<Map<String, String>> answers = endPoint.runQueryHttpSolutions(query);

      List<Map<String, String>> e1Name =
          endPoint.runQueryHttpSolutions(String.format(
              "PREFIX fb: <http://rdf.freebase.com/ns/> PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> SELECT DISTINCT ?x1name  WHERE { OPTIONAL {FILTER(langMatches(lang(?x1name), \"en\")) . FILTER(!langMatches(lang(?x1name), \"en-gb\")) . fb:%s fb:type.object.name ?x1name . } } LIMIT 100000",
              mid));


      List<String> relationTriple = new ArrayList<>();
      relationTriple.add(relation.getLeft());
      relationTriple.add(relation.getRight());

      // System.out.println(relation);
      for (Map<String, String> answerDict : answers) {
        String entity2 = answerDict.get("x1");
        entity2 = entity2.replace("http://rdf.freebase.com/ns/", "");
        entity2 =
            entity2.replace("^^<\"http://www.w3.org/2001/XMLSchema#", ":");
        entity2 = entity2.replaceFirst("\">$", "");
        // System.out.println(entity2);
        relationTriple.add(entity2);
      }
      triples.add(jsonParser.parse(gson.toJson(relationTriple)));
    }
    sentence.add("relations", triples);
  }
}
