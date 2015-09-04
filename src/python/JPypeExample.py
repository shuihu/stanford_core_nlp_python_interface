from jpype import *

startJVM("/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/MacOS/libjli.dylib", "-Djava.class.path=/Users/shuihu/workspace/test_corenlp/lib/json-simple-1.1.jar:/Users/shuihu/workspace/test_corenlp/lib/joda-time.jar:/Users/shuihu/workspace/test_corenlp/lib/stanford-corenlp-2012-07-06-models.jar:/Users/shuihu/workspace/test_corenlp/lib/stanford-corenlp-2012-07-09-sources.jar:/Users/shuihu/workspace/test_corenlp/lib/stanford-corenlp-2012-07-09.jar:/Users/shuihu/workspace/test_corenlp/lib/stanford-corenlp-wrapper-2015-09-04.jar:/Users/shuihu/workspace/test_corenlp/lib/xom.jar")

wrapperPkg = JPackage('edu').stanford.nlp.wrapper
wrapper = wrapperPkg.StanfordCoreNLPWrapper

wrapper.tokenizeAndDepParse("data/enron1.json", "output/enron1.json", 1)
print 'Python called Java successfully'

shutdownJVM()

