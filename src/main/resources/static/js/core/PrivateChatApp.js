import { Storage } from '../utils/Storage.js';
import { Message } from '../models/Message.js';
import { MessageStore } from '../services/MessageStore.js';
import { UIManager } from '../ui/UIManager.js';
import { PresenceTracker } from '../services/PresenceTracker.js';
import { TypingIndicator } from '../services/TypingIndicator.js';

// Main chat application class
export class PrivateChatApp {
    constructor() {
        this.socket = null;
        this.conversationId = null;
        this.messageStore = new MessageStore();
        this.uiManager = new UIManager();
        this.presenceTracker = new PresenceTracker(this.handlePresenceChange.bind(this));
        this.typingIndicator = null;

        this.userData = Storage.getUserData();
    }

    // Initialize the application
    init() {
        if (!this.userData.username || !this.userData.target) {
            alert("Missing sender or receiver. Please go back to the home page.");
            return;
        }

        this.uiManager.updateChatInfo(this.userData.username, this.userData.target);
        this.presenceTracker.connect(this.userData.targetUserId, this.userData.userId);
        this.connect();
        this.setupEventListeners();
    }

    // Connect to WebSocket
    connect() {
        let url;
        if (this.userData.conversationId && this.userData.conversationId.trim() !== "") {
            this.conversationId = this.userData.conversationId;
            url = `ws://localhost:8080/chat?sender=${this.userData.userId}&type=private&conversationId=${this.userData.conversationId}`;
            console.log("✅ Using existing conversation:", this.userData.conversationId);
        } else {
            url = `ws://localhost:8080/chat?sender=${this.userData.userId}&receiver=${this.userData.targetUserId}&type=private`;
            console.log("✅ Creating new conversation with:", this.userData.target);
        }

        this.socket = new WebSocket(url);
        this.typingIndicator = new TypingIndicator(this.socket);

        this.socket.onopen = () => {
            console.log("✅ Connected to private chat");
            this.uiManager.enableInput();

            if (this.conversationId) {
                this.loadHistory();
            }
        };

        this.socket.onmessage = (event) => {
            this.handleMessage(event.data);
        };

        this.socket.onclose = () => {
            console.warn("WebSocket connection closed");
            this.uiManager.disableInput();
        };

        this.socket.onerror = (error) => {
            console.error("WebSocket error:", error);
        };
    }

    // Handle incoming messages
    handleMessage(rawData) {
        const data = this.parseJson(rawData);
        if (!data) return;

        console.log({data});

        // Handle conversation ID
        if (typeof data === 'string' && data.startsWith("conversationId:")) {
            this.conversationId = data.split(":")[1];
            Storage.set("conversationId", this.conversationId);
            console.log("✅ New conversation ID received:", this.conversationId);
            this.loadHistory();
            return;
        }

        // Handle typing indicator
        if (data.type === "typing") {
            if (data.from === this.userData.targetUserId) {
                const typingText = data.isTyping === "true" ? `${this.userData.target} is typing...` : "";
                this.uiManager.updateTypingStatus(typingText);
            }
            return;
        }

        // Handle message status updates
        if (data.type === "status") {
            console.log(`msgId: ${data.msgId} status: ${data.status} statusReceiver: ${data.statusReceiver}`);
            this.updateMessageStatus(data.msgId, data.status);
            return;
        }

        // Handle reload command
        if (data.type === 'reload') {
            console.log("reload.....!!!!");
            if (this.conversationId) {
                this.messageStore.clearMessages(this.userData.targetUserId, this.conversationId);
            }
            this.loadHistory();
            return;
        }

        // Handle regular messages
        if (data.conversationId && data.sender && data.msg) {
            console.log("incoming msg:", data);
            this.handleIncomingMessage(data);
        }
    }

    // Handle incoming chat messages
    handleIncomingMessage(data) {
        // Ensure we have a conversationId
        if (!this.conversationId) {
            this.conversationId = data.conversationId;
            Storage.set("conversationId", this.conversationId);
        }

        const message = new Message(data.sender, data.msg, 'DELIVERED', data.msgId);
        this.messageStore.addMessage(this.userData.targetUserId, this.conversationId, message);
        this.uiManager.updateTypingStatus("");
        this.uiManager.addMessage(`Received: ${data.msg}`, "other");
    }

    // Handle presence changes
    handlePresenceChange(isOnline) {
        this.uiManager.updateChatInfo(this.userData.username, this.userData.target, isOnline);

        if (isOnline) {
            this.markPendingMessagesAsDelivered();
        }
    }

    // Mark pending messages as delivered
    markPendingMessagesAsDelivered() {
        if (!this.conversationId) return;

        const hasUpdates = this.messageStore.markPendingMessagesAsDelivered(
            this.userData.targetUserId,
            this.conversationId,
            this.userData.userId
        );

        if (hasUpdates) {
            console.log("✅ Marked pending messages as delivered");
            this.reloadMessages();
        }
    }

    // Send a message
    sendMessage() {
        const msg = this.uiManager.getInputValue();
        if (!msg || this.socket.readyState !== WebSocket.OPEN) return;

        const payload = {
            receiver: this.userData.targetUserId,
            msg: msg
        };

        this.socket.send(JSON.stringify(payload));

        // Determine initial status based on receiver's online status
        const initialStatus = this.presenceTracker.getReceiverStatus() ? 'DELIVERED' : 'SEND';
        const message = new Message(this.userData.userId, msg, initialStatus);

        // Only add to store if we have a conversationId
        if (this.conversationId) {
            this.messageStore.addMessage(this.userData.targetUserId, this.conversationId, message);
        }

        this.uiManager.addMessage(`You: ${msg}`, "you", initialStatus);
        this.uiManager.clearInput();
        this.typingIndicator.stop();
    }

    // Update message status
    updateMessageStatus(msgId, newStatus) {
        
        if (!this.conversationId) return;

        const messageFound = this.messageStore.updateMessageStatus(
            this.userData.targetUserId,
            this.conversationId,
            msgId,
            newStatus
        );

        console.log("isMessage : : "+messageFound);

        if (messageFound) {
            this.reloadMessages();
        }
    }

    // Reload and display all messages
    reloadMessages() {

        console.log("reloading messages...");
        
        if (!this.conversationId) return;

        this.uiManager.clearMessages();
        const messages = this.messageStore.getMessages(this.userData.targetUserId, this.conversationId);

        messages.forEach(message => {
            const isYou = message.isFromUser(this.userData.userId);
            const displayText = `${isYou ? "You" : "Received"}: ${message.msg}`;
            this.uiManager.addMessage(displayText, isYou ? "you" : "other", message.status);
        });
    }

    // Load chat history from server
    async loadHistory() {
        const cid = this.conversationId || Storage.get("conversationId");
        if (!cid || cid.trim() === "") {
            console.log("No conversation ID available for loading history");
            return;
        }

        try {
            const response = await fetch(`http://localhost:8080/api/chat-history?conversationId=${cid}`);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            data.forEach(msg => {
                const message = new Message(msg.senderUUID, msg.message, msg.status, msg.id);
                this.messageStore.addMessage(this.userData.targetUserId, this.conversationId, message);

                const isYou = message.isFromUser(this.userData.userId);
                const displayText = `${isYou ? "You" : "Received"}: ${message.msg}`;
                this.uiManager.addMessage(displayText, isYou ? "you" : "other", message.status);
            });
        } catch (error) {
            console.error("Chat history fetch failed:", error);
        }
    }

    // Setup event listeners
    setupEventListeners() {
        // Typing indicator
        this.uiManager.elements.msgInput.addEventListener("input", () => {
            this.typingIndicator.start();
        });

        // Enter key for sending messages
        this.uiManager.elements.msgInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.sendMessage();
            }
        });

        this.uiManager.elements.sendBtn.addEventListener('click', (e) => {
                this.sendMessage();
        });

        // Handle page unload
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });
    }

    // Cleanup resources
    cleanup() {
        if (this.typingIndicator) {
            this.typingIndicator.stop();
        }

        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.close();
        }

        this.presenceTracker.disconnect();
    }

    // Utility method to parse JSON
    parseJson(raw) {
        try {
            return JSON.parse(raw);
        } catch {
            console.warn("Non-JSON message:", raw);
            return raw;
        }
    }

    // Additional utility methods for the new MessageStore structure

    // Get conversation statistics
    getConversationStats() {
        if (!this.conversationId) return null;

        return {
            totalMessages: this.messageStore.getMessageCount(this.userData.targetUserId, this.conversationId),
            unreadMessages: this.messageStore.getUnreadMessageCount(this.userData.targetUserId, this.conversationId, this.userData.userId),
            lastMessage: this.messageStore.getLastMessage(this.userData.targetUserId, this.conversationId)
        };
    }

    // Mark all messages as seen (when user focuses on chat)
    markAllMessagesAsSeen() {
        if (!this.conversationId) return;

        const hasUpdates = this.messageStore.markAllMessagesAsSeen(
            this.userData.targetUserId,
            this.conversationId,
            this.userData.userId
        );

        if (hasUpdates) {
            console.log("✅ Marked all messages as seen");
            this.reloadMessages();
        }
    }
}