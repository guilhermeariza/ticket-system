import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../components/AuthContext';
import { AppBar, Toolbar, Typography, Button } from '@mui/material';

const Navbar = () => {
  const { isAuthenticated, logout } = useAuth();

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component={NavLink} to="/" sx={{ flexGrow: 1, color: 'inherit', textDecoration: 'none' }}>
          TicketSystem
        </Typography>
        <Button color="inherit" component={NavLink} to="/">Home</Button>
        <Button color="inherit" component={NavLink} to="/events">Events</Button>
        {isAuthenticated() ? (
          <>
            <Button color="inherit" component={NavLink} to="/orders">My Orders</Button>
            <Button color="inherit" onClick={logout}>Logout</Button>
          </>
        ) : (
          <>
            <Button color="inherit" component={NavLink} to="/login">Login</Button>
            <Button color="inherit" component={NavLink} to="/register">Register</Button>
          </>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default Navbar;

