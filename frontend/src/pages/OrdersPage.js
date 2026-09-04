import React, { useState, useEffect } from 'react';
import { useAuth } from '../components/AuthContext';
import { Container, Typography, CircularProgress, Box, Paper, List, ListItem, ListItemText, Divider } from '@mui/material';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const OrdersPage = () => {
  const { token, isAuthenticated } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchOrders = async () => {
      if (!isAuthenticated()) {
        setLoading(false);
        setError('You must be logged in to view your orders.');
        return;
      }

      try {
        const response = await fetch(`${API_BASE_URL}/api/orders`, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        setOrders(data);
      } catch (error) {
        console.error("Error fetching orders:", error);
        setError('Failed to load orders.');
      } finally {
        setLoading(false);
      }
    };

    if (token) {
      fetchOrders();
    }
  }, [token, isAuthenticated]);

  if (loading) {
    return (
      <Container maxWidth="md">
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (error) {
    return (
      <Container maxWidth="md">
        <Typography variant="h6" color="error" align="center" sx={{ mt: 4 }}>
          {error}
        </Typography>
      </Container>
    );
  }

  return (
    <Container maxWidth="md">
      <Typography variant="h4" component="h1" gutterBottom sx={{ mt: 4, mb: 4 }}>
        My Orders
      </Typography>
      {orders.length === 0 ? (
        <Typography variant="h6" align="center">No orders found.</Typography>
      ) : (
        <List>
          {orders.map((order) => (
            <Paper key={order.id} sx={{ mb: 3, p: 2 }}>
              <ListItem alignItems="flex-start">
                <ListItemText
                  primary={
                    <Typography variant="h6">
                      Order ID: {order.id} - Status: {order.status}
                    </Typography>
                  }
                  secondary={
                    <>
                      <Typography component="span" variant="body2" color="text.primary">
                        Total: ${order.totalAmount.toFixed(2)}
                      </Typography>
                      <br />
                      <Typography component="span" variant="body2" color="text.secondary">
                        Created At: {new Date(order.createdAt).toLocaleString()}
                      </Typography>
                      <br />
                      <Typography component="span" variant="body2" color="text.primary">
                        Items:
                      </Typography>
                      <List dense disablePadding>
                        {order.items.map((item) => (
                          <ListItem key={item.id}>
                            <ListItemText
                              primary={`Ticket Type ID: ${item.ticketTypeId} - Quantity: ${item.quantity} - Unit Price: $${item.unitPrice.toFixed(2)}`}
                            />
                          </ListItem>
                        ))}
                      </List>
                    </>
                  }
                />
              </ListItem>
              <Divider component="li" />
            </Paper>
          ))}
        </List>
      )}
    </Container>
  );
};

export default OrdersPage;
