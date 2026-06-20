package com.pact.auth;

import com.pact.auth.dto.AuthDtos.*;
import com.pact.common.ApiException;
import com.pact.member.Member;
import com.pact.member.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists."
            );
        }

        Member member = new Member();
        member.setEmail(request.email());
        member.setDisplayName(request.displayName());
        member.setPasswordHash(passwordEncoder.encode(request.password()));

        Member saved;
        try {
            saved = memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // A second request raced us between the existsByEmail check above and this
            // save — the unique constraint on email caught it. Surface the precise
            // error code instead of falling through to the generic CONFLICT handler.
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_REGISTERED",
                    "An account with this email already exists."
            );
        }

        String token = jwtService.generateToken(saved.getId());
        return new AuthResponse(token, toSummary(saved));
    }

    public AuthResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS",
                        "Invalid email or password."
                ));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "Invalid email or password."
            );
        }

        String token = jwtService.generateToken(member.getId());
        return new AuthResponse(token, toSummary(member));
    }

    private MemberSummary toSummary(Member member) {
        return new MemberSummary(member.getId(), member.getDisplayName(), member.getEmail());
    }
}