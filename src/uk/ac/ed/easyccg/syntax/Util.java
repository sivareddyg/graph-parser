package uk.ac.ed.easyccg.syntax;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class Util
{
  public static int indexOfAny(String haystack, String needles) {
    for (int i=0; i<haystack.length(); i++) {
      for (int j=0; j<needles.length(); j++) {
        if (haystack.charAt(i) == needles.charAt(j)) {
          return i;
        }
      }  
    }
    
    return -1;
  }

  public static Iterable<String> readFile(final File filePath)
  throws java.io.IOException
  {
    return new Iterable<String>()    
    {

      public Iterator<String> iterator() {
        
      try
      {
        return readFileLineByLine(filePath);
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
    };
      
  }
  
  public static String executeCommand(String command) throws IOException
  {
      Process process = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
      
      BufferedReader input =
        new BufferedReader
          (new InputStreamReader(process.getInputStream()));

      StringBuilder output = new StringBuilder();
      String line;
      while ((line = input.readLine()) != null) {
        output.append(line);
        output.append("\n");
      }
      input.close();
      
      return output.toString();      
    
  }
  
  public static void writeStringToFile(String text, File filePath)
  throws java.io.IOException{
      BufferedWriter out = new BufferedWriter(new FileWriter(filePath));
      out.write(text);
      out.close();
  }

  public static Iterator<String> readFileLineByLine(final File filePath)
  throws java.io.IOException
  {
    return new Iterator<String>()
    {

      // Open the file that is the first 
      // command line parameter
      InputStream fstream = new FileInputStream(filePath);
      {
        if (filePath.getName().endsWith(".gz")) {
          // Automatically unzip zipped files.
          fstream = new GZIPInputStream(fstream);
        }
      }
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      String next = br.readLine();

      @Override
      public boolean hasNext()
      {
        
        boolean result = (next != null);
        if(!result) {
          try
          {
            br.close();
          }
          catch (IOException e)
          {
            throw new RuntimeException(e);         
          }
        }
        
        return result;
      }

      @Override
      public String next()
      {
        String result = next;
        try
        {
          next = br.readLine();
        }
        catch (IOException e)
        {
          throw new RuntimeException(e);
        }
        return result;
      }

      @Override
      public void remove()
      {
        throw new UnsupportedOperationException();
      }
    };
  }

  static String dropBrackets(String cat) {
    if (cat.startsWith("(") && cat.endsWith(")") && findClosingBracket(cat) == cat.length() - 1) {
      return cat.substring(1, cat.length() - 1);
    } else {
      return cat; 
    }      
  }

  public static int findClosingBracket(String source)
  {
    return findClosingBracket(source, 0);
  }

  public static int findClosingBracket(String source, int startIndex)
  {
    int openBrackets = 0;
    for (int i = startIndex; i < source.length(); i++)
    {
      if (source.charAt(i) == '(')
      {
        openBrackets++;
      }
      else if (source.charAt(i) == ')')
      {
        openBrackets--;
      }
  
      if (openBrackets==0)
      {
        return i;
      }
    }
    
    throw new Error("Mismatched brackets in string: " + source);
  }

  /**
   * Finds the first index of a needle character in the haystack, that is not nested in brackets.
   */
  public static int findNonNestedChar(String haystack, String needles)
  {
    int openBrackets = 0;

    for (int i=0; i<haystack.length(); i++) {
      if (haystack.charAt(i) == '(')
      {
        openBrackets++;
      }
      else if (haystack.charAt(i) == ')')
      {
        openBrackets--;
      } else if (openBrackets == 0) {
        for (int j=0; j<needles.length(); j++) {
          if (haystack.charAt(i) == needles.charAt(j)) {
            return i;
          }
        }  
      }
    }
     
    return -1;
  }
  
  public static List<File> findAllFiles(File folder, final String regex) {
    return Util.findAllFiles(folder,
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
              return name.matches(regex);
          }
      });    
  }
  
  public static List<File> findAllFiles(File folder, FilenameFilter filter) {
    List<File> result = new ArrayList<File>();
    
    findAllFiles(folder, filter, result);
    return result;
  }

  private static void findAllFiles(File folder, FilenameFilter filter, List<File> result) {
    for (File file : folder.listFiles()) {
      if (file.isDirectory()) {
        findAllFiles(file, filter, result);
      } else if (filter.accept(file, file.getName())) {
        result.add(file.getAbsoluteFile());
      }
    }
  }
}
