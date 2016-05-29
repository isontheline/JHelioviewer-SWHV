package org.helioviewer.jhv.plugins.swek.config;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.JSONUtils;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.data.datatype.event.SWEKConfiguration;
import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.data.datatype.event.SWEKParameter;
import org.helioviewer.jhv.data.datatype.event.SWEKParameterFilter;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedEvents;
import org.helioviewer.jhv.data.datatype.event.SWEKRelatedOn;
import org.helioviewer.jhv.data.datatype.event.SWEKSource;
import org.helioviewer.jhv.data.datatype.event.SWEKSpatialRegion;
import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;
import org.helioviewer.jhv.database.EventDatabase;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;
import org.helioviewer.jhv.plugins.swek.settings.SWEKProperties;
import org.helioviewer.jhv.plugins.swek.settings.SWEKSettings;
import org.helioviewer.jhv.plugins.swek.view.SWEKIconBank;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SWEKConfigurationManager {

    private static SWEKConfigurationManager singletonInstance;

    /** Config loaded */
    private boolean configLoaded;

    /** Config file URL */
    private URL configFileURL;

    /** The loaded configuration */
    private SWEKConfiguration configuration;

    /** Map containing the sources */
    private final Map<String, SWEKSource> sources;

    /** Map containing the parameters */
    private final Map<String, SWEKParameter> parameters;

    /** Map containing the event types */
    private final Map<String, SWEKEventType> eventTypes;

    private final List<SWEKEventType> orderedEventTypes;

    /** The properties of the swek plugin */
    private final Properties swekProperties;

    private SWEKConfigurationManager() {
        configLoaded = false;
        sources = new HashMap<String, SWEKSource>();
        parameters = new HashMap<String, SWEKParameter>();
        eventTypes = new HashMap<String, SWEKEventType>();
        swekProperties = SWEKProperties.getSingletonInstance().getSWEKProperties();
        orderedEventTypes = new ArrayList<SWEKEventType>();
    }

    public static SWEKConfigurationManager getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new SWEKConfigurationManager();
        }
        return singletonInstance;
    }

    /**
     * Loads the configuration.
     *
     * If no configuration file is set by the user, the program downloads the
     * configuration file online and saves it the
     * JHelioviewer/Plugins/swek-plugin folder.
     *
     */
    public void loadConfiguration() {
        if (!configLoaded) {
            Log.debug("search and open the configuration file");
            boolean isConfigParsed;
            if (checkAndOpenUserSetFile()) {
                isConfigParsed = parseConfigFile();
            } else if (checkAndOpenHomeDirectoryFile()) {
                boolean manuallyChanged = isManuallyChanged();
                if (!manuallyChanged) {
                    // check if the file is manually changed if not we download
                    // the latest version anyway.
                    if (checkAndOpenZippedFile()) {
                        isConfigParsed = parseConfigFile();
                    } else {
                        isConfigParsed = false;
                    }
                } else {
                    isConfigParsed = parseConfigFile();
                }
            } else if (checkAndOpenZippedFile()) {
                isConfigParsed = parseConfigFile();
            } else {
                isConfigParsed = false;
            }
            configLoaded = isConfigParsed; // TODO set on the panel the config file could not be parsed
        }
    }

    /**
     * Checks if the configuration file was manually changed.
     *
     * @return true if the configuration file was initially changed, false if
     *         not
     */
    private boolean isManuallyChanged() {
        try {
            Log.debug("configURL: " + configFileURL);
            JSONObject configJSON = JSONUtils.getJSONStream(configFileURL.openStream());
            return parseManuallyChanged(configJSON);
        } catch (JSONException e) {
            Log.error("Could not parse JSON: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            Log.error("Could not load the file: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gives a map with all the event types. The event type name is the key and
     * the event type is the value.
     *
     * @return map containing the event types found in the configuration file
     */
    public Map<String, SWEKEventType> getEventTypes() {
        loadConfiguration();
        return eventTypes;
    }

    /**
     * Gives an configuration ordered list with all the event types.
     *
     * @return map containing the event types found in the configuration file
     */
    public List<SWEKEventType> getOrderedEventTypes() {
        loadConfiguration();
        return orderedEventTypes;
    }

    /**
     * Gives a map with all the event sources. The source name is the key and
     * the source is the value.
     *
     * @return map containing the sources found in the configuration file
     */
    public Map<String, SWEKSource> getSources() {
        loadConfiguration();
        return sources;
    }

    /**
     * Gets the related event rules.
     *
     * @return the related event rules.
     */
    public List<SWEKRelatedEvents> getSWEKRelatedEvents() {
        loadConfiguration();
        return configuration.getRelatedEvents();
    }

    public SWEKEventType getEventType(String eventTypeName) {
        loadConfiguration();
        return configuration.getSWEKEventType(eventTypeName);
    }

    public SWEKSupplier getSWEKSupplier(String supplierName, String eventTypeName) {
        loadConfiguration();
        return configuration.getSWEKSupplier(supplierName, eventTypeName);
    }

    public SWEKSource getSWEKSource(String sourceName) {
        loadConfiguration();
        return configuration.getSWEKSource(sourceName);
    }

    private boolean checkAndOpenZippedFile() {
        URL url = SWEKPlugin.class.getResource(swekProperties.getProperty("plugin.swek.zipconfigfile"));
        ReadableByteChannel rbc;
        try {
            rbc = Channels.newChannel(url.openStream());
            String saveFile = SWEKSettings.SWEK_HOME + swekProperties.getProperty("plugin.swek.configfilename");
            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            configFileURL = (new File(saveFile)).toURI().toURL();
            return true;
        } catch (IOException e) {
            Log.debug("Something went wrong extracting the configuration file from the jar bundle or saving it: " + e);
        }
        return false;
    }

    /**
     * Checks the home directory of the plugin (normally
     * ~/JHelioviewer/Plugins/swek-plugin/) for the existence of the
     * SWEKSettings.json file.
     *
     * @return true if the file was found and useful, false if the file was not
     *         found.
     */
    private boolean checkAndOpenHomeDirectoryFile() {
        String configFile = SWEKSettings.SWEK_HOME + swekProperties.getProperty("plugin.swek.configfilename");
        try {
            File f = new File(configFile);
            if (f.exists()) {
                configFileURL = f.toURI().toURL();
                return true;
            } else {
                Log.debug("File created from the settings: " + configFile + " does not exists on this system");
            }
        } catch (MalformedURLException e) {
            Log.debug("File " + configFile + " could not be parsed into an URL");
        }
        return false;
    }

    /**
     * Checks the jhelioviewer settings file for a swek configuration file.
     *
     * @return true if the file as found and useful, false if the file was not
     *         found.
     */
    private boolean checkAndOpenUserSetFile() {
        Log.debug("Search for a user defined configuration file in the JHelioviewer setting file");
        Settings jhvSettings = Settings.getSingletonInstance();
        String fileName = jhvSettings.getProperty("plugin.swek.configfile");
        if (fileName == null) {
            Log.debug("No configured filename found");
            return false;
        } else {
            try {
                URI fileLocation = new URI(fileName);
                configFileURL = fileLocation.toURL();
                Log.debug("Config file: " + configFileURL);
                return true;
            } catch (URISyntaxException e) {
                Log.debug("Wrong URI syntax for the found file name: " + fileName);
            } catch (MalformedURLException e) {
                Log.debug("Could not convert the URI in a correct URL. The found file name: " + fileName);
            }
            return false;
        }
    }

    /**
     * Parses the SWEK settings.
     */
    private boolean parseConfigFile() {
        try {
            JSONObject configJSON = JSONUtils.getJSONStream(configFileURL.openStream());
            EventDatabase.config_hash = Arrays.hashCode(configJSON.toString().toCharArray());
            return parseJSONConfig(configJSON);
        } catch (IOException e) {
            Log.debug("Configuration file could not be parsed: " + e);
        } catch (JSONException e) {
            Log.debug("Could not parse JSON: " + e);
        }
        return false;
    }

    /**
     * Parses the JSON from start
     *
     * @param configJSON
     *            The JSON to parse
     * @return true if the JSON configuration could be parsed, false if not.
     */
    private boolean parseJSONConfig(JSONObject configJSON) {
        try {
            configuration = new SWEKConfiguration(parseVersion(configJSON), parseManuallyChanged(configJSON), parseSources(configJSON), parseEventTypes(configJSON), parseRelatedEvents(configJSON));
            SWEKEventType.setSwekRelatedEvents(configuration.getRelatedEvents());
            return true;
        } catch (JSONException e) {
            Log.error("Could not parse JSON");
            e.printStackTrace();
            configuration = new SWEKConfiguration("", false, new ArrayList<SWEKSource>(), new ArrayList<SWEKEventType>(), new ArrayList<SWEKRelatedEvents>());
            return false;
        }
    }

    /**
     * Parses a if the configuration was manually changed from a json.
     *
     * @param jsonObject
     *            the json from which to parse the manually changed indication
     * @return the parsed manually changed indication
     * @throws JSONException
     *             if the manually changed indication could not be parsed
     */
    private boolean parseManuallyChanged(JSONObject configJSON) throws JSONException {
        return configJSON.getBoolean("manually_changed");
    }

    /**
     * Parses the configuration version from the big json.
     *
     * @param configJSON
     *            The JSON from which to parse
     * @return The parsed configuration version
     * @throws JSONException
     *             If the configuration version could not be parsed.
     */
    private String parseVersion(JSONObject configJSON) throws JSONException {
        return configJSON.getString("config_version");
    }

    /**
     * Parses the list of sources from a json and adds the sources to a map
     * indexed on the name of the source.
     *
     * @param configJSON
     *            the JSON from which to parse the sources
     * @return a list of sources parsed from the JSON
     * @throws JSONException
     *             if the sources could not be parsed
     */
    private List<SWEKSource> parseSources(JSONObject configJSON) throws JSONException {
        ArrayList<SWEKSource> swekSources = new ArrayList<SWEKSource>();
        JSONArray sourcesArray = configJSON.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.length(); i++) {
            SWEKSource source = parseSource(sourcesArray.getJSONObject(i));
            sources.put(source.getSourceName(), source);
            swekSources.add(source);
        }
        return swekSources;
    }

    /**
     * Parses a source from a json.
     *
     * @param jsonObject
     *            the json from which to parse the source
     * @return the parsed source
     * @throws JSONException
     *             if the source could not be parsed
     */
    private SWEKSource parseSource(JSONObject jsonObject) throws JSONException {
        return new SWEKSource(parseSourceName(jsonObject), parseProviderName(jsonObject), parseDownloader(jsonObject), parseJarLocation(jsonObject), parseEventParser(jsonObject), parseBaseURL(jsonObject), parseGeneralParameters(jsonObject));
    }

    /**
     * Parses the source from a json.
     *
     * @param jsonObject
     *            the json from which to parse the source
     * @return the parsed source
     * @throws JSONException
     *             if the source could not be parsed
     */
    private String parseSourceName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("name");
    }

    /**
     * Parses the provider name from a json.
     *
     * @param jsonObject
     *            the json from which to parse the provider name
     * @return the parsed provider name
     * @throws JSONException
     *             if the provider name could not be parsed
     */
    private String parseProviderName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("provider_name");
    }

    /**
     * Parses the downloader description from a json.
     *
     * @param jsonObject
     *            the json from which to parse the downloader description
     * @return the parsed downloader description
     * @throws JSONException
     *             if the downloader description could not be parsed
     */
    private String parseDownloader(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("downloader");

    }

    /**
     * Parses the event parser from a json.
     *
     * @param jsonObject
     *            the json from which to parse the event parser
     * @return the parsed event parser
     * @throws JSONException
     *             if the event parser could not be parsed
     */
    private String parseEventParser(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("event_parser");
    }

    /**
     * Parses the jar location from a json.
     *
     * @param jsonObject
     *            the json from which to parse the jar location
     * @return the parsed jar location
     * @throws JSONException
     *             if the event parser could not be parsed
     */

    private String parseJarLocation(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("jar_location");
    }

    /**
     * Parses the base url from a json.
     *
     * @param jsonObject
     *            the json from which to parse the base url
     * @return the parsed base url
     * @throws JSONException
     *             if the base url could not be parsed
     */
    private String parseBaseURL(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("base_url");
    }

    /**
     * Parses the general parameters from a json.
     *
     * @param jsonObject
     *            the json from which to parse the general parameters
     * @return the parsed general parameters
     * @throws JSONException
     *             if the general parameters could not be parsed
     */
    private List<SWEKParameter> parseGeneralParameters(JSONObject jsonObject) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<SWEKParameter>();
        JSONArray parameterArray = jsonObject.getJSONArray("general_parameters");
        for (int i = 0; i < parameterArray.length(); i++) {
            SWEKParameter parameter = parseParameter(parameterArray.getJSONObject(i));
            parameterList.add(parameter);
            parameters.put(parameter.getParameterName(), parameter);
        }
        return parameterList;
    }

    /**
     * Parses the list of event types from a json.
     *
     * @param configJSON
     *            the JSON from which to parse the event types
     * @param relatedEvents
     * @return a list of event types parsed from the JSON
     * @throws JSONException
     *             if the event types could not be parsed
     */
    private List<SWEKEventType> parseEventTypes(JSONObject configJSON) throws JSONException {
        List<SWEKEventType> result = new ArrayList<SWEKEventType>();
        JSONArray eventJSONArray = configJSON.getJSONArray("events_types");
        for (int i = 0; i < eventJSONArray.length(); i++) {
            SWEKEventType eventType = parseEventType(eventJSONArray.getJSONObject(i));
            result.add(eventType);
            eventTypes.put(eventType.getEventName(), eventType);
            orderedEventTypes.add(eventType);
        }
        return result;
    }

    /**
     * Parses an event type from a json.
     *
     * @param object
     *            The event type to parse
     * @param list
     * @return The parsed event type
     * @throws JSONException
     */
    private SWEKEventType parseEventType(JSONObject object) throws JSONException {
        return new SWEKEventType(parseEventName(object), parseSuppliers(object), parseParameterList(object), parseRequestIntervalExtension(object), parseStandardSelected(object), parseGroupOn(object), parseCoordinateSystem(object), parseEventIcon(object), parseColor(object), parseSpatialRegion(object));
    }

    /**
     * Parses the color from the json.
     *
     * @param object
     *            the json to parse from
     * @return the color of the event type or black if something went wrong.
     * @throws JSONException
     *             If the parsing went wrong
     */
    private Color parseColor(JSONObject object) throws JSONException {
        String color = object.getString("color");
        try {
            URI colorURI = new URI(color);
            String colorScheme = colorURI.getScheme().toLowerCase();
            if (colorScheme.equals("colorname")) {
                return parseColorName(colorURI.getHost());
            } else if (colorScheme.equals("colorcode")) {
                return parseColorCode(colorURI.getHost());
            } else {
                Log.info("Could not understand: " + color + ", black returned");
                return Color.black;
            }
        } catch (URISyntaxException e) {
            Log.info("Could not parse the URI " + color + ", black returned");
        }
        return Color.black;
    }

    /**
     * Parses a hexadecimal or octal string to a color.
     *
     * @param colorCode
     *            the code to parse into a color
     * @return the color represented by the hex st ring or black if something
     *         went wrong
     */
    private Color parseColorCode(String colorCode) {
        try {
            return Color.decode(colorCode);
        } catch (NumberFormatException ex) {
            Log.info("Could not parse the color code " + colorCode + ", black returned");
        }
        return Color.black;
    }

    /**
     * Parses a color name into a color.
     *
     * @param colorName
     *            the name to parse into a color
     * @return the color represented by the color name or black if something
     *         went wrong
     */
    private Color parseColorName(String colorName) {
        try {
            Field field = Class.forName("java.awt.Color").getField(colorName);
            return (Color) field.get(null);
        } catch (Exception e) {
            Log.info("Could not parse the color name " + colorName + ", black returned");
        }
        return Color.black; // Not defined
    }

    /**
     * Parses the event icon settings.
     *
     * @param object
     *            the JSON object from where to parse the icon object.
     * @return the icon defined in the configuration
     * @throws JSONException
     *             if the JSON object could not be parsed
     */
    private ImageIcon parseEventIcon(JSONObject object) throws JSONException {
        String eventIconValue = object.getString("icon");
        try {
            URI eventIconURI = new URI(eventIconValue);
            if (eventIconURI.getScheme().toLowerCase().equals("iconbank")) {
                return SWEKIconBank.getSingletonInstance().getIcon(eventIconURI.getHost());
            } else {
                return SWEKIconBank.getSingletonInstance().getIcon("Other");
            }
            // TODO Bram : Add other ways to add icons (file,url,...)
        } catch (URISyntaxException e) {
            Log.info("Could not parse the URI " + eventIconValue + ", null icon returned");
        }
        return null;
    }

    /**
     * Parses the event name from a json.
     *
     * @param object
     *            the json from which the event name is parsed
     * @return the event name
     * @throws JSONException
     *             if the supplier name could not be parsed
     */
    private String parseEventName(JSONObject object) throws JSONException {
        return object.getString("event_name");
    }

    /**
     * Parses the suppliers from a json.
     *
     * @param jsonObject
     *            the json from which the suppliers are parsed
     * @return the suppliers list
     * @throws JSONException
     *             if the suppliers could not be parsed
     */
    private List<SWEKSupplier> parseSuppliers(JSONObject object) throws JSONException {
        List<SWEKSupplier> suppliers = new ArrayList<SWEKSupplier>();
        JSONArray suppliersArray = object.getJSONArray("suppliers");
        for (int i = 0; i < suppliersArray.length(); i++) {
            suppliers.add(parseSupplier(suppliersArray.getJSONObject(i)));
        }
        return suppliers;
    }

    /**
     * Parses a supplier from a json.
     *
     * @param object
     *            the json from which the supplier is parsed
     * @return the supplier
     * @throws JSONException
     *             if the supplier could not be parsed
     */
    private SWEKSupplier parseSupplier(JSONObject object) throws JSONException {
        return new SWEKSupplier(parseSupplierName(object), parseSupplierDisplayName(object), parseSupplierSource(object), parseDbName(object));
    }

    /**
     * Parses the supplier name from a json.
     *
     * @param object
     *            the json from which the supplier name is parsed
     * @return the supplier name
     * @throws JSONException
     *             if the supplier name could not be parsed
     */
    private String parseSupplierName(JSONObject object) throws JSONException {
        return object.getString("supplier_name");
    }

    /**
     * Parses the supplier display name from a json
     *
     * @param object
     *            the JSON from which to parse the supplier display name
     * @return the supplier display name
     * @throws JSONException
     *             if the supplier display name could not be parsed
     */
    private String parseSupplierDisplayName(JSONObject object) throws JSONException {
        return object.getString("supplier_display_name");
    }

    /**
     * Parses a supplier source from a json.
     *
     * @param object
     *            the json from which the supplier source is parsed
     * @return the supplier source
     * @throws JSONException
     *             if the supplier source could not be parsed
     */
    private SWEKSource parseSupplierSource(JSONObject object) throws JSONException {
        return sources.get(object.getString("source"));
    }

    private String parseDbName(JSONObject object) throws JSONException {
        return object.getString("db");
    }

    /**
     * Parses the parameter list from the given JSON.
     *
     * @param object
     *            the json from which to parse the parameter list
     * @return the parameter list
     * @throws JSONException
     *             if the parameter list could not be parsed.
     */
    private List<SWEKParameter> parseParameterList(JSONObject object) throws JSONException {
        List<SWEKParameter> parameterList = new ArrayList<SWEKParameter>();
        JSONArray parameterListArray = object.getJSONArray("parameter_list");
        for (int i = 0; i < parameterListArray.length(); i++) {
            SWEKParameter parameter = parseParameter((JSONObject) parameterListArray.get(i));
            parameterList.add(parameter);
            parameters.put(parameter.getParameterName(), parameter);
        }
        return parameterList;
    }

    /**
     * Parses a parameter from json.
     *
     * @param jsonObject
     *            the json from which to parse
     * @return the parsed parameter
     * @throws JSONException
     *             if the parameter could not be parsed
     */
    private SWEKParameter parseParameter(JSONObject jsonObject) throws JSONException {
        return new SWEKParameter(parseSourceInParameter(jsonObject), parseParameterName(jsonObject), parseParameterDisplayName(jsonObject), parseParameterFilter(jsonObject), parseDefaultVisible(jsonObject));
    }

    /**
     * Parses the source from a json.
     *
     * @param jsonObject
     *            the json from which the source is parsed
     * @return the source
     * @throws JSONException
     *             if the source could not be parsed
     */
    private String parseSourceInParameter(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("source");
    }

    /**
     * Parses the parameter name from a json.
     *
     * @param jsonObject
     *            the json from which the parameter name is parsed
     * @return the parameter name
     * @throws JSONException
     *             if the parameter name could not be parsed
     */
    private String parseParameterName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_name");
    }

    /**
     * Parses the parameter display name from a json.
     *
     * @param jsonObject
     *            the json from which the parameter display name is parsed
     * @return the parameter display name
     * @throws JSONException
     *             if the parameter display name could not be parsed
     */
    private String parseParameterDisplayName(JSONObject jsonObject) throws JSONException {
        return jsonObject.getString("parameter_display_name");
    }

    /**
     * Parses the parameter filter from a given json.
     *
     * @param jsonObject
     *            the json to parse from
     * @return the parsed filter
     * @throws JSONException
     *             if the filter could not be parsed
     */
    private SWEKParameterFilter parseParameterFilter(JSONObject jsonObject) throws JSONException {
        JSONObject filterobject = jsonObject.optJSONObject("filter");
        if (filterobject != null) {
            return new SWEKParameterFilter(parseFilterType(filterobject), parseMin(filterobject), parseMax(filterobject), parseStartValue(filterobject), parseStepSize(filterobject), parseUnits(filterobject), parseDbType(filterobject));
        }
        return null;
    }

    private String parseDbType(JSONObject filterobject) throws JSONException {
        return filterobject.getString("dbtype");
    }

    /**
     * Parses the filter units from the json.
     *
     * @param filterobject
     * @return
     * @throws JSONException
     */
    private String parseUnits(JSONObject filterobject) throws JSONException {
        return filterobject.getString("units");
    }

    /**
     * Parses the filter type from a json.
     *
     * @param object
     *            the json from which the filter type is parsed
     * @return the filter type
     * @throws JSONException
     *             if the filter type could not be parsed
     */
    private String parseFilterType(JSONObject object) throws JSONException {
        return object.getString("filter_type");
    }

    /**
     * parses the minimum filter value from a json.
     *
     * @param object
     *            the json from which to filter the minimum value
     * @return the minimum filter value
     * @throws JSONException
     *             if the minimum filter value could not be parsed
     */
    private double parseMin(JSONObject object) throws JSONException {
        return object.getDouble("min");
    }

    /**
     * Parses the maximum filter value from the json.
     *
     * @param object
     *            the json from which to parse the maximum filter value
     * @return the maximum filter value
     * @throws JSONException
     *             if the maximum filter value could not be parsed
     */
    private double parseMax(JSONObject object) throws JSONException {
        return object.getDouble("max");
    }

    /**
     * Parses the step size from the json.
     *
     * @param object
     *            the json from which to parse the step size
     * @return the step size
     * @throws JSONException
     *             if the step size could not be parsed
     */
    private double parseStepSize(JSONObject object) throws JSONException {
        return object.getDouble("step_size");
    }

    /**
     * Parses the start value from the json.
     *
     * @param object
     *            the json from which to parse the start value
     * @return the start value
     * @throws JSONException
     *             is the start value could not be parsed
     */
    private Double parseStartValue(JSONObject object) throws JSONException {
        return object.getDouble("start_value");
    }

    /**
     * Parses the default visible from the given json.
     *
     * @param jsonObject
     *            the json from which to parse
     * @return the parsed default visible
     * @throws JSONException
     *             if the default visible could not be parsed
     */
    private boolean parseDefaultVisible(JSONObject jsonObject) throws JSONException {
        return jsonObject.getBoolean("default_visible");
    }

    /**
     * Parses the request interval extension from the json.
     *
     * @param object
     *            the json from which to parse the request interval extension
     * @return the parsed request interval extension
     * @throws JSONException
     *             if the request interval extension could not be parsed
     */
    private Long parseRequestIntervalExtension(JSONObject object) throws JSONException {
        return object.getLong("request_interval_extension");
    }

    /**
     * Parses the standard selected from the json.
     *
     * @param object
     *            the json from which to parse the standard selected
     * @return true if standard selected is true in json, false if standard
     *         selected is false in json
     * @throws JSONException
     *             if the standard selected could not be parsed from json.
     */
    private boolean parseStandardSelected(JSONObject object) throws JSONException {
        return object.getBoolean("standard_selected");
    }

    /**
     * Parses the "group on" from the json.
     *
     * @param object
     *            the json from which to parse the "group on"
     * @return the group on parameter
     * @throws JSONException
     *             if the "group on" could not be parsed from the json.
     */
    private SWEKParameter parseGroupOn(JSONObject object) throws JSONException {
        if (!object.isNull("group_on")) {
            return parameters.get(object.getString("group_on"));
        } else {
            return null;
        }
    }

    /**
     * Parses the "coordinate_system" from the json.
     *
     * @param object
     *            the json from which to parse the "coordinate_system"
     * @return the coordinate system
     * @throws JSONException
     *             if the "coordinate_system" could not be parsed from the json
     */
    private String parseCoordinateSystem(JSONObject object) throws JSONException {
        return object.getString("coordinate_system");
    }

    /**
     * Parses the "spacial_region" from the json.
     *
     * @param object
     *            the object from which to parse the "spatial_region"
     * @return the spatial region
     * @throws JSONException
     *             if the "spatial_region" could not be parsed from the json
     */
    private SWEKSpatialRegion parseSpatialRegion(JSONObject object) throws JSONException {
        JSONObject jsonObject = object.getJSONObject("spatial_region");

        return new SWEKSpatialRegion(parseX1(jsonObject), parseY1(jsonObject), parseX2(jsonObject), parseY2(jsonObject));
    }

    /**
     * Parses the x1 coordinate of the spatial region from the json.
     *
     * @param jsonObject
     *            the object from which to parse the x1-coordinate
     * @return the x1 coordinate
     * @throws JSONException
     *             if the x1 coordinate could not be parsed from the json
     */
    private int parseX1(JSONObject jsonObject) throws JSONException {
        return jsonObject.getInt("x1");
    }

    /**
     * Parses the y1 coordinate of the spatial region from the json.
     *
     * @param jsonObject
     *            the object from which to parse the y1-coordinate
     * @return the y1 coordinate
     * @throws JSONException
     *             if the y1 coordinate could not be parsed from the json
     */
    private int parseY1(JSONObject jsonObject) throws JSONException {
        return jsonObject.getInt("y1");
    }

    /**
     * Parses the x2 coordinate of the spatial region from the json.
     *
     * @param jsonObject
     *            the object from which to parse the x2-coordinate
     * @return the x2 coordinate
     * @throws JSONException
     *             if the x2 coordinate could not be parsed
     */
    private int parseX2(JSONObject jsonObject) throws JSONException {
        return jsonObject.getInt("x2");
    }

    /**
     * Parses the y2 coordinate of the spatial region from the json.
     *
     * @param jsonObject
     *            the object from which to parse the y2 coordinate
     * @return the y2 coordinate
     * @throws JSONException
     *             if the y2 coordinate could not be parsed
     */
    private int parseY2(JSONObject jsonObject) throws JSONException {
        return jsonObject.getInt("y2");
    }

    /**
     * Parses the list of related events from a json.
     *
     * @param jsonObject
     *            the json from which to parse the list of related events
     * @return the parsed list of related events
     * @throws JSONException
     *             if the list of related events could not be parsed
     */
    private List<SWEKRelatedEvents> parseRelatedEvents(JSONObject configJSON) throws JSONException {
        List<SWEKRelatedEvents> relatedEventsList = new ArrayList<SWEKRelatedEvents>();
        JSONArray relatedEventsArray = configJSON.getJSONArray("related_events");
        for (int i = 0; i < relatedEventsArray.length(); i++) {
            relatedEventsList.add(parseRelatedEvent(relatedEventsArray.getJSONObject(i)));
        }
        return relatedEventsList;
    }

    /**
     * Parses related events from a json.
     *
     * @param jsonObject
     *            the json from which to parse related events
     * @return the parsed related events
     * @throws JSONException
     *             if the related events could not be parsed
     */
    private SWEKRelatedEvents parseRelatedEvent(JSONObject jsonObject) throws JSONException {
        return new SWEKRelatedEvents(parseRelatedEventName(jsonObject), parseRelatedWith(jsonObject), parseRelatedOnList(jsonObject));
    }

    /**
     * Parses a related event name from a json.
     *
     * @param jsonObject
     *            the json from which to parse the related event name
     * @return the parsed related event type
     * @throws JSONException
     *             if the related event name could not be parsed
     */
    private SWEKEventType parseRelatedEventName(JSONObject jsonObject) throws JSONException {
        return eventTypes.get(jsonObject.getString("event_name"));
    }

    /**
     * Parses a "related with" from a json.
     *
     * @param jsonObject
     *            the json from which to parse the "related with"
     * @return the parsed "related with" event type
     * @throws JSONException
     *             if the "related with" could not be parsed
     */
    private SWEKEventType parseRelatedWith(JSONObject jsonObject) throws JSONException {
        return eventTypes.get(jsonObject.getString("related_with"));
    }

    /**
     * Parses a list of "related on" from a json.
     *
     * @param jsonObject
     *            the json from which to parse the "related on" list
     * @return the parsed "related on" list
     * @throws JSONException
     *             if the "related on" list could not be parsed
     */
    private List<SWEKRelatedOn> parseRelatedOnList(JSONObject jsonObject) throws JSONException {
        List<SWEKRelatedOn> relatedOnList = new ArrayList<SWEKRelatedOn>();
        JSONArray relatedOnArray = jsonObject.getJSONArray("related_on");
        for (int i = 0; i < relatedOnArray.length(); i++) {
            relatedOnList.add(parseRelatedOn(relatedOnArray.getJSONObject(i)));
        }
        return relatedOnList;
    }

    /**
     * Parses a "related on" from a json.
     *
     * @param jsonObject
     *            the json from which to parse the "related on"
     * @return the parsed "related on"
     * @throws JSONException
     *             if the "related on" could not be parsed
     */
    private SWEKRelatedOn parseRelatedOn(JSONObject jsonObject) throws JSONException {
        return new SWEKRelatedOn(parseParameterFrom(jsonObject), parseParameterWith(jsonObject), parseDbType(jsonObject));
    }

    /**
     * Parses a "parameter from" from a json.
     *
     * @param jsonObject
     *            the json from which to parse the "parameter from"
     * @return the parsed "parameter from"
     * @throws JSONException
     *             if the "parameter from" could not be parsed
     */
    private SWEKParameter parseParameterFrom(JSONObject jsonObject) throws JSONException {
        String parameterName = jsonObject.getString("parameter_from");
        return new SWEKParameter("", parameterName, parameterName, null, false);
    }

    /**
     * Parses a "parameter with" from a json.
     *
     * @param jsonObject
     *            the json from which to parse the "parameter with"
     * @return the parsed "parameter with"
     * @throws JSONException
     *             if the "parameter with" could not be parsed
     */
    private SWEKParameter parseParameterWith(JSONObject jsonObject) throws JSONException {
        String parameterName = jsonObject.getString("parameter_with");
        return new SWEKParameter("", parameterName, parameterName, null, false);
    }

}
