package com.dirplayer.player.xtra;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.ScriptError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager for Director Xtras (external plugins).
 * Port of Rust XtraManager.
 */
public class XtraManager {
    private static final Logger logger = LoggerFactory.getLogger(XtraManager.class);

    private Map<String, Xtra> xtras;

    public XtraManager() {
        this.xtras = new HashMap<>();
        registerBuiltInXtras();
    }

    private void registerBuiltInXtras() {
        // Register built-in xtras
        xtras.put("multiuser", new MultiUserXtra());
        xtras.put("fileio", new FileIOXtra());
        xtras.put("netlingo", new NetLingoXtra());
    }

    public Xtra getXtra(String name) {
        return xtras.get(name.toLowerCase());
    }

    public Datum createXtraInstance(String xtraName) throws ScriptError {
        Xtra xtra = getXtra(xtraName);
        if (xtra == null) {
            logger.warn("Xtra not found: {}", xtraName);
            return Datum.ofVoid();
        }
        return xtra.createInstance();
    }

    /**
     * Base interface for Xtras.
     */
    public interface Xtra {
        String getName();
        Datum createInstance() throws ScriptError;
        Datum callMethod(Object instance, String method, Datum[] args) throws ScriptError;
        Datum getProperty(Object instance, String property) throws ScriptError;
        void setProperty(Object instance, String property, Datum value) throws ScriptError;
    }

    /**
     * MultiUser Xtra implementation stub.
     */
    public static class MultiUserXtra implements Xtra {
        @Override
        public String getName() {
            return "multiuser";
        }

        @Override
        public Datum createInstance() {
            return Datum.ofVoid(); // TODO: Create actual instance
        }

        @Override
        public Datum callMethod(Object instance, String method, Datum[] args) throws ScriptError {
            switch (method.toLowerCase()) {
                case "connecttonetserver":
                    return Datum.ofInt(0); // Connection ID
                case "getneterrorstring":
                    return Datum.ofString("");
                case "getnumusers":
                    return Datum.ofInt(0);
                case "getmessage":
                    return Datum.ofVoid();
                default:
                    logger.warn("MultiUser method not implemented: {}", method);
                    return Datum.ofVoid();
            }
        }

        @Override
        public Datum getProperty(Object instance, String property) {
            return Datum.ofVoid();
        }

        @Override
        public void setProperty(Object instance, String property, Datum value) {
            // No-op
        }
    }

    /**
     * FileIO Xtra implementation stub.
     */
    public static class FileIOXtra implements Xtra {
        @Override
        public String getName() {
            return "fileio";
        }

        @Override
        public Datum createInstance() {
            return Datum.ofVoid();
        }

        @Override
        public Datum callMethod(Object instance, String method, Datum[] args) throws ScriptError {
            switch (method.toLowerCase()) {
                case "openfile":
                    return Datum.ofInt(0); // File handle
                case "readfile":
                    return Datum.ofString("");
                case "closefile":
                    return Datum.ofVoid();
                case "getfinderinfo":
                    return Datum.ofString("");
                case "setfinderinfo":
                    return Datum.ofVoid();
                default:
                    logger.warn("FileIO method not implemented: {}", method);
                    return Datum.ofVoid();
            }
        }

        @Override
        public Datum getProperty(Object instance, String property) {
            return Datum.ofVoid();
        }

        @Override
        public void setProperty(Object instance, String property, Datum value) {
            // No-op
        }
    }

    /**
     * NetLingo Xtra implementation stub.
     */
    public static class NetLingoXtra implements Xtra {
        @Override
        public String getName() {
            return "netlingo";
        }

        @Override
        public Datum createInstance() {
            return Datum.ofVoid();
        }

        @Override
        public Datum callMethod(Object instance, String method, Datum[] args) throws ScriptError {
            switch (method.toLowerCase()) {
                case "getnettext":
                    return Datum.ofString("");
                case "prenettext":
                    return Datum.ofString("");
                case "netdone":
                    return Datum.ofInt(1);
                case "neterror":
                    return Datum.ofString("");
                case "gotonetpage":
                    return Datum.ofVoid();
                default:
                    logger.warn("NetLingo method not implemented: {}", method);
                    return Datum.ofVoid();
            }
        }

        @Override
        public Datum getProperty(Object instance, String property) {
            return Datum.ofVoid();
        }

        @Override
        public void setProperty(Object instance, String property, Datum value) {
            // No-op
        }
    }
}
