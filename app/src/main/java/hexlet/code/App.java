package hexlet.code;

import hexlet.code.controller.UrlController;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class App {

    public static final int PORT = 7070;

    public static Javalin getApp() {
        Javalin javalin = Javalin
                .create(javalinConfig -> JavalinThymeleaf.init(getTemplateEngine()))
                .get("/", ctx -> ctx.render("main.html"));

        javalin.before(ctx -> ctx.attribute("ctx", ctx));

        javalin.routes(() -> path("urls", () -> {
            post(UrlController.CreateEndpoint.HANDLER);
            get(UrlController.ListShowEndpoint.HANDLER);
            path("{id}", () -> get(UrlController.SingleShowEndpoint.HANDLER));
        }));
        return javalin;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
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
