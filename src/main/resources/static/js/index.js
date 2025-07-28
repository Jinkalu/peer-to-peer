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

function proceed() {
    const username = document.getElementById("username").value.trim();
    const btn = document.querySelector('.continue-btn');
    const btnText = document.querySelector('.btn-text');

    if (!username) {
        const input = document.querySelector('.input-group input');
        input.style.animation = 'shake 0.5s ease-in-out';
        setTimeout(() => input.style.animation = '', 500);
        showNotification("Username is required", "error");
        return;
    }

    btn.classList.add('loading');
    btnText.style.opacity = '0';

    fetch(`http://localhost:8080/api/v1/user/by-username?username=${username}`)
        .then(res => {
            if (!res.ok) throw new Error("User not found");
            return res.json();
        })
        .then(user => {
            window.userData = {
                userId: user.id,
                username: user.username
            };
localStorage.setItem("username",user.username);
localStorage.setItem("userId",user.id);
            showNotification("Login successful! Redirecting...", "success");
            setTimeout(() => window.location.href = "user-list", 1000);
        })
        .catch(err => {
            btn.classList.remove('loading');
            btnText.style.opacity = '1';
            showNotification("Login failed: " + err.message, "error");
        });
}

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

    setTimeout(() => notification.style.transform = 'translateX(0)', 100);
    setTimeout(() => {
        notification.style.transform = 'translateX(400px)';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

document.getElementById('username').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') proceed();
});

window.addEventListener('load', createParticles);
