package kr.moonseungjun.earthtostars.ship.domain;

public final class ShipPermissionPolicy {
    private ShipPermissionPolicy() {
    }

    public static boolean allows(CrewRole role, ShipPermission permission) {
        return switch (role) {
            case OWNER, CREW -> true;
            case GUEST -> permission == ShipPermission.INTERIOR_ACCESS;
        };
    }
}
