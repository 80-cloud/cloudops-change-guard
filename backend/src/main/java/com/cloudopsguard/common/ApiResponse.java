package com.cloudopsguard.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 成功レスポンスの共通ラッパ（API設計.md §0）。
 * 形式: {@code { "data": ..., "meta": ... }}。meta は一覧のページ情報等で、無ければ省略する。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, Object meta) {

    /** meta 無しの成功レスポンス。 */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    /** meta 付き（一覧など）の成功レスポンス。 */
    public static <T> ApiResponse<T> of(T data, Object meta) {
        return new ApiResponse<>(data, meta);
    }
}
