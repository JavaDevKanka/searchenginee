package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "site")
public class SiteEntity {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime statusTime;

    @Column(columnDefinition="TEXT")
    private String lastError;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String url;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;
    @OneToMany(mappedBy = "siteId", cascade = CascadeType.ALL)
    private Set<PageEntity> pages;

    public SiteEntity(Status status, LocalDateTime statusTime, String url, String name) {
        this.status = status;
        this.statusTime = statusTime;
        this.url = url;
        this.name = name;
    }

    public SiteEntity(Status status, LocalDateTime statusTime, String lastError) {
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
    }

    public SiteEntity(Status status, LocalDateTime statusTime) {
        this.status = status;
        this.statusTime = statusTime;
    }
}
