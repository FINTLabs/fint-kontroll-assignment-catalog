package no.fintlabs.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fintlabs.search.SearchCriteria;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto{
    private List<SearchCriteria> searchCriteriaList;
    private String dataOption;
}
