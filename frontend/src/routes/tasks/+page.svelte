<script>
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { tasks } from '$lib/api.js';
  import { isLoggedIn } from '$lib/stores.js';
  import { get } from 'svelte/store';

  const STATUS_FILTERS = ['ALL', 'PENDING', 'VALIDATING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'DEAD_LETTER'];
  const TASK_TYPES = ['COMPUTE', 'IO', 'NOTIFICATION', 'SCHEDULED', 'BATCH'];
  const TASK_PRIORITIES = ['LOW', 'NORMAL', 'HIGH', 'CRITICAL'];

  let taskList = [];
  let totalElements = 0;
  let totalPages = 0;
  let currentPage = 0;
  let activeFilter = 'ALL';
  let loading = false;
  let error = '';
  let taskCount = 0;

  // Modal state
  let showModal = false;
  let creating = false;
  let createError = '';
  let form = { name: '', description: '', type: 'COMPUTE', priority: 'NORMAL', scheduledAt: '' };

  onMount(() => {
    if (!get(isLoggedIn)) { goto('/login'); return; }
    fetchTasks();
    fetchCount();
  });

  async function fetchTasks() {
    loading = true;
    error = '';
    try {
      const params = { page: currentPage, size: 20 };
      if (activeFilter !== 'ALL') params.status = activeFilter;
      const res = await tasks.list(params);
      taskList = res.content ?? [];
      totalElements = res.page?.totalElements ?? 0;
      totalPages = res.page?.totalPages ?? 0;
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  async function fetchCount() {
    try { taskCount = await tasks.count(); } catch {}
  }

  function setFilter(f) {
    activeFilter = f;
    currentPage = 0;
    fetchTasks();
  }

  function prevPage() { if (currentPage > 0) { currentPage--; fetchTasks(); } }
  function nextPage() { if (currentPage < totalPages - 1) { currentPage++; fetchTasks(); } }

  function openModal() {
    form = { name: '', description: '', type: 'COMPUTE', priority: 'NORMAL', scheduledAt: '' };
    createError = '';
    showModal = true;
  }

  async function handleCreate() {
    createError = '';
    creating = true;
    try {
      const payload = {
        name: form.name,
        type: form.type,
        priority: form.priority
      };
      if (form.description) payload.description = form.description;
      if (form.scheduledAt) payload.scheduledAt = new Date(form.scheduledAt).toISOString();
      await tasks.create(payload);
      showModal = false;
      currentPage = 0;
      fetchTasks();
      fetchCount();
    } catch (e) {
      createError = e.message;
    } finally {
      creating = false;
    }
  }

  function statusClass(status) {
    const map = {
      PENDING: 'status-pending',
      VALIDATING: 'status-validating',
      PROCESSING: 'status-processing',
      COMPLETED: 'status-completed',
      FAILED: 'status-failed',
      CANCELLED: 'status-cancelled',
      DEAD_LETTER: 'status-dlq'
    };
    return map[status] ?? 'status-pending';
  }

  function priorityClass(priority) {
    const map = { LOW: 'pri-low', NORMAL: 'pri-normal', HIGH: 'pri-high', CRITICAL: 'pri-critical' };
    return map[priority] ?? 'pri-normal';
  }

  function formatDate(ts) {
    if (!ts) return '—';
    return new Date(ts).toLocaleString();
  }
</script>

<div class="header">
  <div class="header-left">
    <h1>Tasks</h1>
    <span class="count-badge">{taskCount}</span>
  </div>
  <button class="btn-primary" on:click={openModal}>+ New Task</button>
</div>

<!-- Status filter tabs -->
<div class="filters">
  {#each STATUS_FILTERS as f}
    <button
      class="filter-tab"
      class:active={activeFilter === f}
      on:click={() => setFilter(f)}
    >
      {f === 'ALL' ? 'All' : f}
    </button>
  {/each}
</div>

{#if error}
  <div class="alert-error">{error}</div>
{/if}

<!-- Task table -->
<div class="table-wrap">
  {#if loading}
    <div class="loading">Loading…</div>
  {:else if taskList.length === 0}
    <div class="empty">
      <p>No tasks found.</p>
      <button class="btn-primary" on:click={openModal}>Create your first task</button>
    </div>
  {:else}
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Type</th>
          <th>Priority</th>
          <th>Status</th>
          <th>Created</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {#each taskList as task}
          <tr>
            <td class="name-cell">
              <span class="task-name">{task.name}</span>
              {#if task.description}
                <span class="task-desc">{task.description}</span>
              {/if}
            </td>
            <td><span class="type-badge">{task.type}</span></td>
            <td><span class="badge {priorityClass(task.priority)}">{task.priority}</span></td>
            <td><span class="badge {statusClass(task.status)}">{task.status}</span></td>
            <td class="date-cell">{formatDate(task.createdAt)}</td>
            <td>
              <a href="/tasks/{task.id}" class="btn-view">View</a>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>

    <!-- Pagination -->
    {#if totalPages > 1}
      <div class="pagination">
        <button on:click={prevPage} disabled={currentPage === 0}>← Prev</button>
        <span>Page {currentPage + 1} of {totalPages} ({totalElements} total)</span>
        <button on:click={nextPage} disabled={currentPage >= totalPages - 1}>Next →</button>
      </div>
    {/if}
  {/if}
</div>

<!-- Create Task Modal -->
{#if showModal}
  <div class="overlay" on:click|self={() => (showModal = false)} role="dialog" aria-modal="true">
    <div class="modal">
      <div class="modal-header">
        <h2>New Task</h2>
        <button class="btn-close" on:click={() => (showModal = false)}>✕</button>
      </div>

      {#if createError}
        <div class="alert-error">{createError}</div>
      {/if}

      <form on:submit|preventDefault={handleCreate}>
        <div class="field">
          <label for="name">Name <span class="required">*</span></label>
          <input id="name" type="text" bind:value={form.name} maxlength="255" required />
        </div>
        <div class="field">
          <label for="desc">Description</label>
          <textarea id="desc" bind:value={form.description} rows="3" maxlength="5000"></textarea>
        </div>
        <div class="row">
          <div class="field">
            <label for="type">Type <span class="required">*</span></label>
            <select id="type" bind:value={form.type}>
              {#each TASK_TYPES as t}
                <option value={t}>{t}</option>
              {/each}
            </select>
          </div>
          <div class="field">
            <label for="priority">Priority</label>
            <select id="priority" bind:value={form.priority}>
              {#each TASK_PRIORITIES as p}
                <option value={p}>{p}</option>
              {/each}
            </select>
          </div>
        </div>
        <div class="field">
          <label for="scheduledAt">Scheduled At (optional)</label>
          <input id="scheduledAt" type="datetime-local" bind:value={form.scheduledAt} />
        </div>
        <div class="modal-actions">
          <button type="button" class="btn-secondary" on:click={() => (showModal = false)}>Cancel</button>
          <button type="submit" class="btn-primary" disabled={creating}>
            {creating ? 'Creating…' : 'Create Task'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

<style>
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  h1 {
    font-size: 22px;
    font-weight: 700;
  }

  .count-badge {
    background: var(--color-primary);
    color: white;
    border-radius: 999px;
    padding: 2px 10px;
    font-size: 12px;
    font-weight: 600;
  }

  .btn-primary {
    background: var(--color-primary);
    color: white;
    border: none;
    border-radius: var(--radius);
    padding: 8px 16px;
    font-weight: 500;
    transition: background 0.15s;
  }

  .btn-primary:hover:not(:disabled) { background: var(--color-primary-hover); }
  .btn-primary:disabled { opacity: 0.65; cursor: not-allowed; }

  .btn-secondary {
    background: white;
    color: var(--color-text);
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    padding: 8px 16px;
    font-weight: 500;
    transition: background 0.15s;
  }

  .btn-secondary:hover { background: var(--color-bg); }

  .filters {
    display: flex;
    gap: 4px;
    margin-bottom: 16px;
    flex-wrap: wrap;
  }

  .filter-tab {
    padding: 6px 14px;
    border: 1px solid var(--color-border);
    border-radius: 999px;
    background: white;
    color: var(--color-text-muted);
    font-size: 12px;
    font-weight: 500;
    transition: all 0.15s;
  }

  .filter-tab:hover { border-color: var(--color-primary); color: var(--color-primary); }
  .filter-tab.active {
    background: var(--color-primary);
    border-color: var(--color-primary);
    color: white;
  }

  .alert-error {
    background: #fef2f2;
    border: 1px solid #fecaca;
    color: var(--color-danger);
    padding: 10px 14px;
    border-radius: var(--radius);
    margin-bottom: 16px;
    font-size: 13px;
  }

  .table-wrap {
    background: white;
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    overflow: hidden;
  }

  .loading, .empty {
    padding: 60px 24px;
    text-align: center;
    color: var(--color-text-muted);
  }

  .empty { display: flex; flex-direction: column; align-items: center; gap: 16px; }

  table {
    width: 100%;
    border-collapse: collapse;
  }

  thead tr {
    background: var(--color-bg);
    border-bottom: 1px solid var(--color-border);
  }

  th {
    padding: 10px 16px;
    text-align: left;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-muted);
  }

  td {
    padding: 12px 16px;
    border-bottom: 1px solid var(--color-border);
    vertical-align: middle;
  }

  tr:last-child td { border-bottom: none; }
  tr:hover td { background: #fafafa; }

  .name-cell { max-width: 280px; }
  .task-name { display: block; font-weight: 500; }
  .task-desc {
    display: block;
    font-size: 12px;
    color: var(--color-text-muted);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 260px;
  }

  .date-cell { font-size: 12px; color: var(--color-text-muted); white-space: nowrap; }

  .badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .type-badge {
    display: inline-block;
    padding: 2px 8px;
    border-radius: var(--radius);
    font-size: 11px;
    font-weight: 500;
    background: #f1f5f9;
    color: var(--color-text-muted);
  }

  /* Status badges */
  .status-pending   { background: #f1f5f9; color: #475569; }
  .status-validating{ background: #eff6ff; color: #1d4ed8; }
  .status-processing{ background: #fffbeb; color: #b45309; }
  .status-completed { background: #f0fdf4; color: #15803d; }
  .status-failed    { background: #fef2f2; color: #b91c1c; }
  .status-cancelled { background: #f8fafc; color: #94a3b8; }
  .status-dlq       { background: #fff1f2; color: #9f1239; }

  /* Priority badges */
  .pri-low      { background: #f1f5f9; color: #64748b; }
  .pri-normal   { background: #eff6ff; color: #1d4ed8; }
  .pri-high     { background: #fff7ed; color: #c2410c; }
  .pri-critical { background: #fef2f2; color: #b91c1c; }

  .btn-view {
    padding: 4px 12px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    font-size: 12px;
    color: var(--color-text);
    transition: border-color 0.15s, color 0.15s;
  }

  .btn-view:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    text-decoration: none;
  }

  .pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 16px;
    border-top: 1px solid var(--color-border);
    font-size: 13px;
    color: var(--color-text-muted);
  }

  .pagination button {
    background: white;
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    padding: 5px 12px;
    font-size: 13px;
    transition: background 0.15s;
  }

  .pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
  .pagination button:not(:disabled):hover { background: var(--color-bg); }

  /* Modal */
  .overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 200;
    padding: 24px;
  }

  .modal {
    background: white;
    border-radius: var(--radius);
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
    width: 100%;
    max-width: 500px;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px 0;
  }

  .modal-header h2 { font-size: 18px; font-weight: 600; }

  .btn-close {
    background: none;
    border: none;
    font-size: 16px;
    color: var(--color-text-muted);
    padding: 4px;
  }

  .btn-close:hover { color: var(--color-text); }

  form { padding: 20px 24px 24px; }

  .field { margin-bottom: 16px; }

  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 6px;
  }

  .required { color: var(--color-danger); }

  input[type="text"],
  input[type="datetime-local"],
  select,
  textarea {
    width: 100%;
    padding: 9px 12px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    outline: none;
    transition: border-color 0.15s, box-shadow 0.15s;
    background: white;
  }

  input:focus, select:focus, textarea:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
  }

  textarea { resize: vertical; }

  .row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .modal-actions {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    margin-top: 20px;
  }
</style>
