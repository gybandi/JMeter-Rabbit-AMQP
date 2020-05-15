package com.zeroclue.jmeter.protocol.amqp.gui;

import com.zeroclue.jmeter.protocol.amqp.AMQPRPCClient;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.JDateField;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledTextArea;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import javax.swing.*;
import java.awt.*;

/**
 * AMQP RPC Client
 * <p>
 * This class provides the AMQP RPC Sampler Gui
 * <p>
 * The GUI class is not invoked in non-GUI mode, so it should not perform any
 * additional setup that a test would need at run-time
 */
public class AMQPRPCClientGui extends AMQPSamplerGui {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggingManager.getLoggerForClass();

    private JPanel mainPanel;

    private JLabeledTextArea message = new JLabeledTextArea("Message Content");
    private JLabeledTextField rpcTimeoutInMs = new JLabeledTextField("RPC timeout (ms)");
    private JLabeledTextField messageRoutingKey = new JLabeledTextField("Routing Key");
    private JLabeledTextField messageType = new JLabeledTextField("Message Type");
    private JLabeledTextField messageId = new JLabeledTextField("Message Id");
    private JLabeledTextField appId = new JLabeledTextField("App Id");
    private JLabeledTextField userId = new JLabeledTextField("User Id");
    private JLabeledTextField clusterId = new JLabeledTextField("Cluster Id");
    private JLabeledTextField timestamp = new JLabeledTextField("Timestamp");
    private JLabeledTextField expiration = new JLabeledTextField("Expiration");
    private JLabeledTextField priority = new JLabeledTextField("Priority");
    private JLabeledTextField replyToQueue = new JLabeledTextField("Reply-To Queue");
    private JLabeledTextField contentType = new JLabeledTextField("ContentType");
    private JLabeledTextField contentEncoding = new JLabeledTextField("ContentEncoding");
    private JLabeledTextField correlationId = new JLabeledTextField("Correlation Id");
    private final JCheckBox autoAck = new JCheckBox("Auto ACK", true);
    private final JCheckBox directReplyTo = new JCheckBox("Direct Reply-To", false);

    private JCheckBox persistent = new JCheckBox("Persistent?", AMQPRPCClient.DEFAULT_PERSISTENT);
    private JCheckBox useTx = new JCheckBox("Use Transactions?", AMQPRPCClient.DEFAULT_USE_TX);

    private ArgumentsPanel headers = new ArgumentsPanel("Headers");

    public AMQPRPCClientGui() {
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel() {
        return "AMQP RPC Client";
    }

    @Override
    protected String getQueueSettingsLabel() {
        return "Reply-to Queue settings";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (!(element instanceof AMQPRPCClient)) return;
        AMQPRPCClient sampler = (AMQPRPCClient) element;

        persistent.setSelected(sampler.getPersistent());
        useTx.setSelected(sampler.getUseTx());

        messageRoutingKey.setText(sampler.getMessageRoutingKey());
        rpcTimeoutInMs.setText(Integer.toString(sampler.getRpcTimeout()));
        messageType.setText(sampler.getMessageType());
        messageId.setText(sampler.getMessageId());
        appId.setText(sampler.getAppId());
        userId.setText(sampler.getUserId());
        clusterId.setText(sampler.getClusterId());
        timestamp.setText(sampler.getTimestamp());
        expiration.setText(sampler.getExpiration());
        priority.setText(sampler.getPriority());
        replyToQueue.setText(sampler.getReplyToQueue());
        contentType.setText(sampler.getContentType());
        contentEncoding.setText(sampler.getContentEncoding());
        correlationId.setText(sampler.getCorrelationId());
        autoAck.setSelected(sampler.autoAck());
        directReplyTo.setSelected(sampler.directReplyTo());
        message.setText(sampler.getMessage());
        configureHeaders(sampler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestElement createTestElement() {
        AMQPRPCClient sampler = new AMQPRPCClient();
        modifyTestElement(sampler);
        return sampler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyTestElement(TestElement te) {
        AMQPRPCClient sampler = (AMQPRPCClient) te;
        sampler.clear();
        configureTestElement(sampler);

        super.modifyTestElement(sampler);

        sampler.setPersistent(persistent.isSelected());
        sampler.setUseTx(useTx.isSelected());

        sampler.setMessageRoutingKey(messageRoutingKey.getText());
        sampler.setRpcTimeout(Integer.parseInt(rpcTimeoutInMs.getText()));
        sampler.setMessage(message.getText());
        sampler.setAppId(appId.getText());
        sampler.setClusterId(clusterId.getText());
        sampler.setUserId(userId.getText());
        sampler.setTimestamp(timestamp.getText());
        sampler.setExpiration(expiration.getText());
        sampler.setPriority(priority.getText());
        sampler.setContentType(contentType.getText());
        sampler.setContentEncoding(contentEncoding.getText());
        sampler.setMessageType(messageType.getText());
        sampler.setMessageId(messageId.getText());
        sampler.setReplyToQueue(replyToQueue.getText());
        sampler.setCorrelationId(correlationId.getText());
        sampler.setAutoAck(autoAck.isSelected());
        sampler.setDirectReplyTo(directReplyTo.isSelected());
        sampler.setHeaders((Arguments) headers.createTestElement());
    }

    @Override
    protected void setMainPanel(JPanel panel) {
        mainPanel = panel;
    }

    /*
     * Helper method to set up the GUI screen
     */
    @Override
    protected final void init() {
        super.init();
        persistent.setPreferredSize(new Dimension(100, 25));
        useTx.setPreferredSize(new Dimension(100, 25));
        rpcTimeoutInMs.setPreferredSize(new Dimension(20, 25));
        messageRoutingKey.setPreferredSize(new Dimension(100, 25));
        messageType.setPreferredSize(new Dimension(100, 25));
        messageId.setPreferredSize(new Dimension(100, 25));
        userId.setPreferredSize(new Dimension(100, 25));
        clusterId.setPreferredSize(new Dimension(100, 25));
        timestamp.setPreferredSize(new Dimension(100, 25));
        expiration.setPreferredSize(new Dimension(100, 25));
        priority.setPreferredSize(new Dimension(10, 25));
        contentType.setPreferredSize(new Dimension(100, 25));
        contentEncoding.setPreferredSize(new Dimension(100, 25));
        replyToQueue.setPreferredSize(new Dimension(100, 25));
        correlationId.setPreferredSize(new Dimension(100, 25));
        message.setPreferredSize(new Dimension(400, 150));
        iterations.setVisible(false);
        directReplyTo.addChangeListener(e -> {
            JCheckBox source = (JCheckBox) e.getSource();
            if (source.isSelected()) {
                clearReplyQueueProperties();
                setReplyQueuePropertiesEnabled(false);
                replyToQueue.setText("amq.rabbitmq.reply-to");
            } else {
                clearReplyQueueProperties();
                setReplyQueuePropertiesEnabled(true);
            }
        });
        mainPanel.add(persistent);
        mainPanel.add(useTx);
        mainPanel.add(rpcTimeoutInMs);
        mainPanel.add(messageRoutingKey);
        mainPanel.add(messageType);
        mainPanel.add(messageId);
        mainPanel.add(userId);
        mainPanel.add(clusterId);
        mainPanel.add(timestamp);
        mainPanel.add(expiration);
        mainPanel.add(priority);
        mainPanel.add(replyToQueue);
        mainPanel.add(correlationId);
        mainPanel.add(contentType);
        mainPanel.add(contentEncoding);
        mainPanel.add(autoAck);
        mainPanel.add(directReplyTo);
        mainPanel.add(headers);
        mainPanel.add(message);
    }

    private void clearReplyQueueProperties() {
        queue.setText("");
        routingKey.setText("");
        messageTTL.setText("");
        messageExpires.setText("");
        replyToQueue.setText("");
        queueDurable.setSelected(false);
        queueExclusive.setSelected(false);
        queueAutoDelete.setSelected(false);
        queueRedeclare.setSelected(false);
    }

    private void setReplyQueuePropertiesEnabled(boolean enabled) {
        queueSettings.setEnabled(enabled);
        queue.setEnabled(enabled);
        routingKey.setEnabled(enabled);
        messageTTL.setEnabled(enabled);
        messageExpires.setEnabled(enabled);
        replyToQueue.setEnabled(enabled);
        queueDurable.setEnabled(enabled);
        queueExclusive.setEnabled(enabled);
        queueAutoDelete.setEnabled(enabled);
        queueRedeclare.setEnabled(enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGui() {
        super.clearGui();
        persistent.setSelected(AMQPRPCClient.DEFAULT_PERSISTENT);
        useTx.setSelected(AMQPRPCClient.DEFAULT_USE_TX);
        messageRoutingKey.setText("");
        messageType.setText("");
        messageId.setText("");
        userId.setText("");
        clusterId.setText("");
        timestamp.setText("");
        expiration.setText("");
        priority.setText("");
        replyToQueue.setText("amq.rabbitmq.reply-to");
        correlationId.setText("");
        contentEncoding.setText("");
        rpcTimeoutInMs.setText("5000");
        contentType.setText("");
        autoAck.setSelected(true);
        directReplyTo.setSelected(false);
        headers.clearGui();
        message.setText("");
    }

    private void configureHeaders(AMQPRPCClient sampler) {
        Arguments sampleHeaders = sampler.getHeaders();
        if (sampleHeaders != null) {
            headers.configure(sampleHeaders);
        } else {
            headers.clearGui();
        }
    }
}