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
      li.textContent = targetUser.username;
      li.id = `user-${targetUser.id}`;
      li.className = "offline";

      li.onclick = () => {
        localStorage.setItem("target", targetUser.username);
        localStorage.setItem("targetUserId", targetUser.id);
        localStorage.setItem("conversationId", targetUser.conversationId || "");

        window.location.href = "private-chat.html"; // or "/chat"
      };

      userListElement.appendChild(li);

      const subSocket = new WebSocket(
        `ws://localhost:8080/presence?type=subscribe&target=${targetUser.id}&user=${currentUserId}`
      );

      statusMap[targetUser.id] = subSocket;

      subSocket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        const liElement = document.getElementById(`user-${data.user}`);
        if (liElement) {
          liElement.className = data.online ? "online" : "offline";
        }
      };
    });
  } catch (error) {
    console.error("Error loading user list:", error);
    alert("Unable to load user list.");
  }
})();