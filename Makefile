# Convert GraphParser format to deplambda input format
convert_graphparser_to_deplambda_format:
	head -n915 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json | python scripts/convert-graph-parser-to-entity-mention-format.py > working/webquestions.train.txt
	tail -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json | python scripts/convert-graph-parser-to-entity-mention-format.py > working/webquestions.dev.txt
	cat data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json | python scripts/convert-graph-parser-to-entity-mention-format.py > working/webquestions.test.txt
	head -n915 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.noheuristics.json | python scripts/convert-graph-parser-to-entity-mention-format.py > working/webquestions.train.noheuristics.txt
	tail -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.noheuristics.json | python scripts/convert-graph-parser-to-entity-mention-format.py > working/webquestions.dev.noheuristics.txt
	cat data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.noheuristics.json | python scripts/convert-graph-parser-to-entity-mention-format.py > working/webquestions.test.noheuristics.txt
	cat data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt | python scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people.txt

convert_graphparser_to_deplambda_format_tom:
	cat data/webquestions/webquestions.train.all.entity_annotated.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.train.json.txt
	cat data/webquestions/webquestions.test.all.entity_annotated.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.test.json.txt

convert_cai_yates_splits_to_deplambda:
	mkdir -p working/free917_business_film_people_splits
	cat data/cai-yates-2013/free917_business_film_people_splits/0.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/0.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/1.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/1.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/2.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/2.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/3.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/3.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/4.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/4.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/5.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/5.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/6.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/6.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/7.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/7.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/8.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/8.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/9.txt | python  scripts/convert-graph-parser-to-entity-mention-format.py > working/free917_business_film_people_splits/9.txt

# Converts deplambda documents in json format to graphparsers json format.
convert_deplambda_output_to_graphparser:
	#cat data/deplambda/training/* \
	#| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	#| python scripts/cleaning/remove_duplicate_sentences.py \
	#| gzip > data/deplambda/unsupervised.graphparser.txt.gz
	cat data/deplambda/wq-train-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	> data/deplambda/webquestions.train.graphparser.txt
	cat data/deplambda/wq-dev-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	> data/deplambda/webquestions.dev.graphparser.txt
	cat data/deplambda/wq-test-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	> data/deplambda/webquestions.test.graphparser.txt
	cat data/deplambda/free917-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt \
	> data/deplambda/free917.txt

# Create dependency based grounded lexicon.
# Unfortunately, this cannot run parallel version since I have to write
# sentences into a single file.
create_deplambda_grounded_lexicon:
	mkdir -p data/deplambda/sentences_training
	mkdir -p data/deplambda/grounded_lexicon
	zcat data/deplambda/unsupervised.graphparser.txt.gz \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey dependency_lambda \
	--argumentLexicalIdentifiers mid \
	--relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.1.txt \
	| python scripts/freebase-training/filter_training_sentences.py 0 0 0 0 0 \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| gzip > data/deplambda/sentences_training/deplambda_training_sentences.1.txt.gz

# TACL CCG lexicon and training sentences
create_tacl_ccg_grounded_lexicon_and_training_sentences:
	mkdir -p data/tacl/sentences_training
	mkdir -p data/tacl/grounded_lexicon
	zcat data/freebase/sentences_filtered/* \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/candc_markedup.modified \
	--unaryRulesFile lib_data/unary_rules.txt \
	--binaryRulesFile lib_data/binary_rules.txt \
	--specialCasesFile lib_data/lexicon_specialCases.txt \
	--relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	| python scripts/freebase-training/filter_training_sentences.py 0 0 0 0 0 \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| gzip > data/tacl/sentences_training/tacl_training_sentences.txt.gz

# Deplambda Experiments
# Baseline to evaluate the accuracy of lexicon
deplambda_mwg:
	mkdir -p working/deplambda_mwg
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-iterations 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint stkilda \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile working/deplambda_mwg/business_film_people.log.txt \
	> working/deplambda_mwg/business_film_people.txt

deplambda_mwg_on_training_data:
	mkdir -p working/deplambda_mwg_on_training
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-iterations 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-devFile data/deplambda/webquestions.train.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile working/deplambda_mwg_on_training/business_film_people.log.txt \
	> working/deplambda_mwg_on_training/business_film_people.txt

deplambda_mwg_free917:
	mkdir -p working/deplambda_mwg_free917
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-iterations 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-devFile data/deplambda/free917.txt \
	-logFile working/deplambda_mwg_free917/business_film_people.log.txt \
	> working/deplambda_mwg_free917/business_film_people.txt

# Supervised Expermients
# Deplambda results without unsupervised lexicon.
deplambda_supervised:
	mkdir -p ../working/deplambda_supervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint stkilda \
	-supervisedCorpus data/deplambda/webquestions.train.graphparser.txt \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_supervised/business_film_people.log.txt \
	> ../working/deplambda_supervised/business_film_people.txt

# Deplambda results with unsupervised lexicon.
deplambda_supervised_with_unsupervised_lexicon:
	mkdir -p ../working/deplambda_supervised_with_unsupervised_lexicon
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint kinloch \
	-supervisedCorpus data/deplambda/webquestions.train.graphparser.txt \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_supervised_with_unsupervised_lexicon/business_film_people.log.txt \
	> ../working/deplambda_supervised_with_unsupervised_lexicon/business_film_people.txt

# deplambda with unsupervised training
deplambda_unsupervised:
	mkdir -p ../working/deplambda_unsupervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 80 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint stkilda \
	-trainingCorpora data/deplambda/sentences_training/deplambda_training_sentences.txt.gz \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_unsupervised/business_film_people.log.txt \
	> ../working/deplambda_unsupervised/business_film_people.txt

############################################### TACL Experiments  ################################################
# TACL MWG Baseline
tacl_mwg:
	mkdir -p working/tacl_mwg
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-iterations 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-devFile data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile working/tacl_mwg/business_film_people.log.txt \
	> working/tacl_mwg/business_film_people.txt

tacl_mwg_on_training_data:
	mkdir -p working/tacl_mwg_on_training
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-iterations 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint stkilda \
	-devFile data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile working/tacl_mwg_on_training/business_film_people.log.txt \
	> working/tacl_mwg_on_training/business_film_people.txt

tacl_mwg_free917:
	mkdir -p working/tacl_mwg_free917
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-iterations 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-devFile data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt \
	-logFile working/tacl_mwg_free917/business_film_people.log.txt \
	> working/tacl_mwg_free917/business_film_people.txt

# TACL GraphPaser results
tacl_unsupervised:
	mkdir -p ../working/tacl_unsupervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 80 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-trainingCorpora data/tacl/sentences_training/tacl_training_sentences.txt.gz \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_unsupervised/business_film_people.log.txt \
	> ../working/tacl_unsupervised/business_film_people.txt

tacl_supervised:
	mkdir -p ../working/tacl_supervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-supervisedCorpus data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised/business_film_people.log.txt \
	> ../working/tacl_supervised/business_film_people.txt

tacl_supervised_with_unsupervised_lexicon:
	mkdir -p ../working/tacl_supervised_with_unsupervised_lexicon
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint kinloch \
	-supervisedCorpus data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised_with_unsupervised_lexicon/business_film_people.log.txt \
	> ../working/tacl_supervised_with_unsupervised_lexicon/business_film_people.txt

# To load an existing model and to parse and input corpus using it.
tacl_unsupervised_loaded_model:
	mkdir -p ../working/tacl_unsupervised_loaded_model
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 10 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint bravas \
	-loadModelFromFile working/tacl_unsupervised/business_film_people.log.txt.model.bestIteration \
	-groundInputCorpora data/tacl/sentences_training/business_tacl_training_sentences_small.txt.gz \
	-logFile working/tacl_unsupervised_loaded_model/business_film_people.log.txt \
	> working/tacl_unsupervised_loaded_model/business_film_people.txt

# Spanish Experiments

# Tokenize Entities
tokenize_spanish_entities:
	zcat data/freebase/spanish/spanish_business_entities.txt.gz | java -cp lib/*:graph-parser.jar others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_business_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_film_entities.txt.gz | java -cp lib/*:graph-parser.jar others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_film_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_people_entities.txt.gz | java -cp lib/*:graph-parser.jar others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_people_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_business_entities.tokenized.txt.gz data/freebase/spanish/spanish_film_entities.tokenized.txt.gz data/freebase/spanish/spanish_people_entities.tokenized.txt.gz | gzip > data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz

extract_spanish_sentences:
	bzcat data/bravas/extracted/AA/wiki_00.bz2 \
		| java -cp lib/*:graph-parser.jar others.SpanishTokenizer                 \
		| perl -pe 's|=LRB=.*?=RRB=||g'                 \
		| grep -v =LRB= | grep -v =RRB=                 \
		| python scripts/spanish/select_sentences_with_entities_in_relation.py data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt                 \
		| python scripts/spanish/select_sentences_with_non_adjacent_main_relation.py data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt \
		| java -cp lib/*:graph-parser.jar others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		| gzip > data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz

create_spanish_deplambda_format:
	zcat data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz \
		| python scripts/spanish/create-entity-mention-format.py \
		| gzip \
		> working/spanish_wikipedia.txt.gz

## Unsupervised Parsing experiments
unsupervised_first_experiment:
	mkdir -p working/unsupervised_first_experiment
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
    -schema data/freebase/schema/business_film_people_schema.txt \
    -relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
    -lexicon data/dummy.txt \
    -ccgLexicon data/dummy.txt \
    -ccgIndexedMapping lib_data/ybisk-mapping.txt \
    -unaryRules data/dummy.txt \
    -binaryRules data/dummy.txt \
    -cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
    -domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
    -nthreads 20 \
    -trainingSampleSize 600 \
    -iterations 10 \
    -nBestTrainSyntacticParses 100 \
    -nBestTestSyntacticParses 1 \
    -nbestGraphs 100 \
    -debugEnabledFlag false \
    -useSchema true \
    -useKB true \
    -groundFreeVariables true \
    -useEmptyTypes false \
    -ignoreTypes true \
    -urelGrelFlag true \
    -urelPartGrelPartFlag false \
    -utypeGtypeFlag false \
    -gtypeGrelFlag false \
    -wordGrelPartFlag false \
    -wordBigramGrelPartFlag false \
    -argGrelPartFlag false \
    -stemMatchingFlag true \
    -mediatorStemGrelPartMatchingFlag false \
    -argumentStemMatchingFlag false \
    -argumentStemGrelPartMatchingFlag false \
    -graphIsConnectedFlag true \
    -graphHasEdgeFlag true \
    -countNodesFlag true \
    -edgeNodeCountFlag true \
    -duplicateEdgesFlag true \
    -grelGrelFlag true \
    -useLexiconWeightsRel false \
    -useLexiconWeightsType false \
    -validQueryFlag true \
    -initialEdgeWeight 1.0 \
    -initialTypeWeight -1.0 \
    -initialWordWeight 10.00 \
    -stemFeaturesWeight 0.0 \
    -useNbestGraphsFlag true \
    -endpoint darkstar \
    -trainingCorpora "data/unsupervised/training/unsupervised_parser.json.noDeps.gz" \
    -logFile working/unsupervised_first_experiment/business_film_people.log.txt \
    > working/unsupervised_first_experiment/business_film_people.txt
