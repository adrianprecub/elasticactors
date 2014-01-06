package org.elasticsoftware.elasticactors.cluster.scheduler;

import org.elasticsoftware.elasticactors.ActorRef;
import org.elasticsoftware.elasticactors.messaging.UUIDTools;
import org.elasticsoftware.elasticactors.serialization.MessageDeserializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author Joost van de Wijgerd
 */
public final class ScheduledMessageImpl implements ScheduledMessage {
    private final UUID id;
    private final long fireTime; // milliseconds since epoch
    private final ActorRef sender;
    private final ActorRef receiver;
    private final Class messageClass;
    private final ByteBuffer messageBytes;

    public ScheduledMessageImpl(long fireTime, ActorRef sender, ActorRef receiver, Class messageClass,ByteBuffer messageBytes) {
        this(UUIDTools.createTimeBasedUUID(),fireTime,sender,receiver, messageClass, messageBytes);
    }

    public ScheduledMessageImpl(UUID id, long fireTime, ActorRef sender, ActorRef receiver, Class messageClass, ByteBuffer messageBytes) {
        this.id = id;
        this.fireTime = fireTime;
        this.sender = sender;
        this.receiver = receiver;
        this.messageClass = messageClass;
        this.messageBytes = messageBytes;
    }


    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ActorRef getReceiver() {
        return receiver;
    }

    @Override
    public Class getMessageClass() {
        return messageClass;
    }

    @Override
    public ByteBuffer getMessageBytes() {
        return messageBytes;
    }

    public <T> T getPayload(MessageDeserializer<T> deserializer) throws IOException {
        return deserializer.deserialize(messageBytes);
    }

    @Override
    public ActorRef getSender() {
        return sender;
    }

    @Override
    public long getFireTime(TimeUnit timeUnit) {
        return timeUnit.convert(fireTime,TimeUnit.MILLISECONDS);
    }

    private long now() {
        return System.currentTimeMillis();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(fireTime - now(),TimeUnit.MILLISECONDS);
    }

    public int compareTo(Delayed other) {
        if (other == this)
            return 0;
        long d = (getDelay(TimeUnit.MILLISECONDS) -
                other.getDelay(TimeUnit.MILLISECONDS));
        if(d != 0) {
            return (d < 0) ? -1 : 1;
        } else {
            // use the ordering of the id as well in case the other Delayed is a ScheduledMessage as well
            if(other instanceof ScheduledMessage) {
                return getId().compareTo(((ScheduledMessage)other).getId());
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScheduledMessageImpl that = (ScheduledMessageImpl) o;

        if (fireTime != that.fireTime) return false;
        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (int) (fireTime ^ (fireTime >>> 32));
        return result;
    }
}
