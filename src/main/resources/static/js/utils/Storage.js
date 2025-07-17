export class Storage {
    static get(key) {
        return localStorage.getItem(key);
    }

    static set(key, value) {
        localStorage.setItem(key, value);
    }

    static remove(key) {
        localStorage.removeItem(key);
    }

    static getUserData() {
        return {
            username: this.get('username'),
            userId: this.get('userId'),
            target: this.get('target'),
            targetUserId: this.get('targetUserId'),
            conversationId: this.get('conversationId')
        };
    }
}