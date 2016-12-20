package in.sivareddy.util;

import java.util.HashSet;

import com.google.common.collect.Sets;

public class SentenceKeys {
  public static String WORDS_KEY = "words";
  public static String WORD_KEY = "word";
  public static String POS_KEY = "pos";
  public static String LEMMA_KEY = "lemma";
  public static String NER_KEY = "ner";
  public static final String LANGUAGE_CODE = "lang";
  public static String SENTENCE_KEY = "sentence";
  public static String INDEX_KEY = "index";
  public static String HEAD_KEY = "head";
  public static String DEPENDENCY_KEY = "dep";
  public static String DEPENDENCY_ROOT = "ROOT";
  public static String SENT_END = "sentEnd";
  public static String SVG_TREES = "svgTrees";
  
  public static String CCG_PARSES = "synPars";
  public static String CCG_PARSE = "synPar";
  public static String DEPENDENCY_LAMBDA = "dependency_lambda";
  public static String DEPENDENCY_QUESTION_GRAPH = "dependency_question_graph";
  public static String DEPENDENCY_GRAPH = "dependency_graph";
  public static String BOW_QUESTION_GRAPH = "bow_question_graph";
  
  public static String ENTITIES = "entities";
  public static String ENTITY = "entity";
  public static String ENTITY_NAME = "name";
  public static String ENTITY_ID = "id";
  public static String ENTITY_INDEX = "index";
  public static String MATCHED_ENTITIES = "matchedEntities";
  public static String RANKED_ENTITIES = "rankedEntities";
  public static String DISAMBIGUATED_ENTITIES = "disambiguatedEntities";
  public static String PHRASE = "phrase";
  public static String START = "start";
  public static String END = "end";
  public static String SCORE = "score";
  public static String PATTERN = "pattern";
  
  public static String PARAPHRASE = "paraphrase";
  public static String PARAPHRASE_SCORE = "paraphraseScore";
  public static String PARAPHRASE_CLASSIFIER_SCORE = "utteranceParaphraseClassifierScore";
  public static String IS_ORIGINAL_SENTENCE = "isOriginal";
  
  public static String FOREST = "forest";
  
  public static String GOLD_MID = "goldMid";
  public static String GOLD_MIDS = "goldMids";
  public static String GOLD_RELATIONS = "goldRelations";
  public static String RELATION = "relation";
  public static String RELATION_LEFT = "relationLeft";
  public static String RELATION_RIGHT = "relationRight";
  public static String TARGET_VALUE = "targetValue";
  public static String ANSWER_F1 = "answerF1";
  public static String SPARQL_QUERY = "sparqlQuery";
  
  public static String BLANK_WORD = "_blank_";
  public static String DUMMY_WORD = "_dummy_";
  
  public static String ENTITY_PAIR = "entity_pair";
  public static String COUNT = "count";
  public static String RELATIONS = "relations";
  
  
public static String NTHREADS = "nthreads";
  
  // CONLL-X keys
  public static String FINE_POS_KEY = "fpos";
  public static String FEATS_KEY = "feats";
  public static String PHEAD = "phead";
  public static String PDEPREL = "pdep";

  // Language codes
  public static String ENGLISH_LANGUAGE_CODE = "en";
  public static String GERMAN_LANGUAGE_CODE = "de";
  public static String SPANISH_LANGUAGE_CODE = "es";

  public static String UNIVERSAL_DEPENDENCIES_POS_TAG_CODE = "UD";
  
  public static String PENN_DEPENDENCIES_POS_TAG_CODE = "PTB";
  public static String PENN_PROPER_NOUN_TAG = "NNP";
  
  public static String UD_PROPER_NOUN_TAG = "PROPN";
  public static String UD_NOUN_TAG = "NOUN";
  
  public static String DEFAULT_DEPENDENCY_KEY = "dep";
  
  public static HashSet<String> PUNCTUATION_TAGS = Sets.newHashSet("PUNCT", "SYM", "P");
}

