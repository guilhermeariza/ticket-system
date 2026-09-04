import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../components/AuthContext';
import { Container, Typography, Button, Box, Paper, Grid } from '@mui/material';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const EventDetailsPage = () => {
  const { id } = useParams();
  const [event, setEvent] = useState(null);
  const { token } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchEvent = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/events/${id}`, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        });
        if (!response.ok) {
          throw new Error('Failed to fetch event');
        }
        const data = await response.json();
        setEvent(data);
      } catch (error) {
        console.error('Error fetching event:', error);
      }
    };

    if (token) {
      fetchEvent();
    }
  }, [id, token]);

  if (!token) {
    return <Typography>Please log in to view event details.</Typography>;
  }

  if (!event) {
    return <Typography>Loading...</Typography>;
  }

  return (
    <Container maxWidth="md">
      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          {event.name}
        </Typography>
        <Typography variant="body1" gutterBottom>
          {event.description}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Date: {new Date(event.date).toLocaleString()}
        </Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Location: {event.location}
        </Typography>

        <Box sx={{ mt: 4 }}>
          <Typography variant="h5" component="h2" gutterBottom>
            Ticket Types
          </Typography>
          <Grid container spacing={2}>
            {event.ticketTypes.map((ticketType) => (
              <Grid item xs={12} sm={6} md={4} key={ticketType.id}>
                <Paper sx={{ p: 2, display: 'flex', flexDirection: 'column', height: '100%' }}>
                  <Typography variant="h6">{ticketType.name}</Typography>
                  <Typography variant="body1">Price: ${ticketType.price.toFixed(2)}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Available: {ticketType.availableQuantity}
                  </Typography>
                  <Box sx={{ flexGrow: 1 }} />
                  <Button
                    variant="contained"
                    onClick={() => navigate(`/buy-ticket/${event.id}/${ticketType.id}`)}
                    sx={{ mt: 2 }}
                  >
                    Buy Ticket
                  </Button>
                </Paper>
              </Grid>
            ))}
          </Grid>
        </Box>
      </Paper>
    </Container>
  );
};

export default EventDetailsPage;
