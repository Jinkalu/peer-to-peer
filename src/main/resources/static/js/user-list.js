(async function () {
  const currentUser = localStorage.getItem("username"); // was "user"
  const currentUserId = localStorage.getItem("userId"); // was "currentUserId"

  if (!currentUser || !currentUserId) {
    alert("No user found in localStorage. Please log in.");
    return;
  }

  try {
    const res = await fetch(`http://localhost:8080/api/v1/user/${currentUserId}`);
    if (!res.ok) {
      throw new Error("Failed to fetch user list");
    }

    const userList = await res.json();
    const statusMap = {};
    const userListElement = document.getElementById("userList");

    userList.forEach(targetUser => {
      console.log(`targetUser ${targetUser.id} : : currentUserId ${currentUserId}`)
      if (String(targetUser.id) === String(currentUserId)) return; // Skip self

      const li = document.createElement("li");
      li.id = `user-${targetUser.id}`;
      li.className = "offline";

      // Create username span
      const usernameSpan = document.createElement("span");
      usernameSpan.textContent = targetUser.username;
      usernameSpan.className = "username";

      // Create unread count badge
      const unreadBadge = document.createElement("span");
      unreadBadge.className = "unread-badge hidden";
      unreadBadge.id = `unread-${targetUser.id}`;
      unreadBadge.textContent = "0";

      // Append elements to li
      li.appendChild(usernameSpan);
      li.appendChild(unreadBadge);

      li.onclick = () => {
        localStorage.setItem("target", targetUser.username);
        localStorage.setItem("targetUserId", targetUser.id);
        localStorage.setItem("conversationId", targetUser.conversationId || "");

        // Clear unread count when clicking on user
        const badge = document.getElementById(`unread-${targetUser.id}`);
        if (badge) {
          badge.textContent = "0";
          badge.classList.add("hidden");
        }

        window.location.href = "/private"; // or "/chat"
      };

      userListElement.appendChild(li);

      const subSocket = new WebSocket(
        `ws://localhost:8080/presence?type=subscribe&target=${targetUser.id}&user=${currentUserId}`
      );

      statusMap[targetUser.id] = subSocket;

      subSocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        const liElement = document.getElementById(`user-${data.user}`);
        const unreadBadge = document.getElementById(`unread-${data.user}`);

        if (liElement) {
          // Update online status
          liElement.className = data.online ? "online" : "offline";

          // Update unread message count if present in response
          if (data.unreadCount !== undefined && unreadBadge) {
            if (data.unreadCount > 0) {
              unreadBadge.textContent = data.unreadCount > 99 ? "99+" : data.unreadCount;
              unreadBadge.classList.remove("hidden");
            } else {
              unreadBadge.classList.add("hidden");
            }
          }
        }
      };

      subSocket.onerror = (error) => {
        console.error(`WebSocket error for user ${targetUser.id}:`, error);
      };

      subSocket.onclose = (event) => {
        console.log(`WebSocket closed for user ${targetUser.id}:`, event.code, event.reason);
      };
    });

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
    alert("Unable to load user list.");
  }
})();