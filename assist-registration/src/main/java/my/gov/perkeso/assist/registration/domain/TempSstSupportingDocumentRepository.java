package my.gov.perkeso.assist.registration.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TempSstSupportingDocumentRepository extends JpaRepository<TempSstSupportingDocument, Long> {

    List<TempSstSupportingDocument> findByTempSstInfoIdAndDeletedFalseOrderByIdAsc(Long tempSstInfoId);

    Optional<TempSstSupportingDocument> findByIdAndTempSstInfoIdAndDeletedFalse(Long id, Long tempSstInfoId);
}
