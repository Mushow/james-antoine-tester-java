package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inMillis = ticket.getInTime().getTime();
        long outMillis = ticket.getOutTime().getTime();

        double durationInMinutes = (outMillis - inMillis) / 1000.0 / 60.0;

        if(durationInMinutes <= 30){
            ticket.setPrice(0);
        }else{
            // Appliquer le tarif existant si plus de 30 minutes
            // On s'assure de recalculer la durée pour correspondre à la tarrification avec getTime (en milis)
            switch (ticket.getParkingSpot().getParkingType()){
                case CAR: {
                    ticket.setPrice((durationInMinutes / 60) * Fare.CAR_RATE_PER_HOUR);
                    break;
                }
                case BIKE: {
                    ticket.setPrice((durationInMinutes / 60) * Fare.BIKE_RATE_PER_HOUR);
                    break;
                }
                default: throw new IllegalArgumentException("Unknown Parking Type");
            }
        }
    }
}

