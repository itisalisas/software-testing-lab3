package org.itmo.testing.lab2.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserStatusServiceTest {

    private UserAnalyticsService userAnalyticsService;
    private UserStatusService userStatusService;

    @BeforeAll
    void setUp() {
        userAnalyticsService = mock(UserAnalyticsService.class);
        userStatusService = new UserStatusService(userAnalyticsService);
    }

    @ParameterizedTest
    @DisplayName("Тест получения статуса пользователя")
    @CsvSource({"0,Inactive", "30,Inactive", "59,Inactive", "60,Active", "90,Active", "119,Active", "120,Highly active", "200,Highly active"})
    public void testGetUserStatus(long activityRime, String expectedStatus) {

        when(userAnalyticsService.getTotalActivityTime("user123")).thenReturn(activityRime);

        String status = userStatusService.getUserStatus("user123");

        assertEquals(expectedStatus, status);
    }

    @ParameterizedTest
    @DisplayName("Тест получения даты последнего сеанса пользователя")
    @MethodSource
    public void testGetUserLastSessionDate(List<UserAnalyticsService.Session> userSessions, Optional<String> expectedLastDate) {
        when(userAnalyticsService.getUserSessions("user123")).thenReturn(userSessions);

        Optional<String> lastDate = userStatusService.getUserLastSessionDate("user123");

        assertEquals(expectedLastDate, lastDate);
    }

    private static Stream<Arguments> testGetUserLastSessionDate() {
        UserAnalyticsService.Session lastSession = new UserAnalyticsService.Session(LocalDateTime.now().minusHours(1),
                LocalDateTime.now());
        UserAnalyticsService.Session session = new UserAnalyticsService.Session(LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(2));
        return Stream.of(
                Arguments.of(Named.of("Пустой список сеансов", List.of()), Optional.empty()),
                Arguments.of(Named.of("Список из одного сеанса", List.of(lastSession)), Optional.of(lastSession.getLogoutTime().toLocalDate().toString())),
                Arguments.of(Named.of("Список из двух сеансов, упорядочены", List.of(session, lastSession)), Optional.of(lastSession.getLogoutTime().toLocalDate().toString())),
                Arguments.of(Named.of("Список из двух сеансов, неупорядочены", List.of(lastSession, session)), Optional.of(lastSession.getLogoutTime().toLocalDate().toString()))
        );
    }

}
