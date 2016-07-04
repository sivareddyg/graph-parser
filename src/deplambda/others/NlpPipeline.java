package deplambda.others;

import in.sivareddy.util.ProcessStreamInterface;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.core.exception.MaltChainedException;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import deplambda.parser.TreeTransformerMain;
import deplambda.util.TransformationRuleGroups;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.cornell.cs.nlp.spf.mr.lambda.FlexibleTypeComparator;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;
import edu.cornell.cs.nlp.spf.mr.language.type.MutableTypeRepository;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

public class NlpPipeline extends ProcessStreamInterface {
  public static String ANNOTATORS_KEY = "annotators";
  public static String POS_ANNOTATOR = "pos";
  public static String NER_ANNOTATOR = "ner";
  public static String PARSER_ANNOTATOR = "parser";
  public static String TOKENIZE_ANNOTATOR = "tokenize";
  public static String SENTENCE_SPLIT_ANNOTATOR = "ssplit";
  public static String LEMMA_ANNOTATOR = "lemma";

  public static String LANGUAGE_KEY = "languageCode";
  public static String MALT_PARSER_KEY = "maltparser";
  public static String STANFORD_DEP_PARSER_KEY = "depparse";
  public static String DRAW_SVG_TREES = "drawSvgTrees";
  public static String SVG_ZOOM_FACTOR = "svgZoomFactor";
  public static String WHITESPACE_TOKENIZER = "tokenize.whitespace";
  public static String SENTENCE_EOL_SPLITTER = "ssplit.eolonly";
  public static String NEW_LINE_IS_SENTENCE_BREAK =
      "ssplit.newlineIsSentenceBreak";

  public static String DEPLAMBDA = "deplambda";
  public static String DEPLAMBDA_LEXICALIZE_PREDICATES =
      "deplambda.lexicalizePredicates";
  public static String DEPLAMBDA_TREE_TRANSFORMATIONS_FILE =
      "deplambda.treeTransformationsFile";
  public static String DEPLAMBDA_DEFINED_TYPES_FILE =
      "deplambda.definedTypesFile";
  public static String DEPLAMBDA_RELATION_PRORITIES_FILE =
      "deplambda.relationPrioritiesFile";
  public static String DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE =
      "deplambda.lambdaAssignmentRulesFile";
  public static String DEPLAMBDA_DEBUG = "deplambda.debug";

  private StanfordCoreNLP pipeline;
  private Map<String, String> options;
  private Set<String> annotators;
  ConcurrentMaltParserModel maltModel = null;
  RenderSVG svgRenderer = null;
  TreeTransformerMain treeTransformer = null;

  public NlpPipeline(Map<String, String> options) {
    System.err.println(options);
    this.options = options;
    
    if (options.containsKey(DEPLAMBDA)) {
      System.err.println("Loading DepLambda Model.. ");
      try {
        MutableTypeRepository types = new MutableTypeRepository(
            options.get(DEPLAMBDA_DEFINED_TYPES_FILE));
        System.err.println(String.format("%s=%s", DEPLAMBDA_DEFINED_TYPES_FILE,
            options.get(DEPLAMBDA_DEFINED_TYPES_FILE)));

        LogicLanguageServices
            .setInstance(new LogicLanguageServices.Builder(types,
                new FlexibleTypeComparator()).closeOntology(false)
                    .setNumeralTypeName("i").build());

        TransformationRuleGroups treeTransformationRules;
        treeTransformationRules = new TransformationRuleGroups(
            options.get(DEPLAMBDA_TREE_TRANSFORMATIONS_FILE));
        System.err
            .println(String.format("%s=%s", DEPLAMBDA_TREE_TRANSFORMATIONS_FILE,
                options.get(DEPLAMBDA_TREE_TRANSFORMATIONS_FILE)));

        TransformationRuleGroups relationPrioritiesRules =
            new TransformationRuleGroups(
                options.get(DEPLAMBDA_RELATION_PRORITIES_FILE));
        System.err
            .println(String.format("%s=%s", DEPLAMBDA_RELATION_PRORITIES_FILE,
                options.get(DEPLAMBDA_RELATION_PRORITIES_FILE)));

        TransformationRuleGroups lambdaAssignmentRules =
            new TransformationRuleGroups(
                options.get(DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE));
        System.err.println(
            String.format("%s=%s", DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE,
                options.get(DEPLAMBDA_LAMBDA_ASSIGNMENT_RULES_FILE)));
        Boolean lexicalizePredicates =
            Boolean.parseBoolean(options.getOrDefault(DEPLAMBDA_LEXICALIZE_PREDICATES, "true"));

        treeTransformer = new TreeTransformerMain(treeTransformationRules,
            relationPrioritiesRules, lambdaAssignmentRules, null,
            lexicalizePredicates);
        System.err.println("Loaded DepLambda Model.. ");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Properties props = new Properties();
    annotators = Arrays.asList(options.get(ANNOTATORS_KEY).split(",")).stream()
        .map(String::trim).collect(Collectors.toSet());

    if (annotators.contains(STANFORD_DEP_PARSER_KEY)) {
      // Stanford parser picks up default parsing model if
      // deparse.extradependencies is not set.
      options.putIfAbsent("depparse.extradependencies", "ref_only_uncollapsed");
    }

    options.entrySet().stream()
        .forEach(option -> props.put(option.getKey(), option.getValue()));

    System.err.println("NlpPipeline Specified Options : " + props);
    pipeline = new StanfordCoreNLP(props);

    if (options.containsKey(MALT_PARSER_KEY)) {
      URL modelURL = null;
      try {
        modelURL = new File(options.get(MALT_PARSER_KEY)).toURI().toURL();
        maltModel = ConcurrentMaltParserService.initializeParserModel(modelURL);
      } catch (MalformedURLException | MaltChainedException e) {
        e.printStackTrace();
      }
    }

    if (options.containsKey(DRAW_SVG_TREES)) {
      svgRenderer = new RenderSVG();
      if (options.containsKey(SVG_ZOOM_FACTOR)) {
        svgRenderer
            .setZoomInFactor(Double.parseDouble(options.get(SVG_ZOOM_FACTOR)));
      }
    }
  }

  @Override
  public void processSentence(JsonObject jsonSentence) {
    String sentence;
    JsonArray words;
    if (jsonSentence.has(SentenceKeys.WORDS_KEY)) {
      words = jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray();
      Preconditions.checkArgument(
          options.containsKey(WHITESPACE_TOKENIZER)
              && options.get(WHITESPACE_TOKENIZER).equals("true"),
          "words are already tokenized. You should use tokenize.whitespace=true");

      Preconditions.checkArgument(
          options.containsKey(WHITESPACE_TOKENIZER)
              && options.get(WHITESPACE_TOKENIZER).equals("true"),
          "input is already tokenized. You should use tokenize.whitespace=true");

      Preconditions
          .checkArgument(
              (options.containsKey(SENTENCE_EOL_SPLITTER) && options
                  .get(SENTENCE_EOL_SPLITTER).equals("true"))
          || (options.containsKey(NEW_LINE_IS_SENTENCE_BREAK)
              && options.get(NEW_LINE_IS_SENTENCE_BREAK).equals("always")),
          "input is already tokenized. You should either use "
              + "ssplit.eolonly=true if you have a single sentence or"
              + " ssplit.newlineIsSentenceBreak=always if there are multiple sentences");

      StringBuilder sb = new StringBuilder();
      jsonSentence.get(SentenceKeys.WORDS_KEY).getAsJsonArray()
          .forEach(word -> {
            sb.append(word.getAsJsonObject().get(SentenceKeys.WORD_KEY)
                .getAsString());
            sb.append(" ");
          });
      sentence = sb.toString().trim();
    } else {
      sentence = jsonSentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
      words = new JsonArray();
    }

    Annotation annotation = new Annotation(sentence);
    pipeline.annotate(annotation);
    int wordCount = 0;
    for (CoreMap sentenceAnnotation : annotation
        .get(SentencesAnnotation.class)) {
      int sentStart = wordCount;
      for (CoreLabel token : sentenceAnnotation.get(TokensAnnotation.class)) {
        JsonObject wordObject;
        if (jsonSentence.has(SentenceKeys.WORDS_KEY)) {
          Preconditions.checkArgument(wordCount < words.size(),
              "Inconsistent number of already tokenized words, and newly tokenized words. Remove the key 'words' and try again");
          wordObject = words.get(wordCount).getAsJsonObject();
        } else {
          wordObject = new JsonObject();
          words.add(wordObject);
        }

        String word = token.get(TextAnnotation.class);
        wordObject.addProperty(SentenceKeys.WORD_KEY, word);
        if (annotators.contains(LEMMA_ANNOTATOR)) {
          String lemma = token.get(LemmaAnnotation.class);
          wordObject.addProperty(SentenceKeys.LEMMA_KEY, lemma);
        }
        if (annotators.contains(POS_ANNOTATOR)) {
          String pos = token.get(PartOfSpeechAnnotation.class);
          wordObject.addProperty(SentenceKeys.POS_KEY, pos);
        }
        if (annotators.contains(NER_ANNOTATOR)) {
          String ner = token.get(NamedEntityTagAnnotation.class);
          wordObject.addProperty(SentenceKeys.NER_KEY, ner);
        }
        wordCount += 1;
      }

      if (annotators.contains(STANFORD_DEP_PARSER_KEY)) {
        SemanticGraph depTree = sentenceAnnotation.get(
            SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
        for (IndexedWord root : depTree.getRoots()) {
          int rootIndex = sentStart + root.index();
          JsonObject rootWord = words.get(rootIndex - 1).getAsJsonObject();
          rootWord.addProperty(SentenceKeys.DEPENDENCY_KEY, "root");
          rootWord.addProperty(SentenceKeys.HEAD_KEY, 0);
          rootWord.addProperty(SentenceKeys.INDEX_KEY, rootIndex);
        }
        for (SemanticGraphEdge edge : depTree.edgeIterable()) {
          int dependentIndex = sentStart + edge.getDependent().index();
          int sourceIndex = sentStart + edge.getSource().index();
          String relation = edge.getRelation().getShortName();

          JsonObject dependent =
              words.get(dependentIndex - 1).getAsJsonObject();
          dependent.addProperty(SentenceKeys.INDEX_KEY, dependentIndex);
          dependent.addProperty(SentenceKeys.HEAD_KEY, sourceIndex);
          dependent.addProperty(SentenceKeys.DEPENDENCY_KEY, relation);
        }
      }

      int sentEnd = wordCount;
      if (sentEnd != sentStart) {
        JsonObject wordObject = words.get(sentEnd - 1).getAsJsonObject();
        wordObject.addProperty(SentenceKeys.SENT_END, true);
      }
    }


    if (options.containsKey(MALT_PARSER_KEY)) {
      int sentStart = 0;
      int currentWordIndex = -1;
      List<String> sentTokens = new ArrayList<>();
      for (JsonElement word : words) {
        currentWordIndex++;
        JsonObject currentWord = word.getAsJsonObject();
        sentTokens.add(String
            .format("%d\t%s\t_\t%s\t_\t_", currentWordIndex - sentStart + 1,
                currentWord.get(SentenceKeys.WORD_KEY).getAsString(),
                currentWord.has(SentenceKeys.POS_KEY)
                    ? currentWord.get(SentenceKeys.POS_KEY).getAsString()
                    : "_"));

        if ((currentWord.has(SentenceKeys.SENT_END)
            && currentWord.get(SentenceKeys.SENT_END).getAsBoolean())
            || currentWordIndex + 1 == words.size()) {
          try {
            String[] sentTokensArr = new String[sentTokens.size()];
            sentTokensArr = sentTokens.toArray(sentTokensArr);
            sentTokens = new ArrayList<>();

            String[] parsedTokens = maltModel.parseTokens(sentTokensArr);
            for (int i = 0; i < parsedTokens.length; i++) {
              String[] parts = parsedTokens[i].split("\t");
              int head = Integer.parseInt(parts[6]);
              if (head != 0)
                head = head + sentStart;
              String label = parts[7];
              JsonObject wordObject =
                  words.get(i + sentStart).getAsJsonObject();
              wordObject.addProperty(SentenceKeys.INDEX_KEY, i + sentStart + 1);
              wordObject.addProperty(SentenceKeys.HEAD_KEY, head);
              wordObject.addProperty(SentenceKeys.DEPENDENCY_KEY, label);
            }
          } catch (MaltChainedException e) {
            e.printStackTrace();
          }
          sentStart = currentWordIndex + 1;
        }
      }
    }

    if (!jsonSentence.has(SentenceKeys.WORDS_KEY))
      jsonSentence.add(SentenceKeys.WORDS_KEY, words);

    if (options.containsKey(DRAW_SVG_TREES)
        && options.get(DRAW_SVG_TREES).equals("true")) {
      try {
        jsonSentence.addProperty(SentenceKeys.SVG_TREES,
            svgRenderer.drawSVGTrees(jsonSentence));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (options.containsKey(DEPLAMBDA)) {
      System.err.println("Runnning deplambda");
      treeTransformer.processSentence(jsonSentence);
    }
  }

  public static void main(String[] args)
      throws IOException, InterruptedException {
    if (args.length == 0 || args.length % 2 != 0) {
      System.err.println(
          "Specify pipeline arguments, e.g., annotator, languageCode. See the NlpPipelineTest file.");
    }

    Map<String, String> options = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) {
      options.put(args[i], args[i + 1]);
    }

    NlpPipeline englishPipeline = new NlpPipeline(options);
    englishPipeline.processStream(System.in, System.out, 20, true);

  }
}
