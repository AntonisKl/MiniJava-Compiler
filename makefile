all: compile

compile:
	java -jar jtb132di.jar miniJava.jj
	java -jar javacc5.jar miniJava-jtb.jj
	javac Main.java

clean:
	rm -f *.class *~ MyClasses/*.class
