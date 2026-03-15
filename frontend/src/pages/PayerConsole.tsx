import React, { useState } from 'react';
import { Box, Paper, Typography, TextField, Button, Alert, Stack } from '@mui/material';

const PayerConsole: React.FC = () => {
  const [clientRequestId, setClientRequestId] = useState('demo-req-' + Math.floor(Math.random() * 10000));
  const [payerVpa, setPayerVpa] = useState('payer@issuer');
  const [payeeVpa, setPayeeVpa] = useState('payee@acquirer');
  const [amount, setAmount] = useState('120.00');
  const [mpin, setMpin] = useState('1111');
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setResult(null);
    setLoading(true);
    try {
      const jwt = localStorage.getItem('jwt');
      const res = await fetch('/api/payments/push', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + jwt
        },
        body: JSON.stringify({ clientRequestId, payerVpa, payeeVpa, amount: parseFloat(amount), mpin })
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error || 'Payment failed');
      } else {
        setResult(data);
      }
    } catch (err) {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box minHeight="100vh" bgcolor="#f4f7fb" p={4}>
      <Paper sx={{ p: 3, mb: 3, maxWidth: 500, mx: 'auto' }}>
        <Typography variant="h4" color="primary" fontWeight={700} mb={2}>Payer Console</Typography>
        <Typography variant="subtitle1" color="text.secondary" mb={2}>Initiate a push payment and watch the transaction flow.</Typography>
        <form onSubmit={handleSubmit}>
          <Stack spacing={2}>
            <TextField label="Client Request ID" value={clientRequestId} onChange={e => setClientRequestId(e.target.value)} required />
            <TextField label="Payer VPA" value={payerVpa} onChange={e => setPayerVpa(e.target.value)} required />
            <TextField label="Payee VPA" value={payeeVpa} onChange={e => setPayeeVpa(e.target.value)} required />
            <TextField label="Amount" value={amount} onChange={e => setAmount(e.target.value)} required type="number" inputProps={{ step: '0.01' }} />
            <TextField label="MPIN" value={mpin} onChange={e => setMpin(e.target.value)} required type="password" />
            <Button type="submit" variant="contained" color="primary" disabled={loading}>Start Push Payment</Button>
          </Stack>
        </form>
        {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        {result && (
          <Alert severity="success" sx={{ mt: 2 }}>
            Payment sent! Transaction ID: <b>{result.transactionId}</b><br />
            State: <b>{result.state}</b>
          </Alert>
        )}
      </Paper>
    </Box>
  );
};

export default PayerConsole;
