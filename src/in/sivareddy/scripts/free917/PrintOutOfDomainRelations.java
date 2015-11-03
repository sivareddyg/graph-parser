package in.sivareddy.scripts.free917;

import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

public class PrintOutOfDomainRelations extends ProcessStreamInterface {
  private final Schema schema;

  public PrintOutOfDomainRelations(Schema schema) {
    this.schema = schema;
  }

  @Override
  public void processSentence(JsonObject sentence) {
    String query = sentence.get(SentenceKeys.SPARQL_QUERY).getAsString();
    Pattern datesPattern = Pattern.compile("xsd\\:dateTime\\(\\?([^\\)]+)\\)");
    Set<String> dates = new HashSet<>();
    Matcher datesMatcher = datesPattern.matcher(query);
    while (datesMatcher.find()) {
      dates.add(datesMatcher.group(1));
    }

    Pattern p = Pattern.compile("FILTER .*? \\.");
    query = p.matcher(query).replaceAll("");
    p = Pattern.compile("OPTIONAL \\{[^}]+\\}");
    query = p.matcher(query).replaceAll("");
    p = Pattern.compile("\\{([^}{]+)\\}");
    Matcher m = p.matcher(query);
    if (m.find()) {
      query = m.group(1);
    }

    for (String date : dates) {
      query =
          query.replaceAll(String.format("\\?%s", date), "rdf:type.datetime");
    }

    Pattern floatPattern = Pattern.compile(" [0-9][0-9\\.]+");
    query = floatPattern.matcher(query).replaceAll(" rdf:type.int");
    floatPattern = Pattern.compile("^[0-9][0-9\\.]+");
    query = floatPattern.matcher(query).replaceAll("rdf:type.int");

    query = query.replaceAll(" true", " type.boolean");
    query = query.replaceAll("^true", "type.boolean");

    List<String> predicates =
        Lists.newArrayList(Splitter.on(" . ").trimResults()
            .trimResults(CharMatcher.anyOf(" .")).omitEmptyStrings()
            .split(query));

    boolean isMediator = false;
    for (String predicate : predicates) {
      if (predicate.matches("\\?.*\\?.*")) {
        isMediator = true;
        break;
      }
    }

    for (String predicate : predicates) {
      if (!isDomainRelation(predicate)) {
        System.out.println(String.format("%s\t%s", predicate, isMediator));
      }
    }
  }

  boolean isDomainRelation(String predicate) {
    List<String> parts =
        Splitter.on(CharMatcher.WHITESPACE).splitToList(predicate);
    String relation = parts.get(1).replace("fb:", "");
    return schema.getRelationArguments(relation) != null;
  }

  public static void main(String[] args) throws InterruptedException,
      IOException {
    Schema schema = new Schema(args[0]);
    PrintOutOfDomainRelations engine = new PrintOutOfDomainRelations(schema);

    engine.processStream(System.in, System.out, 1, false);
  }
}
