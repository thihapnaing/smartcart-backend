package nus.iss.smartcart.backend.admin.service;

import nus.iss.smartcart.backend.admin.dto.AdminAccountDto;
import nus.iss.smartcart.backend.admin.dto.CreateAdminRequest;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;

// AUTHOR: Htet Nandar(Grace)

/** "As an admin, I want to create other admin accounts so that the platform never needs a
 * public admin sign-up form. */
@Service
public class AdminAccountService {

    // AUTHOR: Htet Nandar(Grace)
    // Every admin-invited account starts on a freshly generated, single-use random password
    // (never a shared/fixed literal - a static value here would be a standing credential anyone
    // reading the source could use) and is forced to replace it via POST /api/auth/change-password
    // before it's usable for anything else (mustChangePassword below).
    //
    // TgenerateTemporaryPassword() below.
    private static final String RANDOM_VALUE_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GENERATED_VALUE_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public AdminAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    // AUTHOR: Htet Nandar(Grace)
    /** Every admin account, newest first - backs the "Admins" management page. */
    @Transactional
    public List<AdminAccountDto> getAllAdmins() {
        currentUserProvider.getCurrentAdmin();

        return userRepository.findByRole(UserRole.ADMIN).stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AdminAccountDto createAdmin(CreateAdminRequest request) {
        currentUserProvider.getCurrentAdmin();

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already registered");
        }

        String temporaryPassword = generateTemporaryPassword();

        User admin = new User();
        admin.setUsername(request.getUsername());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(temporaryPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setMustChangePassword(true);

        User saved = userRepository.save(admin);

        // Only the create response ever carries the temporary password - see the DTO's javadoc.
        // It's generated fresh above and never persisted or logged in plaintext anywhere else.
        return toDto(saved).toBuilder()
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(GENERATED_VALUE_LENGTH);
        for (int i = 0; i < GENERATED_VALUE_LENGTH; i++) {
            password.append(RANDOM_VALUE_ALPHABET.charAt(SECURE_RANDOM.nextInt(RANDOM_VALUE_ALPHABET.length())));
        }
        return password.toString();
    }

    private AdminAccountDto toDto(User admin) {
        return AdminAccountDto.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .email(admin.getEmail())
                .status(admin.getStatus() != null ? admin.getStatus().name() : null)
                .createdAt(admin.getCreatedAt())
                .mustChangePassword(Boolean.TRUE.equals(admin.getMustChangePassword()))
                .build();
    }
}
