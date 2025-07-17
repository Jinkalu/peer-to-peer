// Message class to represent individual messages
export class Message {
    constructor(from, msg, status = 'SEND', msgId = null) {
        this.from = from;
        this.msg = msg;
        this.status = status;
        this.msgId = msgId;
    }

    updateStatus(newStatus) {
        this.status = newStatus;
    }

    setId(msgId) {
        this.msgId = msgId;
    }

    isFromUser(userId) {
        return this.from === userId;
    }

    isPending() {
        return this.status === 'SEND';
    }
}