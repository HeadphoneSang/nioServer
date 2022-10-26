package com.chbcraft.internals.components.enums;

public enum SectionName {
    MAX_PLUGIN_LIMIT("max_plugin_limit"),
    PLUGIN_LOADING("plugin_loading_message"),
    ENABLE_SECURITY("enable_security_IO"),
    ENABLE_CROSS_DOMAIN("enable_cross_domain"),
    ENABLE_DEPEND_OTHER("enable_depends"),
    ENABLE_SYSTEM_OUT("replace_system_out"),
    MAIN_CLASS("main"),
    PLUGIN_NAME("name"),
    PLUGIN_DEPENDS("after");
    private final String section;
    private SectionName(String section){
        this.section = section;
    }
    public String value(){
        return this.section;
    }
}
