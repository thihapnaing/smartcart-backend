package nus.iss.smartcart.backend.admin.service;

// AUTHOR: Htet Nandar(Grace)

import nus.iss.smartcart.backend.admin.dto.AdminAccountDto;
import nus.iss.smartcart.backend.admin.dto.CreateAdminRequest;
import nus.iss.smartcart.backend.exception.ForbiddenException;
import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.model.UserStatus;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers the admin guard (getCurrentAdmin() called before anything else - a stray customer or
 * merchant JWT that somehow reaches this far must never be able to create an admin account),
 * the duplicate email/username checks, and that a created account always comes out ADMIN/ACTIVE
 * with its password encoded rather than stored as-is.
 */
@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private AdminAccountService adminAccountService;

    private CreateAdminRequest validRequest() {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("newadmin");
        request.setEmail("newadmin@smartcart.demo");
        return request;
    }

    @Test
    void createAdmin_callerNotAnAdmin_throwsForbiddenExceptionBeforeTouchingUserRepository() {
        when(currentUserProvider.getCurrentAdmin()).thenThrow(new ForbiddenException("Not authenticated."));

        // request is built outside the lambda so assertThrows only wraps the single call under
        // test (Sonar S5778 - a lambda passed to assertThrows should contain exactly one
        // invocation that could throw, not this one plus validRequest()).
        CreateAdminRequest request = validRequest();
        assertThrows(ForbiddenException.class, () -> adminAccountService.createAdmin(request));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createAdmin_emailAlreadyRegistered_throwsIllegalArgumentException() {
        CreateAdminRequest request = validRequest();
        when(userRepository.existsByEmail("newadmin@smartcart.demo")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> adminAccountService.createAdmin(request));
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void createAdmin_usernameAlreadyRegistered_throwsIllegalArgumentException() {
        CreateAdminRequest request = validRequest();
        when(userRepository.existsByEmail("newadmin@smartcart.demo")).thenReturn(false);
        when(userRepository.existsByUsername("newadmin")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> adminAccountService.createAdmin(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdmin_validRequest_savesEncodedRandomPasswordWithAdminRoleAndForcesChange() {
        CreateAdminRequest request = validRequest();
        when(userRepository.existsByEmail("newadmin@smartcart.demo")).thenReturn(false);
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        User saved = new User();
        saved.setId(42L);
        saved.setUsername("newadmin");
        saved.setEmail("newadmin@smartcart.demo");
        saved.setRole(UserRole.ADMIN);
        saved.setStatus(UserStatus.ACTIVE);
        saved.setMustChangePassword(true);
        saved.setCreatedAt(LocalDateTime.of(2026, Month.AUGUST, 16, 10, 0));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(userCaptor.capture())).thenReturn(saved);
        ArgumentCaptor<String> encodedPlaintextCaptor = ArgumentCaptor.forClass(String.class);

        AdminAccountDto result = adminAccountService.createAdmin(request);

        User persisted = userCaptor.getValue();
        assertEquals("newadmin", persisted.getUsername());
        assertEquals("newadmin@smartcart.demo", persisted.getEmail());
        assertEquals("hashed-password", persisted.getPassword());
        assertEquals(UserRole.ADMIN, persisted.getRole());
        assertEquals(UserStatus.ACTIVE, persisted.getStatus());
        assertEquals(Boolean.TRUE, persisted.getMustChangePassword());

        assertEquals(42L, result.getId());
        assertEquals("newadmin", result.getUsername());
        assertEquals("newadmin@smartcart.demo", result.getEmail());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(saved.getCreatedAt(), result.getCreatedAt());
        assertTrue(result.isMustChangePassword());

        // The temporary password is freshly generated per admin (never a fixed/shared literal),
        // long enough to resist guessing, and the exact same value that was actually encoded and
        // persisted - not some unrelated string.
        verify(passwordEncoder).encode(encodedPlaintextCaptor.capture());
        assertEquals(encodedPlaintextCaptor.getValue(), result.getTemporaryPassword());
        assertTrue(result.getTemporaryPassword().matches("[A-Za-z0-9]{12}"));
    }

    @Test
    void createAdmin_calledTwice_generatesADifferentTemporaryPasswordEachTime() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAccountDto first = adminAccountService.createAdmin(validRequest());
        AdminAccountDto second = adminAccountService.createAdmin(validRequest());

        assertNotEquals(first.getTemporaryPassword(), second.getTemporaryPassword());
    }

    @Test
    void getAllAdmins_callerNotAnAdmin_throwsForbiddenExceptionBeforeTouchingUserRepository() {
        when(currentUserProvider.getCurrentAdmin()).thenThrow(new ForbiddenException("Not authenticated."));

        assertThrows(ForbiddenException.class, () -> adminAccountService.getAllAdmins());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getAllAdmins_returnsEveryAdminNewestFirstWithNoTemporaryPassword() {
        User older = new User();
        older.setId(1L);
        older.setUsername("firstadmin");
        older.setEmail("firstadmin@smartcart.demo");
        older.setStatus(UserStatus.ACTIVE);
        older.setMustChangePassword(false);
        older.setCreatedAt(LocalDateTime.of(2026, Month.JANUARY, 1, 9, 0));

        User newer = new User();
        newer.setId(2L);
        newer.setUsername("secondadmin");
        newer.setEmail("secondadmin@smartcart.demo");
        newer.setStatus(UserStatus.ACTIVE);
        newer.setMustChangePassword(true);
        newer.setCreatedAt(LocalDateTime.of(2026, Month.AUGUST, 16, 10, 0));

        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(List.of(older, newer));

        List<AdminAccountDto> result = adminAccountService.getAllAdmins();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertTrue(result.get(0).isMustChangePassword());
        assertEquals(1L, result.get(1).getId());
        assertFalse(result.get(1).isMustChangePassword());
        assertNull(result.get(0).getTemporaryPassword());
    }
}
