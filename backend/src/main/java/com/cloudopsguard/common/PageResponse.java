package com.cloudopsguard.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 一覧 API の共通ページネーション形式。{@link ApiResponse} の data に載せる。
 * Spring Data の {@link Page} から DTO へ写しつつ {@link PageMeta} を組み立てる
 * （全件取得しない＝P-2。Entity をそのまま返さない＝DTO へ変換する）。
 */
public record PageResponse<T>(List<T> content, PageMeta meta) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent().stream().map(mapper).toList();
        PageMeta meta = new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
        return new PageResponse<>(content, meta);
    }

    /** ページ情報。 */
    public record PageMeta(int page, int size, long totalElements, int totalPages) {
    }
}
