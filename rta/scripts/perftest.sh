#!/bin/bash
CLASSPATH=$(mvn -Ptest dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q):target/test-classes:target/classes
echo $CLASSPATH
java -Djava.util.logging.config.file=log.properties -Xmx512M -Dglass.platform=Headless -Dprism.order=sw -cp $CLASSPATH com.gluonhq.richtextarea.PerformanceTests
