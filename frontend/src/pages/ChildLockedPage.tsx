/**
 * ChildLockedPage — shown when a child profile is locked after too many
 * failed PIN attempts (UC-002, BR-004).
 *
 * UI copy is Swiss High German with umlauts, without sharp s
 * (NFR-I18N-002, NFR-I18N-004).
 */
export default function ChildLockedPage() {
  return (
    <main>
      <h2>Profil gesperrt</h2>
      <p>
        Zu viele fehlgeschlagene Anmeldeversuche. Bitte wenden Sie sich an Ihren
        Elternteil, um das Profil entsperren zu lassen.
      </p>
    </main>
  );
}
