# This makefile can build JAR files for command line use
# or for Eclipse plugin use.
# NB: The first line of file README.TXT, must have the format
# SASyLF version VERSIONSTRING\r
# where VERSION string is a valid Eclipse Version, e.g. 1.1.3

VERSION=`head -1 README.txt | sed 's/^SASyLF version \(.*\).$$/\1/'`.v`date +'%Y%m%d'`
.PHONY: build build-plugin test

build :
	(cd src && cd edu && cd cmu && cd cs && cd sasylf && cd parser; javacc parser.jj)
	mkdir -p bin
	(cd src && javac -source 1.6 -target 1.6 -classpath ../bin:. -d ../bin edu/cmu/cs/sasylf/Main.java)
	jar cmf sasylf.mf SASyLF.jar README.TXT -C bin edu

TESTBIN= bin/org/sasylf/Activator.class
build-plugin : ${TESTBIN} README.TXT
	jar cmf META-INF/MANIFEST.MF org.sasylf_${VERSION}.jar plugin.xml README.TXT icons/*.gif icons/*.png -C bin .

${TESTBIN}:
	@echo Unable to compile Eclipse plugin code in Makefile.
	@echo Load project into Eclipse and build.
	@echo Then come back and make build-plugin
	false

ADDTESTS= \
	examples/and.slf \
	examples/featherweight-java.slf \
	examples/lambda-loc.slf \
	examples/lambda-unicode.slf \
	examples/lambda.slf \
	examples/object-calculus.slf \
	examples/poplmark-2a.slf \
	examples/test-mutual.slf \
	examples/test-structural.slf
	
test:
	@-for f in regression/*.slf ${ADDTESTS}; do \
	  printf "."; \
	  ./sasylf.local $$f 2>&1 | sed "s#Internal SASyLF error!#$$f:0:Internal error#" | grep '.*:[0-9]*:' | sed 's/: .*/:/' | sort -u -t ':' -n -k 2 > test.out; \
	  grep -n '//!' /dev/null $$f | sed 's/:\([0-9]*\):.*/:\1:/' | diff - test.out; \
	done
	@echo "  Done."
	@rm test.out
	
clean:
	rm -rf bin SASyLF.jar org.sasylf*.jar
