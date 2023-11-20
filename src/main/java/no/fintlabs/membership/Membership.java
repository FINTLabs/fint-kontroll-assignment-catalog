package no.fintlabs.membership;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import java.util.UUID;

@Getter
@Setter
@ToString
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
}