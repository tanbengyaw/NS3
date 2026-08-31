package my.gov.perkeso.assist.registration.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SequenceNoService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public long nextValue(final String sequenceName) {
        ensureSequence(sequenceName);
        jdbcTemplate.update("UPDATE registration.sequence_no SET next_val = next_val + 1 WHERE name = ?", sequenceName);
        final Long value = jdbcTemplate.queryForObject(
                "SELECT next_val FROM registration.sequence_no WHERE name = ?", Long.class, sequenceName);
        if (value == null) {
            throw new IllegalStateException("Sequence not found: " + sequenceName);
        }
        return value;
    }

    public String getEmployerCode8DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.EMPLOYER_CODE));
    }

    public String getEmployerCode8DigitSequenceNoByAreaCode(final String areaCode) {
        return String.format("%08d", nextValue(SequenceNames.employerCodeAreaCode(areaCode)));
    }

    public String getNewRegCase8DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.NEW_REG_CASE));
    }

    public String getUpdateC18DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.UPDATE_C1_CASE));
    }

    public String getUpdateC28DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.UPDATE_C2_CASE));
    }

    public String getUpdateAddEmployee8DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.UPDATE_ADD_NEW_EMPLOYEE_CASE));
    }

    public String getSsnCase8DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.UPDATE_SSN_CASE));
    }

    public String getSsnCase5DigitSequenceNo() {
        return String.format("%05d", nextValue(SequenceNames.SSN));
    }

    public String getSipUpdateC18DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.SIP_UPDATE_C1_CASE));
    }

    public String getSipUpdateC28DigitSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.SIP_UPDATE_C2_CASE));
    }

    public String getSsnForeignEmployeeSequenceNo() {
        return String.format("%08d", nextValue(SequenceNames.SSN_FOREIGN_EMPLOYEE));
    }

    public String getSmkNo() {
        return String.format("%08d", nextValue(SequenceNames.SMK_NO));
    }

    private void ensureSequence(final String sequenceName) {
        jdbcTemplate.update(
                "INSERT INTO registration.sequence_no (name, next_val) SELECT ?, 1 "
                        + "WHERE NOT EXISTS (SELECT 1 FROM registration.sequence_no WHERE name = ?)",
                sequenceName, sequenceName);
    }
}
