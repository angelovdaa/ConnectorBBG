package com.neoxam.poc;

import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.Name;

import com.bloomberglp.blpapi.Names;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Main {
    private static final String BLOOMBERG_REF_DATA_SERVICE_NAME_PROPERTY = "//blp/refdata";

    private static final String REFERENCE_DATA_REQUEST = "ReferenceDataRequest";

    // Names
    private static final Name SECURITIES = new Name("securities");
    private static final Name FIELDS = new Name("fields");
    private static final Name REASON = Name.getName("reason");

    private static final Name SECURITY_DATA = new Name("securityData");
    private static final Name SECURITY = new Name("security");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name RESPONSE_ERROR = new Name("responseError");
    private static final Name SECURITY_ERROR = new Name("securityError");
    private static final Name FIELD_EXCEPTIONS = new Name("fieldExceptions");
    private static final Name FIELD_ID = new Name("fieldId");
    private static final Name ERROR_INFO = new Name("errorInfo");

    private static final String CSV_FILE_NAME = "Result_BBG.csv";
    // Init data map
    private static final
    Map<String, Map<String, String>> dataMap = new TreeMap<>();

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Wrong input, must be: API_KEY HOST PORT TICKER1;TICKER2...  FIELD1;FIELD2....");
            return;
        }
        String apiKey, host;
        String[] tickers;
        String[] fields;
        Integer port;
        // Parse args
        try {
            apiKey = args[0];
            host = args[1];
            port = Integer.parseInt(args[2]);
            tickers = args[3].split(";");
            fields = args[4].split(";");

            if (tickers.length == 0 || fields.length == 0) {
                System.out.println("Wrong input, must be: TICKER1;TICKER2...  FIELD1;FIELD2....");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Wrong input, must be: TICKER1;TICKER2...  FIELD1;FIELD2....");
            return;

        }

        SessionOptions sessionOptions = new SessionOptions();
        // TODO: add session authentication options here
        sessionOptions.setApplicationIdentityKey(apiKey);
        sessionOptions.setServerHost(host);
        sessionOptions.setServerPort(port);// default is 8194;

        Session session = new Session(sessionOptions);
        try {
            if (!session.start() || !session.openService(BLOOMBERG_REF_DATA_SERVICE_NAME_PROPERTY)) {
                checkFailures(session);
                return;
            }

            // Send request
            Service service = session.getService(BLOOMBERG_REF_DATA_SERVICE_NAME_PROPERTY);
            Request request = createRequest(service, tickers, fields);
            System.out.println("Sending Request " + request.getRequestId() + ": " + request);
            session.sendRequest(request, null); // correlationId

            // Treat response
            waitForResponse(session);
            createCSV(dataMap);
            session.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Request createRequest(Service service, String[] tickers, String[] fields) {
        Request request = service.createRequest(REFERENCE_DATA_REQUEST);
        // Add securities to request
        Element securitiesElement = request.getElement(SECURITIES);
        for (String security : tickers) {
            securitiesElement.appendValue(security);
        }
        // Add fields to request
        Element fieldsElement = request.getElement(FIELDS);
        for (String field : fields) {
            fieldsElement.appendValue(field);
        }
        return request;
    }

    private static void waitForResponse(Session session) throws Exception {
        boolean done = false;
        while (!done) {
            Event event = session.nextEvent();
            Event.EventType eventType = event.eventType();
            if (eventType == Event.EventType.PARTIAL_RESPONSE) {
                System.out.println("Processing Partial Response");
                processResponseEvent(event);
            } else if (eventType == Event.EventType.RESPONSE) {
                System.out.println("Processing Response");
                processResponseEvent(event);
                done = true;
            } else if (eventType == Event.EventType.REQUEST_STATUS) {
                for (Message msg : event) {
                    System.out.println("Received request status message:" + msg);
                    if (msg.messageType().equals(Names.REQUEST_FAILURE)) {
                        Element reason = msg.getElement(REASON);
                        System.err.println("Request failed: " + reason);
                        done = true;
                    }
                }
            } else {
                // SESSION_STATUS events can happen at any time and should be
                // handled as the session can be terminated, e.g.
                // session identity can be revoked at a later time, which
                // terminates the session.
                done = processGenericEvent(event);
            }
        }
    }

    public static void processResponseEvent(Event event) {
        for (Message msg : event) {
            System.out.println("Received response to request " + msg.getRequestId());
            if (msg.hasElement(RESPONSE_ERROR)) {
                System.out.println("REQUEST FAILED: " + msg.getElement(RESPONSE_ERROR));
                continue;
            }

            Element securities = msg.getElement(SECURITY_DATA);
            int numSecurities = securities.numValues();
            System.out.println("Processing " + numSecurities + " securities:");
            for (int i = 0; i < numSecurities; ++i) {
                Element security = securities.getValueAsElement(i);
                String ticker = security.getElementAsString(SECURITY);
                System.out.println();
                System.out.println("Ticker: " + ticker);
                if (security.hasElement(SECURITY_ERROR)) {
                    System.out.println("SECURITY FAILED: " + security.getElement(SECURITY_ERROR));
                    continue;
                }

                if (security.hasElement(FIELD_DATA)) {
                    Element fields = security.getElement(FIELD_DATA);
                    getDataFromElements(ticker, fields);
                }
                System.out.println();
                Element fieldExceptions = security.getElement(FIELD_EXCEPTIONS);
                if (fieldExceptions.numValues() > 0) {
                    System.out.println("FIELD\t\tEXCEPTION");
                    System.out.println("-----\t\t---------");
                    for (int k = 0; k < fieldExceptions.numValues(); ++k) {
                        Element fieldException = fieldExceptions.getValueAsElement(k);
                        System.out.println(
                                fieldException.getElementAsString(FIELD_ID)
                                        + "\t\t"
                                        + fieldException.getElement(ERROR_INFO));
                    }
                }
            }
        }
    }

    private static void getDataFromElements(String ticker, Element fields) {
        Map<String, String> fieldMap;
        if (dataMap.containsKey(ticker)) {
            fieldMap = dataMap.get(ticker);
        } else {
            fieldMap = new TreeMap<>();
            dataMap.put(ticker, fieldMap);
        }
        if (fields.numElements() > 0) {
            System.out.println("FIELD\t\tVALUE");
            System.out.println("-----\t\t-----");
            int numElements = fields.numElements();
            for (int j = 0; j < numElements; ++j) {
                Element field = fields.getElement(j);
                fieldMap.put(field.name().toString(), field.getValueAsString());
            }
        }

    }

    private static void checkFailures(Session session) {
        while (true) {
            Event event = session.tryNextEvent();
            if (event == null) {
                break;
            }
            if (processGenericEvent(event)) {
                break;
            }
        }
    }

    private static boolean processGenericEvent(Event event) {
        Event.EventType eventType = event.eventType();
        for (Message msg : event) {
            System.out.println(msg);
            Name messageType = msg.messageType();
            if (eventType == Event.EventType.SESSION_STATUS) {
                if (messageType.equals(Names.SESSION_TERMINATED)
                        || messageType.equals(Name.getName("SessionStartupFailure"))) {
                    System.err.println("Failed to start session or session terminated.");
                    return true;
                }
            } else if (eventType == Event.EventType.SERVICE_STATUS) {
                if (messageType.equals(Names.SERVICE_OPEN_FAILURE)) {
                    System.err.println("Failed to open service " + BLOOMBERG_REF_DATA_SERVICE_NAME_PROPERTY + ".");
                }
            }
        }

        return false;
    }

    private static boolean createCSV(Map<String, Map<String, String>> dataMap) {
        if (dataMap.values().isEmpty()) {
            return false;
        }
        List<String> headers = dataMap.values().stream().flatMap(map -> map.keySet().stream()).distinct().collect(Collectors.toList());
        headers.add(0, "TICKER");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < headers.size(); i++) {
            sb.append(headers.get(i));
            sb.append(i == headers.size() - 1 ? "\n" : ",");
        }
        for (Map.Entry<String, Map<String, String>> kvp : dataMap.entrySet()) {
            String ticker = kvp.getKey();
            Map<String, String> fieldMap = kvp.getValue();
            sb.append(ticker + ",");
            for (int i = 1; i < headers.size(); i++) {
                sb.append(fieldMap.get(headers.get(i)));
                sb.append(i == headers.size() - 1 ? "\n" : ",");
            }
        }
        File csvOutputFile = new File(CSV_FILE_NAME);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            pw.print(sb);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
}