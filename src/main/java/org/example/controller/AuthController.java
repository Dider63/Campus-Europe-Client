package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final AccountRepository accountRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager, AccountRepository accountRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/csrf")
    public ApiResponse<Map<String, String>> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) token = (CsrfToken) request.getAttribute("_csrf");
        if (token == null) throw new IllegalStateException("CSRF token was not made available by Spring Security.");
        return ApiResponse.ok("CSRF token ready.", Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Applicant account created. You can now sign in.", RegisterResponse.from(authService.register(body))));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest body,
                                                                  HttpServletRequest request) {
        Object username = body.get("username");
        Object password = body.get("password");
        if (username == null || password == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Username and password are required."));
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(String.valueOf(username).trim(), String.valueOf(password)));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            HttpSession existingSession = request.getSession(false);
            if (existingSession != null) request.changeSessionId();
            request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            Map<String, Object> profile = accountRepository.profile(CurrentUser.id());
            return ResponseEntity.ok(ApiResponse.ok("Signed in successfully.", profile));
        } catch (InternalAuthenticationServiceException ex) {
            SecurityContextHolder.clearContext();
            log.error("Authentication could not load account data. Check Oracle connectivity, schema permissions, and USER_ACCOUNT / USER_ROLE objects.", ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Authentication service cannot access account data. Check the Oracle connection and application logs."));
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid username or password, or the account is not active."));
        }
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest body) {
        authService.changePassword(body);
        return ApiResponse.ok("Password changed successfully.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ApiResponse.ok("Signed out successfully.");
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok("Current account.", accountRepository.profile(CurrentUser.id()));
    }
}
