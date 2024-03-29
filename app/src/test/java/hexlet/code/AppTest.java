package hexlet.code;

import hexlet.code.db.dao.UrlCheckDao;
import hexlet.code.db.dao.UrlDao;
import hexlet.code.entity.Url;
import hexlet.code.entity.UrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    //use 0 to start at a random available port
    public static final int PORT = 0;

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
        app.start(PORT);
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

    @AfterEach
    void afterEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed.sql");
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void urlIndexTest() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void urlShowTest() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + 1).asString();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void urlStoreTest() {
        String inputUrl = "https://github.com";
        HttpResponse<?> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("url", inputUrl)
                .asString();

        assertThat(responsePost.getStatus()).isEqualTo(HttpStatus.FOUND);

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);

        Url actualUrl = database.find(Url.class)
                .select("name")
                .where()
                .eq("name", inputUrl)
                .findOne();

        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(inputUrl);
    }

    @Test
    void urlCheckStoreTest() {
        String url = mockServer.url("/").toString().replaceAll("/$", "");

        Unirest.post(baseUrl + "/urls")
                .field("url", url)
                .asEmpty();

        Url actualUrl = UrlDao.getUrlByName(url);

        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(url);

        Unirest.post(baseUrl + "/urls/" + actualUrl.getId() + "/checks")
                .asEmpty();

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls/" + actualUrl.getId())
                .asString();

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK);

        UrlCheck actualCheckUrl = UrlCheckDao.getUrlChecksMap().get(actualUrl.getId());

        assertThat(actualCheckUrl).isNotNull();
        assertThat(actualCheckUrl.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actualCheckUrl.getTitle()).isEqualTo("Test title");
        assertThat(actualCheckUrl.getH1()).isEqualTo("h1 example");
        assertThat(actualCheckUrl.getDescription()).contains("description text");
    }
}
