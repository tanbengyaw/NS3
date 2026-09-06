package my.gov.perkeso.assist.registration.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "doc_file", schema = "base")
@Getter
@Setter
public class BaseDocFile extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_doc_type_id", nullable = false)
    private Long refDocTypeId;

    @Column(name = "is_uploaded", nullable = false)
    private boolean uploaded;

    @Column(name = "doc_name", length = 100)
    private String docName;

    @Column(name = "doc_uri", length = 255)
    private String docUri;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
