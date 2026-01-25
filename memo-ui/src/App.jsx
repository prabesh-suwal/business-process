import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import CreateMemo from './pages/CreateMemo';
import MemoEditor from './pages/MemoEditor';
import Login from './pages/Login';
import Settings from './pages/Settings';
import TaskInbox from './pages/TaskInbox';
import WorkflowDesignerPage from './pages/WorkflowDesignerPage';
import ViewOnlyMemos from './pages/ViewOnlyMemos';
import ViewOnlyMemoDetail from './pages/ViewOnlyMemoDetail';

import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    <Route path="/login" element={<Login />} />

                    {/* Protected Routes */}
                    <Route path="/" element={
                        <ProtectedRoute>
                            <Layout />
                        </ProtectedRoute>
                    }>
                        <Route index element={<Dashboard />} />
                        <Route path="create" element={<CreateMemo />} />
                        <Route path="memo/:id" element={<MemoEditor />} />
                        <Route path="edit/:id" element={<MemoEditor />} />
                        <Route path="tasks" element={<TaskInbox />} />
                        <Route path="view-only-memos" element={<ViewOnlyMemos />} />
                        <Route path="memos/:id/view-only" element={<ViewOnlyMemoDetail />} />
                        <Route path="settings" element={<Settings />} />
                        <Route path="workflow/:topicId" element={<WorkflowDesignerPage />} />
                    </Route>
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;
