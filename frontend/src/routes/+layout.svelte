<script>
  import '../app.css';
  import { goto } from '$app/navigation';
  import { authStore, isLoggedIn } from '$lib/stores.js';
  import { auth } from '$lib/api.js';

  async function handleLogout() {
    try { await auth.logout(); } catch {}
    authStore.logout();
    goto('/login');
  }
</script>

{#if $isLoggedIn}
  <nav>
    <span class="brand">Task Platform</span>
    <div class="nav-links">
      <a href="/tasks">Tasks</a>
    </div>
    <div class="nav-user">
      <span class="username">{$authStore.user?.username ?? ''}</span>
      <button class="btn-logout" on:click={handleLogout}>Logout</button>
    </div>
  </nav>
{/if}

<main class:no-nav={!$isLoggedIn}>
  <slot />
</main>

<style>
  nav {
    display: flex;
    align-items: center;
    padding: 0 24px;
    height: 56px;
    background: white;
    border-bottom: 1px solid var(--color-border);
    gap: 24px;
    position: sticky;
    top: 0;
    z-index: 100;
  }

  .brand {
    font-weight: 700;
    font-size: 16px;
    color: var(--color-primary);
    white-space: nowrap;
  }

  .nav-links {
    flex: 1;
    display: flex;
    gap: 8px;
  }

  .nav-links a {
    padding: 6px 12px;
    border-radius: var(--radius);
    color: var(--color-text-muted);
    font-weight: 500;
    transition: background 0.15s, color 0.15s;
  }

  .nav-links a:hover {
    background: var(--color-bg);
    color: var(--color-text);
    text-decoration: none;
  }

  .nav-user {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .username {
    color: var(--color-text-muted);
    font-size: 13px;
  }

  .btn-logout {
    background: none;
    border: 1px solid var(--color-border);
    padding: 5px 14px;
    border-radius: var(--radius);
    color: var(--color-text-muted);
    font-size: 13px;
    transition: border-color 0.15s, color 0.15s;
  }

  .btn-logout:hover {
    border-color: var(--color-danger);
    color: var(--color-danger);
  }

  main {
    max-width: 1200px;
    margin: 0 auto;
    padding: 28px 24px;
  }

  main.no-nav {
    padding: 0;
    max-width: none;
  }
</style>
