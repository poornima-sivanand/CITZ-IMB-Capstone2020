import React from 'react';
import './Navbar.css';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';

function Navbar() {

    return (
      // Contents of the Navbar
      <Container fluid id="navbar">
          <Row>
            <Col xs={1}></Col>
            <Col xs={3} padding-left="0px"><p class="panel">Ministries</p></Col>
            <Col xs={3}><p class="panel">Sectors</p></Col>
            <Col xs={3}><p class="panel">Favorites</p></Col>
            <Col xs={2}>
                <img id="settings-button" src={require("./includes/settings-button.svg")} alt="settings-button" title="settings button" />
            </Col>
          </Row>
      </Container>
    );
  }
  
  export default Navbar;