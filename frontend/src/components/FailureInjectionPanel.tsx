import React, { useEffect, useState } from 'react';
import { Paper, Typography, Stack, Button, Chip, CircularProgress } from '@mui/material';

interface Scenario {
  name: string;
  enabled: boolean;
}

const fetchScenarios = async (jwt: string) => {
  const res = await fetch('/api/admin/failure-scenarios', {
    headers: { 'Authorization': 'Bearer ' + jwt }
  });
  if (!res.ok) throw new Error('Failed to fetch scenarios');
  return await res.json();
};

const setScenario = async (jwt: string, scenario: string, enable: boolean) => {
  await fetch(`/api/admin/failure-scenarios/${scenario}/${enable ? 'enable' : 'disable'}`, {
    method: 'POST',
    headers: { 'Authorization': 'Bearer ' + jwt }
  });
};

const FailureInjectionPanel: React.FC = () => {
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const jwt = localStorage.getItem('jwt');
    if (!jwt) return;
    fetchScenarios(jwt)
      .then(data => setScenarios(Object.entries(data).map(([name, enabled]) => ({ name, enabled }))))
      .finally(() => setLoading(false));
  }, []);

  const handleToggle = async (name: string, enable: boolean) => {
    const jwt = localStorage.getItem('jwt');
    if (!jwt) return;
    await setScenario(jwt, name, enable);
    setScenarios(scenarios => scenarios.map(s => s.name === name ? { ...s, enabled: enable } : s));
  };

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h6" mb={2}>Failure Injection</Typography>
      {loading ? <CircularProgress /> : (
        <Stack direction="row" spacing={2} flexWrap="wrap">
          {scenarios.map(s => (
            <Chip
              key={s.name}
              label={s.name}
              color={s.enabled ? 'error' : 'default'}
              onClick={() => handleToggle(s.name, !s.enabled)}
              variant={s.enabled ? 'filled' : 'outlined'}
              sx={{ mb: 1, fontWeight: 600 }}
            />
          ))}
        </Stack>
      )}
    </Paper>
  );
};

export default FailureInjectionPanel;
