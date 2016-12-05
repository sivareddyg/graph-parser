package in.sivareddy.scripts;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.MergeEntity;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;
import in.sivareddy.others.EasyCcgCli;
import in.sivareddy.others.StanfordPipeline;
import in.sivareddy.util.SentenceKeys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

public class RunGraphParserKunData {
  private static Map<String, String> options =
      ImmutableMap
          .of("annotators",
              "tokenize, ssplit, pos, lemma, ner",
              "pos.model",
              "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger",
              "ner.model",
              "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz,"
                  + "edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
  private static StanfordPipeline englishPipeline = new StanfordPipeline(
      options);
  private static Gson gson = new Gson();
  private static JsonParser jsonParser = new JsonParser();
  private final GroundedGraphs graphCreator;
  private final EasyCcgCli ccgParser;
  private final EasyCcgCli ccgParserQuestions;
  private final int nbestParses;
  private final Logger logger;

  public RunGraphParserKunData(Logger logger, String dataFolder, int nbestParses)
      throws ArgumentValidationException, IOException {
    this.logger = logger;
    this.nbestParses = nbestParses;

    String ccgModelDir = Paths.get(dataFolder, "easyccg_model").toString();
    ccgParser =
        new EasyCcgCli(ccgModelDir + " -r S[dcl] S[pss] S[pt] S[b] S[ng] S",
            nbestParses);

    String ccgModelDirQuestions =
        Paths.get(dataFolder, "easyccg_model_questions").toString();
    ccgParserQuestions =
        new EasyCcgCli(ccgModelDirQuestions + " -s -r S[q] S[qem] S[wq]",
            nbestParses);

    String markupFile =
        Paths.get(dataFolder, "candc_markedup.modified").toString();
    String unaryRulesFile = Paths.get(dataFolder, "unary_rules.txt").toString();
    String binaryRulesFile =
        Paths.get(dataFolder, "binary_rules.txt").toString();
    String specialCasesFile =
        Paths.get(dataFolder, "lexicon_specialCases.txt").toString();
    String specialCasesQuestionsFile =
        Paths.get(dataFolder, "lexicon_specialCases_questions_vanilla.txt")
            .toString();

    CcgAutoLexicon normalCcgAutoLexicon =
        new CcgAutoLexicon(markupFile, unaryRulesFile, binaryRulesFile,
            specialCasesFile);

    CcgAutoLexicon questionCcgAutoLexicon =
        new CcgAutoLexicon(markupFile, unaryRulesFile, binaryRulesFile,
            specialCasesQuestionsFile);

    String[] relationLexicalIdentifiers = {"lemma"};
    String[] relationTypingIdentifiers = {};

    Schema schema = null;
    KnowledgeBaseCached kb = new KnowledgeBaseCached(null, null);
    GroundedLexicon groundedLexicon = new GroundedLexicon(null);
    graphCreator = new GroundedGraphs(schema, kb, groundedLexicon,
        normalCcgAutoLexicon, questionCcgAutoLexicon,
        relationLexicalIdentifiers, relationTypingIdentifiers, null, 1, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, 10.0, 1.0, 0.0, 0.0);
  }

  public void processSentence(JsonObject sentence) {
    String sentenceString =
        sentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
    sentenceString = sentenceString.trim();
    String cleanSentenceString =
        Joiner
            .on(" ")
            .join(
                Splitter
                    .on(CharMatcher.WHITESPACE)
                    .trimResults(
                        CharMatcher.JAVA_LETTER_OR_DIGIT.or(
                            CharMatcher.anyOf(",?.")).negate())
                    .omitEmptyStrings().split(sentenceString)).toString();

    sentence.addProperty(SentenceKeys.SENTENCE_KEY, cleanSentenceString);

    // System.err.println("Hello : " + cleanSentenceString);

    englishPipeline.processSentence(sentence);
    JsonArray entities = new JsonArray();
    int i = 0;
    for (JsonElement wordElm : sentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      JsonObject word = wordElm.getAsJsonObject();
      if (word.get(SentenceKeys.WORD_KEY).getAsString().matches("(SUBJ|OBJ)")) {
        JsonObject entity = new JsonObject();
        entity.addProperty(SentenceKeys.ENTITY,
            "m." + word.get(SentenceKeys.WORD_KEY).getAsString());
        entity.addProperty(SentenceKeys.START, i);
        entity.addProperty(SentenceKeys.END, i);
        entities.add(entity);
      }
      i++;
    }
    sentence.add(SentenceKeys.ENTITIES, entities);
    sentence = MergeEntity.mergeEntityWordsToSingleWord(gson.toJson(sentence));
    sentence =
        MergeEntity.mergeNamedEntitiesToSingleWord(gson.toJson(sentence));
  }

  public List<String> getCcgParse(JsonObject sentence)
      throws ArgumentValidationException, IOException, InterruptedException {
    List<String> words = new ArrayList<>();
    for (JsonElement wordElm : sentence.get(SentenceKeys.WORDS_KEY)
        .getAsJsonArray()) {
      JsonObject wordObj = wordElm.getAsJsonObject();
      words.add(String.format("%s|%s|%s", wordObj.get(SentenceKeys.WORD_KEY)
          .getAsString(), wordObj.get(SentenceKeys.POS_KEY).getAsString(),
          wordObj.get(SentenceKeys.POS_KEY).getAsString(),
          wordObj.get(SentenceKeys.NER_KEY).getAsString()));
    }
    String processedSentence = Joiner.on(" ").join(words).toString();
    List<String> ccgParseStrings =
        ccgParserQuestions != null && processedSentence.endsWith("?|.|O") ? ccgParserQuestions
            .parse(processedSentence) : ccgParser.parse(processedSentence);
    return ccgParseStrings;
  }

  public List<LexicalGraph> getCcgUngroundedGraphs(JsonObject sentence)
      throws ArgumentValidationException, IOException, InterruptedException {
    List<String> ccgParseStrings = getCcgParse(sentence);
    List<Map<String, String>> ccgParses = new ArrayList<>();
    for (String ccgParseString : ccgParseStrings) {
      Map<String, String> ccgParseMap = new HashMap<>();
      ccgParseMap.put(SentenceKeys.CCG_PARSE, ccgParseString);
      ccgParseMap.put("score", "1.0");
      ccgParses.add(ccgParseMap);
    }
    sentence.add(SentenceKeys.CCG_PARSES,
        jsonParser.parse(gson.toJson(ccgParses)));
    GroundedGraphs.resetAllCounters();
    List<LexicalGraph> graphs =
        graphCreator.buildUngroundedGraph(sentence, "synPars", nbestParses,
            logger);
    return graphs;
  }

  public SemanticGraph getSemanticGraph(LexicalGraph ungroundedGraph) {
    return graphCreator.buildSemanticGraphFromSemanticParse(
        ungroundedGraph.getSemanticParse(), ungroundedGraph.getActualNodes());
  }

  public static void main(String[] args) throws IOException,
      ArgumentValidationException, InterruptedException {
    PatternLayout layout = new PatternLayout("%r [%t] %-5p: %m%n");
    Logger logger = Logger.getLogger(RunGraphParserKunData.class);
    logger.setLevel(Level.DEBUG);
    logger.setAdditivity(false);
    Appender stdoutAppender = new ConsoleAppender(layout);
    logger.addAppender(stdoutAppender);

    RunGraphParserKunData engine =
        new RunGraphParserKunData(logger, "lib_data", 10);

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    try {
      String line = br.readLine();
      while (line != null) {
        JsonObject sentence = jsonParser.parse(line).getAsJsonObject();
        engine.processSentence(sentence);
        logger.debug("# Original Sentence: "
            + jsonParser.parse(line).getAsJsonObject()
                .get(SentenceKeys.SENTENCE_KEY).getAsString());

        List<LexicalGraph> unGroundedGraphs =
            engine.getCcgUngroundedGraphs(sentence);

        int subjIndex = -1;
        int objIndex = -1;

        for (JsonElement entityElm : sentence.get(SentenceKeys.ENTITIES)
            .getAsJsonArray()) {
          JsonObject entity = entityElm.getAsJsonObject();
          if (entity.get(SentenceKeys.ENTITY).getAsString().equals("m.SUBJ")) {
            subjIndex = entity.get(SentenceKeys.INDEX_KEY).getAsInt();
          } else if (entity.get(SentenceKeys.ENTITY).getAsString()
              .equals("m.OBJ")) {
            objIndex = entity.get(SentenceKeys.INDEX_KEY).getAsInt();
          }
        }

        if (subjIndex < 0 || objIndex < 0) {
          logger.debug("Either SUBJ or OBJ not present");
          System.err.println(line);
        } else {
          int synParseCount = 0;
          int semparseCount = 0;
          String prevSynParse = "";
          boolean hasPath = false;
          for (LexicalGraph ungroundedGraph : unGroundedGraphs) {
            if (!prevSynParse.equals(ungroundedGraph.getSyntacticParse())) {
              prevSynParse = ungroundedGraph.getSyntacticParse();
              synParseCount++;
              semparseCount = 0;
              logger.debug("Syntactic Parse " + synParseCount + " : "
                  + ungroundedGraph.getSyntacticParse());
            }
            semparseCount++;

            logger.debug("Semantic Parse " + semparseCount + " : "
                + ungroundedGraph.getSemanticParse());
            SemanticGraph semanticGraph =
                engine.getSemanticGraph(ungroundedGraph);

            IndexedWord subjNode =
                GroundedGraphs.makeWord(ungroundedGraph.getActualNodes().get(
                    subjIndex));
            IndexedWord objNode =
                GroundedGraphs.makeWord(ungroundedGraph.getActualNodes().get(
                    objIndex));

            // Path from subj to obj.
            List<SemanticGraphEdge> edges =
                semanticGraph.getShortestUndirectedPathEdges(subjNode, objNode);

            if (edges != null) {
              List<String> relations = new ArrayList<>();
              edges.forEach(x -> relations.add(x.getRelation().getShortName()));
              logger.debug("Semantic Distance: " + relations.size());
              logger.debug("Semantic Path: " + relations);
              hasPath = true;
            } else {
              logger.debug("Semantic path could not be found!");
            }
          }
          if (hasPath) {
            logger.debug("At least one semantic path found!");
          } else {
            logger.debug("No semantic path could be found");
            System.err.println(line);
          }
        }

        logger.debug("  ");
        line = br.readLine();
      }
    } finally {
      br.close();
    }
  }
}
