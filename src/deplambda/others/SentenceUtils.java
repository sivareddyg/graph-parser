package deplambda.others;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SentenceUtils {

  public static JsonParser jsonParser = new JsonParser();

  public static void loadSentences(InputStream stream,
      List<JsonObject> jsonSentences) throws IOException {
    Preconditions.checkNotNull(jsonSentences);

    BufferedReader br =
        new BufferedReader(new InputStreamReader(stream, "UTF-8"));
    try {
      String line = br.readLine();
      while (line != null) {
        line = line.trim();
        if (line.equals("") || line.charAt(0) == '#') {
          line = br.readLine();
          continue;
        }
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        jsonSentences.add(sentence);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }

  public static InputStream getInputStream(String inputFile)
      throws FileNotFoundException, IOException {
    if (inputFile.endsWith(".gz")) {
      return new GZIPInputStream(new FileInputStream(inputFile));
    } else if (inputFile.equals("stdin")) {
      return System.in;
    } else {
      return new FileInputStream(inputFile);
    }
  }
}
