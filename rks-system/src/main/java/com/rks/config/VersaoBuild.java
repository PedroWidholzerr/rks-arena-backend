package com.rks.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/** Versão do build para /readyz e /api/public/status; no Render soma o commit (RENDER_GIT_COMMIT). */
@Component
public class VersaoBuild {

    private final String versao;

    public VersaoBuild(ObjectProvider<BuildProperties> build) {
        BuildProperties props = build.getIfAvailable();
        String base = props != null ? props.getVersion() : "dev";
        String commit = System.getenv("RENDER_GIT_COMMIT");
        this.versao = commit != null && commit.length() >= 7 ? base + "+" + commit.substring(0, 7) : base;
    }

    public String valor() {
        return versao;
    }
}
