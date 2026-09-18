Drop the connector's shaded (all-dependencies-included) jar here before
opening/deploying the module in Studio Pro:

    amqp-broker-connector-1.0.0-with-dependencies.jar

That jar is produced by building the ../../connector Maven module:

    cd mendix-amqp-broker/connector
    mvn -DskipTests package
    cp target/amqp-broker-connector-1.0.0-with-dependencies.jar \
       ../mendix-module/userlib/

It bundles the connector classes plus Apache Qpid JMS and SLF4J, so this is
the only jar the module needs in userlib. Studio Pro automatically adds
every jar under userlib/ to the module's Java classpath - no further
project configuration is required.
