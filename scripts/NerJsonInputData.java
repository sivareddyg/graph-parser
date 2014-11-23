import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

/**
 * This is a demo of calling CRFClassifier programmatically.
 * <p>
 * Usage:
 * <code> java -mx400m -cp "stanford-ner.jar:." NERDemo [serializedClassifier [fileName]]</code>
 * <p>
 * If arguments aren't specified, they default to
 * ner-eng-ie.crf-3-all2006.ser.gz and some hardcoded sample text.
 * <p>
 * To use CRFClassifier from the command line: java -mx400m
 * edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier [classifier] -textFile
 * [file] Or if the file is already tokenized and one word per line, perhaps in
 * a tab-separated value format with extra columns for part-of-speech tag, etc.,
 * use the version below (note the 's' instead of the 'x'): java -mx400m
 * edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier [classifier] -testFile
 * [file]
 * 
 * @author Jenny Finkel
 * @author Christopher Manning
 */

public class NerJsonInputData {

	public static void main(String[] args) throws IOException {

		String serializedClassifier = "./tools/stanford-ner-2012-11-11/classifiers/english.muc.7class.distsim.crf.ser.gz";
		// System.out.println(Lists.newArrayList(Splitter.on(CharMatcher.WHITESPACE).split("Book/O them/O now/O from/O Cosmos/ORGANIZATION Agency/ORGANIZATION on/O UK/O 0/O 131 558 3146/O ./O")));

		if (args.length > 0) {
			serializedClassifier = args[0];
		}

		AbstractSequenceClassifier<CoreLabel> classifier = CRFClassifier
				.getClassifierNoExceptions(serializedClassifier);

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));

			String input;

			Gson gson = new Gson();
			JsonParser parser = new JsonParser();
			while ((input = br.readLine()) != null) {
				JsonElement jelement = parser.parse(input);
				JsonObject jobject = jelement.getAsJsonObject();
				JsonArray words = jobject.getAsJsonArray("words");
				List<JsonObject> wordObjects = Lists.newArrayList();
				List<String> wordStrings = Lists.newArrayList();

				for (JsonElement word : words) {
					JsonObject wordObject = word.getAsJsonObject();
					String wordString = gson.fromJson(wordObject.get("word"),
							String.class);
					wordObjects.add(wordObject);
					wordStrings.add(wordString);
				}

				String sent = Joiner.on(" ").join(wordStrings);
				// System.out.println(sent);

				String sentNer = classifier.classifyToString(sent);
				// System.out.println(sentNer);

				int i = 0;
				for (String nerWord : Splitter.on(CharMatcher.WHITESPACE).split(sentNer)) {
					// System.out.println(nerWord);
					ArrayList<String> newWordSplit = Lists
							.newArrayList(Splitter.on("/").split(nerWord));
					String nerTag = "O";
					if (newWordSplit.size() == 2)
						nerTag = newWordSplit.get(newWordSplit.size() - 1);
					JsonObject wordObject = wordObjects.get(i);
					wordObject.addProperty("ner", nerTag);
					// System.out.println(wordObject);
					i += 1;
				}
				System.out.println(gson.toJson(jelement));
			}
		} catch (IOException io) {
			io.printStackTrace();
		}
	}
}
