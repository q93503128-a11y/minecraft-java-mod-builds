package kr.moonseungjun.anomalousclasses.client;

/** Client-only return context for the class-management screen flow. */
final class ClassManagementNavigation {
    private static boolean returnToProfile;

    private ClassManagementNavigation() {}

    static void enterFromProfile() { returnToProfile = true; }
    static void enterFromWorld() { returnToProfile = false; }
    static boolean shouldReturnToProfile() { return returnToProfile; }
    static void clearReturnToProfile() { returnToProfile = false; }
}
