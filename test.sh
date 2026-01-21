#!/bin/sh

make clean
make build
java -jar SASyLF.jar tests/sum.slf
java -jar SASyLF.jar --lsp tests/sum.slf
cat tests/sum.slf | java -jar SASyLF.jar --lsp --stdin
cat tests/test.slf | java -jar SASyLF.jar --lsp --stdin
cat tests/hw8m.slf | java -jar SASyLF.jar --lsp --stdin
cat tests/exercise1.slf | java -jar SASyLF.jar --lsp --stdin
