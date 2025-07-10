let socket;
let typingTimeout;
let lastTypingState = false;
let conversationId = null;
const messageStore = {};

 function connect() {
    const sender = localStorage.getItem("username"); // Updated to match user list
    const senderId = localStorage.getItem("userId")
    const receiver = localStorage.getItem("target");
    const receiverId = localStorage.getItem("targetUserId")
    const existingConversationId = localStorage.getItem("conversationId");

    if (!sender || !receiver) {
        alert("Missing sender or receiver. Please go back to the home page.");
        return;
    }

    document.getElementById("chatInfo").innerText = `You are ${sender}. Chatting with ${receiver}`;

    let url;

    if (existingConversationId && existingConversationId.trim() !== "") {
        // Use existing conversation
        conversationId = existingConversationId;
        url = `ws://localhost:8080/chat?sender=${senderId}&type=private&conversationId=${existingConversationId}`;
        console.log("✅ Using existing conversation:", existingConversationId);
    } else {
        // Create new conversation
        url = `ws://localhost:8080/chat?sender=${senderId}&receiver=${receiverId}&type=private`;
        console.log("✅ Creating new conversation with:", receiver);
    }

    socket = new WebSocket(url);

    socket.onopen = () => {
        console.log("✅ Connected to private chat");
        document.getElementById("msgInput").disabled = false;

        // Load history immediately if we have an existing conversation
        if (conversationId) {
            loadHistory();
        }
    };

    socket.onmessage = (event) => {
        const data = parseJson(event.data);
        if (!data) return;
        console.log({data});

        // Handle conversation ID assignment (only for new conversations)
        if (typeof data === 'string' && data.startsWith("conversationId:")) {
            conversationId = data.split(":")[1];
            localStorage.setItem("conversationId", conversationId);
            console.log("✅ New conversation ID received:", conversationId);
            // Load history after getting conversation ID (though new convo won't have history)
            loadHistory();
            return;
        }

        // Handle typing status
        if (data.type === "typing") {
            const typingStatus = document.getElementById("typingStatus");
            const receiverUsername = localStorage.getItem("target");
             const receiverId = localStorage.getItem("targetUserId");
            if (data.from === receiverId) {
                typingStatus.textContent = data.isTyping === "true" ? `${receiverUsername} is typing...` : "";
            }
            return;
        }

        // Handle message delivery status
        if (data.type === "status") {
            console.log(`msgId : ${data.msgId}  status ${data.status}`);
            updateMessageStatus(data.msgId, data.status);
            return;
        }

        if(data.type === 'reload'){
        console.log("reload.....!!!!")
       messageStore[data.receiver] =[];
       loadHistory();

        }

        // Handle normal message
        if (data.conversationId && data.sender && data.msg) {
             console.log("incomming msg : : ",data);
            const from = data.sender;
            const msg = data.msg;
            const msgId = data.msgId;

            if (!messageStore[receiverId]) {
                messageStore[receiverId] = [];
            }

            // Store message with ID for status tracking
            messageStore[receiverId].push({
                from,
                msg,
                msgId,
                status: 'DELIVERED'
            });

            // Clear typing indicator and show message
            document.getElementById("typingStatus").textContent = "";
            log(`Received: ${msg}`, "other");
        }
    };

    socket.onclose = () => {
        console.warn("WebSocket connection closed");
        document.getElementById("msgInput").disabled = true;
    };

    socket.onerror = (error) => {
        console.error("WebSocket error:", error);
    };

    setupTypingListener();
}

function sendMessage() {
    const msgBox = document.getElementById("msgInput");
    const msg = msgBox.value.trim();
    const receiver = localStorage.getItem("targetUserId");
    const sender = localStorage.getItem("userId"); // Updated to match user list

    if (!receiver || !msg || socket.readyState !== WebSocket.OPEN) return;

    // Send message with required fields based on backend
    const payload = {
        receiver: receiver,
        msg: msg
    };

    socket.send(JSON.stringify(payload));

    // Store message locally (will get msgId from status update)
    if (!messageStore[receiver]) messageStore[receiver] = [];
    const localMessage = {
        from: sender,
        msg,
        status: 'SEND',
        msgId: null // Will be updated when we get status
    };
    messageStore[receiver].push(localMessage);

    log(`You: ${msg}`, "you", "SEND");
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
    const receiverId = localStorage.getItem("targetUserId");

    if (lastTypingState === isTyping) return;
    lastTypingState = isTyping;

    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({
            type: "typing",
            to: receiverId, // Use receiver ID
            isTyping: isTyping.toString()
        }));
    }
}

function updateMessageStatus(msgId, newStatus) {
    console.log({msgId, newStatus});
    const currentTarget = localStorage.getItem("targetUserId");
    const chat = messageStore[currentTarget] || [];
    console.log({messageStore});

    // Find message by ID or update the last message if no ID match
    let messageFound = false;
    for (let i = chat.length - 1; i >= 0; i--) {
        if (chat[i].msgId === msgId || (!chat[i].msgId && i === chat.length - 1)) {
            chat[i].status = newStatus;
            chat[i].msgId = msgId;
            messageFound = true;
            break;
        }
    }

    if (messageFound) {
        reloadMessages();
    }
}

function reloadMessages() {
    const messages = document.getElementById("messages");
    messages.innerHTML = '';

    const currentTarget = localStorage.getItem("targetUserId");
    const chat = messageStore[currentTarget] || [];
    const currentUser = localStorage.getItem("userId"); // Updated to match user list

    chat.forEach(m => {
        const isYou = m.from === currentUser;
        log(`${isYou ? "You" : "Received"}: ${m.msg}`, isYou ? "you" : "other", m.status);
    });
}

function log(message, type = "info", status = null) {
    const messages = document.getElementById("messages");
    const div = document.createElement("div");
    div.className = `message ${type}`;

    if (type === "you" && status) {
        const icon = getStatusIcon(status);
        div.innerHTML = `${message} <span style="float:right">${icon}</span>`;
    } else {
        div.textContent = message;
    }

    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
}

function getStatusIcon(status) {
    switch (status) {
        case 'SEND': return '✓';
        case 'DELIVERED': return '✓✓';
        case 'SEEN': return '<span style="color:blue;">✓✓</span>';
        default: return '';
    }
}

function loadHistory() {
    const cid = conversationId || localStorage.getItem("conversationId");
    if (!cid || cid.trim() === "") {
        console.log("No conversation ID available for loading history");
        return;
    }

    const url = `http://localhost:8080/api/chat-history?conversationId=${cid}`;
    const receiver = localStorage.getItem("target");
    const receiverId = localStorage.getItem("targetUserId");
    const sender = localStorage.getItem("username");
    const senderId = localStorage.getItem("userId"); // Updated to match user list

    fetch(url)
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }
            return res.json();
        })
        .then(data => {
            if (!messageStore[receiverId]) messageStore[receiverId] = [];

            data.forEach(msg => {
                const isYou = msg.senderUUID === senderId;
                messageStore[receiverId].push({
                    from: msg.senderUUID,
                    msg: msg.message,
                    status: msg.status,
                    msgId: msg.id
                });
                log(`${isYou ? "You" : "Received"}: ${msg.message}`, isYou ? "you" : "other", msg.status);
            });
        })
        .catch(err => {
            console.error("Chat history fetch failed:", err);
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