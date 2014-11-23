#!/bin/sh
scriptdir=`dirname $0`

# java -mx2048m -cp "$scriptdir/stanford-ner_no_sentence_tokenisation.jar:" edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier $scriptdir/classifiers/english.all.3class.nodistsim.crf.ser.gz -tokenizerFactory edu.stanford.nlp.process.WhitespaceTokenizer -tokenizerOptions "tokenizeNLs=true" -textFile $1


java -mx2048m -cp "$scriptdir/stanford-ner_no_sentence_tokenisation.jar:" edu.stanford.nlp.ie.crf.CRFClassifier -loadClassifier $scriptdir/classifiers/english.muc.7class.distsim.crf.ser.gz -tokenizerFactory edu.stanford.nlp.process.WhitespaceTokenizer -tokenizerOptions "tokenizeNLs=false" -textFile $1
