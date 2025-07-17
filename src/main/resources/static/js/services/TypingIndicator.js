import { Storage } from '../utils/Storage.js';

// Typing indicator class
export class TypingIndicator {
    constructor(socket) {
        this.socket = socket;
        this.timeout = null;
        this.lastTypingState = false;
    }

    start() {
        this.send(true);
        clearTimeout(this.timeout);
        this.timeout = setTimeout(() => {
            this.send(false);
        }, 1500);
    }

    send(isTyping) {
        const receiverId = Storage.get('targetUserId');

        if (this.lastTypingState === isTyping) return;
        this.lastTypingState = isTyping;

        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify({
                type: "typing",
                to: receiverId,
                isTyping: isTyping.toString()
            }));
        }
    }


    stop() {
        this.send(false);
        clearTimeout(this.timeout);
    }
}