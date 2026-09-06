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
@Table(name = "temp_address", schema = "base")
@Getter
@Setter
public class TempAddress extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "address_line_1", length = 100)
    private String addressLine1;

    @Column(name = "address_line_2", length = 100)
    private String addressLine2;

    @Column(name = "address_line_3", length = 100)
    private String addressLine3;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "postcode", length = 10)
    private String postcode;

    @Column(name = "po_box", length = 30)
    private String poBox;

    @Column(name = "locked_bag", length = 30)
    private String lockedBag;

    @Column(name = "wdt", length = 30)
    private String wdt;

    @Column(name = "address_type_id")
    private Long addressTypeId;

    @Column(name = "table_name", length = 100)
    private String tableName;

    @Column(name = "table_pk_id")
    private Long tablePkId;
}
