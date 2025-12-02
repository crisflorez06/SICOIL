# Repository Guidelines

## Project Structure & Module Organization
- **Backend**: Java Spring Boot under `src/main/java` with REST controllers, services, repositories, and DTOs. Resources (Liquibase, config, templates) reside in `src/main/resources`. Tests live in `src/test/java`.
- **Frontend**: Angular app in `frontend/` (components under `src/app`, shared assets in `frontend/public`).
- **Ops**: Docker setup via `Dockerfile` and `docker-compose.yml`; SQL helpers in `sql/`.

## Build, Test, and Development Commands
- `mvn -DskipTests package`: builds backend jar quickly when you only need compilation.
- `mvn test`: runs the Spring/JUnit test suite; keep it clean before pushing.
- `npm install` then `npm run start -- --host 0.0.0.0` inside `frontend/`: installs dependencies and starts the Angular dev server.
- `npm run test`: executes Angular unit tests with Karma/Jest (depending on config).

## Coding Style & Naming Conventions
- **Java**: follow Spring defaults—classes `PascalCase`, methods/fields `camelCase`, 4-space indentation, Lombok for boilerplate. Prefer constructor injection and keep controllers thin.
- **TypeScript/HTML**: Angular style guide—components `kebab-case` filenames (`ventas.component.ts`), services ending in `.service.ts`, 2-space indentation.
- Run `mvn fmt:format` or IDE auto-format for Java and `npm run lint` for Angular when available.

## Testing Guidelines
- Backend tests use JUnit 5 + Spring Boot test slices; name classes `*Test` and place under matching package in `src/test/java`.
- Frontend tests follow Angular’s `.spec.ts` convention. Keep coverage meaningful around service logic and shared pipes.
- Before raising PRs, execute `mvn test` and `npm run test` to avoid regressions.

## Commit & Pull Request Guidelines
- Commit messages follow the conventional short imperative style (e.g., `Add venta cancellation flow`). Group related changes per commit.
- PRs should describe motivation, summarize major changes, mention affected modules, and link Jira/GitHub issues. Include screenshots for UI tweaks and note any manual verification steps or remaining TODOs.
