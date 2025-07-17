export class PresenceTracker {
    constructor(onPresenceChange) {
        this.socket = null;
        this.onPresenceChange = onPresenceChange;
        this.isReceiverOnline = false;
    }

    connect(targetUserId, currentUserId) {
        this.socket = new WebSocket(
            `ws://localhost:8080/presence?type=subscribe&target=${targetUserId}&user=${currentUserId}`
        );

        this.socket.onopen = () => {
            console.log("✅ Connected to presence tracking for user:", targetUserId);
        };

        this.socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log("Presence update:", data);

            if (data.user === targetUserId) {
                this.isReceiverOnline = data.online;
                this.onPresenceChange(data.online);
            }
        };

        this.socket.onclose = () => {
            console.warn("Presence socket closed, retrying...");
            setTimeout(() => this.connect(targetUserId, currentUserId), 3000);
        };

        this.socket.onerror = (error) => {
            console.error("Presence socket error:", error);
        };
    }

    disconnect() {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.close();
        }
    }

    getReceiverStatus() {
        return this.isReceiverOnline;
    }
}