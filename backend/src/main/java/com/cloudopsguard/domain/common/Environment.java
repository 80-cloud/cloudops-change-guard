package com.cloudopsguard.domain.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 対象環境。DB / JSON 上の値は<b>小文字</b>（development/staging/production）で保持する
 * （seed・テストの値と一致させる＝B4 の罠回避）。
 *
 * <p>JSON は {@link JsonValue}/{@link JsonCreator} で小文字に対応し、
 * JPA は {@link com.cloudopsguard.domain.common.EnvironmentConverter} で小文字列に対応する。
 */
public enum Environment {
    DEVELOPMENT("development"),
    STAGING("staging"),
    PRODUCTION("production");

    private final String wire;

    Environment(String wire) {
        this.wire = wire;
    }

    /** DB / JSON 上の表現（小文字）。 */
    @JsonValue
    public String wire() {
        return wire;
    }

    @JsonCreator
    public static Environment fromWire(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        for (Environment e : values()) {
            if (e.wire.equalsIgnoreCase(v) || e.name().equalsIgnoreCase(v)) {
                return e;
            }
        }
        throw new IllegalArgumentException("不正な environment 値: " + value);
    }
}
