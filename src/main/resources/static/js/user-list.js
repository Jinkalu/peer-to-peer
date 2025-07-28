(async function () {
    // Get user data from localStorage
    const currentUser = localStorage.getItem("username");
    const currentUserId = localStorage.getItem("userId");
    console.log("Inside user list....!");
    if (!currentUser || !currentUserId) {
        showNotification("No user found. Please log in.", "error");
        setTimeout(() => {
            window.location.href = "/";
        }, 2000);
        return;
    }

    try {
        const res = await fetch(`http://localhost:8080/api/conversation/${currentUserId}`);
        if (!res.ok) {
            throw new Error("Failed to fetch user list");
        }

        const userList = await res.json();
        const statusMap = {};
        const userListElement = document.getElementById("userList");

        // Clear loading spinner
        userListElement.innerHTML = '';

        if (userList.length === 0) {
            userListElement.innerHTML = `
                <div class="empty-state">
                    <h3>No conversations yet</h3>
                    <p>Start a new conversation to see users here</p>
                </div>
            `;
            return;
        }

        let onlineCount = 0;
        let totalUnreadCount = 0;

        userList.forEach((conversation, index) => {
            console.log(`conversation ${conversation.id} : : currentUserId ${currentUserId}`);

            const li = document.createElement("li");
            li.id = `user-${conversation.id}`;
            li.className = "user-item offline";

            // Determine display name and type
            let displayName, conversationType , unreadCount;
            if (conversation.type === "PRIVATE_CHAT") {
                displayName = conversation.peerUser.username;
                conversationType = "private";
                unreadCount = conversation.unreadCount;
            } else {
                displayName = conversation.groupName;
                conversationType = "group";
                unreadCount = conversation.unreadCount;
            }

            li.innerHTML = `
                <div class="user-info">
                    <div class="status-indicator"></div>
                    <span class="username">${displayName}</span>
                </div>
                <div class="user-meta">
                    <span class="conversation-type ${conversationType}">${conversationType}</span>
                    <span class="unread-badge hidden" id="unread-${conversation.id}">${unreadCount}</span>
                </div>
            `;

            li.onclick = () => {
                // Store conversation ID for messaging
                localStorage.setItem("conversationId", conversation.id);

                // Store additional info based on conversation type
                if (conversation.type === "PRIVATE_CHAT") {
                    localStorage.setItem("targetUserId", conversation.peerUser.id);
                    localStorage.setItem("targetUsername", conversation.peerUser.username);
                } else {
                    localStorage.setItem("groupId", conversation.id);
                    localStorage.setItem("groupName", conversation.groupName);
                }

                // Clear unread count when clicking on conversation
                const badge = document.getElementById(`unread-${conversation.id}`);
                if (badge && !badge.classList.contains('hidden')) {
                    const currentUnread = parseInt(badge.textContent) || 0;
                    totalUnreadCount -= currentUnread;
                    updateStats();

                    badge.textContent = "0";
                    badge.classList.add("hidden");
                }

                // Add click animation
                li.style.transform = 'scale(0.95)';
                setTimeout(() => {
                    li.style.transform = '';

                    // Redirect based on conversation type
                    if (conversation.type === "PRIVATE_CHAT") {
                        window.location.href = "/private";
                    } else if (conversation.type === "GROUP_CHAT") {
                        window.location.href = "/group";
                    }
                }, 150);
            };

            userListElement.appendChild(li);

            // Only subscribe to presence for private chats
            if (conversation.type === "PRIVATE_CHAT") {
                const subSocket = new WebSocket(
                    `ws://localhost:8080/presence?type=subscribe&target=${conversation.peerUser.id}&user=${currentUserId}&convoId=${conversation.id}`
                );

                statusMap[conversation.id] = subSocket;

                subSocket.onmessage = (event) => {
                    const data = JSON.parse(event.data);
                    console.log(data);
                    const liElement = document.getElementById(`user-${data.conversationId}`);
                    const unreadBadge = document.getElementById(`unread-${data.conversationId}`);

                    if (liElement) {
                        // Update online status
                        const wasOnline = liElement.classList.contains("online");
                        const isOnline = data.online;

                        liElement.className = `user-item ${isOnline ? "online" : "offline"}`;

                        // Update online count
                        if (wasOnline !== isOnline) {
                            onlineCount += isOnline ? 1 : -1;
                            updateStats();
                        }

                        // Update unread message count if present in response
                        if (data.unreadCount !== undefined && unreadBadge) {
                            const previousUnread = unreadBadge.classList.contains('hidden') ? 0 : parseInt(unreadBadge.textContent) || 0;
                            const newUnread = data.unreadCount;

                            totalUnreadCount = totalUnreadCount - previousUnread + newUnread;

                            if (newUnread > 0) {
                                unreadBadge.textContent = newUnread > 99 ? "99+" : newUnread;
                    
                                unreadBadge.classList.remove("hidden");
                            } else {
                                unreadBadge.classList.add("hidden");
                            }
document.getElementById("unreadCount").textContent = totalUnreadCount;
                            updateStats();
                        }
                    }
                };

                subSocket.onerror = (error) => {
                    console.error(`WebSocket error for conversation ${conversation.id}:`, error);
                };

                subSocket.onclose = (event) => {
                    console.log(`WebSocket closed for conversation ${conversation.id}:`, event.code, event.reason);
                };
            } else {
                // For group chats, mark as online by default
                li.classList.add("online");
                onlineCount++;
            }
        });

        // Initial stats update
        updateStats();

        // Cleanup function to close all WebSocket connections
        window.addEventListener('beforeunload', () => {
            Object.values(statusMap).forEach(socket => {
                if (socket.readyState === WebSocket.OPEN) {
                    socket.close();
                }
            });
        });

    } catch (error) {
        console.error("Error loading user list:", error);
        showNotification("Unable to load user list.", "error");

        document.getElementById("userList").innerHTML = `
            <div class="empty-state">
                <h3>Error loading conversations</h3>
                <p>Please try refreshing the page</p>
            </div>
        `;
    }

    function updateStats() {
        document.getElementById("onlineCount").textContent = onlineCount;
        document.getElementById("totalCount").textContent = document.querySelectorAll('.user-item').length;
        // document.getElementById("unreadCount").textContent = totalUnreadCount;
    }
})();

// Enhanced notification system
function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
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