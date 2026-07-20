import { useMemo } from "react";
import { marked } from "marked";
import DOMPurify from "dompurify";

/**
 * 서버가 CommonMark 원문으로 저장한 FAQ/문의 답변을 렌더링한다. Markdown -> HTML 변환 후
 * 반드시 sanitize한다(customerservice.md §0) — 서버가 raw HTML을 거부하더라도 프론트에서
 * 한 번 더 방어한다.
 */
function MarkdownContent({ markdown, className }) {
  const html = useMemo(() => {
    if (!markdown) return "";
    const rawHtml = marked.parse(markdown, { breaks: true });
    return DOMPurify.sanitize(rawHtml);
  }, [markdown]);

  return (
    <div className={className} dangerouslySetInnerHTML={{ __html: html }} />
  );
}

export default MarkdownContent;
