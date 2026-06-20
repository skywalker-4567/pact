package com.pact.squad;

import com.pact.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InviteCodeGenerator {

    // Uppercase alphanumeric, excluding 0, O, 1, I, L
    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final int MAX_RETRIES = 5;

    private final SecureRandom random = new SecureRandom();
    private final SquadRepository squadRepository;

    public InviteCodeGenerator(SquadRepository squadRepository) {
        this.squadRepository = squadRepository;
    }

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String code = generateCode();
            if (!squadRepository.existsByInviteCode(code)) {
                return code;
            }
        }

        throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INVITE_CODE_GENERATION_FAILED",
                "Could not generate a unique invite code. Please try again."
        );
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}