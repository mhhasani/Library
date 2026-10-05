# Keycloak for the library realm: providers (offline captcha, MFA condition, strict
# password change, bcrypt migration), the Persian theme and the realm definition are
# baked into an optimized image, so the target server needs no internet access.

FROM maven:3.9-eclipse-temurin-21 AS extensions
WORKDIR /build
COPY extensions/pom.xml .
COPY extensions/src ./src
RUN mvn package --batch-mode --no-transfer-progress

FROM quay.io/keycloak/keycloak:26.8.0 AS builder
ENV KC_DB=postgres \
    KC_HTTP_RELATIVE_PATH=/auth \
    KC_HEALTH_ENABLED=true
COPY --from=extensions /build/target/library-keycloak-extensions.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:26.8.0
COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY realm/library-realm.json /opt/keycloak/data/import/library-realm.json
USER 1000
EXPOSE 8080
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
# The realm is imported on first start only (existing realms are left untouched)
CMD ["start", "--optimized", "--import-realm"]
