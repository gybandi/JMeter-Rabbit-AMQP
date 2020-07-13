# JMeter-Rabbit-AMQP #
======================

A [JMeter](http://jmeter.apache.org/) plugin to publish & consume messages from [RabbitMQ](http://www.rabbitmq.com/) or any [AMQP](http://www.amqp.org/) message broker.


Building
--------

The project is built using Maven. To build, execute the following command:

`mvn clean install`


Installing
----------

To install the plugin, build the project and copy the generated JMeterAMQP-jar-with-dependencies.jar file from target/dist to JMeter's lib/ext/ directory.
This contains the necessary AMQP Client dependency, no other jar files needed.

If for some reason you need the plugin without the AMQP Client included, you can use the JMeterAMQP.jar, but in this case, you need to provide the 
the RabbitMQ client library (amqp-client-5.x.x.jar) by installing it in JMeter's lib/ directory.


Additional information
----------------------

This fork has been extended with an AMQP RPC Client Sampler which enables RPC style communication on AMQP protocol. 
Furthermore, maven has been introduced to the project and the amqp-client dependency has been upgraded to version 5.9.0 with the necessary code changes.
