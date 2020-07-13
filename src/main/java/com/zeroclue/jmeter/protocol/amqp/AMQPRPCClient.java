package com.zeroclue.jmeter.protocol.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AMQP RPC Client Sampler using direct-reply-to
 * <p>
 * JMeter creates an instance of a sampler class for every occurrence of the
 * element in every thread. [some additional copies may be created before the
 * test run starts]
 * <p>
 * Thus each sampler is guaranteed to be called by a single thread - there is no
 * need to synchronize access to instance variables.
 * <p>
 * However, access to class fields must be synchronized.
 */
public class AMQPRPCClient extends AMQPSampler implements Interruptible {

    private static final long serialVersionUID = 1234578;

    private static final Logger log = LoggingManager.getLoggerForClass();

    //++ These are JMX names, and must not be changed
    private final static String MESSAGE = "AMQPPublisher.Message";
    private final static String MESSAGE_ROUTING_KEY = "AMQPPublisher.MessageRoutingKey";
    private final static String MESSAGE_TYPE = "AMQPPublisher.MessageType";
    private final static String REPLY_TO_QUEUE = "AMQPPublisher.ReplyToQueue";
    private final static String CORRELATION_ID = "AMQPPublisher.CorrelationId";
    private final static String MESSAGE_ID = "AMQPPublisher.MessageId";
    private static final String AUTO_ACK = "AMQPConsumer.AutoAck";
    private final static String HEADERS = "AMQPPublisher.Headers";
    private final static String RPC_TIMEOUT = "AMQPConsumer.rpcTimeout";

    private final static String USER_ID = "AMQPPublisher.UserId";
    private final static String APP_ID = "AMQPPublisher.AppId";
    private final static String CLUSTER_ID = "AMQPPublisher.ClusterId";
    private final static String TIMESTAMP = "AMQPPublisher.Timestamp";
    private final static String EXPIRATION = "AMQPPublisher.Expiration";
    private final static String PRIORITY = "AMQPPublisher.Priority";

    private final static String CONTENT_TYPE = "AMQPPublisher.ContentType";
    private final static String CONTENT_ENCODING = "AMQPPublisher.ContentEncoding";
    private static final String DIRECT_REPLY_TO = "AMQPPublisher.DirectReplyTo";
    private final static String THREADNUM_IN_LABEL = "ThreadnumInLabel";
    public static boolean DEFAULT_PERSISTENT = false;
    private final static String PERSISTENT = "AMQPConsumer.Persistent";

    public final static boolean DEFAULT_THREADNUM_IN_LABEL = true;
    public static boolean DEFAULT_USE_TX = false;
    private final static String USE_TX = "AMQPConsumer.UseTx";

    private transient Channel channel;
    private transient BlockingQueue<Delivery> consumerQueue = new ArrayBlockingQueue<>(1);
    private transient String ctag;

    public AMQPRPCClient() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampleResult sample(Entry e) {
        SampleResult result = new SampleResult();
        String label = getTitle();
        if (getThreadNumInLabel()) {
            label += " thread-" + getThreadContext().getThreadNum();
        }
        result.setSampleLabel(label);
        result.setSuccessful(false);
        result.setResponseCode("500");
        result.setContentType(getContentType());

        try {
            initChannel();
            initConsumer();
        } catch (Exception ex) {
            log.error("Failed to initialize channel : ", ex);
            result.setResponseMessage(ex.toString());
            return result;
        }

        String data = getMessage(); // Sampler data
        /*
         * Perform the sampling
         */

        // aggregate samples.
        int loop = getIterationsAsInt();

        try {
            byte[] response = null;

            AMQP.BasicProperties messageProperties = getProperties();
            byte[] messageBytes = getMessageBytes();

            result.sampleStart(); // Start timing

            publishMessage(result, messageProperties, messageBytes);

            receiveMessage(result, data);
            // commit the sample.
            if (getUseTx()) {
                channel.txCommit();
            }
        } catch (IOException ioe) {
            log.info(ioe.getMessage(), ioe);
        }


        return result;
    }

    private void receiveMessage(SampleResult result, String data) throws IOException {
        byte[] responseBody = new byte[0];
        AMQP.BasicProperties responseProperties = null;

        try {
            int rpcTimeout = getRpcTimeout();
            log.info("RPC timeout: " + getRpcTimeout());
            log.info("reply-to: " + getReplyToQueue());
            Delivery delivery = consumerQueue.poll(getRpcTimeout(), TimeUnit.MILLISECONDS);

            if (delivery == null) {
                throw new IllegalStateException("RPC called timed out, no message received!");
            }
            log.debug("Got message with appropriate correlation Id: " + this.getCorrelationId());
            responseBody = delivery.getBody();
            responseProperties = delivery.getProperties();

            if (!autoAck()) {
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
            /*
             * Set up the sample result details
             */
            result.sampleEnd();
            result.setSamplerData(data);
            result.setResponseHeaders(responseProperties.getHeaders().toString());
            result.setResponseData(new String(responseBody, StandardCharsets.UTF_8), StandardCharsets.UTF_8.name());
            result.setDataType(SampleResult.TEXT);

            result.setResponseCodeOK();
            result.setResponseMessage("OK");
            result.setSuccessful(true);
        } catch (
                InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (
                Exception ex) {
            log.info(ex.getMessage(), ex);
            result.sampleEnd();
            result.setResponseCode("500");
            result.setResponseMessage(ex.toString());
        } finally {
            channel.basicCancel(ctag);
        }

    }

    private void publishMessage(SampleResult result, AMQP.BasicProperties messageProperties, byte[] messageBytes) throws IOException {
        log.debug("Publishing message on exchange '" + getExchange() + "' with routingkey '" + getMessageRoutingKey());
        log.debug("Message Header: " + messageProperties);
        log.debug("Message Payload: " + new String(messageBytes, "UTF8"));
        result.setRequestHeaders(messageProperties.getHeaders().toString());
        channel.basicPublish(getExchange(), getMessageRoutingKey(), messageProperties, messageBytes);
    }


    private byte[] getMessageBytes() {
        return getMessage().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @return the message routing key for the sample
     */
    public String getMessageRoutingKey() {
        return getPropertyAsString(MESSAGE_ROUTING_KEY);
    }

    public void setMessageRoutingKey(String content) {
        setProperty(MESSAGE_ROUTING_KEY, content);
    }

    /**
     * @return the message for the sample
     */
    public String getMessage() {
        return getPropertyAsString(MESSAGE);
    }

    public void setMessage(String content) {
        setProperty(MESSAGE, content);
    }

    /**
     * @return the message type for the sample
     */
    public String getMessageType() {
        return getPropertyOrNull(getPropertyAsString(MESSAGE_TYPE));
    }

    public void setMessageType(String content) {
        setProperty(MESSAGE_TYPE, content);
    }

    public String getAppId() {
        return getPropertyOrNull(getPropertyAsString(APP_ID));
    }

    public void setAppId(String content) {
        setProperty(APP_ID, content);
    }

    public String getUserId() {
        return getPropertyOrNull(getPropertyAsString(USER_ID));
    }

    public void setUserId(String content) {
        setProperty(USER_ID, content);
    }

    public String getClusterId() {
        return getPropertyOrNull(getPropertyAsString(CLUSTER_ID));
    }

    public void setClusterId(String content) {
        setProperty(CLUSTER_ID, content);
    }

    public String getExpiration() {
        return getPropertyOrNull(getPropertyAsString(EXPIRATION));
    }

    public void setExpiration(String content) {
        setProperty(EXPIRATION, content);
    }

    public String getPriority() {
        return getPropertyOrNull(getPropertyAsString(PRIORITY));
    }

    public void setPriority(String content) {
        setProperty(PRIORITY, content);
    }

    public String getTimestamp() {
        return getPropertyOrNull(getPropertyAsString(TIMESTAMP));
    }

    public void setTimestamp(String content) {
        setProperty(TIMESTAMP, content);
    }

    /**
     * @return the reply-to queue for the sample
     */
    public String getReplyToQueue() {
        return getPropertyAsString(REPLY_TO_QUEUE);
    }

    public void setReplyToQueue(String content) {
        setProperty(REPLY_TO_QUEUE, content);
    }

    /**
     * @return the correlation identifier for the sample
     */
    public String getCorrelationId() {
        return getPropertyAsString(CORRELATION_ID);
    }

    public void setCorrelationId(String content) {
        setProperty(CORRELATION_ID, content);
    }

    /**
     * @return the whether or not to auto ack
     */
    public String getAutoAck() {
        return getPropertyAsString(AUTO_ACK);
    }

    public void setAutoAck(String content) {
        setProperty(AUTO_ACK, content);
    }

    public void setAutoAck(Boolean autoAck) {
        setProperty(AUTO_ACK, autoAck.toString());
    }

    public Boolean getThreadNumInLabel() {
        return getPropertyAsBoolean(THREADNUM_IN_LABEL, DEFAULT_THREADNUM_IN_LABEL);
    }

    public void setThreadNumInLabel(Boolean threadNumInLabel) {
        setProperty(THREADNUM_IN_LABEL, threadNumInLabel);
    }

    public boolean autoAck() {
        return getPropertyAsBoolean(AUTO_ACK);
    }

    public Arguments getHeaders() {
        return (Arguments) getProperty(HEADERS).getObjectValue();
    }

    public void setHeaders(Arguments headers) {
        setProperty(new TestElementProperty(HEADERS, headers));
    }

    public Boolean getPersistent() {
        return getPropertyAsBoolean(PERSISTENT, DEFAULT_PERSISTENT);
    }

    public void setPersistent(Boolean persistent) {
        setProperty(PERSISTENT, persistent);
    }

    public void setRpcTimeout(int rpcTimeoutInMs) {
        setProperty(RPC_TIMEOUT, rpcTimeoutInMs);
    }

    public int getRpcTimeout() {
        return getPropertyAsInt(RPC_TIMEOUT);
    }

    public String getContentEncoding() {
        return getPropertyAsString(CONTENT_ENCODING);
    }

    public void setContentEncoding(String contentEncoding) {
        setProperty(CONTENT_ENCODING, contentEncoding);
    }


    public String getContentType() {
        return getPropertyOrNull(getPropertyAsString(CONTENT_TYPE));
    }

    public void setContentType(String contentType) {
        setProperty(CONTENT_TYPE, contentType);
    }

    public Boolean getUseTx() {
        return getPropertyAsBoolean(USE_TX, DEFAULT_USE_TX);
    }

    public void setUseTx(Boolean tx) {
        setProperty(USE_TX, tx);
    }

    public boolean directReplyTo() {
        return getPropertyAsBoolean(DIRECT_REPLY_TO);
    }

    public void setDirectReplyTo(boolean directReplyTo) {
        setProperty(DIRECT_REPLY_TO, directReplyTo);
    }

    public String getDirectReplyTo() {
        return getPropertyAsString(DIRECT_REPLY_TO);
    }

    /**
     * @return the message id for the sample
     */
    public String getMessageId() {
        return getPropertyOrNull(getPropertyAsString(MESSAGE_ID));
    }

    public void setMessageId(String content) {
        setProperty(MESSAGE_ID, content);
    }

    @Override
    public boolean interrupt() {
        cleanup();
        return true;
    }

    @Override
    protected Channel getChannel() {
        return channel;
    }

    @Override
    protected void setChannel(Channel channel) {
        this.channel = channel;
    }

    protected AMQP.BasicProperties getProperties() {
        int deliveryMode = getPersistent() ? 2 : 1;

        final AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        AMQP.BasicProperties publishProperties = builder
                .contentType(getContentType())
                .contentEncoding(getContentEncoding())
                .correlationId(getCorrelationId())
                .timestamp(getTimestamp() != null ? new Date(Long.parseLong(getTimestamp())) : null)
                .priority(getPriority() != null ? Integer.parseInt(getPriority()) : null)
                .appId(getAppId())
                .clusterId(getClusterId())
                .userId(getUserId())
                .replyTo(getReplyToQueue())
                .messageId(getMessageId())
                .headers(prepareHeaders())
                .type(getMessageType())
                .build();

        return publishProperties;
    }

    private String getPropertyOrNull(String property) {
        if (property != null) {
            return property.isEmpty() ? null : property;
        } else {
            return null;
        }
    }

    protected boolean initChannel() throws IOException, NoSuchAlgorithmException, KeyManagementException, TimeoutException {
        boolean ret = super.initChannel();
        if (getUseTx()) {
            channel.txSelect();
        }
        return ret;
    }

    private void initConsumer() throws IOException {
        String correlationId = this.getCorrelationId();
        log.info("correlation id: " + correlationId);
        ctag = channel.basicConsume(getReplyToQueue(), autoAck(), (consumerTag, delivery) -> {
            log.info("delivery properties: " + delivery.getProperties());
            log.info("publisher correlation id: " + correlationId);
            if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                consumerQueue.offer(delivery);
            }
        }, consumerTag -> {
        });
    }

    private Map<String, Object> prepareHeaders() {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, String> source = getHeaders().getArgumentsAsMap();
        for (Map.Entry<String, String> item : source.entrySet()) {
            Object value;
            try {
                value = Long.parseLong(item.getValue());
                log.debug("Parsed to long: " + item.getKey() + " value: " + item.getValue());
            } catch (Exception e) {
                value = item.getValue();
                log.debug("cannot parse to long: " + item.getKey() + " value: " + item.getValue());
            }
            if (value instanceof String) {
                String strValue = (String) value;
                if (!strValue.isEmpty() && strValue.charAt(0) == '"' && strValue.charAt(strValue.length() - 1) == '"') {
                    value = strValue.substring(1, strValue.length() - 1);
                }
            }
            result.put(item.getKey(), value);
            log.debug("added header: " + item.getKey() + " with value: " + item.getValue());
        }
        return result;
    }

}