import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Grid, Card, CardContent, CircularProgress, Alert, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Chip, Stack, AppBar, Toolbar, IconButton, Menu, MenuItem, Button } from '@mui/material';
import { Line, Bar } from 'react-chartjs-2';
import AccountCircle from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import TransactionTimeline from '../components/TransactionTimeline';
import { useWebSocket } from '../hooks/useWebSocket';
import FailureInjectionPanel from '../components/FailureInjectionPanel';
import ExportButton from '../components/ExportButton';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend);

const fetchDashboard = async (jwt: string) => {
  const res = await fetch('/api/admin/dashboard', {
    headers: { 'Authorization': 'Bearer ' + jwt }
  });
  if (res.status === 401) {
    localStorage.removeItem('jwt');
    window.location.href = '/login';
    return null;
  }
  return await res.json();
};

const kpiCard = (label: string, value: string | number, color: string) => (
  <Card sx={{ minWidth: 180, borderTop: `4px solid ${color}` }}>
    <CardContent>
      <Typography variant="subtitle2" color="text.secondary">{label}</Typography>
      <Typography variant="h5" fontWeight={700}>{value}</Typography>
    </CardContent>
  </Card>
);

const AdminDashboard: React.FC = () => {
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedTxId, setSelectedTxId] = useState<string | null>(null);

  // Real-time updates every 10 seconds + WebSocket
  useEffect(() => {
    let mounted = true;
    const jwt = localStorage.getItem('jwt');
    if (!jwt) return;
    const fetchAndSet = () => fetchDashboard(jwt).then(d => { if (mounted) setData(d); });
    fetchAndSet();
    setLoading(false);
    const interval = setInterval(fetchAndSet, 10000);
    return () => { mounted = false; clearInterval(interval); };
  }, []);

  // WebSocket for real-time transaction updates
  useWebSocket('ws://localhost:8080/ws/transactions', (event) => {
    // If event is a transaction update, refresh dashboard data
    if (event && event.transactionId) {
      const jwt = localStorage.getItem('jwt');
      if (jwt) fetchDashboard(jwt).then(setData);
    }
  });

  const handleMenu = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleClose = () => {
    setAnchorEl(null);
  };
  const handleLogout = () => {
    localStorage.removeItem('jwt');
    window.location.href = '/login';
  };

  if (loading) return <Box display="flex" justifyContent="center" alignItems="center" minHeight="60vh"><CircularProgress /></Box>;
  if (error) return <Alert severity="error">{error}</Alert>;
  if (!data) return null;

  const kpi = [
    { label: 'Total Transactions', value: data.totalTransactions, color: '#0f766e' },
    { label: 'Last 24 Hours', value: data.transactionsLast24Hours, color: '#2563eb' },
    { label: 'Success Rate', value: `${data.successRatePercent}%`, color: '#15803d' },
    { label: 'Completed GMV', value: data.completedAmount, color: '#b91c1c' },
  ];

  const chartData = data.kpiCharts || {};
  const stateBreakdown = data.stateBreakdown || {};
  const recentTransactions = data.recentTransactions || [];

  return (
    <Box minHeight="100vh" bgcolor="#f4f7fb">
      <AppBar position="static" color="default" elevation={1} sx={{ mb: 3 }}>
        <Toolbar>
          <Typography variant="h6" color="primary" sx={{ flexGrow: 1, fontWeight: 700 }}>DIPS Admin Dashboard</Typography>
          <IconButton color="inherit" onClick={handleMenu} size="large">
            <AccountCircle />
          </IconButton>
          <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleClose}>
            <MenuItem onClick={handleLogout}><LogoutIcon fontSize="small" sx={{ mr: 1 }} />Logout</MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>
      <Box p={4}>
        <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
          <Typography variant="h4" color="primary" fontWeight={700}>Admin Dashboard</Typography>
          <Typography variant="subtitle1" color="text.secondary">Enterprise KPIs, charts, and controls.</Typography>
        </Paper>
        <Grid container spacing={2} mb={3}>
          {kpi.map(k => (
            <Grid item key={k.label} xs={12} sm={6} md={3}>{kpiCard(k.label, k.value, k.color)}</Grid>
          ))}
        </Grid>
        <Grid container spacing={3} mb={3}>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="subtitle2">Transaction Volume (7d)</Typography>
              <Line
                data={{
                  labels: chartData.txVolume?.labels || [],
                  datasets: [{
                    label: 'Transactions',
                    data: chartData.txVolume?.values || [],
                    borderColor: '#0f766e',
                    backgroundColor: 'rgba(15, 118, 110, 0.1)',
                    fill: true,
                  }]
                }}
                options={{ responsive: true, plugins: { legend: { display: false } } }}
              />
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="subtitle2">Error Rate (7d)</Typography>
              <Bar
                data={{
                  labels: chartData.errorRate?.labels || [],
                  datasets: [{
                    label: 'Error Rate',
                    data: chartData.errorRate?.values || [],
                    backgroundColor: '#b91c1c',
                  }]
                }}
                options={{ responsive: true, plugins: { legend: { display: false } } }}
              />
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="subtitle2">GMV (7d)</Typography>
              <Line
                data={{
                  labels: chartData.gmv?.labels || [],
                  datasets: [{
                    label: 'GMV',
                    data: chartData.gmv?.values || [],
                    borderColor: '#15803d',
                    backgroundColor: 'rgba(21, 128, 61, 0.1)',
                    fill: true,
                  }]
                }}
                options={{ responsive: true, plugins: { legend: { display: false } } }}
              />
            </Paper>
          </Grid>
        </Grid>
        <Grid container spacing={3} mb={3}>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="subtitle2" mb={1}>State Breakdown</Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap">
                {Object.entries(stateBreakdown).map(([state, count]) => (
                  <Chip key={state} label={`${state}: ${count}`} color="primary" variant="outlined" sx={{ mb: 1 }} />
                ))}
              </Stack>
            </Paper>
          </Grid>
          <Grid item xs={12} md={6}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="subtitle2" mb={1}>Recent Transactions</Typography>
              <ExportButton data={recentTransactions} filename="transactions.xlsx" label="Export Transactions" />
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Transaction ID</TableCell>
                      <TableCell>Payer</TableCell>
                      <TableCell>Payee</TableCell>
                      <TableCell>Amount</TableCell>
                      <TableCell>State</TableCell>
                      <TableCell>Source</TableCell>
                      <TableCell>Processing (ms)</TableCell>
                      <TableCell>Timeline</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recentTransactions.length === 0 ? (
                      <TableRow><TableCell colSpan={8}>No transactions found.</TableCell></TableRow>
                    ) : recentTransactions.map((tx: any) => (
                      <TableRow key={tx.transactionId}>
                        <TableCell title={tx.transactionId}>{String(tx.transactionId).slice(0, 12)}...</TableCell>
                        <TableCell>{tx.payerVpa}</TableCell>
                        <TableCell>{tx.payeeVpa}</TableCell>
                        <TableCell>{tx.amount}</TableCell>
                        <TableCell>{tx.state}</TableCell>
                        <TableCell>{tx.source}</TableCell>
                        <TableCell>{tx.processingTimeMs}</TableCell>
                        <TableCell>
                          <Button size="small" variant="outlined" onClick={() => setSelectedTxId(tx.transactionId)}>
                            View
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Grid>
        </Grid>
        <FailureInjectionPanel />
        {selectedTxId && (
          <Paper sx={{ p: 3, mt: 3 }}>
            <Typography variant="h6" mb={2}>Transaction Timeline</Typography>
            <TransactionTimeline transactionId={selectedTxId} />
            <Button sx={{ mt: 2 }} onClick={() => setSelectedTxId(null)}>Close</Button>
          </Paper>
        )}
      </Box>
    </Box>
  );
};

export default AdminDashboard;
