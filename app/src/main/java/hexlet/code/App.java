package hexlet.code;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class App {

    public static final int PORT = 7070;

    public static Javalin getApp() {
        return Javalin.create(javalinConfig -> {
                    JavalinThymeleaf.init(getTemplateEngine());
                })
                .get("/", ctx -> ctx.render("main.html"));
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
