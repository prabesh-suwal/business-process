// LMS API Client - connecting to lms-gateway (ALL calls go through gateway)
const LMS_GATEWAY_URL = import.meta.env.PROD
    ? 'https://lms-api.example.com'
    : 'http://localhost:8086';  // LMS Gateway

let accessToken = localStorage.getItem('accessToken');

export function setToken(token) {
    accessToken = token;
    localStorage.setItem('accessToken', token);
}

export function getToken() {
    return accessToken;
}

export function clearToken() {
    accessToken = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
}

async function request(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${LMS_GATEWAY_URL}${endpoint}`, {
        ...options,
        headers,
        credentials: 'include', // Always include cookies for SSO
    });

    if (response.status === 401) {
        // Clear token and SSO check flag to prevent auto-login loop
        clearToken();
        // Mark that we had an auth failure (prevents SSO auto-login)
        sessionStorage.setItem('auth_failed', 'true');
        sessionStorage.removeItem('sso_check_in_progress');
        window.location.href = '/login';
        throw new Error('Unauthorized');
    }

    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: 'Request failed' }));
        throw new Error(error.message || 'Request failed');
    }

    if (response.status === 204) return null;
    return response.json();
}

// LMS-specific APIs
export const loanProducts = {
    list: () => request('/lms/api/loan-products'),
    get: (id) => request(`/lms/api/loan-products/${id}`),
    create: (data) => request('/lms/api/loan-products', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    update: (id, data) => request(`/lms/api/loan-products/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    }),
    delete: (id) => request(`/lms/api/loan-products/${id}`, {
        method: 'DELETE',
    }),
};

export const loanApplications = {
    list: () => request('/lms/api/loan-applications'),
    get: (id) => request(`/lms/api/loan-applications/${id}`),
    getMyApplications: () => request('/lms/api/loan-applications/my-submissions'),
    getByStatus: (status) => request(`/lms/api/loan-applications?status=${status}`),
    create: (data) => request('/lms/api/loan-applications', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    submit: (id) => request(`/lms/api/loan-applications/${id}/submit`, {
        method: 'POST',
    }),
};

// Person Service API
export const persons = {
    search: (query) => request(`/lms/api/persons/search?q=${encodeURIComponent(query)}`),
    get: (id) => request(`/lms/api/persons/${id}`),
    get360View: (id) => request(`/lms/api/persons/${id}/360-view`),
    getByCitizenship: (value) => request(`/lms/api/persons/by-identifier?type=CITIZENSHIP&value=${value}`),
    create: (data) => request('/lms/api/persons', {
        method: 'POST',
        body: JSON.stringify(data),
    }),
    getRelationships: (id) => request(`/lms/api/persons/${id}/relationships`),
    getRoles: (id) => request(`/lms/api/persons/${id}/roles`),
};

// Tasks API - connecting to workflow-service via gateway
export const tasks = {
    getMine: () => request('/workflow/api/tasks/my'),
    getClaimable: () => request('/workflow/api/tasks/claimable'),
    claim: (taskId) => request(`/workflow/api/tasks/${taskId}/claim`, { method: 'POST' }),
    unclaim: (taskId) => request(`/workflow/api/tasks/${taskId}/unclaim`, { method: 'POST' }),
    complete: (taskId, variables = {}) => request(`/workflow/api/tasks/${taskId}/complete`, {
        method: 'POST',
        body: JSON.stringify(variables),
    }),
    getForm: (taskId) => request(`/workflow/api/tasks/${taskId}/form`),
};

// Workflow Templates API
export const workflowTemplates = {
    list: (productId) => request(`/workflow/api/process-templates${productId ? `?productId=${productId}` : ''}`),
    get: (id) => request(`/workflow/api/process-templates/${id}`),
};

// Form Service API
export const forms = {
    getDefinition: (key) => request(`/lms/api/forms/definitions/key/${key}`),
    getDefinitionById: (id) => request(`/lms/api/forms/definitions/${id}`),
    submit: (key, data) => request(`/lms/api/forms/submissions/${key}`, {
        method: 'POST',
        body: JSON.stringify(data),
    }),
};

// Auth - ALL calls go through LMS Gateway (no direct CAS calls)
export const auth = {
    /**
     * Login using custom endpoint.
     * Gateway proxies to CAS and forwards SSO cookie.
     */
    login: async (username, password) => {
        const response = await fetch(`${LMS_GATEWAY_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // Include cookies for SSO
            body: JSON.stringify({
                username,
                password,
                productCode: 'LMS'
            })
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Login failed' }));
            throw new Error(error.message || 'Login failed');
        }

        const data = await response.json();

        // Store tokens from response (CAS returns snake_case)
        if (data.tokens) {
            setToken(data.tokens.access_token);
            localStorage.setItem('refreshToken', data.tokens.refresh_token);
        }

        // Store user info
        if (data.user) {
            localStorage.setItem('user', JSON.stringify(data.user));
        }

        return data;
    },

    /**
     * Global logout - destroys SSO session (logs out of all products).
     */
    logout: async () => {
        try {
            await fetch(`${LMS_GATEWAY_URL}/auth/logout/global`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (e) {
            console.warn('Global logout failed:', e);
        }
        clearToken();
        window.location.href = '/login';
    },

    /**
     * Check if there's an active SSO session.
     * Returns session info if active, null if not.
     */
    checkSSO: async () => {
        try {
            const response = await fetch(`${LMS_GATEWAY_URL}/auth/session`, {
                credentials: 'include' // Include SSO cookie
            });
            const data = await response.json();
            return data.active ? data : null;
        } catch (error) {
            console.warn('SSO check failed:', error);
            return null;
        }
    },

    /**
     * Get tokens for a product using existing SSO session.
     * Called when user already has valid SSO session from another product.
     */
    getTokenForProduct: async (productCode = 'LMS') => {
        const response = await fetch(`${LMS_GATEWAY_URL}/auth/token-for-product`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ productCode })
        });

        if (!response.ok) {
            throw new Error('Failed to get token for product');
        }

        const data = await response.json();

        // Store tokens (CAS returns snake_case: access_token, refresh_token)
        if (data.tokens) {
            setToken(data.tokens.access_token);
            localStorage.setItem('refreshToken', data.tokens.refresh_token);
        }

        // Store user info
        if (data.user) {
            localStorage.setItem('user', JSON.stringify(data.user));
        }

        return data;
    },

    /**
     * Global logout - clears SSO session and all local data.
     * This ensures user can't auto-login via SSO after logout.
     */
    globalLogout: async () => {
        try {
            // Call CAS global logout to invalidate SSO session
            await fetch(`${LMS_GATEWAY_URL}/auth/logout/global`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.warn('Global logout request failed:', error);
        }

        // Clear local storage
        clearToken();

        // Clear SSO check flag so next visit will check SSO
        sessionStorage.removeItem('sso_check_in_progress');

        // Redirect to login
        window.location.href = '/login';
    }
};
