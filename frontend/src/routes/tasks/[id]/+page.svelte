<script>
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { tasks } from '$lib/api.js';
  import { isLoggedIn } from '$lib/stores.js';
  import { get } from 'svelte/store';

  let task = null;
  let loading = true;
  let error = '';

  onMount(async () => {
    if (!get(isLoggedIn)) { goto('/login'); return; }
    try {
      task = await tasks.get($page.params.id);
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  });

  function formatDate(ts) {
    if (!ts) return null;
    return new Date(ts).toLocaleString();
  }

  function statusClass(status) {
    const map = {
      PENDING: 'status-pending', VALIDATING: 'status-validating',
      PROCESSING: 'status-processing', COMPLETED: 'status-completed',
      FAILED: 'status-failed', CANCELLED: 'status-cancelled', DEAD_LETTER: 'status-dlq'
    };
    return map[status] ?? 'status-pending';
  }

  function priorityClass(priority) {
    const map = { LOW: 'pri-low', NORMAL: 'pri-normal', HIGH: 'pri-high', CRITICAL: 'pri-critical' };
    return map[priority] ?? 'pri-normal';
  }

  function copyTrace() {
    if (task?.traceId) navigator.clipboard.writeText(task.traceId);
  }

  $: isFailed = task?.status === 'FAILED' || task?.status === 'DEAD_LETTER';
  $: isCompleted = task?.status === 'COMPLETED';
</script>

<div class="back">
  <a href="/tasks">← Back to Tasks</a>
</div>

{#if loading}
  <div class="loading">Loading task…</div>
{:else if error}
  <div class="alert-error">{error}</div>
{:else if task}
  <div class="task-header">
    <div class="title-row">
      <h1>{task.name}</h1>
      <span class="badge {statusClass(task.status)}">{task.status}</span>
    </div>
    <div class="meta-row">
      <span class="badge {priorityClass(task.priority)}">{task.priority}</span>
      <span class="type-badge">{task.type}</span>
      <span class="task-id">ID: {task.id}</span>
    </div>
  </div>

  {#if isFailed && task.errorMessage}
    <div class="alert-error">
      <strong>Error:</strong> {task.errorMessage}
      {#if task.retryCount > 0}
        <span class="retry-info"> · Retried {task.retryCount}/{task.maxRetries} times</span>
      {/if}
    </div>
  {/if}

  {#if isCompleted && task.result}
    <div class="alert-success">
      <strong>Result:</strong>
      <pre>{JSON.stringify(task.result, null, 2)}</pre>
    </div>
  {/if}

  <div class="grid">
    <!-- Left: Details -->
    <div class="card">
      <h2>Details</h2>
      <dl>
        {#if task.description}
          <div class="dl-row">
            <dt>Description</dt>
            <dd>{task.description}</dd>
          </div>
        {/if}
        <div class="dl-row">
          <dt>Type</dt>
          <dd>{task.type}</dd>
        </div>
        <div class="dl-row">
          <dt>Priority</dt>
          <dd><span class="badge {priorityClass(task.priority)}">{task.priority}</span></dd>
        </div>
        <div class="dl-row">
          <dt>Status</dt>
          <dd><span class="badge {statusClass(task.status)}">{task.status}</span></dd>
        </div>
        <div class="dl-row">
          <dt>Retries</dt>
          <dd>{task.retryCount} / {task.maxRetries}</dd>
        </div>
        {#if task.payload}
          <div class="dl-row">
            <dt>Payload</dt>
            <dd><pre class="code">{JSON.stringify(task.payload, null, 2)}</pre></dd>
          </div>
        {/if}
      </dl>
    </div>

    <!-- Right: Timeline -->
    <div class="card">
      <h2>Timeline</h2>
      <div class="timeline">
        <div class="tl-item">
          <div class="tl-dot dot-created"></div>
          <div>
            <div class="tl-label">Created</div>
            <div class="tl-time">{formatDate(task.createdAt) ?? '—'}</div>
          </div>
        </div>
        {#if task.scheduledAt}
          <div class="tl-item">
            <div class="tl-dot dot-scheduled"></div>
            <div>
              <div class="tl-label">Scheduled</div>
              <div class="tl-time">{formatDate(task.scheduledAt)}</div>
            </div>
          </div>
        {/if}
        {#if task.startedAt}
          <div class="tl-item">
            <div class="tl-dot dot-started"></div>
            <div>
              <div class="tl-label">Started</div>
              <div class="tl-time">{formatDate(task.startedAt)}</div>
            </div>
          </div>
        {/if}
        {#if task.completedAt}
          <div class="tl-item">
            <div class="tl-dot dot-done"></div>
            <div>
              <div class="tl-label">Completed</div>
              <div class="tl-time">{formatDate(task.completedAt)}</div>
            </div>
          </div>
        {/if}
        <div class="tl-item">
          <div class="tl-dot dot-updated"></div>
          <div>
            <div class="tl-label">Last Updated</div>
            <div class="tl-time">{formatDate(task.updatedAt) ?? '—'}</div>
          </div>
        </div>
      </div>
    </div>
  </div>

  {#if task.traceId}
    <div class="trace-card">
      <span class="trace-label">Trace ID</span>
      <code class="trace-id">{task.traceId}</code>
      <button class="btn-copy" on:click={copyTrace}>Copy</button>
    </div>
  {/if}
{/if}

<style>
  .back {
    margin-bottom: 20px;
    font-size: 13px;
  }

  .back a { color: var(--color-text-muted); }
  .back a:hover { color: var(--color-text); }

  .loading {
    padding: 60px;
    text-align: center;
    color: var(--color-text-muted);
  }

  .task-header {
    margin-bottom: 24px;
  }

  .title-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
    flex-wrap: wrap;
  }

  h1 {
    font-size: 24px;
    font-weight: 700;
  }

  .meta-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .task-id {
    font-size: 11px;
    color: var(--color-text-muted);
    font-family: monospace;
  }

  .alert-error {
    background: #fef2f2;
    border: 1px solid #fecaca;
    color: #b91c1c;
    padding: 12px 16px;
    border-radius: var(--radius);
    margin-bottom: 20px;
    font-size: 13px;
  }

  .retry-info { color: #9f1239; }

  .alert-success {
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    color: #15803d;
    padding: 12px 16px;
    border-radius: var(--radius);
    margin-bottom: 20px;
    font-size: 13px;
  }

  .alert-success pre { margin-top: 8px; font-size: 12px; }

  .grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    margin-bottom: 16px;
  }

  @media (max-width: 640px) {
    .grid { grid-template-columns: 1fr; }
  }

  .card {
    background: white;
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 20px 24px;
  }

  h2 {
    font-size: 14px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--color-text-muted);
    margin-bottom: 16px;
  }

  dl { display: flex; flex-direction: column; gap: 12px; }

  .dl-row { display: flex; gap: 12px; }
  dt { width: 100px; flex-shrink: 0; font-size: 12px; color: var(--color-text-muted); padding-top: 2px; }
  dd { font-size: 13px; }

  .code {
    font-size: 11px;
    background: var(--color-bg);
    padding: 8px;
    border-radius: 4px;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .timeline { display: flex; flex-direction: column; gap: 16px; }

  .tl-item { display: flex; align-items: flex-start; gap: 12px; }

  .tl-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    margin-top: 3px;
    flex-shrink: 0;
  }

  .dot-created   { background: var(--color-text-muted); }
  .dot-scheduled { background: var(--color-info); }
  .dot-started   { background: var(--color-warning); }
  .dot-done      { background: var(--color-success); }
  .dot-updated   { background: var(--color-border); }

  .tl-label { font-size: 12px; font-weight: 500; }
  .tl-time  { font-size: 12px; color: var(--color-text-muted); }

  .trace-card {
    background: white;
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 14px 20px;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .trace-label {
    font-size: 12px;
    font-weight: 600;
    color: var(--color-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    white-space: nowrap;
  }

  .trace-id {
    font-family: monospace;
    font-size: 12px;
    color: var(--color-text);
    flex: 1;
    word-break: break-all;
  }

  .btn-copy {
    background: white;
    border: 1px solid var(--color-border);
    border-radius: var(--radius);
    padding: 4px 12px;
    font-size: 12px;
    color: var(--color-text-muted);
    white-space: nowrap;
  }

  .btn-copy:hover { border-color: var(--color-primary); color: var(--color-primary); }

  /* Badges (same as tasks page) */
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

  .status-pending   { background: #f1f5f9; color: #475569; }
  .status-validating{ background: #eff6ff; color: #1d4ed8; }
  .status-processing{ background: #fffbeb; color: #b45309; }
  .status-completed { background: #f0fdf4; color: #15803d; }
  .status-failed    { background: #fef2f2; color: #b91c1c; }
  .status-cancelled { background: #f8fafc; color: #94a3b8; }
  .status-dlq       { background: #fff1f2; color: #9f1239; }

  .pri-low      { background: #f1f5f9; color: #64748b; }
  .pri-normal   { background: #eff6ff; color: #1d4ed8; }
  .pri-high     { background: #fff7ed; color: #c2410c; }
  .pri-critical { background: #fef2f2; color: #b91c1c; }
</style>
