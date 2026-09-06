package my.gov.perkeso.assist.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "staff_user_role", schema = "identity", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_user_role", columnNames = { "staff_user_id", "role_code" }) })
@Getter
@Setter
public class StaffUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private StaffUser staffUser;

    @Column(name = "role_code", nullable = false, length = 30)
    private String roleCode;
}
