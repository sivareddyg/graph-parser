# GraphParser 

GraphParser is decribed in the paper http://sivareddy.in/papers/reddy2014semanticparsing.pdf

## Installation

GraphParser is written in Java, and requires few external java libraries. You can install them using

> ./install.py ungrounded

## Ungrounded Semantic Parser

GraphParser can parse natural language sentences to logical parses and graphs. Run the following command

> cat input.txt | sh run.sh

## Online demo
Online demo of GraphParser can be accessed at http://sivareddy.in/graphparser.html

## Evaluation datasets
If you are interested in evaluation datasets, and the output of GraphParser on the test datasets, you can download them using

> ./install.py evaluation

The datasets will be downloaded to the folders data/tacl_splits and data/tacl_ouput. 

## Grounded Semantic Parser
To replicate TACL results, you will have to install Freebase SPARQL endpoint. Please email me personally.

## References:
If you are using GraphParser, please cite

```

@Article{Q14-1030,
author="Reddy, Siva and Lapata, Mirella and Steedman, Mark",
title="Large-scale Semantic Parsing without Question-Answer Pairs",
journal="Transactions of the Association of Computational Linguistics -- Volume 2, Issue 1",
year="2014",
pages="377-392",
url="http://aclweb.org/anthology/Q14-1030"
}

```
