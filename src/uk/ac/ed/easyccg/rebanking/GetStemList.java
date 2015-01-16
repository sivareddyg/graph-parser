package uk.ac.ed.easyccg.rebanking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.ed.easyccg.lemmatizer.MorphaStemmer;
import uk.ac.ed.easyccg.syntax.Util;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * Makes a list of lemmas, to use in training instead of full words. Doesn't seem to help.
 */
public class GetStemList
{
  public static void main(String[] args) throws IOException {
    File embeddingsFolder = new File("/disk/data2/s1049478/my_parser/turian/");
    File trainingData = new File("/disk/data2/s1049478/my_parser/experiments/wsj_train/gold.stagged");
    
    File wordListFile = new File(embeddingsFolder, "/embeddings.words");
    File outputFile = new File(embeddingsFolder, "/lemmas");
    
    Map<String, Multiset<String>> wordToPos = new HashMap<String, Multiset<String>>();
    for (String line : Util.readFile(trainingData)) {
      if (line.isEmpty() || line.startsWith("#")) continue ;
      for (String wordEntries : line.split(" ")) {
        String[] fields = wordEntries.split("\\|");
        String word = fields[0];
        Multiset<String> posForWord = wordToPos.get(word);
        if (posForWord == null) {
          posForWord = HashMultiset.create();
          wordToPos.put(word, posForWord);
        }
        posForWord.add(fields[1]);
      }
    }
    
    FileWriter fw = new FileWriter(outputFile.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);
    
    for (String word : Util.readFile(wordListFile)) {
      Multiset<String> posCounts = wordToPos.get(word);
      if (posCounts == null) {
        System.out.println(word);
        continue ;
      }
      String lemma = MorphaStemmer.stem(word);
      
      if (!lemma.equals(word)) {
        bw.write(word + " " + lemma);
        bw.newLine();
      }
    }
    
    bw.flush();
  }
}
