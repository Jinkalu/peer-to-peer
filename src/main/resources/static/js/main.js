import { PrivateChatApp } from './core/PrivateChatApp.js';



// Initialize the application when the page loads
window.addEventListener('load', () => {
    const app = new PrivateChatApp();
    app.init();
});

// Also handle DOMContentLoaded for compatibility
document.addEventListener('DOMContentLoaded', () => {
    if (document.readyState === 'loading') {
        return; // Let the 'load' event handle initialization
    }

    const app = new PrivateChatApp();
    app.init();
});