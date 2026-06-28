package com.cloudopsguard.domain.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@link Environment} を DB 上の小文字列（development/staging/production）と相互変換する。
 * {@code @Enumerated(STRING)} は enum 名（大文字）を保存してしまうため、小文字維持には本コンバータを使う。
 * {@code autoApply=true} で当該型のフィールドへ自動適用する。
 */
@Converter(autoApply = true)
public class EnvironmentConverter implements AttributeConverter<Environment, String> {

    @Override
    public String convertToDatabaseColumn(Environment attribute) {
        return attribute == null ? null : attribute.wire();
    }

    @Override
    public Environment convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Environment.fromWire(dbData);
    }
}
