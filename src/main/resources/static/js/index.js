function proceed() {
    const username = document.getElementById("username").value.trim();
    if (!username) {
        alert("Username is required");
        return;
    }

    fetch(`http://localhost:8080/api/v1/user/by-username?username=${encodeURIComponent(username)}`)
        .then(res => {
            if (!res.ok) throw new Error("User not found");
            return res.json();
        })
        .then(user => {
            localStorage.setItem("userId", user.id);
            localStorage.setItem("username", user.username);
            window.location.href = "user-list";
        })
        .catch(err => alert("Login failed: " + err.message));
}

