package hexlet.code.db.dao;

import hexlet.code.entity.Url;
import hexlet.code.entity.UrlCheck;
import io.ebean.DB;

import java.util.Map;

public class UrlCheckDao {
    public static void save(Url url, Integer statusCode, String title, String h1, String description) {
        UrlCheck newUrlCheck = new UrlCheck(statusCode, title, h1, description);
        url.getUrlChecks().add(newUrlCheck);
        url.save();
    }

    public static Map<Long, UrlCheck> getUrlChecksMap() {
        return DB.getDefault()
                .find(UrlCheck.class)
                .setMapKey("url.id")
                .select("*")
                .orderBy("created_at DESC")
                .findMap();
    }
}
