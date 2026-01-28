import axios from 'axios';

// API Configuration - Memo Gateway
const GATEWAY_URL = 'http://localhost:8086'; // gateway-product port

const api = axios.create({
    baseURL: GATEWAY_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true // Important for SSO cookies
});

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

// Interceptor to handle 401s
api.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            console.warn("Unauthorized - session might be expired.");
            localStorage.removeItem('access_token');
            // We rely on React State (AuthContext) to update UI, but window redirect is a safety net
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
            if (res.data.tokens && res.data.tokens.access_token) {
                localStorage.setItem('access_token', res.data.tokens.access_token);
            }
            return res.data;
        });
    },

    logout: () => {
        return api.post('/auth/logout/global').then(() => {
            localStorage.removeItem('access_token');
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
    completeTask: (taskId, action, comment, variables) => api.post(`/memo/api/tasks/${taskId}/action`, {
        action,
        comment,
        variables
    }).then(res => res.data),

    // Get tasks for a specific memo
    getTasksForMemo: (memoId) => api.get(`/memo/api/tasks/memo/${memoId}`).then(res => res.data),

    // Committee voting (still through workflow for now, TODO: move to memo)
    castVote: (taskId, decision, comment) => api.post(`/workflow/api/tasks/${taskId}/vote`, { decision, comment }).then(res => res.data),
    getVoteStatus: (taskId) => api.get(`/workflow/api/tasks/${taskId}/vote-status`).then(res => res.data),

    // Send Back / Reject
    getReturnPoints: (taskId) => api.get(`/memo/api/tasks/${taskId}/return-points`).then(res => res.data),
    sendBackTask: (taskId, targetActivityId, reason) => api.post(`/memo/api/tasks/${taskId}/send-back`, { targetActivityId, reason }).then(res => res.data),
};

export const HistoryApi = {
    getTimeline: (processInstanceId) => api.get(`/workflow/api/history/timeline/${processInstanceId}`).then(res => res.data),
    getTimelineByType: (processInstanceId, type) => api.get(`/workflow/api/history/timeline/${processInstanceId}/by-type?actionType=${type}`).then(res => res.data),
    getVariableHistory: (processInstanceId) => api.get(`/workflow/api/history/variables/${processInstanceId}`).then(res => res.data),
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
};

export default api;


