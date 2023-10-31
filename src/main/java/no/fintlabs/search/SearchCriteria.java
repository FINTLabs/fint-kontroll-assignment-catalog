package no.fintlabs.search;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {

    private String filterKey;
    private Object value;
    private String operation;
    private String dataOption;

    public SearchCriteria(String filterKey, String operation, Object value, String dataOption){
        super();
        this.filterKey = filterKey;
        this.operation = operation;
        this.value = value;
        this.dataOption = dataOption;
    }
}
