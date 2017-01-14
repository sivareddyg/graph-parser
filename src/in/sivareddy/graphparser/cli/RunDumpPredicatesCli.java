package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.ccg.LexicalItem;
import in.sivareddy.graphparser.parsing.GraphToSparqlConverter;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.parsing.LexicalGraph;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.graph.Edge;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;
import in.sivareddy.ml.learning.StructuredPercepton;
import in.sivareddy.util.ProcessStreamInterface;
import in.sivareddy.util.SentenceKeys;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class RunDumpPredicatesCli extends AbstractCli {

  // Sparql End point and details
  private OptionSpec<String> endpoint;

  // Freebase relation to identity the type of an entity.
  private OptionSpec<String> typeKey;

  // Schema File
  private OptionSpec<String> schema;

  // CCG Bank co-indexed mapping, non-standard unary rules, and non-standard
  // binary rules.
  private OptionSpec<String> ccgIndexedMapping;
  private OptionSpec<String> unaryRules;
  private OptionSpec<String> binaryRules;

  // CCG special categories lexicon
  private OptionSpec<String> ccgLexicon;
  private OptionSpec<String> ccgLexiconQuestions;

  // Relations that are potential types
  private OptionSpec<String> relationTypesFile;

  // Log File
  private OptionSpec<String> lexicon;
  private OptionSpec<String> cachedKB;
  
  // Content Pos Tags
  private OptionSpec<String> contentWordPosTags;

  private OptionSpec<String> semanticParseKey;
  private OptionSpec<Integer> nthreads;
  
  @Override
  public void initializeOptions(OptionParser parser) {
    parser.acceptsAll(Arrays.asList("help", "h"), "Print this help message.");

    endpoint =
        parser.accepts("endpoint", "SPARQL endpoint").withRequiredArg()
            .ofType(String.class).required();

    typeKey =
        parser
            .accepts(
                "typeKey",
                "Freebase relation name to identify the type of an entity. e.g. rdf:type or fb:type.object.type")
            .withRequiredArg().ofType(String.class).defaultsTo("rdf:type");

    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();

    ccgIndexedMapping =
        parser
            .accepts("ccgIndexedMapping",
                "Co-indexation information for categories").withRequiredArg()
            .ofType(String.class)
            .defaultsTo("./lib_data/candc_markedup.modified");

    unaryRules =
        parser.accepts("unaryRules", "Type-Changing Rules in CCGbank")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./lib_data/unary_rules.txt");

    binaryRules =
        parser.accepts("binaryRules", "Binary Type-Changing rules in CCGbank")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./lib_data/binary_rules.txt");

    ccgLexicon =
        parser.accepts("ccgLexicon", "ccg special categories lexicon")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("./lib_data/lexicon_specialCases.txt");

    ccgLexiconQuestions =
        parser
            .accepts("ccgLexiconQuestions",
                "ccg special categories Questions lexicon").withRequiredArg()
            .ofType(String.class)
            .defaultsTo("./lib_data/lexicon_specialCases_questions.txt");

    relationTypesFile =
        parser
            .accepts(
                "relationTypesFile",
                "File containing relations that may be potential types e.g. data/freebase/stats/business_relation_types.txt")
            .withRequiredArg().ofType(String.class).required();

    contentWordPosTags =
        parser
            .accepts("contentWordPosTags",
                "content Word Pos tags for extracting ngram features. Seperate each tag with ;")
            .withRequiredArg().ofType(String.class).defaultsTo("");

    semanticParseKey =
        parser
            .accepts("semanticParseKey",
                "key from which a semantic parse is read").withRequiredArg()
            .ofType(String.class).defaultsTo("synPars");
    
    
    lexicon =
        parser.accepts("lexicon", "lexicon containing nl to grounded mappings")
            .withRequiredArg().ofType(String.class).required();

    cachedKB =
        parser.accepts("cachedKB", "cached version of KB").withRequiredArg()
            .ofType(String.class).defaultsTo("");

    
    nthreads =
        parser.accepts("nthreads", "number of threads").withRequiredArg()
            .ofType(Integer.class).required();
      }

  @Override
  public void run(OptionSet options) {
    try {
      Schema schemaObj = new Schema(options.valueOf(schema));
      String relationTypesFileName = options.valueOf(relationTypesFile);
      KnowledgeBase kb = null;

      if (!options.valueOf(cachedKB).equals("")) {
        kb =
            new KnowledgeBaseCached(options.valueOf(cachedKB),
                relationTypesFileName);
      } else {
        KnowledgeBaseOnline.TYPE_KEY = options.valueOf(typeKey);
        kb =
            new KnowledgeBaseOnline(options.valueOf(endpoint), String.format(
                "http://%s:8890/sparql", options.valueOf(endpoint)), "dba",
                "dba", 50000, schemaObj);
      }

      GraphToSparqlConverter.TYPE_KEY = options.valueOf(typeKey);
      GroundedGraphs.CONTENT_WORD_POS =
          Sets.newHashSet(Splitter.on(";").trimResults().omitEmptyStrings()
              .split(options.valueOf(contentWordPosTags)));

      CcgAutoLexicon normalCcgAutoLexicon =
          new CcgAutoLexicon(options.valueOf(ccgIndexedMapping),
              options.valueOf(unaryRules), options.valueOf(binaryRules),
              options.valueOf(ccgLexicon));

      CcgAutoLexicon questionCcgAutoLexicon =
          new CcgAutoLexicon(options.valueOf(ccgIndexedMapping),
              options.valueOf(unaryRules), options.valueOf(binaryRules),
              options.valueOf(ccgLexiconQuestions));

      GroundedLexicon groundedLexicon =
          new GroundedLexicon(options.valueOf(lexicon));


      String[] relationLexicalIdentifiers = {"word"};
      String[] relationTypingIdentifiers = {};

      GroundedGraphs graphCreator =
          new GroundedGraphs(schemaObj, kb, groundedLexicon,
              normalCcgAutoLexicon, questionCcgAutoLexicon,
              relationLexicalIdentifiers, relationTypingIdentifiers,
              new StructuredPercepton(), null, 1, true, true, true, true, true,
              true, true, true, true, true, true, true, true, true, true, true,
              true, true, true, true, true, true, true, true, true, true, true,
              true, true, true, true, false, false, false, false, 10.0, 1.0,
              0.0, 0.0, 0.0);

      GraphToMainPredicate graphToMainPredicate =
          new GraphToMainPredicate(graphCreator,
              options.valueOf(semanticParseKey));
      graphToMainPredicate.processStream(System.in, System.out,
          options.valueOf(nthreads), true);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static class GraphToMainPredicate extends ProcessStreamInterface {

    private final GroundedGraphs graphCreator;
    private final String semanticParseKey;

    public GraphToMainPredicate(GroundedGraphs graphCreator,
        String semanticParseKey) {
      this.graphCreator = graphCreator;
      this.semanticParseKey = semanticParseKey;
    }

    @Override
    public void processSentence(JsonObject sentence) {
      JsonObject firstSentence = sentence;
      if (sentence.has(SentenceKeys.FOREST)) {
        firstSentence =
            sentence.get(SentenceKeys.FOREST).getAsJsonArray().get(0)
                .getAsJsonObject();
      }
      
      // Form a sentence.
      String sentenceString = "";
      if (sentence.has(SentenceKeys.SENTENCE_KEY))
        sentenceString = sentence.get(SentenceKeys.SENTENCE_KEY).getAsString();
      else if (sentence.has(SentenceKeys.WORDS_KEY)) {
        List<String> wordStrings = new ArrayList<>();
        sentence
            .get(SentenceKeys.WORDS_KEY)
            .getAsJsonArray()
            .forEach(
                x -> wordStrings.add(x.getAsJsonObject()
                    .get(SentenceKeys.WORD_KEY).getAsString()));
        sentenceString = Joiner.on(" ").join(wordStrings);
      } else {
        List<String> wordStrings = new ArrayList<>();
        firstSentence
            .get(SentenceKeys.WORDS_KEY)
            .getAsJsonArray()
            .forEach(
                x -> wordStrings.add(x.getAsJsonObject()
                    .get(SentenceKeys.WORD_KEY).getAsString()));
        sentenceString = Joiner.on(" ").join(wordStrings);
      }

      List<LexicalGraph> graphs =
          graphCreator.buildUngroundedGraph(firstSentence, semanticParseKey, 1);

      List<String> keys = new ArrayList<>();
      sentence.entrySet().forEach(x -> keys.add(x.getKey()));
      keys.forEach(x -> sentence.remove(x));
      sentence.addProperty(SentenceKeys.SENTENCE_KEY, sentenceString);
      JsonArray predicates = new JsonArray();
      sentence.add(SentenceKeys.RELATIONS, predicates);

      if (graphs.size() > 0 && graphs.get(0).getQuestionNode().size() > 0) {
        // System.out.println(graphs.get(0));
        LexicalGraph firstGraph = graphs.get(0);
        LexicalItem qNode = firstGraph.getQuestionNode().iterator().next();
        TreeSet<Edge<LexicalItem>> qEdges = firstGraph.getEdges(qNode);
        if (qEdges != null) {
          for (Edge<LexicalItem> edge : qEdges) {
            if (edge.getRight().isEntity()) {
              String entity = edge.getRight().getWord();
              String predicate = edge.getMediator().getWord();
              JsonObject predicateObj = new JsonObject();
              predicateObj.addProperty(SentenceKeys.ENTITY, entity);
              predicateObj.addProperty(SentenceKeys.RELATION, predicate);
              predicates.add(predicateObj);
            }
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    new RunDumpPredicatesCli().run(args);
  }
}
