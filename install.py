#!/usr/bin/env python
import sys
import os

def install_ungrounded_parser():
    os.system("git clone https://sivareddyg@bitbucket.org/sivareddyg/graph-parser-data.git && cd graph-parser-data && git checkout -b temp 4778dbd")
    os.system("cp -r graph-parser-data/* .")
    os.system("rm -rf graph-parser-data")

    os.system("ant clean")
    os.system("ant build")

    os.system("wget https://bitbucket.org/sivareddyg/public/downloads/easyccg_model_questions.tgz")
    os.system("wget https://bitbucket.org/sivareddyg/public/downloads/easyccg_model.tgz")
    os.system("tar -xvzf easyccg_model_questions.tgz -C lib_data")
    os.system("tar -xvzf easyccg_model.tgz -C lib_data")

    os.system("rm easyccg_model_questions.tgz")
    os.system("rm easyccg_model.tgz")

def download_evaluation_data():
    os.system("wget https://bitbucket.org/sivareddyg/public/downloads/tacl2014_eval_data.tgz")
    os.system("tar -xvzf tacl2014_eval_data.tgz")
    os.system("rm tacl2014_eval_data.tgz")

if __name__ == "__main__":
    if sys.argv[1] == "ungrounded":
        install_ungrounded_parser()
    if sys.argv[1] == "evaluation":
        download_evaluation_data()
