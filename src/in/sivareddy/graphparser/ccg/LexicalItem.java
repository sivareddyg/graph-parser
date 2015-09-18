package in.sivareddy.graphparser.ccg;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Created by bisk1 on 1/26/15.
 */
public class LexicalItem extends CcgParseTree implements
    Comparable<LexicalItem>, Serializable {
  private static final long serialVersionUID = 16874271356455052L;
  int wordPosition = -1;
  private String synCat;
  String word;
  String lemma;
  String pos;

  // named entity type
  String neType;

  // useful field to set freebase mids or any other
  private String mid;

  public String getMID() {
    return mid;
  }

  public void setMID(String mid) {
    this.mid = mid;
  }

  int key = -1;
  LexicalItem copula;

  public LexicalItem(String synCat, String word, String lemma, String pos,
      String neType, Category cat) {
    // (<L N Titanic Titanic NNP O I-NP N>)
    super();
    this.synCat = synCat;
    this.word = word;
    this.lemma = lemma.toLowerCase();
    if (lemma.equals("_blank_")) {
      this.mid = "x";
    } else {
      this.mid = this.lemma;
    }
    this.pos = pos;
    this.neType = neType;
    this.currentCategory = cat;
    key = getNodeCount();
    nodesIndexMap.put(key, this);
    if (currentCategory != null)
      currentCategory.getSyntacticCategory().getIndex().setVariableValue(key);
    copula = this;
  }

  /**
   * Copy lexical item without unifying the new index variables with the
   * original index variables in syntactic and semantic category.
   *
   * @return
   */
  public LexicalItem shallowCopy() {
    Category copyCat = null;
    if (currentCategory != null) {
      copyCat = currentCategory.shallowCopy();
    }
    LexicalItem item =
        new LexicalItem(synCat, word, lemma, pos, neType, copyCat);
    return item;
  };

  @Override
  public int hashCode() {
    // final int prime = 31;
    // int result = 1;
    // result = prime * result + wordPosition;
    /*-result = prime * result + word.hashCode();
    result = prime * result + lemma.hashCode();
    result = prime * result + pos.hashCode();
    result = prime * result + neType.hashCode();
    result = prime * result + mid.hashCode();
    result = prime * result + synCat.hashCode();*/
    return wordPosition;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!obj.getClass().equals(getClass()))
      return false;
    LexicalItem other = (LexicalItem) obj;
    // if the word positions are equal, then they are the same lexical
    // items.
    if (wordPosition == other.wordPosition)
      return true;
    return false;
  }

  public int getWordPosition() {
    return wordPosition;
  }

  public void setWordPosition(int wordPosition) {
    this.wordPosition = wordPosition;
  }

  public String getSynCat() {
    return synCat;
  }

  public void setSynCat(String synCat) {
    this.synCat = synCat;
  }

  public String getWord() {
    return word;
  }

  public void setWord(String word) {
    this.word = word;
  }

  public String getLemma() {
    return lemma;
  }

  public void setLemma(String lemma) {
    this.lemma = lemma;
  }

  public String getPos() {
    return pos;
  }

  public void setPos(String pos) {
    this.pos = pos;
  }

  public String getNeType() {
    return neType;
  }

  public void setNeType(String neType) {
    this.neType = neType;
  }

  public String getMid() {
    return mid;
  }

  public void setMid(String mid) {
    this.mid = mid;
  }

  public boolean isEntity() {
    return mid.startsWith("m.") || mid.startsWith("type.");
  }

  public boolean isStandardEntity() {
    return mid.startsWith("type.");
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(word).addValue(pos)
        .addValue(currentCategory).toString();
  }

  public String lexicaliseRelationName() {
    String result = "";
    try {
      String lexName = RELATION_IDENTIFIERS[0];
      result = this.getClass().getDeclaredField(lexName).get(this).toString();
      for (int i = 1; i < RELATION_IDENTIFIERS.length; i++) {
        lexName = RELATION_IDENTIFIERS[i];
        result +=
            ":"
                + this.getClass().getDeclaredField(lexName).get(this)
                    .toString();
      }
    } catch (NoSuchFieldException | SecurityException
        | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return result;
  }

  public String lexicaliseArgument() {
    String result = "";
    try {
      String lexName;
      result = String.valueOf(wordPosition);

      for (int i = 0; i < ARGUMENT_IDENTIFIERS.length; i++) {
        lexName = ARGUMENT_IDENTIFIERS[i];
        result +=
            ":"
                + this.getClass().getDeclaredField(lexName).get(this)
                    .toString();
      }
    } catch (NoSuchFieldException | SecurityException
        | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return result;
  }

  public String getRelTypingIdentifier() {
    String result = "";
    try {
      String lexName = RELATION_TYPING_IDENTIFIERS[0];
      result = this.getClass().getDeclaredField(lexName).get(this).toString();

      for (int i = 1; i < RELATION_TYPING_IDENTIFIERS.length; i++) {
        lexName = RELATION_TYPING_IDENTIFIERS[i];
        result +=
            ":"
                + this.getClass().getDeclaredField(lexName).get(this)
                    .toString();
      }
    } catch (NoSuchFieldException | SecurityException
        | IllegalArgumentException | IllegalAccessException e) {
      e.printStackTrace();
    }
    return result;
  }

  @Override
  public int compareTo(LexicalItem o) {
    return (new Integer(this.wordPosition)).compareTo(new Integer(
        o.wordPosition));
  }
}
