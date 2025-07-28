let socket;
let presenceSocket; // New: WebSocket for presence tracking
let typingTimeout;
let lastTypingState = false;
let conversationId = null;
let isReceiverOnline = false; // New: Track receiver's online status
const messageStore = {};

function connect() {
    const sender = localStorage.getItem("username");
    const senderId = localStorage.getItem("userId");
    const receiver = localStorage.getItem("target");
    const receiverId = localStorage.getItem("targetUserId");
    const existingConversationId = localStorage.getItem("conversationId");

    if (!sender || !receiver) {
        alert("Missing sender or receiver. Please go back to the home page.");
        return;
    }

    document.getElementById("chatInfo").innerText = `You are ${sender}. Chatting with ${receiver}`;

    // Connect to presence tracking for the receiver
    connectPresenceSocket(receiverId, senderId);

    let url;
    if (existingConversationId && existingConversationId.trim() !== "") {
        conversationId = existingConversationId;
        url = `ws://localhost:8080/chat?sender=${senderId}&type=private&conversationId=${existingConversationId}`;
        console.log("✅ Using existing conversation:", existingConversationId);
    } else {
        url = `ws://localhost:8080/chat?sender=${senderId}&receiver=${receiverId}&type=private`;
        console.log("✅ Creating new conversation with:", receiver);
    }

    socket = new WebSocket(url);

    socket.onopen = () => {
        console.log("✅ Connected to private chat");
        document.getElementById("msgInput").disabled = false;

        if (conversationId) {
            loadHistory();
        }
    };

    socket.onmessage = (event) => {
        const data = parseJson(event.data);
        if (!data) return;
        console.log({data});

        if (typeof data === 'string' && data.startsWith("conversationId:")) {
            conversationId = data.split(":")[1];
            localStorage.setItem("conversationId", conversationId);
            console.log("✅ New conversation ID received:", conversationId);
            loadHistory();
            return;
        }

        if (data.type === "typing") {
            const typingStatus = document.getElementById("typingStatus");
            const receiverUsername = localStorage.getItem("target");
            const receiverId = localStorage.getItem("targetUserId");
            if (data.from === receiverId) {
                typingStatus.textContent = data.isTyping === "true" ? `${receiverUsername} is typing...` : "";
            }
            return;
        }

        if (data.type === "status") {
            console.log(`msgId : ${data.msgId}  status ${data.status}`);
            updateMessageStatus(data.msgId, data.status);
            return;
        }

        if(data.type === 'reload'){
            console.log("reload.....!!!!");
            messageStore[data.receiver +"_"+localStorage.getItem("conversationId")] = [];
            loadHistory();
        }

        if (data.conversationId && data.sender && data.msg) {
            console.log("incoming msg : : ", data);
            const from = data.sender;
            const msg = data.msg;
            const msgId = data.msgId;

            if (!messageStore[receiverId +"_"+ localStorage.getItem("conversationId")]) {
                messageStore[receiverId +"_" + localStorage.getItem("conversationId")] = [];
            }

            messageStore[receiverId + "_" + localStorage.getItem("conversationId")].push({
                from,
                msg,
                msgId,
                status: 'DELIVERED'
            });

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

// New: Connect to presence tracking for the receiver
function connectPresenceSocket(targetUserId, currentUserId) {
    presenceSocket = new WebSocket(
        `ws://localhost:8080/presence?type=subscribe&target=${targetUserId}&user=${currentUserId}`
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
        setTimeout(() => connectPresenceSocket(targetUserId, currentUserId), 3000);
    };

    presenceSocket.onerror = (error) => {
        console.error("Presence socket error:", error);
    };
}

// New: Update presence indicator in UI
function updatePresenceIndicator(isOnline) {
    const receiverName = localStorage.getItem("target");
    const chatInfo = document.getElementById("chatInfo");
    const onlineStatus = isOnline ? "🟢 Online" : "🔴 Offline";

    chatInfo.innerHTML = `
        You are ${localStorage.getItem("username")}.
        Chatting with ${receiverName}
        <span style="margin-left: 10px; font-size: 0.9em;">${onlineStatus}</span>
    `;
}

// New: Mark pending messages as delivered when user comes online
function markPendingMessagesAsDelivered() {
    const receiverId = localStorage.getItem("targetUserId");
    const chat = messageStore[receiverId  +"_"+localStorage.getItem("conversationId")] || [];
    const currentUserId = localStorage.getItem("userId");

    let hasUpdates = false;

    chat.forEach(message => {
        // Mark own messages that are only "SEND" as "DELIVERED"
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

    if (!receiver || !msg || socket.readyState !== WebSocket.OPEN) return;

    const payload = {
        receiver: receiver,
        msg: msg
    };

    socket.send(JSON.stringify(payload));

    if (!messageStore[receiver +"_"+localStorage.getItem("conversationId")]) messageStore[receiver +"_"+localStorage.getItem("conversationId")] = [];

    // New: Set initial status based on receiver's online status
    const initialStatus = isReceiverOnline ? 'DELIVERED' : 'SEND';

    const localMessage = {
        from: sender,
        msg,
        status: initialStatus,
        msgId: null
    };
    messageStore[receiver +"_"+localStorage.getItem("conversationId")].push(localMessage);

    log(`You: ${msg}`, "you", initialStatus);
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
            to: receiverId,
            isTyping: isTyping.toString()
        }));
    }
}

function updateMessageStatus(msgId, newStatus) {
    console.log({msgId, newStatus});
    const currentTarget = localStorage.getItem("targetUserId");
    const chat = messageStore[currentTarget +"_"+localStorage.getItem("conversationId")] || [];
    console.log({messageStore});

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
    const chat = messageStore[currentTarget +"_"+localStorage.getItem("conversationId")] || [];
    const currentUser = localStorage.getItem("userId");

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
        case 'SEND': return '<span style="color:grey;">✓</span>';
        case 'DELIVERED': return '<span style="color:grey;">✓✓</span>';
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
    const receiverId = localStorage.getItem("targetUserId");
    const senderId = localStorage.getItem("userId");

    // Clear existing chat history for the specific conversation
    messageStore[receiverId + "_" + cid] = [];

    // Ensure messageStore entry exists
    if (!messageStore[receiverId + "_" + cid]) {
        messageStore[receiverId + "_" + cid] = [];
    }

    // Fetch the new chat history
    fetch(url)
        .then(res => {
            if (!res.ok) {
                console.error(`HTTP error! status: ${res.status}`);
                return;
            }
            return res.json();
        })
        .then(data => {
            if (!data || data.length === 0) {
                console.log("No chat history available");
                return;
            }
            document.getElementById("messages").innerHTML = '';
            // Populate messageStore with new chat history
            data.forEach(msg => {
                const isYou = msg.senderUUID === senderId;
                messageStore[receiverId + "_" + cid].push({
                    from: msg.senderUUID,
                    msg: msg.message,
                    status: msg.status,
                    msgId: msg.id
                });

                // Log the message to the UI
                log(`${isYou ? "You" : "Received"}: ${msg.message}`, isYou ? "you" : "other", msg.status);
            });

            console.log("history:", messageStore);
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

    // New: Close presence socket
    if (presenceSocket && presenceSocket.readyState === WebSocket.OPEN) {
        presenceSocket.close();
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