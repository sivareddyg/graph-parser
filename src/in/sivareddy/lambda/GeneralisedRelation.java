package in.sivareddy.lambda;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.ArrayList;

public class GeneralisedRelation {
  public String relationName;
  public ArrayList<String> entities;

  public GeneralisedRelation(String relationName, ArrayList<String> entities) {
    this.relationName = relationName;
    this.entities = entities;
  }

  public GeneralisedRelation(String relationName, String... entities) {
    this.relationName = relationName;
    this.entities = Lists.newArrayList(entities);
  }

  public static GeneralisedRelation parseString(String relation) {
    String[] elements = relation.split("@");
    return new GeneralisedRelation(elements[0], Lists.newArrayList(elements[1]
        .split(",")));
  }

  @Override
  public String toString() {
    String entityString = "";
    if (entities.size() > 0) {
      String firstEntity = entities.get(0);
      if (firstEntity.startsWith("$")) {
        entityString += "*";
      } else {
        entityString += firstEntity;
      }

      if (entities.size() > 1) {
        for (String entity : entities.subList(1, entities.size())) {
          if (entity.startsWith("$")) {
            entityString += ",*";
          } else {
            entityString += "," + entity;
          }
        }
      }
    }

    return relationName + "@" + entityString;
  }

  public String toTypeString() {
    String entityString = "";
    if (entities.size() > 0) {
      String firstEntity = entities.get(0);
      if (firstEntity.startsWith("$")) {
        entityString += "TYPE:*";
      } else {
        entityString +=
            "TYPE:"
                + Lists.newArrayList(Splitter.on(":").split(firstEntity))
                    .get(1);
      }

      if (entities.size() > 1) {
        for (String entity : entities.subList(1, entities.size())) {
          if (entity.startsWith("$")) {
            entityString += ",TYPE:*";
          } else {
            entityString +=
                "," + "TYPE:"
                    + Lists.newArrayList(Splitter.on(":").split(entity)).get(1);
          }
        }
      }
    }

    return relationName + "@" + entityString;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    GeneralisedRelation objGen = (GeneralisedRelation) obj;
    return objGen.relationName.equals(relationName)
        && objGen.entities.equals(entities) ? true : false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + relationName.hashCode();
    result = prime * result + entities.hashCode();
    return result;
  }
}
