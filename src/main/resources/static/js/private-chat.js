    let socket;
    let typingTimeout;
    let lastTypingState = false;
    const messageStore = {};

function getChatId(user1, user2) {
    return user1 < user2 ? `${user1}_${user2}` : `${user2}_${user1}`;
}


    function connect() {
        const user = localStorage.getItem("username");
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
        };


        socket.onopen = () => {
            log("Connected to private chat.");
            document.getElementById("msgInput").disabled = false;


            // ✅ Mark messages from currentTarget as seen
//            markMessagesAsSeen(currentTarget, user);
        };

        socket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);

                if (data.type === "typing") {
                    const typingStatus = document.getElementById("typingStatus");
                    if (data.from === currentTarget) {
                        typingStatus.textContent = data.isTyping ? `${data.from} is typing...` : "";
                    }
                    return;
                }

                if (data.type === "status") {
                    console.log(`Status update: ${data.status}`);
                    // update messageStore and re-render messages if needed
                    updateMessageStatus(data.msg, data.status);
                    return;
                }

                const sender = data.from;
                const msg = data.msg;

                if (!messageStore[sender]) {
                    messageStore[sender] = [];
                }
                messageStore[sender].push({from: sender, msg});

                if (sender === currentTarget) {
                    document.getElementById("typingStatus").textContent = "";
                    log(`Received: ${msg}`, "other");
                }
            } catch (e) {
                console.warn("Non-JSON message received:", event.data);
            }
        };

        socket.onclose = () => log("Connection closed.");

const conversationId = localStorage.getItem("conversationId");

if (!messageStore[currentTarget]) {
    messageStore[currentTarget] = [];
}

const historyUrl = conversationId
    ? `http://localhost:8080/api/chat-history?conversationId=${conversationId}`
    : `http://localhost:8080/api/chat-history?sender=${user}&receiver=${currentTarget}`;

fetch(historyUrl)
    .then(response => response.json())
    .then(data => {
        data.forEach(msg => {
            const isYou = msg.senderUUID === user;
            messageStore[currentTarget].push({ from: msg.senderUUID, msg: msg.message });
            log(`${isYou ? "You" : "Received"}: ${msg.message}`, isYou ? "you" : "other", msg.status);
        });
    })
    .catch(err => console.error("Failed to load chat history:", err));

        // ⬇️ Typing input listener
        const inputBox = document.getElementById("msgInput");
        inputBox.addEventListener("input", () => {
            sendTypingStatus(true);
            clearTimeout(typingTimeout);
            typingTimeout = setTimeout(() => {
                sendTypingStatus(false);
            }, 1500);
        });
    }




    function sendMessage() {
        const msgBox = document.getElementById("msgInput");
        const msg = msgBox.value.trim();
        const to = localStorage.getItem("target");
        const from = localStorage.getItem("user");

        if (!to || !msg) return;

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({to, msg}));
        }

        if (!messageStore[to]) messageStore[to] = [];
        messageStore[to].push({from, msg});

        log(`You: ${msg}`, "you", "SEND");
        msgBox.value = '';

        sendTypingStatus(false); // Stop typing indicator
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



    function sendTypingStatus(isTyping) {
        const to = localStorage.getItem("target");

        console.log("isTyping : : " + isTyping)

        if (lastTypingState === isTyping) return;
        lastTypingState = isTyping;

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                to,
                type: "typing",
                isTyping
            }));
        } else {
            console.warn("WebSocket not open. Cannot send typing status.");
        }
    }

    function getStatusIcon(status) {
        switch (status) {
            case 'SEND':
                return '✓';
            case 'DELIVERED':
                return '✓✓';
            case 'SEEN':
                return '<span style="color:blue;">✓✓</span>';
            default:
                return '';
        }
    }

  /*  function updateMessageStatus(messageText, newStatus) {
        const currentTarget = localStorage.getItem("target");

        const chat = messageStore[currentTarget] || [];

        for (let msg of chat) {
            // You can match on message text for now — or use message ID if available
            if (msg.msg === messageText && msg.status !== newStatus) {
                msg.status = newStatus;
                break;
            }
        }
        reloadMessages(); // re-render chat messages
    }*/

    function updateMessageStatus(newStatus) {
        const currentTarget = localStorage.getItem("target");
        const chat = messageStore[currentTarget] || [];

        if (chat.length > 0) {
            const lastMessage = chat[chat.length - 1];
            if (lastMessage.status !== newStatus) {
                lastMessage.status = newStatus;
            }
        }

        reloadMessages(); // re-render chat messages
    }

    function reloadMessages() {
        const messages = document.getElementById("messages");
        messages.innerHTML = '';

        const currentTarget = localStorage.getItem("target");
        const chat = messageStore[currentTarget] || [];
        const currentUser = localStorage.getItem("user");

        chat.forEach(m => {
            const isYou = m.from === currentUser;
            log(`${isYou ? "You" : "Received"}: ${m.msg}`, isYou ? "you" : "other", m.status);
        });
    }




 /*   function markMessagesAsSeen(sender, receiver) {
        fetch(`http://localhost:8080/api/mark-seen?sender=${sender}&receiver=${receiver}`, {
            method: 'POST'
        })
            .then(res => {
                if (res.ok) {
                    console.log("✅ Marked messages as seen");
                } else {
                    console.error("❌ Mark seen failed. HTTP status:", res.status);
                }
            })
            .catch(err => {
                console.error("❌ Fetch error while marking seen:", err);
            });
    }*/



    window.onload = connect;


    window.onbeforeunload = () => {
        const user = localStorage.getItem("user");
        const currentTarget = localStorage.getItem("target");
        const chatId = getChatId(user, currentTarget);

       /* if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({
                type: "leaveChatScreen",
                chatId: chatId
            }));
        }*/
    };

