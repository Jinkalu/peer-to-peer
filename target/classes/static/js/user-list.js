  (async function () {
    const currentUser = localStorage.getItem("user");

    if (!currentUser) {
      alert("No user found in localStorage. Please log in.");
      return;
    }

    try {
      const res = await fetch("http://localhost:8080/api/users");
      if (!res.ok) {
        throw new Error("Failed to fetch user list");
      }

      const userList = await res.json();
      const statusMap = {};

      const userListElement = document.getElementById("userList");

      userList.forEach(targetUser => {
        if (targetUser === currentUser) return;

        const li = document.createElement("li");
        li.textContent = targetUser;
        li.id = `user-${targetUser}`;
        li.className = "offline";

        li.onclick = () => {
          localStorage.setItem("target", targetUser);
          window.location.href = "/private";
        };

        userListElement.appendChild(li);

        const subSocket = new WebSocket(
                `ws://localhost:8080/presence?type=subscribe&target=${targetUser}&user=${currentUser}`
        );
        statusMap[targetUser] = subSocket;

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
