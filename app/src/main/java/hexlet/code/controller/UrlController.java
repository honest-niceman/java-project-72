package hexlet.code.controller;

import hexlet.code.db.dao.UrlCheckDao;
import hexlet.code.db.dao.UrlDao;
import hexlet.code.entity.Url;
import hexlet.code.entity.UrlCheck;
import io.ebean.PagedList;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
public class UrlController {

    public static class ListShowEndpoint {
        public static final int ROWS_PER_PAGE = 10;

        public static final Handler HANDLER = ctx -> {
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
            log.debug("int page: %d.".formatted(page));

            PagedList<Url> pagedUrls = UrlDao.getUrlPagedList(page, ROWS_PER_PAGE);
            List<Url> urls = pagedUrls.getList();
            log.debug("List<Url> urls: %s.".formatted(pagedUrls));
            configurePages(ctx, pagedUrls);
            Map<Long, UrlCheck> urlChecks = UrlCheckDao.getUrlChecksMap();
            ctx.attribute("urls", urls);
            ctx.attribute("urlChecks", urlChecks);
            ctx.render("list.html");
        };

        private static void configurePages(Context ctx, PagedList<Url> pagedUrls) {
            int lastPage = pagedUrls.getTotalPageCount() + 1;
            int currentPage = pagedUrls.getPageIndex() + 1;
            List<Integer> pages = IntStream
                    .range(1, lastPage)
                    .boxed()
                    .toList();
            log.debug("lastPage: %d, currentPage: %d, List<Integer> pages: %s."
                    .formatted(lastPage, currentPage, pages));
            ctx.attribute("pages", pages);
            ctx.attribute("currentPage", currentPage);
        }
    }

    public static class CreateEndpoint {
        public static final Handler HANDLER = ctx -> {
            String urlInput = ctx.formParam("url");
            log.debug("String urlInput: %s.".formatted(urlInput));
            URL url = parseUrl(ctx, urlInput);
            log.debug("Url url: %s.".formatted(url));
            if (url == null) {
                return;
            }
            String normalizedUrl = getNormalizedUrl(url);
            log.debug("String normalizedUrl: %s.".formatted(normalizedUrl));
            if (isUrlAlreadyExists(ctx, normalizedUrl)) {
                return;
            }
            createUrl(ctx, normalizedUrl);
        };

        private static void createUrl(Context ctx, String normalizedUrl) {
            Url url = UrlDao.createUrl(normalizedUrl);
            log.debug("Url saved: %s.".formatted(url));
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect("/urls");
        }

        private static boolean isUrlAlreadyExists(Context ctx, String normalizedUrl) {
            Url url = UrlDao.getUrlByName(normalizedUrl);
            if (url != null) {
                log.debug("Url with such name: %s ,already exists.".formatted(normalizedUrl));
                ctx.sessionAttribute("flash",
                        "Страница уже существует. Используй urls/%d чтобы получить информацию о ней."
                                .formatted(url.getId()));
                ctx.sessionAttribute("flash-type", "info");
                ctx.redirect("/urls/" + url.getId(), HttpStatus.FOUND);
                return true;
            }
            log.debug("Url with name: %s doesn't exists yet.".formatted(normalizedUrl));
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
                return new URI(urlInput).toURL();
            } catch (URISyntaxException | MalformedURLException e) {
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.sessionAttribute("flash-type", "danger");
                ctx.redirect("/");
                return null;
            }
        }
    }

    public static class SingleShowEndpoint {
        public static final Handler HANDLER = ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
            log.debug("int id: %d.".formatted(id));
            Url url = UrlDao.getUrlById(id);
            log.debug("Url url: %s.".formatted(url));
            if (url == null) {
                throw new NotFoundResponse();
            }
            ctx.attribute("url", url);
            ctx.render("single.html");
        };
    }

    public static class UrlChecksEndpoint {
        public static final Handler HANDLER = ctx -> {
            Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
            log.debug("int id: %d.".formatted(id));
            Url url = UrlDao.getUrlById(id);
            log.debug("Url url: %s.".formatted(url));
            if (url == null) {
                throw new NotFoundResponse();
            }
            parseUrl(ctx, url);

            ctx.redirect("/urls/" + url.getId());
        };

        private static void parseUrl(Context ctx, Url url) {
            try {
                HttpResponse<String> response = Unirest.get(url.getName()).asString();
                Document doc = Jsoup.parse(response.getBody());

                createUrlCheck(url, response, doc);

                ctx.sessionAttribute("flash", "Страница успешно проверена");
                ctx.sessionAttribute("flash-type", "success");
            } catch (UnirestException e) {
                ctx.sessionAttribute("flash", "Некорректный адрес");
                ctx.sessionAttribute("flash-type", "danger");
            } catch (Exception e) {
                ctx.sessionAttribute("flash", e.getMessage());
                ctx.sessionAttribute("flash-type", "danger");
            }
        }

        private static void createUrlCheck(Url url, HttpResponse<String> response, Document doc) {
            Integer statusCode = response.getStatus();
            String title = doc.title();
            Element h1Element = doc.selectFirst("h1");
            String h1 = h1Element == null ? "" : h1Element.text();
            Element descriptionElement = doc.selectFirst("meta[name=description]");
            String description = descriptionElement == null ? "" : descriptionElement.attr("content");

            log.debug("Integer statusCode: %d, String title: %s, String h1: %s, String description: %s"
                    .formatted(statusCode, title, h1, description));
            UrlCheckDao.save(url, statusCode, title, h1, description);
        }
    }
}
