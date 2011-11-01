.PHONY: build

build :
	(cd src && cd edu && cd cmu && cd cs && cd sasylf && cd parser; javacc parser.jj)
	mkdir -p bin
	(cd src && javac -classpath ../bin:. -d ../bin edu/cmu/cs/sasylf/Main.java)
	(cd bin && find . -name "*.class" -print > classlist)
	(cd bin && jar cmf ../META-INF/MANIFEST.MF ../SASyLF.jar @classlist)
	rm bin/classlist
	
clean:
	rm -rf bin SASyLF.jar