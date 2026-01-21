import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import CreateMemo from './pages/CreateMemo';
import MemoEditor from './pages/MemoEditor';
import Login from './pages/Login';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<Layout />}>
                    <Route index element={<Dashboard />} />
                    <Route path="create" element={<CreateMemo />} />
                    <Route path="edit/:id" element={<MemoEditor />} />
                </Route>
            </Routes>
        </Router>
    );
}

export default App;
