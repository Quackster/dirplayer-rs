package com.dirplayer.player.xtra;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
import com.dirplayer.player.ScriptError;
import com.dirplayer.SimpleLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.LinkedList;

/**
 * MultiUser Xtra implementation for Director network multiplayer functionality.
 * Port of Rust MultiuserXtraManager.
 */
public class MultiUserXtra implements XtraManager.Xtra {
    private static final SimpleLogger logger = SimpleLogger.getLogger(MultiUserXtra.class);

    private final Map<Integer, MultiUserInstance> instances;
    private int instanceCounter;

    public MultiUserXtra() {
        this.instances = new HashMap<>();
        this.instanceCounter = 0;
    }

    @Override
    public String getName() {
        return "multiuser";
    }

    @Override
    public Datum createInstance() throws ScriptError {
        instanceCounter++;
        MultiUserInstance instance = new MultiUserInstance(instanceCounter);
        instances.put(instanceCounter, instance);

        Datum xtraInstance = new Datum(DatumType.XtraInstance);
        xtraInstance.setXtraName("multiuser");
        xtraInstance.setXtraInstanceId(instanceCounter);
        return xtraInstance;
    }

    @Override
    public Datum callMethod(Object instanceObj, String method, Datum[] args) throws ScriptError {
        int instanceId = getInstanceId(instanceObj);
        MultiUserInstance instance = instances.get(instanceId);
        if (instance == null) {
            throw new ScriptError("MultiUser instance not found: " + instanceId);
        }

        return callInstanceHandler(method, instance, args);
    }

    @Override
    public Datum getProperty(Object instanceObj, String property) throws ScriptError {
        return Datum.ofVoid();
    }

    @Override
    public void setProperty(Object instanceObj, String property, Datum value) throws ScriptError {
        // No-op for now
    }

    /**
     * Call an instance handler by name.
     */
    private Datum callInstanceHandler(String handlerName, MultiUserInstance instance, Datum[] args) throws ScriptError {
        switch (handlerName.toLowerCase()) {
            case "setnetbufferlimits":
                return Datum.ofVoid();

            case "setnetmessagehandler":
                return handleSetNetMessageHandler(instance, args);

            case "connecttonetserver":
                return handleConnectToNetServer(instance, args);

            case "getnetmessage":
                return handleGetNetMessage(instance);

            case "sendnetmessage":
                return handleSendNetMessage(instance, args);

            case "getneterrorstring":
                return Datum.ofString("");

            case "getnumusers":
                return Datum.ofInt(0);

            default:
                logger.warn("MultiUser method not implemented: {}", handlerName);
                return Datum.ofVoid();
        }
    }

    /**
     * Handle setNetMessageHandler call.
     * setNetMessageHandler(handlerSymbol, handlerObject, subject, senderID)
     */
    private Datum handleSetNetMessageHandler(MultiUserInstance instance, Datum[] args) throws ScriptError {
        if (args.length < 2) {
            throw new ScriptError("setNetMessageHandler requires at least 2 arguments");
        }

        Datum handlerSymbol = args[0];
        int handlerObjRef = -1;

        // The second argument is the handler object reference
        if (args.length >= 2 && args[1] != null) {
            if (args[1].isScriptInstanceRef()) {
                handlerObjRef = args[1].getScriptInstanceRef();
            }
        }

        // Get subject and sender filtering from args[2] and args[3]
        String subjectFilter = null;
        String senderFilter = null;
        if (args.length >= 3 && args[2] != null && !args[2].isVoid()) {
            subjectFilter = args[2].stringValue();
        }
        if (args.length >= 4 && args[3] != null && !args[3].isVoid()) {
            senderFilter = args[3].stringValue();
        }

        if (handlerSymbol.isVoid()) {
            instance.clearNetMessageHandler();
        } else {
            String symbolName = handlerSymbol.symbolValue();
            instance.setNetMessageHandler(handlerObjRef, symbolName, subjectFilter, senderFilter);
        }

        // Return error code 0 (success)
        return Datum.ofInt(0);
    }

    /**
     * Handle connectToNetServer call.
     * connectToNetServer(userName, password, serverID, port, movieID, mode, encryptionKey)
     */
    private Datum handleConnectToNetServer(MultiUserInstance instance, Datum[] args) throws ScriptError {
        if (args.length < 4) {
            throw new ScriptError("connectToNetServer requires at least 4 arguments");
        }

        // userNameString, passwordString, serverIDString, portNumber, movieIDString {, mode, encryptionKey}
        String host = args[2].stringValue();
        int port = args[3].intValue();

        String wsUrl = "ws://" + host + ":" + port;
        logger.info("Connecting to WebSocket: {}", wsUrl);

        try {
            instance.connect(wsUrl);
        } catch (Exception e) {
            logger.error("Failed to connect to WebSocket", e);
            throw new ScriptError("Failed to connect to server: " + e.getMessage());
        }

        return Datum.ofVoid();
    }

    /**
     * Handle getNetMessage call.
     * Returns a property list with message details or VOID if no messages.
     */
    private Datum handleGetNetMessage(MultiUserInstance instance) throws ScriptError {
        MultiUserMessage message = instance.nextMessage();
        if (message == null) {
            return Datum.ofVoid();
        }

        // Build the message property list
        // Format: [#errorCode: 0, #recipients: [list], #senderID: "...", #subject: "...", #content: ..., #timeStamp: ...]
        List<Datum.PropListPair> props = new ArrayList<>();

        // Note: In the actual implementation, these would be DatumRefs allocated through the player
        // For now, we return a simplified structure that can be processed by the caller
        return buildMessagePropList(message);
    }

    /**
     * Build a property list Datum from a MultiUserMessage.
     */
    private Datum buildMessagePropList(MultiUserMessage message) {
        // Create a list of recipients
        List<Integer> recipientRefs = new ArrayList<>();
        // Note: In full implementation, each recipient string would be allocated as a Datum

        List<Datum.PropListPair> props = new ArrayList<>();

        // The property list structure matches the Rust implementation:
        // errorCode, recipients, senderID, subject, content, timeStamp
        // For now, we return a simplified representation

        // Create property keys and values
        // In the real implementation, these would go through the DatumAllocator
        // This is a simplified version that returns inline data

        return Datum.ofPropListDirect(
            "errorCode", Datum.ofInt(message.errorCode),
            "recipients", Datum.ofList(new ArrayList<>(), false),
            "senderID", Datum.ofString(message.senderId),
            "subject", Datum.ofString(message.subject),
            "content", message.content,
            "timeStamp", Datum.ofInt((int) message.timeStamp)
        );
    }

    /**
     * Handle sendNetMessage call.
     * sendNetMessage(recipientID, subject, message)
     */
    private Datum handleSendNetMessage(MultiUserInstance instance, Datum[] args) throws ScriptError {
        if (args.length < 3) {
            throw new ScriptError("sendNetMessage requires at least 3 arguments");
        }

        String msgString = args[2].stringValue();
        logger.info("sendNetMessage: {}", msgString);

        if (!instance.isConnected()) {
            throw new ScriptError("Socket not connected");
        }

        instance.sendMessage(msgString);
        return Datum.ofVoid();
    }

    /**
     * Get instance ID from instance object.
     */
    private int getInstanceId(Object instanceObj) throws ScriptError {
        if (instanceObj instanceof Integer) {
            return (Integer) instanceObj;
        } else if (instanceObj instanceof Datum) {
            Datum datum = (Datum) instanceObj;
            if (datum.getType() == DatumType.XtraInstance) {
                return datum.getXtraInstanceId();
            }
        }
        throw new ScriptError("Invalid MultiUser instance reference");
    }

    /**
     * Get an instance by ID.
     */
    public MultiUserInstance getInstance(int instanceId) {
        return instances.get(instanceId);
    }

    /**
     * Check if a handler name has an async version.
     */
    public static boolean hasAsyncHandler(String name) {
        return false;
    }

    // =========================================================================
    // Inner Classes
    // =========================================================================

    /**
     * Represents a message received from the MultiUser server.
     */
    public static class MultiUserMessage {
        public int errorCode;
        public List<String> recipients;
        public String senderId;
        public String subject;
        public Datum content;
        public long timeStamp;

        public MultiUserMessage() {
            this.errorCode = 0;
            this.recipients = new ArrayList<>();
            this.senderId = "";
            this.subject = "";
            this.content = Datum.ofVoid();
            this.timeStamp = 0;
        }

        public MultiUserMessage(int errorCode, List<String> recipients, String senderId,
                                String subject, Datum content, long timeStamp) {
            this.errorCode = errorCode;
            this.recipients = recipients;
            this.senderId = senderId;
            this.subject = subject;
            this.content = content;
            this.timeStamp = timeStamp;
        }
    }

    /**
     * Represents a single MultiUser Xtra instance with its own connection and message queue.
     */
    public static class MultiUserInstance {
        private final int id;
        private int handlerObjRef;
        private String handlerSymbol;
        private String subjectFilter;  // Subject filter for message handler
        private String senderFilter;   // Sender ID filter for message handler
        private final LinkedList<MultiUserMessage> messageQueue;
        private WebSocket webSocket;
        private volatile boolean connected;

        // Callback interface for dispatching messages to the player
        private MessageDispatcher messageDispatcher;

        public MultiUserInstance(int id) {
            this.id = id;
            this.handlerObjRef = -1;
            this.handlerSymbol = null;
            this.messageQueue = new LinkedList<>();
            this.webSocket = null;
            this.connected = false;
        }

        public int getId() {
            return id;
        }

        public void setNetMessageHandler(int objRef, String symbol) {
            this.handlerObjRef = objRef;
            this.handlerSymbol = symbol;
            this.subjectFilter = null;
            this.senderFilter = null;
        }

        public void setNetMessageHandler(int objRef, String symbol, String subject, String sender) {
            this.handlerObjRef = objRef;
            this.handlerSymbol = symbol;
            this.subjectFilter = subject;
            this.senderFilter = sender;
        }

        public void clearNetMessageHandler() {
            this.handlerObjRef = -1;
            this.handlerSymbol = null;
            this.subjectFilter = null;
            this.senderFilter = null;
        }

        public String getSubjectFilter() {
            return subjectFilter;
        }

        public String getSenderFilter() {
            return senderFilter;
        }

        public boolean hasNetMessageHandler() {
            return handlerSymbol != null;
        }

        public int getHandlerObjRef() {
            return handlerObjRef;
        }

        public String getHandlerSymbol() {
            return handlerSymbol;
        }

        public void setMessageDispatcher(MessageDispatcher dispatcher) {
            this.messageDispatcher = dispatcher;
        }

        /**
         * Dispatch the message handler callback.
         */
        public void dispatchMessageHandler() {
            if (hasNetMessageHandler() && messageDispatcher != null) {
                messageDispatcher.dispatchCallback(handlerObjRef, handlerSymbol);
            }
        }

        /**
         * Add a message to the queue and dispatch the handler.
         * Subject and sender filtering is applied if set.
         */
        public void dispatchMessage(MultiUserMessage message) {
            // Apply subject filter if set
            if (subjectFilter != null && !subjectFilter.isEmpty()) {
                if (!subjectFilter.equals(message.subject)) {
                    return; // Message doesn't match subject filter
                }
            }
            // Apply sender filter if set
            if (senderFilter != null && !senderFilter.isEmpty()) {
                if (!senderFilter.equals(message.senderId)) {
                    return; // Message doesn't match sender filter
                }
            }
            messageQueue.add(message);
            dispatchMessageHandler();
        }

        /**
         * Get the next message from the queue.
         */
        public MultiUserMessage nextMessage() {
            return messageQueue.poll();
        }

        /**
         * Check if there are pending messages.
         */
        public boolean hasMessages() {
            return !messageQueue.isEmpty();
        }

        /**
         * Check if connected.
         */
        public boolean isConnected() {
            return connected;
        }

        /**
         * Connect to a WebSocket server.
         */
        public void connect(String wsUrl) throws Exception {
            HttpClient client = HttpClient.newHttpClient();

            CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        logger.info("WebSocket opened");
                        connected = true;

                        // Dispatch connection message
                        List<String> recipients = new ArrayList<>();
                        recipients.add("*");
                        dispatchMessage(new MultiUserMessage(
                            0,
                            recipients,
                            "System",
                            "ConnectToNetServer",
                            Datum.ofVoid(),
                            System.currentTimeMillis()
                        ));

                        webSocket.request(1);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        logger.info("WebSocket text message: {}", data);
                        handleIncomingMessage(data.toString());
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String text = new String(bytes);
                        logger.info("WebSocket binary message: {}", text);
                        handleIncomingMessage(text);
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        logger.info("WebSocket closed: {} - {}", statusCode, reason);
                        connected = false;
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        logger.error("WebSocket error", error);
                        connected = false;
                    }
                });

            this.webSocket = wsFuture.join();
        }

        /**
         * Handle an incoming message from the WebSocket.
         */
        private void handleIncomingMessage(String text) {
            List<String> recipients = new ArrayList<>();
            recipients.add("*");

            dispatchMessage(new MultiUserMessage(
                0,
                recipients,
                "System",
                "String",
                Datum.ofString(text),
                System.currentTimeMillis()
            ));
        }

        /**
         * Send a message through the WebSocket.
         */
        public void sendMessage(String message) {
            if (webSocket != null && connected) {
                webSocket.sendText(message, true);
            }
        }

        /**
         * Close the connection.
         */
        public void close() {
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
                connected = false;
            }
        }
    }

    /**
     * Interface for dispatching callbacks to the player.
     */
    public interface MessageDispatcher {
        void dispatchCallback(int handlerObjRef, String handlerSymbol);
    }
}
