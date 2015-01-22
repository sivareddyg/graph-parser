package in.sivareddy.graphparser.util;

import java.io.IOException;

import org.junit.Test;

public class KnowledgeBaseTest {

  @Test
  public void test() throws IOException {
    KnowledgeBase kb =
        new KnowledgeBase("data/freebase/domain_facts/business_facts.txt.gz",
            "data/freebase/stats/business_relation_types.txt");

    // true
    System.out.println(kb.hasRelation("m.06rn8p", "m.031q99"));

    // ["m.06rn8p","m.031q99"]
    // [["organization.organization_board_membership.member","organization.organization_board_membership.organization"],["organization.leadership.person","organization.leadership.organization"]]
    System.out.println(kb.getRelations("m.06rn8p", "m.031q99"));

    // "m.06rn8p" ["business.board_member"]
    System.out.println(kb.getTypes("m.06rn8p"));

    // m.017nt
    System.out.println(kb.getTypes("m.017nt"));
  }
}
