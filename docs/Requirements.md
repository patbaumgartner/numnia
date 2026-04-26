# Software Requirements Specification (SRS) - Numnia

## Project

Numnia - web-based 3D math learning game for children of primary-school age.

## Document Control

| Field | Value |
| --- | --- |
| Document type | Software Requirements Specification (SRS) |
| Template basis | ISO/IEC/IEEE 29148 (adapted for this project) |
| Version | 1.1 (final, released for use-case phase) |
| Status | Released - no open product or process decisions |
| Last updated | 2026-04-26 |
| Scope | Product requirements for Release 1 through Release 5 |
| Language | English (project documentation); Swiss High German with umlauts (ä, ö, ü), without sharp s, for in-product UI |

Change log:

| Version | Date | Change |
| --- | --- | --- |
| 1.0 | 2026-04-26 | Baseline SRS created. |
| 1.1 | 2026-04-26 | Pedagogical assumptions (section 6.1.2) added; mastery thresholds, BDD/TDD/craftsmanship NFRs (7.6) and toolchain (4.1) anchored; creature names with variable endings (FR-CRE-007); Decision Log extended with D-13; open follow-up items converted into operational work packages with owner and due date; in-product language consistently Swiss High German with umlauts, without sharp s. |
| 1.2 | 2026-04-26 | Translated to English. Project documentation language is now English; in-product UI language remains Swiss High German per NFR-I18N. |

## Reading Rules

This document is the binding basis for planning, architecture, implementation, testing and subsequent use cases.

Normative terms:

- MUST: binding
- SHOULD: strongly recommended; deviations require justification
- MAY: optional

---

## 1. Purpose and Scope

### 1.1 Purpose

This SRS describes the complete functional and non-functional requirements for a web-based 3D math learning game for children of primary-school age.

### 1.2 Scope

In scope:

- Math learning with addition, subtraction, multiplication and division with remainder
- Number range up to 1,000,000
- 3D worlds, creatures, avatar gamification
- Turn-based multiplayer (synchronous/asynchronous)
- Parent area and school mode
- Data protection, child safety, moderation
- Operations in Switzerland

Out of current scope:

- US launch
- Offline mode
- Free-text communication for children
- School SSO (SwissEdu-ID, Microsoft Entra) in initial scope
- Monetization, advertising, in-app purchases

### 1.3 Goal of this Baseline Document

The structure is deliberately designed to be use-case-ready, so that in the next step use cases can be developed systematically without restructuring requirements.

---

## 2. Product Overview

### 2.1 Product Vision

The product is a web-based 3D math learning game in which children practice mathematical skills, collect creatures, unlock worlds and compete in safe multiplayer formats.

### 2.2 Product Classification

Not a pure times-table game, but a holistic math learning game covering all four basic arithmetic operations.

### 2.3 Target Groups

- Children (ages 7-12)
- Parents
- Teachers
- School admins
- Operator/admin team

### 2.4 Success Criteria (Business Outcome)

The product is considered successful when:

- Children regularly practice voluntarily
- Measurable learning progress can be demonstrated
- Parents and schools have confidence in data protection and security
- Multiplayer remains fair and friendly
- Operations can be run stably with a small team

---

## 3. Stakeholders and User Classes

| Stakeholder | Main goals | Critical requirements |
| --- | --- | --- |
| Child | Fun, progress, collecting, fair competition | Easy operation, motivation, no public exposure |
| Parent | Control and safety | Playtime control, consent, export/deletion |
| Teacher | Classroom-aligned control | Class management, task release, reports |
| School admin | Safe rollout in the school context | Invitation process, role management |
| Operator | Stable, secure operations | Automation, moderation, monitoring |

---

## 4. System Context and Constraints

### 4.1 Technical Guardrails

- Frontend: TypeScript 5.x, React 19, Babylon.js 7, Vite 6
- Backend: Java 21 LTS, Spring Boot 4.0  (stable LTS version)
- Persistence: PostgreSQL 16
- Session/matchmaking cache: Redis 7
- Asset storage: MinIO (self-hosted)
- Communication: REST/GraphQL + WebSocket
- API contract: OpenAPI 3.1 for REST endpoints
- Build/tooling: Maven Wrapper (backend), pnpm (frontend), Docker and docker-compose
- Test tooling: JUnit 5, AssertJ, Mockito, Testcontainers, Playwright, Cucumber (Gherkin)

### 4.2 Operational Guardrails

- Hosting exclusively in Switzerland
- No public-cloud hyperscaler
- No external CDN outside Switzerland
- Small operations team (1-2 part-time people)
- Everything orchestrable via Docker and docker-compose, no mandatory 24/7 operation

### 4.3 Legal Guardrails

- Comply with FADP (CH) and GDPR
- Double opt-in for parents before unlocking sensitive functions
- Privacy by Design and Privacy by Default

---

## 5. Functional Domain Model (high level)

Central domain objects:

- UserAccount
- ChildProfile
- ParentProfile
- TeacherProfile
- SchoolClass
- LearningProgress
- MathTask
- World
- Portal
- Creature
- CreatureInstance
- Match
- MatchResult
- Event
- Season
- Reward
- ModerationCase
- AuditLog

---

## 6. Functional Requirements

Note: All requirements are formulated as MUST unless explicitly marked as SHOULD/MAY.

### 6.1 Learning System and Task Engine

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-LEARN-001 | The system MUST provide tasks for addition, subtraction, multiplication and division with remainder. | MUST | Domain test of task catalog |
| FR-LEARN-002 | The system MUST generate tasks up to a maximum of 1,000,000 and limit results to this number range. | MUST | Unit and integration tests of generator |
| FR-LEARN-003 | The system MUST support the task types arithmetic task, inverse task, word problem, comparison task, estimation task and division-with-remainder. | MUST | Test matrix for task types |
| FR-LEARN-004 | The system MUST provide difficulty levels S1-S6 according to defined complexity. | MUST | Configuration and E2E test |
| FR-LEARN-005 | The system MUST provide speed levels G0-G5 with configurable time limits. | MUST | E2E with time-limit validation |
| FR-LEARN-006 | The system MUST detect error patterns and be able to trigger adaptive adjustments (pace/mode). | MUST | Integration test of adaptive engine |
| FR-LEARN-007 | The system MUST apply spaced repetition to tasks solved incorrectly or hesitantly. | MUST | Scheduler tests, history check |
| FR-LEARN-008 | The system MUST offer the explanation mode with visual solution steps. | MUST | UI/domain test of explanation mode |
| FR-LEARN-009 | The system MUST track mastery status per content domain according to the mastery thresholds in section 6.1.1. | MUST | Reporting and rule tests |
| FR-LEARN-010 | The system MUST make task pools manageable per world, operation and number range. | MUST | Admin and content tests |
| FR-LEARN-011 | The system MUST, when the retention threshold (relapse) is undercut, demote a content domain from mastery and put it back into the spaced-repetition plan. | MUST | Rule and history tests |
| FR-LEARN-012 | The system MUST grant mastery only after consolidation across at least two sessions on at least two different calendar days. | MUST | Time and session-history tests |

#### 6.1.1 Mastery Thresholds (pedagogically grounded)

Foundation: Bloom Mastery Learning (target corridor 80-90% accuracy), Anderson/Koedinger Cognitive Tutoring (Bayesian Knowledge Tracing typically with P(L) >= 0.85-0.95), Ebbinghaus/Cepeda spacing effect (consolidation across sleep phases, retest after several days), Bjork desirable difficulties.

A content domain is considered mastered (mastery achieved) when all of the following criteria are met simultaneously:

| Level | Accuracy (sliding window) | Window size | Median response time | Min. sessions on different days | Retest after |
| --- | --- | --- | --- | --- | --- |
| S1 (entry, add/sub up to 20) | >= 80% | last 15 tasks | <= 8 s | 2 | 3 days |
| S2 (add/sub up to 100, small times tables entry) | >= 85% | last 20 tasks | <= 6 s | 2 | 5 days |
| S3 (small/large times tables, division without remainder) | >= 90% | last 20 tasks | <= 4 s | 2 | 7 days |
| S4 (multi-digit operations, div. with remainder) | >= 85% | last 20 tasks | <= 10 s | 2 | 7 days |
| S5 (mixed multi-digit up to 100,000) | >= 85% | last 25 tasks | <= 12 s | 2 | 10 days |
| S6 (up to 1,000,000, word problems) | >= 80% | last 25 tasks | <= 15 s | 3 | 14 days |

Additional rules:

- Retention threshold (mastery remains valid): accuracy in the sliding window >= 70%. If it drops below this, relapse applies: demotion into the spaced-repetition queue of the previous level (FR-LEARN-011).
- Minimum quantity: per content domain, at least 30 tasks MUST be completed before mastery is granted for the first time.
- Consolidation: mastery is granted only after at least two sessions on at least two different calendar days (FR-LEARN-012).
- Retest (spacing): an achieved mastery is verified after the level-specific interval with a short probe (3-5 tasks); failure triggers spaced repetition, not immediate demotion.
- Thresholds are set as the default baseline; fine-tuning is performed using pilot-class data (see section 11.2) and is configurable without code deployment (see FR-OPS-002).

#### 6.1.2 Pedagogical Assumptions (binding)

These assumptions are the domain foundation of the learning and game rules and are considered settled. Deviations require justification (pilot data per section 11.2).

- Target-group capacity: the attention span of 7- to 10-year-olds is around 10-15 minutes of focused practice; sprint sessions (FR-GAME-004, 60-90 s) and standard sessions (10-15 min) are calibrated to this.
- Practice frequency: the target is 3-5 short sessions per week; the mastery model (section 6.1.1, at least 2 sessions on 2 calendar days) is calibrated to this frequency.
- Spacing: retest intervals follow an expanding interval curve (3, 5, 7, 10, 14 days), aligned with Cepeda et al. (2008) and the Ebbinghaus forgetting curve.
- Frustration protection: three consecutive errors or time-outs in the same content domain MUST automatically trigger a pace downgrade (G level -1) and a switch to accuracy mode or explanation mode (operationalizes FR-LEARN-006 and FR-GAME-006).
- Success corridor: the short-term hit rate in training mode SHOULD lie between 70% and 90% (Bjork desirable difficulties); outside this corridor, the adaptive engine adjusts difficulty or pace.
- Errors without penalty: tasks may be repeated without limit; errors cost neither star points nor items (see FR-GAM-005).
- Consolidation across sleep phases: mastery is confirmed at the earliest on the following day; targeted retests check retention rather than daily form.
- Language and reading load: word problems (S6) MUST be calibrated to grade-level reading ability in CH High German and are optionally voiceable (see NFR-I18N-003).
- Multiplayer as amplifier, not filter: multiplayer (FR-MP-001 ff.) MUST bring together learners with similar mastery levels so that competition is motivating rather than discouraging.

### 6.2 Game Modes and Progression

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-GAME-001 | The system MUST provide training mode, accuracy mode, duel mode, level mode, sprint mode and explanation mode. | MUST | E2E mode coverage |
| FR-GAME-002 | The accuracy mode MUST run without time pressure (G0). | MUST | Mode test |
| FR-GAME-003 | The level mode MUST represent progress across S1-S6. | MUST | Progression test |
| FR-GAME-004 | The sprint mode MUST support short sessions (approximately 60-90 seconds). | MUST | Session and timer test |
| FR-GAME-005 | The system MUST provide a learning-progress history per child. | MUST | Data and UI test |
| FR-GAME-006 | The system MUST reduce pace or suggest a suitable mode in case of overload. | MUST | Adaptive decision tests |

### 6.3 Worlds and Portals

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-WORLD-001 | The product MUST define 24 3D worlds as the functional target build-out. | MUST | Content acceptance |
| FR-WORLD-002 | Each world MUST have a mathematical focus and a default difficulty level. | MUST | Catalog review of worlds |
| FR-WORLD-003 | The system MUST provide portal types for training, duel, team, event, boss, class and season. | MUST | Portal function test |
| FR-WORLD-004 | Portals MUST be unlockable via rules (level, task pool, release, event window). | MUST | Rule-engine tests |
| FR-WORLD-005 | The system MUST display visual cues for difficulty and progress in worlds. | MUST | UX acceptance |

### 6.4 Creatures, Gallery and Avatar

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-CRE-001 | The product MUST carry 24 starter creatures in the target build-out. | MUST | Catalog reconciliation |
| FR-CRE-002 | The system MUST tie creature evolution to learning progress. | MUST | Integration test of progression |
| FR-CRE-003 | The system MUST provide a gallery for creatures and avatar. | MUST | UI test of gallery |
| FR-CRE-004 | The system MUST allow selection of an active companion creature. | MUST | E2E selection/application |
| FR-CRE-005 | The system MUST offer gender-neutral fantasy base models for avatars. | MUST | Content review |
| FR-CRE-006 | The system MUST keep avatar items permanently in the inventory once unlocked. | MUST | Inventory tests |
| FR-CRE-007 | The system MUST support creature names with variable endings and MUST NOT enforce a fixed ending (e.g., on "i"). | MUST | Content and validation test |

### 6.5 Gamification and Reward System

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-GAM-001 | The system MUST use star points as the only soft currency. | MUST | Currency-logic test |
| FR-GAM-002 | Star points MUST be earnable only by playing, never purchasable. | MUST | Shop and payment-flow test |
| FR-GAM-003 | The system MUST display transparent, fixed item prices. | MUST | UI and config test |
| FR-GAM-004 | The system MUST offer daily and weekly goals with rewards. | MUST | Goal-system test |
| FR-GAM-005 | The system MUST NOT cause permanent loss of progress/items due to errors. | MUST | Negative tests for error cases |
| FR-GAM-006 | The loss mechanic MUST be implemented as a recoverable medium variant (shield + round pool). | MUST | Rule and parental-control test |

### 6.6 Multiplayer and Matchmaking

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-MP-001 | Multiplayer MUST be turn-based (no real-time duels). | MUST | Architecture and E2E test |
| FR-MP-002 | The system MUST support 1v1, 2v2, class challenge, world event, boss fight, friend duel and bot practice. | MUST | Mode test matrix |
| FR-MP-003 | Asynchronous duels MUST be available from Release 2. | MUST | Release acceptance R2 |
| FR-MP-004 | Move time limits MUST be controlled exclusively via G0-G5. | MUST | Config/rule tests |
| FR-MP-005 | Matchmaking MUST take into account learning level, accuracy, speed, fairness and connection quality. | MUST | Simulation tests |
| FR-MP-006 | The system MUST provide bot fallback when opponents are missing. | MUST | Load/fallback test |
| FR-MP-007 | The system MUST exclude global public leaderboards. | MUST | Product and UAT review |
| FR-MP-008 | The system MUST allow only class-internal or friend-circle leaderboards with fantasy names. | MUST | Permission and visibility test |

### 6.7 Parent Area

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-PAR-001 | Parents MUST be able to manage child profiles, multiplayer permissions and friend functions. | MUST | Role/UI test |
| FR-PAR-002 | Parents MUST be able to configure daily hard limits and break recommendations. | MUST | Time-limit test |
| FR-PAR-003 | Parents MUST be able to disable the risk mechanic per child. | MUST | Permission/rule test |
| FR-PAR-004 | Parents MUST be able to trigger data export in JSON and PDF. | MUST | Export test |
| FR-PAR-005 | Parents MUST be able to trigger account deletion via self-service. | MUST | Deletion and audit test |

### 6.8 School Mode and Teacher Area

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-SCH-001 | The school mode MUST support class creation via QR/code. | MUST | Class-flow test |
| FR-SCH-002 | Teachers MUST be able to release and block tasks/worlds. | MUST | Permission/release test |
| FR-SCH-003 | Teachers MUST be able to configure class challenges (cooperative/competitive). | MUST | Challenge test |
| FR-SCH-004 | The homework mode MUST support tasks with due dates. | MUST | End-to-end homework |
| FR-SCH-005 | Learning-status reports MUST be exportable as PDF/CSV. | MUST | Export test |
| FR-SCH-006 | Teaching mode MUST be able to override parental hard limits during school hours. | MUST | Time-rule test |
| FR-SCH-007 | Teacher onboarding MUST occur exclusively by invitation from a school admin. | MUST | Auth/role test |

### 6.9 Child Safety, Moderation and Communication

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-SAFE-001 | The child area MUST prevent free text messages. | MUST | Negative communication test |
| FR-SAFE-002 | Child communication MUST be limited to predefined signals/emojis/short phrases. | MUST | Feature test |
| FR-SAFE-003 | Children's names MUST be chosen from a vetted fantasy-name list. | MUST | Validation test |
| FR-SAFE-004 | Moderation MUST support auto-hide for suspected cases. | MUST | Moderation test |
| FR-SAFE-005 | Security-relevant actions MUST be logged in an auditable manner. | MUST | Audit-log tests |
| FR-SAFE-006 | Parental consent via double opt-in MUST be enforced before unlocking multiplayer/communication. | MUST | Consent-flow test |

### 6.10 LiveOps and Admin

| ID | Requirement | Priority | Acceptance |
| --- | --- | --- | --- |
| FR-OPS-001 | Admins MUST be able to configure events, season rules and rewards without code deployment. | MUST | Admin E2E |
| FR-OPS-002 | Task pools MUST be live-configurable (with versioning). | MUST | Configuration test |
| FR-OPS-003 | The system MUST provide moderation case management with prioritization. | MUST | Moderation-workflow test |
| FR-OPS-004 | The system MUST display system status/health and central operational metrics. | MUST | Monitoring acceptance |

---

## 7. Non-Functional Requirements

### 7.1 Performance and Scaling

| ID | Requirement | Target value/quality criterion | Acceptance |
| --- | --- | --- | --- |
| NFR-PERF-001 | The start page SHOULD become interactive quickly on typical school devices. | p75 Time-to-Interactive <= 4 s | Performance test |
| NFR-PERF-002 | World transitions SHOULD be short. | p75 load time <= 5 s with warm cache | E2E performance |
| NFR-PERF-003 | Multiplayer move processing MUST remain stable. | p95 server response <= 500 ms excluding network latency | Load test |
| NFR-PERF-004 | The system MUST continue to operate in a degraded but controlled manner under load spikes. | No total outage during event peaks | Load/chaos test |

### 7.2 Availability and Operations

| ID | Requirement | Target value/quality criterion | Acceptance |
| --- | --- | --- | --- |
| NFR-OPS-001 | Operations MUST be realistically manageable with 1-2 part-time people. | High degree of automation, no 24/7 obligation | Operations review |
| NFR-OPS-002 | Backups MUST be performed regularly and restored in a testable manner. | Daily, restore test at least monthly | Backup/restore log |
| NFR-OPS-003 | Operational data MUST NOT leave Switzerland insofar as technically/legally possible. | Hosting and backups in CH | Infrastructure audit |

### 7.3 Security and Data Protection

| ID | Requirement | Target value/quality criterion | Acceptance |
| --- | --- | --- | --- |
| NFR-SEC-001 | All external connections MUST be TLS-secured. | HTTPS-only | Security test |
| NFR-SEC-002 | Inputs MUST be validated server-side. | 100% of API endpoints with validation | API test |
| NFR-SEC-003 | Roles and permissions MUST be enforced server-side. | No privileged access without a role | Pen/auth test |
| NFR-SEC-004 | The system MUST provide rate limiting and bot protection. | Active on auth and critical endpoints | Security test |
| NFR-PRIV-001 | Data minimization MUST be demonstrable in the data model and processes. | Only necessary fields | Data audit |
| NFR-PRIV-002 | Parental export and account deletion MUST work in compliance with FADP/GDPR. | Completeness + deletion record | Legal/domain acceptance |

### 7.4 Usability and Accessibility

| ID | Requirement | Target value/quality criterion | Acceptance |
| --- | --- | --- | --- |
| NFR-UX-001 | Operation MUST be understandable for children aged 7-12. | Positive usability rating in tests | User test |
| NFR-A11Y-001 | The dyscalculia mode MUST provide reduced stimulus density and extended time limits. | Mode activatable, rules effective | A11Y test |
| NFR-A11Y-002 | A color-blind mode MUST be available for deuteranopia/protanopia/tritanopia. | 3 profiles | Visual test |
| NFR-A11Y-003 | Reduced motion MUST tone down intense animations. | Global option effective | UI test |
| NFR-A11Y-004 | The parent/teacher area MUST be screen-reader-capable. | ARIA-conformant core flows | A11Y test |
| NFR-A11Y-005 | The child flow MUST be operable by keyboard. | Core tasks without a mouse | E2E keyboard test |

### 7.5 Internationalization and Language

| ID | Requirement | Target value/quality criterion | Acceptance |
| --- | --- | --- | --- |
| NFR-I18N-001 | The product MUST support Swiss High German and English. | 100% of core flows in both languages | Localization test |
| NFR-I18N-002 | Swiss High German MUST be used consistently without sharp s. | 0 occurrences of sharp s in UI texts | Text QA |
| NFR-I18N-003 | Task voice output MUST be available in CH High German and English. | Both audio paths available | Audio test |
| NFR-I18N-004 | Swiss High German MUST consistently support umlauts (ä, ö, ü) in UI, content and audio texts. | 100% correct rendering in core flows | Text/UI QA |

### 7.6 Engineering Quality and Software Craftsmanship

| ID | Requirement | Target value/quality criterion | Acceptance |
| --- | --- | --- | --- |
| NFR-ENG-001 | Development MUST follow software-craftsmanship principles (Clean Code, SOLID, refactoring, small increments). | Demonstrable in review checklists | Architecture/code review |
| NFR-ENG-002 | Test First and TDD MUST be applied as mandatory practice for new or changed business logic. | Red-Green-Refactor traceable per story | PR review + test history |
| NFR-ENG-003 | Acceptance criteria MUST be specified as executable BDD scenarios (Given/When/Then) and tested automatically. | 100% of user stories with BDD scenarios | BDD test report |
| NFR-ENG-004 | CI MUST run unit, integration, E2E and BDD suites on every merge request and block on failure. | 0 merges with red quality gates | CI log |
| NFR-ENG-005 | The toolchain defined in section 4.1 MUST be kept state of the art and assessed at least every six months. | Documented toolchain reviews | Architecture log |
| NFR-ENG-006 | A minimum coverage of automated tests MUST be maintained for core logic. | Backend >= 80% line/70% branch, frontend >= 70% line | Coverage report |

---

## 8. Roles and Permissions Concept

| Role | Core authorization |
| --- | --- |
| Child | Play, learn, use creatures/avatar |
| Parent | Manage child account, consent, limits, export/deletion |
| Teacher | Class management, task releases, reports |
| School admin | Invite and manage teachers |
| Support | Case handling without excessive data access |
| Content Manager | Task pools, events, rewards |
| System admin | Operations, security, infrastructure |

Permission assignment MUST follow the least-privilege principle.

---

## 9. Interface Requirements

### 9.1 External and Internal APIs

- REST or GraphQL for master data and administration
- WebSocket for synchronous multiplayer events and live events
- API versioning MUST be in place

### 9.2 Integrations

Currently no mandatory external education-SSO integrations in the initial scope.

---

## 10. Data Requirements and Data Storage

### 10.1 Minimum Necessary Data

- Pseudonymized child identification
- Learning progress and task history
- Security and audit logs
- Roles and consent status

### 10.2 Explicitly to be Avoided

- Precise location data
- Free contact information for children
- Advertising tracking

### 10.3 Data Location

Production data and backups MUST be kept in Switzerland.

---

## 11. Quality Assurance and Acceptance

### 11.1 Test Types

- Unit tests
- Integration tests
- End-to-end tests
- BDD acceptance tests (Gherkin/Cucumber)
- API contract tests
- Load tests
- Security tests
- Usability tests with children
- Accessibility tests

### 11.2 Pedagogical Acceptance (binding)

Before launch, the following MUST be performed:

- Domain review by an external mathematics-didactics expert
- Piloting in the secured pilot class (Swiss primary school, available)
- Validation of the mastery thresholds from section 6.1.1 against real pilot data
- Release of new worlds/task types prior to activation

### 11.3 Acceptance Gates per Release

Each release MUST meet the following gates:

- Functional coverage of the planned FRs
- All BDD acceptance scenarios for the release scope green
- No blockers in security/data protection
- Critical accessibility requirements met
- Operability with a small team demonstrated

### 11.4 Binding Development and Testing Principles

- Test First is mandatory: before implementing new business logic, tests/scenarios MUST be created first.
- TDD is mandatory for business logic: Red -> Green -> Refactor as the binding work mode.
- BDD is mandatory for acceptance criteria: every story MUST contain Given/When/Then scenarios that run automatically in CI.
- Definition of Done: code, unit tests, integration tests, BDD scenarios, review and documentation update completed.
- In case of conflict between deadline and quality gate: release postponement before quality compromise.

---

## 12. Release and Scope Plan

### 12.1 Release 1 (safe MVP)

- Login with parent + child profile and double opt-in
- 3 playable worlds
- 3 unlockable creatures + starter avatar
- Training/accuracy mode
- Gallery + simple avatar shop
- Learning-progress view
- Parent self-service (limits, export, deletion)

### 12.2 Release 2

- 1v1 synchronous (default G3 = 15 s)
- Asynchronous duels
- Matchmaking with bot fallback
- First class/friend-circle leaderboards
- Simple events

### 12.3 Release 3

- Full class mode
- Teacher area
- Team challenges
- Moderation extension

### 12.4 Release 4

- Extended world coverage
- Seasons
- Boss portals
- LiveOps admin expansion

### 12.5 Release 5

- Full target build-out
- Scaled operations and an established content pipeline

---

## 13. Risks, Assumptions and Dependencies

### 13.1 Main Risks

- Scope (24 worlds + 24 creatures) is large
- 3D performance on school devices
- Moderation effort despite the restrictive communication model
- Balance between motivation and learning effectiveness

### 13.2 Assumptions

- Parents and schools accept a consent-centric model
- CH hosting and self-hosting are budget-feasible
- Pedagogical support is available before launch
- A pilot class in a Swiss primary school is secured

### 13.3 Dependencies

- Content production (worlds, creatures, audio)
- Legal review FADP/GDPR
- Trademark and domain research for the name Numnia before R1 launch

---

## 14. Decision Log (binding product decisions)

| ID | Decision | Status |
| --- | --- | --- |
| D-01 | Math learning game, not a pure times-table game | Decided |
| D-02 | Project name: Numnia | Decided |
| D-03 | Target group 7-12, single UI | Decided |
| D-04 | Target markets CH/EU, USA explicitly not a market | Decided |
| D-05 | No monetization, no advertising | Decided |
| D-06 | Turn-based multiplayer, no real-time | Decided |
| D-07 | G0-G5 as the only time model | Decided |
| D-08 | School mode is a mandatory part | Decided |
| D-09 | Parent self-service for export/deletion | Decided |
| D-10 | Hosting only Switzerland, self-hosted | Decided |
| D-11 | Mastery thresholds per section 6.1.1 (Bloom-/spacing-based), fine-tuning via pilot class | Decided |
| D-12 | Pilot class in a Swiss primary school is secured | Decided |
| D-13 | Pedagogical assumptions (section 6.1.2) are binding; deviations may only be justified via pilot data | Decided |
| D-14 | Domains numnia.com and numnia.ch released for reservation; final trademark and WHOIS check as acceptance gate before R1 launch | Decided |

Planned operational work packages (no open product or process decisions):

| WP | Work package | Owner | Due date |
| --- | --- | --- | --- |
| AP-01 | Trademark and domain registration Numnia (CH/EU, .com/.ch), incl. WHOIS and IPI/EUIPO research | Product lead | Before R1 launch (acceptance gate R1) |
| AP-02 | Empirical validation of the mastery thresholds (section 6.1.1) against pilot-class data and fine-tuning via configuration (FR-OPS-002) | Pedagogical lead + data analysis | Pilot phase R1, completion before R2 |
| AP-03 | Semi-annual toolchain review per NFR-ENG-005 with documented decision | Architecture lead | First time before R1 launch, then H1/H2 annually |

---

## 15. Use-Case Preparation (basis only, no elaboration)

### 15.1 Purpose

This section defines the structure with which use cases will be created in the next step.

### 15.2 Binding Use-Case Template

Each later use case SHOULD contain at least:

- Use-case ID (e.g., UC-LEARN-001)
- Title
- Goal
- Primary actor
- Involved roles
- Preconditions
- Trigger
- Main flow
- Alternative flows
- Exception flows
- Postconditions
- Assigned requirements (FR-/NFR- IDs)
- Acceptance criteria as BDD scenarios (Given/When/Then)

### 15.3 Use-Case Candidates (backlog for the next step)

- Child starts training mode for a chosen operation
- Child plays a turn-based duel against a bot
- Child plays an asynchronous duel against a friend
- Parents set a daily hard limit and disable the risk mechanic
- Parents export child data as JSON/PDF
- Parents delete a child account
- Teacher creates a class and invites children via QR/code
- Teacher releases the task pool for the week
- Teacher exports a learning-status report as CSV
- Admin activates an event with a limited time window
- Moderator processes an auto-hide case

---

## 16. Appendix A: World Catalog (functional target build-out)

| No. | World | Math focus | Default level |
| --- | --- | --- | --- |
| 1 | Wurzelwald | Addition up to 20 | S1 |
| 2 | Bonbontal | Subtraction up to 20 | S1 |
| 3 | Pilzdschungel | Addition up to 100 | S2 |
| 4 | Sumpfgärten | Subtraction up to 100 | S2 |
| 5 | Wasserstadt | Small times tables (1-5) | S2 |
| 6 | Klangwiesen | Small times tables (6-10) | S3 |
| 7 | Wolkeninseln | Division without remainder | S3 |
| 8 | Spiegelseen | Inverse tasks times/divided | S3 |
| 9 | Kristallhöhlen | Large times tables up to 12x12 | S3 |
| 10 | Eisspitzen | 11-/12-times tables + mix | S3 |
| 11 | Wüstenoase | Multiplication 2-digit x 1-digit | S4 |
| 12 | Maschinenhain | Division with remainder (small) | S4 |
| 13 | Schattenmoor | Add/sub up to 1,000 | S4 |
| 14 | Sandsegel-Meer | Multiplication 2-digit x 2-digit | S4 |
| 15 | Glasstadt | Division with remainder up to 1,000 | S4 |
| 16 | Wurzelhöhlen | Mixed operations up to 10,000 | S5 |
| 17 | Drachenklippen | Multiplication 3-digit x 1-digit | S5 |
| 18 | Polartundra | Division with remainder up to 10,000 | S5 |
| 19 | Feuerberge | Mixed operations up to 100,000 | S5 |
| 20 | Himmelszitadelle | Multiplication 3-digit x 2-digit | S5 |
| 21 | Mondkrater | Division with remainder up to 100,000 | S5 |
| 22 | Sternenbibliothek | Mixed operations up to 1,000,000 | S6 |
| 23 | Regenbogenkluft | Mult/div in the millions range | S6 |
| 24 | Sternenkern | All operations mixed incl. word problems | S6 |

---

## 17. Appendix B: Creature Catalog (starter roster)

Wuschli, Bonbon, Pilzar, Moosin, Welleno, Tondo, Luftan, Spiegelon, Glitzer, Froston, Sandor, Zacka, Nebulo, Dünar, Prismon, Wurzelix, Drakon, Polaris, Flamara, Wolkan, Mondor, Sternox, Bogari, Funkel.

---

## 18. Appendix C: Project Name (decision made)

Selected name: Numnia (D-02, section 14).

Rationale: a short, easy-to-pronounce coined name with a clear affinity to "num" (number reference), usable internationally, well brandable in the DACH/EU region, free from references to pure times-tables games.

Candidates not chosen (for documentation of the decision): Zahlenreich, Rechenreich, Zahlenwelten, Mathlantis, Numina, Sternenrechner, Kalkuria, Mathlings, PortalMath, NumiQuest.

Operational work package: trademark and domain registration Numnia before R1 launch (see section 14, AP-01).
