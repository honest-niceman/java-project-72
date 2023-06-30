package hexlet.code.controller;

import hexlet.code.App;
import hexlet.code.entity.Url;
import io.ebean.DB;
import io.ebean.PagedList;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class UrlController {

    public static class ListShowEndpoint {
        public static final Handler handler = ctx -> {
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
            int rowsPerPage = 10;

            PagedList<Url> pagedUrls = getUrlPagedList(page, rowsPerPage);
            List<Url> urls = pagedUrls.getList();
            configurePages(ctx, pagedUrls);

            ctx.attribute("urls", urls);
            ctx.render("list.html");
        };

        private static void configurePages(Context ctx, PagedList<Url> pagedUrls) {
            int lastPage = pagedUrls.getTotalPageCount() + 1;
            int currentPage = pagedUrls.getPageIndex() + 1;
            List<Integer> pages = IntStream
                    .range(1, lastPage)
                    .boxed()
                    .toList();
            ctx.attribute("pages", pages);
            ctx.attribute("currentPage", currentPage);
        }

        private static PagedList<Url> getUrlPagedList(int page, int rowsPerPage) {
            return DB.byName(App.DB_NAME)
                    .find(Url.class)
                    .setFirstRow(page * rowsPerPage)
                    .setMaxRows(rowsPerPage)
                    .orderBy("id ASC")
                    .findPagedList();
        }
    }

    public static class CreateEndpoint {
        public static final Handler handler = ctx -> {
            String urlInput = ctx.formParam("url");
            URL url = parseUrl(ctx, urlInput);
            if (url == null) return;
            String normalizedUrl = getNormalizedUrl(url);
            if (isUrlAlreadyExists(ctx, normalizedUrl)) return;
            createUrl(ctx, normalizedUrl);
        };

        private static void createUrl(Context ctx, String normalizedUrl) {
            Url newUrl = new Url();
            newUrl.setName(normalizedUrl);
            DB.byName(App.DB_NAME).save(newUrl);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls");
        }

        private static boolean isUrlAlreadyExists(Context ctx, String normalizedUrl) {
            Url url = DB.byName(App.DB_NAME)
                    .find(Url.class)
                    .select("name")
                    .where()
                    .eq("name", normalizedUrl)
                    .findOne();
            if (url != null) {
                ctx.sessionAttribute("flash",
                        "Страница уже существует. Используй urls/%d чтобы получить информацию о ней.".formatted(url.getId()));
                ctx.sessionAttribute("flash-type", "info");
                ctx.redirect("/");
                return true;
            }
            return false;
        }

        private static String getNormalizedUrl(URL url) {
            return String.format(
                    "%s://%s%s",
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort() == -1 ? "" : ":" + url.getPort()
            ).toLowerCase();
        }

        private static URL parseUrl(Context ctx, String urlInput) {
            try {
                return new URL(urlInput);
            } catch (MalformedURLException e) {
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.sessionAttribute("flash-type", "danger");
                ctx.redirect("/");
                return null;
            }
        }
    }

    public static class SingleShowEndpoint {
        public static Handler handler = ctx -> {
            int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

            Url url = DB.byName(App.DB_NAME)
                    .find(Url.class, id);

            if (url == null) {
                throw new NotFoundResponse();
            }

            ctx.attribute("url", url);
            ctx.render("single.html");
        };
    }

}
