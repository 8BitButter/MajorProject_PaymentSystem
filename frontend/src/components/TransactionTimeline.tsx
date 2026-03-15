import React, { useEffect, useState, useCallback } from 'react';
import { Box, Typography, Stepper, Step, StepLabel, CircularProgress } from '@mui/material';
import { useWebSocket } from '../hooks/useWebSocket';

interface TimelineEvent {
  toState: string;
  createdAt: string;
  reason?: string;
}

interface Props {
  transactionId: string;
}

const TransactionTimeline: React.FC<Props> = ({ transactionId }) => {
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchEvents = useCallback(() => {
    setLoading(true);
    setError('');
    fetch(`/api/payments/${transactionId}/events`, {
      headers: { 'Authorization': 'Bearer ' + localStorage.getItem('jwt') }
    })
      .then(res => res.ok ? res.json() : Promise.reject(res))
      .then(setEvents)
      .catch(() => setError('Failed to load timeline'))
      .finally(() => setLoading(false));
  }, [transactionId]);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  // WebSocket for real-time timeline updates
  useWebSocket('ws://localhost:8080/ws/transactions', (event) => {
    if (event && event.transactionId === transactionId) {
      fetchEvents();
    }
  });

  if (loading) return <CircularProgress size={24} />;
  if (error) return <Typography color="error">{error}</Typography>;
  if (!events.length) return <Typography>No timeline events found.</Typography>;

  return (
    <Box>
      <Stepper activeStep={events.length - 1} orientation="vertical">
        {events.map((ev, idx) => (
          <Step key={ev.toState + ev.createdAt} completed={idx < events.length - 1}>
            <StepLabel>
              <Typography fontWeight={600}>{ev.toState}</Typography>
              <Typography variant="caption" color="text.secondary">{new Date(ev.createdAt).toLocaleString()}</Typography>
              {ev.reason && <Typography variant="body2">{ev.reason}</Typography>}
            </StepLabel>
          </Step>
        ))}
      </Stepper>
    </Box>
  );
};

export default TransactionTimeline;
