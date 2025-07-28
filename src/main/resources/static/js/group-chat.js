let socket;
let typingTimeout;
let lastTypingState = false;
let conversationId = null;
let currentUser = null;
let groupName = null;
const messageStore = {};

function connect() {
    currentUser = localStorage.getItem("username");
    const senderId = localStorage.getItem("userId");
    conversationId = localStorage.getItem("conversationId");
    groupName = localStorage.getItem("groupName");

    if (!currentUser || !senderId || !conversationId) {
        alert("Missing required data. Please go back to the user list.");
        return;
    }

    document.getElementById("chatInfo").innerHTML = `
        <div>Group: <strong>${groupName}</strong></div>
        <div class="members-count">You are ${currentUser}</div>
    `;

    // Connect to group chat WebSocket
    const url = `ws://localhost:8080/group-chat?sender=${senderId}&conversationId=${conversationId}`;
    console.log("✅ Connecting to group chat:", conversationId);

    socket = new WebSocket(url);

    socket.onopen = () => {
        console.log("✅ Connected to group chat");
        document.getElementById("msgInput").disabled = false;
        loadHistory();
    };

    socket.onmessage = (event) => {
        const data = parseJson(event.data);
        if (!data) return;
        console.log("Received:", data);

        if (data.type === "typing") {
            handleTypingStatus(data);
            return;
        }

        if (data.type === "msg") {
            handleIncomingMessage(data);
            return;
        }

        // Handle other message types if needed
        console.log("Unhandled message type:", data);
    };

    socket.onclose = () => {
        console.warn("Group chat WebSocket connection closed");
        document.getElementById("msgInput").disabled = true;
    };

    socket.onerror = (error) => {
        console.error("Group chat WebSocket error:", error);
    };

    setupTypingListener();
}

function handleTypingStatus(data) {
    const typingStatus = document.getElementById("typingStatus");
    const fromUser = data.from;
    
    if (data.isTyping === "true") {
        typingStatus.textContent = `${fromUser} is typing...`;
    } else {
        typingStatus.textContent = "";
    }
}

function handleIncomingMessage(data) {
    const from = data.from;
    const msg = data.msg;
    const fromUsername = data.fromUsername;
    
    if (!messageStore[conversationId]) {
        messageStore[conversationId] = [];
    }

    // Add message to store
    messageStore[conversationId].push({
        fromUsername: fromUsername,
        from: from,
        msg: msg,
        timestamp: new Date().toLocaleTimeString()
    });

    // Clear typing status
    document.getElementById("typingStatus").textContent = "";
    
    // Display message
    const currentUserId = localStorage.getItem("userId");
    const isYou = from === currentUserId;
    logMessage(msg, isYou ? "you" : "other", fromUsername);
}

function sendMessage() {
    const msgBox = document.getElementById("msgInput");
    const msg = msgBox.value.trim();
    const senderId = localStorage.getItem("userId");

    if (!msg || socket.readyState !== WebSocket.OPEN) return;

    const payload = {
        type: "msg",
        msg: msg
    };

    socket.send(JSON.stringify(payload));

    // Add to local message store
    if (!messageStore[conversationId]) {
        messageStore[conversationId] = [];
    }

    messageStore[conversationId].push({
        from: senderId,
        msg: msg,
        timestamp: new Date().toLocaleTimeString()
    });

    // Display message immediately
    logMessage(msg, "you", currentUser);
    
    msgBox.value = '';
    sendTypingStatus(false);
}

function setupTypingListener() {
    const inputBox = document.getElementById("msgInput");
    
    inputBox.addEventListener("input", () => {
        sendTypingStatus(true);
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => {
            sendTypingStatus(false);
        }, 1500);
    });
}

function sendTypingStatus(isTyping) {
    if (lastTypingState === isTyping) return;
    lastTypingState = isTyping;

    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: "typing",
            isTyping: isTyping.toString()
        }));
    }
}

function logMessage(message, type = "info", sender = null) {
    const messages = document.getElementById("messages");
    const div = document.createElement("div");
    div.className = `message ${type}`;

    if (type === "you") {
        div.innerHTML = `
            <div class="sender">You</div>
            <div class="content">${escapeHtml(message)}</div>
            <div class="timestamp">${new Date().toLocaleTimeString()}</div>
        `;
    } else {
        div.innerHTML = `
            <div class="sender">${escapeHtml(sender || 'Unknown')}</div>
            <div class="content">${escapeHtml(message)}</div>
            <div class="timestamp">${new Date().toLocaleTimeString()}</div>
        `;
    }

    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function loadHistory() {
    if (!conversationId || conversationId.trim() === "") {
        console.log("No conversation ID available for loading history");
        return;
    }

    const url = `http://localhost:8080/api/chat-history?conversationId=${conversationId}`;
    const senderId = localStorage.getItem("userId");

    // Clear existing chat history
    messageStore[conversationId] = [];

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
                console.log("No group chat history available");
                return;
            }
            
            // Clear messages display
            document.getElementById("messages").innerHTML = '';
            
            // Populate messageStore and display messages
            data.forEach(msg => {
                const isYou = msg.senderUUID === senderId;
                
                messageStore[conversationId].push({
                    fromUsername:msg.fromUsername,
                    from: msg.senderUUID,
                    msg: msg.message,
                    timestamp: msg.createdAt || new Date().toLocaleTimeString()
                });

                // For group messages, we need to show the sender's username
                // You might need to fetch username from your user service
                logMessage(msg.message, isYou ? "you" : "other", 
                    isYou ? currentUser : (msg.senderUsername || `User ${msg.senderUUID}`));
            });

            console.log("Group chat history loaded:", messageStore);
        })
        .catch(err => {
            console.error("Group chat history fetch failed:", err);
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

// Handle page events
window.onload = connect;

window.onbeforeunload = () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
        sendTypingStatus(false);
        socket.close();
    }
};

// Handle Enter key for sending messages
document.addEventListener('DOMContentLoaded', function() {
    const msgInput = document.getElementById('msgInput');
    if (msgInput) {
        msgInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });
    }
});