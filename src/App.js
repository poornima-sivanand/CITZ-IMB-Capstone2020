import React from 'react';
import Header from './Header.js';
import Footer from './Footer.js';
import Navbar from './Navbar.js';
import FetchLatestNews from './FetchLatestNews.js';
import 'bootstrap/dist/css/bootstrap.min.css';
//import './App.css';

function App() {
  return (
    <div>
      <Header />
      <Navbar />
      <FetchLatestNews />
      <Footer />
     
    </div>
  );
}

export default App;
