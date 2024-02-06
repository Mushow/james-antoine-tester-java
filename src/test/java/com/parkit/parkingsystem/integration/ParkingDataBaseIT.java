package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();

    private static ParkingSpotDAO parkingSpotDAO;

    private static TicketDAO ticketDAO;

    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("123XYZ");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();

        assertTrue(ticketDAO.getNbTicket("123XYZ") > 0);
        assertNotEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR));
    }

    @Test
    public void testParkingLotExit() throws Exception {
        testParkingACar();
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("123XYZ");

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        Ticket initialTicket = new Ticket();
        initialTicket.setId(1);
        initialTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        initialTicket.setVehicleRegNumber("123XYZ");
        initialTicket.setPrice(0);
        initialTicket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        initialTicket.setOutTime(null);
        ticketDAO.saveTicket(initialTicket);
        Ticket ticketSaved = ticketDAO.getTicket("123XYZ");
        ticketSaved.setOutTime(new Date(System.currentTimeMillis()));
        ticketDAO.updateTicket(ticketSaved);

        parkingService.processExitingVehicle();

        assertNotNull(ticketDAO.getTicket("123XYZ").getOutTime());
        assertNotEquals(0, ticketSaved.getPrice());
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        // First ticket
        Ticket firstTicket = new Ticket();
        firstTicket.setId(1);
        firstTicket.setParkingSpot(parkingSpot);
        firstTicket.setVehicleRegNumber("ABCDEF");
        firstTicket.setPrice(0);
        firstTicket.setInTime(new Date(System.currentTimeMillis() - (3 * 60 * 60 * 1000)));
        firstTicket.setOutTime(new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000)));
        ticketDAO.saveTicket(firstTicket);
        parkingSpotDAO.updateParking(firstTicket.getParkingSpot());
        parkingSpot.setAvailable(true);
        parkingSpotDAO.updateParking(parkingSpot);

        // Second Ticket
        Ticket secondTicket = new Ticket();
        secondTicket.setId(2);
        secondTicket.setParkingSpot(parkingSpot);
        secondTicket.setVehicleRegNumber("ABCDEF");
        secondTicket.setPrice(0);
        secondTicket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        secondTicket.setOutTime(null);
        ticketDAO.saveTicket(secondTicket);
        parkingSpotDAO.updateParking(secondTicket.getParkingSpot());

        parkingService.processExitingVehicle();

        double inHour = ticketDAO.getTicket(secondTicket.getVehicleRegNumber()).getInTime().getTime();
        double outHour = ticketDAO.getTicket(secondTicket.getVehicleRegNumber()).getOutTime().getTime();
        double duration = (outHour - inHour) / (1000 * 60 * 60);
        double expectedPrice = 0.95 * duration * Fare.CAR_RATE_PER_HOUR;
        double price = ticketDAO.getTicket(secondTicket.getVehicleRegNumber()).getPrice();

        assertNotNull(ticketDAO.getTicket(secondTicket.getVehicleRegNumber()).getOutTime());
        assertEquals(expectedPrice, price, 0.01);
    }
}