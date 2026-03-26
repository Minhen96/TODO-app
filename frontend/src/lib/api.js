async function request(method, path, body, extraHeaders = {}) {
  const headers = { 'Content-Type': 'application/json', ...extraHeaders };

  if (typeof localStorage !== 'undefined') {
    const token = localStorage.getItem('accessToken');
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });

  // Try token refresh on 401
  if (res.status === 401 && path !== '/api/auth/refresh') {
    const refreshToken = typeof localStorage !== 'undefined'
      ? localStorage.getItem('refreshToken')
      : null;

    if (refreshToken) {
      try {
        const refreshRes = await fetch('/api/auth/refresh', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });

        if (refreshRes.ok) {
          const data = await refreshRes.json();
          localStorage.setItem('accessToken', data.accessToken);
          localStorage.setItem('refreshToken', data.refreshToken);

          headers['Authorization'] = `Bearer ${data.accessToken}`;
          const retry = await fetch(path, {
            method,
            headers,
            body: body ? JSON.stringify(body) : undefined
          });

          if (retry.status === 204) return null;
          if (retry.ok) return retry.json();
        }
      } catch {}
    }

    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
    if (typeof window !== 'undefined') window.location.href = '/login';
    return;
  }

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(error.message || `Request failed: ${res.status}`);
  }

  if (res.status === 204) return null;
  return res.json();
}

export const auth = {
  login: (username, password) =>
    request('POST', '/api/auth/login', { username, password }),

  register: (username, email, password) =>
    request('POST', '/api/auth/register', { username, email, password }),

  logout: () => request('POST', '/api/auth/logout')
};

export const tasks = {
  list: (params = {}) => {
    const query = new URLSearchParams();
    if (params.status) query.set('status', params.status);
    query.set('page', params.page ?? 0);
    query.set('size', params.size ?? 20);
    query.set('sort', 'createdAt,desc');
    return request('GET', `/api/tasks?${query}`);
  },

  get: (id) => request('GET', `/api/tasks/${id}`),

  create: (data) =>
    request('POST', '/api/tasks', data, {
      'Idempotency-Key': crypto.randomUUID()
    }),

  count: () => request('GET', '/api/tasks/count')
};
