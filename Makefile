# This makefile can build JAR files for command line use
# or for Eclipse plugin use.
# NB: The first line of file README.TXT, must have the format
# SASAyLF version VERSIONSTRING\r
# where VERSION string is a valid Eclipse Version, e.g. 1.1.3

VERSION=`head -1 README.txt | sed 's/^SASyLF version \(.*\).$$/\1/'`.v`date +'%Y%m%d'`
.PHONY: build build-plugin

build :
	(cd src && cd edu && cd cmu && cd cs && cd sasylf && cd parser; javacc parser.jj)
	mkdir -p bin
	(cd src && javac -classpath ../bin:. -d ../bin edu/cmu/cs/sasylf/Main.java)
	jar cmf sasylf.mf SASyLF.jar README.TXT -C bin edu

TESTBIN= bin/org/sasylf/Activator.class
build-plugin : ${TESTBIN} README.TXT
	jar cmf META-INF/MANIFEST.MF org.sasylf_${VERSION}.jar plugin.xml README.TXT icons -C bin .

${TESTBIN}:
	@echo Unable to compile Eclipse plugin code in Makefile.
	@echo Load project into Eclipse and build.
	@echo Then come back and make build-plugin
	false
		
clean:
	rm -rf bin SASyLF.jar org.sasylf*.jar