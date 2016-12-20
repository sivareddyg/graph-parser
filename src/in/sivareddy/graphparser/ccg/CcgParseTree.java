package in.sivareddy.graphparser.ccg;

import in.sivareddy.graphparser.ccg.SyntacticCategory.BadParseException;
import in.sivareddy.graphparser.ccg.SyntacticCategory.Direction;
import in.sivareddy.util.SentenceKeys;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CcgParseTree {
  /*- (<T S[dcl] ba 1 2> 
   		(<T NP lex 0 1> (<L N Cameron Cameron NNP I-PER I-NP N>)) 
  			(<T S[dcl]\NP fa 0 2> (<L (S[dcl]\NP)/NP directed direct VBD O I-VP (S[dcl]\NP)/NP>) 
  					(<T NP lex 0 1> (<L N Titanic Titanic NNP O I-NP N>))))
  					
   */

  protected CcgAutoLexicon autoLexicon = null;
  static Map<Integer, LexicalItem> nodesIndexMap = new ConcurrentHashMap<>();
  private static int nodeCount = 1;
  private static int maxNodeCount = 50000;
  public static Set<String> lexicalPosTags = Sets.newHashSet("NNP", "CD",
      "NNPS", "PRP", "PRON", "NUM");
  private static Set<String> cardinalPosTags = Sets.newHashSet("CD", "NUM");
  private static Set<String> dateNerTags = Sets.newHashSet("I-DAT");

  // changed these variables from static to dynamic. Should use lower case
  // variable names
  public static String[] RELATION_IDENTIFIERS;
  public static String[] ARGUMENT_IDENTIFIERS;
  public static String[] RELATION_TYPING_IDENTIFIERS;
  public static boolean IGNOREPRONOUNS = true;
  private int MAX_SEMANTIC_PARSES = 16;

  public static synchronized int getNodeCount() {
    nodeCount++;
    if (nodeCount % maxNodeCount == 0)
      nodeCount = 0;
    return nodeCount;
  }

  /**
   * Construct leaf nodes of a CCG Tree.
   * 
   * @param lexicalItemString
   * @return
   */
  public List<LexicalItem> buildLexicalItems(String lexicalItemString) {
    ArrayList<String> items =
        Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> ")).trimResults()
            .omitEmptyStrings().split(lexicalItemString));
    String synCat = items.get(1);
    String word = items.get(2);
    String lemma = items.get(3).toLowerCase();
    String pos = items.get(4);
    String neType = items.get(5);
    List<Category> currentCategories =
        autoLexicon.getCategory(lemma.toLowerCase(), pos, synCat);
    List<LexicalItem> lexItems = new ArrayList<>();
    for (Category cat : currentCategories) {
      LexicalItem lex = new LexicalItem(synCat, word, lemma, pos, neType,
          SentenceKeys.ENGLISH_LANGUAGE_CODE, cat);
      lexItems.add(lex);
    }
    return lexItems;
  }

  CcgParseTree() {}

  public LexicalItem getFirstLeafNode() {
    if (this.getClass().equals(LexicalItem.class))
      return (LexicalItem) this;
    else
      return children.get(0).getFirstLeafNode();
  }

  public List<LexicalItem> getLeafNodes() {
    List<LexicalItem> leaves = Lists.newArrayList();
    getLeafNodes(leaves);
    return leaves;
  }

  private void getLeafNodes(List<LexicalItem> leaves) {
    if (this.getClass().equals(LexicalItem.class))
      leaves.add((LexicalItem) this);
    else {
      for (CcgParseTree child : children) {
        child.getLeafNodes(leaves);
      }
    }
  }

  protected Category currentCategory;
  private List<CcgParseTree> children;
  @SuppressWarnings("unused")
  private int head;
  private CcgCombinator combinator;
  private List<LexicalItem> leaves;

  public Category getCategory() {
    return currentCategory;
  }

  public List<CcgParseTree> parseFromString(String treeString)
      throws FunnyCombinatorException, BadParseException,
      TooManyParsesException {
    // System.err.println(treeString);
    List<CcgParseTree> nodes = parseFromStringHidden(treeString);
    for (CcgParseTree node : nodes) {
      node.leaves = node.getLeafNodes();
      int wordPosition = 0;
      for (LexicalItem leaf : node.leaves) {
        leaf.wordPosition = wordPosition;
        wordPosition += 1;
      }
    }
    return nodes;
  }

  private List<CcgParseTree> parseFromStringHidden(String treeString)
      throws FunnyCombinatorException, BadParseException,
      TooManyParsesException {
    Stack<CcgParseTree> nodes = new Stack<>();
    List<CcgParseTree> trees = new ArrayList<>();
    Stack<Character> cStack = new Stack<>();
    char[] cArray = treeString.toCharArray();
    boolean foundOpenLessThan = false;

    List<List<LexicalItem>> treePaths = new ArrayList<>();
    treePaths.add(new ArrayList<LexicalItem>());
    StringBuilder newTreeSb = new StringBuilder();

    int len = cArray.length;
    int leafCount = 0;
    for (int i = 0; i < len; i++) {
      // leaf start
      if (i + 1 < len && i + 2 < len && cArray[i] == '<'
          && cArray[i + 1] == 'L' && cArray[i + 2] == ' ') {
        StringBuilder leafString = new StringBuilder();
        while (cArray[i] != '>') {
          leafString.append(cArray[i]);
          i++;
        }
        leafString.append('>');
        List<LexicalItem> leaves = buildLexicalItems(leafString.toString());
        List<List<LexicalItem>> paths = new ArrayList<>();
        for (LexicalItem leaf : leaves) {
          for (List<LexicalItem> path : treePaths) {
            List<LexicalItem> pathCopy = new ArrayList<>(path);
            pathCopy.add(leaf);
            paths.add(pathCopy);
          }
        }
        treePaths = paths;
        newTreeSb.append("<L ");
        newTreeSb.append(leafCount);
        newTreeSb.append(">");
        leafCount += 1;
      } else {
        newTreeSb.append(cArray[i]);
      }
    }

    if (treePaths.size() > MAX_SEMANTIC_PARSES) {
      throw new TooManyParsesException(String.format("%d parses possible.",
          treePaths.size()));
    }

    cArray = newTreeSb.toString().toCharArray();
    for (List<LexicalItem> path : treePaths) {
      for (Character c : cArray) {
        if (c == '<')
          foundOpenLessThan = true;
        else if (c == '>')
          foundOpenLessThan = false;

        if (c == ')' && !foundOpenLessThan) {
          StringBuilder sb = new StringBuilder();
          Character cPop = cStack.pop();
          while (cPop != '<') {
            sb.append(cPop);
            cPop = cStack.pop();
          }
          sb.append(cPop);
          cStack.pop();
          sb.reverse();
          String nodeString = sb.toString();
          // (<L N Titanic Titanic NNP O I-NP N>)
          if (nodeString.charAt(1) == 'L') {
            Pattern pattern = Pattern.compile("[0-9]+");
            Matcher matcher = pattern.matcher(nodeString);
            matcher.find();
            int leafNodeIndex = Integer.parseInt(matcher.group());
            CcgParseTree node = path.get(leafNodeIndex).shallowCopy();
            nodes.add(node);
          } else if (nodeString.charAt(1) == 'T') {
            // (<T NP lex 0 1> (<L N Titanic Titanic NNP O I-NP
            // N>))))
            ArrayList<String> items =
                Lists.newArrayList(Splitter.on(CharMatcher.anyOf("<> "))
                    .trimResults().omitEmptyStrings().split(nodeString));
            CcgParseTree node = new CcgParseTree();
            int childrenSize = Integer.parseInt(items.get(4));
            node.children = Lists.newArrayList();
            node.head = Integer.parseInt(items.get(3));
            while (childrenSize > 0) {
              node.children.add(0, nodes.pop());
              childrenSize--;
            }
            node.combinator = CcgCombinator.getCombinator(items.get(2));
            String resultantCategoryString = items.get(1);
            node.currentCategory =
                applyCombinator(node.combinator, node.children,
                    resultantCategoryString);
            nodes.add(node);
          }
        } else {
          cStack.add(c);
        }
      }
      Preconditions.checkArgument(nodes.size() == 1, "Bad Tree");
      trees.add(nodes.pop());
    }
    return trees;
  }

  /**
   * resultantCategoryString is only used for unary and binary rules only.
   * 
   * @param combinator
   * @param children
   * @param resultantSynCatString
   * @return
   * @throws FunnyCombinatorException
   * @throws BadParseException
   */
  private Category applyCombinator(CcgCombinator combinator,
      List<CcgParseTree> children, String resultantSynCatString)
      throws FunnyCombinatorException, BadParseException {
    Category result = null;
    Category cat1;
    Category cat2;

    switch (combinator) {
      case fa: {
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply forward application");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.forwardApplication(cat1, cat2);
        break;
      }
      case ba: {
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply backward application");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;

        // Below is a dirty implemetation to cover special conjunction cases.
        // C&C treats noun complements with punctuation as conjunctions rather
        // than modifiers.
        // Cameron , the director of Titanic .....
        // We aim to treat it as modifier construction.
        CategoryIndex cat2Index = cat2.getSyntacticCategory().getIndex();
        String cat2SimpleString =
            cat2.getSyntacticCategory().toSuperSimpleString();
        if (cat2SimpleString.equals("(NP|NP)") && cat2Index.isCC()
            && cat2Index.getCCvars().size() == 2) {
          Iterator<CategoryIndex> ccVars = cat2Index.getCCvars().iterator();
          CategoryIndex node2Index = ccVars.next();
          // trying to find the right most category in the conjunction
          if (node2Index.getVariableValue() != null
              && !node2Index.getVariableValue().isInitialised())
            node2Index = ccVars.next();
          else if (node2Index.getVariableValue() == null) {
            node2Index = ccVars.next();
          }

          Integer node2Key = null;
          if (node2Index.getVariableValue() != null)
            node2Key = node2Index.getVariableValue().getValue();

          CategoryIndex node1Index = cat1.getSyntacticCategory().getIndex();
          Integer node1Key = null;
          if (node1Index.getVariableValue() != null)
            node1Key = node1Index.getVariableValue().getValue();

          if (node2Key != null && node1Key != null
              && nodesIndexMap.containsKey(node2Key)
              && nodesIndexMap.containsKey(node1Key)) {
            LexicalItem node2 = nodesIndexMap.get(node2Key);
            LexicalItem node1 = nodesIndexMap.get(node1Key);
            if (lexicalPosTags.contains(node1.pos)
                && !lexicalPosTags.contains(node2.pos)) {
              // phrase is a complement rather than conjunction
              List<String> bodyExpressions =
                  cat2.getSemanticCategory().getContentRelations();
              SyntacticCategory synCat2Old = cat2.getSyntacticCategory();
              SyntacticCategory syncat2New =
                  new SyntacticCategory(synCat2Old.getArgument(),
                      synCat2Old.getArgument(), new CategoryIndex(),
                      Direction.LEFT);
              List<String> lambdas = Lists.newArrayList();
              Set<String> exists = Sets.newHashSet();

              Pattern pattern = Pattern.compile("\\$[^\\s\\)]+");
              Matcher matcher;
              for (String bodyExpression : bodyExpressions) {
                matcher = pattern.matcher(bodyExpression);
                while (matcher.find())
                  exists.add(matcher.group());
              }

              bodyExpressions
                  .add(String.format("(COPULA %s %s)",
                      node1Index.getVarNameAndKey(),
                      node2Index.getVarNameAndKey()));
              bodyExpressions.add(String.format("($f %s)",
                  node1Index.getVarNameAndKey()));
              lambdas.add("$f");
              lambdas.add(node1Index.getVarNameAndKey());
              exists.removeAll(lambdas);

              SemanticCategory semcat2New =
                  SemanticCategory.generateSemanticCategory(lambdas, exists,
                      bodyExpressions);
              cat2 = new Category(syncat2New, semcat2New);
            }
          }
        }

        result = Category.backwardApplication(cat1, cat2);
        break;
      }
      case fc: {
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply composition");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.forwardComposition(cat1, cat2);
        break;
      }
      case bc:
      case bx: {
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply composition");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.backwardComposition(cat1, cat2);
        break;
      }
      case gfc: {
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply generalised composition");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.generalisedForwardComposition(cat1, cat2);
        break;
      }
      case gbc:
      case gbx: {
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply generalised composition");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.generalisedBackwardComposition(cat1, cat2);
        break;
      }
      case tr: {
        Preconditions.checkArgument(children.size() == 1,
            "Cannot apply typeraising");
        cat1 = children.get(0).currentCategory;
        result = Category.typeRaising(cat1);
        SyntacticCategory resultSyncat = result.getSyntacticCategory();
        SyntacticCategory resultSynCatActual =
            SyntacticCategory.fromString(resultantSynCatString);
        resultSyncat.unify(resultSynCatActual);
        break;
      }
      case conj:
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply conjunction");
        cat2 = children.get(1).currentCategory;
        CcgCombinator comb2 = children.get(1).combinator;
        // word2 = children.get(1).getFirstLeafNode().word;
        // if (cat2.getSyntacticCategory().getIndex().isCC() &&
        // cat2.getSyntacticCategory().toSuperSimpleString().equals("(NP|NP)"))
        // if cat2 is cc, just return cc - Currently support only one cc.
        /*-if (cat2.getSyntacticCategory().getIndex().isCC() && children.get(0).getClass().equals(LexicalItem.class)
        		&& (((LexicalItem) children.get(0)).pos.equals("SYM") || ((LexicalItem) children.get(0)).pos.matches("[^A-Z]"))
        		&& !cat2.getSyntacticCategory().isBasic())*/
        if (comb2 == CcgCombinator.conj) {
          result = cat2;
        } else {
          result = Category.coordinationApplication(cat2);
        }
        break;
      case lp:
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply left punctuation rules");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.applyLeftIdentity(cat1, cat2);
        break;
      case rp:
        Preconditions.checkArgument(children.size() == 2,
            "Cannot apply right punctuation rules");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        result = Category.applyRightIdentity(cat1, cat2);
        break;
      case lex:
        Preconditions.checkArgument(children.size() == 1,
            "Cannot apply unary rules");
        cat1 = children.get(0).currentCategory;
        String inputSynCatString = cat1.getSyntacticCategory().toSimpleString();
        SyntacticCategory resultantantSynCatSimple =
            SyntacticCategory.fromString(resultantSynCatString);
        String resultantSynCatStringSimple =
            resultantantSynCatSimple.toSimpleString();
        String unaryRule =
            autoLexicon.selectUnaryRule(inputSynCatString,
                resultantSynCatStringSimple);
        if (unaryRule == null || unaryRule.equals("")) {
          inputSynCatString = cat1.getSyntacticCategory().toSuperSimpleString();
          resultantSynCatStringSimple =
              resultantantSynCatSimple.toSuperSimpleString();
          unaryRule =
              autoLexicon.selectUnaryRule(inputSynCatString,
                  resultantSynCatStringSimple);
        }
        // No unary rule found
        if (unaryRule == null || unaryRule.equals("")) {
          resultantSynCatStringSimple =
              resultantantSynCatSimple.toSimpleString();
          String indexCat =
              autoLexicon
                  .getIndexedSyntacticCategory(resultantSynCatStringSimple);
          if (indexCat == null) {
            resultantSynCatStringSimple =
                resultantantSynCatSimple.toSuperSimpleString();
            indexCat =
                autoLexicon
                    .getIndexedSyntacticCategory(resultantSynCatStringSimple);
          }
          if (indexCat == null) {
            indexCat = resultantantSynCatSimple.toSimpleString();
          }
          unaryRule = inputSynCatString + "\t" + indexCat;
        }
        Preconditions.checkArgument(unaryRule != null && !unaryRule.equals(""),
            "Bad unary rule");
        result = Category.applyUnaryRule(cat1, unaryRule);
        break;
      case ltc:
      case rtc:
      case other:
        if (children.size() != 2)
          throw new FunnyCombinatorException(
              "candc funny combinators. Do not use this parse");
        // Preconditions.checkArgument(children.size() == 2,
        // "Cannot apply binary rules");
        cat1 = children.get(0).currentCategory;
        cat2 = children.get(1).currentCategory;
        String synCat1String = cat1.getSyntacticCategory().toSimpleString();
        String synCat2String = cat2.getSyntacticCategory().toSimpleString();
        resultantantSynCatSimple =
            SyntacticCategory.fromString(resultantSynCatString);
        resultantSynCatStringSimple = resultantantSynCatSimple.toSimpleString();
        String binaryRule =
            autoLexicon.selectBinaryRule(synCat1String, synCat2String,
                resultantSynCatStringSimple);
        if (binaryRule == null || binaryRule.equals("")) {
          synCat1String = cat1.getSyntacticCategory().toSuperSimpleString();
          synCat2String = cat2.getSyntacticCategory().toSuperSimpleString();
          resultantSynCatStringSimple =
              resultantantSynCatSimple.toSuperSimpleString();
          binaryRule =
              autoLexicon.selectBinaryRule(synCat1String, synCat2String,
                  resultantSynCatStringSimple);
        }

        // No binary rule found
        if (binaryRule == null || binaryRule.equals("")) {
          resultantSynCatStringSimple =
              resultantantSynCatSimple.toSimpleString();
          String indexCat =
              autoLexicon
                  .getIndexedSyntacticCategory(resultantSynCatStringSimple);
          if (indexCat == null) {
            resultantSynCatStringSimple =
                resultantantSynCatSimple.toSuperSimpleString();
            indexCat =
                autoLexicon
                    .getIndexedSyntacticCategory(resultantSynCatStringSimple);
          }
          if (indexCat == null) {
            indexCat = resultantantSynCatSimple.toSimpleString();
          }
          binaryRule = synCat1String + "\t" + synCat2String + "\t" + indexCat;
        }

        if (binaryRule == null || binaryRule.equals(""))
          throw new FunnyCombinatorException(
              "candc funny combinators. Do not use this parse");
        // Preconditions.checkArgument(binaryRule != null &&
        // !binaryRule.equals(""), "Bad binary rule");
        result = Category.applyBinaryRule(cat1, cat2, binaryRule);
        break;
      default:
        throw new FunnyCombinatorException(
            "candc funny combinators. Do not use this parse");
    }
    return result;
  }

  /**
   * Returns semantic parses of a the current ccg parse. For sentences with
   * integers, multiple parses may be given containing the predicate COUNT
   * 
   * @return
   */
  public Set<Set<String>> getLexicalisedSemanticPredicates() {
    return getLexicalisedSemanticPredicates(false);
  }

  /**
   * Returns semantic parses of a the current ccg parse
   * 
   * @param handleNumbers if set true, for sentences containing integers,
   *        multiple parses may be given containing the predicate COUNT
   * @return
   */
  public Set<Set<String>> getLexicalisedSemanticPredicates(boolean handleNumbers) {
    SemanticCategory semCat = currentCategory.getSemanticCategory();
    Set<Set<String>> parses = Sets.newHashSet();

    List<String> contentRelations = semCat.getContentRelations();
    Set<String> actualRelations = Sets.newHashSet();
    processCopulaRelations(contentRelations);
    for (String contentRel : contentRelations) {
      List<String> lexicalisedRelation = lexicaliseRelation(contentRel);
      actualRelations.addAll(lexicalisedRelation);
    }
    parses.add(actualRelations);

    if (!handleNumbers)
      return parses;

    Pattern integerPattern =
        Pattern
            .compile("((^[0-9][0-9,]*$)|(^one$)|(^two$)|(^three$)|(^four$)|(^five$)|(^six$)|(^seven$)|(^eight$)|(^nine$)|(^ten$)|(^how-many$))");

    BiMap<Integer, Integer> nodeToIntegerNode = HashBiMap.create();
    for (LexicalItem leaf : leaves) {
      LexicalItem lexicalNode = leaf.copula;
      if (lexicalNode.wordPosition != leaf.wordPosition
          && cardinalPosTags.contains(lexicalNode.pos)
          && !dateNerTags.contains(lexicalNode.neType)
          && integerPattern.matcher(lexicalNode.lemma).matches()) {
        // the leaf is a number
        if (!nodeToIntegerNode.containsKey(leaf.wordPosition)
            && !nodeToIntegerNode.inverse().containsKey(
                lexicalNode.wordPosition))
          nodeToIntegerNode.put(leaf.wordPosition, lexicalNode.wordPosition);
      }
    }

    Set<Integer> integerIndexes = nodeToIntegerNode.inverse().keySet();
    Set<Set<String>> currentSemanticParses = Sets.newHashSet(parses);

    Pattern relationPattern =
        Pattern.compile("(.*)\\((.*\\:e) , ([0-9]+)\\:.*\\)");
    Pattern typePattern = Pattern.compile("(.*)\\((.*\\:s) , ([0-9]+)\\:.*\\)");

    while (currentSemanticParses.size() > 0) {
      Set<Set<String>> newParses = Sets.newHashSet(currentSemanticParses);
      for (Integer integerIndex : integerIndexes) {
        LexicalItem node =
            leaves.get(nodeToIntegerNode.inverse().get(integerIndex));
        LexicalItem integerNode = leaves.get(integerIndex);

        for (Set<String> currentSemanticParsePredicates : currentSemanticParses) {
          boolean isValidTransormation = false;
          Set<String> newPredicates = Sets.newHashSet();
          for (String predicate : currentSemanticParsePredicates) {
            Matcher matcher = relationPattern.matcher(predicate);
            String newPredicate = predicate;
            if (matcher.find() && !matcher.group(1).equals("COUNT")
                && Integer.valueOf(matcher.group(3)).equals(integerIndex)) {
              newPredicate =
                  String.format("%s(%s , %d:x)", matcher.group(1),
                      matcher.group(2), node.wordPosition);
            } else {
              matcher = typePattern.matcher(predicate);
              if (matcher.find() && !matcher.group(1).equals("COUNT")
                  && Integer.valueOf(matcher.group(3)).equals(integerIndex)) {
                if (!SemanticCategoryType.types.contains(matcher.group(1))) {
                  isValidTransormation = true;
                }
                newPredicate =
                    String.format("%s(%s , %d:x)", matcher.group(1),
                        matcher.group(2), node.wordPosition);
              }
            }
            newPredicates.add(newPredicate);
          }
          if (isValidTransormation) {
            String relation =
                String.format("COUNT(%d:x , %d:%s)", node.getWordPosition(),
                    integerIndex, integerNode.getMid());
            newPredicates.add(relation);
            newParses.add(newPredicates);
            parses.add(newPredicates);
          }
        }
      }
      newParses.removeAll(currentSemanticParses);
      currentSemanticParses = newParses;
    }

    return parses;

  }

  private void processCopulaRelations(List<String> contentRelations) {
    for (String relation : contentRelations) {
      List<String> relationParts =
          Lists.newArrayList(Splitter.on(CharMatcher.anyOf(")( "))
              .trimResults().omitEmptyStrings().split(relation));
      if (relationParts.size() < 2)
        continue;
      String semanticTypeString = relationParts.get(0);
      SemanticCategoryType semanticType =
          SemanticCategoryType.valueOf(semanticTypeString);
      if (semanticType == SemanticCategoryType.COPULA) {

        String headVar = relationParts.get(1);
        String lexicalVar = relationParts.get(2);

        CategoryIndex headIndex = CategoryIndex.getCategoryIndex(headVar);
        CategoryIndex lexicalIndex = CategoryIndex.getCategoryIndex(lexicalVar);

        if (headIndex == null || lexicalIndex == null)
          continue;

        if (headIndex.isCC()) {
          for (CategoryIndex headIndexCC : headIndex.getCCvars()) {
            if (headIndexCC.getVariableValue() == null
                || lexicalIndex.getVariableValue() == null)
              continue;
            if (!headIndexCC.getVariableValue().isInitialised()
                || !lexicalIndex.getVariableValue().isInitialised())
              continue;
            LexicalItem headNode =
                nodesIndexMap.get(headIndexCC.getVariableValue().getValue());
            LexicalItem lexicalNode =
                nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());
            headNode.copula = lexicalNode;
          }

        } else {
          if (headIndex.getVariableValue() == null
              || lexicalIndex.getVariableValue() == null)
            continue;
          if (!headIndex.getVariableValue().isInitialised()
              || !lexicalIndex.getVariableValue().isInitialised())
            continue;
          LexicalItem headNode =
              nodesIndexMap.get(headIndex.getVariableValue().getValue());
          LexicalItem lexicalNode =
              nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());
          // System.out.println(lexicalNode);
          if (lexicalPosTags.contains(lexicalNode.pos)
              || lexicalNode.isEntity())
            headNode.copula = lexicalNode;
          else
            lexicalNode.copula = headNode;
        }
      }
    }
  }

  private List<String> lexicaliseRelation(String relation) {
    List<String> relationParts =
        Lists.newArrayList(Splitter.on(CharMatcher.anyOf(")( ")).trimResults()
            .omitEmptyStrings().split(relation));

    List<String> lexicalisedRelation = Lists.newArrayList();
    if (relationParts.size() < 2)
      return lexicalisedRelation;
    String semanticTypeString = relationParts.get(0);
    SemanticCategoryType semanticType =
        SemanticCategoryType.valueOf(semanticTypeString);

    switch (semanticType) {
      case EVENT:
        String headVar = relationParts.get(1);
        String lexicalVar = relationParts.get(2);
        String relationName = relationParts.get(3);
        String childVar = relationParts.get(4);
        String argType = relationParts.get(5);

        CategoryIndex headIndexMain = CategoryIndex.getCategoryIndex(headVar);
        CategoryIndex lexicalIndexMain =
            CategoryIndex.getCategoryIndex(lexicalVar);
        CategoryIndex childIndexMain = CategoryIndex.getCategoryIndex(childVar);

        if (headIndexMain == null || lexicalIndexMain == null
            || childIndexMain == null)
          return lexicalisedRelation;

        Set<CategoryIndex> headVars = Sets.newHashSet(headIndexMain);
        Set<CategoryIndex> lexicalVars = Sets.newHashSet(lexicalIndexMain);
        Set<CategoryIndex> childVars = Sets.newHashSet(childIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        if (lexicalIndexMain.isCC()) {
          lexicalVars = lexicalIndexMain.getCCvars();
        }

        if (childIndexMain.isCC()) {
          childVars = childIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          for (CategoryIndex lexicalIndex : lexicalVars) {
            for (CategoryIndex childIndex : childVars) {
              if (headIndex.getVariableValue() == null
                  || lexicalIndex.getVariableValue() == null
                  || childIndex.getVariableValue() == null)
                continue;
              if (!headIndex.getVariableValue().isInitialised()
                  || !lexicalIndex.getVariableValue().isInitialised()
                  || !childIndex.getVariableValue().isInitialised())
                continue;
              LexicalItem headNode =
                  nodesIndexMap.get(headIndex.getVariableValue().getValue());
              LexicalItem lexicalNode =
                  nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());
              LexicalItem childNode =
                  nodesIndexMap.get(childIndex.getVariableValue().getValue());

              if (IGNOREPRONOUNS
                  && (CcgAutoLexicon.pronounPosTags.contains(headNode.pos) || CcgAutoLexicon.pronounPosTags
                      .contains(lexicalNode.pos)))
                continue;

              StringBuilder sb = new StringBuilder();
              if (!lexicalPosTags.contains(headNode.pos)
                  && headNode != lexicalNode && !headNode.isEntity()) {
                sb.append(headNode.lexicaliseRelationName());
                sb.append(".");
              }
              sb.append(lexicalNode.lexicaliseRelationName());
              sb.append(".");
              sb.append(relationName);
              if (RELATION_TYPING_IDENTIFIERS.length > 0
                  && argType.equals("ENTITY")) {
                sb.append(".");
                sb.append(childNode.copula.getRelTypingIdentifier());
              }
              sb.append("(");
              sb.append(headNode.wordPosition + ":" + "e");
              sb.append(" , ");
              if (argType.equals("ENTITY")) {
                if (lexicalPosTags.contains(childNode.copula.pos)
                    || childNode.isEntity())
                  sb.append(childNode.copula.lexicaliseArgument());
                else
                  sb.append(childNode.copula.wordPosition + ":" + "x");
              } else if (argType.equals("EVENT")) {
                sb.append(childNode.wordPosition + ":" + "e");
              }
              sb.append(")");
              lexicalisedRelation.add(sb.toString());
            }
          }
        }
        break;
      case EVENTMOD:
        headVar = relationParts.get(1);
        lexicalVar = relationParts.get(2);
        relationName = relationParts.get(3);

        headIndexMain = CategoryIndex.getCategoryIndex(headVar);
        lexicalIndexMain = CategoryIndex.getCategoryIndex(lexicalVar);

        if (headIndexMain == null || lexicalIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);
        lexicalVars = Sets.newHashSet(lexicalIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        if (lexicalIndexMain.isCC()) {
          lexicalVars = lexicalIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          for (CategoryIndex lexicalIndex : lexicalVars) {
            if (headIndex.getVariableValue() == null
                || lexicalIndex.getVariableValue() == null)
              continue;

            if (!headIndex.getVariableValue().isInitialised()
                || !lexicalIndex.getVariableValue().isInitialised())
              continue;

            LexicalItem headNode =
                nodesIndexMap.get(headIndex.getVariableValue().getValue());
            LexicalItem lexicalNode =
                nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());

            if (IGNOREPRONOUNS
                && (CcgAutoLexicon.pronounPosTags.contains(headNode.pos) || CcgAutoLexicon.pronounPosTags
                    .contains(lexicalNode.pos)))
              continue;

            if (headNode.equals(lexicalNode))
              continue;

            StringBuilder sb = new StringBuilder();
            if (!lexicalPosTags.contains(headNode.pos)
                && headNode != lexicalNode && !headNode.isEntity()) {
              sb.append(headNode.lexicaliseRelationName());
              sb.append(".");
            }
            sb.append(lexicalNode.lexicaliseRelationName());
            // Uncomment if argument label is important
            // sb.append(".");
            // sb.append(relationName);
            sb.append("(");
            sb.append(lexicalNode.wordPosition + ":s");
            sb.append(" , ");
            sb.append(headNode.wordPosition + ":" + "e");
            sb.append(")");
            lexicalisedRelation.add(sb.toString());
          }
        }
        break;
      case NEGATION:
        headVar = relationParts.get(1);

        headIndexMain = CategoryIndex.getCategoryIndex(headVar);

        if (headIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          if (headIndex.getVariableValue() == null)
            continue;

          if (!headIndex.getVariableValue().isInitialised())
            continue;
          LexicalItem headNode =
              nodesIndexMap.get(headIndex.getVariableValue().getValue());

          StringBuilder sb = new StringBuilder();
          sb.append("NEGATION");
          sb.append("(");
          sb.append(headNode.wordPosition + ":" + "e");
          sb.append(")");
          lexicalisedRelation.add(sb.toString());

          if (CcgAutoLexicon.typePosTags.contains(headNode.pos)) {
            lexicalisedRelation.add(String.format("NEGATION(%d:s)",
                headNode.wordPosition));
          }
        }
        break;
      case TYPE:
        headVar = relationParts.get(1);
        childVar = relationParts.get(2);

        headIndexMain = CategoryIndex.getCategoryIndex(headVar);
        childIndexMain = CategoryIndex.getCategoryIndex(childVar);

        if (headIndexMain == null || childIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);
        childVars = Sets.newHashSet(childIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        if (childIndexMain.isCC()) {
          childVars = childIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          for (CategoryIndex childIndex : childVars) {
            if (headIndex.getVariableValue() == null
                || childIndex.getVariableValue() == null)
              continue;

            if (!headIndex.getVariableValue().isInitialised()
                || !childIndex.getVariableValue().isInitialised())
              continue;
            LexicalItem headNode =
                nodesIndexMap.get(headIndex.getVariableValue().getValue());
            LexicalItem childNode =
                nodesIndexMap.get(childIndex.getVariableValue().getValue());
            childNode = childNode.copula;



            if (IGNOREPRONOUNS
                && (CcgAutoLexicon.pronounPosTags.contains(headNode.pos)))
              continue;

            StringBuilder sb = new StringBuilder();
            if (!lexicalPosTags.contains(headNode.pos) && !headNode.isEntity()) {
              sb.append(headNode.lexicaliseRelationName());
            } else {
              break;
            }

            if (RELATION_TYPING_IDENTIFIERS.length > 0) {
              sb.append(".");
              sb.append(childNode.getRelTypingIdentifier());
            }

            sb.append("(");

            sb.append(headNode.wordPosition + ":s");
            sb.append(" , ");

            if (lexicalPosTags.contains(childNode.pos) || childNode.isEntity()) {
              sb.append(childNode.lexicaliseArgument());
            } else {
              sb.append(childNode.wordPosition + ":" + "x");
            }
            sb.append(")");
            lexicalisedRelation.add(sb.toString());
          }
        }
        break;
      case TYPEMOD:
        // (typemod head current relationName argument) =
        // head.current.relationName(argument)
        headVar = relationParts.get(1);
        lexicalVar = relationParts.get(2);
        relationName = relationParts.get(3);
        childVar = relationParts.get(4);

        headIndexMain = CategoryIndex.getCategoryIndex(headVar);
        lexicalIndexMain = CategoryIndex.getCategoryIndex(lexicalVar);
        childIndexMain = CategoryIndex.getCategoryIndex(childVar);

        if (headIndexMain == null || lexicalIndexMain == null
            || childIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);
        lexicalVars = Sets.newHashSet(lexicalIndexMain);
        childVars = Sets.newHashSet(childIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        if (lexicalIndexMain.isCC()) {
          lexicalVars = lexicalIndexMain.getCCvars();
        }

        if (childIndexMain.isCC()) {
          childVars = childIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          for (CategoryIndex lexicalIndex : lexicalVars) {
            for (CategoryIndex childIndex : childVars) {
              if (headIndex.getVariableValue() == null
                  || lexicalIndex.getVariableValue() == null
                  || childIndex.getVariableValue() == null)
                continue;

              if (!headIndex.getVariableValue().isInitialised()
                  || !lexicalIndex.getVariableValue().isInitialised()
                  || !childIndex.getVariableValue().isInitialised())
                continue;
              LexicalItem headNode =
                  nodesIndexMap.get(headIndex.getVariableValue().getValue());
              LexicalItem lexicalNode =
                  nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());
              LexicalItem childNode =
                  nodesIndexMap.get(childIndex.getVariableValue().getValue());
              childNode = childNode.copula;

              if (IGNOREPRONOUNS
                  && (CcgAutoLexicon.pronounPosTags.contains(headNode.pos) || CcgAutoLexicon.pronounPosTags
                      .contains(lexicalNode.pos)))
                continue;

              // if (headNode.equals(childNode))
              // return "";

              StringBuilder sb = new StringBuilder();
              if (!lexicalPosTags.contains(headNode.pos)
                  && headNode != lexicalNode && !headNode.isEntity()) {
                sb.append(headNode.lexicaliseRelationName());
                sb.append(".");
              }
              sb.append(lexicalNode.lexicaliseRelationName());

              // Uncomment below line if argument numbers are important
              // sb.append(".");
              // sb.append(relationName);

              if (RELATION_TYPING_IDENTIFIERS.length > 0) {
                sb.append(".");
                sb.append(childNode.getRelTypingIdentifier());
              }

              sb.append("(");

              sb.append(lexicalNode.wordPosition + ":s");
              sb.append(" , ");

              if (lexicalPosTags.contains(childNode.pos)
                  || childNode.isEntity()) {
                sb.append(childNode.lexicaliseArgument());
              } else {
                sb.append(childNode.wordPosition + ":" + "x");
              }
              sb.append(")");
              lexicalisedRelation.add(sb.toString());
            }
          }
        }
        break;
      case COUNT:
        // (typemod head current relationName argument) =
        // head.current.relationName(argument)
        headVar = relationParts.get(1);
        lexicalVar = relationParts.get(2);

        headIndexMain = CategoryIndex.getCategoryIndex(headVar);
        lexicalIndexMain = CategoryIndex.getCategoryIndex(lexicalVar);

        if (headIndexMain == null || lexicalIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);
        lexicalVars = Sets.newHashSet(lexicalIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        if (lexicalIndexMain.isCC()) {
          lexicalVars = lexicalIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          for (CategoryIndex lexicalIndex : lexicalVars) {

            if (headIndex.getVariableValue() == null
                || lexicalIndex.getVariableValue() == null)
              continue;

            if (!headIndex.getVariableValue().isInitialised()
                || !lexicalIndex.getVariableValue().isInitialised())
              continue;
            LexicalItem headNode =
                nodesIndexMap.get(headIndex.getVariableValue().getValue());
            LexicalItem lexicalNode =
                nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());

            StringBuilder sb = new StringBuilder();
            sb.append("COUNT");

            sb.append("(");
            sb.append(headNode.wordPosition + ":x");
            sb.append(" , ");
            sb.append(lexicalNode.wordPosition + ":" + "x");
            sb.append(")");
            lexicalisedRelation.add(sb.toString());
          }
        }
        break;
      case COMPLEMENT:
        // (complement head)
        headVar = relationParts.get(1);
        // lexicalVar = relationParts.get(2);

        headIndexMain = CategoryIndex.getCategoryIndex(headVar);
        // lexicalIndexMain = CategoryIndex.getCategoryIndex(lexicalVar);

        // if (headIndexMain == null || lexicalIndexMain == null)
        if (headIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);
        // lexicalVars = Sets.newHashSet(lexicalIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        /*-if (lexicalIndexMain.isCC()) {
        	lexicalVars = lexicalIndexMain.getCCvars();
        }*/

        for (CategoryIndex headIndex : headVars) {
          // for (CategoryIndex lexicalIndex : lexicalVars) {
          // if (headIndex.getVariableValue() == null ||
          // lexicalIndex.getVariableValue() == null)
          if (headIndex.getVariableValue() == null)
            continue;

          // if (!headIndex.getVariableValue().isInitialised() ||
          // !lexicalIndex.getVariableValue().isInitialised())
          if (!headIndex.getVariableValue().isInitialised())
            continue;
          LexicalItem headNode =
              nodesIndexMap.get(headIndex.getVariableValue().getValue());
          // LexicalItem lexicalNode =
          // nodesIndexMap.get(lexicalIndex.getVariableValue().getValue());

          // if (IGNOREPRONOUNS &&
          // (CcgAutoLexicon.pronounPosTags.contains(headNode.pos) ||
          // CcgAutoLexicon.pronounPosTags.contains(lexicalNode.pos)))
          // continue;

          // if (headNode.equals(childNode))
          // return "";
          StringBuilder sb = new StringBuilder();
          sb.append("COMPLEMENT");
          sb.append("(");
          sb.append(headNode.wordPosition + ":" + "s");
          // sb.append(" , ");
          // sb.append(lexicalNode.wordPosition + ":" + "x");
          sb.append(")");
          lexicalisedRelation.add(sb.toString());
          // }
        }
        break;

      case UNIQUE:
        // (unique head)
        headVar = relationParts.get(1);
        headIndexMain = CategoryIndex.getCategoryIndex(headVar);

        if (headIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          if (headIndex.getVariableValue() == null)
            continue;
          if (!headIndex.getVariableValue().isInitialised())
            continue;
          LexicalItem headNode =
              nodesIndexMap.get(headIndex.getVariableValue().getValue());
          headNode = headNode.copula;

          // if (IGNOREPRONOUNS &&
          // (CcgAutoLexicon.pronounPosTags.contains(headNode.pos)))
          // continue;

          // if (headNode.equals(childNode))
          // return "";

          StringBuilder sb = new StringBuilder();
          sb.append("UNIQUE");
          sb.append("(");
          if (lexicalPosTags.contains(headNode.pos) || headNode.isEntity()) {
            sb.append(headNode.lexicaliseArgument());
          } else {
            sb.append(headNode.wordPosition + ":" + "x");
          }
          sb.append(")");
          lexicalisedRelation.add(sb.toString());
        }
        break;
      case QUESTION:
        // (unique head)
        headVar = relationParts.get(1);
        headIndexMain = CategoryIndex.getCategoryIndex(headVar);

        if (headIndexMain == null)
          return lexicalisedRelation;

        headVars = Sets.newHashSet(headIndexMain);

        if (headIndexMain.isCC()) {
          headVars = headIndexMain.getCCvars();
        }

        for (CategoryIndex headIndex : headVars) {
          if (headIndex.getVariableValue() == null)
            continue;
          if (!headIndex.getVariableValue().isInitialised())
            continue;
          LexicalItem headNode =
              nodesIndexMap.get(headIndex.getVariableValue().getValue());
          headNode = headNode.copula;

          // if (IGNOREPRONOUNS &&
          // (CcgAutoLexicon.pronounPosTags.contains(headNode.pos)))
          // continue;

          // if (headNode.equals(childNode))
          // return "";

          StringBuilder sb = new StringBuilder();
          sb.append("QUESTION");
          sb.append("(");
          if (lexicalPosTags.contains(headNode.pos) || headNode.isEntity()) {
            sb.append(headNode.lexicaliseArgument());
          } else {
            sb.append(headNode.wordPosition + ":" + "x");
          }
          sb.append(")");
          lexicalisedRelation.add(sb.toString());
        }
        break;

      default:
        break;
    }
    return lexicalisedRelation;
  }

  public void freeCache() {
    // List<LexicalItem> leaves = getLeafNodes();
    for (LexicalItem leaf : leaves)
      nodesIndexMap.remove(leaf.key);
  }

  public static synchronized void resetCounter() {
    nodesIndexMap = Maps.newConcurrentMap();
    nodeCount = 1;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(currentCategory).toString();
  }

  /**
   * An exception to indicate too many semantic parse possibilities.
   *
   */
  public static class TooManyParsesException extends Exception {
    private static final long serialVersionUID = 1L;

    public TooManyParsesException() {
      super();
    }

    public TooManyParsesException(String message) {
      super(message);
    }
  }

}
