// ======= UPDATED private-chat.js (client-side) =======
let socket;
let presenceSocket;
let typingTimeout;
let lastTypingState = false;
const messageStore = {};
let isTargetPresent = false;

function getChatId(user1, user2) {
    return user1 < user2 ? `${user1}_${user2}` : `${user2}_${user1}`;
}

function connect() {
    const user = localStorage.getItem("user");
    const currentTarget = localStorage.getItem("target");
    if (!user || !currentTarget) {
        alert("Missing user or target. Please go back to the home page.");
        return;
    }

 // ======= UPDATED private-chat.js (client-side) =======
let socket;
let presenceSocket;
let typingTimeout;
let lastTypingState = false;
const messageStore = {};
let isTargetPresent = false;

function getChatId(user1, user2) {
    return user1 < user2 ? `${user1}_${user2}` : `${user2}_${user1}`;
}

function connect() {
    const user = localStorage.getItem("user");
    const currentTarget = localStorage.getItem("target");
    if (!user || !currentTarget) {
        alert("Missing user or target. Please go back to the home page.");
        return;
    }

    document.getElementById("chatInfo").innerText = `You are ${user}. Chatting with ${currentTarget}`;
    const url = `ws://localhost:8080/chat?user=${user}&type=private`;
    socket = new WebSocket(url);

    socket.onopen = () => {
        log("Connected to private chat.");
        document.getElementById("msgInput").disabled = false;

        markMessagesAsSeen(currentTarget, user);
        const chatId = getChatId(user, currentTarget);
        socket.send(JSON.stringify({ type: "joinChatScreen", chatId }));
        connectOnScreenPresence();
    };

    socket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.type === "typing") {
                if (data.from === currentTarget) {
                    document.getElementById("typingStatus").textContent = data.isTyping ? `${data.from} is typing...` : "";
                }
                return;
            }

            if (data.type === "status") {
                updateMessageStatus(data.msg, data.status);
                return;
            }

            const sender = data.from;
            const msg = data.msg;
            if (!messageStore[sender]) messageStore[sender] = [];
            messageStore[sender].push({ from: sender, msg });

            if (sender === currentTarget) {
                document.getElementById("typingStatus").textContent = "";
                if (isTargetPresent) {
                    markMessagesAsSeen(sender, user);
                    updateMessageStatus(msg, "SEEN");
                }
                log(`Received: ${msg}`, "other");
            }
        } catch (e) {
            console.warn("Non-JSON message received:", event.data);
        }
    };

    socket.onclose = () => log("Connection closed.");

    fetch(`http://localhost:8080/api/chat-history?sender=${user}&receiver=${currentTarget}`)
        .then(res => res.json())
        .then(data => {
            if (!messageStore[currentTarget]) messageStore[currentTarget] = [];
            data.forEach(msg => {
                const isYou = msg.senderUUID === user;
                messageStore[currentTarget].push({ from: msg.senderUUID, msg: msg.message });
                log(`${isYou ? "You" : "Received"}: ${msg.message}`, isYou ? "you" : "other", msg.status);
            });
        })
        .catch(err => console.error("Failed to load chat history:", err));

    document.getElementById("msgInput").addEventListener("input", () => {
        sendTypingStatus(true);
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => sendTypingStatus(false), 1500);
    });
}

function connectOnScreenPresence() {
    const user = localStorage.getItem("user");
    const target = localStorage.getItem("target");
    const chatId = getChatId(user, target);

    presenceSocket = new WebSocket(`ws://localhost:8080/onScreenPresence?user=${user}&chatId=${chatId}`);

    presenceSocket.onopen = () => {
        console.log(`[Presence] ${user} watching chat with ${target}`);
    };

    presenceSocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === "onScreenStatus" && data.from === target) {
            isTargetPresent = data.present;
            console.log(`[Presence] ${target} is on-screen: ${isTargetPresent}`);
        }
    };

    presenceSocket.onclose = () => console.log("[Presence] connection closed");
}

function sendMessage() {
    const msgBox = document.getElementById("msgInput");
    const msg = msgBox.value.trim();
    const to = localStorage.getItem("target");
    const from = localStorage.getItem("user");
    if (!to || !msg) return;

    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ to, msg }));
    }

    if (!messageStore[to]) messageStore[to] = [];
    messageStore[to].push({ from, msg });
    log(`You: ${msg}`, "you", "SEND");
    msgBox.value = "";
    sendTypingStatus(false);
}

function log(message, type = "info", status = null) {
    const messages = document.getElementById("messages");
    const div = document.createElement("div");
    div.className = `message ${type}`;
    div.innerHTML = type === "you" && status ? `${message} <span style="float:right">${getStatusIcon(status)}</span>` : message;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
}

function sendTypingStatus(isTyping) {
    const to = localStorage.getItem("target");
    if (lastTypingState === isTyping) return;
    lastTypingState = isTyping;
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ to, type: "typing", isTyping }));
    }
}

function getStatusIcon(status) {
    switch (status) {
        case 'SEND': return '✓';
        case 'DELIVERED': return '✓✓';
        case 'SEEN': return '<span style="color:blue;">✓✓</span>';
        default: return '';
    }
}

function updateMessageStatus(messageText, newStatus) {
    const currentTarget = localStorage.getItem("target");
    const chat = messageStore[currentTarget] || [];
    for (let m of chat) {
        if (m.msg === messageText) m.status = newStatus;
    }
    reloadMessages();
}

function reloadMessages() {
    const messages = document.getElementById("messages");
    messages.innerHTML = "";
    const currentTarget = localStorage.getItem("target");
    const chat = messageStore[currentTarget] || [];
    const currentUser = localStorage.getItem("user");
    chat.forEach(m => log(`${m.from === currentUser ? "You" : "Received"}: ${m.msg}`, m.from === currentUser ? "you" : "other", m.status));
}

function markMessagesAsSeen(sender, receiver) {
    fetch(`http://localhost:8080/api/mark-seen?sender=${sender}&receiver=${receiver}`, { method: 'POST' })
        .then(res => res.ok ? console.log("✅ Marked messages as seen") : console.error("❌ Mark seen failed"))
        .catch(err => console.error("❌ Error:", err));
}

window.onload = connect;

window.onbeforeunload = () => {
    const user = localStorage.getItem("user");
    const currentTarget = localStorage.getItem("target");
    const chatId = getChatId(user, currentTarget);
    if (socket?.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "leaveChatScreen", chatId }));
    }
    if (presenceSocket?.readyState === WebSocket.OPEN) {
        presenceSocket.send(JSON.stringify({ type: "leaveChatScreen", chatId, from: user }));
        presenceSocket.close();
    }
};


    document.getElementById("chatInfo").innerText = `You are ${user}. Chatting with ${currentTarget}`;
    const url = `ws://localhost:8080/chat?user=${user}&type=private`;
    socket = new WebSocket(url);

    socket.onopen = () => {
        log("Connected to private chat.");
        document.getElementById("msgInput").disabled = false;

        markMessagesAsSeen(currentTarget, user);
        const chatId = getChatId(user, currentTarget);
        socket.send(JSON.stringify({ type: "joinChatScreen", chatId }));
        connectOnScreenPresence();
    };

    socket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);
            if (data.type === "typing") {
                if (data.from === currentTarget) {
                    document.getElementById("typingStatus").textContent = data.isTyping ? `${data.from} is typing...` : "";
                }
                return;
            }

            if (data.type === "status") {
                updateMessageStatus(data.msg, data.status);
                return;
            }

            const sender = data.from;
            const msg = data.msg;
            if (!messageStore[sender]) messageStore[sender] = [];
            messageStore[sender].push({ from: sender, msg });

            if (sender === currentTarget) {
                document.getElementById("typingStatus").textContent = "";
                if (isTargetPresent) {
                    markMessagesAsSeen(sender, user);
                    updateMessageStatus(msg, "SEEN");
                }
                log(`Received: ${msg}`, "other");
            }
        } catch (e) {
            console.warn("Non-JSON message received:", event.data);
        }
    };

    socket.onclose = () => log("Connection closed.");

    fetch(`http://localhost:8080/api/chat-history?sender=${user}&receiver=${currentTarget}`)
        .then(res => res.json())
        .then(data => {
            if (!messageStore[currentTarget]) messageStore[currentTarget] = [];
            data.forEach(msg => {
                const isYou = msg.senderUUID === user;
                messageStore[currentTarget].push({ from: msg.senderUUID, msg: msg.message });
                log(`${isYou ? "You" : "Received"}: ${msg.message}`, isYou ? "you" : "other", msg.status);
            });
        })
        .catch(err => console.error("Failed to load chat history:", err));

    document.getElementById("msgInput").addEventListener("input", () => {
        sendTypingStatus(true);
        clearTimeout(typingTimeout);
        typingTimeout = setTimeout(() => sendTypingStatus(false), 1500);
    });
}

function connectOnScreenPresence() {
    const user = localStorage.getItem("user");
    const target = localStorage.getItem("target");
    const chatId = getChatId(user, target);

    presenceSocket = new WebSocket(`ws://localhost:8080/onScreenPresence?user=${user}&chatId=${chatId}`);

    presenceSocket.onopen = () => {
        console.log(`[Presence] ${user} watching chat with ${target}`);
    };

    presenceSocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === "onScreenStatus" && data.from === target) {
            isTargetPresent = data.present;
            console.log(`[Presence] ${target} is on-screen: ${isTargetPresent}`);
        }
    };

    presenceSocket.onclose = () => console.log("[Presence] connection closed");
}

function sendMessage() {
    const msgBox = document.getElementById("msgInput");
    const msg = msgBox.value.trim();
    const to = localStorage.getItem("target");
    const from = localStorage.getItem("user");
    if (!to || !msg) return;

    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ to, msg }));
    }

    if (!messageStore[to]) messageStore[to] = [];
    messageStore[to].push({ from, msg });
    log(`You: ${msg}`, "you", "SEND");
    msgBox.value = "";
    sendTypingStatus(false);
}

function log(message, type = "info", status = null) {
    const messages = document.getElementById("messages");
    const div = document.createElement("div");
    div.className = `message ${type}`;
    div.innerHTML = type === "you" && status ? `${message} <span style="float:right">${getStatusIcon(status)}</span>` : message;
    messages.appendChild(div);
    messages.scrollTop = messages.scrollHeight;
}

function sendTypingStatus(isTyping) {
    const to = localStorage.getItem("target");
    if (lastTypingState === isTyping) return;
    lastTypingState = isTyping;
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ to, type: "typing", isTyping }));
    }
}

function getStatusIcon(status) {
    switch (status) {
        case 'SEND': return '✓';
        case 'DELIVERED': return '✓✓';
        case 'SEEN': return '<span style="color:blue;">✓✓</span>';
        default: return '';
    }
}

function updateMessageStatus(messageText, newStatus) {
    const currentTarget = localStorage.getItem("target");
    const chat = messageStore[currentTarget] || [];
    for (let m of chat) {
        if (m.msg === messageText) m.status = newStatus;
    }
    reloadMessages();
}

function reloadMessages() {
    const messages = document.getElementById("messages");
    messages.innerHTML = "";
    const currentTarget = localStorage.getItem("target");
    const chat = messageStore[currentTarget] || [];
    const currentUser = localStorage.getItem("user");
    chat.forEach(m => log(`${m.from === currentUser ? "You" : "Received"}: ${m.msg}`, m.from === currentUser ? "you" : "other", m.status));
}

function markMessagesAsSeen(sender, receiver) {
    fetch(`http://localhost:8080/api/mark-seen?sender=${sender}&receiver=${receiver}`, { method: 'POST' })
        .then(res => res.ok ? console.log("✅ Marked messages as seen") : console.error("❌ Mark seen failed"))
        .catch(err => console.error("❌ Error:", err));
}

window.onload = connect;

window.onbeforeunload = () => {
    const user = localStorage.getItem("user");
    const currentTarget = localStorage.getItem("target");
    const chatId = getChatId(user, currentTarget);
    if (socket?.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ type: "leaveChatScreen", chatId }));
    }
    if (presenceSocket?.readyState === WebSocket.OPEN) {
        presenceSocket.send(JSON.stringify({ type: "leaveChatScreen", chatId, from: user }));
        presenceSocket.close();
    }
};
