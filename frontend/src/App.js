import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './components/AuthContext';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import EventsPage from './pages/EventsPage';
import EventDetailsPage from './pages/EventDetailsPage';
import BuyTicketPage from './pages/BuyTicketPage';
import OrdersPage from './pages/OrdersPage';

function App() {
  return (
    <AuthProvider>
      <Navbar />
      <div className="container">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/events/:id" element={<EventDetailsPage />} />
          <Route path="/buy-ticket/:eventId/:ticketTypeId" element={<BuyTicketPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          {/* Add more routes as needed */}
        </Routes>
      </div>
    </AuthProvider>
  );
}

export default App;