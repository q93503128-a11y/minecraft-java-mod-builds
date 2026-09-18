package dev.moonseungjun.openworldrpg.integration.api;

public interface IntegrationModule {
    String id();

    default void initialize() {
    }
}
