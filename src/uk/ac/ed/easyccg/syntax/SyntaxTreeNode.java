package uk.ac.ed.easyccg.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import uk.ac.ed.easyccg.syntax.Combinator.RuleType;

public abstract class SyntaxTreeNode implements Comparable<SyntaxTreeNode> {
  
  private final Category category;
  final double probability;
  final int hash;
  final int totalDependencyLength;
  private final int headIndex;
  
  abstract SyntaxTreeNodeLeaf getHead();
  
  private SyntaxTreeNode(
      Category category, 
      double probability, 
      int hash, 
      int totalDependencyLength,
      int headIndex
      )
  {
    this.category = category;
    this.probability = probability;
    this.hash = hash;
    this.totalDependencyLength = totalDependencyLength;
    this.headIndex = headIndex;
  }
  
  static class SyntaxTreeNodeBinary extends SyntaxTreeNode {
    final RuleType ruleType;
    final boolean headIsLeft;
    final SyntaxTreeNode leftChild;
    final SyntaxTreeNode rightChild;
    private SyntaxTreeNodeBinary(Category category, double probability, int hash, int totalDependencyLength, int headIndex,
        RuleType ruleType, boolean headIsLeft, SyntaxTreeNode leftChild, SyntaxTreeNode rightChild)
    {
      super(category, probability, hash, totalDependencyLength, headIndex);
      this.ruleType = ruleType;
      this.headIsLeft = headIsLeft;
      this.leftChild = leftChild;
      this.rightChild = rightChild;
    }
    @Override
    void accept(SyntaxTreeNodeVisitor v)
    {
      v.visit(this);
    }
    @Override
    public RuleType getRuleType()
    {
      return ruleType;
    }
    @Override
    public List<SyntaxTreeNode> getChildren()
    {
      return Arrays.asList(leftChild, rightChild);
    }
    @Override
    SyntaxTreeNodeLeaf getHead()
    {
      return headIsLeft ? leftChild.getHead() : rightChild.getHead();
    }
    @Override
    int dimension()
    {
      int left = leftChild.dimension();
      int right = rightChild.dimension();
      if (left == right) {
        return 1 + left;
      } else {
        return Math.max(left, right);
      }
      
      
    }
    
  }
  
  public static class SyntaxTreeNodeLeaf extends SyntaxTreeNode {
    private SyntaxTreeNodeLeaf(
        String word, String pos, String ner, 
        Category category, double probability, int hash, int totalDependencyLength, int headIndex
        )
    {
      super(category, probability, hash, totalDependencyLength, headIndex);
      this.pos = pos;
      this.ner = ner;
      this.word = word;
    }
    private final String pos;
    private final String ner;
    private final String word;    
    @Override
    void accept(SyntaxTreeNodeVisitor v)
    {
      v.visit(this);
    }
    @Override
    public RuleType getRuleType()
    {
      return RuleType.LEXICON;
    }
    @Override
    public List<SyntaxTreeNode> getChildren()
    {
      return Collections.emptyList();
    }
    
    @Override
    public boolean isLeaf()
    {
      return true;
    }
    
    @Override
    void getWords(List<SyntaxTreeNodeLeaf> result) {
      result.add(this);
    }
    public String getPos()
    {
      return pos;
    }
    public String getNER()
    {
      return ner;
    }
    public String getWord()
    {
      return word;
    }
    @Override
    SyntaxTreeNodeLeaf getHead()
    {
      return this;
    }
    @Override
    int dimension()
    {
      return 0;
    }
    public int getSentencePosition()
    {
      return getHeadIndex();
    }
  }
  
  static class SyntaxTreeNodeUnary extends SyntaxTreeNode {
    private SyntaxTreeNodeUnary(Category category, double probability, int hash, int totalDependencyLength, int headIndex, SyntaxTreeNode child)
    {
      super(category, probability, hash, totalDependencyLength, headIndex);

      this.child = child;
    }

    final SyntaxTreeNode child;

    @Override
    void accept(SyntaxTreeNodeVisitor v)
    {
      v.visit(this);
    }

    @Override
    public RuleType getRuleType()
    {
      return RuleType.UNARY;
    }

    @Override
    public List<SyntaxTreeNode> getChildren()
    {
      return Arrays.asList(child);
    }

    @Override
    SyntaxTreeNodeLeaf getHead()
    {
      return child.getHead();
    }

    @Override
    int dimension()
    {
      return child.dimension();
    }
  }

  public String toString() {
    return ParsePrinter.CCGBANK_PRINTER.print(this, -1);
  }
  
  abstract int dimension();
  
  public int getHeadIndex() {
    return headIndex;
  }
  
  @Override
  public int compareTo(SyntaxTreeNode o)
  {
    return Double.compare(o.probability, probability); 
  }
  
  /**
   * Factory for SyntaxTreeNode. Using a factory so we can have different hashing/caching behaviour when N-best parsing.
   */
  public static class SyntaxTreeNodeFactory {
    private final int[][] categoryHash;
    private final int[][] dependencyHash;
    private final boolean hashWords;
    
    /**
     * These parameters are needed so that it can pre-compute some hash values. 
     * @param maxSentenceLength
     * @param numberOfLexicalCategories
     */
    public SyntaxTreeNodeFactory(int maxSentenceLength, int numberOfLexicalCategories) {
      
      hashWords = numberOfLexicalCategories > 0;
      categoryHash = makeRandomArray(maxSentenceLength, numberOfLexicalCategories + 1);
      dependencyHash = makeRandomArray(maxSentenceLength, maxSentenceLength);
    }

    private int[][] makeRandomArray(int x, int y) {
      Random random = new Random();
      int[][] result = new int[x][y];  
      for (int i=0; i<x; i++) {
        for (int j=0; j<y; j++) {
          result[i][j] = random.nextInt();
        }
      }
      
      return result;
    }
    
    public SyntaxTreeNodeLeaf makeTerminal(String word, Category category, String pos, String ner, double probability, int sentencePosition) {
      return new SyntaxTreeNodeLeaf(
          word, pos, ner, category, probability, 
          hashWords ? categoryHash[sentencePosition][category.getID()] : 0, 0, sentencePosition);
    }
    
    public SyntaxTreeNode makeUnary(Category category, SyntaxTreeNode child) {
      return new SyntaxTreeNodeUnary(category, child.probability, child.hash, child.totalDependencyLength, child.getHeadIndex(), child);
    }
    
    public SyntaxTreeNode makeBinary(Category category, SyntaxTreeNode left, SyntaxTreeNode right, RuleType ruleType, boolean headIsLeft) {
      
      int totalDependencyLength = (right.getHeadIndex() - left.getHeadIndex())
                                  + left.totalDependencyLength + right.totalDependencyLength;

      int hash;
      if (right.getCategory().isPunctuation()) {
        // Ignore punctuation when calculating the hash, because we don't really care where a full-stop attaches.
        hash = left.hash;
      } else if (left.getCategory().isPunctuation()) {
        // Ignore punctuation when calculating the hash, because we don't really care where a full-stop attaches.
        hash = right.hash;
      } else {
        // Combine the hash codes in a commutive way, so that left and right branching derivations can still be equivalent.
        hash = left.hash ^ right.hash ^ dependencyHash[left.getHeadIndex()][right.getHeadIndex()];
      }
      
      SyntaxTreeNode result = new SyntaxTreeNodeBinary(
          category, 
          left.probability + right.probability, // log probabilities 
          hash,  
          totalDependencyLength,
          headIsLeft ? left.getHeadIndex() : right.getHeadIndex(),
          ruleType, 
          headIsLeft,
          left, 
          right);
      
      
      return result;
    }      
  }

  abstract void accept(SyntaxTreeNodeVisitor v);
  
  public interface SyntaxTreeNodeVisitor {
    void visit(SyntaxTreeNodeBinary node);
    void visit(SyntaxTreeNodeUnary node);
    void visit(SyntaxTreeNodeLeaf node);
  }

  public abstract RuleType getRuleType();

  public abstract List<SyntaxTreeNode> getChildren();

  public boolean isLeaf()
  {
    return false;
  }

  /**
   * Returns all the terminal nodes in the sentence, from left to right. 
   */
  public List<SyntaxTreeNodeLeaf> getWords()
  {
    List<SyntaxTreeNodeLeaf> result = new ArrayList<SyntaxTreeNodeLeaf>();
    getWords(result);
    return result;
  }
  
  void getWords(List<SyntaxTreeNodeLeaf> result) {
    for (SyntaxTreeNode child : getChildren()) {
      child.getWords(result);
    }
  }

  public Category getCategory()
  {
    return category;
  }
}