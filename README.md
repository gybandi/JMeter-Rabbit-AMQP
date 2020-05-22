# JMeter-Rabbit-AMQP #
======================

A [JMeter](http://jmeter.apache.org/) plugin to publish & consume messages from [RabbitMQ](http://www.rabbitmq.com/) or any [AMQP](http://www.amqp.org/) message broker.


JMeter Runtime Dependencies
---------------------------

Prior to building or installing this JMeter plugin, ensure that the RabbitMQ client library (amqp-client-3.x.x.jar) is installed in JMeter's lib/ directory.


Build Dependencies
------------------

Build dependencies are managed by Ivy. JARs should automagically be downloaded by Ivy as part of the build process.

In addition, you'll need to copy or symlink the following from JMeter's lib/ext directory:
* ApacheJMeter_core.jar


Building
--------

The project is built using Ant. To execute the build script, just execute:
    ant


Installing
----------

To install the plugin, build the project and copy the generated JMeterAMQP.jar file from target/dist to JMeter's lib/ext/ directory.

Additional information
----------------------

This fork has been extended with an AMQP RPC Client Sampler which enables RPC style communication on AMQP protocol. 
Furthermore, maven has been introduced to the project and the amqp-client dependency has been upgraded to version 5.9.0 with the necessary code changes.
