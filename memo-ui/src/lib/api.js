import axios from 'axios';

// API Configuration - Memo Gateway
const GATEWAY_URL = 'http://localhost:8088';

const api = axios.create({
    baseURL: GATEWAY_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true // Important for SSO cookies
});

// Add interceptor to inject X-User-Id (simulating Gateway behavior for dev/demo if needed, 
// though typically Gateway handles this. For local dev against backend directly, we might need it.
// But here we go through Gateway or Vite Proxy. 
// If going through Vite Proxy -> Gateway, Gateway might expect cookie.
// If we are mocking user, we can send X-User-Id.
// Interceptor to handle 401s
api.interceptors.response.use(
    response => response,
    error => {
        if (error.response && error.response.status === 401) {
            console.warn("Unauthorized - session might be expired.");
            // Redirect to Internal Login
            if (!window.location.pathname.includes('/login')) {
                window.location.href = '/login';
            }
            return Promise.reject(error);
        }
        return Promise.reject(error);
    }
);

export const MemoApi = {
    // Config
    getCategories: () => api.get('/memo-config/categories').then(res => res.data),
    getTopics: (categoryId) => api.get(`/memo-config/categories/${categoryId}/topics`).then(res => res.data),

    // Memos
    createDraft: (data) => api.post('/memos/draft', data).then(res => res.data),
    getMemo: (id) => api.get(`/memos/${id}`).then(res => res.data),
    updateMemo: (id, data) => api.put(`/memos/${id}`, data).then(res => res.data),
    getMyMemos: () => api.get('/memos/my-memos').then(res => res.data),

    // File Upload (Future)
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
        // Post to Gateway (which proxies to CAS)
        // Matching cas-admin-ui: Send JSON body.
        return api.post('/auth/login', {
            username,
            password,
            productCode: 'MMS' // Assuming product code is required/useful
        });
    }
};

export default api;
