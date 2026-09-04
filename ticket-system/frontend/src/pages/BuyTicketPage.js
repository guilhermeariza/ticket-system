import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../components/AuthContext';
import { Container, Typography, TextField, Button, Box, Paper } from '@mui/material';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const BuyTicketPage = () => {
  const { eventId, ticketTypeId } = useParams();
  const [quantity, setQuantity] = useState(1);
  const { token } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!token) {
      alert('You must be logged in to purchase tickets.');
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ ticketTypeId: parseInt(ticketTypeId), quantity: parseInt(quantity) }),
      });

      if (!response.ok) {
        throw new Error('Failed to purchase tickets');
      }

      alert('Tickets purchased successfully!');
      navigate(`/events/${eventId}`);
    } catch (error) {
      console.error('Error purchasing tickets:', error);
      alert('Failed to purchase tickets.');
    }
  };

  return (
    <Container maxWidth="xs">
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Buy Tickets
        </Typography>
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
          <TextField
            margin="normal"
            required
            fullWidth
            name="quantity"
            label="Quantity"
            type="number"
            id="quantity"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            InputProps={{
              inputProps: { min: 1 },
            }}
          />
          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}
          >
            Purchase
          </Button>
        </Box>
      </Paper>
    </Container>
  );
};

export default BuyTicketPage;
