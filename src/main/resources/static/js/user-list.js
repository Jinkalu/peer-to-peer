/**
 * Extracts conversation details based on type
 */
function getConversationDetails(conversation) {
    if (conversation.type === "PRIVATE_CHAT") {
        return {
            displayName: conversation.peerUser.username,
            conversationType: "private",
            unreadCount: conversation.unreadCount,
            userId: conversation.peerUser.id
        };
    } else {
        return {
            displayName: conversation.groupName,
            conversationType: "group",
            unreadCount: conversation.unreadCount,
            ownerId: conversation.owner?.id
        };
    }
}

/**
 * Creates user list item HTML
 */
function createUserListItem(conversation, details) {
    const li = document.createElement("li");
    li.id = `conversation-${conversation.id}`;
    li.className = `conversation-item ${details.conversationType}`;

    // Status indicator color based on type
    const statusColor = details.conversationType === "private" ? "#4ade80" : "#60a5fa";

    li.innerHTML = `
        <div class="conversation-info">
            <div class="status-indicator" style="background: ${statusColor}"></div>
            <div class="text-content">
                <span class="name">${details.displayName}</span>
                <span class="type-badge ${details.conversationType}">
                    ${details.conversationType === "private" ? "Direct" : "Group"}
                </span>
            </div>
        </div>
        <div class="meta-info">
            ${details.conversationType === "group" ?
                `<span class="owner">Owner: ${conversation.owner?.username || 'Unknown'}</span>` :
                ''
            }
            ${details.unreadCount > 0 ?
                `<span class="unread-count" id="unread-${conversation.id}">
                    ${details.unreadCount > 99 ? '99+' : details.unreadCount}
                </span>` :
                ''
            }
        </div>
    `;

    return li;
}

/**
 * Main function to render conversations
 */
async function renderConversations() {
    try {
        const currentUserId = localStorage.getItem("userId");
        const token = localStorage.getItem("token");

        const response = await fetch(`http://localhost:8080/api/conversation/conversation-list`, {
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) throw new Error(response.status === 401 ? "SESSION_EXPIRED" : "Failed to load conversations");

        const conversations = await response.json();
        const container = document.getElementById("conversations-container");
        container.innerHTML = '';

        if (conversations.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <img src="/images/no-chats.svg" alt="No conversations">
                    <h3>No conversations yet</h3>
                    <p>Start by messaging a friend or creating a group</p>
                </div>
            `;
            return;
        }

        // Sort conversations (groups first, then by unread count)
        conversations.sort((a, b) => {
            if (a.type !== b.type) return a.type === "GROUP_CHAT" ? -1 : 1;
            return (b.unreadCount || 0) - (a.unreadCount || 0);
        });

        conversations.forEach(conversation => {
            const details = getConversationDetails(conversation);
            const li = createUserListItem(conversation, details);

            li.addEventListener('click', () => {
                handleConversationClick(conversation, details);
            });

            container.appendChild(li);

            // Initialize WebSocket for private chats
            if (conversation.type === "PRIVATE_CHAT") {
                initPresenceWebSocket(conversation, currentUserId, token);
            }
        });

        updateConversationStats(conversations);

    } catch (error) {
        handleConversationError(error);
    }
}

/**
 * Handles conversation click
 */
function handleConversationClick(conversation, details) {
    // Clear any existing conversation data
    localStorage.removeItem("targetUserId");
    localStorage.removeItem("targetUsername");
    localStorage.removeItem("groupId");
    localStorage.removeItem("groupName");

    // Store new conversation data
    localStorage.setItem("conversationId", conversation.id);

    if (conversation.type === "PRIVATE_CHAT") {
        localStorage.setItem("targetUserId", details.userId);
        localStorage.setItem("targetUsername", details.displayName);
        window.location.href = "/private-chat";
    } else {
        localStorage.setItem("groupId", conversation.id);
        localStorage.setItem("groupName", details.displayName);
        window.location.href = "/group-chat";
    }
}

/**
 * Initializes presence WebSocket for private chats
 */
function initPresenceWebSocket(conversation, currentUserId, token) {
    const wsUrl = `ws://localhost:8080/presence?token=${token}&type=subscribe&target=${conversation.peerUser.id}&convoId=${conversation.id}`;
    const socket = new WebSocket(wsUrl);

    socket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        const li = document.getElementById(`conversation-${data.conversationId}`);

        if (li) {
            // Update online status
            const indicator = li.querySelector('.status-indicator');
            if (indicator) {
                indicator.style.backgroundColor = data.online ? '#4ade80' : '#9ca3af';
                indicator.title = data.online ? 'Online' : 'Offline';
            }

            // Update unread count if provided
            if (data.unreadCount !== undefined) {
                updateUnreadCount(data.conversationId, data.unreadCount);
            }
        }
    };

    socket.onclose = () => console.log(`Presence closed for conversation ${conversation.id}`);
}

/**
 * Updates unread count for a conversation
 */
function updateUnreadCount(conversationId, count) {
    const badge = document.getElementById(`unread-${conversationId}`);
    const li = document.getElementById(`conversation-${conversationId}`);

    if (count > 0) {
        if (!badge) {
            const metaDiv = li.querySelector('.meta-info');
            if (metaDiv) {
                metaDiv.innerHTML += `
                    <span class="unread-count" id="unread-${conversationId}">
                        ${count > 99 ? '99+' : count}
                    </span>
                `;
            }
        } else {
            badge.textContent = count > 99 ? '99+' : count;
        }
        li.classList.add('has-unread');
    } else {
        if (badge) badge.remove();
        li.classList.remove('has-unread');
    }
}

/**
 * Updates conversation statistics
 */
function updateConversationStats(conversations) {
    const stats = {
        total: conversations.length,
        unread: conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0),
        groups: conversations.filter(c => c.type === "GROUP_CHAT").length
    };

    document.getElementById("total-conversations").textContent = stats.total;
    document.getElementById("unread-messages").textContent = stats.unread;
    document.getElementById("group-chats").textContent = stats.groups;
}

/**
 * Error handling
 */
function handleConversationError(error) {
    const container = document.getElementById("conversations-container");

    if (error.message === "SESSION_EXPIRED") {
        showNotification("Session expired. Please log in again.", "error");
        setTimeout(() => {
            localStorage.clear();
            window.location.href = "/login";
        }, 2000);
    } else {
        container.innerHTML = `
            <div class="error-state">
                <img src="/images/error.svg" alt="Error">
                <h3>Failed to load conversations</h3>
                <p>${error.message || "Please try again later"}</p>
                <button onclick="renderConversations()">Retry</button>
            </div>
        `;
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', renderConversations);

// Export functions for testing
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        getConversationDetails,
        createUserListItem,
        updateUnreadCount
    };
}