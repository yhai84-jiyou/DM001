import DOMPurify from 'dompurify'

export function sanitizeHtml(html: string): string {
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['video', 'source', 'iframe'],
    ADD_ATTR: ['controls', 'autoplay', 'poster', 'preload', 'allowfullscreen', 'frameborder', 'target'],
  })
}
