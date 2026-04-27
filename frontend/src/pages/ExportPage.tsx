/**
 * UC-010 — ExportPage: parent triggers a self-service data export of the
 * child's data in JSON, PDF, or both formats.
 *
 * Swiss High German UI copy, umlauts, no sharp s (NFR-I18N-002/004).
 * No PII shown (NFR-PRIV-001); audit log is server-side.
 */
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { triggerExport } from '../api/client';
import type { ExportFormat, ExportSummaryResponse } from '../api/types';

type Status = 'idle' | 'loading' | 'done' | 'error';

export default function ExportPage() {
  const { childId } = useParams<{ childId: string }>();
  const parentId =
    (typeof window !== 'undefined' && window.localStorage.getItem('parentId')) || '';

  const [format, setFormat] = useState<ExportFormat>('JSON');
  const [status, setStatus] = useState<Status>('idle');
  const [files, setFiles] = useState<ExportSummaryResponse[]>([]);
  const [error, setError] = useState<string>('');

  if (!parentId) {
    return (
      <main>
        <h2>Daten-Export</h2>
        <p role="alert">Bitte zuerst als Elternteil anmelden.</p>
      </main>
    );
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!childId) return;
    setStatus('loading');
    setError('');
    try {
      const result = await triggerExport(parentId, childId, format);
      setFiles(result);
      setStatus('done');
    } catch {
      setStatus('error');
      setError('Export konnte nicht erstellt werden. Bitte erneut versuchen.');
    }
  }

  return (
    <main>
      <h2>Daten-Export</h2>
      <p>
        Hier koennen Sie eine vollstaendige Kopie der Daten Ihres Kindes
        anfordern. Die Datei steht ueber einen sicheren Link bereit.
      </p>

      <form onSubmit={onSubmit} aria-label="Daten-Export anfordern">
        <fieldset>
          <legend>Format waehlen</legend>
          <label>
            <input
              type="radio"
              name="format"
              value="JSON"
              checked={format === 'JSON'}
              onChange={() => setFormat('JSON')}
            />{' '}
            JSON
          </label>
          <label>
            <input
              type="radio"
              name="format"
              value="PDF"
              checked={format === 'PDF'}
              onChange={() => setFormat('PDF')}
            />{' '}
            PDF
          </label>
          <label>
            <input
              type="radio"
              name="format"
              value="BOTH"
              checked={format === 'BOTH'}
              onChange={() => setFormat('BOTH')}
            />{' '}
            Beide Formate
          </label>
        </fieldset>

        <button type="submit" disabled={status === 'loading'}>
          Export erstellen
        </button>
      </form>

      {status === 'loading' && <p role="status">Export wird erstellt ...</p>}

      {status === 'done' && files.length > 0 && (
        <section aria-label="Bereitgestellte Dateien">
          <h3>Ihre Datei{files.length > 1 ? 'en' : ''}</h3>
          <p>
            Der Link bleibt 7 Tage lang gueltig und wird danach automatisch
            ungueltig.
          </p>
          <ul>
            {files.map((f) => (
              <li key={f.id}>
                <a href={f.signedUrlPath} download>
                  Herunterladen ({f.format})
                </a>
                {' — '}
                <span>{(f.size / 1024).toFixed(1)} KB</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {status === 'error' && <p role="alert">{error}</p>}

      <p>
        Hinweis: Jede Anfrage und jedes Herunterladen wird im Audit-Log mit
        Zeitstempel sicher protokolliert.
      </p>
    </main>
  );
}
