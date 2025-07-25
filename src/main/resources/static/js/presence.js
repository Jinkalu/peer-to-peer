(function () {
    const currentUser = localStorage.getItem("username");
    const currentUserId = localStorage.getItem("userId");
    if (!currentUser) return;

    let socket;
    function connectPingSocket() {
        socket = new WebSocket(`ws://localhost:8080/presence?type=ping&user=${currentUserId}`);

        socket.onopen = () => {
            console.log("Ping WebSocket connected");
        };

        socket.onclose = () => {
            console.warn("Ping WebSocket closed, retrying...");
            setTimeout(connectPingSocket, 3000); // Reconnect logic
        };

        socket.onerror = (err) => {
            console.error("Ping socket error:", err);
            socket.close(); // Trigger reconnect
        };
    }

    connectPingSocket();
})();
