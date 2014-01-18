/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */


package lcmc.data;

import lcmc.gui.DrbdGraph;
import lcmc.gui.resources.BlockDevInfo;
import lcmc.gui.resources.ProxyNetInfo;
import lcmc.utilities.Tools;
import lcmc.utilities.ConvertCmdCallback;
import lcmc.utilities.SSH;
import lcmc.Exceptions;
import lcmc.utilities.Unit;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.math.BigInteger;
import org.apache.commons.collections15.map.MultiKeyMap;
import org.apache.commons.collections15.map.LinkedMap;
import org.apache.commons.collections15.keyvalue.MultiKey;

import lcmc.utilities.Logger;
import lcmc.utilities.LoggerFactory;

/**
 * This class parses xml from drbdsetup and drbdadm, stores the
 * information in the hashes and provides methods to get this
 * information.
 * The xml is obtained with drbdsetp xml command and drbdadm dump-xml.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class DrbdXML extends XML {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DrbdXML.class);
    // TODO: should that not be per host?
    /** Drbd config filename. */
    private String configFile = "unknown";
    /** Map from parameter name to the default value. */
    private final Map<String, Value> paramDefaultMap =
                                               new HashMap<String, Value>();
    /** Map from parameter name to its type. */
    private final Map<String, String> paramTypeMap =
                                                new HashMap<String, String>();
    /** Map from parameter name to its section. */
    private final Map<String, String> paramSectionMap =
                                            new LinkedHashMap<String, String>();
    /** Map from section to this section's parameters. */
    private final Map<String, List<String>> sectionParamsMap =
                                      new LinkedHashMap<String, List<String>>();
    /** Map from parameter name to its unit name (long). */
    private final Map<String, String> paramUnitLongMap =
                                           new LinkedHashMap<String, String>();

    /** Map from parameter name to its unit name. */
    private final Map<String, String> paramDefaultUnitMap =
                                           new LinkedHashMap<String, String>();

    /** Map from parameter name to its long description. */
    private final Map<String, String> paramLongDescMap =
                                                new HashMap<String, String>();

    /** Map from parameter name to its minimum value. */
    private final Map<String, BigInteger> paramMinMap =
                                    new LinkedHashMap<String, BigInteger>();

    /** Map from parameter name to its maximum value. */
    private final Map<String, BigInteger> paramMaxMap =
                                        new LinkedHashMap<String, BigInteger>();

    /** Map from parameter name to its correct value. */
    private final Map<String, Boolean> paramCorrectValueMap =
                                                new HashMap<String, Boolean>();

    /** Map from parameter name to its items if there is a choice list. */
    private final Map<String, List<Value>> paramItemsMap =
                                    new LinkedHashMap<String, List<Value>>();
    /** List of all parameters. */
    private final List<String> parametersList = new ArrayList<String>();
    /** List of all global parameters. */
    private final List<String> globalParametersList = new ArrayList<String>();
    /** List of all required parameters. */
    private final List<String> requiredParametersList =
                                                    new ArrayList<String>();
    /** Map from resource option to the value. */
    private final Map<String, Map<String, Value>> optionsMap =
                                    new HashMap<String, Map<String, Value>>();

    /** List with drbd resources. */
    private final List<String> resourceList = new ArrayList<String>();
    /** Map from drbd resource name to the drbd device. */
    private final MultiKeyMap<String, String> resourceDeviceMap =
             MultiKeyMap.decorate(new LinkedMap<MultiKey<String>, String>());
    /** Map from drbd device to the drbd resource name. */
    private final Map<String, String> deviceResourceMap =
                                                new HashMap<String, String>();
    /** Map from drbd device to the drbd volume. */
    private final Map<String, String> deviceVolumeMap =
                                                new HashMap<String, String>();

    /** Map from drbd resource name and the host to the block device. */
    private final MultiKeyMap<String, Map<String, String>> resourceHostDiskMap =
                                new MultiKeyMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the ip. */
    private final Map<String, Map<String, String>> resourceHostIpMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the port. */
    private final Map<String, Map<String, String>> resourceHostPortMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the family. */
    private final Map<String, Map<String, String>> resourceHostFamilyMap =
                                    new HashMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the meta disk. */
    private final MultiKeyMap<String, Map<String, String>>
                        resourceHostMetaDiskMap =
                                new MultiKeyMap<String, Map<String, String>>();
    /** Map from drbd resource name and the host to the meta disk index. */
    private final MultiKeyMap<String, Map<String, String>>
                        resourceHostMetaDiskIndexMap =
                                new MultiKeyMap<String, Map<String, String>>();
    /** Map from resource and host to the proxy information. */
    private final MultiKeyMap<String, HostProxy> resourceHostProxyMap =
                                         new MultiKeyMap<String, HostProxy>();
    /** Set of all proxy hosts. */
    private final Set<String> proxyHostNames = new LinkedHashSet<String>();
    /** Map from host to the boolean value if drbd is loaded on this host. */
    private final Map<String, Boolean> hostDrbdLoadedMap =
                                                new HashMap<String, Boolean>();
    /** Whether there are unknown sections in the config. */
    private boolean unknownSections = false;
    /** Old config. */
    private String oldConfig = null;

    private static final Pattern UNIT_PATTERN =
                                     Pattern.compile("(\\d*)([kmgtsKMGTS]?)");
    /** Global section. */
    public static final String GLOBAL_SECTION = "global";
    /** DRBD protocol A. */
    public static final Value PROTOCOL_A =
                                    new StringValue("A", "A / Asynchronous");

    public static final Value PROTOCOL_B =
                                new StringValue("B", "B / Semi-Synchronous");
    /** DRBD protocol C, that is a default. */
    public static final Value PROTOCOL_C =
                                     new StringValue("C", "C / Synchronous");
    /** Protocol parameter. */
    public static final String PROTOCOL_PARAM = "protocol";
    /** Ping timeout parameter. */
    public static final String PING_TIMEOUT_PARAM = "ping-timeout";
    /** DRBD communication protocols. */
    static final Value[] PROTOCOLS = {PROTOCOL_A, PROTOCOL_B, PROTOCOL_C};
    /** Some non advanced parameters. */
    static final List<String> NOT_ADVANCED_PARAMS = new ArrayList<String>();
    static {
        NOT_ADVANCED_PARAMS.add("rate");
        NOT_ADVANCED_PARAMS.add(PROTOCOL_PARAM);
        NOT_ADVANCED_PARAMS.add(PING_TIMEOUT_PARAM);
        NOT_ADVANCED_PARAMS.add("fence-peer");
        NOT_ADVANCED_PARAMS.add("wfc-timeout");
        NOT_ADVANCED_PARAMS.add("degr-wfc-timeout");
        NOT_ADVANCED_PARAMS.add("become-primary-on");
        NOT_ADVANCED_PARAMS.add("timeout");
        NOT_ADVANCED_PARAMS.add("allow-two-primaries");
        NOT_ADVANCED_PARAMS.add("fencing");
        NOT_ADVANCED_PARAMS.add("after"); /* before 8.4 */
        NOT_ADVANCED_PARAMS.add("resync-after");
        NOT_ADVANCED_PARAMS.add("usage-count"); /* global */
        NOT_ADVANCED_PARAMS.add("memlimit"); /* proxy */
        NOT_ADVANCED_PARAMS.add("plugin-zlib"); /* proxy */
        NOT_ADVANCED_PARAMS.add("plugin-lzma"); /* proxy */
    }

    /** Drbd config "errors" that are to be ignored. */
    static final Set<String> IGNORE_CONFIG_ERRORS = new HashSet<String>();
    static {
        IGNORE_CONFIG_ERRORS.add("no resources defined!");
        IGNORE_CONFIG_ERRORS.add(
                  "Can not open '/etc/drbd.conf': No such file or directory");
    }
    /** Access types of some parameters. */
    static final Map<String, ConfigData.AccessType> PARAM_ACCESS_TYPE =
                                new HashMap<String, ConfigData.AccessType>();
    static {
        PARAM_ACCESS_TYPE.put("rate", ConfigData.AccessType.OP);
    }

    /** Preferred values. */
    private static final Map<String, Value> PREFERRED_MAP =
                                                 new HashMap<String, Value>();
    static {
        PREFERRED_MAP.put(PROTOCOL_PARAM, PROTOCOL_C);
    }

    /** Yes / true drbd config value. */
    public static final Value CONFIG_YES = new StringValue("yes");
    /** No / false drbd config value. */
    public static final Value CONFIG_NO = new StringValue("no");
    /** Hardcoded defaults, for options that have it but we don't get
        it from the drbdsetup. */
    static final Map<String, Value> HARDCODED_DEFAULTS =
                                                new HashMap<String, Value>();
    static {
        HARDCODED_DEFAULTS.put("usage-count", new StringValue());
        HARDCODED_DEFAULTS.put("disable-ip-verification", CONFIG_NO);

        HARDCODED_DEFAULTS.put(PROTOCOL_PARAM, PROTOCOL_C);
        HARDCODED_DEFAULTS.put("after-sb-0pri", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("after-sb-1pri", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("after-sb-2pri", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("rr-conflict", new StringValue("disconnect"));
        HARDCODED_DEFAULTS.put("on-io-error", new StringValue("pass_on"));
        HARDCODED_DEFAULTS.put("fencing", new StringValue("dont-care"));
        HARDCODED_DEFAULTS.put("on-no-data-accessible", new StringValue("io-error"));
        HARDCODED_DEFAULTS.put("on-congestion", new StringValue("block"));
    }

    /** Prepares a new <code>DrbdXML</code> object. */
    public DrbdXML(final Host[] hosts, final Map<Host, String> drbdParameters) {
        super();
        addSpecialParameter("resource", "name", true);
        for (final Host host : hosts) {
            String output;
            if (drbdParameters.get(host) == null) {
                output = updateDrbdParameters(host);
                drbdParameters.put(host, output);
                if (output == null) {
                    return;
                }
            } else {
                output = drbdParameters.get(host);
            }
            /* TODO: move this part somewhere else, it should be called
               once per invocation or interactively. (drbd-get-xml) */
            parseDrbdParameters(host, output, hosts);
        }
    }

    public String updateDrbdParameters(final Host host) {
        final String command = host.getDistCommand("Drbd.getParameters",
                                                   (ConvertCmdCallback) null);

        final SSH.SSHOutput ret =
                              Tools.execCommand(host,
                                                command,
                                                null,   /* ExecCallback */
                                                false,  /* outputVisible */
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
        if (ret.getExitCode() != 0) {
            return null;
        }
        return ret.getOutput();
    }

    public void parseDrbdParameters(final Host host,
                                    final String output,
                                    final Host[] hosts) {
        final String[] lines = output.split("\\r?\\n");
        final Pattern bp = Pattern.compile("^<command name=\"(.*?)\".*");
        final Pattern ep = Pattern.compile("^</command>$");
        final StringBuilder xml = new StringBuilder();
        String section = null;

        for (final String line : lines) {
            final Matcher m = bp.matcher(line);
            if (m.matches()) {
                section = m.group(1);
            }
            if (section != null) {
                xml.append(line);
                xml.append('\n');
                final Matcher m2 = ep.matcher(line);
                if (m2.matches()) {
                    parseSection(section, xml.toString(), host, hosts);
                    section = null;
                    xml.delete(0, xml.length());
                }
            }
        }
        if (!parametersList.contains(PROTOCOL_PARAM)) {
            /* prior 8.4 */
            addParameter("resource",
                         PROTOCOL_PARAM,
                         new StringValue(),
                         PROTOCOLS,
                         true);
        }
    }

    /** Returns the filename of the drbd config file. */
    String getConfigFile() {
        return configFile;
    }

    /** Returns config from server. */
    public String getConfig(final Host host) {
        if (!host.isConnected()) {
            return null;
        }
        final String command2 = host.getDistCommand("Drbd.getConfig",
                                                    (ConvertCmdCallback) null);
        final SSH.SSHOutput ret = Tools.execCommand(
                                                host,
                                                command2,
                                                null,   /* ExecCallback */
                                                false,  /* outputVisible */
                                                SSH.DEFAULT_COMMAND_TIMEOUT);
        if (ret.getExitCode() == 0) {
            final StringBuffer confSB = new StringBuffer(ret.getOutput());
            return host.getOutput("drbd", confSB);
        }
        return null;
    }

    /**
     * Retrieves and updates all the data. This should be called if there is
     * a drbd event.
     */
    public void update(final String configString) {
        if (configString != null && !configString.isEmpty()) {
            oldConfig = configString;
            parseConfig(configString);
        }
    }

    /** Returns all drbd parameters. */
    public List<String> getParameters() {
        return parametersList;
    }

    /** Gets short description for the parameter. */
    public String getParamShortDesc(final String param) {
        final StringBuilder name =
                                new StringBuilder(param.replaceAll("\\-", " "));
        name.replace(0, 1, name.substring(0, 1).toUpperCase());
        if (paramUnitLongMap.containsKey(param)) {
            name.append(" (").append(paramUnitLongMap.get(param)).append(")");
        }
        return name.toString();
    }

    /** Gets long description for the parameter. */
    public String getParamLongDesc(final String param) {
        return paramLongDescMap.get(param);
    }

    /** Returns the long name of the unit of the specified parameter. */
    public String getUnitLong(final String param) {
        return paramUnitLongMap.get(param);
    }

    /** Returns the default unit of the specified parameter. */
    public String getDefaultUnit(final String param) {
        return paramDefaultUnitMap.get(param);
    }

    /** Returns whether the parameter has a unit prefix. */
    public boolean hasUnitPrefix(final String param) {
        final String unit = paramUnitLongMap.get(param);
        return paramDefaultUnitMap.containsKey(param)
               && (unit == null
                   || "bytes".equals(unit)
                   || "bytes/second".equals(unit));
    }

    /**
     * Returns parameter type, that is one of the following:
     *  numeric
     *  string
     *  boolean
     *  handle
     * .
     */
    public String getParamType(final String param) {
        return paramTypeMap.get(param);
    }

    /** Gets default for the parameter. */
    public Value getParamDefault(final String param) {
        final Value defaultValue = paramDefaultMap.get(param);

        //if (hasUnitPrefix(param)) {
        //    final String unit = getDefaultUnit(param);
        //    return new StringValue(defaultValue, unit);
        //}
        return defaultValue;
    }

    /** Gets preferred value for the parameter. */
    public Value getParamPreferred(final String param) {
        return PREFERRED_MAP.get(param);
    }

    /** Returns section in which this param is in. */
    public String getSection(final String param) {
        return paramSectionMap.get(param);
    }

    /**
     * Checks parameter according to its type. Returns false if value is wrong.
     */
    public boolean checkParam(final String param, final Value rawValue) {
        final String type = paramTypeMap.get(param);
        boolean correctValue = true;

        Value value = rawValue;
        //String unit = null;
        //if (rawValue != null && hasUnitPrefix(param)) {
        //    /* number with unit */
        //    final Pattern p = Pattern.compile("\\d*([kmgtsKMGTS])");
        //    final Matcher m = p.matcher(rawValue.getValueForConfig());
        //    if (m.matches()) {
        //        /* remove unit from value */
        //        unit = m.group(1).toUpperCase();
        //        value = new StringValue(rawValue.getValueForConfig().substring(0,
        //                    rawValue.getValueForConfig().length() - unit.length())); //TODO:
        //    }
        //}

        if (value == null || value.isNothingSelected()) {
            if (isRequired(param)) {
                correctValue = false;
            } else {
                correctValue = true;
            }
        } else if ("boolean".equals(type)) {
            if (!value.equals(CONFIG_YES)
                && !value.equals(CONFIG_NO)) {
                correctValue = false;
            }
        } else if ("numeric".equals(type)) {
            final Pattern p = Pattern.compile("(-?\\d+)|\\d*");
            final Matcher m = p.matcher(value.getValueForConfig());
            if (!m.matches()) {
                correctValue = false;
            //} else if ((unit == null //TODO:
            //            || "k".equalsIgnoreCase(unit)
            //            || "m".equalsIgnoreCase(unit)
            //            || "g".equalsIgnoreCase(unit)
            //            || "t".equalsIgnoreCase(unit))
            //           && "K".equalsIgnoreCase(getDefaultUnit(param))) {
            //    /* except sectors */
            //    long v;
            //    if (unit == null) {
            //        v = Long.parseLong(rawValue.getValueForConfig()) / 1024;
            //    } else {
            //        v = Tools.convertToKilobytes(rawValue.getValueForConfig());
            //    }
            //    if (paramMaxMap.get(param) != null
            //        && v > paramMaxMap.get(param).longValue()) {
            //        correctValue = false;
            //    } else if (paramMinMap.get(param) != null
            //               && v < paramMinMap.get(param).longValue()) {
            //        correctValue = false;
            //    }
            //} else if (!"s".equalsIgnoreCase(unit)) {
            //    final long v = Tools.convertUnits(rawValue.getValueForConfig());
            //    if (paramMaxMap.get(param) != null
            //        && v > paramMaxMap.get(param).longValue()) {
            //        correctValue = false;
            //    } else if (paramMinMap.get(param) != null
            //               && v < paramMinMap.get(param).longValue()) {
            //        correctValue = false;
            //    }
            }
        } else {
            correctValue = true;
        }
        paramCorrectValueMap.put(param, correctValue);
        return correctValue;
    }

    /** Returns whether parameter expects integer value. */
    public boolean isInteger(final String param) {
        final String type = paramTypeMap.get(param);
        return "numeric".equals(type);
    }

    /** Returns whether parameter is read only. */
    public boolean isLabel(final String param) {
        return false;
    }

    /** Returns whether parameter expects string value. */
    public boolean isStringType(final String param) {
        final String type = paramTypeMap.get(param);
        return "string".equals(type);
    }

    /** Checks in the cache if the parameter was correct. */
    boolean checkParamCache(final String param) {
        return paramCorrectValueMap.get(param).booleanValue();
    }

    /**
     * Adds parameter to the specified section. This parameter will be not
     * used in the generated config.
     */
    private void addSpecialParameter(final String section,
                                     final String param,
                                     final boolean required) {
        if (!parametersList.contains(param)) {
            parametersList.add(param);
            if (required) {
                requiredParametersList.add(param);
            }

            paramTypeMap.put(param, "string");
            paramSectionMap.put(param, section);
        }
    }

    /** Add parameter with choice combo box. */
    private void addParameter(final String section,
                              final String param,
                              final Value defaultValue,
                              final Value[] items,
                              final boolean required) {
        addParameter(section, param, defaultValue, required);
        final List<Value> l = new ArrayList<Value>();
        for (int i = 0; i < items.length; i++) {
            if (!l.contains(items[i])) {
                l.add(items[i]);
            }
        }
        paramItemsMap.put(param, l);
        paramTypeMap.put(param, "handler");
    }

    /** Adds parameter to the specified section. */
    private void addParameter(final String section,
                              final String param,
                              final boolean required) {
        addSpecialParameter(section, param, required);
        sectionParamsMap.put(section, new ArrayList<String>());
        sectionParamsMap.get(section).add(param);
    }

    /** Adds parameter with a default value to the specified section. */
    private void addParameter(final String section,
                              final String param,
                              final Value defaultValue,
                              final boolean required) {
        addParameter(section, param, required);
        paramDefaultMap.put(param, defaultValue);
    }

    /** Adds parameter with the specified type. */
    private void addParameter(final String section,
                              final String param,
                              final Value defaultValue,
                              final boolean required,
                              final String type) {
        addParameter(section, param, defaultValue, required);
        paramTypeMap.put(param, type);
    }

    /** Returns array with all the sections. */
    public String[] getSections() {
        return sectionParamsMap.keySet().toArray(
                                        new String[sectionParamsMap.size()]);
    }

    /** Returns parameters for the specified section. */
    public String[] getSectionParams(final String section) {
        final List<String> params = sectionParamsMap.get(section);
        if (params == null) {
            return new String[0];
        }
        return params.toArray(new String[params.size()]);
    }

    /** Returns parameters for the global section. */
    public List<String> getGlobalParams() {
        return globalParametersList;
    }

    /** Returns possible choices. */
    public Value[] getPossibleChoices(final String param) {
        final List<Value> items = paramItemsMap.get(param);
        if (items == null) {
            return null;
        } else {
            return items.toArray(new Value[items.size()]);
        }
    }

    /** Returns whether parameter is required. */
    public boolean isRequired(final String param) {
        return requiredParametersList.contains(param);
    }

    /** Returns whether parameter is advanced. */
    public boolean isAdvanced(final String param) {
        return !isRequired(param)
               && !NOT_ADVANCED_PARAMS.contains(param);
    }

    /** Returns access type of the parameter. */
    public ConfigData.AccessType getAccessType(final String param) {
        final ConfigData.AccessType at = PARAM_ACCESS_TYPE.get(param);
        if (at == null) {
          return ConfigData.AccessType.ADMIN;
        }
        return at;
    }

    /** Parses the global node in the drbd config. */
    private void parseConfigGlobalNode(final Node globalNode,
                                       final Map<String, Value> nameValueMap) {
        final NodeList options = globalNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            final String nodeName = option.getNodeName();
            if ("dialog-refresh".equals(nodeName)) {
                final String dialogRefresh = getAttribute(option, "refresh");
                nameValueMap.put(nodeName, new StringValue(dialogRefresh));
            } else if ("minor-count".equals(nodeName)) {
                final String minorCount = getAttribute(option, "count");
                nameValueMap.put(nodeName, new StringValue(minorCount));
            } else if ("disable-ip-verification".equals(nodeName)) {
                nameValueMap.put(nodeName, CONFIG_YES);
            } else if ("usage-count".equals(nodeName)) {
                /* does not come from drbdadm */
                /* TODO: "count" is guessed. */
                final String usageCount = getAttribute(option, "count");
                if (usageCount != null) {
                    nameValueMap.put(nodeName, new StringValue(usageCount));
                }
            }
        }
    }


    /** Parses command xml for parameters and fills up the hashes. */
    private void parseSection(final String section,
                              final String xml,
                              final Host host,
                              final Host[] hosts) {
        final Document document = getXMLDocument(xml);

        /* get root <command> */
        final Node commandNode = getChildNode(document, "command");
        if (commandNode == null) {
            return;
        }
        final NodeList options = commandNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node optionNode = options.item(i);

            /* <option> */
            if (optionNode.getNodeName().equals("option")) {
                final String name = getAttribute(optionNode, "name");
                final String type = getAttribute(optionNode, "type");
                if ("flag".equals(type)) {
                    /* ignore flags */
                    continue;
                }
                if ("handler".equals(type)) {
                    final List<Value> items = new ArrayList<Value>();
                    items.add(new StringValue());
                    paramItemsMap.put(name, items);
                    paramDefaultMap.put(name, HARDCODED_DEFAULTS.get(name));
                } else if ("boolean".equals(type)) {
                    final List<Value> l = new ArrayList<Value>();
                    l.add(CONFIG_YES);
                    l.add(CONFIG_NO);
                    paramItemsMap.put(name, l);
                    paramDefaultMap.put(name, CONFIG_NO);
                }
                if ("fence-peer".equals(name)) {
                    final List<Value> l = new ArrayList<Value>();
                    l.add(new StringValue());
                    if (!"".equals(host.getArch())) {
                        l.add(new StringValue(host.getHeartbeatLibPath()
                                              + "/drbd-peer-outdater -t 5"));
                    }
                    l.add(new StringValue("/usr/lib/drbd/crm-fence-peer.sh"));
                    paramItemsMap.put(name, l);
                } else if ("after-resync-target".equals(name)) {
                    final List<Value> l = new ArrayList<Value>();
                    l.add(new StringValue());
                    l.add(new StringValue(
                                       "/usr/lib/drbd/crm-unfence-peer.sh"));
                    paramItemsMap.put(name, l);
                } else if ("split-brain".equals(name)) {
                    final List<Value> l = new ArrayList<Value>();
                    l.add(new StringValue());
                    l.add(new StringValue(
                                "/usr/lib/drbd/notify-split-brain.sh root"));
                    paramItemsMap.put(name, l);
                } else if ("become-primary-on".equals(name)) {
                    final List<Value> l = new ArrayList<Value>();
                    l.add(new StringValue());
                    l.add(new StringValue("both"));
                    for (final Host h : hosts) {
                        l.add(new StringValue(h.getName()));
                    }
                    paramItemsMap.put(name, l);
                } else if ("verify-alg".equals(name)
                           || "csums-alg".equals(name)
                           || "data-integrity-alg".equals(name)
                           || "cram-hmac-alg".equals(name)) {
                    final List<Value> l = new ArrayList<Value>();
                    l.add(new StringValue());
                    for (final String cr : host.getCryptoModules()) {
                        l.add(new StringValue(cr));
                    }
                    paramItemsMap.put(name, l);
                }
                final NodeList optionInfos = optionNode.getChildNodes();
                for (int j = 0; j < optionInfos.getLength(); j++) {
                    final Node optionInfo = optionInfos.item(j);
                    final String tag = optionInfo.getNodeName();
                    /* <min>, <max>, <handler>, <default> */
                    if ("min".equals(tag)) {
                        paramMinMap.put(name,
                                        new BigInteger(getText(optionInfo)));
                    } else if ("max".equals(tag)) {
                        paramMaxMap.put(name,
                                        new BigInteger(getText(optionInfo)));
                    } else if ("handler".equals(tag)) {
                        paramItemsMap.get(name).add(
                                       new StringValue(getText(optionInfo)));
                    } else if ("default".equals(tag)) {
                        paramDefaultMap.put(
                               name,
                               new StringValue(
                                   getText(optionInfo),
                                   parseUnit(name,
                                             paramDefaultUnitMap.get(name))));
                    } else if ("unit".equals(tag)) {
                        paramUnitLongMap.put(name, getText(optionInfo));
                    } else if ("unit_prefix".equals(tag)) {
                        if (!"after".equals(name)
                            && !"resync-after".equals(name)) {
                            String option = getText(optionInfo);
                            if (!"s".equals(option)) {
                                /* "s" is an exception */
                                option = option.toUpperCase(Locale.US);
                            }
                            if ("1".equals(option)) {
                                option = "";
                            }
                            paramDefaultUnitMap.put(name, option);
                        }
                    } else if ("desc".equals(tag)) {
                        paramLongDescMap.put(name, getText(optionInfo));
                    }
                }
                paramTypeMap.put(name, type);
                if (!GLOBAL_SECTION.equals(section)
                    && !parametersList.contains(name)) {
                    parametersList.add(name);
                }
                if (!"resource".equals(section)
                    && !globalParametersList.contains(name)
                    && !("syncer".equals(section) && "after".equals(name))
                    && !"resync-after".equals(name)) {
                    globalParametersList.add(name);
                }

                paramSectionMap.put(name, section);
                if (!sectionParamsMap.containsKey(section)) {
                    sectionParamsMap.put(section, new ArrayList<String>());
                }
                if (!sectionParamsMap.get(section).contains(name)) {
                    sectionParamsMap.get(section).add(name);
                }
            }
        }
    }

    /** Parses section node and creates map with option name value pairs. */
    private void parseConfigSectionNode(
                                    final Node sectionNode,
                                    final Map<String, Value> nameValueMap) {
        final NodeList options = sectionNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("option")) {
                final String name = getAttribute(option, "name");
                final String valueS = getAttribute(option, "value");

                Value value;
                if (valueS == null) { /* boolean option */
                    value = CONFIG_YES;
                } else if (hasUnitPrefix(name)) {
                    value = parseValue(name, valueS);
                } else {
                    value = new StringValue(valueS);
                }
                nameValueMap.put(name, value);
            } else if (option.getNodeName().equals("section")) {
                final String name = getAttribute(option, "name");
                if ("plugin".equals(name)) {
                    /* proxy */
                    parseProxyPluginNode(option.getChildNodes(), nameValueMap);
                }
            }
        }
    }

    /** Parse proxy XML plugin section. */
    private void parseProxyPluginNode(final NodeList options,
                                      final Map<String, Value> nameValueMap) {
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("option")) {
                final String nameValues = getAttribute(option, "name");
                final int spacePos = nameValues.indexOf(' ');
                String name;
                String value;
                if (spacePos > 0) {
                    name = DrbdProxy.PLUGIN_PREFIX
                           + nameValues.substring(0, spacePos);
                    value = nameValues.substring(spacePos + 1,
                                                 nameValues.length());
                } else {
                    /* boolean */
                    name = DrbdProxy.PLUGIN_PREFIX + nameValues;
                    value = CONFIG_YES.getValueForConfig();
                }
                nameValueMap.put(name, new StringValue(value));
            }
        }
    }

    /** Parses host node in the drbd config. */
    private void parseHostConfig(final String resName, final Node hostNode) {
        final String hostName = getAttribute(hostNode, "name");
        parseVolumeConfig(hostName, resName, hostNode); /* before 8.4 */
        final NodeList options = hostNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("volume")) {
                parseVolumeConfig(hostName, resName, option);
            } else if (option.getNodeName().equals("address")) {
                final String ip = getText(option);
                final String port = getAttribute(option, "port");
                final String family = getAttribute(option, "family");
                /* ip */
                Map<String, String> hostIpMap = resourceHostIpMap.get(resName);
                if (hostIpMap == null) {
                    hostIpMap = new HashMap<String, String>();
                    resourceHostIpMap.put(resName, hostIpMap);
                }
                hostIpMap.put(hostName, ip);
                /* port */
                Map<String, String> hostPortMap =
                                            resourceHostPortMap.get(resName);
                if (hostPortMap == null) {
                    hostPortMap = new HashMap<String, String>();
                    resourceHostPortMap.put(resName, hostPortMap);
                }
                hostPortMap.put(hostName, port);
                /* family */
                Map<String, String> hostFamilyMap =
                                            resourceHostFamilyMap.get(resName);
                if (hostFamilyMap == null) {
                    hostFamilyMap = new HashMap<String, String>();
                    resourceHostFamilyMap.put(resName, hostFamilyMap);
                }
                hostFamilyMap.put(hostName, family);
            } else if (option.getNodeName().equals("proxy")) {
                parseProxyHostConfig(hostName, resName, option);
            }
        }
    }

    /** Parses host node in the drbd config. */
    private void parseVolumeConfig(final String hostName,
                                   final String resName,
                                   final Node volumeNode) {
        String volumeNr = getAttribute(volumeNode, "vnr");
        if (volumeNr == null) {
            volumeNr = "0";
        }
        final NodeList options = volumeNode.getChildNodes();
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("device")) {
                String device = getText(option);
                if (device != null && "".equals(device)) {
                    final String minor = getAttribute(option, "minor");
                    device = "/dev/drbd" + minor;
                }
                resourceDeviceMap.put(resName, volumeNr, device);
                deviceResourceMap.put(device, resName);
                deviceVolumeMap.put(device, volumeNr);
            } else if (option.getNodeName().equals("disk")) {
                final String disk = getText(option);
                Map<String, String> hostDiskMap =
                                    resourceHostDiskMap.get(resName, volumeNr);
                if (hostDiskMap == null) {
                    hostDiskMap = new HashMap<String, String>();
                    resourceHostDiskMap.put(resName, volumeNr, hostDiskMap);
                }
                hostDiskMap.put(hostName, disk);
            } else if (option.getNodeName().equals("meta-disk")
                       || option.getNodeName().equals("flexible-meta-disk")) {
                final boolean flexible =
                            option.getNodeName().equals("flexible-meta-disk");
                final String metaDisk = getText(option);
                String metaDiskIndex = null;

                if (!flexible) {
                    metaDiskIndex = getAttribute(option, "index");
                }

                if (metaDiskIndex == null) {
                    metaDiskIndex = "Flexible";
                }
                /* meta-disk */
                Map<String, String> hostMetaDiskMap =
                                resourceHostMetaDiskMap.get(resName, volumeNr);
                if (hostMetaDiskMap == null) {
                    hostMetaDiskMap = new HashMap<String, String>();
                    resourceHostMetaDiskMap.put(resName,
                                                volumeNr,
                                                hostMetaDiskMap);
                }
                hostMetaDiskMap.put(hostName, metaDisk);

                /* meta-disk index */
                Map<String, String> hostMetaDiskIndexMap =
                           resourceHostMetaDiskIndexMap.get(resName, volumeNr);
                if (hostMetaDiskIndexMap == null) {
                    hostMetaDiskIndexMap = new HashMap<String, String>();
                    resourceHostMetaDiskIndexMap.put(resName,
                                                     volumeNr,
                                                     hostMetaDiskIndexMap);
                }
                hostMetaDiskIndexMap.put(hostName, metaDiskIndex);
            } else if (option.getNodeName().equals("address")) {
                /* since 8.4, it's outside of volume */
                final String ip = getText(option);
                final String port = getAttribute(option, "port");
                final String family = getAttribute(option, "family");
                /* ip */
                Map<String, String> hostIpMap = resourceHostIpMap.get(resName);
                if (hostIpMap == null) {
                    hostIpMap = new HashMap<String, String>();
                    resourceHostIpMap.put(resName, hostIpMap);
                }
                hostIpMap.put(hostName, ip);
                /* port */
                Map<String, String> hostPortMap =
                                            resourceHostPortMap.get(resName);
                if (hostPortMap == null) {
                    hostPortMap = new HashMap<String, String>();
                    resourceHostPortMap.put(resName, hostPortMap);
                }
                hostPortMap.put(hostName, port);
                /* family */
                Map<String, String> hostFamilyMap =
                                            resourceHostFamilyMap.get(resName);
                if (hostFamilyMap == null) {
                    hostFamilyMap = new HashMap<String, String>();
                    resourceHostFamilyMap.put(resName, hostFamilyMap);
                }
                hostFamilyMap.put(hostName, family);
            } else if (option.getNodeName().equals("proxy")) {
                parseProxyHostConfig(hostName, resName, option);
            }
        }
    }

    /** Parses proxy config in the host section. */
    private void parseProxyHostConfig(final String hostName,
                                      final String resName,
                                      final Node proxyNode) {
        final String proxyHostName = getAttribute(proxyNode, "hostname");
        final NodeList options = proxyNode.getChildNodes();
        String insideIp = null;
        String insidePort = null;
        String outsideIp = null;
        String outsidePort = null;
        for (int i = 0; i < options.getLength(); i++) {
            final Node option = options.item(i);
            if (option.getNodeName().equals("inside")) {
                insideIp = getText(option);
                insidePort = getAttribute(option, "port");
            } else if (option.getNodeName().equals("outside")) {
                outsideIp = getText(option);
                outsidePort = getAttribute(option, "port");
            }
        }
        resourceHostProxyMap.put(resName, hostName, new HostProxy(proxyHostName,
                                                                  new StringValue(insideIp),
                                                                  new StringValue(insidePort),
                                                                  new StringValue(outsideIp),
                                                                  new StringValue(outsidePort)));
        proxyHostNames.add(proxyHostName);
    }

    /** Returns map with hosts as keys and disks as values. */
    public Map<String, String> getHostDiskMap(final String resName,
                                              final String volumeNr) {
        return resourceHostDiskMap.get(resName, volumeNr);
    }

    /** Returns map with hosts as keys and ips as values. */
    Map<String, String> getHostIpMap(final String resName) {
        return resourceHostIpMap.get(resName);
    }

    /** Gets virtual net interface port for a host and a resource. */
    public String getVirtualInterfacePort(final String hostName,
                                                    final String resName) {
        if (resourceHostPortMap.containsKey(resName)) {
            return resourceHostPortMap.get(resName).get(hostName);
        }
        return null;
    }

    /** Gets virtual net interface family for a host and a resource. */
    public String getVirtualInterfaceFamily(final String hostName,
                                            final String resName) {
        if (resourceHostFamilyMap.containsKey(resName)) {
            return resourceHostFamilyMap.get(resName).get(hostName);
        }
        return null;
    }

    /** Gets virtual net interface for a host and a resource. */
    public String getVirtualInterface(final String hostName,
                                      final String resName) {
        if (resourceHostIpMap.containsKey(resName)) {
            final String ip = resourceHostIpMap.get(resName).get(hostName);
            final HostProxy hostProxy = getHostProxy(hostName, resName);
            if (hostProxy == null) {
                return ip;
            }
            final String proxyHostName = hostProxy.getProxyHostName();
            return ProxyNetInfo.displayString(ip, hostName, proxyHostName);
        }
        return null;
    }

    /** Gets virtual net interface for a host and a resource. */
    public HostProxy getHostProxy(final String hostName,
                                  final String resName) {
        return resourceHostProxyMap.get(resName, hostName);
    }

    /** Get all proxy hosts. */
    public Set<String> getProxyHostNames() {
        return proxyHostNames;
    }

    /** Returns whether a proxy is defined for this host and resource. */
    public boolean isHostProxy(final String hostName, final String resName) {
        return resourceHostProxyMap.containsKey(resName, hostName);
    }

    /** Gets meta-disk block device for a host and a resource. */
    public String getMetaDisk(final String hostName,
                              final String resName,
                              final String volumeNr) {
        final Map<String, String> hostMetaDiskMap =
                                resourceHostMetaDiskMap.get(resName, volumeNr);
        if (hostMetaDiskMap != null) {
            return hostMetaDiskMap.get(hostName);
        }
        return null;
    }

    /**
     * Gets meta-disk index for a host and a resource. Index can
     * be keyword 'flexible'. In this case 'flexible-meta-disk option
     * will be generated in the config.
     */
    public String getMetaDiskIndex(final String hostName,
                                   final String resName,
                                   final String volumeNr) {
        final Map<String, String> hostMetaDiskIndexMap =
                           resourceHostMetaDiskIndexMap.get(resName, volumeNr);
        if (hostMetaDiskIndexMap != null) {
            return hostMetaDiskIndexMap.get(hostName);
        }
        return null;
    }

    /** Parses resource xml. */
    private void parseConfigResourceNode(final Node resourceNode,
                                         final String resName) {
        final String resProtocol = getAttribute(resourceNode, PROTOCOL_PARAM);
        if (resProtocol != null) {
            Map<String, Value> nameValueMap =
                                    optionsMap.get(resName + "." + "resource");
            if (nameValueMap == null) {
                nameValueMap = new HashMap<String, Value>();
            } else {
                optionsMap.remove(resName + "." + "resource");
            }

            nameValueMap.put(PROTOCOL_PARAM, new StringValue(resProtocol));
            optionsMap.put(resName + "." + "resource", nameValueMap);
        }
        final NodeList c = resourceNode.getChildNodes();
        for (int i = 0; i < c.getLength(); i++) {
            final Node n = c.item(i);
            if (n.getNodeName().equals("host")) {
                /* <host> */
                parseHostConfig(resName, n);
            } else if (n.getNodeName().equals("section")
                       || (n.getNodeName().equals("#text")
                           && !"".equals(n.getNodeValue().trim()))) {
                String secName;
                if (n.getNodeName().equals("#text")) {
                    secName = "proxy";
                    /* workaround for broken proxy xml in common section
                       at least till drbd 8.4.2 */
                    Map<String, Value> nameValueMap =
                                       optionsMap.get(resName + "." + secName);
                    if (nameValueMap == null) {
                        nameValueMap = new HashMap<String, Value>();
                    } else {
                        optionsMap.remove(resName + "." + secName);
                    }
                    try {
                        final boolean isProxy =
                          DrbdProxy.parse(this, n.getNodeValue(), nameValueMap);
                        if (!isProxy) {
                            continue;
                        }
                    } catch (Exceptions.DrbdConfigException e) {
                        LOG.appWarning("parseConfigResourceNode: "
                                       + e.getMessage());
                        LOG.appWarning("parseConfigResourceNode: "
                                       + n.getNodeValue());
                        continue;
                    }
                    optionsMap.put(resName + "." + secName, nameValueMap);
                } else {
                    /* <resource> */
                    secName = getAttribute(n, "name");

                    Map<String, Value> nameValueMap =
                                       optionsMap.get(resName + "." + secName);
                    if (nameValueMap == null) {
                        nameValueMap = new HashMap<String, Value>();
                    } else {
                        optionsMap.remove(resName + "." + secName);
                    }

                    parseConfigSectionNode(n, nameValueMap);
                    optionsMap.put(resName + "." + secName, nameValueMap);
                }
                if (!sectionParamsMap.containsKey(secName)
                    && !sectionParamsMap.containsKey(secName + "-options")) {
                    LOG.appWarning("parseConfigResourceNode: unknown section: "
                                   + secName);
                    if (!unknownSections) {
                        /* unknown section, so it's not removed. */
                        Tools.progressIndicatorFailed(
                                          "DRBD: unknown section: " + secName);
                        unknownSections = true;
                    }
                }
            }
        }
    }

    /** Parses config xml from drbdadm dump-xml. */
    private void parseConfig(final String configXML) {
        final int start = configXML.indexOf("<config");
        if (start < 0) {
            final String c = configXML.trim();
            if (c.length() != 0 && !IGNORE_CONFIG_ERRORS.contains(c)) {
                LOG.error("parseConfig: error: " + c);
            }
            return;
        }
        final Document document = getXMLDocument(configXML.substring(start));
        if (document == null) {
            return;
        }

        /* get root <config> */
        final Node configNode = getChildNode(document, "config");
        if (configNode == null) {
            return;
        }
        /* config file=".." TODO: */
        configFile = getAttribute(configNode, "file");

        final NodeList resources = configNode.getChildNodes();
        Map<String, Value> globalNameValueMap = optionsMap.get(GLOBAL_SECTION);
        if (globalNameValueMap == null) {
            globalNameValueMap = new HashMap<String, Value>();
            optionsMap.put(GLOBAL_SECTION, globalNameValueMap);
        }
        globalNameValueMap.put("usage-count", CONFIG_YES);
        globalNameValueMap.put("disable-ip-verification", CONFIG_NO);

        for (int i = 0; i < resources.getLength(); i++) {
            final Node resourceNode = resources.item(i);
            /* <global> */
            if (resourceNode.getNodeName().equals(GLOBAL_SECTION)) {
                parseConfigGlobalNode(resourceNode, globalNameValueMap);
            }
            /* <common> */
            if (resourceNode.getNodeName().equals("common")) {
                parseConfigResourceNode(resourceNode, "Section.Common");
            }
            /* <resource> */
            if (resourceNode.getNodeName().equals("resource")) {
                final String resName = getAttribute(resourceNode, "name");
                if (!resourceList.contains(resName)) {
                    resourceList.add(resName);
                }
                parseConfigResourceNode(resourceNode, resName);
            }
        }
    }

    /** Returns value from drbd global config identified by option name. */
    public Value getGlobalConfigValue(final String optionName) {
        final Map<String, Value> option = optionsMap.get(GLOBAL_SECTION);
        if (option != null) {
            return option.get(optionName);
        }
        return null;
    }

    /**
     * Returns value from drbd config identified by resource, section and
     * option name.
     */
    public Value getConfigValue(final String res,
                                final String section,
                                final String optionName) {
        final Map<String, Value> option = optionsMap.get(res + "." + section);

        Value value = null;
        if (option != null) {
            value = option.get(optionName);
        }

        return value;
    }

    /** Returns config value from the common section. */
    public Value getCommonConfigValue(final String section,
                                       final String optionName) {
        Value value = null;
        final Map<String, Value> option =
                                optionsMap.get("Section.Common." + section);
        if (option != null) {
            value = option.get(optionName);
        }
        return value;
    }

    /** Returns array of resource. */
    public String[] getResources() {
        return resourceList.toArray(new String[resourceList.size()]);
    }

    /**
     * Returns drbd device of the resource. Although there can be different
     * drbd devices on different hosts. We do not allow that.
     */
    public String getDrbdDevice(final String res, final String volumeNr) {
        return resourceDeviceMap.get(res, volumeNr);
    }

    /** Returns map from res and volume to drbd device. */
    public MultiKeyMap<String, String> getResourceDeviceMap() {
        return resourceDeviceMap;
    }

    /** Gets block device object from device number. Can return null. */
    private BlockDevInfo getBlockDevInfo(final String devNr,
                                         final String hostName,
                                         final DrbdGraph drbdGraph) {
        final String disk = getBackingDisk(devNr, hostName);
        if (disk != null) {
            return drbdGraph.findBlockDevInfo(hostName, disk);
        }
        return null;
    }

    /** Return backing disk from device number. Can return null. */
    private String getBackingDisk(final String devNr, final String hostName) {
        BlockDevInfo bdi = null;
        final String device = "/dev/drbd" + devNr;
        final String resName = deviceResourceMap.get(device);
        String volumeNr = deviceVolumeMap.get(device);
        if (volumeNr == null) {
            volumeNr = "0";
        }
        if (resName != null) {
            final Map<String, String> hostDiskMap =
                                    resourceHostDiskMap.get(resName, volumeNr);
            if (hostDiskMap != null) {
                return hostDiskMap.get(hostName);
            }
        }
        return null;
    }


    /** Returns whether the drbd is loaded. */
    boolean isDrbdLoaded(final String hostName) {
        final Boolean l = hostDrbdLoadedMap.get(hostName);
        if (l != null) {
            return l.booleanValue();
        }
        return true;
    }

    /**
     * Parses events from drbd kernel module obtained via drbdsetup .. events
     * command and stores the values in the BlockDevice object.
     */
    public boolean parseDrbdEvent(final String hostName,
                                  final DrbdGraph drbdGraph,
                                  final String rawOutput) {
        if (rawOutput == null || hostName == null) {
            return false;
        }

        final String output = rawOutput.trim();
        if ("".equals(output)) {
            return false;
        }
        if ("No response from the DRBD driver! Is the module loaded?".equals(
                output)) {
            if (hostDrbdLoadedMap.get(hostName)) {
                hostDrbdLoadedMap.put(hostName, false);
                return true;
            } else {
                return false;
            }
        } else {
            hostDrbdLoadedMap.put(hostName, true);
        }
        /* since drbd 8.3 there is ro: instead of st: */
        /* since drbd 8.4 there is ro: instead of st: */
        Pattern p =
            Pattern.compile(
                "^(\\d+)\\s+ST\\s+(\\S+)\\s+\\{\\s+cs:(\\S+)\\s+"
                + "(?:st|ro):(\\S+)/(\\S+)\\s+ds:(\\S+)/(\\S+)\\s+(\\S+).*?");
        Matcher m = p.matcher(output);
        final Pattern pDev = Pattern.compile("^(\\d+),(\\S+)\\[(\\d+)\\]$");
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNrString  = m.group(2);
            final String cs           = m.group(3);
            final String ro1          = m.group(4);
            final String ro2          = m.group(5);
            final String ds1          = m.group(6);
            final String ds2          = m.group(7);
            final String flags        = m.group(8);

            final Matcher mDev = pDev.matcher(devNrString);
            String devNr = devNrString;
            String res = ""; // TODO: unused */
            String volumeNr = "0";
            if (mDev.matches()) { /* since 8.4 */
                devNr = mDev.group(1);
                res = mDev.group(2);
                volumeNr = mDev.group(3);
            }
            /* get blockdevice object from device */
            final String disk = getBackingDisk(devNr, hostName);
            if (disk != null) {
                final BlockDevInfo bdi =
                                   drbdGraph.findBlockDevInfo(hostName, disk);
                if (bdi != null) {
                    if (bdi.getBlockDevice().isDifferent(cs,
                                                         ro1,
                                                         ds1,
                                                         flags)) {
                        bdi.getBlockDevice().setDrbdBackingDisk(disk);
                        bdi.getBlockDevice().setConnectionState(cs);
                        bdi.getBlockDevice().setNodeState(ro1);
                        bdi.getBlockDevice().setDiskState(ds1);
                        bdi.getBlockDevice().setNodeStateOther(ro2);
                        bdi.getBlockDevice().setDiskStateOther(ds2);
                        bdi.getBlockDevice().setDrbdFlags(flags);
                        bdi.updateInfo();
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
        /* 19 SP 0 16.9 */
        p = Pattern.compile("^(\\d+)\\s+SP\\s+(\\S+)\\s(\\d+\\.\\d+).*");
        m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNrString = m.group(2);
            final String synced = m.group(3);

            final Matcher mDev = pDev.matcher(devNrString);
            String devNr = devNrString;
            String res = ""; // TODO: unused */
            String volumeNr = "0";
            if (mDev.matches()) { /* since 8.4 */
                devNr = mDev.group(1);
                res = mDev.group(2);
                volumeNr = mDev.group(3);
            }
            final BlockDevInfo bdi =
                                   getBlockDevInfo(devNr, hostName, drbdGraph);
            if (bdi != null && bdi.getBlockDevice().isDrbd()) {
                if (Tools.areEqual(bdi.getBlockDevice().getSyncedProgress(),
                                   synced)) {
                    return false;
                } else {
                    bdi.getBlockDevice().setSyncedProgress(synced);
                    bdi.updateInfo();
                    return true;
                }
            }
            return false;
        }
        /* 19 UH 1 split-brain */
        p = Pattern.compile("^(\\d+)\\s+UH\\s+(\\S+)\\s([a-z-]+).*");
        m = p.matcher(output);
        if (m.matches()) {
            /* String counter      = m.group(1); // not used */
            final String devNrString = m.group(2);
            final String what = m.group(3);
            final Matcher mDev = pDev.matcher(devNrString);
            String devNr = devNrString;
            String res = ""; // TODO: unused */
            String volumeNr = "0";
            if (mDev.matches()) { /* since 8.4 */
                devNr = mDev.group(1);
                res = mDev.group(2);
                volumeNr = mDev.group(3);
            }
            LOG.debug("parseDrbdEvent: event: " + devNr + " - " + what);
            if ("split-brain".equals(what)) {
                final BlockDevInfo bdi = getBlockDevInfo(devNr,
                                                       hostName,
                                                       drbdGraph);

                if (bdi != null && bdi.getBlockDevice().isDrbd()) {
                    if (bdi.getBlockDevice().isSplitBrain()) {
                        return false;
                    } else {
                        bdi.getBlockDevice().setSplitBrain(true);
                        bdi.updateInfo();
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    /** Removes the resource from resources, so that it does not reappear. */
    public void removeResource(final String res) {
        resourceList.remove(res);
    }

    /** Removes the volume from hashes, so that it does not reappear. */
    public void removeVolume(final String res,
                             final String dev,
                             final String volumeNr) {
        resourceDeviceMap.remove(res, volumeNr);
        deviceResourceMap.remove(dev);
        deviceVolumeMap.remove(dev);
    }

    /**
     * Whether drbd is disabled. If there are unknown sections, that we don't
     * want to overwrite.
     */
    public boolean isDrbdDisabled() {
        return unknownSections && !Tools.getConfigData().isAdvancedMode();
    }

    public class HostProxy {
        private final String proxyHostName;
        private final Value insideIp;
        private final Value insidePort;
        private final Value outsideIp;
        private final Value outsidePort;

        public HostProxy(final String proxyHostName,
                         final Value insideIp,
                         final Value insidePort,
                         final Value outsideIp,
                         final Value outsidePort) {
            this.proxyHostName = proxyHostName;
            this.insideIp = insideIp;
            this.insidePort = insidePort;
            this.outsideIp = outsideIp;
            this.outsidePort = outsidePort;
        }

        public String getProxyHostName() {
            return proxyHostName;
        }

        public Value getInsideIp() {
            return insideIp;
        }

        public Value getInsidePort() {
            return insidePort;
        }

        public Value getOutsideIp() {
            return outsideIp;
        }

        public Value getOutsidePort() {
            return outsidePort;
        }
    }

    public String getOldConfig() {
        return oldConfig;
    }

    public Value parseValue(final String param, final String v) {
        if (v == null) {
            return null;
        }
        final Matcher m = UNIT_PATTERN.matcher(v);
        if (m.matches()) {
            final String value = m.group(1);
            final String u = m.group(2);

            final Unit unit = parseUnit(param, u);
            return new StringValue(value, unit);
        }
        return new StringValue(v);
    }

    private Unit parseUnit(final String param, final String u) {
        if (!hasUnitPrefix(param)) {
            return null;
        }
        final String unitType = getUnitLong(param);
        if (unitType == null || "".equals(unitType)) {
            return parseSizeUnit(param, u);
        } else {
            return parseByteUnit(param, u);
        }
    }

    private Unit parseSizeUnit(final String param, final String u) {
        final String unitPart = getUnitPart(getUnitLong(param));
        if (u == null || "".equals(u)) {
            return getUnitDefault(unitPart);
        } else if ("k".equalsIgnoreCase(u)) {
            return getUnitKi(unitPart);
        } else if ("m".equalsIgnoreCase(u)) {
            return getUnitMi(unitPart);
        } else if ("g".equalsIgnoreCase(u)) {
            return getUnitGi(unitPart);
        } else if ("t".equalsIgnoreCase(u)) {
            return getUnitTi(unitPart);
        } else {
            LOG.appError("can't parse unit: " + u + " param: " + param);
        }
        return null;
    }

    private Unit parseByteUnit(final String param, final String u) {
        final String unitPart = getUnitPart(getUnitLong(param));
        if (u == null || "".equals(u)) {
            return getUnitBytes(unitPart);
        } else if ("k".equalsIgnoreCase(u)) {
            return getUnitKiBytes(unitPart);
        } else if ("m".equalsIgnoreCase(u)) {
            return getUnitMiBytes(unitPart);
        } else if ("g".equalsIgnoreCase(u)) {
            return getUnitGiBytes(unitPart);
        } else if ("t".equalsIgnoreCase(u)) {
            return getUnitTiBytes(unitPart);
        } else if ("s".equalsIgnoreCase(u)) {
            return getUnitSectors(unitPart);
        } else {
            LOG.appError("can't parse unit: " + u + " param: " + param);
        }
        return null;
    }

    public static Unit getUnitBytes(final String unitPart) {
        return new Unit("", "", "Byte" + unitPart, "Bytes" + unitPart);
    }

    public static Unit getUnitKiBytes(final String unitPart) {
        return new Unit("K", "k", "KiByte" + unitPart, "KiBytes" + unitPart);
    }

    public static Unit getUnitMiBytes(final String unitPart) {
        return new Unit("M", "m", "MiByte" + unitPart, "MiBytes" + unitPart);
    }

    public static Unit getUnitGiBytes(final String unitPart) {
        return new Unit("G", "g", "GiByte" + unitPart, "GiBytes" + unitPart);
    }

    public static Unit getUnitTiBytes(final String unitPart) {
        return new Unit("T", "t", "TiByte" + unitPart, "TiBytes" + unitPart);
    }

    public static Unit getUnitSectors(final String unitPart) {
        return new Unit("s", "s", "Sector" + unitPart, "Sectors" + unitPart); }

    /** Returns units. */

    public static Unit[] getByteUnits(final String unitPart) {
        return new Unit[]{getUnitBytes(unitPart),
                          getUnitKiBytes(unitPart),
                          getUnitMiBytes(unitPart),
                          getUnitGiBytes(unitPart),
                          getUnitSectors(unitPart)};
    }

    public static Unit getUnitDefault(final String unitPart) {
        return new Unit("", "", "", "");
    }

    public static Unit getUnitKi(final String unitPart) {
        return new Unit("k", "K", "k", "k");
    }
                    
    public static Unit getUnitMi(final String unitPart) {
        return new Unit("m", "M", "m", "m");
    }

    public static Unit getUnitGi(final String unitPart) {
        return new Unit("g", "G", "g", "g");
    }

    public static Unit getUnitTi(final String unitPart) {
        return new Unit("t", "T", "t", "t");
    }

    public static Unit[] getUnits(final String unitPart) {
        return new Unit[]{getUnitDefault(unitPart),
                          getUnitKi(unitPart),
                          getUnitMi(unitPart),
                          getUnitGi(unitPart)};
    }

    /** Return part after '/' from the unit long description. */
    public static String getUnitPart(final String unitLong) {
        if (unitLong == null) {
            return "";
        }

        final int index = unitLong.indexOf('/');
        String unitPart = "";
        if (index > -1) {
            unitPart = unitLong.substring(index);
        }
        return unitPart;
    }
}
