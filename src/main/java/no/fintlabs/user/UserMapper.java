package no.fintlabs.user;

public class UserMapper {
    public static User fromKontrollUser(KontrollUser kontrollUser) {
        return User.builder()
                .id(kontrollUser.getId())
                .userName(kontrollUser.getUserName())
                .identityProviderUserObjectId(kontrollUser.getIdentityProviderUserObjectId())
                .firstName(kontrollUser.getFirstName())
                .lastName(kontrollUser.getLastName())
                .userType(kontrollUser.getUserType())
                .organisationUnitId(kontrollUser.getMainOrganisationUnitId())
                .organisationUnitName(kontrollUser.getMainOrganisationUnitName())
                .status(kontrollUser.getStatus())
                .statusChanged(kontrollUser.getStatusChanged())
                .build();
    }
}
