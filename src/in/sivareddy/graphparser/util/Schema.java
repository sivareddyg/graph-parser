package in.sivareddy.graphparser.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Schema {

  String namespace = "http://rdf.freebase.com/ns/";
  private ImmutableSet<String> types;
  private ImmutableSet<String> mainTypes;
  private ImmutableSet<String> relations;
  private Map<String, ImmutableList<String>> type2Relations;
  private Map<String, EntityTypes> typeIs;
  private Map<String, String> relation2Inverse;
  private Map<String, Boolean> relationIsMaster;
  private Map<String, ImmutableList<String>> relationArguments;
  public static Set<String> acceptableCommonTypes =
      Sets.newHashSet("type.datetime", "type.float", "type.int");

  public Schema(String fileName) throws IOException {
    type2Relations = Maps.newHashMap();
    typeIs = Maps.newHashMap();
    for (String type : acceptableCommonTypes) {
      typeIs.put(type, EntityTypes.FOREIGN);
    }

    relation2Inverse = Maps.newHashMap();
    relationIsMaster = Maps.newHashMap();
    relationArguments = Maps.newHashMap();
    loadFromFile(fileName);
  }

  public static enum EntityTypes {
    MAIN, FOREIGN, MEDIATOR, FOREIGN_MEDIATOR, MAIN_EXTENDED;

    public final static ImmutableSet<String> types =
        ImmutableSet.of("MAIN", "FOREIGN", "MEDIATOR", "FOREIGN_MEDIATOR", "MAIN_EXTENDED");

    public static EntityTypes toType(String typeString) {
      if (typeString.equals("main")) {
        return MAIN;
      } else if (typeString.equals("foreign")) {
        return FOREIGN;
      } else if (typeString.equals("mediator")) {
        return MEDIATOR;
      } else if (typeString.equals("foreign_mediator")) {
        return FOREIGN_MEDIATOR;
      } else if (typeString.equals("main_extended")) {
        return MAIN_EXTENDED;
      }
      Preconditions.checkArgument(false, typeString + " is not in " + types);
      return null;
    }
  }

  public void loadFromFile(String fileName) throws IOException {
    BufferedReader bf = new BufferedReader(new FileReader(fileName));

    Set<String> types = Sets.newHashSet();
    Set<String> mainTypes = Sets.newHashSet("type.datetime", "type.int", "type.float");
    Set<String> relations = Sets.newHashSet();

    String line = bf.readLine();
    while (line != null) {
      if (line.equals("")) {
        line = bf.readLine();
      } else if (line.charAt(0) != '\t' && line.charAt(0) != '#') {
        line = line.replace(namespace, "");
        List<String> parts =
            Lists.newArrayList(Splitter.on('\t').trimResults().omitEmptyStrings().split(line));

        String type = parts.get(0);

        EntityTypes typeType = EntityTypes.toType(parts.get(1));
        /*- Boolean mediator = false;
        if (typeType.equals("mediator"))
        	mediator = true;*/
        types.add(type);
        typeIs.put(type, typeType);
        if (typeType == EntityTypes.MAIN) {
          mainTypes.add(type);
        }

        line = bf.readLine();
        while (line != null && line.length() == 0)
          line = bf.readLine();

        List<String> typeRelations = Lists.newArrayList();
        while (line != null && line.charAt(0) == '\t') {
          line = line.replace(namespace, "");
          parts =
              Lists.newArrayList(Splitter.on('\t').trimResults().omitEmptyStrings().split(line));
          String relation = parts.get(0);
          String childType = parts.get(1);
          String relationType = parts.get(2);
          String relationInverse = parts.get(3);

          typeRelations.add(relation);
          Boolean master = false;
          relations.add(relation);
          if (relationType.equals("master")) {
            master = true;
          }
          relationIsMaster.put(relation, master);

          if (!relationInverse.equals("none")) {
            relation2Inverse.put(relation, relationInverse);
          }

          ImmutableList<String> relationArgs = ImmutableList.of(type, childType);
          relationArguments.put(relation, relationArgs);
          ImmutableList<String> relationArgsInverse = ImmutableList.of(childType, type);
          relationArguments.put(relationInverse, relationArgsInverse);
          line = bf.readLine();
          while (line != null && line.length() == 0)
            line = bf.readLine();

        }
        type2Relations.put(type, ImmutableList.copyOf(typeRelations));

      } else {
        line = bf.readLine();
      }
    }

    this.types = ImmutableSet.copyOf(types);
    this.mainTypes = ImmutableSet.copyOf(mainTypes);
    this.relations = ImmutableSet.copyOf(relations);
  }

  /**
   *
   * Return all types in the current schema
   *
   * @return
   */
  public Set<String> getTypes() {
    return types;
  }

  /**
   * Return the main types of the domain along with standard types i.e. type.datetime, type.int,
   * type.float
   *
   * @return
   */
  public Set<String> getMainTypes() {
    return mainTypes;
  }

  /**
   * Get all the relations in the current schema
   *
   * @return
   */
  public Set<String> getRelations() {
    return relations;
  }

  /**
   * Get all the relations of a type
   *
   * @param key
   * @return
   */
  public List<String> getType2Relations(String key) {
    if (type2Relations.containsKey(key)) {
      return type2Relations.get(key);
    }
    return null;
  }

  /**
   * Returns the type of the entity return values one of these: main, foreign,
   *
   * @param key
   * @return
   */
  public EntityTypes getTypeIs(String key) {
    if (typeIs.containsKey(key)) {
      return typeIs.get(key);
    }
    if (acceptableCommonTypes.contains(key)) {
      return EntityTypes.FOREIGN;
    }
    return null;
  }

  /**
   * Returns if a type is mediator
   *
   * @param key
   * @return
   */
  public Boolean typeIsMediator(String key) {
    if (typeIs.containsKey(key)) {
      if (typeIs.get(key).equals(EntityTypes.MEDIATOR)
          || typeIs.get(key).equals(EntityTypes.FOREIGN_MEDIATOR)) {
        return true;
      } else {
        return false;
      }
    } else if (acceptableCommonTypes.contains(key)) {
      return false;
    }
    return null;
  }

  /**
   * Returns the inverse of a relation or null if not inverse is found
   *
   * @param key
   * @return
   */
  public String getRelation2Inverse(String key) {
    if (relation2Inverse.containsKey(key)) {
      return relation2Inverse.get(key);
    }
    return null;
  }

  /**
   * Checks if the relation is a master relation. True if master, false if inverse
   *
   * @param key
   * @return
   */
  public Boolean getRelationIsMaster(String key) {
    if (relationIsMaster.containsKey(key)) {
      return relationIsMaster.get(key);
    }
    return null;
  }

  /**
   *
   * Returns the parent, child entity types of the relation
   *
   * @param key
   * @return
   */
  public List<String> getRelationArguments(String key) {
    if (relationArguments.containsKey(key)) {
      return relationArguments.get(key);
    }
    return null;
  }

  /**
   * check if the parent of the relation is mediator and returns the mediator relation. If the
   * parent is not mediator, returns null
   *
   * @param relation
   * @return
   */
  public String getMediatorArgument(String relation) {
    List<String> args = getRelationArguments(relation);
    String arg1 = args.get(0);
    if (typeIsMediator(arg1)) {
      return arg1;
    }
    String arg2 = args.get(1);
    if (typeIsMediator(arg2)) {
      return arg2;
    }
    return null;
  }

  public static void main(String[] args) throws IOException {
    /*-Schema schema = new Schema("data/freebase/schema/film_schema.txt");

    System.out.println(schema.getTypes());
    System.out.println(schema.getRelations());
    System.out.println(schema.getType2Relations("film.film_series"));
    System.out.println(schema.getTypeIs("film.film_series"));
    System.out.println(schema.getType2Relations("film.performance"));
    System.out.println(schema.getTypeIs("film.performance"));

    System.out.println(schema.getRelation2Inverse("film.film_film_company_relationship.role_service"));
    System.out.println(schema.getRelationIsMaster("film.film_film_company_relationship.role_service"));
    System.out.println(schema.getRelationIsMaster("film.film_company_role_or_service.companies_performing_this_role_or_service"));

    System.out.println(schema.getRelationArguments("film.film_film_company_relationship.role_service"));
    System.out.println(schema.getRelationArguments("film.film_company_role_or_service.companies_performing_this_role_or_service"));

    schema = new Schema("data/freebase/schema/people_schema.txt");
    System.out.println(schema.getType2Relations("people.marriage"));*/

Schema schema =
        new Schema("data/freebase/schema/business_extended_schema.txt");
    System.out.println(schema.getType2Relations("business.employment_tenure"));



  }

  public boolean hasMediatorArgument(String relation) {
    // System.out.println(relation);
    List<String> args = getRelationArguments(relation);
    for (String arg : args) {
      Boolean type = typeIsMediator(arg);
      if (type != null && type) {
        return true;
      }
    }
    return false;
  }
}
