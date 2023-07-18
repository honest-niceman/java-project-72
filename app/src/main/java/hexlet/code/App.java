package hexlet.code;

import hexlet.code.controller.UrlController;
import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.util.Properties;

import static io.javalin.apibuilder.ApiBuilder.*;

public class App {

    public static final int PORT = 7070;
    public static String DB_NAME;

    private static String getDatabase() {
        Properties prop = new Properties();
        try {
            prop.load(App.class.getClassLoader().getResourceAsStream("application.yaml"));
            return prop.getProperty("dbSchema").equals("${APP_ENV}") ? "db" : prop.getProperty("dbSchema");
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return "db";
        }
    }

    public static Javalin getAppAndInitializeDb() {
        init();
        Javalin javalin = Javalin.create(javalinConfig -> {
                    JavalinThymeleaf.configure(getTemplateEngine());
                })
                .get("/", ctx -> ctx.render("main.html"));

        javalin.before(ctx -> {
            ctx.attribute("ctx", ctx);
        });

        javalin.routes(() -> path("urls", () -> {
            post(UrlController.CreateEndpoint.handler);
            get(UrlController.ListShowEndpoint.handler);
            path("{id}", () -> {
                get(UrlController.SingleShowEndpoint.handler);
            });
        }));
        return javalin;
    }

    public static void init() {
        DB_NAME = getDatabase();
    }

    public static void main(String[] args) {
        Javalin app = getAppAndInitializeDb();
        app.start(PORT);
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("/templates/");
        templateEngine.addTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());

        return templateEngine;
    }
}
