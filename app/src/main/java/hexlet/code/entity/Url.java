package hexlet.code.entity;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;

/**
 * Url entity.
 */
@Getter
@Setter
@Entity
public final class Url extends Model {
    @Id
    private Long id;

    private String name;

    @WhenCreated
    private Instant createdAt;
}
