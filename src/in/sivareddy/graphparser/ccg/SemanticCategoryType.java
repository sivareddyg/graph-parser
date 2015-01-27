package in.sivareddy.graphparser.ccg;

import com.google.common.collect.ImmutableSet;

/**
* Created by bisk1 on 1/26/15.
*/
public enum SemanticCategoryType {
  TYPE, TYPEMOD, COMPLEMENT, EVENT, EVENTMOD, NEGATION, CLOSED, EMPTY, IDENTITY, COPULA, UNIQUE, COUNT, QUESTION;

  public final static ImmutableSet<String> types = ImmutableSet.of("TYPE",
      "TYPEMOD", "COMPLEMENT", "EVENT", "EVENTMOD", "NEGATION", "CLOSED",
      "EMPTY", "IDENTITY", "COPULA", "UNIQUE", "COUNT", "QUESTION");
}
