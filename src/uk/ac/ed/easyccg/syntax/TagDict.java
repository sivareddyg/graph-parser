package uk.ac.ed.easyccg.syntax;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ed.easyccg.syntax.InputReader.InputToParser;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class TagDict
{

  private static final int MIN_OCCURENCES_OF_WORD = 500;
  /**
   * Key used in the tag dictionary for infrequent words
   */
  public static final String OTHER_WORDS = "*other_words*";
  private final static String fileName = "tagdict";
  
  /**
   * Saves a tag dictionary to the model folder
   */
  public static void writeTagDict(Map<String, Collection<Category>> tagDict, File modelFolder) throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter(new File(modelFolder, fileName), "UTF-8");
    for (java.util.Map.Entry<String, Collection<Category>> entry : tagDict.entrySet()) {
      writer.print(entry.getKey());
      for (Category c : entry.getValue()) {
        writer.print("\t" + c.toString());
      }
      writer.println();
    }
    
    writer.close();
  }
  
  /**
   * Loads a tag dictionary from the model folder
   */
  public static Map<String, Collection<Category>> readDict(File modelFolder) throws IOException {
    Map<String, Collection<Category>> result = new HashMap<String, Collection<Category>>();
    File file = new File(modelFolder, fileName);
    if (!file.exists()) return null;
    for (String line : Util.readFile(file)) {
      String[] fields = line.split("\t");
      List<Category> cats = new ArrayList<Category>();
      for (int i=1; i<fields.length; i++) {
        cats.add(Category.valueOf(fields[i]));        
      }
      result.put(fields[0], ImmutableSet.copyOf(cats));
    }
    
    return ImmutableMap.copyOf(result);
  }
  
  private final static Comparator<Entry<Category>> comparator = new Comparator<Entry<Category>>()
  {
    @Override
    public int compare(Entry<Category> arg0, Entry<Category> arg1)
    {
      return arg1.getCount() - arg0.getCount();
    }
  };
  
  /**
   * Finds the set of categories used for each word in a corpus
   */
  public static Map<String, Collection<Category>> makeDict(Iterable<InputToParser> input) {
    Multiset<String> wordCounts = HashMultiset.create();
    Map<String, Multiset<Category>> wordToCatToCount = new HashMap<String, Multiset<Category>>();
    
    // First, count how many times each word occurs with each category
    for (InputToParser sentence : input) {
      for (int i=0; i<sentence.getInputWords().size(); i++) {
        String word = sentence.getInputWords().get(i).word;
        Category cat = sentence.getGoldCategories().get(i);
        wordCounts.add(word);
        
        if (!wordToCatToCount.containsKey(word)) {
          Multiset<Category> tmp = HashMultiset.create();
          wordToCatToCount.put(word, tmp);
        }

        wordToCatToCount.get(word).add(cat);
      }
    }
    

    // Now, save off a sorted list of categories
    Multiset<Category> countsForOtherWords = HashMultiset.create();
    
    Map<String, Collection<Category>> result = new HashMap<String, Collection<Category>>();
    for (Entry<String> wordAndCount : wordCounts.entrySet()) {
      Multiset<Category> countForCategory = wordToCatToCount.get(wordAndCount.getElement());
      if (wordAndCount.getCount() > MIN_OCCURENCES_OF_WORD) {
        // Frequent word
        addEntryForWord(countForCategory, result, wordAndCount.getElement());
      } else {
        // Group stats for all rare words together.
        
        for (Entry<Category> catToCount : countForCategory.entrySet()) {
          countsForOtherWords.add(catToCount.getElement(), catToCount.getCount());
        }
      }
    }
    addEntryForWord(countsForOtherWords, result, OTHER_WORDS);


    return ImmutableMap.copyOf(result);
  }

  private static void addEntryForWord(Multiset<Category> countForCategory,
      Map<String, Collection<Category>> result, String word)
  {
    List<Entry<Category>> cats = new ArrayList<Entry<Category>>();
    for (Entry<Category> catToCount : countForCategory.entrySet()) {
      cats.add(catToCount);
    }
    
    Collections.sort(cats, comparator);
    List<Category> cats2 = new ArrayList<Category>();
        
    for (Entry<Category> entry : cats) {
      cats2.add(entry.getElement());
    }
    
    result.put(word, cats2);
  }
}
