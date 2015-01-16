package uk.ac.ed.easyccg.rebanking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.ed.easyccg.syntax.Category;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeLeaf;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.Dependency;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.DependencyParse;

/**
 * Subcategorizes PP and PR categories, making categories like: ((S[dcl]\NP)/PP[with])/PR[out]
 */
public class SubcategorizePrepositionRebanker extends Rebanker
{

  private final static Category POSSESSIVE_PRONOUN = Category.valueOf("NP/(N/PP)");
  private final static Category GENITIVE = Category.valueOf("(NP/(N/PP))\\NP");
  
  public int errors = 0;

  
  @Override
  List<SyntaxTreeNodeLeaf> doRebanking(SyntaxTreeNode parse, DependencyParse dependencyParse)
  {
    List<SyntaxTreeNodeLeaf> result = new ArrayList<SyntaxTreeNodeLeaf>(parse.getWords());
    for (int i=0; i<result.size(); i++) {
      
      SyntaxTreeNodeLeaf leaf = result.get(i);
      String word = leaf.getWord().toLowerCase();
      Category leafCategory = leaf.getCategory();
      // PP/NP PP PR 
      if (leafCategory.getHeadCategory().equals(Category.PP) && !leafCategory.isFunctionIntoModifier()) {
        String preposition = word;
        // Add feature        
        Category newCategory = leafCategory.replaceArgument(0, Category.PP.addFeature(preposition));
        
        result.set(i, setCategory(leaf, newCategory));        

        subcategorizePredicates(result, dependencyParse, preposition, leaf, Category.PP);

        
        // Find dependency path from PP its argument NP, and then to its parent predicate
        // president   of   America 
        //    N/PP   PP/NP    NP
        for (Dependency depFromPPtoNP : dependencyParse.getArguments(leaf, 1)) {
          SyntaxTreeNodeLeaf np = depFromPPtoNP.getChild();
          subcategorizePredicates(result, dependencyParse, preposition, np, Category.PP);
        }
        
      } else if (Category.PR.equals(leaf.getCategory())) {
        // Phrasal verb particles
        String preposition = word;
        result.set(leaf.getSentencePosition(), setCategory(leaf, Category.PR.addFeature(preposition)));        
        subcategorizePredicates(result, dependencyParse, preposition, leaf, Category.PR);

      } else if (POSSESSIVE_PRONOUN.equals(leafCategory) || GENITIVE.equals(leafCategory)) {
        // Deal with genitive cases, where a PP argument is used, even though there isn't actually a preposition.

        String preposition = "poss";
        // Add feature        
        Category newCategory = leafCategory.replaceArgument(1, leafCategory.getArgument(1).replaceArgument(1, Category.PP.addFeature(preposition)));
        result.set(i, setCategory(leaf, newCategory));        
        
        if (POSSESSIVE_PRONOUN.equals(leafCategory)) {
          subcategorizePredicates(result, dependencyParse, preposition, leaf, Category.PP);
        } else {
          // Update all the N/PP arguments
          for (Dependency depFromPPtoNoun : dependencyParse.getArguments(leaf, 1)) {
            SyntaxTreeNodeLeaf n_pp = depFromPPtoNoun.getChild();
            
            if (n_pp.getCategory().isFunctionInto(Category.valueOf("(N/PP)\\(N/PP)"))) {

              for (Dependency dependencyToArgumentNoun : dependencyParse.getArguments(n_pp, 2)) {
                updatePredicateCategory(result, preposition, Category.PP, dependencyToArgumentNoun.getChild(), 1);
              }
            }
            
            updatePredicateCategory(result, preposition, Category.PP, n_pp, 1);
          }
        }
        
      }
    }
    
    for (int i=0; i<result.size(); i++) {
      SyntaxTreeNodeLeaf leaf = result.get(i);
      if (!leaf.getCategory().isModifier() &&
          leaf.getCategory().getArgument(leaf.getCategory().getNumberOfArguments()).equals(Category.PP)) {
        if (i < result.size() - 1 && Category.PP.matches(result.get(i + 1).getCategory().getArgument(0))) {
          //X/PP PP/Y
          updatePredicateCategory(result, result.get(i + 1).getWord().toLowerCase(), Category.PP, leaf, leaf.getCategory().getNumberOfArguments());
        } else if (i < result.size() - 2 && 
            Category.PR.matches(result.get(i + 1).getCategory()) && 
            result.get(i + 2).getCategory().isFunctionInto(Category.valueOf("PP\\PR"))) {
          // X/PP PR PP\PR
          updatePredicateCategory(result, result.get(i + 2).getWord().toLowerCase(), Category.PP, leaf, leaf.getCategory().getNumberOfArguments());
        }
      }
    }

    
    for (SyntaxTreeNodeLeaf r : result) {
      
      if (r.getCategory().equals(Category.valueOf("(S[dcl]\\NP)/PP")) || Category.valueOf("N/PP").equals(r.getCategory())) {
        int y=0;
        y++;
        errors++;
        
        if (result.size() < 20) {
          int z=0;
          z++;
        }
      }
    }
    
    return result;
  }

  private void subcategorizePredicates(List<SyntaxTreeNodeLeaf> result, DependencyParse dependencyParse,
      String preposition, SyntaxTreeNodeLeaf np, Category prepositionCategory)
  {
    for (Dependency dependencyToArgumentNP : dependencyParse.getPredicates(np)) {
      SyntaxTreeNodeLeaf predicate = dependencyToArgumentNP.getParent();
      int argToReplace = dependencyToArgumentNP.getArgNumber();

      if (predicate.getCategory().isFunctionInto(Category.valueOf("(N/PP)\\(N/PP)")) && dependencyToArgumentNP.getArgNumber() < 3) {
        int y=0;
        y++;
        for (Dependency dependencyToArgumentNoun : dependencyParse.getArguments(predicate, 2)) {
          updatePredicateCategory(result, preposition, prepositionCategory, dependencyToArgumentNoun.getChild(), 1);
        }
      } else if (predicate.getCategory().isFunctionInto(Category.valueOf("(PP/PP)/NP"))) {

        for (Dependency actualPredicate : dependencyParse.getPredicates(predicate)) {

          updatePredicateCategory(result, preposition, prepositionCategory, actualPredicate.getParent(), actualPredicate.getArgNumber());

        }
      } else if (Category.valueOf("PP|PP").matches(predicate.getCategory())) {

        
      } else if (predicate.getCategory().isModifier() && prepositionCategory.equals(predicate.getCategory().getArgument(argToReplace))) {
        int y=0;
        y++;
      } else {
        updatePredicateCategory(result, preposition, prepositionCategory, predicate, argToReplace);
      }
      
    }
  }
  


  private void updatePredicateCategory(List<SyntaxTreeNodeLeaf> result, String preposition,
      Category prepositionCategory, SyntaxTreeNodeLeaf predicate, int argToReplace)
  {

    
    
    if (!predicate.getCategory().isModifier() &&
        prepositionCategory.equals(predicate.getCategory().getArgument(argToReplace))) {
      Category newCategory = result.get(predicate.getSentencePosition()).getCategory().replaceArgument(argToReplace, prepositionCategory.addFeature(preposition));
      result.set(predicate.getSentencePosition(), setCategory(predicate, newCategory));
    }
  }

  
  private SyntaxTreeNodeLeaf setCategory(SyntaxTreeNodeLeaf leaf, Category newCategory)
  {
    return CCGBankParseReader.factory.makeTerminal(leaf.getWord(), newCategory, leaf.getPos(), leaf.getNER(), 1.0, leaf.getSentencePosition());
  }

  private final static Set<Category> catsToFilter = new HashSet<Category>(Arrays.asList(Category.valueOf("N/PP"), Category.valueOf("(N/PP)/PP"), Category.valueOf("N/PP")));
  
  @Override
  boolean filter(Category c)
  {
    if (c.toString().matches(".*\\[[^\\]]*[^a-z\\]].*") || catsToFilter.contains(c)) {
      return true;
    } else {
      return false;
    }
  }
}
