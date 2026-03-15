import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import AdminDashboard from './pages/AdminDashboard';
import LogsPage from './pages/LogsPage';
import HealthPage from './pages/HealthPage';
import SwaggerPage from './pages/SwaggerPage';
import PayerConsole from './pages/PayerConsole';
import PayeeConsole from './pages/PayeeConsole';

function App() {
  const isAuthenticated = !!localStorage.getItem('jwt');
  return (
    <Router>
      <nav style={{ padding: 16, background: '#f4f7fb', borderBottom: '1px solid #eee', marginBottom: 24 }}>
        <Link to="/payer" style={{ marginRight: 16 }}>Payer Console</Link>
        <Link to="/payee" style={{ marginRight: 16 }}>Payee Console</Link>
        <Link to="/admin" style={{ marginRight: 16 }}>Admin Dashboard</Link>
      </nav>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/admin" element={isAuthenticated ? <AdminDashboard /> : <Navigate to="/login" />} />
        <Route path="/admin/logs" element={isAuthenticated ? <LogsPage /> : <Navigate to="/login" />} />
        <Route path="/admin/health" element={isAuthenticated ? <HealthPage /> : <Navigate to="/login" />} />
        <Route path="/admin/api-docs" element={isAuthenticated ? <SwaggerPage /> : <Navigate to="/login" />} />
        <Route path="/payer" element={isAuthenticated ? <PayerConsole /> : <Navigate to="/login" />} />
        <Route path="/payee" element={isAuthenticated ? <PayeeConsole /> : <Navigate to="/login" />} />
        <Route path="*" element={<Navigate to={isAuthenticated ? '/admin' : '/login'} />} />
      </Routes>
    </Router>
  );
}

export default App;
