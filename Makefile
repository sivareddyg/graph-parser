# Extract clueweb sentences
extract_sentences_with_multiple_entities:
	python scripts/freebase/extract_clueweb_sentences_containing_entities.py \
        ../data/clueweb/CLUEWEB09_1/wiki00/ \
        ../data/clueweb/freebase_annotated_data/ClueWeb09_English_1 \
        | gzip > ../data/clueweb/wiki-sentences.json.txt.00.gz

process_clueweb_split_%:
	#python scripts/freebase/extract_clueweb_sentences_containing_entities.py \
        #working/clueweb/ClueWeb09_English_1/ \
        #../data/clueweb/freebase_annotated_data/ClueWeb09_English_1_split$* \
        #| gzip > ../data/clueweb/ClueWeb09_1-sentences.json.txt.$*.gz
	# python scripts/freebase/merge_duplicate_sentences.py ../data/clueweb/ClueWeb09_1-sentences.json.txt.$*.gz | gzip > ../data/clueweb/ClueWeb09_1-sentences.cleaned.json.txt.$*.gz
	zcat ../data0/clueweb/ClueWeb09_1-sentences.cleaned.json.txt.$*.gz \
		| java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunTokenizerOnEntityTaggedClueweb \
		| python scripts/cleaning/remove_longer_sentences.py \
		| python scripts/cleaning/remove_duplicate_sentences.py \
		| java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunPosTaggerAndNerWithoutTokenizerPipeline \
		| python scripts/run_batch_process.py "java -cp lib/*:bin in.sivareddy.scripts.RunEasyCCG 1" 100000 \
		| gzip > ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.$*.gz

webq_dev_split:
	python scripts/webquestions-preprocessing/training_split.py data/webquestions/webquestions.examples.train.domains.entity.matches.json

extract_easyccg_lexicon_clueweb_split_%:
	zcat ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.$*.gz \
	| python scripts/run_batch_process.py \
		"java -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
		-endpoint localhost \
		-schema data/freebase/schema/all_domains_schema.txt \
		-typeKey "fb:type.object.type" \
		-nthreads 40 \
		-ignoreTypes true" \
		100000 \
	| gzip > ../data0/clueweb/ClueWeb09_1-lexicon.txt.$*.gz

extract_wiki_lexicon:
	zcat ../data/clueweb/wiki-sentences.cleaned.json.txt.00.gz  ../data/clueweb/wiki-sentences.cleaned.json.txt.01.gz  ../data/clueweb/wiki-sentences.cleaned.json.txt.02.gz ../data/clueweb/wiki-sentences.cleaned.json.txt.03.gz \
        | java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunTokenizerOnEntityTaggedClueweb \
        | python scripts/cleaning/remove_longer_sentences.py \
        | python scripts/cleaning/remove_duplicate_sentences.py \
        | java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunPosTaggerAndNerWithoutTokenizerPipeline \
        | python scripts/run_batch_process.py "java -cp lib/*:bin in.sivareddy.scripts.RunEasyCCG 1" 100000 \
        | gzip > ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.wiki.gz
	make extract_easyccg_lexicon_clueweb_split_wiki

merge_easyccg_lexicon:
	zcat ../data0/clueweb/ClueWeb09_1-lexicon.txt.1.gz ../data0/clueweb/ClueWeb09_1-lexicon.txt.2.gz ../data0/clueweb/ClueWeb09_1-lexicon.txt.3.gz ../data0/clueweb/ClueWeb09_1-lexicon.txt.wiki.gz \
		|	python scripts/freebase/merge_lexicon.py \
	    | gzip > data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz

merge_deplambda_lexicon:
	zcat ../data0/clueweb/ClueWeb09_1-lexicon.deplambda.txt.0.gz ../data0/clueweb/ClueWeb09_1-lexicon.deplambda.txt.1.gz ../data0/clueweb/ClueWeb09_1-lexicon.deplambda.txt.2.gz ../data0/clueweb/ClueWeb09_1-lexicon.deplambda.txt.3.gz \
		|	python scripts/freebase/merge_lexicon.py \
	    | gzip > data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz

# Merge schema
all_domains_schema:
	python scripts/freebase/merge_schema_folder.py data/freebase/schema/all-domains/ > data/freebase/schema/all_domains_schema.txt

# English Automatic Entity Annotation
#
dump_sempre_db:
	isql-vt localhost:1111 dba dba < scripts/dump_freebase.isql

create_mid_to_key_dict:
	zcat ../data/freebase-cleaned.rdf-2013-08-11-00-00.gz | python scripts/entity-annotation/print_entity_to_id.py | gzip > data/freebase/mid_to_key.txt.gz

convert_sempre_to_standard_db:
	zcat /disk/scratch/users/s1051585/tools/var/lib/virtuoso/vdb/data_000001.ttl.gz \
	| python scripts/entity-annotation/convert_sempre_freebase_to_standard_freebase.py data/freebase/mid_to_key.txt.gz \
	| gzip > /disk/scratch/users/s1051585/data/freebase_sempre.ttl.gz


extract_sempre_entities_list:
	zcat /disk/scratch/users/s1051585/data/freebase_sempre.ttl.gz \
	| python scripts/entity-annotation/extract_entities_from_standard_sempre.py \
	| gzip > data/freebase/freebase_sempre_entities.gz

create_entity_dict:
	zcat /disk/scratch/users/s1051585/data/freebase-cleaned.rdf-2013-08-11-00-00.gz | python scripts/entity-annotation/extract_entities_based_on_langcode.py en,en-gb \
	| java -cp lib/*:bin others.EnglishEntityTokenizer \
	| sed -e 's/\-LRB\-/\(/g' \
	| sed -e 's/\-RRB\-/\)/g' \
	| sed -e 's/\-LSB\-/\[/g' \
	| sed -e 's/\-RSB\-/\]/g' \
	| gzip >  data/freebase/en_entity_lexicon_tokenized.gz

tokenize_entity_dict:
	zcat data/freebase/en_entity_lexicon.gz \
	| java -cp lib/*:bin others.EnglishEntityTokenizer \
	| sed -e 's/\-LRB\-/\(/g' \
	| sed -e 's/\-RRB\-/\)/g' \
	| sed -e 's/\-LSB\-/\[/g' \
	| sed -e 's/\-RSB\-/\]/g' \
	| gzip \
	> data/freebase/en_entity_lexicon_tokenized.gz

entity_span_tag_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.json \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
		| python scripts/webquestions-preprocessing/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator EN_PTB \
		> data/webquestions/webquestions.examples.train.domains.entity.matches.json
	cat data/webquestions/webquestions.examples.test.domains.json \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
		| python scripts/webquestions-preprocessing/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator EN_PTB \
		> data/webquestions/webquestions.examples.test.domains.entity.matches.json

free917_print_out_of_domain_relations:
	cat data/complete/free917/free917.train.json         \
		| python scripts/cai-yates-preprocessing/print_json_sentences_from_list.py         \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py         \
		| python scripts/cai-yates-preprocessing/rename_targetSparql_to_sparqlQuery.py         \
		| java -cp lib/*:bin in.sivareddy.scripts.free917.PrintOutOfDomainRelations data/freebase/schema/all_domains_schema.txt \
	    > working/out_of_domain.txt
	cat data/complete/free917/free917.test.json         \
		| python scripts/cai-yates-preprocessing/print_json_sentences_from_list.py         \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py         \
		| python scripts/cai-yates-preprocessing/rename_targetSparql_to_sparqlQuery.py         \
		| java -cp lib/*:bin in.sivareddy.scripts.free917.PrintOutOfDomainRelations data/freebase/schema/all_domains_schema.txt \
	    >> working/out_of_domain.txt


free917_merge_out_of_domain_schema:
	# First manually create the schema using out_of_domain.txt
	python scripts/freebase/merge_schema.py data/freebase/schema/all_domains_schema.txt  data/freebase/schema/free917_out_of_domain.txt > data/freebase/schema/all_domains_schema_free917.txt

free917_add_gold_relations_and_process:
	cat data/complete/free917/free917.train.json \
		| python scripts/cai-yates-preprocessing/print_json_sentences_from_list.py \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
		| python scripts/cai-yates-preprocessing/rename_targetSparql_to_sparqlQuery.py \
		| java -cp lib/*:bin in.sivareddy.scripts.free917.AddGoldRelationsAndMidToFree917Data data/freebase/schema/all_domains_schema_free917.txt \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| python scripts/cai-yates-preprocessing/add_question_word.py \
		> data/complete/free917/free917.train.tokenized.json
	cat data/complete/free917/free917.test.json \
		| python scripts/cai-yates-preprocessing/print_json_sentences_from_list.py \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
		| python scripts/cai-yates-preprocessing/rename_targetSparql_to_sparqlQuery.py \
		| java -cp lib/*:bin in.sivareddy.scripts.free917.AddGoldRelationsAndMidToFree917Data data/freebase/schema/all_domains_schema_free917.txt \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| python scripts/cai-yates-preprocessing/add_question_word.py \
		> data/complete/free917/free917.test.tokenized.json

free917_split_data:
	python scripts/cai-yates-preprocessing/training_split.py data/complete/free917/free917.train.tokenized.json
	mv data/complete/free917/free917.train.tokenized.json.70 data/complete/free917/free917.train.split.tokenized.json
	mv data/complete/free917/free917.train.tokenized.json.30 data/complete/free917/free917.dev.split.tokenized.json

free917_tag_entities_using_manual_lexicon:
	python scripts/cai-yates-preprocessing/convert_named_enttiy_lexicon_to_gp_entity_lexicon_format.py \
		< data/complete/free917/free917_entities.txt \
		> data/complete/free917/free917_entities_gp_format.txt
	cat data/complete/free917/free917_entities_gp_format.txt \
		| java -cp lib/*:bin others.EnglishEntityTokenizer \
		| sed -e 's/\-LRB\-/\(/g' \
		| sed -e 's/\-RRB\-/\)/g' \
		| sed -e 's/\-LSB\-/\[/g' \
		| sed -e 's/\-RSB\-/\]/g' \
		| sed -e 's/m.0csy8\t/m.02217f\t/g' \
	> data/complete/free917/free917_entities_gp_format_tokenized.txt


	java -cp lib/*:bin in.sivareddy.scripts.free917.EntityAnnotateFree917 data/complete/free917/free917_entities_gp_format_tokenized.txt \
	< data/complete/free917/free917.train.split.tokenized.json \
	> data/complete/free917/free917.train.tokenized.disambiguatedEntities.json

	cat data/complete/free917/free917.train.tokenized.disambiguatedEntities.json \
	| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
	> data/complete/vanilla_gold/free917.train.easyccg.json

	java -cp lib/*:bin in.sivareddy.scripts.free917.EntityAnnotateFree917 data/complete/free917/free917_entities_gp_format_tokenized.txt \
	< data/complete/free917/free917.dev.split.tokenized.json \
	> data/complete/free917/free917.dev.tokenized.disambiguatedEntities.json

	cat data/complete/free917/free917.dev.tokenized.disambiguatedEntities.json \
	| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
	> data/complete/vanilla_gold/free917.dev.easyccg.json

	java -cp lib/*:bin in.sivareddy.scripts.free917.EntityAnnotateFree917 data/complete/free917/free917_entities_gp_format_tokenized.txt \
	< data/complete/free917/free917.test.tokenized.json \
	> data/complete/free917/free917.test.tokenized.disambiguatedEntities.json

	cat data/complete/free917/free917.test.tokenized.disambiguatedEntities.json \
	| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
	> data/complete/vanilla_gold/free917.test.easyccg.json

add_gold_relations:
		cat data/webquestions/webquestions.examples.train.domains.entity.matches.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> working/webquestions.examples.train.domains.entity.matches.json
		mv working/webquestions.examples.train.domains.entity.matches.json data/webquestions/webquestions.examples.train.domains.entity.matches.json

		cat data/webquestions/webquestions.examples.test.domains.entity.matches.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> working/webquestions.examples.test.domains.entity.matches.json
		mv working/webquestions.examples.test.domains.entity.matches.json data/webquestions/webquestions.examples.test.domains.entity.matches.json

rank_entity_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.json
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.json

noun_phrase_annotator:
	cat data/complete/free917/free917.train.tokenized.json
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator EN_PTB \
		> data/complete/free917/free917.train.entity.matches.json

	cat data/complete/free917/free917.test.tokzenized.json
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator EN_PTB \
		> data/complete/free917/free917.test.entity.matches.json

rank_entity_free917_data:
	cat data/complete/free917/free917.train.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/complete/free917/free917.train.entity.matches.ranked.json
	cat data/complete/free917/free917.test.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/complete/free917/free917.test.entity.matches.ranked.json

disambiguate_entities_webq_data:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.json

disambiguate_entities_free917_data:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema_free917.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-inputFile data/complete/free917/free917.train.entity.matches.ranked.json \
		-outputFile data/complete/free917/free917.train.entity.disambiguated.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema_free917.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-inputFile data/complete/free917/free917.test.entity.matches.ranked.json \
		-outputFile data/complete/free917/free917.test.entity.disambiguated.json

disambiguate_entities_webq_data_second_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.2.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.2.json

disambiguate_entities_free917_data_second_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema_free917.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-inputFile data/complete/free917/free917.train.entity.disambiguated.json \
		-outputFile data/complete/free917/free917.train.entity.disambiguated.2.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema_free917.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-inputFile data/complete/free917/free917.test.entity.disambiguated.json \
		-outputFile data/complete/free917/free917.test.entity.disambiguated.2.json

disambiguate_entities_webq_data_third_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity true \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.2.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity true \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.2.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json

disambiguate_entities_free917_data_third_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema_free917.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity true \
		-inputFile data/complete/free917/free917.train.entity.disambiguated.2.json \
		-outputFile data/complete/free917/free917.train.entity.disambiguated.3.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema_free917.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity true \
		-inputFile data/complete/free917/free917.test.entity.disambiguated.2.json \
		-outputFile data/complete/free917/free917.test.entity.disambiguated.3.json

disambiguate_entities_free917_data_fourth_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity false \
		-inputFile data/complete/free917/free917.train.entity.disambiguated.3.json \
		-outputFile data/complete/free917/free917.train.entity.disambiguated.4.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity false \
		-inputFile data/complete/free917/free917.test.entity.disambiguated.3.json \
		-outputFile data/complete/free917/free917.test.entity.disambiguated.4.json

replace_unknown_mids:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.MapNewMidToOldMid data/freebase/entities_sempre.txt.gz data/freebase/mid_to_key.txt.gz data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		> working/train.txt
	mv working/train.txt data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json

	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.MapNewMidToOldMid data/freebase/entities_sempre.txt.gz data/freebase/mid_to_key.txt.gz data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		> working/test.txt
	mv working/test.txt data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json

replace_unknown_mids_free917:
	cat data/complete/free917/free917.train.entity.disambiguated.3.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.MapNewMidToOldMid data/freebase/entities_sempre.txt.gz data/freebase/mid_to_key.txt.gz \
		> working/train.free917.txt
	mv working/train.free917.txt data/complete/free917/free917.train.entity.disambiguated.3.json

	cat data/complete/free917/free917.test.entity.disambiguated.3.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.MapNewMidToOldMid data/freebase/entities_sempre.txt.gz data/freebase/mid_to_key.txt.gz \
		> working/test.free917.txt
	mv working/test.free917.txt data/complete/free917/free917.test.entity.disambiguated.3.json

entity_disambiguation_results:
	python scripts/entity-annotation/evaluate_entity_annotation.py < data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json | less

entity_disambiguation_results_free917:
	python scripts/entity-annotation/evaluate_entity_annotation.py < data/complete/free917/free917.train.entity.disambiguated.3.json | less

copy_gold_relations:
	python scripts/entity-annotation/copy_gold_relations.py data/webquestions/webquestions.examples.train.domains.entity.matches.json \
		< data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		> working/webquestions.examples.train.domains.entity.disambiguated.3.json
	mv working/webquestions.examples.train.domains.entity.disambiguated.3.json data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json

	python scripts/entity-annotation/copy_gold_relations.py data/webquestions/webquestions.examples.test.domains.entity.matches.json \
		< data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		> working/webquestions.examples.test.domains.entity.disambiguated.3.json
	mv working/webquestions.examples.test.domains.entity.disambiguated.3.json data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json

disambiguate_entities_webq_data_fourth_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.4.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.4.json

entity_dismabiguated_webq_to_graphparser_forrest:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		> working/webquestions.train.full.pass3.txt

	cat working/webquestions.train.full.pass3.txt \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.30 \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt

	cat working/webquestions.train.full.pass3.txt \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.70 \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt

	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		> working/webquestions.test.full.pass3.txt

	cat working/webquestions.test.full.pass3.txt \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.test.full.easyccg.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt

entity_dismabiguated_webq_to_graphparser_forrest_easysrl:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences easysrl \
		> working/webquestions.train.easysrl.full.pass3.txt

	cat working/webquestions.train.easysrl.full.pass3.txt \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.30 \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.easysrl.pass3.json.txt

	cat working/webquestions.train.easysrl.full.pass3.txt \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.70 \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.easysrl.pass3.json.txt

	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences easysrl \
		> working/webquestions.test.easysrl.full.pass3.txt

	cat working/webquestions.test.easysrl.full.pass3.txt \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.test.full.easyccg.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.easysrl.pass3.json.txt

split_forest_to_sentences:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity \
		> working/webquestions.automaticDismabiguation.train.pass3.json.txt
	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity \
		> working/webquestions.automaticDismabiguation.test.pass3.json.txt

free917_split_forest_to_sentences:
	cat data/complete/free917/free917.train.tokenized.disambiguatedEntities.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity false \
		> working/free917.train.forest_split.json
	cat data/complete/free917/free917.dev.tokenized.disambiguatedEntities.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity false \
		> working/free917.dev.forest_split.json
	cat data/complete/free917/free917.test.tokenized.disambiguatedEntities.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity false \
		> working/free917.test.forest_split.json

full_data_to_xukun:
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  data/webquestions/webquestions.examples.train.domains.entity.matches.json.30 > working/dev.txt
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  data/webquestions/webquestions.examples.train.domains.entity.matches.json.70 > working/training.txt
	cat working/webquestions.automaticDismabiguation.test.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt > working/test.txt

webq_to_oscar:
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt > working/dev.txt
	cat working/dev.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.vanilla.freebaseAPIannotations.dev.split.json.txt
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt > working/training.txt
	cat working/training.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.vanilla.freebaseAPIannotations.train.split.json.txt
	cat working/webquestions.automaticDismabiguation.test.pass3.json.txt | python scripts/extract_subset.py  data/webquestions/webquestions.examples.test.domains.entity.matches.json > working/test.txt
	cat working/test.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.vanilla.freebaseAPIannotations.test.split.json.txt

free917_to_oscar:
	cat working/free917.train.forest_split.json \
		| python scripts/cai-yates-preprocessing/add_answers.py data/complete/free917/free917.answers.txt \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> working/free917.train.gold_entities.json.txt
	cat working/free917.dev.forest_split.json \
		| python scripts/cai-yates-preprocessing/add_answers.py data/complete/free917/free917.answers.txt \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> working/free917.dev.gold_entities.json.txt
	cat working/free917.test.forest_split.json \
		| python scripts/cai-yates-preprocessing/add_answers.py data/complete/free917/free917.answers.txt \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> working/free917.test.gold_entities.json.txt

clueweb_to_deplamba_%:
	zcat ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.$*.gz \
		| python scripts/cleaning/filter_sentences_with_no_parses.py \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		| gzip > working/ClueWeb09_1-sentences.$*.gz


convert_deplambda_clueweb_to_gp_forest:
	zcat working/clueweb-1shard-lambdas.json.txt.gz \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates.py \
		| python scripts/dependency_semantic_parser/collapse_copula_relation.py \
		| gzip > data/complete/clueweb/clueweb_deplambda_singletype.txt.gz

	zcat data/complete/clueweb/clueweb_deplambda_singletype.txt.gz | python scripts/split_file.py 14740240 4 ../data0/clueweb/clueweb_deplambda_singletype
	gzip ../data0/clueweb/clueweb_deplambda_singletype.0
	gzip ../data0/clueweb/clueweb_deplambda_singletype.1
	gzip ../data0/clueweb/clueweb_deplambda_singletype.2
	gzip ../data0/clueweb/clueweb_deplambda_singletype.3

extract_deplambda_lexicon_clueweb_split_%:
	zcat ../data0/clueweb/clueweb_deplambda_singletype.$*.gz \
	| python scripts/run_batch_process.py \
		"java -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
		-endpoint localhost \
		-schema data/freebase/schema/all_domains_schema.txt \
		-semanticParseKey dependency_lambda \
		-typeKey "fb:type.object.type" \
		-nthreads 40 \
		-ignoreTypes true" \
		100000 \
	| gzip > ../data0/clueweb/ClueWeb09_1-lexicon.deplambda.txt.$*.gz

convert_deplambda_to_gp_forest:
	cat working/wq-train-lambdas.json.txt working/wq-dev-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates.py \
		| python scripts/dependency_semantic_parser/collapse_copula_relation.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> working/webquestions-train-full-lambdas.json.txt
	python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.30 \
   		< working/webquestions-train-full-lambdas.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt 
	python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.70 \
   		< working/webquestions-train-full-lambdas.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt 
	cat working/wq-test-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates.py \
		| python scripts/dependency_semantic_parser/collapse_copula_relation.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.test.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt 

convert_deplambda_to_gp_forest_with_coupla:
	cat working/wq-train-lambdas.json.txt working/wq-dev-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> working/webquestions-train-full-lambdas.json.txt
	python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.30 \
   		< working/webquestions-train-full-lambdas.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.dev.pass3.deplambda.singletype.json.txt 
	python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.entity.matches.json.70 \
   		< working/webquestions-train-full-lambdas.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.train.pass3.deplambda.singletype.json.txt 
	cat working/wq-test-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.test.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.test.pass3.deplambda.singletype.json.txt 

free917_convert_deplambda_to_gp_forest:
	cat working/free917-dev-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates.py \
		| python scripts/dependency_semantic_parser/collapse_copula_relation.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/free917.dev.forest_split.json \
		> data/complete/vanilla_gold/free917.dev.deplambda.singletype.json 
	cat working/free917-train-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates.py \
		| python scripts/dependency_semantic_parser/collapse_copula_relation.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/free917.train.forest_split.json \
		> data/complete/vanilla_gold/free917.train.deplambda.singletype.json
	cat working/free917-test-lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| python scripts/dependency_semantic_parser/remove_spurious_predicates.py \
		| python scripts/dependency_semantic_parser/collapse_copula_relation.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/free917.test.forest_split.json \
		> data/complete/vanilla_gold/free917.test.deplambda.singletype.json

data_to_xukun:
	cat working/webquestions.automaticDismabiguation.test.pass3.json.txt \
		| python scripts/extract_subset.py data/tacl/vanilla_one_best/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.tacl.json \
		> working/webquestions.automaticDismabiguation.test.pass3.taclSubset.json.txt 
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		| python scripts/extract_subset.py data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.915 \
		> working/webquestions.automaticDismabiguation.train.pass3.taclSubset.json.txt 
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		| python scripts/extract_subset.py data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.200 \
		> working/webquestions.automaticDismabiguation.dev.pass3.taclSubset.json.txt 

# Spanish Experiments

# Tokenize Entities
tokenize_spanish_entities:
	zcat data/freebase/spanish/spanish_business_entities.txt.gz | java -cp lib/*:bin others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_business_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_film_entities.txt.gz | java -cp lib/*:bin others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_film_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_people_entities.txt.gz | java -cp lib/*:bin others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_people_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_business_entities.tokenized.txt.gz data/freebase/spanish/spanish_film_entities.tokenized.txt.gz data/freebase/spanish/spanish_people_entities.tokenized.txt.gz | gzip > data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz

extract_spanish_sentences:
	bzcat data/bravas/extracted/AA/wiki_00.bz2 \
		| java -cp lib/*:bin others.SpanishTokenizer                 \
		| perl -pe 's|=LRB=.*?=RRB=||g'                 \
		| grep -v =LRB= | grep -v =RRB=                 \
		| python scripts/spanish/select_sentences_with_entities_in_relation.py data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt                 \
		| python scripts/spanish/select_sentences_with_non_adjacent_main_relation.py data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt \
		| java -cp lib/*:bin others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		| gzip > data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz

create_spanish_deplambda_format:
	zcat data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz \
		| python scripts/spanish/create-entity-mention-format.py \
		| gzip \
		> ../working/spanish_wikipedia.txt.gz

# Spanish WebQuestions Processing
entity_tag_spanish:
	python scripts/spanish/combine_wq_entities_spanish_english.py data/spanish_webquestions/webquestions.lexicon.txt data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz > data/spanish_webquestions/webquestions.lexicon.extended.txt
	cat data/spanish_webquestions/webquestions.examples.test.utterances_es \
		| java -cp lib/*:bin others.SpanishTokenizerEol \
		| python scripts/spanish/annotate_entities_maximal_string_matching.py data/spanish_webquestions/webquestions.lexicon.extended.txt \
		| python scripts/spanish/add_sentences.py data/spanish_webquestions/webquestions.examples.test.utterances data/spanish_webquestions/webquestions.examples.test.utterances_es \
		| python scripts/spanish/merge_english_annotations.py data/webquestions/webquestions.examples.test.domains.json \
		| java -cp lib/*:bin others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		> data/spanish_webquestions/webquestions.examples.test.es.json
	cat data/spanish_webquestions/webquestions.examples.train.utterances_es \
		| java -cp lib/*:bin others.SpanishTokenizerEol \
		| python scripts/spanish/annotate_entities_maximal_string_matching.py data/spanish_webquestions/webquestions.lexicon.extended.txt \
		| python scripts/spanish/add_sentences.py data/spanish_webquestions/webquestions.examples.train.utterances data/spanish_webquestions/webquestions.examples.train.utterances_es \
		| python scripts/spanish/merge_english_annotations.py data/webquestions/webquestions.examples.train.domains.json \
		| java -cp lib/*:bin others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		> data/spanish_webquestions/webquestions.examples.train.es.json

create_spanish_splits:
	python scripts/spanish/select_splits_from_english.py data/spanish_webquestions/webquestions.examples.train.es.json data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 > data/spanish_webquestions/webquestions.examples.business_film_people.train.json.txt
	python scripts/spanish/select_splits_from_english.py data/spanish_webquestions/webquestions.examples.train.es.json data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 > data/spanish_webquestions/webquestions.examples.business_film_people.dev.json.txt
	python scripts/spanish/select_splits_from_english.py data/spanish_webquestions/webquestions.examples.test.es.json data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json > data/spanish_webquestions/webquestions.examples.business_film_people.test.json.txt
	cat data/spanish_webquestions/webquestions.examples.business_film_people.train.json.txt \
		| python scripts/spanish/create-entity-mention-format.py \
		> working/webquestions.es.examples.business_film_people.train.json.txt
	cat data/spanish_webquestions/webquestions.examples.business_film_people.dev.json.txt \
		| python scripts/spanish/create-entity-mention-format.py \
		> working/webquestions.es.examples.business_film_people.dev.json.txt
	cat data/spanish_webquestions/webquestions.examples.business_film_people.test.json.txt \
		| python scripts/spanish/create-entity-mention-format.py \
		> working/webquestions.es.examples.business_film_people.test.json.txt

## Unsupervised Parsing experiments

clean_data:
	zcat data/distant_eval/test.json.blank.gz | python scripts/cleaning/unannonate_named_entities.py > data/distant_eval/test.json.blank
	gzip data/distant_eval/test.json.blank
	zcat data/distant_eval/test.json.blank.gz | python scripts/cleaning/remove_sentences_with_consecutive_entities.py > data/distant_eval/test.json.blank
	gzip data/distant_eval/test.json.blank
	zcat data/distant_eval/train.json.blank.gz | python scripts/cleaning/remove_sentences_with_consecutive_entities.py > data/distant_eval/train.json.blank
	gzip data/distant_eval/train.json.blank
	zcat data/distant_eval/dev.json.blank.gz | python scripts/cleaning/remove_sentences_with_consecutive_entities.py > data/distant_eval/dev.json.blank
	gzip data/distant_eval/dev.json.blank

clean_unsup_data:
	zcat data/distant_eval/unsupervised_syntax/dev.json.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/unsupervised_syntax/dev.json.blank
	gzip data/distant_eval/unsupervised_syntax/dev.json.blank

	zcat data/distant_eval/unsupervised_syntax/train.json.blank.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/unsupervised_syntax/train.json.blank
	gzip data/distant_eval/unsupervised_syntax/train.json.blank


clean_test_data:
	zcat data/distant_eval/semi_supervised_syntax/test.json.blank.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/semi_supervised_syntax/test.json.blank
	gzip data/distant_eval/semi_supervised_syntax/test.json.blank

	zcat data/distant_eval/unsupervised_syntax/test.json.blank.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/unsupervised_syntax/test.json.blank
	gzip data/distant_eval/unsupervised_syntax/test.json.blank

create_blank_spanish:
	zcat data/distant_eval/spanish/semisup/train.json.gz \
		| python scripts/createBLANK.py \
		| gzip > data/distant_eval/spanish/semisup/train.json.blank.gz

unsupervised_round_2_data:
	zcat data/distant_eval/unsupervised_syntax_round_2/train.json.gz \
		| python scripts/createBLANK.py \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		| gzip > data/distant_eval/unsupervised_syntax_round_2/train.json.blank.gz
	zcat data/distant_eval/unsupervised_syntax_round_2/test.json.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		| gzip > data/distant_eval/unsupervised_syntax_round_2/test.json.blank.gz
	zcat data/distant_eval/unsupervised_syntax_round_2/dev.json.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		| gzip > data/distant_eval/unsupervised_syntax_round_2/dev.json.blank.gz

clean_spanish_data:
	zcat data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz \
		| python scripts/spanish/merge_entity_words_to_one_entity.py \
		| python scripts/spanish/filter_sentences_with_less_than_two_entities.py \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities_spanish.py \
		| gzip > data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.filtered.txt.gz

unsupervised_first_experiment:
	mkdir -p ../working/unsupervised_first_experiment
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
    -iterations 3 \
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
    -eventTypeGrelPartFlag false \
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
    -endpoint localhost \
    -trainingCorpora "data/unsupervised/training/unsupervised_parser.json.noDeps.gz" \
    -logFile ../working/unsupervised_first_experiment/business_film_people.log.txt \
    > ../working/unsupervised_first_experiment/business_film_people.txt

create_candc_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/candc_markedup.modified \
	--unaryRulesFile lib_data/unary_rules.txt \
	--binaryRulesFile lib_data/binary_rules.txt \
	--specialCasesFile lib_data/lexicon_specialCases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/candc_grounded_lexicon.txt \
	> /dev/null

create_semisup_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/semi_supervised_syntax/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/ybisk-semi-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile lib_data/ybisk-specialcases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/semisup_grounded_lexicon.txt \
	> /dev/null

create_spanish_semisup_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/spanish/semisup/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers word \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/ybisk-semi-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile data/distant_eval/spanish/semisup/lexicon_fullSpecialCases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/spanish_semisup_grounded_lexicon.txt \
	> /dev/null

create_unsup_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/unsupervised_syntax/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile lib_data/ybisk-specialcases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/unsup_grounded_lexicon.txt \
	> /dev/null

create_unsup_grounded_lexicon_round_2:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/unsupervised_syntax_round_2/train.json.gz \
	| java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile lib_data/ybisk-specialcases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/unsup_grounded_lexicon_round_2.txt \
	> /dev/null

candc_distant_eval:
	rm -rf ../working/candc_distant_eval
	mkdir -p ../working/candc_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/distant_eval/grounded_lexicon/candc_grounded_lexicon.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 50 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/train.json.blank.gz \
	-devFile data/distant_eval/dev.json.1000.blank.gz \
	-logFile ../working/candc_distant_eval/business_film_people.log.txt \
	> ../working/candc_distant_eval/business_film_people.txt

candc_distant_eval_loaded_model:
	rm -rf ../working/candc_distant_eval_loaded_model
	mkdir -p ../working/candc_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/distant_eval/grounded_lexicon/candc_grounded_lexicon.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-loadModelFromFile ../working/candc_distant_eval.9/business_film_people.log.txt.model.iteration45 \
	-devFile data/distant_eval/dev.json.blank.gz \
	-testFile data/distant_eval/test.json.blank.gz \
	-logFile ../working/candc_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/candc_distant_eval_loaded_model/business_film_people.txt

bow_distant_eval:
	rm -rf ../working/bow_distant_eval
	mkdir -p ../working/bow_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 30 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag true \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -1.0 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/train.json.blank.gz \
	-devFile data/distant_eval/dev.json.1000.blank.gz \
	-logFile ../working/bow_distant_eval/business_film_people.log.txt \
	> ../working/bow_distant_eval/business_film_people.txt

bow_distant_eval_loaded_model:
	rm -rf ../working/bow_distant_eval_loaded_model
	mkdir -p ../working/bow_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag true \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -1.0 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-loadModelFromFile ../working/bow_distant_eval.4/business_film_people.log.txt.model.iteration6 \
	-devFile data/distant_eval/dev.json.blank.gz \
	-testFile data/distant_eval/test.json.blank.gz \
	-logFile ../working/bow_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/bow_distant_eval_loaded_model/business_film_people.txt

semisup_specialcases_distant_eval:
	rm -rf ../working/semisup_specialcases_distant_eval
	mkdir -p ../working/semisup_specialcases_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping lib_data/ybisk-semi-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/semisup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 30 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/semi_supervised_syntax/train.json.blank.gz \
	-devFile data/distant_eval/semi_supervised_syntax/dev.json.1000.blank.gz \
	-logFile ../working/semisup_specialcases_distant_eval/business_film_people.log.txt \
	> ../working/semisup_specialcases_distant_eval/business_film_people.txt

semisup_specialcases_distant_eval_loaded_model:
	rm -rf ../working/semisup_specialcases_distant_eval_loaded_model
	mkdir -p ../working/semisup_specialcases_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping lib_data/ybisk-semi-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/semisup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-loadModelFromFile ../working/semisup_specialcases_distant_eval/business_film_people.log.txt.model.iteration82 \
	-devFile data/distant_eval/semi_supervised_syntax/dev.json.blank.gz \
	-testFile data/distant_eval/semi_supervised_syntax/test.json.blank.gz \
	-logFile ../working/semisup_specialcases_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/semisup_specialcases_distant_eval_loaded_model/business_film_people.txt

unsup_specialcases_distant_eval:
	rm -rf ../working/unsup_specialcases_distant_eval
	mkdir -p ../working/unsup_specialcases_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/unsup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 30 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/unsupervised_syntax/train.json.blank.gz \
	-devFile data/distant_eval/unsupervised_syntax/dev.json.1000.blank.gz \
	-logFile ../working/unsup_specialcases_distant_eval/business_film_people.log.txt \
	> ../working/unsup_specialcases_distant_eval/business_film_people.txt

unsup_specialcases_distant_eval_round_2:
	rm -rf ../working/unsup_specialcases_distant_eval_round_2
	mkdir -p ../working/unsup_specialcases_distant_eval_round_2
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/unsup_grounded_lexicon_round_2.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 30 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/unsupervised_syntax_round_2/train.json.blank.gz \
	-devFile data/distant_eval/unsupervised_syntax_round_2/dev.json.1000.blank.gz \
	-logFile ../working/unsup_specialcases_distant_eval_round_2/business_film_people.log.txt \
	> ../working/unsup_specialcases_distant_eval_round_2/business_film_people.txt

unsup_specialcases_distant_eval_loaded_model:
	rm -rf ../working/unsup_specialcases_distant_eval_loaded_model
	mkdir -p ../working/unsup_specialcases_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/unsup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-loadModelFromFile ../working/unsup_specialcases_distant_eval/business_film_people.log.txt.model.iteration74 \
	-devFile data/distant_eval/unsupervised_syntax/dev.json.blank.gz \
	-testFile data/distant_eval/unsupervised_syntax/test.json.blank.gz \
	-logFile ../working/unsup_specialcases_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/unsup_specialcases_distant_eval_loaded_model/business_film_people.txt

distant_evaluation:
	python scripts/error-analysis/entity_wise_results.py data/distant_eval/unsupervised_syntax_round_2/dev.json.1000.blank.gz ~/Dropbox/SivaData/unsup_best_model_round_2/business_film_people.log.txt.eval.dev.iteration5.1best.answers.txt


spanish_bow_distant_eval:
	rm -rf ../working/spanish_bow_distant_eval
	mkdir -p ../working/spanish_bow_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 30 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag true \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -1.0 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/spanish/bow/train.json.blank.gz \
	-devFile data/distant_eval/spanish/bow/dev.json.1000.blank.gz \
	-logFile ../working/spanish_bow_distant_eval/business_film_people.log.txt \
	> ../working/spanish_bow_distant_eval/business_film_people.txt

spanish_semisup_specialcases_distant_eval:
	rm -rf ../working/spanish_semisup_specialcases_distant_eval
	mkdir -p ../working/spanish_semisup_specialcases_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping lib_data/ybisk-semi-mapping.txt \
	-ccgLexicon data/distant_eval/spanish/semisup/lexicon_fullSpecialCases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/spanish_semisup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 30 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/spanish/semisup/train.json.blank.gz \
	-devFile data/distant_eval/spanish/semisup/dev.json.1000.blank.gz \
	-logFile ../working/spanish_semisup_specialcases_distant_eval/business_film_people.log.txt \
	> ../working/spanish_semisup_specialcases_distant_eval/business_film_people.txt

create_old_to_new_mid_mappings:
	zcat kinloch:/gpfs/scratch/users/s1051585/freebase/freebase-20150720.gz | grep dataworld.gardening_hint.replaced_by | cut -f1,3 | python scripts/freebase/clean_old_mid_to_mid.py | gzip > data/freebase/freebase_20150720_old_to_new_mid.txt.gz
	zcat kinloch:/disk/scratch/users/s1051585/data/freebase-cleaned.rdf-2013-08-11-00-00.gz | grep dataworld.gardening_hint.replaced_by | cut -f1,3 | python scripts/freebase/clean_old_mid_to_mid.py | gzip > data/freebase/freebase_20130811_old_to_new_mid.txt.gz	
	zcat ../data/freebase_sempre.ttl.gz | cut -f1,3 | python scripts/freebase/extract_entities_from_rdf_freebase.py | less

### WebQuestions complete data experiments ###
extract_gold_graphs_deplambda_singletype_dev:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	true \
		true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/gold_graphs/deplambda_singletype_with_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_singletype_without_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	false \
		true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/gold_graphs/deplambda_singletype_without_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/gold_graphs/deplambda_singletype_with_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_without_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	false \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/gold_graphs/deplambda_singletype_without_merge_without_expand.dev.answers.txt

extract_gold_graphs_deplambda_singletype:
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge_with_expand.full \
		lib_data/dummy.txt \
	   	true \
		true \
		> data/gold_graphs/deplambda_singletype_with_merge_with_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_singletype_without_merge_with_expand.full \
		lib_data/dummy.txt \
	   	false \
		true \
		> data/gold_graphs/deplambda_singletype_without_merge_with_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
		false \
		> data/gold_graphs/deplambda_singletype_with_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_singletype_without_merge_without_expand.full \
		lib_data/dummy.txt \
	   	false \
		false \
		> data/gold_graphs/deplambda_singletype_without_merge_without_expand.full.answers.txt

extract_gold_graphs_deplambda_singletype_with_lexicon:
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge_with_expand_with_lexicon.full \
		data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz \
		true \
		true \
		> data/gold_graphs/deplambda_singletype_with_merge_with_expand_with_lexicon.full.answers.txt

extract_gold_graphs_dependency_dev:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_without_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	false \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/gold_graphs/dependency_without_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/gold_graphs/dependency_with_merge_without_expand.dev.answers.txt

extract_gold_graphs_dependency:
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_without_merge_without_expand.full \
		lib_data/dummy.txt \
	   	false \
		false \
		> data/gold_graphs/dependency_without_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
		false \
		> data/gold_graphs/dependency_with_merge_without_expand.full.answers.txt

extract_gold_graphs_ccg_dev:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/gold_graphs/ccg_with_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	true \
		true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/gold_graphs/ccg_with_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_without_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	false \
		true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/gold_graphs/ccg_without_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_without_merge_without_expand.dev \
		lib_data/dummy.txt \
		false \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/gold_graphs/ccg_without_merge_without_expand.dev.answers.txt

extract_gold_graphs_ccg:
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
		false \
		> data/gold_graphs/ccg_with_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge_with_expand.full \
		lib_data/dummy.txt \
	   	true \
		true \
		> data/gold_graphs/ccg_with_merge_with_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_without_merge_without_expand.full \
		lib_data/dummy.txt \
		false \
		false \
		> data/gold_graphs/ccg_without_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_without_merge_with_expand.full \
		lib_data/dummy.txt \
		false \
		true \
		> data/gold_graphs/ccg_without_merge_with_expand.full.answers.txt

extract_gold_graphs_ccg_with_lexicon:
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge_with_expand_with_lexicon.full \
		data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz \
	   	true \
		true \
		> data/gold_graphs/ccg_with_merge_with_expand_with_lexicon.full.answers.txt

extract_gold_graphs_bow_dev:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_without_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	false \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/gold_graphs/bow_without_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/gold_graphs/bow_with_merge_without_expand.dev.answers.txt

extract_gold_graphs_bow:
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_without_merge_without_expand.full \
		lib_data/dummy.txt \
	   	false \
		false \
		> data/gold_graphs/bow_without_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
		false \
		> data/gold_graphs/bow_with_merge_without_expand.full.answers.txt

easyccg_supervised_without_merge_without_expand:
	rm -rf ../working/easyccg_supervised_without_merge_without_expand
	mkdir -p ../working/easyccg_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/ccg_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/easyccg_supervised_without_merge_without_expand/all.log.txt \
	> ../working/easyccg_supervised_without_merge_without_expand/all.txt

easyccg_supervised_without_merge_with_expand:
	rm -rf ../working/easyccg_supervised_without_merge_with_expand
	mkdir -p ../working/easyccg_supervised_without_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/ccg_without_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/easyccg_supervised_without_merge_with_expand/all.log.txt \
	> ../working/easyccg_supervised_without_merge_with_expand/all.txt

easyccg_supervised_with_merge_without_expand:
	rm -rf ../working/easyccg_supervised_with_merge_without_expand
	mkdir -p ../working/easyccg_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	-goldParsesFile data/gold_graphs/ccg_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/easyccg_supervised_with_merge_without_expand/all.log.txt \
	> ../working/easyccg_supervised_with_merge_without_expand/all.txt

easyccg_supervised_with_merge_with_expand:
	rm -rf ../working/easyccg_supervised_with_merge_with_expand
	mkdir -p ../working/easyccg_supervised_with_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useBackOffGraph true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	-goldParsesFile data/gold_graphs/ccg_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/easyccg_supervised_with_merge_with_expand/all.log.txt \
	> ../working/easyccg_supervised_with_merge_with_expand/all.txt

easyccg_supervised_with_merge_with_expand_with_lexicon:
	rm -rf ../working/easyccg_supervised_with_merge_with_expand_with_lexicon
	mkdir -p ../working/easyccg_supervised_with_merge_with_expand_with_lexicon
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useBackOffGraph true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	-goldParsesFile data/gold_graphs/ccg_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/easyccg_supervised_with_merge_with_expand_with_lexicon/all.log.txt \
	> ../working/easyccg_supervised_with_merge_with_expand_with_lexicon/all.txt

easyccg_supervised_with_merge_with_expand_with_lexicon_full:
	rm -rf ../working/easyccg_supervised_with_merge_with_expand_with_lexicon_full
	mkdir -p ../working/easyccg_supervised_with_merge_with_expand_with_lexicon_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useBackOffGraph true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/ccg_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/easyccg_supervised_with_merge_with_expand_with_lexicon_full/all.log.txt \
	> ../working/easyccg_supervised_with_merge_with_expand_with_lexicon_full/all.txt

easyccg_supervised_with_merge_with_expand_full:
	rm -rf ../working/easyccg_supervised_with_merge_with_expand_full
	mkdir -p ../working/easyccg_supervised_with_merge_with_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/ccg_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/easyccg_supervised_with_merge_with_expand_full/all.log.txt \
	> ../working/easyccg_supervised_with_merge_with_expand_full/all.txt

easyccg_mwg:
	rm -rf ../working/easyccg_mwg
	mkdir -p ../working/easyccg_mwg
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-evaluateBeforeTraining true \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/easyccg_mwg/all.log.txt \
	> ../working/easyccg_mwg/all.txt

deplambda_singletype_supervised_without_merge_with_expand:
	rm -rf ../working/deplambda_singletype_supervised_without_merge_with_expand
	mkdir -p ../working/deplambda_singletype_supervised_without_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_without_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_without_merge_with_expand/all.log.txt \
	> ../working/deplambda_singletype_supervised_without_merge_with_expand/all.txt

deplambda_singletype_supervised_with_merge_with_expand:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_with_expand
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_with_expand/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_with_expand/all.txt

deplambda_singletype_supervised_with_merge_with_expand_with_lexicon:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon/all.txt

deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt" \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full/all.txt



deplambda_singletype_supervised_with_merge_with_copulath_expand:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula/all.txt

deplambda_singletype_supervised_with_merge_with_copula_with_lexicon:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula_with_lexicon
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula_with_lexicon
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula_with_lexicon/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_with_expand_with_copula_with_lexicon/all.txt

deplambda_singletype_supervised_without_merge_without_expand:
	rm -rf ../working/deplambda_singletype_supervised_without_merge_without_expand
	mkdir -p ../working/deplambda_singletype_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-useBackOffGraph false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_without_merge_without_expand/all.log.txt \
	> ../working/deplambda_singletype_supervised_without_merge_without_expand/all.txt

deplambda_singletype_supervised_with_merge_without_expand:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_without_expand
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_without_expand/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_without_expand/all.txt

deplambda_singletype_supervised_with_merge_with_expand_full:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_with_expand_full
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_with_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.train.pass3.deplambda.singletype.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.dev.pass3.deplambda.singletype.json.txt" \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.with_copula.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_with_expand_full/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_with_expand_full/all.txt

deplambda_singletype_supervised_without_merge_without_expand_full:
	rm -rf ../working/deplambda_singletype_supervised_without_merge_without_expand_full
	mkdir -p ../working/deplambda_singletype_supervised_without_merge_without_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-useBackOffGraph false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt" \
	-goldParsesFile data/gold_graphs/deplambda_singletype_without_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_without_merge_without_expand_full/all.log.txt \
	> ../working/deplambda_singletype_supervised_without_merge_without_expand_full/all.txt

deplambda_singletype_supervised_with_merge_without_expand_full:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_without_expand_full
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_without_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph false \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt" \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_without_expand_full/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_without_expand_full/all.txt

bow_supervised_without_merge_without_expand:
	rm -rf ../working/bow_supervised_without_merge_without_expand
	mkdir -p ../working/bow_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/bow_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/bow_supervised_without_merge_without_expand/all.log.txt \
	> ../working/bow_supervised_without_merge_without_expand/all.txt

bow_supervised_with_merge_without_expand:
	rm -rf ../working/bow_supervised_with_merge_without_expand
	mkdir -p ../working/bow_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/bow_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-logFile ../working/bow_supervised_with_merge_without_expand/all.log.txt \
	> ../working/bow_supervised_with_merge_without_expand/all.txt

bow_supervised_without_merge_without_expand_full:
	rm -rf ../working/bow_supervised_without_merge_without_expand_full
	mkdir -p ../working/bow_supervised_without_merge_without_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt" \
	-goldParsesFile data/gold_graphs/bow_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/bow_supervised_without_merge_without_expand_full/all.log.txt \
	> ../working/bow_supervised_full/all.txt

dependency_with_merge_without_expand.full:
	rm -rf ../working/dependency_with_merge_without_expand.full
	mkdir -p ../working/dependency_with_merge_without_expand.full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging true \
	-handleEventEventEdges true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt;data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt" \
	-goldParsesFile data/gold_graphs/dependency_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/dependency_with_merge_without_expand.full/all.log.txt \
	> ../working/dependency_with_merge_without_expand.full/all.txt

dependency_with_merge_without_expand:
	rm -rf ../working/dependency_with_merge_without_expand
	mkdir -p ../working/dependency_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging true \
	-handleEventEventEdges true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt" \
	-goldParsesFile data/gold_graphs/dependency_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-logFile ../working/dependency_with_merge_without_expand/all.log.txt \
	> ../working/dependency_with_merge_without_expand/all.txt

dependency_without_merge_without_expand:
	rm -rf ../working/dependency_without_merge_without_expand
	mkdir -p ../working/dependency_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging false \
	-handleEventEventEdges true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt" \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/dependency_without_merge_without_expand.full.ser \
	-logFile ../working/dependency_without_merge_without_expand/all.log.txt \
	> ../working/dependency_without_merge_without_expand/all.txt

create_deplambda_lexicon:
	cat ~/Downloads/clueweb-subset-academic-lexicon.json | sed -e "s/ u\"/ \"/g" | sed -e "s/ u'/ \"/g" | sed -e "s/{'/{\"/g" | sed -e "s/' /\" /g" | sed -e "s/':/\":/g" | sed -e "s/ '/ \"/g" | sed -e "s/',/\",/g" | java -cp lib/*:bin/ in.sivareddy.scripts.ConvertDepLambdaGroundedLexiconToGraphParser data/freebase/schema/all_domains_schema.txt > data/deplambda/clueweb-subset-academic-grounded_lexicon.txt

# Free917 experiments

# Dump most frequent types in Freebase
dump_most_frequent_freebase_types:
	java -cp lib/*:bin in.sivareddy.scripts.GetMostFrequentFreebaseTypes > data/freebase/stats/freebase_most_frequent_types.txt

free917_extract_gold_graphs_ccg:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_gold/free917.dev.easyccg.json \
		> data/gold_graphs/free917_ccg_with_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_without_merge_without_expand.dev \
		lib_data/dummy.txt \
		false \
		false \
		< data/complete/vanilla_gold/free917.dev.easyccg.json \
		> data/gold_graphs/free917_ccg_without_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_with_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	true \
	   	true \
		< data/complete/vanilla_gold/free917.dev.easyccg.json \
		> data/gold_graphs/free917_ccg_with_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_without_merge_with_expand.dev \
		lib_data/dummy.txt \
		false \
		true \
		< data/complete/vanilla_gold/free917.dev.easyccg.json \
		> data/gold_graphs/free917_ccg_without_merge_with_expand.dev.answers.txt

	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
		false \
		> data/gold_graphs/free917_ccg_with_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
    	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_without_merge_without_expand.full \
		lib_data/dummy.txt \
		false \
		false \
		> data/gold_graphs/free917_ccg_without_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_with_merge_with_expand.full \
		lib_data/dummy.txt \
	   	true \
		true \
		> data/gold_graphs/free917_ccg_with_merge_with_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_without_merge_with_expand.full \
		lib_data/dummy.txt \
	   	false \
		true \
		> data/gold_graphs/free917_ccg_without_merge_with_expand.full.answers.txt

free917_extract_gold_graphs_ccg_with_lexicon:
	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		synPars \
		data/gold_graphs/free917_ccg_with_merge_with_expand_with_lexicon.full \
		data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz \
	   	true \
		true \
		> data/gold_graphs/free917_ccg_with_merge_with_expand_with_lexicon.full.answers.txt

free917_extract_gold_graphs_deplambda_singletype:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	true \
		true \
		< data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
		> data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_without_merge_with_expand.dev \
		lib_data/dummy.txt \
	   	false \
		true \
		< data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
		> data/gold_graphs/free917_deplambda_singletype_without_merge_with_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
		> data/gold_graphs/free917_deplambda_singletype_with_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_without_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	false \
		false \
		< data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
		> data/gold_graphs/free917_deplambda_singletype_without_merge_without_expand.dev.answers.txt

	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand.full \
		lib_data/dummy.txt \
	   	true \
		true \
		> data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
	   	false \
		> data/gold_graphs/free917_deplambda_singletype_with_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_without_merge_with_expand.full \
		lib_data/dummy.txt \
	   	false \
		true \
		> data/gold_graphs/free917_deplambda_singletype_without_merge_with_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_without_merge_without_expand.full \
		lib_data/dummy.txt \
	   	false \
		false \
		> data/gold_graphs/free917_deplambda_singletype_without_merge_without_expand.full.answers.txt

free917_extract_gold_graphs_deplambda_singletype_with_lexicon:
	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_lambda \
		data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand_with_lexicon.full \
		data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz \
	   	true \
		true \
		> data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand_with_lexicon.full.answers.txt

free917_extract_gold_graphs_dependency:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_question_graph \
		data/gold_graphs/free917_dependency_with_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	true \
		false \
		< data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
		> data/gold_graphs/free917_dependency_with_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_question_graph \
		data/gold_graphs/free917_dependency_without_merge_without_expand.dev \
		lib_data/dummy.txt \
	   	false \
		false \
		< data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
		> data/gold_graphs/free917_dependency_without_merge_without_expand.dev.answers.txt

	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_question_graph \
		data/gold_graphs/free917_dependency_with_merge_without_expand.full \
		lib_data/dummy.txt \
	   	true \
		false \
		> data/gold_graphs/free917_dependency_with_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.deplambda.singletype.json \
		data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		dependency_question_graph \
		data/gold_graphs/free917_dependency_without_merge_without_expand.full \
		lib_data/dummy.txt \
	   	false \
		false \
		> data/gold_graphs/free917_dependency_without_merge_without_expand.full.answers.txt

free917_extract_gold_graphs_bow:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		bow_question_graph \
		data/gold_graphs/free917_bow_without_merge_without_expand.dev \
		lib_data/dummy.txt \
		false \
		false \
		< data/complete/vanilla_gold/free917.dev.easyccg.json \
		> data/gold_graphs/free917_bow_without_merge_without_expand.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		bow_question_graph \
		data/gold_graphs/free917_bow_with_merge_without_expand.dev \
		lib_data/dummy.txt \
		true \
		false \
		< data/complete/vanilla_gold/free917.dev.easyccg.json \
		> data/gold_graphs/free917_bow_with_merge_without_expand.dev.answers.txt

	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
    	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		bow_question_graph \
		data/gold_graphs/free917_bow_without_merge_without_expand.full \
		lib_data/dummy.txt \
		false \
		false \
		> data/gold_graphs/free917_bow_without_merge_without_expand.full.answers.txt
	cat data/complete/vanilla_gold/free917.train.easyccg.json data/complete/vanilla_gold/free917.dev.easyccg.json \
    	| java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema_free917.txt localhost \
		bow_question_graph \
		data/gold_graphs/free917_bow_with_merge_without_expand.full \
		lib_data/dummy.txt \
		true \
		false \
		> data/gold_graphs/free917_bow_with_merge_without_expand.full.answers.txt

free917_bow_supervised_without_merge_without_expand:
	rm -rf ../working/free917_bow_supervised_without_merge_without_expand
	mkdir -p ../working/free917_bow_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_gold/free917.train.easyccg.json \
	-goldParsesFile data/gold_graphs/free917_bow_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_bow_supervised_without_merge_without_expand/all.log.txt \
	> ../working/free917_bow_supervised_without_merge_without_expand/all.txt

free917_bow_supervised_with_merge_without_expand:
	rm -rf ../working/free917_bow_supervised_with_merge_without_expand
	mkdir -p ../working/free917_bow_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_gold/free917.train.easyccg.json \
	-goldParsesFile data/gold_graphs/free917_bow_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_bow_supervised_with_merge_without_expand/all.log.txt \
	> ../working/free917_bow_supervised_with_merge_without_expand/all.txt

free917_easyccg_supervised_with_merge_without_expand:
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand
	mkdir -p ../working/free917_easyccg_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_with_merge_without_expand/all.log.txt \
	> ../working/free917_easyccg_supervised_with_merge_without_expand/all.txt

free917_easyccg_supervised_with_merge_with_expand_full:
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand_full
	mkdir -p ../working/free917_easyccg_supervised_with_merge_with_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useBackOffGraph true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json;data/complete/vanilla_gold/free917.dev.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-testFile data/complete/vanilla_gold/free917.test.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_with_merge_with_expand_full/all.log.txt \
	> ../working/free917_easyccg_supervised_with_merge_with_expand_full/all.txt

free917_easyccg_supervised_with_merge_with_expand:
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand
	mkdir -p ../working/free917_easyccg_supervised_with_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useBackOffGraph true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_with_merge_with_expand/all.log.txt \
	> ../working/free917_easyccg_supervised_with_merge_with_expand/all.txt

free917_easyccg_supervised_with_merge_with_expand_with_lexicon_full:
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand_with_lexicon_full
	mkdir -p ../working/free917_easyccg_supervised_with_merge_with_expand_with_lexicon_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useBackOffGraph true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json;data/complete/vanilla_gold/free917.dev.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-testFile data/complete/vanilla_gold/free917.test.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_with_merge_with_expand_with_lexicon_full/all.log.txt \
	> ../working/free917_easyccg_supervised_with_merge_with_expand_with_lexicon_full/all.txt

free917_easyccg_supervised_with_merge_without_expand_with_lexicon:
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand_with_lexicon
	mkdir -p ../working/free917_easyccg_supervised_with_merge_without_expand_with_lexicon
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_with_merge_without_expand_with_lexicon/all.log.txt \
	> ../working/free917_easyccg_supervised_with_merge_without_expand_with_lexicon/all.txt

free917_easyccg_supervised_without_merge_without_expand:
	rm -rf ../working/free917_easyccg_supervised_without_merge_without_expand
	mkdir -p ../working/free917_easyccg_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_without_merge_without_expand/all.log.txt \
	> ../working/free917_easyccg_supervised_without_merge_without_expand/all.txt

free917_easyccg_supervised_without_merge_with_expand:
	rm -rf ../working/free917_easyccg_supervised_without_merge_with_expand
	mkdir -p ../working/free917_easyccg_supervised_without_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.easyccg.json" \
	-goldParsesFile data/gold_graphs/free917_ccg_without_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.easyccg.json \
	-logFile ../working/free917_easyccg_supervised_without_merge_with_expand/all.log.txt \
	> ../working/free917_easyccg_supervised_without_merge_with_expand/all.txt

free917_deplambda_singletype_supervised_with_merge_with_expand:
	rm -rf ../working/free917_deplambda_singletype_supervised_with_merge_with_expand
	mkdir -p ../working/free917_deplambda_singletype_supervised_with_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-allowMerging true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;" \
	-goldParsesFile data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-logFile ../working/free917_deplambda_singletype_supervised_with_merge_with_expand/all.log.txt \
	> ../working/free917_deplambda_singletype_supervised_with_merge_with_expand/all.txt

free917_deplambda_singletype_supervised_without_merge_with_expand:
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_with_expand
	mkdir -p ../working/free917_deplambda_singletype_supervised_without_merge_with_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-allowMerging false \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;" \
	-goldParsesFile data/gold_graphs/free917_deplambda_singletype_without_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-logFile ../working/free917_deplambda_singletype_supervised_without_merge_with_expand/all.log.txt \
	> ../working/free917_deplambda_singletype_supervised_without_merge_with_expand/all.txt

free917_deplambda_singletype_supervised_without_merge_without_expand:
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_without_expand
	mkdir -p ../working/free917_deplambda_singletype_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-handleEventEventEdges true \
	-useBackOffGraph false \
	-allowMerging false \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;" \
	-goldParsesFile data/gold_graphs/free917_deplambda_singletype_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-logFile ../working/free917_deplambda_singletype_supervised_without_merge_without_expand/all.log.txt \
	> ../working/free917_deplambda_singletype_supervised_without_merge_without_expand/all.txt

free917_deplambda_singletype_supervised_with_merge_without_expand:
	rm -rf ../working/free917_deplambda_singletype_supervised_with_merge_without_expand
	mkdir -p ../working/free917_deplambda_singletype_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-allowMerging false \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;" \
	-goldParsesFile data/gold_graphs/free917_deplambda_singletype_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-logFile ../working/free917_deplambda_singletype_supervised_with_merge_without_expand/all.log.txt \
	> ../working/free917_deplambda_singletype_supervised_with_merge_without_expand/all.txt

free917_deplambda_singletype_supervised_with_merge_with_expand_full:
	rm -rf ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_full
	mkdir -p ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-allowMerging true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;data/complete/vanilla_gold/free917.dev.deplambda.singletype.json" \
	-goldParsesFile data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-testFile data/complete/vanilla_gold/free917.test.deplambda.singletype.json \
	-logFile ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_full/all.log.txt \
	> ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_full/all.txt

free917_deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full:
	rm -rf ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full
	mkdir -p ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon data/complete/grounded_lexicon/deplambda_grounded_lexicon.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-allowMerging true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;data/complete/vanilla_gold/free917.dev.deplambda.singletype.json" \
	-goldParsesFile data/gold_graphs/free917_deplambda_singletype_with_merge_with_expand_with_lexicon.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-testFile data/complete/vanilla_gold/free917.test.deplambda.singletype.json \
	-logFile ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full/all.log.txt \
	> ../working/free917_deplambda_singletype_supervised_with_merge_with_expand_with_lexicon_full/all.txt

free917_dependency_supervised_without_merge_without_expand:
	rm -rf ../working/free917_dependency_supervised_without_merge_without_expand
	mkdir -p ../working/free917_dependency_supervised_without_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json" \
	-goldParsesFile data/gold_graphs/free917_dependency_without_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-logFile ../working/free917_dependency_supervised_without_merge_without_expand/all.log.txt \
	> ../working/free917_dependency_supervised_without_merge_without_expand/all.txt

free917_dependency_supervised_with_merge_without_expand:
	rm -rf ../working/free917_dependency_supervised_with_merge_without_expand
	mkdir -p ../working/free917_dependency_supervised_with_merge_without_expand
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json" \
	-goldParsesFile data/gold_graphs/free917_dependency_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-logFile ../working/free917_dependency_supervised_with_merge_without_expand/all.log.txt \
	> ../working/free917_dependency_supervised_with_merge_without_expand/all.txt

free917_dependency_supervised_with_merge_without_expand_full:
	rm -rf ../working/free917_dependency_supervised_with_merge_without_expand_full
	mkdir -p ../working/free917_dependency_supervised_with_merge_without_expand_full
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema_free917.txt \
	-relationTypesFile data/dummy.txt \
	-mostFrequentTypesFile data/freebase/stats/freebase_most_frequent_types.txt \
	-lexicon lib_data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 3 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag false \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag true \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useAnswerTypeQuestionWordFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag false \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus "data/complete/vanilla_gold/free917.train.deplambda.singletype.json;data/complete/vanilla_gold/free917.dev.deplambda.singletype.json" \
	-goldParsesFile data/gold_graphs/free917_dependency_with_merge_without_expand.full.ser \
	-devFile data/complete/vanilla_gold/free917.dev.deplambda.singletype.json \
	-testFile data/complete/vanilla_gold/free917.test.deplambda.singletype.json \
	-logFile ../working/free917_dependency_supervised_with_merge_without_expand_full/all.log.txt \
	> ../working/free917_dependency_supervised_with_merge_without_expand_full/all.txt

# TACL 2015 camera ready experiments

free917_easyccg_dev_table:
	make free917_extract_gold_graphs_ccg
	
	make free917_easyccg_supervised_without_merge_without_expand
	rm -rf ../working/free917_easyccg_supervised_without_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_easyccg_supervised_without_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_easyccg_supervised_without_merge_without_expand/*model.iteration*
	
	make free917_easyccg_supervised_without_merge_with_expand
	rm -rf ../working/free917_easyccg_supervised_without_merge_with_expand/*dev.iteration*
	rm -rf ../working/free917_easyccg_supervised_without_merge_with_expand/*train.iteration*
	rm -rf ../working/free917_easyccg_supervised_without_merge_with_expand/*model.iteration*
	
	make free917_easyccg_supervised_with_merge_without_expand
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand/*model.iteration*
	
	make free917_easyccg_supervised_with_merge_with_expand
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand/*dev.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand/*train.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand/*model.iteration*

free917_deplambda_singletype_dev_table:
	make free917_extract_gold_graphs_deplambda_singletype

	make free917_deplambda_singletype_supervised_without_merge_without_expand
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_without_expand/*model.iteration*

	make free917_deplambda_singletype_supervised_without_merge_with_expand
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_with_expand/*dev.iteration*
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_with_expand/*train.iteration*
	rm -rf ../working/free917_deplambda_singletype_supervised_without_merge_with_expand/*model.iteration*

	make free917_deplambda_singletype_supervised_with_merge_without_expand
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_without_expand/*model.iteration*

	make free917_deplambda_singletype_supervised_with_merge_with_expand
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand/*dev.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand/*train.iteration*
	rm -rf ../working/free917_easyccg_supervised_with_merge_with_expand/*model.iteration*

free917_dependency_dev_table:
	make free917_extract_gold_graphs_dependency
	
	make free917_dependency_supervised_without_merge_without_expand
	rm -rf ../working/free917_dependency_supervised_without_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_dependency_supervised_without_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_dependency_supervised_without_merge_without_expand/*model.iteration*

	make free917_dependency_supervised_with_merge_without_expand
	rm -rf ../working/free917_dependency_supervised_with_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_dependency_supervised_with_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_dependency_supervised_with_merge_without_expand/*model.iteration*

free917_bow_dev_table:
	make free917_extract_gold_graphs_bow

	make free917_bow_supervised_without_merge_without_expand
	rm -rf ../working/free917_bow_supervised_without_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_bow_supervised_without_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_bow_supervised_without_merge_without_expand/*model.iteration*

	make free917_bow_supervised_with_merge_without_expand
	rm -rf ../working/free917_bow_supervised_with_merge_without_expand/*dev.iteration*
	rm -rf ../working/free917_bow_supervised_with_merge_without_expand/*train.iteration*
	rm -rf ../working/free917_bow_supervised_with_merge_without_expand/*model.iteration*


