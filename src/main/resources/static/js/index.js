/**
 * Decodes a JWT token and returns its payload.
 * @param {string} token - JWT token to decode.
 * @returns {object|null} Decoded payload or null if invalid.
 */
function decodeJWT(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(atob(base64));
        return payload;
    } catch (err) {
        console.error('JWT decode error:', err);
        return null;
    }
}

/**
 * Creates floating particles animation.
 */
function createParticles() {
    const particleContainer = document.querySelector('.particles');
    for (let i = 0; i < 50; i++) {
        const particle = document.createElement('div');
        particle.className = 'particle';
        particle.style.left = Math.random() * 100 + '%';
        particle.style.animationDelay = Math.random() * 15 + 's';
        particle.style.animationDuration = (Math.random() * 10 + 10) + 's';
        particleContainer.appendChild(particle);
    }
}

/**
 * Handles login process with JWT.
 */
async function proceed() {
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const btn = document.querySelector('.continue-btn');
    const btnText = document.querySelector('.btn-text');

    // Validation
    if (!username || !password) {
        const inputs = document.querySelectorAll('.input-group input');
        inputs.forEach(input => {
            input.style.animation = 'shake 0.5s ease-in-out';
            setTimeout(() => input.style.animation = '', 500);
        });
        showNotification("Username and password are required", "error");
        return;
    }

    // UI Loading state
    btn.classList.add('loading');
    btnText.style.opacity = '0';

    try {
        // API Request
        const response = await fetch("http://localhost:8080/api/v1/user/log-in", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password }),
        });

        if (!response.ok) {
            throw new Error(response.status === 401 ? "Invalid credentials" : "Login failed");
        }

        const data = await response.json();
        const token = data.token;

        // Decode and verify JWT
        const jwtData = decodeJWT(token);
        if (!jwtData || !jwtData.sub || !jwtData.userID) {
            throw new Error("Invalid token data");
        }

        // Secure storage
        localStorage.setItem("token", token);
        localStorage.setItem("username", jwtData.sub);
        localStorage.setItem("userId", jwtData.userID);

        // Redirect
        showNotification("Login successful! Redirecting...", "success");
        setTimeout(() => window.location.href = "user-list", 1000);

    } catch (err) {
        console.error("Login error:", err);
        showNotification(err.message, "error");
    } finally {
        btn.classList.remove('loading');
        btnText.style.opacity = '1';
    }
}

/**
 * Shows a notification toast.
 * @param {string} message - Notification text.
 * @param {string} type - 'success' or 'error'.
 */
function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.className = 'notification';
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
        transition: all 0.3s ease;
        backdrop-filter: blur(10px);
        box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
        ${type === 'success' 
            ? 'background: rgba(34, 197, 94, 0.9);' 
            : 'background: rgba(239, 68, 68, 0.9);'
        }
    `;
    notification.textContent = message;
    document.body.appendChild(notification);

    // Animation
    setTimeout(() => notification.style.transform = 'translateX(0)', 100);
    setTimeout(() => {
        notification.style.transform = 'translateX(400px)';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Event Listeners
document.getElementById('username').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') proceed();
});

document.getElementById('password').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') proceed();
});

// Initialize particles on load
window.addEventListener('load', () => {
    createParticles();
    
    // Auto-focus username field
    document.getElementById('username').focus();
});