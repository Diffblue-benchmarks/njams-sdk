/* 
 * Copyright (c) 2018 Faiz & Siegeln Software GmbH
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * The Software shall be used for Good, not Evil.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.im.njams.sdk.logmessage;

import com.faizsiegeln.njams.messageformat.v4.projectmessage.AttributeType;
import com.faizsiegeln.njams.messageformat.v4.projectmessage.Extract;
import com.faizsiegeln.njams.messageformat.v4.projectmessage.ExtractRule;
import com.faizsiegeln.njams.messageformat.v4.projectmessage.RuleType;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPathConstants;
import net.sf.saxon.xpath.XPathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author pnientiedt
 */
public class ExtractHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractHandler.class);
    private static final Map<String, Matcher> MATCHER = new ConcurrentHashMap<>();

    private ExtractHandler() {
        //utility
    }

    /**
     * Handle extract. Input is the sourceData object and the string data, which
     * is only filled if a tracepoint was already evaluated. If not, the
     * sourceData has to be serialized via Njams to get extractData.
     *
     * @param job the job
     * @param activity the activity
     * @param direction the diration
     * @param sourceData the sourceData
     * @param data the data
     */
    public static void handleExtract(JobImpl job, ActivityImpl activity, String direction, Object sourceData, String data) {

        Extract extract = job.getExtract(activity.getModelId());
        if (extract == null) {
            return;
        }

        Iterator<ExtractRule> erl = extract.getExtractRules().iterator();

        XpathContext xpathContext = new XpathContext();

        while (erl.hasNext()) {
            ExtractRule er = erl.next();

            if (er.getRuleType() == RuleType.DISABLED || !er.getInout().equalsIgnoreCase(direction)) {
                continue;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("nJAMS: after execution - rule: " + er.getRule());
            }

            switch (er.getRuleType()) {
                case REGEXP:
                    doRegexp(job, activity, er, sourceData, data);
                    break;
                case EVENT:
                    doEvent(job, activity, er);
                    break;
                case VALUE:
                    doValue(job, activity, er);
                    break;
                case XPATH:
                    doXpath(job, activity, er, sourceData, data, xpathContext);
                    break;
                default:
                    break;
            }
        }
    }

    private static void doRegexp(JobImpl job, ActivityImpl activity, ExtractRule er, Object sourceData, String paramData) {
        String data = paramData;
        if (paramData == null || paramData.length() == 0) {
            data = job.getProcessModel().getNjams().serialize(sourceData);
            if (data == null || data.length() == 0) {
                return;
            }
        }
        String setting = er.getAttribute();

        LOG.debug("nJAMS: regex extract for setting: {}", setting);

        Matcher localMatcher = getExtractMatcher(er.getRule());
        if (localMatcher == null) {
            return;
        }

        if (localMatcher.reset(data).find()) {
            int group = localMatcher.groupCount() > 0 ? 1 : 0;
            String value = localMatcher.group(group);

            LOG.debug("nJAMS: regex result: {}", value);

            if (er.getAttributeType() == AttributeType.EVENT) {
                activity.setEventStatus(getEventStatus(value));
            } else {
                setAttributes(job, activity, setting, value);
            }
        }
        job.setInstrumented(true);
    }

    private static void doEvent(JobImpl job, ActivityImpl activity, ExtractRule er) {
        String evt = er.getRule();
        if (evt != null && evt.length() > 0) {
            activity.setEventStatus(getEventStatus(evt));
            job.setInstrumented(true);
        }
    }

    private static void doValue(JobImpl job, ActivityImpl activity, ExtractRule er) {
        if (er.getAttributeType() == AttributeType.EVENT) {
            activity.setEventStatus(getEventStatus(er.getRule()));
        } else {
            setAttributes(job, activity, er.getAttribute(), er.getRule());
        }
        job.setInstrumented(true);
    }

    private static void doXpath(JobImpl job, ActivityImpl activity, ExtractRule er, Object sourceData, String paramData, XpathContext xpathContext) {
        String data = paramData;
        if (paramData == null || paramData.length() == 0) {
            data = job.getProcessModel().getNjams().serialize(sourceData);
            if (data == null || data.length() == 0) {
                return;
            }
        }
        try {
            if (xpathContext.getXpf() == null) {
                xpathContext.setXpf(new net.sf.saxon.xpath.XPathFactoryImpl());
                xpathContext.setXpath(xpathContext.getXpf().newXPath());
                xpathContext.getXpath().setNamespaceContext(new NamespaceResolver(data, false));
                xpathContext.setSs(new SAXSource(new InputSource(new StringReader(data))));
                xpathContext.setDoc(((XPathEvaluator) xpathContext.getXpath()).setSource(xpathContext.getSs()));
            }
            xpathContext.setExpr(xpathContext.getXpath().compile(er.getRule()));
            Object result = xpathContext.getExpr().evaluate(xpathContext.getDoc(), XPathConstants.NODESET);
            List nodes = null;
            if (result instanceof List) {
                nodes = (List) result;
            } else if (result instanceof NodeList) {
                final int len = ((NodeList) result).getLength();
                nodes = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    String val = ((NodeList) result).item(i).getNodeValue();
                    nodes.add(val);
                }
            } else {
                LOG.error("Unknown class " + result.getClass() + " returned from XPath evaluator");
            }
            String strResult = "";
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    Object o = nodes.get(i);
                    if (o instanceof net.sf.saxon.tinytree.TinyNodeImpl) {
                        if (!(o instanceof net.sf.saxon.tinytree.WhitespaceTextImpl)) {
                            strResult += ((net.sf.saxon.tinytree.TinyNodeImpl) o).getStringValue();
                        }
                    } else {
                        strResult += nodes.get(i);
                    }
                }
            }
            if (er.getAttributeType() == AttributeType.EVENT) {
                LOG.debug("nJAMS: xpath extract for setting: {}", er.getAttribute());
                LOG.debug("nJAMS: xpath result: {}", strResult);
                activity.setEventStatus(getEventStatus(strResult));
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("nJAMS: xpath extract for setting: {}", er.getAttribute());
                    LOG.debug("nJAMS: xpath result: {}", strResult);
                }
                setAttributes(job, activity, er.getAttribute(), strResult);
            }
            job.setInstrumented(true);
        } catch (Exception e) {
            LOG.error("", e);
        }
    }

    private static Matcher getExtractMatcher(String rule) {
        Matcher m = MATCHER.get(rule);
        if (m == null) {
            try {
                Pattern pattern = Pattern.compile(rule);
                m = pattern.matcher("");
                MATCHER.put(rule, m);
            } catch (Exception e) {
                LOG.warn("Could not compile pattern: {}: {}", rule);
                LOG.debug("Error in getExtractMatcher", e);
            }
        }
        return m;
    }

    private static void setAttributes(Job job, ActivityImpl activity, String uncheckedsetting, String uncheckedvalue) {
        String setting = checkLength("Extract Property/Attribute Name", uncheckedsetting, 500);
        String value = checkLength("Extract Property/Attribute '" + setting + "': ", uncheckedvalue, 2000);
        LOG.debug("nJAMS: setAttributes: {}/{}", uncheckedsetting, uncheckedvalue);

        String settingLowerCase = setting.toLowerCase();
        switch (settingLowerCase) {
            case "correlationlogid":
                job.setCorrelationLogId(value);
                break;
            case "parentlogid":
                job.setParentLogId(value);
                break;
            case "externallogid":
                job.setExternalLogId(value);
                break;
            case "businessservice":
                job.setBusinessService(value);
                break;
            case "businessobject":
                job.setBusinessObject(value);
                break;
            case "eventmessage":
                activity.setEventMessage(value);
                break;
            case "eventcode":
                activity.setEventCode(value);
                break;
            case "payload":
                activity.setEventPayload(value);
                break;
            case "stacktrace":
                activity.setStackTrace(value);
                break;
            default:
                activity.addAttribute(setting, value);
        }
    }

    private static int getEventStatus(String status) {
        if ("success".equalsIgnoreCase(status)) {
            return EventStatus.SUCCESS.getValue();
        } else if ("warning".equalsIgnoreCase(status)) {
            return EventStatus.WARNING.getValue();
        } else if ("error".equalsIgnoreCase(status)) {
            return EventStatus.ERROR.getValue();
        } else {
            return EventStatus.INFO.getValue();
        }
    }

    /**
     * Returns the given input string and ensures that it is not longer than the
     * given maximum length.
     *
     * @param context Only used for logging
     * @param in The input message that is returned but possibly truncated
     * @param maxLength Maximum length for the returned string
     * @return The given input but no longer thatn the given maximum length
     */
    public static String checkLength(String context, String in, int maxLength) {
        String out = in;
        if (in != null && out.length() > maxLength) {
            LOG.warn("String too long (max length: " + maxLength + ") for '" + context + "': " + in);
            out = out.substring(0, maxLength - 1);
            LOG.warn("Using truncated version of input string: " + out);
        }
        return out;
    }
}