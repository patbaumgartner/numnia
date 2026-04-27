/**
 * ParentDashboardPage — minimal parent area shell (UC-002 cross-area test).
 *
 * Only parent sessions may reach this page; child sessions are blocked by the
 * backend {@code SessionInterceptor} which responds 403 Forbidden.
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
export default function ParentDashboardPage() {
  return (
    <main>
      <h2>Hallo, Elternteil!</h2>
      <p>Hier verwalten Sie die Kinderprofile und den Lernfortschritt.</p>
    </main>
  );
}
