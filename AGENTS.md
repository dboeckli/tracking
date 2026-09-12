# AGENTS.md

## Projekt

`tracking` — Kafka/Spring-Boot-Tracking-Service (`dev.lydtech`), Schwesterprojekt von
[dispatch](https://github.com/dboeckli/dispatch). Konsumiert `dispatch.tracking`-Events und
speichert den Tracking-Status. Eigenes Helm-Chart unter `helm-charts/tracking/`.
Siehe README.md für Build-/Kubernetes-/Helm-Anleitung.

## Kommandos

| Zweck | Befehl |
|---|---|
| Format/Spotless prüfen | `./mvnw validate` |
| Build (ohne Docker/Start) | `./mvnw package -Dskip.docker.build=true -Dskip.start.stop.springboot=true` |
| Voll-Build inkl. Helm (lint/template/package) | `./mvnw clean install -Dskip.start.stop.springboot=true` |
| Tests | `./mvnw test` |

## Sandbox

- Kit: opencode-sandbox-kit (README → Sandbox). Sandbox-Quirk: vor jedem `./mvnw`
  `export npm_config_bin_links=false` (Spotless/prettier → EPERM im Mount).
- Maven-Auflösung nutzt bei Mount `C:\development\maven-repo:ro` den Host-Cache; GitHub-Packages-Zugriff
  (`github-maven`-Secret) läuft über den Kit-Proxy. Nur echte Maven-Builds sind repräsentativ
  (`mvn dependency:get` ignoriert settings-`<proxies>`).
- Formatting: shfmt `3.14.1` (Spotless `<shfmt>` + CI `mfinelli/setup-shfmt@v4`).

## Hinweise

- Registry-/Migrations-Entscheidungen: opencode-sandbox-kit Buchhaltung #44.
- Onboarding-Drehbuch: opencode-sandbox-kit #45 (dieses Projekt: Issue #101).
