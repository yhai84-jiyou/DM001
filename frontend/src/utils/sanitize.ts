// Will use DOMPurify for XSS protection when rendering article HTML
export function sanitizeHtml(html: string): string {
  // DOMPurify will be added in a later phase
  // For now, return as-is (backend already sanitizes with jsoup)
  return html
}
