package org.itmo.testing.lab2.integration;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import org.itmo.testing.lab2.controller.UserAnalyticsController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.javalin.Javalin;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserAnalyticsIntegrationTest {

    private Javalin app;
    private int port = 8000;

    @BeforeAll
    void setUp() {
        app = UserAnalyticsController.createApp();
        app.start(port);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @AfterAll
    void tearDown() {
        app.stop();
    }

    @Test
    @Order(1)
    @DisplayName("Тест регистрации пользователя")
    void testUserRegistration() {
        given()
                .queryParam("userId", "user1")
                .queryParam("userName", "Alice")
                .when()
                .post("/register")
                .then()
                .statusCode(200)
                .body(equalTo("User registered: true"));
    }

    @ParameterizedTest
    @Order(2)
    @DisplayName("Тест записи сессии")
    @CsvSource({"2025-01-01T10:10:10,2025-01-01T11:10:10"})
    void testRecordSession(String loginTime, String logoutTime) {
        given()
                .queryParam("userId", "user1")
                .queryParam("loginTime", loginTime)
                .queryParam("logoutTime", logoutTime)
                .when()
                .post("/recordSession")
                .then()
                .statusCode(200)
                .body(equalTo("Session recorded"));
    }

    @Test
    @Order(3)
    @DisplayName("Тест получения общего времени активности")
    void testGetTotalActivity() {
        given()
                .queryParam("userId", "user1")
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(200)
                .body(containsString("Total activity:"))
                .body(containsString("minutes"));
    }

    @ParameterizedTest
    @Order(4)
    @DisplayName("Тест post-запросов без параметров")
    @ValueSource(strings = {"/register", "/recordSession"})
    void testPostWithoutParams(String path) {
        given()
                .when()
                .post(path)
                .then()
                .statusCode(400)
                .body(containsString("Missing"));
    }

    @ParameterizedTest
    @Order(5)
    @DisplayName("Тест get-запросов без параметров")
    @ValueSource(strings = {"/totalActivity", "/inactiveUsers", "/monthlyActivity"})
    void testGetWithoutParams(String path) {
        given()
                .when()
                .get(path)
                .then()
                .statusCode(400)
                .body(containsString("Missing"));
    }

    @Test
    @Order(6)
    @DisplayName("Тест повторной регистрации пользователя")
    void testExistUserRegistration() {
        given()
                .queryParam("userId", "user1")
                .queryParam("userName", "Alice2")
                .when()
                .post("/register")
                .then()
                .statusCode(400)
                .body(equalTo("User already exists"));
    }

    @ParameterizedTest
    @Order(7)
    @DisplayName("Тест записи сессии с некорректным форматом даты")
    @CsvSource({"2024-10-02T00:00:00,0", "0,2024-10-02T00:00:00"})
    void testRecordSessionBadDateFormat(String loginTime, String logoutTime) {
        given()
                .queryParam("userId", "user1")
                .queryParam("loginTime", loginTime)
                .queryParam("logoutTime", logoutTime)
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data:"));
    }

    @Test
    @Order(8)
    @DisplayName("Тест получения неактивных пользователей с неверным форматом числа дней")
    void testGetInactiveUsersBadNumberFormat() {
        given()
                .queryParam("days", "fon")
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(400)
                .body(containsString("Invalid number format for days"));
    }

    @ParameterizedTest
    @Order(9)
    @DisplayName("Тест получения месячной активности пользователя с неверным форматом аргументом")
    @CsvSource({"user123,fon","no,2025-01"})
    void testGetMonthlyActivityBadFormat(String userId, String month) {
        given()
                .queryParam("userId", userId)
                .queryParam("month", month)
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data:"));
    }

    @ParameterizedTest
    @Order(9)
    @DisplayName("Тест получения месячной активности пользователя")
    @CsvSource({"2025-01"})
    void testGetMonthlyActivity(String month) {
        given()
                .queryParam("userId", "user1")
                .queryParam("month", month)
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(200);
    }

    @ParameterizedTest
    @Order(10)
    @DisplayName("Тест получения списка неактивных пользователей")
    @CsvSource({"1,[\"user1\"]","100000,[]"})
    void testGetInactiveUsers(String days, String expectedResponse) {
        given()
                .queryParam("days", days)
                .when()
                .get("/inactiveUsers")
                .then()
                .statusCode(200)
                .body(equalTo(expectedResponse));
    }

    @Test
    @Order(11)
    @DisplayName("Тест записи сессии для несуществующего пользователя")
    void testRecordSessionWithNonExistentUser() {
        given()
                .queryParam("userId", "user2")
                .queryParam("loginTime", LocalDateTime.now().toString())
                .queryParam("logoutTime", LocalDateTime.now().toString())
                .when()
                .post("/recordSession")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data:"));
    }

    @Test
    @Order(11)
    @DisplayName("Тест получения активности для несуществующего пользователя")
    void testTotalActivityWithNonExistentUser() {
        given()
                .queryParam("userId", "user2")
                .when()
                .get("/totalActivity")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data:"));
    }

    @Test
    @Order(11)
    @DisplayName("Тест месячной активности для несуществующего пользователя")
    void testMonthlyActivityWithNonExistentUser() {
        given()
                .queryParam("userId", "user2")
                .queryParam("month", "2025-01")
                .when()
                .get("/monthlyActivity")
                .then()
                .statusCode(400)
                .body(containsString("Invalid data:"));
    }


}
