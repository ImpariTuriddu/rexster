package com.tinkerpop.rexster.protocol.filter;

import com.tinkerpop.rexster.protocol.msg.ConsoleScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.ErrorResponseMessage;
import com.tinkerpop.rexster.protocol.msg.MessageFlag;
import com.tinkerpop.rexster.protocol.msg.MessageTokens;
import com.tinkerpop.rexster.protocol.msg.MessageType;
import com.tinkerpop.rexster.protocol.msg.MessageUtil;
import com.tinkerpop.rexster.protocol.msg.MsgPackScriptResponseMessage;
import com.tinkerpop.rexster.protocol.msg.RexProMessage;
import com.tinkerpop.rexster.protocol.msg.ScriptRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionRequestMessage;
import com.tinkerpop.rexster.protocol.msg.SessionResponseMessage;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handles incoming/outgoing RexProMessage instances.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RexProMessageFilter extends BaseFilter {
    private static final Logger logger = Logger.getLogger(RexProMessageFilter.class);
    private static final MessagePack msgpack = new MessagePack();

    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Get the source buffer from the context
        final Buffer sourceBuffer = ctx.getMessage();
        final int sourceBufferLength = sourceBuffer.remaining();

        // If source buffer doesn't contain header
        if (sourceBufferLength < 5) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        final byte messageType = sourceBuffer.get(0);
        final int bodyLength = sourceBuffer.getInt(1);
        final int completeMessageLength = 5 + bodyLength;

        // If the source message doesn't contain entire body
        if (sourceBufferLength < completeMessageLength) {
            // stop the filterchain processing and store sourceBuffer to be
            // used next time
            return ctx.getStopAction(sourceBuffer);
        }

        // Check if the source buffer has more than 1 complete message
        // If yes - split up the first message and the remainder
        final Buffer remainder = sourceBufferLength > completeMessageLength ?
                sourceBuffer.split(completeMessageLength) : null;

        byte[] messageAsBytes = new byte[bodyLength];
        sourceBuffer.position(5);
        sourceBuffer.get(messageAsBytes);

        final ByteArrayInputStream in = new ByteArrayInputStream(messageAsBytes);
        final Unpacker unpacker = msgpack.createUnpacker(in);
        unpacker.setArraySizeLimit(Integer.MAX_VALUE);
        unpacker.setMapSizeLimit(Integer.MAX_VALUE);
        unpacker.setRawSizeLimit(Integer.MAX_VALUE);

        RexProMessage message = null;
        if (messageType == MessageType.SCRIPT_REQUEST) {
            message = unpacker.read(ScriptRequestMessage.class);
        } else if (messageType == MessageType.SESSION_REQUEST) {
            message = unpacker.read(SessionRequestMessage.class);
        } else if (messageType == MessageType.CONSOLE_SCRIPT_RESPONSE) {
            message = unpacker.read(ConsoleScriptResponseMessage.class);
        } else if (messageType == MessageType.SESSION_RESPONSE) {
            message = unpacker.read(SessionResponseMessage.class);
        } else if (messageType == MessageType.ERROR) {
            message = unpacker.read(ErrorResponseMessage.class);
        } else if (messageType == MessageType.MSGPACK_SCRIPT_RESPONSE) {
            message = unpacker.read(MsgPackScriptResponseMessage.class);
        }

        if (message == null) {
            logger.warn("Message did not match an expected type.");

            ctx.write(MessageUtil.createErrorResponse(RexProMessage.EMPTY_REQUEST_AS_BYTES,
                    RexProMessage.EMPTY_SESSION_AS_BYTES, MessageFlag.ERROR_MESSAGE_VALIDATION,
                    MessageTokens.ERROR_UNEXPECTED_MESSAGE_TYPE));
        }

        ctx.setMessage(message);

        sourceBuffer.tryDispose();

        return ctx.getInvokeAction(remainder);
    }

    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        // Get the source message to be written
        final RexProMessage msg = ctx.getMessage();

        final ByteArrayOutputStream rexProMessageStream = new ByteArrayOutputStream();
        final Packer packer = msgpack.createPacker(rexProMessageStream);
        packer.write(msg);
        byte[] rexProMessageAsBytes = rexProMessageStream.toByteArray();
        rexProMessageStream.close();

        final ByteBuffer bb = ByteBuffer.allocate(5 + rexProMessageAsBytes.length);

        if (msg instanceof SessionResponseMessage) {
            bb.put(MessageType.SESSION_RESPONSE);
        } else if (msg instanceof ConsoleScriptResponseMessage) {
            bb.put(MessageType.CONSOLE_SCRIPT_RESPONSE);
        } else if (msg instanceof ErrorResponseMessage) {
            bb.put(MessageType.ERROR);
        } else if (msg instanceof ScriptRequestMessage) {
            bb.put(MessageType.SCRIPT_REQUEST);
        } else if (msg instanceof SessionRequestMessage) {
            bb.put(MessageType.SESSION_REQUEST);
        } else if (msg instanceof MsgPackScriptResponseMessage) {
            bb.put(MessageType.MSGPACK_SCRIPT_RESPONSE);
        }

        bb.putInt(rexProMessageAsBytes.length);
        bb.put(rexProMessageAsBytes);

        final byte[] messageAsBytes = bb.array();
        final int size = messageAsBytes.length;

        // Retrieve the memory manager
        final MemoryManager memoryManager =
                ctx.getConnection().getTransport().getMemoryManager();

        // allocate the buffer of required size
        final Buffer output = memoryManager.allocate(size);
        output.put(messageAsBytes);

        // Allow Grizzly core to dispose the buffer, once it's written
        output.allowBufferDispose(true);

        // Set the Buffer as a context message
        ctx.setMessage(output.flip());

        // Instruct the FilterChain to call the next filter
        return ctx.getInvokeAction();
    }
}