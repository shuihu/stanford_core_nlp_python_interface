all:
	rm -rf classes
	rm -f lib/stanford-corenlp-wrapper.jar
	mkdir classes
	javac -cp "lib/*" -d classes src/edu/stanford/nlp/wrapper/*.java
	cd classes ; jar -cfv ../lib/stanford-corenlp-wrapper-`date +%Y-%m-%d`.jar edu ; cd ..

clean:
	rm -rf classes
	rm -f lib/stanford-corenlp-wrapper*.jar
