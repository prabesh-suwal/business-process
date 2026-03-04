import { Routes, Route } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import Dashboard from './pages/Dashboard';
import CreateMemo from './pages/CreateMemo';
import MemoEditor from './pages/MemoEditor';
import Drafts from './pages/Drafts';
import Login from './pages/Login';
import Settings from './pages/Settings';
import TaskInbox from './pages/TaskInbox';
import WorkflowDesignerPage from './pages/WorkflowDesignerPage';
import Memos from './pages/Memos';
import ViewOnlyMemoDetail from './pages/ViewOnlyMemoDetail';
import ReportsPage from './pages/ReportsPage';
import DmnListPage from './pages/DmnListPage';
import DmnDesignerPage from './pages/DmnDesignerPage';

import { AccessProvider } from './context/AccessContext';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import { GuardedRoute } from './components/Guard';

function App() {
    return (
        <AccessProvider>
            <AuthProvider>
                <Routes>

                    <Route path="/login" element={<Login />} />

                    <Route element={<ProtectedRoute />}>
                        <Route path="/" element={<MainLayout />}>
                            <Route index element={<Dashboard />} />
                            <Route path="create" element={<GuardedRoute access="MMS.MEMO.CREATE"><CreateMemo /></GuardedRoute>} />
                            <Route path="create-memo" element={<GuardedRoute access="MMS.MEMO.CREATE"><CreateMemo /></GuardedRoute>} />
                            <Route path="memo/:id" element={<MemoEditor />} />
                            <Route path="edit/:id" element={<MemoEditor />} />
                            <Route path="tasks" element={<GuardedRoute access="MMS.TASK.VIEW"><TaskInbox /></GuardedRoute>} />
                            <Route path="drafts" element={<GuardedRoute access="MMS.MEMO.CREATE"><Drafts /></GuardedRoute>} />
                            <Route path="memos" element={<GuardedRoute access="MMS.MEMO.VIEW"><Memos /></GuardedRoute>} />
                            <Route path="memos/:id/view" element={<ViewOnlyMemoDetail />} />
                            <Route path="memos/:id/view-only" element={<ViewOnlyMemoDetail />} />
                            <Route path="settings" element={<GuardedRoute module="MMS.CONFIG"><Settings /></GuardedRoute>} />
                            <Route path="reports" element={<GuardedRoute module="MMS.REPORT"><ReportsPage /></GuardedRoute>} />
                            <Route path="archives" element={<GuardedRoute access="MMS.MEMO.VIEW"><Memos /></GuardedRoute>} />
                            <Route path="workflow/:topicId" element={<GuardedRoute access="MMS.WORKFLOW.DESIGN"><WorkflowDesignerPage /></GuardedRoute>} />
                            <Route path="dmn" element={<GuardedRoute module="MMS.DMN"><DmnListPage /></GuardedRoute>} />
                            <Route path="dmn/:decisionId" element={<GuardedRoute module="MMS.DMN"><DmnDesignerPage /></GuardedRoute>} />
                        </Route>
                    </Route>

                </Routes>
            </AuthProvider>
        </AccessProvider>
    );
}

export default App;

