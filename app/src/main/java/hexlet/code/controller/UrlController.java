package hexlet.code.controller;

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
        public static final int ROWS_PER_PAGE = 10;

        public static final Handler HANDLER = ctx -> {
            log.info("ListShowEndpoint int");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;

            PagedList<Url> pagedUrls = getUrlPagedList(page, ROWS_PER_PAGE);
            List<Url> urls = pagedUrls.getList();
            configurePages(ctx, pagedUrls);

            ctx.attribute("urls", urls);
            ctx.render("list.html");
            log.info("ListShowEndpoint out");
        };

        private static void configurePages(Context ctx, PagedList<Url> pagedUrls) {
            log.info("configurePages in");
            int lastPage = pagedUrls.getTotalPageCount() + 1;
            int currentPage = pagedUrls.getPageIndex() + 1;
            List<Integer> pages = IntStream
                    .range(1, lastPage)
                    .boxed()
                    .toList();
            ctx.attribute("pages", pages);
            ctx.attribute("currentPage", currentPage);
            log.info("configurePages out");
        }

        private static PagedList<Url> getUrlPagedList(int page, int rowsPerPage) {
            log.info("configurePages in");
            return DB.getDefault()
                    .find(Url.class)
                    .setFirstRow(page * rowsPerPage)
                    .setMaxRows(rowsPerPage)
                    .orderBy("id ASC")
                    .findPagedList();
        }
    }

    public static class CreateEndpoint {
        public static final Handler HANDLER = ctx -> {
            log.info("CreateEndpoint in");
            String urlInput = ctx.formParam("url");
            URL url = parseUrl(ctx, urlInput);
            if (url == null) {
                return;
            }
            String normalizedUrl = getNormalizedUrl(url);
            if (isUrlAlreadyExists(ctx, normalizedUrl)) {
                return;
            }
            createUrl(ctx, normalizedUrl);
            log.info("CreateEndpoint out");
        };

        private static void createUrl(Context ctx, String normalizedUrl) {
            log.info("createUrl in");
            Url newUrl = new Url();
            newUrl.setName(normalizedUrl);
            DB.getDefault().save(newUrl);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls");
            log.info("createUrl out");
        }

        private static boolean isUrlAlreadyExists(Context ctx, String normalizedUrl) {
            log.info("isUrlAlreadyExists in");
            Url url = DB.getDefault()
                    .find(Url.class)
                    .select("name")
                    .where()
                    .eq("name", normalizedUrl)
                    .findOne();
            if (url != null) {
                ctx.sessionAttribute("flash",
                        "Страница уже существует. Используй urls/%d чтобы получить информацию о ней."
                                .formatted(url.getId()));
                ctx.sessionAttribute("flash-type", "info");
                ctx.redirect("/");
                log.info("isUrlAlreadyExists out != null");
                return true;
            }
            log.info("isUrlAlreadyExists == null");
            return false;
        }

        private static String getNormalizedUrl(URL url) {
            log.info("getNormalizedUrl in");
            return String.format(
                    "%s://%s%s",
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort() == -1 ? "" : ":" + url.getPort()
            ).toLowerCase();
        }

        private static URL parseUrl(Context ctx, String urlInput) {
            log.info("parseUrl in");
            try {
                return new URL(urlInput);
            } catch (MalformedURLException e) {
                log.info("parseUrl catch");
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.sessionAttribute("flash-type", "danger");
                ctx.redirect("/");
                return null;
            }
        }
    }

    public static class SingleShowEndpoint {
        public static final Handler HANDLER = ctx -> {
            log.info("SingleShowEndpoint in");
            int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

            Url url = DB.getDefault()
                    .find(Url.class, id);

            if (url == null) {
                throw new NotFoundResponse();
            }

            ctx.attribute("url", url);
            ctx.render("single.html");
            log.info("SingleShowEndpoint out");
        };
    }

}
