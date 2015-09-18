package in.sivareddy.graphparser.cli;

import in.sivareddy.graphparser.ccg.CcgAutoLexicon;
import in.sivareddy.graphparser.parsing.CreateGroundedLexicon;
import in.sivareddy.graphparser.parsing.GroundedGraphs;
import in.sivareddy.graphparser.util.GroundedLexicon;
import in.sivareddy.graphparser.util.Schema;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBase;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseCached;
import in.sivareddy.graphparser.util.knowledgebase.KnowledgeBaseOnline;

import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class RunPrintDomainLexicon extends AbstractCli {

  // Schema File
  private OptionSpec<String> schema;

  private OptionSpec<String> cachedKB;

  // Relations that are potential types
  private OptionSpec<String> relationTypesFile;

  // Freebase relation to identity the type of an entity.
  private OptionSpec<String> typeKey;

  // Sparql End point and details
  private OptionSpec<String> endpoint;

  // CCG Bank co-indexed mapping, non-standard unary rules, and non-standard
  // binary rules.
  private OptionSpec<String> ccgIndexedMapping;
  private OptionSpec<String> unaryRules;
  private OptionSpec<String> binaryRules;

  // CCG special categories lexicon
  private OptionSpec<String> ccgLexicon;
  private OptionSpec<String> ccgLexiconQuestions;

  private OptionSpec<String> semanticParseKey;

  private OptionSpec<Integer> nBestCcgParses;

  private OptionSpec<Integer> nthreads;

  private OptionSpec<Boolean> ignoreTypes;

  @Override
  public void initializeOptions(OptionParser parser) {
    parser.acceptsAll(Arrays.asList("help", "h"), "Print this help message.");
    schema =
        parser.accepts("schema", "File containing schema of the domain")
            .withRequiredArg().ofType(String.class).required();

    cachedKB =
        parser.accepts("cachedKB", "cached version of KB").withRequiredArg()
            .ofType(String.class).defaultsTo("");

    endpoint =
        parser.accepts("endpoint", "SPARQL endpoint").withRequiredArg()
            .ofType(String.class).defaultsTo("localhost");

    typeKey =
        parser
            .accepts(
                "typeKey",
                "Freebase relation name to identify the type of an entity. e.g. rdf:type or fb:type.object.type")
            .withRequiredArg().ofType(String.class).defaultsTo("rdf:type");

    relationTypesFile =
        parser
            .accepts(
                "relationTypesFile",
                "File containing relations that may be potential types e.g. data/freebase/stats/business_relation_types.txt")
            .withRequiredArg().ofType(String.class)
            .defaultsTo("lib_data/dummy.txt");

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
                "ccg special categories Questions lexicon")
            .withRequiredArg()
            .ofType(String.class)
            .defaultsTo("./lib_data/lexicon_specialCases_questions_vanilla.txt");

    semanticParseKey =
        parser
            .accepts("semanticParseKey",
                "key from which a semantic parse is read").withRequiredArg()
            .ofType(String.class).defaultsTo("synPars");

    nBestCcgParses =
        parser
            .accepts("nBestCcgParses",
                "number of syntactic parses to use while training")
            .withRequiredArg().ofType(Integer.class).defaultsTo(1);

    nthreads =
        parser.accepts("nthreads", "number of threads").withRequiredArg()
            .ofType(Integer.class).defaultsTo(10);

    ignoreTypes =
        parser
            .accepts("ignoreTypes",
                "ignore types: subsumes groundFreeVariables and useEmptyTypes")
            .withRequiredArg().ofType(Boolean.class).defaultsTo(false);
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

      CcgAutoLexicon normalCcgAutoLexicon =
          new CcgAutoLexicon(options.valueOf(ccgIndexedMapping),
              options.valueOf(unaryRules), options.valueOf(binaryRules),
              options.valueOf(ccgLexicon));

      CcgAutoLexicon questionCcgAutoLexicon =
          new CcgAutoLexicon(options.valueOf(ccgIndexedMapping),
              options.valueOf(unaryRules), options.valueOf(binaryRules),
              options.valueOf(ccgLexiconQuestions));

      String semanticParseKeyString = options.valueOf(semanticParseKey);
      int nBestCcgParsesVal = options.valueOf(nBestCcgParses);
      int threadCount = options.valueOf(nthreads);
      boolean ignoreTypesVal = options.valueOf(ignoreTypes);

      String[] relationLexicalIdentifiers = {"lemma"};
      String[] relationTypingIdentifiers = {};

      GroundedLexicon groundedLexicon = new GroundedLexicon(null);
      GroundedGraphs graphCreator =
          new GroundedGraphs(schemaObj, kb, groundedLexicon,
              normalCcgAutoLexicon, questionCcgAutoLexicon,
              relationLexicalIdentifiers, relationTypingIdentifiers, null, 1,
              false, false, false, false, false, false, false, false, false,
              false, false, false, false, false, false, false, false, false,
              false, false, false, false, false, false, false, false, false,
              false, false, false, 0.0, 0.0, 0.0, 0.0);

      CreateGroundedLexicon engine =
          new CreateGroundedLexicon(graphCreator, kb, semanticParseKeyString,
              ignoreTypesVal, nBestCcgParsesVal);

      engine.processStream(System.in, System.out, threadCount);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    new RunPrintDomainLexicon().run(args);
  }
}
