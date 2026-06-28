package com.cloudopsguard.domain.common;

/** IaC 種別。値は大文字のまま DB / JSON に保持する。 */
public enum IacType {
    CLOUDFORMATION,
    TERRAFORM
}
