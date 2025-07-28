let socket;
let presenceSocket;
let typingTimeout;
let lastTypingState = false;
let conversationId = null;
let isReceiverOnline = false;
const messageStore = {};

function connect() {
    const sender = localStorage.getItem("username");
    const senderId = localStorage.getItem("userId");
    const receiver = localStorage.getItem("targetUsername");
    const receiverId = localStorage.getItem("targetUserId");
    conversationId = localStorage.getItem("conversationId");
    localStorage.setItem("isReceiverOnScreen",false);

    if (!sender || !receiverId || !conversationId) {
        showNotification("Missing required data. Please go back to the user list.", "error");
        setTimeout(() => {
            window.location.href = "/user-list";
        }, 2000);
        return;
    }

    // Update UI with user info
    updateChatHeader(sender, receiver);
    updateConnectionStatus("connecting");

    // Connect to presence tracking for the receiver
    connectPresenceSocket(receiverId, senderId, conversationId);

    // Connect to chat WebSocket
    const url = `ws://localhost:8080/chat?sender=${senderId}&type=private&conversationId=${conversationId}`;
    console.log("✅ Connecting with conversation ID:", conversationId);

    socket = new WebSocket(url);

    socket.onopen = () => {
        console.log("✅ Connected to private chat");
        document.getElementById("msgInput").disabled = false;
        document.getElementById("sendButton").disabled = false;
        updateConnectionStatus("connected");
        loadHistory();
    };

    socket.onmessage = (event) => {
        const data = parseJson(event.data);
        if (!data) return;
        console.log({data});

        if (typeof data === 'string' && data.startsWith("conversationId:")) {
            console.log("Received conversation ID confirmation:", data);
            return;
        }

        if (data.type === "typing") {
            localStorage.setItem("isReceiverOnScreen",true);
            handleTypingStatus(data);
            return;
        }

        if (data.type === "status") {
            console.log(`msgId : ${data.msgId}  status ${data.status}`);
            updateMessageStatus(data.msgId, data.status);
            return;
        }

      

        if(data.type === 'reload'){
            console.log("reload.....!!!!");
            // localStorage.setItem("isReceiverOnScreen",true);
            messageStore[conversationId] = [];
            loadHistory();
            return;
        }

        if (data.type === "receiverStatus") {
            localStorage.setItem("isReceiverOnScreen",false);
            return;
        }



        if (data.conversationId && data.sender && data.msg) {
            console.log("incoming msg : : ", data);
            const from = data.sender;
            const msg = data.msg;
            const msgId = data.msgId;

            if (!messageStore[conversationId]) {
                messageStore[conversationId] = [];
            }

            let status = 'SEND';
            if (isReceiverOnline && localStorage.getItem("isReceiverOnScreen")) {
                status = 'SEEN'
            }else if (isReceiverOnline) {
                status = 'DELIVERED'
            }
            console.log(`initial status ${status}`);
            messageStore[conversationId].push({
                from,
                msg,
                msgId,
                status: status,
                timestamp: new Date()
            });

            // Clear typing indicator
            clearTypingIndicator();

            // Add message to UI
            addMessageToUI(msg, false, status , new Date(), msgId);
        }
    };

    socket.onclose = () => {
        console.warn("WebSocket connection closed");
        document.getElementById("msgInput").disabled = true;
        document.getElementById("sendButton").disabled = true;
        updateConnectionStatus("disconnected");
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
        updateConnectionStatus("error");
    };

    setupEventListeners();
}

function updateChatHeader(currentUser, targetUser) {
    const username = document.getElementById("chatUsername");
    const userInitial = document.getElementById("userInitial");

    username.textContent = targetUser || 'User';
    userInitial.textContent = (targetUser || 'U')[0].toUpperCase();
}

function updateConnectionStatus(status) {
    const statusElement = document.getElementById("connectionStatus");
    const statusText = statusElement.querySelector(".status-text");
    const statusIndicator = statusElement.querySelector(".status-indicator");

    statusIndicator.className = "status-indicator";
    statusElement.classList.remove("show");

    switch(status) {
        case "connecting":
            statusText.textContent = "Connecting...";
            statusElement.classList.add("show");
            break;
        case "connected":
            statusText.textContent = "Connected";
            statusIndicator.classList.add("connected");
            statusElement.classList.add("show");
            setTimeout(() => statusElement.classList.remove("show"), 2000);
            break;
        case "disconnected":
            statusText.textContent = "Disconnected";
            statusElement.classList.add("show");
            break;
        case "error":
            statusText.textContent = "Connection Error";
            statusElement.classList.add("show");
            break;
    }
}

function connectPresenceSocket(targetUserId, currentUserId, conversationId) {
    console.log(`presence function ${targetUserId} :: ${currentUserId} :: ${conversationId}`)
    presenceSocket = new WebSocket(
        `ws://localhost:8080/presence?type=subscribe&target=${targetUserId}&user=${currentUserId}&convoId=${conversationId}`
    );

    presenceSocket.onopen = () => {
        console.log("✅ Connected to presence tracking for user:", targetUserId);
    };

    presenceSocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        console.log("Presence update:", data);

        if (data.user === targetUserId) {
            isReceiverOnline = data.online;

            updatePresenceIndicator(data.online);

            // If user comes online, mark pending messages as delivered
            if (data.online) {
                markPendingMessagesAsDelivered();
            }
        }
    };

    presenceSocket.onclose = () => {
        console.warn("Presence socket closed, retrying...");
        setTimeout(() => connectPresenceSocket(targetUserId, currentUserId, conversationId), 3000);
    };

    presenceSocket.onerror = (error) => {
        console.error("Presence socket error:", error);
    };
}

function updatePresenceIndicator(isOnline) {
    const userAvatar = document.querySelector(".user-avatar");
    const userStatus = document.getElementById("userStatus");

    if (isOnline) {
        userAvatar.classList.add("online");
        userStatus.innerHTML = '<span class="status-dot"></span>Online';
        userStatus.className = "status online";
    } else {
        userAvatar.classList.remove("online");
        userStatus.innerHTML = '<span class="status-dot"></span>Offline';
        userStatus.className = "status offline";
    }
}

function markPendingMessagesAsDelivered() {
    const chat = messageStore[conversationId] || [];
    const currentUserId = localStorage.getItem("userId");
    let hasUpdates = false;

    chat.forEach(message => {
        if (message.from === currentUserId && message.status === 'SEND') {
            message.status = 'DELIVERED';
            hasUpdates = true;
        }
    });

    if (hasUpdates) {
        console.log("✅ Marked pending messages as delivered");
        reloadMessages();
    }
}

function sendMessage() {
    const msgBox = document.getElementById("msgInput");
    const msg = msgBox.value.trim();
    const receiver = localStorage.getItem("targetUserId");
    const sender = localStorage.getItem("userId");

    if (!msg || socket.readyState !== WebSocket.OPEN) return;

    const payload = {
        type: "msg",
        receiver: receiver,
        msg: msg
    };

    socket.send(JSON.stringify(payload));

    if (!messageStore[conversationId]) messageStore[conversationId] = [];

     let initialStatus = 'SEND';

     if (isReceiverOnline && localStorage.getItem("isReceiverOnScreen")) {
        initialStatus = 'SEEN';
     }else if(isReceiverOnline){
        initialStatus = 'DELIVERED';
     }
   
    const timestamp = new Date();

    const localMessage = {
        from: sender,
        msg,
        status: initialStatus,
        msgId: null,
        timestamp
    };
    messageStore[conversationId].push(localMessage);

    // Add message to UI
    addMessageToUI(msg, true, initialStatus, timestamp);

    msgBox.value = '';
    sendTypingStatus(false);

    // Auto-focus input
    msgBox.focus();
}

function addMessageToUI(message, isSent, status, timestamp, msgId = null) {
    const messagesContainer = document.getElementById("messages");
    const messageDiv = document.createElement("div");
    messageDiv.className = `message-bubble ${isSent ? 'sent' : 'received'}`;

    const timeStr = timestamp.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

    messageDiv.innerHTML = `
        <div class="message-content">${escapeHtml(message)}</div>
        <div class="message-meta">
            <span class="message-time">${timeStr}</span>
            ${isSent ? `<span class="message-status">${getStatusIcon(status)}</span>` : ''}
        </div>
    `;

    if (msgId) {
        messageDiv.dataset.msgId = msgId;
    }

    messagesContainer.appendChild(messageDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function handleTypingStatus(data) {
    const receiverUsername = localStorage.getItem("targetUsername");
    const receiverId = localStorage.getItem("targetUserId");

    if (data.from === receiverId) {
        if (data.isTyping === "true") {
            showTypingIndicator(receiverUsername);
        } else {
            clearTypingIndicator();
        }
    }
}

function showTypingIndicator(username) {
    const typingStatus = document.getElementById("typingStatus");
    typingStatus.innerHTML = `
        ${username} is typing
        <div class="typing-dots">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
        </div>
    `;
}

function clearTypingIndicator() {
    const typingStatus = document.getElementById("typingStatus");
    typingStatus.innerHTML = '';
}

function setupEventListeners() {
    const inputBox = document.getElementById("msgInput");

    // Typing indicator
    inputBox.addEventListener("input", () => {
        sendTypingStatus(true);
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => {
            sendTypingStatus(false);
        }, 1500);
    });

    // Enter key to send
    inputBox.addEventListener("keypress", (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // Auto-resize input
    inputBox.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 120) + 'px';
    });
}

function sendTypingStatus(isTyping) {
    const receiverId = localStorage.getItem("targetUserId");

    if (lastTypingState === isTyping) return;
    lastTypingState = isTyping;

    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: "typing",
            receiver: receiverId,
            isTyping: isTyping.toString()
        }));
    }
}

function updateMessageStatus(msgId, newStatus) {
    console.log({msgId, newStatus});
    const chat = messageStore[conversationId] || [];
    console.log({messageStore});

    let messageFound = false;
    for (let i = chat.length - 1; i >= 0; i--) {
        if (chat[i].msgId === msgId || (!chat[i].msgId && i === chat.length - 1)) {
            // chat[i].status = newStatus;
            chat[i].msgId = msgId;
            messageFound = true;
            break;
        }
    }

    if (messageFound) {
        // updateMessageStatusInUI(msgId, newStatus);
    }
}

function updateMessageStatusInUI(msgId, status) {
    // Find message in UI and update status
    const messages = document.querySelectorAll('.message-bubble.sent');
    const targetMessage = msgId ?
        Array.from(messages).find(msg => msg.dataset.msgId === msgId) :
        messages[messages.length - 1]; // Last message if no msgId

    if (targetMessage) {
        const statusSpan = targetMessage.querySelector('.message-status');
        if (statusSpan) {
            statusSpan.innerHTML = getStatusIcon(status);
        }
    }
}

function reloadMessages() {
    const messagesContainer = document.getElementById("messages");
    messagesContainer.innerHTML = '';

    const chat = messageStore[conversationId] || [];
    const currentUser = localStorage.getItem("userId");

    chat.forEach(m => {
        const isSent = m.from === currentUser;
        addMessageToUI(m.msg, isSent, m.status, m.timestamp || new Date(), m.msgId);
    });
}

function getStatusIcon(status) {
    console.log(`inside status icon ${status}`)
    switch (status) {
        case 'SEND':
            return '<span class="status-icon sent">✓</span>';
        case 'DELIVERED':
            return '<span class="status-icon delivered">✓✓</span>';
        case 'SEEN':
            return '<span class="status-icon seen">✓✓</span>';
        default:
            return '';
    }
}

function loadHistory() {
    if (!conversationId || conversationId.trim() === "") {
        console.log("No conversation ID available for loading history");
        return;
    }

    const url = `http://localhost:8080/api/chat-history?conversationId=${conversationId}`;
    const senderId = localStorage.getItem("userId");

    // Clear existing chat history for the conversation
    messageStore[conversationId] = [];
    document.getElementById("messages").innerHTML = '';

    fetch(url)
        .then(res => {
            if (!res.ok) {
                console.error(`HTTP error! status: ${res.status}`);
                return null;
            }
            return res.json();
        })
        .then(data => {
            if (!data || data.length === 0) {
                console.log("No chat history available");
                return;
            }

            // Populate messageStore with chat history
            data.forEach(msg => {
                const isYou = msg.senderUUID === senderId;
                const timestamp = new Date(msg.timestamp || Date.now());

                messageStore[conversationId].push({
                    from: msg.senderUUID,
                    msg: msg.message,
                    status: msg.status,
                    msgId: msg.id,
                    timestamp
                });

                // Add message to UI
                addMessageToUI(msg.message, isYou, msg.status, timestamp, msg.id);
            });

            console.log("history:", messageStore);
        })
        .catch(err => {
            console.error("Chat history fetch failed:", err);
            showNotification("Failed to load chat history", "error");
        });
}

function parseJson(raw) {
    try {
        return JSON.parse(raw);
    } catch {
        console.warn("Non-JSON message:", raw);
        return raw;
    }
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function goBack() {
    // Add exit animation
    const chatContainer = document.querySelector('.chat-container');
    chatContainer.style.transform = 'translateY(30px) scale(0.95)';
    chatContainer.style.opacity = '0';

    setTimeout(() => {
        window.location.href = "/user-list";
    }, 300);
}

function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 80px;
        right: 20px;
        padding: 16px 24px;
        border-radius: 12px;
        color: white;
        font-weight: 500;
        z-index: 1000;
        transform: translateX(400px);
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        backdrop-filter: blur(10px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
        ${type === 'success'
            ? 'background: rgba(34, 197, 94, 0.9);'
            : 'background: rgba(239, 68, 68, 0.9);'
        }
    `;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.transform = 'translateX(0)';
    }, 100);

    setTimeout(() => {
        notification.style.transform = 'translateX(400px)';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Handle page events
window.onload = connect;

window.onbeforeunload = () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        sendTypingStatus(false);
        socket.close();
    }

    if (presenceSocket && presenceSocket.readyState === WebSocket.OPEN) {
        presenceSocket.close();
    }
};