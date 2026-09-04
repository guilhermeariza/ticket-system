import React from 'react';
import { useAuth } from '../components/AuthContext';
import { Container, Typography, Box } from '@mui/material';

const HomePage = () => {
  const { isAuthenticated } = useAuth();

  return (
    <Container maxWidth="md">
      <Box sx={{ marginTop: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Typography component="h1" variant="h4" gutterBottom>
          Welcome to the Ticket System!
        </Typography>
        {isAuthenticated() ? (
          <Typography variant="h6" color="text.secondary">
            You are logged in. Explore events or manage your profile.
          </Typography>
        ) : (
          <Typography variant="h6" color="text.secondary">
            Please log in or register to continue.
          </Typography>
        )}
      </Box>
    </Container>
  );
};

export default HomePage;
