import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

function createAuthStore() {
  const initial = browser
    ? {
        accessToken: localStorage.getItem('accessToken'),
        user: JSON.parse(localStorage.getItem('user') || 'null')
      }
    : { accessToken: null, user: null };

  const { subscribe, set } = writable(initial);

  return {
    subscribe,
    login(authResponse) {
      if (browser) {
        localStorage.setItem('accessToken', authResponse.accessToken);
        localStorage.setItem('refreshToken', authResponse.refreshToken);
        localStorage.setItem('user', JSON.stringify(authResponse.user));
      }
      set({ accessToken: authResponse.accessToken, user: authResponse.user });
    },
    logout() {
      if (browser) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
      }
      set({ accessToken: null, user: null });
    }
  };
}

export const authStore = createAuthStore();
export const isLoggedIn = derived(authStore, ($auth) => !!$auth.accessToken);
