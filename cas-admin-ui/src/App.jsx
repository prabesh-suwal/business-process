import { useState } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { getToken } from './api'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import UsersPage from './pages/UsersPage'
import RolesPage from './pages/RolesPage'
import ClientsPage from './pages/ClientsPage'
import AuditLogsPage from './pages/AuditLogsPage'
import PoliciesPage from './pages/PoliciesPage'
import ProductsPage from './pages/ProductsPage'
import PermissionsPage from './pages/PermissionsPage'
import BranchesPage from './pages/BranchesPage'
import DepartmentsPage from './pages/DepartmentsPage'
import GroupsPage from './pages/GroupsPage'
import WorkflowsPage from './pages/WorkflowsPage'
import WorkflowDesignerPage from './pages/WorkflowDesignerPage'
import WorkflowConfigsPage from './pages/WorkflowConfigsPage'
import FormsPage from './pages/FormsPage'
import FormDesignerPage from './pages/FormDesignerPage'
import FormPreviewPage from './pages/FormPreviewPage'

function PrivateRoute({ children }) {
    const token = getToken();
    return token ? children : <Navigate to="/login" replace />;
}

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            {/* Standalone Form Preview (opens in new tab, no layout) */}
            <Route path="/forms/:id/preview" element={<FormPreviewPage />} />
            <Route
                path="/"
                element={
                    <PrivateRoute>
                        <Layout />
                    </PrivateRoute>
                }
            >
                <Route index element={<DashboardPage />} />
                <Route path="users" element={<UsersPage />} />
                <Route path="roles" element={<RolesPage />} />
                <Route path="products" element={<ProductsPage />} />
                <Route path="permissions" element={<PermissionsPage />} />
                <Route path="clients" element={<ClientsPage />} />
                <Route path="audit-logs" element={<AuditLogsPage />} />
                <Route path="policies" element={<PoliciesPage />} />
                <Route path="branches" element={<BranchesPage />} />
                <Route path="departments" element={<DepartmentsPage />} />
                <Route path="groups" element={<GroupsPage />} />
                {/* Workflow Management */}
                <Route path="workflows" element={<WorkflowsPage />} />
                <Route path="workflows/:id/design" element={<WorkflowDesignerPage />} />
                <Route path="workflow-configs" element={<WorkflowConfigsPage />} />
                {/* Form Management */}
                <Route path="forms" element={<FormsPage />} />
                <Route path="forms/:id/design" element={<FormDesignerPage />} />
            </Route>
        </Routes>
    );
}


