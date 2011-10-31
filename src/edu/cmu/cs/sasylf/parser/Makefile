.SUFFIXES = .java .class
SHELL=bash
SOURCES	= $(wildcard *.java)
CLASSFILES	= $(SOURCES:.java=.class)

%.class: %.java
	javac -source 1.5 -g $<

all:	DSLToolkitParser.java $(CLASSFILES)

DSLToolkitParser.java:	parser.jj
	javacc.bat parser.jj
	javac -source 1.5 -g DSLToolkitParser.java

clean:
	rm *.class DSLToolkitParser*.java Token*.java
