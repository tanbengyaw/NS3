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
@Table(name = "temp_doc_mapping", schema = "base")
@Getter
@Setter
public class TempDocMapping extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "temp_doc_file_id", nullable = false)
    private Long tempDocFileId;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "table_pk_id")
    private Long tablePkId;

    @Column(name = "process_instance_id")
    private Long processInstanceId;

    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "individual_id")
    private Long individualId;

    @Column(name = "description", length = 500)
    private String description;
}
