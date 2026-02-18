// API Configuration - Admin Gateway
const GATEWAY_URL = import.meta.env.VITE_GATEWAY_URL || (import.meta.env.PROD ? '' : 'http://localhost:8085');


let accessToken = localStorage.getItem('accessToken');
let isRefreshing = false;
let refreshPromise = null;

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

// Try to refresh the token
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
        throw new Error('No refresh token');
    }

    const response = await fetch(`${GATEWAY_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            refreshToken: refreshToken,
            productCode: 'CAS_ADMIN'
        }),
    });

    if (!response.ok) {
        throw new Error('Refresh failed');
    }

    const data = await response.json();
    setToken(data.access_token);
    localStorage.setItem('refreshToken', data.refresh_token);
    return data.access_token;
}

async function request(endpoint, options = {}, isRetry = false) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (accessToken) {
        headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${GATEWAY_URL}${endpoint}`, {
        ...options,
        headers,
        credentials: 'include', // Enable SSO cookie sending
    });

    // On 401, try to refresh token (but only once)
    if (response.status === 401 && !isRetry) {
        try {
            // Prevent multiple simultaneous refresh attempts
            if (!isRefreshing) {
                isRefreshing = true;
                refreshPromise = refreshAccessToken();
            }
            await refreshPromise;
            isRefreshing = false;
            refreshPromise = null;

            // Retry the original request with new token
            return request(endpoint, options, true);
        } catch (refreshError) {
            isRefreshing = false;
            refreshPromise = null;
            clearToken();
            window.location.href = '/login';
            throw new Error('Session expired');
        }
    }

    if (response.status === 401) {
        clearToken();
        window.location.href = '/login';
        throw new Error('Unauthorized');
    }

    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: 'Request failed' }));
        throw new Error(error.message || error.errorDescription || 'Request failed');
    }

    if (response.status === 204) return null;
    return response.json();
}

// Auth
export const auth = {
    login: (username, password, productCode = 'CAS_ADMIN') =>
        request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password, productCode }),
        }),
    refresh: (refreshToken, productCode = 'CAS_ADMIN') =>
        request('/auth/refresh', {
            method: 'POST',
            body: JSON.stringify({ refreshToken, productCode }),
        }),

    /**
     * Check if there's an active SSO session.
     * Returns session info if active, null if not.
     */
    checkSSO: async () => {
        try {
            const response = await fetch(`${GATEWAY_URL}/auth/session`, {
                credentials: 'include'
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
     */
    getTokenForProduct: async (productCode = 'CAS_ADMIN') => {
        const response = await fetch(`${GATEWAY_URL}/auth/token-for-product`, {
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
            await fetch(`${GATEWAY_URL}/auth/logout/global`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.warn('Global logout request failed:', error);
        }

        // Clear local storage
        clearToken();

        // Clear SSO check flag so next visit will check SSO
        sessionStorage.removeItem('sso_check_cas_admin_in_progress');

        // Redirect to login
        window.location.href = '/login';
    }
};

// Users
export const users = {
    list: (page = 0, size = 20, search = '') =>
        request(`/admin/users?page=${page}&size=${size}${search ? `&search=${search}` : ''}`),
    get: (id) => request(`/admin/users/${id}`),
    create: (data) => request('/admin/users', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/admin/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/admin/users/${id}`, { method: 'DELETE' }),

    // Role management with constraints
    getRoles: (userId) => request(`/admin/users/${userId}/roles`),
    assignRole: (userId, roleId, constraints = {}) =>
        request(`/admin/users/${userId}/roles`, {
            method: 'POST',
            body: JSON.stringify({ roleId, constraints })
        }),
    updateConstraints: (userId, userRoleId, constraints) =>
        request(`/admin/users/${userId}/roles/${userRoleId}/constraints`, {
            method: 'PUT',
            body: JSON.stringify({ constraints })
        }),
    removeRole: (userId, userRoleId) =>
        request(`/admin/users/${userId}/roles/${userRoleId}`, { method: 'DELETE' }),
};

// Roles
export const roles = {
    list: (productCode = null) =>
        request(`/admin/roles${productCode ? `?productCode=${productCode}` : ''}`),
    create: (data) => request('/admin/roles', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/admin/roles/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/admin/roles/${id}`, { method: 'DELETE' }),
    setPermissions: (id, permissionIds) =>
        request(`/admin/roles/${id}/permissions`, { method: 'PUT', body: JSON.stringify({ permissionIds }) }),
};

// Products
export const products = {
    list: () => request('/admin/products'),
    get: (code) => request(`/admin/products/${code}`),
    create: (data) => request('/admin/products', { method: 'POST', body: JSON.stringify(data) }),
    update: (code, data) => request(`/admin/products/${code}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (code) => request(`/admin/products/${code}`, { method: 'DELETE' }),

    // Permissions
    permissions: (code) => request(`/admin/products/${code}/permissions`),
    createPermission: (code, data) =>
        request(`/admin/products/${code}/permissions`, { method: 'POST', body: JSON.stringify(data) }),
    updatePermission: (code, permId, data) =>
        request(`/admin/products/${code}/permissions/${permId}`, { method: 'PUT', body: JSON.stringify(data) }),
    deletePermission: (code, permId) =>
        request(`/admin/products/${code}/permissions/${permId}`, { method: 'DELETE' }),
};

// Standalone Permissions (can be assigned to multiple products)
export const permissions = {
    list: () => request('/admin/permissions'),
    get: (id) => request(`/admin/permissions/${id}`),
    create: (data) => request('/admin/permissions', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/admin/permissions/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/admin/permissions/${id}`, { method: 'DELETE' }),
    assignProducts: (id, productCodes) =>
        request(`/admin/permissions/${id}/products`, { method: 'POST', body: JSON.stringify({ productCodes }) }),
    removeProduct: (id, productCode) =>
        request(`/admin/permissions/${id}/products/${productCode}`, { method: 'DELETE' }),
};

// API Clients
export const clients = {
    list: (page = 0, size = 20) => request(`/admin/clients?page=${page}&size=${size}`),
    get: (id) => request(`/admin/clients/${id}`),
    create: (data) => request('/admin/clients', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/admin/clients/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    revoke: (id) => request(`/admin/clients/${id}`, { method: 'DELETE' }),
    rotateSecret: (id) => request(`/admin/clients/${id}/rotate-secret`, { method: 'POST' }),
};

// Audit Logs (routed via gateway to Audit Service)
export const auditLogs = {
    // Search audit logs with filters
    search: (filters = {}, page = 0, size = 20) =>
        request(`/audit/search?page=${page}&size=${size}&sort=timestamp,desc`, {
            method: 'POST',
            body: JSON.stringify(filters)
        }),

    // Get a specific audit log by ID
    get: (id) => request(`/audit/${id}`),

    // Get audit trail for a specific resource
    byResource: (resourceType, resourceId, page = 0, size = 20) =>
        request(`/audit/resource/${resourceType}/${resourceId}?page=${page}&size=${size}`),

    // Get audit trail for a specific actor (user)
    byActor: (actorId, page = 0, size = 20) =>
        request(`/audit/actor/${actorId}?page=${page}&size=${size}`),

    // Get all logs for a correlation ID (trace a transaction)
    byCorrelation: (correlationId) =>
        request(`/audit/correlation/${correlationId}`),

    // Verify integrity of audit log chain (admin only)
    verifyIntegrity: (startSequence, endSequence) =>
        request(`/audit/verify-integrity?startSequence=${startSequence}&endSequence=${endSequence}`),
};

// API Logs (technical/system logs for debugging and monitoring)
export const apiLogs = {
    // Search API logs with filters
    search: (filters = {}, page = 0, size = 20) =>
        request(`/audit/api-logs/search?page=${page}&size=${size}&sort=timestamp,desc`, {
            method: 'POST',
            body: JSON.stringify(filters)
        }),

    // Get a specific API log by ID
    get: (id) => request(`/audit/api-logs/${id}`),

    // Get API logs by correlation ID
    byCorrelation: (correlationId) =>
        request(`/audit/api-logs/correlation/${correlationId}`),

    // Get API logs by endpoint
    byEndpoint: (endpoint, page = 0, size = 20) =>
        request(`/audit/api-logs/endpoint?endpoint=${encodeURIComponent(endpoint)}&page=${page}&size=${size}`),
};

// Activity Logs (user timeline and operational logs)
export const activityLogs = {
    // Search activity logs with filters
    search: (filters = {}, page = 0, size = 20) =>
        request(`/audit/activity-logs/search?page=${page}&size=${size}&sort=timestamp,desc`, {
            method: 'POST',
            body: JSON.stringify(filters)
        }),

    // Get a specific activity log by ID
    get: (id) => request(`/audit/activity-logs/${id}`),

    // Get activity logs for a specific user
    byUser: (userId, page = 0, size = 20) =>
        request(`/audit/activity-logs/user/${userId}?page=${page}&size=${size}`),

    // Get activity logs by entity
    byEntity: (entityType, entityId, page = 0, size = 20) =>
        request(`/audit/activity-logs/entity/${entityType}/${entityId}?page=${page}&size=${size}`),
};

// Policies (routed via gateway to Policy Engine)
export const policies = {
    list: () => request('/policies'),
    get: (id) => request(`/policies/${id}`),
    create: (data) => request('/policies', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/policies/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/policies/${id}`, { method: 'DELETE' }),
    activate: (id) => request(`/policies/${id}/activate`, { method: 'POST' }),
    deactivate: (id) => request(`/policies/${id}/deactivate`, { method: 'POST' }),
};

// ==================== ORGANIZATION SERVICE ====================

// Geographical Hierarchy
export const geo = {
    types: (country = 'NP') => request(`/org/geo/types?country=${country}`),
    locations: (country, type) => request(`/org/geo/locations?country=${country}&type=${type}`),
    children: (id) => request(`/org/geo/locations/${id}/children`),
    create: (data) => request('/org/geo/locations', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/org/geo/locations/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
};

// Branches
export const branches = {
    list: () => request('/org/branches'),
    get: (id) => request(`/org/branches/${id}`),
    getByCode: (code) => request(`/org/branches/code/${code}`),
    create: (data) => request('/org/branches', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/org/branches/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (id) => request(`/org/branches/${id}`, { method: 'DELETE' }),
};

// Departments
export const departments = {
    list: () => request('/org/departments'),
    root: () => request('/org/departments/root'),
    get: (id) => request(`/org/departments/${id}`),
    children: (id) => request(`/org/departments/${id}/children`),
    create: (data) => request('/org/departments', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/org/departments/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
};

// Groups
export const groups = {
    list: () => request('/org/groups'),
    byType: (type) => request(`/org/groups?type=${type}`),
    get: (id) => request(`/org/groups/${id}`),
    userGroups: (userId) => request(`/org/groups/user/${userId}`),
    create: (data) => request('/org/groups', { method: 'POST', body: JSON.stringify(data) }),
    update: (id, data) => request(`/org/groups/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    addMember: (id, data) => request(`/org/groups/${id}/members`, { method: 'POST', body: JSON.stringify(data) }),
    removeMember: (id, userId) => request(`/org/groups/${id}/members/${userId}`, { method: 'DELETE' }),
};

// ==================== WORKFLOW SERVICE ====================

// Process Templates (Workflow Definitions)
export const processTemplates = {
    list: (productId, activeOnly = false) =>
        request(`/workflow/templates?productId=${productId}&activeOnly=${activeOnly}`),
    get: (id) => request(`/workflow/templates/${id}`),
    create: (data) => request('/workflow/templates', {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    update: (id, data) => request(`/workflow/templates/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
    }),
    deploy: (id) => request(`/workflow/templates/${id}/deploy`, { method: 'POST' }),
    deprecate: (id) => request(`/workflow/templates/${id}/deprecate`, { method: 'POST' }),
    delete: (id) => request(`/workflow/templates/${id}`, { method: 'DELETE' }),
    createNewVersion: (id) => request(`/workflow/templates/${id}/new-version`, { method: 'POST' }),
};

// Process Instances
export const processInstances = {
    list: (productId, page = 0, size = 20) =>
        request(`/workflow/instances?productId=${productId}&page=${page}&size=${size}`),
    get: (id) => request(`/workflow/instances/${id}`),
    start: (data) => request('/workflow/instances', {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    cancel: (id, reason) => request(`/workflow/instances/${id}?reason=${encodeURIComponent(reason)}`, {
        method: 'DELETE'
    }),
    variables: (id) => request(`/workflow/instances/${id}/variables`),
};

// Tasks
export const tasks = {
    inbox: () => request('/workflow/tasks/inbox'),
    assigned: () => request('/workflow/tasks/assigned'),
    claimable: () => request('/workflow/tasks/claimable'),
    byProduct: (productId) => request(`/workflow/tasks/by-product?productId=${productId}`),
    byProcess: (processInstanceId) => request(`/workflow/tasks/by-process/${processInstanceId}`),
    get: (id) => request(`/workflow/tasks/${id}`),
    claim: (id) => request(`/workflow/tasks/${id}/claim`, { method: 'POST' }),
    complete: (id, data) => request(`/workflow/tasks/${id}/complete`, {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    delegate: (id, delegateTo) => request(`/workflow/tasks/${id}/delegate?delegateTo=${delegateTo}`, {
        method: 'POST'
    }),
};

// Workflow History
export const workflowHistory = {
    timeline: (processInstanceId) => request(`/workflow/history/timeline/${processInstanceId}`),
    variables: (processInstanceId) => request(`/workflow/history/variables/${processInstanceId}`),
};

// ==================== FORM SERVICE ====================

// Form Definitions
export const formDefinitions = {
    list: (productId, activeOnly = false) =>
        request(`/forms/definitions?productId=${productId}&activeOnly=${activeOnly}`),
    get: (id) => request(`/forms/definitions/${id}`),
    create: (data) => request('/forms/definitions', {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    update: (id, data) => request(`/forms/definitions/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
    }),
    activate: (id) => request(`/forms/definitions/${id}/activate`, { method: 'POST' }),
    deprecate: (id) => request(`/forms/definitions/${id}/deprecate`, { method: 'POST' }),
    delete: (id) => request(`/forms/definitions/${id}`, { method: 'DELETE' }),
};

// Form Submissions
export const formSubmissions = {
    submit: (data) => request('/forms/submissions', {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    get: (id) => request(`/forms/submissions/${id}`),
    byProcess: (processInstanceId) => request(`/forms/submissions/by-process/${processInstanceId}`),
    byTask: (taskId) => request(`/forms/submissions/by-task/${taskId}`),
    validate: (formId, data) => request(`/forms/submissions/validate?formId=${formId}`, {
        method: 'POST',
        body: JSON.stringify(data)
    }),
};

// Workflow Configurations (links products, workflows, forms, and assignment rules)
export const workflowConfigs = {
    list: (productCode = null, activeOnly = false) => {
        let url = '/workflow/workflow-configs';
        const params = [];
        if (productCode) params.push(`productCode=${productCode}`);
        if (activeOnly) params.push(`activeOnly=true`);
        if (params.length) url += '?' + params.join('&');
        return request(url);
    },
    get: (id) => request(`/workflow/workflow-configs/${id}`),
    getByCode: (code) => request(`/workflow/workflow-configs/code/${code}`),
    create: (data) => request('/workflow/workflow-configs', {
        method: 'POST',
        body: JSON.stringify(data)
    }),
    update: (id, data) => request(`/workflow/workflow-configs/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
    }),
    delete: (id) => request(`/workflow/workflow-configs/${id}`, { method: 'DELETE' }),
    activate: (id) => request(`/workflow/workflow-configs/${id}/activate`, { method: 'POST' }),
    deactivate: (id) => request(`/workflow/workflow-configs/${id}/deactivate`, { method: 'POST' }),

    // Task-Form mappings
    getFormForTask: (code, taskKey) =>
        request(`/workflow/workflow-configs/code/${code}/tasks/${taskKey}/form`),
    setTaskFormMapping: (id, taskKey, formId) =>
        request(`/workflow/workflow-configs/${id}/tasks/${taskKey}/form`, {
            method: 'PUT',
            body: JSON.stringify({ formId })
        }),
    removeTaskFormMapping: (id, taskKey) =>
        request(`/workflow/workflow-configs/${id}/tasks/${taskKey}/form`, { method: 'DELETE' }),

    // Assignment rules
    getAssignmentRule: (code, taskKey) =>
        request(`/workflow/workflow-configs/code/${code}/tasks/${taskKey}/assignment`),
    resolveAssignment: (code, taskKey, variables) =>
        request(`/workflow/workflow-configs/code/${code}/tasks/${taskKey}/resolve-assignment`, {
            method: 'POST',
            body: JSON.stringify(variables)
        }),
};

