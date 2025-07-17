// UI Manager class
export class UIManager {
    constructor() {
        this.elements = {
            chatInfo: document.getElementById('chatInfo'),
            messages: document.getElementById('messages'),
            msgInput: document.getElementById('msgInput'),
            typingStatus: document.getElementById('typingStatus'),
            sendBtn: document.getElementById('sendBtn')
        };
    }

    updateChatInfo(username, target, isOnline = false) {
        const onlineStatus = isOnline ? "🟢 Online" : "🔴 Offline";
        this.elements.chatInfo.innerHTML = `
            You are ${username}.
            Chatting with ${target}
            <span style="margin-left: 10px; font-size: 0.9em;">${onlineStatus}</span>
        `;
    }

    updateTypingStatus(text) {
        this.elements.typingStatus.textContent = text;
    }

    addMessage(message, type, status = null) {
        const div = document.createElement("div");
        div.className = `message ${type}`;

        if (type === "you" && status) {
            const icon = this.getStatusIcon(status);
            div.innerHTML = `${message} <span style="float:right">${icon}</span>`;
        } else {
            div.textContent = message;
        }

        this.elements.messages.appendChild(div);
        this.elements.messages.scrollTop = this.elements.messages.scrollHeight;
    }

    clearMessages() {
        this.elements.messages.innerHTML = '';
    }

    getStatusIcon(status) {
        switch (status) {
            case 'SEND': return '✓';
            case 'DELIVERED': return '✓✓';
            case 'SEEN': return '<span style="color:blue;">✓✓</span>';
            default: return '';
        }
    }

    enableInput() {
        this.elements.msgInput.disabled = false;
    }

    disableInput() {
        this.elements.msgInput.disabled = true;
    }

    getInputValue() {
        return this.elements.msgInput.value.trim();
    }

    clearInput() {
        this.elements.msgInput.value = '';
    }
}