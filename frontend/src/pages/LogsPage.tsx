import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, CircularProgress, TextField, Stack } from '@mui/material';

interface LogEntry {
  timestamp: string;
  level: string;
  user?: string;
  action: string;
  message: string;
}

const fetchLogs = async (jwt: string) => {
  const res = await fetch('/api/admin/logs', {
    headers: { 'Authorization': 'Bearer ' + jwt }
  });
  if (!res.ok) throw new Error('Failed to fetch logs');
  return await res.json();
};

const LogsPage: React.FC = () => {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    const jwt = localStorage.getItem('jwt');
    if (!jwt) return;
    fetchLogs(jwt)
      .then(setLogs)
      .finally(() => setLoading(false));
  }, []);

  const filteredLogs = logs.filter(
    l => l.action.toLowerCase().includes(filter.toLowerCase()) ||
         l.message.toLowerCase().includes(filter.toLowerCase()) ||
         (l.user || '').toLowerCase().includes(filter.toLowerCase())
  );

  return (
    <Box minHeight="100vh" bgcolor="#f4f7fb" p={4}>
      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h4" color="primary" fontWeight={700}>Audit Logs</Typography>
        <Typography variant="subtitle1" color="text.secondary">Centralized logs and audit trail for all actions.</Typography>
      </Paper>
      <Stack direction="row" spacing={2} mb={2}>
        <TextField label="Search logs" value={filter} onChange={e => setFilter(e.target.value)} size="small" />
      </Stack>
      {loading ? <CircularProgress /> : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Timestamp</TableCell>
                <TableCell>Level</TableCell>
                <TableCell>User</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Message</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredLogs.length === 0 ? (
                <TableRow><TableCell colSpan={5}>No logs found.</TableCell></TableRow>
              ) : filteredLogs.map((log, idx) => (
                <TableRow key={idx}>
                  <TableCell>{new Date(log.timestamp).toLocaleString()}</TableCell>
                  <TableCell>{log.level}</TableCell>
                  <TableCell>{log.user || '-'}</TableCell>
                  <TableCell>{log.action}</TableCell>
                  <TableCell>{log.message}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
};

export default LogsPage;
