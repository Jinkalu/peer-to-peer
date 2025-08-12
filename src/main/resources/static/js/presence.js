(function () {
    const currentUser = localStorage.getItem("username");
    const currentUserId = localStorage.getItem("userId");
    const token = localStorage.getItem("token");
    if (!currentUser) return;

    let socket;
    function connectPingSocket() {
        socket = new WebSocket(`ws://localhost:8080/presence?token=${token}&type=ping`);

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
