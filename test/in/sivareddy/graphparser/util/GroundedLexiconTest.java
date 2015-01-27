package in.sivareddy.graphparser.util;

import in.sivareddy.graphparser.util.KnowledgeBase.EntityType;
import in.sivareddy.graphparser.util.KnowledgeBase.Relation;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class GroundedLexiconTest {

  @Test
  public void testGroundedLexicon() throws IOException {
    GroundedLexicon lexicon =
        new GroundedLexicon(
            "data/freebase/grounded_lexicon/business_grounded_lexicon.txt");
    Relation urel = new Relation("founder.of.1", "founder.of.2");
    List<Relation> grels = lexicon.getGroundedRelations(urel);

    Relation grel = grels.get(2);
    System.out.println("Ungrounded Relation: " + urel);
    System.out.println("urel tf-idf: "
        + lexicon.getUngroundedRelationScore(urel));
    System.out.println("Rel freq: " + lexicon.getUngroundedRelationFreq(urel));

    System.out.println("Grounded Relation: " + grels);
    System.out.println("Target grel: " + grel);
    System.out.println("Grel freq: " + " "
        + lexicon.getGroundedRelationFreq(grel));
    System.out.println("prob urel grel: " + grel.getWeight());
    System.out
        .println("prob urel grel: " + lexicon.getUrelGrelProb(urel, grel));

    System.out.println("urel grel part prob : "
        + lexicon.getUrelPartGrelPartProb(urel.getLeft(), grel.getLeft()));
    System.out.println("urel grel part prob : "
        + lexicon.getUrelPartGrelPartProb(urel.getLeft(), grel.getRight()));

    EntityType utype = new EntityType("founder.1");
    List<EntityType> gtypes = lexicon.getGroundedTypes(utype);
    System.out.println("Ungrounded: " + utype);
    System.out.println("Ungrounded tf-idf: "
        + lexicon.getUngroundedTypeScore(utype));
    System.out.println("Grounded Types: " + gtypes);
    EntityType gtype = gtypes.get(2);
    System.out.println("Target Grounded Type: " + gtype);
    System.out.println("prob utype gtype: "
        + lexicon.getUtypeGtypeProb(utype.getType(), gtype.getType()));
    System.out.println("prob utype gtype: " + gtype.getWeight());
  }

}
