package com.solace.mendix.amqp;

/** Whether an AMQP address should be treated as a point-to-point queue or a publish/subscribe topic. */
public enum DestinationType {
    QUEUE,
    TOPIC
}
