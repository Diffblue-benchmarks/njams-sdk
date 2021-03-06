/*
 * Copyright (c) 2019 Faiz & Siegeln Software GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * The Software shall be used for Good, not Evil.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.im.njams.sdk.communication;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.faizsiegeln.njams.messageformat.v4.tracemessage.TraceMessage;
import org.slf4j.LoggerFactory;

import com.faizsiegeln.njams.messageformat.v4.common.CommonMessage;
import com.faizsiegeln.njams.messageformat.v4.logmessage.LogMessage;
import com.faizsiegeln.njams.messageformat.v4.projectmessage.ProjectMessage;
import com.im.njams.sdk.common.NjamsSdkRuntimeException;
import com.im.njams.sdk.settings.Settings;

/**
 * Superclass for all Senders. When writing your own Sender, extend this class and overwrite methods, when needed.
 * All Sender will be automatically pooled by the SDK; you must not implement your own connection pooling!
 *
 * @author hsiegeln
 * @version 4.0.6
 */
public abstract class AbstractSender implements Sender {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AbstractSender.class);

    protected ConnectionStatus connectionStatus;
    protected String discardPolicy;
    protected Properties properties;

    private static final AtomicBoolean hasConnected = new AtomicBoolean(false);

    private static final AtomicInteger connecting = new AtomicInteger(0);

    /**
     * returns a new AbstractSender
     */
    public AbstractSender() {
        connectionStatus = ConnectionStatus.DISCONNECTED;
    }

    @Override
    public void init(Properties properties) {
        this.properties = properties;
        discardPolicy = properties.getProperty(Settings.PROPERTY_DISCARD_POLICY, "none").toLowerCase();
    }

    /**
     * override this method to implement your own connection initialization
     *
     * @throws NjamsSdkRuntimeException NjamsSdkRuntimeException
     */
    public synchronized void connect() throws NjamsSdkRuntimeException {
        if (isConnected()) {
            return;
        }
        try {
            connectionStatus = ConnectionStatus.CONNECTING;
            connectionStatus = ConnectionStatus.CONNECTED;
        } catch (Exception e) {
            connectionStatus = ConnectionStatus.DISCONNECTED;
            throw new NjamsSdkRuntimeException("Unable to connect", e);
        }

    }

    /**
     * initiates a reconnect, if isConnected() is false and no other reconnect is currently executed.
     * Override this for your own reconnect handling
     *
     * @param ex the exception that initiated the reconnect
     */
    public synchronized void reconnect(NjamsSdkRuntimeException ex) {
        if (isConnecting() || isConnected()) {
            return;
        } else {
            synchronized (hasConnected) {
                hasConnected.set(false);
                if (LOG.isInfoEnabled() && ex != null) {
                    if (ex.getCause() == null) {
                        LOG.info("Initialized reconnect, because of : {}", ex.toString());

                    } else {
                        LOG.info("Initialized reconnect, because of : {}, {}", ex.toString(), ex.getCause().toString());
                    }
                }
                LOG.info("{} senders are reconnecting now", connecting.incrementAndGet());
            }
        }

        while (!isConnected()) {
            try {
                connect();
                synchronized (hasConnected) {
                    if (!hasConnected.get()) {
                        LOG.info("Connection can be established again!");
                        LOG.info("Reconnected sender {}", getName());
                        hasConnected.set(true);
                    }
                    LOG.debug("{} senders still need to reconnect.", connecting.decrementAndGet());
                }
            } catch (NjamsSdkRuntimeException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    return;
                }
            }
        }
    }

    /**
     * Send the given message. This method automatically applies the discardPolicy onConnectionLoss, if set
     *
     * @param msg the message to send
     */
    @Override
    public void send(CommonMessage msg) {
        // do this until message is sent or discard policy onConnectionLoss is satisfied
        boolean isSent = false;
        do {
            if (isConnected()) {
                try {
                    if (msg instanceof LogMessage) {
                        send((LogMessage) msg);
                    } else if (msg instanceof ProjectMessage) {
                        send((ProjectMessage) msg);
                    } else if (msg instanceof TraceMessage) {
                        send((TraceMessage) msg);
                    }
                    isSent = true;
                    break;
                } catch (NjamsSdkRuntimeException e) {
                    onException(e);
                }
            }
            if (isDisconnected()) {
                // discard message, if onConnectionLoss is used
                isSent = "onconnectionloss".equalsIgnoreCase(discardPolicy);
                if (isSent) {
                    LOG.debug("Applying discard policy [{}]. Message discarded.", discardPolicy);
                    break;
                }
            }
            // wait for reconnect
            if (isConnecting()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                // trigger reconnect
                onException(null);
            }
        } while (!isSent);
    }

    /**
     * used to implement your exception handling for this sender. Is called, if sending of a message fails.
     * It will automatically close any try to reconnect the connection;
     * override this method for your own handling
     *
     * @param exception NjamsSdkRuntimeException
     */
    protected void onException(NjamsSdkRuntimeException exception) {
        // close the existing connection
        close();
        reconnect(exception);
    }

    /**
     * Implement this method to send LogMessages
     *
     * @param msg the message to send
     * @throws NjamsSdkRuntimeException NjamsSdkRuntimeException
     */
    protected abstract void send(LogMessage msg) throws NjamsSdkRuntimeException;

    /**
     * Implement this method to send ProjectMessages
     *
     * @param msg the message to send
     * @throws NjamsSdkRuntimeException NjamsSdkRuntimeException
     */
    protected abstract void send(ProjectMessage msg) throws NjamsSdkRuntimeException;

    /**
     * Implement this method to send TraceMessages
     *
     * @param msg the message to send
     * @throws NjamsSdkRuntimeException NjamsSdkRuntimeException
     */
    protected abstract void send(TraceMessage msg) throws NjamsSdkRuntimeException;

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    /**
     * @return true if connectionStatus == ConnectionStatus.CONNECTED
     */
    public boolean isConnected() {
        return connectionStatus == ConnectionStatus.CONNECTED;
    }

    /**
     * @return true if connectionStatus == ConnectionStatus.DISCONNECTED
     */
    public boolean isDisconnected() {
        return connectionStatus == ConnectionStatus.DISCONNECTED;
    }

    /**
     * @return true if connectionStatus == ConnectionStatus.CONNECTING
     */
    public boolean isConnecting() {
        return connectionStatus == ConnectionStatus.CONNECTING;
    }

}
