Codebase for JMockit 1.x releases - [Documentation](http://jmockit.org) - [Release notes](http://jmockit.org/changes.html)

How to build the project:
* use JDK 1.8
* use Maven 3.3.1 or newer; the following are the top-level modules:
    1. main/pom.xml            builds jmockit-1.n.jar, running JUnit and TestNG test suites
    2. coverageTests/pom.xml   runs JUnit tests for the coverage tool
    3. others in samples       dir various sample test suites

This version adds [Prime Path Coverage](http://cs.gmu.edu/~offutt/softwaretest/) (PPC) to JMockit ```1.35```. In order to activate PPC you have to use the [system properties mechanism](http://jmockit.org/tutorial/CodeCoverage.html#configuration) of the original JMockit framework. Now you have the extra option ```primepath``` to activate PPC. Note that ```path``` and ```primepath``` exclude each other, so you can only use one at a given test run.


In order to use this version of jmockit with maven, you have to add the following to your pom.xml:

* the github respository:

```xml
	<repositories>
		<repository>
			<id>jmockit1-mvn-repo</id>
			<url>https://raw.github.com/neich/jmockit1/mvn-repo/</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
		</repository>
	</repositories>
```

* and then the dependency (note the different version name ```1.35_pp```):

```xml
		<dependency>
			<groupId>org.jmockit</groupId>
			<artifactId>jmockit</artifactId>
			<version>1.35_pp</version>
		</dependency>
```

A different version name is used to avoid a clash in the maven local folders and then making possible to use the two versions at the same time.
