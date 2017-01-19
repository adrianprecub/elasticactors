/*
 * Copyright 2013 - 2016 The Original Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsoftware.elasticactors.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import net.jodah.lyra.config.RecoveryPolicy;
import net.jodah.lyra.event.ChannelListener;
import net.jodah.lyra.event.DefaultChannelListener;
import net.jodah.lyra.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsoftware.elasticactors.PhysicalNode;
import org.elasticsoftware.elasticactors.messaging.*;
import org.elasticsoftware.elasticactors.rabbitmq.ack.AsyncMessageAcker;
import org.elasticsoftware.elasticactors.rabbitmq.ack.BufferingMessageAcker;
import org.elasticsoftware.elasticactors.rabbitmq.ack.DirectMessageAcker;
import org.elasticsoftware.elasticactors.rabbitmq.ack.WriteBehindMessageAcker;
import org.elasticsoftware.elasticactors.serialization.internal.InternalMessageDeserializer;
import org.elasticsoftware.elasticactors.util.concurrent.ThreadBoundExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.elasticsoftware.elasticactors.rabbitmq.MessageAcker.Type.*;

/**
 * @author Joost van de Wijgerd
 */
public final class RabbitMQMessagingService extends DefaultChannelListener implements RabbitMQMessagingServiceInterface, ChannelListenerRegistry {
    private static final Logger logger = LogManager.getLogger(RabbitMQMessagingService.class);
    private final ConnectionFactory connectionFactory = new ConnectionFactory();
    private final String rabbitmqHosts;
    private static final String QUEUE_NAME_FORMAT = "%s/%s";
    private final String elasticActorsCluster;
    private static final String EA_EXCHANGE_FORMAT = "ea.%s";
    private final String exchangeName;
    private Connection clientConnection;
    private Channel consumerChannel;
    private Channel producerChannel;
    private final LocalMessageQueueFactory localMessageQueueFactory;
    private final RemoteMessageQueueFactory remoteMessageQueueFactory;
    private final RemoteActorSystemMessageQueueFactoryFactory remoteActorSystemMessageQueueFactoryFactory;
    private final ThreadBoundExecutor queueExecutor;
    private final String username;
    private final String password;
    private final InternalMessageDeserializer internalMessageDeserializer;
    private final ConcurrentMap<Channel,Set<ChannelListener>> channelListenerRegistry = new ConcurrentHashMap<>();
    private final MessageAcker.Type ackType;
    private MessageAcker messageAcker;

    public RabbitMQMessagingService(String elasticActorsCluster,
                                    String rabbitmqHosts,
                                    String username,
                                    String password,
                                    MessageAcker.Type ackType,
                                    ThreadBoundExecutor queueExecutor,
                                    InternalMessageDeserializer internalMessageDeserializer) {
        this.rabbitmqHosts = rabbitmqHosts;
        this.elasticActorsCluster = elasticActorsCluster;
        this.queueExecutor = queueExecutor;
        this.username = username;
        this.password = password;
        this.ackType = ackType;
        this.internalMessageDeserializer = internalMessageDeserializer;
        this.exchangeName = format(EA_EXCHANGE_FORMAT, elasticActorsCluster);
        this.localMessageQueueFactory = new LocalMessageQueueFactory();
        this.remoteMessageQueueFactory = new RemoteMessageQueueFactory();
        this.remoteActorSystemMessageQueueFactoryFactory = new RemoteActorSystemMessageQueueFactoryFactory();
    }

    @PostConstruct
    public void start() throws IOException, TimeoutException {
        // millis
        connectionFactory.setConnectionTimeout(1000);
        // seconds
        connectionFactory.setRequestedHeartbeat(4);
        // lyra reconnect logic
        Config config = new Config()
                .withRecoveryPolicy(new RecoveryPolicy()
                        .withMaxAttempts(-1)
                        .withInterval(Duration.seconds(1)))
                .withChannelListeners(this);

        ConnectionOptions connectionOptions = new ConnectionOptions(connectionFactory).withAddresses(rabbitmqHosts);
        connectionOptions.withUsername(username);
        connectionOptions.withPassword(password);
        // create single connection
        //clientConnection = connectionFactory.newConnection(Address.parseAddresses(rabbitmqHosts));
        clientConnection = Connections.create(connectionOptions,config);
        // create a seperate producer and a seperate consumer channel
        consumerChannel = clientConnection.createChannel();
        producerChannel = clientConnection.createChannel();
        // ensure the exchange is there
        consumerChannel.exchangeDeclare(exchangeName,"direct",true);
        if(ackType == BUFFERED) {
            messageAcker = new BufferingMessageAcker(consumerChannel);
        } else if(ackType == WRITE_BEHIND) {
            messageAcker = new WriteBehindMessageAcker(consumerChannel);
        } else if(ackType == ASYNC) {
            messageAcker = new AsyncMessageAcker(consumerChannel);
        } else {
            messageAcker = new DirectMessageAcker(consumerChannel);
        }
        messageAcker.start();
    }

    @PreDestroy
    public void stop() {
        try {
            messageAcker.stop();
            producerChannel.close();
            consumerChannel.close();
            clientConnection.close();
        } catch (IOException|TimeoutException e) {
            logger.error("Failed to close all RabbitMQ Client resources",e);
        }
    }

    @Override
    public void sendWireMessage(String queueName, byte[] serializedMessage, PhysicalNode receiver) throws IOException {
        producerChannel.basicPublish(exchangeName,queueName,true,false,null,serializedMessage);
    }

    public MessageQueueFactory getLocalMessageQueueFactory() {
        return localMessageQueueFactory;
    }

    public MessageQueueFactory getRemoteMessageQueueFactory() {
        return remoteMessageQueueFactory;
    }

    public MessageQueueFactoryFactory getRemoteActorSystemMessageQueueFactoryFactory() {
        return remoteActorSystemMessageQueueFactoryFactory;
    }

    @Override
    public void addChannelListener(final Channel channel,final ChannelListener channelListener) {
        Set<ChannelListener> listeners = this.channelListenerRegistry.get(channel);
        if(listeners == null) {
            listeners = Collections.newSetFromMap(new ConcurrentHashMap<ChannelListener, Boolean>());
            if(this.channelListenerRegistry.putIfAbsent(channel,listeners) != null) {
                // was already created
                listeners = this.channelListenerRegistry.get(channel);
            }
        }
        listeners.add(channelListener);
    }

    @Override
    public void removeChannelListener(final Channel channel,final ChannelListener channelListener) {
        final Set<ChannelListener> listeners = this.channelListenerRegistry.get(channel);
        if(listeners != null) {
            listeners.remove(channelListener);
        }
    }

    @Override
    public void onRecovery(final Channel channel) {
        final Set<ChannelListener> listeners = this.channelListenerRegistry.get(channel);
        if(listeners != null) {
            for (ChannelListener listener : listeners) {
                try {
                    listener.onRecovery(channel);
                } catch(Exception e) {
                    logger.error(format("Exception while calling onRecovery on ChannelListener [%s]",listener.toString()),e);
                }
            }
        }
    }

    @Override
    public void onRecoveryFailure(final Channel channel,final Throwable failure) {
        final Set<ChannelListener> listeners = this.channelListenerRegistry.get(channel);
        if(listeners != null) {
            for (ChannelListener listener : listeners) {
                try {
                    listener.onRecoveryFailure(channel,failure);
                } catch(Exception e) {
                    logger.error(format("Exception while calling onRecoveryFailure on ChannelListener [%s]",listener.toString()),e);
                }
            }
        }
    }

    private void ensureQueueExists(final Channel channel,final String queueName) throws IOException {
        // ensure we have the queue created on the broker
        AMQP.Queue.DeclareOk result = channel.queueDeclare(queueName, true, false, false, null);
        // and bound to the exchange
        channel.queueBind(queueName,exchangeName,queueName);
    }

    private final class LocalMessageQueueFactory implements MessageQueueFactory {
        @Override
        public MessageQueue create(String name, MessageHandler messageHandler) throws Exception {
            final String queueName = format(QUEUE_NAME_FORMAT,elasticActorsCluster,name);
            ensureQueueExists(consumerChannel,queueName);
            LocalMessageQueue messageQueue = new LocalMessageQueue(queueExecutor,
                                                                   RabbitMQMessagingService.this,
                                                                   consumerChannel,
                                                                   producerChannel,
                                                                   exchangeName,queueName,messageHandler,
                                                                   internalMessageDeserializer, messageAcker);
            messageQueue.initialize();
            return messageQueue;
        }
    }

    private final class RemoteMessageQueueFactory implements MessageQueueFactory {
        @Override
        public MessageQueue create(String name, MessageHandler messageHandler) throws Exception {
            final String queueName = format(QUEUE_NAME_FORMAT,elasticActorsCluster,name);
            ensureQueueExists(producerChannel,queueName);
            return new RemoteMessageQueue(RabbitMQMessagingService.this,producerChannel,exchangeName,queueName);
        }
    }

    private final class RemoteActorSystemMessageQueueFactory implements MessageQueueFactory {
        private final String clusterName;

        private RemoteActorSystemMessageQueueFactory(String clusterName) {
            this.clusterName = clusterName;
        }

        @Override
        public MessageQueue create(String name, MessageHandler messageHandler) throws Exception {
            final String queueName = format(QUEUE_NAME_FORMAT,this.clusterName,name);
            ensureQueueExists(producerChannel,queueName);
            return new RemoteMessageQueue(RabbitMQMessagingService.this,producerChannel,exchangeName,queueName);
        }
    }

    private final class RemoteActorSystemMessageQueueFactoryFactory implements MessageQueueFactoryFactory {
        @Override
        public MessageQueueFactory create(String clusterName) {
            return new RemoteActorSystemMessageQueueFactory(clusterName);
        }
    }
}
