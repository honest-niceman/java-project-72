package hexlet.code;

import hexlet.code.entity.Url;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static MockWebServer mockServer;

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(777);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();

        mockServer = new MockWebServer();
        MockResponse mockedResponse = new MockResponse().setBody(readFixture("index.html"));
        mockServer.enqueue(mockedResponse);
        mockServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockServer.shutdown();
    }

    @Nested
    class RootTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Nested
    class UrlTest {

        @AfterEach
        void afterEach() {
            database.script().run("/truncate.sql");
            database.script().run("/seed.sql");
        }

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + 1).asString();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        void testStore() {
            String inputUrl = "https://github.com/honest-niceman";
            HttpResponse<?> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputUrl)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            assertThat(response.getStatus()).isEqualTo(200);

            Url actualUrl = database.find(Url.class)
                    .select("name")
                    .where()
                    .eq("name", inputUrl)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputUrl);
        }
    }
}