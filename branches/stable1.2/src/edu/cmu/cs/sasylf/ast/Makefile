.SUFFIXES = .java .class
SHELL=bash
SOURCES	= $(wildcard *.java)
CLASSFILES	= $(SOURCES:.java=.class)

%.class: %.java
	javac -source 1.5 -g $<

all:	$(CLASSFILES)

clean:
	rm *.class *~
