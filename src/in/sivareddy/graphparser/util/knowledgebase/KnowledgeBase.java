package in.sivareddy.graphparser.util.knowledgebase;

import java.util.Set;

public interface KnowledgeBase {
  public Set<Relation> getRelations(String entity1, String entity2);

  public Set<Relation> getRelations(String entity1);

  public boolean hasRelation(String entity1, String entity2);

  public Set<String> getTypes(String entity);

  public boolean hasEntity(String entity);
}
