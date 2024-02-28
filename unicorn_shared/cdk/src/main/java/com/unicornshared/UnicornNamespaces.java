package com.unicornshared;

public interface UnicornNamespaces {
    public enum NameSpace{
        UnicornContractsNamespaceParam("/uni-prop/UnicornContractsNamespace","unicorn.contracts"),
        UnicornPropertiesNamespaceParam("/uni-prop/UnicornPropertiesNamespace","unicorn.properties"),
        UnicornWebNamespaceParam("/uni-prop/UnicornWebNamespace","unicorn.web");

        public final String name;
        public final String value;
        NameSpace(String name, String value) {
            this.name=name;
            this.value=value;
        }
    }
}
