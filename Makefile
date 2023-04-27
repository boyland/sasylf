# This makefile can build JAR files for command line use
# or for Eclipse plugin use.
# NB: The first line of file ChangeLog.txt, must have the format
# SASyLF version VERSIONSTRING\r
# where VERSION string is a valid Eclipse Version, e.g. 1.1.3

VERSION=`head -1 ChangeLog.txt | sed 's/^SASyLF version \(.*\).$$/\1/'`.v`date +'%Y%m%d'`
.PHONY: build build-plugin test default unit-test regression-test

default: test

build : SHELL:=/bin/bash
build :
	(cd src && cd edu && cd cmu && cd cs && cd sasylf && cd parser; javacc parser.jj)
	mkdir -p bin
	(cd src && javac -cp .:../bin:../lib/* -source 1.8 -target 1.8 -d ../bin edu/cmu/cs/sasylf/Main.java ${TESTSRC})
	jar cmf sasylf.mf SASyLF.jar ChangeLog.txt -C bin edu -C library org

TESTBIN= bin/org/sasylf/Activator.class
TESTLIB= bin/org/sasylf/util/Natural.slf
TESTSRC= edu/cmu/cs/sasylf/term/UnitTests.java \
	 edu/cmu/cs/sasylf/util/UnitTests.java \
	 edu/cmu/cs/sasylf/reduction/UnitTests.java

build-plugin : ${TESTBIN} ${TESTLIB} ChangeLog.txt
	jar cmf META-INF/MANIFEST.MF org.sasylf_${VERSION}.jar plugin.xml ChangeLog.txt icons/*.gif icons/*.png -C bin . 

${TESTBIN}:
	@echo Unable to compile Eclipse plugin code in Makefile.
	@echo Load project into Eclipse and build.
	@echo Then come back and make build-plugin
	false

${TESTLIB}:
	${MAKE} install-lib


.PHONY: install-lib
install-lib:
	cp -r library/org bin/.

	
ADDTESTS= \
	examples/and.slf \
	examples/cut-elimination.slf \
	examples/featherweight-java.slf \
	examples/iso-recursive-sub.slf \
	examples/lambda-loc.slf \
	examples/lambda-unicode.slf \
	examples/lambda.slf \
	examples/LF.slf \
	examples/object-calculus.slf \
	examples/poplmark-2a.slf \
	examples/test-mutual.slf \
	examples/test-structural.slf

SUBJECT=bin

test: unit-test regression-test

unit-test:
	java -cp ${SUBJECT} edu/cmu/cs/sasylf/term/UnitTests
	java -cp ${SUBJECT} edu/cmu/cs/sasylf/util/UnitTests
	java -cp ${SUBJECT} edu/cmu/cs/sasylf/reduction/UnitTests

regression-test: ${TESTLIB}
	@echo "Regression Tests: " `echo regression/*.slf ${ADDTESTS} | wc -w`
	@-for f in regression/*.slf ${ADDTESTS}; do \
	  printf "."; \
	  java -ea -cp ${SUBJECT} edu.cmu.cs.sasylf.Main $$f 2>&1 | sed "s#Internal SASyLF error.*#$$f:0:Internal error#" | grep '.*:[0-9]*:' | grep "$$f" | sed 's/: .*/:/' | sort -u -t ':' -n -k 2 > test.out; \
	  grep -n '/[*/]!' /dev/null $$f | sed 's/:\([0-9]*\):.*/:\1:/' | diff - test.out; \
	done
	@echo "  Done."
	@rm test.out

clean:
	rm -rf bin SASyLF.jar org.sasylf*.jar
