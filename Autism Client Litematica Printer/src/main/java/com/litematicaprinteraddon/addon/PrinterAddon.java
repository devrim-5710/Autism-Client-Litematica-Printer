package com.litematicaprinteraddon.addon;

import autismclient.api.ApiVersion;
import autismclient.api.SimpleAddon;

public final class PrinterAddon extends SimpleAddon {
    public static final String ID = "litematica-printer-addon";

    public PrinterAddon() {
        super(ApiVersion.CURRENT, "com.litematicaprinteraddon.addon");
    }

    @Override
    protected void initialize() {
        registerModule(new PrinterModule());
    }
}
