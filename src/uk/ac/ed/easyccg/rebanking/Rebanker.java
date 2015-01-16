package uk.ac.ed.easyccg.rebanking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import uk.ac.ed.easyccg.syntax.Category;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode;
import uk.ac.ed.easyccg.syntax.SyntaxTreeNode.SyntaxTreeNodeLeaf;
import uk.ac.ed.easyccg.syntax.Util;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies;
import uk.ac.ed.easyccg.syntax.evaluation.CCGBankDependencies.DependencyParse;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

public abstract class Rebanker
{

  public static void main(String[] args) throws IOException {
    Rebanker rebanker = new SubcategorizePrepositionRebanker();
    File ccgbank = new File("/disk/data2/s1049478/CCGrebank_v1.0/data/");

    File trainingFolder = new File("/disk/data2/s1049478/my_parser/experiments/ccgbank_preps_train/");
    rebanker.processCorpus(ccgbank, true, new File("/disk/data2/s1049478/my_parser/experiments/ccgbank_preps_dev/gold.stagged"));
    rebanker.processCorpus(ccgbank, false, new File(trainingFolder, "gold.stagged"));

    rebanker.writeCatList(rebanker.allCats, new File(trainingFolder, "categories"));
  }
  
  private void writeCatList(Multiset<Category> cats, File outputFile) throws IOException {
    Multiset<Category> catsNoPPorPRfeatures = HashMultiset.create();
    for (Category cat : cats) {
      catsNoPPorPRfeatures.add(cat.dropPPandPRfeatures());
    }
    FileWriter fw = new FileWriter(outputFile.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);
    
    
    int categories = 0;
    for (Category type : Multisets.copyHighestCountFirst(cats).elementSet()) {
      if (catsNoPPorPRfeatures.count(type.dropPPandPRfeatures()) >= 10) {
        bw.write(type.toString());
        bw.newLine();
        categories++;
      }
    }
    System.out.println("Number of cats occurring 10 times: " + categories);
    
    
    bw.flush();
    bw.close();
    

  }
    
  private final static String devRegex = "wsj_00.*";
  private final static String trainRegex = "wsj_((0[2-9])|(1[0-9])|(2[0-1])).*";
  
  public void processCorpus(File folder, boolean isDev, File outputFile) throws IOException {
    
    
    String regex = isDev ? devRegex : trainRegex;
    List<File> autoFiles = Util.findAllFiles(folder, regex + ".*.auto");
    Collections.sort(autoFiles);

    List<File> pargFiles = Util.findAllFiles(folder, regex + ".*.parg");
    Collections.sort(pargFiles);
    
    if (autoFiles.size() != pargFiles.size()) {
      throw new RuntimeException("Different numbers of AUTO and PARG files found in: " + folder);
    }
    FileWriter fw = new FileWriter(outputFile.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);
    
    for (int i = 0; i < autoFiles.size(); i++) {

      File autoFile = autoFiles.get(i);
      File pargFile = pargFiles.get(i);
      
      getTrainingData(autoFile, pargFile, bw, isDev);
    }
    
    bw.flush();

    System.out.println("Errors: " + ((SubcategorizePrepositionRebanker) this).errors);
  }
  Multiset<Category> allCats = HashMultiset.create();

  private void getTrainingData(File autoFile, File pargFile, BufferedWriter bw, boolean isTest) throws IOException
  {
    Iterator<String> autoLines = Util.readFileLineByLine(autoFile);
    Iterator<String> pargLines = Util.readFileLineByLine(pargFile);
    
    while (autoLines.hasNext()) {
      SyntaxTreeNode autoParse = getParse(autoLines);
      DependencyParse depParse = CCGBankDependencies.getDependencyParseCCGBank(pargLines, autoParse.getWords());
      List<SyntaxTreeNodeLeaf> rebanked = doRebanking(autoParse, depParse);
      
      
      for (SyntaxTreeNodeLeaf leaf : rebanked) {
        if (leaf.getSentencePosition() > 0) {
          bw.write(" ");
        }
        
        if (!isTest && filter(leaf.getCategory())) {
          bw.write(leaf.getWord() + "||" +
          		"" +
          		"");
        } else {
          bw.write(leaf.getWord() + "|" + leaf.getPos() + "|" + leaf.getCategory());
          
          if (!isTest) {
            allCats.add(leaf.getCategory());
          }
        }
      }
      
      bw.newLine();
    }
  }
  
  abstract boolean filter(Category c);
  
  private SyntaxTreeNode getParse(Iterator<String> autoLines)
  {
    String line = autoLines.next();
    line = autoLines.next();
    return CCGBankParseReader.parse(line);
  }

  
  abstract List<SyntaxTreeNodeLeaf> doRebanking(SyntaxTreeNode parse, DependencyParse dependencyParse);
}
