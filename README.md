# stanford_core_nlp_python_interface
This repo contains a Java wrapper for StanfordCoreNLP, as well as an example file of how to use JPype to call it from Python.

Instructions:

1. Download StanfordCoreNLP version 1.3.3 here: http://nlp.stanford.edu/software/stanford-corenlp-2012-07-09.tgz.
2. Unpack StanfordCoreNLP and copy the following jars into the lib/ directory of this repo:
   - joda-time.jar
   - stanford-corenlp-2012-07-06-models.jar
   - stanford-corenlp-2012-07-09.jar
   - xom.jar
3. Build the Java code in this repo by executing 'make all' from the top directory of this repo.
4. To execute the python code that calls the Java wrapper, you have to install JPype on your computer/server and make some
   modifications to src/python/JPypeExample.py (currently the parameters for this file were specific to my laptop).
  a) Install JPype by cloning the jpype repo, and executing its setup script.  From your terminal, enter these commands:
     - git clone https://github.com/originell/jpype.git
     - cd jpype
     - sudo python setup.py install
  b) Open src/python/JPypeExample.py
  c) The startJVM method takes two parameters.  The first parameter is the path to the dynamic link library of the JVM on
     your system.  For me, it was /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/MacOS/libjli.dylib.  Replace
     this with the appropriate path on your system.  The second parameter contains the paths to each of the jars that is needed.
     Unfortunately, the JVM does not accept the '*' syntax in this case, so we have to list all the jars.  Modify the jar paths
     for your system.
  d) Lastly, you can modify the parameters passed to wrapper.tokenizeAndDepParse.  The first parameter and second parameters
     specify the input and output file/directories, respectively, while the third parameter is the number of threads to use.
     Example input files can be found under the data directory, while by convention, you should put the output files in the
     output directory.  IF you want to process only a single input file containing JSONified Gmail emails, the first parameter
     should be the path to the input .json file, while the second parameter should be path to the output .json file.  If you
     want to process multiple files, create a .txt file with each line spelling out the path to an input .json file.  The
     first parameter to wrapper.tokenizeAndDepParse should then be the path to the .txt file, while the second parameter
     should be the output directory.  For each input .json file listed in the .txt file, a .json file with the same name will
     be generated under the output directory.
5. From the terminal, execute 'python src/python/JPypeExample.py'.  The output will be a .json file(s) that contains a json
   for each input email (also in json format), but with two additional fields: "tokenized_body" and "dependency_parsed_body".
   Both fields are arrays, with one element for each sentence in the email.  For each sentence, the tokenized_body field
   contains a list of the tokens, while the dependency_parsed_body field contains a list of dependencies.  Each dependency
   consists of the relation, the head, and the dependent.  The list of dependencies constitute a dependency parse tree for
   the sentence.  Example output can be found in the output directory.

To use StanfordCoreNLPWrapper in a different repo, just move the jar files into your repo.  The necessary jars are listed in
the second parameter to startJVM in JPypeExample.
