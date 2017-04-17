classify-sentences:
	zcat ../distant_eval/parses/easyccg/dev.json.blank.cleaned.gz \
		| python classify-sentences.py \
		> dev-syntactic-classification.json.txt
	zcat ../distant_eval/parses/easyccg/test.json.blank.cleaned.gz \
		| python classify-sentences.py \
		> test-syntactic-classification.json.txt
	zcat ../distant_eval/parses/easyccg/train.json.blank.cleaned.gz \
		| python classify-sentences.py \
		> train-syntactic-classification.json.txt

