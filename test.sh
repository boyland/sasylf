#!/bin/sh

make clean
make build
java -jar SASyLF.jar tests/sum.slf
java -jar SASyLF.jar --lsp tests/test.slf
