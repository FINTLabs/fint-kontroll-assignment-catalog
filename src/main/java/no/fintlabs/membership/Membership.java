package no.fintlabs.membership;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
//@RequiredArgsConstructor
@Slf4j
@Entity
@Table(name="Assignment_memberships")
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
public class Membership {
    @Id
    private String id;
    private Long roleId;
    private Long memberId;
    private UUID identityProviderUserObjectId;
    private String memberStatus;
    private Date memberStatusChanged;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Membership that = (Membership) o;
        return Objects.equals(id, that.id) && Objects.equals(roleId, that.roleId) && Objects.equals(memberId, that.memberId) &&
               Objects.equals(identityProviderUserObjectId, that.identityProviderUserObjectId) &&
               Objects.equals(memberStatus, that.memberStatus) && Objects.equals(memberStatusChanged, that.memberStatusChanged);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roleId, memberId, identityProviderUserObjectId, memberStatus, memberStatusChanged);
    }
}



