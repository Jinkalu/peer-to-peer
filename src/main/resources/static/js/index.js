 function proceed() {
        const username = document.getElementById("username").value.trim();
        if (!username) {
            alert("Username is required");
            return;
        }


 /*           try {
              const res = await fetch("http://localhost:8080/api/login", {
                method: "POST",
                headers: {
                  "Content-Type": "application/json"
                },
                body: JSON.stringify({
                  username: "john_doe"
                })
              });
              if (!res.ok) {
                throw new Error("Failed to fetch user list");
              }

              const data = await res.json();
              console.log(data);
   } catch (error) {
      console.error("Error loading user list:", error);
      alert("Unable to load user list.");
    }*/


        localStorage.setItem("user", username);
        window.location.href = "user-list";
    }
