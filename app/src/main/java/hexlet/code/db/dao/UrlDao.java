package hexlet.code.db.dao;

import hexlet.code.entity.Url;
import io.ebean.DB;
import io.ebean.PagedList;

public class UrlDao {
    public static Url getUrlById(Long id) {
        return DB.getDefault().find(Url.class, id);
    }

    public static Url getUrlByName(String name) {
        return DB.getDefault()
                .find(Url.class)
                .select("name")
                .where()
                .eq("name", name)
                .findOne();
    }

    public static Url createUrl(String name) {
        Url newUrl = new Url();
        newUrl.setName(name);
        DB.getDefault().save(newUrl);
        return getUrlByName(name);
    }

    public static PagedList<Url> getUrlPagedList(int page, int rowsPerPage) {
        return DB.getDefault()
                .find(Url.class)
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy("id ASC")
                .findPagedList();
    }
}
