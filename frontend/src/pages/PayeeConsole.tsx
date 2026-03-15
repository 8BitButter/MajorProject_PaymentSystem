import React, { useState } from 'react';
import { Box, Paper, Typography, TextField, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Stack } from '@mui/material';

const PayeeConsole: React.FC = () => {
  const [payeeVpa, setPayeeVpa] = useState('payee@acquirer');
  const [transactions, setTransactions] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchTransactions = async () => {
    setLoading(true);
    const jwt = localStorage.getItem('jwt');
    const res = await fetch(`/api/admin/dashboard?userId=${encodeURIComponent(payeeVpa)}`, {
      headers: { 'Authorization': 'Bearer ' + jwt }
    });
    const data = await res.json();
    setTransactions(data.recentTransactions || []);
    setLoading(false);
  };

  return (
    <Box minHeight="100vh" bgcolor="#f4f7fb" p={4}>
      <Paper sx={{ p: 3, mb: 3, maxWidth: 700, mx: 'auto' }}>
        <Typography variant="h4" color="primary" fontWeight={700} mb={2}>Payee Console</Typography>
        <Typography variant="subtitle1" color="text.secondary" mb={2}>View incoming payments for your VPA.</Typography>
        <Stack direction="row" spacing={2} mb={2}>
          <TextField label="Payee VPA" value={payeeVpa} onChange={e => setPayeeVpa(e.target.value)} />
          <Button variant="contained" onClick={fetchTransactions} disabled={loading}>Refresh</Button>
        </Stack>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Transaction ID</TableCell>
                <TableCell>Payer</TableCell>
                <TableCell>Amount</TableCell>
                <TableCell>State</TableCell>
                <TableCell>Source</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {transactions.length === 0 ? (
                <TableRow><TableCell colSpan={5}>No transactions found.</TableCell></TableRow>
              ) : transactions.map((tx: any) => (
                <TableRow key={tx.transactionId}>
                  <TableCell title={tx.transactionId}>{String(tx.transactionId).slice(0, 12)}...</TableCell>
                  <TableCell>{tx.payerVpa}</TableCell>
                  <TableCell>{tx.amount}</TableCell>
                  <TableCell>{tx.state}</TableCell>
                  <TableCell>{tx.source}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );
};

export default PayeeConsole;
