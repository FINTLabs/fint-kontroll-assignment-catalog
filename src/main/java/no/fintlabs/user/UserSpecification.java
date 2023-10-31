package no.fintlabs.user;

import no.fintlabs.assignment.Assignment;
import no.fintlabs.search.SearchCriteria;
import no.fintlabs.search.SearchOperation;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Objects;

public class UserSpecification implements Specification<User> {
    private final SearchCriteria searchCriteria;

    public UserSpecification(final SearchCriteria searchCriteria){
        super();
        this.searchCriteria = searchCriteria;
    }

    @Override
    public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        String strToSearch = searchCriteria.getValue().toString().toLowerCase();

        switch(Objects.requireNonNull(SearchOperation.getSimpleOperation(searchCriteria.getOperation()))){
            case CONTAINS:
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(searchCriteria.getFilterKey())), "%" + strToSearch + "%");

            case DOES_NOT_CONTAIN:
                return criteriaBuilder.notLike(criteriaBuilder.lower(root.get(searchCriteria.getFilterKey())), "%" + strToSearch + "%");

            case BEGINS_WITH:
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(searchCriteria.getFilterKey())), strToSearch + "%");

            case DOES_NOT_BEGIN_WITH:
                return criteriaBuilder.notLike(criteriaBuilder.lower(root.get(searchCriteria.getFilterKey())), strToSearch + "%");

            case ENDS_WITH:
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(searchCriteria.getFilterKey())), "%" + strToSearch);

            case DOES_NOT_END_WITH:
                return criteriaBuilder.notLike(criteriaBuilder.lower(root.get(searchCriteria.getFilterKey())), "%" + strToSearch);

            case EQUAL:
                if(searchCriteria.getFilterKey().equals("resourceRef")){
                    //System.out.println(searchCriteria.getValue());
                    return criteriaBuilder.equal(resourceJoin(root).<Long>get(searchCriteria.getFilterKey()), searchCriteria.getValue());
                }
                return criteriaBuilder.equal(root.get(searchCriteria.getFilterKey()), searchCriteria.getValue());

            case NOT_EQUAL:
                return criteriaBuilder.notEqual(root.get(searchCriteria.getFilterKey()), searchCriteria.getValue());

            case NUL:
                return criteriaBuilder.isNull(root.get(searchCriteria.getFilterKey()));

            case NOT_NULL:
                return criteriaBuilder.isNotNull(root.get(searchCriteria.getFilterKey()));

            case GREATER_THAN:
                return criteriaBuilder.greaterThan(root.<String> get(searchCriteria.getFilterKey()), searchCriteria.getValue().toString());

            case GREATER_THAN_EQUAL:
                return criteriaBuilder.greaterThanOrEqualTo(root.<String> get(searchCriteria.getFilterKey()), searchCriteria.getValue().toString());

            case LESS_THAN:
                return criteriaBuilder.lessThan(root.<String> get(searchCriteria.getFilterKey()), searchCriteria.getValue().toString());

            case LESS_THAN_EQUAL:
                return criteriaBuilder.lessThanOrEqualTo(root.<String> get(searchCriteria.getFilterKey()), searchCriteria.getValue().toString());
        }
        return null;
    }
    private Join<User, Assignment> resourceJoin(Root<User> root){
        return root.join("assignments");

    }
}
