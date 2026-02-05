import axios from 'axios';

// API Configuration - Memo Gateway
const GATEWAY_URL = 'http://localhost:8086'; // gateway-product port

// Token refresh state
let isRefreshing = false;
let refreshPromise = null;

const api = axios.create({
    baseURL: GATEWAY_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true // Important for SSO cookies
});

// Try to refresh the token
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
        throw new Error('No refresh token');
    }

    const response = await axios.post(`${GATEWAY_URL}/auth/refresh`, {
        refreshToken: refreshToken,
        productCode: 'MMS'
    }, {
        headers: { 'Content-Type': 'application/json' }
    });

    if (response.status !== 200) {
        throw new Error('Refresh failed');
    }

    const data = response.data;
    localStorage.setItem('access_token', data.access_token);
    localStorage.setItem('refresh_token', data.refresh_token);
    return data.access_token;
}

// Clear all auth tokens
function clearTokens() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user');
}

// Request interceptor to add Bearer token
api.interceptors.request.use(
    config => {
        const token = localStorage.getItem('access_token');
        // console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`);
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

// Interceptor to handle 401s with token refresh
api.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;

        // On 401, try to refresh token (but only once per request)
        if (error.response && error.response.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

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
                const newToken = localStorage.getItem('access_token');
                originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
                return api(originalRequest);
            } catch (refreshError) {
                isRefreshing = false;
                refreshPromise = null;
                console.warn("Token refresh failed - redirecting to login.");
                clearTokens();
                if (!window.location.pathname.includes('/login')) {
                    window.location.href = '/login';
                }
                return Promise.reject(refreshError);
            }
        }

        // For other 401s (after retry failed), redirect to login
        if (error.response && error.response.status === 401) {
            console.warn("Unauthorized - session might be expired.");
            clearTokens();
            if (!window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

export const MemoApi = {
    // Config
    getCategories: () => api.get('/memo-config/categories').then(res => res.data),
    createCategory: (data) => api.post('/memo-config/categories', data).then(res => res.data),

    getTopics: (categoryId) => api.get(`/memo-config/categories/${categoryId}/topics`).then(res => res.data),
    createTopic: (data) => api.post('/memo-config/topics', data).then(res => res.data),

    getTopic: (topicId) => api.get(`/memo-config/topics/${topicId}`).then(res => res.data),
    updateTopicWorkflow: (topicId, workflowXml) => api.put(`/memo-config/topics/${topicId}/workflow`, workflowXml, {
        headers: { 'Content-Type': 'text/plain' }
    }).then(res => res.data),
    updateTopicFormSchema: (topicId, formSchema) => api.put(`/memo-config/topics/${topicId}/form-schema`, formSchema).then(res => res.data),

    // Deploy workflow to Flowable engine and link back templateId
    deployTopicWorkflow: (topicId) => api.post(`/memo-config/topics/${topicId}/deploy-workflow`).then(res => res.data),

    // Update topic viewer configuration
    updateTopicViewers: (topicId, viewerConfig) => api.patch(`/memo-config/topics/${topicId}/viewers`, viewerConfig).then(res => res.data),

    // Update topic override permissions (what users can customize when creating memos)
    updateTopicOverridePermissions: (topicId, overridePermissions) => api.patch(`/memo-config/topics/${topicId}/override-permissions`, overridePermissions).then(res => res.data),

    // Get available workflow variables for condition building
    getWorkflowVariables: (topicId) => api.get(`/memo-config/topics/${topicId}/workflow-variables`).then(res => res.data),

    // Copy workflow to new version (unlocks deployed workflow for editing)
    copyTopicWorkflow: (topicId) => api.post(`/memo-config/topics/${topicId}/copy-workflow`).then(res => res.data),

    // Update topic's default assignee configuration
    updateTopicDefaultAssignee: (topicId, config) => api.patch(`/memo-config/topics/${topicId}/default-assignee`, config).then(res => res.data),

    // ==================== GATEWAY CONFIGURATION ====================

    // Get all gateway configs for a topic
    getGatewayConfigs: (topicId) => api.get(`/memo/api/topics/${topicId}/gateways`).then(res => res.data),

    // Get a specific gateway config
    getGatewayConfig: (topicId, gatewayId) =>
        api.get(`/memo/api/topics/${topicId}/gateways/${gatewayId}`).then(res => res.data),

    // Save a gateway config (completion mode: ALL, ANY, N_OF_M)
    saveGatewayConfig: (topicId, gatewayId, config) =>
        api.put(`/memo/api/topics/${topicId}/gateways/${gatewayId}`, config).then(res => res.data),

    // Bulk save gateway configs
    saveGatewayConfigs: (topicId, configs) =>
        api.put(`/memo/api/topics/${topicId}/gateways`, configs).then(res => res.data),

    // Delete a gateway config
    deleteGatewayConfig: (topicId, gatewayId) =>
        api.delete(`/memo/api/topics/${topicId}/gateways/${gatewayId}`).then(res => res.data),

    // Memos
    createDraft: (data) => api.post('/memos/draft', data).then(res => res.data),
    getMemo: (id) => api.get(`/memos/${id}`).then(res => res.data),
    updateMemo: (id, data) => api.put(`/memos/${id}`, data).then(res => res.data),
    submitMemo: (id) => api.post(`/memos/${id}/submit`).then(res => res.data),
    getMyMemos: () => api.get('/memos/my-memos').then(res => res.data),

    // View-only access
    getViewableMemos: () => api.get('/memo/api/memos/viewable').then(res => res.data),
    canViewMemo: (id) => api.get(`/memo/api/memos/${id}/can-view`).then(res => res.data),

    // All accessible memos (created by, involved in workflow, or viewer)
    getAccessibleMemos: () => api.get('/memos/accessible').then(res => res.data),

    // Attachments
    uploadAttachment: (memoId, file) => {
        const formData = new FormData();
        formData.append('file', file);
        return api.post(`/memos/${memoId}/attachments`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        }).then(res => res.data);
    },
    getAttachments: (memoId) => api.get(`/memos/${memoId}/attachments`).then(res => res.data),
    getAttachmentUrl: (memoId, attachmentId) => `${GATEWAY_URL}/memos/${memoId}/attachments/${attachmentId}/download`,

    // Auth
    getSession: () => api.get('/auth/session').then(res => res.data),

    login: (username, password) => {
        return api.post('/auth/login', {
            username,
            password,
            productCode: 'MMS'
        }).then(res => {
            if (res.data.tokens) {
                if (res.data.tokens.access_token) {
                    localStorage.setItem('access_token', res.data.tokens.access_token);
                }
                if (res.data.tokens.refresh_token) {
                    localStorage.setItem('refresh_token', res.data.tokens.refresh_token);
                }
            }
            return res.data;
        });
    },

    logout: () => {
        return api.post('/auth/logout/global').then(() => {
            clearTokens();
        }).catch(() => {
            // Clear tokens even if logout request fails
            clearTokens();
        });
    }
};

export const WorkflowApi = {
    // Process Templates
    listTemplates: () => api.get('/workflow/api/process-templates').then(res => res.data.content),
    getTemplate: (id) => api.get(`/workflow/api/process-templates/${id}`).then(res => res.data),
    createTemplate: (data) => api.post('/workflow/api/process-templates', data).then(res => res.data),
    updateTemplate: (id, data) => api.put(`/workflow/api/process-templates/${id}`, data).then(res => res.data),
    deployTemplate: (id) => api.post(`/workflow/api/process-templates/${id}/deploy`).then(res => res.data),
    createNewVersion: (id) => api.post(`/workflow/api/process-templates/${id}/new-version`).then(res => res.data),

    // Workflow Variables
    getVariables: () => api.get('/workflow/api/workflow-variables').then(res => res.data),
};

export const TaskApi = {
    // Task Inbox - routed through memo-service
    getInbox: () => api.get('/memo/api/tasks/inbox').then(res => res.data),
    getTask: (taskId) => api.get(`/memo/api/tasks/${taskId}`).then(res => res.data),
    claimTask: (taskId) => api.post(`/memo/api/tasks/${taskId}/claim`).then(res => res.data),

    // Task actions (approve, reject, send_back, etc.)
    // cancelOthers: if true, cancels other parallel tasks (for "first approval wins" mode)
    completeTask: (taskId, action, comment, variables, cancelOthers = false) => {
        const url = cancelOthers
            ? `/memo/api/tasks/${taskId}/action?cancelOthers=true`
            : `/memo/api/tasks/${taskId}/action`;
        return api.post(url, {
            action,
            comment,
            variables
        }).then(res => res.data);
    },

    // Get tasks for a specific memo
    getTasksForMemo: (memoId) => api.get(`/memo/api/tasks/memo/${memoId}`).then(res => res.data),

    // Committee voting (still through workflow for now, TODO: move to memo)
    castVote: (taskId, decision, comment) => api.post(`/workflow/api/tasks/${taskId}/vote`, { decision, comment }).then(res => res.data),
    getVoteStatus: (taskId) => api.get(`/workflow/api/tasks/${taskId}/vote-status`).then(res => res.data),

    // Send Back / Reject
    getReturnPoints: (taskId) => api.get(`/memo/api/tasks/${taskId}/return-points`).then(res => res.data),
    sendBackTask: (taskId, targetActivityId, reason) => api.post(`/memo/api/tasks/${taskId}/send-back`, { targetActivityId, reason }).then(res => res.data),

    // ==================== PARALLEL EXECUTION TRACKING ====================

    // Get parallel execution status for a process (shows "2 of 3 branches completed")
    getParallelStatus: (processInstanceId) =>
        api.get(`/memo/api/tasks/parallel-status/${processInstanceId}`).then(res => res.data),

    // Get parallel execution status by memo ID (convenience method)
    getParallelStatusByMemo: (memoId) =>
        api.get(`/memo/api/tasks/parallel-status/memo/${memoId}`).then(res => res.data),

    // Get all active tasks for a process (multiple during parallel execution)
    getActiveTasks: (processInstanceId) =>
        api.get(`/memo/api/tasks/active/${processInstanceId}`).then(res => res.data),

    // Get active executions (tokens) in a process for visualization
    getActiveExecutions: (processInstanceId) =>
        api.get(`/workflow/api/tasks/executions/${processInstanceId}`).then(res => res.data),
};

export const HistoryApi = {
    getTimeline: (processInstanceId) => api.get(`/workflow/api/history/timeline/${processInstanceId}`).then(res => res.data),
    getTimelineByType: (processInstanceId, type) => api.get(`/workflow/api/history/timeline/${processInstanceId}/by-type?actionType=${type}`).then(res => res.data),
    getVariableHistory: (processInstanceId) => api.get(`/workflow/api/history/variables/${processInstanceId}`).then(res => res.data),
};

/**
 * Organization API - Geo locations, branches, departments for assignment dropdowns.
 * Routes through gateway to organization-service.
 */
export const OrganizationApi = {
    // Geo hierarchy types (Province, District, Municipality, etc.)
    getGeoTypes: (country = 'NP') => api.get(`/org/api/geo/types?country=${country}`).then(res => res.data),

    // Get locations by type (all provinces, all districts, etc.)
    getLocationsByType: (type, country = 'NP') => api.get(`/org/api/geo/locations?type=${type}&country=${country}`).then(res => res.data),

    // Get child locations of a parent (districts in a province, etc.)
    getChildLocations: (parentId) => api.get(`/org/api/geo/locations/${parentId}/children`).then(res => res.data),

    // Branches
    getAllBranches: () => api.get('/org/api/branches').then(res => res.data),
    getBranch: (id) => api.get(`/org/api/branches/${id}`).then(res => res.data),

    // Departments
    getAllDepartments: () => api.get('/org/api/departments').then(res => res.data),

    // Groups/Committees
    getAllGroups: () => api.get('/org/api/groups').then(res => res.data),
};

/**
 * CAS Admin API - Dynamic dropdown data for workflow configuration.
 * All roles/groups/departments fetched from CAS server.
 */
export const CasAdminApi = {
    // Assignment type options
    getAssignmentTypes: () => api.get('/cas-admin/workflow-config/assignment-types').then(res => res.data),

    // Roles for a product
    getRoles: (productCode = 'MMS') => api.get(`/cas-admin/workflow-config/roles?productCode=${productCode}`).then(res => res.data),

    // Groups/Committees
    getGroups: (productCode = 'MMS') => api.get(`/cas-admin/workflow-config/groups?productCode=${productCode}`).then(res => res.data),

    // Departments
    getDepartments: (productCode = 'MMS') => api.get(`/cas-admin/workflow-config/departments?productCode=${productCode}`).then(res => res.data),

    // Users for viewer selection
    getUsers: (productCode = 'MMS') => api.get(`/cas-admin/workflow-config/users?productCode=${productCode}`).then(res => res.data),

    // Organizational scopes
    getScopes: () => api.get('/cas-admin/workflow-config/scopes').then(res => res.data),

    // Condition operators
    getOperators: () => api.get('/cas-admin/workflow-config/operators').then(res => res.data),

    // Escalation actions
    getEscalationActions: () => api.get('/cas-admin/workflow-config/escalation-actions').then(res => res.data),

    // SLA duration options
    getSlaDurations: () => api.get('/cas-admin/workflow-config/sla-durations').then(res => res.data),

    // Preview assignment - resolve users
    previewAssignment: (data) => api.post('/cas-admin/workflow-config/preview-assignment', data).then(res => res.data),
};

/**
 * Workflow Config API - Per-step configuration.
 * Now routes to workflow-service for centralized task configuration.
 * 
 * NOTE: processTemplateId is stored in topic.workflowTemplateId after deployment.
 */
export const WorkflowConfigApi = {
    // Step configurations - uses processTemplateId (not topicId)
    // Legacy endpoints (memo-service) - still work but deprecated
    getStepConfigs: (topicId) => api.get(`/memo-admin/topics/${topicId}/workflow-config/steps`).then(res => res.data),
    getStepConfig: (topicId, taskKey) => api.get(`/memo-admin/topics/${topicId}/workflow-config/steps/${taskKey}`).then(res => res.data),
    saveStepConfig: (topicId, taskKey, config) => api.put(`/memo-admin/topics/${topicId}/workflow-config/steps/${taskKey}`, config).then(res => res.data),

    // NEW: Direct to workflow-service using processTemplateId
    getTaskConfigs: (processTemplateId) => api.get(`/workflow/api/process-templates/${processTemplateId}/task-configs`).then(res => res.data),
    getTaskConfig: (processTemplateId, taskKey) => api.get(`/workflow/api/process-templates/${processTemplateId}/task-configs/${taskKey}`).then(res => res.data),
    saveTaskConfig: (processTemplateId, taskKey, config) => api.put(`/workflow/api/process-templates/${processTemplateId}/task-configs/${taskKey}`, config).then(res => res.data),

    // Gateway decision rules
    getGatewayRules: (topicId) => api.get(`/memo-admin/topics/${topicId}/workflow-config/gateways`).then(res => res.data),
    getGatewayRule: (topicId, gatewayKey) => api.get(`/memo-admin/topics/${topicId}/workflow-config/gateways/${gatewayKey}`).then(res => res.data),
    saveGatewayRule: (topicId, gatewayKey, rule) => api.put(`/memo-admin/topics/${topicId}/workflow-config/gateways/${gatewayKey}`, rule).then(res => res.data),
    activateGatewayRule: (topicId, ruleId) => api.post(`/memo-admin/topics/${topicId}/workflow-config/gateways/${ruleId}/activate`).then(res => res.data),

    // Test gateway evaluation
    evaluateGateway: (topicId, gatewayKey, memoData) => api.post(`/memo-admin/topics/${topicId}/workflow-config/gateways/${gatewayKey}/evaluate`, memoData).then(res => res.data),

    // Preview assignment - shows which users would match the given assignment rules
    previewAssignment: (topicId, rules) => api.post(`/memo-admin/topics/${topicId}/workflow-config/assignments/preview`, rules).then(res => res.data),
};

export default api;


