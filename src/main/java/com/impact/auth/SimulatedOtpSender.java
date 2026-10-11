package com.impact.auth;

import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class SimulatedOtpSender implements OtpSender {
    @Override
    public String sendOtp(String phone) {
        // Generate a 6-digit simulated OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        // In a real app we'd trigger an SMS API here.
        return otp;
    }
}