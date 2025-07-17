import { Message } from '../models/Message.js';

// Message store to manage all messages
export class MessageStore {
    constructor() {
        this.messages = {};
    }

    // Generate conversation key using receiverId and conversationId
    getConversationKey(receiverId, conversationId) {
        return `${receiverId}_${conversationId}`;
    }

    addMessage(receiverId, conversationId, message) {
        console.log("new message send");
        const key = this.getConversationKey(receiverId, conversationId);
        if (!this.messages[key]) {
            this.messages[key] = [];
        }
        this.messages[key].push(message);
        console.log("after new message "+ key);
        console.table(this.messages[key]);
    }

    getMessages(receiverId, conversationId) {
        const key = this.getConversationKey(receiverId, conversationId);
        return this.messages[key] || [];
    }

    clearMessages(receiverId, conversationId) {
        const key = this.getConversationKey(receiverId, conversationId);
        this.messages[key] = [];
    }

    // Clear all messages for a specific receiver (across all conversations)
    clearAllMessagesForReceiver(receiverId) {
        const keysToDelete = Object.keys(this.messages).filter(key =>
            key.startsWith(`${receiverId}_`)
        );
        keysToDelete.forEach(key => {
            delete this.messages[key];
        });
    }

    // Clear all messages for a specific conversation (useful for group chats)
    clearAllMessagesForConversation(conversationId) {
        const keysToDelete = Object.keys(this.messages).filter(key =>
            key.endsWith(`_${conversationId}`)
        );
        keysToDelete.forEach(key => {
            delete this.messages[key];
        });
    }

    updateMessageStatus(receiverId, conversationId, msgId, newStatus) {
        console.log("inside message status");
        const key = this.getConversationKey(receiverId, conversationId);
        const chat = this.messages[key] || [];
        let messageFound = false;

        console.log(`chat ${key}:`);
        console.table(chat);
        

        for (let i = chat.length - 1; i >= 0; i--) {
            if (chat[i].msgId === msgId || (!chat[i].msgId && i === chat.length - 1)) {
                chat[i].updateStatus(newStatus);
                chat[i].setId(msgId);
                messageFound = true;
                break;
            }
        }

        return messageFound;
    }

    markPendingMessagesAsDelivered(receiverId, conversationId, currentUserId) {
        const key = this.getConversationKey(receiverId, conversationId);
        const chat = this.messages[key] || [];
        let hasUpdates = false;

        chat.forEach(message => {
            if (message.isFromUser(currentUserId) && message.isPending()) {
                message.updateStatus('DELIVERED');
                hasUpdates = true;
            }
        });

        return hasUpdates;
    }

    // Get all conversations for a specific receiver
    getConversationsForReceiver(receiverId) {
        const conversations = [];
        Object.keys(this.messages).forEach(key => {
            if (key.startsWith(`${receiverId}_`)) {
                const conversationId = key.split('_')[1];
                conversations.push({
                    conversationId,
                    messages: this.messages[key]
                });
            }
        });
        return conversations;
    }

    // Get message count for a specific conversation
    getMessageCount(receiverId, conversationId) {
        const key = this.getConversationKey(receiverId, conversationId);
        return this.messages[key] ? this.messages[key].length : 0;
    }

    // Get unread message count for a specific conversation
    getUnreadMessageCount(receiverId, conversationId, currentUserId) {
        const key = this.getConversationKey(receiverId, conversationId);
        const chat = this.messages[key] || [];

        return chat.filter(message =>
            !message.isFromUser(currentUserId) &&
            message.status !== 'SEEN'
        ).length;
    }

    // Mark all messages in a conversation as seen
    markAllMessagesAsSeen(receiverId, conversationId, currentUserId) {
        const key = this.getConversationKey(receiverId, conversationId);
        const chat = this.messages[key] || [];
        let hasUpdates = false;

        chat.forEach(message => {
            if (!message.isFromUser(currentUserId) && message.status !== 'SEEN') {
                message.updateStatus('SEEN');
                hasUpdates = true;
            }
        });

        return hasUpdates;
    }

    // Get the last message in a conversation
    getLastMessage(receiverId, conversationId) {
        const key = this.getConversationKey(receiverId, conversationId);
        const chat = this.messages[key] || [];
        return chat.length > 0 ? chat[chat.length - 1] : null;
    }

    // Debug method to see all stored conversations
    getAllConversationKeys() {
        return Object.keys(this.messages);
    }

    // Get total message count across all conversations
    getTotalMessageCount() {
        let total = 0;
        Object.values(this.messages).forEach(conversation => {
            total += conversation.length;
        });
        return total;
    }
}