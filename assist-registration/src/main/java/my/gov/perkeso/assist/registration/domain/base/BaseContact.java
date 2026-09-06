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
@Table(name = "contact", schema = "base")
@Getter
@Setter
public class BaseContact extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_type_id", nullable = false)
    private Long contactTypeId;

    @Column(name = "contact_value", length = 50)
    private String contactValue;

    @Column(name = "ext", length = 10)
    private String ext;

    @Column(name = "calling_code", length = 10)
    private String callingCode;

    @Column(name = "address_id")
    private Long addressId;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "table_pk_id")
    private Long tablePkId;
}
