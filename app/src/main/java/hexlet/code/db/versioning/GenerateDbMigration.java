package hexlet.code.db.versioning;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;
import java.io.IOException;

public class GenerateDbMigration {

    /**
     * Generate the DDL for the next DB migration.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        DbMigration dbMigration = DbMigration.create();
        dbMigration.addPlatform(Platform.H2, "h2");
        dbMigration.addPlatform(Platform.POSTGRES, "postgres");

        dbMigration.generateMigration();
    }
}
