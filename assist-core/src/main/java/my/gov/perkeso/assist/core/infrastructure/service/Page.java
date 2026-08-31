package my.gov.perkeso.assist.core.infrastructure.service;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Page<T> {

    private final int totalFilteredRecords;
    private final List<T> pageItems;
}
