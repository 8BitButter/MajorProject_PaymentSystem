import React, { useEffect, useState } from 'react';
import { Box, Typography, Paper, CircularProgress, Chip, Stack } from '@mui/material';

interface HealthStatus {
  status: string;
  uptime: string;
  dbStatus: string;
  queueDepth: number;
  lastEvent: string;
}

const fetchHealth = async (jwt: string) => {
  const res = await fetch('/api/admin/health', {
    headers: { 'Authorization': 'Bearer ' + jwt }
  });
  if (!res.ok) throw new Error('Failed to fetch health');
  return await res.json();
};

const HealthPage: React.FC = () => {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const jwt = localStorage.getItem('jwt');
    if (!jwt) return;
    fetchHealth(jwt)
      .then(setHealth)
      .finally(() => setLoading(false));
  }, []);

  return (
    <Box minHeight="100vh" bgcolor="#f4f7fb" p={4}>
      <Paper elevation={3} sx={{ p: 3, mb: 3 }}>
        <Typography variant="h4" color="primary" fontWeight={700}>System Health</Typography>
        <Typography variant="subtitle1" color="text.secondary">Live system health, uptime, and metrics.</Typography>
      </Paper>
      {loading ? <CircularProgress /> : health && (
        <Stack spacing={2}>
          <Chip label={`Status: ${health.status}`} color={health.status === 'UP' ? 'success' : 'error'} sx={{ fontSize: 18, p: 2 }} />
          <Typography>Uptime: {health.uptime}</Typography>
          <Typography>Database: <b>{health.dbStatus}</b></Typography>
          <Typography>Queue Depth: <b>{health.queueDepth}</b></Typography>
          <Typography>Last Event: <b>{health.lastEvent}</b></Typography>
        </Stack>
      )}
    </Box>
  );
};

export default HealthPage;
