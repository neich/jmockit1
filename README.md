Codebase for JMockit 1.x releases - [Documentation](http://jmockit.org) - [Release notes](http://jmockit.org/changes.html)

How to build the project:
* use JDK 1.8
* use Maven 3.3.1 or newer; the following are the top-level modules:
    1. main/pom.xml            builds jmockit-1.n.jar, running JUnit and TestNG test suites
    2. coverageTests/pom.xml   runs JUnit tests for the coverage tool
    3. others in samples       dir various sample test suites
    
In this version of the library, if you select path coverage, you will get prime path coverage. The old path coverage is gone.


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

* and then the dependency:

```xml
		<dependency>
			<groupId>org.jmockit</groupId>
			<artifactId>jmockit</artifactId>
			<version>1.29</version>
		</dependency>
```