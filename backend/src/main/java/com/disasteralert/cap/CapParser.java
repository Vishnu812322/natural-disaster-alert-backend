package com.disasteralert.cap;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class CapParser {

    public ParsedCap parse(InputStream input) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        Document document =
                factory.newDocumentBuilder().parse(input);

        Element root = document.getDocumentElement();

        String areaDesc = text(root, "areaDesc");
        String circle = findCircle(root);
        String polygon = text(root, "polygon");
        String polygonUrl = findPolygonUrl(root);

        List<String> lgdDistrictCodes = findLgdDistrictCodes(root);

        return new ParsedCap(
                text(root, "identifier"),
                text(root, "sender"),
                text(root, "sent"),
                text(root, "status"),
                text(root, "msgType"),
                text(root, "scope"),
                text(root, "event"),
                text(root, "headline"),
                text(root, "description"),
                text(root, "severity"),
                text(root, "urgency"),
                text(root, "certainty"),
                areaDesc,
                polygonUrl,
                circle,
                polygon,
                lgdDistrictCodes,
                text(root, "instruction"),
                text(root, "expires")
        );
    }

    private String text(Element root, String name) {

        NodeList nodes =
                root.getElementsByTagNameNS("*", name);

        if (nodes.getLength() == 0) {
            return null;
        }

        String value =
                nodes.item(0).getTextContent();

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String findPolygonUrl(Element root) {

        NodeList parameters =
                root.getElementsByTagNameNS("*", "parameter");

        for (int i = 0; i < parameters.getLength(); i++) {

            Element parameter =
                    (Element) parameters.item(i);

            String valueName = null;
            String value = null;

            NodeList children =
                    parameter.getElementsByTagNameNS("*", "valueName");

            if (children.getLength() > 0) {
                valueName =
                        children.item(0).getTextContent();
            }

            children =
                    parameter.getElementsByTagNameNS("*", "value");

            if (children.getLength() > 0) {
                value =
                        children.item(0).getTextContent();
            }

            if (valueName != null
                    && valueName.trim()
                            .equalsIgnoreCase("Polygon URL")
                    && value != null
                    && value.trim().startsWith("http")) {

                return value.trim();
            }
        }

        return null;
    }

    private String findCircle(Element root) {

        NodeList nodes =
                root.getElementsByTagNameNS("*", "circle");

        for (int i = 0; i < nodes.getLength(); i++) {

            String value =
                    nodes.item(i).getTextContent();

            if (value == null || value.isBlank()) {
                continue;
            }

            value = value.trim();

            // A real CAP circle looks like:
            // latitude,longitude radius

            if (value.matches(
                    "[-+]?\\d+(\\.\\d+)?\\s*,\\s*"
                    + "[-+]?\\d+(\\.\\d+)?\\s+"
                    + "[-+]?\\d+(\\.\\d+)?"
            )) {
                return value;
            }
        }

        return null;
    }

    /**
     * Extract official LGD District Codes from CAP.
     *
     * Example:
     *
     * <cap:geocode>
     *     <cap:valueName>LGD District Code</cap:valueName>
     *     <cap:value>480</cap:value>
     * </cap:geocode>
     */
    private List<String> findLgdDistrictCodes(Element root) {

        List<String> codes = new ArrayList<>();

        NodeList geocodes =
                root.getElementsByTagNameNS("*", "geocode");

                System.out.println(
        "CAP GEOCODE COUNT = " + geocodes.getLength()
);

        for (int i = 0; i < geocodes.getLength(); i++) {

            Element geocode =
                    (Element) geocodes.item(i);

                    System.out.println(
        "GEOCODE RAW = " + geocode.getTextContent()
);

            String valueName = null;
            String value = null;

            NodeList valueNameNodes =
                    geocode.getElementsByTagNameNS(
                            "*",
                            "valueName"
                    );

            if (valueNameNodes.getLength() > 0) {
                valueName =
                        valueNameNodes
                                .item(0)
                                .getTextContent();
            }

            NodeList valueNodes =
                    geocode.getElementsByTagNameNS(
                            "*",
                            "value"
                    );

            if (valueNodes.getLength() > 0) {
                value =
                        valueNodes
                                .item(0)
                                .getTextContent();
            }
            System.out.println(
        "GEOCODE valueName=[" + valueName + "] value=[" + value + "]"
);

            if (valueName != null
                    && valueName.trim()
                            .equalsIgnoreCase(
                                    "LGD District Code"
                            )
                    && value != null
                    && !value.trim().isBlank()) {

                String code = value.trim();

                if (!codes.contains(code)) {
                    codes.add(code);
                }
            }
        }

        return codes;
    }

    public record ParsedCap(
            String identifier,
            String sender,
            String sent,
            String status,
            String msgType,
            String scope,
            String event,
            String headline,
            String description,
            String severity,
            String urgency,
            String certainty,
            String areaDesc,
            String polygonUrl,
            String circle,
            String polygon,
            List<String> lgdDistrictCodes,
            String instruction,
            String expires
    ) {}
}