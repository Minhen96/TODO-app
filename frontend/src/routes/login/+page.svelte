<script>
  import { goto } from '$app/navigation';
  import { auth } from '$lib/api.js';
  import { authStore } from '$lib/stores.js';

  let username = '';
  let password = '';
  let error = '';
  let loading = false;

  async function handleLogin() {
    error = '';
    loading = true;
    try {
      const res = await auth.login(username, password);
      authStore.login(res);
      goto('/tasks');
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }
</script>

<div class="page">
  <div class="card">
    <h1>Task Platform</h1>
    <p class="subtitle">Sign in to your account</p>

    {#if error}
      <div class="alert">{error}</div>
    {/if}

    <form on:submit|preventDefault={handleLogin}>
      <div class="field">
        <label for="username">Username</label>
        <input id="username" type="text" bind:value={username} autocomplete="username" required />
      </div>
      <div class="field">
        <label for="password">Password</label>
        <input id="password" type="password" bind:value={password} autocomplete="current-password" required />
      </div>
      <button type="submit" class="btn-primary" disabled={loading}>
        {loading ? 'Signing in…' : 'Sign in'}
      </button>
    </form>

    <p class="footer">Don't have an account? <a href="/register">Register</a></p>
  </div>
</div>

<style>
  .page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: var(--color-bg);
  }

  .card {
    background: white;
    border-radius: var(--radius);
    box-shadow: var(--shadow-md);
    padding: 40px;
    width: 100%;
    max-width: 400px;
  }

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: var(--color-primary);
    margin-bottom: 4px;
  }

  .subtitle {
    color: var(--color-text-muted);
    margin-bottom: 28px;
  }

  .alert {
    background: #fef2f2;
    border: 1px solid #fecaca;
    color: var(--color-danger);
    padding: 10px 14px;
    border-radius: var(--radius);
    margin-bottom: 16px;
    font-size: 13px;
  }

  .field {
    margin-bottom: 16px;
  }

  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 6px;
  }

  input {
    width: 100%;
    padding: 9px 12px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    outline: none;
    transition: border-color 0.15s, box-shadow 0.15s;
  }

  input:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
  }

  .btn-primary {
    width: 100%;
    padding: 10px;
    background: var(--color-primary);
    color: white;
    border: none;
    border-radius: var(--radius);
    font-weight: 500;
    margin-top: 8px;
    transition: background 0.15s;
  }

  .btn-primary:hover:not(:disabled) {
    background: var(--color-primary-hover);
  }

  .btn-primary:disabled {
    opacity: 0.65;
    cursor: not-allowed;
  }

  .footer {
    text-align: center;
    margin-top: 20px;
    color: var(--color-text-muted);
    font-size: 13px;
  }
</style>
