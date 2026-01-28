import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
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

import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    <Route path="/login" element={<Login />} />

                    {/* Protected Routes */}
                    <Route element={<ProtectedRoute />}>
                        <Route path="/" element={<MainLayout />}>
                            <Route index element={<Dashboard />} />
                            <Route path="create" element={<CreateMemo />} />
                            <Route path="create-memo" element={<CreateMemo />} />
                            <Route path="memo/:id" element={<MemoEditor />} />
                            <Route path="edit/:id" element={<MemoEditor />} />
                            <Route path="tasks" element={<TaskInbox />} />

                            <Route path="drafts" element={<Drafts />} />
                            <Route path="memos" element={<Memos />} />
                            <Route path="memos/:id/view" element={<ViewOnlyMemoDetail />} />
                            <Route path="memos/:id/view-only" element={<ViewOnlyMemoDetail />} />
                            <Route path="settings" element={<Settings />} />
                            <Route path="reports" element={<ReportsPage />} />
                            <Route path="archives" element={<Memos />} />
                            <Route path="workflow/:topicId" element={<WorkflowDesignerPage />} />
                        </Route>
                    </Route>
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;

