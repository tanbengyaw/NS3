package my.gov.perkeso.assist.core.infrastructure.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchParameters {

    private final String searchType;
    private final String searchValue;
    private final Long officeId;
    private final Integer offset;
    private final Integer limit;

    public static SearchParameters forEmployers(final String searchType, final String searchValue, final Long officeId,
            final Integer offset, final Integer limit) {
        return SearchParameters.builder().searchType(searchType).searchValue(searchValue).officeId(officeId)
                .offset(offset != null ? offset : 0).limit(limit != null ? limit : 20).build();
    }
}
