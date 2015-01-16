package uk.ac.ed.easyccg.syntax.evaluation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.Dependency;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.DependencyParse;

import com.google.common.collect.Sets;

public class Evaluate
{

  public static class Results {
    public static Results EMPTY = new Results(0, 0, 0);
    private final AtomicInteger parseDependencies;
    private final AtomicInteger correctDependencies;
    private final AtomicInteger goldDependencies;
    private Results(int parseDependencies, int correctDependencies, int goldDependencies)
    {
      this.parseDependencies = new AtomicInteger(parseDependencies);
      this.correctDependencies = new AtomicInteger(correctDependencies);
      this.goldDependencies = new AtomicInteger(goldDependencies);
    }
    public Results()
    {
      this (0, 0, 0);
    }
    public void add(Results other)
    {
      parseDependencies.addAndGet(other.parseDependencies.get());
      correctDependencies.addAndGet(other.correctDependencies.get());
      goldDependencies.addAndGet(other.goldDependencies.get());
    }
    public double getRecall()
    {
      return (double) correctDependencies.get() / goldDependencies.get();
    }
    
    public double getPrecision()
    {
      return (double) correctDependencies.get() / parseDependencies.get();
    }
    
    public double getF1()
    {
      return 2 * (getPrecision() * getRecall()) / (getPrecision() + getRecall());
    }
    public boolean isEmpty()
    {
      return goldDependencies.get() == 0;
    }
  }

  /**
   * Evaluates a parse against a gold standard parse.
   */
  public static Results evaluate(DependencyParse parse, DependencyParse gold) {
    //Conversion script errors
    if (parse == null) return new Results();
    
    Set<String> parseDeps = convertDependencies(parse);
    Set<String> goldDeps = convertDependencies(gold);
    
    Set<String> right = Sets.intersection(parseDeps, goldDeps);

    return new Results(parseDeps.size(), right.size(), goldDeps.size());
  }
  
  /**
   * Converts a DependencyParse into a set of string objects, to make it easier to compare with others.
   */
  private static Set<String> convertDependencies(DependencyParse parse) {
    Set<String> result = new HashSet<String>();
    for (Dependency dep : parse.getDependencies()) {
      result.add(dep.getParent().getCategory() + "_" + dep.getArgNumber() + "_" + dep.getSentencePositionOfPredicate() + "_" + dep.getSentencePositionOfArgument());
    }
    
    return result;
  }
  
  /**
   * Given a list of parses, and a gold dependencies file, it returns the parse which maximizes recall.
   */
  public static SyntaxTreeNode getOracle(List<SyntaxTreeNode> parses, DependencyParse gold) {
    Results best = null;
    SyntaxTreeNode bestParse = null;
    List<DependencyParse> dependencyParses = CCGBankDependencies.getDependencies(parses, -1);
    
    
    for (int i=0; i<dependencyParses.size(); i++) {
      SyntaxTreeNode parse = parses.get(i);
      DependencyParse dependencyParse = dependencyParses.get(i);
      if (dependencyParse == null) continue;

      Results results = evaluate(dependencyParse, gold);
      if (best == null || results.getRecall() > best.getRecall()) {
        best = results;
        bestParse = parse;
      }
    }
    
    return bestParse;
  }
}
