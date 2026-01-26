package com.dirplayer.player.xtra;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.director.lingo.DatumType;
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

    private final Map<String, Xtra> xtras;
    private final MultiUserXtra multiUserXtra;

    public XtraManager() {
        this.xtras = new HashMap<>();
        this.multiUserXtra = new MultiUserXtra();
        registerBuiltInXtras();
    }

    private void registerBuiltInXtras() {
        // Register built-in xtras
        xtras.put("multiuser", multiUserXtra);
        xtras.put("fileio", new FileIOXtra());
        xtras.put("netlingo", new NetLingoXtra());
    }

    /**
     * Get an Xtra by name.
     */
    public Xtra getXtra(String name) {
        return xtras.get(name.toLowerCase());
    }

    /**
     * Get the MultiUser Xtra instance.
     */
    public MultiUserXtra getMultiUserXtra() {
        return multiUserXtra;
    }

    /**
     * Create a new instance of an Xtra.
     */
    public Datum createXtraInstance(String xtraName) throws ScriptError {
        Xtra xtra = getXtra(xtraName);
        if (xtra == null) {
            logger.warn("Xtra not found: {}", xtraName);
            return Datum.ofVoid();
        }
        return xtra.createInstance();
    }

    /**
     * Call a method on an Xtra instance.
     */
    public Datum callXtraMethod(Datum instanceDatum, String method, Datum[] args) throws ScriptError {
        if (instanceDatum.getType() != DatumType.XtraInstance) {
            throw new ScriptError("Expected XtraInstance, got " + instanceDatum.getType());
        }

        String xtraName = instanceDatum.getXtraName();
        Xtra xtra = getXtra(xtraName);
        if (xtra == null) {
            throw new ScriptError("Xtra not found: " + xtraName);
        }

        return xtra.callMethod(instanceDatum, method, args);
    }

    /**
     * Get a property from an Xtra instance.
     */
    public Datum getXtraProperty(Datum instanceDatum, String property) throws ScriptError {
        if (instanceDatum.getType() != DatumType.XtraInstance) {
            throw new ScriptError("Expected XtraInstance, got " + instanceDatum.getType());
        }

        String xtraName = instanceDatum.getXtraName();
        Xtra xtra = getXtra(xtraName);
        if (xtra == null) {
            throw new ScriptError("Xtra not found: " + xtraName);
        }

        return xtra.getProperty(instanceDatum, property);
    }

    /**
     * Set a property on an Xtra instance.
     */
    public void setXtraProperty(Datum instanceDatum, String property, Datum value) throws ScriptError {
        if (instanceDatum.getType() != DatumType.XtraInstance) {
            throw new ScriptError("Expected XtraInstance, got " + instanceDatum.getType());
        }

        String xtraName = instanceDatum.getXtraName();
        Xtra xtra = getXtra(xtraName);
        if (xtra == null) {
            throw new ScriptError("Xtra not found: " + xtraName);
        }

        xtra.setProperty(instanceDatum, property, value);
    }

    /**
     * Check if an Xtra exists by name.
     */
    public boolean hasXtra(String name) {
        return xtras.containsKey(name.toLowerCase());
    }

    /**
     * Register a custom Xtra.
     */
    public void registerXtra(Xtra xtra) {
        xtras.put(xtra.getName().toLowerCase(), xtra);
    }

    /**
     * Base interface for Xtras.
     */
    public interface Xtra {
        /**
         * Get the name of this Xtra.
         */
        String getName();

        /**
         * Create a new instance of this Xtra.
         */
        Datum createInstance() throws ScriptError;

        /**
         * Call a method on an instance.
         */
        Datum callMethod(Object instance, String method, Datum[] args) throws ScriptError;

        /**
         * Get a property from an instance.
         */
        Datum getProperty(Object instance, String property) throws ScriptError;

        /**
         * Set a property on an instance.
         */
        void setProperty(Object instance, String property, Datum value) throws ScriptError;
    }

    /**
     * FileIO Xtra implementation stub.
     */
    public static class FileIOXtra implements Xtra {
        private static final Logger logger = LoggerFactory.getLogger(FileIOXtra.class);

        @Override
        public String getName() {
            return "fileio";
        }

        @Override
        public Datum createInstance() {
            return Datum.ofXtraInstance("fileio", 1);
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
                case "displayopen":
                    return Datum.ofString("");
                case "displaysave":
                    return Datum.ofString("");
                case "createfile":
                    return Datum.ofInt(0);
                case "delete":
                    return Datum.ofInt(0);
                case "getlength":
                    return Datum.ofInt(0);
                case "getposition":
                    return Datum.ofInt(0);
                case "setposition":
                    return Datum.ofVoid();
                case "readchar":
                    return Datum.ofString("");
                case "readword":
                    return Datum.ofString("");
                case "readline":
                    return Datum.ofString("");
                case "writestring":
                    return Datum.ofVoid();
                case "writechar":
                    return Datum.ofVoid();
                case "status":
                    return Datum.ofInt(0);
                case "error":
                    return Datum.ofString("");
                default:
                    logger.warn("FileIO method not implemented: {}", method);
                    return Datum.ofVoid();
            }
        }

        @Override
        public Datum getProperty(Object instance, String property) {
            switch (property.toLowerCase()) {
                case "filename":
                    return Datum.ofString("");
                case "status":
                    return Datum.ofInt(0);
                default:
                    return Datum.ofVoid();
            }
        }

        @Override
        public void setProperty(Object instance, String property, Datum value) {
            // No-op for now
        }
    }

    /**
     * NetLingo Xtra implementation stub.
     */
    public static class NetLingoXtra implements Xtra {
        private static final Logger logger = LoggerFactory.getLogger(NetLingoXtra.class);

        @Override
        public String getName() {
            return "netlingo";
        }

        @Override
        public Datum createInstance() {
            return Datum.ofXtraInstance("netlingo", 1);
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
                case "gotonetmovie":
                    return Datum.ofVoid();
                case "preloadnetthing":
                    return Datum.ofInt(0);
                case "downloadnetthing":
                    return Datum.ofInt(0);
                case "netlastmoddate":
                    return Datum.ofString("");
                case "netmime":
                    return Datum.ofString("");
                case "nettextresult":
                    return Datum.ofString("");
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
